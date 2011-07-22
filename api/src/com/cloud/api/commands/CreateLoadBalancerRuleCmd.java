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
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.LoadBalancerResponse;
import com.cloud.exception.ConcurrentOperationException;
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
public class CreateLoadBalancerRuleCmd extends BaseCmd  implements LoadBalancer {
    public static final Logger s_logger = Logger.getLogger(CreateLoadBalancerRuleCmd.class.getName());

    private static final String s_name = "createloadbalancerruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ALGORITHM, type=CommandType.STRING, required=true, description="load balancer algorithm (source, roundrobin, leastconn)")
    private String algorithm;

    @Parameter(name=ApiConstants.DESCRIPTION, type=CommandType.STRING, description="the description of the load balancer rule")
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

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the account associated with the load balancer. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the domain ID associated with the load balancer")
    private Long domainId;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    @Override
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

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    protected LoadBalancer findExistingLB() {
       List<? extends LoadBalancer> lbs = _lbService.searchForLoadBalancers(new ListLoadBalancerRulesCmd(getAccountName(), getDomainId(), null, getName(), publicIpId, null, getZoneId()) );
       if (lbs != null && lbs.size() > 0) {
           return lbs.get(0);
       }
       return null;
    }
    
    protected void allocateIp() throws ResourceAllocationException, ResourceUnavailableException {
        AssociateIPAddrCmd allocIpCmd = new AssociateIPAddrCmd(getAccountName(), getDomainId(), getZoneId(), null);
        try {
            IpAddress ip = _networkService.allocateIP(allocIpCmd);
            if (ip != null) {
                this.setPublicIpId(ip.getId());
                allocIpCmd.setEntityId(ip.getId());
            } else {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to allocate ip address");
            }
            //UserContext.current().setEventDetails("Ip Id: "+ ip.getId());
            //IpAddress result = _networkService.associateIP(allocIpCmd);
        } catch (ConcurrentOperationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex.getMessage());
        } catch (InsufficientAddressCapacityException ex) {
            s_logger.info(ex);
            s_logger.trace(ex);
            throw new ServerApiException(BaseCmd.INSUFFICIENT_CAPACITY_ERROR, ex.getMessage());
        }
    }
    
    @Override
    public void execute() throws ResourceAllocationException, ResourceUnavailableException {
        LoadBalancer result = null;
        try {
            if (publicIpId == null) {
                if (getZoneId() == null ) {
                    throw new InvalidParameterValueException("Either zone id or public ip id needs to be specified");
                }
                LoadBalancer existing = findExistingLB();
                if (existing == null) {
                    allocateIp();
                } else {
                    this.setPublicIpId(existing.getSourceIpAddressId());
                }
            }
            result = _lbService.createLoadBalancerRule(this);
        } catch (NetworkRuleConflictException e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(BaseCmd.NETWORK_RULE_CONFLICT_ERROR, e.getMessage());
        }
        LoadBalancerResponse response = _responseGenerator.createLoadBalancerResponse(result);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public long getId() {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public String getXid() {
        // FIXME: Should fix this.
        return null;
    }

    @Override
    public long getSourceIpAddressId() {
        return publicIpId;
    }

    @Override
    public int getSourcePortStart() {
        return publicPort.intValue();
    }

    @Override
    public int getSourcePortEnd() {
        return publicPort.intValue();
    }

    @Override
    public String getProtocol() {
        return NetUtils.TCP_PROTO;
    }

    @Override
    public Purpose getPurpose() {
        return Purpose.LoadBalancing;
    }

    @Override
    public State getState() {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public long getNetworkId() {
        return -1;
    }

    @Override
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

    @Override
    public long getDomainId() {
        if (publicIpId != null)
            return _networkService.getIp(getPublicIpId()).getDomainId();
        if (domainId != null) {
            return domainId;
        }
        return UserContext.current().getCaller().getDomainId();
    }

    @Override
    public int getDefaultPortStart() {
        return privatePort.intValue();
    }

    @Override
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

}
