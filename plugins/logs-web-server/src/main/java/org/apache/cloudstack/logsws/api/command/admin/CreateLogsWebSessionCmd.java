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


import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.logsws.LogsWebSessionApiService;
import org.apache.cloudstack.logsws.LogsWebSession;
import org.apache.cloudstack.logsws.api.response.LogsWebSessionResponse;

import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "createLogsWebSession",
        description = "Creates a session to connect to logs web socket server",
        responseObject = LogsWebSessionResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {LogsWebSession.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin},
        since = "4.21.0")
public class CreateLogsWebSessionCmd extends BaseCmd {

    @Inject
    LogsWebSessionApiService logsWebSessionApiService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.FILTERS, type = CommandType.LIST, collectionType = CommandType.STRING,
            description = "List of filter keywords")
    private List<String> filters;

    @Parameter(name = ApiConstants.TOKEN, type = CommandType.STRING,
            description = "(Optional) extra security token, valid when the extra validation is enabled")
    private String extraSecurityToken;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public List<String> getFilters() {
        return filters;
    }

    public String getExtraSecurityToken() {
        return extraSecurityToken;
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
            LogsWebSessionResponse response = logsWebSessionApiService.createLogsWebSession(this);
            if (response == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create logs web session");
            }
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

}
