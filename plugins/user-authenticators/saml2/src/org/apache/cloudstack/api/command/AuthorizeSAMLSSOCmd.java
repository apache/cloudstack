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
package org.apache.cloudstack.api.command;

import com.cloud.domain.Domain;
import com.cloud.user.Account;
import com.cloud.user.UserAccount;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.IdpResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.saml.SAML2AuthManager;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = "authorizeSamlSso", description = "Allow or disallow a user to use SAML SSO", responseObject = SuccessResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class AuthorizeSAMLSSOCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AuthorizeSAMLSSOCmd.class.getName());

    private static final String s_name = "authorizesamlssoresponse";

    @Inject
    SAML2AuthManager _samlAuthManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.USER_ID, type = CommandType.UUID, entityType = UserResponse.class, required = true, description = "User uuid")
    private Long id;

    @Parameter(name = ApiConstants.ENABLE, type = CommandType.BOOLEAN, required = true, description = "If true, authorizes user to be able to use SAML for Single Sign. If False, disable user to user SAML SSO.")
    private Boolean enable;

    public Boolean getEnable() {
        return enable;
    }

    public String getEntityId() {
        return entityId;
    }

    @Parameter(name = ApiConstants.ENTITY_ID, type = CommandType.STRING, entityType = IdpResponse.class, description = "The Identity Provider ID the user is allowed to get single signed on from")
    private String entityId;

    public Long getId() {
        return id;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        // Check permissions
        UserAccount userAccount = _accountService.getUserAccountById(getId());
        if (userAccount == null) {
            throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR , "Unable to find a user account with the given ID");
        }
        Domain domain = _domainService.getDomain(userAccount.getDomainId());
        Account account = _accountService.getAccount(userAccount.getAccountId());
        _accountService.checkAccess(CallContext.current().getCallingAccount(), domain);
        _accountService.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, true, account);

        CallContext.current().setEventDetails("UserId: " + getId());
        SuccessResponse response = new SuccessResponse();
        Boolean status = false;

        if (_samlAuthManager.authorizeUser(getId(), getEntityId(), getEnable())) {
            status = true;
        }
        response.setResponseName(getCommandName());
        response.setSuccess(status);
        setResponseObject(response);
    }
}