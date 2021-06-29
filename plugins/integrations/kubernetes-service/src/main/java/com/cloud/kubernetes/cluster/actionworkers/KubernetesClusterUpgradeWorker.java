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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Level;

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
import com.google.common.base.Strings;

public class KubernetesClusterUpgradeWorker extends KubernetesClusterActionWorker {

    private List<UserVm> clusterVMs = new ArrayList<>();
    private KubernetesSupportedVersion upgradeVersion;
    private File upgradeScriptFile;
    private long upgradeTimeoutTime;

    public KubernetesClusterUpgradeWorker(final KubernetesCluster kubernetesCluster,
                                          final KubernetesSupportedVersion upgradeVersion,
                                          final KubernetesClusterManagerImpl clusterManager) {
        super(kubernetesCluster, clusterManager);
        this.upgradeVersion = upgradeVersion;
    }

    private void retrieveUpgradeScriptFile() {
        try {
            String upgradeScriptData = readResourceFile("/script/upgrade-kubernetes.sh");
            upgradeScriptFile = File.createTempFile("upgrade-kuberntes", ".sh");
            BufferedWriter upgradeScriptFileWriter = new BufferedWriter(new FileWriter(upgradeScriptFile));
            upgradeScriptFileWriter.write(upgradeScriptData);
            upgradeScriptFileWriter.close();
        } catch (IOException e) {
            logAndThrow(Level.ERROR, String.format("Failed to upgrade Kubernetes cluster : %s, unable to prepare upgrade script", kubernetesCluster.getName()), e);
        }
    }

    private Pair<Boolean, String> runInstallScriptOnVM(final UserVm vm, final int index) throws Exception {
        int nodeSshPort = sshPort == 22 ? sshPort : sshPort + index;
        String nodeAddress = (index > 0 && sshPort == 22) ? vm.getPrivateIpAddress() : publicIpAddress;
        SshHelper.scpTo(nodeAddress, nodeSshPort, CLUSTER_NODE_VM_USER, sshKeyFile, null,
                "~/", upgradeScriptFile.getAbsolutePath(), "0755");
        String cmdStr = String.format("sudo ./%s %s %s %s %s",
                upgradeScriptFile.getName(),
                upgradeVersion.getSemanticVersion(),
                index == 0 ? "true" : "false",
                KubernetesVersionManagerImpl.compareSemanticVersions(upgradeVersion.getSemanticVersion(), "1.15.0") < 0 ? "true" : "false",
                Hypervisor.HypervisorType.VMware.equals(vm.getHypervisorType()));
        return SshHelper.sshExecute(nodeAddress, nodeSshPort, CLUSTER_NODE_VM_USER, sshKeyFile, null,
                cmdStr,
                10000, 10000, 10 * 60 * 1000);
    }

    private void upgradeKubernetesClusterNodes() {
        Pair<Boolean, String> result = null;
        for (int i = 0; i < clusterVMs.size(); ++i) {
            UserVm vm = clusterVMs.get(i);
            String hostName = vm.getHostName();
            if (!Strings.isNullOrEmpty(hostName)) {
                hostName = hostName.toLowerCase();
            }
            result = null;
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Upgrading node on VM ID: %s in Kubernetes cluster ID: %s with Kubernetes version(%s) ID: %s",
                        vm.getUuid(), kubernetesCluster.getUuid(), upgradeVersion.getSemanticVersion(), upgradeVersion.getUuid()));
            }
            try {
                result = SshHelper.sshExecute(publicIpAddress, sshPort, CLUSTER_NODE_VM_USER, sshKeyFile, null,
                        String.format("sudo kubectl drain %s --ignore-daemonsets --delete-local-data", hostName),
                        10000, 10000, 60000);
            } catch (Exception e) {
                logTransitStateDetachIsoAndThrow(Level.ERROR, String.format("Failed to upgrade Kubernetes cluster : %s, unable to drain Kubernetes node on VM : %s", kubernetesCluster.getName(), vm.getDisplayName()), kubernetesCluster, clusterVMs, KubernetesCluster.Event.OperationFailed, e);
            }
            if (!result.first()) {
                logTransitStateDetachIsoAndThrow(Level.ERROR, String.format("Failed to upgrade Kubernetes cluster : %s, unable to drain Kubernetes node on VM : %s", kubernetesCluster.getName(), vm.getDisplayName()), kubernetesCluster, clusterVMs, KubernetesCluster.Event.OperationFailed, null);
            }
            if (System.currentTimeMillis() > upgradeTimeoutTime) {
                logTransitStateDetachIsoAndThrow(Level.ERROR, String.format("Failed to upgrade Kubernetes cluster : %s, upgrade action timed out", kubernetesCluster.getName()), kubernetesCluster, clusterVMs, KubernetesCluster.Event.OperationFailed, null);
            }
            try {
                result = runInstallScriptOnVM(vm, i);
            } catch (Exception e) {
                logTransitStateDetachIsoAndThrow(Level.ERROR, String.format("Failed to upgrade Kubernetes cluster : %s, unable to upgrade Kubernetes node on VM : %s", kubernetesCluster.getName(), vm.getDisplayName()), kubernetesCluster, clusterVMs, KubernetesCluster.Event.OperationFailed, e);
            }
            if (!result.first()) {
                logTransitStateDetachIsoAndThrow(Level.ERROR, String.format("Failed to upgrade Kubernetes cluster : %s, unable to upgrade Kubernetes node on VM : %s", kubernetesCluster.getName(), vm.getDisplayName()), kubernetesCluster, clusterVMs, KubernetesCluster.Event.OperationFailed, null);
            }
            if (System.currentTimeMillis() > upgradeTimeoutTime) {
                logTransitStateDetachIsoAndThrow(Level.ERROR, String.format("Failed to upgrade Kubernetes cluster : %s, upgrade action timed out", kubernetesCluster.getName()), kubernetesCluster, clusterVMs, KubernetesCluster.Event.OperationFailed, null);
            }
            if (!KubernetesClusterUtil.uncordonKubernetesClusterNode(kubernetesCluster, publicIpAddress, sshPort, CLUSTER_NODE_VM_USER, getManagementServerSshPublicKeyFile(), vm, upgradeTimeoutTime, 15000)) {
                logTransitStateDetachIsoAndThrow(Level.ERROR, String.format("Failed to upgrade Kubernetes cluster : %s, unable to uncordon Kubernetes node on VM : %s", kubernetesCluster.getName(), vm.getDisplayName()), kubernetesCluster, clusterVMs, KubernetesCluster.Event.OperationFailed, null);
            }
            if (i == 0) { // Wait for control node to get in Ready state
                if (!KubernetesClusterUtil.isKubernetesClusterNodeReady(kubernetesCluster, publicIpAddress, sshPort, CLUSTER_NODE_VM_USER, getManagementServerSshPublicKeyFile(), hostName, upgradeTimeoutTime, 15000)) {
                    logTransitStateDetachIsoAndThrow(Level.ERROR, String.format("Failed to upgrade Kubernetes cluster : %s, unable to get control Kubernetes node on VM : %s in ready state", kubernetesCluster.getName(), vm.getDisplayName()), kubernetesCluster, clusterVMs, KubernetesCluster.Event.OperationFailed, null);
                }
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Successfully upgraded node on VM ID: %s in Kubernetes cluster ID: %s with Kubernetes version(%s) ID: %s",
                        vm.getUuid(), kubernetesCluster.getUuid(), upgradeVersion.getSemanticVersion(), upgradeVersion.getUuid()));
            }
        }
    }

    public boolean upgradeCluster() throws CloudRuntimeException {
        init();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Upgrading Kubernetes cluster : %s", kubernetesCluster.getName()));
        }
        upgradeTimeoutTime = System.currentTimeMillis() + KubernetesClusterService.KubernetesClusterUpgradeTimeout.value() * 1000;
        Pair<String, Integer> publicIpSshPort = getKubernetesClusterServerIpSshPort(null);
        publicIpAddress = publicIpSshPort.first();
        sshPort = publicIpSshPort.second();
        if (Strings.isNullOrEmpty(publicIpAddress)) {
            logAndThrow(Level.ERROR, String.format("Upgrade failed for Kubernetes cluster : %s, unable to retrieve associated public IP", kubernetesCluster.getName()));
        }
        clusterVMs = getKubernetesClusterVMs();
        if (CollectionUtils.isEmpty(clusterVMs)) {
            logAndThrow(Level.ERROR, String.format("Upgrade failed for Kubernetes cluster : %s, unable to retrieve VMs for cluster", kubernetesCluster.getName()));
        }
        retrieveUpgradeScriptFile();
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.UpgradeRequested);
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
}
