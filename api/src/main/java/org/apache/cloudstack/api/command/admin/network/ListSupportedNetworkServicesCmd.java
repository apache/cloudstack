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


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ServiceResponse;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.Network.Service;
import com.cloud.user.Account;

@APICommand(name = "listSupportedNetworkServices",
            description = "Lists all network services provided by CloudStack or for the given Provider.",
            responseObject = ServiceResponse.class,
            since = "3.0.0",
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class ListSupportedNetworkServicesCmd extends BaseListCmd {

    @Parameter(name = ApiConstants.PROVIDER, type = CommandType.STRING, description = "network service provider name")
    private String providerName;

    @Parameter(name = ApiConstants.SERVICE, type = CommandType.STRING, description = "network service name to list providers and capabilities of")
    private String serviceName;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getServiceName() {
        return serviceName;
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
        List<? extends Network.Service> services;
        if (getServiceName() != null) {
            Network.Service service = null;
            if (serviceName != null) {
                service = Network.Service.getService(serviceName);
                if (service == null) {
                    throw new InvalidParameterValueException("Invalid Network Service=" + serviceName);
                }
            }
            List<Network.Service> serviceList = new ArrayList<Network.Service>();
            serviceList.add(service);
            services = serviceList;
        } else {
            services = _networkService.listNetworkServices(getProviderName());
        }

        ListResponse<ServiceResponse> response = new ListResponse<ServiceResponse>();
        List<ServiceResponse> servicesResponses = new ArrayList<ServiceResponse>();
        for (Network.Service service : services) {
            //skip gateway service
            if (service == Service.Gateway) {
                continue;
            }
            ServiceResponse serviceResponse = _responseGenerator.createNetworkServiceResponse(service);
            servicesResponses.add(serviceResponse);
        }

        response.setResponses(servicesResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
