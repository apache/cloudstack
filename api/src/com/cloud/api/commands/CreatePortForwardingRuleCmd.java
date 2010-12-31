/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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

package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.FirewallRuleResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.user.UserContext;
import com.cloud.utils.net.Ip;

@Implementation(description="Creates a port forwarding rule", responseObject=FirewallRuleResponse.class)
public class CreatePortForwardingRuleCmd extends BaseAsyncCreateCmd  implements PortForwardingRule {
    public static final Logger s_logger = Logger.getLogger(CreatePortForwardingRuleCmd.class.getName());

    private static final String s_name = "createportforwardingruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.IP_ADDRESS, type=CommandType.STRING, required=true, description="the IP address of the port forwarding rule")
    private String ipAddress;

    @Parameter(name=ApiConstants.PRIVATE_PORT, type=CommandType.STRING, required=true, description="the private port of the port forwarding rule")
    private String privatePort;

    @Parameter(name=ApiConstants.PROTOCOL, type=CommandType.STRING, required=true, description="the protocol for the port fowarding rule. Valid values are TCP or UDP.")
    private String protocol;

    @Parameter(name=ApiConstants.PUBLIC_PORT, type=CommandType.STRING, required=true, description="	the public port of the port forwarding rule")
    private String publicPort;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, required=true, description="the ID of the virtual machine for the port forwarding rule")
    private Long virtualMachineId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getIpAddress() {
        return ipAddress;
    }

    public String getPrivatePort() {
        return privatePort;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    public String getPublicPort() {
        return publicPort;
    }
    
    @Override
    public long getVirtualMachineId() {
        return virtualMachineId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public void execute() throws ResourceUnavailableException {
        UserContext callerContext = UserContext.current();
    
        boolean success = false;
        PortForwardingRule rule = _entityMgr.findById(PortForwardingRule.class, getEntityId());
        try {
            success = _rulesService.applyPortForwardingRules(rule.getSourceIpAddress(), callerContext.getCaller());
            
            //State is different after the rule is applied, so get new object here
            rule = _entityMgr.findById(PortForwardingRule.class, getEntityId());
            FirewallRuleResponse fwResponse = new FirewallRuleResponse();
            if (rule != null) {
                fwResponse = _responseGenerator.createFirewallRuleResponse(rule);
                setResponseObject(fwResponse);
            }
            fwResponse.setResponseName(getCommandName());
        }  finally {
            if (!success || rule == null) {
                _rulesService.revokePortForwardingRule(getEntityId(), true);
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to apply port forwarding rule");
            }
        }
    }

    @Override
    public long getId() {
        throw new UnsupportedOperationException("database id can only provided by VO objects"); 
    }

    @Override
    public String getXid() {
        // FIXME: We should allow for end user to specify Xid.
        return null;
    }

    @Override
    public Ip getSourceIpAddress() {
        return new Ip(ipAddress);
    }

    @Override
    public int getSourcePortStart() {
        return Integer.parseInt(publicPort);
    }

    @Override
    public int getSourcePortEnd() {
        return Integer.parseInt(publicPort);
    }

    @Override
    public Purpose getPurpose() {
        return Purpose.PortForwarding;
    }

    @Override
    public State getState() {
        throw new UnsupportedOperationException("Should never call me to find the state");
    }

    @Override
    public long getNetworkId() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public long getEntityOwnerId() {
        return _entityMgr.findById(PortForwardingRule.class, getEntityId()).getAccountId();
    }

    @Override
    public long getDomainId() {
        throw new UnsupportedOperationException("Get the domain id from network");
    }

    @Override
    public Ip getDestinationIpAddress() {
        return null;
    }

    @Override
    public int getDestinationPortStart() {
        return Integer.parseInt(privatePort);
    }

    @Override
    public int getDestinationPortEnd() {
        return Integer.parseInt(privatePort);
    }

    @Override
    public void create() {
        try {
            PortForwardingRule result = _rulesService.createPortForwardingRule(this, virtualMachineId);
            setEntityId(result.getId());
        } catch (NetworkRuleConflictException ex) {
            s_logger.info("Network rule conflict: " + ex.getMessage());
            s_logger.trace("Network Rule Conflict: ", ex);
            throw new ServerApiException(BaseCmd.NETWORK_RULE_CONFLICT_ERROR, ex.getMessage());
        }
    }
    
    @Override
    public String getEventType() {
        return EventTypes.EVENT_NET_RULE_ADD;
    }

    @Override
    public String getEventDescription() {
        return  ("Creating an port forwarding  rule for "+ipAddress+" with virtual machine:"+virtualMachineId);
    }

    @Override
    public long getAccountId() {
        throw new UnsupportedOperationException("Get the account id from network");
    }

}
