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
package org.apache.cloudstack.api.command.user.userdata;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UserDataResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.user.Account;
import com.cloud.user.UserData;


@APICommand(name = "deleteUserData", description = "Deletes a userdata", responseObject = SuccessResponse.class, entityType = {UserData.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.18",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class DeleteUserDataCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(DeleteUserDataCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, required = true, entityType = UserDataResponse.class, description = "the ID of the Userdata")
    private Long id;

    //Owner information
    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "an optional account for the userdata. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.UUID,
            entityType = DomainResponse.class,
            description = "an optional domainId for the userdata. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "an optional project for the userdata")
    private Long projectId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getProjectId() {
        return projectId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        boolean result = _mgr.deleteUserData(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            response.setSuccess(result);
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete userdata");
        }
    }

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();
        if ((account == null || _accountService.isAdmin(account.getId())) && (domainId != null && accountName != null)) {
            Account userAccount = _responseGenerator.findAccountByNameDomain(accountName, domainId);
            if (userAccount != null) {
                return userAccount.getId();
            }
        }

        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }
}
