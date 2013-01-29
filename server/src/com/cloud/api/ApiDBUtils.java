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
import com.cloud.dc.AccountVlanMapVO;
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
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IPAddressVO;
import com.cloud.network.IpAddress;
import com.cloud.network.LoadBalancerVO;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkDomainVO;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkProfile;
import com.cloud.network.NetworkRuleConfigVO;
import com.cloud.network.NetworkVO;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PhysicalNetworkVO;
import com.cloud.network.Site2SiteVpnGatewayVO;
import com.cloud.network.Site2SiteCustomerGatewayVO;
import com.cloud.network.Networks.TrafficType;
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
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.NetworkDomainDao;
import com.cloud.network.dao.NetworkRuleConfigDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeVO;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.Site2SiteCustomerGatewayDao;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.network.vpc.StaticRouteVO;
import com.cloud.network.vpc.VpcGatewayVO;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.StaticRouteDao;
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
import com.cloud.server.Criteria;
import com.cloud.server.ManagementServer;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.server.StatsCollector;
import com.cloud.server.TaggedResourceService;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.StorageStats;
import com.cloud.storage.UploadVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateS3VO;
import com.cloud.storage.VMTemplateSwiftVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.VolumeHostVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.UploadDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplateS3Dao;
import com.cloud.storage.dao.VMTemplateSwiftDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeHostDao;
import com.cloud.storage.snapshot.SnapshotPolicy;
import com.cloud.user.Account;
import com.cloud.user.AccountDetailsDao;
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
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.InstanceGroup;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmStats;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.network.vpc.dao.VpcDao;

public class ApiDBUtils {
    private static ManagementServer _ms;
    public static AsyncJobManager _asyncMgr;
    private static SecurityGroupManager _securityGroupMgr;
    private static StorageManager _storageMgr;
    private static UserVmManager _userVmMgr;
    private static NetworkManager _networkMgr;
    private static NetworkModel _networkModel;
    private static StatsCollector _statsCollector;

    private static AccountDao _accountDao;
    private static AccountVlanMapDao _accountVlanMapDao;
    private static ClusterDao _clusterDao;
    private static CapacityDao _capacityDao;
    private static DiskOfferingDao _diskOfferingDao;
    private static DomainDao _domainDao;
    private static DomainRouterDao _domainRouterDao;
    private static DomainRouterJoinDao _domainRouterJoinDao;
    private static GuestOSDao _guestOSDao;
    private static GuestOSCategoryDao _guestOSCategoryDao;
    private static HostDao _hostDao;
    private static IPAddressDao _ipAddressDao;
    private static LoadBalancerDao _loadBalancerDao;
    private static SecurityGroupDao _securityGroupDao;
    private static SecurityGroupJoinDao _securityGroupJoinDao;
    private static NetworkRuleConfigDao _networkRuleConfigDao;
    private static HostPodDao _podDao;
    private static ServiceOfferingDao _serviceOfferingDao;
    private static SnapshotDao _snapshotDao;
    private static StoragePoolDao _storagePoolDao;
    private static VMTemplateDao _templateDao;
    private static VMTemplateDetailsDao _templateDetailsDao;
    private static VMTemplateHostDao _templateHostDao;
    private static VMTemplateSwiftDao _templateSwiftDao;
    private static VMTemplateS3Dao _templateS3Dao;
    private static UploadDao _uploadDao;
    private static UserDao _userDao;
    private static UserStatisticsDao _userStatsDao;
    private static UserVmDao _userVmDao;
    private static UserVmJoinDao _userVmJoinDao;
    private static VlanDao _vlanDao;
    private static VolumeDao _volumeDao;
    private static Site2SiteVpnGatewayDao _site2SiteVpnGatewayDao;
    private static Site2SiteCustomerGatewayDao _site2SiteCustomerGatewayDao;
    private static VolumeHostDao _volumeHostDao;
    private static DataCenterDao _zoneDao;
    private static NetworkOfferingDao _networkOfferingDao;
    private static NetworkDao _networkDao;
    private static PhysicalNetworkDao _physicalNetworkDao;
    private static ConfigurationService _configMgr;
    private static ConfigurationDao _configDao;
    private static ConsoleProxyDao _consoleProxyDao;
    private static FirewallRulesCidrsDao _firewallCidrsDao;
    private static VMInstanceDao _vmDao;
    private static ResourceLimitService _resourceLimitMgr;
    private static ProjectService _projectMgr;
    private static ResourceManager _resourceMgr;
    private static AccountDetailsDao _accountDetailsDao;
    private static NetworkDomainDao _networkDomainDao;
    private static HighAvailabilityManager _haMgr;
    private static VpcManager _vpcMgr;
    private static TaggedResourceService _taggedResourceService;
    private static UserVmDetailsDao _userVmDetailsDao;
    private static SSHKeyPairDao _sshKeyPairDao;

    private static ConditionDao _asConditionDao;
    private static AutoScalePolicyConditionMapDao _asPolicyConditionMapDao;
    private static AutoScaleVmGroupPolicyMapDao _asVmGroupPolicyMapDao;
    private static AutoScalePolicyDao _asPolicyDao;
    private static AutoScaleVmProfileDao _asVmProfileDao;
    private static AutoScaleVmGroupDao _asVmGroupDao;
    private static CounterDao _counterDao;
    private static ResourceTagJoinDao _tagJoinDao;
    private static EventJoinDao _eventJoinDao;
    private static InstanceGroupJoinDao _vmGroupJoinDao;
    private static UserAccountJoinDao _userAccountJoinDao;
    private static ProjectJoinDao _projectJoinDao;
    private static ProjectAccountJoinDao _projectAccountJoinDao;
    private static ProjectInvitationJoinDao _projectInvitationJoinDao;
    private static HostJoinDao _hostJoinDao;
    private static VolumeJoinDao _volJoinDao;
    private static StoragePoolJoinDao _poolJoinDao;
    private static AccountJoinDao _accountJoinDao;
    private static AsyncJobJoinDao _jobJoinDao;
    private static DiskOfferingJoinDao _diskOfferingJoinDao;
    private static ServiceOfferingJoinDao _srvOfferingJoinDao;
    private static DataCenterJoinDao _dcJoinDao;

    private static PhysicalNetworkTrafficTypeDao _physicalNetworkTrafficTypeDao;
    private static PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    private static FirewallRulesDao _firewallRuleDao;
    private static StaticRouteDao _staticRouteDao;
    private static VpcGatewayDao _vpcGatewayDao;
    private static VpcDao _vpcDao;
    private static VpcOfferingDao _vpcOfferingDao;
    private static SnapshotPolicyDao _snapshotPolicyDao;
    private static AsyncJobDao _asyncJobDao;

    static {
        _ms = (ManagementServer) ComponentLocator.getComponent(ManagementServer.Name);
        ComponentLocator locator = ComponentLocator.getLocator(ManagementServer.Name);
        _asyncMgr = locator.getManager(AsyncJobManager.class);
        _securityGroupMgr = locator.getManager(SecurityGroupManager.class);
        _storageMgr = locator.getManager(StorageManager.class);
        _userVmMgr = locator.getManager(UserVmManager.class);
        _networkMgr = locator.getManager(NetworkManager.class);
        _networkModel = locator.getManager(NetworkModel.class);
        _configMgr = locator.getManager(ConfigurationService.class);

        _accountDao = locator.getDao(AccountDao.class);
        _accountVlanMapDao = locator.getDao(AccountVlanMapDao.class);
        _clusterDao = locator.getDao(ClusterDao.class);
        _capacityDao = locator.getDao(CapacityDao.class);
        _diskOfferingDao = locator.getDao(DiskOfferingDao.class);
        _domainDao = locator.getDao(DomainDao.class);
        _domainRouterDao = locator.getDao(DomainRouterDao.class);
        _domainRouterJoinDao = locator.getDao(DomainRouterJoinDao.class);
        _guestOSDao = locator.getDao(GuestOSDao.class);
        _guestOSCategoryDao = locator.getDao(GuestOSCategoryDao.class);
        _hostDao = locator.getDao(HostDao.class);
        _ipAddressDao = locator.getDao(IPAddressDao.class);
        _loadBalancerDao = locator.getDao(LoadBalancerDao.class);
        _networkRuleConfigDao = locator.getDao(NetworkRuleConfigDao.class);
        _podDao = locator.getDao(HostPodDao.class);
        _serviceOfferingDao = locator.getDao(ServiceOfferingDao.class);
        _snapshotDao = locator.getDao(SnapshotDao.class);
        _storagePoolDao = locator.getDao(StoragePoolDao.class);
        _templateDao = locator.getDao(VMTemplateDao.class);
        _templateDetailsDao = locator.getDao(VMTemplateDetailsDao.class);
        _templateHostDao = locator.getDao(VMTemplateHostDao.class);
        _templateSwiftDao = locator.getDao(VMTemplateSwiftDao.class);
        _templateS3Dao = locator.getDao(VMTemplateS3Dao.class);
        _uploadDao = locator.getDao(UploadDao.class);
        _userDao = locator.getDao(UserDao.class);
        _userStatsDao = locator.getDao(UserStatisticsDao.class);
        _userVmDao = locator.getDao(UserVmDao.class);
        _userVmJoinDao = locator.getDao(UserVmJoinDao.class);
        _vlanDao = locator.getDao(VlanDao.class);
        _volumeDao = locator.getDao(VolumeDao.class);
        _site2SiteVpnGatewayDao = locator.getDao(Site2SiteVpnGatewayDao.class);
        _site2SiteCustomerGatewayDao = locator.getDao(Site2SiteCustomerGatewayDao.class);
        _volumeHostDao = locator.getDao(VolumeHostDao.class);
        _zoneDao = locator.getDao(DataCenterDao.class);
        _securityGroupDao = locator.getDao(SecurityGroupDao.class);
        _securityGroupJoinDao = locator.getDao(SecurityGroupJoinDao.class);
        _networkOfferingDao = locator.getDao(NetworkOfferingDao.class);
        _networkDao = locator.getDao(NetworkDao.class);
        _physicalNetworkDao = locator.getDao(PhysicalNetworkDao.class);
        _configDao = locator.getDao(ConfigurationDao.class);
        _consoleProxyDao = locator.getDao(ConsoleProxyDao.class);
        _firewallCidrsDao = locator.getDao(FirewallRulesCidrsDao.class);
        _vmDao = locator.getDao(VMInstanceDao.class);
        _resourceLimitMgr = locator.getManager(ResourceLimitService.class);
        _projectMgr = locator.getManager(ProjectService.class);
        _resourceMgr = locator.getManager(ResourceManager.class);
        _accountDetailsDao = locator.getDao(AccountDetailsDao.class);
        _networkDomainDao = locator.getDao(NetworkDomainDao.class);
        _haMgr = locator.getManager(HighAvailabilityManager.class);
        _vpcMgr = locator.getManager(VpcManager.class);
        _taggedResourceService = locator.getManager(TaggedResourceService.class);
        _sshKeyPairDao = locator.getDao(SSHKeyPairDao.class);
        _userVmDetailsDao = locator.getDao(UserVmDetailsDao.class);
        _asConditionDao = locator.getDao(ConditionDao.class);
        _asPolicyDao = locator.getDao(AutoScalePolicyDao.class);
        _asPolicyConditionMapDao = locator.getDao(AutoScalePolicyConditionMapDao.class);
        _counterDao = locator.getDao(CounterDao.class);
        _asVmGroupPolicyMapDao = locator.getDao(AutoScaleVmGroupPolicyMapDao.class);
        _tagJoinDao = locator.getDao(ResourceTagJoinDao.class);
        _vmGroupJoinDao = locator.getDao(InstanceGroupJoinDao.class);
        _eventJoinDao = locator.getDao(EventJoinDao.class);
        _userAccountJoinDao = locator.getDao(UserAccountJoinDao.class);
        _projectJoinDao = locator.getDao(ProjectJoinDao.class);
        _projectAccountJoinDao = locator.getDao(ProjectAccountJoinDao.class);
        _projectInvitationJoinDao = locator.getDao(ProjectInvitationJoinDao.class);
        _hostJoinDao = locator.getDao(HostJoinDao.class);
        _volJoinDao = locator.getDao(VolumeJoinDao.class);
        _poolJoinDao = locator.getDao(StoragePoolJoinDao.class);
        _accountJoinDao = locator.getDao(AccountJoinDao.class);
        _jobJoinDao = locator.getDao(AsyncJobJoinDao.class);

        _physicalNetworkTrafficTypeDao = locator.getDao(PhysicalNetworkTrafficTypeDao.class);
        _physicalNetworkServiceProviderDao = locator.getDao(PhysicalNetworkServiceProviderDao.class);
        _firewallRuleDao = locator.getDao(FirewallRulesDao.class);
        _staticRouteDao = locator.getDao(StaticRouteDao.class);
        _vpcGatewayDao = locator.getDao(VpcGatewayDao.class);
        _asVmProfileDao = locator.getDao(AutoScaleVmProfileDao.class);
        _asVmGroupDao = locator.getDao(AutoScaleVmGroupDao.class);
        _vpcDao = locator.getDao(VpcDao.class);
        _vpcOfferingDao = locator.getDao(VpcOfferingDao.class);
        _snapshotPolicyDao = locator.getDao(SnapshotPolicyDao.class);
        _asyncJobDao = locator.getDao(AsyncJobDao.class);
        _diskOfferingJoinDao = locator.getDao(DiskOfferingJoinDao.class);
        _srvOfferingJoinDao = locator.getDao(ServiceOfferingJoinDao.class);
        _dcJoinDao = locator.getDao(DataCenterJoinDao.class);

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
        return snapshot.getType().name();
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
        if (snapshot != null && snapshot.getRemoved() == null && snapshot.getStatus() == Snapshot.Status.BackedUp) {
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
            return _storageMgr.getTemplateHostRef(zoneId, templateId, readyOnly);
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

        return _storageMgr.volumeOnSharedStoragePool(volume);
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
       return _srvOfferingJoinDao.newServiceOfferingResponse(offering);
   }

   public static ServiceOfferingJoinVO newServiceOfferingView(ServiceOffering offering){
       return _srvOfferingJoinDao.newServiceOfferingView(offering);
   }

   public static ZoneResponse newDataCenterResponse(DataCenterJoinVO dc, Boolean showCapacities) {
       return _dcJoinDao.newDataCenterResponse(dc, showCapacities);
   }

   public static DataCenterJoinVO newDataCenterView(DataCenter dc){
       return _dcJoinDao.newDataCenterView(dc);
   }
}
