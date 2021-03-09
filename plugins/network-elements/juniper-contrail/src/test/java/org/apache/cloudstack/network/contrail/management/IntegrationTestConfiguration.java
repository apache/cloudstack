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

package org.apache.cloudstack.network.contrail.management;

import java.io.IOException;

import javax.inject.Inject;

import org.eclipse.jetty.security.IdentityService;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import org.apache.cloudstack.acl.APIChecker;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.affinity.AffinityGroupService;
import org.apache.cloudstack.affinity.AffinityGroupVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupDaoImpl;
import org.apache.cloudstack.affinity.dao.AffinityGroupDomainMapDaoImpl;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDaoImpl;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.DcDetailsDaoImpl;
import org.apache.cloudstack.engine.orchestration.NetworkOrchestrator;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.engine.service.api.OrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigDepotAdmin;
import org.apache.cloudstack.framework.config.dao.ConfigurationDaoImpl;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobDaoImpl;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobJoinMapDaoImpl;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobJournalDaoImpl;
import org.apache.cloudstack.framework.jobs.dao.SyncQueueItemDaoImpl;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobManagerImpl;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobMonitor;
import org.apache.cloudstack.framework.jobs.impl.SyncQueueManager;
import org.apache.cloudstack.lb.dao.ApplicationLoadBalancerRuleDaoImpl;
import org.apache.cloudstack.network.element.InternalLoadBalancerElement;
import org.apache.cloudstack.network.lb.ApplicationLoadBalancerService;
import org.apache.cloudstack.network.lb.InternalLoadBalancerVMManager;
import org.apache.cloudstack.network.lb.InternalLoadBalancerVMService;
import org.apache.cloudstack.query.QueryService;
import org.apache.cloudstack.region.PortableIpDaoImpl;
import org.apache.cloudstack.region.PortableIpRangeDaoImpl;
import org.apache.cloudstack.region.RegionManager;
import org.apache.cloudstack.region.dao.RegionDaoImpl;
import org.apache.cloudstack.spring.lifecycle.registry.ExtensionRegistry;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreProviderManager;
import org.apache.cloudstack.storage.image.datastore.ImageStoreProviderManager;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDaoImpl;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDaoImpl;
import org.apache.cloudstack.storage.image.db.TemplateDataStoreDaoImpl;
import org.apache.cloudstack.usage.UsageService;

import com.cloud.acl.DomainChecker;
import com.cloud.agent.AgentManager;
import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.agent.manager.allocator.PodAllocator;
import com.cloud.alert.AlertManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.dao.AccountJoinDaoImpl;
import com.cloud.api.query.dao.AffinityGroupJoinDaoImpl;
import com.cloud.api.query.dao.AsyncJobJoinDaoImpl;
import com.cloud.api.query.dao.DataCenterJoinDaoImpl;
import com.cloud.api.query.dao.DiskOfferingJoinDaoImpl;
import com.cloud.api.query.dao.DomainRouterJoinDaoImpl;
import com.cloud.api.query.dao.HostJoinDaoImpl;
import com.cloud.api.query.dao.ImageStoreJoinDaoImpl;
import com.cloud.api.query.dao.InstanceGroupJoinDaoImpl;
import com.cloud.api.query.dao.ProjectAccountJoinDaoImpl;
import com.cloud.api.query.dao.ProjectInvitationJoinDaoImpl;
import com.cloud.api.query.dao.ProjectJoinDaoImpl;
import com.cloud.api.query.dao.ResourceTagJoinDaoImpl;
import com.cloud.api.query.dao.SecurityGroupJoinDaoImpl;
import com.cloud.api.query.dao.ServiceOfferingJoinDaoImpl;
import com.cloud.api.query.dao.StoragePoolJoinDaoImpl;
import com.cloud.api.query.dao.TemplateJoinDaoImpl;
import com.cloud.api.query.dao.UserAccountJoinDaoImpl;
import com.cloud.api.query.dao.UserVmJoinDaoImpl;
import com.cloud.api.query.dao.VolumeJoinDaoImpl;
import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.dao.CapacityDaoImpl;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.agentlb.dao.HostTransferMapDaoImpl;
import com.cloud.cluster.dao.ManagementServerHostDaoImpl;
import com.cloud.configuration.ConfigurationManagerImpl;
import com.cloud.configuration.dao.ResourceCountDaoImpl;
import com.cloud.configuration.dao.ResourceLimitDaoImpl;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.consoleproxy.ConsoleProxyService;
import com.cloud.dc.ClusterDetailsDaoImpl;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.AccountVlanMapDaoImpl;
import com.cloud.dc.dao.ClusterDaoImpl;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterDaoImpl;
import com.cloud.dc.dao.DataCenterDetailsDaoImpl;
import com.cloud.dc.dao.DataCenterIpAddressDaoImpl;
import com.cloud.dc.dao.DataCenterLinkLocalIpAddressDaoImpl;
import com.cloud.dc.dao.DataCenterVnetDaoImpl;
import com.cloud.dc.dao.DedicatedResourceDaoImpl;
import com.cloud.dc.dao.HostPodDaoImpl;
import com.cloud.dc.dao.PodVlanDaoImpl;
import com.cloud.dc.dao.PodVlanMapDaoImpl;
import com.cloud.dc.dao.VlanDaoImpl;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanningManager;
import com.cloud.deploy.dao.PlannerHostReservationDaoImpl;
import com.cloud.domain.dao.DomainDaoImpl;
import com.cloud.domain.dao.DomainDetailsDaoImpl;
import com.cloud.event.dao.EventDaoImpl;
import com.cloud.event.dao.EventJoinDaoImpl;
import com.cloud.event.dao.UsageEventDaoImpl;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.dao.HostDaoImpl;
import com.cloud.host.dao.HostDetailsDaoImpl;
import com.cloud.host.dao.HostTagsDaoImpl;
import com.cloud.hypervisor.HypervisorGuruManagerImpl;
import com.cloud.hypervisor.XenServerGuru;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDaoImpl;
import com.cloud.network.ExternalDeviceUsageManager;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManagerImpl;
import com.cloud.network.Ipv6AddressManagerImpl;
import com.cloud.network.NetworkServiceImpl;
import com.cloud.network.NetworkUsageService;
import com.cloud.network.StorageNetworkManager;
import com.cloud.network.StorageNetworkService;
import com.cloud.network.as.AutoScaleService;
import com.cloud.network.as.dao.AutoScalePolicyConditionMapDaoImpl;
import com.cloud.network.as.dao.AutoScalePolicyDaoImpl;
import com.cloud.network.as.dao.AutoScaleVmGroupDaoImpl;
import com.cloud.network.as.dao.AutoScaleVmGroupPolicyMapDaoImpl;
import com.cloud.network.as.dao.AutoScaleVmProfileDaoImpl;
import com.cloud.network.as.dao.ConditionDaoImpl;
import com.cloud.network.as.dao.CounterDaoImpl;
import com.cloud.network.dao.AccountGuestVlanMapDaoImpl;
import com.cloud.network.dao.FirewallRulesCidrsDaoImpl;
import com.cloud.network.dao.FirewallRulesDaoImpl;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressDaoImpl;
import com.cloud.network.dao.LBHealthCheckPolicyDaoImpl;
import com.cloud.network.dao.LBStickinessPolicyDaoImpl;
import com.cloud.network.dao.LoadBalancerDaoImpl;
import com.cloud.network.dao.LoadBalancerVMMapDaoImpl;
import com.cloud.network.dao.NetworkAccountDaoImpl;
import com.cloud.network.dao.NetworkDaoImpl;
import com.cloud.network.dao.NetworkDomainDaoImpl;
import com.cloud.network.dao.NetworkOpDaoImpl;
import com.cloud.network.dao.NetworkRuleConfigDaoImpl;
import com.cloud.network.dao.NetworkServiceMapDaoImpl;
import com.cloud.network.dao.PhysicalNetworkDaoImpl;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDaoImpl;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDaoImpl;
import com.cloud.network.dao.RemoteAccessVpnDaoImpl;
import com.cloud.network.dao.RouterNetworkDaoImpl;
import com.cloud.network.dao.Site2SiteCustomerGatewayDaoImpl;
import com.cloud.network.dao.Site2SiteVpnConnectionDaoImpl;
import com.cloud.network.dao.Site2SiteVpnGatewayDaoImpl;
import com.cloud.network.dao.UserIpv6AddressDaoImpl;
import com.cloud.network.dao.VirtualRouterProviderDaoImpl;
import com.cloud.network.dao.VpnUserDaoImpl;
import com.cloud.network.element.FirewallServiceProvider;
import com.cloud.network.element.NetworkACLServiceProvider;
import com.cloud.network.element.PortForwardingServiceProvider;
import com.cloud.network.element.Site2SiteVpnServiceProvider;
import com.cloud.network.element.VpcProvider;
import com.cloud.network.firewall.FirewallManagerImpl;
import com.cloud.network.lb.LoadBalancingRulesManagerImpl;
import com.cloud.network.router.VpcVirtualNetworkApplianceManagerImpl;
import com.cloud.network.rules.RulesManagerImpl;
import com.cloud.network.rules.dao.PortForwardingRulesDaoImpl;
import com.cloud.network.security.dao.SecurityGroupDaoImpl;
import com.cloud.network.security.dao.SecurityGroupRuleDaoImpl;
import com.cloud.network.security.dao.SecurityGroupRulesDaoImpl;
import com.cloud.network.security.dao.SecurityGroupVMMapDaoImpl;
import com.cloud.network.security.dao.SecurityGroupWorkDaoImpl;
import com.cloud.network.security.dao.VmRulesetLogDaoImpl;
import com.cloud.network.vpc.NetworkACLManagerImpl;
import com.cloud.network.vpc.NetworkACLService;
import com.cloud.network.vpc.VpcManagerImpl;
import com.cloud.network.vpc.dao.NetworkACLDaoImpl;
import com.cloud.network.vpc.dao.NetworkACLItemDaoImpl;
import com.cloud.network.vpc.dao.PrivateIpDaoImpl;
import com.cloud.network.vpc.dao.StaticRouteDaoImpl;
import com.cloud.network.vpc.dao.VpcDaoImpl;
import com.cloud.network.vpc.dao.VpcGatewayDaoImpl;
import com.cloud.network.vpc.dao.VpcOfferingDaoImpl;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDaoImpl;
import com.cloud.network.vpc.dao.VpcServiceMapDaoImpl;
import com.cloud.network.vpn.RemoteAccessVpnService;
import com.cloud.network.vpn.Site2SiteVpnManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingDaoImpl;
import com.cloud.offerings.dao.NetworkOfferingDetailsDaoImpl;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDaoImpl;
import com.cloud.projects.ProjectManagerImpl;
import com.cloud.projects.dao.ProjectAccountDaoImpl;
import com.cloud.projects.dao.ProjectDaoImpl;
import com.cloud.projects.dao.ProjectInvitationDaoImpl;
import com.cloud.region.ha.GlobalLoadBalancingRulesService;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ConfigurationServerImpl;
import com.cloud.server.ManagementServer;
import com.cloud.server.ResourceMetaDataService;
import com.cloud.server.StatsCollector;
import com.cloud.server.TaggedResourceService;
import com.cloud.server.auth.UserAuthenticator;
import com.cloud.service.dao.ServiceOfferingDaoImpl;
import com.cloud.service.dao.ServiceOfferingDetailsDaoImpl;
import com.cloud.storage.DataStoreProviderApiService;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.dao.DiskOfferingDaoImpl;
import com.cloud.storage.dao.GuestOSCategoryDaoImpl;
import com.cloud.storage.dao.GuestOSDaoImpl;
import com.cloud.storage.dao.LaunchPermissionDaoImpl;
import com.cloud.storage.dao.SnapshotDaoImpl;
import com.cloud.storage.dao.SnapshotPolicyDaoImpl;
import com.cloud.storage.dao.StoragePoolDetailsDaoImpl;
import com.cloud.storage.dao.StoragePoolHostDaoImpl;
import com.cloud.storage.dao.UploadDaoImpl;
import com.cloud.storage.dao.VMTemplateDaoImpl;
import com.cloud.storage.dao.VMTemplateDetailsDaoImpl;
import com.cloud.storage.dao.VMTemplateZoneDaoImpl;
import com.cloud.storage.dao.VolumeDaoImpl;
import com.cloud.storage.snapshot.SnapshotApiService;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.tags.dao.ResourceTagsDaoImpl;
import com.cloud.template.TemplateApiService;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountDetailsDaoImpl;
import com.cloud.user.DomainManagerImpl;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.AccountDaoImpl;
import com.cloud.user.dao.SSHKeyPairDaoImpl;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserDaoImpl;
import com.cloud.user.dao.UserStatisticsDaoImpl;
import com.cloud.user.dao.UserStatsLogDaoImpl;
import com.cloud.user.dao.VmDiskStatisticsDaoImpl;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.vm.ItWorkDaoImpl;
import com.cloud.vm.UserVmManagerImpl;
import com.cloud.vm.VirtualMachineManagerImpl;
import com.cloud.vm.dao.ConsoleProxyDaoImpl;
import com.cloud.vm.dao.DomainRouterDaoImpl;
import com.cloud.vm.dao.InstanceGroupDaoImpl;
import com.cloud.vm.dao.InstanceGroupVMMapDaoImpl;
import com.cloud.vm.dao.NicDaoImpl;
import com.cloud.vm.dao.NicIpAliasDaoImpl;
import com.cloud.vm.dao.NicSecondaryIpDaoImpl;
import com.cloud.vm.dao.SecondaryStorageVmDaoImpl;
import com.cloud.vm.dao.UserVmCloneSettingDaoImpl;
import com.cloud.vm.dao.UserVmDaoImpl;
import com.cloud.vm.dao.UserVmDetailsDaoImpl;
import com.cloud.vm.dao.VMInstanceDaoImpl;
import com.cloud.vm.snapshot.VMSnapshotManager;
import com.cloud.vm.snapshot.dao.VMSnapshotDaoImpl;

@ComponentScan(basePackageClasses = {AccountDaoImpl.class, AccountDetailsDaoImpl.class, AccountGuestVlanMapDaoImpl.class, AccountJoinDaoImpl.class,
    AccountVlanMapDaoImpl.class, AffinityGroupDaoImpl.class, AffinityGroupDomainMapDaoImpl.class, AffinityGroupJoinDaoImpl.class, AffinityGroupVMMapDaoImpl.class,
    ApiDBUtils.class, ApplicationLoadBalancerRuleDaoImpl.class, AsyncJobDaoImpl.class, AsyncJobJoinDaoImpl.class, AsyncJobJoinMapDaoImpl.class,
    AsyncJobJournalDaoImpl.class, AsyncJobManagerImpl.class, AutoScalePolicyConditionMapDaoImpl.class, AutoScalePolicyDaoImpl.class, AutoScaleVmGroupDaoImpl.class,
    AutoScaleVmGroupPolicyMapDaoImpl.class, AutoScaleVmProfileDaoImpl.class, CapacityDaoImpl.class, ClusterDaoImpl.class, ClusterDetailsDaoImpl.class,
    ConditionDaoImpl.class, ConfigurationDaoImpl.class, ConfigurationManagerImpl.class, ConfigurationServerImpl.class, ConsoleProxyDaoImpl.class,
    ContrailElementImpl.class, ContrailGuru.class, ContrailManagerImpl.class, CounterDaoImpl.class, DataCenterDaoImpl.class, DataCenterDetailsDaoImpl.class, DataCenterIpAddressDaoImpl.class,
    DataCenterJoinDaoImpl.class, DataCenterLinkLocalIpAddressDaoImpl.class, DataCenterVnetDaoImpl.class, DcDetailsDaoImpl.class, DedicatedResourceDaoImpl.class,
    DiskOfferingDaoImpl.class, DiskOfferingJoinDaoImpl.class, DomainDaoImpl.class, DomainDetailsDaoImpl.class, DomainManagerImpl.class, DomainRouterDaoImpl.class, DomainRouterJoinDaoImpl.class,
    EventDaoImpl.class, EventJoinDaoImpl.class, EventUtils.class, ExtensionRegistry.class, FirewallManagerImpl.class, FirewallRulesCidrsDaoImpl.class,
    FirewallRulesDaoImpl.class, GuestOSCategoryDaoImpl.class, GuestOSDaoImpl.class, HostDaoImpl.class, HostDetailsDaoImpl.class, HostJoinDaoImpl.class,
    HostPodDaoImpl.class, HostTagsDaoImpl.class, HostTransferMapDaoImpl.class, HypervisorCapabilitiesDaoImpl.class, HypervisorGuruManagerImpl.class,
 ImageStoreDaoImpl.class, ImageStoreJoinDaoImpl.class, InstanceGroupDaoImpl.class, InstanceGroupJoinDaoImpl.class,
    InstanceGroupVMMapDaoImpl.class, InternalLoadBalancerElement.class, IPAddressDaoImpl.class, IpAddressManagerImpl.class, Ipv6AddressManagerImpl.class, ItWorkDaoImpl.class, LBHealthCheckPolicyDaoImpl.class,
    LBStickinessPolicyDaoImpl.class, LaunchPermissionDaoImpl.class, LoadBalancerDaoImpl.class, LoadBalancerVMMapDaoImpl.class, LoadBalancingRulesManagerImpl.class,
    ManagementServerHostDaoImpl.class, MockAccountManager.class, NetworkACLDaoImpl.class, NetworkACLItemDaoImpl.class, NetworkACLManagerImpl.class,
    NetworkAccountDaoImpl.class, NetworkDaoImpl.class, NetworkDomainDaoImpl.class, NetworkOfferingDaoImpl.class,
    NetworkOfferingDetailsDaoImpl.class, NetworkOfferingServiceMapDaoImpl.class, NetworkOpDaoImpl.class, NetworkOrchestrator.class, NetworkRuleConfigDaoImpl.class, NetworkServiceImpl.class,
    NetworkServiceMapDaoImpl.class, NicDaoImpl.class, NicIpAliasDaoImpl.class, NicSecondaryIpDaoImpl.class, PhysicalNetworkDaoImpl.class, PhysicalNetworkServiceProviderDaoImpl.class,
    PhysicalNetworkTrafficTypeDaoImpl.class, PlannerHostReservationDaoImpl.class, PodVlanDaoImpl.class, PodVlanMapDaoImpl.class, PortForwardingRulesDaoImpl.class,
    PortableIpDaoImpl.class, PortableIpRangeDaoImpl.class, PrimaryDataStoreDaoImpl.class, PrivateIpDaoImpl.class, ProjectAccountDaoImpl.class,
    ProjectAccountJoinDaoImpl.class, ProjectInvitationDaoImpl.class, ProjectDaoImpl.class, ProjectInvitationJoinDaoImpl.class, ProjectJoinDaoImpl.class,
    ProjectManagerImpl.class, RegionDaoImpl.class, RemoteAccessVpnDaoImpl.class, ResourceCountDaoImpl.class, ResourceLimitDaoImpl.class, ResourceTagDao.class,
    ResourceTagJoinDaoImpl.class, ResourceTagsDaoImpl.class, RouterNetworkDaoImpl.class, RulesManagerImpl.class, SSHKeyPairDaoImpl.class,
    SecondaryStorageVmDaoImpl.class, SecurityGroupDaoImpl.class, SecurityGroupJoinDaoImpl.class, SecurityGroupRuleDaoImpl.class, SecurityGroupRulesDaoImpl.class,
    SecurityGroupVMMapDaoImpl.class, SecurityGroupWorkDaoImpl.class, ServerEventHandlerImpl.class, ServiceOfferingDaoImpl.class, ServiceOfferingDetailsDaoImpl.class,
    ServiceOfferingJoinDaoImpl.class, Site2SiteCustomerGatewayDaoImpl.class, Site2SiteVpnConnectionDaoImpl.class, Site2SiteVpnGatewayDaoImpl.class,
    SnapshotDaoImpl.class, SnapshotPolicyDaoImpl.class, StaticRouteDaoImpl.class, StatsCollector.class, StoragePoolDetailsDaoImpl.class, StoragePoolHostDaoImpl.class,
    StoragePoolJoinDaoImpl.class, SyncQueueItemDaoImpl.class, TemplateDataStoreDaoImpl.class, TemplateJoinDaoImpl.class, UploadDaoImpl.class, UsageEventDaoImpl.class,
    UserAccountJoinDaoImpl.class, UserDaoImpl.class, UserIpv6AddressDaoImpl.class, UserStatisticsDaoImpl.class, UserStatsLogDaoImpl.class,
    UserVmCloneSettingDaoImpl.class, UserVmDaoImpl.class, UserVmDetailsDaoImpl.class, UserVmJoinDaoImpl.class, UserVmManagerImpl.class, VMInstanceDaoImpl.class, VMSnapshotDaoImpl.class,
    VMTemplateDaoImpl.class, VMTemplateDetailsDaoImpl.class, VMTemplateZoneDaoImpl.class, VirtualMachineManagerImpl.class, VirtualRouterProviderDaoImpl.class,
    VlanDaoImpl.class, VmDiskStatisticsDaoImpl.class, VmRulesetLogDaoImpl.class, VolumeDaoImpl.class, VolumeJoinDaoImpl.class, VpcDaoImpl.class,
    VpcGatewayDaoImpl.class, VpcManagerImpl.class, VpcOfferingDaoImpl.class, VpcOfferingServiceMapDaoImpl.class, VpcServiceMapDaoImpl.class,
    VpcVirtualNetworkApplianceManagerImpl.class, VpnUserDaoImpl.class, XenServerGuru.class}, includeFilters = {@Filter(value = IntegrationTestConfiguration.ComponentFilter.class,
                                                                                                  type = FilterType.CUSTOM)}, useDefaultFilters = false)
@Configuration
public class IntegrationTestConfiguration {
    public static class ComponentFilter implements TypeFilter {
        @Override
        public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
            String clsname = mdr.getClassMetadata().getClassName();
            ComponentScan cs = IntegrationTestConfiguration.class.getAnnotation(ComponentScan.class);
            return includedInBasePackageClasses(clsname, cs);
        }
    }

    public static boolean includedInBasePackageClasses(String clazzName, ComponentScan cs) {
        Class<?> clazzToCheck;
        try {
            clazzToCheck = Class.forName(clazzName);
        } catch (ClassNotFoundException e) {
            System.out.println("Unable to find " + clazzName);
            return false;
        }
        Class<?>[] clazzes = cs.basePackageClasses();
        for (Class<?> clazz : clazzes) {
            if (clazzToCheck.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    @Inject
    AffinityGroupDao _affinityGroupDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    UserDao _userDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    DataCenterDao _zoneDao;
    @Inject
    IPAddressDao _ipAddressDao;

    @Bean
    public AffinityGroupService affinityGroupService() {
        AffinityGroupService mock = Mockito.mock(AffinityGroupService.class);
        try {
            final AffinityGroupVO gmock = new AffinityGroupVO("grp1", "grp-type", "affinity group", 1, Account.ACCOUNT_ID_SYSTEM, ControlledEntity.ACLType.Account);
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    _affinityGroupDao.persist(gmock);
                }
            });
            Mockito.when(
                mock.createAffinityGroup(Matchers.any(String.class), Matchers.any(Long.class), Matchers.any(Long.class), Matchers.any(String.class), Matchers.any(String.class),
                    Matchers.any(String.class))).thenReturn(gmock);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mock;
    }

    @Bean
    public AgentManager agentManager() {
        return Mockito.mock(AgentManager.class);
    }

    @Bean
    public AlertManager alertManager() {
        return Mockito.mock(AlertManager.class);
    }

    @Bean
    public APIChecker apiChecker() {
        return Mockito.mock(APIChecker.class);
    }

    @Bean
    public AsyncJobDispatcher asyncJobDispatcher() {
        return Mockito.mock(AsyncJobDispatcher.class);
    }

    @Bean
    public AsyncJobMonitor asyncJobMonitor() {
        return Mockito.mock(AsyncJobMonitor.class);
    }

    @Bean
    public AutoScaleService autoScaleService() {
        return Mockito.mock(AutoScaleService.class);
    }

    @Bean
    public CapacityManager capacityManager() {
        return Mockito.mock(CapacityManager.class);
    }

    @Bean
    public ClusterManager clusterManager() {
        return Mockito.mock(ClusterManager.class);
    }

    @Bean
    public ConfigDepot configDepot() {
        return Mockito.mock(ConfigDepot.class);
    }

    @Bean
    public ConfigDepotAdmin configDepotAdmin() {
        return Mockito.mock(ConfigDepotAdmin.class);
    }

    @Bean
    public ConsoleProxyManager consoleProxyManager() {
        return Mockito.mock(ConsoleProxyManager.class);
    }

    @Bean
    public ConsoleProxyService consoleProxyService() {
        return Mockito.mock(ConsoleProxyService.class);
    }

    @Bean
    public DataStoreManager dataStoreManager() {
        return Mockito.mock(DataStoreManager.class);
    }

    @Bean
    public DataStoreProviderApiService dataStoreProviderApiService() {
        return Mockito.mock(DataStoreProviderApiService.class);
    }

    @Bean
    public DeploymentPlanner deploymentPlanner() {
        return Mockito.mock(DeploymentPlanner.class);
    }

    @Bean
    public DeploymentPlanningManager deploymentPlanningManager() {
        return Mockito.mock(DeploymentPlanningManager.class);
    }

    @Bean
    public DomainChecker domainChecker() {
        DomainChecker mock = Mockito.mock(DomainChecker.class);
        try {
            Mockito.when(mock.checkAccess(Matchers.any(Account.class), Matchers.any(DataCenter.class))).thenReturn(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mock;
    }

    @Bean
    public EndPointSelector endPointSelector() {
        return Mockito.mock(EndPointSelector.class);
    }

    @Bean
    public EntityManager entityManager() {
        EntityManager mock = Mockito.mock(EntityManager.class);
        try {
            Mockito.when(mock.findById(Matchers.same(Account.class), Matchers.anyLong())).thenReturn(_accountDao.findById(Account.ACCOUNT_ID_SYSTEM));
            Mockito.when(mock.findById(Matchers.same(User.class), Matchers.anyLong())).thenReturn(_userDao.findById(User.UID_SYSTEM));
            Mockito.when(mock.findById(Matchers.same(NetworkOffering.class), Matchers.any(Long.class))).thenAnswer(new Answer<NetworkOffering>() {
                @Override
                public NetworkOffering answer(final InvocationOnMock invocation) throws Throwable {
                    Long id = (Long)invocation.getArguments()[1];
                    return _networkOfferingDao.findById(id);
                }
            });
            Mockito.when(mock.findById(Matchers.same(IpAddress.class), Matchers.any(Long.class))).thenAnswer(new Answer<IpAddress>() {
                @Override
                public IpAddress answer(final InvocationOnMock invocation) throws Throwable {
                    Long id = (Long)invocation.getArguments()[1];
                    return _ipAddressDao.findById(id);
                }
            });
            Mockito.when(mock.findById(Matchers.same(DataCenter.class), Matchers.any(Long.class))).thenAnswer(new Answer<DataCenter>() {
                @Override
                public DataCenter answer(final InvocationOnMock invocation) throws Throwable {
                    Long id = (Long)invocation.getArguments()[1];
                    return _zoneDao.findById(id);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        CallContext.init(mock);
        return mock;
    }

    @Bean
    public ExternalDeviceUsageManager externalDeviceUsageManager() {
        return Mockito.mock(ExternalDeviceUsageManager.class);
    }

    @Bean
    public GlobalLoadBalancingRulesService globalLoadBalancingRulesService() {
        return Mockito.mock(GlobalLoadBalancingRulesService.class);
    }

    @Bean
    public HighAvailabilityManager highAvailabilityManager() {
        return Mockito.mock(HighAvailabilityManager.class);
    }

    @Bean
    public HostAllocator hostAllocator() {
        return Mockito.mock(HostAllocator.class);
    }

    @Bean
    public IdentityService identityService() {
        return Mockito.mock(IdentityService.class);
    }

    @Bean
    public InternalLoadBalancerVMManager internalLoadBalancerVMManager() {
        return Mockito.mock(InternalLoadBalancerVMManager.class);
    }

    @Bean
    public InternalLoadBalancerVMService internalLoadBalancerVMService() {
        return Mockito.mock(InternalLoadBalancerVMService.class);
    }

    @Bean
    public ApplicationLoadBalancerService applicationLoadBalancerService() {
        return Mockito.mock(ApplicationLoadBalancerService.class);
    }

    @Bean
    public ManagementServer managementServer() {
        return Mockito.mock(ManagementServer.class);
    }

    @Bean
    public NetworkACLService networkACLService() {
        return Mockito.mock(NetworkACLService.class);
    }

    @Bean
    public NetworkUsageService networkUsageService() {
        return Mockito.mock(NetworkUsageService.class);
    }

    @Bean
    public OrchestrationService orchSrvc() {
        return Mockito.mock(OrchestrationService.class);
    }

    @Bean
    public PodAllocator podAllocator() {
        return Mockito.mock(PodAllocator.class);
    }

    @Bean
    public QueryService queryService() {
        return Mockito.mock(QueryService.class);
    }

    @Bean
    public RegionManager regionManager() {
        return Mockito.mock(RegionManager.class);
    }

    @Bean
    public RemoteAccessVpnService remoteAccessVpnService() {
        return Mockito.mock(RemoteAccessVpnService.class);
    }

    @Bean
    public ResourceLimitService resourceLimitService() {
        return Mockito.mock(ResourceLimitService.class);
    }

    @Bean
    public ResourceManager resourceManager() {
        return Mockito.mock(ResourceManager.class);
    }

    @Bean
    public ResourceMetaDataService resourceMetaDataService() {
        return Mockito.mock(ResourceMetaDataService.class);
    }

    @Bean
    public Site2SiteVpnManager site2SiteVpnManager() {
        return Mockito.mock(Site2SiteVpnManager.class);
    }

    @Bean
    public Site2SiteVpnServiceProvider site2SiteVpnServiceProvider() {
        return Mockito.mock(Site2SiteVpnServiceProvider.class);
    }

    @Bean
    public SnapshotApiService snapshotApiService() {
        return Mockito.mock(SnapshotApiService.class);
    }

    @Bean
    public SnapshotManager snapshotManager() {
        return Mockito.mock(SnapshotManager.class);
    }

    @Bean
    public StorageManager storageManager() {
        return Mockito.mock(StorageManager.class);
    }

    @Bean
    public StorageNetworkManager storageNetworkManager() {
        return Mockito.mock(StorageNetworkManager.class);
    }

    @Bean
    public StorageNetworkService storageNetworkService() {
        return Mockito.mock(StorageNetworkService.class);
    }

    @Bean
    public StoragePoolAllocator storagePoolAllocator() {
        return Mockito.mock(StoragePoolAllocator.class);
    }

    @Bean
    public SyncQueueManager syncQueueManager() {
        return Mockito.mock(SyncQueueManager.class);
    }

    @Bean
    public TaggedResourceService taggedResourceService() {
        return Mockito.mock(TaggedResourceService.class);
    }

    @Bean
    public TemplateDataFactory templateDataFactory() {
        return Mockito.mock(TemplateDataFactory.class);
    }

    @Bean
    public TemplateApiService templateApiService() {
        return Mockito.mock(TemplateApiService.class);
    }

    @Bean
    public TemplateManager templateManager() {
        return Mockito.mock(TemplateManager.class);
    }

    @Bean
    public TemplateService templateService() {
        return Mockito.mock(TemplateService.class);
    }

    @Bean
    public UsageService usageService() {
        return Mockito.mock(UsageService.class);
    }

    @Bean
    public UserAuthenticator userAuthenticator() {
        return Mockito.mock(UserAuthenticator.class);
    }

    @Bean
    public VMSnapshotManager vMSnapshotManager() {
        return Mockito.mock(VMSnapshotManager.class);
    }

    @Bean
    public VolumeApiService volumeApiService() {
        return Mockito.mock(VolumeApiService.class);
    }

    @Bean
    public VolumeDataFactory volumeDataFactory() {
        return Mockito.mock(VolumeDataFactory.class);
    }

    @Bean
    public VolumeOrchestrationService volumeOrchestrationService() {
        return Mockito.mock(VolumeOrchestrationService.class);
    }
    @Bean
    public FirewallServiceProvider firewallServiceProvider() {
        return Mockito.mock(FirewallServiceProvider.class);
    }
    @Bean
    public PortForwardingServiceProvider portForwardingServiceProvider() {
        return Mockito.mock(PortForwardingServiceProvider.class);
    }
    @Bean
    public NetworkACLServiceProvider networkACLServiceProvider() {
        return Mockito.mock(NetworkACLServiceProvider.class);
    }
    @Bean
    public VpcProvider vpcProvier() {
        return Mockito.mock(VpcProvider.class);
    }
    @Bean
    public VolumeService volumeService() {
        return Mockito.mock(VolumeService.class);
    }
    @Bean
    public PrimaryDataStoreProviderManager privateDataStoreProviderManager() {
        return Mockito.mock(PrimaryDataStoreProviderManager.class);
    }
    @Bean
    public ImageStoreProviderManager imageStoreProviderManager() {
        return Mockito.mock(ImageStoreProviderManager.class);
    }
    @Bean
    public DataStoreProvider dataStoreProvider() {
        return Mockito.mock(DataStoreProvider.class);
    }
}
