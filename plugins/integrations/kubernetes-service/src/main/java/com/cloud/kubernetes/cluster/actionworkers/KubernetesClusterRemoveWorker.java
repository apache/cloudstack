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

import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventVO;
import com.cloud.exception.ManagementServerException;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterEventTypes;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.UserVmVO;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.context.CallContext;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class KubernetesClusterRemoveWorker extends KubernetesClusterActionWorker {

    @Inject
    private FirewallRulesDao firewallRulesDao;

    private long removeNodeTimeoutTime;

    public KubernetesClusterRemoveWorker(KubernetesCluster kubernetesCluster, KubernetesClusterManagerImpl clusterManager) {
        super(kubernetesCluster, clusterManager);
    }

    public boolean removeNodesFromCluster(List<Long> nodeIds) {
        init();
        removeNodeTimeoutTime = System.currentTimeMillis() + KubernetesClusterService.KubernetesClusterRemoveNodeTimeout.value() * 1000;
        Long networkId = kubernetesCluster.getNetworkId();
        Network network = networkDao.findById(networkId);
        if (Objects.isNull(network)) {
            throw new CloudRuntimeException(String.format("Failed to find network with id: %s", networkId));
        }
        IpAddress publicIp = null;
        try {
            publicIp = getPublicIp(network);
        } catch (ManagementServerException e) {
            throw new CloudRuntimeException(String.format("Failed to retrieve public IP for the network: %s ", network.getName()));
        }
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.RemoveNodeRequested);
        boolean result = removeNodesFromCluster(nodeIds, network, publicIp);
        if (!result) {
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        } else {
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
        }
        String description = String.format("Successfully removed %s nodes from the Kubernetes Cluster %s", nodeIds.size(), kubernetesCluster.getUuid());
        ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), CallContext.current().getCallingAccountId(),
                EventVO.LEVEL_INFO, KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_NODES_REMOVE,
                description, kubernetesCluster.getId(), ApiCommandResourceType.KubernetesCluster.toString(), 0);
        return result;
    }

    private boolean removeNodesFromCluster(List<Long> nodeIds, Network network, IpAddress publicIp) {
        boolean result = true;
        List<Long> removedNodeIds = new ArrayList<>();
        long removedMemory = 0L;
        long removedCores = 0L;
        for (Long nodeId : nodeIds) {
            UserVmVO vm = userVmDao.findById(nodeId);
            if (vm == null) {
                logger.debug(String.format("Couldn't find a VM with ID %s, skipping removal from Kubernetes cluster", nodeId));
                continue;
            }
            try {
                removeNodeVmFromCluster(nodeId, vm.getDisplayName().toLowerCase(Locale.ROOT), publicIp.getAddress().addr());
                result &= removeNodePortForwardingRules(nodeId, network, publicIp, vm);
                if (System.currentTimeMillis() > removeNodeTimeoutTime) {
                    logger.error(String.format("Removal of node %s from Kubernetes cluster %s timed out", vm.getName(), kubernetesCluster.getName()));
                    result = false;
                    continue;
                }
                ServiceOfferingVO offeringVO = serviceOfferingDao.findById(vm.getId(), vm.getServiceOfferingId());
                removedNodeIds.add(nodeId);
                removedMemory += offeringVO.getRamSize();
                removedCores += offeringVO.getCpu();
                String description = String.format("Successfully removed the node %s from Kubernetes cluster %s", vm.getUuid(), kubernetesCluster.getUuid());
                logger.info(description);
                ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), CallContext.current().getCallingAccountId(),
                        EventVO.LEVEL_INFO, KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_NODES_REMOVE,
                        description, vm.getId(), ApiCommandResourceType.VirtualMachine.toString(), 0);
            } catch (Exception e) {
                String err = String.format("Error trying to remove node %s from Kubernetes Cluster %s: %s", vm.getUuid(), kubernetesCluster.getUuid(), e.getMessage());
                logger.error(err, e);
                result = false;
            }
        }
        updateKubernetesCluster(kubernetesCluster.getId(), removedNodeIds, removedMemory, removedCores);
        return result;
    }

    protected boolean removeNodePortForwardingRules(Long nodeId, Network network, IpAddress publicIp, UserVmVO vm) {
        List<PortForwardingRuleVO> pfRules = portForwardingRulesDao.listByVm(nodeId).stream()
                .filter(rule -> isNodeSshPfRule(rule, publicIp)).collect(Collectors.toList());
        boolean result = true;
        for (PortForwardingRuleVO pfRule : pfRules) {
            if (Objects.isNull(network.getVpcId())) {
                try {
                    for (FirewallRuleVO ruleVO : listNodeSshFirewallRules(publicIp, pfRule.getSourcePortStart())) {
                        result &= revokeNodeRule(ruleVO, vm);
                    }
                } catch (Exception e) {
                    logger.error(String.format("Failed to find the firewall rule on port %s of node %s, due to: %s",
                            pfRule.getSourcePortStart(), vm.getName(), e.getMessage()), e);
                    result = false;
                }
            }
            result &= revokeNodeRule(pfRule, vm);
        }
        return result;
    }

    private boolean revokeNodeRule(FirewallRuleVO rule, UserVmVO vm) {
        try {
            if (rule.getPurpose() == FirewallRule.Purpose.PortForwarding) {
                return rulesService.revokePortForwardingRule(rule.getId(), true);
            }
            return firewallService.revokeIngressFirewallRule(rule.getId(), true);
        } catch (Exception e) {
            logger.error(String.format("Failed to remove network rule %s of node %s, due to: %s", rule.getUuid(), vm.getName(), e.getMessage()), e);
            return false;
        }
    }

    private boolean isNodeSshPfRule(PortForwardingRuleVO pfRule, IpAddress publicIp) {
        return Objects.equals(pfRule.getSourceIpAddressId(), publicIp.getId()) && Objects.nonNull(pfRule.getSourcePortStart())
                && Objects.equals(pfRule.getSourcePortStart(), pfRule.getSourcePortEnd()) && NetUtils.TCP_PROTO.equalsIgnoreCase(pfRule.getProtocol())
                && pfRule.getDestinationPortStart() == DEFAULT_SSH_PORT && pfRule.getDestinationPortEnd() == DEFAULT_SSH_PORT
                && pfRule.getState() != FirewallRule.State.Revoke;
    }

    private List<FirewallRuleVO> listNodeSshFirewallRules(IpAddress publicIp, int port) {
        return firewallRulesDao.listByIpPurposePortsProtocolAndNotRevoked(publicIp.getId(), port, port, NetUtils.TCP_PROTO, FirewallRule.Purpose.Firewall);
    }

    private void removeNodeVmFromCluster(Long nodeId, String nodeName, String publicIp) throws Exception {
        File removeNodeScriptFile = retrieveScriptFile(removeNodeFromClusterScript);
        copyScriptFile(publicIp, CLUSTER_NODES_DEFAULT_START_SSH_PORT, removeNodeScriptFile, removeNodeFromClusterScript);
        File pkFile = getManagementServerSshPublicKeyFile();
        String command = String.format("%s%s %s %s %s", scriptPath, removeNodeFromClusterScript, nodeName, "control", "remove");
        Pair<Boolean, String> result = SshHelper.sshExecute(publicIp, CLUSTER_NODES_DEFAULT_START_SSH_PORT, getControlNodeLoginUser(),
                pkFile, null, command, 10000, 10000, 10 * 60 * 1000);
        if (Boolean.FALSE.equals(result.first())) {
            logger.error(String.format("Node: %s failed to be gracefully drained as a worker node from cluster %s ", nodeName, kubernetesCluster.getName()));
        }
        List<PortForwardingRuleVO> nodePfRules = portForwardingRulesDao.listByVm(nodeId);
        Optional<PortForwardingRuleVO> nodeSshPort = nodePfRules.stream().filter(rule -> rule.getDestinationPortStart() == DEFAULT_SSH_PORT
                && rule.getVirtualMachineId() == nodeId && rule.getSourcePortStart() >= CLUSTER_NODES_DEFAULT_START_SSH_PORT).findFirst();
        if (nodeSshPort.isPresent()) {
            copyScriptFile(publicIp, nodeSshPort.get().getSourcePortStart(), removeNodeScriptFile, removeNodeFromClusterScript);
            command = String.format("sudo %s%s %s %s %s", scriptPath, removeNodeFromClusterScript, nodeName, "worker", "remove");
            result = SshHelper.sshExecute(publicIp, nodeSshPort.get().getSourcePortStart(), getControlNodeLoginUser(),
                    pkFile, null, command, 10000, 10000, 10 * 60 * 1000);
            if (Boolean.FALSE.equals(result.first())) {
                logger.error(String.format("Failed to reset node: %s from cluster %s ", nodeName, kubernetesCluster.getName()));
            }
            command = String.format("%s%s %s %s %s", scriptPath, removeNodeFromClusterScript, nodeName, "control", "delete");
            result = SshHelper.sshExecute(publicIp, CLUSTER_NODES_DEFAULT_START_SSH_PORT, getControlNodeLoginUser(),
                    pkFile, null, command, 10000, 10000, 10 * 60 * 1000);
            if (Boolean.FALSE.equals(result.first())) {
                logger.error(String.format("Node: %s failed to be gracefully delete node from cluster %s ", nodeName, kubernetesCluster.getName()));
            }

        }
    }

    private void updateKubernetesCluster(long clusterId, List<Long> nodesRemoved, long deallocatedRam, long deallocatedCores) {
        KubernetesClusterVO kubernetesClusterVO = kubernetesClusterDao.findById(clusterId);
        kubernetesClusterVO.setNodeCount(kubernetesClusterVO.getNodeCount() - nodesRemoved.size());
        kubernetesClusterVO.setMemory(kubernetesClusterVO.getMemory() - deallocatedRam);
        kubernetesClusterVO.setCores(kubernetesClusterVO.getCores() - deallocatedCores);
        kubernetesClusterDao.update(clusterId, kubernetesClusterVO);

        nodesRemoved.forEach(id -> kubernetesClusterVmMapDao.removeByClusterIdAndVmIdsIn(clusterId, nodesRemoved));
    }
}
