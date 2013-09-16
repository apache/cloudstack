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
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.ProviderResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.user.Account;

@APICommand(name = "addNetworkServiceProvider", description="Adds a network serviceProvider to a physical network", responseObject=ProviderResponse.class, since="3.0.0")
public class AddNetworkServiceProviderCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(AddNetworkServiceProviderCmd.class.getName());

    private static final String s_name = "addnetworkserviceproviderresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.UUID, entityType=PhysicalNetworkResponse.class,
            required=true, description="the Physical Network ID to add the provider to")
    private Long physicalNetworkId;

    @Parameter(name=ApiConstants.DEST_PHYSICAL_NETWORK_ID, type=CommandType.UUID, entityType=PhysicalNetworkResponse.class,
            description="the destination Physical Network ID to bridge to")
    private Long destinationPhysicalNetworkId;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name for the physical network service provider")
    private String name;

    @Parameter(name=ApiConstants.SERVICE_LIST, type=CommandType.LIST, collectionType = CommandType.STRING, description="the list of services to be enabled for this physical network service provider")
    private List<String> enabledServices;


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
        CallContext.current().setEventDetails("Network ServiceProvider Id: "+getEntityId());
        PhysicalNetworkServiceProvider result = _networkService.getCreatedPhysicalNetworkServiceProvider(getEntityId());
        if (result != null) {
            ProviderResponse response = _responseGenerator.createNetworkServiceProviderResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add service provider to physical network");
        }
    }

    @Override
    public void create() throws ResourceAllocationException {
        PhysicalNetworkServiceProvider result = _networkService.addProviderToPhysicalNetwork(getPhysicalNetworkId(), getProviderName(), getDestinationPhysicalNetworkId(), getEnabledServices());
        if (result != null) {
            setEntityId(result.getId());
            setEntityUuid(result.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add service provider entity to physical network");
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

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.PhysicalNetworkServiceProvider;
    }
}
