//
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
//
package org.apache.cloudstack.oauth2.api.command;

import com.cloud.domain.Domain;
import com.cloud.user.Account;
import com.cloud.user.UserAccount;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.oauth2.OAuth2AuthManager;
import org.apache.cloudstack.oauth2.api.response.OauthProviderResponse;

import javax.inject.Inject;
import java.util.logging.Logger;

@APICommand(name = "authorizeOauthSso", description = "Allow or disallow a user to use OAUTH SSO", responseObject = SuccessResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)

public class AuthorizeOauthSSOCmd extends BaseCmd {
    public final Logger s_logger = Logger.getLogger(AuthorizeOauthSSOCmd.class.getName());

    @Inject
    OAuth2AuthManager _oauthManager;

    private static final String s_name = "authorizeoauthssoresponse";


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.USER_ID, type = BaseCmd.CommandType.UUID, entityType = UserResponse.class, required = true, description = "User uuid")
    private Long id;

    @Parameter(name = ApiConstants.ENABLE, type = BaseCmd.CommandType.BOOLEAN, required = true, description = "If true, authorizes user to be able to use OAUTH for Single Sign. If False, disable user to user OAUTH SSO.")
    private Boolean enable;

    @Parameter(name = ApiConstants.ENTITY_ID, type = CommandType.STRING, entityType = OauthProviderResponse.class, description = "The Identity Provider ID the user is allowed to get single signed on from")
    private String OauthProviderId;


    public Long getId() {
        return id;
    }

    public Boolean getEnable() {
        return enable;
    }

    public String getOauthProviderId() {
        return OauthProviderId;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }

    @Override
    public void execute() {
        // Check permissions
        UserAccount userAccount = _accountService.getUserAccountById(getId());
        if (userAccount == null) {
            throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, "Unable to find a user account with the given ID");
        }
        Domain domain = _domainService.getDomain(userAccount.getDomainId());
        Account account = _accountService.getAccount(userAccount.getAccountId());
        _accountService.checkAccess(CallContext.current().getCallingAccount(), domain);
        _accountService.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, true, account);

        CallContext.current().setEventDetails("UserId: " + getId());
        SuccessResponse response = new SuccessResponse();
        Boolean status = false;

        if (_oauthManager.authorizeUser(getId(), getOauthProviderId(), getEnable())) {
            status = true;
        }
        response.setResponseName(getCommandName());
        response.setSuccess(status);
        setResponseObject(response);
    }
}
