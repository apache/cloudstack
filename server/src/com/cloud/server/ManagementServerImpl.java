/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.server;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.proxy.UpdateCertificateCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.alert.AlertManager;
import com.cloud.alert.AlertVO;
import com.cloud.alert.dao.AlertDao;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.api.commands.CreateDomainCmd;
import com.cloud.api.commands.DeleteDomainCmd;
import com.cloud.api.commands.DeletePreallocatedLunCmd;
import com.cloud.api.commands.DeployVMCmd;
import com.cloud.api.commands.ExtractVolumeCmd;
import com.cloud.api.commands.GetCloudIdentifierCmd;
import com.cloud.api.commands.ListAccountsCmd;
import com.cloud.api.commands.ListAlertsCmd;
import com.cloud.api.commands.ListAsyncJobsCmd;
import com.cloud.api.commands.ListCapabilitiesCmd;
import com.cloud.api.commands.ListCapacityCmd;
import com.cloud.api.commands.ListCfgsByCmd;
import com.cloud.api.commands.ListClustersCmd;
import com.cloud.api.commands.ListDiskOfferingsCmd;
import com.cloud.api.commands.ListDomainChildrenCmd;
import com.cloud.api.commands.ListDomainsCmd;
import com.cloud.api.commands.ListEventsCmd;
import com.cloud.api.commands.ListGuestOsCategoriesCmd;
import com.cloud.api.commands.ListGuestOsCmd;
import com.cloud.api.commands.ListHostsCmd;
import com.cloud.api.commands.ListHypervisorsCmd;
import com.cloud.api.commands.ListIsosCmd;
import com.cloud.api.commands.ListLoadBalancerRuleInstancesCmd;
import com.cloud.api.commands.ListLoadBalancerRulesCmd;
import com.cloud.api.commands.ListPodsByCmd;
import com.cloud.api.commands.ListPreallocatedLunsCmd;
import com.cloud.api.commands.ListPublicIpAddressesCmd;
import com.cloud.api.commands.ListRemoteAccessVpnsCmd;
import com.cloud.api.commands.ListRoutersCmd;
import com.cloud.api.commands.ListServiceOfferingsCmd;
import com.cloud.api.commands.ListSnapshotsCmd;
import com.cloud.api.commands.ListStoragePoolsCmd;
import com.cloud.api.commands.ListSystemVMsCmd;
import com.cloud.api.commands.ListTemplateOrIsoPermissionsCmd;
import com.cloud.api.commands.ListTemplatesCmd;
import com.cloud.api.commands.ListUsersCmd;
import com.cloud.api.commands.ListVMGroupsCmd;
import com.cloud.api.commands.ListVMsCmd;
import com.cloud.api.commands.ListVlanIpRangesCmd;
import com.cloud.api.commands.ListVolumesCmd;
import com.cloud.api.commands.ListVpnUsersCmd;
import com.cloud.api.commands.ListZonesByCmd;
import com.cloud.api.commands.RebootSystemVmCmd;
import com.cloud.api.commands.RegisterCmd;
import com.cloud.api.commands.RegisterPreallocatedLunCmd;
import com.cloud.api.commands.StartSystemVMCmd;
import com.cloud.api.commands.StopSystemVmCmd;
import com.cloud.api.commands.UpdateDomainCmd;
import com.cloud.api.commands.UpdateIPForwardingRuleCmd;
import com.cloud.api.commands.UpdateIsoCmd;
import com.cloud.api.commands.UpdateIsoPermissionsCmd;
import com.cloud.api.commands.UpdateTemplateCmd;
import com.cloud.api.commands.UpdateTemplateOrIsoCmd;
import com.cloud.api.commands.UpdateTemplateOrIsoPermissionsCmd;
import com.cloud.api.commands.UpdateTemplatePermissionsCmd;
import com.cloud.api.commands.UpdateVMGroupCmd;
import com.cloud.api.commands.UploadCustomCertificateCmd;
import com.cloud.api.response.ExtractResponse;
import com.cloud.async.AsyncJobExecutor;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.async.dao.AsyncJobDao;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.certificate.CertificateVO;
import com.cloud.certificate.dao.CertificateDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ConfigurationVO;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterIpAddressDaoImpl;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventState;
import com.cloud.event.EventTypes;
import com.cloud.event.EventUtils;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.info.ConsoleProxyInfo;
import com.cloud.network.FirewallRuleVO;
import com.cloud.network.IPAddressVO;
import com.cloud.network.LoadBalancerVMMapVO;
import com.cloud.network.LoadBalancerVO;
import com.cloud.network.NetworkManager;
import com.cloud.network.RemoteAccessVpnVO;
import com.cloud.network.VpnUserVO;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.RemoteAccessVpnDao;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.security.NetworkGroupManager;
import com.cloud.network.security.NetworkGroupVO;
import com.cloud.network.security.dao.NetworkGroupDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.server.auth.UserAuthenticator;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.DiskTemplateVO;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.LaunchPermissionVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Snapshot.SnapshotType;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.StorageStats;
import com.cloud.storage.Upload;
import com.cloud.storage.Upload.Mode;
import com.cloud.storage.Upload.Type;
import com.cloud.storage.UploadVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.storage.VolumeStats;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.DiskTemplateDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.LaunchPermissionDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.UploadDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateDao.TemplateFilter;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.preallocatedlun.PreallocatedLunVO;
import com.cloud.storage.preallocatedlun.dao.PreallocatedLunDao;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.storage.upload.UploadMonitor;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserAccountVO;
import com.cloud.user.UserContext;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.MacAddress;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.InstanceGroupVMMapVO;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.State;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.InstanceGroupVMMapDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

public class ManagementServerImpl implements ManagementServer {	
    public static final Logger s_logger = Logger.getLogger(ManagementServerImpl.class.getName());

    private final AccountManager _accountMgr;
    private final AgentManager _agentMgr;
    private final ConfigurationManager _configMgr;
    private final FirewallRulesDao _firewallRulesDao;
	private final NetworkGroupDao _networkSecurityGroupDao;
    private final LoadBalancerDao _loadBalancerDao;
    private final IPAddressDao _publicIpAddressDao;
    private final DataCenterIpAddressDaoImpl _privateIpAddressDao;
    private final LoadBalancerVMMapDao _loadBalancerVMMapDao;
    private final DomainRouterDao _routerDao;
    private final ConsoleProxyDao _consoleProxyDao;
    private final ClusterDao _clusterDao;
    private final SecondaryStorageVmDao _secStorageVmDao;
    private final EventDao _eventDao;
    private final DataCenterDao _dcDao;
    private final VlanDao _vlanDao;
    private final AccountVlanMapDao _accountVlanMapDao;
    private final PodVlanMapDao _podVlanMapDao;
    private final HostDao _hostDao;
    private final UserDao _userDao;
    private final UserVmDao _userVmDao;
    private final ConfigurationDao _configDao;
    private final NetworkManager _networkMgr;
    private final UserVmManager _vmMgr;
    private final ConsoleProxyManager _consoleProxyMgr;
    private final SecondaryStorageVmManager _secStorageVmMgr;
    private final ServiceOfferingDao _offeringsDao;
    private final DiskOfferingDao _diskOfferingDao;
    private final VMTemplateDao _templateDao;
    private final LaunchPermissionDao _launchPermissionDao;
    private final DomainDao _domainDao;
    private final AccountDao _accountDao;
    private final ResourceLimitDao _resourceLimitDao;
    private final UserAccountDao _userAccountDao;
    private final AlertDao _alertDao;
    private final CapacityDao _capacityDao;
    private final SnapshotDao _snapshotDao;
    private final SnapshotPolicyDao _snapshotPolicyDao;
    private final GuestOSDao _guestOSDao;
    private final GuestOSCategoryDao _guestOSCategoryDao;
    private final StoragePoolDao _poolDao;
    private final StoragePoolHostDao _poolHostDao;
    private final StorageManager _storageMgr;
    private final UserVmDao _vmDao;

    private final Adapters<UserAuthenticator> _userAuthenticators;
    private final HostPodDao _hostPodDao;
    private final UserStatisticsDao _userStatsDao;
    private final VMInstanceDao _vmInstanceDao;
    private final VolumeDao _volumeDao;
    private final DiskTemplateDao _diskTemplateDao;
    private final AlertManager _alertMgr;
    private final AsyncJobDao _jobDao;
    private final AsyncJobManager _asyncMgr;
    private final TemplateManager _tmpltMgr;
    private final SnapshotManager _snapMgr;
    private final NetworkGroupManager _networkGroupMgr;
    private final int _purgeDelay;
    private final boolean _directAttachNetworkExternalIpAllocator;
    private final PreallocatedLunDao _lunDao;
    private final InstanceGroupDao _vmGroupDao;
    private final InstanceGroupVMMapDao _groupVMMapDao;
    private final UploadMonitor _uploadMonitor;
    private final UploadDao _uploadDao;
    private final CertificateDao _certDao;
    private final RemoteAccessVpnDao _remoteAccessVpnDao;
    private final VpnUserDao _vpnUsersDao;

    private final ScheduledExecutorService _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("AccountChecker"));
    private final ScheduledExecutorService _eventExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("EventChecker"));

    private final StatsCollector _statsCollector;

    private final Map<String, String> _configs;

    private String _domain;

    private final int _routerRamSize;
    private final int _proxyRamSize;
    private final int _ssRamSize;
    private int _maxVolumeSizeInGb;

    private final Map<String, Boolean> _availableIdsMap;

	private boolean _networkGroupsEnabled = false;

    private boolean _isHypervisorSnapshotCapable = false;

    protected ManagementServerImpl() {
        ComponentLocator locator = ComponentLocator.getLocator(Name);
        _lunDao = locator.getDao(PreallocatedLunDao.class);
        _configDao = locator.getDao(ConfigurationDao.class);
        _routerDao = locator.getDao(DomainRouterDao.class);
        _eventDao = locator.getDao(EventDao.class);
        _dcDao = locator.getDao(DataCenterDao.class);
        _vlanDao = locator.getDao(VlanDao.class);
        _accountVlanMapDao = locator.getDao(AccountVlanMapDao.class);
        _podVlanMapDao = locator.getDao(PodVlanMapDao.class);
        _hostDao = locator.getDao(HostDao.class);
        _hostPodDao = locator.getDao(HostPodDao.class);
        _jobDao = locator.getDao(AsyncJobDao.class);
        _clusterDao = locator.getDao(ClusterDao.class);

        _accountMgr = locator.getManager(AccountManager.class);
        _agentMgr = locator.getManager(AgentManager.class);
        _configMgr = locator.getManager(ConfigurationManager.class);
        _networkMgr = locator.getManager(NetworkManager.class);
        _vmMgr = locator.getManager(UserVmManager.class);
        _consoleProxyMgr = locator.getManager(ConsoleProxyManager.class);
        _secStorageVmMgr = locator.getManager(SecondaryStorageVmManager.class);
        _storageMgr = locator.getManager(StorageManager.class);
        _firewallRulesDao = locator.getDao(FirewallRulesDao.class);
        _networkSecurityGroupDao  = locator.getDao(NetworkGroupDao.class);
        _loadBalancerDao = locator.getDao(LoadBalancerDao.class);
        _publicIpAddressDao = locator.getDao(IPAddressDao.class);
        _privateIpAddressDao = locator.getDao(DataCenterIpAddressDaoImpl.class);
        _loadBalancerVMMapDao = locator.getDao(LoadBalancerVMMapDao.class);
        _consoleProxyDao = locator.getDao(ConsoleProxyDao.class);
        _secStorageVmDao = locator.getDao(SecondaryStorageVmDao.class);
        _userDao = locator.getDao(UserDao.class);
        _userVmDao = locator.getDao(UserVmDao.class);
        _offeringsDao = locator.getDao(ServiceOfferingDao.class);
        _diskOfferingDao = locator.getDao(DiskOfferingDao.class);
        _templateDao = locator.getDao(VMTemplateDao.class);
        _launchPermissionDao = locator.getDao(LaunchPermissionDao.class);
        _domainDao = locator.getDao(DomainDao.class);
        _accountDao = locator.getDao(AccountDao.class);
        _resourceLimitDao = locator.getDao(ResourceLimitDao.class);
        _userAccountDao = locator.getDao(UserAccountDao.class);
        _alertDao = locator.getDao(AlertDao.class);
        _capacityDao = locator.getDao(CapacityDao.class);
        _snapshotDao = locator.getDao(SnapshotDao.class);
        _snapshotPolicyDao = locator.getDao(SnapshotPolicyDao.class);
        _guestOSDao = locator.getDao(GuestOSDao.class);
        _guestOSCategoryDao = locator.getDao(GuestOSCategoryDao.class);
        _poolDao = locator.getDao(StoragePoolDao.class);
        _poolHostDao = locator.getDao(StoragePoolHostDao.class);
        _vmDao = locator.getDao(UserVmDao.class);
        _vmGroupDao = locator.getDao(InstanceGroupDao.class);
        _groupVMMapDao = locator.getDao(InstanceGroupVMMapDao.class);
        _uploadDao = locator.getDao(UploadDao.class);
        _certDao = locator.getDao(CertificateDao.class);
        _remoteAccessVpnDao = locator.getDao(RemoteAccessVpnDao.class);
        _vpnUsersDao = locator.getDao(VpnUserDao.class);
        _configs = _configDao.getConfiguration();
        _userStatsDao = locator.getDao(UserStatisticsDao.class);
        _vmInstanceDao = locator.getDao(VMInstanceDao.class);
        _volumeDao = locator.getDao(VolumeDao.class);
        _diskTemplateDao = locator.getDao(DiskTemplateDao.class);
        _alertMgr = locator.getManager(AlertManager.class);
        _asyncMgr = locator.getManager(AsyncJobManager.class);
        _tmpltMgr = locator.getManager(TemplateManager.class);
        _snapMgr = locator.getManager(SnapshotManager.class);
        _networkGroupMgr = locator.getManager(NetworkGroupManager.class);
        _uploadMonitor = locator.getManager(UploadMonitor.class);        
        
        _userAuthenticators = locator.getAdapters(UserAuthenticator.class);
        if (_userAuthenticators == null || !_userAuthenticators.isSet()) {
            s_logger.error("Unable to find an user authenticator.");
        }

        _domain = _configs.get("domain");
        if (_domain == null) {
            _domain = ".myvm.com";
        }
        if (!_domain.startsWith(".")) {
            _domain = "." + _domain;
        }

        String value = _configs.get("account.cleanup.interval");
        int cleanup = NumbersUtil.parseInt(value, 60 * 60 * 24); // 1 hour.

        // Parse the max number of UserVMs and public IPs from server-setup.xml,
        // and set them in the right places
        _routerRamSize = NumbersUtil.parseInt(_configs.get("router.ram.size"),NetworkManager.DEFAULT_ROUTER_VM_RAMSIZE);
        _proxyRamSize = NumbersUtil.parseInt(_configs.get("consoleproxy.ram.size"), ConsoleProxyManager.DEFAULT_PROXY_VM_RAMSIZE);
        _ssRamSize = NumbersUtil.parseInt(_configs.get("secstorage.ram.size"), SecondaryStorageVmManager.DEFAULT_SS_VM_RAMSIZE);

        _directAttachNetworkExternalIpAllocator =
        										Boolean.parseBoolean(_configs.get("direct.attach.network.externalIpAllocator.enabled"));
        
        _statsCollector = StatsCollector.getInstance(_configs);
        _executor.scheduleAtFixedRate(new AccountCleanupTask(), cleanup, cleanup, TimeUnit.SECONDS);

        _purgeDelay = NumbersUtil.parseInt(_configs.get("event.purge.delay"), 0);
        if(_purgeDelay != 0){
            _eventExecutor.scheduleAtFixedRate(new EventPurgeTask(), cleanup, cleanup, TimeUnit.SECONDS);
        }
        
        String[] availableIds = TimeZone.getAvailableIDs();
        _availableIdsMap = new HashMap<String, Boolean>(availableIds.length);
        for (String id: availableIds) {
            _availableIdsMap.put(id, true);
        }
        String enabled =_configDao.getValue("direct.attach.network.groups.enabled");
		if ("true".equalsIgnoreCase(enabled)) {
			_networkGroupsEnabled = true;
		}
		
        String maxVolumeSizeInGbString = _configDao.getValue("max.volume.size.gb");
        int maxVolumeSizeGb = NumbersUtil.parseInt(maxVolumeSizeInGbString, 2000);
        _maxVolumeSizeInGb = maxVolumeSizeGb;
    }
    
    protected Map<String, String> getConfigs() {
        return _configs;
    }

    @Override
    public StorageStats getStorageStatistics(long hostId) {
        return _statsCollector.getStorageStats(hostId);
    }
    
    @Override
    public PreallocatedLunVO registerPreallocatedLun(RegisterPreallocatedLunCmd cmd) {
        Long zoneId = cmd.getZoneId();
        String portal = cmd.getPortal();
        String targetIqn = cmd.getTargetIqn();
        Integer lun = cmd.getLun();
        Long size = cmd.getDiskSize();
        String t = cmd.getTags();

        String[] tags = null;
        if (t != null) {
            tags = t.split(",");
            for (int i = 0; i < tags.length; i++) {
                tags[i] = tags[i].trim();
            }
        } else {
            tags = new String[0];
        }
        
        PreallocatedLunVO vo = new PreallocatedLunVO(zoneId, portal, targetIqn, lun, size);
        return _lunDao.persist(vo, tags);
    }
    
    @Override
    public boolean unregisterPreallocatedLun(DeletePreallocatedLunCmd cmd) throws IllegalArgumentException {
        Long id = cmd.getId();
    	PreallocatedLunVO lun = null;
    	if ((lun = _lunDao.findById(id)) == null) {
    		throw new IllegalArgumentException("Unable to find a LUN with ID " + id);
    	}
    	
    	if (lun.getTaken() != null) {
    		throw new IllegalArgumentException("The LUN is currently in use and cannot be deleted.");
    	}
    	
        return _lunDao.delete(id);
    }

    @Override
    public VolumeStats[] getVolumeStatistics(long[] volIds) {
        return _statsCollector.getVolumeStats(volIds);
    }

    @Override
    public String updateAdminPassword(long userId, String oldPassword, String newPassword) {
        // String old = StringToMD5(oldPassword);
        // User user = getUser(userId);
        // if (old.equals(user.getPassword())) {
        UserVO userVO = _userDao.createForUpdate(userId);
        userVO.setPassword(StringToMD5(newPassword));
        _userDao.update(userId, userVO);
        return newPassword;
        // } else {
        // return null;
        // }
    }

    private String StringToMD5(String string) {
        MessageDigest md5;

        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new CloudRuntimeException("Error", e);
        }

        md5.reset();
        BigInteger pwInt = new BigInteger(1, md5.digest(string.getBytes()));

        // make sure our MD5 hash value is 32 digits long...
        StringBuffer sb = new StringBuffer();
        String pwStr = pwInt.toString(16);
        int padding = 32 - pwStr.length();
        for (int i = 0; i < padding; i++) {
            sb.append('0');
        }
        sb.append(pwStr);
        return sb.toString();
    }

    @Override
    public User getUser(long userId) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Retrieiving user with id: " + userId);
        }

        UserVO user = _userDao.getUser(userId);
        if (user == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find user with id " + userId);
            }
            return null;
        }

        return user;
    }

    @Override
    public User getUser(long userId, boolean active) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Retrieiving user with id: " + userId + " and active = " + active);
        }

        if (active) {
            return _userDao.getUser(userId);
        } else {
            return _userDao.findById(userId);
        }
    }

    @Override
    public UserAccount getUserAccount(String username, Long domainId) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Retrieiving user: " + username + " in domain " + domainId);
        }

        UserAccount userAccount = _userAccountDao.getUserAccount(username, domainId);
        if (userAccount == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find user with name " + username + " in domain " + domainId);
            }
            return null;
        }

        return userAccount;
    }

    private UserAccount getUserAccount(String username, String password, Long domainId) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Attempting to log in user: " + username + " in domain " + domainId);
        }

        UserAccount userAccount = _userAccountDao.getUserAccount(username, domainId);
        if (userAccount == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find user with name " + username + " in domain " + domainId);
            }
            return null;
        }

        if (!userAccount.getState().equals("enabled") || !userAccount.getAccountState().equals("enabled")) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("user " + username + " in domain " + domainId + " is disabled/locked (or account is disabled/locked), returning null");
            }
            return null;
        }

        // We only use the first adapter even if multiple have been
        // configured
        Enumeration<UserAuthenticator> en = _userAuthenticators.enumeration();
        UserAuthenticator authenticator = en.nextElement();
        boolean authenticated = authenticator.authenticate(username, password, domainId);

        if (authenticated) {
        	return userAccount;
        } else {
        	return null;
        }
    }

    @Override
    public Pair<User, Account> findUserByApiKey(String apiKey) {
        return _accountDao.findUserAccountByApiKey(apiKey);
    }

    @Override
    public Account getAccount(long accountId) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Retrieiving account with id: " + accountId);
        }

        AccountVO account = _accountDao.findById(Long.valueOf(accountId));
        if (account == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find account with id " + accountId);
            }
            return null;
        }

        return account;
    }
    
    @Override
    public String[] createApiKeyAndSecretKey(RegisterCmd cmd) {
    	Long userId = cmd.getId();
    	User user = _userDao.findById(userId);

    	if (user == null) {
           throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "unable to find user for id : " + userId);
    	}

    	// generate both an api key and a secret key, update the user table with the keys, return the keys to the user
    	String[] keys = new String[2];
    	keys[0] = createApiKey(userId);
    	keys[1] = createSecretKey(userId);

    	return keys;
    }

    private String createApiKey(Long userId) {
        User user = findUserById(userId);
        try {
            UserVO updatedUser = _userDao.createForUpdate();

            String encodedKey = null;
            Pair<User, Account> userAcct = null;
            int retryLimit = 10;
            do {
                // FIXME: what algorithm should we use for API keys?
                KeyGenerator generator = KeyGenerator.getInstance("HmacSHA1");
                SecretKey key = generator.generateKey();
                encodedKey = Base64.encodeBase64URLSafeString(key.getEncoded());
                userAcct = _accountDao.findUserAccountByApiKey(encodedKey);
                retryLimit--;
            } while ((userAcct != null) && (retryLimit >= 0));

            if (userAcct != null) {
                return null;
            }
            updatedUser.setApiKey(encodedKey);
            _userDao.update(user.getId(), updatedUser);
            return encodedKey;
        } catch (NoSuchAlgorithmException ex) {
            s_logger.error("error generating secret key for user: " + user.getUsername(), ex);
        }
        return null;
    }

    private String createSecretKey(Long userId) {
        User user = findUserById(userId);
        try {
            UserVO updatedUser = _userDao.createForUpdate();

            String encodedKey = null;
            int retryLimit = 10;
            UserVO userBySecretKey = null;
            do {
                KeyGenerator generator = KeyGenerator.getInstance("HmacSHA1");
                SecretKey key = generator.generateKey();
                encodedKey = Base64.encodeBase64URLSafeString(key.getEncoded());
                userBySecretKey = _userDao.findUserBySecretKey(encodedKey);
                retryLimit--;
            } while ((userBySecretKey != null) && (retryLimit >= 0));

            if (userBySecretKey != null) {
                return null;
            }

            updatedUser.setSecretKey(encodedKey);
            _userDao.update(user.getId(), updatedUser);
            return encodedKey;
        } catch (NoSuchAlgorithmException ex) {
            s_logger.error("error generating secret key for user: " + user.getUsername(), ex);
        }
        return null;
    }

    @Override
    public List<IPAddressVO> listPublicIpAddressesBy(Long accountId, boolean allocatedOnly, Long zoneId, Long vlanDbId) {
        SearchCriteria<IPAddressVO> sc = _publicIpAddressDao.createSearchCriteria();

        if (accountId != null)
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        if (zoneId != null)
            sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        if (vlanDbId != null)
            sc.addAnd("vlanDbId", SearchCriteria.Op.EQ, vlanDbId);
        if (allocatedOnly)
            sc.addAnd("allocated", SearchCriteria.Op.NNULL);

        return _publicIpAddressDao.search(sc, null);
    }

    @Override
    public List<DataCenterIpAddressVO> listPrivateIpAddressesBy(Long podId, Long zoneId) {
        if (podId != null && zoneId != null)
            return _privateIpAddressDao.listByPodIdDcId(podId.longValue(), zoneId.longValue());
        else
            return new ArrayList<DataCenterIpAddressVO>();
    }

    @Override
    public String generateRandomPassword() {
    	return PasswordGenerator.generateRandomPassword(6);
    }

    @Override
    public boolean attachISOToVM(long vmId, long userId, long isoId, boolean attach, long startEventId) {
    	UserVmVO vm = _userVmDao.findById(vmId);
    	VMTemplateVO iso = _templateDao.findById(isoId);
    	if(attach){
    		EventUtils.saveStartedEvent(userId, vm.getAccountId(), EventTypes.EVENT_ISO_ATTACH, "Attaching ISO: "+isoId+" to Vm: "+vmId, startEventId);
    	} else {
    		EventUtils.saveStartedEvent(userId, vm.getAccountId(), EventTypes.EVENT_ISO_DETACH, "Detaching ISO: "+isoId+" from Vm: "+vmId, startEventId);
    	}
        boolean success = _vmMgr.attachISOToVM(vmId, isoId, attach);

        if (success) {
            if (attach) {
                vm.setIsoId(iso.getId().longValue());
            } else {
                vm.setIsoId(null);
            }
            _userVmDao.update(vmId, vm);

            if (attach) {
            	EventUtils.saveEvent(userId, vm.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_ISO_ATTACH, "Successfully attached ISO: " + iso.getName() + " to VM with ID: " + vmId,
                        null, startEventId);
            } else {
            	EventUtils.saveEvent(userId, vm.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_ISO_DETACH, "Successfully detached ISO from VM with ID: " + vmId, null, startEventId);
            }
        } else {
            if (attach) {
            	EventUtils.saveEvent(userId, vm.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_ISO_ATTACH, "Failed to attach ISO: " + iso.getName() + " to VM with ID: " + vmId, null, startEventId);
            } else {
            	EventUtils.saveEvent(userId, vm.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_ISO_DETACH, "Failed to detach ISO from VM with ID: " + vmId, null, startEventId);
            }
        }
        return success;
    }

    private boolean validPassword(String password) {
        for (int i = 0; i < password.length(); i++) {
            if (password.charAt(i) == ' ') {
                return false;
            }
        }
        return true;
    }

    private UserVm deployVirtualMachineImpl(long userId, long accountId, long dataCenterId, long serviceOfferingId, VMTemplateVO template, Long diskOfferingId,
            String domain, String password, String displayName, String group, String userData, String [] networkGroups, long startEventId, long size) throws ResourceAllocationException, 
            InsufficientStorageCapacityException, ExecutionException, StorageUnavailableException, ConcurrentOperationException {

    	EventUtils.saveStartedEvent(userId, accountId, EventTypes.EVENT_VM_CREATE, "Deploying Vm", startEventId);

        AccountVO account = _accountDao.findById(accountId);
        DataCenterVO dc = _dcDao.findById(dataCenterId);
        ServiceOfferingVO offering = _offeringsDao.findById(serviceOfferingId);
      
        // Make sure a valid template ID was specified
        if (template == null) {
            throw new InvalidParameterValueException("Please specify a valid template or ISO ID.");
        }
        
        long templateId = template.getId();
        
        byte [] decodedUserData = null;
        if (userData != null) {
        	if (userData.length() >= 2* UserVmManager.MAX_USER_DATA_LENGTH_BYTES) {
        		throw new InvalidParameterValueException("User data is too long");
        	}
        	decodedUserData = org.apache.commons.codec.binary.Base64.decodeBase64(userData.getBytes());
        	if (decodedUserData.length > UserVmManager.MAX_USER_DATA_LENGTH_BYTES){
        		throw new InvalidParameterValueException("User data is too long");
        	}
			
        }

        boolean isIso = Storage.ImageFormat.ISO.equals(template.getFormat());
        DiskOfferingVO diskOffering = _diskOfferingDao.findById(diskOfferingId);
        
        // TODO: Checks such as is the user allowed to use the template and purchase the service offering id.

        if (domain == null) {
            domain = "v" + Long.toHexString(accountId) + _domain;
        }

        // Check that the password was passed in and is valid
        if (!template.getEnablePassword()) {
            password = "saved_password";
        }

        if (password == null || password.equals("") || (!validPassword(password))) {
            throw new InvalidParameterValueException("A valid password for this virtual machine was not provided.");
        }
        List<NetworkGroupVO> networkGroupVOs = new ArrayList<NetworkGroupVO>();
        if (networkGroups != null) {
        	for (String groupName: networkGroups) {
        		NetworkGroupVO networkGroupVO = _networkSecurityGroupDao.findByAccountAndName(accountId, groupName);
        		if (networkGroupVO == null) {
        			throw new InvalidParameterValueException("Network Group " + groupName + " does not exist");
        		}
        		networkGroupVOs.add(networkGroupVO);
        	}
        }
        
        UserStatisticsVO stats = _userStatsDao.findBy(account.getId(), dataCenterId);
        if (stats == null) {
            stats = new UserStatisticsVO(account.getId(), dataCenterId);
            _userStatsDao.persist(stats);
        }
        
    	Long vmId = _vmDao.getNextInSequence(Long.class, "id");
    	
        // check if we are within context of async-execution
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if (asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();

            if (s_logger.isInfoEnabled())
                s_logger.info("DeployVM acquired a new instance " + vmId + ", update async job-" + job.getId() + " progress status");

            _asyncMgr.updateAsyncJobAttachment(job.getId(), "vm_instance", vmId);
            _asyncMgr.updateAsyncJobStatus(job.getId(), BaseCmd.PROGRESS_INSTANCE_CREATED, vmId);
        }

        HashMap<Long, StoragePoolVO> avoids = new HashMap<Long, StoragePoolVO>();

        // Pod allocator now allocate VM based on a reservation style allocation, disable retry here for now
        for (int retry = 0; retry < 1; retry++) {
            String externalIp = null;
            UserVmVO created = null;

            ArrayList<StoragePoolVO> a = new ArrayList<StoragePoolVO>(avoids.values());
            if (_directAttachNetworkExternalIpAllocator) {
            	try {
            		created = _vmMgr.createDirectlyAttachedVMExternal(vmId, userId, account, dc, offering, template, diskOffering, displayName, userData, a, networkGroupVOs, startEventId, size);
            	} catch (ResourceAllocationException rae) {
            		throw rae;
            	}
            } else {
            	if (offering.getGuestIpType() == NetworkOffering.GuestIpType.Virtualized) {
            		try {
            			externalIp = _networkMgr.assignSourceNatIpAddress(account, dc, domain, offering, startEventId, template.getHypervisorType());
            		} catch (ResourceAllocationException rae) {
            			throw rae;
            		}

            		if (externalIp == null) {
            			throw new CloudRuntimeException("Unable to allocate a source nat ip address");
            		}

            		if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Source Nat acquired: " + externalIp);
                    }

                    try {
                        created = _vmMgr.createVirtualMachine(vmId, userId, account, dc, offering, template, diskOffering, displayName, userData, a, startEventId, size);
                    } catch (ResourceAllocationException rae) {
                        throw rae;
                    }
                } else {
                    try {
                        created = _vmMgr.createDirectlyAttachedVM(vmId, userId, account, dc, offering, template, diskOffering, displayName, userData, a, networkGroupVOs, startEventId, size);
                    } catch (ResourceAllocationException rae) {
                        throw rae;
                    }
                }
            }

            //assign vm to the group
            try{
                if (group != null) {
                boolean addToGroup = _vmMgr.addInstanceToGroup(Long.valueOf(vmId), group);
                if (!addToGroup) {
                    throw new CloudRuntimeException("Unable to assing Vm to the group " + group);
                }
                }
            } catch (Exception ex) {
                throw new CloudRuntimeException("Unable to assing Vm to the group " + group);
            }
            
            
            if (created == null) {
                throw new CloudRuntimeException("Unable to create VM for account (" + accountId + "): " + account.getAccountName());
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM created: " + created.getId() + "-" + created.getHostName());
            }
            boolean executionExceptionFlag = false;
            boolean storageUnavailableExceptionFlag = false;
            boolean concurrentOperationExceptionFlag = false;
            String executionExceptionMsg= "";
            String storageUnavailableExceptionMsg = "";
            String concurrentOperationExceptionMsg = "";
            UserVmVO started = null;

            if (isIso)
            {
                Pair<String, String> isoPath = _storageMgr.getAbsoluteIsoPath(templateId, dataCenterId);
                assert(isoPath != null);
                try
                {
                    started = _vmMgr.startVirtualMachine(userId, created.getId(), password, isoPath.first(), startEventId);
                }
                catch (ExecutionException e)
                {
                    executionExceptionFlag = true;
                    executionExceptionMsg = e.getMessage();
                }
                catch (StorageUnavailableException e)
                {
                    storageUnavailableExceptionFlag = true;
                    storageUnavailableExceptionMsg = e.getMessage();
                }
                catch (ConcurrentOperationException e)
                {
                    concurrentOperationExceptionFlag = true;
                    concurrentOperationExceptionMsg = e.getMessage();
                }
            }
            else
            {
                try
                {
                    started = _vmMgr.startVirtualMachine(userId, created.getId(), password, null, startEventId);
                }
                catch (ExecutionException e)
                {
                    executionExceptionFlag = true;
                    executionExceptionMsg = e.getMessage();
                }
                catch (StorageUnavailableException e)
                {
                        storageUnavailableExceptionFlag = true;
                        storageUnavailableExceptionMsg = e.getMessage();
                }
                catch (ConcurrentOperationException e)
                {
                        concurrentOperationExceptionFlag = true;
                        concurrentOperationExceptionMsg = e.getMessage();
                }
            }

            if (started == null) {
                List<Pair<VolumeVO, StoragePoolVO>> disks = _storageMgr.isStoredOn(created);
                // NOTE: We now destroy a VM if the deploy process fails at any step. We now
                // have a lazy delete so there is still some time to figure out what's wrong.
                _vmMgr.destroyVirtualMachine(userId, created.getId());

                boolean retryCreate = true;
                for (Pair<VolumeVO, StoragePoolVO> disk : disks) {
                    if (disk.second().isLocal()) {
                        avoids.put(disk.second().getId(), disk.second());
                    } else {
                        retryCreate = false;
                    }
                }

                if (retryCreate) {
                    continue;
                } else if(executionExceptionFlag){
                    throw new ExecutionException(executionExceptionMsg);
                } else if (storageUnavailableExceptionFlag){
                    throw new StorageUnavailableException(storageUnavailableExceptionMsg);
                }else if (concurrentOperationExceptionFlag){
                    throw new ConcurrentOperationException(concurrentOperationExceptionMsg);
                }
                else{
                    throw new CloudRuntimeException("Unable to start the VM " + created.getId() + "-" + created.getHostName());
                }
                
            } else {
                if (isIso) {
                    started.setIsoId(templateId);
                    _userVmDao.update(started.getId(), started);
                    started = _userVmDao.findById(started.getId());
                }

                try {
                    _configMgr.associateIpAddressListToAccount(userId, accountId, dc.getId(),null);                                                         
                } catch (InsufficientAddressCapacityException e) {
                    s_logger.debug("Unable to assign public IP address pool: " +e.getMessage());                    
                }
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM started: " + started.getId() + "-" + started.getHostName());
            }
            return started;
        }

        return null;
    }

    @Override
    public UserVm deployVirtualMachine(DeployVMCmd cmd) throws ResourceAllocationException,
                                                               InsufficientStorageCapacityException, ExecutionException,
                                                               StorageUnavailableException, ConcurrentOperationException {
        Account ctxAccount = UserContext.current().getAccount();
        Long userId = UserContext.current().getUserId();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long accountId = null;
        long dataCenterId = cmd.getZoneId();
        long serviceOfferingId = cmd.getServiceOfferingId();
        long templateId = cmd.getTemplateId();
        Long diskOfferingId = cmd.getDiskOfferingId();
        String domain = null; // FIXME:  this was hardcoded to null in DeployVMCmd in the old framework, do we need it?
        String password = generateRandomPassword();
        String displayName = cmd.getDisplayName();
        String group = cmd.getGroup();
        String userData = cmd.getUserData();
        String[] networkGroups = null;
        Long sizeObj = cmd.getSize();
        long size = (sizeObj == null) ? 0 : sizeObj;

        DataCenterVO dc = _dcDao.findById(dataCenterId);
        if (dc == null) {
            throw new InvalidParameterValueException("Unable to find zone: " + dataCenterId);
        }
                
        if ((ctxAccount == null) || isAdmin(ctxAccount.getType())) {
            if (domainId != null) {
                if ((ctxAccount != null) && !_domainDao.isChildDomain(ctxAccount.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Failed to deploy VM, invalid domain id (" + domainId + ") given.");
                }
                if (accountName != null) {
                    Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                    if (userAccount == null) {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                    accountId = userAccount.getId();
                }
            } else {
                accountId = ((ctxAccount != null) ? ctxAccount.getId() : null);
            }
        } else {
            accountId = ctxAccount.getId();
        }

        if (accountId == null) {
            throw new InvalidParameterValueException("No valid account specified for deploying a virtual machine.");
        }

        if(domainId == null){
        	domainId = dc.getDomainId(); //get the domain id from zone
        	
        	if(domainId == null){
        		//do nothing (public zone case)
        	}
        	else{
        		//check if this account has the permission to deploy a vm in this domain
        		if(ctxAccount != null){
        			if((ctxAccount.getType() == Account.ACCOUNT_TYPE_NORMAL) || ctxAccount.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN){
        				if(domainId == ctxAccount.getDomainId()){
        					//user in same domain as dedicated zone
        				}
        				else if ((!_domainDao.isChildDomain(domainId,ctxAccount.getDomainId()))){
        					//may need to revisit domain admin case for leaves
        					throw new PermissionDeniedException("Failed to deploy VM, user does not have permission to deploy a vm within this dedicated private zone under domain id:"+domainId);
        				}
        			}
        		}
        	}
        }

        List<String> netGrpList = cmd.getNetworkGroupList();
        if ((netGrpList != null) && !netGrpList.isEmpty()) {
            networkGroups = netGrpList.toArray(new String[netGrpList.size()]);
        }

    	AccountVO account = _accountDao.findById(accountId);
        if (account == null) {
            throw new InvalidParameterValueException("Unable to find account: " + accountId);
        }

        ServiceOfferingVO offering = _offeringsDao.findById(serviceOfferingId);
        if (offering == null) {
            throw new InvalidParameterValueException("Unable to find service offering: " + serviceOfferingId);
        }

        VMTemplateVO template = _templateDao.findById(templateId);
        // Make sure a valid template ID was specified
        if (template == null) {
            throw new InvalidParameterValueException("Please specify a valid template or ISO ID.");
        }

        boolean isIso = Storage.ImageFormat.ISO.equals(template.getFormat());
        
        if (isIso && !template.isBootable()) {
        	throw new InvalidParameterValueException("Please specify a bootable ISO.");
        }

        // If the template represents an ISO, a disk offering must be passed in, and will be used to create the root disk
        // Else, a disk offering is optional, and if present will be used to create the data disk
        DiskOfferingVO diskOffering = null;

        if (diskOfferingId != null) {
        	diskOffering = _diskOfferingDao.findById(diskOfferingId);
        }
 
        if (isIso && diskOffering == null) {
        	throw new InvalidParameterValueException("Please specify a valid disk offering ID.");
        }
        
        if (isIso) {
        	/*iso template doesn;t have hypervisor type, temporarily set it's type as user specified, pass it to storage allocator */
        	template.setHypervisorType(HypervisorType.getType(cmd.getHypervisor()));
        }
        
        //if it is a custom disk offering,AND the size passed in here is <= 0; error out
        if(diskOffering != null && diskOffering.isCustomized() && size <= 0){
        	throw new InvalidParameterValueException("Please specify a valid disk size for VM creation; custom disk offering has no size set");
        }
        
        if(diskOffering != null && diskOffering.isCustomized() && size > _maxVolumeSizeInGb){
        	throw new InvalidParameterValueException("Please specify a valid disk size for VM creation; custom disk offering max size is:"+_maxVolumeSizeInGb);
        }
        
        // validate that the template is usable by the account
        if (!template.isPublicTemplate()) {
            Long templateOwner = template.getAccountId();
            if (!BaseCmd.isAdmin(account.getType()) && ((templateOwner == null) || (templateOwner.longValue() != accountId))) {
                // since the current account is not the owner of the template, check the launch permissions table to see if the
                // account can launch a VM from this template
                LaunchPermissionVO permission = _launchPermissionDao.findByTemplateAndAccount(templateId, account.getId());
                if (permission == null) {
                    throw new PermissionDeniedException("Account " + account.getAccountName() + " does not have permission to launch instances from template " + template.getName());
                }
            }
        }
        
      

        byte [] decodedUserData = null;
        if (userData != null) {
        	if (userData.length() >= 2* UserVmManager.MAX_USER_DATA_LENGTH_BYTES) {
        		throw new InvalidParameterValueException("User data is too long");
        	}
        	decodedUserData = org.apache.commons.codec.binary.Base64.decodeBase64(userData.getBytes());
        	if (decodedUserData.length > UserVmManager.MAX_USER_DATA_LENGTH_BYTES){
        		throw new InvalidParameterValueException("User data is too long");
        	}
        	if (decodedUserData.length < 1) {
        		throw new InvalidParameterValueException("User data is too short");
        	}
			
        }
        if (offering.getGuestIpType() != NetworkOffering.GuestIpType.Virtualized) {
        	_networkGroupMgr.createDefaultNetworkGroup(accountId);
    	}
        
        if (networkGroups != null) {
        	if (offering.getGuestIpType() == NetworkOffering.GuestIpType.Virtualized) {
        		throw new InvalidParameterValueException("Network groups are not compatible with service offering " + offering.getName());
        	}
        	Set<String> nameSet = new HashSet<String>(); //handle duplicate names -- allowed
        	nameSet.addAll(Arrays.asList(networkGroups));
        	nameSet.add(NetworkGroupManager.DEFAULT_GROUP_NAME);
        	networkGroups = nameSet.toArray(new String[nameSet.size()]);
        	List<NetworkGroupVO> networkGroupVOs = _networkSecurityGroupDao.findByAccountAndNames(accountId, networkGroups);
        	if (networkGroupVOs.size() != nameSet.size()) {
        		throw new InvalidParameterValueException("Some network group names do not exist");
        	}
        } else { //create a default group if necessary
        	if (offering.getGuestIpType() != NetworkOffering.GuestIpType.Virtualized && _networkGroupsEnabled) {
        		networkGroups = new String[]{NetworkGroupManager.DEFAULT_GROUP_NAME};
        	}
        }

        Long eventId = cmd.getStartEventId();
        try {
            return deployVirtualMachineImpl(userId, accountId, dataCenterId, serviceOfferingId, template, diskOfferingId, domain, password, displayName, group, userData, networkGroups, eventId, size);
        } catch (ResourceAllocationException e) {
            if(s_logger.isDebugEnabled())
                s_logger.debug("Unable to deploy VM: " + e.getMessage());
            EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_CREATE, "Unable to deploy VM: VM_INSUFFICIENT_CAPACITY", null, eventId);
            throw e;
        } catch (ExecutionException e) {
            if(s_logger.isDebugEnabled())
                s_logger.debug("Unable to deploy VM: " + e.getMessage());
            EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_CREATE, "Unable to deploy VM: VM_HOST_LICENSE_EXPIRED", null, eventId);
            throw e;
        } catch (InvalidParameterValueException e) {
            if(s_logger.isDebugEnabled())
                s_logger.debug("Unable to deploy VM: " + e.getMessage());
            EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_CREATE, "Unable to deploy VM: VM_INVALID_PARAM_ERROR", null, eventId);
            throw e;
        } catch (InsufficientStorageCapacityException e) {
            if(s_logger.isDebugEnabled())
                s_logger.debug("Unable to deploy VM: " + e.getMessage());
            EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_CREATE, "Unable to deploy VM: VM_INSUFFICIENT_CAPACITY", null, eventId);
            throw e;
        } catch (PermissionDeniedException e) {
            if(s_logger.isDebugEnabled())
                s_logger.debug("Unable to deploy VM: " + e.getMessage());
            EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_CREATE, "Unable to deploy VM: ACCOUNT_ERROR", null, eventId);
            throw e;
        } catch (ConcurrentOperationException e) {
            if(s_logger.isDebugEnabled())
                s_logger.debug("Unable to deploy VM: " + e.getMessage());
            EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_CREATE, "Unable to deploy VM: INTERNAL_ERROR", null, eventId);
            throw e;
        } catch(Exception e) {
            s_logger.warn("Unable to deploy VM : " + e.getMessage(), e);
            EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_CREATE, "Unable to deploy VM: INTERNAL_ERROR", null, eventId);
            throw new CloudRuntimeException("Unable to deploy VM : " + e.getMessage());
        }
    }

    @Override
    public DomainRouterVO findDomainRouterBy(long accountId, long dataCenterId) {
        return _routerDao.findBy(accountId, dataCenterId);
    }

    @Override
    public DomainRouterVO findDomainRouterById(long domainRouterId) {
        return _routerDao.findById(domainRouterId);
    }

    @Override
    public List<DataCenterVO> listDataCenters(ListZonesByCmd cmd) {   	
        Account account = UserContext.current().getAccount();    	
        List<DataCenterVO> dcs = null;
    	Long domainId = cmd.getDomainId();
    	if(domainId != null){
    		//for domainId != null
    		//right now, we made the decision to only list zones associated with this domain
    		dcs  = _dcDao.findZonesByDomainId(domainId); //private zones
    	}
    	else if((account == null || account.getType() ==  Account.ACCOUNT_TYPE_ADMIN)){
    		dcs = _dcDao.listAll(); //all zones
    	}else if(account.getType() ==  Account.ACCOUNT_TYPE_NORMAL){
    		//it was decided to return all zones for the user's domain, and everything above till root
    		//list all zones belonging to this domain, and all of its parents
    		//check the parent, if not null, add zones for that parent to list
    		dcs = new ArrayList<DataCenterVO>();
    		DomainVO domainRecord = _domainDao.findById(account.getDomainId());
    		if(domainRecord != null)
    		{
    			while(true){
    				dcs.addAll(_dcDao.findZonesByDomainId(domainRecord.getId()));
    				if(domainRecord.getParent() != null)
    					domainRecord = _domainDao.findById(domainRecord.getParent());
    				else
    					break;
    			}
    		}
    		//add all public zones too
    		dcs.addAll(_dcDao.listPublicZones());    		
    	}else if(account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN){
    		//it was decided to return all zones for the domain admin, and everything above till root
    		dcs = new ArrayList<DataCenterVO>();
    		DomainVO domainRecord = _domainDao.findById(account.getDomainId());
    		//this covers path till root
    		if(domainRecord != null)
    		{
    			DomainVO localRecord = domainRecord;
    			while(true){
    				dcs.addAll(_dcDao.findZonesByDomainId(localRecord.getId()));
    				if(localRecord.getParent() != null)
    					localRecord = _domainDao.findById(localRecord.getParent());
    				else
    					break;
    			}
    		}
    		//this covers till leaf
    		if(domainRecord != null){
    			DomainVO localParent = domainRecord;
    			DomainVO immediateChild = null;
    			while(true){
    				//find immediate child domain
    				immediateChild = _domainDao.findImmediateChildForParent(localParent.getId());
    				if(immediateChild != null){
    					dcs.addAll(_dcDao.findZonesByDomainId(immediateChild.getId()));
    					localParent = immediateChild;//traverse down the list
    				}else{
    					break;
    				}
    			}
    		}   		
    		//add all public zones too
    		dcs.addAll(_dcDao.listPublicZones());
    	}

        Boolean available = cmd.isAvailable();
        if (account != null) {
            if ((available != null) && Boolean.FALSE.equals(available)) {
                List<DomainRouterVO> routers = _routerDao.listBy(account.getId());
                for (Iterator<DataCenterVO> iter = dcs.iterator(); iter.hasNext();) {
                    DataCenterVO dc = iter.next();
                    boolean found = false;
                    for (DomainRouterVO router : routers) {
                        if (dc.getId() == router.getDataCenterId()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        iter.remove();
                }
            }
        }

        return dcs;
    }

    @Override
    public HostVO getHostBy(long hostId) {
        return _hostDao.findById(hostId);
    }

    @Override
    public long getId() {
        return MacAddress.getMacAddress().toLong();
    }

    protected void checkPortParameters(String publicPort, String privatePort, String privateIp, String proto) throws InvalidParameterValueException {

        if (!NetUtils.isValidPort(publicPort)) {
            throw new InvalidParameterValueException("publicPort is an invalid value");
        }
        if (!NetUtils.isValidPort(privatePort)) {
            throw new InvalidParameterValueException("privatePort is an invalid value");
        }

//        s_logger.debug("Checking if " + privateIp + " is a valid private IP address. Guest IP address is: " + _configs.get("guest.ip.network"));
//
//        if (!NetUtils.isValidPrivateIp(privateIp, _configs.get("guest.ip.network"))) {
//            throw new InvalidParameterValueException("Invalid private ip address");
//        }
        if (!NetUtils.isValidProto(proto)) {
            throw new InvalidParameterValueException("Invalid protocol");
        }
    }
   


    @Override
    public List<EventVO> getEvents(long userId, long accountId, Long domainId, String type, String level, Date startDate, Date endDate) {
        SearchCriteria<EventVO> sc = _eventDao.createSearchCriteria();
        if (userId > 0) {
            sc.addAnd("userId", SearchCriteria.Op.EQ, userId);
        }
        if (accountId > 0) {
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        }
        if (domainId != null) {
            sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        }
        if (type != null) {
            sc.addAnd("type", SearchCriteria.Op.EQ, type);
        }
        if (level != null) {
            sc.addAnd("level", SearchCriteria.Op.EQ, level);
        }
        if (startDate != null && endDate != null) {
            startDate = massageDate(startDate, 0, 0, 0);
            endDate = massageDate(endDate, 23, 59, 59);
            sc.addAnd("createDate", SearchCriteria.Op.BETWEEN, startDate, endDate);
        } else if (startDate != null) {
            startDate = massageDate(startDate, 0, 0, 0);
            sc.addAnd("createDate", SearchCriteria.Op.GTEQ, startDate);
        } else if (endDate != null) {
            endDate = massageDate(endDate, 23, 59, 59);
            sc.addAnd("createDate", SearchCriteria.Op.LTEQ, endDate);
        }

        return _eventDao.search(sc, null);
    }

    private Date massageDate(Date date, int hourOfDay, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        return cal.getTime();
    }

    @Override
    public List<UserAccountVO> searchForUsers(ListUsersCmd cmd) throws PermissionDeniedException {
        Account account = UserContext.current().getAccount();
        Long domainId = cmd.getDomainId();
        if (domainId != null) {
            if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                throw new PermissionDeniedException("Invalid domain id (" + domainId + ") given, unable to list users.");
            }
        } else {
            // default domainId to the admin's domain
            domainId = ((account == null) ? DomainVO.ROOT_DOMAIN : account.getDomainId());
        }

        Filter searchFilter = new Filter(UserAccountVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object id = cmd.getId();
        Object username = cmd.getUsername();
        Object type = cmd.getAccountType();
        Object accountName = cmd.getAccountName();
        Object state = cmd.getState();
        Object keyword = cmd.getKeyword();

        SearchBuilder<UserAccountVO> sb = _userAccountDao.createSearchBuilder();
        sb.and("username", sb.entity().getUsername(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        sb.and("accountName", sb.entity().getAccountName(), SearchCriteria.Op.LIKE);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);

        if ((accountName == null) && (domainId != null)) {
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<UserAccountVO> sc = sb.create();
        if (keyword != null) {
            SearchCriteria<UserAccountVO> ssc = _userAccountDao.createSearchCriteria();
            ssc.addOr("username", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("firstname", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("lastname", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("email", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("accountName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("type", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("accountState", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("username", SearchCriteria.Op.SC, ssc);
        }

        if (username != null) {
            sc.setParameters("username", "%" + username + "%");
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (type != null) {
            sc.setParameters("type", type);
        }

        if (accountName != null) {
            sc.setParameters("accountName", "%" + accountName + "%");
            if (domainId != null) {
                sc.setParameters("domainId", domainId);
            }
        } else if (domainId != null) {
            DomainVO domainVO = _domainDao.findById(domainId);
            sc.setJoinParameters("domainSearch", "path", domainVO.getPath() + "%");
        }

        if (state != null) {
            sc.setParameters("state", state);
        }

        return _userAccountDao.search(sc, searchFilter);
    }

    @Override
    public List<ServiceOfferingVO> searchForServiceOfferings(ListServiceOfferingsCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Filter searchFilter = new Filter(ServiceOfferingVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<ServiceOfferingVO> sc = _offeringsDao.createSearchCriteria();

        Object name = cmd.getServiceOfferingName();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Long vmId = cmd.getVirtualMachineId();

        if (keyword != null) {
            SearchCriteria<ServiceOfferingVO> ssc = _offeringsDao.createSearchCriteria();
            ssc.addOr("displayText", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        } else if (vmId != null) {
            Account account = UserContext.current().getAccount();

            UserVmVO vmInstance = _userVmDao.findById(vmId);
            if ((vmInstance == null) || (vmInstance.getRemoved() != null)) {
                throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
            }
            if ((account != null) && !isAdmin(account.getType())) {
                if (account.getId() != vmInstance.getAccountId()) {
                    throw new PermissionDeniedException("unable to find a virtual machine with id " + vmId + " for this account");
                }
            }

            ServiceOfferingVO offering = _offeringsDao.findById(vmInstance.getServiceOfferingId());
            sc.addAnd("id", SearchCriteria.Op.NEQ, offering.getId());
            
            // Only return offerings with the same Guest IP type and storage pool preference
            sc.addAnd("guestIpType", SearchCriteria.Op.EQ, offering.getGuestIpType());
            sc.addAnd("useLocalStorage", SearchCriteria.Op.EQ, offering.getUseLocalStorage());
        }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }
        sc.addAnd("systemUse", SearchCriteria.Op.EQ, false);

        return _offeringsDao.search(sc, searchFilter);
    }

    @Override
    public List<ClusterVO> searchForClusters(ListClustersCmd cmd) {
        Filter searchFilter = new Filter(ClusterVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<ClusterVO> sc = _clusterDao.createSearchCriteria();

        Object id = cmd.getId();
        Object name = cmd.getClusterName();
        Object podId = cmd.getPodId();
        Object zoneId = cmd.getZoneId();

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }
        
        if (podId != null) {
        	sc.addAnd("podId", SearchCriteria.Op.EQ, podId);
        }
        
        if (zoneId != null) {
        	sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        }

        return _clusterDao.search(sc, searchFilter);
    }

    @Override
    public List<HostVO> searchForServers(ListHostsCmd cmd) {
        Object name = cmd.getHostName();
        Object type = cmd.getType();
        Object state = cmd.getState();
        Object zone = cmd.getZoneId();
        Object pod = cmd.getPodId();
        Object cluster = cmd.getClusterId();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();

        return searchForServers(cmd.getStartIndex(), cmd.getPageSizeVal(), name, type, state, zone, pod, cluster, id, keyword);
    }

    private List<HostVO> searchForServers(Long startIndex, Long pageSize, Object name, Object type, Object state, Object zone, Object pod, Object cluster, Object id, Object keyword) {
        Filter searchFilter = new Filter(HostVO.class, "id", Boolean.TRUE, startIndex, pageSize);
        SearchCriteria<HostVO> sc = _hostDao.createSearchCriteria();

        if (keyword != null) {
            SearchCriteria<HostVO> ssc = _hostDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("status", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("type", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }
        if (type != null) {
            sc.addAnd("type", SearchCriteria.Op.EQ, type);
        }
        if (state != null) {
            sc.addAnd("status", SearchCriteria.Op.EQ, state);
        }
        if (zone != null) {
            sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zone);
        }
        if (pod != null) {
            sc.addAnd("podId", SearchCriteria.Op.EQ, pod);
        }
        if (cluster != null) {
            sc.addAnd("clusterId", SearchCriteria.Op.EQ, cluster);
        }

        return _hostDao.search(sc, searchFilter);
    }

    @Override
    public List<HostPodVO> searchForPods(ListPodsByCmd cmd) {
        Filter searchFilter = new Filter(HostPodVO.class, "dataCenterId", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<HostPodVO> sc = _hostPodDao.createSearchCriteria();

        String podName = cmd.getPodName();
        Long id = cmd.getId();
        Long zoneId = cmd.getZoneId();
        Object keyword = cmd.getKeyword();

        if (keyword != null) {
            SearchCriteria<HostPodVO> ssc = _hostPodDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (podName != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + podName + "%");
        }

        if (zoneId != null) {
            sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        }

        return _hostPodDao.search(sc, searchFilter);
    }

    @Override
    public List<DataCenterVO> searchForZones(Criteria c) {
        Long dataCenterId = (Long) c.getCriteria(Criteria.DATACENTERID);

        if (dataCenterId != null) {
            DataCenterVO dc = _dcDao.findById(dataCenterId);
            List<DataCenterVO> datacenters = new ArrayList<DataCenterVO>();
            datacenters.add(dc);
            return datacenters;
        }

        Filter searchFilter = new Filter(DataCenterVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria<DataCenterVO> sc = _dcDao.createSearchCriteria();

        String zoneName = (String) c.getCriteria(Criteria.ZONENAME);

        if (zoneName != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + zoneName + "%");
        }

        return _dcDao.search(sc, searchFilter);
    }

    @Override
    public List<VlanVO> searchForVlans(ListVlanIpRangesCmd cmd) throws InvalidParameterValueException {
        // If an account name and domain ID are specified, look up the account
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long accountId = null;
        if (accountName != null && domainId != null) {
            Account account = _accountDao.findActiveAccount(accountName, domainId);
            if (account == null) {
                throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
            } else {
                accountId = account.getId();
            }
        } 

        Filter searchFilter = new Filter(VlanVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object id = cmd.getId();
        Object vlan = cmd.getVlan();
        Object dataCenterId = cmd.getZoneId();
        Object podId = cmd.getPodId();
        Object keyword = cmd.getKeyword();

        SearchBuilder<VlanVO> sb = _vlanDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("vlan", sb.entity().getVlanId(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
       
        if (accountId != null) {
        	SearchBuilder<AccountVlanMapVO> accountVlanMapSearch = _accountVlanMapDao.createSearchBuilder();
        	accountVlanMapSearch.and("accountId", accountVlanMapSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        	sb.join("accountVlanMapSearch", accountVlanMapSearch, sb.entity().getId(), accountVlanMapSearch.entity().getVlanDbId(), JoinBuilder.JoinType.INNER);
        }
        
        if (podId != null) {
        	SearchBuilder<PodVlanMapVO> podVlanMapSearch = _podVlanMapDao.createSearchBuilder();
        	podVlanMapSearch.and("podId", podVlanMapSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        	sb.join("podVlanMapSearch", podVlanMapSearch, sb.entity().getId(), podVlanMapSearch.entity().getVlanDbId(), JoinBuilder.JoinType.INNER);
        }
        
        SearchCriteria<VlanVO> sc = sb.create();
        if (keyword != null) {
            SearchCriteria<VlanVO> ssc = _vlanDao.createSearchCriteria();
            ssc.addOr("vlanId", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("ipRange", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("vlanId", SearchCriteria.Op.SC, ssc);
        } else {
        	if (id != null) {
            	sc.setParameters("id", id);
        	}
        	
        	if (vlan != null) {
        		sc.setParameters("vlan", vlan);
        	}
        
        	if (dataCenterId != null) {
            	sc.setParameters("dataCenterId", dataCenterId);
        	}
        	
        	if (accountId != null) {
        		sc.setJoinParameters("accountVlanMapSearch", "accountId", accountId);
        	}
        	
        	if (podId != null) {
        		sc.setJoinParameters("podVlanMapSearch", "podId", podId);
        	}
        }

        return _vlanDao.search(sc, searchFilter);
    }
    
    @Override
    public Long getPodIdForVlan(long vlanDbId) {
    	List<PodVlanMapVO> podVlanMaps = _podVlanMapDao.listPodVlanMapsByVlan(vlanDbId);
    	if (podVlanMaps.isEmpty()) {
    		return null;
    	} else {
    		return podVlanMaps.get(0).getPodId();
    	}
    }
    
    @Override
    public List<ConfigurationVO> searchForConfigurations(ListCfgsByCmd cmd) {
        Filter searchFilter = new Filter(ConfigurationVO.class, "name", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<ConfigurationVO> sc = _configDao.createSearchCriteria();

        Object name = cmd.getConfigName();
        Object category = cmd.getCategory();
        Object keyword = cmd.getKeyword();

        if (keyword != null) {
            SearchCriteria<ConfigurationVO> ssc = _configDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("instance", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("component", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("category", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("value", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }

        if (category != null) {
            sc.addAnd("category", SearchCriteria.Op.EQ, category);
        }

        // hidden configurations are not displayed using the search API
        sc.addAnd("category", SearchCriteria.Op.NEQ, "Hidden");

        return _configDao.search(sc, searchFilter);
    }

    @Override
    public List<HostVO> searchForAlertServers(Criteria c) {
        Filter searchFilter = new Filter(HostVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria<HostVO> sc = _hostDao.createSearchCriteria();

        Object[] states = (Object[]) c.getCriteria(Criteria.STATE);

        if (states != null) {
            sc.addAnd("status", SearchCriteria.Op.IN, states);
        }

        return _hostDao.search(sc, searchFilter);
    }

    @Override
    public List<VMTemplateVO> searchForTemplates(Criteria c) {
        Filter searchFilter = new Filter(VMTemplateVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());

        Object name = c.getCriteria(Criteria.NAME);
        Object isPublic = c.getCriteria(Criteria.ISPUBLIC);
        Object id = c.getCriteria(Criteria.ID);
        Object keyword = c.getCriteria(Criteria.KEYWORD);
        Long creator = (Long) c.getCriteria(Criteria.CREATED_BY);

        SearchBuilder<VMTemplateVO> sb = _templateDao.createSearchBuilder();
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("publicTemplate", sb.entity().isPublicTemplate(), SearchCriteria.Op.EQ);
        sb.and("format", sb.entity().getFormat(), SearchCriteria.Op.NEQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        
        SearchCriteria<VMTemplateVO> sc = sb.create();
        
        if (keyword != null) {
            SearchCriteria<VMTemplateVO> ssc = _templateDao.createSearchCriteria();
            ssc.addOr("displayName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("group", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("instanceName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }
        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }

        if (isPublic != null) {
            sc.setParameters("publicTemplate", isPublic);
        }
        if (creator != null) {
            sc.setParameters("accountId", creator);
        }

        sc.setParameters("format", ImageFormat.ISO);

        return _templateDao.search(sc, searchFilter);
    }

    @Override
    public List<VMTemplateVO> listIsos(ListIsosCmd cmd) throws IllegalArgumentException, InvalidParameterValueException {
        TemplateFilter isoFilter = TemplateFilter.valueOf(cmd.getIsoFilter());
        Long accountId = null;
        Account account = UserContext.current().getAccount();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        if ((account == null) || (account.getType() == Account.ACCOUNT_TYPE_ADMIN)) {
            // validate domainId before proceeding
            if ((domainId != null) && (accountName != null)) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new InvalidParameterValueException("Invalid domain id (" + domainId + ") given, unable to list events.");
                }

                Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                if (userAccount != null) {
                    accountId = userAccount.getId();
                } else {
                    throw new InvalidParameterValueException("Failed to list ISOs.  Unable to find account " + accountName + " in domain " + domainId);
                }
            } else if (account != null) {
                accountId = account.getId();
            }
        } else {
            accountId = account.getId();
        }

        HypervisorType hypervisorType = HypervisorType.getType(cmd.getHypervisor());
        return listTemplates(cmd.getId(), cmd.getIsoName(), cmd.getKeyword(), isoFilter, true, cmd.isBootable(), accountId, cmd.getPageSizeVal().intValue(), cmd.getStartIndex(), cmd.getZoneId(), hypervisorType);
    }

    @Override
    public List<VMTemplateVO> listTemplates(ListTemplatesCmd cmd) throws IllegalArgumentException, InvalidParameterValueException {
        TemplateFilter templateFilter = TemplateFilter.valueOf(cmd.getTemplateFilter());
        Long accountId = null;
        Account account = UserContext.current().getAccount();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        if ((account == null) || (account.getType() == Account.ACCOUNT_TYPE_ADMIN)) {
            // validate domainId before proceeding
            if ((domainId != null) && (accountName != null)) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new InvalidParameterValueException("Invalid domain id (" + domainId + ") given, unable to list events.");
                }

                Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                if (userAccount != null) {
                    accountId = userAccount.getId();
                } else {
                    throw new InvalidParameterValueException("Failed to list ISOs.  Unable to find account " + accountName + " in domain " + domainId);
                }
            } else if (account != null) {
                accountId = account.getId();
            }
        } else {
            accountId = account.getId();
        }

        HypervisorType hypervisorType = HypervisorType.getType(cmd.getHypervisor());
        return listTemplates(cmd.getId(), cmd.getTemplateName(), cmd.getKeyword(), templateFilter, false, null, accountId, cmd.getPageSizeVal().intValue(), cmd.getStartIndex(), cmd.getZoneId(), hypervisorType);
    }

    private List<VMTemplateVO> listTemplates(Long templateId, String name, String keyword, TemplateFilter templateFilter, boolean isIso, Boolean bootable, Long accountId, Integer pageSize, Long startIndex, Long zoneId, HypervisorType hyperType) throws InvalidParameterValueException {
        VMTemplateVO template = null;
    	if (templateId != null) {
    		template = _templateDao.findById(templateId);
    		if (template == null) {
    			throw new InvalidParameterValueException("Please specify a valid template ID.");
    		}// If ISO requested then it should be ISO.
    		if (isIso && template.getFormat() != ImageFormat.ISO){
    			s_logger.error("Template Id " + templateId + " is not an ISO");
    			throw new InvalidParameterValueException("Template Id " + templateId + " is not an ISO");
    		}// If ISO not requested then it shouldn't be an ISO.
    		if (!isIso && template.getFormat() == ImageFormat.ISO){
    			s_logger.error("Incorrect format of the template id " + templateId);
    			throw new InvalidParameterValueException("Incorrect format " + template.getFormat() + " of the template id " + templateId);
    		}
        }
    	
    	Account account = null;
    	DomainVO domain = null;
        if (accountId != null) {
        	account = _accountDao.findById(accountId);
        	domain = _domainDao.findById(account.getDomainId());
        } else {
        	domain = _domainDao.findById(DomainVO.ROOT_DOMAIN);
        }
        
        List<VMTemplateVO> templates = new ArrayList<VMTemplateVO>();
        
        if (template == null) {
    		templates = _templateDao.searchTemplates(name, keyword, templateFilter, isIso, bootable, account, domain, pageSize, startIndex, zoneId, hyperType);
    	} else {
    		templates = new ArrayList<VMTemplateVO>();
    		templates.add(template);
    	}
        
        return templates;
    }

    @Override
    public List<VMTemplateVO> listPermittedTemplates(long accountId) {
        return _launchPermissionDao.listPermittedTemplates(accountId);
    }

    @Override
    public List<HostPodVO> listPods(long dataCenterId) {
        return _hostPodDao.listByDataCenterId(dataCenterId);
    }
    
    @Override
    public String changePrivateIPRange(boolean add, Long podId, String startIP, String endIP) throws InvalidParameterValueException {
        return _configMgr.changePrivateIPRange(add, podId, startIP, endIP);
    }

    @Override
    public User findUserById(Long userId) {
        return _userDao.findById(userId);
    }

    @Override
    public List<AccountVO> findAccountsLike(String accountName) {
        return _accountDao.findAccountsLike(accountName);
    }

    @Override
    public Account findActiveAccountByName(String accountName) {
        return _accountDao.findActiveAccountByName(accountName);
    }

    @Override
    public Account findActiveAccount(String accountName, Long domainId) {
        if (domainId == null) {
            domainId = DomainVO.ROOT_DOMAIN;
        }
        return _accountDao.findActiveAccount(accountName, domainId);
    }

    @Override
    public Account findAccountByName(String accountName, Long domainId) {
        if (domainId == null)
            domainId = DomainVO.ROOT_DOMAIN;
        return _accountDao.findAccount(accountName, domainId);
    }

    @Override
    public Account findAccountById(Long accountId) {
        return _accountDao.findById(accountId);
    }

    @Override
    public List<AccountVO> searchForAccounts(ListAccountsCmd cmd) {
        Account account = UserContext.current().getAccount();
        Long domainId = cmd.getDomainId();
        Long accountId = cmd.getId();
        String accountName = null;

        if(accountId != null && accountId == 1){
        	//system account should NOT be searchable
        	List<AccountVO> emptyList = new ArrayList<AccountVO>();
        	return emptyList;
        }
        
        if ((account == null) || isAdmin(account.getType())) {
            accountName = cmd.getSearchName(); // admin's can specify a name to search for
            if (domainId == null) {
                // default domainId to the admin's domain
                domainId = ((account == null) ? DomainVO.ROOT_DOMAIN : account.getDomainId());
            } else if (account != null) {
                if (!_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid domain id (" + domainId + ") given, unable to list accounts");
                }
            }
        } else {
            accountId = account.getId();
            accountName = account.getAccountName(); // regular users must be constrained to their own account
        }

        Filter searchFilter = new Filter(AccountVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object type = cmd.getAccountType();
        Object state = cmd.getState();
        Object isCleanupRequired = cmd.isCleanupRequired();
        Object keyword = cmd.getKeyword();

        SearchBuilder<AccountVO> sb = _accountDao.createSearchBuilder();
        sb.and("accountName", sb.entity().getAccountName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("nid", sb.entity().getId(), SearchCriteria.Op.NEQ);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("needsCleanup", sb.entity().getNeedsCleanup(), SearchCriteria.Op.EQ);

        if ((accountId == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<AccountVO> sc = sb.create();
        if (keyword != null) {
            SearchCriteria<AccountVO> ssc = _accountDao.createSearchCriteria();
            ssc.addOr("accountName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            
            sc.addAnd("accountName", SearchCriteria.Op.SC, ssc);
        }

        if (accountName != null) {
            sc.setParameters("accountName", "%" + accountName + "%");
        }

        if (accountId != null) {
            sc.setParameters("id", accountId);
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);

            // I want to join on user_vm.domain_id = domain.id where domain.path like 'foo%'
            sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
            sc.setParameters("nid", 1L);
        } else {
        	sc.setParameters("nid", 1L);
        }

        if (type != null) {
            sc.setParameters("type", type);
        }

        if (state != null) {
            sc.setParameters("state", state);
        }

        if (isCleanupRequired != null) {
            sc.setParameters("needsCleanup", isCleanupRequired);
        }

        return _accountDao.search(sc, searchFilter);
    }

    @Override
    public Account findAccountByIpAddress(String ipAddress) {
        IPAddressVO address = _publicIpAddressDao.findById(ipAddress);
        if ((address != null) && (address.getAccountId() != null)) {
            return _accountDao.findById(address.getAccountId());
        }
        return null;
    }

    @Override
    public boolean deleteLimit(Long limitId) {
        // A limit ID must be passed in
        if (limitId == null)
            return false;

        return _resourceLimitDao.expunge(limitId);
    }

    @Override
    public ResourceLimitVO findLimitById(long limitId) {
        return _resourceLimitDao.findById(limitId);
    }

    @Override
    public List<VMTemplateVO> listIsos(Criteria c) {
        Filter searchFilter = new Filter(VMTemplateVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        Boolean ready = (Boolean) c.getCriteria(Criteria.READY);
        Boolean isPublic = (Boolean) c.getCriteria(Criteria.ISPUBLIC);
        Long creator = (Long) c.getCriteria(Criteria.CREATED_BY);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        SearchCriteria<VMTemplateVO> sc = _templateDao.createSearchCriteria();

        if (keyword != null) {
            SearchCriteria<VMTemplateVO> ssc = _templateDao.createSearchCriteria();
            ssc.addOr("displayText", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (creator != null) {
            sc.addAnd("accountId", SearchCriteria.Op.EQ, creator);
        }
        if (ready != null) {
            sc.addAnd("ready", SearchCriteria.Op.EQ, ready);
        }
        if (isPublic != null) {
            sc.addAnd("publicTemplate", SearchCriteria.Op.EQ, isPublic);
        }

        sc.addAnd("format", SearchCriteria.Op.EQ, ImageFormat.ISO);

        return _templateDao.search(sc, searchFilter);
    }

    @Override
    public List<VMInstanceVO> findVMInstancesLike(String vmInstanceName) {
        return _vmInstanceDao.findVMInstancesLike(vmInstanceName);
    }

    @Override
    public VMInstanceVO findVMInstanceById(long vmId) {
        return _vmInstanceDao.findById(vmId);
    }

    @Override
    public UserVmVO findUserVMInstanceById(long userVmId) {
        return _userVmDao.findById(userVmId);
    }

    @Override
    public ServiceOfferingVO findServiceOfferingById(long offeringId) {
        return _offeringsDao.findById(offeringId);
    }

    @Override
    public List<ServiceOfferingVO> listAllServiceOfferings() {
        return _offeringsDao.listAllIncludingRemoved();
    }

    @Override
    public List<HostVO> listAllActiveHosts() {
        return _hostDao.listAll();
    }

    @Override
    public DataCenterVO findDataCenterById(long dataCenterId) {
        return _dcDao.findById(dataCenterId);
    }

    @Override
    public VMTemplateVO updateTemplate(UpdateIsoCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        return updateTemplateOrIso(cmd);
    }

    @Override
    public VMTemplateVO updateTemplate(UpdateTemplateCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        return updateTemplateOrIso(cmd);
    }

    private VMTemplateVO updateTemplateOrIso(UpdateTemplateOrIsoCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
    	Long id = cmd.getId();
    	String name = cmd.getTemplateName();
    	String displayText = cmd.getDisplayText();
    	String format = cmd.getFormat();
    	Long guestOSId = cmd.getOsTypeId();
    	Boolean passwordEnabled = cmd.isPasswordEnabled();
    	Boolean bootable = cmd.isBootable();
    	Account account= UserContext.current().getAccount();
    	
    	//verify that template exists
    	VMTemplateVO template = findTemplateById(id);
    	if (template == null) {
    		throw new InvalidParameterValueException("unable to find template/iso with id " + id);
    	}
    	
        //Don't allow to modify system template
        if (id == Long.valueOf(1)) {
        	throw new InvalidParameterValueException("Unable to update template/iso with id " + id);
        }
    	
    	//do a permission check
        if (account != null) {
            Long templateOwner = template.getAccountId();
            if (!BaseCmd.isAdmin(account.getType())) {
                if ((templateOwner == null) || (account.getId() != templateOwner.longValue())) {
                    throw new PermissionDeniedException("Unable to modify template/iso with id " + id + ", permission denied.");
                }
            } else if (account.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                Long templateOwnerDomainId = findDomainIdByAccountId(templateOwner);
                if (!isChildDomain(account.getDomainId(), templateOwnerDomainId)) {
                    throw new PermissionDeniedException("Unable to modify template/iso with id " + id + ", permission denied");
                }
            }
        }
        

    	boolean updateNeeded = !(name == null && displayText == null && format == null && guestOSId == null && passwordEnabled == null && bootable == null);
    	if (!updateNeeded) {
    		return template;
    	}
    	
    	template = _templateDao.createForUpdate(id);
    	
    	if (name != null) {
    		template.setName(name);
    	}
    	
    	if (displayText != null) {
    		template.setDisplayText(displayText);
    	}
    	
    	ImageFormat imageFormat = null;
    	if (format != null) {
    		try {
    			imageFormat = ImageFormat.valueOf(format.toUpperCase());
    		} catch (IllegalArgumentException e) {
    			throw new InvalidParameterValueException("Image format: " + format + " is incorrect. Supported formats are " + EnumUtils.listValues(ImageFormat.values()));
    		}
    		
    		template.setFormat(imageFormat);
    	}
    	
    	if (guestOSId != null) {
    		GuestOSVO guestOS = _guestOSDao.findById(guestOSId);
    		
    		if (guestOS == null) {
    			throw new InvalidParameterValueException("Please specify a valid guest OS ID.");
    		} else {
    			template.setGuestOSId(guestOSId);
    		}
    	}
    	
    	if (passwordEnabled != null) {
    		template.setEnablePassword(passwordEnabled);
    	}
    	
    	if (bootable != null) {
    		template.setBootable(bootable);
    	}
    	
        _templateDao.update(id, template);

        return _templateDao.findById(id);
    }
    
    @Override
    public boolean copyTemplate(long userId, long templateId, long sourceZoneId, long destZoneId, long startEventId) {
    	boolean success = false;
		try {
			success = _tmpltMgr.copy(userId, templateId, sourceZoneId, destZoneId, startEventId);
		} catch (Exception e) {
			s_logger.warn("Unable to copy template " + templateId + " from zone " + sourceZoneId + " to " + destZoneId , e);
			success = false;
		}
		return success;
    }

    @Override
    public VMTemplateVO findTemplateById(long templateId) {
        return _templateDao.findById(templateId);
    }
    
    @Override
    public List<UserVmVO> searchForUserVMs(ListVMsCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Account account = UserContext.current().getAccount();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;
        boolean isAdmin = false;
        if ((account == null) || isAdmin(account.getType())) {
            isAdmin = true;
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Invalid domain id (" + domainId + ") given, unable to list virtual machines.");
                }

                if (accountName != null) {
                    account = _accountDao.findActiveAccount(accountName, domainId);
                    if (account == null) {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                    accountId = account.getId();
                }
            } else {
                domainId = ((account == null) ? DomainVO.ROOT_DOMAIN : account.getDomainId());
            }

        } else {
            accountName = account.getAccountName();
            accountId = account.getId();
            domainId = account.getDomainId();
        }

        Criteria c = new Criteria("id", Boolean.TRUE, cmd.getStartIndex(), cmd.getPageSizeVal());
        c.addCriteria(Criteria.KEYWORD, cmd.getKeyword());
        c.addCriteria(Criteria.ID, cmd.getId());
        c.addCriteria(Criteria.NAME, cmd.getInstanceName());
        c.addCriteria(Criteria.STATE, cmd.getState());
        c.addCriteria(Criteria.DATACENTERID, cmd.getZoneId());
        c.addCriteria(Criteria.GROUPID, cmd.getGroupId());

        // ignore these search requests if it's not an admin
        if (isAdmin == true) {
            c.addCriteria(Criteria.DOMAINID, domainId);
            c.addCriteria(Criteria.PODID, cmd.getPodId());
            c.addCriteria(Criteria.HOSTID, cmd.getHostId());
        }

        c.addCriteria(Criteria.ACCOUNTID, new Object[] {accountId});
        c.addCriteria(Criteria.ISADMIN, isAdmin); 

        return searchForUserVMs(c);
    }

    @Override
    public List<UserVmVO> searchForUserVMs(Criteria c) {
        Filter searchFilter = new Filter(UserVmVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        
        SearchBuilder<UserVmVO> sb = _userVmDao.createSearchBuilder();
       
        // some criteria matter for generating the join condition
        Object[] accountIds = (Object[]) c.getCriteria(Criteria.ACCOUNTID);
        Object domainId = c.getCriteria(Criteria.DOMAINID);
        
        // get the rest of the criteria
        Object id = c.getCriteria(Criteria.ID);
        Object name = c.getCriteria(Criteria.NAME);
        Object state = c.getCriteria(Criteria.STATE);
        Object notState = c.getCriteria(Criteria.NOTSTATE);
        Object zone = c.getCriteria(Criteria.DATACENTERID);
        Object pod = c.getCriteria(Criteria.PODID);
        Object hostId = c.getCriteria(Criteria.HOSTID);
        Object hostName = c.getCriteria(Criteria.HOSTNAME);
        Object keyword = c.getCriteria(Criteria.KEYWORD);
        Object isAdmin = c.getCriteria(Criteria.ISADMIN);
        Object ipAddress = c.getCriteria(Criteria.IPADDRESS);
        Object groupId = c.getCriteria(Criteria.GROUPID);
        
        sb.and("displayName", sb.entity().getDisplayName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("accountIdEQ", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("accountIdIN", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("name", sb.entity().getHostName(), SearchCriteria.Op.LIKE);
        sb.and("stateEQ", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("stateNEQ", sb.entity().getState(), SearchCriteria.Op.NEQ);
        sb.and("stateNIN", sb.entity().getState(), SearchCriteria.Op.NIN);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("hostIdEQ", sb.entity().getHostId(), SearchCriteria.Op.EQ);
        sb.and("hostIdIN", sb.entity().getHostId(), SearchCriteria.Op.IN);
        sb.and("guestIP", sb.entity().getGuestIpAddress(), SearchCriteria.Op.EQ);
        
        if ((accountIds == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }
        
        if (groupId != null && (Long)groupId == -1) {
        	SearchBuilder<InstanceGroupVMMapVO> vmSearch = _groupVMMapDao.createSearchBuilder();
        	vmSearch.and("instanceId", vmSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
            sb.join("vmSearch", vmSearch, sb.entity().getId(), vmSearch.entity().getInstanceId(), JoinBuilder.JoinType.LEFTOUTER);
        } else if (groupId != null) {
        	SearchBuilder<InstanceGroupVMMapVO> groupSearch = _groupVMMapDao.createSearchBuilder();
        	groupSearch.and("groupId", groupSearch.entity().getGroupId(), SearchCriteria.Op.EQ);
            sb.join("groupSearch", groupSearch, sb.entity().getId(), groupSearch.entity().getInstanceId(), JoinBuilder.JoinType.INNER);
        }

        // populate the search criteria with the values passed in
        SearchCriteria<UserVmVO> sc = sb.create();
        
        if (groupId != null && (Long)groupId == -1){
        	sc.setJoinParameters("vmSearch", "instanceId", (Object)null);
        } else if (groupId != null ) {
        	sc.setJoinParameters("groupSearch", "groupId", groupId);
        }

        if (keyword != null) {
            SearchCriteria<UserVmVO> ssc = _userVmDao.createSearchCriteria();
            ssc.addOr("displayName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("instanceName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("displayName", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }
        if (accountIds != null) {
            if (accountIds.length == 1) {
                if (accountIds[0] != null) {
                    sc.setParameters("accountIdEQ", accountIds[0]);
                }
            } else {
                sc.setParameters("accountIdIN", accountIds);
            }
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById((Long)domainId);

            // I want to join on user_vm.domain_id = domain.id where domain.path like 'foo%'
            sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
        }

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }
        if (state != null) {
            if (notState != null && (Boolean) notState == true) {
                sc.setParameters("stateNEQ", state);
            } else {
                sc.setParameters("stateEQ", state);
            }
        }

        if ((isAdmin != null) && ((Boolean) isAdmin != true)) {
            sc.setParameters("stateNIN", "Destroyed", "Expunging");
        }

        if (zone != null) {
            sc.setParameters("dataCenterId", zone);
            
            if(state == null)
            	sc.setParameters("stateNEQ", "Destroyed");
        }
        if (pod != null) {
            sc.setParameters("podId", pod);
            
            if(state == null)
            	sc.setParameters("stateNEQ", "Destroyed");
        }

        if (hostId != null) {
            sc.setParameters("hostIdEQ", hostId);
        } else {
            if (hostName != null) {
                List<HostVO> hosts = _hostDao.findHostsLike((String) hostName);
                if (hosts != null & !hosts.isEmpty()) {
                    Long[] hostIds = new Long[hosts.size()];
                    for (int i = 0; i < hosts.size(); i++) {
                        HostVO host = hosts.get(i);
                        hostIds[i] = host.getId();
                    }
                    sc.setParameters("hostIdIN", (Object[]) hostIds);
                } else {
                    return new ArrayList<UserVmVO>();
                }
            }
        }

        if (ipAddress != null) {
            sc.setParameters("guestIP", ipAddress);
        }
        
        return _userVmDao.search(sc, searchFilter);
    }

    @Override
    public FirewallRuleVO updatePortForwardingRule(UpdateIPForwardingRuleCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
    	String publicIp = cmd.getPublicIp();
    	String privateIp = cmd.getPrivateIp();
    	String privatePort = cmd.getPrivatePort();
    	String publicPort = cmd.getPublicPort();
    	String protocol = cmd.getProtocol();
    	Long vmId = cmd.getVirtualMachineId();
    	Long userId = UserContext.current().getUserId();
    	Account account = UserContext.current().getAccount();
    	UserVmVO userVM = null;
    	
        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }

        IPAddressVO ipAddressVO = findIPAddressById(publicIp);
        if (ipAddressVO == null) {
            throw new InvalidParameterValueException("Unable to find IP address " + publicIp);
        }

        if (ipAddressVO.getAccountId() == null) {
            throw new InvalidParameterValueException("Unable to update port forwarding rule, owner of IP address " + publicIp + " not found.");
        }

        if (privateIp != null) {
            if (!NetUtils.isValidIp(privateIp)) {
                throw new InvalidParameterValueException("Invalid private IP address specified: " + privateIp);
            }
            Criteria c = new Criteria();
            c.addCriteria(Criteria.ACCOUNTID, new Object[] {ipAddressVO.getAccountId()});
            c.addCriteria(Criteria.DATACENTERID, ipAddressVO.getDataCenterId());
            c.addCriteria(Criteria.IPADDRESS, privateIp);
            List<UserVmVO> userVMs = searchForUserVMs(c);
            if ((userVMs == null) || userVMs.isEmpty()) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid private IP address specified: " + privateIp + ", no virtual machine instances running with that address.");
            }
            userVM = userVMs.get(0);
        } else if (vmId != null) {
            userVM = findUserVMInstanceById(vmId);
            if (userVM == null) {
                throw new InvalidParameterValueException("Unable to find virtual machine with id " + vmId);
            }

            if ((ipAddressVO.getAccountId() == null) || (ipAddressVO.getAccountId().longValue() != userVM.getAccountId())) {
                throw new PermissionDeniedException("Unable to update port forwarding rule on IP address " + publicIp + ", permission denied."); 
            }

            if (ipAddressVO.getDataCenterId() != userVM.getDataCenterId()) {
                throw new PermissionDeniedException("Unable to update port forwarding rule, IP address " + publicIp + " is not in the same availability zone as virtual machine " + userVM.toString());
            }

            privateIp = userVM.getGuestIpAddress();
        } else {
            throw new InvalidParameterValueException("No private IP address (privateip) or virtual machine instance id (virtualmachineid) specified, unable to update port forwarding rule");
        }

        // if an admin account was passed in, or no account was passed in, make sure we honor the accountName/domainId parameters
        if (account != null) {
            if (isAdmin(account.getType())) {
                if (!_domainDao.isChildDomain(account.getDomainId(), ipAddressVO.getDomainId())) {
                    throw new PermissionDeniedException("Unable to update port forwarding rule on IP address " + publicIp + ", permission denied.");
                }
            } else if (account.getId() != ipAddressVO.getAccountId()) {
                throw new PermissionDeniedException("Unable to update port forwarding rule on IP address " + publicIp + ", permission denied.");
            }
        }
        
        List<FirewallRuleVO> fwRules = _firewallRulesDao.listIPForwardingForUpdate(publicIp, publicPort, protocol);
        if ((fwRules != null) && (fwRules.size() == 1)) {
            FirewallRuleVO fwRule = fwRules.get(0);
            String oldPrivateIP = fwRule.getPrivateIpAddress();
            String oldPrivatePort = fwRule.getPrivatePort();
            fwRule.setPrivateIpAddress(privateIp);
            fwRule.setPrivatePort(privatePort);
            _firewallRulesDao.update(fwRule.getId(), fwRule);
            _networkMgr.updateFirewallRule(fwRule, oldPrivateIP, oldPrivatePort);
            return fwRule;
        }else{
        	s_logger.warn("Unable to find the rule to be updated for public ip:public port"+publicIp+":"+publicPort+ "private ip:private port:"+privateIp+":"+privatePort);
        	throw new InvalidParameterValueException("Unable to find the rule to be updated for public ip:public port"+publicIp+":"+publicPort+ " private ip:private port:"+privateIp+":"+privatePort);
        }
    }

    @Override
    public FirewallRuleVO findForwardingRuleById(Long ruleId) {
        return _firewallRulesDao.findById(ruleId);
    }

    @Override
    public IPAddressVO findIPAddressById(String ipAddress) {
        return _publicIpAddressDao.findById(ipAddress);
    }

    @Override
    public List<EventVO> searchForEvents(ListEventsCmd cmd) throws PermissionDeniedException, InvalidParameterValueException {
        Account account = UserContext.current().getAccount();
        Long accountId = null;
        boolean isAdmin = false;
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();

        if ((account == null) || isAdmin(account.getType())) {
            isAdmin = true;
            // validate domainId before proceeding
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Invalid domain id (" + domainId + ") given, unable to list events.");
                }

                if (accountName != null) {
                    Account userAccount = _accountDao.findAccount(accountName, domainId);
                    if (userAccount != null) {
                        accountId = userAccount.getId();
                    } else {
                        throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to find account " + accountName + " in domain " + domainId);
                    }
                }
            } else {
                domainId = ((account == null) ? DomainVO.ROOT_DOMAIN : account.getDomainId());
            }
        } else {
            accountId = account.getId();
        }

        Filter searchFilter = new Filter(EventVO.class, "createDate", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object type = cmd.getType();
        Object level = cmd.getLevel();
        Date startDate = cmd.getStartDate();
        Date endDate = cmd.getEndDate();
        Object keyword = cmd.getKeyword();
        Integer entryTime = cmd.getEntryTime();
        Integer duration = cmd.getDuration();

        if ((entryTime != null) && (duration != null)) {
            if (entryTime <= duration){
                throw new InvalidParameterValueException("Entry time must be greater than duration");
            }
            return listPendingEvents(entryTime, duration);
        }

        SearchBuilder<EventVO> sb = _eventDao.createSearchBuilder();
        sb.and("levelL", sb.entity().getLevel(), SearchCriteria.Op.LIKE);
        sb.and("levelEQ", sb.entity().getLevel(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("accountName", sb.entity().getAccountName(), SearchCriteria.Op.LIKE);
        sb.and("domainIdEQ", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
        sb.and("createDateB", sb.entity().getCreateDate(), SearchCriteria.Op.BETWEEN);
        sb.and("createDateG", sb.entity().getCreateDate(), SearchCriteria.Op.GTEQ);
        sb.and("createDateL", sb.entity().getCreateDate(), SearchCriteria.Op.LTEQ);

        if ((accountId == null) && (accountName == null) && (domainId != null) && isAdmin) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<EventVO> sc = sb.create();
        if (keyword != null) {
            SearchCriteria<EventVO> ssc = _eventDao.createSearchCriteria();
            ssc.addOr("type", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("level", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("level", SearchCriteria.Op.SC, ssc);
        }
        
        if (level != null)
        	sc.setParameters("levelEQ", level);
        	
        if (accountId != null) {
            sc.setParameters("accountId", accountId);
        } else if (domainId != null) {
            if (accountName != null) {
                sc.setParameters("domainIdEQ", domainId);
                sc.setParameters("accountName", "%" + accountName + "%");
                sc.addAnd("removed", SearchCriteria.Op.NULL);
            } else if (isAdmin) {
                DomainVO domain = _domainDao.findById(domainId);
                sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
            }
        }

        if (type != null) {
            sc.setParameters("type", type);
        }
        
        if (startDate != null && endDate != null) {
            startDate = massageDate(startDate, 0, 0, 0);
            endDate = massageDate(endDate, 23, 59, 59);
            sc.setParameters("createDateB", startDate, endDate);
        } else if (startDate != null) {
            startDate = massageDate(startDate, 0, 0, 0);
            sc.setParameters("createDateG", startDate);
        } else if (endDate != null) {
            endDate = massageDate(endDate, 23, 59, 59);
            sc.setParameters("createDateL", endDate);
        }

        return _eventDao.searchAllEvents(sc, searchFilter);
    }

    @Override
    public List<DomainRouterVO> listRoutersByHostId(long hostId) {
        return _routerDao.listByHostId(hostId);
    }

    @Override
    public List<DomainRouterVO> listAllActiveRouters() {
        return _routerDao.listAll();
    }

    @Override
    public List<DomainRouterVO> searchForRouters(ListRoutersCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;
        Account account = UserContext.current().getAccount();

        // validate domainId before proceeding
        if (domainId != null) {
            if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                throw new PermissionDeniedException("Invalid domain id (" + domainId + ") given, unable to list routers");
            }
            if (accountName != null) {
                Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                if (userAccount != null) {
                    accountId = userAccount.getId();
                } else {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to find account " + accountName + " in domain " + domainId);
                }
            }
        } else {
            domainId = ((account == null) ? DomainVO.ROOT_DOMAIN : account.getDomainId());
        }

        Filter searchFilter = new Filter(DomainRouterVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object name = cmd.getRouterName();
        Object state = cmd.getState();
        Object zone = cmd.getZoneId();
        Object pod = cmd.getPodId();
        Object hostId = cmd.getHostId();
        Object keyword = cmd.getKeyword();

        SearchBuilder<DomainRouterVO> sb = _routerDao.createSearchBuilder();
        sb.and("name", sb.entity().getHostName(), SearchCriteria.Op.LIKE);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("hostId", sb.entity().getHostId(), SearchCriteria.Op.EQ);

        if ((accountId == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<DomainRouterVO> sc = sb.create();
        if (keyword != null) {
            SearchCriteria<DomainRouterVO> ssc = _routerDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("instanceName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }

        if (accountId != null) {
            sc.setParameters("accountId", accountId);
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
        }

        if (state != null) {
            sc.setParameters("state", state);
        }
        if (zone != null) {
            sc.setParameters("dataCenterId", zone);
        }
        if (pod != null) {
            sc.setParameters("podId", pod);
        }
        if (hostId != null) {
            sc.setParameters("hostId", hostId);
        }

        return _routerDao.search(sc, searchFilter);
    }

    @Override
    public List<ConsoleProxyVO> searchForConsoleProxy(Criteria c) {
        Filter searchFilter = new Filter(ConsoleProxyVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria<ConsoleProxyVO> sc = _consoleProxyDao.createSearchCriteria();

        Object id = c.getCriteria(Criteria.ID);
        Object name = c.getCriteria(Criteria.NAME);
        Object state = c.getCriteria(Criteria.STATE);
        Object zone = c.getCriteria(Criteria.DATACENTERID);
        Object pod = c.getCriteria(Criteria.PODID);
        Object hostId = c.getCriteria(Criteria.HOSTID);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        if (keyword != null) {
            SearchCriteria<ConsoleProxyVO> ssc = _consoleProxyDao.createSearchCriteria();
            ssc.addOr("displayName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("group", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("instanceName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }
        
        if(id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }
        if (state != null) {
            sc.addAnd("state", SearchCriteria.Op.EQ, state);
        }
        if (zone != null) {
            sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zone);
        }
        if (pod != null) {
            sc.addAnd("podId", SearchCriteria.Op.EQ, pod);
        }
        if (hostId != null) {
            sc.addAnd("hostId", SearchCriteria.Op.EQ, hostId);
        }

        return _consoleProxyDao.search(sc, searchFilter);
    }

    @Override
    public List<VolumeVO> searchForVolumes(ListVolumesCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Account account = UserContext.current().getAccount();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;
        boolean isAdmin = false;
        if ((account == null) || isAdmin(account.getType())) {
            isAdmin = true;
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Invalid domain id (" + domainId + ") given, unable to list volumes.");
                }
                if (accountName != null) {
                    Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                    if (userAccount != null) {
                        accountId = userAccount.getId();
                    } else {
                        throw new InvalidParameterValueException("could not find account " + accountName + " in domain " + domainId);
                    }
                }
            } else {
                domainId = ((account == null) ? DomainVO.ROOT_DOMAIN : account.getDomainId());
            }
        } else {
            accountId = account.getId();
        }

        Filter searchFilter = new Filter(VolumeVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object id = cmd.getId();
        Long vmInstanceId = cmd.getVirtualMachineId();
        Object name = cmd.getVolumeName();
        Object keyword = cmd.getKeyword();
        Object type = cmd.getType();

        Object zone = null;
        Object pod = null;
        //Object host = null; TODO
        if (isAdmin) {            
            zone = cmd.getZoneId();
            pod = cmd.getPodId();
            // host = cmd.getHostId(); TODO
        } else {
            domainId = null;
        }

        // hack for now, this should be done better but due to needing a join I opted to
        // do this quickly and worry about making it pretty later
        SearchBuilder<VolumeVO> sb = _volumeDao.createSearchBuilder();
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("accountIdEQ", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("accountIdIN", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("volumeType", sb.entity().getVolumeType(), SearchCriteria.Op.LIKE);
        sb.and("instanceId", sb.entity().getInstanceId(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);

        // Don't return DomR and ConsoleProxy volumes
        sb.and("domRNameLabel", sb.entity().getName(), SearchCriteria.Op.NLIKE);
        sb.and("domPNameLabel", sb.entity().getName(), SearchCriteria.Op.NLIKE);
        sb.and("domSNameLabel", sb.entity().getName(), SearchCriteria.Op.NLIKE);

        // Only return Volumes that are in the "Created" state
        sb.and("status", sb.entity().getStatus(), SearchCriteria.Op.EQ);

        // Only return volumes that are not destroyed
        sb.and("destroyed", sb.entity().getDestroyed(), SearchCriteria.Op.EQ);

        if ((accountId == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        // now set the SC criteria...
        SearchCriteria<VolumeVO> sc = sb.create();
        if (keyword != null) {
            SearchCriteria<VolumeVO> ssc = _volumeDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("nameLabel", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("volumeType", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (accountId != null) {
            sc.setParameters("accountIdEQ", accountId);
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
        }
        if (type != null) {
            sc.setParameters("volumeType", "%" + type + "%");
        }
        if (vmInstanceId != null) {
            sc.setParameters("instanceId", vmInstanceId);
        }
        if (zone != null) {
            sc.setParameters("dataCenterId", zone);
        }
        if (pod != null) {
            sc.setParameters("podId", pod);
        }
        
        // Don't return DomR and ConsoleProxy volumes
        /*
        sc.setParameters("domRNameLabel", "r-%");
        sc.setParameters("domPNameLabel", "v-%");
        sc.setParameters("domSNameLabel", "s-%");
		*/
        
        // Only return volumes that are not destroyed
        sc.setParameters("destroyed", false);

        List<VolumeVO> allVolumes = _volumeDao.search(sc, searchFilter);
        List<VolumeVO> returnableVolumes = new ArrayList<VolumeVO>(); //these are ones without domr and console proxy
        
        for(VolumeVO v:allVolumes)
        {
        	VMTemplateVO template = _templateDao.findById(v.getTemplateId());
        	if(template!=null && (template.getTemplateType() == TemplateType.SYSTEM))
        	{
        		//do nothing
        	}
        	else
        	{
        		returnableVolumes.add(v);
        	}
        }
        
        return returnableVolumes;
    }

    @Override
    public VolumeVO findVolumeByInstanceAndDeviceId(long instanceId, long deviceId) {
         VolumeVO volume = _volumeDao.findByInstanceAndDeviceId(instanceId, deviceId).get(0);
         if (volume != null && !volume.getDestroyed() && volume.getRemoved() == null) {
             return volume;
         } else {
             return null;
         }
    }

    @Override
    public HostPodVO findHostPodById(long podId) {
        return _hostPodDao.findById(podId);
    }
    
    @Override
    public HostVO findSecondaryStorageHosT(long zoneId) {
    	return _storageMgr.getSecondaryStorageHost(zoneId);
    }

    @Override
    public List<IPAddressVO> searchForIPAddresses(ListPublicIpAddressesCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Account account = UserContext.current().getAccount();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;

        if ((account == null) || isAdmin(account.getType())) {
            // validate domainId before proceeding
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Unable to list IP addresses for domain " + domainId + ", permission denied.");
                }

                if (accountName != null) {
                    Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                    if (userAccount != null) {
                        accountId = userAccount.getId();
                    } else {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                }
            } else {
                domainId = ((account == null) ? DomainVO.ROOT_DOMAIN : account.getDomainId());
            }
        } else {
            accountId = account.getId();
        }

        Boolean isAllocated = cmd.isAllocatedOnly();
        if (isAllocated == null) {
            isAllocated = Boolean.TRUE;
        }

        Filter searchFilter = new Filter(IPAddressVO.class, "address", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object zone = cmd.getZoneId();
        Object address = cmd.getIpAddress();
        Object vlan = cmd.getVlanId();
        Object keyword = cmd.getKeyword();
        Object forVirtualNetwork  = cmd.isForVirtualNetwork();

        SearchBuilder<IPAddressVO> sb = _publicIpAddressDao.createSearchBuilder();
        sb.and("accountIdEQ", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("address", sb.entity().getAddress(), SearchCriteria.Op.LIKE);
        sb.and("vlanDbId", sb.entity().getVlanDbId(), SearchCriteria.Op.EQ);

        if ((accountId == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }
        
        if (forVirtualNetwork != null) {
        	SearchBuilder<VlanVO> vlanSearch = _vlanDao.createSearchBuilder();
        	vlanSearch.and("vlanType", vlanSearch.entity().getVlanType(), SearchCriteria.Op.EQ);
        	sb.join("vlanSearch", vlanSearch, sb.entity().getVlanDbId(), vlanSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        if ((isAllocated != null) && (isAllocated == true)) {
            sb.and("allocated", sb.entity().getAllocated(), SearchCriteria.Op.NNULL);
        }

        SearchCriteria<IPAddressVO> sc = sb.create();
        if (accountId != null) {
            sc.setParameters("accountIdEQ", accountId);
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
        }
        
        if (forVirtualNetwork != null) {
        	VlanType vlanType = (Boolean) forVirtualNetwork ? VlanType.VirtualNetwork : VlanType.DirectAttached;
        	sc.setJoinParameters("vlanSearch", "vlanType", vlanType);
        }

        if (zone != null) {
            sc.setParameters("dataCenterId", zone);
        }

        if ((address == null) && (keyword != null)) {
            address = keyword;
        }

        if (address != null) {
            sc.setParameters("address", address + "%");
        }

        if (vlan != null) {
            sc.setParameters("vlanDbId", vlan);
        }

        return _publicIpAddressDao.search(sc, searchFilter);
    }

    @Override
    public List<DiskTemplateVO> listAllActiveDiskTemplates() {
        return _diskTemplateDao.listAll();
    }

    @Override
    public UserAccount authenticateUser(String username, String password, Long domainId, Map<String, Object[]> requestParameters) {
        UserAccount user = null;
        if (password != null) {
            user = getUserAccount(username, password, domainId);
        } else {
            String key = getConfigurationValue("security.singlesignon.key");
            if (key == null) {
                // the SSO key is gone, don't authenticate
                return null;
            }

            String singleSignOnTolerance = getConfigurationValue("security.singlesignon.tolerance.millis");
            if (singleSignOnTolerance == null) {
                // the SSO tolerance is gone (how much time before/after system time we'll allow the login request to be valid), don't authenticate
                return null;
            }

            long tolerance = Long.parseLong(singleSignOnTolerance);
            String signature = null;
            long timestamp = 0L;
            String unsignedRequest = null;

            // - build a request string with sorted params, make sure it's all lowercase
            // - sign the request, verify the signature is the same
            List<String> parameterNames = new ArrayList<String>();

            for (Object paramNameObj : requestParameters.keySet()) {
                parameterNames.add((String)paramNameObj); // put the name in a list that we'll sort later
            }

            Collections.sort(parameterNames);

            try {
                for (String paramName : parameterNames) {
                    // parameters come as name/value pairs in the form String/String[]
                    String paramValue = ((String[])requestParameters.get(paramName))[0];

                    if ("signature".equalsIgnoreCase(paramName)) {
                        signature = paramValue;
                    } else {
                        if ("timestamp".equalsIgnoreCase(paramName)) {
                            String timestampStr = paramValue;
                            try {
                                // If the timestamp is in a valid range according to our tolerance, verify the request signature, otherwise return null to indicate authentication failure
                                timestamp = Long.parseLong(timestampStr);
                                long currentTime = System.currentTimeMillis();
                                if (Math.abs(currentTime - timestamp) > tolerance) {
                                    if (s_logger.isDebugEnabled()) {
                                        s_logger.debug("Expired timestamp passed in to login, current time = " + currentTime + ", timestamp = " + timestamp);
                                    }
                                    return null;
                                }
                            } catch (NumberFormatException nfe) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Invalid timestamp passed in to login: " + timestampStr);
                                }
                                return null;
                            }
                        }

                        if (unsignedRequest == null) {
                            unsignedRequest = paramName + "=" + URLEncoder.encode(paramValue, "UTF-8").replaceAll("\\+", "%20");
                        } else {
                            unsignedRequest = unsignedRequest + "&" + paramName + "=" + URLEncoder.encode(paramValue, "UTF-8").replaceAll("\\+", "%20");
                        }
                    }
                }

                if ((signature == null) || (timestamp == 0L)) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Missing parameters in login request, signature = " + signature + ", timestamp = " + timestamp);
                    }
                    return null;
                }

                unsignedRequest = unsignedRequest.toLowerCase();

                Mac mac = Mac.getInstance("HmacSHA1");
                SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "HmacSHA1");
                mac.init(keySpec);
                mac.update(unsignedRequest.getBytes());
                byte[] encryptedBytes = mac.doFinal();
                String computedSignature = new String(Base64.encodeBase64(encryptedBytes));
                boolean equalSig = signature.equals(computedSignature);
                if (!equalSig) {
                    s_logger.info("User signature: " + signature + " is not equaled to computed signature: " + computedSignature);
                } else {
                    user = getUserAccount(username, domainId);
                }
            } catch (Exception ex) {
                s_logger.error("Exception authenticating user", ex);
                return null;
            }
        }

        if (user != null) {
        	if (s_logger.isDebugEnabled()) {
                s_logger.debug("User: " + username + " in domain " + domainId + " has successfully logged in");
            }
        	EventUtils.saveEvent(user.getId(), user.getAccountId(), EventTypes.EVENT_USER_LOGIN, "user has logged in");
            return user;
        } else {
        	if (s_logger.isDebugEnabled()) {
                s_logger.debug("User: " + username + " in domain " + domainId + " has failed to log in");
            }
            return null;
        }
    }

    @Override
    public void logoutUser(Long userId) {
        UserAccount userAcct = _userAccountDao.findById(userId);
        if (userAcct != null) {
            EventUtils.saveEvent(userId, userAcct.getAccountId(), EventTypes.EVENT_USER_LOGOUT, "user has logged out");
        } // else log some kind of error event?  This likely means the user doesn't exist, or has been deleted...
    }


    @Override
    public List<VMTemplateVO> listAllTemplates() {
        return _templateDao.listAllIncludingRemoved();
    }

    @Override
    public List<GuestOSVO> listGuestOSByCriteria(ListGuestOsCmd cmd) {
        Filter searchFilter = new Filter(GuestOSVO.class, "displayName", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Long id = cmd.getId();
        Long osCategoryId = cmd.getOsCategoryId();

        SearchBuilder<GuestOSVO> sb = _guestOSDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("categoryId", sb.entity().getCategoryId(), SearchCriteria.Op.EQ);

        SearchCriteria<GuestOSVO> sc = sb.create();

        if (id != null) {
            sc.setParameters("id",id);
        }

        if (osCategoryId != null) {
            sc.setParameters("categoryId", osCategoryId);
        }

        return _guestOSDao.search(sc, searchFilter);
    }

    @Override
    public List<GuestOSCategoryVO> listGuestOSCategoriesByCriteria(ListGuestOsCategoriesCmd cmd) {
        Filter searchFilter = new Filter(GuestOSCategoryVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Long id = cmd.getId();

        SearchBuilder<GuestOSCategoryVO> sb = _guestOSCategoryDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);

        SearchCriteria<GuestOSCategoryVO> sc = sb.create();

        if (id != null) {
            sc.setParameters("id",id);
        }

    	return _guestOSCategoryDao.search(sc, searchFilter);
    }
    
    @Override
    public String getConfigurationValue(String name) {
    	return _configDao.getValue(name);
    }

    @Override
    public ConsoleProxyInfo getConsoleProxy(long dataCenterId, long userVmId) {
        return _consoleProxyMgr.assignProxy(dataCenterId, userVmId);
    }

    @Override
    public ConsoleProxyVO startConsoleProxy(long instanceId, long startEventId) {
        return _consoleProxyMgr.startProxy(instanceId, startEventId);
    }

    @Override
    public ConsoleProxyVO stopConsoleProxy(long instanceId, long startEventId) {
        _consoleProxyMgr.stopProxy(instanceId, startEventId);
        return _consoleProxyDao.findById(instanceId);
    }

    @Override
    public ConsoleProxyVO rebootConsoleProxy(long instanceId, long startEventId) {
        _consoleProxyMgr.rebootProxy(instanceId, startEventId);
        return _consoleProxyDao.findById(instanceId);
    }

    @Override
    public String getConsoleAccessUrlRoot(long vmId) {
        VMInstanceVO vm = this.findVMInstanceById(vmId);
        if (vm != null) {
            ConsoleProxyInfo proxy = getConsoleProxy(vm.getDataCenterId(), vmId);
            if (proxy != null)
                return proxy.getProxyImageUrl();
        }
        return null;
    }

    @Override
    public int getVncPort(VirtualMachine vm) {
        if (vm.getHostId() == null) {
        	s_logger.warn("VM " + vm.getHostName() + " does not have host, return -1 for its VNC port");
            return -1;
        }
        
        if(s_logger.isTraceEnabled())
        	s_logger.trace("Trying to retrieve VNC port from agent about VM " + vm.getHostName());
        
        GetVncPortAnswer answer = (GetVncPortAnswer) _agentMgr.easySend(vm.getHostId(), new GetVncPortCommand(vm.getId(), vm.getInstanceName()));
        int port = answer == null ? -1 : answer.getPort();
        
        if(s_logger.isTraceEnabled())
        	s_logger.trace("Retrieved VNC port about VM " + vm.getHostName() + " is " + port);
        
        return port;
    }

    @Override
    public ConsoleProxyVO findConsoleProxyById(long instanceId) {
        return _consoleProxyDao.findById(instanceId);
    }

    @Override
    public List<DomainVO> searchForDomains(ListDomainsCmd cmd) throws PermissionDeniedException {
        Long domainId = cmd.getId();
        Account account = UserContext.current().getAccount();
        if (account != null) {
            if (domainId != null) {
                if (!_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Unable to list domains for domain id " + domainId + ", permission denied.");
                }
            } else {
                domainId = account.getDomainId();
            }
        }

        Filter searchFilter = new Filter(DomainVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        String domainName = cmd.getDomainName();
        Integer level = cmd.getLevel();
        Object keyword = cmd.getKeyword();

        SearchBuilder<DomainVO> sb = _domainDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("level", sb.entity().getLevel(), SearchCriteria.Op.EQ);
        sb.and("path", sb.entity().getPath(), SearchCriteria.Op.LIKE);

        SearchCriteria<DomainVO> sc = sb.create();

        if (keyword != null) {
            SearchCriteria<DomainVO> ssc = _domainDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (domainName != null) {
            sc.setParameters("name", "%" + domainName + "%");
        }

        if (level != null) {
            sc.setParameters("level", level);
        }

        if (domainId != null) {
            sc.setParameters("id", domainId);
        }

        return _domainDao.search(sc, searchFilter);
    }

    @Override
    public List<DomainVO> searchForDomainChildren(ListDomainChildrenCmd cmd) throws PermissionDeniedException {
        Filter searchFilter = new Filter(DomainVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Long domainId = cmd.getId();
        String domainName = cmd.getDomainName();
        Object keyword = cmd.getKeyword();

        Account account = UserContext.current().getAccount();
        if (account != null) {
            if (domainId != null) {
                if (!_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Unable to list domains children for domain id " + domainId + ", permission denied.");
                }
            } else {
                domainId = account.getDomainId();
            }
        }

        SearchCriteria<DomainVO> sc = _domainDao.createSearchCriteria();

        if (keyword != null) {
            SearchCriteria<DomainVO> ssc = _domainDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (domainId != null) {
            sc.addAnd("parent", SearchCriteria.Op.EQ, domainId);
        }

        if (domainName != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + domainName + "%");
        }

        return _domainDao.search(sc, searchFilter);
	}

    @Override
    public DomainVO createDomain(CreateDomainCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        String name = cmd.getDomainName();
        Long parentId = cmd.getParentDomainId();
        Long ownerId = UserContext.current().getAccountId();
        Account account = UserContext.current().getAccount();

        if (ownerId == null) {
            ownerId = Long.valueOf(1);
        }

        if (parentId == null) {
            parentId = Long.valueOf(DomainVO.ROOT_DOMAIN);
        }

        DomainVO parentDomain = _domainDao.findById(parentId);
        if (parentDomain == null) {
            throw new InvalidParameterValueException("Unable to create domain " + name + ", parent domain " + parentId + " not found.");
        }

        if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), parentId)) {
            throw new PermissionDeniedException("Unable to create domain " + name + ", permission denied.");
        }

        SearchCriteria<DomainVO> sc = _domainDao.createSearchCriteria();
        sc.addAnd("name", SearchCriteria.Op.EQ, name);
        sc.addAnd("parent", SearchCriteria.Op.EQ, parentId);
        List<DomainVO> domains = _domainDao.search(sc, null);
        if ((domains == null) || domains.isEmpty()) {
            DomainVO domain = new DomainVO(name, ownerId, parentId);
            try {
                DomainVO dbDomain = _domainDao.create(domain);
                EventUtils.saveEvent(new Long(1), ownerId, EventVO.LEVEL_INFO, EventTypes.EVENT_DOMAIN_CREATE, "Domain, " + name + " created with owner id = " + ownerId
                        + " and parentId " + parentId);
                return dbDomain;
            } catch (IllegalArgumentException ex) {
            	EventUtils.saveEvent(new Long(1), ownerId, EventVO.LEVEL_ERROR, EventTypes.EVENT_DOMAIN_CREATE, "Domain, " + name + " was not created with owner id = " + ownerId
                        + " and parentId " + parentId);
                throw ex;
            }
        } else {
        	EventUtils.saveEvent(new Long(1), ownerId, EventVO.LEVEL_ERROR, EventTypes.EVENT_DOMAIN_CREATE, "Domain, " + name + " was not created with owner id = " + ownerId
                    + " and parentId " + parentId);
        }
        return null;
    }

    @Override
    public boolean deleteDomain(DeleteDomainCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Account account = UserContext.current().getAccount();
        Long domainId = cmd.getId();
        Boolean cleanup = cmd.getCleanup();

        if ((domainId == DomainVO.ROOT_DOMAIN) || ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId))) {
            throw new PermissionDeniedException("Unable to delete domain " + domainId + ", permission denied.");
        }

        try {
            DomainVO domain = _domainDao.findById(domainId);
            if (domain != null) {
                long ownerId = domain.getAccountId();
                if ((cleanup != null) && cleanup.booleanValue()) {
                    boolean success = cleanupDomain(domainId, ownerId);
                    if (!success) {
                    	EventUtils.saveEvent(new Long(1), ownerId, EventVO.LEVEL_ERROR, EventTypes.EVENT_DOMAIN_DELETE, "Failed to clean up domain resources and sub domains, domain with id " + domainId + " was not deleted.");
                        s_logger.error("Failed to clean up domain resources and sub domains, delete failed on domain " + domain.getName() + " (id: " + domainId + ").");
                        return false;
                    }
                } else {
                    if (!_domainDao.remove(domainId)) {
                    	EventUtils.saveEvent(new Long(1), ownerId, EventVO.LEVEL_ERROR, EventTypes.EVENT_DOMAIN_DELETE, "Domain with id " + domainId + " was not deleted");
                        s_logger.error("Delete failed on domain " + domain.getName() + " (id: " + domainId + "); please make sure all users and sub domains have been removed from the domain before deleting");
                        return false;
                    } else {
                    	EventUtils.saveEvent(new Long(1), ownerId, EventVO.LEVEL_INFO, EventTypes.EVENT_DOMAIN_DELETE, "Domain with id " + domainId + " was deleted");
                    }
                }
            } else {
                throw new InvalidParameterValueException("Failed to delete domain nable " + domainId + ", domain not found");
            }
            return true;
        } catch (InvalidParameterValueException ex) {
            throw ex;
        } catch (Exception ex) {
            s_logger.error("Exception deleting domain with id " + domainId, ex);
            return false;
        }
    }

    private boolean cleanupDomain(Long domainId, Long ownerId) {
        boolean success = true;
        {
            SearchCriteria<DomainVO> sc = _domainDao.createSearchCriteria();
            sc.addAnd("parent", SearchCriteria.Op.EQ, domainId);
            List<DomainVO> domains = _domainDao.search(sc, null);

            // cleanup sub-domains first
            for (DomainVO domain : domains) {
                success = (success && cleanupDomain(domain.getId(), domain.getAccountId()));
            }
        }

        {
            // delete users which will also delete accounts and release resources for those accounts
            SearchCriteria<AccountVO> sc = _accountDao.createSearchCriteria();
            sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
            List<AccountVO> accounts = _accountDao.search(sc, null);
            for (AccountVO account : accounts) {
                SearchCriteria<UserVO> userSc = _userDao.createSearchCriteria();
                userSc.addAnd("accountId", SearchCriteria.Op.EQ, account.getId());
                List<UserVO> users = _userDao.search(userSc, null);
                for (UserVO user : users) {
                    success = (success && _accountMgr.deleteUserInternal(user.getId()));
                }
            }
        }

        // delete the domain itself
        boolean deleteDomainSuccess = _domainDao.remove(domainId);
        if (!deleteDomainSuccess) {
        	EventUtils.saveEvent(new Long(1), ownerId, EventVO.LEVEL_ERROR, EventTypes.EVENT_DOMAIN_DELETE, "Domain with id " + domainId + " was not deleted");
        } else {
        	EventUtils.saveEvent(new Long(1), ownerId, EventVO.LEVEL_INFO, EventTypes.EVENT_DOMAIN_DELETE, "Domain with id " + domainId + " was deleted");
        }

        return success && deleteDomainSuccess;
    }

    @Override
    public DomainVO updateDomain(UpdateDomainCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
    	Long domainId = cmd.getId();
    	String domainName = cmd.getDomainName();
    	
        //check if domain exists in the system
    	DomainVO domain = _domainDao.findById(domainId);
    	if (domain == null) {
    		throw new InvalidParameterValueException("Unable to find domain " + domainId);
    	} else if (domain.getParent() == null) {
            //check if domain is ROOT domain - and deny to edit it
    		throw new InvalidParameterValueException("ROOT domain can not be edited");
    	}

    	// check permissions
    	Account account = UserContext.current().getAccount();
    	if ((account != null) && !isChildDomain(account.getDomainId(), domain.getId())) {
            throw new PermissionDeniedException("Unable to update domain " + domainId + ", permission denied");
    	}

    	if (domainName == null || domainName.equals(domain.getName())) {
    		return _domainDao.findById(domainId);
    	}
    	
        SearchCriteria<DomainVO> sc = _domainDao.createSearchCriteria();
        sc.addAnd("name", SearchCriteria.Op.EQ, domainName);
        List<DomainVO> domains = _domainDao.search(sc, null);
        if ((domains == null) || domains.isEmpty()) {
            _domainDao.update(domainId, domainName);
            domain = _domainDao.findById(domainId);
            EventUtils.saveEvent(new Long(1), domain.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_DOMAIN_UPDATE, "Domain, " + domainName + " was updated");
            return _domainDao.findById(domainId);
        } else {
            domain = _domainDao.findById(domainId);
            EventUtils.saveEvent(new Long(1), domain.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_DOMAIN_UPDATE, "Failed to update domain " + domain.getName() + " with name " + domainName + ", name in use.");
            s_logger.error("Domain with name " + domainName + " already exists in the system");
            throw new CloudRuntimeException("Fail to update domain " + domainId);
        }
    }

    @Override
    public Long findDomainIdByAccountId(Long accountId) {
        if (accountId == null) {
            return null;
        }

        AccountVO account = _accountDao.findById(accountId);
        if (account != null) {
            return account.getDomainId();
        }

        return null;
    }

    @Override
    public DomainVO findDomainByPath(String domainPath) {
        return _domainDao.findDomainByPath(domainPath);
    }

    @Override
    public List<AlertVO> searchForAlerts(ListAlertsCmd cmd) {
        Filter searchFilter = new Filter(AlertVO.class, "lastSent", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<AlertVO> sc = _alertDao.createSearchCriteria();

        
        Object type = cmd.getType();
        Object keyword = cmd.getKeyword();

        if (keyword != null) {
            SearchCriteria<AlertVO> ssc = _alertDao.createSearchCriteria();
            ssc.addOr("subject", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            
            sc.addAnd("subject", SearchCriteria.Op.SC, ssc);
        }

        if (type != null) {
            sc.addAnd("type", SearchCriteria.Op.EQ, type);
        }

        return _alertDao.search(sc, searchFilter);
    }

    @Override
    public List<CapacityVO> listCapacities(ListCapacityCmd cmd) {
        // make sure capacity is accurate before displaying it anywhere
        // NOTE: listCapacities is currently called by the UI only, so this
        // shouldn't be called much since it checks all hosts/VMs
        // to figure out what has been allocated.
        _alertMgr.recalculateCapacity();

        Filter searchFilter = new Filter(CapacityVO.class, "capacityType", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<CapacityVO> sc = _capacityDao.createSearchCriteria();

        Object type = cmd.getType();
        Object zoneId = cmd.getZoneId();
        Object podId = cmd.getPodId();
        Object hostId = cmd.getHostId();

        if (type != null) {
            sc.addAnd("capacityType", SearchCriteria.Op.EQ, type);
        }

        if (zoneId != null) {
            sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        }

        if (podId != null) {
            sc.addAnd("podId", SearchCriteria.Op.EQ, podId);
        }

        if (hostId != null) {
            sc.addAnd("hostOrPoolId", SearchCriteria.Op.EQ, hostId);
        }

        return _capacityDao.search(sc, searchFilter);
    }
    
    @Override
    public long getMemoryUsagebyHost(Long hostId) {
        long mem = 0;
        List<VMInstanceVO> vms = _vmInstanceDao.listUpByHostIdTypes(hostId, VirtualMachine.Type.DomainRouter);
        mem += vms.size() * _routerRamSize * 1024L * 1024L;
 
        vms = _vmInstanceDao.listUpByHostIdTypes(hostId, VirtualMachine.Type.SecondaryStorageVm);
        mem += vms.size() * _ssRamSize * 1024L * 1024L;
        
        vms = _vmInstanceDao.listUpByHostIdTypes(hostId, VirtualMachine.Type.ConsoleProxy);
        mem += vms.size() * _proxyRamSize * 1024L * 1024L;
        
        
        List<UserVmVO> instances = _userVmDao.listUpByHostId(hostId);
        for (UserVmVO vm : instances) {
            ServiceOffering so = findServiceOfferingById(vm.getServiceOfferingId());
            mem += so.getRamSize() * 1024L * 1024L;
        }
        return mem;
    }

    @Override
    public boolean destroyTemplateSnapshot(Long userId, long snapshotId) {
        return _vmMgr.destroyTemplateSnapshot(userId, snapshotId);
    }

    @Override
    public List<SnapshotVO> listSnapshots(ListSnapshotsCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Long volumeId = cmd.getVolumeId();

        // Verify parameters
        if(volumeId != null){
            VolumeVO volume = _volumeDao.findById(volumeId);
            if (volume == null) {
                throw new InvalidParameterValueException("unable to find a volume with id " + volumeId);
            }
            checkAccountPermissions(volume.getAccountId(), volume.getDomainId(), "volume", volumeId);
        }

        Account account = UserContext.current().getAccount();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;
        if ((account == null) || isAdmin(account.getType())) {
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Unable to list templates for domain " + domainId + ", permission denied.");
                }
            } else if ((account != null) && (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)) {
                domainId = account.getDomainId();
            }

            if (domainId != null && accountName != null) {
                Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                if (userAccount != null) {
                    accountId = userAccount.getId();
                }
            }
        } else {
            accountId = account.getId();
        }

        Object name = cmd.getSnapshotName();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Object snapshotTypeStr = cmd.getSnapshotType();

        Filter searchFilter = new Filter(SnapshotVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<SnapshotVO> sb = _snapshotDao.createSearchBuilder();
        sb.and("status", sb.entity().getStatus(), SearchCriteria.Op.EQ);
        sb.and("volumeId", sb.entity().getVolumeId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("snapshotTypeEQ", sb.entity().getSnapshotType(), SearchCriteria.Op.EQ);
        sb.and("snapshotTypeNEQ", sb.entity().getSnapshotType(), SearchCriteria.Op.NEQ);

        if ((accountId == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<AccountVO> accountSearch = _accountDao.createSearchBuilder();
            sb.join("accountSearch", accountSearch, sb.entity().getAccountId(), accountSearch.entity().getId(), JoinType.INNER);

            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            accountSearch.join("domainSearch", domainSearch, accountSearch.entity().getDomainId(), domainSearch.entity().getId(), JoinType.INNER);
        }

        SearchCriteria<SnapshotVO> sc = sb.create();

        sc.setParameters("status", Snapshot.Status.BackedUp);

        if (volumeId != null) {
            sc.setParameters("volumeId", volumeId);
        }

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (keyword != null) {
            SearchCriteria<SnapshotVO> ssc = _snapshotDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (accountId != null) {
            sc.setParameters("accountId", accountId);
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            SearchCriteria<?> joinSearch = sc.getJoin("accountSearch");
            joinSearch.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
        }

        if (snapshotTypeStr != null) {
            SnapshotType snapshotType = SnapshotVO.getSnapshotType((String)snapshotTypeStr);
            if (snapshotType == null) {
                throw new InvalidParameterValueException("Unsupported snapshot type " + snapshotTypeStr);
            }
            sc.setParameters("snapshotTypeEQ", snapshotType.ordinal());
        } else {
            // Show only MANUAL and RECURRING snapshot types
            sc.setParameters("snapshotTypeNEQ", Snapshot.SnapshotType.TEMPLATE.ordinal());
        }
        
        return _snapshotDao.search(sc, searchFilter);
    }

    @Override
    public DiskOfferingVO findDiskOfferingById(long diskOfferingId) {
        return _diskOfferingDao.findById(diskOfferingId);
    }

    @Override
    public List<DiskOfferingVO> findPrivateDiskOffering() {
        return _diskOfferingDao.findPrivateDiskOffering();
    }
    
    protected boolean templateIsCorrectType(VMTemplateVO template) {
    	return true;
    }
    
	public static boolean isAdmin(short accountType) {
	    return ((accountType == Account.ACCOUNT_TYPE_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
	}

    @Override @DB
    public boolean updateTemplatePermissions(UpdateTemplatePermissionsCmd cmd)  {
        return updateTemplateOrIsoPermissions(cmd);
    }

    @Override @DB
    public boolean updateTemplatePermissions(UpdateIsoPermissionsCmd cmd) {
        return updateTemplateOrIsoPermissions(cmd);
    }

    @DB
    protected boolean updateTemplateOrIsoPermissions(UpdateTemplateOrIsoPermissionsCmd cmd) {
        Transaction txn = Transaction.currentTxn();
        
        //Input validation
        Long id = cmd.getId();
        Account account = UserContext.current().getAccount();
        List<String> accountNames = cmd.getAccountNames();
        Long userId = UserContext.current().getUserId();
        Boolean isFeatured = cmd.isFeatured();
        Boolean isPublic = cmd.isPublic();
        String operation = cmd.getOperation();
        String mediaType = "";

        VMTemplateVO template = _templateDao.findById(id);
        
        if (template == null || !templateIsCorrectType(template)) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "unable to find " + mediaType + " with id " + id);
        }

        if(cmd instanceof UpdateTemplatePermissionsCmd)
        {
        	mediaType = "template";
        	if(template.getFormat().equals(ImageFormat.ISO))
        	{
        		throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Please provide a valid template");
        	}
        }
        if(cmd instanceof UpdateIsoPermissionsCmd)
        {
        	mediaType = "iso";
        	if(!template.getFormat().equals(ImageFormat.ISO))
        	{
        		throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Please provide a valid iso");
        	}
        }
        
        if (account != null) 
        {
            if (!isAdmin(account.getType()) && (template.getAccountId() != account.getId())) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "unable to update permissions for " + mediaType + " with id " + id);
            } else if (account.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                Long templateOwnerDomainId = findDomainIdByAccountId(template.getAccountId());
                if (!isChildDomain(account.getDomainId(), templateOwnerDomainId)) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to update permissions for " + mediaType + " with id " + id);
                }
            }
        }

        // If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }
        
        // If the template is removed throw an error.
        if (template.getRemoved() != null){
        	s_logger.error("unable to update permissions for " + mediaType + " with id " + id + " as it is removed  ");
        	throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "unable to update permissions for " + mediaType + " with id " + id + " as it is removed ");
        }
        
        if (id == Long.valueOf(1)) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to update permissions for " + mediaType + " with id " + id);
        }
        
        boolean isAdmin = ((account == null) || isAdmin(account.getType()));
        boolean allowPublicUserTemplates = Boolean.parseBoolean(getConfigurationValue("allow.public.user.templates"));        
        if (!isAdmin && !allowPublicUserTemplates && isPublic != null && isPublic) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Only private " + mediaType + "s can be created.");
        }

//        // package up the accountNames as a list
//        List<String> accountNameList = new ArrayList<String>();
        if (accountNames != null) 
        {
            if ((operation == null) || (!operation.equalsIgnoreCase("add") && !operation.equalsIgnoreCase("remove") && !operation.equalsIgnoreCase("reset"))) 
            {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid operation on accounts, the operation must be either 'add' or 'remove' in order to modify launch permissions." +
                        "  Given operation is: '" + operation + "'");
            }
//            StringTokenizer st = new StringTokenizer(accountNames, ",");
//            while (st.hasMoreTokens()) {
//                accountNameList.add(st.nextToken());
//            }
        }

        Long accountId = template.getAccountId();
        if (accountId == null) {
            // if there is no owner of the template then it's probably already a public template (or domain private template) so publishing to individual users is irrelevant
            throw new InvalidParameterValueException("Update template permissions is an invalid operation on template " + template.getName());
        }

        VMTemplateVO updatedTemplate = _templateDao.createForUpdate();
        
        if (isPublic != null) {
            updatedTemplate.setPublicTemplate(isPublic.booleanValue());
        }
        
        if (isFeatured != null) {
        	updatedTemplate.setFeatured(isFeatured.booleanValue());
        }
        
        _templateDao.update(template.getId(), updatedTemplate);

        Long domainId = account.getDomainId();
        if ("add".equalsIgnoreCase(operation)) {
            txn.start();
            for (String accountName : accountNames) {
                Account permittedAccount = _accountDao.findActiveAccount(accountName, domainId);
                if (permittedAccount != null) {
                    if (permittedAccount.getId() == account.getId()) {
                        continue; // don't grant permission to the template owner, they implicitly have permission
                    }
                    LaunchPermissionVO existingPermission = _launchPermissionDao.findByTemplateAndAccount(id, permittedAccount.getId());
                    if (existingPermission == null) {
                        LaunchPermissionVO launchPermission = new LaunchPermissionVO(id, permittedAccount.getId());
                        _launchPermissionDao.persist(launchPermission);
                    }
                } else {
                    txn.rollback();
                    throw new InvalidParameterValueException("Unable to grant a launch permission to account " + accountName + ", account not found.  "
                            + "No permissions updated, please verify the account names and retry.");
                }
            }
            txn.commit();
        } else if ("remove".equalsIgnoreCase(operation)) {
            List<Long> accountIds = new ArrayList<Long>();
            for (String accountName : accountNames) {
                Account permittedAccount = _accountDao.findActiveAccount(accountName, domainId);
                if (permittedAccount != null) {
                    accountIds.add(permittedAccount.getId());
                }
            }
            _launchPermissionDao.removePermissions(id, accountIds);
        } else if ("reset".equalsIgnoreCase(operation)) {
            // do we care whether the owning account is an admin? if the
            // owner is an admin, will we still set public to false?
            updatedTemplate = _templateDao.createForUpdate();
            updatedTemplate.setPublicTemplate(false);
            updatedTemplate.setFeatured(false);
            _templateDao.update(template.getId(), updatedTemplate);
            _launchPermissionDao.removeAllPermissions(id);
        }
        return true;
    }

    @Override
    public List<String> listTemplatePermissions(ListTemplateOrIsoPermissionsCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Account account = UserContext.current().getAccount();
        Long domainId = cmd.getDomainId();
        String acctName = cmd.getAccountName();
        Long id = cmd.getId();
        Long accountId = null;

        if ((account == null) || account.getType() == Account.ACCOUNT_TYPE_ADMIN) {
            // validate domainId before proceeding
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Invalid domain id (" + domainId + ") given, unable to list " + cmd.getMediaType() + " permissions.");
                }
                if (acctName != null) {
                    Account userAccount = _accountDao.findActiveAccount(acctName, domainId);
                    if (userAccount != null) {
                        accountId = userAccount.getId();
                    } else {
                        throw new PermissionDeniedException("Unable to find account " + acctName + " in domain " + domainId);
                    }
                }
            }
        } else {
            accountId = account.getId();
        }

        VMTemplateVO template = _templateDao.findById(id.longValue());
        if (template == null || !templateIsCorrectType(template)) {
            throw new InvalidParameterValueException("unable to find " + cmd.getMediaType() + " with id " + id);
        }

        if (accountId != null && !template.isPublicTemplate()) {
            if (account.getType() == Account.ACCOUNT_TYPE_NORMAL && template.getAccountId() != accountId) {
                throw new PermissionDeniedException("unable to list permissions for " + cmd.getMediaType() + " with id " + id);
            } else if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
                DomainVO accountDomain = _domainDao.findById(account.getDomainId());
                Account templateAccount = _accountDao.findById(template.getAccountId());
                DomainVO templateDomain = _domainDao.findById(templateAccount.getDomainId());                    
                if (!templateDomain.getPath().contains(accountDomain.getPath())) {
                    throw new PermissionDeniedException("unable to list permissions for " + cmd.getMediaType() + " with id " + id);
                }
            }                                    
        }

        if (id == Long.valueOf(1)) {
            throw new PermissionDeniedException("unable to list permissions for " + cmd.getMediaType() + " with id " + id);
        }

        List<String> accountNames = new ArrayList<String>();        
        List<LaunchPermissionVO> permissions = _launchPermissionDao.findByTemplate(id);
        if ((permissions != null) && !permissions.isEmpty()) {
            for (LaunchPermissionVO permission : permissions) {
                Account acct = _accountDao.findById(permission.getAccountId());
                accountNames.add(acct.getAccountName());
            }
        }
        return accountNames;
    }

    @Override
    public List<DiskOfferingVO> searchForDiskOfferings(ListDiskOfferingsCmd cmd) {
        Filter searchFilter = new Filter(DiskOfferingVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<DiskOfferingVO> sb = _diskOfferingDao.createSearchBuilder();

        // SearchBuilder and SearchCriteria are now flexible so that the search builder can be built with all possible
        // search terms and only those with criteria can be set.  The proper SQL should be generated as a result.
        Object name = cmd.getDiskOfferingName();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();

        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);

        // FIXME:  disk offerings should search back up the hierarchy for available disk offerings...
        /*
        sb.addAnd("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        if (domainId != null) {
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.addAnd("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId());
        }
        */

        SearchCriteria<DiskOfferingVO> sc = sb.create();
        if (keyword != null) {
            SearchCriteria<DiskOfferingVO> ssc = _diskOfferingDao.createSearchCriteria();
            ssc.addOr("displayText", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        // FIXME:  disk offerings should search back up the hierarchy for available disk offerings...
        /*
        if (domainId != null) {
            sc.setParameters("domainId", domainId);
            //
            //DomainVO domain = _domainDao.findById((Long)domainId);
            //
            // I want to join on user_vm.domain_id = domain.id where domain.path like 'foo%'
            //sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
            //
        }
        */

        return _diskOfferingDao.search(sc, searchFilter);
    }

//    @Override
//    public AsyncJobResult queryAsyncJobResult(QueryAsyncJobResultCmd cmd) throws PermissionDeniedException {
//        return queryAsyncJobResult(cmd.getId());
//    }

    @Override
    public AsyncJobResult queryAsyncJobResult(long jobId) throws PermissionDeniedException {
        AsyncJobVO job = _asyncMgr.getAsyncJob(jobId);
        if (job == null) {
            if (s_logger.isDebugEnabled())
                s_logger.debug("queryAsyncJobResult error: Permission denied, invalid job id " + jobId);

            throw new PermissionDeniedException("Permission denied, invalid job id " + jobId);
        }

        // treat any requests from API server as trusted requests
        if (!UserContext.current().isApiServer() && job.getAccountId() != UserContext.current().getAccountId()) {
            if (s_logger.isDebugEnabled())
                s_logger.debug("Mismatched account id in job and user context, perform further securty check. job id: "
                	+ jobId + ", job owner account: " + job.getAccountId() + ", accound id in current context: " + UserContext.current().getAccountId());
        	
        	Account account = UserContext.current().getAccount();
        	if (account != null) {
        	    if (isAdmin(account.getType())) {
        	        Account jobAccount = _accountDao.findById(job.getAccountId());
        	        if (jobAccount == null) {
        	            if (s_logger.isDebugEnabled()) {
                            s_logger.debug("queryAsyncJobResult error: Permission denied, account no long exist for account id in context, job id: " + jobId
                                    + ", accountId  " + job.getAccountId());
        	            }
        	            throw new PermissionDeniedException("Permission denied, invalid job ownership, job id: " + jobId);
        	        }

        	        if (!_domainDao.isChildDomain(account.getDomainId(), jobAccount.getDomainId())) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("queryAsyncJobResult error: Permission denied, invalid ownership for job " + jobId + ", job account owner: "
                                    + job.getAccountId() + " in domain: " + jobAccount.getDomainId() + ", account id in context: " + account.getId() +
                                    " in domain: " + account.getDomainId());
                        }
                        throw new PermissionDeniedException("Permission denied, invalid job ownership, job id: " + jobId);
        	        }
        	    } else {
        	        if (s_logger.isDebugEnabled()) {
                        s_logger.debug("queryAsyncJobResult error: Permission denied, invalid ownership for job " + jobId + ", job account owner: "
                                + job.getAccountId() + ", account id in context: " + account.getId());
        	        }
                    throw new PermissionDeniedException("Permission denied, invalid job ownership, job id: " + jobId);
        	    }
        	}
        }

        return _asyncMgr.queryAsyncJobResult(jobId);
    }

    @Override
    public AsyncJobVO findAsyncJobById(long jobId) {
        return _asyncMgr.getAsyncJob(jobId);
    }
   
    @Override
    public LoadBalancerVO findLoadBalancer(Long accountId, String name) {
        SearchCriteria<LoadBalancerVO> sc = _loadBalancerDao.createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        sc.addAnd("name", SearchCriteria.Op.EQ, name);
        List<LoadBalancerVO> loadBalancers = _loadBalancerDao.search(sc, null);
        if ((loadBalancers != null) && !loadBalancers.isEmpty()) {
            return loadBalancers.get(0);
        }
        return null;
    }

    @Override
    public LoadBalancerVO findLoadBalancerById(long loadBalancerId) {
        return _loadBalancerDao.findById(Long.valueOf(loadBalancerId));
    }

    @Override
    public List<UserVmVO> listLoadBalancerInstances(ListLoadBalancerRuleInstancesCmd cmd) throws PermissionDeniedException {
        Account account = UserContext.current().getAccount();
        Long loadBalancerId = cmd.getId();
        Boolean applied = cmd.isApplied();

        if (applied == null) {
            applied = Boolean.TRUE;
        }

        LoadBalancerVO loadBalancer = _loadBalancerDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            return null;
        }

        if (account != null) {
            long lbAcctId = loadBalancer.getAccountId();
            if (isAdmin(account.getType())) {
                Account userAccount = _accountDao.findById(lbAcctId);
                if (!_domainDao.isChildDomain(account.getDomainId(), userAccount.getDomainId())) {
                    throw new PermissionDeniedException("Invalid load balancer rule id (" + loadBalancerId + ") given, unable to list load balancer instances.");
                }
            } else if (account.getId() != lbAcctId) {
                throw new PermissionDeniedException("Unable to list load balancer instances, account " + account.getAccountName() + " does not own load balancer rule " + loadBalancer.getName());
            }
        }

        List<UserVmVO> loadBalancerInstances = new ArrayList<UserVmVO>();
        List<LoadBalancerVMMapVO> vmLoadBalancerMappings = null;
        if (applied) {
            // List only the instances that have actually been applied to the load balancer (pending is false).
            vmLoadBalancerMappings = _loadBalancerVMMapDao.listByLoadBalancerId(loadBalancerId, false);
        } else {
            // List all instances applied, even pending ones that are currently being assigned, so that the semantics
            // of "what instances can I apply to this load balancer" are maintained.
            vmLoadBalancerMappings = _loadBalancerVMMapDao.listByLoadBalancerId(loadBalancerId);
        }
        List<Long> appliedInstanceIdList = new ArrayList<Long>();
        if ((vmLoadBalancerMappings != null) && !vmLoadBalancerMappings.isEmpty()) {
            for (LoadBalancerVMMapVO vmLoadBalancerMapping : vmLoadBalancerMappings) {
                appliedInstanceIdList.add(vmLoadBalancerMapping.getInstanceId());
            }
        }

        IPAddressVO addr = _publicIpAddressDao.findById(loadBalancer.getIpAddress());
        List<UserVmVO> userVms = _userVmDao.listVirtualNetworkInstancesByAcctAndZone(loadBalancer.getAccountId(), addr.getDataCenterId());

        for (UserVmVO userVm : userVms) {
            // if the VM is destroyed, being expunged, in an error state, or in an unknown state, skip it
            switch (userVm.getState()) {
            case Destroyed:
            case Expunging:
            case Error:
            case Unknown:
                continue;
            }

            boolean isApplied = appliedInstanceIdList.contains(userVm.getId());
            if (!applied && !isApplied) {
                loadBalancerInstances.add(userVm);
            } else if (applied && isApplied) {
                loadBalancerInstances.add(userVm);
            }
        }

        return loadBalancerInstances;
    }

    @Override
    public List<LoadBalancerVO> searchForLoadBalancers(ListLoadBalancerRulesCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        // do some parameter validation
        Account account = UserContext.current().getAccount();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long accountId = null;
        Account ipAddressOwner = null;
        String ipAddress = cmd.getPublicIp();

        if (ipAddress != null) {
            IPAddressVO ipAddressVO = _publicIpAddressDao.findById(ipAddress);
            if (ipAddressVO == null) {
                throw new InvalidParameterValueException("Unable to list load balancers, IP address " + ipAddress + " not found.");
            } else {
                Long ipAddrAcctId = ipAddressVO.getAccountId();
                if (ipAddrAcctId == null) {
                    throw new InvalidParameterValueException("Unable to list load balancers, IP address " + ipAddress + " is not associated with an account.");
                }
                ipAddressOwner = _accountDao.findById(ipAddrAcctId);
            }
        }

        if ((account == null) || isAdmin(account.getType())) {
            // validate domainId before proceeding
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Unable to list load balancers for domain id " + domainId + ", permission denied.");
                }
                if (accountName != null) {
                    Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                    if (userAccount != null) {
                        accountId = userAccount.getId();
                    } else {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                }
            } else if (ipAddressOwner != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), ipAddressOwner.getDomainId())) {
                    throw new PermissionDeniedException("Unable to list load balancer rules for IP address " + ipAddress + ", permission denied.");
                }
            } else {
                domainId = ((account == null) ? DomainVO.ROOT_DOMAIN : account.getDomainId());
            }
        } else {
            accountId = account.getId();
        }

        Filter searchFilter = new Filter(LoadBalancerVO.class, "ipAddress", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object id = cmd.getId();
        Object name = cmd.getLoadBalancerRuleName();
        Object keyword = cmd.getKeyword();
        Object instanceId = cmd.getVirtualMachineId();

        SearchBuilder<LoadBalancerVO> sb = _loadBalancerDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("nameEQ", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("ipAddress", sb.entity().getIpAddress(), SearchCriteria.Op.EQ);

        if ((accountId == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        if (instanceId != null) {
            SearchBuilder<LoadBalancerVMMapVO> lbVMSearch = _loadBalancerVMMapDao.createSearchBuilder();
            lbVMSearch.and("instanceId", lbVMSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
            sb.join("lbVMSearch", lbVMSearch, sb.entity().getId(), lbVMSearch.entity().getLoadBalancerId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<LoadBalancerVO> sc = sb.create();
        if (keyword != null) {
            SearchCriteria<LoadBalancerVO> ssc = _loadBalancerDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.setParameters("nameEQ", name);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (ipAddress != null) {
            sc.setParameters("ipAddress", ipAddress);
        }

        if (accountId != null) {
            sc.setParameters("accountId", accountId);
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
        }

        if (instanceId != null) {
            sc.setJoinParameters("lbVMSearch", "instanceId", instanceId);
        }

        return _loadBalancerDao.search(sc, searchFilter);
    }

    @Override
    public String[] getApiConfig() {
        return new String[] { "commands.properties" };
    }

    protected class AccountCleanupTask implements Runnable {
        @Override
        public void run() {
            try {
                GlobalLock lock = GlobalLock.getInternLock("AccountCleanup");
                if (lock == null) {
                    s_logger.debug("Couldn't get the global lock");
                    return;
                }

                if (!lock.lock(30)) {
                    s_logger.debug("Couldn't lock the db");
                    return;
                }

                Transaction txn = null;
                try {
                	txn = Transaction.open(Transaction.CLOUD_DB);
                	
                    List<AccountVO> accounts = _accountDao.findCleanups();
                    s_logger.info("Found " + accounts.size() + " accounts to cleanup");
                    for (AccountVO account : accounts) {
                        s_logger.debug("Cleaning up " + account.getId());
                        try {
                            _accountMgr.deleteAccount(account);
                        } catch (Exception e) {
                            s_logger.error("Skipping due to error on account " + account.getId(), e);
                        }
                    }
                } catch (Exception e) {
                    s_logger.error("Exception ", e);
                } finally {
                	if(txn != null)
                		txn.close();
                	
                    lock.unlock();
                }
            } catch (Exception e) {
                s_logger.error("Exception ", e);
            }
        }
    }
    
    protected class EventPurgeTask implements Runnable {
        @Override
        public void run() {
            try {
                GlobalLock lock = GlobalLock.getInternLock("EventPurge");
                if (lock == null) {
                    s_logger.debug("Couldn't get the global lock");
                    return;
                }
                if (!lock.lock(30)) {
                    s_logger.debug("Couldn't lock the db");
                    return;
                }
                try {
                    final Calendar purgeCal = Calendar.getInstance();
                    purgeCal.add(Calendar.DAY_OF_YEAR, -_purgeDelay);
                    Date purgeTime = purgeCal.getTime();
                    s_logger.debug("Deleting events older than: "+purgeTime.toString());
                    List<EventVO> oldEvents = _eventDao.listOlderEvents(purgeTime);
                    s_logger.debug("Found "+oldEvents.size()+" events to be purged");
                    for (EventVO event : oldEvents){
                        _eventDao.expunge(event.getId());
                    }
                } catch (Exception e) {
                    s_logger.error("Exception ", e);
                } finally {
                    lock.unlock();
                }
            } catch (Exception e) {
                s_logger.error("Exception ", e);
            }
        }
    }

    @Override
    public StoragePoolVO findPoolById(Long id) {
        return _poolDao.findById(id);
    }

    @Override
    public List<ClusterVO> listClusterByPodId(long podId) {
        return _clusterDao.listByPodId(podId);
    }

    @Override
    public List<? extends StoragePoolVO> searchForStoragePools(ListStoragePoolsCmd cmd) {
        Criteria c = new Criteria("id", Boolean.TRUE, cmd.getStartIndex(), cmd.getPageSizeVal());
        c.addCriteria(Criteria.NAME, cmd.getStoragePoolName());
        c.addCriteria(Criteria.CLUSTERID, cmd.getClusterId());
        c.addCriteria(Criteria.ADDRESS, cmd.getIpAddress());
        c.addCriteria(Criteria.KEYWORD, cmd.getKeyword());
        c.addCriteria(Criteria.PATH, cmd.getPath());
        c.addCriteria(Criteria.PODID, cmd.getPodId());
        c.addCriteria(Criteria.DATACENTERID, cmd.getZoneId());

        return searchForStoragePools(c);
    }

    @Override
    public List<? extends StoragePoolVO> searchForStoragePools(Criteria c) {
        Filter searchFilter = new Filter(StoragePoolVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria<StoragePoolVO> sc = _poolDao.createSearchCriteria();

        Object name = c.getCriteria(Criteria.NAME);
        Object host = c.getCriteria(Criteria.HOST);
        Object path = c.getCriteria(Criteria.PATH);
        Object zone = c.getCriteria(Criteria.DATACENTERID);
        Object pod = c.getCriteria(Criteria.PODID);
        Object cluster = c.getCriteria(Criteria.CLUSTERID);
        Object address = c.getCriteria(Criteria.ADDRESS);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        if (keyword != null) {
            SearchCriteria<StoragePoolVO> ssc = _poolDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("type", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }
        if (host != null) {
            sc.addAnd("host", SearchCriteria.Op.EQ, host);
        }
        if (path != null) {
            sc.addAnd("path", SearchCriteria.Op.EQ, path);
        }
        if (zone != null) {
            sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zone);
        }
        if (pod != null) {
        	sc.addAnd("podId", SearchCriteria.Op.EQ, pod);
        }
        if (address != null) {
        	sc.addAnd("hostAddress", SearchCriteria.Op.EQ, address);
        }
        if (cluster != null) {
        	sc.addAnd("clusterId", SearchCriteria.Op.EQ, cluster);
        }

        return _poolDao.search(sc, searchFilter);
    }



    @Override
    public List<String> searchForStoragePoolDetails(long poolId, String value)
    {
    	return _poolDao.searchForStoragePoolDetails(poolId, value);
    }
    
    @Override
    public List<AsyncJobVO> searchForAsyncJobs(ListAsyncJobsCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Filter searchFilter = new Filter(AsyncJobVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<AsyncJobVO> sb = _jobDao.createSearchBuilder();

        Object accountId = null;
        Long domainId = cmd.getDomainId();
        Account account = UserContext.current().getAccount();
        if ((account == null) || isAdmin(account.getType())) {
            String accountName = cmd.getAccountName();

            if ((accountName != null) && (domainId != null)) {
                Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                if (userAccount != null) {
                    accountId = userAccount.getId();
                } else {
                    throw new InvalidParameterValueException("Failed to list async jobs for account " + accountName + " in domain " + domainId + "; account not found.");
                }
            } else if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Failed to list async jobs for domain " + domainId + "; permission denied.");
                }                    

                // we can do a domain match for the admin case
                SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
                domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);

                SearchBuilder<AccountVO> accountSearch = _accountDao.createSearchBuilder();
                accountSearch.join("domainSearch", domainSearch, accountSearch.entity().getDomainId(), domainSearch.entity().getId(), JoinType.INNER);

                sb.join("accountSearch", accountSearch, sb.entity().getAccountId(), accountSearch.entity().getId(), JoinType.INNER);
            }
        } else {
            accountId = account.getId();
        }

        Object keyword = cmd.getKeyword();
        Object startDate = cmd.getStartDate();


        SearchCriteria<AsyncJobVO> sc = _jobDao.createSearchCriteria();
        if (keyword != null) {
            sc.addAnd("cmd", SearchCriteria.Op.LIKE, "%" + keyword + "%");
        }

        if (accountId != null) {
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
        }

        if (startDate != null) {
            sc.addAnd("created", SearchCriteria.Op.GTEQ, startDate);
        }

        return _jobDao.search(sc, searchFilter);
    }

    @Override
    public SnapshotPolicyVO findSnapshotPolicyById(Long policyId) {
        return _snapshotPolicyDao.findById(policyId);
    }

    @Override
    public boolean isChildDomain(Long parentId, Long childId) {
        return _domainDao.isChildDomain(parentId, childId);
    }

    public SecondaryStorageVmVO startSecondaryStorageVm(long instanceId, long startEventId) {
        return _secStorageVmMgr.startSecStorageVm(instanceId, startEventId);
    }

    public SecondaryStorageVmVO stopSecondaryStorageVm(long instanceId, long startEventId) {
        _secStorageVmMgr.stopSecStorageVm(instanceId, startEventId);
        return _secStorageVmDao.findById(instanceId);
    }

    public SecondaryStorageVmVO rebootSecondaryStorageVm(long instanceId, long startEventId) {
        _secStorageVmMgr.rebootSecStorageVm(instanceId, startEventId);
        return _secStorageVmDao.findById(instanceId);
    }

    public boolean destroySecondaryStorageVm(long instanceId, long startEventId) {
        return _secStorageVmMgr.destroySecStorageVm(instanceId, startEventId);
    }

	@Override
    public List<SecondaryStorageVmVO> searchForSecondaryStorageVm(Criteria c) {
        Filter searchFilter = new Filter(SecondaryStorageVmVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria<SecondaryStorageVmVO> sc = _secStorageVmDao.createSearchCriteria();

        Object id = c.getCriteria(Criteria.ID);
        Object name = c.getCriteria(Criteria.NAME);
        Object state = c.getCriteria(Criteria.STATE);
        Object zone = c.getCriteria(Criteria.DATACENTERID);
        Object pod = c.getCriteria(Criteria.PODID);
        Object hostId = c.getCriteria(Criteria.HOSTID);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        if (keyword != null) {
            SearchCriteria<SecondaryStorageVmVO> ssc = _secStorageVmDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("displayName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("instanceName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }
        
        if(id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }
        if (state != null) {
            sc.addAnd("state", SearchCriteria.Op.EQ, state);
        }
        if (zone != null) {
            sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zone);
        }
        if (pod != null) {
            sc.addAnd("podId", SearchCriteria.Op.EQ, pod);
        }
        if (hostId != null) {
            sc.addAnd("hostId", SearchCriteria.Op.EQ, hostId);
        }

        return _secStorageVmDao.search(sc, searchFilter);
    }

	@Override @SuppressWarnings({"unchecked", "rawtypes"})
	public List<? extends VMInstanceVO> searchForSystemVm(ListSystemVMsCmd cmd) {
        Criteria c = new Criteria("id", Boolean.TRUE, cmd.getStartIndex(), cmd.getPageSizeVal());

        c.addCriteria(Criteria.KEYWORD, cmd.getKeyword());
        c.addCriteria(Criteria.ID, cmd.getId());
        c.addCriteria(Criteria.DATACENTERID, cmd.getZoneId());
        c.addCriteria(Criteria.PODID, cmd.getPodId());
        c.addCriteria(Criteria.HOSTID, cmd.getHostId());
        c.addCriteria(Criteria.NAME, cmd.getSystemVmName());
        c.addCriteria(Criteria.STATE, cmd.getState());

        String type = cmd.getSystemVmType();
        List systemVMs = new ArrayList();

        if (type == null) { //search for all vm types
            systemVMs.addAll(searchForConsoleProxy(c));
            systemVMs.addAll(searchForSecondaryStorageVm(c));
        } else if((type != null) && (type.equalsIgnoreCase("secondarystoragevm"))) { // search for ssvm
            systemVMs.addAll(searchForSecondaryStorageVm(c));
        } else if((type != null) && (type.equalsIgnoreCase("consoleproxy"))) { // search for consoleproxy
            systemVMs.addAll(searchForConsoleProxy(c));
        }

        return systemVMs;
	}

	@Override
	public VMInstanceVO findSystemVMById(long instanceId) {
		VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(instanceId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
		if(systemVm == null)
			return null;
		
		if(systemVm.getType() == VirtualMachine.Type.ConsoleProxy)
			return _consoleProxyDao.findById(instanceId);
		return _secStorageVmDao.findById(instanceId);
	}

	@Override
	public VMInstanceVO startSystemVM(StartSystemVMCmd cmd) {
		
		//verify input
		Long id = cmd.getId();

		VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(id, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
        	throw new ServerApiException (BaseCmd.PARAM_ERROR, "unable to find a system vm with id " + id);
        }
		
		if (systemVm.getType().equals(VirtualMachine.Type.ConsoleProxy)){
			long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_START, "Starting console proxy with Id: "+id);
	        try {
				checkIfStoragePoolAvailable(id);
			} catch (StorageUnavailableException e) {
				s_logger.warn(e.getMessage());
				return null;
			} catch (Exception e){
				//unforseen exceptions
				s_logger.warn(e.getMessage());
				return null;
			}
			return startConsoleProxy(id, eventId);
		} else {
			long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_START, "Starting secondary storage Vm Id: "+id);
	        try {
				checkIfStoragePoolAvailable(id);
			} catch (StorageUnavailableException e) {
				s_logger.warn(e.getMessage());
				return null;
			} catch (Exception e){
				//unforseen exceptions
				s_logger.warn(e.getMessage());
				return null;
			}
			return startSecondaryStorageVm(id, eventId);
		}
	}

	private void checkIfStoragePoolAvailable(Long id) throws StorageUnavailableException {
		//check if the sp is up before starting
        List<VolumeVO> rootVolList = _volumeDao.findByInstanceAndType(id, VolumeType.ROOT); 
        if(rootVolList == null || rootVolList.size() == 0){
        	throw new StorageUnavailableException("Could not find the root disk for this vm to verify if the pool on which it exists is Up or not");
        }else{
        	Long poolId = rootVolList.get(0).getPoolId();//each vm has 1 root vol
        	StoragePoolVO sp = _poolDao.findById(poolId);
        	if(sp == null){
        		throw new StorageUnavailableException("Could not find the pool for the root disk of vm"+id+", to confirm if it is Up or not");
        	}else{
        		//found pool
        		if(!sp.getStatus().equals(com.cloud.host.Status.Up)){
        			throw new StorageUnavailableException("Could not start the vm; the associated storage pool is in:"+sp.getStatus().toString()+" state");
        		}
        	}
        }
	}

	@Override
	public VMInstanceVO stopSystemVM(StopSystemVmCmd cmd) {
		Long id = cmd.getId();
		
	    // verify parameters      
		VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(id, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
        	throw new ServerApiException (BaseCmd.PARAM_ERROR, "unable to find a system vm with id " + id);
        }

        // FIXME: We need to return the system VM from this method, so what do we do with the boolean response from stopConsoleProxy and stopSecondaryStorageVm?
		if (systemVm.getType().equals(VirtualMachine.Type.ConsoleProxy)){
			long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_STOP, "stopping console proxy with Id: "+id);
			return stopConsoleProxy(id, eventId);
		} else {
			long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_STOP, "stopping secondary storage Vm Id: "+id);
			return stopSecondaryStorageVm(id, eventId);
		}
	}

	@Override
	public VMInstanceVO rebootSystemVM(RebootSystemVmCmd cmd)  {
		VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(cmd.getId(), VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
	
		if (systemVm == null) {
        	throw new ServerApiException (BaseCmd.PARAM_ERROR, "unable to find a system vm with id " + cmd.getId());
        }
		
		if (systemVm.getType().equals(VirtualMachine.Type.ConsoleProxy)){
			long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_REBOOT, "Rebooting console proxy with Id: "+cmd.getId());
			return rebootConsoleProxy(cmd.getId(), eventId);
		} else {
			long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_REBOOT, "Rebooting secondary storage vm with Id: "+cmd.getId());
			return rebootSecondaryStorageVm(cmd.getId(), eventId);
		}
	}

	private String signRequest(String request, String key) {
		try
		{
			s_logger.info("Request: "+request);
			s_logger.info("Key: "+key);
			
			if(key != null && request != null)
			{
				Mac mac = Mac.getInstance("HmacSHA1");
				SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(),
						"HmacSHA1");
				mac.init(keySpec);
				mac.update(request.getBytes());
				byte[] encryptedBytes = mac.doFinal();
				return new String ((Base64.encodeBase64(encryptedBytes)));
			}
		} catch (Exception ex) {
			s_logger.error("unable to sign request", ex);
		}
		return null;
	}

    @Override
    public ArrayList<String> getCloudIdentifierResponse(GetCloudIdentifierCmd cmd) throws InvalidParameterValueException{
    	Long userId = cmd.getUserId();
    	
    	//verify that user exists
        User user = findUserById(userId);
        if ((user == null) || (user.getRemoved() != null))
        	throw new InvalidParameterValueException("Unable to find active user by id " + userId);

    	String cloudIdentifier = _configDao.getValue("cloud.identifier");
    	if (cloudIdentifier == null) {
    	    cloudIdentifier = "";
    	}

    	String signature = "";
    	try {
        	//get the user obj to get his secret key
        	user = getUser(userId);
        	String secretKey = user.getSecretKey();
        	String input = cloudIdentifier;
    		signature = signRequest(input, secretKey);
    	} catch (Exception e) {
			s_logger.warn("Exception whilst creating a signature:"+e);
		}
    	
    	ArrayList<String> cloudParams = new ArrayList<String>();
    	cloudParams.add(cloudIdentifier);
    	cloudParams.add(signature);

        return cloudParams;
    }

	@Override
	public NetworkGroupVO findNetworkGroupByName(Long accountId, String groupName) {
		NetworkGroupVO groupVO = _networkSecurityGroupDao.findByAccountAndName(accountId, groupName);
		return groupVO;
	}

    @Override
    public NetworkGroupVO findNetworkGroupById(long networkGroupId) {
        NetworkGroupVO groupVO = _networkSecurityGroupDao.findById(networkGroupId);
        return groupVO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHypervisorSnapshotCapable() {
        return _isHypervisorSnapshotCapable;
    }
    
    @Override
    public List<EventVO> listPendingEvents(int entryTime, int duration) {
        Calendar calMin = Calendar.getInstance();
        Calendar calMax = Calendar.getInstance();
        calMin.add(Calendar.SECOND, -entryTime);
        calMax.add(Calendar.SECOND, -duration);
        Date minTime = calMin.getTime();
        Date maxTime = calMax.getTime();
        List<EventVO> startedEvents = _eventDao.listStartedEvents(minTime, maxTime);
        List<EventVO> pendingEvents = new ArrayList<EventVO>();
        for (EventVO event : startedEvents){
            EventVO completedEvent = _eventDao.findCompletedEvent(event.getId());
            if(completedEvent == null){
                pendingEvents.add(event);
            }
        }
        return pendingEvents;
    }

	@Override
	public List<PreallocatedLunVO> getPreAllocatedLuns(ListPreallocatedLunsCmd cmd)	{
       Filter searchFilter = new Filter(PreallocatedLunVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<PreallocatedLunVO> sc = _lunDao.createSearchCriteria();

        Object targetIqn = cmd.getTargetIqn();
        Object scope = cmd.getScope();

        if (targetIqn != null) {
            sc.addAnd("targetIqn", SearchCriteria.Op.EQ, targetIqn);
        }
        
        if (scope == null || scope.toString().equalsIgnoreCase("ALL")) {
            return _lunDao.search(sc, searchFilter);
        } else if(scope.toString().equalsIgnoreCase("ALLOCATED")) {
        	sc.addAnd("volumeId", SearchCriteria.Op.NNULL);
        	sc.addAnd("taken", SearchCriteria.Op.NNULL);
        	
        	return _lunDao.search(sc, searchFilter);
        } else if(scope.toString().equalsIgnoreCase("FREE")) {
        	sc.addAnd("volumeId", SearchCriteria.Op.NULL);
        	sc.addAnd("taken", SearchCriteria.Op.NULL);
        	
        	return _lunDao.search(sc, searchFilter);
        }

		return null;
	}

	@Override
	public boolean checkLocalStorageConfigVal()
	{
		String value = _configs.get("use.local.storage");
		
		if(value!=null && value.equalsIgnoreCase("true"))
			return true;
		else
			return false;
	}

	@Override
	public boolean checkIfMaintenable(long hostId) {

		//get the poolhostref record
		List<StoragePoolHostVO> poolHostRecordSet = _poolHostDao.listByHostId(hostId);
		
		if(poolHostRecordSet!=null)
		{
			//the above list has only 1 record
			StoragePoolHostVO poolHostRecord = poolHostRecordSet.get(0);
			
			//get the poolId and get hosts associated in that pool
			List<StoragePoolHostVO> hostsInPool = _poolHostDao.listByPoolId(poolHostRecord.getPoolId());
			
			if(hostsInPool!=null && hostsInPool.size()>1)
			{
				return true; //since there are other hosts to take over as master in this pool
			}
		}
		return false;
	}

    @Override
    public Map<String, String> listCapabilities(ListCapabilitiesCmd cmd) {
        Map<String, String> capabilities = new HashMap<String, String>();
        
        String networkGroupsEnabled = _configs.get("direct.attach.network.groups.enabled");
        if(networkGroupsEnabled == null) 
            networkGroupsEnabled = "false";             

        capabilities.put("networkGroupsEnabled", networkGroupsEnabled);        
        capabilities.put("cloudStackVersion", getVersion());
        return capabilities;
    }

    @Override
    public GuestOSVO getGuestOs(Long guestOsId)
    {
    	return _guestOSDao.findById(guestOsId);
    }
    
    @Override
    public VolumeVO getRootVolume(Long instanceId)
    {
    	return _volumeDao.findByInstanceAndType(instanceId, Volume.VolumeType.ROOT).get(0);
    }
    
    @Override
    public long getPsMaintenanceCount(long podId){
    	List<StoragePoolVO> poolsInTransition = new ArrayList<StoragePoolVO>();
    	poolsInTransition.addAll(_poolDao.listPoolsByStatus(Status.Maintenance));
    	poolsInTransition.addAll(_poolDao.listPoolsByStatus(Status.PrepareForMaintenance));
    	poolsInTransition.addAll(_poolDao.listPoolsByStatus(Status.ErrorInMaintenance));

    	return poolsInTransition.size();
    }
    
    @Override
    public boolean isPoolUp(long instanceId){
		VolumeVO rootVolume = _volumeDao.findByInstance(instanceId).get(0);
		
		if(rootVolume!=null){
			Status poolStatus = _poolDao.findById(rootVolume.getPoolId()).getStatus();
    	
			if(!poolStatus.equals(Status.Up))
				return false;
			else
				return true;
		}
		
		return false;
    }

    @Override
    public Long extractVolume(ExtractVolumeCmd cmd) throws URISyntaxException {
        Long volumeId = cmd.getId();
        String url = cmd.getUrl();
        Long zoneId = cmd.getZoneId();
        AsyncJobVO job = cmd.getJob();
        String mode = cmd.getMode();
        Account account = UserContext.current().getAccount();
        
        VolumeVO volume = _volumeDao.findById(volumeId);        
        if (volume == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find volume with id " + volumeId);
        }

        if (_dcDao.findById(zoneId) == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid zone.");          
        }
        if(volume.getPoolId() == null){
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "The volume doesnt belong to a storage pool so cant extract it");
        }
        //Extract activity only for detached volumes or for volumes whose instance is stopped
        if(volume.getInstanceId() != null && ApiDBUtils.findVMInstanceById(volume.getInstanceId()).getState() != State.Stopped ){
            s_logger.debug("Invalid state of the volume with ID: " + volumeId + ". It should be either detached or the VM should be in stopped state.");
            throw new PermissionDeniedException("Invalid state of the volume with ID: " + volumeId + ". It should be either detached or the VM should be in stopped state.");
        }
        
        Upload.Mode extractMode;
        if( mode == null || (!mode.equals(Upload.Mode.FTP_UPLOAD.toString()) && !mode.equals(Upload.Mode.HTTP_DOWNLOAD.toString())) ){
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid extract Mode ");
        }else{
            extractMode = mode.equals(Upload.Mode.FTP_UPLOAD.toString()) ? Upload.Mode.FTP_UPLOAD : Upload.Mode.HTTP_DOWNLOAD;
        }
        
        if (account != null) {    
            if(!isAdmin(account.getType())){
                if (volume.getAccountId() != account.getId()){
                    throw new PermissionDeniedException("Unable to find volume with ID: " + volumeId + " for account: " + account.getAccountName());
                }
            } else {
                Account userAccount = _accountDao.findById(volume.getAccountId());
                if((userAccount == null) || !_domainDao.isChildDomain(account.getDomainId(), userAccount.getDomainId())) {
                    throw new PermissionDeniedException("Unable to extract volume:" + volumeId + " - permission denied.");
                }
            }
        }
        
        // If mode is upload perform extra checks on url and also see if there is an ongoing upload on the same.
        if (extractMode == Upload.Mode.FTP_UPLOAD){
            URI uri = new URI(url);
            if ( (uri.getScheme() == null) || (!uri.getScheme().equalsIgnoreCase("ftp") )) {
               throw new IllegalArgumentException("Unsupported scheme for url: " + url);
            }
    
            String host = uri.getHost();
            try {
                InetAddress hostAddr = InetAddress.getByName(host);
                if (hostAddr.isAnyLocalAddress() || hostAddr.isLinkLocalAddress() || hostAddr.isLoopbackAddress() || hostAddr.isMulticastAddress() ) {
                    throw new IllegalArgumentException("Illegal host specified in url");
                }
                if (hostAddr instanceof Inet6Address) {
                    throw new IllegalArgumentException("IPV6 addresses not supported (" + hostAddr.getHostAddress() + ")");
                }
            } catch (UnknownHostException uhe) {
                throw new IllegalArgumentException("Unable to resolve " + host);
            }        
            
            if ( _uploadMonitor.isTypeUploadInProgress(volumeId, Type.VOLUME) ){
                throw new IllegalArgumentException(volume.getName() + " upload is in progress. Please wait for some time to schedule another upload for the same");
            }
        }
        
        long userId = UserContext.current().getUserId();
        long accountId = volume.getAccountId();        

        String secondaryStorageURL = _storageMgr.getSecondaryStorageURL(zoneId); 
        StoragePoolVO srcPool = _poolDao.findById(volume.getPoolId());
        Long sourceHostId = _storageMgr.findHostIdForStoragePool(srcPool);
        List<HostVO> storageServers = _hostDao.listByTypeDataCenter(Host.Type.SecondaryStorage, zoneId);
        HostVO sserver = storageServers.get(0);

        EventUtils.saveStartedEvent(userId, accountId, cmd.getEventType(), "Starting extraction of " +volume.getName()+ " mode:"+mode, cmd.getStartEventId());
        List<UploadVO> extractURLList = _uploadDao.listByTypeUploadStatus(volumeId, Upload.Type.VOLUME, UploadVO.Status.DOWNLOAD_URL_CREATED);
        
        if (extractMode == Upload.Mode.HTTP_DOWNLOAD && extractURLList.size() > 0){   
            return extractURLList.get(0).getId(); // If download url already exists then return 
        }else {
            UploadVO uploadJob = _uploadMonitor.createNewUploadEntry(sserver.getId(), volumeId, UploadVO.Status.COPY_IN_PROGRESS, Type.VOLUME, url, extractMode);
            s_logger.debug("Extract Mode - " +uploadJob.getMode());
            uploadJob = _uploadDao.createForUpdate(uploadJob.getId());
            
            // Update the async Job
            ExtractResponse resultObj = new ExtractResponse(volumeId, volume.getName(), accountId, UploadVO.Status.COPY_IN_PROGRESS.toString(), uploadJob.getId());
            resultObj.setResponseName(cmd.getName());
            _asyncMgr.updateAsyncJobAttachment(job.getId(), Type.VOLUME.toString(), volumeId);
            _asyncMgr.updateAsyncJobStatus(job.getId(), AsyncJobResult.STATUS_IN_PROGRESS, resultObj);
    
            // Copy the volume from the source storage pool to secondary storage
            CopyVolumeCommand cvCmd = new CopyVolumeCommand(volume.getId(), volume.getPath(), srcPool, secondaryStorageURL, true);
            CopyVolumeAnswer cvAnswer = (CopyVolumeAnswer) _agentMgr.easySend(sourceHostId, cvCmd);

            // Check if you got a valid answer.
            if (cvAnswer == null || !cvAnswer.getResult()) {                
                String errorString = "Failed to copy the volume from the source primary storage pool to secondary storage.";

                //Update the async job.
                resultObj.setResultString(errorString);
                resultObj.setUploadStatus(UploadVO.Status.COPY_ERROR.toString());
                _asyncMgr.completeAsyncJob(job.getId(), AsyncJobResult.STATUS_FAILED, 0, resultObj);

                //Update the DB that volume couldn't be copied
                uploadJob.setUploadState(UploadVO.Status.COPY_ERROR);            
                uploadJob.setErrorString(errorString);
                uploadJob.setLastUpdated(new Date());
                _uploadDao.update(uploadJob.getId(), uploadJob);
                
                EventUtils.saveEvent(userId, accountId, EventTypes.EVENT_VOLUME_UPLOAD, errorString);                
                throw new CloudRuntimeException(errorString);            
            }
            
            String volumeLocalPath = "volumes/"+volume.getId()+"/"+cvAnswer.getVolumePath()+".vhd";
            //Update the DB that volume is copied and volumePath
            uploadJob.setUploadState(UploadVO.Status.COPY_COMPLETE);
            uploadJob.setLastUpdated(new Date());
            uploadJob.setInstallPath(volumeLocalPath);
            _uploadDao.update(uploadJob.getId(), uploadJob);
            
            if (extractMode == Mode.FTP_UPLOAD){ // Now that the volume is copied perform the actual uploading
                _uploadMonitor.extractVolume(uploadJob, sserver, volume, url, zoneId, volumeLocalPath, cmd.getStartEventId(), job.getId(), _asyncMgr);
                return uploadJob.getId();
            }else{ // Volume is copied now make it visible under apache and create a URL.
                _uploadMonitor.createVolumeDownloadURL(volumeId, volumeLocalPath, Type.VOLUME, zoneId, uploadJob.getId());                
                EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_INFO, cmd.getEventType(), "Completed extraction of "+volume.getName()+ " in mode:" +mode, null, cmd.getStartEventId());
                return uploadJob.getId();
            }
        }
    }

    @Override
    public InstanceGroupVO updateVmGroup(UpdateVMGroupCmd cmd) {
        Account account = UserContext.current().getAccount();
        Long groupId = cmd.getId();
        String groupName = cmd.getGroupName();

        // Verify input parameters
        InstanceGroupVO group = _vmGroupDao.findById(groupId.longValue());
        if (group == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find a vm group with id " + groupId);
        }

        if (account != null) {
            Account tempAccount = _accountDao.findById(group.getAccountId());
            if (!isAdmin(account.getType()) && (account.getId() != group.getAccountId())) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "unable to find a group with id " + groupId + " for this account");
            } else if (!_domainDao.isChildDomain(account.getDomainId(), tempAccount.getDomainId())) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Invalid group id (" + groupId + ") given, unable to update the group.");
            }
        }

        //Check if name is already in use by this account (exclude this group)
        boolean isNameInUse = _vmGroupDao.isNameInUse(group.getAccountId(), groupName);

        if (isNameInUse && !group.getName().equals(groupName)) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to update vm group, a group with name " + groupName + " already exisits for account");
        }

        if (groupName != null) {
        	_vmGroupDao.updateVmGroup(groupId, groupName);
        }
        InstanceGroupVO vmGroup = _vmGroupDao.findById(groupId);
        return vmGroup;
    }

    @Override
    public List<InstanceGroupVO> searchForVmGroups(ListVMGroupsCmd cmd) {
        Account account = UserContext.current().getAccount();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;
        if ((account == null) || isAdmin(account.getType())) {
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid domain id (" + domainId + ") given, unable to list vm groups.");
                }

                if (accountName != null) {
                    account = _accountDao.findActiveAccount(accountName, domainId);
                    if (account == null) {
                        throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to find account " + accountName + " in domain " + domainId);
                    }
                    accountId = account.getId();
                }
            } else {
                domainId = ((account == null) ? DomainVO.ROOT_DOMAIN : account.getDomainId());
            }
        } else {
            accountName = account.getAccountName();
            accountId = account.getId();
            domainId = account.getDomainId();
        }

        Filter searchFilter = new Filter(InstanceGroupVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object id = cmd.getId();
        Object name = cmd.getGroupName();
        Object keyword = cmd.getKeyword();

        SearchBuilder<InstanceGroupVO> sb = _vmGroupDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);

        if ((accountId == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<InstanceGroupVO> sc = sb.create();
        if (keyword != null) {
            SearchCriteria<InstanceGroupVO> ssc = _vmGroupDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
        }
        
    	if (id != null) {
        	sc.setParameters("id", id);
    	}
        
        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }

        if (accountId != null) {
            sc.setParameters("accountId", accountId);
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            if (domain != null){
            	sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
            }   
        }

        return _vmGroupDao.search(sc, searchFilter);
    }

	@Override
	public InstanceGroupVO getGroupForVm(long vmId){
		return _vmMgr.getGroupForVm(vmId);
	}

    @Override
    public List<VlanVO> searchForZoneWideVlans(long dcId, String vlanType, String vlanId){
    	return _vlanDao.searchForZoneWideVlans(dcId, vlanType, vlanId);
    }

    @Override
    public String getVersion(){
        final Class<?> c = ManagementServer.class;
        String fullVersion = c.getPackage().getImplementationVersion();
        String version = "unknown"; 
        if(fullVersion.length() > 0){
            version = fullVersion.substring(0,fullVersion.lastIndexOf("."));
        }
        return version;
    }
    
    private Long saveScheduledEvent(Long userId, Long accountId, String type, String description) 
    {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setState(EventState.Scheduled);
        event.setDescription("Scheduled async job for "+description);
        event = _eventDao.persist(event);
        return event.getId();
    }

    @Override @DB
    public String uploadCertificate(UploadCustomCertificateCmd cmd) throws ServerApiException{
    	CertificateVO cert = null;
    	Long certVOId = null;
    	try 
    	{
        	Transaction.currentTxn();
			String certificate = cmd.getCertificate();
    		cert = _certDao.listAll().get(0); //always 1 record in db (from the deploydb time)
			cert = _certDao.acquireInLockTable(cert.getId());
			//assign mgmt server id to mark as processing under this ms
			if(cert == null){
				String msg = "Unable to obtain lock on the cert from uploadCertificate()";
				s_logger.error(msg);
				throw new ResourceUnavailableException(msg);
			}else{
	    		if(cert.getUpdated().equalsIgnoreCase("Y")){
					 if(s_logger.isDebugEnabled())
						 s_logger.debug("A custom certificate already exists in the DB, will replace it with the new one being uploaded");
				}else{
					 if(s_logger.isDebugEnabled())
						 s_logger.debug("No custom certificate exists in the DB, will upload a new one");				
				}
	    		
				//validate if the cert follows X509 format, if not, don't persist to db
				InputStream is = new ByteArrayInputStream(certificate.getBytes("UTF-8"));
				BufferedInputStream bis = new BufferedInputStream(is);
				CertificateFactory cf = CertificateFactory.getInstance("X.509");			
				while (bis.available() > 1) {
				   Certificate localCert = cf.generateCertificate(bis);//throws certexception if not valid cert format
				   if(s_logger.isDebugEnabled()){
					   s_logger.debug("The custom certificate generated for validation is:"+localCert.toString());
				   }
				}
				
				certVOId = _certDao.persistCustomCertToDb(certificate,cert,this.getId());//0 implies failure				
				 if(s_logger.isDebugEnabled())
					 s_logger.debug("Custom certificate persisted to the DB");				
			}
			
			if (certVOId != 0) 
			{
				//certficate uploaded to db successfully	
				//get a list of all Console proxies from the cp table
				List<ConsoleProxyVO> cpList = _consoleProxyDao.listAll();
				if(cpList.size() == 0){
					String msg = "Unable to find any console proxies in the system for certificate update";
					s_logger.warn(msg);
					throw new ResourceUnavailableException(msg);
				}
				//get a list of all hosts in host table for type cp
				List<HostVO> cpHosts = _hostDao.listByType(com.cloud.host.Host.Type.ConsoleProxy);
				if(cpHosts.size() == 0){
					String msg = "Unable to find any console proxy hosts in the system for certificate update";
					s_logger.warn(msg);
					throw new ResourceUnavailableException(msg);
				}
				//create a hashmap for fast lookup
				Map<String,Long> hostNameToHostIdMap = new HashMap<String, Long>();
				//updated console proxies id list
				List<Long> updatedCpIdList = new ArrayList<Long>();
				for(HostVO cpHost : cpHosts){
					hostNameToHostIdMap.put(cpHost.getName(), cpHost.getId());
				}
				for(ConsoleProxyVO cp : cpList)
				{
					Long cpHostId = hostNameToHostIdMap.get(cp.getHostName());
					//now send a command to each console proxy host
					UpdateCertificateCommand certCmd = new UpdateCertificateCommand(_certDao.findById(certVOId).getCertificate(), false);
					try {
							Answer updateCertAns = _agentMgr.send(cpHostId, certCmd);
							if(updateCertAns.getResult() == true)
							{
								//we have the cert copied over on cpvm
								long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_REBOOT, "rebooting console proxy with Id: "+cp.getId());    				
								_consoleProxyMgr.rebootProxy(cp.getId(), eventId);
								//when cp reboots, the context will be reinit with the new cert
								if(s_logger.isDebugEnabled())
									s_logger.debug("Successfully updated custom certificate on console proxy vm id:"+cp.getId()+" ,console proxy host id:"+cpHostId);
								updatedCpIdList.add(cp.getId());
							}
					} catch (AgentUnavailableException e) {
						s_logger.warn("Unable to send update certificate command to the console proxy resource as agent is unavailable for console proxy vm id:"+cp.getId()+" ,console proxy host id:"+cpHostId, e);
					} catch (OperationTimedoutException e) {
						s_logger.warn("Unable to send update certificate command to the console proxy resource as there was a timeout for console proxy vm id:"+cp.getId()+" ,console proxy host id:"+cpHostId, e);
					}	
				}
				
				if(updatedCpIdList.size() == cpList.size()){
					//success case, all updated
					return ("Updated:"+updatedCpIdList.size()+" out of:"+cpList.size()+" console proxies");
				}else{
					//failure case, if even one update fails
					throw new ManagementServerException("Updated:"+updatedCpIdList.size()+" out of:"+cpList.size()+" console proxies with successfully updated console proxy ids being:"+(updatedCpIdList.size() > 0 ? updatedCpIdList.toString():""));
				}
			}
			else
			{
				throw new ManagementServerException("Unable to persist custom certificate to the cloud db");
			}
		}catch (Exception e) {
			s_logger.warn("Failed to successfully update the cert across console proxies on management server:"+this.getId());			
			if(e instanceof ResourceUnavailableException)
				throw new ServerApiException(BaseCmd.CUSTOM_CERT_UPDATE_ERROR, e.getMessage());
			if(e instanceof ManagementServerException)
				throw new ServerApiException(BaseCmd.CUSTOM_CERT_UPDATE_ERROR, e.getMessage());
			if(e instanceof IndexOutOfBoundsException){
				String msg = "Custom certificate record in the db deleted; this should never happen. Please create a new record in the certificate table";
				s_logger.error(msg,e);
				throw new ServerApiException(BaseCmd.CUSTOM_CERT_UPDATE_ERROR, msg);
			}
			if(e instanceof FileNotFoundException){
				String msg = "Invalid file path for custom cert found during cert validation";
				s_logger.error(msg,e);
				throw new ServerApiException(BaseCmd.CUSTOM_CERT_UPDATE_ERROR, msg);
			}
			if(e instanceof CertificateException){
				String msg = "The file format for custom cert does not conform to the X.509 specification";
				s_logger.error(msg,e);
				throw new ServerApiException(BaseCmd.CUSTOM_CERT_UPDATE_ERROR, msg);				
			}
			if(e instanceof UnsupportedEncodingException){
				String msg = "Unable to encode the certificate into UTF-8 input stream for validation";
				s_logger.error(msg,e);
				throw new ServerApiException(BaseCmd.CUSTOM_CERT_UPDATE_ERROR, msg);								
			}
			if(e instanceof IOException){
				String msg = "Cannot generate input stream during custom cert validation";
				s_logger.error(msg,e);
				throw new ServerApiException(BaseCmd.CUSTOM_CERT_UPDATE_ERROR, msg);								
			}
		}finally{
				_certDao.releaseFromLockTable(cert.getId());					
		}
		return null;
    }

    @Override
    public String[] getHypervisors(ListHypervisorsCmd cmd) {
    	String hypers = _configDao.getValue(Config.HypervisorList.key());
    	if (hypers == "" || hypers == null) {
    		return null;
    	}
    	return hypers.split(",");
    }

	private Long checkAccountPermissions(long targetAccountId, long targetDomainId, String targetDesc, long targetId) throws ServerApiException {
        Long accountId = null;

        Account account = UserContext.current().getAccount();
        if (account != null) {
            if (!isAdmin(account.getType())) {
                if (account.getId() != targetAccountId) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find a " + targetDesc + " with id " + targetId + " for this account");
                }
            } else if (!_domainDao.isChildDomain(account.getDomainId(), targetDomainId)) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to perform operation for " + targetDesc + " with id " + targetId + ", permission denied.");
            }
            accountId = account.getId();
        }
    
        return accountId;
    }

	@Override
	public List<RemoteAccessVpnVO> searchForRemoteAccessVpns(ListRemoteAccessVpnsCmd cmd) throws InvalidParameterValueException,
			PermissionDeniedException {
		// do some parameter validation
        Account account = UserContext.current().getAccount();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long accountId = null;
        Account ipAddressOwner = null;
        String ipAddress = cmd.getPublicIp();

        if (ipAddress != null) {
            IPAddressVO ipAddressVO = _publicIpAddressDao.findById(ipAddress);
            if (ipAddressVO == null) {
                throw new InvalidParameterValueException("Unable to list remote access vpns, IP address " + ipAddress + " not found.");
            } else {
                Long ipAddrAcctId = ipAddressVO.getAccountId();
                if (ipAddrAcctId == null) {
                    throw new InvalidParameterValueException("Unable to list remote access vpns, IP address " + ipAddress + " is not associated with an account.");
                }
                ipAddressOwner = _accountDao.findById(ipAddrAcctId);
            }
        }

        if ((account == null) || isAdmin(account.getType())) {
            // validate domainId before proceeding
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Unable to list remote access vpns for domain id " + domainId + ", permission denied.");
                }
                if (accountName != null) {
                    Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                    if (userAccount != null) {
                        accountId = userAccount.getId();
                    } else {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                }
            } else if (ipAddressOwner != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), ipAddressOwner.getDomainId())) {
                    throw new PermissionDeniedException("Unable to list remote access vpn  for IP address " + ipAddress + ", permission denied.");
                }
            } else {
                domainId = ((account == null) ? DomainVO.ROOT_DOMAIN : account.getDomainId());
            }
        } else {
            accountId = account.getId();
        }

        Filter searchFilter = new Filter(RemoteAccessVpnVO.class, "vpnServerAddress", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object id = cmd.getId();
        Object zoneId = cmd.getZoneId();
        

        SearchBuilder<RemoteAccessVpnVO> sb = _remoteAccessVpnDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("zoneId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("ipAddress", sb.entity().getVpnServerAddress(), SearchCriteria.Op.EQ);

        if ((accountId == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<RemoteAccessVpnVO> sc = sb.create();
       
        if (id != null) {
            sc.setParameters("id", id);
        }

        if (ipAddress != null) {
            sc.setParameters("ipAddress", ipAddress);
        }
        
        if (zoneId != null) {
        	sc.setParameters("zoneId", zoneId);
        }

        if (accountId != null) {
            sc.setParameters("accountId", accountId);
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
        }

        return _remoteAccessVpnDao.search(sc, searchFilter);
	}

	@Override
	public List<VpnUserVO> searchForVpnUsers(ListVpnUsersCmd cmd) {
		Account account = UserContext.current().getAccount();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long accountId = null;
        String username = cmd.getUsername();


        if ((account == null) || isAdmin(account.getType())) {
            // validate domainId before proceeding
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Unable to list remote access vpn users for domain id " + domainId + ", permission denied.");
                }
                if (accountName != null) {
                    Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                    if (userAccount != null) {
                        accountId = userAccount.getId();
                    } else {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                }
            } else {
                domainId = ((account == null) ? DomainVO.ROOT_DOMAIN : account.getDomainId());
            }
        } else {
            accountId = account.getId();
        }

        Filter searchFilter = new Filter(VpnUserVO.class, "username", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object id = cmd.getId();
        

        SearchBuilder<VpnUserVO> sb = _vpnUsersDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("username", sb.entity().getUsername(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);

        if ((accountId == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<VpnUserVO> sc = sb.create();
       
        if (id != null) {
            sc.setParameters("id", id);
        }

        if (username != null) {
            sc.setParameters("username", username);
        }
        

        if (accountId != null) {
            sc.setParameters("accountId", accountId);
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
        }

        return _vpnUsersDao.search(sc, searchFilter);
	}
}
