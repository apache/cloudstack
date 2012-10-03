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
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.PlugService;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.element.NiciraNvpElementService;
import com.cloud.user.UserContext;
import com.cloud.utils.exception.CloudRuntimeException;

@Implementation(responseObject=SuccessResponse.class, description=" delete a nicira nvp device")
public class DeleteNiciraNvpDeviceCmd extends BaseCmd {
    private static final Logger s_logger = Logger.getLogger(DeleteNiciraNvpDeviceCmd.class.getName());
    private static final String s_name = "addniciranvpdevice";
    @PlugService NiciraNvpElementService _niciraNvpElementService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="external_nicira_nvp_devices")
    @Parameter(name=ApiConstants.NICIRA_NVP_DEVICE_ID, type=CommandType.LONG, required=true, description="Nicira device ID")
    private Long niciraNvpDeviceId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getNiciraNvpDeviceId() {
        return niciraNvpDeviceId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
            boolean result = _niciraNvpElementService.deleteNiciraNvpDevice(this);
            if (result) {
                SuccessResponse response = new SuccessResponse(getCommandName());
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to delete Nicira device.");
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
