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
package com.cloud.hypervisor.kvm.resource;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class LibvirtStorageVolumeXMLParser {
    private static final Logger s_logger = Logger.getLogger(LibvirtStorageVolumeXMLParser.class);

    public LibvirtStorageVolumeDef parseStorageVolumeXML(String volXML) {
        DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(volXML));
            Document doc = builder.parse(is);

            Element rootElement = doc.getDocumentElement();

            String VolName = getTagValue("name", rootElement);
            Element target = (Element)rootElement.getElementsByTagName("target").item(0);
            String format = getAttrValue("type", "format", target);
            Long capacity = Long.parseLong(getTagValue("capacity", rootElement));
            return new LibvirtStorageVolumeDef(VolName, capacity, LibvirtStorageVolumeDef.VolumeFormat.getFormat(format), null, null);
        } catch (ParserConfigurationException e) {
            s_logger.debug(e.toString());
        } catch (SAXException e) {
            s_logger.debug(e.toString());
        } catch (IOException e) {
            s_logger.debug(e.toString());
        }
        return null;
    }

    private static String getTagValue(String tag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(tag).item(0).getChildNodes();
        Node nValue = nlList.item(0);

        return nValue.getNodeValue();
    }

    private static String getAttrValue(String tag, String attr, Element eElement) {
        NodeList tagNode = eElement.getElementsByTagName(tag);
        if (tagNode.getLength() == 0) {
            return null;
        }
        Element node = (Element)tagNode.item(0);
        return node.getAttribute(attr);
    }
}
