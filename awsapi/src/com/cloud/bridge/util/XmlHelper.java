// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.bridge.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class XmlHelper {
	protected static Logger logger = Logger.getLogger(XmlHelper.class);
	
	public static Document parse(String xmlContent) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(xmlContent.getBytes("UTF-8"));
		return parse(is);
	}
	
	public static Document parse(File file) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setCoalescing(true);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setNamespaceAware(true);
            DocumentBuilder parser = factory.newDocumentBuilder();
            return parser.parse(file);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        }
	}
	
    public static Document parse(InputStream is) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setCoalescing(true);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setNamespaceAware(true);
            DocumentBuilder parser = factory.newDocumentBuilder();
            InputSource in = new InputSource(is);
            return parser.parse(in);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }
	
	public static Document newDocument() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder builder;
		try {
			builder = dbf.newDocumentBuilder();
			Document document = builder.newDocument();
			return document;
		} catch (ParserConfigurationException e) {
			logger.error("Unexpected exception " + e.getMessage(), e);
		}
		
		return null;
	}
	
	public static Node getRootNode(Document doc) {
		NodeList l = doc.getChildNodes();
		if(l != null && l.getLength() == 1)
			return l.item(0);
		
		return null;
	}
	
	public static Node getChildNode(Node parentNode, String childElementName) {
		NodeList l = parentNode.getChildNodes();
		for(int i = 0; i < l.getLength(); i++) {
			Node node = l.item(i);
			if(node.getNodeName().equals(childElementName))
				return node;
		}
		return null;
	}
	
	public static String getChildNodeTextContent(Node parentNode, String childElementName) {
		Node node = getChildNode(parentNode, childElementName);
		if(node != null)
			return node.getTextContent();
		return null;
	}
	
	public static String getAttribute(Node node, String name) {
		NamedNodeMap attributes = node.getAttributes();
		Node attrNode = attributes.getNamedItem(name);
		if(attrNode != null)
			return attrNode.getNodeValue();
		return null;
	}
	
	public static String toXML(Node node) {
		if (node != null) {
			Transformer transformer = newTransformer();
			try {
				StringWriter sw = new StringWriter();
				transformer.transform(new DOMSource(node), new StreamResult(sw));
				return sw.toString();
			} catch (TransformerException e) {
				logger.error("Unexpected exception " + e.getMessage(), e);
			}
		}
		return StringHelper.EMPTY_STRING;
	}
	
	public static Transformer newTransformer() {
		return newTransformer("UTF-8", false);
	}
	
	public static Transformer newTransformer(String encoding, boolean indent) {
		try {
			Transformer transformer = TransformerFactory.newInstance()
					.newTransformer();
			Properties properties = transformer.getOutputProperties();
			
			properties.setProperty(OutputKeys.ENCODING, encoding);
			properties.setProperty(OutputKeys.METHOD, "XML");
			properties.setProperty(OutputKeys.VERSION, "1.0");
			if(indent)
				properties.setProperty(OutputKeys.INDENT, "YES");
			else
				properties.setProperty(OutputKeys.INDENT, "NO");
			transformer.setOutputProperties(properties);
			return transformer;
		} catch (TransformerConfigurationException e) {
			logger.error("Unexpected exception " + e.getMessage(), e);
		}
		
		return null;
	}	
}
