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
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterEventTypes;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterUtil;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.UserVmVO;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.user.vm.RebootVMCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.Level;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class KubernetesClusterAddWorker extends KubernetesClusterActionWorker {

    @Inject
    private FirewallRulesDao firewallRulesDao;
    private long addNodeTimeoutTime;

    List<Long> finalNodeIds = new ArrayList<>();

    public KubernetesClusterAddWorker(KubernetesCluster kubernetesCluster, KubernetesClusterManagerImpl clusterManager) {
        super(kubernetesCluster, clusterManager);
    }

    public boolean addNodesToCluster(List<Long> nodeIds, boolean mountCksIsoOnVr, boolean manualUpgrade) throws CloudRuntimeException {
        try {
            init();
            addNodeTimeoutTime = System.currentTimeMillis() + KubernetesClusterService.KubernetesClusterAddNodeTimeout.value() * 1000;
            Long networkId = kubernetesCluster.getNetworkId();
            Network network = networkDao.findById(networkId);
            if (Objects.isNull(network)) {
                throw new CloudRuntimeException(String.format("Failed to find network with id: %s", networkId));
            }
            templateDao.findById(kubernetesCluster.getTemplateId());
            IpAddress publicIp = null;
            try {
                publicIp = getPublicIp(network);
            } catch (ManagementServerException e) {
                throw new CloudRuntimeException(String.format("Failed to retrieve public IP for the network: %s ", network.getName()));
            }
            attachCksIsoForNodesAdditionToCluster(nodeIds, kubernetesCluster.getId(), mountCksIsoOnVr);
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.AddNodeRequested);
            String controlNodeGuestIp = getControlVmPrivateIp();
            Ternary<Integer, Long, Long> nodesAddedAndMemory = importNodeToCluster(nodeIds, network, publicIp, controlNodeGuestIp, mountCksIsoOnVr);
            int nodesAdded = nodesAddedAndMemory.first();
            updateKubernetesCluster(kubernetesCluster.getId(), nodesAddedAndMemory, manualUpgrade);
            if (nodeIds.size() != nodesAdded) {
                String msg = String.format("Not every node was added to the CKS cluster %s, nodes added: %s out of %s", kubernetesCluster.getUuid(), nodesAdded, nodeIds.size());
                logger.info(msg);
                detachCksIsoFromNodesAddedToCluster(nodeIds, kubernetesCluster.getId(), mountCksIsoOnVr);
                stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
                ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), CallContext.current().getCallingAccountId(),
                        EventVO.LEVEL_ERROR, KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_NODES_ADD,
                        msg, kubernetesCluster.getId(), ApiCommandResourceType.KubernetesCluster.toString(), 0);
                return false;
            }
            Pair<String, Integer> publicIpSshPort = getKubernetesClusterServerIpSshPort(null);
            KubernetesClusterUtil.validateKubernetesClusterReadyNodesCount(kubernetesCluster, publicIpSshPort.first(), publicIpSshPort.second(),
                    getControlNodeLoginUser(), sshKeyFile, addNodeTimeoutTime, 15000);
            detachCksIsoFromNodesAddedToCluster(nodeIds, kubernetesCluster.getId(), mountCksIsoOnVr);
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
            String description = String.format("Successfully added %s nodes to Kubernetes Cluster %s", nodesAdded, kubernetesCluster.getUuid());
            ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), CallContext.current().getCallingAccountId(),
                    EventVO.LEVEL_INFO, KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_NODES_ADD,
                    description, kubernetesCluster.getId(), ApiCommandResourceType.KubernetesCluster.toString(), 0);
            return true;
        } catch (Exception e) {
            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            throw new CloudRuntimeException(e);
        }
    }

    private void detachCksIsoFromNodesAddedToCluster(List<Long> nodeIds, long kubernetesClusterId, boolean mountCksIsoOnVr) {
        if (mountCksIsoOnVr) {
            detachIsoOnVirtualRouter(kubernetesClusterId);
        } else {
            logger.info("Detaching CKS ISO from the nodes");
            List<UserVm> vms = nodeIds.stream().map(nodeId -> userVmDao.findById(nodeId)).collect(Collectors.toList());
            detachIsoKubernetesVMs(vms);
        }
    }

    public void detachIsoOnVirtualRouter(Long kubernetesClusterId) {
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        Long virtualRouterId = getVirtualRouterNicOnKubernetesClusterNetwork(kubernetesCluster).getInstanceId();
        long isoId = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId()).getIsoId();

        try {
            networkService.handleCksIsoOnNetworkVirtualRouter(virtualRouterId, false);
        } catch (ResourceUnavailableException e) {
            String err = String.format("Error trying to handle ISO %s on virtual router %s", isoId, virtualRouterId);
            logger.error(err);
            throw new CloudRuntimeException(err);
        }

        try {
            templateService.detachIso(virtualRouterId, isoId, true, true);
        } catch (CloudRuntimeException e) {
            String err = String.format("Error trying to detach ISO %s from virtual router %s", isoId, virtualRouterId);
            logger.error(err, e);
        }
    }

    public void attachCksIsoForNodesAdditionToCluster(List<Long> nodeIds, Long kubernetesClusterId, boolean mountCksIsoOnVr) {
        if (mountCksIsoOnVr) {
            attachAndServeIsoOnVirtualRouter(kubernetesClusterId);
        } else {
            logger.info("Attaching CKS ISO to the nodes");
            List<UserVm> vms = nodeIds.stream().map(nodeId -> userVmDao.findById(nodeId)).collect(Collectors.toList());
            attachIsoKubernetesVMs(vms);
        }
    }

    public void attachAndServeIsoOnVirtualRouter(Long kubernetesClusterId) {
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        Long virtualRouterId = getVirtualRouterNicOnKubernetesClusterNetwork(kubernetesCluster).getInstanceId();
        long isoId = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId()).getIsoId();

        try {
            templateService.attachIso(isoId, virtualRouterId, true, true);
        } catch (CloudRuntimeException e) {
            String err = String.format("Error trying to attach ISO %s to virtual router %s", isoId, virtualRouterId);
            logger.error(err);
            throw new CloudRuntimeException(err);
        }

        try {
            networkService.handleCksIsoOnNetworkVirtualRouter(virtualRouterId, true);
        } catch (ResourceUnavailableException e) {
            String err = String.format("Error trying to handle ISO %s on virtual router %s", isoId, virtualRouterId);
            logger.error(err);
            throw new CloudRuntimeException(err);
        }
    }

    private Ternary<Integer, Long, Long> importNodeToCluster(List<Long> nodeIds, Network network, IpAddress publicIp,
                                                             String controlNodeGuestIp, boolean mountCksIsoOnVr) {
        int nodeIndex = 0;
        Long additionalMemory = 0L;
        Long additionalCores = 0L;
        for (Long nodeId : nodeIds) {
            UserVmVO vm = userVmDao.findById(nodeId);
            String k8sControlNodeConfig = null;
            try {
                k8sControlNodeConfig = getKubernetesNodeConfig(controlNodeGuestIp, Hypervisor.HypervisorType.VMware.equals(clusterTemplate.getHypervisorType()), mountCksIsoOnVr);
            } catch (IOException e) {
                logAndThrow(Level.ERROR, "Failed to read Kubernetes control node configuration file", e);
            }
            if (Objects.isNull(k8sControlNodeConfig)) {
                logAndThrow(Level.ERROR, "Error generating worker node configuration");
            }
            String base64UserData = Base64.encodeBase64String(k8sControlNodeConfig.getBytes(com.cloud.utils.StringUtils.getPreferredCharset()));

            Pair<Boolean, Integer> result = validateAndSetupNode(network, publicIp, owner, nodeId,  nodeIndex, base64UserData);
            if (Boolean.TRUE.equals(result.first())) {
                ServiceOfferingVO offeringVO = serviceOfferingDao.findById(vm.getId(), vm.getServiceOfferingId());
                additionalMemory += offeringVO.getRamSize();
                additionalCores += offeringVO.getCpu();
                String msg = String.format("VM %s added as a node on the Kubernetes Cluster %s", vm.getUuid(), kubernetesCluster.getUuid());
                ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), CallContext.current().getCallingAccountId(),
                        EventVO.LEVEL_INFO, KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_NODES_ADD,
                        msg, vm.getId(), ApiCommandResourceType.VirtualMachine.toString(), 0);
            }
            if (Boolean.FALSE.equals(result.first())) {
                logger.error(String.format("Failed to add node %s [%s] to Kubernetes cluster : %s", vm.getName(), vm.getUuid(), kubernetesCluster.getName()));
            }
            if (System.currentTimeMillis() > addNodeTimeoutTime) {
                logger.error(String.format("Failed to add node %s to Kubernetes cluster : %s", nodeId, kubernetesCluster.getName()));
            }
            nodeIndex = result.second();
        }
        return new Ternary<>(nodeIndex, additionalMemory, additionalCores);
    }

    private Pair<Boolean, Integer> validateAndSetupNode(Network network, IpAddress publicIp, Account account,
                                   Long nodeId, int nodeIndex, String base64UserData) {
        int startSshPortNumber = KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT + (int) kubernetesCluster.getTotalNodeCount() - kubernetesCluster.getEtcdNodeCount().intValue();
        int sshStartPort = startSshPortNumber + nodeIndex;
        try {
            if (Objects.isNull(network.getVpcId())) {
                provisionFirewallRules(publicIp, owner, sshStartPort, sshStartPort);
            }
            provisionPublicIpPortForwardingRule(publicIp, network, account, nodeId, sshStartPort, DEFAULT_SSH_PORT);
            boolean isCompatible = validateNodeCompatibility(publicIp, nodeId, sshStartPort);
            if (!isCompatible) {
                revertNetworkRules(network, nodeId, sshStartPort);
                return new Pair<>(false, nodeIndex);
            }

            userVmManager.updateVirtualMachine(nodeId, null, null, null, null,
                    null, null, base64UserData, null, null, null,
                    BaseCmd.HTTPMethod.POST, null, null, null, null, null);

            RebootVMCmd rebootVMCmd = new RebootVMCmd();
            Field idField = rebootVMCmd.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(rebootVMCmd, nodeId);
            userVmService.rebootVirtualMachine(rebootVMCmd);
            finalNodeIds.add(nodeId);
        } catch (ResourceUnavailableException | NetworkRuleConflictException | NoSuchFieldException |
                 InsufficientCapacityException | IllegalAccessException e) {
            logger.error(String.format("Failed to activate API port forwarding rules for the Kubernetes cluster : %s", kubernetesCluster.getName()));
            // remove added Firewall and PF rules
            revertNetworkRules(network, nodeId, sshStartPort);
            return new Pair<>( false, nodeIndex);
        } catch (Exception e) {
            String errMsg = String.format("Unexpected exception while trying to add the external node %s to the Kubernetes cluster %s: %s",
                    nodeId, kubernetesCluster.getName(), e.getMessage());
            logger.error(errMsg, e);
            revertNetworkRules(network, nodeId, sshStartPort);
            throw new CloudRuntimeException(e);
        }
        return new Pair<>(true, ++nodeIndex);
    }

    private void updateKubernetesCluster(long clusterId, Ternary<Integer, Long, Long> additionalNodesDetails, boolean manualUpgrade) {
        int additionalNodeCount = additionalNodesDetails.first();
        KubernetesClusterVO kubernetesClusterVO = kubernetesClusterDao.findById(clusterId);
        kubernetesClusterVO.setNodeCount(kubernetesClusterVO.getNodeCount() + additionalNodeCount);
        kubernetesClusterVO.setMemory(kubernetesClusterVO.getMemory() + additionalNodesDetails.second());
        kubernetesClusterVO.setCores(kubernetesClusterVO.getCores() + additionalNodesDetails.third());
        kubernetesClusterDao.update(clusterId, kubernetesClusterVO);
        kubernetesCluster = kubernetesClusterVO;

        finalNodeIds.forEach(id -> addKubernetesClusterVm(clusterId, id, false, true, false, manualUpgrade));
    }


    private boolean validateNodeCompatibility(IpAddress publicIp, long nodeId, int nodeSshPort) throws CloudRuntimeException {
        File pkFile = getManagementServerSshPublicKeyFile();
        try {
            File validateNodeScriptFile = retrieveScriptFile(validateNodeScript);
            Thread.sleep(15*1000);
            copyScriptFile(publicIp.getAddress().addr(), nodeSshPort, validateNodeScriptFile, validateNodeScript);
            String command = String.format("%s%s", scriptPath, validateNodeScript);
            Pair<Boolean, String> result = SshHelper.sshExecute(publicIp.getAddress().addr(), nodeSshPort, getControlNodeLoginUser(),
                    pkFile, null, command, 10000, 10000, 10 * 60 * 1000);
            if (Boolean.FALSE.equals(result.first())) {
                logger.error(String.format("Node with ID: %s cannot be added as a worker node as it does not have " +
                        "the following dependencies: %s ", nodeId, result.second()));
                return false;
            }
        } catch (Exception e) {
            logger.error(String.format("Failed to validate node with ID: %s", nodeId), e);
            return false;
        }
        UserVmVO userVm = userVmDao.findById(nodeId);
        cleanupCloudInitSemFolder(userVm, publicIp, pkFile, nodeSshPort);
        return true;
    }

    private void cleanupCloudInitSemFolder(UserVm userVm, IpAddress publicIp, File pkFile, int nodeSshPort) {
        try {
            String command = String.format("sudo rm -rf /var/lib/cloud/instances/%s/sem/*", userVm.getUuid());
            Pair<Boolean, String> result = SshHelper.sshExecute(publicIp.getAddress().addr(), nodeSshPort, getControlNodeLoginUser(),
                    pkFile, null, command, 10000, 10000, 10 * 60 * 1000);
            if (Boolean.FALSE.equals(result.first())) {
                logger.error(String.format("Failed to cleanup previous applied userdata on node: %s; This may hamper to addition of the node to the cluster ", userVm.getName()));
            }
        } catch (Exception e) {
            logger.error(String.format("Failed to cleanup previous applied userdata on node: %s; This may hamper to addition of the node to the cluster ", userVm.getName()), e);
        }
    }

    private void revertNetworkRules(Network network, long vmId, int port) {
        logger.debug(String.format("Reverting network rules for VM ID %s on network %s", vmId, network.getName()));
        FirewallRuleVO ruleVO = firewallRulesDao.findByNetworkIdAndPorts(network.getId(), port, port);
        if (Objects.isNull(network.getVpcId())) {
            logger.debug(String.format("Removing firewall rule %s", ruleVO.getId()));
            firewallService.revokeIngressFirewallRule(ruleVO.getId(), true);
        }
        List<PortForwardingRuleVO> pfRules = portForwardingRulesDao.listByVm(vmId);
        for (PortForwardingRuleVO pfRule : pfRules) {
            logger.debug(String.format("Removing port forwarding rule %s", pfRule.getId()));
            rulesService.revokePortForwardingRule(pfRule.getId(), true);
        }
    }
}
