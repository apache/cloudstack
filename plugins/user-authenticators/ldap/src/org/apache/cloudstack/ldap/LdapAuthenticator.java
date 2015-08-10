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
package org.apache.cloudstack.ldap;

import com.cloud.server.auth.DefaultUserAuthenticator;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

public class LdapAuthenticator extends DefaultUserAuthenticator {
    private static final Logger s_logger = Logger.getLogger(LdapAuthenticator.class.getName());

    @Inject
    private LdapManager _ldapManager;
    @Inject
    private UserAccountDao _userAccountDao;
    @Inject
    public AccountService _accountService;

    public LdapAuthenticator() {
        super();
    }

    public LdapAuthenticator(final LdapManager ldapManager, final UserAccountDao userAccountDao) {
        super();
        _ldapManager = ldapManager;
        _userAccountDao = userAccountDao;
    }

    @Override
    public Pair<Boolean, ActionOnFailedAuthentication> authenticate(final String username, final String password, final Long domainId, final Map<String, Object[]> requestParameters) {

        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            s_logger.debug("Username or Password cannot be empty");
            return new Pair<Boolean, ActionOnFailedAuthentication>(false, null);
        }

        boolean result = false;
        ActionOnFailedAuthentication action = null;

        if (_ldapManager.isLdapEnabled()) {
            LdapTrustMapVO ldapTrustMapVO = _ldapManager.getDomainLinkedToLdap(domainId);
            if(ldapTrustMapVO != null) {
                try {
                    LdapUser ldapUser = _ldapManager.getUser(username, ldapTrustMapVO.getType(), ldapTrustMapVO.getName());
                    if(!ldapUser.isDisabled()) {
                        result = _ldapManager.canAuthenticate(ldapUser.getPrincipal(), password);
                        if(result) {
                            final UserAccount user = _userAccountDao.getUserAccount(username, domainId);
                            if (user == null) {
                                // import user to cloudstack
                                createCloudStackUserAccount(ldapUser, domainId);
                            }
                        }
                    } else {
                        //disable user in cloudstack
                        disableUserInCloudStack(ldapUser, domainId);
                    }
                } catch (NoLdapUserMatchingQueryException e) {
                    s_logger.debug(e.getMessage());
                }

            } else {
                //domain is not linked to ldap follow normal authentication
                final UserAccount user = _userAccountDao.getUserAccount(username, domainId);
                if(user != null ) {
                    try {
                        LdapUser ldapUser = _ldapManager.getUser(username);
                        if(!ldapUser.isDisabled()) {
                            result = _ldapManager.canAuthenticate(ldapUser.getPrincipal(), password);
                        } else {
                            s_logger.debug("user with principal "+ ldapUser.getPrincipal() + " is disabled in ldap");
                        }
                    } catch (NoLdapUserMatchingQueryException e) {
                        s_logger.debug(e.getMessage());
                    }
                }
            }
        }

        if (!result) {
            action = ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT;
        }
        return new Pair<Boolean, ActionOnFailedAuthentication>(result, action);
    }

    private void createCloudStackUserAccount(LdapUser user, long domainId) {
        String username = user.getUsername();
        _accountService.createUserAccount(username, "", user.getFirstname(), user.getLastname(), user.getEmail(), "GMT", username, Account.ACCOUNT_TYPE_DOMAIN_ADMIN, domainId,
                                          username, null, UUID.randomUUID().toString(), UUID.randomUUID().toString(), User.Source.LDAP);
    }

    private void disableUserInCloudStack(LdapUser ldapUser, long domainId) {
        final UserAccount user = _userAccountDao.getUserAccount(ldapUser.getUsername(), domainId);
        _accountService.lockUser(user.getId());
    }

    @Override
    public String encode(final String password) {
        return password;
    }
}
