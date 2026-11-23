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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.VMTemplateVO;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.dc.DataCenter;
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
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.LaunchPermissionVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.ssh.SshHelper;
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
    private long scaleTimeoutTime;

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

    protected IpAddress getNodeSshIp() throws ManagementServerException {
        return getNodeSshIp(network);
    }

    /**
     * Gives SSH access to each of the given nodes that has no SSH port forwarding rule yet, such as the nodes just
     * created by the scale. The rules of the other nodes are left as they are. A node created with the cluster or by a
     * scale gets the port of its place among those nodes when that port is free, else the lowest free port from
     * {@link #CLUSTER_NODES_DEFAULT_START_SSH_PORT} up. Any other node gets the lowest free port after those places. An
     * existing node whose SSH port no firewall rule covers at all gets a firewall rule for it again.
     */
    protected void provisionMissingNodeSshRules(final List<Long> nodeVmIds) throws ManagementServerException {
        IpAddress sshIp = getNodeSshIp();
        if (sshIp == null) {
            logger.debug("Network: {} for Kubernetes cluster: {} is not an isolated network or ROUTED network, therefore, no need for network rules", network, kubernetesCluster);
            return;
        }
        boolean firewall = network.getVpcId() == null;
        List<PortForwardingRuleVO> sshRules = portForwardingRulesDao.listByIpAndNotRevoked(sshIp.getId()).stream()
                .filter(rule -> isNodeSshPortForwardingRule(rule, sshIp) && nodeVmIds.contains(rule.getVirtualMachineId()))
                .collect(Collectors.toList());
        Set<Long> nodesWithSshRule = sshRules.stream().map(PortForwardingRuleVO::getVirtualMachineId).collect(Collectors.toSet());
        List<Long> positionalNodeIds = getKubernetesClusterVMMaps().stream()
                .filter(map -> !map.isExternalNode() && !map.isEtcdNode())
                .map(KubernetesClusterVmMapVO::getVmId).collect(Collectors.toList());
        int firstNonPositionalPort = CLUSTER_NODES_DEFAULT_START_SSH_PORT + positionalNodeIds.size();
        List<FirewallRuleVO> rules = firewallRulesDao.listByIp(sshIp.getId()).stream()
                .filter(rule -> rule.getSourcePortStart() != null && rule.getSourcePortEnd() != null
                        && rule.getSourcePortEnd() >= CLUSTER_NODES_DEFAULT_START_SSH_PORT)
                .collect(Collectors.toList());
        List<FirewallRuleVO> usedPortRules = rules.stream()
                .filter(rule -> rule.getPurpose() != FirewallRule.Purpose.Firewall).collect(Collectors.toList());
        List<FirewallRuleVO> openPortRules = !firewall ? Collections.emptyList() : rules.stream()
                .filter(rule -> rule.getPurpose() == FirewallRule.Purpose.Firewall && isOpenTcpFirewallRule(rule))
                .collect(Collectors.toList());
        if (firewall) {
            List<FirewallRuleVO> firewallRules = rules.stream()
                    .filter(rule -> rule.getPurpose() == FirewallRule.Purpose.Firewall && rule.getState() != FirewallRule.State.Revoke
                            && NetUtils.TCP_PROTO.equalsIgnoreCase(rule.getProtocol()))
                    .collect(Collectors.toList());
            for (PortForwardingRuleVO sshRule : sshRules) {
                int port = sshRule.getSourcePortStart();
                if (!isPortInRules(firewallRules, port)) {
                    logger.warn("SSH port {} of node VM {} of Kubernetes cluster {} is not open on {}, adding a firewall rule for it",
                            port, sshRule.getVirtualMachineId(), kubernetesCluster.getName(), sshIp.getAddress().addr());
                    try {
                        provisionNodeSshFirewallRule(sshIp, port, port);
                    } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException | NetworkRuleConflictException | RuntimeException e) {
                        throw new ManagementServerException(String.format("Failed to provision SSH firewall rule for port %d of %s of Kubernetes cluster %s",
                                port, sshIp.getAddress().addr(), kubernetesCluster.getName()), e);
                    }
                }
            }
        }
        Set<Integer> takenPorts = new HashSet<>();
        for (Long vmId : nodeVmIds) {
            if (nodesWithSshRule.contains(vmId)) {
                continue;
            }
            int position = positionalNodeIds.indexOf(vmId);
            int preferredPort = position < 0 ? firstNonPositionalPort : CLUSTER_NODES_DEFAULT_START_SSH_PORT + position;
            int searchFrom = position < 0 ? firstNonPositionalPort : CLUSTER_NODES_DEFAULT_START_SSH_PORT;
            Integer port = getFreeNodeSshPort(usedPortRules, takenPorts, preferredPort, searchFrom);
            if (port == null) {
                throw new ManagementServerException(String.format("No free port left on %s for SSH to node VM %d of Kubernetes cluster %s",
                        sshIp.getAddress().addr(), vmId, kubernetesCluster.getName()));
            }
            takenPorts.add(port);
            boolean firewallRuleAdded = false;
            try {
                if (firewall && !isPortInRules(openPortRules, port)) {
                    firewallRuleAdded = true;
                    provisionNodeSshFirewallRule(sshIp, port, port);
                }
                provisionPublicIpPortForwardingRule(sshIp, network, owner, vmId, port, DEFAULT_SSH_PORT);
            } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException | NetworkRuleConflictException | RuntimeException e) {
                removeNodeSshRules(sshIp, listNodeSshPortForwardingRules(sshIp, vmId), false);
                if (firewallRuleAdded) {
                    removeNodeSshFirewallRules(sshIp, port, port);
                }
                throw new ManagementServerException(String.format("Failed to provision SSH access on port %d of %s to node VM %d of Kubernetes cluster %s",
                        port, sshIp.getAddress().addr(), vmId, kubernetesCluster.getName()), e);
            }
        }
    }

    /**
     * Returns preferredPort, or else the lowest port from searchFrom up, that is not in takenPorts and that no port
     * forwarding, load balancing or other non-firewall rule in usedPortRules covers, or null if there is none.
     */
    protected Integer getFreeNodeSshPort(List<FirewallRuleVO> usedPortRules, Set<Integer> takenPorts, int preferredPort, int searchFrom) {
        if (!takenPorts.contains(preferredPort) && !isPortInRules(usedPortRules, preferredPort)) {
            return preferredPort;
        }
        for (int port = searchFrom; port <= NetUtils.PORT_RANGE_MAX; port++) {
            if (!takenPorts.contains(port) && !isPortInRules(usedPortRules, port)) {
                return port;
            }
        }
        return null;
    }

    private boolean isPortInRules(List<FirewallRuleVO> rules, int port) {
        return rules.stream().anyMatch(rule -> rule.getSourcePortStart() <= port && port <= rule.getSourcePortEnd());
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
                        pkFile, null, String.format("sudo /opt/bin/kubectl drain %s --ignore-daemonsets --delete-emptydir-data", hostName),
                        10000, 10000, 60000);
                if (!result.first()) {
                    logger.warn("Draining node: {} on VM: {} in Kubernetes cluster: {} unsuccessful", hostName, userVm, kubernetesCluster);
                } else {
                    result = SshHelper.sshExecute(ipAddress, port, getControlNodeLoginUser(),
                            pkFile, null, String.format("sudo /opt/bin/kubectl delete node %s", hostName),
                            10000, 10000, 30000);
                    if (result.first()) {
                        return true;
                    } else {
                        logger.warn("Deleting node: {} on VM: {} in Kubernetes cluster: {} unsuccessful", hostName, userVm, kubernetesCluster);
                    }
                }
                break;
            } catch (Exception e) {
                String msg = String.format("Failed to remove Kubernetes cluster: %s node: %s on VM: %s", kubernetesCluster, hostName, userVm);
                logger.warn(msg, e);
            }
            try {
                Thread.sleep(waitDuration);
            } catch (InterruptedException ie) {
                logger.error("Error while waiting for Kubernetes cluster: {} node: {} on VM: {} removal", kubernetesCluster, hostName, userVm, ie);
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

    private void removeNodesFromCluster(List<KubernetesClusterVmMapVO> vmMaps) throws CloudRuntimeException {
        IpAddress sshIp = null;
        try {
            sshIp = getNodeSshIp();
        } catch (ManagementServerException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes " +
                    "cluster %s, unable to update network rules", kubernetesCluster),
                    kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed, e);
        }
        for (KubernetesClusterVmMapVO vmMapVO : vmMaps) {
            UserVmVO userVM = userVmDao.findById(vmMapVO.getVmId());
            List<PortForwardingRuleVO> nodeSshRules = sshIp == null ? Collections.emptyList()
                    : listNodeSshPortForwardingRules(sshIp, vmMapVO.getVmId());
            logger.info("Removing vm {} from cluster {}", userVM, kubernetesCluster);
            if (!removeKubernetesClusterNode(publicIpAddress, sshPort, userVM, 3, 30000)) {
                logTransitStateAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes" +
                        " cluster %s, failed to remove Kubernetes node: %s running on VM : %s",
                        kubernetesCluster, userVM.getHostName(), userVM), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            }
            CallContext vmContext = CallContext.register(CallContext.current(),
                    ApiCommandResourceType.VirtualMachine);
            vmContext.setEventResourceId(userVM.getId());
            try {
                UserVm vm = userVmService.destroyVm(userVM.getId(), true);
                if (!userVmManager.expunge(userVM)) {
                    logTransitStateAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster %s failed, unable to expunge VM '%s'."
                            , kubernetesCluster, vm), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
                }
            } catch (ResourceUnavailableException e) {
                logTransitStateAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster %s failed, unable to remove VM ID: %s",
                        kubernetesCluster, userVM), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed, e);
            } finally {
                CallContext.unregister();
            }
            boolean sshRulesRemoved = true;
            if (sshIp != null) {
                try {
                    sshRulesRemoved = removeNodeSshRules(sshIp, nodeSshRules, network.getVpcId() == null);
                } catch (RuntimeException e) {
                    logger.error("Failed to remove the SSH network rules of VM {} of Kubernetes cluster {}", userVM, kubernetesCluster, e);
                    sshRulesRemoved = false;
                }
            }
            kubernetesClusterVmMapDao.expunge(vmMapVO.getId());
            if (!sshRulesRemoved) {
                logTransitStateAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster %s failed, unable to remove the SSH network rules of VM %s",
                        kubernetesCluster, userVM), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            }
            if (System.currentTimeMillis() > scaleTimeoutTime) {
                logTransitStateAndThrow(Level.WARN, String.format("Scaling Kubernetes cluster %s failed, scaling action timed out",
                        kubernetesCluster), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            }
        }

        if (sshIp != null && network.getVpcId() == null) {
            try {
                shrinkNodeSshFirewallRule(sshIp);
            } catch (ManagementServerException e) {
                logTransitStateAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes " +
                        "cluster %s, unable to update the SSH firewall rule", kubernetesCluster),
                        kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed, e);
            }
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
            List<Long> clusterVMIds = getKubernetesClusterVMMaps().stream().filter(vm -> !vm.isEtcdNode()).map(KubernetesClusterVmMapVO::getVmId).collect(Collectors.toList());
            provisionMissingNodeSshRules(clusterVMIds);
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
