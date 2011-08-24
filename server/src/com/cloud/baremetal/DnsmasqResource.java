/**
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.baremetal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.utils.script.Script;
import com.cloud.utils.ssh.SSHCmdHelper;
import com.cloud.vm.VirtualMachine.State;
import com.trilead.ssh2.SCPClient;

public class DnsmasqResource extends ExternalDhcpResourceBase {
	private static final Logger s_logger = Logger.getLogger(DnsmasqResource.class);

	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		com.trilead.ssh2.Connection sshConnection = null;
		try {
			super.configure(name, params);
			s_logger.debug(String.format("Trying to connect to DHCP server(IP=%1$s, username=%2$s, password=%3$s)", _ip, _username, _password));
			sshConnection = SSHCmdHelper.acquireAuthorizedConnection(_ip, _username, _password);
			if (sshConnection == null) {
				throw new ConfigurationException(
						String.format("Cannot connect to DHCP server(IP=%1$s, username=%2$s, password=%3$s", _ip, _username, _password));
			}

			if (!SSHCmdHelper.sshExecuteCmd(sshConnection, "[ -f '/usr/sbin/dnsmasq' ]")) {
				throw new ConfigurationException("Cannot find dnsmasq at /usr/sbin/dnsmasq on " + _ip);
			}

			SCPClient scp = new SCPClient(sshConnection);
			
			String editHosts = "scripts/network/exdhcp/dnsmasq_edithosts.sh";
			String editHostsPath = Script.findScript("", editHosts);
			if (editHostsPath == null) {
				throw new ConfigurationException("Can not find script dnsmasq_edithosts.sh at " + editHosts);
			}
			scp.put(editHostsPath, "/usr/bin/", "0755");
			
			String prepareDnsmasq = "scripts/network/exdhcp/prepare_dnsmasq.sh";
			String prepareDnsmasqPath = Script.findScript("", prepareDnsmasq);
			if (prepareDnsmasqPath == null) {
				throw new ConfigurationException("Can not find script prepare_dnsmasq.sh at " + prepareDnsmasq);
			}
			scp.put(prepareDnsmasqPath, "/usr/bin/", "0755");
			
			String prepareCmd = String.format("sh /usr/bin/prepare_dnsmasq.sh %1$s %2$s %3$s", _gateway, _dns, _ip);
			if (!SSHCmdHelper.sshExecuteCmd(sshConnection, prepareCmd)) {
				throw new ConfigurationException("prepare dnsmasq at " + _ip + " failed");
			}
			
			s_logger.debug("Dnsmasq resource configure successfully");
			return true;
		} catch (Exception e) {
			s_logger.debug("Dnsmasq resorce configure failed", e);
			throw new ConfigurationException(e.getMessage());
		} finally {
			SSHCmdHelper.releaseSshConnection(sshConnection);
		}
	}
	
	@Override
	public PingCommand getCurrentStatus(long id) {
		com.trilead.ssh2.Connection sshConnection = SSHCmdHelper.acquireAuthorizedConnection(_ip, _username, _password);
		if (sshConnection == null) {
			return null;
		} else {
			SSHCmdHelper.releaseSshConnection(sshConnection);
			return new PingRoutingCommand(getType(), id, new HashMap<String, State>());
		}
	}

	Answer execute(DhcpEntryCommand cmd) {
		com.trilead.ssh2.Connection sshConnection = null;
		try {
			sshConnection = SSHCmdHelper.acquireAuthorizedConnection(_ip, _username, _password);
			if (sshConnection == null) {
				return new Answer(cmd, false, "ssh authenticate failed");
			}
			String addDhcp = String.format("/usr/bin/dnsmasq_edithosts.sh %1$s %2$s %3$s", cmd.getVmMac(), cmd.getVmIpAddress(), cmd.getVmName());
			if (!SSHCmdHelper.sshExecuteCmd(sshConnection, addDhcp)) {
				return new Answer(cmd, false, "add Dhcp entry failed");
			} else {
				return new Answer(cmd);
			}
		} finally {
			SSHCmdHelper.releaseSshConnection(sshConnection);
		}
	}
	
	@Override
	public Answer executeRequest(Command cmd) {
		if (cmd instanceof DhcpEntryCommand) {
			return execute((DhcpEntryCommand)cmd);
		} else {
			return super.executeRequest(cmd);
		}
	}
}
