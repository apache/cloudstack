package com.cloud.utils.cisco.n1kv.vsm;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

public class VsmCommand {

    private static final Logger s_logger = Logger.getLogger(VsmCommand.class);
    private static final String s_namespace = "urn:ietf:params:xml:ns:netconf:base:1.0";
    private static final String s_ciscons = "http://www.cisco.com/nxos:1.0:ppm";
    private static final String s_configuremode = "__XML__MODE__exec_configure";
    private static final String s_portprofmode = "__XML__MODE_port-prof";
    private static final String s_paramvalue = "__XML__PARAM_value";

    public enum PortProfileType {
        none,
        vethernet,
        ethernet;
    }

    public enum BindingType {
        none,
        portbindingstatic,
        portbindingdynamic,
        portbindingephermal;
    }

    public enum SwitchPortMode {
        none,
        access,
        trunk,
        privatevlanhost,
        privatevlanpromiscuous
    }

    public static String getAddPortProfile(String name, PortProfileType type,
            BindingType binding, SwitchPortMode mode, int vlanid) {
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
            s_logger.error("Error while creating delete message : " + e.getMessage());
            return null;
        } catch (DOMException e) {
            s_logger.error("Error while creating delete message : " + e.getMessage());
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
            s_logger.error("Error while creating delete message : " + e.getMessage());
            return null;
        } catch (DOMException e) {
            s_logger.error("Error while creating delete message : " + e.getMessage());
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
            s_logger.error("Error while creating delete message : " + e.getMessage());
            return null;
        }
    }

    private static Element configPortProfileDetails(Document doc, String name, PortProfileType type,
            BindingType binding, SwitchPortMode mode, int vlanid) {

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
        case ethernet:
            {
                Element typetag = doc.createElement("type");
                Element ethernettype = doc.createElement("ethernet");
                portProfile.appendChild(typetag);
                typetag.appendChild(ethernettype);
                ethernettype.appendChild(portDetails);
            }
            break;
        case vethernet:
            {
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
            portProf.appendChild(getVlanDetails(doc, mode, vlanid));
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
        // modeConfigure.appendChild(persistConfiguration(doc));

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
        // modeConfigure.appendChild(persistConfiguration(doc));

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

    private static Element getVlanDetails(Document doc, SwitchPortMode mode, int vlanid) {
        Element switchport = doc.createElement("switchport");

        // Handling is there only for 'access' mode command.
        if (mode == SwitchPortMode.access) {
            Element access = doc.createElement("access");
            switchport.appendChild(access);

            Element vlan = doc.createElement("vlan");
            access.appendChild(vlan);

            Element vlancreate = doc.createElement("vlan-id-create-delete");
            vlan.appendChild(vlancreate);

            Element value = doc.createElement(s_paramvalue);
            value.setTextContent(Integer.toString(vlanid));
            vlancreate.appendChild(value);
        }

        return switchport;
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

    private static Document createDocument(DOMImplementation dom) {
        Document doc = dom.createDocument(s_namespace, "nf:rpc", null);
        doc.getDocumentElement().setAttribute( "message-id", "101" );
        doc.getDocumentElement().setAttributeNS(s_ciscons, "portprofile", "true");
        return doc;
    }

    private static String serialize(DOMImplementation domImpl, Document document) {
        DOMImplementationLS ls = (DOMImplementationLS) domImpl;
        LSSerializer lss = ls.createLSSerializer();
        return lss.writeToString(document);
    }
}
