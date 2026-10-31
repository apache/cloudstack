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
import java.util.function.Function;

import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType;
import com.cloud.kubernetes.cluster.utils.GuestResources;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterNodeCapacityReconciler;
import com.cloud.kubernetes.cluster.utils.NodeAccess;
import com.cloud.kubernetes.cluster.utils.NodeCapacitySnapshot;
import com.cloud.kubernetes.cluster.utils.NodeCapacityReconciliationException;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.CONTROL;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.WORKER;

/**
 * Moves the VMs of a CKS cluster to a new service offering one at a time. For control and worker nodes of a
 * running CloudManaged KVM cluster it also makes Kubernetes report the new CPU and memory before the next
 * VM is changed. Other VMs, including dedicated etcd VMs, get the plain VM resize.
 */
public class KubernetesClusterNodeOfferingResizer {
    protected Logger logger = LogManager.getLogger(getClass());

    private final KubernetesCluster kubernetesCluster;
    private final boolean clusterRunning;
    private final UserVmDao userVmDao;
    private final ServiceOfferingDao serviceOfferingDao;
    private final UserVmManager userVmManager;
    private final KubernetesClusterNodeCapacityReconciler reconciler;
    private final Function<UserVm, NodeAccess> nodeAccessResolver;

    public KubernetesClusterNodeOfferingResizer(KubernetesCluster kubernetesCluster,
                                                boolean clusterRunning,
                                                UserVmDao userVmDao,
                                                ServiceOfferingDao serviceOfferingDao,
                                                UserVmManager userVmManager,
                                                KubernetesClusterNodeCapacityReconciler reconciler,
                                                Function<UserVm, NodeAccess> nodeAccessResolver) {
        this.kubernetesCluster = kubernetesCluster;
        this.clusterRunning = clusterRunning;
        this.userVmDao = userVmDao;
        this.serviceOfferingDao = serviceOfferingDao;
        this.userVmManager = userVmManager;
        this.reconciler = reconciler;
        this.nodeAccessResolver = nodeAccessResolver;
    }

    /**
     * Moves one VM to the target offering. A failure is thrown as a CloudRuntimeException that keeps the
     * original failure as its cause and says what state the VM and its Kubernetes node were left in.
     */
    public void resize(KubernetesClusterVmMapVO vmMap, KubernetesClusterNodeType nodeType, ServiceOffering targetOffering, long deadline) {
        processOffering(vmMap, nodeType, targetOffering, deadline, false);
    }

    /** Check a retry against the committed role offering without changing any individual VM's offering. */
    public void reconcilePending(KubernetesClusterVmMapVO vmMap, KubernetesClusterNodeType nodeType, ServiceOffering targetOffering, long deadline) {
        processOffering(vmMap, nodeType, targetOffering, deadline, true);
    }

    private void processOffering(KubernetesClusterVmMapVO vmMap, KubernetesClusterNodeType nodeType, ServiceOffering targetOffering,
                                 long deadline, boolean pendingOnly) {
        UserVmVO vm = userVmDao.findById(vmMap.getVmId());
        if (vm == null) {
            throw new CloudRuntimeException(String.format("unable to find the VM with ID %d", vmMap.getVmId()));
        }
        String context = String.format("unable to scale %s node VM %s (%s) to offering %s", nodeType, vm.getDisplayName(), vm.getUuid(),
                targetOffering.getName());
        boolean onTarget = isOnTarget(vm, targetOffering);
        String skipReason = getReconciliationSkipReason(vmMap, vm, nodeType);
        if (skipReason != null) {
            logSkip(vm, skipReason);
            if (!pendingOnly && !onTarget) {
                resizeWithoutReconciliation(vm, targetOffering, context);
            }
            return;
        }
        if (onTarget || pendingOnly) {
            finishPendingReconciliation(vm, targetOffering, deadline, context, onTarget);
            return;
        }
        ServiceOffering currentOffering = serviceOfferingDao.findByIdIncludingRemoved(vm.getServiceOfferingId());
        if (currentOffering == null) {
            throw new CloudRuntimeException(context + ": the current offering of the VM was not found. The VM was not resized.");
        }
        if (!KubernetesClusterNodeCapacityReconciler.capacityChanged(currentOffering, targetOffering)) {
            resizeWithoutReconciliation(vm, targetOffering, context);
            finishPendingReconciliation(vm, targetOffering, deadline, context, true);
            return;
        }
        resizeAndReconcile(vm, targetOffering, deadline, context);
    }

    private String getReconciliationSkipReason(KubernetesClusterVmMapVO vmMap, UserVm vm, KubernetesClusterNodeType nodeType) {
        if (vmMap.isExternalNode()) {
            return "it is an external node";
        }
        if (KubernetesCluster.ClusterType.CloudManaged != kubernetesCluster.getClusterType()) {
            return "the cluster is not CloudManaged";
        }
        if (Hypervisor.HypervisorType.KVM != vm.getHypervisorType()) {
            return "it is not a KVM VM";
        }
        if (!clusterRunning) {
            return "the cluster was not Running";
        }
        if (WORKER != nodeType && CONTROL != nodeType) {
            return "only control and worker VMs are Kubernetes nodes";
        }
        return null;
    }

    private void logSkip(UserVm vm, String reason) {
        logger.info("Not reconciling the Kubernetes capacity of VM {} in Kubernetes cluster {}: {}", vm, kubernetesCluster, reason);
    }

    private boolean isOnTarget(UserVm vm, ServiceOffering targetOffering) {
        return targetOffering.getId() == vm.getServiceOfferingId();
    }

    private void resizeWithoutReconciliation(UserVm vm, ServiceOffering targetOffering, String context) {
        boolean resized;
        try {
            resized = userVmManager.upgradeVirtualMachine(vm.getId(), targetOffering.getId(), new HashMap<>());
        } catch (ResourceUnavailableException | ManagementServerException | VirtualMachineMigrationException | RuntimeException e) {
            throw new CloudRuntimeException(String.format("%s: %s", context, describe(e)), e);
        }
        if (!resized) {
            throw new CloudRuntimeException(context);
        }
    }

    /**
     * Only an owned marker justifies touching the guest on an otherwise unchanged offering request.
     */
    private void finishPendingReconciliation(UserVm vm, ServiceOffering targetOffering, long deadline, String context, boolean onTarget) {
        try {
            NodeAccess access = nodeAccessResolver.apply(vm);
            NodeCapacitySnapshot node = reconciler.readNode(vm, access);
            String clusterUuid = kubernetesCluster.getUuid();
            if (node.hasForeignMarker(clusterUuid)) {
                throw new NodeCapacityReconciliationException("the Kubernetes node carries live resize markers of another cluster");
            }
            if (!node.isPendingFor(clusterUuid) && !node.hasOwnershipMarker(clusterUuid)) {
                logSkip(vm, "there is no pending capacity change");
                return;
            }
            if (!onTarget) {
                throw new NodeCapacityReconciliationException("the VM is on a different offering; retry the interrupted resize with its intended offering");
            }
            logger.info("Finishing the unverified Kubernetes capacity reconciliation of VM {} in Kubernetes cluster {}", vm, kubernetesCluster);
            reconcile(vm, targetOffering, access, node.getUid(), deadline);
        } catch (RuntimeException e) {
            String retry = onTarget ? ". The VM is already on the target offering; retry the same scale operation to finish reconciling its Kubernetes node." : "";
            throw new CloudRuntimeException(String.format("%s: %s%s", context, describe(e), retry), e);
        }
    }

    private void resizeAndReconcile(UserVmVO vm, ServiceOffering targetOffering, long deadline, String context) {
        NodeAccess access;
        NodeCapacitySnapshot initial;
        try {
            access = nodeAccessResolver.apply(vm);
            initial = reconciler.readNode(vm, access);
            if (initial.hasForeignMarker(kubernetesCluster.getUuid())) {
                throw new NodeCapacityReconciliationException("the Kubernetes node carries live resize markers of another cluster");
            }
            GuestResources guest = reconciler.readGuest(vm, access);
            if (KubernetesClusterNodeCapacityReconciler.isGuestAboveTarget(guest, targetOffering)) {
                throw new CloudRuntimeException(String.format("the VM already has %d CPUs and %d MiB of memory online, more than the requested " +
                        "offering; use an offering at least as large as the VM's current resources", guest.getOnlineCpus(),
                        guest.getOnlineMemoryBytes() / ByteScaleUtils.MiB));
            }
        } catch (RuntimeException e) {
            throw new CloudRuntimeException(String.format("%s: %s. The VM was not resized and its Kubernetes node was not changed.",
                    context, describe(e)), e);
        }
        MutableObject<NodeCapacitySnapshot> started = new MutableObject<>();
        try {
            UserVmVO resizedVm = upgrade(vm, targetOffering,
                    () -> started.setValue(reconciler.startResize(kubernetesCluster, vm, access, initial.getUid())));
            reconcile(resizedVm, targetOffering, access, initial.getUid(), deadline);
        } catch (ResourceUnavailableException | ManagementServerException | VirtualMachineMigrationException | RuntimeException e) {
            String recovery = started.getValue() == null ? "Live CPU/RAM resize did not start. If node preparation failed, retry the scale operation." :
                    recoverAfterFailure(vm, targetOffering, access, initial, started.getValue(), e);
            throw new CloudRuntimeException(String.format("%s: %s. %s", context, describe(e), recovery), e);
        }
    }

    private UserVmVO upgrade(UserVm vm, ServiceOffering targetOffering, Runnable beforeLiveResize)
            throws ResourceUnavailableException, ManagementServerException, VirtualMachineMigrationException {
        if (!userVmManager.upgradeVirtualMachine(vm.getId(), targetOffering.getId(), new HashMap<>(), beforeLiveResize)) {
            throw new CloudRuntimeException("the VM could not be scaled");
        }
        UserVmVO resizedVm = userVmDao.findById(vm.getId());
        if (resizedVm == null || !isOnTarget(resizedVm, targetOffering)) {
            throw new CloudRuntimeException("the VM did not reach the target offering");
        }
        return resizedVm;
    }

    /**
     * After a failed or uncertain resize the pending marker always stays, so a retry finds the node. The CKS
     * cordon stays only when the VM is on the target offering; otherwise the next scale resizes the VM again
     * (KVM only adds what is missing) and the cordon would just keep the node unschedulable until then.
     */
    private String recoverAfterFailure(UserVm vm, ServiceOffering targetOffering, NodeAccess access,
                                       NodeCapacitySnapshot initial, NodeCapacitySnapshot started, Exception failure) {
        String retry = " Retry the same scale operation to finish it.";
        if (Thread.currentThread().isInterrupted()) {
            return "The operation was interrupted and the Kubernetes node was left as it is." + retry;
        }
        UserVmVO currentVm = null;
        try {
            currentVm = userVmDao.findById(vm.getId());
        } catch (RuntimeException e) {
            failure.addSuppressed(e);
        }
        if (currentVm == null) {
            return "The VM could not be read again, so its Kubernetes node was left as it is." + retry;
        }
        String clusterUuid = kubernetesCluster.getUuid();
        if (isOnTarget(currentVm, targetOffering)) {
            String cordon = started.isCordonOwnedBy(clusterUuid) ? " and cordoned" : "";
            return String.format("The VM was resized, but its Kubernetes node is not reconciled yet and stays marked for reconciliation%s.%s",
                    cordon, retry);
        }
        String cordon = "";
        // Release only a cordon this attempt added: the node was schedulable before it started.
        if (started.isCordonOwnedBy(clusterUuid) && !initial.isUnschedulable()) {
            try {
                reconciler.releaseCordon(kubernetesCluster, vm, access, initial.getUid());
                cordon = " CKS removed its temporary cordon from the Kubernetes node.";
            } catch (RuntimeException e) {
                failure.addSuppressed(e);
                cordon = String.format(" CKS could not remove its temporary cordon from the Kubernetes node: %s.", describe(e));
            }
        }
        return "The VM may have been partly resized on its host." + cordon + retry;
    }

    private void reconcile(UserVm vm, ServiceOffering targetOffering, NodeAccess access, String nodeUid, long deadline) {
        reconciler.onlineGuestResources(access);
        GuestResources guest = reconciler.waitForGuestResources(vm, targetOffering, access, deadline);
        if (!reconciler.isKubernetesNodeAlreadyResized(reconciler.readNode(vm, access), guest) || !reconciler.isKubeletActive(access)) {
            reconciler.restartKubelet(vm, access, deadline);
            reconciler.waitForKubernetesResources(vm, guest, access, deadline);
        }
        NodeCapacitySnapshot node = reconciler.finishResize(kubernetesCluster, vm, access, nodeUid);
        logger.info("Kubernetes node of VM {} in Kubernetes cluster {} reports {}m CPU and {} bytes of memory after the resize and is {}",
                vm, kubernetesCluster, node.getCapacity().getCpuMillis(), node.getCapacity().getMemoryBytes(),
                node.isUnschedulable() ? "unschedulable" : "schedulable");
    }

    private static String describe(Exception e) {
        return StringUtils.defaultIfEmpty(e.getMessage(), e.getClass().getName());
    }
}
