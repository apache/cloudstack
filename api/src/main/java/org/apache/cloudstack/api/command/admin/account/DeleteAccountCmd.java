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
package org.apache.cloudstack.api.command.admin.account;

import javax.inject.Inject;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.region.RegionService;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;

@APICommand(name = "deleteAccount", description = "Deletes a account, and all users associated with this account", responseObject = SuccessResponse.class, entityType = {Account.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DeleteAccountCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteAccountCmd.class.getName());
    private static final String s_name = "deleteaccountresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = AccountResponse.class, required = true, description = "Account id")
    private Long id;

    @Inject
    RegionService _regionService;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public static String getStaticName() {
        return s_name;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();// Let's give the caller here for event logging.
        if (account != null) {
            return account.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_ACCOUNT_DELETE;
    }

    @Override
    public String getEventDescription() {
        Account account = _accountService.getAccount(getId());
        return (account != null ? "Deleting user account " + account.getAccountName() + " (ID: " + account.getUuid() + ") and all corresponding users"
            : "Account delete, but this account does not exist in the system");
    }

    @Override
    public void execute() {
        Account account = _accountService.getAccount(getId());
        CallContext.current().setEventDetails("Account ID: " + (account != null ? account.getUuid() : getId())); // Account not found is already handled by service

        boolean result = _regionService.deleteUserAccount(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete user account and all corresponding users");
        }
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Account;
    }

    @Override
    public Long getApiResourceId() {
        return id;
    }
}
