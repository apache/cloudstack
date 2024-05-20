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
package com.cloud.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.acl.APIChecker;
import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.exception.PermissionDeniedException;
import com.cloud.projects.Project;
import com.cloud.user.User;

@RunWith(MockitoJUnitRunner.class)
public class ApiServerTest {

    @InjectMocks
    ApiServer apiServer = new ApiServer();

    private static final String PROJECT_ACCESSIBLE_CMD_NAME = "projectcommand";
    private static final String PROJECT_INACCESSIBLE_CMD_NAME = "noprojectcommand";
    private static final String ACCESSIBLE_CMD_NAME = "command";
    private static final String INACCESSIBLE_CMD_NAME = "nocommand";
    private User user = Mockito.mock(User.class);
    private CallContext callContext = Mockito.mock(CallContext.class);
    MockedStatic<CallContext> mockedCallContext;

    @Before
    public void setup() throws Exception {
        mockedCallContext = Mockito.mockStatic(CallContext.class);
        mockedCallContext.when(CallContext::current).thenReturn(callContext);
        List<APIChecker> apiCheckers = new ArrayList<>();
        APIChecker checker1 = Mockito.mock(APIChecker.class);
        apiCheckers.add(checker1);
        APIChecker checker2 = Mockito.mock(APIChecker.class);
        Mockito.when(checker2.isProjectRoleBasedChecker())
                .thenReturn(true);
        apiCheckers.add(checker2);
        Mockito.doAnswer((Answer<Boolean>) invocation -> {
            String cmd = (String) invocation.getArguments()[1];
            if (!cmd.equals(ACCESSIBLE_CMD_NAME)) {
                throw new PermissionDeniedException("Denied");
            }
            return true;
        }).when(checker1).checkAccess(Mockito.any(User.class), Mockito.anyString());
        Mockito.doAnswer((Answer<Boolean>) invocation -> {
            String cmd = (String) invocation.getArguments()[1];
            if (List.of(PROJECT_ACCESSIBLE_CMD_NAME, ACCESSIBLE_CMD_NAME).contains(cmd)) {
                return true;
            } else if (cmd.equals(PROJECT_INACCESSIBLE_CMD_NAME)) {
                return false;
            }
            throw new PermissionDeniedException("Denied");
        }).when(checker2).checkAccess(Mockito.any(User.class), Mockito.anyString());
        ReflectionTestUtils.setField(apiServer, "apiAccessCheckers", apiCheckers);
    }

    @After
    public void tearDown() throws Exception {
        mockedCallContext.close();
    }

    @Test
    public void testCheckCommandAccessWithCheckersProjectCommand() {
        Mockito.when(callContext.getProject()).thenReturn(Mockito.mock(Project.class));
        Assert.assertTrue(apiServer.checkCommandAccessWithCheckers(user, PROJECT_ACCESSIBLE_CMD_NAME));
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCheckCommandAccessWithCheckersInaccessibleProjectCommand() {
        Mockito.when(callContext.getProject()).thenReturn(Mockito.mock(Project.class));
        apiServer.checkCommandAccessWithCheckers(user, PROJECT_INACCESSIBLE_CMD_NAME);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCheckCommandAccessWithCheckersProjectCommandWithoutProject() {
        apiServer.checkCommandAccessWithCheckers(user, PROJECT_INACCESSIBLE_CMD_NAME);
    }

    @Test
    public void testCheckCommandAccessWithCheckersAccessibleCommand() {
        Assert.assertTrue(apiServer.checkCommandAccessWithCheckers(user, ACCESSIBLE_CMD_NAME));
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCheckCommandAccessWithCheckersInaccessibleCommand() {
        apiServer.checkCommandAccessWithCheckers(user, INACCESSIBLE_CMD_NAME);
    }
}
