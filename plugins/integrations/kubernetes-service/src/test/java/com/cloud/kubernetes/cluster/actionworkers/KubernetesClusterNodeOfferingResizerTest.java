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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType;
import com.cloud.kubernetes.cluster.utils.GuestResources;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterNodeCapacityReconciler;
import com.cloud.kubernetes.cluster.utils.NodeAccess;
import com.cloud.kubernetes.cluster.utils.NodeCapacityReconciliationException;
import com.cloud.kubernetes.cluster.utils.NodeCapacitySnapshot;
import com.cloud.kubernetes.cluster.utils.NodeResources;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.ETCD;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.WORKER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

public class KubernetesClusterNodeOfferingResizerTest {
    private static final String CLUSTER_UUID = "cluster-uuid";
    private static final long DEADLINE = 600000L;
    private static final long MIB = 1024L * 1024L;

    private final KubernetesCluster cluster = Mockito.mock(KubernetesCluster.class);
    private final UserVmDao vmDao = Mockito.mock(UserVmDao.class);
    private final ServiceOfferingDao offeringDao = Mockito.mock(ServiceOfferingDao.class);
    private final UserVmManager vmManager = Mockito.mock(UserVmManager.class);
    private final KubernetesClusterNodeCapacityReconciler reconciler = Mockito.mock(KubernetesClusterNodeCapacityReconciler.class);
    private final NodeAccess access = new NodeAccess("control", 22, "node", 2222, "cloud", null);
    private final ServiceOfferingVO current = offering(10L, 2, 2048);
    private final ServiceOfferingVO target = offering(20L, 4, 4096);
    private final UserVmVO vm = vm(10L);
    private final UserVmVO resizedVm = vm(20L);
    private final KubernetesClusterVmMapVO vmMap = Mockito.mock(KubernetesClusterVmMapVO.class);
    private final GuestResources resizedGuest = guest(4, 4026532L, 4096);
    private boolean clusterRunning = true;
    private int accessResolutions;
    private int liveResizeAttempts;

    @Before
    public void setUp() throws Exception {
        Mockito.when(cluster.getUuid()).thenReturn(CLUSTER_UUID);
        Mockito.when(cluster.getName()).thenReturn("test-cluster");
        Mockito.when(cluster.getClusterType()).thenReturn(KubernetesCluster.ClusterType.CloudManaged);
        Mockito.when(vmMap.getVmId()).thenReturn(1L);
        Mockito.when(vmDao.findById(1L)).thenReturn(vm);
        Mockito.when(offeringDao.findByIdIncludingRemoved(10L)).thenReturn(current);
        Mockito.when(vmManager.upgradeVirtualMachine(eq(1L), eq(20L), any())).thenAnswer(invocation -> {
            Mockito.when(vmDao.findById(1L)).thenReturn(resizedVm);
            return true;
        });
        acceptLiveResize();
        Mockito.when(reconciler.readNode(any(), eq(access))).thenReturn(node(false, null, null));
        Mockito.when(reconciler.readGuest(vm, access)).thenReturn(guest(2, 2029380L, 2048));
        Mockito.when(reconciler.startResize(cluster, vm, access, "uid-1")).thenReturn(node(true, CLUSTER_UUID, CLUSTER_UUID));
        Mockito.when(reconciler.waitForGuestResources(any(), eq(target), eq(access), eq(DEADLINE))).thenReturn(resizedGuest);
        Mockito.when(reconciler.isKubeletActive(access)).thenReturn(true);
        Mockito.when(reconciler.finishResize(eq(cluster), any(), eq(access), eq("uid-1"))).thenReturn(node(false, null, null));
    }

    @Test
    public void testResizesAndReconcilesAWorkerNode() throws Exception {
        resize(WORKER);

        InOrder order = Mockito.inOrder(reconciler, vmManager);
        order.verify(reconciler).readNode(vm, access);
        order.verify(reconciler).readGuest(vm, access);
        order.verify(vmManager).upgradeVirtualMachine(eq(1L), eq(20L), any(), any(Runnable.class));
        order.verify(reconciler).startResize(cluster, vm, access, "uid-1");
        Assert.assertEquals(1, liveResizeAttempts);
        order.verify(reconciler).onlineGuestResources(access);
        order.verify(reconciler).waitForGuestResources(resizedVm, target, access, DEADLINE);
        order.verify(reconciler).restartKubelet(resizedVm, access, DEADLINE);
        order.verify(reconciler).waitForKubernetesResources(resizedVm, resizedGuest, access, DEADLINE);
        order.verify(reconciler).finishResize(cluster, resizedVm, access, "uid-1");
        Mockito.verify(reconciler, Mockito.never()).releaseCordon(any(), any(), any(), any());
        Mockito.verify(offeringDao, Mockito.never()).findById(anyLong());
    }

    @Test
    public void testUpToDateNodeWithActiveKubeletIsNotRestarted() {
        Mockito.when(reconciler.isKubernetesNodeAlreadyResized(any(), eq(resizedGuest))).thenReturn(true);

        resize(WORKER);

        Mockito.verify(reconciler, Mockito.never()).restartKubelet(any(), any(), anyLong());
        Mockito.verify(reconciler).finishResize(cluster, resizedVm, access, "uid-1");
    }

    @Test
    public void testInactiveKubeletIsRestartedEvenIfTheNodeLooksUpToDate() {
        Mockito.when(reconciler.isKubernetesNodeAlreadyResized(any(), eq(resizedGuest))).thenReturn(true);
        Mockito.when(reconciler.isKubeletActive(access)).thenReturn(false);

        resize(WORKER);

        Mockito.verify(reconciler).restartKubelet(resizedVm, access, DEADLINE);
        Mockito.verify(reconciler).finishResize(cluster, resizedVm, access, "uid-1");
    }

    @Test
    public void testDedicatedEtcdVmGetsThePlainResize() throws Exception {
        assertPlainResize(ETCD);
    }

    @Test
    public void testExternalNodeGetsThePlainResize() throws Exception {
        Mockito.when(vmMap.isExternalNode()).thenReturn(true);
        assertPlainResize(WORKER);
    }

    @Test
    public void testExternallyManagedClusterGetsThePlainResize() throws Exception {
        Mockito.when(cluster.getClusterType()).thenReturn(KubernetesCluster.ClusterType.ExternalManaged);
        assertPlainResize(WORKER);
    }

    @Test
    public void testNonKvmVmGetsThePlainResize() throws Exception {
        Mockito.when(vm.getHypervisorType()).thenReturn(HypervisorType.VMware);
        assertPlainResize(WORKER);
    }

    @Test
    public void testClusterThatWasNotRunningGetsThePlainResize() throws Exception {
        clusterRunning = false;
        assertPlainResize(WORKER);
    }

    @Test
    public void testOfferingWithTheSameCpuAndRamGetsThePlainResizeAndOneNodeRead() throws Exception {
        ServiceOfferingVO sameCapacity = offering(10L, 4, 4096);
        Mockito.when(offeringDao.findByIdIncludingRemoved(10L)).thenReturn(sameCapacity);

        resize(WORKER);

        Mockito.verify(vmManager).upgradeVirtualMachine(eq(1L), eq(20L), any());
        Mockito.verify(reconciler).readNode(vm, access);
        Mockito.verifyNoMoreInteractions(reconciler);
    }

    @Test
    public void testOfferingWithTheSameCpuAndRamFinishesWorkLeftByAnEarlierResize() throws Exception {
        ServiceOfferingVO sameCapacity = offering(10L, 4, 4096);
        Mockito.when(offeringDao.findByIdIncludingRemoved(10L)).thenReturn(sameCapacity);
        Mockito.when(reconciler.readNode(vm, access)).thenReturn(node(true, CLUSTER_UUID, CLUSTER_UUID));

        resize(WORKER);

        InOrder order = Mockito.inOrder(vmManager, reconciler);
        order.verify(vmManager).upgradeVirtualMachine(eq(1L), eq(20L), any());
        order.verify(reconciler).waitForGuestResources(vm, target, access, DEADLINE);
        order.verify(reconciler).finishResize(cluster, vm, access, "uid-1");
        Mockito.verify(reconciler, Mockito.never()).startResize(any(), any(), any(), any());
    }

    @Test
    public void testPlainResizeFailureKeepsTheCause() throws Exception {
        ResourceUnavailableException failure = new ResourceUnavailableException("host unavailable", UserVmVO.class, 1L);
        Mockito.doThrow(failure).when(vmManager).upgradeVirtualMachine(eq(1L), eq(20L), any());

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, () -> resize(ETCD));

        Assert.assertSame(failure, error.getCause());
        Assert.assertTrue(error.getMessage(), error.getMessage().contains("unable to scale ETCD node VM worker-1"));
    }

    @Test
    public void testPlainResizeRejectedByTheVmApiFails() throws Exception {
        Mockito.doReturn(false).when(vmManager).upgradeVirtualMachine(eq(1L), eq(20L), any());

        Assert.assertThrows(CloudRuntimeException.class, () -> resize(ETCD));
    }

    @Test
    public void testVmOnTargetWithoutMarkersIsSkippedWithoutGuestAccess() {
        Mockito.when(vmDao.findById(1L)).thenReturn(resizedVm);

        resize(WORKER);

        Mockito.verify(reconciler).readNode(resizedVm, access);
        Mockito.verifyNoMoreInteractions(reconciler);
        Mockito.verifyNoInteractions(vmManager);
    }

    @Test
    public void testVmOnTargetWithPendingWorkIsFinishedBeforeAnythingElse() {
        Mockito.when(vmDao.findById(1L)).thenReturn(resizedVm);
        Mockito.when(reconciler.readNode(resizedVm, access)).thenReturn(node(true, CLUSTER_UUID, null));

        resize(WORKER);

        Mockito.verify(reconciler).onlineGuestResources(access);
        Mockito.verify(reconciler).waitForGuestResources(resizedVm, target, access, DEADLINE);
        Mockito.verify(reconciler).finishResize(cluster, resizedVm, access, "uid-1");
        Mockito.verify(reconciler, Mockito.never()).startResize(any(), any(), any(), any());
        Mockito.verifyNoInteractions(vmManager);
    }

    @Test
    public void testLeftoverOwnershipMarkerOnASchedulableNodeIsCleanedUp() {
        Mockito.when(vmDao.findById(1L)).thenReturn(resizedVm);
        Mockito.when(reconciler.readNode(resizedVm, access)).thenReturn(node(false, null, CLUSTER_UUID));

        resize(WORKER);

        Mockito.verify(reconciler).finishResize(cluster, resizedVm, access, "uid-1");
    }

    @Test
    public void testOwnershipMarkerWithoutPendingMarkerIsStillFinished() {
        Mockito.when(vmDao.findById(1L)).thenReturn(resizedVm);
        Mockito.when(reconciler.readNode(resizedVm, access)).thenReturn(node(true, null, CLUSTER_UUID));

        resize(WORKER);

        Mockito.verify(reconciler).finishResize(cluster, resizedVm, access, "uid-1");
    }

    @Test
    public void testUnreadableNodeOfAVmOnTargetFailsInsteadOfBeingSkipped() {
        Mockito.when(vmDao.findById(1L)).thenReturn(resizedVm);
        NodeCapacityReconciliationException failure = new NodeCapacityReconciliationException("control plane unreachable");
        Mockito.when(reconciler.readNode(resizedVm, access)).thenThrow(failure);

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));

        Assert.assertSame(failure, error.getCause());
        Assert.assertTrue(error.getMessage(), error.getMessage().contains("retry the same scale operation"));
        Mockito.verify(reconciler, Mockito.never()).readGuest(any(), any());
    }

    @Test
    public void testMarkersOfAnotherClusterAreNotTreatedAsDone() {
        Mockito.when(vmDao.findById(1L)).thenReturn(resizedVm);
        Mockito.when(reconciler.readNode(resizedVm, access)).thenReturn(node(true, "another-cluster", null));

        Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));

        Mockito.verify(reconciler, Mockito.never()).finishResize(any(), any(), any(), any());
    }

    @Test
    public void testVmOnTargetThatIsNotAKubernetesNodeIsLeftAlone() {
        Mockito.when(vmDao.findById(1L)).thenReturn(resizedVm);

        resize(ETCD);

        Mockito.verifyNoInteractions(reconciler, vmManager);
    }

    @Test
    public void testGuestAboveTheTargetIsRejectedBeforeAnyChange() {
        Mockito.when(reconciler.readGuest(vm, access)).thenReturn(guest(4, 8152860L, 8192));

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));

        Assert.assertTrue(error.getMessage(), error.getMessage().contains("more than the requested offering"));
        Assert.assertTrue(error.getMessage(), error.getMessage().contains("The VM was not resized and its Kubernetes node was not changed"));
        Mockito.verify(reconciler, Mockito.never()).startResize(any(), any(), any(), any());
        Mockito.verifyNoInteractions(vmManager);
    }

    @Test
    public void testGuestReadFailureStopsBeforeAnyChange() {
        NodeCapacityReconciliationException failure = new NodeCapacityReconciliationException("SSH unavailable");
        Mockito.when(reconciler.readGuest(vm, access)).thenThrow(failure);

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));

        Assert.assertSame(failure, error.getCause());
        Mockito.verify(reconciler, Mockito.never()).startResize(any(), any(), any(), any());
        Mockito.verifyNoInteractions(vmManager);
    }

    @Test
    public void testStartFailureStopsBeforeTheVmResize() {
        NodeCapacityReconciliationException failure = new NodeCapacityReconciliationException("patch failed");
        Mockito.when(reconciler.startResize(cluster, vm, access, "uid-1")).thenThrow(failure);

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));

        Assert.assertSame(failure, error.getCause());
        Assert.assertTrue(error.getMessage(), error.getMessage().contains("resize did not start"));
        Assert.assertEquals(0, liveResizeAttempts);
        Mockito.verify(reconciler, Mockito.never()).releaseCordon(any(), any(), any(), any());
    }

    @Test
    public void testResourceUnavailableReleasesTheCksCordonAndKeepsPendingWork() throws Exception {
        assertCordonReleasedAfter(new ResourceUnavailableException("host unavailable", UserVmVO.class, 1L));
    }

    @Test
    public void testManagementFailureReleasesTheCksCordonAndKeepsPendingWork() throws Exception {
        assertCordonReleasedAfter(new ManagementServerException("upgrade failed"));
    }

    @Test
    public void testMigrationFailureReleasesTheCksCordonAndKeepsPendingWork() throws Exception {
        assertCordonReleasedAfter(new VirtualMachineMigrationException("migration failed"));
    }

    @Test
    public void testUnexpectedErrorReleasesTheCksCordonAndKeepsItsCause() throws Exception {
        assertCordonReleasedAfter(new NullPointerException("programming error"));
    }

    @Test
    public void testCapacityRejectionBeforePlacementLeavesTheOperatorCordonAndNoPendingMarker() throws Exception {
        Mockito.when(reconciler.readNode(vm, access)).thenReturn(node(true, null, null));
        Mockito.doReturn(false).when(vmManager).upgradeVirtualMachine(eq(1L), eq(20L), any(), any(Runnable.class));

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));
        Assert.assertTrue(error.getMessage(), error.getMessage().contains("resize did not start"));
        resizer().reconcilePending(vmMap, WORKER, current, DEADLINE);

        Mockito.verify(reconciler, Mockito.times(2)).readNode(vm, access);
        Mockito.verify(reconciler).readGuest(vm, access);
        Mockito.verifyNoMoreInteractions(reconciler);
        Assert.assertEquals(0, liveResizeAttempts);
    }

    @Test
    public void testPlacementRejectionPreservesPendingWorkFromAnEarlierAttempt() throws Exception {
        Mockito.when(reconciler.readNode(vm, access)).thenReturn(node(true, CLUSTER_UUID, CLUSTER_UUID));
        Mockito.doReturn(false).when(vmManager).upgradeVirtualMachine(eq(1L), eq(20L), any(), any(Runnable.class));

        Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));

        Mockito.verify(reconciler).readNode(vm, access);
        Mockito.verify(reconciler).readGuest(vm, access);
        Mockito.verifyNoMoreInteractions(reconciler);
    }

    @Test
    public void testRetryOfCommittedOfferingFinishesLegacyPendingOnOperatorCordonedNode() {
        Mockito.when(reconciler.readNode(resizedVm, access)).thenReturn(node(true, CLUSTER_UUID, null));
        Mockito.when(vmDao.findById(1L)).thenReturn(resizedVm);

        resizer().reconcilePending(vmMap, WORKER, target, DEADLINE);

        Mockito.verify(reconciler).waitForGuestResources(resizedVm, target, access, DEADLINE);
        Mockito.verify(reconciler).finishResize(cluster, resizedVm, access, "uid-1");
        Mockito.verify(reconciler, Mockito.never()).releaseCordon(any(), any(), any(), any());
        Mockito.verifyNoInteractions(vmManager);
    }

    @Test
    public void testRetryDoesNotResizeAMixedOfferingVmOrClearItsUnverifiedMarker() {
        resizer().reconcilePending(vmMap, WORKER, target, DEADLINE);
        Mockito.when(reconciler.readNode(vm, access)).thenReturn(node(true, CLUSTER_UUID, null));

        Assert.assertThrows(CloudRuntimeException.class, () -> resizer().reconcilePending(vmMap, WORKER, target, DEADLINE));

        Mockito.verify(reconciler, Mockito.times(2)).readNode(vm, access);
        Mockito.verifyNoMoreInteractions(reconciler);
        Mockito.verifyNoInteractions(vmManager);
    }

    @Test
    public void testUncertainVmResizeReleasesTheCksCordon() throws Exception {
        afterPlacement(() -> false);

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));

        Assert.assertTrue(error.getMessage(), error.getMessage().contains("may have been partly resized"));
        Mockito.verify(reconciler).releaseCordon(cluster, vm, access, "uid-1");
    }

    @Test
    public void testFailureAfterTheVmReachedTheTargetKeepsPendingWorkAndCordon() throws Exception {
        ManagementServerException failure = new ManagementServerException("agent answer lost");
        afterPlacement(() -> {
            Mockito.when(vmDao.findById(1L)).thenReturn(resizedVm);
            throw failure;
        });

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));

        Assert.assertSame(failure, error.getCause());
        Assert.assertTrue(error.getMessage(), error.getMessage().contains("stays marked for reconciliation and cordoned"));
        Mockito.verify(reconciler, Mockito.never()).releaseCordon(any(), any(), any(), any());
        Mockito.verify(reconciler, Mockito.never()).finishResize(any(), any(), any(), any());
    }

    @Test
    public void testReconciliationFailureAfterTheResizeKeepsPendingWorkAndCordon() {
        NodeCapacityReconciliationException failure = new NodeCapacityReconciliationException("guest verification failed");
        Mockito.when(reconciler.waitForGuestResources(resizedVm, target, access, DEADLINE)).thenThrow(failure);

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));

        Assert.assertSame(failure, error.getCause());
        Assert.assertTrue(error.getMessage(), error.getMessage().contains("stays marked for reconciliation and cordoned"));
        Mockito.verify(reconciler, Mockito.never()).releaseCordon(any(), any(), any(), any());
        Mockito.verify(reconciler, Mockito.never()).finishResize(any(), any(), any(), any());
    }

    @Test
    public void testOperatorCordonIsNeverReleased() throws Exception {
        Mockito.when(reconciler.readNode(vm, access)).thenReturn(node(true, null, null));
        Mockito.when(reconciler.startResize(cluster, vm, access, "uid-1")).thenReturn(node(true, CLUSTER_UUID, null));
        afterPlacement(() -> false);

        Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));

        Mockito.verify(reconciler, Mockito.never()).releaseCordon(any(), any(), any(), any());
    }

    @Test
    public void testCordonOfAnEarlierAttemptIsKeptAfterAFailedRetry() throws Exception {
        Mockito.when(reconciler.readNode(vm, access)).thenReturn(node(true, CLUSTER_UUID, CLUSTER_UUID));
        afterPlacement(() -> false);

        Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));

        Mockito.verify(reconciler, Mockito.never()).releaseCordon(any(), any(), any(), any());
    }

    @Test
    public void testLeftoverOwnershipMarkerDoesNotKeepThisAttemptsCordon() throws Exception {
        Mockito.when(reconciler.readNode(vm, access)).thenReturn(node(false, null, CLUSTER_UUID));
        afterPlacement(() -> false);

        Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));

        Mockito.verify(reconciler).releaseCordon(cluster, vm, access, "uid-1");
    }

    @Test
    public void testUnreadableVmAfterAFailureLeavesTheNodeAsItIs() throws Exception {
        ManagementServerException failure = new ManagementServerException("upgrade failed");
        CloudRuntimeException databaseFailure = new CloudRuntimeException("database unavailable");
        Mockito.when(vmDao.findById(1L)).thenReturn(vm).thenThrow(databaseFailure);
        failAfterPlacement(failure);

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));

        Assert.assertSame(failure, error.getCause());
        Assert.assertTrue(error.getMessage(), error.getMessage().contains("could not be read again"));
        Assert.assertSame(databaseFailure, failure.getSuppressed()[0]);
        Mockito.verify(reconciler, Mockito.never()).releaseCordon(any(), any(), any(), any());
    }

    @Test
    public void testFailedCordonReleaseKeepsTheOriginalFailure() throws Exception {
        ManagementServerException failure = new ManagementServerException("upgrade failed");
        NodeCapacityReconciliationException releaseFailure = new NodeCapacityReconciliationException("control plane unreachable");
        failAfterPlacement(failure);
        Mockito.when(reconciler.releaseCordon(cluster, vm, access, "uid-1")).thenThrow(releaseFailure);

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));

        Assert.assertSame(failure, error.getCause());
        Assert.assertSame(releaseFailure, failure.getSuppressed()[0]);
        Assert.assertTrue(error.getMessage(), error.getMessage().contains("could not remove its temporary cordon"));
    }

    @Test
    public void testInterruptedResizeLeavesTheNodeAsItIs() throws Exception {
        afterPlacement(() -> {
            Thread.currentThread().interrupt();
            throw new ManagementServerException("interrupted");
        });
        try {
            CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));

            Assert.assertTrue(error.getMessage(), error.getMessage().contains("The operation was interrupted"));
            Mockito.verify(reconciler, Mockito.never()).releaseCordon(any(), any(), any(), any());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void testRetryOfTheSameScaleConvergesAndReconciles() throws Exception {
        failAfterPlacement(new ManagementServerException("agent timeout"));
        Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));
        Mockito.verify(reconciler).releaseCordon(cluster, vm, access, "uid-1");
        Mockito.when(reconciler.readNode(vm, access)).thenReturn(node(false, CLUSTER_UUID, null));
        acceptLiveResize();

        resize(WORKER);

        Mockito.verify(vmManager, Mockito.times(2)).upgradeVirtualMachine(eq(1L), eq(20L), any(), any(Runnable.class));
        Mockito.verify(reconciler).finishResize(cluster, resizedVm, access, "uid-1");
    }

    @Test
    public void testMissingCurrentOfferingFailsBeforeAnyChange() {
        Mockito.when(offeringDao.findByIdIncludingRemoved(10L)).thenReturn(null);

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));

        Assert.assertTrue(error.getMessage(), error.getMessage().contains("current offering of the VM was not found"));
        Mockito.verifyNoInteractions(reconciler, vmManager);
    }

    @Test
    public void testMissingVmFails() {
        Mockito.when(vmDao.findById(1L)).thenReturn(null);

        Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));

        Mockito.verifyNoInteractions(reconciler, vmManager);
    }

    private void acceptLiveResize() throws Exception {
        afterPlacement(() -> {
            Mockito.when(vmDao.findById(1L)).thenReturn(resizedVm);
            return true;
        });
    }

    private void failAfterPlacement(Exception failure) throws Exception {
        afterPlacement(() -> {
            throw failure;
        });
    }

    private void afterPlacement(Callable<Boolean> resize) throws Exception {
        Mockito.doAnswer(invocation -> {
            invocation.<Runnable>getArgument(3).run();
            liveResizeAttempts++;
            return resize.call();
        }).when(vmManager).upgradeVirtualMachine(eq(1L), eq(20L), any(), any(Runnable.class));
    }

    private void resize(KubernetesClusterNodeType nodeType) {
        resizer().resize(vmMap, nodeType, target, DEADLINE);
    }

    private KubernetesClusterNodeOfferingResizer resizer() {
        return new KubernetesClusterNodeOfferingResizer(cluster,
                clusterRunning,
                vmDao,
                offeringDao,
                vmManager,
                reconciler,
                userVm -> {
                    accessResolutions++;
                    return access;
                });
    }

    private void assertPlainResize(KubernetesClusterNodeType nodeType) throws Exception {
        resize(nodeType);

        Mockito.verify(vmManager).upgradeVirtualMachine(eq(1L), eq(20L), any());
        Mockito.verifyNoInteractions(reconciler);
        Assert.assertEquals(0, accessResolutions);
    }

    private void assertCordonReleasedAfter(Exception failure) throws Exception {
        failAfterPlacement(failure);

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, () -> resize(WORKER));

        Assert.assertSame(failure, error.getCause());
        Assert.assertTrue(error.getMessage(), error.getMessage().contains("may have been partly resized"));
        Assert.assertTrue(error.getMessage(), error.getMessage().contains("removed its temporary cordon"));
        Mockito.verify(reconciler).releaseCordon(cluster, vm, access, "uid-1");
        Mockito.verify(reconciler, Mockito.never()).finishResize(any(), any(), any(), any());
    }

    private static NodeCapacitySnapshot node(boolean unschedulable, String pendingOwner, String cordonOwner) {
        Map<String, String> annotations = new HashMap<>();
        if (pendingOwner != null) {
            annotations.put(NodeCapacitySnapshot.PENDING_ANNOTATION, pendingOwner);
        }
        if (cordonOwner != null) {
            annotations.put(NodeCapacitySnapshot.CORDON_ANNOTATION, cordonOwner);
        }
        return new NodeCapacitySnapshot("uid-1",
                "7",
                unschedulable,
                annotations,
                true,
                new NodeResources(4000L, 4L * 1024L * 1024L * 1024L),
                new NodeResources(3900L, 3L * 1024L * 1024L * 1024L));
    }

    private static GuestResources guest(long cpus, long memTotalKiB, long onlineMemoryMiB) {
        return new GuestResources(cpus, memTotalKiB, onlineMemoryMiB * MIB, 128L * MIB);
    }

    private static ServiceOfferingVO offering(long id, int cpu, int memory) {
        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offering.getId()).thenReturn(id);
        Mockito.when(offering.getCpu()).thenReturn(cpu);
        Mockito.when(offering.getRamSize()).thenReturn(memory);
        Mockito.when(offering.getName()).thenReturn("offering-" + id);
        return offering;
    }

    private static UserVmVO vm(long offeringId) {
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        Mockito.when(vm.getId()).thenReturn(1L);
        Mockito.when(vm.getUuid()).thenReturn("vm-uuid");
        Mockito.when(vm.getDisplayName()).thenReturn("worker-1");
        Mockito.when(vm.getServiceOfferingId()).thenReturn(offeringId);
        Mockito.when(vm.getHypervisorType()).thenReturn(HypervisorType.KVM);
        return vm;
    }
}
