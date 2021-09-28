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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class LibvirtStoragePoolXMLParser {
    private static final Logger s_logger = Logger.getLogger(LibvirtStoragePoolXMLParser.class);

    public LibvirtStoragePoolDef parseStoragePoolXML(String poolXML) {
        DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(poolXML));
            Document doc = builder.parse(is);

            Element rootElement = doc.getDocumentElement();
            String type = rootElement.getAttribute("type");

            String uuid = getTagValue("uuid", rootElement);

            String poolName = getTagValue("name", rootElement);

            Element source = (Element)rootElement.getElementsByTagName("source").item(0);
            String host = getAttrValue("host", "name", source);
            String format = getAttrValue("format", "type", source);

            if (type.equalsIgnoreCase("rbd") || type.equalsIgnoreCase("powerflex")) {
                int port = 0;
                String xmlPort = getAttrValue("host", "port", source);
                if (StringUtils.isNotBlank(xmlPort)) {
                    port = Integer.parseInt(xmlPort);
                }
                String pool = getTagValue("name", source);

                Element auth = (Element)source.getElementsByTagName("auth").item(0);

                if (auth != null) {
                    String authUsername = auth.getAttribute("username");
                    String authType = auth.getAttribute("type");
                    return new LibvirtStoragePoolDef(LibvirtStoragePoolDef.PoolType.valueOf(type.toUpperCase()), poolName, uuid, host, port, pool, authUsername,
                            LibvirtStoragePoolDef.AuthenticationType.valueOf(authType.toUpperCase()), uuid);
                } else {
                    return new LibvirtStoragePoolDef(LibvirtStoragePoolDef.PoolType.valueOf(type.toUpperCase()), poolName, uuid, host, port, pool, "");
                }
                /* Gluster is a sub-type of LibvirtStoragePoolDef.poolType.NETFS, need to check format */
            } else if (format != null && format.equalsIgnoreCase("glusterfs")) {
                /* libvirt does not return the default port, but requires it for a disk-definition */
                int port = 24007;

                String path = getAttrValue("dir", "path", source);

                Element target = (Element) rootElement.getElementsByTagName(
                        "target").item(0);
                String targetPath = getTagValue("path", target);

                String portValue = getAttrValue("host", "port", source);
                if (portValue != null && !portValue.isEmpty())
                    port = Integer.parseInt(portValue);

                return new LibvirtStoragePoolDef(LibvirtStoragePoolDef.PoolType.valueOf(format.toUpperCase()),
                        poolName, uuid, host, port, path, targetPath);
            } else {
                String path = getAttrValue("dir", "path", source);

                Element target = (Element)rootElement.getElementsByTagName("target").item(0);
                String targetPath = getTagValue("path", target);

                return new LibvirtStoragePoolDef(LibvirtStoragePoolDef.PoolType.valueOf(type.toUpperCase()), poolName, uuid, host, path, targetPath);
            }
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
