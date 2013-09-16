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
package org.apache.cloudstack.api.command.admin.router;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ProviderResponse;
import org.apache.cloudstack.api.response.VirtualRouterProviderResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VirtualRouterProvider.VirtualRouterProviderType;
import com.cloud.network.element.VirtualRouterElementService;
import com.cloud.user.Account;

@APICommand(name = "createVirtualRouterElement", responseObject=VirtualRouterProviderResponse.class, description="Create a virtual router element.")
public class CreateVirtualRouterElementCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateVirtualRouterElementCmd.class.getName());
    private static final String s_name = "createvirtualrouterelementresponse";

    @Inject
    private List<VirtualRouterElementService> _service;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.NETWORK_SERVICE_PROVIDER_ID, type=CommandType.UUID, entityType = ProviderResponse.class, required=true, description="the network service provider ID of the virtual router element")
    private Long nspId;
    
    @Parameter(name=ApiConstants.PROVIDER_TYPE, type=CommandType.UUID, entityType = ProviderResponse.class, description="The provider type. Supported types are VirtualRouter (default) and VPCVirtualRouter")
    private String providerType;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public void setNspId(Long nspId) {
        this.nspId = nspId;
    }

    public Long getNspId() {
        return nspId;
    }
    
    public VirtualRouterProviderType getProviderType() {
        if (providerType != null) {
            if (providerType.equalsIgnoreCase(VirtualRouterProviderType.VirtualRouter.toString())) {
                return VirtualRouterProviderType.VirtualRouter;
            } else if (providerType.equalsIgnoreCase(VirtualRouterProviderType.VPCVirtualRouter.toString())) {
                return VirtualRouterProviderType.VPCVirtualRouter;
            } else throw new InvalidParameterValueException("Invalid providerType specified");
        } 
        return VirtualRouterProviderType.VirtualRouter;
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
        CallContext.current().setEventDetails("Virtual router element Id: "+getEntityId());
        VirtualRouterProvider result = _service.get(0).getCreatedElement(getEntityId());
        if (result != null) {
            VirtualRouterProviderResponse response = _responseGenerator.createVirtualRouterProviderResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add Virtual Router entity to physical network");
        }
    }

    @Override
    public void create() throws ResourceAllocationException {
        VirtualRouterProvider result = _service.get(0).addElement(getNspId(), getProviderType());
        if (result != null) {
            setEntityId(result.getId());
            setEntityUuid(result.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add Virtual Router entity to physical network");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SERVICE_PROVIDER_CREATE;
    }

    @Override
    public String getEventDescription() {
        return  "Adding physical network ServiceProvider Virtual Router: " + getEntityId();
    }
}
