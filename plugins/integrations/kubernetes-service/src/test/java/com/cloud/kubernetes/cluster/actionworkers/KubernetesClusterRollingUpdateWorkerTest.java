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
import org.apache.cloudstack.api.ApiConstants;
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
import com.cloud.kubernetes.version.KubernetesSupportedVersionVO;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.storage.LaunchPermissionVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.LaunchPermissionDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.template.TemplateApiService;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmService;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VMInstanceDetailVO;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

/**
 * Runs the rollout against a simulated cluster: the VMs, their offerings, and the Kubernetes nodes kubectl reports,
 * with the calls made to them recorded in order.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class KubernetesClusterRollingUpdateWorkerTest {

    private static final long CLUSTER_ID = 1L;
    private static final long OLD_OFFERING = 10L;
    private static final long NEW_OFFERING = 20L;
    private static final long OLD_TEMPLATE = 30L;
    private static final long NEW_TEMPLATE = 40L;
    private static final long ISO = 50L;

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
    @Mock
    VMTemplateDao templateDao;
    @Mock
    TemplateApiService templateService;
    @Mock
    LaunchPermissionDao launchPermissionDao;
    @Mock
    KubernetesSupportedVersionDao kubernetesSupportedVersionDao;
    @Mock
    Account owner;
    @Mock
    VMInstanceDetailsDao vmDetailsDao;
    @Mock
    SnapshotDao snapshotDao;
    @Mock
    VMSnapshotDao vmSnapshotDao;
    @Mock
    DiskOfferingDao diskOfferingDao;
    @Mock
    VirtualMachineManager itMgr;
    @Mock
    com.cloud.user.AccountManager accountManager;
    @Mock
    IPAddressDao ipAddressDao;
    @Mock
    PortForwardingRulesDao portForwardingRulesDao;

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
    final Map<Long, Long> vmTemplates = new HashMap<>();
    final Map<Long, Long> vmIsos = new HashMap<>();
    final Map<String, Kube> kube = new LinkedHashMap<>();
    final Map<KubernetesClusterNodeType, Long> desiredOfferings = new HashMap<>();
    final List<String> calls = new ArrayList<>();
    final Set<String> staleAfterStart = new HashSet<>();
    final Set<String> failingDrains = new HashSet<>();
    final Set<String> failingStarts = new HashSet<>();
    final Set<String> details = new HashSet<>();
    static final String JOIN_HASH = "sha256:" + "ab".repeat(32);
    static final String OLD_JOIN = "kubeadm join 10.1.1.1:6443 --token abcdef.0123456789abcdef --discovery-token-unsafe-skip-ca-verification";
    /** The user data of the VMs, decoded */
    final Map<Long, String> vmUserData = new HashMap<>();
    /** The VMs that have the recreate detail, with its value */
    final Map<Long, String> recreating = new HashMap<>();
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
        Mockito.when(manager.getNodeTypeTemplateId(Mockito.any(), Mockito.any())).thenReturn(null);
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
        Mockito.when(vm.getTemplateId()).thenAnswer(i -> vmTemplates.getOrDefault(id, OLD_TEMPLATE));
        Mockito.when(vm.getIsoId()).thenAnswer(i -> vmIsos.get(id));
        vmUserData.put(id, "#cloud-config\n      " + OLD_JOIN + "\n");
        Mockito.when(vm.getUserData()).thenAnswer(i -> org.apache.commons.codec.binary.Base64.encodeBase64String(vmUserData.get(id).getBytes()));
        Mockito.doAnswer(i -> {
            vmUserData.put(id, new String(org.apache.commons.codec.binary.Base64.decodeBase64((String) i.getArgument(0))));
            return calls.add("userData:" + name);
        }).when(vm).setUserData(Mockito.anyString());
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
        if (command.startsWith("sudo /opt/bin/kubeadm token create --ttl ")) {
            calls.add("tokenCreate");
            return "kubeadm join 10.1.1.1:6443 --token fresh1.0123456789abcdef --discovery-token-ca-cert-hash " + JOIN_HASH + " \n";
        }
        if (command.startsWith("sudo /opt/bin/kubeadm token delete ")) {
            calls.add("tokenDelete:" + command.substring(command.lastIndexOf(' ') + 1));
            return "";
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
        worker.templateDao = templateDao;
        worker.templateService = templateService;
        worker.launchPermissionDao = launchPermissionDao;
        worker.kubernetesSupportedVersionDao = kubernetesSupportedVersionDao;
        worker.owner = owner;
        worker.vmInstanceDetailsDao = vmDetailsDao;
        worker.snapshotDao = snapshotDao;
        worker.vmSnapshotDao = vmSnapshotDao;
        worker.diskOfferingDao = diskOfferingDao;
        worker.itMgr = itMgr;
        worker.accountManager = accountManager;
        Mockito.doAnswer(i -> {
            List<UserVm> detached = i.getArgument(0);
            detached.forEach(vm -> {
                calls.add("detachIso:" + vm.getHostName());
                vmIsos.remove(vm.getId());
            });
            return null;
        }).when(worker).detachIsoKubernetesVMs(Mockito.anyList());
        Mockito.when(vmDetailsDao.findDetail(Mockito.anyLong(), Mockito.eq(KubernetesClusterRollingUpdateWorker.RECREATE_DETAIL))).thenAnswer(i -> {
            String value = recreating.get((Long) i.getArgument(0));
            if (value == null) {
                return null;
            }
            VMInstanceDetailVO detail = Mockito.mock(VMInstanceDetailVO.class);
            Mockito.when(detail.getValue()).thenReturn(value);
            return detail;
        });
        Mockito.doAnswer(i -> recreating.put(i.getArgument(0), i.getArgument(2))).when(vmDetailsDao)
                .addDetail(Mockito.anyLong(), Mockito.eq(KubernetesClusterRollingUpdateWorker.RECREATE_DETAIL), Mockito.anyString(), Mockito.anyBoolean());
        Mockito.doAnswer(i -> recreating.remove((Long) i.getArgument(0))).when(vmDetailsDao)
                .removeDetail(Mockito.anyLong(), Mockito.eq(KubernetesClusterRollingUpdateWorker.RECREATE_DETAIL));
        worker.kubectlAddress = new Pair<>("10.0.0.1", 2222);
        Mockito.doNothing().when(worker).init();
        Mockito.doReturn(true).when(worker).transitToOperationState(KubernetesCluster.Event.UpdateRequested);
        Mockito.doNothing().when(worker).resolveKubectlAddress(Mockito.anySet());
        Mockito.doAnswer(i -> {
            worker.nodeSshAddresses = vms.keySet().stream().collect(Collectors.toMap(id -> id, id -> new Pair<>("10.0.0.1", 2222)));
            return null;
        }).when(worker).resolveNodeSshAddresses();
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
        Mockito.verify(manager).syncClusterServiceOffering(CLUSTER_ID);
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
        // not once the cluster has left Updating, when another operation may hold it
        Mockito.verify(manager).syncClusterServiceOffering(CLUSTER_ID);
    }

    @Test
    public void releaseEndsTheRolloutWhenTheClusterOfferingCanNotBeUpdated() {
        addStackedControlPlane(1);
        addNode("w1", WORKER, OLD_OFFERING);
        Mockito.doThrow(new CloudRuntimeException("db down")).when(manager).syncClusterServiceOffering(CLUSTER_ID);

        Assert.assertTrue(newWorker(1).rollout());

        Assert.assertEquals(KubernetesCluster.State.Running, clusterState);
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

    // a new template

    /** Gives the worker nodes a new template; the VM of a node is recreated from it, and Kubernetes sees the node go down */
    private void setUpNewWorkerTemplate(Storage.TemplateType templateType) {
        Mockito.doReturn(NEW_TEMPLATE).when(manager).getNodeTypeTemplateId(Mockito.any(), Mockito.eq(WORKER));
        Mockito.doAnswer(i -> {
            KubernetesClusterVmMapVO vmMap = i.getArgument(1);
            UserVmVO vm = i.getArgument(2);
            KubernetesClusterNodeType type = KubernetesClusterUtil.getNodeType(vmMap);
            if (type == WORKER && vm.getTemplateId() != NEW_TEMPLATE) {
                return "its template differs";
            }
            return desiredOfferings.get(type).equals(vmOfferings.get(vm.getId())) ? null : "its service offering differs";
        }).when(manager).getNodeOutOfDateReason(Mockito.any(), Mockito.any(), Mockito.any());
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        Mockito.when(template.getSize()).thenReturn(5L << 30);
        Mockito.when(template.getTemplateType()).thenReturn(templateType);
        Mockito.when(template.getAccountId()).thenReturn(1L);
        Mockito.when(template.getId()).thenReturn(NEW_TEMPLATE);
        Mockito.when(template.getState()).thenReturn(VirtualMachineTemplate.State.Active);
        Mockito.when(manager.isTemplateReadyInZone(Mockito.any(), Mockito.anyLong())).thenReturn(true);
        Mockito.when(templateDao.findById(NEW_TEMPLATE)).thenReturn(template);
        Mockito.when(templateDao.findById(OLD_TEMPLATE)).thenReturn(Mockito.mock(VMTemplateVO.class));
        KubernetesSupportedVersionVO version = Mockito.mock(KubernetesSupportedVersionVO.class);
        Mockito.when(version.getIsoId()).thenReturn(ISO);
        Mockito.when(kubernetesSupportedVersionDao.findById(Mockito.anyLong())).thenReturn(version);
        VMTemplateVO iso = Mockito.mock(VMTemplateVO.class);
        Mockito.when(iso.getState()).thenReturn(VirtualMachineTemplate.State.Active);
        Mockito.when(iso.getId()).thenReturn(ISO);
        Mockito.when(templateDao.findById(ISO)).thenReturn(iso);
        Mockito.when(owner.getId()).thenReturn(7L);
        VolumeVO root = Mockito.mock(VolumeVO.class);
        Mockito.when(root.getSize()).thenReturn(20L << 30);
        Mockito.when(volumeDao.findByInstanceAndType(Mockito.anyLong(), Mockito.eq(Volume.Type.ROOT))).thenReturn(List.of(root));
        try {
            Mockito.doAnswer(i -> {
                long id = i.getArgument(0);
                Map<String, String> details = i.getArgument(4);
                calls.add("restore:" + name(id) + ":" + details.get(VmDetailConstants.ROOT_DISK_SIZE));
                vmTemplates.put(id, NEW_TEMPLATE);
                kube.get(name(id)).ready = false;
                return vms.get(id);
            }).when(itMgr).restoreVirtualMachine(Mockito.anyLong(), Mockito.eq(NEW_TEMPLATE), Mockito.isNull(), Mockito.eq(false), Mockito.anyMap());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Mockito.when(templateService.attachIso(Mockito.eq(ISO), Mockito.anyLong(), Mockito.eq(true))).thenAnswer(i -> {
            vmIsos.put(i.getArgument(1), ISO);
            return calls.add("attachIso:" + name(i.getArgument(1)));
        });
    }

    @Test
    public void workerWithAnotherTemplateIsRecreatedFromTheTemplateOfItsType() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        addNode("w1", WORKER, NEW_OFFERING);
        UserVmVO w2 = addNode("w2", WORKER, NEW_OFFERING);
        vmTemplates.put(w2.getId(), NEW_TEMPLATE);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        Assert.assertTrue(worker.rollout());

        // the root disk keeps its 20 GB, the larger of it and the 5 GB template
        Assert.assertEquals(List.of("tokenCreate", "annotate:w1", "cordon:w1", "drain:w1", "stop:w1", "userData:w1", "restore:w1:20", "attachIso:w1", "start:w1",
                "release:w1", "tokenDelete:fresh1",
                "detachIso:w1", "event:OperationSucceeded"), calls);
        Assert.assertNull(kube.get("w1").annotation);
        Assert.assertTrue(recreating.isEmpty());
        Mockito.verify(launchPermissionDao, Mockito.never()).persist(Mockito.any());
        Assert.assertEquals(KubernetesCluster.State.Running, clusterState);
    }

    @Test
    public void recreateFromASystemVmTemplateLetsTheOwnerUseItMeanwhile() {
        setUpNewWorkerTemplate(Storage.TemplateType.SYSTEM);
        addStackedControlPlane(1);
        addNode("w1", WORKER, NEW_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        Assert.assertTrue(worker.rollout());

        Mockito.verify(launchPermissionDao).persist(Mockito.any(LaunchPermissionVO.class));
        long callerId = CallContext.current().getCallingAccountId();
        Mockito.verify(launchPermissionDao).removePermissions(NEW_TEMPLATE, List.of(callerId));
    }

    @Test
    public void recreateWaitsForTheNodeToBeNotReady() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        addNode("w1", WORKER, NEW_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);
        try {
            // Kubernetes still sees the node Ready
            Mockito.doAnswer(i -> calls.add("restore:" + name(i.getArgument(0))) ? null : null).when(itMgr)
                    .restoreVirtualMachine(Mockito.anyLong(), Mockito.anyLong(), Mockito.isNull(), Mockito.eq(false), Mockito.anyMap());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("did not report NotReady"));
        Assert.assertFalse(calls.contains("start:w1"));
        // the ISO stays for the VM to install from once started; the node stays cordoned, and the detail has the next rollout recreate it
        Assert.assertFalse(calls.contains("detachIso:w1"));
        Assert.assertTrue(kube.get("w1").unschedulable);
        Assert.assertEquals(1, recreating.size());
    }

    @Test
    public void rerunRecreatesAgainANodeWhoseRecreateDidNotFinish() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        UserVmVO w1 = addNode("w1", WORKER, NEW_OFFERING);
        // recreated from the new template, but stopped before it joined again
        vmTemplates.put(w1.getId(), NEW_TEMPLATE);
        vmStates.put(w1.getId(), VirtualMachine.State.Stopped);
        kube.get("w1").ready = false;
        kube.get("w1").annotation = "boot-before";
        kube.get("w1").unschedulable = true;
        recreating.put(w1.getId(), kube.get("w1").bootId);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        Assert.assertTrue(worker.rollout());

        Assert.assertEquals(List.of("tokenCreate", "cordon:w1", "userData:w1", "restore:w1:20", "attachIso:w1", "start:w1", "release:w1", "tokenDelete:fresh1", "detachIso:w1", "event:OperationSucceeded"), calls);
    }

    @Test
    public void controlNodesAreNotRecreated() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        Mockito.doReturn(NEW_TEMPLATE).when(manager).getNodeTypeTemplateId(Mockito.any(), Mockito.eq(CONTROL));
        addStackedControlPlane(3);
        desiredOfferings.put(CONTROL, NEW_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        Assert.assertTrue(worker.rollout());

        Assert.assertTrue(calls.contains("upgrade:c1"));
        Assert.assertTrue(calls.stream().noneMatch(call -> call.startsWith("restore:") || call.startsWith("attachIso:")));
    }

    @Test
    public void aStartedRecreatedNodeThatIsNotBackKeepsTheIsoUntilALaterRolloutSeesItBack() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        addNode("w1", WORKER, NEW_OFFERING);
        staleAfterStart.add("w1");
        KubernetesClusterRollingUpdateWorker first = newWorker(1);
        Assert.assertThrows(CloudRuntimeException.class, first::rollout);
        Assert.assertFalse(calls.contains("detachIso:w1"));

        // it joins after all: the next rollout releases it without recreating it again
        kube.get("w1").ready = true;
        kube.get("w1").bootId = UUID.randomUUID().toString();
        calls.clear();
        clusterState = KubernetesCluster.State.Updating;
        Assert.assertTrue(newWorker(1).rollout());

        Assert.assertEquals(List.of("detachIso:w1", "release:w1", "event:OperationSucceeded"), calls);
        Assert.assertTrue(recreating.isEmpty());
    }

    @Test
    public void aNodeWhoseRootDiskIsBeingSnapshottedIsNotRecreated() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        addNode("w1", WORKER, NEW_OFFERING);
        Mockito.when(snapshotDao.listByStatus(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(List.of(Mockito.mock(SnapshotVO.class)));
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("snapshot"));
        Assert.assertFalse(calls.contains("drain:w1"));
    }

    @Test
    public void aNodeWithVmSnapshotsIsNotRecreated() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        addNode("w1", WORKER, NEW_OFFERING);
        Mockito.when(vmSnapshotDao.findByVm(Mockito.anyLong())).thenReturn(List.of(Mockito.mock(VMSnapshotVO.class)));
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("VM snapshots"));
        Assert.assertFalse(calls.contains("drain:w1"));
    }

    @Test
    public void aRecreatedNodeSomeoneElseCordonedIsCleanedUpOnceItIsReady() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        UserVmVO w1 = addNode("w1", WORKER, NEW_OFFERING);
        vmTemplates.put(w1.getId(), NEW_TEMPLATE);
        kube.get("w1").unschedulable = true;
        recreating.put(w1.getId(), "boot-before-recreate");
        vmIsos.put(w1.getId(), ISO);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        Assert.assertTrue(worker.rollout());

        // not recreated again, and left cordoned, as someone else cordoned it
        Assert.assertEquals(List.of("detachIso:w1", "event:OperationSucceeded"), calls);
        Assert.assertTrue(recreating.isEmpty());
        Assert.assertTrue(kube.get("w1").unschedulable);
    }

    @Test
    public void aTemplateThatIsNoLongerReadyIsRefusedBeforeTheDrain() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        addNode("w1", WORKER, NEW_OFFERING);
        Mockito.when(manager.isTemplateReadyInZone(Mockito.eq(templateDao.findById(NEW_TEMPLATE)), Mockito.anyLong())).thenReturn(false);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("not active and ready"));
        Assert.assertFalse(calls.contains("drain:w1"));
    }

    @Test
    public void aTemplateLargerThanTheRootDiskOfAStrictOfferingIsRefusedBeforeTheDrain() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        Mockito.when(templateDao.findById(NEW_TEMPLATE).getSize()).thenReturn(30L << 30);
        com.cloud.storage.DiskOfferingVO strict = Mockito.mock(com.cloud.storage.DiskOfferingVO.class);
        Mockito.when(strict.getDiskSizeStrictness()).thenReturn(true);
        Mockito.when(diskOfferingDao.findById(Mockito.anyLong())).thenReturn(strict);
        addStackedControlPlane(1);
        addNode("w1", WORKER, NEW_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("strictly fixed"));
        Assert.assertFalse(calls.contains("drain:w1"));
    }

    @Test
    public void aRootDiskSizeOfTheTypeLargerThanTheRootDiskOfAStrictOfferingIsRefusedBeforeTheDrain() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        Mockito.when(clusterVO.getWorkerNodeRootDiskSize()).thenReturn(30L);
        com.cloud.storage.DiskOfferingVO strict = Mockito.mock(com.cloud.storage.DiskOfferingVO.class);
        Mockito.when(strict.getDiskSizeStrictness()).thenReturn(true);
        Mockito.when(diskOfferingDao.findById(Mockito.anyLong())).thenReturn(strict);
        addStackedControlPlane(1);
        addNode("w1", WORKER, NEW_OFFERING);

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, newWorker(1)::rollout);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("strictly fixed"));
        Assert.assertFalse(calls.contains("drain:w1"));
    }

    @Test
    public void aRestoreRefusedBeforeTheRootDiskWasReplacedLeavesNoRecreateDetail() throws Exception {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        addNode("w1", WORKER, NEW_OFFERING);
        Mockito.doThrow(new CloudRuntimeException("refused")).when(itMgr)
                .restoreVirtualMachine(Mockito.anyLong(), Mockito.eq(NEW_TEMPLATE), Mockito.isNull(), Mockito.eq(false), Mockito.anyMap());

        Assert.assertThrows(CloudRuntimeException.class, newWorker(1)::rollout);

        Assert.assertTrue(calls.toString(), calls.contains("stop:w1"));
        Assert.assertTrue(recreating.isEmpty());
    }

    @Test
    public void aFailedIsoDetachDoesNotHaveTheNodeRecreatedAgain() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        UserVmVO w1 = addNode("w1", WORKER, NEW_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);
        // the detach fails, and only logs it
        Mockito.doAnswer(i -> calls.add("detachIsoFailed") ? null : null).when(worker).detachIsoKubernetesVMs(Mockito.anyList());

        Assert.assertTrue(worker.rollout());

        Assert.assertTrue(recreating.isEmpty());
        // a later rollout for another setting restarts the node, without recreating it
        vmOfferings.put(w1.getId(), OLD_OFFERING);
        calls.clear();
        clusterState = KubernetesCluster.State.Updating;
        Assert.assertTrue(newWorker(1).rollout());
        Assert.assertTrue(calls.toString(), calls.contains("upgrade:w1"));
        Assert.assertTrue(calls.stream().noneMatch(call -> call.startsWith("restore:")));
    }

    @Test
    public void aNodeWhoseTemplateWasDeletedIsRefusedBeforeTheDrain() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        Mockito.when(templateDao.findById(OLD_TEMPLATE)).thenReturn(null);
        addStackedControlPlane(1);
        addNode("w1", WORKER, NEW_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("was deleted"));
        Assert.assertFalse(calls.contains("drain:w1"));
    }

    @Test
    public void aRecreatedNodeSomeoneElseCordonedThatIsNotBackIsRecreatedAgain() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        UserVmVO w1 = addNode("w1", WORKER, NEW_OFFERING);
        vmTemplates.put(w1.getId(), NEW_TEMPLATE);
        kube.get("w1").unschedulable = true;
        kube.get("w1").ready = false;
        recreating.put(w1.getId(), kube.get("w1").bootId);
        // the rollout before recreated the node someone else had cordoned, but it did not come back
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);

        Assert.assertTrue(worker.rollout());

        Assert.assertTrue(calls.toString(), calls.contains("restore:w1:20"));
        Assert.assertTrue(recreating.isEmpty());
        Assert.assertTrue(kube.get("w1").unschedulable);
    }

    @Test
    public void aNodeWithAnotherIsoOrADeleteProtectedRootDiskIsRefusedBeforeTheDrain() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        UserVmVO w1 = addNode("w1", WORKER, NEW_OFFERING);
        vmIsos.put(w1.getId(), 99L);
        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, newWorker(1)::rollout);
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("another ISO"));

        vmIsos.clear();
        VolumeVO root = volumeDao.findByInstanceAndType(w1.getId(), Volume.Type.ROOT).get(0);
        Mockito.when(root.isDeleteProtection()).thenReturn(true);
        clusterState = KubernetesCluster.State.Updating;
        e = Assert.assertThrows(CloudRuntimeException.class, newWorker(1)::rollout);
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("delete protection"));
        Assert.assertFalse(calls.contains("drain:w1"));
    }

    @Test
    public void aRecreateWhoseNewRootDiskIsNotCreatedYetIsNotRestoredAgain() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        UserVmVO w1 = addNode("w1", WORKER, NEW_OFFERING);
        vmTemplates.put(w1.getId(), NEW_TEMPLATE);
        vmStates.put(w1.getId(), VirtualMachine.State.Stopped);
        kube.get("w1").ready = false;
        kube.get("w1").annotation = "boot-before";
        kube.get("w1").unschedulable = true;
        recreating.put(w1.getId(), kube.get("w1").bootId);
        VolumeVO root = volumeDao.findByInstanceAndType(w1.getId(), Volume.Type.ROOT).get(0);
        Mockito.when(root.getState()).thenReturn(Volume.State.Allocated);

        Assert.assertTrue(newWorker(1).rollout());

        Assert.assertEquals(List.of("tokenCreate", "cordon:w1", "userData:w1", "attachIso:w1", "start:w1", "release:w1", "tokenDelete:fresh1", "detachIso:w1", "event:OperationSucceeded"), calls);
    }

    @Test
    public void aVmThatIsNotStoppedIsNotRestored() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        UserVmVO w1 = addNode("w1", WORKER, NEW_OFFERING);
        vmStates.put(w1.getId(), VirtualMachine.State.Migrating);

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, newWorker(1)::rollout);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("not stopped"));
        Assert.assertTrue(calls.stream().noneMatch(call -> call.startsWith("restore:")));
    }

    @Test
    public void aRecreatedWorkerJoinsWithAFreshTokenAndThePinnedCa() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        UserVmVO w1 = addNode("w1", WORKER, NEW_OFFERING);
        // user data of a cluster that already pins the CA
        vmUserData.put(w1.getId(), "x\n  kubeadm join 10.1.1.1:6443 --token old123.0123456789abcdef --discovery-token-ca-cert-hash sha256:" + "cd".repeat(32) + "\ny\n");

        Assert.assertTrue(newWorker(1).rollout());

        Assert.assertEquals("x\n  kubeadm join 10.1.1.1:6443 --token fresh1.0123456789abcdef --discovery-token-ca-cert-hash " + JOIN_HASH + "\ny\n",
                vmUserData.get(w1.getId()));
        Assert.assertTrue(calls.indexOf("userData:w1") < calls.indexOf("restore:w1:20"));
        Assert.assertTrue(calls.indexOf("tokenDelete:fresh1") > calls.indexOf("start:w1"));
    }

    @Test
    public void userDataWithoutTheCaPinnedGetsItPinned() {
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        Mockito.when(vm.getUserData()).thenReturn(org.apache.commons.codec.binary.Base64.encodeBase64String(("a\n" + OLD_JOIN + "\n").getBytes()));

        String userData = KubernetesClusterRollingUpdateWorker.getJoinUserData(vm, null, "fresh1.0123456789abcdef", JOIN_HASH);

        Assert.assertEquals("a\nkubeadm join 10.1.1.1:6443 --token fresh1.0123456789abcdef --discovery-token-ca-cert-hash " + JOIN_HASH + "\n",
                new String(org.apache.commons.codec.binary.Base64.decodeBase64(userData)));
        Mockito.when(vm.getUserData()).thenReturn(org.apache.commons.codec.binary.Base64.encodeBase64String("#!/bin/bash\necho".getBytes()));
        Assert.assertNull(KubernetesClusterRollingUpdateWorker.getJoinUserData(vm, null, "fresh1.0123456789abcdef", JOIN_HASH));
    }

    @Test
    public void userDataJoinGetsTheGivenApiServerAddress() {
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        Mockito.when(vm.getUserData()).thenReturn(org.apache.commons.codec.binary.Base64.encodeBase64String(("a\n" + OLD_JOIN
                + "\nExecStartPre=/usr/bin/curl -k https://10.1.1.1:6443/version\ncurl https://10.1.1.1:5000/v2/\n").getBytes()));

        String userData = KubernetesClusterRollingUpdateWorker.getJoinUserData(vm, "203.0.113.5", "fresh1.0123456789abcdef", JOIN_HASH);

        Assert.assertEquals("a\nkubeadm join 203.0.113.5:6443 --token fresh1.0123456789abcdef --discovery-token-ca-cert-hash " + JOIN_HASH
                + "\nExecStartPre=/usr/bin/curl -k https://203.0.113.5:6443/version\ncurl https://10.1.1.1:5000/v2/\n",
                new String(org.apache.commons.codec.binary.Base64.decodeBase64(userData)));
    }

    private KubernetesClusterRollingUpdateWorker workerPrinting(String endpoint, String certificateCheck) {
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);
        Mockito.doReturn("kubeadm join " + endpoint + ":6443 --token fresh1.0123456789abcdef --discovery-token-ca-cert-hash " + JOIN_HASH + " \n")
                .when(worker).runKubectl(Mockito.startsWith("sudo /opt/bin/kubeadm token create"));
        Mockito.doReturn(certificateCheck).when(worker).runKubectl(Mockito.startsWith("echo | timeout 10 openssl s_client -connect 10.1.1.1:6443 "));
        return worker;
    }

    @Test
    public void aRecreatedWorkerJoinsOnThePrintedEndpointIfTheCertificateDoesNotCoverItsAddress() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        UserVmVO w1 = addNode("w1", WORKER, NEW_OFFERING);

        Assert.assertTrue(workerPrinting("203.0.113.5", "IP 10.1.1.1 does NOT match certificate\n").rollout());

        Assert.assertTrue(vmUserData.get(w1.getId()), vmUserData.get(w1.getId()).contains(
                "kubeadm join 203.0.113.5:6443 --token fresh1.0123456789abcdef --discovery-token-ca-cert-hash " + JOIN_HASH));
    }

    @Test
    public void aRecreatedWorkerKeepsItsAddressIfTheCertificateCoversIt() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        UserVmVO w1 = addNode("w1", WORKER, NEW_OFFERING);

        Assert.assertTrue(workerPrinting("203.0.113.5", "IP 10.1.1.1 does match certificate\n").rollout());

        Assert.assertTrue(vmUserData.get(w1.getId()), vmUserData.get(w1.getId()).contains("kubeadm join 10.1.1.1:6443 --token fresh1.0123456789abcdef"));
    }

    @Test
    public void aRecreatedWorkerKeepsItsAddressIfTheCertificateCanNotBeChecked() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        UserVmVO w1 = addNode("w1", WORKER, NEW_OFFERING);

        Assert.assertTrue(workerPrinting("203.0.113.5", null).rollout());

        Assert.assertTrue(vmUserData.get(w1.getId()), vmUserData.get(w1.getId()).contains("kubeadm join 10.1.1.1:6443 --token fresh1.0123456789abcdef"));
    }

    @Test
    public void aRecreatedWorkerKeepsItsAddressIfTheEndpointIsNotAnAddress() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        UserVmVO w1 = addNode("w1", WORKER, NEW_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = workerPrinting("api.example.com", "IP 10.1.1.1 does NOT match certificate\n");

        Assert.assertTrue(worker.rollout());

        Assert.assertTrue(vmUserData.get(w1.getId()), vmUserData.get(w1.getId()).contains("kubeadm join 10.1.1.1:6443 --token fresh1.0123456789abcdef"));
    }

    @Test
    public void joinEndpointParsing() {
        Assert.assertEquals("203.0.113.5", KubernetesClusterRollingUpdateWorker.parseJoinEndpointIp(
                "kubeadm join 203.0.113.5:6443 --token fresh1.0123456789abcdef --discovery-token-ca-cert-hash " + JOIN_HASH + " \n"));
        Assert.assertNull(KubernetesClusterRollingUpdateWorker.parseJoinEndpointIp("kubeadm join api.example.com:6443 --token fresh1.0123456789abcdef"));
        Assert.assertNull(KubernetesClusterRollingUpdateWorker.parseJoinEndpointIp("kubeadm join 999.1.1.1:6443 --token fresh1.0123456789abcdef"));
        Assert.assertNull(KubernetesClusterRollingUpdateWorker.parseJoinEndpointIp("kubeadm join 10.1.1.1:6443;id --token fresh1.0123456789abcdef"));
        for (String ip : List.of("127.0.0.1", "0.0.0.0", "169.254.1.1", "224.0.0.1", "255.255.255.255")) {
            Assert.assertNull(ip, KubernetesClusterRollingUpdateWorker.parseJoinEndpointIp("kubeadm join " + ip + ":6443 --token fresh1.0123456789abcdef"));
        }
        Assert.assertNull(KubernetesClusterRollingUpdateWorker.parseJoinEndpointIp("e.g. kubeadm join 10.9.9.9:6443\n"));
        Assert.assertNull(KubernetesClusterRollingUpdateWorker.parseJoinEndpointIp(null));
    }

    @Test
    public void joinCommandParsing() {
        Assert.assertEquals(new Pair<>("fresh1.0123456789abcdef", JOIN_HASH), KubernetesClusterRollingUpdateWorker.parseJoinCommand(
                "kubeadm join 10.1.1.1:6443 --token fresh1.0123456789abcdef --discovery-token-ca-cert-hash " + JOIN_HASH + " \n"));
        Assert.assertNull(KubernetesClusterRollingUpdateWorker.parseJoinCommand("kubeadm join 10.1.1.1:6443 --token fresh1.0123456789abcdef"));
        Assert.assertNull(KubernetesClusterRollingUpdateWorker.parseJoinCommand(null));
    }

    @Test
    public void aWorkerWhoseUserDataHasNoJoinIsRefusedBeforeTheDrain() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        UserVmVO w1 = addNode("w1", WORKER, NEW_OFFERING);
        vmUserData.put(w1.getId(), "#!/bin/bash\necho");

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, newWorker(1)::rollout);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("no kubeadm join"));
        Assert.assertFalse(calls.contains("tokenCreate"));
        Assert.assertFalse(calls.contains("drain:w1"));
    }

    @Test
    public void aJoinTokenThatCanNotBeCreatedStopsTheBatchBeforeTheDrain() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        addNode("w1", WORKER, NEW_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);
        Mockito.doReturn(null).when(worker).runKubectl(Mockito.startsWith("sudo /opt/bin/kubeadm token create"));

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("join token"));
        Assert.assertFalse(calls.contains("drain:w1"));
    }

    @Test
    public void theJoinTokenIsDeletedOnAFailureBeforeAnyNodeWasStarted() throws Exception {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        UserVmVO w1 = addNode("w1", WORKER, NEW_OFFERING);
        Mockito.doThrow(new CloudRuntimeException("refused")).when(itMgr)
                .restoreVirtualMachine(Mockito.anyLong(), Mockito.eq(NEW_TEMPLATE), Mockito.isNull(), Mockito.eq(false), Mockito.anyMap());

        Assert.assertThrows(CloudRuntimeException.class, newWorker(1)::rollout);

        Assert.assertTrue(calls.toString(), calls.contains("tokenDelete:fresh1"));
        // the restore was refused, so the VM keeps the user data it had
        Assert.assertTrue(vmUserData.get(w1.getId()).contains(OLD_JOIN));
    }

    @Test
    public void aJoinTokenWhoseHashCouldNotBeReadIsDeleted() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        addNode("w1", WORKER, NEW_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);
        Mockito.doReturn("W1122 warning\nkubeadm join 10.1.1.1:6443 --token fresh1.0123456789abcdef").when(worker)
                .runKubectl(Mockito.startsWith("sudo /opt/bin/kubeadm token create"));

        Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(calls.toString(), calls.contains("tokenDelete:fresh1"));
        Assert.assertFalse(calls.contains("drain:w1"));
    }

    @Test
    public void theJoinTokenIsKeptForANodeThatMayStillBeJoining() {
        setUpNewWorkerTemplate(Storage.TemplateType.USER);
        addStackedControlPlane(1);
        addNode("w1", WORKER, NEW_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);
        Mockito.doThrow(new CloudRuntimeException("timed out")).when(worker).waitUntilBack(Mockito.any(), Mockito.anyLong());

        Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(calls.toString(), calls.contains("start:w1"));
        Assert.assertTrue(calls.stream().noneMatch(call -> call.startsWith("tokenDelete:")));
    }

    // SSH addresses

    private static final String SSH_IP = "203.0.113.10";
    private static final long SSH_IP_ID = 5L;

    private UserVmVO vmNamed(String name) {
        return vms.values().stream().filter(vm -> vm.getHostName().equals(name)).findFirst().get();
    }

    /** The worker resolves the SSH addresses for real, from the given tcp port forwarding rules to port 22 on the SSH IP */
    private KubernetesClusterRollingUpdateWorker newWorkerWithSshRules(Map<String, Integer> publicPorts) {
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);
        Mockito.doCallRealMethod().when(worker).resolveNodeSshAddresses();
        Mockito.doReturn(new Pair<>(SSH_IP, KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT)).when(worker).getKubernetesClusterServerIpSshPort(null);
        worker.ipAddressDao = ipAddressDao;
        // the field of the class that resolves them, which KubernetesClusterResourceModifierActionWorker hides
        ((KubernetesClusterActionWorker) worker).portForwardingRulesDao = portForwardingRulesDao;
        IPAddressVO ip = Mockito.mock(IPAddressVO.class);
        Mockito.when(ip.getId()).thenReturn(SSH_IP_ID);
        Mockito.when(ipAddressDao.findByIpAndDcId(Mockito.anyLong(), Mockito.eq(SSH_IP))).thenReturn(ip);
        List<PortForwardingRuleVO> rules = new ArrayList<>();
        publicPorts.forEach((name, port) -> {
            PortForwardingRuleVO rule = new PortForwardingRuleVO(null, SSH_IP_ID, port, new Ip("10.1.1.10"), 22, "tcp", 1L, 1L, 1L, vmNamed(name).getId());
            rule.setState(FirewallRule.State.Active);
            rules.add(rule);
        });
        Mockito.when(portForwardingRulesDao.listByIpAndNotRevoked(SSH_IP_ID)).thenReturn(rules);
        return worker;
    }

    @Test
    public void controlAndEtcdNodesAreReachedOnThePortOfTheirOwnRule() {
        addNode("e1", ETCD, OLD_OFFERING);
        addStackedControlPlane(2);
        addNode("w1", WORKER, OLD_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorkerWithSshRules(Map.of("e1", 2224, "c1", 2223, "c2", 2222));
        Mockito.doCallRealMethod().when(worker).resolveKubectlAddress(Mockito.anySet());

        worker.begin();

        Assert.assertEquals(Map.of(vmNamed("e1").getId(), new Pair<>(SSH_IP, 2224), vmNamed("c1").getId(), new Pair<>(SSH_IP, 2223),
                vmNamed("c2").getId(), new Pair<>(SSH_IP, 2222)), worker.nodeSshAddresses);
        worker.resolveKubectlAddress(Set.of());
        Assert.assertEquals(new Pair<>(SSH_IP, 2223), worker.kubectlAddress);
        worker.resolveKubectlAddress(Set.of(vmNamed("c1").getId()));
        Assert.assertEquals(new Pair<>(SSH_IP, 2222), worker.kubectlAddress);
    }

    @Test
    public void aNodeWithoutARuleFailsTheRolloutBeforeItChangesAnything() {
        addNode("e1", ETCD, OLD_OFFERING);
        addStackedControlPlane(2);
        addNode("w1", WORKER, OLD_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorkerWithSshRules(Map.of("e1", 2222, "c1", 2223));

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, worker::rollout);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("c2") && e.getMessage().contains(SSH_IP));
        Mockito.verify(worker, Mockito.never()).transitToOperationState(Mockito.any());
        Mockito.verify(worker, Mockito.never()).runKubectl(Mockito.anyString());
        Assert.assertEquals(List.of(), calls);
    }

    @Test
    public void aDirectAccessNodeWithoutAnAddressFailsTheRolloutBeforeItChangesAnything() {
        addStackedControlPlane(2);
        Mockito.when(vmNamed("c1").getPrivateIpAddress()).thenReturn("10.1.1.20");
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);
        Mockito.doCallRealMethod().when(worker).resolveNodeSshAddresses();
        Mockito.doReturn(new Pair<>("10.1.1.20", KubernetesClusterActionWorker.DEFAULT_SSH_PORT)).when(worker).getKubernetesClusterServerIpSshPort(null);

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, worker::begin);

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("node c2 has no IP address"));
        Mockito.verify(worker, Mockito.never()).transitToOperationState(Mockito.any());
    }

    @Test
    public void aMissingAddressIsNotReplacedByThePositionOfTheNode() {
        addStackedControlPlane(1);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);
        UserVmVO c1 = vmNamed("c1");

        CloudRuntimeException e = Assert.assertThrows(CloudRuntimeException.class, () -> worker.getSshAddress(c1));

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("c1"));
    }

    @Test
    public void etcdNodesBehindAnExternalLoadBalancerAreLeftOutAndReported() {
        for (int i = 1; i <= 3; i++) {
            addNode("e" + i, ETCD, OLD_OFFERING);
        }
        addStackedControlPlane(1);
        addNode("w1", WORKER, OLD_OFFERING);
        desiredOfferings.put(ETCD, NEW_OFFERING);
        KubernetesClusterRollingUpdateWorker worker = newWorker(1);
        Mockito.doCallRealMethod().when(worker).resolveNodeSshAddresses();
        Mockito.doReturn(new Pair<>("198.51.100.7", KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT)).when(worker).getKubernetesClusterServerIpSshPort(null);
        Mockito.when(manager.hasExternalLoadBalancer(CLUSTER_ID)).thenReturn(true);
        Mockito.when(detailsDao.findDetail(CLUSTER_ID, ApiConstants.EXTERNAL_LOAD_BALANCER_IP_ADDRESS))
                .thenReturn(new KubernetesClusterDetailsVO(CLUSTER_ID, ApiConstants.EXTERNAL_LOAD_BALANCER_IP_ADDRESS, "198.51.100.7", true));

        Assert.assertTrue(worker.rollout());

        Assert.assertEquals(Map.of(vmNamed("c1").getId(), new Pair<>("198.51.100.7", 2222)), worker.nodeSshAddresses);
        Assert.assertTrue(calls.contains("stop:w1"));
        Assert.assertFalse(calls.contains("stop:e1"));
        Mockito.verify(detailsDao).addDetail(Mockito.eq(CLUSTER_ID), Mockito.eq(KubernetesClusterManagerImpl.ROLLOUT_LAST_ERROR_DETAIL),
                Mockito.contains("the 3 etcd node(s) were not restarted"), Mockito.anyBoolean());
    }
}
