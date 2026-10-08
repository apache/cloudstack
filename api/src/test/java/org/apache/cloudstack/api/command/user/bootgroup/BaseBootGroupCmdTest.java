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
package org.apache.cloudstack.api.command.user.bootgroup;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupService;
import org.junit.After;
import org.junit.Before;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.cloud.user.Account;
import com.cloud.user.AccountService;

/**
 * Shared setup for all Instance Boot Group command unit tests.
 */
public abstract class BaseBootGroupCmdTest {

    protected static final long ACCOUNT_ID = 42L;
    protected static final long ENTITY_ID = 100L;

    protected InstanceBootGroupService instanceBootGroupService;
    protected AccountService accountService;
    protected Account callingAccount;

    private MockedStatic<CallContext> callContextMock;

    @Before
    public void setUp() {
        instanceBootGroupService = mock(InstanceBootGroupService.class);
        accountService = mock(AccountService.class);

        callingAccount = mock(Account.class);
        when(callingAccount.getId()).thenReturn(ACCOUNT_ID);

        CallContext callContext = mock(CallContext.class);
        when(callContext.getCallingAccount()).thenReturn(callingAccount);

        callContextMock = Mockito.mockStatic(CallContext.class);
        callContextMock.when(CallContext::current).thenReturn(callContext);
    }

    @After
    public void tearDown() {
        callContextMock.close();
    }

    /**
     * Sets a private/inherited field value via reflection.
     */
    protected void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = null;
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (field == null) {
            throw new NoSuchFieldException(fieldName + " not found in hierarchy of " + target.getClass().getName());
        }
        field.setAccessible(true);
        field.set(target, value);
    }
}
