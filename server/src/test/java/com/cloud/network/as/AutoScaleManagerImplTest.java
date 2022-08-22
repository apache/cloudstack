// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.network.as;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.as.dao.AutoScalePolicyDao;
import com.cloud.network.as.dao.ConditionDao;
import com.cloud.network.as.dao.CounterDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import org.apache.cloudstack.api.ApiCmdTestUtil;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.admin.autoscale.CreateCounterCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateAutoScalePolicyCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateConditionCmd;
import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AutoScaleManagerImplTest {

    final static String INVALID = "invalid";

    @Spy
    @InjectMocks
    AutoScaleManagerImpl autoScaleManagerImplMock;

    @Mock
    CounterDao _counterDao;

    @Mock
    CounterVO counterMock;

    @Mock
    ConditionDao _conditionDao;

    @Mock
    ConditionVO conditionMock;

    @Mock
    AutoScalePolicyDao _asPolicyDao;

    @Mock
    AutoScalePolicyVO asPolicyMock;

    @Mock
    AccountManager _accountMgr;

    @Mock
    AccountDao _accountDao;

    @Mock
    AccountVO accountMock;

    AccountVO account;
    UserVO user;

    @Before
    public void setUp() {

        account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        account.setId(2L);
        user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone",
                UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);

        when(_counterDao.persist(any(CounterVO.class))).thenReturn(counterMock);
        when(_counterDao.findById(anyLong())).thenReturn(counterMock);
        when(_conditionDao.persist(any(ConditionVO.class))).thenReturn(conditionMock);

        doNothing().when(autoScaleManagerImplMock).checkCallerAccess(nullable(String.class), nullable(Long.class));

        when(_asPolicyDao.persist(any(AutoScalePolicyVO.class))).thenReturn(asPolicyMock);
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }

    @Test
    public void testCreateCounterCmd() throws IllegalArgumentException, IllegalAccessException {
        CreateCounterCmd cmd = new CreateCounterCmd();
        ApiCmdTestUtil.set(cmd, ApiConstants.NAME, "test-name");
        ApiCmdTestUtil.set(cmd, ApiConstants.PROVIDER, "VirtualRouter");
        ApiCmdTestUtil.set(cmd, ApiConstants.SOURCE, "virtualrouter");
        ApiCmdTestUtil.set(cmd, ApiConstants.VALUE, "test-value");

        autoScaleManagerImplMock.createCounter(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateCounterCmdWithInvalidSource() throws IllegalArgumentException, IllegalAccessException {
        CreateCounterCmd cmd = new CreateCounterCmd();
        ApiCmdTestUtil.set(cmd, ApiConstants.NAME, "test-name");
        ApiCmdTestUtil.set(cmd, ApiConstants.PROVIDER, "VirtualRouter");
        ApiCmdTestUtil.set(cmd, ApiConstants.SOURCE, INVALID);
        ApiCmdTestUtil.set(cmd, ApiConstants.VALUE, "test-value");

        autoScaleManagerImplMock.createCounter(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateCounterCmdWithInvalidProvider() throws IllegalArgumentException, IllegalAccessException {
        CreateCounterCmd cmd = new CreateCounterCmd();
        ApiCmdTestUtil.set(cmd, ApiConstants.NAME, "test-name");
        ApiCmdTestUtil.set(cmd, ApiConstants.PROVIDER, INVALID);
        ApiCmdTestUtil.set(cmd, ApiConstants.SOURCE, "virtualrouter");
        ApiCmdTestUtil.set(cmd, ApiConstants.VALUE, "test-value");

        autoScaleManagerImplMock.createCounter(cmd);
    }

    private void seCommandField(BaseCmd cmd, String fieldName, Object value) throws IllegalAccessException {
        Field field = null;
        try {
            field = cmd.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException exception) {
            System.out.println(String.format("Cannot find field %s in command %s", fieldName, cmd.getClass().getName()));
        }
        if (field == null) {
            try {
                field = cmd.getClass().getSuperclass().getDeclaredField(fieldName);
            } catch (NoSuchFieldException exception) {
                System.out.println(String.format("Cannot find field %s in command %s", fieldName, cmd.getClass().getSuperclass().getName()));
                return;
            }
        }
        field.setAccessible(true);
        field.set(cmd, value);
    }

    @Test
    public void testCreateConditionCmd() throws IllegalArgumentException, IllegalAccessException {
        CreateConditionCmd cmd = new CreateConditionCmd() {
            @Override
            public long getEntityOwnerId() {
                return 2;
            }
        };

        seCommandField(cmd, "counterId", 1L);
        seCommandField(cmd, "relationalOperator", "LT");
        seCommandField(cmd, "threshold", 100L);

        autoScaleManagerImplMock.createCondition(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateConditionCmdWithInvalidOperator() throws IllegalArgumentException, IllegalAccessException {
        CreateConditionCmd cmd = new CreateConditionCmd();

        seCommandField(cmd, "counterId", 1L);
        seCommandField(cmd, "relationalOperator", INVALID);
        seCommandField(cmd, "threshold", 100L);

        autoScaleManagerImplMock.createCondition(cmd);
    }

    @Test
    public void testCreateAutoScalePolicyCmd() throws IllegalArgumentException, IllegalAccessException {
        CreateAutoScalePolicyCmd cmd = new CreateAutoScalePolicyCmd() {
            @Override
            public long getEntityOwnerId() {
                return 2;
            }

            @Override
            public long getDomainId() {
                return 1L;
            }

            @Override
            public long getAccountId() {
                return 2L;
            }
        };

        seCommandField(cmd, "action", "ScaleUp");
        seCommandField(cmd, "duration", 300);

        autoScaleManagerImplMock.createAutoScalePolicy(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateAutoScalePolicyCmdWithInvalidAction() throws IllegalArgumentException, IllegalAccessException {
        CreateAutoScalePolicyCmd cmd = new CreateAutoScalePolicyCmd() {
            @Override
            public long getEntityOwnerId() {
                return 2;
            }
        };

        seCommandField(cmd, "action", INVALID);
        seCommandField(cmd, "duration", 300);

        autoScaleManagerImplMock.createAutoScalePolicy(cmd);
    }
}