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
import com.cloud.api.BaseListProjectAndAccountResourcesCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.NetworkResponse;
import com.cloud.network.Network;


@Implementation(description="Lists all available networks.", responseObject=NetworkResponse.class)
public class ListNetworksCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListNetworksCmd.class.getName());
    private static final String _name = "listnetworksresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @IdentityMapper(entityTableName="networks")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="list networks by id")
    private Long id;
    
    @IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="the Zone ID of the network")
    private Long zoneId;
    
    @Parameter(name=ApiConstants.TYPE, type=CommandType.STRING, description="the type of the network. Supported values are: Isolated and Shared")
    private String guestIpType;
    
    @Parameter(name=ApiConstants.IS_SYSTEM, type=CommandType.BOOLEAN, description="true if network is system, false otherwise")
    private Boolean isSystem;
    
    @Parameter(name=ApiConstants.ACL_TYPE, type=CommandType.STRING, description="list networks by ACL (access control list) type. Supported values are Account and Domain")
    private String aclType;
    
    @Parameter(name=ApiConstants.TRAFFIC_TYPE, type=CommandType.STRING, description="type of the traffic")
    private String trafficType;
    
    @IdentityMapper(entityTableName="physical_network")
    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.LONG, description="list networks by physical network id")
    private Long physicalNetworkId;
    
    @Parameter(name=ApiConstants.SUPPORTED_SERVICES, type=CommandType.LIST, collectionType=CommandType.STRING, description="list network offerings supporting certain services")
    private List<String> supportedServices;
    
    @Parameter(name=ApiConstants.RESTART_REQUIRED, type=CommandType.BOOLEAN, description="list network offerings by restartRequired option")
    private Boolean restartRequired;
    
    @Parameter(name=ApiConstants.SPECIFY_IP_RANGES, type=CommandType.BOOLEAN, description="true if need to list only networks which support specifying ip ranges")
    private Boolean specifyIpRanges;
   
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
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
    
    public String getAclType() {
		return aclType;
	}

    public String getTrafficType() {
        return trafficType;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public List<String> getSupportedServices() {
        return supportedServices;
    }

    public Boolean getRestartRequired() {
		return restartRequired;
	}
    
    public Boolean getSpecifyIpRanges() {
    	return specifyIpRanges;
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
