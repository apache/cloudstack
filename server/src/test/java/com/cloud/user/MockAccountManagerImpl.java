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
import java.net.InetAddress;

import javax.naming.ConfigurationException;

import com.cloud.api.auth.SetupUserTwoFactorAuthenticationCmd;
import org.apache.cloudstack.api.command.admin.account.CreateAccountCmd;
import org.apache.cloudstack.api.command.admin.user.GetUserKeysCmd;
import org.apache.cloudstack.api.command.admin.user.MoveUserCmd;
import org.apache.cloudstack.api.response.UserTwoFactorAuthenticationSetupResponse;
import org.apache.cloudstack.auth.UserTwoFactorAuthenticator;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.command.admin.account.UpdateAccountCmd;
import org.apache.cloudstack.api.command.admin.user.DeleteUserCmd;
import org.apache.cloudstack.api.command.admin.user.RegisterCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;

import com.cloud.api.query.vo.ControlledViewEntity;
import com.cloud.dc.DataCenter;
import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class MockAccountManagerImpl extends ManagerBase implements Manager, AccountManager {

    @Override
    public boolean deleteUserAccount(long accountId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public UserAccount disableUser(long userId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserAccount enableUser(long userId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserAccount lockUser(long userId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserAccount updateUser(UpdateUserCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account disableAccount(String accountName, Long domainId, Long accountId) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account enableAccount(String accountName, Long domainId, Long accountId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account lockAccount(String accountName, Long domainId, Long accountId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account updateAccount(UpdateAccountCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account getSystemAccount() {
        return new AccountVO();
    }

    @Override
    public User getSystemUser() {
        return new UserVO();
    }

    @Override
    public boolean deleteUser(DeleteUserCmd deleteUserCmd) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean moveUser(MoveUserCmd moveUserCmd) {
        return false;
    }

    @Override
    public boolean moveUser(long id, Long domainId, Account account) {
        return false;
    }

    @Override
    public UserTwoFactorAuthenticator getUserTwoFactorAuthenticator(Long domainId, Long userAccountId) {
        return null;
    }

    @Override
    public void verifyUsingTwoFactorAuthenticationCode(String code, Long domainId, Long userAccountId) {

    }

    @Override
    public UserTwoFactorAuthenticationSetupResponse setupUserTwoFactorAuthentication(SetupUserTwoFactorAuthenticationCmd cmd) {
        return null;
    }

    @Override
    public boolean isAdmin(Long accountId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Account finalizeOwner(Account caller, String accountName, Long domainId, Long projectId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account getActiveAccountByName(String accountName, Long domainId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserAccount getActiveUserAccount(String username, Long domainId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account getActiveAccountById(long accountId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account getAccount(long accountId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public User getActiveUser(long userId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public User getUserIncludingRemoved(long userId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isRootAdmin(Long accountId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public User getActiveUserByRegistrationToken(String registrationToken) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void markUserRegistered(long userId) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean disableAccount(long accountId) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void checkAccess(Account account, Domain domain) throws PermissionDeniedException {
        // TODO Auto-generated method stub

    }

    @Override
    public void checkAccess(Account account, ServiceOffering so, DataCenter zone) throws PermissionDeniedException {
        // TODO Auto-generated method stub
    }

    @Override
    public void checkAccess(Account account, DiskOffering dof, DataCenter zone) throws PermissionDeniedException {
        // TODO Auto-generated method stub
    }

    @Override
    public void checkAccess(Account account, NetworkOffering nof, DataCenter zone) throws PermissionDeniedException {
        // TODO Auto-generated method stub
    }

    @Override
    public void checkAccess(Account account, VpcOffering vof, DataCenter zone) throws PermissionDeniedException {
        // TODO Auto-generated method stub
    }

    @Override
    public Long checkAccessAndSpecifyAuthority(Account caller, Long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void checkAccess(Account account, AccessType accessType, boolean sameOwner, ControlledEntity... entities) throws PermissionDeniedException {
        // TODO Auto-generated method stub
    }


    @Override
    public UserAccount getUserAccountById(Long userId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void logoutUser(long userId) {
        // TODO Auto-generated method stub
    }

    @Override
    public UserAccount authenticateUser(String username, String password, Long domainId, InetAddress loginIpAddress, Map<String, Object[]> requestParameters) {
        return null;
    }

    @Override
    public Pair<User, Account> findUserByApiKey(String apiKey) {
        return null;
    }

    @Override
    public String[] createApiKeyAndSecretKey(RegisterCmd cmd) {
        return null;
    }

    @Override
    public String[] createApiKeyAndSecretKey(final long userId) {
        return null;
    }

    @Override
    public boolean enableAccount(long accountId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void buildACLSearchBuilder(SearchBuilder<? extends ControlledEntity> sb, Long domainId, boolean isRecursive, List<Long> permittedAccounts,
            ListProjectResourcesCriteria listProjectResourcesCriteria) {
        // TODO Auto-generated method stub

    }

    @Override
    public void buildACLSearchCriteria(SearchCriteria<? extends ControlledEntity> sc, Long domainId, boolean isRecursive, List<Long> permittedAccounts,
            ListProjectResourcesCriteria listProjectResourcesCriteria) {
        // TODO Auto-generated method stub

    }

    @Override
    public void buildACLSearchParameters(Account caller, Long id, String accountName, Long projectId, List<Long> permittedAccounts, Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject, boolean listAll, boolean forProjectInvitation) {
        // TODO Auto-generated method stub
    }

    @Override
    public void buildACLViewSearchBuilder(SearchBuilder<? extends ControlledViewEntity> sb, Long domainId,
            boolean isRecursive, List<Long> permittedAccounts, ListProjectResourcesCriteria listProjectResourcesCriteria) {
        // TODO Auto-generated method stub
    }

    @Override
    public void buildACLViewSearchCriteria(SearchCriteria<? extends ControlledViewEntity> sc, Long domainId,
            boolean isRecursive, List<Long> permittedAccounts, ListProjectResourcesCriteria listProjectResourcesCriteria) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see com.cloud.user.AccountService#getUserByApiKey(java.lang.String)
     */
    @Override
    public UserAccount getUserByApiKey(String apiKey) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserAccount createUserAccount(CreateAccountCmd cmd) {
        return createUserAccount(cmd.getUsername(), cmd.getPassword(), cmd.getFirstName(),
                cmd.getLastName(), cmd.getEmail(), cmd.getTimeZone(), cmd.getAccountName(),
                cmd.getAccountType(), cmd.getRoleId(), cmd.getDomainId(),
                cmd.getNetworkDomain(), cmd.getDetails(), cmd.getAccountUUID(),
                cmd.getUserUUID(), User.Source.UNKNOWN);
    }

    @Override
    public UserAccount createUserAccount(String userName, String password, String firstName, String lastName, String email, String timezone, String accountName,
                                         Account.Type accountType, Long roleId, Long domainId, String networkDomain, Map<String, String> details, String accountUUID,
                                         String userUUID, User.Source source) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public User createUser(String userName, String password, String firstName,
            String lastName, String email, String timeZone, String accountName,
            Long domainId, String userUUID) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override public User createUser(String userName, String password, String firstName, String lastName, String email, String timeZone, String accountName, Long domainId,
                                     String userUUID, User.Source source) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RoleType getRoleType(Account account) {
        return null;
    }

    @Override
    public boolean deleteAccount(AccountVO account, long callerUserId, Account caller) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Account createAccount(String accountName, Account.Type accountType, Long roleId, Long domainId, String networkDomain, Map<String, String> details, String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isDomainAdmin(Long accountId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isNormalUser(long accountId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<String> listAclGroupsByAccount(Long accountId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void checkAccess(Account account, AccessType accessType, boolean sameOwner, String apiName,
            ControlledEntity... entities) throws PermissionDeniedException {
        // TODO Auto-generated method stub
    }

    @Override
    public Long finalyzeAccountId(String accountName, Long domainId, Long projectId, boolean enabledOnly) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getKeys(GetUserKeysCmd cmd) {
        return null;
    }

    @Override
    public Map<String, String> getKeys(Long userId) {
        return null;
    }

    @Override
    public List<UserTwoFactorAuthenticator> listUserTwoFactorAuthenticationProviders() {
        return null;
    }

    @Override
    public UserTwoFactorAuthenticator getUserTwoFactorAuthenticationProvider(Long domainId) {
        return null;
    }

    @Override
    public void checkAccess(User user, ControlledEntity entity)
        throws PermissionDeniedException {

    }
    @Override
    public String getConfigComponentName() {
        return null;
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return null;
    }

}
