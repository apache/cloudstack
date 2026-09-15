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

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Field;
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
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.cloud.agent.api.to.NetworkTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.network.Networks;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class BridgeVifDriverTest {

    private static final String BRIDGE_NAME = "cloudbr1";
    private static final String MODIFY_BRDR = "/scripts/modifybrdr.sh";
    private static final String MODIFY_MACIP = "/scripts/modifymacip.sh";

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
        Mockito.lenient().doNothing().when(driver).runBridgeVlanCommand(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = BridgeVifDriver.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Feeds the given text to the OutputInterpreter a Script.execute(parser) call passes, the
     * way Script does with the merged stdout/stderr of the real process, and reports success.
     */
    private static Answer<String> scriptOutput(String output) {
        return invocation -> {
            OutputInterpreter parser = invocation.getArgument(0);
            parser.interpret(new BufferedReader(new StringReader(output)));
            return null;
        };
    }

    private static MockedConstruction<Script> mockScriptOutput(String output) {
        return Mockito.mockConstruction(Script.class, (mock, context) ->
                Mockito.lenient().when(mock.execute(Mockito.any(OutputInterpreter.class))).thenAnswer(scriptOutput(output)));
    }

    private static MockedConstruction<Script> mockScriptFailure(String error) {
        return Mockito.mockConstruction(Script.class, (mock, context) ->
                Mockito.lenient().when(mock.execute(Mockito.any(OutputInterpreter.class))).thenReturn(error));
    }

    private NicTO directRoutedNic() throws URISyntaxException {
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Routed);
        nic.setBroadcastUri(new URI("routed://600"));
        nic.setMac("02:00:00:00:00:01");
        nic.setGateway("169.254.0.1");
        nic.setIp6Gateway("fe80::1");
        return nic;
    }

    private void configureDirectRoutedScripts() throws Exception {
        setField(driver, "_modifyBrdrPath", MODIFY_BRDR);
        setField(driver, "_macIpScriptPath", MODIFY_MACIP);
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

        Mockito.verify(driver, Mockito.never()).runBridgeVlanCommand(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void ensureVlanTrunkMembershipNoOpsForNonTrunkNic() throws InternalErrorException {
        NicTO nic = new NicTO();
        nic.setTrunkVlan(false);
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();
        intf.setDevName("vnet5");

        driver.ensureVlanTrunkMembership(intf, nic);

        Mockito.verify(driver, Mockito.never()).runBridgeVlanCommand(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void usesSharedVlanAwareBridgeTrueOnlyForVlanNicOnFilteringReadyHost() {
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Vlan);

        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);
        Assert.assertTrue(driver.usesSharedVlanAwareBridge(nic));

        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(false);
        Assert.assertFalse(driver.usesSharedVlanAwareBridge(nic));

        nic.setBroadcastType(Networks.BroadcastDomainType.Vxlan);
        Assert.assertFalse(driver.usesSharedVlanAwareBridge(nic));
    }

    @Test
    public void usesSharedVlanAwareBridgeFalseForDirectRoutedNicWithVlanBroadcastType() throws URISyntaxException {
        // a SystemVM public NIC on a routed range keeps the Public network's Vlan type; only its URI is routed://
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Vlan);
        nic.setBroadcastUri(new URI("routed://600"));
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);

        Assert.assertFalse(driver.usesSharedVlanAwareBridge(nic));
    }

    @Test
    public void ensureVlanTrunkMembershipNoOpsForDirectRoutedNicOnFilteringReadyHost() throws Exception {
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Vlan);
        nic.setBroadcastUri(new URI("routed://600"));
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();
        intf.setDevName("vnet5");

        driver.ensureVlanTrunkMembership(intf, nic);

        Mockito.verify(driver, Mockito.never()).runBridgeVlanCommand(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void ensureVlanTrunkMembershipSkipsUntaggedOrdinaryNicOnFilteringReadyHost() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Vlan);
        nic.setBroadcastUri(URI.create("vlan://untagged"));
        nic.setTrunkVlan(false);
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();
        intf.setDevName("vnet5");

        Assert.assertFalse(driver.usesSharedVlanAwareBridge(nic));
        driver.ensureVlanTrunkMembership(intf, nic);

        Mockito.verify(driver, Mockito.never()).runBridgeVlanCommand(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void ensureVlanTrunkMembershipAppliesNativeVlanForOrdinaryNicOnFilteringReadyHost() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(false);
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);
        NicTO nic = buildTrunkNic(100, null);
        nic.setTrunkVlan(false);
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();
        intf.setDevName("vnet5");
        Mockito.doReturn(Collections.emptyMap()).when(driver).readCurrentVlanMembership("vnet5");

        driver.ensureVlanTrunkMembership(intf, nic);

        Mockito.verify(driver).runBridgeVlanCommand("add", "vnet5", "100", true);
    }

    @Test
    public void ensureVlanTrunkMembershipNoOpsForOrdinaryNicWhenHostNotFilteringReady() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(false);
        NicTO nic = buildTrunkNic(100, null);
        nic.setTrunkVlan(false);
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();
        intf.setDevName("vnet5");

        driver.ensureVlanTrunkMembership(intf, nic);

        Mockito.verify(driver, Mockito.never()).runBridgeVlanCommand(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void ensureVlanTrunkMembershipAppliesManualBridgeVlanAddOnOldLibvirt() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(false);
        NicTO nic = buildTrunkNic(100, Collections.singletonList(200));
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();
        intf.setDevName("vnet5");
        Mockito.doReturn(Collections.emptyMap()).when(driver).readCurrentVlanMembership("vnet5");

        driver.ensureVlanTrunkMembership(intf, nic);

        Mockito.verify(driver).runBridgeVlanCommand("add", "vnet5", "100", true);
        Mockito.verify(driver).runBridgeVlanCommand("add", "vnet5", "200", false);
    }

    @Test
    public void ensureVlanTrunkMembershipRemovesDefaultVlanOneOnOldLibvirt() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(false);
        NicTO nic = buildTrunkNic(100, Collections.singletonList(200));
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();
        intf.setDevName("vnet5");
        Map<Integer, Boolean> currentMembership = new HashMap<>();
        currentMembership.put(1, true);
        Mockito.doReturn(currentMembership).when(driver).readCurrentVlanMembership("vnet5");

        driver.ensureVlanTrunkMembership(intf, nic);

        Mockito.verify(driver).runBridgeVlanCommand("del", "vnet5", "1", false);
        Mockito.verify(driver).runBridgeVlanCommand("add", "vnet5", "100", true);
        Mockito.verify(driver).runBridgeVlanCommand("add", "vnet5", "200", false);
    }

    @Test
    public void updateVlanTrunkMembershipRestoresOriginalMembershipWhenLaterAddFailsOnOldLibvirt() throws Exception {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(false);
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);
        NicTO nic = buildTrunkNic(100, java.util.Arrays.asList(200, 300));
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();
        intf.setDevName("vnet5");
        Map<Integer, Boolean> original = new java.util.LinkedHashMap<>();
        original.put(100, true);
        Map<Integer, Boolean> afterPartialApply = new java.util.LinkedHashMap<>(original);
        afterPartialApply.put(200, false);
        Mockito.doReturn(original, afterPartialApply).when(driver).readCurrentVlanMembership("vnet5");
        Mockito.doNothing().when(driver).runBridgeVlanCommand("add", "vnet5", "200", false);
        Mockito.doThrow(new InternalErrorException("boom")).when(driver).runBridgeVlanCommand("add", "vnet5", "300", false);
        Mockito.doNothing().when(driver).runBridgeVlanCommand("del", "vnet5", "200", false);

        try {
            driver.updateVlanTrunkMembership(Mockito.mock(org.libvirt.Domain.class), intf, nic);
            Assert.fail("expected InternalErrorException");
        } catch (InternalErrorException expected) {
            Mockito.verify(driver).runBridgeVlanCommand("del", "vnet5", "200", false);
        }
    }

    @Test(expected = InternalErrorException.class)
    public void ensureVlanTrunkMembershipFailsWhenTapNameUnknown() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(false);
        NicTO nic = buildTrunkNic(100, null);
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();

        driver.ensureVlanTrunkMembership(intf, nic);
    }

    @Test
    public void updateVlanTrunkMembershipUpdatesXmlOnModernLibvirt() throws Exception {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(true);
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);
        NicTO nic = buildTrunkNic(100, Collections.singletonList(200));
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();
        intf.setDevName("vnet5");
        org.libvirt.Domain vm = Mockito.mock(org.libvirt.Domain.class);

        driver.updateVlanTrunkMembership(vm, intf, nic);

        Mockito.verify(vm).updateDeviceFlags(Mockito.contains("trunk"),
                Mockito.eq(org.libvirt.Domain.DeviceModifyFlags.LIVE));
    }

    @Test
    public void updateVlanTrunkMembershipAppliesDiffOnOldLibvirt() throws Exception {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(false);
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);
        NicTO nic = buildTrunkNic(100, Collections.singletonList(200));
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();
        intf.setDevName("vnet5");
        org.libvirt.Domain vm = Mockito.mock(org.libvirt.Domain.class);
        Map<Integer, Boolean> currentMembership = new HashMap<>();
        currentMembership.put(100, true);
        currentMembership.put(999, false);
        Mockito.doReturn(currentMembership).when(driver).readCurrentVlanMembership("vnet5");

        driver.updateVlanTrunkMembership(vm, intf, nic);

        Mockito.verify(driver).runBridgeVlanCommand("del", "vnet5", "999", false);
        Mockito.verify(driver).runBridgeVlanCommand("add", "vnet5", "200", false);
        Mockito.verify(driver, Mockito.never()).runBridgeVlanCommand("add", "vnet5", "100", true);
        Mockito.verify(vm, Mockito.never()).updateDeviceFlags(Mockito.anyString(), Mockito.anyInt());
    }

    @Test
    public void updateVlanTrunkMembershipChecksUplinkMembershipForTheResolvedPif() throws Exception {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(true);
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);
        Mockito.doReturn(Collections.emptyMap()).when(driver).readCurrentVlanMembership(Mockito.anyString());
        NicTO nic = buildTrunkNic(100, Collections.singletonList(200));
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();
        intf.defBridgeNet(BRIDGE_NAME, null, nic.getMac(), LibvirtVMDef.InterfaceDef.NicModel.VIRTIO);
        intf.setDevName("vnet5");
        org.libvirt.Domain vm = Mockito.mock(org.libvirt.Domain.class);

        driver.updateVlanTrunkMembership(vm, intf, nic);

        // a live associateNetworkToNic/disassociateNetworkFromNic reaches the uplink the same way a plug does
        Mockito.verify(driver).readCurrentVlanMembership("eth1");
    }

    @Test(expected = InternalErrorException.class)
    public void updateVlanTrunkMembershipFailsForNonTrunkNic() throws Exception {
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Vlan);
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(false);
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();
        org.libvirt.Domain vm = Mockito.mock(org.libvirt.Domain.class);

        driver.updateVlanTrunkMembership(vm, intf, nic);
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
        Assert.assertEquals(Integer.valueOf(100), intf.getNativeVlanTag());
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
        Assert.assertEquals(Integer.valueOf(100), intf.getNativeVlanTag());
    }

    @Test
    public void plugTrunkVlanNicNeverModifiesTheUplinkItself() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(true);
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);
        Mockito.doReturn(Collections.emptyMap()).when(driver).readCurrentVlanMembership(Mockito.anyString());

        driver.plugTrunkVlanNic(new LibvirtVMDef.InterfaceDef(), buildTrunkNic(100, null), null, null, null, 0);
        driver.plugTrunkVlanNic(new LibvirtVMDef.InterfaceDef(), buildTrunkNic(300, null), "customLabel", null, null, 0);

        // the uplink's own VLAN membership is the operator's responsibility, not CloudStack's - plugging a nic
        // never adds anything to eth1/eth2, regardless of whether their current membership already covers the VLAN
        Mockito.verify(driver, Mockito.never()).runBridgeVlanCommand(Mockito.eq("add"), Mockito.eq("eth1"), Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(driver, Mockito.never()).runBridgeVlanCommand(Mockito.eq("add"), Mockito.eq("eth2"), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void plugTrunkVlanNicChecksUplinkMembershipForTheResolvedPif() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(true);
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);
        Mockito.doReturn(Collections.emptyMap()).when(driver).readCurrentVlanMembership(Mockito.anyString());

        driver.plugTrunkVlanNic(new LibvirtVMDef.InterfaceDef(), buildTrunkNic(100, Collections.singletonList(200)), null, null, null, 0);

        Mockito.verify(driver).readCurrentVlanMembership("eth1");
    }

    @Test
    public void plugTrunkVlanNicSucceedsWhenUplinkMembershipCannotBeRead() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(true);
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);
        Mockito.doThrow(new InternalErrorException("bridge command failed")).when(driver).readCurrentVlanMembership(Mockito.anyString());
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();

        // a failure to even read the uplink's current membership is only a missed diagnostic, never a reason to refuse the plug
        driver.plugTrunkVlanNic(intf, buildTrunkNic(100, null), null, null, null, 0);

        Assert.assertEquals(BRIDGE_NAME, intf.getBrName());
    }

    @Test
    public void plugTrunkVlanNicSucceedsWhenUplinkPifUnknownForBridge() throws InternalErrorException {
        Mockito.when(libvirtComputingResource.hostSupportsVlanTrunkXml()).thenReturn(true);
        Mockito.when(libvirtComputingResource.hostSupportsVlanFiltering()).thenReturn(true);
        NicTO nic = buildTrunkNic(100, null);
        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();

        // not being able to even determine the uplink is only a missed diagnostic, never a reason to refuse the plug
        driver.plugTrunkVlanNic(intf, nic, "unknownLabel", null, null, 0);

        Assert.assertEquals("unknownLabel", intf.getBrName());
    }

    @Test
    public void isDirectRoutedNicRecognisesBroadcastType() throws URISyntaxException {
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Routed);
        nic.setBroadcastUri(new URI("routed://600"));
        Assert.assertTrue(BridgeVifDriver.isDirectRoutedNic(nic));
    }

    /**
     * Regression: a SystemVM's public NIC is rebuilt at start with the shared Public network's
     * broadcast domain type (Vlan) — only its own broadcast URI carries routed://. It must still
     * be recognised, or the agent runs modifyvlan.sh on the routed id and skips modifymacip.sh.
     */
    @Test
    public void isDirectRoutedNicRecognisesRoutedUriWithForeignBroadcastType() throws URISyntaxException {
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Vlan);
        nic.setBroadcastUri(new URI("routed://600"));
        Assert.assertTrue(BridgeVifDriver.isDirectRoutedNic(nic));
    }

    @Test
    public void isDirectRoutedNicRejectsOrdinaryNics() throws URISyntaxException {
        Assert.assertFalse(BridgeVifDriver.isDirectRoutedNic(null));
        NicTO nic = new NicTO();
        nic.setBroadcastType(Networks.BroadcastDomainType.Vlan);
        Assert.assertFalse(BridgeVifDriver.isDirectRoutedNic(nic));
        nic.setBroadcastUri(new URI("vlan://600"));
        Assert.assertFalse(BridgeVifDriver.isDirectRoutedNic(nic));
    }

    @Test
    public void lastNonBlankLineTakesTheLastLineWithContent() {
        Assert.assertNull(BridgeVifDriver.lastNonBlankLine(null));
        Assert.assertNull(BridgeVifDriver.lastNonBlankLine(""));
        Assert.assertNull(BridgeVifDriver.lastNonBlankLine(" \n\n  \n"));
        Assert.assertEquals("brdr-600", BridgeVifDriver.lastNonBlankLine("brdr-600\n"));
        Assert.assertEquals("brdr-600", BridgeVifDriver.lastNonBlankLine("sysctl: cannot stat /proc/sys/net/ipv6/conf/brdr-600/disable_ipv6\n  brdr-600  \n\n"));
    }

    @Test
    public void createDirectRoutedBridgeUsesLastLineOfScriptOutput() throws Exception {
        configureDirectRoutedScripts();
        try (MockedConstruction<Script> scripts = mockScriptOutput("sysctl: cannot stat /proc/sys/net/ipv6/conf/brdr-600/disable_ipv6\nbrdr-600\n")) {
            Assert.assertEquals("brdr-600", driver.createDirectRoutedBridge(directRoutedNic()));
            Assert.assertEquals(1, scripts.constructed().size());
            Script script = scripts.constructed().get(0);
            Mockito.verify(script).add("-o", "add");
            Mockito.verify(script).add("-n", "600");
            Mockito.verify(script).add("-4", "169.254.0.1");
            Mockito.verify(script).add("-6", "fe80::1");
        }
    }

    @Test
    public void createDirectRoutedBridgeRejectsOutputThatIsNotAnInterfaceName() throws Exception {
        configureDirectRoutedScripts();
        try (MockedConstruction<Script> ignored = mockScriptOutput("sysctl: cannot stat /proc/sys/net/ipv6/conf/brdr-600/disable_ipv6\n")) {
            driver.createDirectRoutedBridge(directRoutedNic());
            Assert.fail("a diagnostic must not be accepted as the bridge name");
        } catch (InternalErrorException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("cannot stat"));
        }
        try (MockedConstruction<Script> ignored = mockScriptOutput("a-name-that-is-far-too-long-for-an-interface\n")) {
            driver.createDirectRoutedBridge(directRoutedNic());
            Assert.fail("an over-long name must not be accepted as the bridge name");
        } catch (InternalErrorException expected) {
        }
        try (MockedConstruction<Script> ignored = mockScriptOutput("")) {
            driver.createDirectRoutedBridge(directRoutedNic());
            Assert.fail("empty output must not be accepted");
        } catch (InternalErrorException expected) {
        }
    }

    @Test(expected = InternalErrorException.class)
    public void createDirectRoutedBridgeFailsWhenScriptFails() throws Exception {
        configureDirectRoutedScripts();
        try (MockedConstruction<Script> ignored = mockScriptFailure("modifybrdr.sh: command failed: ip link add name brdr-600 type bridge stp_state 0 forward_delay 0")) {
            driver.createDirectRoutedBridge(directRoutedNic());
        }
    }

    @Test
    public void createDirectRoutedBridgeRequiresBothScripts() throws Exception {
        setField(driver, "_modifyBrdrPath", MODIFY_BRDR);
        setField(driver, "_macIpScriptPath", null);
        try {
            driver.createDirectRoutedBridge(directRoutedNic());
            Assert.fail("a missing modifymacip.sh must be fatal for a Direct Routed NIC");
        } catch (InternalErrorException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("modifymacip.sh"));
        }
        setField(driver, "_modifyBrdrPath", null);
        setField(driver, "_macIpScriptPath", MODIFY_MACIP);
        try {
            driver.createDirectRoutedBridge(directRoutedNic());
            Assert.fail("a missing modifybrdr.sh must be fatal for a Direct Routed NIC");
        } catch (InternalErrorException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("modifybrdr.sh"));
        }
    }

    @Test
    public void deleteDirectRoutedBridgeAcceptsOnlyTheThreeVerdicts() throws Exception {
        configureDirectRoutedScripts();
        for (String verdict : new String[] {"notmine", "kept", "deleted"}) {
            try (MockedConstruction<Script> ignored = mockScriptOutput("ip: some diagnostic\n" + verdict + "\n")) {
                Assert.assertTrue(verdict, driver.deleteDirectRoutedBridge("brdr-600"));
            }
        }
        try (MockedConstruction<Script> ignored = mockScriptOutput("RTNETLINK answers: Operation not permitted\n")) {
            Assert.assertFalse(driver.deleteDirectRoutedBridge("brdr-600"));
        }
        try (MockedConstruction<Script> ignored = mockScriptOutput("")) {
            Assert.assertFalse(driver.deleteDirectRoutedBridge("brdr-600"));
        }
        try (MockedConstruction<Script> ignored = mockScriptFailure("modifybrdr.sh: command failed: ip link delete brdr-600 type bridge")) {
            Assert.assertFalse(driver.deleteDirectRoutedBridge("brdr-600"));
        }
    }

    @Test
    public void isDirectRoutedBridgeAcceptsOnlyMine() throws Exception {
        configureDirectRoutedScripts();
        try (MockedConstruction<Script> scripts = mockScriptOutput("mine\n")) {
            Assert.assertTrue(driver.isDirectRoutedBridge("brdr-600"));
            Mockito.verify(scripts.constructed().get(0)).add("-o", "query");
            Mockito.verify(scripts.constructed().get(0)).add("-b", "brdr-600");
        }
        try (MockedConstruction<Script> ignored = mockScriptOutput("notmine\n")) {
            Assert.assertFalse(driver.isDirectRoutedBridge("cloudbr0"));
        }
        try (MockedConstruction<Script> ignored = mockScriptOutput("something else\n")) {
            Assert.assertFalse(driver.isDirectRoutedBridge("brdr-600"));
        }
        try (MockedConstruction<Script> ignored = mockScriptFailure(Script.ERR_TIMEOUT)) {
            Assert.assertFalse(driver.isDirectRoutedBridge("brdr-600"));
        }
        setField(driver, "_modifyBrdrPath", null);
        Assert.assertFalse(driver.isDirectRoutedBridge("brdr-600"));
    }

    /**
     * On unplug of a Direct Routed interface the host route and neighbour entry must go before
     * the bridge they reference may be deleted: query, then modifymacip.sh delete, then
     * modifybrdr.sh delete, and no regular vnet bridge handling.
     */
    @Test
    public void unplugRemovesMacIpEntriesBeforeDeletingDirectRoutedBridge() throws Exception {
        configureDirectRoutedScripts();
        LibvirtVMDef.InterfaceDef iface = new LibvirtVMDef.InterfaceDef();
        iface.defBridgeNet("brdr-600", null, "02:00:00:00:00:01", LibvirtVMDef.InterfaceDef.NicModel.VIRTIO);
        try (MockedConstruction<Script> scripts = Mockito.mockConstruction(Script.class, (mock, context) -> {
            String output = context.getCount() == 1 ? "mine\n" : "deleted\n";
            Mockito.lenient().when(mock.execute(Mockito.any(OutputInterpreter.class))).thenAnswer(scriptOutput(output));
        })) {
            driver.unplug(iface, true);
            List<Script> constructed = scripts.constructed();
            Assert.assertEquals(3, constructed.size());
            Mockito.verify(constructed.get(0)).add("-o", "query");
            Mockito.verify(constructed.get(0)).add("-b", "brdr-600");
            Mockito.verify(constructed.get(1)).add("-o", "delete");
            Mockito.verify(constructed.get(1)).add("-m", "02:00:00:00:00:01");
            Mockito.verify(constructed.get(1)).execute();
            Mockito.verify(constructed.get(2)).add("-o", "delete");
            Mockito.verify(constructed.get(2)).add("-b", "brdr-600");
        }
    }
}
