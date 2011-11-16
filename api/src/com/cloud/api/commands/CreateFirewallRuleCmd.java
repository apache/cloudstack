/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.FirewallResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.rules.FirewallRule;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.net.NetUtils;

@Implementation(description = "Creates a firewall rule for a given ip address", responseObject = FirewallResponse.class)
public class CreateFirewallRuleCmd extends BaseAsyncCreateCmd implements FirewallRule {
    public static final Logger s_logger = Logger.getLogger(CreateFirewallRuleCmd.class.getName());

    private static final String s_name = "createfirewallruleresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @IdentityMapper(entityTableName="user_ip_address")
    @Parameter(name = ApiConstants.IP_ADDRESS_ID, type = CommandType.LONG, description = "the IP address id of the port forwarding rule")
    private Long ipAddressId;

    @Parameter(name = ApiConstants.PROTOCOL, type = CommandType.STRING, required = true, description = "the protocol for the firewall rule. Valid values are TCP/UDP/ICMP.")
    private String protocol;

    @Parameter(name = ApiConstants.START_PORT, type = CommandType.INTEGER, description = "the starting port of firewall rule")
    private Integer publicStartPort;

    @Parameter(name = ApiConstants.END_PORT, type = CommandType.INTEGER, description = "the ending port of firewall rule")
    private Integer publicEndPort;
    
    @Parameter(name = ApiConstants.CIDR_LIST, type = CommandType.LIST, collectionType = CommandType.STRING, description = "the cidr list to forward traffic from")
    private List<String> cidrlist;
    
    @Parameter(name = ApiConstants.ICMP_TYPE, type = CommandType.INTEGER, description = "type of the icmp message being sent")
    private Integer icmpType;

    @Parameter(name = ApiConstants.ICMP_CODE, type = CommandType.INTEGER, description = "error code for this icmp message")
    private Integer icmpCode;
    
    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, description = "type of firewallrule: system/user")
    private String type;
    
    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////
    
    public String getEntityTable() {
    	return "firewall_rules";
    }

    public Long getIpAddressId() {
        return ipAddressId;
    }

    @Override
    public String getProtocol() {
        return protocol.trim();
    }

    public List<String> getSourceCidrList() {
        if (cidrlist != null) {
            return cidrlist;
        } else {
            List<String> oneCidrList = new ArrayList<String>();
            oneCidrList.add(NetUtils.ALL_CIDRS);
            return oneCidrList;
        }
        
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    public void setSourceCidrList(List<String> cidrs){
        cidrlist = cidrs;
    }

    @Override
    public void execute() throws ResourceUnavailableException {
        UserContext callerContext = UserContext.current();
        boolean success = false;
        FirewallRule rule = _entityMgr.findById(FirewallRule.class, getEntityId());
        try {
            UserContext.current().setEventDetails("Rule Id: " + getEntityId());
            success = _firewallService.applyFirewallRules(rule.getSourceIpAddressId(), callerContext.getCaller());

            // State is different after the rule is applied, so get new object here
            rule = _entityMgr.findById(FirewallRule.class, getEntityId());
            FirewallResponse fwResponse = new FirewallResponse(); 
            if (rule != null) {
                fwResponse = _responseGenerator.createFirewallResponse(rule);
                setResponseObject(fwResponse);
            }
            fwResponse.setResponseName(getCommandName());
        } finally {
            if (!success || rule == null) {
                _firewallService.revokeFirewallRule(getEntityId(), true);
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create firewall rule");
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
    public long getSourceIpAddressId() {
        return ipAddressId;
    }

    @Override
    public Integer getSourcePortStart() {
        if (publicStartPort != null) {
            return publicStartPort.intValue();
        }
        return null;
    }

    @Override
    public Integer getSourcePortEnd() {
        if (publicEndPort == null) {
            if (publicStartPort != null) {
                return publicStartPort.intValue();
            }
        } else {
            return publicEndPort.intValue();
        }
        
        return null;
    }

    @Override
    public Purpose getPurpose() {
        return Purpose.Firewall;
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
        Account account = UserContext.current().getCaller();

        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public long getDomainId() {
        IpAddress ip = _networkService.getIp(ipAddressId);
        return ip.getDomainId();
    }

    @Override
    public void create() {
        if (getSourceCidrList() != null) {
            for (String cidr: getSourceCidrList()){
                if (!NetUtils.isValidCIDR(cidr)){
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Source cidrs formatting error " + cidr); 
                }
            }
        }

        try {
            FirewallRule result = _firewallService.createFirewallRule(this);
            setEntityId(result.getId());
        } catch (NetworkRuleConflictException ex) {
            s_logger.info("Network rule conflict: " + ex.getMessage());
            s_logger.trace("Network Rule Conflict: ", ex);
            throw new ServerApiException(BaseCmd.NETWORK_RULE_CONFLICT_ERROR, ex.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_FIREWALL_OPEN;
    }

    @Override
    public String getEventDescription() {
        IpAddress ip = _networkService.getIp(ipAddressId);
        return ("Createing firewall rule for Ip: " + ip.getAddress() + " for protocol:" + this.getProtocol());
    }

    @Override
    public long getAccountId() {
        IpAddress ip = _networkService.getIp(ipAddressId);
        return ip.getAccountId();
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return getIp().getAssociatedWithNetworkId();
    }

    private IpAddress getIp() {
        IpAddress ip = _networkService.getIp(ipAddressId);
        if (ip == null) {
            throw new InvalidParameterValueException("Unable to find ip address by id " + ipAddressId);
        }
        return ip;
    }
    
    @Override
    public Integer getIcmpCode() {
        if (icmpCode != null) {
            return icmpCode;
        } else if (protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO)) {
            return -1;
        }
        return null;
    }
    
    @Override
    public Integer getIcmpType() {
        if (icmpType != null) {
            return icmpType;
        } else if (protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO)) {
                return -1;
            
        }
        return null;
    }

    @Override
    public Long getRelated() {
        return null;
    }

	@Override
	public FirewallRuleType getType() {
		if (type != null && type.equalsIgnoreCase("system")) {
			return FirewallRuleType.System;
		} else {
			return FirewallRuleType.User;
		}
	}

}
