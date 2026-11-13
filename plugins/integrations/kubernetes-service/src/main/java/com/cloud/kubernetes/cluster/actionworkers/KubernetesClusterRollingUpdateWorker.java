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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.command.user.volume.ResizeVolumeCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.StringUtils;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterUtil;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;

/**
 * Restarts the nodes of a cluster whose service offering, root disk size or affinity groups differ from the settings
 * of their node type, with those settings: the external etcd nodes, then the control nodes, one at a time, then the
 * worker nodes, a batch at a time.
 * <p>
 * Each Kubernetes node is annotated with its boot ID before it is drained, unless it was cordoned by someone else. It is
 * then stopped, changed and started, and only taken to be back once it is Ready with another boot ID, as Kubernetes
 * keeps reporting a node Ready for a while after it went down. Nothing else is recorded: the nodes that still differ,
 * are stopped or carry the annotation are the work left, so a rollout that stopped is resumed by running it again.
 * However it ends, {@link #release} puts back in service what can be.
 */
public class KubernetesClusterRollingUpdateWorker extends KubernetesClusterResourceModifierActionWorker {

    public static final String BOOT_ID_ANNOTATION = "cloudstack.apache.org/rollout-boot-id";
    protected static final String KUBECTL = "sudo /opt/bin/kubectl ";
    /** One line per node: its name, Ready condition, boot ID, rollout annotation and whether it is cordoned, tab separated */
    protected static final String NODE_STATUSES_COMMAND = KUBECTL + "get nodes -o jsonpath='{range .items[*]}{.metadata.name}{\"\\t\"}"
            + "{.status.conditions[?(@.type==\"Ready\")].status}{\"\\t\"}{.status.nodeInfo.bootID}{\"\\t\"}"
            + "{.metadata.annotations.cloudstack\\.apache\\.org/rollout-boot-id}{\"\\t\"}{.spec.unschedulable}{\"\\n\"}{end}'";
    protected static final String ANNOTATE_COMMAND = KUBECTL + "annotate node %s --overwrite " + BOOT_ID_ANNOTATION + "=%s";
    protected static final String CORDON_COMMAND = KUBECTL + "cordon %s";
    protected static final String RELEASE_COMMAND = KUBECTL + "uncordon %s && " + KUBECTL + "annotate node %s " + BOOT_ID_ANNOTATION + "-";
    protected static final String ETCD_READY_COMMAND = KUBECTL + "get --raw=/readyz/etcd";
    protected static final String ETCD_MEMBER_HEALTH_COMMAND = "sudo /opt/bin/etcdctl --endpoints=http://127.0.0.1:2379 endpoint health";
    /** The health of the etcd member a control node runs, from the metrics address kubeadm gives it */
    protected static final String STACKED_ETCD_MEMBER_HEALTH_COMMAND = "curl -s http://127.0.0.1:2381/health";
    protected static final List<KubernetesClusterNodeType> NODE_TYPE_ORDER = List.of(ETCD, CONTROL, WORKER);
    protected static final int DRAIN_ATTEMPTS = 3;
    protected static final long POLL_WAIT = 15000;
    protected static final int MAX_ERROR_LENGTH = 1024;

    /** A Kubernetes node as kubectl reports it */
    protected static class NodeStatus {
        final boolean ready;
        final String bootId;
        final String annotation;
        final boolean unschedulable;

        NodeStatus(boolean ready, String bootId, String annotation, boolean unschedulable) {
            this.ready = ready;
            this.bootId = bootId;
            this.annotation = annotation;
            this.unschedulable = unschedulable;
        }
    }

    /** A node of a batch, with what the batch did to it */
    protected static class Node {
        final KubernetesClusterVmMapVO vmMap;
        final KubernetesClusterNodeType type;
        UserVmVO vm;
        /** The boot ID the Kubernetes node has to change from to be back, "" if any will do */
        String bootIdBefore = "";
        /** Whether the node carries the annotation, so that it is uncordoned once it is back */
        boolean annotated;
        /** Whether this batch annotated the node */
        boolean annotatedHere;
        /** Whether its VM may have gone down in this batch */
        boolean stopped;

        Node(KubernetesClusterVmMapVO vmMap, UserVmVO vm) {
            this.vmMap = vmMap;
            this.vm = vm;
            this.type = KubernetesClusterUtil.getNodeType(vmMap);
        }

        boolean isKubernetesNode() {
            return type != ETCD;
        }
    }

    protected final int maxUnavailable;
    private boolean begun;
    /** The SSH address of the control node kubectl runs on */
    protected Pair<String, Integer> kubectlAddress;

    public KubernetesClusterRollingUpdateWorker(final KubernetesCluster kubernetesCluster, final int maxUnavailable,
                                                final KubernetesClusterManagerImpl clusterManager) {
        super(kubernetesCluster, clusterManager);
        this.maxUnavailable = maxUnavailable;
    }

    // pure decisions

    /** The worker batch size: at most maxunavailable, and leaving a worker node running, but at least 1 */
    public static int clampMaxUnavailable(final int requested, final int workers) {
        return Math.max(1, Math.min(requested, workers - 1));
    }

    /** The number of members of an etcd cluster that have to be up for it to work */
    public static int quorum(final int members) {
        return members / 2 + 1;
    }

    /**
     * The number of other healthy nodes of its type a control or etcd node needs to be taken down: a quorum of the
     * etcd members, which the control nodes are unless etcd runs on its own nodes, in which case one other control
     * node to serve the Kubernetes API.
     */
    public static int healthyOthersNeeded(final KubernetesClusterNodeType type, final int members, final boolean etcdOnOwnNodes) {
        return type == CONTROL && etcdOnOwnNodes ? 1 : quorum(members);
    }

    /** Whether the nodes of a type can be restarted one at a time, with the others keeping the cluster running */
    public static boolean canRestartOneAtATime(final KubernetesClusterNodeType type, final int members, final boolean etcdOnOwnNodes) {
        return type == WORKER || members - 1 >= healthyOthersNeeded(type, members, etcdOnOwnNodes);
    }

    /** Whether a node is Ready since a boot other than the one it had before its restart */
    public static boolean isBack(final NodeStatus status, final String bootIdBefore) {
        return status != null && status.ready && status.bootId != null && !status.bootId.equals(bootIdBefore);
    }

    public static <T> List<List<T>> toBatches(final List<T> nodes, final int size) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i += size) {
            batches.add(new ArrayList<>(nodes.subList(i, Math.min(nodes.size(), i + size))));
        }
        return batches;
    }

    /** Reads the output of {@link #NODE_STATUSES_COMMAND}; lines that are not about a node, such as a warning from sudo, are left out */
    public static Map<String, NodeStatus> parseNodeStatuses(final String output) {
        Map<String, NodeStatus> statuses = new HashMap<>();
        if (output == null) {
            return statuses;
        }
        for (String line : output.split("\\R")) {
            String[] fields = line.split("\t", -1);
            if (fields.length != 5 || StringUtils.isBlank(fields[0])) {
                continue;
            }
            statuses.put(fields[0].trim(), new NodeStatus("True".equals(fields[1].trim()), StringUtils.trimToNull(fields[2]),
                    StringUtils.trimToNull(fields[3]), "true".equals(fields[4].trim())));
        }
        return statuses;
    }

    // the rollout

    /**
     * Moves the cluster to Updating, which only one operation can do at a time, so that the caller can then change
     * the settings of the node types without another operation working from them
     */
    public void begin() {
        if (begun) {
            return;
        }
        init();
        transitToOperationState(KubernetesCluster.Event.UpdateRequested);
        begun = true;
        // a cancel left by a rollout that did not end; one asked for from now on is for this rollout
        try {
            kubernetesClusterDetailsDao.removeDetail(kubernetesCluster.getId(), KubernetesClusterManagerImpl.ROLLOUT_CANCEL_DETAIL);
        } catch (RuntimeException e) {
            try {
                abort();
            } catch (RuntimeException releaseError) {
                e.addSuppressed(releaseError);
            }
            throw e;
        }
    }

    /**
     * Runs the rollout. Throws if it does not complete, after {@link #release} has put back in service what it could.
     * Node types that can not lose a node are left out and reported, without failing the rollout.
     */
    public boolean rollout() {
        begin();
        String error;
        List<String> skipped = new ArrayList<>();
        try {
            // the settings to roll out, which the caller stored after begin()
            kubernetesCluster = kubernetesClusterDao.findById(kubernetesCluster.getId());
            // nodes a rollout that stopped restarted, but did not get to uncordon, are not restarted again
            releaseNodesThatAreBack();
            error = rollNodes(skipped);
        } catch (Exception e) {
            logger.warn("Rollout of Kubernetes cluster {} failed", kubernetesCluster, e);
            error = StringUtils.defaultIfEmpty(e.getMessage(), e.getClass().getSimpleName());
        }
        List<String> problems = new ArrayList<>(skipped);
        if (error != null) {
            problems.add(0, error);
        }
        final String summary = problems.isEmpty() ? null : String.join("; ", problems);
        release(summary);
        if (error != null) {
            throw new CloudRuntimeException(String.format("The rollout of Kubernetes cluster %s stopped: %s. Call updateKubernetesCluster with rollout=true " +
                    "to resume it", kubernetesCluster.getName(), summary));
        }
        logger.info("Rollout of Kubernetes cluster {} completed{}", kubernetesCluster, summary == null ? "" : ", but " + summary);
        return true;
    }

    /**
     * Works through the node types in order; returns why it stopped, or null once no node is left but those of the
     * types added to skipped
     */
    protected String rollNodes(final List<String> skipped) {
        for (KubernetesClusterNodeType type : NODE_TYPE_ORDER) {
            resolveKubectlAddress(Set.of());
            List<Node> nodes = selectNodes(type, getNodeStatuses());
            int members = countManagedNodes(type);
            if (!nodes.isEmpty() && !canRestartOneAtATime(type, members, isEtcdOnOwnNodes())) {
                // the others are left to roll; this one can never be, so it is reported rather than retried
                String reason = String.format("the %d %s node(s) were not restarted, as a cluster with %d of them can not keep running without one",
                        nodes.size(), type.name().toLowerCase(), members);
                logger.warn("Not restarting the {} nodes of Kubernetes cluster {}: {}", type.name().toLowerCase(), kubernetesCluster, reason);
                skipped.add(reason);
                continue;
            }
            int batchSize = type == WORKER ? clampMaxUnavailable(maxUnavailable, members) : 1;
            for (List<Node> batch : toBatches(nodes, batchSize)) {
                if (isCancelRequested()) {
                    return "it was cancelled";
                }
                rollBatch(batch);
            }
        }
        return null;
    }

    protected boolean isCancelRequested() {
        return kubernetesClusterDetailsDao.findDetail(kubernetesCluster.getId(), KubernetesClusterManagerImpl.ROLLOUT_CANCEL_DETAIL) != null;
    }

    /**
     * The nodes of the type to restart: those that differ from the settings of their type, are stopped, or carry the
     * annotation of a rollout that did not finish with them, which go first. External nodes are left out.
     */
    protected List<Node> selectNodes(final KubernetesClusterNodeType type, final Map<String, NodeStatus> statuses) {
        List<Node> nodes = new ArrayList<>();
        Set<Long> unfinished = new HashSet<>();
        for (KubernetesClusterVmMapVO vmMap : getKubernetesClusterVMMaps()) {
            if (vmMap.isExternalNode() || KubernetesClusterUtil.getNodeType(vmMap) != type) {
                continue;
            }
            UserVmVO vm = userVmDao.findById(vmMap.getVmId());
            if (vm == null) {
                continue;
            }
            NodeStatus status = type == ETCD ? null : statuses.get(getNodeName(vm));
            boolean stopped = !VirtualMachine.State.Running.equals(vm.getState());
            if (stopped || status != null && status.annotation != null) {
                unfinished.add(vm.getId());
            } else if (manager.getNodeOutOfDateReason(kubernetesCluster, vmMap, vm) == null) {
                continue;
            }
            nodes.add(new Node(vmMap, vm));
        }
        nodes.sort(Comparator.comparing(node -> !unfinished.contains(node.vm.getId())));
        return nodes;
    }

    protected int countManagedNodes(final KubernetesClusterNodeType type) {
        return KubernetesClusterUtil.countManagedNodes(getKubernetesClusterVMMaps(), type);
    }

    /**
     * Restarts a batch of nodes with the settings of their type. A node is only uncordoned once it is back; on a
     * failure, the nodes this batch annotated but did not stop are uncordoned at once, as they never went down.
     */
    protected void rollBatch(final List<Node> batch) {
        final KubernetesClusterNodeType type = batch.get(0).type;
        // the VMs may have changed while the batches before ran
        for (Node node : batch) {
            UserVmVO vm = userVmDao.findById(node.vm.getId());
            if (vm == null) {
                throw new CloudRuntimeException(String.format("the VM of node %s is gone", getNodeName(node.vm)));
            }
            node.vm = vm;
        }
        final long deadline = System.currentTimeMillis() + getNodeTimeout() * 1000;
        final Set<Long> batchVmIds = batch.stream().map(node -> node.vm.getId()).collect(Collectors.toSet());
        logger.info("Restarting {} node(s) {} of Kubernetes cluster {} with the settings of their type", type.name().toLowerCase(),
                batch.stream().map(node -> getNodeName(node.vm)).collect(Collectors.toList()), kubernetesCluster);
        try {
            resolveKubectlAddress(type == CONTROL ? batchVmIds : Set.of());
            Map<String, NodeStatus> statuses = getNodeStatuses();
            String unhealthy = getUnhealthyReason(batchVmIds, statuses);
            if (unhealthy != null) {
                throw new CloudRuntimeException(String.format("the cluster is not healthy: %s. Start it, or pass a maxunavailable that covers it", unhealthy));
            }
            if (type != WORKER) {
                checkQuorum(batch.get(0), statuses);
            }
            for (Node node : batch) {
                annotateAndCordon(node, statuses.get(getNodeName(node.vm)));
            }
            for (Node node : batch) {
                drain(node, deadline);
            }
            for (Node node : batch) {
                stopVm(node);
            }
            for (Node node : batch) {
                applySettings(node);
            }
            for (Node node : batch) {
                startVm(node);
            }
            for (Node node : batch) {
                waitUntilBack(node, deadline);
            }
            for (Node node : batch) {
                String reason = manager.getNodeOutOfDateReason(kubernetesCluster, node.vmMap, userVmDao.findById(node.vm.getId()));
                if (reason != null) {
                    throw new CloudRuntimeException(String.format("node %s still differs from the settings of its type after its restart: %s", getNodeName(node.vm), reason));
                }
                if (node.annotated && !releaseNode(node.vm)) {
                    throw new CloudRuntimeException(String.format("unable to uncordon node %s", getNodeName(node.vm)));
                }
            }
        } catch (RuntimeException e) {
            for (Node node : batch) {
                if (node.annotatedHere && !node.stopped && !releaseNode(node.vm)) {
                    logger.warn("Unable to uncordon node {} of Kubernetes cluster {}, which was not restarted", getNodeName(node.vm), kubernetesCluster);
                }
            }
            throw e;
        }
    }

    /**
     * Records the boot ID of a Kubernetes node on it, unless it is cordoned by someone else, and cordons it. A node
     * that carries the annotation already keeps it: its rollout did not finish, and the boot ID is from before it.
     */
    protected void annotateAndCordon(final Node node, final NodeStatus status) {
        if (!node.isKubernetesNode()) {
            return;
        }
        final String name = getNodeName(node.vm);
        if (status == null) {
            if (VirtualMachine.State.Running.equals(node.vm.getState())) {
                throw new CloudRuntimeException(String.format("node %s is not in the Kubernetes cluster", name));
            }
            // a stopped VM whose node was deleted: it registers again once started, with any boot ID
            return;
        }
        if (status.annotation != null) {
            node.annotated = true;
            node.bootIdBefore = status.annotation;
        } else {
            if (status.bootId == null) {
                throw new CloudRuntimeException(String.format("node %s reports no boot ID", name));
            }
            node.bootIdBefore = status.bootId;
            if (!status.unschedulable) {
                // recorded before the cordon, so that any cordon of ours can be told apart from someone else's
                if (runKubectl(String.format(ANNOTATE_COMMAND, name, status.bootId)) == null) {
                    throw new CloudRuntimeException(String.format("unable to annotate node %s", name));
                }
                node.annotated = true;
                node.annotatedHere = true;
            }
        }
        if (runKubectl(String.format(CORDON_COMMAND, name)) == null) {
            throw new CloudRuntimeException(String.format("unable to cordon node %s", name));
        }
    }

    protected void drain(final Node node, final long deadline) {
        if (!node.isKubernetesNode() || !VirtualMachine.State.Running.equals(node.vm.getState())) {
            return;
        }
        Pair<Boolean, String> drained = drainKubernetesClusterNode(kubectlAddress.first(), kubectlAddress.second(), node.vm, DRAIN_ATTEMPTS, DRAIN_RETRY_WAIT, deadline);
        if (!Boolean.TRUE.equals(drained.first())) {
            throw new CloudRuntimeException(String.format("unable to drain node %s: %s", getNodeName(node.vm), drained.second()));
        }
    }

    protected void stopVm(final Node node) {
        node.stopped = true;
        if (!VirtualMachine.State.Running.equals(node.vm.getState())) {
            return;
        }
        final String name = getNodeName(node.vm);
        registerVmContext(node.vm);
        try {
            userVmService.stopVirtualMachine(node.vm.getId(), false);
        } catch (Exception e) {
            logger.warn("Failed to stop VM {} of Kubernetes cluster {}", node.vm, kubernetesCluster, e);
        } finally {
            CallContext.unregister();
        }
        UserVmVO vm = userVmDao.findById(node.vm.getId());
        if (vm == null || !VirtualMachine.State.Stopped.equals(vm.getState())) {
            // a VM that is still running never went down
            node.stopped = vm == null || !VirtualMachine.State.Running.equals(vm.getState());
            throw new CloudRuntimeException(String.format("unable to stop the VM of node %s", name));
        }
        node.vm = vm;
    }

    /** Gives the stopped VM the service offering, root disk size and affinity groups of its node type, as needed */
    protected void applySettings(final Node node) {
        final String name = getNodeName(node.vm);
        try {
            Long offeringId = manager.getNodeTypeServiceOfferingId(kubernetesCluster, node.type);
            if (offeringId != null && offeringId != node.vm.getServiceOfferingId()) {
                registerVmContext(node.vm);
                try {
                    if (!userVmManager.upgradeVirtualMachine(node.vm.getId(), offeringId, new HashMap<>())) {
                        throw new CloudRuntimeException(String.format("unable to change the service offering of the VM of node %s", name));
                    }
                } finally {
                    CallContext.unregister();
                }
            }
            long sizeGb = KubernetesClusterUtil.getNodeRootDiskSize(kubernetesCluster, node.type);
            List<VolumeVO> roots = volumeDao.findByInstanceAndType(node.vm.getId(), Volume.Type.ROOT);
            if (sizeGb > 0 && !roots.isEmpty() && roots.get(0).getSize() != null && roots.get(0).getSize() < (sizeGb << 30)) {
                ResizeVolumeCmd resizeVolumeCmd = ComponentContext.inject(new ResizeVolumeCmd());
                resizeVolumeCmd.setId(roots.get(0).getId());
                resizeVolumeCmd.setSize(sizeGb);
                volumeService.resizeVolume(resizeVolumeCmd);
            }
            Set<Long> wanted = manager.getNodeTypeWantedAffinityGroupIds(kubernetesCluster.getId(), node.type);
            Set<Long> groupIds = wanted == null ? null : new LinkedHashSet<>(wanted);
            if (groupIds != null && !groupIds.equals(manager.getVmAffinityGroupIds(node.vm.getId(), false))) {
                // the explicit dedication group CKS gives the nodes of a dedicated domain or account stays
                groupIds.addAll(manager.getVmAffinityGroupIds(node.vm.getId(), true));
                affinityGroupVMMapDao.updateMap(node.vm.getId(), new ArrayList<>(groupIds));
            }
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Failed to change the VM of node {} of Kubernetes cluster {}", name, kubernetesCluster, e);
            throw new CloudRuntimeException(String.format("unable to change the VM of node %s: %s", name, e.getMessage()), e);
        }
    }

    /** Starts the VM, which the deployment planner places by its new affinity groups */
    protected void startVm(final Node node) {
        registerVmContext(node.vm);
        try {
            userVmManager.startVirtualMachine(userVmDao.findById(node.vm.getId()), null);
        } catch (Exception e) {
            logger.warn("Failed to start VM {} of Kubernetes cluster {}", node.vm, kubernetesCluster, e);
            throw new CloudRuntimeException(String.format("unable to start the VM of node %s: %s", getNodeName(node.vm),
                    StringUtils.defaultIfEmpty(e.getMessage(), e.getClass().getSimpleName())), e);
        } finally {
            CallContext.unregister();
        }
        node.vm = userVmDao.findById(node.vm.getId());
    }

    /**
     * Waits for a Kubernetes node to be Ready with a new boot ID, and for the etcd member of an etcd or control node
     * and the etcd cluster to be healthy again
     */
    protected void waitUntilBack(final Node node, final long deadline) {
        final String name = getNodeName(node.vm);
        if (node.isKubernetesNode() && !pollUntil(deadline, () -> isBack(getNodeStatusesOrEmpty().get(name), node.bootIdBefore))) {
            throw new CloudRuntimeException(String.format("node %s is not Ready with a new boot ID", name));
        }
        if (isEtcdMember(node.type) && !pollUntil(deadline, () -> isEtcdMemberHealthy(node.vm))) {
            throw new CloudRuntimeException(String.format("the etcd member of node %s does not report healthy", name));
        }
        if (node.type != WORKER && !pollUntil(deadline, this::isEtcdReady)) {
            throw new CloudRuntimeException("etcd does not report ready");
        }
    }

    /**
     * Refuses to take a control or etcd node down if the others of its type could not keep a quorum without it,
     * or, for a control node when etcd runs on its own nodes, if no other control node serves the Kubernetes API.
     */
    protected void checkQuorum(final Node node, final Map<String, NodeStatus> statuses) {
        final boolean etcdOnOwnNodes = isEtcdOnOwnNodes();
        int members = 0;
        int healthyOthers = 0;
        for (KubernetesClusterVmMapVO vmMap : getKubernetesClusterVMMaps()) {
            if (KubernetesClusterUtil.getNodeType(vmMap) != node.type) {
                continue;
            }
            members++;
            UserVmVO vm = vmMap.getVmId() == node.vm.getId() ? null : userVmDao.findById(vmMap.getVmId());
            if (vm == null || !VirtualMachine.State.Running.equals(vm.getState())) {
                continue;
            }
            NodeStatus status = statuses.get(getNodeName(vm));
            boolean ready = node.type == ETCD || status != null && status.ready;
            if (ready && (!isEtcdMember(node.type) || isEtcdMemberHealthy(vm))) {
                healthyOthers++;
            }
        }
        int needed = healthyOthersNeeded(node.type, members, etcdOnOwnNodes);
        if (healthyOthers < needed) {
            throw new CloudRuntimeException(String.format("%s node %s can not be restarted: only %d of the other %d are healthy, and %d are needed to keep the cluster available",
                    node.type.name().toLowerCase(), getNodeName(node.vm), healthyOthers, members - 1, needed));
        }
        if (!isEtcdReady()) {
            throw new CloudRuntimeException("etcd does not report ready");
        }
    }

    /**
     * Why the cluster is not healthy, apart from the given VMs, or null if it is: a VM that is not running, or a
     * Kubernetes node that is not Ready
     */
    protected String getUnhealthyReason(final Set<Long> exemptVmIds, final Map<String, NodeStatus> statuses) {
        for (KubernetesClusterVmMapVO vmMap : getKubernetesClusterVMMaps()) {
            if (exemptVmIds.contains(vmMap.getVmId())) {
                continue;
            }
            UserVmVO vm = userVmDao.findById(vmMap.getVmId());
            if (vm == null || !VirtualMachine.State.Running.equals(vm.getState())) {
                return String.format("the VM of node %s is %s", vm == null ? vmMap.getVmId() : getNodeName(vm), vm == null ? "missing" : vm.getState());
            }
            NodeStatus status = vmMap.isEtcdNode() ? null : statuses.get(getNodeName(vm));
            if (!vmMap.isEtcdNode() && (status == null || !status.ready)) {
                return String.format("node %s is not Ready", getNodeName(vm));
            }
        }
        return null;
    }

    // release

    /**
     * Puts back in service what a rollout leaves behind, however it ends; running it again changes nothing more.
     * It uncordons the annotated nodes that are Ready with a new boot ID, and leaves the others cordoned, counts the
     * CPU cores and memory of the cluster from its VMs, moves the cluster from Updating to Running, or to Alert if it is
     * not healthy, and records why the rollout stopped, if it did.
     */
    public void release(final String error) {
        release(error, true);
    }

    /**
     * Ends a rollout that did no node work, such as one whose settings could not be stored or were refused: the
     * cluster goes back to the state it was in, without looking at its nodes, and the last error, which may tell
     * of a node left down, stays as it is
     */
    public void abort() {
        KubernetesClusterVO cluster = kubernetesClusterDao.findById(kubernetesCluster.getId());
        if (cluster == null || !KubernetesCluster.State.Updating.equals(cluster.getState())) {
            return;
        }
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.State.Running.equals(getOperationStartState())
                ? KubernetesCluster.Event.OperationSucceeded : KubernetesCluster.Event.OperationFailed);
    }

    protected void release(final String error, final boolean recordError) {
        Map<String, NodeStatus> statuses = null;
        try {
            statuses = releaseNodesThatAreBack();
        } catch (Exception e) {
            logger.warn("Unable to uncordon the nodes of Kubernetes cluster {} the rollout is done with", kubernetesCluster, e);
        }
        try {
            manager.updateKubernetesClusterCapacity(kubernetesCluster.getId());
        } catch (Exception e) {
            logger.warn("Unable to count the CPU cores and memory of Kubernetes cluster {}", kubernetesCluster, e);
        }
        String unhealthy = statuses == null ? "its nodes can not be reached" : getUnhealthyReason(Set.of(), statuses);
        KubernetesClusterVO cluster = kubernetesClusterDao.findById(kubernetesCluster.getId());
        if (cluster == null || !KubernetesCluster.State.Updating.equals(cluster.getState())) {
            // released already: the details may be those of a rollout that started since
            return;
        }
        // before the cluster leaves Updating, after which another rollout may start and be asked to cancel
        if (!recordError) {
            logger.debug("Kept the last rollout error of Kubernetes cluster {}", kubernetesCluster);
        } else if (error == null) {
            kubernetesClusterDetailsDao.removeDetail(kubernetesCluster.getId(), KubernetesClusterManagerImpl.ROLLOUT_LAST_ERROR_DETAIL);
        } else {
            kubernetesClusterDetailsDao.addDetail(kubernetesCluster.getId(), KubernetesClusterManagerImpl.ROLLOUT_LAST_ERROR_DETAIL,
                    StringUtils.abbreviate(error, MAX_ERROR_LENGTH), true);
        }
        kubernetesClusterDetailsDao.removeDetail(kubernetesCluster.getId(), KubernetesClusterManagerImpl.ROLLOUT_CANCEL_DETAIL);
        if (unhealthy != null) {
            logger.warn("Kubernetes cluster {} is not healthy after its rollout: {}", kubernetesCluster, unhealthy);
        }
        stateTransitTo(kubernetesCluster.getId(), unhealthy == null ? KubernetesCluster.Event.OperationSucceeded : KubernetesCluster.Event.OperationFailed);
    }

    /**
     * Uncordons the annotated nodes that are Ready with a new boot ID, and removes their annotation; returns the
     * status of the nodes after that
     */
    protected Map<String, NodeStatus> releaseNodesThatAreBack() {
        resolveKubectlAddress(Set.of());
        Map<String, NodeStatus> statuses = getNodeStatuses();
        boolean released = false;
        for (KubernetesClusterVmMapVO vmMap : getKubernetesClusterVMMaps()) {
            UserVmVO vm = userVmDao.findById(vmMap.getVmId());
            NodeStatus status = vm == null ? null : statuses.get(getNodeName(vm));
            if (status == null || status.annotation == null || !isBack(status, status.annotation)) {
                continue;
            }
            if (releaseNode(vm)) {
                released = true;
            } else {
                logger.warn("Unable to uncordon node {} of Kubernetes cluster {}", getNodeName(vm), kubernetesCluster);
            }
        }
        return released ? getNodeStatuses() : statuses;
    }

    // access to the nodes

    /**
     * Runs kubectl on the first running control node that is not one of the given VMs: the first control node, which
     * the address of the cluster reaches, unless it is being restarted
     */
    protected void resolveKubectlAddress(final Set<Long> excludedVmIds) {
        Pair<String, Integer> address = getKubernetesClusterServerIpSshPort(null);
        if (StringUtils.isEmpty(address.first())) {
            throw new CloudRuntimeException(String.format("unable to find the address of Kubernetes cluster %s", kubernetesCluster.getName()));
        }
        publicIpAddress = address.first();
        sshPort = address.second();
        for (KubernetesClusterVmMapVO vmMap : getKubernetesClusterVMMaps()) {
            if (!vmMap.isControlNode() || excludedVmIds.contains(vmMap.getVmId())) {
                continue;
            }
            UserVmVO vm = userVmDao.findById(vmMap.getVmId());
            if (vm != null && VirtualMachine.State.Running.equals(vm.getState())) {
                kubectlAddress = getSshAddress(vm);
                return;
            }
        }
        throw new CloudRuntimeException(String.format("no control node of Kubernetes cluster %s is running", kubernetesCluster.getName()));
    }

    /**
     * The address to reach a node over SSH: without port forwarding, its own address; with it, the port of its rule,
     * or else of its position among the VMs of the cluster
     */
    protected Pair<String, Integer> getSshAddress(final UserVm vm) {
        if (sshPort == DEFAULT_SSH_PORT && StringUtils.isNotEmpty(vm.getPrivateIpAddress())) {
            return new Pair<>(vm.getPrivateIpAddress(), DEFAULT_SSH_PORT);
        }
        List<KubernetesClusterVmMapVO> vmMaps = getKubernetesClusterVMMaps();
        int index = 0;
        for (int i = 0; i < vmMaps.size(); i++) {
            if (vmMaps.get(i).getVmId() == vm.getId()) {
                index = i;
                break;
            }
        }
        return getNodeSshAddress(vm, index);
    }

    /** The output of a kubectl command, or null if it failed */
    protected String runKubectl(final String command) {
        try {
            Pair<Boolean, String> result = executeSshCommand(kubectlAddress.first(), kubectlAddress.second(), command, 10000, 10000, 60000);
            return Boolean.TRUE.equals(result.first()) && result.second() != null ? result.second() : null;
        } catch (Exception e) {
            logger.debug("Unable to run {} on Kubernetes cluster {}", command, kubernetesCluster, e);
            return null;
        }
    }

    protected Map<String, NodeStatus> getNodeStatuses() {
        String output = runKubectl(NODE_STATUSES_COMMAND);
        if (output == null) {
            throw new CloudRuntimeException(String.format("unable to get the status of the nodes of Kubernetes cluster %s", kubernetesCluster.getName()));
        }
        return parseNodeStatuses(output);
    }

    /** The node statuses, or none while the Kubernetes API can not be reached, as while a control node restarts */
    protected Map<String, NodeStatus> getNodeStatusesOrEmpty() {
        try {
            return getNodeStatuses();
        } catch (CloudRuntimeException e) {
            logger.debug("Unable to get the status of the nodes of Kubernetes cluster {}: {}", kubernetesCluster, e.getMessage());
            return Map.of();
        }
    }

    /** Uncordons a node and removes its annotation */
    protected boolean releaseNode(final UserVm vm) {
        String name = getNodeName(vm);
        return runKubectl(String.format(RELEASE_COMMAND, name, name)) != null;
    }

    protected boolean isEtcdReady() {
        String output = runKubectl(ETCD_READY_COMMAND);
        return output != null && output.lines().anyMatch(line -> "ok".equals(line.trim()));
    }

    protected boolean isEtcdOnOwnNodes() {
        return kubernetesCluster.getEtcdNodeCount() != null && kubernetesCluster.getEtcdNodeCount() > 0;
    }

    /** Whether nodes of the type run an etcd member: etcd nodes, or control nodes when etcd runs on them */
    protected boolean isEtcdMember(final KubernetesClusterNodeType type) {
        return isEtcdOnOwnNodes() ? type == ETCD : type == CONTROL;
    }

    /** Whether the etcd member on a VM reports itself healthy, which it only does once it is part of the etcd cluster */
    protected boolean isEtcdMemberHealthy(final UserVm vm) {
        boolean ownNode = isEtcdOnOwnNodes();
        Pair<String, Integer> address = getSshAddress(vm);
        try {
            Pair<Boolean, String> result = executeSshCommand(address.first(), address.second(),
                    ownNode ? ETCD_MEMBER_HEALTH_COMMAND : STACKED_ETCD_MEMBER_HEALTH_COMMAND, 10000, 10000, 30000);
            if (!Boolean.TRUE.equals(result.first()) || result.second() == null) {
                return false;
            }
            return ownNode ? result.second().contains("is healthy") : result.second().replace(" ", "").contains("\"health\":\"true\"");
        } catch (Exception e) {
            logger.debug("Unable to check the etcd member on {} of Kubernetes cluster {}", vm, kubernetesCluster, e);
            return false;
        }
    }

    /** Checks the condition until it holds or the deadline passes; returns whether it held */
    protected boolean pollUntil(final long deadline, final BooleanSupplier condition) {
        do {
            if (condition.getAsBoolean()) {
                return true;
            }
        } while (System.currentTimeMillis() < deadline && sleepBeforeRetry(POLL_WAIT));
        return false;
    }

    protected long getNodeTimeout() {
        Long timeout = KubernetesClusterService.KubernetesClusterUpdateNodeTimeout.value();
        return timeout != null && timeout > 0 ? timeout : Long.parseLong(KubernetesClusterService.KubernetesClusterUpdateNodeTimeout.defaultValue());
    }

    /** Registers a call context for the events of a VM action; the caller unregisters it */
    protected void registerVmContext(final UserVm vm) {
        CallContext vmContext = CallContext.register(CallContext.current(), ApiCommandResourceType.VirtualMachine);
        vmContext.setEventResourceId(Objects.requireNonNull(vm).getId());
    }
}
