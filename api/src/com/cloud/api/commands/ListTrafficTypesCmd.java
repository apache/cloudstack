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
import com.cloud.api.BaseListCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.ProviderResponse;
import com.cloud.api.response.TrafficTypeResponse;
import com.cloud.network.PhysicalNetworkTrafficType;
import com.cloud.user.Account;


@Implementation(description="Lists traffic types of a given physical network.", responseObject=ProviderResponse.class)
public class ListTrafficTypesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListTrafficTypesCmd.class.getName());
    private static final String _name = "listtraffictypesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @IdentityMapper(entityTableName="physical_network")
    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.LONG, required=true, description="the Physical Network ID")
    private Long physicalNetworkId;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    
    public void setPhysicalNetworkId(Long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return _name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
    
    @Override
    public void execute(){
        List<? extends PhysicalNetworkTrafficType> trafficTypes = _networkService.listTrafficTypes(getPhysicalNetworkId());
        ListResponse<TrafficTypeResponse> response = new ListResponse<TrafficTypeResponse>();
        List<TrafficTypeResponse> trafficTypesResponses = new ArrayList<TrafficTypeResponse>();
        for (PhysicalNetworkTrafficType trafficType : trafficTypes) {
            TrafficTypeResponse trafficTypeResponse = _responseGenerator.createTrafficTypeResponse(trafficType);
            trafficTypesResponses.add(trafficTypeResponse);
        }

        response.setResponses(trafficTypesResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }


}
