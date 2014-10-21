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

import com.cloud.api.response.BrocadeVcsDeviceResponse;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.BrocadeVcsDeviceVO;
import com.cloud.network.brocade.Constants;
import com.cloud.network.element.BrocadeVcsElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "listBrocadeVcsDevices", responseObject = BrocadeVcsDeviceResponse.class, description = "Lists Brocade VCS Switches", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListBrocadeVcsDevicesCmd extends BaseListCmd {
    private static final String s_name = "listbrocadevcsdeviceresponse";

    @Inject
    protected BrocadeVcsElementService brocadeVcsElementService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class, description = "the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name = Constants.BROCADE_VCS_DEVICE_ID, type = CommandType.UUID, entityType = BrocadeVcsDeviceResponse.class, description = "Brocade VCS switch ID")
    private Long brocadeVcsDeviceId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getBrocadeVcsDeviceId() {
        return brocadeVcsDeviceId;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        try {
            List<BrocadeVcsDeviceVO> brocadeDevices = brocadeVcsElementService.listBrocadeVcsDevices(this);
            ListResponse<BrocadeVcsDeviceResponse> response = new ListResponse<BrocadeVcsDeviceResponse>();
            List<BrocadeVcsDeviceResponse> brocadeDevicesResponse = new ArrayList<BrocadeVcsDeviceResponse>();

            if (brocadeDevices != null && !brocadeDevices.isEmpty()) {
                for (BrocadeVcsDeviceVO brocadeDeviceVO : brocadeDevices) {
                    BrocadeVcsDeviceResponse brocadeDeviceResponse = brocadeVcsElementService.createBrocadeVcsDeviceResponse(brocadeDeviceVO);
                    brocadeDevicesResponse.add(brocadeDeviceResponse);
                }
            }

            response.setResponses(brocadeDevicesResponse);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

}
