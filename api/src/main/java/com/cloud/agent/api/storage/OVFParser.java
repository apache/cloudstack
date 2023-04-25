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
package com.cloud.agent.api.storage;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.cloudstack.utils.security.ParserUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class OVFParser {
    private static final Logger s_logger = Logger.getLogger(OVFParser.class);

    private static final String DEFAULT_OVF_SCHEMA = "http://schemas.dmtf.org/ovf/envelope/1";
    private static final String VMW_SCHEMA = "http://www.vmware.com/schema/ovf";

    private static final Map<String, String> ATTRIBUTE_SCHEMA_MAP = Map.of(
            "osType", VMW_SCHEMA
    );

    private DocumentBuilder documentBuilder;

    public OVFParser() {
        try {
            DocumentBuilderFactory documentBuilderFactory = ParserUtils.getSaferDocumentBuilderFactory();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            s_logger.error("Cannot start the OVF parser: " + e.getMessage(), e);
        }
    }

    public Document parseOVF(String ovfString) throws IOException, SAXException {
        InputSource is = new InputSource(new StringReader(ovfString));
        return documentBuilder.parse(is);
    }

    public Document parseOVFFile(String ovfFilePath) {
        if (StringUtils.isBlank(ovfFilePath)) {
            return null;
        }
        try {
            return documentBuilder.parse(new File(ovfFilePath));
        } catch (SAXException | IOException e) {
            s_logger.error("Error parsing " + ovfFilePath + " " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Retrieve elements with tag name from the document, according to the OVF schema definition
     */
    public NodeList getElementsFromOVFDocument(Document doc, String tagName) {
        return doc != null ? doc.getElementsByTagNameNS(DEFAULT_OVF_SCHEMA, tagName) : null;
    }

    /**
     * Retrieve an attribute value from an OVF element
     */
    public String getNodeAttribute(Element element, String attr) {
        return element != null ? element.getAttributeNS(ATTRIBUTE_SCHEMA_MAP.getOrDefault(attr, DEFAULT_OVF_SCHEMA), attr) : null;
    }

    /**
     * Get the text value of a node's child with name or suffix "childNodeName", null if not present
     * Example:
     * <Node>
     *    <childNodeName>Text value</childNodeName>
     *    <rasd:childNodeName>Text value</rasd:childNodeName>
     * </Node>
     */
    public String getChildNodeValue(Node node, String childNodeName) {
        if (node == null || !node.hasChildNodes()) {
            return null;
        }
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node value = childNodes.item(i);
            // Also match if the child's name has a suffix:
            // Example: <rasd:AllocationUnits>
            if (value != null && (value.getNodeName().equals(childNodeName) || value.getNodeName().endsWith(":" + childNodeName))) {
                return value.getTextContent();
            }
        }
        return null;
    }
}
