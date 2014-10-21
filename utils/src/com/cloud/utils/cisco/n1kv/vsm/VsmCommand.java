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

package com.cloud.utils.cisco.n1kv.vsm;

import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import com.cloud.utils.Pair;

public class VsmCommand {

    private static final Logger s_logger = Logger.getLogger(VsmCommand.class);
    private static final String s_namespace = "urn:ietf:params:xml:ns:netconf:base:1.0";
    private static final String s_ciscons = "http://www.cisco.com/nxos:1.0:ppm";
    private static final String s_configuremode = "__XML__MODE__exec_configure";
    private static final String s_portprofmode = "__XML__MODE_port-prof";
    private static final String s_policymapmode = "__XML__MODE_policy-map";
    private static final String s_classtypemode = "__XML__MODE_policy-map_class_type";
    private static final String s_paramvalue = "__XML__PARAM_value";

    public enum PortProfileType {
        none, vethernet, ethernet;
    }

    public enum BindingType {
        none, portbindingstatic, portbindingdynamic, portbindingephermal;
    }

    public enum SwitchPortMode {
        none, access, trunk, privatevlanhost, privatevlanpromiscuous
    }

    public enum OperationType {
        addvlanid, removevlanid
    }

    public static String getAddPortProfile(String name, PortProfileType type, BindingType binding, SwitchPortMode mode, int vlanid, String vdc, String espName) {
        try {
            // Create the document and root element.
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            DOMImplementation domImpl = docBuilder.getDOMImplementation();
            Document doc = createDocument(domImpl);

            // Edit configuration command.
            Element editConfig = doc.createElement("nf:edit-config");
            doc.getDocumentElement().appendChild(editConfig);

            // Command to get into exec configure mode.
            Element target = doc.createElement("nf:target");
            Element running = doc.createElement("nf:running");
            target.appendChild(running);
            editConfig.appendChild(target);

            // Command to create the port profile with the desired configuration.
            Element config = doc.createElement("nf:config");
            config.appendChild(configPortProfileDetails(doc, name, type, binding, mode, vlanid, vdc, espName));
            editConfig.appendChild(config);

            return serialize(domImpl, doc);
        } catch (ParserConfigurationException e) {
            s_logger.error("Error while creating add port profile message : " + e.getMessage());
            return null;
        } catch (DOMException e) {
            s_logger.error("Error while creating add port profile message : " + e.getMessage());
            return null;
        }
    }

    public static String getAddPortProfile(String name, PortProfileType type, BindingType binding, SwitchPortMode mode, int vlanid) {
        try {
            // Create the document and root element.
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            DOMImplementation domImpl = docBuilder.getDOMImplementation();
            Document doc = createDocument(domImpl);

            // Edit configuration command.
            Element editConfig = doc.createElement("nf:edit-config");
            doc.getDocumentElement().appendChild(editConfig);

            // Command to get into exec configure mode.
            Element target = doc.createElement("nf:target");
            Element running = doc.createElement("nf:running");
            target.appendChild(running);
            editConfig.appendChild(target);

            // Command to create the port profile with the desired configuration.
            Element config = doc.createElement("nf:config");
            config.appendChild(configPortProfileDetails(doc, name, type, binding, mode, vlanid));
            editConfig.appendChild(config);

            return serialize(domImpl, doc);
        } catch (ParserConfigurationException e) {
            s_logger.error("Error while creating add port profile message : " + e.getMessage());
            return null;
        } catch (DOMException e) {
            s_logger.error("Error while creating add port profile message : " + e.getMessage());
            return null;
        }
    }

    public static String getUpdatePortProfile(String name, SwitchPortMode mode, List<Pair<VsmCommand.OperationType, String>> params) {
        try {
            // Create the document and root element.
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            DOMImplementation domImpl = docBuilder.getDOMImplementation();
            Document doc = createDocument(domImpl);

            // Edit configuration command.
            Element editConfig = doc.createElement("nf:edit-config");
            doc.getDocumentElement().appendChild(editConfig);

            // Command to get into exec configure mode.
            Element target = doc.createElement("nf:target");
            Element running = doc.createElement("nf:running");
            target.appendChild(running);
            editConfig.appendChild(target);

            // Command to update the port profile with the desired configuration.
            Element config = doc.createElement("nf:config");
            config.appendChild(configPortProfileDetails(doc, name, mode, params));
            editConfig.appendChild(config);

            return serialize(domImpl, doc);
        } catch (ParserConfigurationException e) {
            s_logger.error("Error while creating update port profile message : " + e.getMessage());
            return null;
        } catch (DOMException e) {
            s_logger.error("Error while creating update port profile message : " + e.getMessage());
            return null;
        }
    }

    public static String getDeletePortProfile(String portName) {
        try {
            // Create the document and root element.
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            DOMImplementation domImpl = docBuilder.getDOMImplementation();
            Document doc = createDocument(domImpl);

            // Edit configuration command.
            Element editConfig = doc.createElement("nf:edit-config");
            doc.getDocumentElement().appendChild(editConfig);

            // Command to get into exec configure mode.
            Element target = doc.createElement("nf:target");
            Element running = doc.createElement("nf:running");
            target.appendChild(running);
            editConfig.appendChild(target);

            // Command to create the port profile with the desired configuration.
            Element config = doc.createElement("nf:config");
            config.appendChild(deletePortProfileDetails(doc, portName));
            editConfig.appendChild(config);

            return serialize(domImpl, doc);
        } catch (ParserConfigurationException e) {
            s_logger.error("Error while creating delete port profile message : " + e.getMessage());
            return null;
        } catch (DOMException e) {
            s_logger.error("Error while creating delete port profile message : " + e.getMessage());
            return null;
        }
    }

    public static String getAddPolicyMap(String name, int averageRate, int maxRate, int burstRate) {
        try {
            // Create the document and root element.
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            DOMImplementation domImpl = docBuilder.getDOMImplementation();
            Document doc = createDocument(domImpl);

            // Edit configuration command.
            Element editConfig = doc.createElement("nf:edit-config");
            doc.getDocumentElement().appendChild(editConfig);

            // Command to get into exec configure mode.
            Element target = doc.createElement("nf:target");
            Element running = doc.createElement("nf:running");
            target.appendChild(running);
            editConfig.appendChild(target);

            // Command to create the port profile with the desired configuration.
            Element config = doc.createElement("nf:config");
            config.appendChild(policyMapDetails(doc, name, averageRate, maxRate, burstRate));
            editConfig.appendChild(config);

            return serialize(domImpl, doc);
        } catch (ParserConfigurationException e) {
            s_logger.error("Error while creating policy map message : " + e.getMessage());
            return null;
        } catch (DOMException e) {
            s_logger.error("Error while creating policy map message : " + e.getMessage());
            return null;
        }
    }

    public static String getDeletePolicyMap(String name) {
        try {
            // Create the document and root element.
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            DOMImplementation domImpl = docBuilder.getDOMImplementation();
            Document doc = createDocument(domImpl);

            // Edit configuration command.
            Element editConfig = doc.createElement("nf:edit-config");
            doc.getDocumentElement().appendChild(editConfig);

            // Command to get into exec configure mode.
            Element target = doc.createElement("nf:target");
            Element running = doc.createElement("nf:running");
            target.appendChild(running);
            editConfig.appendChild(target);

            // Command to create the port profile with the desired configuration.
            Element config = doc.createElement("nf:config");
            config.appendChild(deletePolicyMapDetails(doc, name));
            editConfig.appendChild(config);

            return serialize(domImpl, doc);
        } catch (ParserConfigurationException e) {
            s_logger.error("Error while creating delete policy map message : " + e.getMessage());
            return null;
        } catch (DOMException e) {
            s_logger.error("Error while creating delete policy map message : " + e.getMessage());
            return null;
        }
    }

    public static String getServicePolicy(String policyMap, String portProfile, boolean attach) {
        try {
            // Create the document and root element.
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            DOMImplementation domImpl = docBuilder.getDOMImplementation();
            Document doc = createDocument(domImpl);

            // Edit configuration command.
            Element editConfig = doc.createElement("nf:edit-config");
            doc.getDocumentElement().appendChild(editConfig);

            // Command to get into exec configure mode.
            Element target = doc.createElement("nf:target");
            Element running = doc.createElement("nf:running");
            target.appendChild(running);
            editConfig.appendChild(target);

            // Command to create the port profile with the desired configuration.
            Element config = doc.createElement("nf:config");
            config.appendChild(serviceDetails(doc, policyMap, portProfile, attach));
            editConfig.appendChild(config);

            return serialize(domImpl, doc);
        } catch (ParserConfigurationException e) {
            s_logger.error("Error while creating attach/detach service policy message : " + e.getMessage());
            return null;
        } catch (DOMException e) {
            s_logger.error("Error while creating attach/detach service policy message : " + e.getMessage());
            return null;
        }
    }

    public static String getPortProfile(String name) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            DOMImplementation domImpl = docBuilder.getDOMImplementation();
            Document doc = createDocument(domImpl);

            Element get = doc.createElement("nf:get");
            doc.getDocumentElement().appendChild(get);

            Element filter = doc.createElement("nf:filter");
            filter.setAttribute("type", "subtree");
            get.appendChild(filter);

            // Create the show port-profile name <profile-name> command.
            Element show = doc.createElement("show");
            filter.appendChild(show);
            Element portProfile = doc.createElement("port-profile");
            show.appendChild(portProfile);
            Element nameNode = doc.createElement("name");
            portProfile.appendChild(nameNode);

            // Profile name
            Element profileName = doc.createElement("profile_name");
            profileName.setTextContent(name);
            nameNode.appendChild(profileName);

            return serialize(domImpl, doc);
        } catch (ParserConfigurationException e) {
            s_logger.error("Error while creating the message to get port profile details: " + e.getMessage());
            return null;
        } catch (DOMException e) {
            s_logger.error("Error while creating the message to get port profile details: " + e.getMessage());
            return null;
        }
    }

    public static String getPolicyMap(String name) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            DOMImplementation domImpl = docBuilder.getDOMImplementation();
            Document doc = createDocument(domImpl);

            Element get = doc.createElement("nf:get");
            doc.getDocumentElement().appendChild(get);

            Element filter = doc.createElement("nf:filter");
            filter.setAttribute("type", "subtree");
            get.appendChild(filter);

            // Create the show port-profile name <profile-name> command.
            Element show = doc.createElement("show");
            filter.appendChild(show);
            Element policyMap = doc.createElement("policy-map");
            show.appendChild(policyMap);
            Element nameNode = doc.createElement("name");
            nameNode.setTextContent(name);
            policyMap.appendChild(nameNode);

            return serialize(domImpl, doc);
        } catch (ParserConfigurationException e) {
            s_logger.error("Error while creating the message to get policy map details : " + e.getMessage());
            return null;
        } catch (DOMException e) {
            s_logger.error("Error while creating the message to get policy map details : " + e.getMessage());
            return null;
        }
    }

    public static String getHello() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            DOMImplementation domImpl = docBuilder.getDOMImplementation();

            // Root elements.
            Document doc = domImpl.createDocument(s_namespace, "nc:hello", null);

            // Client capacity. We are only supporting basic capacity.
            Element capabilities = doc.createElement("nc:capabilities");
            Element capability = doc.createElement("nc:capability");
            capability.setTextContent("urn:ietf:params:xml:ns:netconf:base:1.0");

            capabilities.appendChild(capability);
            doc.getDocumentElement().appendChild(capabilities);

            return serialize(domImpl, doc);
        } catch (ParserConfigurationException e) {
            s_logger.error("Error while creating hello message : " + e.getMessage());
            return null;
        } catch (DOMException e) {
            s_logger.error("Error while creating hello message : " + e.getMessage());
            return null;
        }
    }

    public static String getVServiceNode(String vlanId, String ipAddr) {
        try {
            // Create the document and root element.
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            DOMImplementation domImpl = docBuilder.getDOMImplementation();
            Document doc = createDocument(domImpl);

            // Edit configuration command.
            Element editConfig = doc.createElement("nf:edit-config");
            doc.getDocumentElement().appendChild(editConfig);

            // Command to get into exec configure mode.
            Element target = doc.createElement("nf:target");
            Element running = doc.createElement("nf:running");
            target.appendChild(running);
            editConfig.appendChild(target);

            // Command to create the port profile with the desired configuration.
            Element config = doc.createElement("nf:config");
            config.appendChild(configVServiceNodeDetails(doc, vlanId, ipAddr));
            editConfig.appendChild(config);

            return serialize(domImpl, doc);
        } catch (ParserConfigurationException e) {
            s_logger.error("Error while adding vservice node for vlan " + vlanId + ", " + e.getMessage());
            return null;
        } catch (DOMException e) {
            s_logger.error("Error while adding vservice node for vlan " + vlanId + ", " + e.getMessage());
            return null;
        }
    }

    private static Element configVServiceNodeDetails(Document doc, String vlanId, String ipAddr) {
        // In mode, exec_configure.
        Element configure = doc.createElementNS(s_ciscons, "nxos:configure");
        Element modeConfigure = doc.createElement("nxos:" + s_configuremode);
        configure.appendChild(modeConfigure);

        // vservice node %name% type asa
        Element vservice = doc.createElement("vservice");
        vservice.appendChild(doc.createElement("node"))
            .appendChild(doc.createElement("ASA_" + vlanId))
            .appendChild(doc.createElement("type"))
            .appendChild(doc.createElement("asa"));
        modeConfigure.appendChild(vservice);

        Element address = doc.createElement(s_paramvalue);
        address.setAttribute("isKey", "true");
        address.setTextContent(ipAddr);

        // ip address %ipAddr%
        modeConfigure.appendChild(doc.createElement("ip")).appendChild(doc.createElement("address")).appendChild(doc.createElement("value")).appendChild(address);

        Element vlan = doc.createElement(s_paramvalue);
        vlan.setAttribute("isKey", "true");
        vlan.setTextContent(vlanId);

        // adjacency l2 vlan %vlanId%
        modeConfigure.appendChild(doc.createElement("adjacency"))
            .appendChild(doc.createElement("l2"))
            .appendChild(doc.createElement("vlan"))
            .appendChild(doc.createElement("value"))
            .appendChild(vlan);

        // fail-mode close
        modeConfigure.appendChild(doc.createElement("fail-mode")).appendChild(doc.createElement("close"));

        // Persist the configuration across reboots.
        modeConfigure.appendChild(persistConfiguration(doc));

        return configure;
    }

    private static Element configPortProfileDetails(Document doc, String name, PortProfileType type, BindingType binding, SwitchPortMode mode, int vlanid, String vdc,
        String espName) {

        // In mode, exec_configure.
        Element configure = doc.createElementNS(s_ciscons, "nxos:configure");
        Element modeConfigure = doc.createElement("nxos:" + s_configuremode);
        configure.appendChild(modeConfigure);

        // Port profile name and type configuration.
        Element portProfile = doc.createElement("port-profile");
        modeConfigure.appendChild(portProfile);

        // Port profile type.
        Element portDetails = doc.createElement("name");
        switch (type) {
            case none:
                portProfile.appendChild(portDetails);
                break;
            case ethernet: {
                Element typetag = doc.createElement("type");
                Element ethernettype = doc.createElement("ethernet");
                portProfile.appendChild(typetag);
                typetag.appendChild(ethernettype);
                ethernettype.appendChild(portDetails);
            }
                break;
            case vethernet: {
                Element typetag = doc.createElement("type");
                Element ethernettype = doc.createElement("vethernet");
                portProfile.appendChild(typetag);
                typetag.appendChild(ethernettype);
                ethernettype.appendChild(portDetails);
            }
                break;
        }

        // Port profile name.
        Element value = doc.createElement(s_paramvalue);
        value.setAttribute("isKey", "true");
        value.setTextContent(name);
        portDetails.appendChild(value);

        // element for port prof mode.
        Element portProf = doc.createElement(s_portprofmode);
        portDetails.appendChild(portProf);

        // Binding type.
        if (binding != BindingType.none) {
            portProf.appendChild(getBindingType(doc, binding));
        }

        if (mode != SwitchPortMode.none) {
            // Switchport mode.
            portProf.appendChild(getSwitchPortMode(doc, mode));
            // Adding vlan details.
            if (vlanid > 0) {
                portProf.appendChild(getAddVlanDetails(doc, mode, Integer.toString(vlanid)));
            }
        }

        // Command "vmware port-group".
        Element vmware = doc.createElement("vmware");
        Element portgroup = doc.createElement("port-group");
        vmware.appendChild(portgroup);
        portProf.appendChild(vmware);

        // org %vdc%
        // vservice node <Node Name> profile <Edge Security Profile Name in VNMC>
        Element vdcValue = doc.createElement(s_paramvalue);
        vdcValue.setAttribute("isKey", "true");
        vdcValue.setTextContent(vdc);

        Element org = doc.createElement("org");
        org.appendChild(doc.createElement("orgname")).appendChild(vdcValue);
        portProf.appendChild(org);

        String asaNodeName = "ASA_" + vlanid;
        Element vservice = doc.createElement("vservice");
        vservice.appendChild(doc.createElement("node"))
            .appendChild(doc.createElement(asaNodeName))
            .appendChild(doc.createElement("profile"))
            .appendChild(doc.createElement(espName));
        portProf.appendChild(vservice);

        // no shutdown.
        Element no = doc.createElement("no");
        Element shutdown = doc.createElement("shutdown");
        no.appendChild(shutdown);
        portProf.appendChild(no);

        // Enable the port profile.
        Element state = doc.createElement("state");
        Element enabled = doc.createElement("enabled");
        state.appendChild(enabled);
        portProf.appendChild(state);

        // Persist the configuration across reboots.
        modeConfigure.appendChild(persistConfiguration(doc));

        return configure;
    }

    private static Element configPortProfileDetails(Document doc, String name, PortProfileType type, BindingType binding, SwitchPortMode mode, int vlanid) {

        // In mode, exec_configure.
        Element configure = doc.createElementNS(s_ciscons, "nxos:configure");
        Element modeConfigure = doc.createElement("nxos:" + s_configuremode);
        configure.appendChild(modeConfigure);

        // Port profile name and type configuration.
        Element portProfile = doc.createElement("port-profile");
        modeConfigure.appendChild(portProfile);

        // Port profile type.
        Element portDetails = doc.createElement("name");
        switch (type) {
            case none:
                portProfile.appendChild(portDetails);
                break;
            case ethernet: {
                Element typetag = doc.createElement("type");
                Element ethernettype = doc.createElement("ethernet");
                portProfile.appendChild(typetag);
                typetag.appendChild(ethernettype);
                ethernettype.appendChild(portDetails);
            }
                break;
            case vethernet: {
                Element typetag = doc.createElement("type");
                Element ethernettype = doc.createElement("vethernet");
                portProfile.appendChild(typetag);
                typetag.appendChild(ethernettype);
                ethernettype.appendChild(portDetails);
            }
                break;
        }

        // Port profile name.
        Element value = doc.createElement(s_paramvalue);
        value.setAttribute("isKey", "true");
        value.setTextContent(name);
        portDetails.appendChild(value);

        // element for port prof mode.
        Element portProf = doc.createElement(s_portprofmode);
        portDetails.appendChild(portProf);

        // Binding type.
        if (binding != BindingType.none) {
            portProf.appendChild(getBindingType(doc, binding));
        }

        if (mode != SwitchPortMode.none) {
            // Switchport mode.
            portProf.appendChild(getSwitchPortMode(doc, mode));
            // Adding vlan details.
            if (vlanid > 0) {
                portProf.appendChild(getAddVlanDetails(doc, mode, Integer.toString(vlanid)));
            }
        }

        // Command "vmware port-group".
        Element vmware = doc.createElement("vmware");
        Element portgroup = doc.createElement("port-group");
        vmware.appendChild(portgroup);
        portProf.appendChild(vmware);

        // no shutdown.
        Element no = doc.createElement("no");
        Element shutdown = doc.createElement("shutdown");
        no.appendChild(shutdown);
        portProf.appendChild(no);

        // Enable the port profile.
        Element state = doc.createElement("state");
        Element enabled = doc.createElement("enabled");
        state.appendChild(enabled);
        portProf.appendChild(state);

        // Persist the configuration across reboots.
        modeConfigure.appendChild(persistConfiguration(doc));

        return configure;
    }

    private static Element configPortProfileDetails(Document doc, String name, SwitchPortMode mode, List<Pair<VsmCommand.OperationType, String>> params) {

        // In mode, exec_configure.
        Element configure = doc.createElementNS(s_ciscons, "nxos:configure");
        Element modeConfigure = doc.createElement("nxos:" + s_configuremode);
        configure.appendChild(modeConfigure);

        // Port profile name and type configuration.
        Element portProfile = doc.createElement("port-profile");
        modeConfigure.appendChild(portProfile);

        // Port profile type.
        Element portDetails = doc.createElement("name");
        portProfile.appendChild(portDetails);

        // Name of the profile to update.
        Element value = doc.createElement(s_paramvalue);
        value.setAttribute("isKey", "true");
        value.setTextContent(name);
        portDetails.appendChild(value);

        // element for port prof mode.
        Element portProfMode = doc.createElement(s_portprofmode);
        portDetails.appendChild(portProfMode);

        for (Pair<VsmCommand.OperationType, String> item : params) {
            if (item.first() == OperationType.addvlanid) {
                // Set the access mode configuration or the list
                // of allowed vlans on the trunking interface.
                portProfMode.appendChild(getAddVlanDetails(doc, mode, item.second()));
            } else if (item.first() == OperationType.removevlanid) {
                portProfMode.appendChild(getDeleteVlanDetails(doc, mode, item.second()));
            }
        }

        // Persist the configuration across reboots.
        modeConfigure.appendChild(persistConfiguration(doc));

        return configure;
    }

    private static Element deletePortProfileDetails(Document doc, String name) {
        Element configure = doc.createElementNS(s_ciscons, "nxos:configure");
        Element modeConfigure = doc.createElement("nxos:" + s_configuremode);
        configure.appendChild(modeConfigure);

        // Command and name for the port profile to be deleted.
        Element deletePortProfile = doc.createElement("no");
        modeConfigure.appendChild(deletePortProfile);

        Element portProfile = doc.createElement("port-profile");
        deletePortProfile.appendChild(portProfile);

        Element portDetails = doc.createElement("name");
        portProfile.appendChild(portDetails);

        // Name of the profile to delete.
        Element value = doc.createElement(s_paramvalue);
        value.setAttribute("isKey", "true");
        value.setTextContent(name);
        portDetails.appendChild(value);

        // Persist the configuration across reboots.
        modeConfigure.appendChild(persistConfiguration(doc));

        return configure;
    }

    private static Element policyMapDetails(Document doc, String name, int averageRate, int maxRate, int burstRate) {
        Element configure = doc.createElementNS(s_ciscons, "nxos:configure");
        Element modeConfigure = doc.createElement("nxos:" + s_configuremode);
        configure.appendChild(modeConfigure);

        // Policy map details
        Element policyMap = doc.createElement("policy-map");
        modeConfigure.appendChild(policyMap);

        Element policyDetails = doc.createElement("name");
        policyMap.appendChild(policyDetails);

        // Name of the policy to create/update.
        Element value = doc.createElement(s_paramvalue);
        value.setAttribute("isKey", "true");
        value.setTextContent(name);
        policyDetails.appendChild(value);

        Element policyMapMode = doc.createElement(s_policymapmode);
        policyDetails.appendChild(policyMapMode);

        // Create the default class to match all traffic.
        Element classRoot = doc.createElement("class");
        Element classDefault = doc.createElement("class-default");
        policyMapMode.appendChild(classRoot);
        classRoot.appendChild(classDefault);

        Element classMode = doc.createElement(s_classtypemode);
        classDefault.appendChild(classMode);

        // Set the average, max and burst rate.
        // TODO: Add handling for max and burst.
        Element police = doc.createElement("police");
        classMode.appendChild(police);

        // Set the committed information rate and its value in mbps.
        Element cir = doc.createElement("cir");
        police.appendChild(cir);
        Element cirValue = doc.createElement("cir-val");
        cir.appendChild(cirValue);
        Element value2 = doc.createElement(s_paramvalue);
        Element mbps = doc.createElement("mbps");
        value2.setTextContent(Integer.toString(averageRate));
        cirValue.appendChild(value2);
        cirValue.appendChild(mbps);

        // Persist the configuration across reboots.
        modeConfigure.appendChild(persistConfiguration(doc));

        return configure;
    }

    private static Element deletePolicyMapDetails(Document doc, String name) {
        Element configure = doc.createElementNS(s_ciscons, "nxos:configure");
        Element modeConfigure = doc.createElement("nxos:" + s_configuremode);
        configure.appendChild(modeConfigure);

        // Delete Policy map details
        Element deletePolicyMap = doc.createElement("no");
        Element policyMap = doc.createElement("policy-map");
        deletePolicyMap.appendChild(policyMap);
        modeConfigure.appendChild(deletePolicyMap);

        Element policyDetails = doc.createElement("name");
        policyMap.appendChild(policyDetails);

        // Name of the policy to create/update.
        Element value = doc.createElement(s_paramvalue);
        value.setAttribute("isKey", "true");
        value.setTextContent(name);
        policyDetails.appendChild(value);

        // Persist the configuration across reboots.
        modeConfigure.appendChild(persistConfiguration(doc));

        return configure;
    }

    private static Element serviceDetails(Document doc, String policyMap, String portProfile, boolean attach) {
        // In mode, exec_configure.
        Element configure = doc.createElementNS(s_ciscons, "nxos:configure");
        Element modeConfigure = doc.createElement("nxos:" + s_configuremode);
        configure.appendChild(modeConfigure);

        // Port profile name and type configuration.
        Element profile = doc.createElement("port-profile");
        modeConfigure.appendChild(profile);

        // Port profile type.
        Element portDetails = doc.createElement("name");
        profile.appendChild(portDetails);

        // Name of the profile to update.
        Element value = doc.createElement(s_paramvalue);
        value.setAttribute("isKey", "true");
        value.setTextContent(portProfile);
        portDetails.appendChild(value);

        // element for port prof mode.
        Element portProfMode = doc.createElement(s_portprofmode);
        portDetails.appendChild(portProfMode);

        // Associate/Remove the policy for input.
        if (attach) {
            portProfMode.appendChild(getServicePolicyCmd(doc, policyMap, "input"));
        } else {
            Element detach = doc.createElement("no");
            portProfMode.appendChild(detach);
            detach.appendChild(getServicePolicyCmd(doc, policyMap, "input"));
        }

        // Associate/Remove the policy for output.
        if (attach) {
            portProfMode.appendChild(getServicePolicyCmd(doc, policyMap, "output"));
        } else {
            Element detach = doc.createElement("no");
            portProfMode.appendChild(detach);
            detach.appendChild(getServicePolicyCmd(doc, policyMap, "output"));
        }

        // Persist the configuration across reboots.
        modeConfigure.appendChild(persistConfiguration(doc));

        return configure;
    }

    private static Element persistConfiguration(Document doc) {
        Element copy = doc.createElement("copy");
        Element running = doc.createElement("running-config");
        Element startup = doc.createElement("startup-config");
        copy.appendChild(running);
        running.appendChild(startup);
        return copy;
    }

    private static Element getAddVlanDetails(Document doc, SwitchPortMode mode, String vlanid) {
        Element switchport = doc.createElement("switchport");

        // Details of the vlanid to add.
        Element vlancreate = doc.createElement("vlan-id-create-delete");
        Element value = doc.createElement(s_paramvalue);
        value.setTextContent(vlanid);
        vlancreate.appendChild(value);

        // Handling is there only for 'access' and 'trunk allowed' mode command.
        if (mode == SwitchPortMode.access) {
            Element access = doc.createElement("access");
            switchport.appendChild(access);

            Element vlan = doc.createElement("vlan");
            access.appendChild(vlan);

            vlan.appendChild(vlancreate);
        } else if (mode == SwitchPortMode.trunk) {
            Element trunk = doc.createElement("trunk");
            switchport.appendChild(trunk);

            Element allowed = doc.createElement("allowed");
            trunk.appendChild(allowed);

            Element vlan = doc.createElement("vlan");
            allowed.appendChild(vlan);

            Element add = doc.createElement("add");
            vlan.appendChild(add);

            add.appendChild(vlancreate);
        }

        return switchport;
    }

    private static Node getDeleteVlanDetails(Document doc, SwitchPortMode mode, String vlanid) {
        Node parentNode = null;
        Element switchport = doc.createElement("switchport");

        // Handling is there only for 'access' and 'trunk allowed' mode command.
        if (mode == SwitchPortMode.access) {
            Element no = doc.createElement("no");
            no.appendChild(switchport);
            parentNode = no;

            Element access = doc.createElement("access");
            switchport.appendChild(access);

            Element vlan = doc.createElement("vlan");
            access.appendChild(vlan);
        } else if (mode == SwitchPortMode.trunk) {
            parentNode = switchport;

            Element trunk = doc.createElement("trunk");
            switchport.appendChild(trunk);

            Element allowed = doc.createElement("allowed");
            trunk.appendChild(allowed);

            Element vlan = doc.createElement("vlan");
            allowed.appendChild(vlan);

            Element remove = doc.createElement("remove");
            vlan.appendChild(remove);

            // Details of the vlanid to add.
            Element vlancreate = doc.createElement("vlan-id-create-delete");
            Element value = doc.createElement(s_paramvalue);
            value.setTextContent(vlanid);
            vlancreate.appendChild(value);

            remove.appendChild(vlancreate);
        }

        return parentNode;
    }

    private static Element getBindingType(Document doc, BindingType binding) {
        Element portBinding = doc.createElement("port-binding");

        // We only have handling for access or trunk mode. Handling for private-vlan
        // host/promiscuous command will have to be added.
        if (binding == BindingType.portbindingstatic) {
            Element type = doc.createElement("static");
            portBinding.appendChild(type);
        } else if (binding == BindingType.portbindingdynamic) {
            Element type = doc.createElement("dynamic");
            portBinding.appendChild(type);
        } else if (binding == BindingType.portbindingephermal) {
            Element type = doc.createElement("ephemeral");
            portBinding.appendChild(type);
        }

        return portBinding;
    }

    private static Element getSwitchPortMode(Document doc, SwitchPortMode mode) {
        Element switchport = doc.createElement("switchport");
        Element accessmode = doc.createElement("mode");
        switchport.appendChild(accessmode);

        // We only have handling for access or trunk mode. Handling for private-vlan
        // host/promiscuous command will have to be added.
        if (mode == SwitchPortMode.access) {
            Element access = doc.createElement("access");
            accessmode.appendChild(access);
        } else if (mode == SwitchPortMode.trunk) {
            Element trunk = doc.createElement("trunk");
            accessmode.appendChild(trunk);
        }

        return switchport;
    }

    private static Element getServicePolicyCmd(Document doc, String policyMap, String type) {
        Element service = doc.createElement("service-policy");
        Element input = doc.createElement(type);
        service.appendChild(input);

        Element name = doc.createElement("name");
        input.appendChild(name);

        Element policyValue = doc.createElement(s_paramvalue);
        policyValue.setTextContent(policyMap);
        name.appendChild(policyValue);

        return service;
    }

    private static Document createDocument(DOMImplementation dom) {
        Document doc = dom.createDocument(s_namespace, "nf:rpc", null);
        doc.getDocumentElement().setAttribute("message-id", "101");
        doc.getDocumentElement().setAttributeNS(s_ciscons, "portprofile", "true");
        return doc;
    }

    private static String serialize(DOMImplementation domImpl, Document document) {
        DOMImplementationLS ls = (DOMImplementationLS)domImpl;
        LSSerializer lss = ls.createLSSerializer();
        return lss.writeToString(document);
    }
}
