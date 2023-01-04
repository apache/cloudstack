//
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
//

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

import com.cloud.api.response.BigSwitchBcfDeviceResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.BigSwitchBcfDeviceVO;
import com.cloud.network.element.BigSwitchBcfElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "addBigSwitchBcfDevice", responseObject = BigSwitchBcfDeviceResponse.class, description = "Adds a BigSwitch BCF Controller device", since = "4.6.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class AddBigSwitchBcfDeviceCmd extends BaseAsyncCmd {
    @Inject
    private BigSwitchBcfElementService bcfElementService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID,
               type = CommandType.UUID,
               entityType = PhysicalNetworkResponse.class,
               required = true,
               description = "the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.HOST_NAME, type = CommandType.STRING, required = true, description = "Hostname of ip address of the BigSwitch BCF Controller.")
    private String host;

    @Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, required=true,
            description="Username of the BigSwitch BCF Controller.")
    private String username;

    @Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, required=true,
            description="Password of the BigSwitch BCF Controller.")
    private String password;

    @Parameter(name=BcfConstants.BIGSWITCH_BCF_DEVICE_NAT, type=CommandType.BOOLEAN, required=true,
            description="NAT support of the BigSwitch BCF Controller.")
    private Boolean nat;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public String getHost() {
        return host;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Boolean getNat() {
        return nat;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException {
        try {
            final BigSwitchBcfDeviceVO bigswitchBcfDeviceVO = bcfElementService.addBigSwitchBcfDevice(this);
            if (bigswitchBcfDeviceVO == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add BigSwitch BCF Controller device due to internal error.");
            }
            final BigSwitchBcfDeviceResponse response = bcfElementService.createBigSwitchBcfDeviceResponse(bigswitchBcfDeviceVO);
            response.setObjectName("bigswitchbcfdevice");
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage(), invalidParamExcp);
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage(), runtimeExcp);
        }
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public String getEventType() {
        return BcfConstants.EVENT_BCF_CONTROLLER_ADD;
    }

    @Override
    public String getEventDescription() {
        return "Adding a BigSwitch BCF Controller";
    }
}
