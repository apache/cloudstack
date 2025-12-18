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
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.user.Account;
import com.cloud.user.UserData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@APICommand(name = "deleteCniConfiguration", description = "Deletes a CNI Configuration", responseObject = SuccessResponse.class, entityType = {UserData.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.21.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class DeleteCniConfigurationCmd extends DeleteUserDataCmd {

    public static final Logger logger = LogManager.getLogger(DeleteCniConfigurationCmd.class.getName());


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        boolean result = _mgr.deleteCniConfiguration(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            response.setSuccess(result);
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete CNI configuration");
        }
    }

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();
        Long domainId = this.getDomainId();
        String accountName = this.getAccountName();
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
