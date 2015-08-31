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

package com.cloud.consoleproxy;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.utils.db.GlobalLock;
import com.cloud.vm.ConsoleProxyVO;

public class ConsoleProxyManagerTest {

    private static final Logger s_logger = Logger.getLogger(ConsoleProxyManagerTest.class);

    @Mock
    GlobalLock globalLock;
    @Mock
    ConsoleProxyVO proxyVO;
    @Mock
    ConsoleProxyManagerImpl cpvmManager;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(cpvmManager, "_allocProxyLock", globalLock);
        Mockito.doCallRealMethod().when(cpvmManager).expandPool(Mockito.anyLong(), Mockito.anyObject());
    }

    @Test
    public void testNewCPVMCreation() throws Exception {
        s_logger.info("Running test for new CPVM creation");

        // No existing CPVM
        Mockito.when(cpvmManager.assignProxyFromStoppedPool(Mockito.anyLong())).thenReturn(null);
        // Allocate a new one
        Mockito.when(globalLock.lock(Mockito.anyInt())).thenReturn(true);
        Mockito.when(globalLock.unlock()).thenReturn(true);
        Mockito.when(cpvmManager.startNew(Mockito.anyLong())).thenReturn(proxyVO);
        // Start CPVM
        Mockito.when(cpvmManager.startProxy(Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(proxyVO);

        cpvmManager.expandPool(new Long(1), new Object());
    }

    @Test
    public void testExistingCPVMStart() throws Exception {
        s_logger.info("Running test for existing CPVM start");

        // CPVM already exists
        Mockito.when(cpvmManager.assignProxyFromStoppedPool(Mockito.anyLong())).thenReturn(proxyVO);
        // Start CPVM
        Mockito.when(cpvmManager.startProxy(Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(proxyVO);

        cpvmManager.expandPool(new Long(1), new Object());
    }

    @Test
    public void testExisingCPVMStartFailure() throws Exception {
        s_logger.info("Running test for existing CPVM start failure");

        // CPVM already exists
        Mockito.when(cpvmManager.assignProxyFromStoppedPool(Mockito.anyLong())).thenReturn(proxyVO);
        // Start CPVM
        Mockito.when(cpvmManager.startProxy(Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(null);
        // Destroy existing CPVM, so that a new one is created subsequently
        Mockito.when(cpvmManager.destroyProxy(Mockito.anyLong())).thenReturn(true);

        cpvmManager.expandPool(new Long(1), new Object());
    }
}
