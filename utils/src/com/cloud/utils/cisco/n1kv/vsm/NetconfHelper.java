package com.cloud.utils.cisco.n1kv.vsm;

import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.DOMException;

import com.cloud.utils.ssh.*;
import com.trilead.ssh2.Session;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.ChannelCondition;
import com.cloud.utils.exception.CloudRuntimeException;

public class NetconfHelper {
    private static final Logger s_logger = Logger.getLogger(NetconfHelper.class);

    private static final String SSH_NETCONF_TERMINATOR = "]]>]]>";

    private Connection _connection;

    private Session _session;

    public NetconfHelper(String ip, String username, String password) throws CloudRuntimeException {
        _connection = SSHCmdHelper.acquireAuthorizedConnection(ip, username, password);
        if (_connection == null) {
            throw new CloudRuntimeException("Error opening ssh connection.");
        }

        try {
            _session = _connection.openSession();
            _session.startSubSystem("xmlagent");
            exchangeHello();
        } catch (final Exception e) {
            disconnect();
            s_logger.error("Failed to connect to device SSH server: " + e.getMessage());
            throw new CloudRuntimeException("Failed to connect to SSH server: "
                    + _connection.getHostname());
        }
    }

    public void disconnect() {
        if (_session != null) {
            _session.close();
        }
        SSHCmdHelper.releaseSshConnection(_connection);
    }

    public void queryStatus() {
        // This command is used query the server status.
        String status = "<?xml version=\"1.0\"?>"
                + "<nc:rpc message-id=\"1\" xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0"
                + "\"xmlns=\"http://www.cisco.com/nxos:1.0:xml\">" + "  <nc:get>"
                + "    <nc:filter type=\"subtree\">" + "      <show>" + "        <xml>"
                + "          <server>" + "            <status/>" + "          </server>"
                + "        </xml>" + "      </show>" + "    </nc:filter>" + "  </nc:get>"
                + "</nc:rpc>" + SSH_NETCONF_TERMINATOR;
        send(status);
        String reply = receive();
    }

    private String getAddPortProfile(String portName, int vlanid) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            DOMImplementation domImpl = docBuilder.getDOMImplementation();

            // Root elements.
            Document doc = domImpl.createDocument("urn:ietf:params:xml:ns:netconf:base:1.0", 
                    "nf:rpc", null);
            doc.getDocumentElement().setAttribute( "message-id", "101" );
            doc.getDocumentElement().setAttributeNS("http://www.cisco.com/nxos:1.0:ppm",
                    "addportprofile", "true");

            // Edit configuration command.
            Element editConfig = doc.createElement("nf:edit-config");
            Element target = doc.createElement("nf:target");
            Element running = doc.createElement("nf:running");
            target.appendChild(running);
            Element config = doc.createElement("nf:config");
            Element configure = doc.createElementNS("http://www.cisco.com/nxos:1.0:ppm", "nxos:configure");
            Element execConfigure = doc.createElement("nxos:__XML__MODE__exec_configure");
            Element portProfile = doc.createElement("port-profile");
            Element type = doc.createElement("type");
            Element ethernettype = doc.createElement("vethernet");
            Element name = doc.createElement("name");
            Element value = doc.createElement("__XML__PARAM_value");
            value.setAttribute("isKey", "true");
            value.setTextContent(portName);

            // Port profile details start here.
            Element portProf = doc.createElement("__XML__MODE__port-prof");

            // Command : switchport mode access
            Element switchport1 = doc.createElement("switchport");
            Element mode1 = doc.createElement("mode");
            Element access1 = doc.createElement("access");
            mode1.appendChild(access1);
            switchport1.appendChild(mode1);

            // Command : switchport access vlan <vlanid>
            Element switchport2 = doc.createElement("switchport");
            Element access2 = doc.createElement("access");
            Element vlan = doc.createElement("vlan");
            Element vlancreate = doc.createElement("vlan-id-create-delete");
            Element value2 = doc.createElement("__XML__PARAM_value");
            value2.setTextContent(Integer.toString(vlanid));
            vlancreate.appendChild(value2);
            vlan.appendChild(vlancreate);
            access2.appendChild(vlan);
            switchport2.appendChild(access2);

            // Command : vmware port-group
            Element vmware = doc.createElement("vmware");
            Element portgroup = doc.createElement("port-group");
            vmware.appendChild(portgroup);

            // Command : state enabled.
            Element state = doc.createElement("state");
            Element enabled = doc.createElement("enabled");
            state.appendChild(enabled);

            // Command : no shutdown.
            Element no = doc.createElement("no");
            Element shutdown = doc.createElement("shutdown");
            no.appendChild(shutdown);

            // Put the port profile details together.
            portProf.appendChild(switchport1);
            portProf.appendChild(switchport2);
            portProf.appendChild(vmware);
            portProf.appendChild(state);
            portProf.appendChild(no);

            // Put the xml-rpc together.
            name.appendChild(value);
            name.appendChild(portProf);
            ethernettype.appendChild(name);
            type.appendChild(ethernettype);
            portProfile.appendChild(type);
            execConfigure.appendChild(portProfile);
            configure.appendChild(execConfigure);
            config.appendChild(configure);
            editConfig.appendChild(target);
            editConfig.appendChild(config);
            doc.getDocumentElement().appendChild(editConfig);

            return serialize(domImpl, doc);
        } catch (ParserConfigurationException e) {
            s_logger.error("Error while creating add port profile message : " + e.getMessage());
            return null;
        } catch (DOMException e) {
            s_logger.error("Error while creating add port profile message : " + e.getMessage());
            return null;
        }
    }

    public void addPortProfile(String name, int vlan) {
        String command = getAddPortProfile(name, vlan) + SSH_NETCONF_TERMINATOR;
        send(command);

        // parse the rpc reply and the return success or failure.
        String reply = receive().trim();
        if (reply.endsWith(SSH_NETCONF_TERMINATOR)) {
            reply = reply.substring(0, reply.length() - (new String(SSH_NETCONF_TERMINATOR).length()));
        }
        else {
            throw new CloudRuntimeException("Malformed repsonse from vsm for add " +
                    "port profile request: " + reply);
        }

        VsmResponse response = new VsmResponse(reply);
        if (!response.isResponseOk()) {
            throw new CloudRuntimeException(response.toString());
        }
    }

    private String getDeletePortProfile(String portName) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            DOMImplementation domImpl = docBuilder.getDOMImplementation();

            // Root elements.
            Document doc = domImpl.createDocument("urn:ietf:params:xml:ns:netconf:base:1.0",
                    "nf:rpc", null);
            doc.getDocumentElement().setAttribute( "message-id", "101" );
            doc.getDocumentElement().setAttributeNS("http://www.cisco.com/nxos:1.0:ppm",
                    "deleteportprofile", "true");

            // Edit configuration command.
            Element editConfig = doc.createElement("nf:edit-config");

            // Details of the port profile to delete.
            Element target = doc.createElement("nf:target");
            Element running = doc.createElement("nf:running");
            target.appendChild(running);

            Element config = doc.createElement("nf:config");
            Element configure = doc.createElementNS("http://www.cisco.com/nxos:1.0:ppm", "nxos:configure");
            Element execConfigure = doc.createElement("nxos:__XML__MODE__exec_configure");
            Element delete = doc.createElement("no");
            Element portProfile = doc.createElement("port-profile");
            Element name = doc.createElement("name");
            Element value = doc.createElement("__XML__PARAM_value");
            value.setAttribute("isKey", "true");
            value.setTextContent(portName);

            // Put the xml-rpc together.
            name.appendChild(value);
            portProfile.appendChild(name);
            delete.appendChild(portProfile);
            execConfigure.appendChild(delete);
            configure.appendChild(execConfigure);
            config.appendChild(configure);
            editConfig.appendChild(target);
            editConfig.appendChild(config);
            doc.getDocumentElement().appendChild(editConfig);

            return serialize(domImpl, doc);
        } catch (ParserConfigurationException e) {
            s_logger.error("Error while creating delete message : " + e.getMessage());
            return null;
        } catch (DOMException e) {
            s_logger.error("Error while creating delete message : " + e.getMessage());
            return null;
        }
    }

    public void deletePortProfile(String name) {
        String command = getDeletePortProfile(name) + SSH_NETCONF_TERMINATOR;
        send(command);

        // parse the rpc reply and the return success or failure.
        String reply = receive().trim();
        if (reply.endsWith(SSH_NETCONF_TERMINATOR)) {
            reply = reply.substring(0, reply.length() - (new String(SSH_NETCONF_TERMINATOR).length()));
        }
        else {
            throw new CloudRuntimeException("Malformed repsonse from vsm for delete " +
                    "port profile request :" + reply);
        }

        VsmResponse response = new VsmResponse(reply);
        if (!response.isResponseOk()) {
            throw new CloudRuntimeException(response.toString());
        }
    }

    private String getHello() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            DOMImplementation domImpl = docBuilder.getDOMImplementation();

            // Root elements.
            Document doc = domImpl.createDocument("urn:ietf:params:xml:ns:netconf:base:1.0",
                    "nc:hello", null);

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
        }
    }

    private String serialize(DOMImplementation domImpl, Document document) {
        DOMImplementationLS ls = (DOMImplementationLS) domImpl;
        LSSerializer lss = ls.createLSSerializer();
        return lss.writeToString(document);
    }

    private void exchangeHello() {
        String ack = receive();
        String hello = getHello() + SSH_NETCONF_TERMINATOR;
        send(hello);
    }

    private void send(String message) {
        try {
            OutputStream outputStream = _session.getStdin();
            outputStream.write(message.getBytes());
            outputStream.flush();
        } catch (Exception e) {
            s_logger.error("Failed to send message: " + e.getMessage());
            throw new CloudRuntimeException("Failed to send message: " + e.getMessage());
        }
    }

    private String receive() {
        byte[] buffer = new byte[8192];
        InputStream inputStream = _session.getStdout();
        try {
            while (true) {
                if (inputStream.available() == 0) {
                    int conditions = _session.waitForCondition(ChannelCondition.STDOUT_DATA
                            | ChannelCondition.STDERR_DATA | ChannelCondition.EOF, 100);

                    if ((conditions & ChannelCondition.TIMEOUT) != 0) {
                        break;
                    }

                    if ((conditions & ChannelCondition.EOF) != 0) {
                        if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0) {
                            break;
                        }
                    }
                }

                while (inputStream.available() > 0) {
                    inputStream.read(buffer);
                }
            }
        } catch (Exception e) {
            s_logger.error("Failed to receive message: " + e.getMessage());
            throw new CloudRuntimeException("Failed to receives message: " + e.getMessage());
        }

        return new String(buffer);
    }
}
