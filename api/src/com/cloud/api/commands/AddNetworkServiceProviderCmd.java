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

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ProviderResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(description="Adds a network serviceProvider to a physical network", responseObject=ProviderResponse.class)
public class AddNetworkServiceProviderCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(AddNetworkServiceProviderCmd.class.getName());

    private static final String s_name = "addnetworkserviceproviderresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    
    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.LONG, required=true, description="the Physical Network ID to add the provider to")
    private Long physicalNetworkId;

    @Parameter(name=ApiConstants.DEST_PHYSICAL_NETWORK_ID, type=CommandType.LONG, description="the destination Physical Network ID to bridge to")
    private Long destinationPhysicalNetworkId;
    
    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name for the physical network service provider")
    private String name;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    
    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public String getProviderName() {
        return name;
    }

    public Long getDestinationPhysicalNetworkId() {
        return destinationPhysicalNetworkId;
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
        UserContext.current().setEventDetails("Network ServiceProvider Id: "+getEntityId());
        PhysicalNetworkServiceProvider result = _networkService.getCreatedPhysicalNetworkServiceProvider(getEntityId());
        if (result != null) {
            ProviderResponse response = _responseGenerator.createNetworkServiceProviderResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add service provider to physical network");
        }
    }

    @Override
    public void create() throws ResourceAllocationException {
        PhysicalNetworkServiceProvider result = _networkService.addProviderToPhysicalNetwork(getPhysicalNetworkId(), getProviderName(), getDestinationPhysicalNetworkId());
        if (result != null) {
            setEntityId(result.getId());
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add service provider entity to physical network");
        }        
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SERVICE_PROVIDER_CREATE;
    }
    
    @Override
    public String getEventDescription() {
        return  "Adding physical network ServiceProvider: " + getEntityId();
    }
}
