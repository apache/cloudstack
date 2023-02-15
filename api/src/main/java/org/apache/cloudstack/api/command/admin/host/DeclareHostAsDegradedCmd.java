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
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = "declareHostAsDegraded",
        description = "Declare host as 'Degraded'. Host must be on 'Disconnected' or 'Alert' state. The ADMIN must be sure that there are no VMs running on the respective host otherwise this command might corrupted VMs that were running on the 'Degraded' host.",
        since = "4.16.0.0",
        responseObject = HostResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class DeclareHostAsDegradedCmd extends BaseAsyncCmd {

    private static final String COMMAND_RESPONSE_NAME = "declarehostasdegradedresponse";

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

    public static String getResultObjectName() {
        return "host";
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_DECLARE_HOST_DEGRADED;
    }

    @Override
    public String getEventDescription() {
        return "declaring host: " + getId() + " as Degraded";
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Host;
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    @Override
    public void execute() {
        Host host;
        try {
            host = _resourceService.declareHostAsDegraded(this);
        } catch (NoTransitionException exception) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to declare host as Degraded due to: " + exception.getMessage());
        }

        HostResponse response = _responseGenerator.createHostResponse(host);
        response.setResponseName(COMMAND_RESPONSE_NAME);
        this.setResponseObject(response);
    }

}
