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

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.info.ConsoleProxyStatus;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.ConsoleProxyVO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConsoleProxyManagerTest {

    private static final Logger s_logger = Logger.getLogger(ConsoleProxyManagerTest.class);

    @Mock
    GlobalLock globalLockMock;
    @Mock
    ConsoleProxyVO consoleProxyVOMock;
    @Mock
    DataCenterDao dataCenterDaoMock;
    @Mock
    NetworkDao networkDaoMock;
    @Mock
    ConsoleProxyManagerImpl consoleProxyManagerImplMock;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(consoleProxyManagerImplMock, "allocProxyLock", globalLockMock);
        ReflectionTestUtils.setField(consoleProxyManagerImplMock, "dataCenterDao", dataCenterDaoMock);
        ReflectionTestUtils.setField(consoleProxyManagerImplMock, "networkDao", networkDaoMock);
        Mockito.doCallRealMethod().when(consoleProxyManagerImplMock).expandPool(Mockito.anyLong(), Mockito.anyObject());
        Mockito.doCallRealMethod().when(consoleProxyManagerImplMock).getDefaultNetworkForCreation(Mockito.any(DataCenter.class));
        Mockito.doCallRealMethod().when(consoleProxyManagerImplMock).getDefaultNetworkForAdvancedZone(Mockito.any(DataCenter.class));
        Mockito.doCallRealMethod().when(consoleProxyManagerImplMock).getDefaultNetworkForBasicZone(Mockito.any(DataCenter.class));
    }

    @Test
    public void testNewCPVMCreation() throws Exception {
        s_logger.info("Running test for new CPVM creation");

        // No existing CPVM
        Mockito.when(consoleProxyManagerImplMock.assignProxyFromStoppedPool(Mockito.anyLong())).thenReturn(null);
        // Allocate a new one
        Mockito.when(globalLockMock.lock(Mockito.anyInt())).thenReturn(true);
        Mockito.when(globalLockMock.unlock()).thenReturn(true);
        Mockito.when(consoleProxyManagerImplMock.startNew(Mockito.anyLong())).thenReturn(consoleProxyVOMock);
        // Start CPVM
        Mockito.when(consoleProxyManagerImplMock.startProxy(Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(consoleProxyVOMock);

        consoleProxyManagerImplMock.expandPool(new Long(1), new Object());
    }

    @Test
    public void testExistingCPVMStart() throws Exception {
        s_logger.info("Running test for existing CPVM start");

        // CPVM already exists
        Mockito.when(consoleProxyManagerImplMock.assignProxyFromStoppedPool(Mockito.anyLong())).thenReturn(consoleProxyVOMock);
        // Start CPVM
        Mockito.when(consoleProxyManagerImplMock.startProxy(Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(consoleProxyVOMock);

        consoleProxyManagerImplMock.expandPool(new Long(1), new Object());
    }

    @Test
    public void testExisingCPVMStartFailure() throws Exception {
        s_logger.info("Running test for existing CPVM start failure");

        // CPVM already exists
        Mockito.when(consoleProxyManagerImplMock.assignProxyFromStoppedPool(Mockito.anyLong())).thenReturn(consoleProxyVOMock);
        // Start CPVM
        Mockito.when(consoleProxyManagerImplMock.startProxy(Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(null);
        // Destroy existing CPVM, so that a new one is created subsequently
        Mockito.when(consoleProxyManagerImplMock.destroyProxy(Mockito.anyLong())).thenReturn(true);

        consoleProxyManagerImplMock.expandPool(new Long(1), new Object());
    }

    @Test
    public void getDefaultNetworkForAdvancedNonSG() {
        DataCenterVO dc = mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Advanced);
        when(dc.isSecurityGroupEnabled()).thenReturn(false);

        when(dataCenterDaoMock.findById(Mockito.anyLong())).thenReturn(dc);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        NetworkVO badNetwork = Mockito.mock(NetworkVO.class);
        when(networkDaoMock.listByZoneAndTrafficType(anyLong(), eq(TrafficType.Public)))
                    .thenReturn(Collections.singletonList(network));

        when(networkDaoMock.listByZoneAndTrafficType(anyLong(), not(eq(TrafficType.Public))))
        .thenReturn(Collections.singletonList(badNetwork));

        when(networkDaoMock.listByZoneSecurityGroup(anyLong()))
                    .thenReturn(Collections.singletonList(badNetwork));

        NetworkVO returnedNetwork = consoleProxyManagerImplMock.getDefaultNetworkForAdvancedZone(dc);

        Assert.assertNotNull(returnedNetwork);
        Assert.assertEquals(network, returnedNetwork);
        Assert.assertNotEquals(badNetwork, returnedNetwork);
    }

    @Test
    public void getDefaultNetworkForAdvancedSG() {
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Advanced);
        when(dc.isSecurityGroupEnabled()).thenReturn(true);

        when(dataCenterDaoMock.findById(Mockito.anyLong())).thenReturn(dc);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        NetworkVO badNetwork = Mockito.mock(NetworkVO.class);
        when(networkDaoMock.listByZoneAndTrafficType(anyLong(), any(TrafficType.class)))
                    .thenReturn(Collections.singletonList(badNetwork));

        when(networkDaoMock.listByZoneSecurityGroup(anyLong()))
                    .thenReturn(Collections.singletonList(network));

        NetworkVO returnedNetwork = consoleProxyManagerImplMock.getDefaultNetworkForAdvancedZone(dc);

        Assert.assertEquals(network, returnedNetwork);
        Assert.assertNotEquals(badNetwork, returnedNetwork);
    }

    @Test
    public void getDefaultNetworkForBasicNonSG() {
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Basic);
        when(dc.isSecurityGroupEnabled()).thenReturn(false);

        when(dataCenterDaoMock.findById(Mockito.anyLong())).thenReturn(dc);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        NetworkVO badNetwork = Mockito.mock(NetworkVO.class);
        when(networkDaoMock.listByZoneAndTrafficType(anyLong(), eq(TrafficType.Guest)))
                    .thenReturn(Collections.singletonList(network));

        when(networkDaoMock.listByZoneAndTrafficType(anyLong(), not(eq(TrafficType.Guest))))
                    .thenReturn(Collections.singletonList(badNetwork));

        NetworkVO returnedNetwork = consoleProxyManagerImplMock.getDefaultNetworkForBasicZone(dc);
        Assert.assertNotNull(returnedNetwork);
        Assert.assertEquals(network, returnedNetwork);
        Assert.assertNotEquals(badNetwork, returnedNetwork);
    }

    @Test
    public void getDefaultNetworkForBasicSG() {
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Basic);
        when(dc.isSecurityGroupEnabled()).thenReturn(true);

        when(dataCenterDaoMock.findById(Mockito.anyLong())).thenReturn(dc);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        NetworkVO badNetwork = Mockito.mock(NetworkVO.class);
        when(networkDaoMock.listByZoneAndTrafficType(anyLong(), eq(TrafficType.Guest)))
                    .thenReturn(Collections.singletonList(network));

        when(networkDaoMock.listByZoneAndTrafficType(anyLong(), not(eq(TrafficType.Guest))))
                    .thenReturn(Collections.singletonList(badNetwork));

        NetworkVO returnedNetwork = consoleProxyManagerImplMock.getDefaultNetworkForBasicZone(dc);

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

        when(dataCenterDaoMock.findById(Mockito.anyLong())).thenReturn(dc);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        NetworkVO badNetwork = Mockito.mock(NetworkVO.class);
        when(networkDaoMock.listByZoneAndTrafficType(anyLong(), eq(TrafficType.Guest)))
                    .thenReturn(Collections.singletonList(network));

        when(networkDaoMock.listByZoneAndTrafficType(anyLong(), not(eq(TrafficType.Guest))))
                    .thenReturn(Collections.singletonList(badNetwork));

        consoleProxyManagerImplMock.getDefaultNetworkForBasicZone(dc);
    }

    @Test(expected=CloudRuntimeException.class)
    public void getDefaultNetworkForAdvancedWrongZoneType() {
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Basic);
        when(dc.isSecurityGroupEnabled()).thenReturn(true);

        when(dataCenterDaoMock.findById(Mockito.anyLong())).thenReturn(dc);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        NetworkVO badNetwork = Mockito.mock(NetworkVO.class);
        when(networkDaoMock.listByZoneAndTrafficType(anyLong(), any(TrafficType.class)))
                    .thenReturn(Collections.singletonList(badNetwork));

        when(networkDaoMock.listByZoneSecurityGroup(anyLong()))
                    .thenReturn(Collections.singletonList(network));

        consoleProxyManagerImplMock.getDefaultNetworkForAdvancedZone(dc);
    }

    @Test
    public void validateParseJsonToConsoleProxyStatusWithValidParamMustReturnValue() {
        ConsoleProxyStatus expectedResult = new ConsoleProxyStatus();

        GsonBuilder gb = new GsonBuilder();
        gb.setVersion(1.3);
        Gson gson = gb.create();

        ConsoleProxyStatus result = new ConsoleProxyManagerImpl().parseJsonToConsoleProxyStatus(gson.toJson(expectedResult));

        Assert.assertArrayEquals(expectedResult.getConnections(), result.getConnections());
    }

    @Test (expected = JsonParseException.class)
    public void validateParseJsonToConsoleProxyStatusWithInvalidParamMustThrowJsonParseException() {
        new ConsoleProxyManagerImpl().parseJsonToConsoleProxyStatus("Invalid format to throw exception");
    }

    @Test
    public void validateParseJsonToConsoleProxyStatusWithNullParamMustReturnNull() {
        ConsoleProxyStatus expectedResult = null;
        ConsoleProxyStatus result = new ConsoleProxyManagerImpl().parseJsonToConsoleProxyStatus(null);
        Assert.assertEquals(expectedResult, result);
    }

    private void verifyScannablePoolsZoneIds(List<Long> expected, Long[] result) {
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.length);
        for (int i = 0; i < expected.size(); ++i) {
            Assert.assertEquals(expected.get(i), result[i]);
        }
    }

    @Test
    public void testGetScannablePools() {
        List<Long> dbZoneIds = new ArrayList<>();
        Mockito.when(dataCenterDaoMock.listEnabledNonEdgeZoneIds()).thenReturn(dbZoneIds);
        ConsoleProxyManagerImpl consoleProxyManager = new ConsoleProxyManagerImpl();
        ReflectionTestUtils.setField(consoleProxyManager, "dataCenterDao", dataCenterDaoMock);
        verifyScannablePoolsZoneIds(dbZoneIds, consoleProxyManager.getScannablePools());
        dbZoneIds = Arrays.asList(2L, 3L);
        Mockito.when(dataCenterDaoMock.listEnabledNonEdgeZoneIds()).thenReturn(dbZoneIds);
        verifyScannablePoolsZoneIds(dbZoneIds, consoleProxyManager.getScannablePools());
    }
}
