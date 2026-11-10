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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.alert.AlertService;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.alert.AlertManager;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventVO;
import com.cloud.kubernetes.cluster.actionworkers.KubernetesClusterVmMapCheckWorker;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.user.User;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;

/**
 * Compares, for each CloudManaged Kubernetes cluster in Running or Alert state, the VMs CloudStack records for the
 * cluster (kubernetes_cluster_vm_map) with the VMs of the cluster in CloudStack and with the nodes Kubernetes knows
 * about, and raises an event and an alert for each mismatch found.
 * <p>
 * It only reports: the map, the VMs and the cluster are left as they are. An alert is raised once for a mismatch,
 * and again only once it has gone and come back. The mismatches already reported are kept as details of the cluster,
 * so that they are not reported again after a restart or by another management server, and are removed once the
 * cluster matches again.
 */
public class KubernetesClusterVmMapChecker {
    protected Logger logger = LogManager.getLogger(getClass());

    public static final ConfigKey<Boolean> KubernetesClusterVmMapCheckEnabled = new ConfigKey<>("Advanced", Boolean.class,
            "cloud.kubernetes.cluster.vm.map.check.enabled",
            "true",
            "Indicates whether the Kubernetes cluster status scanner checks, every few minutes, that the VMs CloudStack records " +
                    "for each Running or Alert CloudManaged Kubernetes cluster match the cluster's VMs and its Kubernetes nodes, " +
                    "and raises an alert and an event for each mismatch. The check only reports, it changes nothing",
            true,
            KubernetesClusterService.KubernetesServiceEnabled.key());

    protected static final long CHECK_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);
    /**
     * A running VM that is not a Kubernetes node is reported only once it has been in its state for this long, and a
     * VM named as a node of the cluster but not mapped only once it has existed for this long.
     */
    protected static final long GRACE_PERIOD_MS = TimeUnit.MINUTES.toMillis(15);
    /** The mismatches found by comparing the VM map with the VMs in CloudStack that were reported. */
    protected static final String REPORTED_VM_DRIFT_DETAIL = "vmMapCheckReportedVmDrift";
    /** The mismatches found by comparing the VM map with the Kubernetes nodes that were reported. */
    protected static final String REPORTED_NODE_DRIFT_DETAIL = "vmMapCheckReportedNodeDrift";
    protected static final int MAX_REPORTED_DRIFT_LENGTH = 10000;
    protected static final String CHECK_LOCK_NAME = "KubernetesCluster.VmMap.Check.Lock";
    /** Beyond this many new mismatches in a cluster, one alert sums up the rest; each still gets its event. */
    protected static final int MAX_ALERTS_PER_REPORT = 5;
    private static final String KEY_SEPARATOR = ",";
    private static final String HASHED_KEYS_PREFIX = "sha256:";
    private static final List<KubernetesCluster.State> CHECKED_STATES = Arrays.asList(KubernetesCluster.State.Running, KubernetesCluster.State.Alert);
    private static final List<VirtualMachine.State> GONE_VM_STATES = Arrays.asList(VirtualMachine.State.Destroyed,
            VirtualMachine.State.Expunging, VirtualMachine.State.Error);

    protected enum DriftType {
        /** Keyed by the VM alone, so a VM going from destroyed to expunged to gone is reported once. */
        MAPPED_VM,
        UNMAPPED_VM,
        NODE_WITHOUT_VM,
        VM_NOT_A_NODE
    }

    protected static class Drift {
        private final DriftType type;
        private final String id;
        private final String description;

        protected Drift(DriftType type, String id, String description) {
            this.type = type;
            this.id = id;
            this.description = description;
        }

        public DriftType getType() {
            return type;
        }

        public String getKey() {
            return type.name() + ":" + id;
        }

        public String getDescription() {
            return description;
        }
    }

    // from the manager, as the plugin's own beans cannot be injected here, as in the action workers
    protected KubernetesClusterDao kubernetesClusterDao;
    protected KubernetesClusterDetailsDao kubernetesClusterDetailsDao;
    protected KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    @Inject
    protected VMInstanceDao vmInstanceDao;
    @Inject
    protected AlertManager alertManager;

    private final KubernetesClusterManagerImpl clusterManager;
    private final AtomicBoolean checkRunning = new AtomicBoolean(false);
    private ExecutorService executor;
    private Long lastCheckNanoTime;

    public KubernetesClusterVmMapChecker(KubernetesClusterManagerImpl clusterManager) {
        this.clusterManager = clusterManager;
        if (clusterManager != null) {
            this.kubernetesClusterDao = clusterManager.kubernetesClusterDao;
            this.kubernetesClusterDetailsDao = clusterManager.kubernetesClusterDetailsDao;
            this.kubernetesClusterVmMapDao = clusterManager.kubernetesClusterVmMapDao;
        }
    }

    /**
     * Starts a check of the clusters if it is enabled, the last one was long enough ago and it is not still running.
     * Called on each run of the cluster status scanner, which runs far more often than the check needs to. The check
     * needs SSH to each cluster, so it runs on its own thread, not holding up the scanner and its lock.
     */
    public void checkClustersIfDue() {
        if (!isCheckEnabled()) {
            return;
        }
        // monotonic, so that a step of the clock does not hold up the checks
        long nanoTime = System.nanoTime();
        if ((lastCheckNanoTime != null && nanoTime - lastCheckNanoTime < TimeUnit.MILLISECONDS.toNanos(CHECK_INTERVAL_MS)) ||
                !checkRunning.compareAndSet(false, true)) {
            return;
        }
        lastCheckNanoTime = nanoTime;
        long now = System.currentTimeMillis();
        try {
            runInBackground(new ManagedContextRunnable() {
                @Override
                public void run() {
                    // here, as the managed context may fail before runInContext is called
                    try {
                        super.run();
                    } finally {
                        checkRunning.set(false);
                    }
                }

                @Override
                protected void runInContext() {
                    checkClustersLocked(new Date(now));
                }
            });
        } catch (RuntimeException e) {
            checkRunning.set(false);
            throw e;
        }
    }

    protected synchronized void runInBackground(Runnable check) {
        if (executor == null) {
            NamedThreadFactory threadFactory = new NamedThreadFactory("Kubernetes-Cluster-VM-Map-Checker");
            executor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = threadFactory.newThread(runnable);
                thread.setDaemon(true);
                return thread;
            });
        }
        executor.execute(check);
    }

    /** Only one management server checks at a time. */
    protected void checkClustersLocked(Date now) {
        GlobalLock lock = GlobalLock.getInternLock(CHECK_LOCK_NAME);
        try {
            if (lock.lock(0)) {
                try {
                    checkClusters(now);
                } finally {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            logger.warn("Caught exception while checking the VM maps of Kubernetes clusters", e);
        } finally {
            lock.releaseRef();
        }
    }

    protected boolean isCheckEnabled() {
        return Boolean.TRUE.equals(KubernetesClusterService.KubernetesServiceEnabled.value()) &&
                Boolean.TRUE.equals(KubernetesClusterVmMapCheckEnabled.value());
    }

    protected void checkClusters(Date now) {
        // by ID, as a cluster can move from one state to the other between the two listings
        Map<Long, KubernetesClusterVO> clusters = new LinkedHashMap<>();
        for (KubernetesCluster.State state : CHECKED_STATES) {
            kubernetesClusterDao.findManagedKubernetesClustersInState(state).forEach(cluster -> clusters.putIfAbsent(cluster.getId(), cluster));
        }
        for (KubernetesClusterVO cluster : clusters.values()) {
            try {
                checkCluster(cluster.getId(), now);
            } catch (Exception e) {
                logger.warn("Failed to check the VM map of Kubernetes cluster {}", cluster, e);
            }
        }
    }

    protected boolean isCheckable(KubernetesCluster cluster) {
        return cluster != null &&
                KubernetesCluster.ClusterType.CloudManaged.equals(cluster.getClusterType()) &&
                CHECKED_STATES.contains(cluster.getState());
    }

    protected KubernetesClusterVmMapCheckWorker createWorker(KubernetesCluster cluster) {
        return ComponentContext.inject(new KubernetesClusterVmMapCheckWorker(cluster, clusterManager));
    }

    protected void checkCluster(long clusterId, Date now) {
        // a cluster listed a while ago may since have been taken by an operation, during which its map changes
        KubernetesClusterVO cluster = kubernetesClusterDao.findById(clusterId);
        if (!isCheckable(cluster)) {
            return;
        }
        KubernetesClusterVmMapCheckWorker worker = createWorker(cluster);

        List<Drift> vmDrift = new ArrayList<>();
        // the VMs of the cluster that are there, mapped or not, by lower case host name
        Map<String, VMInstanceVO> clusterVmsByHostName = new HashMap<>();
        List<KubernetesClusterVmMapVO> vmMaps = kubernetesClusterVmMapDao.listByClusterId(clusterId);
        Set<Long> mappedVmIds = new HashSet<>();
        Map<KubernetesClusterVmMapVO, VMInstanceVO> liveMappedVms = new LinkedHashMap<>();
        for (KubernetesClusterVmMapVO vmMap : vmMaps) {
            mappedVmIds.add(vmMap.getVmId());
            VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(vmMap.getVmId());
            Drift drift = getMappedVmDrift(cluster, vmMap, vm);
            if (drift != null) {
                vmDrift.add(drift);
                // its Kubernetes node, if still there, is already accounted for by this report
                if (vm != null) {
                    addByHostName(clusterVmsByHostName, vm);
                }
                continue;
            }
            liveMappedVms.put(vmMap, vm);
            addByHostName(clusterVmsByHostName, vm);
        }

        Pattern hostNamePattern = worker.getClusterVmHostNamePattern();
        for (VMInstanceVO vm : vmInstanceDao.listByAccountId(cluster.getAccountId())) {
            if (mappedVmIds.contains(vm.getId()) || !VirtualMachine.Type.User.equals(vm.getType()) || vm.getRemoved() != null ||
                    GONE_VM_STATES.contains(vm.getState()) || StringUtils.isEmpty(vm.getHostName()) ||
                    !hostNamePattern.matcher(vm.getHostName()).matches()) {
                continue;
            }
            // a VM of another cluster of the same owner whose name happens to match
            if (kubernetesClusterVmMapDao.getClusterMapFromVmId(vm.getId()) != null) {
                continue;
            }
            addByHostName(clusterVmsByHostName, vm);
            // or one just created by an operation, on this cluster or another with the same name, that has not
            // mapped it yet
            if (isWithinGracePeriod(vm.getCreated(), now)) {
                continue;
            }
            vmDrift.add(new Drift(DriftType.UNMAPPED_VM, String.valueOf(vm.getId()),
                    String.format("VM %s (%s, %s) is named as a node of Kubernetes cluster %s but is not one of its VMs in CloudStack",
                            vm.getHostName(), vm.getUuid(), vm.getState(), cluster.getName())));
        }

        List<Drift> nodeDrift = null;
        List<String> nodeNames = worker.listKubernetesNodeNames();
        if (nodeNames == null) {
            logger.debug("Could not list the Kubernetes nodes of cluster {}, comparing its VM map with the VMs in CloudStack only", cluster);
        } else {
            nodeDrift = getNodeDrift(cluster, nodeNames, liveMappedVms, clusterVmsByHostName, getReportedKeys(clusterId, REPORTED_NODE_DRIFT_DETAIL), now);
        }

        // nor report what an operation that ran meanwhile may have changed
        if (!isCheckable(kubernetesClusterDao.findById(clusterId)) || !mappedVmIds.equals(getMappedVmIds(clusterId))) {
            logger.debug("Kubernetes cluster {} changed during its VM map check, not reporting it", cluster);
            return;
        }
        reportDrift(cluster, REPORTED_VM_DRIFT_DETAIL, vmDrift);
        if (nodeDrift != null) {
            reportDrift(cluster, REPORTED_NODE_DRIFT_DETAIL, nodeDrift);
        }
    }

    private Set<String> getReportedKeys(long clusterId, String reportedDetail) {
        KubernetesClusterDetailsVO detail = kubernetesClusterDetailsDao.findDetail(clusterId, reportedDetail);
        return parseKeys(detail == null ? null : detail.getValue());
    }

    private Set<Long> getMappedVmIds(long clusterId) {
        return kubernetesClusterVmMapDao.listByClusterId(clusterId).stream().map(KubernetesClusterVmMapVO::getVmId).collect(Collectors.toSet());
    }

    private static void addByHostName(Map<String, VMInstanceVO> vmsByHostName, VMInstanceVO vm) {
        if (StringUtils.isNotEmpty(vm.getHostName())) {
            vmsByHostName.put(vm.getHostName().toLowerCase(Locale.ROOT), vm);
        }
    }

    protected Drift getMappedVmDrift(KubernetesCluster cluster, KubernetesClusterVmMapVO vmMap, VMInstanceVO vm) {
        String problem;
        if (vm == null) {
            problem = "does not exist";
        } else if (vm.getRemoved() != null || VirtualMachine.State.Expunging.equals(vm.getState())) {
            problem = "has been or is being expunged";
        } else if (VirtualMachine.State.Destroyed.equals(vm.getState())) {
            problem = "has been destroyed";
        } else if (VirtualMachine.State.Error.equals(vm.getState())) {
            problem = "is in Error state";
        } else {
            return null;
        }
        String vmName = vm == null ? String.format("with ID %d", vmMap.getVmId()) : String.format("%s (%s)", vm.getHostName(), vm.getUuid());
        return new Drift(DriftType.MAPPED_VM, String.valueOf(vmMap.getVmId()),
                String.format("VM %s is one of the VMs of Kubernetes cluster %s in CloudStack, but %s", vmName, cluster.getName(), problem));
    }

    protected List<Drift> getNodeDrift(KubernetesCluster cluster, List<String> nodeNames, Map<KubernetesClusterVmMapVO, VMInstanceVO> liveMappedVms,
            Map<String, VMInstanceVO> clusterVmsByHostName, Set<String> reportedKeys, Date now) {
        List<Drift> drift = new ArrayList<>();
        Set<String> nodes = new HashSet<>(nodeNames);
        // a node may be registered under the fully qualified name of its VM
        Set<String> shortNodeNames = nodes.stream().map(n -> StringUtils.substringBefore(n, ".")).collect(Collectors.toSet());
        for (String nodeName : new TreeSet<>(nodes)) {
            if (!clusterVmsByHostName.containsKey(nodeName) && !clusterVmsByHostName.containsKey(StringUtils.substringBefore(nodeName, "."))) {
                drift.add(new Drift(DriftType.NODE_WITHOUT_VM, nodeName,
                        String.format("Kubernetes node %s of Kubernetes cluster %s matches none of the cluster's VMs in CloudStack, " +
                                "neither the VMs in its VM map nor the VMs named as its nodes", nodeName, cluster.getName())));
            }
        }
        for (Map.Entry<KubernetesClusterVmMapVO, VMInstanceVO> entry : liveMappedVms.entrySet()) {
            KubernetesClusterVmMapVO vmMap = entry.getKey();
            VMInstanceVO vm = entry.getValue();
            // etcd nodes run etcd only and are not Kubernetes nodes
            if (vmMap.isEtcdNode() || !VirtualMachine.State.Running.equals(vm.getState()) || StringUtils.isEmpty(vm.getHostName()) ||
                    nodes.contains(vm.getHostName().toLowerCase(Locale.ROOT)) || shortNodeNames.contains(vm.getHostName().toLowerCase(Locale.ROOT))) {
                continue;
            }
            Drift notANode = new Drift(DriftType.VM_NOT_A_NODE, String.valueOf(vm.getId()),
                    String.format("VM %s (%s) of Kubernetes cluster %s is running but is not a Kubernetes node of the cluster",
                            vm.getHostName(), vm.getUuid(), cluster.getName()));
            // a node VM that was just created or started may still be joining, unless it was already reported, as
            // a restart or a migration does not make it one
            if (isWithinNodeJoinGracePeriod(vm, now) && !reportedKeys.contains(notANode.getKey())) {
                continue;
            }
            drift.add(notANode);
        }
        return drift;
    }

    protected static boolean isWithinNodeJoinGracePeriod(VMInstanceVO vm, Date now) {
        return isWithinGracePeriod(vm.getCreated(), now) || isWithinGracePeriod(vm.getUpdateTime(), now);
    }

    private static boolean isWithinGracePeriod(Date since, Date now) {
        return since != null && now.getTime() - since.getTime() < GRACE_PERIOD_MS;
    }

    /**
     * Raises an event and an alert for each mismatch not reported yet, and records the mismatches reported. A
     * mismatch that went away is reported again if it comes back.
     */
    protected void reportDrift(KubernetesCluster cluster, String reportedDetail, List<Drift> drift) {
        KubernetesClusterDetailsVO detail = kubernetesClusterDetailsDao.findDetail(cluster.getId(), reportedDetail);
        String reported = detail == null ? null : detail.getValue();
        Set<String> keys = drift.stream().map(Drift::getKey).collect(Collectors.toCollection(TreeSet::new));
        String toReport = keys.isEmpty() ? null : serializeKeys(keys);
        if (StringUtils.equals(reported, toReport)) {
            return;
        }
        if (toReport == null) {
            kubernetesClusterDetailsDao.removeDetail(cluster.getId(), reportedDetail);
            String message = String.format("The VM map of Kubernetes cluster %s matches %s again", cluster.getName(),
                    REPORTED_VM_DRIFT_DETAIL.equals(reportedDetail) ? "the VMs of the cluster in CloudStack" : "the Kubernetes nodes of the cluster");
            logger.info(message);
            recordEvent(cluster, EventVO.LEVEL_INFO, message);
            return;
        }
        // recorded first, so that a failure to record does not report the same mismatches on every check
        kubernetesClusterDetailsDao.addDetail(cluster.getId(), reportedDetail, toReport, false);
        Set<String> reportedKeys = parseKeys(reported);
        // distinct, as the map may hold the same VM twice
        Set<String> seen = new HashSet<>(reportedKeys);
        List<Drift> newDrift = drift.stream().filter(item -> seen.add(item.getKey())).collect(Collectors.toList());
        for (int i = 0; i < newDrift.size(); i++) {
            Drift item = newDrift.get(i);
            logger.warn(item.getDescription());
            recordEvent(cluster, EventVO.LEVEL_WARN, item.getDescription());
            if (i < MAX_ALERTS_PER_REPORT) {
                sendAlert(cluster, item.getDescription(), String.format("%s. This is only reported, nothing was changed. Check " +
                        "the VMs of the Kubernetes cluster in CloudStack against its VMs and its Kubernetes nodes.", item.getDescription()));
            }
        }
        if (newDrift.size() > MAX_ALERTS_PER_REPORT) {
            String subject = String.format("%d more mismatches between Kubernetes cluster %s and its VMs in CloudStack",
                    newDrift.size() - MAX_ALERTS_PER_REPORT, cluster.getName());
            sendAlert(cluster, subject, String.format("%s, listed in the events of the cluster.", subject));
        }
    }

    /**
     * The keys, sorted, or a hash of them if they are too many to be stored. Once hashed, a change to the mismatches
     * reports them all again.
     */
    protected static String serializeKeys(Set<String> sortedKeys) {
        String joined = String.join(KEY_SEPARATOR, sortedKeys);
        if (joined.length() <= MAX_REPORTED_DRIFT_LENGTH) {
            return joined;
        }
        return HASHED_KEYS_PREFIX + DigestUtils.sha256Hex(joined);
    }

    protected static Set<String> parseKeys(String reported) {
        if (StringUtils.isEmpty(reported) || reported.startsWith(HASHED_KEYS_PREFIX)) {
            return new HashSet<>();
        }
        return new LinkedHashSet<>(Arrays.asList(reported.split(KEY_SEPARATOR)));
    }

    protected void recordEvent(KubernetesCluster cluster, String level, String description) {
        try {
            ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, cluster.getAccountId(), level,
                    KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_VM_MAP_CHECK, false, description,
                    cluster.getId(), ApiCommandResourceType.KubernetesCluster.toString(), 0);
        } catch (Exception e) {
            logger.warn("Unable to record the event \"{}\" for Kubernetes cluster {}", description, cluster, e);
        }
    }

    protected void sendAlert(KubernetesCluster cluster, String subject, String body) {
        try {
            alertManager.sendAlert(AlertService.AlertType.ALERT_TYPE_USERVM, cluster.getZoneId(), null, subject, body);
        } catch (Exception e) {
            logger.warn("Unable to send the alert \"{}\" for Kubernetes cluster {}", subject, cluster, e);
        }
    }
}
