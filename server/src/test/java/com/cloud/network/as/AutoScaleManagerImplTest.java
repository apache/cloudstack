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
import com.cloud.network.as.dao.CounterDao;
import org.apache.cloudstack.api.ApiCmdTestUtil;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.autoscale.CreateCounterCmd;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AutoScaleManagerImplTest {

    @Spy
    @InjectMocks
    AutoScaleManagerImpl autoScaleManagerImplMock;

    @Mock
    CounterDao _counterDao;

    @Mock
    CounterVO counterMock;

    @Before
    public void initTest() {
        when(_counterDao.persist(any(CounterVO.class))).thenReturn(counterMock);
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
        ApiCmdTestUtil.set(cmd, ApiConstants.SOURCE, "invalid");
        ApiCmdTestUtil.set(cmd, ApiConstants.VALUE, "test-value");

        autoScaleManagerImplMock.createCounter(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateCounterCmdWithInvalidProvider() throws IllegalArgumentException, IllegalAccessException {
        CreateCounterCmd cmd = new CreateCounterCmd();
        ApiCmdTestUtil.set(cmd, ApiConstants.NAME, "test-name");
        ApiCmdTestUtil.set(cmd, ApiConstants.PROVIDER, "invalid");
        ApiCmdTestUtil.set(cmd, ApiConstants.SOURCE, "virtualrouter");
        ApiCmdTestUtil.set(cmd, ApiConstants.VALUE, "test-value");

        autoScaleManagerImplMock.createCounter(cmd);
    }
}