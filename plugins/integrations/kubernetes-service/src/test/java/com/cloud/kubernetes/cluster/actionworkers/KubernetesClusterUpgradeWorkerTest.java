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

import com.cloud.hypervisor.Hypervisor;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterUtil;
import com.cloud.kubernetes.version.KubernetesSupportedVersion;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.utils.ssh.SshHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterUpgradeWorkerTest {

    @Mock
    private KubernetesCluster kubernetesCluster;
    @Mock
    private KubernetesSupportedVersion kubernetesSupportedVersion;
    @Mock
    private KubernetesClusterManagerImpl clusterManager;
    @Mock
    private KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    @Mock
    private KubernetesClusterDetailsDao kubernetesClusterDetailsDao;
    @Mock
    private IPAddressDao ipAddressDao;
    @Mock
    private PortForwardingRulesDao portForwardingRulesDao;

    private KubernetesClusterUpgradeWorker worker;

    @Before
    public void setUp() {
        String[] keys = {};
        worker = new KubernetesClusterUpgradeWorker(kubernetesCluster, kubernetesSupportedVersion, clusterManager, keys);
        worker.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
    }

    @Test
    public void testFilterOutManualUpgradeNodesFromClusterUpgrade() {
        long controlNodeId = 1L;
        long workerNode1Id = 2L;
        long workerNode2Id = 3L;
        UserVm controlNode = Mockito.mock(UserVm.class);
        Mockito.when(controlNode.getId()).thenReturn(controlNodeId);
        UserVm workerNode1 = Mockito.mock(UserVm.class);
        Mockito.when(workerNode1.getId()).thenReturn(workerNode1Id);
        UserVm workerNode2 = Mockito.mock(UserVm.class);
        Mockito.when(workerNode2.getId()).thenReturn(workerNode2Id);
        KubernetesClusterVmMapVO controlNodeMap = Mockito.mock(KubernetesClusterVmMapVO.class);
        KubernetesClusterVmMapVO workerNode1Map = Mockito.mock(KubernetesClusterVmMapVO.class);
        KubernetesClusterVmMapVO workerNode2Map = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(workerNode2Map.isManualUpgrade()).thenReturn(true);
        Mockito.when(kubernetesClusterVmMapDao.getClusterMapFromVmId(controlNodeId)).thenReturn(controlNodeMap);
        Mockito.when(kubernetesClusterVmMapDao.getClusterMapFromVmId(workerNode1Id)).thenReturn(workerNode1Map);
        Mockito.when(kubernetesClusterVmMapDao.getClusterMapFromVmId(workerNode2Id)).thenReturn(workerNode2Map);
        worker.clusterVMs = Arrays.asList(controlNode, workerNode1, workerNode2);
        worker.filterOutManualUpgradeNodesFromClusterUpgrade();
        Assert.assertEquals(2, worker.clusterVMs.size());
        List<Long> ids = worker.clusterVMs.stream().map(UserVm::getId).collect(Collectors.toList());
        Assert.assertTrue(ids.contains(controlNodeId) && ids.contains(workerNode1Id));
        Assert.assertFalse(ids.contains(workerNode2Id));
    }

    private static final String SSH_IP = "203.0.113.10";
    private static final long SSH_IP_ID = 5L;

    private UserVm mockNode(long id, String hostName) {
        UserVm vm = Mockito.mock(UserVm.class);
        Mockito.when(vm.getId()).thenReturn(id);
        Mockito.when(vm.getHostName()).thenReturn(hostName);
        return vm;
    }

    private PortForwardingRuleVO sshRule(long vmId, int publicPort) {
        PortForwardingRuleVO rule = new PortForwardingRuleVO(null, SSH_IP_ID, publicPort, new Ip("10.1.1.10"), 22, "tcp", 1L, 1L, 1L, vmId);
        rule.setState(FirewallRule.State.Active);
        return rule;
    }

    private KubernetesClusterUpgradeWorker prepareUpgradeWorker(List<UserVm> vms, List<PortForwardingRuleVO> rules) throws Exception {
        worker.kubernetesClusterDetailsDao = kubernetesClusterDetailsDao;
        worker.ipAddressDao = ipAddressDao;
        worker.portForwardingRulesDao = portForwardingRulesDao;
        worker.publicIpAddress = SSH_IP;
        worker.sshPort = KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT;
        worker.clusterVMs = vms;
        Mockito.when(kubernetesCluster.getZoneId()).thenReturn(1L);
        IPAddressVO address = Mockito.mock(IPAddressVO.class);
        Mockito.when(address.getId()).thenReturn(SSH_IP_ID);
        Mockito.when(ipAddressDao.findByIpAndDcId(1L, SSH_IP)).thenReturn(address);
        Mockito.when(portForwardingRulesDao.listByIpAndNotRevoked(SSH_IP_ID)).thenReturn(rules);
        java.lang.reflect.Field timeout = KubernetesClusterUpgradeWorker.class.getDeclaredField("upgradeTimeoutTime");
        timeout.setAccessible(true);
        timeout.setLong(worker, System.currentTimeMillis() + 3600 * 1000L);
        java.lang.reflect.Field script = KubernetesClusterUpgradeWorker.class.getDeclaredField("upgradeScriptFile");
        script.setAccessible(true);
        script.set(worker, new File("upgrade-k8s.sh"));
        KubernetesClusterUpgradeWorker spy = Mockito.spy(worker);
        Mockito.doReturn("cloud").when(spy).getControlNodeLoginUser();
        return spy;
    }

    @Test
    public void testUpgradeReachesEachNodeOnItsPortForwardingRule() throws Exception {
        UserVm control = mockNode(10L, "cks-control");
        UserVm worker1 = mockNode(11L, "cks-node-1");
        UserVm external = mockNode(13L, "cks-external");
        Mockito.when(control.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(worker1.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(external.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(kubernetesSupportedVersion.getSemanticVersion()).thenReturn("1.30.2");
        KubernetesClusterUpgradeWorker spy = prepareUpgradeWorker(Arrays.asList(control, worker1, external),
                Arrays.asList(sshRule(10L, 2222), sshRule(11L, 2223), sshRule(13L, 2225)));
        Mockito.doReturn(true).when(spy).deployProvider();
        Mockito.doReturn(null).when(spy).getManagementServerSshPublicKeyFile();
        try (MockedStatic<SshHelper> ssh = Mockito.mockStatic(SshHelper.class);
             MockedStatic<KubernetesClusterUtil> util = Mockito.mockStatic(KubernetesClusterUtil.class)) {
            ssh.when(() -> SshHelper.sshExecute(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                    ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
                    .thenReturn(new Pair<>(true, ""));
            util.when(() -> KubernetesClusterUtil.uncordonKubernetesClusterNode(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
                    ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyLong(), ArgumentMatchers.anyInt())).thenReturn(true);
            util.when(() -> KubernetesClusterUtil.isKubernetesClusterNodeReady(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
                    ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyLong(), ArgumentMatchers.anyInt())).thenReturn(true);
            util.when(() -> KubernetesClusterUtil.clusterNodeVersionMatches(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
                    ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong(),
                    ArgumentMatchers.anyLong(), ArgumentMatchers.any())).thenReturn(true);

            spy.upgradeKubernetesClusterNodes(spy.resolveNodeSshIpPorts());

            for (int port : new int[]{2222, 2223, 2225}) {
                ssh.verify(() -> SshHelper.scpTo(ArgumentMatchers.eq(SSH_IP), ArgumentMatchers.eq(port), ArgumentMatchers.any(), ArgumentMatchers.any(),
                        ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()));
                ssh.verify(() -> SshHelper.sshExecute(ArgumentMatchers.eq(SSH_IP), ArgumentMatchers.eq(port), ArgumentMatchers.any(), ArgumentMatchers.any(),
                        ArgumentMatchers.any(), ArgumentMatchers.startsWith("sudo ./upgrade-k8s.sh"), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()));
            }
            ssh.verify(() -> SshHelper.scpTo(ArgumentMatchers.anyString(), ArgumentMatchers.eq(2224), ArgumentMatchers.any(), ArgumentMatchers.any(),
                    ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()), Mockito.never());
        }
    }

    @Test
    public void testUpgradeFailsBeforeAnyChangeWhenANodeHasNoSshRule() throws Exception {
        UserVm control = mockNode(10L, "cks-control");
        UserVm external = mockNode(13L, "cks-external");
        List<UserVm> vms = Arrays.asList(control, external);
        KubernetesClusterUpgradeWorker spy = prepareUpgradeWorker(vms, List.of(sshRule(10L, 2222)));
        Mockito.doNothing().when(spy).init();
        Mockito.doReturn(new Pair<>(SSH_IP, KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT)).when(spy).getKubernetesClusterServerIpSshPort(null);
        Mockito.doReturn(vms).when(spy).getKubernetesClusterVMs();
        KubernetesClusterVmMapVO nodeMap = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(kubernetesClusterVmMapDao.getClusterMapFromVmId(ArgumentMatchers.anyLong())).thenReturn(nodeMap);
        try (MockedStatic<SshHelper> ssh = Mockito.mockStatic(SshHelper.class)) {
            try {
                spy.upgradeCluster();
                Assert.fail("Expected a CloudRuntimeException");
            } catch (CloudRuntimeException e) {
                Assert.assertTrue(e.getMessage().contains("cks-external"));
            }
            ssh.verifyNoInteractions();
            Mockito.verify(spy, Mockito.never()).stateTransitTo(ArgumentMatchers.anyLong(), ArgumentMatchers.any());
            Mockito.verify(spy, Mockito.never()).attachIsoKubernetesVMs(ArgumentMatchers.any(), ArgumentMatchers.any());
        }
    }
}
