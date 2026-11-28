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
package com.cloud.kubernetes.cluster.actionworkers;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.exception.ManagementServerException;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.RulesService;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.user.Account;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterScaleWorkerSshRulesTest {

    private static final long CLUSTER_ID = 1L;
    private static final long IP_ID = 10L;

    @Mock
    private KubernetesCluster kubernetesCluster;
    @Mock
    private KubernetesClusterManagerImpl clusterManager;
    @Mock
    private KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    @Mock
    private FirewallRulesDao firewallRulesDao;
    @Mock
    private PortForwardingRulesDao portForwardingRulesDao;
    @Mock
    private FirewallService firewallService;
    @Mock
    private RulesService rulesService;
    @Mock
    private Network network;
    @Mock
    private IpAddress sshIp;
    @Mock
    private Account owner;

    private KubernetesClusterScaleWorker worker;
    private final List<KubernetesClusterVmMapVO> vmMaps = new ArrayList<>();
    private final List<FirewallRuleVO> ipRules = new ArrayList<>();
    private final List<PortForwardingRuleVO> pfRules = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        worker = Mockito.spy(new KubernetesClusterScaleWorker(kubernetesCluster, clusterManager));
        worker.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        worker.firewallRulesDao = firewallRulesDao;
        worker.portForwardingRulesDao = portForwardingRulesDao;
        worker.firewallService = firewallService;
        worker.rulesService = rulesService;
        worker.owner = owner;
        ReflectionTestUtils.setField(worker, "network", network);
        Mockito.lenient().when(network.getVpcId()).thenReturn(null);
        Mockito.lenient().when(kubernetesCluster.getId()).thenReturn(CLUSTER_ID);
        Mockito.lenient().when(sshIp.getId()).thenReturn(IP_ID);
        Mockito.lenient().when(sshIp.getAddress()).thenReturn(new Ip("192.0.2.10"));
        Mockito.lenient().when(kubernetesClusterVmMapDao.listByClusterId(CLUSTER_ID)).thenReturn(vmMaps);
        Mockito.lenient().when(firewallRulesDao.listByIp(IP_ID)).thenReturn(ipRules);
        Mockito.lenient().when(portForwardingRulesDao.listByIpAndNotRevoked(IP_ID)).thenReturn(pfRules);
        Mockito.lenient().when(portForwardingRulesDao.findById(Mockito.anyLong())).thenThrow(
                new NullPointerException("PortForwardingRulesDaoImpl.findById dereferences a removed rule"));
        Mockito.lenient().doReturn(sshIp).when(worker).getNodeSshIp();
        Mockito.lenient().doNothing().when(worker).provisionNodeSshFirewallRule(Mockito.any(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.lenient().doNothing().when(worker).provisionPublicIpPortForwardingRule(Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.anyLong(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.lenient().when(firewallService.revokeIngressFirewallRule(Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(true);
        Mockito.lenient().when(rulesService.revokePortForwardingRule(Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(true);
    }

    private void vmMap(long vmId, boolean external) {
        KubernetesClusterVmMapVO map = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.lenient().when(map.getVmId()).thenReturn(vmId);
        Mockito.lenient().when(map.isExternalNode()).thenReturn(external);
        Mockito.lenient().when(map.isEtcdNode()).thenReturn(false);
        vmMaps.add(map);
    }

    private PortForwardingRuleVO sshRule(long id, long vmId, int port) {
        PortForwardingRuleVO rule = Mockito.mock(PortForwardingRuleVO.class);
        Mockito.lenient().when(rule.getId()).thenReturn(id);
        Mockito.lenient().when(rule.getVirtualMachineId()).thenReturn(vmId);
        Mockito.lenient().when(rule.getSourceIpAddressId()).thenReturn(IP_ID);
        Mockito.lenient().when(rule.getSourcePortStart()).thenReturn(port);
        Mockito.lenient().when(rule.getSourcePortEnd()).thenReturn(port);
        Mockito.lenient().when(rule.getDestinationPortStart()).thenReturn(22);
        Mockito.lenient().when(rule.getDestinationPortEnd()).thenReturn(22);
        Mockito.lenient().when(rule.getProtocol()).thenReturn(NetUtils.TCP_PROTO);
        Mockito.lenient().when(rule.getPurpose()).thenReturn(FirewallRule.Purpose.PortForwarding);
        Mockito.lenient().when(rule.getState()).thenReturn(FirewallRule.State.Active);
        pfRules.add(rule);
        ipRules.add(rule);
        return rule;
    }

    private FirewallRuleVO firewallRule(long id, int start, int end, String cidr) {
        FirewallRuleVO rule = Mockito.mock(FirewallRuleVO.class);
        Mockito.lenient().when(rule.getId()).thenReturn(id);
        Mockito.lenient().when(rule.getSourcePortStart()).thenReturn(start);
        Mockito.lenient().when(rule.getSourcePortEnd()).thenReturn(end);
        Mockito.lenient().when(rule.getProtocol()).thenReturn(NetUtils.TCP_PROTO);
        Mockito.lenient().when(rule.getPurpose()).thenReturn(FirewallRule.Purpose.Firewall);
        Mockito.lenient().when(rule.getState()).thenReturn(FirewallRule.State.Active);
        Mockito.lenient().when(rule.getSourceCidrList()).thenReturn(List.of(cidr));
        ipRules.add(rule);
        return rule;
    }

    private void verifyNoSshRuleProvisionedFor(long vmId) throws Exception {
        Mockito.verify(worker, Mockito.never()).provisionPublicIpPortForwardingRule(Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.eq(vmId), Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    public void provisionMissingNodeSshRulesKeepsExistingRulesAndSkipsAddedNodePort() throws Exception {
        vmMap(1L, false);
        vmMap(2L, false);
        vmMap(3L, true);
        vmMap(4L, false);
        sshRule(101L, 1L, 2222);
        sshRule(102L, 2L, 2223);
        sshRule(103L, 3L, 2224);
        firewallRule(201L, 2222, 2223, NetUtils.ALL_IP4_CIDRS);
        firewallRule(202L, 2224, 2224, NetUtils.ALL_IP4_CIDRS);

        worker.provisionMissingNodeSshRules(List.of(1L, 2L, 3L, 4L));

        verifyNoSshRuleProvisionedFor(1L);
        verifyNoSshRuleProvisionedFor(2L);
        verifyNoSshRuleProvisionedFor(3L);
        Mockito.verify(worker).provisionNodeSshFirewallRule(sshIp, 2225, 2225);
        Mockito.verify(worker).provisionPublicIpPortForwardingRule(sshIp, network, owner, 4L, 2225, 22);
        Mockito.verify(rulesService, Mockito.never()).revokePortForwardingRule(Mockito.anyLong(), Mockito.anyBoolean());
        Mockito.verify(firewallService, Mockito.never()).revokeIngressFirewallRule(Mockito.anyLong(), Mockito.anyBoolean());
    }

    @Test
    public void provisionMissingNodeSshRulesGivesNewWorkersTheirPositionalPortWhenFree() throws Exception {
        vmMap(1L, false);
        vmMap(2L, false);
        vmMap(3L, false);
        vmMap(4L, false);
        sshRule(101L, 1L, 2222);
        sshRule(102L, 2L, 2223);
        firewallRule(201L, 2222, 2223, NetUtils.ALL_IP4_CIDRS);

        worker.provisionMissingNodeSshRules(List.of(1L, 2L, 3L, 4L));

        Mockito.verify(worker).provisionPublicIpPortForwardingRule(sshIp, network, owner, 3L, 2224, 22);
        Mockito.verify(worker).provisionNodeSshFirewallRule(sshIp, 2224, 2224);
        Mockito.verify(worker).provisionPublicIpPortForwardingRule(sshIp, network, owner, 4L, 2225, 22);
        Mockito.verify(worker).provisionNodeSshFirewallRule(sshIp, 2225, 2225);
    }

    @Test
    public void provisionMissingNodeSshRulesReusesPortStillOpenByClusterFirewallRule() throws Exception {
        vmMap(1L, false);
        vmMap(2L, false);
        vmMap(3L, false);
        sshRule(101L, 1L, 2222);
        sshRule(102L, 2L, 2223);
        firewallRule(201L, 2222, 2224, NetUtils.ALL_IP4_CIDRS);

        worker.provisionMissingNodeSshRules(List.of(1L, 2L, 3L));

        Mockito.verify(worker).provisionPublicIpPortForwardingRule(sshIp, network, owner, 3L, 2224, 22);
        Mockito.verify(worker, Mockito.never()).provisionNodeSshFirewallRule(Mockito.any(), Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    public void provisionMissingNodeSshRulesInVpcTierAddsNoFirewallRule() throws Exception {
        Mockito.when(network.getVpcId()).thenReturn(5L);
        vmMap(1L, false);
        vmMap(2L, true);
        vmMap(3L, false);
        sshRule(101L, 1L, 2222);
        sshRule(102L, 2L, 2224);

        worker.provisionMissingNodeSshRules(List.of(1L, 2L, 3L));

        Mockito.verify(worker).provisionPublicIpPortForwardingRule(sshIp, network, owner, 3L, 2223, 22);
        Mockito.verify(worker, Mockito.never()).provisionNodeSshFirewallRule(Mockito.any(), Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    public void provisionMissingNodeSshRulesDoesNothingOnDirectAccessNetwork() throws Exception {
        Mockito.doReturn(null).when(worker).getNodeSshIp();
        vmMap(1L, false);

        worker.provisionMissingNodeSshRules(List.of(1L));

        verifyNoSshRuleProvisionedFor(1L);
        Mockito.verifyNoInteractions(firewallRulesDao, portForwardingRulesDao);
    }

    @Test(expected = ManagementServerException.class)
    public void provisionMissingNodeSshRulesFailsWhenNoPortIsFree() throws Exception {
        vmMap(1L, false);
        vmMap(2L, false);
        sshRule(101L, 1L, 2222);
        FirewallRuleVO lb = Mockito.mock(FirewallRuleVO.class);
        Mockito.when(lb.getSourcePortStart()).thenReturn(2223);
        Mockito.when(lb.getSourcePortEnd()).thenReturn(NetUtils.PORT_RANGE_MAX);
        Mockito.when(lb.getPurpose()).thenReturn(FirewallRule.Purpose.LoadBalancing);
        ipRules.add(lb);

        worker.provisionMissingNodeSshRules(List.of(1L, 2L));
    }

    @Test
    public void provisionMissingNodeSshRulesAddsFirewallRuleWhenOnlyARestrictedRuleCoversThePort() throws Exception {
        vmMap(1L, false);
        vmMap(2L, false);
        sshRule(101L, 1L, 2222);
        firewallRule(201L, 2222, 2230, "10.0.0.0/8");

        worker.provisionMissingNodeSshRules(List.of(1L, 2L));

        Mockito.verify(worker).provisionNodeSshFirewallRule(sshIp, 2223, 2223);
        Mockito.verify(worker, Mockito.never()).provisionNodeSshFirewallRule(sshIp, 2222, 2222);
        Mockito.verify(worker).provisionPublicIpPortForwardingRule(sshIp, network, owner, 2L, 2223, 22);
    }

    @Test
    public void provisionMissingNodeSshRulesReusesHoleLeftByRemovedWorker() throws Exception {
        vmMap(1L, false);
        vmMap(3L, false);
        vmMap(4L, false);
        sshRule(101L, 1L, 2222);
        sshRule(103L, 3L, 2224);
        firewallRule(201L, 2222, 2224, NetUtils.ALL_IP4_CIDRS);

        worker.provisionMissingNodeSshRules(List.of(1L, 3L, 4L));

        Mockito.verify(worker).provisionPublicIpPortForwardingRule(sshIp, network, owner, 4L, 2223, 22);
        Mockito.verify(worker, Mockito.never()).provisionNodeSshFirewallRule(Mockito.any(), Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    public void provisionMissingNodeSshRulesNeverGivesTwoNewNodesTheSamePort() throws Exception {
        vmMap(1L, false);
        vmMap(2L, true);
        vmMap(3L, true);
        sshRule(101L, 1L, 2222);

        worker.provisionMissingNodeSshRules(List.of(1L, 2L, 3L));

        Mockito.verify(worker).provisionPublicIpPortForwardingRule(sshIp, network, owner, 2L, 2223, 22);
        Mockito.verify(worker).provisionPublicIpPortForwardingRule(sshIp, network, owner, 3L, 2224, 22);
    }

    @Test
    public void provisionMissingNodeSshRulesReopensExistingNodePortWithoutFirewallRule() throws Exception {
        vmMap(1L, false);
        vmMap(2L, false);
        sshRule(101L, 1L, 2222);
        sshRule(102L, 2L, 2223);
        firewallRule(201L, 2223, 2223, NetUtils.ALL_IP4_CIDRS);

        worker.provisionMissingNodeSshRules(List.of(1L, 2L));

        Mockito.verify(worker).provisionNodeSshFirewallRule(sshIp, 2222, 2222);
        Mockito.verify(worker, Mockito.never()).provisionNodeSshFirewallRule(sshIp, 2223, 2223);
        verifyNoSshRuleProvisionedFor(1L);
        verifyNoSshRuleProvisionedFor(2L);
    }

    @Test
    public void provisionMissingNodeSshRulesRevokesNewFirewallRuleWhenPortForwardingFails() throws Exception {
        vmMap(1L, false);
        vmMap(2L, false);
        sshRule(101L, 1L, 2222);
        firewallRule(201L, 2222, 2222, NetUtils.ALL_IP4_CIDRS);
        Mockito.doThrow(new com.cloud.exception.NetworkRuleConflictException("conflict")).when(worker)
                .provisionPublicIpPortForwardingRule(sshIp, network, owner, 2L, 2223, 22);
        FirewallRuleVO added = firewallRule(202L, 2223, 2223, NetUtils.ALL_IP4_CIDRS);
        Mockito.when(firewallRulesDao.listByIpPurposePortsProtocolAndNotRevoked(IP_ID, 2223, 2223, NetUtils.TCP_PROTO, FirewallRule.Purpose.Firewall))
                .thenReturn(List.of(added));
        ipRules.remove(added);

        try {
            worker.provisionMissingNodeSshRules(List.of(1L, 2L));
            Assert.fail("expected ManagementServerException");
        } catch (ManagementServerException e) {
            Mockito.verify(worker).provisionNodeSshFirewallRule(sshIp, 2223, 2223);
            Mockito.verify(firewallService).revokeIngressFirewallRule(202L, true);
            Mockito.verify(portForwardingRulesDao).listByVm(2L);
        }
    }

    @Test
    public void shrinkNodeSshFirewallRuleClosesRemovedNodesPortsAtTheTop() throws Exception {
        FirewallRuleVO range = firewallRule(201L, 2222, 2225, NetUtils.ALL_IP4_CIDRS);
        sshRule(101L, 1L, 2222);
        sshRule(102L, 2L, 2223);
        Mockito.when(firewallRulesDao.listByIpPurposeProtocolAndNotRevoked(IP_ID, FirewallRule.Purpose.Firewall, NetUtils.TCP_PROTO))
                .thenReturn(List.of(range));

        worker.shrinkNodeSshFirewallRule(sshIp);

        InOrder inOrder = Mockito.inOrder(firewallService, worker);
        inOrder.verify(firewallService).revokeIngressFirewallRule(201L, true);
        inOrder.verify(worker).provisionNodeSshFirewallRule(sshIp, 2222, 2223);
    }

    @Test
    public void shrinkNodeSshFirewallRuleRestoresOldRangeWhenShrunkRuleFails() throws Exception {
        FirewallRuleVO range = firewallRule(201L, 2222, 2225, NetUtils.ALL_IP4_CIDRS);
        sshRule(101L, 1L, 2222);
        Mockito.when(firewallRulesDao.listByIpPurposeProtocolAndNotRevoked(IP_ID, FirewallRule.Purpose.Firewall, NetUtils.TCP_PROTO))
                .thenReturn(List.of(range));
        Mockito.doThrow(new com.cloud.exception.ResourceUnavailableException("vr down", Network.class, 1L)).when(worker)
                .provisionNodeSshFirewallRule(sshIp, 2222, 2222);
        FirewallRuleVO failed = firewallRule(202L, 2222, 2222, NetUtils.ALL_IP4_CIDRS);
        ipRules.remove(failed);
        Mockito.when(firewallRulesDao.listByIpPurposePortsProtocolAndNotRevoked(IP_ID, 2222, 2222, NetUtils.TCP_PROTO, FirewallRule.Purpose.Firewall))
                .thenReturn(List.of(failed));

        try {
            worker.shrinkNodeSshFirewallRule(sshIp);
            Assert.fail("expected ManagementServerException");
        } catch (ManagementServerException e) {
            InOrder inOrder = Mockito.inOrder(firewallService, worker);
            inOrder.verify(firewallService).revokeIngressFirewallRule(201L, true);
            inOrder.verify(firewallService).revokeIngressFirewallRule(202L, true);
            inOrder.verify(worker).provisionNodeSshFirewallRule(sshIp, 2222, 2225);
        }
    }

    @Test
    public void shrinkNodeSshFirewallRuleKeepsRuleWhenTopPortIsStillUsed() throws Exception {
        FirewallRuleVO range = firewallRule(201L, 2222, 2225, NetUtils.ALL_IP4_CIDRS);
        sshRule(101L, 1L, 2222);
        sshRule(103L, 3L, 2225);
        Mockito.when(firewallRulesDao.listByIpPurposeProtocolAndNotRevoked(IP_ID, FirewallRule.Purpose.Firewall, NetUtils.TCP_PROTO))
                .thenReturn(List.of(range));

        worker.shrinkNodeSshFirewallRule(sshIp);

        Mockito.verifyNoInteractions(firewallService);
        Mockito.verify(worker, Mockito.never()).provisionNodeSshFirewallRule(Mockito.any(), Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    public void removeNodeSshRulesRevokesOnlyTheNodesOwnRules() {
        PortForwardingRuleVO removed = sshRule(101L, 4L, 2225);
        FirewallRuleVO ownFirewallRule = firewallRule(201L, 2225, 2225, NetUtils.ALL_IP4_CIDRS);
        Mockito.when(firewallRulesDao.listByIpPurposePortsProtocolAndNotRevoked(IP_ID, 2225, 2225, NetUtils.TCP_PROTO, FirewallRule.Purpose.Firewall))
                .thenReturn(List.of(ownFirewallRule));
        Mockito.when(portForwardingRulesDao.listByVm(4L)).thenReturn(List.of(removed));

        Assert.assertTrue(worker.removeNodeSshRules(sshIp, List.of(removed), true));

        InOrder inOrder = Mockito.inOrder(firewallService, rulesService);
        inOrder.verify(firewallService).revokeIngressFirewallRule(201L, true);
        inOrder.verify(rulesService).revokePortForwardingRule(101L, true);
        Mockito.verifyNoMoreInteractions(firewallService, rulesService);
    }

    @Test
    public void removeNodeSshRulesKeepsRestrictedFirewallRuleOnThePort() {
        PortForwardingRuleVO removed = sshRule(101L, 4L, 2225);
        FirewallRuleVO userRule = firewallRule(201L, 2225, 2225, "10.0.0.0/8");
        Mockito.when(firewallRulesDao.listByIpPurposePortsProtocolAndNotRevoked(IP_ID, 2225, 2225, NetUtils.TCP_PROTO, FirewallRule.Purpose.Firewall))
                .thenReturn(List.of(userRule));
        Mockito.when(portForwardingRulesDao.listByVm(4L)).thenReturn(List.of(removed));

        Assert.assertTrue(worker.removeNodeSshRules(sshIp, List.of(removed), true));

        Mockito.verify(firewallService, Mockito.never()).revokeIngressFirewallRule(Mockito.anyLong(), Mockito.anyBoolean());
        Mockito.verify(rulesService).revokePortForwardingRule(101L, true);
    }

    @Test
    public void removeNodeSshRulesSkipsRuleAlreadyRemovedWithVmAndFirewallInVpc() {
        PortForwardingRuleVO removed = sshRule(101L, 4L, 2225);
        Mockito.when(portForwardingRulesDao.listByVm(4L)).thenReturn(List.of());

        Assert.assertTrue(worker.removeNodeSshRules(sshIp, List.of(removed), false));

        Mockito.verifyNoInteractions(firewallService, rulesService);
        Mockito.verify(firewallRulesDao, Mockito.never()).listByIpPurposePortsProtocolAndNotRevoked(Mockito.anyLong(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void removeNodeSshRulesAfterExpungeRevokesOnlyTheFirewallRule() {
        PortForwardingRuleVO removed = sshRule(101L, 4L, 2225);
        FirewallRuleVO ownFirewallRule = firewallRule(201L, 2225, 2225, NetUtils.ALL_IP4_CIDRS);
        Mockito.when(firewallRulesDao.listByIpPurposePortsProtocolAndNotRevoked(IP_ID, 2225, 2225, NetUtils.TCP_PROTO, FirewallRule.Purpose.Firewall))
                .thenReturn(List.of(ownFirewallRule));
        Mockito.when(portForwardingRulesDao.listByVm(4L)).thenReturn(List.of());

        Assert.assertTrue(worker.removeNodeSshRules(sshIp, List.of(removed), true));

        Mockito.verify(firewallService).revokeIngressFirewallRule(201L, true);
        Mockito.verifyNoInteractions(rulesService);
    }

    @Test
    public void removeNodeSshRulesReportsFailure() {
        PortForwardingRuleVO removed = sshRule(101L, 4L, 2225);
        Mockito.when(portForwardingRulesDao.listByVm(4L)).thenReturn(List.of(removed));
        Mockito.when(firewallRulesDao.listByIpPurposePortsProtocolAndNotRevoked(IP_ID, 2225, 2225, NetUtils.TCP_PROTO, FirewallRule.Purpose.Firewall))
                .thenReturn(List.of());
        Mockito.when(rulesService.revokePortForwardingRule(101L, true)).thenThrow(new RuntimeException("boom"));

        Assert.assertFalse(worker.removeNodeSshRules(sshIp, List.of(removed), true));
    }

    @Test
    public void isNodeSshPortForwardingRuleIgnoresOtherIpsAndPorts() {
        PortForwardingRuleVO rule = sshRule(101L, 1L, 2222);
        Assert.assertTrue(worker.isNodeSshPortForwardingRule(rule, sshIp));
        Mockito.when(rule.getDestinationPortStart()).thenReturn(6443);
        Assert.assertFalse(worker.isNodeSshPortForwardingRule(rule, sshIp));
        PortForwardingRuleVO otherIp = sshRule(102L, 2L, 2223);
        Mockito.when(otherIp.getSourceIpAddressId()).thenReturn(IP_ID + 1);
        Assert.assertFalse(worker.isNodeSshPortForwardingRule(otherIp, sshIp));
    }
}
