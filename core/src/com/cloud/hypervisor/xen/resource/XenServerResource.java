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
package com.cloud.hypervisor.xen.resource;


import javax.ejb.Local;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.SecurityIngressRuleAnswer;
import com.cloud.agent.api.SecurityIngressRulesCmd;
import com.cloud.hypervisor.xen.resource.CitrixResourceBase;
import com.xensource.xenapi.VM;
import com.cloud.resource.ServerResource;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Types.XenAPIException;


@Local(value=ServerResource.class)
public class XenServerResource extends CitrixResourceBase {
    private static final Logger s_logger = Logger.getLogger(XenServerResource.class);
    
    public XenServerResource() {
        super();
    }
    
    @Override
    public Answer executeRequest(Command cmd) {       
        if (cmd instanceof SecurityIngressRulesCmd) {
            return execute((SecurityIngressRulesCmd) cmd);
        } else {
            return super.executeRequest(cmd);
        }
    }
    
    @Override
    protected String getGuestOsType(String stdType, boolean bootFromCD) {
    	return CitrixHelper.getXenServerGuestOsType(stdType);
    }

    @Override
    protected void setMemory(Connection conn, VM vm, long memsize) throws XmlRpcException, XenAPIException {
        vm.setMemoryLimits(conn, memsize, memsize, memsize, memsize);
    }   
 
    @Override
    protected String getPatchPath() {
        return "scripts/vm/hypervisor/xenserver/xenserver56";
    } 
    
    @Override
    protected boolean can_bridge_firewall(Connection conn) {   
        return Boolean.valueOf(callHostPlugin(conn, "vmops", "can_bridge_firewall", "host_uuid", _host.uuid));
    }
    
    private Answer execute(SecurityIngressRulesCmd cmd) {
        Connection conn = getConnection();
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Sending network rules command to " + _host.ip);
        }

        if (!_canBridgeFirewall) {
            s_logger.info("Host " + _host.ip + " cannot do bridge firewalling");
            return new SecurityIngressRuleAnswer(cmd, false, "Host " + _host.ip + " cannot do bridge firewalling");
        }
      
        String result = callHostPlugin(conn, "vmops", "network_rules",
                "vmName", cmd.getVmName(),
                "vmIP", cmd.getGuestIp(),
                "vmMAC", cmd.getGuestMac(),
                "vmID", Long.toString(cmd.getVmId()),
                "signature", cmd.getSignature(),
                "seqno", Long.toString(cmd.getSeqNum()),
                "rules", cmd.stringifyRules());

        if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
            s_logger.warn("Failed to program network rules for vm " + cmd.getVmName());
            return new SecurityIngressRuleAnswer(cmd, false, "programming network rules failed");
        } else {
            s_logger.info("Programmed network rules for vm " + cmd.getVmName() + " guestIp=" + cmd.getGuestIp() + ", numrules=" + cmd.getRuleSet().length);
            return new SecurityIngressRuleAnswer(cmd);
        }
    }

}
