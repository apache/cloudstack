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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.ProviderResponse;

import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

@APICommand(name = "listNetworkServiceProviders",
            description = "Lists network serviceproviders for a given physical network.",
            responseObject = ProviderResponse.class,
            since = "3.0.0",
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class ListNetworkServiceProvidersCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListNetworkServiceProvidersCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class, description = "the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "list providers by name")
    private String name;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "list providers by state")
    private String state;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public void setPhysicalNetworkId(Long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
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
        Pair<List<? extends PhysicalNetworkServiceProvider>, Integer> serviceProviders =
            _networkService.listNetworkServiceProviders(getPhysicalNetworkId(), getName(), getState(), this.getStartIndex(), this.getPageSizeVal());
        ListResponse<ProviderResponse> response = new ListResponse<ProviderResponse>();
        List<ProviderResponse> serviceProvidersResponses = new ArrayList<ProviderResponse>();
        for (PhysicalNetworkServiceProvider serviceProvider : serviceProviders.first()) {
            ProviderResponse serviceProviderResponse = _responseGenerator.createNetworkServiceProviderResponse(serviceProvider);
            serviceProvidersResponses.add(serviceProviderResponse);
        }

        response.setResponses(serviceProvidersResponses, serviceProviders.second());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

}
