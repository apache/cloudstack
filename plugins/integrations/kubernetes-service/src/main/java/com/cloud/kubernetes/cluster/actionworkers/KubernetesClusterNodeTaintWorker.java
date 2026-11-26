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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.Level;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterDetailsVO;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.version.KubernetesSupportedVersion;
import com.cloud.kubernetes.version.KubernetesVersionManagerImpl;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;

/**
 * Applies the Kubernetes non-graceful node shutdown taint to nodes whose VM CloudStack
 * knows is not running, and removes it once the VM runs again.
 */
public class KubernetesClusterNodeTaintWorker extends KubernetesClusterActionWorker {

    public static final String OUT_OF_SERVICE_TAINTED_VMS = "outOfServiceTaintedVms";
    protected static final String OUT_OF_SERVICE_TAINT_KEY = "node.kubernetes.io/out-of-service";
    protected static final Set<VirtualMachine.State> DOWN_STATES =
            EnumSet.of(VirtualMachine.State.Stopped, VirtualMachine.State.Destroyed, VirtualMachine.State.Expunging);
    protected static final String MIN_KUBERNETES_VERSION = "1.26.0";
    protected static final long CONFIRMED_TAINT_TTL_MS = 10 * 60 * 1000L;
    /**
     * VM id to the time its taint was last confirmed by this management server. A confirmed taint is
     * re-sent only after the TTL, as another management server may have run the scanner in between.
     */
    protected static final Map<Long, Long> CONFIRMED_TAINTED_VMS = new ConcurrentHashMap<>();
    protected static final String NOT_FOUND_PATTERN = "taint .* not found|nodes? \".*\" not found";

    private List<Pair<String, Integer>> controlEndpoints;
    private String loginUser;

    public KubernetesClusterNodeTaintWorker(final KubernetesCluster kubernetesCluster, final KubernetesClusterManagerImpl clusterManager) {
        super(kubernetesCluster, clusterManager);
    }

    public void syncOutOfServiceTaints(boolean taintEnabled) {
        Set<Long> tainted = getTaintedVmIds();
        boolean taint = taintEnabled && isOutOfServiceTaintSupported();
        if (!taint && tainted.isEmpty()) {
            return;
        }
        List<KubernetesClusterVmMapVO> nodeMaps = getKubernetesClusterVMMaps().stream()
                .filter(m -> !m.isEtcdNode()).collect(Collectors.toList());
        List<UserVmVO> nodeVms = new ArrayList<>();
        for (KubernetesClusterVmMapVO map : nodeMaps) {
            nodeVms.add(userVmDao.findByIdIncludingRemoved(map.getVmId()));
        }
        Set<Long> saved = tainted;
        Set<Long> updated = new LinkedHashSet<>(tainted);
        updated.retainAll(nodeMaps.stream().map(KubernetesClusterVmMapVO::getVmId).collect(Collectors.toSet()));
        long now = System.currentTimeMillis();
        CONFIRMED_TAINTED_VMS.values().removeIf(confirmedAt -> now - confirmedAt > CONFIRMED_TAINT_TTL_MS);

        for (UserVmVO vm : nodeVms) {
            if (vm == null || StringUtils.isEmpty(vm.getHostName())) {
                continue;
            }
            String nodeName = vm.getHostName().toLowerCase(Locale.ROOT);
            if (taint && DOWN_STATES.contains(vm.getState())) {
                if (updated.contains(vm.getId()) && CONFIRMED_TAINTED_VMS.containsKey(vm.getId())) {
                    continue;
                }
                if (!isStillInState(vm.getId(), DOWN_STATES, true)) {
                    continue;
                }
                // Recorded before tainting so that a taint whose result is lost is still removed later.
                boolean newlyRecorded = updated.add(vm.getId());
                if (newlyRecorded) {
                    saveTaintedVmIds(updated);
                    saved = new LinkedHashSet<>(updated);
                }
                if (runOnControlNode(nodeMaps, nodeVms, String.format("taint nodes %s %s=nodeshutdown:NoExecute --overwrite", nodeName, OUT_OF_SERVICE_TAINT_KEY))) {
                    logger.log(newlyRecorded ? Level.INFO : Level.DEBUG, "Applied out-of-service taint to node {} of Kubernetes cluster {}, its VM is {}",
                            nodeName, kubernetesCluster, vm.getState());
                    CONFIRMED_TAINTED_VMS.put(vm.getId(), now);
                }
            } else if (VirtualMachine.State.Running.equals(vm.getState()) && updated.contains(vm.getId())) {
                CONFIRMED_TAINTED_VMS.remove(vm.getId());
                if (isStillInState(vm.getId(), EnumSet.of(VirtualMachine.State.Running), false) &&
                        runOnControlNode(nodeMaps, nodeVms, String.format("taint nodes %s %s:NoExecute-", nodeName, OUT_OF_SERVICE_TAINT_KEY))) {
                    logger.info("Removed out-of-service taint from node {} of Kubernetes cluster {}", nodeName, kubernetesCluster);
                    updated.remove(vm.getId());
                }
            }
        }
        if (!updated.equals(saved)) {
            saveTaintedVmIds(updated);
        }
    }

    /**
     * Re-checks right before running kubectl, as earlier SSH calls may have taken long enough for the
     * VM state to change or, when tainting, for a cluster operation to begin.
     */
    protected boolean isStillInState(long vmId, Set<VirtualMachine.State> states, boolean requireClusterIdle) {
        if (requireClusterIdle) {
            KubernetesClusterVO cluster = kubernetesClusterDao.findById(kubernetesCluster.getId());
            if (cluster == null || !EnumSet.of(KubernetesCluster.State.Running, KubernetesCluster.State.Alert).contains(cluster.getState())) {
                return false;
            }
        }
        UserVmVO vm = userVmDao.findByIdIncludingRemoved(vmId);
        return vm != null && states.contains(vm.getState());
    }

    protected boolean isOutOfServiceTaintSupported() {
        KubernetesSupportedVersion version = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId());
        try {
            return version != null && KubernetesVersionManagerImpl.compareSemanticVersions(version.getSemanticVersion(), MIN_KUBERNETES_VERSION) >= 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    protected Set<Long> getTaintedVmIds() {
        Set<Long> ids = new LinkedHashSet<>();
        KubernetesClusterDetailsVO detail = kubernetesClusterDetailsDao.findDetail(kubernetesCluster.getId(), OUT_OF_SERVICE_TAINTED_VMS);
        if (detail == null || StringUtils.isBlank(detail.getValue())) {
            return ids;
        }
        for (String id : detail.getValue().split(",")) {
            long vmId = NumberUtils.toLong(id.trim(), -1L);
            if (vmId > 0) {
                ids.add(vmId);
            }
        }
        return ids;
    }

    protected void saveTaintedVmIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            kubernetesClusterDetailsDao.removeDetail(kubernetesCluster.getId(), OUT_OF_SERVICE_TAINTED_VMS);
            return;
        }
        String value = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        kubernetesClusterDetailsDao.addDetail(kubernetesCluster.getId(), OUT_OF_SERVICE_TAINTED_VMS, value, false);
    }

    /**
     * Control nodes are reached the same way the upgrade worker reaches them: the n-th non-etcd
     * node is behind port forward start + n, or on its own address at port 22 on a shared network.
     */
    protected List<Pair<String, Integer>> getRunningControlNodeEndpoints(List<KubernetesClusterVmMapVO> nodeMaps, List<UserVmVO> nodeVms) {
        List<Pair<String, Integer>> endpoints = new ArrayList<>();
        Pair<String, Integer> server = getKubernetesClusterServerIpSshPort(null);
        if (StringUtils.isEmpty(server.first()) && server.second() != DEFAULT_SSH_PORT) {
            return endpoints;
        }
        for (int i = 0; i < nodeMaps.size(); i++) {
            UserVmVO vm = nodeVms.get(i);
            if (!nodeMaps.get(i).isControlNode() || vm == null || !VirtualMachine.State.Running.equals(vm.getState())) {
                continue;
            }
            if (server.second() == DEFAULT_SSH_PORT) {
                endpoints.add(new Pair<>(vm.getPrivateIpAddress(), DEFAULT_SSH_PORT));
            } else {
                endpoints.add(new Pair<>(server.first(), server.second() + i));
            }
        }
        return endpoints;
    }

    protected boolean runOnControlNode(List<KubernetesClusterVmMapVO> nodeMaps, List<UserVmVO> nodeVms, String kubectlArgs) {
        if (controlEndpoints == null) {
            controlEndpoints = getRunningControlNodeEndpoints(nodeMaps, nodeVms);
            sshKeyFile = getManagementServerSshPublicKeyFile();
            try {
                loginUser = getControlNodeLoginUser();
            } catch (CloudRuntimeException e) {
                loginUser = CLUSTER_NODE_VM_USER;
            }
        }
        if (controlEndpoints.isEmpty()) {
            logger.debug("No running control node to update out-of-service taints on Kubernetes cluster {}", kubernetesCluster);
            return false;
        }
        String command = String.format("out=$(sudo /opt/bin/kubectl --request-timeout=15s %s 2>&1) || { echo \"$out\"; echo \"$out\" | grep -qE '%s'; }",
                kubectlArgs, NOT_FOUND_PATTERN);
        Iterator<Pair<String, Integer>> it = controlEndpoints.iterator();
        while (it.hasNext()) {
            Pair<String, Integer> endpoint = it.next();
            try {
                Pair<Boolean, String> result = SshHelper.sshExecute(endpoint.first(), endpoint.second(), loginUser,
                        sshKeyFile, null, command, 10000, 10000, 30000);
                if (Boolean.TRUE.equals(result.first())) {
                    return true;
                }
                logger.warn("Failed to run kubectl {} on control node {}:{} of Kubernetes cluster {}: {}",
                        kubectlArgs, endpoint.first(), endpoint.second(), kubernetesCluster, result.second());
            } catch (Exception e) {
                logger.warn("Unable to reach control node {}:{} of Kubernetes cluster {}: {}", endpoint.first(), endpoint.second(), kubernetesCluster, e.getMessage());
                logger.debug("SSH to control node failed", e);
                it.remove();
            }
        }
        return false;
    }
}
