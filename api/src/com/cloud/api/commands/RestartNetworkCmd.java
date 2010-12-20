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
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.IPAddressResponse;
import com.cloud.api.response.SuccessResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.user.UserContext;

@Implementation(description="Reapplies all ip addresses for the particular network", responseObject=IPAddressResponse.class)
public class RestartNetworkCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(RestartNetworkCmd.class.getName());
    private static final String s_name = "restartnetworkresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the account to associate with this IP address")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the ID of the domain to associate with this IP address")
    private Long domainId;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required=true, description="the ID of the availability zone you want to acquire an public IP address from")
    private Long zoneId;
    
    @Parameter(name=ApiConstants.NETWORK_ID, type=CommandType.LONG, description="The network this ip address should be associated to.")
    private Long networkId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        if (accountName != null) { 
            return accountName;
        }
        return UserContext.current().getAccount().getAccountName();
    }

    public long getDomainId() {
        if (domainId != null) {
            return domainId;
        }
        return UserContext.current().getAccount().getDomainId();
    }

    public long getZoneId() {
        return zoneId;
    }
    
    public String getEventDescription() {
        return  "Restarting network: " + getNetworkId();
    }
    
    public String getEventType() {
        return EventTypes.NETWORK_RESTART;
    }
    
    public long getEntityOwnerId() {
        List<? extends Network> networks = _networkService.getVirtualNetworksOwnedByAccountInZone(getAccountName(), getDomainId(), getZoneId());
        if (networks.size() == 0) {
            assert (networks.size() <= 1) : "No virtual network is found";
        }
        assert (networks.size() <= 1) : "Too many virtual networks.  This logic should be obsolete";
        
        return networks.get(0).getAccountId();
    }
    
    public Long getNetworkId() {
        if (networkId != null) {
            return networkId;
        }
        
        List<? extends Network> networks = _networkService.getVirtualNetworksOwnedByAccountInZone(getAccountName(), getDomainId(), getZoneId());
        if (networks.size() == 0) {
            return null;
        }
        assert (networks.size() <= 1) : "Too many virtual networks.  This logic should be obsolete";
        return networks.get(0).getId();
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
    public void execute() throws ResourceUnavailableException, ResourceAllocationException, ConcurrentOperationException, InsufficientCapacityException {
        boolean result = _networkService.restartNetwork(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to restart network");
        }
    }
}
