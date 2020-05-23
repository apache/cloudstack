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
import com.cloud.utils.exception.CloudRuntimeException;
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

@APICommand(name = "declareHostAsDead", description = "Declares host as dead. Host must be on disconnected or alert state.", responseObject = HostResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.15", authorized = {RoleType.Admin})
public class DeclareHostAsDeadCmd extends BaseAsyncCmd {

    private static final String S_NAME = "declarehostasdead";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HOST_ID, type = BaseCmd.CommandType.UUID, entityType = HostResponse.class, description = "ID of the host", required = true, validations = {ApiArgValidator.PositiveNumber})
    private Long hostId;

    @Parameter(name = ApiConstants.FORCE_DELETE_HOST, type = CommandType.BOOLEAN, description = "Forces delete host and destroys local storage on this host (if exists).")
    private Boolean forceDeleteHost;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getHostId() {
        return hostId;
    }

    public boolean isForceDeleteHost() {
        return forceDeleteHost;
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
        return EventTypes.EVENT_DECLARE_HOST_DEAD;
    }

    @Override
    public String getEventDescription() {
        return "declaring host: " + getHostId() + " as dead";
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.Host;
    }

    @Override
    public Long getInstanceId() {
        return getHostId();
    }

    @Override
    public void execute() {
        try {
            Host result = _resourceService.declareHostAsDead(this);
            if (result != null) {
                HostResponse response = _responseGenerator.createHostResponse(result);
                response.setResponseName("host");
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to declare host as dead");
            }
        } catch (CloudRuntimeException exception) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to declare host as dead due to: " + exception.getMessage());
        }
    }

}
