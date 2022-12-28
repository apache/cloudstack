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

package com.cloud.hypervisor.kvm.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.utils.linux.MemStat;
import org.junit.Before;
import org.junit.Test;

import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource.BridgeType;
import com.cloud.network.Networks.TrafficType;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {MemStat.class, AgentPropertiesFileHandler.class})
public class LibvirtVifDriverTest {
    private LibvirtComputingResource res;

    private Map<TrafficType, VifDriver> assertions;

    final String LibVirtVifDriver = "libvirt.vif.driver";
    final String FakeVifDriverClassName = "com.cloud.hypervisor.kvm.resource.FakeVifDriver";
    final String NonExistentVifDriverClassName = "com.cloud.hypervisor.kvm.resource.NonExistentVifDriver";

    private VifDriver fakeVifDriver, bridgeVifDriver, ovsVifDriver;

    final String memInfo = "MemTotal:        5830236 kB\n" +
            "MemFree:          156752 kB\n" +
            "Buffers:          326836 kB\n" +
            "Cached:          2606764 kB\n" +
            "SwapCached:            0 kB\n" +
            "Active:          4260808 kB\n" +
            "Inactive:         949392 kB\n";
    @Before
    public void setUp() throws Exception {
        Scanner scanner = new Scanner(memInfo);
        PowerMockito.whenNew(Scanner.class).withAnyArguments().thenReturn(scanner);
        // Use a spy because we only want to override getVifDriverClass
        LibvirtComputingResource resReal = new LibvirtComputingResource();
        res = spy(resReal);

        try {
            bridgeVifDriver = (VifDriver)Class.forName(LibvirtComputingResource.DEFAULT_BRIDGE_VIF_DRIVER_CLASS_NAME).newInstance();
            ovsVifDriver = (VifDriver)Class.forName(LibvirtComputingResource.DEFAULT_OVS_VIF_DRIVER_CLASS_NAME).newInstance();

            // Instantiating bridge vif driver again as the fake vif driver
            // is good enough, as this is a separate instance
            fakeVifDriver = (VifDriver)Class.forName(LibvirtComputingResource.DEFAULT_BRIDGE_VIF_DRIVER_CLASS_NAME).newInstance();

            doReturn(bridgeVifDriver).when(res).getVifDriverClass(eq(LibvirtComputingResource.DEFAULT_BRIDGE_VIF_DRIVER_CLASS_NAME), anyMap());
            doReturn(ovsVifDriver).when(res).getVifDriverClass(eq(LibvirtComputingResource.DEFAULT_OVS_VIF_DRIVER_CLASS_NAME), anyMap());
            doReturn(fakeVifDriver).when(res).getVifDriverClass(eq(FakeVifDriverClassName), anyMap());

        } catch (final ConfigurationException ex) {
            fail("Unexpected ConfigurationException while configuring VIF drivers: " + ex.getMessage());
        } catch (final Exception ex) {
            fail("Unexpected Exception while configuring VIF drivers");
        }

        assertions = new HashMap<TrafficType, VifDriver>();
    }

    // Helper function
    // Configure LibvirtComputingResource using params
    private void configure(Map<String, Object> params) throws ConfigurationException {
        res.configureVifDrivers(params);
    }

    // Helper function
    private void checkAssertions() {
        // Check the defined assertions
        for (Map.Entry<TrafficType, VifDriver> assertion : assertions.entrySet()) {
            assertEquals(res.getVifDriver(assertion.getKey()), assertion.getValue());
        }
    }

    // Helper when all answers should be the same
    private void checkAllSame(VifDriver vifDriver) throws ConfigurationException {

        for (TrafficType trafficType : TrafficType.values()) {
            assertions.put(trafficType, vifDriver);
        }

        checkAssertions();
    }

    @Test
    public void testDefaults() throws ConfigurationException {
        // If no special vif driver settings, all traffic types should
        // map to the default vif driver for the bridge type
        Map<String, Object> params = new HashMap<String, Object>();

        res._bridgeType = BridgeType.NATIVE;
        configure(params);
        checkAllSame(bridgeVifDriver);

        res._bridgeType = BridgeType.OPENVSWITCH;
        configure(params);
        checkAllSame(ovsVifDriver);
    }

    @Test
    public void configureVifDriversTestWhenSetEqualToDefault() throws Exception {
        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.doReturn(LibvirtComputingResource.DEFAULT_BRIDGE_VIF_DRIVER_CLASS_NAME, LibvirtComputingResource.DEFAULT_OVS_VIF_DRIVER_CLASS_NAME).when(AgentPropertiesFileHandler.class, "getPropertyValue", Mockito.eq(AgentProperties.LIBVIRT_VIF_DRIVER));

        Map<String, Object> params = new HashMap<String, Object>();

        // Switch res' bridge type for test purposes
        res._bridgeType = BridgeType.NATIVE;
        configure(params);
        checkAllSame(bridgeVifDriver);

        res._bridgeType = BridgeType.OPENVSWITCH;
        configure(params);
        checkAllSame(ovsVifDriver);

        PowerMockito.verifyStatic(AgentPropertiesFileHandler.class, Mockito.times(2));
        AgentPropertiesFileHandler.getPropertyValue(AgentProperties.LIBVIRT_VIF_DRIVER);
    }

    @Test
    public void configureVifDriversTestWhenSetDifferentFromDefault() throws Exception {
        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.doReturn(LibvirtComputingResource.DEFAULT_OVS_VIF_DRIVER_CLASS_NAME, LibvirtComputingResource.DEFAULT_BRIDGE_VIF_DRIVER_CLASS_NAME).when(AgentPropertiesFileHandler.class, "getPropertyValue", Mockito.eq(AgentProperties.LIBVIRT_VIF_DRIVER));

        // Tests when explicitly set vif driver to OVS when using regular bridges and vice versa
        Map<String, Object> params = new HashMap<String, Object>();

        // Switch res' bridge type for test purposes
        res._bridgeType = BridgeType.NATIVE;
        configure(params);
        checkAllSame(ovsVifDriver);

        res._bridgeType = BridgeType.OPENVSWITCH;
        configure(params);
        checkAllSame(bridgeVifDriver);

        PowerMockito.verifyStatic(AgentPropertiesFileHandler.class, Mockito.times(2));
        AgentPropertiesFileHandler.getPropertyValue(AgentProperties.LIBVIRT_VIF_DRIVER);
    }

    @Test
    public void testOverrideSomeTrafficTypes() throws ConfigurationException {

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(LibVirtVifDriver + "." + "Public", FakeVifDriverClassName);
        params.put(LibVirtVifDriver + "." + "Guest", LibvirtComputingResource.DEFAULT_OVS_VIF_DRIVER_CLASS_NAME);
        res._bridgeType = BridgeType.NATIVE;
        configure(params);

        // Initially, set all traffic types to use default
        for (TrafficType trafficType : TrafficType.values()) {
            assertions.put(trafficType, bridgeVifDriver);
        }

        assertions.put(TrafficType.Public, fakeVifDriver);
        assertions.put(TrafficType.Guest, ovsVifDriver);

        checkAssertions();
    }

    @Test
    public void testBadTrafficType() throws ConfigurationException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(LibVirtVifDriver + "." + "NonExistentTrafficType", FakeVifDriverClassName);
        res._bridgeType = BridgeType.NATIVE;
        configure(params);

        // Set all traffic types to use default, because bad traffic type should be ignored
        for (TrafficType trafficType : TrafficType.values()) {
            assertions.put(trafficType, bridgeVifDriver);
        }

        checkAssertions();
    }

    @Test
    public void testEmptyTrafficType() throws ConfigurationException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(LibVirtVifDriver + ".", FakeVifDriverClassName);
        res._bridgeType = BridgeType.NATIVE;
        configure(params);

        // Set all traffic types to use default, because bad traffic type should be ignored
        for (TrafficType trafficType : TrafficType.values()) {
            assertions.put(trafficType, bridgeVifDriver);
        }

        checkAssertions();
    }

    @Test(expected = ConfigurationException.class)
    public void testBadVifDriverClassName() throws ConfigurationException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(LibVirtVifDriver + "." + "Public", NonExistentVifDriverClassName);
        res._bridgeType = BridgeType.NATIVE;
        configure(params);
    }
}
