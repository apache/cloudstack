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
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
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
import java.util.List;

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

    private KubernetesClusterVmMapVO mockWorkerVmMap(boolean external) {
        KubernetesClusterVmMapVO vm = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(vm.isExternalNode()).thenReturn(external);
        Mockito.lenient().when(vm.isControlNode()).thenReturn(false);
        Mockito.lenient().when(vm.isEtcdNode()).thenReturn(false);
        return vm;
    }

    private List<KubernetesClusterVmMapVO> getWorkerNodesToRemoveWithAddedNodes(long controlNodeCount, long newSize, List<KubernetesClusterVmMapVO> vmMaps) {
        KubernetesCluster cluster = Mockito.mock(KubernetesCluster.class);
        Mockito.when(cluster.getControlNodeCount()).thenReturn(controlNodeCount);
        KubernetesClusterScaleWorker worker = new KubernetesClusterScaleWorker(cluster, new java.util.HashMap<>(), newSize, null, false, null, null, clusterManager);
        KubernetesClusterScaleWorker spyWorker = Mockito.spy(worker);
        Mockito.doReturn(vmMaps).when(spyWorker).getKubernetesClusterVMMaps();
        return spyWorker.getWorkerNodesToRemove();
    }

    @Test
    public void testGetWorkerNodesToRemoveForDownsizeWithAddedNodes() {
        KubernetesClusterVmMapVO control = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(control.isExternalNode()).thenReturn(false);
        Mockito.when(control.isControlNode()).thenReturn(true);
        KubernetesClusterVmMapVO vm1 = mockWorkerVmMap(false);
        KubernetesClusterVmMapVO added1 = mockWorkerVmMap(true);
        KubernetesClusterVmMapVO added2 = mockWorkerVmMap(true);
        KubernetesClusterVmMapVO vm2 = mockWorkerVmMap(false);
        KubernetesClusterVmMapVO vm3 = mockWorkerVmMap(false);
        KubernetesClusterVmMapVO vm4 = mockWorkerVmMap(false);
        List<KubernetesClusterVmMapVO> vmMaps = Arrays.asList(control, vm1, added1, added2, vm2, vm3, vm4);

        List<KubernetesClusterVmMapVO> toRemove = getWorkerNodesToRemoveWithAddedNodes(1L, 5L, vmMaps);
        Assert.assertEquals(List.of(vm4), toRemove);

        toRemove = getWorkerNodesToRemoveWithAddedNodes(1L, 2L, vmMaps);
        Assert.assertEquals(Arrays.asList(vm1, vm2, vm3, vm4), toRemove);
    }

    private KubernetesClusterVmMapVO mockNodeVmMap(long vmId, boolean control, boolean etcd, boolean external) {
        KubernetesClusterVmMapVO vm = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.lenient().when(vm.getVmId()).thenReturn(vmId);
        Mockito.lenient().when(vm.isControlNode()).thenReturn(control);
        Mockito.lenient().when(vm.isEtcdNode()).thenReturn(etcd);
        Mockito.lenient().when(vm.isExternalNode()).thenReturn(external);
        return vm;
    }

    // 1 control, 1 etcd, 3 CKS-created workers (ids 3-5) and 2 added nodes (ids 6-7)
    private KubernetesClusterScaleWorker mockSyncWorker(long storedNodeCount, Long newSize, List<Long> removeNodeIds) {
        KubernetesCluster cluster = Mockito.mock(KubernetesCluster.class);
        Mockito.lenient().when(cluster.getId()).thenReturn(1L);
        Mockito.lenient().when(cluster.getNodeCount()).thenReturn(storedNodeCount);
        Mockito.lenient().when(cluster.getControlNodeCount()).thenReturn(1L);
        Mockito.lenient().when(cluster.getEtcdNodeCount()).thenReturn(1L);
        KubernetesClusterScaleWorker worker = new KubernetesClusterScaleWorker(cluster, new java.util.HashMap<>(), newSize, removeNodeIds, false, null, null, clusterManager);
        worker.kubernetesClusterDao = Mockito.mock(KubernetesClusterDao.class);
        worker.serviceOfferingDao = serviceOfferingDao;
        worker.userVmDao = userVmDao;
        KubernetesClusterScaleWorker spyWorker = Mockito.spy(worker);
        Mockito.doReturn(Arrays.asList(mockNodeVmMap(1L, true, false, false), mockNodeVmMap(2L, false, true, false),
                mockNodeVmMap(3L, false, false, false), mockNodeVmMap(4L, false, false, false), mockNodeVmMap(5L, false, false, false),
                mockNodeVmMap(6L, false, false, true), mockNodeVmMap(7L, false, false, true))).when(spyWorker).getKubernetesClusterVMMaps();
        return spyWorker;
    }

    // Control: 4 vCPU / 4 GB, etcd: 2 / 2 GB, CKS workers: 2 / 2 GB each, added nodes: 8 / 8 GB each
    private void mockClusterVmOfferings(long lastVmId) {
        for (long vmId = 1; vmId <= lastVmId; vmId++) {
            int cpu = vmId == 1 ? 4 : vmId == 2 ? 2 : vmId <= 5 ? 2 : 8;
            UserVmVO userVm = Mockito.mock(UserVmVO.class);
            Mockito.when(userVm.getId()).thenReturn(vmId);
            Mockito.when(userVm.getServiceOfferingId()).thenReturn(100 + vmId);
            Mockito.when(userVmDao.findById(vmId)).thenReturn(userVm);
            ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
            Mockito.when(offering.getCpu()).thenReturn(cpu);
            Mockito.when(offering.getRamSize()).thenReturn(cpu * 1024);
            if (vmId == 7) {
                // A custom offering, sized by the VM's details
                ServiceOfferingVO customOffering = Mockito.mock(ServiceOfferingVO.class);
                Mockito.when(customOffering.isDynamic()).thenReturn(true);
                Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(100 + vmId)).thenReturn(customOffering);
                Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(vmId, 100 + vmId)).thenReturn(offering);
            } else {
                Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(100 + vmId)).thenReturn(offering);
            }
        }
    }

    private KubernetesClusterVO mockClusterVO(KubernetesClusterScaleWorker worker) {
        KubernetesClusterVO clusterVO = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(clusterVO.getId()).thenReturn(1L);
        Mockito.when(worker.kubernetesClusterDao.findById(1L)).thenReturn(clusterVO);
        return clusterVO;
    }

    // Covers both drift shapes: the stored cores and memory are never read, they are recomputed from the VMs
    @Test
    public void testValidateAndSyncWorkerNodeCountCorrectsDriftedSize() {
        KubernetesClusterScaleWorker worker = mockSyncWorker(3L, 5L, null);
        KubernetesClusterVO clusterVO = mockClusterVO(worker);
        mockClusterVmOfferings(7);

        worker.validateAndSyncWorkerNodeCount();

        Mockito.verify(clusterVO).setNodeCount(5L);
        Mockito.verify(clusterVO).setCores(28L);
        Mockito.verify(clusterVO).setMemory(28672L);
        Mockito.verify(clusterVO, Mockito.never()).getCores();
        Mockito.verify(clusterVO, Mockito.never()).getMemory();
        Mockito.verify(worker.kubernetesClusterDao).update(1L, clusterVO);
        Assert.assertSame(clusterVO, ReflectionTestUtils.getField(worker, "kubernetesCluster"));
    }

    @Test
    public void testValidateAndSyncWorkerNodeCountMissingOfferingKeepsCapacity() {
        KubernetesClusterScaleWorker worker = mockSyncWorker(3L, 5L, null);
        KubernetesClusterVO clusterVO = mockClusterVO(worker);
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        Mockito.when(userVm.getServiceOfferingId()).thenReturn(101L);
        Mockito.when(userVmDao.findById(1L)).thenReturn(userVm);

        worker.validateAndSyncWorkerNodeCount();

        Mockito.verify(serviceOfferingDao, Mockito.never()).findByIdIncludingRemoved(Mockito.anyLong(), Mockito.anyLong());
        Mockito.verify(clusterVO).setNodeCount(5L);
        Mockito.verify(clusterVO, Mockito.never()).setCores(Mockito.anyLong());
        Mockito.verify(clusterVO, Mockito.never()).setMemory(Mockito.anyLong());
        Mockito.verify(worker.kubernetesClusterDao).update(1L, clusterVO);
    }

    @Test
    public void testValidateAndSyncWorkerNodeCountMissingVmKeepsCapacity() {
        KubernetesClusterScaleWorker worker = mockSyncWorker(3L, 5L, null);
        KubernetesClusterVO clusterVO = mockClusterVO(worker);
        Mockito.when(userVmDao.findById(Mockito.anyLong())).thenReturn(null);

        worker.validateAndSyncWorkerNodeCount();

        Mockito.verify(clusterVO).setNodeCount(5L);
        Mockito.verify(clusterVO, Mockito.never()).setCores(Mockito.anyLong());
        Mockito.verify(clusterVO, Mockito.never()).setMemory(Mockito.anyLong());
        Mockito.verify(worker.kubernetesClusterDao).update(1L, clusterVO);
    }

    @Test
    public void testValidateAndSyncWorkerNodeCountCustomOfferingWithoutDetailsKeepsCapacity() {
        KubernetesClusterScaleWorker worker = mockSyncWorker(3L, 5L, null);
        KubernetesClusterVO clusterVO = mockClusterVO(worker);
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        Mockito.when(userVm.getId()).thenReturn(1L);
        Mockito.when(userVm.getServiceOfferingId()).thenReturn(101L);
        Mockito.when(userVmDao.findById(1L)).thenReturn(userVm);
        ServiceOfferingVO customOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(customOffering.isDynamic()).thenReturn(true);
        Mockito.when(customOffering.getCpu()).thenReturn(null);
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(101L)).thenReturn(customOffering);
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(1L, 101L)).thenReturn(customOffering);

        worker.validateAndSyncWorkerNodeCount();

        Mockito.verify(clusterVO).setNodeCount(5L);
        Mockito.verify(clusterVO, Mockito.never()).setCores(Mockito.anyLong());
        Mockito.verify(clusterVO, Mockito.never()).setMemory(Mockito.anyLong());
        Mockito.verify(worker.kubernetesClusterDao).update(1L, clusterVO);
    }

    @Test
    public void testValidateAndSyncWorkerNodeCountInvalidCustomDetailsKeepsCapacity() {
        KubernetesClusterScaleWorker worker = mockSyncWorker(3L, 5L, null);
        KubernetesClusterVO clusterVO = mockClusterVO(worker);
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        Mockito.when(userVm.getId()).thenReturn(1L);
        Mockito.when(userVm.getServiceOfferingId()).thenReturn(101L);
        Mockito.when(userVmDao.findById(1L)).thenReturn(userVm);
        ServiceOfferingVO customOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(customOffering.isDynamic()).thenReturn(true);
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(101L)).thenReturn(customOffering);
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(1L, 101L)).thenThrow(new NumberFormatException());

        worker.validateAndSyncWorkerNodeCount();

        Mockito.verify(clusterVO).setNodeCount(5L);
        Mockito.verify(clusterVO, Mockito.never()).setCores(Mockito.anyLong());
        Mockito.verify(clusterVO, Mockito.never()).setMemory(Mockito.anyLong());
        Mockito.verify(worker.kubernetesClusterDao).update(1L, clusterVO);
    }

    @Test
    public void testValidateAndSyncWorkerNodeCountStoredSizePassedBack() {
        KubernetesClusterScaleWorker worker = mockSyncWorker(3L, 3L, null);
        mockClusterVO(worker);
        mockClusterVmOfferings(7);
        worker.validateAndSyncWorkerNodeCount();
        Assert.assertEquals(5L, ReflectionTestUtils.getField(worker, "clusterSize"));
    }

    @Test
    public void testValidateAndSyncWorkerNodeCountCorrectsControlAndEtcdCounts() {
        KubernetesClusterScaleWorker worker = mockSyncWorker(4L, 4L, null);
        Mockito.when(worker.kubernetesCluster.getControlNodeCount()).thenReturn(2L);
        Mockito.when(worker.kubernetesCluster.getEtcdNodeCount()).thenReturn(2L);
        KubernetesClusterVO clusterVO = mockClusterVO(worker);
        mockClusterVmOfferings(7);

        worker.validateAndSyncWorkerNodeCount();

        Mockito.verify(clusterVO).setControlNodeCount(1L);
        Mockito.verify(clusterVO).setEtcdNodeCount(1L);
        Mockito.verify(clusterVO).setNodeCount(5L);
        Mockito.verify(clusterVO).setCores(28L);
        Mockito.verify(clusterVO).setMemory(28672L);
        Assert.assertEquals(5L, ReflectionTestUtils.getField(worker, "clusterSize"));
    }

    @Test
    public void testValidateAndSyncWorkerNodeCountWithoutControlNodeFlags() {
        KubernetesClusterScaleWorker worker = mockSyncWorker(3L, 3L, null);
        Mockito.doReturn(Arrays.asList(mockNodeVmMap(1L, false, false, false), mockNodeVmMap(2L, false, true, false),
                mockNodeVmMap(3L, false, false, false), mockNodeVmMap(4L, false, false, false), mockNodeVmMap(5L, false, false, false),
                mockNodeVmMap(6L, false, false, true), mockNodeVmMap(7L, false, false, true))).when(worker).getKubernetesClusterVMMaps();
        worker.validateAndSyncWorkerNodeCount();
        Mockito.verify(worker.kubernetesClusterDao, Mockito.never()).update(Mockito.anyLong(), Mockito.any());
        Assert.assertEquals(3L, ReflectionTestUtils.getField(worker, "clusterSize"));
    }

    @Test
    public void testValidateAndSyncWorkerNodeCountNoDrift() {
        KubernetesClusterScaleWorker worker = mockSyncWorker(5L, null, List.of(4L));
        worker.validateAndSyncWorkerNodeCount();
        Mockito.verify(worker.kubernetesClusterDao, Mockito.never()).update(Mockito.anyLong(), Mockito.any());
        Mockito.verify(userVmDao, Mockito.never()).findById(Mockito.anyLong());
        Assert.assertEquals(4L, ReflectionTestUtils.getField(worker, "clusterSize"));
    }

    private void assertValidateAndSyncFails(KubernetesClusterScaleWorker worker, String message) {
        try {
            worker.validateAndSyncWorkerNodeCount();
            Assert.fail("Expected the scale request to be rejected");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains(message));
        }
        Mockito.verify(worker.kubernetesClusterDao, Mockito.never()).update(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void testValidateAndSyncWorkerNodeCountDriftedNewSize() {
        assertValidateAndSyncFails(mockSyncWorker(3L, 4L, null), "records size 3 but has 5 worker nodes");
    }

    @Test
    public void testValidateAndSyncWorkerNodeCountNoWorkerNodes() {
        KubernetesClusterScaleWorker worker = mockSyncWorker(3L, 3L, null);
        Mockito.doReturn(Arrays.asList(mockNodeVmMap(1L, true, false, false), mockNodeVmMap(2L, false, true, false))).when(worker).getKubernetesClusterVMMaps();
        assertValidateAndSyncFails(worker, "it has no worker nodes");
    }

    @Test
    public void testValidateAndSyncWorkerNodeCountBelowAddedNodes() {
        assertValidateAndSyncFails(mockSyncWorker(5L, 1L, null), "size 1 is below the 2 nodes added");
    }

    @Test
    public void testValidateAndSyncWorkerNodeCountRejectsRemovingAllNodes() {
        KubernetesClusterScaleWorker worker = mockSyncWorker(5L, null, List.of(3L, 4L, 5L));
        Mockito.doReturn(Arrays.asList(mockNodeVmMap(1L, true, false, false), mockNodeVmMap(3L, false, false, false),
                mockNodeVmMap(4L, false, false, false), mockNodeVmMap(5L, false, false, false))).when(worker).getKubernetesClusterVMMaps();
        assertValidateAndSyncFails(worker, "can not remove all nodes");
    }

    @Test
    public void testValidateAndSyncWorkerNodeCountRejectsNonWorkerNodeIds() {
        for (long nodeId : List.of(1L, 2L, 6L, 99L)) {
            assertValidateAndSyncFails(mockSyncWorker(5L, null, List.of(3L, nodeId)), "only CKS-created worker nodes of the cluster can be removed");
        }
    }

    @Test
    public void testGetWorkerNodesToRemoveWithoutControlNodeFlags() {
        KubernetesClusterVmMapVO unflaggedControl = mockWorkerVmMap(false);
        KubernetesClusterVmMapVO vm1 = mockWorkerVmMap(false);
        List<KubernetesClusterVmMapVO> vmMaps = Arrays.asList(unflaggedControl, vm1, mockWorkerVmMap(true), mockWorkerVmMap(true));
        Assert.assertEquals(List.of(vm1), getWorkerNodesToRemoveWithAddedNodes(1L, 2L, vmMaps));
    }

    @Test
    public void testGetWorkerNodesToRemoveForUpsizeWithAddedNodes() {
        List<KubernetesClusterVmMapVO> vmMaps = Arrays.asList(mockWorkerVmMap(false), mockWorkerVmMap(true), mockWorkerVmMap(true), mockWorkerVmMap(false));
        Assert.assertTrue(getWorkerNodesToRemoveWithAddedNodes(0L, 4L, vmMaps).isEmpty());
        Assert.assertTrue(getWorkerNodesToRemoveWithAddedNodes(0L, 6L, vmMaps).isEmpty());
    }
}
