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

import com.cloud.api.response.BrocadeVcsDeviceResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.BrocadeVcsDeviceVO;
import com.cloud.network.brocade.Constants;
import com.cloud.network.element.BrocadeVcsElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "addBrocadeVcsDevice", responseObject = BrocadeVcsDeviceResponse.class, description = "Adds a Brocade VCS Switch", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class AddBrocadeVcsDeviceCmd extends BaseAsyncCmd {
    @Inject
    protected BrocadeVcsElementService brocadeVcsElementService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class, required = true, description = "the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.HOST_NAME, type = CommandType.STRING, required = true, description = "Hostname of ip address of the Brocade VCS Switch.")
    private String host;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = true, description = "Credentials to access the Brocade VCS Switch API")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, required = true, description = "Credentials to access the Brocade VCS Switch API")
    private String password;

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

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
            BrocadeVcsDeviceVO brocadeVcsDeviceVO = brocadeVcsElementService.addBrocadeVcsDevice(this);
            if (brocadeVcsDeviceVO != null) {
                BrocadeVcsDeviceResponse response = brocadeVcsElementService.createBrocadeVcsDeviceResponse(brocadeVcsDeviceVO);
                response.setObjectName("brocadevcsdevice");
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add Brocade VCS Switch due to internal error.");
            }
        } catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public String getEventType() {
        return Constants.EVENT_EXTERNAL_VCS_CONTROLLER_ADD;
    }

    @Override
    public String getEventDescription() {
        return "Adding a Brocade VCS Switch";
    }
}
