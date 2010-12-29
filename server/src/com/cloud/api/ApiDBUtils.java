package com.cloud.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.cloud.agent.AgentManager;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.ClusterVO;
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
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IPAddressVO;
import com.cloud.network.LoadBalancerVO;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkRuleConfigVO;
import com.cloud.network.NetworkVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkRuleConfigDao;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.server.Criteria;
import com.cloud.server.ManagementServer;
import com.cloud.server.StatsCollector;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.StorageStats;
import com.cloud.storage.UploadVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.UploadDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.DateUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.Nic;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmStats;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;

public class ApiDBUtils {
    private static ManagementServer _ms;
    private static AccountManager _accountMgr;
    private static AgentManager _agentMgr;
    public static AsyncJobManager _asyncMgr;
    private static SecurityGroupManager _networkGroupMgr;
    private static SnapshotManager _snapMgr;
    private static StorageManager _storageMgr;
    private static UserVmManager _userVmMgr;
    private static NetworkManager _networkMgr;
    private static StatsCollector _statsCollector;

    private static AccountDao _accountDao;
    private static AccountVlanMapDao _accountVlanMapDao;
    private static ClusterDao _clusterDao;
    private static DiskOfferingDao _diskOfferingDao;
    private static DomainDao _domainDao;
    private static DomainRouterDao _domainRouterDao;
    private static GuestOSDao _guestOSDao;
    private static GuestOSCategoryDao _guestOSCategoryDao;
    private static HostDao _hostDao;
    private static IPAddressDao _ipAddressDao;
    private static LoadBalancerDao _loadBalancerDao;
    private static SecurityGroupDao _networkGroupDao;
    private static NetworkRuleConfigDao _networkRuleConfigDao;
    private static HostPodDao _podDao;
    private static ServiceOfferingDao _serviceOfferingDao;
    private static SnapshotDao _snapshotDao;
    private static StoragePoolDao _storagePoolDao;
    private static VMTemplateDao _templateDao;
    private static VMTemplateHostDao _templateHostDao;
    private static UploadDao _uploadDao;
    private static UserDao _userDao;
    private static UserStatisticsDao _userStatsDao;
    private static UserVmDao _userVmDao;
    private static VlanDao _vlanDao;
    private static VolumeDao _volumeDao;
    private static DataCenterDao _zoneDao;
    private static NetworkOfferingDao _networkOfferingDao;
    private static NetworkDao _networkDao;

    static {
        _ms = (ManagementServer)ComponentLocator.getComponent(ManagementServer.Name);

        ComponentLocator locator = ComponentLocator.getLocator(ManagementServer.Name);
        _accountMgr = locator.getManager(AccountManager.class);
        _agentMgr = locator.getManager(AgentManager.class);
        _asyncMgr = locator.getManager(AsyncJobManager.class);
        _networkGroupMgr = locator.getManager(SecurityGroupManager.class);
        _snapMgr = locator.getManager(SnapshotManager.class);
        _storageMgr = locator.getManager(StorageManager.class);
        _userVmMgr = locator.getManager(UserVmManager.class);
        _networkMgr = locator.getManager(NetworkManager.class);

        _accountDao = locator.getDao(AccountDao.class);
        _accountVlanMapDao = locator.getDao(AccountVlanMapDao.class);
        _clusterDao = locator.getDao(ClusterDao.class);
        _diskOfferingDao = locator.getDao(DiskOfferingDao.class);
        _domainDao = locator.getDao(DomainDao.class);        
        _domainRouterDao = locator.getDao(DomainRouterDao.class);        
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
        _templateHostDao = locator.getDao(VMTemplateHostDao.class);
        _uploadDao = locator.getDao(UploadDao.class);
        _userDao = locator.getDao(UserDao.class);
        _userStatsDao = locator.getDao(UserStatisticsDao.class);
        _userVmDao = locator.getDao(UserVmDao.class);
        _vlanDao = locator.getDao(VlanDao.class);
        _volumeDao = locator.getDao(VolumeDao.class);
        _zoneDao = locator.getDao(DataCenterDao.class);
        _networkGroupDao = locator.getDao(SecurityGroupDao.class);
        _networkOfferingDao = locator.getDao(NetworkOfferingDao.class);
        _networkDao = locator.getDao(NetworkDao.class);

        // Note:  stats collector should already have been initialized by this time, otherwise a null instance is returned
        _statsCollector = StatsCollector.getInstance();
    }

    /////////////////////////////////////////////////////////////
    //               ManagementServer methods                  //
    /////////////////////////////////////////////////////////////

    public static VMInstanceVO findVMInstanceById(long vmId) {
        return _ms.findVMInstanceById(vmId);
    }

    public static long getMemoryUsagebyHost(Long hostId) {
        // TODO:  This method is for the API only, but it has configuration values (ramSize for system vms)
        // so if this Utils class can have some kind of config rather than a static initializer (maybe from
        // management server instantiation?) then maybe the management server method can be moved entirely
        // into this utils class.
        return _ms.getMemoryUsagebyHost(hostId);
    }

    public static Long getPodIdForVlan(long vlanDbId) {
        return _ms.getPodIdForVlan(vlanDbId);
    }

    public static String getVersion() {
        return _ms.getVersion();
    }

    public static List<UserVmVO> searchForUserVMs(Criteria c) {
        return _ms.searchForUserVMs(c);
    }

    public static List<? extends StoragePoolVO> searchForStoragePools(Criteria c) {
        return _ms.searchForStoragePools(c);
    }

    /////////////////////////////////////////////////////////////
    //                   Manager methods                       //
    /////////////////////////////////////////////////////////////

    public static long findCorrectResourceLimit(ResourceType type, long accountId) {
        AccountVO account = _accountDao.findById(accountId);

        if (account == null) {
            return -1;
        }

        return _accountMgr.findCorrectResourceLimit(account, type);
    }

    public static AsyncJobVO findInstancePendingAsyncJob(String instanceType, long instanceId) {
        return _asyncMgr.findInstancePendingAsyncJob(instanceType, instanceId);
    }

    public static long getResourceCount(ResourceType type, long accountId) {
        AccountVO account = _accountDao.findById(accountId);
        
        if (account == null) {
            return -1;
        }
        
        return _accountMgr.getResourceCount(account, type);
    }

    public static String getNetworkGroupsNamesForVm(long vmId) {
        return _networkGroupMgr.getSecurityGroupsNamesForVm(vmId);
    }

    public static String getSnapshotIntervalTypes(long snapshotId) {
        String intervalTypes = "";

        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
        if (snapshot.getSnapshotType() == Snapshot.Type.MANUAL.ordinal()) {
            return "MANUAL";
        }

        List<SnapshotPolicyVO> policies = _snapMgr.listPoliciesforVolume(snapshot.getVolumeId());
        for (SnapshotPolicyVO policy : policies) {
            if (!intervalTypes.isEmpty()) {
                intervalTypes += ",";
            }
            if (policy.getId() == Snapshot.MANUAL_POLICY_ID) {
                intervalTypes += "MANUAL";
            } else {
                intervalTypes += DateUtil.getIntervalType(policy.getInterval()).toString();
            }
        }
        return intervalTypes;
    }

    public static String getStoragePoolTags(long poolId) {
        return _storageMgr.getStoragePoolTags(poolId);
    }
    
    public static boolean isLocalStorageActiveOnHost(Host host) {
        return _storageMgr.isLocalStorageActiveOnHost(host);
    }

    public static InstanceGroupVO findInstanceGroupForVM(long vmId) {
        return _userVmMgr.getGroupForVm(vmId);
    }

    /////////////////////////////////////////////////////////////
    //                    Misc methods                         //
    /////////////////////////////////////////////////////////////

    public static HostStats getHostStatistics(long hostId) {
        return _statsCollector.getHostStats(hostId);
    }

    public static StorageStats getStoragePoolStatistics(long id) {
        return _statsCollector.getStoragePoolStats(id);
    }

    public static VmStats getVmStatistics(long hostId) {
        return _statsCollector.getVmStats(hostId);
    }

    /////////////////////////////////////////////////////////////
    //                     Dao methods                         //
    /////////////////////////////////////////////////////////////

    public static Account findAccountById(Long accountId) {
        return _accountDao.findById(accountId);
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
        return _domainDao.findById(domainId);
    }

    public static DomainRouterVO findDomainRouterById(Long routerId) {
        return _domainRouterDao.findById(routerId);
    }

    public static GuestOS findGuestOSById(Long id) {
        return _guestOSDao.findById(id);
    }

    public static HostVO findHostById(Long hostId) {
        return _hostDao.findById(hostId);
    }

    public static IPAddressVO findIpAddressById(String address) {
        return _ipAddressDao.findById(address);
    }

    public static GuestOSCategoryVO getHostGuestOSCategory(long hostId) {
        Long guestOSCategoryID = _agentMgr.getGuestOSCategoryId(hostId);
        
        if (guestOSCategoryID != null) {
            return _guestOSCategoryDao.findById(guestOSCategoryID);
        } else {
            return null;
        }
    }

    public static LoadBalancerVO findLoadBalancerById(Long loadBalancerId) {
        return _loadBalancerDao.findById(loadBalancerId);
    }

    public static NetworkRuleConfigVO findNetworkRuleById(Long ruleId) {
        return _networkRuleConfigDao.findById(ruleId);
    }
    
    public static SecurityGroup findNetworkGroupById(Long groupId) {
        return _networkGroupDao.findById(groupId);
    }

    public static HostPodVO findPodById(Long podId) {
        return _podDao.findById(podId);
    }

    public static VolumeVO findRootVolume(long vmId) {
        List<VolumeVO> volumes = _volumeDao.findByInstanceAndType(vmId, VolumeType.ROOT);
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
        }
        else {
            return null;
        }
    }

    public static StoragePoolVO findStoragePoolById(Long storagePoolId) {
        return _storagePoolDao.findById(storagePoolId);
    }

    public static VMTemplateVO findTemplateById(Long templateId) {
        return _templateDao.findByIdIncludingRemoved(templateId);
    }

    public static VMTemplateHostVO findTemplateHostRef(long templateId, long zoneId) {
        HostVO secondaryStorageHost = _storageMgr.getSecondaryStorageHost(zoneId);
        if (secondaryStorageHost == null) {
            return null;
        } else {
            return _templateHostDao.findByHostTemplate(secondaryStorageHost.getId(), templateId);
        }
    }
    
    public static UploadVO findUploadById(Long id){
        return _uploadDao.findById(id);
    }
    
    public static User findUserById(Long userId) {
        return _userDao.findById(userId);
    }

    public static UserVm findUserVmById(Long vmId) {
        return _userVmDao.findById(vmId);
    }

    public static UserVm findUserVmByPublicIpAndGuestIp(String publicIp, String guestIp) {
        IPAddressVO addr = _ipAddressDao.findById(publicIp);
        List<UserVmVO> vms = _userVmDao.listVmsUsingGuestIpAddress(addr.getDataCenterId(), guestIp);
        if (vms != null) {
            for (UserVmVO vm : vms) {
                if (vm.getAccountId() == addr.getAllocatedToAccountId()) {
                    return vm;
                }
            }
        }
        return null;
    }

    public static VlanVO findVlanById(long vlanDbId) {
        return _vlanDao.findById(vlanDbId);
    }

    public static VolumeVO findVolumeById(Long volumeId) {
        return _volumeDao.findByIdIncludingRemoved(volumeId);
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

    public static List<VMTemplateHostVO> listTemplateHostBy(long templateId, Long zoneId) {
        if (zoneId != null) {
            HostVO secondaryStorageHost = _storageMgr.getSecondaryStorageHost(zoneId);
            if (secondaryStorageHost == null) {
                return new ArrayList<VMTemplateHostVO>();
            } else {
                return _templateHostDao.listByHostTemplate(secondaryStorageHost.getId(), templateId);
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

    public static boolean volumeIsOnSharedStorage(long volumeId) throws InvalidParameterValueException {
        // Check that the volume is valid
        VolumeVO volume = _volumeDao.findById(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Please specify a valid volume ID.");
        }

        return _storageMgr.volumeOnSharedStoragePool(volume);
    }
    
    public static List<? extends Nic> getNics(VirtualMachine vm) {
        return _networkMgr.getNics(vm);
    }
    
    public static Network getNetwork(long id) {
        return _networkMgr.getNetwork(id);
    }
    
    public static void synchronizeCommand(Object job, String syncObjType, long syncObjId) {
        _asyncMgr.syncAsyncJobExecution((AsyncJobVO)job, syncObjType, syncObjId);
    }
    
    public static NetworkOfferingVO findNetworkOfferingById(long networkOfferingId) {
        return _networkOfferingDao.findByIdIncludingRemoved(networkOfferingId);
    }
    
    public static List<? extends Vlan> listVlanByNetworkId(long networkId) {
        return _vlanDao.listVlansByNetworkId(networkId);
    }
    
    public static NetworkVO findNetworkById(long id) {
        return _networkDao.findById(id);
    }
    
    public static Map<Service, Map<Capability, String>> getZoneCapabilities(long zoneId) {
        return _networkMgr.getZoneCapabilities(zoneId);
    }
    
    public static long getPublicNetworkIdByZone(long zoneId) {
        return _networkMgr.getSystemNetworkIdByZoneAndTrafficTypeAndGuestType(zoneId, TrafficType.Public, null);
    }

    public static Long getVlanNetworkId(long vlanId) {
        VlanVO vlan = _vlanDao.findById(vlanId);
        if (vlan != null) {
            return vlan.getNetworkId();
        } else {
            return null;
        }
    }
    
}
