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
package org.apache.cloudstack.api.command.test;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import org.apache.cloudstack.api.command.user.project.ActivateProjectCmd;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectService;
import com.cloud.user.Account;

public class ActivateProjectCmdTest extends TestCase {

    private ActivateProjectCmd activateProjectCmd;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    @Before
    public void setUp() {

        activateProjectCmd = new ActivateProjectCmd() {

            @Override
            public Long getId() {
                return 2L;
            }

        };
    }

    @Test
    public void testGetEntityOwnerIdForNullProject() {
        ProjectService projectService = Mockito.mock(ProjectService.class);
        Mockito.when(projectService.getProject(ArgumentMatchers.anyLong())).thenReturn(null);
        activateProjectCmd._projectService = projectService;

        try {
            activateProjectCmd.getEntityOwnerId();
        } catch (InvalidParameterValueException exception) {
            Assert.assertEquals("Unable to find project by id 2", exception.getLocalizedMessage());
        }
    }

    @Test
    public void testGetEntityOwnerIdForProject() {
        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getId()).thenReturn(2L);
        ProjectService projectService = Mockito.mock(ProjectService.class);
        Account account = Mockito.mock(Account.class);
        Mockito.when(account.getId()).thenReturn(2L);
        Mockito.when(projectService.getProject(ArgumentMatchers.anyLong())).thenReturn(project);

        Mockito.when(projectService.getProjectOwner(ArgumentMatchers.anyLong())).thenReturn(account);
        activateProjectCmd._projectService = projectService;

        Assert.assertEquals(2L, activateProjectCmd.getEntityOwnerId());

    }

}
