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
import com.cloud.utils.exception.CloudRuntimeException;

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
                    // We have a single mapping of a domain to an ldap group or ou
                    return authenticate(username, password, domainId, user, ldapTrustMapVOs.get(0));
                } else {
                    // we are dealing with mapping of accounts in a domain to ldap groups
                    return authenticate(username, password, domainId, user, ldapTrustMapVOs);
                }
            } else {
                //domain is not linked to ldap follow normal authentication
                return authenticate(username, password, domainId, user);
            }
        }

        return rc;
    }

    /**
     * checks if the user exists in ldap and create in cloudstack if needed.
     *
     * @param username login id
     * @param password pass phrase
     * @param domainId domain the user is trying to log on to
     * @param userAccount cloudstack user object
     * @param ldapTrustMapVOs the trust mappings of accounts in the domain to ldap groups
     * @return false if the ldap user object does not exist, is not mapped to an account, is mapped to multiple accounts or if authenitication fails
     */
    private Pair<Boolean, ActionOnFailedAuthentication> authenticate(String username, String password, Long domainId, UserAccount userAccount, List<LdapTrustMapVO> ldapTrustMapVOs) {
        Pair<Boolean, ActionOnFailedAuthentication> rc = new Pair<Boolean, ActionOnFailedAuthentication>(false, null);
        try {
            LdapUser ldapUser = _ldapManager.getUser(username, domainId);
            List<String> memberships = ldapUser.getMemberships();
            List<String> mappedGroups = getMappedGroups(ldapTrustMapVOs);
            mappedGroups.retainAll(memberships);
            // check membership, there must be only one match in this domain
            if(ldapUser.isDisabled()) {
                logAndDisable(userAccount, "attempt to log on using disabled ldap user " + userAccount.getUsername(), false);
            } else if(mappedGroups.size() > 1) {
                logAndDisable(userAccount, "user '" + username + "' is mapped to more then one account in domain and will be disabled.", false);
            } else if(mappedGroups.size() < 1) {
                logAndDisable(userAccount, "user '" + username + "' is not mapped to an account in domain and will be removed.", true);
            } else {
                // a valid ldap configured user exists
                LdapTrustMapVO mapping = _ldapManager.getLinkedLdapGroup(domainId,mappedGroups.get(0));
                // we could now assert that ldapTrustMapVOs.contains(mapping);
                // createUser in Account can only be done by account name not by account id
                String accountName = _accountManager.getAccount(mapping.getAccountId()).getAccountName();
                rc.first(_ldapManager.canAuthenticate(ldapUser.getPrincipal(), password, domainId));
                // for security reasons we keep processing on faulty login attempt to not give a way information on userid existence
                if (userAccount == null) {
                    // new user that is in ldap; authenticate and create
                    User user = _accountManager.createUser(username, "", ldapUser.getFirstname(), ldapUser.getLastname(), ldapUser.getEmail(), null, accountName,
                            domainId, UUID.randomUUID().toString(), User.Source.LDAP);
                    /* expected error conditions:
                     *
                     * caught in APIServlet: CloudRuntimeException("The domain " + domainId + " does not exist; unable to create user");
                     * caught in APIServlet: CloudRuntimeException("The user cannot be created as domain " + domain.getName() + " is being deleted");
                     * would have been thrown above: InvalidParameterValueException("Unable to find account " + accountName + " in domain id=" + domainId + " to create user");
                     * we are system user: PermissionDeniedException("Account id : " + account.getId() + " is a system account, can't add a user to it");
                     * serious and must be thrown: CloudRuntimeException("The user " + userName + " already exists in domain " + domainId);
                     * fatal system error and must be thrown: CloudRuntimeException("Failed to encode password");
                     */
                    userAccount = _accountManager.getUserAccountById(user.getId());
                } else {
                    // not a new user, check if mapped group has changed
                    if(userAccount.getAccountId() != mapping.getAccountId()) {
                        final Account mappedAccount = _accountManager.getAccount(mapping.getAccountId());
                        if (mappedAccount == null || mappedAccount.getRemoved() != null) {
                            throw new CloudRuntimeException("Mapped account for users does not exist. Please contact your administrator.");
                        }
                        _accountManager.moveUser(userAccount.getId(), userAccount.getDomainId(), mappedAccount);
                    }
                    // else { the user hasn't changed in ldap, the ldap group stayed the same, hurray, pass, fun thou self a lot of fun }
                }
            }
        } catch (NoLdapUserMatchingQueryException e) {
            s_logger.debug(e.getMessage());
            disableUserInCloudStack(userAccount);
        }

        return rc;
    }

    private void logAndDisable(UserAccount userAccount, String msg, boolean remove) {
        if (s_logger.isInfoEnabled()) {
            s_logger.info(msg);
        }
        if(remove) {
            removeUserInCloudStack(userAccount);
        } else {
            disableUserInCloudStack(userAccount);
        }
    }

    private List<String> getMappedGroups(List<LdapTrustMapVO> ldapTrustMapVOs) {
        List<String> groups = new ArrayList<>();
        for (LdapTrustMapVO vo : ldapTrustMapVOs) {
            groups.add(vo.getName());
        }
        return groups;
    }

    /**
     * checks if the user exists in ldap and create in cloudstack if needed
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
            final short accountType = ldapTrustMapVO.getAccountType();
            processLdapUser(password, domainId, user, rc, ldapUser, accountType);
        } catch (NoLdapUserMatchingQueryException e) {
            s_logger.debug(e.getMessage());
        }
        return rc;
    }

    private void processLdapUser(String password, Long domainId, UserAccount user, Pair<Boolean, ActionOnFailedAuthentication> rc, LdapUser ldapUser, short accountType) {
        if(!ldapUser.isDisabled()) {
            rc.first(_ldapManager.canAuthenticate(ldapUser.getPrincipal(), password, domainId));
            if(rc.first()) {
                if(user == null) {
                    // import user to cloudstack
                    createCloudStackUserAccount(ldapUser, domainId, accountType);
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
    }

    /**
     * checks if the user is configured both in ldap and in cloudstack.
     * @param username login id
     * @param password pass phrase
     * @param domainId domain the user is trying to log on to
     * @param user cloudstack user object
     * @return false if either user object does not exist or authenitication fails
     */
    Pair<Boolean, ActionOnFailedAuthentication> authenticate(String username, String password, Long domainId, UserAccount user) {
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
                new Pair<Boolean, ActionOnFailedAuthentication>(result, ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT):
                new Pair<Boolean, ActionOnFailedAuthentication>(result, null);
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

    private void removeUserInCloudStack(UserAccount user) {
        if (user != null) {
            _accountManager.disableUser(user.getId());
        }
    }

    @Override
    public String encode(final String password) {
        return password;
    }
}
