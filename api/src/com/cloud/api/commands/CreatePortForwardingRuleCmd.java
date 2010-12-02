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
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.FirewallRuleResponse;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.user.UserContext;
import com.cloud.utils.net.Ip;

@Implementation(description="Creates a port forwarding rule", responseObject=FirewallRuleResponse.class)
public class CreatePortForwardingRuleCmd extends BaseCmd implements PortForwardingRule {
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

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }
    
    @Override
    public void execute() throws ResourceUnavailableException {
        try {
            UserContext callerContext = UserContext.current();
            
            PortForwardingRule result = _rulesService.createPortForwardingRule(this, virtualMachineId, callerContext.getAccount());
            if (result == null) {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "An existing rule for ipAddress / port / protocol of " + ipAddress + " / " + publicPort + " / " + protocol + " exits.");
            }
            boolean success = false;
            try {
                success = _rulesService.applyPortForwardingRules(result.getSourceIpAddress(), callerContext.getAccount());
            }  finally {
                if (!success) {
                    _rulesService.revokePortForwardingRule(result.getId(), true, callerContext.getAccount());
                }
            }
            FirewallRuleResponse fwResponse = _responseGenerator.createFirewallRuleResponse(result);
            fwResponse.setResponseName(getName());
            setResponseObject(fwResponse);
        } catch (NetworkRuleConflictException ex) {
            throw new ServerApiException(BaseCmd.NETWORK_RULE_CONFLICT_ERROR, ex.getMessage());
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
    public long getAccountId() {
        throw new UnsupportedOperationException("Get the account id from network");
    }

    @Override
    public long getDomainId() {
        throw new UnsupportedOperationException("Get the domain id from network");
    }

    @Override
    public Ip getDestinationIpAddress() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int getDestinationPortStart() {
        return Integer.parseInt(privatePort);
    }

    @Override
    public int getDestinationPortEnd() {
        return Integer.parseInt(privatePort);
    }
}
