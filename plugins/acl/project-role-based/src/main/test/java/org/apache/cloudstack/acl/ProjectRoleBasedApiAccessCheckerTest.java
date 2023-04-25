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

import com.cloud.exception.PermissionDeniedException;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectAccount;
import com.cloud.projects.ProjectAccountVO;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.user.User;
import com.cloud.user.UserVO;

import org.apache.cloudstack.context.CallContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CallContext.class)
public class ProjectRoleBasedApiAccessCheckerTest extends TestCase {
    @Mock
    ProjectAccountDao projectAccountDaoMock;

    @Mock
    RoleService roleServiceMock;

    @Mock
    ProjectAccountVO projectAccountVOMock;

    @Mock
    CallContext callContextMock;

    @InjectMocks
    ProjectRoleBasedApiAccessChecker projectRoleBasedApiAccessCheckerSpy = Mockito.spy(ProjectRoleBasedApiAccessChecker.class);

    List<String> apiNames = new ArrayList<>(Arrays.asList("apiName"));

    @Before
    public void setup() {

        Mockito.doReturn(true).when(roleServiceMock).isEnabled();
    }

    public Project getTestProject() {
        return new ProjectVO("Teste", "Teste", 1L, 1L);
    }

    private User getTestUser() {
        return new UserVO(12L, "some user", "password", "firstName", "lastName",
                "email@gmail.com", "GMT", "uuid", User.Source.UNKNOWN);
    }

    @Test
    public void getApisAllowedToUserTestRoleServiceIsDisabledShouldReturnUnchangedApiList() {
        Mockito.doReturn(false).when(roleServiceMock).isEnabled();

        List<String> apisReceived = projectRoleBasedApiAccessCheckerSpy.getApisAllowedToUser(null, getTestUser(), apiNames);
        Assert.assertEquals(1, apisReceived.size());
    }

    @Test
    public void getApisAllowedToUserTestProjectIsNullShouldReturnUnchangedApiList() {
        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.doReturn(null).when(callContextMock).getProject();

        List<String> apisReceived = projectRoleBasedApiAccessCheckerSpy.getApisAllowedToUser(null, getTestUser(), apiNames);
        Assert.assertEquals(1, apisReceived.size());
    }

    @Test (expected = PermissionDeniedException.class)
    public void getApisAllowedToUserTestProjectAccountIsNullThrowPermissionDeniedException() {
        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.when(callContextMock.getProject()).thenReturn(getTestProject());
        Mockito.when(projectAccountDaoMock.findByProjectIdAccountId(Mockito.anyLong(), Mockito.anyLong())).thenReturn(null);
        Mockito.when(projectAccountDaoMock.findByProjectIdUserId(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong())).thenReturn(null);

        projectRoleBasedApiAccessCheckerSpy.getApisAllowedToUser(null, getTestUser(), apiNames);
    }

    @Test
    public void getApisAllowedToUserTestProjectAccountHasAdminRoleReturnsUnchangedApiList() {
        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.doReturn(getTestProject()).when(callContextMock).getProject();
        Mockito.doReturn(projectAccountVOMock).when(projectAccountDaoMock).findByProjectIdUserId(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(ProjectAccount.Role.Admin).when(projectAccountVOMock).getAccountRole();

        List<String> apisReceived = projectRoleBasedApiAccessCheckerSpy.getApisAllowedToUser(null, getTestUser(), apiNames);
        Assert.assertEquals(1, apisReceived.size());
    }

    @Test
    public void getApisAllowedToUserTestProjectAccountNotPermittedForTheApiListShouldReturnEmptyList() {
        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.doReturn(getTestProject()).when(callContextMock).getProject();
        Mockito.doReturn(projectAccountVOMock).when(projectAccountDaoMock).findByProjectIdUserId(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(ProjectAccount.Role.Regular).when(projectAccountVOMock).getAccountRole();
        Mockito.doReturn(false).when(projectRoleBasedApiAccessCheckerSpy).isPermitted(Mockito.any(Project.class), Mockito.any(ProjectAccount.class), Mockito.anyString());


        List<String> apisReceived = projectRoleBasedApiAccessCheckerSpy.getApisAllowedToUser(null, getTestUser(), apiNames);
        Assert.assertTrue(apisReceived.isEmpty());
    }

    @Test
    public void getApisAllowedToUserTestProjectAccountPermittedForTheApiListShouldReturnTheSameList() {
        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.doReturn(getTestProject()).when(callContextMock).getProject();
        Mockito.doReturn(projectAccountVOMock).when(projectAccountDaoMock).findByProjectIdUserId(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(ProjectAccount.Role.Regular).when(projectAccountVOMock).getAccountRole();
        Mockito.doReturn(true).when(projectRoleBasedApiAccessCheckerSpy).isPermitted(Mockito.any(Project.class), Mockito.any(ProjectAccount.class), Mockito.anyString());


        List<String> apisReceived = projectRoleBasedApiAccessCheckerSpy.getApisAllowedToUser(null, getTestUser(), apiNames);
        Assert.assertEquals(1, apisReceived.size());
    }
}
