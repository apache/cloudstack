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

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.CONTROL;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.ETCD;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.WORKER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterDetailsVO;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterUtil;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmService;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;

/**
 * Runs the rollout against a simulated cluster: the VMs, their offerings, and the Kubernetes nodes kubectl reports,
 * with the calls made to them recorded in order.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class KubernetesClusterRollingUpdateWorkerTest {

    private static final long CLUSTER_ID = 1L;
    private static final long OLD_OFFERING = 10L;
    private static final long NEW_OFFERING = 20L;

    @Mock
    KubernetesClusterManagerImpl manager;
    @Mock
    KubernetesClusterDao kubernetesClusterDao;
    @Mock
    KubernetesClusterDetailsDao detailsDao;
    @Mock
    KubernetesClusterVmMapDao vmMapDao;
    @Mock
    UserVmDao userVmDao;
    @Mock
    UserVmService userVmService;
    @Mock
    UserVmManager userVmManager;
    @Mock
    VolumeDao volumeDao;
    @Mock
    AffinityGroupVMMapDao affinityGroupVMMapDao;
    @Mock
    KubernetesCluster cluster;
    @Mock
    KubernetesClusterVO clusterVO;

    /** A Kubernetes node as the simulated kubectl reports it */
    static class Kube {
        boolean ready = true;
        String bootId = UUID.randomUUID().toString();
        String annotation;
        boolean unschedulable;
    }

    final List<KubernetesClusterVmMapVO> maps = new ArrayList<>();
    final Map<Long, UserVmVO> vms = new HashMap<>();
    final Map<Long, VirtualMachine.State> vmStates = new HashMap<>();
    final Map<Long, Long> vmOfferings = new HashMap<>();
    final Map<String, Kube> kube = new LinkedHashMap<>();
    final Map<KubernetesClusterNodeType, Long> desiredOfferings = new HashMap<>();
    final List<String> calls = new ArrayList<>();
    final Set<String> staleAfterStart = new HashSet<>();
    final Set<String> failingDrains = new HashSet<>();
    final Set<String> failingStarts = new HashSet<>();
    final Set<String> details = new HashSet<>();
    KubernetesCluster.State clusterState;
    long nextVmId = 100;

    @Before
    public void setUp() throws Exception {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        manager.kubernetesClusterDao = kubernetesClusterDao;
        manager.kubernetesClusterDetailsDao = detailsDao;
        manager.kubernetesClusterVmMapDao = vmMapDao;
        clusterState = KubernetesCluster.State.Updating;
        Mockito.when(cluster.getId()).thenReturn(CLUSTER_ID);
        Mockito.when(cluster.getName()).thenReturn("cluster");
        Mockito.when(clusterVO.getId()).thenReturn(CLUSTER_ID);
        Mockito.when(clusterVO.getName()).thenReturn("cluster");
        Mockito.when(kubernetesClusterDao.findById(CLUSTER_ID)).thenReturn(clusterVO);
        Mockito.when(clusterVO.getState()).thenAnswer(i -> clusterState);
        Mockito.when(vmMapDao.listByClusterId(CLUSTER_ID)).thenReturn(maps);
        Mockito.when(userVmDao.findById(Mockito.anyLong())).thenAnswer(i -> vms.get((Long) i.getArgument(0)));
        desiredOfferings.put(CONTROL, OLD_OFFERING);
        desiredOfferings.put(WORKER, NEW_OFFERING);
        desiredOfferings.put(ETCD, OLD_OFFERING);
        Mockito.when(manager.getNodeTypeServiceOfferingId(Mockito.any(), Mockito.any())).thenAnswer(i -> desiredOfferings.get((KubernetesClusterNodeType) i.getArgument(1)));
        Mockito.when(manager.getNodeOutOfDateReason(Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer(i -> {
            KubernetesClusterVmMapVO vmMap = i.getArgument(1);
            UserVmVO vm = i.getArgument(2);
            return desiredOfferings.get(KubernetesClusterUtil.getNodeType(vmMap)).equals(vmOfferings.get(vm.getId())) ? null : "its service offering differs";
        });
        Mockito.when(manager.getNodeTypeWantedAffinityGroupIds(Mockito.anyLong(), Mockito.any())).thenReturn(null);
        Mockito.when(manager.getVmAffinityGroupIds(Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(Set.of());
        Mockito.doAnswer(i -> details.add(i.getArgument(1)) ? null : null).when(detailsDao).addDetail(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
        Mockito.doAnswer(i -> {
            details.remove((String) i.getArgument(1));
            return null;
        }).when(detailsDao).removeDetail(Mockito.anyLong(), Mockito.anyString());
        Mockito.when(detailsDao.findDetail(Mockito.anyLong(), Mockito.anyString())).thenAnswer(i -> details.contains((String) i.getArgument(1)) ?
                Mockito.mock(KubernetesClusterDetailsVO.class) : null);
        Mockito.when(volumeDao.findByInstanceAndType(Mockito.anyLong(), Mockito.any())).thenReturn(List.of());
        Mockito.when(userVmService.stopVirtualMachine(Mockito.anyLong(), Mockito.eq(false))).thenAnswer(i -> {
            long id = i.getArgument(0);
            calls.add("stop:" + name(id));
            // Kubernetes keeps the node Ready for a while
            vmStates.put(id, VirtualMachine.State.Stopped);
            return null;
        });
        Mockito.when(userVmManager.upgradeVirtualMachine(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyMap())).thenAnswer(i -> {
            long id = i.getArgument(0);
            calls.add("upgrade:" + name(id));
            vmOfferings.put(id, i.getArgument(1));
            return true;
        });
        Mockito.doAnswer(i -> {
            UserVmVO vm = i.getArgument(0);
            String name = name(vm.getId());
            calls.add("start:" + name);
            if (failingStarts.contains(name)) {
                throw new CloudRuntimeException("insufficient capacity");
            }
            vmStates.put(vm.getId(), VirtualMachine.State.Running);
            if (!staleAfterStart.contains(name) && kube.containsKey(name)) {
                kube.get(name).bootId = UUID.randomUUID().toString();
                kube.get(name).ready = true;
            }
            return null;
        }).when(userVmManager).startVirtualMachine(Mockito.any(UserVmVO.class), Mockito.isNull());
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }

    private String name(long vmId) {
        return vms.get(vmId).getHostName();
    }

    private UserVmVO addNode(String name, KubernetesClusterNodeType type, long offeringId) {
        long id = nextVmId++;
        KubernetesClusterVmMapVO vmMap = new KubernetesClusterVmMapVO(CLUSTER_ID, id, type == CONTROL);
        vmMap.setEtcdNode(type == ETCD);
        maps.add(vmMap);
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        Mockito.when(vm.getId()).thenReturn(id);
        Mockito.when(vm.getHostName()).thenReturn(name);
        Mockito.when(vm.getState()).thenAnswer(i -> vmStates.get(id));
        Mockito.when(vm.getServiceOfferingId()).thenAnswer(i -> vmOfferings.get(id));
        vms.put(id, vm);
        vmStates.put(id, VirtualMachine.State.Running);
        vmOfferings.put(id, offeringId);
        if (type != ETCD) {
            kube.put(name, new Kube());
        }
        return vm;
    }

    private String kubectl(String command) {
        if (KubernetesClusterRollingUpdateWorker.NODE_STATUSES_COMMAND.equals(command)) {
            return "sudo: unable to resolve host\n" + kube.entrySet().stream().map(e -> String.join("\t", e.getKey(), e.getValue().ready ? "True" : "Unknown",
                    e.getValue().bootId, e.getValue().annotation == null ? "" : e.getValue().annotation, e.getValue().unschedulable ? "true" : ""))
                    .collect(Collectors.joining("\n"));
        }
        if (KubernetesClusterRollingUpdateWorker.ETCD_READY_COMMAND.equals(command)) {
            return "ok";
        }
        String[] tokens = command.split(" ");
        String name = tokens[3].equals("node") ? tokens[4] : tokens[3];
        switch (tokens[2]) {
            case "annotate":
                kube.get(name).annotation = tokens[6].split("=")[1];
                calls.add("annotate:" + name);
                break;
            case "cordon":
                kube.get(name).unschedulable = true;
                calls.add("cordon:" + name);
                break;
            case "uncordon":
                kube.get(name).unschedulable = false;
                kube.get(name).annotation = null;
                calls.add("release:" + name);
                break;
            default:
                return null;
        }
        return "";
    }

    private KubernetesClusterRollingUpdateWorker newWorker(int maxUnavailable) {
        KubernetesClusterRollingUpdateWorker worker = Mockito.spy(new KubernetesClusterRollingUpdateWorker(cluster, maxUnavailable, manager));
        worker.userVmDao = userVmDao;
        worker.userVmService = userVmService;
        worker.userVmManager = userVmManager;
        worker.volumeDao = volumeDao;
        worker.affinityGroupVMMapDao = affinityGroupVMMapDao;
        worker.kubectlAddress = new Pair<>("10.0.0.1", 2222);
        Mockito.doNothing().when(worker).init();
        Mockito.doReturn(true).when(worker).transitToOperationState(KubernetesCluster.Event.UpdateRequested);
        Mockito.doNothing().when(worker).resolveKubectlAddress(Mockito.anySet());
        Mockito.doReturn(false).when(worker).sleepBeforeRetry(Mockito.anyLong());
        Mockito.doReturn(true).when(worker).isEtcdMemberHealthy(Mockito.any());
        // the worker unregisters it after each VM action
        Mockito.doAnswer(i -> CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class))).when(worker).registerVmContext(Mockito.any());
        Mockito.doAnswer(i -> kubectl(i.getArgument(0))).when(worker).runKubectl(Mockito.anyString());
        Mockito.doAnswer(i -> {
            String name = KubernetesClusterActionWorker.getNodeName(i.getArgument(2));
            calls.add("drain:" + name);
            return failingDrains.contains(name) ? new Pair<>(false, "Cannot evict pod as it would violate the pod's disruption budget") : new Pair<>(true, "");
        }).when(worker).drainKubernetesClusterNode(Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyInt(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doAnswer(i -> {
            KubernetesCluster.Event event = i.getArgument(1);
            calls.add("event:" + event);
            clusterState = event == KubernetesCluster.Event.OperationSucceeded ? KubernetesCluster.State.Running : KubernetesCluster.State.Alert;
            return true;
        }).when(worker).stateTransitTo(Mockito.eq(CLUSTER_ID), Mockito.any());
        return worker;
    }

    private void addStackedControlPlane(int controls) {
        for (int i = 1; i <= controls; i++) {
            addNode("c" + i, CONTROL, OLD_OFFERING);
        }
    }

    // pure decisions

    @Test
    public void clampMaxUnavailableLeavesAWorkerRunning() {
        Assert.assertEquals(3, KubernetesClusterRollingUpdateWorker.clampMaxUnavailable(3, 4));
        Assert.assertEquals(3, KubernetesClusterRollingUpdateWorker.clampMaxUnavailable(5, 4));
        Assert.assertEquals(1, KubernetesClusterRollingUpdateWorker.clampMaxUnavailable(2, 2));
        Assert.assertEquals(1, KubernetesClusterRollingUpdateWorker.clampMaxUnavailable(1, 1));
        Assert.assertEquals(1, KubernetesClusterRollingUpdateWorker.clampMaxUnavailable(3, 0));
    }

    @Test
    public void quorumAndHealthyOthersNeeded() {
        Assert.assertEquals(1, KubernetesClusterRollingUpdateWorker.quorum(1));
        Assert.assertEquals(2, KubernetesClusterRollingUpdateWorker.quorum(2));
        Assert.assertEquals(2, KubernetesClusterRollingUpdateWorker.quorum(3));
        Assert.assertEquals(3, KubernetesClusterRollingUpdateWorker.quorum(5));
        Assert.assertEquals(2, KubernetesClusterRollingUpdateWorker.healthyOthersNeeded(CONTROL, 3, false));
        Assert.assertEquals(1, KubernetesClusterRollingUpdateWorker.healthyOthersNeeded(CONTROL, 3, true));
        Assert.assertEquals(2, KubernetesClusterRollingUpdateWorker.healthyOthersNeeded(ETCD, 3, true));
    }

    @Test
    public void toBatchesSplitsInOrder() {
        Assert.assertEquals(List.of(List.of(1, 2), List.of(3, 4), List.of(5)), KubernetesClusterRollingUpdateWorker.toBatches(List.of(1, 2, 3, 4, 5), 2));
        Assert.assertEquals(List.of(), KubernetesClusterRollingUpdateWorker.toBatches(List.of(), 1));
    }

    @Test
    public void parseNodeStatusesSkipsOtherLines() {
        Map<String, KubernetesClusterRollingUpdateWorker.NodeStatus> statuses = KubernetesClusterRollingUpdateWorker.parseNodeStatuses(
                "sudo: unable to resolve host x\nw1\tTrue\tb1\t\t\nw2\tFalse\tb2\tb0\ttrue\n");
        Assert.assertEquals(2, statuses.size());
        Assert.assertTrue(statuses.get("w1").ready);
        Assert.assertNull(statuses.get("w1").annotation);
        Assert.assertFalse(statuses.get("w1").unschedulable);
        Assert.assertFalse(statuses.get("w2").ready);
        Assert.assertEquals("b0", statuses.get("w2").annotation);
        Assert.assertTrue(statuses.get("w2").unschedulable);
    }

    @Test
    public void isBackNeedsReadyAndANewBootId() {
        Assert.assertFalse(KubernetesClusterRollingUpdateWorker.isBack(null, "b1"));
        Assert.assertFalse(KubernetesClusterRollingUpdateWorker.isBack(new KubernetesClusterRollingUpdateWorker.NodeStatus(true, "b1", "b1", true), "b1"));
        Assert.assertFalse(KubernetesClusterRollingUpdateWorker.isBack(new KubernetesClusterRollingUpdateWorker.NodeStatus(false, "b2", "b1", true), "b1"));
        Assert.assertFalse(KubernetesClusterRollingUpdateWorker.isBack(new KubernetesClusterRollingUpdateWorker.NodeStatus(true, null, "b1", true), "b1"));
        Assert.assertTrue(KubernetesClusterRollingUpdateWorker.isBack(new KubernetesClusterRollingUpdateWorker.NodeStatus(true, "b2", "b1", true), "b1"));
    }

    // selection

    @Test
    public void selectNodesTakesOutOfDateStoppedAndAnnotatedNodesFirst() {
        addStackedControlPlane(1);
        addNode("w1", WORKER, OLD_OFFERING);
        addNode("w2", WORKER, NEW_OFFERING);
        addNode("w3", WORKER, NEW_OFFERING);
        UserVmVO w4 = addNode("w4", WORKER, NEW_OFFERING);
        UserVmVO external = addNode("x1", WORKER, OLD_OFFERING);
        maps.get(maps.size() - 1).setExternalNode(true);
        kube.get("w3").annotation = "b0";
        vmStates.put(w4.getId(), VirtualMachine.State.Stopped);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        List<String> selected = worker.selectNodes(WORKER, worker.getNodeStatuses()).stream()
                .map(node -> node.vm.getHostName()).collect(Collectors.toList());

        Assert.assertEquals(List.of("w3", "w4", "w1"), selected);
        Assert.assertTrue(worker.selectNodes(CONTROL, worker.getNodeStatuses()).isEmpty());
        Assert.assertNotNull(external);
    }

    // the sequence

    @Test
    public void rolloutAnnotatesDrainsStopsChangesStartsAndReleases() {
        addStackedControlPlane(1);
        addNode("w1", WORKER, OLD_OFFERING);
        addNode("w2", WORKER, NEW_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        Assert.assertTrue(worker.rollout());

        Assert.assertEquals(List.of("annotate:w1", "cordon:w1", "drain:w1", "stop:w1", "upgrade:w1", "start:w1", "release:w1",
                "event:OperationSucceeded"), calls);
        Assert.assertEquals(NEW_OFFERING, (long) vmOfferings.get(vms.keySet().stream().filter(id -> name(id).equals("w1")).findFirst().get()));
        Assert.assertNull(kube.get("w1").annotation);
        Assert.assertFalse(kube.get("w1").unschedulable);
        Assert.assertEquals(KubernetesCluster.State.Running, clusterState);
        Mockito.verify(manager).updateKubernetesClusterCapacity(CLUSTER_ID);
        Assert.assertTrue(details.isEmpty());
    }

    @Test
    public void rolloutRejectsAStaleReadyWithTheSameBootId() {
        addStackedControlPlane(1);
        addNode("w1", WORKER, OLD_OFFERING);
        staleAfterStart.add("w1");
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("not Ready with a new boot ID"));
        Assert.assertFalse(calls.contains("release:w1"));
        // left cordoned and annotated, for the next rollout to take up first
        Assert.assertTrue(kube.get("w1").unschedulable);
        Assert.assertNotNull(kube.get("w1").annotation);
        Assert.assertTrue(details.contains(KubernetesClusterManagerImpl.ROLLOUT_LAST_ERROR_DETAIL));
    }

    @Test
    public void nodeCordonedBySomeoneElseIsNotAnnotatedNorUncordoned() {
        addStackedControlPlane(1);
        addNode("w1", WORKER, OLD_OFFERING);
        kube.get("w1").unschedulable = true;
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        worker.rollout();

        Assert.assertEquals(List.of("cordon:w1", "drain:w1", "stop:w1", "upgrade:w1", "start:w1", "event:OperationSucceeded"), calls);
        Assert.assertTrue(kube.get("w1").unschedulable);
    }

    @Test
    public void workerBatchIsClampedToLeaveAWorkerRunning() {
        addStackedControlPlane(1);
        addNode("w1", WORKER, OLD_OFFERING);
        addNode("w2", WORKER, OLD_OFFERING);
        addNode("w3", WORKER, OLD_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(5);

        worker.rollout();

        List<String> stops = calls.stream().filter(call -> call.startsWith("stop:") || call.startsWith("release:")).collect(Collectors.toList());
        Assert.assertEquals(List.of("stop:w1", "stop:w2", "release:w1", "release:w2", "stop:w3", "release:w3"), stops);
    }

    @Test
    public void preflightRefusesWhileAnotherNodeIsDown() {
        addStackedControlPlane(1);
        addNode("w1", WORKER, OLD_OFFERING);
        addNode("w2", WORKER, NEW_OFFERING);
        kube.get("w2").ready = false;
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("node w2 is not Ready"));
        Assert.assertFalse(calls.stream().anyMatch(call -> call.startsWith("stop:")));
        Assert.assertEquals(KubernetesCluster.State.Alert, clusterState);
    }

    // quorum

    @Test
    public void quorumRefusesTheControlNodeOfATwoNodeStackedControlPlane() {
        addStackedControlPlane(2);
        desiredOfferings.put(CONTROL, NEW_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        Assert.assertTrue(worker.rollout());

        Mockito.verify(detailsDao).addDetail(Mockito.eq(CLUSTER_ID), Mockito.eq(KubernetesClusterManagerImpl.ROLLOUT_LAST_ERROR_DETAIL),
                Mockito.contains("the 2 control node(s) were not restarted, as a cluster with 2 of them can not keep running without one"), Mockito.anyBoolean());
        Assert.assertTrue(calls.stream().noneMatch(call -> call.startsWith("annotate:") || call.startsWith("stop:")));
    }

    @Test
    public void workersOfASingleControlNodeClusterRollWhileItsControlNodeIsReported() {
        addStackedControlPlane(1);
        addNode("w1", WORKER, OLD_OFFERING);
        addNode("w2", WORKER, OLD_OFFERING);
        desiredOfferings.put(CONTROL, NEW_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        // the control node can never be restarted, so a rerun would fail the same way: it is reported, not failed
        Assert.assertTrue(worker.rollout());

        Mockito.verify(detailsDao).addDetail(Mockito.eq(CLUSTER_ID), Mockito.eq(KubernetesClusterManagerImpl.ROLLOUT_LAST_ERROR_DETAIL),
                Mockito.contains("the 1 control node(s) were not restarted"), Mockito.anyBoolean());
        Assert.assertEquals(List.of("stop:w1", "release:w1", "stop:w2", "release:w2"),
                calls.stream().filter(call -> call.startsWith("stop:") || call.startsWith("release:")).collect(Collectors.toList()));
        // the cluster is healthy, the error is recorded
        Assert.assertEquals(KubernetesCluster.State.Running, clusterState);
        Assert.assertTrue(details.contains(KubernetesClusterManagerImpl.ROLLOUT_LAST_ERROR_DETAIL));
    }

    @Test
    public void etcdAndControlNodesThatCanNotBeRestartedAreBothReported() {
        addStackedControlPlane(1);
        addNode("e1", ETCD, OLD_OFFERING);
        addNode("e2", ETCD, OLD_OFFERING);
        desiredOfferings.put(CONTROL, NEW_OFFERING);
        desiredOfferings.put(ETCD, NEW_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);
        Mockito.doReturn(true).when(worker).isEtcdOnOwnNodes();

        Assert.assertTrue(worker.rollout());

        Mockito.verify(detailsDao).addDetail(Mockito.eq(CLUSTER_ID), Mockito.eq(KubernetesClusterManagerImpl.ROLLOUT_LAST_ERROR_DETAIL),
                Mockito.argThat(error -> error.contains("2 etcd node(s)") && error.contains("1 control node(s)")), Mockito.anyBoolean());
        Assert.assertTrue(calls.stream().noneMatch(call -> call.startsWith("stop:")));
    }

    @Test
    public void aFailureIsReportedWithTheNodesThatWereSkipped() {
        addStackedControlPlane(1);
        addNode("w1", WORKER, OLD_OFFERING);
        addNode("w2", WORKER, OLD_OFFERING);
        desiredOfferings.put(CONTROL, NEW_OFFERING);
        failingDrains.add("w1");
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("w1") && e.getMessage().contains("the 1 control node(s) were not restarted"));
    }

    @Test
    public void abortPutsTheClusterBackWithoutLookingAtItsNodes() {
        addStackedControlPlane(1);
        kube.get("c1").ready = false;
        for (KubernetesCluster.State state : List.of(KubernetesCluster.State.Running, KubernetesCluster.State.Alert)) {
            calls.clear();
            clusterState = state;
            KubernetesClusterRollingUpdateWorker worker = newWorker(1);
            worker.begin();
            Mockito.doReturn(state).when(worker).getOperationStartState();
            clusterState = KubernetesCluster.State.Updating;

            worker.abort();

            Assert.assertEquals(List.of("event:" + (state == KubernetesCluster.State.Running ? "OperationSucceeded" : "OperationFailed")), calls);
            Assert.assertEquals(state, clusterState);
            Mockito.verify(worker, Mockito.never()).runKubectl(Mockito.anyString());
        }
    }

    @Test
    public void aCancelAskedForAfterBeginIsKept() {
        addStackedControlPlane(1);
        addNode("w1", WORKER, OLD_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);
        details.add(KubernetesClusterManagerImpl.ROLLOUT_CANCEL_DETAIL);
        worker.begin();
        // asked for while the settings are stored
        details.add(KubernetesClusterManagerImpl.ROLLOUT_CANCEL_DETAIL);

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("cancelled"));
        Assert.assertTrue(calls.stream().noneMatch(call -> call.startsWith("stop:")));
    }

    @Test
    public void affinityGroupsOfTheTypeReplaceTheNodesAndKeepItsDedicationGroup() {
        addStackedControlPlane(1);
        UserVmVO w1 = addNode("w1", WORKER, OLD_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);
        Mockito.when(manager.getNodeTypeWantedAffinityGroupIds(CLUSTER_ID, WORKER)).thenReturn(Set.of(5L));
        Mockito.when(manager.getVmAffinityGroupIds(w1.getId(), false)).thenReturn(Set.of(4L));
        Mockito.when(manager.getVmAffinityGroupIds(w1.getId(), true)).thenReturn(Set.of(30L));

        Assert.assertTrue(worker.rollout());

        Mockito.verify(affinityGroupVMMapDao).updateMap(Mockito.eq(w1.getId()), Mockito.argThat(ids -> new HashSet<>(ids).equals(Set.of(5L, 30L))));
    }

    @Test
    public void quorumRefusesWhenAnotherEtcdMemberIsUnhealthy() {
        addStackedControlPlane(3);
        desiredOfferings.put(CONTROL, NEW_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);
        UserVmVO c2 = vms.values().stream().filter(vm -> vm.getHostName().equals("c2")).findFirst().get();
        Mockito.doReturn(false).when(worker).isEtcdMemberHealthy(c2);
        UserVmVO c3 = vms.values().stream().filter(vm -> vm.getHostName().equals("c3")).findFirst().get();
        Mockito.doReturn(false).when(worker).isEtcdMemberHealthy(c3);

        Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(calls.stream().noneMatch(call -> call.startsWith("stop:")));
    }

    @Test
    public void controlNodesOfAThreeNodeStackedControlPlaneGoOneAtATime() {
        addStackedControlPlane(3);
        desiredOfferings.put(CONTROL, NEW_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(3);

        worker.rollout();

        List<String> steps = calls.stream().filter(call -> call.startsWith("stop:") || call.startsWith("release:")).collect(Collectors.toList());
        Assert.assertEquals(List.of("stop:c1", "release:c1", "stop:c2", "release:c2", "stop:c3", "release:c3"), steps);
        Mockito.verify(worker).resolveKubectlAddress(Set.of(maps.get(0).getVmId()));
    }

    // every exit releases, idempotently

    private List<String> releaseAgain(KubernetesClusterRollingUpdateWorker worker, String error) {
        Set<String> detailsBefore = new HashSet<>(details);
        KubernetesCluster.State stateBefore = clusterState;
        int callsBefore = calls.size();
        worker.release(error);
        Assert.assertEquals(stateBefore, clusterState);
        Assert.assertEquals(detailsBefore, details);
        return calls.subList(callsBefore, calls.size());
    }

    @Test
    public void releaseAfterSuccessIsIdempotent() {
        addStackedControlPlane(1);
        addNode("w1", WORKER, OLD_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);
        worker.rollout();

        Assert.assertEquals(List.of(), releaseAgain(worker, null));
        Mockito.verify(manager, Mockito.times(2)).updateKubernetesClusterCapacity(CLUSTER_ID);
    }

    @Test
    public void releaseAfterADrainFailureUncordonsTheNodeThatNeverWentDown() {
        addStackedControlPlane(1);
        addNode("w1", WORKER, OLD_OFFERING);
        failingDrains.add("w1");
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(calls.contains("release:w1"));
        Assert.assertFalse(calls.stream().anyMatch(call -> call.startsWith("stop:")));
        Assert.assertFalse(kube.get("w1").unschedulable);
        Assert.assertEquals(KubernetesCluster.State.Running, clusterState);
        Assert.assertTrue(details.contains(KubernetesClusterManagerImpl.ROLLOUT_LAST_ERROR_DETAIL));
        Assert.assertEquals(List.of(), releaseAgain(worker, "unable to drain"));
    }

    @Test
    public void releaseAfterAStartFailureLeavesTheNodeCordonedAndTheClusterInAlert() {
        addStackedControlPlane(1);
        addNode("w1", WORKER, OLD_OFFERING);
        failingStarts.add("w1");
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("unable to start the VM of node w1"));
        Assert.assertTrue(kube.get("w1").unschedulable);
        Assert.assertEquals(KubernetesCluster.State.Alert, clusterState);
        Assert.assertEquals(List.of(), releaseAgain(worker, e.getMessage()));
    }

    @Test
    public void releaseAfterACancelStopsBetweenBatches() {
        addStackedControlPlane(1);
        addNode("w1", WORKER, OLD_OFFERING);
        addNode("w2", WORKER, OLD_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);
        Mockito.doAnswer(i -> {
            Object result = i.callRealMethod();
            // asked to stop while the first batch runs
            details.add(KubernetesClusterManagerImpl.ROLLOUT_CANCEL_DETAIL);
            return result;
        }).when(worker).rollBatch(Mockito.anyList());

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("cancelled"));
        Assert.assertTrue(calls.contains("release:w1"));
        Assert.assertFalse(calls.contains("stop:w2"));
        Assert.assertFalse(details.contains(KubernetesClusterManagerImpl.ROLLOUT_CANCEL_DETAIL));
        Assert.assertEquals(KubernetesCluster.State.Running, clusterState);
        Assert.assertEquals(List.of(), releaseAgain(worker, "it was cancelled"));
    }

    @Test
    public void releaseAfterAnExceptionBeforeTheFirstBatch() {
        addStackedControlPlane(1);
        addNode("w1", WORKER, OLD_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);
        Mockito.doThrow(new NullPointerException()).when(worker).rollNodes(Mockito.anyList());

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("NullPointerException"));
        Assert.assertEquals(KubernetesCluster.State.Running, clusterState);
        Assert.assertTrue(details.contains(KubernetesClusterManagerImpl.ROLLOUT_LAST_ERROR_DETAIL));
        Assert.assertEquals(List.of(), releaseAgain(worker, e.getMessage()));
    }

    @Test
    public void releaseDoesNotMoveAClusterItDoesNotHold() {
        addStackedControlPlane(1);
        clusterState = KubernetesCluster.State.Running;
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        worker.release(null);

        Assert.assertFalse(calls.stream().anyMatch(call -> call.startsWith("event:")));
    }

    @Test
    public void releaseUncordonsOnlyAnnotatedNodesThatAreBack() {
        addStackedControlPlane(1);
        addNode("w1", WORKER, NEW_OFFERING);
        addNode("w2", WORKER, NEW_OFFERING);
        addNode("w3", WORKER, NEW_OFFERING);
        kube.get("w1").annotation = "old-boot";
        kube.get("w1").unschedulable = true;
        kube.get("w2").annotation = kube.get("w2").bootId;
        kube.get("w2").unschedulable = true;
        kube.get("w3").unschedulable = true;
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        worker.release("failed");

        Assert.assertEquals(List.of("release:w1", "event:OperationSucceeded"), calls);
        Assert.assertTrue(kube.get("w2").unschedulable);
        Assert.assertTrue(kube.get("w3").unschedulable);
    }

    // a rerun after a failure

    @Test
    public void rerunAfterAFailureTakesUpTheStoppedNodeFirst() {
        addStackedControlPlane(1);
        addNode("w1", WORKER, OLD_OFFERING);
        addNode("w2", WORKER, OLD_OFFERING);
        addNode("w3", WORKER, OLD_OFFERING);
        failingStarts.add("w2");
        KubernetesClusterRollingUpdateWorker first = newWorker(1);
        Assert.assertThrows(CloudRuntimeException.class, first::rollout);
        Assert.assertEquals(KubernetesCluster.State.Alert, clusterState);

        failingStarts.clear();
        calls.clear();
        clusterState = KubernetesCluster.State.Updating;
        KubernetesClusterRollingUpdateWorker second = newWorker(1);
        second.rollout();

        // w2 is stopped and annotated: started first, without a drain or a stop; w3 after it
        Assert.assertEquals(List.of("cordon:w2", "start:w2", "release:w2", "annotate:w3", "cordon:w3", "drain:w3", "stop:w3", "upgrade:w3", "start:w3",
                "release:w3", "event:OperationSucceeded"), calls);
        Assert.assertEquals(KubernetesCluster.State.Running, clusterState);
    }

    @Test
    public void rerunReleasesANodeLeftBackButCordonedWithoutRestartingIt() {
        addStackedControlPlane(1);
        addNode("w1", WORKER, NEW_OFFERING);
        kube.get("w1").annotation = "boot-before-crash";
        kube.get("w1").unschedulable = true;
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        worker.rollout();

        Assert.assertEquals(List.of("release:w1", "event:OperationSucceeded"), calls);
    }
}
