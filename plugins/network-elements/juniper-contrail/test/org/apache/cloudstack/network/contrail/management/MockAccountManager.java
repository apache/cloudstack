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

package org.apache.cloudstack.network.contrail.management;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.command.admin.account.UpdateAccountCmd;
import org.apache.cloudstack.api.command.admin.user.DeleteUserCmd;
import org.apache.cloudstack.api.command.admin.user.RegisterCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;
import org.apache.cloudstack.context.CallContext;

import com.cloud.api.query.dao.AccountJoinDao;
import com.cloud.api.query.vo.ControlledViewEntity;
import com.cloud.configuration.ResourceLimit;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;

public class MockAccountManager extends ManagerBase implements AccountManager {
    private static final Logger s_logger = Logger.getLogger(MockAccountManager.class);

    @Inject
    AccountDao _accountDao;
    @Inject
    ResourceCountDao _resourceCountDao;

    @Inject
    AccountJoinDao _accountJoinDao;
    @Inject
    UserDao _userDao;

    UserVO _systemUser;
    AccountVO _systemAccount;

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _systemAccount = _accountDao.findById(Account.ACCOUNT_ID_SYSTEM);
        if (_systemAccount == null) {
            throw new ConfigurationException("Unable to find the system account using " + Account.ACCOUNT_ID_SYSTEM);
        }

        _systemUser = _userDao.findById(User.UID_SYSTEM);
        if (_systemUser == null) {
            throw new ConfigurationException("Unable to find the system user using " + User.UID_SYSTEM);
        }
        CallContext.register(_systemUser, _systemAccount);
        s_logger.info("MockAccountManager initialization successful");
        return true;
    }

    @Override
    public void checkAccess(Account arg0, Domain arg1) throws PermissionDeniedException {
        // TODO Auto-generated method stub

    }

    @Override
    public void checkAccess(Account arg0, AccessType arg1, boolean arg2, ControlledEntity... arg3) throws PermissionDeniedException {
        // TODO Auto-generated method stub

    }

    @Override
    public String[] createApiKeyAndSecretKey(RegisterCmd arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public User createUser(String arg0, String arg1, String arg2, String arg3, String arg4, String arg5, String arg6, Long arg7, String arg8) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserAccount createUserAccount(String arg0, String arg1, String arg2, String arg3, String arg4, String arg5, String arg6, short arg7, Long arg8, String arg9,
        Map<String, String> arg10, String arg11, String arg12) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account finalizeOwner(Account arg0, String arg1, Long arg2, Long arg3) {
        return _systemAccount;
    }

    @Override
    public Account getActiveAccountByName(String arg0, Long arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public User getActiveUser(long arg0) {
        return _systemUser;
    }

    @Override
    public User getActiveUserByRegistrationToken(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RoleType getRoleType(Account arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account getSystemAccount() {
        return _systemAccount;
    }

    @Override
    public User getSystemUser() {
        return _systemUser;
    }

    @Override
    public UserAccount getUserByApiKey(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public User getUserIncludingRemoved(long arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isAdmin(short arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isRootAdmin(short arg0) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public UserAccount lockUser(long arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void markUserRegistered(long arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public UserAccount authenticateUser(String arg0, String arg1, Long arg2, String arg3, Map<String, Object[]> arg4) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void buildACLSearchBuilder(SearchBuilder<? extends ControlledEntity> arg0, Long arg1, boolean arg2, List<Long> arg3, ListProjectResourcesCriteria arg4) {
        // TODO Auto-generated method stub

    }

    @Override
    public void buildACLSearchCriteria(SearchCriteria<? extends ControlledEntity> arg0, Long arg1, boolean arg2, List<Long> arg3, ListProjectResourcesCriteria arg4) {
        // TODO Auto-generated method stub

    }

    @Override
    public void buildACLSearchParameters(Account arg0, Long arg1, String arg2, Long arg3, List<Long> arg4, Ternary<Long, Boolean, ListProjectResourcesCriteria> arg5,
        boolean arg6, boolean arg7) {
        // TODO Auto-generated method stub

    }

    @Override
    public void
        buildACLViewSearchBuilder(SearchBuilder<? extends ControlledViewEntity> arg0, Long arg1, boolean arg2, List<Long> arg3, ListProjectResourcesCriteria arg4) {
        // TODO Auto-generated method stub

    }

    @Override
    public void buildACLViewSearchCriteria(SearchCriteria<? extends ControlledViewEntity> arg0, Long arg1, boolean arg2, List<Long> arg3,
        ListProjectResourcesCriteria arg4) {
        // TODO Auto-generated method stub

    }

    @Override
    public Long checkAccessAndSpecifyAuthority(Account arg0, Long arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deleteAccount(AccountVO arg0, long arg1, Account arg2) {
        return true;
    }

    @Override
    public boolean deleteUser(DeleteUserCmd arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteUserAccount(long arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean disableAccount(long arg0) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Account disableAccount(String arg0, Long arg1, Long arg2) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserAccount disableUser(long arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean enableAccount(long arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Account enableAccount(String arg0, Long arg1, Long arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserAccount enableUser(long arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pair<User, Account> findUserByApiKey(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account lockAccount(String arg0, Long arg1, Long arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account updateAccount(UpdateAccountCmd arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserAccount updateUser(UpdateUserCmd arg0) {
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
        return _systemAccount;
    }

    @Override
    public Account createAccount(String accountName, short accountType, Long domainId, String networkDomain, Map<String, String> details, String uuid) {
        final AccountVO account = new AccountVO(accountName, domainId, networkDomain, accountType, uuid);
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {

                _accountDao.persist(account);
                _resourceCountDao.createResourceCounts(account.getId(), ResourceLimit.ResourceOwnerType.Account);
            }
        });
        return account;
    }

    @Override
    public void logoutUser(long userId) {
        // TODO Auto-generated method stub

    }

}
