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

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.network.Network;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashMap;
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
    private DataCenterDao dataCenterDao;
    @Mock
    private VMTemplateDao templateDao;

    private KubernetesClusterScaleWorker worker;

    private static final Long defaultOfferingId = 1L;
    private static final Long workerOfferingId = 2L;

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
        Mockito.when(kubernetesCluster.getServiceOfferingId()).thenReturn(defaultOfferingId);
        Mockito.when(kubernetesCluster.getControlNodeServiceOfferingId()).thenReturn(null);

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

        Pair<Long, Long> newClusterCapacity = worker.calculateNewClusterCountAndCapacity(null, CONTROL, newOffering);

        long expectedCores = remainingClusterCpu + (controlNodes * newCores);
        long expectedMemory = remainingClusterMemory + (controlNodes * newMemory);
        Assert.assertEquals(expectedCores, newClusterCapacity.first().longValue());
        Assert.assertEquals(expectedMemory, newClusterCapacity.second().longValue());
    }

    private ServiceOfferingVO mockOffering(Long offeringId, int cores, int memory) {
        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offering.getCpu()).thenReturn(cores);
        Mockito.when(offering.getRamSize()).thenReturn(memory);
        Mockito.when(serviceOfferingDao.findById(offeringId)).thenReturn(offering);
        return offering;
    }

    @Test
    public void testCalculateNewClusterCountAndCapacityWorkerOfferingScaleAfterVmUpgrade() {
        long workerNodes = 3L;
        long otherNodesCores = 4L;
        long otherNodesMemory = 4096L;
        Mockito.when(kubernetesCluster.getNodeCount()).thenReturn(workerNodes);
        Mockito.when(kubernetesCluster.getServiceOfferingId()).thenReturn(defaultOfferingId);
        Mockito.when(kubernetesCluster.getWorkerNodeServiceOfferingId()).thenReturn(workerOfferingId);
        mockOffering(workerOfferingId, 2, 2048);
        Mockito.when(kubernetesCluster.getCores()).thenReturn(otherNodesCores + workerNodes * 2);
        Mockito.when(kubernetesCluster.getMemory()).thenReturn(otherNodesMemory + workerNodes * 2048);
        ServiceOfferingVO newOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(newOffering.getCpu()).thenReturn(4);
        Mockito.when(newOffering.getRamSize()).thenReturn(8192);

        Pair<Long, Long> newClusterCapacity = worker.calculateNewClusterCountAndCapacity(null, WORKER, newOffering);

        Assert.assertEquals(otherNodesCores + workerNodes * 4, newClusterCapacity.first().longValue());
        Assert.assertEquals(otherNodesMemory + workerNodes * 8192, newClusterCapacity.second().longValue());
        Mockito.verifyNoInteractions(userVmDao);
    }

    private KubernetesClusterVmMapVO mockVmMap(long vmId, boolean externalNode) {
        KubernetesClusterVmMapVO vmMap = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.lenient().when(vmMap.getVmId()).thenReturn(vmId);
        Mockito.when(vmMap.isExternalNode()).thenReturn(externalNode);
        return vmMap;
    }

    @Test
    public void testCalculateNewClusterCountAndCapacityWorkerOfferingScaleKeepsAddedNodes() {
        long kubernetesClusterId = 10L;
        long cksWorkers = 3L;
        long addedNodeCores = 8L;
        long addedNodeMemory = 16384L;
        long otherNodesCores = 4L;
        long otherNodesMemory = 4096L;
        Mockito.when(kubernetesCluster.getId()).thenReturn(kubernetesClusterId);
        Mockito.when(kubernetesCluster.getNodeCount()).thenReturn(cksWorkers + 1);
        Mockito.when(kubernetesCluster.getServiceOfferingId()).thenReturn(defaultOfferingId);
        Mockito.when(kubernetesCluster.getWorkerNodeServiceOfferingId()).thenReturn(workerOfferingId);
        mockOffering(workerOfferingId, 2, 2048);
        Mockito.when(kubernetesCluster.getCores()).thenReturn(otherNodesCores + addedNodeCores + cksWorkers * 2);
        Mockito.when(kubernetesCluster.getMemory()).thenReturn(otherNodesMemory + addedNodeMemory + cksWorkers * 2048);
        List<KubernetesClusterVmMapVO> workerVmMaps = List.of(mockVmMap(11L, false), mockVmMap(12L, false), mockVmMap(13L, false), mockVmMap(14L, true));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterIdAndVmType(kubernetesClusterId, WORKER)).thenReturn(workerVmMaps);
        ServiceOfferingVO newOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(newOffering.getCpu()).thenReturn(4);
        Mockito.when(newOffering.getRamSize()).thenReturn(8192);

        Pair<Long, Long> newClusterCapacity = worker.calculateNewClusterCountAndCapacity(null, WORKER, newOffering);

        Assert.assertEquals(otherNodesCores + addedNodeCores + cksWorkers * 4, newClusterCapacity.first().longValue());
        Assert.assertEquals(otherNodesMemory + addedNodeMemory + cksWorkers * 8192, newClusterCapacity.second().longValue());
    }

    @Test
    public void testCalculateNewClusterCountAndCapacityWorkerSizeAndOfferingKeepsAddedNodes() {
        long kubernetesClusterId = 10L;
        long cksWorkers = 2L;
        long newWorkerSize = 5L;
        long addedNodeCores = 8L;
        long addedNodeMemory = 16384L;
        Mockito.when(kubernetesCluster.getId()).thenReturn(kubernetesClusterId);
        Mockito.when(kubernetesCluster.getNodeCount()).thenReturn(cksWorkers + 1);
        Mockito.when(kubernetesCluster.getServiceOfferingId()).thenReturn(defaultOfferingId);
        Mockito.when(kubernetesCluster.getWorkerNodeServiceOfferingId()).thenReturn(workerOfferingId);
        mockOffering(workerOfferingId, 2, 2048);
        Mockito.when(kubernetesCluster.getCores()).thenReturn(addedNodeCores + cksWorkers * 2);
        Mockito.when(kubernetesCluster.getMemory()).thenReturn(addedNodeMemory + cksWorkers * 2048);
        List<KubernetesClusterVmMapVO> workerVmMaps = List.of(mockVmMap(11L, false), mockVmMap(12L, false), mockVmMap(14L, true));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterIdAndVmType(kubernetesClusterId, WORKER)).thenReturn(workerVmMaps);
        ServiceOfferingVO newOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(newOffering.getCpu()).thenReturn(4);
        Mockito.when(newOffering.getRamSize()).thenReturn(8192);

        Pair<Long, Long> newClusterCapacity = worker.calculateNewClusterCountAndCapacity(newWorkerSize, WORKER, newOffering);

        Assert.assertEquals(addedNodeCores + (newWorkerSize - 1) * 4, newClusterCapacity.first().longValue());
        Assert.assertEquals(addedNodeMemory + (newWorkerSize - 1) * 8192, newClusterCapacity.second().longValue());
    }

    @Test
    public void testGetExistingOfferingIdForWorkerSkipsAddedNodes() {
        long kubernetesClusterId = 10L;
        Mockito.when(kubernetesCluster.getId()).thenReturn(kubernetesClusterId);
        List<KubernetesClusterVmMapVO> workerVmMaps = List.of(mockVmMap(14L, true), mockVmMap(11L, false));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterIdAndVmType(kubernetesClusterId, WORKER)).thenReturn(workerVmMaps);
        UserVmVO cksWorker = Mockito.mock(UserVmVO.class);
        Mockito.when(cksWorker.getServiceOfferingId()).thenReturn(workerOfferingId);
        Mockito.when(userVmDao.findById(11L)).thenReturn(cksWorker);

        Assert.assertEquals(workerOfferingId, worker.getExistingOfferingIdForNodeType(WORKER, kubernetesCluster));
        Mockito.verify(userVmDao, Mockito.never()).findById(14L);
    }

    @Test
    public void testGetExistingOfferingIdForWorkerWithOnlyAddedNodesUsesWorkerOffering() {
        long kubernetesClusterId = 10L;
        Mockito.when(kubernetesCluster.getId()).thenReturn(kubernetesClusterId);
        Mockito.when(kubernetesCluster.getWorkerNodeServiceOfferingId()).thenReturn(workerOfferingId);
        List<KubernetesClusterVmMapVO> workerVmMaps = List.of(mockVmMap(14L, true));
        Mockito.when(kubernetesClusterVmMapDao.listByClusterIdAndVmType(kubernetesClusterId, WORKER)).thenReturn(workerVmMaps);

        Assert.assertEquals(workerOfferingId, worker.getExistingOfferingIdForNodeType(WORKER, kubernetesCluster));
        Mockito.verifyNoInteractions(userVmDao);
    }

    @Test
    public void testCalculateNewClusterCountAndCapacityWorkerSizeFallsBackToClusterOffering() {
        long workerNodes = 3L;
        long newWorkerSize = 1L;
        long otherNodesCores = 4L;
        long otherNodesMemory = 4096L;
        Mockito.when(kubernetesCluster.getNodeCount()).thenReturn(workerNodes);
        Mockito.when(kubernetesCluster.getServiceOfferingId()).thenReturn(defaultOfferingId);
        Mockito.when(kubernetesCluster.getWorkerNodeServiceOfferingId()).thenReturn(null);
        ServiceOfferingVO clusterOffering = mockOffering(defaultOfferingId, 2, 2048);
        Mockito.when(kubernetesCluster.getCores()).thenReturn(otherNodesCores + workerNodes * 2);
        Mockito.when(kubernetesCluster.getMemory()).thenReturn(otherNodesMemory + workerNodes * 2048);

        Pair<Long, Long> newClusterCapacity = worker.calculateNewClusterCountAndCapacity(newWorkerSize, WORKER, clusterOffering);

        Assert.assertEquals(otherNodesCores + newWorkerSize * 2, newClusterCapacity.first().longValue());
        Assert.assertEquals(otherNodesMemory + newWorkerSize * 2048, newClusterCapacity.second().longValue());
    }

    @Test
    public void testUpdateKubernetesClusterEntryForWorkerSizeUsesWorkerOffering() {
        long workerNodes = 3L;
        long newWorkerSize = 5L;
        int workerCores = 2;
        int workerMemory = 2048;
        long otherNodesCores = 4L;
        long otherNodesMemory = 4096L;
        Mockito.when(kubernetesCluster.getNodeCount()).thenReturn(workerNodes);
        Mockito.when(kubernetesCluster.getCores()).thenReturn(otherNodesCores + workerNodes * workerCores);
        Mockito.when(kubernetesCluster.getMemory()).thenReturn(otherNodesMemory + workerNodes * workerMemory);
        Mockito.when(kubernetesCluster.getServiceOfferingId()).thenReturn(defaultOfferingId);
        Mockito.when(kubernetesCluster.getWorkerNodeServiceOfferingId()).thenReturn(workerOfferingId);
        mockOffering(workerOfferingId, workerCores, workerMemory);

        KubernetesClusterScaleWorker spyWorker = Mockito.spy(worker);
        KubernetesClusterVO updatedCluster = Mockito.mock(KubernetesClusterVO.class);
        Mockito.doReturn(updatedCluster).when(spyWorker).updateKubernetesClusterEntry(Mockito.anyLong(), Mockito.anyLong(),
                Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.anyBoolean(), Mockito.anyBoolean());

        Assert.assertSame(updatedCluster, spyWorker.updateKubernetesClusterEntryForNodeType(newWorkerSize, WORKER, null, false, false));

        long expectedCores = otherNodesCores + newWorkerSize * workerCores;
        long expectedMemory = otherNodesMemory + newWorkerSize * workerMemory;
        Mockito.verify(spyWorker).updateKubernetesClusterEntry(Mockito.eq(expectedCores), Mockito.eq(expectedMemory),
                Mockito.eq(newWorkerSize), Mockito.isNull(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq(WORKER),
                Mockito.eq(false), Mockito.eq(false));
    }

    @Test
    public void testValidateKubernetesClusterScaleSizeParametersPlansWithWorkerOffering() throws Exception {
        long kubernetesClusterId = 10L;
        long zoneId = 1L;
        long templateId = 3L;
        Mockito.when(kubernetesCluster.getState()).thenReturn(KubernetesCluster.State.Running);
        Mockito.when(kubernetesCluster.getId()).thenReturn(kubernetesClusterId);
        Mockito.when(kubernetesCluster.getNodeCount()).thenReturn(3L);
        Mockito.when(kubernetesCluster.getTotalNodeCount()).thenReturn(4L);
        Mockito.when(kubernetesCluster.getZoneId()).thenReturn(zoneId);
        Mockito.when(kubernetesCluster.getTemplateId()).thenReturn(templateId);
        Mockito.when(kubernetesCluster.getServiceOfferingId()).thenReturn(defaultOfferingId);
        Mockito.when(kubernetesCluster.getWorkerNodeServiceOfferingId()).thenReturn(workerOfferingId);
        ServiceOfferingVO workerOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(serviceOfferingDao.findById(workerOfferingId)).thenReturn(workerOffering);
        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        Mockito.when(dataCenterDao.findById(zoneId)).thenReturn(zone);
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        Mockito.when(template.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(templateDao.findById(templateId)).thenReturn(template);
        KubernetesClusterVmMapVO vmMap = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(kubernetesClusterId)).thenReturn(List.of(vmMap, vmMap, vmMap, vmMap));

        KubernetesClusterScaleWorker sizeWorker = new KubernetesClusterScaleWorker(kubernetesCluster, new HashMap<>(), 5L, null, null, null, null, clusterManager);
        sizeWorker.serviceOfferingDao = serviceOfferingDao;
        sizeWorker.dataCenterDao = dataCenterDao;
        sizeWorker.templateDao = templateDao;
        sizeWorker.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        ReflectionTestUtils.setField(sizeWorker, "network", Mockito.mock(Network.class));
        KubernetesClusterScaleWorker spyWorker = Mockito.spy(sizeWorker);
        Mockito.doReturn(null).when(spyWorker).plan(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any());

        spyWorker.validateKubernetesClusterScaleSizeParameters();

        Mockito.verify(spyWorker).plan(Mockito.eq(2L), Mockito.eq(zone), Mockito.eq(workerOffering), Mockito.any(),
                Mockito.any(), Mockito.eq(Hypervisor.HypervisorType.KVM), Mockito.any());
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
