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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;

import com.cloud.api.response.BigSwitchBcfDeviceResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.BigSwitchBcfDeviceVO;
import com.cloud.network.element.BigSwitchBcfElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "listBigSwitchBcfDevices", responseObject = BigSwitchBcfDeviceResponse.class, description = "Lists BigSwitch BCF Controller devices", since = "4.6.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListBigSwitchBcfDevicesCmd extends BaseListCmd {
    private static final String S_NAME = "listbigswitchbcfdeviceresponse";
    @Inject
    private BigSwitchBcfElementService bigswitchBcfElementService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class, description = "the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name = BcfConstants.BIGSWITCH_BCF_DEVICE_ID,
               type = CommandType.UUID,
               entityType = BigSwitchBcfDeviceResponse.class,
               description = "bigswitch BCF controller device ID")
    private Long bigswitchBcfDeviceId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getBigSwitchBcfDeviceId() {
        return bigswitchBcfDeviceId;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException {
        try {
            final List<BigSwitchBcfDeviceVO> bigswitchDevices = bigswitchBcfElementService.listBigSwitchBcfDevices(this);
            final ListResponse<BigSwitchBcfDeviceResponse> response = new ListResponse<BigSwitchBcfDeviceResponse>();
            final List<BigSwitchBcfDeviceResponse> bigswitchDevicesResponse = new ArrayList<BigSwitchBcfDeviceResponse>();

            if (bigswitchDevices != null && !bigswitchDevices.isEmpty()) {
                for (final BigSwitchBcfDeviceVO bigswitchDeviceVO : bigswitchDevices) {
                    final BigSwitchBcfDeviceResponse bigswitchDeviceResponse = bigswitchBcfElementService.createBigSwitchBcfDeviceResponse(bigswitchDeviceVO);
                    bigswitchDevicesResponse.add(bigswitchDeviceResponse);
                }
            }

            response.setResponses(bigswitchDevicesResponse);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage(), invalidParamExcp);
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage(), runtimeExcp);
        }
    }

    @Override
    public String getCommandName() {
        return S_NAME;
    }
}
