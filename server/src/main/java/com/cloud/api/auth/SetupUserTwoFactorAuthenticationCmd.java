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

import com.cloud.user.AccountManager;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.api.response.UserTwoFactorAuthenticationSetupResponse;
import org.apache.cloudstack.context.CallContext;

import javax.inject.Inject;

@APICommand(name = SetupUserTwoFactorAuthenticationCmd.APINAME, description = "Setup the 2FA for the user.", authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.ResourceAdmin, RoleType.User}, requestHasSensitiveInfo = false,
        responseObject = UserTwoFactorAuthenticationSetupResponse.class, entityType = {}, since = "4.18.0")
public class SetupUserTwoFactorAuthenticationCmd extends BaseCmd {

    public static final String APINAME = "setupUserTwoFactorAuthentication";

    @Inject
    private AccountManager accountManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PROVIDER, type = CommandType.STRING, description = "two factor authentication code")
    private String provider;

    @Parameter(name = ApiConstants.ENABLE, type = CommandType.BOOLEAN, description = "Enabled by default, provide false to disable 2FA")
    private Boolean enable = true;

    @Parameter(name = ApiConstants.USER_ID, type = CommandType.UUID, entityType = UserResponse.class, description = "optional: the id of the user for which 2FA has to be disabled")
    private Long userId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getProvider() {
        return provider;
    }

    public Boolean getEnable() {
        return enable;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public void execute() throws ServerApiException {
        UserTwoFactorAuthenticationSetupResponse response = accountManager.setupUserTwoFactorAuthentication(this);
        response.setObjectName("setup2fa");
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
