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

package org.apache.cloudstack.acl;

import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.acl.dao.ProjectRoleDao;
import org.apache.cloudstack.acl.dao.ProjectRolePermissionsDao;
import org.apache.cloudstack.api.command.admin.acl.project.CreateProjectRolePermissionCmd;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.projects.ProjectAccount;
import com.cloud.projects.ProjectAccountVO;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;

/**
 * Regression tests for the ProjectRole authorization bypass: a Domain Admin of any subdomain
 * used to be granted access to any project's roles, regardless of whether that project's domain
 * fell under their own domain hierarchy.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectRoleManagerImplTest {

    private static final long PROJECT_ID = 10L;
    private static final long CALLER_ACCOUNT_ID = 100L;
    private static final long CALLER_ROLE_ID = 5L;
    private static final long CALLER_USER_ID = 55L;

    @Spy
    @InjectMocks
    private ProjectRoleManagerImpl projectRoleManagerImpl;

    @Mock
    private ProjectAccountDao projAccDaoMock;
    @Mock
    private ProjectRoleDao projRoleDaoMock;
    @Mock
    private ProjectDao projectDaoMock;
    @Mock
    private AccountDao accountDaoMock;
    @Mock
    private ProjectRolePermissionsDao projRolePermissionsDaoMock;
    @Mock
    private AccountService accountServiceMock;
    @Mock
    private DomainDao domainDaoMock;

    @Mock
    private ProjectVO projectMock;
    @Mock
    private DomainVO projectDomainMock;
    @Mock
    private User callerUserMock;
    @Mock
    private AccountVO callerAccountMock;
    @Mock
    private ProjectAccountVO projectAccountMock;
    @Mock
    private ProjectRolePermission projectRolePermissionMock;
    @Mock
    private CreateProjectRolePermissionCmd createProjectRolePermissionCmdMock;

    @Before
    public void setup() {
        Mockito.doReturn(true).when(projectRoleManagerImpl).isEnabled();

        Mockito.doReturn(projectMock).when(projectDaoMock).findById(PROJECT_ID);
        Mockito.doReturn(7L).when(projectMock).getDomainId();
        Mockito.doReturn(projectDomainMock).when(domainDaoMock).findById(7L);

        Mockito.doReturn(callerUserMock).when(projectRoleManagerImpl).getCurrentUser();
        Mockito.doReturn(CALLER_USER_ID).when(callerUserMock).getId();
        Mockito.doReturn(CALLER_ACCOUNT_ID).when(callerUserMock).getAccountId();
        Mockito.doReturn(callerAccountMock).when(accountDaoMock).findById(CALLER_ACCOUNT_ID);
        Mockito.doReturn(CALLER_ACCOUNT_ID).when(callerAccountMock).getId();
        Mockito.doReturn(CALLER_ACCOUNT_ID).when(callerAccountMock).getAccountId();
        Mockito.doReturn(CALLER_ROLE_ID).when(callerAccountMock).getRoleId();

        Mockito.doReturn(false).when(accountServiceMock).isRootAdmin(Mockito.anyLong());
        Mockito.doReturn(false).when(accountServiceMock).isDomainAdmin(Mockito.anyLong());

        Mockito.doReturn(Collections.singletonList(new ProjectRoleVO())).when(projRoleDaoMock).findAllRoles(Mockito.eq(PROJECT_ID), Mockito.any());
    }

    private List<ProjectRole> listRoles() {
        return projectRoleManagerImpl.findProjectRoles(PROJECT_ID, null);
    }

    @Test
    public void rootAdminCanAccessAnyProject() {
        Mockito.doReturn(true).when(accountServiceMock).isRootAdmin(CALLER_ACCOUNT_ID);

        List<ProjectRole> roles = listRoles();

        Assert.assertEquals(1, roles.size());
        Mockito.verify(accountServiceMock, Mockito.never()).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));
    }

    @Test
    public void domainAdminWithinProjectDomainHierarchyIsAllowed() {
        Mockito.doReturn(true).when(accountServiceMock).isDomainAdmin(CALLER_ACCOUNT_ID);
        Mockito.doNothing().when(accountServiceMock).checkAccess(callerAccountMock, projectDomainMock);

        List<ProjectRole> roles = listRoles();

        Assert.assertEquals(1, roles.size());
        Mockito.verify(accountServiceMock).checkAccess(callerAccountMock, projectDomainMock);
    }

    /**
     * Regression test: a domain admin whose domain has no ancestor relationship to the project's
     * domain must be denied.
     */
    @Test(expected = PermissionDeniedException.class)
    public void domainAdminOutsideProjectDomainHierarchyIsDenied() {
        Mockito.doReturn(true).when(accountServiceMock).isDomainAdmin(CALLER_ACCOUNT_ID);
        Mockito.doThrow(new PermissionDeniedException("not in hierarchy"))
                .when(accountServiceMock).checkAccess(callerAccountMock, projectDomainMock);

        listRoles();
    }

    @Test
    public void projectAdminMemberIsAllowed() {
        Mockito.doReturn(projectAccountMock).when(projAccDaoMock)
                .findByProjectIdUserId(PROJECT_ID, CALLER_ACCOUNT_ID, CALLER_USER_ID);
        Mockito.doReturn(ProjectAccount.Role.Admin).when(projectAccountMock).getAccountRole();

        List<ProjectRole> roles = listRoles();

        Assert.assertEquals(1, roles.size());
    }

    @Test(expected = PermissionDeniedException.class)
    public void projectMemberWithoutAdminRoleIsDenied() {
        Mockito.doReturn(projectAccountMock).when(projAccDaoMock)
                .findByProjectIdUserId(PROJECT_ID, CALLER_ACCOUNT_ID, CALLER_USER_ID);
        Mockito.doReturn(ProjectAccount.Role.Regular).when(projectAccountMock).getAccountRole();

        listRoles();
    }

    @Test(expected = PermissionDeniedException.class)
    public void accountNotPartOfProjectIsDenied() {
        Mockito.doReturn(null).when(projAccDaoMock)
                .findByProjectIdUserId(PROJECT_ID, CALLER_ACCOUNT_ID, CALLER_USER_ID);
        Mockito.doReturn(null).when(projAccDaoMock).findByProjectIdAccountId(PROJECT_ID, CALLER_ACCOUNT_ID);

        listRoles();
    }

    @Test(expected = PermissionDeniedException.class)
    public void dynamicApiCheckerDisabledDeniesEveryone() {
        Mockito.doReturn(false).when(projectRoleManagerImpl).isEnabled();

        listRoles();
    }

    @Test
    public void listProjectRolePermissionsAllowedForRootAdmin() {
        Mockito.doReturn(true).when(accountServiceMock).isRootAdmin(CALLER_ACCOUNT_ID);
        Mockito.doReturn(Collections.singletonList(new ProjectRolePermissionVO(PROJECT_ID, 1L, "rule", RolePermissionEntity.Permission.ALLOW, "desc")))
                .when(projRolePermissionsDaoMock).findAllByRoleIdSorted(Mockito.eq(1L), Mockito.eq(PROJECT_ID));

        List<ProjectRolePermission> permissions = projectRoleManagerImpl.findAllProjectRolePermissions(PROJECT_ID, 1L);

        Assert.assertEquals(1, permissions.size());
    }

    @Test(expected = PermissionDeniedException.class)
    public void listProjectRolePermissionsDeniedForDomainAdminOutsideProjectDomainHierarchy() {
        Mockito.doReturn(true).when(accountServiceMock).isDomainAdmin(CALLER_ACCOUNT_ID);
        Mockito.doThrow(new PermissionDeniedException("not in hierarchy"))
                .when(accountServiceMock).checkAccess(callerAccountMock, projectDomainMock);

        projectRoleManagerImpl.findAllProjectRolePermissions(PROJECT_ID, 1L);
    }

    @Test
    public void deleteProjectRolePermissionAllowedForRootAdmin() {
        Mockito.doReturn(true).when(accountServiceMock).isRootAdmin(CALLER_ACCOUNT_ID);
        Mockito.doReturn(PROJECT_ID).when(projectRolePermissionMock).getProjectId();
        Mockito.doReturn(1L).when(projectRolePermissionMock).getId();
        Mockito.doReturn(true).when(projRolePermissionsDaoMock).remove(1L);

        boolean result = projectRoleManagerImpl.deleteProjectRolePermission(projectRolePermissionMock);

        Assert.assertTrue(result);
    }

    @Test(expected = PermissionDeniedException.class)
    public void deleteProjectRolePermissionDeniedForDomainAdminOutsideProjectDomainHierarchy() {
        Mockito.doReturn(true).when(accountServiceMock).isDomainAdmin(CALLER_ACCOUNT_ID);
        Mockito.doReturn(PROJECT_ID).when(projectRolePermissionMock).getProjectId();
        Mockito.doThrow(new PermissionDeniedException("not in hierarchy"))
                .when(accountServiceMock).checkAccess(callerAccountMock, projectDomainMock);

        projectRoleManagerImpl.deleteProjectRolePermission(projectRolePermissionMock);
    }

    @Test(expected = PermissionDeniedException.class)
    public void createProjectRolePermissionDeniedForDomainAdminOutsideProjectDomainHierarchy() {
        Mockito.doReturn(PROJECT_ID).when(createProjectRolePermissionCmdMock).getProjectId();
        Mockito.doReturn(true).when(accountServiceMock).isDomainAdmin(CALLER_ACCOUNT_ID);
        Mockito.doThrow(new PermissionDeniedException("not in hierarchy"))
                .when(accountServiceMock).checkAccess(callerAccountMock, projectDomainMock);

        projectRoleManagerImpl.createProjectRolePermission(createProjectRolePermissionCmdMock);
    }
}
