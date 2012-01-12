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
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.LoadBalancerResponse;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
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

    @IdentityMapper(entityTableName="user_ip_address")
    @Parameter(name=ApiConstants.PUBLIC_IP_ID, type=CommandType.LONG, description="public ip address id from where the network traffic will be load balanced from")
    private Long publicIpId;
    
    @IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required=false, description="zone where the load balancer is going to be created. This parameter is required when LB service provider is ElasticLoadBalancerVm")
    private Long zoneId;

    @Parameter(name=ApiConstants.PUBLIC_PORT, type=CommandType.INTEGER, required=true, description="the public port from where the network traffic will be load balanced from")
    private Integer publicPort;

    @Parameter(name = ApiConstants.OPEN_FIREWALL, type = CommandType.BOOLEAN, description = "if true, firewall rule for source/end pubic port is automatically created; if false - firewall rule has to be created explicitely. Has value true by default")
    private Boolean openFirewall;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the account associated with the load balancer. Must be used with the domainId parameter.")
    private String accountName;

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the domain ID associated with the load balancer")
    private Long domainId;
    
    @Parameter(name = ApiConstants.CIDR_LIST, type = CommandType.LIST, collectionType = CommandType.STRING, description = "the cidr list to forward traffic from")
    private List<String> cidrlist;
    
    @IdentityMapper(entityTableName="networks")
    @Parameter(name=ApiConstants.NETWORK_ID, type=CommandType.LONG, description="The guest network this rule will be created for")
    private Long networkId;
    
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

    public String getEntityTable() {
    	return "firewall_rules";
    }
    
    public Long getSourceIpAddressId() {
    	if (publicIpId != null) {
    		IpAddress ipAddr = _networkService.getIp(publicIpId);
	        if (ipAddr == null || !ipAddr.readyToUse()) {
	            throw new InvalidParameterValueException("Unable to create load balancer rule, invalid IP address id " + ipAddr.getId());
	        }
    	} else if (getEntityId() != null) {
    		LoadBalancer rule = _entityMgr.findById(LoadBalancer.class, getEntityId());
    		return rule.getSourceIpAddressId();
    	}
    	
    	return publicIpId;
    }
    
    public Long getNetworkId() {
        if (networkId != null) {
            return networkId;
        } 
        Long zoneId = getZoneId();
        
        if (zoneId == null) {
        	throw new InvalidParameterValueException("Either networkId or zoneId has to be specified");
        }
        
        DataCenter zone = _configService.getZone(zoneId);
        if (zone.getNetworkType() == NetworkType.Advanced) {
            List<? extends Network> networks = _networkService.getIsolatedNetworksOwnedByAccountInZone(getZoneId(), _accountService.getAccount(getEntityOwnerId()));
            if (networks.size() == 0) {
                String domain = _domainService.getDomain(getDomainId()).getName();
                throw new InvalidParameterValueException("Account name=" + getAccountName() + " domain=" + domain + " doesn't have virtual networks in zone=" + zone.getName());
            }
            
            if (networks.size() < 1) {
            	throw new InvalidParameterValueException("Account doesn't have any Isolated networks in the zone");
            } else if (networks.size() > 1) {
            	throw new InvalidParameterValueException("Account has more than one Isolated network in the zone");
            }
            
            return networks.get(0).getId();
        } else {
            Network defaultGuestNetwork = _networkService.getExclusiveGuestNetwork(zoneId);
            if (defaultGuestNetwork == null) {
                throw new InvalidParameterValueException("Unable to find a default Guest network for account " + getAccountName() + " in domain id=" + getDomainId());
            } else {
                return defaultGuestNetwork.getId();
            }
        }
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
                success = success && _firewallService.applyFirewallRules(getSourceIpAddressId(), callerContext.getCaller());
            }

            // State might be different after the rule is applied, so get new object here
            rule = _entityMgr.findById(LoadBalancer.class, getEntityId());
            LoadBalancerResponse lbResponse = new LoadBalancerResponse(); 
            if (rule != null) {
                lbResponse = _responseGenerator.createLoadBalancerResponse(rule);
                setResponseObject(lbResponse);
            }
            lbResponse.setResponseName(getCommandName());
        } catch (Exception ex) {
        	s_logger.warn("Failed to create LB rule due to exception ", ex);
        }finally {
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
            return _networkService.getIp(getSourceIpAddressId()).getAccountId();
        
        Account account = null;
        if ((domainId != null) && (accountName != null)) {
            account = _responseGenerator.findAccountByNameDomain(accountName, domainId);
            if (account != null) {
                return account.getId();
            } else {
                throw new InvalidParameterValueException("Unable to find account " + account + " in domain id=" + domainId);
            }
        } else {
            throw new InvalidParameterValueException("Can't define IP owner. Either specify account/domainId or ipAddressId");
        }
    }

    public long getDomainId() {
        if (publicIpId != null)
            return _networkService.getIp(getSourceIpAddressId()).getDomainId();
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

