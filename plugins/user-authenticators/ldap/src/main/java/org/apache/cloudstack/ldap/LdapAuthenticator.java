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
import org.apache.commons.lang3.StringUtils;
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
    private static final Logger LOGGER = Logger.getLogger(LdapAuthenticator.class.getName());

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

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Retrieving ldap user: " + username);
        }

        // TODO not allowing an empty password is a policy we shouldn't decide on. A private cloud may well want to allow this.
        if (StringUtils.isNoneEmpty(username, password)) {
            if (_ldapManager.isLdapEnabled(domainId) || _ldapManager.isLdapEnabled()) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("LDAP is enabled in the ldapManager");
                }
                final UserAccount user = _userAccountDao.getUserAccount(username, domainId);
                if (user != null && ! User.Source.LDAP.equals(user.getSource())) {
                    return rc;
                }
                List<LdapTrustMapVO> ldapTrustMapVOs = getLdapTrustMapVOS(domainId);
                if(ldapTrustMapVOs != null && ldapTrustMapVOs.size() > 0) {
                    if(ldapTrustMapVOs.size() == 1 && ldapTrustMapVOs.get(0).getAccountId() == 0) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("We have a single mapping of a domain to an ldap group or ou");
                        }
                        rc = authenticate(username, password, domainId, user, ldapTrustMapVOs.get(0));
                    } else {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("we are dealing with mapping of accounts in a domain to ldap groups");
                        }
                        rc = authenticate(username, password, domainId, user, ldapTrustMapVOs);
                    }
                } else {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(String.format("'this' domain (%d) is not linked to ldap follow normal authentication", domainId));
                    }
                    rc = authenticate(username, password, domainId, user);
                }
            }
        } else {
            LOGGER.debug("Username or Password cannot be empty");
        }

        return rc;
    }

    private List<LdapTrustMapVO> getLdapTrustMapVOS(Long domainId) {
        return _ldapManager.getDomainLinkage(domainId);
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
    Pair<Boolean, ActionOnFailedAuthentication> authenticate(String username, String password, Long domainId, UserAccount userAccount, List<LdapTrustMapVO> ldapTrustMapVOs) {
        Pair<Boolean, ActionOnFailedAuthentication> rc = new Pair<Boolean, ActionOnFailedAuthentication>(false, null);
        try {
            LdapUser ldapUser = _ldapManager.getUser(username, domainId);
            List<String> memberships = ldapUser.getMemberships();
            tracelist("memberships for " + username, memberships);
            List<String> mappedGroups = getMappedGroups(ldapTrustMapVOs);
            tracelist("mappedgroups for " + username, mappedGroups);
            mappedGroups.retainAll(memberships);
            tracelist("actual groups for " + username, mappedGroups);
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
                // createUser in Account can only be done by account name not by account id;
                Account account = _accountManager.getAccount(mapping.getAccountId());
                if(null == account) {
                    throw new CloudRuntimeException(String.format("account for user (%s) not found by id %d", username, mapping.getAccountId()));
                }
                String accountName = account.getAccountName();
                rc.first(_ldapManager.canAuthenticate(ldapUser.getPrincipal(), password, domainId));
                if (! rc.first()) {
                    rc.second(ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT);
                }
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
            LOGGER.debug(e.getMessage());
            disableUserInCloudStack(userAccount);
        }

        return rc;
    }

    private void tracelist(String msg, List<String> listToTrace) {
        if (LOGGER.isTraceEnabled()) {
            StringBuilder logMsg = new StringBuilder();
            logMsg.append(msg);
            logMsg.append(':');
            for (String listMember : listToTrace) {
                logMsg.append(' ');
                logMsg.append(listMember);
            }
            LOGGER.trace(logMsg.toString());
        }
    }

    private void logAndDisable(UserAccount userAccount, String msg, boolean remove) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(msg);
        }
        if(remove) {
            removeUserInCloudStack(userAccount);
        } else {
            disableUserInCloudStack(userAccount);
        }
    }

    List<String> getMappedGroups(List<LdapTrustMapVO> ldapTrustMapVOs) {
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
            final Account.Type accountType = ldapTrustMapVO.getAccountType();
            processLdapUser(password, domainId, user, rc, ldapUser, accountType);
        } catch (NoLdapUserMatchingQueryException e) {
            LOGGER.debug(e.getMessage());
            // no user in ldap ==>> disable user in cloudstack
            disableUserInCloudStack(user);
        }
        return rc;
    }

    private void processLdapUser(String password, Long domainId, UserAccount user, Pair<Boolean, ActionOnFailedAuthentication> rc, LdapUser ldapUser, Account.Type accountType) {
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
                    LOGGER.debug("user with principal "+ ldapUser.getPrincipal() + " is disabled in ldap");
                }
            } catch (NoLdapUserMatchingQueryException e) {
                LOGGER.debug(e.getMessage());
            }
        }
        return processResultAndAction(user, result);
    }

    private Pair<Boolean, ActionOnFailedAuthentication> processResultAndAction(UserAccount user, boolean result) {
        return (!result && user != null) ?
                new Pair<Boolean, ActionOnFailedAuthentication>(result, ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT):
                new Pair<Boolean, ActionOnFailedAuthentication>(result, null);
    }

    private void enableUserInCloudStack(UserAccount user) {
        if(user != null && (user.getState().equalsIgnoreCase(Account.State.DISABLED.toString()))) {
            _accountManager.enableUser(user.getId());
        }
    }

    private void createCloudStackUserAccount(LdapUser user, long domainId, Account.Type accountType) {
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
