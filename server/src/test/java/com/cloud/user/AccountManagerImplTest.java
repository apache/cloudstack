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

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.command.admin.user.GetUserKeysCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;
import org.apache.cloudstack.context.CallContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.acl.DomainChecker;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectAccountVO;
import com.cloud.server.auth.UserAuthenticator;
import com.cloud.server.auth.UserAuthenticator.ActionOnFailedAuthentication;
import com.cloud.user.Account.State;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManagerImpl;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.snapshot.VMSnapshotVO;

@RunWith(MockitoJUnitRunner.class)
public class AccountManagerImplTest extends AccountManagetImplTestBase {

    @Mock
    private UserVmManagerImpl _vmMgr;
    @Mock
    private AccountVO callingAccount;
    @Mock
    private DomainChecker domainChecker;
    @Mock
    private AccountService accountService;
    @Mock
    private GetUserKeysCmd _listkeyscmd;
    @Mock
    private User _user;
    @Mock
    private UserAccountVO userAccountVO;

    @Mock
    private UpdateUserCmd UpdateUserCmdMock;

    private long userVoIdMock = 111l;
    @Mock
    private UserVO userVoMock;

    private long accountMockId = 100l;

    @Mock
    private Account accountMock;

    @Mock
    private ProjectAccountVO projectAccountVO;
    @Mock
    private Project project;


    @Before
    public void beforeTest() {
        Mockito.doReturn(accountMockId).when(accountMock).getId();
        Mockito.doReturn(accountMock).when(accountManagerImpl).getCurrentCallingAccount();

        Mockito.doReturn(accountMockId).when(userVoMock).getAccountId();

        Mockito.doReturn(userVoIdMock).when(userVoMock).getId();
    }

    @Test
    public void disableAccountNotexisting() throws ConcurrentOperationException, ResourceUnavailableException {
        Mockito.when(_accountDao.findById(42l)).thenReturn(null);
        Assert.assertTrue(accountManagerImpl.disableAccount(42));
    }

    @Test
    public void disableAccountDisabled() throws ConcurrentOperationException, ResourceUnavailableException {
        AccountVO disabledAccount = new AccountVO();
        disabledAccount.setState(State.disabled);
        Mockito.when(_accountDao.findById(42l)).thenReturn(disabledAccount);
        Assert.assertTrue(accountManagerImpl.disableAccount(42));
    }

    @Test
    public void disableAccount() throws ConcurrentOperationException, ResourceUnavailableException {
        AccountVO account = new AccountVO();
        account.setState(State.enabled);
        Mockito.when(_accountDao.findById(42l)).thenReturn(account);
        Mockito.when(_accountDao.createForUpdate()).thenReturn(new AccountVO());
        Mockito.when(_accountDao.update(Mockito.eq(42l), Mockito.any(AccountVO.class))).thenReturn(true);
        Mockito.when(_vmDao.listByAccountId(42l)).thenReturn(Arrays.asList(Mockito.mock(VMInstanceVO.class)));
        Assert.assertTrue(accountManagerImpl.disableAccount(42));
        Mockito.verify(_accountDao, Mockito.atLeastOnce()).update(Mockito.eq(42l), Mockito.any(AccountVO.class));
    }

    @Test
    public void deleteUserAccount() {
        AccountVO account = new AccountVO();
        account.setId(42l);
        DomainVO domain = new DomainVO();
        Mockito.when(_accountDao.findById(42l)).thenReturn(account);
        Mockito.doNothing().when(accountManagerImpl).checkAccess(Mockito.any(Account.class), Mockito.isNull(), Mockito.anyBoolean(), Mockito.any(Account.class));
        Mockito.when(_accountDao.remove(42l)).thenReturn(true);
        Mockito.when(_configMgr.releaseAccountSpecificVirtualRanges(42l)).thenReturn(true);
        Mockito.lenient().when(_domainMgr.getDomain(Mockito.anyLong())).thenReturn(domain);
        Mockito.lenient().when(securityChecker.checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class))).thenReturn(true);
        Mockito.when(_vmSnapshotDao.listByAccountId(Mockito.anyLong())).thenReturn(new ArrayList<VMSnapshotVO>());

        List<SSHKeyPairVO> sshkeyList = new ArrayList<SSHKeyPairVO>();
        SSHKeyPairVO sshkey = new SSHKeyPairVO();
        sshkey.setId(1l);
        sshkeyList.add(sshkey);
        Mockito.when(_sshKeyPairDao.listKeyPairs(Mockito.anyLong(), Mockito.anyLong())).thenReturn(sshkeyList);
        Mockito.when(_sshKeyPairDao.remove(Mockito.anyLong())).thenReturn(true);

        Assert.assertTrue(accountManagerImpl.deleteUserAccount(42l));
        // assert that this was a clean delete
        Mockito.verify(_accountDao, Mockito.never()).markForCleanup(Mockito.eq(42l));
    }

    @Test
    public void deleteUserAccountCleanup() {
        AccountVO account = new AccountVO();
        account.setId(42l);
        DomainVO domain = new DomainVO();
        Mockito.when(_accountDao.findById(42l)).thenReturn(account);
        Mockito.doNothing().when(accountManagerImpl).checkAccess(Mockito.any(Account.class), Mockito.isNull(), Mockito.anyBoolean(), Mockito.any(Account.class));
        Mockito.when(_accountDao.remove(42l)).thenReturn(true);
        Mockito.when(_configMgr.releaseAccountSpecificVirtualRanges(42l)).thenReturn(true);
        Mockito.when(_userVmDao.listByAccountId(42l)).thenReturn(Arrays.asList(Mockito.mock(UserVmVO.class)));
        Mockito.when(_vmMgr.expunge(Mockito.any(UserVmVO.class), Mockito.anyLong(), Mockito.any(Account.class))).thenReturn(false);
        Mockito.lenient().when(_domainMgr.getDomain(Mockito.anyLong())).thenReturn(domain);
        Mockito.lenient().when(securityChecker.checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class))).thenReturn(true);

        Assert.assertTrue(accountManagerImpl.deleteUserAccount(42l));
        // assert that this was NOT a clean delete
        Mockito.verify(_accountDao, Mockito.atLeastOnce()).markForCleanup(Mockito.eq(42l));
    }

    @Test
    public void testAuthenticateUser() throws UnknownHostException {
        Pair<Boolean, UserAuthenticator.ActionOnFailedAuthentication> successAuthenticationPair = new Pair<>(true, null);
        Pair<Boolean, UserAuthenticator.ActionOnFailedAuthentication> failureAuthenticationPair = new Pair<>(false,
                UserAuthenticator.ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT);

        UserAccountVO userAccountVO = new UserAccountVO();
        userAccountVO.setSource(User.Source.UNKNOWN);
        userAccountVO.setState(Account.State.disabled.toString());
        Mockito.when(userAccountDaoMock.getUserAccount("test", 1L)).thenReturn(userAccountVO);
        Mockito.when(userAuthenticator.authenticate("test", "fail", 1L, null)).thenReturn(failureAuthenticationPair);
        Mockito.lenient().when(userAuthenticator.authenticate("test", null, 1L, null)).thenReturn(successAuthenticationPair);
        Mockito.lenient().when(userAuthenticator.authenticate("test", "", 1L, null)).thenReturn(successAuthenticationPair);

        //Test for incorrect password. authentication should fail
        UserAccount userAccount = accountManagerImpl.authenticateUser("test", "fail", 1L, InetAddress.getByName("127.0.0.1"), null);
        Assert.assertNull(userAccount);

        //Test for null password. authentication should fail
        userAccount = accountManagerImpl.authenticateUser("test", null, 1L, InetAddress.getByName("127.0.0.1"), null);
        Assert.assertNull(userAccount);

        //Test for empty password. authentication should fail
        userAccount = accountManagerImpl.authenticateUser("test", "", 1L, InetAddress.getByName("127.0.0.1"), null);
        Assert.assertNull(userAccount);

        //Verifying that the authentication method is only called when password is specified
        Mockito.verify(userAuthenticator, Mockito.times(1)).authenticate("test", "fail", 1L, null);
        Mockito.verify(userAuthenticator, Mockito.never()).authenticate("test", null, 1L, null);
        Mockito.verify(userAuthenticator, Mockito.never()).authenticate("test", "", 1L, null);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testgetUserCmd() {
        CallContext.register(callingUser, callingAccount); // Calling account is user account i.e normal account
        Mockito.when(_listkeyscmd.getID()).thenReturn(1L);
        Mockito.when(accountManagerImpl.getActiveUser(1L)).thenReturn(userVoMock);
        Mockito.when(accountManagerImpl.getUserAccountById(1L)).thenReturn(userAccountVO);
        Mockito.when(userAccountVO.getAccountId()).thenReturn(1L);
        Mockito.lenient().when(accountManagerImpl.getAccount(Mockito.anyLong())).thenReturn(accountMock); // Queried account - admin account

        Mockito.lenient().when(callingUser.getAccountId()).thenReturn(1L);
        Mockito.lenient().when(_accountDao.findById(1L)).thenReturn(callingAccount);

        Mockito.lenient().when(accountService.isNormalUser(Mockito.anyLong())).thenReturn(Boolean.TRUE);
        Mockito.lenient().when(accountMock.getAccountId()).thenReturn(2L);

        accountManagerImpl.getKeys(_listkeyscmd);
    }

    @Test
    public void updateUserTestTimeZoneAndEmailNull() {
        prepareMockAndExecuteUpdateUserTest(0);
    }

    @Test
    public void updateUserTestTimeZoneAndEmailNotNull() {
        Mockito.when(UpdateUserCmdMock.getEmail()).thenReturn("email");
        Mockito.when(UpdateUserCmdMock.getTimezone()).thenReturn("timezone");
        prepareMockAndExecuteUpdateUserTest(1);
    }

    private void prepareMockAndExecuteUpdateUserTest(int numberOfExpectedCallsForSetEmailAndSetTimeZone) {
        Mockito.doReturn("password").when(UpdateUserCmdMock).getPassword();
        Mockito.doReturn("newpassword").when(UpdateUserCmdMock).getCurrentPassword();
        Mockito.doReturn(userVoMock).when(accountManagerImpl).retrieveAndValidateUser(UpdateUserCmdMock);
        Mockito.doNothing().when(accountManagerImpl).validateAndUpdateApiAndSecretKeyIfNeeded(UpdateUserCmdMock, userVoMock);
        Mockito.doReturn(accountMock).when(accountManagerImpl).retrieveAndValidateAccount(userVoMock);

        Mockito.doNothing().when(accountManagerImpl).validateAndUpdateFirstNameIfNeeded(UpdateUserCmdMock, userVoMock);
        Mockito.doNothing().when(accountManagerImpl).validateAndUpdateLastNameIfNeeded(UpdateUserCmdMock, userVoMock);
        Mockito.doNothing().when(accountManagerImpl).validateAndUpdateUsernameIfNeeded(UpdateUserCmdMock, userVoMock, accountMock);
        Mockito.doNothing().when(accountManagerImpl).validateUserPasswordAndUpdateIfNeeded(Mockito.anyString(), Mockito.eq(userVoMock), Mockito.anyString());

        Mockito.doReturn(true).when(userDaoMock).update(Mockito.anyLong(), Mockito.eq(userVoMock));
        Mockito.doReturn(Mockito.mock(UserAccountVO.class)).when(userAccountDaoMock).findById(Mockito.anyLong());

        accountManagerImpl.updateUser(UpdateUserCmdMock);

        InOrder inOrder = Mockito.inOrder(userVoMock, accountManagerImpl, userDaoMock, userAccountDaoMock);

        inOrder.verify(accountManagerImpl).retrieveAndValidateUser(UpdateUserCmdMock);
        inOrder.verify(accountManagerImpl).validateAndUpdateApiAndSecretKeyIfNeeded(UpdateUserCmdMock, userVoMock);
        inOrder.verify(accountManagerImpl).retrieveAndValidateAccount(userVoMock);

        inOrder.verify(accountManagerImpl).validateAndUpdateFirstNameIfNeeded(UpdateUserCmdMock, userVoMock);
        inOrder.verify(accountManagerImpl).validateAndUpdateLastNameIfNeeded(UpdateUserCmdMock, userVoMock);
        inOrder.verify(accountManagerImpl).validateAndUpdateUsernameIfNeeded(UpdateUserCmdMock, userVoMock, accountMock);
        inOrder.verify(accountManagerImpl).validateUserPasswordAndUpdateIfNeeded(UpdateUserCmdMock.getPassword(), userVoMock, UpdateUserCmdMock.getCurrentPassword());

        inOrder.verify(userVoMock, Mockito.times(numberOfExpectedCallsForSetEmailAndSetTimeZone)).setEmail(Mockito.anyString());
        inOrder.verify(userVoMock, Mockito.times(numberOfExpectedCallsForSetEmailAndSetTimeZone)).setTimezone(Mockito.anyString());

        inOrder.verify(userDaoMock).update(Mockito.anyLong(), Mockito.eq(userVoMock));
        inOrder.verify(userAccountDaoMock).findById(Mockito.anyLong());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void retrieveAndValidateUserTestNoUserFound() {
        Mockito.doReturn(null).when(userDaoMock).getUser(Mockito.anyLong());

        accountManagerImpl.retrieveAndValidateUser(UpdateUserCmdMock);
    }

    @Test
    public void retrieveAndValidateUserTestUserIsFound() {
        Mockito.doReturn(userVoMock).when(userDaoMock).getUser(Mockito.anyLong());

        UserVO receivedUser = accountManagerImpl.retrieveAndValidateUser(UpdateUserCmdMock);

        Assert.assertEquals(userVoMock, receivedUser);
    }

    @Test
    public void validateAndUpdatApiAndSecretKeyIfNeededTestNoKeys() {
        accountManagerImpl.validateAndUpdateApiAndSecretKeyIfNeeded(UpdateUserCmdMock, userVoMock);

        Mockito.verify(_accountDao, Mockito.times(0)).findUserAccountByApiKey(Mockito.anyString());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateAndUpdatApiAndSecretKeyIfNeededTestOnlyApiKeyInformed() {
        Mockito.doReturn("apiKey").when(UpdateUserCmdMock).getApiKey();

        accountManagerImpl.validateAndUpdateApiAndSecretKeyIfNeeded(UpdateUserCmdMock, userVoMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateAndUpdatApiAndSecretKeyIfNeededTestOnlySecretKeyInformed() {
        Mockito.doReturn("secretKey").when(UpdateUserCmdMock).getSecretKey();

        accountManagerImpl.validateAndUpdateApiAndSecretKeyIfNeeded(UpdateUserCmdMock, userVoMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateAndUpdatApiAndSecretKeyIfNeededTestApiKeyAlreadyUsedBySomeoneElse() {
        String apiKey = "apiKey";
        Mockito.doReturn(apiKey).when(UpdateUserCmdMock).getApiKey();
        Mockito.doReturn("secretKey").when(UpdateUserCmdMock).getSecretKey();

        Mockito.doReturn(1L).when(userVoMock).getId();

        User otherUserMock = Mockito.mock(User.class);
        Mockito.doReturn(2L).when(otherUserMock).getId();

        Pair<User, Account> pairUserAccountMock = new Pair<User, Account>(otherUserMock, Mockito.mock(Account.class));
        Mockito.doReturn(pairUserAccountMock).when(_accountDao).findUserAccountByApiKey(apiKey);

        accountManagerImpl.validateAndUpdateApiAndSecretKeyIfNeeded(UpdateUserCmdMock, userVoMock);
    }

    @Test
    public void validateAndUpdatApiAndSecretKeyIfNeededTest() {
        String apiKey = "apiKey";
        Mockito.doReturn(apiKey).when(UpdateUserCmdMock).getApiKey();

        String secretKey = "secretKey";
        Mockito.doReturn(secretKey).when(UpdateUserCmdMock).getSecretKey();

        Mockito.doReturn(1L).when(userVoMock).getId();

        User otherUserMock = Mockito.mock(User.class);
        Mockito.doReturn(1L).when(otherUserMock).getId();

        Pair<User, Account> pairUserAccountMock = new Pair<User, Account>(otherUserMock, Mockito.mock(Account.class));
        Mockito.doReturn(pairUserAccountMock).when(_accountDao).findUserAccountByApiKey(apiKey);

        accountManagerImpl.validateAndUpdateApiAndSecretKeyIfNeeded(UpdateUserCmdMock, userVoMock);

        Mockito.verify(_accountDao).findUserAccountByApiKey(apiKey);
        Mockito.verify(userVoMock).setApiKey(apiKey);
        Mockito.verify(userVoMock).setSecretKey(secretKey);
    }

    @Test(expected = CloudRuntimeException.class)
    public void retrieveAndValidateAccountTestAccountNotFound() {
        Mockito.doReturn(accountMockId).when(userVoMock).getAccountId();

        Mockito.doReturn(null).when(_accountDao).findById(accountMockId);

        accountManagerImpl.retrieveAndValidateAccount(userVoMock);
    }

    @Test
    public void retrieveAndValidateAccountTestAccountTypeEqualsProjectType() {
        Mockito.doReturn(accountMockId).when(userVoMock).getAccountId();
        Mockito.lenient().doReturn(Account.ACCOUNT_TYPE_PROJECT).when(accountMock).getType();
        Mockito.doReturn(callingAccount).when(_accountDao).findById(accountMockId);
        Mockito.doNothing().when(accountManagerImpl).checkAccess(Mockito.any(Account.class), Mockito.any(AccessType.class), Mockito.anyBoolean(), Mockito.any(Account.class));

        accountManagerImpl.retrieveAndValidateAccount(userVoMock);
    }

    @Test
    public void retrieveAndValidateAccountTestAccountTypeEqualsSystemType() {
        Mockito.doReturn(Account.ACCOUNT_ID_SYSTEM).when(userVoMock).getAccountId();
        Mockito.doReturn(Account.ACCOUNT_ID_SYSTEM).when(accountMock).getId();
        Mockito.doReturn(callingAccount).when(_accountDao).findById(Account.ACCOUNT_ID_SYSTEM);
        accountManagerImpl.retrieveAndValidateAccount(userVoMock);
    }

    @Test
    public void retrieveAndValidateAccountTest() {
        Mockito.doReturn(accountMockId).when(userVoMock).getAccountId();
        Mockito.doReturn(callingAccount).when(_accountDao).findById(accountMockId);

        Mockito.doNothing().when(accountManagerImpl).checkAccess(Mockito.eq(accountMock), Mockito.eq(AccessType.OperateEntry), Mockito.anyBoolean(), Mockito.any(Account.class));
        accountManagerImpl.retrieveAndValidateAccount(userVoMock);

        Mockito.verify(accountManagerImpl).getCurrentCallingAccount();
        Mockito.verify(accountManagerImpl).checkAccess(Mockito.eq(accountMock), Mockito.eq(AccessType.OperateEntry), Mockito.anyBoolean(), Mockito.any(Account.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateAndUpdateFirstNameIfNeededTestFirstNameBlank() {
        Mockito.doReturn("   ").when(UpdateUserCmdMock).getFirstname();

        accountManagerImpl.validateAndUpdateFirstNameIfNeeded(UpdateUserCmdMock, userVoMock);
    }

    @Test
    public void validateAndUpdateFirstNameIfNeededTestFirstNameNull() {
        Mockito.doReturn(null).when(UpdateUserCmdMock).getFirstname();

        accountManagerImpl.validateAndUpdateFirstNameIfNeeded(UpdateUserCmdMock, userVoMock);

        Mockito.verify(userVoMock, Mockito.times(0)).setFirstname(Mockito.anyString());
    }

    @Test
    public void validateAndUpdateFirstNameIfNeededTest() {
        String firstname = "firstName";
        Mockito.doReturn(firstname).when(UpdateUserCmdMock).getFirstname();

        accountManagerImpl.validateAndUpdateFirstNameIfNeeded(UpdateUserCmdMock, userVoMock);

        Mockito.verify(userVoMock).setFirstname(firstname);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateAndUpdateLastNameIfNeededTestLastNameBlank() {
        Mockito.doReturn("   ").when(UpdateUserCmdMock).getLastname();

        accountManagerImpl.validateAndUpdateLastNameIfNeeded(UpdateUserCmdMock, userVoMock);
    }

    @Test
    public void validateAndUpdateLastNameIfNeededTestLastNameNull() {
        Mockito.doReturn(null).when(UpdateUserCmdMock).getLastname();

        accountManagerImpl.validateAndUpdateLastNameIfNeeded(UpdateUserCmdMock, userVoMock);

        Mockito.verify(userVoMock, Mockito.times(0)).setLastname(Mockito.anyString());
    }

    @Test
    public void validateAndUpdateLastNameIfNeededTest() {
        String lastName = "lastName";
        Mockito.doReturn(lastName).when(UpdateUserCmdMock).getLastname();

        accountManagerImpl.validateAndUpdateLastNameIfNeeded(UpdateUserCmdMock, userVoMock);

        Mockito.verify(userVoMock).setLastname(lastName);
    }

    @Test
    public void validateAndUpdateUsernameIfNeededTestNullUsername() {
        Mockito.doReturn(null).when(UpdateUserCmdMock).getUsername();

        accountManagerImpl.validateAndUpdateUsernameIfNeeded(UpdateUserCmdMock, userVoMock, accountMock);

        Mockito.verify(userVoMock, Mockito.times(0)).setUsername(Mockito.anyString());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateAndUpdateUsernameIfNeededTestBlankUsername() {
        Mockito.doReturn("   ").when(UpdateUserCmdMock).getUsername();

        accountManagerImpl.validateAndUpdateUsernameIfNeeded(UpdateUserCmdMock, userVoMock, accountMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateAndUpdateUsernameIfNeededTestDuplicatedUserSameDomainThisUser() {
        long domanIdCurrentUser = 22l;

        String userName = "username";
        Mockito.doReturn(userName).when(UpdateUserCmdMock).getUsername();
        Mockito.lenient().doReturn(userName).when(userVoMock).getUsername();
        Mockito.doReturn(domanIdCurrentUser).when(accountMock).getDomainId();

        long userVoDuplicatedMockId = 67l;
        UserVO userVoDuplicatedMock = Mockito.mock(UserVO.class);
        Mockito.doReturn(userName).when(userVoDuplicatedMock).getUsername();
        Mockito.doReturn(userVoDuplicatedMockId).when(userVoDuplicatedMock).getId();

        long accountIdUserDuplicated = 98l;

        Mockito.doReturn(accountIdUserDuplicated).when(userVoDuplicatedMock).getAccountId();

        Account accountUserDuplicatedMock = Mockito.mock(AccountVO.class);
        Mockito.lenient().doReturn(accountIdUserDuplicated).when(accountUserDuplicatedMock).getId();
        Mockito.doReturn(domanIdCurrentUser).when(accountUserDuplicatedMock).getDomainId();

        List<UserVO> usersWithSameUserName = new ArrayList<>();
        usersWithSameUserName.add(userVoMock);
        usersWithSameUserName.add(userVoDuplicatedMock);

        Mockito.doReturn(usersWithSameUserName).when(userDaoMock).findUsersByName(userName);

        Mockito.lenient().doReturn(accountMock).when(_accountDao).findById(accountMockId);
        Mockito.doReturn(accountUserDuplicatedMock).when(_accountDao).findById(accountIdUserDuplicated);

        Mockito.doReturn(Mockito.mock(DomainVO.class)).when(_domainDao).findById(Mockito.anyLong());

        accountManagerImpl.validateAndUpdateUsernameIfNeeded(UpdateUserCmdMock, userVoMock, accountMock);
    }

    @Test
    public void validateAndUpdateUsernameIfNeededTestDuplicatedUserButInDifferentDomains() {
        long domanIdCurrentUser = 22l;

        String userName = "username";
        Mockito.doReturn(userName).when(UpdateUserCmdMock).getUsername();
        Mockito.lenient().doReturn(userName).when(userVoMock).getUsername();
        Mockito.doReturn(domanIdCurrentUser).when(accountMock).getDomainId();

        long userVoDuplicatedMockId = 67l;
        UserVO userVoDuplicatedMock = Mockito.mock(UserVO.class);
        Mockito.lenient().doReturn(userName).when(userVoDuplicatedMock).getUsername();
        Mockito.doReturn(userVoDuplicatedMockId).when(userVoDuplicatedMock).getId();

        long accountIdUserDuplicated = 98l;
        Mockito.doReturn(accountIdUserDuplicated).when(userVoDuplicatedMock).getAccountId();

        Account accountUserDuplicatedMock = Mockito.mock(AccountVO.class);
        Mockito.lenient().doReturn(accountIdUserDuplicated).when(accountUserDuplicatedMock).getId();
        Mockito.doReturn(45l).when(accountUserDuplicatedMock).getDomainId();

        List<UserVO> usersWithSameUserName = new ArrayList<>();
        usersWithSameUserName.add(userVoMock);
        usersWithSameUserName.add(userVoDuplicatedMock);

        Mockito.doReturn(usersWithSameUserName).when(userDaoMock).findUsersByName(userName);

        Mockito.lenient().doReturn(accountMock).when(_accountDao).findById(accountMockId);
        Mockito.doReturn(accountUserDuplicatedMock).when(_accountDao).findById(accountIdUserDuplicated);

        accountManagerImpl.validateAndUpdateUsernameIfNeeded(UpdateUserCmdMock, userVoMock, accountMock);

        Mockito.verify(userVoMock).setUsername(userName);
    }

    @Test
    public void validateAndUpdateUsernameIfNeededTestNoDuplicatedUserNames() {
        long domanIdCurrentUser = 22l;

        String userName = "username";
        Mockito.doReturn(userName).when(UpdateUserCmdMock).getUsername();
        Mockito.lenient().doReturn(userName).when(userVoMock).getUsername();
        Mockito.lenient().doReturn(domanIdCurrentUser).when(accountMock).getDomainId();

        List<UserVO> usersWithSameUserName = new ArrayList<>();

        Mockito.doReturn(usersWithSameUserName).when(userDaoMock).findUsersByName(userName);

        Mockito.lenient().doReturn(accountMock).when(_accountDao).findById(accountMockId);

        accountManagerImpl.validateAndUpdateUsernameIfNeeded(UpdateUserCmdMock, userVoMock, accountMock);

        Mockito.verify(userVoMock).setUsername(userName);
    }

    @Test
    public void valiateUserPasswordAndUpdateIfNeededTestPasswordNull() {
        accountManagerImpl.validateUserPasswordAndUpdateIfNeeded(null, userVoMock, null);

        Mockito.verify(userVoMock, Mockito.times(0)).setPassword(Mockito.anyString());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void valiateUserPasswordAndUpdateIfNeededTestBlankPassword() {
        accountManagerImpl.validateUserPasswordAndUpdateIfNeeded("       ", userVoMock, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void valiateUserPasswordAndUpdateIfNeededTestNoAdminAndNoCurrentPasswordProvided() {
        Mockito.doReturn(accountMock).when(accountManagerImpl).getCurrentCallingAccount();
        Mockito.doReturn(false).when(accountManagerImpl).isRootAdmin(accountMockId);
        Mockito.doReturn(false).when(accountManagerImpl).isDomainAdmin(accountMockId);
        Mockito.lenient().doReturn(true).when(accountManagerImpl).isResourceDomainAdmin(accountMockId);

        accountManagerImpl.validateUserPasswordAndUpdateIfNeeded("newPassword", userVoMock, "  ");
    }

    @Test(expected = CloudRuntimeException.class)
    public void valiateUserPasswordAndUpdateIfNeededTestNoUserAuthenticatorsConfigured() {
        Mockito.doReturn(accountMock).when(accountManagerImpl).getCurrentCallingAccount();
        Mockito.doReturn(true).when(accountManagerImpl).isRootAdmin(accountMockId);
        Mockito.doReturn(false).when(accountManagerImpl).isDomainAdmin(accountMockId);

        Mockito.lenient().doNothing().when(accountManagerImpl).validateCurrentPassword(Mockito.eq(userVoMock), Mockito.anyString());

        accountManagerImpl.validateUserPasswordAndUpdateIfNeeded("newPassword", userVoMock, null);
    }

    @Test
    public void validateUserPasswordAndUpdateIfNeededTestRootAdminUpdatingUserPassword() {
        Mockito.doReturn(accountMock).when(accountManagerImpl).getCurrentCallingAccount();
        Mockito.doReturn(true).when(accountManagerImpl).isRootAdmin(accountMockId);
        Mockito.doReturn(false).when(accountManagerImpl).isDomainAdmin(accountMockId);

        String newPassword = "newPassword";

        String expectedUserPasswordAfterEncoded = configureUserMockAuthenticators(newPassword);

        Mockito.lenient().doNothing().when(accountManagerImpl).validateCurrentPassword(Mockito.eq(userVoMock), Mockito.anyString());

        accountManagerImpl.validateUserPasswordAndUpdateIfNeeded(newPassword, userVoMock, null);

        Mockito.verify(accountManagerImpl, Mockito.times(0)).validateCurrentPassword(Mockito.eq(userVoMock), Mockito.anyString());
        Mockito.verify(userVoMock, Mockito.times(1)).setPassword(expectedUserPasswordAfterEncoded);
    }

    @Test
    public void validateUserPasswordAndUpdateIfNeededTestDomainAdminUpdatingUserPassword() {
        Mockito.doReturn(accountMock).when(accountManagerImpl).getCurrentCallingAccount();
        Mockito.doReturn(false).when(accountManagerImpl).isRootAdmin(accountMockId);
        Mockito.doReturn(true).when(accountManagerImpl).isDomainAdmin(accountMockId);

        String newPassword = "newPassword";

        String expectedUserPasswordAfterEncoded = configureUserMockAuthenticators(newPassword);

        Mockito.lenient().doNothing().when(accountManagerImpl).validateCurrentPassword(Mockito.eq(userVoMock), Mockito.anyString());

        accountManagerImpl.validateUserPasswordAndUpdateIfNeeded(newPassword, userVoMock, null);

        Mockito.verify(accountManagerImpl, Mockito.times(0)).validateCurrentPassword(Mockito.eq(userVoMock), Mockito.anyString());
        Mockito.verify(userVoMock, Mockito.times(1)).setPassword(expectedUserPasswordAfterEncoded);
    }

    @Test
    public void validateUserPasswordAndUpdateIfNeededTestUserUpdatingHisPassword() {
        Mockito.doReturn(accountMock).when(accountManagerImpl).getCurrentCallingAccount();
        Mockito.doReturn(false).when(accountManagerImpl).isRootAdmin(accountMockId);
        Mockito.doReturn(false).when(accountManagerImpl).isDomainAdmin(accountMockId);

        String newPassword = "newPassword";
        String expectedUserPasswordAfterEncoded = configureUserMockAuthenticators(newPassword);

        Mockito.doNothing().when(accountManagerImpl).validateCurrentPassword(Mockito.eq(userVoMock), Mockito.anyString());

        String currentPassword = "theCurrentPassword";
        accountManagerImpl.validateUserPasswordAndUpdateIfNeeded(newPassword, userVoMock, currentPassword);

        Mockito.verify(accountManagerImpl, Mockito.times(1)).validateCurrentPassword(userVoMock, currentPassword);
        Mockito.verify(userVoMock, Mockito.times(1)).setPassword(expectedUserPasswordAfterEncoded);
    }

    private String configureUserMockAuthenticators(String newPassword) {
        accountManagerImpl._userPasswordEncoders = new ArrayList<>();
        UserAuthenticator authenticatorMock1 = Mockito.mock(UserAuthenticator.class);
        String expectedUserPasswordAfterEncoded = "passwordEncodedByAuthenticator1";
        Mockito.doReturn(expectedUserPasswordAfterEncoded).when(authenticatorMock1).encode(newPassword);

        UserAuthenticator authenticatorMock2 = Mockito.mock(UserAuthenticator.class);
        Mockito.lenient().doReturn("passwordEncodedByAuthenticator2").when(authenticatorMock2).encode(newPassword);

        accountManagerImpl._userPasswordEncoders.add(authenticatorMock1);
        accountManagerImpl._userPasswordEncoders.add(authenticatorMock2);
        return expectedUserPasswordAfterEncoded;
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateCurrentPasswordTestUserNotAuthenticatedWithProvidedCurrentPassword() {
        Mockito.doReturn(Mockito.mock(AccountVO.class)).when(_accountDao).findById(accountMockId);
        String newPassword = "newPassword";
        configureUserMockAuthenticators(newPassword);

        accountManagerImpl.validateCurrentPassword(userVoMock, "currentPassword");
    }

    @Test
    public void validateCurrentPasswordTestUserAuthenticatedWithProvidedCurrentPasswordViaFirstAuthenticator() {
        AccountVO accountVoMock = Mockito.mock(AccountVO.class);
        long domainId = 14l;
        Mockito.doReturn(domainId).when(accountVoMock).getDomainId();

        Mockito.doReturn(accountVoMock).when(_accountDao).findById(accountMockId);
        String username = "username";
        Mockito.doReturn(username).when(userVoMock).getUsername();

        accountManagerImpl._userPasswordEncoders = new ArrayList<>();
        UserAuthenticator authenticatorMock1 = Mockito.mock(UserAuthenticator.class);
        UserAuthenticator authenticatorMock2 = Mockito.mock(UserAuthenticator.class);

        accountManagerImpl._userPasswordEncoders.add(authenticatorMock1);
        accountManagerImpl._userPasswordEncoders.add(authenticatorMock2);

        Pair<Boolean, ActionOnFailedAuthentication> authenticationResult = new Pair<Boolean, UserAuthenticator.ActionOnFailedAuthentication>(true,
                UserAuthenticator.ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT);

        String currentPassword = "currentPassword";
        Mockito.doReturn(authenticationResult).when(authenticatorMock1).authenticate(username, currentPassword, domainId, null);

        accountManagerImpl.validateCurrentPassword(userVoMock, currentPassword);

        Mockito.verify(authenticatorMock1, Mockito.times(1)).authenticate(username, currentPassword, domainId, null);
        Mockito.verify(authenticatorMock2, Mockito.times(0)).authenticate(username, currentPassword, domainId, null);
    }

    @Test
    public void validateCurrentPasswordTestUserAuthenticatedWithProvidedCurrentPasswordViaSecondAuthenticator() {
        AccountVO accountVoMock = Mockito.mock(AccountVO.class);
        long domainId = 14l;
        Mockito.doReturn(domainId).when(accountVoMock).getDomainId();

        Mockito.doReturn(accountVoMock).when(_accountDao).findById(accountMockId);
        String username = "username";
        Mockito.doReturn(username).when(userVoMock).getUsername();

        accountManagerImpl._userPasswordEncoders = new ArrayList<>();
        UserAuthenticator authenticatorMock1 = Mockito.mock(UserAuthenticator.class);
        UserAuthenticator authenticatorMock2 = Mockito.mock(UserAuthenticator.class);

        accountManagerImpl._userPasswordEncoders.add(authenticatorMock1);
        accountManagerImpl._userPasswordEncoders.add(authenticatorMock2);

        Pair<Boolean, ActionOnFailedAuthentication> authenticationResult = new Pair<Boolean, UserAuthenticator.ActionOnFailedAuthentication>(true,
                UserAuthenticator.ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT);

        String currentPassword = "currentPassword";
        Mockito.doReturn(authenticationResult).when(authenticatorMock2).authenticate(username, currentPassword, domainId, null);

        accountManagerImpl.validateCurrentPassword(userVoMock, currentPassword);

        Mockito.verify(authenticatorMock1, Mockito.times(1)).authenticate(username, currentPassword, domainId, null);
        Mockito.verify(authenticatorMock2, Mockito.times(1)).authenticate(username, currentPassword, domainId, null);
    }

}
