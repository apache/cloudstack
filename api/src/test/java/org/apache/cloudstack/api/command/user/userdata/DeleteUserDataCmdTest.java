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
package org.apache.cloudstack.api.command.user.userdata;

import com.cloud.server.ManagementService;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class DeleteUserDataCmdTest {

    @InjectMocks
    DeleteUserDataCmd cmd =  new DeleteUserDataCmd();

    @Mock
    AccountService _accountService;
    @Mock
    ManagementService _mgr;

    private static final long DOMAIN_ID = 5L;
    private static final long PROJECT_ID = 10L;
    private static final String ACCOUNT_NAME = "user";

    private AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(cmd, "accountName", ACCOUNT_NAME);
        ReflectionTestUtils.setField(cmd, "domainId", DOMAIN_ID);
        ReflectionTestUtils.setField(cmd, "projectId", PROJECT_ID);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testValidUserDataExecute() {
        Mockito.doReturn(true).when(_mgr).deleteUserData(cmd);

        try {
            cmd.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertEquals(cmd.getResponseObject().getClass(), SuccessResponse.class);
    }

    @Test(expected = ServerApiException.class)
    public void testDeleteFailure() {
        Mockito.doReturn(false).when(_mgr).deleteUserData(cmd);
        cmd.execute();
    }

    @Test
    public void validateArgsCmd() {
        try (MockedStatic<CallContext> callContextMocked = Mockito.mockStatic(CallContext.class)) {
            CallContext callContextMock = Mockito.mock(CallContext.class);
            callContextMocked.when(CallContext::current).thenReturn(callContextMock);
            Account accountMock = Mockito.mock(Account.class);
            Mockito.when(callContextMock.getCallingAccount()).thenReturn(accountMock);
            Mockito.when(accountMock.getId()).thenReturn(2L);
            Mockito.doReturn(false).when(_accountService).isAdmin(2L);

            ReflectionTestUtils.setField(cmd, "id", 1L);

            Assert.assertEquals(1L, (long) cmd.getId());
            Assert.assertEquals(2L, cmd.getEntityOwnerId());
        }
    }
}
