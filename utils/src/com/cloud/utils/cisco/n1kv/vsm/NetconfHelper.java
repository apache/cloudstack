package com.cloud.utils.cisco.n1kv.vsm;

import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;

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

	public NetconfHelper (String ip, String username, String password) throws CloudRuntimeException {
		_connection = SSHCmdHelper.acquireAuthorizedConnection(ip, username, password);
		if (_connection == null) {
			throw new CloudRuntimeException("Error opening ssh connection.");
		}

		try	{
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

	public void queryStatus() {
		// FIXME: Use an xml parser to generate this request.
		String status = "<?xml version=\"1.0\"?>" +
				"<nc:rpc message-id=\"1\" xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0" +
				"\"xmlns=\"http://www.cisco.com/nxos:1.0:xml\">" +
				"  <nc:get>" +
				"    <nc:filter type=\"subtree\">" +
				"      <show>" +
				"        <xml>" +
				"          <server>" +
				"            <status/>" +
				"          </server>" +
				"        </xml>" +
				"      </show>" +
				"    </nc:filter>" +
				"  </nc:get>" +
				"</nc:rpc>" +
				SSH_NETCONF_TERMINATOR;
		send(status);
		String reply = receive();
	}

	public boolean addPortProfile(String name, int vlan) {
		// FIXME: Use an xml parser to generate this request.
		String command = "<?xml version=\"1.0\"?>" +
				"<nf:rpc xmlns=\"http://www.cisco.com/nxos:1.0:ppm\" " +
				"xmlns:nf=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"110\">" +
				"  <nf:edit-config>" +
				"    <nf:target>" +
				"      <nf:running/>" +
				"    </nf:target>" +
				"    <nf:config>" +
				"      <nxos:configure xmlns:nxos=\"http://www.cisco.com/nxos:1.0:ppm\">" +
				"        <nxos:__XML__MODE__exec_configure>" +
				"          <port-profile>" +
				"                <type>" +
				"                  <vethernet>" +
				"                    <name>" +
				"                      <__XML__PARAM_value isKey=\"true\">@name</__XML__PARAM_value>" +
				"                        <__XML__MODE_port-prof>" +
				"                          <switchport>" +
				"                            <mode>" +
				"                              <access/>" +
				"                            </mode>" +
				"                          </switchport>" +
				"                          <switchport>" +
				"                            <access>" +
				"                            <vlan>" +
				"                            <vlan-id-create-delete>" +
				"                            <__XML__PARAM_value>@vlan</__XML__PARAM_value>" +
				"                            </vlan-id-create-delete>" +
				"                            </vlan>" +
				"                            </access>" +
				"                          </switchport>" +
				"                          <vmware>" +
				"                            <port-group/>" +
				"                          </vmware>" +
				"                          <state>" +
				"                            <enabled/>" +
				"                          </state>" +
				"                          <no>" +
				"                            <shutdown/>" +
				"                          </no>" +
				"                        </__XML__MODE_port-prof>" +
				"                      </name>" +
				"                    </vethernet>" +
				"                  </type>" +
				"                </port-profile>" +
				"              </nxos:__XML__MODE__exec_configure>" +
				"          </nxos:configure>" +
				"        </nf:config>" +
				"	</nf:edit-config>" +
				"</nf:rpc>" +
				SSH_NETCONF_TERMINATOR;
		command = command.replace("@name", name);
		command = command.replace("@vlan", Integer.toString(vlan));
		send(command);
		// parse the rpc reply and the return success or failure.
		String reply = receive();
		return true;
	}

	public boolean deletePortProfile(String name) {
		// FIXME: Use an xml parser to generate this request.
		String command = "<?xml version=\"1.0\"?>" +
				"<nf:rpc xmlns=\"http://www.cisco.com/nxos:1.0:ppm\" " +
				"xmlns:nf=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"110\">" +
				"  <nf:edit-config>" +
				"    <nf:target>" +
				"      <nf:running/>" +
				"    </nf:target>" +
				"    <nf:config>" +
				"      <nxos:configure xmlns:nxos=\"http://www.cisco.com/nxos:1.0:ppm\">" +
				"        <nxos:__XML__MODE__exec_configure>" +
				"          <no>" +
				"            <port-profile>" +
				"              <name>" +
				"                <__XML__PARAM_value isKey=\"true\">@name</__XML__PARAM_value>" +
				"              </name>" +
				"            </port-profile>" +
				"          </no>" +
				"        </nxos:__XML__MODE__exec_configure>" +
				"      </nxos:configure>" +
				"    </nf:config>" +
				"  </nf:edit-config>" +
				"</nf:rpc>" +
				SSH_NETCONF_TERMINATOR;
		command = command.replace("@name", name);
		send(command);
		// parse the rpc reply and the return success or failure.
		String reply = receive();
		return true;
	}

	private void exchangeHello() {
		String ack = receive();
		String hello = "<?xml version=\"1.0\"?>" +
				"<nc:hello xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" +
				"  <nc:capabilities>" +
				"    <nc:capability>urn:ietf:params:xml:ns:netconf:base:1.0</nc:capability>" +
				"  </nc:capabilities>" +
				"</nc:hello>" +
				SSH_NETCONF_TERMINATOR;
		send(hello);
	}

	private void send(String message) {
		try	{
			OutputStream outputStream = _session.getStdin();
			outputStream.write(message.getBytes());
			outputStream.flush();
		} catch(Exception e) {
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
					int conditions = _session.waitForCondition(ChannelCondition.STDOUT_DATA |
							ChannelCondition.STDERR_DATA | ChannelCondition.EOF, 500);

					if ((conditions & ChannelCondition.TIMEOUT) != 0) {
						break;
					}

					if ((conditions & ChannelCondition.EOF) != 0) {
						if ((conditions & (ChannelCondition.STDOUT_DATA |
							 ChannelCondition.STDERR_DATA)) == 0) {
							break;
						}
					}
				}

				while (inputStream.available() > 0) {
					inputStream.read(buffer);
				}
			}
		} catch(Exception e) {
			s_logger.error("Failed to receive message: " + e.getMessage());
			throw new CloudRuntimeException("Failed to receives message: " + e.getMessage());
		}

		return new String(buffer);
	}
}
