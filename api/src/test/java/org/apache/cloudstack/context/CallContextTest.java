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

package org.apache.cloudstack.context;

import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.EntityManager;

@RunWith(MockitoJUnitRunner.class)
public class CallContextTest {

    @Mock
    EntityManager entityMgr;
    @Mock
    User user;
    @Mock
    Account account;

    @Before
    public void setUp() {
        CallContext.init(entityMgr);
        CallContext.register(user, account);
    }

    @After
    public void tearDown() throws Exception {
        CallContext.unregisterAll();
    }

    @Test
    public void testGetContextParameter() {
        CallContext currentContext = CallContext.current();

        Assert.assertEquals("There is nothing in the context. It should return null", null, currentContext.getContextParameter("key"));
        Assert.assertTrue("There is nothing in the context. The map should be empty", currentContext.getContextParameters().isEmpty());

        UUID objectUUID = UUID.randomUUID();
        UUID stringUUID = UUID.randomUUID();

        //Case1: when an entry with the object class is present
        currentContext.putContextParameter(User.class, objectUUID);
        Assert.assertEquals("it should return objectUUID: " + objectUUID, objectUUID, currentContext.getContextParameter(User.class));
        Assert.assertEquals("current context map should have exactly one entry", 1, currentContext.getContextParameters().size());

        //Case2: when an entry with the object class name as String is present
        currentContext.putContextParameter(Account.class.toString(), stringUUID);
        //object is put with key as Account.class.toString but get with key as Account.class
        Assert.assertEquals("it should return stringUUID: " + stringUUID, stringUUID, currentContext.getContextParameter(Account.class));
        Assert.assertEquals("current context map should have exactly two entries", 2, currentContext.getContextParameters().size());

        //Case3: when an entry with both object class and object class name as String is present
        //put an entry of account class object in the context
        currentContext.putContextParameter(Account.class, objectUUID);
        //since both object and string a present in the current context, it should return object value
        Assert.assertEquals("it should return objectUUID: " + objectUUID, objectUUID, currentContext.getContextParameter(Account.class));
        Assert.assertEquals("current context map should have exactly three entries", 3, currentContext.getContextParameters().size());
    }


    @Test
    public void isCallingAccountRootAdminReturnsTrueWhenAccountIsRootAdminAccountServiceNotAvailable() {
        Mockito.when(account.getType()).thenReturn(Account.Type.ADMIN);

        CallContext context = CallContext.current();
        Assert.assertTrue(context.isCallingAccountRootAdmin());
    }

    @Test
    public void isCallingAccountRootAdminReturnsFalseWhenAccountIsNotRootAdminAccountServiceNotAvailable() {
        Mockito.when(account.getType()).thenReturn(Account.Type.NORMAL);

        CallContext context = CallContext.current();
        Assert.assertFalse(context.isCallingAccountRootAdmin());
        Assert.assertFalse(context.isCallingAccountRootAdmin());
    }

    @Test
    public void isCallingAccountRootAdminTrueWhenAccountServiceAvailable() {
        try (MockedStatic<ComponentContext> componentContextMockedStatic = Mockito.mockStatic(ComponentContext.class)) {
            AccountService accountService = Mockito.mock(AccountService.class);
            Mockito.when(accountService.isRootAdmin(account)).thenReturn(true);
            componentContextMockedStatic.when(() -> ComponentContext.getDelegateComponentOfType(AccountService.class)).thenReturn(accountService);
            CallContext context = CallContext.current();
            Assert.assertTrue(context.isCallingAccountRootAdmin());
            // Verify isRootAdmin was called only once
            Assert.assertTrue(context.isCallingAccountRootAdmin());
            componentContextMockedStatic.verify(() -> ComponentContext.getDelegateComponentOfType(AccountService.class));
        }
    }

    @Test
    public void isCallingAccountRootAdminFalseWhenAccountServiceAvailable() {
        try (MockedStatic<ComponentContext> componentContextMockedStatic = Mockito.mockStatic(ComponentContext.class)) {
            AccountService accountService = Mockito.mock(AccountService.class);
            Mockito.when(accountService.isRootAdmin(account)).thenReturn(false);
            componentContextMockedStatic.when(() -> ComponentContext.getDelegateComponentOfType(AccountService.class)).thenReturn(accountService);
            CallContext context = CallContext.current();
            Assert.assertFalse(context.isCallingAccountRootAdmin());
            // Verify isRootAdmin was called only once
            Assert.assertFalse(context.isCallingAccountRootAdmin());
            componentContextMockedStatic.verify(() -> ComponentContext.getDelegateComponentOfType(AccountService.class));
        }
    }

}
