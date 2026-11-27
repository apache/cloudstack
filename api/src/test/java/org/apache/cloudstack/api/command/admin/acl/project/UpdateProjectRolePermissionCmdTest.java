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
package org.apache.cloudstack.api.command.admin.acl.project;

import org.apache.cloudstack.acl.ProjectRole;
import org.apache.cloudstack.acl.ProjectRolePermission;
import org.apache.cloudstack.acl.ProjectRoleService;
import org.apache.cloudstack.api.ServerApiException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

public class UpdateProjectRolePermissionCmdTest {

    @Mock
    private ProjectRoleService projectRoleService;
    @Mock
    private ProjectRole projectRole;
    @Mock
    private ProjectRolePermission projectRolePermissionOne;
    @Mock
    private ProjectRolePermission projectRolePermissionTwo;

    private final UpdateProjectRolePermissionCmd updateProjectRolePermissionCmd = new UpdateProjectRolePermissionCmd();

    private static final Long projectId = 9L;
    private static final Long ruleOneId = 111L;
    private static final Long ruleTwoId = 222L;
    private static final Long projectRoleId = 20L;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        updateProjectRolePermissionCmd.projRoleService = projectRoleService;

        Mockito.when(projectRolePermissionOne.getProjectId()).thenReturn(projectId);
        Mockito.when(projectRolePermissionTwo.getProjectId()).thenReturn(projectId);
        Mockito.when(projectRolePermissionOne.getProjectRoleId()).thenReturn(projectRoleId);
        Mockito.when(projectRolePermissionTwo.getProjectRoleId()).thenReturn(projectRoleId);
        Mockito.when(projectRole.getId()).thenReturn(projectRoleId);

        ReflectionTestUtils.setField(updateProjectRolePermissionCmd, "projectRulePermissionOrder", Arrays.asList(ruleOneId, ruleTwoId));
        ReflectionTestUtils.setField(updateProjectRolePermissionCmd, "projectId", projectId);

        Mockito.when(projectRoleService.findProjectRolePermission(ruleOneId)).thenReturn(projectRolePermissionOne);
        Mockito.when(projectRoleService.findProjectRolePermission(ruleTwoId)).thenReturn(projectRolePermissionTwo);
    }

    @Test(expected = ServerApiException.class)
    public void testUpdateProjectRolePermissionOrderRuleIdFromAnotherProject() {
        Long differentProjectId = 2L;
        Mockito.when(projectRolePermissionTwo.getProjectId()).thenReturn(differentProjectId);
        updateProjectRolePermissionCmd.updateProjectRolePermissionOrder(projectRole);
    }

    @Test(expected = ServerApiException.class)
    public void testUpdateProjectRolePermissionOrderDifferentRuleRoleInSameProject() {
        Long differentRoleId = 3L;
        Mockito.when(projectRolePermissionTwo.getProjectRoleId()).thenReturn(differentRoleId);
        updateProjectRolePermissionCmd.updateProjectRolePermissionOrder(projectRole);
    }

    @Test
    public void testUpdateProjectRolePermissionOrder() {
        updateProjectRolePermissionCmd.updateProjectRolePermissionOrder(projectRole);
        Mockito.verify(projectRoleService).updateProjectRolePermission(Mockito.eq(projectId), Mockito.eq(projectRole),
                Mockito.eq(Arrays.asList(projectRolePermissionOne, projectRolePermissionTwo)));
    }
}
