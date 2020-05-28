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
package org.apache.cloudstack.api.command.admin.host;

import com.cloud.event.EventTypes;
import com.cloud.host.Host;
import com.cloud.utils.fsm.NoTransitionException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = "cancelHostAsDead",
        description = "Cancel host status from 'Dead'. Host will transit back to status 'Enabled'.",
        responseObject = HostResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class CancelHostAsDeadCmd extends BaseAsyncCmd {

    private static final String S_NAME = "cancelhostasdeadresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = BaseCmd.CommandType.UUID, entityType = HostResponse.class, description = "host ID", required = true, validations = {ApiArgValidator.PositiveNumber})
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return S_NAME;
    }

    public static String getResultObjectName() {
        return "host";
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_CANCEL_HOST_DEAD;
    }

    @Override
    public String getEventDescription() {
        return "declaring host: " + getId() + " as dead";
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.Host;
    }

    @Override
    public Long getInstanceId() {
        return getId();
    }

    @Override
    public void execute() {
        try {
            Host result = _resourceService.cancelHostAsDead(this);
            if (result != null) {
                HostResponse response = _responseGenerator.createHostResponse(result);
                response.setResponseName("host");
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to cancel host from Dead status");
            }
        } catch (NoTransitionException exception) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to Cancel host from Dead status due to: " + exception.getMessage());
        }
    }

}
