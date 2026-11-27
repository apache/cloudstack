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

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.KubernetesSupportedVersion;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
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
    private KubernetesClusterDao kubernetesClusterDao;
    @Mock
    private NetworkDao networkDao;

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

    private KubernetesClusterUpgradeWorker prepareUpgrade(boolean clusterUpdated) {
        long clusterId = 5L;
        Mockito.when(kubernetesCluster.getId()).thenReturn(clusterId);
        Mockito.lenient().when(kubernetesCluster.getNetworkId()).thenReturn(9L);
        Mockito.lenient().when(networkDao.findById(9L)).thenReturn(Mockito.mock(NetworkVO.class));
        KubernetesClusterVO clusterVO = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(kubernetesClusterDao.findById(clusterId)).thenReturn(clusterVO);
        Mockito.when(kubernetesClusterDao.update(clusterId, clusterVO)).thenReturn(clusterUpdated);
        UserVm controlNode = Mockito.mock(UserVm.class);
        worker.kubernetesClusterDao = kubernetesClusterDao;
        worker.networkDao = networkDao;
        KubernetesClusterUpgradeWorker spy = Mockito.spy(worker);
        Mockito.doNothing().when(spy).init();
        Mockito.doReturn(new Pair<>("10.1.1.10", 2222)).when(spy).getKubernetesClusterServerIpSshPort(null);
        Mockito.doReturn(Collections.singletonList(controlNode)).when(spy).getKubernetesClusterVMs();
        Mockito.doNothing().when(spy).filterOutManualUpgradeNodesFromClusterUpgrade();
        Mockito.doNothing().when(spy).retrieveScriptFiles();
        Mockito.doReturn(true).when(spy).stateTransitTo(ArgumentMatchers.anyLong(), ArgumentMatchers.any(KubernetesCluster.Event.class));
        Mockito.doNothing().when(spy).attachIsoKubernetesVMs(ArgumentMatchers.anyList(), ArgumentMatchers.any());
        Mockito.doNothing().when(spy).upgradeKubernetesClusterNodes();
        Mockito.doNothing().when(spy).detachIsoKubernetesVMs(ArgumentMatchers.anyList());
        return spy;
    }

    @Test
    public void testUpgradeRefreshesTheStoredKubeConfigBeforeCompleting() {
        KubernetesClusterUpgradeWorker spy = prepareUpgrade(true);
        Mockito.doReturn(true).when(spy).refreshKubeConfigData(ArgumentMatchers.anyLong());

        Assert.assertTrue(spy.upgradeCluster());

        InOrder inOrder = Mockito.inOrder(spy);
        inOrder.verify(spy).upgradeKubernetesClusterNodes();
        inOrder.verify(spy).refreshKubeConfigData(ArgumentMatchers.anyLong());
        inOrder.verify(spy).stateTransitTo(5L, KubernetesCluster.Event.OperationSucceeded);
        // the kubeconfig must point at the cluster's public endpoint
        Assert.assertEquals("10.1.1.10", spy.apiPublicIpAddress);
    }

    @Test
    public void testUpgradeSucceedsWhenTheKubeConfigRefreshFails() {
        KubernetesClusterUpgradeWorker spy = prepareUpgrade(true);
        Mockito.doReturn(false).when(spy).refreshKubeConfigData(ArgumentMatchers.anyLong());

        Assert.assertTrue(spy.upgradeCluster());

        Mockito.verify(spy).stateTransitTo(5L, KubernetesCluster.Event.OperationSucceeded);
        Mockito.verify(spy, Mockito.never()).stateTransitTo(5L, KubernetesCluster.Event.OperationFailed);
    }

    @Test
    public void testFailedUpgradeDoesNotRefreshTheKubeConfig() {
        KubernetesClusterUpgradeWorker spy = prepareUpgrade(false);

        Assert.assertFalse(spy.upgradeCluster());

        Mockito.verify(spy, Mockito.never()).refreshKubeConfigData(ArgumentMatchers.anyLong());
        Mockito.verify(spy).stateTransitTo(5L, KubernetesCluster.Event.OperationFailed);
    }
}
