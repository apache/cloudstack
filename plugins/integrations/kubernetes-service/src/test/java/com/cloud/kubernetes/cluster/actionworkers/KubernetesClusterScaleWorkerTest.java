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
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private KubernetesClusterScaleWorker mockScaleCluster(Map<String, ServiceOffering> offeringMap, Long clusterSize,
                                                          Boolean autoscalingEnabled, Long minSize, Long maxSize) {
        long clusterOfferingId = 1L;
        long workerOfferingId = 2L;
        Mockito.when(kubernetesCluster.getNodeCount()).thenReturn(3L);
        Mockito.lenient().when(kubernetesCluster.getServiceOfferingId()).thenReturn(clusterOfferingId);
        Mockito.lenient().when(kubernetesCluster.getAutoscalingEnabled()).thenReturn(false);

        ServiceOfferingVO clusterOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.lenient().when(clusterOffering.getId()).thenReturn(clusterOfferingId);
        ServiceOfferingVO workerOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.lenient().when(workerOffering.getId()).thenReturn(workerOfferingId);
        Mockito.lenient().when(serviceOfferingDao.findById(clusterOfferingId)).thenReturn(clusterOffering);
        Mockito.lenient().when(serviceOfferingDao.findById(workerOfferingId)).thenReturn(workerOffering);

        KubernetesClusterScaleWorker scaleWorker = new KubernetesClusterScaleWorker(kubernetesCluster, offeringMap, clusterSize,
                null, autoscalingEnabled, minSize, maxSize, clusterManager);
        scaleWorker.serviceOfferingDao = serviceOfferingDao;
        scaleWorker = Mockito.spy(scaleWorker);
        Mockito.doNothing().when(scaleWorker).init();
        Mockito.lenient().doReturn(clusterOfferingId).when(scaleWorker).getExistingOfferingIdForNodeType(Mockito.any(), Mockito.any());
        Mockito.lenient().doReturn(workerOfferingId).when(scaleWorker).getExistingOfferingIdForNodeType(WORKER, kubernetesCluster);
        Mockito.doReturn(true).when(scaleWorker).stateTransitTo(Mockito.anyLong(), Mockito.any());
        Mockito.lenient().doNothing().when(scaleWorker).scaleKubernetesClusterSize(Mockito.any());
        Mockito.lenient().doNothing().when(scaleWorker).scaleKubernetesClusterOffering(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.lenient().doReturn(true).when(scaleWorker).autoscaleCluster(Mockito.anyBoolean(), Mockito.any(), Mockito.any());
        return scaleWorker;
    }

    @Test
    public void testScaleClusterSizeOnlyKeepsNodeOfferings() {
        KubernetesClusterScaleWorker scaleWorker = mockScaleCluster(new HashMap<>(), 2L, null, null, null);
        Assert.assertTrue(scaleWorker.scaleCluster());
        Mockito.verify(scaleWorker).scaleKubernetesClusterSize(WORKER);
        Mockito.verify(scaleWorker, Mockito.never()).scaleKubernetesClusterOffering(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    @Test
    public void testScaleClusterAutoscalingOnlyKeepsNodeOfferings() {
        KubernetesClusterScaleWorker scaleWorker = mockScaleCluster(new HashMap<>(), null, true, 1L, 5L);
        Assert.assertTrue(scaleWorker.scaleCluster());
        Mockito.verify(scaleWorker).autoscaleCluster(true, 1L, 5L);
        Mockito.verify(scaleWorker, Mockito.never()).scaleKubernetesClusterOffering(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    @Test
    public void testScaleClusterAutoscalingDisableKeepsNodeOfferings() {
        KubernetesClusterScaleWorker scaleWorker = mockScaleCluster(new HashMap<>(), null, false, null, null);
        Mockito.when(kubernetesCluster.getAutoscalingEnabled()).thenReturn(true);
        Assert.assertTrue(scaleWorker.scaleCluster());
        Mockito.verify(scaleWorker).autoscaleCluster(false, null, null);
        Mockito.verify(scaleWorker, Mockito.never()).scaleKubernetesClusterOffering(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    @Test
    public void testScaleClusterDefaultOfferingScalesDifferingNodeType() {
        ServiceOfferingVO clusterOffering = Mockito.mock(ServiceOfferingVO.class);
        Map<String, ServiceOffering> offeringMap = new HashMap<>();
        offeringMap.put(DEFAULT.name(), clusterOffering);
        KubernetesClusterScaleWorker scaleWorker = mockScaleCluster(offeringMap, 2L, null, null, null);
        Mockito.when(clusterOffering.getId()).thenReturn(1L);
        Assert.assertTrue(scaleWorker.scaleCluster());
        Mockito.verify(scaleWorker).scaleKubernetesClusterSize(WORKER);
        Mockito.verify(scaleWorker).scaleKubernetesClusterOffering(WORKER, clusterOffering, true, true);
        Mockito.verify(scaleWorker, Mockito.times(1)).scaleKubernetesClusterOffering(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
    }
}
