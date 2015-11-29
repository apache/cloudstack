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

import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.server.auth.UserAuthenticator;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;

public class LdapAuthenticator extends AdapterBase implements UserAuthenticator {
    private static final Logger s_logger = Logger.getLogger(LdapAuthenticator.class.getName());

    @Inject
    private LdapManager _ldapManager;
    @Inject
    private UserAccountDao _userAccountDao;
    @Inject
    private AccountManager _accountManager;

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
            final UserAccount user = _userAccountDao.getUserAccount(username, domainId);
            LdapTrustMapVO ldapTrustMapVO = _ldapManager.getDomainLinkedToLdap(domainId);
            if(ldapTrustMapVO != null) {
                try {
                    LdapUser ldapUser = _ldapManager.getUser(username, ldapTrustMapVO.getType().toString(), ldapTrustMapVO.getName());
                    if(!ldapUser.isDisabled()) {
                        result = _ldapManager.canAuthenticate(ldapUser.getPrincipal(), password);
                        if(result) {
                            if(user == null) {
                                // import user to cloudstack
                                createCloudStackUserAccount(ldapUser, domainId, ldapTrustMapVO.getAccountType());
                            } else {
                                enableUserInCloudStack(user);
                            }
                        }
                    } else {
                        //disable user in cloudstack
                        disableUserInCloudStack(user);
                    }
                } catch (NoLdapUserMatchingQueryException e) {
                    s_logger.debug(e.getMessage());
                }

            } else {
                //domain is not linked to ldap follow normal authentication
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
            if (!result && user != null) {
                action = ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT;
            }
        }

        return new Pair<Boolean, ActionOnFailedAuthentication>(result, action);
    }

    private void enableUserInCloudStack(UserAccount user) {
        if(user != null && (user.getState().equalsIgnoreCase(Account.State.disabled.toString()))) {
            _accountManager.enableUser(user.getId());
        }
    }

    private void createCloudStackUserAccount(LdapUser user, long domainId, short accountType) {
        String username = user.getUsername();
        _accountManager.createUserAccount(username, "", user.getFirstname(), user.getLastname(), user.getEmail(), null, username, accountType, domainId, username, null,
                                          UUID.randomUUID().toString(), UUID.randomUUID().toString(), User.Source.LDAP);
    }

    private void disableUserInCloudStack(UserAccount user) {
        if (user != null) {
            _accountManager.disableUser(user.getId());
        }
    }

    @Override
    public String encode(final String password) {
        return password;
    }
}
