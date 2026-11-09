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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cloudstack.api.InternalIdentity;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterNodeGroupVO;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterNodeGroupUtil;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterUtil;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.VMTemplateVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.UserVmVO;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.WORKER;

/**
 * Provisions, scales and removes the worker nodes of a node group of a CloudManaged Kubernetes cluster.
 * The nodes of a node group are deployed like the other worker nodes, but with the service offering, template,
 * Kubernetes node labels and taints of the node group.
 * Operations on different node groups of a cluster can run at the same time: the state of the cluster is managed by
 * {@link KubernetesClusterManagerImpl} for all of them, the counters of the cluster are updated under a row lock and
 * the network rules of the cluster under a cluster lock, and only the nodes of the node group are cleaned up on failure.
 */
public class KubernetesClusterNodeGroupWorker extends KubernetesClusterScaleWorker {

    private static final int MAX_HOST_NAME_LENGTH = 63;
    private static final int NODE_READY_WAIT_DURATION = 15000;

    private KubernetesClusterNodeGroupVO nodeGroup;

    public KubernetesClusterNodeGroupWorker(final KubernetesCluster kubernetesCluster,
                                            final KubernetesClusterNodeGroupVO nodeGroup,
                                            final KubernetesClusterManagerImpl clusterManager) {
        super(kubernetesCluster, clusterManager);
        this.nodeGroup = nodeGroup;
        this.originalState = kubernetesCluster.getState();
    }

    @Override
    protected void init() {
        super.init();
        VMTemplateVO nodeGroupTemplate = templateDao.findById(nodeGroup.getTemplateId());
        if (nodeGroupTemplate != null) {
            this.workerNodeTemplate = nodeGroupTemplate;
        }
        this.scaleTimeoutTime = System.currentTimeMillis() + KubernetesClusterService.KubernetesClusterScaleTimeout.value() * 1000;
    }

    @Override
    protected KubernetesClusterNodeGroupVO getWorkerNodeGroup() {
        return nodeGroup;
    }

    @Override
    protected String getWorkerNodeHostName(String suffix) {
        if (nodeGroup.isDefault()) {
            return super.getWorkerNodeHostName(suffix);
        }
        String nodeGroupName = nodeGroup.getName();
        int maxPrefixLength = MAX_HOST_NAME_LENGTH - nodeGroupName.length() - suffix.length() - 2;
        String prefix = StringUtils.stripEnd(StringUtils.truncate(kubernetesClusterNodeNamePrefix, maxPrefixLength), "-");
        return String.format("%s-%s-%s", prefix, nodeGroupName, suffix);
    }

    @Override
    protected ServiceOffering getServiceOfferingForNodeTypeOnCluster(KubernetesClusterNodeType nodeType, KubernetesCluster cluster) {
        if (WORKER == nodeType) {
            return serviceOfferingDao.findById(nodeGroup.getServiceOfferingId());
        }
        return super.getServiceOfferingForNodeTypeOnCluster(nodeType, cluster);
    }

    /**
     * Node group operations never change the state of the cluster, which is managed by the manager for all the node
     * groups of the cluster, so failures only log and throw
     */
    @Override
    protected void logTransitStateAndThrow(final Level logLevel, final String message, final Long kubernetesClusterId,
                                           final KubernetesCluster.Event event, final Exception e) throws CloudRuntimeException {
        super.logTransitStateAndThrow(logLevel, message, null, null, e);
    }

    @Override
    protected boolean stateTransitTo(long kubernetesClusterId, KubernetesCluster.Event e) {
        logger.debug("Not changing the state of the Kubernetes cluster {} on event {} during an operation on its node group {}",
                kubernetesCluster, e, nodeGroup);
        return true;
    }

    /**
     * The launch permission of the default template is granted and revoked by
     * {@link #grantTemplateLaunchPermissionIfNeeded()} as other node groups may be using it at the same time
     */
    @Override
    protected void deleteTemplateLaunchPermission() {
    }

    protected List<KubernetesClusterVmMapVO> getNodeGroupVmMaps() {
        return kubernetesClusterVmMapDao.listByNodeGroupId(nodeGroup.getId());
    }

    /**
     * Removes the VMs of the node group which were not part of it before a failed scale up, the VMs of the other
     * node groups which may be scaled up at the same time are kept
     */
    @Override
    protected void cleanupNewlyCreatedVms(Set<Long> originalVmIds) {
        Set<Long> otherVmIds = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId()).stream()
                .filter(vmMap -> !Long.valueOf(nodeGroup.getId()).equals(vmMap.getNodeGroupId()))
                .map(KubernetesClusterVmMapVO::getVmId).collect(Collectors.toSet());
        otherVmIds.addAll(originalVmIds);
        super.cleanupNewlyCreatedVms(otherVmIds);
    }

    /**
     * Recreates the SSH firewall and port forwarding rules of all the nodes of the cluster. The node list is taken
     * under the cluster network rules lock, as other node groups may add or remove nodes at the same time
     */
    @Override
    protected void scaleKubernetesClusterNetworkRules(final List<Long> clusterVMIds) throws ManagementServerException {
        try {
            manager.executeWithKubernetesClusterLock(kubernetesCluster.getId(), KubernetesClusterManagerImpl.NETWORK_RULES_LOCK, () -> {
                List<KubernetesClusterVmMapVO> vmMaps = getKubernetesClusterVMMaps();
                List<Long> vmIds = vmMaps.stream().filter(vm -> !vm.isExternalNode() && !vm.isEtcdNode())
                        .map(KubernetesClusterVmMapVO::getVmId).collect(Collectors.toList());
                vmIds.addAll(vmMaps.stream().filter(KubernetesClusterVmMapVO::isExternalNode)
                        .map(KubernetesClusterVmMapVO::getVmId).collect(Collectors.toList()));
                try {
                    super.scaleKubernetesClusterNetworkRules(vmIds);
                } catch (ManagementServerException e) {
                    throw new CloudRuntimeException(e.getMessage(), e);
                }
                return null;
            });
        } catch (CloudRuntimeException e) {
            throw new ManagementServerException(e.getMessage(), e);
        }
    }

    private void initClusterServerIpSshPort() {
        Pair<String, Integer> publicIpSshPort = getKubernetesClusterServerIpSshPort(null);
        publicIpAddress = publicIpSshPort.first();
        sshPort = publicIpSshPort.second();
        if (StringUtils.isEmpty(publicIpAddress)) {
            logAndThrow(Level.ERROR, String.format("Unable to retrieve the public IP address of the Kubernetes cluster : %s", kubernetesCluster.getName()));
        }
    }

    /**
     * Updates the node count and service offering of the node group and the counters of the cluster from the nodes
     * of the node group, which keeps them consistent even when an operation fails half-way
     * @param newServiceOffering the new service offering of the node group, null to keep the existing one
     */
    protected void syncNodeGroupEntry(final ServiceOffering newServiceOffering) {
        nodeGroup = manager.syncNodeGroupEntries(kubernetesCluster.getId(), nodeGroup.getId(),
                newServiceOffering != null ? newServiceOffering.getId() : null);
        kubernetesCluster = kubernetesClusterDao.findById(kubernetesCluster.getId());
    }

    /**
     * Grants the owner of the cluster the permission to deploy the node group nodes from the default CKS template,
     * which is owned by the system. The permission is revoked by the last node group operation of the cluster using it
     * @return whether the template of the node group requires the permission, to be released after the scale up
     */
    private boolean grantTemplateLaunchPermissionIfNeeded() {
        if (owner == null || nodeGroup.getTemplateId() != kubernetesCluster.getTemplateId()) {
            return false;
        }
        manager.grantNodeGroupTemplateLaunchPermission(kubernetesCluster.getId(), nodeGroup.getTemplateId(), owner.getId());
        return true;
    }

    private void revokeTemplateLaunchPermission() {
        manager.revokeNodeGroupTemplateLaunchPermission(kubernetesCluster.getId(), nodeGroup.getId(), nodeGroup.getTemplateId(), owner.getId());
    }

    /**
     * Waits for the given new nodes to join the cluster and become ready, the nodes added by other node groups
     * at the same time are not waited for
     */
    protected boolean waitForNodesReady(final List<UserVm> nodeVMs) {
        for (UserVm vm : nodeVMs) {
            String nodeName = vm.getHostName().toLowerCase();
            if (!KubernetesClusterUtil.isKubernetesClusterNodeReady(kubernetesCluster, publicIpAddress, sshPort,
                    getControlNodeLoginUser(), sshKeyFile, nodeName, scaleTimeoutTime, NODE_READY_WAIT_DURATION)) {
                logger.warn("Node {} of node group {} of Kubernetes cluster {} is not ready", nodeName, nodeGroup, kubernetesCluster);
                return false;
            }
        }
        return true;
    }

    private void rollbackScaleUp(final Set<Long> originalVmIds, final String message, final Exception e) throws CloudRuntimeException {
        cleanupNewlyCreatedVms(originalVmIds);
        try {
            scaleKubernetesClusterNetworkRules(null);
        } catch (ManagementServerException ex) {
            logger.warn("Failed to update the network rules of Kubernetes cluster {} after removing the new nodes of node group {}",
                    kubernetesCluster, nodeGroup, ex);
        }
        syncNodeGroupEntry(null);
        logAndThrow(Level.ERROR, message, e);
    }

    private void scaleUpNodeGroup(final long newVmCount) throws CloudRuntimeException {
        Set<Long> originalVmIds = getNodeGroupVmMaps().stream().map(KubernetesClusterVmMapVO::getVmId).collect(Collectors.toSet());
        boolean releaseTemplateLaunchPermission = grantTemplateLaunchPermissionIfNeeded();
        try {
            List<UserVm> nodeVMs = new ArrayList<>();
            try {
                nodeVMs = provisionKubernetesClusterNodeVms(newVmCount, 0, publicIpAddress, kubernetesCluster.getDomainId(), kubernetesCluster.getAccountId());
                updateLoginUserDetails(nodeVMs.stream().map(InternalIdentity::getId).collect(Collectors.toList()));
            } catch (CloudRuntimeException | ManagementServerException | ResourceUnavailableException | InsufficientCapacityException e) {
                rollbackScaleUp(originalVmIds, String.format("Scaling failed for node group %s of Kubernetes cluster : %s, unable to provision node VM",
                        nodeGroup.getName(), kubernetesCluster.getName()), e);
            }
            try {
                scaleKubernetesClusterNetworkRules(null);
            } catch (ManagementServerException e) {
                rollbackScaleUp(originalVmIds, String.format("Scaling failed for node group %s of Kubernetes cluster : %s, unable to update network rules",
                        nodeGroup.getName(), kubernetesCluster.getName()), e);
            }
            attachIsoKubernetesVMs(nodeVMs);
            syncNodeGroupEntry(null);
            boolean nodesReady = waitForNodesReady(nodeVMs);
            detachIsoKubernetesVMs(nodeVMs);
            if (!nodesReady) {
                rollbackScaleUp(originalVmIds, String.format("Scaling unsuccessful for node group %s of Kubernetes cluster : %s as its new nodes are not ready",
                        nodeGroup.getName(), kubernetesCluster.getName()), null);
            }
        } finally {
            if (releaseTemplateLaunchPermission) {
                revokeTemplateLaunchPermission();
            }
        }
    }

    private void scaleDownNodeGroup(final List<KubernetesClusterVmMapVO> vmMaps) throws CloudRuntimeException {
        if (CollectionUtils.isEmpty(vmMaps)) {
            return;
        }
        try {
            removeNodesFromCluster(vmMaps);
        } finally {
            syncNodeGroupEntry(null);
        }
    }

    private void scaleNodeGroupOffering(final ServiceOffering serviceOffering) throws CloudRuntimeException {
        for (KubernetesClusterVmMapVO vmMap : getNodeGroupVmMaps()) {
            UserVmVO userVM = userVmDao.findById(vmMap.getVmId());
            if (userVM.getServiceOfferingId() == serviceOffering.getId()) {
                continue;
            }
            boolean result = false;
            try {
                result = userVmManager.upgradeVirtualMachine(userVM.getId(), serviceOffering.getId(), new HashMap<>());
            } catch (RuntimeException | ResourceUnavailableException | ManagementServerException | VirtualMachineMigrationException e) {
                logAndThrow(Level.ERROR, String.format("Scaling node group %s of Kubernetes cluster : %s failed, unable to scale VM : %s due to %s",
                        nodeGroup.getName(), kubernetesCluster.getName(), userVM.getDisplayName(), e.getMessage()), e);
            }
            if (!result) {
                logAndThrow(Level.WARN, String.format("Scaling node group %s of Kubernetes cluster : %s failed, unable to scale VM : %s",
                        nodeGroup.getName(), kubernetesCluster.getName(), userVM.getDisplayName()));
            }
            if (System.currentTimeMillis() > scaleTimeoutTime) {
                logAndThrow(Level.WARN, String.format("Scaling node group %s of Kubernetes cluster : %s failed, scaling action timed out",
                        nodeGroup.getName(), kubernetesCluster.getName()));
            }
        }
        syncNodeGroupEntry(serviceOffering);
    }

    /**
     * Scales the node group for its size and/or service offering
     * @param size new number of nodes of the node group, null to keep the current size
     * @param nodeIds IDs of the node group VMs to be removed, can not be passed along with size
     * @param serviceOffering new service offering of the node group, null to keep the current one
     */
    public boolean scaleNodeGroup(final Long size, final List<Long> nodeIds, final ServiceOffering serviceOffering) throws CloudRuntimeException {
        init();
        logger.info("Scaling node group {} of Kubernetes cluster {}", nodeGroup, kubernetesCluster);
        initClusterServerIpSshPort();
        if (serviceOffering != null && serviceOffering.getId() != nodeGroup.getServiceOfferingId()) {
            scaleNodeGroupOffering(serviceOffering);
        }
        List<KubernetesClusterVmMapVO> vmMaps = getNodeGroupVmMaps();
        if (CollectionUtils.isNotEmpty(nodeIds)) {
            List<KubernetesClusterVmMapVO> vmMapsToRemove = vmMaps.stream()
                    .filter(vmMap -> nodeIds.contains(vmMap.getVmId())).collect(Collectors.toList());
            scaleDownNodeGroup(vmMapsToRemove);
        } else if (size != null && size < vmMaps.size()) {
            // Remove the most recently added nodes first
            List<KubernetesClusterVmMapVO> vmMapsToRemove = new ArrayList<>(vmMaps.subList(size.intValue(), vmMaps.size()));
            Collections.reverse(vmMapsToRemove);
            scaleDownNodeGroup(vmMapsToRemove);
        } else if (size != null && size > vmMaps.size()) {
            scaleUpNodeGroup(size - vmMaps.size());
        }
        return true;
    }

    /**
     * Removes all the nodes of the node group from the cluster and removes the node group
     */
    public boolean deleteNodeGroup() throws CloudRuntimeException {
        init();
        logger.info("Deleting node group {} of Kubernetes cluster {}", nodeGroup, kubernetesCluster);
        List<KubernetesClusterVmMapVO> vmMaps = getNodeGroupVmMaps();
        if (CollectionUtils.isNotEmpty(vmMaps)) {
            initClusterServerIpSshPort();
            List<KubernetesClusterVmMapVO> vmMapsToRemove = new ArrayList<>(vmMaps);
            Collections.reverse(vmMapsToRemove);
            scaleDownNodeGroup(vmMapsToRemove);
        }
        kubernetesClusterNodeGroupDao.remove(nodeGroup.getId());
        return true;
    }

    /**
     * Brings the labels and taints of a node from the old ones to the new ones, both being the merge of the labels and
     * taints of the node group with the labels and taints of the node. The node group label is (re)applied too
     */
    private void applyNodeLabelsAndTaints(final KubernetesClusterVmMapVO vmMap, final String oldLabels, final String newLabels,
                                          final String oldTaints, final String newTaints) throws CloudRuntimeException {
        UserVmVO userVM = userVmDao.findById(vmMap.getVmId());
        if (userVM == null) {
            return;
        }
        String nodeName = userVM.getHostName().toLowerCase();
        for (String command : KubernetesClusterNodeGroupUtil.getLabelAndTaintUpdateCommands(nodeName, nodeGroup.getName(),
                oldLabels, newLabels, oldTaints, newTaints)) {
            Pair<Boolean, String> result = null;
            try {
                result = SshHelper.sshExecute(publicIpAddress, sshPort, getControlNodeLoginUser(), sshKeyFile, null,
                        command, 10000, 10000, 60000);
            } catch (Exception e) {
                logAndThrow(Level.ERROR, String.format("Failed to update labels and taints of node %s in node group %s of Kubernetes cluster : %s",
                        nodeName, nodeGroup.getName(), kubernetesCluster.getName()), e);
            }
            if (!result.first()) {
                logAndThrow(Level.ERROR, String.format("Failed to update labels and taints of node %s in node group %s of Kubernetes cluster : %s, %s",
                        nodeName, nodeGroup.getName(), kubernetesCluster.getName(), result.second()));
            }
        }
    }

    /**
     * Applies the labels and taints of the node group, merged with the labels and taints of each node, as well as the
     * node group label to the existing nodes of the node group
     * @param oldLabels labels of the node group before the update, removed from the nodes when not present anymore
     * @param oldTaints taints of the node group before the update, removed from the nodes when not present anymore
     */
    public boolean updateNodeGroupLabelsAndTaints(final String oldLabels, final String oldTaints) throws CloudRuntimeException {
        init();
        List<KubernetesClusterVmMapVO> vmMaps = getNodeGroupVmMaps();
        if (CollectionUtils.isEmpty(vmMaps)) {
            return true;
        }
        initClusterServerIpSshPort();
        for (KubernetesClusterVmMapVO vmMap : vmMaps) {
            applyNodeLabelsAndTaints(vmMap,
                    KubernetesClusterNodeGroupUtil.mergeLabels(oldLabels, vmMap.getLabels()),
                    KubernetesClusterNodeGroupUtil.mergeLabels(nodeGroup.getLabels(), vmMap.getLabels()),
                    KubernetesClusterNodeGroupUtil.mergeTaints(oldTaints, vmMap.getTaints()),
                    KubernetesClusterNodeGroupUtil.mergeTaints(nodeGroup.getTaints(), vmMap.getTaints()));
        }
        return true;
    }

    /**
     * Applies the labels and taints of a node of the node group, merged with the labels and taints of the node group
     * @param vmMap the node, with its new labels and taints
     * @param oldNodeLabels labels of the node before the update, removed from the node when not present anymore
     * @param oldNodeTaints taints of the node before the update, removed from the node when not present anymore
     */
    public boolean updateNodeLabelsAndTaints(final KubernetesClusterVmMapVO vmMap, final String oldNodeLabels,
                                             final String oldNodeTaints) throws CloudRuntimeException {
        init();
        initClusterServerIpSshPort();
        applyNodeLabelsAndTaints(vmMap,
                KubernetesClusterNodeGroupUtil.mergeLabels(nodeGroup.getLabels(), oldNodeLabels),
                KubernetesClusterNodeGroupUtil.mergeLabels(nodeGroup.getLabels(), vmMap.getLabels()),
                KubernetesClusterNodeGroupUtil.mergeTaints(nodeGroup.getTaints(), oldNodeTaints),
                KubernetesClusterNodeGroupUtil.mergeTaints(nodeGroup.getTaints(), vmMap.getTaints()));
        return true;
    }

    /**
     * Deploys the cluster autoscaler for the node groups of the cluster with autoscaling enabled, or removes it when
     * autoscaling is not enabled for any node group anymore
     */
    public boolean updateClusterAutoscaler() throws CloudRuntimeException {
        init();
        applyClusterAutoscaler();
        return true;
    }
}
