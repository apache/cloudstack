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
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.user.UserContext;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;

@Implementation(description="Creates an ip forwarding rule", responseObject=FirewallRuleResponse.class)
public class CreateIpForwardingRuleCmd extends BaseAsyncCreateCmd implements PortForwardingRule {
    public static final Logger s_logger = Logger.getLogger(CreateIpForwardingRuleCmd.class.getName());

    private static final String s_name = "createipforwardingruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.IP_ADDRESS, type=CommandType.STRING, required=true, description="the public IP address of the forwarding rule, already associated via associateIp")
    private String ipAddress;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, required=true, description="the ID of the virtual machine for the forwarding rule")
    private Long virtualMachineId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getIpAddress() {
        return ipAddress;
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
    public void execute(){ 
        boolean result;
        try {
            result = _rulesService.applyPortForwardingRules(new Ip(ipAddress), UserContext.current().getAccount());
        } catch (Exception e) {
            s_logger.error("Unable to apply port forwarding rules", e);
            _rulesService.revokePortForwardingRule(getEntityId(), true);
            result = false;
        }
        if (result) {
            PortForwardingRule rule = _entityMgr.findById(PortForwardingRule.class, getEntityId());
            FirewallRuleResponse fwResponse = _responseGenerator.createFirewallRuleResponse(rule);
            fwResponse.setResponseName(getName());
            this.setResponseObject(fwResponse);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Error in creating ip forwarding rule on the domr");
        }
       
    }

	@Override
	public void callCreate() {
		PortForwardingRule rule;
        try {
            rule = _rulesService.createPortForwardingRule(this, virtualMachineId);
        } catch (NetworkRuleConflictException e) {
            s_logger.info("Unable to create Port Forwarding Rule due to " + e.getMessage());
            throw new ServerApiException(BaseCmd.NETWORK_RULE_CONFLICT_ERROR, e.getMessage());
        }
        
        this.setEntityId(rule.getId());
	}

    @Override
    public long getEntityOwnerId() {
        return _entityMgr.findById(PortForwardingRule.class, getEntityId()).getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NET_RULE_ADD;
    }

    @Override
    public String getEventDescription() {
        return  ("Creating an ipforwarding 1:1 NAT rule for "+ipAddress+" with virtual machine:"+virtualMachineId);
    }

    @Override
    public long getId() {
        throw new UnsupportedOperationException("Don't call me");
    }

    @Override
    public String getXid() {
        return null;
    }

    @Override
    public Ip getSourceIpAddress() {
        return new Ip(ipAddress);
    }

    @Override
    public int getSourcePortStart() {
        return -1;
    }

    @Override
    public int getSourcePortEnd() {
        return -1;
    }

    @Override
    public String getProtocol() {
        return NetUtils.NAT_PROTO;
    }

    @Override
    public Purpose getPurpose() {
        return Purpose.PortForwarding;
    }

    @Override
    public State getState() {
        throw new UnsupportedOperationException("Don't call me");
    }

    @Override
    public long getNetworkId() {
        return -1;
    }

    @Override
    public long getDomainId() {
        throw new UnsupportedOperationException("Don't call me");
    }

    @Override
    public Ip getDestinationIpAddress() {
        return null;
    }

    @Override
    public int getDestinationPortStart() {
        return -1;
    }

    @Override
    public int getDestinationPortEnd() {
        return -1;
    }

    @Override
    public long getAccountId() {
        throw new UnsupportedOperationException("Get the account id from network");
    }

}
