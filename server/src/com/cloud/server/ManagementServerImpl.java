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

import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
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
import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.alert.AlertManager;
import com.cloud.alert.AlertVO;
import com.cloud.alert.dao.AlertDao;
import com.cloud.api.BaseCmd;
import com.cloud.api.commands.AssociateIPAddrCmd;
import com.cloud.api.commands.AuthorizeNetworkGroupIngressCmd;
import com.cloud.api.commands.CancelMaintenanceCmd;
import com.cloud.api.commands.CopyTemplateCmd;
import com.cloud.api.commands.CreatePortForwardingServiceRuleCmd;
import com.cloud.api.commands.CreateTemplateCmd;
import com.cloud.api.commands.CreateVolumeCmd;
import com.cloud.api.commands.DeleteIsoCmd;
import com.cloud.api.commands.DeleteTemplateCmd;
import com.cloud.api.commands.DeleteUserCmd;
import com.cloud.api.commands.DeployVMCmd;
import com.cloud.api.commands.PrepareForMaintenanceCmd;
import com.cloud.api.commands.ReconnectHostCmd;
import com.cloud.api.commands.StartRouterCmd;
import com.cloud.api.commands.StartSystemVMCmd;
import com.cloud.api.commands.StartVMCmd;
import com.cloud.api.commands.UpgradeVMCmd;
import com.cloud.async.AsyncInstanceCreateStatus;
import com.cloud.async.AsyncJobExecutor;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.async.dao.AsyncJobDao;
import com.cloud.async.executor.AssociateIpAddressParam;
import com.cloud.async.executor.AttachISOParam;
import com.cloud.async.executor.CopyTemplateParam;
import com.cloud.async.executor.CreateOrUpdateRuleParam;
import com.cloud.async.executor.CreatePrivateTemplateParam;
import com.cloud.async.executor.DeleteDomainParam;
import com.cloud.async.executor.DeleteRuleParam;
import com.cloud.async.executor.DeleteTemplateParam;
import com.cloud.async.executor.DeployVMParam;
import com.cloud.async.executor.DisassociateIpAddressParam;
import com.cloud.async.executor.LoadBalancerParam;
import com.cloud.async.executor.NetworkGroupIngressParam;
import com.cloud.async.executor.ResetVMPasswordParam;
import com.cloud.async.executor.SecurityGroupParam;
import com.cloud.async.executor.UpdateLoadBalancerParam;
import com.cloud.async.executor.UpgradeVMParam;
import com.cloud.async.executor.VMOperationParam;
import com.cloud.async.executor.VolumeOperationParam;
import com.cloud.async.executor.VMOperationParam.VmOp;
import com.cloud.async.executor.VolumeOperationParam.VolumeOp;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ConfigurationVO;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.Vlan.VlanType;
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
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.info.ConsoleProxyInfo;
import com.cloud.network.FirewallRuleVO;
import com.cloud.network.IPAddressVO;
import com.cloud.network.LoadBalancerVMMapVO;
import com.cloud.network.LoadBalancerVO;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkRuleConfigVO;
import com.cloud.network.SecurityGroupVMMapVO;
import com.cloud.network.SecurityGroupVO;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.NetworkRuleConfigDao;
import com.cloud.network.dao.SecurityGroupDao;
import com.cloud.network.dao.SecurityGroupVMMapDao;
import com.cloud.network.security.IngressRuleVO;
import com.cloud.network.security.NetworkGroupManager;
import com.cloud.network.security.NetworkGroupRulesVO;
import com.cloud.network.security.NetworkGroupVO;
import com.cloud.network.security.dao.NetworkGroupDao;
import com.cloud.pricing.PricingVO;
import com.cloud.pricing.dao.PricingDao;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.auth.UserAuthenticator;
import com.cloud.service.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.ServiceOffering.GuestIpType;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.DiskTemplateVO;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.InsufficientStorageCapacityException;
import com.cloud.storage.LaunchPermissionVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotScheduleVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.StorageStats;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeStats;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Snapshot.SnapshotType;
import com.cloud.storage.Storage.FileSystem;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.DiskTemplateDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.LaunchPermissionDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VMTemplateDao.TemplateFilter;
import com.cloud.storage.preallocatedlun.PreallocatedLunVO;
import com.cloud.storage.preallocatedlun.dao.PreallocatedLunDao;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.storage.snapshot.SnapshotScheduler;
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
import com.cloud.utils.DateUtil;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.StringUtils;
import com.cloud.utils.DateUtil.IntervalType;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.MacAddress;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouter;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.State;
import com.cloud.vm.UserVm;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmStats;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.gson.Gson;

public class ManagementServerImpl implements ManagementServer {
    public static final Logger s_logger = Logger.getLogger(ManagementServerImpl.class.getName());

    private final AccountManager _accountMgr;
    private final AgentManager _agentMgr;
    private final ConfigurationManager _configMgr;
    private final FirewallRulesDao _firewallRulesDao;
    private final SecurityGroupDao _securityGroupDao;
	private final NetworkGroupDao _networkSecurityGroupDao;
    private final LoadBalancerDao _loadBalancerDao;
    private final NetworkRuleConfigDao _networkRuleConfigDao;
    private final SecurityGroupVMMapDao _securityGroupVMMapDao;
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
    private final VMTemplateHostDao _templateHostDao;
    private final LaunchPermissionDao _launchPermissionDao;
    private final PricingDao _pricingDao;
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
    private final SnapshotScheduler _snapshotScheduler;
    private final NetworkGroupManager _networkGroupMgr;
    private final int _purgeDelay;
    private final boolean _directAttachNetworkExternalIpAllocator;
    private final PreallocatedLunDao _lunDao;

    private final ScheduledExecutorService _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("AccountChecker"));
    private final ScheduledExecutorService _eventExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("EventChecker"));

    private final StatsCollector _statsCollector;

    private final Map<String, String> _configs;

    private String _domain;
    private int _consoleProxyPort = ConsoleProxyManager.DEFAULT_PROXY_VNC_PORT;
    // private int _consoleProxyUrlPort =
    // ConsoleProxyManager.DEFAULT_PROXY_URL_PORT;

    private final int _routerRamSize;
    private final int _proxyRamSize;
    private final int _ssRamSize;

    private final int _maxVolumeSizeInGb;
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
        _securityGroupDao = locator.getDao(SecurityGroupDao.class);
        _networkSecurityGroupDao  = locator.getDao(NetworkGroupDao.class);
        _loadBalancerDao = locator.getDao(LoadBalancerDao.class);
        _networkRuleConfigDao = locator.getDao(NetworkRuleConfigDao.class);
        _securityGroupVMMapDao = locator.getDao(SecurityGroupVMMapDao.class);
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
        _templateHostDao = locator.getDao(VMTemplateHostDao.class);
        _launchPermissionDao = locator.getDao(LaunchPermissionDao.class);
        _pricingDao = locator.getDao(PricingDao.class);
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
        _vmDao = locator.getDao(UserVmDao.class);

        _configs = _configDao.getConfiguration();
        _userStatsDao = locator.getDao(UserStatisticsDao.class);
        _vmInstanceDao = locator.getDao(VMInstanceDao.class);
        _volumeDao = locator.getDao(VolumeDao.class);
        _diskTemplateDao = locator.getDao(DiskTemplateDao.class);
        _alertMgr = locator.getManager(AlertManager.class);
        _asyncMgr = locator.getManager(AsyncJobManager.class);
        _tmpltMgr = locator.getManager(TemplateManager.class);
        _snapMgr = locator.getManager(SnapshotManager.class);
        _snapshotScheduler = locator.getManager(SnapshotScheduler.class);
        _networkGroupMgr = locator.getManager(NetworkGroupManager.class);
        
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

        String value = _configs.get("consoleproxy.port");
        if (value != null)
            _consoleProxyPort = NumbersUtil.parseInt(value, ConsoleProxyManager.DEFAULT_PROXY_VNC_PORT);

        // value = _configs.get("consoleproxy.url.port");
        // if(value != null)
        // _consoleProxyUrlPort = NumbersUtil.parseInt(value,
        // ConsoleProxyManager.DEFAULT_PROXY_URL_PORT);

        value = _configs.get("account.cleanup.interval");
        int cleanup = NumbersUtil.parseInt(value, 60 * 60 * 24); // 1 hour.

        // Parse the max number of UserVMs and public IPs from server-setup.xml,
        // and set them in the right places

        String maxVolumeSizeInGbString = _configs.get("max.volume.size.gb");
        int maxVolumeSizeGb = NumbersUtil.parseInt(maxVolumeSizeInGbString, 2000);

        _maxVolumeSizeInGb = maxVolumeSizeGb;

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
 		
		String hypervisorType = _configDao.getValue("hypervisor.type");
        _isHypervisorSnapshotCapable  = hypervisorType.equals(Hypervisor.Type.XenServer.name());
    }

    protected Map<String, String> getConfigs() {
        return _configs;
    }

    @Override
    public List<? extends Host> discoverHosts(long dcId, Long podId, Long clusterId, String url, String username, String password) throws IllegalArgumentException, DiscoveryException {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unable to convert the url" + url, e);
        }
        // TODO: parameter checks.
        return _agentMgr.discoverHosts(dcId, podId, clusterId, uri, username, password);
    }

    @Override
    public StorageStats getStorageStatistics(long hostId) {
        return _statsCollector.getStorageStats(hostId);
    }
    
    @Override
    public GuestOSCategoryVO getHostGuestOSCategory(long hostId) {
    	Long guestOSCategoryID = _agentMgr.getGuestOSCategoryId(hostId);
    	
    	if (guestOSCategoryID != null) {
    		return _guestOSCategoryDao.findById(guestOSCategoryID);
    	} else {
    		return null;
    	}
    }
    
    @Override
    public PreallocatedLunVO registerPreallocatedLun(String targetIqn, String portal, int lun, long size, long dcId, String t) {
        String[] tags = null;
        if (t != null) {
            tags = t.split(",");
            for (int i = 0; i < tags.length; i++) {
                tags[i] = tags[i].trim();
            }
        } else {
            tags = new String[0];
        }
        
        PreallocatedLunVO vo = new PreallocatedLunVO(dcId, portal, targetIqn, lun, size);
        return _lunDao.persist(vo, tags);
    }
    
    @Override
    public boolean unregisterPreallocatedLun(long id) throws IllegalArgumentException {
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
    public VmStats getVmStatistics(long hostId) {
        return _statsCollector.getVmStats(hostId);
    }

    @Override
    public VolumeStats[] getVolumeStatistics(long[] volIds) {
        return _statsCollector.getVolumeStats(volIds);
    }

    @Override
    public User createUserAPI(String username, String password, String firstName, String lastName, Long domainId, String accountName, short userType, String email, String timezone) {
        Long accountId = null;
        try {
            if (accountName == null) {
                accountName = username;
            }
            if (domainId == null) {
                domainId = DomainVO.ROOT_DOMAIN;
            }

            Account account = _accountDao.findActiveAccount(accountName, domainId);
            if (account != null) {
                if (account.getType() != userType) {
                    throw new CloudRuntimeException("Account " + accountName + " is not the correct account type for user " + username);
                }
                accountId = account.getId();
            }

            if (!_userAccountDao.validateUsernameInDomain(username, domainId)) {
                throw new CloudRuntimeException("The user " + username + " already exists in domain " + domainId);
            }

            if (accountId == null) {
                if ((userType < Account.ACCOUNT_TYPE_NORMAL) || (userType > Account.ACCOUNT_TYPE_READ_ONLY_ADMIN)) {
                    throw new CloudRuntimeException("Invalid account type " + userType + " given; unable to create user");
                }

                // create a new account for the user
                AccountVO newAccount = new AccountVO();
                if (domainId == null) {
                    // root domain is default
                    domainId = DomainVO.ROOT_DOMAIN;
                }

                if ((domainId != DomainVO.ROOT_DOMAIN) && (userType == Account.ACCOUNT_TYPE_ADMIN)) {
                    throw new CloudRuntimeException("Invalid account type " + userType + " given for an account in domain " + domainId + "; unable to create user.");
                }

                newAccount.setAccountName(accountName);
                newAccount.setDomainId(domainId);
                newAccount.setType(userType);
                newAccount.setState("enabled");
                newAccount = _accountDao.persist(newAccount);
                accountId = newAccount.getId();
            }

            if (accountId == null) {
                throw new CloudRuntimeException("Failed to create account for user: " + username + "; unable to create user");
            }

            UserVO user = new UserVO();
            user.setUsername(username);
            user.setPassword(password);
            user.setState("enabled");
            user.setFirstname(firstName);
            user.setLastname(lastName);
            user.setAccountId(accountId.longValue());
            user.setEmail(email);
            user.setTimezone(timezone);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Creating user: " + username + ", account: " + accountName + " (id:" + accountId + "), domain: " + domainId + " timezone:"+ timezone);
            }

            UserVO dbUser = _userDao.persist(user);
            
            _networkGroupMgr.createDefaultNetworkGroup(accountId);

            if (!user.getPassword().equals(dbUser.getPassword())) {
                throw new CloudRuntimeException("The user " + username + " being creating is using a password that is different than what's in the db");
            }

            saveEvent(new Long(1), new Long(1), EventVO.LEVEL_INFO, EventTypes.EVENT_USER_CREATE, "User, " + username + " for accountId = " + accountId
                    + " and domainId = " + domainId + " was created.");
            return dbUser;
        } catch (Exception e) {
            saveEvent(new Long(1), new Long(1), EventVO.LEVEL_ERROR, EventTypes.EVENT_USER_CREATE, "Error creating user, " + username + " for accountId = " + accountId
                    + " and domainId = " + domainId);
            if (e instanceof CloudRuntimeException) {
                s_logger.info("unable to create user: " + e);
            } else {
                s_logger.warn("unknown exception creating user", e);
            }
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    @Override
    public boolean prepareForMaintenance(long hostId) {
        try {
            return _agentMgr.maintain(hostId);
        } catch (AgentUnavailableException e) {
            return false;
        }
    }

    @Override
    public long prepareForMaintenanceAsync(long hostId) throws InvalidParameterValueException {
    	HostVO host = _hostDao.findById(hostId);
    	
    	if (host == null) {
            s_logger.debug("Unable to find host " + hostId);
            throw new InvalidParameterValueException("Unable to find host with ID: " + hostId + ". Please specify a valid host ID.");
        }
        
        if (_hostDao.countBy(host.getClusterId(), Status.PrepareForMaintenance, Status.ErrorInMaintenance, Status.Maintenance) > 0) {
            throw new InvalidParameterValueException("There are other servers in maintenance mode.");
        }
        
        if (_storageMgr.isLocalStorageActiveOnHost(host)) {
        	throw new InvalidParameterValueException("There are active VMs using the host's local storage pool. Please stop all VMs on this host that use local storage.");
        }
    	
        Long param = new Long(hostId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
        job.setCmd("PrepareMaintenance");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(PrepareForMaintenanceCmd.getResultObjectName());
        return _asyncMgr.submitAsyncJob(job);
    }

    @Override
    public boolean maintenanceCompleted(long hostId) {
        return _agentMgr.cancelMaintenance(hostId);
    }

    @Override
    public long maintenanceCompletedAsync(long hostId) {
        Long param = new Long(hostId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
        job.setCmd("CompleteMaintenance");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(CancelMaintenanceCmd.getResultObjectName());
        return _asyncMgr.submitAsyncJob(job);
    }

    @Override
    public User createUser(String username, String password, String firstName, String lastName, Long domain, String accountName, short userType, String email, String timezone) {
        return createUserAPI(username, StringToMD5(password), firstName, lastName, domain, accountName, userType, email, timezone);
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
    public boolean deleteUser(long userId) {
        UserAccount userAccount = null;
        Long accountId = null;
        String username = null;
        try {
            UserVO user = _userDao.findById(userId);
            if (user == null || user.getRemoved() != null) {
                return true;
            }
            username = user.getUsername();
            boolean result = _userDao.remove(userId);
            if (!result) {
                s_logger.error("Unable to remove the user with id: " + userId + "; username: " + user.getUsername());
                return false;
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("User is removed, id: " + userId + "; username: " + user.getUsername());
            }

            accountId = user.getAccountId();
            userAccount = _userAccountDao.findById(userId);

            List<UserVO> users = _userDao.listByAccount(accountId);
            if (users.size() != 0) {
                s_logger.debug("User (" + userId + "/" + user.getUsername() + ") is deleted but there's still other users in the account so not deleting account.");
                return true;
            }

            result = _accountDao.remove(accountId);
            if (!result) {
                s_logger.error("Unable to delete account " + accountId);
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Remove account " + accountId);
            }

            AccountVO account = _accountDao.findById(accountId);
            deleteAccount(account);
            saveEvent(Long.valueOf(1), Long.valueOf(1), EventVO.LEVEL_INFO, EventTypes.EVENT_USER_DELETE, "User " + username + " (id: " + userId
                    + ") for accountId = " + accountId + " and domainId = " + userAccount.getDomainId() + " was deleted.");
            return true;
        } catch (Exception e) {
            s_logger.error("exception deleting user: " + userId, e);
            long domainId = 0L;
            if (userAccount != null)
                domainId = userAccount.getDomainId();
            saveEvent(Long.valueOf(1), Long.valueOf(1), EventVO.LEVEL_INFO, EventTypes.EVENT_USER_DELETE, "Error deleting user " + username + " (id: " + userId
                    + ") for accountId = " + accountId + " and domainId = " + domainId);
            return false;
        }
    }

    @Override
    public long deleteUserAsync(long userId) {
        Long param = new Long(userId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
        job.setCmd("DeleteUser");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(DeleteUserCmd.getStaticName());

        return _asyncMgr.submitAsyncJob(job);
    }

    public boolean deleteAccount(AccountVO account) {
        long accountId = account.getId();
        long userId = 1L; // only admins can delete users, pass in userId 1 XXX: Shouldn't it be userId 2.
        boolean accountCleanupNeeded = false;
        
        try {
            // Delete the snapshots dir for the account. Have to do this before destroying the VMs.
            boolean success = _snapMgr.deleteSnapshotDirsForAccount(accountId);
            if (success) {
                s_logger.debug("Successfully deleted snapshots directories for all volumes under account " + accountId + " across all zones");
            }
            // else, there are no snapshots, hence no directory to delete.
            
            // Destroy the account's VMs
            List<UserVmVO> vms = _userVmDao.listByAccountId(accountId);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Destroying # of vms (accountId=" + accountId + "): " + vms.size());
            }

            for (UserVmVO vm : vms) {
                if (!_vmMgr.destroyVirtualMachine(userId, vm.getId())) {
                    s_logger.error("Unable to destroy vm: " + vm.getId());
                    accountCleanupNeeded = true;
                } else {
                	//_vmMgr.releaseGuestIpAddress(vm); FIXME FIXME bug 5561
                }
            }
            
            // Mark the account's volumes as destroyed
            List<VolumeVO> volumes = _volumeDao.findDetachedByAccount(accountId);
            for (VolumeVO volume : volumes) {
            	if(volume.getPoolId()==null){
            		accountCleanupNeeded = true;
            	}
            	_storageMgr.destroyVolume(volume);
            }

            // Destroy the account's routers
            List<DomainRouterVO> routers = _routerDao.listBy(accountId);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Destroying # of routers (accountId=" + accountId + "): " + routers.size());
            }

            boolean routersCleanedUp = true;
            for (DomainRouterVO router : routers) {
                if (!_networkMgr.destroyRouter(router.getId())) {
                    s_logger.error("Unable to destroy router: " + router.getId());
                    routersCleanedUp = false;
                }
            }

            if (routersCleanedUp) {
            	List<IPAddressVO> ips = _publicIpAddressDao.listByAccount(accountId);
            	
                if (s_logger.isDebugEnabled()) {
            		s_logger.debug("Found " + ips.size() + " public IP addresses for account with ID " + accountId);
                }

            	for (IPAddressVO ip : ips) {
            		Long podId = getPodIdForVlan(ip.getVlanDbId());
            		if (podId != null) {
            			continue;//bug 5561 do not release direct attach pod ips until vm is destroyed
            		}
            		if (!_networkMgr.releasePublicIpAddress(User.UID_SYSTEM, ip.getAddress())) {
            			s_logger.error("Unable to release IP: " + ip.getAddress());
                        accountCleanupNeeded = true;
                    } else {
                    	_accountMgr.decrementResourceCount(accountId, ResourceType.public_ip);
                    }
                }
            } else {
            	accountCleanupNeeded = true;
            }
            
            List<SecurityGroupVO> securityGroups = _securityGroupDao.listByAccountId(accountId);
            if (securityGroups != null) {
                for (SecurityGroupVO securityGroup : securityGroups) {
                    // All vm instances have been destroyed, delete the security group -> instance_id mappings
                    SearchCriteria sc = _securityGroupVMMapDao.createSearchCriteria();
                    sc.addAnd("securityGroupId", SearchCriteria.Op.EQ, securityGroup.getId());
                    _securityGroupVMMapDao.delete(sc);

                    // now clean the network rules and security groups themselves
                    _networkRuleConfigDao.deleteBySecurityGroup(securityGroup.getId().longValue());
                    _securityGroupDao.remove(securityGroup.getId());
                }
            }
            
            // Delete the account's VLANs
            List<VlanVO> accountVlans = _vlanDao.listVlansForAccountByType(null, accountId, VlanType.DirectAttached);
            boolean allVlansDeleted = true;
            for (VlanVO vlan : accountVlans) {
            	try {
            		allVlansDeleted = _configMgr.deleteVlanAndPublicIpRange(User.UID_SYSTEM, vlan.getId());
            	} catch (InvalidParameterValueException e) {
            		allVlansDeleted = false;
            	}
            }

            if (!allVlansDeleted) {
            	accountCleanupNeeded = true;
            }
            
            // clean up templates
            List<VMTemplateVO> userTemplates = _templateDao.listByAccountId(accountId);
            boolean allTemplatesDeleted = true;
            for (VMTemplateVO template : userTemplates) {
            	try {
            		allTemplatesDeleted = _tmpltMgr.delete(userId, template.getId(), null, 0);
            	} catch (InternalErrorException e) {
            		s_logger.warn("Failed to delete template while removing account: " + template.getName() + " due to: " + e.getMessage());
            		allTemplatesDeleted = false;
            	}
            }
            
            if (!allTemplatesDeleted) {
            	accountCleanupNeeded = true;
            }

            return true;
        } finally {
            s_logger.info("Cleanup for account " + account.getId() + (accountCleanupNeeded ? " is needed." : " is not needed."));
            
            if (accountCleanupNeeded) {
            	_accountDao.markForCleanup(accountId);
            }
        }
    }

    @Override
    public boolean disableUser(long userId) {
        if (userId <= 2) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("disableUser -- invalid user id: " + userId);
            }
            return false;
        }

        return doSetUserStatus(userId, Account.ACCOUNT_STATE_DISABLED);
    }

    @Override
    public long disableUserAsync(long userId) {
        Long param = new Long(userId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
        job.setCmd("DisableUser");
        job.setCmdInfo(gson.toJson(param));
        
        return _asyncMgr.submitAsyncJob(job);
    }

    @Override
    public boolean enableUser(long userId) {
        boolean success = false;
        success = doSetUserStatus(userId, Account.ACCOUNT_STATE_ENABLED);

        // make sure the account is enabled too
        UserVO user = _userDao.findById(userId);
        if (user != null) {
            success = (success && enableAccount(user.getAccountId()));
        } else {
            s_logger.warn("Unable to find user with id: " + userId);
        }
        return success;
    }

    @Override
    public boolean lockUser(long userId) {
        boolean success = false;

        // make sure the account is enabled too
        UserVO user = _userDao.findById(userId);
        if (user != null) {
            // if the user is either locked already or disabled already, don't change state...only lock currently enabled users
            if (user.getState().equals(Account.ACCOUNT_STATE_LOCKED)) {
                // already locked...no-op
                return true;
            } else if (user.getState().equals(Account.ACCOUNT_STATE_ENABLED)) {
                success = doSetUserStatus(userId, Account.ACCOUNT_STATE_LOCKED);

                boolean lockAccount = true;
                List<UserVO> allUsersByAccount = _userDao.listByAccount(user.getAccountId());
                for (UserVO oneUser : allUsersByAccount) {
                    if (oneUser.getState().equals(Account.ACCOUNT_STATE_ENABLED)) {
                        lockAccount = false;
                        break;
                    }
                }

                if (lockAccount) {
                    success = (success && lockAccount(user.getAccountId()));
                }
            } else {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Attempting to lock a non-enabled user, current state is " + user.getState() + " (userId: " + userId + "), locking failed.");
                }
            }
        } else {
            s_logger.warn("Unable to find user with id: " + userId);
        }
        return success;
    }

    private boolean doSetUserStatus(long userId, String state) {
        UserVO userForUpdate = _userDao.createForUpdate();
        userForUpdate.setState(state);
        return _userDao.update(Long.valueOf(userId), userForUpdate);
    }

    @Override
    public boolean disableAccount(long accountId) {
        boolean success = false;
        if (accountId <= 2) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("disableAccount -- invalid account id: " + accountId);
            }
            return false;
        }

        AccountVO account = _accountDao.findById(accountId);
        if ((account == null) || account.getState().equals(Account.ACCOUNT_STATE_DISABLED)) {
            success = true;
        } else {
            AccountVO acctForUpdate = _accountDao.createForUpdate();
            acctForUpdate.setState(Account.ACCOUNT_STATE_DISABLED);
            success = _accountDao.update(Long.valueOf(accountId), acctForUpdate);

            success = (success && doDisableAccount(accountId));
        }
        return success;
    }

    @Override
    public long disableAccountAsync(long accountId) {
        Long param = new Long(accountId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
        job.setCmd("DisableAccount");
        job.setCmdInfo(gson.toJson(param));
        
        return _asyncMgr.submitAsyncJob(job);
    }

    @Override
    public boolean updateAccount(long accountId, String accountName) {
        boolean success = false;
        AccountVO account = _accountDao.findById(accountId);
        if ((account == null) || (account.getAccountName().equals(accountName))) {
            success = true;
        } else {
            AccountVO acctForUpdate = _accountDao.createForUpdate();
            acctForUpdate.setAccountName(accountName);
            success = _accountDao.update(Long.valueOf(accountId), acctForUpdate);
        }
        return success;
    }

    private boolean doDisableAccount(long accountId) {
        List<UserVmVO> vms = _userVmDao.listByAccountId(accountId);
        boolean success = true;
        for (UserVmVO vm : vms) {
            try {
                success = (success && _vmMgr.stop(vm, 0));
            } catch (AgentUnavailableException aue) {
                s_logger.warn("Agent running on host " + vm.getHostId() + " is unavailable, unable to stop vm " + vm.getName());
                success = false;
            }
        }

        List<DomainRouterVO> routers = _routerDao.listBy(accountId);
        for (DomainRouterVO router : routers) {
            success = (success && _networkMgr.stopRouter(router.getId(), 0));
        }

        return success;
    }

    @Override
    public boolean enableAccount(long accountId) {
        boolean success = false;
        AccountVO acctForUpdate = _accountDao.createForUpdate();
        acctForUpdate.setState(Account.ACCOUNT_STATE_ENABLED);
        success = _accountDao.update(Long.valueOf(accountId), acctForUpdate);
        return success;
    }

    @Override
    public boolean lockAccount(long accountId) {
        boolean success = false;
        Account account = _accountDao.findById(accountId);
        if (account != null) {
            if (account.getState().equals(Account.ACCOUNT_STATE_LOCKED)) {
                return true; // already locked, no-op
            } else if (account.getState().equals(Account.ACCOUNT_STATE_ENABLED)) {
                AccountVO acctForUpdate = _accountDao.createForUpdate();
                acctForUpdate.setState(Account.ACCOUNT_STATE_LOCKED);
                success = _accountDao.update(Long.valueOf(accountId), acctForUpdate);
            } else {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Attempting to lock a non-enabled account, current state is " + account.getState() + " (accountId: " + accountId + "), locking failed.");
                }
            }
        } else {
            s_logger.warn("Failed to lock account " + accountId + ", account not found.");
        }
        return success;
    }


    private Long saveEvent(Long userId, Long accountId, String type, String description) {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setDescription(description);
        event = _eventDao.persist(event);
        return event.getId();
    }
    
    /*
     * Save event after scheduling an async job
     */
    private Long saveScheduledEvent(Long userId, Long accountId, String type, String description) {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setState(EventState.Scheduled);
        event.setDescription("Scheduled async job for "+description);
        event = _eventDao.persist(event);
        return event.getId();
    }
    
    /*
     * Save event after starting execution of an async job
     */
    private Long saveStartedEvent(Long userId, Long accountId, String type, String description, long startEventId) {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setState(EventState.Started);
        event.setDescription(description);
        event.setStartId(startEventId);
        event = _eventDao.persist(event);
    	return event.getId();
    }

    @Override
    public boolean updateUser(long userId, String username, String password, String firstname, String lastname, String email, String timezone, String apiKey, String secretKey) throws InvalidParameterValueException{
        UserVO user = _userDao.findById(userId);
        Long accountId = user.getAccountId();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("updating user with id: " + userId);
        }
        UserAccount userAccount = _userAccountDao.findById(userId);
        try
        {
        	//check if the apiKey and secretKey are globally unique
        	if(apiKey != null && secretKey != null)
        	{
        		Pair<User, Account> apiKeyOwner = findUserByApiKey(apiKey);
        		
        		if(apiKeyOwner != null)
        		{
        			User usr = apiKeyOwner.first();
        			
        			if(usr.getId() != userId)
            			throw new InvalidParameterValueException("The api key:"+apiKey+" exists in the system for user id:"+userId+" ,please provide a unique key");
        			else
        			{
        				//allow the updation to take place
        			}
        		}
        	
        	}

        	
            _userDao.update(userId, username, password, firstname, lastname, email, accountId, timezone, apiKey, secretKey);
            saveEvent(new Long(1), Long.valueOf(1), EventVO.LEVEL_INFO, EventTypes.EVENT_USER_UPDATE, "User, " + username + " for accountId = "
                    + accountId + " domainId = " + userAccount.getDomainId() + " and timezone = "+timezone + " was updated.");
        } catch (Throwable th) {
            s_logger.error("error updating user", th);
            saveEvent(Long.valueOf(1), Long.valueOf(1), EventVO.LEVEL_ERROR, EventTypes.EVENT_USER_UPDATE, "Error updating user, " + username
                    + " for accountId = " + accountId + " and domainId = " + userAccount.getDomainId());
            return false;
        }
        return true;
    }
    
    private Long saveEvent(Long userId, Long accountId, String level, String type, String description) {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setDescription(description);
        event.setLevel(level);
        event = _eventDao.persist(event);
        return event.getId();
    }
    
    private Long saveEvent(Long userId, Long accountId, String level, String type, String description, String params) {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setDescription(description);
        event.setLevel(level);
        event.setParameters(params);
        event = _eventDao.persist(event);
        return event.getId();
    }
    
    private Long saveEvent(Long userId, Long accountId, String level, String type, String description, String params, long startEventId) {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setDescription(description);
        event.setLevel(level);
        event.setParameters(params);
        event.setStartId(startEventId);
        event = _eventDao.persist(event);
        return event.getId();
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
    public String createApiKey(Long userId) {
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

    @Override
    public String createSecretKey(Long userId) {
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

    @DB
    public void associateIpAddressListToAccount(long userId, long accountId, long zoneId, Long vlanId) throws InsufficientAddressCapacityException,
    		InvalidParameterValueException, InternalErrorException {
    	
        Transaction txn = Transaction.currentTxn();
        AccountVO account = null;
        
        try {
            //Acquire Lock                    
            account = _accountDao.acquire(accountId);
            if (account == null) {
                s_logger.warn("Unable to lock account: " + accountId);
                throw new InternalErrorException("Unable to acquire account lock");
            }            
            s_logger.debug("Associate IP address lock acquired");
            
            //Get Router
            DomainRouterVO router = _routerDao.findBy(accountId, zoneId);
            if (router == null) {
                s_logger.debug("No router found for account: " + account.getAccountName() + ".");
                return;
            }
            
            if (router.getState() == State.Running) {
	            //Get Vlans associated with the account
            	List<VlanVO> vlansForAccount = new ArrayList<VlanVO>();
            	if (vlanId == null){
            		vlansForAccount.addAll(_vlanDao.listVlansForAccountByType(zoneId, account.getId(), VlanType.VirtualNetwork));
            		s_logger.debug("vlansForAccount "+ vlansForAccount);
            	}else{
            		vlansForAccount.add(_vlanDao.findById(vlanId));
            	}
		         
	            // Creating a list of all the ips that can be assigned to this account
		        txn.start();
		        List<String> ipAddrsList = new ArrayList<String>();
		     	for (VlanVO vlan : vlansForAccount){
		     		ipAddrsList.addAll(_publicIpAddressDao.assignAcccountSpecificIps(accountId, account.getDomainId().longValue(), vlan.getId(), false));
		     				     		
		     		long size = ipAddrsList.size();
		     		_accountMgr.incrementResourceCount(accountId, ResourceType.public_ip, size);
		     		s_logger.debug("Assigning new ip addresses " +ipAddrsList);		     		
		     	}
		     	if(ipAddrsList.isEmpty())
		     		return;
		     	
		     	// Associate the IP's to DomR
		     	boolean success = true;
		     	String params = "\nsourceNat=" + false + "\ndcId=" + zoneId;
		     	ArrayList<String> dummyipAddrList = new ArrayList<String>();
		     	success = _networkMgr.associateIP(router,ipAddrsList, true);
		     	String errorMsg = "Unable to assign public IP address pool";
            	if (!success) {
            		s_logger.debug(errorMsg);
            		 for(String ip : ipAddrsList){
            			 saveEvent(userId, accountId, EventVO.LEVEL_ERROR, EventTypes.EVENT_NET_IP_ASSIGN, "Unable to assign public IP " +ip, params);
                     }
            		throw new InternalErrorException(errorMsg);
            	}
                txn.commit();
                for(String ip : ipAddrsList){
                	saveEvent(userId, accountId, EventVO.LEVEL_INFO, EventTypes.EVENT_NET_IP_ASSIGN, "Successfully assigned account IP " +ip, params);
                }
            }
            } catch (InternalErrorException iee) {
                s_logger.error("Associate IP threw an InternalErrorException.", iee);
                throw iee;
            } catch (Throwable t) {
                s_logger.error("Associate IP address threw an exception.", t);
                throw new InternalErrorException("Associate IP address exception");
            } finally {
                if (account != null) {
                    _accountDao.release(accountId);
                    s_logger.debug("Associate IP address lock released");
                }
            }
    
    }
    
    @Override
    @DB
    public String associateIpAddress(long userId, long accountId, long domainId, long zoneId) throws ResourceAllocationException, InsufficientAddressCapacityException,
            InvalidParameterValueException, InternalErrorException {
        Transaction txn = Transaction.currentTxn();
        AccountVO account = null;
        try {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Associate IP address called for user " + userId + " account " + accountId);
            }
            account = _accountDao.acquire(accountId);

            if (account == null) {
                s_logger.warn("Unable to lock account: " + accountId);
                throw new InternalErrorException("Unable to acquire account lock");
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Associate IP address lock acquired");
            }

            // Check that the maximum number of public IPs for the given
            // accountId will not be exceeded
            if (_accountMgr.resourceLimitExceeded(account, ResourceType.public_ip)) {
                ResourceAllocationException rae = new ResourceAllocationException("Maximum number of public IP addresses for account: " + account.getAccountName()
                        + " has been exceeded.");
                rae.setResourceType("ip");
                throw rae;
            }

            DomainRouterVO router = _routerDao.findBy(accountId, zoneId);
            if (router == null) {
                throw new InvalidParameterValueException("No router found for account: " + account.getAccountName() + ".");
            }

            txn.start();

            String ipAddress = null;
            Pair<String, VlanVO> ipAndVlan = _vlanDao.assignIpAddress(zoneId, accountId, domainId, VlanType.VirtualNetwork, false);
            
            if (ipAndVlan == null) {
                throw new InsufficientAddressCapacityException("Unable to find available public IP addresses");
            } else {
            	ipAddress = ipAndVlan.first();
            	_accountMgr.incrementResourceCount(accountId, ResourceType.public_ip);
            }

            boolean success = true;
            String errorMsg = "";

            List<String> ipAddrs = new ArrayList<String>();
            ipAddrs.add(ipAddress);

            if (router.getState() == State.Running) {
                success = _networkMgr.associateIP(router, ipAddrs, true);
                if (!success) {
                    errorMsg = "Unable to assign public IP address.";
                }
            }

            EventVO event = new EventVO();
            event.setUserId(userId);
            event.setAccountId(accountId);
            event.setType(EventTypes.EVENT_NET_IP_ASSIGN);
            event.setParameters("address=" + ipAddress + "\nsourceNat=" + false + "\ndcId=" + zoneId);

            if (!success) {
                _publicIpAddressDao.unassignIpAddress(ipAddress);
                ipAddress = null;
                _accountMgr.decrementResourceCount(accountId, ResourceType.public_ip);

                event.setLevel(EventVO.LEVEL_ERROR);
                event.setDescription(errorMsg);
                _eventDao.persist(event);
                txn.commit();

                throw new InternalErrorException(errorMsg);
            } else {
                event.setDescription("Assigned a public IP address: " + ipAddress);
                _eventDao.persist(event);
            }

            txn.commit();
            return ipAddress;

        } catch (ResourceAllocationException rae) {
            s_logger.error("Associate IP threw a ResourceAllocationException.", rae);
            throw rae;
        } catch (InsufficientAddressCapacityException iace) {
            s_logger.error("Associate IP threw an InsufficientAddressCapacityException.", iace);
            throw iace;
        } catch (InvalidParameterValueException ipve) {
            s_logger.error("Associate IP threw an InvalidParameterValueException.", ipve);
            throw ipve;
        } catch (InternalErrorException iee) {
            s_logger.error("Associate IP threw an InternalErrorException.", iee);
            throw iee;
        } catch (Throwable t) {
            s_logger.error("Associate IP address threw an exception.", t);
            throw new InternalErrorException("Associate IP address exception");
        } finally {
            if (account != null) {
                _accountDao.release(accountId);
                s_logger.debug("Associate IP address lock released");
            }
        }
    }

    @Override
    public long associateIpAddressAsync(long userId, long accountId, long domainId, long zoneId) {
        AssociateIpAddressParam param = new AssociateIpAddressParam(userId, accountId, domainId, zoneId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(accountId);
        job.setCmd("AssociateIpAddress");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(AssociateIPAddrCmd.getResultObjectName());
        
        return _asyncMgr.submitAsyncJob(job, true);
    }

    @Override
    @DB
    public boolean disassociateIpAddress(long userId, long accountId, String publicIPAddress) throws PermissionDeniedException, IllegalArgumentException {
        Transaction txn = Transaction.currentTxn();
        try {
            IPAddressVO ipVO = _publicIpAddressDao.findById(publicIPAddress);
            if (ipVO == null) {
                return false;
            }

            if (ipVO.getAllocated() == null) {
                return true;
            }

            AccountVO accountVO = _accountDao.findById(accountId);
            if (accountVO == null) {
                return false;
            }
          
            if ((ipVO.getAccountId() == null) || (ipVO.getAccountId().longValue() != accountId)) {
                // FIXME: is the user visible in the admin account's domain????
                if (!BaseCmd.isAdmin(accountVO.getType())) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("permission denied disassociating IP address " + publicIPAddress + "; acct: " + accountId + "; ip (acct / dc / dom / alloc): "
                                + ipVO.getAccountId() + " / " + ipVO.getDataCenterId() + " / " + ipVO.getDomainId() + " / " + ipVO.getAllocated());
                    }
                    throw new PermissionDeniedException("User/account does not own supplied address");
                }
            }

            if (ipVO.getAllocated() == null) {
                return true;
            }

            if (ipVO.isSourceNat()) {
                throw new IllegalArgumentException("ip address is used for source nat purposes and can not be disassociated.");
            }
            
            VlanVO vlan = _vlanDao.findById(ipVO.getVlanDbId());
            if (!vlan.getVlanType().equals(VlanType.VirtualNetwork)) {
            	throw new IllegalArgumentException("only ip addresses that belong to a virtual network may be disassociated.");
            }
			
			//Check for account wide pool. It will have an entry for account_vlan_map. 
            if (_accountVlanMapDao.findAccountVlanMap(accountId,ipVO.getVlanDbId()) != null){
            	throw new PermissionDeniedException(publicIPAddress + " belongs to Account wide IP pool and cannot be disassociated");
            }
			
            txn.start();
            boolean success = _networkMgr.releasePublicIpAddress(userId, publicIPAddress);
            if (success)
            	_accountMgr.decrementResourceCount(accountId, ResourceType.public_ip);
            txn.commit();
            return success;

        } catch (PermissionDeniedException pde) {
            throw pde;
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Throwable t) {
            s_logger.error("Disassociate IP address threw an exception.");
            throw new IllegalArgumentException("Disassociate IP address threw an exception");
        }
    }

    @Override
    public long disassociateIpAddressAsync(long userId, long accountId, String ipAddress) {
        DisassociateIpAddressParam param = new DisassociateIpAddressParam(userId, accountId, ipAddress);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(accountId);
        job.setCmd("DisassociateIpAddress");
        job.setCmdInfo(gson.toJson(param));
        
        return _asyncMgr.submitAsyncJob(job, true);
    }

    @DB
    @Override
    public VlanVO createVlanAndPublicIpRange(long userId, VlanType vlanType, Long zoneId, Long accountId, Long podId, String vlanId, String vlanGateway, String vlanNetmask, String startIP, String endIP) throws Exception{
    	          
		if(accountId != null && vlanType == VlanType.VirtualNetwork){
			long ipResourceLimit = _accountMgr.findCorrectResourceLimit( _accountDao.findById(accountId), ResourceType.public_ip);
			long accountIpRange  = NetUtils.ip2Long(endIP) - NetUtils.ip2Long(startIP) + 1 ;
			s_logger.debug(" IPResourceLimit " +ipResourceLimit + " accountIpRange " + accountIpRange);
			if (ipResourceLimit != -1 && accountIpRange > ipResourceLimit){ // -1 means infinite
				throw new InvalidParameterValueException(" Public IP Resource Limit is set to " + ipResourceLimit + " which is less than the IP range of " + accountIpRange + " provided");
			}
			String params = "\nsourceNat=" + false + "\ndcId=" + zoneId;
			Transaction txn = Transaction.currentTxn();
			try{
				txn.start();
				VlanVO vlan = _configMgr.createVlanAndPublicIpRange(userId, vlanType, zoneId, accountId, podId, vlanId, vlanGateway, vlanNetmask, startIP, endIP);
				associateIpAddressListToAccount(userId, accountId, zoneId, vlan.getId());
				txn.commit();
				return vlan;
			}catch(Exception e){
				txn.rollback();
		    	long startIPLong = NetUtils.ip2Long(startIP);
		    	long endIPLong = NetUtils.ip2Long(endIP);		    	
		        while (startIPLong <= endIPLong) {
		        	saveEvent(userId, accountId, EventVO.LEVEL_ERROR, EventTypes.EVENT_NET_IP_ASSIGN, "Unable to assign public IP " +NetUtils.long2Ip(startIPLong), params);
		        	startIPLong += 1;
		        }
				throw new Exception(e.getMessage());
			}
		}
		return _configMgr.createVlanAndPublicIpRange(userId, vlanType, zoneId, accountId, podId, vlanId, vlanGateway, vlanNetmask, startIP, endIP);
    }

    @Override
    public boolean deleteVlanAndPublicIpRange(long userId, long vlanDbId) throws InvalidParameterValueException {
        return _configMgr.deleteVlanAndPublicIpRange(userId, vlanDbId);
    }

    @Override
    public VolumeVO createVolume(long userId, long accountId, String name, long zoneId, long diskOfferingId, long startEventId) throws InternalErrorException {
        saveStartedEvent(userId, accountId, EventTypes.EVENT_VOLUME_CREATE, "Creating volume", startEventId);
        DataCenterVO zone = _dcDao.findById(zoneId);
        DiskOfferingVO diskOffering = _diskOfferingDao.findById(diskOfferingId);
        VolumeVO createdVolume = _storageMgr.createVolume(accountId, userId, name, zone, diskOffering, startEventId);

        if (createdVolume != null)
            return createdVolume;
        else
            throw new InternalErrorException("Failed to create volume.");
    }

    @Override
    public long createVolumeAsync(long userId, long accountId, String name, long zoneId, long diskOfferingId) throws InvalidParameterValueException, InternalErrorException, ResourceAllocationException {
        // Check that the account is valid
    	AccountVO account = _accountDao.findById(accountId);
    	if (account == null) {
    		throw new InvalidParameterValueException("Please specify a valid account.");
    	}
    	
    	// Check that the zone is valid
        DataCenterVO zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }
        
        // Check that the the disk offering is specified
        DiskOfferingVO diskOffering = _diskOfferingDao.findById(diskOfferingId);
        if ((diskOffering == null) || !DiskOfferingVO.Type.Disk.equals(diskOffering.getType())) {
            throw new InvalidParameterValueException("Please specify a valid disk offering.");
        }
            
        // Check that there is a shared primary storage pool in the specified zone
        List<StoragePoolVO> storagePools = _poolDao.listByDataCenterId(zoneId);
        boolean sharedPoolExists = false;
        for (StoragePoolVO storagePool : storagePools) {
        	if (storagePool.isShared()) {
        		sharedPoolExists = true;
        	}
        }
        
        // Check that there is at least one host in the specified zone
        List<HostVO> hosts = _hostDao.listByDataCenter(zoneId);
        if (hosts.isEmpty()) {
        	throw new InvalidParameterValueException("Please add a host in the specified zone before creating a new volume.");
        }
        
        if (!sharedPoolExists) {
        	throw new InvalidParameterValueException("Please specify a zone that has at least one shared primary storage pool.");
        }
        
        // Check that the resource limit for volumes won't be exceeded
        if (_accountMgr.resourceLimitExceeded(account, ResourceType.volume)) {
        	ResourceAllocationException rae = new ResourceAllocationException("Maximum number of volumes for account: " + account.getAccountName() + " has been exceeded.");
        	rae.setResourceType("volume");
        	throw rae;
        }

        long eventId = saveScheduledEvent(userId, accountId, EventTypes.EVENT_VOLUME_CREATE, "creating volume");
        
        VolumeOperationParam param = new VolumeOperationParam();
        param.setOp(VolumeOp.Create);
        param.setAccountId(accountId);
        param.setUserId(UserContext.current().getUserId());
        param.setName(name);
        param.setZoneId(zoneId);
        param.setDiskOfferingId(diskOfferingId);
        param.setEventId(eventId);

        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(accountId);
        job.setCmd("VolumeOperation");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(CreateVolumeCmd.getResultObjectName());
        
        return _asyncMgr.submitAsyncJob(job);
    }

    @Override
    public long createVolumeFromSnapshotAsync(long userId, long accountId, long snapshotId, String volumeName) throws InternalErrorException, ResourceAllocationException {
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
        AccountVO account = _accountDao.findById(snapshot.getAccountId());
        
        // Check that the resource limit for volumes won't be exceeded
        if (_accountMgr.resourceLimitExceeded(account, ResourceType.volume)) {
            ResourceAllocationException rae = new ResourceAllocationException("Maximum number of volumes for account: " + account.getAccountName() + " has been exceeded.");
            rae.setResourceType("volume");
            throw rae;
        }
        
        return _snapMgr.createVolumeFromSnapshotAsync(userId, accountId, snapshotId, volumeName);
    }

    

    @Override
    public VolumeVO findRootVolume(long vmId) {
        List<VolumeVO> volumes = _volumeDao.findByInstanceAndType(vmId, VolumeType.ROOT);
        if (volumes != null && volumes.size() == 1)
            return volumes.get(0);
        else
            return null;
    }
    
    @Override
    public void destroyVolume(long volumeId) throws InvalidParameterValueException {
        // Check that the volume is valid
        VolumeVO volume = _volumeDao.findById(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Please specify a valid volume ID.");
        }

        // Check that the volume is stored on shared storage
        if (!_storageMgr.volumeOnSharedStoragePool(volume)) {
            throw new InvalidParameterValueException("Please specify a volume that has been created on a shared storage pool.");
        }

        // Check that the volume is not currently attached to any VM
        if (volume.getInstanceId() != null) {
            throw new InvalidParameterValueException("Please specify a volume that is not attached to any VM.");
        }
           
        // Check that the volume is not already destroyed
        if (volume.getDestroyed()) {
            throw new InvalidParameterValueException("Please specify a volume that is not already destroyed.");
        }
        
        // Destroy the volume
        _storageMgr.destroyVolume(volume);
    }

    @Override
    public List<IPAddressVO> listPublicIpAddressesBy(Long accountId, boolean allocatedOnly, Long zoneId, Long vlanDbId) {
        SearchCriteria sc = _publicIpAddressDao.createSearchCriteria();

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
    	return PasswordGenerator.generateRandomPassword();
    }

    @Override
    public boolean resetVMPassword(long userId, long vmId, String password) {
        if (password == null || password.equals("")) {
            return false;
        }
        boolean succeed = _vmMgr.resetVMPassword(userId, vmId, password);

        // Log event
        UserVmVO userVm = _userVmDao.findById(vmId);
        if (userVm != null) {
            if (succeed) {
                saveEvent(userId, userVm.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_VM_RESETPASSWORD, "successfully reset password for VM : " + userVm.getName(), null);
            } else {
                saveEvent(userId, userVm.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_RESETPASSWORD, "unable to reset password for VM : " + userVm.getName(), null);
            }
        } else {
            s_logger.warn("Unable to find vm = " + vmId + " to reset password");
        }
        return succeed;
    }

    @Override
    public void attachVolumeToVM(long vmId, long volumeId, Long deviceId, long startEventId) throws InternalErrorException {
        _vmMgr.attachVolumeToVM(vmId, volumeId, deviceId, startEventId);
    }

    @Override
    public long attachVolumeToVMAsync(long vmId, long volumeId, Long deviceId) throws InvalidParameterValueException {
        VolumeVO volume = _volumeDao.findById(volumeId);

        // Check that the volume is a data volume
        if (volume == null || volume.getVolumeType() != VolumeType.DATADISK) {
            throw new InvalidParameterValueException("Please specify a valid data volume.");
        }

        // Check that the volume is stored on shared storage
        if (!_storageMgr.volumeOnSharedStoragePool(volume)) {
            throw new InvalidParameterValueException("Please specify a volume that has been created on a shared storage pool.");
        }

        // Check that the VM is a UserVM
        UserVmVO vm = _userVmDao.findById(vmId);
        if (vm == null || vm.getType() != VirtualMachine.Type.User) {
            throw new InvalidParameterValueException("Please specify a valid User VM.");
        }
        
        // Check that the VM is in the correct state
        if (vm.getState() != State.Running && vm.getState() != State.Stopped) {
        	throw new InvalidParameterValueException("Please specify a VM that is either running or stopped.");
        }

        // Check that the volume is not currently attached to any VM
        if (volume.getInstanceId() != null) {
            throw new InvalidParameterValueException("Please specify a volume that is not attached to any VM.");
        }

        // Check that the volume is not destroyed
        if (volume.getDestroyed()) {
            throw new InvalidParameterValueException("Please specify a volume that is not destroyed.");
        }

        // Check that the VM has less than 6 data volumes attached
        List<VolumeVO> existingDataVolumes = _volumeDao.findByInstanceAndType(vmId, VolumeType.DATADISK);
        if (existingDataVolumes.size() >= 6) {
            throw new InvalidParameterValueException("The specified VM already has the maximum number of data disks (6). Please specify another VM.");
        }
        
        // Check that the VM and the volume are in the same zone
        if (vm.getDataCenterId() != volume.getDataCenterId()) {
        	throw new InvalidParameterValueException("Please specify a VM that is in the same zone as the volume.");
        }
        long eventId = saveScheduledEvent(1L, volume.getAccountId(), EventTypes.EVENT_VOLUME_ATTACH, "attaching volume: "+volumeId+" to Vm: "+vmId);
        VolumeOperationParam param = new VolumeOperationParam();
        param.setOp(VolumeOp.Attach);
        param.setVmId(vmId);
        param.setVolumeId(volumeId);
        param.setEventId(eventId);
        param.setDeviceId(deviceId);

        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(vm.getAccountId());
        job.setCmd("VolumeOperation");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator("virtualmachine");
        
        return _asyncMgr.submitAsyncJob(job);
    }

    @Override
    public void detachVolumeFromVM(long volumeId, long startEventId) throws InternalErrorException {
        _vmMgr.detachVolumeFromVM(volumeId, startEventId);
    }

    @Override
    public long detachVolumeFromVMAsync(long volumeId) throws InvalidParameterValueException {
        VolumeVO volume = _volumeDao.findById(volumeId);

        // Check that the volume is a data volume
        if (volume.getVolumeType() != VolumeType.DATADISK) {
            throw new InvalidParameterValueException("Please specify a data volume.");
        }

        // Check that the volume is stored on shared storage
        if (!_storageMgr.volumeOnSharedStoragePool(volume)) {
            throw new InvalidParameterValueException("Please specify a volume that has been created on a shared storage pool.");
        }

        Long vmId = volume.getInstanceId();

        // Check that the volume is currently attached to a VM
        if (vmId == null) {
            throw new InvalidParameterValueException("The specified volume is not attached to a VM.");
        }

        // Check that the VM is in the correct state
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm.getState() != State.Running && vm.getState() != State.Stopped) {
        	throw new InvalidParameterValueException("Please specify a VM that is either running or stopped.");
        }
        
        long eventId = saveScheduledEvent(1L, volume.getAccountId(), EventTypes.EVENT_VOLUME_DETACH, "detaching volume: "+volumeId+" from Vm: "+vmId);
        VolumeOperationParam param = new VolumeOperationParam();
        param.setOp(VolumeOp.Detach);
        param.setVolumeId(volumeId);
        param.setEventId(eventId);

        Gson gson = GsonHelper.getBuilder().create();

		AsyncJobVO job = new AsyncJobVO();
		job.setUserId(UserContext.current().getUserId());
		job.setAccountId(vm.getAccountId());
		job.setCmd("VolumeOperation");
		job.setCmdInfo(gson.toJson(param));
		job.setCmdOriginator("virtualmachine");
		
		return _asyncMgr.submitAsyncJob(job);
	}

    @Override
    public boolean attachISOToVM(long vmId, long userId, long isoId, boolean attach, long startEventId) {
    	UserVmVO vm = _userVmDao.findById(vmId);
    	VMTemplateVO iso = _templateDao.findById(isoId);
    	if(attach){
    	    saveStartedEvent(userId, vm.getAccountId(), EventTypes.EVENT_ISO_ATTACH, "Attaching ISO: "+isoId+" to Vm: "+vmId, startEventId);
    	} else {
    	    saveStartedEvent(userId, vm.getAccountId(), EventTypes.EVENT_ISO_DETACH, "Detaching ISO: "+isoId+" from Vm: "+vmId, startEventId);
    	}
        boolean success = _vmMgr.attachISOToVM(vmId, isoId, attach);

        if (success) {
            VMInstanceVO updatedInstance = _vmInstanceDao.createForUpdate();
            if (attach) {
                updatedInstance.setIsoId(iso.getId().longValue());
            } else {
                updatedInstance.setIsoId(null);
            }
            _vmInstanceDao.update(vmId, updatedInstance);

            if (attach) {
                saveEvent(userId, vm.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_ISO_ATTACH, "Successfully attached ISO: " + iso.getName() + " to VM with ID: " + vmId,
                        null, startEventId);
            } else {
                saveEvent(userId, vm.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_ISO_DETACH, "Successfully detached ISO from VM with ID: " + vmId, null, startEventId);
            }
        } else {
            if (attach) {
                saveEvent(userId, vm.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_ISO_ATTACH, "Failed to attach ISO: " + iso.getName() + " to VM with ID: " + vmId, null, startEventId);
            } else {
                saveEvent(userId, vm.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_ISO_DETACH, "Failed to detach ISO from VM with ID: " + vmId, null, startEventId);
            }
        }

        return success;
    }

    @Override
    public long attachISOToVMAsync(long vmId, long userId, long isoId) throws InvalidParameterValueException {
        UserVmVO vm = _userVmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to find VM with ID " + vmId);
        }

        VMTemplateVO iso = _templateDao.findById(isoId);
        if (iso == null) {
            throw new InvalidParameterValueException("Unable to find ISO with id " + isoId);
        }

        AccountVO account = _accountDao.findById(vm.getAccountId());
        if (account == null) {
            throw new InvalidParameterValueException("Unable to find account for VM with ID " + vmId);
        }
        
        State vmState = vm.getState();
        if (vmState != State.Running && vmState != State.Stopped) {
        	throw new InvalidParameterValueException("Please specify a VM that is either Stopped or Running.");
        }
        
        long eventId = saveScheduledEvent(userId, account.getId(), EventTypes.EVENT_ISO_ATTACH, "attaching ISO: "+isoId+" to Vm: "+vmId);
        
        AttachISOParam param = new AttachISOParam(vmId, userId, isoId, true);
        param.setEventId(eventId);
        Gson gson = GsonHelper.getBuilder().create();

		AsyncJobVO job = new AsyncJobVO();
		job.setUserId(UserContext.current().getUserId());
		job.setAccountId(vm.getAccountId());
		job.setCmd("AttachISO");
		job.setCmdInfo(gson.toJson(param));
		return _asyncMgr.submitAsyncJob(job, true);
	}

    @Override
    public long detachISOFromVMAsync(long vmId, long userId) throws InvalidParameterValueException {
        UserVm userVM = _userVmDao.findById(vmId);
        if (userVM == null) {
            throw new InvalidParameterValueException("Please specify a valid VM.");
        }

        Long isoId = userVM.getIsoId();
        if (isoId == null) {
            throw new InvalidParameterValueException("Please specify a valid ISO.");
        }
        
        State vmState = userVM.getState();
        if (vmState != State.Running && vmState != State.Stopped) {
        	throw new InvalidParameterValueException("Please specify a VM that is either Stopped or Running.");
        }

        long eventId = saveScheduledEvent(userId, userVM.getAccountId(), EventTypes.EVENT_ISO_DETACH, "detaching ISO: "+isoId+" from Vm: "+vmId);
        AttachISOParam param = new AttachISOParam(vmId, userId, isoId.longValue(), false);
        param.setEventId(eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(userVM.getAccountId());
        job.setCmd("AttachISO");
        job.setCmdInfo(gson.toJson(param));
        return _asyncMgr.submitAsyncJob(job, true);
    }

    @Override
    public long resetVMPasswordAsync(long userId, long vmId, String password) {
        ResetVMPasswordParam param = new ResetVMPasswordParam(userId, vmId, password);
        Gson gson = GsonHelper.getBuilder().create();

        UserVm vm = _userVmDao.findById(vmId);
		AsyncJobVO job = new AsyncJobVO();
		job.setUserId(UserContext.current().getUserId());
		job.setAccountId(vm.getAccountId());
		job.setCmd("ResetVMPassword");
		job.setCmdInfo(gson.toJson(param));
		job.setCmdOriginator("virtualmachine");
		
		return _asyncMgr.submitAsyncJob(job, true);
	}

    private boolean validPassword(String password) {
        for (int i = 0; i < password.length(); i++) {
            if (password.charAt(i) == ' ') {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean reconnect(long hostId) {
        try {
            return _agentMgr.reconnect(hostId);
        } catch (AgentUnavailableException e) {
            return false;
        }
    }

    @Override
    public long reconnectAsync(long hostId) {
        Long param = new Long(hostId);
        Gson gson = GsonHelper.getBuilder().create();

		AsyncJobVO job = new AsyncJobVO();
		job.setUserId(UserContext.current().getUserId());
		job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
		job.setCmd("Reconnect");
		job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(ReconnectHostCmd.getResultObjectName());
        
		return _asyncMgr.submitAsyncJob(job);
	}

    @Override
    public UserVm deployVirtualMachine(long userId, long accountId, long dataCenterId, long serviceOfferingId, long templateId, Long diskOfferingId,
            String domain, String password, String displayName, String group, String userData, String [] networkGroups, long startEventId) throws ResourceAllocationException, InvalidParameterValueException, InternalErrorException,
            InsufficientStorageCapacityException, PermissionDeniedException, ExecutionException, StorageUnavailableException, ConcurrentOperationException {

        saveStartedEvent(userId, accountId, EventTypes.EVENT_VM_CREATE, "Deploying Vm", startEventId);
        
        AccountVO account = _accountDao.findById(accountId);
        DataCenterVO dc = _dcDao.findById(dataCenterId);
        ServiceOfferingVO offering = _offeringsDao.findById(serviceOfferingId);
        VMTemplateVO template = _templateDao.findById(templateId);

        // Make sure a valid template ID was specified
        if (template == null) {
            throw new InvalidParameterValueException("Please specify a valid template or ISO ID.");
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
            		created = _vmMgr.createDirectlyAttachedVMExternal(vmId, userId, account, dc, offering, template, diskOffering, displayName, group, userData, a, networkGroupVOs, startEventId);
            	} catch (ResourceAllocationException rae) {
            		throw rae;
            	}
            } else {
            	if (offering.getGuestIpType() == GuestIpType.Virtualized) {
            		try {
            			externalIp = _networkMgr.assignSourceNatIpAddress(account, dc, domain, offering);
            		} catch (ResourceAllocationException rae) {
            			throw rae;
            		}

            		if (externalIp == null) {
            			throw new InternalErrorException("Unable to allocate a source nat ip address");
            		}

            		if (s_logger.isDebugEnabled()) {
            			s_logger.debug("Source Nat acquired: " + externalIp);
            		}

            		try {
            			created = _vmMgr.createVirtualMachine(vmId, userId, account, dc, offering, template, diskOffering, displayName, group, userData, a, startEventId);
            		} catch (ResourceAllocationException rae) {
            			throw rae;
            		}
            	} else {
            		try {
            			created = _vmMgr.createDirectlyAttachedVM(vmId, userId, account, dc, offering, template, diskOffering, displayName, group, userData, a, networkGroupVOs, startEventId);
            		} catch (ResourceAllocationException rae) {
            			throw rae;
            		}
            	}
            }

            if (created == null) {
                throw new InternalErrorException("Unable to create VM for account (" + accountId + "): " + account.getAccountName());
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM created: " + created.getId() + "-" + created.getName());
            }
            boolean executionExceptionFlag = false;
            boolean storageUnavailableExceptionFlag = false;
            boolean concurrentOperationExceptionFlag = false;
            String executionExceptionMsg= "";
            String storageUnavailableExceptionMsg = "";
            String concurrentOperationExceptionMsg = "";
            UserVm started = null;
            if (isIso)
            {
                String isoPath = _storageMgr.getAbsoluteIsoPath(templateId, dataCenterId);
                try
                {
					started = _vmMgr.startVirtualMachine(userId, created.getId(), password, isoPath);
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
					started = _vmMgr.startVirtualMachine(userId, created.getId(), password, null);
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
                    throw new InternalErrorException("Unable to start the VM " + created.getId() + "-" + created.getName());
                }
                
            } else {
                if (isIso) {
                    VMInstanceVO updatedInstance = _vmInstanceDao.createForUpdate();
                    updatedInstance.setIsoId(templateId);
                    _vmInstanceDao.update(started.getId(), updatedInstance);
                    started = _userVmDao.findById(started.getId());
                }
                String params = "\nsourceNat=" + false + "\ndcId=" + dc.getId();
                try {
					associateIpAddressListToAccount(userId, accountId, dc.getId(),null);															
				} catch (InsufficientAddressCapacityException e) {
					s_logger.debug("Unable to assign public IP address pool: " +e.getMessage());					
				}
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM started: " + started.getId() + "-" + started.getName());
            }
            return started;
        }

        return null;
    }

    @Override
    public long deployVirtualMachineAsync(long userId, long accountId, long dataCenterId, long serviceOfferingId, long templateId,
            Long diskOfferingId, String domain, String password, String displayName, String group, String userData, String [] networkGroups)  throws InvalidParameterValueException, PermissionDeniedException {

    	AccountVO account = _accountDao.findById(accountId);
        if (account == null) {
            throw new InvalidParameterValueException("Unable to find account: " + accountId);
        }

        DataCenterVO dc = _dcDao.findById(dataCenterId);
        if (dc == null) {
            throw new InvalidParameterValueException("Unable to find zone: " + dataCenterId);
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
        
        // validate that the template is usable by the account
        if (!template.isPublicTemplate()) {
            Long templateOwner = template.getAccountId();
            if (!BaseCmd.isAdmin(account.getType()) && ((templateOwner == null) || (templateOwner.longValue() != accountId))) {
                // since the current account is not the owner of the template, check the launch permissions table to see if the
                // account can launch a VM from this template
                LaunchPermissionVO permission = _launchPermissionDao.findByTemplateAndAccount(templateId, account.getId().longValue());
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
        if (offering.getGuestIpType() != GuestIpType.Virtualized) {
        	_networkGroupMgr.createDefaultNetworkGroup(accountId);
    	}
        
        if (networkGroups != null) {
        	if (offering.getGuestIpType() == GuestIpType.Virtualized) {
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
        	if (offering.getGuestIpType() != GuestIpType.Virtualized && _networkGroupsEnabled) {
        		networkGroups = new String[]{NetworkGroupManager.DEFAULT_GROUP_NAME};
        	}
        }
        
        long eventId = saveScheduledEvent(userId, accountId, EventTypes.EVENT_VM_CREATE, "deploying Vm");
        
        DeployVMParam param = new DeployVMParam(userId, accountId, dataCenterId, serviceOfferingId, templateId, diskOfferingId, domain, password,
                displayName, group, userData, networkGroups, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(accountId);
        job.setCmd("DeployVM");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(DeployVMCmd.getResultObjectName());
        return _asyncMgr.submitAsyncJob(job);
    }

    @Override
    public UserVm startVirtualMachine(long userId, long vmId, String isoPath) throws InternalErrorException, ExecutionException, StorageUnavailableException, ConcurrentOperationException {
        return _vmMgr.startVirtualMachine(userId, vmId, isoPath);
    }

    @Override
    public long startVirtualMachineAsync(long userId, long vmId, String isoPath) {
        
        UserVmVO userVm = _userVmDao.findById(vmId);

        long eventId = saveScheduledEvent(userId, userVm.getAccountId(), EventTypes.EVENT_VM_START, "starting Vm with Id: "+vmId);
        
        VMOperationParam param = new VMOperationParam(userId, vmId, isoPath, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(userVm.getAccountId());
        job.setCmd("StartVM");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(StartVMCmd.getResultObjectName());
        return _asyncMgr.submitAsyncJob(job, true);
    }

    @Override
    public boolean stopVirtualMachine(long userId, long vmId) {
        return _vmMgr.stopVirtualMachine(userId, vmId);
    }

    @Override
    public long stopVirtualMachineAsync(long userId, long vmId) {
        
        UserVmVO userVm = _userVmDao.findById(vmId);

        long eventId = saveScheduledEvent(userId, userVm.getAccountId(), EventTypes.EVENT_VM_STOP, "stopping Vm with Id: "+vmId);
        
        VMOperationParam param = new VMOperationParam(userId, vmId, null, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(userVm.getAccountId());
        job.setCmd("StopVM");
        job.setCmdInfo(gson.toJson(param));
        
        // use the same result result object name as StartVMCmd
        job.setCmdOriginator(StartVMCmd.getResultObjectName());
        return _asyncMgr.submitAsyncJob(job, true);
    }

    @Override
    public boolean rebootVirtualMachine(long userId, long vmId) {
        return _vmMgr.rebootVirtualMachine(userId, vmId);
    }

    @Override
    public long rebootVirtualMachineAsync(long userId, long vmId) {
        
        UserVmVO userVm = _userVmDao.findById(vmId);

        long eventId = saveScheduledEvent(userId, userVm.getAccountId(), EventTypes.EVENT_VM_REBOOT, "rebooting Vm with Id: "+vmId);
        
        VMOperationParam param = new VMOperationParam(userId, vmId, null, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(userVm.getAccountId());
        job.setCmd("RebootVM");
        job.setCmdInfo(gson.toJson(param));
        
        // use the same result result object name as StartVMCmd
        job.setCmdOriginator(StartVMCmd.getResultObjectName());
        return _asyncMgr.submitAsyncJob(job, true);
    }

    @Override
    public boolean destroyVirtualMachine(long userId, long vmId) {
        return _vmMgr.destroyVirtualMachine(userId, vmId);
    }

    @Override
    public long destroyVirtualMachineAsync(long userId, long vmId) {
        
        UserVmVO userVm = _userVmDao.findById(vmId);

        long eventId = saveScheduledEvent(userId, userVm.getAccountId(), EventTypes.EVENT_VM_DESTROY, "destroying Vm with Id: "+vmId);
        VMOperationParam param = new VMOperationParam(userId, vmId, null, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(userVm.getAccountId());
        job.setCmd("DestroyVM");
        job.setCmdInfo(gson.toJson(param));
        return _asyncMgr.submitAsyncJob(job, true);
    }

    @Override
    public boolean recoverVirtualMachine(long vmId) throws ResourceAllocationException {
        return _vmMgr.recoverVirtualMachine(vmId);
    }

    @Override
    public boolean upgradeVirtualMachine(long userId, long vmId, long serviceOfferingId,long startEventId) {
        UserVmVO userVm = _userVmDao.findById(vmId);
        saveStartedEvent(userId, userVm.getAccountId(), EventTypes.EVENT_VM_UPGRADE, "upgrading service offering on VM : " + userVm.getName(), startEventId);
        boolean success = _vmMgr.upgradeVirtualMachine(vmId, serviceOfferingId);

        String params = "id=" + vmId + "\nvmName=" + userVm.getName() + "\nsoId=" + serviceOfferingId + "\ntId=" + userVm.getTemplateId() + "\ndcId=" + userVm.getDataCenterId();

        if (success) {
            this.saveEvent(userId, userVm.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_VM_UPGRADE, "Successfully upgrade service offering on VM : " + userVm.getName(), params, startEventId);
        } else {
            this.saveEvent(userId, userVm.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_UPGRADE, "Failed to upgrade service offering on VM : " + userVm.getName(), params, startEventId);
        }
        
        return success;
    }

    @Override
    public long upgradeVirtualMachineAsync(long userId, long vmId, long serviceOfferingId) throws InvalidParameterValueException {
        UpgradeVMParam param = new UpgradeVMParam(userId, vmId, serviceOfferingId);
        Gson gson = GsonHelper.getBuilder().create();
                       
        // Check that the specified VM ID is valid
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null) {
        	throw new InvalidParameterValueException("Unable to find a virtual machine with id " + vmId);
        }
        
        // Check that the specified service offering ID is valid
        ServiceOfferingVO newServiceOffering = _offeringsDao.findById(serviceOfferingId);
        if (newServiceOffering == null) {
        	throw new InvalidParameterValueException("Unable to find a service offering with id " + serviceOfferingId);
        }
        
        // Check that the VM is stopped
        if (!vm.getState().equals(State.Stopped)) {
            s_logger.warn("Unable to upgrade virtual machine " + vm.toString() + " in state " + vm.getState());
            throw new InvalidParameterValueException("Unable to upgrade virtual machine " + vm.toString() + " in state " + vm.getState() + "; make sure the virtual machine is stopped and not in an error state before upgrading.");
        }
        
        // Check if the service offering being upgraded to is what the VM is already running with
        if (vm.getServiceOfferingId() == newServiceOffering.getId()) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Not upgrading vm " + vm.toString() + " since it already has the requested service offering (" + newServiceOffering.getName() + ")");
            }
            
            throw new InvalidParameterValueException("Not upgrading vm " + vm.toString() + " since it already has the requested service offering (" + newServiceOffering.getName() + ")");
        }
        
        // Check that the service offering being upgraded to has the same Guest IP type as the VM's current service offering
        ServiceOfferingVO currentServiceOffering = _offeringsDao.findById(vm.getServiceOfferingId());
        if (!currentServiceOffering.getGuestIpType().equals(newServiceOffering.getGuestIpType())) {
        	String errorMsg = "The service offering being upgraded to has a guest IP type: " + newServiceOffering.getGuestIpType();
        	errorMsg += ". Please select a service offering with the same guest IP type as the VM's current service offering (" + currentServiceOffering.getGuestIpType() + ").";
        	throw new InvalidParameterValueException(errorMsg);
        }
        
        // Check that the service offering being upgraded to has the same storage pool preference as the VM's current service offering
        if (currentServiceOffering.getUseLocalStorage() != newServiceOffering.getUseLocalStorage()) {
            throw new InvalidParameterValueException("Unable to upgrade virtual machine " + vm.toString() + ", cannot switch between local storage and shared storage service offerings.  Current offering useLocalStorage=" +
                   currentServiceOffering.getUseLocalStorage() + ", target offering useLocalStorage=" + newServiceOffering.getUseLocalStorage());
        }

        // Check that there are enough resources to upgrade the service offering
        if (!_agentMgr.isVirtualMachineUpgradable(vm, newServiceOffering)) {
           throw new InvalidParameterValueException("Unable to upgrade virtual machine, not enough resources available for an offering of " +
                   newServiceOffering.getCpu() + " cpu(s) at " + newServiceOffering.getSpeed() + " Mhz, and " + newServiceOffering.getRamSize() + " MB of memory");
        }
        
        // Check that the service offering being upgraded to has all the tags of the current service offering
        List<String> currentTags = _configMgr.csvTagsToList(currentServiceOffering.getTags());
        List<String> newTags = _configMgr.csvTagsToList(newServiceOffering.getTags());
        if (!newTags.containsAll(currentTags)) {
        	throw new InvalidParameterValueException("Unable to upgrade virtual machine; the new service offering does not have all the tags of the " +
        											 "current service offering. Current service offering tags: " + currentTags + "; " +
        											 "new service offering tags: " + newTags);
        }
        
        long eventId = saveScheduledEvent(userId, vm.getAccountId(), EventTypes.EVENT_VM_UPGRADE, "upgrading Vm with Id: "+vmId);
        param.setEventId(eventId);
        
        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(vm.getAccountId());
        job.setCmd("UpgradeVM");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(UpgradeVMCmd.getResultObjectName());
        
        return _asyncMgr.submitAsyncJob(job, true);
    }

    @Override
    public void updateVirtualMachine(long vmId, String displayName, String group, boolean enable, Long userId, long accountId) {
        VMInstanceVO vm = _vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("Unable to find virual machine with id " + vmId);
        }

        boolean haEnabled = vm.isHaEnabled();
        _vmInstanceDao.updateVM(vmId, displayName, group, enable);
        if (haEnabled != enable) {
            String description = null;
            String type = null;
            if (enable) {
                description = "Successfully enabled HA for virtual machine " + vm.getName();
                type = EventTypes.EVENT_VM_ENABLE_HA;
            } else {
                description = "Successfully disabled HA for virtual machine " + vm.getName();
                type = EventTypes.EVENT_VM_DISABLE_HA;
            }
            // create a event for the change in HA Enabled flag
            saveEvent(userId, accountId, EventVO.LEVEL_INFO, type, description, null);
        }
    }
    
    @Override
    public StoragePoolVO updateStoragePool(long poolId, String tags) throws IllegalArgumentException {
    	return _storageMgr.updateStoragePool(poolId, tags);
    }

    @Override
    public DomainRouter startRouter(long routerId, long startEventId) throws InternalErrorException {
        return _networkMgr.startRouter(routerId, startEventId);
    }

    @Override
    public long startRouterAsync(long routerId) {
        long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_ROUTER_START, "starting Router with Id: "+routerId);

        VMOperationParam param = new VMOperationParam(0, routerId, null, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
        job.setCmd("StartRouter");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(StartRouterCmd.getResultObjectName());
        return _asyncMgr.submitAsyncJob(job, true);
    }

    @Override
    public boolean stopRouter(long routerId, long startEventId) {
        return _networkMgr.stopRouter(routerId, startEventId);
    }

    @Override
    public long stopRouterAsync(long routerId) {
        long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_ROUTER_STOP, "stopping Router with Id: "+routerId);
        VMOperationParam param = new VMOperationParam(0, routerId, null, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
        job.setCmd("StopRouter");
        job.setCmdInfo(gson.toJson(param));
        // use the same result object name as StartRouterCmd
        job.setCmdOriginator(StartRouterCmd.getResultObjectName());
        
        return _asyncMgr.submitAsyncJob(job, true);
    }

    @Override
    public boolean rebootRouter(long routerId, long startEventId) throws InternalErrorException {
        return _networkMgr.rebootRouter(routerId, startEventId);
    }

    @Override
    public long rebootRouterAsync(long routerId) {
        long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_ROUTER_REBOOT, "rebooting Router with Id: "+routerId);
        VMOperationParam param = new VMOperationParam(0, routerId, null, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
        job.setCmd("RebootRouter");
        job.setCmdInfo(gson.toJson(param));
        // use the same result object name as StartRouterCmd
        job.setCmdOriginator(StartRouterCmd.getResultObjectName());
        return _asyncMgr.submitAsyncJob(job, true);
    }

    @Override
    public boolean destroyRouter(long routerId) {
        return _networkMgr.destroyRouter(routerId);
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
    public DataCenterVO getDataCenterBy(long dataCenterId) {
        return _dcDao.findById(dataCenterId);
    }

    @Override
    public HostPodVO getPodBy(long podId) {
        return _hostPodDao.findById(podId);
    }

    
    @Override
    public List<DataCenterVO> listDataCenters() {
        return _dcDao.listAllActive();
    }

    @Override
    public List<DataCenterVO> listDataCentersBy(long accountId) {
        List<DataCenterVO> dcs = _dcDao.listAllActive();
        List<DomainRouterVO> routers = _routerDao.listBy(accountId);
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
        return dcs;
    }

    @Override
    public HostVO getHostBy(long hostId) {
        return _hostDao.findById(hostId);
    }

    public void updateHost(long hostId, long guestOSCategoryId) throws InvalidParameterValueException {
    	// Verify that the guest OS Category exists
    	if (guestOSCategoryId > 0) {
    		if (_guestOSCategoryDao.findById(guestOSCategoryId) == null) {
    			throw new InvalidParameterValueException("Please specify a valid guest OS category.");
    		}
    	}
    	
    	_agentMgr.updateHost(hostId, guestOSCategoryId);
    }
    
    public boolean deleteHost(long hostId) {
        return _agentMgr.deleteHost(hostId);
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
    @DB
    public void assignSecurityGroup(Long userId, Long securityGroupId, List<Long> securityGroupIdList, String publicIp, Long vmId, long startEventId) throws PermissionDeniedException,
            NetworkRuleConflictException, InvalidParameterValueException, InternalErrorException {
        boolean locked = false;
        Transaction txn = Transaction.currentTxn();
        try {
            UserVmVO userVm = _userVmDao.findById(vmId);
            if (userVm == null) {
                s_logger.warn("Unable to find virtual machine with id " + vmId);
                throw new InvalidParameterValueException("Unable to find virtual machine with id " + vmId);
            }
            saveStartedEvent(userId, userVm.getAccountId(), EventTypes.EVENT_PORT_FORWARDING_SERVICE_APPLY, "Applying port forwarding service for Vm with Id: "+vmId, startEventId);
            
            State vmState = userVm.getState();
            switch (vmState) {
            case Destroyed:
            case Error:
            case Expunging:
            case Unknown:
                throw new InvalidParameterValueException("Unable to assign port forwarding service(s) '"
                        + ((securityGroupId == null) ? StringUtils.join(securityGroupIdList, ",") : securityGroupId) + "' to virtual machine " + vmId
                        + " due to virtual machine being in an invalid state for assigning a port forwarding service (" + vmState + ")");
            }

            // sanity check that the vm can be applied to the load balancer
            ServiceOfferingVO offering = _offeringsDao.findById(userVm.getServiceOfferingId());
            if ((offering == null) || !GuestIpType.Virtualized.equals(offering.getGuestIpType())) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to apply port forwarding service to virtual machine " + userVm.toString() + ", bad network type (" + ((offering == null) ? "null" : offering.getGuestIpType()) + ")");
                }

                throw new InvalidParameterValueException("Unable to apply port forwarding service to virtual machine " + userVm.toString() + ", bad network type (" + ((offering == null) ? "null" : offering.getGuestIpType()) + ")");
            }
            
            DomainRouterVO router = null;
            if (userVm.getDomainRouterId() != null)
            	router = _routerDao.findById(userVm.getDomainRouterId());
            if (router == null) {
                s_logger.warn("Unable to find router (" + userVm.getDomainRouterId() + ") for virtual machine " + userVm.toString());
                throw new InvalidParameterValueException("Unable to find router (" + userVm.getDomainRouterId() + ") for virtual machine with id " + vmId);
            }

            IPAddressVO ipVO = _publicIpAddressDao.acquire(publicIp);
            if (ipVO == null) {
                // throw this exception because hackers can use the api to probe for allocated ips
                throw new PermissionDeniedException("User does not own supplied address");
            }
            locked = true;

            if ((ipVO.getAllocated() == null) || (ipVO.getAccountId() == null) || (ipVO.getAccountId().longValue() != userVm.getAccountId())) {
                throw new PermissionDeniedException("User does not own supplied address");
            }

            VlanVO vlan = _vlanDao.findById(ipVO.getVlanDbId());
            if (!VlanType.VirtualNetwork.equals(vlan.getVlanType())) {
                throw new InvalidParameterValueException("Invalid IP address " + publicIp + " for applying port forwarding services, the IP address is not in a 'virtual network' vlan.");
            }

            txn.start();

            // save off the owner of the instance to be used for events
            Account account = _accountDao.findById(userVm.getAccountId());

            if (securityGroupId == null) {
                // - send one command to agent to remove *all* rules for
                // publicIp/vm combo
                // - add back all rules based on list passed in
                List<FirewallRuleVO> fwRulesToRemove = _firewallRulesDao.listForwardingByPubAndPrivIp(true, publicIp, userVm.getGuestIpAddress());
                {
                    // Save and create the event
                    String description;
                    String type = EventTypes.EVENT_NET_RULE_DELETE;
                    String level = EventVO.LEVEL_INFO;

                    for (FirewallRuleVO fwRule : fwRulesToRemove) {
                        fwRule.setEnabled(false); // disable rule for sending to the agent
                        _firewallRulesDao.remove(fwRule.getId()); // remove the rule from the database

                        description = "deleted ip forwarding rule [" + fwRule.getPublicIpAddress() + ":" + fwRule.getPublicPort() + "]->[" + fwRule.getPrivateIpAddress() + ":"
                                + fwRule.getPrivatePort() + "]" + " " + fwRule.getProtocol();

                        saveEvent(userId, account.getId(), level, type, description);
                    }
                }

                List<FirewallRuleVO> updatedRules = _networkMgr.updateFirewallRules(null, fwRulesToRemove, router);
                if ((updatedRules != null) && (updatedRules.size() != fwRulesToRemove.size())) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Unable to clean up all port forwarding service rules for public IP " + publicIp + " and guest vm " + userVm.getName()
                                + " while applying port forwarding service(s) '" + ((securityGroupId == null) ? StringUtils.join(securityGroupIdList, ",") : securityGroupId) + "'"
                                + " -- intended to remove " + fwRulesToRemove.size() + " rules, removd " + ((updatedRules == null) ? "null" : updatedRules.size()) + " rules.");
                    }
                }

                List<SecurityGroupVMMapVO> sgVmMappings = _securityGroupVMMapDao.listByIpAndInstanceId(publicIp, vmId);
                for (SecurityGroupVMMapVO sgVmMapping : sgVmMappings) {
                    boolean success = _securityGroupVMMapDao.remove(sgVmMapping.getId());

                    SecurityGroupVO securityGroup = _securityGroupDao.findById(sgVmMapping.getSecurityGroupId());

                    // save off an event for removing the security group
                    EventVO event = new EventVO();
                    event.setUserId(userId);
                    event.setAccountId(userVm.getAccountId());
                    event.setType(EventTypes.EVENT_PORT_FORWARDING_SERVICE_REMOVE);
                    String sgRemoveLevel = EventVO.LEVEL_INFO;
                    String sgRemoveDesc = "Successfully removed ";
                    if (!success) {
                        sgRemoveLevel = EventVO.LEVEL_ERROR;
                        sgRemoveDesc = "Failed to remove ";
                    }
                    String params = "sgId="+securityGroup.getId()+"\nvmId="+vmId;
                    event.setParameters(params);
                    event.setDescription(sgRemoveDesc + "port forwarding service " + securityGroup.getName() + " from virtual machine " + userVm.getName());
                    event.setLevel(sgRemoveLevel);
                    _eventDao.persist(event);
                }
            } else {
                List<SecurityGroupVMMapVO> existingVMMaps = _securityGroupVMMapDao.listBySecurityGroup(securityGroupId.longValue());
                if ((existingVMMaps != null) && !existingVMMaps.isEmpty()) {
                    for (SecurityGroupVMMapVO existingVMMap : existingVMMaps) {
                        if (existingVMMap.getInstanceId() == userVm.getId().longValue()) {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("port forwarding service " + securityGroupId + " is already applied to virtual machine " + userVm.toString() + ", skipping assignment.");
                            }
                            return;
                        }
                    }
                }
            }

            List<Long> finalSecurityGroupIdList = new ArrayList<Long>();
            if (securityGroupId != null) {
                finalSecurityGroupIdList.add(securityGroupId);
            } else {
                finalSecurityGroupIdList.addAll(securityGroupIdList);
            }

            for (Long sgId : finalSecurityGroupIdList) {
                if (sgId.longValue() == 0) {
                    // group id of 0 means to remove all groups, which we just did above
                    break;
                }

                SecurityGroupVO securityGroup = _securityGroupDao.findById(Long.valueOf(sgId));
                if (securityGroup == null) {
                    s_logger.warn("Unable to find port forwarding service with id " + sgId);
                    throw new InvalidParameterValueException("Unable to find port forwarding service with id " + sgId);
                }

                if (!_domainDao.isChildDomain(securityGroup.getDomainId(), userVm.getDomainId())) {
                    s_logger.warn("Unable to assign port forwarding service " + sgId + " to user vm " + vmId + ", user vm's domain (" + userVm.getDomainId()
                            + ") is not in the domain of the port forwarding service (" + securityGroup.getDomainId() + ")");
                    throw new InvalidParameterValueException("Unable to assign port forwarding service " + sgId + " to user vm " + vmId + ", user vm's domain (" + userVm.getDomainId()
                            + ") is not in the domain of the port forwarding service (" + securityGroup.getDomainId() + ")");
                }

                // check for ip address/port conflicts by checking exising forwarding and loadbalancing rules
                List<FirewallRuleVO> existingRulesOnPubIp = _firewallRulesDao.listIPForwarding(publicIp);
                Map<String, Pair<String, String>> mappedPublicPorts = new HashMap<String, Pair<String, String>>();

                if (existingRulesOnPubIp != null) {
                    for (FirewallRuleVO fwRule : existingRulesOnPubIp) {
                        mappedPublicPorts.put(fwRule.getPublicPort(), new Pair<String, String>(fwRule.getPrivateIpAddress(), fwRule.getPrivatePort()));
                    }
                }

                List<LoadBalancerVO> loadBalancers = _loadBalancerDao.listByIpAddress(publicIp);
                if (loadBalancers != null) {
                    for (LoadBalancerVO loadBalancer : loadBalancers) {
                        // load balancers don't have to be applied to an
                        // instance for there to be a conflict on the load
                        // balancers ip/port, so just
                        // map the public port to a pair of empty strings
                        mappedPublicPorts.put(loadBalancer.getPublicPort(), new Pair<String, String>("", ""));
                    }
                }

                List<FirewallRuleVO> firewallRulesToApply = new ArrayList<FirewallRuleVO>();
                List<NetworkRuleConfigVO> netRules = _networkRuleConfigDao.listBySecurityGroupId(sgId);
                for (NetworkRuleConfigVO netRule : netRules) {
                    Pair<String, String> privateIpPort = mappedPublicPorts.get(netRule.getPublicPort());
                    if (privateIpPort != null) {
                        if (privateIpPort.first().equals(userVm.getGuestIpAddress()) && privateIpPort.second().equals(netRule.getPrivatePort())) {
                            continue; // already mapped
                        } else {
                            throw new NetworkRuleConflictException("An existing service rule for " + publicIp + ":" + netRule.getPublicPort()
                                    + " already exists, found while trying to apply service rule " + netRule.getId() + " from port forwarding service " + securityGroup.getName() + ".");
                        }
                    }

                    FirewallRuleVO newFwRule = new FirewallRuleVO();
                    newFwRule.setEnabled(true);
                    newFwRule.setForwarding(true);
                    newFwRule.setPrivatePort(netRule.getPrivatePort());
                    newFwRule.setProtocol(netRule.getProtocol());
                    newFwRule.setPublicPort(netRule.getPublicPort());
                    newFwRule.setPublicIpAddress(publicIp);
                    newFwRule.setPrivateIpAddress(userVm.getGuestIpAddress());
                    newFwRule.setGroupId(netRule.getSecurityGroupId());

                    firewallRulesToApply.add(newFwRule);
                    _firewallRulesDao.persist(newFwRule);

                    String description = "created new ip forwarding rule [" + newFwRule.getPublicIpAddress() + ":" + newFwRule.getPublicPort() + "]->["
                            + newFwRule.getPrivateIpAddress() + ":" + newFwRule.getPrivatePort() + "]" + " " + newFwRule.getProtocol();

                    saveEvent(userId, account.getId(), EventVO.LEVEL_INFO, EventTypes.EVENT_NET_RULE_ADD, description);
                }

                // now that individual rules have been created from the security group, save the security group mapping for this ip/vm instance
                SecurityGroupVMMapVO sgVmMap = new SecurityGroupVMMapVO(sgId, publicIp, vmId);
                _securityGroupVMMapDao.persist(sgVmMap);

                // Save off information for the event that the security group was applied
                EventVO event = new EventVO();
                event.setUserId(userId);
                event.setAccountId(userVm.getAccountId());
                event.setType(EventTypes.EVENT_PORT_FORWARDING_SERVICE_APPLY);
                event.setStartId(startEventId);
                event.setDescription("Successfully applied port forwarding service " + securityGroup.getName() + " to virtual machine " + userVm.getName());
                String params = "sgId="+securityGroup.getId()+"\nvmId="+vmId+"\nnumRules="+firewallRulesToApply.size()+"\ndcId="+userVm.getDataCenterId();
                event.setParameters(params);
                event.setLevel(EventVO.LEVEL_INFO);
                _eventDao.persist(event);

                _networkMgr.updateFirewallRules(publicIp, firewallRulesToApply, router);
            }

            txn.commit();
        } catch (Throwable e) {
            txn.rollback();
            if (e instanceof NetworkRuleConflictException) {
                throw (NetworkRuleConflictException) e;
            } else if (e instanceof InvalidParameterValueException) {
                throw (InvalidParameterValueException) e;
            } else if (e instanceof PermissionDeniedException) {
                throw (PermissionDeniedException) e;
            } else if (e instanceof InternalErrorException) {
                s_logger.warn("ManagementServer error", e);
                throw (InternalErrorException) e;
            }
            s_logger.warn("ManagementServer error", e);
        } finally {
            if (locked) {
                _publicIpAddressDao.release(publicIp);
            }
        }
    }

    @Override
    public long assignSecurityGroupAsync(Long userId, Long securityGroupId, List<Long> securityGroupIdList, String publicIp, Long vmId) {
        UserVm userVm = _userVmDao.findById(vmId);
        long eventId = saveScheduledEvent(userId, userVm.getAccountId(), EventTypes.EVENT_PORT_FORWARDING_SERVICE_APPLY, "applying port forwarding service for Vm with Id: "+vmId);
        SecurityGroupParam param = new SecurityGroupParam(userId, securityGroupId, securityGroupIdList, publicIp, vmId, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(userVm.getAccountId());
        job.setCmd("AssignSecurityGroup");
        job.setCmdInfo(gson.toJson(param));
        return _asyncMgr.submitAsyncJob(job);
    }

    @Override
    @DB
    public void removeSecurityGroup(long userId, long securityGroupId, String publicIp, long vmId, long startEventId) throws InvalidParameterValueException, PermissionDeniedException {
        // This gets complicated with overlapping rules. As an example:
        // security group 1 has the following port mappings: 22->22 on TCP,
        // 23->23 on TCP, 80->8080 on TCP
        // security group 2 has the following port mappings: 22->22 on TCP,
        // 7891->7891 on TCP
        // User assigns group 1 & 2 on 192.168.10.120 to vm 1
        // Later, user removed group 1 from 192.168.10.120 and vm 1
        // Final valid port mappings should be 22->22 and 7891->7891 which both
        // come from security group 2. The mapping
        // for port 22 should not be removed.
        boolean locked = false;
        UserVm userVm = _userVmDao.findById(vmId);
        if (userVm == null) {
            throw new InvalidParameterValueException("Unable to find vm: " + vmId);
        }
        saveStartedEvent(userId, userVm.getAccountId(), EventTypes.EVENT_PORT_FORWARDING_SERVICE_REMOVE, "Removing security groups for Vm with Id: "+vmId, startEventId);
        SecurityGroupVO securityGroup = _securityGroupDao.findById(Long.valueOf(securityGroupId));
        if (securityGroup == null) {
            throw new InvalidParameterValueException("Unable to find port forwarding service: " + securityGroupId);
        }

        DomainRouterVO router = null;
        if (userVm.getDomainRouterId() != null)
        	router = _routerDao.findById(userVm.getDomainRouterId());
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find router for ip address: " + publicIp);
        }

        Transaction txn = Transaction.currentTxn();
        try {
            IPAddressVO ipVO = _publicIpAddressDao.acquire(publicIp);
            if (ipVO == null) {
                // throw this exception because hackers can use the api to probe
                // for allocated ips
                throw new PermissionDeniedException("User does not own supplied address");
            }

            locked = true;
            if ((ipVO.getAllocated() == null) || (ipVO.getAccountId() == null) || (ipVO.getAccountId().longValue() != userVm.getAccountId())) {
                throw new PermissionDeniedException("User/account does not own supplied address");
            }

            txn.start();

            // get the account for writing events
            Account account = _accountDao.findById(userVm.getAccountId());
            {
                // - send one command to agent to remove *all* rules for
                // publicIp/vm combo
                // - add back all rules based on existing SG mappings
                List<FirewallRuleVO> fwRulesToRemove = _firewallRulesDao.listForwardingByPubAndPrivIp(true, publicIp, userVm.getGuestIpAddress());
                for (FirewallRuleVO fwRule : fwRulesToRemove) {
                    fwRule.setEnabled(false);
                }

                List<FirewallRuleVO> updatedRules = _networkMgr.updateFirewallRules(null, fwRulesToRemove, router);

                // Save and create the event
                String description;
                String type = EventTypes.EVENT_NET_RULE_DELETE;
                String ruleName = "ip forwarding";
                String level = EventVO.LEVEL_INFO;

                for (FirewallRuleVO fwRule : updatedRules) {
                    _firewallRulesDao.remove(fwRule.getId());

                    description = "deleted " + ruleName + " rule [" + fwRule.getPublicIpAddress() + ":" + fwRule.getPublicPort() + "]->[" + fwRule.getPrivateIpAddress() + ":"
                            + fwRule.getPrivatePort() + "]" + " " + fwRule.getProtocol();

                    saveEvent(userId, account.getId(), level, type, description);
                }
            }

            // since we know these groups all pass muster, just keep track
            // of the public ports we are mapping on this public IP and
            // don't duplicate
            List<String> alreadyMappedPorts = new ArrayList<String>();
            List<FirewallRuleVO> fwRulesToAdd = new ArrayList<FirewallRuleVO>();
            List<SecurityGroupVMMapVO> sgVmMappings = _securityGroupVMMapDao.listByIpAndInstanceId(publicIp, vmId);
            for (SecurityGroupVMMapVO sgVmMapping : sgVmMappings) {
                if (sgVmMapping.getSecurityGroupId() == securityGroupId) {
                    _securityGroupVMMapDao.remove(sgVmMapping.getId());
                } else {
                    List<NetworkRuleConfigVO> netRules = _networkRuleConfigDao.listBySecurityGroupId(sgVmMapping.getSecurityGroupId());
                    for (NetworkRuleConfigVO netRule : netRules) {
                        if (!alreadyMappedPorts.contains(netRule.getPublicPort())) {
                            FirewallRuleVO newFwRule = new FirewallRuleVO();
                            newFwRule.setEnabled(true);
                            newFwRule.setForwarding(true);
                            newFwRule.setPrivatePort(netRule.getPrivatePort());
                            newFwRule.setProtocol(netRule.getProtocol());
                            newFwRule.setPublicPort(netRule.getPublicPort());
                            newFwRule.setPublicIpAddress(publicIp);
                            newFwRule.setPrivateIpAddress(userVm.getGuestIpAddress());
                            newFwRule.setGroupId(netRule.getSecurityGroupId());

                            fwRulesToAdd.add(newFwRule);

                            alreadyMappedPorts.add(netRule.getPublicPort());
                        }
                    }
                }
            }

            for (FirewallRuleVO addedRule : fwRulesToAdd) {
                _firewallRulesDao.persist(addedRule);

                String description = "created new ip forwarding rule [" + addedRule.getPublicIpAddress() + ":" + addedRule.getPublicPort() + "]->["
                        + addedRule.getPrivateIpAddress() + ":" + addedRule.getPrivatePort() + "]" + " " + addedRule.getProtocol();

                saveEvent(userId, account.getId(), EventVO.LEVEL_INFO, EventTypes.EVENT_NET_RULE_ADD, description);
            }

            // save off an event for removing the security group
            EventVO event = new EventVO();
            event.setUserId(userId);
            event.setAccountId(userVm.getAccountId());
            event.setType(EventTypes.EVENT_PORT_FORWARDING_SERVICE_REMOVE);
            event.setDescription("Successfully removed port forwarding service " + securityGroup.getName() + " from virtual machine " + userVm.getName());
            event.setLevel(EventVO.LEVEL_INFO);
            String params = "sgId="+securityGroup.getId()+"\nvmId="+vmId;
            event.setParameters(params);
            _eventDao.persist(event);

            _networkMgr.updateFirewallRules(publicIp, fwRulesToAdd, router);

            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Unhandled exception", ex);
        } finally {
            if (locked) {
                _publicIpAddressDao.release(publicIp);
            }
        }
    }

    @Override
    public long removeSecurityGroupAsync(Long userId, long securityGroupId, String publicIp, long vmId) {
        UserVm userVm = _userVmDao.findById(vmId);
        long eventId = saveScheduledEvent(userId, userVm.getAccountId(), EventTypes.EVENT_PORT_FORWARDING_SERVICE_REMOVE, "removing security groups for Vm with Id: "+vmId);
        SecurityGroupParam param = new SecurityGroupParam(userId, securityGroupId, null, publicIp, vmId, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(userVm.getAccountId());
        job.setCmd("RemoveSecurityGroup");
        job.setCmdInfo(gson.toJson(param));
        return _asyncMgr.submitAsyncJob(job);
    }

    @Override
    public Long validateSecurityGroupsAndInstance(List<Long> securityGroupIds, Long instanceId) {
        if ((securityGroupIds == null) || securityGroupIds.isEmpty() || (instanceId == null)) {
            return null;
        }

        List<SecurityGroupVO> securityGroups = new ArrayList<SecurityGroupVO>();
        for (Long securityGroupId : securityGroupIds) {
            if (securityGroupId.longValue() == 0) {
                continue;
            }
            SecurityGroupVO securityGroup = _securityGroupDao.findById(securityGroupId);
            if (securityGroup == null) {
                return null;
            }
            securityGroups.add(securityGroup);
        }

        UserVm userVm = _userVmDao.findById(instanceId);
        if (userVm == null) {
            return null;
        }

        long accountId = userVm.getAccountId();
        for (SecurityGroupVO securityGroup : securityGroups) {
            Long sgAccountId = securityGroup.getAccountId();
            if ((sgAccountId != null) && (sgAccountId.longValue() != accountId)) {
                return null;
            }
        }
        return Long.valueOf(accountId);
    }

    private FirewallRuleVO createFirewallRule(long userId, String ipAddress, UserVm userVm, String publicPort, String privatePort, String protocol, Long securityGroupId) throws NetworkRuleConflictException {
        // sanity check that the vm can be applied to the load balancer
        ServiceOfferingVO offering = _offeringsDao.findById(userVm.getServiceOfferingId());
        if ((offering == null) || !GuestIpType.Virtualized.equals(offering.getGuestIpType())) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to create port forwarding rule (" + protocol + ":" + publicPort + "->" + privatePort+ ") for virtual machine " + userVm.toString() + ", bad network type (" + ((offering == null) ? "null" : offering.getGuestIpType()) + ")");
            }

            throw new IllegalArgumentException("Unable to create port forwarding rule (" + protocol + ":" + publicPort + "->" + privatePort+ ") for virtual machine " + userVm.toString() + ", bad network type (" + ((offering == null) ? "null" : offering.getGuestIpType()) + ")");
        }

        // check for ip address/port conflicts by checking existing forwarding and load balancing rules
        List<FirewallRuleVO> existingRulesOnPubIp = _firewallRulesDao.listIPForwarding(ipAddress);
        Map<String,StringBuilder> mappedPublicPorts = new HashMap<String, StringBuilder>();
        Map<String, StringBuilder> publicPortToProtocolMapping=new HashMap<String, StringBuilder>();
        if (existingRulesOnPubIp != null) {
            for (FirewallRuleVO fwRule : existingRulesOnPubIp) {
            	
                //mappedPublicPorts.put(fwRule.getPublicPort(), new Pair<String, String>(fwRule.getPrivateIpAddress(), fwRule.getPrivatePort()));
            	if(mappedPublicPorts.containsKey(fwRule.getPublicPort())){
            		mappedPublicPorts.put(fwRule.getPublicPort(), mappedPublicPorts.get(fwRule.getPublicPort()).append(";").append(fwRule.getPrivateIpAddress().concat(",").concat(fwRule.getPrivatePort())));
            	}
            	else{
            		mappedPublicPorts.put(fwRule.getPublicPort(), new StringBuilder(fwRule.getPrivateIpAddress()+","+fwRule.getPrivatePort()));
            	}
            	
            	if(publicPortToProtocolMapping.containsKey(fwRule.getPublicPort())){
            		publicPortToProtocolMapping.put(fwRule.getPublicPort(), publicPortToProtocolMapping.get(fwRule.getPublicPort()).append(";").append(fwRule.getProtocol()));
            	}
            	else{
            		publicPortToProtocolMapping.put(fwRule.getPublicPort(),new StringBuilder(fwRule.getProtocol()));
            	}
            }
        }

        if (userVm != null) 
        {
        	if(mappedPublicPorts.size()>0)
        	{
        		StringBuilder privateIpPortIntermediate = mappedPublicPorts.get(publicPort);
	            String privateIpPort = null;
	            if(privateIpPortIntermediate != null && privateIpPortIntermediate.length()>0)
	            	privateIpPort = privateIpPortIntermediate.toString();//eg: 10.1.1.2,30 ; 10.1.1.2,34
	            
	            if (privateIpPort != null && privateIpPort.length()>0) 
	            {
	                String publicPortProtocol = publicPortToProtocolMapping.get(publicPort).toString();
	                String[] privateIpPortPairs = privateIpPort.toString().split(";"); //eg. 10.1.1.2,30
	                String[] privateIpAndPortStr;
	                boolean errFlag = false;
	
	            	for(String pair: privateIpPortPairs)
	            	{
	            		privateIpAndPortStr = pair.split(",");//split into 10.1.1.2 & 30
	            	
		                if (privateIpAndPortStr[0].equals(userVm.getGuestIpAddress()) && privateIpAndPortStr[1].equals(privatePort) && publicPortProtocol.contains(protocol)) {
		                    if (s_logger.isDebugEnabled()) {
		                        s_logger.debug("skipping the creating of firewall rule " + ipAddress + ":" + publicPort + " to " + userVm.getGuestIpAddress() + ":" + privatePort + "; rule already exists.");
		                    }
		                    return null; // already mapped
		                }
		                //at this point protocol string looks like: eg. tcp;udp || tcp || udp || udp;tcp 
		                else if(!publicPortProtocol.contains(protocol))//check if this public port is mapped to the protocol or not
		                {
		                	//this is the case eg:
		                	//pub:1 pri:2 pro: tcp
		                	//pub 1 pri:3 pro: udp
		                	break; //we break here out of the loop, for the record to be created
		                }
		                else
		                {
		                	errFlag = true;
	//	                    throw new NetworkRuleConflictException("An existing port forwarding service rule for " + ipAddress + ":" + publicPort
	//	                            + " already exists, found while trying to create mapping to " + userVm.getGuestIpAddress() + ":" + privatePort + ((securityGroupId == null) ? "." : " from port forwarding service "
	//	                            + securityGroupId.toString() + "."));
		                }
	            	}
	            	
	            	if(errFlag)
	                    throw new NetworkRuleConflictException("An existing port forwarding service rule for " + ipAddress + ":" + publicPort
	                            + " already exists, found while trying to create mapping to " + userVm.getGuestIpAddress() + ":" + privatePort + ((securityGroupId == null) ? "." : " from port forwarding service "
	                            + securityGroupId.toString() + "."));
	            }
        	}
            FirewallRuleVO newFwRule = new FirewallRuleVO();
            newFwRule.setEnabled(true);
            newFwRule.setForwarding(true);
            newFwRule.setPrivatePort(privatePort);
            newFwRule.setProtocol(protocol);
            newFwRule.setPublicPort(publicPort);
            newFwRule.setPublicIpAddress(ipAddress);
            newFwRule.setPrivateIpAddress(userVm.getGuestIpAddress());
            newFwRule.setGroupId(securityGroupId);

            // In 1.0 the rules were always persisted when a user created a rule.  When the rules get sent down
            // the stopOnError parameter is set to false, so the agent will apply all rules that it can.  That
            // behavior is preserved here by persisting the rule before sending it to the agent.
            _firewallRulesDao.persist(newFwRule);

            boolean success = _networkMgr.updateFirewallRule(newFwRule, null, null);

            // Save and create the event
            String description;
            String ruleName = "ip forwarding";
            String level = EventVO.LEVEL_INFO;
            Account account = _accountDao.findById(userVm.getAccountId());

            if (success == true) {
                description = "created new " + ruleName + " rule [" + newFwRule.getPublicIpAddress() + ":" + newFwRule.getPublicPort() + "]->["
                        + newFwRule.getPrivateIpAddress() + ":" + newFwRule.getPrivatePort() + "]" + " " + newFwRule.getProtocol();
            } else {
                level = EventVO.LEVEL_ERROR;
                description = "failed to create new " + ruleName + " rule [" + newFwRule.getPublicIpAddress() + ":" + newFwRule.getPublicPort() + "]->["
                        + newFwRule.getPrivateIpAddress() + ":" + newFwRule.getPrivatePort() + "]" + " " + newFwRule.getProtocol();
            }

            saveEvent(Long.valueOf(userId), account.getId(), level, EventTypes.EVENT_NET_RULE_ADD, description);

            return newFwRule;
        }
        return null;
    }

    @DB
    protected NetworkRuleConfigVO createNetworkRuleConfig(long userId, long securityGroupId, String port, String privatePort, String protocol, String algorithm)
            throws NetworkRuleConflictException {
        if (protocol == null) {
            protocol = "TCP";
        }

        Long ruleId = null;
        Transaction txn = Transaction.currentTxn();
        try {
            List<NetworkRuleConfigVO> existingRules = _networkRuleConfigDao.listBySecurityGroupId(securityGroupId);
            for (NetworkRuleConfigVO existingRule : existingRules) {
                if (existingRule.getPublicPort().equals(port) && existingRule.getProtocol().equals(protocol)) {
                    throw new NetworkRuleConflictException("port conflict, port forwarding service contains a rule on public port " + port + " for protocol " + protocol);
                }
            }

            txn.start();
            NetworkRuleConfigVO netRule = new NetworkRuleConfigVO(securityGroupId, port, privatePort, protocol);
            netRule.setCreateStatus(AsyncInstanceCreateStatus.Creating);
            netRule = _networkRuleConfigDao.persist(netRule);
            ruleId = netRule.getId();
            txn.commit();

            // check if we are within context of async-execution
            AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
            if (asyncExecutor != null) {
                AsyncJobVO job = asyncExecutor.getJob();

                if (s_logger.isInfoEnabled())
                    s_logger.info("Created a new port forwarding service rule instance " + ruleId + ", update async job-" + job.getId() + " progress status");

                _asyncMgr.updateAsyncJobAttachment(job.getId(), "network_rule_config", ruleId);
                _asyncMgr.updateAsyncJobStatus(job.getId(), BaseCmd.PROGRESS_INSTANCE_CREATED, ruleId);
            }

            txn.start();
            if (ruleId != null) {
                List<SecurityGroupVMMapVO> sgMappings = _securityGroupVMMapDao.listBySecurityGroup(securityGroupId);
                if ((sgMappings != null) && !sgMappings.isEmpty()) {
                    for (SecurityGroupVMMapVO sgMapping : sgMappings) {
                        UserVm userVm = _userVmDao.findById(sgMapping.getInstanceId());
                        createFirewallRule(userId, sgMapping.getIpAddress(), userVm, netRule.getPublicPort(), netRule.getPrivatePort(), netRule.getProtocol(), Long.valueOf(securityGroupId));
                    }
                }

                NetworkRuleConfigVO rule = _networkRuleConfigDao.findById(ruleId);
                rule.setCreateStatus(AsyncInstanceCreateStatus.Created);
                _networkRuleConfigDao.update(ruleId, rule);
            }

            txn.commit();
        } catch (Exception ex) {
            txn.rollback();

            if (ruleId != null) {
                txn.start();
                NetworkRuleConfigVO rule = _networkRuleConfigDao.findById(ruleId);
                rule.setCreateStatus(AsyncInstanceCreateStatus.Corrupted);
                _networkRuleConfigDao.update(ruleId, rule);
                txn.commit();
            }

            if (ex instanceof NetworkRuleConflictException) {
                throw (NetworkRuleConflictException) ex;
            }
            s_logger.error("Unexpected exception creating port forwarding service rule (pfServiceId:" + securityGroupId + ",port:" + port + ",privatePort:" + privatePort + ",protocol:" + protocol + ")",
                    ex);
        }

        return _networkRuleConfigDao.findById(ruleId);
    }

    @Override
    public boolean deleteNetworkRuleConfig(long userId, long networkRuleId) {
        try {
            NetworkRuleConfigVO netRule = _networkRuleConfigDao.findById(networkRuleId);
            if (netRule != null) {
                List<SecurityGroupVMMapVO> sgMappings = _securityGroupVMMapDao.listBySecurityGroup(netRule.getSecurityGroupId());
                if ((sgMappings != null) && !sgMappings.isEmpty()) {
                    for (SecurityGroupVMMapVO sgMapping : sgMappings) {
                        UserVm userVm = _userVmDao.findById(sgMapping.getInstanceId());
                        if (userVm != null) {
                            List<FirewallRuleVO> fwRules = _firewallRulesDao.listIPForwarding(sgMapping.getIpAddress(), netRule.getPublicPort(), true);
                            FirewallRuleVO rule = null;
                            for (FirewallRuleVO fwRule : fwRules) {
                                if (fwRule.getPrivatePort().equals(netRule.getPrivatePort()) && fwRule.getPrivateIpAddress().equals(userVm.getGuestIpAddress())) {
                                    rule = fwRule;
                                    break;
                                }
                            }

                            if (rule != null) {
                                rule.setEnabled(false);
                                _networkMgr.updateFirewallRule(rule, null, null);

                                // Save and create the event
                                Account account = _accountDao.findById(userVm.getAccountId());

                                _firewallRulesDao.remove(rule.getId());
                                String description = "deleted ip forwarding rule [" + rule.getPublicIpAddress() + ":" + rule.getPublicPort() + "]->[" + rule.getPrivateIpAddress()
                                                     + ":" + rule.getPrivatePort() + "]" + " " + rule.getProtocol();

                                saveEvent(Long.valueOf(userId), account.getId(), EventVO.LEVEL_INFO, EventTypes.EVENT_NET_RULE_DELETE, description);
                            }
                        }
                    }
                }
                _networkRuleConfigDao.remove(netRule.getId());
            }
        } catch (Exception ex) {
            s_logger.error("Unexpected exception deleting port forwarding service rule " + networkRuleId, ex);
            return false;
        }

        return true;
    }

    @Override
    public long deleteNetworkRuleConfigAsync(long userId, Account account, Long networkRuleId) throws PermissionDeniedException {
        // do a quick permissions check to make sure the account is either an
        // admin or the owner of the security group to which the network rule
        // belongs
        NetworkRuleConfigVO netRule = _networkRuleConfigDao.findById(networkRuleId);
        long accountId = Account.ACCOUNT_ID_SYSTEM;
        if (netRule != null) {
            SecurityGroupVO sg = _securityGroupDao.findById(netRule.getSecurityGroupId());
            if (account != null) {
                if (!BaseCmd.isAdmin(account.getType())) {
                    if ((sg.getAccountId() == null) || (sg.getAccountId().longValue() != account.getId().longValue())) {
                        throw new PermissionDeniedException("Unable to delete port forwarding service rule " + networkRuleId + "; account: " + account.getAccountName() + " is not the owner");
                    }
                } else if (!isChildDomain(account.getDomainId(), sg.getDomainId())) {
                    throw new PermissionDeniedException("Unable to delete port forwarding service rule " + networkRuleId + "; account: " + account.getAccountName() + " is not an admin in the domain hierarchy.");
                }
            }
            if (sg != null) {
                accountId = sg.getAccountId().longValue();
            }
        } else {
            return 0L;  // failed to delete due to netRule not found
        }

        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(accountId);
        job.setCmd("DeleteNetworkRuleConfig");
        job.setCmdInfo(gson.toJson(networkRuleId));
        
        return _asyncMgr.submitAsyncJob(job);
    }


    @Override
    public List<EventVO> getEvents(long userId, long accountId, Long domainId, String type, String level, Date startDate, Date endDate) {
        SearchCriteria sc = _eventDao.createSearchCriteria();
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
    public List<UserAccountVO> searchForUsers(Criteria c) {
        Filter searchFilter = new Filter(UserAccountVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());

        Object id = c.getCriteria(Criteria.ID);
        Object username = c.getCriteria(Criteria.USERNAME);
        Object type = c.getCriteria(Criteria.TYPE);
        Object domainId = c.getCriteria(Criteria.DOMAINID);
        Object account = c.getCriteria(Criteria.ACCOUNTNAME);
        Object state = c.getCriteria(Criteria.STATE);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        SearchBuilder<UserAccountVO> sb = _userAccountDao.createSearchBuilder();
        sb.and("username", sb.entity().getUsername(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        sb.and("accountName", sb.entity().getAccountName(), SearchCriteria.Op.LIKE);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);

        if ((account == null) && (domainId != null)) {
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId());
        }

        SearchCriteria sc = sb.create();
        if (keyword != null) {
            SearchCriteria ssc = _userAccountDao.createSearchCriteria();
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

        if (account != null) {
            sc.setParameters("accountName", "%" + account + "%");
            if (domainId != null) {
                sc.setParameters("domainId", domainId);
            }
        } else if (domainId != null) {
            DomainVO domainVO = _domainDao.findById((Long)domainId);
            sc.setJoinParameters("domainSearch", "path", domainVO.getPath() + "%");
        }

        if (state != null) {
            sc.setParameters("state", state);
        }

        return _userAccountDao.search(sc, searchFilter);
    }

    @Override
    public List<ServiceOfferingVO> searchForServiceOfferings(Criteria c) {
        Filter searchFilter = new Filter(ServiceOfferingVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria sc = _offeringsDao.createSearchCriteria();

        Object name = c.getCriteria(Criteria.NAME);
        Object vmIdObj = c.getCriteria(Criteria.INSTANCEID);
        Object id = c.getCriteria(Criteria.ID);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        if (keyword != null) {
            SearchCriteria ssc = _offeringsDao.createSearchCriteria();
            ssc.addOr("displayText", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }

        if (vmIdObj != null) {
            UserVmVO vm = _userVmDao.findById((Long) vmIdObj);
            if (vm != null) {
                ServiceOfferingVO offering = _offeringsDao.findById(vm.getServiceOfferingId());
                sc.addAnd("id", SearchCriteria.Op.NEQ, offering.getId());
                
                // Only return offerings with the same Guest IP type and storage pool preference
                sc.addAnd("guestIpType", SearchCriteria.Op.EQ, offering.getGuestIpType());
                sc.addAnd("useLocalStorage", SearchCriteria.Op.EQ, offering.getUseLocalStorage());
            }
        }

        return _offeringsDao.search(sc, searchFilter);
    }
    
    @Override
    public List<ClusterVO> searchForClusters(Criteria c) {
        Filter searchFilter = new Filter(ClusterVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria sc = _clusterDao.createSearchCriteria();

        Object id = c.getCriteria(Criteria.ID);
        Object name = c.getCriteria(Criteria.NAME);
        Object podId = c.getCriteria(Criteria.PODID);
        Object zoneId = c.getCriteria(Criteria.DATACENTERID);

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
    public List<HostVO> searchForServers(Criteria c) {
        Filter searchFilter = new Filter(HostVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria sc = _hostDao.createSearchCriteria();

        Object name = c.getCriteria(Criteria.NAME);
        Object type = c.getCriteria(Criteria.TYPE);
        Object state = c.getCriteria(Criteria.STATE);
        Object zone = c.getCriteria(Criteria.DATACENTERID);
        Object pod = c.getCriteria(Criteria.PODID);
        Object id = c.getCriteria(Criteria.ID);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        if (keyword != null) {
            SearchCriteria ssc = _hostDao.createSearchCriteria();
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

        return _hostDao.search(sc, searchFilter);
    }

    @Override
    public List<HostPodVO> searchForPods(Criteria c) {
        Filter searchFilter = new Filter(HostPodVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria sc = _hostPodDao.createSearchCriteria();

        String podName = (String) c.getCriteria(Criteria.NAME);
        Long id = (Long) c.getCriteria(Criteria.ID);
        Long zoneId = (Long) c.getCriteria(Criteria.DATACENTERID);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        if (keyword != null) {
            SearchCriteria ssc = _hostPodDao.createSearchCriteria();
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
        SearchCriteria sc = _dcDao.createSearchCriteria();

        String zoneName = (String) c.getCriteria(Criteria.ZONENAME);

        if (zoneName != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + zoneName + "%");
        }

        return _dcDao.search(sc, searchFilter);

    }
    
    @Override
    public List<VlanVO> searchForVlans(Criteria c) {
    	Filter searchFilter = new Filter(VlanVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
    	
        Object id = c.getCriteria(Criteria.ID);
        Object vlan = c.getCriteria(Criteria.VLAN);
        Object dataCenterId = c.getCriteria(Criteria.DATACENTERID);
        Object accountId = c.getCriteria(Criteria.ACCOUNTID);
        Object podId = c.getCriteria(Criteria.PODID);
        Object keyword = c.getCriteria(Criteria.KEYWORD);
        
        SearchBuilder<VlanVO> sb = _vlanDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("vlan", sb.entity().getVlanId(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
       
        if (accountId != null) {
        	SearchBuilder<AccountVlanMapVO> accountVlanMapSearch = _accountVlanMapDao.createSearchBuilder();
        	accountVlanMapSearch.and("accountId", accountVlanMapSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        	sb.join("accountVlanMapSearch", accountVlanMapSearch, sb.entity().getId(), accountVlanMapSearch.entity().getVlanDbId());
        }
        
        if (podId != null) {
        	SearchBuilder<PodVlanMapVO> podVlanMapSearch = _podVlanMapDao.createSearchBuilder();
        	podVlanMapSearch.and("podId", podVlanMapSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        	sb.join("podVlanMapSearch", podVlanMapSearch, sb.entity().getId(), podVlanMapSearch.entity().getVlanDbId());
        }
        
        SearchCriteria sc = sb.create();
        if (keyword != null) {
            SearchCriteria ssc = _vlanDao.createSearchCriteria();
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
    public Long getAccountIdForVlan(long vlanDbId) {
    	List<AccountVlanMapVO> accountVlanMaps = _accountVlanMapDao.listAccountVlanMapsByVlan(vlanDbId);
    	if (accountVlanMaps.isEmpty()) {
    		return null;
    	} else {
    		return accountVlanMaps.get(0).getAccountId();
    	}
    }

    @Override
    public List<ConfigurationVO> searchForConfigurations(Criteria c, boolean showHidden) {
        Filter searchFilter = new Filter(ConfigurationVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria sc = _configDao.createSearchCriteria();

        Object name = c.getCriteria(Criteria.NAME);
        Object category = c.getCriteria(Criteria.CATEGORY);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        if (keyword != null) {
            SearchCriteria ssc = _configDao.createSearchCriteria();
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

        if (!showHidden) {
        	sc.addAnd("category", SearchCriteria.Op.NEQ, "Hidden");
        }

        return _configDao.search(sc, searchFilter);
    }

    @Override
    public List<HostVO> searchForAlertServers(Criteria c) {
        Filter searchFilter = new Filter(HostVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria sc = _hostDao.createSearchCriteria();

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
        
        SearchCriteria sc = sb.create();
        
        if (keyword != null) {
            SearchCriteria ssc = _templateDao.createSearchCriteria();
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
    public List<VMTemplateVO> listTemplates(Long templateId, String name, String keyword, TemplateFilter templateFilter, boolean isIso, Boolean bootable, Long accountId, Integer pageSize, Long startIndex, Long zoneId) throws InvalidParameterValueException {
        VMTemplateVO template = null;
    	if (templateId != null) {
    		template = _templateDao.findById(templateId);
    		if (template == null) {
    			throw new InvalidParameterValueException("Please specify a valid template ID.");
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
    		templates = _templateDao.searchTemplates(name, keyword, templateFilter, isIso, bootable, account, domain, pageSize, startIndex, zoneId);
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
    public List<VMTemplateHostVO> listTemplateHostBy(long templateId, Long zoneId) {
    	if (zoneId != null) {
    		HostVO secondaryStorageHost = _storageMgr.getSecondaryStorageHost(zoneId);
    		if (secondaryStorageHost == null) {
    			return new ArrayList<VMTemplateHostVO>();
    		} else {
    			return _templateHostDao.listByHostTemplate(secondaryStorageHost.getId(), templateId);
    		}
    	} else {
    		return _templateHostDao.listByTemplateId(templateId);
    	}
    }

    @Override
    public List<HostPodVO> listPods(long dataCenterId) {
        return _hostPodDao.listByDataCenterId(dataCenterId);
    }

    @Override
    public PricingVO findPricingByTypeAndId(String type, Long id) {
        return _pricingDao.findByTypeAndId(type, id);
    }

    @Override
    public Long createPricing(Long id, float price, String priceUnit, String type, Long typeId, Date created) {
        PricingVO pricing = new PricingVO(id, price, priceUnit, type, typeId, created);
        return _pricingDao.persist(pricing).getId();
    }

    @Override
    public void updateConfiguration(long userId, String name, String value) throws InvalidParameterValueException, InternalErrorException {
        _configMgr.updateConfiguration(userId, name, value);
    }

    @Override
    public ServiceOfferingVO createServiceOffering(long userId, String name, int cpu, int ramSize, int speed, String displayText, boolean localStorageRequired, boolean offerHA, boolean useVirtualNetwork, String tags) {
        return _configMgr.createServiceOffering(userId, name, cpu, ramSize, speed, displayText, localStorageRequired, offerHA, useVirtualNetwork, tags);
    }
    
    @Override
    public ServiceOfferingVO updateServiceOffering(long userId, long serviceOfferingId, String name, String displayText, Boolean offerHA, Boolean useVirtualNetwork, String tags) {
    	return _configMgr.updateServiceOffering(userId, serviceOfferingId, name, displayText, offerHA, useVirtualNetwork, tags);
    }
    
    @Override
    public boolean deleteServiceOffering(long userId, long serviceOfferingId) {
        return _configMgr.deleteServiceOffering(userId, serviceOfferingId);
    }

    private void updatePricing(Long id, float price, String priceUnit, String type, Long typeId, Date created) {
        PricingVO pricing = new PricingVO(id, price, priceUnit, type, typeId, created);
        _pricingDao.update(pricing);
    }

    @Override
    public HostPodVO createPod(long userId, String podName, Long zoneId, String gateway, String cidr, String startIp, String endIp) throws InvalidParameterValueException, InternalErrorException {
        return _configMgr.createPod(userId, podName, zoneId, gateway, cidr, startIp, endIp);
    }

    @Override
    public HostPodVO editPod(long userId, long podId, String newPodName, String gateway, String cidr, String startIp, String endIp) throws InvalidParameterValueException, InternalErrorException {
        return _configMgr.editPod(userId, podId, newPodName, gateway, cidr, startIp, endIp);
    }

    @Override
    public void deletePod(long userId, long podId) throws InvalidParameterValueException, InternalErrorException {
        _configMgr.deletePod(userId, podId);
    }

    @Override
    public DataCenterVO createZone(long userId, String zoneName, String dns1, String dns2, String internalDns1, String internalDns2, String vnetRange,String guestCidr) throws InvalidParameterValueException, InternalErrorException {
        return _configMgr.createZone(userId, zoneName, dns1, dns2, internalDns1, internalDns2, vnetRange, guestCidr);
    }

    @Override
    public DataCenterVO editZone(long userId, Long zoneId, String newZoneName, String dns1, String dns2, String dns3, String dns4, String vnetRange, String guestCidr) throws InvalidParameterValueException, InternalErrorException {
        return _configMgr.editZone(userId, zoneId, newZoneName, dns1, dns2, dns3, dns4, vnetRange, guestCidr);
    }

    @Override
    public void deleteZone(long userId, Long zoneId) throws InvalidParameterValueException, InternalErrorException {
        _configMgr.deleteZone(userId, zoneId);
    }

    @Override
    public String changePrivateIPRange(boolean add, Long podId, String startIP, String endIP) throws InvalidParameterValueException {
        return _configMgr.changePrivateIPRange(add, podId, startIP, endIP);
    }

    private List<UserVO> findUsersLike(String username) {
        return _userDao.findUsersLike(username);
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
    public GuestOS findGuestOSById(Long id) {
        return this._guestOSDao.findById(id);
    }

    @Override
    public List<AccountVO> searchForAccounts(Criteria c) {
        Filter searchFilter = new Filter(AccountVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());

        Object id = c.getCriteria(Criteria.ID);
        Object accountname = c.getCriteria(Criteria.ACCOUNTNAME);
        Object domainId = c.getCriteria(Criteria.DOMAINID);
        Object type = c.getCriteria(Criteria.TYPE);
        Object state = c.getCriteria(Criteria.STATE);
        Object isCleanupRequired = c.getCriteria(Criteria.ISCLEANUPREQUIRED);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        SearchBuilder<AccountVO> sb = _accountDao.createSearchBuilder();
        sb.and("accountName", sb.entity().getAccountName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("nid", sb.entity().getId(), SearchCriteria.Op.NEQ);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("needsCleanup", sb.entity().getNeedsCleanup(), SearchCriteria.Op.EQ);

        if ((id == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId());
        }

        SearchCriteria sc = sb.create();
        if (keyword != null) {
            SearchCriteria ssc = _accountDao.createSearchCriteria();
            ssc.addOr("accountName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            
            sc.addAnd("accountName", SearchCriteria.Op.SC, ssc);
        }

        if (accountname != null) {
            sc.setParameters("accountName", "%" + accountname + "%");
        }

        if (id != null) {
            sc.setParameters("id", id);
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById((Long)domainId);

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
    public ResourceLimitVO updateResourceLimit(Long domainId, Long accountId, ResourceType type, Long max) throws InvalidParameterValueException {
        return _accountMgr.updateResourceLimit(domainId, accountId, type, max);
    }

    @Override
    public boolean deleteLimit(Long limitId) {
        // A limit ID must be passed in
        if (limitId == null)
            return false;

        return _resourceLimitDao.delete(limitId);
    }

    @Override
    public ResourceLimitVO findLimitById(long limitId) {
        return _resourceLimitDao.findById(limitId);
    }

    @Override
    public List<ResourceLimitVO> searchForLimits(Criteria c) {
        Long domainId = (Long) c.getCriteria(Criteria.DOMAINID);
        Long accountId = (Long) c.getCriteria(Criteria.ACCOUNTID);
        ResourceType type = (ResourceType) c.getCriteria(Criteria.TYPE);
        
        // For 2.0, we are just limiting the scope to having an user retrieve
        // limits for himself and if limits don't exist, use the ROOT domain's limits.
        // - Will
        List<ResourceLimitVO> limits = new ArrayList<ResourceLimitVO>();
        

        if(accountId!=null && domainId!=null)
        {
	        //if domainId==1 and account belongs to admin
	        //return all records for resource limits (bug 3778)
	        
	        if(domainId==1)
	        {
	        	AccountVO account = _accountDao.findById(accountId);
	        	
	        	if(account!=null && account.getType()==1)
	        	{
	        		//account belongs to admin
	        		//return all limits
	        		limits = _resourceLimitDao.listAll();
	        		return limits;
	        	}
	        }
	
	        //if account belongs to system, accountid=1,domainid=1
	        //return all the records for resource limits (bug:3778)
	        if(accountId==1 && domainId==1)
	        {
	        	limits = _resourceLimitDao.listAll();
	        	return limits;
	        }
        }
        
        if (accountId != null) {
        	SearchBuilder<ResourceLimitVO> sb = _resourceLimitDao.createSearchBuilder();
        	sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        	sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);

        	SearchCriteria sc = sb.create();

        	if (accountId != null) {
        		sc.setParameters("accountId", accountId);
        	}

        	if (type != null) {
        		sc.setParameters("type", type);
        	}
        	
        	// Listing all limits for an account
        	if (type == null) {
        		//List<ResourceLimitVO> userLimits = _resourceLimitDao.search(sc, searchFilter);
        		List<ResourceLimitVO> userLimits = _resourceLimitDao.listByAccountId(accountId);
	        	List<ResourceLimitVO> rootLimits = _resourceLimitDao.listByDomainId(DomainVO.ROOT_DOMAIN);
	        	ResourceType resourceTypes[] = ResourceType.values();
        	
	        	for (ResourceType resourceType: resourceTypes) {
	        		boolean found = false;
	        		for (ResourceLimitVO userLimit : userLimits) {
	        			if (userLimit.getType() == resourceType) {
	        				limits.add(userLimit);
	        				found = true;
	        				break;
	        			}
	        		}
	        		if (!found) {
	        			// Check the ROOT domain
	        			for (ResourceLimitVO rootLimit : rootLimits) {
	        				if (rootLimit.getType() == resourceType) {
	        					limits.add(rootLimit);
	        					found = true;
	        					break;
	        				}
	        			}
	        		}
	        		if (!found) {
	        			limits.add(new ResourceLimitVO(domainId, accountId, resourceType, -1L));
	        		}
	        	}
        	} else {
        		AccountVO account = _accountDao.findById(accountId);
        		limits.add(new ResourceLimitVO(null, accountId, type, _accountMgr.findCorrectResourceLimit(account, type)));
        	}
        } else if (domainId != null) {
        	if (type == null) {
        		ResourceType resourceTypes[] = ResourceType.values();
        		List<ResourceLimitVO> domainLimits = _resourceLimitDao.listByDomainId(domainId);
        		for (ResourceType resourceType: resourceTypes) {
	        		boolean found = false;
	        		for (ResourceLimitVO domainLimit : domainLimits) {
	        			if (domainLimit.getType() == resourceType) {
	        				limits.add(domainLimit);
	        				found = true;
	        				break;
	        			}
	        		}
	        		if (!found) {
	        			limits.add(new ResourceLimitVO(domainId, null, resourceType, -1L));
	        		}
        		}
        	} else {
        		limits.add(_resourceLimitDao.findByDomainIdAndType(domainId, type));
        	}
        }
        return limits;
    }

    @Override
    public long findCorrectResourceLimit(ResourceType type, long accountId) {
        AccountVO account = _accountDao.findById(accountId);
        
        if (account == null) {
            return -1;
        }
        
        return _accountMgr.findCorrectResourceLimit(account, type);
    }
    
    @Override
    public long getResourceCount(ResourceType type, long accountId) {
    	AccountVO account = _accountDao.findById(accountId);
        
        if (account == null) {
            return -1;
        }
        
        return _accountMgr.getResourceCount(account, type);
    }

    @Override
    public List<VMTemplateVO> listIsos(Criteria c) {
        Filter searchFilter = new Filter(VMTemplateVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        Boolean ready = (Boolean) c.getCriteria(Criteria.READY);
        Boolean isPublic = (Boolean) c.getCriteria(Criteria.ISPUBLIC);
        Long creator = (Long) c.getCriteria(Criteria.CREATED_BY);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        SearchCriteria sc = _templateDao.createSearchCriteria();

        if (keyword != null) {
            SearchCriteria ssc = _templateDao.createSearchCriteria();
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
    public List<UserStatisticsVO> listUserStatsBy(Long accountId) {
        return _userStatsDao.listBy(accountId);
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
        return _offeringsDao.listAll();
    }

    @Override
    public List<HostVO> listAllActiveHosts() {
        return _hostDao.listAllActive();
    }

    @Override
    public DataCenterVO findDataCenterById(long dataCenterId) {
        return _dcDao.findById(dataCenterId);
    }

    @Override
    public VlanVO findVlanById(long vlanDbId) {
        return _vlanDao.findById(vlanDbId);
    }

    @Override
    public Long createTemplate(long userId, Long zoneId, String name, String displayText, boolean isPublic, boolean featured, String format, String diskType, String url, String chksum, boolean requiresHvm, int bits, boolean enablePassword, long guestOSId, boolean bootable) throws InvalidParameterValueException,IllegalArgumentException, ResourceAllocationException {
        try
        {
            if (name.length() > 32)
            {
                throw new InvalidParameterValueException("Template name should be less than 32 characters");
            }
            	
            if (!name.matches("^[\\p{Alnum} ._-]+")) {
                throw new InvalidParameterValueException("Only alphanumeric, space, dot, dashes and underscore characters allowed");
            }
        	
            ImageFormat imgfmt = ImageFormat.valueOf(format.toUpperCase());
            if (imgfmt == null) {
                throw new IllegalArgumentException("Image format is incorrect " + format + ". Supported formats are " + EnumUtils.listValues(ImageFormat.values()));
            }
            
            FileSystem fileSystem = FileSystem.valueOf(diskType);
            if (fileSystem == null) {
                throw new IllegalArgumentException("File system is incorrect " + diskType + ". Supported file systems are " + EnumUtils.listValues(FileSystem.values()));
            }
            
            URI uri = new URI(url);
            if ((uri.getScheme() == null) || (!uri.getScheme().equalsIgnoreCase("http") && !uri.getScheme().equalsIgnoreCase("https") && !uri.getScheme().equalsIgnoreCase("file"))) {
               throw new IllegalArgumentException("Unsupported scheme for url: " + url);
            }
            int port = uri.getPort();
            if (!(port == 80 || port == 443 || port == -1)) {
            	throw new IllegalArgumentException("Only ports 80 and 443 are allowed");
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
            
            // Check that the resource limit for templates/ISOs won't be exceeded
            UserVO user = _userDao.findById(userId);
            if (user == null) {
                throw new IllegalArgumentException("Unable to find user with id " + userId);
            }
        	AccountVO account = _accountDao.findById(user.getAccountId());
            if (_accountMgr.resourceLimitExceeded(account, ResourceType.template)) {
            	ResourceAllocationException rae = new ResourceAllocationException("Maximum number of templates and ISOs for account: " + account.getAccountName() + " has been exceeded.");
            	rae.setResourceType("template");
            	throw rae;
            }
            
            // If a zoneId is specified, make sure it is valid
            if (zoneId != null) {
            	if (_dcDao.findById(zoneId) == null) {
            		throw new IllegalArgumentException("Please specify a valid zone.");
            	}
            }
            VMTemplateVO systemvmTmplt = _templateDao.findRoutingTemplate();
            if (systemvmTmplt.getName().equalsIgnoreCase(name) || systemvmTmplt.getDisplayText().equalsIgnoreCase(displayText)) {
            	throw new IllegalArgumentException("Cannot use reserved names for templates");
            }
            
            return _tmpltMgr.create(userId, zoneId, name, displayText, isPublic, featured, imgfmt, fileSystem, uri, chksum, requiresHvm, bits, enablePassword, guestOSId, bootable);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL " + url);
        }
    }

    @Override
    public boolean updateTemplate(Long id, String name, String displayText, String format, Long guestOSId, Boolean passwordEnabled, Boolean bootable) throws InvalidParameterValueException {
    	boolean updateNeeded = !(name == null && displayText == null && format == null && guestOSId == null && passwordEnabled == null && bootable == null);
    	if (!updateNeeded) {
    		return true;
    	}
    	
    	VMTemplateVO template = _templateDao.createForUpdate(id);
    	
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
    	
        return _templateDao.update(id, template);
    }

    @Override
    public boolean deleteTemplate(long userId, long templateId, Long zoneId, long startEventId) throws InternalErrorException {
    	return _tmpltMgr.delete(userId, templateId, zoneId, startEventId);
    }
    
    @Override
    public long deleteTemplateAsync(long userId, long templateId, Long zoneId) throws InvalidParameterValueException {
    	UserVO user = _userDao.findById(userId);
    	if (user == null) {
    		throw new InvalidParameterValueException("Please specify a valid user.");
    	}
    	
    	VMTemplateVO template = _templateDao.findById(templateId);
    	if (template == null) {
    		throw new InvalidParameterValueException("Please specify a valid template.");
    	}
    	
    	if (template.getFormat() == ImageFormat.ISO) {
    		throw new InvalidParameterValueException("Please specify a valid template.");
    	}
    	
    	if (template.getUniqueName().equals("routing")) {
    		throw new InvalidParameterValueException("The DomR template cannot be deleted.");
    	}
    	
    	if (zoneId != null && (_hostDao.findSecondaryStorageHost(zoneId) == null)) {
    		throw new InvalidParameterValueException("Failed to find a secondary storage host in the specified zone.");
    	}

    	long eventId = saveScheduledEvent(userId, template.getAccountId(), EventTypes.EVENT_TEMPLATE_DELETE, "deleting template with Id: "+templateId);
    	
        DeleteTemplateParam param = new DeleteTemplateParam(userId, templateId, zoneId, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(template.getAccountId());
        job.setCmd("DeleteTemplate");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(DeleteTemplateCmd.getStaticName());
        
        return _asyncMgr.submitAsyncJob(job);
    }
    
    @Override
    public boolean copyTemplate(long userId, long templateId, long sourceZoneId, long destZoneId, long startEventId) throws InternalErrorException {
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
    public long copyTemplateAsync(long userId, long templateId, long sourceZoneId, long destZoneId) throws InvalidParameterValueException {
    	UserVO user = _userDao.findById(userId);
    	if (user == null) {
    		throw new InvalidParameterValueException("Please specify a valid user.");
    	}
    	
    	VMTemplateVO template = _templateDao.findById(templateId);
    	if (template == null) {
    		throw new InvalidParameterValueException("Please specify a valid template/ISO.");
    	}
    	
    	DataCenterVO sourceZone = _dcDao.findById(sourceZoneId);
    	if (sourceZone == null) {
    		throw new InvalidParameterValueException("Please specify a valid source zone.");
    	}
    	
    	DataCenterVO destZone = _dcDao.findById(destZoneId);
    	if (destZone == null) {
    		throw new InvalidParameterValueException("Please specify a valid destination zone.");
    	}
    	
    	if (sourceZoneId == destZoneId) {
    		throw new InvalidParameterValueException("Please specify different source and destination zones.");
    	}
    	
    	HostVO srcSecHost = _hostDao.findSecondaryStorageHost(sourceZoneId);
    	
    	if (srcSecHost == null) {
    		throw new InvalidParameterValueException("Source zone is not ready");
    	}
    	
    	if (_hostDao.findSecondaryStorageHost(destZoneId) == null) {
    		throw new InvalidParameterValueException("Destination zone is not ready.");
    	}
    	
       	VMTemplateHostVO srcTmpltHost = null;
        srcTmpltHost = _templateHostDao.findByHostTemplate(srcSecHost.getId(), templateId);
        if (srcTmpltHost == null || srcTmpltHost.getDestroyed() || srcTmpltHost.getDownloadState() != VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
        	throw new InvalidParameterValueException("Please specify a template that is installed on secondary storage host: " + srcSecHost.getName());
        }
        
        long eventId = saveScheduledEvent(userId, template.getAccountId(), EventTypes.EVENT_TEMPLATE_COPY, "copying template with Id: "+templateId+" from zone: "+sourceZoneId+" to: "+destZoneId);
        
        CopyTemplateParam param = new CopyTemplateParam(userId, templateId, sourceZoneId, destZoneId, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(template.getAccountId());
        job.setCmd("CopyTemplate");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(CopyTemplateCmd.getStaticName());
        
        return _asyncMgr.submitAsyncJob(job);
    }
    
    @Override
    public long deleteIsoAsync(long userId, long isoId, Long zoneId) throws InvalidParameterValueException {
    	UserVO user = _userDao.findById(userId);
    	if (user == null) {
    		throw new InvalidParameterValueException("Please specify a valid user.");
    	}
    	
    	VMTemplateVO iso = _templateDao.findById(isoId);
    	if (iso == null) {
    		throw new InvalidParameterValueException("Please specify a valid ISO.");
    	}
    	
    	if (iso.getFormat() != ImageFormat.ISO) {
    		throw new InvalidParameterValueException("Please specify a valid ISO.");
    	}
    	
    	if (zoneId != null && (_hostDao.findSecondaryStorageHost(zoneId) == null)) {
    		throw new InvalidParameterValueException("Failed to find a secondary storage host in the specified zone.");
    	}
    	
    	long eventId = saveScheduledEvent(userId, iso.getAccountId(), EventTypes.EVENT_ISO_DELETE, "deleting ISO with Id: "+isoId+" from zone: "+zoneId);
    	
    	DeleteTemplateParam param = new DeleteTemplateParam(userId, isoId, zoneId, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(iso.getAccountId());
        job.setCmd("DeleteTemplate");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(DeleteIsoCmd.getStaticName());
        
        return _asyncMgr.submitAsyncJob(job);
    }

    @Override
    public VMTemplateVO findTemplateById(long templateId) {
        return _templateDao.findById(templateId);
    }
    
    @Override
    public VMTemplateHostVO findTemplateHostRef(long templateId, long zoneId) {
    	HostVO secondaryStorageHost = _storageMgr.getSecondaryStorageHost(zoneId);
    	if (secondaryStorageHost == null) {
    		return null;
    	} else {
    		return _templateHostDao.findByHostTemplate(secondaryStorageHost.getId(), templateId);
    	}
    }
    

    @Override
    public List<UserVmVO> listUserVMsByHostId(long hostId) {
        return _userVmDao.listByHostId(hostId);
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

        sb.and("displayName", sb.entity().getDisplayName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("accountIdEQ", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("accountIdIN", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
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
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId());
        }

        // populate the search criteria with the values passed in
        SearchCriteria sc = sb.create();

        if (keyword != null) {
            SearchCriteria ssc = _userVmDao.createSearchCriteria();
            ssc.addOr("displayName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("group", SearchCriteria.Op.LIKE, "%" + keyword + "%");
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
        }
        if (pod != null) {
            sc.setParameters("podId", pod);
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
    public List<FirewallRuleVO> listIPForwarding(String publicIPAddress, boolean forwarding) {
        return _firewallRulesDao.listIPForwarding(publicIPAddress, forwarding);
    }

    @Override
    public FirewallRuleVO createPortForwardingRule(long userId, IPAddressVO ipAddressVO, UserVmVO userVM, String publicPort, String privatePort, String protocol) throws NetworkRuleConflictException {
        return createFirewallRule(userId, ipAddressVO.getAddress(), userVM, publicPort, privatePort, protocol, null);
    }

    @Override
    public FirewallRuleVO updatePortForwardingRule(long userId, String publicIp, String privateIp, String publicPort, String privatePort, String protocol) {
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
        }
        return null;
    }

    @Override
    public long updatePortForwardingRuleAsync(long userId, long accountId, String publicIp, String privateIp, String publicPort, String privatePort, String protocol) {
        CreateOrUpdateRuleParam param = new CreateOrUpdateRuleParam(true, userId, Long.valueOf(accountId), publicIp, publicPort, privateIp, privatePort, protocol, null, null, null);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(accountId);
        job.setCmd("UpdatePortForwardingRule");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator("portforwardingrule");
        
        return _asyncMgr.submitAsyncJob(job);
    }

    @Override @DB
    public LoadBalancerVO updateLoadBalancerRule(long userId, LoadBalancerVO loadBalancer, String privatePort, String algorithm) {
        String updatedPrivatePort = ((privatePort == null) ? loadBalancer.getPrivatePort() : privatePort);
        String updatedAlgorithm = ((algorithm == null) ? loadBalancer.getAlgorithm() : algorithm);

        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            loadBalancer.setPrivatePort(updatedPrivatePort);
            loadBalancer.setAlgorithm(updatedAlgorithm);
            _loadBalancerDao.update(loadBalancer.getId(), loadBalancer);

            List<FirewallRuleVO> fwRules = _firewallRulesDao.listByLoadBalancerId(loadBalancer.getId());
            if ((fwRules != null) && !fwRules.isEmpty()) {
                for (FirewallRuleVO fwRule : fwRules) {
                    fwRule.setPrivatePort(updatedPrivatePort);
                    fwRule.setAlgorithm(updatedAlgorithm);
                    _firewallRulesDao.update(fwRule.getId(), fwRule);
                }
            }
            txn.commit();
        } catch (RuntimeException ex) {
            s_logger.warn("Unhandled exception trying to update load balancer rule", ex);
            txn.rollback();
            throw ex;
        } finally {
            txn.close();
        }

        // now that the load balancer has been updated, reconfigure the HA Proxy on the router with all the LB rules 
        List<FirewallRuleVO> allLbRules = new ArrayList<FirewallRuleVO>();
        IPAddressVO ipAddress = _publicIpAddressDao.findById(loadBalancer.getIpAddress());
        List<IPAddressVO> ipAddrs = _networkMgr.listPublicIpAddressesInVirtualNetwork(loadBalancer.getAccountId(), ipAddress.getDataCenterId(), null);
        for (IPAddressVO ipv : ipAddrs) {
            List<FirewallRuleVO> rules = _firewallRulesDao.listIPForwarding(ipv.getAddress(), false);
            allLbRules.addAll(rules);
        }

        IPAddressVO ip = _publicIpAddressDao.findById(loadBalancer.getIpAddress());
        DomainRouterVO router = _routerDao.findBy(ip.getAccountId(), ip.getDataCenterId());
        _networkMgr.updateFirewallRules(loadBalancer.getIpAddress(), allLbRules, router);
        return _loadBalancerDao.findById(loadBalancer.getId());
    }

    @Override
    public LoadBalancerVO updateLoadBalancerRule(LoadBalancerVO loadBalancer, String name, String description) throws InvalidParameterValueException {
        if ((name == null) && (description == null)) {
            return loadBalancer; // nothing to do
        }

        LoadBalancerVO lbForUpdate = _loadBalancerDao.createForUpdate();
        // make sure the name's not already in use
        if (name != null) {
            LoadBalancerVO existingLB = _loadBalancerDao.findByAccountAndName(loadBalancer.getAccountId(), name);
            if ((existingLB != null) && (existingLB.getId().longValue() != loadBalancer.getId().longValue())) {
                throw new InvalidParameterValueException("Unable to update load balancer " + loadBalancer.getName() + " with new name " + name + ", the name is already in use.");
            }
            lbForUpdate.setName(name);
        }

        if (description != null) {
            lbForUpdate.setDescription(description);
        }
        _loadBalancerDao.update(loadBalancer.getId(), lbForUpdate);
        return _loadBalancerDao.findById(loadBalancer.getId());
    }

    @Override
    public long updateLoadBalancerRuleAsync(long userId, long accountId, long loadBalancerId, String name, String description, String privatePort, String algorithm) {
        UpdateLoadBalancerParam param = new UpdateLoadBalancerParam(userId, loadBalancerId, name, description, privatePort, algorithm);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(accountId);
        job.setCmd("UpdateLoadBalancerRule");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator("loadbalancer");

        return _asyncMgr.submitAsyncJob(job);
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
    public List<NetworkRuleConfigVO> searchForNetworkRules(Criteria c) {
        Filter searchFilter = new Filter(NetworkRuleConfigVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());

        Object groupId = c.getCriteria(Criteria.GROUPID);
        Object id = c.getCriteria(Criteria.ID);
        Object accountId = c.getCriteria(Criteria.ACCOUNTID);

        SearchBuilder<NetworkRuleConfigVO> sb = _networkRuleConfigDao.createSearchBuilder();
        if (id != null) {
            sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        }

        if (groupId != null) {
            sb.and("securityGroupId", sb.entity().getSecurityGroupId(), SearchCriteria.Op.EQ);
        }

        if (accountId != null) {
            // join with securityGroup table to make sure the account is the owner of the network rule
            SearchBuilder<SecurityGroupVO> securityGroupSearch = _securityGroupDao.createSearchBuilder();
            securityGroupSearch.and("accountId", securityGroupSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
            sb.join("groupId", securityGroupSearch, securityGroupSearch.entity().getId(), sb.entity().getSecurityGroupId());
        }

        SearchCriteria sc = sb.create();

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (groupId != null) {
            sc.setParameters("securityGroupId", groupId);
        }

        if (accountId != null) {
            sc.setJoinParameters("groupId", "accountId", accountId);
        }

        return _networkRuleConfigDao.search(sc, searchFilter);
    }

    @Override
    public List<EventVO> searchForEvents(Criteria c) {
        Filter searchFilter = new Filter(EventVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());

        Object[] userIds = (Object[]) c.getCriteria(Criteria.USERID);
        Object[] accountIds = (Object[]) c.getCriteria(Criteria.ACCOUNTID);
        Object username = c.getCriteria(Criteria.USERNAME);
        Object accountName = c.getCriteria(Criteria.ACCOUNTNAME);
        Object type = c.getCriteria(Criteria.TYPE);
        Object level = c.getCriteria(Criteria.LEVEL);
//        Object description = c.getCriteria(Criteria.DESCRIPTION);
        Date startDate = (Date) c.getCriteria(Criteria.STARTDATE);
        Date endDate = (Date) c.getCriteria(Criteria.ENDDATE);
        Object domainId = c.getCriteria(Criteria.DOMAINID);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        SearchBuilder<EventVO> sb = _eventDao.createSearchBuilder();
        sb.and("levelL", sb.entity().getLevel(), SearchCriteria.Op.LIKE);
        sb.and("userIdEQ", sb.entity().getUserId(), SearchCriteria.Op.EQ);
        sb.and("userIdIN", sb.entity().getUserId(), SearchCriteria.Op.IN);
        sb.and("accountIdEQ", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("accountIdIN", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("accountName", sb.entity().getAccountName(), SearchCriteria.Op.LIKE);
        sb.and("domainIdEQ", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
        sb.and("levelEQ", sb.entity().getLevel(), SearchCriteria.Op.EQ);
//        sb.and("description", sb.entity().getDescription(), SearchCriteria.Op.LIKE);
        sb.and("createDateB", sb.entity().getCreateDate(), SearchCriteria.Op.BETWEEN);
        sb.and("createDateG", sb.entity().getCreateDate(), SearchCriteria.Op.GTEQ);
        sb.and("createDateL", sb.entity().getCreateDate(), SearchCriteria.Op.LTEQ);

        if ((accountIds == null) && (accountName == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId());
        }

        SearchCriteria sc = sb.create();
        if (keyword != null) {
            SearchCriteria ssc = _eventDao.createSearchCriteria();
            ssc.addOr("type", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("level", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("level", SearchCriteria.Op.SC, ssc);
        }

//        if (keyword != null) {
//            sc.setParameters("levelL", "%" + keyword + "%");
//        } else if (level != null) {
//            sc.setParameters("levelEQ", level);
//        }

//        if (description != null) {
//        	sc.setParameters("description", "%" + description + "%");
//        }
        
        if(level!=null)
        	sc.setParameters("levelEQ", level);
        	
        if (userIds == null && username != null) {
            List<UserVO> users = findUsersLike((String) username);
            if (users == null || users.size() == 0) {
                return new ArrayList<EventVO>();
            }
            userIds = new Long[users.size()];
            for (int i = 0; i < users.size(); i++) {
                userIds[i] = users.get(i).getId();
            }
        }

        if (userIds != null) {
            if (userIds.length == 1) {
                if ((userIds[0] != null) && !((Long) userIds[0]).equals(Long.valueOf(-1))) {
                    sc.setParameters("userIdEQ", userIds[0]);
                }
            } else {
                sc.setParameters("userIdIN", userIds);
            }
        }
        if (accountIds != null) {
            if (accountIds.length == 1) {
                if ((accountIds[0] != null) && !((Long) accountIds[0]).equals(Long.valueOf(-1))) {
                    sc.setParameters("accountIdEQ", accountIds[0]);
                }
            } else {
                sc.setParameters("accountIdIN", accountIds);
            }
        } else if (domainId != null) {
            if (accountName != null) {
                sc.setParameters("domainIdEQ", domainId);
                sc.setParameters("accountName", "%" + accountName + "%");
                sc.addAnd("removed", SearchCriteria.Op.NULL);
            } else {
                DomainVO domain = _domainDao.findById((Long)domainId);
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
        return _routerDao.listAllActive();
    }

    @Override
    public List<DomainRouterVO> searchForRouters(Criteria c) {
        Filter searchFilter = new Filter(DomainRouterVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());

        Object[] accountIds = (Object[]) c.getCriteria(Criteria.ACCOUNTID);
        Object name = c.getCriteria(Criteria.NAME);
        Object state = c.getCriteria(Criteria.STATE);
        Object zone = c.getCriteria(Criteria.DATACENTERID);
        Object pod = c.getCriteria(Criteria.PODID);
        Object hostId = c.getCriteria(Criteria.HOSTID);
        Object domainId = c.getCriteria(Criteria.DOMAINID);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        SearchBuilder<DomainRouterVO> sb = _routerDao.createSearchBuilder();
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("hostId", sb.entity().getHostId(), SearchCriteria.Op.EQ);

        if ((accountIds == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId());
        }

        SearchCriteria sc = sb.create();
        if (keyword != null) {
            SearchCriteria ssc = _routerDao.createSearchCriteria();
            ssc.addOr("displayName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("instanceName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }

        if (accountIds != null) {
            sc.setParameters("accountId", accountIds);
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById((Long)domainId);
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
        SearchCriteria sc = _consoleProxyDao.createSearchCriteria();

        Object id = c.getCriteria(Criteria.ID);
        Object name = c.getCriteria(Criteria.NAME);
        Object state = c.getCriteria(Criteria.STATE);
        Object zone = c.getCriteria(Criteria.DATACENTERID);
        Object pod = c.getCriteria(Criteria.PODID);
        Object hostId = c.getCriteria(Criteria.HOSTID);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        if (keyword != null) {
            SearchCriteria ssc = _consoleProxyDao.createSearchCriteria();
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
    public VolumeVO findVolumeById(long id) {
         VolumeVO volume = _volumeDao.findById(id);
         if (volume != null && !volume.getDestroyed() && volume.getRemoved() == null) {
             return volume;
         }
         else {
             return null;
         }
    }


    @Override
    public VolumeVO findAnyVolumeById(long volumeId) {
        return _volumeDao.findById(volumeId);
    }
    
    @Override
    public List<VolumeVO> searchForVolumes(Criteria c) {
        Filter searchFilter = new Filter(VolumeVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        // SearchCriteria sc = _volumeDao.createSearchCriteria();

        Object[] accountIds = (Object[]) c.getCriteria(Criteria.ACCOUNTID);
        Object type = c.getCriteria(Criteria.VTYPE);
        Long vmInstanceId = (Long) c.getCriteria(Criteria.INSTANCEID);
        Object zone = c.getCriteria(Criteria.DATACENTERID);
        Object pod = c.getCriteria(Criteria.PODID);
        Object domainId = c.getCriteria(Criteria.DOMAINID);
        Object id = c.getCriteria(Criteria.ID);
        Object keyword = c.getCriteria(Criteria.KEYWORD);
        Object name = c.getCriteria(Criteria.NAME);

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

        if ((accountIds == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId());
        }

        // now set the SC criteria...
        SearchCriteria sc = sb.create();
        if (keyword != null) {
            SearchCriteria ssc = _volumeDao.createSearchCriteria();
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

        if (accountIds != null) {
            if ((accountIds.length == 1) && (accountIds[0] != null)) {
                sc.setParameters("accountIdEQ", accountIds[0]);
            } else {
                sc.setParameters("accountIdIN", accountIds);
            }
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById((Long)domainId);
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
        sc.setParameters("domRNameLabel", "r-%");
        sc.setParameters("domPNameLabel", "v-%");
        sc.setParameters("domSNameLabel", "s-%");

        // Only return volumes that are not destroyed
        sc.setParameters("destroyed", false);

        return _volumeDao.search(sc, searchFilter);
    }

    @Override
    public boolean volumeIsOnSharedStorage(long volumeId) throws InvalidParameterValueException {
        // Check that the volume is valid
        VolumeVO volume = _volumeDao.findById(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Please specify a valid volume ID.");
        }

        return _storageMgr.volumeOnSharedStoragePool(volume);
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
    public List<IPAddressVO> searchForIPAddresses(Criteria c) {
        Filter searchFilter = new Filter(IPAddressVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());

        Object[] accountIds = (Object[]) c.getCriteria(Criteria.ACCOUNTID);
        Object zone = c.getCriteria(Criteria.DATACENTERID);
        Object address = c.getCriteria(Criteria.IPADDRESS);
        Object domainId = c.getCriteria(Criteria.DOMAINID);
        Object vlan = c.getCriteria(Criteria.VLAN);
        Object isAllocated = c.getCriteria(Criteria.ISALLOCATED);
        Object keyword = c.getCriteria(Criteria.KEYWORD);
        Object forVirtualNetwork  = c.getCriteria(Criteria.FOR_VIRTUAL_NETWORK);

        SearchBuilder<IPAddressVO> sb = _publicIpAddressDao.createSearchBuilder();
        sb.and("accountIdEQ", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("accountIdIN", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("address", sb.entity().getAddress(), SearchCriteria.Op.LIKE);
        sb.and("vlanDbId", sb.entity().getVlanDbId(), SearchCriteria.Op.EQ);

        if ((accountIds == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId());
        }
        
        if (forVirtualNetwork != null) {
        	SearchBuilder<VlanVO> vlanSearch = _vlanDao.createSearchBuilder();
        	vlanSearch.and("vlanType", vlanSearch.entity().getVlanType(), SearchCriteria.Op.EQ);
        	sb.join("vlanSearch", vlanSearch, sb.entity().getVlanDbId(), vlanSearch.entity().getId());
        }

        if ((isAllocated != null) && ((Boolean) isAllocated == true)) {
            sb.and("allocated", sb.entity().getAllocated(), SearchCriteria.Op.NNULL);
        }

        SearchCriteria sc = sb.create();
        if (accountIds != null) {
            if ((accountIds.length == 1) && (accountIds[0] != null)) {
                sc.setParameters("accountIdEQ", accountIds[0]);
            } else {
                sc.setParameters("accountIdIN", accountIds);
            }
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById((Long)domainId);
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

    /*
     * Left in just in case we have to resurrect this code for demo purposes, but for now
     * 
     * @Override public List<UsageVO> searchForUsage(Criteria c) { Filter searchFilter = new Filter(UsageVO.class,
     * c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit()); SearchCriteria sc = new
     * SearchCriteria(UsageVO.class);
     * 
     * Object[] accountIds = (Object[]) c.getCriteria(Criteria.ACCOUNTID); Object startDate =
     * c.getCriteria(Criteria.STARTDATE); Object endDate = c.getCriteria(Criteria.ENDDATE); Object domainId =
     * c.getCriteria(Criteria.DOMAINID);
     * 
     * if (accountIds.length == 1) { if (accountIds[0] != null) { sc.addAnd("accountId", SearchCriteria.Op.EQ,
     * accountIds[0]); } } else { sc.addAnd("accountId", SearchCriteria.Op.IN, accountIds); } if (domainId != null) {
     * sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId); } if (startDate != null && endDate != null) {
     * sc.addAnd("startDate", SearchCriteria.Op.BETWEEN, startDate, endDate); sc.addAnd("startDate",
     * SearchCriteria.Op.BETWEEN, startDate, endDate); } else if (startDate != null) { sc.addAnd("startDate",
     * SearchCriteria.Op.LTEQ, startDate); sc.addAnd("endDate", SearchCriteria.Op.GTEQ, startDate); } else if (endDate
     * != null) { sc.addAnd("startDate", SearchCriteria.Op.LTEQ, endDate); sc.addAnd("endDate", SearchCriteria.Op.GTEQ,
     * endDate); }
     * 
     * List<UsageVO> usageRecords = null; Transaction txn = Transaction.currentTxn(Transaction.USAGE_DB); try {
     * usageRecords = _usageDao.search(sc, searchFilter); } finally { txn.close();
     * 
     * // switch back to VMOPS_DB txn = Transaction.currentTxn(Transaction.VMOPS_DB); txn.close(); }
     * 
     * return usageRecords; }
     */

    @Override
    public List<DiskTemplateVO> listAllActiveDiskTemplates() {
        return _diskTemplateDao.listAllActive();
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
            saveEvent(user.getId(), user.getAccountId(), EventTypes.EVENT_USER_LOGIN, "user has logged in");
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
        saveEvent(userId, userAcct.getAccountId(), EventTypes.EVENT_USER_LOGOUT, "user has logged out");
    }

    @Override
    public String updateTemplatePricing(long userId, Long id, float price) {
        VMTemplateVO template = _templateDao.findById(id);

        if (template == null) {
            return "Template (id=" + id + ") does not exist";
        }

        // update the price for the offering if it exists, else update it.
        PricingVO existingPrice = _pricingDao.findByTypeAndId("VMTemplate", id);
        DecimalFormat decimalFormat = new DecimalFormat("#.###");

        if (existingPrice == null) {
            PricingVO pricing = new PricingVO(null, new Float(decimalFormat.format(price)), "per hour", "VMTemplate", id, new Date());
            _pricingDao.persist(pricing);
        } else {
            updatePricing(existingPrice.getId(), new Float(decimalFormat.format(price)), "per hour", "VMTemplate", id, new Date());
        }

        UserAccount userAcct = _userAccountDao.findById(Long.valueOf(userId));

        saveEvent(userId, userAcct.getAccountId(), EventTypes.EVENT_TEMPLATE_UPDATE, "Set price of template:  " + template.getName() + " to " + price
                + " per hour");
        return null;
    }

    @Override
    public NetworkRuleConfigVO createOrUpdateRule(long userId, long securityGroupId, String address, String port, String privateIpAddress, String privatePort, String protocol,
            String algorithm) throws InvalidParameterValueException, PermissionDeniedException, NetworkRuleConflictException, InternalErrorException {
        NetworkRuleConfigVO rule = null;
        try {
            SecurityGroupVO sg = _securityGroupDao.findById(Long.valueOf(securityGroupId));
            if (sg == null) {
                throw new InvalidParameterValueException("port forwarding service " + securityGroupId + " does not exist");
            }
            if (!NetUtils.isValidPort(port)) {
                throw new InvalidParameterValueException("port is an invalid value");
            }
            if (!NetUtils.isValidPort(privatePort)) {
                throw new InvalidParameterValueException("privatePort is an invalid value");
            }
            if (protocol != null) {
                if (!NetUtils.isValidProto(protocol)) {
                    throw new InvalidParameterValueException("Invalid protocol");
                }
            }
            if (algorithm != null) {
                if (!NetUtils.isValidAlgorithm(algorithm)) {
                    throw new InvalidParameterValueException("Invalid algorithm");
                }
            }
            rule = createNetworkRuleConfig(userId, securityGroupId, port, privatePort, protocol, algorithm);
        } catch (Exception e) {
            if (e instanceof NetworkRuleConflictException) {
                throw (NetworkRuleConflictException) e;
            } else if (e instanceof InvalidParameterValueException) {
                throw (InvalidParameterValueException) e;
            } else if (e instanceof PermissionDeniedException) {
                throw (PermissionDeniedException) e;
            } else if (e instanceof InternalErrorException) {
                throw (InternalErrorException) e;
            } else {
                s_logger.error("Unhandled exception creating or updating network rule", e);
                throw new CloudRuntimeException("Unhandled exception creating network rule", e);
            }
        }
        return rule;
    }

    @Override
    public long createOrUpdateRuleAsync(boolean isForwarding, long userId, long accountId, Long domainId, long securityGroupId, String address, String port,
            String privateIpAddress, String privatePort, String protocol, String algorithm) {

        CreateOrUpdateRuleParam param = new CreateOrUpdateRuleParam(isForwarding, userId, accountId, address, port, privateIpAddress, privatePort, protocol, algorithm, domainId,
                securityGroupId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(accountId);
        job.setCmd("CreateOrUpdateRule");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(CreatePortForwardingServiceRuleCmd.getResultObjectName());
        
        return _asyncMgr.submitAsyncJob(job);
    }

    public void deleteRule(long ruleId, long userId, long accountId) throws InvalidParameterValueException, PermissionDeniedException, InternalErrorException 
    {
    	_networkMgr.deleteRule(ruleId, userId, accountId);
    }

    public long deleteRuleAsync(long id, long userId, long accountId) {
        DeleteRuleParam param = new DeleteRuleParam(id, userId, accountId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(accountId);
        job.setCmd("DeleteRule");
        job.setCmdInfo(gson.toJson(param));
        
        return _asyncMgr.submitAsyncJob(job);
    }

    public List<VMTemplateVO> listAllTemplates() {
        return _templateDao.listAll();
    }

    public List<GuestOSVO> listAllGuestOS() {
        return _guestOSDao.listAll();
    }
    
    public List<GuestOSCategoryVO> listAllGuestOSCategories() {
    	return _guestOSCategoryDao.listAll();
    }
    
    public String getConfigurationValue(String name) {
    	return _configDao.getValue(name);
    }

    public ConsoleProxyInfo getConsoleProxy(long dataCenterId, long userVmId) {
        ConsoleProxyVO proxy = _consoleProxyMgr.assignProxy(dataCenterId, userVmId);
        if (proxy == null)
            return null;
        
        return new ConsoleProxyInfo(proxy.isSslEnabled(), proxy.getPublicIpAddress(), _consoleProxyPort, proxy.getPort());
    }

    public ConsoleProxyVO startConsoleProxy(long instanceId, long startEventId) throws InternalErrorException {
        return _consoleProxyMgr.startProxy(instanceId, startEventId);
    }

    public boolean stopConsoleProxy(long instanceId, long startEventId) {
        return _consoleProxyMgr.stopProxy(instanceId, startEventId);
    }

    public boolean rebootConsoleProxy(long instanceId, long startEventId) {
        return _consoleProxyMgr.rebootProxy(instanceId, startEventId);
    }

    public boolean destroyConsoleProxy(long instanceId, long startEventId) {
        return _consoleProxyMgr.destroyProxy(instanceId, startEventId);
    }

    public long startConsoleProxyAsync(long instanceId) {
        long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_START, "starting console proxy with Id: "+instanceId);
        VMOperationParam param = new VMOperationParam(0, instanceId, null, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
        job.setCmd("StartConsoleProxy");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(StartSystemVMCmd.getResultObjectName());
        return _asyncMgr.submitAsyncJob(job, true);
    }

    public long stopConsoleProxyAsync(long instanceId) {
        long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_STOP, "stopping console proxy with Id: "+instanceId);
        VMOperationParam param = new VMOperationParam(0, instanceId, null, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
        job.setCmd("StopConsoleProxy");
        job.setCmdInfo(gson.toJson(param));
        // use the same result object name as StartConsoleProxyCmd
        job.setCmdOriginator(StartSystemVMCmd.getResultObjectName());
        
        return _asyncMgr.submitAsyncJob(job, true);
    }

    public long rebootConsoleProxyAsync(long instanceId) {
        long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_REBOOT, "rebooting console proxy with Id: "+instanceId);
        VMOperationParam param = new VMOperationParam(0, instanceId, null, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
        job.setCmd("RebootConsoleProxy");
        job.setCmdInfo(gson.toJson(param));
        // use the same result object name as StartConsoleProxyCmd
        job.setCmdOriginator(StartSystemVMCmd.getResultObjectName());
        
        return _asyncMgr.submitAsyncJob(job, true);
    }

    public long destroyConsoleProxyAsync(long instanceId) {
        long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_DESTROY, "destroying console proxy with Id: "+instanceId);
        VMOperationParam param = new VMOperationParam(0, instanceId, null, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
        job.setCmd("DestroyConsoleProxy");
        job.setCmdInfo(gson.toJson(param));
        
        return _asyncMgr.submitAsyncJob(job);
    }

    public String getConsoleAccessUrlRoot(long vmId) {
        VMInstanceVO vm = this.findVMInstanceById(vmId);
        if (vm != null) {
            ConsoleProxyInfo proxy = getConsoleProxy(vm.getDataCenterId(), vmId);
            if (proxy != null)
                return proxy.getProxyImageUrl();
        }
        return null;
    }

    public int getVncPort(VirtualMachine vm) {
        if (vm.getHostId() == null) {
        	s_logger.warn("VM " + vm.getName() + " does not have host, return -1 for its VNC port");
            return -1;
        }
        
        if(s_logger.isTraceEnabled())
        	s_logger.trace("Trying to retrieve VNC port from agent about VM " + vm.getName());
        
        GetVncPortAnswer answer = (GetVncPortAnswer) _agentMgr.easySend(vm.getHostId(), new GetVncPortCommand(vm.getId(), vm.getInstanceName()));
        int port = answer == null ? -1 : answer.getPort();
        
        if(s_logger.isTraceEnabled())
        	s_logger.trace("Retrieved VNC port about VM " + vm.getName() + " is " + port);
        
        return port;
    }

    public ConsoleProxyVO findConsoleProxyById(long instanceId) {
        return _consoleProxyDao.findById(instanceId);
    }

    public List<DomainVO> searchForDomains(Criteria c) {
        Filter searchFilter = new Filter(DomainVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        Long domainId = (Long) c.getCriteria(Criteria.ID);
        String domainName = (String) c.getCriteria(Criteria.NAME);
        Integer level = (Integer) c.getCriteria(Criteria.LEVEL);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        SearchBuilder<DomainVO> sb = _domainDao.createSearchBuilder();
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("level", sb.entity().getLevel(), SearchCriteria.Op.EQ);
        sb.and("path", sb.entity().getPath(), SearchCriteria.Op.LIKE);

        SearchCriteria sc = sb.create();

        if (keyword != null) {
            SearchCriteria ssc = _domainDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (domainName != null) {
            sc.setParameters("name", "%" + domainName + "%");
        }

        if (level != null) {
            sc.setParameters("level", level);
        }

        if ((domainName == null) && (level == null) && (domainId != null)) {
            DomainVO domain = _domainDao.findById(domainId);
            if (domain != null) {
                sc.setParameters("path", domain.getPath() + "%");
            }
        }

        return _domainDao.search(sc, searchFilter);
    }

    public List<DomainVO> searchForDomainChildren(Criteria c) {
        Filter searchFilter = new Filter(DomainVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        Long domainId = (Long) c.getCriteria(Criteria.ID);
        String domainName = (String) c.getCriteria(Criteria.NAME);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        SearchCriteria sc = _domainDao.createSearchCriteria();

        if (keyword != null) {
            SearchCriteria ssc = _domainDao.createSearchCriteria();
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
    public DomainVO createDomain(String name, Long ownerId, Long parentId) {
        SearchCriteria sc = _domainDao.createSearchCriteria();
        sc.addAnd("name", SearchCriteria.Op.EQ, name);
        sc.addAnd("parent", SearchCriteria.Op.EQ, parentId);
        List<DomainVO> domains = _domainDao.search(sc, null);
        if ((domains == null) || domains.isEmpty()) {
            DomainVO domain = new DomainVO(name, ownerId, parentId);
            try {
                DomainVO dbDomain = _domainDao.create(domain);
                saveEvent(new Long(1), ownerId, EventVO.LEVEL_INFO, EventTypes.EVENT_DOMAIN_CREATE, "Domain, " + name + " created with owner id = " + ownerId
                        + " and parentId " + parentId);
                return dbDomain;
            } catch (IllegalArgumentException ex) {
                saveEvent(new Long(1), ownerId, EventVO.LEVEL_ERROR, EventTypes.EVENT_DOMAIN_CREATE, "Domain, " + name + " was not created with owner id = " + ownerId
                        + " and parentId " + parentId);
                throw ex;
            }
        } else {
            saveEvent(new Long(1), ownerId, EventVO.LEVEL_ERROR, EventTypes.EVENT_DOMAIN_CREATE, "Domain, " + name + " was not created with owner id = " + ownerId
                    + " and parentId " + parentId);
        }
        return null;
    }

    @Override
    public long deleteDomainAsync(Long domainId, Long ownerId, Boolean cleanup) {
        DeleteDomainParam param = new DeleteDomainParam(domainId, ownerId, cleanup);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(UserContext.current().getAccountId());
        job.setCmd("DeleteDomain");
        job.setCmdInfo(gson.toJson(param));
        return _asyncMgr.submitAsyncJob(job);
    }

    // FIXME:  need userId so the event can be saved with proper id
    @Override
    public String deleteDomain(Long domainId, Long ownerId, Boolean cleanup) {
        try {
            DomainVO domain = _domainDao.findById(domainId);
            if (domain != null) {
                if ((cleanup != null) && cleanup.booleanValue()) {
                    boolean success = cleanupDomain(domainId, ownerId);
                    if (!success) {
                        saveEvent(new Long(1), ownerId, EventVO.LEVEL_ERROR, EventTypes.EVENT_DOMAIN_DELETE, "Failed to clean up domain resources and sub domains, domain with id " + domainId + " was not deleted.");
                        return "Failed to clean up domain resources and sub domains, delete failed on domain " + domain.getName() + " (id: " + domainId + ").";
                    }
                } else {
                    if (!_domainDao.remove(domainId)) {
                        saveEvent(new Long(1), ownerId, EventVO.LEVEL_ERROR, EventTypes.EVENT_DOMAIN_DELETE, "Domain with id " + domainId + " was not deleted");
                        return "Delete failed on domain " + domain.getName() + " (id: " + domainId + "); please make sure all users and sub domains have been removed from the domain before deleting";
                    } else {
                        saveEvent(new Long(1), ownerId, EventVO.LEVEL_INFO, EventTypes.EVENT_DOMAIN_DELETE, "Domain with id " + domainId + " was deleted");
                    }
                }
            }
            return null;
        } catch (Exception ex) {
            s_logger.error("Exception deleting domain with id " + domainId, ex);
            return "Delete failed on domain with id " + domainId + " due to an internal server error.";
        }
    }

    private boolean cleanupDomain(Long domainId, Long ownerId) {
        boolean success = true;
        {
            SearchCriteria sc = _domainDao.createSearchCriteria();
            sc.addAnd("parent", SearchCriteria.Op.EQ, domainId);
            List<DomainVO> domains = _domainDao.search(sc, null);

            // cleanup sub-domains first
            for (DomainVO domain : domains) {
                success = (success && cleanupDomain(domain.getId(), domain.getOwner()));
            }
        }

        {
            // delete users which will also delete accounts and release resources for those accounts
            SearchCriteria sc = _accountDao.createSearchCriteria();
            sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
            List<AccountVO> accounts = _accountDao.search(sc, null);
            for (AccountVO account : accounts) {
                SearchCriteria userSc = _userDao.createSearchCriteria();
                userSc.addAnd("accountId", SearchCriteria.Op.EQ, account.getId());
                List<UserVO> users = _userDao.search(userSc, null);
                for (UserVO user : users) {
                    success = (success && deleteUser(user.getId()));
                }
            }
        }

        // delete the domain itself
        boolean deleteDomainSuccess = _domainDao.remove(domainId);
        if (!deleteDomainSuccess) {
            saveEvent(new Long(1), ownerId, EventVO.LEVEL_ERROR, EventTypes.EVENT_DOMAIN_DELETE, "Domain with id " + domainId + " was not deleted");
        } else {
            saveEvent(new Long(1), ownerId, EventVO.LEVEL_INFO, EventTypes.EVENT_DOMAIN_DELETE, "Domain with id " + domainId + " was deleted");
        }

        return success && deleteDomainSuccess;
    }

    public void updateDomain(Long domainId, String domainName) {
        SearchCriteria sc = _domainDao.createSearchCriteria();
        sc.addAnd("name", SearchCriteria.Op.EQ, domainName);
        List<DomainVO> domains = _domainDao.search(sc, null);
        if ((domains == null) || domains.isEmpty()) {
            _domainDao.update(domainId, domainName);
            DomainVO domain = _domainDao.findById(domainId);
            saveEvent(new Long(1), domain.getOwner(), EventVO.LEVEL_INFO, EventTypes.EVENT_DOMAIN_UPDATE, "Domain, " + domainName + " was updated");
        } else {
            DomainVO domain = _domainDao.findById(domainId);
            saveEvent(new Long(1), domain.getOwner(), EventVO.LEVEL_ERROR, EventTypes.EVENT_DOMAIN_UPDATE, "Failed to update domain " + domain.getName() + " with name " + domainName + ", name in use.");
        }
    }

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

    public DomainVO findDomainIdById(Long domainId) {
        return _domainDao.findById(domainId);
    }

    @Override
    public DomainVO findDomainByPath(String domainPath) {
        return _domainDao.findDomainByPath(domainPath);
    }

    @Override
    public List<AlertVO> searchForAlerts(Criteria c) {
        Filter searchFilter = new Filter(AlertVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria sc = _alertDao.createSearchCriteria();

        Object type = c.getCriteria(Criteria.TYPE);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        if (keyword != null) {
            SearchCriteria ssc = _alertDao.createSearchCriteria();
            ssc.addOr("subject", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            
            sc.addAnd("subject", SearchCriteria.Op.SC, ssc);
        }

        if (type != null) {
            sc.addAnd("type", SearchCriteria.Op.EQ, type);
        }

        return _alertDao.search(sc, searchFilter);
    }

    @Override
    public List<CapacityVO> listCapacities(Criteria c) {
        // make sure capacity is accurate before displaying it anywhere
        // NOTE: listCapacities is currently called by the UI only, so this
        // shouldn't be called much since it checks all hosts/VMs
        // to figure out what has been allocated.
        _alertMgr.recalculateCapacity();

        Filter searchFilter = new Filter(CapacityVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria sc = _capacityDao.createSearchCriteria();

        Object type = c.getCriteria(Criteria.TYPE);
        Object zoneId = c.getCriteria(Criteria.DATACENTERID);
        Object podId = c.getCriteria(Criteria.PODID);
        Object hostId = c.getCriteria(Criteria.HOSTID);

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
    public long createSnapshotAsync(long userId, long volumeId)
    throws InvalidParameterValueException,
           ResourceAllocationException,
           InternalErrorException
    {
        VolumeVO volume = findVolumeById(volumeId); // not null, precondition.
        if (volume.getStatus() != AsyncInstanceCreateStatus.Created) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " is not in Created state but " + volume.getStatus() + ". Cannot take snapshot.");
        }
        StoragePoolVO storagePoolVO = findPoolById(volume.getPoolId());
        if (storagePoolVO == null) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " does not have a valid storage pool. Is it destroyed?");
        }
        if (storagePoolVO.isLocal()) {
            throw new InvalidParameterValueException("Cannot create a snapshot from a volume residing on a local storage pool, poolId: " + volume.getPoolId());
        }

        Long instanceId = volume.getInstanceId();
        if (instanceId != null) {
            // It is not detached, but attached to a VM
            if (findUserVMInstanceById(instanceId) == null) {
                // It is not a UserVM but a SystemVM or DomR
                throw new InvalidParameterValueException("Snapshots of volumes attached to System or router VM are not allowed");
            }
        }
        
    	Long jobId = _snapshotScheduler.scheduleManualSnapshot(userId, volumeId);
    	if (jobId == null) {
    	    throw new InternalErrorException("Snapshot could not be scheduled because there is another snapshot underway for the same volume. " +
    	    		                         "Please wait for some time.");
    	}
        
    	return jobId;
    }

    @Override
    public long deleteSnapshotAsync(long userId, long snapshotId) {
    	Snapshot snapshot = findSnapshotById(snapshotId);
        long volumeId = snapshot.getVolumeId();
        List<SnapshotPolicyVO> policies = _snapMgr.listPoliciesforSnapshot(snapshotId);
        
        // Return the job id of the last destroySnapshotAsync job which actually destroys the snapshot.
        // The rest of the async jobs just update the db and don't really do any meaningful thing.
        long finalJobId = 0;
        for (SnapshotPolicyVO policy : policies) {
            finalJobId = _snapMgr.destroySnapshotAsync(userId, volumeId, snapshotId, policy.getId());
        }
        return finalJobId;
    }


    @Override
    public List<SnapshotVO> listSnapshots(Criteria c, String interval) throws InvalidParameterValueException {
        Filter searchFilter = new Filter(SnapshotVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria sc = _snapshotDao.createSearchCriteria();

        Object volumeId = c.getCriteria(Criteria.VOLUMEID);
        Object name = c.getCriteria(Criteria.NAME);
        Object id = c.getCriteria(Criteria.ID);
        Object keyword = c.getCriteria(Criteria.KEYWORD);
        Object accountId = c.getCriteria(Criteria.ACCOUNTID);
        Object snapshotTypeStr = c.getCriteria(Criteria.TYPE);
        
        sc.addAnd("status", SearchCriteria.Op.EQ, Snapshot.Status.BackedUp);
        
        if(volumeId != null){
            sc.addAnd("volumeId", SearchCriteria.Op.EQ, volumeId);
        }
        
        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (keyword != null) {
            SearchCriteria ssc = _snapshotDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }
        
        if (accountId != null) {
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        }

        if (snapshotTypeStr != null) {
            SnapshotType snapshotType = SnapshotVO.getSnapshotType((String)snapshotTypeStr);
            if (snapshotType == null) {
                throw new InvalidParameterValueException("Unsupported snapshot type " + snapshotTypeStr);
            }
            sc.addAnd("snapshotType", SearchCriteria.Op.EQ, snapshotType.ordinal());
        }
        else {
            // Show only MANUAL and RECURRING snapshot types
            sc.addAnd("snapshotType", SearchCriteria.Op.NEQ, Snapshot.SnapshotType.TEMPLATE.ordinal());
        }
        if(interval != null && volumeId != null) {
            IntervalType intervalType =  DateUtil.IntervalType.getIntervalType(interval);
            if(intervalType == null) {
                throw new InvalidParameterValueException("Unsupported interval type " + intervalType);
            }
            SnapshotPolicyVO snapPolicy = _snapMgr.getPolicyForVolumeByInterval((Long)volumeId, (short)intervalType.ordinal());
            if (snapPolicy == null) {
                s_logger.warn("Policy with interval "+ intervalType +" not assigned to volume: "+volumeId);
                return new ArrayList<SnapshotVO>();
            }
            return _snapMgr.listSnapsforPolicy(snapPolicy.getId(), searchFilter);
        }
        
        return _snapshotDao.search(sc, searchFilter);
    }

    @Override
    public Snapshot findSnapshotById(long snapshotId) {
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
        if (snapshot != null && snapshot.getRemoved() == null && snapshot.getStatus() == Snapshot.Status.BackedUp) {
            return snapshot;
        }
        else {
            return null;
        }
    }

    @Override
    public VMTemplateVO createPrivateTemplate(VMTemplateVO template, Long userId, long snapshotId, String name, String description) throws InvalidParameterValueException {

        return _vmMgr.createPrivateTemplate(template, userId, snapshotId, name, description);
    }

    @Override
    public long createPrivateTemplateAsync(Long userId, long volumeId, String name, String description, long guestOSId, Boolean requiresHvm, Integer bits, Boolean passwordEnabled, boolean isPublic, boolean featured, Long snapshotId)
            throws InvalidParameterValueException, ResourceAllocationException, InternalErrorException {
        if (name.length() > 32)
        {
            throw new InvalidParameterValueException("Template name should be less than 32 characters");
        }
        		
        if(!name.matches("^[\\p{Alnum} ._-]+"))
        {
            throw new InvalidParameterValueException("Only alphanumeric, space, dot, dashes and underscore characters allowed");
        }

        // The volume below could be destroyed or removed.
        VolumeVO volume = _volumeDao.findById(volumeId);
            	
        // If private template is created from Volume, check that the volume will not be active when the private template is created
        if (snapshotId == null && !_storageMgr.volumeInactive(volume)) {
            String msg = "Unable to create private template for volume: " + volume.getName() + "; volume is attached to a non-stopped VM.";

            if (s_logger.isInfoEnabled()) {
                s_logger.info(msg);
            }

            throw new InternalErrorException(msg);
        }

        // Check that the guest OS is valid
        GuestOSVO guestOS = _guestOSDao.findById(guestOSId);
        if (guestOS == null) {
            throw new InvalidParameterValueException("Please specify a valid guest OS.");
        }

        CreatePrivateTemplateParam param = new CreatePrivateTemplateParam(userId, volumeId, guestOSId, name, description, requiresHvm, bits, passwordEnabled, isPublic, featured, snapshotId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(volume.getAccountId());
        job.setCmd("CreatePrivateTemplate");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(CreateTemplateCmd.getResultObjectName());
        
        return _asyncMgr.submitAsyncJob(job, true);
    }

    @Override
    public DiskOfferingVO findDiskOfferingById(long diskOfferingId) {
        return _diskOfferingDao.findById(diskOfferingId);
    }

    @Override
    @DB
    public boolean updateTemplatePermissions(long templateId, String operation, Boolean isPublic, Boolean isFeatured, List<String> accountNames) throws InvalidParameterValueException,
            PermissionDeniedException, InternalErrorException {
        Transaction txn = Transaction.currentTxn();
        VMTemplateVO template = _templateDao.findById(templateId);
        if (template == null) {
            throw new InvalidParameterValueException("Unable to find template with id " + templateId);
        }

        Long accountId = template.getAccountId();
        if (accountId == null) {
            // if there is no owner of the template then it's probably already a public template (or domain private template) so publishing to individual users is irrelevant
            throw new InvalidParameterValueException("Update template permissions is an invalid operation on template " + template.getName());
        }

        Account account = _accountDao.findById(accountId);
        if (account == null) {
            throw new PermissionDeniedException("Unable to verify owner of template " + template.getName());
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
                    if (permittedAccount.getId().longValue() == account.getId().longValue()) {
                        continue; // don't grant permission to the template owner, they implicitly have permission
                    }
                    LaunchPermissionVO existingPermission = _launchPermissionDao.findByTemplateAndAccount(templateId, permittedAccount.getId().longValue());
                    if (existingPermission == null) {
                        LaunchPermissionVO launchPermission = new LaunchPermissionVO(templateId, permittedAccount.getId().longValue());
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
            try {
                List<Long> accountIds = new ArrayList<Long>();
                for (String accountName : accountNames) {
                    Account permittedAccount = _accountDao.findActiveAccount(accountName, domainId);
                    if (permittedAccount != null) {
                        accountIds.add(permittedAccount.getId());
                    }
                }
                _launchPermissionDao.removePermissions(templateId, accountIds);
            } catch (CloudRuntimeException ex) {
                throw new InternalErrorException("Internal error removing launch permissions for template " + template.getName());
            }
        } else if ("reset".equalsIgnoreCase(operation)) {
            // do we care whether the owning account is an admin? if the
            // owner is an admin, will we still set public to false?
            updatedTemplate = _templateDao.createForUpdate();
            updatedTemplate.setPublicTemplate(false);
            updatedTemplate.setFeatured(false);
            _templateDao.update(template.getId(), updatedTemplate);
            _launchPermissionDao.removeAllPermissions(templateId);
        }
        return true;
    }

    @Override
    public List<String> listTemplatePermissions(long templateId) {
        List<String> accountNames = new ArrayList<String>();
        
        List<LaunchPermissionVO> permissions = _launchPermissionDao.findByTemplate(templateId);
        if ((permissions != null) && !permissions.isEmpty()) {
            for (LaunchPermissionVO permission : permissions) {
                Account acct = _accountDao.findById(permission.getAccountId());
                accountNames.add(acct.getAccountName());
            }
        }
        return accountNames;
    }

    @Override
    public List<DiskOfferingVO> searchForDiskOfferings(Criteria c) {
        Filter searchFilter = new Filter(DiskOfferingVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchBuilder<DiskOfferingVO> sb = _diskOfferingDao.createSearchBuilder();

        // SearchBuilder and SearchCriteria are now flexible so that the search builder can be built with all possible
        // search terms and only those with criteria can be set.  The proper SQL should be generated as a result.
        Object name = c.getCriteria(Criteria.NAME);
        //Object domainId = c.getCriteria(Criteria.DOMAINID);
        Object id = c.getCriteria(Criteria.ID);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

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

        SearchCriteria sc = sb.create();
        if (keyword != null) {
            SearchCriteria ssc = _diskOfferingDao.createSearchCriteria();
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

    @Override
    public DiskOfferingVO createDiskOffering(long domainId, String name, String description, int numGibibytes, boolean mirrored, String tags) throws InvalidParameterValueException {
        if (numGibibytes < 1) {
            throw new InvalidParameterValueException("Please specify a disk size of at least 1 Gb.");
        } else if (numGibibytes > _maxVolumeSizeInGb) {
        	throw new InvalidParameterValueException("The maximum size for a disk is " + _maxVolumeSizeInGb + " Gb.");
        }

        return _configMgr.createDiskOffering(domainId, name, description, numGibibytes, mirrored, tags);
    }

    @Override
    public DiskOfferingVO updateDiskOffering(long userId, long diskOfferingId, String name, String description, String tags) {
    	return _configMgr.updateDiskOffering(userId, diskOfferingId, name, description, tags);
    }

    @Override
    public boolean deleteDiskOffering(long id) {
        return _diskOfferingDao.remove(Long.valueOf(id));
    }

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
        	
        	Account account = _accountDao.findById(UserContext.current().getAccountId());
        	if(account == null || account.getType() != Account.ACCOUNT_TYPE_ADMIN) {
	            if (s_logger.isDebugEnabled()) {
	            	if(account == null)
		                s_logger.debug("queryAsyncJobResult error: Permission denied, account no long exist for account id in context, job id: " + jobId
		                	+ ", accountId  " + UserContext.current().getAccountId());
	            	else
	            		s_logger.debug("queryAsyncJobResult error: Permission denied, invalid ownership for job " + jobId + ", job account owner: "
	            			+ job.getAccountId() + ", account id in context: " + UserContext.current().getAccountId());
	            }
	
	            throw new PermissionDeniedException("Permission denied, invalid job ownership, job id: " + jobId);
        	}
        }
        return _asyncMgr.queryAsyncJobResult(jobId);
    }

    @Override
    public AsyncJobVO findInstancePendingAsyncJob(String instanceType, long instanceId) {
        return _asyncMgr.findInstancePendingAsyncJob(instanceType, instanceId);
    }

    @Override
    public AsyncJobVO findAsyncJobById(long jobId) {
        return _asyncMgr.getAsyncJob(jobId);
    }

    @Override
    public SecurityGroupVO createSecurityGroup(String name, String description, Long domainId, Long accountId) {
        SecurityGroupVO group = new SecurityGroupVO(name, description, domainId, accountId);
        return _securityGroupDao.persist(group);
    }

    @Override
    public long deleteSecurityGroupAsync(long userId, Long accountId, long securityGroupId) {
        long eventId = saveScheduledEvent(userId, accountId, EventTypes.EVENT_PORT_FORWARDING_SERVICE_DELETE, "deleting security group with Id: " + securityGroupId);
        SecurityGroupParam param = new SecurityGroupParam(userId, securityGroupId, null, null, null, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(accountId);
        job.setCmd("DeleteSecurityGroup");
        job.setCmdInfo(gson.toJson(param));
        return _asyncMgr.submitAsyncJob(job);
    }

    @Override
    public boolean deleteSecurityGroup(long userId, long securityGroupId, long startEventId) throws InvalidParameterValueException, PermissionDeniedException {
        SecurityGroupVO securityGroup = _securityGroupDao.findById(Long.valueOf(securityGroupId));
        if (securityGroup == null) {
            return true; // already deleted, return true
        }

        final EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(securityGroup.getAccountId());
        event.setType(EventTypes.EVENT_PORT_FORWARDING_SERVICE_DELETE);
        event.setStartId(startEventId);
        try {
            List<SecurityGroupVMMapVO> sgVmMappings = _securityGroupVMMapDao.listBySecurityGroup(securityGroupId);
            if (sgVmMappings != null) {
                for (SecurityGroupVMMapVO sgVmMapping : sgVmMappings) {
                    removeSecurityGroup(userId, sgVmMapping.getSecurityGroupId(), sgVmMapping.getIpAddress(), sgVmMapping.getInstanceId(), startEventId);
                }
            }

            _networkRuleConfigDao.deleteBySecurityGroup(securityGroupId);

        } catch (InvalidParameterValueException ex1) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Invalid parameter value exception deleting port forwarding service " + securityGroup.getName() + " (id: " + securityGroup.getId() + "), " + ex1);
            }
            event.setLevel(EventVO.LEVEL_ERROR);
            event.setDescription("Failed to delete port forwarding service - " + securityGroup.getName() + " (id: " + securityGroup.getId() + ")");
            _eventDao.persist(event);
            throw ex1;
        } catch (PermissionDeniedException ex2) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Invalid parameter value exception deleting port forwarding service " + securityGroup.getName() + " (id: " + securityGroup.getId() + "), " + ex2);
            }
            event.setLevel(EventVO.LEVEL_ERROR);
            event.setDescription("ailed to delete port forwarding service - " + securityGroup.getName() + " (id: " + securityGroup.getId() + ")");
            _eventDao.persist(event);
            throw ex2;
        }

        boolean success = _securityGroupDao.remove(Long.valueOf(securityGroupId));

        event.setLevel(EventVO.LEVEL_INFO);
        event.setDescription("Deleting port forwarding service - " + securityGroup.getName() + " (id: " + securityGroup.getId() + ")");
        _eventDao.persist(event);

        return success;
    }

    @Override
    public List<SecurityGroupVO> listSecurityGroups(Long accountId, Long domainId) {
        if (accountId != null) {
            Account acct = _accountDao.findById(accountId);
            domainId = acct.getDomainId();
        }
        return _securityGroupDao.listAvailableGroups(accountId, domainId);
    }

    @Override
    public List<SecurityGroupVO> searchForSecurityGroups(Criteria c) {
        Filter searchFilter = new Filter(SecurityGroupVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());

        Object domainId = c.getCriteria(Criteria.DOMAINID);
        Object accountId = c.getCriteria(Criteria.ACCOUNTID);
        Object name = c.getCriteria(Criteria.NAME);
        Object id = c.getCriteria(Criteria.ID);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        SearchBuilder<SecurityGroupVO> sb = _securityGroupDao.createSearchBuilder();
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);

        if ((accountId == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId());
        }

        SearchCriteria sc = sb.create();
        if (keyword != null) {
            SearchCriteria ssc = _securityGroupDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, name + "%");
        }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (accountId != null) {
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById((Long)domainId);
            sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
        }

        return _securityGroupDao.search(sc, searchFilter);
    }

    @Override
    public Map<String, List<SecurityGroupVO>> searchForSecurityGroupsByVM(Criteria c) {
        Filter searchFilter = new Filter(SecurityGroupVMMapVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria sc = _securityGroupVMMapDao.createSearchCriteria();

        Object instanceId = c.getCriteria(Criteria.INSTANCEID);
        Object ipAddress = c.getCriteria(Criteria.ADDRESS);
        // TODO: keyword search on vm name?  vm group?  what makes sense here?  We can't search directly on 'name' as that's not a field of SecurityGroupVMMapVO.
        //Object keyword = c.getCriteria(Criteria.KEYWORD);

        /*
        if (keyword != null) {
            SearchCriteria ssc = _securityGroupVMMapDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }
        */

        if (instanceId != null) {
            sc.addAnd("instanceId", SearchCriteria.Op.EQ, instanceId);
        }

        if (ipAddress != null) {
            sc.addAnd("ipAddress", SearchCriteria.Op.EQ, ipAddress);
        }

        Map<String, List<SecurityGroupVO>> securityGroups = new HashMap<String, List<SecurityGroupVO>>();
        List<SecurityGroupVMMapVO> sgVmMappings = _securityGroupVMMapDao.search(sc, searchFilter);
        if (sgVmMappings != null) {
            for (SecurityGroupVMMapVO sgVmMapping : sgVmMappings) {
                SecurityGroupVO sg = _securityGroupDao.findById(sgVmMapping.getSecurityGroupId());
                List<SecurityGroupVO> sgList = securityGroups.get(sgVmMapping.getIpAddress());
                if (sgList == null) {
                    sgList = new ArrayList<SecurityGroupVO>();
                }
                sgList.add(sg);
                securityGroups.put(sgVmMapping.getIpAddress(), sgList);
            }
        }
        return securityGroups;
    }

    @Override
    public boolean isSecurityGroupNameInUse(Long domainId, Long accountId, String name) {
        if (domainId == null) {
            domainId = DomainVO.ROOT_DOMAIN;
        }

        return _securityGroupDao.isNameInUse(accountId, domainId, name);
    }

    @Override
    public SecurityGroupVO findSecurityGroupById(Long groupId) {
        return _securityGroupDao.findById(groupId);
    }

    @Override
    public LoadBalancerVO findLoadBalancer(Long accountId, String name) {
        SearchCriteria sc = _loadBalancerDao.createSearchCriteria();
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
    @DB
    public LoadBalancerVO createLoadBalancer(Long userId, Long accountId, String name, String description, String ipAddress, String publicPort, String privatePort, String algorithm)
            throws InvalidParameterValueException, PermissionDeniedException {
        if (accountId == null) {
            throw new InvalidParameterValueException("accountId not specified");
        }
        if (!NetUtils.isValidIp(ipAddress)) {
            throw new InvalidParameterValueException("invalid ip address");
        }
        if (!NetUtils.isValidPort(publicPort)) {
            throw new InvalidParameterValueException("publicPort is an invalid value");
        }
        if (!NetUtils.isValidPort(privatePort)) {
            throw new InvalidParameterValueException("privatePort is an invalid value");
        }
        if ((algorithm == null) || !NetUtils.isValidAlgorithm(algorithm)) {
            throw new InvalidParameterValueException("Invalid algorithm");
        }

        boolean locked = false;
        try {
            LoadBalancerVO exitingLB = _loadBalancerDao.findByIpAddressAndPublicPort(ipAddress, publicPort);
            if (exitingLB != null) {
                throw new InvalidParameterValueException("IP Address/public port already load balanced by an existing load balancer rule");
            }

            List<FirewallRuleVO> existingFwRules = _firewallRulesDao.listIPForwarding(ipAddress, publicPort, true);
            if ((existingFwRules != null) && !existingFwRules.isEmpty()) {
                FirewallRuleVO existingFwRule = existingFwRules.get(0);
                String securityGroupName = null;
                if (existingFwRule.getGroupId() != null) {
                    long groupId = existingFwRule.getGroupId();
                    SecurityGroupVO securityGroup = _securityGroupDao.findById(groupId);
                    securityGroupName = securityGroup.getName();
                }
                throw new InvalidParameterValueException("IP Address (" + ipAddress + ") and port (" + publicPort + ") already in use" +
                        ((securityGroupName == null) ? "" : " by port forwarding service " + securityGroupName));
            }

            IPAddressVO addr = _publicIpAddressDao.acquire(ipAddress);

            if (addr == null) {
                throw new PermissionDeniedException("User does not own ip address " + ipAddress);
            }

            locked = true;
            if ((addr.getAllocated() == null) || !accountId.equals(addr.getAccountId())) {
                throw new PermissionDeniedException("User does not own ip address " + ipAddress);
            }

            LoadBalancerVO loadBalancer = new LoadBalancerVO(name, description, accountId.longValue(), ipAddress, publicPort, privatePort, algorithm);
            loadBalancer = _loadBalancerDao.persist(loadBalancer);
            Long id = loadBalancer.getId();

            // Save off information for the event that the security group was applied
            EventVO event = new EventVO();
            event.setUserId(userId);
            event.setAccountId(accountId);
            event.setType(EventTypes.EVENT_LOAD_BALANCER_CREATE);

            if (id == null) {
                event.setDescription("Failed to create load balancer " + loadBalancer.getName() + " on ip address " + ipAddress + "[" + publicPort + "->" + privatePort + "]");
                event.setLevel(EventVO.LEVEL_ERROR);
            } else {
                event.setDescription("Successfully created load balancer " + loadBalancer.getName() + " on ip address " + ipAddress + "[" + publicPort + "->" + privatePort + "]");
                String params = "id="+loadBalancer.getId()+"\ndcId="+addr.getDataCenterId();
                event.setParameters(params);
                event.setLevel(EventVO.LEVEL_INFO);
            }
            _eventDao.persist(event);

            return _loadBalancerDao.findById(id);
        } finally {
            if (locked) {
                _publicIpAddressDao.release(ipAddress);
            }
        }
    }

    @Override @DB
    public void assignToLoadBalancer(long userId, long loadBalancerId, List<Long> instanceIds) throws NetworkRuleConflictException, InternalErrorException,
            PermissionDeniedException, InvalidParameterValueException {
        Transaction txn = Transaction.currentTxn();
        try {
            List<FirewallRuleVO> firewallRulesToApply = new ArrayList<FirewallRuleVO>();
            long accountId = 0;
            DomainRouterVO router = null;

            LoadBalancerVO loadBalancer = _loadBalancerDao.findById(Long.valueOf(loadBalancerId));
            if (loadBalancer == null) {
                s_logger.warn("Unable to find load balancer with id " + loadBalancerId);
                return;
            }

            List<LoadBalancerVMMapVO> mappedInstances = _loadBalancerVMMapDao.listByLoadBalancerId(loadBalancerId, false);
            Set<Long> mappedInstanceIds = new HashSet<Long>();
            if (mappedInstances != null) {
                for (LoadBalancerVMMapVO mappedInstance : mappedInstances) {
                    mappedInstanceIds.add(Long.valueOf(mappedInstance.getInstanceId()));
                }
            }

            for (Long instanceId : instanceIds) {
                if (mappedInstanceIds.contains(instanceId)) {
                    continue;
                }

                UserVmVO userVm = _userVmDao.findById(instanceId);
                if (userVm == null) {
                    s_logger.warn("Unable to find virtual machine with id " + instanceId);
                    throw new InvalidParameterValueException("Unable to find virtual machine with id " + instanceId);
                } else {
                    // sanity check that the vm can be applied to the load balancer
                    ServiceOfferingVO offering = _offeringsDao.findById(userVm.getServiceOfferingId());
                    if ((offering == null) || !GuestIpType.Virtualized.equals(offering.getGuestIpType())) {
                        // we previously added these instanceIds to the loadBalancerVMMap, so remove them here as we are rejecting the API request
                        // without actually modifying the load balancer
                        _loadBalancerVMMapDao.remove(loadBalancerId, instanceIds, Boolean.TRUE);

                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Unable to add virtual machine " + userVm.toString() + " to load balancer " + loadBalancerId + ", bad network type (" + ((offering == null) ? "null" : offering.getGuestIpType()) + ")");
                        }

                        throw new InvalidParameterValueException("Unable to add virtual machine " + userVm.toString() + " to load balancer " + loadBalancerId + ", bad network type (" + ((offering == null) ? "null" : offering.getGuestIpType()) + ")");
                    }
                }

                if (accountId == 0) {
                    accountId = userVm.getAccountId();
                } else if (accountId != userVm.getAccountId()) {
                    s_logger.warn("guest vm " + userVm.getName() + " (id:" + userVm.getId() + ") belongs to account " + userVm.getAccountId()
                            + ", previous vm in list belongs to account " + accountId);
                    throw new InvalidParameterValueException("guest vm " + userVm.getName() + " (id:" + userVm.getId() + ") belongs to account " + userVm.getAccountId()
                            + ", previous vm in list belongs to account " + accountId);
                }
                
                DomainRouterVO nextRouter = null;
                if (userVm.getDomainRouterId() != null)
                	nextRouter = _routerDao.findById(userVm.getDomainRouterId());
                if (nextRouter == null) {
                    s_logger.warn("Unable to find router (" + userVm.getDomainRouterId() + ") for virtual machine with id " + instanceId);
                    throw new InvalidParameterValueException("Unable to find router (" + userVm.getDomainRouterId() + ") for virtual machine with id " + instanceId);
                }

                if (router == null) {
                    router = nextRouter;

                    // Make sure owner of router is owner of load balancer.  Since we are already checking that all VMs belong to the same router, by checking router
                    // ownership once we'll make sure all VMs belong to the owner of the load balancer.
                    if (router.getAccountId() != loadBalancer.getAccountId()) {
                        throw new InvalidParameterValueException("guest vm " + userVm.getName() + " (id:" + userVm.getId() + ") does not belong to the owner of load balancer " +
                                loadBalancer.getName() + " (owner is account id " + loadBalancer.getAccountId() + ")");
                    }
                } else if (router.getId().longValue() != nextRouter.getId().longValue()) {
                    throw new InvalidParameterValueException("guest vm " + userVm.getName() + " (id:" + userVm.getId() + ") belongs to router " + nextRouter.getName()
                            + ", previous vm in list belongs to router " + router.getName());
                }

                // check for ip address/port conflicts by checking exising forwarding and loadbalancing rules
                String ipAddress = loadBalancer.getIpAddress();
                String privateIpAddress = userVm.getGuestIpAddress();
                List<FirewallRuleVO> existingRulesOnPubIp = _firewallRulesDao.listIPForwarding(ipAddress);

                if (existingRulesOnPubIp != null) {
                    for (FirewallRuleVO fwRule : existingRulesOnPubIp) {
                        if (!(  (fwRule.isForwarding() == false) &&
                                (fwRule.getGroupId() != null) &&
                                (fwRule.getGroupId() == loadBalancer.getId().longValue())  )) {
                            // if the rule is not for the current load balancer, check to see if the private IP is our target IP,
                            // in which case we have a conflict
                            if (fwRule.getPublicPort().equals(loadBalancer.getPublicPort())) {
                                throw new NetworkRuleConflictException("An existing port forwarding service rule for " + ipAddress + ":" + loadBalancer.getPublicPort()
                                        + " exists, found while trying to apply load balancer " + loadBalancer.getName() + " (id:" + loadBalancer.getId() + ") to instance "
                                        + userVm.getName() + ".");
                            }
                        } else if (fwRule.getPrivateIpAddress().equals(privateIpAddress) && fwRule.getPrivatePort().equals(loadBalancer.getPrivatePort()) && fwRule.isEnabled()) {
                            // for the current load balancer, don't add the same instance to the load balancer more than once
                            continue;
                        }
                    }
                }

                FirewallRuleVO newFwRule = new FirewallRuleVO();
                newFwRule.setAlgorithm(loadBalancer.getAlgorithm());
                newFwRule.setEnabled(true);
                newFwRule.setForwarding(false);
                newFwRule.setPrivatePort(loadBalancer.getPrivatePort());
                newFwRule.setPublicPort(loadBalancer.getPublicPort());
                newFwRule.setPublicIpAddress(loadBalancer.getIpAddress());
                newFwRule.setPrivateIpAddress(userVm.getGuestIpAddress());
                newFwRule.setGroupId(loadBalancer.getId());

                firewallRulesToApply.add(newFwRule);
            }

            // if there's no work to do, bail out early rather than reconfiguring the proxy with the existing rules
            if (firewallRulesToApply.isEmpty()) {
                return;
            }

            IPAddressVO ipAddr = _publicIpAddressDao.findById(loadBalancer.getIpAddress());
            List<IPAddressVO> ipAddrs = _networkMgr.listPublicIpAddressesInVirtualNetwork(accountId, ipAddr.getDataCenterId(), null);
            for (IPAddressVO ipv : ipAddrs) {
                List<FirewallRuleVO> rules = _firewallRulesDao.listIPForwarding(ipv.getAddress(), false);
                firewallRulesToApply.addAll(rules);
            }

            txn.start();

            List<FirewallRuleVO> updatedRules = null;
            if (router.getState().equals(State.Starting)) {
                // Starting is a special case...if the router is starting that means the IP address hasn't yet been assigned to the domR and the update firewall rules script will fail.
                // In this case, just store the rules and they will be applied when the router state is resent (after the router is started).
                updatedRules = firewallRulesToApply;
            } else {
                updatedRules = _networkMgr.updateFirewallRules(loadBalancer.getIpAddress(), firewallRulesToApply, router);
            }

            // Save and create the event
            String description;
            String type = EventTypes.EVENT_NET_RULE_ADD;
            String ruleName = "load balancer";
            String level = EventVO.LEVEL_INFO;
            Account account = _accountDao.findById(accountId);

            LoadBalancerVO loadBalancerLock = null;
            try {
                loadBalancerLock = _loadBalancerDao.acquire(loadBalancerId);
                if (loadBalancerLock == null) {
                    s_logger.warn("assignToLoadBalancer: Failed to lock load balancer " + loadBalancerId + ", proceeding with updating loadBalancerVMMappings...");
                }
                if ((updatedRules != null) && (updatedRules.size() == firewallRulesToApply.size())) {
                    // flag the instances as mapped to the load balancer
                    List<LoadBalancerVMMapVO> pendingMappedVMs = _loadBalancerVMMapDao.listByLoadBalancerId(loadBalancerId, true);
                    for (LoadBalancerVMMapVO pendingMappedVM : pendingMappedVMs) {
                        if (instanceIds.contains(pendingMappedVM.getInstanceId())) {
                            LoadBalancerVMMapVO pendingMappedVMForUpdate = _loadBalancerVMMapDao.createForUpdate();
                            pendingMappedVMForUpdate.setPending(false);
                            _loadBalancerVMMapDao.update(pendingMappedVM.getId(), pendingMappedVMForUpdate);
                        }
                    }

                    for (FirewallRuleVO updatedRule : updatedRules) {
                        if (updatedRule.getId() == null) {
                            _firewallRulesDao.persist(updatedRule);

                            description = "created new " + ruleName + " rule [" + updatedRule.getPublicIpAddress() + ":"
                                    + updatedRule.getPublicPort() + "]->[" + updatedRule.getPrivateIpAddress() + ":"
                                    + updatedRule.getPrivatePort() + "]" + " " + updatedRule.getProtocol();

                            saveEvent(userId, account.getId(), level, type, description);
                        }
                    }
                } else {
                    // Remove the instanceIds from the load balancer since there was a failure.  Make sure to commit the
                    // transaction here, otherwise the act of throwing the internal error exception will cause this
                    // remove operation to be rolled back.
                    _loadBalancerVMMapDao.remove(loadBalancerId, instanceIds, null);
                    txn.commit();

                    s_logger.warn("Failed to apply load balancer " + loadBalancer.getName() + " (id:" + loadBalancerId + ") to guest virtual machines " + StringUtils.join(instanceIds, ","));
                    throw new InternalErrorException("Failed to apply load balancer " + loadBalancer.getName() + " (id:" + loadBalancerId + ") to guest virtual machine " + StringUtils.join(instanceIds, ","));
                }
            } finally {
                if (loadBalancerLock != null) {
                    _loadBalancerDao.release(loadBalancerId);
                }
            }

            txn.commit();
        } catch (Throwable e) {
            txn.rollback();
            if (e instanceof NetworkRuleConflictException) {
                throw (NetworkRuleConflictException) e;
            } else if (e instanceof InvalidParameterValueException) {
                throw (InvalidParameterValueException) e;
            } else if (e instanceof PermissionDeniedException) {
                throw (PermissionDeniedException) e;
            } else if (e instanceof InternalErrorException) {
                s_logger.warn("ManagementServer error", e);
                throw (InternalErrorException) e;
            }
            s_logger.warn("ManagementServer error", e);
	    }
    }

    @Override @DB
    public long assignToLoadBalancerAsync(long userId, long loadBalancerId, List<Long> instanceIds) {
        LoadBalancerVO loadBalancer = null;
        try {
            loadBalancer = _loadBalancerDao.acquire(loadBalancerId);

            // if unable to lock the load balancer, throw an exception
            if (loadBalancer == null) {
                throw new CloudRuntimeException("Failed to assign instances to load balancer, unable to lock load balancer " + loadBalancerId);
            }

            IPAddressVO ipAddress = _publicIpAddressDao.findById(loadBalancer.getIpAddress());
            DomainRouterVO router = _routerDao.findBy(loadBalancer.getAccountId(), ipAddress.getDataCenterId());

            List<LoadBalancerVMMapVO> mappedVMs = _loadBalancerVMMapDao.listByLoadBalancerId(loadBalancerId);
            for (LoadBalancerVMMapVO mappedVM : mappedVMs) {
                if (instanceIds.contains(mappedVM.getInstanceId())) {
                    instanceIds.remove(mappedVM.getInstanceId());
                }
            }

            if (instanceIds.isEmpty()) {
                // nothing to do, return 0 since no job is being submitted
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("assignToLoadBalancerAsync:  all given instance ids are already mapped, no work to do...skipping async job");
                }
                return 0L;
            }
            for (Long instanceId : instanceIds) {
                LoadBalancerVMMapVO loadBalancerMapping = new LoadBalancerVMMapVO(loadBalancerId, instanceId.longValue(), true);
                _loadBalancerVMMapDao.persist(loadBalancerMapping);
            }

            LoadBalancerParam param = new LoadBalancerParam(userId, router.getId(), loadBalancerId, instanceIds);
            Gson gson = GsonHelper.getBuilder().create();

            AsyncJobVO job = new AsyncJobVO();
            job.setUserId(UserContext.current().getUserId());
            job.setAccountId(loadBalancer.getAccountId());
            job.setCmd("AssignToLoadBalancer");
            job.setCmdInfo(gson.toJson(param));
            return _asyncMgr.submitAsyncJob(job, true);
        } finally {
            if (loadBalancer != null) {
                _loadBalancerDao.release(loadBalancer.getId());
            }
        }
    }

    @Override @DB
    public boolean removeFromLoadBalancer(long userId, long loadBalancerId, List<Long> instanceIds) throws InvalidParameterValueException {
        Transaction txn = Transaction.currentTxn();
        LoadBalancerVO loadBalancerLock = null;
        boolean success = true;
        try {
            LoadBalancerVO loadBalancer = _loadBalancerDao.findById(loadBalancerId);
            if (loadBalancer == null) {
                return false;
            }

            IPAddressVO ipAddress = _publicIpAddressDao.findById(loadBalancer.getIpAddress());
            if (ipAddress == null) {
                return false;
            }

            DomainRouterVO router = _routerDao.findBy(ipAddress.getAccountId(), ipAddress.getDataCenterId());
            if (router == null) {
                return false;
            }

            txn.start();
            for (Long instanceId : instanceIds) {
                UserVm userVm = _userVmDao.findById(instanceId);
                if (userVm == null) {
                    s_logger.warn("Unable to find virtual machine with id " + instanceId);
                    throw new InvalidParameterValueException("Unable to find virtual machine with id " + instanceId);
                }
                FirewallRuleVO fwRule = _firewallRulesDao.findByGroupAndPrivateIp(loadBalancerId, userVm.getGuestIpAddress(), false);
                if (fwRule != null) {
                    fwRule.setEnabled(false);
                    _firewallRulesDao.update(fwRule.getId(), fwRule);
                }
            }

            List<FirewallRuleVO> allLbRules = new ArrayList<FirewallRuleVO>();
            IPAddressVO ipAddr = _publicIpAddressDao.findById(loadBalancer.getIpAddress());
            List<IPAddressVO> ipAddrs = _networkMgr.listPublicIpAddressesInVirtualNetwork(loadBalancer.getAccountId(), ipAddr.getDataCenterId(), null);
            for (IPAddressVO ipv : ipAddrs) {
                List<FirewallRuleVO> rules = _firewallRulesDao.listIPForwarding(ipv.getAddress(), false);
                allLbRules.addAll(rules);
            }

            _networkMgr.updateFirewallRules(loadBalancer.getIpAddress(), allLbRules, router);

            // firewall rules are updated, lock the load balancer as mappings are updated
            loadBalancerLock = _loadBalancerDao.acquire(loadBalancerId);
            if (loadBalancerLock == null) {
                s_logger.warn("removeFromLoadBalancer: failed to lock load balancer " + loadBalancerId + ", deleting mappings anyway...");
            }

            // remove all the loadBalancer->VM mappings
            _loadBalancerVMMapDao.remove(loadBalancerId, instanceIds, Boolean.FALSE);

            // Save and create the event
            String description;
            String type = EventTypes.EVENT_NET_RULE_DELETE;
            String level = EventVO.LEVEL_INFO;
            Account account = _accountDao.findById(loadBalancer.getAccountId());

            for (FirewallRuleVO updatedRule : allLbRules) {
                if (!updatedRule.isEnabled()) {
                    _firewallRulesDao.remove(updatedRule.getId());

                    description = "deleted load balancer rule [" + updatedRule.getPublicIpAddress() + ":" + updatedRule.getPublicPort() + "]->["
                            + updatedRule.getPrivateIpAddress() + ":" + updatedRule.getPrivatePort() + "]" + " " + updatedRule.getProtocol();

                    saveEvent(userId, account.getId(), level, type, description);
                }
            }
            txn.commit();
        } catch (Exception ex) {
            s_logger.warn("Failed to delete load balancing rule with exception: ", ex);
            success = false;
            txn.rollback();
        } finally {
            if (loadBalancerLock != null) {
                _loadBalancerDao.release(loadBalancerId);
            }
        }
        return success;
    }

    @Override
    public long removeFromLoadBalancerAsync(long userId, long loadBalancerId, List<Long> instanceIds) {
        LoadBalancerVO loadBalancer = _loadBalancerDao.findById(loadBalancerId);
        IPAddressVO ipAddress = _publicIpAddressDao.findById(loadBalancer.getIpAddress());
        DomainRouterVO router = _routerDao.findBy(loadBalancer.getAccountId(), ipAddress.getDataCenterId());
        LoadBalancerParam param = new LoadBalancerParam(userId, router.getId(), loadBalancerId, instanceIds);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(loadBalancer.getAccountId());
        job.setCmd("RemoveFromLoadBalancer");
        job.setCmdInfo(gson.toJson(param));
        
        return _asyncMgr.submitAsyncJob(job, true);
    }

    @Override @DB
    public boolean deleteLoadBalancer(long userId, long loadBalancerId) {
        Transaction txn = Transaction.currentTxn();
        LoadBalancerVO loadBalancer = null;
        LoadBalancerVO loadBalancerLock = null;
        try {
            loadBalancer = _loadBalancerDao.findById(loadBalancerId);
            if (loadBalancer == null) {
                return false;
            }

            IPAddressVO ipAddress = _publicIpAddressDao.findById(loadBalancer.getIpAddress());
            if (ipAddress == null) {
                return false;
            }

            DomainRouterVO router = _routerDao.findBy(ipAddress.getAccountId(), ipAddress.getDataCenterId());
            List<FirewallRuleVO> fwRules = _firewallRulesDao.listByLoadBalancerId(loadBalancerId);

            txn.start();

            if ((fwRules != null) && !fwRules.isEmpty()) {
                for (FirewallRuleVO fwRule : fwRules) {
                    fwRule.setEnabled(false);
                    _firewallRulesDao.update(fwRule.getId(), fwRule);
                }

                List<FirewallRuleVO> allLbRules = new ArrayList<FirewallRuleVO>();
                List<IPAddressVO> ipAddrs = _networkMgr.listPublicIpAddressesInVirtualNetwork(loadBalancer.getAccountId(), ipAddress.getDataCenterId(), null);
                for (IPAddressVO ipv : ipAddrs) {
                    List<FirewallRuleVO> rules = _firewallRulesDao.listIPForwarding(ipv.getAddress(), false);
                    allLbRules.addAll(rules);
                }

                _networkMgr.updateFirewallRules(loadBalancer.getIpAddress(), allLbRules, router);

                // firewall rules are updated, lock the load balancer as the mappings are updated
                loadBalancerLock = _loadBalancerDao.acquire(loadBalancerId);
                if (loadBalancerLock == null) {
                    s_logger.warn("deleteLoadBalancer: failed to lock load balancer " + loadBalancerId + ", deleting mappings anyway...");
                }

                // remove all loadBalancer->VM mappings
                _loadBalancerVMMapDao.remove(loadBalancerId);

                // Save and create the event
                String description;
                String type = EventTypes.EVENT_NET_RULE_DELETE;
                String ruleName = "load balancer";
                String level = EventVO.LEVEL_INFO;
                Account account = _accountDao.findById(loadBalancer.getAccountId());

                for (FirewallRuleVO updatedRule : fwRules) {
                    _firewallRulesDao.remove(updatedRule.getId());

                    description = "deleted " + ruleName + " rule [" + updatedRule.getPublicIpAddress() + ":" + updatedRule.getPublicPort() + "]->["
                                  + updatedRule.getPrivateIpAddress() + ":" + updatedRule.getPrivatePort() + "]" + " " + updatedRule.getProtocol();

                    saveEvent(userId, account.getId(), level, type, description);
                }
            }

            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            s_logger.error("Unexpected exception deleting load balancer " + loadBalancerId, ex);
            return false;
        } finally {
            if (loadBalancerLock != null) {
                _loadBalancerDao.release(loadBalancerId);
            }
        }

        boolean success = _loadBalancerDao.remove(loadBalancerId);

        // save off an event for removing the security group
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(loadBalancer.getAccountId());
        event.setType(EventTypes.EVENT_LOAD_BALANCER_DELETE);
        if (success) {
            event.setLevel(EventVO.LEVEL_INFO);
            String params = "id="+loadBalancer.getId();
            event.setParameters(params);
            event.setDescription("Successfully deleted load balancer " + loadBalancer.getName() + " (id:" + loadBalancer.getId() + ")");
        } else {
            event.setLevel(EventVO.LEVEL_ERROR);
            event.setDescription("Failed to delete load balancer " + loadBalancer.getName() + " (id:" + loadBalancer.getId() + ")");
        }
        _eventDao.persist(event);
        return success;
    }

    @Override
    public long deleteLoadBalancerAsync(long userId, long loadBalancerId) {
        LoadBalancerVO loadBalancer = _loadBalancerDao.findById(loadBalancerId);
        IPAddressVO ipAddress = _publicIpAddressDao.findById(loadBalancer.getIpAddress());
        DomainRouterVO router = _routerDao.findBy(loadBalancer.getAccountId(), ipAddress.getDataCenterId());
        LoadBalancerParam param = new LoadBalancerParam(userId, router.getId(), loadBalancerId, null);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(loadBalancer.getAccountId());
        job.setCmd("DeleteLoadBalancer");
        job.setCmdInfo(gson.toJson(param));
        
        return _asyncMgr.submitAsyncJob(job, true);
    }

    @Override
    public List<UserVmVO> listLoadBalancerInstances(long loadBalancerId, boolean applied) {
        LoadBalancerVO loadBalancer = _loadBalancerDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            return null;
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
    public List<LoadBalancerVO> searchForLoadBalancers(Criteria c) {
        Filter searchFilter = new Filter(LoadBalancerVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());

        Object accountId = c.getCriteria(Criteria.ACCOUNTID);
        Object id = c.getCriteria(Criteria.ID);
        Object name = c.getCriteria(Criteria.NAME);
        Object domainId = c.getCriteria(Criteria.DOMAINID);
        Object keyword = c.getCriteria(Criteria.KEYWORD);
        Object ipAddress = c.getCriteria(Criteria.IPADDRESS);
        Object instanceId = c.getCriteria(Criteria.INSTANCEID);

        SearchBuilder<LoadBalancerVO> sb = _loadBalancerDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("nameEQ", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("ipAddress", sb.entity().getIpAddress(), SearchCriteria.Op.EQ);

        if ((accountId == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId());
        }

        if (instanceId != null) {
            SearchBuilder<LoadBalancerVMMapVO> lbVMSearch = _loadBalancerVMMapDao.createSearchBuilder();
            lbVMSearch.and("instanceId", lbVMSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
            sb.join("lbVMSearch", lbVMSearch, sb.entity().getId(), lbVMSearch.entity().getLoadBalancerId());
        }

        SearchCriteria sc = sb.create();
        if (keyword != null) {
            SearchCriteria ssc = _loadBalancerDao.createSearchCriteria();
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
            DomainVO domain = _domainDao.findById((Long)domainId);
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

                Transaction txn = Transaction.open(Transaction.CLOUD_DB);
                try {
                    List<AccountVO> accounts = _accountDao.findCleanups();
                    s_logger.info("Found " + accounts.size() + " accounts to cleanup");
                    for (AccountVO account : accounts) {
                        s_logger.debug("Cleaning up " + account.getId());
                        try {
                            deleteAccount(account);
                        } catch (Exception e) {
                            s_logger.error("Skipping due to error on account " + account.getId(), e);
                        }
                    }
                } catch (Exception e) {
                    s_logger.error("Exception ", e);
                } finally {
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
                        _eventDao.delete(event.getId());
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
    public StoragePoolVO addPool(Long zoneId, Long podId, Long clusterId, String poolName, String storageUri, String tags, Map<String, String> details) throws ResourceInUseException, IllegalArgumentException, UnknownHostException, ResourceAllocationException {
        URI uri;
        try {
            uri = new URI(storageUri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URI syntax needed for " + storageUri, e);
        }
        return _storageMgr.createPool(zoneId.longValue(), podId, clusterId, poolName, uri, tags, details);
    }
    
    @Override
    public ClusterVO findClusterById(long clusterId) {
        return _clusterDao.findById(clusterId);
    }
    
    @Override
    public List<ClusterVO> listClusterByPodId(long podId) {
        return _clusterDao.listByPodId(podId);
    }
    
    @Override
    public ClusterVO createCluster(long dcId, long podId, String name) {
        ClusterVO cluster = new ClusterVO(dcId, podId, name);
        try {
            cluster = _clusterDao.persist(cluster);
        } catch (Exception e) {
            cluster = _clusterDao.findBy(name, podId);
            if (cluster == null) {
                throw new CloudRuntimeException("Unable to create cluster " + name + " in pod " + podId + " and data center " + dcId, e);
            }
        }
        return cluster;
    }

    @Override
    public boolean deletePool(Long id)
    {
        return _storageMgr.deletePool(id);
    }

    @Override
    public List<? extends StoragePoolVO> searchForStoragePools(Criteria c) {
        Filter searchFilter = new Filter(StoragePoolVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria sc = _poolDao.createSearchCriteria();

        Object name = c.getCriteria(Criteria.NAME);
        Object host = c.getCriteria(Criteria.HOST);
        Object path = c.getCriteria(Criteria.PATH);
        Object zone = c.getCriteria(Criteria.DATACENTERID);
        Object pod = c.getCriteria(Criteria.PODID);
        Object address = c.getCriteria(Criteria.ADDRESS);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        if (keyword != null) {
            SearchCriteria ssc = _poolDao.createSearchCriteria();
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

        return _poolDao.search(sc, searchFilter);
    }

    @Override
    public StorageStats getStoragePoolStatistics(long id) {
        return _statsCollector.getStoragePoolStats(id);
    }
    
    @Override
    public List<String> searchForStoragePoolDetails(long poolId, String value)
    {
    	return _poolDao.searchForStoragePoolDetails(poolId, value);
    }
    
    @Override
    public String getStoragePoolTags(long poolId) {
    	return _storageMgr.getStoragePoolTags(poolId);
    }
    
    @Override
    public List<AsyncJobVO> searchForAsyncJobs(Criteria c) {
        Filter searchFilter = new Filter(AsyncJobVO.class, c.getOrderBy(), c
                .getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria sc = _jobDao.createSearchCriteria();

        Object accountId = c.getCriteria(Criteria.ACCOUNTID);
        Object status = c.getCriteria(Criteria.STATUS);
        Object keyword = c.getCriteria(Criteria.KEYWORD);
        Object startDate = c.getCriteria(Criteria.STARTDATE);

        if (keyword != null) {
            sc.addAnd("cmd", SearchCriteria.Op.LIKE, "%" + keyword+ "%");
        }

        if (accountId != null) {
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        }

        if(status != null) {
            sc.addAnd("status", SearchCriteria.Op.EQ, status);
        }
        
        if(startDate != null) {
            sc.addAnd("created", SearchCriteria.Op.GTEQ, startDate);
        }
        
        return _jobDao.search(sc, searchFilter);
    }

    @Override
    public SnapshotPolicyVO createSnapshotPolicy(long userId, long accountId, long volumeId, String schedule, String intervalType, int maxSnaps, String timeZoneStr) throws InvalidParameterValueException {
    	IntervalType type =  DateUtil.IntervalType.getIntervalType(intervalType);
    	if(type == null){
    		throw new InvalidParameterValueException("Unsupported interval type " + intervalType);
    	}
    	
    	TimeZone timeZone = TimeZone.getTimeZone(timeZoneStr);
    	String timezoneId = timeZone.getID();
    	if (!timezoneId.equals(timeZoneStr)) {
    	    s_logger.warn("Using timezone: " + timezoneId + " for running this snapshot policy as an equivalent of " + timeZoneStr);
    	}
    	
    	try {
    		DateUtil.getNextRunTime(type, schedule, timezoneId, null);
    	} catch (Exception e){
    		throw new InvalidParameterValueException("Invalid schedule: "+ schedule +" for interval type: " + intervalType);
    	}
    	
    	int intervalMaxSnaps = type.getMax();
    	if(maxSnaps > intervalMaxSnaps){
    		throw new InvalidParameterValueException("maxSnaps exceeds limit: "+ intervalMaxSnaps +" for interval type: " + intervalType);
    	}
    	
    	return _snapMgr.createPolicy(userId, accountId, volumeId, schedule, (short)type.ordinal() , maxSnaps, timezoneId);
    }

    @Override
    public SnapshotPolicyVO findSnapshotPolicyById(Long policyId) {
        return _snapshotPolicyDao.findById(policyId);
    }
    
	@Override
	public boolean deleteSnapshotPolicies(long userId, List<Long> policyIds) throws InvalidParameterValueException {
		boolean result = true;
		if (policyIds.contains(Snapshot.MANUAL_POLICY_ID)) {
		    throw new InvalidParameterValueException("Invalid Policy id given: " + Snapshot.MANUAL_POLICY_ID);
		}
		for (long policyId : policyIds) {
			if (!_snapMgr.deletePolicy(userId, policyId)) {
				result = false;
				s_logger.warn("Failed to delete snapshot policy with Id: " + policyId);
			}
		}
		return result;
	}

	@Override
	public String getSnapshotIntervalTypes(long snapshotId){
	    String intervalTypes = "";
	    List<SnapshotPolicyVO> policies = _snapMgr.listPoliciesforSnapshot(snapshotId);
	    for (SnapshotPolicyVO policy : policies){
	        if(!intervalTypes.isEmpty()){
	            intervalTypes += ",";
	        }
	        if(policy.getId() == Snapshot.MANUAL_POLICY_ID){
	            intervalTypes+= "MANUAL";
	        }
	        else {
	            intervalTypes += DateUtil.getIntervalType(policy.getInterval()).toString();
	        }
	    }
	    return intervalTypes;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<SnapshotScheduleVO> findRecurringSnapshotSchedule(Long volumeId, Long policyId) {
	    return _snapMgr.findRecurringSnapshotSchedule(volumeId, policyId);
	}

	@Override
	public List<SnapshotPolicyVO> listSnapshotPolicies(long volumeId) {
    	if( _volumeDao.findById(volumeId) == null){
    		return null;
    	}
		return _snapMgr.listPoliciesforVolume(volumeId);
	}
    
    @Override
    public boolean isChildDomain(Long parentId, Long childId) {
        return _domainDao.isChildDomain(parentId, childId);
    }
    
    public SecondaryStorageVmVO startSecondaryStorageVm(long instanceId, long startEventId) throws InternalErrorException {
        return _secStorageVmMgr.startSecStorageVm(instanceId, startEventId);
    }

    public boolean stopSecondaryStorageVm(long instanceId, long startEventId) {
        return _secStorageVmMgr.stopSecStorageVm(instanceId, startEventId);
    }

    public boolean rebootSecondaryStorageVm(long instanceId, long startEventId) {
        return _secStorageVmMgr.rebootSecStorageVm(instanceId, startEventId);
    }

    public boolean destroySecondaryStorageVm(long instanceId, long startEventId) {
        return _secStorageVmMgr.destroySecStorageVm(instanceId, startEventId);
    }

    public long startSecondaryStorageVmAsync(long instanceId) {
        long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_START, "starting secondary storage Vm Id: "+instanceId);
        VMOperationParam param = new VMOperationParam(0, instanceId, null, eventId);
        param.setOperation(VmOp.Start);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
        job.setCmd("SystemVmCmd");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(StartSystemVMCmd.getResultObjectName());
        return _asyncMgr.submitAsyncJob(job, true);
    }

    public long stopSecondaryStorageVmAsync(long instanceId) {
        long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_STOP, "stopping secondary storage Vm Id: "+instanceId);
        VMOperationParam param = new VMOperationParam(0, instanceId, null, eventId);
        Gson gson = GsonHelper.getBuilder().create();
        param.setOperation(VmOp.Stop);

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
        job.setCmd("SystemVmCmd");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(StartSystemVMCmd.getResultObjectName());
        return _asyncMgr.submitAsyncJob(job, true);
    }

    public long rebootSecondaryStorageVmAsync(long instanceId) {
        long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_REBOOT, "rebooting secondary storage Vm Id: "+instanceId);
        VMOperationParam param = new VMOperationParam(0, instanceId, null, eventId);
        Gson gson = GsonHelper.getBuilder().create();
        param.setOperation(VmOp.Reboot);

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
        job.setCmd("SystemVmCmd");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(StartSystemVMCmd.getResultObjectName());
        return _asyncMgr.submitAsyncJob(job, true);
    }

    public long destroySecondaryStorageVmAsync(long instanceId) {
        long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_DESTROY, "destroying secondary storage Vm Id: "+instanceId);
        VMOperationParam param = new VMOperationParam(0, instanceId, null, eventId);
        Gson gson = GsonHelper.getBuilder().create();
        param.setOperation(VmOp.Destroy);

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
        job.setCmd("SystemVmCmd");
        job.setCmdInfo(gson.toJson(param));
        return _asyncMgr.submitAsyncJob(job);

    }

	@Override
    public List<SecondaryStorageVmVO> searchForSecondaryStorageVm(Criteria c) {
        Filter searchFilter = new Filter(SecondaryStorageVmVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria sc = _secStorageVmDao.createSearchCriteria();

        Object id = c.getCriteria(Criteria.ID);
        Object name = c.getCriteria(Criteria.NAME);
        Object state = c.getCriteria(Criteria.STATE);
        Object zone = c.getCriteria(Criteria.DATACENTERID);
        Object pod = c.getCriteria(Criteria.PODID);
        Object hostId = c.getCriteria(Criteria.HOSTID);
        Object keyword = c.getCriteria(Criteria.KEYWORD);

        if (keyword != null) {
            SearchCriteria ssc = _secStorageVmDao.createSearchCriteria();
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
	public boolean stopSystemVM(long instanceId, long startEventId) {
		VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(instanceId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
		if (systemVm.getType().equals(VirtualMachine.Type.ConsoleProxy)){
			return stopConsoleProxy(instanceId, startEventId);
		} else {
			return stopSecondaryStorageVm(instanceId, startEventId);
		}
	}

	@Override
	public long stopSystemVmAsync(long instanceId) {
		VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(instanceId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
		if (systemVm.getType().equals(VirtualMachine.Type.ConsoleProxy)){
			return stopConsoleProxyAsync(instanceId);
		} else {
			return stopSecondaryStorageVmAsync(instanceId);
		}
	}

	@Override
	public long rebootSystemVmAsync(long instanceId) {
		VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(instanceId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
		if (systemVm.getType().equals(VirtualMachine.Type.ConsoleProxy)){
			return rebootConsoleProxyAsync(instanceId);
		} else {
			return rebootSecondaryStorageVmAsync(instanceId);
		}
	}
	
	@Override
	public long startSystemVmAsync(long instanceId) {
		VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(instanceId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
		if (systemVm.getType().equals(VirtualMachine.Type.ConsoleProxy)){
			return startConsoleProxyAsync(instanceId);
		} else {
			return startSecondaryStorageVmAsync(instanceId);
		}
	}
	
	@Override
	public VMInstanceVO startSystemVM(long instanceId, long startEventId) throws InternalErrorException {
		VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(instanceId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
		if (systemVm.getType().equals(VirtualMachine.Type.ConsoleProxy)){
			return startConsoleProxy(instanceId, startEventId);
		} else {
			return startSecondaryStorageVm(instanceId, startEventId);
		}
	}

	@Override
	public boolean rebootSystemVM(long instanceId, long startEventId)  {
		VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(instanceId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
		if (systemVm.getType().equals(VirtualMachine.Type.ConsoleProxy)){
			return rebootConsoleProxy(instanceId, startEventId);
		} else {
			return rebootSecondaryStorageVm(instanceId, startEventId);
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
        
    public ArrayList<String> getCloudIdentifierResponse(long userId)
    {
    	Criteria c = new Criteria ();
    	c.addCriteria(Criteria.NAME, "cloud.identifier");

    	List<ConfigurationVO> configs = searchForConfigurations(c, true);
    	
    	String cloudIdentifier;
    	if (configs == null || configs.size() != 1) {
    		cloudIdentifier = "";
    	} else {
    		cloudIdentifier = configs.get(0).getValue();
    	}
    	
    	String signature = "";
    	try {
        	//get the user obj to get his secret key
        	User user = getUser(userId);
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

	@Override
	public boolean isNetworkSecurityGroupNameInUse(Long domainId, Long accountId, String name) {
		if (domainId == null) {
            domainId = DomainVO.ROOT_DOMAIN;
        }
		_networkGroupMgr.createDefaultNetworkGroup(accountId);
        return _networkSecurityGroupDao.isNameInUse(accountId, domainId, name);
	}

	@Override
	public List<IngressRuleVO> authorizeNetworkGroupIngress(AccountVO account, String groupName, String protocol, int startPort, int endPort, String [] cidrList, List<NetworkGroupVO> authorizedGroups) {
		return _networkGroupMgr.authorizeNetworkGroupIngress(account, groupName, protocol, startPort, endPort, cidrList, authorizedGroups);
	}

    @Override
    public long authorizeNetworkGroupIngressAsync(Long accountId, String groupName, String protocol, int startPort, int endPort, String [] cidrList, List<NetworkGroupVO> authorizedGroups) {
        AccountVO account = (AccountVO)findAccountById(accountId);
        if (account == null) {
            s_logger.warn("Unable to authorize network group ingress on group: " + groupName + " for account " + accountId + " -- account not found.");
            return 0;
        }

        NetworkGroupIngressParam param = new NetworkGroupIngressParam(account, groupName, protocol, startPort, endPort, cidrList, authorizedGroups);
        Gson gson = GsonHelper.getBuilder().create();
        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(accountId);
        job.setCmd("AuthorizeNetworkGroupIngress");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(AuthorizeNetworkGroupIngressCmd.getResultObjectName());
        return _asyncMgr.submitAsyncJob(job);
    }

    @Override
	public boolean revokeNetworkGroupIngress(AccountVO account, String groupName, String protocol, int startPort, int endPort, String [] cidrList, List<NetworkGroupVO> authorizedGroups) {
		return _networkGroupMgr.revokeNetworkGroupIngress(account, groupName, protocol, startPort, endPort, cidrList, authorizedGroups);
	}

	@Override
	public long revokeNetworkGroupIngressAsync(Long accountId, String groupName, String protocol, int startPort, int endPort, String [] cidrList, List<NetworkGroupVO> authorizedGroups) {
		AccountVO account = (AccountVO)findAccountById(accountId);
		if (account == null) {
			s_logger.warn("Unable to revoke network group ingress on group: " + groupName + " for account " + accountId + " -- account not found.");
			return 0;
		}

		NetworkGroupIngressParam param = new NetworkGroupIngressParam(account, groupName, protocol, startPort, endPort, cidrList, authorizedGroups);
        Gson gson = GsonHelper.getBuilder().create();
        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(UserContext.current().getUserId());
        job.setAccountId(accountId);
        job.setCmd("RevokeNetworkGroupIngress");
        job.setCmdInfo(gson.toJson(param));
        return _asyncMgr.submitAsyncJob(job);
	}

    @Override
    public NetworkGroupVO createNetworkGroup(String name, String description, Long domainId, Long accountId, String accountName) {
    	return _networkGroupMgr.createNetworkGroup(name, description, domainId, accountId, accountName);
    }

    @Override
    public void deleteNetworkGroup(Long groupId, Long accountId) throws ResourceInUseException, PermissionDeniedException {
        _networkGroupMgr.deleteNetworkGroup(groupId, accountId);
    }

    @Override
    public List<NetworkGroupRulesVO> searchForNetworkGroupRules(Criteria c) {
        return _networkGroupMgr.searchForNetworkGroupRules(c);
    }

	@Override
	public HostStats getHostStatistics(long hostId) {
		return _statsCollector.getHostStats(hostId);
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHypervisorSnapshotCapable() {
        return _isHypervisorSnapshotCapable;
    }
    
    @Override
    public boolean isLocalStorageActiveOnHost(HostVO host) {
    	return _storageMgr.isLocalStorageActiveOnHost(host);
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
	public List<PreallocatedLunVO> getPreAllocatedLuns(Criteria c)
	{
       Filter searchFilter = new Filter(PreallocatedLunVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria sc = _lunDao.createSearchCriteria();

        Object targetIqn = c.getCriteria(Criteria.TARGET_IQN);
        Object scope = c.getCriteria(Criteria.SCOPE);

        if (targetIqn != null) {
            sc.addAnd("targetIqn", SearchCriteria.Op.EQ, targetIqn);
        }
        
        if (scope == null || scope.toString().equalsIgnoreCase("ALL")) {
            return _lunDao.search(sc, searchFilter);
        }
        else if(scope.toString().equalsIgnoreCase("ALLOCATED"))
        {
        	sc.addAnd("volumeId", SearchCriteria.Op.NNULL);
        	sc.addAnd("taken", SearchCriteria.Op.NNULL);
        	
        	return _lunDao.search(sc, searchFilter);
        }
        else if(scope.toString().equalsIgnoreCase("FREE"))
        {
        	sc.addAnd("volumeId", SearchCriteria.Op.NULL);
        	sc.addAnd("taken", SearchCriteria.Op.NULL);
        	
        	return _lunDao.search(sc, searchFilter);
        }
        
		return null;

	}

	@Override
	public String getNetworkGroupsNamesForVm(long vmId) 
	{

		return _networkGroupMgr.getNetworkGroupsNamesForVm(vmId);
	}
}

