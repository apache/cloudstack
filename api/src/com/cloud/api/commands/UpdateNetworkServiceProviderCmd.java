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
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ProviderResponse;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.user.Account;

@Implementation(description="Updates a network serviceProvider of a physical network", responseObject=ProviderResponse.class)
public class UpdateNetworkServiceProviderCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateNetworkServiceProviderCmd.class.getName());

    private static final String s_name = "updatenetworkserviceproviderresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.ENABLED, type=CommandType.BOOLEAN, description="true/false enable/disable the physical network service provider")
    private Boolean enabled;
    
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="network service provider id")
    private Long id;    

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Boolean isEnabled() {
        return enabled;
    }
    
    private Long getId() {
        return id;
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
        PhysicalNetworkServiceProvider result = _networkService.updateNetworkServiceProvider(getId(), isEnabled());
        if (result != null) {
            ProviderResponse response = _responseGenerator.createNetworkServiceProviderResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add service provider to physical network");
        }
    }





}
