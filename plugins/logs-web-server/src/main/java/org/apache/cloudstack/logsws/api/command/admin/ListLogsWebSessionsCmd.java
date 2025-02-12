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
import org.apache.cloudstack.api.BaseListAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.logsws.LogsWebSessionApiService;
import org.apache.cloudstack.logsws.LogsWebSession;
import org.apache.cloudstack.logsws.api.response.LogsWebSessionResponse;

@APICommand(name = "listLogsWebSessions",
        description = "Lists logs web sessions",
        responseObject = LogsWebSessionResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {LogsWebSession.class},
        authorized = {RoleType.Admin},
        since = "4.21.0")
public class ListLogsWebSessionsCmd extends BaseListAccountResourcesCmd {

    @Inject
    LogsWebSessionApiService logsWebSessionApiService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = LogsWebSessionResponse.class,
            description = "The ID of the logs web session")
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
    public void execute() throws ServerApiException {
        ListResponse<LogsWebSessionResponse> response = logsWebSessionApiService.listLogsWebSessions(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
