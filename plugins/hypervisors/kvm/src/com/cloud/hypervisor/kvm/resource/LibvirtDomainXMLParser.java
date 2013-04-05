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
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef.nicModel;

public class LibvirtDomainXMLParser {
    private static final Logger s_logger = Logger
            .getLogger(LibvirtDomainXMLParser.class);
    private final List<InterfaceDef> interfaces = new ArrayList<InterfaceDef>();
    private final List<DiskDef> diskDefs = new ArrayList<DiskDef>();
    private Integer vncPort;
    private String desc;

    public boolean parseDomainXML(String domXML) {
        DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(domXML));
            Document doc = builder.parse(is);

            Element rootElement = doc.getDocumentElement();

            desc = getTagValue("description", rootElement);

            Element devices = (Element) rootElement.getElementsByTagName(
                    "devices").item(0);
            NodeList disks = devices.getElementsByTagName("disk");
            for (int i = 0; i < disks.getLength(); i++) {
                Element disk = (Element) disks.item(i);
                String diskFmtType = getAttrValue("driver", "type", disk);
                String diskFile = getAttrValue("source", "file", disk);
                String diskDev = getAttrValue("source", "dev", disk);

                String diskLabel = getAttrValue("target", "dev", disk);
                String bus = getAttrValue("target", "bus", disk);
                String type = disk.getAttribute("type");
                String device = disk.getAttribute("device");

                DiskDef def = new DiskDef();
                if (type.equalsIgnoreCase("file")) {
                    if (device.equalsIgnoreCase("disk")) {
                        DiskDef.diskFmtType fmt = null;
                        if (diskFmtType != null) {
                            fmt = DiskDef.diskFmtType.valueOf(diskFmtType
                                    .toUpperCase());
                        }
                        def.defFileBasedDisk(diskFile, diskLabel,
                                DiskDef.diskBus.valueOf(bus.toUpperCase()), fmt);
                    } else if (device.equalsIgnoreCase("cdrom")) {
                        def.defISODisk(diskFile);
                    }
                } else if (type.equalsIgnoreCase("block")) {
                    def.defBlockBasedDisk(diskDev, diskLabel,
                            DiskDef.diskBus.valueOf(bus.toUpperCase()));
                }
                diskDefs.add(def);
            }

            NodeList nics = devices.getElementsByTagName("interface");
            for (int i = 0; i < nics.getLength(); i++) {
                Element nic = (Element) nics.item(i);

                String type = nic.getAttribute("type");
                String mac = getAttrValue("mac", "address", nic);
                String dev = getAttrValue("target", "dev", nic);
                String model = getAttrValue("model", "type", nic);
                InterfaceDef def = new InterfaceDef();

                if (type.equalsIgnoreCase("network")) {
                    String network = getAttrValue("source", "network", nic);
                    def.defPrivateNet(network, dev, mac,
                            nicModel.valueOf(model.toUpperCase()));
                } else if (type.equalsIgnoreCase("bridge")) {
                    String bridge = getAttrValue("source", "bridge", nic);
                    def.defBridgeNet(bridge, dev, mac,
                            nicModel.valueOf(model.toUpperCase()));
                } else if (type.equalsIgnoreCase("ethernet"))  {
                    String scriptPath = getAttrValue("script", "path", nic);
                    def.defEthernet(dev, mac, nicModel.valueOf(model.toUpperCase()), scriptPath);
                }
                interfaces.add(def);
            }

            Element graphic = (Element) devices
                    .getElementsByTagName("graphics").item(0);

            if (graphic != null) {
                String port = graphic.getAttribute("port");
                if (port != null) {
                    try {
                        vncPort = Integer.parseInt(port);
                        if (vncPort != -1) {
                            vncPort = vncPort - 5900;
                        } else {
                            vncPort = null;
                        }
                    } catch (NumberFormatException nfe) {
                        vncPort = null;
                    }
                }
            }

            return true;
        } catch (ParserConfigurationException e) {
            s_logger.debug(e.toString());
        } catch (SAXException e) {
            s_logger.debug(e.toString());
        } catch (IOException e) {
            s_logger.debug(e.toString());
        }
        return false;
    }

    private static String getTagValue(String tag, Element eElement) {
        NodeList tagNodeList = eElement.getElementsByTagName(tag);
        if (tagNodeList == null || tagNodeList.getLength() == 0) {
            return null;
        }

        NodeList nlList = tagNodeList.item(0).getChildNodes();

        Node nValue = (Node) nlList.item(0);

        return nValue.getNodeValue();
    }

    private static String getAttrValue(String tag, String attr, Element eElement) {
        NodeList tagNode = eElement.getElementsByTagName(tag);
        if (tagNode.getLength() == 0) {
            return null;
        }
        Element node = (Element) tagNode.item(0);
        return node.getAttribute(attr);
    }

    public Integer getVncPort() {
        return vncPort;
    }

    public List<InterfaceDef> getInterfaces() {
        return interfaces;
    }

    public List<DiskDef> getDisks() {
        return diskDefs;
    }

    public String getDescription() {
        return desc;
    }
}
