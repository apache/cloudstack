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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.cloud.projects.dao.ProjectDao;


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
}
