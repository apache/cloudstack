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
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.dao.UserVmDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.CONTROL;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.DEFAULT;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.ETCD;
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
        Mockito.when(kubernetesCluster.getControlNodeCount()).thenReturn(controlNodes);
        Mockito.when(kubernetesCluster.getNodeCount()).thenReturn(4L);
        Mockito.when(kubernetesCluster.getWorkerNodeServiceOfferingId()).thenReturn(defaultOfferingId);
        int remainingClusterCpu = 8;
        int remainingClusterMemory = 12288;
        ServiceOfferingVO workerOffering = offering(2, 3072);
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(defaultOfferingId)).thenReturn(workerOffering);

        ServiceOfferingVO newOffering = Mockito.mock(ServiceOfferingVO.class);
        int newCores = 4;
        int newMemory = 2048;
        Mockito.when(newOffering.getCpu()).thenReturn(newCores);
        Mockito.when(newOffering.getRamSize()).thenReturn(newMemory);

        Pair<Long, Long> newClusterCapacity = worker.calculateNewClusterCountAndCapacity(null, CONTROL, newOffering);

        long expectedCores = remainingClusterCpu + (controlNodes * newCores);
        long expectedMemory = remainingClusterMemory + (controlNodes * newMemory);
        Assert.assertEquals(expectedCores, newClusterCapacity.first().longValue());
        Assert.assertEquals(expectedMemory, newClusterCapacity.second().longValue());
    }

    @Test
    public void testWorkerResizeRecomputesStaleTotalsWithoutReadingResizedVmOfferings() {
        Mockito.when(kubernetesCluster.getControlNodeCount()).thenReturn(1L);
        Mockito.when(kubernetesCluster.getNodeCount()).thenReturn(1L);
        Mockito.when(kubernetesCluster.getControlNodeServiceOfferingId()).thenReturn(2L);
        Mockito.doReturn(offering(3, 3072)).when(serviceOfferingDao).findByIdIncludingRemoved(2L);

        Pair<Long, Long> capacity = worker.calculateNewClusterCountAndCapacity(null, WORKER, offering(3, 3072));

        Assert.assertEquals(Long.valueOf(6), capacity.first());
        Assert.assertEquals(Long.valueOf(6144), capacity.second());
        Mockito.verify(kubernetesCluster, Mockito.never()).getCores();
        Mockito.verify(kubernetesCluster, Mockito.never()).getMemory();
        Mockito.verifyNoInteractions(userVmDao, kubernetesClusterVmMapDao);
    }

    @Test
    public void testWorkerCountChangePreservesMixedControlAndEtcdOfferings() {
        Mockito.when(kubernetesCluster.getControlNodeCount()).thenReturn(1L);
        Mockito.when(kubernetesCluster.getEtcdNodeCount()).thenReturn(3L);
        Mockito.when(kubernetesCluster.getControlNodeServiceOfferingId()).thenReturn(2L);
        Mockito.when(kubernetesCluster.getEtcdNodeServiceOfferingId()).thenReturn(3L);
        Mockito.doReturn(offering(4, 4096)).when(serviceOfferingDao).findByIdIncludingRemoved(2L);
        Mockito.doReturn(offering(2, 2048)).when(serviceOfferingDao).findByIdIncludingRemoved(3L);

        Pair<Long, Long> capacity = worker.calculateNewClusterCountAndCapacity(2L, WORKER, offering(3, 3072));

        Assert.assertEquals(Long.valueOf(16), capacity.first());
        Assert.assertEquals(Long.valueOf(16384), capacity.second());
    }

    @Test
    public void testCapacityFallsBackToClusterOfferingForLegacyNodeTypes() {
        Mockito.when(kubernetesCluster.getControlNodeCount()).thenReturn(1L);
        Mockito.when(kubernetesCluster.getControlNodeServiceOfferingId()).thenReturn(null);
        Mockito.when(kubernetesCluster.getServiceOfferingId()).thenReturn(defaultOfferingId);
        Mockito.doReturn(offering(2, 2048)).when(serviceOfferingDao).findByIdIncludingRemoved(defaultOfferingId);

        Pair<Long, Long> capacity = worker.calculateNewClusterCountAndCapacity(1L, WORKER, offering(3, 3072));

        Assert.assertEquals(Long.valueOf(5), capacity.first());
        Assert.assertEquals(Long.valueOf(5120), capacity.second());
    }

    private ServiceOfferingVO offering(int cpu, int memory) {
        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offering.getCpu()).thenReturn(cpu);
        Mockito.when(offering.getRamSize()).thenReturn(memory);
        return offering;
    }

    @Test
    public void testExistingOfferingUsesCommittedMetadataRatherThanPartiallyResizedFirstVm() {
        Mockito.when(kubernetesCluster.getWorkerNodeServiceOfferingId()).thenReturn(17L);
        Assert.assertEquals(Long.valueOf(17), worker.getExistingOfferingIdForNodeType(WORKER, kubernetesCluster));
        Mockito.verifyNoInteractions(userVmDao, kubernetesClusterVmMapDao);
    }

    @Test
    public void testWorkerPlanningUsesNodeOfferingWithoutLegacyGlobalOffering() {
        Mockito.when(kubernetesCluster.getWorkerNodeServiceOfferingId()).thenReturn(17L);
        ServiceOfferingVO workerOffering = offering(3, 3072);
        Mockito.when(serviceOfferingDao.findById(17L)).thenReturn(workerOffering);
        Assert.assertSame(workerOffering, worker.getServiceOfferingForNodeTypeOnCluster(WORKER, kubernetesCluster));
    }

    @Test
    public void testGlobalOfferingChangeSkipsAbsentDedicatedEtcdNodes() {
        Mockito.when(kubernetesCluster.getState()).thenReturn(KubernetesCluster.State.Running);
        Mockito.when(kubernetesCluster.getServiceOfferingId()).thenReturn(null);
        Mockito.when(kubernetesCluster.getControlNodeServiceOfferingId()).thenReturn(11L);
        Mockito.when(kubernetesCluster.getWorkerNodeServiceOfferingId()).thenReturn(12L);
        ServiceOfferingVO controlOffering = Mockito.mock(ServiceOfferingVO.class);
        ServiceOfferingVO workerOffering = Mockito.mock(ServiceOfferingVO.class);
        ServiceOfferingVO target = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(controlOffering.getId()).thenReturn(11L);
        Mockito.when(workerOffering.getId()).thenReturn(12L);
        Mockito.when(target.getId()).thenReturn(13L);
        Mockito.when(serviceOfferingDao.findById(11L)).thenReturn(controlOffering);
        Mockito.when(serviceOfferingDao.findById(12L)).thenReturn(workerOffering);
        KubernetesClusterScaleWorker scaleWorker = Mockito.spy(new KubernetesClusterScaleWorker(kubernetesCluster,
                java.util.Map.of(DEFAULT.name(), target), null, null, null, null, null, clusterManager));
        scaleWorker.serviceOfferingDao = serviceOfferingDao;
        Mockito.doNothing().when(scaleWorker).init();
        Mockito.doNothing().when(scaleWorker).scaleKubernetesClusterOffering(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.doReturn(true).when(scaleWorker).stateTransitTo(Mockito.anyLong(), Mockito.any());

        Assert.assertTrue(scaleWorker.scaleCluster());

        Mockito.verify(scaleWorker).scaleKubernetesClusterOffering(CONTROL, target, true, false);
        Mockito.verify(scaleWorker).scaleKubernetesClusterOffering(WORKER, target, true, true);
        Mockito.verify(scaleWorker, Mockito.never()).scaleKubernetesClusterOffering(Mockito.eq(ETCD), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    @Test
    public void testSameCommittedOfferingRetryChecksBothWorkersWithoutChangingTheirOfferings() {
        Mockito.when(kubernetesCluster.getState()).thenReturn(KubernetesCluster.State.Running);
        Mockito.when(kubernetesCluster.getClusterType()).thenReturn(KubernetesCluster.ClusterType.CloudManaged);
        Mockito.when(kubernetesCluster.getNodeCount()).thenReturn(2L);
        Mockito.when(kubernetesCluster.getWorkerNodeServiceOfferingId()).thenReturn(11L);
        ServiceOfferingVO committed = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(committed.getId()).thenReturn(11L);
        Mockito.when(serviceOfferingDao.findById(11L)).thenReturn(committed);
        KubernetesClusterVmMapVO first = Mockito.mock(KubernetesClusterVmMapVO.class);
        KubernetesClusterVmMapVO second = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId())).thenReturn(List.of(first, second));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterIdAndVmType(kubernetesCluster.getId(), WORKER)).thenReturn(List.of(first, second));
        KubernetesClusterScaleWorker scaleWorker = Mockito.spy(new KubernetesClusterScaleWorker(kubernetesCluster,
                java.util.Map.of(WORKER.name(), committed), null, null, null, null, null, clusterManager));
        scaleWorker.serviceOfferingDao = serviceOfferingDao;
        scaleWorker.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        KubernetesClusterNodeOfferingResizer resizer = Mockito.mock(KubernetesClusterNodeOfferingResizer.class);
        Mockito.doReturn(resizer).when(scaleWorker).createNodeOfferingResizer();
        Mockito.doNothing().when(scaleWorker).init();
        Mockito.doReturn(true).when(scaleWorker).stateTransitTo(Mockito.anyLong(), Mockito.any());

        Assert.assertTrue(scaleWorker.scaleCluster());

        Mockito.verify(resizer).reconcilePending(Mockito.eq(first), Mockito.eq(WORKER), Mockito.eq(committed), Mockito.anyLong());
        Mockito.verify(resizer).reconcilePending(Mockito.eq(second), Mockito.eq(WORKER), Mockito.eq(committed), Mockito.anyLong());
        Mockito.verifyNoMoreInteractions(resizer);
        Mockito.verify(scaleWorker, Mockito.never()).scaleKubernetesClusterOffering(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    @Test
    public void testRetryUsesUncommittedWorkerOfferingToEnterResizeLoop() {
        Mockito.when(kubernetesCluster.getState()).thenReturn(KubernetesCluster.State.Running);
        Mockito.when(kubernetesCluster.getWorkerNodeServiceOfferingId()).thenReturn(11L);
        ServiceOfferingVO old = Mockito.mock(ServiceOfferingVO.class);
        ServiceOfferingVO target = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(old.getId()).thenReturn(11L);
        Mockito.when(target.getId()).thenReturn(12L);
        Mockito.when(serviceOfferingDao.findById(11L)).thenReturn(old);
        KubernetesClusterScaleWorker scaleWorker = Mockito.spy(new KubernetesClusterScaleWorker(kubernetesCluster,
                java.util.Map.of(WORKER.name(), target), null, null, null, null, null, clusterManager));
        scaleWorker.serviceOfferingDao = serviceOfferingDao;
        Mockito.doNothing().when(scaleWorker).init();
        Mockito.doNothing().when(scaleWorker).scaleKubernetesClusterOffering(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.doReturn(true).when(scaleWorker).stateTransitTo(Mockito.anyLong(), Mockito.any());

        Assert.assertTrue(scaleWorker.scaleCluster());

        Mockito.verify(scaleWorker).scaleKubernetesClusterOffering(WORKER, target, true, false);
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

    @Test
    public void testUsesLiveResizeReconcilerWithoutComponentContextRegistration() {
        Assert.assertNotNull(worker.kubernetesClusterNodeCapacityReconciler);
    }

    @Test
    public void testMissingOfferingOfALaterNodeTypeFailsBeforeAnyVmIsResized() {
        Mockito.when(kubernetesCluster.getState()).thenReturn(KubernetesCluster.State.Running);
        Mockito.when(kubernetesCluster.getControlNodeServiceOfferingId()).thenReturn(11L);
        Mockito.when(kubernetesCluster.getWorkerNodeServiceOfferingId()).thenReturn(12L);
        ServiceOfferingVO controlOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(serviceOfferingDao.findById(11L)).thenReturn(controlOffering);
        ServiceOfferingVO target = Mockito.mock(ServiceOfferingVO.class);
        KubernetesClusterScaleWorker scaleWorker = Mockito.spy(new KubernetesClusterScaleWorker(kubernetesCluster,
                java.util.Map.of(CONTROL.name(), target, WORKER.name(), target), null, null, null, null, null, clusterManager));
        scaleWorker.serviceOfferingDao = serviceOfferingDao;
        Mockito.doNothing().when(scaleWorker).init();

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, scaleWorker::scaleCluster);

        Assert.assertTrue(error.getMessage(), error.getMessage().contains("ID 12") && error.getMessage().contains("WORKER"));

        Mockito.verify(scaleWorker, Mockito.never()).scaleKubernetesClusterOffering(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
    }
}
