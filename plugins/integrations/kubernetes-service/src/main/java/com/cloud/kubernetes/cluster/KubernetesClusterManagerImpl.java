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

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.CONTROL;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.DEFAULT;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.ETCD;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.WORKER;
import static com.cloud.utils.NumbersUtil.toHumanReadableSize;
import static com.cloud.vm.UserVmManager.AllowUserExpungeRecoverVm;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RolePermissionEntity;
import org.apache.cloudstack.acl.RoleService;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.Rule;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.affinity.AffinityGroupVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.annotation.AnnotationService;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.address.AssociateIPAddrCmd;
import org.apache.cloudstack.api.command.user.address.DisassociateIPAddrCmd;
import org.apache.cloudstack.api.command.user.address.ListPublicIpAddressesCmd;
import org.apache.cloudstack.api.command.user.firewall.CreateFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.firewall.DeleteFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.firewall.ListFirewallRulesCmd;
import org.apache.cloudstack.api.command.user.firewall.UpdateFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.job.QueryAsyncJobResultCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.AddNodesToKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.AddVirtualMachinesToKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.CreateKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.DeleteKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.GetKubernetesClusterConfigCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.ListKubernetesClustersCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.RemoveNodesFromKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.RemoveVirtualMachinesFromKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.ScaleKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.StartKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.StopKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.UpgradeKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.AssignToLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.CreateLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.DeleteLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLoadBalancerRuleInstancesCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLoadBalancerRulesCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.RemoveFromLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.UpdateLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.network.CreateNetworkACLCmd;
import org.apache.cloudstack.api.command.user.network.DeleteNetworkACLCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworkACLsCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworksCmd;
import org.apache.cloudstack.api.command.user.vm.ListVMsCmd;
import org.apache.cloudstack.api.response.KubernetesClusterConfigResponse;
import org.apache.cloudstack.api.response.KubernetesClusterResponse;
import org.apache.cloudstack.api.response.KubernetesUserVmResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.RemoveVirtualMachinesFromKubernetesClusterResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.network.RoutedIpv4Manager;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.dao.NetworkOfferingJoinDao;
import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.vo.NetworkOfferingJoinVO;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.bgp.BGPService;
import com.cloud.capacity.CapacityManager;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.event.ActionEvent;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType;
import com.cloud.kubernetes.cluster.actionworkers.KubernetesClusterActionWorker;
import com.cloud.kubernetes.cluster.actionworkers.KubernetesClusterAddWorker;
import com.cloud.kubernetes.cluster.actionworkers.KubernetesClusterDestroyWorker;
import com.cloud.kubernetes.cluster.actionworkers.KubernetesClusterRemoveWorker;
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
import com.cloud.network.dao.NsxProviderDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.element.NsxProviderVO;
import com.cloud.network.router.NetworkHelper;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.SecurityGroupService;
import com.cloud.network.security.SecurityRule;
import com.cloud.network.vpc.NetworkACL;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectAccount;
import com.cloud.projects.ProjectManager;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.template.TemplateApiService;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserDataVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserDataDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.UuidUtils;
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
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmService;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

public class KubernetesClusterManagerImpl extends ManagerBase implements KubernetesClusterService {

    private static final String DEFAULT_NETWORK_OFFERING_FOR_KUBERNETES_SERVICE_NAME = "DefaultNetworkOfferingforKubernetesService";
    private static final List<Class<?>> PROJECT_KUBERNETES_ACCOUNT_ROLE_ALLOWED_APIS = Arrays.asList(
            QueryAsyncJobResultCmd.class,
            ListVMsCmd.class,
            ListNetworksCmd.class,
            ListPublicIpAddressesCmd.class,
            AssociateIPAddrCmd.class,
            DisassociateIPAddrCmd.class,
            ListLoadBalancerRulesCmd.class,
            CreateLoadBalancerRuleCmd.class,
            UpdateLoadBalancerRuleCmd.class,
            DeleteLoadBalancerRuleCmd.class,
            AssignToLoadBalancerRuleCmd.class,
            RemoveFromLoadBalancerRuleCmd.class,
            ListLoadBalancerRuleInstancesCmd.class,
            ListFirewallRulesCmd.class,
            CreateFirewallRuleCmd.class,
            UpdateFirewallRuleCmd.class,
            DeleteFirewallRuleCmd.class,
            ListNetworkACLsCmd.class,
            CreateNetworkACLCmd.class,
            DeleteNetworkACLCmd.class,
            ListKubernetesClustersCmd.class,
            ScaleKubernetesClusterCmd.class
    );
    private static final String PROJECT_KUBERNETES_ACCOUNT_FIRST_NAME = "Kubernetes";
    private static final String PROJECT_KUBERNETES_ACCOUNT_LAST_NAME = "Service User";
    private static final String DEFAULT_NETWORK_OFFERING_FOR_KUBERNETES_SERVICE_DISPLAY_TEXT = "Network Offering used for CloudStack Kubernetes service";
    private static final String DEFAULT_NSX_NETWORK_OFFERING_FOR_KUBERNETES_SERVICE_NAME = "DefaultNSXNetworkOfferingforKubernetesService";
    private static final String DEFAULT_NSX_VPC_TIER_NETWORK_OFFERING_FOR_KUBERNETES_SERVICE_NAME = "DefaultNSXVPCNetworkOfferingforKubernetesService";
    private static final String DEFAULT_NSX_NETWORK_OFFERING_FOR_KUBERNETES_SERVICE_DISPLAY_TEXT = "Network Offering for NSX CloudStack Kubernetes Service";
    private static final String DEFAULT_NSX_VPC_NETWORK_OFFERING_FOR_KUBERNETES_SERVICE_DISPLAY_TEXT = "Network Offering for NSX CloudStack Kubernetes service on VPC";

    protected StateMachine2<KubernetesCluster.State, KubernetesCluster.Event, KubernetesCluster> _stateMachine = KubernetesCluster.State.getStateMachine();

    protected final static List<String> CLUSTER_NODES_TYPES_LIST = Arrays.asList(WORKER.name(), CONTROL.name(), ETCD.name());

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
    protected AffinityGroupDao affinityGroupDao;
    @Inject
    protected ServiceOfferingDao serviceOfferingDao;
    @Inject
    protected UserDataDao userDataDao;
    @Inject
    protected VMTemplateDao templateDao;
    @Inject
    protected TemplateJoinDao templateJoinDao;
    @Inject
    protected DedicatedResourceDao dedicatedResourceDao;
    @Inject
    protected AccountDao accountDao;
    @Inject
    protected AccountService accountService;
    @Inject
    protected AccountManager accountManager;
    @Inject
    protected UserDao userDao;
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
    @Inject
    private AnnotationDao annotationDao;
    @Inject
    private SecurityGroupManager securityGroupManager;
    @Inject
    public SecurityGroupService securityGroupService;
    @Inject
    public NetworkHelper networkHelper;
    @Inject
    private NsxProviderDao nsxProviderDao;
    @Inject
    private NicDao nicDao;
    @Inject
    private UserVmService userVmService;
    @Inject
    private TemplateApiService templateService;
    @Inject
    private PortForwardingRulesDao pfRuleDao;
    @Inject
    RoutedIpv4Manager routedIpv4Manager;
    @Inject
    private BGPService bgpService;
    @Inject
    public ProjectManager projectManager;
    @Inject
    RoleService roleService;

    private void logMessage(final Level logLevel, final String message, final Exception e) {
        if (logLevel == Level.WARN) {
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

    private boolean isKubernetesServiceNetworkOfferingConfigured(DataCenter zone, Long networkId) {
        // Check network offering
        String networkOfferingName = KubernetesClusterNetworkOffering.value();
        if (StringUtils.isEmpty(networkOfferingName) && networkId == null) {
            logger.warn("Global setting: {} is empty. Admin has not yet specified the network offering to be used for provisioning isolated network for the cluster nor has a pre-created network been passed", KubernetesClusterNetworkOffering.key());
            return false;
        }
        NetworkOfferingVO networkOffering = null;
        if (networkId != null) {
            NetworkVO network = networkDao.findById(networkId);
            if (network == null) {
                logger.warn("Unable to find the network with ID: {} passed for the Kubernetes cluster", networkId);
                return false;
            }
            networkOffering = networkOfferingDao.findById(network.getNetworkOfferingId());
            if (networkOffering == null) {
                logger.warn("Unable to find the network offering of the network: {} ({}) to be used for provisioning Kubernetes cluster", network.getName(), network.getUuid());
                return false;
            }
        } else if (StringUtils.isNotEmpty(networkOfferingName)) {
            networkOffering = networkOfferingDao.findByUniqueName(networkOfferingName);
            if (networkOffering == null) {
                logger.warn("Unable to find the network offering: {} to be used for provisioning Kubernetes cluster", networkOfferingName);
                return false;
            }
        }

        if (networkOffering.getState() == NetworkOffering.State.Disabled) {
            logger.warn("Network offering: {} is not enabled", networkOffering);
            return false;
        }
        List<String> services = networkOfferingServiceMapDao.listServicesForNetworkOffering(networkOffering.getId());
        if (services == null || services.isEmpty() || !services.contains("SourceNat")) {
            logger.warn("Network offering: {} does not have necessary services to provision Kubernetes cluster", networkOffering);
            return false;
        }
        if (!networkOffering.isEgressDefaultPolicy()) {
            logger.warn("Network offering: {} has egress default policy turned off should be on to provision Kubernetes cluster", networkOffering);
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
            logger.warn("Network offering: {} is not available for zone: {}", networkOffering, zone);
            return false;
        }
        long physicalNetworkId = networkModel.findPhysicalNetworkId(zone.getId(), networkOffering.getTags(), networkOffering.getTrafficType());
        PhysicalNetwork physicalNetwork = physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            logger.warn(String.format("Unable to find physical network with tag: %s", networkOffering.getTags()));
            return false;
        }
        return true;
    }

    private boolean isKubernetesServiceConfigured(DataCenter zone, Long networkId) {
        if (!isKubernetesServiceNetworkOfferingConfigured(zone, networkId)) {
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

    public VMTemplateVO getKubernetesServiceTemplate(DataCenter dataCenter, Hypervisor.HypervisorType hypervisorType, Map<String, Long> templateNodeTypeMap, KubernetesClusterNodeType nodeType,
                                                     KubernetesSupportedVersion clusterKubernetesVersion) {
        String systemVMPreferredArchitecture = ResourceManager.SystemVmPreferredArchitecture.valueIn(dataCenter.getId());
        VMTemplateVO cksIso = clusterKubernetesVersion != null ?
                templateDao.findById(clusterKubernetesVersion.getIsoId()) :
                null;
        String preferredArchitecture = getCksClusterPreferredArch(systemVMPreferredArchitecture, cksIso);
        VMTemplateVO template = templateDao.findSystemVMReadyTemplate(dataCenter.getId(), hypervisorType, preferredArchitecture);
        if (DataCenter.Type.Edge.equals(dataCenter.getType()) && template != null && !template.isDirectDownload()) {
            logger.debug(String.format("Template %s can not be used for edge zone %s", template, dataCenter));
            template = templateDao.findRoutingTemplate(hypervisorType, networkHelper.getHypervisorRouterTemplateConfigMap().get(hypervisorType).valueIn(dataCenter.getId()));
        }
        switch (nodeType) {
            case CONTROL:
            case ETCD:
            case WORKER:
                VMTemplateVO nodeTemplate = Objects.nonNull(templateNodeTypeMap) ? templateDao.findById(templateNodeTypeMap.getOrDefault(nodeType.name(), 0L)) : template;
                template = Objects.nonNull(nodeTemplate) ? nodeTemplate : template;
                if (Objects.isNull(template)) {
                    throwDefaultCksTemplateNotFound(dataCenter.getUuid());
                }
                return template;
            default:
                if (Objects.isNull(template)) {
                    throwDefaultCksTemplateNotFound(dataCenter.getUuid());
                }
                return template;
        }
    }

    public void throwDefaultCksTemplateNotFound(String datacenterId) {
        throw new CloudRuntimeException("Not able to find the System or Routing template in ready state for the zone " + datacenterId);
    }

    protected String getCksClusterPreferredArch(String systemVMPreferredArchitecture, VMTemplateVO cksIso) {
        if (cksIso == null) {
            return systemVMPreferredArchitecture;
        }
        String cksIsoArchName = cksIso.getArch().name();
        return cksIsoArchName.equals(systemVMPreferredArchitecture) ? systemVMPreferredArchitecture : cksIsoArchName;
    }

    protected void validateIsolatedNetworkIpRules(long ipId, FirewallRule.Purpose purpose, Network network, int clusterTotalNodeCount) {
        List<FirewallRuleVO> rules = firewallRulesDao.listByIpPurposeProtocolAndNotRevoked(ipId, purpose, NetUtils.TCP_PROTO);
        for (FirewallRuleVO rule : rules) {
            int startPort = ObjectUtils.defaultIfNull(rule.getSourcePortStart(), 1);
            int endPort = ObjectUtils.defaultIfNull(rule.getSourcePortEnd(), NetUtils.PORT_RANGE_MAX);
            if (logger.isDebugEnabled()) {
                logger.debug("Validating rule with purpose: {} for network: {} with ports: {}-{}", purpose.toString(), network, startPort, endPort);
            }
            if (startPort <= KubernetesClusterActionWorker.CLUSTER_API_PORT && KubernetesClusterActionWorker.CLUSTER_API_PORT <= endPort) {
                throw new InvalidParameterValueException(String.format("Network: %s has conflicting %s rules to provision Kubernetes cluster for API access", network, purpose.toString().toLowerCase()));
            }
            int expectedSshStart = KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT;
            int expectedSshEnd = expectedSshStart + clusterTotalNodeCount - 1;
            if (Math.max(expectedSshStart, startPort) <= Math.min(expectedSshEnd, endPort)) {
                throw new InvalidParameterValueException(String.format("Network: %s has conflicting %s rules to provision Kubernetes cluster for node VM SSH access", network, purpose.toString().toLowerCase()));
            }
        }
    }

    private void validateIsolatedNetwork(Network network, int clusterTotalNodeCount) {
        if (!Network.GuestType.Isolated.equals(network.getGuestType())) {
            return;
        }
        if (Network.State.Allocated.equals(network.getState())) { // Allocated networks won't have IP and rules
            return;
        }
        IpAddress sourceNatIp = getSourceNatIp(network);
        if (sourceNatIp == null) {
            throw new InvalidParameterValueException(String.format("Network ID: %s does not have a source NAT IP associated with it. To provision a Kubernetes Cluster, source NAT IP is required", network.getUuid()));
        }
        validateIsolatedNetworkIpRules(sourceNatIp.getId(), FirewallRule.Purpose.Firewall, network, clusterTotalNodeCount);
        validateIsolatedNetworkIpRules(sourceNatIp.getId(), FirewallRule.Purpose.PortForwarding, network, clusterTotalNodeCount);
    }

    protected void validateVpcTier(Network network) {
        if (Network.State.Allocated.equals(network.getState())) { // Allocated networks won't have IP and rules
            return;
        }
        if (network.getNetworkACLId() == NetworkACL.DEFAULT_DENY) {
            throw new InvalidParameterValueException(String.format("Network ID: %s can not be used for Kubernetes cluster as it uses default deny ACL", network.getUuid()));
        }
    }

    private void validateNetwork(Network network, int clusterTotalNodeCount) {
        NetworkOffering networkOffering = networkOfferingDao.findById(network.getNetworkOfferingId());
        if (networkOffering.isSystemOnly()) {
            throw new InvalidParameterValueException(String.format("Network ID: %s is for system use only", network.getUuid()));
        }
        if (!networkModel.areServicesSupportedInNetwork(network.getId(), Service.UserData)) {
            throw new InvalidParameterValueException(String.format("Network ID: %s does not support userdata that is required for Kubernetes cluster", network.getUuid()));
        }
        if (!networkModel.areServicesSupportedInNetwork(network.getId(), Service.Dhcp)) {
            throw new InvalidParameterValueException(String.format("Network ID: %s does not support DHCP that is required for Kubernetes cluster", network.getUuid()));
        }
        if (routedIpv4Manager.isRoutedNetwork(network)) {
            logger.debug("No need to add firewall and port forwarding rules for ROUTED network. Assume the VMs are reachable.");
            return;
        }
        Long vpcId = network.getVpcId();
        if (vpcId == null && !networkModel.areServicesSupportedInNetwork(network.getId(), Service.Firewall)) {
            throw new InvalidParameterValueException(String.format("Network ID: %s does not support firewall that is required for Kubernetes cluster", network.getUuid()));
        }
        if (!networkModel.areServicesSupportedInNetwork(network.getId(), Service.PortForwarding)) {
            throw new InvalidParameterValueException(String.format("Network ID: %s does not support port forwarding that is required for Kubernetes cluster", network.getUuid()));
        }
        if (network.getVpcId() != null) {
            validateVpcTier(network);
            return;
        }
        validateIsolatedNetwork(network, clusterTotalNodeCount);
    }

    protected void validateServiceOffering(final ServiceOffering serviceOffering, final KubernetesSupportedVersion version) throws InvalidParameterValueException {
        if (serviceOffering.isDynamic()) {
            throw new InvalidParameterValueException(String.format("Custom service offerings are not supported for creating clusters, service offering ID: %s", serviceOffering.getUuid()));
        }
        if (serviceOffering.getCpu() < MIN_KUBERNETES_CLUSTER_NODE_CPU || serviceOffering.getRamSize() < MIN_KUBERNETES_CLUSTER_NODE_RAM_SIZE) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster cannot be created with service offering ID: %s, Kubernetes cluster template needs minimum %d vCPUs and %d MB RAM", serviceOffering.getUuid(), MIN_KUBERNETES_CLUSTER_NODE_CPU, MIN_KUBERNETES_CLUSTER_NODE_RAM_SIZE));
        }
        if (serviceOffering.getCpu() < version.getMinimumCpu()) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster cannot be created with service offering ID: %s, Kubernetes version ID: %s needs minimum %d vCPUs", serviceOffering.getUuid(), version.getUuid(), version.getMinimumCpu()));
        }
        if (serviceOffering.getRamSize() < version.getMinimumRamSize()) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster cannot be created with service offering ID: %s, associated Kubernetes version ID: %s needs minimum %d MB RAM", serviceOffering.getUuid(), version.getUuid(), version.getMinimumRamSize()));
        }
    }

    private void validateDockerRegistryParams(final String dockerRegistryUserName,
                                              final String dockerRegistryPassword,
                                              final String dockerRegistryUrl) {
        // if no params related to docker registry specified then nothing to validate so return true
        if ((dockerRegistryUserName == null || dockerRegistryUserName.isEmpty()) &&
                (dockerRegistryPassword == null || dockerRegistryPassword.isEmpty()) &&
                (dockerRegistryUrl == null || dockerRegistryUrl.isEmpty())) {
            return;
        }

        // all params related to docker registry must be specified or nothing
        if (!((dockerRegistryUserName != null && !dockerRegistryUserName.isEmpty()) &&
                (dockerRegistryPassword != null && !dockerRegistryPassword.isEmpty()) &&
                (dockerRegistryUrl != null && !dockerRegistryUrl.isEmpty()))) {

            throw new InvalidParameterValueException("All the docker private registry parameters (username, password, url) required are specified");
        }

        try {
            URL url = new URL(dockerRegistryUrl);
        } catch (MalformedURLException e) {
            throw new InvalidParameterValueException("Invalid docker registry url specified");
        }
    }

    public Long getExplicitAffinityGroup(Long domainId) {
        AffinityGroupVO groupVO = affinityGroupDao.findDomainLevelGroupByType(domainId, "ExplicitDedication");
        if (Objects.nonNull(groupVO)) {
            return groupVO.getId();
        }
        return null;
    }

    private DeployDestination plan(final long nodesCount, final DataCenter zone, final ServiceOffering offering,
                                   final Long domainId, final Long accountId, Hypervisor.HypervisorType hypervisorType) throws InsufficientServerCapacityException {
        final int cpu_requested = offering.getCpu() * offering.getSpeed();
        final long ram_requested = offering.getRamSize() * 1024L * 1024L;
        boolean useDedicatedHosts = false;
        Long group = getExplicitAffinityGroup(domainId);
        List<HostVO> hosts = new ArrayList<>();
        if (Objects.nonNull(group)) {
            List<DedicatedResourceVO> dedicatedHosts = new ArrayList<>();
            if (Objects.nonNull(accountId)) {
                dedicatedHosts = dedicatedResourceDao.listByAccountId(accountId);
            } else if (Objects.nonNull(domainId)) {
                dedicatedHosts = dedicatedResourceDao.listByDomainId(domainId);
            }
            for (DedicatedResourceVO dedicatedHost : dedicatedHosts) {
                hosts.add(hostDao.findById(dedicatedHost.getHostId()));
                useDedicatedHosts = true;
            }
        }
        if (hosts.isEmpty()) {
            hosts = resourceManager.listAllHostsInOneZoneByType(Host.Type.Routing, zone.getId());
        }
        if (hypervisorType != null) {
            hosts = hosts.stream().filter(x -> x.getHypervisorType() == hypervisorType).collect(Collectors.toList());
        }
        final Map<String, Pair<HostVO, Integer>> hosts_with_resevered_capacity = new ConcurrentHashMap<String, Pair<HostVO, Integer>>();
        for (HostVO h : hosts) {
            hosts_with_resevered_capacity.put(h.getUuid(), new Pair<HostVO, Integer>(h, 0));
        }
        boolean suitable_host_found = false;
        Cluster planCluster = null;
        HostVO suitableHost = null;
        for (int i = 1; i <= nodesCount; i++) {
            suitable_host_found = false;
            for (Map.Entry<String, Pair<HostVO, Integer>> hostEntry : hosts_with_resevered_capacity.entrySet()) {
                Pair<HostVO, Integer> hp = hostEntry.getValue();
                HostVO hostVO = hp.first();
                hostDao.loadHostTags(hostVO);
                if (!hostVO.checkHostServiceOfferingTags(offering)) {
                    continue;
                }
                int reserved = hp.second();
                reserved++;
                ClusterVO cluster = clusterDao.findById(hostVO.getClusterId());
                ClusterDetailsVO cluster_detail_cpu = clusterDetailsDao.findDetail(cluster.getId(), "cpuOvercommitRatio");
                ClusterDetailsVO cluster_detail_ram = clusterDetailsDao.findDetail(cluster.getId(), "memoryOvercommitRatio");
                Float cpuOvercommitRatio = Float.parseFloat(cluster_detail_cpu.getValue());
                Float memoryOvercommitRatio = Float.parseFloat(cluster_detail_ram.getValue());
                if (logger.isDebugEnabled()) {
                    logger.debug("Checking host: {} for capacity already reserved {}", hostVO, reserved);
                }
                if (capacityManager.checkIfHostHasCapacity(hostVO, cpu_requested * reserved, ram_requested * reserved, false, cpuOvercommitRatio, memoryOvercommitRatio, true)) {
                    logger.debug("Found host {} to have enough capacity, CPU={} RAM={}", hostVO, cpu_requested * reserved, toHumanReadableSize(ram_requested * reserved));
                    hostEntry.setValue(new Pair<HostVO, Integer>(hostVO, reserved));
                    suitable_host_found = true;
                    suitableHost = hostVO;
                    planCluster = cluster;
                    break;
                }
            }
            if (!suitable_host_found) {
                if (logger.isInfoEnabled()) {
                    logger.info("Suitable hosts not found in datacenter: {} for node {} with offering: {}", zone, i, offering);
                }
                break;
            }
        }
        if (suitable_host_found) {
            if (logger.isInfoEnabled()) {
                logger.info("Suitable hosts found in datacenter: {}, creating deployment destination", zone);
            }
            if (useDedicatedHosts) {
                planCluster = clusterDao.findById(suitableHost.getClusterId());
                return new DeployDestination(zone, null, planCluster, suitableHost);
            }
            return new DeployDestination(zone, null, planCluster, null);
        }
        String msg = String.format("Cannot find enough capacity for Kubernetes cluster(requested cpu=%d memory=%s) with offering: %s",
                cpu_requested * nodesCount, toHumanReadableSize(ram_requested * nodesCount), offering);
        logger.warn(msg);
        throw new InsufficientServerCapacityException(msg, DataCenter.class, zone.getId());
    }

    protected void setNodeTypeServiceOfferingResponse(KubernetesClusterResponse response,
                                                      KubernetesClusterNodeType nodeType,
                                                      Long offeringId) {
        if (offeringId == null) {
            return;
        }
        ServiceOfferingVO offering = serviceOfferingDao.findById(offeringId);
        if (offering != null) {
            setServiceOfferingResponseForNodeType(response, offering, nodeType);
        }
    }

    protected void setServiceOfferingResponseForNodeType(KubernetesClusterResponse response,
                                                         ServiceOfferingVO offering,
                                                         KubernetesClusterNodeType nodeType) {
        if (CONTROL == nodeType) {
            response.setControlOfferingId(offering.getUuid());
            response.setControlOfferingName(offering.getName());
        } else if (WORKER == nodeType) {
            response.setWorkerOfferingId(offering.getUuid());
            response.setWorkerOfferingName(offering.getName());
        } else if (ETCD == nodeType) {
            response.setEtcdOfferingId(offering.getUuid());
            response.setEtcdOfferingName(offering.getName());
        }
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
        if (template != null) {
            response.setTemplateId(template.getUuid());
        }
        ServiceOfferingVO offering = serviceOfferingDao.findByIdIncludingRemoved(kubernetesCluster.getServiceOfferingId());
        if (offering != null) {
            response.setServiceOfferingId(offering.getUuid());
            response.setServiceOfferingName(offering.getName());
        }

        Long cniConfigId = kubernetesCluster.getCniConfigId();
        if (Objects.nonNull(cniConfigId)) {
            UserDataVO cniConfig = userDataDao.findById(cniConfigId);
            response.setCniConfigId(cniConfig.getUuid());
            response.setCniConfigName(cniConfig.getName());
        }
        setNodeTypeServiceOfferingResponse(response, WORKER, kubernetesCluster.getWorkerNodeServiceOfferingId());
        setNodeTypeServiceOfferingResponse(response, CONTROL, kubernetesCluster.getControlNodeServiceOfferingId());
        setNodeTypeServiceOfferingResponse(response, ETCD, kubernetesCluster.getEtcdNodeServiceOfferingId());

        if (kubernetesCluster.getEtcdNodeCount() != null) {
            response.setEtcdNodes(kubernetesCluster.getEtcdNodeCount());
        }
        KubernetesSupportedVersionVO version = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId());
        if (version != null) {
            response.setKubernetesVersionId(version.getUuid());
            response.setKubernetesVersionName(version.getName());
        }
        Account account = ApiDBUtils.findAccountById(kubernetesCluster.getAccountId());
        if (account.getType() == Account.Type.PROJECT) {
            Project project = ApiDBUtils.findProjectByProjectAccountId(account.getId());
            response.setProjectId(project.getUuid());
            response.setProjectName(project.getName());
        } else {
            response.setAccountName(account.getAccountName());
        }
        Domain domain = ApiDBUtils.findDomainById(kubernetesCluster.getDomainId());
        response.setDomainId(domain.getUuid());
        response.setDomainName(domain.getName());
        response.setDomainPath(domain.getPath());
        response.setKeypair(kubernetesCluster.getKeyPair());
        response.setState(kubernetesCluster.getState().toString());
        response.setCores(String.valueOf(kubernetesCluster.getCores()));
        response.setMemory(String.valueOf(kubernetesCluster.getMemory()));
        NetworkVO ntwk = networkDao.findByIdIncludingRemoved(kubernetesCluster.getNetworkId());
        response.setEndpoint(kubernetesCluster.getEndpoint());
        if (ntwk != null) {
            response.setNetworkId(ntwk.getUuid());
            response.setAssociatedNetworkName(ntwk.getName());
            if (!isDirectAccess(ntwk)) {
                List<IPAddressVO> ipAddresses = ipAddressDao.listByAssociatedNetwork(ntwk.getId(), true);
                if (ipAddresses != null && ipAddresses.size() == 1) {
                    response.setIpAddress(ipAddresses.get(0).getAddress().addr());
                    response.setIpAddressId(ipAddresses.get(0).getUuid());
                }
            }
        }

        List<KubernetesUserVmResponse> vmResponses = new ArrayList<>();
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
                    KubernetesUserVmResponse kubernetesUserVmResponse = new KubernetesUserVmResponse();
                    try {
                        BeanUtils.copyProperties(kubernetesUserVmResponse, vmResponse);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to generate zone metrics response");
                    }
                    kubernetesUserVmResponse.setExternalNode(vmMapVO.isExternalNode());
                    kubernetesUserVmResponse.setEtcdNode(vmMapVO.isEtcdNode());
                    kubernetesUserVmResponse.setNodeVersion(vmMapVO.getNodeVersion());
                    vmResponses.add(kubernetesUserVmResponse);
                }
            }
            List<Long> etcdNodeIds = vmList.stream().filter(KubernetesClusterVmMapVO::isEtcdNode).map(KubernetesClusterVmMapVO::getVmId).collect(Collectors.toList());
            List<Long> etcdIpIds = new ArrayList<>();
            Map<String, String> etcdIps = new HashMap<>();
            int etcdNodeSshPort = KubernetesClusterService.KubernetesEtcdNodeStartPort.value();
            etcdNodeIds.forEach(id -> {
               etcdIpIds.addAll(pfRuleDao.listByVm(id).stream().filter(rule -> rule.getSourcePortStart() == etcdNodeSshPort)
                       .map(PortForwardingRuleVO::getSourceIpAddressId).collect(Collectors.toList()));
            });
            etcdIpIds.forEach(id -> {
                IPAddressVO ipAddress = ipAddressDao.findById(id);
                etcdIps.put(ipAddress.getUuid(), ipAddress.getAddress().addr());
            });
            response.setEtcdIps(etcdIps);
        }
        response.setHasAnnotation(annotationDao.hasAnnotations(kubernetesCluster.getUuid(),
                AnnotationService.EntityType.KUBERNETES_CLUSTER.name(), accountService.isRootAdmin(caller.getId())));
        response.setVirtualMachines(vmResponses);
        response.setAutoscalingEnabled(kubernetesCluster.getAutoscalingEnabled());
        response.setMinSize(kubernetesCluster.getMinSize());
        response.setMaxSize(kubernetesCluster.getMaxSize());
        response.setClusterType(kubernetesCluster.getClusterType());
        response.setCreated(kubernetesCluster.getCreated());

        return response;
    }

    private void validateEndpointUrl() {
        String csUrl = ApiServiceConfiguration.ApiServletPath.value();
        if (csUrl == null || csUrl.contains("localhost")) {
            String error = String.format("Global setting %s has to be set to the Management Server's API end point",
                ApiServiceConfiguration.ApiServletPath.key());
            throw new InvalidParameterValueException(error);
        }
    }

    private DataCenter validateAndGetZoneForKubernetesCreateParameters(Long zoneId, Long networkId) {
        DataCenter zone = dataCenterDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by ID: " + zoneId);
        }
        if (zone.getAllocationState() == Grouping.AllocationState.Disabled) {
            throw new PermissionDeniedException(String.format("Cannot perform this operation, zone ID: %s is currently disabled", zone.getUuid()));
        }
        if (DataCenter.Type.Edge.equals(zone.getType()) && networkId == null) {
            throw new PermissionDeniedException("Kubernetes clusters cannot be created on an edge zone without an existing network");
        }
        return zone;
    }

    private void validateSshKeyPairForKubernetesCreateParameters(String sshKeyPair, Account owner) {
        if (!StringUtils.isBlank(sshKeyPair)) {
            SSHKeyPairVO sshKeyPairVO = sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), sshKeyPair);
            if (sshKeyPairVO == null) {
                throw new InvalidParameterValueException(String.format("Given SSH key pair with name: %s was not found for the account %s", sshKeyPair, owner.getAccountName()));
            }
        }
    }

    private Network validateAndGetNetworkForKubernetesCreateParameters(Long networkId) {
        Network network = null;
        if (networkId != null) {
            network = networkService.getNetwork(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Unable to find network with given ID");
            }
        }
        return network;
    }

    private void validateUnmanagedKubernetesClusterCreateParameters(final CreateKubernetesClusterCmd cmd) throws CloudRuntimeException {
        final String name = cmd.getName();
        final Long zoneId = cmd.getZoneId();
        final Account owner = accountService.getActiveAccountById(cmd.getEntityOwnerId());
        final Long networkId = cmd.getNetworkId();
        final String sshKeyPair = cmd.getSSHKeyPairName();
        final String dockerRegistryUserName = cmd.getDockerRegistryUserName();
        final String dockerRegistryPassword = cmd.getDockerRegistryPassword();
        final String dockerRegistryUrl = cmd.getDockerRegistryUrl();
        final Long nodeRootDiskSize = cmd.getNodeRootDiskSize();
        final String externalLoadBalancerIpAddress = cmd.getExternalLoadBalancerIpAddress();

        if (name == null || name.isEmpty()) {
            throw new InvalidParameterValueException("Invalid name for the Kubernetes cluster name: " + name);
        }

        validateAndGetZoneForKubernetesCreateParameters(zoneId, networkId);
        validateSshKeyPairForKubernetesCreateParameters(sshKeyPair, owner);

        if (nodeRootDiskSize != null && nodeRootDiskSize <= 0) {
            throw new InvalidParameterValueException(String.format("Invalid value for %s", ApiConstants.NODE_ROOT_DISK_SIZE));
        }

        validateDockerRegistryParams(dockerRegistryUserName, dockerRegistryPassword, dockerRegistryUrl);

        validateAndGetNetworkForKubernetesCreateParameters(networkId);

        if (StringUtils.isNotEmpty(externalLoadBalancerIpAddress) && (!NetUtils.isValidIp4(externalLoadBalancerIpAddress) && !NetUtils.isValidIp6(externalLoadBalancerIpAddress))) {
            throw new InvalidParameterValueException("Invalid external load balancer IP address");
        }
    }

    public boolean isCommandSupported(KubernetesCluster cluster, String cmdName) {
        switch (cluster.getClusterType()) {
            case CloudManaged:
                return Arrays.asList(
                        BaseCmd.getCommandNameByClass(CreateKubernetesClusterCmd.class),
                        BaseCmd.getCommandNameByClass(ListKubernetesClustersCmd.class),
                        BaseCmd.getCommandNameByClass(DeleteKubernetesClusterCmd.class),
                        BaseCmd.getCommandNameByClass(ScaleKubernetesClusterCmd.class),
                        BaseCmd.getCommandNameByClass(StartKubernetesClusterCmd.class),
                        BaseCmd.getCommandNameByClass(StopKubernetesClusterCmd.class),
                        BaseCmd.getCommandNameByClass(UpgradeKubernetesClusterCmd.class),
                        BaseCmd.getCommandNameByClass(AddNodesToKubernetesClusterCmd.class),
                        BaseCmd.getCommandNameByClass(RemoveNodesFromKubernetesClusterCmd.class)
                ).contains(cmdName);
            case ExternalManaged:
                return Arrays.asList(
                        BaseCmd.getCommandNameByClass(CreateKubernetesClusterCmd.class),
                        BaseCmd.getCommandNameByClass(ListKubernetesClustersCmd.class),
                        BaseCmd.getCommandNameByClass(DeleteKubernetesClusterCmd.class),
                        BaseCmd.getCommandNameByClass(AddVirtualMachinesToKubernetesClusterCmd.class),
                        BaseCmd.getCommandNameByClass(RemoveVirtualMachinesFromKubernetesClusterCmd.class)
                ).contains(cmdName);
            default:
                return false;
        }
    }

    private void validateManagedKubernetesClusterCreateParameters(final CreateKubernetesClusterCmd cmd) throws CloudRuntimeException {
        validateEndpointUrl();
        final String name = cmd.getName();
        final Long zoneId = cmd.getZoneId();
        final Long kubernetesVersionId = cmd.getKubernetesVersionId();
        final Account owner = accountService.getActiveAccountById(cmd.getEntityOwnerId());
        final Long networkId = cmd.getNetworkId();
        final String sshKeyPair = cmd.getSSHKeyPairName();
        final Long controlNodeCount = cmd.getControlNodes();
        final Long clusterSize = cmd.getClusterSize();
        final String dockerRegistryUserName = cmd.getDockerRegistryUserName();
        final String dockerRegistryPassword = cmd.getDockerRegistryPassword();
        final String dockerRegistryUrl = cmd.getDockerRegistryUrl();
        final Long nodeRootDiskSize = cmd.getNodeRootDiskSize();
        final String externalLoadBalancerIpAddress = cmd.getExternalLoadBalancerIpAddress();
        final Map<String, Long> serviceOfferingNodeTypeMap = cmd.getServiceOfferingNodeTypeMap();
        final Long defaultServiceOfferingId = cmd.getServiceOfferingId();

        if (name == null || name.isEmpty()) {
            throw new InvalidParameterValueException("Invalid name for the Kubernetes cluster name: " + name);
        }

        if (controlNodeCount < 1) {
            throw new InvalidParameterValueException("Invalid cluster control nodes count: " + controlNodeCount);
        }
        if (clusterSize == null || clusterSize < 1) {
            throw new InvalidParameterValueException("Invalid cluster size: " + clusterSize);
        }

        int maxClusterSize = KubernetesMaxClusterSize.valueIn(owner.getId());
        final long totalNodeCount = controlNodeCount + clusterSize;
        if (totalNodeCount > maxClusterSize) {
            throw new InvalidParameterValueException(
                String.format("Maximum cluster size can not exceed %d. Please contact your administrator", maxClusterSize));
        }

        DataCenter zone = validateAndGetZoneForKubernetesCreateParameters(zoneId, networkId);

        if (!isKubernetesServiceConfigured(zone, networkId)) {
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

        validateServiceOfferingsForNodeTypes(serviceOfferingNodeTypeMap, defaultServiceOfferingId, cmd.getEtcdNodes(), clusterKubernetesVersion);

        validateSshKeyPairForKubernetesCreateParameters(sshKeyPair, owner);

        if (nodeRootDiskSize != null && nodeRootDiskSize <= 0) {
            throw new InvalidParameterValueException(String.format("Invalid value for %s", ApiConstants.NODE_ROOT_DISK_SIZE));
        }

        validateDockerRegistryParams(dockerRegistryUserName, dockerRegistryPassword, dockerRegistryUrl);

        Network network = validateAndGetNetworkForKubernetesCreateParameters(networkId);

        if (StringUtils.isNotEmpty(externalLoadBalancerIpAddress)) {
            NsxProviderVO nsxProviderVO = nsxProviderDao.findByZoneId(zone.getId());
            if (Objects.nonNull(nsxProviderVO)) {
                throw new InvalidParameterValueException("External load balancer IP address is not supported on NSX-enabled zones");
            }
            if (!NetUtils.isValidIp4(externalLoadBalancerIpAddress) && !NetUtils.isValidIp6(externalLoadBalancerIpAddress)) {
                throw new InvalidParameterValueException("Invalid external load balancer IP address");
            }
            if (network == null) {
                throw new InvalidParameterValueException(String.format("%s parameter must be specified along with %s parameter", ApiConstants.EXTERNAL_LOAD_BALANCER_IP_ADDRESS, ApiConstants.NETWORK_ID));
            }
            if (!Network.GuestType.Shared.equals(network.getGuestType()) || routedIpv4Manager.isRoutedNetwork(network)) {
                throw new InvalidParameterValueException(String.format("%s parameter must be specified when network type is not %s or is %s network", ApiConstants.EXTERNAL_LOAD_BALANCER_IP_ADDRESS, Network.GuestType.Shared, NetworkOffering.NetworkMode.ROUTED));
            }
        }

        if (!KubernetesClusterExperimentalFeaturesEnabled.value() && !StringUtils.isAllEmpty(dockerRegistryUrl, dockerRegistryUserName, dockerRegistryPassword)) {
            throw new CloudRuntimeException(String.format("Private registry for the Kubernetes cluster is an experimental feature. Use %s configuration for enabling experimental features", KubernetesClusterExperimentalFeaturesEnabled.key()));
        }
    }

    protected void validateServiceOfferingsForNodeTypes(Map<String, Long> map,
                                                        Long defaultServiceOfferingId,
                                                        Long etcdNodes,
                                                        KubernetesSupportedVersion clusterKubernetesVersion) {
        for (String key : CLUSTER_NODES_TYPES_LIST) {
            validateServiceOfferingForNode(map, defaultServiceOfferingId, key, etcdNodes, clusterKubernetesVersion);
        }
    }

    protected void validateServiceOfferingForNode(Map<String, Long> map,
                                                  Long defaultServiceOfferingId,
                                                  String key, Long etcdNodes,
                                                  KubernetesSupportedVersion clusterKubernetesVersion) {
        if (ETCD.name().equalsIgnoreCase(key) && (etcdNodes == null || etcdNodes == 0)) {
            return;
        }
        Long serviceOfferingId = map.getOrDefault(key, defaultServiceOfferingId);
        ServiceOffering serviceOffering = serviceOfferingId != null ? serviceOfferingDao.findById(serviceOfferingId) : null;
        if (serviceOffering == null) {
            throw new InvalidParameterValueException("When serviceofferingid is not specified, " +
                    "service offerings for each node type must be specified in the nodeofferings parameter.");
        }
        try {
            validateServiceOffering(serviceOffering, clusterKubernetesVersion);
        } catch (InvalidParameterValueException e) {
            String msg = String.format("Given service offering ID: %s for %s nodes is not suitable for the Kubernetes cluster version %s - %s",
                    serviceOffering, key, clusterKubernetesVersion, e.getMessage());
            logger.error(msg);
            throw new InvalidParameterValueException(msg);
        }
    }

    private Network getKubernetesClusterNetworkIfMissing(final String clusterName, final DataCenter zone,  final Account owner, final int controlNodesCount,
                         final int nodesCount, final String externalLoadBalancerIpAddress, final Long networkId, final Long asNumber) throws CloudRuntimeException {
        Network network = null;
        if (networkId != null) {
            network = networkDao.findById(networkId);
            if (Network.GuestType.Isolated.equals(network.getGuestType())) {
                if (kubernetesClusterDao.listByNetworkId(network.getId()).isEmpty()) {
                    validateNetwork(network, controlNodesCount + nodesCount);
                    networkModel.checkNetworkPermissions(owner, network);
                } else {
                    throw new InvalidParameterValueException(String.format("Network ID: %s is already under use by another Kubernetes cluster", network.getUuid()));
                }
            }
            if (isDirectAccess(network)) {
                if (controlNodesCount > 1 && StringUtils.isEmpty(externalLoadBalancerIpAddress)) {
                    throw new InvalidParameterValueException(String.format("Multi-control nodes, HA Kubernetes cluster with %s network ID: %s needs an external load balancer IP address. %s parameter can be used",
                            network.getGuestType().toString(), network.getUuid(), ApiConstants.EXTERNAL_LOAD_BALANCER_IP_ADDRESS));
                }
            }
        } else { // user has not specified network in which cluster VM's to be provisioned, so create a network for Kubernetes cluster
            NetworkOfferingVO networkOffering = networkOfferingDao.findByUniqueName(KubernetesClusterNetworkOffering.value());

            long physicalNetworkId = networkModel.findPhysicalNetworkId(zone.getId(), networkOffering.getTags(), networkOffering.getTrafficType());
            PhysicalNetwork physicalNetwork = physicalNetworkDao.findById(physicalNetworkId);

            if (logger.isInfoEnabled()) {
                logger.info("Creating network for account: {} from the network offering: {} as part of Kubernetes cluster: {} deployment process", owner, networkOffering, clusterName);
            }

            CallContext networkContext = CallContext.register(CallContext.current(), ApiCommandResourceType.Network);
            try {
                network = networkService.createGuestNetwork(networkOffering.getId(), clusterName + "-network",
                        owner.getAccountName() + "-network", owner, physicalNetwork, zone.getId(),
                        ControlledEntity.ACLType.Account);
                if (!networkOffering.isForVpc() && NetworkOffering.RoutingMode.Dynamic == networkOffering.getRoutingMode()) {
                    bgpService.allocateASNumber(zone.getId(), asNumber, network.getId(), null);
                }
            } catch (ConcurrentOperationException | InsufficientCapacityException | ResourceAllocationException e) {
                logAndThrow(Level.ERROR, String.format("Unable to create network for the Kubernetes cluster: %s", clusterName));
            } finally {
                CallContext.unregister();
            }
        }
        return network;
    }

    private void addKubernetesClusterDetails(final KubernetesCluster kubernetesCluster, final Network network, final CreateKubernetesClusterCmd cmd) {
        final String externalLoadBalancerIpAddress = cmd.getExternalLoadBalancerIpAddress();
        final String dockerRegistryUserName = cmd.getDockerRegistryUserName();
        final String dockerRegistryPassword = cmd.getDockerRegistryPassword();
        final String dockerRegistryUrl = cmd.getDockerRegistryUrl();
        final boolean networkCleanup = cmd.getNetworkId() == null;
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                List<KubernetesClusterDetailsVO> details = new ArrayList<>();
                long kubernetesClusterId = kubernetesCluster.getId();

                if ((network != null && isDirectAccess(network)) || kubernetesCluster.getClusterType() == KubernetesCluster.ClusterType.ExternalManaged) {
                    addKubernetesClusterDetailIfIsNotEmpty(details, kubernetesClusterId, ApiConstants.EXTERNAL_LOAD_BALANCER_IP_ADDRESS, externalLoadBalancerIpAddress, true);
                }

                addKubernetesClusterDetailIfIsNotEmpty(details, kubernetesClusterId, ApiConstants.DOCKER_REGISTRY_USER_NAME, dockerRegistryUserName, true);
                addKubernetesClusterDetailIfIsNotEmpty(details, kubernetesClusterId, ApiConstants.DOCKER_REGISTRY_PASSWORD, dockerRegistryPassword, false);
                addKubernetesClusterDetailIfIsNotEmpty(details, kubernetesClusterId, ApiConstants.DOCKER_REGISTRY_URL, dockerRegistryUrl, true);
                if (kubernetesCluster.getClusterType() == KubernetesCluster.ClusterType.CloudManaged) {
                    details.add(new KubernetesClusterDetailsVO(kubernetesClusterId, "networkCleanup", String.valueOf(networkCleanup), true));
                }
                kubernetesClusterDetailsDao.saveDetails(details);
            }
        });
    }

    protected void addKubernetesClusterDetailIfIsNotEmpty(List<KubernetesClusterDetailsVO> details, long id, String name, String value, boolean display) {
        if (StringUtils.isNotEmpty(value)) {
            details.add(new KubernetesClusterDetailsVO(id, name, value, display));
        }
    }

    protected void validateKubernetesClusterScaleSize(final KubernetesClusterVO kubernetesCluster, final Long clusterSize, final int maxClusterSize, final DataCenter zone) {
        if (clusterSize == null) {
            return;
        }
        if (clusterSize == kubernetesCluster.getNodeCount()) {
            return;
        }
        if (kubernetesCluster.getState().equals(KubernetesCluster.State.Stopped)) { // Cannot scale stopped cluster currently for cluster size
            throw new PermissionDeniedException(String.format("Kubernetes cluster : %s is in %s state", kubernetesCluster.getName(), kubernetesCluster.getState().toString()));
        }
        if (clusterSize < 1) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster : %s cannot be scaled for size, %d", kubernetesCluster.getName(), clusterSize));
        }
        if (clusterSize + kubernetesCluster.getControlNodeCount() > maxClusterSize) {
            throw new InvalidParameterValueException(
                    String.format("Maximum cluster size can not exceed %d. Please contact your administrator", maxClusterSize));
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

    private void validateKubernetesClusterScaleParameters(ScaleKubernetesClusterCmd cmd) {
        final Long kubernetesClusterId = cmd.getId();
        final Long clusterSize = cmd.getClusterSize();
        final List<Long> nodeIds = cmd.getNodeIds();
        final Boolean isAutoscalingEnabled = cmd.isAutoscalingEnabled();
        final Long minSize = cmd.getMinSize();
        final Long maxSize = cmd.getMaxSize();
        final Long defaultServiceOfferingId = cmd.getServiceOfferingId();
        final Map<String, Long> serviceOfferingNodeTypeMap = cmd.getServiceOfferingNodeTypeMap();

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

        if (defaultServiceOfferingId == null && isAnyNodeOfferingEmpty(serviceOfferingNodeTypeMap)
                && clusterSize == null && nodeIds == null && isAutoscalingEnabled == null) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster %s cannot be scaled, either service offering or cluster size or nodeids to be removed or autoscaling must be passed", kubernetesCluster.getName()));
        }

        Account caller = CallContext.current().getCallingAccount();
        accountManager.checkAccess(caller, SecurityChecker.AccessType.OperateEntry, false, kubernetesCluster);
        if (!isCommandSupported(kubernetesCluster, cmd.getActualCommandName())) {
            throw new InvalidParameterValueException(String.format("Scale kubernetes cluster is not supported for an externally managed cluster (%s)", kubernetesCluster.getName()));
        }

        final KubernetesSupportedVersion clusterVersion = kubernetesSupportedVersionDao.findById(kubernetesCluster.getKubernetesVersionId());
        if (clusterVersion == null) {
            throw new CloudRuntimeException(String.format("Invalid Kubernetes version associated with Kubernetes cluster : %s", kubernetesCluster.getName()));
        }
        List<KubernetesCluster.State> validClusterStates = Arrays.asList(KubernetesCluster.State.Created, KubernetesCluster.State.Running, KubernetesCluster.State.Stopped);
        if (!(validClusterStates.contains(kubernetesCluster.getState()))) {
            throw new PermissionDeniedException(String.format("Kubernetes cluster %s is in %s state and can not be scaled", kubernetesCluster.getName(), kubernetesCluster.getState().toString()));
        }

        int maxClusterSize = KubernetesMaxClusterSize.valueIn(kubernetesCluster.getAccountId());
        if (isAutoscalingEnabled != null && isAutoscalingEnabled) {
            if (clusterSize != null || nodeIds != null) {
                throw new InvalidParameterValueException("Autoscaling can not be passed along with nodeids or clustersize");
            }

            if (!KubernetesVersionManagerImpl.versionSupportsAutoscaling(clusterVersion)) {
                throw new InvalidParameterValueException(String.format("Autoscaling requires Kubernetes Version %s or above",
                    KubernetesVersionManagerImpl.MINIMUN_AUTOSCALER_SUPPORTED_VERSION ));
            }

            validateEndpointUrl();

            if (minSize == null || maxSize == null) {
                throw new InvalidParameterValueException("Autoscaling requires minsize and maxsize to be passed");
            }
            if (minSize < 1) {
                throw new InvalidParameterValueException("Minsize must be at least than 1");
            }
            if (maxSize <= minSize) {
                throw new InvalidParameterValueException("Maxsize must be greater than minsize");
            }
            if (maxSize + kubernetesCluster.getControlNodeCount() > maxClusterSize) {
                throw new InvalidParameterValueException(
                    String.format("Maximum cluster size can not exceed %d. Please contact your administrator", maxClusterSize));
            }
        }

        Long workerOfferingId = serviceOfferingNodeTypeMap != null ? serviceOfferingNodeTypeMap.getOrDefault(WORKER.name(), null) : null;
        if (nodeIds != null) {
            if (clusterSize != null || defaultServiceOfferingId != null || workerOfferingId != null) {
                throw new InvalidParameterValueException("nodeids can not be passed along with clustersize or service offering");
            }
            List<KubernetesClusterVmMapVO> nodes = kubernetesClusterVmMapDao.listByClusterIdAndVmIdsIn(kubernetesCluster.getId(), nodeIds);
            // Do all the nodes exist ?
            if (nodes == null || nodes.size() != nodeIds.size()) {
                throw new InvalidParameterValueException("Invalid node ids");
            }
            // Ensure there's always a control node
            long controleNodesToRemove = nodes.stream().filter(x -> x.isControlNode()).count();
            if (controleNodesToRemove >= kubernetesCluster.getControlNodeCount()) {
                throw new InvalidParameterValueException("Can not remove all control nodes from a cluster");
            }
            // Ensure there's always a node
            long nodesToRemove = nodes.stream().filter(x -> !x.isControlNode()).count();
            if (nodesToRemove >= kubernetesCluster.getNodeCount()) {
                throw new InvalidParameterValueException("Can not remove all nodes from a cluster");
            }
        }

        validateServiceOfferingsForNodeTypesScale(serviceOfferingNodeTypeMap, defaultServiceOfferingId, kubernetesCluster, clusterVersion);

        validateKubernetesClusterScaleSize(kubernetesCluster, clusterSize, maxClusterSize, zone);
    }

    protected void validateServiceOfferingsForNodeTypesScale(Map<String, Long> map, Long defaultServiceOfferingId, KubernetesClusterVO kubernetesCluster, KubernetesSupportedVersion clusterVersion) {
        for (String key : CLUSTER_NODES_TYPES_LIST) {
            Long serviceOfferingId = map.getOrDefault(key, defaultServiceOfferingId);
            if (serviceOfferingId != null) {
                ServiceOffering serviceOffering = serviceOfferingDao.findById(serviceOfferingId);
                if (serviceOffering == null) {
                    throw new InvalidParameterValueException("Failed to find service offering ID: " + serviceOfferingId);
                }
                checkServiceOfferingForNodesScale(serviceOffering, kubernetesCluster, clusterVersion);
                Long nodeTypeOfferingId = getExistingServiceOfferingIdForNodeType(key, kubernetesCluster);
                if (nodeTypeOfferingId == null) {
                    nodeTypeOfferingId = kubernetesCluster.getServiceOfferingId();
                }
                final ServiceOffering existingServiceOffering = serviceOfferingDao.findById(nodeTypeOfferingId);
                if (KubernetesCluster.State.Running.equals(kubernetesCluster.getState()) && (serviceOffering.getRamSize() < existingServiceOffering.getRamSize() ||
                        serviceOffering.getCpu() * serviceOffering.getSpeed() < existingServiceOffering.getCpu() * existingServiceOffering.getSpeed())) {
                    logAndThrow(Level.WARN, String.format("Kubernetes cluster cannot be scaled down for service offering. Service offering : %s offers lesser resources as compared to service offering : %s of Kubernetes cluster : %s",
                            serviceOffering.getName(), existingServiceOffering.getName(), kubernetesCluster.getName()));
                }
            }
        }
    }

    private Long getExistingServiceOfferingIdForNodeType(String key, KubernetesClusterVO kubernetesCluster) {
        if (key.equalsIgnoreCase(WORKER.name())) {
            return kubernetesCluster.getWorkerNodeServiceOfferingId();
        } else if (key.equalsIgnoreCase(CONTROL.name())) {
            return kubernetesCluster.getControlNodeServiceOfferingId();
        } else if (key.equalsIgnoreCase(ETCD.name())) {
            return kubernetesCluster.getEtcdNodeServiceOfferingId();
        }
        return kubernetesCluster.getServiceOfferingId();
    }

    protected void checkServiceOfferingForNodesScale(ServiceOffering serviceOffering, KubernetesClusterVO kubernetesCluster, KubernetesSupportedVersion clusterVersion) {
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

    protected boolean isAnyNodeOfferingEmpty(Map<String, Long> map) {
        if (MapUtils.isEmpty(map)) {
            return true;
        }
        return map.values().stream().anyMatch(Objects::isNull);
    }

    private void validateKubernetesClusterUpgradeParameters(UpgradeKubernetesClusterCmd cmd) {
        // Validate parameters
        validateEndpointUrl();

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
        if (!isCommandSupported(kubernetesCluster, cmd.getActualCommandName())) {
            throw new InvalidParameterValueException(String.format("Upgrade kubernetes cluster is not supported for an externally managed cluster (%s)", kubernetesCluster.getName()));
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
            throw new InvalidParameterValueException(String.format("Invalid Kubernetes version associated with cluster : %s",
                    kubernetesCluster.getName()));
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
            logger.warn("Failed to transition state of the Kubernetes cluster: {} in state {} on event {}", kubernetesCluster, kubernetesCluster.getState().toString(), e.toString(), nte);
            return false;
        }
    }

    @Override
    @ActionEvent(eventType = KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_CREATE,
            eventDescription = "creating Kubernetes cluster", create = true)
    public KubernetesCluster createUnmanagedKubernetesCluster(CreateKubernetesClusterCmd cmd) throws CloudRuntimeException {
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }

        validateUnmanagedKubernetesClusterCreateParameters(cmd);

        final DataCenter zone = dataCenterDao.findById(cmd.getZoneId());
        final long controlNodeCount = cmd.getControlNodes();
        final long clusterSize = Objects.requireNonNullElse(cmd.getClusterSize(), 0L);
        final ServiceOffering serviceOffering = serviceOfferingDao.findById(cmd.getServiceOfferingId());
        Map<String, Long> nodeTypeOfferingMap = cmd.getServiceOfferingNodeTypeMap();
        final Account owner = accountService.getActiveAccountById(cmd.getEntityOwnerId());
        final KubernetesSupportedVersion clusterKubernetesVersion = kubernetesSupportedVersionDao.findById(cmd.getKubernetesVersionId());

        final Network network = networkDao.findById(cmd.getNetworkId());
        long cores = 0;
        long memory = 0;
        Long serviceOfferingId = null;
        if (serviceOffering != null) {
            serviceOfferingId = serviceOffering.getId();
            cores = serviceOffering.getCpu() * (controlNodeCount + clusterSize);
            memory = serviceOffering.getRamSize() * (controlNodeCount + clusterSize);
        }

        final Long finalServiceOfferingId = serviceOfferingId;
        final Long defaultNetworkId = network == null ? null : network.getId();
        final Long clusterKubernetesVersionId = clusterKubernetesVersion == null ? null : clusterKubernetesVersion.getId();
        final long finalCores = cores;
        final long finalMemory = memory;
        final KubernetesClusterVO cluster = Transaction.execute(new TransactionCallback<KubernetesClusterVO>() {
            @Override
            public KubernetesClusterVO doInTransaction(TransactionStatus status) {
                KubernetesClusterVO newCluster = new KubernetesClusterVO(cmd.getName(), cmd.getDisplayName(), zone.getId(), clusterKubernetesVersionId,
                        finalServiceOfferingId, null, defaultNetworkId, owner.getDomainId(),
                        owner.getAccountId(), controlNodeCount, clusterSize, KubernetesCluster.State.Running, cmd.getSSHKeyPairName(), finalCores, finalMemory,
                        cmd.getNodeRootDiskSize(), "", KubernetesCluster.ClusterType.ExternalManaged);
                kubernetesClusterDao.persist(newCluster);
                return newCluster;
            }
        });

        addKubernetesClusterDetails(cluster, network, cmd);

        if (logger.isInfoEnabled()) {
            logger.info(String.format("Kubernetes cluster with name: %s and ID: %s has been created", cluster.getName(), cluster.getUuid()));
        }
        CallContext.current().putContextParameter(KubernetesCluster.class, cluster.getUuid());
        return cluster;
    }

    @Override
    @ActionEvent(eventType = KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_CREATE,
            eventDescription = "creating Kubernetes cluster", create = true)
    public KubernetesCluster createManagedKubernetesCluster(CreateKubernetesClusterCmd cmd) throws CloudRuntimeException {
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }

        validateManagedKubernetesClusterCreateParameters(cmd);

        final DataCenter zone = dataCenterDao.findById(cmd.getZoneId());
        final long controlNodeCount = cmd.getControlNodes();
        final long clusterSize = cmd.getClusterSize();
        final long etcdNodes = cmd.getEtcdNodes();
        final Map<String, Long> nodeTypeCount = Map.of(WORKER.name(), clusterSize,
                CONTROL.name(), controlNodeCount, ETCD.name(), etcdNodes);
        final Account owner = accountService.getActiveAccountById(cmd.getEntityOwnerId());
        final KubernetesSupportedVersion clusterKubernetesVersion = kubernetesSupportedVersionDao.findById(cmd.getKubernetesVersionId());
        final Hypervisor.HypervisorType hypervisor = cmd.getHypervisorType();
        final Long asNumber = cmd.getAsNumber();

        Map<String, Long> serviceOfferingNodeTypeMap = cmd.getServiceOfferingNodeTypeMap();
        Long defaultServiceOfferingId = cmd.getServiceOfferingId();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long accountId = null;
        if (Objects.nonNull(accountName) && Objects.nonNull(domainId)) {
            Account account = accountDao.findActiveAccount(accountName, domainId);
            if (Objects.nonNull(account)) {
                accountId = account.getId();
            }
        }
        Hypervisor.HypervisorType hypervisorType = getHypervisorTypeAndValidateNodeDeployments(serviceOfferingNodeTypeMap, defaultServiceOfferingId, nodeTypeCount, zone, domainId, accountId, hypervisor);

        SecurityGroup securityGroup = null;
        if (zone.isSecurityGroupEnabled()) {
            securityGroup = getOrCreateSecurityGroupForAccount(owner);
        }

        Map<String, Long> templateNodeTypeMap = cmd.getTemplateNodeTypeMap();
        final VMTemplateVO finalTemplate = getKubernetesServiceTemplate(zone, hypervisorType, templateNodeTypeMap, DEFAULT, clusterKubernetesVersion);
        final VMTemplateVO controlNodeTemplate = getKubernetesServiceTemplate(zone, hypervisorType, templateNodeTypeMap, CONTROL, clusterKubernetesVersion);
        final VMTemplateVO workerNodeTemplate = getKubernetesServiceTemplate(zone, hypervisorType, templateNodeTypeMap, WORKER, clusterKubernetesVersion);
        final VMTemplateVO etcdNodeTemplate = getKubernetesServiceTemplate(zone, hypervisorType, templateNodeTypeMap, ETCD, clusterKubernetesVersion);
        final Network defaultNetwork = getKubernetesClusterNetworkIfMissing(cmd.getName(), zone, owner, (int)controlNodeCount, (int)clusterSize, cmd.getExternalLoadBalancerIpAddress(), cmd.getNetworkId(), asNumber);

        final SecurityGroup finalSecurityGroup = securityGroup;
        final KubernetesClusterVO cluster = Transaction.execute(new TransactionCallback<KubernetesClusterVO>() {
            @Override
            public KubernetesClusterVO doInTransaction(TransactionStatus status) {
                Pair<Long, Long> capacityPair = calculateClusterCapacity(serviceOfferingNodeTypeMap, nodeTypeCount, defaultServiceOfferingId);
                final long cores = capacityPair.first();
                final long memory = capacityPair.second();

                KubernetesClusterVO newCluster = new KubernetesClusterVO(cmd.getName(), cmd.getDisplayName(), zone.getId(), clusterKubernetesVersion.getId(),
                        defaultServiceOfferingId, Objects.nonNull(finalTemplate) ? finalTemplate.getId() : null,
                        defaultNetwork.getId(), owner.getDomainId(), owner.getAccountId(), controlNodeCount, clusterSize,
                        KubernetesCluster.State.Created, cmd.getSSHKeyPairName(), cores, memory,
                        cmd.getNodeRootDiskSize(), "", KubernetesCluster.ClusterType.CloudManaged);
                newCluster.setCniConfigId(cmd.getCniConfigId());
                String cniConfigDetails = null;
                if (MapUtils.isNotEmpty(cmd.getCniConfigDetails())) {
                    cniConfigDetails = cmd.getCniConfigDetails().toString();
                }
                newCluster.setCniConfigDetails(cniConfigDetails);
                if (serviceOfferingNodeTypeMap.containsKey(WORKER.name())) {
                    newCluster.setWorkerNodeServiceOfferingId(serviceOfferingNodeTypeMap.get(WORKER.name()));
                }
                if (serviceOfferingNodeTypeMap.containsKey(CONTROL.name())) {
                    newCluster.setControlNodeServiceOfferingId(serviceOfferingNodeTypeMap.get(CONTROL.name()));
                }
                if (etcdNodes > 0) {
                    newCluster.setEtcdNodeTemplateId(etcdNodeTemplate.getId());
                    newCluster.setEtcdNodeCount(etcdNodes);
                    if (serviceOfferingNodeTypeMap.containsKey(ETCD.name())) {
                        newCluster.setEtcdNodeServiceOfferingId(serviceOfferingNodeTypeMap.get(ETCD.name()));
                    }
                }
                newCluster.setWorkerNodeTemplateId(workerNodeTemplate.getId());
                newCluster.setControlNodeTemplateId(controlNodeTemplate.getId());
                if (zone.isSecurityGroupEnabled()) {
                    newCluster.setSecurityGroupId(finalSecurityGroup.getId());
                }
                kubernetesClusterDao.persist(newCluster);
                addKubernetesClusterDetails(newCluster, defaultNetwork, cmd);
                return newCluster;
            }
        });

        if (logger.isInfoEnabled()) {
            logger.info("Kubernetes cluster {} has been created", cluster);
        }
        CallContext.current().putContextParameter(KubernetesCluster.class, cluster.getUuid());
        return cluster;
    }

    protected Pair<Long, Long> calculateClusterCapacity(Map<String, Long> map, Map<String, Long> nodeTypeCount, Long defaultServiceOfferingId) {
        long cores = 0L;
        long memory = 0L;
        for (String key : CLUSTER_NODES_TYPES_LIST) {
            if (nodeTypeCount.getOrDefault(key, 0L) == 0) {
                continue;
            }
            Long serviceOfferingId = map.getOrDefault(key, defaultServiceOfferingId);
            ServiceOffering serviceOffering = serviceOfferingDao.findById(serviceOfferingId);
            Long nodes = nodeTypeCount.get(key);
            cores = cores + (serviceOffering.getCpu() * nodes);
            memory = memory + (serviceOffering.getRamSize() * nodes);
        }
        return new Pair<>(cores, memory);
    }

    protected Hypervisor.HypervisorType getHypervisorTypeAndValidateNodeDeployments(Map<String, Long> serviceOfferingNodeTypeMap,
                                                                                    Long defaultServiceOfferingId,
                                                                                    Map<String, Long> nodeTypeCount,
                                                                                    DataCenter zone, Long domainId, Long accountId,
                                                                                    Hypervisor.HypervisorType hypervisorType) {
        Hypervisor.HypervisorType deploymentHypervisor = null;
        for (String nodeType : CLUSTER_NODES_TYPES_LIST) {
            if (!nodeTypeCount.containsKey(nodeType)) {
                continue;
            }
            Long serviceOfferingId = serviceOfferingNodeTypeMap.getOrDefault(nodeType, defaultServiceOfferingId);
            ServiceOffering serviceOffering = serviceOfferingDao.findById(serviceOfferingId);
            Long nodes = nodeTypeCount.getOrDefault(nodeType, defaultServiceOfferingId);
            try {
                if (nodeType.equalsIgnoreCase(ETCD.name()) &&
                        (!serviceOfferingNodeTypeMap.containsKey(ETCD.name()) || nodes == 0)) {
                    continue;
                }
                DeployDestination deployDestination = plan(nodes, zone, serviceOffering, domainId, accountId, hypervisorType);
                if (deployDestination.getCluster() == null) {
                    logAndThrow(Level.ERROR, String.format("Creating Kubernetes cluster failed due to error while finding suitable deployment plan for cluster in zone : %s", zone.getName()));
                }
                if (deploymentHypervisor == null) {
                    deploymentHypervisor = deployDestination.getCluster().getHypervisorType();
                    if (hypervisorType != deploymentHypervisor) {
                        String msg = String.format("The hypervisor type planned for the CKS cluster deployment %s is different " +
                                "from the selected hypervisor %s", deployDestination.getCluster().getHypervisorType(), hypervisorType);
                        logger.warn(msg);
                    }
                }
            } catch (InsufficientCapacityException e) {
                logAndThrow(Level.ERROR, String.format("Creating Kubernetes cluster failed due to insufficient capacity for %d nodes cluster in zone : %s with service offering : %s", nodes, zone.getName(), serviceOffering.getName()));
            }
        }
        return deploymentHypervisor;
    }

    private SecurityGroup getOrCreateSecurityGroupForAccount(Account owner) {
        String securityGroupName = String.format("%s-%s", KubernetesClusterActionWorker.CKS_CLUSTER_SECURITY_GROUP_NAME, owner.getUuid());
        String securityGroupDesc = String.format("%s and account %s", KubernetesClusterActionWorker.CKS_SECURITY_GROUP_DESCRIPTION, owner.getName());
        SecurityGroup securityGroup = securityGroupManager.getSecurityGroup(securityGroupName, owner.getId());
        if (securityGroup == null) {
            securityGroup = securityGroupManager.createSecurityGroup(securityGroupName, securityGroupDesc, owner.getDomainId(), owner.getId(), owner.getAccountName());
            if (securityGroup == null) {
                throw new CloudRuntimeException(String.format("Failed to create security group: %s", KubernetesClusterActionWorker.CKS_CLUSTER_SECURITY_GROUP_NAME));
            }
            List<String> cidrList = new ArrayList<>();
            cidrList.add(NetUtils.ALL_IP4_CIDRS);
            securityGroupService.authorizeSecurityGroupRule(securityGroup.getId(), NetUtils.TCP_PROTO,
                    KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_SSH_PORT_SG, KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_SSH_PORT_SG,
                    null, null, cidrList, null, SecurityRule.SecurityRuleType.IngressRule);
            securityGroupService.authorizeSecurityGroupRule(securityGroup.getId(), NetUtils.TCP_PROTO,
                    KubernetesClusterActionWorker.CLUSTER_API_PORT, KubernetesClusterActionWorker.CLUSTER_API_PORT,
                    null, null, cidrList, null, SecurityRule.SecurityRuleType.IngressRule);
            securityGroupService.authorizeSecurityGroupRule(securityGroup.getId(), NetUtils.ALL_PROTO,
                    null, null, null, null, cidrList, null, SecurityRule.SecurityRuleType.EgressRule);
        }
        return securityGroup;
    }

    @Override
    @ActionEvent(eventType = KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_CREATE,
            eventDescription = "creating Kubernetes cluster", async = true)
    public void startKubernetesCluster(CreateKubernetesClusterCmd cmd) throws CloudRuntimeException, ManagementServerException, ResourceUnavailableException, InsufficientCapacityException {
        final Long id = cmd.getEntityId();
        if (KubernetesCluster.ClusterType.valueOf(cmd.getClusterType()) != KubernetesCluster.ClusterType.CloudManaged) {
            return;
        }
        final KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(id);
        if (kubernetesCluster == null) {
            throw new InvalidParameterValueException("Failed to find Kubernetes cluster with given ID");
        }
        Account account = accountService.getAccount(kubernetesCluster.getAccountId());
        if (!startKubernetesCluster(kubernetesCluster.getId(), kubernetesCluster.getDomainId(), account.getAccountName(), cmd.getAsNumber(), true)) {
            throw new CloudRuntimeException(String.format("Failed to start created Kubernetes cluster: %s",
                    kubernetesCluster.getName()));
        }
    }

    @Override
    @ActionEvent(eventType = KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_START,
            eventDescription = "starting Kubernetes cluster", async = true)
    public void startKubernetesCluster(StartKubernetesClusterCmd cmd) throws CloudRuntimeException, ManagementServerException, ResourceUnavailableException, InsufficientCapacityException {
        final Long id = cmd.getId();
        if (id == null || id < 1L) {
            throw new InvalidParameterValueException("Invalid Kubernetes cluster ID provided");
        }
        final KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(id);
        if (kubernetesCluster == null) {
            throw new InvalidParameterValueException("Given Kubernetes cluster was not found");
        }
        if (!isCommandSupported(kubernetesCluster, cmd.getActualCommandName())) {
            throw new InvalidParameterValueException(String.format("Start kubernetes cluster is not supported for " +
                    "an externally managed cluster (%s)", kubernetesCluster.getName()));
        }
        Account account = accountService.getAccount(kubernetesCluster.getAccountId());
        if (!startKubernetesCluster(kubernetesCluster.getId(), kubernetesCluster.getDomainId(), account.getAccountName(), null, false)) {
            throw new CloudRuntimeException(String.format("Failed to start Kubernetes cluster: %s",
                    kubernetesCluster.getName()));
        }
    }

    /**
     * Start operation can be performed at two different life stages of Kubernetes cluster. First when a freshly created cluster
     * in which case there are no resources provisioned for the Kubernetes cluster. So during start all the resources
     * are provisioned from scratch. Second kind of start, happens on  Stopped Kubernetes cluster, in which all resources
     * are provisioned (like volumes, nics, networks etc). It just that VM's are not in running state. So just
     * start the VM's (which can possibly implicitly start the network also).
     *
     * @param kubernetesClusterId
     * @param domainId
     * @param accountName
     * @param onCreate
     * @return
     * @throws CloudRuntimeException
     */
    @Override
    public boolean startKubernetesCluster(long kubernetesClusterId, Long domainId, String accountName, Long asNumber, boolean onCreate)
            throws CloudRuntimeException, ManagementServerException, ResourceUnavailableException, InsufficientCapacityException {
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }
        final KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        if (kubernetesCluster == null) {
            throw new InvalidParameterValueException("Failed to find Kubernetes cluster with given ID");
        }
        if (kubernetesCluster.getRemoved() != null) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster : %s is already deleted",
                    kubernetesCluster.getName()));
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, false, kubernetesCluster);
        if (kubernetesCluster.getState().equals(KubernetesCluster.State.Running)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Kubernetes cluster {} is in running state", kubernetesCluster);
            }
            return true;
        }
        if (kubernetesCluster.getState().equals(KubernetesCluster.State.Starting)) {
            if (logger.isDebugEnabled())
                logger.debug("Kubernetes cluster {} is already in starting state", kubernetesCluster);
            return true;
        }
        final DataCenter zone = dataCenterDao.findById(kubernetesCluster.getZoneId());
        if (zone == null) {
            logAndThrow(Level.WARN, String.format("Unable to find zone for Kubernetes cluster %s", kubernetesCluster));
        }
        Long accountId = null;
        if (Objects.nonNull(accountName) && Objects.nonNull(domainId)) {
            Account account = accountDao.findActiveAccount(accountName, domainId);
            if (Objects.nonNull(account)) {
                accountId = account.getId();
            }
        }
        KubernetesClusterStartWorker startWorker =
            new KubernetesClusterStartWorker(kubernetesCluster, this);
        startWorker = ComponentContext.inject(startWorker);
        if (onCreate) {
            // Start for Kubernetes cluster in 'Created' state
            String[] keys = getServiceUserKeys(kubernetesCluster);
            startWorker.setKeys(keys);
            return startWorker.startKubernetesClusterOnCreate(domainId, accountId, asNumber);
        } else {
            // Start for Kubernetes cluster in 'Stopped' state. Resources are already provisioned, just need to be started
            return startWorker.startStoppedKubernetesCluster(domainId, accountId);
        }
    }

    protected String[] createUserApiKeyAndSecretKey(long userId) {
        CallContext.register(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM);
        try {
            return accountService.createApiKeyAndSecretKey(userId);
        } finally {
            CallContext.unregister();
        }
    }

    protected String[] getServiceUserKeys(Account owner) {
        String username = owner.getAccountName();
        if (!username.startsWith(KUBEADMIN_ACCOUNT_NAME + "-")) {
            username += "-" + KUBEADMIN_ACCOUNT_NAME;
        }
        UserAccount kubeadmin = accountService.getActiveUserAccount(username, owner.getDomainId());
        String[] keys;
        if (kubeadmin == null) {
            User kube = userDao.persist(new UserVO(owner.getAccountId(), username, UUID.randomUUID().toString(), owner.getAccountName(),
                    KUBEADMIN_ACCOUNT_NAME, "kubeadmin", null, UUID.randomUUID().toString(), User.Source.UNKNOWN));
            keys = createUserApiKeyAndSecretKey(kube.getId());
        } else {
            String apiKey = kubeadmin.getApiKey();
            String secretKey = kubeadmin.getSecretKey();
            if (StringUtils.isAnyEmpty(apiKey, secretKey)) {
                keys = createUserApiKeyAndSecretKey(kubeadmin.getId());
            } else {
                keys = new String[]{apiKey, secretKey};
            }
        }
        return keys;
    }

    protected Role createProjectKubernetesAccountRole() {
        Role role = roleService.createRole(PROJECT_KUBEADMIN_ACCOUNT_ROLE_NAME, RoleType.User,
                PROJECT_KUBEADMIN_ACCOUNT_ROLE_NAME, false);
        for (Class<?> allowedApi : PROJECT_KUBERNETES_ACCOUNT_ROLE_ALLOWED_APIS) {
            final String apiName = BaseCmd.getCommandNameByClass(allowedApi);
            roleService.createRolePermission(role, new Rule(apiName), RolePermissionEntity.Permission.ALLOW,
                    String.format("Allow %s", apiName));
        }
        roleService.createRolePermission(role, new Rule("*"), RolePermissionEntity.Permission.DENY,
                "Deny all");
        logger.debug(String.format("Created default role for Kubernetes service account in projects: %s", role));
        return role;
    }

    public Role getProjectKubernetesAccountRole() {
        List<Role> roles = roleService.findRolesByName(PROJECT_KUBEADMIN_ACCOUNT_ROLE_NAME);
        if (CollectionUtils.isNotEmpty(roles)) {
            Role role = roles.get(0);
            logger.debug(String.format("Found default role for Kubernetes service account in projects: %s", role));
            return role;
        }
        return createProjectKubernetesAccountRole();
    }

    protected Account createProjectKubernetesAccount(final Project project, final String accountName) {
        CallContext.register(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM);
        try {
            Role role = getProjectKubernetesAccountRole();
            UserAccount userAccount = accountService.createUserAccount(accountName,
                    UuidUtils.first(UUID.randomUUID().toString()), PROJECT_KUBERNETES_ACCOUNT_FIRST_NAME,
                    PROJECT_KUBERNETES_ACCOUNT_LAST_NAME, null, null, accountName, Account.Type.NORMAL, role.getId(),
                    project.getDomainId(), null, null, null, null, User.Source.NATIVE);
            projectManager.assignAccountToProject(project, userAccount.getAccountId(), ProjectAccount.Role.Regular,
                    userAccount.getId(), null);
            Account account = accountService.getAccount(userAccount.getAccountId());
            logger.debug(String.format("Created Kubernetes service account in project %s: %s", project, account));
            return account;
        } finally {
            CallContext.unregister();
        }
    }

    protected Account getProjectKubernetesAccount(final Account callerAccount, final boolean create) {
        Project project = ApiDBUtils.findProjectByProjectAccountId(callerAccount.getId());
        final String accountName = String.format("%s-%s", KUBEADMIN_ACCOUNT_NAME, UuidUtils.first(project.getUuid()));
        List<AccountVO> accounts = accountDao.findAccountsByName(accountName);
        for (AccountVO account : accounts) {
            if (projectManager.canAccessProjectAccount(account, project.getProjectAccountId())) {
                logger.debug(String.format("Created Kubernetes service account in project %s: %s", project, account));
                return account;
            }
        }
        return create ? createProjectKubernetesAccount(project, accountName) : null;
    }

    protected Account getProjectKubernetesAccount(final Account callerAccount) {
        return getProjectKubernetesAccount(callerAccount, true);
    }

    private String[] getServiceUserKeys(KubernetesClusterVO kubernetesCluster) {
        Account owner = accountService.getActiveAccountById(kubernetesCluster.getAccountId());
        if (owner == null) {
            owner = CallContext.current().getCallingAccount();
        }
        if (owner.getType() == Account.Type.PROJECT) {
            owner = getProjectKubernetesAccount(owner);
        }
        return getServiceUserKeys(owner);
    }

    @Override
    @ActionEvent(eventType = KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_STOP,
            eventDescription = "stopping Kubernetes cluster", async = true)
    public boolean stopKubernetesCluster(StopKubernetesClusterCmd cmd) throws CloudRuntimeException {
        long kubernetesClusterId = cmd.getId();
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }
        final KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(kubernetesClusterId);
        if (kubernetesCluster == null) {
            throw new InvalidParameterValueException("Failed to find Kubernetes cluster with given ID");
        }
        if (!isCommandSupported(kubernetesCluster, cmd.getActualCommandName())) {
            throw new InvalidParameterValueException(String.format("Stop kubernetes cluster is not supported for an externally managed cluster (%s)", kubernetesCluster.getName()));
        }
        if (kubernetesCluster.getRemoved() != null) {
            throw new InvalidParameterValueException(String.format("Kubernetes cluster : %s is already deleted", kubernetesCluster.getName()));
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, false, kubernetesCluster);
        if (kubernetesCluster.getState().equals(KubernetesCluster.State.Stopped)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Kubernetes cluster: {} is already stopped", kubernetesCluster);
            }
            return true;
        }
        if (kubernetesCluster.getState().equals(KubernetesCluster.State.Stopping)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Kubernetes cluster: {} is getting stopped", kubernetesCluster);
            }
            return true;
        }
        KubernetesClusterStopWorker stopWorker = new KubernetesClusterStopWorker(kubernetesCluster, this);
        stopWorker = ComponentContext.inject(stopWorker);
        return stopWorker.stop();
    }

    @Override
    @ActionEvent(eventType = KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_DELETE,
            eventDescription = "deleting Kubernetes cluster", async = true)
    public boolean deleteKubernetesCluster(DeleteKubernetesClusterCmd cmd) throws CloudRuntimeException {
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }
        Long kubernetesClusterId = cmd.getId();
        final KubernetesClusterVO cluster = kubernetesClusterDao.findById(kubernetesClusterId);
        if (cluster == null) {
            throw new InvalidParameterValueException("Invalid cluster id specified");
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, false, cluster);
        if (cluster.getClusterType() == KubernetesCluster.ClusterType.CloudManaged) {
            return destroyKubernetesCluster(cluster);
        } else {
            boolean cleanup = cmd.getCleanup();
            boolean expunge = cmd.getExpunge();
            if (cleanup || expunge) {
                CallContext ctx = CallContext.current();

                if (expunge && !accountManager.isAdmin(ctx.getCallingAccount().getId()) && !AllowUserExpungeRecoverVm.valueIn(cmd.getEntityOwnerId())) {
                    throw new PermissionDeniedException("Parameter " + ApiConstants.EXPUNGE + " can be passed by Admin only. Or when the allow.user.expunge.recover.vm key is set.");
                }

                List<KubernetesClusterVmMapVO> vmMapList = kubernetesClusterVmMapDao.listByClusterId(kubernetesClusterId);
                List<VMInstanceVO> vms = vmMapList.stream().map(vmMap -> vmInstanceDao.findById(vmMap.getVmId())).collect(Collectors.toList());
                if (checkIfVmsAssociatedWithBackupOffering(vms)) {
                    throw new CloudRuntimeException("Unable to delete Kubernetes cluster, as node(s) are associated to a backup offering");
                }
                for (KubernetesClusterVmMapVO vmMap : vmMapList) {
                    try {
                        userVmService.destroyVm(vmMap.getVmId(), expunge);
                        if (expunge) {
                            userVmService.expungeVm(vmMap.getVmId());
                        }
                    } catch (Exception exception) {
                        logMessage(Level.WARN, String.format("Failed to destroy vm %d", vmMap.getVmId()), exception);
                    }
                }
            }
            return Transaction.execute((TransactionCallback<Boolean>) status -> {
                kubernetesClusterDetailsDao.removeDetails(kubernetesClusterId);
                kubernetesClusterVmMapDao.removeByClusterId(kubernetesClusterId);
                if (kubernetesClusterDao.remove(kubernetesClusterId)) {
                    deleteProjectKubernetesAccountIfNeeded(cluster);
                    return true;
                }
                return false;
            });
        }
    }

    public static boolean checkIfVmsAssociatedWithBackupOffering(List<VMInstanceVO> vms) {
        for(VMInstanceVO vm : vms) {
            if (Objects.nonNull(vm.getBackupOfferingId())) {
               return true;
            }
        }
        return false;
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
        final String cmdClusterType = cmd.getClusterType();
        List<KubernetesClusterResponse> responsesList = new ArrayList<KubernetesClusterResponse>();
        List<Long> permittedAccounts = new ArrayList<Long>();
        Ternary<Long, Boolean, Project.ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, Project.ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountManager.buildACLSearchParameters(caller, clusterId, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        Project.ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        KubernetesCluster.ClusterType clusterType = null;

        if (cmdClusterType != null) {
            try {
             clusterType = KubernetesCluster.ClusterType.valueOf(cmdClusterType);
            } catch (IllegalArgumentException exception) {
                throw new InvalidParameterValueException("Unable to resolve cluster type " + cmdClusterType + " to a supported value (CloudManaged, ExternalManaged)");
            }
        }

        Filter searchFilter = new Filter(KubernetesClusterVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<KubernetesClusterVO> sb = kubernetesClusterDao.createSearchBuilder();
        accountManager.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("keyword", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.IN);
        sb.and("cluster_type", sb.entity().getClusterType(), SearchCriteria.Op.EQ);
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
        if (clusterType != null) {
            sc.setParameters("cluster_type", clusterType);
        }
        Pair<List<KubernetesClusterVO>, Integer> kubernetesClustersAndCount = kubernetesClusterDao.searchAndCount(sc, searchFilter);
        for (KubernetesClusterVO cluster : kubernetesClustersAndCount.first()) {
            KubernetesClusterResponse clusterResponse = createKubernetesClusterResponse(cluster.getId());
            responsesList.add(clusterResponse);
        }
        ListResponse<KubernetesClusterResponse> response = new ListResponse<>();
        response.setResponses(responsesList, kubernetesClustersAndCount.second());
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
        Account caller = CallContext.current().getCallingAccount();
        accountManager.checkAccess(caller, SecurityChecker.AccessType.OperateEntry, false, kubernetesCluster);
        KubernetesClusterConfigResponse response = new KubernetesClusterConfigResponse();
        response.setId(kubernetesCluster.getUuid());
        response.setName(kubernetesCluster.getName());
        String configData = "";
        KubernetesClusterDetailsVO clusterDetailsVO = kubernetesClusterDetailsDao.findDetail(kubernetesCluster.getId(), "kubeConfigData");
        if (clusterDetailsVO != null && StringUtils.isNotEmpty(clusterDetailsVO.getValue())) {
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
    @ActionEvent(eventType = KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_SCALE,
            eventDescription = "scaling Kubernetes cluster", async = true)
    public boolean scaleKubernetesCluster(ScaleKubernetesClusterCmd cmd) throws CloudRuntimeException {
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }
        validateKubernetesClusterScaleParameters(cmd);
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(cmd.getId());
        Map<String, ServiceOffering> nodeToOfferingMap = createNodeTypeToServiceOfferingMap(cmd.getServiceOfferingNodeTypeMap(), cmd.getServiceOfferingId(), kubernetesCluster);

        String[] keys = getServiceUserKeys(kubernetesCluster);
        KubernetesClusterScaleWorker scaleWorker =
            new KubernetesClusterScaleWorker(kubernetesCluster,
                nodeToOfferingMap,
                cmd.getClusterSize(),
                cmd.getNodeIds(),
                cmd.isAutoscalingEnabled(),
                cmd.getMinSize(),
                cmd.getMaxSize(),
                this);
        scaleWorker.setKeys(keys);
        scaleWorker = ComponentContext.inject(scaleWorker);
        return scaleWorker.scaleCluster();
    }

    /**
     * Creates a map for the requested node type service offering
     * For the node type DEFAULT: Every node is scaled to the same offering
     */
    protected Map<String, ServiceOffering> createNodeTypeToServiceOfferingMap(Map<String, Long> idsMapping,
                                                                              Long serviceOfferingId, KubernetesClusterVO kubernetesCluster) {
        Map<String, ServiceOffering> map = new HashMap<>();
        if (MapUtils.isEmpty(idsMapping)) {
            ServiceOfferingVO offering = serviceOfferingId != null ?
                    serviceOfferingDao.findById(serviceOfferingId) :
                    serviceOfferingDao.findById(kubernetesCluster.getServiceOfferingId());
            map.put(DEFAULT.name(), offering);
            return map;
        }
        for (String key : CLUSTER_NODES_TYPES_LIST) {
            if (!idsMapping.containsKey(key)) {
                continue;
            }
            map.put(key, serviceOfferingDao.findById(idsMapping.get(key)));
        }
        return map;
    }

    @Override
    @ActionEvent(eventType = KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_UPGRADE,
            eventDescription = "upgrading Kubernetes cluster", async = true)
    public boolean upgradeKubernetesCluster(UpgradeKubernetesClusterCmd cmd) throws CloudRuntimeException {
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }

        validateKubernetesClusterUpgradeParameters(cmd);
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(cmd.getId());
        String[] keys = getServiceUserKeys(kubernetesCluster);
        KubernetesClusterUpgradeWorker upgradeWorker =
            new KubernetesClusterUpgradeWorker(kubernetesClusterDao.findById(cmd.getId()),
                kubernetesSupportedVersionDao.findById(cmd.getKubernetesVersionId()), this, keys);
        upgradeWorker = ComponentContext.inject(upgradeWorker);
        return upgradeWorker.upgradeCluster();
    }

    private void updateNodeCount(KubernetesClusterVO kubernetesCluster) {
        List<KubernetesClusterVmMapVO> nodeList = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());
        kubernetesCluster.setControlNodeCount(nodeList.stream().filter(KubernetesClusterVmMapVO::isControlNode).count());
        kubernetesCluster.setNodeCount(nodeList.size() - kubernetesCluster.getControlNodeCount());
        kubernetesClusterDao.persist(kubernetesCluster);
    }

    @Override
    public boolean addVmsToCluster(AddVirtualMachinesToKubernetesClusterCmd cmd) {
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }
        List<Long> vmIds = cmd.getVmIds();
        Long clusterId = cmd.getId();

        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(clusterId);
        if (kubernetesCluster == null) {
            throw new InvalidParameterValueException("Invalid Kubernetes cluster ID specified");
        }
        if (!isCommandSupported(kubernetesCluster, cmd.getActualCommandName())) {
            throw new InvalidParameterValueException("VM cannot be added to a CloudStack managed Kubernetes cluster");
        }

        // User should have access to both VM and Kubernetes cluster
        accountManager.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, false, kubernetesCluster);

        for (Long vmId : vmIds) {
            VMInstanceVO vmInstance = vmInstanceDao.findById(vmId);
            if (vmInstance == null) {
                throw new InvalidParameterValueException("Invalid VM ID specified");
            }
            accountManager.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, false, vmInstance);
        }

        KubernetesClusterVmMapVO clusterVmMap = null;
        List<KubernetesClusterVmMapVO> clusterVmMapList = kubernetesClusterVmMapDao.listByClusterIdAndVmIdsIn(clusterId, vmIds);
        ArrayList<Long> alreadyExistingVmIds = new ArrayList<>();
        for (KubernetesClusterVmMapVO clusterVmMapVO : clusterVmMapList) {
            alreadyExistingVmIds.add(clusterVmMapVO.getVmId());
        }
        vmIds.removeAll(alreadyExistingVmIds);
        for (Long vmId : vmIds) {
            clusterVmMap = new KubernetesClusterVmMapVO(clusterId, vmId, cmd.isControlNode());
            kubernetesClusterVmMapDao.persist(clusterVmMap);
        }
        updateNodeCount(kubernetesCluster);
        return true;
    }

    @Override
    public boolean addNodesToKubernetesCluster(AddNodesToKubernetesClusterCmd cmd) {
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }
        KubernetesClusterVO kubernetesCluster = validateCluster(cmd.getClusterId());
        long networkId = kubernetesCluster.getNetworkId();
        NetworkVO networkVO = networkDao.findById(networkId);
        List<Long> validNodeIds = validateNodes(cmd.getNodeIds(), networkId, networkVO.getName(), kubernetesCluster, false);
        if (validNodeIds.isEmpty()) {
            throw new CloudRuntimeException("No valid nodes found to be added to the Kubernetes cluster");
        }
        KubernetesClusterAddWorker addWorker = new KubernetesClusterAddWorker(kubernetesCluster, KubernetesClusterManagerImpl.this);
        addWorker = ComponentContext.inject(addWorker);
        return addWorker.addNodesToCluster(validNodeIds, cmd.isMountCksIsoOnVr(), cmd.isManualUpgrade());
    }

    @Override
    public boolean removeNodesFromKubernetesCluster(RemoveNodesFromKubernetesClusterCmd cmd) throws Exception {
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }
        KubernetesClusterVO kubernetesCluster = validateCluster(cmd.getClusterId());
        List<Long> validNodeIds = validateNodes(cmd.getNodeIds(), null, null, kubernetesCluster, true);
        if (validNodeIds.isEmpty()) {
            throw new CloudRuntimeException("No valid nodes found to be removed from the Kubernetes cluster");
        }
        KubernetesClusterRemoveWorker removeWorker = new KubernetesClusterRemoveWorker(kubernetesCluster, KubernetesClusterManagerImpl.this);
        removeWorker = ComponentContext.inject(removeWorker);
        return removeWorker.removeNodesFromCluster(validNodeIds);
    }

    private KubernetesClusterVO validateCluster(long clusterId) {
        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(clusterId);
        if (kubernetesCluster == null) {
            throw new InvalidParameterValueException("Invalid Kubernetes cluster ID specified");
        }
        return kubernetesCluster;
    }

    private List<Long> validateNodes(List<Long> nodeIds, Long networkId, String networkName, KubernetesCluster cluster,  boolean removeNodes) {
        List<Long> validNodeIds = new ArrayList<>(nodeIds);
        for (Long id : nodeIds) {
            VMInstanceVO node = vmInstanceDao.findById(id);
            if (Objects.isNull(node)) {
                logger.error(String.format("Failed to find node (physical or virtual machine) with ID: %s", id));
                validNodeIds.remove(id);
            } else if (!removeNodes) {
                VMTemplateVO template = templateDao.findById(node.getTemplateId());
                if (Objects.isNull(template)) {
                    logger.error((String.format("Failed to find template with ID: %s", id)));
                    validNodeIds.remove(id);
                } else if (!template.isForCks()) {
                    logger.error(String.format("Node: %s is deployed with a template that is not marked to be used for CKS", node.getId()));
                    validNodeIds.remove(id);
                }
                NicVO nicVO = nicDao.findDefaultNicForVM(id);
                if (networkId != nicVO.getNetworkId()) {
                    logger.error(String.format("Node: %s does not have its default NIC in the kubernetes cluster network: %s", node.getId(), networkName));
                    validNodeIds.remove(id);
                }
                List<KubernetesClusterVmMapVO> clusterVmMapVO = kubernetesClusterVmMapDao.listByClusterIdAndVmIdsIn(cluster.getId(), Collections.singletonList(id));
                if (Objects.nonNull(clusterVmMapVO) && !clusterVmMapVO.isEmpty()) {
                    logger.warn(String.format("Node: %s is already part of the cluster %s", node.getId(), cluster.getName()));
                    validNodeIds.remove(id);
                }
            }
        }
        return validNodeIds;
    }

    @Override
    public List<RemoveVirtualMachinesFromKubernetesClusterResponse> removeVmsFromCluster(RemoveVirtualMachinesFromKubernetesClusterCmd cmd) {
        if (!KubernetesServiceEnabled.value()) {
            logAndThrow(Level.ERROR, "Kubernetes Service plugin is disabled");
        }
        List<Long> vmIds = cmd.getVmIds();
        Long clusterId = cmd.getId();

        KubernetesClusterVO kubernetesCluster = kubernetesClusterDao.findById(clusterId);
        if (kubernetesCluster == null) {
            throw new InvalidParameterValueException("Invalid Kubernetes cluster ID specified");
        }
        if (!isCommandSupported(kubernetesCluster, cmd.getActualCommandName())) {
            throw new InvalidParameterValueException("VM cannot be removed from a CloudStack Managed Kubernetes cluster");
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, false, kubernetesCluster);

        List<KubernetesClusterVmMapVO> kubernetesClusterVmMap = kubernetesClusterVmMapDao.listByClusterIdAndVmIdsIn(clusterId, vmIds);
        List<RemoveVirtualMachinesFromKubernetesClusterResponse> responseList = new ArrayList<>();

        Set<Long> vmIdsRemoved = new HashSet<>();

        for (KubernetesClusterVmMapVO clusterVmMap : kubernetesClusterVmMap) {
            RemoveVirtualMachinesFromKubernetesClusterResponse response = new RemoveVirtualMachinesFromKubernetesClusterResponse();
            UserVm vm = userVmService.getUserVm(clusterVmMap.getVmId());
            response.setVmId(vm.getUuid());
            response.setSuccess(kubernetesClusterVmMapDao.remove(clusterVmMap.getId()));
            response.setObjectName(cmd.getCommandName());
            responseList.add(response);
            vmIdsRemoved.add(clusterVmMap.getVmId());
        }

        for (Long vmId : vmIds) {
            if (!vmIdsRemoved.contains(vmId)) {
                RemoveVirtualMachinesFromKubernetesClusterResponse response = new RemoveVirtualMachinesFromKubernetesClusterResponse();
                VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(vmId);
                if (vm == null) {
                    response.setVmId(vmId.toString());
                    response.setDisplayText("Not a valid vm id");
                    vmIdsRemoved.add(vmId);
                } else {
                    response.setVmId(vm.getUuid());
                    vmIdsRemoved.add(vmId);
                    if (vm.isRemoved()) {
                        response.setDisplayText("VM is already removed");
                    } else {
                        response.setDisplayText("VM is not part of the cluster");
                    }
                }
                response.setObjectName(cmd.getCommandName());
                response.setSuccess(false);
                responseList.add(response);
            }
        }
        updateNodeCount(kubernetesCluster);
        return responseList;
    }

    protected void deleteProjectKubernetesAccount(Account projectAccount) {
        CallContext.register(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM);
        try {
            Account serviceAccount = getProjectKubernetesAccount(projectAccount, false);
            if (serviceAccount != null) {
                accountManager.deleteAccount(accountDao.findById(serviceAccount.getId()), User.UID_SYSTEM,
                        accountService.getSystemAccount());
            }
        } finally {
            CallContext.unregister();
        }
    }

    protected void deleteProjectKubernetesAccountIfNeeded(final KubernetesCluster kubernetesCluster) {
        Account owner = accountService.getAccount(kubernetesCluster.getAccountId());
        if (owner == null) {
            return;
        }
        if (Account.Type.PROJECT.equals(owner.getType()) &&
                kubernetesClusterDao.countNotForGCByAccount(owner.getAccountId()) == 0) {
            deleteProjectKubernetesAccount(owner);
        }
    }

    protected boolean destroyKubernetesCluster(KubernetesCluster kubernetesCluster, boolean deleteProjectAccount) {
        KubernetesClusterDestroyWorker destroyWorker = new KubernetesClusterDestroyWorker(kubernetesCluster,
                KubernetesClusterManagerImpl.this);
        destroyWorker = ComponentContext.inject(destroyWorker);
        boolean result = destroyWorker.destroy();
        if (deleteProjectAccount) {
            deleteProjectKubernetesAccountIfNeeded(kubernetesCluster);
        }
        return result;
    }

    protected boolean destroyKubernetesCluster(KubernetesCluster kubernetesCluster) {
        return destroyKubernetesCluster(kubernetesCluster, true);
    }

    @Override
    public void cleanupForAccount(Account account) {
        List<KubernetesClusterVO> clusters = kubernetesClusterDao.listForCleanupByAccount(account.getId());
        if (CollectionUtils.isEmpty(clusters)) {
            return;
        }
        logger.debug(String.format("Cleaning up %d Kubernetes cluster for %s", clusters.size(), account));
        for (KubernetesClusterVO cluster : clusters) {
            try {
                destroyKubernetesCluster(cluster, false);
            } catch (CloudRuntimeException e) {
                logger.warn(String.format("Failed to destroy Kubernetes cluster: %s during cleanup for %s",
                        cluster.getName(), account), e);
            }
        }
        if (!Account.Type.PROJECT.equals(account.getType())) {
            return;
        }
        deleteProjectKubernetesAccount(account);
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
        cmdList.add(AddVirtualMachinesToKubernetesClusterCmd.class);
        cmdList.add(RemoveVirtualMachinesFromKubernetesClusterCmd.class);
        cmdList.add(AddNodesToKubernetesClusterCmd.class);
        cmdList.add(RemoveNodesFromKubernetesClusterCmd.class);
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
                    if (logger.isInfoEnabled()) {
                        logger.info("Running Kubernetes cluster garbage collector on Kubernetes cluster: {}", kubernetesCluster);
                    }
                    try {
                        if (destroyKubernetesCluster(kubernetesCluster)) {
                            logger.info("Garbage collection complete for Kubernetes cluster: {}", kubernetesCluster);
                        } else {
                            logger.warn("Garbage collection failed for Kubernetes cluster : {}, it will be attempted to garbage collected in next run", kubernetesCluster);
                        }
                    } catch (CloudRuntimeException e) {
                        logger.warn("Failed to destroy Kubernetes cluster : {} during GC", kubernetesCluster, e);
                        // proceed further with rest of the Kubernetes cluster garbage collection
                    }
                }
            } catch (Exception e) {
                logger.warn("Caught exception while running Kubernetes cluster gc: ", e);
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
                List<KubernetesClusterVO> runningKubernetesClusters = kubernetesClusterDao.findManagedKubernetesClustersInState(KubernetesCluster.State.Running);
                for (KubernetesCluster kubernetesCluster : runningKubernetesClusters) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Running Kubernetes cluster state scanner on Kubernetes cluster: {}", kubernetesCluster);
                    }
                    try {
                        if (!isClusterVMsInDesiredState(kubernetesCluster, VirtualMachine.State.Running)) {
                            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.FaultsDetected);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to run Kubernetes cluster Running state scanner on Kubernetes cluster: {} status scanner", kubernetesCluster, e);
                    }
                }

                // run through Kubernetes clusters in 'Stopped' state and ensure all the VM's are Stopped in the cluster
                List<KubernetesClusterVO> stoppedKubernetesClusters = kubernetesClusterDao.findManagedKubernetesClustersInState(KubernetesCluster.State.Stopped);
                for (KubernetesCluster kubernetesCluster : stoppedKubernetesClusters) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Running Kubernetes cluster state scanner on Kubernetes cluster: {} for state: {}", kubernetesCluster, KubernetesCluster.State.Stopped.toString());
                    }
                    try {
                        if (!isClusterVMsInDesiredState(kubernetesCluster, VirtualMachine.State.Stopped)) {
                            stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.FaultsDetected);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to run Kubernetes cluster Stopped state scanner on Kubernetes cluster: {} status scanner", kubernetesCluster, e);
                    }
                }

                // run through Kubernetes clusters in 'Alert' state and reconcile state as 'Running' if the VM's are running or 'Stopped' if VM's are stopped
                List<KubernetesClusterVO> alertKubernetesClusters = kubernetesClusterDao.findManagedKubernetesClustersInState(KubernetesCluster.State.Alert);
                for (KubernetesClusterVO kubernetesCluster : alertKubernetesClusters) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Running Kubernetes cluster state scanner on Kubernetes cluster: {} for state: {}", kubernetesCluster, KubernetesCluster.State.Alert.toString());
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
                        logger.warn("Failed to run Kubernetes cluster Alert state scanner on Kubernetes cluster: {} status scanner", kubernetesCluster, e);
                    }
                }


                if (firstRun) {
                    // run through Kubernetes clusters in 'Starting' state and reconcile state as 'Alert' or 'Error' if the VM's are running
                    List<KubernetesClusterVO> startingKubernetesClusters = kubernetesClusterDao.findManagedKubernetesClustersInState(KubernetesCluster.State.Starting);
                    for (KubernetesCluster kubernetesCluster : startingKubernetesClusters) {
                        if ((new Date()).getTime() - kubernetesCluster.getCreated().getTime() < 10*60*1000) {
                            continue;
                        }
                        if (logger.isInfoEnabled()) {
                            logger.info("Running Kubernetes cluster state scanner on Kubernetes cluster: {} for state: {}", kubernetesCluster, KubernetesCluster.State.Starting.toString());
                        }
                        try {
                            if (isClusterVMsInDesiredState(kubernetesCluster, VirtualMachine.State.Running)) {
                                stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.FaultsDetected);
                            } else {
                                stateTransitTo(kubernetesCluster.getId(), KubernetesCluster.Event.OperationFailed);
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to run Kubernetes cluster Starting state scanner on Kubernetes cluster: {} status scanner", kubernetesCluster, e);
                        }
                    }
                    List<KubernetesClusterVO> destroyingKubernetesClusters = kubernetesClusterDao.findManagedKubernetesClustersInState(KubernetesCluster.State.Destroying);
                    for (KubernetesCluster kubernetesCluster : destroyingKubernetesClusters) {
                        if (logger.isInfoEnabled()) {
                            logger.info("Running Kubernetes cluster state scanner on Kubernetes cluster: {} for state: {}", kubernetesCluster, KubernetesCluster.State.Destroying.toString());
                        }
                        try {
                            destroyKubernetesCluster(kubernetesCluster);
                        } catch (Exception e) {
                            logger.warn("Failed to run Kubernetes cluster Destroying state scanner on Kubernetes cluster : {} status scanner", kubernetesCluster, e);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Caught exception while running Kubernetes cluster state scanner", e);
            }
            firstRun = false;
        }
    }

    // checks if Kubernetes cluster is in desired state
    private boolean isClusterVMsInDesiredState(KubernetesCluster kubernetesCluster, VirtualMachine.State state) {
        List<KubernetesClusterVmMapVO> clusterVMs = kubernetesClusterVmMapDao.listByClusterId(kubernetesCluster.getId());

        // check cluster is running at desired capacity include control nodes as well
        if (clusterVMs.size() < kubernetesCluster.getTotalNodeCount()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Found only {} VMs in the Kubernetes cluster {} while expected {} VMs to be in state: {}",
                        clusterVMs.size(), kubernetesCluster, kubernetesCluster.getTotalNodeCount(), state.toString());
            }
            return false;
        }
        // check if all the VM's are in same state
        for (KubernetesClusterVmMapVO clusterVm : clusterVMs) {
            VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(clusterVm.getVmId());
            if (vm.getState() != state) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Found VM: {} in the Kubernetes cluster {} in state: {} while " +
                            "expected to be in state: {}. So moving the cluster to Alert state for reconciliation",
                            vm, kubernetesCluster, vm.getState().toString(), state.toString());
                }
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean start() {
        createNetworkOfferingForKubernetes(DEFAULT_NETWORK_OFFERING_FOR_KUBERNETES_SERVICE_NAME,
                DEFAULT_NETWORK_OFFERING_FOR_KUBERNETES_SERVICE_DISPLAY_TEXT, false, false);

        createNetworkOfferingForKubernetes(DEFAULT_NSX_NETWORK_OFFERING_FOR_KUBERNETES_SERVICE_NAME,
                DEFAULT_NSX_NETWORK_OFFERING_FOR_KUBERNETES_SERVICE_DISPLAY_TEXT, true, false);

        createNetworkOfferingForKubernetes(DEFAULT_NSX_VPC_TIER_NETWORK_OFFERING_FOR_KUBERNETES_SERVICE_NAME,
                DEFAULT_NSX_VPC_NETWORK_OFFERING_FOR_KUBERNETES_SERVICE_DISPLAY_TEXT , true, true);

        getProjectKubernetesAccountRole();

        _gcExecutor.scheduleWithFixedDelay(new KubernetesClusterGarbageCollector(), 300, 300, TimeUnit.SECONDS);
        _stateScanner.scheduleWithFixedDelay(new KubernetesClusterStatusScanner(), 300, 30, TimeUnit.SECONDS);

        return true;
    }

    private void createNetworkOfferingForKubernetes(String offeringName, String offeringDesc, boolean forNsx, boolean forVpc) {
        final Map<Network.Service, Network.Provider> defaultKubernetesServiceNetworkOfferingProviders = new HashMap<Service, Network.Provider>();
        Network.Provider provider = forVpc ? Network.Provider.VPCVirtualRouter : Network.Provider.VirtualRouter;
        defaultKubernetesServiceNetworkOfferingProviders.put(Service.Dhcp, provider);
        defaultKubernetesServiceNetworkOfferingProviders.put(Service.Dns, provider);
        defaultKubernetesServiceNetworkOfferingProviders.put(Service.UserData, provider);
        if (forVpc) {
            defaultKubernetesServiceNetworkOfferingProviders.put(Service.NetworkACL, forNsx ? Network.Provider.Nsx : provider);
        } else {
            defaultKubernetesServiceNetworkOfferingProviders.put(Service.Firewall, forNsx ? Network.Provider.Nsx : provider);
        }
        defaultKubernetesServiceNetworkOfferingProviders.put(Service.Lb, forNsx ? Network.Provider.Nsx : provider);
        defaultKubernetesServiceNetworkOfferingProviders.put(Service.SourceNat, forNsx ? Network.Provider.Nsx : provider);
        defaultKubernetesServiceNetworkOfferingProviders.put(Service.StaticNat, forNsx ? Network.Provider.Nsx : provider);
        defaultKubernetesServiceNetworkOfferingProviders.put(Service.PortForwarding, forNsx ? Network.Provider.Nsx : provider);

        if (!forNsx) {
            defaultKubernetesServiceNetworkOfferingProviders.put(Service.Gateway, Network.Provider.VirtualRouter);
            defaultKubernetesServiceNetworkOfferingProviders.put(Service.Vpn, Network.Provider.VirtualRouter);
        }

        NetworkOfferingVO defaultKubernetesServiceNetworkOffering =
                new NetworkOfferingVO(offeringName,
                        offeringDesc, Networks.TrafficType.Guest,
                        false, false, null, null, true,
                        NetworkOffering.Availability.Required, null, Network.GuestType.Isolated, true,
                        true, false, false, false, false,
                        false, false, false, true, true, false,
                        forVpc, true, false, false);
        if (forNsx) {
            defaultKubernetesServiceNetworkOffering.setNetworkMode(NetworkOffering.NetworkMode.NATTED);
        }
        defaultKubernetesServiceNetworkOffering.setSupportsVmAutoScaling(true);
        defaultKubernetesServiceNetworkOffering.setState(NetworkOffering.State.Enabled);
        defaultKubernetesServiceNetworkOffering = networkOfferingDao.persistDefaultNetworkOffering(defaultKubernetesServiceNetworkOffering);

        for (Service service : defaultKubernetesServiceNetworkOfferingProviders.keySet()) {
            NetworkOfferingServiceMapVO offService =
                    new NetworkOfferingServiceMapVO(defaultKubernetesServiceNetworkOffering.getId(), service,
                            defaultKubernetesServiceNetworkOfferingProviders.get(service));
            networkOfferingServiceMapDao.persist(offService);
            logger.trace("Added service for the network offering: " + offService);
        }
    }

    @Override
    public boolean isDirectAccess(Network network) {
        return Network.GuestType.Shared.equals(network.getGuestType()) || routedIpv4Manager.isRoutedNetwork(network);
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
            KubernetesClusterNetworkOffering,
            KubernetesClusterStartTimeout,
            KubernetesClusterScaleTimeout,
            KubernetesClusterUpgradeTimeout,
            KubernetesClusterUpgradeRetries,
            KubernetesClusterAddNodeTimeout,
            KubernetesClusterRemoveNodeTimeout,
            KubernetesClusterExperimentalFeaturesEnabled,
            KubernetesMaxClusterSize,
            KubernetesControlNodeInstallAttemptWait,
            KubernetesControlNodeInstallReattempts,
            KubernetesWorkerNodeInstallAttemptWait,
            KubernetesWorkerNodeInstallReattempts,
            KubernetesEtcdNodeStartPort
        };
    }
}
