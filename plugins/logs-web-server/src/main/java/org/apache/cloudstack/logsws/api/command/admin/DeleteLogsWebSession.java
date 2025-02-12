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

package org.apache.cloudstack.logsws.api.command.admin;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.logsws.LogsWebSessionApiService;
import org.apache.cloudstack.logsws.LogsWebSession;
import org.apache.cloudstack.logsws.api.response.LogsWebSessionResponse;

import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "deleteLogsWebSession",
        description = "Deletes a logs web session",
        responseObject = SuccessResponse.class,
        entityType = {LogsWebSession.class},
        authorized = {RoleType.Admin},
        since = "4.21.0")
public class DeleteLogsWebSession extends BaseCmd {

    @Inject
    LogsWebSessionApiService logsWebSessionApiService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = BaseCmd.CommandType.UUID,
            entityType = LogsWebSessionResponse.class,
            required = true,
            description = "The ID of the logs web session")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getId() {
        return id;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() throws ServerApiException {
        try {
            if (!logsWebSessionApiService.deleteLogsWebSession(this)) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                        String.format("Failed to delete log web session ID: %d", getId()));
            }
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
