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

import java.nio.charset.StandardCharsets;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.command.user.volume.ResizeVolumeCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterUtil;
import com.cloud.kubernetes.version.KubernetesSupportedVersion;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.LaunchPermissionVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.AccountManager;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceDetailVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

/**
 * Restarts the nodes of a cluster whose service offering, root disk size or affinity groups differ from the settings
 * of their node type, with those settings: the external etcd nodes, then the control nodes, one at a time, then the
 * worker nodes, a batch at a time. A worker node whose template differs is recreated from the template of its type.
 * <p>
 * Each Kubernetes node is annotated with its boot ID before it is drained, unless it was cordoned by someone else. It is
 * then stopped, changed and started, and only taken to be back once it is Ready with another boot ID, as Kubernetes
 * keeps reporting a node Ready for a while after it went down. Nothing else is recorded: the nodes that still differ,
 * are stopped or carry the annotation are the work left, so a rollout that stopped is resumed by running it again.
 * However it ends, {@link #release} puts back in service what can be.
 */
public class KubernetesClusterRollingUpdateWorker extends KubernetesClusterResourceModifierActionWorker {

    public static final String BOOT_ID_ANNOTATION = "cloudstack.apache.org/rollout-boot-id";
    /** A detail of a VM that is being recreated, so that a rollout that stopped recreates it again rather than only starting it */
    public static final String RECREATE_DETAIL = "cks.rollout.recreate";
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
    protected static final String JOIN_TOKEN_CREATE_COMMAND = "sudo /opt/bin/kubeadm token create --ttl %ds --print-join-command";
    protected static final String JOIN_TOKEN_DELETE_COMMAND = "sudo /opt/bin/kubeadm token delete %s";
    private static final Pattern JOIN_COMMAND_TOKEN = Pattern.compile("--token\\s+([a-z0-9]{6}\\.[a-z0-9]{16})(?!\\S)");
    private static final Pattern JOIN_COMMAND_CA_CERT_HASH = Pattern.compile("--discovery-token-ca-cert-hash\\s+(sha256:[0-9a-f]{64})(?!\\S)");
    /** The token and CA verification arguments of the kubeadm join of a worker's user data, with or without a pinned CA */
    private static final Pattern USER_DATA_JOIN = Pattern.compile(
            "(kubeadm join \\S+ --token )\\S+ (?:--discovery-token-unsafe-skip-ca-verification|--discovery-token-ca-cert-hash \\S+)");
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
        /** The boot ID the Kubernetes node reports before the batch, "" if none */
        String bootIdNow = "";
        /** Whether the node carries the annotation, so that it is uncordoned once it is back */
        boolean annotated;
        /** Whether this batch annotated the node */
        boolean annotatedHere;
        /** Whether its VM may have gone down in this batch */
        boolean stopped;
        /** Whether its VM is recreated from the template of its type */
        boolean recreate;
        /** Whether its VM was recreated by this batch and has the binaries ISO attached, to be detached once it is back */
        boolean isoAttached;
        /** Whether this batch saw the node back */
        boolean back;

        Node(KubernetesClusterVmMapVO vmMap, UserVmVO vm) {
            this.vmMap = vmMap;
            this.vm = vm;
            this.type = KubernetesClusterUtil.getNodeType(vmMap);
        }

        boolean isKubernetesNode() {
            return type != ETCD;
        }
    }

    @Inject
    protected SnapshotDao snapshotDao;
    @Inject
    protected VMSnapshotDao vmSnapshotDao;
    @Inject
    protected DiskOfferingDao diskOfferingDao;
    @Inject
    protected VirtualMachineManager itMgr;
    @Inject
    protected AccountManager accountManager;

    protected final int maxUnavailable;
    private boolean begun;
    /** The SSH address of the control node kubectl runs on */
    protected Pair<String, Integer> kubectlAddress;
    /** The SSH address of each control node, and of each etcd node but behind an external load balancer, by VM id */
    protected Map<Long, Pair<String, Integer>> nodeSshAddresses = Map.of();
    /** Whether the etcd nodes can be reached over SSH, which they can not behind an external load balancer */
    protected boolean etcdReachable = true;
    /** The join token and CA certificate hash the recreated workers of a batch join with */
    protected Pair<String, String> recreateJoin;

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
        // a node that can not be reached fails the rollout before it changes anything
        resolveNodeSshAddresses();
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
            if (type == ETCD && !nodes.isEmpty() && !etcdReachable) {
                String reason = String.format("the %d etcd node(s) were not restarted, as they can not be reached over SSH behind the external load balancer",
                        nodes.size());
                logger.warn("Not restarting the etcd nodes of Kubernetes cluster {}: {}", kubernetesCluster, reason);
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
     * annotation or recreate detail of a rollout that did not finish with them, which go first. External nodes are left out.
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
            boolean recreating = type == WORKER && vmInstanceDetailsDao.findDetail(vm.getId(), RECREATE_DETAIL) != null;
            if (stopped || recreating || status != null && status.annotation != null) {
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
                node.recreate = isRecreateNeeded(node);
                if (node.recreate) {
                    checkRecreatable(node);
                }
            }
            if (batch.stream().anyMatch(node -> node.recreate)) {
                createRecreateJoinToken();
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
                if (node.recreate) {
                    waitUntilNotReady(node, deadline);
                }
            }
            for (Node node : batch) {
                startVm(node);
            }
            for (Node node : batch) {
                waitUntilBack(node, deadline);
                node.back = true;
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
        } finally {
            // a recreated node that may still be joining needs the token, which expires on its own
            try {
                if (batch.stream().noneMatch(node -> node.isoAttached && !node.back && !isStopped(node.vm))) {
                    deleteRecreateJoinToken();
                }
            } catch (RuntimeException e) {
                logger.warn("Unable to tell whether the recreated nodes of Kubernetes cluster {} are joining, so their join token is left to expire", kubernetesCluster, e);
            }
            recreateJoin = null;
            // a recreated VM that is not back needs the ISO to install from once started; a later rollout detaches it
            for (Node node : batch) {
                if (node.isoAttached && node.back) {
                    try {
                        finishRecreate(node.vm);
                    } catch (RuntimeException e) {
                        logger.warn("Unable to detach the binaries ISO from the VM of node {} of Kubernetes cluster {}", getNodeName(node.vm), kubernetesCluster, e);
                    }
                }
            }
        }
    }

    /**
     * Whether the VM of a node is to be recreated: a worker node whose template differs from that of its type, or
     * whose recreation a rollout that stopped did not finish
     */
    protected boolean isRecreateNeeded(final Node node) {
        if (node.type != WORKER) {
            return false;
        }
        Long templateId = manager.getNodeTypeTemplateId(kubernetesCluster, node.type);
        return templateId != null && templateId != node.vm.getTemplateId() || vmInstanceDetailsDao.findDetail(node.vm.getId(), RECREATE_DETAIL) != null;
    }

    /** Removes the recreate detail of a VM whose node joined again, and detaches the binaries ISO from it */
    protected void finishRecreate(final UserVm vm) {
        vmInstanceDetailsDao.removeDetail(vm.getId(), RECREATE_DETAIL);
        detachIsoKubernetesVMs(List.of(vm));
    }

    protected boolean isReadyInZone(final VMTemplateVO template) {
        return VirtualMachineTemplate.State.Active.equals(template.getState()) && manager.isTemplateReadyInZone(template, kubernetesCluster.getZoneId());
    }

    protected long getRecreateTemplateId(final Node node) {
        final Long typeTemplateId = manager.getNodeTypeTemplateId(kubernetesCluster, node.type);
        return typeTemplateId != null ? typeTemplateId : node.vm.getTemplateId();
    }

    public static boolean isSystemTemplate(final VMTemplateVO template) {
        return Storage.TemplateType.SYSTEM.equals(template.getTemplateType()) || Storage.TemplateType.ROUTING.equals(template.getTemplateType());
    }

    /**
     * Refuses to recreate a VM that restoreVirtualMachine would refuse, or that could not come back: one with VM
     * snapshots or a snapshot of its root disk in progress, or whose template is not active and ready in the zone.
     * Returns the size in GB of the new root disk: the largest of the old one, the size of the node type and the
     * template, but the old one if its disk offering has a strictly fixed size, which the template has to fit in.
     */
    protected long checkRecreatable(final Node node) {
        final String name = getNodeName(node.vm);
        if (!vmSnapshotDao.findByVm(node.vm.getId()).isEmpty()) {
            throw new CloudRuntimeException(String.format("the VM of node %s has VM snapshots, which have to be removed for it to be recreated", name));
        }
        List<VolumeVO> roots = volumeDao.findByInstanceAndType(node.vm.getId(), Volume.Type.ROOT);
        for (VolumeVO root : roots) {
            if (!snapshotDao.listByStatus(root.getId(), Snapshot.State.Creating, Snapshot.State.CreatedOnPrimary, Snapshot.State.BackingUp).isEmpty()) {
                throw new CloudRuntimeException(String.format("a snapshot of the root disk of node %s is in progress", name));
            }
            if (root.isDeleteProtection()) {
                throw new CloudRuntimeException(String.format("the root disk of node %s has delete protection", name));
            }
        }
        if (getJoinUserData(node.vm, "", "") == null) {
            throw new CloudRuntimeException(String.format("the user data of the VM of node %s has no kubeadm join to give a new join token", name));
        }
        if (templateDao.findById(node.vm.getTemplateId()) == null) {
            // restoreVirtualMachine needs the template the VM has
            throw new CloudRuntimeException(String.format("the template of the VM of node %s was deleted, so it can not be recreated", name));
        }
        final VMTemplateVO template = templateDao.findById(getRecreateTemplateId(node));
        if (template == null || !isReadyInZone(template)) {
            throw new CloudRuntimeException(String.format("the template to recreate node %s from is not active and ready in the zone", name));
        }
        if (!isSystemTemplate(template)) {
            // the restore checks the access of the caller, who may not be the one who set the template
            accountManager.checkAccess(CallContext.current().getCallingAccount(), null, false, template);
        }
        final KubernetesSupportedVersion version = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId());
        final VMTemplateVO iso = version == null ? null : templateDao.findById(version.getIsoId());
        if (iso == null || !isReadyInZone(iso)) {
            throw new CloudRuntimeException(String.format("the binaries ISO of the Kubernetes version of cluster %s is not active and ready in the zone",
                    kubernetesCluster.getName()));
        }
        if (node.vm.getIsoId() != null && node.vm.getIsoId() != iso.getId()) {
            throw new CloudRuntimeException(String.format("the VM of node %s has another ISO attached, which has to be detached for the binaries ISO", name));
        }
        final long oldSizeGb = roots.isEmpty() ? 0 : toGb(roots.get(0).getSize());
        final DiskOfferingVO diskOffering = roots.isEmpty() ? null : diskOfferingDao.findById(roots.get(0).getDiskOfferingId());
        if (diskOffering != null && diskOffering.getDiskSizeStrictness()) {
            if (toGb(template.getSize()) > oldSizeGb || KubernetesClusterUtil.getNodeRootDiskSize(kubernetesCluster, node.type) > oldSizeGb) {
                throw new CloudRuntimeException(String.format("the template or the root disk size of its type is larger than the root disk of node %s, whose disk offering has a strictly fixed size", name));
            }
            return oldSizeGb;
        }
        return Math.max(oldSizeGb, Math.max(KubernetesClusterUtil.getNodeRootDiskSize(kubernetesCluster, node.type), toGb(template.getSize())));
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
        node.bootIdNow = status == null ? "" : StringUtils.defaultString(status.bootId);
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

    /** Gives the stopped VM the template, service offering, root disk size and affinity groups of its node type, as needed */
    protected void applySettings(final Node node) {
        final String name = getNodeName(node.vm);
        try {
            if (node.recreate) {
                recreateVm(node);
            }
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

    /**
     * Reinstalls the stopped VM of a worker node from the template of its type, keeping its name, addresses and
     * Kubernetes node. Its user data then joins it to the cluster again, with the binaries ISO attached, as for a new
     * node. The old root disk is destroyed, not expunged.
     */
    protected void recreateVm(final Node node) throws Exception {
        if (!VirtualMachine.State.Stopped.equals(node.vm.getState())) {
            // the restore would restart a running VM, without the drain
            throw new CloudRuntimeException(String.format("the VM of node %s is %s, not stopped, so it can not be recreated", getNodeName(node.vm), node.vm.getState()));
        }
        // a snapshot may have started while the node was drained
        final long sizeGb = checkRecreatable(node);
        final long templateId = getRecreateTemplateId(node);
        final VMTemplateVO template = templateDao.findById(templateId);
        final KubernetesSupportedVersion version = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId());
        Map<String, String> details = new HashMap<>();
        if (sizeGb > 0) {
            details.put(VmDetailConstants.ROOT_DISK_SIZE, String.valueOf(sizeGb));
        }
        logger.info("Recreating the VM of node {} of Kubernetes cluster {} from template {}", getNodeName(node.vm), kubernetesCluster, template);
        // the boot ID before, for a later rollout to tell when the node is back, even if it does not carry the annotation
        vmInstanceDetailsDao.addDetail(node.vm.getId(), RECREATE_DETAIL, node.bootIdNow, false);
        // the token the VM joined with may be gone, as it expires
        final String oldUserData = node.vm.getUserData();
        setUserData(node, getJoinUserData(node.vm, recreateJoin.first(), recreateJoin.second()));
        // as for new nodes, the system VM template is only let use while the node is deployed from it; the restore
        // checks the access of the caller
        final long callerId = CallContext.current().getCallingAccountId();
        final boolean grant = isSystemTemplate(template) && !template.isPublicTemplate()
                && template.getAccountId() != callerId && launchPermissionDao.findByTemplateAndAccount(templateId, callerId) == null;
        List<VolumeVO> roots = volumeDao.findByInstanceAndType(node.vm.getId(), Volume.Type.ROOT);
        if (node.vm.getTemplateId() == templateId && !roots.isEmpty() && Volume.State.Allocated.equals(roots.get(0).getState())) {
            // restored already by a rollout that stopped before the VM started: its new root disk is created when it starts
            logger.info("The VM of node {} of Kubernetes cluster {} was recreated from template {} already", getNodeName(node.vm), kubernetesCluster, template);
        } else {
            try {
                restoreVm(node, templateId, grant, callerId, details);
            } catch (Exception e) {
                List<VolumeVO> after = volumeDao.findByInstanceAndType(node.vm.getId(), Volume.Type.ROOT);
                if (after.size() == 1 && !roots.isEmpty() && after.get(0).getId() == roots.get(0).getId()) {
                    // refused before the root disk was replaced: the VM is put back as it was
                    setUserData(node, oldUserData);
                    vmInstanceDetailsDao.removeDetail(node.vm.getId(), RECREATE_DETAIL);
                }
                throw e;
            }
        }
        attachBinariesIso(node, version);
    }

    protected void restoreVm(final Node node, final long templateId, final boolean grant, final long callerId, final Map<String, String> details) throws Exception {
        registerVmContext(node.vm);
        try {
            if (grant) {
                launchPermissionDao.persist(new LaunchPermissionVO(templateId, callerId));
            }
            itMgr.restoreVirtualMachine(node.vm.getId(), templateId, null, false, details);
        } finally {
            CallContext.unregister();
            if (grant) {
                launchPermissionDao.removePermissions(templateId, List.of(callerId));
            }
        }
        node.vm = userVmDao.findById(node.vm.getId());
    }

    protected void attachBinariesIso(final Node node, final KubernetesSupportedVersion version) {
        if (!Objects.equals(version.getIsoId(), node.vm.getIsoId())) {
            registerVmContext(node.vm);
            try {
                if (!templateService.attachIso(version.getIsoId(), node.vm.getId(), true)) {
                    throw new CloudRuntimeException(String.format("unable to attach the binaries ISO to the VM of node %s", getNodeName(node.vm)));
                }
            } finally {
                CallContext.unregister();
            }
        }
        node.isoAttached = true;
    }

    /**
     * Creates a join token on a control node for the recreated workers of a batch, valid for as long as the batch may
     * take, and reads the hash of the cluster CA they pin
     */
    protected void createRecreateJoinToken() {
        String output = runKubectl(String.format(JOIN_TOKEN_CREATE_COMMAND, Math.max(getNodeTimeout(), 1)));
        recreateJoin = parseJoinCommand(output);
        if (recreateJoin == null) {
            Matcher token = output == null ? null : JOIN_COMMAND_TOKEN.matcher(output);
            if (token != null && token.find()) {
                recreateJoin = new Pair<>(token.group(1), null);
                deleteRecreateJoinToken();
            }
            throw new CloudRuntimeException(String.format("unable to create a join token on Kubernetes cluster %s", kubernetesCluster.getName()));
        }
    }

    protected void deleteRecreateJoinToken() {
        if (recreateJoin == null) {
            return;
        }
        if (runKubectl(String.format(JOIN_TOKEN_DELETE_COMMAND, recreateJoin.first().substring(0, 6))) == null) {
            logger.warn("Unable to delete the join token of the recreated nodes of Kubernetes cluster {}, it expires on its own", kubernetesCluster);
        }
        recreateJoin = null;
    }

    protected void setUserData(final Node node, final String userData) {
        node.vm.setUserData(userData);
        userVmDao.update(node.vm.getId(), node.vm);
    }

    protected boolean isStopped(final UserVm vm) {
        UserVmVO current = userVmDao.findById(vm.getId());
        return current == null || VirtualMachine.State.Stopped.equals(current.getState());
    }

    /** The token and CA certificate hash of the output of kubeadm token create --print-join-command, or null */
    protected static Pair<String, String> parseJoinCommand(final String joinCommand) {
        if (StringUtils.isBlank(joinCommand)) {
            return null;
        }
        Matcher token = JOIN_COMMAND_TOKEN.matcher(joinCommand);
        Matcher hash = JOIN_COMMAND_CA_CERT_HASH.matcher(joinCommand);
        return token.find() && hash.find() ? new Pair<>(token.group(1), hash.group(1)) : null;
    }

    /**
     * The user data of a worker VM, base64 encoded, with its kubeadm join given a token and the CA hash to pin, or null
     * if it has no kubeadm join
     */
    protected static String getJoinUserData(final UserVm vm, final String token, final String caCertHash) {
        String userData = decodeUserData(vm);
        Matcher join = userData == null ? null : USER_DATA_JOIN.matcher(userData);
        if (join == null || !join.find()) {
            return null;
        }
        userData = join.replaceAll("$1" + Matcher.quoteReplacement(token + " --discovery-token-ca-cert-hash " + caCertHash));
        return Base64.encodeBase64String(userData.getBytes(StandardCharsets.UTF_8));
    }

    protected static String decodeUserData(final UserVm vm) {
        return StringUtils.isBlank(vm.getUserData()) ? null : new String(Base64.decodeBase64(vm.getUserData()), StandardCharsets.UTF_8);
    }

    protected static long toGb(final Long bytes) {
        return bytes == null ? 0 : (bytes + (1L << 30) - 1) >> 30;
    }

    /**
     * Waits for Kubernetes to see the node of a stopped VM as not Ready, as kubeadm join refuses to join a node under
     * the name of a Ready one
     */
    protected void waitUntilNotReady(final Node node, final long deadline) {
        final String name = getNodeName(node.vm);
        BooleanSupplier notReady = () -> {
            try {
                NodeStatus status = getNodeStatuses().get(name);
                return status == null || !status.ready;
            } catch (CloudRuntimeException e) {
                return false;
            }
        };
        if (!pollUntil(deadline, notReady)) {
            throw new CloudRuntimeException(String.format("node %s did not report NotReady while its VM was stopped, or its status could not be read", name));
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
     * CPU cores and memory of the cluster from its VMs, makes the offering all its nodes run that of the cluster if
     * it is the one of their types, moves the cluster from Updating to Running, or to Alert if it is not healthy, and
     * records why the rollout stopped, if it did.
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
        try {
            manager.syncClusterServiceOffering(kubernetesCluster.getId());
        } catch (Exception e) {
            logger.warn("Unable to update the service offering of Kubernetes cluster {} from its nodes", kubernetesCluster, e);
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
            VMInstanceDetailVO recreating = vm == null ? null : vmInstanceDetailsDao.findDetail(vm.getId(), RECREATE_DETAIL);
            if (recreating != null && status != null && status.bootId != null && !status.bootId.equals(recreating.getValue())) {
                // recreated by a rollout that stopped before the node was back; it has joined again since, as it has
                // another boot ID, even if it is down now
                finishRecreate(vm);
            }
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
     * Resolves the SSH address of the nodes the rollout runs commands on: the control nodes, and the etcd nodes but
     * behind an external load balancer, which has no port for them. Fails naming a node that has no address.
     */
    protected void resolveNodeSshAddresses() {
        Pair<String, Integer> address = getKubernetesClusterServerIpSshPort(null);
        if (StringUtils.isEmpty(address.first())) {
            throw new CloudRuntimeException(String.format("Unable to find the address of Kubernetes cluster %s", kubernetesCluster.getName()));
        }
        publicIpAddress = address.first();
        sshPort = address.second();
        etcdReachable = !manager.hasExternalLoadBalancer(kubernetesCluster.getId());
        List<UserVm> vms = new ArrayList<>();
        for (KubernetesClusterVmMapVO vmMap : getKubernetesClusterVMMaps()) {
            if (!vmMap.isControlNode() && !(vmMap.isEtcdNode() && etcdReachable)) {
                continue;
            }
            UserVmVO vm = userVmDao.findById(vmMap.getVmId());
            if (vm != null) {
                vms.add(vm);
            }
        }
        Map<Long, Pair<String, Integer>> addresses;
        try {
            addresses = getClusterNodesSshIpPorts(vms);
        } catch (CloudRuntimeException e) {
            throw new CloudRuntimeException(String.format("Unable to reach the nodes of Kubernetes cluster %s over SSH: %s", kubernetesCluster.getName(), e.getMessage()), e);
        }
        for (UserVm vm : vms) {
            if (StringUtils.isEmpty(addresses.get(vm.getId()).first())) {
                throw new CloudRuntimeException(String.format("Unable to reach the nodes of Kubernetes cluster %s over SSH: node %s has no IP address",
                        kubernetesCluster.getName(), getNodeName(vm)));
            }
        }
        nodeSshAddresses = addresses;
    }

    /**
     * Runs kubectl on the first running control node that is not one of the given VMs: the first control node, which
     * the address of the cluster reaches, unless it is being restarted
     */
    protected void resolveKubectlAddress(final Set<Long> excludedVmIds) {
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

    /** The SSH address of a control or etcd node, as resolved when the rollout began */
    protected Pair<String, Integer> getSshAddress(final UserVm vm) {
        Pair<String, Integer> address = nodeSshAddresses.get(vm.getId());
        if (address == null) {
            throw new CloudRuntimeException(String.format("the SSH address of node %s is not known", getNodeName(vm)));
        }
        return address;
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
