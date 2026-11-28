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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterAddWorkerTest {

    private static final long IP_ID = 10L;
    private static final int START = KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT;

    @Mock
    private KubernetesClusterManagerImpl kubernetesClusterManager;
    @Mock
    private KubernetesCluster kubernetesCluster;
    @Mock
    private FirewallRulesDao firewallRulesDao;
    @Mock
    private Network network;
    @Mock
    private KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    @Mock
    private IpAddress publicIp;

    private KubernetesClusterAddWorker worker;

    @Before
    public void setUp() {
        worker = new KubernetesClusterAddWorker(kubernetesCluster, kubernetesClusterManager);
        ReflectionTestUtils.setField(worker, "firewallRulesDao", firewallRulesDao);
        worker.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        Mockito.when(publicIp.getId()).thenReturn(IP_ID);
        Mockito.lenient().when(network.getVpcId()).thenReturn(null);
    }

    private FirewallRuleVO rule(FirewallRule.Purpose purpose, Integer start, Integer end) {
        FirewallRuleVO rule = Mockito.mock(FirewallRuleVO.class);
        Mockito.lenient().when(rule.getPurpose()).thenReturn(purpose);
        Mockito.lenient().when(rule.getSourcePortStart()).thenReturn(start);
        Mockito.lenient().when(rule.getSourcePortEnd()).thenReturn(end);
        Mockito.lenient().when(rule.getProtocol()).thenReturn("tcp");
        return rule;
    }

    private FirewallRuleVO pf(int port) {
        return rule(FirewallRule.Purpose.PortForwarding, port, port);
    }

    private FirewallRuleVO firewall(Integer start, Integer end) {
        return firewall(start, end, List.of("0.0.0.0/0"), Collections.emptyList());
    }

    private FirewallRuleVO firewall(Integer start, Integer end, List<String> sourceCidrs, List<String> destCidrs) {
        FirewallRuleVO rule = rule(FirewallRule.Purpose.Firewall, start, end);
        Mockito.lenient().when(rule.getSourceCidrList()).thenReturn(sourceCidrs);
        Mockito.lenient().when(rule.getDestinationCidrList()).thenReturn(destCidrs);
        return rule;
    }

    private void mockRules(FirewallRuleVO... rules) {
        Mockito.when(firewallRulesDao.listByIp(IP_ID)).thenReturn(Arrays.asList(rules));
    }

    private Integer freePort() {
        return worker.getFreeNodeSshPort(network, publicIp);
    }

    @Test
    public void freePortIsStartWhenNoRules() {
        mockRules();
        Assert.assertEquals(Integer.valueOf(START), freePort());
    }

    @Test
    public void freePortIsAfterContiguousNodes() {
        FirewallRuleVO nodeRange = firewall(START, START + 2);
        mockRules(nodeRange, pf(START), pf(START + 1), pf(START + 2), pf(START + 3), firewall(START + 3, START + 3));
        Assert.assertEquals(Integer.valueOf(START + 4), freePort());
        Mockito.verify(firewallRulesDao).loadSourceCidrs(nodeRange);
        Mockito.verify(firewallRulesDao).loadDestinationCidrs(nodeRange);
    }

    @Test
    public void freePortFillsGapLeftByRemovedNode() {
        // control + 2 workers on START..START+2, added nodes on START+3 and START+5, START+4 was removed
        mockRules(pf(START + 5), pf(START + 3), pf(START + 1), pf(START), pf(START + 2),
                firewall(START + 5, START + 5), firewall(START + 3, START + 3), firewall(START, START + 2));
        Assert.assertEquals(Integer.valueOf(START + 4), freePort());
    }

    @Test
    public void freePortStepsPastNonFirewallRulesOfAnyKind() {
        FirewallRuleVO udp = pf(START + 12);
        Mockito.lenient().when(udp.getProtocol()).thenReturn("udp");
        mockRules(rule(FirewallRule.Purpose.LoadBalancing, START, START), pf(START + 1),
                rule(FirewallRule.Purpose.PortForwarding, START + 2, START + 10), rule(FirewallRule.Purpose.Vpn, START + 11, START + 11), udp);
        Assert.assertEquals(Integer.valueOf(START + 13), freePort());
    }

    @Test
    public void freePortIgnoresRulesWithoutPortsAndBelowStart() {
        mockRules(firewall(null, null), rule(FirewallRule.Purpose.LoadBalancing, 6443, 6443), pf(22), firewall(1, 1024), firewall(22, 22));
        Assert.assertEquals(Integer.valueOf(START), freePort());
    }

    @Test
    public void freePortSkipsRangeStartingBelowStart() {
        mockRules(firewall(2000, START + 1), pf(START + 3));
        Assert.assertEquals(Integer.valueOf(START + 2), freePort());
    }

    @Test
    public void freePortIgnoresFirewallRangesThatDoNotClash() {
        FirewallRuleVO udp = firewall(START, START + 10);
        Mockito.lenient().when(udp.getProtocol()).thenReturn("udp");
        mockRules(udp, firewall(1024, 65535, List.of("198.51.100.0/24"), Collections.emptyList()),
                firewall(START, START + 10, List.of("0.0.0.0/0"), List.of("10.1.1.5/32")),
                firewall(START, START, List.of("198.51.100.0/24"), Collections.emptyList()));
        Assert.assertEquals(Integer.valueOf(START), freePort());
    }

    @Test
    public void freePortIgnoresFirewallRulesInVpc() {
        Mockito.when(network.getVpcId()).thenReturn(5L);
        mockRules(firewall(START, START), pf(START + 1));
        Assert.assertEquals(Integer.valueOf(START), freePort());
    }

    private KubernetesClusterVmMapVO vmMap(boolean external, boolean etcd) {
        KubernetesClusterVmMapVO map = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.lenient().when(map.isExternalNode()).thenReturn(external);
        Mockito.lenient().when(map.isEtcdNode()).thenReturn(etcd);
        return map;
    }

    @Test
    public void freePortKeepsPositionalPortsOfDefaultNodesReserved() {
        List<KubernetesClusterVmMapVO> maps = List.of(vmMap(false, false), vmMap(false, false), vmMap(false, false),
                vmMap(false, true), vmMap(true, false));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(Mockito.anyLong())).thenReturn(maps);
        mockRules(pf(START + 4));
        Assert.assertEquals(Integer.valueOf(START + 3), freePort());
    }

    @Test
    public void freePortIsNullWhenNoneLeft() {
        mockRules(rule(FirewallRule.Purpose.PortForwarding, START, 65535));
        Assert.assertNull(freePort());
    }
}
