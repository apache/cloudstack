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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
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
        Pair<Boolean, ActionOnFailedAuthentication> rc = new Pair<Boolean, ActionOnFailedAuthentication>(false, null);

        // TODO not allowing an empty password is a policy we shouldn't decide on. A private cloud may well want to allow this.
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            s_logger.debug("Username or Password cannot be empty");
            return rc;
        }

        if (_ldapManager.isLdapEnabled()) {
            final UserAccount user = _userAccountDao.getUserAccount(username, domainId);
            List<LdapTrustMapVO> ldapTrustMapVOs = _ldapManager.getDomainLinkage(domainId);
            if(ldapTrustMapVOs != null && ldapTrustMapVOs.size() > 0) {
                if(ldapTrustMapVOs.size() == 1 && ldapTrustMapVOs.get(0).getAccountId() == 0) {
                    return authenticate(username, password, domainId, user, ldapTrustMapVOs.get(0));
                } else {
                    return authenticateInConfiguredDomain(username, password, domainId, user, ldapTrustMapVOs);
                }
            } else {
                //domain is not linked to ldap follow normal authentication
                return authenticate(username, password, domainId, user);
            }
        }

        return rc;
    }

    private Pair<Boolean, ActionOnFailedAuthentication> authenticateInConfiguredDomain(String username, String password, Long domainId, UserAccount user, List<LdapTrustMapVO> ldapTrustMapVOs) {
        Pair<Boolean, ActionOnFailedAuthentication> rc = new Pair<Boolean, ActionOnFailedAuthentication>(false, null);
        try {
            LdapUser ldapUser = _ldapManager.getUser(username, domainId);
            List<String> memberships = ldapUser.getMemberships();
            List<String> mappedGroups = getMappedGroups(ldapTrustMapVOs);
            mappedGroups.retainAll(memberships);
            // check membership, there must be only one match in this domain
            if(mappedGroups.size() > 1 || mappedGroups.size() < 1) {
                disableUserInCloudStack(user);
// or                rc.second(ActionOnFailedAuthentication.DISABLE_USER);
            } else {
                // TODO do stuff like find the right location the user is in now, and move or create if neccessary
            }
        } catch (NoLdapUserMatchingQueryException e) {
            s_logger.debug(e.getMessage());
            disableUserInCloudStack(user);
// or            rc.second(ActionOnFailedAuthentication.DISABLE_USER);
        }

        return rc;
    }

    private List<String> getMappedGroups(List<LdapTrustMapVO> ldapTrustMapVOs) {
        List<String> groups = new ArrayList<>();
        for (LdapTrustMapVO vo : ldapTrustMapVOs) {
            groups.add(vo.getName());
        }
        return groups;
    }

    /**
     * check if the user exists in ldap and create in cloudstack if needed
     * @param username login id
     * @param password pass phrase
     * @param domainId domain the user is trying to log on to
     * @param user cloudstack user object
     * @param ldapTrustMapVO the trust mapping for the domain to the ldap group
     * @return false if the ldap user object does not exist or authenitication fails
     */
    private Pair<Boolean, ActionOnFailedAuthentication> authenticate(String username, String password, Long domainId, UserAccount user, LdapTrustMapVO ldapTrustMapVO) {
        Pair<Boolean, ActionOnFailedAuthentication> rc = new Pair<Boolean, ActionOnFailedAuthentication>(false, null);
        try {
            LdapUser ldapUser = _ldapManager.getUser(username, ldapTrustMapVO.getType().toString(), ldapTrustMapVO.getName(), domainId);
            if(!ldapUser.isDisabled()) {
                rc.first(_ldapManager.canAuthenticate(ldapUser.getPrincipal(), password, domainId));
                if(rc.first()) {
                    if(user == null) {
                        // import user to cloudstack
                        createCloudStackUserAccount(ldapUser, domainId, ldapTrustMapVO.getAccountType());
                    } else {
                        enableUserInCloudStack(user);
                    }
                } else if(user != null) {
                    rc.second(ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT);
                }
            } else {
                //disable user in cloudstack
                disableUserInCloudStack(user);
            }
        } catch (NoLdapUserMatchingQueryException e) {
            s_logger.debug(e.getMessage());
        }
        return rc;
    }

    /**
     * checks if the user is configured both in ldap and in cloudstack.
     * @param username login id
     * @param password pass phrase
     * @param domainId domain the user is trying to log on to
     * @param user cloudstack user object
     * @return false if either user object does not exist or authenitication fails
     */
    private Pair<Boolean, ActionOnFailedAuthentication> authenticate(String username, String password, Long domainId, UserAccount user) {
        boolean result = false;

        if(user != null ) {
            try {
                LdapUser ldapUser = _ldapManager.getUser(username, domainId);
                if(!ldapUser.isDisabled()) {
                    result = _ldapManager.canAuthenticate(ldapUser.getPrincipal(), password, domainId);
                } else {
                    s_logger.debug("user with principal "+ ldapUser.getPrincipal() + " is disabled in ldap");
                }
            } catch (NoLdapUserMatchingQueryException e) {
                s_logger.debug(e.getMessage());
            }
        }
        return (!result && user != null) ?
                new Pair<Boolean, ActionOnFailedAuthentication>(false, ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT):
                new Pair<Boolean, ActionOnFailedAuthentication>(false, null);
    }

    private void enableUserInCloudStack(UserAccount user) {
        if(user != null && (user.getState().equalsIgnoreCase(Account.State.disabled.toString()))) {
            _accountManager.enableUser(user.getId());
        }
    }

    private void createCloudStackUserAccount(LdapUser user, long domainId, short accountType) {
        String username = user.getUsername();
        _accountManager.createUserAccount(username, "", user.getFirstname(), user.getLastname(), user.getEmail(), null, username,
                                          accountType, RoleType.getByAccountType(accountType).getId(), domainId, null, null,
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
