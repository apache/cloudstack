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

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ProviderResponse;
import com.cloud.event.EventTypes;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.user.Account;

@Implementation(description="Updates a network serviceProvider of a physical network", responseObject=ProviderResponse.class)
public class UpdateNetworkServiceProviderCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateNetworkServiceProviderCmd.class.getName());

    private static final String s_name = "updatenetworkserviceproviderresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.STATE, type=CommandType.STRING, description="Enabled/Disabled/Shutdown the physical network service provider")
    private String state;
    
    @IdentityMapper(entityTableName="physical_network_service_providers")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="network service provider id")
    private Long id;    

    @Parameter(name=ApiConstants.SERVICE_LIST, type=CommandType.LIST, collectionType = CommandType.STRING, description="the list of services to be enabled for this physical network service provider")
    private List<String> enabledServices;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getState() {
        return state;
    }
    
    private Long getId() {
        return id;
    }    
    
    public List<String> getEnabledServices() {
        return enabledServices;
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
        PhysicalNetworkServiceProvider result = _networkService.updateNetworkServiceProvider(getId(), getState(), getEnabledServices());
        if (result != null) {
            ProviderResponse response = _responseGenerator.createNetworkServiceProviderResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update service provider");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SERVICE_PROVIDER_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return  "Updating physical network ServiceProvider: " + getId();
    }

}
