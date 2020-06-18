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
package org.apache.cloudstack.api.command.user.account;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ProjectAccountResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ProjectRoleResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.log4j.Logger;

import com.cloud.user.Account;

@APICommand(name = "listProjectAccounts", description = "Lists project's accounts", responseObject = ProjectResponse.class, since = "3.0.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListProjectAccountsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListProjectAccountsCmd.class.getName());

    private static final String s_name = "listprojectaccountsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, required = true, description = "ID of the project")
    private Long projectId;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "list accounts of the project by account name")
    private String accountName;

    @Parameter(name = ApiConstants.USER_ID, type = CommandType.UUID, entityType = UserResponse.class, description = "list invitation by user ID")
    private Long userId;

    @Parameter(name = ApiConstants.ROLE, type = CommandType.STRING, description = "list accounts of the project by role")
    private String role;

    @Parameter(name = ApiConstants.PROJECT_ROLE_ID, type = CommandType.UUID, entityType = ProjectRoleResponse.class, description = "list accounts of the project by project role id")
    private Long projectRoleId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getRole() {
        return role;
    }

    public Long getUserId() { return userId; }

    public Long getProjectRoleId() {
        return projectRoleId;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        //TODO - return project entity ownerId

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ListResponse<ProjectAccountResponse> response = _queryService.listProjectAccounts(this);
        response.setResponseName(getCommandName());

        this.setResponseObject(response);
    }
}
