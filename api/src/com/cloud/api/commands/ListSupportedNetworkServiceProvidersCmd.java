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
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.ProviderResponse;
import com.cloud.network.Network;
import com.cloud.user.Account;


@Implementation(description="Lists all network serviceproviders supported by CloudStack or for the given service.", responseObject=ProviderResponse.class)
public class ListSupportedNetworkServiceProvidersCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListSupportedNetworkServiceProvidersCmd.class.getName());
    private static final String _name = "listsupportednetworkserviceprovidersresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    
    @Parameter(name=ApiConstants.SERVICE_NAME, type=CommandType.STRING, description="network service name")
    private String serviceName;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
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
        List<? extends Network.Provider> serviceProviders = _networkService.listSupportedNetworkServiceProviders(getServiceName());
        ListResponse<ProviderResponse> response = new ListResponse<ProviderResponse>();
        List<ProviderResponse> serviceProvidersResponses = new ArrayList<ProviderResponse>();
        for (Network.Provider serviceProvider : serviceProviders) {
            ProviderResponse serviceProviderResponse = _responseGenerator.createNetworkServiceProviderResponse(serviceProvider);
            serviceProvidersResponses.add(serviceProviderResponse);
        }

        response.setResponses(serviceProvidersResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
