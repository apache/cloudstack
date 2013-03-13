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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants.HostDetails;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.EventResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.apache.cloudstack.api.response.ProjectAccountResponse;
import org.apache.cloudstack.api.response.ProjectInvitationResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ResourceTagResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.springframework.stereotype.Component;

import com.cloud.api.query.dao.AccountJoinDao;
import com.cloud.api.query.dao.AsyncJobJoinDao;
import com.cloud.api.query.dao.DataCenterJoinDao;
import com.cloud.api.query.dao.DiskOfferingJoinDao;
import com.cloud.api.query.dao.DomainRouterJoinDao;
import com.cloud.api.query.dao.HostJoinDao;
import com.cloud.api.query.dao.InstanceGroupJoinDao;
import com.cloud.api.query.dao.ProjectAccountJoinDao;
import com.cloud.api.query.dao.ProjectInvitationJoinDao;
import com.cloud.api.query.dao.ProjectJoinDao;
import com.cloud.api.query.dao.ResourceTagJoinDao;
import com.cloud.api.query.dao.SecurityGroupJoinDao;
import com.cloud.api.query.dao.ServiceOfferingJoinDao;
import com.cloud.api.query.dao.StoragePoolJoinDao;
import com.cloud.api.query.dao.UserAccountJoinDao;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.dao.VolumeJoinDao;
import com.cloud.api.query.vo.AccountJoinVO;
import com.cloud.api.query.vo.AsyncJobJoinVO;
import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.api.query.vo.DiskOfferingJoinVO;
import com.cloud.api.query.vo.DomainRouterJoinVO;
import com.cloud.api.query.vo.EventJoinVO;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.api.query.vo.InstanceGroupJoinVO;
import com.cloud.api.query.vo.ProjectAccountJoinVO;
import com.cloud.api.query.vo.ProjectInvitationJoinVO;
import com.cloud.api.query.vo.ProjectJoinVO;
import com.cloud.api.query.vo.ResourceTagJoinVO;
import com.cloud.api.query.vo.SecurityGroupJoinVO;
import com.cloud.api.query.vo.ServiceOfferingJoinVO;
import com.cloud.api.query.vo.StoragePoolJoinVO;
import com.cloud.api.query.vo.UserAccountJoinVO;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.api.query.vo.VolumeJoinVO;
import com.cloud.async.AsyncJob;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.dao.AsyncJobDao;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.capacity.dao.CapacityDaoImpl.SummedCapacity;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationService;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.*;
import com.cloud.dc.dao.*;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.Event;
import com.cloud.event.dao.EventJoinDao;
import com.cloud.exception.InvalidParameterValueException;
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
import com.cloud.network.NetworkManager;
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
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesDao;
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
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.network.vpc.*;
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
import com.cloud.resource.ResourceManager;
import com.cloud.server.*;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.*;
import com.cloud.storage.Storage.ImageFormat;

import com.cloud.storage.Volume.Type;
import com.cloud.storage.dao.*;
import com.cloud.storage.snapshot.SnapshotPolicy;
import com.cloud.template.TemplateManager;
import com.cloud.user.*;

import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.InstanceGroup;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicSecondaryIp;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmStats;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.NicSecondaryIpVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

@Component
public class ApiDBUtils {
    private static ManagementServer _ms;
    static AsyncJobManager _asyncMgr;
    static SecurityGroupManager _securityGroupMgr;
    static StorageManager _storageMgr;
    static VolumeManager _volumeMgr;
    static UserVmManager _userVmMgr;
    static NetworkModel _networkModel;
    static NetworkManager _networkMgr;
    static TemplateManager _templateMgr;
    
    static StatsCollector _statsCollector;

    static AccountDao _accountDao;
    static AccountVlanMapDao _accountVlanMapDao;
    static ClusterDao _clusterDao;
    static CapacityDao _capacityDao;
    static DiskOfferingDao _diskOfferingDao;
    static DiskOfferingJoinDao _diskOfferingJoinDao;
    static DataCenterJoinDao _dcJoinDao;
    static DomainDao _domainDao;
    static DomainRouterDao _domainRouterDao;
    static DomainRouterJoinDao _domainRouterJoinDao;
    static GuestOSDao _guestOSDao;
    static GuestOSCategoryDao _guestOSCategoryDao;
    static HostDao _hostDao;
    static IPAddressDao _ipAddressDao;
    static LoadBalancerDao _loadBalancerDao;
    static SecurityGroupDao _securityGroupDao;
    static SecurityGroupJoinDao _securityGroupJoinDao;
    static ServiceOfferingJoinDao _serviceOfferingJoinDao;
    static NetworkRuleConfigDao _networkRuleConfigDao;
    static HostPodDao _podDao;
    static ServiceOfferingDao _serviceOfferingDao;
    static SnapshotDao _snapshotDao;
    static PrimaryDataStoreDao _storagePoolDao;
    static VMTemplateDao _templateDao;
    static VMTemplateDetailsDao _templateDetailsDao;
    static VMTemplateHostDao _templateHostDao;
    static VMTemplateSwiftDao _templateSwiftDao;
    static VMTemplateS3Dao _templateS3Dao;
    static UploadDao _uploadDao;
    static UserDao _userDao;
    static UserStatisticsDao _userStatsDao;
    static UserVmDao _userVmDao;
    static UserVmJoinDao _userVmJoinDao;
    static VlanDao _vlanDao;
    static VolumeDao _volumeDao;
    static Site2SiteVpnGatewayDao _site2SiteVpnGatewayDao;
    static Site2SiteCustomerGatewayDao _site2SiteCustomerGatewayDao;
    static VolumeHostDao _volumeHostDao;
    static DataCenterDao _zoneDao;
    static NetworkOfferingDao _networkOfferingDao;
    static NetworkDao _networkDao;
    static PhysicalNetworkDao _physicalNetworkDao;
    static ConfigurationService _configMgr;
    static ConfigurationDao _configDao;
    static ConsoleProxyDao _consoleProxyDao;
    static FirewallRulesCidrsDao _firewallCidrsDao;
    static VMInstanceDao _vmDao;
    static ResourceLimitService _resourceLimitMgr;
    static ProjectService _projectMgr;
    static ResourceManager _resourceMgr;
    static AccountDetailsDao _accountDetailsDao;
    static NetworkDomainDao _networkDomainDao;
    static HighAvailabilityManager _haMgr;
    static VpcManager _vpcMgr;
    static TaggedResourceService _taggedResourceService;
    static UserVmDetailsDao _userVmDetailsDao;
    static SSHKeyPairDao _sshKeyPairDao;

    static ConditionDao _asConditionDao;
    static AutoScalePolicyConditionMapDao _asPolicyConditionMapDao;
    static AutoScaleVmGroupPolicyMapDao _asVmGroupPolicyMapDao;
    static AutoScalePolicyDao _asPolicyDao;
    static AutoScaleVmProfileDao _asVmProfileDao;
    static AutoScaleVmGroupDao _asVmGroupDao;
    static CounterDao _counterDao;
    static ResourceTagJoinDao _tagJoinDao;
    static EventJoinDao _eventJoinDao;
    static InstanceGroupJoinDao _vmGroupJoinDao;
    static UserAccountJoinDao _userAccountJoinDao;
    static ProjectJoinDao _projectJoinDao;
    static ProjectAccountJoinDao _projectAccountJoinDao;
    static ProjectInvitationJoinDao _projectInvitationJoinDao;
    static HostJoinDao _hostJoinDao;
    static VolumeJoinDao _volJoinDao;
    static StoragePoolJoinDao _poolJoinDao;
    static AccountJoinDao _accountJoinDao;
    static AsyncJobJoinDao _jobJoinDao;

    static PhysicalNetworkTrafficTypeDao _physicalNetworkTrafficTypeDao;
    static PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    static FirewallRulesDao _firewallRuleDao;
    static StaticRouteDao _staticRouteDao;
    static VpcGatewayDao _vpcGatewayDao;
    static VpcDao _vpcDao;
    static VpcOfferingDao _vpcOfferingDao;
    static SnapshotPolicyDao _snapshotPolicyDao;
    static AsyncJobDao _asyncJobDao;
    static HostDetailsDao _hostDetailsDao;
    static VMSnapshotDao _vmSnapshotDao;
    static ClusterDetailsDao _clusterDetailsDao;
    static NicSecondaryIpDao _nicSecondaryIpDao;

    @Inject private ManagementServer ms;
    @Inject public AsyncJobManager asyncMgr;
    @Inject private SecurityGroupManager securityGroupMgr;
    @Inject private StorageManager storageMgr;
    @Inject private UserVmManager userVmMgr;
    @Inject private NetworkModel networkModel;
    @Inject private NetworkManager networkMgr;
    @Inject private StatsCollector statsCollector;
    @Inject private TemplateManager templateMgr;
    @Inject private VolumeManager volumeMgr;

    @Inject private AccountDao accountDao;
    @Inject private AccountVlanMapDao accountVlanMapDao;
    @Inject private ClusterDao clusterDao;
    @Inject private CapacityDao capacityDao;
    @Inject private DataCenterJoinDao dcJoinDao;
    @Inject private DiskOfferingDao diskOfferingDao;
    @Inject private DiskOfferingJoinDao diskOfferingJoinDao;
    @Inject private DomainDao domainDao;
    @Inject private DomainRouterDao domainRouterDao;
    @Inject private DomainRouterJoinDao domainRouterJoinDao;
    @Inject private GuestOSDao guestOSDao;
    @Inject private GuestOSCategoryDao guestOSCategoryDao;
    @Inject private HostDao hostDao;
    @Inject private IPAddressDao ipAddressDao;
    @Inject private LoadBalancerDao loadBalancerDao;
    @Inject private SecurityGroupDao securityGroupDao;
    @Inject private SecurityGroupJoinDao securityGroupJoinDao;
    @Inject private ServiceOfferingJoinDao serviceOfferingJoinDao;
    @Inject private NetworkRuleConfigDao networkRuleConfigDao;
    @Inject private HostPodDao podDao;
    @Inject private ServiceOfferingDao serviceOfferingDao;
    @Inject private SnapshotDao snapshotDao;
    @Inject private PrimaryDataStoreDao storagePoolDao;
    @Inject private VMTemplateDao templateDao;
    @Inject private VMTemplateDetailsDao templateDetailsDao;
    @Inject private VMTemplateHostDao templateHostDao;
    @Inject private VMTemplateSwiftDao templateSwiftDao;
    @Inject private VMTemplateS3Dao templateS3Dao;
    @Inject private UploadDao uploadDao;
    @Inject private UserDao userDao;
    @Inject private UserStatisticsDao userStatsDao;
    @Inject private UserVmDao userVmDao;
    @Inject private UserVmJoinDao userVmJoinDao;
    @Inject private VlanDao vlanDao;
    @Inject private VolumeDao volumeDao;
    @Inject private Site2SiteVpnGatewayDao site2SiteVpnGatewayDao;
    @Inject private Site2SiteCustomerGatewayDao site2SiteCustomerGatewayDao;
    @Inject private VolumeHostDao volumeHostDao;
    @Inject private DataCenterDao zoneDao;
    @Inject private NetworkOfferingDao networkOfferingDao;
    @Inject private NetworkDao networkDao;
    @Inject private PhysicalNetworkDao physicalNetworkDao;
    @Inject private ConfigurationService configMgr;
    @Inject private ConfigurationDao configDao;
    @Inject private ConsoleProxyDao consoleProxyDao;
    @Inject private FirewallRulesCidrsDao firewallCidrsDao;
    @Inject private VMInstanceDao vmDao;
    @Inject private ResourceLimitService resourceLimitMgr;
    @Inject private ProjectService projectMgr;
    @Inject private ResourceManager resourceMgr;
    @Inject private AccountDetailsDao accountDetailsDao;
    @Inject private NetworkDomainDao networkDomainDao;
    @Inject private HighAvailabilityManager haMgr;
    @Inject private VpcManager vpcMgr;
    @Inject private TaggedResourceService taggedResourceService;
    @Inject private UserVmDetailsDao userVmDetailsDao;
    @Inject private SSHKeyPairDao sshKeyPairDao;

    @Inject private ConditionDao asConditionDao;
    @Inject private AutoScalePolicyConditionMapDao asPolicyConditionMapDao;
    @Inject private AutoScaleVmGroupPolicyMapDao asVmGroupPolicyMapDao;
    @Inject private AutoScalePolicyDao asPolicyDao;
    @Inject private AutoScaleVmProfileDao asVmProfileDao;
    @Inject private AutoScaleVmGroupDao asVmGroupDao;
    @Inject private CounterDao counterDao;
    @Inject private ResourceTagJoinDao tagJoinDao;
    @Inject private EventJoinDao eventJoinDao;
    @Inject private InstanceGroupJoinDao vmGroupJoinDao;
    @Inject private UserAccountJoinDao userAccountJoinDao;
    @Inject private ProjectJoinDao projectJoinDao;
    @Inject private ProjectAccountJoinDao projectAccountJoinDao;
    @Inject private ProjectInvitationJoinDao projectInvitationJoinDao;
    @Inject private HostJoinDao hostJoinDao;
    @Inject private VolumeJoinDao volJoinDao;
    @Inject private StoragePoolJoinDao poolJoinDao;
    @Inject private AccountJoinDao accountJoinDao;
    @Inject private AsyncJobJoinDao jobJoinDao;

    @Inject private PhysicalNetworkTrafficTypeDao physicalNetworkTrafficTypeDao;
    @Inject private PhysicalNetworkServiceProviderDao physicalNetworkServiceProviderDao;
    @Inject private FirewallRulesDao firewallRuleDao;
    @Inject private StaticRouteDao staticRouteDao;
    @Inject private VpcGatewayDao vpcGatewayDao;
    @Inject private VpcDao vpcDao;
    @Inject private VpcOfferingDao vpcOfferingDao;
    @Inject private SnapshotPolicyDao snapshotPolicyDao;
    @Inject private AsyncJobDao asyncJobDao;
    @Inject private HostDetailsDao hostDetailsDao;
    @Inject private ClusterDetailsDao clusterDetailsDao;
    @Inject private VMSnapshotDao vmSnapshotDao;
    @Inject private NicSecondaryIpDao nicSecondaryIpDao;
    @PostConstruct
    void init() {
        _ms = ms;
        _asyncMgr = asyncMgr;
        _securityGroupMgr = securityGroupMgr;
        _storageMgr = storageMgr;
        _userVmMgr = userVmMgr;
        _networkModel = networkModel;
        _networkMgr = networkMgr;
        _configMgr = configMgr;
        _templateMgr = templateMgr;

        _accountDao = accountDao;
        _accountVlanMapDao = accountVlanMapDao;
        _clusterDao = clusterDao;
        _capacityDao = capacityDao;
        _dcJoinDao = dcJoinDao;
        _diskOfferingDao = diskOfferingDao;
        _diskOfferingJoinDao = diskOfferingJoinDao;
        _domainDao = domainDao;
        _domainRouterDao = domainRouterDao;
        _domainRouterJoinDao = domainRouterJoinDao;
        _guestOSDao = guestOSDao;
        _guestOSCategoryDao = guestOSCategoryDao;
        _hostDao = hostDao;
        _ipAddressDao = ipAddressDao;
        _loadBalancerDao = loadBalancerDao;
        _networkRuleConfigDao = networkRuleConfigDao;
        _podDao = podDao;
        _serviceOfferingDao = serviceOfferingDao;
        _serviceOfferingJoinDao = serviceOfferingJoinDao;
        _snapshotDao = snapshotDao;
        _storagePoolDao = storagePoolDao;
        _templateDao = templateDao;
        _templateDetailsDao = templateDetailsDao;
        _templateHostDao = templateHostDao;
        _templateSwiftDao = templateSwiftDao;
        _templateS3Dao = templateS3Dao;
        _uploadDao = uploadDao;
        _userDao = userDao;
        _userStatsDao = userStatsDao;
        _userVmDao = userVmDao;
        _userVmJoinDao = userVmJoinDao;
        _vlanDao = vlanDao;
        _volumeDao = volumeDao;
        _site2SiteVpnGatewayDao = site2SiteVpnGatewayDao;
        _site2SiteCustomerGatewayDao = site2SiteCustomerGatewayDao;
        _volumeHostDao = volumeHostDao;
        _zoneDao = zoneDao;
        _securityGroupDao = securityGroupDao;
        _securityGroupJoinDao = securityGroupJoinDao;
        _networkOfferingDao = networkOfferingDao;
        _networkDao = networkDao;
        _physicalNetworkDao = physicalNetworkDao;
        _configDao = configDao;
        _consoleProxyDao = consoleProxyDao;
        _firewallCidrsDao = firewallCidrsDao;
        _vmDao = vmDao;
        _resourceLimitMgr = resourceLimitMgr;
        _projectMgr = projectMgr;
        _resourceMgr = resourceMgr;
        _accountDetailsDao = accountDetailsDao;
        _networkDomainDao = networkDomainDao;
        _haMgr = haMgr;
        _vpcMgr = vpcMgr;
        _taggedResourceService = taggedResourceService;
        _sshKeyPairDao = sshKeyPairDao;
        _userVmDetailsDao = userVmDetailsDao;
        _asConditionDao = asConditionDao;
        _asPolicyDao = asPolicyDao;
        _asPolicyConditionMapDao = asPolicyConditionMapDao;
        _counterDao = counterDao;
        _asVmGroupPolicyMapDao = asVmGroupPolicyMapDao;
        _tagJoinDao = tagJoinDao;
        _vmGroupJoinDao = vmGroupJoinDao;
        _eventJoinDao = eventJoinDao;
        _userAccountJoinDao = userAccountJoinDao;
        _projectJoinDao = projectJoinDao;
        _projectAccountJoinDao = projectAccountJoinDao;
        _projectInvitationJoinDao = projectInvitationJoinDao;
        _hostJoinDao = hostJoinDao;
        _volJoinDao = volJoinDao;
        _poolJoinDao = poolJoinDao;
        _accountJoinDao = accountJoinDao;
        _jobJoinDao = jobJoinDao;

        _physicalNetworkTrafficTypeDao = physicalNetworkTrafficTypeDao;
        _physicalNetworkServiceProviderDao = physicalNetworkServiceProviderDao;
        _firewallRuleDao = firewallRuleDao;
        _staticRouteDao = staticRouteDao;
        _vpcGatewayDao = vpcGatewayDao;
        _asVmProfileDao = asVmProfileDao;
        _asVmGroupDao = asVmGroupDao;
        _vpcDao = vpcDao;
        _vpcOfferingDao = vpcOfferingDao;
        _snapshotPolicyDao = snapshotPolicyDao;
        _asyncJobDao = asyncJobDao;
        _hostDetailsDao = hostDetailsDao;
        _clusterDetailsDao = clusterDetailsDao;
        _vmSnapshotDao = vmSnapshotDao;
        _nicSecondaryIpDao = nicSecondaryIpDao;
        // Note: stats collector should already have been initialized by this time, otherwise a null instance is returned
        _statsCollector = StatsCollector.getInstance();
    }

    // ///////////////////////////////////////////////////////////
    // ManagementServer methods //
    // ///////////////////////////////////////////////////////////

    public static VMInstanceVO findVMInstanceById(long vmId) {
        return _vmDao.findById(vmId);
    }

    public static long getMemoryOrCpuCapacitybyHost(Long hostId, short capacityType) {
        // TODO: This method is for the API only, but it has configuration values (ramSize for system vms)
        // so if this Utils class can have some kind of config rather than a static initializer (maybe from
        // management server instantiation?) then maybe the management server method can be moved entirely
        // into this utils class.
        return _ms.getMemoryOrCpuCapacityByHost(hostId,capacityType);
    }

    public static long getStorageCapacitybyPool(Long poolId, short capacityType) {
        // TODO: This method is for the API only, but it has configuration values (ramSize for system vms)
        // so if this Utils class can have some kind of config rather than a static initializer (maybe from
        // management server instantiation?) then maybe the management server method can be moved entirely
        // into this utils class.
        return _ms.getMemoryOrCpuCapacityByHost(poolId, capacityType);
    }

    public static List<SummedCapacity> getCapacityByClusterPodZone(Long zoneId, Long podId, Long clusterId){
        return _capacityDao.findByClusterPodZone(zoneId,podId,clusterId);
    }

    public static List<SummedCapacity> findNonSharedStorageForClusterPodZone(Long zoneId, Long podId, Long clusterId){
        return _capacityDao.findNonSharedStorageForClusterPodZone(zoneId,podId,clusterId);
    }

    public static List<CapacityVO> getCapacityByPod(){
        return null;

    }

    public static Long getPodIdForVlan(long vlanDbId) {
        return _networkModel.getPodIdForVlan(vlanDbId);
    }

    public static String getVersion() {
        return _ms.getVersion();
    }

    public static List<UserVmJoinVO> searchForUserVMs(Criteria c, List<Long> permittedAccounts) {
        return _userVmMgr.searchForUserVMs(c, _accountDao.findById(Account.ACCOUNT_ID_SYSTEM),
                null, false, permittedAccounts, false, null, null).first();
    }

    public static List<? extends StoragePoolVO> searchForStoragePools(Criteria c) {
        return _ms.searchForStoragePools(c).first();
    }

    // ///////////////////////////////////////////////////////////
    // Manager methods //
    // ///////////////////////////////////////////////////////////

    public static long findCorrectResourceLimit(ResourceType type, long accountId) {
        AccountVO account = _accountDao.findById(accountId);

        if (account == null) {
            return -1;
        }

        return _resourceLimitMgr.findCorrectResourceLimitForAccount(account, type);
    }

    public static long findCorrectResourceLimit(Long limit, short accountType, ResourceType type) {
        return _resourceLimitMgr.findCorrectResourceLimitForAccount(accountType, limit, type);
    }

    public static AsyncJobVO findInstancePendingAsyncJob(String instanceType, long instanceId) {
        return _asyncMgr.findInstancePendingAsyncJob(instanceType, instanceId);
    }

    public static long getResourceCount(ResourceType type, long accountId) {
        AccountVO account = _accountDao.findById(accountId);

        if (account == null) {
            return -1;
        }

        return _resourceLimitMgr.getResourceCount(account, type);
    }

    public static String getSecurityGroupsNamesForVm(long vmId) {
        return _securityGroupMgr.getSecurityGroupsNamesForVm(vmId);
    }

    public static List<SecurityGroupVO> getSecurityGroupsForVm(long vmId) {
        return _securityGroupMgr.getSecurityGroupsForVm(vmId);
    }

    public static String getSnapshotIntervalTypes(long snapshotId) {
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
        return snapshot.getRecurringType().name();
    }

    public static String getStoragePoolTags(long poolId) {
        return _storageMgr.getStoragePoolTags(poolId);
    }

    public static boolean isLocalStorageActiveOnHost(Long hostId) {
        return _storageMgr.isLocalStorageActiveOnHost(hostId);
    }

    public static InstanceGroupVO findInstanceGroupForVM(long vmId) {
        return _userVmMgr.getGroupForVm(vmId);
    }

    // ///////////////////////////////////////////////////////////
    // Misc methods //
    // ///////////////////////////////////////////////////////////

    public static HostStats getHostStatistics(long hostId) {
        return _statsCollector.getHostStats(hostId);
    }

    public static StorageStats getStoragePoolStatistics(long id) {
        return _statsCollector.getStoragePoolStats(id);
    }

    public static VmStats getVmStatistics(long hostId) {
        return _statsCollector.getVmStats(hostId);
    }

    public static StorageStats getSecondaryStorageStatistics(long id) {
        return _statsCollector.getStorageStats(id);
    }

    public static CapacityVO getStoragePoolUsedStats(Long poolId, Long clusterId, Long podId, Long zoneId){
        return _storageMgr.getStoragePoolUsedStats(poolId, clusterId, podId, zoneId);
    }

    public static CapacityVO getSecondaryStorageUsedStats(Long hostId, Long zoneId){
        return _storageMgr.getSecondaryStorageUsedStats(hostId, zoneId);
    }

    // ///////////////////////////////////////////////////////////
    // Dao methods //
    // ///////////////////////////////////////////////////////////

    public static Account findAccountById(Long accountId) {
        return _accountDao.findByIdIncludingRemoved(accountId);
    }

    public static Account findAccountByIdIncludingRemoved(Long accountId) {
        return _accountDao.findByIdIncludingRemoved(accountId);
    }

    public static Account findAccountByNameDomain(String accountName, Long domainId) {
        return _accountDao.findActiveAccount(accountName, domainId);
    }

    public static ClusterVO findClusterById(long clusterId) {
        return _clusterDao.findById(clusterId);
    }

    public static ClusterDetailsVO findClusterDetails(long clusterId, String name){
         return _clusterDetailsDao.findDetail(clusterId,name);
    }

    public static DiskOfferingVO findDiskOfferingById(Long diskOfferingId) {
        return _diskOfferingDao.findByIdIncludingRemoved(diskOfferingId);
    }

    public static DomainVO findDomainById(Long domainId) {
        return _domainDao.findByIdIncludingRemoved(domainId);
    }

    public static DomainVO findDomainByIdIncludingRemoved(Long domainId) {
        return _domainDao.findByIdIncludingRemoved(domainId);
    }

    public static boolean isChildDomain(long parentId, long childId) {
        return _domainDao.isChildDomain(parentId, childId);
    }

    public static DomainRouterVO findDomainRouterById(Long routerId) {
        return _domainRouterDao.findByIdIncludingRemoved(routerId);
    }

    public static GuestOS findGuestOSById(Long id) {
        return _guestOSDao.findByIdIncludingRemoved(id);
    }

    public static GuestOS findGuestOSByDisplayName(String displayName) {
        return _guestOSDao.listByDisplayName(displayName);
    }

    public static HostVO findHostById(Long hostId) {
        return _hostDao.findByIdIncludingRemoved(hostId);
    }

    public static IPAddressVO findIpAddressById(long addressId) {
        return _ipAddressDao.findById(addressId);
    }

    public static GuestOSCategoryVO getHostGuestOSCategory(long hostId) {
        Long guestOSCategoryID = _resourceMgr.getGuestOSCategoryId(hostId);

        if (guestOSCategoryID != null) {
            return _guestOSCategoryDao.findById(guestOSCategoryID);
        } else {
            return null;
        }
    }

    public static String getHostTags(long hostId) {
        return _resourceMgr.getHostTags(hostId);
    }

    public static LoadBalancerVO findLoadBalancerById(Long loadBalancerId) {
        return _loadBalancerDao.findById(loadBalancerId);
    }

    public static NetworkRuleConfigVO findNetworkRuleById(Long ruleId) {
        return _networkRuleConfigDao.findById(ruleId);
    }

    public static SecurityGroup findSecurityGroupById(Long groupId) {
        return _securityGroupDao.findById(groupId);
    }

    public static HostPodVO findPodById(Long podId) {
        return _podDao.findById(podId);
    }

    public static VolumeVO findRootVolume(long vmId) {
        List<VolumeVO> volumes = _volumeDao.findByInstanceAndType(vmId, Type.ROOT);
        if (volumes != null && volumes.size() == 1) {
            return volumes.get(0);
        } else {
            return null;
        }
    }

    public static ServiceOffering findServiceOfferingById(Long serviceOfferingId) {
        return _serviceOfferingDao.findByIdIncludingRemoved(serviceOfferingId);
    }

    public static Snapshot findSnapshotById(long snapshotId) {
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
        if (snapshot != null && snapshot.getRemoved() == null && snapshot.getState() == Snapshot.State.BackedUp) {
            return snapshot;
        } else {
            return null;
        }
    }

    public static StoragePoolVO findStoragePoolById(Long storagePoolId) {
        return _storagePoolDao.findByIdIncludingRemoved(storagePoolId);
    }

    public static VMTemplateVO findTemplateById(Long templateId) {
        VMTemplateVO template = _templateDao.findByIdIncludingRemoved(templateId);
        if(template != null) {
            Map details = _templateDetailsDao.findDetails(templateId);
            if(details != null && !details.isEmpty())
                template.setDetails(details);
        }
        return template;
    }

    public static VMTemplateHostVO findTemplateHostRef(long templateId, long zoneId) {
        return findTemplateHostRef(templateId, zoneId, false);
    }

    public static VMTemplateHostVO findTemplateHostRef(long templateId, long zoneId, boolean readyOnly) {
        VMTemplateVO vmTemplate = findTemplateById(templateId);
        if (vmTemplate.getHypervisorType() == HypervisorType.BareMetal) {
            List<VMTemplateHostVO> res = _templateHostDao.listByTemplateId(templateId);
            return res.size() == 0 ? null : res.get(0);
        } else {
            return _templateMgr.getTemplateHostRef(zoneId, templateId, readyOnly);
        }
    }


    public static VolumeHostVO findVolumeHostRef(long volumeId, long zoneId) {
        return _volumeHostDao.findVolumeByZone(volumeId, zoneId);
    }

    public static VMTemplateSwiftVO findTemplateSwiftRef(long templateId) {
        return _templateSwiftDao.findOneByTemplateId(templateId);
    }

    public static VMTemplateS3VO findTemplateS3Ref(long templateId) {
        return _templateS3Dao.findOneByTemplateId(templateId);
    }

    public static UploadVO findUploadById(Long id) {
        return _uploadDao.findById(id);
    }

    public static User findUserById(Long userId) {
        return _userDao.findById(userId);
    }

    public static UserVm findUserVmById(Long vmId) {
        return _userVmDao.findById(vmId);
    }

    public static VlanVO findVlanById(long vlanDbId) {
        return _vlanDao.findById(vlanDbId);
    }

    public static VolumeVO findVolumeById(Long volumeId) {
        return _volumeDao.findByIdIncludingRemoved(volumeId);
    }

    public static Site2SiteVpnGatewayVO findVpnGatewayById(Long vpnGatewayId) {
        return _site2SiteVpnGatewayDao.findById(vpnGatewayId);
    }

    public static Site2SiteCustomerGatewayVO findCustomerGatewayById(Long customerGatewayId) {
        return _site2SiteCustomerGatewayDao.findById(customerGatewayId);
    }

    public static List<UserVO> listUsersByAccount(long accountId) {
        return _userDao.listByAccount(accountId);
    }

    public static DataCenterVO findZoneById(Long zoneId) {
        return _zoneDao.findById(zoneId);
    }

    public static Long getAccountIdForVlan(long vlanDbId) {
        List<AccountVlanMapVO> accountVlanMaps = _accountVlanMapDao.listAccountVlanMapsByVlan(vlanDbId);
        if (accountVlanMaps.isEmpty()) {
            return null;
        } else {
            return accountVlanMaps.get(0).getAccountId();
        }
    }

    public static HypervisorType getVolumeHyperType(long volumeId) {
        return _volumeDao.getHypervisorType(volumeId);
    }

    public static HypervisorType getHypervisorTypeFromFormat(ImageFormat format){
        return _storageMgr.getHypervisorTypeFromFormat(format);
    }

    public static List<VMTemplateHostVO> listTemplateHostBy(long templateId, Long zoneId, boolean readyOnly) {
        if (zoneId != null) {
            VMTemplateVO vmTemplate = findTemplateById(templateId);
            if (vmTemplate.getHypervisorType() == HypervisorType.BareMetal) {
                return _templateHostDao.listByTemplateId(templateId);
            } else {
                return _templateHostDao.listByZoneTemplate(zoneId, templateId, readyOnly);
            }
        } else {
            return _templateHostDao.listByOnlyTemplateId(templateId);
        }
    }

    public static List<UserStatisticsVO> listUserStatsBy(Long accountId) {
        return _userStatsDao.listBy(accountId);
    }

    public static List<UserVmVO> listUserVMsByHostId(long hostId) {
        return _userVmDao.listByHostId(hostId);
    }

    public static List<DataCenterVO> listZones() {
        return _zoneDao.listAll();
    }

    public static boolean volumeIsOnSharedStorage(long volumeId) {
        // Check that the volume is valid
        VolumeVO volume = _volumeDao.findById(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Please specify a valid volume ID.");
        }

        return _volumeMgr.volumeOnSharedStoragePool(volume);
    }

    public static List<NicProfile> getNics(VirtualMachine vm) {
        return _networkMgr.getNicProfiles(vm);
    }

    public static NetworkProfile getNetworkProfile(long networkId) {
        return _networkMgr.convertNetworkToNetworkProfile(networkId);
    }

    public static NetworkOfferingVO findNetworkOfferingById(long networkOfferingId) {
        return _networkOfferingDao.findByIdIncludingRemoved(networkOfferingId);
    }

    public static List<? extends Vlan> listVlanByNetworkId(long networkId) {
        return _vlanDao.listVlansByNetworkId(networkId);
    }

    public static PhysicalNetworkVO findPhysicalNetworkById(long id) {
        return _physicalNetworkDao.findById(id);
    }

    public static PhysicalNetworkTrafficTypeVO findPhysicalNetworkTrafficTypeById(long id) {
        return _physicalNetworkTrafficTypeDao.findById(id);
    }

    public static NetworkVO findNetworkById(long id) {
        return _networkDao.findById(id);
    }

    public static Map<Service, Map<Capability, String>> getNetworkCapabilities(long networkId, long zoneId) {
        return _networkModel.getNetworkCapabilities(networkId);
    }

    public static long getPublicNetworkIdByZone(long zoneId) {
        return _networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, TrafficType.Public).getId();
    }

    public static Long getVlanNetworkId(long vlanId) {
        VlanVO vlan = _vlanDao.findById(vlanId);
        if (vlan != null) {
            return vlan.getNetworkId();
        } else {
            return null;
        }
    }

    public static Integer getNetworkRate(long networkOfferingId) {
        return _configMgr.getNetworkOfferingNetworkRate(networkOfferingId);
    }

    public static Account getVlanAccount(long vlanId) {
        return _configMgr.getVlanAccount(vlanId);
    }

    public static boolean isSecurityGroupEnabledInZone(long zoneId) {
        DataCenterVO dc = _zoneDao.findById(zoneId);
        if (dc == null) {
            return false;
        } else {
            return dc.isSecurityGroupEnabled();
        }
    }

    public static Long getDedicatedNetworkDomain(long networkId) {
        return _networkModel.getDedicatedNetworkDomain(networkId);
    }

    public static float getCpuOverprovisioningFactor() {
        String opFactor = _configDao.getValue(Config.CPUOverprovisioningFactor.key());
        float cpuOverprovisioningFactor = NumbersUtil.parseFloat(opFactor, 1);
        return cpuOverprovisioningFactor;
    }

    public static boolean isExtractionDisabled(){
        String disableExtractionString = _configDao.getValue(Config.DisableExtraction.toString());
        boolean disableExtraction  = (disableExtractionString == null) ? false : Boolean.parseBoolean(disableExtractionString);
        return disableExtraction;
    }

    public static SecurityGroup getSecurityGroup(String groupName, long ownerId) {
        return _securityGroupMgr.getSecurityGroup(groupName, ownerId);
    }

    public static ConsoleProxyVO findConsoleProxy(long id) {
        return _consoleProxyDao.findById(id);
    }

    public static List<String> findFirewallSourceCidrs(long id){
        return _firewallCidrsDao.getSourceCidrs(id);
    }

    public static Account getProjectOwner(long projectId) {
        return _projectMgr.getProjectOwner(projectId);
    }

    public static Project findProjectByProjectAccountId(long projectAccountId) {
        return _projectMgr.findByProjectAccountId(projectAccountId);
    }

    public static Project findProjectByProjectAccountIdIncludingRemoved(long projectAccountId) {
        return _projectMgr.findByProjectAccountIdIncludingRemoved(projectAccountId);
    }

    public static Project findProjectById(long projectId) {
        return _projectMgr.getProject(projectId);
    }

    public static long getProjectOwnwerId(long projectId) {
        return _projectMgr.getProjectOwner(projectId).getId();
    }

    public static Map<String, String> getAccountDetails(long accountId) {
        Map<String, String> details = _accountDetailsDao.findDetails(accountId);
        return details.isEmpty() ? null : details;
    }

    public static Map<Service, Set<Provider>> listNetworkOfferingServices(long networkOfferingId) {
        return _networkModel.getNetworkOfferingServiceProvidersMap(networkOfferingId);
    }

    public static List<Service> getElementServices(Provider provider) {
        return _networkModel.getElementServices(provider);
    }

    public static List<? extends Provider> getProvidersForService(Service service) {
        return _networkModel.listSupportedNetworkServiceProviders(service.getName());
    }

    public static boolean canElementEnableIndividualServices(Provider serviceProvider) {
        return _networkModel.canElementEnableIndividualServices(serviceProvider);
    }

    public static Pair<Long, Boolean> getDomainNetworkDetails(long networkId) {
        NetworkDomainVO map = _networkDomainDao.getDomainNetworkMapByNetworkId(networkId);

        boolean subdomainAccess = (map.isSubdomainAccess() != null) ? map.isSubdomainAccess() : _networkModel.getAllowSubdomainAccessGlobal();

        return new Pair<Long, Boolean>(map.getDomainId(), subdomainAccess);
    }

    public static long countFreePublicIps() {
        return _ipAddressDao.countFreePublicIPs();
    }

    public static long findDefaultRouterServiceOffering() {
        ServiceOfferingVO serviceOffering = _serviceOfferingDao.findByName(ServiceOffering.routerDefaultOffUniqueName);
        return serviceOffering.getId();
    }

    public static IpAddress findIpByAssociatedVmId(long vmId) {
        return _ipAddressDao.findByAssociatedVmId(vmId);
    }

    public static String getHaTag() {
        return _haMgr.getHaTag();
    }

    public static Map<Service, Set<Provider>> listVpcOffServices(long vpcOffId) {
        return _vpcMgr.getVpcOffSvcProvidersMap(vpcOffId);
    }

    public static List<? extends Network> listVpcNetworks(long vpcId) {
        return _networkModel.listNetworksByVpc(vpcId);
    }

    public static boolean canUseForDeploy(Network network) {
        return _networkModel.canUseForDeploy(network);
    }

    public static VMSnapshot getVMSnapshotById(Long vmSnapshotId) {
        VMSnapshot vmSnapshot = _vmSnapshotDao.findById(vmSnapshotId);
        return vmSnapshot;
    }

    public static String getUuid(String resourceId, TaggedResourceType resourceType) {
        return _taggedResourceService.getUuid(resourceId, resourceType);
    }

    public static boolean isOfferingForVpc(NetworkOffering offering) {
        boolean vpcProvider = _configMgr.isOfferingForVpc(offering);
        return vpcProvider;
    }

    public static List<? extends ResourceTag> listByResourceTypeAndId(TaggedResourceType type, long resourceId) {
        return _taggedResourceService.listByResourceTypeAndId(type, resourceId);
    }
    public static List<ConditionVO> getAutoScalePolicyConditions(long policyId)
    {
        List<AutoScalePolicyConditionMapVO> vos = _asPolicyConditionMapDao.listByAll(policyId, null);
        ArrayList<ConditionVO> conditions = new ArrayList<ConditionVO>(vos.size());
        for (AutoScalePolicyConditionMapVO vo : vos) {
            conditions.add(_asConditionDao.findById(vo.getConditionId()));
        }

        return conditions;
    }

    public static void getAutoScaleVmGroupPolicyIds(long vmGroupId, List<Long> scaleUpPolicyIds, List<Long> scaleDownPolicyIds)
    {
        List<AutoScaleVmGroupPolicyMapVO> vos = _asVmGroupPolicyMapDao.listByVmGroupId(vmGroupId);
        for (AutoScaleVmGroupPolicyMapVO vo : vos) {
            AutoScalePolicy autoScalePolicy = _asPolicyDao.findById(vo.getPolicyId());
            if(autoScalePolicy.getAction().equals("scaleup"))
                scaleUpPolicyIds.add(autoScalePolicy.getId());
            else
                scaleDownPolicyIds.add(autoScalePolicy.getId());
        }
    }
    public static String getKeyPairName(String sshPublicKey) {
        SSHKeyPairVO sshKeyPair = _sshKeyPairDao.findByPublicKey(sshPublicKey);
        //key might be removed prior to this point
        if (sshKeyPair != null) {
            return sshKeyPair.getName();
        }
        return null;
    }

    public static UserVmDetailVO  findPublicKeyByVmId(long vmId) {
        return _userVmDetailsDao.findDetail(vmId, "SSH.PublicKey");
    }

    public static void getAutoScaleVmGroupPolicies(long vmGroupId, List<AutoScalePolicy> scaleUpPolicies, List<AutoScalePolicy> scaleDownPolicies)
    {
        List<AutoScaleVmGroupPolicyMapVO> vos = _asVmGroupPolicyMapDao.listByVmGroupId(vmGroupId);
        for (AutoScaleVmGroupPolicyMapVO vo : vos) {
            AutoScalePolicy autoScalePolicy = _asPolicyDao.findById(vo.getPolicyId());
            if(autoScalePolicy.getAction().equals("scaleup"))
                scaleUpPolicies.add(autoScalePolicy);
            else
                scaleDownPolicies.add(autoScalePolicy);
        }
    }

    public static CounterVO getCounter(long counterId) {
        return _counterDao.findById(counterId);
    }

    public static ConditionVO findConditionById(long conditionId){
        return _asConditionDao.findById(conditionId);
    }

    public static PhysicalNetworkServiceProviderVO findPhysicalNetworkServiceProviderById(long providerId){
        return _physicalNetworkServiceProviderDao.findById(providerId);
    }

    public static FirewallRuleVO findFirewallRuleById(long ruleId){
        return _firewallRuleDao.findById(ruleId);
    }

    public static StaticRouteVO findStaticRouteById(long routeId){
        return _staticRouteDao.findById(routeId);
    }

    public static VpcGatewayVO findVpcGatewayById(long gatewayId){
        return _vpcGatewayDao.findById(gatewayId);
    }

    public static AutoScalePolicyVO findAutoScalePolicyById(long policyId){
        return _asPolicyDao.findById(policyId);
    }

    public static AutoScaleVmProfileVO findAutoScaleVmProfileById(long profileId){
        return _asVmProfileDao.findById(profileId);
    }

    public static AutoScaleVmGroupVO findAutoScaleVmGroupById(long groupId){
        return _asVmGroupDao.findById(groupId);
    }

    public static GuestOSCategoryVO findGuestOsCategoryById(long catId){
        return _guestOSCategoryDao.findById(catId);
    }

    public static VpcVO findVpcById(long vpcId){
        return _vpcDao.findById(vpcId);
    }

    public static SnapshotPolicy findSnapshotPolicyById(long policyId){
        return _snapshotPolicyDao.findById(policyId);
    }

    public static VpcOffering findVpcOfferingById(long offeringId){
        return _vpcOfferingDao.findById(offeringId);
    }


    public static AsyncJob findAsyncJobById(long jobId){
        return _asyncJobDao.findById(jobId);
    }

    public static String findJobInstanceUuid(AsyncJob job){
        if ( job == null )
            return null;
        String jobInstanceId = null;
        if (job.getInstanceType() == AsyncJob.Type.Volume) {
            VolumeVO volume = ApiDBUtils.findVolumeById(job.getInstanceId());
            if (volume != null) {
                jobInstanceId = volume.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.Template || job.getInstanceType() == AsyncJob.Type.Iso) {
            VMTemplateVO template = ApiDBUtils.findTemplateById(job.getInstanceId());
            if (template != null) {
                jobInstanceId = template.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.VirtualMachine || job.getInstanceType() == AsyncJob.Type.ConsoleProxy
                || job.getInstanceType() == AsyncJob.Type.SystemVm || job.getInstanceType() == AsyncJob.Type.DomainRouter) {
            VMInstanceVO vm = ApiDBUtils.findVMInstanceById(job.getInstanceId());
            if (vm != null) {
                jobInstanceId = vm.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.Snapshot) {
            Snapshot snapshot = ApiDBUtils.findSnapshotById(job.getInstanceId());
            if (snapshot != null) {
                jobInstanceId = snapshot.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.Host) {
            Host host = ApiDBUtils.findHostById(job.getInstanceId());
            if (host != null) {
                jobInstanceId = host.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.StoragePool) {
            StoragePoolVO spool = ApiDBUtils.findStoragePoolById(job.getInstanceId());
            if (spool != null) {
                jobInstanceId = spool.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.IpAddress) {
            IPAddressVO ip = ApiDBUtils.findIpAddressById(job.getInstanceId());
            if (ip != null) {
                jobInstanceId = ip.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.SecurityGroup) {
            SecurityGroup sg = ApiDBUtils.findSecurityGroupById(job.getInstanceId());
            if (sg != null) {
                jobInstanceId = sg.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.PhysicalNetwork) {
            PhysicalNetworkVO pnet = ApiDBUtils.findPhysicalNetworkById(job.getInstanceId());
            if (pnet != null) {
                jobInstanceId = pnet.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.TrafficType) {
            PhysicalNetworkTrafficTypeVO trafficType = ApiDBUtils.findPhysicalNetworkTrafficTypeById(job.getInstanceId());
            if (trafficType != null) {
                jobInstanceId = trafficType.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.PhysicalNetworkServiceProvider) {
            PhysicalNetworkServiceProvider sp = ApiDBUtils.findPhysicalNetworkServiceProviderById(job.getInstanceId());
            if (sp != null) {
                jobInstanceId = sp.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.FirewallRule) {
            FirewallRuleVO fw = ApiDBUtils.findFirewallRuleById(job.getInstanceId());
            if (fw != null) {
                jobInstanceId = fw.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.Account) {
            Account acct = ApiDBUtils.findAccountById(job.getInstanceId());
            if (acct != null) {
                jobInstanceId = acct.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.User) {
            User usr = ApiDBUtils.findUserById(job.getInstanceId());
            if (usr != null) {
                jobInstanceId = usr.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.StaticRoute) {
            StaticRouteVO route = ApiDBUtils.findStaticRouteById(job.getInstanceId());
            if (route != null) {
                jobInstanceId = route.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.PrivateGateway) {
            VpcGatewayVO gateway = ApiDBUtils.findVpcGatewayById(job.getInstanceId());
            if (gateway != null) {
                jobInstanceId = gateway.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.Counter) {
            CounterVO counter = ApiDBUtils.getCounter(job.getInstanceId());
            if (counter != null) {
                jobInstanceId = counter.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.Condition) {
            ConditionVO condition = ApiDBUtils.findConditionById(job.getInstanceId());
            if (condition != null) {
                jobInstanceId = condition.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.AutoScalePolicy) {
            AutoScalePolicyVO policy = ApiDBUtils.findAutoScalePolicyById(job.getInstanceId());
            if (policy != null) {
                jobInstanceId = policy.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.AutoScaleVmProfile) {
            AutoScaleVmProfileVO profile = ApiDBUtils.findAutoScaleVmProfileById(job.getInstanceId());
            if (profile != null) {
                jobInstanceId = profile.getUuid();
            }
        } else if (job.getInstanceType() == AsyncJob.Type.AutoScaleVmGroup) {
            AutoScaleVmGroupVO group = ApiDBUtils.findAutoScaleVmGroupById(job.getInstanceId());
            if (group != null) {
                jobInstanceId = group.getUuid();
            }
        } else if (job.getInstanceType() != AsyncJob.Type.None) {
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
        return _domainRouterJoinDao.newDomainRouterResponse(vr, caller);
    }

    public static DomainRouterResponse fillRouterDetails(DomainRouterResponse vrData, DomainRouterJoinVO vr){
        return _domainRouterJoinDao.setDomainRouterResponse(vrData, vr);
    }

    public static List<DomainRouterJoinVO> newDomainRouterView(VirtualRouter vr){
        return _domainRouterJoinDao.newDomainRouterView(vr);
    }

    public static UserVmResponse newUserVmResponse(String objectName, UserVmJoinVO userVm, EnumSet<VMDetails> details, Account caller) {
        return _userVmJoinDao.newUserVmResponse(objectName, userVm, details, caller);
    }

    public static UserVmResponse fillVmDetails(UserVmResponse vmData, UserVmJoinVO vm){
        return _userVmJoinDao.setUserVmResponse(vmData, vm);
    }

    public static List<UserVmJoinVO> newUserVmView(UserVm... userVms){
        return _userVmJoinDao.newUserVmView(userVms);
    }

    public static SecurityGroupResponse newSecurityGroupResponse(SecurityGroupJoinVO vsg, Account caller) {
        return _securityGroupJoinDao.newSecurityGroupResponse(vsg, caller);
    }

    public static SecurityGroupResponse fillSecurityGroupDetails(SecurityGroupResponse vsgData, SecurityGroupJoinVO sg){
        return _securityGroupJoinDao.setSecurityGroupResponse(vsgData, sg);
    }

    public static List<SecurityGroupJoinVO> newSecurityGroupView(SecurityGroup sg){
        return _securityGroupJoinDao.newSecurityGroupView(sg);
    }

    public static List<SecurityGroupJoinVO> findSecurityGroupViewById(Long sgId){
        return _securityGroupJoinDao.searchByIds(sgId);
    }

    public static ResourceTagResponse newResourceTagResponse(ResourceTagJoinVO vsg, boolean keyValueOnly) {
        return _tagJoinDao.newResourceTagResponse(vsg, keyValueOnly);
    }

    public static ResourceTagJoinVO newResourceTagView(ResourceTag sg){
        return _tagJoinDao.newResourceTagView(sg);
    }

    public static ResourceTagJoinVO findResourceTagViewById(Long tagId){
        List<ResourceTagJoinVO> tags = _tagJoinDao.searchByIds(tagId);
        if ( tags != null && tags.size() > 0 ){
            return tags.get(0);
        }
        else{
            return null;
        }
    }

    public static EventResponse newEventResponse(EventJoinVO ve) {
        return _eventJoinDao.newEventResponse(ve);
    }

    public static EventJoinVO newEventView(Event e){
        return _eventJoinDao.newEventView(e);
    }

    public static InstanceGroupResponse newInstanceGroupResponse(InstanceGroupJoinVO ve) {
        return _vmGroupJoinDao.newInstanceGroupResponse(ve);
    }

    public static InstanceGroupJoinVO newInstanceGroupView(InstanceGroup e){
        return _vmGroupJoinDao.newInstanceGroupView(e);
    }

    public static UserResponse newUserResponse(UserAccountJoinVO usr) {
        return _userAccountJoinDao.newUserResponse(usr);
    }

    public static UserAccountJoinVO newUserView(User usr){
        return _userAccountJoinDao.newUserView(usr);
    }

    public static UserAccountJoinVO newUserView(UserAccount usr){
        return _userAccountJoinDao.newUserView(usr);
    }

    public static ProjectResponse newProjectResponse(ProjectJoinVO proj) {
        return _projectJoinDao.newProjectResponse(proj);
    }

    public static ProjectResponse fillProjectDetails(ProjectResponse rsp, ProjectJoinVO proj){
        return _projectJoinDao.setProjectResponse(rsp,proj);
    }

    public static List<ProjectJoinVO> newProjectView(Project proj){
        return _projectJoinDao.newProjectView(proj);
    }

    public static List<UserAccountJoinVO> findUserViewByAccountId(Long accountId){
        return _userAccountJoinDao.searchByAccountId(accountId);
    }

    public static ProjectAccountResponse newProjectAccountResponse(ProjectAccountJoinVO proj) {
        return _projectAccountJoinDao.newProjectAccountResponse(proj);
    }

    public static ProjectAccountJoinVO newProjectAccountView(ProjectAccount proj) {
        return _projectAccountJoinDao.newProjectAccountView(proj);
    }

    public static ProjectInvitationResponse newProjectInvitationResponse(ProjectInvitationJoinVO proj) {
        return _projectInvitationJoinDao.newProjectInvitationResponse(proj);
    }

    public static ProjectInvitationJoinVO newProjectInvitationView(ProjectInvitation proj) {
        return _projectInvitationJoinDao.newProjectInvitationView(proj);
    }

    public static HostResponse newHostResponse(HostJoinVO vr, EnumSet<HostDetails> details) {
        return _hostJoinDao.newHostResponse(vr, details);
    }

    public static HostResponse fillHostDetails(HostResponse vrData, HostJoinVO vr){
        return _hostJoinDao.setHostResponse(vrData, vr);
    }

    public static List<HostJoinVO> newHostView(Host vr){
        return _hostJoinDao.newHostView(vr);
    }

    public static VolumeResponse newVolumeResponse(VolumeJoinVO vr) {
        return _volJoinDao.newVolumeResponse(vr);
    }


    public static VolumeResponse fillVolumeDetails(VolumeResponse vrData, VolumeJoinVO vr){
        return _volJoinDao.setVolumeResponse(vrData, vr);
    }

    public static List<VolumeJoinVO> newVolumeView(Volume vr){
        return _volJoinDao.newVolumeView(vr);
    }

    public static StoragePoolResponse newStoragePoolResponse(StoragePoolJoinVO vr) {
        return _poolJoinDao.newStoragePoolResponse(vr);
    }

    public static StoragePoolResponse fillStoragePoolDetails(StoragePoolResponse vrData, StoragePoolJoinVO vr){
        return _poolJoinDao.setStoragePoolResponse(vrData, vr);
    }

    public static List<StoragePoolJoinVO> newStoragePoolView(StoragePool vr){
        return _poolJoinDao.newStoragePoolView(vr);
    }


    public static AccountResponse newAccountResponse(AccountJoinVO ve) {
        return _accountJoinDao.newAccountResponse(ve);
    }

    public static AccountJoinVO newAccountView(Account e){
        return _accountJoinDao.newAccountView(e);
    }

    public static AccountJoinVO findAccountViewById(Long accountId) {
        return _accountJoinDao.findByIdIncludingRemoved(accountId);
    }

    public static AsyncJobResponse newAsyncJobResponse(AsyncJobJoinVO ve) {
        return _jobJoinDao.newAsyncJobResponse(ve);
    }

    public static AsyncJobJoinVO newAsyncJobView(AsyncJob e){
        return _jobJoinDao.newAsyncJobView(e);
    }

   public static DiskOfferingResponse newDiskOfferingResponse(DiskOfferingJoinVO offering) {
       return _diskOfferingJoinDao.newDiskOfferingResponse(offering);
   }

   public static DiskOfferingJoinVO newDiskOfferingView(DiskOffering offering){
       return _diskOfferingJoinDao.newDiskOfferingView(offering);
   }

   public static ServiceOfferingResponse newServiceOfferingResponse(ServiceOfferingJoinVO offering) {
       return _serviceOfferingJoinDao.newServiceOfferingResponse(offering);
   }

   public static ServiceOfferingJoinVO newServiceOfferingView(ServiceOffering offering){
       return _serviceOfferingJoinDao.newServiceOfferingView(offering);
   }

   public static ZoneResponse newDataCenterResponse(DataCenterJoinVO dc, Boolean showCapacities) {
       return _dcJoinDao.newDataCenterResponse(dc, showCapacities);
   }

   public static DataCenterJoinVO newDataCenterView(DataCenter dc){
       return _dcJoinDao.newDataCenterView(dc);
   }
   
   public static Map<String, String> findHostDetailsById(long hostId){
	   return _hostDetailsDao.findDetails(hostId);
   }

   public static List<NicSecondaryIpVO> findNicSecondaryIps(long nicId) {
       return _nicSecondaryIpDao.listByNicId(nicId);
   }
}
