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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.BaseResponse;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.xstream.XStream;

public class ApiXmlDocWriter {
    public static final Logger s_logger = Logger.getLogger(ApiXmlDocWriter.class.getName());
    
    private static final short DOMAIN_ADMIN_COMMAND = 2;
    private static final short USER_COMMAND = 8;
    private static LinkedHashMap<Object, String> all_api_commands = new LinkedHashMap<Object, String>();
    private static LinkedHashMap<Object, String> domain_admin_api_commands = new LinkedHashMap<Object, String>();
    private static LinkedHashMap<Object, String> regular_user_api_commands = new LinkedHashMap<Object, String>();
	private static TreeMap<Object, String> all_api_commands_sorted = new TreeMap<Object, String>();
	private static TreeMap<Object, String> domain_admin_api_commands_sorted = new TreeMap<Object, String>();
	private static TreeMap<Object, String> regular_user_api_commands_sorted = new TreeMap<Object, String>();
	private static String dirName="";
	
	public static void main (String[] args) {
	    LinkedProperties preProcessedCommands = new LinkedProperties();
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
			System.out.println("Please specify input file(s) separated by coma using -f option");
			System.exit(2);
		}
		
		for (String fileName : fileNames) {
			try {
				FileInputStream in = new FileInputStream(fileName);
				preProcessedCommands.load(in);
			}catch (FileNotFoundException ex) {
			    System.out.println("Can't find file " + fileName);
				System.exit(2);
			} catch (IOException ex1) {
			    System.out.println("Error reading from file " + ex1);
				System.exit(2);
			}
		}
		
		Iterator<?> propertiesIterator = preProcessedCommands.keys.iterator();
		//Get command classes and response object classes
		while (propertiesIterator.hasNext()) {
		    String key = (String)propertiesIterator.next();
            String preProcessedCommand = preProcessedCommands.getProperty(key);
            String[] commandParts = preProcessedCommand.split(";");
            String commandName = commandParts[0];
            all_api_commands.put(key, commandName);
            
            short cmdPermissions = 1;
            if (commandParts.length > 1 && commandParts[1] != null) {
                cmdPermissions = Short.parseShort(commandParts[1]);
            } 
           
            if ((cmdPermissions & DOMAIN_ADMIN_COMMAND) != 0) {
                domain_admin_api_commands.put(key, commandName);
            }
            if ((cmdPermissions & USER_COMMAND) != 0) {
                regular_user_api_commands.put(key, commandName);
            }
		}
		
		all_api_commands_sorted.putAll(all_api_commands);
		domain_admin_api_commands_sorted.putAll(domain_admin_api_commands);
		regular_user_api_commands_sorted.putAll(regular_user_api_commands);
		
		try {
			//Create object writer
			XStream xs = new XStream();
			xs.alias("command", Command.class);
			xs.alias("arg", Argument.class);
			String xmlDocDir =  dirName + "/xmldoc";
			String rootAdminDirName = xmlDocDir + "/root_admin";
			String domainAdminDirName = xmlDocDir + "/domain_admin";
			String regularUserDirName = xmlDocDir + "/regular_user";
			(new File(rootAdminDirName)).mkdirs();
			(new File(domainAdminDirName)).mkdirs();
			(new File(regularUserDirName)).mkdirs();

			ObjectOutputStream out = xs.createObjectOutputStream(new FileWriter(dirName + "/commands.xml"), "commands");
			ObjectOutputStream rootAdmin = xs.createObjectOutputStream(new FileWriter(rootAdminDirName + "/" + "rootAdminSummary.xml"), "commands");
			ObjectOutputStream rootAdminSorted = xs.createObjectOutputStream(new FileWriter(rootAdminDirName + "/" + "rootAdminSummarySorted.xml"), "commands");
			ObjectOutputStream domainAdmin = xs.createObjectOutputStream(new FileWriter(domainAdminDirName + "/" + "domainAdminSummary.xml"), "commands");
			ObjectOutputStream outDomainAdminSorted = xs.createObjectOutputStream(new FileWriter(domainAdminDirName + "/" + "domainAdminSummarySorted.xml"), "commands");
			ObjectOutputStream regularUser = xs.createObjectOutputStream(new FileWriter(regularUserDirName + "/regularUserSummary.xml"), "commands");
			ObjectOutputStream regularUserSorted = xs.createObjectOutputStream(new FileWriter(regularUserDirName + "/regularUserSummarySorted.xml"), "commands");
	
			//Write commands in the order they are represented in commands.properties.in file
			Iterator<?> it = all_api_commands.keySet().iterator();
			while (it.hasNext()) {	  
				String key = (String)it.next(); 
				
	            //Write admin commands
				writeCommand(out, key);
				writeCommand(rootAdmin, key);	

				//Write single commands to separate xml files
				ObjectOutputStream singleRootAdminCommandOs = xs.createObjectOutputStream(new FileWriter(rootAdminDirName + "/" + key + ".xml"), "command");
				writeCommand(singleRootAdminCommandOs, key);
				singleRootAdminCommandOs.close();
				
				if (domain_admin_api_commands.containsKey(key)){
				    writeCommand(domainAdmin, key);
				    ObjectOutputStream singleDomainAdminCommandOs = xs.createObjectOutputStream(new FileWriter(domainAdminDirName + "/" + key + ".xml"), "command");
				    writeCommand(singleDomainAdminCommandOs, key);
				    singleDomainAdminCommandOs.close();
				}
				
				if (regular_user_api_commands.containsKey(key)){
				    writeCommand(regularUser, key);
				    ObjectOutputStream singleRegularUserCommandOs = xs.createObjectOutputStream(new FileWriter(regularUserDirName + "/" + key + ".xml"), "command");
				    writeCommand(singleRegularUserCommandOs, key);
				    singleRegularUserCommandOs.close();
				}
			}
			
			//Write sorted commands
			it = all_api_commands_sorted.keySet().iterator();
			while (it.hasNext()) {     
                String key = (String)it.next(); 
                writeCommand(rootAdminSorted, key);
                

                if (domain_admin_api_commands.containsKey(key)){
                    writeCommand(outDomainAdminSorted, key);    
                }
                
                if (regular_user_api_commands.containsKey(key)){
                    writeCommand(regularUserSorted, key);
                }
            }
			
			out.close();
			rootAdmin.close();
			rootAdminSorted.close();
			domainAdmin.close();
			outDomainAdminSorted.close();
			regularUser.close();
			regularUserSorted.close();
			
			//gzip directory with xml doc
			//zipDir(dirName + "xmldoc.zip", xmlDocDir);
			
			//Delete directory
			//deleteDir(new File(xmlDocDir));
			
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(2);
		} 
	}
	
	
	private static void writeCommand(ObjectOutputStream out, String command) throws ClassNotFoundException, IOException{
	    Class<?> clas = Class.forName(all_api_commands.get(command));
	    ArrayList<Argument> request = new ArrayList<Argument>();
        ArrayList<Argument> response = new ArrayList<Argument>();
        
        //Create a new command, set name and description
        Command apiCommand = new Command();
        apiCommand.setName(command);
        
        Implementation impl = (Implementation)clas.getAnnotation(Implementation.class);
        if (impl == null)
            impl = (Implementation)clas.getSuperclass().getAnnotation(Implementation.class);
        String commandDescription = impl.description();
        if (commandDescription != null)
            apiCommand.setDescription(commandDescription);
        else
            System.out.println("Command " + apiCommand.getName() + " misses description");
        
        //Set request parameters   
        Field[] fields = clas.getDeclaredFields();
        
        //Get fields from superclass
        Class<?> superClass = clas.getSuperclass();
        String superName = superClass.getName();
        if (!superName.equals(BaseCmd.class.getName()) && !superName.equals(BaseAsyncCmd.class.getName()) && !superName.equals(BaseAsyncCreateCmd.class.getName())) {
            Field[] superClassFields = superClass.getDeclaredFields();
            if (superClassFields != null && !superClass.getName().equals(BaseListCmd.class.getName())) {
                Field[] tmpFields = new Field[fields.length + superClassFields.length];
                System.arraycopy(fields, 0, tmpFields, 0, fields.length);
                System.arraycopy(superClassFields, 0, tmpFields, fields.length, superClassFields.length);
                fields = tmpFields;
            }
            superClass = superClass.getSuperclass();
        }
        request = setRequestFields(fields);
        
        
        //Get response parameters
        Class<?> responseClas = impl.responseObject();
        Field[] responseFields = responseClas.getDeclaredFields();
        response = setResponseFields(responseFields);
        
        apiCommand.setRequest(request);
        apiCommand.setResponse(response);
        
        out.writeObject(apiCommand);
	}
	
	
	private static ArrayList<Argument> setRequestFields(Field[] fields) {
	    ArrayList<Argument> arguments = new ArrayList<Argument>();
	    for (Field f : fields) {
            Parameter parameterAnnotation = f.getAnnotation(Parameter.class);
            if (parameterAnnotation != null && parameterAnnotation.expose()) {
                Argument reqArg = new Argument(parameterAnnotation.name());
                reqArg.setRequired(parameterAnnotation.required());
                if (!parameterAnnotation.description().isEmpty()) {
                    reqArg.setDescription(parameterAnnotation.description());
                }   
                arguments.add(reqArg);
            }
        } 
	    return arguments;
	}
	
	private static ArrayList<Argument> setResponseFields(Field[] responseFields) {
	    ArrayList<Argument> arguments = new ArrayList<Argument>();
	    for (Field responseField : responseFields) {    
            SerializedName nameAnnotation = responseField.getAnnotation(SerializedName.class);
            Param paramAnnotation = responseField.getAnnotation(Param.class);
            Argument respArg = new Argument(nameAnnotation.value());   
            boolean toExpose = true;
            if (paramAnnotation != null) {
                String description = paramAnnotation.description();
                Class fieldClass = paramAnnotation.responseObject();
                toExpose = paramAnnotation.expose();
                if (description != null && !description.isEmpty()) {
                    respArg.setDescription(description);
                } 
                
                if (fieldClass != null) {
                    Class<?> superClass = fieldClass.getSuperclass();
                    if (superClass != null) {
                        String superName = superClass.getName();
                        if (superName.equals(BaseResponse.class.getName())) {
                            ArrayList<Argument> fieldArguments = new ArrayList<Argument>();
                            Field[] fields = fieldClass.getDeclaredFields();
                            fieldArguments = setResponseFields(fields);
                            respArg.setArguments(fieldArguments);
                        }
                    }
                }
            } 
                
            if (toExpose) {
                arguments.add(respArg);
            } 
            
            
        }
	    return arguments;
	}
	
	private static void zipDir(String zipFileName, String dir) throws Exception {
	    File dirObj = new File(dir);
	    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
	    addDir(dirObj, out);
	    out.close();
	  }

	  static void addDir(File dirObj, ZipOutputStream out) throws IOException {
	    File[] files = dirObj.listFiles();
	    byte[] tmpBuf = new byte[1024];
	    String pathToDir = dirName;

	    for (int i = 0; i < files.length; i++) {
	      if (files[i].isDirectory()) {
	        addDir(files[i], out);
	        continue;
	      }
	      FileInputStream in = new FileInputStream(files[i].getPath());
	      out.putNextEntry(new ZipEntry(files[i].getPath().substring(pathToDir.length())));
	      int len;
	      while ((len = in.read(tmpBuf)) > 0) {
	        out.write(tmpBuf, 0, len);
	      }
	      out.closeEntry();
	      in.close();
	    }
	  }
	
	private static void deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                deleteDir(new File(dir, children[i]));
            }
        }
        dir.delete();
	 }
	
	
	private static class LinkedProperties extends Properties {
	    private final LinkedList<Object> keys = new LinkedList<Object>();

	    public Enumeration<Object> keys() {
	        return Collections.<Object>enumeration(keys);
	    }

	    public Object put(Object key, Object value) {
	        //System.out.println("Adding key" + key);
	        keys.add(key);
	        return super.put(key, value);
	    }
	}

}