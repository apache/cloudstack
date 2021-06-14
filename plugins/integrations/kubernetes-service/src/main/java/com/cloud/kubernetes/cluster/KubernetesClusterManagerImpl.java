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
package com.cloud.kubernetes.cluster;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.CreateKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.DeleteKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.GetKubernetesClusterConfigCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.ListKubernetesClustersCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.ScaleKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.StartKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.StopKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.UpgradeKubernetesClusterCmd;
import org.apache.cloudstack.api.response.KubernetesClusterConfigResponse;
import org.apache.cloudstack.api.response.KubernetesClusterResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.dao.NetworkOfferingJoinDao;
import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.vo.NetworkOfferingJoinVO;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.capacity.CapacityManager;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.kubernetes.cluster.actionworkers.KubernetesClusterActionWorker;
import com.cloud.kubernetes.cluster.actionworkers.KubernetesClusterDestroyWorker;
import com.cloud.kubernetes.cluster.actionworkers.KubernetesClusterScaleWorker;
import com.cloud.kubernetes.cluster.actionworkers.KubernetesClusterStartWorker;
import com.cloud.kubernetes.cluster.actionworkers.KubernetesClusterStopWorker;
import com.cloud.kubernetes.cluster.actionworkers.KubernetesClusterUpgradeWorker;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.KubernetesSupportedVersion;
import com.cloud.kubernetes.version.KubernetesSupportedVersionVO;
import com.cloud.kubernetes.version.KubernetesVersionManagerImpl;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping;
import com.cloud.projects.Project;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.base.Strings;

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

public class KubernetesClusterManagerImpl extends ManagerBase implements KubernetesClusterService {

    private static final Logger LOGGER = Logger.getLogger(KubernetesClusterManagerImpl.class);
    private static final String DEFAULT_NETWORK_OFFERING_FOR_KUBERNETES_SERVICE_NAME = "DefaultNetworkOfferingforKubernetesService";

    protected StateMachine2<KubernetesCluster.State, KubernetesCluster.Event, KubernetesCluster> _stateMachine = KubernetesCluster.State.getStateMachine();

    ScheduledExecutorService _gcExecutor;
    ScheduledExecutorService _stateScanner;

    @Inject
    public KubernetesClusterDao kubernetesClusterDao;
    @Inject
    public KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    @Inject
    public KubernetesClusterDetailsDao kubernetesClusterDetailsDao;
    @Inject
    public KubernetesSupportedVersionDao kubernetesSupportedVersionDao;
    @Inject
    protected SSHKeyPairDao sshKeyPairDao;
    @Inject
    protected DataCenterDao dataCenterDao;
    @Inject
    protected ClusterDao clusterDao;
    @Inject
    protected ClusterDetailsDao clusterDetailsDao;
    @Inject
    protected HostDao hostDao;
    @Inject
    protected ServiceOfferingDao serviceOfferingDao;
    @Inject
    protected VMTemplateDao templateDao;
    @Inject
    protected TemplateJoinDao templateJoinDao;
    @Inject
    protected AccountService accountService;
    @Inject
    protected AccountManager accountManager;
    @Inject
    protected VMInstanceDao vmInstanceDao;
    @Inject
    protected UserVmJoinDao userVmJoinDao;
    @Inject
    protected NetworkOfferingDao networkOfferingDao;
    @Inject
    protected NetworkOfferingJoinDao networkOfferingJoinDao;
    @Inject
    protected NetworkOfferingServiceMapDao networkOfferingServiceMapDao;
    @Inject
    protected NetworkService networkService;
    @Inject
    protected NetworkModel networkModel;
    @Inject
    protected PhysicalNetworkDao physicalNetworkDao;
    @Inject
    protected NetworkOrchestrationService networkMgr;
    @Inject
    protected NetworkDao networkDao;
    @Inject
    protected IPAddressDao ipAddressDao;
    @Inject
    protected CapacityManager capacityManager;
    @Inject
    protected ResourceManager resourceManager;
    @Inject
    protected FirewallRulesDao firewallRulesDao;

    private void logMessage(final Level logLevel, final String message, final Exception e) {
        if (logLevel == Level.WARN) {
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

    private void logTransitStateAndThrow(final Level logLevel, final String message, final Long kubernetesClusterId, final KubernetesCluster.Event event, final Exception e) throws CloudRuntimeException {
        logMessage(logLevel, message, e);
        if (kubernetesClusterId != null && event != null) {
            stateTransitTo(kubernetesClusterId, event);
        }
        if (e == null) {
            throw new CloudRuntimeException(message);
        }
        throw new CloudRuntimeException(message, e);
    }

    private void logAndThrow(final Level logLevel, final String message) throws CloudRuntimeException {
        logTransitStateAndThrow(logLevel, message, null, null, null);
    }

    private void logAndThrow(final Level logLevel, final String message, final Exception ex) throws CloudRuntimeException {
        logTransitStateAndThrow(logLevel, message, null, null, ex);
    }

    private boolean isKubernetesServiceTemplateConfigured(DataCenter zone) {
        // Check Kubernetes VM template for zone
        boolean isHyperVAvailable = false;
        boolean isKVMAvailable = false;
        boolean isVMwareAvailable = false;
        boolean isXenserverAvailable = false;
        List<ClusterVO> clusters = clusterDao.listByZoneId(zone.getId());
        for (ClusterVO clusterVO : clusters) {
            if (Hypervisor.HypervisorType.Hyperv.equals(clusterVO.getHypervisorType())) {
                isHyperVAvailable = true;
            }
            if (Hypervisor.HypervisorType.KVM.equals(clusterVO.getHypervisorType())) {
                isKVMAvailable = true;
            }
            if (Hypervisor.HypervisorType.VMware.equals(clusterVO.getHypervisorType())) {
                isVMwareAvailable = true;
            }
            if (Hypervisor.HypervisorType.XenServer.equals(clusterVO.getHypervisorType())) {
                isXenserverAvailable = true;
            }
        }
        List<Pair<String, String>> templatePairs = new ArrayList<>();
        if (isHyperVAvailable) {
            templatePairs.add(new Pair<>(KubernetesClusterHyperVTemplateName.key(), KubernetesClusterHyperVTemplateName.value()));
        }
        if (isKVMAvailable) {
            templatePairs.add(new Pair<>(KubernetesClusterKVMTemplateName.key(), KubernetesClusterKVMTemplateName.value()));
        }
        if (isVMwareAvailable) {
            templatePairs.add(new Pair<>(KubernetesClusterVMwareTemplateName.key(), KubernetesClusterVMwareTemplateName.value()));
        }
        if (isXenserverAvailable) {
            templatePairs.add(new Pair<>(KubernetesClusterXenserverTemplateName.key(), KubernetesClusterXenserverTemplateName.value()));
        }
        for (Pair<String, String> templatePair : templatePairs) {
            String templateKey = templatePair.first();
            String templateName = templatePair.second();
            if (Strings.isNullOrEmpty(templateName)) {
                LOGGER.warn(String.format("Global setting %s is empty. Template name need to be specified for Kubernetes service to function", templateKey));
                return false;
            }
            final VMTemplateVO template = templateDao.findValidByTemplateName(templateName);
            if (template == null) {
                LOGGER.warn(String.format("Unable to find the template %s to be used for provisioning Kubernetes cluster nodes", templateName));
                return false;
            }
            if (CollectionUtils.isEmpty(templateJoinDao.newTemplateView(template, zone.getId(), true))) {
                LOGGER.warn(String.format("The template ID: %s, name: %s is not available for use in zone ID: %s provisioning Kubernetes cluster nodes", template.getUuid(), templateName, zone.getUuid()));
                return false;
            }
        }
        return true;
    }

    private boolean isKubernetesServiceNetworkOfferingConfigured(DataCenter zone) {
        // Check network offering
        String networkOfferingName = KubernetesClusterNetworkOffering.value();
        if (networkOfferingName == null || networkOfferingName.isEmpty()) {
            LOGGER.warn(String.format("Global setting %s is empty. Admin has not yet specified the network offering to be used for provisioning isolated network for the cluster", KubernetesClusterNetworkOffering.key()));
            return false;
        }
        NetworkOfferingVO networkOffering = networkOfferingDao.findByUniqueName(networkOfferingName);
        if (networkOffering == null) {
            LOGGER.warn(String.format("Unable to find the network offering %s to be used for provisioning Kubernetes cluster", networkOfferingName));
            return false;
        }
        if (networkOffering.getState() == NetworkOffering.State.Disabled) {
            LOGGER.warn(String.format("Network offering ID: %s is not enabled", networkOffering.getUuid()));
            return false;
        }
        List<String> services = networkOfferingServiceMapDao.listServicesForNetworkOffering(networkOffering.getId());
        if (services == null || services.isEmpty() || !services.contains("SourceNat")) {
            LOGGER.warn(String.format("Network offering ID: %s does not have necessary services to provision Kubernetes cluster", networkOffering.getUuid()));
            return false;
        }
        if (!networkOffering.isEgressDefaultPolicy()) {
            LOGGER.warn(String.format("Network offering ID: %s has egress default policy turned off should be on to provision Kubernetes cluster", networkOffering.getUuid()));
            return false;
        }
        boolean offeringAvailableForZone = false;
        List<NetworkOfferingJoinVO> networkOfferingJoinVOs = networkOfferingJoinDao.findByZoneId(zone.getId(), true);
        for (NetworkOfferingJoinVO networkOfferingJoinVO : networkOfferingJoinVOs) {
            if (networkOffering.getId() == networkOfferingJoinVO.getId()) {
                offeringAvailableForZone = true;
                break;
            }
        }
        if (!offeringAvailableForZone) {
            LOGGER.warn(String.format("Network offering ID: %s is not available for zone ID: %s", networkOffering.getUuid(), zone.getUuid()));
            return false;
        }
        long physicalNetworkId = networkModel.findPhysicalNetworkId(zone.getId(), networkOffering.getTags(), networkOffering.getTrafficType());
        PhysicalNetwork physicalNetwork = physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            LOGGER.warn(String.format("Unable to find physical network with tag: %s", networkOffering.getTags()));
            return false;
        }
        return true;
    }

    private boolean isKubernetesServiceConfigured(DataCenter zone) {
        if (!isKubernetesServiceTemplateConfigured(zone)) {
            return false;
        }
        if (!isKubernetesServiceNetworkOfferingConfigured(zone)) {
            return false;
        }
        return true;
    }

    private IpAddress getSourceNatIp(Network network) {
        List<? extends IpAddress> addresses = networkModel.listPublicIpsAssignedToGuestNtwk(network.getId(), true);
        if (CollectionUtils.isEmpty(addresses)) {
            return null;
        }
        for (IpAddress address : addresses) {
            if (address.isSourceNat()) {
                return address;
            }
        }
        return null;
    }

    private VMTemplateVO getKubernetesServiceTemplate(Hypervisor.HypervisorType hypervisorType) {
        String templateName = null;
        switch (hypervisorType) {
            case Hyperv:
                templateName = KubernetesClusterHyperVTemplateName.value();
                break;
            case KVM:
                templateName = KubernetesClusterKVMTemplateName.value();
                break;
            case VMware:
                templateName = KubernetesClusterVMwareTemplateName.value();
                break;
            case XenServer:
                templateName = KubernetesClusterXenserverTemplateName.value();
                break;
        }
        return templateDao.findValidByTemplateName(templateName);
    }

    private boolean validateIsolatedNetwork(Network network, int clusterTotalNodeCount) {
        if (Network.GuestType.Isolated.equals(network.getGuestType())) {
            if (Network.State.Allocated.equals(network.getState())) { // Allocated networks won't have IP and rules
                return true;
            }
            IpAddress sourceNatIp = getSourceNatIp(network);
            if (sourceNatIp == null) {
                throw new InvalidParameterValueException(String.format("Network ID: %s does not have a source NAT IP associated with it. To provision a Kubernetes Cluster, source NAT IP is required", network.getUuid()));
            }
            List<FirewallRuleVO> rules = firewallRulesDao.listByIpAndPurposeAndNotRevoked(sourceNatIp.getId(), FirewallRule.Purpose.Firewall);
            for (FirewallRuleVO rule : rules) {
                Integer startPort = rule.getSourcePortStart();
                Integer endPort = rule.getSourcePortEnd();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Network rule : " + startPort + " " + endPort);
                }
                if (startPort <= KubernetesClusterActionWorker.CLUSTER_API_PORT && KubernetesClusterActionWorker.CLUSTER_API_PORT <= endPort) {
                    throw new InvalidParameterValueException(String.format("Network ID: %s has conflicting firewall rules to provision Kubernetes cluster for API access", network.getUuid()));
                }
                if (startPort <= KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT && KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT + clusterTotalNodeCount <= endPort) {
                    throw new InvalidParameterValueException(String.format("Network ID: %s has conflicting firewall rules to provision Kubernetes cluster for node VM SSH access", network.getUuid()));
                }
            }
            rules = firewallRulesDao.listByIpAndPurposeAndNotRevoked(sourceNatIp.getId(), FirewallRule.Purpose.PortForwarding);
            for (FirewallRuleVO rule : rules) {
                Integer startPort = rule.getSourcePortStart();
                Integer endPort = rule.getSourcePortEnd();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Network rule : " + startPort + " " + endPort);
                }
                if (startPort <= KubernetesClusterActionWorker.CLUSTER_API_PORT && KubernetesClusterActionWorker.CLUSTER_API_PORT <= endPort) {
                    throw new InvalidParameterValueException(String.format("Network ID: %s has conflicting port forwarding rules to provision Kubernetes cluster for API access", network.getUuid()));
                }
                if (startPort <= KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT && KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT + clusterTotalNodeCount <= endPort) {
                    throw new InvalidParameterValueException(String.format("Network ID: %s has conflicting port forwarding rules to provision Kubernetes cluster for node VM SSH access", network.getUuid()));
                }
            }
        }
        return true;
    }

    private boolean validateNetwork(Network network, int clusterTotalNodeCount) {
        NetworkOffering networkOffering = networkOfferingDao.findById(network.getNetworkOfferingId());
        if (networkOffering.isSystemOnly()) {
            throw new InvalidParameterValueException(String.format("Network ID: %s is for system use only", network.getUuid()));
        }
        if (!networkModel.areServicesSupportedInNetwork(network.getId(), Service.UserData)) {
            throw new InvalidParameterValueException(String.format("Network ID: %s does not support userdata that is required for Kubernetes cluster", network.getUuid()));
        }
        if (!networkModel.areServicesSupportedInNetwork(network.getId(), Service.Firewall)) {
            throw new InvalidParameterValueException(String.format("Network ID: %s does not support firewall that is required for Kubernetes cluster", network.getUuid()));
        }
        if (!networkModel.areServicesSupportedInNetwork(network.getId(), Service.PortForwarding)) {
            throw new InvalidParameterValueException(String.format("Network ID: %s does not support port forwarding that is required for Kubernetes cluster", network.getUuid()));
        }
        if (!networkModel.areServicesSupportedInNetwork(network.getId(), Service.Dhcp)) {
            throw new InvalidParameterValueException(String.format("Network ID: %s does not support DHCP that is required for Kubernetes cluster", network.getUuid()));
        }
        validateIsolatedNetwork(network, clusterTotalNodeCount);
        return true;
    }

    private boolean validateServiceOffering(final ServiceOffering serviceOffering, final KubernetesSupportedVersion version) {
        if (serviceOffering.isDynamic()) {
            throw new InvalidParameterValueException(String.format("Custom service offerings are not supported for creating clusters, service offering ID: %s", serviceOffering.getUuid()));
        }
        if (serviceOffering.getCpu() < MIN_KUBERNETES_CLUSTER_NODE_CPU || serviceOffering.getRamSize() < MIN_KUBERNETES_CLUSTER_NODE_RAM_SIZE) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster cannot be created with service offering ID: %s, Kubernetes cluster template(CoreOS) needs minimum %d vCPUs and %d MB RAM", serviceOffering.getUuid(), MIN_KUBERNETES_CLUSTER_NODE_CPU, MIN_KUBERNETES_CLUSTER_NODE_RAM_SIZE));
        }
        if (serviceOffering.getCpu() < version.getMinimumCpu()) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster cannot be created with service offering ID: %s, Kubernetes version ID: %s needs minimum %d vCPUs", serviceOffering.getUuid(), version.getUuid(), version.getMinimumCpu()));
        }
        if (serviceOffering.getRamSize() < version.getMinimumRamSize()) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster cannot be created with service offering ID: %s, associated Kubernetes version ID: %s needs minimum %d MB RAM", serviceOffering.getUuid(), version.getUuid(), version.getMinimumRamSize()));
        }
        return true;
    }

    private void validateDockerRegistryParams(final String dockerRegistryUserName,
                                              final String dockerRegistryPassword,
                                              final String dockerRegistryUrl,
                                              final String dockerRegistryEmail) {
        // if no params related to docker registry specified then nothing to validate so return true
        if ((dockerRegistryUserName == null || dockerRegistryUserName.isEmpty()) &&
                (dockerRegistryPassword == null || dockerRegistryPassword.isEmpty()) &&
                (dockerRegistryUrl == null || dockerRegistryUrl.isEmpty()) &&
                (dockerRegistryEmail == null || dockerRegistryEmail.isEmpty())) {
            return;
        }

        // all params related to docker registry must be specified or nothing
        if (!((dockerRegistryUserName != null && !dockerRegistryUserName.isEmpty()) &&
                (dockerRegistryPassword != null && !dockerRegistryPassword.isEmpty()) &&
                (dockerRegistryUrl != null && !dockerRegistryUrl.isEmpty()) &&
                (dockerRegistryEmail != null && !dockerRegistryEmail.isEmpty()))) {
            throw new InvalidParameterValueException("All the docker private registry parameters (username, password, url, email) required are specified");
        }

        try {
            URL url = new URL(dockerRegistryUrl);
        } catch (MalformedURLException e) {
            throw new InvalidParameterValueException("Invalid docker registry url specified");
        }

        Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(dockerRegistryEmail);
        if (!matcher.find()) {
            throw new InvalidParameterValueException("Invalid docker registry email specified");
        }
    }

    private DeployDestination plan(final long nodesCount, final DataCenter zone, final ServiceOffering offering) throws InsufficientServerCapacityException {
        final int cpu_requested = offering.getCpu() * offering.getSpeed();
        final long ram_requested = offering.getRamSize() * 1024L * 1024L;
        List<HostVO> hosts = resourceManager.listAllHostsInOneZoneByType(Type.Routing, zone.getId());
        final Map<String, Pair<HostVO, Integer>> hosts_with_resevered_capacity = new ConcurrentHashMap<String, Pair<HostVO, Integer>>();
        for (HostVO h : hosts) {
            hosts_with_resevered_capacity.put(h.getUuid(), new Pair<HostVO, Integer>(h, 0));
        }
        boolean suitable_host_found = false;
        Cluster planCluster = null;
        for (int i = 1; i <= nodesCount; i++) {
            suitable_host_found = false;
            for (Map.Entry<String, Pair<HostVO, Integer>> hostEntry : hosts_with_resevered_capacity.entrySet()) {
                Pair<HostVO, Integer> hp = hostEntry.getValue();
                HostVO h = hp.first();
                hostDao.loadHostTags(h);
                if (!Strings.isNullOrEmpty(offering.getHostTag()) && !(h.getHostTags() != null && h.getHostTags().contains(offering.getHostTag()))) {
                    continue;
                }
                int reserved = hp.second();
                reserved++;
                ClusterVO cluster = clusterDao.findById(h.getClusterId());
                ClusterDetailsVO cluster_detail_cpu = clusterDetailsDao.findDetail(cluster.getId(), "cpuOvercommitRatio");
                ClusterDetailsVO cluster_detail_ram = clusterDetailsDao.findDetail(cluster.getId(), "memoryOvercommitRatio");
                Float cpuOvercommitRatio = Float.parseFloat(cluster_detail_cpu.getValue());
                Float memoryOvercommitRatio = Float.parseFloat(cluster_detail_ram.getValue());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("Checking host ID: %s for capacity already reserved %d", h.getUuid(), reserved));
                }
                if (capacityManager.checkIfHostHasCapacity(h.getId(), cpu_requested * reserved, ram_requested * reserved, false, cpuOvercommitRatio, memoryOvercommitRatio, true)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(String.format("Found host ID: %s for with enough capacity, CPU=%d RAM=%s", h.getUuid(), cpu_requested * reserved, toHumanReadableSize(ram_requested * reserved)));
                    }
                    hostEntry.setValue(new Pair<HostVO, Integer>(h, reserved));
                    suitable_host_found = true;
                    planCluster = cluster;
                    break;
                }
            }
            if (!suitable_host_found) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format("Suitable hosts not found in datacenter ID: %s for node %d with offering ID: %s", zone.getUuid(), i, offering.getUuid()));
                }
                break;
            }
        }
        if (suitable_host_found) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Suitable hosts found in datacenter ID: %s, creating deployment destination", zone.getUuid()));
            }
            return new DeployDestination(zone, null, planCluster, null);
        }
        String msg = String.format("Cannot find enough capacity for Kubernetes cluster(requested cpu=%d memory=%s) with offering ID: %s",
                cpu_requested * nodesCount, toHumanReadableSize(ram_requested * nodesCount), offering.getUuid());
        LOGGER.warn(msg);
        throw new InsufficientServerCapacityException(msg, DataCenter.class, zone.getId());
    }

    @Override
    public KubernetesClusterResponse createKubernetesClusterResponse(long kubernetesClusterId) {
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        KubernetesClusterResponse response = new KubernetesClusterResponse();
        response.setObjectName(KubernetesCluster.class.getSimpleName().toLowerCase());
        response.setId(kubernetesCluster.getUuid());
        response.setName(kubernetesCluster.getName());
        response.setDescription(kubernetesCluster.getDescription());
        DataCenterVO zone = ApiDBUtils.findZoneById(kubernetesCluster.getZoneId());
        response.setZoneId(zone.getUuid());
        response.setZoneName(zone.getName());
        response.setMasterNodes(kubernetesCluster.getControlNodeCount());
        response.setControlNodes(kubernetesCluster.getControlNodeCount());
        response.setClusterSize(kubernetesCluster.getNodeCount());
        VMTemplateVO template = ApiDBUtils.findTemplateById(kubernetesCluster.getTemplateId());
        response.setTemplateId(template.getUuid());
        ServiceOfferingVO offering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
        response.setServiceOfferingId(offering.getUuid());
        response.setServiceOfferingName(offering.getName());
        KubernetesSupportedVersionVO version = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId());
        if (version != null) {
            response.setKubernetesVersionId(version.getUuid());
            response.setKubernetesVersionName(version.getName());
        }
        Account account = ApiDBUtils.findAccountById(kubernetesCluster.getAccountId());
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            Project project = ApiDBUtils.findProjectByProjectAccountId(account.getId());
            response.setProjectId(project.getUuid());
            response.setProjectName(project.getName());
        } else {
            response.setAccountName(account.getAccountName());
        }
        Domain domain = ApiDBUtils.findDomainById(kubernetesCluster.getDomainId());
        response.setDomainId(domain.getUuid());
        response.setDomainName(domain.getName());
        response.setKeypair(kubernetesCluster.getKeyPair());
        response.setState(kubernetesCluster.getState().toString());
        response.setCores(String.valueOf(kubernetesCluster.getCores()));
        response.setMemory(String.valueOf(kubernetesCluster.getMemory()));
        NetworkVO ntwk = networkDao.findByIdIncludingRemoved(kubernetesCluster.getNetworkId());
        response.setEndpoint(kubernetesCluster.getEndpoint());
        response.setNetworkId(ntwk.getUuid());
        response.setAssociatedNetworkName(ntwk.getName());
        if (ntwk.getGuestType() == Network.GuestType.Isolated) {
            List<IPAddressVO> ipAddresses = ipAddressDao.listByAssociatedNetwork(ntwk.getId(), true);
            if (ipAddresses != null && ipAddresses.size() == 1) {
                response.setIpAddress(ipAddresses.get(0).getAddress().addr());
                response.setIpAddressId(ipAddresses.get(0).getUuid());
            }
        }
        List<UserVmResponse> vmResponses = new ArrayList<UserVmResponse>();
        List<KubernetesClusterVmMapVO> vmList = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());
        ResponseView respView = ResponseView.Restricted;
        Account caller = CallContext.current().getCallingAccount();
        if (accountService.isRootAdmin(caller.getId())) {
            respView = ResponseView.Full;
        }
        final String responseName = "virtualmachine";
        if (vmList != null && !vmList.isEmpty()) {
            for (KubernetesClusterVmMapVO vmMapVO : vmList) {
                UserVmJoinVO userVM = userVmJoinDao.findById(vmMapVO.getVmId());
                if (userVM != null) {
                    UserVmResponse vmResponse = ApiDBUtils.newUserVmResponse(respView, responseName, userVM,
                        EnumSet.of(VMDetails.nics), caller);
                    vmResponses.add(vmResponse);
                }
            }
        }
        response.setVirtualMachines(vmResponses);
        return response;
    }

    private void validateKubernetesClusterCreateParameters(final CreateKubernetesClusterCmd cmd) throws CloudRuntimeException {
        final String name = cmd.getName();
        final Long zoneId = cmd.getZoneId();
        final Long kubernetesVersionId = cmd.getKubernetesVersionId();
        final Long serviceOfferingId = cmd.getServiceOfferingId();
        final Account owner = accountService.getActiveAccountById(cmd.getEntityOwnerId());
        final Long networkId = cmd.getNetworkId();
        final String sshKeyPair = cmd.getSSHKeyPairName();
        final Long controlNodeCount = cmd.getControlNodes();
        final Long clusterSize = cmd.getClusterSize();
        final String dockerRegistryUserName = cmd.getDockerRegistryUserName();
        final String dockerRegistryPassword = cmd.getDockerRegistryPassword();
        final String dockerRegistryUrl = cmd.getDockerRegistryUrl();
        final String dockerRegistryEmail = cmd.getDockerRegistryEmail();
        final Long nodeRootDiskSize = cmd.getNodeRootDiskSize();
        final String externalLoadBalancerIpAddress = cmd.getExternalLoadBalancerIpAddress();

        if (name == null || name.isEmpty()) {
            throw new InvalidParameterValueException("Invalid name for the Kubernetes cluster name:" + name);
        }

        if (controlNodeCount < 1 || controlNodeCount > 100) {
            throw new InvalidParameterValueException("Invalid cluster control nodes count: " + controlNodeCount);
        }

        if (clusterSize < 1 || clusterSize > 100) {
            throw new InvalidParameterValueException("Invalid cluster size: " + clusterSize);
        }

        DataCenter zone = dataCenterDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by ID: " + zoneId);
        }

        if (Grouping.AllocationState.Disabled == zone.getAllocationState()) {
            throw new PermissionDeniedException(String.format("Cannot perform this operation, zone ID: %s is currently disabled", zone.getUuid()));
        }

        if (!isKubernetesServiceConfigured(zone)) {
            throw new CloudRuntimeException("Kubernetes service has not been configured properly to provision Kubernetes clusters");
        }

        final KubernetesSupportedVersion clusterKubernetesVersion = kubernetesSupportedVersionDao.findById(kubernetesVersionId);
        if (clusterKubernetesVersion == null) {
            throw new InvalidParameterValueException("Unable to find given Kubernetes version in supported versions");
        }
        if (!KubernetesSupportedVersion.State.Enabled.equals(clusterKubernetesVersion.getState())) {
            throw new InvalidParameterValueException(String.format("Kubernetes version ID: %s is in %s state", clusterKubernetesVersion.getUuid(), clusterKubernetesVersion.getState()));
        }
        if (clusterKubernetesVersion.getZoneId() != null && !clusterKubernetesVersion.getZoneId().equals(zone.getId())) {
            throw new InvalidParameterValueException(String.format("Kubernetes version ID: %s is not available for zone ID: %s", clusterKubernetesVersion.getUuid(), zone.getUuid()));
        }
        if (controlNodeCount > 1 ) {
            try {
                if (KubernetesVersionManagerImpl.compareSemanticVersions(clusterKubernetesVersion.getSemanticVersion(), MIN_KUBERNETES_VERSION_HA_SUPPORT) < 0) {
                    throw new InvalidParameterValueException(String.format("HA support is available only for Kubernetes version %s and above. Given version ID: %s is %s", MIN_KUBERNETES_VERSION_HA_SUPPORT, clusterKubernetesVersion.getUuid(), clusterKubernetesVersion.getSemanticVersion()));
                }
            } catch (IllegalArgumentException e) {
                logAndThrow(Level.WARN, String.format("Unable to compare Kubernetes version for given version ID: %s with %s", clusterKubernetesVersion.getUuid(), MIN_KUBERNETES_VERSION_HA_SUPPORT), e);
            }
        }

        if (clusterKubernetesVersion.getZoneId() != null && clusterKubernetesVersion.getZoneId() != zone.getId()) {
            throw new InvalidParameterValueException(String.format("Kubernetes version ID: %s is not available for zone ID: %s", clusterKubernetesVersion.getUuid(), zone.getUuid()));
        }

        VMTemplateVO iso = templateDao.findById(clusterKubernetesVersion.getIsoId());
        if (iso == null) {
            throw new InvalidParameterValueException(String.format("Invalid ISO associated with version ID: %s",  clusterKubernetesVersion.getUuid()));
        }
        if (CollectionUtils.isEmpty(templateJoinDao.newTemplateView(iso, zone.getId(), true))) {
            throw new InvalidParameterValueException(String.format("ISO associated with version ID: %s is not in Ready state for datacenter ID: %s",  clusterKubernetesVersion.getUuid(), zone.getUuid()));
        }

        ServiceOffering serviceOffering = serviceOfferingDao.findById(serviceOfferingId);
        if (serviceOffering == null) {
            throw new InvalidParameterValueException("No service offering with ID: " + serviceOfferingId);
        }

        if (sshKeyPair != null && !sshKeyPair.isEmpty()) {
            SSHKeyPairVO sshKeyPairVO = sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), sshKeyPair);
            if (sshKeyPairVO == null) {
                throw new InvalidParameterValueException(String.format("Given SSH key pair with name: %s was not found for the account %s", sshKeyPair, owner.getAccountName()));
            }
        }

        if (nodeRootDiskSize != null && nodeRootDiskSize <= 0) {
            throw new InvalidParameterValueException(String.format("Invalid value for %s", ApiConstants.NODE_ROOT_DISK_SIZE));
        }

        if (!validateServiceOffering(serviceOffering, clusterKubernetesVersion)) {
            throw new InvalidParameterValueException("Given service offering ID: %s is not suitable for Kubernetes cluster");
        }

        validateDockerRegistryParams(dockerRegistryUserName, dockerRegistryPassword, dockerRegistryUrl, dockerRegistryEmail);

        Network network = null;
        if (networkId != null) {
            network = networkService.getNetwork(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Unable to find network with given ID");
            }
        }

        if (!Strings.isNullOrEmpty(externalLoadBalancerIpAddress)) {
            if (!NetUtils.isValidIp4(externalLoadBalancerIpAddress) && !NetUtils.isValidIp6(externalLoadBalancerIpAddress)) {
                throw new InvalidParameterValueException("Invalid external load balancer IP address");
            }
            if (network == null) {
                throw new InvalidParameterValueException(String.format("%s parameter must be specified along with %s parameter", ApiConstants.EXTERNAL_LOAD_BALANCER_IP_ADDRESS, ApiConstants.NETWORK_ID));
            }
            if (Network.GuestType.Shared.equals(network.getGuestType())) {
                throw new InvalidParameterValueException(String.format("%s parameter must be specified along with %s type of network", ApiConstants.EXTERNAL_LOAD_BALANCER_IP_ADDRESS, Network.GuestType.Shared.toString()));
            }
        }

        if (!KubernetesClusterExperimentalFeaturesEnabled.value() && (!Strings.isNullOrEmpty(dockerRegistryUrl) ||
                !Strings.isNullOrEmpty(dockerRegistryUserName) || !Strings.isNullOrEmpty(dockerRegistryEmail) || !Strings.isNullOrEmpty(dockerRegistryPassword))) {
            throw new CloudRuntimeException(String.format("Private registry for the Kubernetes cluster is an experimental feature. Use %s configuration for enabling experimental features", KubernetesClusterExperimentalFeaturesEnabled.key()));
        }
    }

    private Network getKubernetesClusterNetworkIfMissing(final String clusterName, final DataCenter zone,  final Account owner, final int controlNodesCount,
                         final int nodesCount, final String externalLoadBalancerIpAddress, final Long networkId) throws CloudRuntimeException {
        Network network = null;
        if (networkId != null) {
            network = networkDao.findById(networkId);
            if (Network.GuestType.Isolated.equals(network.getGuestType())) {
                if (kubernetesClusterDao.listByNetworkId(network.getId()).isEmpty()) {
                    if (!validateNetwork(network, controlNodesCount + nodesCount)) {
                        throw new InvalidParameterValueException(String.format("Network ID: %s is not suitable for Kubernetes cluster", network.getUuid()));
                    }
                    networkModel.checkNetworkPermissions(owner, network);
                } else {
                    throw new InvalidParameterValueException(String.format("Network ID: %s is already under use by another Kubernetes cluster", network.getUuid()));
                }
            } else if (Network.GuestType.Shared.equals(network.getGuestType())) {
                if (controlNodesCount > 1 && Strings.isNullOrEmpty(externalLoadBalancerIpAddress)) {
                    throw new InvalidParameterValueException(String.format("Multi-control nodes, HA Kubernetes cluster with %s network ID: %s needs an external load balancer IP address. %s parameter can be used",
                            network.getGuestType().toString(), network.getUuid(), ApiConstants.EXTERNAL_LOAD_BALANCER_IP_ADDRESS));
                }
            }
        } else { // user has not specified network in which cluster VM's to be provisioned, so create a network for Kubernetes cluster
            NetworkOfferingVO networkOffering = networkOfferingDao.findByUniqueName(KubernetesClusterNetworkOffering.value());

            long physicalNetworkId = networkModel.findPhysicalNetworkId(zone.getId(), networkOffering.getTags(), networkOffering.getTrafficType());
            PhysicalNetwork physicalNetwork = physicalNetworkDao.findById(physicalNetworkId);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Creating network for account ID: %s from the network offering ID: %s as part of Kubernetes cluster: %s deployment process", owner.getUuid(), networkOffering.getUuid(), clusterName));
            }

            try {
                network = networkMgr.createGuestNetwork(networkOffering.getId(), clusterName + "-network", owner.getAccountName() + "-network",
                        null, null, null, false, null, owner, null, physicalNetwork, zone.getId(), ControlledEntity.ACLType.Account, null, null, null, null, true, null, null, null, null, null);
            } catch (ConcurrentOperationException | InsufficientCapacityException | ResourceAllocationException e) {
                logAndThrow(Level.ERROR, String.format("Unable to create network for the Kubernetes cluster: %s", clusterName));
            }
        }
        return network;
    }

    private void addKubernetesClusterDetails(final KubernetesCluster kubernetesCluster, final Network network, final CreateKubernetesClusterCmd cmd) {
        final String externalLoadBalancerIpAddress = cmd.getExternalLoadBalancerIpAddress();
        final String dockerRegistryUserName = cmd.getDockerRegistryUserName();
        final String dockerRegistryPassword = cmd.getDockerRegistryPassword();
        final String dockerRegistryUrl = cmd.getDockerRegistryUrl();
        final String dockerRegistryEmail = cmd.getDockerRegistryEmail();
        final boolean networkCleanup = cmd.getNetworkId() == null;
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                List<KubernetesClusterDetailsVO> details = new ArrayList<>();
                if (Network.GuestType.Shared.equals(network.getGuestType()) && !Strings.isNullOrEmpty(externalLoadBalancerIpAddress)) {
                    details.add(new KubernetesClusterDetailsVO(kubernetesCluster.getId(), ApiConstants.EXTERNAL_LOAD_BALANCER_IP_ADDRESS, externalLoadBalancerIpAddress, true));
                }
                if (!Strings.isNullOrEmpty(dockerRegistryUserName)) {
                    details.add(new KubernetesClusterDetailsVO(kubernetesCluster.getId(), ApiConstants.DOCKER_REGISTRY_USER_NAME, dockerRegistryUserName, true));
                }
                if (!Strings.isNullOrEmpty(dockerRegistryPassword)) {
                    details.add(new KubernetesClusterDetailsVO(kubernetesCluster.getId(), ApiConstants.DOCKER_REGISTRY_PASSWORD, dockerRegistryPassword, false));
                }
                if (!Strings.isNullOrEmpty(dockerRegistryUrl)) {
                    details.add(new KubernetesClusterDetailsVO(kubernetesCluster.getId(), ApiConstants.DOCKER_REGISTRY_URL, dockerRegistryUrl, true));
                }
                if (!Strings.isNullOrEmpty(dockerRegistryEmail)) {
                    details.add(new KubernetesClusterDetailsVO(kubernetesCluster.getId(), ApiConstants.DOCKER_REGISTRY_EMAIL, dockerRegistryEmail, true));
                }
                details.add(new KubernetesClusterDetailsVO(kubernetesCluster.getId(), ApiConstants.USERNAME, "admin", true));
                SecureRandom random = new SecureRandom();
                String randomPassword = new BigInteger(130, random).toString(32);
                details.add(new KubernetesClusterDetailsVO(kubernetesCluster.getId(), ApiConstants.PASSWORD, randomPassword, false));
                details.add(new KubernetesClusterDetailsVO(kubernetesCluster.getId(), "networkCleanup", String.valueOf(networkCleanup), true));
                kubernetesClusterDetailsDao.saveDetails(details);
            }
        });
    }

    private void validateKubernetesClusterScaleParameters(ScaleKubernetesClusterCmd cmd) {
        final Long kubernetesClusterId = cmd.getId();
        final Long serviceOfferingId = cmd.getServiceOfferingId();
        final Long clusterSize = cmd.getClusterSize();
        if (kubernetesClusterId == null || kubernetesClusterId < 1L) {
            throw new InvalidParameterValueException("Invalid Kubernetes cluster ID");
        }
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        if (kubernetesCluster == null || kubernetesCluster.getRemoved() != null) {
            throw new InvalidParameterValueException("Invalid Kubernetes cluster ID");
        }
        final DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
        if (zone == null) {
            logAndThrow(Level.WARN, String.format("Unable to find zone for Kubernetes cluster : %s", kubernetesCluster.getName()));
        }

        Account caller = CallContext.current().getCallingAccount();
        accountManager.checkAccess(caller, SecurityChecker.AccessType.OperateEntry, false, kubernetesCluster);

        if (serviceOfferingId == null && clusterSize == null) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster : %s cannot be scaled, either a new service offering or a new cluster size must be passed", kubernetesCluster.getName()));
        }

        final KubernetesSupportedVersion clusterVersion = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId());
        if (clusterVersion == null) {
            throw new CloudRuntimeException(String.format("Invalid Kubernetes version associated with Kubernetes cluster : %s", kubernetesCluster.getName()));
        }

        ServiceOffering serviceOffering = null;
        if (serviceOfferingId != null) {
            serviceOffering = serviceOfferingDao.findById(serviceOfferingId);
            if (serviceOffering == null) {
                throw new InvalidParameterValueException("Failed to find service offering ID: " + serviceOfferingId);
            } else {
                if (serviceOffering.isDynamic()) {
                    throw new InvalidParameterValueException(String.format("Custom service offerings are not supported for Kubernetes clusters. Kubernetes cluster : %s, service offering : %s", kubernetesCluster.getName(), serviceOffering.getName()));
                }
                if (serviceOffering.getCpu() < MIN_KUBERNETES_CLUSTER_NODE_CPU || serviceOffering.getRamSize() < MIN_KUBERNETES_CLUSTER_NODE_RAM_SIZE) {
                    throw new InvalidParameterValueException(String.format("Kubernetes cluster : %s cannot be scaled with service offering : %s, Kubernetes cluster template(CoreOS) needs minimum %d vCPUs and %d MB RAM",
                            kubernetesCluster.getName(), serviceOffering.getName(), MIN_KUBERNETES_CLUSTER_NODE_CPU, MIN_KUBERNETES_CLUSTER_NODE_RAM_SIZE));
                }
                if (serviceOffering.getCpu() < clusterVersion.getMinimumCpu()) {
                    throw new InvalidParameterValueException(String.format("Kubernetes cluster : %s cannot be scaled with service offering : %s, associated Kubernetes version : %s needs minimum %d vCPUs",
                            kubernetesCluster.getName(), serviceOffering.getName(), clusterVersion.getName(), clusterVersion.getMinimumCpu()));
                }
                if (serviceOffering.getRamSize() < clusterVersion.getMinimumRamSize()) {
                    throw new InvalidParameterValueException(String.format("Kubernetes cluster : %s cannot be scaled with service offering : %s, associated Kubernetes version : %s needs minimum %d MB RAM",
                            kubernetesCluster.getName(), serviceOffering.getName(), clusterVersion.getName(), clusterVersion.getMinimumRamSize()));
                }
            }
            final ServiceOffering existingServiceOffering = serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
            if (serviceOffering.getRamSize() < existingServiceOffering.getRamSize() ||
                    serviceOffering.getCpu() * serviceOffering.getSpeed() < existingServiceOffering.getCpu() * existingServiceOffering.getSpeed()) {
                logAndThrow(Level.WARN, String.format("Kubernetes cluster cannot be scaled down for service offering. Service offering : %s offers lesser resources as compared to service offering : %s of Kubernetes cluster : %s",
                        serviceOffering.getName(), existingServiceOffering.getName(), kubernetesCluster.getName()));
            }
        }

        if (!(kubernetesCluster.getState().equals(KubernetesCluster.State.Created) ||
                kubernetesCluster.getState().equals(KubernetesCluster.State.Running) ||
                kubernetesCluster.getState().equals(KubernetesCluster.State.Stopped))) {
            throw new PermissionDeniedException(String.format("Kubernetes cluster : %s is in %s state", kubernetesCluster.getName(), kubernetesCluster.getState().toString()));
        }

        if (clusterSize != null) {
            if (kubernetesCluster.getState().equals(KubernetesCluster.State.Stopped)) { // Cannot scale stopped cluster currently for cluster size
                throw new PermissionDeniedException(String.format("Kubernetes cluster : %s is in %s state", kubernetesCluster.getName(), kubernetesCluster.getState().toString()));
            }
            if (clusterSize < 1) {
                throw new InvalidParameterValueException(String.format("Kubernetes cluster : %s cannot be scaled for size, %d", kubernetesCluster.getName(), clusterSize));
            }
            if (clusterSize > kubernetesCluster.getNodeCount()) { // Upscale
                VMTemplateVO template = templateDao.findById(kubernetesCluster.getTemplateId());
                if (template == null) {
                    throw new InvalidParameterValueException(String.format("Invalid template associated with Kubernetes cluster : %s",  kubernetesCluster.getName()));
                }
                if (CollectionUtils.isEmpty(templateJoinDao.newTemplateView(template, zone.getId(), true))) {
                    throw new InvalidParameterValueException(String.format("Template : %s associated with Kubernetes cluster : %s is not in Ready state for datacenter : %s", template.getName(), kubernetesCluster.getName(), zone.getName()));
                }
            }
        }
    }

    private void validateKubernetesClusterUpgradeParameters(UpgradeKubernetesClusterCmd cmd) {
        // Validate parameters
        final Long kubernetesClusterId = cmd.getId();
        final Long upgradeVersionId = cmd.getKubernetesVersionId();
        if (kubernetesClusterId == null || kubernetesClusterId < 1L) {
            throw new InvalidParameterValueException("Invalid Kubernetes cluster ID");
        }
        if (upgradeVersionId == null || upgradeVersionId < 1L) {
            throw new InvalidParameterValueException("Invalid Kubernetes version ID");
        }
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        if (kubernetesCluster == null || kubernetesCluster.getRemoved() != null) {
            throw new InvalidParameterValueException("Invalid Kubernetes cluster ID");
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, false, kubernetesCluster);
        if (!KubernetesCluster.State.Running.equals(kubernetesCluster.getState())) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster : %s is not in running state", kubernetesCluster.getName()));
        }
        final DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
        if (zone == null) {
            logAndThrow(Level.WARN, String.format("Unable to find zone for Kubernetes cluster : %s", kubernetesCluster.getName()));
        }
        KubernetesSupportedVersionVO upgradeVersion = kubernetesSupportedVersionDao.findById(upgradeVersionId);
        if (upgradeVersion == null || upgradeVersion.getRemoved() != null) {
            throw new InvalidParameterValueException("Invalid Kubernetes version ID");
        }
        if (!KubernetesSupportedVersion.State.Enabled.equals(upgradeVersion.getState())) {
            throw new InvalidParameterValueException(String.format("Kubernetes version ID: %s for upgrade is in %s state", upgradeVersion.getUuid(), upgradeVersion.getState()));
        }
        KubernetesSupportedVersionVO clusterVersion = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId());
        if (clusterVersion == null || clusterVersion.getRemoved() != null) {
            throw new InvalidParameterValueException(String.format("Invalid Kubernetes version associated with cluster ID: %s",
                    kubernetesCluster.getUuid()));
        }
        final ServiceOffering serviceOffering = serviceOfferingDao.findByIdIncludingRemoved(kubernetesCluster.getServiceOfferingId());
        if (serviceOffering == null) {
            throw new CloudRuntimeException(String.format("Invalid service offering associated with Kubernetes cluster : %s", kubernetesCluster.getName()));
        }
        if (serviceOffering.getCpu() < upgradeVersion.getMinimumCpu()) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster : %s cannot be upgraded with Kubernetes version : %s which needs minimum %d vCPUs while associated service offering : %s offers only %d vCPUs",
                    kubernetesCluster.getName(), upgradeVersion.getName(), upgradeVersion.getMinimumCpu(), serviceOffering.getName(), serviceOffering.getCpu()));
        }
        if (serviceOffering.getRamSize() < upgradeVersion.getMinimumRamSize()) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster : %s cannot be upgraded with Kubernetes version : %s which needs minimum %d MB RAM while associated service offering : %s offers only %d MB RAM",
                    kubernetesCluster.getName(), upgradeVersion.getName(), upgradeVersion.getMinimumRamSize(), serviceOffering.getName(), serviceOffering.getRamSize()));
        }
        // Check upgradeVersion is either patch upgrade or immediate minor upgrade
        try {
            KubernetesVersionManagerImpl.canUpgradeKubernetesVersion(clusterVersion.getSemanticVersion(), upgradeVersion.getSemanticVersion());
        } catch (IllegalArgumentException e) {
            throw new InvalidParameterValueException(e.getMessage());
        }

        VMTemplateVO iso = templateDao.findById(upgradeVersion.getIsoId());
        if (iso == null) {
            throw new InvalidParameterValueException(String.format("Invalid ISO associated with version : %s",  upgradeVersion.getName()));
        }
        if (CollectionUtils.isEmpty(templateJoinDao.newTemplateView(iso, zone.getId(), true))) {
            throw new InvalidParameterValueException(String.format("ISO associated with version : %s is not in Ready state for datacenter : %s",  upgradeVersion.getName(), zone.getName()));
        }
    }

    protected boolean stateTransitTo(long kubernetesClusterId, KubernetesCluster.Event e) {
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        try {
            return _stateMachine.transitTo(kubernetesCluster, e, null, kubernetesClusterDao);
        } catch (NoTransitionException nte) {
            LOGGER.warn(String.format("Failed to transition state of the Kubernetes cluster : %s in state %s on event %s", kubernetesCluster.getName(), kubernetesCluster.getState().toString(), e.toString()), nte);
            return false;
        }
    }

    @Override
    public KubernetesCluster createKubernetesCluster(CreateKubernetesClusterCmd cmd) throws CloudRuntimeException {
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }

        validateKubernetesClusterCreateParameters(cmd);

        final DataCenter zone = dataCenterDao.findById(cmd.getZoneId());
        final long controlNodeCount = cmd.getControlNodes();
        final long clusterSize = cmd.getClusterSize();
        final long totalNodeCount = controlNodeCount + clusterSize;
        final ServiceOffering serviceOffering = serviceOfferingDao.findById(cmd.getServiceOfferingId());
        final Account owner = accountService.getActiveAccountById(cmd.getEntityOwnerId());
        final KubernetesSupportedVersion clusterKubernetesVersion = kubernetesSupportedVersionDao.findById(cmd.getKubernetesVersionId());

        DeployDestination deployDestination = null;
        try {
            deployDestination = plan(totalNodeCount, zone, serviceOffering);
        } catch (InsufficientCapacityException e) {
            logAndThrow(Level.ERROR, String.format("Creating Kubernetes cluster failed due to insufficient capacity for %d nodes cluster in zone : %s with service offering : %s", totalNodeCount, zone.getName(), serviceOffering.getName()));
        }
        if (deployDestination == null || deployDestination.getCluster() == null) {
            logAndThrow(Level.ERROR, String.format("Creating Kubernetes cluster failed due to error while finding suitable deployment plan for cluster in zone : %s", zone.getName()));
        }

        final Network defaultNetwork = getKubernetesClusterNetworkIfMissing(cmd.getName(), zone, owner, (int)controlNodeCount, (int)clusterSize, cmd.getExternalLoadBalancerIpAddress(), cmd.getNetworkId());
        final VMTemplateVO finalTemplate = getKubernetesServiceTemplate(deployDestination.getCluster().getHypervisorType());
        final long cores = serviceOffering.getCpu() * (controlNodeCount + clusterSize);
        final long memory = serviceOffering.getRamSize() * (controlNodeCount + clusterSize);

        final KubernetesClusterVO cluster = Transaction.execute(new TransactionCallback<KubernetesClusterVO>() {
            @Override
            public KubernetesClusterVO doInTransaction(TransactionStatus status) {
                KubernetesClusterVO newCluster = new KubernetesClusterVO(cmd.getName(), cmd.getDisplayName(), zone.getId(), clusterKubernetesVersion.getId(),
                        serviceOffering.getId(), finalTemplate.getId(), defaultNetwork.getId(), owner.getDomainId(),
                        owner.getAccountId(), controlNodeCount, clusterSize, KubernetesCluster.State.Created, cmd.getSSHKeyPairName(), cores, memory, cmd.getNodeRootDiskSize(), "");
                kubernetesClusterDao.persist(newCluster);
                return newCluster;
            }
        });

        addKubernetesClusterDetails(cluster, defaultNetwork, cmd);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Kubernetes cluster name: %s and ID: %s has been created", cluster.getName(), cluster.getUuid()));
        }
        return cluster;
    }

    /**
     * Start operation can be performed at two different life stages of Kubernetes cluster. First when a freshly created cluster
     * in which case there are no resources provisioned for the Kubernetes cluster. So during start all the resources
     * are provisioned from scratch. Second kind of start, happens on  Stopped Kubernetes cluster, in which all resources
     * are provisioned (like volumes, nics, networks etc). It just that VM's are not in running state. So just
     * start the VM's (which can possibly implicitly start the network also).
     * @param kubernetesClusterId
     * @param onCreate
     * @return
     * @throws CloudRuntimeException
     */

    @Override
    public boolean startKubernetesCluster(long kubernetesClusterId, boolean onCreate) throws CloudRuntimeException {
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }
        final KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        if (kubernetesCluster == null) {
            throw new InvalidParameterValueException("Failed to find Kubernetes cluster with given ID");
        }
        if (kubernetesCluster.getRemoved() != null) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster : %s is already deleted", kubernetesCluster.getName()));
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, false, kubernetesCluster);
        if (kubernetesCluster.getState().equals(KubernetesCluster.State.Running)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Kubernetes cluster : %s is in running state", kubernetesCluster.getName()));
            }
            return true;
        }
        if (kubernetesCluster.getState().equals(KubernetesCluster.State.Starting)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Kubernetes cluster : %s is already in starting state", kubernetesCluster.getName()));
            }
            return true;
        }
        final DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
        if (zone == null) {
            logAndThrow(Level.WARN, String.format("Unable to find zone for Kubernetes cluster : %s", kubernetesCluster.getName()));
        }
        KubernetesClusterStartWorker startWorker =
                new KubernetesClusterStartWorker(kubernetesCluster, this);
        startWorker = ComponentContext.inject(startWorker);
        if (onCreate) {
            // Start for Kubernetes cluster in 'Created' state
            return startWorker.startKubernetesClusterOnCreate();
        } else {
            // Start for Kubernetes cluster in 'Stopped' state. Resources are already provisioned, just need to be started
            return startWorker.startStoppedKubernetesCluster();
        }
    }

    @Override
    public boolean stopKubernetesCluster(long kubernetesClusterId) throws CloudRuntimeException {
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }
        final KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        if (kubernetesCluster == null) {
            throw new InvalidParameterValueException("Failed to find Kubernetes cluster with given ID");
        }
        if (kubernetesCluster.getRemoved() != null) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster : %s is already deleted", kubernetesCluster.getName()));
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, false, kubernetesCluster);
        if (kubernetesCluster.getState().equals(KubernetesCluster.State.Stopped)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Kubernetes cluster : %s is already stopped", kubernetesCluster.getName()));
            }
            return true;
        }
        if (kubernetesCluster.getState().equals(KubernetesCluster.State.Stopping)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Kubernetes cluster : %s is getting stopped", kubernetesCluster.getName()));
            }
            return true;
        }
        KubernetesClusterStopWorker stopWorker = new KubernetesClusterStopWorker(kubernetesCluster, this);
        stopWorker = ComponentContext.inject(stopWorker);
        return stopWorker.stop();
    }

    @Override
    public boolean deleteKubernetesCluster(Long kubernetesClusterId) throws CloudRuntimeException {
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }
        KubernetesClusterVO cluster = kubernetesClusterDao.findById(kubernetesClusterId);
        if (cluster == null) {
            throw new InvalidParameterValueException("Invalid cluster id specified");
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, false, cluster);
        KubernetesClusterDestroyWorker destroyWorker = new KubernetesClusterDestroyWorker(cluster, this);
        destroyWorker = ComponentContext.inject(destroyWorker);
        return destroyWorker.destroy();
    }

    @Override
    public ListResponse<KubernetesClusterResponse> listKubernetesClusters(ListKubernetesClustersCmd cmd) {
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }
        final CallContext ctx = CallContext.current();
        final Account caller = ctx.getCallingAccount();
        final Long clusterId = cmd.getId();
        final String state = cmd.getState();
        final String name = cmd.getName();
        final String keyword = cmd.getKeyword();
        List<KubernetesClusterResponse> responsesList = new ArrayList<KubernetesClusterResponse>();
        List<Long> permittedAccounts = new ArrayList<Long>();
        Ternary<Long, Boolean, Project.ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, Project.ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountManager.buildACLSearchParameters(caller, clusterId, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        Project.ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(KubernetesClusterVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<KubernetesClusterVO> sb = kubernetesClusterDao.createSearchBuilder();
        accountManager.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("keyword", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.IN);
        SearchCriteria<KubernetesClusterVO> sc = sb.create();
        accountManager.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        if (state != null) {
            sc.setParameters("state", state);
        }
        if(keyword != null){
            sc.setParameters("keyword", "%" + keyword + "%");
        }
        if (clusterId != null) {
            sc.setParameters("id", clusterId);
        }
        if (name != null) {
            sc.setParameters("name", name);
        }
        List<KubernetesClusterVO> kubernetesClusters = kubernetesClusterDao.search(sc, searchFilter);
        for (KubernetesClusterVO cluster : kubernetesClusters) {
            KubernetesClusterResponse clusterResponse = createKubernetesClusterResponse(cluster.getId());
            responsesList.add(clusterResponse);
        }
        ListResponse<KubernetesClusterResponse> response = new ListResponse<KubernetesClusterResponse>();
        response.setResponses(responsesList);
        return response;
    }

    public KubernetesClusterConfigResponse getKubernetesClusterConfig(GetKubernetesClusterConfigCmd cmd) {
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }
        final Long clusterId = cmd.getId();
        KubernetesCluster kubernetesCluster = kubernetesClusterDao.findById(clusterId);
        if (kubernetesCluster == null) {
            throw new InvalidParameterValueException("Invalid Kubernetes cluster ID specified");
        }
        KubernetesClusterConfigResponse response = new KubernetesClusterConfigResponse();
        response.setId(kubernetesCluster.getUuid());
        response.setName(kubernetesCluster.getName());
        String configData = "";
        KubernetesClusterDetailsVO clusterDetailsVO = kubernetesClusterDetailsDao.findDetail(kubernetesCluster.getId(), "kubeConfigData");
        if (clusterDetailsVO != null && !Strings.isNullOrEmpty(clusterDetailsVO.getValue())) {
            configData = new String(Base64.decodeBase64(clusterDetailsVO.getValue()));
        } else {
            String exceptionMessage = KubernetesCluster.State.Starting.equals(kubernetesCluster.getState()) ?
                    String.format("Setup is in progress for Kubernetes cluster : %s, config not available at this moment", kubernetesCluster.getName()) :
                    String.format("Config not found for Kubernetes cluster : %s", kubernetesCluster.getName());
            throw new CloudRuntimeException(exceptionMessage);
        }
        response.setConfigData(configData);
        response.setObjectName("clusterconfig");
        return response;
    }

    @Override
    public boolean scaleKubernetesCluster(ScaleKubernetesClusterCmd cmd) throws CloudRuntimeException {
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }
        validateKubernetesClusterScaleParameters(cmd);
        KubernetesClusterScaleWorker scaleWorker =
                new KubernetesClusterScaleWorker(kubernetesClusterDao.findById(cmd.getId()),
                        serviceOfferingDao.findById(cmd.getServiceOfferingId()), cmd.getClusterSize(), this);
        scaleWorker = ComponentContext.inject(scaleWorker);
        return scaleWorker.scaleCluster();
    }

    @Override
    public boolean upgradeKubernetesCluster(UpgradeKubernetesClusterCmd cmd) throws CloudRuntimeException {
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }
        validateKubernetesClusterUpgradeParameters(cmd);
        KubernetesClusterUpgradeWorker upgradeWorker =
                new KubernetesClusterUpgradeWorker(kubernetesClusterDao.findById(cmd.getId()),
                        kubernetesSupportedVersionDao.findById(cmd.getKubernetesVersionId()), this);
        upgradeWorker = ComponentContext.inject(upgradeWorker);
        return upgradeWorker.upgradeCluster();
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        if (!KubernetesServiceEnabled.value()) {
            return cmdList;
        }
        cmdList.add(CreateKubernetesClusterCmd.class);
        cmdList.add(StartKubernetesClusterCmd.class);
        cmdList.add(StopKubernetesClusterCmd.class);
        cmdList.add(DeleteKubernetesClusterCmd.class);
        cmdList.add(ListKubernetesClustersCmd.class);
        cmdList.add(GetKubernetesClusterConfigCmd.class);
        cmdList.add(ScaleKubernetesClusterCmd.class);
        cmdList.add(UpgradeKubernetesClusterCmd.class);
        return cmdList;
    }

    @Override
    public KubernetesCluster findById(final Long id) {
        return kubernetesClusterDao.findById(id);
    }

    // Garbage collector periodically run through the Kubernetes clusters marked for GC. For each Kubernetes cluster
    // marked for GC, attempt is made to destroy cluster.
    public class KubernetesClusterGarbageCollector extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            GlobalLock gcLock = GlobalLock.getInternLock("KubernetesCluster.GC.Lock");
            try {
                if (gcLock.lock(3)) {
                    try {
                        reallyRun();
                    } finally {
                        gcLock.unlock();
                    }
                }
            } finally {
                gcLock.releaseRef();
            }
        }

        public void reallyRun() {
            try {
                List<KubernetesClusterVO> kubernetesClusters = kubernetesClusterDao.findKubernetesClustersToGarbageCollect();
                for (KubernetesCluster kubernetesCluster : kubernetesClusters) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("Running Kubernetes cluster garbage collector on Kubernetes cluster : %s", kubernetesCluster.getName()));
                    }
                    try {
                        KubernetesClusterDestroyWorker destroyWorker = new KubernetesClusterDestroyWorker(kubernetesCluster, KubernetesClusterManagerImpl.this);
                        destroyWorker = ComponentContext.inject(destroyWorker);
                        if (destroyWorker.destroy()) {
                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info(String.format("Garbage collection complete for Kubernetes cluster : %s", kubernetesCluster.getName()));
                            }
                        } else {
                            LOGGER.warn(String.format("Garbage collection failed for Kubernetes cluster : %s, it will be attempted to garbage collected in next run", kubernetesCluster.getName()));
                        }
                    } catch (CloudRuntimeException e) {
                        LOGGER.warn(String.format("Failed to destroy Kubernetes cluster : %s during GC", kubernetesCluster.getName()), e);
                        // proceed further with rest of the Kubernetes cluster garbage collection
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Caught exception while running Kubernetes cluster gc: ", e);
            }
        }
    }

    /* Kubernetes cluster scanner checks if the Kubernetes cluster is in desired state. If it detects Kubernetes cluster
       is not in desired state, it will trigger an event and marks the Kubernetes cluster to be 'Alert' state. For e.g a
       Kubernetes cluster in 'Running' state should mean all the cluster of node VM's in the custer should be running and
       number of the node VM's should be of cluster size, and the control node VM's is running. It is possible due to
       out of band changes by user or hosts going down, we may end up one or more VM's in stopped state. in which case
       scanner detects these changes and marks the cluster in 'Alert' state. Similarly cluster in 'Stopped' state means
       all the cluster VM's are in stopped state any mismatch in states should get picked up by Kubernetes cluster and
       mark the Kubernetes cluster to be 'Alert' state. Through recovery API, or reconciliation clusters in 'Alert' will
       be brought back to known good state or desired state.
     */
    public class KubernetesClusterStatusScanner extends ManagedContextRunnable {
        private boolean firstRun = true;
        @Override
        protected void runInContext() {
            GlobalLock gcLock = GlobalLock.getInternLock("KubernetesCluster.State.Scanner.Lock");
            try {
                if (gcLock.lock(3)) {
                    try {
                        reallyRun();
                    } finally {
                        gcLock.unlock();
                    }
                }
            } finally {
                gcLock.releaseRef();
            }
        }

        public void reallyRun() {
            try {
                // run through Kubernetes clusters in 'Running' state and ensure all the VM's are Running in the cluster
                List<KubernetesClusterVO> runningKubernetesClusters = kubernetesClusterDao.findKubernetesClustersInState(KubernetesCluster.State.Running);
                for (KubernetesCluster kubernetesCluster : runningKubernetesClusters) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("Running Kubernetes cluster state scanner on Kubernetes cluster : %s", kubernetesCluster.getName()));
                    }
                    try {
                        if (!isClusterVMsInDesiredState(kubernetesCluster, VirtualMachine.State.Running)) {
                            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.FaultsDetected);
                        }
                    } catch (Exception e) {
                        LOGGER.warn(String.format("Failed to run Kubernetes cluster Running state scanner on Kubernetes cluster : %s status scanner", kubernetesCluster.getName()), e);
                    }
                }

                // run through Kubernetes clusters in 'Stopped' state and ensure all the VM's are Stopped in the cluster
                List<KubernetesClusterVO> stoppedKubernetesClusters = kubernetesClusterDao.findKubernetesClustersInState(KubernetesCluster.State.Stopped);
                for (KubernetesCluster kubernetesCluster : stoppedKubernetesClusters) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("Running Kubernetes cluster state scanner on Kubernetes cluster : %s for state: %s", kubernetesCluster.getName(), KubernetesCluster.State.Stopped.toString()));
                    }
                    try {
                        if (!isClusterVMsInDesiredState(kubernetesCluster, VirtualMachine.State.Stopped)) {
                            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.FaultsDetected);
                        }
                    } catch (Exception e) {
                        LOGGER.warn(String.format("Failed to run Kubernetes cluster Stopped state scanner on Kubernetes cluster : %s status scanner", kubernetesCluster.getName()), e);
                    }
                }

                // run through Kubernetes clusters in 'Alert' state and reconcile state as 'Running' if the VM's are running or 'Stopped' if VM's are stopped
                List<KubernetesClusterVO> alertKubernetesClusters = kubernetesClusterDao.findKubernetesClustersInState(KubernetesCluster.State.Alert);
                for (KubernetesClusterVO kubernetesCluster : alertKubernetesClusters) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("Running Kubernetes cluster state scanner on Kubernetes cluster : %s for state: %s", kubernetesCluster.getName(), KubernetesCluster.State.Alert.toString()));
                    }
                    try {
                        if (isClusterVMsInDesiredState(kubernetesCluster, VirtualMachine.State.Running)) {
                            KubernetesClusterStartWorker startWorker =
                                    new KubernetesClusterStartWorker(kubernetesCluster, KubernetesClusterManagerImpl.this);
                            startWorker = ComponentContext.inject(startWorker);
                            startWorker.reconcileAlertCluster();
                        } else if (isClusterVMsInDesiredState(kubernetesCluster, VirtualMachine.State.Stopped)) {
                            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.StopRequested);
                            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationSucceeded);
                        }
                    } catch (Exception e) {
                        LOGGER.warn(String.format("Failed to run Kubernetes cluster Alert state scanner on Kubernetes cluster : %s status scanner", kubernetesCluster.getName()), e);
                    }
                }


                if (firstRun) {
                    // run through Kubernetes clusters in 'Starting' state and reconcile state as 'Alert' or 'Error' if the VM's are running
                    List<KubernetesClusterVO> startingKubernetesClusters = kubernetesClusterDao.findKubernetesClustersInState(KubernetesCluster.State.Starting);
                    for (KubernetesCluster kubernetesCluster : startingKubernetesClusters) {
                        if ((new Date()).getTime() - kubernetesCluster.getCreated().getTime() < 10*60*1000) {
                            continue;
                        }
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info(String.format("Running Kubernetes cluster state scanner on Kubernetes cluster : %s for state: %s", kubernetesCluster.getName(), KubernetesCluster.State.Starting.toString()));
                        }
                        try {
                            if (isClusterVMsInDesiredState(kubernetesCluster, VirtualMachine.State.Running)) {
                                stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.FaultsDetected);
                            } else {
                                stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
                            }
                        } catch (Exception e) {
                            LOGGER.warn(String.format("Failed to run Kubernetes cluster Starting state scanner on Kubernetes cluster : %s status scanner", kubernetesCluster.getName()), e);
                        }
                    }
                    List<KubernetesClusterVO> destroyingKubernetesClusters = kubernetesClusterDao.findKubernetesClustersInState(KubernetesCluster.State.Destroying);
                    for (KubernetesCluster kubernetesCluster : destroyingKubernetesClusters) {
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info(String.format("Running Kubernetes cluster state scanner on Kubernetes cluster : %s for state: %s", kubernetesCluster.getName(), KubernetesCluster.State.Destroying.toString()));
                        }
                        try {
                            KubernetesClusterDestroyWorker destroyWorker = new KubernetesClusterDestroyWorker(kubernetesCluster, KubernetesClusterManagerImpl.this);
                            destroyWorker = ComponentContext.inject(destroyWorker);
                            destroyWorker.destroy();
                        } catch (Exception e) {
                            LOGGER.warn(String.format("Failed to run Kubernetes cluster Destroying state scanner on Kubernetes cluster : %s status scanner", kubernetesCluster.getName()), e);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Caught exception while running Kubernetes cluster state scanner", e);
            }
            firstRun = false;
        }
    }

    // checks if Kubernetes cluster is in desired state
    boolean isClusterVMsInDesiredState(KubernetesCluster kubernetesCluster, VirtualMachine.State state) {
        List<KubernetesClusterVmMapVO> clusterVMs = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());

        // check cluster is running at desired capacity include control nodes as well
        if (clusterVMs.size() < kubernetesCluster.getTotalNodeCount()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Found only %d VMs in the Kubernetes cluster ID: %s while expected %d VMs to be in state: %s",
                        clusterVMs.size(), kubernetesCluster.getUuid(), kubernetesCluster.getTotalNodeCount(), state.toString()));
            }
            return false;
        }
        // check if all the VM's are in same state
        for (KubernetesClusterVmMapVO clusterVm : clusterVMs) {
            VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(clusterVm.getVmId());
            if (vm.getState() != state) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("Found VM : %s in the Kubernetes cluster : %s in state: %s while expected to be in state: %s. So moving the cluster to Alert state for reconciliation",
                            vm.getUuid(), kubernetesCluster.getName(), vm.getState().toString(), state.toString()));
                }
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean start() {
        final Map<Network.Service, Network.Provider> defaultKubernetesServiceNetworkOfferingProviders = new HashMap<Service, Network.Provider>();
        defaultKubernetesServiceNetworkOfferingProviders.put(Service.Dhcp, Network.Provider.VirtualRouter);
        defaultKubernetesServiceNetworkOfferingProviders.put(Service.Dns, Network.Provider.VirtualRouter);
        defaultKubernetesServiceNetworkOfferingProviders.put(Service.UserData, Network.Provider.VirtualRouter);
        defaultKubernetesServiceNetworkOfferingProviders.put(Service.Firewall, Network.Provider.VirtualRouter);
        defaultKubernetesServiceNetworkOfferingProviders.put(Service.Gateway, Network.Provider.VirtualRouter);
        defaultKubernetesServiceNetworkOfferingProviders.put(Service.Lb, Network.Provider.VirtualRouter);
        defaultKubernetesServiceNetworkOfferingProviders.put(Service.SourceNat, Network.Provider.VirtualRouter);
        defaultKubernetesServiceNetworkOfferingProviders.put(Service.StaticNat, Network.Provider.VirtualRouter);
        defaultKubernetesServiceNetworkOfferingProviders.put(Service.PortForwarding, Network.Provider.VirtualRouter);
        defaultKubernetesServiceNetworkOfferingProviders.put(Service.Vpn, Network.Provider.VirtualRouter);

        NetworkOfferingVO defaultKubernetesServiceNetworkOffering =
                new NetworkOfferingVO(DEFAULT_NETWORK_OFFERING_FOR_KUBERNETES_SERVICE_NAME,
                        "Network Offering used for CloudStack Kubernetes service", Networks.TrafficType.Guest,
                        false, false, null, null, true,
                        NetworkOffering.Availability.Required, null, Network.GuestType.Isolated, true,
                        true, false, false, false, false,
                        false, false, false, true, true, false,
                        false, true, false, false);
        defaultKubernetesServiceNetworkOffering.setState(NetworkOffering.State.Enabled);
        defaultKubernetesServiceNetworkOffering = networkOfferingDao.persistDefaultNetworkOffering(defaultKubernetesServiceNetworkOffering);

        for (Service service : defaultKubernetesServiceNetworkOfferingProviders.keySet()) {
            NetworkOfferingServiceMapVO offService =
                    new NetworkOfferingServiceMapVO(defaultKubernetesServiceNetworkOffering.getId(), service,
                            defaultKubernetesServiceNetworkOfferingProviders.get(service));
            networkOfferingServiceMapDao.persist(offService);
            LOGGER.trace("Added service for the network offering: " + offService);
        }

        _gcExecutor.scheduleWithFixedDelay(new KubernetesClusterGarbageCollector(), 300, 300, TimeUnit.SECONDS);
        _stateScanner.scheduleWithFixedDelay(new KubernetesClusterStatusScanner(), 300, 30, TimeUnit.SECONDS);

        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        _configParams = params;
        _gcExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Kubernetes-Cluster-Scavenger"));
        _stateScanner = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Kubernetes-Cluster-State-Scanner"));

        return true;
    }

    @Override
    public String getConfigComponentName() {
        return KubernetesClusterService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                KubernetesServiceEnabled,
                KubernetesClusterHyperVTemplateName,
                KubernetesClusterKVMTemplateName,
                KubernetesClusterVMwareTemplateName,
                KubernetesClusterXenserverTemplateName,
                KubernetesClusterNetworkOffering,
                KubernetesClusterStartTimeout,
                KubernetesClusterScaleTimeout,
                KubernetesClusterUpgradeTimeout,
                KubernetesClusterExperimentalFeaturesEnabled
        };
    }
}
