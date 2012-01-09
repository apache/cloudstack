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

public class DhcpdResource extends ExternalDhcpResourceBase {
	private static final Logger s_logger = Logger.getLogger(DhcpdResource.class);
	
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		com.trilead.ssh2.Connection sshConnection = null;
		try {
			super.configure(name, params);
			s_logger.debug(String.format("Trying to connect to DHCP server(IP=%1$s, username=%2$s, password=%3$s)", _ip, _username, "******"));
			sshConnection = SSHCmdHelper.acquireAuthorizedConnection(_ip, _username, _password);
			if (sshConnection == null) {
				throw new ConfigurationException(
						String.format("Cannot connect to DHCP server(IP=%1$s, username=%2$s, password=%3$s", _ip, _username, "******"));
			}

			if (!SSHCmdHelper.sshExecuteCmd(sshConnection, "[ -f '/usr/sbin/dhcpd' ]")) {
				throw new ConfigurationException("Cannot find dhcpd.conf /etc/dhcpd.conf at  on " + _ip);
			}

			SCPClient scp = new SCPClient(sshConnection);

			String editHosts = "scripts/network/exdhcp/dhcpd_edithosts.py";
			String editHostsPath = Script.findScript("", editHosts);
			if (editHostsPath == null) {
				throw new ConfigurationException("Can not find script dnsmasq_edithosts.sh at " + editHosts);
			}
			scp.put(editHostsPath, "/usr/bin/", "0755");
			
			String prepareDhcpdScript = "scripts/network/exdhcp/prepare_dhcpd.sh";
			String prepareDhcpdScriptPath = Script.findScript("", prepareDhcpdScript);
			if (prepareDhcpdScriptPath == null) {
				throw new ConfigurationException("Can not find prepare_dhcpd.sh at " + prepareDhcpdScriptPath);
			}
			scp.put(prepareDhcpdScriptPath, "/usr/bin/", "0755");
			
			//TODO: tooooooooooooooo ugly here!!!
			String[] ips = _ip.split("\\.");
			ips[3] = "0";
			StringBuffer buf = new StringBuffer();
			int i;
			for (i=0;i<ips.length-1;i++) {
				buf.append(ips[i]).append(".");
			}
			buf.append(ips[i]);
			String subnet = buf.toString();
			String cmd = String.format("sh /usr/bin/prepare_dhcpd.sh %1$s", subnet);
			if (!SSHCmdHelper.sshExecuteCmd(sshConnection, cmd)) {
				throw new ConfigurationException("prepare Dhcpd at " + _ip + " failed, command:" + cmd);
			}	
			
			s_logger.debug("Dhcpd resource configure successfully");
			return true;
		} catch (Exception e) {
			s_logger.debug("Dhcpd resorce configure failed", e);
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
			String addDhcp = String.format("python /usr/bin/dhcpd_edithosts.py %1$s %2$s %3$s %4$s %5$s %6$s",
					cmd.getVmMac(), cmd.getVmIpAddress(), cmd.getVmName(), cmd.getDns(), cmd.getGateway(), cmd.getNextServer());
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
