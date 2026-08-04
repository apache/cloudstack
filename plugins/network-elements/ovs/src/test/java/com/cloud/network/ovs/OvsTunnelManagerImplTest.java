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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.OvsVpcPhysicalTopologyConfigCommand;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.ovs.dao.VpcDistributedRouterSeqNoDao;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

public class OvsTunnelManagerImplTest {
    private static final long VPC_ID = 7L;
    private static final long SECOND_VPC_ID = 8L;

    private OvsTunnelManagerImpl manager;
    private VpcDao vpcDao;
    private VpcManager vpcManager;
    private OvsNetworkTopologyGuru topologyGuru;
    private NicDao nicDao;

    @Before
    public void setUp() {
        manager = new OvsTunnelManagerImpl();
        vpcDao = mock(VpcDao.class);
        vpcManager = mock(VpcManager.class);
        topologyGuru = mock(OvsNetworkTopologyGuru.class);
        nicDao = mock(NicDao.class);
        manager._vpcDao = vpcDao;
        manager._vpcMgr = vpcManager;
        manager._ovsNetworkToplogyGuru = topologyGuru;
        manager._nicDao = nicDao;
        manager._hostDao = mock(HostDao.class);
        manager._vmInstanceDao = mock(VMInstanceDao.class);
        manager._networkDao = mock(NetworkDao.class);
        manager._vpcDrSeqNoDao = mock(VpcDistributedRouterSeqNoDao.class);
        manager._agentMgr = mock(AgentManager.class);
    }

    @Test
    public void testIsOvsDistributedRouterVpcReturnsFalseWhenVpcIsMissing() {
        assertFalse(manager.isOvsDistributedRouterVpc(VPC_ID));
        verify(vpcManager, never()).isProviderSupportServiceInVpc(anyLong(),
                org.mockito.ArgumentMatchers.any(Network.Service.class),
                org.mockito.ArgumentMatchers.any(Network.Provider.class));
    }

    @Test
    public void testIsOvsDistributedRouterVpcReturnsFalseWhenVpcIsNotDistributed() {
        VpcVO vpc = mock(VpcVO.class);
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        when(vpc.usesDistributedRouter()).thenReturn(false);

        assertFalse(manager.isOvsDistributedRouterVpc(VPC_ID));
    }

    @Test
    public void testIsOvsDistributedRouterVpcReturnsFalseForNsxDistributedVpc() {
        VpcVO vpc = mock(VpcVO.class);
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        when(vpc.usesDistributedRouter()).thenReturn(true);
        when(vpcManager.isProviderSupportServiceInVpc(VPC_ID, Network.Service.Connectivity, Network.Provider.Ovs))
                .thenReturn(false);

        assertFalse(manager.isOvsDistributedRouterVpc(VPC_ID));
    }

    @Test
    public void testIsOvsDistributedRouterVpcReturnsTrueForOvsConnectivityDistributedVpc() {
        VpcVO vpc = mock(VpcVO.class);
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        when(vpc.usesDistributedRouter()).thenReturn(true);
        when(vpcManager.isProviderSupportServiceInVpc(VPC_ID, Network.Service.Connectivity, Network.Provider.Ovs))
                .thenReturn(true);

        assertTrue(manager.isOvsDistributedRouterVpc(VPC_ID));
    }

    @Test
    public void testPostStateTransitionEventIgnoresNsxDistributedVpc() {
        VpcVO vpc = mock(VpcVO.class);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        @SuppressWarnings("unchecked")
        StateMachine2.Transition<VirtualMachine.State, VirtualMachine.Event> transition = mock(StateMachine2.Transition.class);
        when(vm.getId()).thenReturn(11L);
        when(topologyGuru.getVpcIdsVmIsPartOf(11L)).thenReturn(List.of(VPC_ID));
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        when(vpc.usesDistributedRouter()).thenReturn(true);
        when(vpcManager.isProviderSupportServiceInVpc(VPC_ID, Network.Service.Connectivity, Network.Provider.Ovs))
                .thenReturn(false);
        when(transition.getCurrentState()).thenReturn(VirtualMachine.State.Starting);
        when(transition.getEvent()).thenReturn(VirtualMachine.Event.OperationSucceeded);
        when(transition.getToState()).thenReturn(VirtualMachine.State.Running);

        assertTrue(manager.postStateTransitionEvent(transition, vm, true, null));

        verify(topologyGuru, never()).getVpcSpannedHosts(anyLong());
        verify(vpcManager, never()).getVpcNetworks(anyLong());
    }

    @Test
    public void testPostStateTransitionEventContinuesAfterNonOvsVpc() {
        VpcVO firstVpc = mock(VpcVO.class);
        VpcVO secondVpc = mock(VpcVO.class);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        @SuppressWarnings("unchecked")
        StateMachine2.Transition<VirtualMachine.State, VirtualMachine.Event> transition = mock(StateMachine2.Transition.class);
        when(vm.getId()).thenReturn(11L);
        when(topologyGuru.getVpcIdsVmIsPartOf(11L)).thenReturn(List.of(VPC_ID, SECOND_VPC_ID));
        when(vpcDao.findById(VPC_ID)).thenReturn(firstVpc);
        when(vpcDao.findById(SECOND_VPC_ID)).thenReturn(secondVpc);
        when(firstVpc.usesDistributedRouter()).thenReturn(true);
        when(secondVpc.usesDistributedRouter()).thenReturn(true);
        when(vpcManager.isProviderSupportServiceInVpc(VPC_ID, Network.Service.Connectivity, Network.Provider.Ovs))
                .thenReturn(false);
        when(vpcManager.isProviderSupportServiceInVpc(SECOND_VPC_ID, Network.Service.Connectivity, Network.Provider.Ovs))
                .thenReturn(false);
        when(transition.getCurrentState()).thenReturn(VirtualMachine.State.Starting);
        when(transition.getEvent()).thenReturn(VirtualMachine.Event.OperationSucceeded);
        when(transition.getToState()).thenReturn(VirtualMachine.State.Running);

        assertTrue(manager.postStateTransitionEvent(transition, vm, true, null));

        verify(vpcDao).findById(SECOND_VPC_ID);
    }

    @Test
    public void testNetworkAclSubscriberIgnoresNsxDistributedVpc() {
        VpcVO vpc = mock(VpcVO.class);
        NetworkVO network = mock(NetworkVO.class);
        when(network.getVpcId()).thenReturn(VPC_ID);
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        when(vpc.usesDistributedRouter()).thenReturn(true);
        when(vpcManager.isProviderSupportServiceInVpc(VPC_ID, Network.Service.Connectivity, Network.Provider.Ovs))
                .thenReturn(false);

        manager.new NetworkAclEventsSubscriber().onPublishMessage("sender", "Network_ACL_Replaced", network);

        verify(topologyGuru, never()).getVpcSpannedHosts(anyLong());
        verify(vpcManager, never()).getVpcNetworks(anyLong());
    }

    @Test
    public void testPrepareVpcTopologyUpdateRejectsNonVswitchTier() {
        VpcVO vpc = mock(VpcVO.class);
        Network network = mock(Network.class);
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        when(vpc.getUuid()).thenReturn("vpc-uuid");
        doReturn(List.of(network)).when(vpcManager).getVpcNetworks(VPC_ID);
        when(topologyGuru.getVpcSpannedHosts(VPC_ID)).thenReturn(Collections.emptyList());
        when(topologyGuru.getAllActiveVmsInVpc(VPC_ID)).thenReturn(Collections.emptyList());
        when(network.getUuid()).thenReturn("network-uuid");
        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.NSX);

        assertThrows(CloudRuntimeException.class, () -> manager.prepareVpcTopologyUpdate(VPC_ID));
    }

    @Test
    public void testPrepareVpcTopologyUpdateRejectsBroadcastKeyForAnotherVpc() {
        Network network = prepareVswitchNetwork("8.123");

        assertThrows(CloudRuntimeException.class, () -> manager.prepareVpcTopologyUpdate(VPC_ID));

        verify(nicDao, never()).findByIp4AddressAndNetworkId("10.0.1.1", 13L);
    }

    @Test
    public void testPrepareVpcTopologyUpdateRejectsNonNumericGreKey() {
        prepareVswitchNetwork("7.invalid");

        assertThrows(CloudRuntimeException.class, () -> manager.prepareVpcTopologyUpdate(VPC_ID));
    }

    @Test
    public void testPrepareVpcTopologyUpdateRejectsMissingGatewayNic() {
        prepareVswitchNetwork("7.123");

        assertThrows(CloudRuntimeException.class, () -> manager.prepareVpcTopologyUpdate(VPC_ID));
    }

    @Test
    public void testPrepareVpcTopologyUpdateBuildsValidOvsTopology() {
        prepareVswitchNetwork("7.123");
        NicVO gatewayNic = mock(NicVO.class);
        when(nicDao.findByIp4AddressAndNetworkId("10.0.1.1", 13L)).thenReturn(gatewayNic);
        when(gatewayNic.getMacAddress()).thenReturn("02:00:00:00:00:01");

        OvsVpcPhysicalTopologyConfigCommand command = manager.prepareVpcTopologyUpdate(VPC_ID);

        String topology = command.getVpcConfigInJson();
        assertTrue(topology.contains("\"grekey\":123"));
        assertTrue(topology.contains("\"networkuuid\":\"network-uuid\""));
        assertTrue(topology.contains("\"gatewaymac\":\"02:00:00:00:00:01\""));
    }

    private Network prepareVswitchNetwork(String broadcastKey) {
        VpcVO vpc = mock(VpcVO.class);
        Network network = mock(Network.class);
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        when(vpc.getUuid()).thenReturn("vpc-uuid");
        when(vpc.getCidr()).thenReturn("10.0.0.0/16");
        doReturn(List.of(network)).when(vpcManager).getVpcNetworks(VPC_ID);
        when(topologyGuru.getVpcSpannedHosts(VPC_ID)).thenReturn(Collections.emptyList());
        when(topologyGuru.getAllActiveVmsInVpc(VPC_ID)).thenReturn(Collections.emptyList());
        when(network.getId()).thenReturn(13L);
        when(network.getUuid()).thenReturn("network-uuid");
        when(network.getGateway()).thenReturn("10.0.1.1");
        when(network.getCidr()).thenReturn("10.0.1.0/24");
        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Vswitch);
        when(network.getBroadcastUri()).thenReturn(BroadcastDomainType.Vswitch.toUri(broadcastKey));
        return network;
    }
}
