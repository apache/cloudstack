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

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.offering.ServiceOffering;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.ssh.SshHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class KubernetesClusterNodeCapacityReconcilerTest {
    private static final String CLUSTER_UUID = "cluster-uuid";
    private static final String PENDING = NodeCapacitySnapshot.PENDING_ANNOTATION;
    private static final String CORDON = NodeCapacitySnapshot.CORDON_ANNOTATION;
    private static final long MIB = 1024L * 1024L;
    private static final long GIB = 1024L * MIB;

    private final KubernetesCluster cluster = cluster();
    private final UserVm vm = vm("worker-1");
    private final NodeAccess access = new NodeAccess("control", 22, "node", 22, "root", null);
    private final KubernetesClusterNodeCapacityReconciler reconciler = new KubernetesClusterNodeCapacityReconciler();

    @Test
    public void testParseCpuAndMemoryQuantities() {
        Assert.assertEquals(3900L, KubernetesClusterNodeCapacityReconciler.parseCpuMillis("3900m"));
        Assert.assertEquals(4000L, KubernetesClusterNodeCapacityReconciler.parseCpuMillis("4"));
        Assert.assertEquals(123L, KubernetesClusterNodeCapacityReconciler.parseMemoryBytes("123"));
        Assert.assertEquals(4037048L * 1024L, KubernetesClusterNodeCapacityReconciler.parseMemoryBytes("4037048Ki"));
        Assert.assertEquals(2L * 1024L * 1024L, KubernetesClusterNodeCapacityReconciler.parseMemoryBytes("2Mi"));
        Assert.assertEquals(4L * GIB, KubernetesClusterNodeCapacityReconciler.parseMemoryBytes("4Gi"));
        Assert.assertEquals(2L * 1024L * GIB, KubernetesClusterNodeCapacityReconciler.parseMemoryBytes("2Ti"));
        Assert.assertEquals(2000L, KubernetesClusterNodeCapacityReconciler.parseMemoryBytes("2K"));
        Assert.assertEquals(2000000L, KubernetesClusterNodeCapacityReconciler.parseMemoryBytes("2M"));
        Assert.assertEquals(2000000000L, KubernetesClusterNodeCapacityReconciler.parseMemoryBytes("2G"));
        Assert.assertEquals(2000000000000L, KubernetesClusterNodeCapacityReconciler.parseMemoryBytes("2T"));
    }

    @Test
    public void testQuantityParsingRejectsMalformedAndOverflowingValues() {
        Assert.assertThrows(IllegalArgumentException.class, () -> KubernetesClusterNodeCapacityReconciler.parseMemoryBytes("4Gi; reboot"));
        Assert.assertThrows(IllegalArgumentException.class, () -> KubernetesClusterNodeCapacityReconciler.parseMemoryBytes("1.5Gi"));
        Assert.assertThrows(IllegalArgumentException.class, () -> KubernetesClusterNodeCapacityReconciler.parseMemoryBytes("-1"));
        Assert.assertThrows(IllegalArgumentException.class, () -> KubernetesClusterNodeCapacityReconciler.parseCpuMillis("1e3"));
        Assert.assertThrows(ArithmeticException.class, () -> KubernetesClusterNodeCapacityReconciler.parseMemoryBytes("99999999999999999Ti"));
        Assert.assertThrows(ArithmeticException.class, () -> KubernetesClusterNodeCapacityReconciler.parseCpuMillis("99999999999999999"));
    }

    @Test
    public void testParseNodeReadsIdentityMarkersSchedulingAndResources() {
        FakeNode node = new FakeNode();
        node.unschedulable = true;
        node.annotations.put(PENDING, CLUSTER_UUID);
        node.annotations.put(CORDON, CLUSTER_UUID);

        NodeCapacitySnapshot snapshot = KubernetesClusterNodeCapacityReconciler.parseNode(node.toJson());

        Assert.assertEquals("uid-1", snapshot.getUid());
        Assert.assertEquals("7", snapshot.getResourceVersion());
        Assert.assertTrue(snapshot.isUnschedulable());
        Assert.assertTrue(snapshot.isReady());
        Assert.assertTrue(snapshot.isPendingFor(CLUSTER_UUID));
        Assert.assertTrue(snapshot.isCordonOwnedBy(CLUSTER_UUID));
        Assert.assertFalse(snapshot.hasForeignMarker(CLUSTER_UUID));
        Assert.assertTrue(snapshot.hasForeignMarker("another-cluster"));
        Assert.assertEquals(2000L, snapshot.getCapacity().getCpuMillis());
        Assert.assertEquals(2L * GIB, snapshot.getCapacity().getMemoryBytes());
        Assert.assertEquals(1900L, snapshot.getAllocatable().getCpuMillis());
        Assert.assertEquals(1900L * 1024L * 1024L, snapshot.getAllocatable().getMemoryBytes());
    }

    @Test
    public void testParseNodeAcceptsMissingOptionalFields() {
        JsonObject node = JsonParser.parseString(new FakeNode().toJson()).getAsJsonObject();
        node.remove("spec");
        node.getAsJsonObject("metadata").remove("annotations");
        node.getAsJsonObject("status").remove("conditions");

        NodeCapacitySnapshot snapshot = KubernetesClusterNodeCapacityReconciler.parseNode(node.toString());

        Assert.assertFalse(snapshot.isUnschedulable());
        Assert.assertFalse(snapshot.isReady());
        Assert.assertFalse(snapshot.isPendingFor(CLUSTER_UUID));
        Assert.assertFalse(snapshot.hasForeignMarker(CLUSTER_UUID));
    }

    @Test
    public void testParseNodeRejectsMissingRequiredFields() {
        JsonObject withoutUid = JsonParser.parseString(new FakeNode().toJson()).getAsJsonObject();
        withoutUid.getAsJsonObject("metadata").remove("uid");
        JsonObject withoutMemory = JsonParser.parseString(new FakeNode().toJson()).getAsJsonObject();
        withoutMemory.getAsJsonObject("status").getAsJsonObject("capacity").remove("memory");

        String withoutUidJson = withoutUid.toString();
        String withoutMemoryJson = withoutMemory.toString();

        Assert.assertThrows(IllegalArgumentException.class, () -> KubernetesClusterNodeCapacityReconciler.parseNode(withoutUidJson));
        Assert.assertThrows(IllegalArgumentException.class, () -> KubernetesClusterNodeCapacityReconciler.parseNode(withoutMemoryJson));
        Assert.assertThrows(IllegalArgumentException.class, () -> KubernetesClusterNodeCapacityReconciler.parseNode("[]"));
    }

    @Test
    public void testMalformedKubectlOutputFailsWithPhaseAndVm() {
        FakeReconciler fake = new FakeReconciler();
        fake.nodeOutput = "Error from server: not json";

        assertPhaseFailure("READ_NODE", () -> fake.readNode(vm, access));
    }

    @Test
    public void testNodeNameIsLowercaseInKubectlCommand() {
        FakeReconciler fake = new FakeReconciler();

        fake.readNode(vm("Worker-1"), access);

        Assert.assertEquals("sudo /opt/bin/kubectl get node worker-1 -o json --request-timeout=20s", fake.controlCommands.get(0));
    }

    @Test
    public void testInvalidOrMissingHostNameFailsBeforeAnyCommand() {
        FakeReconciler fake = new FakeReconciler();
        UserVm invalidName = vm("worker; reboot");
        UserVm noName = vm(null);
        UserVm blankName = vm(" ");

        assertPhaseFailure("NODE_NAME", () -> fake.readNode(invalidName, access));
        assertPhaseFailure("NODE_NAME", () -> fake.readNode(noName, access));
        assertPhaseFailure("NODE_NAME", () -> fake.startResize(cluster, blankName, access, "uid-1"));

        Assert.assertTrue(fake.controlCommands.isEmpty());
    }

    @Test
    public void testReadGuestParsesCpusMemTotalAndOnlineMemoryBlocks() {
        FakeReconciler fake = new FakeReconciler();
        fake.guestOutput = "4\n4026532\n8000000\n32\n";

        GuestResources guest = fake.readGuest(vm, access);

        Assert.assertEquals(4L, guest.getOnlineCpus());
        Assert.assertEquals(4026532L * 1024L, guest.getMemoryBytes());
        Assert.assertEquals(128L * MIB, guest.getMemoryBlockBytes());
        Assert.assertEquals(4L * GIB, guest.getOnlineMemoryBytes());
        Assert.assertEquals(List.of(KubernetesClusterNodeCapacityReconciler.GUEST_RESOURCES_COMMAND), fake.nodeCommands);
    }

    @Test
    public void testMalformedGuestOutputFails() {
        FakeReconciler fake = new FakeReconciler();
        for (String output : List.of("4\n4194304", "0 4194304 8000000 32", "four 4194304 8000000 32", "4 4194304 8000000 32 1",
                "4 4194304 0 32", "4 4194304 0x8000000 32", "4 4194304 8000000 0")) {
            fake.guestOutput = output;
            assertPhaseFailure("READ_GUEST", () -> fake.readGuest(vm, access));
        }
    }

    @Test
    public void testStartCordonsSchedulableNodeAndRecordsOwnershipInOnePatch() {
        FakeReconciler fake = new FakeReconciler();

        NodeCapacitySnapshot started = fake.startResize(cluster, vm, access, "uid-1");

        Assert.assertEquals(List.of("sudo /opt/bin/kubectl patch node worker-1 --type merge --request-timeout=20s -p '{\"metadata\":"
                + "{\"resourceVersion\":\"7\",\"annotations\":{\"" + PENDING + "\":\"cluster-uuid\",\"" + CORDON + "\":\"cluster-uuid\"}},"
                + "\"spec\":{\"unschedulable\":true}}'"), fake.patches);
        Assert.assertTrue(started.isUnschedulable());
        Assert.assertTrue(started.isPendingFor(CLUSTER_UUID));
        Assert.assertTrue(started.isCordonOwnedBy(CLUSTER_UUID));
    }

    @Test
    public void testStartKeepsOperatorCordonAndAddsOnlyThePendingMarker() {
        FakeReconciler fake = new FakeReconciler();
        fake.node.unschedulable = true;

        NodeCapacitySnapshot started = fake.startResize(cluster, vm, access, "uid-1");

        Assert.assertEquals(1, fake.patches.size());
        Assert.assertFalse(fake.patches.get(0).contains("spec"));
        Assert.assertFalse(fake.patches.get(0).contains(CORDON + "\""));
        Assert.assertTrue(started.isUnschedulable());
        Assert.assertTrue(started.isPendingFor(CLUSTER_UUID));
        Assert.assertFalse(started.isCordonOwnedBy(CLUSTER_UUID));
    }

    @Test
    public void testStartDoesNothingWhenThePendingWorkIsAlreadyMarked() {
        FakeReconciler fake = new FakeReconciler();
        fake.node.unschedulable = true;
        fake.node.annotations.put(PENDING, CLUSTER_UUID);
        fake.node.annotations.put(CORDON, CLUSTER_UUID);

        NodeCapacitySnapshot started = fake.startResize(cluster, vm, access, "uid-1");

        Assert.assertTrue(fake.patches.isEmpty());
        Assert.assertTrue(started.isCordonOwnedBy(CLUSTER_UUID));
    }

    @Test
    public void testStartNeverChangesMarkersOfAnotherCluster() {
        FakeReconciler fake = new FakeReconciler();
        fake.node.annotations.put(PENDING, "another-cluster");

        assertPhaseFailure("START", () -> fake.startResize(cluster, vm, access, "uid-1"));

        Assert.assertTrue(fake.patches.isEmpty());
    }

    @Test
    public void testRejectedPatchFailsAfterBoundedAttempts() {
        FakeReconciler fake = new FakeReconciler();
        fake.failingPatches = 10;

        assertPhaseFailure("START", () -> fake.startResize(cluster, vm, access, "uid-1"));

        Assert.assertEquals(KubernetesClusterNodeCapacityReconciler.MAX_NODE_UPDATE_ATTEMPTS, fake.patches.size());
        Assert.assertFalse(fake.node.annotations.containsKey(PENDING));
        Assert.assertFalse(fake.node.unschedulable);
    }

    @Test
    public void testLostPatchReplyIsSettledByReadingTheNode() {
        FakeReconciler fake = new FakeReconciler();
        fake.loseNextReply = true;

        NodeCapacitySnapshot started = fake.startResize(cluster, vm, access, "uid-1");

        Assert.assertEquals(1, fake.patches.size());
        Assert.assertTrue(started.isPendingFor(CLUSTER_UUID));
        Assert.assertTrue(started.isUnschedulable());
    }

    @Test
    public void testConflictIsRetriedWithTheNewResourceVersion() {
        FakeReconciler fake = new FakeReconciler();
        fake.node.annotations.put(PENDING, CLUSTER_UUID);
        fake.node.annotations.put(CORDON, CLUSTER_UUID);
        fake.node.unschedulable = true;
        // kubelet writes the node status right after its restart
        fake.beforeNextPatch = () -> fake.node.resourceVersion++;

        NodeCapacitySnapshot finished = fake.finishResize(cluster, vm, access, "uid-1");

        Assert.assertEquals(2, fake.patches.size());
        Assert.assertTrue(fake.patches.get(0).contains("\"resourceVersion\":\"7\""));
        Assert.assertTrue(fake.patches.get(1).contains("\"resourceVersion\":\"8\""));
        Assert.assertFalse(finished.isUnschedulable());
        Assert.assertFalse(finished.isPendingFor(CLUSTER_UUID));
    }

    @Test
    public void testReplacedNodeStopsTheUpdate() {
        FakeReconciler fake = new FakeReconciler();

        assertPhaseFailure("FINISH", () -> fake.finishResize(cluster, vm, access, "uid-0"));

        Assert.assertTrue(fake.patches.isEmpty());
    }

    @Test
    public void testFinishRemovesMarkersAndUncordonsOnlyACksCordon() {
        FakeReconciler fake = new FakeReconciler();
        fake.node.unschedulable = true;
        fake.node.annotations.put(PENDING, CLUSTER_UUID);
        fake.node.annotations.put(CORDON, CLUSTER_UUID);
        fake.node.annotations.put("other", "kept");

        NodeCapacitySnapshot finished = fake.finishResize(cluster, vm, access, "uid-1");

        Assert.assertEquals(1, fake.patches.size());
        Assert.assertTrue(fake.patches.get(0).contains("\"spec\":{\"unschedulable\":false}"));
        Assert.assertFalse(finished.isUnschedulable());
        Assert.assertEquals(Map.of("other", "kept"), fake.node.annotations);
    }

    @Test
    public void testFinishKeepsAnOperatorCordon() {
        FakeReconciler fake = new FakeReconciler();
        fake.node.unschedulable = true;
        fake.node.annotations.put(PENDING, CLUSTER_UUID);

        NodeCapacitySnapshot finished = fake.finishResize(cluster, vm, access, "uid-1");

        Assert.assertFalse(fake.patches.get(0).contains("spec"));
        Assert.assertTrue(finished.isUnschedulable());
        Assert.assertFalse(finished.isPendingFor(CLUSTER_UUID));
    }

    @Test
    public void testFinishDoesNothingWithoutMarkersOfThisCluster() {
        FakeReconciler fake = new FakeReconciler();
        fake.node.annotations.put(PENDING, "another-cluster");

        fake.finishResize(cluster, vm, access, "uid-1");

        Assert.assertTrue(fake.patches.isEmpty());
        Assert.assertEquals("another-cluster", fake.node.annotations.get(PENDING));
    }

    @Test
    public void testReleaseCordonKeepsThePendingMarker() {
        FakeReconciler fake = new FakeReconciler();
        fake.node.unschedulable = true;
        fake.node.annotations.put(PENDING, CLUSTER_UUID);
        fake.node.annotations.put(CORDON, CLUSTER_UUID);

        NodeCapacitySnapshot released = fake.releaseCordon(cluster, vm, access, "uid-1");

        Assert.assertFalse(released.isUnschedulable());
        Assert.assertFalse(released.isCordonOwnedBy(CLUSTER_UUID));
        Assert.assertTrue(released.isPendingFor(CLUSTER_UUID));
    }

    @Test
    public void testOwnershipMarkerOnASchedulableNodeIsNotACksCordon() {
        FakeNode node = new FakeNode();
        node.annotations.put(CORDON, CLUSTER_UUID);

        NodeCapacitySnapshot snapshot = KubernetesClusterNodeCapacityReconciler.parseNode(node.toJson());

        Assert.assertTrue(snapshot.hasOwnershipMarker(CLUSTER_UUID));
        Assert.assertFalse(snapshot.isCordonOwnedBy(CLUSTER_UUID));
    }

    @Test
    public void testFinishRemovesALeftoverOwnershipMarkerWithoutTouchingScheduling() {
        FakeReconciler fake = new FakeReconciler();
        fake.node.annotations.put(PENDING, CLUSTER_UUID);
        fake.node.annotations.put(CORDON, CLUSTER_UUID);

        NodeCapacitySnapshot finished = fake.finishResize(cluster, vm, access, "uid-1");

        Assert.assertFalse(fake.patches.get(0).contains("spec"));
        Assert.assertFalse(finished.hasOwnershipMarker(CLUSTER_UUID));
        Assert.assertFalse(finished.isUnschedulable());
    }

    @Test
    public void testReleaseRemovesALeftoverOwnershipMarkerWithoutTouchingScheduling() {
        FakeReconciler fake = new FakeReconciler();
        fake.node.annotations.put(PENDING, CLUSTER_UUID);
        fake.node.annotations.put(CORDON, CLUSTER_UUID);

        NodeCapacitySnapshot released = fake.releaseCordon(cluster, vm, access, "uid-1");

        Assert.assertFalse(fake.patches.get(0).contains("spec"));
        Assert.assertFalse(released.hasOwnershipMarker(CLUSTER_UUID));
        Assert.assertTrue(released.isPendingFor(CLUSTER_UUID));
    }

    @Test
    public void testAlreadyResizedNeedsReadyNodeMatchingGuestAndAllocatableWithinCapacity() {
        GuestResources guest = guest(4, 4L * 1024L * 1024L, 32);

        Assert.assertTrue(reconciler.isKubernetesNodeAlreadyResized(snapshot(true, 4000, 4 * GIB, 3900, 3 * GIB), guest));
        // reservations may use up all of the capacity
        Assert.assertTrue(reconciler.isKubernetesNodeAlreadyResized(snapshot(true, 4000, 4 * GIB, 0, 0), guest));
        Assert.assertFalse(reconciler.isKubernetesNodeAlreadyResized(snapshot(false, 4000, 4 * GIB, 3900, 3 * GIB), guest));
        Assert.assertFalse(reconciler.isKubernetesNodeAlreadyResized(snapshot(true, 2000, 4 * GIB, 1900, 3 * GIB), guest));
        Assert.assertFalse(reconciler.isKubernetesNodeAlreadyResized(snapshot(true, 4000, 2 * GIB, 3900, GIB), guest));
        Assert.assertFalse(reconciler.isKubernetesNodeAlreadyResized(snapshot(true, 4000, 4 * GIB, 4100, 3 * GIB), guest));
        Assert.assertFalse(reconciler.isKubernetesNodeAlreadyResized(snapshot(true, 4000, 4 * GIB, 3900, 5 * GIB), guest));
    }

    @Test
    public void testGuestMemoryIsCountedInOnlineMemoryBlocks() {
        ServiceOffering target = offering(4, 4608);

        Assert.assertTrue(KubernetesClusterNodeCapacityReconciler.guestMatchesTarget(guest(4, 4568788L, 36), target));
        // 512 MiB of the hot-added memory is not online yet
        Assert.assertFalse(KubernetesClusterNodeCapacityReconciler.guestMatchesTarget(guest(4, 4044500L, 32), target));
        Assert.assertFalse(KubernetesClusterNodeCapacityReconciler.guestMatchesTarget(guest(2, 4568788L, 36), target));
        Assert.assertFalse(KubernetesClusterNodeCapacityReconciler.guestMatchesTarget(guest(4, 4699860L, 37), target));
        Assert.assertTrue(KubernetesClusterNodeCapacityReconciler.isGuestAboveTarget(guest(4, 4699860L, 37), target));
        Assert.assertTrue(KubernetesClusterNodeCapacityReconciler.isGuestAboveTarget(guest(6, 4568788L, 36), target));
        Assert.assertFalse(KubernetesClusterNodeCapacityReconciler.isGuestAboveTarget(guest(4, 4568788L, 36), target));
    }

    @Test
    public void testGuestMemoryCheckDoesNotDependOnTheMemoryTheKernelKeeps() {
        // An 8 GiB VM reports about 230 MiB less MemTotal than its RAM; its memory blocks still add up to 8 GiB.
        Assert.assertTrue(KubernetesClusterNodeCapacityReconciler.guestMatchesTarget(guest(4, 8152860L, 64), offering(4, 8192)));
        // RAM that is not a whole number of blocks ends in a partly filled block, counted as a whole one.
        Assert.assertTrue(KubernetesClusterNodeCapacityReconciler.guestMatchesTarget(guest(1, 950000L, 8), offering(1, 1000)));
        Assert.assertFalse(KubernetesClusterNodeCapacityReconciler.isGuestAboveTarget(guest(1, 950000L, 8), offering(1, 1000)));
    }

    @Test
    public void testGuestVerificationWaitsForHotpluggedResources() {
        FakeReconciler fake = new FakeReconciler();
        fake.guestOutputs.add("2\n2029380\n8000000\n16\n");
        fake.guestOutput = "4\n4026532\n8000000\n32\n";
        ServiceOffering target = offering(4, 4096);

        GuestResources guest = fake.waitForGuestResources(vm, target, access, 60000L);

        Assert.assertEquals(4L, guest.getOnlineCpus());
        Assert.assertEquals(List.of(KubernetesClusterNodeCapacityReconciler.POLL_INTERVAL_MS), fake.sleeps);
    }

    @Test
    public void testGuestAboveTheTargetFailsAtOnce() {
        FakeReconciler fake = new FakeReconciler();
        fake.guestOutput = "4\n8152860\n8000000\n64\n";
        ServiceOffering target = offering(4, 4096);

        assertPhaseFailure("GUEST_VERIFY", () -> fake.waitForGuestResources(vm, target, access, 60000L));

        Assert.assertTrue(fake.sleeps.isEmpty());
    }

    @Test
    public void testGuestVerificationStopsAtTheDeadline() {
        FakeReconciler fake = new FakeReconciler();
        ServiceOffering target = offering(4, 4096);

        assertPhaseFailure("GUEST_VERIFY", () -> fake.waitForGuestResources(vm, target, access, 12000L));

        Assert.assertEquals(List.of(5000L, 5000L, 2000L), fake.sleeps);
        Assert.assertEquals(4, fake.nodeCommands.size());
    }

    @Test
    public void testExpiredDeadlineAllowsOneObservationWithoutSleeping() {
        FakeReconciler fake = new FakeReconciler();
        fake.now = 1000L;
        GuestResources guest = guest(4, 4194304L, 32);

        assertPhaseFailure("CAPACITY_VERIFY", () -> fake.waitForKubernetesResources(vm, guest, access, 999L));

        Assert.assertEquals(1, fake.controlCommands.size());
        Assert.assertTrue(fake.sleeps.isEmpty());
    }

    @Test
    public void testWaitForKubernetesResourcesReturnsTheUpdatedNode() {
        FakeReconciler fake = new FakeReconciler();
        fake.node.capacityCpu = "4";
        fake.node.capacityMemory = "4Gi";
        fake.node.allocatableCpu = "3900m";

        NodeCapacitySnapshot node = fake.waitForKubernetesResources(vm, guest(4, 4194304L, 32), access, 60000L);

        Assert.assertEquals(4000L, node.getCapacity().getCpuMillis());
        Assert.assertTrue(fake.sleeps.isEmpty());
    }

    @Test
    public void testRestartKubeletWaitsUntilActive() {
        FakeReconciler fake = new FakeReconciler();
        fake.kubeletStates.add("activating");

        fake.restartKubelet(vm, access, 60000L);

        Assert.assertEquals(List.of(KubernetesClusterNodeCapacityReconciler.RESTART_KUBELET_COMMAND,
                KubernetesClusterNodeCapacityReconciler.KUBELET_STATE_COMMAND,
                KubernetesClusterNodeCapacityReconciler.KUBELET_STATE_COMMAND), fake.nodeCommands);
        Assert.assertEquals(1, fake.sleeps.size());
    }

    @Test
    public void testKubeletIsNotRestartedAfterTheDeadline() {
        FakeReconciler fake = new FakeReconciler();
        fake.now = 1000L;

        assertPhaseFailure("KUBELET_RESTART", () -> fake.restartKubelet(vm, access, 1000L));

        Assert.assertTrue(fake.nodeCommands.isEmpty());
    }

    @Test
    public void testInactiveKubeletIsReportedWithoutFailingTheCommand() {
        FakeReconciler fake = new FakeReconciler();
        fake.kubeletStates.add("inactive");

        Assert.assertFalse(fake.isKubeletActive(access));
        Assert.assertTrue(fake.isKubeletActive(access));
        Assert.assertTrue(KubernetesClusterNodeCapacityReconciler.KUBELET_STATE_COMMAND.endsWith("|| true"));
        Assert.assertFalse(KubernetesClusterNodeCapacityReconciler.KUBELET_STATE_COMMAND.contains("sudo"));
    }

    @Test
    public void testOnlineGuestResourcesActivatesHotpluggedCpuAndMemory() {
        FakeReconciler fake = new FakeReconciler();

        fake.onlineGuestResources(access);

        Assert.assertEquals(List.of(KubernetesClusterNodeCapacityReconciler.ONLINE_HOTPLUGGED_RESOURCES), fake.nodeCommands);
    }

    @Test
    public void testNodeAccessKeepsControlAndNodeEndpointsApart() {
        File key = new File("test-only-key");
        NodeAccess endpoints = new NodeAccess("control", 2222, "worker", 2223, "cloud", key);

        Assert.assertEquals("control", endpoints.getControlAddress());
        Assert.assertEquals(2222, endpoints.getControlPort());
        Assert.assertEquals("worker", endpoints.getNodeAddress());
        Assert.assertEquals(2223, endpoints.getNodePort());
        Assert.assertEquals("cloud", endpoints.getUser());
        Assert.assertSame(key, endpoints.getSshKeyFile());
    }

    @Test
    public void testSshSuccessReturnsOutput() {
        try (MockedStatic<SshHelper> ssh = Mockito.mockStatic(SshHelper.class)) {
            ssh.when(() -> SshHelper.sshExecute("node", 22, "root", null, null, "command", 10000, 10000, 30000))
                    .thenReturn(new Pair<>(true, "output"));
            Assert.assertEquals("output", reconciler.executeNode(access, "command"));
        }
    }

    @Test
    public void testFailedSshCommandReportsItsOutput() {
        try (MockedStatic<SshHelper> ssh = Mockito.mockStatic(SshHelper.class)) {
            ssh.when(() -> SshHelper.sshExecute("node", 22, "root", null, null, "command", 10000, 10000, 30000))
                    .thenReturn(new Pair<>(false, "remote error"));
            NodeCapacityReconciliationException error = Assert.assertThrows(NodeCapacityReconciliationException.class,
                    () -> reconciler.executeNode(access, "command"));
            Assert.assertTrue(error.getMessage().contains("remote error"));
        }
    }

    @Test
    public void testSshExceptionIsTranslatedAtTheTransportBoundary() {
        Exception failure = new Exception("SSH authentication failed");
        try (MockedStatic<SshHelper> ssh = Mockito.mockStatic(SshHelper.class)) {
            ssh.when(() -> SshHelper.sshExecute("node", 22, "root", null, null, "command", 10000, 10000, 30000))
                    .thenThrow(failure);
            NodeCapacityReconciliationException error = Assert.assertThrows(NodeCapacityReconciliationException.class,
                    () -> reconciler.executeNode(access, "command"));
            Assert.assertSame(failure, error.getCause());
        }
    }

    @Test
    public void testSshInterruptionKeepsTheInterruptFlag() {
        InterruptedException failure = new InterruptedException("cancelled");
        try (MockedStatic<SshHelper> ssh = Mockito.mockStatic(SshHelper.class)) {
            ssh.when(() -> SshHelper.sshExecute("node", 22, "root", null, null, "command", 10000, 10000, 30000))
                    .thenThrow(failure);
            NodeCapacityReconciliationException error = Assert.assertThrows(NodeCapacityReconciliationException.class,
                    () -> reconciler.executeNode(access, "command"));
            Assert.assertSame(failure, error.getCause());
            Assert.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void testSshProgrammingErrorsAreNotTranslated() {
        NullPointerException failure = new NullPointerException("programming error");
        try (MockedStatic<SshHelper> ssh = Mockito.mockStatic(SshHelper.class)) {
            ssh.when(() -> SshHelper.sshExecute("node", 22, "root", null, null, "command", 10000, 10000, 30000))
                    .thenThrow(failure);
            Assert.assertSame(failure, Assert.assertThrows(NullPointerException.class,
                    () -> reconciler.executeNode(access, "command")));
        }
    }

    @Test
    public void testInterruptedPollingKeepsTheInterruptFlag() {
        KubernetesClusterNodeCapacityReconciler inactiveKubelet = new KubernetesClusterNodeCapacityReconciler() {
            @Override
            protected String executeNode(NodeAccess nodeAccess, String command) {
                return "inactive";
            }
        };
        long deadline = System.currentTimeMillis() + 10000L;
        Thread.currentThread().interrupt();
        try {
            NodeCapacityReconciliationException error = Assert.assertThrows(NodeCapacityReconciliationException.class,
                    () -> inactiveKubelet.restartKubelet(vm, access, deadline));
            Assert.assertTrue(error.getCause() instanceof InterruptedException);
            Assert.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    private void assertPhaseFailure(String phase, ThrowingRunnable action) {
        NodeCapacityReconciliationException error = Assert.assertThrows(NodeCapacityReconciliationException.class, action);
        Assert.assertTrue(error.getMessage(), error.getMessage().contains(phase + " failed for VM vm-uuid"));
    }

    private static NodeCapacitySnapshot snapshot(boolean ready, long capacityCpuMillis, long capacityMemory,
                                                 long allocatableCpuMillis, long allocatableMemory) {
        return new NodeCapacitySnapshot("uid-1",
                "7",
                false,
                new HashMap<>(),
                ready,
                new NodeResources(capacityCpuMillis, capacityMemory),
                new NodeResources(allocatableCpuMillis, allocatableMemory));
    }

    private static GuestResources guest(long cpus, long memTotalKiB, long onlineMemoryBlocks) {
        return new GuestResources(cpus, memTotalKiB, onlineMemoryBlocks * 128L * MIB, 128L * MIB);
    }

    private static ServiceOffering offering(int cpu, int memory) {
        ServiceOffering offering = Mockito.mock(ServiceOffering.class);
        Mockito.when(offering.getCpu()).thenReturn(cpu);
        Mockito.when(offering.getRamSize()).thenReturn(memory);
        return offering;
    }

    private static KubernetesCluster cluster() {
        KubernetesCluster cluster = Mockito.mock(KubernetesCluster.class);
        Mockito.when(cluster.getUuid()).thenReturn(CLUSTER_UUID);
        return cluster;
    }

    private static UserVm vm(String hostname) {
        UserVm vm = Mockito.mock(UserVm.class);
        Mockito.when(vm.getHostName()).thenReturn(hostname);
        Mockito.when(vm.getUuid()).thenReturn("vm-uuid");
        return vm;
    }

    /** A Node as kubectl returns it, changed by the merge patches the reconciler sends. */
    private static class FakeNode {
        private final Map<String, String> annotations = new LinkedHashMap<>();
        private final String uid = "uid-1";
        private int resourceVersion = 7;
        private boolean unschedulable;
        private String capacityCpu = "2";
        private String capacityMemory = "2Gi";
        private String allocatableCpu = "1900m";
        private String allocatableMemory = "1900Mi";

        String toJson() {
            JsonObject metadata = new JsonObject();
            metadata.addProperty("name", "worker-1");
            metadata.addProperty("uid", uid);
            metadata.addProperty("resourceVersion", String.valueOf(resourceVersion));
            JsonObject annotationsJson = new JsonObject();
            annotations.forEach(annotationsJson::addProperty);
            metadata.add("annotations", annotationsJson);
            JsonObject spec = new JsonObject();
            if (unschedulable) {
                spec.addProperty("unschedulable", true);
            }
            JsonObject capacity = new JsonObject();
            capacity.addProperty("cpu", capacityCpu);
            capacity.addProperty("memory", capacityMemory);
            JsonObject allocatable = new JsonObject();
            allocatable.addProperty("cpu", allocatableCpu);
            allocatable.addProperty("memory", allocatableMemory);
            JsonObject readyCondition = new JsonObject();
            readyCondition.addProperty("type", "Ready");
            readyCondition.addProperty("status", "True");
            JsonArray conditions = new JsonArray();
            conditions.add(readyCondition);
            JsonObject status = new JsonObject();
            status.add("capacity", capacity);
            status.add("allocatable", allocatable);
            status.add("conditions", conditions);
            JsonObject node = new JsonObject();
            node.add("metadata", metadata);
            node.add("spec", spec);
            node.add("status", status);
            return node.toString();
        }
    }

    private static class FakeReconciler extends KubernetesClusterNodeCapacityReconciler {
        private final FakeNode node = new FakeNode();
        private final List<String> controlCommands = new ArrayList<>();
        private final List<String> nodeCommands = new ArrayList<>();
        private final List<String> patches = new ArrayList<>();
        private final List<Long> sleeps = new ArrayList<>();
        private final Deque<String> guestOutputs = new ArrayDeque<>();
        private final Deque<String> kubeletStates = new ArrayDeque<>();
        private String guestOutput = "2\n2029380\n8000000\n16\n";
        private String nodeOutput;
        private int failingPatches;
        private boolean loseNextReply;
        private Runnable beforeNextPatch;
        private long now;

        @Override
        protected String executeControl(NodeAccess nodeAccess, String command) {
            controlCommands.add(command);
            if (command.contains(" patch node ")) {
                return patch(command);
            }
            return nodeOutput != null ? nodeOutput : node.toJson();
        }

        @Override
        protected String executeNode(NodeAccess nodeAccess, String command) {
            nodeCommands.add(command);
            if (KubernetesClusterNodeCapacityReconciler.GUEST_RESOURCES_COMMAND.equals(command)) {
                return guestOutputs.isEmpty() ? guestOutput : guestOutputs.poll();
            }
            if (KubernetesClusterNodeCapacityReconciler.KUBELET_STATE_COMMAND.equals(command)) {
                return kubeletStates.isEmpty() ? "active\n" : kubeletStates.poll();
            }
            return "";
        }

        @Override
        protected long currentTimeMillis() {
            return now;
        }

        @Override
        protected void sleep(long millis) {
            sleeps.add(millis);
            now += millis;
        }

        private String patch(String command) {
            patches.add(command);
            if (beforeNextPatch != null) {
                Runnable change = beforeNextPatch;
                beforeNextPatch = null;
                change.run();
            }
            if (failingPatches > 0) {
                failingPatches--;
                throw new NodeCapacityReconciliationException("Error from server (Forbidden)");
            }
            JsonObject patch = JsonParser.parseString(command.substring(command.indexOf("-p '") + 4, command.length() - 1)).getAsJsonObject();
            JsonObject metadata = patch.getAsJsonObject("metadata");
            if (!String.valueOf(node.resourceVersion).equals(metadata.get("resourceVersion").getAsString())) {
                throw new NodeCapacityReconciliationException("Error from server (Conflict): the object has been modified");
            }
            for (Map.Entry<String, JsonElement> annotation : metadata.getAsJsonObject("annotations").entrySet()) {
                if (annotation.getValue().isJsonNull()) {
                    node.annotations.remove(annotation.getKey());
                } else {
                    node.annotations.put(annotation.getKey(), annotation.getValue().getAsString());
                }
            }
            if (patch.has("spec")) {
                node.unschedulable = patch.getAsJsonObject("spec").get("unschedulable").getAsBoolean();
            }
            node.resourceVersion++;
            if (loseNextReply) {
                loseNextReply = false;
                throw new NodeCapacityReconciliationException("Unable to run command: connection reset");
            }
            return "node/worker-1 patched";
        }
    }
}
