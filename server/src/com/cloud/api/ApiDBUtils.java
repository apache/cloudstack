package com.cloud.api;

import java.util.List;

import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.IPAddressVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.security.NetworkGroupManager;
import com.cloud.offering.ServiceOffering;
import com.cloud.server.Criteria;
import com.cloud.server.ManagementServer;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
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

public class ApiDBUtils {
    private static ManagementServer _ms;
    private static AccountManager _accountMgr;
    private static NetworkGroupManager _networkGroupMgr;
    private static AccountDao _accountDao;
    private static DiskOfferingDao _diskOfferingDao;
    private static DomainDao _domainDao;
    private static HostDao _hostDao;
    private static IPAddressDao _ipAddressDao;
    private static ServiceOfferingDao _serviceOfferingDao;
    private static StoragePoolDao _storagePoolDao;
    private static VMTemplateDao _templateDao;
    private static UserDao _userDao;
    private static UserStatisticsDao _userStatsDao;
    private static VolumeDao _volumeDao;
    private static DataCenterDao _zoneDao;

    static {
        _ms = (ManagementServer)ComponentLocator.getComponent(ManagementServer.Name);

        ComponentLocator locator = ComponentLocator.getLocator(ManagementServer.Name);
        _accountMgr = locator.getManager(AccountManager.class);
        _networkGroupMgr = locator.getManager(NetworkGroupManager.class);
        _accountDao = locator.getDao(AccountDao.class);
        _diskOfferingDao = locator.getDao(DiskOfferingDao.class);
        _domainDao = locator.getDao(DomainDao.class);
        _hostDao = locator.getDao(HostDao.class);
        _ipAddressDao = locator.getDao(IPAddressDao.class);
        _serviceOfferingDao = locator.getDao(ServiceOfferingDao.class);
        _storagePoolDao = locator.getDao(StoragePoolDao.class);
        _templateDao = locator.getDao(VMTemplateDao.class);
        _userDao = locator.getDao(UserDao.class);
        _userStatsDao = locator.getDao(UserStatisticsDao.class);
        _volumeDao = locator.getDao(VolumeDao.class);
        _zoneDao = locator.getDao(DataCenterDao.class);
    }

    /////////////////////////////////////////////////////////////
    //               ManagementServer methods                  //
    /////////////////////////////////////////////////////////////

    public static List<UserVmVO> searchForUserVMs(Criteria c) {
        return _ms.searchForUserVMs(c);
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

    /////////////////////////////////////////////////////////////
    //                     Dao methods                         //
    /////////////////////////////////////////////////////////////

    public static Account findAccountById(Long accountId) {
        return _accountDao.findById(accountId);
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
}
