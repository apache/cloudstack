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

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;

import javax.inject.Inject;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.ldap.LdapManager;
import org.apache.cloudstack.ldap.LdapUser;
import org.apache.cloudstack.ldap.NoLdapUserMatchingQueryException;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.cloud.domain.DomainVO;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.user.UserAccount;

@APICommand(name = "ldapCreateAccount", description = "Creates an account from an LDAP user", responseObject = AccountResponse.class, since = "4.2.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class LdapCreateAccountCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(LdapCreateAccountCmd.class.getName());
    private static final String s_name = "createaccountresponse";

    @Inject
    private LdapManager _ldapManager;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "Creates the user under the specified account. If no account is specified, the username will be used as the account name.")
    private String accountName;

    @Parameter(name = ApiConstants.ACCOUNT_TYPE, type = CommandType.SHORT, required = true, description = "Type of the account.  Specify 0 for user, 1 for root admin, and 2 for domain admin")
    private Short accountType;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "Creates the user under the specified domain.")
    private Long domainId;

    @Parameter(name = ApiConstants.TIMEZONE, type = CommandType.STRING, description = "Specifies a timezone for this command. For more information on the timezone parameter, see Time Zone Format.")
    private String timezone;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = true, description = "Unique username.")
    private String username;

    @Parameter(name = ApiConstants.NETWORK_DOMAIN, type = CommandType.STRING, description = "Network domain for the account's networks")
    private String networkDomain;

    @Parameter(name = ApiConstants.ACCOUNT_DETAILS, type = CommandType.MAP, description = "details for account used to store specific parameters")
    private Map<String, String> details;

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.STRING, description = "Account UUID, required for adding account from external provisioning system")
    private String accountUUID;

    @Parameter(name = ApiConstants.USER_ID, type = CommandType.STRING, description = "User UUID, required for adding account from external provisioning system")
    private String userUUID;

    public LdapCreateAccountCmd() {
        super();
    }

    public LdapCreateAccountCmd(final LdapManager ldapManager, final AccountService accountService) {
        super();
        _ldapManager = ldapManager;
        _accountService = accountService;
    }

    UserAccount createCloudstackUserAccount(final LdapUser user, String accountName, Long domainId) {
        Account account = _accountService.getActiveAccountByName(accountName, domainId);
        if (account == null) {
            return _accountService.createUserAccount(username, generatePassword(), user.getFirstname(), user.getLastname(), user.getEmail(), timezone, accountName, accountType,
                    domainId, networkDomain, details, accountUUID, userUUID, User.Source.LDAP);
        } else {
            User newUser = _accountService.createUser(username, generatePassword(), user.getFirstname(), user.getLastname(), user.getEmail(), timezone, accountName, domainId,
                    userUUID, User.Source.LDAP);
            return _accountService.getUserAccountById(newUser.getId());
        }
    }

    private String getAccountName() {
        String name = accountName;
        if (accountName == null) {
            name = username;
        }
        return name;
    }

    private Long getDomainId() {
        Long id = domainId;
        if (id == null) {
            id = DomainVO.ROOT_DOMAIN;
        }
        return id;
    }

    @Override
    public void execute() throws ServerApiException {
        final CallContext callContext = getCurrentContext();
        String finalAccountName = getAccountName();
        Long finalDomainId = getDomainId();
        callContext.setEventDetails("Account Name: " + finalAccountName + ", Domain Id:" + finalDomainId);
        try {
            final LdapUser user = _ldapManager.getUser(username);
            validateUser(user);
            final UserAccount userAccount = createCloudstackUserAccount(user, finalAccountName, finalDomainId);
            if (userAccount != null) {
                final AccountResponse response = _responseGenerator.createUserAccountResponse(ResponseView.Full, userAccount);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create a user account");
            }
        } catch (NoLdapUserMatchingQueryException e) {
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, "No LDAP user exists with the username of " + username);
        }
    }

    private String generatePassword() throws ServerApiException {
        try {
            final SecureRandom randomGen = SecureRandom.getInstance("SHA1PRNG");
            final byte bytes[] = new byte[20];
            randomGen.nextBytes(bytes);
            return new String(Base64.encode(bytes), "UTF-8");
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to generate random password");
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    CallContext getCurrentContext() {
        return CallContext.current();
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    private boolean validateUser(final LdapUser user) throws ServerApiException {
        if (user.getEmail() == null) {
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, username + " has no email address set within LDAP");
        }
        if (user.getFirstname() == null) {
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, username + " has no firstname set within LDAP");
        }
        if (user.getLastname() == null) {
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, username + " has no lastname set within LDAP");
        }
        return true;
    }
}
