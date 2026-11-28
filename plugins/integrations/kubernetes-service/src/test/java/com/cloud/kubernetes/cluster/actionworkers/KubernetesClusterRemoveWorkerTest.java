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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
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
import com.cloud.vm.UserVmVO;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterRemoveWorkerTest {

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
    private UserVmVO vm;

    private KubernetesClusterRemoveWorker worker;

    @Before
    public void setUp() {
        worker = new KubernetesClusterRemoveWorker(kubernetesCluster, kubernetesClusterManager);
        ReflectionTestUtils.setField(worker, "firewallRulesDao", firewallRulesDao);
        worker.portForwardingRulesDao = portForwardingRulesDao;
        worker.firewallService = firewallService;
        worker.rulesService = rulesService;
        Mockito.lenient().when(publicIp.getId()).thenReturn(IP_ID);
        Mockito.lenient().when(network.getVpcId()).thenReturn(null);
        Mockito.lenient().when(vm.getName()).thenReturn("node-1");
        Mockito.lenient().when(firewallService.revokeIngressFirewallRule(Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(true);
        Mockito.lenient().when(rulesService.revokePortForwardingRule(Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(true);
    }

    private PortForwardingRuleVO pfRule(long id, long ipId, int sourcePort, int destPort) {
        PortForwardingRuleVO rule = Mockito.mock(PortForwardingRuleVO.class);
        Mockito.lenient().when(rule.getId()).thenReturn(id);
        Mockito.lenient().when(rule.getSourceIpAddressId()).thenReturn(ipId);
        Mockito.lenient().when(rule.getSourcePortStart()).thenReturn(sourcePort);
        Mockito.lenient().when(rule.getSourcePortEnd()).thenReturn(sourcePort);
        Mockito.lenient().when(rule.getDestinationPortStart()).thenReturn(destPort);
        Mockito.lenient().when(rule.getDestinationPortEnd()).thenReturn(destPort);
        Mockito.lenient().when(rule.getProtocol()).thenReturn("tcp");
        Mockito.lenient().when(rule.getPurpose()).thenReturn(FirewallRule.Purpose.PortForwarding);
        Mockito.lenient().when(rule.getState()).thenReturn(FirewallRule.State.Active);
        return rule;
    }

    private FirewallRuleVO firewallRule(long id) {
        FirewallRuleVO rule = Mockito.mock(FirewallRuleVO.class);
        Mockito.lenient().when(rule.getId()).thenReturn(id);
        Mockito.lenient().when(rule.getPurpose()).thenReturn(FirewallRule.Purpose.Firewall);
        return rule;
    }

    private void mockFirewallRules(int port, FirewallRuleVO... rules) {
        Mockito.when(firewallRulesDao.listByIpPurposePortsProtocolAndNotRevoked(IP_ID, port, port, "tcp", FirewallRule.Purpose.Firewall))
                .thenReturn(List.of(rules));
    }

    @Test
    public void removeNodePortForwardingRulesRevokesFirewallRuleOnPublicIpThenPfRule() {
        PortForwardingRuleVO pf = pfRule(1L, IP_ID, SSH_PORT, 22);
        FirewallRuleVO fw = firewallRule(2L);
        Mockito.when(portForwardingRulesDao.listByVm(VM_ID)).thenReturn(List.of(pf));
        mockFirewallRules(SSH_PORT, fw);

        Assert.assertTrue(worker.removeNodePortForwardingRules(VM_ID, network, publicIp, vm));

        InOrder order = Mockito.inOrder(firewallService, rulesService);
        order.verify(firewallService).revokeIngressFirewallRule(2L, true);
        order.verify(rulesService).revokePortForwardingRule(1L, true);
        Mockito.verify(firewallRulesDao, Mockito.never()).findByNetworkIdAndPorts(Mockito.anyLong(), Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    public void removeNodePortForwardingRulesLeavesPfRulesThatAreNotTheNodeSshRule() {
        PortForwardingRuleVO otherIp = pfRule(1L, IP_ID + 1, SSH_PORT, 22);
        PortForwardingRuleVO otherDestPort = pfRule(3L, IP_ID, 8080, 80);
        PortForwardingRuleVO udp = pfRule(4L, IP_ID, SSH_PORT + 1, 22);
        Mockito.when(udp.getProtocol()).thenReturn("udp");
        PortForwardingRuleVO range = pfRule(5L, IP_ID, SSH_PORT + 2, 22);
        Mockito.when(range.getSourcePortEnd()).thenReturn(SSH_PORT + 3);
        PortForwardingRuleVO revoked = pfRule(6L, IP_ID, SSH_PORT + 4, 22);
        Mockito.when(revoked.getState()).thenReturn(FirewallRule.State.Revoke);
        Mockito.when(portForwardingRulesDao.listByVm(VM_ID)).thenReturn(List.of(otherIp, otherDestPort, udp, range, revoked));

        Assert.assertTrue(worker.removeNodePortForwardingRules(VM_ID, network, publicIp, vm));

        Mockito.verifyNoInteractions(firewallService, rulesService);
        Mockito.verify(firewallRulesDao, Mockito.never()).listByIpPurposePortsProtocolAndNotRevoked(
                Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void removeNodePortForwardingRulesOnlyRevokesTheNodeSshRuleAmongOthers() {
        PortForwardingRuleVO other = pfRule(1L, IP_ID, 8080, 80);
        PortForwardingRuleVO destRange = pfRule(3L, IP_ID, SSH_PORT + 1, 22);
        Mockito.when(destRange.getDestinationPortEnd()).thenReturn(23);
        PortForwardingRuleVO ssh = pfRule(5L, IP_ID, SSH_PORT, 22);
        FirewallRuleVO fw = firewallRule(6L);
        Mockito.when(portForwardingRulesDao.listByVm(VM_ID)).thenReturn(List.of(other, destRange, ssh));
        mockFirewallRules(SSH_PORT, fw);

        Assert.assertTrue(worker.removeNodePortForwardingRules(VM_ID, network, publicIp, vm));

        Mockito.verify(firewallService).revokeIngressFirewallRule(6L, true);
        Mockito.verify(rulesService).revokePortForwardingRule(5L, true);
        Mockito.verifyNoMoreInteractions(firewallService, rulesService);
    }

    @Test
    public void removeNodePortForwardingRulesWithoutFirewallRuleStillRevokesPfRule() {
        PortForwardingRuleVO pf = pfRule(1L, IP_ID, SSH_PORT, 22);
        Mockito.when(portForwardingRulesDao.listByVm(VM_ID)).thenReturn(List.of(pf));
        mockFirewallRules(SSH_PORT);

        Assert.assertTrue(worker.removeNodePortForwardingRules(VM_ID, network, publicIp, vm));

        Mockito.verifyNoInteractions(firewallService);
        Mockito.verify(rulesService).revokePortForwardingRule(1L, true);
    }

    @Test
    public void removeNodePortForwardingRulesTriesEveryRuleWhenFirewallRevokeThrows() {
        PortForwardingRuleVO pf = pfRule(1L, IP_ID, SSH_PORT, 22);
        FirewallRuleVO fw = firewallRule(2L);
        FirewallRuleVO fw2 = firewallRule(4L);
        Mockito.when(portForwardingRulesDao.listByVm(VM_ID)).thenReturn(List.of(pf));
        mockFirewallRules(SSH_PORT, fw, fw2);
        Mockito.when(firewallService.revokeIngressFirewallRule(2L, true)).thenThrow(new CloudRuntimeException("failed"));

        Assert.assertFalse(worker.removeNodePortForwardingRules(VM_ID, network, publicIp, vm));

        Mockito.verify(firewallService).revokeIngressFirewallRule(4L, true);
        Mockito.verify(rulesService).revokePortForwardingRule(1L, true);
    }

    @Test
    public void removeNodePortForwardingRulesRevokesPfRuleWhenFirewallLookupFails() {
        PortForwardingRuleVO pf = pfRule(1L, IP_ID, SSH_PORT, 22);
        Mockito.when(portForwardingRulesDao.listByVm(VM_ID)).thenReturn(List.of(pf));
        Mockito.when(firewallRulesDao.listByIpPurposePortsProtocolAndNotRevoked(IP_ID, SSH_PORT, SSH_PORT, "tcp", FirewallRule.Purpose.Firewall))
                .thenThrow(new CloudRuntimeException("db error"));

        Assert.assertFalse(worker.removeNodePortForwardingRules(VM_ID, network, publicIp, vm));

        Mockito.verify(rulesService).revokePortForwardingRule(1L, true);
    }

    @Test
    public void removeNodePortForwardingRulesReportsFailedRevoke() {
        PortForwardingRuleVO pf = pfRule(1L, IP_ID, SSH_PORT, 22);
        FirewallRuleVO fw = firewallRule(2L);
        Mockito.when(portForwardingRulesDao.listByVm(VM_ID)).thenReturn(List.of(pf));
        mockFirewallRules(SSH_PORT, fw);
        Mockito.when(rulesService.revokePortForwardingRule(1L, true)).thenReturn(false);

        Assert.assertFalse(worker.removeNodePortForwardingRules(VM_ID, network, publicIp, vm));

        Mockito.verify(firewallService).revokeIngressFirewallRule(2L, true);
    }

    @Test
    public void removeNodePortForwardingRulesInVpcOnlyRevokesPfRule() {
        PortForwardingRuleVO pf = pfRule(1L, IP_ID, SSH_PORT, 22);
        Mockito.when(network.getVpcId()).thenReturn(5L);
        Mockito.when(portForwardingRulesDao.listByVm(VM_ID)).thenReturn(List.of(pf));

        Assert.assertTrue(worker.removeNodePortForwardingRules(VM_ID, network, publicIp, vm));

        Mockito.verifyNoInteractions(firewallService, firewallRulesDao);
        Mockito.verify(rulesService).revokePortForwardingRule(1L, true);
    }
}
