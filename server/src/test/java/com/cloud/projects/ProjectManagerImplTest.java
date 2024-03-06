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
import java.util.List;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.webhook.WebhookHelper;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import com.cloud.projects.dao.ProjectDao;
import com.cloud.utils.component.ComponentContext;


@RunWith(MockitoJUnitRunner.class)
public class ProjectManagerImplTest {

    @Spy
    @InjectMocks
    ProjectManagerImpl projectManager;

    @Mock
    ProjectDao projectDao;

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
}
