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

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;

import com.cloud.api.response.VyosRouterFirewallResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.dao.ExternalFirewallDeviceVO;
import com.cloud.network.element.VyosRouterFirewallElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "listVyosRouterFirewalls", responseObject = VyosRouterFirewallResponse.class, description = "lists Vyos Router devices in a physical network",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListVyosRouterFirewallsCmd extends BaseListCmd {

    public static final Logger s_logger = Logger.getLogger(ListVyosRouterFirewallsCmd.class.getName());
    private static final String s_name = "listvyosrouterfirewallresponse";
    @Inject
    VyosRouterFirewallElementService _vyFwService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class, description = "the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.FIREWALL_DEVICE_ID, type = CommandType.UUID, entityType = VyosRouterFirewallResponse.class, description = "Vyos Router device ID")
    private Long fwDeviceId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getFirewallDeviceId() {
        return fwDeviceId;
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
            List<ExternalFirewallDeviceVO> fwDevices = _vyFwService.listVyosRouterFirewalls(this);
            ListResponse<VyosRouterFirewallResponse> response = new ListResponse<VyosRouterFirewallResponse>();
            List<VyosRouterFirewallResponse> fwDevicesResponse = new ArrayList<VyosRouterFirewallResponse>();

            if (fwDevices != null && !fwDevices.isEmpty()) {
                for (ExternalFirewallDeviceVO fwDeviceVO : fwDevices) {
                    VyosRouterFirewallResponse deviceResponse = _vyFwService.createVyosRouterFirewallResponse(fwDeviceVO);
                    fwDevicesResponse.add(deviceResponse);
                }
            }

            response.setResponses(fwDevicesResponse);
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