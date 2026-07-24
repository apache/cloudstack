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
package com.cloud.api.auth;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.UserTwoFactorAuthenticationBackupCodesResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.user.AccountManager;

@APICommand(name = GenerateUserTwoFactorAuthenticationBackupCodesCmd.APINAME,
        description = "Generates a fresh set of one-time backup (recovery) codes for the caller's two factor authentication. " +
                "Any previously generated backup codes are invalidated. The codes are returned only once.",
        authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.ResourceAdmin, RoleType.User},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true,
        responseObject = UserTwoFactorAuthenticationBackupCodesResponse.class, entityType = {}, since = "4.22.2.0")
public class GenerateUserTwoFactorAuthenticationBackupCodesCmd extends BaseCmd {

    public static final String APINAME = "generateUserTwoFactorAuthenticationBackupCodes";

    @Inject
    private AccountManager accountManager;

    @Override
    public void execute() throws ServerApiException {
        UserTwoFactorAuthenticationBackupCodesResponse response = accountManager.generateUserTwoFactorAuthenticationBackupCodes(this);
        response.setObjectName("backupcodes2fa");
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.User;
    }
}
