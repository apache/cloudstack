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
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.api.response.CiscoAsa1000vResourceResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.cisco.CiscoAsa1000vDevice;
import com.cloud.network.element.CiscoAsa1000vService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name="addCiscoAsa1000vResource", responseObject=CiscoAsa1000vResourceResponse.class, description="Adds a Cisco Asa 1000v appliance")
public class AddCiscoAsa1000vResourceCmd extends BaseCmd {
    private static final Logger s_logger = Logger.getLogger(AddCiscoAsa1000vResourceCmd.class.getName());
    private static final String s_name = "addCiscoAsa1000vResource";
    @Inject CiscoAsa1000vService _ciscoAsa1000vService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.UUID, entityType = PhysicalNetworkResponse.class, required=true, description="the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name=ApiConstants.HOST_NAME, type=CommandType.STRING, required = true, description="Hostname or ip address of the Cisco ASA 1000v appliance.")
    private String host;

    @Parameter(name=ApiConstants.ASA_INSIDE_PORT_PROFILE, type=CommandType.STRING, required = true, description="Nexus port profile associated with inside interface of ASA 1000v")
    private String inPortProfile;

    @Parameter(name=ApiConstants.CLUSTER_ID, type=CommandType.UUID, entityType = ClusterResponse.class, required=true, description="the Cluster ID")
    private Long clusterId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public String getManagementIp() {
        return host;
    }

    public String getInPortProfile() {
        return inPortProfile;
    }

    public Long getClusterId() {
        return clusterId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
            CiscoAsa1000vDevice ciscoAsa1000v = _ciscoAsa1000vService.addCiscoAsa1000vResource(this);
            if (ciscoAsa1000v != null) {
                CiscoAsa1000vResourceResponse response = _ciscoAsa1000vService.createCiscoAsa1000vResourceResponse(ciscoAsa1000v);
                response.setObjectName("CiscoAsa1000vResource");
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add Cisco ASA 1000v appliance due to internal error.");
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
}
