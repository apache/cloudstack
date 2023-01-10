/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.api.command;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.LinkAccountToLdapResponse;
import org.apache.cloudstack.api.response.LinkDomainToLdapResponse;
import org.apache.cloudstack.ldap.LdapManager;
import org.apache.cloudstack.ldap.LdapUser;
import org.apache.cloudstack.ldap.NoLdapUserMatchingQueryException;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserAccount;

@APICommand(name = "linkAccountToLdap", description = "link a cloudstack account to a group or OU in ldap", responseObject = LinkDomainToLdapResponse.class, since = "4.11.0",
    requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin,RoleType.DomainAdmin})
public class LinkAccountToLdapCmd extends BaseCmd {

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, required = true, entityType = DomainResponse.class, description = "The id of the domain that is to contain the linked account.")
    private Long domainId;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, required = false, description = "type of the ldap name. GROUP or OU, defaults to GROUP")
    private String type;

    @Parameter(name = ApiConstants.LDAP_DOMAIN, type = CommandType.STRING, required = true, description = "name of the group or OU in LDAP")
    private String ldapDomain;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, required = true, description = "name of the account, it will be created if it does not exist")
    private String accountName;

    @Parameter(name = ApiConstants.ADMIN, type = CommandType.STRING, required = false, description = "domain admin username in LDAP ")
    private String admin;

    @Parameter(name = ApiConstants.ACCOUNT_TYPE, type = CommandType.INTEGER, required = true, description = "Type of the account to auto import. Specify 0 for user and 2 for "
            + "domain admin")
    private int accountType;

    @Inject
    private LdapManager _ldapManager;

    @Override
    public void execute() throws ServerApiException {
        try {
            LinkAccountToLdapResponse response = _ldapManager.linkAccountToLdap(this);
            if (admin != null) {
                LdapUser ldapUser = null;
                try {
                    ldapUser = _ldapManager.getUser(admin, type, ldapDomain, domainId);
                } catch (NoLdapUserMatchingQueryException e) {
                    logger.debug("no ldap user matching username " + admin + " in the given group/ou", e);
                }
                if (ldapUser != null && !ldapUser.isDisabled()) {
                    Account account = _accountService.getActiveAccountByName(admin, domainId);
                    if (account == null) {
                        try {
                            UserAccount userAccount = _accountService
                                    .createUserAccount(admin, "", ldapUser.getFirstname(), ldapUser.getLastname(), ldapUser.getEmail(), null, admin, Account.Type.DOMAIN_ADMIN, RoleType.DomainAdmin.getId(), domainId, null, null, UUID.randomUUID().toString(),
                                            UUID.randomUUID().toString(), User.Source.LDAP);
                            response.setAdminId(String.valueOf(userAccount.getAccountId()));
                            logger.info("created an account with name " + admin + " in the given domain " + domainId);
                        } catch (Exception e) {
                            logger.info("an exception occurred while creating account with name " + admin + " in domain " + domainId, e);
                        }
                    } else {
                        logger.debug("an account with name " + admin + " already exists in the domain " + domainId);
                    }
                } else {
                    logger.debug("ldap user with username " + admin + " is disabled in the given group/ou");
                }
            }
            response.setObjectName(this.getActualCommandName());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (final InvalidParameterValueException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.toString());
        }
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getType() {
        return type;
    }

    public String getLdapDomain() {
        return ldapDomain;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getAdmin() {
        return admin;
    }

    public Account.Type getAccountType() {
        return Account.Type.getFromValue(accountType);
    }

    @Override
    public Long getApiResourceId() {
        Account account = _accountService.getActiveAccountByName(accountName, domainId);
        return account != null ? account.getAccountId() : null;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Account;
    }
}
