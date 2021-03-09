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
package com.cloud.api;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RoleService;
import org.apache.cloudstack.affinity.AffinityGroup;
import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants.DomainDetails;
import org.apache.cloudstack.api.ApiConstants.HostDetails;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.api.response.BackupOfferingResponse;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.BackupScheduleResponse;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.EventResponse;
import org.apache.cloudstack.api.response.HostForMigrationResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.HostTagResponse;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.apache.cloudstack.api.response.NetworkOfferingResponse;
import org.apache.cloudstack.api.response.ProjectAccountResponse;
import org.apache.cloudstack.api.response.ProjectInvitationResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ResourceTagResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.StorageTagResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.api.response.VpcOfferingResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.BackupOffering;
import org.apache.cloudstack.backup.BackupSchedule;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.backup.dao.BackupScheduleDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobDao;
import org.apache.cloudstack.resourcedetail.dao.DiskOfferingDetailsDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.agent.api.VgpuTypesInfo;
import com.cloud.api.query.dao.AccountJoinDao;
import com.cloud.api.query.dao.AffinityGroupJoinDao;
import com.cloud.api.query.dao.AsyncJobJoinDao;
import com.cloud.api.query.dao.DataCenterJoinDao;
import com.cloud.api.query.dao.DiskOfferingJoinDao;
import com.cloud.api.query.dao.DomainJoinDao;
import com.cloud.api.query.dao.DomainRouterJoinDao;
import com.cloud.api.query.dao.HostJoinDao;
import com.cloud.api.query.dao.HostTagDao;
import com.cloud.api.query.dao.ImageStoreJoinDao;
import com.cloud.api.query.dao.InstanceGroupJoinDao;
import com.cloud.api.query.dao.NetworkOfferingJoinDao;
import com.cloud.api.query.dao.ProjectAccountJoinDao;
import com.cloud.api.query.dao.ProjectInvitationJoinDao;
import com.cloud.api.query.dao.ProjectJoinDao;
import com.cloud.api.query.dao.ResourceTagJoinDao;
import com.cloud.api.query.dao.SecurityGroupJoinDao;
import com.cloud.api.query.dao.ServiceOfferingJoinDao;
import com.cloud.api.query.dao.StoragePoolJoinDao;
import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.api.query.dao.UserAccountJoinDao;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.dao.VolumeJoinDao;
import com.cloud.api.query.dao.VpcOfferingJoinDao;
import com.cloud.api.query.vo.AccountJoinVO;
import com.cloud.api.query.vo.AffinityGroupJoinVO;
import com.cloud.api.query.vo.AsyncJobJoinVO;
import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.api.query.vo.DiskOfferingJoinVO;
import com.cloud.api.query.vo.DomainJoinVO;
import com.cloud.api.query.vo.DomainRouterJoinVO;
import com.cloud.api.query.vo.EventJoinVO;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.api.query.vo.HostTagVO;
import com.cloud.api.query.vo.ImageStoreJoinVO;
import com.cloud.api.query.vo.InstanceGroupJoinVO;
import com.cloud.api.query.vo.NetworkOfferingJoinVO;
import com.cloud.api.query.vo.ProjectAccountJoinVO;
import com.cloud.api.query.vo.ProjectInvitationJoinVO;
import com.cloud.api.query.vo.ProjectJoinVO;
import com.cloud.api.query.vo.ResourceTagJoinVO;
import com.cloud.api.query.vo.SecurityGroupJoinVO;
import com.cloud.api.query.vo.ServiceOfferingJoinVO;
import com.cloud.api.query.vo.StoragePoolJoinVO;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.api.query.vo.UserAccountJoinVO;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.api.query.vo.VolumeJoinVO;
import com.cloud.api.query.vo.VpcOfferingJoinVO;
import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.capacity.dao.CapacityDaoImpl.SummedCapacity;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ConfigurationService;
import com.cloud.configuration.Resource;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.Event;
import com.cloud.event.dao.EventJoinDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.gpu.HostGpuGroupsVO;
import com.cloud.gpu.VGPUTypesVO;
import com.cloud.gpu.dao.HostGpuGroupsDao;
import com.cloud.gpu.dao.VGPUTypesDao;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.as.AutoScalePolicy;
import com.cloud.network.as.AutoScalePolicyConditionMapVO;
import com.cloud.network.as.AutoScalePolicyVO;
import com.cloud.network.as.AutoScaleVmGroupPolicyMapVO;
import com.cloud.network.as.AutoScaleVmGroupVO;
import com.cloud.network.as.AutoScaleVmProfileVO;
import com.cloud.network.as.ConditionVO;
import com.cloud.network.as.CounterVO;
import com.cloud.network.as.dao.AutoScalePolicyConditionMapDao;
import com.cloud.network.as.dao.AutoScalePolicyDao;
import com.cloud.network.as.dao.AutoScaleVmGroupDao;
import com.cloud.network.as.dao.AutoScaleVmGroupPolicyMapDao;
import com.cloud.network.as.dao.AutoScaleVmProfileDao;
import com.cloud.network.as.dao.ConditionDao;
import com.cloud.network.as.dao.CounterDao;
import com.cloud.network.dao.AccountGuestVlanMapDao;
import com.cloud.network.dao.AccountGuestVlanMapVO;
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.FirewallRulesDcidrsDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDomainDao;
import com.cloud.network.dao.NetworkDomainVO;
import com.cloud.network.dao.NetworkRuleConfigDao;
import com.cloud.network.dao.NetworkRuleConfigVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.dao.Site2SiteCustomerGatewayDao;
import com.cloud.network.dao.Site2SiteCustomerGatewayVO;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.Site2SiteVpnGatewayVO;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.network.vpc.NetworkACL;
import com.cloud.network.vpc.StaticRouteVO;
import com.cloud.network.vpc.VpcGatewayVO;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.network.vpc.VpcProvisioningService;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.network.vpc.dao.StaticRouteDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectAccount;
import com.cloud.projects.ProjectInvitation;
import com.cloud.projects.ProjectService;
import com.cloud.region.ha.GlobalLoadBalancingRulesService;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ManagementServer;
import com.cloud.server.ResourceMetaDataService;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.server.StatsCollector;
import com.cloud.server.TaggedResourceService;
import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.ImageStore;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolTagVO;
import com.cloud.storage.StorageStats;
import com.cloud.storage.UploadVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.VolumeStats;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.StoragePoolTagsDao;
import com.cloud.storage.dao.UploadDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotPolicy;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.InstanceGroup;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.VmStats;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.NicSecondaryIpVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

public class ApiDBUtils {
    private static ManagementServer s_ms;
    static AsyncJobManager s_asyncMgr;
    static SecurityGroupManager s_securityGroupMgr;
    static StorageManager s_storageMgr;
    static VolumeOrchestrationService s_volumeMgr;
    static UserVmManager s_userVmMgr;
    static NetworkModel s_networkModel;
    static NetworkOrchestrationService s_networkMgr;
    static TemplateManager s_templateMgr;
    static ConfigurationManager s_configMgr;

    static StatsCollector s_statsCollector;

    static AccountDao s_accountDao;
    static AccountVlanMapDao s_accountVlanMapDao;
    static ClusterDao s_clusterDao;
    static CapacityDao s_capacityDao;
    static DiskOfferingDao s_diskOfferingDao;
    static DiskOfferingJoinDao s_diskOfferingJoinDao;
    static DiskOfferingDetailsDao s_diskOfferingDetailsDao;
    static DataCenterJoinDao s_dcJoinDao;
    static DomainDao s_domainDao;
    static DomainJoinDao s_domainJoinDao;
    static DomainRouterDao s_domainRouterDao;
    static DomainRouterJoinDao s_domainRouterJoinDao;
    static GuestOSDao s_guestOSDao;
    static GuestOSCategoryDao s_guestOSCategoryDao;
    static HostDao s_hostDao;
    static AccountGuestVlanMapDao s_accountGuestVlanMapDao;
    static IPAddressDao s_ipAddressDao;
    static LoadBalancerDao s_loadBalancerDao;
    static SecurityGroupDao s_securityGroupDao;
    static SecurityGroupJoinDao s_securityGroupJoinDao;
    static ServiceOfferingJoinDao s_serviceOfferingJoinDao;
    static NetworkRuleConfigDao s_networkRuleConfigDao;
    static HostPodDao s_podDao;
    static ServiceOfferingDao s_serviceOfferingDao;
    static ServiceOfferingDetailsDao s_serviceOfferingDetailsDao;
    static SnapshotDao s_snapshotDao;
    static PrimaryDataStoreDao s_storagePoolDao;
    static VMTemplateDao s_templateDao;
    static VMTemplateDetailsDao s_templateDetailsDao;
    static UploadDao s_uploadDao;
    static UserDao s_userDao;
    static UserStatisticsDao s_userStatsDao;
    static UserVmDao s_userVmDao;
    static UserVmJoinDao s_userVmJoinDao;
    static VlanDao s_vlanDao;
    static VolumeDao s_volumeDao;
    static Site2SiteVpnGatewayDao s_site2SiteVpnGatewayDao;
    static Site2SiteCustomerGatewayDao s_site2SiteCustomerGatewayDao;
    static DataCenterDao s_zoneDao;
    static NetworkOfferingDao s_networkOfferingDao;
    static NetworkOfferingJoinDao s_networkOfferingJoinDao;
    static NetworkDao s_networkDao;
    static PhysicalNetworkDao s_physicalNetworkDao;
    static ConfigurationService s_configSvc;
    static ConfigurationDao s_configDao;
    static ConsoleProxyDao s_consoleProxyDao;
    static FirewallRulesCidrsDao s_firewallCidrsDao;
    static FirewallRulesDcidrsDao s_firewallDcidrsDao;
    static VMInstanceDao s_vmDao;
    static ResourceLimitService s_resourceLimitMgr;
    static ProjectService s_projectMgr;
    static ResourceManager s_resourceMgr;
    static AccountDetailsDao s_accountDetailsDao;
    static NetworkDomainDao s_networkDomainDao;
    static HighAvailabilityManager s_haMgr;
    static VpcManager s_vpcMgr;
    static TaggedResourceService s_taggedResourceService;
    static UserVmDetailsDao s_userVmDetailsDao;
    static SSHKeyPairDao s_sshKeyPairDao;

    static ConditionDao s_asConditionDao;
    static AutoScalePolicyConditionMapDao s_asPolicyConditionMapDao;
    static AutoScaleVmGroupPolicyMapDao s_asVmGroupPolicyMapDao;
    static AutoScalePolicyDao s_asPolicyDao;
    static AutoScaleVmProfileDao s_asVmProfileDao;
    static AutoScaleVmGroupDao s_asVmGroupDao;
    static CounterDao s_counterDao;
    static ResourceTagJoinDao s_tagJoinDao;
    static EventJoinDao s_eventJoinDao;
    static InstanceGroupJoinDao s_vmGroupJoinDao;
    static UserAccountJoinDao s_userAccountJoinDao;
    static ProjectJoinDao s_projectJoinDao;
    static ProjectAccountJoinDao s_projectAccountJoinDao;
    static ProjectInvitationJoinDao s_projectInvitationJoinDao;
    static HostJoinDao s_hostJoinDao;
    static VolumeJoinDao s_volJoinDao;
    static StoragePoolJoinDao s_poolJoinDao;
    static StoragePoolTagsDao s_tagDao;
    static HostTagDao s_hostTagDao;
    static ImageStoreJoinDao s_imageStoreJoinDao;
    static AccountJoinDao s_accountJoinDao;
    static AsyncJobJoinDao s_jobJoinDao;
    static TemplateJoinDao s_templateJoinDao;

    static PhysicalNetworkTrafficTypeDao s_physicalNetworkTrafficTypeDao;
    static PhysicalNetworkServiceProviderDao s_physicalNetworkServiceProviderDao;
    static FirewallRulesDao s_firewallRuleDao;
    static StaticRouteDao s_staticRouteDao;
    static VpcGatewayDao s_vpcGatewayDao;
    static VpcDao s_vpcDao;
    static VpcOfferingDao s_vpcOfferingDao;
    static VpcOfferingJoinDao s_vpcOfferingJoinDao;
    static SnapshotPolicyDao s_snapshotPolicyDao;
    static AsyncJobDao s_asyncJobDao;
    static HostDetailsDao s_hostDetailsDao;
    static VMSnapshotDao s_vmSnapshotDao;
    static ClusterDetailsDao s_clusterDetailsDao;
    static NicSecondaryIpDao s_nicSecondaryIpDao;
    static VpcProvisioningService s_vpcProvSvc;
    static AffinityGroupDao s_affinityGroupDao;
    static AffinityGroupJoinDao s_affinityGroupJoinDao;
    static GlobalLoadBalancingRulesService s_gslbService;
    static NetworkACLDao s_networkACLDao;
    static RoleService s_roleService;
    static AccountService s_accountService;
    static ResourceMetaDataService s_resourceDetailsService;
    static HostGpuGroupsDao s_hostGpuGroupsDao;
    static VGPUTypesDao s_vgpuTypesDao;
    static BackupDao s_backupDao;
    static BackupScheduleDao s_backupScheduleDao;
    static BackupOfferingDao s_backupOfferingDao;
    static NicDao s_nicDao;

    @Inject
    private ManagementServer ms;
    @Inject
    public AsyncJobManager asyncMgr;
    @Inject
    private SecurityGroupManager securityGroupMgr;
    @Inject
    private StorageManager storageMgr;
    @Inject
    private UserVmManager userVmMgr;
    @Inject
    private NetworkModel networkModel;
    @Inject
    private NetworkOrchestrationService networkMgr;
    @Inject
    private StatsCollector statsCollector;
    @Inject
    private TemplateManager templateMgr;
    @Inject
    private VolumeOrchestrationService volumeMgr;

    @Inject
    private AccountDao accountDao;
    @Inject
    private AccountVlanMapDao accountVlanMapDao;
    @Inject
    private ClusterDao clusterDao;
    @Inject
    private CapacityDao capacityDao;
    @Inject
    private DataCenterJoinDao dcJoinDao;
    @Inject
    private DiskOfferingDao diskOfferingDao;
    @Inject
    private DiskOfferingJoinDao diskOfferingJoinDao;
    @Inject
    private DiskOfferingDetailsDao diskOfferingDetailsDao;
    @Inject
    private DomainDao domainDao;
    @Inject
    private DomainJoinDao domainJoinDao;
    @Inject
    private DomainRouterDao domainRouterDao;
    @Inject
    private DomainRouterJoinDao domainRouterJoinDao;
    @Inject
    private GuestOSDao guestOSDao;
    @Inject
    private GuestOSCategoryDao guestOSCategoryDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private AccountGuestVlanMapDao accountGuestVlanMapDao;
    @Inject
    private IPAddressDao ipAddressDao;
    @Inject
    private LoadBalancerDao loadBalancerDao;
    @Inject
    private SecurityGroupDao securityGroupDao;
    @Inject
    private SecurityGroupJoinDao securityGroupJoinDao;
    @Inject
    private ServiceOfferingJoinDao serviceOfferingJoinDao;
    @Inject
    private NetworkRuleConfigDao networkRuleConfigDao;
    @Inject
    private HostPodDao podDao;
    @Inject
    private ServiceOfferingDao serviceOfferingDao;
    @Inject
    private ServiceOfferingDetailsDao serviceOfferingDetailsDao;
    @Inject
    private SnapshotDao snapshotDao;
    @Inject
    private PrimaryDataStoreDao storagePoolDao;
    @Inject
    private VMTemplateDao templateDao;
    @Inject
    private VMTemplateDetailsDao templateDetailsDao;
    @Inject
    private UploadDao uploadDao;
    @Inject
    private UserDao userDao;
    @Inject
    private UserStatisticsDao userStatsDao;
    @Inject
    private UserVmDao userVmDao;
    @Inject
    private UserVmJoinDao userVmJoinDao;
    @Inject
    private VlanDao vlanDao;
    @Inject
    private VolumeDao volumeDao;
    @Inject
    private Site2SiteVpnGatewayDao site2SiteVpnGatewayDao;
    @Inject
    private Site2SiteCustomerGatewayDao site2SiteCustomerGatewayDao;
    @Inject
    private DataCenterDao zoneDao;
    @Inject
    private NetworkOfferingDao networkOfferingDao;
    @Inject
    private NetworkOfferingJoinDao networkOfferingJoinDao;
    @Inject
    private NetworkDao networkDao;
    @Inject
    private PhysicalNetworkDao physicalNetworkDao;
    @Inject
    private ConfigurationService configSvc;
    @Inject
    private ConfigurationDao configDao;
    @Inject
    private ConsoleProxyDao consoleProxyDao;
    @Inject
    private FirewallRulesCidrsDao firewallCidrsDao;
    @Inject
    private FirewallRulesDcidrsDao firewalDcidrsDao;
    @Inject
    private VMInstanceDao vmDao;
    @Inject
    private ResourceLimitService resourceLimitMgr;
    @Inject
    private ProjectService projectMgr;
    @Inject
    private ResourceManager resourceMgr;
    @Inject
    private AccountDetailsDao accountDetailsDao;
    @Inject
    private NetworkDomainDao networkDomainDao;
    @Inject
    private HighAvailabilityManager haMgr;
    @Inject
    private VpcManager vpcMgr;
    @Inject
    private TaggedResourceService taggedResourceService;
    @Inject
    private UserVmDetailsDao userVmDetailsDao;
    @Inject
    private SSHKeyPairDao sshKeyPairDao;

    @Inject
    private ConditionDao asConditionDao;
    @Inject
    private AutoScalePolicyConditionMapDao asPolicyConditionMapDao;
    @Inject
    private AutoScaleVmGroupPolicyMapDao asVmGroupPolicyMapDao;
    @Inject
    private AutoScalePolicyDao asPolicyDao;
    @Inject
    private AutoScaleVmProfileDao asVmProfileDao;
    @Inject
    private AutoScaleVmGroupDao asVmGroupDao;
    @Inject
    private CounterDao counterDao;
    @Inject
    private ResourceTagJoinDao tagJoinDao;
    @Inject
    private EventJoinDao eventJoinDao;
    @Inject
    private InstanceGroupJoinDao vmGroupJoinDao;
    @Inject
    private UserAccountJoinDao userAccountJoinDao;
    @Inject
    private ProjectJoinDao projectJoinDao;
    @Inject
    private ProjectAccountJoinDao projectAccountJoinDao;
    @Inject
    private ProjectInvitationJoinDao projectInvitationJoinDao;
    @Inject
    private HostJoinDao hostJoinDao;
    @Inject
    private VolumeJoinDao volJoinDao;
    @Inject
    private StoragePoolJoinDao poolJoinDao;
    @Inject
    private StoragePoolTagsDao tagDao;
    @Inject
    private HostTagDao hosttagDao;
    @Inject
    private ImageStoreJoinDao imageStoreJoinDao;
    @Inject
    private AccountJoinDao accountJoinDao;
    @Inject
    private AsyncJobJoinDao jobJoinDao;
    @Inject
    private TemplateJoinDao templateJoinDao;

    @Inject
    private PhysicalNetworkTrafficTypeDao physicalNetworkTrafficTypeDao;
    @Inject
    private PhysicalNetworkServiceProviderDao physicalNetworkServiceProviderDao;
    @Inject
    private FirewallRulesDao firewallRuleDao;
    @Inject
    private StaticRouteDao staticRouteDao;
    @Inject
    private VpcGatewayDao vpcGatewayDao;
    @Inject
    private VpcDao vpcDao;
    @Inject
    private VpcOfferingDao vpcOfferingDao;
    @Inject
    private VpcOfferingJoinDao vpcOfferingJoinDao;
    @Inject
    private SnapshotPolicyDao snapshotPolicyDao;
    @Inject
    private AsyncJobDao asyncJobDao;
    @Inject
    private HostDetailsDao hostDetailsDao;
    @Inject
    private ClusterDetailsDao clusterDetailsDao;
    @Inject
    private VMSnapshotDao vmSnapshotDao;
    @Inject
    private NicSecondaryIpDao nicSecondaryIpDao;
    @Inject
    private VpcProvisioningService vpcProvSvc;
    @Inject
    private AffinityGroupDao affinityGroupDao;
    @Inject
    private AffinityGroupJoinDao affinityGroupJoinDao;
    @Inject
    private GlobalLoadBalancingRulesService gslbService;
    @Inject
    private NetworkACLDao networkACLDao;
    @Inject
    private RoleService roleService;
    @Inject
    private AccountService accountService;
    @Inject
    private ConfigurationManager configMgr;
    @Inject
    private ResourceMetaDataService resourceDetailsService;
    @Inject
    private HostGpuGroupsDao hostGpuGroupsDao;
    @Inject
    private VGPUTypesDao vgpuTypesDao;
    @Inject
    private BackupDao backupDao;
    @Inject
    private BackupOfferingDao backupOfferingDao;
    @Inject
    private BackupScheduleDao backupScheduleDao;
    @Inject
    private NicDao nicDao;

    @PostConstruct
    void init() {
        s_ms = ms;
        s_configMgr = configMgr;
        s_asyncMgr = asyncMgr;
        s_securityGroupMgr = securityGroupMgr;
        s_storageMgr = storageMgr;
        s_userVmMgr = userVmMgr;
        s_networkModel = networkModel;
        s_networkMgr = networkMgr;
        s_configSvc = configSvc;
        s_templateMgr = templateMgr;

        s_accountDao = accountDao;
        s_accountGuestVlanMapDao = accountGuestVlanMapDao;
        s_accountVlanMapDao = accountVlanMapDao;
        s_clusterDao = clusterDao;
        s_capacityDao = capacityDao;
        s_dcJoinDao = dcJoinDao;
        s_diskOfferingDao = diskOfferingDao;
        s_diskOfferingJoinDao = diskOfferingJoinDao;
        s_diskOfferingDetailsDao = diskOfferingDetailsDao;
        s_domainDao = domainDao;
        s_domainJoinDao = domainJoinDao;
        s_domainRouterDao = domainRouterDao;
        s_domainRouterJoinDao = domainRouterJoinDao;
        s_guestOSDao = guestOSDao;
        s_guestOSCategoryDao = guestOSCategoryDao;
        s_hostDao = hostDao;
        s_ipAddressDao = ipAddressDao;
        s_loadBalancerDao = loadBalancerDao;
        s_networkRuleConfigDao = networkRuleConfigDao;
        s_podDao = podDao;
        s_serviceOfferingDao = serviceOfferingDao;
        s_serviceOfferingDetailsDao = serviceOfferingDetailsDao;
        s_serviceOfferingJoinDao = serviceOfferingJoinDao;
        s_snapshotDao = snapshotDao;
        s_storagePoolDao = storagePoolDao;
        s_templateDao = templateDao;
        s_templateDetailsDao = templateDetailsDao;
        s_uploadDao = uploadDao;
        s_userDao = userDao;
        s_userStatsDao = userStatsDao;
        s_userVmDao = userVmDao;
        s_userVmJoinDao = userVmJoinDao;
        s_vlanDao = vlanDao;
        s_volumeDao = volumeDao;
        s_site2SiteVpnGatewayDao = site2SiteVpnGatewayDao;
        s_site2SiteCustomerGatewayDao = site2SiteCustomerGatewayDao;
        s_zoneDao = zoneDao;
        s_securityGroupDao = securityGroupDao;
        s_securityGroupJoinDao = securityGroupJoinDao;
        s_networkOfferingDao = networkOfferingDao;
        s_networkOfferingJoinDao = networkOfferingJoinDao;
        s_networkDao = networkDao;
        s_physicalNetworkDao = physicalNetworkDao;
        s_configDao = configDao;
        s_consoleProxyDao = consoleProxyDao;
        s_firewallCidrsDao = firewallCidrsDao;
        s_firewallDcidrsDao  = firewalDcidrsDao;
        s_vmDao = vmDao;
        s_resourceLimitMgr = resourceLimitMgr;
        s_projectMgr = projectMgr;
        s_resourceMgr = resourceMgr;
        s_accountDetailsDao = accountDetailsDao;
        s_networkDomainDao = networkDomainDao;
        s_haMgr = haMgr;
        s_vpcMgr = vpcMgr;
        s_taggedResourceService = taggedResourceService;
        s_sshKeyPairDao = sshKeyPairDao;
        s_userVmDetailsDao = userVmDetailsDao;
        s_asConditionDao = asConditionDao;
        s_asPolicyDao = asPolicyDao;
        s_asPolicyConditionMapDao = asPolicyConditionMapDao;
        s_counterDao = counterDao;
        s_asVmGroupPolicyMapDao = asVmGroupPolicyMapDao;
        s_tagJoinDao = tagJoinDao;
        s_vmGroupJoinDao = vmGroupJoinDao;
        s_eventJoinDao = eventJoinDao;
        s_userAccountJoinDao = userAccountJoinDao;
        s_projectJoinDao = projectJoinDao;
        s_projectAccountJoinDao = projectAccountJoinDao;
        s_projectInvitationJoinDao = projectInvitationJoinDao;
        s_hostJoinDao = hostJoinDao;
        s_volJoinDao = volJoinDao;
        s_poolJoinDao = poolJoinDao;
        s_tagDao = tagDao;
        s_hostTagDao = hosttagDao;
        s_imageStoreJoinDao = imageStoreJoinDao;
        s_accountJoinDao = accountJoinDao;
        s_jobJoinDao = jobJoinDao;
        s_templateJoinDao = templateJoinDao;

        s_physicalNetworkTrafficTypeDao = physicalNetworkTrafficTypeDao;
        s_physicalNetworkServiceProviderDao = physicalNetworkServiceProviderDao;
        s_firewallRuleDao = firewallRuleDao;
        s_staticRouteDao = staticRouteDao;
        s_vpcGatewayDao = vpcGatewayDao;
        s_asVmProfileDao = asVmProfileDao;
        s_asVmGroupDao = asVmGroupDao;
        s_vpcDao = vpcDao;
        s_vpcOfferingDao = vpcOfferingDao;
        s_vpcOfferingJoinDao = vpcOfferingJoinDao;
        s_snapshotPolicyDao = snapshotPolicyDao;
        s_asyncJobDao = asyncJobDao;
        s_hostDetailsDao = hostDetailsDao;
        s_clusterDetailsDao = clusterDetailsDao;
        s_vmSnapshotDao = vmSnapshotDao;
        s_nicDao = nicDao;
        s_nicSecondaryIpDao = nicSecondaryIpDao;
        s_vpcProvSvc = vpcProvSvc;
        s_affinityGroupDao = affinityGroupDao;
        s_affinityGroupJoinDao = affinityGroupJoinDao;
        s_gslbService = gslbService;
        // Note: stats collector should already have been initialized by this time, otherwise a null instance is returned
        s_statsCollector = StatsCollector.getInstance();
        s_networkACLDao = networkACLDao;
        s_roleService = roleService;
        s_accountService = accountService;
        s_resourceDetailsService = resourceDetailsService;
        s_hostGpuGroupsDao = hostGpuGroupsDao;
        s_vgpuTypesDao = vgpuTypesDao;
        s_backupDao = backupDao;
        s_backupScheduleDao = backupScheduleDao;
        s_backupOfferingDao = backupOfferingDao;
    }

    // ///////////////////////////////////////////////////////////
    // ManagementServer methods //
    // ///////////////////////////////////////////////////////////

    public static VMInstanceVO findVMInstanceById(long vmId) {
        return s_vmDao.findByIdIncludingRemoved(vmId);
    }

    public static long getStorageCapacitybyPool(Long poolId, short capacityType) {
        // TODO: This method is for the API only, but it has configuration values (ramSize for system vms)
        // so if this Utils class can have some kind of config rather than a static initializer (maybe from
        // management server instantiation?) then maybe the management server method can be moved entirely
        // into this utils class.
        return s_ms.getMemoryOrCpuCapacityByHost(poolId, capacityType);
    }

    public static List<SummedCapacity> getCapacityByClusterPodZone(Long zoneId, Long podId, Long clusterId) {
        return s_capacityDao.findByClusterPodZone(zoneId, podId, clusterId);
    }

    public static List<SummedCapacity> findNonSharedStorageForClusterPodZone(Long zoneId, Long podId, Long clusterId) {
        return s_capacityDao.findNonSharedStorageForClusterPodZone(zoneId, podId, clusterId);
    }

    public static List<CapacityVO> getCapacityByPod() {
        return null;

    }

    public static Long getPodIdForVlan(long vlanDbId) {
        return s_networkModel.getPodIdForVlan(vlanDbId);
    }

    public static String getVersion() {
        return s_ms.getVersion();
    }


    // ///////////////////////////////////////////////////////////
    // Manager methods //
    // ///////////////////////////////////////////////////////////

    public static long findCorrectResourceLimitForDomain(ResourceType type, long domainId) {
        DomainVO domain = s_domainDao.findById(domainId);

        if (domain == null) {
            return -1;
        }

        return s_resourceLimitMgr.findCorrectResourceLimitForDomain(domain, type);
    }

    public static long findCorrectResourceLimitForDomain(Long limit, boolean isRootDomain, ResourceType type, long domainId) {
        long max = Resource.RESOURCE_UNLIMITED; // if resource limit is not found, then we treat it as unlimited

        // No limits for Root domain
        if (isRootDomain) {
            return max;
        }
        if (limit != null) {
            return limit.longValue();
        } else {
            return findCorrectResourceLimitForDomain(type, domainId);
        }
    }

    public static long findCorrectResourceLimit(ResourceType type, long accountId) {
        AccountVO account = s_accountDao.findById(accountId);

        if (account == null) {
            return -1;
        }

        return s_resourceLimitMgr.findCorrectResourceLimitForAccount(account, type);
    }

    public static long findCorrectResourceLimit(Long limit, long accountId, ResourceType type) {
        return s_resourceLimitMgr.findCorrectResourceLimitForAccount(accountId, limit, type);
    }

    public static long findCorrectResourceLimitForDomain(Long limit, ResourceType resourceType, long domainId) {
        //-- No limits for Root domain
        if (domainId == Domain.ROOT_DOMAIN) {
            return Resource.RESOURCE_UNLIMITED;
        }
        //--If limit doesn't have a value then fetch default limit from the configs
        return (limit == null) ? s_resourceLimitMgr.findDefaultResourceLimitForDomain(resourceType) : limit;
    }

    public static long getResourceCount(ResourceType type, long accountId) {
        AccountVO account = s_accountDao.findById(accountId);

        if (account == null) {
            return -1;
        }

        return s_resourceLimitMgr.getResourceCount(account, type);
    }

    public static String getSecurityGroupsNamesForVm(long vmId) {
        return s_securityGroupMgr.getSecurityGroupsNamesForVm(vmId);
    }

    public static List<SecurityGroupVO> getSecurityGroupsForVm(long vmId) {
        return s_securityGroupMgr.getSecurityGroupsForVm(vmId);
    }

    public static String getSnapshotIntervalTypes(long snapshotId) {
        SnapshotVO snapshot = s_snapshotDao.findById(snapshotId);
        return snapshot.getRecurringType().name();
    }

    public static String getSnapshotLocationType(long snapshotId) {
        SnapshotVO snapshot = s_snapshotDao.findById(snapshotId);

        return snapshot.getLocationType() != null ? snapshot.getLocationType().name() : null;
    }

    public static String getStoragePoolTags(long poolId) {
        return s_storageMgr.getStoragePoolTags(poolId);
    }

    public static boolean isLocalStorageActiveOnHost(Long hostId) {
        return s_storageMgr.isLocalStorageActiveOnHost(hostId);
    }

    public static InstanceGroupVO findInstanceGroupForVM(long vmId) {
        return s_userVmMgr.getGroupForVm(vmId);
    }

    // ///////////////////////////////////////////////////////////
    // Misc methods //
    // ///////////////////////////////////////////////////////////

    public static HostStats getHostStatistics(long hostId) {
        return s_statsCollector.getHostStats(hostId);
    }

    public static StorageStats getStoragePoolStatistics(long id) {
        return s_statsCollector.getStoragePoolStats(id);
    }

    public static VmStats getVmStatistics(long hostId) {
        return s_statsCollector.getVmStats(hostId);
    }

    public static VolumeStats getVolumeStatistics(String volumeUuid) {
        return s_statsCollector.getVolumeStats(volumeUuid);
    }

    public static StorageStats getSecondaryStorageStatistics(long id) {
        return s_statsCollector.getStorageStats(id);
    }

    public static CapacityVO getStoragePoolUsedStats(Long poolId, Long clusterId, Long podId, Long zoneId) {
        return s_storageMgr.getStoragePoolUsedStats(poolId, clusterId, podId, zoneId);
    }

    public static CapacityVO getSecondaryStorageUsedStats(Long hostId, Long zoneId) {
        return s_storageMgr.getSecondaryStorageUsedStats(hostId, zoneId);
    }

    // ///////////////////////////////////////////////////////////
    // Dao methods //
    // ///////////////////////////////////////////////////////////

    public static Account findAccountById(Long accountId) {
        return s_accountDao.findByIdIncludingRemoved(accountId);
    }

    public static Account findAccountByIdIncludingRemoved(Long accountId) {
        return s_accountDao.findByIdIncludingRemoved(accountId);
    }

    public static Account findAccountByNameDomain(String accountName, Long domainId) {
        return s_accountDao.findActiveAccount(accountName, domainId);
    }

    public static ClusterVO findClusterById(long clusterId) {
        return s_clusterDao.findById(clusterId);
    }

    public static String findClusterDetails(long clusterId, String name) {
        ClusterDetailsVO detailsVO = s_clusterDetailsDao.findDetail(clusterId, name);
        if (detailsVO != null) {
            return detailsVO.getValue();
        }

        return null;
    }

    public static DiskOfferingVO findDiskOfferingById(Long diskOfferingId) {
        DiskOfferingVO off = s_diskOfferingDao.findByIdIncludingRemoved(diskOfferingId);
        if (off.getType() == DiskOfferingVO.Type.Disk) {
            return off;
        }
        return null;
    }

    public static DomainVO findDomainById(Long domainId) {
        return s_domainDao.findByIdIncludingRemoved(domainId);
    }

    public static DomainJoinVO findDomainJoinVOById(Long domainId) {
        return s_domainJoinDao.findByIdIncludingRemoved(domainId);
    }

    public static DomainVO findDomainByIdIncludingRemoved(Long domainId) {
        return s_domainDao.findByIdIncludingRemoved(domainId);
    }

    public static boolean isChildDomain(long parentId, long childId) {
        return s_domainDao.isChildDomain(parentId, childId);
    }

    public static DomainRouterVO findDomainRouterById(Long routerId) {
        return s_domainRouterDao.findByIdIncludingRemoved(routerId);
    }

    public static GuestOS findGuestOSById(Long id) {
        return s_guestOSDao.findByIdIncludingRemoved(id);
    }

    public static GuestOS findGuestOSByDisplayName(String displayName) {
        return s_guestOSDao.listByDisplayName(displayName);
    }

    public static HostVO findHostById(Long hostId) {
        return s_hostDao.findByIdIncludingRemoved(hostId);
    }

    public static HostVO findHostByTypeNameAndZoneId(Long zoneId, String name, Host.Type type) {
        return s_hostDao.findByTypeNameAndZoneId(zoneId, name, type);
    }

    public static IPAddressVO findIpAddressById(long addressId) {
        return s_ipAddressDao.findById(addressId);
    }

    public static GuestOSCategoryVO getHostGuestOSCategory(long hostId) {
        Long guestOSCategoryID = s_resourceMgr.getGuestOSCategoryId(hostId);

        if (guestOSCategoryID != null) {
            return s_guestOSCategoryDao.findById(guestOSCategoryID);
        } else {
            return null;
        }
    }

    public static String getHostTags(long hostId) {
        return s_resourceMgr.getHostTags(hostId);
    }

    public static LoadBalancerVO findLoadBalancerById(Long loadBalancerId) {
        return s_loadBalancerDao.findById(loadBalancerId);
    }

    public static NetworkRuleConfigVO findNetworkRuleById(Long ruleId) {
        return s_networkRuleConfigDao.findById(ruleId);
    }

    public static SecurityGroup findSecurityGroupById(Long groupId) {
        return s_securityGroupDao.findById(groupId);
    }

    public static HostPodVO findPodById(Long podId) {
        return s_podDao.findById(podId);
    }

    public static VolumeVO findRootVolume(long vmId) {
        List<VolumeVO> volumes = s_volumeDao.findByInstanceAndType(vmId, Type.ROOT);
        if (volumes != null && volumes.size() == 1) {
            return volumes.get(0);
        } else {
            return null;
        }
    }

    public static ServiceOffering findServiceOfferingById(Long serviceOfferingId) {
        return s_serviceOfferingDao.findByIdIncludingRemoved(serviceOfferingId);
    }

    public static ServiceOffering findServiceOfferingByUuid(String serviceOfferingUuid) {
        return s_serviceOfferingDao.findByUuidIncludingRemoved(serviceOfferingUuid);
    }

    public static ServiceOfferingDetailsVO findServiceOfferingDetail(long serviceOfferingId, String key) {
        return s_serviceOfferingDetailsDao.findDetail(serviceOfferingId, key);
    }

    public static Snapshot findSnapshotById(long snapshotId) {
        return s_snapshotDao.findByIdIncludingRemoved(snapshotId);
    }

    public static StoragePoolVO findStoragePoolById(Long storagePoolId) {
        return s_storagePoolDao.findByIdIncludingRemoved(storagePoolId);
    }

    public static VMTemplateVO findTemplateById(Long templateId) {
        VMTemplateVO template = s_templateDao.findByIdIncludingRemoved(templateId);
        if (template != null) {
            Map<String, String> details = s_templateDetailsDao.listDetailsKeyPairs(templateId);
            if (details != null && !details.isEmpty()) {
                template.setDetails(details);
            }
        }
        return template;
    }

    public static UploadVO findUploadById(Long id) {
        return s_uploadDao.findById(id);
    }

    public static User findUserById(Long userId) {
        return s_userDao.findById(userId);
    }

    public static UserAccountJoinVO findUserAccountById(Long id) {
        return s_userAccountJoinDao.findById(id);
    }

    public static UserVm findUserVmById(Long vmId) {
        return s_userVmDao.findById(vmId);
    }

    public static VlanVO findVlanById(long vlanDbId) {
        return s_vlanDao.findById(vlanDbId);
    }

    public static VolumeVO findVolumeById(Long volumeId) {
        return s_volumeDao.findByIdIncludingRemoved(volumeId);
    }

    public static Site2SiteVpnGatewayVO findVpnGatewayById(Long vpnGatewayId) {
        return s_site2SiteVpnGatewayDao.findById(vpnGatewayId);
    }

    public static Site2SiteCustomerGatewayVO findCustomerGatewayById(Long customerGatewayId) {
        return s_site2SiteCustomerGatewayDao.findById(customerGatewayId);
    }

    public static List<UserVO> listUsersByAccount(long accountId) {
        return s_userDao.listByAccount(accountId);
    }

    public static DataCenterVO findZoneById(Long zoneId) {
        return s_zoneDao.findById(zoneId);
    }

    public static Long getAccountIdForVlan(long vlanDbId) {
        List<AccountVlanMapVO> accountVlanMaps = s_accountVlanMapDao.listAccountVlanMapsByVlan(vlanDbId);
        if (accountVlanMaps.isEmpty()) {
            return null;
        } else {
            return accountVlanMaps.get(0).getAccountId();
        }
    }

    public static Long getAccountIdForGuestVlan(long vlanDbId) {
        List<AccountGuestVlanMapVO> accountGuestVlanMaps = s_accountGuestVlanMapDao.listAccountGuestVlanMapsByVlan(vlanDbId);
        if (accountGuestVlanMaps.isEmpty()) {
            return null;
        } else {
            return accountGuestVlanMaps.get(0).getAccountId();
        }
    }

    public static HypervisorType getVolumeHyperType(long volumeId) {
        return s_volumeDao.getHypervisorType(volumeId);
    }

    public static HypervisorType getHypervisorTypeFromFormat(long dcId, ImageFormat format){
        HypervisorType type = s_storageMgr.getHypervisorTypeFromFormat(format);
        if (format == ImageFormat.VHD) {
            // Xenserver and Hyperv both support vhd format. Additionally hyperv is only supported
            // in a dc/zone if there aren't any other hypervisor types present in the zone). If the
            // format type is VHD check is any xenserver clusters are present. If not, we assume it
            // is a hyperv zone and update the type.
            List<ClusterVO> xenClusters = s_clusterDao.listByDcHyType(dcId, HypervisorType.XenServer.toString());
            if (xenClusters.isEmpty()) {
                type = HypervisorType.Hyperv;
            }
        } if (format == ImageFormat.RAW) {
            // Currently, KVM only supports RBD and PowerFlex images of type RAW.
            // This results in a weird collision with OVM volumes which
            // can only be raw, thus making KVM RBD volumes show up as OVM
            // rather than RBD. This block of code can (hopefuly) by checking to
            // see if the pool is using either RBD or NFS. However, it isn't
            // quite clear what to do if both storage types are used. If the image
            // format is RAW, it narrows the hypervisor choice down to OVM and KVM / RBD or KVM / CLVM
            // This would be better implemented at a cluster level.
            List<StoragePoolVO> pools = s_storagePoolDao.listByDataCenterId(dcId);
            ListIterator<StoragePoolVO> itr = pools.listIterator();
            while(itr.hasNext()) {
                StoragePoolVO pool = itr.next();
                if(pool.getPoolType() == StoragePoolType.RBD || pool.getPoolType() == StoragePoolType.PowerFlex || pool.getPoolType() == StoragePoolType.CLVM) {
                  // This case will note the presence of non-qcow2 primary stores, suggesting KVM without NFS. Otherwse,
                  // If this check is not passed, the hypervisor type will remain OVM.
                  type = HypervisorType.KVM;
                  break;
                }
            }
        }
        return type;
    }

    public static List<HostGpuGroupsVO> getGpuGroups(long hostId) {
        return s_hostGpuGroupsDao.listByHostId(hostId);
    }

    public static List<VgpuTypesInfo> getGpuCapacites(Long zoneId, Long podId, Long clusterId) {
        return s_vgpuTypesDao.listGPUCapacities(zoneId, podId, clusterId);
    }

    public static HashMap<String, Long> getVgpuVmsCount(Long zoneId, Long podId, Long clusterId) {
        return s_vmDao.countVgpuVMs(zoneId, podId, clusterId);
    }

    public static List<VGPUTypesVO> getVgpus(long groupId) {
        return s_vgpuTypesDao.listByGroupId(groupId);
    }

    public static List<UserStatisticsVO> listUserStatsBy(Long accountId) {
        return s_userStatsDao.listBy(accountId);
    }

    public static List<UserVmVO> listUserVMsByHostId(long hostId) {
        return s_userVmDao.listByHostId(hostId);
    }

    public static List<UserVmVO> listUserVMsByNetworkId(long networkId) {
        return s_userVmDao.listByNetworkIdAndStates(networkId, VirtualMachine.State.Running,
                VirtualMachine.State.Starting, VirtualMachine.State.Stopping, VirtualMachine.State.Unknown,
                VirtualMachine.State.Migrating);
    }

    public static List<DomainRouterVO> listDomainRoutersByNetworkId(long networkId) {
        return s_domainRouterDao.findByNetwork(networkId);
    }

    public static List<DataCenterVO> listZones() {
        return s_zoneDao.listAll();
    }

    public static boolean volumeIsOnSharedStorage(long volumeId) {
        // Check that the volume is valid
        VolumeVO volume = s_volumeDao.findById(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Please specify a valid volume ID.");
        }

        return s_volumeMgr.volumeOnSharedStoragePool(volume);
    }

    public static List<NicProfile> getNics(VirtualMachine vm) {
        return s_networkMgr.getNicProfiles(vm);
    }

    public static NetworkProfile getNetworkProfile(long networkId) {
        return s_networkMgr.convertNetworkToNetworkProfile(networkId);
    }

    public static NetworkOfferingResponse newNetworkOfferingResponse(NetworkOffering offering) {
        return s_networkOfferingJoinDao.newNetworkOfferingResponse(offering);
    }

    public static NetworkOfferingJoinVO newNetworkOfferingView(NetworkOffering offering) {
        return s_networkOfferingJoinDao.newNetworkOfferingView(offering);
    }

    public static NetworkOfferingVO findNetworkOfferingById(long networkOfferingId) {
        return s_networkOfferingDao.findByIdIncludingRemoved(networkOfferingId);
    }

    public static List<? extends Vlan> listVlanByNetworkId(long networkId) {
        return s_vlanDao.listVlansByNetworkId(networkId);
    }

    public static PhysicalNetworkVO findPhysicalNetworkById(long id) {
        return s_physicalNetworkDao.findById(id);
    }

    public static PhysicalNetworkTrafficTypeVO findPhysicalNetworkTrafficTypeById(long id) {
        return s_physicalNetworkTrafficTypeDao.findById(id);
    }

    public static NetworkVO findNetworkById(long id) {
        return s_networkDao.findByIdIncludingRemoved(id);
    }

    public static Map<Service, Map<Capability, String>> getNetworkCapabilities(long networkId, long zoneId) {
        return s_networkModel.getNetworkCapabilities(networkId);
    }

    public static long getPublicNetworkIdByZone(long zoneId) {
        return s_networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, TrafficType.Public).getId();
    }

    public static Long getVlanNetworkId(long vlanId) {
        VlanVO vlan = s_vlanDao.findById(vlanId);
        if (vlan != null) {
            return vlan.getNetworkId();
        } else {
            return null;
        }
    }

    public static Integer getNetworkRate(long networkOfferingId) {
        return s_configMgr.getNetworkOfferingNetworkRate(networkOfferingId, null);
    }

    public static Account getVlanAccount(long vlanId) {
        return s_configSvc.getVlanAccount(vlanId);
    }

    public static Domain getVlanDomain(long vlanId) {
        return s_configSvc.getVlanDomain(vlanId);
    }

    public static boolean isSecurityGroupEnabledInZone(long zoneId) {
        DataCenterVO dc = s_zoneDao.findById(zoneId);
        if (dc == null) {
            return false;
        } else {
            return dc.isSecurityGroupEnabled();
        }
    }

    public static Long getDedicatedNetworkDomain(long networkId) {
        return s_networkModel.getDedicatedNetworkDomain(networkId);
    }

    public static float getCpuOverprovisioningFactor(long clusterId) {
        float opFactor = CapacityManager.CpuOverprovisioningFactor.valueIn(clusterId);
        return opFactor;
    }

    public static float getMemOverprovisioningFactor(long clusterId) {
        float opFactor = CapacityManager.MemOverprovisioningFactor.valueIn(clusterId);
        return opFactor;
    }

    public static boolean isExtractionDisabled() {
        String disableExtractionString = s_configDao.getValue(Config.DisableExtraction.toString());
        boolean disableExtraction  = (disableExtractionString == null) ? false : Boolean.parseBoolean(disableExtractionString);
        return disableExtraction;
    }

    public static SecurityGroup getSecurityGroup(String groupName, long ownerId) {
        return s_securityGroupMgr.getSecurityGroup(groupName, ownerId);
    }

    public static ConsoleProxyVO findConsoleProxy(long id) {
        return s_consoleProxyDao.findById(id);
    }

    public static List<String> findFirewallSourceCidrs(long id) {
        return s_firewallCidrsDao.getSourceCidrs(id);
    }

    public static List<String> findFirewallDestCidrs(long id){
        return s_firewallDcidrsDao.getDestCidrs(id);
    }

    public static Account getProjectOwner(long projectId) {
        return s_projectMgr.getProjectOwner(projectId);
    }

    public static Project findProjectByProjectAccountId(long projectAccountId) {
        return s_projectMgr.findByProjectAccountId(projectAccountId);
    }

    public static Project findProjectByProjectAccountIdIncludingRemoved(long projectAccountId) {
        return s_projectMgr.findByProjectAccountIdIncludingRemoved(projectAccountId);
    }

    public static Project findProjectById(long projectId) {
        return s_projectMgr.getProject(projectId);
    }

    public static long getProjectOwnwerId(long projectId) {
        return s_projectMgr.getProjectOwner(projectId).getId();
    }

    public static Map<String, String> getAccountDetails(long accountId) {
        Map<String, String> details = s_accountDetailsDao.findDetails(accountId);
        return details.isEmpty() ? null : details;
    }

    public static Map<Service, Set<Provider>> listNetworkOfferingServices(long networkOfferingId) {
        return s_networkModel.getNetworkOfferingServiceProvidersMap(networkOfferingId);
    }

    public static List<Service> getElementServices(Provider provider) {
        return s_networkModel.getElementServices(provider);
    }

    public static List<? extends Provider> getProvidersForService(Service service) {
        return s_networkModel.listSupportedNetworkServiceProviders(service.getName());
    }

    public static boolean canElementEnableIndividualServices(Provider serviceProvider) {
        return s_networkModel.canElementEnableIndividualServices(serviceProvider);
    }

    public static Pair<Long, Boolean> getDomainNetworkDetails(long networkId) {
        NetworkDomainVO map = s_networkDomainDao.getDomainNetworkMapByNetworkId(networkId);

        boolean subdomainAccess = (map.isSubdomainAccess() != null) ? map.isSubdomainAccess() : s_networkModel.getAllowSubdomainAccessGlobal();

        return new Pair<Long, Boolean>(map.getDomainId(), subdomainAccess);
    }

    public static long countFreePublicIps() {
        return s_ipAddressDao.countFreePublicIPs();
    }

    public static long findDefaultRouterServiceOffering() {
        ServiceOfferingVO serviceOffering = s_serviceOfferingDao.findByName(ServiceOffering.routerDefaultOffUniqueName);
        return serviceOffering.getId();
    }

    public static IpAddress findIpByAssociatedVmId(long vmId) {
        return s_ipAddressDao.findByAssociatedVmId(vmId);
    }

    public static String getHaTag() {
        return s_haMgr.getHaTag();
    }

    public static Map<Service, Set<Provider>> listVpcOffServices(long vpcOffId) {
        return s_vpcMgr.getVpcOffSvcProvidersMap(vpcOffId);
    }

    public static List<? extends Network> listVpcNetworks(long vpcId) {
        return s_networkModel.listNetworksByVpc(vpcId);
    }

    public static boolean canUseForDeploy(Network network) {
        return s_networkModel.canUseForDeploy(network);
    }

    public static VMSnapshot getVMSnapshotById(Long vmSnapshotId) {
        VMSnapshot vmSnapshot = s_vmSnapshotDao.findById(vmSnapshotId);
        return vmSnapshot;
    }

    public static String getUuid(String resourceId, ResourceObjectType resourceType) {
        return s_taggedResourceService.getUuid(resourceId, resourceType);
    }

    public static List<? extends ResourceTag> listByResourceTypeAndId(ResourceObjectType type, long resourceId) {
        return s_taggedResourceService.listByResourceTypeAndId(type, resourceId);
    }

    public static List<ConditionVO> getAutoScalePolicyConditions(long policyId) {
        List<AutoScalePolicyConditionMapVO> vos = s_asPolicyConditionMapDao.listByAll(policyId, null);
        ArrayList<ConditionVO> conditions = new ArrayList<ConditionVO>(vos.size());
        for (AutoScalePolicyConditionMapVO vo : vos) {
            conditions.add(s_asConditionDao.findById(vo.getConditionId()));
        }

        return conditions;
    }

    public static void getAutoScaleVmGroupPolicyIds(long vmGroupId, List<Long> scaleUpPolicyIds, List<Long> scaleDownPolicyIds) {
        List<AutoScaleVmGroupPolicyMapVO> vos = s_asVmGroupPolicyMapDao.listByVmGroupId(vmGroupId);
        for (AutoScaleVmGroupPolicyMapVO vo : vos) {
            AutoScalePolicy autoScalePolicy = s_asPolicyDao.findById(vo.getPolicyId());
            if (autoScalePolicy.getAction().equals("scaleup")) {
                scaleUpPolicyIds.add(autoScalePolicy.getId());
            } else {
                scaleDownPolicyIds.add(autoScalePolicy.getId());
            }
        }
    }

    public static String getKeyPairName(String sshPublicKey) {
        SSHKeyPairVO sshKeyPair = s_sshKeyPairDao.findByPublicKey(sshPublicKey);
        //key might be removed prior to this point
        if (sshKeyPair != null) {
            return sshKeyPair.getName();
        }
        return null;
    }

    public static UserVmDetailVO  findPublicKeyByVmId(long vmId) {
        return s_userVmDetailsDao.findDetail(vmId, VmDetailConstants.SSH_PUBLIC_KEY);
    }

    public static void getAutoScaleVmGroupPolicies(long vmGroupId, List<AutoScalePolicy> scaleUpPolicies, List<AutoScalePolicy> scaleDownPolicies) {
        List<AutoScaleVmGroupPolicyMapVO> vos = s_asVmGroupPolicyMapDao.listByVmGroupId(vmGroupId);
        for (AutoScaleVmGroupPolicyMapVO vo : vos) {
            AutoScalePolicy autoScalePolicy = s_asPolicyDao.findById(vo.getPolicyId());
            if (autoScalePolicy.getAction().equals("scaleup")) {
                scaleUpPolicies.add(autoScalePolicy);
            } else {
                scaleDownPolicies.add(autoScalePolicy);
            }
        }
    }

    public static CounterVO getCounter(long counterId) {
        return s_counterDao.findById(counterId);
    }

    public static ConditionVO findConditionById(long conditionId) {
        return s_asConditionDao.findById(conditionId);
    }

    public static PhysicalNetworkServiceProviderVO findPhysicalNetworkServiceProviderById(long providerId) {
        return s_physicalNetworkServiceProviderDao.findById(providerId);
    }

    public static FirewallRuleVO findFirewallRuleById(long ruleId) {
        return s_firewallRuleDao.findById(ruleId);
    }

    public static StaticRouteVO findStaticRouteById(long routeId) {
        return s_staticRouteDao.findById(routeId);
    }

    public static VpcGatewayVO findVpcGatewayById(long gatewayId) {
        return s_vpcGatewayDao.findById(gatewayId);
    }

    public static AutoScalePolicyVO findAutoScalePolicyById(long policyId) {
        return s_asPolicyDao.findById(policyId);
    }

    public static AutoScaleVmProfileVO findAutoScaleVmProfileById(long profileId) {
        return s_asVmProfileDao.findById(profileId);
    }

    public static AutoScaleVmGroupVO findAutoScaleVmGroupById(long groupId) {
        return s_asVmGroupDao.findById(groupId);
    }

    public static GuestOSCategoryVO findGuestOsCategoryById(long catId) {
        return s_guestOSCategoryDao.findById(catId);
    }

    public static VpcVO findVpcById(long vpcId) {
        return s_vpcDao.findById(vpcId);
    }

    public static VpcVO findVpcByIdIncludingRemoved(long vpcId) {
        return s_vpcDao.findByIdIncludingRemoved(vpcId);
    }

    public static SnapshotPolicy findSnapshotPolicyById(long policyId) {
        return s_snapshotPolicyDao.findById(policyId);
    }

    public static VpcOffering findVpcOfferingById(long offeringId) {
        return s_vpcOfferingDao.findById(offeringId);
    }

    public static VpcOfferingResponse newVpcOfferingResponse(VpcOffering offering) {
        return s_vpcOfferingJoinDao.newVpcOfferingResponse(offering);
    }

    public static VpcOfferingJoinVO newVpcOfferingView(VpcOffering offering) {
        return s_vpcOfferingJoinDao.newVpcOfferingView(offering);
    }

    public static NetworkACL findByNetworkACLId(long aclId) {
        return s_networkACLDao.findById(aclId);
    }

    public static AsyncJob findAsyncJobById(long jobId) {
        return s_asyncJobDao.findById(jobId);
    }

    public static String findJobInstanceUuid(AsyncJob job) {
        if (job == null) {
            return null;
        }
        String jobInstanceId = null;
        ApiCommandJobType jobInstanceType = EnumUtils.fromString(ApiCommandJobType.class, job.getInstanceType(), ApiCommandJobType.None);

        if (job.getInstanceId() == null) {
            // when assert is hit, implement 'getInstanceId' of BaseAsyncCmd and return appropriate instance id
            assert (false);
            return null;
        }

        if (jobInstanceType == ApiCommandJobType.Volume) {
            VolumeVO volume = ApiDBUtils.findVolumeById(job.getInstanceId());
            if (volume != null) {
                jobInstanceId = volume.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.Template || jobInstanceType == ApiCommandJobType.Iso) {
            VMTemplateVO template = ApiDBUtils.findTemplateById(job.getInstanceId());
            if (template != null) {
                jobInstanceId = template.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.VirtualMachine || jobInstanceType == ApiCommandJobType.ConsoleProxy ||
            jobInstanceType == ApiCommandJobType.SystemVm || jobInstanceType == ApiCommandJobType.DomainRouter) {
            VMInstanceVO vm = ApiDBUtils.findVMInstanceById(job.getInstanceId());
            if (vm != null) {
                jobInstanceId = vm.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.Snapshot) {
            Snapshot snapshot = ApiDBUtils.findSnapshotById(job.getInstanceId());
            if (snapshot != null) {
                jobInstanceId = snapshot.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.Host) {
            Host host = ApiDBUtils.findHostById(job.getInstanceId());
            if (host != null) {
                jobInstanceId = host.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.StoragePool) {
            StoragePoolVO spool = ApiDBUtils.findStoragePoolById(job.getInstanceId());
            if (spool != null) {
                jobInstanceId = spool.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.IpAddress) {
            IPAddressVO ip = ApiDBUtils.findIpAddressById(job.getInstanceId());
            if (ip != null) {
                jobInstanceId = ip.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.SecurityGroup) {
            SecurityGroup sg = ApiDBUtils.findSecurityGroupById(job.getInstanceId());
            if (sg != null) {
                jobInstanceId = sg.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.PhysicalNetwork) {
            PhysicalNetworkVO pnet = ApiDBUtils.findPhysicalNetworkById(job.getInstanceId());
            if (pnet != null) {
                jobInstanceId = pnet.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.TrafficType) {
            PhysicalNetworkTrafficTypeVO trafficType = ApiDBUtils.findPhysicalNetworkTrafficTypeById(job.getInstanceId());
            if (trafficType != null) {
                jobInstanceId = trafficType.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.PhysicalNetworkServiceProvider) {
            PhysicalNetworkServiceProvider sp = ApiDBUtils.findPhysicalNetworkServiceProviderById(job.getInstanceId());
            if (sp != null) {
                jobInstanceId = sp.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.FirewallRule) {
            FirewallRuleVO fw = ApiDBUtils.findFirewallRuleById(job.getInstanceId());
            if (fw != null) {
                jobInstanceId = fw.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.Account) {
            Account acct = ApiDBUtils.findAccountById(job.getInstanceId());
            if (acct != null) {
                jobInstanceId = acct.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.User) {
            User usr = ApiDBUtils.findUserById(job.getInstanceId());
            if (usr != null) {
                jobInstanceId = usr.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.StaticRoute) {
            StaticRouteVO route = ApiDBUtils.findStaticRouteById(job.getInstanceId());
            if (route != null) {
                jobInstanceId = route.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.PrivateGateway) {
            VpcGatewayVO gateway = ApiDBUtils.findVpcGatewayById(job.getInstanceId());
            if (gateway != null) {
                jobInstanceId = gateway.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.Counter) {
            CounterVO counter = ApiDBUtils.getCounter(job.getInstanceId());
            if (counter != null) {
                jobInstanceId = counter.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.Condition) {
            ConditionVO condition = ApiDBUtils.findConditionById(job.getInstanceId());
            if (condition != null) {
                jobInstanceId = condition.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.AutoScalePolicy) {
            AutoScalePolicyVO policy = ApiDBUtils.findAutoScalePolicyById(job.getInstanceId());
            if (policy != null) {
                jobInstanceId = policy.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.AutoScaleVmProfile) {
            AutoScaleVmProfileVO profile = ApiDBUtils.findAutoScaleVmProfileById(job.getInstanceId());
            if (profile != null) {
                jobInstanceId = profile.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.AutoScaleVmGroup) {
            AutoScaleVmGroupVO group = ApiDBUtils.findAutoScaleVmGroupById(job.getInstanceId());
            if (group != null) {
                jobInstanceId = group.getUuid();
            }
        } else if (jobInstanceType == ApiCommandJobType.Network) {
            NetworkVO networkVO = ApiDBUtils.findNetworkById(job.getInstanceId());
            if(networkVO != null) {
                jobInstanceId = networkVO.getUuid();
            }
        } else if (jobInstanceType != ApiCommandJobType.None) {
            // TODO : when we hit here, we need to add instanceType -> UUID
            // entity table mapping
            assert (false);
        }
        return jobInstanceId;
    }

    ///////////////////////////////////////////////////////////////////////
    //  Newly Added Utility Methods for List API refactoring             //
    ///////////////////////////////////////////////////////////////////////

    public static DomainRouterResponse newDomainRouterResponse(DomainRouterJoinVO vr, Account caller) {
        DomainRouterResponse response = s_domainRouterJoinDao.newDomainRouterResponse(vr, caller);
        if (StringUtils.isBlank(response.getHypervisor())) {
            VMInstanceVO vm = ApiDBUtils.findVMInstanceById(vr.getId());
            if (vm.getLastHostId() != null) {
                HostVO lastHost = ApiDBUtils.findHostById(vm.getLastHostId());
                if (lastHost != null) {
                    response.setHypervisor(lastHost.getHypervisorType().toString());
                }
            }
        }
        return  response;
    }

    public static DomainRouterResponse fillRouterDetails(DomainRouterResponse vrData, DomainRouterJoinVO vr) {
        return s_domainRouterJoinDao.setDomainRouterResponse(vrData, vr);
    }

    public static List<DomainRouterJoinVO> newDomainRouterView(VirtualRouter vr) {
        return s_domainRouterJoinDao.newDomainRouterView(vr);
    }

    public static UserVmResponse newUserVmResponse(ResponseView view, String objectName, UserVmJoinVO userVm, EnumSet<VMDetails> details, Account caller) {
        return s_userVmJoinDao.newUserVmResponse(view, objectName, userVm, details, caller);
    }

    public static UserVmResponse fillVmDetails(ResponseView view, UserVmResponse vmData, UserVmJoinVO vm) {
        return s_userVmJoinDao.setUserVmResponse(view, vmData, vm);
    }

    public static List<UserVmJoinVO> newUserVmView(UserVm... userVms) {
        return s_userVmJoinDao.newUserVmView(userVms);
    }

    public static SecurityGroupResponse newSecurityGroupResponse(SecurityGroupJoinVO vsg, Account caller) {
        return s_securityGroupJoinDao.newSecurityGroupResponse(vsg, caller);
    }

    public static SecurityGroupResponse fillSecurityGroupDetails(SecurityGroupResponse vsgData, SecurityGroupJoinVO sg) {
        return s_securityGroupJoinDao.setSecurityGroupResponse(vsgData, sg);
    }

    public static List<SecurityGroupJoinVO> newSecurityGroupView(SecurityGroup sg) {
        return s_securityGroupJoinDao.newSecurityGroupView(sg);
    }

    public static List<SecurityGroupJoinVO> findSecurityGroupViewById(Long sgId) {
        return s_securityGroupJoinDao.searchByIds(sgId);
    }

    public static ResourceTagResponse newResourceTagResponse(ResourceTagJoinVO vsg, boolean keyValueOnly) {
        return s_tagJoinDao.newResourceTagResponse(vsg, keyValueOnly);
    }

    public static ResourceTagJoinVO newResourceTagView(ResourceTag sg) {
        return s_tagJoinDao.newResourceTagView(sg);
    }

    public static ResourceTagJoinVO findResourceTagViewById(Long tagId) {
        return s_tagJoinDao.searchById(tagId);
    }

    public static EventResponse newEventResponse(EventJoinVO ve) {
        return s_eventJoinDao.newEventResponse(ve);
    }

    public static EventJoinVO newEventView(Event e) {
        return s_eventJoinDao.newEventView(e);
    }

    public static InstanceGroupResponse newInstanceGroupResponse(InstanceGroupJoinVO ve) {
        return s_vmGroupJoinDao.newInstanceGroupResponse(ve);
    }

    public static InstanceGroupJoinVO newInstanceGroupView(InstanceGroup e) {
        return s_vmGroupJoinDao.newInstanceGroupView(e);
    }

    public static UserResponse newUserResponse(UserAccountJoinVO usr) {
        return newUserResponse(usr, null);
    }

    public static UserResponse newUserResponse(UserAccountJoinVO usr, Long domainId) {
        UserResponse response = s_userAccountJoinDao.newUserResponse(usr);
        if(!AccountManager.UseSecretKeyInResponse.value()){
            response.setSecretKey(null);
        }
        // Populate user account role information
        if (usr.getAccountRoleId() != null) {
            Role role = s_roleService.findRole( usr.getAccountRoleId());
            if (role != null) {
                response.setRoleId(role.getUuid());
                response.setRoleType(role.getRoleType());
                response.setRoleName(role.getName());
            }
        }
        if (domainId != null && usr.getDomainId() != domainId)
            response.setIsCallerChildDomain(true);
        else
            response.setIsCallerChildDomain(false);
        return response;
    }

    public static UserAccountJoinVO newUserView(User usr) {
        return s_userAccountJoinDao.newUserView(usr);
    }

    public static UserAccountJoinVO newUserView(UserAccount usr) {
        return s_userAccountJoinDao.newUserView(usr);
    }

    public static ProjectResponse newProjectResponse(EnumSet<DomainDetails> details, ProjectJoinVO proj) {
        return s_projectJoinDao.newProjectResponse(details, proj);
    }

    public static List<ProjectJoinVO> newProjectView(Project proj) {

        return s_projectJoinDao.newProjectView(proj);
    }

    public static List<UserAccountJoinVO> findUserViewByAccountId(Long accountId) {
        return s_userAccountJoinDao.searchByAccountId(accountId);
    }


    public static ProjectAccountResponse newProjectAccountResponse(ProjectAccountJoinVO proj) {
        return s_projectAccountJoinDao.newProjectAccountResponse(proj);
    }

    public static ProjectAccountJoinVO newProjectAccountView(ProjectAccount proj) {
        return s_projectAccountJoinDao.newProjectAccountView(proj);
    }

    public static ProjectInvitationResponse newProjectInvitationResponse(ProjectInvitationJoinVO proj) {
        return s_projectInvitationJoinDao.newProjectInvitationResponse(proj);
    }

    public static ProjectInvitationJoinVO newProjectInvitationView(ProjectInvitation proj) {
        return s_projectInvitationJoinDao.newProjectInvitationView(proj);
    }

    public static HostResponse newHostResponse(HostJoinVO vr, EnumSet<HostDetails> details) {
        return s_hostJoinDao.newHostResponse(vr, details);
    }

    public static HostForMigrationResponse newHostForMigrationResponse(HostJoinVO vr, EnumSet<HostDetails> details) {
        return s_hostJoinDao.newHostForMigrationResponse(vr, details);
    }

    public static List<HostJoinVO> newHostView(Host vr) {
        return s_hostJoinDao.newHostView(vr);
    }

    public static VolumeResponse newVolumeResponse(ResponseView view, VolumeJoinVO vr) {
        return s_volJoinDao.newVolumeResponse(view, vr);
    }

    public static VolumeResponse fillVolumeDetails(ResponseView view, VolumeResponse vrData, VolumeJoinVO vr) {
        return s_volJoinDao.setVolumeResponse(view, vrData, vr);
    }

    public static List<VolumeJoinVO> newVolumeView(Volume vr) {
        return s_volJoinDao.newVolumeView(vr);
    }

    public static StoragePoolResponse newStoragePoolResponse(StoragePoolJoinVO vr) {
        return s_poolJoinDao.newStoragePoolResponse(vr);
    }

    public static StorageTagResponse newStorageTagResponse(StoragePoolTagVO vr) {
        return s_tagDao.newStorageTagResponse(vr);
    }

    public static HostTagResponse newHostTagResponse(HostTagVO vr) {
        return s_hostTagDao.newHostTagResponse(vr);
    }

    public static StoragePoolResponse fillStoragePoolDetails(StoragePoolResponse vrData, StoragePoolJoinVO vr) {
        return s_poolJoinDao.setStoragePoolResponse(vrData, vr);
    }

    public static StoragePoolResponse newStoragePoolForMigrationResponse(StoragePoolJoinVO vr) {
        return s_poolJoinDao.newStoragePoolForMigrationResponse(vr);
    }

    public static StoragePoolResponse fillStoragePoolForMigrationDetails(StoragePoolResponse vrData, StoragePoolJoinVO vr) {
        return s_poolJoinDao.setStoragePoolForMigrationResponse(vrData, vr);
    }

    public static List<StoragePoolJoinVO> newStoragePoolView(StoragePool vr) {
        return s_poolJoinDao.newStoragePoolView(vr);
    }

    public static ImageStoreResponse newImageStoreResponse(ImageStoreJoinVO vr) {
        return s_imageStoreJoinDao.newImageStoreResponse(vr);
    }

    public static ImageStoreResponse fillImageStoreDetails(ImageStoreResponse vrData, ImageStoreJoinVO vr) {
        return s_imageStoreJoinDao.setImageStoreResponse(vrData, vr);
    }

    public static List<ImageStoreJoinVO> newImageStoreView(ImageStore vr) {
        return s_imageStoreJoinDao.newImageStoreView(vr);
    }

    public static DomainResponse newDomainResponse(ResponseView view, EnumSet<DomainDetails> details, DomainJoinVO ve) {
        return s_domainJoinDao.newDomainResponse(view, details, ve);
    }

    public static AccountResponse newAccountResponse(ResponseView view, EnumSet<DomainDetails> details, AccountJoinVO ve) {
        AccountResponse response = s_accountJoinDao.newAccountResponse(view, details, ve);
        // Populate account role information
        if (ve.getRoleId() != null) {
            Role role = s_roleService.findRole(ve.getRoleId());
            if (role != null) {
                response.setRoleId(role.getUuid());
                response.setRoleType(role.getRoleType());
                response.setRoleName(role.getName());
            }
        }
        return response;
    }

    public static AccountJoinVO newAccountView(Account e) {
        return s_accountJoinDao.newAccountView(e);
    }

    public static AccountJoinVO findAccountViewById(Long accountId) {
        return s_accountJoinDao.findByIdIncludingRemoved(accountId);
    }

    public static AsyncJobResponse newAsyncJobResponse(AsyncJobJoinVO ve) {
        return s_jobJoinDao.newAsyncJobResponse(ve);
    }

    public static AsyncJobJoinVO newAsyncJobView(AsyncJob e) {
        return s_jobJoinDao.newAsyncJobView(e);
    }

    public static DiskOfferingResponse newDiskOfferingResponse(DiskOfferingJoinVO offering) {
        return s_diskOfferingJoinDao.newDiskOfferingResponse(offering);
    }

    public static DiskOfferingJoinVO newDiskOfferingView(DiskOffering offering) {
        return s_diskOfferingJoinDao.newDiskOfferingView(offering);
    }

    public static ServiceOfferingResponse newServiceOfferingResponse(ServiceOfferingJoinVO offering) {
        return s_serviceOfferingJoinDao.newServiceOfferingResponse(offering);
    }

    public static ServiceOfferingJoinVO newServiceOfferingView(ServiceOffering offering) {
        return s_serviceOfferingJoinDao.newServiceOfferingView(offering);
    }

    public static ZoneResponse newDataCenterResponse(ResponseView view, DataCenterJoinVO dc, Boolean showCapacities) {
        return s_dcJoinDao.newDataCenterResponse(view, dc, showCapacities);
    }

    public static DataCenterJoinVO newDataCenterView(DataCenter dc) {
        return s_dcJoinDao.newDataCenterView(dc);
    }

    public static Map<String, String> findHostDetailsById(long hostId) {
        return s_hostDetailsDao.findDetails(hostId);
    }

    public static List<NicSecondaryIpVO> findNicSecondaryIps(long nicId) {
        return s_nicSecondaryIpDao.listByNicId(nicId);
    }

    public static TemplateResponse newTemplateUpdateResponse(TemplateJoinVO vr) {
        return s_templateJoinDao.newUpdateResponse(vr);
    }

    public static TemplateResponse newTemplateResponse(EnumSet<DomainDetails> detailsView, ResponseView view, TemplateJoinVO vr) {
        return s_templateJoinDao.newTemplateResponse(detailsView, view, vr);
    }

    public static TemplateResponse newIsoResponse(TemplateJoinVO vr) {
        return s_templateJoinDao.newIsoResponse(vr);
    }

    public static TemplateResponse fillTemplateDetails(EnumSet<DomainDetails> detailsView, ResponseView view, TemplateResponse vrData, TemplateJoinVO vr) {
        return s_templateJoinDao.setTemplateResponse(detailsView, view, vrData, vr);
    }

    public static List<TemplateJoinVO> newTemplateView(VirtualMachineTemplate vr) {
        return s_templateJoinDao.newTemplateView(vr);
    }

    public static List<TemplateJoinVO> newTemplateView(VirtualMachineTemplate vr, long zoneId, boolean readyOnly) {
        return s_templateJoinDao.newTemplateView(vr, zoneId, readyOnly);
    }

    public static AffinityGroup getAffinityGroup(String groupName, long accountId) {
        return s_affinityGroupDao.findByAccountAndName(accountId, groupName);
    }

    public static AffinityGroupResponse newAffinityGroupResponse(AffinityGroupJoinVO group) {
        return s_affinityGroupJoinDao.newAffinityGroupResponse(group);
    }

    public static AffinityGroupResponse fillAffinityGroupDetails(AffinityGroupResponse resp, AffinityGroupJoinVO group) {
        return s_affinityGroupJoinDao.setAffinityGroupResponse(resp, group);
    }


    public static List<? extends LoadBalancer> listSiteLoadBalancers(long gslbRuleId) {
        return s_gslbService.listSiteLoadBalancers(gslbRuleId);
    }

    public static String getDnsNameConfiguredForGslb() {
        String providerDnsName = s_configDao.getValue(Config.CloudDnsName.key());
        return providerDnsName;
    }

    public static Map<String, String> getResourceDetails(long resourceId, ResourceObjectType resourceType) {
        Map<String, String> details = null;
        if (isAdmin(CallContext.current().getCallingAccount())) {
            details = s_resourceDetailsService.getDetailsMap(resourceId, resourceType, null);
        } else {
            details = s_resourceDetailsService.getDetailsMap(resourceId, resourceType, true);
        }
        return details.isEmpty() ? null : details;
    }

    public static boolean isAdmin(Account account) {
        return s_accountService.isAdmin(account.getId());
    }

    public static List<ResourceTagJoinVO> listResourceTagViewByResourceUUID(String resourceUUID, ResourceObjectType resourceType) {
        return s_tagJoinDao.listBy(resourceUUID, resourceType);
    }

    public static BackupResponse newBackupResponse(Backup backup) {
        return s_backupDao.newBackupResponse(backup);
    }

    public static BackupScheduleResponse newBackupScheduleResponse(BackupSchedule schedule) {
        return s_backupScheduleDao.newBackupScheduleResponse(schedule);
    }

    public static BackupOfferingResponse newBackupOfferingResponse(BackupOffering policy) {
        return s_backupOfferingDao.newBackupOfferingResponse(policy);
    }

    public static NicVO findByIp4AddressAndNetworkId(String ip4Address, long networkId) {
        return s_nicDao.findByIp4AddressAndNetworkId(ip4Address, networkId);
    }

    public static NicSecondaryIpVO findSecondaryIpByIp4AddressAndNetworkId(String ip4Address, long networkId) {
        return s_nicSecondaryIpDao.findByIp4AddressAndNetworkId(ip4Address, networkId);
    }
}
