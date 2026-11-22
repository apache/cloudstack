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

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
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
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterAddWorkerTest {

    private static final long IP_ID = 10L;
    private static final long VM_ID = 20L;
    private static final int SSH_PORT = 2224;

    @Mock
    private KubernetesClusterManagerImpl kubernetesClusterManager;
    @Mock
    private KubernetesCluster kubernetesCluster;
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
    private IpAddress publicIp;
    @Mock
    private KubernetesClusterVmMapDao kubernetesClusterVmMapDao;

    private KubernetesClusterAddWorker worker;

    @Before
    public void setUp() {
        worker = new KubernetesClusterAddWorker(kubernetesCluster, kubernetesClusterManager);
        ReflectionTestUtils.setField(worker, "firewallRulesDao", firewallRulesDao);
        worker.portForwardingRulesDao = portForwardingRulesDao;
        worker.firewallService = firewallService;
        worker.rulesService = rulesService;
        worker.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        Mockito.lenient().when(publicIp.getAddress()).thenReturn(new Ip("192.0.2.10"));
        Mockito.when(publicIp.getId()).thenReturn(IP_ID);
        Mockito.when(network.getVpcId()).thenReturn(null);
    }

    private PortForwardingRuleVO pfRule(long id, long ipId, int sourcePort, int destPort) {
        PortForwardingRuleVO rule = Mockito.mock(PortForwardingRuleVO.class);
        Mockito.lenient().when(rule.getId()).thenReturn(id);
        Mockito.lenient().when(rule.getUuid()).thenReturn("uuid-" + id);
        Mockito.lenient().when(rule.getPurpose()).thenReturn(FirewallRule.Purpose.PortForwarding);
        Mockito.lenient().when(rule.getSourceIpAddressId()).thenReturn(ipId);
        Mockito.lenient().when(rule.getSourcePortStart()).thenReturn(sourcePort);
        Mockito.lenient().when(rule.getDestinationPortStart()).thenReturn(destPort);
        Mockito.lenient().when(rule.getSourcePortEnd()).thenReturn(sourcePort);
        Mockito.lenient().when(rule.getDestinationPortEnd()).thenReturn(destPort);
        Mockito.lenient().when(rule.getProtocol()).thenReturn("tcp");
        Mockito.lenient().when(rule.getVirtualMachineId()).thenReturn(VM_ID);
        return rule;
    }

    private FirewallRuleVO firewallRule(long id) {
        FirewallRuleVO rule = Mockito.mock(FirewallRuleVO.class);
        Mockito.when(rule.getId()).thenReturn(id);
        Mockito.lenient().when(rule.getUuid()).thenReturn("uuid-" + id);
        Mockito.lenient().when(rule.getSourcePortStart()).thenReturn(SSH_PORT);
        Mockito.lenient().when(rule.getPurpose()).thenReturn(FirewallRule.Purpose.Firewall);
        return rule;
    }

    private void mockFirewallRules(FirewallRuleVO... rules) {
        Mockito.when(firewallRulesDao.listByIpPurposePortsProtocolAndNotRevoked(IP_ID, SSH_PORT, SSH_PORT, "tcp",
                FirewallRule.Purpose.Firewall)).thenReturn(List.of(rules));
    }

    @Test
    public void revertNetworkRulesRemovesFirewallThenPortForwardingRule() {
        PortForwardingRuleVO pf = pfRule(1L, IP_ID, SSH_PORT, 22);
        Mockito.when(portForwardingRulesDao.listByVm(VM_ID)).thenReturn(List.of(pf));
        mockFirewallRules(firewallRule(2L));
        Mockito.when(rulesService.revokePortForwardingRule(1L, true)).thenReturn(true);
        Mockito.when(firewallService.revokeIngressFirewallRule(2L, true)).thenReturn(true);

        worker.revertNetworkRules(network, publicIp, VM_ID, SSH_PORT, Set.of());

        InOrder inOrder = Mockito.inOrder(rulesService, firewallService);
        inOrder.verify(firewallService).revokeIngressFirewallRule(2L, true);
        inOrder.verify(rulesService).revokePortForwardingRule(1L, true);
    }

    @Test
    public void revertNetworkRulesKeepsRulesThatExistedBefore() {
        Mockito.when(portForwardingRulesDao.listByVm(VM_ID)).thenReturn(List.of());
        mockFirewallRules(firewallRule(2L));

        worker.revertNetworkRules(network, publicIp, VM_ID, SSH_PORT, Set.of(2L));

        Mockito.verifyNoInteractions(firewallService, rulesService);
    }

    @Test
    public void revertNetworkRulesLeavesOtherPortForwardingRules() {
        PortForwardingRuleVO otherPort = pfRule(3L, IP_ID, 8080, 22);
        PortForwardingRuleVO otherIp = pfRule(4L, 99L, SSH_PORT, 22);
        PortForwardingRuleVO otherDest = pfRule(5L, IP_ID, SSH_PORT, 80);
        Mockito.when(portForwardingRulesDao.listByVm(VM_ID)).thenReturn(List.of(otherPort, otherIp, otherDest));
        mockFirewallRules();

        worker.revertNetworkRules(network, publicIp, VM_ID, SSH_PORT, Set.of());

        Mockito.verifyNoInteractions(rulesService, firewallService);
    }

    @Test
    public void revertNetworkRulesKeepsPortForwardingRuleWhenFirewallRevokeFails() {
        PortForwardingRuleVO pf = pfRule(1L, IP_ID, SSH_PORT, 22);
        Mockito.when(portForwardingRulesDao.listByVm(VM_ID)).thenReturn(List.of(pf));
        mockFirewallRules(firewallRule(2L));
        Mockito.when(firewallService.revokeIngressFirewallRule(2L, true)).thenThrow(new CloudRuntimeException("apply failed"));

        worker.revertNetworkRules(network, publicIp, VM_ID, SSH_PORT, Set.of());

        Mockito.verifyNoInteractions(rulesService);
    }

    private void mockNodeCounts() {
        Mockito.when(kubernetesCluster.getTotalNodeCount()).thenReturn(2L);
        Mockito.when(kubernetesCluster.getEtcdNodeCount()).thenReturn(0L);
    }

    private KubernetesClusterAddWorker spyForSetup() {
        mockNodeCounts();
        return Mockito.spy(worker);
    }

    @Test
    public void validateAndSetupNodeFirewallConflictKeepsExistingRule() throws Exception {
        KubernetesClusterAddWorker spy = spyForSetup();
        Mockito.when(portForwardingRulesDao.listByVm(VM_ID)).thenReturn(List.of());
        mockFirewallRules(firewallRule(7L));
        Mockito.doThrow(new NetworkRuleConflictException("conflict")).when(spy)
                .provisionFirewallRules(publicIp, null, SSH_PORT, SSH_PORT);

        Assert.assertFalse(spy.validateAndSetupNode(network, publicIp, null, VM_ID, 0, "").first());

        Mockito.verifyNoInteractions(firewallService, rulesService);
    }

    @Test
    public void validateAndSetupNodeRemovesRulesCreatedBeforeFailure() throws Exception {
        KubernetesClusterAddWorker spy = spyForSetup();
        Mockito.when(portForwardingRulesDao.listByVm(VM_ID)).thenReturn(List.of());
        FirewallRuleVO existing = firewallRule(7L);
        mockFirewallRules(existing);
        Mockito.doAnswer(invocation -> {
            mockFirewallRules(existing, firewallRule(2L));
            return null;
        }).when(spy).provisionFirewallRules(publicIp, null, SSH_PORT, SSH_PORT);
        Mockito.doThrow(new NetworkRuleConflictException("conflict")).when(spy)
                .provisionPublicIpPortForwardingRule(publicIp, network, null, VM_ID, SSH_PORT, 22);
        Mockito.when(firewallService.revokeIngressFirewallRule(2L, true)).thenReturn(true);

        Assert.assertFalse(spy.validateAndSetupNode(network, publicIp, null, VM_ID, 0, "").first());

        Mockito.verify(firewallService).revokeIngressFirewallRule(2L, true);
        Mockito.verify(firewallService, Mockito.never()).revokeIngressFirewallRule(7L, true);
    }

    @Test
    public void revertNetworkRulesInVpcSkipsFirewallRules() {
        Mockito.when(network.getVpcId()).thenReturn(5L);
        Mockito.when(portForwardingRulesDao.listByVm(VM_ID)).thenReturn(List.of());

        worker.revertNetworkRules(network, publicIp, VM_ID, SSH_PORT, Set.of());

        Mockito.verifyNoInteractions(firewallRulesDao, firewallService);
    }

    @Test
    public void listNodeSshRuleIdsReturnsPortForwardingAndFirewallRuleIds() {
        PortForwardingRuleVO pf = pfRule(1L, IP_ID, SSH_PORT, 22);
        Mockito.when(portForwardingRulesDao.listByVm(VM_ID)).thenReturn(List.of(pf));
        mockFirewallRules(firewallRule(2L));

        Assert.assertEquals(Set.of(1L, 2L), worker.listNodeSshRuleIds(network, publicIp, VM_ID, SSH_PORT));
    }

    @Test
    public void validateAndSetupNodeRemovesNewPortForwardingRuleWhenApplyFails() throws Exception {
        KubernetesClusterAddWorker spy = spyForSetup();
        PortForwardingRuleVO existing = pfRule(6L, IP_ID, SSH_PORT, 22);
        PortForwardingRuleVO created = pfRule(1L, IP_ID, SSH_PORT, 22);
        Mockito.when(portForwardingRulesDao.listByVm(VM_ID)).thenReturn(List.of(existing));
        mockFirewallRules();
        Mockito.doNothing().when(spy).provisionFirewallRules(publicIp, null, SSH_PORT, SSH_PORT);
        Mockito.doAnswer(invocation -> {
            Mockito.when(portForwardingRulesDao.listByVm(VM_ID)).thenReturn(List.of(existing, created));
            throw new ResourceUnavailableException("apply failed", Network.class, 1L);
        }).when(spy).provisionPublicIpPortForwardingRule(publicIp, network, null, VM_ID, SSH_PORT, 22);
        Mockito.when(rulesService.revokePortForwardingRule(1L, true)).thenReturn(true);

        Assert.assertFalse(spy.validateAndSetupNode(network, publicIp, null, VM_ID, 0, "").first());

        Mockito.verify(rulesService).revokePortForwardingRule(1L, true);
        Mockito.verify(rulesService, Mockito.never()).revokePortForwardingRule(6L, true);
    }

    private FirewallRuleVO ipRule(long id, FirewallRule.Purpose purpose, int start, int end, String... cidrs) {
        FirewallRuleVO rule = Mockito.mock(FirewallRuleVO.class);
        Mockito.lenient().when(rule.getId()).thenReturn(id);
        Mockito.lenient().when(rule.getUuid()).thenReturn("uuid-" + id);
        Mockito.lenient().when(rule.getPurpose()).thenReturn(purpose);
        Mockito.lenient().when(rule.getProtocol()).thenReturn("tcp");
        Mockito.lenient().when(rule.getSourcePortStart()).thenReturn(start);
        Mockito.lenient().when(rule.getSourcePortEnd()).thenReturn(end);
        Mockito.lenient().when(rule.getSourceCidrList()).thenReturn(List.of(cidrs));
        return rule;
    }

    private void mockIpRules(FirewallRuleVO... rules) {
        Mockito.when(firewallRulesDao.listByIpAndPurposeAndNotRevoked(IP_ID, null)).thenReturn(List.of(rules));
    }

    private void mockLeftover() {
        PortForwardingRuleVO pf = pfRule(1L, IP_ID, SSH_PORT, 22);
        Mockito.when(portForwardingRulesDao.findById(1L)).thenReturn(pf);
        mockIpRules(ipRule(1L, FirewallRule.Purpose.PortForwarding, SSH_PORT, SSH_PORT),
                ipRule(2L, FirewallRule.Purpose.Firewall, SSH_PORT, SSH_PORT, "0.0.0.0/0"));
    }

    @Test
    public void removeLeftoverNodeSshRulesRemovesOwnLeftover() {
        mockNodeCounts();
        mockLeftover();
        Mockito.when(rulesService.revokePortForwardingRule(1L, true)).thenReturn(true);
        Mockito.when(firewallService.revokeIngressFirewallRule(2L, true)).thenReturn(true);

        worker.removeLeftoverNodeSshRules(network, publicIp, List.of(VM_ID));

        InOrder inOrder = Mockito.inOrder(rulesService, firewallService);
        inOrder.verify(firewallService).revokeIngressFirewallRule(2L, true);
        inOrder.verify(rulesService).revokePortForwardingRule(1L, true);
    }

    @Test
    public void removeLeftoverNodeSshRulesRejectsRuleForVmNotInRequest() {
        mockNodeCounts();
        mockLeftover();

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class,
                () -> worker.removeLeftoverNodeSshRules(network, publicIp, List.of(99L)));

        Assert.assertTrue(e.getMessage().contains("192.0.2.10"));
        Assert.assertTrue(e.getMessage().contains(String.valueOf(SSH_PORT)));
        Assert.assertTrue(e.getMessage().contains("uuid-1 (PortForwarding, ports 2224-2224)"));
        Assert.assertTrue(e.getMessage().contains("uuid-2 (Firewall, ports 2224-2224)"));
        Mockito.verifyNoInteractions(rulesService, firewallService);
    }

    @Test
    public void removeLeftoverNodeSshRulesRejectsRuleForClusterNode() {
        mockNodeCounts();
        mockLeftover();
        Mockito.when(kubernetesClusterVmMapDao.findByVmId(VM_ID)).thenReturn(Mockito.mock(KubernetesClusterVmMapVO.class));

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class,
                () -> worker.removeLeftoverNodeSshRules(network, publicIp, List.of(VM_ID)));

        Assert.assertTrue(e.getMessage().contains("uuid-1 (PortForwarding, ports 2224-2224, forwards to a Kubernetes cluster node)"));
        Assert.assertTrue(e.getMessage().contains("must not be removed"));
        Mockito.verifyNoInteractions(rulesService, firewallService);
    }

    @Test
    public void removeLeftoverNodeSshRulesKeepsUserFirewallRuleNextToLeftover() {
        mockNodeCounts();
        PortForwardingRuleVO pf = pfRule(1L, IP_ID, SSH_PORT, 22);
        Mockito.when(portForwardingRulesDao.findById(1L)).thenReturn(pf);
        mockIpRules(ipRule(1L, FirewallRule.Purpose.PortForwarding, SSH_PORT, SSH_PORT),
                ipRule(2L, FirewallRule.Purpose.Firewall, SSH_PORT, SSH_PORT, "203.0.113.0/24"));
        Mockito.when(rulesService.revokePortForwardingRule(1L, true)).thenReturn(true);

        worker.removeLeftoverNodeSshRules(network, publicIp, List.of(VM_ID));

        Mockito.verify(rulesService).revokePortForwardingRule(1L, true);
        Mockito.verifyNoInteractions(firewallService);
    }

    @Test
    public void removeLeftoverNodeSshRulesIgnoresNonClashingFirewallRange() {
        mockNodeCounts();
        mockIpRules(ipRule(2L, FirewallRule.Purpose.Firewall, 2000, 3000, "10.0.0.0/8"));

        worker.removeLeftoverNodeSshRules(network, publicIp, List.of(VM_ID));

        Mockito.verifyNoInteractions(rulesService, firewallService);
    }

    @Test
    public void removeLeftoverNodeSshRulesRejectsPortRangeRules() {
        mockNodeCounts();
        PortForwardingRuleVO pf = pfRule(1L, IP_ID, 2220, 22);
        Mockito.when(pf.getSourcePortEnd()).thenReturn(2230);
        Mockito.when(portForwardingRulesDao.findById(1L)).thenReturn(pf);
        mockIpRules(ipRule(1L, FirewallRule.Purpose.PortForwarding, 2220, 2230),
                ipRule(3L, FirewallRule.Purpose.LoadBalancing, SSH_PORT, SSH_PORT));

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class,
                () -> worker.removeLeftoverNodeSshRules(network, publicIp, List.of(VM_ID)));

        Assert.assertTrue(e.getMessage().contains("uuid-1 (PortForwarding, ports 2220-2230)"));
        Assert.assertTrue(e.getMessage().contains("uuid-3 (LoadBalancing, ports 2224-2224)"));
        Mockito.verifyNoInteractions(rulesService, firewallService);
    }

    @Test
    public void removeLeftoverNodeSshRulesRejectsFirewallRuleWithoutPortForwarding() {
        mockNodeCounts();
        mockIpRules(ipRule(2L, FirewallRule.Purpose.Firewall, SSH_PORT, SSH_PORT, "0.0.0.0/0"));

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class,
                () -> worker.removeLeftoverNodeSshRules(network, publicIp, List.of(VM_ID)));

        Assert.assertTrue(e.getMessage().contains("uuid-2 (Firewall, ports 2224-2224)"));
        Mockito.verifyNoInteractions(firewallService);
    }

    @Test
    public void removeLeftoverNodeSshRulesIgnoresOtherPortsAndProtocols() {
        mockNodeCounts();
        FirewallRuleVO udp = ipRule(4L, FirewallRule.Purpose.Firewall, SSH_PORT, SSH_PORT, "0.0.0.0/0");
        Mockito.when(udp.getProtocol()).thenReturn("udp");
        mockIpRules(ipRule(5L, FirewallRule.Purpose.Firewall, 8080, 8080, "0.0.0.0/0"), udp);

        worker.removeLeftoverNodeSshRules(network, publicIp, List.of(VM_ID));

        Mockito.verifyNoInteractions(rulesService, firewallService);
    }

    @Test
    public void removeLeftoverNodeSshRulesKeepsPortForwardingWhenFirewallRevokeFails() {
        mockNodeCounts();
        mockLeftover();
        Mockito.when(firewallService.revokeIngressFirewallRule(2L, true)).thenThrow(new CloudRuntimeException("apply failed"));

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class,
                () -> worker.removeLeftoverNodeSshRules(network, publicIp, List.of(VM_ID)));

        Assert.assertTrue(e.getMessage().contains("uuid-2"));
        Mockito.verifyNoInteractions(rulesService);
    }

    @Test
    public void removeLeftoverNodeSshRulesRejectsCidrRestrictedPortForwarding() {
        mockNodeCounts();
        PortForwardingRuleVO pf = pfRule(1L, IP_ID, SSH_PORT, 22);
        Mockito.when(portForwardingRulesDao.findById(1L)).thenReturn(pf);
        mockIpRules(ipRule(1L, FirewallRule.Purpose.PortForwarding, SSH_PORT, SSH_PORT, "203.0.113.0/24"));

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class,
                () -> worker.removeLeftoverNodeSshRules(network, publicIp, List.of(VM_ID)));

        Assert.assertTrue(e.getMessage().contains("uuid-1 (PortForwarding, ports 2224-2224)"));
        Mockito.verifyNoInteractions(rulesService, firewallService);
    }
}
