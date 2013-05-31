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
package com.cloud.api.commands;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.api.response.BigSwitchVnsDeviceResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.BigSwitchVnsDeviceVO;
import com.cloud.network.element.BigSwitchVnsElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "addBigSwitchVnsDevice", responseObject=BigSwitchVnsDeviceResponse.class, description="Adds a BigSwitch VNS device", since = "4.1.0")
public class AddBigSwitchVnsDeviceCmd extends BaseAsyncCmd {
    private static final String s_name = "addbigswitchvnsdeviceresponse";
    @Inject BigSwitchVnsElementService _bigswitchVnsElementService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.UUID, entityType = PhysicalNetworkResponse.class,
            required=true, description="the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name=ApiConstants.HOST_NAME, type=CommandType.STRING, required=true,
		description="Hostname of ip address of the BigSwitch VNS Controller.")
    private String host;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public String getHost() {
        return host;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
            BigSwitchVnsDeviceVO bigswitchVnsDeviceVO = _bigswitchVnsElementService.addBigSwitchVnsDevice(this);
            if (bigswitchVnsDeviceVO != null) {
                BigSwitchVnsDeviceResponse response = _bigswitchVnsElementService.createBigSwitchVnsDeviceResponse(bigswitchVnsDeviceVO);
                response.setObjectName("bigswitchvnsdevice");
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add BigSwitch VNS device due to internal error.");
            }
        }  catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public String getEventType() {
        return VnsConstants.EVENT_EXTERNAL_VNS_CONTROLLER_ADD;
    }

    @Override
    public String getEventDescription() {
        return "Adding a BigSwitch VNS Controller";
    }
}
