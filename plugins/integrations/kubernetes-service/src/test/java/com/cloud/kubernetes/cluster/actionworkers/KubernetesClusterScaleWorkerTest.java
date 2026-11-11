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

import com.cloud.exception.ManagementServerException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.RulesService;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;
import org.apache.cloudstack.context.CallContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.CONTROL;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.DEFAULT;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterScaleWorkerTest {

    @Mock
    private KubernetesCluster kubernetesCluster;
    @Mock
    private KubernetesClusterManagerImpl clusterManager;
    @Mock
    private ServiceOfferingDao serviceOfferingDao;
    @Mock
    private KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    @Mock
    private UserVmDao userVmDao;

    private KubernetesClusterScaleWorker worker;

    private static final Long defaultOfferingId = 1L;

    @Before
    public void setUp() {
        worker = new KubernetesClusterScaleWorker(kubernetesCluster, clusterManager);
        worker.serviceOfferingDao = serviceOfferingDao;
        worker.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        worker.userVmDao = userVmDao;
    }

    @Test
    public void testCalculateNewClusterCountAndCapacityAllNodesScaleSize() {
        long controlNodes = 3L;
        long etcdNodes = 2L;
        Mockito.when(kubernetesCluster.getControlNodeCount()).thenReturn(controlNodes);
        Mockito.when(kubernetesCluster.getEtcdNodeCount()).thenReturn(etcdNodes);

        ServiceOffering newOffering = Mockito.mock(ServiceOffering.class);
        int newCores = 4;
        int newMemory = 4096;
        Mockito.when(newOffering.getCpu()).thenReturn(newCores);
        Mockito.when(newOffering.getRamSize()).thenReturn(newMemory);

        long newWorkerSize = 4L;
        Pair<Long, Long> newClusterCapacity = worker.calculateNewClusterCountAndCapacity(newWorkerSize, DEFAULT, newOffering);

        long expectedCores = (newCores * newWorkerSize) + (newCores * controlNodes) + (newCores * etcdNodes);
        long expectedMemory = (newMemory * newWorkerSize) + (newMemory * controlNodes) + (newMemory * etcdNodes);
        Assert.assertEquals(expectedCores, newClusterCapacity.first().longValue());
        Assert.assertEquals(expectedMemory, newClusterCapacity.second().longValue());
    }

    @Test
    public void testCalculateNewClusterCountAndCapacityNodeTypeScaleControlOffering() {
        long controlNodes = 2L;
        long kubernetesClusterId = 10L;
        Mockito.when(kubernetesCluster.getId()).thenReturn(kubernetesClusterId);
        Mockito.when(kubernetesCluster.getControlNodeCount()).thenReturn(controlNodes);

        ServiceOfferingVO existingOffering = Mockito.mock(ServiceOfferingVO.class);
        int existingCores = 2;
        int existingMemory = 2048;
        Mockito.when(existingOffering.getCpu()).thenReturn(existingCores);
        Mockito.when(existingOffering.getRamSize()).thenReturn(existingMemory);
        int remainingClusterCpu = 8;
        int remainingClusterMemory = 12288;
        Mockito.when(kubernetesCluster.getCores()).thenReturn(remainingClusterCpu + (controlNodes * existingCores));
        Mockito.when(kubernetesCluster.getMemory()).thenReturn(remainingClusterMemory + (controlNodes * existingMemory));

        Mockito.when(serviceOfferingDao.findById(1L)).thenReturn(existingOffering);

        ServiceOfferingVO newOffering = Mockito.mock(ServiceOfferingVO.class);
        int newCores = 4;
        int newMemory = 2048;
        Mockito.when(newOffering.getCpu()).thenReturn(newCores);
        Mockito.when(newOffering.getRamSize()).thenReturn(newMemory);

        KubernetesClusterVmMapVO controlNodeVM1 = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(controlNodeVM1.getVmId()).thenReturn(10L);
        UserVmVO userVmVO = Mockito.mock(UserVmVO.class);
        Mockito.when(userVmVO.getServiceOfferingId()).thenReturn(defaultOfferingId);
        Mockito.when(userVmDao.findById(10L)).thenReturn(userVmVO);
        Mockito.when(kubernetesClusterVmMapDao.listByClusterIdAndVmType(kubernetesClusterId, CONTROL)).thenReturn(List.of(controlNodeVM1));
        Pair<Long, Long> newClusterCapacity = worker.calculateNewClusterCountAndCapacity(null, CONTROL, newOffering);

        long expectedCores = remainingClusterCpu + (controlNodes * newCores);
        long expectedMemory = remainingClusterMemory + (controlNodes * newMemory);
        Assert.assertEquals(expectedCores, newClusterCapacity.first().longValue());
        Assert.assertEquals(expectedMemory, newClusterCapacity.second().longValue());
    }


    @Test
    public void testGetWorkerNodesToRemoveForDownsize_singleRemoval() {
        KubernetesCluster kubernetesCluster = Mockito.mock(KubernetesCluster.class);
        KubernetesClusterManagerImpl clusterManager = Mockito.mock(KubernetesClusterManagerImpl.class);
        KubernetesClusterScaleWorker worker = new KubernetesClusterScaleWorker(kubernetesCluster, new java.util.HashMap<>(), 2L, null, false, null, null, clusterManager);
        KubernetesClusterScaleWorker spyWorker = Mockito.spy(worker);

        KubernetesClusterVmMapVO vm1 = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(vm1.isExternalNode()).thenReturn(false);
        Mockito.when(vm1.isControlNode()).thenReturn(false);
        Mockito.when(vm1.isEtcdNode()).thenReturn(false);

        KubernetesClusterVmMapVO vm2 = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(vm2.isExternalNode()).thenReturn(false);
        Mockito.when(vm2.isControlNode()).thenReturn(false);
        Mockito.when(vm2.isEtcdNode()).thenReturn(false);

        KubernetesClusterVmMapVO vm3 = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(vm3.isExternalNode()).thenReturn(false);
        Mockito.when(vm3.isControlNode()).thenReturn(false);
        Mockito.when(vm3.isEtcdNode()).thenReturn(false);

        Mockito.doReturn(Arrays.asList(vm1, vm2, vm3)).when(spyWorker).getKubernetesClusterVMMaps();

        List<KubernetesClusterVmMapVO> toRemove = spyWorker.getWorkerNodesToRemove();

        Assert.assertEquals(1, toRemove.size());
        Assert.assertSame(vm3, toRemove.get(0));
    }

    @Test
    public void testGetWorkerNodesToRemoveForDownsize_noRemoval() {
        KubernetesCluster kubernetesCluster = Mockito.mock(KubernetesCluster.class);

        KubernetesClusterScaleWorker worker = new KubernetesClusterScaleWorker(kubernetesCluster, new java.util.HashMap<>(), 3L, null, false, null, null, clusterManager);
        KubernetesClusterScaleWorker spyWorker = Mockito.spy(worker);

        KubernetesClusterVmMapVO vm1 = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(vm1.isExternalNode()).thenReturn(false);
        Mockito.when(vm1.isControlNode()).thenReturn(false);
        Mockito.when(vm1.isEtcdNode()).thenReturn(false);

        KubernetesClusterVmMapVO vm2 = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(vm2.isExternalNode()).thenReturn(false);
        Mockito.when(vm2.isControlNode()).thenReturn(false);
        Mockito.when(vm2.isEtcdNode()).thenReturn(false);

        KubernetesClusterVmMapVO vm3 = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(vm3.isExternalNode()).thenReturn(false);
        Mockito.when(vm3.isControlNode()).thenReturn(false);
        Mockito.when(vm3.isEtcdNode()).thenReturn(false);

        Mockito.doReturn(Arrays.asList(vm1, vm2, vm3)).when(spyWorker).getKubernetesClusterVMMaps();

        List<KubernetesClusterVmMapVO> toRemove = spyWorker.getWorkerNodesToRemove();

        Assert.assertTrue(toRemove.isEmpty());
    }

    private static final long CLUSTER_ID = 1L;
    private static final long NETWORK_ID = 10L;
    private static final long PUBLIC_IP_ID = 20L;
    private static final long OTHER_PUBLIC_IP_ID = 21L;
    private static final long CONTROL_VM_ID = 100L;
    private static final long WORKER_VM_ID = 101L;
    private static final long EXTERNAL_VM_ID = 102L;
    private static final long NEW_WORKER_VM_ID = 103L;
    private static final long SECOND_EXTERNAL_VM_ID = 104L;
    private static final long ETCD_VM_ID = 105L;
    private static final long THIRD_WORKER_VM_ID = 106L;
    private static final int API_PORT = KubernetesClusterActionWorker.CLUSTER_API_PORT;
    private static final int SSH_PORT = KubernetesClusterActionWorker.DEFAULT_SSH_PORT;
    private static final int ETCD_SSH_PORT = KubernetesClusterService.KubernetesEtcdNodeStartPort.value();

    // VM map of the cluster and the rules of its network; all firewall rules are on the source NAT IP
    private final List<KubernetesClusterVmMapVO> vmMaps = new ArrayList<>();
    private final List<FirewallRuleVO> firewallRules = new ArrayList<>();
    private final List<PortForwardingRuleVO> portForwardingRules = new ArrayList<>();
    private long lastRuleId = 0;
    private IpAddress publicIp;

    private void addVm(long vmId, boolean controlNode, boolean externalNode, boolean etcdNode) {
        KubernetesClusterVmMapVO vmMap = new KubernetesClusterVmMapVO(CLUSTER_ID, vmId, controlNode);
        vmMap.setExternalNode(externalNode);
        vmMap.setEtcdNode(etcdNode);
        vmMaps.add(vmMap);
    }

    // As when the VM of a removed node is expunged
    private void removeVm(long vmId) {
        vmMaps.removeIf(vmMap -> vmMap.getVmId() == vmId);
        portForwardingRules.removeIf(rule -> rule.getVirtualMachineId() == vmId);
    }

    private void addFirewallRule(int startPort, int endPort) {
        FirewallRuleVO rule = new FirewallRuleVO(null, PUBLIC_IP_ID, startPort, endPort, NetUtils.TCP_PROTO, NETWORK_ID, 2L, 3L,
                FirewallRule.Purpose.Firewall, List.of("0.0.0.0/0"), null, null, null, FirewallRule.TrafficType.Ingress);
        ReflectionTestUtils.setField(rule, "id", ++lastRuleId);
        firewallRules.add(rule);
    }

    private long addPortForwardingRule(long publicIpId, int publicPort, long vmId, int privatePort) {
        PortForwardingRuleVO rule = new PortForwardingRuleVO(null, publicIpId, publicPort, publicPort, new Ip("10.1.1.10"), privatePort, privatePort,
                NetUtils.TCP_PROTO, NETWORK_ID, 2L, 3L, vmId);
        ReflectionTestUtils.setField(rule, "id", ++lastRuleId);
        portForwardingRules.add(rule);
        return rule.getId();
    }

    private List<String> getFirewallRules() {
        return firewallRules.stream().map(rule -> rule.getSourcePortStart() + "-" + rule.getSourcePortEnd()).sorted().collect(Collectors.toList());
    }

    private List<String> getPortForwardingRules() {
        return portForwardingRules.stream().map(rule -> String.format("%d:%d->%d:%d", rule.getSourceIpAddressId(), rule.getSourcePortStart(),
                rule.getVirtualMachineId(), rule.getDestinationPortStart())).sorted().collect(Collectors.toList());
    }

    /**
     * Returns a worker for the cluster in vmMaps on an isolated network, whose firewall and port forwarding rules
     * are kept in firewallRules and portForwardingRules. A new firewall rule is refused if its ports overlap an
     * existing one, as FirewallManagerImpl.detectRulesConflict does for two TCP rules with the source CIDR 0.0.0.0/0.
     */
    private KubernetesClusterScaleWorker createWorkerForNetworkRules() throws Exception {
        PortForwardingRulesDao portForwardingRulesDao = Mockito.mock(PortForwardingRulesDao.class);
        FirewallRulesDao firewallRulesDao = Mockito.mock(FirewallRulesDao.class);
        FirewallService firewallService = Mockito.mock(FirewallService.class);
        RulesService rulesService = Mockito.mock(RulesService.class);
        Network network = Mockito.mock(Network.class);
        publicIp = Mockito.mock(IpAddress.class);

        Mockito.lenient().when(kubernetesCluster.getId()).thenReturn(CLUSTER_ID);
        Mockito.lenient().when(kubernetesCluster.getEtcdNodeCount()).thenAnswer(invocation -> vmMaps.stream().filter(KubernetesClusterVmMapVO::isEtcdNode).count());
        Mockito.lenient().when(kubernetesClusterVmMapDao.listByClusterId(CLUSTER_ID)).thenAnswer(invocation -> new ArrayList<>(vmMaps));
        Mockito.lenient().when(network.getId()).thenReturn(NETWORK_ID);
        Mockito.lenient().when(publicIp.getId()).thenReturn(PUBLIC_IP_ID);
        Mockito.lenient().when(publicIp.getAddress()).thenReturn(new Ip("192.0.2.10"));
        Mockito.lenient().when(firewallRulesDao.listByIpPurposeProtocolAndNotRevoked(PUBLIC_IP_ID, FirewallRule.Purpose.Firewall, NetUtils.TCP_PROTO))
                .thenAnswer(invocation -> new ArrayList<>(firewallRules));
        Mockito.lenient().when(firewallRulesDao.listByIpPurposePortsProtocolAndNotRevoked(Mockito.eq(PUBLIC_IP_ID), Mockito.any(), Mockito.any(),
                Mockito.eq(NetUtils.TCP_PROTO), Mockito.eq(FirewallRule.Purpose.Firewall))).thenAnswer(invocation -> firewallRules.stream()
                .filter(rule -> rule.getSourcePortStart().equals(invocation.getArgument(1)) && rule.getSourcePortEnd().equals(invocation.getArgument(2)))
                .collect(Collectors.toList()));
        Mockito.lenient().when(firewallService.revokeIngressFwRule(Mockito.anyLong(), Mockito.eq(true)))
                .thenAnswer(invocation -> firewallRules.removeIf(rule -> rule.getId() == (long) invocation.getArgument(0)));
        Mockito.lenient().when(portForwardingRulesDao.listByNetwork(NETWORK_ID)).thenAnswer(invocation -> new ArrayList<>(portForwardingRules));
        Mockito.lenient().when(portForwardingRulesDao.listByVm(Mockito.anyLong())).thenAnswer(invocation -> portForwardingRules.stream()
                .filter(rule -> rule.getVirtualMachineId() == (long) invocation.getArgument(0)).collect(Collectors.toList()));
        Mockito.lenient().when(portForwardingRulesDao.findByNetworkAndPorts(Mockito.eq(NETWORK_ID), Mockito.anyInt(), Mockito.anyInt()))
                .thenAnswer(invocation -> portForwardingRules.stream().filter(rule -> rule.getSourcePortStart() == (int) invocation.getArgument(1)
                        && rule.getSourcePortEnd() == (int) invocation.getArgument(2)).findFirst().orElse(null));
        Mockito.lenient().when(portForwardingRulesDao.remove(Mockito.anyLong()))
                .thenAnswer(invocation -> portForwardingRules.removeIf(rule -> rule.getId() == (long) invocation.getArgument(0)));
        Mockito.lenient().when(rulesService.revokePortForwardingRule(Mockito.anyLong(), Mockito.eq(true)))
                .thenAnswer(invocation -> portForwardingRules.removeIf(rule -> rule.getId() == (long) invocation.getArgument(0)));

        KubernetesClusterScaleWorker scaleWorker = Mockito.spy(new KubernetesClusterScaleWorker(kubernetesCluster, clusterManager));
        scaleWorker.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        scaleWorker.portForwardingRulesDao = portForwardingRulesDao;
        scaleWorker.firewallRulesDao = firewallRulesDao;
        scaleWorker.firewallService = firewallService;
        scaleWorker.rulesService = rulesService;
        scaleWorker.firewallManager = Mockito.mock(FirewallManager.class);
        ReflectionTestUtils.setField(scaleWorker, "network", network);
        Mockito.lenient().doReturn(publicIp).when(scaleWorker).getNetworkSourceNatIp(network);
        Mockito.lenient().doAnswer(invocation -> {
            int startPort = invocation.getArgument(2);
            int endPort = invocation.getArgument(3);
            for (FirewallRuleVO rule : firewallRules) {
                if (startPort <= rule.getSourcePortEnd() && rule.getSourcePortStart() <= endPort) {
                    throw new NetworkRuleConflictException(String.format("The range specified, %d-%d, conflicts with rule %d-%d",
                            startPort, endPort, rule.getSourcePortStart(), rule.getSourcePortEnd()));
                }
            }
            addFirewallRule(startPort, endPort);
            return null;
        }).when(scaleWorker).provisionFirewallRules(Mockito.eq(publicIp), Mockito.any(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.lenient().doAnswer(invocation -> {
            addPortForwardingRule(PUBLIC_IP_ID, invocation.getArgument(4), invocation.getArgument(3), invocation.getArgument(5));
            return null;
        }).when(scaleWorker).provisionPublicIpPortForwardingRule(Mockito.eq(publicIp), Mockito.eq(network), Mockito.any(),
                Mockito.anyLong(), Mockito.anyInt(), Mockito.anyInt());
        return scaleWorker;
    }

    // With the node VMs as passed by scaleUpKubernetesClusterSize and removeNodesFromCluster
    private void scaleIsolatedNetworkRules(KubernetesClusterScaleWorker scaleWorker) throws ManagementServerException {
        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            scaleWorker.scaleKubernetesClusterIsolatedNetworkRules(scaleWorker.getNodeVmIdsForSshRules());
        }
    }

    @Test
    public void testGetNodeVmIdsForSshRulesPutsExternalNodesLast() {
        // A worker added by a scale-up after an external node, and an etcd node
        addVm(CONTROL_VM_ID, true, false, false);
        addVm(WORKER_VM_ID, false, false, false);
        addVm(EXTERNAL_VM_ID, false, true, false);
        addVm(NEW_WORKER_VM_ID, false, false, false);
        addVm(ETCD_VM_ID, false, false, true);
        Mockito.when(kubernetesCluster.getId()).thenReturn(CLUSTER_ID);
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(CLUSTER_ID)).thenReturn(vmMaps);

        Assert.assertEquals(List.of(CONTROL_VM_ID, WORKER_VM_ID, NEW_WORKER_VM_ID, EXTERNAL_VM_ID), worker.getNodeVmIdsForSshRules());
    }

    @Test
    public void testScaleKubernetesClusterIsolatedNetworkRulesScaleUpWithExternalNodes() throws Exception {
        // 1 control node and 1 worker (2222-2223), two external nodes (2224, 2225), an etcd node and the worker added by the scale-up
        addVm(CONTROL_VM_ID, true, false, false);
        addVm(WORKER_VM_ID, false, false, false);
        addVm(EXTERNAL_VM_ID, false, true, false);
        addVm(SECOND_EXTERNAL_VM_ID, false, true, false);
        addVm(ETCD_VM_ID, false, false, true);
        addVm(NEW_WORKER_VM_ID, false, false, false);
        KubernetesClusterScaleWorker scaleWorker = createWorkerForNetworkRules();
        // The rules of the external nodes are listed first, removeSshFirewallRule must still find the range
        addFirewallRule(2224, 2224);
        addFirewallRule(2225, 2225);
        addFirewallRule(2222, 2223);
        addFirewallRule(API_PORT, API_PORT);
        addFirewallRule(ETCD_SSH_PORT, ETCD_SSH_PORT);
        addPortForwardingRule(PUBLIC_IP_ID, 2222, CONTROL_VM_ID, SSH_PORT);
        addPortForwardingRule(PUBLIC_IP_ID, 2223, WORKER_VM_ID, SSH_PORT);
        addPortForwardingRule(PUBLIC_IP_ID, 2224, EXTERNAL_VM_ID, SSH_PORT);
        addPortForwardingRule(PUBLIC_IP_ID, 2225, SECOND_EXTERNAL_VM_ID, SSH_PORT);
        addPortForwardingRule(PUBLIC_IP_ID, ETCD_SSH_PORT, ETCD_VM_ID, SSH_PORT);

        scaleIsolatedNetworkRules(scaleWorker);

        Assert.assertEquals(List.of("2222-2224", "2225-2225", "2226-2226", ETCD_SSH_PORT + "-" + ETCD_SSH_PORT, "6443-6443"), getFirewallRules());
        Assert.assertEquals(List.of("20:2222->100:22", "20:2223->101:22", "20:2224->103:22", "20:2225->102:22", "20:2226->104:22",
                "20:" + ETCD_SSH_PORT + "->105:22"), getPortForwardingRules());
    }

    @Test
    public void testScaleKubernetesClusterIsolatedNetworkRulesScaleUpThenDownWithExternalNode() throws Exception {
        // 1 control node and 1 worker (2222-2223) and an external node (2224)
        addVm(CONTROL_VM_ID, true, false, false);
        addVm(WORKER_VM_ID, false, false, false);
        addVm(EXTERNAL_VM_ID, false, true, false);
        KubernetesClusterScaleWorker scaleWorker = createWorkerForNetworkRules();
        addFirewallRule(2222, 2223);
        addFirewallRule(API_PORT, API_PORT);
        addFirewallRule(2224, 2224);
        addPortForwardingRule(PUBLIC_IP_ID, 2222, CONTROL_VM_ID, SSH_PORT);
        addPortForwardingRule(PUBLIC_IP_ID, 2223, WORKER_VM_ID, SSH_PORT);
        addPortForwardingRule(PUBLIC_IP_ID, 2224, EXTERNAL_VM_ID, SSH_PORT);

        // Scale-up by one worker, which comes after the external node in the VM map
        addVm(NEW_WORKER_VM_ID, false, false, false);
        scaleIsolatedNetworkRules(scaleWorker);
        Assert.assertEquals(List.of("2222-2224", "2225-2225", "6443-6443"), getFirewallRules());
        Assert.assertEquals(List.of("20:2222->100:22", "20:2223->101:22", "20:2224->103:22", "20:2225->102:22"), getPortForwardingRules());

        // Scale-down removing the first worker by its ID
        removeVm(WORKER_VM_ID);
        scaleIsolatedNetworkRules(scaleWorker);
        Assert.assertEquals(List.of("2222-2223", "2224-2224", "6443-6443"), getFirewallRules());
        Assert.assertEquals(List.of("20:2222->100:22", "20:2223->103:22", "20:2224->102:22"), getPortForwardingRules());

        // Scale-up again
        addVm(THIRD_WORKER_VM_ID, false, false, false);
        scaleIsolatedNetworkRules(scaleWorker);
        Assert.assertEquals(List.of("2222-2224", "2225-2225", "6443-6443"), getFirewallRules());
        Assert.assertEquals(List.of("20:2222->100:22", "20:2223->103:22", "20:2224->106:22", "20:2225->102:22"), getPortForwardingRules());
    }

    @Test
    public void testScaleKubernetesClusterIsolatedNetworkRulesScaleUpWithoutExternalNode() throws Exception {
        addVm(CONTROL_VM_ID, true, false, false);
        addVm(WORKER_VM_ID, false, false, false);
        addVm(NEW_WORKER_VM_ID, false, false, false);
        KubernetesClusterScaleWorker scaleWorker = createWorkerForNetworkRules();
        addFirewallRule(2222, 2223);
        addFirewallRule(API_PORT, API_PORT);
        addPortForwardingRule(PUBLIC_IP_ID, 2222, CONTROL_VM_ID, SSH_PORT);
        addPortForwardingRule(PUBLIC_IP_ID, 2223, WORKER_VM_ID, SSH_PORT);

        scaleIsolatedNetworkRules(scaleWorker);

        Assert.assertEquals(List.of("2222-2224", "6443-6443"), getFirewallRules());
        Assert.assertEquals(List.of("20:2222->100:22", "20:2223->101:22", "20:2224->103:22"), getPortForwardingRules());
        Mockito.verify(scaleWorker.portForwardingRulesDao, Mockito.never()).listByVm(Mockito.anyLong());
        Mockito.verify(scaleWorker.rulesService, Mockito.never()).revokePortForwardingRule(Mockito.anyLong(), Mockito.anyBoolean());
    }

    @Test
    public void testRemoveExternalNodeSshRulesKeepsOtherRules() throws Exception {
        addVm(CONTROL_VM_ID, true, false, false);
        addVm(WORKER_VM_ID, false, false, false);
        addVm(EXTERNAL_VM_ID, false, true, false);
        KubernetesClusterScaleWorker scaleWorker = createWorkerForNetworkRules();
        addFirewallRule(2222, 2223);
        addFirewallRule(API_PORT, API_PORT);
        addFirewallRule(2224, 2224);
        addPortForwardingRule(PUBLIC_IP_ID, 2222, CONTROL_VM_ID, SSH_PORT);
        addPortForwardingRule(PUBLIC_IP_ID, 2223, WORKER_VM_ID, SSH_PORT);
        addPortForwardingRule(PUBLIC_IP_ID, 2224, EXTERNAL_VM_ID, SSH_PORT);
        // An old SSH rule of the external node, left by an earlier scale-down
        addFirewallRule(2226, 2226);
        addPortForwardingRule(PUBLIC_IP_ID, 2226, EXTERNAL_VM_ID, SSH_PORT);
        // Other rules of the external node VM: to another port, from outside the node SSH port range, and on another IP
        addFirewallRule(8080, 8080);
        addPortForwardingRule(PUBLIC_IP_ID, 8080, EXTERNAL_VM_ID, 80);
        addFirewallRule(2022, 2022);
        addPortForwardingRule(PUBLIC_IP_ID, 2022, EXTERNAL_VM_ID, SSH_PORT);
        addFirewallRule(ETCD_SSH_PORT + 22, ETCD_SSH_PORT + 22);
        addPortForwardingRule(PUBLIC_IP_ID, ETCD_SSH_PORT + 22, EXTERNAL_VM_ID, SSH_PORT);
        addPortForwardingRule(OTHER_PUBLIC_IP_ID, 2224, EXTERNAL_VM_ID, SSH_PORT);

        scaleWorker.removeExternalNodeSshRules(publicIp);

        String etcdRangePort = String.valueOf(ETCD_SSH_PORT + 22);
        Assert.assertEquals(List.of("2022-2022", "2222-2223", etcdRangePort + "-" + etcdRangePort, "6443-6443", "8080-8080"), getFirewallRules());
        Assert.assertEquals(List.of("20:2022->102:22", "20:2222->100:22", "20:2223->101:22", "20:" + etcdRangePort + "->102:22", "20:8080->102:80",
                "21:2224->102:22"), getPortForwardingRules());
    }

    @Test
    public void testRemoveExternalNodeSshRulesCanBeRetriedAfterAFailure() throws Exception {
        addVm(CONTROL_VM_ID, true, false, false);
        addVm(EXTERNAL_VM_ID, false, true, false);
        addVm(SECOND_EXTERNAL_VM_ID, false, true, false);
        KubernetesClusterScaleWorker scaleWorker = createWorkerForNetworkRules();
        addFirewallRule(2222, 2222);
        addFirewallRule(2223, 2223);
        addFirewallRule(2224, 2224);
        addPortForwardingRule(PUBLIC_IP_ID, 2222, CONTROL_VM_ID, SSH_PORT);
        addPortForwardingRule(PUBLIC_IP_ID, 2223, EXTERNAL_VM_ID, SSH_PORT);
        long failingRuleId = addPortForwardingRule(PUBLIC_IP_ID, 2224, SECOND_EXTERNAL_VM_ID, SSH_PORT);
        // Revoking the port forwarding rule of the second external node fails once
        Mockito.doThrow(new CloudRuntimeException("Failed to delete port forwarding rule"))
                .doAnswer(invocation -> portForwardingRules.removeIf(rule -> rule.getId() == failingRuleId))
                .when(scaleWorker.rulesService).revokePortForwardingRule(failingRuleId, true);

        Assert.assertThrows(ManagementServerException.class, () -> scaleWorker.removeExternalNodeSshRules(publicIp));
        Assert.assertEquals(List.of("2222-2222"), getFirewallRules());
        Assert.assertEquals(List.of("20:2222->100:22", "20:2224->104:22"), getPortForwardingRules());

        scaleWorker.removeExternalNodeSshRules(publicIp);
        Assert.assertEquals(List.of("2222-2222"), getFirewallRules());
        Assert.assertEquals(List.of("20:2222->100:22"), getPortForwardingRules());
    }

    @Test
    public void testRemoveExternalNodeSshRulesReportsAFirewallRuleThatIsNotRevoked() throws Exception {
        addVm(CONTROL_VM_ID, true, false, false);
        addVm(EXTERNAL_VM_ID, false, true, false);
        KubernetesClusterScaleWorker scaleWorker = createWorkerForNetworkRules();
        addFirewallRule(2222, 2222);
        addFirewallRule(2223, 2223);
        addPortForwardingRule(PUBLIC_IP_ID, 2222, CONTROL_VM_ID, SSH_PORT);
        addPortForwardingRule(PUBLIC_IP_ID, 2223, EXTERNAL_VM_ID, SSH_PORT);
        Mockito.doReturn(false).when(scaleWorker.firewallService).revokeIngressFwRule(Mockito.anyLong(), Mockito.eq(true));

        Assert.assertThrows(ManagementServerException.class, () -> scaleWorker.removeExternalNodeSshRules(publicIp));
        Assert.assertEquals(List.of("20:2222->100:22", "20:2223->102:22"), getPortForwardingRules());
        Mockito.verify(scaleWorker.rulesService, Mockito.never()).revokePortForwardingRule(Mockito.anyLong(), Mockito.anyBoolean());
    }
}
