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

import com.cloud.api.response.NiciraNvpDeviceResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NiciraNvpDeviceVO;
import com.cloud.network.element.NiciraNvpElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "addNiciraNvpDevice", responseObject = NiciraNvpDeviceResponse.class, description = "Adds a Nicira NVP device",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class AddNiciraNvpDeviceCmd extends BaseAsyncCmd {
    private static final String s_name = "addniciranvpdeviceresponse";
    @Inject
    protected NiciraNvpElementService niciraNvpElementService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID,
               type = CommandType.UUID,
               entityType = PhysicalNetworkResponse.class,
               required = true,
               description = "the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.HOST_NAME, type = CommandType.STRING, required = true, description = "Hostname of ip address of the Nicira NVP Controller.")
    private String host;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = true, description = "Credentials to access the Nicira Controller API")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, required = true, description = "Credentials to access the Nicira Controller API")
    private String password;

    @Parameter(name = ApiConstants.NICIRA_NVP_TRANSPORT_ZONE_UUID,
               type = CommandType.STRING,
               required = true,
               description = "The Transportzone UUID configured on the Nicira Controller")
    private String transportzoneuuid;

    @Parameter(name = ApiConstants.NICIRA_NVP_GATEWAYSERVICE_UUID,
               type = CommandType.STRING,
               required = false,
               description = "The L3 Gateway Service UUID configured on the Nicira Controller")
    private String l3gatewayserviceuuid;

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

    public String getL3GatewayServiceUuid() {
        return l3gatewayserviceuuid;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException {
        try {
            NiciraNvpDeviceVO niciraNvpDeviceVO = niciraNvpElementService.addNiciraNvpDevice(this);
            if (niciraNvpDeviceVO != null) {
                NiciraNvpDeviceResponse response = niciraNvpElementService.createNiciraNvpDeviceResponse(niciraNvpDeviceVO);
                response.setObjectName("niciranvpdevice");
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add Nicira NVP device due to internal error.");
            }
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

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_EXTERNAL_NVP_CONTROLLER_ADD;
    }

    @Override
    public String getEventDescription() {
        return "Adding a Nicira Nvp Controller";
    }
}
