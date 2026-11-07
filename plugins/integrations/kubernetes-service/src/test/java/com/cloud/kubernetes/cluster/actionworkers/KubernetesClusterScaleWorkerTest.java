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
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
    @Mock
    private VMInstanceDao vmInstanceDao;

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
        worker.vmInstanceDao = vmInstanceDao;
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
        worker.vmInstanceDao = vmInstanceDao;
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

    private KubernetesClusterVmMapVO mockWorkerVmMap(long vmId, VirtualMachine.State state) {
        KubernetesClusterVmMapVO vmMap = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.lenient().when(vmMap.isExternalNode()).thenReturn(false);
        Mockito.lenient().when(vmMap.isControlNode()).thenReturn(false);
        Mockito.lenient().when(vmMap.isEtcdNode()).thenReturn(false);
        Mockito.lenient().when(vmMap.getVmId()).thenReturn(vmId);
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.lenient().when(vm.getState()).thenReturn(state);
        Mockito.lenient().when(vmInstanceDao.findById(vmId)).thenReturn(vm);
        return vmMap;
    }

    @Test
    public void testGetWorkerNodesToRemovePrefersNodesDown() {
        KubernetesClusterScaleWorker worker = new KubernetesClusterScaleWorker(kubernetesCluster, new java.util.HashMap<>(), 2L, null, false, null, null, clusterManager);
        worker.vmInstanceDao = vmInstanceDao;
        KubernetesClusterScaleWorker spyWorker = Mockito.spy(worker);

        KubernetesClusterVmMapVO vm1 = mockWorkerVmMap(1L, VirtualMachine.State.Running);
        KubernetesClusterVmMapVO vm2 = mockWorkerVmMap(2L, VirtualMachine.State.Stopped);
        KubernetesClusterVmMapVO vm3 = mockWorkerVmMap(3L, VirtualMachine.State.Running);
        KubernetesClusterVmMapVO vm4 = mockWorkerVmMap(4L, VirtualMachine.State.Running);
        Mockito.doReturn(Arrays.asList(vm1, vm2, vm3, vm4)).when(spyWorker).getKubernetesClusterVMMaps();

        List<KubernetesClusterVmMapVO> toRemove = spyWorker.getWorkerNodesToRemove();

        // The stopped node goes first, then the last running node as before
        Assert.assertEquals(2, toRemove.size());
        Assert.assertTrue(toRemove.contains(vm2));
        Assert.assertTrue(toRemove.contains(vm4));
    }

    @Test
    public void testGetDownNodeCountIgnoresEtcdNodes() {
        KubernetesClusterScaleWorker worker = new KubernetesClusterScaleWorker(kubernetesCluster, new java.util.HashMap<>(), 2L, null, false, null, null, clusterManager);
        worker.vmInstanceDao = vmInstanceDao;
        KubernetesClusterScaleWorker spyWorker = Mockito.spy(worker);

        KubernetesClusterVmMapVO running = mockWorkerVmMap(1L, VirtualMachine.State.Running);
        KubernetesClusterVmMapVO stopped = mockWorkerVmMap(2L, VirtualMachine.State.Stopped);
        KubernetesClusterVmMapVO etcd = mockWorkerVmMap(3L, VirtualMachine.State.Stopped);
        Mockito.when(etcd.isEtcdNode()).thenReturn(true);
        Mockito.doReturn(Arrays.asList(running, stopped, etcd)).when(spyWorker).getKubernetesClusterVMMaps();

        Assert.assertEquals(1L, spyWorker.getDownNodeCount(Collections.emptySet()));
        // Newly provisioned VMs aren't left out of the ready nodes
        Assert.assertEquals(0L, spyWorker.getDownNodeCount(Set.of(2L)));
    }

    @Test
    public void testIsVmDown() {
        Assert.assertTrue(KubernetesClusterScaleWorker.isVmDown(VirtualMachine.State.Stopped));
        Assert.assertTrue(KubernetesClusterScaleWorker.isVmDown(VirtualMachine.State.Shutdown));
        Assert.assertTrue(KubernetesClusterScaleWorker.isVmDown(VirtualMachine.State.Error));
        Assert.assertTrue(KubernetesClusterScaleWorker.isVmDown(VirtualMachine.State.Destroyed));
        Assert.assertTrue(KubernetesClusterScaleWorker.isVmDown(VirtualMachine.State.Expunging));
        // The VM may still be running in these
        Assert.assertFalse(KubernetesClusterScaleWorker.isVmDown(VirtualMachine.State.Running));
        Assert.assertFalse(KubernetesClusterScaleWorker.isVmDown(VirtualMachine.State.Migrating));
        Assert.assertFalse(KubernetesClusterScaleWorker.isVmDown(VirtualMachine.State.Unknown));
        Assert.assertFalse(KubernetesClusterScaleWorker.isVmDown(VirtualMachine.State.Starting));
    }
}
