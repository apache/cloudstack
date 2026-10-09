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
package com.cloud.hypervisor.kvm.resource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.to.NicTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.network.Networks;

@RunWith(MockitoJUnitRunner.class)
public class BridgeVifDriverTest {

    private static final String BRIDGE_NAME = "cloudbr1";

    @Spy
    @InjectMocks
    private BridgeVifDriver driver = new BridgeVifDriver();

    @Test
    public void isBroadcastTypeVlanOrVxlan() {
        final NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Native);
        Assert.assertFalse(driver.isBroadcastTypeVlanOrVxlan(null));
        Assert.assertFalse(driver.isBroadcastTypeVlanOrVxlan(nic));
        // Test VLAN
        nic.setBroadcastType(Networks.BroadcastDomainType.Vlan);
        Assert.assertTrue(driver.isBroadcastTypeVlanOrVxlan(nic));
        // Test VXLAN
        nic.setBroadcastType(Networks.BroadcastDomainType.Vxlan);
        Assert.assertTrue(driver.isBroadcastTypeVlanOrVxlan(nic));
    }

    @Test
    public void isValidProtocolAndVnetId() {
        Assert.assertFalse(driver.isValidProtocolAndVnetId(null, null));
        Assert.assertFalse(driver.isValidProtocolAndVnetId("123", null));
        Assert.assertFalse(driver.isValidProtocolAndVnetId(null, "vlan"));
        Assert.assertFalse(driver.isValidProtocolAndVnetId("untagged", "vxlan"));
        Assert.assertTrue(driver.isValidProtocolAndVnetId("123", "vlan"));
        Assert.assertTrue(driver.isValidProtocolAndVnetId("456", "vxlan"));
    }

    @Test
    public void createStorageVnetBridgeIfNeededReturnsStorageBrNameWhenBroadcastTypeIsNotStorageButValidValues() throws InternalErrorException {
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Storage);
        int vlan = 123;
        String newBridge = "br-" + vlan;
        nic.setBroadcastUri(Networks.BroadcastDomainType.Storage.toUri(vlan));
        Mockito.doReturn(newBridge).when(driver).createVnetBr(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        String result = driver.createStorageVnetBridgeIfNeeded(nic, "trafficLabel", BRIDGE_NAME);
        Assert.assertEquals(newBridge, result);
    }

    @Test
    public void createStorageVnetBridgeIfNeededReturnsStorageBrNameWhenBroadcastTypeIsNotStorage() throws InternalErrorException {
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Vlan);
        String result = driver.createStorageVnetBridgeIfNeeded(nic, "trafficLabel", BRIDGE_NAME);
        Assert.assertEquals(BRIDGE_NAME, result);
    }

    @Test
    public void createStorageVnetBridgeIfNeededReturnsStorageBrNameWhenBroadcastUriIsNull() throws InternalErrorException {
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Storage);
        String result = driver.createStorageVnetBridgeIfNeeded(nic,  "trafficLabel", BRIDGE_NAME);
        Assert.assertEquals(BRIDGE_NAME, result);
    }

    @Test
    public void createStorageVnetBridgeIfNeededCreatesVnetBridgeWhenUntaggedVlan() throws InternalErrorException, URISyntaxException {
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Storage);
        nic.setBroadcastUri(new URI(Networks.BroadcastDomainType.Storage.scheme() + "://untagged"));
        String result = driver.createStorageVnetBridgeIfNeeded(nic, "trafficLabel", BRIDGE_NAME);
        Assert.assertEquals(BRIDGE_NAME, result);
    }

    @Test
    public void isSystemTrafficLabelReturnsTrueForMatchingVlanBridge() {
        Set<String> labels = new HashSet<>(Arrays.asList("cloudbr0", "brbond0-100"));
        Assert.assertTrue(driver.isSystemTrafficLabel("brbond0-100", labels));
    }

    @Test
    public void isSystemTrafficLabelReturnsTrueForMatchingVxlanBridge() {
        Set<String> labels = Collections.singleton("brvx-1000");
        Assert.assertTrue(driver.isSystemTrafficLabel("brvx-1000", labels));
    }

    @Test
    public void isSystemTrafficLabelReturnsFalseForNonMatchingBridge() {
        Set<String> labels = Collections.singleton("brbond0-100");
        Assert.assertFalse(driver.isSystemTrafficLabel("brbond0-200", labels));
    }

    @Test
    public void isSystemTrafficLabelReturnsFalseWhenBridgeOrLabelsAreNullOrBlank() {
        Set<String> labels = Collections.singleton("brbond0-100");
        Assert.assertFalse(driver.isSystemTrafficLabel("brbond0-100", null));
        Assert.assertFalse(driver.isSystemTrafficLabel("brbond0-100", Collections.emptySet()));
        Assert.assertFalse(driver.isSystemTrafficLabel(null, labels));
        Assert.assertFalse(driver.isSystemTrafficLabel("", labels));
        Assert.assertFalse(driver.isSystemTrafficLabel("  ", labels));
    }
}
