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
package com.cloud.projects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.reservation.dao.ReservationDao;
import org.apache.cloudstack.webhook.WebhookHelper;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import com.cloud.configuration.Resource.ResourceType;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.projects.ProjectAccount.Role;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.projects.dao.ProjectInvitationDao;
import com.cloud.resourcelimit.CheckedReservation;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.ComponentContext;


@RunWith(MockitoJUnitRunner.class)
public class ProjectManagerImplTest {

    @Spy
    @InjectMocks
    ProjectManagerImpl projectManager;

    @Mock
    ProjectDao projectDao;

    @Mock
    ProjectInvitationDao projectInvitationDao;

    @Mock
    ProjectAccountDao projectAccountDao;

    @Mock
    AccountDao accountDao;

    @Mock
    ResourceLimitService resourceLimitMgr;

    @Mock
    ReservationDao reservationDao;

    List<ProjectVO> updateProjects;

    @Before
    public void setUp() throws Exception {
        updateProjects = new ArrayList<>();
        Mockito.when(projectDao.update(Mockito.anyLong(), Mockito.any(ProjectVO.class))).thenAnswer((Answer<Boolean>) invocation -> {
            ProjectVO project = (ProjectVO)invocation.getArguments()[1];
            updateProjects.add(project);
            return true;
        });
    }

    private void runUpdateProjectNameAndDisplayTextTest(boolean nonNullName, boolean nonNullDisplayText) {
        ProjectVO projectVO = new ProjectVO();
        String newName = nonNullName ? "NewName" : null;
        String newDisplayText = nonNullDisplayText ? "NewDisplayText" : null;
        projectManager.updateProjectNameAndDisplayText(projectVO, newName, newDisplayText);
        if (!nonNullName && !nonNullDisplayText) {
            Assert.assertTrue(updateProjects.isEmpty());
        } else {
            Assert.assertFalse(updateProjects.isEmpty());
            Assert.assertEquals(1, updateProjects.size());
            ProjectVO updatedProject = updateProjects.get(0);
            Assert.assertNotNull(updatedProject);
            if (nonNullName) {
                Assert.assertEquals(newName, updatedProject.getName());
            }
            if (nonNullDisplayText) {
                Assert.assertEquals(newDisplayText, updatedProject.getDisplayText());
            }
        }
    }

    @Test
    public void testUpdateProjectNameAndDisplayTextNoUpdate() {
        runUpdateProjectNameAndDisplayTextTest(false, false);
    }

    @Test
    public void testUpdateProjectNameAndDisplayTextUpdateName() {
        runUpdateProjectNameAndDisplayTextTest(true, false);
    }

    @Test
    public void testUpdateProjectNameAndDisplayTextUpdateDisplayText() {
        runUpdateProjectNameAndDisplayTextTest(false, true);
    }

    @Test
    public void testUpdateProjectNameAndDisplayTextUpdateNameDisplayText() {
        runUpdateProjectNameAndDisplayTextTest(true, true);
    }

    @Test
    public void testDeleteWebhooksForAccount() {
        try (MockedStatic<ComponentContext> mockedComponentContext = Mockito.mockStatic(ComponentContext.class)) {
            WebhookHelper webhookHelper = Mockito.mock(WebhookHelper.class);
            List<ControlledEntity> webhooks = List.of(Mockito.mock(ControlledEntity.class),
                    Mockito.mock(ControlledEntity.class));
            Mockito.doReturn(webhooks).when(webhookHelper).listWebhooksByAccount(Mockito.anyLong());
            mockedComponentContext.when(() -> ComponentContext.getDelegateComponentOfType(WebhookHelper.class))
                    .thenReturn(webhookHelper);
            Project project = Mockito.mock(Project.class);
            Mockito.when(project.getProjectAccountId()).thenReturn(1L);
            List<? extends ControlledEntity> result = projectManager.listWebhooksForProject(project);
            Assert.assertEquals(2, result.size());
        }
    }

    @Test
    public void testDeleteWebhooksForAccountNoBean() {
        try (MockedStatic<ComponentContext> mockedComponentContext = Mockito.mockStatic(ComponentContext.class)) {
            mockedComponentContext.when(() -> ComponentContext.getDelegateComponentOfType(WebhookHelper.class))
                    .thenThrow(NoSuchBeanDefinitionException.class);
            List<? extends ControlledEntity> result =
                    projectManager.listWebhooksForProject(Mockito.mock(Project.class));
            Assert.assertTrue(CollectionUtils.isEmpty(result));
        }
    }

    @Test
    public void cleanupProjectsForUserTestNoAssociationsReturnsFalse() {
        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getId()).thenReturn(100L);
        User user = mockUser(1L, 10L);
        Mockito.when(projectAccountDao.listBy(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Collections.emptyList());

        boolean result = projectManager.cleanupProjectsForUser(project, user);

        Assert.assertFalse(result);
        Mockito.verify(projectInvitationDao).removeBy(100L, 10L, 1L);
        Mockito.verify(projectAccountDao, Mockito.never()).remove(Mockito.anyLong());
        Mockito.verify(resourceLimitMgr, Mockito.never()).decrementResourceCount(Mockito.anyLong(), Mockito.any(ResourceType.class));
    }

    @Test
    public void cleanupProjectsForUserTestRemovesAdminAndRegularAssociations() {
        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getId()).thenReturn(100L);
        User user = mockUser(1L, 10L);
        ProjectAccountVO admin = mockProjectAccount(1L, Role.Admin);
        ProjectAccountVO regular = mockProjectAccount(2L, Role.Regular);
        Mockito.when(projectAccountDao.listBy(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(List.of(admin, regular));

        boolean result = projectManager.cleanupProjectsForUser(project, user);

        Assert.assertTrue(result);
        Mockito.verify(projectInvitationDao).removeBy(100L, 10L, 1L);
        Mockito.verify(projectAccountDao).remove(1L);
        Mockito.verify(projectAccountDao).remove(2L);
        Mockito.verify(resourceLimitMgr).decrementResourceCount(10L, ResourceType.project);
    }

    @Test
    public void cleanupProjectsForUserTestNullProject() {
        User user = mockUser(1L, 10L);
        ProjectAccountVO admin = mockProjectAccount(1L, Role.Admin);
        Mockito.when(projectAccountDao.listBy(Mockito.isNull(), Mockito.eq(10L), Mockito.eq(1L)))
                .thenReturn(List.of(admin));

        boolean result = projectManager.cleanupProjectsForUser(null, user);

        Assert.assertTrue(result);
        Mockito.verify(projectInvitationDao).removeBy(Mockito.isNull(), Mockito.eq(10L), Mockito.eq(1L));
        Mockito.verify(projectAccountDao).remove(1L);
        Mockito.verify(resourceLimitMgr).decrementResourceCount(10L, ResourceType.project);
    }

    @Test
    public void moveProjectAssociationsToUserTestNoProjectAccounts() throws ResourceAllocationException {
        User oldUser = mockUser(1L, 10L);
        User newUser = mockUser(2L, 20L);
        Mockito.when(projectAccountDao.listBy(Mockito.isNull(), Mockito.eq(10L), Mockito.eq(1L)))
                .thenReturn(Collections.emptyList());

        projectManager.moveProjectAssociationsToUser(oldUser, newUser);

        Mockito.verify(projectInvitationDao).move(oldUser, newUser);
        Mockito.verify(projectAccountDao, Mockito.never()).move(Mockito.any(), Mockito.any());
        Mockito.verifyNoInteractions(accountDao);
        Mockito.verifyNoInteractions(resourceLimitMgr);
    }

    @Test
    public void moveProjectAssociationsToUserTestSameAccount() throws ResourceAllocationException {
        User oldUser = mockUser(1L, 10L);
        User newUser = mockUser(2L, 10L);
        ProjectAccountVO regular = mockProjectAccount(1L, Role.Regular);
        Mockito.when(projectAccountDao.listBy(Mockito.isNull(), Mockito.eq(10L), Mockito.eq(1L)))
                .thenReturn(List.of(regular));
        AccountVO oldAccount = mockAccount(10L);
        AccountVO newAccount = mockAccount(10L);
        Mockito.when(accountDao.findById(10L)).thenReturn(oldAccount).thenReturn(newAccount);

        try (MockedConstruction<CheckedReservation> ignored = Mockito.mockConstruction(CheckedReservation.class)) {
            projectManager.moveProjectAssociationsToUser(oldUser, newUser);
        }

        Mockito.verify(projectInvitationDao).move(oldUser, newUser);
        Mockito.verify(projectAccountDao).move(oldUser, newUser);
        Mockito.verify(resourceLimitMgr, Mockito.never())
                .incrementResourceCount(Mockito.anyLong(), Mockito.any(ResourceType.class), Mockito.anyLong());
        Mockito.verify(resourceLimitMgr, Mockito.never())
                .decrementResourceCount(Mockito.anyLong(), Mockito.any(ResourceType.class), Mockito.anyLong());
    }

    @Test
    public void moveProjectAssociationsToUserTestDifferentAccountsWithAdminRole() throws ResourceAllocationException {
        User oldUser = mockUser(1L, 10L);
        User newUser = mockUser(2L, 20L);
        ProjectAccountVO admin = mockProjectAccount(1L, Role.Admin);
        Mockito.when(projectAccountDao.listBy(Mockito.isNull(), Mockito.eq(10L), Mockito.eq(1L)))
                .thenReturn(List.of(admin));
        AccountVO oldAccount = mockAccount(10L);
        AccountVO newAccount = mockAccount(20L);
        Mockito.when(accountDao.findById(10L)).thenReturn(oldAccount);
        Mockito.when(accountDao.findById(20L)).thenReturn(newAccount);

        try (MockedConstruction<CheckedReservation> ignored = Mockito.mockConstruction(CheckedReservation.class)) {
            projectManager.moveProjectAssociationsToUser(oldUser, newUser);
        }

        Mockito.verify(projectInvitationDao).move(oldUser, newUser);
        Mockito.verify(projectAccountDao).move(oldUser, newUser);
        Mockito.verify(resourceLimitMgr).incrementResourceCount(20L, ResourceType.project, 1L);
        Mockito.verify(resourceLimitMgr).decrementResourceCount(10L, ResourceType.project, 1L);
    }

    @Test
    public void moveProjectAssociationsToUserTestDifferentAccountsWithoutAdminRole() throws ResourceAllocationException {
        User oldUser = mockUser(1L, 10L);
        User newUser = mockUser(2L, 20L);
        ProjectAccountVO regular = mockProjectAccount(1L, Role.Regular);
        Mockito.when(projectAccountDao.listBy(Mockito.isNull(), Mockito.eq(10L), Mockito.eq(1L)))
                .thenReturn(List.of(regular));
        AccountVO oldAccount = mockAccount(10L);
        AccountVO newAccount = mockAccount(20L);
        Mockito.when(accountDao.findById(10L)).thenReturn(oldAccount);
        Mockito.when(accountDao.findById(20L)).thenReturn(newAccount);

        try (MockedConstruction<CheckedReservation> ignored = Mockito.mockConstruction(CheckedReservation.class)) {
            projectManager.moveProjectAssociationsToUser(oldUser, newUser);
        }

        Mockito.verify(projectInvitationDao).move(oldUser, newUser);
        Mockito.verify(projectAccountDao).move(oldUser, newUser);
        Mockito.verify(resourceLimitMgr, Mockito.never())
                .incrementResourceCount(Mockito.anyLong(), Mockito.any(ResourceType.class), Mockito.anyLong());
        Mockito.verify(resourceLimitMgr, Mockito.never())
                .decrementResourceCount(Mockito.anyLong(), Mockito.any(ResourceType.class), Mockito.anyLong());
    }

    private User mockUser(long id, long accountId) {
        User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(id);
        Mockito.when(user.getAccountId()).thenReturn(accountId);
        return user;
    }

    private AccountVO mockAccount(long id) {
        AccountVO account = Mockito.mock(AccountVO.class);
        Mockito.when(account.getId()).thenReturn(id);
        return account;
    }

    private ProjectAccountVO mockProjectAccount(long id, Role role) {
        ProjectAccountVO projectAccount = Mockito.mock(ProjectAccountVO.class);
        Mockito.when(projectAccount.getId()).thenReturn(id);
        Mockito.when(projectAccount.getAccountRole()).thenReturn(role);
        return projectAccount;
    }
}
