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
package com.cloud.user;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import com.cloud.api.auth.SetupUserTwoFactorAuthenticationCmd;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.command.admin.account.UpdateAccountCmd;
import org.apache.cloudstack.api.command.admin.user.DeleteUserCmd;
import org.apache.cloudstack.api.command.admin.user.MoveUserCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;
import org.apache.cloudstack.api.response.UserTwoFactorAuthenticationSetupResponse;
import org.apache.cloudstack.auth.UserTwoFactorAuthenticator;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.api.query.vo.ControlledViewEntity;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

/**
 * AccountManager includes logic that deals with accounts, domains, and users.
 *
 */
public interface AccountManager extends AccountService, Configurable {
    /**
     * Disables an account by accountId
     * @return true if disable was successful, false otherwise
     */
    boolean disableAccount(long accountId) throws ConcurrentOperationException, ResourceUnavailableException;

    boolean deleteAccount(AccountVO account, long callerUserId, Account caller);

    Long checkAccessAndSpecifyAuthority(Account caller, Long zoneId);

    Account createAccount(String accountName, Account.Type accountType, Long roleId, Long domainId, String networkDomain, Map<String, String> details, String uuid);

    /**
     * Logs out a user
     */
    void logoutUser(long userId);

    /**
     * Authenticates a user when s/he logs in.
     *
     * @param username
     *            required username for authentication
     * @param password
     *            password to use for authentication, can be null for single sign-on case
     * @param domainId
     *            id of domain where user with username resides
     * @param requestParameters
     *            the request parameters of the login request, which should contain timestamp of when the request signature is
     *            made, and the signature itself in the single sign-on case
     * @return a user object, null if the user failed to authenticate
     */
    UserAccount authenticateUser(String username, String password, Long domainId, InetAddress loginIpAddress, Map<String, Object[]> requestParameters);

    /**
     * Locate a user by their apiKey
     *
     * @param apiKey
     *            that was created for a particular user
     * @return the user/account pair if one exact match was found, null otherwise
     */
    Pair<User, Account> findUserByApiKey(String apiKey);

    boolean enableAccount(long accountId);

    void buildACLSearchBuilder(SearchBuilder<? extends ControlledEntity> sb, Long domainId, boolean isRecursive, List<Long> permittedAccounts,
            ListProjectResourcesCriteria listProjectResourcesCriteria);

    void buildACLViewSearchBuilder(SearchBuilder<? extends ControlledViewEntity> sb, Long domainId, boolean isRecursive, List<Long> permittedAccounts,
            ListProjectResourcesCriteria listProjectResourcesCriteria);

    void buildACLSearchCriteria(SearchCriteria<? extends ControlledEntity> sc, Long domainId, boolean isRecursive, List<Long> permittedAccounts,
            ListProjectResourcesCriteria listProjectResourcesCriteria);

    void buildACLSearchParameters(Account caller, Long id, String accountName, Long projectId, List<Long> permittedAccounts,
            Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject, boolean listAll, boolean forProjectInvitation);

    void buildACLViewSearchCriteria(SearchCriteria<? extends ControlledViewEntity> sc, Long domainId, boolean isRecursive, List<Long> permittedAccounts,
            ListProjectResourcesCriteria listProjectResourcesCriteria);

    /**
     * Deletes a user by userId
     *
     * @param accountId
     *            - id of the account do delete
     *
     * @return true if delete was successful, false otherwise
     */
    boolean deleteUserAccount(long accountId);

    /**
     * Updates an account
     *
     * @param cmd
     *            - the parameter containing accountId or account nameand domainId
     * @return updated account object
     */
    Account updateAccount(UpdateAccountCmd cmd);

    /**
     * Disables an account by accountName and domainId
     * @param disabled
     *            account if success
     * @return true if disable was successful, false otherwise
     */
    Account disableAccount(String accountName, Long domainId, Long accountId) throws ConcurrentOperationException, ResourceUnavailableException;

    /**
     * Enables an account by accountId
     *
     * @param accountName
     *            - the enableAccount command defining the accountId to be deleted.
     */
    Account enableAccount(String accountName, Long domainId, Long accountId);

    /**
     * Deletes user by Id
     */
    boolean deleteUser(DeleteUserCmd deleteUserCmd);

    /**
     * moves a user to another account within the same domain
     * @return true if the user was successfully moved
     */
    boolean moveUser(MoveUserCmd moveUserCmd);

    @Override
    UserAccount updateUser(UpdateUserCmd cmd);

    /**
     * Disables a user by userId
     *
     * @param userId
     *            - the userId
     * @return UserAccount object
     */
    UserAccount disableUser(long userId);

    /**
     * Enables a user
     *
     * @param userId
     *            - the userId
     * @return UserAccount object
     */
    UserAccount enableUser(long userId);

    /**
     * Locks an account by accountId. A locked account cannot access the API, but will still have running VMs/IP
     * addresses
     * allocated/etc.
     *
     * @param accountName
     *            - the LockAccount command defining the accountId to be locked.
     */
    Account lockAccount(String accountName, Long domainId, Long accountId);

    List<String> listAclGroupsByAccount(Long accountId);

    String MESSAGE_ADD_ACCOUNT_EVENT = "Message.AddAccount.Event";

    String MESSAGE_REMOVE_ACCOUNT_EVENT = "Message.RemoveAccount.Event";

    ConfigKey<Boolean> UseSecretKeyInResponse = new ConfigKey<Boolean>("Advanced", Boolean.class, "use.secret.key.in.response", "false",
            "This parameter allows the users to enable or disable of showing secret key as a part of response for various APIs. By default it is set to false.", true);

    boolean moveUser(long id, Long domainId, Account newAccount);

    UserTwoFactorAuthenticator getUserTwoFactorAuthenticator(final Long domainId, final Long userAccountId);

    void verifyUsingTwoFactorAuthenticationCode(String code, Long domainId, Long userAccountId);
    UserTwoFactorAuthenticationSetupResponse setupUserTwoFactorAuthentication(SetupUserTwoFactorAuthenticationCmd cmd);

}
