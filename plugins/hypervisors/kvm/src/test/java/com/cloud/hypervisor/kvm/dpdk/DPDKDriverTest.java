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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

@PrepareForTest({ Script.class })
@RunWith(PowerMockRunner.class)
public class DPDKDriverTest {

    private static final int dpdkPortNumber = 7;

    private DPDKDriver driver = new DPDKDriverImpl();

    private Map<String, String> extraConfig;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Script.class);
        Mockito.when(Script.runSimpleBashScript(Matchers.anyString())).thenReturn(null);
        extraConfig = new HashMap<>();
    }

    @Test
    public void testGetDpdkLatestPortNumberUsedNoDpdkPorts() {
        Assert.assertEquals(0, driver.getDpdkLatestPortNumberUsed());
    }

    @Test
    public void testGetDpdkLatestPortNumberUsedExistingDpdkPorts() {
        Mockito.when(Script.runSimpleBashScript(Matchers.anyString())).
                thenReturn(DPDKDriverImpl.DPDK_PORT_PREFIX + String.valueOf(dpdkPortNumber));
        Assert.assertEquals(dpdkPortNumber, driver.getDpdkLatestPortNumberUsed());
    }

    @Test
    public void testGetNextDpdkPortNoDpdkPorts() {
        Mockito.when(Script.runSimpleBashScript(Matchers.anyString())).
                thenReturn(null);
        String expectedPortName = DPDKDriverImpl.DPDK_PORT_PREFIX + String.valueOf(1);
        Assert.assertEquals(expectedPortName, driver.getNextDpdkPort());
    }

    @Test
    public void testGetNextDpdkPortExistingDpdkPorts() {
        Mockito.when(Script.runSimpleBashScript(Matchers.anyString())).
                thenReturn(DPDKDriverImpl.DPDK_PORT_PREFIX + String.valueOf(dpdkPortNumber));
        String expectedPortName = DPDKDriverImpl.DPDK_PORT_PREFIX + String.valueOf(dpdkPortNumber + 1);
        Assert.assertEquals(expectedPortName, driver.getNextDpdkPort());
    }

    @Test
    public void testGetGuestInterfacesModeFromDPDKVhostUserModeClientDPDK() {
        String guestMode = driver.getGuestInterfacesModeFromDPDKVhostUserMode(DPDKHelper.VHostUserMode.CLIENT);
        Assert.assertEquals("server", guestMode);
    }

    @Test
    public void testGetGuestInterfacesModeFromDPDKVhostUserModeServerDPDK() {
        String guestMode = driver.getGuestInterfacesModeFromDPDKVhostUserMode(DPDKHelper.VHostUserMode.SERVER);
        Assert.assertEquals("client", guestMode);
    }

    @Test
    public void testGetDPDKvHostUserModeServerExtraConfig() {
        extraConfig.put(DPDKHelper.DPDK_VHOST_USER_MODE, DPDKHelper.VHostUserMode.SERVER.toString());
        DPDKHelper.VHostUserMode dpdKvHostUserMode = driver.getDPDKvHostUserMode(extraConfig);
        Assert.assertEquals(DPDKHelper.VHostUserMode.SERVER, dpdKvHostUserMode);
    }

    @Test
    public void testGetDPDKvHostUserModeServerClientExtraConfig() {
        extraConfig.put(DPDKHelper.DPDK_VHOST_USER_MODE, DPDKHelper.VHostUserMode.CLIENT.toString());
        DPDKHelper.VHostUserMode dpdKvHostUserMode = driver.getDPDKvHostUserMode(extraConfig);
        Assert.assertEquals(DPDKHelper.VHostUserMode.CLIENT, dpdKvHostUserMode);
    }

    @Test
    public void testGetDPDKvHostUserModeServerEmptyExtraConfig() {
        DPDKHelper.VHostUserMode dpdKvHostUserMode = driver.getDPDKvHostUserMode(extraConfig);
        Assert.assertEquals(DPDKHelper.VHostUserMode.SERVER, dpdKvHostUserMode);
    }
}