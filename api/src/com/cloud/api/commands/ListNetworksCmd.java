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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.NetworkResponse;
import com.cloud.network.Network;


@Implementation(description="Lists all available networks.", responseObject=NetworkResponse.class)
public class ListNetworksCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListNetworksCmd.class.getName());
    private static final String _name = "listnetworksresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="list networks by id")
    private Long id;
    
    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="account who will own the VLAN. If VLAN is Zone wide, this parameter should be ommited")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="domain ID of the account owning a VLAN")
    private Long domainId;
    
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="the Zone ID of the network")
    private Long zoneId;
    
    @Parameter(name=ApiConstants.TYPE, type=CommandType.STRING, description="the type of the network")
    private String guestIpType;
    
    @Parameter(name=ApiConstants.IS_SYSTEM, type=CommandType.BOOLEAN, description="true if network is system, false otherwise")
    private Boolean isSystem;
    
    @Parameter(name=ApiConstants.IS_SHARED, type=CommandType.BOOLEAN, description="true if network is shared across accounts in the Zone, false otherwise")
    private Boolean isShared;
    
    @Parameter(name=ApiConstants.IS_DEFAULT, type=CommandType.BOOLEAN, description="true if network is default, false otherwise")
    private Boolean isDefault;
    
    @Parameter(name=ApiConstants.TRAFFIC_TYPE, type=CommandType.STRING, description="type of the traffic")
    private String trafficType;
    
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="list networks by project id")
    private Long projectId;
    
    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.LONG, description="list networks by physical network id")
    private Long physicalNetworkId;
    
    @Parameter(name=ApiConstants.SOURCE_NAT_ENABLED, type=CommandType.BOOLEAN, description="list networks that support/don't support sourceNat service")
    private Boolean sourceNatEnabled;
   
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }
    
    public String getAccountName() {
        return accountName;
    }
    
    public Long getDomainId() {
        return domainId;
    }
    
    public Long getZoneId() {
        return zoneId;
    }

    public String getGuestIpType() {
        return guestIpType;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public Boolean getIsShared() {
        return isShared;
    }
    
    public Boolean isDefault() {
        return isDefault;
    }

    public String getTrafficType() {
        return trafficType;
    }
    
    public Long getProjectId() {
        return projectId;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public Boolean getSourceNatEnabled() {
        return sourceNatEnabled;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return _name;
    }

    @Override
    public void execute(){
        List<? extends Network> networks = _networkService.searchForNetworks(this);
        ListResponse<NetworkResponse> response = new ListResponse<NetworkResponse>();
        List<NetworkResponse> networkResponses = new ArrayList<NetworkResponse>();
        for (Network network : networks) {
            NetworkResponse networkResponse = _responseGenerator.createNetworkResponse(network);
            networkResponses.add(networkResponse);
        }

        response.setResponses(networkResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
