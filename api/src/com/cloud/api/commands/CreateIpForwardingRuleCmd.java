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
import com.cloud.api.response.IpForwardingRuleResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.network.IpAddress;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.net.Ip;

@Implementation(description="Creates an ip forwarding rule", responseObject=FirewallRuleResponse.class)
public class CreateIpForwardingRuleCmd extends BaseAsyncCreateCmd implements PortForwardingRule {
    public static final Logger s_logger = Logger.getLogger(CreateIpForwardingRuleCmd.class.getName());

    private static final String s_name = "createipforwardingruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.IP_ADDRESS_ID, type=CommandType.LONG, required=true, description="the public IP address id of the forwarding rule, already associated via associateIp")
    private Long ipAddressId;
    
    @Parameter(name=ApiConstants.START_PORT, type=CommandType.INTEGER, required=true, description="the start port for the rule")
    private Integer startPort;

    @Parameter(name=ApiConstants.END_PORT, type=CommandType.INTEGER, description="the end port for the rule")
    private Integer endPort;
    
    @Parameter(name=ApiConstants.PROTOCOL, type=CommandType.STRING, required=true, description="the protocol for the rule. Valid values are TCP or UDP.")
    private String protocol;
    

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getIpAddressId() {
        return ipAddressId;
    }
    
    public int getStartPort() {
        return startPort;
    }
    
    public int getEndPort() {
        return endPort;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){ 
        boolean result;
        try {
            UserContext.current().setEventDetails("Rule Id: "+getEntityId());
            result = _rulesService.applyPortForwardingRules(ipAddressId, UserContext.current().getCaller());
        } catch (Exception e) {
            s_logger.error("Unable to apply port forwarding rules", e);
            _rulesService.revokePortForwardingRule(getEntityId(), true);
            result = false;
        }
        if (result) {
            PortForwardingRule rule = _entityMgr.findById(PortForwardingRule.class, getEntityId());
            IpForwardingRuleResponse fwResponse = _responseGenerator.createIpForwardingRuleResponse(rule);
            fwResponse.setResponseName(getCommandName());
            this.setResponseObject(fwResponse);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Error in creating ip forwarding rule on the domr");
        }
       
    }

	@Override
	public void create() {
		PortForwardingRule rule;
        try {
            rule = _rulesService.createPortForwardingRule(this, getVirtualMachineId(), true);
        } catch (NetworkRuleConflictException e) {
            s_logger.info("Unable to create Port Forwarding Rule due to " + e.getMessage());
            throw new ServerApiException(BaseCmd.NETWORK_RULE_CONFLICT_ERROR, e.getMessage());
        }
        
        this.setEntityId(rule.getId());
	}

    @Override
    public long getEntityOwnerId() {
        Account account = UserContext.current().getCaller();

        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NET_RULE_ADD;
    }

    @Override
    public String getEventDescription() {
        IpAddress ip = _networkService.getIp(ipAddressId);
        return  ("Applying an ipforwarding 1:1 NAT rule for Ip: "+ip.getAddress()+" with virtual machine:"+ getVirtualMachineId());
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
    public long getSourceIpAddressId() {
        return ipAddressId;
    }

    @Override
    public int getSourcePortStart() {
        return startPort;
    }

    @Override
    public int getSourcePortEnd() {
        if (endPort == null) {
            return startPort;
        } else {
            return endPort;
        }
    }

    @Override
    public String getProtocol() {
        return protocol;
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
        IpAddress ip = _networkService.getIp(ipAddressId);
        return ip.getDomainId();
    }

    @Override
    public Ip getDestinationIpAddress() {
        return null;
    }

    @Override
    public int getDestinationPortStart() {
        return startPort;
    }

    @Override
    public int getDestinationPortEnd() {
        if (endPort == null) {
            return startPort;
        } else {
            return endPort;
        }
    }

    @Override
    public long getAccountId() {
        IpAddress ip = _networkService.getIp(ipAddressId);
        return ip.getAccountId();
    }
    
    @Override
    public boolean isOneToOneNat() {
        return true;
    }
    
    @Override
    public long getVirtualMachineId() {
        IpAddress ip = _networkService.getIp(ipAddressId);
        if (ip == null) {
            throw new InvalidParameterValueException("Ip address id=" + ipAddressId + " doesn't exist in the system");
        } else if (ip.isOneToOneNat() && ip.getAssociatedWithVmId() != null){
            return _networkService.getIp(ipAddressId).getAssociatedWithVmId();
        } else {
            throw new InvalidParameterValueException("Ip address id=" + ipAddressId + " doesn't have 1-1 Nat feature enabled");
        }  
    }
    
    @Override
    public String getSyncObjType() {
        return this.ipAddressSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return ipAddressId;
    }

}
