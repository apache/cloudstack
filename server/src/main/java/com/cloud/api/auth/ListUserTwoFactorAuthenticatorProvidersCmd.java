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

import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.UserTwoFactorAuthenticatorProviderResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.auth.UserTwoFactorAuthenticator;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = ListUserTwoFactorAuthenticatorProvidersCmd.APINAME,
        description = "Lists user two factor authenticator providers",
        authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.ResourceAdmin, RoleType.User},
        responseObject = UserTwoFactorAuthenticatorProviderResponse.class, since = "4.18.0")
public class ListUserTwoFactorAuthenticatorProvidersCmd extends BaseCmd {

    public static final String APINAME = "listUserTwoFactorAuthenticatorProviders";

    @Inject
    private AccountManager accountManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = BaseCmd.CommandType.STRING, description = "List user two factor authenticator provider by name")
    private String name;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    private void setupResponse(final List<UserTwoFactorAuthenticator> providers) {
        final ListResponse<UserTwoFactorAuthenticatorProviderResponse> response = new ListResponse<>();
        final List<UserTwoFactorAuthenticatorProviderResponse> responses = new ArrayList<>();
        for (final UserTwoFactorAuthenticator provider : providers) {
            if (provider == null || (getName() != null && !provider.getName().equals(getName()))) {
                continue;
            }
            final UserTwoFactorAuthenticatorProviderResponse userTwoFactorAuthenticatorProviderResponse = new UserTwoFactorAuthenticatorProviderResponse();
            userTwoFactorAuthenticatorProviderResponse.setName(provider.getName());
            userTwoFactorAuthenticatorProviderResponse.setDescription(provider.getDescription());
            userTwoFactorAuthenticatorProviderResponse.setObjectName("providers");
            responses.add(userTwoFactorAuthenticatorProviderResponse);
        }
        response.setResponses(responses);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public void execute() {
        List<UserTwoFactorAuthenticator> providers = accountManager.listUserTwoFactorAuthenticationProviders();
        setupResponse(providers);
    }

}
