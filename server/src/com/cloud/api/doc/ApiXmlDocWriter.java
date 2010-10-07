/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api.doc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.xstream.XStream;

public class ApiXmlDocWriter {
	private static final Logger s_logger = Logger.getLogger(ApiXmlDocWriter.class.getName());
	private static Properties api_commands = new Properties();
	private static String dirName="";
	
	public static void main (String[] args) {
		Properties preProcessedCommands = new Properties();
		Enumeration command = null;
		String[] fileNames = null;
		
		List<String> argsList = Arrays.asList(args);
		Iterator<String> iter = argsList.iterator();
		while (iter.hasNext()) {
			String arg = iter.next();
			// populate the file names
			if (arg.equals("-f")) {
				fileNames = iter.next().split(",");
			}
			if (arg.equals("-d")) {
				dirName = iter.next();
			}
		}

		if ((fileNames == null) || (fileNames.length == 0)){
			s_logger.error("Please specify input file(s) separated by coma using -f option");
			System.exit(2);
		}
		
		for (String fileName : fileNames) {
			try {
				FileInputStream in = new FileInputStream(fileName);
				preProcessedCommands.load(in);
			}catch (FileNotFoundException ex) {
				s_logger.error("Can't find file " + fileName);
				System.exit(2);
			} catch (IOException ex1) {
				s_logger.error("Error reading from file " + ex1);
				System.exit(2);
			}
		}
		
		//Get command classes and response object classes
		for (Object key : preProcessedCommands.keySet()) {
            String preProcessedCommand = preProcessedCommands.getProperty((String)key);
            String[] commandParts = preProcessedCommand.split(";");
            api_commands.put(key, commandParts[0]);
		}
		
		command = api_commands.propertyNames();
		
		try {
			//Create object writer
			XStream xs = new XStream();
			xs.alias("command", Command.class);
			xs.alias("arg", Argument.class);

			ObjectOutputStream out = xs.createObjectOutputStream(new FileWriter(dirName + "commands.xml"), "commands");

			while (command.hasMoreElements()) {
				String key = (String) command.nextElement();
				Class clas = Class.forName(api_commands.getProperty(key));
				ArrayList<Argument> request = new ArrayList<Argument>();
				ArrayList<Argument> response = new ArrayList<Argument>();
				
				//Create a new command, set name and description
				Command apiCommand = new Command();
				apiCommand.setName(key);
	            Implementation impl = (Implementation)clas.getAnnotation(Implementation.class);
	            if (impl == null)
	            	impl = (Implementation)clas.getSuperclass().getAnnotation(Implementation.class);
	            String commandDescription = impl.description();
	            if (commandDescription != null)
	            	apiCommand.setDescription(commandDescription);
	            else
	            	s_logger.warn("Command " + apiCommand.getName() + " misses description");
	            
	            //Get request parameters        
	            Field[] fields = clas.getDeclaredFields();
	            
	            //Get fields from superclass
	            Class<?> superClass = clas.getSuperclass();
	            while (BaseCmd.class.isAssignableFrom(superClass) && !superClass.getName().equals(BaseCmd.class.getName())) {
	            	Field[] superClassFields = superClass.getDeclaredFields();
	                if (superClassFields != null) {
	                    Field[] tmpFields = new Field[fields.length + superClassFields.length];
	                    System.arraycopy(fields, 0, tmpFields, 0, fields.length);
	                    System.arraycopy(superClassFields, 0, tmpFields, fields.length, superClassFields.length);
	                    fields = tmpFields;
	                }
	                superClass = superClass.getSuperclass();
	            }
	          
				for (Field f : fields) {
					Parameter parameterAnnotation = f.getAnnotation(Parameter.class);
					if (parameterAnnotation != null) {
						Argument reqArg = new Argument(parameterAnnotation.name());
						reqArg.setRequired(parameterAnnotation.required());
						if (!parameterAnnotation.description().isEmpty())
							reqArg.setDescription(parameterAnnotation.description());
						else
							s_logger.warn("Description is missing for the parameter " + parameterAnnotation.name() + " of the command " + apiCommand.getName() );
						
						request.add(reqArg);
					}
				}
	            
	            //Get response parameters
	            Method getResponseMethod = clas.getMethod("getResponse");
	            Class responseClas = (Class)getResponseMethod.getReturnType();
	            Type returnType = getResponseMethod.getGenericReturnType();

	            if(returnType != null && returnType instanceof ParameterizedType){
	                ParameterizedType type = (ParameterizedType) returnType;
	                Type[] typeArguments = type.getActualTypeArguments();
	                responseClas = (Class)typeArguments[0];
	            }
				
				//Get response parameters
				Field[] responseFields = responseClas.getDeclaredFields();
				for (Field responseField : responseFields) {
					SerializedName nameAnnotation = responseField.getAnnotation(SerializedName.class);
					Param descAnnotation = responseField.getAnnotation(Param.class);
					Argument respArg = new Argument(nameAnnotation.value());
					if (descAnnotation != null)
						respArg.setDescription(descAnnotation.description());
					response.add(respArg);
				}
	            
	            
	            apiCommand.setRequest(request);
	            apiCommand.setResponse(response);
	            
	            //Write command to xml file
				out.writeObject(apiCommand);
			}
			
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(2);
		}
	}
}