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
package com.cloud.acl;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.backup.BackupOfferingVO;
import org.apache.cloudstack.backup.dao.BackupOfferingDetailsDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.projects.ProjectManager;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Ternary;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class DomainCheckerTest {

    @Mock
    AccountService _accountService;
    @Mock
    AccountDao _accountDao;
    @Mock
    DomainDao _domainDao;
    @Mock
    ProjectManager _projectMgr;
    @Mock
    BackupOfferingDetailsDao backupOfferingDetailsDao;

    @Spy
    @InjectMocks
    DomainChecker domainChecker;

    private ControlledEntity getMockedEntity(long accountId) {
        ControlledEntity entity = Mockito.mock(Account.class);
        Mockito.when(entity.getAccountId()).thenReturn(accountId);
        Mockito.when(entity.getEntityType()).thenReturn((Class)Account.class);
        return entity;
    }

    @Test
    public void testRootAdminHasAccess() {
        Account rootAdmin = Mockito.mock(Account.class);
        Mockito.when(rootAdmin.getId()).thenReturn(1L);
        ControlledEntity entity = getMockedEntity(2L);
        Mockito.when(_accountService.isRootAdmin(rootAdmin.getId())).thenReturn(true);

        domainChecker.validateCallerHasAccessToEntityOwner(rootAdmin, entity, SecurityChecker.AccessType.ModifyProject);
    }

    @Test
    public void testCallerIsOwner() {
        Account caller = Mockito.mock(Account.class);
        Mockito.when(caller.getId()).thenReturn(1L);
        ControlledEntity entity = getMockedEntity(1L);

        domainChecker.validateCallerHasAccessToEntityOwner(caller, entity, SecurityChecker.AccessType.ModifyProject);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testOwnerNotFound() {
        Account caller = Mockito.mock(Account.class);
        Mockito.when(caller.getId()).thenReturn(1L);
        ControlledEntity entity = getMockedEntity(2L);
        Mockito.when(_accountDao.findById(entity.getAccountId())).thenReturn(null);

        domainChecker.validateCallerHasAccessToEntityOwner(caller, entity, SecurityChecker.AccessType.ModifyProject);
    }

    @Test
    public void testDomainAdminHasAccess() {
        Account caller = Mockito.mock(Account.class);
        Mockito.when(caller.getId()).thenReturn(1L);
        Mockito.when(caller.getDomainId()).thenReturn(100L);
        Mockito.when(caller.getType()).thenReturn(Account.Type.DOMAIN_ADMIN);
        ControlledEntity entity = getMockedEntity(2L);
        AccountVO owner = Mockito.mock(AccountVO.class);
        Mockito.when(owner.getDomainId()).thenReturn(101L);
        Mockito.when(_accountDao.findById(entity.getAccountId())).thenReturn(owner);
        Mockito.when(_domainDao.isChildDomain(100L, 101L)).thenReturn(true);

        domainChecker.validateCallerHasAccessToEntityOwner(caller, entity, SecurityChecker.AccessType.ModifyProject);
    }

    private Ternary<Account, ControlledEntity, AccountVO> getProjectAccessCheckResources() {
        Account caller = Mockito.mock(Account.class);
        Mockito.when(caller.getId()).thenReturn(100L);
        Mockito.when(caller.getType()).thenReturn(Account.Type.PROJECT);
        ControlledEntity entity = getMockedEntity(2L);
        AccountVO projectAccount = Mockito.mock(AccountVO.class);
        Mockito.when(projectAccount.getId()).thenReturn(2L);
        Mockito.when(projectAccount.getType()).thenReturn(Account.Type.PROJECT);
        return new Ternary<>(caller, entity, projectAccount);
    }

    @Test
    public void testProjectOwnerCanModify() {
        Ternary<Account, ControlledEntity, AccountVO> resources = getProjectAccessCheckResources();
        Account caller = resources.first();
        ControlledEntity entity = resources.second();
        AccountVO projectAccount = resources.third();
        Mockito.when(_accountDao.findById(entity.getAccountId())).thenReturn(projectAccount);
        Mockito.when(_projectMgr.canModifyProjectAccount(caller, projectAccount.getId())).thenReturn(true);
        Mockito.doReturn(true).when(domainChecker).checkOperationPermitted(caller, entity);

        domainChecker.validateCallerHasAccessToEntityOwner(caller, entity, SecurityChecker.AccessType.ModifyProject);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testProjectOwnerCannotModify() {
        Ternary<Account, ControlledEntity, AccountVO> resources = getProjectAccessCheckResources();
        Account caller = resources.first();
        ControlledEntity entity = resources.second();
        AccountVO projectAccount = resources.third();
        Mockito.when(_accountDao.findById(entity.getAccountId())).thenReturn(projectAccount);
        Mockito.when(_projectMgr.canModifyProjectAccount(caller, projectAccount.getId())).thenReturn(false);

        domainChecker.validateCallerHasAccessToEntityOwner(caller, entity, SecurityChecker.AccessType.ModifyProject);
    }

    @Test
    public void testProjectOwnerCanAccess() {
        Ternary<Account, ControlledEntity, AccountVO> resources = getProjectAccessCheckResources();
        Account caller = resources.first();
        ControlledEntity entity = resources.second();
        AccountVO projectAccount = resources.third();
        Mockito.when(_accountDao.findById(entity.getAccountId())).thenReturn(projectAccount);
        Mockito.when(_projectMgr.canAccessProjectAccount(caller, projectAccount.getId())).thenReturn(true);
        Mockito.doReturn(true).when(domainChecker).checkOperationPermitted(caller, entity);

        domainChecker.validateCallerHasAccessToEntityOwner(caller, entity, SecurityChecker.AccessType.ListEntry);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testProjectOwnerCannotAccess() {
        Ternary<Account, ControlledEntity, AccountVO> resources = getProjectAccessCheckResources();
        Account caller = resources.first();
        ControlledEntity entity = resources.second();
        AccountVO projectAccount = resources.third();
        Mockito.when(_accountDao.findById(entity.getAccountId())).thenReturn(projectAccount);
        Mockito.when(_projectMgr.canAccessProjectAccount(caller, projectAccount.getId())).thenReturn(false);

        domainChecker.validateCallerHasAccessToEntityOwner(caller, entity, SecurityChecker.AccessType.ListEntry);
    }

    @Test
    public void testBackupOfferingAccessRootAdmin() {
        Account rootAdmin = Mockito.mock(Account.class);
        Mockito.when(rootAdmin.getId()).thenReturn(1L);
        BackupOfferingVO backupOfferingVO = Mockito.mock(BackupOfferingVO.class);
        Mockito.when(_accountService.isRootAdmin(rootAdmin.getId())).thenReturn(true);

        boolean hasAccess = domainChecker.checkAccess(rootAdmin, backupOfferingVO);
        Assert.assertTrue(hasAccess);
    }

    @Test
    public void testBackupOfferingAccessDomainAdmin() {
        Account domainAdmin = Mockito.mock(Account.class);
        Mockito.when(domainAdmin.getId()).thenReturn(2L);
        BackupOfferingVO backupOfferingVO = Mockito.mock(BackupOfferingVO.class);
        AccountVO owner = Mockito.mock(AccountVO.class);
        Mockito.when(_accountService.isDomainAdmin(domainAdmin.getId())).thenReturn(true);
        Mockito.when(domainAdmin.getDomainId()).thenReturn(10L);
        Mockito.when(_domainDao.isChildDomain(100L, 10L)).thenReturn(true);
        Mockito.when(backupOfferingDetailsDao.findDomainIds(backupOfferingVO.getId())).thenReturn(Collections.singletonList(100L));

        boolean hasAccess = domainChecker.checkAccess(domainAdmin, backupOfferingVO);
        Assert.assertTrue(hasAccess);
    }

    @Test
    public void testBackupOfferingAccessNoAccess() {
        Account normalUser = Mockito.mock(Account.class);
        Mockito.when(normalUser.getId()).thenReturn(3L);
        BackupOfferingVO backupOfferingVO = Mockito.mock(BackupOfferingVO.class);
        Mockito.when(_accountService.isRootAdmin(normalUser.getId())).thenReturn(false);
        Mockito.when(_accountService.isDomainAdmin(normalUser.getId())).thenReturn(false);

        boolean hasAccess = domainChecker.checkAccess(normalUser, backupOfferingVO);
        Assert.assertFalse(hasAccess);
    }

}
