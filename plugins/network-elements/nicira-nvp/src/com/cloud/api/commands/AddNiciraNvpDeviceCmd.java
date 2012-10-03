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

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.PlugService;
import com.cloud.api.ServerApiException;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.response.NiciraNvpDeviceResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.ExternalLoadBalancerDeviceVO;
import com.cloud.network.NiciraNvpDeviceVO;
import com.cloud.network.element.NiciraNvpElementService;
import com.cloud.user.UserContext;
import com.cloud.utils.exception.CloudRuntimeException;

@Implementation(responseObject=NiciraNvpDeviceResponse.class, description="Adds a Nicira NVP device")
public class AddNiciraNvpDeviceCmd extends BaseCmd {
    private static final Logger s_logger = Logger.getLogger(AddNiciraNvpDeviceCmd.class.getName());
    private static final String s_name = "addniciranvpdevice";
    @PlugService NiciraNvpElementService _niciraNvpElementService;
    
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="physical_network")
    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.LONG, required=true, description="the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name=ApiConstants.HOST_NAME, type=CommandType.STRING, required = true, description="Hostname of ip address of the Nicira NVP Controller.")
    private String host;

    @Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, required = true, description="Credentials to access the Nicira Controller API")
    private String username;
    
    @Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, required = true, description="Credentials to access the Nicira Controller API")
    private String password;
    
    @Parameter(name=ApiConstants.NICIRA_NVP_TRANSPORT_ZONE_UUID, type=CommandType.STRING, required = true, description="The Transportzone UUID configured on the Nicira Controller")
    private String transportzoneuuid;
    
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
    
    public String getTransportzoneUuid() {
        return transportzoneuuid;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
            NiciraNvpDeviceVO niciraNvpDeviceVO = _niciraNvpElementService.addNiciraNvpDevice(this);
            if (niciraNvpDeviceVO != null) {
                NiciraNvpDeviceResponse response = _niciraNvpElementService.createNiciraNvpDeviceResponse(niciraNvpDeviceVO);
                response.setObjectName("niciranvpdevice");
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(BaseAsyncCmd.INTERNAL_ERROR, "Failed to add Nicira NVP device due to internal error.");
            }
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

    @Override
    public long getEntityOwnerId() {
        return UserContext.current().getCaller().getId();
    }
}
