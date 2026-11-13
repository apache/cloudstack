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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterUtil;
import com.cloud.kubernetes.version.KubernetesSupportedVersion;
import com.cloud.kubernetes.version.KubernetesVersionManagerImpl;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SshHelper;

public class KubernetesClusterUpgradeWorker extends KubernetesClusterActionWorker {

    /** Cluster detail holding the id of the Kubernetes version that an upgrade, not finished yet, is upgrading to. */
    public static final String UPGRADE_VERSION_DETAIL = "upgradeKubernetesVersionId";
    /** Prints the STATUS of a node, such as Ready, NotReady or Ready,SchedulingDisabled. */
    protected static final String NODE_STATUS_COMMAND = "sudo /opt/bin/kubectl get nodes | awk '{if ($1 == \"%s\") print $2}'";

    protected List<UserVm> clusterVMs = new ArrayList<>();
    private KubernetesSupportedVersion upgradeVersion;
    private final String upgradeScriptFilename = "upgrade-kubernetes.sh";
    private File upgradeScriptFile;
    private long upgradeTimeoutTime;

    public KubernetesClusterUpgradeWorker(final KubernetesCluster kubernetesCluster,
                                          final KubernetesSupportedVersion upgradeVersion,
                                          final KubernetesClusterManagerImpl clusterManager,
                                          final String[] keys) {
        super(kubernetesCluster, clusterManager);
        this.upgradeVersion = upgradeVersion;
        this.keys = keys;
    }

    protected void retrieveScriptFiles() {
        super.retrieveScriptFiles();
        upgradeScriptFile = retrieveScriptFile(upgradeScriptFilename);
    }

    private Pair<Boolean, String> runInstallScriptOnVM(final UserVm vm, final int index) throws Exception {
        Pair<String, Integer> nodeSshAddress = getNodeSshAddress(vm, index);
        String nodeAddress = nodeSshAddress.first();
        int nodeSshPort = nodeSshAddress.second();
        SshHelper.scpTo(nodeAddress, nodeSshPort, getControlNodeLoginUser(), sshKeyFile, null,
                "~/", upgradeScriptFile.getAbsolutePath(), "0755");
        String cmdStr = String.format("sudo ./%s %s %s %s %s %s",
                upgradeScriptFile.getName(),
                upgradeVersion.getSemanticVersion(),
                index == 0 ? "true" : "false",
                KubernetesVersionManagerImpl.compareSemanticVersions(upgradeVersion.getSemanticVersion(), "1.15.0") < 0 ? "true" : "false",
                Hypervisor.HypervisorType.VMware.equals(vm.getHypervisorType()), Objects.isNull(kubernetesCluster.getCniConfigId()));
        return SshHelper.sshExecute(nodeAddress, nodeSshPort, getControlNodeLoginUser(), sshKeyFile, null,
                cmdStr,
                10000, 10000, 10 * 60 * 1000);
    }

    private void upgradeKubernetesClusterNodes() {
        for (int i = 0; i < clusterVMs.size(); ++i) {
            UserVm vm = clusterVMs.get(i);
            String hostName = getNodeName(vm);
            Pair<Boolean, String> result;
            if (isNodeUpgraded(vm, hostName)) {
                // A previous, failed, upgrade to this version got this far. On the first control node this also
                // means that kubeadm upgrade apply has run, so the other nodes only need kubeadm upgrade node.
                logger.info("Skipping node {} on VM {} in Kubernetes cluster {}, it is already at Kubernetes version {}", hostName, vm, kubernetesCluster, upgradeVersion);
                continue;
            }
            if (logger.isInfoEnabled()) {
                logger.info("Upgrading node {} on VM {} in Kubernetes cluster {} with Kubernetes version {}", hostName, vm, kubernetesCluster, upgradeVersion);
            }
            if (System.currentTimeMillis() > upgradeTimeoutTime) {
                logTransitStateDetachIsoAndThrow(Level.ERROR, String.format("Failed to upgrade Kubernetes cluster : %s, upgrade action timed out on Kubernetes node : %s on VM : %s", kubernetesCluster.getName(), hostName, vm.getDisplayName()), kubernetesCluster, clusterVMs, KubernetesCluster.Event.OperationFailed, null);
            }
            // A node cordoned by someone else stays cordoned if the upgrade stops before upgrading it
            final List<UserVm> toUncordon = isNodeCordoned(vm) ? List.of() : List.of(vm);
            Pair<Boolean, String> drained = drainKubernetesClusterNode(publicIpAddress, sshPort, vm,
                    KubernetesClusterService.KubernetesClusterUpgradeRetries.value() + 1, DRAIN_RETRY_WAIT, upgradeTimeoutTime);
            if (!drained.first()) {
                uncordonNodes(toUncordon);
                logTransitStateDetachIsoAndThrow(Level.ERROR, String.format("Failed to upgrade Kubernetes cluster : %s, unable to drain Kubernetes node : %s on VM : %s: %s",
                        kubernetesCluster.getName(), hostName, vm.getDisplayName(), drained.second()), kubernetesCluster, clusterVMs, KubernetesCluster.Event.OperationFailed, null);
            }
            if (System.currentTimeMillis() > upgradeTimeoutTime) {
                // Nothing has been changed on the node, so don't leave it unschedulable
                uncordonNodes(toUncordon);
                logTransitStateDetachIsoAndThrow(Level.ERROR, String.format("Failed to upgrade Kubernetes cluster : %s, upgrade action timed out on Kubernetes node : %s on VM : %s", kubernetesCluster.getName(), hostName, vm.getDisplayName()), kubernetesCluster, clusterVMs, KubernetesCluster.Event.OperationFailed, null);
            }
            String errorMessage = String.format("Failed to upgrade Kubernetes cluster : %s, unable to upgrade Kubernetes node : %s on VM : %s", kubernetesCluster.getName(), hostName, vm.getDisplayName());
            for (int retry = KubernetesClusterService.KubernetesClusterUpgradeRetries.value(); retry >= 0; retry--) {
                try {
                    deployProvider();
                    result = runInstallScriptOnVM(vm, i);
                    if (result.first()) {
                        break;
                    }
                    if (retry > 0) {
                        logger.error(String.format("%s, retries left: %s", errorMessage, retry));
                    } else {
                        logTransitStateDetachIsoAndThrow(Level.ERROR, errorMessage, kubernetesCluster, clusterVMs, KubernetesCluster.Event.OperationFailed, null);
                    }
                } catch (Exception e) {
                    if (retry > 0) {
                        logger.error(String.format("%s due to %s, retries left: %s", errorMessage, e, retry));
                    } else {
                        logTransitStateDetachIsoAndThrow(Level.ERROR, errorMessage, kubernetesCluster, clusterVMs, KubernetesCluster.Event.OperationFailed, e);
                    }
                }
            }
            if (System.currentTimeMillis() > upgradeTimeoutTime) {
                // The node has been upgraded, so don't leave it unschedulable, unless someone else cordoned it
                uncordonNodes(toUncordon);
                logTransitStateDetachIsoAndThrow(Level.ERROR, String.format("Failed to upgrade Kubernetes cluster : %s, upgrade action timed out on Kubernetes node : %s on VM : %s", kubernetesCluster.getName(), hostName, vm.getDisplayName()), kubernetesCluster, clusterVMs, KubernetesCluster.Event.OperationFailed, null);
            }
            if (!KubernetesClusterUtil.uncordonKubernetesClusterNode(kubernetesCluster, publicIpAddress, sshPort, getControlNodeLoginUser(), getManagementServerSshPublicKeyFile(), vm, upgradeTimeoutTime, 15000)) {
                logTransitStateDetachIsoAndThrow(Level.ERROR, String.format("Failed to upgrade Kubernetes cluster : %s, unable to uncordon Kubernetes node : %s on VM : %s", kubernetesCluster.getName(), hostName, vm.getDisplayName()), kubernetesCluster, clusterVMs, KubernetesCluster.Event.OperationFailed, null);
            }
            // Wait for every node, not only the first control node, to be Ready before the next one is drained
            if (!KubernetesClusterUtil.isKubernetesClusterNodeReady(kubernetesCluster, publicIpAddress, sshPort, getControlNodeLoginUser(), getManagementServerSshPublicKeyFile(), hostName, upgradeTimeoutTime, 15000)) {
                logTransitStateDetachIsoAndThrow(Level.ERROR, String.format("Failed to upgrade Kubernetes cluster : %s, unable to get Kubernetes node : %s on VM : %s in ready state", kubernetesCluster.getName(), hostName, vm.getDisplayName()), kubernetesCluster, clusterVMs, KubernetesCluster.Event.OperationFailed, null);
            }
            if (!KubernetesClusterUtil.clusterNodeVersionMatches(upgradeVersion.getSemanticVersion(), publicIpAddress, sshPort, getControlNodeLoginUser(), getManagementServerSshPublicKeyFile(), hostName, upgradeTimeoutTime, 15000, vm.getId(), kubernetesClusterVmMapDao)) {
                logTransitStateDetachIsoAndThrow(Level.ERROR, String.format("Failed to upgrade Kubernetes cluster : %s, unable to get Kubernetes node : %s on VM : %s upgraded to version %s", kubernetesCluster.getName(), hostName, vm.getDisplayName(), upgradeVersion.getSemanticVersion()), kubernetesCluster, clusterVMs, KubernetesCluster.Event.OperationFailed, null);
            }
            if (logger.isInfoEnabled()) {
                logger.info("Successfully upgraded node on VM {} in Kubernetes cluster {} with Kubernetes version {}", vm, kubernetesCluster, upgradeVersion);
            }
        }
    }

    /**
     * Tells whether a node was upgraded to the target version by an earlier upgrade of the cluster that failed on a
     * later node, so that a retry can carry on from where that one stopped. The version recorded for the node is
     * only updated once the node has been upgraded, uncordoned and found Ready, but it is not trusted on its own:
     * the node must also report the target version to Kubernetes, and still be Ready and schedulable.
     */
    protected boolean isNodeUpgraded(final UserVm vm, final String hostName) {
        KubernetesClusterVmMapVO vmMapVO = kubernetesClusterVmMapDao.getClusterMapFromVmId(vm.getId());
        String version = upgradeVersion.getSemanticVersion();
        if (vmMapVO == null || !KubernetesClusterUtil.isSameNodeVersion(vmMapVO.getNodeVersion(), version)) {
            return false;
        }
        try {
            Pair<Boolean, String> result = executeSshCommand(publicIpAddress, sshPort,
                    String.format(KubernetesClusterUtil.CLUSTER_NODE_VERSION_COMMAND, hostName), 10000, 10000, 20000);
            if (!Boolean.TRUE.equals(KubernetesClusterUtil.clusterNodeVersionMatches(result, version).first())) {
                logger.info("Node {} on VM {} in Kubernetes cluster {} is recorded at Kubernetes version {}, but reports: {}",
                        hostName, vm, kubernetesCluster, version, result.second());
                return false;
            }
            // Only a node that is also Ready is done; any other one is upgraded again. A node that is Ready but
            // cordoned (Ready,SchedulingDisabled) was cordoned by someone else after its upgrade, and is left as it is.
            result = executeSshCommand(publicIpAddress, sshPort,
                    String.format(NODE_STATUS_COMMAND, hostName), 10000, 10000, 20000);
            if (Boolean.TRUE.equals(result.first()) && result.second() != null &&
                    result.second().lines().anyMatch(line -> Arrays.asList(line.trim().split(",")).contains("Ready"))) {
                return true;
            }
            logger.info("Node {} on VM {} in Kubernetes cluster {} is at Kubernetes version {}, but is not Ready, so it is upgraded again",
                    hostName, vm, kubernetesCluster, version);
        } catch (Exception e) {
            logger.warn("Unable to get the Kubernetes version of node {} on VM {} in Kubernetes cluster {}", hostName, vm, kubernetesCluster, e);
        }
        return false;
    }

    public boolean upgradeCluster() throws CloudRuntimeException {
        init();
        if (logger.isInfoEnabled()) {
            logger.info("Upgrading Kubernetes cluster: {}", kubernetesCluster);
        }
        upgradeTimeoutTime = System.currentTimeMillis() + KubernetesClusterService.KubernetesClusterUpgradeTimeout.value() * 1000;
        Pair<String, Integer> publicIpSshPort = getKubernetesClusterServerIpSshPort(null);
        publicIpAddress = publicIpSshPort.first();
        sshPort = publicIpSshPort.second();
        if (StringUtils.isEmpty(publicIpAddress)) {
            logAndThrow(Level.ERROR, String.format("Upgrade failed for Kubernetes cluster: %s, unable to retrieve associated public IP", kubernetesCluster));
        }
        clusterVMs = getKubernetesClusterVMs();
        if (CollectionUtils.isEmpty(clusterVMs)) {
            logAndThrow(Level.ERROR, String.format("Upgrade failed for Kubernetes cluster: %s, unable to retrieve VMs for cluster", kubernetesCluster));
        }
        filterOutManualUpgradeNodesFromClusterUpgrade();
        retrieveScriptFiles();
        transitToOperationState(KubernetesCluster.Event.UpgradeRequested);
        // Kept until the cluster is Running again (see stateTransitTo), so that a failed upgrade can be told apart
        // from other causes of Alert
        kubernetesClusterDetailsDao.addDetail(kubernetesCluster.getId(), UPGRADE_VERSION_DETAIL, String.valueOf(upgradeVersion.getId()), false);
        attachIsoKubernetesVMs(clusterVMs, upgradeVersion);
        upgradeKubernetesClusterNodes();
        detachIsoKubernetesVMs(clusterVMs);
        KubernetesClusterVO kubernetesClusterVO = kubernetesClusterDao.findById(kubernetesCluster.getId());
        kubernetesClusterVO.setKubernetesVersionId(upgradeVersion.getId());
        boolean updated = kubernetesClusterDao.update(kubernetesCluster.getId(), kubernetesClusterVO);
        if (!updated) {
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        } else {
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
        }
        return updated;
    }

    protected void filterOutManualUpgradeNodesFromClusterUpgrade() {
        if (CollectionUtils.isEmpty(clusterVMs)) {
            return;
        }
        clusterVMs = clusterVMs.stream().filter(x -> {
            KubernetesClusterVmMapVO mapVO = kubernetesClusterVmMapDao.getClusterMapFromVmId(x.getId());
            return mapVO != null && !mapVO.isManualUpgrade();
        }).collect(Collectors.toList());
    }
}
