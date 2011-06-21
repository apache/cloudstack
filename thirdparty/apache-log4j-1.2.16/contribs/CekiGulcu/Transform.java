/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.log4j.xml;

import org.apache.log4j.Category;
import org.apache.log4j.Layout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.helpers.DateLayout;

import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.SAXException;
import org.apache.xerces.parsers.SAXParser;

import org.apache.trax.Processor;
import org.apache.trax.TemplatesBuilder;
import org.apache.trax.Templates;
import org.apache.trax.Transformer;
import org.apache.trax.Result;
import org.apache.trax.ProcessorException; 
import org.apache.trax.ProcessorFactoryException;
import org.apache.trax.TransformException; 


import org.apache.serialize.SerializerFactory;
import org.apache.serialize.Serializer;
import org.apache.serialize.OutputFormat;
import org.xml.sax.helpers.AttributesImpl;


import java.io.FileOutputStream;
import java.io.IOException;


public class Transform {

  public static void main(String[] args) throws Exception {
    PropertyConfigurator.disableAll();
    PropertyConfigurator.configure("x.lcf");

    // I. Instantiate  a stylesheet processor.
    Processor processor = Processor.newInstance("xslt");

    // II. Process the stylesheet. producing a Templates object.

    // Get the XMLReader.
    XMLReader reader = XMLReaderFactory.createXMLReader();

    // Set the ContentHandler.
    TemplatesBuilder templatesBuilder = processor.getTemplatesBuilder();
    reader.setContentHandler(templatesBuilder);

    // Set the ContentHandler to also function as a LexicalHandler, which
    // includes "lexical" (e.g., comments and CDATA) events. The Xalan
    // TemplatesBuilder -- org.apache.xalan.processor.StylesheetHandler -- is
    // also a LexicalHandler).
    if(templatesBuilder instanceof LexicalHandler) {
       reader.setProperty("http://xml.org/sax/properties/lexical-handler", 
                           templatesBuilder);
    }

    // Parse the stylesheet.                       
    reader.parse(args[0]);

    //Get the Templates object from the ContentHandler.
    Templates templates = templatesBuilder.getTemplates();

    // III. Use the Templates object to instantiate a Transformer.
    Transformer transformer = templates.newTransformer();

    // IV. Perform the transformation.

    // Set up the ContentHandler for the output.
	FileOutputStream fos = new FileOutputStream(args[2]);
    Result result = new Result(fos);
    Serializer serializer = SerializerFactory.getSerializer("xml");
    serializer.setOutputStream(fos);

    transformer.setContentHandler(serializer.asContentHandler());

    // Set up the ContentHandler for the input.
    org.xml.sax.ContentHandler chandler = transformer.getInputContentHandler();
    DC dc = new DC(chandler);
    reader.setContentHandler(dc);
    if(chandler instanceof LexicalHandler) {
       reader.setProperty("http://xml.org/sax/properties/lexical-handler", 
			  chandler);
    } else {
       reader.setProperty("http://xml.org/sax/properties/lexical-handler", 
			  null);
    }

    // Parse the XML input document. The input ContentHandler and
    // output ContentHandler work in separate threads to optimize
    // performance.
    reader.parse(args[1]);
  }	
}

 class DC implements ContentHandler {

   static Category cat = Category.getInstance("DC");

   ContentHandler  chandler;

   DC(ContentHandler chandler) {
     this.chandler = chandler;
   }


  public 
  void characters(char[] ch, int start, int length) 
                            throws org.xml.sax.SAXException {
    cat.debug("characters: ["+new String(ch, start, length)+ "] called");
    chandler.characters(ch, start, length);

  }

  public 
  void endDocument() throws org.xml.sax.SAXException {
    cat.debug("endDocument called.");
    chandler.endDocument();

  }

  public 
  void endElement(String namespaceURI, String localName, String qName)
                                           throws org.xml.sax.SAXException {
    cat.debug("endElement("+namespaceURI+", "+localName+", "+qName+") called");
    chandler.endElement(namespaceURI, localName, qName);
  }
   
   public
   void endPrefixMapping(String prefix) throws org.xml.sax.SAXException {
     cat.debug("endPrefixMapping("+prefix+") called");
     chandler.endPrefixMapping(prefix);
   }

  public 
  void ignorableWhitespace(char[] ch, int start, int length) 
                                     throws org.xml.sax.SAXException {
    cat.debug("ignorableWhitespace called");
    chandler.ignorableWhitespace(ch, start, length);
  }
  
  public 
  void processingInstruction(java.lang.String target, java.lang.String data) 
                                              throws org.xml.sax.SAXException {
    cat.debug("processingInstruction called");
    chandler.processingInstruction(target, data);
  }

  public 
  void setDocumentLocator(Locator locator)  {
    cat.debug("setDocumentLocator called");
    chandler.setDocumentLocator(locator);
  }

   public
   void skippedEntity(String name) throws org.xml.sax.SAXException {
     cat.debug("skippedEntity("+name+")  called");
     chandler.skippedEntity(name);     
   }
  
  public 
  void startDocument() throws org.xml.sax.SAXException {
    cat.debug("startDocument called");
    chandler.startDocument();
  }
  
  public 
  void startElement(String namespaceURI, String localName, String qName,
		    Attributes atts) throws org.xml.sax.SAXException {
    cat.debug("startElement("+namespaceURI+", "+localName+", "+qName+")called");

    if("log4j:event".equals(qName)) {
      cat.debug("-------------");      
      if(atts instanceof org.xml.sax.helpers.AttributesImpl) {
	AttributesImpl ai = (AttributesImpl) atts;
	int i = atts.getIndex("timestamp");
	ai.setValue(i, "hello");
      }
      String ts = atts.getValue("timestamp");
      cat.debug("New timestamp is " + ts);
    }
    chandler.startElement(namespaceURI, localName, qName, atts);
  }

   public
   void startPrefixMapping(String prefix, String uri) 
                                          throws org.xml.sax.SAXException {
     cat.debug("startPrefixMapping("+prefix+", "+uri+") called");     
     chandler.startPrefixMapping(prefix, uri);
   }
           
   
}

