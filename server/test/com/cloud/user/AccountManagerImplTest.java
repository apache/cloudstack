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
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cloud.acl.DomainChecker;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.server.auth.UserAuthenticator;
import com.cloud.utils.Pair;

import org.apache.cloudstack.api.command.admin.user.GetUserKeysCmd;
import org.apache.cloudstack.context.CallContext;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account.State;
import com.cloud.vm.UserVmManagerImpl;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;


public class AccountManagerImplTest extends AccountManagetImplTestBase {


    @Mock
    UserVmManagerImpl _vmMgr;

    @Test
    public void disableAccountNotexisting()
            throws ConcurrentOperationException, ResourceUnavailableException {
        Mockito.when(_accountDao.findById(42l)).thenReturn(null);
        Assert.assertTrue(accountManager.disableAccount(42));
    }

    @Test
    public void disableAccountDisabled() throws ConcurrentOperationException,
    ResourceUnavailableException {
        AccountVO disabledAccount = new AccountVO();
        disabledAccount.setState(State.disabled);
        Mockito.when(_accountDao.findById(42l)).thenReturn(disabledAccount);
        Assert.assertTrue(accountManager.disableAccount(42));
    }

    @Test
    public void disableAccount() throws ConcurrentOperationException,
    ResourceUnavailableException {
        AccountVO account = new AccountVO();
        account.setState(State.enabled);
        Mockito.when(_accountDao.findById(42l)).thenReturn(account);
        Mockito.when(_accountDao.createForUpdate()).thenReturn(new AccountVO());
        Mockito.when(
                _accountDao.update(Mockito.eq(42l),
                        Mockito.any(AccountVO.class))).thenReturn(true);
        Mockito.when(_vmDao.listByAccountId(42l)).thenReturn(
                Arrays.asList(Mockito.mock(VMInstanceVO.class)));
        Assert.assertTrue(accountManager.disableAccount(42));
        Mockito.verify(_accountDao, Mockito.atLeastOnce()).update(
                Mockito.eq(42l), Mockito.any(AccountVO.class));
    }

    @Test
    public void deleteUserAccount() {
        AccountVO account = new AccountVO();
        account.setId(42l);
        DomainVO domain = new DomainVO();
        Mockito.when(_accountDao.findById(42l)).thenReturn(account);
        Mockito.when(
                securityChecker.checkAccess(Mockito.any(Account.class),
                        Mockito.any(ControlledEntity.class), Mockito.any(AccessType.class),
                        Mockito.anyString()))
        .thenReturn(true);
        Mockito.when(_accountDao.remove(42l)).thenReturn(true);
        Mockito.when(_configMgr.releaseAccountSpecificVirtualRanges(42l))
        .thenReturn(true);
        Mockito.when(_domainMgr.getDomain(Mockito.anyLong())).thenReturn(domain);
        Mockito.when(
                securityChecker.checkAccess(Mockito.any(Account.class),
                        Mockito.any(Domain.class)))
        .thenReturn(true);
        Mockito.when(_vmSnapshotDao.listByAccountId(Mockito.anyLong())).thenReturn(new ArrayList<VMSnapshotVO>());

        List<SSHKeyPairVO> sshkeyList = new ArrayList<SSHKeyPairVO>();
        SSHKeyPairVO sshkey = new SSHKeyPairVO();
        sshkey.setId(1l);
        sshkeyList.add(sshkey);
        Mockito.when(_sshKeyPairDao.listKeyPairs(Mockito.anyLong(), Mockito.anyLong())).thenReturn(sshkeyList);
        Mockito.when(_sshKeyPairDao.remove(Mockito.anyLong())).thenReturn(true);

        Assert.assertTrue(accountManager.deleteUserAccount(42));
        // assert that this was a clean delete
        Mockito.verify(_accountDao, Mockito.never()).markForCleanup(
                Mockito.eq(42l));
    }

    @Test
    public void deleteUserAccountCleanup() {
        AccountVO account = new AccountVO();
        account.setId(42l);
        DomainVO domain = new DomainVO();
        Mockito.when(_accountDao.findById(42l)).thenReturn(account);
        Mockito.when(
                securityChecker.checkAccess(Mockito.any(Account.class),
                        Mockito.any(ControlledEntity.class), Mockito.any(AccessType.class),
                        Mockito.anyString()))
        .thenReturn(true);
        Mockito.when(_accountDao.remove(42l)).thenReturn(true);
        Mockito.when(_configMgr.releaseAccountSpecificVirtualRanges(42l))
        .thenReturn(true);
        Mockito.when(_userVmDao.listByAccountId(42l)).thenReturn(
                Arrays.asList(Mockito.mock(UserVmVO.class)));
        Mockito.when(
                _vmMgr.expunge(Mockito.any(UserVmVO.class), Mockito.anyLong(),
                        Mockito.any(Account.class))).thenReturn(false);
        Mockito.when(_domainMgr.getDomain(Mockito.anyLong())).thenReturn(domain);
        Mockito.when(
                securityChecker.checkAccess(Mockito.any(Account.class),
                        Mockito.any(Domain.class)))
        .thenReturn(true);

        Assert.assertTrue(accountManager.deleteUserAccount(42));
        // assert that this was NOT a clean delete
        Mockito.verify(_accountDao, Mockito.atLeastOnce()).markForCleanup(
                Mockito.eq(42l));
    }


    @Test
    public void testAuthenticateUser() throws UnknownHostException {
        Pair<Boolean, UserAuthenticator.ActionOnFailedAuthentication> successAuthenticationPair = new Pair<>(true, null);
        Pair<Boolean, UserAuthenticator.ActionOnFailedAuthentication> failureAuthenticationPair = new Pair<>(false,
                UserAuthenticator.ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT);

        UserAccountVO userAccountVO = new UserAccountVO();
        userAccountVO.setSource(User.Source.UNKNOWN);
        userAccountVO.setState(Account.State.disabled.toString());
        Mockito.when(_userAccountDao.getUserAccount("test", 1L)).thenReturn(userAccountVO);
        Mockito.when(userAuthenticator.authenticate("test", "fail", 1L, null)).thenReturn(failureAuthenticationPair);
        Mockito.when(userAuthenticator.authenticate("test", null, 1L, null)).thenReturn(successAuthenticationPair);
        Mockito.when(userAuthenticator.authenticate("test", "", 1L, null)).thenReturn(successAuthenticationPair);

        //Test for incorrect password. authentication should fail
        UserAccount userAccount = accountManager.authenticateUser("test", "fail", 1L, InetAddress.getByName("127.0.0.1"), null);
        Assert.assertNull(userAccount);

        //Test for null password. authentication should fail
        userAccount = accountManager.authenticateUser("test", null, 1L, InetAddress.getByName("127.0.0.1"), null);
        Assert.assertNull(userAccount);

        //Test for empty password. authentication should fail
        userAccount = accountManager.authenticateUser("test", "", 1L, InetAddress.getByName("127.0.0.1"), null);
        Assert.assertNull(userAccount);

        //Verifying that the authentication method is only called when password is specified
        Mockito.verify(userAuthenticator, Mockito.times(1)).authenticate("test", "fail", 1L, null);
        Mockito.verify(userAuthenticator, Mockito.never()).authenticate("test", null, 1L, null);
        Mockito.verify(userAuthenticator, Mockito.never()).authenticate("test", "", 1L, null);
    }

    @Mock
    AccountVO callingAccount;
    @Mock
    DomainChecker domainChecker;
    @Mock
    AccountService accountService;
    @Mock
    private GetUserKeysCmd _listkeyscmd;
    @Mock
    private Account _account;
    @Mock
    private User _user;
    @Mock
    private UserAccountVO userAccountVO;


    @Test (expected = PermissionDeniedException.class)
    public void testgetUserCmd(){
        CallContext.register(callingUser, callingAccount); // Calling account is user account i.e normal account
        Mockito.when(_listkeyscmd.getID()).thenReturn(1L);
        Mockito.when(accountManager.getActiveUser(1L)).thenReturn(_user);
        Mockito.when(accountManager.getUserAccountById(1L)).thenReturn(userAccountVO);
        Mockito.when(userAccountVO.getAccountId()).thenReturn(1L);
        Mockito.when(accountManager.getAccount(Mockito.anyLong())).thenReturn(_account); // Queried account - admin account

        Mockito.when(callingUser.getAccountId()).thenReturn(1L);
        Mockito.when(_accountDao.findById(1L)).thenReturn(callingAccount);

        Mockito.when(accountService.isNormalUser(Mockito.anyLong())).thenReturn(Boolean.TRUE);
        Mockito.when(_account.getAccountId()).thenReturn(2L);

        accountManager.getKeys(_listkeyscmd);

        }
}
