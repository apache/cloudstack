/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.api.command.admin.user;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;

public class CreateUserCmdTest {
    protected Logger logger = LogManager.getLogger(getClass());

    @Mock
    private AccountService accountService;

    @InjectMocks
    private CreateUserCmd createUserCmd = new CreateUserCmd();

    private AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
        CallContext.unregister();
    }

    @Test
    public void testExecuteWithNotBlankPassword() {
        ReflectionTestUtils.setField(createUserCmd, "password", "Test");
        try {
            createUserCmd.execute();
        } catch (ServerApiException e) {
            Assert.assertTrue("Received exception as the mock accountService createUser returns null user", true);
        }
        Mockito.verify(accountService, Mockito.times(1)).createUser(null, "Test", null, null, null, null, null, null, null);
    }

    @Test
    public void testExecuteWithNullPassword() {
        ReflectionTestUtils.setField(createUserCmd, "password", null);
        try {
            createUserCmd.execute();
            Assert.fail("should throw exception for a null password");
        } catch (ServerApiException e) {
            Assert.assertEquals(ApiErrorCode.PARAM_ERROR,e.getErrorCode());
            Assert.assertEquals("Empty passwords are not allowed", e.getMessage());
        }
        Mockito.verify(accountService, Mockito.never()).createUser(null, null, null, null, null, null, null, null, null);
    }

    @Test
    public void testExecuteWithEmptyPassword() {
        ReflectionTestUtils.setField(createUserCmd, "password", "");
        try {
            createUserCmd.execute();
            Assert.fail("should throw exception for a empty password");
        } catch (ServerApiException e) {
            Assert.assertEquals(ApiErrorCode.PARAM_ERROR,e.getErrorCode());
            Assert.assertEquals("Empty passwords are not allowed", e.getMessage());
        }
        Mockito.verify(accountService, Mockito.never()).createUser(null, null, null, null, null, null, null, null, null);
    }
}
