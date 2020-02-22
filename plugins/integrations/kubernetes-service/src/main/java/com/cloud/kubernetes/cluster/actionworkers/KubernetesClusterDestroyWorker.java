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
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.context.CallContext;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Level;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterDetailsVO;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMap;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.rules.FirewallRule;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.uservm.UserVm;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.UserVmVO;

public class KubernetesClusterDestroyWorker extends KubernetesClusterResourceModifierActionWorker {

    @Inject
    protected AccountManager accountManager;

    private List<KubernetesClusterVmMapVO> clusterVMs;

    public KubernetesClusterDestroyWorker(final KubernetesCluster kubernetesCluster, final KubernetesClusterManagerImpl clusterManager) {
        super(kubernetesCluster, clusterManager);
    }

    private void validateClusterSate() {
        if (!(kubernetesCluster.getState().equals(KubernetesCluster.State.Running)
                || kubernetesCluster.getState().equals(KubernetesCluster.State.Stopped)
                || kubernetesCluster.getState().equals(KubernetesCluster.State.Alert)
                || kubernetesCluster.getState().equals(KubernetesCluster.State.Error)
                || kubernetesCluster.getState().equals(KubernetesCluster.State.Destroying))) {
            String msg = String.format("Cannot perform delete operation on cluster ID: %s in state: %s",kubernetesCluster.getUuid(), kubernetesCluster.getState());
            LOGGER.warn(msg);
            throw new PermissionDeniedException(msg);
        }
    }

    private boolean destroyClusterVMs() {
        boolean vmDestroyed = true;
        if (!CollectionUtils.isEmpty(clusterVMs)) {
            for (KubernetesClusterVmMapVO clusterVM : clusterVMs) {
                long vmID = clusterVM.getVmId();

                // delete only if VM exists and is not removed
                UserVmVO userVM = userVmDao.findById(vmID);
                if (userVM == null || userVM.isRemoved()) {
                    continue;
                }
                try {
                    UserVm vm = userVmService.destroyVm(vmID, true);
                    if (!userVmManager.expunge(userVM, CallContext.current().getCallingUserId(), CallContext.current().getCallingAccount())) {
                        LOGGER.warn(String.format("Unable to expunge VM '%s' ID: %s, destroying Kubernetes cluster will probably fail"
                                , vm.getInstanceName()
                                , vm.getUuid()));
                    }
                    kubernetesClusterVmMapDao.expunge(clusterVM.getId());
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("Destroyed VM ID: %s as part of Kubernetes cluster ID: %s cleanup", vm.getUuid(), kubernetesCluster.getUuid()));
                    }
                } catch (ResourceUnavailableException | ConcurrentOperationException e) {
                    LOGGER.warn(String.format("Failed to destroy VM ID: %s part of the Kubernetes cluster ID: %s cleanup. Moving on with destroying remaining resources provisioned for the Kubernetes cluster", userVM.getUuid(), kubernetesCluster.getUuid()), e);
                    return false;
                }
            }
        }
        return vmDestroyed;
    }

    private boolean updateKubernetesClusterEntryForGC() {
        KubernetesClusterVO kubernetesClusterVO = kubernetesClusterDao.findById(kubernetesCluster.getId());
        kubernetesClusterVO.setCheckForGc(true);
        return kubernetesClusterDao.update(kubernetesCluster.getId(), kubernetesClusterVO);
    }

    private void destroyKubernetesClusterNetwork() throws ManagementServerException {
        NetworkVO network = networkDao.findById(kubernetesCluster.getNetworkId());
        if (network != null && network.getRemoved() == null) {
            Account owner = accountManager.getAccount(network.getAccountId());
            User callerUser = accountManager.getActiveUser(CallContext.current().getCallingUserId());
            ReservationContext context = new ReservationContextImpl(null, null, callerUser, owner);
            boolean networkDestroyed = networkMgr.destroyNetwork(kubernetesCluster.getNetworkId(), context, true);
            if (!networkDestroyed) {
                String msg = String.format("Failed to destroy network ID: %s as part of Kubernetes cluster ID: %s cleanup", network.getUuid(), kubernetesCluster.getUuid());
                LOGGER.warn(msg);
                throw new ManagementServerException(msg);
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Destroyed network: %s as part of Kubernetes cluster ID: %s cleanup", network.getUuid(), kubernetesCluster.getUuid()));
            }
        }
    }

    private void deleteKubernetesClusterNetworkRules() throws ManagementServerException {
        NetworkVO network = networkDao.findById(kubernetesCluster.getNetworkId());
        if (network == null || !Network.GuestType.Isolated.equals(network.getGuestType())) {
            return;
        }
        List<Long> removedVmIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(clusterVMs)) {
            for (KubernetesClusterVmMapVO clusterVM : clusterVMs) {
                removedVmIds.add(clusterVM.getVmId());
            }
        }
        IpAddress publicIp = getSourceNatIp(network);
        if (publicIp == null) {
            throw new ManagementServerException(String.format("No source NAT IP addresses found for network ID: %s", network.getUuid()));
        }
        try {
            removeLoadBalancingRule(publicIp, network, owner, CLUSTER_API_PORT);
        } catch (ResourceUnavailableException e) {
            throw new ManagementServerException(String.format("Failed to KubernetesCluster load balancing rule for network ID: %s", network.getUuid()));
        }
        FirewallRule firewallRule = removeApiFirewallRule(publicIp);
        if (firewallRule == null) {
            logMessage(Level.WARN, "Firewall rule for API access can't be removed", null);
        }
        firewallRule = removeSshFirewallRule(publicIp);
        if (firewallRule == null) {
            logMessage(Level.WARN, "Firewall rule for SSH access can't be removed", null);
        }
        try {
            removePortForwardingRules(publicIp, network, owner, removedVmIds);
        } catch (ResourceUnavailableException e) {
            throw new ManagementServerException(String.format("Failed to KubernetesCluster port forwarding rules for network ID: %s", network.getUuid()));
        }
    }

    private void validateClusterVMsDestroyed() {
        if(clusterVMs!=null  && !clusterVMs.isEmpty()) { // Wait for few seconds to get all VMs really expunged
            final int maxRetries = 3;
            int retryCounter = 0;
            while (retryCounter < maxRetries) {
                boolean allVMsRemoved = true;
                for (KubernetesClusterVmMap clusterVM : clusterVMs) {
                    UserVmVO userVM = userVmDao.findById(clusterVM.getVmId());
                    if (userVM != null && !userVM.isRemoved()) {
                        allVMsRemoved = false;
                        break;
                    }
                }
                if (allVMsRemoved) {
                    break;
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ie) {}
                retryCounter++;
            }
        }
    }

    public boolean destroy() throws CloudRuntimeException {
        init();
        validateClusterSate();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Destroying Kubernetes cluster ID: %s", kubernetesCluster.getUuid()));
        }
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.DestroyRequested);
        this.clusterVMs = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());
        boolean vmsDestroyed = destroyClusterVMs();
        boolean cleanupNetwork = true;
        final KubernetesClusterDetailsVO clusterDetails = kubernetesClusterDetailsDao.findDetail(kubernetesCluster.getId(), "networkCleanup");
        if (clusterDetails != null) {
            cleanupNetwork = Boolean.parseBoolean(clusterDetails.getValue());
        }
        // if there are VM's that were not expunged, we can not delete the network
        if (vmsDestroyed) {
            if (cleanupNetwork) {
                validateClusterVMsDestroyed();
                try {
                    destroyKubernetesClusterNetwork();
                } catch (ManagementServerException e) {
                    String msg = String.format("Failed to destroy network of Kubernetes cluster ID: %s cleanup", kubernetesCluster.getUuid());
                    LOGGER.warn(msg, e);
                    updateKubernetesClusterEntryForGC();
                    throw new CloudRuntimeException(msg, e);
                }
            } else {
                try {
                    deleteKubernetesClusterNetworkRules();
                } catch (ManagementServerException e) {
                    String msg = String.format("Failed to remove network rules of Kubernetes cluster ID: %s", kubernetesCluster.getUuid());
                    LOGGER.warn(msg, e);
                    updateKubernetesClusterEntryForGC();
                    throw new CloudRuntimeException(msg, e);
                }
            }
        } else {
            String msg = String.format("Failed to destroy one or more VMs as part of Kubernetes cluster ID: %s cleanup", kubernetesCluster.getUuid());
            LOGGER.warn(msg);
            updateKubernetesClusterEntryForGC();
            throw new CloudRuntimeException(msg);
        }
        stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
        boolean deleted = kubernetesClusterDao.remove(kubernetesCluster.getId());
        if (!deleted) {
            logMessage(Level.WARN, String.format("Failed to delete Kubernetes cluster ID: %s", kubernetesCluster.getUuid()), null);
            updateKubernetesClusterEntryForGC();
            return false;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Kubernetes cluster ID: %s is successfully deleted", kubernetesCluster.getUuid()));
        }
        return true;
    }
}
