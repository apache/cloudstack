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

package com.cloud.utils.commandlinetool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.cloud.utils.Pair;

public class BuildCommandLineInputFile {
	private static Properties api_commands = new Properties();
	private static String dirName="";
	
	public static void main (String[] args) {
		Properties preProcessedCommands = new Properties();
		Class clas = null;
		Enumeration e = null;
		String[] fileNames = null;
		
		//load properties	
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
		
		
		for (Object key : preProcessedCommands.keySet()) {
            String preProcessedCommand = preProcessedCommands.getProperty((String)key);
            String[] commandParts = preProcessedCommand.split(";");
            api_commands.put(key, commandParts[0]);
		}
		
		
		e = api_commands.propertyNames();
		
		try {
			DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element root = doc.createElement("commands");
	        doc.appendChild(root);
		
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				try {
					clas = Class.forName(api_commands.getProperty(key));
					Element child1 = doc.createElement("command");
					root.appendChild(child1);
					Element child2 = doc.createElement("name");
					child1.appendChild(child2);
					Text text = doc.createTextNode(key);
		            child2.appendChild(text);
		            
					Field m[] = clas.getDeclaredFields();
		            for (int i = 0; i < m.length; i++) {
		            	if (m[i].getName().endsWith("s_properties")) {
		            		m[i].setAccessible(true);
		            		List<Pair<Enum, Boolean>> properties = (List<Pair<Enum, Boolean>>) m[i].get(null);
		            		for (Pair property : properties){
		            			if (!property.first().toString().equals("ACCOUNT_OBJ") && !property.first().toString().equals("USER_ID")){
		            				Element child3 = doc.createElement("arg");
			            			child1.appendChild(child3);
			            			Class clas2 = property.first().getClass();
			            			Method m2 = clas2.getMethod("getName");
			            			text = doc.createTextNode(m2.invoke(property.first()).toString());
			            			child3.appendChild(text);
			            			child3.setAttribute("required", property.second().toString());
		            			}
		            		}		
		            	}
		            }
				} catch (ClassNotFoundException ex2) {
					System.out.println("Can't find class " + api_commands.getProperty(key));
					System.exit(2);
				}
			}
	        TransformerFactory transfac = TransformerFactory.newInstance();
	        Transformer trans = transfac.newTransformer();
	        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	        trans.setOutputProperty(OutputKeys.INDENT, "yes");
	
	        StringWriter sw = new StringWriter();
	        StreamResult result = new StreamResult(sw);
	        DOMSource source = new DOMSource(doc);
	        trans.transform(source, result);
	        String xmlString = sw.toString();
	
	        //write xml to file         
	        File f=new File(dirName + "/commands.xml");
	        Writer output = new BufferedWriter(new FileWriter(f));
	        output.write(xmlString);
	        output.close();
		} catch (Exception ex) {
			System.out.println(ex);
			System.exit(2);
		}
	}

}