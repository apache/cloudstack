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
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.DEFAULT;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.CONTROL;
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

    private KubernetesCluster createExistingKubernetesClusterForTesting(Long defaultOfferingId, Long workerOfferingId,
                                                                        Long controlOfferingId) {
        KubernetesCluster kubernetesClusterMock = Mockito.mock(KubernetesCluster.class);
        Mockito.when(kubernetesClusterMock.getServiceOfferingId()).thenReturn(defaultOfferingId);
        Mockito.when(kubernetesClusterMock.getWorkerNodeServiceOfferingId()).thenReturn(workerOfferingId);
        Mockito.when(kubernetesClusterMock.getControlNodeServiceOfferingId()).thenReturn(controlOfferingId);
        return kubernetesClusterMock;
    }

    private ServiceOffering createServiceOfferingForTesting(Long offeringId) {
        ServiceOffering cksOffering = Mockito.mock(ServiceOffering.class);
        Mockito.when(cksOffering.getId()).thenReturn(offeringId);
        return cksOffering;
    }

    @Test
    public void testCheckScalingKubernetesClusterOfferingsPerNodeTypeToNewDefaultOffering() {
        Long cksOfferingId = 20L;

        ServiceOffering newCksOffering = createServiceOfferingForTesting(21L);

        KubernetesCluster kubernetesClusterMock = createExistingKubernetesClusterForTesting(cksOfferingId,
                null, null);

        Map<String, ServiceOffering> scalingMap = new HashMap<>();
        scalingMap.put(DEFAULT.name(), newCksOffering);

        worker.checkScalingKubernetesClusterOfferingsPerNodeType(scalingMap, kubernetesClusterMock);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCheckScalingKubernetesClusterOfferingsPerNodeTypeToSameDefaultOffering() {
        Long cksOfferingId = 20L;
        ServiceOffering cksOffering = createServiceOfferingForTesting(cksOfferingId);

        KubernetesCluster kubernetesClusterMock = createExistingKubernetesClusterForTesting(cksOfferingId,
                null, null);

        Map<String, ServiceOffering> scalingMap = new HashMap<>();
        scalingMap.put(DEFAULT.name(), cksOffering);

        worker.checkScalingKubernetesClusterOfferingsPerNodeType(scalingMap, kubernetesClusterMock);
    }

    @Test
    public void testCheckScalingKubernetesClusterOfferingsPerNodeTypeWorkerAndControlNodes() {
        Long cksOfferingId = 20L;
        Long workerOfferingId = 21L;
        Long controlOfferingId = 22L;
        ServiceOffering scalingWorkerOffering = createServiceOfferingForTesting(30L);
        ServiceOffering scalingControlOffering = createServiceOfferingForTesting(31L);

        KubernetesCluster kubernetesClusterMock = createExistingKubernetesClusterForTesting(cksOfferingId,
                workerOfferingId, controlOfferingId);

        Map<String, ServiceOffering> scalingMap = new HashMap<>();
        scalingMap.put(WORKER.name(), scalingWorkerOffering);
        scalingMap.put(CONTROL.name(), scalingControlOffering);

        worker.checkScalingKubernetesClusterOfferingsPerNodeType(scalingMap, kubernetesClusterMock);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCheckScalingKubernetesClusterOfferingsPerNodeTypeWorkerAndControlNodesUseSameOffering() {
        Long cksOfferingId = 20L;
        Long workerOfferingId = 21L;
        Long controlOfferingId = 22L;
        ServiceOffering scalingWorkerOffering = createServiceOfferingForTesting(30L);
        ServiceOffering controlOffering = createServiceOfferingForTesting(controlOfferingId);

        KubernetesCluster kubernetesClusterMock = createExistingKubernetesClusterForTesting(cksOfferingId,
                workerOfferingId, controlOfferingId);

        Map<String, ServiceOffering> scalingMap = new HashMap<>();
        scalingMap.put(WORKER.name(), scalingWorkerOffering);
        scalingMap.put(CONTROL.name(), controlOffering);

        worker.checkScalingKubernetesClusterOfferingsPerNodeType(scalingMap, kubernetesClusterMock);
    }

    @Test
    public void testCheckScalingKubernetesClusterOfferingsPerNodeTypeWorkerAndControlNodesByDefaultOffering() {
        Long cksOfferingId = 20L;
        Long workerOfferingId = 21L;
        Long controlOfferingId = 22L;
        ServiceOffering scalingOffering = createServiceOfferingForTesting(30L);

        KubernetesCluster kubernetesClusterMock = createExistingKubernetesClusterForTesting(cksOfferingId,
                workerOfferingId, controlOfferingId);

        Map<String, ServiceOffering> scalingMap = new HashMap<>();
        scalingMap.put(DEFAULT.name(), scalingOffering);

        worker.checkScalingKubernetesClusterOfferingsPerNodeType(scalingMap, kubernetesClusterMock);
    }

    @Test
    public void testCheckScalingKubernetesClusterOfferingsPerNodeTypeWorkerEtcdAndControlNodes() {
        Long cksOfferingId = 20L;
        Long workerOfferingId = 21L;
        Long controlOfferingId = 22L;
        Long etcdOfferingId = 23L;
        ServiceOffering scalingOffering = createServiceOfferingForTesting(30L);

        KubernetesCluster kubernetesClusterMock = createExistingKubernetesClusterForTesting(cksOfferingId,
                workerOfferingId, controlOfferingId);
        Mockito.when(kubernetesClusterMock.getEtcdNodeServiceOfferingId()).thenReturn(etcdOfferingId);

        Map<String, ServiceOffering> scalingMap = new HashMap<>();
        scalingMap.put(WORKER.name(), scalingOffering);
        scalingMap.put(CONTROL.name(), scalingOffering);
        scalingMap.put(ETCD.name(), scalingOffering);

        worker.checkScalingKubernetesClusterOfferingsPerNodeType(scalingMap, kubernetesClusterMock);
    }
}
