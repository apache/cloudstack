// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.command.admin.network;

import java.util.List;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ProviderResponse;

import com.cloud.event.EventTypes;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.user.Account;

@APICommand(name = "updateNetworkServiceProvider",
            description = "Updates a network serviceProvider of a physical network",
            responseObject = ProviderResponse.class,
            since = "3.0.0",
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class UpdateNetworkServiceProviderCmd extends BaseAsyncCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "Enabled/Disabled/Shutdown the physical network service provider")
    private String state;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ProviderResponse.class, required = true, description = "network service provider id")
    private Long id;

    @Parameter(name = ApiConstants.SERVICE_LIST,
               type = CommandType.LIST,
               collectionType = CommandType.STRING,
               description = "the list of services to be enabled for this physical network service provider")
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
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        PhysicalNetworkServiceProvider result = _networkService.updateNetworkServiceProvider(getId(), getState(), getEnabledServices());
        if (result != null) {
            ProviderResponse response = _responseGenerator.createNetworkServiceProviderResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update service provider");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SERVICE_PROVIDER_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Updating physical network ServiceProvider: " + getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.PhysicalNetworkServiceProvider;
    }

}
