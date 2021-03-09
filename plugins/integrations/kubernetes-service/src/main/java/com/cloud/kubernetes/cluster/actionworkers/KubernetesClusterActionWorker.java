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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.ca.CAManager;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterDetailsVO;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.KubernetesSupportedVersion;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkDao;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.template.TemplateApiService;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.UserVmService;
import com.cloud.vm.dao.UserVmDao;
import com.google.common.base.Strings;

public class KubernetesClusterActionWorker {

    public static final String CLUSTER_NODE_VM_USER = "core";
    public static final int CLUSTER_API_PORT = 6443;
    public static final int CLUSTER_NODES_DEFAULT_START_SSH_PORT = 2222;

    protected static final Logger LOGGER = Logger.getLogger(KubernetesClusterActionWorker.class);

    protected StateMachine2<KubernetesCluster.State, KubernetesCluster.Event, KubernetesCluster> _stateMachine = KubernetesCluster.State.getStateMachine();

    @Inject
    protected CAManager caManager;
    @Inject
    protected ConfigurationDao configurationDao;
    @Inject
    protected DataCenterDao dataCenterDao;
    @Inject
    protected AccountDao accountDao;
    @Inject
    protected IpAddressManager ipAddressManager;
    @Inject
    protected NetworkOrchestrationService networkMgr;
    @Inject
    protected NetworkDao networkDao;
    @Inject
    protected NetworkModel networkModel;
    @Inject
    protected ServiceOfferingDao serviceOfferingDao;
    @Inject
    protected SSHKeyPairDao sshKeyPairDao;
    @Inject
    protected VMTemplateDao templateDao;
    @Inject
    protected TemplateApiService templateService;
    @Inject
    protected UserVmDao userVmDao;
    @Inject
    protected UserVmService userVmService;
    @Inject
    protected VlanDao vlanDao;

    protected KubernetesClusterDao kubernetesClusterDao;
    protected KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    protected KubernetesClusterDetailsDao kubernetesClusterDetailsDao;
    protected KubernetesSupportedVersionDao kubernetesSupportedVersionDao;

    protected KubernetesCluster kubernetesCluster;
    protected Account owner;
    protected VirtualMachineTemplate clusterTemplate;
    protected File sshKeyFile;
    protected String publicIpAddress;
    protected int sshPort;

    protected KubernetesClusterActionWorker(final KubernetesCluster kubernetesCluster, final KubernetesClusterManagerImpl clusterManager) {
        this.kubernetesCluster = kubernetesCluster;
        this.kubernetesClusterDao = clusterManager.kubernetesClusterDao;
        this.kubernetesClusterDetailsDao = clusterManager.kubernetesClusterDetailsDao;
        this.kubernetesClusterVmMapDao = clusterManager.kubernetesClusterVmMapDao;
        this.kubernetesSupportedVersionDao = clusterManager.kubernetesSupportedVersionDao;
    }

    protected void init() {
        this.owner = accountDao.findById(kubernetesCluster.getAccountId());
        this.clusterTemplate = templateDao.findById(kubernetesCluster.getTemplateId());
        this.sshKeyFile = getManagementServerSshPublicKeyFile();
    }

    protected String readResourceFile(String resource) throws IOException {
        return IOUtils.toString(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)), StringUtils.getPreferredCharset());
    }

    protected void logMessage(final Level logLevel, final String message, final Exception e) {
        if (logLevel == Level.INFO) {
            if (LOGGER.isInfoEnabled()) {
                if (e != null) {
                    LOGGER.info(message, e);
                } else {
                    LOGGER.info(message);
                }
            }
        } else if (logLevel == Level.DEBUG) {
            if (LOGGER.isDebugEnabled()) {
                if (e != null) {
                    LOGGER.debug(message, e);
                } else {
                    LOGGER.debug(message);
                }
            }
        } else if (logLevel == Level.WARN) {
            if (e != null) {
                LOGGER.warn(message, e);
            } else {
                LOGGER.warn(message);
            }
        } else {
            if (e != null) {
                LOGGER.error(message, e);
            } else {
                LOGGER.error(message);
            }
        }
    }

    protected void logTransitStateDetachIsoAndThrow(final Level logLevel, final String message, final KubernetesCluster kubernetesCluster,
                                                    final List<UserVm> clusterVMs, final KubernetesCluster.Event event, final Exception e) throws CloudRuntimeException {
        logMessage(logLevel, message, e);
        stateTransitTo(kubernetesCluster.getId(), event);
        detachIsoKubernetesVMs(clusterVMs);
        if (e == null) {
            throw new CloudRuntimeException(message);
        }
        throw new CloudRuntimeException(message, e);
    }

    protected void logTransitStateAndThrow(final Level logLevel, final String message, final Long kubernetesClusterId, final KubernetesCluster.Event event, final Exception e) throws CloudRuntimeException {
        logMessage(logLevel, message, e);
        if (kubernetesClusterId != null && event != null) {
            stateTransitTo(kubernetesClusterId, event);
        }
        if (e == null) {
            throw new CloudRuntimeException(message);
        }
        throw new CloudRuntimeException(message, e);
    }

    protected void logTransitStateAndThrow(final Level logLevel, final String message, final Long kubernetesClusterId, final KubernetesCluster.Event event) throws CloudRuntimeException {
        logTransitStateAndThrow(logLevel, message, kubernetesClusterId, event, null);
    }

    protected void logAndThrow(final Level logLevel, final String message) throws CloudRuntimeException {
        logTransitStateAndThrow(logLevel, message, null, null, null);
    }

    protected void logAndThrow(final Level logLevel, final String message, final Exception ex) throws CloudRuntimeException {
        logTransitStateAndThrow(logLevel, message, null, null, ex);
    }

    protected File getManagementServerSshPublicKeyFile() {
        boolean devel = Boolean.parseBoolean(configurationDao.getValue("developer"));
        String keyFile = String.format("%s/.ssh/id_rsa", System.getProperty("user.home"));
        if (devel) {
            keyFile += ".cloud";
        }
        return new File(keyFile);
    }

    protected KubernetesClusterVmMapVO addKubernetesClusterVm(final long kubernetesClusterId, final long vmId) {
        return Transaction.execute(new TransactionCallback<KubernetesClusterVmMapVO>() {
            @Override
            public KubernetesClusterVmMapVO doInTransaction(TransactionStatus status) {
                KubernetesClusterVmMapVO newClusterVmMap = new KubernetesClusterVmMapVO(kubernetesClusterId, vmId);
                kubernetesClusterVmMapDao.persist(newClusterVmMap);
                return newClusterVmMap;
            }
        });
    }

    private UserVm fetchControlVmIfMissing(final UserVm controlVm) {
        if (controlVm != null) {
            return controlVm;
        }
        List<KubernetesClusterVmMapVO> clusterVMs = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());
        if (CollectionUtils.isEmpty(clusterVMs)) {
            LOGGER.warn(String.format("Unable to retrieve VMs for Kubernetes cluster : %s", kubernetesCluster.getName()));
            return null;
        }
        List<Long> vmIds = new ArrayList<>();
        for (KubernetesClusterVmMapVO vmMap : clusterVMs) {
            vmIds.add(vmMap.getVmId());
        }
        Collections.sort(vmIds);
        return userVmDao.findById(vmIds.get(0));
    }

    protected String getControlVmPrivateIp() {
        String ip = null;
        UserVm vm = fetchControlVmIfMissing(null);
        if (vm != null) {
            ip = vm.getPrivateIpAddress();
        }
        return ip;
    }

    protected Pair<String, Integer> getKubernetesClusterServerIpSshPort(UserVm controlVm) {
        int port = CLUSTER_NODES_DEFAULT_START_SSH_PORT;
        KubernetesClusterDetailsVO detail = kubernetesClusterDetailsDao.findDetail(kubernetesCluster.getId(), ApiConstants.EXTERNAL_LOAD_BALANCER_IP_ADDRESS);
        if (detail != null && !Strings.isNullOrEmpty(detail.getValue())) {
            return new Pair<>(detail.getValue(), port);
        }
        Network network = networkDao.findById(kubernetesCluster.getNetworkId());
        if (network == null) {
            LOGGER.warn(String.format("Network for Kubernetes cluster : %s cannot be found", kubernetesCluster.getName()));
            return new Pair<>(null, port);
        }
        if (Network.GuestType.Isolated.equals(network.getGuestType())) {
            List<? extends IpAddress> addresses = networkModel.listPublicIpsAssignedToGuestNtwk(network.getId(), true);
            if (CollectionUtils.isEmpty(addresses)) {
                LOGGER.warn(String.format("No public IP addresses found for network : %s, Kubernetes cluster : %s", network.getName(), kubernetesCluster.getName()));
                return new Pair<>(null, port);
            }
            for (IpAddress address : addresses) {
                if (address.isSourceNat()) {
                    return new Pair<>(address.getAddress().addr(), port);
                }
            }
            LOGGER.warn(String.format("No source NAT IP addresses found for network : %s, Kubernetes cluster : %s", network.getName(), kubernetesCluster.getName()));
            return new Pair<>(null, port);
        } else if (Network.GuestType.Shared.equals(network.getGuestType())) {
            port = 22;
            controlVm = fetchControlVmIfMissing(controlVm);
            if (controlVm == null) {
                LOGGER.warn(String.format("Unable to retrieve control VM for Kubernetes cluster : %s", kubernetesCluster.getName()));
                return new Pair<>(null, port);
            }
            return new Pair<>(controlVm.getPrivateIpAddress(), port);
        }
        LOGGER.warn(String.format("Unable to retrieve server IP address for Kubernetes cluster : %s", kubernetesCluster.getName()));
        return  new Pair<>(null, port);
    }

    protected void attachIsoKubernetesVMs(List<UserVm> clusterVMs, final KubernetesSupportedVersion kubernetesSupportedVersion) throws CloudRuntimeException {
        KubernetesSupportedVersion version = kubernetesSupportedVersion;
        if (kubernetesSupportedVersion == null) {
            version = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId());
        }
        KubernetesCluster.Event failedEvent = KubernetesCluster.Event.OperationFailed;
        KubernetesCluster cluster = kubernetesClusterDao.findById(kubernetesCluster.getId());
        if (cluster != null && cluster.getState() == KubernetesCluster.State.Starting) {
            failedEvent = KubernetesCluster.Event.CreateFailed;
        }
        if (version == null) {
            logTransitStateAndThrow(Level.ERROR, String .format("Unable to find Kubernetes version for cluster : %s", kubernetesCluster.getName()), kubernetesCluster.getId(), failedEvent);
        }
        VMTemplateVO iso = templateDao.findById(version.getIsoId());
        if (iso == null) {
            logTransitStateAndThrow(Level.ERROR, String.format("Unable to attach ISO to Kubernetes cluster : %s. Binaries ISO not found.",  kubernetesCluster.getName()), kubernetesCluster.getId(), failedEvent);
        }
        if (!iso.getFormat().equals(Storage.ImageFormat.ISO)) {
            logTransitStateAndThrow(Level.ERROR, String.format("Unable to attach ISO to Kubernetes cluster : %s. Invalid Binaries ISO.",  kubernetesCluster.getName()), kubernetesCluster.getId(), failedEvent);
        }
        if (!iso.getState().equals(VirtualMachineTemplate.State.Active)) {
            logTransitStateAndThrow(Level.ERROR, String.format("Unable to attach ISO to Kubernetes cluster : %s. Binaries ISO not active.",  kubernetesCluster.getName()), kubernetesCluster.getId(), failedEvent);
        }
        for (UserVm vm : clusterVMs) {
            try {
                templateService.attachIso(iso.getId(), vm.getId(), true);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format("Attached binaries ISO for VM : %s in cluster: %s", vm.getDisplayName(), kubernetesCluster.getName()));
                }
            } catch (CloudRuntimeException ex) {
                logTransitStateAndThrow(Level.ERROR, String.format("Failed to attach binaries ISO for VM : %s in the Kubernetes cluster name: %s", vm.getDisplayName(), kubernetesCluster.getName()), kubernetesCluster.getId(), failedEvent, ex);
            }
        }
    }

    protected void attachIsoKubernetesVMs(List<UserVm> clusterVMs) throws CloudRuntimeException {
        attachIsoKubernetesVMs(clusterVMs, null);
    }

    protected void detachIsoKubernetesVMs(List<UserVm> clusterVMs) {
        for (UserVm vm : clusterVMs) {
            boolean result = false;
            try {
                result = templateService.detachIso(vm.getId(), true);
            } catch (CloudRuntimeException ex) {
                LOGGER.warn(String.format("Failed to detach binaries ISO from VM : %s in the Kubernetes cluster : %s ", vm.getDisplayName(), kubernetesCluster.getName()), ex);
            }
            if (result) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format("Detached Kubernetes binaries from VM : %s in the Kubernetes cluster : %s", vm.getDisplayName(), kubernetesCluster.getName()));
                }
                continue;
            }
            LOGGER.warn(String.format("Failed to detach binaries ISO from VM : %s in the Kubernetes cluster : %s ", vm.getDisplayName(), kubernetesCluster.getName()));
        }
    }

    protected List<KubernetesClusterVmMapVO> getKubernetesClusterVMMaps() {
        List<KubernetesClusterVmMapVO> clusterVMs = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());
        if (!CollectionUtils.isEmpty(clusterVMs)) {
            clusterVMs.sort((t1, t2) -> (int)((t1.getId() - t2.getId())/Math.abs(t1.getId() - t2.getId())));
        }
        return clusterVMs;
    }

    protected List<UserVm> getKubernetesClusterVMs() {
        List<UserVm> vmList = new ArrayList<>();
        List<KubernetesClusterVmMapVO> clusterVMs = getKubernetesClusterVMMaps();
        if (!CollectionUtils.isEmpty(clusterVMs)) {
            for (KubernetesClusterVmMapVO vmMap : clusterVMs) {
                vmList.add(userVmDao.findById(vmMap.getVmId()));
            }
        }
        return vmList;
    }

    protected boolean stateTransitTo(long kubernetesClusterId, KubernetesCluster.Event e) {
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        try {
            return _stateMachine.transitTo(kubernetesCluster, e, null, kubernetesClusterDao);
        } catch (NoTransitionException nte) {
            LOGGER.warn(String.format("Failed to transition state of the Kubernetes cluster : %s in state %s on event %s",
                kubernetesCluster.getName(), kubernetesCluster.getState().toString(), e.toString()), nte);
            return false;
        }
    }
}
