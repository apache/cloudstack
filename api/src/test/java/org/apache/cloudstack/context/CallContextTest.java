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

import java.util.Map;
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
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

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
    Account account;

    @Mock
    AccountService accountService;

    private MockedStatic<ComponentContext> componentContextMocked;

    @Before
    public void setUp() {
        CallContext.init(entityMgr);
        CallContext.register(Mockito.mock(User.class), account);
    }

    @After
    public void tearDown() throws Exception {
        if (componentContextMocked != null) {
            componentContextMocked.close();
            componentContextMocked = null;
        }
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
    public void testIsCallingAccountRootAdminDelegatesToAccountServiceAndCaches() {
        componentContextMocked = Mockito.mockStatic(ComponentContext.class);
        componentContextMocked.when(() -> ComponentContext.getDelegateComponentOfType(AccountService.class)).thenReturn(accountService);
        Mockito.when(accountService.isRootAdmin(account)).thenReturn(true);

        CallContext currentContext = CallContext.current();
        Assert.assertTrue(currentContext.isCallingAccountRootAdmin());
        Assert.assertTrue(currentContext.isCallingAccountRootAdmin());

        // result is cached after the first delegate call, so isRootAdmin should only be invoked once
        Mockito.verify(accountService, Mockito.times(1)).isRootAdmin(account);
    }

    @Test
    public void testIsCallingAccountRootAdminReturnsFalseWhenAccountServiceSaysSo() {
        componentContextMocked = Mockito.mockStatic(ComponentContext.class);
        componentContextMocked.when(() -> ComponentContext.getDelegateComponentOfType(AccountService.class)).thenReturn(accountService);
        Mockito.when(accountService.isRootAdmin(account)).thenReturn(false);

        Assert.assertFalse(CallContext.current().isCallingAccountRootAdmin());
    }

    @Test
    public void testIsCallingAccountRootAdminFallsBackToAccountTypeWhenNoAccountServiceBean() {
        componentContextMocked = Mockito.mockStatic(ComponentContext.class);
        componentContextMocked.when(() -> ComponentContext.getDelegateComponentOfType(AccountService.class))
                .thenThrow(new NoSuchBeanDefinitionException(AccountService.class));
        Mockito.when(account.getType()).thenReturn(Account.Type.ADMIN);

        Assert.assertTrue(CallContext.current().isCallingAccountRootAdmin());
        Mockito.verify(account, Mockito.atLeastOnce()).getType();
    }

    @Test
    public void testIsCallingAccountRootAdminFallbackReturnsFalseForNonAdminAccountType() {
        componentContextMocked = Mockito.mockStatic(ComponentContext.class);
        componentContextMocked.when(() -> ComponentContext.getDelegateComponentOfType(AccountService.class))
                .thenThrow(new NoSuchBeanDefinitionException(AccountService.class));
        Mockito.when(account.getType()).thenReturn(Account.Type.NORMAL);

        Assert.assertFalse(CallContext.current().isCallingAccountRootAdmin());
    }

    @Test
    public void testIsCallingAccountRootAdminFallbackIsNotCached() {
        componentContextMocked = Mockito.mockStatic(ComponentContext.class);
        componentContextMocked.when(() -> ComponentContext.getDelegateComponentOfType(AccountService.class))
                .thenThrow(new NoSuchBeanDefinitionException(AccountService.class));
        Mockito.when(account.getType()).thenReturn(Account.Type.ADMIN);

        CallContext currentContext = CallContext.current();
        currentContext.isCallingAccountRootAdmin();
        currentContext.isCallingAccountRootAdmin();

        // the fallback path (no AccountService bean) does not memoize its result, so the
        // delegate lookup is retried on every call rather than being cached
        componentContextMocked.verify(() -> ComponentContext.getDelegateComponentOfType(AccountService.class), Mockito.times(2));
    }

    @Test
    public void testIsCallingAccountRootAdminReturnsFalseWhenNoAccountAndNoEntityManager() {
        CallContext.unregisterAll();
        CallContext.init(null);
        // registerPlaceHolderContext() is the only public factory that leaves the account field
        // unset (lazily loaded via s_entityMgr on first access), which is required to reach the
        // "account == null && s_entityMgr == null" short-circuit branch.
        CallContext.registerPlaceHolderContext();

        Assert.assertFalse(CallContext.current().isCallingAccountRootAdmin());
    }

    @Test
    public void testGetPutErrorContextParameter() {
        CallContext currentContext = CallContext.current();

        Assert.assertTrue(currentContext.getErrorContextParameters().isEmpty());

        currentContext.putErrorContextParameter("key1", "value1");
        Assert.assertEquals("value1", currentContext.getErrorContextParameters().get("key1"));

        currentContext.putErrorContextParameters(Map.of("key2", "value2", "key3", "value3"));
        Assert.assertEquals(3, currentContext.getErrorContextParameters().size());
        Assert.assertEquals("value2", currentContext.getErrorContextParameters().get("key2"));

        // putting an empty/null map is a no-op and must not throw
        currentContext.putErrorContextParameters(null);
        currentContext.putErrorContextParameters(Map.of());
        Assert.assertEquals(3, currentContext.getErrorContextParameters().size());
    }

}
