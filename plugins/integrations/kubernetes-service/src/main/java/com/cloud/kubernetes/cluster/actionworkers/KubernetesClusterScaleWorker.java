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
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Level;

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
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterUtil;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.base.Strings;

public class KubernetesClusterScaleWorker extends KubernetesClusterResourceModifierActionWorker {

    @Inject
    protected VMInstanceDao vmInstanceDao;
    @Inject
    protected UserVmManager userVmManager;

    private ServiceOffering serviceOffering;
    private Long clusterSize;

    public KubernetesClusterScaleWorker(final KubernetesCluster kubernetesCluster,
                                        final ServiceOffering serviceOffering,
                                        final Long clusterSize,
                                        final KubernetesClusterManagerImpl clusterManager) {
        super(kubernetesCluster, clusterManager);
        this.serviceOffering = serviceOffering;
        this.clusterSize = clusterSize;
    }

    private FirewallRule removeSshFirewallRule(IpAddress publicIp) {
        FirewallRule rule = null;
        List<FirewallRuleVO> firewallRules = firewallRulesDao.listByIpAndPurposeAndNotRevoked(publicIp.getId(), FirewallRule.Purpose.Firewall);
        for (FirewallRuleVO firewallRule : firewallRules) {
            if (firewallRule.getSourcePortStart() == CLUSTER_NODES_DEFAULT_START_SSH_PORT) {
                rule = firewallRule;
                firewallService.revokeIngressFwRule(firewallRule.getId(), true);
                break;
            }
        }
        return rule;
    }

    private void removePortForwardingRules(IpAddress publicIp, Network network, Account account, List<Long> removedVMIds) throws ResourceUnavailableException {
        if (!CollectionUtils.isEmpty(removedVMIds)) {
            for (Long vmId : removedVMIds) {
                List<PortForwardingRuleVO> pfRules = portForwardingRulesDao.listByNetwork(network.getId());
                for (PortForwardingRuleVO pfRule : pfRules) {
                    if (pfRule.getVirtualMachineId() == vmId) {
                        portForwardingRulesDao.remove(pfRule.getId());
                        break;
                    }
                }
            }
            rulesService.applyPortForwardingRules(publicIp.getId(), account);
        }
    }

    /**
     * Scale network rules for an existing Kubernetes cluster while scaling it
     * Open up firewall for SSH access from port NODES_DEFAULT_START_SSH_PORT to NODES_DEFAULT_START_SSH_PORT+n.
     * Also remove port forwarding rules for removed virtual machines and create port-forwarding rule
     * to forward public IP traffic to all node VMs' private IP.
     * @param network
     * @param account
     * @param clusterVMIds
     * @param removedVMIds
     * @throws ManagementServerException
     */
    private void scaleKubernetesClusterNetworkRules(Network network, Account account,
                                                    List<Long> clusterVMIds, List<Long> removedVMIds) throws ManagementServerException {
        if (!Network.GuestType.Isolated.equals(network.getGuestType())) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Network ID: %s for Kubernetes cluster ID: %s is not an isolated network, therefore, no need for network rules", network.getUuid(), kubernetesCluster.getUuid()));
            }
            return;
        }
        IpAddress publicIp = getSourceNatIp(network);
        if (publicIp == null) {
            throw new ManagementServerException(String.format("No source NAT IP addresses found for network ID: %s, Kubernetes cluster ID: %s", network.getUuid(), kubernetesCluster.getUuid()));
        }

        // Remove existing SSH firewall rules
        FirewallRule firewallRule = removeSshFirewallRule(publicIp);
        if (firewallRule == null) {
            throw new ManagementServerException("Firewall rule for node SSH access can't be provisioned!");
        }
        int existingFirewallRuleSourcePortEnd = firewallRule.getSourcePortEnd();

        // Provision new SSH firewall rules
        try {
            provisionFirewallRules(publicIp, account, CLUSTER_NODES_DEFAULT_START_SSH_PORT, CLUSTER_NODES_DEFAULT_START_SSH_PORT + (int)kubernetesCluster.getTotalNodeCount() - 1);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Provisioned  firewall rule to open up port %d to %d on %s in Kubernetes cluster ID: %s",
                        CLUSTER_NODES_DEFAULT_START_SSH_PORT, CLUSTER_NODES_DEFAULT_START_SSH_PORT + (int) kubernetesCluster.getTotalNodeCount() - 1, publicIp.getAddress().addr(), kubernetesCluster.getUuid()));
            }
        } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException e) {
            throw new ManagementServerException(String.format("Failed to activate SSH firewall rules for the Kubernetes cluster ID: %s", kubernetesCluster.getUuid()), e);
        }

        try {
            removePortForwardingRules(publicIp, network, account, removedVMIds);
        } catch (ResourceUnavailableException e) {
            throw new ManagementServerException(String.format("Failed to remove SSH port forwarding rules for removed VMs for the Kubernetes cluster ID: %s", kubernetesCluster.getUuid()), e);
        }

        try {
            provisionSshPortForwardingRules(publicIp, network, account, clusterVMIds, existingFirewallRuleSourcePortEnd + 1);
        } catch (ResourceUnavailableException | NetworkRuleConflictException e) {
            throw new ManagementServerException(String.format("Failed to activate SSH port forwarding rules for the Kubernetes cluster ID: %s", kubernetesCluster.getUuid()), e);
        }
    }

    private KubernetesClusterVO updateKubernetesClusterEntry(final long kubernetesClusterId, final long clusterSize,
                                                             final long cores, final long memory, final Long serviceOfferingId) {
        return Transaction.execute(new TransactionCallback<KubernetesClusterVO>() {
            @Override
            public KubernetesClusterVO doInTransaction(TransactionStatus status) {
                KubernetesClusterVO updatedCluster = kubernetesClusterDao.createForUpdate(kubernetesClusterId);
                updatedCluster.setNodeCount(clusterSize);
                updatedCluster.setCores(cores);
                updatedCluster.setMemory(memory);
                if (serviceOfferingId != null) {
                    updatedCluster.setServiceOfferingId(serviceOfferingId);
                }
                kubernetesClusterDao.persist(updatedCluster);
                return updatedCluster;
            }
        });
    }

    private boolean removeKubernetesClusterNode(String ipAddress, int port, UserVm userVm, int retries, int waitDuration) {
        File pkFile = getManagementServerSshPublicKeyFile();
        int retryCounter = 0;
        String hostName = userVm.getHostName();
        if (!Strings.isNullOrEmpty(hostName)) {
            hostName = hostName.toLowerCase();
        }
        while (retryCounter < retries) {
            retryCounter++;
            try {
                Pair<Boolean, String> result = SshHelper.sshExecute(ipAddress, port, CLUSTER_NODE_VM_USER,
                        pkFile, null, String.format("sudo kubectl drain %s --ignore-daemonsets --delete-local-data", hostName),
                        10000, 10000, 60000);
                if (!result.first()) {
                    LOGGER.warn(String.format("Draining node: %s on VM ID: %s in Kubernetes cluster ID: %s unsuccessful", hostName, userVm.getUuid(), kubernetesCluster.getUuid()));
                } else {
                    result = SshHelper.sshExecute(ipAddress, port, CLUSTER_NODE_VM_USER,
                            pkFile, null, String.format("sudo kubectl delete node %s", hostName),
                            10000, 10000, 30000);
                    if (result.first()) {
                        return true;
                    } else {
                        LOGGER.warn(String.format("Deleting node: %s on VM ID: %s in Kubernetes cluster ID: %s unsuccessful", hostName, userVm.getUuid(), kubernetesCluster.getUuid()));
                    }
                }
                break;
            } catch (Exception e) {
                String msg = String.format("Failed to remove Kubernetes cluster ID: %s node: %s on VM ID: %s", kubernetesCluster.getUuid(), hostName, userVm.getUuid());
                LOGGER.warn(msg, e);
            }
            try {
                Thread.sleep(waitDuration);
            } catch (InterruptedException ie) {
                LOGGER.error(String.format("Error while waiting for Kubernetes cluster ID: %s node: %s on VM ID: %s removal", kubernetesCluster.getUuid(), hostName, userVm.getUuid()), ie);
            }
            retryCounter++;
        }
        return false;
    }

    private void scaleKubernetesClusterOffering(final long kubernetesClusterId, final ServiceOffering serviceOffering, final Long clusterSize) {
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);

        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.ScaleUpRequested);

        final long size = (clusterSize == null ? kubernetesCluster.getTotalNodeCount() : kubernetesCluster.getMasterNodeCount() + clusterSize);
        final long cores = serviceOffering.getCpu() * size;
        final long memory = serviceOffering.getRamSize() * size;
        KubernetesClusterVO updatedKubernetesCluster = updateKubernetesClusterEntry(kubernetesCluster.getId(), size, cores, memory, serviceOffering.getId());
        if (updatedKubernetesCluster == null) {
            logTransitStateAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster ID: %s failed, unable to update Kubernetes cluster!", updatedKubernetesCluster.getUuid()), kubernetesClusterId, KubernetesCluster.Event.OperationFailed);
        }
        kubernetesCluster = updatedKubernetesCluster;
        List<KubernetesClusterVmMapVO> vmList = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());
        final long tobeScaledVMCount = Math.min(vmList.size(), size);
        for (long i = 0; i < tobeScaledVMCount; i++) {
            KubernetesClusterVmMapVO vmMapVO = vmList.get((int) i);
            UserVmVO userVM = userVmDao.findById(vmMapVO.getVmId());
            boolean result = false;
            try {
                result = userVmManager.upgradeVirtualMachine(userVM.getId(), serviceOffering.getId(), new HashMap<String, String>());
            } catch (ResourceUnavailableException | ManagementServerException | ConcurrentOperationException | VirtualMachineMigrationException e) {
                logTransitStateAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster ID: %s failed, unable to scale cluster VM ID: %s", kubernetesCluster.getUuid(), userVM.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed, e);
            }
            if (!result) {
                logTransitStateAndThrow(Level.WARN, String.format("Scaling Kubernetes cluster ID: %s failed, unable to scale cluster VM ID: %s", kubernetesCluster.getUuid(), userVM.getUuid()),kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            }
        }
    }

    private void scaleDownKubernetesClusterSize(final List<KubernetesClusterVmMapVO> originalVmList, final Network network) throws CloudRuntimeException {
        int i = originalVmList.size() - 1;
        List<Long> removedVmIds = new ArrayList<>();
        while (i > kubernetesCluster.getMasterNodeCount() && originalVmList.size() > kubernetesCluster.getTotalNodeCount()) { // Reverse order as first VM will be k8s master
            KubernetesClusterVmMapVO vmMapVO = originalVmList.get(i);
            UserVm userVM = userVmDao.findById(vmMapVO.getId());
            if (!removeKubernetesClusterNode(publicIpAddress, sshPort, userVM, 3, 30000)) {
                logTransitStateAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes cluster ID: %s, failed to remove Kubernetes node: %s running on VM ID: %s", kubernetesCluster.getUuid(), userVM.getHostName(), userVM.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            }
            // For removing port-forwarding network rules
            removedVmIds.add(userVM.getId());
            try {
                UserVm vm = userVmService.destroyVm(userVM.getId(), true);
                if (!VirtualMachine.State.Expunging.equals(vm.getState())) {
                    logTransitStateAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster ID: %s failed, VM '%s' is now in state '%s'."
                            , kubernetesCluster.getUuid()
                            , vm.getInstanceName()
                            , vm.getState().toString()),
                            kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
                }
                vm = userVmService.expungeVm(userVM.getId());
                if (!VirtualMachine.State.Expunging.equals(vm.getState())) {
                    logTransitStateAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster ID: %s failed, VM '%s' is now in state '%s'."
                            , kubernetesCluster.getUuid()
                            , vm.getInstanceName()
                            , vm.getState().toString()),
                            kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
                }
            } catch (ResourceUnavailableException e) {
                logTransitStateAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster ID: %s failed, unable to remove VM ID: %s"
                        , kubernetesCluster.getUuid() , userVM.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed, e);
            }
            kubernetesClusterVmMapDao.expunge(vmMapVO.getId());
            i--;
        }
        // Scale network rules to update firewall rule
        try {
            scaleKubernetesClusterNetworkRules(network, owner, null, removedVmIds);
        } catch (ManagementServerException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes cluster ID: %s, unable to update network rules", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed, e);
        }
    }

    private void scaleUpKubernetesClusterSize(final List<KubernetesClusterVmMapVO> originalVmList, final long newVmCount, final Network network) throws CloudRuntimeException {
        List<UserVm> clusterVMs = new ArrayList<>();
        List<Long> clusterVMIds = new ArrayList<>();
        try {
            clusterVMs = provisionKubernetesClusterNodeVms((int) newVmCount + originalVmList.size(), originalVmList.size(), publicIpAddress);
        } catch (ManagementServerException | ResourceUnavailableException | InsufficientCapacityException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes cluster ID: %s, unable to provision node VM in the cluster", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed, e);
        }
        for (UserVm vm : clusterVMs) {
            clusterVMIds.add(vm.getId());
        }
        try {
            scaleKubernetesClusterNetworkRules(network, owner, clusterVMIds, null);
        } catch (ManagementServerException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes cluster ID: %s, unable to update network rules", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed, e);
        }
        attachIsoKubernetesVMs(clusterVMs);
        boolean readyNodesCountValid = KubernetesClusterUtil.validateKubernetesClusterReadyNodesCount(kubernetesCluster, publicIpAddress, sshPort,
                CLUSTER_NODE_VM_USER, sshKeyFile, 30, 30000);
        detachIsoKubernetesVMs(clusterVMs);
        if (!readyNodesCountValid) { // Scaling failed
            logTransitStateAndThrow(Level.ERROR, String.format("Scaling unsuccessful for Kubernetes cluster ID: %s as it does not have desired number of nodes in ready state", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
        }
    }

    private void scaleKubernetesClusterSize(final long originalClusterSize) throws CloudRuntimeException {
        KubernetesClusterVO kubernetesClusterVO = kubernetesClusterDao.findById(kubernetesCluster.getId());
        final Network network = networkDao.findById(kubernetesClusterVO.getNetworkId());
        final long newVmRequiredCount = clusterSize - originalClusterSize;
        List<KubernetesClusterVmMapVO> vmList = kubernetesClusterVmMapDao.listByClusterId(kubernetesClusterVO.getId());
        if (CollectionUtils.isEmpty(vmList) || vmList.size() - 1 < originalClusterSize) {
            logTransitStateAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes cluster ID: %s, t is in unstable state as not enough existing VM instances found", kubernetesClusterVO.getUuid()), kubernetesClusterVO.getId(), KubernetesCluster.Event.OperationFailed);
        }

        Pair<String, Integer> publicIpSshPort = getKubernetesClusterServerIpSshPort(null);
        String publicIpAddress = publicIpSshPort.first();
        int sshPort = publicIpSshPort.second();
        if (Strings.isNullOrEmpty(publicIpAddress)) {
            logTransitStateAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes cluster ID: %s, unable to retrieve associated public IP", kubernetesClusterVO.getUuid()), kubernetesClusterVO.getId(), KubernetesCluster.Event.OperationFailed);
        }
        Account account = accountDao.findById(kubernetesClusterVO.getAccountId());
        if (newVmRequiredCount < 0) { // downscale
            scaleDownKubernetesClusterSize(vmList, network);
        } else { // upscale, same node count handled above
            scaleUpKubernetesClusterSize(vmList, newVmRequiredCount, network);
        }
    }

    private void validateKubernetesClusterScaleOfferingParameters(final ServiceOffering existingServiceOffering, final ServiceOffering serviceOffering) throws CloudRuntimeException {
        final long originalNodeCount = kubernetesCluster.getTotalNodeCount();
        List<KubernetesClusterVmMapVO> vmList = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());
        if (vmList == null || vmList.isEmpty() || vmList.size() < originalNodeCount) {
            logAndThrow(Level.WARN, String.format("Scaling Kubernetes cluster ID: %s failed, it is in unstable state as not enough existing VM instances found!", kubernetesCluster.getUuid()));
        } else {
            for (KubernetesClusterVmMapVO vmMapVO : vmList) {
                VMInstanceVO vmInstance = vmInstanceDao.findById(vmMapVO.getVmId());
                if (vmInstance != null && vmInstance.getState().equals(VirtualMachine.State.Running) &&
                        vmInstance.getHypervisorType() != Hypervisor.HypervisorType.XenServer &&
                        vmInstance.getHypervisorType() != Hypervisor.HypervisorType.VMware &&
                        vmInstance.getHypervisorType() != Hypervisor.HypervisorType.Simulator) {
                    logAndThrow(Level.WARN, String.format("Scaling Kubernetes cluster ID: %s failed, scaling Kubernetes cluster with running VMs on hypervisor %s is not supported!", kubernetesCluster.getUuid(), vmInstance.getHypervisorType()));
                }
            }
        }
        if (serviceOffering.getRamSize() < existingServiceOffering.getRamSize() ||
                serviceOffering.getCpu() * serviceOffering.getSpeed() < existingServiceOffering.getCpu() * existingServiceOffering.getSpeed()) {
            logAndThrow(Level.WARN, String.format("Scaling Kubernetes cluster ID: %s failed, service offering for the Kubernetes cluster cannot be scaled down!", kubernetesCluster.getUuid()));
        }
    }

    private void validateKubernetesClusterScaleSizeParameters(final long originalClusterSize, final long clusterSize, final KubernetesCluster.State clusterState) throws CloudRuntimeException {
        Network network = networkDao.findById(kubernetesCluster.getNetworkId());
        if (network == null) {
            String msg = String.format("Scaling failed for Kubernetes cluster ID: %s, cluster network not found", kubernetesCluster.getUuid());
            if (KubernetesCluster.State.Scaling.equals(kubernetesCluster.getState())) {
                logTransitStateAndThrow(Level.WARN, msg, kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            } else {
                logAndThrow(Level.WARN, msg);
            }
        }
        // Check capacity and transition state
        final long newVmRequiredCount = clusterSize - originalClusterSize;
        final ServiceOffering clusterServiceOffering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
        if (clusterServiceOffering == null) {
            String msg = String.format("Scaling failed for Kubernetes cluster ID: %s, cluster service offering not found", kubernetesCluster.getUuid());
            if (KubernetesCluster.State.Scaling.equals(kubernetesCluster.getState())) {
                logTransitStateAndThrow(Level.WARN, msg, kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
            } else {
                logAndThrow(Level.WARN, msg);
            }
        }
        if (newVmRequiredCount > 0) {
            final DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
            try {
                if (clusterState.equals(KubernetesCluster.State.Running)) {
                    plan(newVmRequiredCount, zone, clusterServiceOffering);
                } else {
                    plan(kubernetesCluster.getTotalNodeCount() + newVmRequiredCount, zone, clusterServiceOffering);
                }
            } catch (InsufficientCapacityException e) {
                String msg = String.format("Scaling failed for Kubernetes cluster ID: %s in zone ID: %s, insufficient capacity", kubernetesCluster.getUuid(), zone.getUuid());
                if (KubernetesCluster.State.Scaling.equals(kubernetesCluster.getState())) {
                    logTransitStateAndThrow(Level.WARN, msg, kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
                } else {
                    logAndThrow(Level.WARN, msg);
                }
            }
        }
    }

    public boolean scaleCluster() throws CloudRuntimeException {
        init();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Scaling Kubernetes cluster ID: %s", kubernetesCluster.getUuid()));
        }

        final KubernetesCluster.State clusterState = kubernetesCluster.getState();
        final long originalClusterSize = kubernetesCluster.getNodeCount();

        final ServiceOffering existingServiceOffering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
        if (existingServiceOffering == null) {
            logAndThrow(Level.ERROR, String.format("Scaling Kubernetes cluster ID: %s failed, service offering for the Kubernetes cluster not found!", kubernetesCluster.getUuid()));
        }
        final boolean serviceOfferingScalingNeeded = serviceOffering != null && serviceOffering.getId() != existingServiceOffering.getId();
        final boolean clusterSizeScalingNeeded = clusterSize != null && clusterSize != originalClusterSize;

        if (serviceOfferingScalingNeeded) {
            validateKubernetesClusterScaleOfferingParameters(existingServiceOffering, serviceOffering);
            scaleKubernetesClusterOffering(kubernetesCluster.getId(), serviceOffering, clusterSize);
        }

        if (clusterSizeScalingNeeded) {
            validateKubernetesClusterScaleSizeParameters(originalClusterSize, clusterSize, clusterState);
            final long newVmRequiredCount = clusterSize - originalClusterSize;
            final ServiceOffering clusterServiceOffering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
            if (newVmRequiredCount > 0) {
                if (!kubernetesCluster.getState().equals(KubernetesCluster.State.Scaling)) {
                    stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.ScaleUpRequested);
                }
            } else {
                if (!kubernetesCluster.getState().equals(KubernetesCluster.State.Scaling)) {
                    stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.ScaleDownRequested);
                }
            }

            if (!serviceOfferingScalingNeeded) { // Else already updated
                final long cores = clusterServiceOffering.getCpu() * (kubernetesCluster.getMasterNodeCount() + clusterSize);
                final long memory = clusterServiceOffering.getRamSize() * (kubernetesCluster.getMasterNodeCount() + clusterSize);

                if (updateKubernetesClusterEntry(kubernetesCluster.getId(), clusterSize, cores, memory, null) == null) {
                    logTransitStateAndThrow(Level.ERROR, String.format("Scaling failed for Kubernetes cluster ID: %s, unable to update cluster", kubernetesCluster.getUuid()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
                }
            }

            // Perform size scaling
            scaleKubernetesClusterSize(originalClusterSize);
        }
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
        return true;
    }
}
