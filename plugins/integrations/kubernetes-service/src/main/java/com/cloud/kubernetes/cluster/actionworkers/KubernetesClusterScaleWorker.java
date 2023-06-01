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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.api.InternalIdentity;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.dc.DataCenter;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterUtil;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.rules.FirewallRule;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.LaunchPermissionVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.logging.log4j.Level;

public class KubernetesClusterScaleWorker extends KubernetesClusterResourceModifierActionWorker {

    @Inject
    protected VMInstanceDao vmInstanceDao;

    private ServiceOffering serviceOffering;
    private Long clusterSize;
    private List<Long> nodeIds;
    private KubernetesCluster.State originalState;
    private Network network;
    private Long minSize;
    private Long maxSize;
    private Boolean isAutoscalingEnabled;
    private long scaleTimeoutTime;

    public KubernetesClusterScaleWorker(final KubernetesCluster kubernetesCluster,
                                        final ServiceOffering serviceOffering,
                                        final Long clusterSize,
                                        final List<Long> nodeIds,
                                        final Boolean isAutoscalingEnabled,
                                        final Long minSize,
                                        final Long maxSize,
                                        final KubernetesClusterManagerImpl clusterManager) {
        super(kubernetesCluster, clusterManager);
        this.serviceOffering = serviceOffering;
        this.nodeIds = nodeIds;
        this.isAutoscalingEnabled = isAutoscalingEnabled;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.originalState = kubernetesCluster.getState();
        if (this.nodeIds != null) {
            this.clusterSize = kubernetesCluster.getNodeCount() - this.nodeIds.size();
        } else {
            this.clusterSize = clusterSize;
        }

    }

    protected void init() {
        super.init();
        this.network = networkDao.findById(kubernetesCluster.getNetworkId());
    }

    private void logTransitStateToFailedIfNeededAndThrow(final Level logLevel, final String message, final Exception e) throws CloudRuntimeException {
        KubernetesCluster cluster = kubernetesClusterDao.findById(kubernetesCluster.getId());
        if (cluster != null && KubernetesCluster.State.Scaling.equals(cluster.getState())) {
            logTransitStateAndThrow(logLevel, message, kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed, e);
        } else {
            logAndThrow(logLevel, message, e);
        }
    }

    private void logTransitStateToFailedIfNeededAndThrow(final Level logLevel, final String message) throws CloudRuntimeException {
        logTransitStateToFailedIfNeededAndThrow(logLevel, message, null);
    }

    private void scaleKubernetesClusterIsolatedNetworkRules(final List<Long> clusterVMIds) throws ManagementServerException {
        IpAddress publicIp = getNetworkSourceNatIp(network);
        if (publicIp == null) {
            throw new ManagementServerException(String.format("No source NAT IP addresses found for network : %s, Kubernetes cluster : %s", network.getName(), kubernetesCluster.getName()));
        }

        // Remove existing SSH firewall rules
        FirewallRule firewallRule = removeSshFirewallRule(publicIp);
        if (firewallRule == null) {
            throw new ManagementServerException("Firewall rule for node SSH access can't be provisioned");
        }
        int existingFirewallRuleSourcePortEnd = firewallRule.getSourcePortEnd();
        try {
            removePortForwardingRules(publicIp, network, owner, CLUSTER_NODES_DEFAULT_START_SSH_PORT, existingFirewallRuleSourcePortEnd);
        } catch (ResourceUnavailableException e) {
            throw new ManagementServerException(String.format("Failed to remove SSH port forwarding rules for removed VMs for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
        setupKubernetesClusterIsolatedNetworkRules(publicIp, network, clusterVMIds, false);
    }

    private void scaleKubernetesClusterVpcTierRules(final List<Long> clusterVMIds) throws ManagementServerException {
        IpAddress publicIp = getVpcTierKubernetesPublicIp(network);
        if (publicIp == null) {
            throw new ManagementServerException(String.format("No public IP addresses found for VPC tier : %s, Kubernetes cluster : %s", network.getName(), kubernetesCluster.getName()));
        }
        try {
            removePortForwardingRules(publicIp, network, owner, CLUSTER_NODES_DEFAULT_START_SSH_PORT, CLUSTER_NODES_DEFAULT_START_SSH_PORT + clusterVMIds.size() - 1);
        } catch (ResourceUnavailableException e) {
            throw new ManagementServerException(String.format("Failed to remove SSH port forwarding rules for removed VMs for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
        // Add port forwarding rule for SSH access on each node VM
        try {
            provisionSshPortForwardingRules(publicIp, network, owner, clusterVMIds);
        } catch (ResourceUnavailableException | NetworkRuleConflictException e) {
            throw new ManagementServerException(String.format("Failed to activate SSH port forwarding rules for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
    }

    /**
     * Scale network rules for an existing Kubernetes cluster while scaling it
     * Open up firewall for SSH access from port NODES_DEFAULT_START_SSH_PORT to NODES_DEFAULT_START_SSH_PORT+n.
     * Also remove port forwarding rules for all virtual machines and re-create port-forwarding rule
     * to forward public IP traffic to all node VMs' private IP.
     * @param clusterVMIds
     * @throws ManagementServerException
     */
    private void scaleKubernetesClusterNetworkRules(final List<Long> clusterVMIds) throws ManagementServerException {
        if (!Network.GuestType.Isolated.equals(network.getGuestType())) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Network : %s for Kubernetes cluster : %s is not an isolated network, therefore, no need for network rules", network.getName(), kubernetesCluster.getName()));
            }
            return;
        }
        if (network.getVpcId() != null) {
            scaleKubernetesClusterVpcTierRules(clusterVMIds);
            return;
        }
        scaleKubernetesClusterIsolatedNetworkRules(clusterVMIds);
    }

    private KubernetesClusterVO updateKubernetesClusterEntry(final Long newSize, final ServiceOffering newServiceOffering) throws CloudRuntimeException {
        final ServiceOffering serviceOffering = newServiceOffering == null ?
                serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId()) : newServiceOffering;
        final Long serviceOfferingId = newServiceOffering == null ? null : serviceOffering.getId();
        final long size = newSize == null ? kubernetesCluster.getTotalNodeCount() : (newSize + kubernetesCluster.getControlNodeCount());
        final long cores = serviceOffering.getCpu() * size;
        final long memory = serviceOffering.getRamSize() * size;
        KubernetesClusterVO kubernetesClusterVO = updateKubernetesClusterEntry(cores, memory, newSize, serviceOfferingId,
            kubernetesCluster.getAutoscalingEnabled(), kubernetesCluster.getMinSize(), kubernetesCluster.getMaxSize());
        if (kubernetesClusterVO == null) {
            logTransitStateAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster %s failed, unable to update Kubernetes cluster",
                    kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        }
        return kubernetesClusterVO;
    }

    private boolean removeKubernetesClusterNode(final String ipAddress, final int port, final UserVm userVm, final int retries, final int waitDuration) {
        File pkFile = getManagementServerSshPublicKeyFile();
        int retryCounter = 0;
        String hostName = userVm.getHostName();
        if (StringUtils.isNotEmpty(hostName)) {
            hostName = hostName.toLowerCase();
        }
        while (retryCounter < retries) {
            retryCounter++;
            try {
                Pair<Boolean, String> result = SshHelper.sshExecute(ipAddress, port, getControlNodeLoginUser(),
                        pkFile, null, String.format("sudo /opt/bin/kubectl drain %s --ignore-daemonsets --delete-local-data", hostName),
                        10000, 10000, 60000);
                if (!result.first()) {
                    logger.warn(String.format("Draining node: %s on VM : %s in Kubernetes cluster : %s unsuccessful", hostName, userVm.getDisplayName(), kubernetesCluster.getName()));
                } else {
                    result = SshHelper.sshExecute(ipAddress, port, getControlNodeLoginUser(),
                            pkFile, null, String.format("sudo /opt/bin/kubectl delete node %s", hostName),
                            10000, 10000, 30000);
                    if (result.first()) {
                        return true;
                    } else {
                        logger.warn(String.format("Deleting node: %s on VM : %s in Kubernetes cluster : %s unsuccessful", hostName, userVm.getDisplayName(), kubernetesCluster.getName()));
                    }
                }
                break;
            } catch (Exception e) {
                String msg = String.format("Failed to remove Kubernetes cluster : %s node: %s on VM : %s", kubernetesCluster.getName(), hostName, userVm.getDisplayName());
                logger.warn(msg, e);
            }
            try {
                Thread.sleep(waitDuration);
            } catch (InterruptedException ie) {
                logger.error(String.format("Error while waiting for Kubernetes cluster : %s node: %s on VM : %s removal", kubernetesCluster.getName(), hostName, userVm.getDisplayName()), ie);
            }
            retryCounter++;
        }
        return false;
    }

    private void validateKubernetesClusterScaleOfferingParameters() throws CloudRuntimeException {
        if (KubernetesCluster.State.Created.equals(originalState)) {
            return;
        }
        final long originalNodeCount = kubernetesCluster.getTotalNodeCount();
        List<KubernetesClusterVmMapVO> vmList = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());
        if (vmList == null || vmList.isEmpty() || vmList.size() < originalNodeCount) {
            logTransitStateToFailedIfNeededAndThrow(Level.WARN, String.format("Scaling Kubernetes cluster : %s failed, it is in unstable state as not enough existing VM instances found!", kubernetesCluster.getName()));
        } else {
            for (KubernetesClusterVmMapVO vmMapVO : vmList) {
                VMInstanceVO vmInstance = vmInstanceDao.findById(vmMapVO.getVmId());
                if (vmInstance != null && vmInstance.getState().equals(VirtualMachine.State.Running) &&
                        vmInstance.getHypervisorType() != Hypervisor.HypervisorType.XenServer &&
                        vmInstance.getHypervisorType() != Hypervisor.HypervisorType.VMware &&
                        vmInstance.getHypervisorType() != Hypervisor.HypervisorType.Simulator) {
                    logTransitStateToFailedIfNeededAndThrow(Level.WARN, String.format("Scaling Kubernetes cluster : %s failed, scaling Kubernetes cluster with running VMs on hypervisor %s is not supported!", kubernetesCluster.getName(), vmInstance.getHypervisorType()));
                }
            }
        }
    }

    private void validateKubernetesClusterScaleSizeParameters() throws CloudRuntimeException {
        final long originalClusterSize = kubernetesCluster.getNodeCount();
        if (network == null) {
            logTransitStateToFailedIfNeededAndThrow(Level.WARN, String.format("Scaling failed for Kubernetes cluster : %s, cluster network not found", kubernetesCluster.getName()));
        }
        // Check capacity and transition state
        final long newVmRequiredCount = clusterSize - originalClusterSize;
        final ServiceOffering clusterServiceOffering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
        if (clusterServiceOffering == null) {
            logTransitStateToFailedIfNeededAndThrow(Level.WARN, String.format("Scaling failed for Kubernetes cluster : %s, cluster service offering not found", kubernetesCluster.getName()));
        }
        if (newVmRequiredCount > 0) {
            final DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
            try {
                if (originalState.equals(KubernetesCluster.State.Running)) {
                    plan(newVmRequiredCount, zone, clusterServiceOffering);
                } else {
                    plan(kubernetesCluster.getTotalNodeCount() + newVmRequiredCount, zone, clusterServiceOffering);
                }
            } catch (InsufficientCapacityException e) {
                logTransitStateToFailedIfNeededAndThrow(Level.WARN, String.format("Scaling failed for Kubernetes cluster : %s in zone : %s, insufficient capacity", kubernetesCluster.getName(), zone.getName()));
            }
        }
        List<KubernetesClusterVmMapVO> vmList = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());
        if (CollectionUtils.isEmpty(vmList) || vmList.size() < kubernetesCluster.getTotalNodeCount()) {
            logTransitStateToFailedIfNeededAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes cluster : %s, it is in unstable state as not enough existing VM instances found", kubernetesCluster.getName()));
        }
    }

    private void scaleKubernetesClusterOffering() throws CloudRuntimeException {
        validateKubernetesClusterScaleOfferingParameters();
        if (!kubernetesCluster.getState().equals(KubernetesCluster.State.Scaling)) {
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.ScaleUpRequested);
        }
        if (KubernetesCluster.State.Created.equals(originalState)) {
            kubernetesCluster = updateKubernetesClusterEntry(null, serviceOffering);
            return;
        }
        final long size = kubernetesCluster.getTotalNodeCount();
        List<KubernetesClusterVmMapVO> vmList = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());
        final long tobeScaledVMCount =  Math.min(vmList.size(), size);
        for (long i = 0; i < tobeScaledVMCount; i++) {
            KubernetesClusterVmMapVO vmMapVO = vmList.get((int) i);
            UserVmVO userVM = userVmDao.findById(vmMapVO.getVmId());
            boolean result = false;
            try {
                result = userVmManager.upgradeVirtualMachine(userVM.getId(), serviceOffering.getId(), new HashMap<String, String>());
            } catch (ResourceUnavailableException | ManagementServerException | ConcurrentOperationException | VirtualMachineMigrationException e) {
                logTransitStateAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster : %s failed, unable to scale cluster VM : %s", kubernetesCluster.getName(), userVM.getDisplayName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed, e);
            }
            if (!result) {
                logTransitStateAndThrow(Level.WARN, String.format("Scaling Kubernetes cluster : %s failed, unable to scale cluster VM : %s", kubernetesCluster.getName(), userVM.getDisplayName()),kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            }
            if (System.currentTimeMillis() > scaleTimeoutTime) {
                logTransitStateAndThrow(Level.WARN, String.format("Scaling Kubernetes cluster : %s failed, scaling action timed out", kubernetesCluster.getName()),kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            }
        }
        kubernetesCluster = updateKubernetesClusterEntry(null, serviceOffering);
    }

    private void removeNodesFromCluster(List<KubernetesClusterVmMapVO> vmMaps) throws CloudRuntimeException {
        for (KubernetesClusterVmMapVO vmMapVO : vmMaps) {
            UserVmVO userVM = userVmDao.findById(vmMapVO.getVmId());
            logger.info(String.format("Removing vm : %s from cluster %s", userVM.getDisplayName(), kubernetesCluster.getName()));
            if (!removeKubernetesClusterNode(publicIpAddress, sshPort, userVM, 3, 30000)) {
                logTransitStateAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes cluster : %s, failed to remove Kubernetes node: %s running on VM : %s", kubernetesCluster.getName(), userVM.getHostName(), userVM.getDisplayName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            }
            try {
                UserVm vm = userVmService.destroyVm(userVM.getId(), true);
                if (!userVmManager.expunge(userVM)) {
                    logTransitStateAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster %s failed, unable to expunge VM '%s'."
                        , kubernetesCluster.getName(), vm.getDisplayName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
                }
            } catch (ResourceUnavailableException e) {
                logTransitStateAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster %s failed, unable to remove VM ID: %s",
                    kubernetesCluster.getName() , userVM.getDisplayName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed, e);
            }
            kubernetesClusterVmMapDao.expunge(vmMapVO.getId());
            if (System.currentTimeMillis() > scaleTimeoutTime) {
                logTransitStateAndThrow(Level.WARN, String.format("Scaling Kubernetes cluster %s failed, scaling action timed out", kubernetesCluster.getName()),kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            }
        }

        // Scale network rules to update firewall rule
        try {
            List<Long> clusterVMIds = getKubernetesClusterVMMaps().stream().map(KubernetesClusterVmMapVO::getVmId).collect(Collectors.toList());
            scaleKubernetesClusterNetworkRules(clusterVMIds);
        } catch (ManagementServerException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes cluster : %s, unable to update network rules", kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed, e);
        }
    }

    private void scaleDownKubernetesClusterSize() throws CloudRuntimeException {
        if (!kubernetesCluster.getState().equals(KubernetesCluster.State.Scaling)) {
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.ScaleDownRequested);
        }
        List<KubernetesClusterVmMapVO> vmList;
        if (this.nodeIds != null) {
            vmList = getKubernetesClusterVMMapsForNodes(this.nodeIds);
        } else {
            vmList  = getKubernetesClusterVMMaps();
            vmList = vmList.subList((int) (kubernetesCluster.getControlNodeCount() + clusterSize), vmList.size());
        }
        Collections.reverse(vmList);
        removeNodesFromCluster(vmList);
    }

    private void scaleUpKubernetesClusterSize(final long newVmCount) throws CloudRuntimeException {
        if (!kubernetesCluster.getState().equals(KubernetesCluster.State.Scaling)) {
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.ScaleUpRequested);
        }
        List<UserVm> clusterVMs = new ArrayList<>();
        LaunchPermissionVO launchPermission =  new LaunchPermissionVO(clusterTemplate.getId(), owner.getId());
        launchPermissionDao.persist(launchPermission);
        try {
            clusterVMs = provisionKubernetesClusterNodeVms((int)(newVmCount + kubernetesCluster.getNodeCount()), (int)kubernetesCluster.getNodeCount(), publicIpAddress);
            updateLoginUserDetails(clusterVMs.stream().map(InternalIdentity::getId).collect(Collectors.toList()));
        } catch (CloudRuntimeException | ManagementServerException | ResourceUnavailableException | InsufficientCapacityException e) {
            logTransitStateToFailedIfNeededAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes cluster : %s, unable to provision node VM in the cluster", kubernetesCluster.getName()), e);
        }
        try {
            List<Long> clusterVMIds = getKubernetesClusterVMMaps().stream().map(KubernetesClusterVmMapVO::getVmId).collect(Collectors.toList());
            scaleKubernetesClusterNetworkRules(clusterVMIds);
        } catch (ManagementServerException e) {
            logTransitStateToFailedIfNeededAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes cluster : %s, unable to update network rules", kubernetesCluster.getName()), e);
        }
        attachIsoKubernetesVMs(clusterVMs);
        KubernetesClusterVO kubernetesClusterVO = kubernetesClusterDao.findById(kubernetesCluster.getId());
        kubernetesClusterVO.setNodeCount(clusterSize);
        boolean readyNodesCountValid = KubernetesClusterUtil.validateKubernetesClusterReadyNodesCount(kubernetesClusterVO, publicIpAddress, sshPort,
                getControlNodeLoginUser(), sshKeyFile, scaleTimeoutTime, 15000);
        detachIsoKubernetesVMs(clusterVMs);
        deleteTemplateLaunchPermission();
        if (!readyNodesCountValid) { // Scaling failed
            logTransitStateToFailedIfNeededAndThrow(Level.ERROR, String.format("Scaling unsuccessful for Kubernetes cluster : %s as it does not have desired number of nodes in ready state", kubernetesCluster.getName()));
        }
    }

    private void scaleKubernetesClusterSize() throws CloudRuntimeException {
        validateKubernetesClusterScaleSizeParameters();
        final long originalClusterSize = kubernetesCluster.getNodeCount();
        final long newVmRequiredCount = clusterSize - originalClusterSize;
        if (KubernetesCluster.State.Created.equals(originalState)) {
            if (!kubernetesCluster.getState().equals(KubernetesCluster.State.Scaling)) {
                stateTransitTo(kubernetesCluster.getId(), newVmRequiredCount > 0 ? KubernetesCluster.Event.ScaleUpRequested : KubernetesCluster.Event.ScaleDownRequested);
            }
            kubernetesCluster = updateKubernetesClusterEntry(null, serviceOffering);
            return;
        }
        Pair<String, Integer> publicIpSshPort = getKubernetesClusterServerIpSshPort(null);
        publicIpAddress = publicIpSshPort.first();
        sshPort = publicIpSshPort.second();
        if (StringUtils.isEmpty(publicIpAddress)) {
            logTransitStateToFailedIfNeededAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes cluster : %s, unable to retrieve associated public IP", kubernetesCluster.getName()));
        }
        if (newVmRequiredCount < 0) { // downscale
            scaleDownKubernetesClusterSize();
        } else { // upscale, same node count handled above
            scaleUpKubernetesClusterSize(newVmRequiredCount);
        }
        kubernetesCluster = updateKubernetesClusterEntry(clusterSize, null);
    }

    private boolean isAutoscalingChanged() {
        if (this.isAutoscalingEnabled == null) {
            return false;
        }
        if (this.isAutoscalingEnabled != kubernetesCluster.getAutoscalingEnabled()) {
            return true;
        }
        if (minSize != null && (!minSize.equals(kubernetesCluster.getMinSize()))) {
            return true;
        }
        return maxSize != null && (!maxSize.equals(kubernetesCluster.getMaxSize()));
    }

    public boolean scaleCluster() throws CloudRuntimeException {
        init();
        if (logger.isInfoEnabled()) {
            logger.info(String.format("Scaling Kubernetes cluster : %s", kubernetesCluster.getName()));
        }
        scaleTimeoutTime = System.currentTimeMillis() + KubernetesClusterService.KubernetesClusterScaleTimeout.value() * 1000;
        final long originalClusterSize = kubernetesCluster.getNodeCount();
        final ServiceOffering existingServiceOffering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
        if (existingServiceOffering == null) {
            logAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster : %s failed, service offering for the Kubernetes cluster not found!", kubernetesCluster.getName()));
        }
        final boolean autscalingChanged = isAutoscalingChanged();
        final boolean serviceOfferingScalingNeeded = serviceOffering != null && serviceOffering.getId() != existingServiceOffering.getId();

        if (autscalingChanged) {
            boolean autoScaled = autoscaleCluster(this.isAutoscalingEnabled, minSize, maxSize);
            if (autoScaled && serviceOfferingScalingNeeded) {
                scaleKubernetesClusterOffering();
            }
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
            return autoScaled;
        }
        final boolean clusterSizeScalingNeeded = clusterSize != null && clusterSize != originalClusterSize;
        final long newVMRequired = clusterSize == null ? 0 : clusterSize - originalClusterSize;
        if (serviceOfferingScalingNeeded && clusterSizeScalingNeeded) {
            if (newVMRequired > 0) {
                scaleKubernetesClusterOffering();
                scaleKubernetesClusterSize();
            } else {
                scaleKubernetesClusterSize();
                scaleKubernetesClusterOffering();
            }
        } else if (serviceOfferingScalingNeeded) {
            scaleKubernetesClusterOffering();
        } else if (clusterSizeScalingNeeded) {
            scaleKubernetesClusterSize();
        }
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
        return true;
    }
}
