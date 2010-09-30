package com.cloud.api;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
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

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class ApiXmlDocBuilder {
	private static final Logger s_logger = Logger.getLogger(ApiXmlDocBuilder.class.getName());
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
				s_logger.error("Can't find file " + fileName);
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
					//Get command name and description
					clas = Class.forName(api_commands.getProperty(key));
					Element commandElement = doc.createElement("command");
					root.appendChild(commandElement);
					Element nameElement = doc.createElement("name");
					commandElement.appendChild(nameElement);
					Text text = doc.createTextNode(key);
		            nameElement.appendChild(text);
		            
		            Annotation[] classAnnotations = clas.getAnnotations();
		            for (Annotation a : classAnnotations) {
		            	Class classAnnotation = a.annotationType();
		            	Method descriptionMethod = classAnnotation.getMethod("description");
		            	nameElement.setAttribute("description", descriptionMethod.invoke(a).toString());
		            }
		            //Get input parameters
		            Element parameter = doc.createElement("arguments");
		            commandElement.appendChild(parameter);
		            
		            Field[] fields = clas.getDeclaredFields();
		          
					for (Field f : fields) {
						Parameter parameterAnnotation = f.getAnnotation(Parameter.class);
						Element argElement = doc.createElement("arg");
						parameter.appendChild(argElement);
						text = doc.createTextNode(parameterAnnotation.name());
						argElement.appendChild(text);
						argElement.setAttribute("required", String.valueOf(parameterAnnotation.required()));
						argElement.setAttribute("description", parameterAnnotation.description());
					}
					
					//Get response parameters
					Element response = doc.createElement("response");
					commandElement.appendChild(response);
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
			System.out.println("Exception" + ex);
			System.exit(2);
		}
	}
}