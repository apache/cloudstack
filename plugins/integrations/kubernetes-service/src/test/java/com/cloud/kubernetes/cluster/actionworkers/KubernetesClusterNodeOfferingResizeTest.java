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
import java.util.List;

import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterNodeCapacityReconciler;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterNodeCapacityReconciler.NodeCapacitySnapshot;
import com.cloud.kubernetes.cluster.utils.NodeCapacityReconciliationException;
import com.cloud.network.Network;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.WORKER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

public class KubernetesClusterNodeOfferingResizeTest {
    private final KubernetesCluster cluster = Mockito.mock(KubernetesCluster.class);
    private final KubernetesClusterManagerImpl manager = Mockito.mock(KubernetesClusterManagerImpl.class);
    private final KubernetesClusterVmMapDao vmMapDao = Mockito.mock(KubernetesClusterVmMapDao.class);
    private final UserVmDao vmDao = Mockito.mock(UserVmDao.class);
    private final UserVmManager vmManager = Mockito.mock(UserVmManager.class);
    private final ServiceOfferingDao offeringDao = Mockito.mock(ServiceOfferingDao.class);
    private final KubernetesClusterNodeCapacityReconciler reconciler = Mockito.mock(KubernetesClusterNodeCapacityReconciler.class);
    private final ServiceOfferingVO oldOffering = offering(10L, 2, 2048);
    private final ServiceOfferingVO target = offering(20L, 4, 4096);
    private final UserVmVO first = vm(1L, 10L);
    private final UserVmVO second = vm(2L, 10L);
    private final UserVmVO resizedFirst = vm(1L, 20L);
    private final UserVmVO resizedSecond = vm(2L, 20L);
    private KubernetesClusterScaleWorker worker;

    @Before
    public void setUp() throws Exception {
        Mockito.when(cluster.getId()).thenReturn(100L);
        Mockito.when(cluster.getName()).thenReturn("test-cluster");
        Mockito.when(cluster.getUuid()).thenReturn("cluster-uuid");
        Mockito.when(cluster.getState()).thenReturn(KubernetesCluster.State.Running);
        Mockito.when(cluster.getClusterType()).thenReturn(KubernetesCluster.ClusterType.CloudManaged);
        Mockito.when(cluster.getNodeCount()).thenReturn(2L);
        Mockito.when(cluster.getTotalNodeCount()).thenReturn(2L);
        worker = Mockito.spy(new KubernetesClusterScaleWorker(cluster, new HashMap<>(), null, null, null, null, null, manager));
        Mockito.when(cluster.getState()).thenReturn(KubernetesCluster.State.Scaling);
        worker.userVmDao = vmDao;
        worker.userVmManager = vmManager;
        worker.serviceOfferingDao = offeringDao;
        worker.kubernetesClusterVmMapDao = vmMapDao;
        worker.kubernetesClusterNodeCapacityReconciler = reconciler;
        ReflectionTestUtils.setField(worker, "scaleTimeoutTime", System.currentTimeMillis() + 60000L);
        ReflectionTestUtils.setField(worker, "network", Mockito.mock(Network.class));
        Mockito.when(manager.isDirectAccess(any())).thenReturn(true);
        Mockito.doReturn(new Pair<>("control", 22)).when(worker).getKubernetesClusterServerIpSshPort(null);
        Mockito.doReturn("cloud").when(worker).getControlNodeLoginUser();
        Mockito.doReturn(null).when(worker).getManagementServerSshPublicKeyFile();
        Mockito.doReturn(true).when(worker).stateTransitTo(anyLong(), any());
        Mockito.doNothing().when(worker).deleteTemplateLaunchPermission();
        Mockito.doReturn(Mockito.mock(KubernetesClusterVO.class)).when(worker).updateKubernetesClusterEntry(
                any(), any(), any(), any(), any(), any(), any(), eq(WORKER), anyBoolean(), anyBoolean());
        List<KubernetesClusterVmMapVO> maps = List.of(vmMap(1L), vmMap(2L));
        Mockito.when(vmMapDao.listByClusterId(100L)).thenReturn(maps);
        Mockito.when(vmMapDao.listByClusterIdAndVmType(100L, WORKER)).thenReturn(maps);
        Mockito.when(offeringDao.findById(10L)).thenReturn(oldOffering);
        Mockito.when(offeringDao.findById(20L)).thenReturn(target);
        Mockito.when(vmDao.findById(1L)).thenReturn(first);
        Mockito.when(vmDao.findById(2L)).thenReturn(second);
        Mockito.when(vmManager.upgradeVirtualMachine(eq(1L), eq(20L), any())).thenAnswer(invocation -> {
            Mockito.when(vmDao.findById(1L)).thenReturn(resizedFirst);
            return true;
        });
        Mockito.when(vmManager.upgradeVirtualMachine(eq(2L), eq(20L), any())).thenAnswer(invocation -> {
            Mockito.when(vmDao.findById(2L)).thenReturn(resizedSecond);
            return true;
        });
        Mockito.when(reconciler.requiresKubeletRefresh(oldOffering, target, WORKER, KubernetesCluster.State.Running)).thenReturn(true);
        Mockito.when(reconciler.captureBefore(eq(cluster), any(), any())).thenReturn(snapshot(false, false));
    }

    @Test
    public void testNodesCompleteReconciliationSequentially() throws Exception {
        scale();

        InOrder order = Mockito.inOrder(reconciler, vmManager);
        order.verify(reconciler).cordonIfNeeded(eq(cluster), eq(first), any(), any(), anyLong());
        order.verify(vmManager).upgradeVirtualMachine(eq(1L), eq(20L), any());
        order.verify(reconciler).verifyGuestResources(eq(resizedFirst), eq(target), any(), any(), anyLong());
        order.verify(reconciler).restartKubelet(eq(resizedFirst), any(), anyLong());
        order.verify(reconciler).waitForKubernetesResources(eq(cluster), eq(resizedFirst), eq(target), any(), any(), anyLong());
        order.verify(reconciler).restoreSchedulability(eq(cluster), eq(resizedFirst), any(), any(), anyLong());
        order.verify(reconciler).cordonIfNeeded(eq(cluster), eq(second), any(), any(), anyLong());
        order.verify(vmManager).upgradeVirtualMachine(eq(2L), eq(20L), any());
        order.verify(reconciler).restoreSchedulability(eq(cluster), eq(resizedSecond), any(), any(), anyLong());
    }

    @Test
    public void testResourceUnavailableRestoresTemporaryCordon() throws Exception {
        assertPreResizeFailure(new ResourceUnavailableException("host unavailable", UserVmVO.class, 1L));
    }

    @Test
    public void testManagementFailureRestoresTemporaryCordon() throws Exception {
        assertPreResizeFailure(new ManagementServerException("upgrade failed"));
    }

    @Test
    public void testMigrationFailureRestoresTemporaryCordon() throws Exception {
        assertPreResizeFailure(new VirtualMachineMigrationException("migration failed"));
    }

    @Test
    public void testRejectedVmResizeRestoresTemporaryCordon() throws Exception {
        Mockito.doReturn(false).when(vmManager).upgradeVirtualMachine(eq(1L), eq(20L), any());
        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, this::scale);
        Assert.assertTrue(error.getMessage().contains("The VM was not resized"));
        Mockito.verify(reconciler).restoreSchedulability(eq(cluster), eq(first), any(), any(), anyLong());
        assertSecondNodeUntouched();
    }

    @Test
    public void testFailedCleanupPreservesOriginalResizeFailure() throws Exception {
        ManagementServerException failure = new ManagementServerException("original failure");
        Mockito.doThrow(new NodeCapacityReconciliationException("cleanup failed")).when(reconciler)
                .restoreSchedulability(eq(cluster), eq(first), any(), any(), anyLong());
        assertPreResizeFailure(failure);
    }

    @Test
    public void testOperatorCordonIsNotRestoredAfterRejectedResize() throws Exception {
        Mockito.when(reconciler.captureBefore(eq(cluster), eq(first), any())).thenReturn(snapshot(true, false));
        Mockito.doReturn(false).when(vmManager).upgradeVirtualMachine(eq(1L), eq(20L), any());
        Assert.assertThrows(CloudRuntimeException.class, this::scale);
        Mockito.verify(reconciler, Mockito.never()).restoreSchedulability(any(), any(), any(), any(), anyLong());
        assertSecondNodeUntouched();
    }

    @Test
    public void testPostResizeFailureLeavesNodeCordonedAndSecondNodeUntouched() {
        NodeCapacityReconciliationException failure = new NodeCapacityReconciliationException("guest verification failed");
        Mockito.doThrow(failure).when(reconciler).verifyGuestResources(eq(resizedFirst), eq(target), any(), any(), anyLong());

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, this::scale);

        Assert.assertSame(failure, error.getCause());
        Assert.assertTrue(error.getMessage().contains("The node remains cordoned"));
        Mockito.verify(reconciler, Mockito.never()).restoreSchedulability(any(), any(), any(), any(), anyLong());
        assertSecondNodeUntouched();
    }

    @Test
    public void testSnapshotFailureStopsBeforeCordonOrVmResize() {
        NodeCapacityReconciliationException failure = new NodeCapacityReconciliationException("SSH unavailable");
        Mockito.when(reconciler.captureBefore(eq(cluster), eq(first), any())).thenThrow(failure);

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, this::scale);

        Assert.assertSame(failure, error.getCause());
        Mockito.verifyNoInteractions(vmManager);
        Mockito.verify(reconciler, Mockito.never()).cordonIfNeeded(any(), any(), any(), any(), anyLong());
        Mockito.verify(reconciler, Mockito.never()).restoreSchedulability(any(), any(), any(), any(), anyLong());
        assertSecondNodeUntouched();
    }

    @Test
    public void testCordonFailureStopsBeforeVmResize() {
        Mockito.doThrow(new NodeCapacityReconciliationException("cordon failed")).when(reconciler)
                .cordonIfNeeded(eq(cluster), eq(first), any(), any(), anyLong());

        Assert.assertThrows(CloudRuntimeException.class, this::scale);

        Mockito.verifyNoInteractions(vmManager);
        Mockito.verify(reconciler, Mockito.never()).restoreSchedulability(any(), any(), any(), any(), anyLong());
        assertSecondNodeUntouched();
    }

    @Test
    public void testRetryReconcilesAlreadyResizedNodeBeforeResizingNext() throws Exception {
        Mockito.when(vmDao.findById(1L)).thenReturn(resizedFirst);
        Mockito.when(reconciler.captureBefore(eq(cluster), eq(resizedFirst), any())).thenReturn(snapshot(true, true));

        scale();

        Mockito.verify(vmManager, Mockito.never()).upgradeVirtualMachine(eq(1L), anyLong(), any());
        InOrder order = Mockito.inOrder(reconciler, vmManager);
        order.verify(reconciler).restartKubelet(eq(resizedFirst), any(), anyLong());
        order.verify(reconciler).restoreSchedulability(eq(cluster), eq(resizedFirst), any(), any(), anyLong());
        order.verify(vmManager).upgradeVirtualMachine(eq(2L), eq(20L), any());
    }

    @Test
    public void testCurrentResourcesDoNotRestartKubelet() {
        Mockito.when(reconciler.isKubernetesResourcesCurrent(any(), eq(target))).thenReturn(true);
        scale();
        Mockito.verify(reconciler, Mockito.never()).restartKubelet(any(), any(), anyLong());
        Mockito.verify(reconciler).restoreSchedulability(eq(cluster), eq(resizedFirst), any(), any(), anyLong());
    }

    @Test
    public void testStoppedClusterResizesWithoutGuestOrKubeletOperations() throws Exception {
        ReflectionTestUtils.setField(worker, "originalState", KubernetesCluster.State.Stopped);
        scale();
        Mockito.verify(vmManager).upgradeVirtualMachine(eq(1L), eq(20L), any());
        Mockito.verify(vmManager).upgradeVirtualMachine(eq(2L), eq(20L), any());
        Mockito.verify(reconciler, Mockito.never()).captureBefore(any(), any(), any());
        Mockito.verify(reconciler, Mockito.never()).restartKubelet(any(), any(), anyLong());
    }

    @Test
    public void testCreatedClusterUpdatesMetadataWithoutTouchingVms() {
        ReflectionTestUtils.setField(worker, "originalState", KubernetesCluster.State.Created);
        scale();
        Mockito.verifyNoInteractions(vmDao, vmManager, reconciler);
        Mockito.verify(worker).updateKubernetesClusterEntry(eq(8L), eq(8192L), any(), eq(20L),
                any(), any(), any(), eq(WORKER), eq(true), eq(false));
    }

    @Test
    public void testMissingVmAfterSuccessfulResizeDoesNotHideFailureWithNullPointer() throws Exception {
        Mockito.doAnswer(invocation -> {
            Mockito.when(vmDao.findById(1L)).thenReturn(null);
            return true;
        }).when(vmManager).upgradeVirtualMachine(eq(1L), eq(20L), any());

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, this::scale);

        Assert.assertTrue(error.getMessage().contains("VM vm-1 did not reach target offering"));
        Mockito.verify(reconciler, Mockito.never()).restoreSchedulability(any(), any(), any(), any(), anyLong());
        assertSecondNodeUntouched();
    }

    @Test
    public void testUnexpectedProgrammingExceptionIsNotWrappedAsOperationalFailure() throws Exception {
        NullPointerException failure = new NullPointerException("programming error");
        Mockito.doThrow(failure).when(vmManager).upgradeVirtualMachine(eq(1L), eq(20L), any());

        Assert.assertSame(failure, Assert.assertThrows(NullPointerException.class, this::scale));

        Mockito.verify(worker, Mockito.never()).stateTransitTo(100L, KubernetesCluster.Event.OperationFailed);
        assertSecondNodeUntouched();
    }

    private void scale() {
        worker.scaleKubernetesClusterOffering(WORKER, target, true, false);
    }

    private void assertPreResizeFailure(Exception failure) throws Exception {
        Mockito.doThrow(failure).when(vmManager).upgradeVirtualMachine(eq(1L), eq(20L), any());
        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, this::scale);
        Assert.assertSame(failure, error.getCause());
        Mockito.verify(reconciler).restoreSchedulability(eq(cluster), eq(first), any(), any(), anyLong());
        Mockito.verify(worker).stateTransitTo(100L, KubernetesCluster.Event.OperationFailed);
        assertSecondNodeUntouched();
    }

    private void assertSecondNodeUntouched() {
        Mockito.verify(vmDao, Mockito.never()).findById(2L);
        Mockito.verify(reconciler, Mockito.never()).captureBefore(eq(cluster), eq(second), any());
        Mockito.verify(worker, Mockito.never()).updateKubernetesClusterEntry(
                any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean());
    }

    private ServiceOfferingVO offering(long id, int cpu, int memory) {
        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offering.getId()).thenReturn(id);
        Mockito.when(offering.getCpu()).thenReturn(cpu);
        Mockito.when(offering.getRamSize()).thenReturn(memory);
        return offering;
    }

    private UserVmVO vm(long id, long offeringId) {
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        Mockito.when(vm.getId()).thenReturn(id);
        Mockito.when(vm.getUuid()).thenReturn("vm-" + id);
        Mockito.when(vm.getDisplayName()).thenReturn("worker-" + id);
        Mockito.when(vm.getPrivateIpAddress()).thenReturn("node-" + id);
        Mockito.when(vm.getServiceOfferingId()).thenReturn(offeringId);
        Mockito.when(vm.getHypervisorType()).thenReturn(HypervisorType.KVM);
        return vm;
    }

    private KubernetesClusterVmMapVO vmMap(long id) {
        KubernetesClusterVmMapVO map = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(map.getVmId()).thenReturn(id);
        return map;
    }

    private NodeCapacitySnapshot snapshot(boolean unschedulable, boolean cksCordon) {
        return new NodeCapacitySnapshot(unschedulable, cksCordon, 2L, 2097152L,
                2000L, 2147483648L, 1900L, 2013265920L, true);
    }
}
