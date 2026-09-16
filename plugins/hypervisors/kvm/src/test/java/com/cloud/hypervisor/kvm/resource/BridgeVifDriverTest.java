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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.to.NetworkTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.network.Networks;

@RunWith(MockitoJUnitRunner.class)
public class BridgeVifDriverTest {

    private static final String BRIDGE_NAME = "cloudbr1";

    @Mock
    private LibvirtComputingResource libvirtComputingResource;

    @Spy
    @InjectMocks
    private BridgeVifDriver driver = new BridgeVifDriver();

    @Before
    public void setup() throws InternalErrorException {
        driver._libvirtComputingResource = libvirtComputingResource;
        Map<String, String> bridges = new HashMap<>();
        bridges.put("guest", BRIDGE_NAME);
        driver._bridges = bridges;
        Map<String, String> pifs = new HashMap<>();
        pifs.put("private", "eth1");
        pifs.put(BRIDGE_NAME, "eth1");
        pifs.put("customLabel", "eth2");
        driver._pifs = pifs;
        Mockito.lenient().doNothing().when(driver).runBridgeVlanCommand(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

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

    private NicTO buildTrunkNic(int primaryVlan, List<Integer> associatedVlans) {
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Vlan);
        nic.setBroadcastUri(Networks.BroadcastDomainType.Vlan.toUri(primaryVlan));
        nic.setMac("00:11:22:aa:bb:dd");
        nic.setTrunkVlan(true);
        if (associatedVlans != null) {
            List<NetworkTO> associated = new java.util.ArrayList<>();
            for (Integer vlan : associatedVlans) {
                NetworkTO associatedTo = new NetworkTO();
                associatedTo.setBroadcastType(Networks.BroadcastDomainType.Vlan);
                associatedTo.setBroadcastUri(Networks.BroadcastDomainType.Vlan.toUri(vlan));
                associated.add(associatedTo);
            }
            nic.setAssociatedNetworks(associated);
        }
        return nic;
    }

    @Test
    public void plugTrunkVlanNicOnOldLibvirtSkipsXmlAndLeavesManualMembershipToPostAttachHook() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(false);
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);
        NicTO nic = buildTrunkNic(100, Collections.singletonList(200));
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();

        driver.plugTrunkVlanNic(intf, nic, null, null, null, 0);

        Assert.assertFalse(intf.isVlanTrunk());
        Assert.assertEquals(BRIDGE_NAME, intf.getBrName());
    }

    @Test(expected = InternalErrorException.class)
    public void plugTrunkVlanNicFailsWhenVlanFilteringNotEnabled() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(false);
        NicTO nic = buildTrunkNic(100, Collections.singletonList(200));
        driver.plugTrunkVlanNic(new LibvirtVMDef.InterfaceDef(), nic, null, null, null, 0);
    }

    @Test
    public void ensureVlanTrunkMembershipNoOpsWhenHostSupportsTrunkXml() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(true);
        NicTO nic = buildTrunkNic(100, Collections.singletonList(200));
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();
        intf.setDevName("vnet5");

        driver.ensureVlanTrunkMembership(intf, nic);

        Mockito.verify(driver, Mockito.never()).runBridgeVlanCommand(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void ensureVlanTrunkMembershipNoOpsForNonTrunkNic() throws InternalErrorException {
        NicTO nic = new NicTO();
        nic.setTrunkVlan(false);
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();
        intf.setDevName("vnet5");

        driver.ensureVlanTrunkMembership(intf, nic);

        Mockito.verify(driver, Mockito.never()).runBridgeVlanCommand(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void ensureVlanTrunkMembershipAppliesManualBridgeVlanAddOnOldLibvirt() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(false);
        NicTO nic = buildTrunkNic(100, Collections.singletonList(200));
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();
        intf.setDevName("vnet5");

        driver.ensureVlanTrunkMembership(intf, nic);

        Mockito.verify(driver).runBridgeVlanCommand("add", "vnet5", "100");
        Mockito.verify(driver).runBridgeVlanCommand("add", "vnet5", "200");
    }

    @Test(expected = InternalErrorException.class)
    public void ensureVlanTrunkMembershipFailsWhenTapNameUnknown() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(false);
        NicTO nic = buildTrunkNic(100, null);
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();

        driver.ensureVlanTrunkMembership(intf, nic);
    }

    @Test(expected = InternalErrorException.class)
    public void plugTrunkVlanNicFailsForNonVlanPrimaryBroadcastType() throws InternalErrorException {
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Vxlan);
        nic.setTrunkVlan(true);
        driver.plugTrunkVlanNic(new LibvirtVMDef.InterfaceDef(), nic, null, null, null, 0);
    }

    @Test(expected = InternalErrorException.class)
    public void plugTrunkVlanNicFailsForNonVlanAssociatedNetwork() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);
        NicTO nic = buildTrunkNic(100, null);
        NetworkTO badAssociation = new NetworkTO();
        badAssociation.setBroadcastType(Networks.BroadcastDomainType.Vxlan);
        nic.setAssociatedNetworks(Collections.singletonList(badAssociation));
        driver.plugTrunkVlanNic(new LibvirtVMDef.InterfaceDef(), nic, null, null, null, 0);
    }

    @Test
    public void plugTrunkVlanNicBuildsInterfaceWithAllVlanTagsOnGuestBridge() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(true);
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);
        NicTO nic = buildTrunkNic(100, java.util.Arrays.asList(200, 300));
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();

        driver.plugTrunkVlanNic(intf, nic, null, null, null, 0);

        Assert.assertTrue(intf.isVlanTrunk());
        Assert.assertEquals(java.util.Arrays.asList(100, 200, 300), intf.getTrunkVlanTags());
        Assert.assertEquals(BRIDGE_NAME, intf.getBrName());
    }

    @Test
    public void plugTrunkVlanNicFailsClearlyWhenAssociatedNetworkHasNoVlanYet() {
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);
        NicTO nic = buildTrunkNic(100, null);
        NetworkTO notYetImplemented = new NetworkTO();
        notYetImplemented.setBroadcastType(Networks.BroadcastDomainType.Vlan);
        notYetImplemented.setUuid("unimplemented-network-uuid");
        // broadcastUri intentionally left null, matching a network that has never been implemented
        nic.setAssociatedNetworks(Collections.singletonList(notYetImplemented));

        try {
            driver.plugTrunkVlanNic(new LibvirtVMDef.InterfaceDef(), nic, null, null, null, 0);
            Assert.fail("expected InternalErrorException");
        } catch (InternalErrorException e) {
            Assert.assertTrue(e.getMessage().contains("unimplemented-network-uuid"));
        }
    }

    @Test
    public void plugTrunkVlanNicUsesTrafficLabelBridgeWhenPresent() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(true);
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);
        NicTO nic = buildTrunkNic(100, null);
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();

        driver.plugTrunkVlanNic(intf, nic, "customLabel", null, null, 0);

        Assert.assertEquals("customLabel", intf.getBrName());
        Assert.assertEquals(Collections.singletonList(100), intf.getTrunkVlanTags());
    }

    @Test
    public void plugTrunkVlanNicProgramsUplinkForWhicheverBridgeTheNicActuallyUses() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(true);
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);

        driver.plugTrunkVlanNic(new LibvirtVMDef.InterfaceDef(), buildTrunkNic(100, null), null, null, null, 0);
        driver.plugTrunkVlanNic(new LibvirtVMDef.InterfaceDef(), buildTrunkNic(300, null), "customLabel", null, null, 0);

        Mockito.verify(driver).runBridgeVlanCommand("add", "eth1", "2-4094");
        Mockito.verify(driver).runBridgeVlanCommand("add", "eth2", "2-4094");
    }

    @Test
    public void plugTrunkVlanNicOnlyProgramsUplinkOnceForRepeatedNicsOnTheSameBridge() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(true);
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);

        driver.plugTrunkVlanNic(new LibvirtVMDef.InterfaceDef(), buildTrunkNic(100, null), null, null, null, 0);
        driver.plugTrunkVlanNic(new LibvirtVMDef.InterfaceDef(), buildTrunkNic(200, null), null, null, null, 0);

        Mockito.verify(driver, Mockito.times(1)).runBridgeVlanCommand("add", "eth1", "2-4094");
    }

    @Test(expected = InternalErrorException.class)
    public void plugTrunkVlanNicFailsWhenUplinkPifUnknownForBridge() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);
        NicTO nic = buildTrunkNic(100, null);

        driver.plugTrunkVlanNic(new LibvirtVMDef.InterfaceDef(), nic, "unknownLabel", null, null, 0);
    }
}
