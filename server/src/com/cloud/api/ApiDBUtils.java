package com.cloud.api;

import java.util.List;

import com.cloud.agent.manager.AgentManager;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.IPAddressVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.security.NetworkGroupManager;
import com.cloud.offering.ServiceOffering;
import com.cloud.server.Criteria;
import com.cloud.server.ManagementServer;
import com.cloud.server.StatsCollector;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;

public class ApiDBUtils {
    private static ManagementServer _ms;
    private static AccountManager _accountMgr;
    private static AgentManager _agentMgr;
    private static NetworkGroupManager _networkGroupMgr;
    private static StorageManager _storageMgr;
    private static StatsCollector _statsCollector;

    private static AccountDao _accountDao;
    private static ClusterDao _clusterDao;
    private static DiskOfferingDao _diskOfferingDao;
    private static DomainDao _domainDao;
    private static GuestOSCategoryDao _guestOSCategoryDao;
    private static HostDao _hostDao;
    private static IPAddressDao _ipAddressDao;
    private static HostPodDao _podDao;
    private static ServiceOfferingDao _serviceOfferingDao;
    private static StoragePoolDao _storagePoolDao;
    private static VMTemplateDao _templateDao;
    private static UserDao _userDao;
    private static UserStatisticsDao _userStatsDao;
    private static UserVmDao _userVmDao;
    private static VolumeDao _volumeDao;
    private static DataCenterDao _zoneDao;

    static {
        _ms = (ManagementServer)ComponentLocator.getComponent(ManagementServer.Name);

        ComponentLocator locator = ComponentLocator.getLocator(ManagementServer.Name);
        _accountMgr = locator.getManager(AccountManager.class);
        _agentMgr = locator.getManager(AgentManager.class);
        _networkGroupMgr = locator.getManager(NetworkGroupManager.class);
        _storageMgr = locator.getManager(StorageManager.class);

        _accountDao = locator.getDao(AccountDao.class);
        _clusterDao = locator.getDao(ClusterDao.class);
        _diskOfferingDao = locator.getDao(DiskOfferingDao.class);
        _domainDao = locator.getDao(DomainDao.class);        
        _guestOSCategoryDao = locator.getDao(GuestOSCategoryDao.class);
        _hostDao = locator.getDao(HostDao.class);
        _ipAddressDao = locator.getDao(IPAddressDao.class);
        _podDao = locator.getDao(HostPodDao.class);
        _serviceOfferingDao = locator.getDao(ServiceOfferingDao.class);
        _storagePoolDao = locator.getDao(StoragePoolDao.class);
        _templateDao = locator.getDao(VMTemplateDao.class);
        _userDao = locator.getDao(UserDao.class);
        _userStatsDao = locator.getDao(UserStatisticsDao.class);
        _userVmDao = locator.getDao(UserVmDao.class);
        _volumeDao = locator.getDao(VolumeDao.class);
        _zoneDao = locator.getDao(DataCenterDao.class);

        // Note:  stats collector should already have been initialized by this time, otherwise a null instance is returned
        _statsCollector = StatsCollector.getInstance();
    }

    /////////////////////////////////////////////////////////////
    //               ManagementServer methods                  //
    /////////////////////////////////////////////////////////////

    public static long getMemoryUsagebyHost(Long hostId) {
        // TODO:  This method is for the API only, but it has configuration values (ramSize for system vms)
        // so if this Utils class can have some kind of config rather than a static initializer (maybe from
        // management server instantiation?) then maybe the management server method can be moved entirely
        // into this utils class.
        return _ms.getMemoryUsagebyHost(hostId);
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
    
    public static long getResourceCount(ResourceType type, long accountId) {
        AccountVO account = _accountDao.findById(accountId);
        
        if (account == null) {
            return -1;
        }
        
        return _accountMgr.getResourceCount(account, type);
    }

    public static String getNetworkGroupsNamesForVm(long vmId) {
        return _networkGroupMgr.getNetworkGroupsNamesForVm(vmId);
    }

    public static boolean isLocalStorageActiveOnHost(HostVO host) {
        return _storageMgr.isLocalStorageActiveOnHost(host);
    }

    /////////////////////////////////////////////////////////////
    //                    Misc methods                         //
    /////////////////////////////////////////////////////////////

    public static HostStats getHostStatistics(long hostId) {
        return _statsCollector.getHostStats(hostId);
    }

    /////////////////////////////////////////////////////////////
    //                     Dao methods                         //
    /////////////////////////////////////////////////////////////

    public static Account findAccountById(Long accountId) {
        return _accountDao.findById(accountId);
    }

    public static ClusterVO findClusterById(long clusterId) {
        return _clusterDao.findById(clusterId);
    }

    public static DiskOfferingVO findDiskOfferingById(Long diskOfferingId) {
        return _diskOfferingDao.findById(diskOfferingId);
    }

    public static DomainVO findDomainById(Long domainId) {
        return _domainDao.findById(domainId);
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

    public static HostPodVO findPodById(Long podId) {
        return _podDao.findById(podId);
    }

    public static ServiceOffering findServiceOfferingById(Long serviceOfferingId) {
        return _serviceOfferingDao.findById(serviceOfferingId);
    }

    public static StoragePoolVO findStoragePoolById(Long storagePoolId) {
        return _storagePoolDao.findById(storagePoolId);
    }

    public static VMTemplateVO findTemplateById(Long templateId) {
        return _templateDao.findById(templateId);
    }

    public static User findUserById(Long userId) {
        return _userDao.findById(userId);
    }

    public static VolumeVO findVolumeById(Long volumeId) {
        return _volumeDao.findById(volumeId);
    }

    public static DataCenterVO findZoneById(Long zoneId) {
        return _zoneDao.findById(zoneId);
    }

    public static List<UserStatisticsVO> listUserStatsBy(Long accountId) {
        return _userStatsDao.listBy(accountId);
    }

    public static List<UserVmVO> listUserVMsByHostId(long hostId) {
        return _userVmDao.listByHostId(hostId);
    }
}
