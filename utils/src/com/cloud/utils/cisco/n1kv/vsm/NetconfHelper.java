package com.cloud.utils.cisco.n1kv.vsm;

import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.cloud.utils.Pair;
import com.cloud.utils.ssh.*;
import com.trilead.ssh2.Session;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.ChannelCondition;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.BindingType;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.PortProfileType;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.SwitchPortMode;
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

    public void queryStatus() throws CloudRuntimeException {
        // This command is used to query the server status.
        String status = "<?xml version=\"1.0\"?>"
                + "<nc:rpc message-id=\"1\" xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0"
                + "\"xmlns=\"http://www.cisco.com/nxos:1.0:xml\">" + "  <nc:get>"
                + "    <nc:filter type=\"subtree\">" + "      <show>" + "        <xml>"
                + "          <server>" + "            <status/>" + "          </server>"
                + "        </xml>" + "      </show>" + "    </nc:filter>" + "  </nc:get>"
                + "</nc:rpc>" + SSH_NETCONF_TERMINATOR;
        send(status);
        // parse the rpc reply.
        parseReply(receive());
    }

    public void addPortProfile(String name, PortProfileType type, BindingType binding,
            SwitchPortMode mode, int vlanid) throws CloudRuntimeException {
        String command = VsmCommand.getAddPortProfile(name, type, binding, mode, vlanid);
        if (command != null) {
            command = command.concat(SSH_NETCONF_TERMINATOR);
            send(command);
            // parse the rpc reply.
            parseReply(receive());
        } else {
            throw new CloudRuntimeException("Error generating rpc request for adding port profile.");
        }
    }

    public void updatePortProfile(String name, SwitchPortMode mode,
            List<Pair<VsmCommand.OperationType, String>> params) throws CloudRuntimeException {
        String command = VsmCommand.getUpdatePortProfile(name, mode, params);
        if (command != null) {
            command = command.concat(SSH_NETCONF_TERMINATOR);
            send(command);
            // parse the rpc reply.
            parseReply(receive());
        } else {
            throw new CloudRuntimeException("Error generating rpc request for updating port profile.");
        }
    }

    public void deletePortProfile(String name) throws CloudRuntimeException {
        String command = VsmCommand.getDeletePortProfile(name);
        if (command != null) {
            command = command.concat(SSH_NETCONF_TERMINATOR);
            send(command);
            // parse the rpc reply.
            parseReply(receive());
        } else {
            throw new CloudRuntimeException("Error generating rpc request for deleting port profile.");
        }
    }

    public void addPolicyMap(String name, int averageRate, int maxRate, int burstRate)
            throws CloudRuntimeException {
        String command = VsmCommand.getPolicyMap(name, averageRate, maxRate, burstRate);
        if (command != null) {
            command = command.concat(SSH_NETCONF_TERMINATOR);
            send(command);
            // parse the rpc reply.
            parseReply(receive());
        } else {
            throw new CloudRuntimeException("Error generating rpc request for adding/updating policy map.");
        }
    }

    public void deletePolicyMap(String name) throws CloudRuntimeException {
        String command = VsmCommand.getDeletePolicyMap(name);
        if (command != null) {
            command = command.concat(SSH_NETCONF_TERMINATOR);
            send(command);
            // parse the rpc reply.
            parseReply(receive());
        } else {
            throw new CloudRuntimeException("Error generating rpc request for deleting policy map.");
        }
    }

    public void updatePolicyMap(String name, int averageRate, int maxRate, int burstRate)
            throws CloudRuntimeException {
        // Add and update of policy map work in the exact same way.
        addPolicyMap(name, averageRate, maxRate, burstRate);
    }

    public void attachServicePolicy(String policyMap, String portProfile)
            throws CloudRuntimeException {
        String command = VsmCommand.getAttachServicePolicy(policyMap, portProfile);
        if (command != null) {
            command = command.concat(SSH_NETCONF_TERMINATOR);
            send(command);
            // parse the rpc reply.
            parseReply(receive());
        } else {
            throw new CloudRuntimeException("Error generating rpc request for adding policy map.");
        }
    }

    private void exchangeHello() {
        String ack = receive();
        String hello = VsmCommand.getHello() + SSH_NETCONF_TERMINATOR;
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
                            | ChannelCondition.STDERR_DATA | ChannelCondition.EOF, 2000);

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

    private void parseReply(String reply) throws CloudRuntimeException {
        reply = reply.trim();
        if (reply.endsWith(SSH_NETCONF_TERMINATOR)) {
            reply = reply.substring(0, reply.length() - (new String(SSH_NETCONF_TERMINATOR).length()));
        }
        else {
            throw new CloudRuntimeException("Malformed response from vsm" + reply);
        }

        VsmResponse response = new VsmResponse(reply);
        if (!response.isResponseOk()) {
            throw new CloudRuntimeException(response.toString());
        }
    }
}
