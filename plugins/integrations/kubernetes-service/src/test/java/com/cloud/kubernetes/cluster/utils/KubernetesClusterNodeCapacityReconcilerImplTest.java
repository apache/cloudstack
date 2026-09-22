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
package com.cloud.kubernetes.cluster.utils;

import java.util.ArrayList;
import java.util.List;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType;
import com.cloud.offering.ServiceOffering;
import com.cloud.uservm.UserVm;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class KubernetesClusterNodeCapacityReconcilerImplTest {
    private final KubernetesClusterNodeCapacityReconciler reconciler = new KubernetesClusterNodeCapacityReconcilerImpl();

    @Test
    public void testParseKubernetesQuantities() {
        Assert.assertEquals(3900L, KubernetesClusterNodeCapacityReconcilerImpl.parseCpuMillis("3900m"));
        Assert.assertEquals(4000L, KubernetesClusterNodeCapacityReconcilerImpl.parseCpuMillis("4"));
        Assert.assertEquals(4037048L * 1024L, KubernetesClusterNodeCapacityReconcilerImpl.parseMemoryBytes("4037048Ki"));
        Assert.assertEquals(4L * 1024L * 1024L * 1024L, KubernetesClusterNodeCapacityReconcilerImpl.parseMemoryBytes("4Gi"));
    }

    @Test
    public void testOnlyRunningKubernetesNodesWithChangedCpuOrRamRequireRefresh() {
        ServiceOffering oldOffering = offering(2, 2048);
        ServiceOffering cpuOffering = offering(4, 2048);
        ServiceOffering ramOffering = offering(2, 4096);
        ServiceOffering capOnlyOffering = offering(2, 2048);

        Assert.assertTrue(reconciler.requiresKubeletRefresh(oldOffering, cpuOffering, KubernetesClusterNodeType.WORKER, KubernetesCluster.State.Running));
        Assert.assertTrue(reconciler.requiresKubeletRefresh(oldOffering, ramOffering, KubernetesClusterNodeType.CONTROL, KubernetesCluster.State.Running));
        Assert.assertFalse(reconciler.requiresKubeletRefresh(oldOffering, capOnlyOffering, KubernetesClusterNodeType.WORKER, KubernetesCluster.State.Running));
        Assert.assertFalse(reconciler.requiresKubeletRefresh(oldOffering, cpuOffering, KubernetesClusterNodeType.ETCD, KubernetesCluster.State.Running));
        Assert.assertFalse(reconciler.requiresKubeletRefresh(oldOffering, cpuOffering, KubernetesClusterNodeType.WORKER, KubernetesCluster.State.Stopped));
    }

    @Test
    public void testCurrentKubernetesResourcesDoNotRequireASecondKubeletRestart() {
        long memoryKiB = 4L * 1024L * 1024L;
        KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot current =
                new KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot(false, false, 4, memoryKiB,
                        4000, memoryKiB * 1024L, 3900, memoryKiB * 1024L - 128L * 1024L * 1024L, true);
        KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot stale =
                new KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot(false, false, 4, memoryKiB,
                        2000, memoryKiB * 1024L, 1900, memoryKiB * 1024L - 128L * 1024L * 1024L, true);

        Assert.assertTrue(reconciler.isKubernetesResourcesCurrent(current, offering(4, 4096)));
        Assert.assertFalse(reconciler.isKubernetesResourcesCurrent(stale, offering(4, 4096)));
    }

    @Test
    public void testCaptureBeforeReadsStructuredNodeState() throws Exception {
        FakeReconciler fakeReconciler = new FakeReconciler(nodeJson(false));
        KubernetesCluster cluster = cluster("cluster-uuid");
        UserVm vm = vm("worker-1");

        KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot snapshot = fakeReconciler.captureBefore(cluster, vm, access());

        Assert.assertFalse(snapshot.isUnschedulable());
        Assert.assertTrue(snapshot.isReady());
        Assert.assertEquals(2L, snapshot.getGuestOnlineCpuCount());
        Assert.assertEquals(2097152L, snapshot.getGuestMemoryKiB());
        Assert.assertEquals(3900L, snapshot.getCapacityCpuMillis());
        Assert.assertEquals(4L * 1024L * 1024L * 1024L, snapshot.getCapacityMemoryBytes());
    }

    @Test
    public void testCordonAndRestoreOnlyNodesOwnedByCks() throws Exception {
        KubernetesCluster cluster = cluster("cluster-uuid");
        UserVm vm = vm("worker-1");
        KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot previouslyCordon =
                new KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot(true, false, 0, 0, 0, 0, 0, 0, true);
        FakeReconciler operatorCordon = new FakeReconciler(nodeJson(true));

        operatorCordon.cordonIfNeeded(cluster, vm, previouslyCordon, access(), System.currentTimeMillis() + 1000L);
        operatorCordon.restoreSchedulability(cluster, vm, previouslyCordon, access(), System.currentTimeMillis() + 1000L);
        Assert.assertTrue(operatorCordon.controlCommands.isEmpty());

        KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot cksCordon =
                new KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot(true, true, 0, 0, 0, 0, 0, 0, true);
        FakeReconciler cleanup = new FakeReconciler(nodeJson(false));
        cleanup.restoreSchedulability(cluster, vm, cksCordon, access(), System.currentTimeMillis() + 1000L);

        Assert.assertEquals("sudo /opt/bin/kubectl uncordon worker-1", cleanup.controlCommands.get(0));
        Assert.assertEquals("sudo /opt/bin/kubectl annotate node worker-1 cloudstack.apache.org/cks-live-resize-", cleanup.controlCommands.get(1));
    }

    private ServiceOffering offering(int cpu, int memory) {
        ServiceOffering offering = Mockito.mock(ServiceOffering.class);
        Mockito.when(offering.getCpu()).thenReturn(cpu);
        Mockito.when(offering.getRamSize()).thenReturn(memory);
        return offering;
    }

    private KubernetesCluster cluster(String uuid) {
        KubernetesCluster cluster = Mockito.mock(KubernetesCluster.class);
        Mockito.when(cluster.getUuid()).thenReturn(uuid);
        return cluster;
    }

    private UserVm vm(String hostname) {
        UserVm vm = Mockito.mock(UserVm.class);
        Mockito.when(vm.getHostName()).thenReturn(hostname);
        Mockito.when(vm.getUuid()).thenReturn("vm-uuid");
        return vm;
    }

    private KubernetesClusterNodeCapacityReconciler.NodeAccess access() {
        return new KubernetesClusterNodeCapacityReconciler.NodeAccess("control", 22, "node", 22, "root", null);
    }

    private String nodeJson(boolean unschedulable) {
        return String.format("{\"metadata\":{\"annotations\":{}},\"spec\":{\"unschedulable\":%s},\"status\":{\"capacity\":{\"cpu\":\"3900m\",\"memory\":\"4Gi\"},\"allocatable\":{\"cpu\":\"3700m\",\"memory\":\"3900Mi\"},\"conditions\":[{\"type\":\"Ready\",\"status\":\"True\"}]}}", unschedulable);
    }

    private static class FakeReconciler extends KubernetesClusterNodeCapacityReconcilerImpl {
        private final String nodeJson;
        private final List<String> controlCommands = new ArrayList<>();

        FakeReconciler(String nodeJson) {
            this.nodeJson = nodeJson;
        }

        @Override
        protected String executeControl(KubernetesClusterNodeCapacityReconciler.NodeAccess access, String command) {
            controlCommands.add(command);
            return command.contains(" get node ") ? nodeJson : "";
        }

        @Override
        protected String executeNode(KubernetesClusterNodeCapacityReconciler.NodeAccess access, String command) {
            return "2\n2097152\n";
        }
    }
}
