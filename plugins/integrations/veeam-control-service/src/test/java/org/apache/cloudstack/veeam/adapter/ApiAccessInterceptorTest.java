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

package org.apache.cloudstack.veeam.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.user.zone.ListZonesCmd;
import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.Pair;

public class ApiAccessInterceptorTest {

    private static final long BASE_USER_ID = 100L;
    private static final long BASE_ACCOUNT_ID = 200L;
    private static final long SERVICE_USER_ID = 300L;
    private static final long SERVICE_ACCOUNT_ID = 400L;

    private final ApiAccessInterceptor interceptor = new ApiAccessInterceptor();
    private final AccountManager accountManager = mock(AccountManager.class);

    @Before
    public void setUp() {
        interceptor.accountManager = accountManager;
        CallContext.unregisterAll();
    }

    @After
    public void tearDown() {
        CallContext.unregisterAll();
    }

    @Test
    public void testInvokePassesThroughWhenTargetIsNull() throws Throwable {
        final MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getThis()).thenReturn(null);
        when(invocation.proceed()).thenReturn("ok");

        final Object result = interceptor.invoke(invocation);

        assertEquals("ok", result);
        verify(invocation).proceed();
        verifyNoInteractions(accountManager);
    }

    @Test
    public void testInvokePassesThroughWhenMethodHasNoApiAccessAnnotation() throws Throwable {
        final TestServerAdapter adapter = new TestServerAdapter(serviceUserAccount());
        final MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getThis()).thenReturn(adapter);
        when(invocation.getMethod()).thenReturn(TestServerAdapter.class.getMethod("noApiAccess"));
        when(invocation.proceed()).thenReturn("done");

        final Object result = interceptor.invoke(invocation);

        assertEquals("done", result);
        verify(invocation).proceed();
        verifyNoInteractions(accountManager);
    }

    @Test
    public void testInvokeChecksApiAccessForDirectlyAnnotatedMethodAndRestoresCallContext() throws Throwable {
        registerBaseContext();

        final TestServerAdapter adapter = new TestServerAdapter(serviceUserAccount());
        final MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getThis()).thenReturn(adapter);
        when(invocation.getMethod()).thenReturn(TestServerAdapter.class.getMethod("classAnnotated"));
        when(invocation.proceed()).thenAnswer(i -> {
            assertEquals(SERVICE_USER_ID, CallContext.current().getCallingUserId());
            assertEquals(SERVICE_ACCOUNT_ID, CallContext.current().getCallingAccountId());
            return "secured";
        });

        final Object result = interceptor.invoke(invocation);

        assertEquals("secured", result);
        verify(accountManager).checkApiAccess(adapter.getServiceAccount().second(),
                BaseCmd.getCommandNameByClass(ListZonesCmd.class));
        assertEquals(BASE_USER_ID, CallContext.current().getCallingUserId());
        assertEquals(BASE_ACCOUNT_ID, CallContext.current().getCallingAccountId());
    }

    @Test
    public void testInvokeFindsAnnotationOnImplementationWhenInterfaceMethodIsUnannotated() throws Throwable {
        registerBaseContext();

        final TestServerAdapter adapter = new TestServerAdapter(serviceUserAccount());
        final Method interfaceMethod = ApiContract.class.getMethod("implAnnotatedThroughInterface");
        final MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getThis()).thenReturn(adapter);
        when(invocation.getMethod()).thenReturn(interfaceMethod);
        when(invocation.proceed()).thenReturn("ok");

        final Object result = interceptor.invoke(invocation);

        assertEquals("ok", result);
        verify(accountManager).checkApiAccess(adapter.getServiceAccount().second(),
                BaseCmd.getCommandNameByClass(ListZonesCmd.class));
        assertEquals(BASE_USER_ID, CallContext.current().getCallingUserId());
        assertEquals(BASE_ACCOUNT_ID, CallContext.current().getCallingAccountId());
    }

    @Test
    public void testInvokeUnregistersServiceContextWhenProceedThrows() throws Throwable {
        registerBaseContext();

        final TestServerAdapter adapter = new TestServerAdapter(serviceUserAccount());
        final MethodInvocation invocation = mock(MethodInvocation.class);
        final RuntimeException expected = new RuntimeException("boom");
        when(invocation.getThis()).thenReturn(adapter);
        when(invocation.getMethod()).thenReturn(TestServerAdapter.class.getMethod("classAnnotated"));
        when(invocation.proceed()).thenThrow(expected);

        try {
            interceptor.invoke(invocation);
        } catch (RuntimeException e) {
            assertSame(expected, e);
        }

        verify(accountManager).checkApiAccess(adapter.getServiceAccount().second(),
                BaseCmd.getCommandNameByClass(ListZonesCmd.class));
        assertEquals(BASE_USER_ID, CallContext.current().getCallingUserId());
        assertEquals(BASE_ACCOUNT_ID, CallContext.current().getCallingAccountId());
    }

    private static void registerBaseContext() {
        final User baseUser = mock(User.class);
        final Account baseAccount = mock(Account.class);
        when(baseUser.getId()).thenReturn(BASE_USER_ID);
        when(baseAccount.getId()).thenReturn(BASE_ACCOUNT_ID);
        CallContext.register(baseUser, baseAccount);
    }

    private static Pair<User, Account> serviceUserAccount() {
        final User serviceUser = mock(User.class);
        final Account serviceAccount = mock(Account.class);
        when(serviceUser.getId()).thenReturn(SERVICE_USER_ID);
        when(serviceAccount.getId()).thenReturn(SERVICE_ACCOUNT_ID);
        return new Pair<>(serviceUser, serviceAccount);
    }

    private interface ApiContract {
        String implAnnotatedThroughInterface();
    }

    private static class TestServerAdapter extends ServerAdapter implements ApiContract {
        private final Pair<User, Account> serviceAccount;

        private TestServerAdapter(final Pair<User, Account> serviceAccount) {
            this.serviceAccount = serviceAccount;
        }

        @Override
        public Pair<User, Account> getServiceAccount() {
            return serviceAccount;
        }

        @ApiAccess(command = ListZonesCmd.class)
        public String classAnnotated() {
            return "classAnnotated";
        }

        @Override
        @ApiAccess(command = ListZonesCmd.class)
        public String implAnnotatedThroughInterface() {
            return "implAnnotatedThroughInterface";
        }

        public String noApiAccess() {
            return "noApiAccess";
        }
    }
}
