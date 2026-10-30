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
package com.cloud.agent.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.dao.NicDao;

@RunWith(MockitoJUnitRunner.class)
public class VlanTrunkMigrationHelperTest {

    private static final long VM_ID = 1L;
    private static final long DEST_HOST_ID = 2L;

    private HostDetailsDao hostDetailsDao;
    private NicDao nicDao;
    private NetworkDao networkDao;
    private VlanTrunkMigrationHelper.CommandSender commandSender;
    private Logger logger;
    private VirtualMachineTO vmTO;

    @Before
    public void setUp() {
        hostDetailsDao = mock(HostDetailsDao.class);
        nicDao = mock(NicDao.class);
        networkDao = mock(NetworkDao.class);
        commandSender = mock(VlanTrunkMigrationHelper.CommandSender.class);
        logger = mock(Logger.class);
        vmTO = mock(VirtualMachineTO.class);
        when(vmTO.getId()).thenReturn(VM_ID);
    }

    private MigrateCommand newMigrateCommand() {
        return new MigrateCommand("i-2-3-VM", "10.0.0.2", false, vmTO, false);
    }

    private NicVO vlanNic(boolean multiNetwork) {
        NicVO nic = mock(NicVO.class);
        when(nic.getBroadcastUri()).thenReturn(Networks.BroadcastDomainType.Vlan.toUri(123));
        when(nic.getMultiNetwork()).thenReturn(multiNetwork);
        return nic;
    }

    // -- populateVlanTrunkMigrationDetails --

    @Test
    public void populateVlanTrunkMigrationDetailsSetsNicBridgeMappingWhenPresent() {
        MigrateCommand migrateCommand = newMigrateCommand();
        PrepareForMigrationAnswer answer = mock(PrepareForMigrationAnswer.class);
        Map<String, String> mapping = Collections.singletonMap("02:00:00:00:00:01", "cloudbr1");
        when(answer.getNicBridgeMapping()).thenReturn(mapping);

        VlanTrunkMigrationHelper.populateVlanTrunkMigrationDetails(migrateCommand, answer, hostDetailsDao, DEST_HOST_ID);

        assertEquals(mapping, migrateCommand.getNicBridgeMapping());
    }

    @Test
    public void populateVlanTrunkMigrationDetailsLeavesNicBridgeMappingNullWhenEmpty() {
        MigrateCommand migrateCommand = newMigrateCommand();
        PrepareForMigrationAnswer answer = mock(PrepareForMigrationAnswer.class);
        when(answer.getNicBridgeMapping()).thenReturn(Collections.emptyMap());

        VlanTrunkMigrationHelper.populateVlanTrunkMigrationDetails(migrateCommand, answer, hostDetailsDao, DEST_HOST_ID);

        assertTrue(migrateCommand.getNicBridgeMapping().isEmpty());
    }

    @Test
    public void populateVlanTrunkMigrationDetailsParsesDestHostDetails() {
        MigrateCommand migrateCommand = newMigrateCommand();
        PrepareForMigrationAnswer answer = mock(PrepareForMigrationAnswer.class);
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED))
                .thenReturn(new DetailVO(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED, "true"));
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_TRUNK_XML_SUPPORTED))
                .thenReturn(new DetailVO(DEST_HOST_ID, Host.HOST_VLAN_TRUNK_XML_SUPPORTED, "false"));

        VlanTrunkMigrationHelper.populateVlanTrunkMigrationDetails(migrateCommand, answer, hostDetailsDao, DEST_HOST_ID);

        assertTrue(migrateCommand.getDestVlanFilteringEnabled());
        assertFalse(migrateCommand.getDestVlanTrunkXmlSupported());
    }

    @Test
    public void populateVlanTrunkMigrationDetailsLeavesFieldsNullWhenNoHostDetails() {
        MigrateCommand migrateCommand = newMigrateCommand();
        PrepareForMigrationAnswer answer = mock(PrepareForMigrationAnswer.class);

        VlanTrunkMigrationHelper.populateVlanTrunkMigrationDetails(migrateCommand, answer, hostDetailsDao, DEST_HOST_ID);

        assertNull(migrateCommand.getDestVlanFilteringEnabled());
        assertNull(migrateCommand.getDestVlanTrunkXmlSupported());
    }

    // -- vmNeedsPostMigrationVlanTrunkMembership --

    @Test
    public void vmNeedsPostMigrationVlanTrunkMembershipFalseWhenDestSupportsTrunkXml() {
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_TRUNK_XML_SUPPORTED))
                .thenReturn(new DetailVO(DEST_HOST_ID, Host.HOST_VLAN_TRUNK_XML_SUPPORTED, "true"));

        assertFalse(VlanTrunkMigrationHelper.vmNeedsPostMigrationVlanTrunkMembership(VM_ID, DEST_HOST_ID, nicDao, hostDetailsDao));
        verify(nicDao, never()).listByVmId(VM_ID);
    }

    @Test
    public void vmNeedsPostMigrationVlanTrunkMembershipTrueForTrunkNicOnManualFallbackHostEvenWithoutDestFiltering() {
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_TRUNK_XML_SUPPORTED)).thenReturn(null);
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED))
                .thenReturn(new DetailVO(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED, "false"));
        List<NicVO> nics = Collections.singletonList(vlanNic(true));
        when(nicDao.listByVmId(VM_ID)).thenReturn(nics);

        assertTrue(VlanTrunkMigrationHelper.vmNeedsPostMigrationVlanTrunkMembership(VM_ID, DEST_HOST_ID, nicDao, hostDetailsDao));
    }

    @Test
    public void vmNeedsPostMigrationVlanTrunkMembershipTrueForOrdinaryVlanNicWhenDestFilteringEnabled() {
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_TRUNK_XML_SUPPORTED)).thenReturn(null);
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED))
                .thenReturn(new DetailVO(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED, "true"));
        List<NicVO> nics = Collections.singletonList(vlanNic(false));
        when(nicDao.listByVmId(VM_ID)).thenReturn(nics);

        assertTrue(VlanTrunkMigrationHelper.vmNeedsPostMigrationVlanTrunkMembership(VM_ID, DEST_HOST_ID, nicDao, hostDetailsDao));
    }

    @Test
    public void vmNeedsPostMigrationVlanTrunkMembershipFalseForOrdinaryVlanNicWhenDestNotFiltering() {
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_TRUNK_XML_SUPPORTED)).thenReturn(null);
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED)).thenReturn(null);
        List<NicVO> nics = Collections.singletonList(vlanNic(false));
        when(nicDao.listByVmId(VM_ID)).thenReturn(nics);

        assertFalse(VlanTrunkMigrationHelper.vmNeedsPostMigrationVlanTrunkMembership(VM_ID, DEST_HOST_ID, nicDao, hostDetailsDao));
    }

    @Test
    public void vmNeedsPostMigrationVlanTrunkMembershipIgnoresNicsWithoutBroadcastUri() {
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_TRUNK_XML_SUPPORTED)).thenReturn(null);
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED))
                .thenReturn(new DetailVO(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED, "true"));
        NicVO nic = mock(NicVO.class);
        when(nic.getBroadcastUri()).thenReturn(null);
        when(nicDao.listByVmId(VM_ID)).thenReturn(Collections.singletonList(nic));

        assertFalse(VlanTrunkMigrationHelper.vmNeedsPostMigrationVlanTrunkMembership(VM_ID, DEST_HOST_ID, nicDao, hostDetailsDao));
    }

    @Test
    public void vmNeedsPostMigrationVlanTrunkMembershipIgnoresNonVlanBroadcastType() {
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_TRUNK_XML_SUPPORTED)).thenReturn(null);
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED))
                .thenReturn(new DetailVO(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED, "true"));
        NicVO nic = mock(NicVO.class);
        URI vxlanUri = Networks.BroadcastDomainType.Vxlan.toUri(555);
        when(nic.getBroadcastUri()).thenReturn(vxlanUri);
        when(nicDao.listByVmId(VM_ID)).thenReturn(Collections.singletonList(nic));

        assertFalse(VlanTrunkMigrationHelper.vmNeedsPostMigrationVlanTrunkMembership(VM_ID, DEST_HOST_ID, nicDao, hostDetailsDao));
    }

    // -- sendPostMigrationVlanTrunkMembershipIfNeeded --

    @Test
    public void sendPostMigrationVlanTrunkMembershipIfNeededNoOpWhenNotNeeded() throws Exception {
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_TRUNK_XML_SUPPORTED))
                .thenReturn(new DetailVO(DEST_HOST_ID, Host.HOST_VLAN_TRUNK_XML_SUPPORTED, "true"));

        VlanTrunkMigrationHelper.sendPostMigrationVlanTrunkMembershipIfNeeded(vmTO, DEST_HOST_ID, nicDao, hostDetailsDao, commandSender, logger);

        verify(commandSender, never()).send(any(), any());
    }

    @Test
    public void sendPostMigrationVlanTrunkMembershipIfNeededSendsAndLogsNothingOnSuccess() throws Exception {
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_TRUNK_XML_SUPPORTED)).thenReturn(null);
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED))
                .thenReturn(new DetailVO(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED, "false"));
        List<NicVO> trunkNics = Collections.singletonList(vlanNic(true));
        when(nicDao.listByVmId(VM_ID)).thenReturn(trunkNics);
        Answer successAnswer = mock(Answer.class);
        when(successAnswer.getResult()).thenReturn(true);
        when(commandSender.send(eq(DEST_HOST_ID), any(PostMigrationCommand.class))).thenReturn(successAnswer);

        VlanTrunkMigrationHelper.sendPostMigrationVlanTrunkMembershipIfNeeded(vmTO, DEST_HOST_ID, nicDao, hostDetailsDao, commandSender, logger);

        verify(commandSender, times(1)).send(eq(DEST_HOST_ID), any(PostMigrationCommand.class));
        verify(logger, never()).warn(any(String.class), any(Object.class), any(Object.class), any(Object.class));
    }

    @Test
    public void sendPostMigrationVlanTrunkMembershipIfNeededWarnsOnNullAnswer() throws Exception {
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_TRUNK_XML_SUPPORTED)).thenReturn(null);
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED))
                .thenReturn(new DetailVO(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED, "false"));
        List<NicVO> trunkNics = Collections.singletonList(vlanNic(true));
        when(nicDao.listByVmId(VM_ID)).thenReturn(trunkNics);
        when(commandSender.send(eq(DEST_HOST_ID), any(PostMigrationCommand.class))).thenReturn(null);

        VlanTrunkMigrationHelper.sendPostMigrationVlanTrunkMembershipIfNeeded(vmTO, DEST_HOST_ID, nicDao, hostDetailsDao, commandSender, logger);

        verify(logger, times(1)).warn(any(String.class), eq(vmTO), eq(DEST_HOST_ID), any());
    }

    @Test
    public void sendPostMigrationVlanTrunkMembershipIfNeededWarnsOnFailedAnswer() throws Exception {
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_TRUNK_XML_SUPPORTED)).thenReturn(null);
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED))
                .thenReturn(new DetailVO(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED, "false"));
        List<NicVO> trunkNics = Collections.singletonList(vlanNic(true));
        when(nicDao.listByVmId(VM_ID)).thenReturn(trunkNics);
        Answer failedAnswer = mock(Answer.class);
        when(failedAnswer.getResult()).thenReturn(false);
        when(failedAnswer.getDetails()).thenReturn("bridge vlan add failed");
        when(commandSender.send(eq(DEST_HOST_ID), any(PostMigrationCommand.class))).thenReturn(failedAnswer);

        VlanTrunkMigrationHelper.sendPostMigrationVlanTrunkMembershipIfNeeded(vmTO, DEST_HOST_ID, nicDao, hostDetailsDao, commandSender, logger);

        verify(logger, times(1)).warn(any(String.class), eq(vmTO), eq(DEST_HOST_ID), eq("bridge vlan add failed"));
    }

    @Test
    public void sendPostMigrationVlanTrunkMembershipIfNeededWarnsOnSendException() throws Exception {
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_TRUNK_XML_SUPPORTED)).thenReturn(null);
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED))
                .thenReturn(new DetailVO(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED, "false"));
        List<NicVO> trunkNics = Collections.singletonList(vlanNic(true));
        when(nicDao.listByVmId(VM_ID)).thenReturn(trunkNics);
        when(commandSender.send(eq(DEST_HOST_ID), any(PostMigrationCommand.class)))
                .thenThrow(new AgentUnavailableException("agent down", DEST_HOST_ID));

        VlanTrunkMigrationHelper.sendPostMigrationVlanTrunkMembershipIfNeeded(vmTO, DEST_HOST_ID, nicDao, hostDetailsDao, commandSender, logger);

        verify(logger, times(1)).warn(any(String.class), eq(vmTO), eq(DEST_HOST_ID), any(String.class), any(AgentUnavailableException.class));
    }

    // -- getVlanFilteringUnsuitableReason --

    private NicVO nicOnVlan(int vlan, boolean multiNetwork) {
        return nicOnVlan(vlan, multiNetwork, Networks.TrafficType.Guest);
    }

    private NicVO nicOnVlan(int vlan, boolean multiNetwork, Networks.TrafficType trafficType) {
        URI broadcastUri = Networks.BroadcastDomainType.Vlan.toUri(vlan);
        NicVO nic = mock(NicVO.class);
        when(nic.getBroadcastUri()).thenReturn(broadcastUri);
        when(nic.getMultiNetwork()).thenReturn(multiNetwork);
        if (!multiNetwork) {
            lenient().when(nic.getNetworkId()).thenReturn(5L);
            NetworkVO network = mock(NetworkVO.class);
            lenient().when(network.getTrafficType()).thenReturn(trafficType);
            lenient().when(networkDao.findById(5L)).thenReturn(network);
        }
        return nic;
    }

    private void givenHostVlanFiltering(boolean enabled) {
        when(hostDetailsDao.findDetail(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED))
                .thenReturn(new DetailVO(DEST_HOST_ID, Host.HOST_VLAN_FILTERING_ENABLED, Boolean.toString(enabled)));
    }

    @Test
    public void getVlanFilteringUnsuitableReasonRejectsVlanOneNicOnFilteringHost() {
        List<NicVO> nics = Collections.singletonList(nicOnVlan(1, false));
        when(nicDao.listByVmId(VM_ID)).thenReturn(nics);
        givenHostVlanFiltering(true);

        assertTrue(VlanTrunkMigrationHelper.getVlanFilteringUnsuitableReason(VM_ID, DEST_HOST_ID, nicDao, networkDao, hostDetailsDao).contains("VLAN 1"));
    }

    @Test
    public void getVlanFilteringUnsuitableReasonAcceptsVlanOneNicOnLegacyHost() {
        List<NicVO> nics = Collections.singletonList(nicOnVlan(1, false));
        when(nicDao.listByVmId(VM_ID)).thenReturn(nics);
        givenHostVlanFiltering(false);

        assertNull(VlanTrunkMigrationHelper.getVlanFilteringUnsuitableReason(VM_ID, DEST_HOST_ID, nicDao, networkDao, hostDetailsDao));
    }

    @Test
    public void getVlanFilteringUnsuitableReasonAcceptsOtherVlanNicOnFilteringHost() {
        List<NicVO> nics = Collections.singletonList(nicOnVlan(100, false));
        when(nicDao.listByVmId(VM_ID)).thenReturn(nics);

        assertNull(VlanTrunkMigrationHelper.getVlanFilteringUnsuitableReason(VM_ID, DEST_HOST_ID, nicDao, networkDao, hostDetailsDao));
    }

    @Test
    public void getVlanFilteringUnsuitableReasonRejectsTrunkNicOnLegacyHost() {
        List<NicVO> nics = Collections.singletonList(nicOnVlan(100, true));
        when(nicDao.listByVmId(VM_ID)).thenReturn(nics);
        givenHostVlanFiltering(false);

        assertTrue(VlanTrunkMigrationHelper.getVlanFilteringUnsuitableReason(VM_ID, DEST_HOST_ID, nicDao, networkDao, hostDetailsDao).contains("trunk"));
    }

    @Test
    public void getVlanFilteringUnsuitableReasonUsesNetworkVlanForStoppedNicWithoutBroadcastUri() {
        NicVO nic = mock(NicVO.class);
        when(nic.getNetworkId()).thenReturn(5L);
        when(nic.getBroadcastUri()).thenReturn(null);
        List<NicVO> nics = Collections.singletonList(nic);
        when(nicDao.listByVmId(VM_ID)).thenReturn(nics);
        NetworkVO network = mock(NetworkVO.class);
        URI vlanOne = Networks.BroadcastDomainType.Vlan.toUri(1);
        when(network.getBroadcastUri()).thenReturn(vlanOne);
        when(network.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        when(networkDao.findById(5L)).thenReturn(network);
        givenHostVlanFiltering(true);

        assertTrue(VlanTrunkMigrationHelper.getVlanFilteringUnsuitableReason(VM_ID, DEST_HOST_ID, nicDao, networkDao, hostDetailsDao).contains("VLAN 1"));
    }

    @Test
    public void getNativeVlanUnsuitableReasonRejectsVlanOneNetworkOnFilteringHostOnly() {
        Network network = mock(Network.class);
        URI vlanOne = Networks.BroadcastDomainType.Vlan.toUri(1);
        when(network.getBroadcastUri()).thenReturn(vlanOne);
        when(network.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        givenHostVlanFiltering(true);
        assertTrue(VlanTrunkMigrationHelper.getNativeVlanUnsuitableReason(network, DEST_HOST_ID, hostDetailsDao).contains("VLAN 1"));

        givenHostVlanFiltering(false);
        assertNull(VlanTrunkMigrationHelper.getNativeVlanUnsuitableReason(network, DEST_HOST_ID, hostDetailsDao));
    }

    @Test
    public void getVlanFilteringUnsuitableReasonIgnoresVlanOnePublicNic() {
        List<NicVO> nics = Collections.singletonList(nicOnVlan(1, false, Networks.TrafficType.Public));
        when(nicDao.listByVmId(VM_ID)).thenReturn(nics);

        assertNull(VlanTrunkMigrationHelper.getVlanFilteringUnsuitableReason(VM_ID, DEST_HOST_ID, nicDao, networkDao, hostDetailsDao));
    }

    @Test
    public void getNativeVlanUnsuitableReasonIgnoresVlanOneNonGuestNetwork() {
        Network network = mock(Network.class);
        when(network.getTrafficType()).thenReturn(Networks.TrafficType.Public);

        assertNull(VlanTrunkMigrationHelper.getNativeVlanUnsuitableReason(network, DEST_HOST_ID, hostDetailsDao));
    }

    @Test
    public void hasNativeVlanGuestNicIsTrueOnlyForGuestVlanOne() {
        assertTrue(VlanTrunkMigrationHelper.hasNativeVlanGuestNic(Collections.singletonList(nicOnVlan(1, false)), networkDao));
        assertFalse(VlanTrunkMigrationHelper.hasNativeVlanGuestNic(Collections.singletonList(nicOnVlan(100, false)), networkDao));
    }

    @Test
    public void isNativeVlanGuestNetworkIsTrueOnlyForGuestVlanOne() {
        Network guestVlanOne = mock(Network.class);
        when(guestVlanOne.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        when(guestVlanOne.getBroadcastUri()).thenReturn(Networks.BroadcastDomainType.Vlan.toUri(1));
        Network guestOtherVlan = mock(Network.class);
        when(guestOtherVlan.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        when(guestOtherVlan.getBroadcastUri()).thenReturn(Networks.BroadcastDomainType.Vlan.toUri(100));

        assertTrue(VlanTrunkMigrationHelper.isNativeVlanGuestNetwork(guestVlanOne));
        assertFalse(VlanTrunkMigrationHelper.isNativeVlanGuestNetwork(guestOtherVlan));
    }

    @Test
    public void hasTrunkNicDetectsMultiNetworkNic() {
        assertTrue(VlanTrunkMigrationHelper.hasTrunkNic(Collections.singletonList(nicOnVlan(100, true))));
        assertFalse(VlanTrunkMigrationHelper.hasTrunkNic(Collections.singletonList(nicOnVlan(100, false))));
    }

    @Test
    public void getVlanFilteringUnsuitableReasonDoesNotLookUpNetworksOrHostForNicsOnOtherVlans() {
        List<NicVO> nics = Collections.singletonList(nicOnVlan(100, false));
        when(nicDao.listByVmId(VM_ID)).thenReturn(nics);

        assertNull(VlanTrunkMigrationHelper.getVlanFilteringUnsuitableReason(VM_ID, DEST_HOST_ID, nicDao, networkDao, hostDetailsDao));
        verify(networkDao, never()).findById(any(Long.class));
        verify(hostDetailsDao, never()).findDetail(any(Long.class), any(String.class));
    }
}
