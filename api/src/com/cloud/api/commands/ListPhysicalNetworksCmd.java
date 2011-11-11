/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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
import com.cloud.api.BaseCmd;
import com.cloud.api.BaseListCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.PhysicalNetworkResponse;
import com.cloud.network.PhysicalNetwork;
import com.cloud.user.Account;

@Implementation(description="Lists physical networks", responseObject=PhysicalNetworkResponse.class)
public class ListPhysicalNetworksCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListPhysicalNetworksCmd.class.getName());

    private static final String s_name = "listphysicalnetworksresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    
    @IdentityMapper(entityTableName="physical_network")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="list physical network by id")
    private Long id;

    @IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="the Zone ID for the physical network")
    private Long zoneId;

    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    
    public Long getId() {
        return id;
    }

    public Long getZoneId() {
        return zoneId;
    }
    
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
    
    @Override
    public void execute(){
        List<? extends PhysicalNetwork> result = _networkService.searchPhysicalNetworks(getId(),getZoneId(), this.getKeyword(), this.getStartIndex(), this.getPageSizeVal());
        if (result != null) {
            ListResponse<PhysicalNetworkResponse> response = new ListResponse<PhysicalNetworkResponse>();
            List<PhysicalNetworkResponse> networkResponses = new ArrayList<PhysicalNetworkResponse>();
            for (PhysicalNetwork network : result) {
                PhysicalNetworkResponse networkResponse = _responseGenerator.createPhysicalNetworkResponse(network);
                networkResponses.add(networkResponse);
            }
            response.setResponses(networkResponses);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to search for physical networks");
        }
    }
}
