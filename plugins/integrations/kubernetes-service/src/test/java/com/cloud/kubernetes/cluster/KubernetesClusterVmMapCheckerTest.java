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

package com.cloud.kubernetes.cluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.alert.AlertManager;
import com.cloud.event.EventVO;
import com.cloud.kubernetes.cluster.actionworkers.KubernetesClusterVmMapCheckWorker;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.Silent.class)
public class KubernetesClusterVmMapCheckerTest {

    private static final long CLUSTER_ID = 1L;
    private static final long ACCOUNT_ID = 2L;
    private static final long OTHER_CLUSTER_ID = 3L;
    private static final Pattern HOST_NAME_PATTERN = Pattern.compile("^k8s-(?:control|node)-[0-9a-f]+$");

    @Mock
    private KubernetesClusterDao kubernetesClusterDao;
    @Mock
    private KubernetesClusterDetailsDao kubernetesClusterDetailsDao;
    @Mock
    private KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    @Mock
    private VMInstanceDao vmInstanceDao;
    @Mock
    private AlertManager alertManager;
    @Mock
    private KubernetesClusterVmMapCheckWorker worker;
    @Mock
    private KubernetesClusterVO cluster;

    private KubernetesClusterVmMapChecker checker;
    private final Map<String, String> details = new HashMap<>();
    private final List<KubernetesClusterVmMapVO> vmMaps = new ArrayList<>();
    private final Map<Long, VMInstanceVO> vms = new HashMap<>();
    private final List<VMInstanceVO> accountVms = new ArrayList<>();
    private final List<String> events = new ArrayList<>();
    private final List<String> alerts = new ArrayList<>();
    private List<String> nodeNames;
    private final Date now = new Date();

    @Before
    public void setUp() {
        checker = Mockito.spy(new KubernetesClusterVmMapChecker(null));
        checker.kubernetesClusterDao = kubernetesClusterDao;
        checker.kubernetesClusterDetailsDao = kubernetesClusterDetailsDao;
        checker.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        checker.vmInstanceDao = vmInstanceDao;
        checker.alertManager = alertManager;
        Mockito.doReturn(worker).when(checker).createWorker(ArgumentMatchers.any());
        Mockito.doAnswer(i -> events.add(i.getArgument(1) + " " + i.getArgument(2))).when(checker)
                .recordEvent(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        Mockito.doAnswer(i -> alerts.add(i.getArgument(1))).when(checker)
                .sendAlert(ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());

        Mockito.when(cluster.getId()).thenReturn(CLUSTER_ID);
        Mockito.when(cluster.getName()).thenReturn("k8s");
        Mockito.when(cluster.getAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(cluster.getZoneId()).thenReturn(1L);
        Mockito.when(cluster.getClusterType()).thenReturn(KubernetesCluster.ClusterType.CloudManaged);
        Mockito.when(cluster.getState()).thenReturn(KubernetesCluster.State.Running);
        Mockito.when(kubernetesClusterDao.findById(CLUSTER_ID)).thenReturn(cluster);

        Mockito.when(kubernetesClusterDetailsDao.findDetail(ArgumentMatchers.eq(CLUSTER_ID), ArgumentMatchers.anyString())).thenAnswer(i -> {
            String value = details.get(i.<String>getArgument(1));
            return value == null ? null : new KubernetesClusterDetailsVO(CLUSTER_ID, i.getArgument(1), value, false);
        });
        Mockito.doAnswer(i -> details.put(i.getArgument(1), i.getArgument(2))).when(kubernetesClusterDetailsDao)
                .addDetail(ArgumentMatchers.eq(CLUSTER_ID), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.eq(false));
        Mockito.doAnswer(i -> details.remove(i.<String>getArgument(1))).when(kubernetesClusterDetailsDao)
                .removeDetail(ArgumentMatchers.eq(CLUSTER_ID), ArgumentMatchers.anyString());

        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(CLUSTER_ID)).thenAnswer(i ->
                vmMaps.stream().filter(m -> m.getClusterId() == CLUSTER_ID).collect(Collectors.toList()));
        Mockito.when(kubernetesClusterVmMapDao.getClusterMapFromVmId(ArgumentMatchers.anyLong())).thenAnswer(i ->
                vmMaps.stream().filter(m -> m.getVmId() == i.<Long>getArgument(0)).findFirst().orElse(null));
        Mockito.when(vmInstanceDao.findByIdIncludingRemoved(ArgumentMatchers.anyLong())).thenAnswer(i -> vms.get(i.<Long>getArgument(0)));
        Mockito.when(vmInstanceDao.listByAccountId(ACCOUNT_ID)).thenReturn(accountVms);
        Mockito.when(worker.getClusterVmHostNamePattern()).thenReturn(HOST_NAME_PATTERN);
        Mockito.when(worker.listKubernetesNodeNames()).thenAnswer(i -> nodeNames);
    }

    private VMInstanceVO addVm(long id, String hostName, VirtualMachine.State state, boolean mapped, boolean etcd) {
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm.getId()).thenReturn(id);
        Mockito.when(vm.getUuid()).thenReturn("uuid-" + id);
        Mockito.when(vm.getHostName()).thenReturn(hostName);
        Mockito.when(vm.getState()).thenReturn(state);
        Mockito.when(vm.getType()).thenReturn(VirtualMachine.Type.User);
        Date longAgo = new Date(now.getTime() - TimeUnit.HOURS.toMillis(1));
        Mockito.when(vm.getCreated()).thenReturn(longAgo);
        Mockito.when(vm.getUpdateTime()).thenReturn(longAgo);
        vms.put(id, vm);
        if (!VirtualMachine.State.Expunging.equals(state)) {
            accountVms.add(vm);
        }
        if (mapped) {
            addVmMap(CLUSTER_ID, id, etcd);
        }
        return vm;
    }

    private void addVmMap(long clusterId, long vmId, boolean etcd) {
        KubernetesClusterVmMapVO vmMap = new KubernetesClusterVmMapVO(clusterId, vmId, false);
        vmMap.setEtcdNode(etcd);
        vmMaps.add(vmMap);
    }

    /** A healthy cluster: one control and two worker nodes, all of them Kubernetes nodes. */
    private void addHealthyCluster() {
        addVm(10L, "k8s-control-1a", VirtualMachine.State.Running, true, false);
        addVm(11L, "k8s-node-2b", VirtualMachine.State.Running, true, false);
        addVm(12L, "k8s-node-3c", VirtualMachine.State.Running, true, false);
        nodeNames = new ArrayList<>(Arrays.asList("k8s-control-1a", "k8s-node-2b", "k8s-node-3c"));
    }

    private void check() {
        events.clear();
        alerts.clear();
        checker.checkCluster(CLUSTER_ID, now);
    }

    private Set<String> reportedKeys(String detail) {
        return KubernetesClusterVmMapChecker.parseKeys(details.get(detail)).stream().collect(Collectors.toCollection(TreeSet::new));
    }

    @Test
    public void pluginDaosAreTakenFromTheManager() {
        KubernetesClusterManagerImpl manager = Mockito.mock(KubernetesClusterManagerImpl.class);
        manager.kubernetesClusterDao = kubernetesClusterDao;
        manager.kubernetesClusterDetailsDao = kubernetesClusterDetailsDao;
        manager.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        KubernetesClusterVmMapChecker fromManager = new KubernetesClusterVmMapChecker(manager);
        Assert.assertSame(kubernetesClusterDao, fromManager.kubernetesClusterDao);
        Assert.assertSame(kubernetesClusterDetailsDao, fromManager.kubernetesClusterDetailsDao);
        Assert.assertSame(kubernetesClusterVmMapDao, fromManager.kubernetesClusterVmMapDao);
    }

    @Test
    public void healthyClusterReportsNothing() {
        addHealthyCluster();
        check();
        Assert.assertTrue(events.isEmpty());
        Assert.assertTrue(alerts.isEmpty());
        Assert.assertTrue(details.isEmpty());
    }

    @Test
    public void mappedVmThatDoesNotExistIsReported() {
        addHealthyCluster();
        addVmMap(CLUSTER_ID, 99L, false);
        check();
        Assert.assertEquals(1, alerts.size());
        Assert.assertTrue(alerts.get(0).contains("VM with ID 99") && alerts.get(0).contains("k8s"));
        Assert.assertEquals(1, events.size());
        Assert.assertTrue(events.get(0).startsWith(EventVO.LEVEL_WARN));
        Assert.assertEquals(Set.of("MAPPED_VM:99"), reportedKeys(KubernetesClusterVmMapChecker.REPORTED_VM_DRIFT_DETAIL));
    }

    @Test
    public void mappedVmThatIsDestroyedIsReported() {
        addHealthyCluster();
        addVm(13L, "k8s-node-4d", VirtualMachine.State.Destroyed, true, false);
        check();
        Assert.assertEquals(1, alerts.size());
        Assert.assertTrue(alerts.get(0).contains("k8s-node-4d") && alerts.get(0).contains("destroyed"));
        Assert.assertEquals(Set.of("MAPPED_VM:13"), reportedKeys(KubernetesClusterVmMapChecker.REPORTED_VM_DRIFT_DETAIL));
    }

    @Test
    public void mappedVmThatIsExpungedIsReported() {
        addHealthyCluster();
        VMInstanceVO vm = addVm(13L, "k8s-node-4d", VirtualMachine.State.Expunging, true, false);
        Mockito.when(vm.getRemoved()).thenReturn(now);
        check();
        Assert.assertEquals(1, alerts.size());
        Assert.assertTrue(alerts.get(0).contains("k8s-node-4d") && alerts.get(0).contains("expunged"));
        Assert.assertEquals(Set.of("MAPPED_VM:13"), reportedKeys(KubernetesClusterVmMapChecker.REPORTED_VM_DRIFT_DETAIL));
    }

    @Test
    public void mappedVmInErrorStateIsReported() {
        addHealthyCluster();
        addVm(13L, "k8s-node-4d", VirtualMachine.State.Error, true, false);
        check();
        Assert.assertEquals(1, alerts.size());
        Assert.assertTrue(alerts.get(0).contains("k8s-node-4d") && alerts.get(0).contains("Error"));
        Assert.assertEquals(Set.of("MAPPED_VM:13"), reportedKeys(KubernetesClusterVmMapChecker.REPORTED_VM_DRIFT_DETAIL));
    }

    @Test
    public void vmMappedTwiceIsReportedOnce() {
        addHealthyCluster();
        addVmMap(CLUSTER_ID, 99L, false);
        addVmMap(CLUSTER_ID, 99L, false);
        check();
        Assert.assertEquals(1, alerts.size());
        Assert.assertEquals(1, events.size());
    }

    @Test
    public void unmappedVmNamedAsANodeOfTheClusterIsReported() {
        addHealthyCluster();
        addVm(13L, "k8s-node-4d", VirtualMachine.State.Running, false, false);
        nodeNames.add("k8s-node-4d");
        // not reported: names that do not match, VMs on their way out and VMs of another cluster
        addVm(14L, "k8s-node-other", VirtualMachine.State.Running, false, false);
        addVm(15L, "k8s-node-5e", VirtualMachine.State.Destroyed, false, false);
        addVm(16L, "k8s-node-6f", VirtualMachine.State.Error, false, false);
        addVm(17L, "k8s-node-7a", VirtualMachine.State.Running, false, false);
        addVmMap(OTHER_CLUSTER_ID, 17L, false);
        nodeNames.add("k8s-node-7a");
        VMInstanceVO router = addVm(18L, "k8s-node-8b", VirtualMachine.State.Running, false, false);
        Mockito.when(router.getType()).thenReturn(VirtualMachine.Type.DomainRouter);
        check();
        Assert.assertEquals(Set.of("UNMAPPED_VM:13"), reportedKeys(KubernetesClusterVmMapChecker.REPORTED_VM_DRIFT_DETAIL));
        // the node of the VM of the other cluster has no VM in this one
        Assert.assertEquals(Set.of("NODE_WITHOUT_VM:k8s-node-7a"), reportedKeys(KubernetesClusterVmMapChecker.REPORTED_NODE_DRIFT_DETAIL));
        Assert.assertEquals(2, alerts.size());
    }

    @Test
    public void unmappedVmJustCreatedIsNotReported() {
        addHealthyCluster();
        VMInstanceVO vm = addVm(13L, "k8s-node-4d", VirtualMachine.State.Running, false, false);
        Mockito.when(vm.getCreated()).thenReturn(new Date(now.getTime() - TimeUnit.MINUTES.toMillis(1)));
        // nor is its node, if it already joined
        nodeNames.add("k8s-node-4d");
        check();
        Assert.assertTrue(alerts.isEmpty());
        Assert.assertTrue(details.isEmpty());
    }

    @Test
    public void kubernetesNodeWithoutVmIsReported() {
        addHealthyCluster();
        nodeNames.add("k8s-node-0ld");
        check();
        Assert.assertEquals(1, alerts.size());
        Assert.assertTrue(alerts.get(0).contains("k8s-node-0ld"));
        Assert.assertTrue(details.get(KubernetesClusterVmMapChecker.REPORTED_VM_DRIFT_DETAIL) == null);
        Assert.assertEquals(Set.of("NODE_WITHOUT_VM:k8s-node-0ld"), reportedKeys(KubernetesClusterVmMapChecker.REPORTED_NODE_DRIFT_DETAIL));
    }

    @Test
    public void nodeOfAMappedVmThatIsDestroyedIsReportedOnce() {
        addHealthyCluster();
        addVm(13L, "k8s-node-4d", VirtualMachine.State.Destroyed, true, false);
        nodeNames.add("k8s-node-4d");
        check();
        Assert.assertEquals(1, alerts.size());
        Assert.assertEquals(Set.of("MAPPED_VM:13"), reportedKeys(KubernetesClusterVmMapChecker.REPORTED_VM_DRIFT_DETAIL));
        Assert.assertNull(details.get(KubernetesClusterVmMapChecker.REPORTED_NODE_DRIFT_DETAIL));
    }

    @Test
    public void nodesRegisteredUnderAFullyQualifiedNameMatchTheirVm() {
        addHealthyCluster();
        nodeNames.remove("k8s-node-3c");
        nodeNames.add("k8s-node-3c.example.com");
        check();
        Assert.assertTrue(alerts.isEmpty());
        Assert.assertTrue(details.isEmpty());
    }

    @Test
    public void allRunningVmsAreReportedWhenTheClusterHasNoNodes() {
        addHealthyCluster();
        nodeNames = new ArrayList<>();
        check();
        Assert.assertEquals(Set.of("VM_NOT_A_NODE:10", "VM_NOT_A_NODE:11", "VM_NOT_A_NODE:12"),
                reportedKeys(KubernetesClusterVmMapChecker.REPORTED_NODE_DRIFT_DETAIL));
    }

    @Test
    public void alertsAreCappedButEachMismatchGetsAnEvent() {
        addHealthyCluster();
        int count = KubernetesClusterVmMapChecker.MAX_ALERTS_PER_REPORT + 3;
        for (int i = 0; i < count; i++) {
            addVmMap(CLUSTER_ID, 100L + i, false);
        }
        check();
        Assert.assertEquals(count, events.size());
        Assert.assertEquals(KubernetesClusterVmMapChecker.MAX_ALERTS_PER_REPORT + 1, alerts.size());
        Assert.assertTrue(alerts.get(alerts.size() - 1).startsWith("3 more mismatches"));
        check();
        Assert.assertTrue(alerts.isEmpty());
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void clusterWhoseMapChangedDuringTheCheckIsNotReported() {
        addHealthyCluster();
        nodeNames = null;
        Mockito.when(worker.listKubernetesNodeNames()).thenAnswer(i -> {
            // e.g. a scale down that started and finished while kubectl ran
            vmMaps.removeIf(m -> m.getVmId() == 12L);
            return null;
        });
        check();
        Assert.assertTrue(alerts.isEmpty());
        Assert.assertTrue(details.isEmpty());
    }

    @Test
    public void nodeNamesAreMatchedIgnoringCase() {
        addHealthyCluster();
        addVm(13L, "K8S-Node-4D", VirtualMachine.State.Running, true, false);
        nodeNames.add("k8s-node-4d");
        check();
        Assert.assertTrue(alerts.isEmpty());
    }

    @Test
    public void runningMappedVmThatIsNotANodeIsReported() {
        addHealthyCluster();
        nodeNames.remove("k8s-node-3c");
        check();
        Assert.assertEquals(1, alerts.size());
        Assert.assertTrue(alerts.get(0).contains("k8s-node-3c"));
        Assert.assertEquals(Set.of("VM_NOT_A_NODE:12"), reportedKeys(KubernetesClusterVmMapChecker.REPORTED_NODE_DRIFT_DETAIL));
    }

    @Test
    public void mappedVmThatIsNotANodeIsNotReportedWithinTheGracePeriod() {
        addHealthyCluster();
        nodeNames.remove("k8s-node-3c");
        Mockito.when(vms.get(12L).getUpdateTime()).thenReturn(new Date(now.getTime() - TimeUnit.MINUTES.toMillis(5)));
        check();
        Assert.assertTrue(alerts.isEmpty());
        Assert.assertTrue(details.isEmpty());
    }

    @Test
    public void reportedVmThatIsNotANodeStaysReportedThroughTheGracePeriod() {
        addHealthyCluster();
        nodeNames.remove("k8s-node-3c");
        check();
        Assert.assertEquals(1, alerts.size());
        // e.g. migrated or restarted: still not a node, and neither cleared nor reported again
        Mockito.when(vms.get(12L).getUpdateTime()).thenReturn(new Date(now.getTime() - TimeUnit.MINUTES.toMillis(1)));
        check();
        Assert.assertTrue(alerts.isEmpty());
        Assert.assertTrue(events.isEmpty());
        Assert.assertEquals(Set.of("VM_NOT_A_NODE:12"), reportedKeys(KubernetesClusterVmMapChecker.REPORTED_NODE_DRIFT_DETAIL));
    }

    @Test
    public void mappedVmIsReportedOnceAsItGoesFromDestroyedToExpungedToGone() {
        addHealthyCluster();
        VMInstanceVO vm = addVm(13L, "k8s-node-4d", VirtualMachine.State.Destroyed, true, false);
        check();
        Assert.assertEquals(1, alerts.size());
        Mockito.when(vm.getState()).thenReturn(VirtualMachine.State.Expunging);
        Mockito.when(vm.getRemoved()).thenReturn(now);
        check();
        Assert.assertTrue(alerts.isEmpty());
        vms.remove(13L);
        check();
        Assert.assertTrue(alerts.isEmpty());
        Assert.assertTrue(events.isEmpty());
        Assert.assertEquals(Set.of("MAPPED_VM:13"), reportedKeys(KubernetesClusterVmMapChecker.REPORTED_VM_DRIFT_DETAIL));
    }

    @Test
    public void mappedVmThatIsNotANodeIsNotReportedWhenStoppedOrEtcd() {
        addHealthyCluster();
        addVm(13L, "k8s-node-4d", VirtualMachine.State.Stopped, true, false);
        addVm(14L, "k8s-etcd-0-5e", VirtualMachine.State.Running, true, true);
        check();
        Assert.assertTrue(alerts.isEmpty());
    }

    @Test
    public void sameDriftIsReportedOnce() {
        addHealthyCluster();
        addVmMap(CLUSTER_ID, 99L, false);
        nodeNames.add("k8s-node-0ld");
        check();
        Assert.assertEquals(2, alerts.size());
        Assert.assertEquals(2, events.size());
        check();
        check();
        Assert.assertTrue(alerts.isEmpty());
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void onlyNewDriftIsReportedWhenTheDriftChanges() {
        addHealthyCluster();
        addVmMap(CLUSTER_ID, 99L, false);
        check();
        Assert.assertEquals(1, alerts.size());
        addVmMap(CLUSTER_ID, 98L, false);
        check();
        Assert.assertEquals(1, alerts.size());
        Assert.assertTrue(alerts.get(0).contains("VM with ID 98"));
        Assert.assertEquals(Set.of("MAPPED_VM:98", "MAPPED_VM:99"),
                reportedKeys(KubernetesClusterVmMapChecker.REPORTED_VM_DRIFT_DETAIL));
        // one of them going away changes what is recorded but raises no alert
        vmMaps.removeIf(m -> m.getVmId() == 98L);
        check();
        Assert.assertTrue(alerts.isEmpty());
        Assert.assertEquals(Set.of("MAPPED_VM:99"), reportedKeys(KubernetesClusterVmMapChecker.REPORTED_VM_DRIFT_DETAIL));
        // and is reported again if it comes back
        addVmMap(CLUSTER_ID, 98L, false);
        check();
        Assert.assertEquals(1, alerts.size());
        Assert.assertTrue(alerts.get(0).contains("VM with ID 98"));
    }

    @Test
    public void resolvedDriftIsClearedAndReportedAgainIfItReturns() {
        addHealthyCluster();
        addVmMap(CLUSTER_ID, 99L, false);
        check();
        Assert.assertEquals(1, alerts.size());
        vmMaps.removeIf(m -> m.getVmId() == 99L);
        check();
        Assert.assertTrue(alerts.isEmpty());
        Assert.assertEquals(1, events.size());
        Assert.assertTrue(events.get(0).startsWith(EventVO.LEVEL_INFO));
        Assert.assertTrue(details.isEmpty());
        check();
        Assert.assertTrue(events.isEmpty());
        addVmMap(CLUSTER_ID, 99L, false);
        check();
        Assert.assertEquals(1, alerts.size());
    }

    @Test
    public void onlyTheVmComparisonRunsWhenTheNodesCannotBeListed() {
        addHealthyCluster();
        nodeNames.add("k8s-node-0ld");
        check();
        Assert.assertEquals(1, alerts.size());
        // SSH fails: the VMs are still compared, and what was found on the nodes is neither cleared nor reported again
        nodeNames = null;
        addVmMap(CLUSTER_ID, 99L, false);
        check();
        Assert.assertEquals(1, alerts.size());
        Assert.assertTrue(alerts.get(0).contains("VM with ID 99"));
        Assert.assertEquals(1, events.size());
        Assert.assertEquals(Set.of("NODE_WITHOUT_VM:k8s-node-0ld"), reportedKeys(KubernetesClusterVmMapChecker.REPORTED_NODE_DRIFT_DETAIL));
        // and is not reported again once SSH works again
        nodeNames = new ArrayList<>(Arrays.asList("k8s-control-1a", "k8s-node-2b", "k8s-node-3c", "k8s-node-0ld"));
        check();
        Assert.assertTrue(alerts.isEmpty());
    }

    @Test
    public void runningVmsAreNotReportedAsNotNodesWhenTheNodesCannotBeListed() {
        addHealthyCluster();
        nodeNames = null;
        check();
        Assert.assertTrue(alerts.isEmpty());
        Assert.assertTrue(details.isEmpty());
    }

    @Test
    public void clustersInOtherStatesAreNotChecked() {
        addHealthyCluster();
        addVmMap(CLUSTER_ID, 99L, false);
        for (KubernetesCluster.State state : KubernetesCluster.State.values()) {
            if (KubernetesCluster.State.Running.equals(state) || KubernetesCluster.State.Alert.equals(state)) {
                continue;
            }
            Mockito.when(cluster.getState()).thenReturn(state);
            check();
            Assert.assertTrue(state.toString(), alerts.isEmpty());
        }
        Mockito.verify(checker, Mockito.never()).createWorker(ArgumentMatchers.any());
        Mockito.when(cluster.getState()).thenReturn(KubernetesCluster.State.Alert);
        check();
        Assert.assertEquals(1, alerts.size());
    }

    @Test
    public void externalManagedClustersAreNotChecked() {
        addHealthyCluster();
        addVmMap(CLUSTER_ID, 99L, false);
        Mockito.when(cluster.getClusterType()).thenReturn(KubernetesCluster.ClusterType.ExternalManaged);
        check();
        Assert.assertTrue(alerts.isEmpty());
        Mockito.verify(checker, Mockito.never()).createWorker(ArgumentMatchers.any());
    }

    @Test
    public void clusterTakenByAnOperationDuringTheCheckIsNotReported() {
        addHealthyCluster();
        addVmMap(CLUSTER_ID, 99L, false);
        KubernetesClusterVO scaling = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(scaling.getClusterType()).thenReturn(KubernetesCluster.ClusterType.CloudManaged);
        Mockito.when(scaling.getState()).thenReturn(KubernetesCluster.State.Scaling);
        Mockito.when(kubernetesClusterDao.findById(CLUSTER_ID)).thenReturn(cluster, scaling);
        check();
        Assert.assertTrue(alerts.isEmpty());
        Assert.assertTrue(details.isEmpty());
    }

    @Test
    public void checkClustersChecksRunningAndAlertClustersOnly() {
        KubernetesClusterVO running = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(running.getId()).thenReturn(5L);
        KubernetesClusterVO alert = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(alert.getId()).thenReturn(6L);
        Mockito.when(kubernetesClusterDao.findManagedKubernetesClustersInState(ArgumentMatchers.any())).thenReturn(Collections.emptyList());
        Mockito.when(kubernetesClusterDao.findManagedKubernetesClustersInState(KubernetesCluster.State.Running)).thenReturn(List.of(running));
        // the running cluster moved to Alert between the two listings: checked once
        Mockito.when(kubernetesClusterDao.findManagedKubernetesClustersInState(KubernetesCluster.State.Alert)).thenReturn(List.of(running, alert));
        // a failure on one cluster does not stop the others being checked
        Mockito.doThrow(new RuntimeException("test")).when(checker).checkCluster(ArgumentMatchers.eq(5L), ArgumentMatchers.any());
        Mockito.doNothing().when(checker).checkCluster(ArgumentMatchers.eq(6L), ArgumentMatchers.any());
        checker.checkClusters(now);
        ArgumentCaptor<Long> ids = ArgumentCaptor.forClass(Long.class);
        Mockito.verify(checker, Mockito.times(2)).checkCluster(ids.capture(), ArgumentMatchers.any());
        Assert.assertEquals(List.of(5L, 6L), ids.getAllValues());
    }

    @Test
    public void checkClustersIfDueRunsOnlyWhenEnabledAndOncePerInterval() {
        Mockito.doNothing().when(checker).checkClustersLocked(ArgumentMatchers.any());
        Mockito.doAnswer(i -> {
            i.<Runnable>getArgument(0).run();
            return null;
        }).when(checker).runInBackground(ArgumentMatchers.any());
        Mockito.doReturn(false).when(checker).isCheckEnabled();
        checker.checkClustersIfDue();
        Mockito.verify(checker, Mockito.never()).runInBackground(ArgumentMatchers.any());
        Mockito.doReturn(true).when(checker).isCheckEnabled();
        checker.checkClustersIfDue();
        checker.checkClustersIfDue();
        Mockito.verify(checker, Mockito.times(1)).checkClustersLocked(ArgumentMatchers.any());
    }

    @Test
    public void tooManyKeysAreStoredAsAHash() {
        Set<String> keys = IntStream.range(0, 2000).mapToObj(i -> "MAPPED_VM:" + i).collect(Collectors.toCollection(TreeSet::new));
        String serialized = KubernetesClusterVmMapChecker.serializeKeys(keys);
        Assert.assertTrue(serialized.length() <= KubernetesClusterVmMapChecker.MAX_REPORTED_DRIFT_LENGTH);
        Assert.assertTrue(KubernetesClusterVmMapChecker.parseKeys(serialized).isEmpty());
        Assert.assertEquals(serialized, KubernetesClusterVmMapChecker.serializeKeys(new TreeSet<>(keys)));
        Set<String> few = new TreeSet<>(List.of("UNMAPPED_VM:1", "NODE_WITHOUT_VM:a"));
        Assert.assertEquals(few, new TreeSet<>(KubernetesClusterVmMapChecker.parseKeys(KubernetesClusterVmMapChecker.serializeKeys(few))));
    }
}
