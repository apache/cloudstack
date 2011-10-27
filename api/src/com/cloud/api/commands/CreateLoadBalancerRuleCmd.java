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

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.LoadBalancerResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.net.NetUtils;

@Implementation(description="Creates a load balancer rule", responseObject=LoadBalancerResponse.class)
public class CreateLoadBalancerRuleCmd extends BaseAsyncCreateCmd  /*implements LoadBalancer */{
    public static final Logger s_logger = Logger.getLogger(CreateLoadBalancerRuleCmd.class.getName());

    private static final String s_name = "createloadbalancerruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ALGORITHM, type=CommandType.STRING, required=true, description="load balancer algorithm (source, roundrobin, leastconn)")
    private String algorithm;

    @Parameter(name=ApiConstants.DESCRIPTION, type=CommandType.STRING, description="the description of the load balancer rule", length=4096)
    private String description;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="name of the load balancer rule")
    private String loadBalancerRuleName;

    @Parameter(name=ApiConstants.PRIVATE_PORT, type=CommandType.INTEGER, required=true, description="the private port of the private ip address/virtual machine where the network traffic will be load balanced to")
    private Integer privatePort;

    @Parameter(name=ApiConstants.PUBLIC_IP_ID, type=CommandType.LONG, required=false, description="public ip address id from where the network traffic will be load balanced from")
    private Long publicIpId;
    
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required=false, description="public ip address id from where the network traffic will be load balanced from")
    private Long zoneId;

    @Parameter(name=ApiConstants.PUBLIC_PORT, type=CommandType.INTEGER, required=true, description="the public port from where the network traffic will be load balanced from")
    private Integer publicPort;

    @Parameter(name = ApiConstants.OPEN_FIREWALL, type = CommandType.BOOLEAN, description = "if true, firewall rule for source/end pubic port is automatically created; if false - firewall rule has to be created explicitely. Has value true by default")
    private Boolean openFirewall;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the account associated with the load balancer. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the domain ID associated with the load balancer")
    private Long domainId;
    
    @Parameter(name = ApiConstants.CIDR_LIST, type = CommandType.LIST, collectionType = CommandType.STRING, description = "the cidr list to forward traffic from")
    private List<String> cidrlist;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAlgorithm() {
        return algorithm;
    }

    public String getDescription() {
        return description;
    }

    public String getLoadBalancerRuleName() {
        return loadBalancerRuleName;
    }

    public Integer getPrivatePort() {
        return privatePort;
    }

    public Long getPublicIpId() {
        IpAddress ipAddr = _networkService.getIp(publicIpId);
        if (ipAddr == null || !ipAddr.readyToUse()) {
            throw new InvalidParameterValueException("Unable to create load balancer rule, invalid IP address id " + ipAddr.getId());
        }
        
        return publicIpId;
    }

    public Integer getPublicPort() {
        return publicPort;
    }
    
    public String getName() {
        return loadBalancerRuleName;
    }
    
    public Boolean getOpenFirewall() {
        if (openFirewall != null) {
            return openFirewall;
        } else {
            return true;
        }
    }
    
    public List<String> getSourceCidrList() {
        if (cidrlist != null) {
            throw new InvalidParameterValueException("Parameter cidrList is deprecated; if you need to open firewall rule for the specific cidr, please refer to createFirewallRule command");
        }
        return null;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public void execute() throws ResourceAllocationException, ResourceUnavailableException {        
        
        UserContext callerContext = UserContext.current();
        boolean success = true;
        LoadBalancer rule = null;
        try {
            UserContext.current().setEventDetails("Rule Id: " + getEntityId());
            
            if (getOpenFirewall()) {
                success = success && _firewallService.applyFirewallRules(publicIpId, callerContext.getCaller());
            }

            // State might be different after the rule is applied, so get new object here
            rule = _entityMgr.findById(LoadBalancer.class, getEntityId());
            LoadBalancerResponse lbResponse = new LoadBalancerResponse(); 
            if (rule != null) {
                lbResponse = _responseGenerator.createLoadBalancerResponse(rule);
                setResponseObject(lbResponse);
            }
            lbResponse.setResponseName(getCommandName());
        } finally {
            if (!success || rule == null) {
                
                if (getOpenFirewall()) {
                    _firewallService.revokeRelatedFirewallRule(getEntityId(), true);
                }
                // no need to apply the rule on the backend as it exists in the db only
                _lbService.deleteLoadBalancerRule(getEntityId(), false);

                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create load balancer rule");
            }
        }
    }
    
    
    @Override
    public void create() {
        //cidr list parameter is deprecated
        if (cidrlist != null) {
            throw new InvalidParameterValueException("Parameter cidrList is deprecated; if you need to open firewall rule for the specific cidr, please refer to createFirewallRule command");
        }
        try {
            LoadBalancer result = _lbService.createLoadBalancerRule(this, getOpenFirewall());
            this.setEntityId(result.getId());
        } catch (NetworkRuleConflictException e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(BaseCmd.NETWORK_RULE_CONFLICT_ERROR, e.getMessage());
        } catch (InsufficientAddressCapacityException e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(BaseCmd.INSUFFICIENT_CAPACITY_ERROR, e.getMessage());
        }
    }

  

    public Long getSourceIpAddressId() {
        return publicIpId;
    }

    public Integer getSourcePortStart() {
        return publicPort.intValue();
    }

    public Integer getSourcePortEnd() {
        return publicPort.intValue();
    }

    public String getProtocol() {
        return NetUtils.TCP_PROTO;
    }
    
    public long getAccountId() {  
        if (publicIpId != null)
            return _networkService.getIp(getPublicIpId()).getAccountId();
        Account account = UserContext.current().getCaller();
        if ((account == null) ) {
            if ((domainId != null) && (accountName != null)) {
                Account userAccount = _responseGenerator.findAccountByNameDomain(accountName, domainId);
                if (userAccount != null) {
                    return userAccount.getId();
                }
            }
        }

        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM;
    }

    public long getDomainId() {
        if (publicIpId != null)
            return _networkService.getIp(getPublicIpId()).getDomainId();
        if (domainId != null) {
            return domainId;
        }
        return UserContext.current().getCaller().getDomainId();
    }

    public int getDefaultPortStart() {
        return privatePort.intValue();
    }

    public int getDefaultPortEnd() {
        return privatePort.intValue();
    }
    
    @Override
    public long getEntityOwnerId() {
       return getAccountId();
    }
    

    public Integer getIcmpCode() {
        return null;
    }
    
    public Integer getIcmpType() {
        return null;
    }
    
    public String getAccountName() {
        return accountName;
    }
    
    public Long getZoneId() {
        return zoneId;
    }

    public void setPublicIpId(Long publicIpId) {
        this.publicIpId = publicIpId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_LOAD_BALANCER_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating load balancer: " + getName() + " account: " + getAccountName();

    }

    public String getXid() {
        /*FIXME*/
        return null;
    }

    public void setSourceIpAddressId(Long ipId) {
        this.publicIpId = ipId;
    }

}

