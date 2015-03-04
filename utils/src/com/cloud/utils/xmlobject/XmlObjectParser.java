//
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
//

package com.cloud.utils.xmlobject;

import com.cloud.utils.exception.CloudRuntimeException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Stack;

public class XmlObjectParser {
    final private InputStream is;

    private class XmlHandler extends DefaultHandler {
        private Stack<XmlObject> stack;
        private String currentValue;
        private XmlObject root;

        XmlHandler() {
            stack = new Stack<XmlObject>();
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            //System.out.println(String.format("startElement: namespaceURI:%s, localName:%s, qName:%s", namespaceURI, localName, qName));
            currentValue = null;
            XmlObject obj = new XmlObject();
            for (int i = 0; i < atts.getLength(); i++) {
                obj.putElement(atts.getQName(i), atts.getValue(i));
            }
            obj.setTag(qName);
            if (!stack.isEmpty()) {
                XmlObject parent = stack.peek();
                parent.putElement(qName, obj);
            }
            stack.push(obj);
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            XmlObject currObj = stack.pop();
            if (currentValue != null) {
                currObj.setText(currentValue);
            }

            if (stack.isEmpty()) {
                root = currObj;
            }

            //System.out.println(String.format("endElement: namespaceURI:%s, localName:%s, qName:%s", namespaceURI, localName, qName));
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            StringBuilder str = new StringBuilder();
            str.append(ch, start, length);
            currentValue = str.toString();
            //System.out.println(String.format("characters: %s", str.toString()));
        }

        XmlObject getRoot() {
            return root;
        }
    }

    private XmlObjectParser(InputStream is) {
        super();
        this.is = is;
    }

    public static XmlObject parseFromFile(String filePath) {
        FileInputStream fs;
        try {
            fs = new FileInputStream(new File(filePath));
            XmlObjectParser p = new XmlObjectParser(fs);
            return p.parse();
        } catch (FileNotFoundException e) {
            throw new CloudRuntimeException(e);
        }
    }

    public static XmlObject parseFromString(String xmlString) {
        InputStream stream = new ByteArrayInputStream(xmlString.getBytes());
        XmlObjectParser p = new XmlObjectParser(stream);
        XmlObject obj = p.parse();
        if (obj.getText() != null && obj.getText().replaceAll("\\n", "").replaceAll("\\r", "").replaceAll(" ", "").isEmpty()) {
            obj.setText(null);
        }
        return obj;
    }

    private XmlObject parse() {
        SAXParserFactory spfactory = SAXParserFactory.newInstance();
        try {
            SAXParser saxParser = spfactory.newSAXParser();
            XmlHandler handler = new XmlHandler();
            saxParser.parse(is, handler);
            return handler.getRoot();
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
