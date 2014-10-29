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

package org.apache.cloudstack.network.opendaylight.api.commands;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.opendaylight.agent.OpenDaylightControllerResourceManager;
import org.apache.cloudstack.network.opendaylight.api.responses.OpenDaylightControllerResponse;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

@APICommand(name = "deleteOpenDaylightController", responseObject = OpenDaylightControllerResponse.class, description = "Removes an OpenDyalight controler",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DeleteOpenDaylightControllerCmd extends BaseAsyncCmd {
    @Inject
    private OpenDaylightControllerResourceManager resourceManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = OpenDaylightControllerResponse.class, required = true, description = "OpenDaylight Controller ID")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_EXTERNAL_OPENDAYLIGHT_DELETE_CONTROLLER;
    }

    @Override
    public String getEventDescription() {
        return "Deleted OpenDaylight Controller";
    }

    @Override
    public String getCommandName() {
        return "deleteOpenDaylightController";
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
    NetworkRuleConflictException {
        try {
            resourceManager.deleteController(this);
            SuccessResponse response = new SuccessResponse(getCommandName());
            response.setResponseName(getCommandName());
            setResponseObject(response); //FIXME
        } catch (InvalidParameterValueException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete OpenDaylight controller.");
        }
    }

}
