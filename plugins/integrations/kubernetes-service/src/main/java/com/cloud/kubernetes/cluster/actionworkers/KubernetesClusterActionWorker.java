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
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.kubernetes.cluster.utils.KubernetesClusterUtil;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.RulesService;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.dao.UserDataDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.net.Ip;
import com.cloud.vm.Nic;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.UserVmManager;
import org.apache.cloudstack.affinity.AffinityGroupVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.user.firewall.CreateFirewallRuleCmd;
import org.apache.cloudstack.ca.CAManager;
import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.userdata.UserDataManager;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor;
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
import com.cloud.network.NetworkService;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.vpc.VpcService;
import com.cloud.projects.ProjectService;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.LaunchPermissionDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.template.TemplateApiService;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.UserVmService;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.CONTROL;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.ETCD;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.WORKER;


public class KubernetesClusterActionWorker {

    public static final String CLUSTER_NODE_VM_USER = "cloud";
    public static final int CLUSTER_API_PORT = 6443;
    public static final int DEFAULT_SSH_PORT = 22;
    public static final int CLUSTER_NODES_DEFAULT_START_SSH_PORT = 2222;
    public static final int ETCD_NODE_CLIENT_REQUEST_PORT = 2379;
    public static final int ETCD_NODE_PEER_COMM_PORT = 2380;
    public static final int CLUSTER_NODES_DEFAULT_SSH_PORT_SG = DEFAULT_SSH_PORT;

    public static final String CKS_CLUSTER_SECURITY_GROUP_NAME = "CKSSecurityGroup";
    public static final String CKS_SECURITY_GROUP_DESCRIPTION = "Security group for CKS nodes";
    public static final String CKS_CONFIG_PATH = "/usr/share/cloudstack-management/cks";

    protected Logger logger = LogManager.getLogger(getClass());

    protected final static List<KubernetesClusterNodeType> CLUSTER_NODES_TYPES_LIST = Arrays.asList(WORKER, CONTROL, ETCD);

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
    protected IPAddressDao ipAddressDao;
    @Inject
    protected NetworkOrchestrationService networkMgr;
    @Inject
    protected NetworkDao networkDao;
    @Inject
    protected NetworkModel networkModel;
    @Inject
    protected NetworkService networkService;
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
    protected UserVmDetailsDao userVmDetailsDao;
    @Inject
    protected UserVmService userVmService;
    @Inject
    protected UserDataManager userDataManager;
    @Inject
    protected UserDataDao userDataDao;
    @Inject
    protected UserVmManager userVmManager;
    @Inject
    protected VlanDao vlanDao;
    @Inject
    protected LaunchPermissionDao launchPermissionDao;
    @Inject
    public ProjectService projectService;
    @Inject
    public VpcService vpcService;
    @Inject
    public PortForwardingRulesDao portForwardingRulesDao;
    @Inject
    protected RulesService rulesService;
    @Inject
    protected FirewallService firewallService;
    @Inject
    private NicDao nicDao;
    @Inject
    protected AffinityGroupDao affinityGroupDao;

    protected KubernetesClusterDao kubernetesClusterDao;
    protected KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    protected KubernetesClusterDetailsDao kubernetesClusterDetailsDao;
    protected KubernetesSupportedVersionDao kubernetesSupportedVersionDao;

    protected KubernetesCluster kubernetesCluster;
    protected Account owner;
    protected VirtualMachineTemplate clusterTemplate;
    protected VirtualMachineTemplate controlNodeTemplate;
    protected VirtualMachineTemplate workerNodeTemplate;
    protected VirtualMachineTemplate etcdTemplate;
    protected File sshKeyFile;
    protected String publicIpAddress;
    protected int sshPort;


    protected final String deploySecretsScriptFilename = "deploy-cloudstack-secret";
    protected final String deployProviderScriptFilename = "deploy-provider";
    protected final String autoscaleScriptFilename = "autoscale-kube-cluster";
    protected final String validateNodeScript = "validate-cks-node";
    protected final String removeNodeFromClusterScript = "remove-node-from-cluster";
    protected final String scriptPath = "/opt/bin/";
    protected File deploySecretsScriptFile;
    protected File deployProviderScriptFile;
    protected File autoscaleScriptFile;
    protected KubernetesClusterManagerImpl manager;
    protected String[] keys;

    protected KubernetesClusterActionWorker(final KubernetesCluster kubernetesCluster, final KubernetesClusterManagerImpl clusterManager) {
        this.kubernetesCluster = kubernetesCluster;
        this.kubernetesClusterDao = clusterManager.kubernetesClusterDao;
        this.kubernetesClusterDetailsDao = clusterManager.kubernetesClusterDetailsDao;
        this.kubernetesClusterVmMapDao = clusterManager.kubernetesClusterVmMapDao;
        this.kubernetesSupportedVersionDao = clusterManager.kubernetesSupportedVersionDao;
        this.manager = clusterManager;
    }

    protected void init() {
        this.owner = accountDao.findById(kubernetesCluster.getAccountId());
        long zoneId = this.kubernetesCluster.getZoneId();
        long templateId = this.kubernetesCluster.getTemplateId();
        DataCenterVO dataCenterVO = dataCenterDao.findById(zoneId);
        VMTemplateVO template = templateDao.findById(templateId);
        Hypervisor.HypervisorType type = template.getHypervisorType();
        this.clusterTemplate = manager.getKubernetesServiceTemplate(dataCenterVO, type, null, KubernetesClusterNodeType.DEFAULT);
        this.controlNodeTemplate = templateDao.findById(this.kubernetesCluster.getControlNodeTemplateId());
        this.workerNodeTemplate = templateDao.findById(this.kubernetesCluster.getWorkerNodeTemplateId());
        this.etcdTemplate = templateDao.findById(this.kubernetesCluster.getEtcdNodeTemplateId());
        this.sshKeyFile = getManagementServerSshPublicKeyFile();
    }

    protected String readResourceFile(String resource) throws IOException {
        return IOUtils.toString(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)), com.cloud.utils.StringUtils.getPreferredCharset());
    }

    protected String readK8sConfigFile(String resource) throws IOException {
        Path path = Paths.get(String.format("%s%s", CKS_CONFIG_PATH, resource));
        return Files.readString(path);
    }

    protected String getControlNodeLoginUser() {
        List<KubernetesClusterVmMapVO> vmMapVOList = getKubernetesClusterVMMaps();
        if (!vmMapVOList.isEmpty()) {
            long vmId = vmMapVOList.get(0).getVmId();
            UserVmVO userVM = userVmDao.findById(vmId);
            if (userVM == null) {
                throw new CloudRuntimeException("Failed to find login user, Unable to log in to node to fetch details");
            }
            UserVmDetailVO vmDetail = userVmDetailsDao.findDetail(vmId, VmDetailConstants.CKS_CONTROL_NODE_LOGIN_USER);
            if (vmDetail != null && !org.apache.commons.lang3.StringUtils.isEmpty(vmDetail.getValue())) {
                return vmDetail.getValue();
            } else {
                return CLUSTER_NODE_VM_USER;
            }
        } else {
            return CLUSTER_NODE_VM_USER;
        }
    }

    protected void logMessage(final Level logLevel, final String message, final Exception e) {
        if (logLevel == Level.INFO) {
            if (logger.isInfoEnabled()) {
                if (e != null) {
                    logger.info(message, e);
                } else {
                    logger.info(message);
                }
            }
        } else if (logLevel == Level.DEBUG) {
            if (logger.isDebugEnabled()) {
                if (e != null) {
                    logger.debug(message, e);
                } else {
                    logger.debug(message);
                }
            }
        } else if (logLevel == Level.WARN) {
            if (e != null) {
                logger.warn(message, e);
            } else {
                logger.warn(message);
            }
        } else {
            if (e != null) {
                logger.error(message, e);
            } else {
                logger.error(message);
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

    protected void deleteTemplateLaunchPermission() {
        if (isDefaultTemplateUsed() && owner != null) {
            logger.info("Revoking launch permission for systemVM template");
            launchPermissionDao.removePermissions(clusterTemplate.getId(), Collections.singletonList(owner.getId()));
        }
    }

    protected void logTransitStateAndThrow(final Level logLevel, final String message, final Long kubernetesClusterId, final KubernetesCluster.Event event, final Exception e) throws CloudRuntimeException {
        logMessage(logLevel, message, e);
        if (kubernetesClusterId != null && event != null) {
            stateTransitTo(kubernetesClusterId, event);
        }
        deleteTemplateLaunchPermission();
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

    protected KubernetesClusterVmMapVO addKubernetesClusterVm(final long kubernetesClusterId, final long vmId,
                                                              boolean isControlNode, boolean isExternalNode,
                                                              boolean isEtcdNode,  boolean markForManualUpgrade) {
        KubernetesSupportedVersion kubernetesVersion = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId());
        return Transaction.execute(new TransactionCallback<KubernetesClusterVmMapVO>() {
            @Override
            public KubernetesClusterVmMapVO doInTransaction(TransactionStatus status) {
                KubernetesClusterVmMapVO newClusterVmMap = new KubernetesClusterVmMapVO(kubernetesClusterId, vmId, isControlNode);
                newClusterVmMap.setExternalNode(isExternalNode);
                newClusterVmMap.setManualUpgrade(markForManualUpgrade);
                newClusterVmMap.setEtcdNode(isEtcdNode);
                if (!isEtcdNode) {
                    newClusterVmMap.setNodeVersion(kubernetesVersion.getSemanticVersion());
                }
                kubernetesClusterVmMapDao.persist(newClusterVmMap);
                return newClusterVmMap;
            }
        });
    }

    private UserVm fetchControlVmIfMissing(final UserVm controlVm) {
        if (controlVm != null) {
            return controlVm;
        }
        Long etcdNodeCount = kubernetesCluster.getEtcdNodeCount();
        List<KubernetesClusterVmMapVO> clusterVMs = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());
        if (CollectionUtils.isEmpty(clusterVMs)) {
            logger.warn(String.format("Unable to retrieve VMs for Kubernetes cluster : %s", kubernetesCluster.getName()));
            return null;
        }
        List<Long> vmIds = new ArrayList<>();
        for (KubernetesClusterVmMapVO vmMap : clusterVMs) {
            vmIds.add(vmMap.getVmId());
        }
        Collections.sort(vmIds);
        int controlNodeIndex = Objects.nonNull(etcdNodeCount) ? etcdNodeCount.intValue() : 0;
        return userVmDao.findById(vmIds.get(controlNodeIndex));
    }

    protected String getControlVmPrivateIp() {
        String ip = null;
        UserVm vm = fetchControlVmIfMissing(null);
        if (vm != null) {
            ip = vm.getPrivateIpAddress();
        }
        return ip;
    }

    protected IpAddress getNetworkSourceNatIp(Network network) {
        List<? extends IpAddress> addresses = networkModel.listPublicIpsAssignedToGuestNtwk(network.getId(), true);
        if (CollectionUtils.isNotEmpty(addresses)) {
            return addresses.get(0);
        }
        logger.warn(String.format("No public IP addresses found for network : %s, Kubernetes cluster : %s", network.getName(), kubernetesCluster.getName()));
        return null;
    }

    protected IpAddress getVpcTierKubernetesPublicIp(Network network) {
        KubernetesClusterDetailsVO detailsVO = kubernetesClusterDetailsDao.findDetail(kubernetesCluster.getId(), ApiConstants.PUBLIC_IP_ID);
        if (detailsVO == null || StringUtils.isEmpty(detailsVO.getValue())) {
            return null;
        }
        IpAddress address = ipAddressDao.findByUuid(detailsVO.getValue());
        if (address == null || !Objects.equals(network.getVpcId(), address.getVpcId())) {
            logger.warn("Public IP with ID: {} linked to the Kubernetes cluster: {} is not usable", detailsVO.getValue(), kubernetesCluster.getName());
            if (address == null) {
                logger.warn("Public IP with ID: {} was not found by uuid", detailsVO.getValue());
            } else {
                logger.warn("Public IP with ID: {} was associated with vpc {} instead of {}", detailsVO.getValue(), address.getVpcId(), network.getVpcId());
            }
            return null;
        }
        return address;
    }

    protected IpAddress getPublicIp(Network network) throws ManagementServerException {
        if (network.getVpcId() != null) {
            IpAddress publicIp = getVpcTierKubernetesPublicIp(network);
            if (publicIp == null) {
                throw new ManagementServerException(String.format("No public IP addresses found for VPC tier : %s, Kubernetes cluster : %s", network.getName(), kubernetesCluster.getName()));
            }
            return publicIp;
        }
        IpAddress publicIp = getNetworkSourceNatIp(network);
        if (publicIp == null) {
            throw new ManagementServerException(String.format("No source NAT IP addresses found for network : %s, Kubernetes cluster : %s",
                    network.getName(), kubernetesCluster.getName()));
        }
        return publicIp;
    }

    protected IpAddress acquireVpcTierKubernetesPublicIp(Network network, boolean forEtcd) throws
            InsufficientAddressCapacityException, ResourceAllocationException, ResourceUnavailableException {
        IpAddress ip = networkService.allocateIP(owner, kubernetesCluster.getZoneId(), network.getId(), null, null);
        if (ip == null) {
            return null;
        }
        ip = vpcService.associateIPToVpc(ip.getId(), network.getVpcId());
        ip = ipAddressManager.associateIPToGuestNetwork(ip.getId(), network.getId(), false);
        if (!forEtcd) {
            kubernetesClusterDetailsDao.addDetail(kubernetesCluster.getId(), ApiConstants.PUBLIC_IP_ID, ip.getUuid(), false);
        }
        return ip;
    }

    protected IpAddress acquirePublicIpForIsolatedNetwork(Network network) throws
            InsufficientAddressCapacityException, ResourceAllocationException, ResourceUnavailableException {
        IpAddress ip = networkService.allocateIP(owner, kubernetesCluster.getZoneId(), network.getId(), null, null);
        if (ip == null) {
            return null;
        }
        ip = networkService.associateIPToNetwork(ip.getId(), network.getId());
        return ip;
    }

    protected Pair<String, Integer> getKubernetesClusterServerIpSshPortForIsolatedNetwork(Network network) {
        String ip = null;
        IpAddress address = getNetworkSourceNatIp(network);
        if (address != null) {
            ip = address.getAddress().addr();
        }
        return new Pair<>(ip, CLUSTER_NODES_DEFAULT_START_SSH_PORT);
    }

    protected Pair<String, Integer> getKubernetesClusterServerIpSshPortForSharedNetwork(UserVm controlVm) {
        int port = DEFAULT_SSH_PORT;
        controlVm = fetchControlVmIfMissing(controlVm);
        if (controlVm == null) {
            logger.warn(String.format("Unable to retrieve control VM for Kubernetes cluster : %s", kubernetesCluster.getName()));
            return new Pair<>(null, port);
        }
        return new Pair<>(controlVm.getPrivateIpAddress(), port);
    }

    protected Pair<String, Integer> getKubernetesClusterServerIpSshPortForVpcTier(Network network,
                                                                                  boolean acquireNewPublicIpForVpcTierIfNeeded) throws
            InsufficientAddressCapacityException, ResourceAllocationException, ResourceUnavailableException {
        int port = CLUSTER_NODES_DEFAULT_START_SSH_PORT;
        IpAddress address = getVpcTierKubernetesPublicIp(network);
        if (address != null) {
            return new Pair<>(address.getAddress().addr(), port);
        }
        if (acquireNewPublicIpForVpcTierIfNeeded) {
            address = acquireVpcTierKubernetesPublicIp(network, false);
            if (address != null) {
                return new Pair<>(address.getAddress().addr(), port);
            }
        }
        logger.warn(String.format("No public IP found for the VPC tier: %s, Kubernetes cluster : %s", network, kubernetesCluster.getName()));
        return new Pair<>(null, port);
    }

    protected Pair<String, Integer> getKubernetesClusterServerIpSshPort(UserVm controlVm, boolean acquireNewPublicIpForVpcTierIfNeeded) throws
            InsufficientAddressCapacityException, ResourceAllocationException, ResourceUnavailableException {
        int port = CLUSTER_NODES_DEFAULT_START_SSH_PORT;
        KubernetesClusterDetailsVO detail = kubernetesClusterDetailsDao.findDetail(kubernetesCluster.getId(), ApiConstants.EXTERNAL_LOAD_BALANCER_IP_ADDRESS);
        if (detail != null && StringUtils.isNotEmpty(detail.getValue())) {
            return new Pair<>(detail.getValue(), port);
        }
        Network network = networkDao.findById(kubernetesCluster.getNetworkId());
        if (network == null) {
            logger.warn(String.format("Network for Kubernetes cluster : %s cannot be found", kubernetesCluster.getName()));
            return new Pair<>(null, port);
        }
        if (manager.isDirectAccess(network)) {
            return getKubernetesClusterServerIpSshPortForSharedNetwork(controlVm);
        }
        if (network.getVpcId() != null) {
            return getKubernetesClusterServerIpSshPortForVpcTier(network, acquireNewPublicIpForVpcTierIfNeeded);
        }
        if (Network.GuestType.Isolated.equals(network.getGuestType())) {
            return getKubernetesClusterServerIpSshPortForIsolatedNetwork(network);
        }
        logger.warn(String.format("Unable to retrieve server IP address for Kubernetes cluster : %s", kubernetesCluster.getName()));
        return  new Pair<>(null, port);
    }

    protected Pair<String, Integer> getKubernetesClusterServerIpSshPort(UserVm controlVm) {
        try {
            return getKubernetesClusterServerIpSshPort(controlVm, false);
        } catch (InsufficientAddressCapacityException | ResourceAllocationException | ResourceUnavailableException e) {
            logger.debug("This exception should not have occurred", e);
        }
        return new Pair<>(null, CLUSTER_NODES_DEFAULT_START_SSH_PORT);
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
            CallContext vmContext  = CallContext.register(CallContext.current(), ApiCommandResourceType.VirtualMachine);
            vmContext.putContextParameter(VirtualMachine.class, vm.getUuid());
            try {
                templateService.attachIso(iso.getId(), vm.getId(), true);
                if (logger.isInfoEnabled()) {
                    logger.info("Attached binaries ISO for VM: {} in cluster: {}", vm, kubernetesCluster);
                }
            } catch (CloudRuntimeException ex) {
                logTransitStateAndThrow(Level.ERROR, String.format("Failed to attach binaries ISO for VM : %s in the Kubernetes cluster name: %s", vm.getDisplayName(), kubernetesCluster.getName()), kubernetesCluster.getId(), failedEvent, ex);
            } finally {
                CallContext.unregister();
            }
        }
    }

    protected void attachIsoKubernetesVMs(List<UserVm> clusterVMs) throws CloudRuntimeException {
        attachIsoKubernetesVMs(clusterVMs, null);
    }

    protected void detachIsoKubernetesVMs(List<UserVm> clusterVMs) {
        for (UserVm vm : clusterVMs) {
            boolean result = false;
            CallContext vmContext  = CallContext.register(CallContext.current(), ApiCommandResourceType.VirtualMachine);
            vmContext.putContextParameter(VirtualMachine.class, vm.getUuid());
            try {
                result = templateService.detachIso(vm.getId(), null, true);
            } catch (CloudRuntimeException ex) {
                logger.warn("Failed to detach binaries ISO from VM: {} in the Kubernetes cluster: {} ", vm, kubernetesCluster, ex);
            } finally {
                CallContext.unregister();
            }
            if (result) {
                if (logger.isInfoEnabled()) {
                    logger.info("Detached Kubernetes binaries from VM: {} in the Kubernetes cluster: {}", vm, kubernetesCluster);
                }
                continue;
            }
            logger.warn("Failed to detach binaries ISO from VM: {} in the Kubernetes cluster: {} ", vm, kubernetesCluster);
        }
    }

    protected List<KubernetesClusterVmMapVO> getKubernetesClusterVMMaps() {
        return kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());
    }

    protected List<KubernetesClusterVmMapVO> getKubernetesClusterVMMapsForNodes(List<Long> nodeIds) {
        return kubernetesClusterVmMapDao.listByClusterIdAndVmIdsIn(kubernetesCluster.getId(), nodeIds);
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

    protected void updateLoginUserDetails(List<Long> clusterVMs) {
        if (clusterVMs == null) {
            clusterVMs = getKubernetesClusterVMMaps().stream().map(KubernetesClusterVmMapVO::getVmId).collect(Collectors.toList());
        }
        if (!CollectionUtils.isEmpty(clusterVMs)) {
            for (Long vmId : clusterVMs) {
                UserVm controlNode = userVmDao.findById(vmId);
                if (controlNode != null) {
                    userVmDetailsDao.addDetail(vmId, VmDetailConstants.CKS_CONTROL_NODE_LOGIN_USER, CLUSTER_NODE_VM_USER, true);
                }
            }
        }
    }

    protected boolean stateTransitTo(long kubernetesClusterId, KubernetesCluster.Event e) {
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        try {
            return _stateMachine.transitTo(kubernetesCluster, e, null, kubernetesClusterDao);
        } catch (NoTransitionException nte) {
            logger.warn(String.format("Failed to transition state of the Kubernetes cluster : %s in state %s on event %s",
                kubernetesCluster.getName(), kubernetesCluster.getState().toString(), e.toString()), nte);
            return false;
        }
    }

    protected boolean createCloudStackSecret(String[] keys) {
        File pkFile = getManagementServerSshPublicKeyFile();
        Pair<String, Integer> publicIpSshPort = getKubernetesClusterServerIpSshPort(null);
        publicIpAddress = publicIpSshPort.first();
        sshPort = publicIpSshPort.second();

        try {
            String command = String.format("sudo %s/%s -u '%s' -k '%s' -s '%s'",
                    scriptPath, deploySecretsScriptFilename, ApiServiceConfiguration.ApiServletPath.value(), keys[0], keys[1]);
            Account account = accountDao.findById(kubernetesCluster.getAccountId());
            if (account != null && account.getType() == Account.Type.PROJECT) {
                String projectId = projectService.findByProjectAccountId(account.getId()).getUuid();
                command = String.format("%s -p '%s'", command, projectId);
            }
            Pair<Boolean, String> result = SshHelper.sshExecute(publicIpAddress, sshPort, getControlNodeLoginUser(),
                    pkFile, null, command, 10000, 10000, 60000);
            return result.first();
        } catch (Exception e) {
            String msg = String.format("Failed to add cloudstack-secret to Kubernetes cluster: %s", kubernetesCluster.getName());
            logger.warn(msg, e);
        }
        return false;
    }

    protected File retrieveScriptFile(String filename) {
        File file = null;
        try {
            String data = readResourceFile("/script/" + filename);
            file = File.createTempFile(filename, ".sh");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(data);
            writer.close();
        } catch (IOException e) {
            logAndThrow(Level.ERROR, String.format("Kubernetes Cluster %s : Failed to fetch script %s",
                    kubernetesCluster.getName(), filename), e);
        }
        return file;
    }

    protected void retrieveScriptFiles() {
        deploySecretsScriptFile = retrieveScriptFile(deploySecretsScriptFilename);
        deployProviderScriptFile = retrieveScriptFile(deployProviderScriptFilename);
        autoscaleScriptFile = retrieveScriptFile(autoscaleScriptFilename);
    }

    protected void copyScripts(String nodeAddress, final int sshPort) {
        copyScriptFile(nodeAddress, sshPort, deploySecretsScriptFile, deploySecretsScriptFilename);
        copyScriptFile(nodeAddress, sshPort, deployProviderScriptFile, deployProviderScriptFilename);
        copyScriptFile(nodeAddress, sshPort, autoscaleScriptFile, autoscaleScriptFilename);
    }

    protected void copyScriptFile(String nodeAddress, final int sshPort, File file, String destination) {
        try {
            if (Objects.isNull(sshKeyFile)) {
                sshKeyFile = getManagementServerSshPublicKeyFile();
            }
            SshHelper.scpTo(nodeAddress, sshPort, getControlNodeLoginUser(), sshKeyFile, null,
                    "~/", file.getAbsolutePath(), "0755", 20000, 30 * 60 * 1000);
            // Ensure destination dir scriptPath exists and copy file to destination
            String cmdStr = String.format("sudo mkdir -p %s ; sudo mv ~/%s %s/%s", scriptPath, file.getName(), scriptPath, destination);
            SshHelper.sshExecute(nodeAddress, sshPort, getControlNodeLoginUser(), sshKeyFile, null,
                    cmdStr, 10000, 10000, 10 * 60 * 1000);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    protected boolean taintControlNodes() {
        StringBuilder commands = new StringBuilder();
        List<KubernetesClusterVmMapVO> vmMapVOList = getKubernetesClusterVMMaps();
        for(KubernetesClusterVmMapVO vmMap :vmMapVOList) {
            if(!vmMap.isControlNode()) {
                continue;
            }
            String name = userVmDao.findById(vmMap.getVmId()).getDisplayName().toLowerCase();
            String command = String.format("sudo /opt/bin/kubectl annotate node %s cluster-autoscaler.kubernetes.io/scale-down-disabled=true ; ", name);
            commands.append(command);
        }
        int retryCounter = 0;
        while (retryCounter < 3) {
            retryCounter++;
            try {
                File pkFile = getManagementServerSshPublicKeyFile();
                Pair<String, Integer> publicIpSshPort = getKubernetesClusterServerIpSshPort(null);
                publicIpAddress = publicIpSshPort.first();
                sshPort = publicIpSshPort.second();

                Pair<Boolean, String> result = SshHelper.sshExecute(publicIpAddress, sshPort, getControlNodeLoginUser(),
                        pkFile, null, commands.toString(), 10000, 10000, 60000);
                return result.first();
            } catch (Exception e) {
                String msg = String.format("Failed to taint control nodes on : %s : %s", kubernetesCluster.getName(), e.getMessage());
                logMessage(Level.ERROR, msg, e);
            }
            try {
                Thread.sleep(5 * 1000L);
            } catch (InterruptedException ie) {
                logger.error(String.format("Error while attempting to taint nodes on Kubernetes cluster: %s", kubernetesCluster.getName()), ie);
            }
            retryCounter++;
        }
        return false;
    }

    protected boolean deployProvider() {
        Network network = networkDao.findById(kubernetesCluster.getNetworkId());
        // Since the provider creates IP addresses, don't deploy it unless the underlying network supports it
        if (manager.isDirectAccess(network)) {
            logMessage(Level.INFO, String.format("Skipping adding the provider as %s is not on an isolated network",
                    kubernetesCluster.getName()), null);
            return true;
        }
        File pkFile = getManagementServerSshPublicKeyFile();
        Pair<String, Integer> publicIpSshPort = getKubernetesClusterServerIpSshPort(null);
        publicIpAddress = publicIpSshPort.first();
        sshPort = publicIpSshPort.second();

        try {
            String command = String.format("sudo %s/%s", scriptPath, deployProviderScriptFilename);
            Pair<Boolean, String> result = SshHelper.sshExecute(publicIpAddress, sshPort, getControlNodeLoginUser(),
                    pkFile, null, command, 10000, 10000, 60000);

            // Maybe the file isn't present. Try and copy it
            if (!result.first()) {
                logMessage(Level.INFO, "Provider files missing. Adding them now", null);
                retrieveScriptFiles();
                copyScripts(publicIpAddress, sshPort);

                if (!createCloudStackSecret(keys)) {
                    logTransitStateAndThrow(Level.ERROR, String.format("Failed to setup keys for Kubernetes cluster %s",
                            kubernetesCluster.getName()), kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
                }

                // If at first you don't succeed ...
                result = SshHelper.sshExecute(publicIpAddress, sshPort, getControlNodeLoginUser(),
                        pkFile, null, command, 10000, 10000, 60000);
                if (!result.first()) {
                    throw new CloudRuntimeException(result.second());
                }
            }
            return true;
        } catch (Exception e) {
            String msg = String.format("Failed to deploy kubernetes provider: %s : %s", kubernetesCluster.getName(), e.getMessage());
            logAndThrow(Level.ERROR, msg);
            return false;
        }
    }

    public void setKeys(String[] keys) {
        this.keys = keys;
    }

    protected ServiceOffering getServiceOfferingForNodeTypeOnCluster(KubernetesClusterNodeType nodeType,
                                                                     KubernetesCluster cluster) {
        Long offeringId = null;
        Long defaultOfferingId = cluster.getServiceOfferingId();
        Long controlOfferingId = cluster.getControlNodeServiceOfferingId();
        Long workerOfferingId = cluster.getWorkerNodeServiceOfferingId();
        Long etcdOfferingId = cluster.getEtcdNodeServiceOfferingId();
        if (KubernetesClusterNodeType.CONTROL == nodeType) {
            offeringId = controlOfferingId != null ? controlOfferingId : defaultOfferingId;
        } else if (KubernetesClusterNodeType.WORKER == nodeType) {
            offeringId = workerOfferingId != null ? workerOfferingId : defaultOfferingId;
        } else if (KubernetesClusterNodeType.ETCD == nodeType && cluster.getEtcdNodeCount() != null && cluster.getEtcdNodeCount() > 0) {
            offeringId = etcdOfferingId != null ? etcdOfferingId : defaultOfferingId;
        }

        if (offeringId == null) {
            String msg = String.format("Cannot find a service offering for the %s nodes on the Kubernetes cluster %s", nodeType.name(), cluster.getName());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
        return serviceOfferingDao.findById(offeringId);
    }

    protected boolean isDefaultTemplateUsed() {
        if (Arrays.asList(kubernetesCluster.getControlNodeTemplateId(), kubernetesCluster.getWorkerNodeTemplateId(), kubernetesCluster.getEtcdNodeTemplateId()).contains(kubernetesCluster.getTemplateId())) {
            return true;
        }
        return false;
    }

    protected void provisionPublicIpPortForwardingRule(IpAddress publicIp, Network network, Account account,
                                                       final long vmId, final int sourcePort, final int destPort) throws NetworkRuleConflictException, ResourceUnavailableException {
        final long publicIpId = publicIp.getId();
        final long networkId = network.getId();
        final long accountId = account.getId();
        final long domainId = account.getDomainId();
        Nic vmNic = networkModel.getNicInNetwork(vmId, networkId);
        final Ip vmIp = new Ip(vmNic.getIPv4Address());
        PortForwardingRuleVO pfRule = Transaction.execute((TransactionCallbackWithException<PortForwardingRuleVO, NetworkRuleConflictException>) status -> {
            PortForwardingRuleVO newRule =
                    new PortForwardingRuleVO(null, publicIpId,
                            sourcePort, sourcePort,
                            vmIp,
                            destPort, destPort,
                            "tcp", networkId, accountId, domainId, vmId);
            newRule.setDisplay(true);
            newRule.setState(FirewallRule.State.Add);
            newRule = portForwardingRulesDao.persist(newRule);
            return newRule;
        });
        rulesService.applyPortForwardingRules(publicIp.getId(), account);
        if (logger.isInfoEnabled()) {
            logger.info(String.format("Provisioned SSH port forwarding rule: %s from port %d to %d on %s to the VM IP : %s in Kubernetes cluster : %s", pfRule.getUuid(), sourcePort, destPort, publicIp.getAddress().addr(), vmIp.toString(), kubernetesCluster.getName()));
        }
    }

    public String getKubernetesNodeConfig(final String joinIp, final boolean ejectIso, final boolean mountCksIsoOnVR) throws IOException {
        String k8sNodeConfig = readK8sConfigFile("/conf/k8s-node.yml");
        final String sshPubKey = "{{ k8s.ssh.pub.key }}";
        final String joinIpKey = "{{ k8s_control_node.join_ip }}";
        final String clusterTokenKey = "{{ k8s_control_node.cluster.token }}";
        final String ejectIsoKey = "{{ k8s.eject.iso }}";
        final String routerIpKey = "{{ k8s.vr.iso.mounted.ip }}";
        final String installWaitTime = "{{ k8s.install.wait.time }}";
        final String installReattemptsCount = "{{ k8s.install.reattempts.count }}";

        final Long waitTime = KubernetesClusterService.KubernetesWorkerNodeInstallAttemptWait.value();
        final Long reattempts = KubernetesClusterService.KubernetesWorkerNodeInstallReattempts.value();
        String routerIp = "";
        if (mountCksIsoOnVR) {
            NicVO routerNicOnNetwork = getVirtualRouterNicOnKubernetesClusterNetwork(kubernetesCluster);
            if (Objects.nonNull(routerNicOnNetwork)) {
                routerIp = routerNicOnNetwork.getIPv4Address();
            }
        }
        String pubKey = "- \"" + configurationDao.getValue("ssh.publickey") + "\"";
        String sshKeyPair = kubernetesCluster.getKeyPair();
        if (StringUtils.isNotEmpty(sshKeyPair)) {
            SSHKeyPairVO sshkp = sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), sshKeyPair);
            if (sshkp != null) {
                pubKey += "\n      - \"" + sshkp.getPublicKey() + "\"";
            }
        }
        k8sNodeConfig = k8sNodeConfig.replace(sshPubKey, pubKey);
        k8sNodeConfig = k8sNodeConfig.replace(joinIpKey, joinIp);
        k8sNodeConfig = k8sNodeConfig.replace(clusterTokenKey, KubernetesClusterUtil.generateClusterToken(kubernetesCluster));
        k8sNodeConfig = k8sNodeConfig.replace(ejectIsoKey, String.valueOf(ejectIso));
        k8sNodeConfig = k8sNodeConfig.replace(routerIpKey, routerIp);
        k8sNodeConfig = k8sNodeConfig.replace(installWaitTime, String.valueOf(waitTime));
        k8sNodeConfig = k8sNodeConfig.replace(installReattemptsCount, String.valueOf(reattempts));

        k8sNodeConfig = updateKubeConfigWithRegistryDetails(k8sNodeConfig);

        return k8sNodeConfig;
    }

    protected String updateKubeConfigWithRegistryDetails(String k8sConfig) {
        /* genarate /etc/containerd/config.toml file on the nodes only if Kubernetes cluster is created to
         * use docker private registry */
        String registryUsername = null;
        String registryPassword = null;
        String registryUrl = null;

        List<KubernetesClusterDetailsVO> details = kubernetesClusterDetailsDao.listDetails(kubernetesCluster.getId());
        for (KubernetesClusterDetailsVO detail : details) {
            if (detail.getName().equals(ApiConstants.DOCKER_REGISTRY_USER_NAME)) {
                registryUsername = detail.getValue();
            }
            if (detail.getName().equals(ApiConstants.DOCKER_REGISTRY_PASSWORD)) {
                registryPassword = detail.getValue();
            }
            if (detail.getName().equals(ApiConstants.DOCKER_REGISTRY_URL)) {
                registryUrl = detail.getValue();
            }
        }

        if (StringUtils.isNoneEmpty(registryUsername, registryPassword, registryUrl)) {
            // Update runcmd in the cloud-init configuration to run a script that updates the containerd config with provided registry details
            String runCmd = "- bash -x /opt/bin/setup-containerd";

            String registryEp = registryUrl.split("://")[1];
            k8sConfig = k8sConfig.replace("- containerd config default > /etc/containerd/config.toml", runCmd);
            final String registryUrlKey = "{{registry.url}}";
            final String registryUrlEpKey = "{{registry.url.endpoint}}";
            final String registryAuthKey = "{{registry.token}}";
            final String registryUname = "{{registry.username}}";
            final String registryPsswd = "{{registry.password}}";

            final String usernamePasswordKey = registryUsername + ":" + registryPassword;
            String base64Auth = Base64.encodeBase64String(usernamePasswordKey.getBytes(com.cloud.utils.StringUtils.getPreferredCharset()));
            k8sConfig = k8sConfig.replace(registryUrlKey,   registryUrl);
            k8sConfig = k8sConfig.replace(registryUrlEpKey, registryEp);
            k8sConfig = k8sConfig.replace(registryUname, registryUsername);
            k8sConfig = k8sConfig.replace(registryPsswd, registryPassword);
            k8sConfig = k8sConfig.replace(registryAuthKey, base64Auth);
        }
        return k8sConfig;
    }

    public Map<Long, Integer> addFirewallRulesForNodes(IpAddress publicIp, int size) throws ManagementServerException {
        Map<Long, Integer> vmIdPortMap = new HashMap<>();
        CallContext.register(CallContext.current(), null);
        try {
            List<KubernetesClusterVmMapVO> clusterVmList = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());
            List<KubernetesClusterVmMapVO> externalNodes = clusterVmList.stream().filter(KubernetesClusterVmMapVO::isExternalNode).collect(Collectors.toList());
            int endPort = (CLUSTER_NODES_DEFAULT_START_SSH_PORT + clusterVmList.size() - externalNodes.size() - kubernetesCluster.getEtcdNodeCount().intValue() - 1);
            provisionFirewallRules(publicIp, owner, CLUSTER_NODES_DEFAULT_START_SSH_PORT, endPort);
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Provisioned firewall rule to open up port %d to %d on %s for Kubernetes cluster : %s", CLUSTER_NODES_DEFAULT_START_SSH_PORT, endPort, publicIp.getAddress().addr(), kubernetesCluster.getName()));
            }
            if (!externalNodes.isEmpty()) {
                AtomicInteger additionalNodes = new AtomicInteger(1);
                externalNodes.forEach(externalNode -> {
                    int port = endPort + additionalNodes.get();
                    try {
                        provisionFirewallRules(publicIp, owner, port, port);
                        vmIdPortMap.put(externalNode.getVmId(), port);
                    } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException | NetworkRuleConflictException e) {
                        throw new CloudRuntimeException(String.format("Failed to provision firewall rules for SSH access for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
                    }
                    additionalNodes.addAndGet(1);
                });
            }
        } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException | NetworkRuleConflictException e) {
            throw new ManagementServerException(String.format("Failed to provision firewall rules for SSH access for the Kubernetes cluster : %s", kubernetesCluster.getName()), e);
        } finally {
            CallContext.unregister();
        }
        return vmIdPortMap;
    }

    protected void provisionFirewallRules(final IpAddress publicIp, final Account account, int startPort, int endPort) throws NoSuchFieldException,
            IllegalAccessException, ResourceUnavailableException, NetworkRuleConflictException {
        List<String> sourceCidrList = new ArrayList<String>();
        sourceCidrList.add("0.0.0.0/0");

        CreateFirewallRuleCmd rule = new CreateFirewallRuleCmd();
        rule = ComponentContext.inject(rule);

        Field addressField = rule.getClass().getDeclaredField("ipAddressId");
        addressField.setAccessible(true);
        addressField.set(rule, publicIp.getId());

        Field protocolField = rule.getClass().getDeclaredField("protocol");
        protocolField.setAccessible(true);
        protocolField.set(rule, "TCP");

        Field startPortField = rule.getClass().getDeclaredField("publicStartPort");
        startPortField.setAccessible(true);
        startPortField.set(rule, startPort);

        Field endPortField = rule.getClass().getDeclaredField("publicEndPort");
        endPortField.setAccessible(true);
        endPortField.set(rule, endPort);

        Field cidrField = rule.getClass().getDeclaredField("cidrlist");
        cidrField.setAccessible(true);
        cidrField.set(rule, sourceCidrList);

        firewallService.createIngressFirewallRule(rule);
        firewallService.applyIngressFwRules(publicIp.getId(), account);
    }

    protected NicVO getVirtualRouterNicOnKubernetesClusterNetwork(KubernetesCluster kubernetesCluster) {
        long networkId = kubernetesCluster.getNetworkId();
        NetworkVO kubernetesClusterNetwork = networkDao.findById(networkId);
        if (kubernetesClusterNetwork == null) {
            logAndThrow(Level.ERROR, String.format("Cannot find network %s set on Kubernetes Cluster %s", networkId, kubernetesCluster.getName()));
        }
        NicVO routerNicOnNetwork = nicDao.findByNetworkIdAndType(networkId, VirtualMachine.Type.DomainRouter);
        if (routerNicOnNetwork == null) {
            logAndThrow(Level.ERROR, String.format("Cannot find a Virtual Router on Kubernetes Cluster %s network %s", kubernetesCluster.getName(), kubernetesClusterNetwork.getName()));
        }
        return routerNicOnNetwork;
    }

    protected Map<Long, Integer> getVmPortMap() {
        List<KubernetesClusterVmMapVO> clusterVmList = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());
        List<KubernetesClusterVmMapVO> externalNodes = clusterVmList.stream().filter(KubernetesClusterVmMapVO::isExternalNode).collect(Collectors.toList());
        Map<Long, Integer> vmIdPortMap = new HashMap<>();
        int defaultNodesCount = clusterVmList.size() - externalNodes.size();
        AtomicInteger i = new AtomicInteger(0);
        externalNodes.forEach(node -> {
            vmIdPortMap.put(node.getVmId(), CLUSTER_NODES_DEFAULT_START_SSH_PORT + defaultNodesCount + i.get());
            i.addAndGet(1);
        });
        return vmIdPortMap;
    }

    public Long getExplicitAffinityGroup(Long domainId, Long accountId) {
        AffinityGroupVO groupVO = null;
        if (Objects.nonNull(accountId)) {
            groupVO = affinityGroupDao.findByAccountAndType(accountId, "ExplicitDedication");
        }
        if (Objects.isNull(groupVO)) {
            groupVO = affinityGroupDao.findDomainLevelGroupByType(domainId, "ExplicitDedication");
        }
        if (Objects.nonNull(groupVO)) {
            return groupVO.getId();
        }
        return null;
    }
}
