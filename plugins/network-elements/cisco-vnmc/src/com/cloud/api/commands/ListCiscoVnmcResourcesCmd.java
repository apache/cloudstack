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

import com.cloud.api.response.CiscoVnmcResourceResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.cisco.CiscoVnmcController;
import com.cloud.network.cisco.CiscoVnmcControllerVO;
import com.cloud.network.element.CiscoVnmcElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "listCiscoVnmcResources", responseObject = CiscoVnmcResourceResponse.class, description = "Lists Cisco VNMC controllers",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListCiscoVnmcResourcesCmd extends BaseListCmd {
    private static final Logger s_logger = Logger.getLogger(ListCiscoVnmcResourcesCmd.class.getName());
    private static final String s_name = "listCiscoVnmcResources";
    @Inject
    CiscoVnmcElementService _ciscoVnmcElementService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class, description = "the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.RESOURCE_ID, type = CommandType.UUID, entityType = CiscoVnmcResourceResponse.class, description = "Cisco VNMC resource ID")
    private Long ciscoVnmcResourceId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getCiscoVnmcResourceId() {
        return ciscoVnmcResourceId;
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
            List<CiscoVnmcControllerVO> ciscoVnmcResources = _ciscoVnmcElementService.listCiscoVnmcResources(this);
            ListResponse<CiscoVnmcResourceResponse> response = new ListResponse<CiscoVnmcResourceResponse>();
            List<CiscoVnmcResourceResponse> ciscoVnmcResourcesResponse = new ArrayList<CiscoVnmcResourceResponse>();

            if (ciscoVnmcResources != null && !ciscoVnmcResources.isEmpty()) {
                for (CiscoVnmcController ciscoVnmcResourceVO : ciscoVnmcResources) {
                    CiscoVnmcResourceResponse ciscoVnmcResourceResponse = _ciscoVnmcElementService.createCiscoVnmcResourceResponse(ciscoVnmcResourceVO);
                    ciscoVnmcResourceResponse.setObjectName("CiscoVnmcResource");
                    ciscoVnmcResourcesResponse.add(ciscoVnmcResourceResponse);
                }
            }

            response.setResponses(ciscoVnmcResourcesResponse);
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
