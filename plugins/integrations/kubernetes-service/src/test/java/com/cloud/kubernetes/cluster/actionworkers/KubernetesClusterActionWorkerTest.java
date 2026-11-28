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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.api.ApiConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterDetailsVO;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterActionWorkerTest {

    @Mock
    KubernetesClusterDao kubernetesClusterDao;

    @Mock
    KubernetesClusterVmMapDao kubernetesClusterVmMapDao;

    @Mock
    KubernetesClusterDetailsDao kubernetesClusterDetailsDao;

    @Mock
    KubernetesSupportedVersionDao kubernetesSupportedVersionDao;

    @Mock
    KubernetesClusterManagerImpl kubernetesClusterManager;

    @Mock
    IPAddressDao ipAddressDao;

    @Mock
    PortForwardingRulesDao portForwardingRulesDao;

    KubernetesClusterActionWorker actionWorker = null;

    final static Long DEFAULT_ID = 1L;

    @Before
    public void setUp() throws Exception {
        kubernetesClusterManager.kubernetesClusterDao = kubernetesClusterDao;
        kubernetesClusterManager.kubernetesSupportedVersionDao = kubernetesSupportedVersionDao;
        kubernetesClusterManager.kubernetesClusterDetailsDao = kubernetesClusterDetailsDao;
        kubernetesClusterManager.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        KubernetesCluster kubernetesCluster = Mockito.mock(KubernetesCluster.class);
        Mockito.when(kubernetesCluster.getId()).thenReturn(DEFAULT_ID);
        actionWorker = new KubernetesClusterActionWorker(kubernetesCluster, kubernetesClusterManager);
        actionWorker.ipAddressDao = ipAddressDao;
        actionWorker.portForwardingRulesDao = portForwardingRulesDao;
    }

    @Test
    public void testGetVpcTierKubernetesPublicIpNullDetail() {
        IpAddress result = actionWorker.getVpcTierKubernetesPublicIp(Mockito.mock(Network.class));
        Assert.assertNull(result);
    }

    private String mockClusterPublicIpDetail(boolean isNull) {
        String uuid = isNull ? null : UUID.randomUUID().toString();
        KubernetesClusterDetailsVO detailsVO = new KubernetesClusterDetailsVO(DEFAULT_ID, ApiConstants.PUBLIC_IP_ID, uuid, false);
        Mockito.when(kubernetesClusterDetailsDao.findDetail(DEFAULT_ID, ApiConstants.PUBLIC_IP_ID)).thenReturn(detailsVO);
        return uuid;
    }

    @Test
    public void testGetVpcTierKubernetesPublicIpNullDetailValue() {
        mockClusterPublicIpDetail(true);
        IpAddress result = actionWorker.getVpcTierKubernetesPublicIp(Mockito.mock(Network.class));
        Assert.assertNull(result);
    }

    private Network mockNetworkForGetVpcTierKubernetesPublicIpTest() {
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getVpcId()).thenReturn(DEFAULT_ID);
        return network;
    }

    @Test
    public void testGetVpcTierKubernetesPublicIpNullVpc() {
        String uuid = mockClusterPublicIpDetail(false);
        IPAddressVO address = Mockito.mock(IPAddressVO.class);
        Mockito.when(ipAddressDao.findByUuid(uuid)).thenReturn(address);
        IpAddress result = actionWorker.getVpcTierKubernetesPublicIp(mockNetworkForGetVpcTierKubernetesPublicIpTest());
        Assert.assertNull(result);
    }

    @Test
    public void testGetVpcTierKubernetesPublicIpDifferentVpc() {
        String uuid = mockClusterPublicIpDetail(false);
        IPAddressVO address = Mockito.mock(IPAddressVO.class);
        Mockito.when(address.getVpcId()).thenReturn(2L);
        Mockito.when(ipAddressDao.findByUuid(uuid)).thenReturn(address);
        IpAddress result = actionWorker.getVpcTierKubernetesPublicIp(mockNetworkForGetVpcTierKubernetesPublicIpTest());
        Assert.assertNull(result);
    }

    @Test
    public void testGetVpcTierKubernetesPublicIpValid() {
        String uuid = mockClusterPublicIpDetail(false);
        IPAddressVO address = Mockito.mock(IPAddressVO.class);
        Mockito.when(address.getVpcId()).thenReturn(DEFAULT_ID);
        Mockito.when(ipAddressDao.findByUuid(uuid)).thenReturn(address);
        IpAddress result = actionWorker.getVpcTierKubernetesPublicIp(mockNetworkForGetVpcTierKubernetesPublicIpTest());
        Assert.assertNotNull(result);
    }

    private static final String SSH_IP = "203.0.113.10";
    private static final long SSH_IP_ID = 5L;

    private UserVm mockNode(long id) {
        UserVm vm = Mockito.mock(UserVm.class);
        Mockito.when(vm.getId()).thenReturn(id);
        return vm;
    }

    private PortForwardingRuleVO sshRule(long vmId, int publicPort, int privatePort, String protocol) {
        PortForwardingRuleVO rule = new PortForwardingRuleVO(null, SSH_IP_ID, publicPort, new Ip("10.1.1.10"), privatePort, protocol, 1L, 1L, 1L, vmId);
        rule.setState(FirewallRule.State.Active);
        return rule;
    }

    private void mockSshIpRules(List<PortForwardingRuleVO> rules) {
        Mockito.when(actionWorker.kubernetesCluster.getZoneId()).thenReturn(DEFAULT_ID);
        IPAddressVO address = Mockito.mock(IPAddressVO.class);
        Mockito.when(address.getId()).thenReturn(SSH_IP_ID);
        Mockito.when(ipAddressDao.findByIpAndDcId(DEFAULT_ID, SSH_IP)).thenReturn(address);
        Mockito.when(portForwardingRulesDao.listByIpAndNotRevoked(SSH_IP_ID)).thenReturn(rules);
    }

    @Test
    public void testGetClusterNodesSshIpPortsUsesNodeActiveCksRuleNotPosition() {
        actionWorker.publicIpAddress = SSH_IP;
        actionWorker.sshPort = KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT;
        PortForwardingRuleVO staleRule = sshRule(12L, 2225, 22, "tcp");
        staleRule.setState(FirewallRule.State.Add);
        PortForwardingRuleVO activeRule = sshRule(12L, 2226, 22, "tcp");
        activeRule.setState(FirewallRule.State.Active);
        PortForwardingRuleVO userRule = sshRule(12L, 2022, 22, "tcp");
        userRule.setState(FirewallRule.State.Active);
        mockSshIpRules(Arrays.asList(
                sshRule(10L, 2222, 22, "tcp"),
                sshRule(11L, 2223, 22, "tcp"),
                sshRule(12L, 2224, 22, "udp"),
                sshRule(12L, 8080, 80, "tcp"),
                staleRule,
                userRule,
                activeRule));
        Map<Long, Pair<String, Integer>> result = actionWorker.getClusterNodesSshIpPorts(Arrays.asList(mockNode(10L), mockNode(12L)));
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(SSH_IP, result.get(12L).first());
        Assert.assertEquals(2226, result.get(12L).second().intValue());
        Assert.assertEquals(2222, result.get(10L).second().intValue());
    }

    @Test
    public void testGetClusterNodesSshIpPortsEtcdNodeBelowDefaultStartPort() {
        actionWorker.publicIpAddress = SSH_IP;
        actionWorker.sshPort = KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT;
        actionWorker.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        List<KubernetesClusterVmMapVO> maps = Arrays.asList(vmMap(1L, true, true), vmMap(10L, false, false), vmMap(11L, false, false));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(DEFAULT_ID)).thenReturn(maps);
        mockSshIpRules(Arrays.asList(sshRule(1L, 2000, 22, "tcp"), sshRule(10L, 2222, 22, "tcp"), sshRule(11L, 2001, 22, "tcp")));
        Map<Long, Pair<String, Integer>> result = actionWorker.getClusterNodesSshIpPorts(Arrays.asList(mockNode(1L), mockNode(10L)));
        Assert.assertEquals(2000, result.get(1L).second().intValue());
        Assert.assertEquals(2222, result.get(10L).second().intValue());
        UserVm worker = mockNode(11L);
        Assert.assertThrows(CloudRuntimeException.class, () -> actionWorker.getClusterNodesSshIpPorts(List.of(worker)));
    }

    @Test
    public void testGetClusterNodesSshIpPortsSkipsDeletingAndPrefersAddOverStaged() {
        actionWorker.publicIpAddress = SSH_IP;
        actionWorker.sshPort = KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT;
        PortForwardingRuleVO stagedRule = sshRule(12L, 2223, 22, "tcp");
        stagedRule.setState(FirewallRule.State.Staged);
        PortForwardingRuleVO deletingRule = sshRule(12L, 2224, 22, "tcp");
        deletingRule.setState(FirewallRule.State.Deleting);
        PortForwardingRuleVO newRule = sshRule(12L, 2227, 22, "tcp");
        newRule.setState(FirewallRule.State.Add);
        mockSshIpRules(Arrays.asList(stagedRule, deletingRule, newRule));
        Map<Long, Pair<String, Integer>> result = actionWorker.getClusterNodesSshIpPorts(List.of(mockNode(12L)));
        Assert.assertEquals(2227, result.get(12L).second().intValue());
    }

    @Test
    public void testGetClusterNodesSshIpPortsNoRuleFailsNamingNode() {
        actionWorker.publicIpAddress = SSH_IP;
        actionWorker.sshPort = KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT;
        mockSshIpRules(Arrays.asList(sshRule(10L, 2222, 22, "tcp"), sshRule(11L, 2223, 22, "tcp")));
        UserVm vm = mockNode(12L);
        Mockito.when(vm.getHostName()).thenReturn("cks1-node-ext");
        try {
            actionWorker.getClusterNodesSshIpPorts(Arrays.asList(mockNode(10L), vm));
            Assert.fail("Expected a CloudRuntimeException");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("cks1-node-ext"));
            Assert.assertTrue(e.getMessage().contains(SSH_IP));
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetClusterNodesSshIpPortsUnknownIpFails() {
        actionWorker.publicIpAddress = SSH_IP;
        actionWorker.sshPort = KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT;
        Mockito.when(actionWorker.kubernetesCluster.getZoneId()).thenReturn(DEFAULT_ID);
        actionWorker.getClusterNodesSshIpPorts(List.of(Mockito.mock(UserVm.class)));
    }

    @Test
    public void testGetClusterNodesSshIpPortsDirectAccessUsesNodeIp() {
        actionWorker.publicIpAddress = "10.1.1.5";
        actionWorker.sshPort = KubernetesClusterActionWorker.DEFAULT_SSH_PORT;
        UserVm vm = mockNode(12L);
        Mockito.when(vm.getPrivateIpAddress()).thenReturn("10.1.1.20");
        Map<Long, Pair<String, Integer>> result = actionWorker.getClusterNodesSshIpPorts(List.of(vm));
        Assert.assertEquals("10.1.1.20", result.get(12L).first());
        Assert.assertEquals(KubernetesClusterActionWorker.DEFAULT_SSH_PORT, result.get(12L).second().intValue());
        Mockito.verifyNoInteractions(portForwardingRulesDao);
    }

    private KubernetesClusterVmMapVO vmMap(long vmId, boolean etcd) {
        return vmMap(vmId, etcd, !etcd);
    }

    private KubernetesClusterVmMapVO vmMap(long vmId, boolean etcd, boolean idUsed) {
        KubernetesClusterVmMapVO map = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(map.isEtcdNode()).thenReturn(etcd);
        if (idUsed) {
            Mockito.when(map.getVmId()).thenReturn(vmId);
        }
        return map;
    }

    @Test
    public void testGetClusterNodesSshIpPortsExternalLoadBalancerUsesClusterPosition() {
        actionWorker.publicIpAddress = "198.51.100.7";
        actionWorker.sshPort = KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT;
        actionWorker.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        Mockito.when(kubernetesClusterDetailsDao.findDetail(DEFAULT_ID, ApiConstants.EXTERNAL_LOAD_BALANCER_IP_ADDRESS))
                .thenReturn(new KubernetesClusterDetailsVO(DEFAULT_ID, ApiConstants.EXTERNAL_LOAD_BALANCER_IP_ADDRESS, "198.51.100.7", true));
        List<KubernetesClusterVmMapVO> maps = Arrays.asList(vmMap(1L, true), vmMap(10L, false), vmMap(11L, false), vmMap(12L, false));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(DEFAULT_ID)).thenReturn(maps);
        Map<Long, Pair<String, Integer>> result = actionWorker.getClusterNodesSshIpPorts(Arrays.asList(mockNode(10L), mockNode(12L)));
        Assert.assertEquals("198.51.100.7", result.get(12L).first());
        Assert.assertEquals(KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT, result.get(10L).second().intValue());
        Assert.assertEquals(KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT + 2, result.get(12L).second().intValue());
        UserVm etcd = mockNode(1L);
        Mockito.when(etcd.getHostName()).thenReturn("cks1-etcd-1");
        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, () -> actionWorker.getClusterNodesSshIpPorts(List.of(etcd)));
        Assert.assertTrue(e.getMessage().contains("cks1-etcd-1"));
        Mockito.verifyNoInteractions(portForwardingRulesDao);
    }
}
