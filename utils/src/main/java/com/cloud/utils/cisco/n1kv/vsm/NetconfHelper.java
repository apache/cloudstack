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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.log4j.Logger;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;

import com.cloud.utils.Pair;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.BindingType;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.PortProfileType;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.SwitchPortMode;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SSHCmdHelper;

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
            throw new CloudRuntimeException("Failed to connect to SSH server: " + _connection.getHostname());
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
        String status =
            "<?xml version=\"1.0\"?>" + "<nc:rpc message-id=\"1\" xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0" + "\"xmlns=\"http://www.cisco.com/nxos:1.0:xml\">" +
                "  <nc:get>" + "    <nc:filter type=\"subtree\">" + "      <show>" + "        <xml>" + "          <server>" + "            <status/>" +
                "          </server>" + "        </xml>" + "      </show>" + "    </nc:filter>" + "  </nc:get>" + "</nc:rpc>" + SSH_NETCONF_TERMINATOR;
        send(status);
        // parse the rpc reply.
        parseOkReply(receive());
    }

    public void addPortProfile(String name, PortProfileType type, BindingType binding, SwitchPortMode mode, int vlanid, String vdc, String espName)
        throws CloudRuntimeException {
        String command = VsmCommand.getAddPortProfile(name, type, binding, mode, vlanid, vdc, espName);
        if (command != null) {
            command = command.concat(SSH_NETCONF_TERMINATOR);
            parseOkReply(sendAndReceive(command));
        } else {
            throw new CloudRuntimeException("Error generating rpc request for adding port profile.");
        }
    }

    public void addPortProfile(String name, PortProfileType type, BindingType binding, SwitchPortMode mode, int vlanid) throws CloudRuntimeException {
        String command = VsmCommand.getAddPortProfile(name, type, binding, mode, vlanid);
        if (command != null) {
            command = command.concat(SSH_NETCONF_TERMINATOR);
            parseOkReply(sendAndReceive(command));
        } else {
            throw new CloudRuntimeException("Error generating rpc request for adding port profile.");
        }
    }

    public void updatePortProfile(String name, SwitchPortMode mode, List<Pair<VsmCommand.OperationType, String>> params) throws CloudRuntimeException {
        String command = VsmCommand.getUpdatePortProfile(name, mode, params);
        if (command != null) {
            command = command.concat(SSH_NETCONF_TERMINATOR);
            parseOkReply(sendAndReceive(command));
        } else {
            throw new CloudRuntimeException("Error generating rpc request for updating port profile.");
        }
    }

    public void deletePortProfile(String name) throws CloudRuntimeException {
        String command = VsmCommand.getDeletePortProfile(name);
        if (command != null) {
            command = command.concat(SSH_NETCONF_TERMINATOR);
            parseOkReply(sendAndReceive(command));
        } else {
            throw new CloudRuntimeException("Error generating rpc request for deleting port profile.");
        }
    }

    public void addPolicyMap(String name, int averageRate, int maxRate, int burstRate) throws CloudRuntimeException {
        String command = VsmCommand.getAddPolicyMap(name, averageRate, maxRate, burstRate);
        if (command != null) {
            command = command.concat(SSH_NETCONF_TERMINATOR);
            parseOkReply(sendAndReceive(command));
        } else {
            throw new CloudRuntimeException("Error generating rpc request for adding/updating policy map.");
        }
    }

    public void deletePolicyMap(String name) throws CloudRuntimeException {
        String command = VsmCommand.getDeletePolicyMap(name);
        if (command != null) {
            command = command.concat(SSH_NETCONF_TERMINATOR);
            parseOkReply(sendAndReceive(command));
        } else {
            throw new CloudRuntimeException("Error generating rpc request for deleting policy map.");
        }
    }

    public void updatePolicyMap(String name, int averageRate, int maxRate, int burstRate) throws CloudRuntimeException {
        // Add and update of policy map work in the exact same way.
        addPolicyMap(name, averageRate, maxRate, burstRate);
    }

    public void attachServicePolicy(String policyMap, String portProfile) throws CloudRuntimeException {
        String command = VsmCommand.getServicePolicy(policyMap, portProfile, true);
        if (command != null) {
            command = command.concat(SSH_NETCONF_TERMINATOR);
            parseOkReply(sendAndReceive(command));
        } else {
            throw new CloudRuntimeException("Error generating rpc request for adding policy map.");
        }
    }

    public void detachServicePolicy(String policyMap, String portProfile) throws CloudRuntimeException {
        String command = VsmCommand.getServicePolicy(policyMap, portProfile, false);
        if (command != null) {
            command = command.concat(SSH_NETCONF_TERMINATOR);
            parseOkReply(sendAndReceive(command));
        } else {
            throw new CloudRuntimeException("Error generating rpc request for removing policy map.");
        }
    }

    public void addVServiceNode(String vlanId, String ipAddr) throws CloudRuntimeException {
        String command = VsmCommand.getVServiceNode(vlanId, ipAddr);
        if (command != null) {
            command = command.concat(SSH_NETCONF_TERMINATOR);
            parseOkReply(sendAndReceive(command));
        } else {
            throw new CloudRuntimeException("Error generating rpc request for adding vservice node for vlan " + vlanId);
        }
    }

    public PortProfile getPortProfileByName(String name) throws CloudRuntimeException {
        String command = VsmCommand.getPortProfile(name);
        if (command != null) {
            command = command.concat(SSH_NETCONF_TERMINATOR);
            String received = sendAndReceive(command);
            VsmPortProfileResponse response = new VsmPortProfileResponse(received.trim());
            if (!response.isResponseOk()) {
                throw new CloudRuntimeException(response.toString());
            } else {
                return response.getPortProfile();
            }
        } else {
            throw new CloudRuntimeException("Error generating rpc request for getting port profile.");
        }
    }

    public PolicyMap getPolicyMapByName(String name) throws CloudRuntimeException {
        String command = VsmCommand.getPolicyMap(name);
        if (command != null) {
            command = command.concat(SSH_NETCONF_TERMINATOR);
            String received = sendAndReceive(command);
            VsmPolicyMapResponse response = new VsmPolicyMapResponse(received.trim());
            if (!response.isResponseOk()) {
                throw new CloudRuntimeException(response.toString());
            } else {
                return response.getPolicyMap();
            }
        } else {
            throw new CloudRuntimeException("Error generating rpc request for getting policy map.");
        }
    }

    private void exchangeHello() {
        String ack = receive();
        String hello = VsmCommand.getHello() + SSH_NETCONF_TERMINATOR;
        send(hello);
    }

    private String sendAndReceive(String command) {
        String received;
        synchronized (NetconfHelper.class) {
            send(command);
            received = receive();
        }
        return received;
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
        String response = new String("");
        InputStream inputStream = _session.getStdout();

        try {
            Delimiter delimiter = new Delimiter();
            byte[] buffer = new byte[1024];
            int count = 0;

            // Read the input stream till we find the end sequence ']]>]]>'.
            while (true) {
                int data = inputStream.read();
                if (data != -1) {
                    byte[] dataStream = delimiter.parse(data);
                    if (delimiter.endReached()) {
                        response += new String(buffer, 0, count);
                        break;
                    }

                    if (dataStream != null) {
                        for (int i = 0; i < dataStream.length; i++) {
                            buffer[count] = dataStream[i];
                            count++;
                            if (count == 1024) {
                                response += new String(buffer, 0, count);
                                count = 0;
                            }
                        }
                    }
                } else {
                    break;
                }
            }
        } catch (final Exception e) {
            throw new CloudRuntimeException("Error occured while reading from the stream: " + e.getMessage());
        }

        return response;
    }

    private void parseOkReply(String reply) throws CloudRuntimeException {
        VsmOkResponse response = new VsmOkResponse(reply.trim());
        if (!response.isResponseOk()) {
            throw new CloudRuntimeException(response.toString());
        }
    }

    private static class Delimiter {
        private boolean _endReached = false;

        // Used to accumulate response read while searching for end of response.
        private byte[] _gatherResponse = new byte[6];

        // Index into number of bytes read.
        private int _offset = 0;

        // True if ']]>]]>' detected.
        boolean endReached() {
            return _endReached;
        }

        // Parses the input stream and checks if end sequence is reached.
        byte[] parse(int input) throws RuntimeException {
            boolean collect = false;
            byte[] streamRead = null;

            // Check if end sequence matched.
            switch (_offset) {
                case 0:
                    if (input == ']') {
                        collect = true;
                    }
                    break;
                case 1:
                    if (input == ']') {
                        collect = true;
                    }
                    break;
                case 2:
                    if (input == '>') {
                        collect = true;
                    }
                    break;
                case 3:
                    if (input == ']') {
                        collect = true;
                    }
                    break;
                case 4:
                    if (input == ']') {
                        collect = true;
                    }
                    break;
                case 5:
                    if (input == '>') {
                        collect = true;
                        _endReached = true;
                    }
                    break;
                default:
                    throw new RuntimeException("Invalid index value: " + _offset);
            }

            if (collect) {
                _gatherResponse[_offset++] = (byte)input;
            } else {
                // End sequence not yet reached. Return the stream of bytes collected so far.
                streamRead = new byte[_offset + 1];
                for (int index = 0; index < _offset; ++index) {
                    streamRead[index] = _gatherResponse[index];
                }

                streamRead[_offset] = (byte)input;
                _offset = 0;
            }

            return streamRead;
        }
    }
}
