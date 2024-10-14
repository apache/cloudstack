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

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.command.admin.account.CreateAccountCmd;
import org.apache.cloudstack.api.command.admin.user.GetUserKeysCmd;
import org.apache.cloudstack.api.command.admin.user.RegisterCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;

import com.cloud.dc.DataCenter;
import com.cloud.domain.Domain;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import org.apache.cloudstack.auth.UserTwoFactorAuthenticator;

public interface AccountService {

    /**
     * Creates a new user and account, stores the password as is so encrypted passwords are recommended.
     * @return the user if created successfully, null otherwise
     */
    UserAccount createUserAccount(CreateAccountCmd accountCmd);

    UserAccount createUserAccount(String userName, String password, String firstName, String lastName, String email, String timezone, String accountName, Account.Type accountType,
                                  Long roleId, Long domainId, String networkDomain, Map<String, String> details, String accountUUID, String userUUID, User.Source source);

    /**
     * Locks a user by userId. A locked user cannot access the API, but will still have running VMs/IP addresses
     * allocated/etc.
     */
    UserAccount lockUser(long userId);

    Account getSystemAccount();

    User getSystemUser();

    User createUser(String userName, String password, String firstName, String lastName, String email, String timeZone, String accountName, Long domainId, String userUUID);

    User createUser(String userName, String password, String firstName, String lastName, String email, String timeZone, String accountName, Long domainId, String userUUID,
                    User.Source source);

    boolean isAdmin(Long accountId);

    Account finalizeOwner(Account caller, String accountName, Long domainId, Long projectId);

    Account getActiveAccountByName(String accountName, Long domainId);

    UserAccount getActiveUserAccount(String username, Long domainId);

    List<UserAccount> getActiveUserAccountByEmail(String email, Long domainId);

    UserAccount updateUser(UpdateUserCmd updateUserCmd);

    Account getActiveAccountById(long accountId);

    Account getAccount(long accountId);

    User getActiveUser(long userId);

    User getUserIncludingRemoved(long userId);

    boolean isRootAdmin(Long accountId);

    boolean isDomainAdmin(Long accountId);

    boolean isNormalUser(long accountId);

    User getActiveUserByRegistrationToken(String registrationToken);

    void markUserRegistered(long userId);

    public String[] createApiKeyAndSecretKey(RegisterCmd cmd);

    public String[] createApiKeyAndSecretKey(final long userId);

    UserAccount getUserByApiKey(String apiKey);

    RoleType getRoleType(Account account);

    void checkAccess(Account account, Domain domain) throws PermissionDeniedException;

    void checkAccess(Account account, AccessType accessType, boolean sameOwner, ControlledEntity... entities) throws PermissionDeniedException;

    void checkAccess(Account account, ServiceOffering so, DataCenter zone) throws PermissionDeniedException;

    void checkAccess(Account account, DiskOffering dof, DataCenter zone) throws PermissionDeniedException;

    void checkAccess(Account account, NetworkOffering nof, DataCenter zone) throws PermissionDeniedException;

    void checkAccess(Account account, VpcOffering vof, DataCenter zone) throws PermissionDeniedException;

    void checkAccess(User user, ControlledEntity entity);

    void checkAccess(Account account, AccessType accessType, boolean sameOwner, String apiName, ControlledEntity... entities) throws PermissionDeniedException;

    void validateAccountHasAccessToResource(Account account, AccessType accessType, Object resource);

    Long finalyzeAccountId(String accountName, Long domainId, Long projectId, boolean enabledOnly);

    /**
     * returns the user account object for a given user id
     * @param userId user id
     * @return {@link UserAccount} object if it exists else null
     */
    UserAccount getUserAccountById(Long userId);

    public Map<String, String> getKeys(GetUserKeysCmd cmd);

    public Map<String, String> getKeys(Long userId);

    /**
     * Lists user two-factor authentication provider plugins
     * @return list of providers
     */
    List<UserTwoFactorAuthenticator> listUserTwoFactorAuthenticationProviders();

    /**
     * Finds user two factor authenticator provider by domain ID
     * @param domainId domain id
     * @return backup provider
     */
    UserTwoFactorAuthenticator getUserTwoFactorAuthenticationProvider(final Long domainId);

}
