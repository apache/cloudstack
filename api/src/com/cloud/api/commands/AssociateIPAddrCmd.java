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
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.IPAddressResponse;
import com.cloud.async.AsyncJob;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.user.UserContext;

@Implementation(description="Acquires and associates a public IP to an account.", responseObject=IPAddressResponse.class)
public class AssociateIPAddrCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(AssociateIPAddrCmd.class.getName());
    private static final String s_name = "associateipaddressresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the account to associate with this IP address")
    private String accountName;

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the ID of the domain to associate with this IP address")
    private Long domainId;

    @IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="the ID of the availability zone you want to acquire an public IP address from")
    private Long zoneId;
    
    @IdentityMapper(entityTableName="networks")
    @Parameter(name=ApiConstants.NETWORK_ID, type=CommandType.LONG, description="The network this ip address should be associated to.")
    private Long networkId;
    
    @IdentityMapper(entityTableName="projects")
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="Deploy vm for the project")
    private Long projectId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getEntityTable() {
    	return "user_ip_address";
    }
    
    public String getAccountName() {
        if (accountName != null) { 
            return accountName;
        }
        return UserContext.current().getCaller().getAccountName();
    }

    public long getDomainId() {
        if (domainId != null) {
            return domainId;
        }
        return UserContext.current().getCaller().getDomainId();
    }

    private Long getZoneId() {
        return zoneId;
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
            assert (networks.size() <= 1) : "Too many virtual networks.  This logic should be obsolete";
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
    
    @Override
    public long getEntityOwnerId() {
        Long accountId = getAccountId(accountName, domainId, projectId);
        if (accountId == null) {
            return UserContext.current().getCaller().getId();
        }
        
        return accountId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NET_IP_ASSIGN;
    }
    
    @Override
    public String getEventDescription() {
        return  "associating ip to network id: " + getNetworkId() + " in zone " + getZoneId();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////


    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
    	return "addressinfo";
    }
    
    @Override
    public void create() throws ResourceAllocationException{
        try {
            IpAddress ip = _networkService.allocateIP(getNetworkId(), _accountService.getAccount(getEntityOwnerId()));
            if (ip != null) {
                this.setEntityId(ip.getId());
            } else {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to allocate ip address");
            }
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
    public void execute() throws ResourceUnavailableException, ResourceAllocationException, ConcurrentOperationException, InsufficientCapacityException {
        UserContext.current().setEventDetails("Ip Id: " + getEntityId());
        IpAddress result = _networkService.associateIP(this);
        if (result != null) {
            IPAddressResponse ipResponse = _responseGenerator.createIPAddressResponse(result);
            ipResponse.setResponseName(getCommandName());
            this.setResponseObject(ipResponse);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to assign ip address");
        }
    }
    
    
    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return getNetworkId();
    }
    
    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.IpAddress;
    }

}
