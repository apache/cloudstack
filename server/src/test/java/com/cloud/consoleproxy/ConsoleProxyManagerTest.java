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

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.ConsoleProxyVO;

public class ConsoleProxyManagerTest {

    private static final Logger s_logger = Logger.getLogger(ConsoleProxyManagerTest.class);

    @Mock
    GlobalLock globalLock;
    @Mock
    ConsoleProxyVO proxyVO;
    @Mock
    DataCenterDao _dcDao;
    @Mock
    NetworkDao _networkDao;
    @Mock
    ConsoleProxyManagerImpl cpvmManager;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(cpvmManager, "_allocProxyLock", globalLock);
        ReflectionTestUtils.setField(cpvmManager, "_dcDao", _dcDao);
        ReflectionTestUtils.setField(cpvmManager, "_networkDao", _networkDao);
        Mockito.doCallRealMethod().when(cpvmManager).expandPool(Mockito.anyLong(), Mockito.anyObject());
        Mockito.doCallRealMethod().when(cpvmManager).getDefaultNetworkForCreation(Mockito.any(DataCenter.class));
        Mockito.doCallRealMethod().when(cpvmManager).getDefaultNetworkForAdvancedZone(Mockito.any(DataCenter.class));
        Mockito.doCallRealMethod().when(cpvmManager).getDefaultNetworkForBasicZone(Mockito.any(DataCenter.class));
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

    @Test
    public void getDefaultNetworkForAdvancedNonSG() {
        DataCenterVO dc = mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Advanced);
        when(dc.isSecurityGroupEnabled()).thenReturn(false);

        when(_dcDao.findById(Mockito.anyLong())).thenReturn(dc);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        NetworkVO badNetwork = Mockito.mock(NetworkVO.class);
        when(_networkDao.listByZoneAndTrafficType(anyLong(), eq(TrafficType.Public)))
                    .thenReturn(Collections.singletonList(network));

        when(_networkDao.listByZoneAndTrafficType(anyLong(), not(eq(TrafficType.Public))))
        .thenReturn(Collections.singletonList(badNetwork));

        when(_networkDao.listByZoneSecurityGroup(anyLong()))
                    .thenReturn(Collections.singletonList(badNetwork));

        NetworkVO returnedNetwork = cpvmManager.getDefaultNetworkForAdvancedZone(dc);

        Assert.assertNotNull(returnedNetwork);
        Assert.assertEquals(network, returnedNetwork);
        Assert.assertNotEquals(badNetwork, returnedNetwork);
    }

    @Test
    public void getDefaultNetworkForAdvancedSG() {
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Advanced);
        when(dc.isSecurityGroupEnabled()).thenReturn(true);

        when(_dcDao.findById(Mockito.anyLong())).thenReturn(dc);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        NetworkVO badNetwork = Mockito.mock(NetworkVO.class);
        when(_networkDao.listByZoneAndTrafficType(anyLong(), any(TrafficType.class)))
                    .thenReturn(Collections.singletonList(badNetwork));

        when(_networkDao.listByZoneSecurityGroup(anyLong()))
                    .thenReturn(Collections.singletonList(network));

        NetworkVO returnedNetwork = cpvmManager.getDefaultNetworkForAdvancedZone(dc);

        Assert.assertEquals(network, returnedNetwork);
        Assert.assertNotEquals(badNetwork, returnedNetwork);
    }

    @Test
    public void getDefaultNetworkForBasicNonSG() {
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Basic);
        when(dc.isSecurityGroupEnabled()).thenReturn(false);

        when(_dcDao.findById(Mockito.anyLong())).thenReturn(dc);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        NetworkVO badNetwork = Mockito.mock(NetworkVO.class);
        when(_networkDao.listByZoneAndTrafficType(anyLong(), eq(TrafficType.Guest)))
                    .thenReturn(Collections.singletonList(network));

        when(_networkDao.listByZoneAndTrafficType(anyLong(), not(eq(TrafficType.Guest))))
                    .thenReturn(Collections.singletonList(badNetwork));

        NetworkVO returnedNetwork = cpvmManager.getDefaultNetworkForBasicZone(dc);
        Assert.assertNotNull(returnedNetwork);
        Assert.assertEquals(network, returnedNetwork);
        Assert.assertNotEquals(badNetwork, returnedNetwork);
    }

    @Test
    public void getDefaultNetworkForBasicSG() {
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Basic);
        when(dc.isSecurityGroupEnabled()).thenReturn(true);

        when(_dcDao.findById(Mockito.anyLong())).thenReturn(dc);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        NetworkVO badNetwork = Mockito.mock(NetworkVO.class);
        when(_networkDao.listByZoneAndTrafficType(anyLong(), eq(TrafficType.Guest)))
                    .thenReturn(Collections.singletonList(network));

        when(_networkDao.listByZoneAndTrafficType(anyLong(), not(eq(TrafficType.Guest))))
                    .thenReturn(Collections.singletonList(badNetwork));

        NetworkVO returnedNetwork = cpvmManager.getDefaultNetworkForBasicZone(dc);

        Assert.assertNotNull(returnedNetwork);
        Assert.assertEquals(network, returnedNetwork);
        Assert.assertNotEquals(badNetwork, returnedNetwork);
    }

    //also test invalid input
    @Test(expected=CloudRuntimeException.class)
    public void getDefaultNetworkForBasicSGWrongZoneType() {
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Advanced);
        when(dc.isSecurityGroupEnabled()).thenReturn(true);

        when(_dcDao.findById(Mockito.anyLong())).thenReturn(dc);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        NetworkVO badNetwork = Mockito.mock(NetworkVO.class);
        when(_networkDao.listByZoneAndTrafficType(anyLong(), eq(TrafficType.Guest)))
                    .thenReturn(Collections.singletonList(network));

        when(_networkDao.listByZoneAndTrafficType(anyLong(), not(eq(TrafficType.Guest))))
                    .thenReturn(Collections.singletonList(badNetwork));

        cpvmManager.getDefaultNetworkForBasicZone(dc);
    }

    @Test(expected=CloudRuntimeException.class)
    public void getDefaultNetworkForAdvancedWrongZoneType() {
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Basic);
        when(dc.isSecurityGroupEnabled()).thenReturn(true);

        when(_dcDao.findById(Mockito.anyLong())).thenReturn(dc);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        NetworkVO badNetwork = Mockito.mock(NetworkVO.class);
        when(_networkDao.listByZoneAndTrafficType(anyLong(), any(TrafficType.class)))
                    .thenReturn(Collections.singletonList(badNetwork));

        when(_networkDao.listByZoneSecurityGroup(anyLong()))
                    .thenReturn(Collections.singletonList(network));

        cpvmManager.getDefaultNetworkForAdvancedZone(dc);
    }
}
