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
package com.cloud.hypervisor.kvm.dpdk;

import com.cloud.utils.script.Script;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;


import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class DpdkDriverTest {

    private static final int dpdkPortNumber = 7;

    private DpdkDriver driver = new DpdkDriverImpl();

    private Map<String, String> extraConfig;

    private MockedStatic<Script> scriptMockedStatic;

    private AutoCloseable closeable;

    @Before
    public void initMocks() {
        closeable = MockitoAnnotations.openMocks(this);
        scriptMockedStatic = Mockito.mockStatic(Script.class);
        Mockito.when(Script.runSimpleBashScript(ArgumentMatchers.anyString())).thenReturn(null);
        extraConfig = new HashMap<>();
    }

    @After
    public void tearDown() throws Exception {
        scriptMockedStatic.close();
        closeable.close();
    }

    @Test
    public void testGetDpdkLatestPortNumberUsedNoDpdkPorts() {
        Assert.assertEquals(0, driver.getDpdkLatestPortNumberUsed());
    }

    @Test
    public void testGetDpdkLatestPortNumberUsedExistingDpdkPorts() {
        Mockito.when(Script.runSimpleBashScript(ArgumentMatchers.anyString())).
                thenReturn(DpdkDriverImpl.DPDK_PORT_PREFIX + String.valueOf(dpdkPortNumber));
        Assert.assertEquals(dpdkPortNumber, driver.getDpdkLatestPortNumberUsed());
    }

    @Test
    public void testGetNextDpdkPortNoDpdkPorts() {
        Mockito.when(Script.runSimpleBashScript(ArgumentMatchers.anyString())).
                thenReturn(null);
        String expectedPortName = DpdkDriverImpl.DPDK_PORT_PREFIX + String.valueOf(1);
        Assert.assertEquals(expectedPortName, driver.getNextDpdkPort());
    }

    @Test
    public void testGetNextDpdkPortExistingDpdkPorts() {
        Mockito.when(Script.runSimpleBashScript(ArgumentMatchers.anyString())).
                thenReturn(DpdkDriverImpl.DPDK_PORT_PREFIX + String.valueOf(dpdkPortNumber));
        String expectedPortName = DpdkDriverImpl.DPDK_PORT_PREFIX + String.valueOf(dpdkPortNumber + 1);
        Assert.assertEquals(expectedPortName, driver.getNextDpdkPort());
    }

    @Test
    public void testGetGuestInterfacesModeFromDpdkVhostUserModeClientDpdk() {
        String guestMode = driver.getGuestInterfacesModeFromDpdkVhostUserMode(DpdkHelper.VHostUserMode.CLIENT);
        Assert.assertEquals("server", guestMode);
    }

    @Test
    public void testGetGuestInterfacesModeFromDpdkVhostUserModeServerDpdk() {
        String guestMode = driver.getGuestInterfacesModeFromDpdkVhostUserMode(DpdkHelper.VHostUserMode.SERVER);
        Assert.assertEquals("client", guestMode);
    }

    @Test
    public void testGetDpdkvHostUserModeServerExtraConfig() {
        extraConfig.put(DpdkHelper.DPDK_VHOST_USER_MODE, DpdkHelper.VHostUserMode.SERVER.toString());
        DpdkHelper.VHostUserMode dpdKvHostUserMode = driver.getDpdkvHostUserMode(extraConfig);
        Assert.assertEquals(DpdkHelper.VHostUserMode.SERVER, dpdKvHostUserMode);
    }

    @Test
    public void testGetDpdkvHostUserModeServerClientExtraConfig() {
        extraConfig.put(DpdkHelper.DPDK_VHOST_USER_MODE, DpdkHelper.VHostUserMode.CLIENT.toString());
        DpdkHelper.VHostUserMode dpdKvHostUserMode = driver.getDpdkvHostUserMode(extraConfig);
        Assert.assertEquals(DpdkHelper.VHostUserMode.CLIENT, dpdKvHostUserMode);
    }

    @Test
    public void testGetDpdkvHostUserModeServerEmptyExtraConfig() {
        DpdkHelper.VHostUserMode dpdKvHostUserMode = driver.getDpdkvHostUserMode(extraConfig);
        Assert.assertEquals(DpdkHelper.VHostUserMode.SERVER, dpdKvHostUserMode);
    }
}
