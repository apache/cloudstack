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

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.BaseListCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.PlugService;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.CiscoVnmcResourceResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.cisco.CiscoVnmcResourceVO;
import com.cloud.network.element.CiscoVnmcElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@Implementation(responseObject=CiscoVnmcResourceResponse.class, description="Lists Cisco VNMC controllers")
public class ListCiscoVnmcResourcesCmd extends BaseListCmd {
    private static final Logger s_logger = Logger.getLogger(ListCiscoVnmcResourcesCmd.class.getName());
    private static final String s_name = "listCiscoVnmcResources";
    @PlugService CiscoVnmcElementService _ciscoVnmcElementService;

   /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="physical_network")
    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.LONG, description="the Physical Network ID")
    private Long physicalNetworkId;

    @IdentityMapper(entityTableName="external_cisco_vnmc_resources")
    @Parameter(name=ApiConstants.RESOURCE_ID, type=CommandType.LONG,  description="Cisco VNMC  resource ID")
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
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
            List<CiscoVnmcResourceVO> CiscoVnmcResources = _ciscoVnmcElementService.listCiscoVnmcResources(this);
            ListResponse<CiscoVnmcResourceResponse> response = new ListResponse<CiscoVnmcResourceResponse>();
            List<CiscoVnmcResourceResponse> CiscoVnmcResourcesResponse = new ArrayList<CiscoVnmcResourceResponse>();

            if (CiscoVnmcResources != null && !CiscoVnmcResources.isEmpty()) {
                for (CiscoVnmcResourceVO CiscoVnmcResourceVO : CiscoVnmcResources) {
                    CiscoVnmcResourceResponse CiscoVnmcResourceResponse = _ciscoVnmcElementService.createCiscoVnmcResourceResponse(CiscoVnmcResourceVO);
                    CiscoVnmcResourcesResponse.add(CiscoVnmcResourceResponse);
                }
            }

            response.setResponses(CiscoVnmcResourcesResponse);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }  catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }
    
}
