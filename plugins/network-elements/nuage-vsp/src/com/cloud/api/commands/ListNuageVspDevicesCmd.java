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

import com.cloud.api.response.NuageVspDeviceResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NuageVspDeviceVO;
import com.cloud.network.manager.NuageVspManager;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = "listNuageVspDevices", responseObject = NuageVspDeviceResponse.class, description = "Lists Nuage VSP devices", since = "4.5")
public class ListNuageVspDevicesCmd extends BaseListCmd {
    private static final String s_name = "listnuagevspdeviceresponse";
    @Inject
    NuageVspManager _nuageVspManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class, description = "the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name = VspConstants.NUAGE_VSP_DEVICE_ID, type = CommandType.UUID, entityType = NuageVspDeviceResponse.class, description = "the Nuage VSP device ID")
    private Long nuageVspDeviceId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getNuageVspDeviceId() {
        return nuageVspDeviceId;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
            List<NuageVspDeviceVO> nuageDevices = _nuageVspManager.listNuageVspDevices(this);
            ListResponse<NuageVspDeviceResponse> response = new ListResponse<NuageVspDeviceResponse>();
            List<NuageVspDeviceResponse> nuageDevicesResponse = new ArrayList<NuageVspDeviceResponse>();

            if (nuageDevices != null && !nuageDevices.isEmpty()) {
                for (NuageVspDeviceVO nuageDeviceVO : nuageDevices) {
                    NuageVspDeviceResponse nuageDeviceResponse = _nuageVspManager.createNuageVspDeviceResponse(nuageDeviceVO);
                    nuageDevicesResponse.add(nuageDeviceResponse);
                }
            }

            response.setResponses(nuageDevicesResponse);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
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
