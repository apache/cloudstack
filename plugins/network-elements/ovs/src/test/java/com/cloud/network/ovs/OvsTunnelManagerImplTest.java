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

package com.cloud.network.ovs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.junit.Before;
import org.junit.Test;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.OvsVpcPhysicalTopologyConfigCommand;
import com.cloud.agent.api.OvsVpcRoutingPolicyConfigCommand;
import com.cloud.configuration.Config;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeVO;
import com.cloud.network.ovs.dao.OvsTunnelInterfaceDao;
import com.cloud.network.ovs.dao.OvsTunnelInterfaceVO;
import com.cloud.network.ovs.dao.VpcDistributedRouterSeqNoDao;
import com.cloud.network.ovs.dao.VpcDistributedRouterSeqNoVO;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.network.vpc.NetworkACLVO;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class OvsTunnelManagerImplTest {
    private static final long VPC_ID = 7L;
    private static final long SECOND_VPC_ID = 8L;
    private static final long CHANGED_VM_ID = 11L;
    private static final long ACTIVE_VM_ID = 12L;
    private static final long NETWORK_ID = 13L;
    private static final long PHYSICAL_NETWORK_ID = 17L;
    private static final long HOST_ID = 21L;
    private static final long SECOND_HOST_ID = 22L;
    private static final long THIRD_HOST_ID = 23L;
    private static final long ACL_ID = 31L;

    private static final String VPC_UUID = "vpc-uuid";
    private static final String VPC_CIDR = "10.0.0.0/16";
    private static final String NETWORK_UUID = "network-uuid";
    private static final String NETWORK_GATEWAY = "10.0.1.1";
    private static final String NETWORK_CIDR = "10.0.1.0/24";
    private static final String GATEWAY_MAC = "02:00:00:00:00:01";
    private static final String VM_IP = "10.0.1.10";
    private static final String VM_MAC = "02:00:00:00:00:02";
    private static final String GRE_ENDPOINT_IP = "192.0.2.10";
    private static final String SECOND_GRE_ENDPOINT_IP = "192.0.2.11";
    private static final String THIRD_GRE_ENDPOINT_IP = "192.0.2.12";
    private static final String PHYSICAL_NETWORK_LABEL = "cloudbr1";
    private static final String BRIDGE_NAME = "OVS-DR-VPC-Bridge7";

    private OvsTunnelManagerImpl manager;
    private AgentManager agentManager;
    private ConfigurationDao configDao;
    private HostDao hostDao;
    private NetworkDao networkDao;
    private NetworkACLDao networkAclDao;
    private NetworkACLItemDao networkAclItemDao;
    private NicDao nicDao;
    private OvsNetworkTopologyGuru topologyGuru;
    private OvsTunnelInterfaceDao tunnelInterfaceDao;
    private PhysicalNetworkTrafficTypeDao physicalNetworkTrafficTypeDao;
    private VMInstanceDao vmInstanceDao;
    private VpcDao vpcDao;
    private VpcDistributedRouterSeqNoDao sequenceNumberDao;
    private VpcDistributedRouterSeqNoVO sequenceNumber;
    private VpcManager vpcManager;

    @Before
    public void setUp() {
        manager = new OvsTunnelManagerImpl();
        agentManager = mock(AgentManager.class);
        configDao = mock(ConfigurationDao.class);
        hostDao = mock(HostDao.class);
        networkDao = mock(NetworkDao.class);
        networkAclDao = mock(NetworkACLDao.class);
        networkAclItemDao = mock(NetworkACLItemDao.class);
        nicDao = mock(NicDao.class);
        topologyGuru = mock(OvsNetworkTopologyGuru.class);
        tunnelInterfaceDao = mock(OvsTunnelInterfaceDao.class);
        physicalNetworkTrafficTypeDao = mock(PhysicalNetworkTrafficTypeDao.class);
        vmInstanceDao = mock(VMInstanceDao.class);
        vpcDao = mock(VpcDao.class);
        sequenceNumberDao = mock(VpcDistributedRouterSeqNoDao.class);
        vpcManager = mock(VpcManager.class);

        manager._agentMgr = agentManager;
        manager._configDao = configDao;
        manager._hostDao = hostDao;
        manager._networkDao = networkDao;
        manager._networkACLDao = networkAclDao;
        manager._networkACLItemDao = networkAclItemDao;
        manager._nicDao = nicDao;
        manager._ovsNetworkToplogyGuru = topologyGuru;
        manager._tunnelInterfaceDao = tunnelInterfaceDao;
        manager._physNetTTDao = physicalNetworkTrafficTypeDao;
        manager._vmInstanceDao = vmInstanceDao;
        manager._vpcDao = vpcDao;
        manager._vpcDrSeqNoDao = sequenceNumberDao;
        manager._vpcMgr = vpcManager;
    }

    @Test
    public void testIsOvsDistributedRouterVpcReturnsFalseWhenVpcIsMissing() {
        assertVpcRejectedBeforeProviderLookup();
    }

    @Test
    public void testIsOvsDistributedRouterVpcReturnsFalseWhenVpcIsNotDistributed() {
        VpcVO vpc = mock(VpcVO.class);
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        when(vpc.usesDistributedRouter()).thenReturn(false);

        assertVpcRejectedBeforeProviderLookup();
    }

    @Test
    public void testIsOvsDistributedRouterVpcReturnsFalseForNsxDistributedVpc() {
        prepareDistributedVpc(VPC_ID, VPC_UUID, VPC_CIDR, false);

        assertFalse(manager.isOvsDistributedRouterVpc(VPC_ID));
    }

    @Test
    public void testIsOvsDistributedRouterVpcReturnsTrueForOvsConnectivityDistributedVpc() {
        prepareDistributedVpc(VPC_ID, VPC_UUID, VPC_CIDR, true);

        assertTrue(manager.isOvsDistributedRouterVpc(VPC_ID));
    }

    @Test
    public void testPostStateTransitionEventIgnoresNsxDistributedVpc() {
        VMInstanceVO vm = prepareChangedVm(List.of(VPC_ID));
        StateMachine2.Transition<VirtualMachine.State, VirtualMachine.Event> transition =
                prepareSuccessfulStartTransition();
        prepareDistributedVpc(VPC_ID, VPC_UUID, VPC_CIDR, false);

        assertTrue(manager.postStateTransitionEvent(transition, vm, true, null));

        verify(topologyGuru, never()).getVpcSpannedHosts(anyLong());
        verify(vpcManager, never()).getVpcNetworks(anyLong());
    }

    @Test
    public void testPostStateTransitionEventContinuesAfterNonOvsVpc() {
        VmStateChangeContext context = prepareStateChangeContext(List.of(VPC_ID, SECOND_VPC_ID));
        prepareDistributedVpc(VPC_ID, VPC_UUID, VPC_CIDR, false);
        prepareDistributedVpc(SECOND_VPC_ID, "second-vpc-uuid", "10.1.0.0/16", true);

        assertSecondVpcStillProcesses(context);
    }

    @Test
    public void testPostStateTransitionEventContinuesAfterMalformedOvsTopology() {
        VmStateChangeContext context = prepareTwoOvsVpcStateChange();
        NetworkVO malformedNetwork = mock(NetworkVO.class);
        when(malformedNetwork.getState()).thenReturn(Network.State.Implemented);
        when(malformedNetwork.getUuid()).thenReturn(NETWORK_UUID);
        when(malformedNetwork.getBroadcastDomainType()).thenReturn(BroadcastDomainType.NSX);
        when(topologyGuru.getVpcSpannedHosts(VPC_ID)).thenReturn(Collections.emptyList());
        when(topologyGuru.getAllActiveVmsInVpc(VPC_ID)).thenReturn(Collections.emptyList());
        doReturn(List.of(malformedNetwork)).when(vpcManager).getVpcNetworks(VPC_ID);

        assertSecondVpcStillProcesses(context);
    }

    @Test
    public void testPostStateTransitionEventContinuesAfterProviderLookupFailure() {
        VmStateChangeContext context = prepareTwoOvsVpcStateChange();
        when(vpcManager.isProviderSupportServiceInVpc(VPC_ID, Network.Service.Connectivity, Network.Provider.Ovs))
                .thenThrow(new CloudRuntimeException("provider lookup failed"));

        assertSecondVpcStillProcesses(context);
    }

    @Test
    public void testPostStateTransitionEventSendsCompleteTopologyCommandToEveryHost() throws Exception {
        prepareDistributedVpc(VPC_ID, VPC_UUID, VPC_CIDR, true);
        VMInstanceVO changedVm = prepareChangedVm(List.of(VPC_ID));
        StateMachine2.Transition<VirtualMachine.State, VirtualMachine.Event> transition =
                prepareSuccessfulStartTransition();
        NetworkVO network = prepareVswitchNetworkForCurrentVpc("7.123");
        prepareGatewayNic();
        prepareHostGreEndpoints(network);
        prepareActiveVm(network);
        prepareSequenceNumber(VPC_ID);
        List<Long> dispatchedHostIds = new ArrayList<>();
        List<String> topologyPayloads = new ArrayList<>();
        when(agentManager.send(anyLong(), any(OvsVpcPhysicalTopologyConfigCommand.class))).thenAnswer(invocation -> {
            Long hostId = invocation.getArgument(0);
            OvsVpcPhysicalTopologyConfigCommand command = invocation.getArgument(1);
            dispatchedHostIds.add(hostId);
            assertCommandTarget(hostId, command.getHostId(), command.getBridgeName(), command.getSequenceNumber());
            topologyPayloads.add(command.getVpcConfigInJson());
            return prepareHostAnswer(hostId);
        });

        assertTrue(manager.postStateTransitionEvent(transition, changedVm, true, null));

        assertPayloadDispatchedToEveryHost(dispatchedHostIds, topologyPayloads);
        assertCompleteTopologyPayload(topologyPayloads.get(0));
        verify(sequenceNumber).incrTopologyUpdateSequenceNo();
        verify(sequenceNumberDao).update(1L, sequenceNumber);
    }

    @Test
    public void testNetworkAclSubscriberIgnoresNsxDistributedVpc() {
        NetworkVO network = prepareNetworkAclEvent(VPC_ID);
        prepareDistributedVpc(VPC_ID, VPC_UUID, VPC_CIDR, false);

        assertNetworkAclUpdateSkipped(network);
    }

    @Test
    public void testNetworkAclSubscriberHandlesProviderLookupFailure() {
        NetworkVO network = prepareNetworkAclEvent(VPC_ID);
        prepareDistributedVpc(VPC_ID, VPC_UUID, VPC_CIDR, true);
        when(vpcManager.isProviderSupportServiceInVpc(VPC_ID, Network.Service.Connectivity, Network.Provider.Ovs))
                .thenThrow(new CloudRuntimeException("provider lookup failed"));

        assertNetworkAclUpdateSkipped(network);
    }

    @Test
    public void testNetworkAclSubscriberIgnoresNetworkWithoutVpc() {
        NetworkVO network = mock(NetworkVO.class);

        manager.new NetworkAclEventsSubscriber().onPublishMessage("sender", "Network_ACL_Replaced", network);

        verify(vpcDao, never()).findById(anyLong());
        verify(topologyGuru, never()).getVpcSpannedHosts(anyLong());
        verify(vpcManager, never()).getVpcNetworks(anyLong());
    }

    @Test
    public void testNetworkAclSubscriberSendsCompleteRoutingPolicyCommandToEveryHost() throws Exception {
        prepareDistributedVpc(VPC_ID, VPC_UUID, VPC_CIDR, true);
        NetworkVO network = prepareNetworkWithAcl();
        prepareSequenceNumber(VPC_ID);
        List<Long> dispatchedHostIds = new ArrayList<>();
        List<String> policyPayloads = new ArrayList<>();
        when(agentManager.send(anyLong(), any(OvsVpcRoutingPolicyConfigCommand.class))).thenAnswer(invocation -> {
            Long hostId = invocation.getArgument(0);
            OvsVpcRoutingPolicyConfigCommand command = invocation.getArgument(1);
            dispatchedHostIds.add(hostId);
            assertCommandTarget(hostId, command.getHostId(), command.getBridgeName(), command.getSequenceNumber());
            policyPayloads.add(command.getVpcConfigInJson());
            return prepareHostAnswer(hostId);
        });

        manager.new NetworkAclEventsSubscriber().onPublishMessage("sender", "Network_ACL_Replaced", network);

        assertPayloadDispatchedToEveryHost(dispatchedHostIds, policyPayloads);
        assertCompleteRoutingPolicyPayload(policyPayloads.get(0));
        verify(sequenceNumber).incrPolicyUpdateSequenceNo();
        verify(sequenceNumberDao).update(1L, sequenceNumber);
    }

    @Test
    public void testPrepareVpcTopologyUpdateRejectsNonVswitchTier() {
        prepareTopologyNetwork(Network.State.Implemented, BroadcastDomainType.NSX, null);

        assertThrows(CloudRuntimeException.class, () -> manager.prepareVpcTopologyUpdate(VPC_ID));
    }

    @Test
    public void testPrepareVpcTopologyUpdateSkipsEveryInactiveTierState() {
        prepareVpcIdentity();
        NetworkVO allocated = prepareNetworkInState(Network.State.Allocated);
        NetworkVO shutdown = prepareNetworkInState(Network.State.Shutdown);
        NetworkVO destroyed = prepareNetworkInState(Network.State.Destroy);
        prepareTopologyCollections(List.of(allocated, shutdown, destroyed));

        OvsVpcPhysicalTopologyConfigCommand command = manager.prepareVpcTopologyUpdate(VPC_ID);

        assertEquals(0, getVpcJson(command.getVpcConfigInJson()).getAsJsonArray("tiers").size());
        verify(allocated, never()).getBroadcastDomainType();
        verify(shutdown, never()).getBroadcastDomainType();
        verify(destroyed, never()).getBroadcastDomainType();
        verify(nicDao, never()).findByIp4AddressAndNetworkId(any(String.class), anyLong());
    }

    @Test
    public void testPrepareVpcTopologyUpdateIncludesEveryActiveTierState() {
        prepareVpcIdentity();
        NetworkVO setup = prepareVswitchNetwork(101L, "setup-network", "10.0.1.1", "10.0.1.0/24",
                Network.State.Setup, "7.101");
        NetworkVO implementing = prepareVswitchNetwork(102L, "implementing-network", "10.0.2.1", "10.0.2.0/24",
                Network.State.Implementing, "7.102");
        NetworkVO implemented = prepareVswitchNetwork(103L, "implemented-network", "10.0.3.1", "10.0.3.0/24",
                Network.State.Implemented, "7.103");
        prepareGatewayNic(101L, "10.0.1.1", "02:00:00:00:01:01");
        prepareGatewayNic(102L, "10.0.2.1", "02:00:00:00:01:02");
        prepareGatewayNic(103L, "10.0.3.1", "02:00:00:00:01:03");
        prepareTopologyCollections(List.of(setup, implementing, implemented));

        OvsVpcPhysicalTopologyConfigCommand command = manager.prepareVpcTopologyUpdate(VPC_ID);

        JsonArray tiers = getVpcJson(command.getVpcConfigInJson()).getAsJsonArray("tiers");
        assertEquals(3, tiers.size());
        assertEquals(101L, tiers.get(0).getAsJsonObject().get("grekey").getAsLong());
        assertEquals(102L, tiers.get(1).getAsJsonObject().get("grekey").getAsLong());
        assertEquals(103L, tiers.get(2).getAsJsonObject().get("grekey").getAsLong());
    }

    @Test
    public void testPrepareVpcTopologyUpdateRejectsImplementedTierWithoutBroadcastUri() {
        prepareTopologyNetwork(Network.State.Implemented, BroadcastDomainType.Vswitch, null);

        assertThrows(CloudRuntimeException.class, () -> manager.prepareVpcTopologyUpdate(VPC_ID));
    }

    @Test
    public void testPrepareVpcTopologyUpdateRejectsBroadcastUriWithoutAuthority() {
        prepareTopologyNetwork(Network.State.Implemented, BroadcastDomainType.Vswitch, URI.create("vs:///"));

        assertThrows(CloudRuntimeException.class, () -> manager.prepareVpcTopologyUpdate(VPC_ID));
        verify(nicDao, never()).findByIp4AddressAndNetworkId(NETWORK_GATEWAY, NETWORK_ID);
    }

    @Test
    public void testPrepareVpcTopologyUpdateRejectsMalformedBroadcastKeys() {
        assertBroadcastKeysRejected(List.of("8.123", "7.", "7.invalid", "7.9223372036854775808",
                "7..123", ".7.123", "7.123."));
    }

    @Test
    public void testPrepareVpcTopologyUpdateAcceptsUnsignedGreKeyBoundaries() {
        for (Long greKey : List.of(0L, 2147483648L, 4294967295L)) {
            prepareVswitchNetwork("7." + greKey);
            prepareGatewayNic();

            OvsVpcPhysicalTopologyConfigCommand command = manager.prepareVpcTopologyUpdate(VPC_ID);

            assertEquals(greKey.longValue(), getOnlyTier(command).get("grekey").getAsLong());
        }
    }

    @Test
    public void testPrepareVpcTopologyUpdateRejectsOutOfRangeGreKeys() {
        assertBroadcastKeysRejected(List.of("7.-1", "7.4294967296"));
    }

    @Test
    public void testPrepareVpcTopologyUpdateRejectsMissingGatewayNic() {
        prepareVswitchNetwork("7.123");

        assertThrows(CloudRuntimeException.class, () -> manager.prepareVpcTopologyUpdate(VPC_ID));
    }

    private void assertVpcRejectedBeforeProviderLookup() {
        assertFalse(manager.isOvsDistributedRouterVpc(VPC_ID));
        verify(vpcManager, never()).isProviderSupportServiceInVpc(anyLong(),
                any(Network.Service.class), any(Network.Provider.class));
    }

    private void assertSecondVpcStillProcesses(VmStateChangeContext context) {
        prepareEmptyTopology(SECOND_VPC_ID);
        prepareSequenceNumber(SECOND_VPC_ID);

        assertTrue(manager.postStateTransitionEvent(context.transition, context.vm, true, null));

        verify(vpcManager).getVpcNetworks(SECOND_VPC_ID);
        verify(sequenceNumberDao).update(1L, sequenceNumber);
    }

    private void assertNetworkAclUpdateSkipped(NetworkVO network) {
        manager.new NetworkAclEventsSubscriber().onPublishMessage("sender", "Network_ACL_Replaced", network);

        verify(topologyGuru, never()).getVpcSpannedHosts(anyLong());
        verify(vpcManager, never()).getVpcNetworks(anyLong());
    }

    private void assertPayloadDispatchedToEveryHost(List<Long> hostIds, List<String> payloads) {
        assertEquals(List.of(HOST_ID, SECOND_HOST_ID, THIRD_HOST_ID), hostIds);
        assertEquals(3, payloads.size());
        assertEquals(payloads.get(0), payloads.get(1));
        assertEquals(payloads.get(1), payloads.get(2));
    }

    private void assertBroadcastKeysRejected(List<String> broadcastKeys) {
        for (String broadcastKey : broadcastKeys) {
            prepareVswitchNetwork(broadcastKey);
            assertThrows(CloudRuntimeException.class, () -> manager.prepareVpcTopologyUpdate(VPC_ID));
        }

        verify(nicDao, never()).findByIp4AddressAndNetworkId(NETWORK_GATEWAY, NETWORK_ID);
    }

    private VpcVO prepareDistributedVpc(long vpcId, String uuid, String cidr, boolean ovsProvider) {
        VpcVO vpc = prepareVpc(vpcId, uuid, cidr);
        when(vpc.usesDistributedRouter()).thenReturn(true);
        when(vpcManager.isProviderSupportServiceInVpc(vpcId, Network.Service.Connectivity, Network.Provider.Ovs))
                .thenReturn(ovsProvider);
        return vpc;
    }

    private VpcVO prepareVpc(long vpcId, String uuid, String cidr) {
        VpcVO vpc = mock(VpcVO.class);
        when(vpcDao.findById(vpcId)).thenReturn(vpc);
        when(vpc.getUuid()).thenReturn(uuid);
        when(vpc.getCidr()).thenReturn(cidr);
        return vpc;
    }

    private VMInstanceVO prepareChangedVm(List<Long> vpcIds) {
        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getId()).thenReturn(CHANGED_VM_ID);
        when(topologyGuru.getVpcIdsVmIsPartOf(CHANGED_VM_ID)).thenReturn(vpcIds);
        return vm;
    }

    private VmStateChangeContext prepareStateChangeContext(List<Long> vpcIds) {
        return new VmStateChangeContext(prepareChangedVm(vpcIds), prepareSuccessfulStartTransition());
    }

    private VmStateChangeContext prepareTwoOvsVpcStateChange() {
        VmStateChangeContext context = prepareStateChangeContext(List.of(VPC_ID, SECOND_VPC_ID));
        prepareDistributedVpc(VPC_ID, VPC_UUID, VPC_CIDR, true);
        prepareDistributedVpc(SECOND_VPC_ID, "second-vpc-uuid", "10.1.0.0/16", true);
        return context;
    }

    @SuppressWarnings("unchecked")
    private StateMachine2.Transition<VirtualMachine.State, VirtualMachine.Event> prepareSuccessfulStartTransition() {
        StateMachine2.Transition<VirtualMachine.State, VirtualMachine.Event> transition =
                mock(StateMachine2.Transition.class);
        when(transition.getCurrentState()).thenReturn(VirtualMachine.State.Starting);
        when(transition.getEvent()).thenReturn(VirtualMachine.Event.OperationSucceeded);
        when(transition.getToState()).thenReturn(VirtualMachine.State.Running);
        return transition;
    }

    private void prepareEmptyTopology(long vpcId) {
        doReturn(Collections.emptyList()).when(vpcManager).getVpcNetworks(vpcId);
        when(topologyGuru.getVpcSpannedHosts(vpcId)).thenReturn(Collections.emptyList());
        when(topologyGuru.getAllActiveVmsInVpc(vpcId)).thenReturn(Collections.emptyList());
    }

    private VpcVO prepareVpcIdentity() {
        return prepareVpc(VPC_ID, VPC_UUID, VPC_CIDR);
    }

    private NetworkVO prepareNetworkInState(Network.State state) {
        NetworkVO network = mock(NetworkVO.class);
        when(network.getState()).thenReturn(state);
        return network;
    }

    private NetworkVO prepareTopologyNetwork(Network.State state, BroadcastDomainType broadcastDomainType,
            URI broadcastUri) {
        prepareVpcIdentity();
        NetworkVO network = prepareNetwork(NETWORK_ID, NETWORK_UUID, NETWORK_GATEWAY, NETWORK_CIDR, state);
        when(network.getBroadcastDomainType()).thenReturn(broadcastDomainType);
        when(network.getBroadcastUri()).thenReturn(broadcastUri);
        prepareTopologyCollections(List.of(network));
        return network;
    }

    private void prepareTopologyCollections(List<NetworkVO> networks) {
        doReturn(networks).when(vpcManager).getVpcNetworks(VPC_ID);
        when(topologyGuru.getVpcSpannedHosts(VPC_ID)).thenReturn(Collections.emptyList());
        when(topologyGuru.getAllActiveVmsInVpc(VPC_ID)).thenReturn(Collections.emptyList());
    }

    private NetworkVO prepareVswitchNetwork(String broadcastKey) {
        prepareVpcIdentity();
        return prepareVswitchNetworkForCurrentVpc(broadcastKey);
    }

    private NetworkVO prepareVswitchNetworkForCurrentVpc(String broadcastKey) {
        NetworkVO network = prepareVswitchNetwork(NETWORK_ID, NETWORK_UUID, NETWORK_GATEWAY, NETWORK_CIDR,
                Network.State.Implemented, broadcastKey);
        prepareTopologyCollections(List.of(network));
        return network;
    }

    private NetworkVO prepareVswitchNetwork(long networkId, String networkUuid, String gateway, String cidr,
            Network.State state, String broadcastKey) {
        NetworkVO network = prepareNetwork(networkId, networkUuid, gateway, cidr, state);
        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Vswitch);
        when(network.getBroadcastUri()).thenReturn(BroadcastDomainType.Vswitch.toUri(broadcastKey));
        return network;
    }

    private NetworkVO prepareNetwork(long networkId, String networkUuid, String gateway, String cidr,
            Network.State state) {
        NetworkVO network = prepareNetworkInState(state);
        when(network.getId()).thenReturn(networkId);
        when(network.getUuid()).thenReturn(networkUuid);
        when(network.getGateway()).thenReturn(gateway);
        when(network.getCidr()).thenReturn(cidr);
        return network;
    }

    private NicVO prepareGatewayNic() {
        return prepareGatewayNic(NETWORK_ID, NETWORK_GATEWAY, GATEWAY_MAC);
    }

    private NicVO prepareGatewayNic(long networkId, String gateway, String macAddress) {
        NicVO gatewayNic = mock(NicVO.class);
        when(nicDao.findByIp4AddressAndNetworkId(gateway, networkId)).thenReturn(gatewayNic);
        when(gatewayNic.getMacAddress()).thenReturn(macAddress);
        return gatewayNic;
    }

    private void prepareHostGreEndpoints(NetworkVO network) {
        PhysicalNetworkTrafficTypeVO trafficType = mock(PhysicalNetworkTrafficTypeVO.class);
        when(topologyGuru.getVpcSpannedHosts(VPC_ID)).thenReturn(List.of(HOST_ID, SECOND_HOST_ID, THIRD_HOST_ID));
        when(network.getPhysicalNetworkId()).thenReturn(PHYSICAL_NETWORK_ID);
        when(configDao.getValue(Config.OvsTunnelNetworkDefaultLabel.key())).thenReturn(PHYSICAL_NETWORK_LABEL);
        when(physicalNetworkTrafficTypeDao.findBy(PHYSICAL_NETWORK_ID, TrafficType.Guest)).thenReturn(trafficType);
        when(trafficType.getKvmNetworkLabel()).thenReturn(PHYSICAL_NETWORK_LABEL);
        prepareHostGreEndpoint(HOST_ID, GRE_ENDPOINT_IP);
        prepareHostGreEndpoint(SECOND_HOST_ID, SECOND_GRE_ENDPOINT_IP);
        prepareHostGreEndpoint(THIRD_HOST_ID, THIRD_GRE_ENDPOINT_IP);
    }

    private void prepareHostGreEndpoint(long hostId, String endpointIp) {
        HostVO host = mock(HostVO.class);
        OvsTunnelInterfaceVO tunnelInterface = mock(OvsTunnelInterfaceVO.class);
        when(hostDao.findById(hostId)).thenReturn(host);
        when(host.getId()).thenReturn(hostId);
        when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        when(tunnelInterfaceDao.getByHostAndLabel(hostId, PHYSICAL_NETWORK_LABEL)).thenReturn(tunnelInterface);
        when(tunnelInterface.getIp()).thenReturn(endpointIp);
    }

    private void prepareActiveVm(NetworkVO network) {
        VMInstanceVO activeVm = mock(VMInstanceVO.class);
        NicVO vmNic = mock(NicVO.class);
        when(topologyGuru.getAllActiveVmsInVpc(VPC_ID)).thenReturn(List.of(ACTIVE_VM_ID));
        when(vmInstanceDao.findById(ACTIVE_VM_ID)).thenReturn(activeVm);
        when(activeVm.getHostId()).thenReturn(HOST_ID);
        when(nicDao.listByVmId(ACTIVE_VM_ID)).thenReturn(List.of(vmNic));
        when(vmNic.getNetworkId()).thenReturn(NETWORK_ID);
        when(vmNic.getIPv4Address()).thenReturn(VM_IP);
        when(vmNic.getMacAddress()).thenReturn(VM_MAC);
        when(network.getTrafficType()).thenReturn(TrafficType.Guest);
        when(networkDao.findById(NETWORK_ID)).thenReturn(network);
    }

    private NetworkVO prepareNetworkAclEvent(Long vpcId) {
        NetworkVO network = mock(NetworkVO.class);
        when(network.getVpcId()).thenReturn(vpcId);
        return network;
    }

    private NetworkVO prepareNetworkWithAcl() {
        NetworkVO network = mock(NetworkVO.class);
        NetworkACLVO networkAcl = mock(NetworkACLVO.class);
        NetworkACLItemVO aclItem = mock(NetworkACLItemVO.class);
        when(network.getVpcId()).thenReturn(VPC_ID);
        when(network.getUuid()).thenReturn(NETWORK_UUID);
        when(network.getCidr()).thenReturn(NETWORK_CIDR);
        when(network.getNetworkACLId()).thenReturn(ACL_ID);
        doReturn(List.of(network)).when(vpcManager).getVpcNetworks(VPC_ID);
        when(topologyGuru.getVpcSpannedHosts(VPC_ID)).thenReturn(List.of(HOST_ID, SECOND_HOST_ID, THIRD_HOST_ID));
        when(networkAclDao.findById(ACL_ID)).thenReturn(networkAcl);
        when(networkAcl.getUuid()).thenReturn("acl-uuid");
        when(networkAclItemDao.listByACL(ACL_ID)).thenReturn(List.of(aclItem));
        when(aclItem.getNumber()).thenReturn(10);
        when(aclItem.getUuid()).thenReturn("acl-item-uuid");
        when(aclItem.getAction()).thenReturn(NetworkACLItem.Action.Allow);
        when(aclItem.getTrafficType()).thenReturn(NetworkACLItem.TrafficType.Ingress);
        when(aclItem.getSourcePortStart()).thenReturn(80);
        when(aclItem.getSourcePortEnd()).thenReturn(80);
        when(aclItem.getProtocol()).thenReturn("tcp");
        when(aclItem.getSourceCidrList()).thenReturn(List.of("192.0.2.0/24"));
        return network;
    }

    private void assertCommandTarget(long expectedHostId, long actualHostId, String bridgeName,
            long sequenceNumber) {
        assertEquals(expectedHostId, actualHostId);
        assertEquals(BRIDGE_NAME, bridgeName);
        assertEquals(1L, sequenceNumber);
    }

    private Answer prepareHostAnswer(long hostId) {
        if (hostId == SECOND_HOST_ID) {
            throw new CloudRuntimeException("host update failed");
        }
        return prepareAnswer(hostId == THIRD_HOST_ID);
    }

    private Answer prepareAnswer(boolean result) {
        Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(result);
        return answer;
    }

    private void prepareSequenceNumber(long vpcId) {
        sequenceNumber = mock(VpcDistributedRouterSeqNoVO.class);
        when(sequenceNumber.getId()).thenReturn(1L);
        when(sequenceNumber.getTopologyUpdateSequenceNo()).thenReturn(1L);
        when(sequenceNumber.getPolicyUpdateSequenceNo()).thenReturn(1L);
        when(sequenceNumberDao.findByVpcId(vpcId)).thenReturn(sequenceNumber);
        when(sequenceNumberDao.lockRow(1L, true)).thenReturn(sequenceNumber);
    }

    private static class VmStateChangeContext {
        private final VMInstanceVO vm;
        private final StateMachine2.Transition<VirtualMachine.State, VirtualMachine.Event> transition;

        private VmStateChangeContext(VMInstanceVO vm,
                StateMachine2.Transition<VirtualMachine.State, VirtualMachine.Event> transition) {
            this.vm = vm;
            this.transition = transition;
        }
    }

    private JsonObject getOnlyTier(OvsVpcPhysicalTopologyConfigCommand command) {
        JsonArray tiers = getVpcJson(command.getVpcConfigInJson()).getAsJsonArray("tiers");
        assertEquals(1, tiers.size());
        return tiers.get(0).getAsJsonObject();
    }

    private JsonObject getVpcJson(String commandJson) {
        return JsonParser.parseString(commandJson).getAsJsonObject().getAsJsonObject("vpc");
    }

    private void assertCompleteTopologyPayload(String commandJson) {
        JsonObject vpc = getVpcJson(commandJson);
        assertEquals(VPC_CIDR, vpc.get("cidr").getAsString());

        JsonArray hosts = vpc.getAsJsonArray("hosts");
        assertEquals(3, hosts.size());
        assertHost(hosts.get(0).getAsJsonObject(), HOST_ID, GRE_ENDPOINT_IP);
        assertHost(hosts.get(1).getAsJsonObject(), SECOND_HOST_ID, SECOND_GRE_ENDPOINT_IP);
        assertHost(hosts.get(2).getAsJsonObject(), THIRD_HOST_ID, THIRD_GRE_ENDPOINT_IP);

        JsonArray tiers = vpc.getAsJsonArray("tiers");
        assertEquals(1, tiers.size());
        JsonObject tier = tiers.get(0).getAsJsonObject();
        assertEquals(123L, tier.get("grekey").getAsLong());
        assertEquals(NETWORK_UUID, tier.get("networkuuid").getAsString());
        assertEquals(NETWORK_GATEWAY, tier.get("gatewayip").getAsString());
        assertEquals(GATEWAY_MAC, tier.get("gatewaymac").getAsString());
        assertEquals(NETWORK_CIDR, tier.get("cidr").getAsString());

        JsonArray vms = vpc.getAsJsonArray("vms");
        assertEquals(1, vms.size());
        JsonObject vm = vms.get(0).getAsJsonObject();
        assertEquals(HOST_ID, vm.get("hostid").getAsLong());
        JsonArray nics = vm.getAsJsonArray("nics");
        assertEquals(1, nics.size());
        JsonObject nic = nics.get(0).getAsJsonObject();
        assertEquals(VM_IP, nic.get("ipaddress").getAsString());
        assertEquals(VM_MAC, nic.get("macaddress").getAsString());
        assertEquals(NETWORK_UUID, nic.get("networkuuid").getAsString());
    }

    private void assertHost(JsonObject host, long hostId, String endpointIp) {
        assertEquals(hostId, host.get("hostid").getAsLong());
        assertEquals(endpointIp, host.get("ipaddress").getAsString());
    }

    private void assertCompleteRoutingPolicyPayload(String commandJson) {
        JsonObject vpc = getVpcJson(commandJson);
        assertEquals(VPC_UUID, vpc.get("id").getAsString());
        assertEquals(VPC_CIDR, vpc.get("cidr").getAsString());

        JsonArray acls = vpc.getAsJsonArray("acls");
        assertEquals(1, acls.size());
        JsonObject acl = acls.get(0).getAsJsonObject();
        assertEquals("acl-uuid", acl.get("id").getAsString());
        JsonArray aclItems = acl.getAsJsonArray("aclitems");
        assertEquals(1, aclItems.size());
        JsonObject aclItem = aclItems.get(0).getAsJsonObject();
        assertEquals(10, aclItem.get("number").getAsInt());
        assertEquals("acl-item-uuid", aclItem.get("uuid").getAsString());
        assertEquals("allow", aclItem.get("action").getAsString());
        assertEquals("ingress", aclItem.get("direction").getAsString());
        assertEquals("80", aclItem.get("sourceportstart").getAsString());
        assertEquals("80", aclItem.get("sourceportend").getAsString());
        assertEquals("tcp", aclItem.get("protocol").getAsString());
        assertEquals("192.0.2.0/24", aclItem.getAsJsonArray("sourcecidrs").get(0).getAsString());

        JsonArray tiers = vpc.getAsJsonArray("tiers");
        assertEquals(1, tiers.size());
        JsonObject tier = tiers.get(0).getAsJsonObject();
        assertEquals(NETWORK_UUID, tier.get("id").getAsString());
        assertEquals(NETWORK_CIDR, tier.get("cidr").getAsString());
        assertEquals("acl-uuid", tier.get("aclid").getAsString());
    }
}
