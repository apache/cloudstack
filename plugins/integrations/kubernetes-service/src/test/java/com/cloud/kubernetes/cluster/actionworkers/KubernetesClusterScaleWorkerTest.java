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

import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventVO;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterEventTypes;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmService;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
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
    private UserVmService userVmService;
    @Mock
    private UserVmManager userVmManager;
    @Mock
    private KubernetesClusterDao kubernetesClusterDao;
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

    private UserVmVO mockNodeVm(String hostName) {
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        Mockito.when(vm.getHostName()).thenReturn(hostName);
        return vm;
    }

    @Test
    public void testDrainNodesToRemoveFailureLeavesClusterUnchanged() throws Exception {
        Mockito.when(kubernetesCluster.getId()).thenReturn(10L);
        worker.publicIpAddress = "10.0.0.1";
        worker.sshPort = 2222;
        worker.scaleTimeoutTime = Long.MAX_VALUE;
        KubernetesClusterScaleWorker spy = Mockito.spy(worker);
        UserVmVO node1 = mockNodeVm("node-1");
        UserVmVO node2 = mockNodeVm("node-2");
        Mockito.doReturn(new Pair<>(true, "")).when(spy).executeSshCommand(Mockito.eq("10.0.0.1"), Mockito.eq(2222),
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.doReturn(new Pair<>(true, "drained")).when(spy).drainKubernetesClusterNode(Mockito.eq("10.0.0.1"), Mockito.eq(2222),
                Mockito.eq(node1), Mockito.anyInt(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(new Pair<>(false, "Cannot evict pod")).when(spy).drainKubernetesClusterNode(Mockito.eq("10.0.0.1"), Mockito.eq(2222),
                Mockito.eq(node2), Mockito.anyInt(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(true).when(spy).uncordonKubernetesClusterNode(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        Mockito.doReturn(true).when(spy).stateTransitTo(Mockito.anyLong(), Mockito.any());

        try {
            spy.drainNodesToRemove(Arrays.asList(node1, node2));
            Assert.fail("Expected the scale-down to stop");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("node-2"));
            Assert.assertTrue(e.getMessage().contains("Cannot evict pod"));
        }
        Mockito.verify(spy).uncordonKubernetesClusterNode("10.0.0.1", 2222, node1);
        Mockito.verify(spy).uncordonKubernetesClusterNode("10.0.0.1", 2222, node2);
        Mockito.verify(spy).stateTransitTo(10L, KubernetesCluster.Event.OperationSucceeded);
        Mockito.verify(spy, Mockito.never()).stateTransitTo(10L, KubernetesCluster.Event.OperationFailed);
    }

    @Test
    public void testDrainNodesToRemoveCordonsAllBeforeDraining() throws Exception {
        worker.publicIpAddress = "10.0.0.1";
        worker.sshPort = 2222;
        worker.scaleTimeoutTime = Long.MAX_VALUE;
        KubernetesClusterScaleWorker spy = Mockito.spy(worker);
        UserVmVO node1 = mockNodeVm("node-1");
        UserVmVO node2 = mockNodeVm("node-2");
        Mockito.doReturn(new Pair<>(true, "")).when(spy).executeSshCommand(Mockito.anyString(), Mockito.anyInt(),
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.doReturn(new Pair<>(true, "drained")).when(spy).drainKubernetesClusterNode(Mockito.anyString(), Mockito.anyInt(),
                Mockito.any(), Mockito.anyInt(), Mockito.anyLong(), Mockito.anyLong());

        spy.drainNodesToRemove(Arrays.asList(node1, node2));

        org.mockito.InOrder inOrder = Mockito.inOrder(spy);
        inOrder.verify(spy).executeSshCommand("10.0.0.1", 2222, "sudo /opt/bin/kubectl cordon node-1", 10000, 10000, 30000);
        inOrder.verify(spy).executeSshCommand("10.0.0.1", 2222, "sudo /opt/bin/kubectl cordon node-2", 10000, 10000, 30000);
        inOrder.verify(spy).drainKubernetesClusterNode(Mockito.eq("10.0.0.1"), Mockito.eq(2222), Mockito.eq(node1), Mockito.anyInt(), Mockito.anyLong(), Mockito.anyLong());
        inOrder.verify(spy).drainKubernetesClusterNode(Mockito.eq("10.0.0.1"), Mockito.eq(2222), Mockito.eq(node2), Mockito.anyInt(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.verify(spy, Mockito.never()).uncordonKubernetesClusterNode(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        Mockito.verify(spy, Mockito.never()).stateTransitTo(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void testDrainNodesToRemoveFailureKeepsAlertCluster() throws Exception {
        Mockito.when(kubernetesCluster.getId()).thenReturn(10L);
        Mockito.when(kubernetesCluster.getState()).thenReturn(KubernetesCluster.State.Alert);
        KubernetesClusterScaleWorker alertWorker = new KubernetesClusterScaleWorker(kubernetesCluster, null, 1L, null, null, null, null, clusterManager);
        alertWorker.publicIpAddress = "10.0.0.1";
        alertWorker.sshPort = 2222;
        KubernetesClusterScaleWorker spy = Mockito.spy(alertWorker);
        UserVmVO node1 = mockNodeVm("node-1");
        Mockito.doReturn(new Pair<>(false, "connection refused")).when(spy).executeSshCommand(Mockito.anyString(), Mockito.anyInt(),
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.doReturn(true).when(spy).stateTransitTo(Mockito.anyLong(), Mockito.any());

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, () -> spy.drainNodesToRemove(List.of(node1)));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("unable to cordon Kubernetes node: node-1"));
        Mockito.verify(spy).stateTransitTo(10L, KubernetesCluster.Event.OperationFailed);
        Mockito.verify(spy, Mockito.never()).uncordonKubernetesClusterNode(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    public void testDrainNodesToRemoveStopsAtDeadline() throws Exception {
        Mockito.when(kubernetesCluster.getId()).thenReturn(10L);
        worker.publicIpAddress = "10.0.0.1";
        worker.sshPort = 2222;
        worker.scaleTimeoutTime = System.currentTimeMillis() - 1;
        KubernetesClusterScaleWorker spy = Mockito.spy(worker);
        UserVmVO node1 = mockNodeVm("node-1");
        Mockito.doReturn(new Pair<>(true, "")).when(spy).executeSshCommand(Mockito.anyString(), Mockito.anyInt(),
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.doReturn(true).when(spy).uncordonKubernetesClusterNode(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        Mockito.doReturn(true).when(spy).stateTransitTo(Mockito.anyLong(), Mockito.any());

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, () -> spy.drainNodesToRemove(List.of(node1)));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("timed out"));
        Mockito.verify(spy, Mockito.never()).drainKubernetesClusterNode(Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyInt(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.verify(spy).uncordonKubernetesClusterNode("10.0.0.1", 2222, node1);
        Mockito.verify(spy).stateTransitTo(10L, KubernetesCluster.Event.OperationSucceeded);
    }

    @Test
    public void testDrainNodesToRemoveKeepsNodeCordonedBeforehand() throws Exception {
        Mockito.when(kubernetesCluster.getId()).thenReturn(10L);
        worker.publicIpAddress = "10.0.0.1";
        worker.sshPort = 2222;
        worker.scaleTimeoutTime = Long.MAX_VALUE;
        KubernetesClusterScaleWorker spy = Mockito.spy(worker);
        UserVmVO node1 = mockNodeVm("node-1");
        UserVmVO node2 = mockNodeVm("node-2");
        Mockito.doReturn(new Pair<>(true, "")).when(spy).executeSshCommand(Mockito.anyString(), Mockito.anyInt(),
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.doReturn(new Pair<>(true, "sudo: unable to resolve host node-1\ntrue")).when(spy).executeSshCommand(Mockito.anyString(), Mockito.anyInt(),
                Mockito.eq("sudo /opt/bin/kubectl get node node-1 -o jsonpath='{.spec.unschedulable}'"), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.doReturn(new Pair<>(false, "Cannot evict pod")).when(spy).drainKubernetesClusterNode(Mockito.anyString(), Mockito.anyInt(),
                Mockito.any(), Mockito.anyInt(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(true).when(spy).uncordonKubernetesClusterNode(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        Mockito.doReturn(true).when(spy).stateTransitTo(Mockito.anyLong(), Mockito.any());

        Assert.assertThrows(CloudRuntimeException.class, () -> spy.drainNodesToRemove(Arrays.asList(node1, node2)));
        Mockito.verify(spy, Mockito.never()).uncordonKubernetesClusterNode("10.0.0.1", 2222, node1);
        Mockito.verify(spy).uncordonKubernetesClusterNode("10.0.0.1", 2222, node2);
    }

    @Test
    public void testDrainNodesToRemoveTakesUnknownCordonAsCordoned() throws Exception {
        Mockito.when(kubernetesCluster.getId()).thenReturn(10L);
        worker.publicIpAddress = "10.0.0.1";
        worker.sshPort = 2222;
        worker.scaleTimeoutTime = Long.MAX_VALUE;
        KubernetesClusterScaleWorker spy = Mockito.spy(worker);
        UserVmVO node1 = mockNodeVm("node-1");
        Mockito.doReturn(new Pair<>(true, "")).when(spy).executeSshCommand(Mockito.anyString(), Mockito.anyInt(),
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.doThrow(new RuntimeException("timed out")).when(spy).executeSshCommand(Mockito.anyString(), Mockito.anyInt(),
                Mockito.startsWith("sudo /opt/bin/kubectl get node"), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.doReturn(new Pair<>(false, "Cannot evict pod")).when(spy).drainKubernetesClusterNode(Mockito.anyString(), Mockito.anyInt(),
                Mockito.any(), Mockito.anyInt(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(true).when(spy).stateTransitTo(Mockito.anyLong(), Mockito.any());

        Assert.assertThrows(CloudRuntimeException.class, () -> spy.drainNodesToRemove(List.of(node1)));
        Mockito.verify(spy).executeSshCommand("10.0.0.1", 2222, "sudo /opt/bin/kubectl cordon node-1", 10000, 10000, 30000);
        Mockito.verify(spy, Mockito.never()).uncordonKubernetesClusterNode(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
    }

    private MockedStatic<CallContext> callContextMock;
    private MockedStatic<ActionEventUtils> actionEventUtilsMock;

    @After
    public void tearDown() {
        if (callContextMock != null) {
            callContextMock.close();
        }
        if (actionEventUtilsMock != null) {
            actionEventUtilsMock.close();
        }
    }

    private static final long CLUSTER_ID = 10L;
    private static final long WORKER_OFFERING_ID = 5L;

    /**
     * Sets up a worker for {@link KubernetesClusterScaleWorker#removeNodesFromCluster(List)} on a cluster of 3 workers
     * of 2 cores and 2048 MB each, with control and drain steps and the Kubernetes node deletion stubbed out.
     */
    private KubernetesClusterScaleWorker setUpNodeRemoval() throws Exception {
        callContextMock = Mockito.mockStatic(CallContext.class);
        CallContext callContext = Mockito.mock(CallContext.class);
        callContextMock.when(CallContext::current).thenReturn(callContext);
        callContextMock.when(() -> CallContext.register(Mockito.any(CallContext.class), Mockito.any())).thenReturn(callContext);
        actionEventUtilsMock = Mockito.mockStatic(ActionEventUtils.class);

        Mockito.when(kubernetesCluster.getId()).thenReturn(CLUSTER_ID);
        worker.userVmService = userVmService;
        worker.userVmManager = userVmManager;
        worker.kubernetesClusterDao = kubernetesClusterDao;
        worker.vmInstanceDao = vmInstanceDao;
        worker.publicIpAddress = "10.0.0.1";
        worker.sshPort = 2222;
        worker.scaleTimeoutTime = Long.MAX_VALUE;

        KubernetesClusterVO clusterVO = Mockito.mock(KubernetesClusterVO.class);
        Mockito.lenient().when(clusterVO.getCores()).thenReturn(10L);
        Mockito.lenient().when(clusterVO.getMemory()).thenReturn(10240L);
        Mockito.lenient().when(clusterVO.getNodeCount()).thenReturn(3L);
        Mockito.lenient().when(kubernetesClusterDao.findById(CLUSTER_ID)).thenReturn(clusterVO);
        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.lenient().when(offering.getCpu()).thenReturn(2);
        Mockito.lenient().when(offering.getRamSize()).thenReturn(2048);
        Mockito.lenient().when(serviceOfferingDao.findByIdIncludingRemoved(Mockito.anyLong(), Mockito.eq(WORKER_OFFERING_ID))).thenReturn(offering);

        KubernetesClusterScaleWorker spy = Mockito.spy(worker);
        Mockito.doAnswer(invocation -> new java.util.ArrayList<>((List<?>) invocation.getArgument(0)))
                .when(spy).drainNodesToRemove(Mockito.anyList());
        // kubectl delete node
        Mockito.doReturn(new Pair<>(true, "")).when(spy).executeSshCommand(Mockito.anyString(), Mockito.anyInt(),
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.lenient().doReturn(true).when(spy).uncordonKubernetesClusterNode(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        Mockito.lenient().doReturn(true).when(spy).stateTransitTo(Mockito.anyLong(), Mockito.any());
        Mockito.lenient().doReturn(KubernetesClusterVO.class.cast(null)).when(spy).updateKubernetesClusterEntry(Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        // No network rules to update
        Mockito.lenient().when(clusterManager.isDirectAccess(Mockito.any())).thenReturn(true);
        return spy;
    }

    private KubernetesClusterVmMapVO mockNodeVmMap(long mapId, long vmId, String hostName) {
        KubernetesClusterVmMapVO vmMap = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.lenient().when(vmMap.getId()).thenReturn(mapId);
        Mockito.lenient().when(vmMap.getVmId()).thenReturn(vmId);
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        Mockito.lenient().when(vm.getHostName()).thenReturn(hostName);
        Mockito.lenient().when(vm.getId()).thenReturn(vmId);
        Mockito.lenient().when(vm.getServiceOfferingId()).thenReturn(WORKER_OFFERING_ID);
        Mockito.when(userVmDao.findById(vmId)).thenReturn(vm);
        VMInstanceVO vmInstance = Mockito.mock(VMInstanceVO.class);
        Mockito.lenient().when(vmInstance.getState()).thenReturn(VirtualMachine.State.Destroyed);
        Mockito.lenient().when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vmInstance);
        return vmMap;
    }

    @Test
    public void testRemoveNodesFromClusterRemovesMapEntriesAndExpungesVms() throws Exception {
        KubernetesClusterScaleWorker spy = setUpNodeRemoval();
        KubernetesClusterVmMapVO map1 = mockNodeVmMap(101L, 201L, "node-1");
        KubernetesClusterVmMapVO map2 = mockNodeVmMap(102L, 202L, "node-2");
        UserVmVO vm1 = userVmDao.findById(201L);
        UserVmVO vm2 = userVmDao.findById(202L);
        Mockito.when(userVmManager.expunge(Mockito.any())).thenReturn(true);

        spy.removeNodesFromCluster(Arrays.asList(map1, map2));

        org.mockito.InOrder inOrder = Mockito.inOrder(kubernetesClusterVmMapDao, userVmService, userVmManager);
        // The VM's expunge still finds the map entry, which goes once the VM is done with
        inOrder.verify(userVmService).destroyVm(201L, true);
        inOrder.verify(userVmManager).expunge(vm1);
        inOrder.verify(kubernetesClusterVmMapDao).expunge(101L);
        inOrder.verify(userVmService).destroyVm(202L, true);
        inOrder.verify(userVmManager).expunge(vm2);
        inOrder.verify(kubernetesClusterVmMapDao).expunge(102L);
        Mockito.verify(spy, Mockito.never()).stateTransitTo(Mockito.anyLong(), Mockito.any());
        Mockito.verify(spy, Mockito.never()).revokeRemovedNodeVmSshRules(Mockito.any());
        // The caller updates the cluster entry once every node is removed
        Mockito.verify(spy, Mockito.never()).updateKubernetesClusterEntry(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        actionEventUtilsMock.verifyNoInteractions();
    }

    @Test
    public void testRemoveNodesFromClusterExpungeFailureRemovesMapEntryAndGoesOn() throws Exception {
        KubernetesClusterScaleWorker spy = setUpNodeRemoval();
        KubernetesClusterVmMapVO map1 = mockNodeVmMap(101L, 201L, "node-1");
        KubernetesClusterVmMapVO map2 = mockNodeVmMap(102L, 202L, "node-2");
        UserVmVO vm1 = userVmDao.findById(201L);
        UserVmVO vm2 = userVmDao.findById(202L);
        Mockito.when(userVmManager.expunge(vm1)).thenReturn(false);
        Mockito.when(userVmManager.expunge(vm2)).thenThrow(new CloudRuntimeException("storage unavailable"));

        spy.removeNodesFromCluster(Arrays.asList(map1, map2));

        Mockito.verify(kubernetesClusterVmMapDao).expunge(101L);
        Mockito.verify(kubernetesClusterVmMapDao).expunge(102L);
        Mockito.verify(userVmService).destroyVm(201L, true);
        Mockito.verify(userVmService).destroyVm(202L, true);
        Mockito.verify(spy, Mockito.never()).stateTransitTo(Mockito.anyLong(), Mockito.any());
        Mockito.verify(spy, Mockito.never()).uncordonKubernetesClusterNode(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        Mockito.verify(spy).revokeRemovedNodeVmSshRules(vm1);
        Mockito.verify(spy).revokeRemovedNodeVmSshRules(vm2);
        actionEventUtilsMock.verify(() -> ActionEventUtils.onCompletedActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                Mockito.eq(EventVO.LEVEL_WARN), Mockito.eq(KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_SCALE),
                Mockito.contains("is left in state Destroyed"), Mockito.eq(201L), Mockito.anyString(), Mockito.eq(0L)));
        actionEventUtilsMock.verify(() -> ActionEventUtils.onCompletedActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                Mockito.eq(EventVO.LEVEL_WARN), Mockito.eq(KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_SCALE),
                Mockito.contains("Unable to expunge VM"), Mockito.eq(202L), Mockito.anyString(), Mockito.eq(0L)));
    }

    @Test
    public void testRemoveNodesFromClusterDestroyFailureRemovesMapEntryAndFails() throws Exception {
        KubernetesClusterScaleWorker spy = setUpNodeRemoval();
        KubernetesClusterVmMapVO map1 = mockNodeVmMap(101L, 201L, "node-1");
        KubernetesClusterVmMapVO map2 = mockNodeVmMap(102L, 202L, "node-2");
        KubernetesClusterVmMapVO map3 = mockNodeVmMap(103L, 203L, "node-3");
        UserVmVO vm1 = userVmDao.findById(201L);
        UserVmVO vm2 = userVmDao.findById(202L);
        UserVmVO vm3 = userVmDao.findById(203L);
        Mockito.when(userVmManager.expunge(Mockito.any())).thenReturn(true);
        Mockito.when(userVmService.destroyVm(202L, true)).thenThrow(new ResourceUnavailableException("host down", com.cloud.host.Host.class, 1L));
        Mockito.when(vmInstanceDao.findByIdIncludingRemoved(202L).getState()).thenReturn(VirtualMachine.State.Running);

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class,
                () -> spy.removeNodesFromCluster(Arrays.asList(map1, map2, map3)));

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("unable to destroy VM"));
        // node-2 has left the Kubernetes cluster, so its VM has left the map although it is not destroyed
        Mockito.verify(kubernetesClusterVmMapDao).expunge(101L);
        Mockito.verify(kubernetesClusterVmMapDao).expunge(102L);
        Mockito.verify(kubernetesClusterVmMapDao, Mockito.never()).expunge(103L);
        Mockito.verify(userVmManager, Mockito.never()).expunge(vm2);
        Mockito.verify(spy).revokeRemovedNodeVmSshRules(vm2);
        Mockito.verify(userVmService, Mockito.never()).destroyVm(203L, true);
        Mockito.verify(spy).stateTransitTo(CLUSTER_ID, KubernetesCluster.Event.OperationFailed);
        // node-3 is still in the cluster, drained, so it is uncordoned
        Mockito.verify(spy).uncordonKubernetesClusterNode("10.0.0.1", 2222, vm3);
        Mockito.verify(spy, Mockito.never()).uncordonKubernetesClusterNode("10.0.0.1", 2222, vm1);
        Mockito.verify(spy, Mockito.never()).uncordonKubernetesClusterNode("10.0.0.1", 2222, vm2);
        // The cluster entry matches the two map entries removed
        Mockito.verify(spy).updateKubernetesClusterEntry(10L - 4L, 10240L - 4096L, 1L, null, false, 0L, 0L, WORKER, false, false);
    }

    @Test
    public void testRemoveNodesFromClusterKubernetesNodeFailureKeepsMapEntry() throws Exception {
        KubernetesClusterScaleWorker spy = setUpNodeRemoval();
        KubernetesClusterVmMapVO map1 = mockNodeVmMap(101L, 201L, "node-1");
        KubernetesClusterVmMapVO map2 = mockNodeVmMap(102L, 202L, "node-2");
        UserVmVO vm2 = userVmDao.findById(202L);
        Mockito.when(userVmManager.expunge(Mockito.any())).thenReturn(true);
        Mockito.doReturn(true).when(spy).sleepBeforeRetry(Mockito.anyLong());
        Mockito.doReturn(new Pair<>(false, "connection refused")).when(spy).executeSshCommand(Mockito.anyString(), Mockito.anyInt(),
                Mockito.eq("sudo /opt/bin/kubectl delete node node-2 --ignore-not-found"), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());

        Assert.assertThrows(CloudRuntimeException.class, () -> spy.removeNodesFromCluster(Arrays.asList(map1, map2)));

        Mockito.verify(kubernetesClusterVmMapDao).expunge(101L);
        Mockito.verify(kubernetesClusterVmMapDao, Mockito.never()).expunge(102L);
        Mockito.verify(userVmService, Mockito.never()).destroyVm(202L, true);
        Mockito.verify(spy).uncordonKubernetesClusterNode("10.0.0.1", 2222, vm2);
        Mockito.verify(spy).updateKubernetesClusterEntry(10L - 2L, 10240L - 2048L, 2L, null, false, 0L, 0L, WORKER, false, false);
    }

    @Test
    public void testRemoveNodesFromClusterLaterFailureMatchesClusterToMap() throws Exception {
        KubernetesClusterScaleWorker spy = setUpNodeRemoval();
        KubernetesClusterVmMapVO controlMap = mockNodeVmMap(101L, 201L, "control-2");
        KubernetesClusterVmMapVO workerMap = mockNodeVmMap(102L, 202L, "node-2");
        Mockito.when(userVmManager.expunge(Mockito.any())).thenReturn(true);
        // Not a CloudRuntimeException, after every node has been removed
        Mockito.when(clusterManager.isDirectAccess(Mockito.any())).thenThrow(new IllegalStateException("network gone"));

        Assert.assertThrows(IllegalStateException.class, () -> spy.removeNodesFromCluster(Arrays.asList(controlMap, workerMap)));

        Mockito.verify(kubernetesClusterVmMapDao).expunge(101L);
        Mockito.verify(kubernetesClusterVmMapDao).expunge(102L);
        Mockito.verify(spy, Mockito.never()).uncordonKubernetesClusterNode(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        // As a full scale-down does, every VM removed comes off the node count, so the total matches the map
        Mockito.verify(spy).updateKubernetesClusterEntry(10L - 4L, 10240L - 4096L, 1L, null, false, 0L, 0L, WORKER, false, false);
    }

    @Test
    public void testRemoveNodesFromClusterDestroyErrorAfterVmDestroyedGoesOn() throws Exception {
        KubernetesClusterScaleWorker spy = setUpNodeRemoval();
        KubernetesClusterVmMapVO map1 = mockNodeVmMap(101L, 201L, "node-1");
        UserVmVO vm1 = userVmDao.findById(201L);
        Mockito.when(userVmService.destroyVm(201L, true)).thenThrow(new CloudRuntimeException("Failed to destroy vm with specified vmId"));
        Mockito.when(userVmManager.expunge(vm1)).thenReturn(true);

        spy.removeNodesFromCluster(List.of(map1));

        Mockito.verify(kubernetesClusterVmMapDao).expunge(101L);
        Mockito.verify(userVmManager).expunge(vm1);
        Mockito.verify(spy, Mockito.never()).stateTransitTo(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void testRemoveNodesFromClusterDestroyErrorAfterVmExpungedSkipsExpunge() throws Exception {
        KubernetesClusterScaleWorker spy = setUpNodeRemoval();
        KubernetesClusterVmMapVO map1 = mockNodeVmMap(101L, 201L, "node-1");
        UserVmVO vm1 = userVmDao.findById(201L);
        Mockito.when(vmInstanceDao.findByIdIncludingRemoved(201L).getRemoved()).thenReturn(new java.util.Date());
        Mockito.when(userVmService.destroyVm(201L, true)).thenThrow(new CloudRuntimeException("Failed to destroy vm with specified vmId"));

        spy.removeNodesFromCluster(List.of(map1));

        Mockito.verify(kubernetesClusterVmMapDao).expunge(101L);
        Mockito.verify(userVmManager, Mockito.never()).expunge(vm1);
        Mockito.verify(spy, Mockito.never()).stateTransitTo(Mockito.anyLong(), Mockito.any());
        actionEventUtilsMock.verifyNoInteractions();
    }

    @Test
    public void testRemoveNodesFromClusterExpungedMeanwhileRecordsNoWarning() throws Exception {
        KubernetesClusterScaleWorker spy = setUpNodeRemoval();
        KubernetesClusterVmMapVO map1 = mockNodeVmMap(101L, 201L, "node-1");
        UserVmVO vm1 = userVmDao.findById(201L);
        Mockito.when(userVmManager.expunge(vm1)).thenReturn(false);
        Mockito.when(vmInstanceDao.findByIdIncludingRemoved(201L).getRemoved()).thenReturn(new java.util.Date());

        spy.removeNodesFromCluster(List.of(map1));

        Mockito.verify(kubernetesClusterVmMapDao).expunge(101L);
        Mockito.verify(spy, Mockito.never()).revokeRemovedNodeVmSshRules(Mockito.any());
        actionEventUtilsMock.verifyNoInteractions();
    }

    private com.cloud.network.rules.PortForwardingRuleVO mockPfRule(long id, long ipId, long networkId, int sourcePort, int destinationPort) {
        com.cloud.network.rules.PortForwardingRuleVO rule = Mockito.mock(com.cloud.network.rules.PortForwardingRuleVO.class);
        Mockito.lenient().when(rule.getSourceIpAddressId()).thenReturn(ipId);
        Mockito.lenient().when(rule.getId()).thenReturn(id);
        Mockito.lenient().when(rule.getNetworkId()).thenReturn(networkId);
        Mockito.lenient().when(rule.getSourcePortStart()).thenReturn(sourcePort);
        Mockito.lenient().when(rule.getDestinationPortStart()).thenReturn(destinationPort);
        return rule;
    }

    @Test
    public void testRevokeRemovedNodeVmSshRulesRevokesOnlyTheClusterSshRule() throws Exception {
        callContextMock = Mockito.mockStatic(CallContext.class);
        CallContext callContext = Mockito.mock(CallContext.class);
        callContextMock.when(CallContext::current).thenReturn(callContext);
        worker.publicIpAddress = "10.0.0.1";
        worker.sshPort = 2222;
        com.cloud.network.dao.IPAddressDao ipAddressDao = Mockito.mock(com.cloud.network.dao.IPAddressDao.class);
        worker.ipAddressDao = ipAddressDao;
        com.cloud.network.dao.IPAddressVO clusterIp = Mockito.mock(com.cloud.network.dao.IPAddressVO.class);
        Mockito.when(clusterIp.getAddress()).thenReturn(new com.cloud.utils.net.Ip("10.0.0.1"));
        com.cloud.network.dao.IPAddressVO otherIp = Mockito.mock(com.cloud.network.dao.IPAddressVO.class);
        Mockito.when(otherIp.getAddress()).thenReturn(new com.cloud.utils.net.Ip("10.0.0.2"));
        Mockito.when(ipAddressDao.findById(7L)).thenReturn(clusterIp);
        Mockito.when(ipAddressDao.findById(8L)).thenReturn(otherIp);
        com.cloud.network.dao.NetworkVO network = Mockito.mock(com.cloud.network.dao.NetworkVO.class);
        Mockito.when(network.getId()).thenReturn(5L);
        worker.networkModel = Mockito.mock(com.cloud.network.NetworkModel.class);
        java.lang.reflect.Field networkField = KubernetesClusterScaleWorker.class.getDeclaredField("network");
        networkField.setAccessible(true);
        networkField.set(worker, network);
        com.cloud.network.rules.dao.PortForwardingRulesDao pfRulesDao = Mockito.mock(com.cloud.network.rules.dao.PortForwardingRulesDao.class);
        com.cloud.network.rules.RulesService rulesService = Mockito.mock(com.cloud.network.rules.RulesService.class);
        worker.portForwardingRulesDao = pfRulesDao;
        worker.rulesService = rulesService;
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        Mockito.when(vm.getId()).thenReturn(201L);
        List<com.cloud.network.rules.PortForwardingRuleVO> rules = List.of(
                mockPfRule(1L, 7L, 5L, 8080, 80),
                mockPfRule(2L, 7L, 6L, 2223, 22),
                mockPfRule(3L, 8L, 5L, 3022, 22),
                mockPfRule(4L, 7L, 5L, 2223, 22),
                // The port the scale reaches the cluster through
                mockPfRule(5L, 7L, 5L, 2222, 22));
        Mockito.when(pfRulesDao.listByVm(201L)).thenReturn(rules);

        worker.revokeRemovedNodeVmSshRules(vm);

        Mockito.verify(rulesService).revokePortForwardingRule(4L, true);
        Mockito.verifyNoMoreInteractions(rulesService);
    }

    @Test
    public void testRevokeRemovedNodeVmSshRulesLeavesNsxRulesAlone() throws Exception {
        com.cloud.network.dao.NetworkVO network = Mockito.mock(com.cloud.network.dao.NetworkVO.class);
        Mockito.when(network.getId()).thenReturn(5L);
        java.lang.reflect.Field networkField = KubernetesClusterScaleWorker.class.getDeclaredField("network");
        networkField.setAccessible(true);
        networkField.set(worker, network);
        worker.publicIpAddress = "10.0.0.1";
        worker.networkModel = Mockito.mock(com.cloud.network.NetworkModel.class);
        Mockito.when(worker.networkModel.isProviderSupportServiceInNetwork(5L, com.cloud.network.Network.Service.PortForwarding,
                com.cloud.network.Network.Provider.Nsx)).thenReturn(true);
        com.cloud.network.rules.dao.PortForwardingRulesDao pfRulesDao = Mockito.mock(com.cloud.network.rules.dao.PortForwardingRulesDao.class);
        worker.portForwardingRulesDao = pfRulesDao;

        worker.revokeRemovedNodeVmSshRules(Mockito.mock(UserVmVO.class));

        Mockito.verifyNoInteractions(pfRulesDao);
    }
}
