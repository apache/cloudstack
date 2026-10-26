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
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SshHelper;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
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
        Assert.assertFalse(reconciler.requiresKubeletRefresh(null, cpuOffering, KubernetesClusterNodeType.WORKER, KubernetesCluster.State.Running));
        Assert.assertFalse(reconciler.requiresKubeletRefresh(oldOffering, null, KubernetesClusterNodeType.WORKER, KubernetesCluster.State.Running));
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

    @Test
    public void testCordonAddsOwnershipAnnotationBeforeCordoning() throws Exception {
        KubernetesCluster cluster = cluster("cluster-uuid");
        UserVm vm = vm("worker-1");
        KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot schedulable =
                new KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot(false, false, 0, 0, 0, 0, 0, 0, true);
        FakeReconciler fakeReconciler = new FakeReconciler(nodeJson(true));

        fakeReconciler.cordonIfNeeded(cluster, vm, schedulable, access(), System.currentTimeMillis() + 1000L);

        Assert.assertEquals("sudo /opt/bin/kubectl annotate node worker-1 cloudstack.apache.org/cks-live-resize=cluster-uuid --overwrite",
                fakeReconciler.controlCommands.get(0));
        Assert.assertEquals("sudo /opt/bin/kubectl cordon worker-1", fakeReconciler.controlCommands.get(1));
    }

    @Test
    public void testGuestVerificationAllowsObservedKvmHotplugOverhead() throws Exception {
        FakeReconciler fakeReconciler = new FakeReconciler(nodeJson(false));
        fakeReconciler.nodeResources = "4\n4568788\n";

        KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot before =
                new KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot(false, false, 2, 2097152L, 2000L, 2L * 1024L * 1024L * 1024L,
                        1900L, 1900L * 1024L * 1024L, true);

        fakeReconciler.verifyGuestResources(vm("worker-1"), offering(4, 4608), before, access(), System.currentTimeMillis() + 1000L);
    }

    @Test
    public void testOnlineGuestResourcesActivatesHotpluggedCpuAndMemory() throws Exception {
        FakeReconciler fakeReconciler = new FakeReconciler(nodeJson(false));

        fakeReconciler.onlineGuestResources(access());

        Assert.assertEquals(KubernetesClusterNodeCapacityReconcilerImpl.ONLINE_HOTPLUGGED_RESOURCES,
                fakeReconciler.nodeCommands.get(0));
    }

    @Test
    public void testParseMemoryDecimalBinaryAndUnitlessQuantities() {
        Assert.assertEquals(123L, KubernetesClusterNodeCapacityReconcilerImpl.parseMemoryBytes("123"));
        Assert.assertEquals(2L * 1024L * 1024L, KubernetesClusterNodeCapacityReconcilerImpl.parseMemoryBytes("2Mi"));
        Assert.assertEquals(2L * 1024L * 1024L * 1024L * 1024L, KubernetesClusterNodeCapacityReconcilerImpl.parseMemoryBytes("2Ti"));
        Assert.assertEquals(2000L, KubernetesClusterNodeCapacityReconcilerImpl.parseMemoryBytes("2K"));
        Assert.assertEquals(2000000L, KubernetesClusterNodeCapacityReconcilerImpl.parseMemoryBytes("2M"));
        Assert.assertEquals(2000000000L, KubernetesClusterNodeCapacityReconcilerImpl.parseMemoryBytes("2G"));
        Assert.assertEquals(2000000000000L, KubernetesClusterNodeCapacityReconcilerImpl.parseMemoryBytes("2T"));
        Assert.assertThrows(IllegalArgumentException.class, () -> KubernetesClusterNodeCapacityReconcilerImpl.parseMemoryBytes("4Gi; reboot"));
    }

    @Test
    public void testRestartKubeletWaitsForActiveService() throws Exception {
        FakeReconciler fake = new FakeReconciler(nodeJson(false));
        fake.nodeResources = " active\n";

        fake.restartKubelet(vm("worker-1"), access(), System.currentTimeMillis() + 1000L);

        Assert.assertEquals(List.of("sudo systemctl restart kubelet", "sudo systemctl is-active kubelet"), fake.nodeCommands);
    }

    @Test
    public void testExpiredDeadlinesReportTheFailedPhaseAndVm() {
        FakeReconciler fake = new FakeReconciler(nodeJson(false));
        KubernetesCluster cluster = cluster("cluster-uuid");
        UserVm vm = vm("worker-1");
        KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot before = snapshot(2, 2048, true);
        long expired = System.currentTimeMillis() - 1L;

        assertPhaseFailure("CORDON", () -> fake.cordonIfNeeded(cluster, vm, before, access(), expired));
        assertPhaseFailure("GUEST_VERIFY", () -> fake.verifyGuestResources(vm, offering(4, 4096), before, access(), expired));
        assertPhaseFailure("KUBELET_RESTART", () -> fake.restartKubelet(vm, access(), expired));
        assertPhaseFailure("CAPACITY_VERIFY", () -> fake.waitForKubernetesResources(cluster, vm, offering(4, 4096), before, access(), expired));
        assertPhaseFailure("RESTORE_SCHEDULABILITY", () -> fake.restoreSchedulability(cluster, vm, before, access(), expired));
    }

    @Test
    public void testWaitForKubernetesResourcesRequiresIncreasedAllocatableCpuAndMemory() throws Exception {
        FakeReconciler fake = new FakeReconciler(nodeJson(false).replace("3900m", "4"));
        fake.nodeResources = "4\n4194304\n";

        KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot observed = fake.waitForKubernetesResources(
                cluster("cluster-uuid"), vm("worker-1"), offering(4, 4096), snapshot(2, 2048, true),
                access(), System.currentTimeMillis() + 1000L);

        Assert.assertTrue(observed.isReady());
        Assert.assertEquals(4000L, observed.getCapacityCpuMillis());
        Assert.assertEquals(3700L, observed.getAllocatableCpuMillis());
        Assert.assertEquals(3900L * 1024L * 1024L, observed.getAllocatableMemoryBytes());
    }

    @Test(timeout = 15000L)
    public void testWaitRejectsUnchangedAllocatableCpuAfterCpuIncrease() {
        FakeReconciler fake = new FakeReconciler(nodeJson(false).replace("3900m", "4").replace("3700m", "1900m"));
        fake.nodeResources = "4\n4194304\n";

        assertPhaseFailure("CAPACITY_VERIFY", () -> fake.waitForKubernetesResources(cluster("cluster-uuid"), vm("worker-1"),
                offering(4, 4096), snapshot(2, 2048, true), access(), System.currentTimeMillis() + 1000L));

        Assert.assertFalse(fake.controlCommands.isEmpty());
        Assert.assertTrue(fake.controlCommands.stream().noneMatch(command -> command.contains("uncordon")));
    }

    @Test(timeout = 15000L)
    public void testWaitRejectsStaleAllocatableMemoryAfterMemoryIncrease() {
        FakeReconciler fake = new FakeReconciler(nodeJson(false).replace("3900m", "4").replace("3900Mi", "1900Mi"));
        fake.nodeResources = "4\n4194304\n";

        assertPhaseFailure("CAPACITY_VERIFY", () -> fake.waitForKubernetesResources(cluster("cluster-uuid"), vm("worker-1"),
                offering(4, 4096), snapshot(2, 2048, true), access(), System.currentTimeMillis() + 1000L));

        Assert.assertFalse(fake.controlCommands.isEmpty());
    }

    @Test(timeout = 15000L)
    public void testRestartKubeletDoesNotTreatInactiveServiceAsSuccess() {
        FakeReconciler fake = new FakeReconciler(nodeJson(false));
        fake.nodeResources = "inactive\n";

        assertPhaseFailure("KUBELET_RESTART", () -> fake.restartKubelet(vm("worker-1"), access(), System.currentTimeMillis() + 1000L));

        Assert.assertEquals(List.of("sudo systemctl restart kubelet", "sudo systemctl is-active kubelet"), fake.nodeCommands);
    }

    @Test
    public void testResourceVerificationRejectsStaleNotReadyOrOutOfToleranceNodes() {
        ServiceOffering target = offering(4, 4096);
        Assert.assertFalse(reconciler.isKubernetesResourcesCurrent(snapshot(4, 4096, false), target));
        Assert.assertFalse(reconciler.isKubernetesResourcesCurrent(snapshot(2, 4096, true), target));
        Assert.assertFalse(reconciler.isKubernetesResourcesCurrent(snapshot(4, 4097, true), target));
        Assert.assertFalse(reconciler.isKubernetesResourcesCurrent(snapshot(4, 3800, true), target));
        KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot staleMemory =
                new KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot(false, false, 4, 4194304L,
                        4000L, 2L * 1024L * 1024L * 1024L, 3700L, 1900L * 1024L * 1024L, true);
        Assert.assertFalse(reconciler.isKubernetesResourcesCurrent(staleMemory, target));
    }

    @Test
    public void testCaptureRecognizesOnlyThisClustersCordonOwnership() throws Exception {
        String owned = nodeJson(true).replace("\"annotations\":{}",
                "\"annotations\":{\"cloudstack.apache.org/cks-live-resize\":\"cluster-uuid\"}");
        FakeReconciler fake = new FakeReconciler(owned);
        Assert.assertTrue(fake.captureBefore(cluster("cluster-uuid"), vm("worker-1"), access()).isCloudStackResizeCordon());
        Assert.assertFalse(fake.captureBefore(cluster("another-cluster"), vm("worker-1"), access()).isCloudStackResizeCordon());
    }

    @Test
    public void testCaptureHandlesMissingOptionalFieldsAndNotReadyNode() throws Exception {
        String minimal = nodeJson(false).replace("\"annotations\":{}", "\"name\":\"worker-1\"")
                .replace("\"spec\":{\"unschedulable\":false},", "")
                .replace("\"status\":\"True\"", "\"status\":\"False\"");
        FakeReconciler fake = new FakeReconciler(minimal);
        KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot observed =
                fake.captureBefore(cluster("cluster-uuid"), vm("worker-1"), access());
        Assert.assertFalse(observed.isReady());
        Assert.assertFalse(observed.isUnschedulable());
        Assert.assertFalse(observed.isCloudStackResizeCordon());
    }

    @Test
    public void testInvalidNodeNameIsRejectedBeforeControlCommand() {
        FakeReconciler fake = new FakeReconciler(nodeJson(false));
        Assert.assertThrows(CloudRuntimeException.class,
                () -> fake.captureBefore(cluster("cluster-uuid"), vm("worker; reboot"), access()));
        Assert.assertTrue(fake.controlCommands.isEmpty());
    }

    @Test
    public void testNodeNameIsLowercaseInKubectlCommand() throws Exception {
        FakeReconciler fake = new FakeReconciler(nodeJson(false));
        fake.captureBefore(cluster("cluster-uuid"), vm("Worker-1"), access());
        Assert.assertEquals("sudo /opt/bin/kubectl get node worker-1 -o json", fake.controlCommands.get(0));
    }

    @Test
    public void testMalformedGuestResourceOutputStopsBeforeKubectl() {
        FakeReconciler fake = new FakeReconciler(nodeJson(false));
        fake.nodeResources = "4\n";
        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class,
                () -> fake.captureBefore(cluster("cluster-uuid"), vm("worker-1"), access()));
        Assert.assertEquals("Unable to read guest CPU and memory", error.getMessage());
        Assert.assertTrue(fake.controlCommands.isEmpty());
    }

    @Test
    public void testNodeAccessPreservesSeparateControlAndWorkerEndpoints() {
        java.io.File key = new java.io.File("test-only-key");
        KubernetesClusterNodeCapacityReconciler.NodeAccess access =
                new KubernetesClusterNodeCapacityReconciler.NodeAccess("control", 2222, "worker", 2223, "cloud", key);
        Assert.assertEquals("control", access.getControlAddress());
        Assert.assertEquals(2222, access.getControlPort());
        Assert.assertEquals("worker", access.getNodeAddress());
        Assert.assertEquals(2223, access.getNodePort());
        Assert.assertEquals("cloud", access.getUser());
        Assert.assertSame(key, access.getSshKeyFile());
    }

    @Test
    public void testSshSuccessReturnsOutput() {
        KubernetesClusterNodeCapacityReconcilerImpl real = new KubernetesClusterNodeCapacityReconcilerImpl();
        KubernetesClusterNodeCapacityReconciler.NodeAccess access = access();
        try (MockedStatic<SshHelper> ssh = Mockito.mockStatic(SshHelper.class)) {
            ssh.when(() -> SshHelper.sshExecute("node", 22, "root", null, null, "command", 10000, 10000, 30000))
                    .thenReturn(new Pair<>(true, "output"));
            Assert.assertEquals("output", real.executeNode(access, "command"));
        }
    }

    @Test
    public void testRejectedSshCommandHasSpecificReconciliationFailure() {
        KubernetesClusterNodeCapacityReconcilerImpl real = new KubernetesClusterNodeCapacityReconcilerImpl();
        KubernetesClusterNodeCapacityReconciler.NodeAccess access = access();
        try (MockedStatic<SshHelper> ssh = Mockito.mockStatic(SshHelper.class)) {
            ssh.when(() -> SshHelper.sshExecute("node", 22, "root", null, null, "command", 10000, 10000, 30000))
                    .thenReturn(new Pair<>(false, "remote error"));
            Assert.assertThrows(NodeCapacityReconciliationException.class, () -> real.executeNode(access, "command"));
        }
    }

    @Test
    public void testLegacySshExceptionIsTranslatedAtTransportBoundary() {
        KubernetesClusterNodeCapacityReconcilerImpl real = new KubernetesClusterNodeCapacityReconcilerImpl();
        KubernetesClusterNodeCapacityReconciler.NodeAccess access = access();
        Exception failure = new Exception("SSH authentication failed");
        try (MockedStatic<SshHelper> ssh = Mockito.mockStatic(SshHelper.class)) {
            ssh.when(() -> SshHelper.sshExecute("node", 22, "root", null, null, "command", 10000, 10000, 30000))
                    .thenThrow(failure);
            NodeCapacityReconciliationException error = Assert.assertThrows(NodeCapacityReconciliationException.class,
                    () -> real.executeNode(access, "command"));
            Assert.assertSame(failure, error.getCause());
        }
    }

    @Test
    public void testSshInterruptionPreservesInterruptFlag() {
        KubernetesClusterNodeCapacityReconcilerImpl real = new KubernetesClusterNodeCapacityReconcilerImpl();
        KubernetesClusterNodeCapacityReconciler.NodeAccess access = access();
        InterruptedException failure = new InterruptedException("cancelled");
        try (MockedStatic<SshHelper> ssh = Mockito.mockStatic(SshHelper.class)) {
            ssh.when(() -> SshHelper.sshExecute("node", 22, "root", null, null, "command", 10000, 10000, 30000))
                    .thenThrow(failure);
            NodeCapacityReconciliationException error = Assert.assertThrows(NodeCapacityReconciliationException.class,
                    () -> real.executeNode(access, "command"));
            Assert.assertSame(failure, error.getCause());
            Assert.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void testSshProgrammingErrorsAreNotTranslated() {
        KubernetesClusterNodeCapacityReconcilerImpl real = new KubernetesClusterNodeCapacityReconcilerImpl();
        KubernetesClusterNodeCapacityReconciler.NodeAccess access = access();
        NullPointerException failure = new NullPointerException("programming error");
        try (MockedStatic<SshHelper> ssh = Mockito.mockStatic(SshHelper.class)) {
            ssh.when(() -> SshHelper.sshExecute("node", 22, "root", null, null, "command", 10000, 10000, 30000))
                    .thenThrow(failure);
            Assert.assertSame(failure, Assert.assertThrows(NullPointerException.class, () -> real.executeNode(access, "command")));
        }
    }

    @Test
    public void testPollingInterruptionPreservesInterruptFlag() {
        FakeReconciler fake = new FakeReconciler(nodeJson(false));
        UserVm vm = vm("worker-1");
        KubernetesClusterNodeCapacityReconciler.NodeAccess access = access();
        long deadline = System.currentTimeMillis() + 10000L;
        fake.nodeResources = "inactive";
        Thread.currentThread().interrupt();
        try {
            NodeCapacityReconciliationException error = Assert.assertThrows(NodeCapacityReconciliationException.class,
                    () -> fake.restartKubelet(vm, access, deadline));
            Assert.assertTrue(error.getCause() instanceof InterruptedException);
            Assert.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    private KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot snapshot(int cpu, int memoryMiB, boolean ready) {
        long bytes = memoryMiB * 1024L * 1024L;
        return new KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot(false, false, cpu, memoryMiB * 1024L,
                cpu * 1000L, bytes, cpu * 1000L - 100L, bytes - 128L * 1024L * 1024L, ready);
    }

    private void assertPhaseFailure(String phase, org.junit.function.ThrowingRunnable action) {
        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, action);
        Assert.assertTrue(error.getMessage(), error.getMessage().contains(phase + " failed for VM vm-uuid"));
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
        private final List<String> nodeCommands = new ArrayList<>();
        private String nodeResources = "2\n2097152\n";

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
            nodeCommands.add(command);
            return nodeResources;
        }
    }
}
