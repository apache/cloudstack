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

import com.cloud.api.response.CiscoAsa1000vResourceResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.cisco.CiscoAsa1000vDevice;
import com.cloud.network.cisco.CiscoAsa1000vDeviceVO;
import com.cloud.network.element.CiscoAsa1000vService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "listCiscoAsa1000vResources", responseObject = CiscoAsa1000vResourceResponse.class, description = "Lists Cisco ASA 1000v appliances",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListCiscoAsa1000vResourcesCmd extends BaseListCmd {
    private static final String s_name = "listCiscoAsa1000vResources";
    @Inject
    CiscoAsa1000vService _ciscoAsa1000vService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class, description = "the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.RESOURCE_ID, type = CommandType.UUID, entityType = CiscoAsa1000vResourceResponse.class, description = "Cisco ASA 1000v resource ID")
    private Long ciscoAsa1000vResourceId;

    @Parameter(name = ApiConstants.HOST_NAME, type = CommandType.STRING, description = "Hostname or ip address of the Cisco ASA 1000v appliance.")
    private String host;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getCiscoAsa1000vResourceId() {
        return ciscoAsa1000vResourceId;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public String getManagementIp() {
        return host;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException {
        try {
            List<CiscoAsa1000vDeviceVO> ciscoAsa1000vDevices = _ciscoAsa1000vService.listCiscoAsa1000vResources(this);
            ListResponse<CiscoAsa1000vResourceResponse> response = new ListResponse<CiscoAsa1000vResourceResponse>();
            List<CiscoAsa1000vResourceResponse> ciscoAsa1000vResourcesResponse = new ArrayList<CiscoAsa1000vResourceResponse>();

            if (ciscoAsa1000vDevices != null && !ciscoAsa1000vDevices.isEmpty()) {
                for (CiscoAsa1000vDevice ciscoAsa1000vDeviceVO : ciscoAsa1000vDevices) {
                    CiscoAsa1000vResourceResponse ciscoAsa1000vResourceResponse = _ciscoAsa1000vService.createCiscoAsa1000vResourceResponse(ciscoAsa1000vDeviceVO);
                    ciscoAsa1000vResourceResponse.setObjectName("CiscoAsa1000vResource");
                    ciscoAsa1000vResourcesResponse.add(ciscoAsa1000vResourceResponse);
                }
            }

            response.setResponses(ciscoAsa1000vResourcesResponse);
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
