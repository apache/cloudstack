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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.VMTemplateVO;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.dc.DataCenter;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventVO;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterEventTypes;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterUtil;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.LaunchPermissionVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.logging.log4j.Level;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.CONTROL;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.DEFAULT;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.ETCD;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.WORKER;

public class KubernetesClusterScaleWorker extends KubernetesClusterResourceModifierActionWorker {

    @Inject
    protected VMInstanceDao vmInstanceDao;

    private Map<String, ServiceOffering> serviceOfferingNodeTypeMap;
    private Long clusterSize;
    private List<Long> nodeIds;
    private KubernetesCluster.State originalState;
    private Network network;
    private Long minSize;
    private Long maxSize;
    private Boolean isAutoscalingEnabled;
    protected long scaleTimeoutTime;
    /** How many times to try to drain a node that is being removed, as scale-down did before. */
    private static final int SCALE_DOWN_DRAIN_ATTEMPTS = 3;

    protected KubernetesClusterScaleWorker(final KubernetesCluster kubernetesCluster, final KubernetesClusterManagerImpl clusterManager) {
        super(kubernetesCluster, clusterManager);
    }

    public KubernetesClusterScaleWorker(final KubernetesCluster kubernetesCluster,
                                        final Map<String, ServiceOffering> serviceOfferingNodeTypeMap,
                                        final Long clusterSize,
                                        final List<Long> nodeIds,
                                        final Boolean isAutoscalingEnabled,
                                        final Long minSize,
                                        final Long maxSize,
                                        final KubernetesClusterManagerImpl clusterManager) {
        super(kubernetesCluster, clusterManager);
        this.serviceOfferingNodeTypeMap = serviceOfferingNodeTypeMap;
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
        FirewallRule firewallRule = removeSshFirewallRule(publicIp, network.getId());
        int existingFirewallRuleSourcePortEnd;
        if (firewallRule == null) {
            logger.warn("SSH firewall rule not found for Kubernetes cluster: {}. It may have been manually deleted or modified.", kubernetesCluster.getName());
            existingFirewallRuleSourcePortEnd = CLUSTER_NODES_DEFAULT_START_SSH_PORT + clusterVMIds.size() - 1;
        } else {
            existingFirewallRuleSourcePortEnd = firewallRule.getSourcePortEnd();
        }

        try {
            removePortForwardingRules(publicIp, network, owner, CLUSTER_NODES_DEFAULT_START_SSH_PORT, existingFirewallRuleSourcePortEnd);
        } catch (ResourceUnavailableException e) {
            throw new ManagementServerException(String.format("Failed to remove SSH port forwarding rules for removed VMs for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
        setupKubernetesClusterIsolatedNetworkRules(publicIp, network, clusterVMIds, false);
    }

    private void scaleKubernetesClusterVpcTierRules(final List<Long> clusterVMIds) throws ManagementServerException {
        if (manager.isNetrisNetwork(network)) {
            // For Netris: SSH port-forwarding is on NAT IP; the LB rule on LB IP for 6443 is fixed and never re-provisioned during scale
            IpAddress natIp = getVpcTierKubernetesPublicIp(network, ApiConstants.NETRIS_NAT_PUBLIC_IP_ID);
            if (natIp == null) {
                throw new ManagementServerException(String.format("No NAT IP found for Netris VPC tier : %s, Kubernetes cluster : %s", network.getName(), kubernetesCluster.getName()));
            }
            try {
                removePortForwardingRules(natIp, network, owner, CLUSTER_NODES_DEFAULT_START_SSH_PORT, CLUSTER_NODES_DEFAULT_START_SSH_PORT + clusterVMIds.size() - 1);
            } catch (ResourceUnavailableException e) {
                throw new ManagementServerException(String.format("Failed to remove SSH port forwarding rules for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
            }
            try {
                Map<Long, Integer> vmIdPortMap = getVmPortMap();
                provisionSshPortForwardingRules(natIp, network, owner, clusterVMIds, vmIdPortMap);
            } catch (ResourceUnavailableException | NetworkRuleConflictException e) {
                throw new ManagementServerException(String.format("Failed to activate SSH port forwarding rules for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
            }
            return;
        }
        IpAddress publicIp = getVpcTierKubernetesPublicIp(network);
        if (publicIp == null) {
            throw new ManagementServerException(String.format("No public IP addresses found for VPC tier : %s, Kubernetes cluster : %s", network.getName(), kubernetesCluster.getName()));
        }
        try {
            removePortForwardingRules(publicIp, network, owner, CLUSTER_NODES_DEFAULT_START_SSH_PORT, CLUSTER_NODES_DEFAULT_START_SSH_PORT + clusterVMIds.size() - 1);
        } catch (ResourceUnavailableException e) {
            throw new ManagementServerException(String.format("Failed to remove SSH port forwarding rules for removed VMs for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        }
        try {
           Map<Long, Integer> vmIdPortMap = getVmPortMap();
            provisionSshPortForwardingRules(publicIp, network, owner, clusterVMIds, vmIdPortMap);
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
        if (manager.isDirectAccess(network)) {
            if (logger.isDebugEnabled())
                logger.debug("Network: {} for Kubernetes cluster: {} is not an isolated network " +
                        "or ROUTED network, therefore, no need for network rules", network, kubernetesCluster);
            return;
        }
        if (network.getVpcId() != null) {
            scaleKubernetesClusterVpcTierRules(clusterVMIds);
            return;
        }
        scaleKubernetesClusterIsolatedNetworkRules(clusterVMIds);
    }

    private KubernetesClusterVO updateKubernetesClusterEntryForNodeType(final Long newWorkerSize, final KubernetesClusterNodeType nodeType,
                                                                        final ServiceOffering newServiceOffering,
                                                                        final boolean updateNodeOffering, boolean updateClusterOffering) throws CloudRuntimeException {
        final ServiceOffering serviceOffering = newServiceOffering == null ?
                serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId()) : newServiceOffering;
        final Long serviceOfferingId = newServiceOffering == null ? null : serviceOffering.getId();

        Pair<Long, Long> clusterCountAndCapacity = calculateNewClusterCountAndCapacity(newWorkerSize, nodeType, serviceOffering);
        long cores = clusterCountAndCapacity.first();
        long memory = clusterCountAndCapacity.second();

        KubernetesClusterVO kubernetesClusterVO = updateKubernetesClusterEntry(cores, memory, newWorkerSize, serviceOfferingId,
            kubernetesCluster.getAutoscalingEnabled(), kubernetesCluster.getMinSize(), kubernetesCluster.getMaxSize(), nodeType, updateNodeOffering, updateClusterOffering);
        if (kubernetesClusterVO == null) {
            logTransitStateAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster %s failed, unable to update Kubernetes cluster",
                    kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        }
        return kubernetesClusterVO;
    }

    protected Pair<Long, Long> calculateNewClusterCountAndCapacity(Long newWorkerSize, KubernetesClusterNodeType nodeType, ServiceOffering serviceOffering) {
        long cores;
        long memory;
        long totalClusterSize = newWorkerSize == null ? kubernetesCluster.getTotalNodeCount() : (newWorkerSize + kubernetesCluster.getControlNodeCount() + kubernetesCluster.getEtcdNodeCount());

        if (nodeType == DEFAULT) {
            cores = serviceOffering.getCpu() * totalClusterSize;
            memory = serviceOffering.getRamSize() * totalClusterSize;
        } else {
            long nodeCount = getNodeCountForType(nodeType, kubernetesCluster);
            Long existingOfferingId = getExistingOfferingIdForNodeType(nodeType, kubernetesCluster);
            if (existingOfferingId == null) {
                existingOfferingId = serviceOffering.getId();
            }
            ServiceOfferingVO previousOffering = serviceOfferingDao.findById(existingOfferingId);
            Pair<Long, Long> previousNodesCapacity = calculateNodesCapacity(previousOffering, nodeCount);
            if (WORKER == nodeType) {
                nodeCount = newWorkerSize == null ? kubernetesCluster.getNodeCount() : newWorkerSize;
            }
            Pair<Long, Long> newNodesCapacity = calculateNodesCapacity(serviceOffering, nodeCount);
            Pair<Long, Long> newClusterCapacity = calculateClusterNewCapacity(kubernetesCluster, previousNodesCapacity, newNodesCapacity);
            cores = newClusterCapacity.first();
            memory = newClusterCapacity.second();
        }
        return new Pair<>(cores, memory);
    }

    private long getNodeCountForType(KubernetesClusterNodeType nodeType, KubernetesCluster kubernetesCluster) {
        if (WORKER == nodeType) {
            return kubernetesCluster.getNodeCount();
        } else if (CONTROL == nodeType) {
            return kubernetesCluster.getControlNodeCount();
        } else if (ETCD == nodeType) {
            return kubernetesCluster.getEtcdNodeCount();
        }
        return kubernetesCluster.getTotalNodeCount();
    }

    protected Pair<Long, Long> calculateClusterNewCapacity(KubernetesCluster kubernetesCluster,
                                                           Pair<Long, Long> previousNodeTypeCapacity,
                                                           Pair<Long, Long> newNodeTypeCapacity) {
        long previousCores = kubernetesCluster.getCores();
        long previousMemory = kubernetesCluster.getMemory();
        long newCores = previousCores - previousNodeTypeCapacity.first() + newNodeTypeCapacity.first();
        long newMemory = previousMemory - previousNodeTypeCapacity.second() + newNodeTypeCapacity.second();
        return new Pair<>(newCores, newMemory);
    }

    protected Pair<Long, Long> calculateNodesCapacity(ServiceOffering offering, long nodeCount) {
        return new Pair<>(offering.getCpu() * nodeCount, offering.getRamSize() * nodeCount);
    }

    /**
     * Deletes the Kubernetes node object of a VM that is being removed from the cluster. The node has been drained
     * already, see {@link #drainNodesToRemove(List)}. A failed attempt is retried after waiting.
     */
    private boolean removeKubernetesClusterNode(final String ipAddress, final int port, final UserVm userVm, final int retries, final int waitDuration) {
        final String hostName = getNodeName(userVm);
        for (int attempt = 1; attempt <= retries; attempt++) {
            if (attempt > 1 && !sleepBeforeRetry(waitDuration)) {
                break;
            }
            try {
                Pair<Boolean, String> result = executeSshCommand(ipAddress, port,
                        String.format("sudo /opt/bin/kubectl delete node %s --ignore-not-found", hostName), 10000, 10000, 30000);
                if (result.first()) {
                    return true;
                }
                logger.warn("Deleting node: {} on VM: {} in Kubernetes cluster: {} unsuccessful: {}", hostName, userVm, kubernetesCluster, result.second());
            } catch (Exception e) {
                logger.warn("Failed to remove Kubernetes cluster: {} node: {} on VM: {}", kubernetesCluster, hostName, userVm, e);
            }
        }
        return false;
    }

    /**
     * Drains the nodes that are being removed, before any of them is removed. All of them are cordoned first, so
     * that the pods evicted from one are not moved to another one that is going away. If a node can't be drained,
     * all of them are uncordoned and the scale-down stops with nothing removed: the nodes are still working members
     * of the cluster, so the cluster goes back to the state it was in, rather than to Alert.
     *
     * @return the nodes that were cordoned here, as opposed to the ones that were cordoned already
     */
    protected List<UserVmVO> drainNodesToRemove(final List<UserVmVO> vms) throws CloudRuntimeException {
        List<UserVmVO> cordoned = new ArrayList<>();
        String failure = null;
        for (UserVmVO vm : vms) {
            // A node cordoned by someone else has to stay cordoned if the scale-down is rolled back
            final boolean cordonedAlready = isNodeCordoned(vm);
            String output;
            try {
                Pair<Boolean, String> result = executeSshCommand(publicIpAddress, sshPort,
                        String.format("sudo /opt/bin/kubectl cordon %s", getNodeName(vm)), 10000, 10000, 30000);
                if (result.first()) {
                    if (!cordonedAlready) {
                        cordoned.add(vm);
                    }
                    continue;
                }
                output = result.second();
            } catch (Exception e) {
                output = e.getMessage();
            }
            failure = String.format("unable to cordon Kubernetes node: %s on VM: %s: %s", getNodeName(vm), vm, output);
            break;
        }
        if (failure == null) {
            for (UserVmVO vm : vms) {
                if (System.currentTimeMillis() > scaleTimeoutTime) {
                    failure = String.format("scaling action timed out before Kubernetes node: %s on VM: %s was drained", getNodeName(vm), vm);
                    break;
                }
                Pair<Boolean, String> drained = drainKubernetesClusterNode(publicIpAddress, sshPort, vm, SCALE_DOWN_DRAIN_ATTEMPTS, DRAIN_RETRY_WAIT, scaleTimeoutTime);
                if (!drained.first()) {
                    failure = String.format("unable to drain Kubernetes node: %s on VM: %s: %s", getNodeName(vm), vm, drained.second());
                    break;
                }
            }
        }
        if (failure == null) {
            return cordoned;
        }
        uncordonNodes(cordoned);
        String message = String.format("Scaling failed for Kubernetes cluster %s, %s. No node was removed", kubernetesCluster, failure);
        logger.error(message);
        // Nothing has changed, so the cluster goes back to the state it was in: Scaling goes back to Running (and
        // ScalingStoppedCluster to Stopped) on OperationSucceeded. Scaling a cluster in Alert is not allowed today,
        // but if it is, such a cluster has to go back to Alert, not to Running.
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.State.Alert.equals(originalState) ?
                KubernetesCluster.Event.OperationFailed : KubernetesCluster.Event.OperationSucceeded);
        throw new CloudRuntimeException(message);
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
            VMTemplateVO clusterTemplate = templateDao.findById(kubernetesCluster.getTemplateId());
            try {
                if (originalState.equals(KubernetesCluster.State.Running)) {
                    plan(newVmRequiredCount, zone, clusterServiceOffering, kubernetesCluster.getDomainId(), kubernetesCluster.getAccountId(), clusterTemplate.getHypervisorType(), clusterTemplate.getArch());
                } else {
                    plan(kubernetesCluster.getTotalNodeCount() + newVmRequiredCount, zone, clusterServiceOffering, kubernetesCluster.getDomainId(), kubernetesCluster.getAccountId(), clusterTemplate.getHypervisorType(), clusterTemplate.getArch());
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

    private void scaleKubernetesClusterOffering(KubernetesClusterNodeType nodeType, ServiceOffering serviceOffering,
                                                boolean updateNodeOffering, boolean updateClusterOffering) throws CloudRuntimeException {
        validateKubernetesClusterScaleOfferingParameters();
        List<KubernetesCluster.State> scalingStates = List.of(KubernetesCluster.State.Scaling, KubernetesCluster.State.ScalingStoppedCluster);
        if (!scalingStates.contains(kubernetesCluster.getState())) {
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.ScaleUpRequested);
        }
        if (KubernetesCluster.State.Created.equals(originalState)) {
            kubernetesCluster = updateKubernetesClusterEntryForNodeType(null, nodeType, serviceOffering, updateNodeOffering, updateClusterOffering);
            return;
        }
        final long size = getNodeCountForType(nodeType, kubernetesCluster);
        List<KubernetesClusterVmMapVO> vmList = kubernetesClusterVmMapDao.listByClusterIdAndVmType(kubernetesCluster.getId(), nodeType);
        final long tobeScaledVMCount =  Math.min(vmList.size(), size);
        for (long i = 0; i < tobeScaledVMCount; i++) {
            KubernetesClusterVmMapVO vmMapVO = vmList.get((int) i);
            UserVmVO userVM = userVmDao.findById(vmMapVO.getVmId());
            boolean result = false;
            try {
                result = userVmManager.upgradeVirtualMachine(userVM.getId(), serviceOffering.getId(), new HashMap<String, String>());
            } catch (RuntimeException | ResourceUnavailableException | ManagementServerException | VirtualMachineMigrationException e) {
                logTransitStateAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster : %s failed, unable to scale cluster VM : %s due to %s", kubernetesCluster.getName(), userVM.getDisplayName(), e.getMessage()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed, e);
            }
            if (!result) {
                logTransitStateAndThrow(Level.WARN, String.format("Scaling Kubernetes cluster : %s failed, unable to scale cluster VM : %s", kubernetesCluster.getName(), userVM.getDisplayName()),kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            }
            if (System.currentTimeMillis() > scaleTimeoutTime) {
                logTransitStateAndThrow(Level.WARN, String.format("Scaling Kubernetes cluster : %s failed, scaling action timed out", kubernetesCluster.getName()),kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            }
        }
        kubernetesCluster = updateKubernetesClusterEntryForNodeType(null, nodeType, serviceOffering, updateNodeOffering, updateClusterOffering);
    }

    protected void removeNodesFromCluster(List<KubernetesClusterVmMapVO> vmMaps) throws CloudRuntimeException {
        List<UserVmVO> vms = vmMaps.stream().map(vmMapVO -> userVmDao.findById(vmMapVO.getVmId())).collect(Collectors.toList());
        List<UserVmVO> cordoned = drainNodesToRemove(vms);
        // Index of the first VM whose Kubernetes node is still in the cluster
        int firstInCluster = 0;
        // VMs taken out of the cluster's VM map so far
        List<UserVmVO> removed = new ArrayList<>();
        boolean completed = false;
        try {
            for (int i = 0; i < vmMaps.size(); i++) {
                // No timeout check here: the nodes are drained, and stopping part way would leave the cluster
                // with some of them removed and the node count not updated
                UserVmVO userVM = vms.get(i);
                KubernetesClusterVmMapVO vmMapVO = vmMaps.get(i);
                logger.info("Removing vm {} from cluster {}", userVM, kubernetesCluster);
                if (!removeKubernetesClusterNode(publicIpAddress, sshPort, userVM, 3, 30000)) {
                    logTransitStateAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes" +
                            " cluster %s, failed to remove Kubernetes node: %s running on VM : %s",
                            kubernetesCluster, userVM.getHostName(), userVM), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
                }
                firstInCluster = i + 1;
                try {
                    destroyRemovedNodeVm(userVM);
                } finally {
                    // The node has left the Kubernetes cluster, so its VM leaves the cluster's VM map, whatever
                    // happened to the VM. A map entry kept for it would never match a Kubernetes node again: the
                    // state scanner would hold the cluster in Alert, which blocks the next scale, and the node count
                    // and the next scale-down's choice of nodes would be worked out from a VM that is gone. The
                    // entry is deleted only now as the VM's expunge looks it up (to leave alone the port-forwarding
                    // rules of a CKS node in an NSX zone).
                    kubernetesClusterVmMapDao.expunge(vmMapVO.getId());
                    removed.add(userVM);
                }
            }

            // Scale network rules to update firewall rule
            try {
                List<Long> clusterVMIds = getKubernetesClusterVMMaps()
                        .stream()
                        .filter(x -> !x.isEtcdNode())
                        .map(KubernetesClusterVmMapVO::getVmId).collect(Collectors.toList());
                scaleKubernetesClusterNetworkRules(clusterVMIds);
            } catch (ManagementServerException e) {
                logTransitStateAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes " +
                        "cluster %s, unable to update network rules", kubernetesCluster),
                        kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed, e);
            }
            completed = true;
        } finally {
            if (!completed) {
                // The nodes that are still in the cluster were drained above and would stay unschedulable
                updateKubernetesClusterEntryForRemovedNodes(removed);
                List<UserVmVO> inCluster = vms.subList(firstInCluster, vms.size());
                uncordonNodes(cordoned.stream().filter(inCluster::contains).collect(Collectors.toList()));
            }
        }
    }

    /**
     * Destroys and expunges the VM of a node that has been deleted from the Kubernetes cluster. The caller then takes
     * the VM out of the cluster's VM map, whether this succeeds or not.
     * <ul>
     * <li>If the VM can't be destroyed, the scale fails: the VM may still be running, though it is no longer a
     * Kubernetes node nor a member of the cluster, and has to be destroyed by hand.</li>
     * <li>If the destroyed VM can't be expunged, the scale goes on, with a warning event that names the VM and its
     * state. The expunge task retries VMs left Destroyed, Expunging or in Error.</li>
     * </ul>
     */
    protected void destroyRemovedNodeVm(final UserVmVO userVM) throws CloudRuntimeException {
        CallContext vmContext = CallContext.register(CallContext.current(),
                ApiCommandResourceType.VirtualMachine);
        vmContext.setEventResourceId(userVM.getId());
        // Whether the VM is left behind, destroyed or not
        boolean leftBehind = false;
        try {
            try {
                userVmService.destroyVm(userVM.getId(), true);
            } catch (ResourceUnavailableException | CloudRuntimeException e) {
                // destroyVm can fail after the VM has been destroyed, on the resource count updates that follow
                VMInstanceVO vm = null;
                boolean found = false;
                try {
                    vm = vmInstanceDao.findByIdIncludingRemoved(userVM.getId());
                    found = true;
                } catch (RuntimeException lookupError) {
                    logger.warn("Unable to look up VM {} after it failed to be destroyed", userVM, lookupError);
                }
                if (found && (vm == null || vm.getRemoved() != null)) {
                    logger.warn("Destroying VM {} of the node removed from Kubernetes cluster {} reported an error, but the VM is expunged",
                            userVM, kubernetesCluster, e);
                    return;
                }
                if (found && List.of(VirtualMachine.State.Destroyed, VirtualMachine.State.Expunging).contains(vm.getState())) {
                    logger.warn("Destroying VM {} of the node removed from Kubernetes cluster {} reported an error, but the VM is {}",
                            userVM, kubernetesCluster, vm.getState(), e);
                } else {
                    leftBehind = true;
                    logTransitStateAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster %s failed, unable to destroy VM %s%s." +
                        " Its node was removed from the Kubernetes cluster and the VM is no longer part of the cluster;" +
                        " it may still be running and has to be destroyed manually", kubernetesCluster, userVM,
                        vm == null ? "" : String.format(" (left in state %s)", vm.getState())),
                        kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed, e);
                }
            }
            boolean expunged = false;
            Exception expungeError = null;
            try {
                expunged = userVmManager.expunge(userVM);
            } catch (RuntimeException e) {
                expungeError = e;
            }
            if (!expunged) {
                leftBehind = warnVmNotExpunged(userVM, expungeError);
            }
        } finally {
            CallContext.unregister();
            if (leftBehind) {
                revokeRemovedNodeVmSshRules(userVM);
            }
        }
    }

    /** @return false if the VM turns out to be expunged, true if it is left behind. Doesn't throw. */
    private boolean warnVmNotExpunged(final UserVmVO userVM, final Exception expungeError) {
        VMInstanceVO vm;
        try {
            vm = vmInstanceDao.findByIdIncludingRemoved(userVM.getId());
        } catch (RuntimeException e) {
            logger.warn("Unable to expunge VM {} of the node removed from Kubernetes cluster {}; it is no longer part of the cluster",
                    userVM, kubernetesCluster, expungeError);
            return true;
        }
        if (vm == null || vm.getRemoved() != null) {
            // Expunged meanwhile, by the expunge task for one
            logger.info("VM {} of the node removed from Kubernetes cluster {} has been expunged", userVM, kubernetesCluster);
            return false;
        }
        String message = String.format("Unable to expunge VM %s of the node removed from Kubernetes cluster %s. The VM is" +
                " no longer part of the cluster and is left in state %s, for the expunge task to retry. Until it is expunged" +
                " it keeps the cluster's network in use, and the cluster can't be deleted with its network",
                userVM, kubernetesCluster, vm.getState());
        logger.warn(message, expungeError);
        try {
            // Recorded for the VM's owner, who has to see why the VM still holds the network
            ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), userVM.getAccountId(),
                    EventVO.LEVEL_WARN, KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_SCALE,
                    message, userVM.getId(), ApiCommandResourceType.VirtualMachine.toString(), 0);
        } catch (RuntimeException e) {
            logger.warn("Unable to record an event for VM {} that was not expunged", userVM, e);
        }
        return true;
    }

    /**
     * Revokes the SSH port-forwarding rule that the cluster has on a removed node's VM that is left behind. Expunging
     * the VM would revoke it; the cluster's network rule updates may not: a VPC tier's only clears the ports of the
     * nodes that remain, where the rule would conflict with the rule of a node added later, and none runs if the
     * scale-down stops. Left alone are: a rule provided by NSX (the VM's expunge leaves the port-forwarding rules of a
     * CKS node alone in an NSX zone); the rule on the port this scale reaches the cluster through, if it is the removed
     * VM's, as the nodes that remain are still to be reached through it; and the VM's other rules.
     */
    protected void revokeRemovedNodeVmSshRules(final UserVmVO userVM) {
        boolean registered = false;
        try {
            if (network == null || publicIpAddress == null || manager.isDirectAccess(network) ||
                    networkModel.isProviderSupportServiceInNetwork(network.getId(), Network.Service.PortForwarding, Network.Provider.Nsx)) {
                return;
            }
            // A context of its own rather than the VM's or the scale's
            CallContext.register(CallContext.current(), null);
            registered = true;
            for (PortForwardingRuleVO pfRule : portForwardingRulesDao.listByVm(userVM.getId())) {
                if (!FirewallRule.State.Revoke.equals(pfRule.getState()) && pfRule.getNetworkId() == network.getId() &&
                        pfRule.getDestinationPortStart() == DEFAULT_SSH_PORT && pfRule.getSourcePortStart() >= CLUSTER_NODES_DEFAULT_START_SSH_PORT &&
                        pfRule.getSourcePortStart() != sshPort && isRuleOnAddress(pfRule, publicIpAddress)) {
                    try {
                        rulesService.revokePortForwardingRule(pfRule.getId(), true);
                    } catch (Exception e) {
                        logger.warn("Unable to revoke the SSH port-forwarding rule {} of VM {} of the node removed from Kubernetes cluster {}",
                                pfRule, userVM, kubernetesCluster, e);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to revoke the SSH port-forwarding rule of VM {} of the node removed from Kubernetes cluster {}",
                    userVM, kubernetesCluster, e);
        } finally {
            if (registered) {
                CallContext.unregister();
            }
        }
    }

    /**
     * Takes the VMs that have left the cluster's VM map off the cluster's node count, cores and memory, when a
     * scale-down stops part way, so that the cluster matches its VM map. The scale-down updates them itself once every
     * node has been removed, and this takes the same view of the node count: every VM removed, including a control
     * node removed by ID, comes off the node count, so that the cluster's total node count matches its VM map. Any
     * failure here is logged, so that the scale-down's own error is the one reported.
     */
    private void updateKubernetesClusterEntryForRemovedNodes(final List<UserVmVO> removed) {
        if (removed.isEmpty()) {
            return;
        }
        try {
            long removedCores = 0;
            long removedMemory = 0;
            for (UserVmVO vm : removed) {
                ServiceOfferingVO offering = serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId());
                removedCores += offering.getCpu();
                removedMemory += offering.getRamSize();
            }
            KubernetesClusterVO cluster = kubernetesClusterDao.findById(kubernetesCluster.getId());
            updateKubernetesClusterEntry(cluster.getCores() - removedCores, cluster.getMemory() - removedMemory,
                    cluster.getNodeCount() - removed.size(), null, cluster.getAutoscalingEnabled(), cluster.getMinSize(),
                    cluster.getMaxSize(), WORKER, false, false);
        } catch (RuntimeException e) {
            logger.error("Unable to take {} removed nodes off the node count, cores and memory of Kubernetes cluster {};" +
                    " they no longer match its VMs", removed.size(), kubernetesCluster, e);
        }
    }

    private void scaleDownKubernetesClusterSize() throws CloudRuntimeException {
        if (!kubernetesCluster.getState().equals(KubernetesCluster.State.Scaling)) {
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.ScaleDownRequested);
        }
        List<KubernetesClusterVmMapVO> vmList;
        if (this.nodeIds != null) {
            vmList = getKubernetesClusterVMMapsForNodes(this.nodeIds).stream().filter(vm -> !vm.isExternalNode()).collect(Collectors.toList());
        } else {
            vmList = getWorkerNodesToRemove();
            if (vmList.isEmpty()) {
                logger.info("No nodes to remove from Kubernetes cluster: {}", kubernetesCluster);
                return;
            }
        }
        Collections.reverse(vmList);
        removeNodesFromCluster(vmList);
    }

    public List<KubernetesClusterVmMapVO> getWorkerNodesToRemove() {
        List<KubernetesClusterVmMapVO> workerVMsMap = getKubernetesClusterVMMaps().stream()
                .filter(vm -> !vm.isExternalNode() && !vm.isControlNode() && !vm.isEtcdNode())
                .collect(Collectors.toList());
        int totalWorkerNodes = workerVMsMap.size();
        int desiredWorkerNodes = clusterSize == null ? (int) kubernetesCluster.getNodeCount() : clusterSize.intValue();
        int toRemoveCount = Math.max(0, totalWorkerNodes - desiredWorkerNodes);
        if (toRemoveCount == 0) {
            return new ArrayList<>();
        }
        int startIndex = Math.max(0, totalWorkerNodes - toRemoveCount);
        return new ArrayList<>(workerVMsMap.subList(startIndex, totalWorkerNodes));
    }

    private void scaleUpKubernetesClusterSize(final long newVmCount) throws CloudRuntimeException {
        if (!kubernetesCluster.getState().equals(KubernetesCluster.State.Scaling)) {
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.ScaleUpRequested);
        }
        List<UserVm> clusterVMs = new ArrayList<>();
        if (isDefaultTemplateUsed()) {
            LaunchPermissionVO launchPermission = new LaunchPermissionVO(clusterTemplate.getId(), owner.getId());
            launchPermissionDao.persist(launchPermission);
        }
        try {
            clusterVMs = provisionKubernetesClusterNodeVms((int)(newVmCount + kubernetesCluster.getNodeCount()), (int)kubernetesCluster.getNodeCount(), apiPublicIpAddress, kubernetesCluster.getDomainId(), kubernetesCluster.getAccountId());
            updateLoginUserDetails(clusterVMs.stream().map(InternalIdentity::getId).collect(Collectors.toList()));
        } catch (CloudRuntimeException | ManagementServerException | ResourceUnavailableException | InsufficientCapacityException e) {
            logTransitStateToFailedIfNeededAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes cluster : %s, unable to provision node VM in the cluster", kubernetesCluster.getName()), e);
        }
        try {
            List<Long> externalNodeIds = getKubernetesClusterVMMaps().stream().filter(KubernetesClusterVmMapVO::isExternalNode).map(KubernetesClusterVmMapVO::getVmId).collect(Collectors.toList());
            List<Long> clusterVMIds = getKubernetesClusterVMMaps().stream().filter(vm -> !vm.isExternalNode() && !vm.isEtcdNode()).map(KubernetesClusterVmMapVO::getVmId).collect(Collectors.toList());
            clusterVMIds.addAll(externalNodeIds);
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

    private void scaleKubernetesClusterSize(KubernetesClusterNodeType nodeType) throws CloudRuntimeException {
        validateKubernetesClusterScaleSizeParameters();
        final long originalClusterSize = kubernetesCluster.getNodeCount();
        final long newVmRequiredCount = clusterSize - originalClusterSize;
        if (KubernetesCluster.State.Created.equals(originalState)) {
            if (!kubernetesCluster.getState().equals(KubernetesCluster.State.Scaling)) {
                stateTransitTo(kubernetesCluster.getId(), newVmRequiredCount > 0 ? KubernetesCluster.Event.ScaleUpRequested : KubernetesCluster.Event.ScaleDownRequested);
            }
            kubernetesCluster = updateKubernetesClusterEntryForNodeType(null, nodeType, serviceOfferingNodeTypeMap.get(nodeType.name()), false, false);
            return;
        }
        Pair<String, Integer> publicIpSshPort = getKubernetesClusterServerIpSshPort(null);
        publicIpAddress = publicIpSshPort.first();
        sshPort = publicIpSshPort.second();
        // For Netris VPC-tier: apiPublicIpAddress = LB IP (kubeadm join target for new nodes).
        // For all others: same as publicIpAddress.
        apiPublicIpAddress = resolveApiPublicIpAddress(network);
        if (StringUtils.isEmpty(publicIpAddress)) {
            logTransitStateToFailedIfNeededAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes cluster : %s, unable to retrieve associated public IP", kubernetesCluster.getName()));
        }
        if (newVmRequiredCount < 0) { // downscale
            scaleDownKubernetesClusterSize();
        } else { // upscale, same node count handled above
            scaleUpKubernetesClusterSize(newVmRequiredCount);
        }
        boolean updateNodeOffering = serviceOfferingNodeTypeMap.containsKey(nodeType.name());
        ServiceOffering nodeOffering = serviceOfferingNodeTypeMap.getOrDefault(nodeType.name(), null);
        kubernetesCluster = updateKubernetesClusterEntryForNodeType(clusterSize, nodeType, nodeOffering, updateNodeOffering, false);
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
            logger.info("Scaling Kubernetes cluster {}", kubernetesCluster);
        }
        scaleTimeoutTime = System.currentTimeMillis() + KubernetesClusterService.KubernetesClusterScaleTimeout.value() * 1000;
        final long originalClusterSize = kubernetesCluster.getNodeCount();

        // DEFAULT node type means only the global service offering has been set for the Kubernetes cluster
        boolean scaleClusterDefaultOffering = serviceOfferingNodeTypeMap.containsKey(DEFAULT.name());
        if (scaleClusterDefaultOffering) {
            final ServiceOffering existingServiceOffering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
            final ServiceOffering existingControlOffering = serviceOfferingDao.findById(kubernetesCluster.getControlNodeServiceOfferingId());
            final ServiceOffering existingWorkerOffering = serviceOfferingDao.findById(kubernetesCluster.getWorkerNodeServiceOfferingId());
            if (existingServiceOffering == null && ObjectUtils.anyNull(existingControlOffering, existingWorkerOffering)) {
                logAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster : %s failed, service offering for the Kubernetes cluster not found!", kubernetesCluster.getName()));
            }
        }

        final boolean autoscalingChanged = isAutoscalingChanged();
        ServiceOffering defaultServiceOffering = serviceOfferingNodeTypeMap.getOrDefault(DEFAULT.name(), null);

        for (KubernetesClusterNodeType nodeType : Arrays.asList(CONTROL, ETCD, WORKER)) {
            boolean isWorkerNode = WORKER == nodeType;
            final long newVMRequired = (!isWorkerNode || clusterSize == null) ? 0 : clusterSize - originalClusterSize;
            if (!scaleClusterDefaultOffering && !serviceOfferingNodeTypeMap.containsKey(nodeType.name()) && newVMRequired == 0) {
                continue;
            }

            ServiceOffering existingServiceOffering = getExistingServiceOfferingForNodeType(nodeType, kubernetesCluster);
            ServiceOffering scalingServiceOffering = serviceOfferingNodeTypeMap.getOrDefault(nodeType.name(), defaultServiceOffering);
            boolean isNodeOfferingScalingNeeded = isServiceOfferingScalingNeededForNodeType(existingServiceOffering, scalingServiceOffering);
            boolean updateNodeOffering = serviceOfferingNodeTypeMap.containsKey(nodeType.name()) || isNodeOfferingScalingNeeded;

            boolean updateClusterOffering = isWorkerNode && scaleClusterDefaultOffering;
            if (isWorkerNode && autoscalingChanged) {
                boolean autoScaled = autoscaleCluster(this.isAutoscalingEnabled, minSize, maxSize);
                if (autoScaled && isNodeOfferingScalingNeeded) {
                    scaleKubernetesClusterOffering(nodeType, scalingServiceOffering, updateNodeOffering, updateClusterOffering);
                }
                stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
                return autoScaled;
            }
            final boolean clusterSizeScalingNeeded = isWorkerNode && clusterSize != null && clusterSize != originalClusterSize;
            if (isNodeOfferingScalingNeeded && clusterSizeScalingNeeded) {
                if (newVMRequired > 0) {
                    scaleKubernetesClusterOffering(nodeType, scalingServiceOffering, updateNodeOffering, updateClusterOffering);
                    scaleKubernetesClusterSize(nodeType);
                } else {
                    scaleKubernetesClusterSize(nodeType);
                    scaleKubernetesClusterOffering(nodeType, scalingServiceOffering, updateNodeOffering, updateClusterOffering);
                }
            } else if (isNodeOfferingScalingNeeded) {
                scaleKubernetesClusterOffering(nodeType, scalingServiceOffering, updateNodeOffering, updateClusterOffering);
            } else if (clusterSizeScalingNeeded) {
                scaleKubernetesClusterSize(nodeType);
            }
        }

        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
        return true;
    }

    private ServiceOffering getExistingServiceOfferingForNodeType(KubernetesClusterNodeType nodeType, KubernetesCluster kubernetesCluster) {
        Long existingOfferingId = getExistingOfferingIdForNodeType(nodeType, kubernetesCluster);
        if (existingOfferingId == null) {
            logAndThrow(Level.ERROR, String.format("The Kubernetes cluster %s does not have a service offering set for node type %s",
                    kubernetesCluster.getName(), nodeType.name()));
        }
        ServiceOffering existingOffering = serviceOfferingDao.findById(existingOfferingId);
        if (existingOffering == null) {
            logAndThrow(Level.ERROR, String.format("Cannot find service offering with ID %s set on the Kubernetes cluster %s node type %s",
                    existingOfferingId, kubernetesCluster.getName(), nodeType.name()));
        }
        return existingOffering;
    }

    protected void compareExistingToScalingServiceOfferingForNodeType(Long existingOfferingId, Long scalingOfferingId,
                                                                      KubernetesClusterNodeType nodeType) {
        if (existingOfferingId.equals(scalingOfferingId)) {
            String err = String.format("Cannot scale the nodes of type %s as the provided offering %s " +
                    "is the same as the existing offering", nodeType.name(), scalingOfferingId);
            logger.error(err);
            throw new CloudRuntimeException(err);
        }
    }

    protected boolean isServiceOfferingScalingNeededForNodeType(ServiceOffering existingServiceOffering,
                                                                ServiceOffering scalingServiceOffering) {
        return scalingServiceOffering != null && existingServiceOffering != null &&
                scalingServiceOffering.getId() != existingServiceOffering.getId();
    }

    protected Long getExistingOfferingIdForNodeType(KubernetesClusterNodeType nodeType, KubernetesCluster kubernetesCluster) {
        List<KubernetesClusterVmMapVO> clusterVms = kubernetesClusterVmMapDao.listByClusterIdAndVmType(kubernetesCluster.getId(), nodeType);
        if (CollectionUtils.isEmpty(clusterVms)) {
            return kubernetesCluster.getServiceOfferingId();
        }
        KubernetesClusterVmMapVO clusterVm = clusterVms.get(0);
        UserVmVO clusterUserVm = userVmDao.findById(clusterVm.getVmId());
        if (clusterUserVm == null) {
            return kubernetesCluster.getServiceOfferingId();
        }
        return clusterUserVm.getServiceOfferingId();
    }
}
