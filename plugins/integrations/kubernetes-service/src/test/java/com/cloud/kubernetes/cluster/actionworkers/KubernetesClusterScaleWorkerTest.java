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
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.network.Network;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.dao.UserVmDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.CONTROL;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.DEFAULT;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.WORKER;

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
    @Mock
    private UserVmManager userVmManager;

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
    public void testNodeIdsScaleInPreservesMixedOfferingsAndRemovesSelectedWorker() {
        assertScaleInPreservesOfferings(true, false);
    }

    @Test
    public void testCountOnlyScaleInPreservesMixedOfferings() {
        assertScaleInPreservesOfferings(false, false);
    }

    @Test
    public void testNodeIdsScaleInPreservesLegacyGlobalOffering() {
        assertScaleInPreservesOfferings(true, true);
    }

    private void assertScaleInPreservesOfferings(boolean selectedNode, boolean legacyOffering) {
        Mockito.when(kubernetesCluster.getId()).thenReturn(7L);
        Mockito.when(kubernetesCluster.getState()).thenReturn(KubernetesCluster.State.Running);
        Mockito.when(kubernetesCluster.getNodeCount()).thenReturn(2L);
        Mockito.when(kubernetesCluster.getTotalNodeCount()).thenReturn(3L);
        Mockito.when(kubernetesCluster.getServiceOfferingId()).thenReturn(defaultOfferingId);
        Mockito.when(kubernetesCluster.getMinSize()).thenReturn(null);
        Mockito.when(kubernetesCluster.getMaxSize()).thenReturn(null);
        Mockito.when(kubernetesCluster.getCores()).thenReturn(legacyOffering ? 6L : 9L);
        Mockito.when(kubernetesCluster.getMemory()).thenReturn(legacyOffering ? 6144L : 11264L);
        ServiceOfferingVO legacy = Mockito.mock(ServiceOfferingVO.class);
        if (legacyOffering) {
            Mockito.when(legacy.getCpu()).thenReturn(2);
            Mockito.when(legacy.getRamSize()).thenReturn(2048);
        }
        Mockito.when(serviceOfferingDao.findById(defaultOfferingId)).thenReturn(legacy);
        long workerOfferingId = legacyOffering ? defaultOfferingId : 12L;
        Mockito.when(kubernetesCluster.getWorkerNodeServiceOfferingId()).thenReturn(legacyOffering ? null : workerOfferingId);
        if (!legacyOffering) {
            ServiceOfferingVO workerOffering = Mockito.mock(ServiceOfferingVO.class);
            Mockito.when(workerOffering.getCpu()).thenReturn(3);
            Mockito.when(workerOffering.getRamSize()).thenReturn(4096);
            Mockito.when(serviceOfferingDao.findById(workerOfferingId)).thenReturn(workerOffering);
        }
        KubernetesClusterVmMapVO control = Mockito.mock(KubernetesClusterVmMapVO.class);
        KubernetesClusterVmMapVO retained = Mockito.mock(KubernetesClusterVmMapVO.class);
        KubernetesClusterVmMapVO removed = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(retained.getVmId()).thenReturn(100L);
        UserVmVO retainedVm = Mockito.mock(UserVmVO.class);
        Mockito.when(retainedVm.getServiceOfferingId()).thenReturn(workerOfferingId);
        Mockito.when(userVmDao.findById(100L)).thenReturn(retainedVm);
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(7L)).thenReturn(Arrays.asList(control, retained, removed));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterIdAndVmType(7L, WORKER)).thenReturn(Arrays.asList(retained, removed));
        KubernetesClusterScaleWorker scaleWorker = Mockito.spy(new KubernetesClusterScaleWorker(kubernetesCluster,
                Collections.emptyMap(), selectedNode ? null : 1L, selectedNode ? List.of(101L) : null, null, null, null, clusterManager));
        scaleWorker.serviceOfferingDao = serviceOfferingDao;
        scaleWorker.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        scaleWorker.userVmDao = userVmDao;
        scaleWorker.userVmManager = userVmManager;
        Network network = Mockito.mock(Network.class);
        ReflectionTestUtils.setField(scaleWorker, "network", network);
        Mockito.doNothing().when(scaleWorker).init();
        Mockito.doReturn(new Pair<>("192.0.2.1", 2222)).when(scaleWorker).getKubernetesClusterServerIpSshPort(null);
        Mockito.doReturn("192.0.2.1").when(scaleWorker).resolveApiPublicIpAddress(network);
        Mockito.doReturn(true).when(scaleWorker).stateTransitTo(Mockito.anyLong(), Mockito.any());
        if (selectedNode) {
            Mockito.doReturn(new ArrayList<>(List.of(removed))).when(scaleWorker).getKubernetesClusterVMMapsForNodes(List.of(101L));
        } else {
            Mockito.doReturn(new ArrayList<>(List.of(removed))).when(scaleWorker).getWorkerNodesToRemove();
        }
        Mockito.doNothing().when(scaleWorker).removeNodesFromCluster(Mockito.anyList());
        KubernetesClusterVO updatedCluster = Mockito.mock(KubernetesClusterVO.class);
        Mockito.doReturn(updatedCluster).when(scaleWorker).updateKubernetesClusterEntry(Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());

        Assert.assertTrue(scaleWorker.scaleCluster());

        Mockito.verify(scaleWorker).removeNodesFromCluster(List.of(removed));
        Mockito.verify(scaleWorker).updateKubernetesClusterEntry(legacyOffering ? 4L : 6L, legacyOffering ? 4096L : 7168L,
                1L, null, false, null, null, WORKER, false, false);
        Mockito.verifyNoInteractions(userVmManager);
        Mockito.verify(kubernetesClusterVmMapDao, Mockito.never()).listByClusterIdAndVmType(7L, CONTROL);
    }

    @Test
    public void testAutoscalingOnlyEnablePreservesOfferings() {
        assertAutoscalingPreservesOfferings(false, true, false);
    }

    @Test
    public void testAutoscalingOnlyDisablePreservesOfferings() {
        assertAutoscalingPreservesOfferings(true, false, false);
    }

    @Test
    public void testAutoscalingBoundsOnlyChangePreservesOfferings() {
        assertAutoscalingPreservesOfferings(true, true, true);
    }

    private void assertAutoscalingPreservesOfferings(boolean existingEnabled, boolean requestedEnabled, boolean boundsChange) {
        Mockito.when(kubernetesCluster.getState()).thenReturn(KubernetesCluster.State.Running);
        Mockito.when(kubernetesCluster.getAutoscalingEnabled()).thenReturn(existingEnabled);
        if (boundsChange) {
            Mockito.when(kubernetesCluster.getMinSize()).thenReturn(1L);
        }
        KubernetesClusterScaleWorker scaleWorker = Mockito.spy(new KubernetesClusterScaleWorker(kubernetesCluster,
                Collections.emptyMap(), null, null, requestedEnabled, 2L, 5L, clusterManager));
        scaleWorker.serviceOfferingDao = serviceOfferingDao;
        scaleWorker.userVmManager = userVmManager;
        ServiceOfferingVO existingOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(serviceOfferingDao.findById(12L)).thenReturn(existingOffering);
        Mockito.doNothing().when(scaleWorker).init();
        Mockito.doReturn(12L).when(scaleWorker).getExistingOfferingIdForNodeType(WORKER, kubernetesCluster);
        Mockito.doReturn(true).when(scaleWorker).autoscaleCluster(requestedEnabled, 2L, 5L);
        Mockito.doReturn(true).when(scaleWorker).stateTransitTo(Mockito.anyLong(), Mockito.any());

        Assert.assertTrue(scaleWorker.scaleCluster());

        Mockito.verify(scaleWorker).autoscaleCluster(requestedEnabled, 2L, 5L);
        Mockito.verifyNoInteractions(userVmManager);
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
}
