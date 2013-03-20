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

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.command.admin.account.UpdateAccountCmd;
import org.apache.cloudstack.api.command.admin.user.DeleteUserCmd;
import org.apache.cloudstack.api.command.admin.user.RegisterCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;
import org.springframework.stereotype.Component;

import com.cloud.api.query.vo.ControlledViewEntity;
import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;


@Component
@Local(value = { AccountManager.class, AccountService.class })
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
    public boolean isAdmin(short accountType) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Account finalizeOwner(Account caller, String accountName, Long domainId, Long projectId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pair<List<Long>,Long> finalizeAccountDomainForList(Account caller, String accountName, Long domainId, Long projectId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account getActiveAccountByName(String accountName, Long domainId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account getActiveAccountById(Long accountId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account getAccount(Long accountId) {
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
    public boolean isRootAdmin(short accountType) {
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
    public boolean deleteAccount(AccountVO account, long callerUserId, Account caller) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void checkAccess(Account account, Domain domain) throws PermissionDeniedException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean cleanupAccount(AccountVO account, long callerUserId, Account caller) {
        // TODO Auto-generated method stub
        return false;
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
    public void logoutUser(Long userId) {
        // TODO Auto-generated method stub
    }

    @Override
    public UserAccount getUserAccount(String username, Long domainId) {
        return null;
    }

    @Override
    public UserAccount authenticateUser(String username, String password, Long domainId, String loginIpAddress, Map<String, Object[]> requestParameters) {
        return null;
    }

    @Override
    public Pair<User, Account> findUserByApiKey(String apiKey) {
        return null;
    }

    @Override
    public UserVO createUser(long accountId, String userName, String password, String firstName, String lastName, String email, String timezone, String userUUID) {
        return null;
    }

    @Override
    public String[] createApiKeyAndSecretKey(RegisterCmd cmd) {
        return null;
    }

    @Override
    public boolean lockAccount(long accountId) {
        return true;
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
    public UserAccount createUserAccount(String userName, String password,
            String firstName, String lastName, String email, String timezone,
            String accountName, short accountType, Long domainId,
            String networkDomain, Map<String, String> details, String accountUUID, String userUUID) {
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

    @Override
    public Account createAccount(String accountName, short accountType,
            Long domainId, String networkDomain, Map details, String uuid) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public RoleType getRoleType(Account account) {
        return null;
    }

}
