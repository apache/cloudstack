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
package org.apache.cloudstack.api.command.admin.account;

import org.apache.cloudstack.acl.RoleService;
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

public class CreateAccountCmdTest {
    protected Logger logger = LogManager.getLogger(getClass());

    @Mock
    private AccountService accountService;
    @Mock
    private RoleService roleService;

    @InjectMocks
    private CreateAccountCmd createAccountCmd = new CreateAccountCmd();

    private long roleId = 1L;
    private Integer accountType = 1;
    private Long domainId = 1L;

    private AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(createAccountCmd, "domainId", domainId);
        ReflectionTestUtils.setField(createAccountCmd, "accountType", accountType);
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
        CallContext.unregister();
    }

    @Test
    public void testExecuteWithNotBlankPassword() {
        ReflectionTestUtils.setField(createAccountCmd, "password", "Test");
        try {
            createAccountCmd.execute();
        } catch (ServerApiException e) {
            Assert.assertTrue("Received exception as the mock accountService createUserAccount returns null user", true);
        }
        Mockito.verify(accountService, Mockito.times(1)).createUserAccount(createAccountCmd);
    }

    @Test
    public void testExecuteWithNullPassword() {
        ReflectionTestUtils.setField(createAccountCmd, "password", null);
        try {
            createAccountCmd.execute();
            Assert.fail("should throw exception for a null password");
        } catch (ServerApiException e) {
            Assert.assertEquals(ApiErrorCode.PARAM_ERROR, e.getErrorCode());
            Assert.assertEquals("Empty passwords are not allowed", e.getMessage());
        }
        Mockito.verify(accountService, Mockito.never()).createUserAccount(createAccountCmd);
    }

    @Test
    public void testExecuteWithEmptyPassword() {
        ReflectionTestUtils.setField(createAccountCmd, "password", "");
        try {
            createAccountCmd.execute();
            Assert.fail("should throw exception for a empty password");
        } catch (ServerApiException e) {
            Assert.assertEquals(ApiErrorCode.PARAM_ERROR, e.getErrorCode());
            Assert.assertEquals("Empty passwords are not allowed", e.getMessage());
        }
        Mockito.verify(accountService, Mockito.never()).createUserAccount(createAccountCmd);
    }
}
