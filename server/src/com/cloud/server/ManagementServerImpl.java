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
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.manager.AgentManager;
import com.cloud.alert.AlertManager;
import com.cloud.alert.AlertVO;
import com.cloud.alert.dao.AlertDao;
import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.api.commands.AuthorizeNetworkGroupIngressCmd;
import com.cloud.api.commands.CancelMaintenanceCmd;
import com.cloud.api.commands.CancelPrimaryStorageMaintenanceCmd;
import com.cloud.api.commands.CreateDomainCmd;
import com.cloud.api.commands.CreatePortForwardingServiceCmd;
import com.cloud.api.commands.CreatePortForwardingServiceRuleCmd;
import com.cloud.api.commands.CreateUserCmd;
import com.cloud.api.commands.CreateVolumeCmd;
import com.cloud.api.commands.DeleteUserCmd;
import com.cloud.api.commands.DeployVMCmd;
import com.cloud.api.commands.EnableAccountCmd;
import com.cloud.api.commands.EnableUserCmd;
import com.cloud.api.commands.GetCloudIdentifierCmd;
import com.cloud.api.commands.ListAlertsCmd;
import com.cloud.api.commands.ListAsyncJobsCmd;
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
import com.cloud.api.commands.ListIsosCmd;
import com.cloud.api.commands.ListLoadBalancerRuleInstancesCmd;
import com.cloud.api.commands.ListLoadBalancerRulesCmd;
import com.cloud.api.commands.ListPodsByCmd;
import com.cloud.api.commands.ListPortForwardingServiceRulesCmd;
import com.cloud.api.commands.ListPortForwardingServicesByVmCmd;
import com.cloud.api.commands.ListPortForwardingServicesCmd;
import com.cloud.api.commands.ListPreallocatedLunsCmd;
import com.cloud.api.commands.ListPublicIpAddressesCmd;
import com.cloud.api.commands.ListRoutersCmd;
import com.cloud.api.commands.ListServiceOfferingsCmd;
import com.cloud.api.commands.ListSnapshotsCmd;
import com.cloud.api.commands.ListStoragePoolsCmd;
import com.cloud.api.commands.ListSystemVMsCmd;
import com.cloud.api.commands.ListTemplateOrIsoPermissionsCmd;
import com.cloud.api.commands.ListTemplatesCmd;
import com.cloud.api.commands.LockAccountCmd;
import com.cloud.api.commands.LockUserCmd;
import com.cloud.api.commands.PrepareForMaintenanceCmd;
import com.cloud.api.commands.PreparePrimaryStorageForMaintenanceCmd;
import com.cloud.api.commands.RebootSystemVmCmd;
import com.cloud.api.commands.RegisterCmd;
import com.cloud.api.commands.RemovePortForwardingServiceCmd;
import com.cloud.api.commands.StartSystemVMCmd;
import com.cloud.api.commands.StopSystemVmCmd;
import com.cloud.api.commands.UpdateAccountCmd;
import com.cloud.api.commands.UpdateDomainCmd;
import com.cloud.api.commands.UpdateIsoPermissionsCmd;
import com.cloud.api.commands.UpdateTemplateOrIsoCmd;
import com.cloud.api.commands.UpdateTemplateOrIsoPermissionsCmd;
import com.cloud.api.commands.UpdateTemplatePermissionsCmd;
import com.cloud.api.commands.UpdateUserCmd;
import com.cloud.async.AsyncInstanceCreateStatus;
import com.cloud.async.AsyncJobExecutor;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.async.dao.AsyncJobDao;
import com.cloud.async.executor.CreateOrUpdateRuleParam;
import com.cloud.async.executor.DeleteDomainParam;
import com.cloud.async.executor.DeleteRuleParam;
import com.cloud.async.executor.DeployVMParam;
import com.cloud.async.executor.LoadBalancerParam;
import com.cloud.async.executor.NetworkGroupIngressParam;
import com.cloud.async.executor.SecurityGroupParam;
import com.cloud.async.executor.UpdateLoadBalancerParam;
import com.cloud.async.executor.VMOperationParam;
import com.cloud.async.executor.VMOperationParam.VmOp;
import com.cloud.async.executor.VolumeOperationParam;
import com.cloud.async.executor.VolumeOperationParam.VolumeOp;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ConfigurationVO;
import com.cloud.configuration.ResourceCount.ResourceType;
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
import com.cloud.event.EventTypes;
import com.cloud.event.EventUtils;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.exception.StorageUnavailableException;
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
import com.cloud.network.security.NetworkGroupVO;
import com.cloud.network.security.dao.NetworkGroupDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.offering.ServiceOffering.GuestIpType;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.auth.UserAuthenticator;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.DiskTemplateVO;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.LaunchPermissionVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Snapshot.SnapshotType;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.StorageStats;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
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
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateDao.TemplateFilter;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VolumeDao;
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
import com.cloud.uservm.UserVm;
import com.cloud.utils.DateUtil;
import com.cloud.utils.DateUtil.IntervalType;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.StringUtils;
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

    private final Map<String, Boolean> _availableIdsMap;

	private boolean _networkGroupsEnabled = false;

    private boolean _isHypervisorSnapshotCapable = false;

    private final int _maxVolumeSizeInGb;
    
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

//    @Override
//    public List<? extends Host> discoverHosts(long dcId, Long podId, Long clusterId, String url, String username, String password) throws IllegalArgumentException, DiscoveryException {
//        URI uri;
//        try {
//            uri = new URI(url);
//        } catch (URISyntaxException e) {
//            throw new IllegalArgumentException("Unable to convert the url" + url, e);
//        }
//        // TODO: parameter checks.
//        return _agentMgr.discoverHosts(dcId, podId, clusterId, uri, username, password);
//    }

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
    public UserAccount createUser(CreateUserCmd cmd) {
        Long accountId = null;
        String username = cmd.getUsername();
        String password = cmd.getPassword();
        String firstName = cmd.getFirstname();
        String lastName = cmd.getLastname();
        Long domainId = cmd.getDomainId();
        String email = cmd.getEmail();
        String timezone = cmd.getTimezone();
        String accountName = cmd.getAccountName();
        short userType = cmd.getAccountType().shortValue();
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

            EventUtils.saveEvent(new Long(1), new Long(1), EventVO.LEVEL_INFO, EventTypes.EVENT_USER_CREATE, "User, " + username + " for accountId = " + accountId
                    + " and domainId = " + domainId + " was created.");
            return _userAccountDao.findById(dbUser.getId());
        } catch (Exception e) {
        	EventUtils.saveEvent(new Long(1), new Long(1), EventVO.LEVEL_ERROR, EventTypes.EVENT_USER_CREATE, "Error creating user, " + username + " for accountId = " + accountId
                    + " and domainId = " + domainId);
            if (e instanceof CloudRuntimeException) {
                s_logger.info("unable to create user: " + e);
            } else {
                s_logger.warn("unknown exception creating user", e);
            }
            throw new CloudRuntimeException(e.getMessage());
        }
    }

//    @Override
//    public boolean prepareForMaintenance(long hostId) {
//        try {
//            return _agentMgr.maintain(hostId);
//        } catch (AgentUnavailableException e) {
//            return false;
//        }
//    }
//
//    @Override
//    public long prepareForMaintenanceAsync(long hostId) throws InvalidParameterValueException {
//    	HostVO host = _hostDao.findById(hostId);
//    	
//    	if (host == null) {
//            s_logger.debug("Unable to find host " + hostId);
//            throw new InvalidParameterValueException("Unable to find host with ID: " + hostId + ". Please specify a valid host ID.");
//        }
//        
//        if (_hostDao.countBy(host.getPodId(), Status.PrepareForMaintenance, Status.ErrorInMaintenance, Status.Maintenance) > 0) {
//            throw new InvalidParameterValueException("There are other servers in maintenance mode.");
//        }
//        
//        if (_storageMgr.isLocalStorageActiveOnHost(host)) {
//        	throw new InvalidParameterValueException("There are active VMs using the host's local storage pool. Please stop all VMs on this host that use local storage.");
//        }
//    	
//        Long param = new Long(hostId);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//        job.setUserId(UserContext.current().getUserId());
//        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
//        job.setCmd("PrepareMaintenance");
//        job.setCmdInfo(gson.toJson(param));
//        job.setCmdOriginator(PrepareForMaintenanceCmd.getResultObjectName());
//        return _asyncMgr.submitAsyncJob(job);
//    }

//    @Override
//    public boolean maintenanceCompleted(long hostId) {
//        return _agentMgr.cancelMaintenance(hostId);
//    }
//
//    @Override
//    public long maintenanceCompletedAsync(long hostId) {
//        Long param = new Long(hostId);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//        job.setUserId(UserContext.current().getUserId());
//        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
//        job.setCmd("CompleteMaintenance");
//        job.setCmdInfo(gson.toJson(param));
//        job.setCmdOriginator(CancelMaintenanceCmd.getResultObjectName());
//        return _asyncMgr.submitAsyncJob(job);
//    }

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

    private boolean deleteUserInternal(long userId) {
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
            EventUtils.saveEvent(Long.valueOf(1), Long.valueOf(1), EventVO.LEVEL_INFO, EventTypes.EVENT_USER_DELETE, "User " + username + " (id: " + userId
                    + ") for accountId = " + accountId + " and domainId = " + userAccount.getDomainId() + " was deleted.");
            return true;
        } catch (Exception e) {
            s_logger.error("exception deleting user: " + userId, e);
            long domainId = 0L;
            if (userAccount != null)
                domainId = userAccount.getDomainId();
            EventUtils.saveEvent(Long.valueOf(1), Long.valueOf(1), EventVO.LEVEL_INFO, EventTypes.EVENT_USER_DELETE, "Error deleting user " + username + " (id: " + userId
                    + ") for accountId = " + accountId + " and domainId = " + domainId);
            return false;
        }
    }

//    @Override
//    public long deleteUserAsync(long userId) {
//        Long param = new Long(userId);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//        job.setUserId(UserContext.current().getUserId());
//        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
//        job.setCmd("DeleteUser");
//        job.setCmdInfo(gson.toJson(param));
//        job.setCmdOriginator(DeleteUserCmd.getStaticName());
//
//        return _asyncMgr.submitAsyncJob(job);
//    }

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
                } 
            }
            
            // Mark the account's volumes as destroyed
            List<VolumeVO> volumes = _volumeDao.findDetachedByAccount(accountId);
            for (VolumeVO volume : volumes) {
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
                    SearchCriteria<SecurityGroupVMMapVO> sc = _securityGroupVMMapDao.createSearchCriteria();
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
            		allTemplatesDeleted = _tmpltMgr.delete(userId, template.getId(), null);
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
    public boolean enableUser(EnableUserCmd cmd) throws InvalidParameterValueException{
    	Long userId = cmd.getId();
    	Account adminAccount = (Account)UserContext.current().getAccountObject();
        boolean success = false;
        
        //Check if user exists in the system
        User user = findUserById(userId);
        if ((user == null) || (user.getRemoved() != null))
        	throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        
        // If the user is a System user, return an error
        Account account = findAccountById(user.getAccountId());
        if ((account != null) && (account.getId() == Account.ACCOUNT_ID_SYSTEM)) {
        	throw new InvalidParameterValueException("User id : " + userId + " is a system user, enabling is not allowed");
        }

        if ((adminAccount != null) && !isChildDomain(adminAccount.getDomainId(), account.getDomainId())) {
        	throw new InvalidParameterValueException("Failed to enable user " + userId + ", permission denied.");
        }
        
        success = doSetUserStatus(userId, Account.ACCOUNT_STATE_ENABLED);

        // make sure the account is enabled too
        success = (success && enableAccount(user.getAccountId()));
        
        return success;
    }

    @Override
    public boolean lockUser(LockUserCmd cmd) {
        boolean success = false;
        
        Account adminAccount = (Account)UserContext.current().getAccountObject();
        Long id = cmd.getId();

        // Check if user with id exists in the system
        User user = _userDao.findById(id);
        if (user == null) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to find user by id");
        } else if (user.getRemoved() != null) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to find user by id");
        }

        // If the user is a System user, return an error.  We do not allow this
        Account account = _accountDao.findById(user.getAccountId());
        if ((account != null) && (account.getId() == Account.ACCOUNT_ID_SYSTEM)) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "user id : " + id + " is a system user, locking is not allowed");
        }

        if ((adminAccount != null) && !_domainDao.isChildDomain(adminAccount.getDomainId(), account.getDomainId())) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Failed to lock user " + id + ", permission denied.");
        }

        // make sure the account is enabled too
        // if the user is either locked already or disabled already, don't change state...only lock currently enabled users
        if (user.getState().equals(Account.ACCOUNT_STATE_LOCKED)) {
            // already locked...no-op
            return true;
        } else if (user.getState().equals(Account.ACCOUNT_STATE_ENABLED)) {
            success = doSetUserStatus(user.getId(), Account.ACCOUNT_STATE_LOCKED);

            boolean lockAccount = true;
            List<UserVO> allUsersByAccount = _userDao.listByAccount(user.getAccountId());
            for (UserVO oneUser : allUsersByAccount) {
                if (oneUser.getState().equals(Account.ACCOUNT_STATE_ENABLED)) {
                    lockAccount = false;
                    break;
                }
            }

            if (lockAccount) {
                success = (success && lockAccountInternal(user.getAccountId()));
            }
        } else {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Attempting to lock a non-enabled user, current state is " + user.getState() + " (userId: " + user.getId() + "), locking failed.");
            }
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
    public boolean updateAccount(UpdateAccountCmd cmd) throws InvalidParameterValueException{
    	Long domainId = cmd.getDomainId();
    	String accountName = cmd.getAccountName();
    	String newAccountName = cmd.getNewName();
    	
    	if (newAccountName == null) {
    		newAccountName = accountName;
    	}
    	
        boolean success = false;
        Account account = _accountDao.findAccount(accountName, domainId);

        //Check if account exists
        if (account == null) {
        	s_logger.error("Unable to find account " + accountName + " in domain " + domainId);
    		throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
        }
        
        //Don't allow to modify system account
        if (account.getId().longValue() == Account.ACCOUNT_ID_SYSTEM) {
    		throw new InvalidParameterValueException ("Can not modify system account");
    	}
        
        //Check if user performing the action is allowed to modify this account
        Account adminAccount = (Account)UserContext.current().getAccountObject();
        if ((adminAccount != null) && isChildDomain(adminAccount.getDomainId(), account.getDomainId())) {
          throw new InvalidParameterValueException("Invalid account " + accountName + " in domain " + domainId + " given, unable to update account.");
        }
        
        if (account.getAccountName().equals(accountName)) {
            success = true;
        } else {
            AccountVO acctForUpdate = _accountDao.createForUpdate();
            acctForUpdate.setAccountName(newAccountName);
            success = _accountDao.update(Long.valueOf(account.getId()), acctForUpdate);
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

    public boolean enableAccount(long accountId) {
        boolean success = false;
        AccountVO acctForUpdate = _accountDao.createForUpdate();
        acctForUpdate.setState(Account.ACCOUNT_STATE_ENABLED);
        success = _accountDao.update(Long.valueOf(accountId), acctForUpdate);
        return success;
    }
    	
    
    @Override
    public boolean enableAccount(EnableAccountCmd cmd) throws InvalidParameterValueException{
    	String accountName = cmd.getAccountName();
    	Long domainId = cmd.getDomainId();
        boolean success = false;
        Account account = _accountDao.findActiveAccount(accountName, domainId);

        //Check if account exists
        if (account == null) {
        	s_logger.error("Unable to find account " + accountName + " in domain " + domainId);
    		throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
        }
        success = enableAccount(account.getId());
        return success;
    }

    private boolean lockAccountInternal(long accountId) {
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


    @Override
    public boolean updateUser(UpdateUserCmd cmd) throws InvalidParameterValueException
    {
        Long id = cmd.getId();
        String apiKey = cmd.getApiKey();
        String firstName = cmd.getFirstname();
    	String email = cmd.getEmail();
    	String lastName = cmd.getLastname();
    	String password = cmd.getPassword();
    	String secretKey = cmd.getSecretKey();
    	String timeZone = cmd.getTimezone();
    	String userName = cmd.getUsername();
    	
        //Input validation
    	UserVO user = _userDao.getUser(id);
    	
        if (user == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find user by id");
        }

        if((apiKey == null && secretKey != null) || (apiKey != null && secretKey == null))
        {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please provide an api key/secret key pair");
        }
        
        // If the account is an admin type, return an error.  We do not allow this
        Account account = (Account)UserContext.current().getAccountObject();
        
        if (account != null && (account.getId() == Account.ACCOUNT_ID_SYSTEM)) {
        	throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "user id : " + id + " is system account, update is not allowed");
        }

        if (firstName == null) { 
        	firstName = user.getFirstname();
        }
        if (lastName == null) { 
        	lastName = user.getLastname(); 
        }
        if (userName == null) { 
        	userName = user.getUsername();  
        }
        if (password == null) { 
        	password = user.getPassword();
        }
        if (email == null) {
        	email = user.getEmail();
        }
        if (timeZone == null) {
        	timeZone = user.getTimezone();
        }
        if (apiKey == null) {
        	apiKey = user.getApiKey();
        }
        if (secretKey == null) {
        	secretKey = user.getSecretKey();
        }

        Long accountId = user.getAccountId();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("updating user with id: " + id);
        }
        UserAccount userAccount = _userAccountDao.findById(id);
        try
        {
        	//check if the apiKey and secretKey are globally unique
        	if(apiKey != null && secretKey != null)
        	{
        		Pair<User, Account> apiKeyOwner = findUserByApiKey(apiKey);
        		
        		if(apiKeyOwner != null)
        		{
        			User usr = apiKeyOwner.first();
        			
        			if(usr.getId() != id)
            			throw new InvalidParameterValueException("The api key:"+apiKey+" exists in the system for user id:"+id+" ,please provide a unique key");
        			else
        			{
        				//allow the updation to take place
        			}
        		}
        	
        	}

        	
            _userDao.update(id, userName, password, firstName, lastName, email, accountId, timeZone, apiKey, secretKey);
            EventUtils.saveEvent(new Long(1), Long.valueOf(1), EventVO.LEVEL_INFO, EventTypes.EVENT_USER_UPDATE, "User, " + userName + " for accountId = "
                    + accountId + " domainId = " + userAccount.getDomainId() + " and timezone = "+timeZone + " was updated.");
        } catch (Throwable th) {
            s_logger.error("error updating user", th);
            EventUtils.saveEvent(Long.valueOf(1), Long.valueOf(1), EventVO.LEVEL_ERROR, EventTypes.EVENT_USER_UPDATE, "Error updating user, " + userName
                    + " for accountId = " + accountId + " and domainId = " + userAccount.getDomainId());
            return false;
        }
        return true;
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
    public String[] createApiKeyAndSecretKey(RegisterCmd cmd)
    {
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

//    @Override
//    @DB
//    public String associateIpAddress(long userId, long accountId, long domainId, long zoneId) throws ResourceAllocationException, InsufficientAddressCapacityException,
//            InvalidParameterValueException, InternalErrorException {
//        Transaction txn = Transaction.currentTxn();
//        AccountVO account = null;
//        try {
//            if (s_logger.isDebugEnabled()) {
//                s_logger.debug("Associate IP address called for user " + userId + " account " + accountId);
//            }
//            account = _accountDao.acquire(accountId);
//
//            if (account == null) {
//                s_logger.warn("Unable to lock account: " + accountId);
//                throw new InternalErrorException("Unable to acquire account lock");
//            }
//
//            if (s_logger.isDebugEnabled()) {
//                s_logger.debug("Associate IP address lock acquired");
//            }
//
//            // Check that the maximum number of public IPs for the given
//            // accountId will not be exceeded
//            if (_accountMgr.resourceLimitExceeded(account, ResourceType.public_ip)) {
//                ResourceAllocationException rae = new ResourceAllocationException("Maximum number of public IP addresses for account: " + account.getAccountName()
//                        + " has been exceeded.");
//                rae.setResourceType("ip");
//                throw rae;
//            }
//
//            DomainRouterVO router = _routerDao.findBy(accountId, zoneId);
//            if (router == null) {
//                throw new InvalidParameterValueException("No router found for account: " + account.getAccountName() + ".");
//            }
//
//            txn.start();
//
//            String ipAddress = null;
//            Pair<String, VlanVO> ipAndVlan = _vlanDao.assignIpAddress(zoneId, accountId, domainId, VlanType.VirtualNetwork, false);
//            
//            if (ipAndVlan == null) {
//                throw new InsufficientAddressCapacityException("Unable to find available public IP addresses");
//            } else {
//            	ipAddress = ipAndVlan.first();
//            	_accountMgr.incrementResourceCount(accountId, ResourceType.public_ip);
//            }
//
//            boolean success = true;
//            String errorMsg = "";
//
//            List<String> ipAddrs = new ArrayList<String>();
//            ipAddrs.add(ipAddress);
//
//            if (router.getState() == State.Running) {
//                success = _networkMgr.associateIP(router, ipAddrs, true);
//                if (!success) {
//                    errorMsg = "Unable to assign public IP address.";
//                }
//            }
//
//            EventVO event = new EventVO();
//            event.setUserId(userId);
//            event.setAccountId(accountId);
//            event.setType(EventTypes.EVENT_NET_IP_ASSIGN);
//            event.setParameters("address=" + ipAddress + "\nsourceNat=" + false + "\ndcId=" + zoneId);
//
//            if (!success) {
//                _publicIpAddressDao.unassignIpAddress(ipAddress);
//                ipAddress = null;
//                _accountMgr.decrementResourceCount(accountId, ResourceType.public_ip);
//
//                event.setLevel(EventVO.LEVEL_ERROR);
//                event.setDescription(errorMsg);
//                _eventDao.persist(event);
//                txn.commit();
//
//                throw new InternalErrorException(errorMsg);
//            } else {
//                event.setDescription("Assigned a public IP address: " + ipAddress);
//                _eventDao.persist(event);
//            }
//
//            txn.commit();
//            return ipAddress;
//
//        } catch (ResourceAllocationException rae) {
//            s_logger.error("Associate IP threw a ResourceAllocationException.", rae);
//            throw rae;
//        } catch (InsufficientAddressCapacityException iace) {
//            s_logger.error("Associate IP threw an InsufficientAddressCapacityException.", iace);
//            throw iace;
//        } catch (InvalidParameterValueException ipve) {
//            s_logger.error("Associate IP threw an InvalidParameterValueException.", ipve);
//            throw ipve;
//        } catch (InternalErrorException iee) {
//            s_logger.error("Associate IP threw an InternalErrorException.", iee);
//            throw iee;
//        } catch (Throwable t) {
//            s_logger.error("Associate IP address threw an exception.", t);
//            throw new InternalErrorException("Associate IP address exception");
//        } finally {
//            if (account != null) {
//                _accountDao.release(accountId);
//                s_logger.debug("Associate IP address lock released");
//            }
//        }
//    }

//    @Override
//    public long associateIpAddressAsync(long userId, long accountId, long domainId, long zoneId) {
//        AssociateIpAddressParam param = new AssociateIpAddressParam(userId, accountId, domainId, zoneId);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//        job.setUserId(UserContext.current().getUserId());
//        job.setAccountId(accountId);
//        job.setCmd("AssociateIpAddress");
//        job.setCmdInfo(gson.toJson(param));
//        job.setCmdOriginator(AssociateIPAddrCmd.getResultObjectName());
//        
//        return _asyncMgr.submitAsyncJob(job, true);
//    }

//    @Override
//    @DB
//    public boolean disassociateIpAddress(DisassociateIPAddrCmd cmd) throws PermissionDeniedException, IllegalArgumentException {
//        Transaction txn = Transaction.currentTxn();
//        
//        Long userId = UserContext.current().getUserId();
//        Account account = (Account)UserContext.current().getAccountObject();
//        String ipAddress = cmd.getIpAddress();
//
//        // Verify input parameters
//        Account accountByIp = findAccountByIpAddress(ipAddress);
//        if(accountByIp == null) {
//            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find account owner for ip " + ipAddress);
//        }
//
//        Long accountId = accountByIp.getId();
//        if (account != null) {
//            if (!isAdmin(account.getType())) {
//                if (account.getId().longValue() != accountId.longValue()) {
//                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "account " + account.getAccountName() + " doesn't own ip address " + ipAddress);
//                }
//            } else if (!isChildDomain(account.getDomainId(), accountByIp.getDomainId())) {
//                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to disassociate IP address " + ipAddress + ", permission denied.");
//            }
//        }
//
//        // If command is executed via 8096 port, set userId to the id of System account (1)
//        if (userId == null) {
//            userId = Long.valueOf(1);
//        }
//
//        try {
//            IPAddressVO ipVO = _publicIpAddressDao.findById(ipAddress);
//            if (ipVO == null) {
//                return false;
//            }
//
//            if (ipVO.getAllocated() == null) {
//                return true;
//            }
//
//            AccountVO accountVO = _accountDao.findById(accountId);
//            if (accountVO == null) {
//                return false;
//            }
//          
//            if ((ipVO.getAccountId() == null) || (ipVO.getAccountId().longValue() != accountId)) {
//                // FIXME: is the user visible in the admin account's domain????
//                if (!BaseCmd.isAdmin(accountVO.getType())) {
//                    if (s_logger.isDebugEnabled()) {
//                        s_logger.debug("permission denied disassociating IP address " + ipAddress + "; acct: " + accountId + "; ip (acct / dc / dom / alloc): "
//                                + ipVO.getAccountId() + " / " + ipVO.getDataCenterId() + " / " + ipVO.getDomainId() + " / " + ipVO.getAllocated());
//                    }
//                    throw new PermissionDeniedException("User/account does not own supplied address");
//                }
//            }
//
//            if (ipVO.getAllocated() == null) {
//                return true;
//            }
//
//            if (ipVO.isSourceNat()) {
//                throw new IllegalArgumentException("ip address is used for source nat purposes and can not be disassociated.");
//            }
//            
//            VlanVO vlan = _vlanDao.findById(ipVO.getVlanDbId());
//            if (!vlan.getVlanType().equals(VlanType.VirtualNetwork)) {
//            	throw new IllegalArgumentException("only ip addresses that belong to a virtual network may be disassociated.");
//            }
//			
//			//Check for account wide pool. It will have an entry for account_vlan_map. 
//            if (_accountVlanMapDao.findAccountVlanMap(accountId,ipVO.getVlanDbId()) != null){
//            	throw new PermissionDeniedException(ipAddress + " belongs to Account wide IP pool and cannot be disassociated");
//            }
//			
//            txn.start();
//            boolean success = _networkMgr.releasePublicIpAddress(userId, ipAddress);
//            if (success)
//            	_accountMgr.decrementResourceCount(accountId, ResourceType.public_ip);
//            txn.commit();
//            return success;
//
//        } catch (PermissionDeniedException pde) {
//            throw pde;
//        } catch (IllegalArgumentException iae) {
//            throw iae;
//        } catch (Throwable t) {
//            s_logger.error("Disassociate IP address threw an exception.");
//            throw new IllegalArgumentException("Disassociate IP address threw an exception");
//        }
//    }

//    @Override
//    public long disassociateIpAddressAsync(long userId, long accountId, String ipAddress) {
//        DisassociateIpAddressParam param = new DisassociateIpAddressParam(userId, accountId, ipAddress);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//        job.setUserId(UserContext.current().getUserId());
//        job.setAccountId(accountId);
//        job.setCmd("DisassociateIpAddress");
//        job.setCmdInfo(gson.toJson(param));
//        
//        return _asyncMgr.submitAsyncJob(job, true);
//    }

    @Override
    public VolumeVO createVolume(long userId, long accountId, String name, long zoneId, long diskOfferingId, long startEventId, long size) throws InternalErrorException {
    	EventUtils.saveStartedEvent(userId, accountId, EventTypes.EVENT_VOLUME_CREATE, "Creating volume", startEventId);
        DataCenterVO zone = _dcDao.findById(zoneId);
        DiskOfferingVO diskOffering = _diskOfferingDao.findById(diskOfferingId);
        VolumeVO createdVolume = _storageMgr.createVolume(accountId, userId, name, zone, diskOffering, startEventId,size);

        if (createdVolume != null)
            return createdVolume;
        else
            throw new InternalErrorException("Failed to create volume.");
    }

    @Override
    public long createVolumeAsync(long userId, long accountId, String name, long zoneId, long diskOfferingId, long size) throws InvalidParameterValueException, InternalErrorException, ResourceAllocationException {
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

        long eventId = EventUtils.saveScheduledEvent(userId, accountId, EventTypes.EVENT_VOLUME_CREATE, "creating volume");
        
        VolumeOperationParam param = new VolumeOperationParam();
        param.setOp(VolumeOp.Create);
        param.setAccountId(accountId);
        param.setUserId(UserContext.current().getUserId());
        param.setName(name);
        param.setZoneId(zoneId);
        param.setDiskOfferingId(diskOfferingId);
        param.setEventId(eventId);
        param.setSize(size);
        
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
    
//    @Override
//    public void destroyVolume(long volumeId) throws InvalidParameterValueException {
//        // Check that the volume is valid
//        VolumeVO volume = _volumeDao.findById(volumeId);
//        if (volume == null) {
//            throw new InvalidParameterValueException("Please specify a valid volume ID.");
//        }
//
//        // Check that the volume is stored on shared storage
//        if (!_storageMgr.volumeOnSharedStoragePool(volume)) {
//            throw new InvalidParameterValueException("Please specify a volume that has been created on a shared storage pool.");
//        }
//
//        // Check that the volume is not currently attached to any VM
//        if (volume.getInstanceId() != null) {
//            throw new InvalidParameterValueException("Please specify a volume that is not attached to any VM.");
//        }
//           
//        // Check that the volume is not already destroyed
//        if (volume.getDestroyed()) {
//            throw new InvalidParameterValueException("Please specify a volume that is not already destroyed.");
//        }
//        
//        // Destroy the volume
//        _storageMgr.destroyVolume(volume);
//    }

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
    	return PasswordGenerator.generateRandomPassword();
    }

//    @Override
//    public boolean resetVMPassword(long userId, long vmId, String password) {
//        if (password == null || password.equals("")) {
//            return false;
//        }
//        boolean succeed = _vmMgr.resetVMPassword(userId, vmId, password);
//
//        // Log event
//        UserVmVO userVm = _userVmDao.findById(vmId);
//        if (userVm != null) {
//            if (succeed) {
//            	EventUtils.saveEvent(userId, userVm.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_VM_RESETPASSWORD, "successfully reset password for VM : " + userVm.getName(), null);
//            } else {
//            	EventUtils.saveEvent(userId, userVm.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_RESETPASSWORD, "unable to reset password for VM : " + userVm.getName(), null);
//            }
//        } else {
//            s_logger.warn("Unable to find vm = " + vmId + " to reset password");
//        }
//        return succeed;
//    }

//    @Override
//    public void attachVolumeToVM(long vmId, long volumeId, Long deviceId, long startEventId) throws InternalErrorException {
//        _vmMgr.attachVolumeToVM(vmId, volumeId, deviceId, startEventId);
//    }
//
//    @Override
//    public long attachVolumeToVMAsync(long vmId, long volumeId, Long deviceId) throws InvalidParameterValueException {
//        VolumeVO volume = _volumeDao.findById(volumeId);
//
//        // Check that the volume is a data volume
//        if (volume == null || volume.getVolumeType() != VolumeType.DATADISK) {
//            throw new InvalidParameterValueException("Please specify a valid data volume.");
//        }
//
//        // Check that the volume is stored on shared storage
//        if (!_storageMgr.volumeOnSharedStoragePool(volume)) {
//            throw new InvalidParameterValueException("Please specify a volume that has been created on a shared storage pool.");
//        }
//
//        // Check that the VM is a UserVM
//        UserVmVO vm = _userVmDao.findById(vmId);
//        if (vm == null || vm.getType() != VirtualMachine.Type.User) {
//            throw new InvalidParameterValueException("Please specify a valid User VM.");
//        }
//        
//        // Check that the VM is in the correct state
//        if (vm.getState() != State.Running && vm.getState() != State.Stopped) {
//        	throw new InvalidParameterValueException("Please specify a VM that is either running or stopped.");
//        }
//
//        // Check that the volume is not currently attached to any VM
//        if (volume.getInstanceId() != null) {
//            throw new InvalidParameterValueException("Please specify a volume that is not attached to any VM.");
//        }
//
//        // Check that the volume is not destroyed
//        if (volume.getDestroyed()) {
//            throw new InvalidParameterValueException("Please specify a volume that is not destroyed.");
//        }
//
//        // Check that the VM has less than 6 data volumes attached
//        List<VolumeVO> existingDataVolumes = _volumeDao.findByInstanceAndType(vmId, VolumeType.DATADISK);
//        if (existingDataVolumes.size() >= 6) {
//            throw new InvalidParameterValueException("The specified VM already has the maximum number of data disks (6). Please specify another VM.");
//        }
//        
//        // Check that the VM and the volume are in the same zone
//        if (vm.getDataCenterId() != volume.getDataCenterId()) {
//        	throw new InvalidParameterValueException("Please specify a VM that is in the same zone as the volume.");
//        }
//        long eventId = EventUtils.saveScheduledEvent(1L, volume.getAccountId(), EventTypes.EVENT_VOLUME_ATTACH, "attaching volume: "+volumeId+" to Vm: "+vmId);
//        VolumeOperationParam param = new VolumeOperationParam();
//        param.setUserId(1);
//        param.setAccountId(volume.getAccountId());
//        param.setOp(VolumeOp.Attach);
//        param.setVmId(vmId);
//        param.setVolumeId(volumeId);
//        param.setEventId(eventId);
//        param.setDeviceId(deviceId);
//
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//        job.setUserId(UserContext.current().getUserId());
//        job.setAccountId(vm.getAccountId());
//        job.setCmd("VolumeOperation");
//        job.setCmdInfo(gson.toJson(param));
//        job.setCmdOriginator("virtualmachine");
//        
//        return _asyncMgr.submitAsyncJob(job);
//    }

//    @Override
//    public void detachVolumeFromVM(long volumeId, long startEventId) throws InternalErrorException {
//        _vmMgr.detachVolumeFromVM(volumeId, startEventId);
//    }

//    @Override
//    public long detachVolumeFromVMAsync(long volumeId) throws InvalidParameterValueException {
//        VolumeVO volume = _volumeDao.findById(volumeId);
//
//        // Check that the volume is a data volume
//        if (volume.getVolumeType() != VolumeType.DATADISK) {
//            throw new InvalidParameterValueException("Please specify a data volume.");
//        }
//
//        // Check that the volume is stored on shared storage
//        if (!_storageMgr.volumeOnSharedStoragePool(volume)) {
//            throw new InvalidParameterValueException("Please specify a volume that has been created on a shared storage pool.");
//        }
//
//        Long vmId = volume.getInstanceId();
//
//        // Check that the volume is currently attached to a VM
//        if (vmId == null) {
//            throw new InvalidParameterValueException("The specified volume is not attached to a VM.");
//        }
//
//        // Check that the VM is in the correct state
//        UserVmVO vm = _vmDao.findById(vmId);
//        if (vm.getState() != State.Running && vm.getState() != State.Stopped) {
//        	throw new InvalidParameterValueException("Please specify a VM that is either running or stopped.");
//        }
//        
//        long eventId = EventUtils.saveScheduledEvent(1L, volume.getAccountId(), EventTypes.EVENT_VOLUME_DETACH, "detaching volume: "+volumeId+" from Vm: "+vmId);
//        VolumeOperationParam param = new VolumeOperationParam();
//        param.setUserId(1);
//        param.setAccountId(volume.getAccountId());
//        param.setOp(VolumeOp.Detach);
//        param.setVolumeId(volumeId);
//        param.setEventId(eventId);
//
//        Gson gson = GsonHelper.getBuilder().create();
//
//		AsyncJobVO job = new AsyncJobVO();
//		job.setUserId(UserContext.current().getUserId());
//		job.setAccountId(vm.getAccountId());
//		job.setCmd("VolumeOperation");
//		job.setCmdInfo(gson.toJson(param));
//		job.setCmdOriginator("virtualmachine");
//		
//		return _asyncMgr.submitAsyncJob(job);
//	}

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
    

//    @Override
//    public long attachISOToVMAsync(long vmId, long userId, long isoId) throws InvalidParameterValueException {
//        UserVmVO vm = _userVmDao.findById(vmId);
//        if (vm == null) {
//            throw new InvalidParameterValueException("Unable to find VM with ID " + vmId);
//        }
//
//        VMTemplateVO iso = _templateDao.findById(isoId);
//        if (iso == null || !iso.getFormat().equals(ImageFormat.ISO)) {
//            throw new InvalidParameterValueException("Unable to find ISO with id " + isoId);
//        }
//
//        AccountVO account = _accountDao.findById(vm.getAccountId());
//        if (account == null) {
//            throw new InvalidParameterValueException("Unable to find account for VM with ID " + vmId);
//        }
//        
//        State vmState = vm.getState();
//        if (vmState != State.Running && vmState != State.Stopped) {
//        	throw new InvalidParameterValueException("Please specify a VM that is either Stopped or Running.");
//        }
//        
//        long eventId = EventUtils.saveScheduledEvent(userId, account.getId(), EventTypes.EVENT_ISO_ATTACH, "attaching ISO: "+isoId+" to Vm: "+vmId);
//        
//        AttachISOParam param = new AttachISOParam(vmId, userId, isoId, true);
//        param.setEventId(eventId);
//        Gson gson = GsonHelper.getBuilder().create();
//
//		AsyncJobVO job = new AsyncJobVO();
//		job.setUserId(UserContext.current().getUserId());
//		job.setAccountId(vm.getAccountId());
//		job.setCmd("AttachISO");
//		job.setCmdInfo(gson.toJson(param));
//		return _asyncMgr.submitAsyncJob(job, true);
//	}

//    @Override
//    public long detachISOFromVMAsync(long vmId, long userId) throws InvalidParameterValueException {
//        UserVm userVM = _userVmDao.findById(vmId);
//        if (userVM == null) {
//            throw new InvalidParameterValueException("Please specify a valid VM.");
//        }
//
//        Long isoId = userVM.getIsoId();
//        if (isoId == null) {
//            throw new InvalidParameterValueException("The specified VM has no ISO attached to it.");
//        }
//        
//        State vmState = userVM.getState();
//        if (vmState != State.Running && vmState != State.Stopped) {
//        	throw new InvalidParameterValueException("Please specify a VM that is either Stopped or Running.");
//        }
//
//        long eventId = EventUtils.saveScheduledEvent(userId, userVM.getAccountId(), EventTypes.EVENT_ISO_DETACH, "detaching ISO: "+isoId+" from Vm: "+vmId);
//        AttachISOParam param = new AttachISOParam(vmId, userId, isoId.longValue(), false);
//        param.setEventId(eventId);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//        job.setUserId(UserContext.current().getUserId());
//        job.setAccountId(userVM.getAccountId());
//        job.setCmd("AttachISO");
//        job.setCmdInfo(gson.toJson(param));
//        return _asyncMgr.submitAsyncJob(job, true);
//    }

//    @Override
//    public long resetVMPasswordAsync(long userId, long vmId, String password) {
//        ResetVMPasswordParam param = new ResetVMPasswordParam(userId, vmId, password);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        UserVm vm = _userVmDao.findById(vmId);
//		AsyncJobVO job = new AsyncJobVO();
//		job.setUserId(UserContext.current().getUserId());
//		job.setAccountId(vm.getAccountId());
//		job.setCmd("ResetVMPassword");
//		job.setCmdInfo(gson.toJson(param));
//		job.setCmdOriginator("virtualmachine");
//		
//		return _asyncMgr.submitAsyncJob(job, true);
//	}

    private boolean validPassword(String password) {
        for (int i = 0; i < password.length(); i++) {
            if (password.charAt(i) == ' ') {
                return false;
            }
        }
        return true;
    }

//    @Override
//    public boolean reconnect(long hostId) {
//        try {
//            return _agentMgr.reconnect(hostId);
//        } catch (AgentUnavailableException e) {
//            return false;
//        }
//    }

//    @Override
//    public long reconnectAsync(long hostId) {
//        Long param = new Long(hostId);
//        Gson gson = GsonHelper.getBuilder().create();
//
//		AsyncJobVO job = new AsyncJobVO();
//		job.setUserId(UserContext.current().getUserId());
//		job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
//		job.setCmd("Reconnect");
//		job.setCmdInfo(gson.toJson(param));
//        job.setCmdOriginator(ReconnectHostCmd.getResultObjectName());
//        
//		return _asyncMgr.submitAsyncJob(job);
//	}

    @Override
    public UserVm deployVirtualMachine(long userId, long accountId, long dataCenterId, long serviceOfferingId, long templateId, Long diskOfferingId,
            String domain, String password, String displayName, String group, String userData, String [] networkGroups, long startEventId, long size) throws ResourceAllocationException, InvalidParameterValueException, InternalErrorException,
            InsufficientStorageCapacityException, PermissionDeniedException, ExecutionException, StorageUnavailableException, ConcurrentOperationException {

    	EventUtils.saveStartedEvent(userId, accountId, EventTypes.EVENT_VM_CREATE, "Deploying Vm", startEventId);
        
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
            		created = _vmMgr.createDirectlyAttachedVMExternal(vmId, userId, account, dc, offering, template, diskOffering, displayName, group, userData, a, networkGroupVOs, startEventId, size);
            	} catch (ResourceAllocationException rae) {
            		throw rae;
            	}
            } else {
            	if (offering.getGuestIpType() == GuestIpType.Virtualized) {
            		try {
            			externalIp = _networkMgr.assignSourceNatIpAddress(account, dc, domain, offering, startEventId);
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
            			created = _vmMgr.createVirtualMachine(vmId, userId, account, dc, offering, template, diskOffering, displayName, group, userData, a, startEventId, size);
            		} catch (ResourceAllocationException rae) {
            			throw rae;
            		}
            	} else {
            		try {
            			created = _vmMgr.createDirectlyAttachedVM(vmId, userId, account, dc, offering, template, diskOffering, displayName, group, userData, a, networkGroupVOs, startEventId, size);
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
            UserVmVO started = null;
            if (isIso)
            {
                String isoPath = _storageMgr.getAbsoluteIsoPath(templateId, dataCenterId);
                try
                {
					started = _vmMgr.startVirtualMachine(userId, created.getId(), password, isoPath, startEventId);
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
                    throw new InternalErrorException("Unable to start the VM " + created.getId() + "-" + created.getName());
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
                s_logger.debug("VM started: " + started.getId() + "-" + started.getName());
            }
            return started;
        }

        return null;
    }

    @Override
    public long deployVirtualMachineAsync(long userId, long accountId, long dataCenterId, long serviceOfferingId, long templateId,
            Long diskOfferingId, String domain, String password, String displayName, String group, String userData, String [] networkGroups, long size)  throws InvalidParameterValueException, PermissionDeniedException {

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
        
        long eventId = EventUtils.saveScheduledEvent(userId, accountId, EventTypes.EVENT_VM_CREATE, "deploying Vm");
        
        DeployVMParam param = new DeployVMParam(userId, accountId, dataCenterId, serviceOfferingId, templateId, diskOfferingId, domain, password,
                displayName, group, userData, networkGroups, eventId, size);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(accountId);
        job.setCmd("DeployVM");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(DeployVMCmd.getResultObjectName());
        return _asyncMgr.submitAsyncJob(job);
    }

//    @Override
//    public UserVm startVirtualMachine(long userId, long vmId, String isoPath) throws InternalErrorException, ExecutionException, StorageUnavailableException, ConcurrentOperationException {
//        return _vmMgr.startVirtualMachine(userId, vmId, isoPath, 0);
//    }

//    @Override
//    public long startVirtualMachineAsync(long userId, long vmId, String isoPath) {
//        
//        UserVmVO userVm = _userVmDao.findById(vmId);
//
//        long eventId = EventUtils.saveScheduledEvent(userId, userVm.getAccountId(), EventTypes.EVENT_VM_START, "starting Vm with Id: "+vmId);
//        
//        VMOperationParam param = new VMOperationParam(userId, vmId, isoPath, eventId);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//    	job.setUserId(UserContext.current().getUserId());
//    	job.setAccountId(userVm.getAccountId());
//        job.setCmd("StartVM");
//        job.setCmdInfo(gson.toJson(param));
//        job.setCmdOriginator(StartVMCmd.getResultObjectName());
//        return _asyncMgr.submitAsyncJob(job, true);
//    }

//    @Override
//    public boolean stopVirtualMachine(long userId, long vmId) {
//        return _vmMgr.stopVirtualMachine(userId, vmId);
//    }

//    @Override
//    public long stopVirtualMachineAsync(long userId, long vmId) {
//        
//        UserVmVO userVm = _userVmDao.findById(vmId);
//
//        long eventId = EventUtils.saveScheduledEvent(userId, userVm.getAccountId(), EventTypes.EVENT_VM_STOP, "stopping Vm with Id: "+vmId);
//        
//        VMOperationParam param = new VMOperationParam(userId, userVm.getAccountId(), vmId, null, eventId);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//    	job.setUserId(UserContext.current().getUserId());
//    	job.setAccountId(userVm.getAccountId());
//        job.setCmd("StopVM");
//        job.setCmdInfo(gson.toJson(param));
//        
//        // use the same result result object name as StartVMCmd
//        job.setCmdOriginator(StartVMCmd.getResultObjectName());
//        return _asyncMgr.submitAsyncJob(job, true);
//    }

//    @Override
//    public boolean rebootVirtualMachine(long userId, long vmId) {
//        return _vmMgr.rebootVirtualMachine(userId, vmId);
//    }

//    @Override
//    public long rebootVirtualMachineAsync(long userId, long vmId) {
//        
//        UserVmVO userVm = _userVmDao.findById(vmId);
//
//        long eventId = EventUtils.saveScheduledEvent(userId, userVm.getAccountId(), EventTypes.EVENT_VM_REBOOT, "rebooting Vm with Id: "+vmId);
//        
//        VMOperationParam param = new VMOperationParam(userId, userVm.getAccountId(), vmId, null, eventId);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//    	job.setUserId(UserContext.current().getUserId());
//    	job.setAccountId(userVm.getAccountId());
//        job.setCmd("RebootVM");
//        job.setCmdInfo(gson.toJson(param));
//        
//        // use the same result result object name as StartVMCmd
//        job.setCmdOriginator(StartVMCmd.getResultObjectName());
//        return _asyncMgr.submitAsyncJob(job, true);
//    }

    @Override
    public boolean destroyVirtualMachine(long userId, long vmId) {
        return _vmMgr.destroyVirtualMachine(userId, vmId);
    }

    @Override
    public long destroyVirtualMachineAsync(long userId, long vmId) {
        
        UserVmVO userVm = _userVmDao.findById(vmId);

        long eventId = EventUtils.saveScheduledEvent(userId, userVm.getAccountId(), EventTypes.EVENT_VM_DESTROY, "destroying Vm with Id: "+vmId);
        VMOperationParam param = new VMOperationParam(userId, userVm.getAccountId(), vmId, null, eventId);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(userVm.getAccountId());
        job.setCmd("DestroyVM");
        job.setCmdInfo(gson.toJson(param));
        return _asyncMgr.submitAsyncJob(job, true);
    }

//    @Override
//    public boolean recoverVirtualMachine(long vmId) throws ResourceAllocationException, InternalErrorException {
//        return _vmMgr.recoverVirtualMachine(vmId);
//    }

    /*
    @Override
    public boolean upgradeVirtualMachine(long userId, long vmId, long serviceOfferingId,long startEventId) {
        UserVmVO userVm = _userVmDao.findById(vmId);
        EventUtils.saveStartedEvent(userId, userVm.getAccountId(), EventTypes.EVENT_VM_UPGRADE, "upgrading service offering on VM : " + userVm.getName(), startEventId);
        boolean success = _vmMgr.upgradeVirtualMachine(vmId, serviceOfferingId);

        String params = "id=" + vmId + "\nvmName=" + userVm.getName() + "\nsoId=" + serviceOfferingId + "\ntId=" + userVm.getTemplateId() + "\ndcId=" + userVm.getDataCenterId();

        if (success) {
        	EventUtils.saveEvent(userId, userVm.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_VM_UPGRADE, "Successfully upgrade service offering on VM : " + userVm.getName(), params, startEventId);
        } else {
        	EventUtils.saveEvent(userId, userVm.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_UPGRADE, "Failed to upgrade service offering on VM : " + userVm.getName(), params, startEventId);
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
        
        long eventId = EventUtils.saveScheduledEvent(userId, vm.getAccountId(), EventTypes.EVENT_VM_UPGRADE, "upgrading Vm with Id: "+vmId);
        param.setEventId(eventId);
        
        AsyncJobVO job = new AsyncJobVO();
    	job.setUserId(UserContext.current().getUserId());
    	job.setAccountId(vm.getAccountId());
        job.setCmd("UpgradeVM");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(UpgradeVMCmd.getResultObjectName());
        
        return _asyncMgr.submitAsyncJob(job, true);
    }
*/
    
//    @Override
//    public StoragePoolVO updateStoragePool(long poolId, String tags) throws IllegalArgumentException {
//    	return _storageMgr.updateStoragePool(poolId, tags);
//    }

    @Override
    public DomainRouter startRouter(long routerId, long startEventId) throws InternalErrorException {
        return _networkMgr.startRouter(routerId, startEventId);
    }

//    @Override
//    public long startRouterAsync(long routerId) {
//        long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_ROUTER_START, "starting Router with Id: "+routerId);
//
//        VMOperationParam param = new VMOperationParam(0, routerId, null, eventId);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//        job.setUserId(UserContext.current().getUserId());
//        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
//        job.setCmd("StartRouter");
//        job.setCmdInfo(gson.toJson(param));
//        job.setCmdOriginator(StartRouterCmd.getResultObjectName());
//        return _asyncMgr.submitAsyncJob(job, true);
//    }

    @Override
    public boolean stopRouter(long routerId, long startEventId) {
        return _networkMgr.stopRouter(routerId, startEventId);
    }

//    @Override
//    public long stopRouterAsync(long routerId) {
//        long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_ROUTER_STOP, "stopping Router with Id: "+routerId);
//        VMOperationParam param = new VMOperationParam(0, routerId, null, eventId);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//        job.setUserId(UserContext.current().getUserId());
//        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
//        job.setCmd("StopRouter");
//        job.setCmdInfo(gson.toJson(param));
//        // use the same result object name as StartRouterCmd
//        job.setCmdOriginator(StartRouterCmd.getResultObjectName());
//        
//        return _asyncMgr.submitAsyncJob(job, true);
//    }

    @Override
    public boolean rebootRouter(long routerId, long startEventId) throws InternalErrorException {
        return _networkMgr.rebootRouter(routerId, startEventId);
    }

//    @Override
//    public long rebootRouterAsync(long routerId) {
//        long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_ROUTER_REBOOT, "rebooting Router with Id: "+routerId);
//        VMOperationParam param = new VMOperationParam(0, routerId, null, eventId);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//        job.setUserId(UserContext.current().getUserId());
//        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
//        job.setCmd("RebootRouter");
//        job.setCmdInfo(gson.toJson(param));
//        // use the same result object name as StartRouterCmd
//        job.setCmdOriginator(StartRouterCmd.getResultObjectName());
//        return _asyncMgr.submitAsyncJob(job, true);
//    }

//    @Override
//    public boolean destroyRouter(long routerId) {
//        return _networkMgr.destroyRouter(routerId);
//    }

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
    
//    public boolean deleteHost(long hostId) {
//        return _agentMgr.deleteHost(hostId);
//    }

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
            EventUtils.saveStartedEvent(userId, userVm.getAccountId(), EventTypes.EVENT_PORT_FORWARDING_SERVICE_APPLY, "Applying port forwarding service for Vm with Id: "+vmId, startEventId);
            
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

                        EventUtils.saveEvent(userId, account.getId(), level, type, description);
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
                        if (existingVMMap.getInstanceId() == userVm.getId()) {
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

                    EventUtils.saveEvent(userId, account.getId(), EventVO.LEVEL_INFO, EventTypes.EVENT_NET_RULE_ADD, description);
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
        long eventId = EventUtils.saveScheduledEvent(userId, userVm.getAccountId(), EventTypes.EVENT_PORT_FORWARDING_SERVICE_APPLY, "applying port forwarding service for Vm with Id: "+vmId);
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
    public void removeSecurityGroup(RemovePortForwardingServiceCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
    	
    	Account account = (Account)UserContext.current().getAccountObject();
        Long userId = UserContext.current().getUserId();
        Long securityGroupId = cmd.getId();
        String publicIp = cmd.getPublicIp();
        Long vmId = cmd.getVirtualMachineId();
        
        //verify input parameters
        SecurityGroupVO securityG = _securityGroupDao.findById(securityGroupId);
        if (securityG == null) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find a port forwarding service with id " + securityGroupId);
        } else if (account != null) {
            if (!isAdmin(account.getType()) && (account.getId().longValue() != securityG.getAccountId())) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find a port forwarding service with id " + securityGroupId + " for this account");
            } else if (!isChildDomain(account.getDomainId(), securityG.getDomainId())) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid port forwarding service id (" + securityGroupId + ") given, unable to remove port forwarding service.");
            }
        }
        
        UserVmVO vmInstance = findUserVMInstanceById(vmId.longValue());
        if (vmInstance == null) {
        	throw new ServerApiException(BaseCmd.VM_INVALID_PARAM_ERROR, "unable to find a virtual machine with id " + vmId);
        }
        if (account != null) {
            if (!isAdmin(account.getType()) && (account.getId().longValue() != vmInstance.getAccountId())) {
                throw new ServerApiException(BaseCmd.VM_INVALID_PARAM_ERROR, "unable to find a virtual machine with id " + vmId + " for this account");
            } else if (!isChildDomain(account.getDomainId(), vmInstance.getDomainId())) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid virtual machine id (" + vmId + ") given, unable to remove port forwarding service.");
            }
        }

        Account ipAddrAccount = findAccountByIpAddress(publicIp);
        if (ipAddrAccount == null) {
            if (account == null) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find ip address " + publicIp);
            } else {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "account " + account.getAccountName() + " doesn't own ip address " + publicIp);
            }
        }

        Long accountId = ipAddrAccount.getId();
        if ((account != null) && !isAdmin(account.getType())) {
            if (account.getId().longValue() != accountId) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "account " + account.getAccountName() + " doesn't own ip address " + publicIp);
            }
        }

        if (userId == null) {
            userId = Long.valueOf(1);
        }

        long eventId = EventUtils.saveScheduledEvent(userId, vmInstance.getAccountId(), EventTypes.EVENT_PORT_FORWARDING_SERVICE_REMOVE, "removing security groups for Vm with Id: "+vmId);

        /*TODO : ASK KRIS AS TO WHAT DO WE DO WITH THIS PART IN THE EXECUTOR CODE
        UserVmVO userVm = userVmDao.findById(param.getInstanceId());
        if(userVm == null)
        	return null;
        
        if (userVm.getDomainRouterId() == null) {
        	return null;
        } else
        	return routerDao.findById(userVm.getDomainRouterId());
	    */
        removeSecurityGroup(userId, securityGroupId, publicIp, vmId, eventId);
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
        EventUtils.saveStartedEvent(userId, userVm.getAccountId(), EventTypes.EVENT_PORT_FORWARDING_SERVICE_REMOVE, "Removing security groups for Vm with Id: "+vmId, startEventId);
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

                    EventUtils.saveEvent(userId, account.getId(), level, type, description);
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

                EventUtils.saveEvent(userId, account.getId(), EventVO.LEVEL_INFO, EventTypes.EVENT_NET_RULE_ADD, description);
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
        long eventId = EventUtils.saveScheduledEvent(userId, userVm.getAccountId(), EventTypes.EVENT_PORT_FORWARDING_SERVICE_REMOVE, "removing security groups for Vm with Id: "+vmId);
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
        Map<String, Pair<String, String>> mappedPublicPorts = new HashMap<String, Pair<String, String>>();

        if (existingRulesOnPubIp != null) {
            for (FirewallRuleVO fwRule : existingRulesOnPubIp) {
                mappedPublicPorts.put(fwRule.getPublicPort(), new Pair<String, String>(fwRule.getPrivateIpAddress(), fwRule.getPrivatePort()));
            }
        }

        Pair<String, String> privateIpPort = mappedPublicPorts.get(publicPort);
        if (privateIpPort != null) {
            if (privateIpPort.first().equals(userVm.getGuestIpAddress()) && privateIpPort.second().equals(privatePort)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("skipping the creating of firewall rule " + ipAddress + ":" + publicPort + " to " + userVm.getGuestIpAddress() + ":" + privatePort + "; rule already exists.");
                }
                return null; // already mapped
            } else {
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

        EventUtils.saveEvent(Long.valueOf(userId), account.getId(), level, EventTypes.EVENT_NET_RULE_ADD, description);

        return newFwRule;
    }

//    @Override
//    public boolean deleteNetworkRuleConfig(long userId, long networkRuleId) {
//        try {
//            NetworkRuleConfigVO netRule = _networkRuleConfigDao.findById(networkRuleId);
//            if (netRule != null) {
//                List<SecurityGroupVMMapVO> sgMappings = _securityGroupVMMapDao.listBySecurityGroup(netRule.getSecurityGroupId());
//                if ((sgMappings != null) && !sgMappings.isEmpty()) {
//                    for (SecurityGroupVMMapVO sgMapping : sgMappings) {
//                        UserVm userVm = _userVmDao.findById(sgMapping.getInstanceId());
//                        if (userVm != null) {
//                            List<FirewallRuleVO> fwRules = _firewallRulesDao.listIPForwarding(sgMapping.getIpAddress(), netRule.getPublicPort(), true);
//                            FirewallRuleVO rule = null;
//                            for (FirewallRuleVO fwRule : fwRules) {
//                                if (fwRule.getPrivatePort().equals(netRule.getPrivatePort()) && fwRule.getPrivateIpAddress().equals(userVm.getGuestIpAddress())) {
//                                    rule = fwRule;
//                                    break;
//                                }
//                            }
//
//                            if (rule != null) {
//                                rule.setEnabled(false);
//                                _networkMgr.updateFirewallRule(rule, null, null);
//
//                                // Save and create the event
//                                Account account = _accountDao.findById(userVm.getAccountId());
//
//                                _firewallRulesDao.remove(rule.getId());
//                                String description = "deleted ip forwarding rule [" + rule.getPublicIpAddress() + ":" + rule.getPublicPort() + "]->[" + rule.getPrivateIpAddress()
//                                                     + ":" + rule.getPrivatePort() + "]" + " " + rule.getProtocol();
//
//                                EventUtils.saveEvent(Long.valueOf(userId), account.getId(), EventVO.LEVEL_INFO, EventTypes.EVENT_NET_RULE_DELETE, description);
//                            }
//                        }
//                    }
//                }
//                _networkRuleConfigDao.remove(netRule.getId());
//            }
//        } catch (Exception ex) {
//            s_logger.error("Unexpected exception deleting port forwarding service rule " + networkRuleId, ex);
//            return false;
//        }
//
//        return true;
//    }

//    @Override
//    public long deleteNetworkRuleConfigAsync(long userId, Account account, Long networkRuleId) throws PermissionDeniedException {
//        // do a quick permissions check to make sure the account is either an
//        // admin or the owner of the security group to which the network rule
//        // belongs
//        NetworkRuleConfigVO netRule = _networkRuleConfigDao.findById(networkRuleId);
//        long accountId = Account.ACCOUNT_ID_SYSTEM;
//        if (netRule != null) {
//            SecurityGroupVO sg = _securityGroupDao.findById(netRule.getSecurityGroupId());
//            if (account != null) {
//                if (!BaseCmd.isAdmin(account.getType())) {
//                    if ((sg.getAccountId() == null) || (sg.getAccountId().longValue() != account.getId().longValue())) {
//                        throw new PermissionDeniedException("Unable to delete port forwarding service rule " + networkRuleId + "; account: " + account.getAccountName() + " is not the owner");
//                    }
//                } else if (!isChildDomain(account.getDomainId(), sg.getDomainId())) {
//                    throw new PermissionDeniedException("Unable to delete port forwarding service rule " + networkRuleId + "; account: " + account.getAccountName() + " is not an admin in the domain hierarchy.");
//                }
//            }
//            if (sg != null) {
//                accountId = sg.getAccountId().longValue();
//            }
//        } else {
//            return 0L;  // failed to delete due to netRule not found
//        }
//
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//    	job.setUserId(UserContext.current().getUserId());
//    	job.setAccountId(accountId);
//        job.setCmd("DeleteNetworkRuleConfig");
//        job.setCmdInfo(gson.toJson(networkRuleId));
//        
//        return _asyncMgr.submitAsyncJob(job);
//    }

    @DB
    protected boolean deleteIpForwardingRule(long userId, long accountId, String publicIp, String publicPort, String privateIp, String privatePort, String proto)
            throws PermissionDeniedException, InvalidParameterValueException, InternalErrorException {

        Transaction txn = Transaction.currentTxn();
        boolean locked = false;
        try {
            AccountVO accountVO = _accountDao.findById(accountId);
            if (accountVO == null) {
                // throw this exception because hackers can use the api to probe
                // for existing user ids
                throw new PermissionDeniedException("Account does not own supplied address");
            }
            // although we are not writing these values to the DB, we will check
            // them out of an abundance
            // of caution (may not be warranted)
            if (!NetUtils.isValidPort(publicPort) || !NetUtils.isValidPort(privatePort)) {
                throw new InvalidParameterValueException("Invalid value for port");
            }
//            if (!NetUtils.isValidPrivateIp(privateIp, _configs.get("guest.ip.network"))) {
//                throw new InvalidParameterValueException("Invalid private ip address");
//            }
            if (!NetUtils.isValidProto(proto)) {
                throw new InvalidParameterValueException("Invalid protocol");
            }
            IPAddressVO ipVO = _publicIpAddressDao.acquire(publicIp);
            if (ipVO == null) {
                // throw this exception because hackers can use the api to probe for allocated ips
                throw new PermissionDeniedException("User does not own supplied address");
            }

            locked = true;
            if ((ipVO.getAllocated() == null) || (ipVO.getAccountId() == null) || (ipVO.getAccountId().longValue() != accountId)) {
                // FIXME: if admin account, make sure the user is visible in the
                // admin's domain, or has that checking been done by this point?
                if (!BaseCmd.isAdmin(accountVO.getType())) {
                    throw new PermissionDeniedException("User/account does not own supplied address");
                }
            }

            txn.start();

            List<FirewallRuleVO> fwdings = _firewallRulesDao.listIPForwardingForUpdate(publicIp, publicPort, proto);
            FirewallRuleVO fwRule = null;
            if (fwdings.size() == 0) {
                throw new InvalidParameterValueException("No such rule");
            } else if (fwdings.size() == 1) {
                fwRule = fwdings.get(0);
                if (fwRule.getPrivateIpAddress().equalsIgnoreCase(privateIp) && fwRule.getPrivatePort().equals(privatePort)) {
                    _firewallRulesDao.delete(fwRule.getId());
                } else {
                    throw new InvalidParameterValueException("No such rule");
                }
            } else {
                throw new InternalErrorException("Multiple matches. Please contact support");
            }
            fwRule.setEnabled(false);
            boolean success = _networkMgr.updateFirewallRule(fwRule, null, null);
            if (!success) {
                throw new InternalErrorException("Failed to update router");
            }
            txn.commit();
            return success;
        } catch (Throwable e) {
            if (e instanceof InvalidParameterValueException) {
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
        return false;
    }

    @DB
    private boolean deleteLoadBalancingRule(long userId, long accountId, String publicIp, String publicPort, String privateIp, String privatePort, String algo)
            throws PermissionDeniedException, InvalidParameterValueException, InternalErrorException {
        Transaction txn = Transaction.currentTxn();
        boolean locked = false;
        try {
            AccountVO accountVO = _accountDao.findById(accountId);
            if (accountVO == null) {
                // throw this exception because hackers can use the api to probe
                // for existing user ids
                throw new PermissionDeniedException("Account does not own supplied address");
            }
            // although we are not writing these values to the DB, we will check
            // them out of an abundance
            // of caution (may not be warranted)
            if (!NetUtils.isValidPort(publicPort) || !NetUtils.isValidPort(privatePort)) {
                throw new InvalidParameterValueException("Invalid value for port");
            }
//            if (!NetUtils.isValidPrivateIp(privateIp, _configs.get("guest.ip.network"))) {
//                throw new InvalidParameterValueException("Invalid private ip address");
//            }
            if (!NetUtils.isValidAlgorithm(algo)) {
                throw new InvalidParameterValueException("Invalid protocol");
            }

            IPAddressVO ipVO = _publicIpAddressDao.acquire(publicIp);

            if (ipVO == null) {
                // throw this exception because hackers can use the api to probe
                // for allocated ips
                throw new PermissionDeniedException("User does not own supplied address");
            }

            locked = true;
            if ((ipVO.getAllocated() == null) || (ipVO.getAccountId() == null) || (ipVO.getAccountId().longValue() != accountId)) {
                // FIXME: the user visible from the admin account's domain? has
                // that check been done already?
                if (!BaseCmd.isAdmin(accountVO.getType())) {
                    throw new PermissionDeniedException("User does not own supplied address");
                }
            }

            List<FirewallRuleVO> fwdings = _firewallRulesDao.listLoadBalanceRulesForUpdate(publicIp, publicPort, algo);
            FirewallRuleVO fwRule = null;
            if (fwdings.size() == 0) {
                throw new InvalidParameterValueException("No such rule");
            }
            for (FirewallRuleVO frv : fwdings) {
                if (frv.getPrivateIpAddress().equalsIgnoreCase(privateIp) && frv.getPrivatePort().equals(privatePort)) {
                    fwRule = frv;
                    break;
                }
            }

            if (fwRule == null) {
                throw new InvalidParameterValueException("No such rule");
            }

            txn.start();

            fwRule.setEnabled(false);
            _firewallRulesDao.update(fwRule.getId(), fwRule);

            boolean success = _networkMgr.updateFirewallRule(fwRule, null, null);
            if (!success) {
                throw new InternalErrorException("Failed to update router");
            }
            _firewallRulesDao.delete(fwRule.getId());

            txn.commit();
            return success;
        } catch (Throwable e) {
            if (e instanceof InvalidParameterValueException) {
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
        return false;
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
            Account account = (Account)UserContext.current().getAccountObject();

            UserVmVO vmInstance = _userVmDao.findById(vmId);
            if ((vmInstance == null) || (vmInstance.getRemoved() != null)) {
                throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
            }
            if ((account != null) && !isAdmin(account.getType())) {
                if (account.getId().longValue() != vmInstance.getAccountId()) {
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

        return _offeringsDao.search(sc, searchFilter);
    }

    @Override
    public List<ClusterVO> searchForClusters(ListClustersCmd cmd) {
        Filter searchFilter = new Filter(ClusterVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<ClusterVO> sc = _clusterDao.createSearchCriteria();

        Object id = cmd.getId();
        Object name = cmd.getName();
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
        Filter searchFilter = new Filter(HostVO.class, "id", Boolean.TRUE, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<HostVO> sc = _hostDao.createSearchCriteria();

        Object name = cmd.getHostName();
        Object type = cmd.getType();
        Object state = cmd.getState();
        Object zone = cmd.getZoneId();
        Object pod = cmd.getPodId();
        Object cluster = cmd.getClusterId();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();

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
        Filter searchFilter = new Filter(HostPodVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
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
    public Long getAccountIdForVlan(long vlanDbId) {
    	List<AccountVlanMapVO> accountVlanMaps = _accountVlanMapDao.listAccountVlanMapsByVlan(vlanDbId);
    	if (accountVlanMaps.isEmpty()) {
    		return null;
    	} else {
    		return accountVlanMaps.get(0).getAccountId();
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
        Account account = (Account)UserContext.current().getAccountObject();
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

        return listTemplates(cmd.getId(), cmd.getIsoName(), cmd.getKeyword(), isoFilter, true, cmd.isBootable(), accountId, cmd.getPageSizeVal().intValue(), cmd.getStartIndex(), cmd.getZoneId());
    }

    @Override
    public List<VMTemplateVO> listTemplates(ListTemplatesCmd cmd) throws IllegalArgumentException, InvalidParameterValueException {
        TemplateFilter templateFilter = TemplateFilter.valueOf(cmd.getTemplateFilter());
        Long accountId = null;
        Account account = (Account)UserContext.current().getAccountObject();
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

        return listTemplates(cmd.getId(), cmd.getTemplateName(), cmd.getKeyword(), templateFilter, false, null, accountId, cmd.getPageSizeVal().intValue(), cmd.getStartIndex(), cmd.getZoneId());
    }

    private List<VMTemplateVO> listTemplates(Long templateId, String name, String keyword, TemplateFilter templateFilter, boolean isIso, Boolean bootable, Long accountId, Integer pageSize, Long startIndex, Long zoneId) throws InvalidParameterValueException {
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
    		return _templateHostDao.listByOnlyTemplateId(templateId);
    	}
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
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("needsCleanup", sb.entity().getNeedsCleanup(), SearchCriteria.Op.EQ);

        if ((id == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId());
        }

        SearchCriteria<AccountVO> sc = sb.create();
        if (keyword != null) {
            SearchCriteria<AccountVO> ssc = _accountDao.createSearchCriteria();
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

//    @Override
//    public ResourceLimitVO updateResourceLimit(Long domainId, Long accountId, ResourceType type, Long max) throws InvalidParameterValueException {
//        return _accountMgr.updateResourceLimit(domainId, accountId, type, max);
//    }

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

//    @Override
//    public Long createTemplate(long userId, Long zoneId, String name, String displayText, boolean isPublic, boolean featured, String format, String diskType, String url, String chksum, boolean requiresHvm, int bits, boolean enablePassword, long guestOSId, boolean bootable) throws InvalidParameterValueException,IllegalArgumentException, ResourceAllocationException {
//        try
//        {
//            if (name.length() > 32)
//            {
//                throw new InvalidParameterValueException("Template name should be less than 32 characters");
//            }
//            	
//            if (!name.matches("^[\\p{Alnum} ._-]+")) {
//                throw new InvalidParameterValueException("Only alphanumeric, space, dot, dashes and underscore characters allowed");
//            }
//        	
//            ImageFormat imgfmt = ImageFormat.valueOf(format.toUpperCase());
//            if (imgfmt == null) {
//                throw new IllegalArgumentException("Image format is incorrect " + format + ". Supported formats are " + EnumUtils.listValues(ImageFormat.values()));
//            }
//            
//            FileSystem fileSystem = FileSystem.valueOf(diskType);
//            if (fileSystem == null) {
//                throw new IllegalArgumentException("File system is incorrect " + diskType + ". Supported file systems are " + EnumUtils.listValues(FileSystem.values()));
//            }
//            
//            URI uri = new URI(url);
//            if ((uri.getScheme() == null) || (!uri.getScheme().equalsIgnoreCase("http") && !uri.getScheme().equalsIgnoreCase("https") && !uri.getScheme().equalsIgnoreCase("file"))) {
//               throw new IllegalArgumentException("Unsupported scheme for url: " + url);
//            }
//            int port = uri.getPort();
//            if (!(port == 80 || port == 443 || port == -1)) {
//            	throw new IllegalArgumentException("Only ports 80 and 443 are allowed");
//            }
//            String host = uri.getHost();
//            try {
//            	InetAddress hostAddr = InetAddress.getByName(host);
//            	if (hostAddr.isAnyLocalAddress() || hostAddr.isLinkLocalAddress() || hostAddr.isLoopbackAddress() || hostAddr.isMulticastAddress() ) {
//            		throw new IllegalArgumentException("Illegal host specified in url");
//            	}
//            	if (hostAddr instanceof Inet6Address) {
//            		throw new IllegalArgumentException("IPV6 addresses not supported (" + hostAddr.getHostAddress() + ")");
//            	}
//            } catch (UnknownHostException uhe) {
//            	throw new IllegalArgumentException("Unable to resolve " + host);
//            }
//            
//            // Check that the resource limit for templates/ISOs won't be exceeded
//            UserVO user = _userDao.findById(userId);
//            if (user == null) {
//                throw new IllegalArgumentException("Unable to find user with id " + userId);
//            }
//        	AccountVO account = _accountDao.findById(user.getAccountId());
//            if (_accountMgr.resourceLimitExceeded(account, ResourceType.template)) {
//            	ResourceAllocationException rae = new ResourceAllocationException("Maximum number of templates and ISOs for account: " + account.getAccountName() + " has been exceeded.");
//            	rae.setResourceType("template");
//            	throw rae;
//            }
//            
//            // If a zoneId is specified, make sure it is valid
//            if (zoneId != null) {
//            	if (_dcDao.findById(zoneId) == null) {
//            		throw new IllegalArgumentException("Please specify a valid zone.");
//            	}
//            }
//            VMTemplateVO systemvmTmplt = _templateDao.findRoutingTemplate();
//            if (systemvmTmplt.getName().equalsIgnoreCase(name) || systemvmTmplt.getDisplayText().equalsIgnoreCase(displayText)) {
//            	throw new IllegalArgumentException("Cannot use reserved names for templates");
//            }
//            
//            return _tmpltMgr.create(userId, zoneId, name, displayText, isPublic, featured, imgfmt, fileSystem, uri, chksum, requiresHvm, bits, enablePassword, guestOSId, bootable);
//        } catch (URISyntaxException e) {
//            throw new IllegalArgumentException("Invalid URL " + url);
//        }
//    }

    @Override
    public boolean updateTemplate(UpdateTemplateOrIsoCmd cmd) throws InvalidParameterValueException {
    	Long id = cmd.getId();
    	String name = cmd.getName();
    	String displayText = cmd.getDisplayText();
    	String format = cmd.getFormat();
    	Long guestOSId = cmd.getOsTypeId();
    	Boolean passwordEnabled = cmd.isPasswordEnabled();
    	Boolean bootable = cmd.isBootable();
    	Account account= (Account)UserContext.current().getAccountObject();
    	
    	//verify that template exists
    	VMTemplateVO template = findTemplateById(id);
    	if (template == null) {
    		throw new InvalidParameterValueException("unable to find template with id " + id);
    	}
    	
        //Don't allow to modify system template
        if (id == Long.valueOf(1)) {
        	throw new InvalidParameterValueException("Unable to update template with id " + id);
        }
    	
    	//do a permission check
        if (account != null) {
            Long templateOwner = template.getAccountId();
            if (!BaseCmd.isAdmin(account.getType())) {
                if ((templateOwner == null) || (account.getId().longValue() != templateOwner.longValue())) {
                    throw new InvalidParameterValueException("Unable to modify template with id " + id);
                }
            } else if (account.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                Long templateOwnerDomainId = findDomainIdByAccountId(templateOwner);
                if (!isChildDomain(account.getDomainId(), templateOwnerDomainId)) {
                    throw new InvalidParameterValueException("Unable to modify template with id " + id);
                }
            }
        }
        

    	boolean updateNeeded = !(name == null && displayText == null && format == null && guestOSId == null && passwordEnabled == null && bootable == null);
    	if (!updateNeeded) {
    		return true;
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
    	
        return _templateDao.update(id, template);
    }

//    @Override
//    public boolean deleteTemplate(long userId, long templateId, Long zoneId, long startEventId) throws InternalErrorException {
//    	return _tmpltMgr.delete(userId, templateId, zoneId, startEventId);
//    }
    
//    @Override
//    public long deleteTemplateAsync(long userId, long templateId, Long zoneId) throws InvalidParameterValueException {
//    	UserVO user = _userDao.findById(userId);
//    	if (user == null) {
//    		throw new InvalidParameterValueException("Please specify a valid user.");
//    	}
//    	
//    	VMTemplateVO template = _templateDao.findById(templateId);
//    	if (template == null) {
//    		throw new InvalidParameterValueException("Please specify a valid template.");
//    	}
//    	
//    	if (template.getFormat() == ImageFormat.ISO) {
//    		throw new InvalidParameterValueException("Please specify a valid template.");
//    	}
//    	
//    	if (template.getUniqueName().equals("routing")) {
//    		throw new InvalidParameterValueException("The DomR template cannot be deleted.");
//    	}
//    	
//    	if (zoneId != null && (_hostDao.findSecondaryStorageHost(zoneId) == null)) {
//    		throw new InvalidParameterValueException("Failed to find a secondary storage host in the specified zone.");
//    	}
//    	
//        DeleteTemplateParam param = new DeleteTemplateParam(userId, templateId, zoneId, 0);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//    	job.setUserId(UserContext.current().getUserId());
//    	job.setAccountId(template.getAccountId());
//        job.setCmd("DeleteTemplate");
//        job.setCmdInfo(gson.toJson(param));
//        job.setCmdOriginator(DeleteTemplateCmd.getStaticName());
//        
//        return _asyncMgr.submitAsyncJob(job);
//    }
    
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
//    
//    @Override
//    public long copyTemplateAsync(long userId, long templateId, long sourceZoneId, long destZoneId) throws InvalidParameterValueException {
//    	UserVO user = _userDao.findById(userId);
//    	if (user == null) {
//    		throw new InvalidParameterValueException("Please specify a valid user.");
//    	}
//    	
//    	VMTemplateVO template = _templateDao.findById(templateId);
//    	if (template == null) {
//    		throw new InvalidParameterValueException("Please specify a valid template/ISO.");
//    	}
//    	
//    	if (template.getFormat().equals(ImageFormat.ISO) && template.getName().equals("xs-tools.iso")) {
//    		throw new InvalidParameterValueException("The XenServer Tools ISO cannot be copied.");
//    	}
//    	
//    	DataCenterVO sourceZone = _dcDao.findById(sourceZoneId);
//    	if (sourceZone == null) {
//    		throw new InvalidParameterValueException("Please specify a valid source zone.");
//    	}
//    	
//    	DataCenterVO destZone = _dcDao.findById(destZoneId);
//    	if (destZone == null) {
//    		throw new InvalidParameterValueException("Please specify a valid destination zone.");
//    	}
//    	
//    	if (sourceZoneId == destZoneId) {
//    		throw new InvalidParameterValueException("Please specify different source and destination zones.");
//    	}
//    	
//    	HostVO srcSecHost = _hostDao.findSecondaryStorageHost(sourceZoneId);
//    	
//    	if (srcSecHost == null) {
//    		throw new InvalidParameterValueException("Source zone is not ready");
//    	}
//    	
//    	if (_hostDao.findSecondaryStorageHost(destZoneId) == null) {
//    		throw new InvalidParameterValueException("Destination zone is not ready.");
//    	}
//    	
//       	VMTemplateHostVO srcTmpltHost = null;
//        srcTmpltHost = _templateHostDao.findByHostTemplate(srcSecHost.getId(), templateId);
//        if (srcTmpltHost == null || srcTmpltHost.getDestroyed() || srcTmpltHost.getDownloadState() != VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
//        	throw new InvalidParameterValueException("Please specify a template that is installed on secondary storage host: " + srcSecHost.getName());
//        }
//        
//        long eventId = EventUtils.saveScheduledEvent(userId, template.getAccountId(), EventTypes.EVENT_TEMPLATE_COPY, "copying template with Id: "+templateId+" from zone: "+sourceZoneId+" to: "+destZoneId);
//        
//        CopyTemplateParam param = new CopyTemplateParam(userId, templateId, sourceZoneId, destZoneId, eventId);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//    	job.setUserId(UserContext.current().getUserId());
//    	job.setAccountId(template.getAccountId());
//        job.setCmd("CopyTemplate");
//        job.setCmdInfo(gson.toJson(param));
//        job.setCmdOriginator(CopyTemplateCmd.getStaticName());
//        
//        return _asyncMgr.submitAsyncJob(job);
//    }
    
//    @Override
//    public long deleteIsoAsync(long userId, long isoId, Long zoneId) throws InvalidParameterValueException {
//    	UserVO user = _userDao.findById(userId);
//    	if (user == null) {
//    		throw new InvalidParameterValueException("Please specify a valid user.");
//    	}
//    	
//    	VMTemplateVO iso = _templateDao.findById(isoId);
//    	if (iso == null) {
//    		throw new InvalidParameterValueException("Please specify a valid ISO.");
//    	}
//    	
//    	if (iso.getFormat() != ImageFormat.ISO) {
//    		throw new InvalidParameterValueException("Please specify a valid ISO.");
//    	}
//    	
//    	if (zoneId != null && (_hostDao.findSecondaryStorageHost(zoneId) == null)) {
//    		throw new InvalidParameterValueException("Failed to find a secondary storage host in the specified zone.");
//    	}
//    	
//    	DeleteTemplateParam param = new DeleteTemplateParam(userId, isoId, zoneId, 0);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//    	job.setUserId(UserContext.current().getUserId());
//    	job.setAccountId(iso.getAccountId());
//        job.setCmd("DeleteTemplate");
//        job.setCmdInfo(gson.toJson(param));
//        job.setCmdOriginator(DeleteIsoCmd.getStaticName());
//        
//        return _asyncMgr.submitAsyncJob(job);
//    }

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
        SearchCriteria<UserVmVO> sc = sb.create();

        if (keyword != null) {
            SearchCriteria<UserVmVO> ssc = _userVmDao.createSearchCriteria();
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

//    @Override @DB
//    public LoadBalancerVO updateLoadBalancerRule(long userId, LoadBalancerVO loadBalancer, String privatePort, String algorithm) {
//        String updatedPrivatePort = ((privatePort == null) ? loadBalancer.getPrivatePort() : privatePort);
//        String updatedAlgorithm = ((algorithm == null) ? loadBalancer.getAlgorithm() : algorithm);
//
//        Transaction txn = Transaction.currentTxn();
//        try {
//            txn.start();
//            loadBalancer.setPrivatePort(updatedPrivatePort);
//            loadBalancer.setAlgorithm(updatedAlgorithm);
//            _loadBalancerDao.update(loadBalancer.getId(), loadBalancer);
//
//            List<FirewallRuleVO> fwRules = _firewallRulesDao.listByLoadBalancerId(loadBalancer.getId());
//            if ((fwRules != null) && !fwRules.isEmpty()) {
//                for (FirewallRuleVO fwRule : fwRules) {
//                    fwRule.setPrivatePort(updatedPrivatePort);
//                    fwRule.setAlgorithm(updatedAlgorithm);
//                    _firewallRulesDao.update(fwRule.getId(), fwRule);
//                }
//            }
//            txn.commit();
//        } catch (RuntimeException ex) {
//            s_logger.warn("Unhandled exception trying to update load balancer rule", ex);
//            txn.rollback();
//            throw ex;
//        } finally {
//            txn.close();
//        }
//
//        // now that the load balancer has been updated, reconfigure the HA Proxy on the router with all the LB rules 
//        List<FirewallRuleVO> allLbRules = new ArrayList<FirewallRuleVO>();
//        IPAddressVO ipAddress = _publicIpAddressDao.findById(loadBalancer.getIpAddress());
//        List<IPAddressVO> ipAddrs = _networkMgr.listPublicIpAddressesInVirtualNetwork(loadBalancer.getAccountId(), ipAddress.getDataCenterId(), null);
//        for (IPAddressVO ipv : ipAddrs) {
//            List<FirewallRuleVO> rules = _firewallRulesDao.listIPForwarding(ipv.getAddress(), false);
//            allLbRules.addAll(rules);
//        }
//
//        IPAddressVO ip = _publicIpAddressDao.findById(loadBalancer.getIpAddress());
//        DomainRouterVO router = _routerDao.findBy(ip.getAccountId(), ip.getDataCenterId());
//        _networkMgr.updateFirewallRules(loadBalancer.getIpAddress(), allLbRules, router);
//        return _loadBalancerDao.findById(loadBalancer.getId());
//    }

//    @Override
//    public LoadBalancerVO updateLoadBalancerRule(LoadBalancerVO loadBalancer, String name, String description) throws InvalidParameterValueException {
//        if ((name == null) && (description == null)) {
//            return loadBalancer; // nothing to do
//        }
//
//        LoadBalancerVO lbForUpdate = _loadBalancerDao.createForUpdate();
//        // make sure the name's not already in use
//        if (name != null) {
//            LoadBalancerVO existingLB = _loadBalancerDao.findByAccountAndName(loadBalancer.getAccountId(), name);
//            if ((existingLB != null) && (existingLB.getId().longValue() != loadBalancer.getId().longValue())) {
//                throw new InvalidParameterValueException("Unable to update load balancer " + loadBalancer.getName() + " with new name " + name + ", the name is already in use.");
//            }
//            lbForUpdate.setName(name);
//        }
//
//        if (description != null) {
//            lbForUpdate.setDescription(description);
//        }
//        _loadBalancerDao.update(loadBalancer.getId(), lbForUpdate);
//        return _loadBalancerDao.findById(loadBalancer.getId());
//    }

//    @Override
//    public long updateLoadBalancerRuleAsync(long userId, long accountId, long loadBalancerId, String name, String description, String privatePort, String algorithm) {
//        UpdateLoadBalancerParam param = new UpdateLoadBalancerParam(userId, loadBalancerId, name, description, privatePort, algorithm);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//        job.setUserId(UserContext.current().getUserId());
//        job.setAccountId(accountId);
//        job.setCmd("UpdateLoadBalancerRule");
//        job.setCmdInfo(gson.toJson(param));
//        job.setCmdOriginator("loadbalancer");
//
//        return _asyncMgr.submitAsyncJob(job);
//    }

    @Override
    public FirewallRuleVO findForwardingRuleById(Long ruleId) {
        return _firewallRulesDao.findById(ruleId);
    }

    @Override
    public IPAddressVO findIPAddressById(String ipAddress) {
        return _publicIpAddressDao.findById(ipAddress);
    }

    @Override
    public List<NetworkRuleConfigVO> searchForNetworkRules(ListPortForwardingServiceRulesCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Long accountId = null;
        Account account = (Account)UserContext.current().getAccountObject();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long groupId = cmd.getPortForwardingServiceId();

        if ((account == null) || isAdmin(account.getType())) {
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Unable to list port forwarding service rules for domain " + domainId + ", permission denied.");
                }
                if (accountName != null) {
                    Account userAcct = _accountDao.findActiveAccount(accountName, domainId);
                    if (userAcct != null) {
                        accountId = userAcct.getId();
                    } else {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                }
            }
        } else {
            accountId = account.getId();
        }

        if ((groupId != null) && (accountId != null)) {
            SecurityGroupVO sg = _securityGroupDao.findById(groupId);
            if (sg != null) {
                if ((sg.getAccountId() != null) && sg.getAccountId().longValue() != accountId.longValue()) {
                    throw new PermissionDeniedException("Unable to list port forwarding service rules, account " + accountId + " does not own port forwarding service " + groupId);
                }
            } else {
                throw new InvalidParameterValueException("Unable to find port forwarding service with id " + groupId);
            }
        }

        Filter searchFilter = new Filter(NetworkRuleConfigVO.class, "id", true, null, null);

        // search by rule id is also supported
        Object id = cmd.getId();

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

        SearchCriteria<NetworkRuleConfigVO> sc = sb.create();

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
    public List<EventVO> searchForEvents(ListEventsCmd cmd) throws PermissionDeniedException, InvalidParameterValueException {
        Account account = (Account)UserContext.current().getAccountObject();
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
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId());
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
    public List<DomainRouterVO> searchForRouters(ListRoutersCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;
        Account account = (Account)UserContext.current().getAccountObject();

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
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("hostId", sb.entity().getHostId(), SearchCriteria.Op.EQ);

        if ((accountId == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId());
        }

        SearchCriteria<DomainRouterVO> sc = sb.create();
        if (keyword != null) {
            SearchCriteria<DomainRouterVO> ssc = _routerDao.createSearchCriteria();
            ssc.addOr("displayName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
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
    public List<IPAddressVO> searchForIPAddresses(ListPublicIpAddressesCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Account account = (Account)UserContext.current().getAccountObject();
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

        SearchCriteria<IPAddressVO> sc = sb.create();
        if (accountId != null) {
            sc.setParameters("accountIdEQ", accountId);
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
        EventUtils.saveEvent(userId, userAcct.getAccountId(), EventTypes.EVENT_USER_LOGOUT, "user has logged out");
    }

    @Override
    public NetworkRuleConfigVO createPortForwardingServiceRule(CreatePortForwardingServiceRuleCmd cmd) throws InvalidParameterValueException, PermissionDeniedException, NetworkRuleConflictException, InternalErrorException {
        NetworkRuleConfigVO rule = null;
        try {
            Long securityGroupId = cmd.getPortForwardingServiceId();
            String port = cmd.getPublicPort();
            String privatePort = cmd.getPrivatePort();
            String protocol = cmd.getProtocol();
            Long userId = UserContext.current().getUserId();
            if (userId == null) {
                userId = Long.valueOf(User.UID_SYSTEM);
            }

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
            } else {
                protocol = "TCP";
            }

            // validate permissions
            Account account = (Account)UserContext.current().getAccountObject();
            if (account != null) {
                if (isAdmin(account.getType())) {
                    if (!_domainDao.isChildDomain(account.getDomainId(), sg.getDomainId())) {
                        throw new PermissionDeniedException("Unable to find rules for port forwarding service id = " + securityGroupId + ", permission denied.");
                    }
                } else if (account.getId().longValue() != sg.getAccountId().longValue()) {
                    throw new PermissionDeniedException("Invalid port forwarding service (" + securityGroupId + ") given, unable to create rule.");
                }
            }

            List<NetworkRuleConfigVO> existingRules = _networkRuleConfigDao.listBySecurityGroupId(securityGroupId);
            for (NetworkRuleConfigVO existingRule : existingRules) {
                if (existingRule.getPublicPort().equals(port) && existingRule.getProtocol().equals(protocol)) {
                    throw new NetworkRuleConflictException("port conflict, port forwarding service contains a rule on public port " + port + " for protocol " + protocol);
                }
            }

            NetworkRuleConfigVO netRule = new NetworkRuleConfigVO(securityGroupId, port, privatePort, protocol);
            netRule.setCreateStatus(AsyncInstanceCreateStatus.Creating);
            rule = _networkRuleConfigDao.persist(netRule);

            /* FIXME:  async job needs to be hooked up...
            // check if we are within context of async-execution
            AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
            if (asyncExecutor != null) {
                AsyncJobVO job = asyncExecutor.getJob();

                if (s_logger.isInfoEnabled())
                    s_logger.info("Created a new port forwarding service rule instance " + rule.getId() + ", update async job-" + job.getId() + " progress status");

                _asyncMgr.updateAsyncJobAttachment(job.getId(), "network_rule_config", rule.getId());
                _asyncMgr.updateAsyncJobStatus(job.getId(), BaseCmd.PROGRESS_INSTANCE_CREATED, rule.getId());
            }
            */

//            rule = createNetworkRuleConfig(userId, securityGroupId, port, privatePort, protocol);
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
    public NetworkRuleConfigVO applyPortForwardingServiceRule(Long ruleId) throws NetworkRuleConflictException {
        NetworkRuleConfigVO netRule = null;
        if (ruleId != null) {
            Long userId = UserContext.current().getUserId();
            if (userId == null) {
                userId = User.UID_SYSTEM;
            }

            netRule = _networkRuleConfigDao.findById(ruleId);
            List<SecurityGroupVMMapVO> sgMappings = _securityGroupVMMapDao.listBySecurityGroup(netRule.getSecurityGroupId());
            if ((sgMappings != null) && !sgMappings.isEmpty()) {
                try {
                    for (SecurityGroupVMMapVO sgMapping : sgMappings) {
                        UserVm userVm = _userVmDao.findById(sgMapping.getInstanceId());
                        createFirewallRule(userId, sgMapping.getIpAddress(), userVm, netRule.getPublicPort(), netRule.getPrivatePort(), netRule.getProtocol(), netRule.getSecurityGroupId());
                    }
                } catch (NetworkRuleConflictException ex) {
                    netRule.setCreateStatus(AsyncInstanceCreateStatus.Corrupted);
                    _networkRuleConfigDao.update(ruleId, netRule);
                    throw ex;
                }
            }

            netRule.setCreateStatus(AsyncInstanceCreateStatus.Created);
            _networkRuleConfigDao.update(ruleId, netRule);
        }

        return netRule;
    }

    public void deleteRule(long ruleId, long userId, long accountId) throws InvalidParameterValueException, PermissionDeniedException, InternalErrorException {
        Exception e = null;
        try {
            FirewallRuleVO rule = _firewallRulesDao.findById(ruleId);
            if (rule != null) {
                boolean success = false;

                try {
                    if (rule.isForwarding()) {
                        success = deleteIpForwardingRule(userId, accountId, rule.getPublicIpAddress(), rule.getPublicPort(), rule.getPrivateIpAddress(), rule.getPrivatePort(),
                                rule.getProtocol());
                    } else {
                        success = deleteLoadBalancingRule(userId, accountId, rule.getPublicIpAddress(), rule.getPublicPort(), rule.getPrivateIpAddress(), rule.getPrivatePort(),
                                rule.getAlgorithm());
                    }
                } catch (Exception ex) {
                    e = ex;
                }

                String description;
                String type = EventTypes.EVENT_NET_RULE_DELETE;
                String level = EventVO.LEVEL_INFO;
                String ruleName = rule.isForwarding() ? "ip forwarding" : "load balancer";

                if (success) {
                    String desc = "deleted " + ruleName + " rule [" + rule.getPublicIpAddress() + ":" + rule.getPublicPort() + "]->[" + rule.getPrivateIpAddress() + ":"
                            + rule.getPrivatePort() + "] " + rule.getProtocol();
                    if (!rule.isForwarding()) {
                        desc = desc + ", algorithm = " + rule.getAlgorithm();
                    }
                    description = desc;
                } else {
                    level = EventVO.LEVEL_ERROR;
                    String desc = "deleted " + ruleName + " rule [" + rule.getPublicIpAddress() + ":" + rule.getPublicPort() + "]->[" + rule.getPrivateIpAddress() + ":"
                            + rule.getPrivatePort() + "] " + rule.getProtocol();
                    if (!rule.isForwarding()) {
                        desc = desc + ", algorithm = " + rule.getAlgorithm();
                    }
                    description = desc;
                }

                EventUtils.saveEvent(userId, accountId, level, type, description);
            }
        } finally {
            if (e != null) {
                if (e instanceof InvalidParameterValueException) {
                    throw (InvalidParameterValueException) e;
                } else if (e instanceof PermissionDeniedException) {
                    throw (PermissionDeniedException) e;
                } else if (e instanceof InternalErrorException) {
                    throw (InternalErrorException) e;
                }
            }
        }
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

    @Override
    public List<GuestOSVO> listGuestOSByCriteria(ListGuestOsCmd cmd) {
        Filter searchFilter = new Filter(GuestOSVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
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

//    public boolean destroyConsoleProxy(long instanceId, long startEventId) {
//        return _consoleProxyMgr.destroyProxy(instanceId, startEventId);
//    }

    public long startConsoleProxyAsync(long instanceId) {
        long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_START, "starting console proxy with Id: "+instanceId);
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
        long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_STOP, "stopping console proxy with Id: "+instanceId);
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
        long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_REBOOT, "rebooting console proxy with Id: "+instanceId);
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

//    public long destroyConsoleProxyAsync(long instanceId) {
//        long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_DESTROY, "destroying console proxy with Id: "+instanceId);
//        VMOperationParam param = new VMOperationParam(0, instanceId, null, eventId);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//        job.setUserId(UserContext.current().getUserId());
//        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
//        job.setCmd("DestroyConsoleProxy");
//        job.setCmdInfo(gson.toJson(param));
//        
//        return _asyncMgr.submitAsyncJob(job);
//    }

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

    @Override
    public List<DomainVO> searchForDomains(ListDomainsCmd cmd) throws PermissionDeniedException {
        Long domainId = cmd.getId();
        Account account = (Account)UserContext.current().getAccountObject();
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

        if ((domainName == null) && (level == null) && (domainId != null)) {
            DomainVO domain = _domainDao.findById(domainId);
            if (domain != null) {
                sc.setParameters("path", domain.getPath() + "%");
            }
        }

        return _domainDao.search(sc, searchFilter);
    }

    @Override
    public List<DomainVO> searchForDomainChildren(ListDomainChildrenCmd cmd) throws PermissionDeniedException {
        Filter searchFilter = new Filter(DomainVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Long domainId = cmd.getId();
        String domainName = cmd.getDomainName();
        Object keyword = cmd.getKeyword();

        Account account = (Account)UserContext.current().getAccountObject();
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
        Account account = (Account)UserContext.current().getAccountObject();

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
                    	EventUtils.saveEvent(new Long(1), ownerId, EventVO.LEVEL_ERROR, EventTypes.EVENT_DOMAIN_DELETE, "Failed to clean up domain resources and sub domains, domain with id " + domainId + " was not deleted.");
                        return "Failed to clean up domain resources and sub domains, delete failed on domain " + domain.getName() + " (id: " + domainId + ").";
                    }
                } else {
                    if (!_domainDao.remove(domainId)) {
                    	EventUtils.saveEvent(new Long(1), ownerId, EventVO.LEVEL_ERROR, EventTypes.EVENT_DOMAIN_DELETE, "Domain with id " + domainId + " was not deleted");
                        return "Delete failed on domain " + domain.getName() + " (id: " + domainId + "); please make sure all users and sub domains have been removed from the domain before deleting";
                    } else {
                    	EventUtils.saveEvent(new Long(1), ownerId, EventVO.LEVEL_INFO, EventTypes.EVENT_DOMAIN_DELETE, "Domain with id " + domainId + " was deleted");
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
            SearchCriteria<DomainVO> sc = _domainDao.createSearchCriteria();
            sc.addAnd("parent", SearchCriteria.Op.EQ, domainId);
            List<DomainVO> domains = _domainDao.search(sc, null);

            // cleanup sub-domains first
            for (DomainVO domain : domains) {
                success = (success && cleanupDomain(domain.getId(), domain.getOwner()));
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
                    success = (success && deleteUserInternal(user.getId()));
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

    public void updateDomain(UpdateDomainCmd cmd) throws InvalidParameterValueException{
    	Long domainId = cmd.getId();
    	String domainName = cmd.getName();
    	
        //check if domain exists in the system
    	DomainVO domain = findDomainIdById(domainId);
    	if (domain == null) {
    		throw new InvalidParameterValueException("Unable to find domain " + domainId);
    	} else if (domain.getParent() == null) {
            //check if domain is ROOT domain - and deny to edit it
    		throw new InvalidParameterValueException("ROOT domain can not be edited");
    	}

    	// check permissions
    	Account account = (Account)UserContext.current().getAccountObject();
    	if ((account != null) && !isChildDomain(account.getDomainId(), domain.getId())) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to update domain " + domainId + ", permission denied");
    	}

    	if (domainName == null) {
    		domainName = domain.getName();
    	}
    	
    	
        SearchCriteria<DomainVO> sc = _domainDao.createSearchCriteria();
        sc.addAnd("name", SearchCriteria.Op.EQ, domainName);
        List<DomainVO> domains = _domainDao.search(sc, null);
        if ((domains == null) || domains.isEmpty()) {
            _domainDao.update(domainId, domainName);
            domain = _domainDao.findById(domainId);
            EventUtils.saveEvent(new Long(1), domain.getOwner(), EventVO.LEVEL_INFO, EventTypes.EVENT_DOMAIN_UPDATE, "Domain, " + domainName + " was updated");
        } else {
            domain = _domainDao.findById(domainId);
            EventUtils.saveEvent(new Long(1), domain.getOwner(), EventVO.LEVEL_ERROR, EventTypes.EVENT_DOMAIN_UPDATE, "Failed to update domain " + domain.getName() + " with name " + domainName + ", name in use.");
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
    public SnapshotVO createTemplateSnapshot(Long userId, long volumeId) {
        return _vmMgr.createTemplateSnapshot(userId, volumeId);
    }

    @Override
    public boolean destroyTemplateSnapshot(Long userId, long snapshotId) {
        return _vmMgr.destroyTemplateSnapshot(userId, snapshotId);
    }

    @Override
    public List<SnapshotVO> listSnapshots(ListSnapshotsCmd cmd) throws InvalidParameterValueException {
        Long volumeId = cmd.getVolumeId();

        // Verify parameters
        if(volumeId != null){
            VolumeVO volume = _volumeDao.findById(volumeId);
            if (volume == null) {
                throw new InvalidParameterValueException("unable to find a volume with id " + volumeId);
            }
            checkAccountPermissions(volume.getAccountId(), volume.getDomainId(), "volume", volumeId);
        }

        Account account = (Account)UserContext.current().getAccountObject();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;
        if ((account == null) || isAdmin(account.getType())) {
            if(account != null && account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)
                accountId = account.getId();
            if (domainId != null && accountName != null) {
                Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                if (userAccount != null) {
                    accountId = userAccount.getId();
                }
            }
        } else {
            accountId = account.getId();
        }

        Filter searchFilter = new Filter(SnapshotVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<SnapshotVO> sc = _snapshotDao.createSearchCriteria();

        Object name = cmd.getSnapshotName();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Object snapshotTypeStr = cmd.getSnapshotType();
        String interval = cmd.getIntervalType();

        sc.addAnd("status", SearchCriteria.Op.EQ, Snapshot.Status.BackedUp);

        if (volumeId != null) {
            sc.addAnd("volumeId", SearchCriteria.Op.EQ, volumeId);
        }
        
        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (keyword != null) {
            SearchCriteria<SnapshotVO> ssc = _snapshotDao.createSearchCriteria();
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
        } else {
            // Show only MANUAL and RECURRING snapshot types
            sc.addAnd("snapshotType", SearchCriteria.Op.NEQ, Snapshot.SnapshotType.TEMPLATE.ordinal());
        }

        if (interval != null && volumeId != null) {
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

    @Override
    @DB
    public boolean updateTemplatePermissions(UpdateTemplateOrIsoPermissionsCmd cmd) throws InvalidParameterValueException,
            PermissionDeniedException, InternalErrorException {
        Transaction txn = Transaction.currentTxn();
        
        //Input validation
        Long id = cmd.getId();
        Account account = (Account) UserContext.current().getAccountObject();
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
                    if (permittedAccount.getId().longValue() == account.getId().longValue()) {
                        continue; // don't grant permission to the template owner, they implicitly have permission
                    }
                    LaunchPermissionVO existingPermission = _launchPermissionDao.findByTemplateAndAccount(id, permittedAccount.getId().longValue());
                    if (existingPermission == null) {
                        LaunchPermissionVO launchPermission = new LaunchPermissionVO(id, permittedAccount.getId().longValue());
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
                _launchPermissionDao.removePermissions(id, accountIds);
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
            _launchPermissionDao.removeAllPermissions(id);
        }
        return true;
    }

    @Override
    public List<String> listTemplatePermissions(ListTemplateOrIsoPermissionsCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Account account = (Account)UserContext.current().getAccountObject();
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
    public SecurityGroupVO createPortForwardingService(CreatePortForwardingServiceCmd cmd) throws InvalidParameterValueException {
        Account account = (Account)UserContext.current().getAccountObject();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;
        String portForwardingServiceName = cmd.getPortForwardingServiceName();

        if (account != null) {
            if (isAdmin(account.getType())) {
                if ((accountName != null) && (domainId != null)) {
                    if (!_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                        throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to create port forwarding service in domain " + domainId + ", permission denied.");
                    }

                    Account userAccount = findActiveAccount(accountName, domainId);
                    if (userAccount != null) {
                        accountId = userAccount.getId();
                    } else {
                        throw new InvalidParameterValueException("Unable to create port forwarding service " + portForwardingServiceName + ", could not find account " + accountName + " in domain " + domainId);
                    }
                } else {
                    // the admin must be creating the security group
                    if (account != null) {
                        accountId = account.getId();
                        domainId = account.getDomainId();
                    }
                }
            } else {
                accountId = account.getId();
                domainId = account.getDomainId();
            }
        }

        if (accountId == null) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to create port forwarding service, no account specified.");
        }

        if (isSecurityGroupNameInUse(domainId, accountId, portForwardingServiceName)) {
            throw new InvalidParameterValueException("Unable to create port forwarding service, a service with name " + portForwardingServiceName + " already exisits.");
        }

        SecurityGroupVO group = new SecurityGroupVO(portForwardingServiceName, cmd.getDescription(), domainId, accountId);
        return _securityGroupDao.persist(group);
    }

    @Override
    public long deleteSecurityGroupAsync(long userId, Long accountId, long securityGroupId) {
    	
        long eventId = EventUtils.saveScheduledEvent(userId, accountId, EventTypes.EVENT_PORT_FORWARDING_SERVICE_DELETE, "deleting security group with Id: " + securityGroupId);
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
    public List<SecurityGroupVO> searchForSecurityGroups(ListPortForwardingServicesCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        // if an admin account was passed in, or no account was passed in, make sure we honor the accountName/domainId parameters
        Account account = (Account)UserContext.current().getAccountObject();
        Long accountId = null;
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();

        if ((account == null) || isAdmin(account.getType())) {
            // validate domainId before proceeding
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Invalid domain id (" + domainId + ") given, unable to list port forwarding services.");
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

        Filter searchFilter = new Filter(SecurityGroupVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object name = cmd.getPortForwardingServiceName();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();

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

        SearchCriteria<SecurityGroupVO> sc = sb.create();
        if (keyword != null) {
            SearchCriteria<SecurityGroupVO> ssc = _securityGroupDao.createSearchCriteria();
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
    public Map<String, List<SecurityGroupVO>> searchForSecurityGroupsByVM(ListPortForwardingServicesByVmCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Account account = (Account)UserContext.current().getAccountObject();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;

        if ((account == null) || isAdmin(account.getType())) {
            // validate domainId before proceeding
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Unable to list port forwarding services for domain " + domainId + ", permission denied.");
                }
                if (accountName != null) {
                    Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                    if (userAccount != null) {
                        accountId = userAccount.getId();
                    } else {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                }
            }
        } else {
            accountId = account.getId();
        }

        Object ipAddress = cmd.getIpAddress();
        Long instanceId = cmd.getVirtualMachineId();
        UserVm userVm = _userVmDao.findById(instanceId);
        if (userVm == null) {
            throw new InvalidParameterValueException("Internal error, unable to find virtual machine " + instanceId + " for listing port forwarding services.");
        }

        if ((accountId != null) && (userVm.getAccountId() != accountId.longValue())) {
            throw new PermissionDeniedException("Unable to list port forwarding services, account " + accountId + " does not own virtual machine " + instanceId);
        }

        Filter searchFilter = new Filter(SecurityGroupVMMapVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<SecurityGroupVMMapVO> sc = _securityGroupVMMapDao.createSearchCriteria();

        // TODO: keyword search on vm name?  vm group?  what makes sense here?  We can't search directly on 'name' as that's not a field of SecurityGroupVMMapVO.
        //Object keyword = cmd.getKeyword();

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

    /*
    @Override @DB
    public long assignToLoadBalancerAsync(long userId, long loadBalancerId, List<Long> instanceIds, Map<String, String> params) {
        LoadBalancerVO loadBalancer = null;
        try {
            // unpack the params
            String accountName = params.get(BaseCmd.Properties.ACCOUNT.getName());
            String domainIdStr = params.get(BaseCmd.Properties.DOMAIN_ID.getName());
            String loadBalancerIdStr = params.get(BaseCmd.Properties.ID.getName());
            String instanceIdStr = params.get(BaseCmd.Properties.VIRTUAL_MACHINE_ID.getName());
            String instanceIdList = params.get(BaseCmd.Properties.VIRTUAL_MACHINE_IDS.getName());

            Long domainId = null;
            if (domainIdStr != null) {
                domainId = Long.valueOf(domainIdStr);
            }

            Long loadBalancerId = null;
            if (loadBalancerIdStr != null) {
                loadBalancerId = Long.valueOf(loadBalancerIdStr);
            }

            List<Long> instanceIds = new ArrayList<Long>();
            if (instanceIdList != null) {
                StringTokenizer st = new StringTokenizer(instanceIdList, ",");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    try {
                        Long nextInstanceId = Long.parseLong(token);
                        instanceIds.add(nextInstanceId);
                    } catch (NumberFormatException nfe) {
                        throw new ServerApiException(BaseCmd.PARAM_ERROR, "The virtual machine id " + token + " is not a valid parameter.");
                    }
                }
            } else if (instanceIdStr != null) {
                instanceIds.add(Long.valueOf(instanceIdStr));
            }

            loadBalancer = _loadBalancerDao.acquire(loadBalancerId);

            // if unable to lock the load balancer, throw an exception
            if (loadBalancer == null) {
                throw new CloudRuntimeException("Failed to assign instances to load balancer, unable to lock load balancer " + loadBalancerId);
            }

            IPAddressVO ipAddress = _publicIpAddressDao.findById(loadBalancer.getIpAddress());
            DomainRouterVO router = _routerDao.findBy(loadBalancer.getAccountId(), ipAddress.getDataCenterId());
            params.put("domainrouterid", Long.valueOf(router.getId()).toString());

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

//            LoadBalancerParam param = new LoadBalancerParam(userId, router.getId(), loadBalancerId, instanceIds);
            Gson gson = GsonHelper.getBuilder().create();

            AsyncJobVO job = new AsyncJobVO();
            job.setUserId(UserContext.current().getUserId());
            job.setAccountId(loadBalancer.getAccountId());
            job.setCmd("AssignToLoadBalancer");
//            job.setCmdInfo(gson.toJson(param));
            job.setCmdInfo(gson.toJson(params));
            return _asyncMgr.submitAsyncJob(job, true);
        } finally {
            if (loadBalancer != null) {
                _loadBalancerDao.release(loadBalancer.getId());
            }
        }
    }
    */

//    @Override @DB
//    public boolean removeFromLoadBalancer(long userId, long loadBalancerId, List<Long> instanceIds) throws InvalidParameterValueException {
//
//    	Transaction txn = Transaction.currentTxn();
//        LoadBalancerVO loadBalancerLock = null;
//        boolean success = true;
//        try {
//            LoadBalancerVO loadBalancer = _loadBalancerDao.findById(loadBalancerId);
//            if (loadBalancer == null) {
//                return false;
//            }
//
//            IPAddressVO ipAddress = _publicIpAddressDao.findById(loadBalancer.getIpAddress());
//            if (ipAddress == null) {
//                return false;
//            }
//
//            DomainRouterVO router = _routerDao.findBy(ipAddress.getAccountId(), ipAddress.getDataCenterId());
//            if (router == null) {
//                return false;
//            }
//
//            txn.start();
//            for (Long instanceId : instanceIds) {
//                UserVm userVm = _userVmDao.findById(instanceId);
//                if (userVm == null) {
//                    s_logger.warn("Unable to find virtual machine with id " + instanceId);
//                    throw new InvalidParameterValueException("Unable to find virtual machine with id " + instanceId);
//                }
//                FirewallRuleVO fwRule = _firewallRulesDao.findByGroupAndPrivateIp(loadBalancerId, userVm.getGuestIpAddress(), false);
//                if (fwRule != null) {
//                    fwRule.setEnabled(false);
//                    _firewallRulesDao.update(fwRule.getId(), fwRule);
//                }
//            }
//
//            List<FirewallRuleVO> allLbRules = new ArrayList<FirewallRuleVO>();
//            IPAddressVO ipAddr = _publicIpAddressDao.findById(loadBalancer.getIpAddress());
//            List<IPAddressVO> ipAddrs = _networkMgr.listPublicIpAddressesInVirtualNetwork(loadBalancer.getAccountId(), ipAddr.getDataCenterId(), null);
//            for (IPAddressVO ipv : ipAddrs) {
//                List<FirewallRuleVO> rules = _firewallRulesDao.listIPForwarding(ipv.getAddress(), false);
//                allLbRules.addAll(rules);
//            }
//
//            _networkMgr.updateFirewallRules(loadBalancer.getIpAddress(), allLbRules, router);
//
//            // firewall rules are updated, lock the load balancer as mappings are updated
//            loadBalancerLock = _loadBalancerDao.acquire(loadBalancerId);
//            if (loadBalancerLock == null) {
//                s_logger.warn("removeFromLoadBalancer: failed to lock load balancer " + loadBalancerId + ", deleting mappings anyway...");
//            }
//
//            // remove all the loadBalancer->VM mappings
//            _loadBalancerVMMapDao.remove(loadBalancerId, instanceIds, Boolean.FALSE);
//
//            // Save and create the event
//            String description;
//            String type = EventTypes.EVENT_NET_RULE_DELETE;
//            String level = EventVO.LEVEL_INFO;
//            Account account = _accountDao.findById(loadBalancer.getAccountId());
//
//            for (FirewallRuleVO updatedRule : allLbRules) {
//                if (!updatedRule.isEnabled()) {
//                    _firewallRulesDao.remove(updatedRule.getId());
//
//                    description = "deleted load balancer rule [" + updatedRule.getPublicIpAddress() + ":" + updatedRule.getPublicPort() + "]->["
//                            + updatedRule.getPrivateIpAddress() + ":" + updatedRule.getPrivatePort() + "]" + " " + updatedRule.getProtocol();
//
//                    EventUtils.saveEvent(userId, account.getId(), level, type, description);
//                }
//            }
//            txn.commit();
//        } catch (Exception ex) {
//            s_logger.warn("Failed to delete load balancing rule with exception: ", ex);
//            success = false;
//            txn.rollback();
//        } finally {
//            if (loadBalancerLock != null) {
//                _loadBalancerDao.release(loadBalancerId);
//            }
//        }
//        return success;
//    }

    
//    @Override
//    public long removeFromLoadBalancerAsync(long userId, long loadBalancerId, List<Long> instanceIds) {
//        LoadBalancerVO loadBalancer = _loadBalancerDao.findById(loadBalancerId);
//        IPAddressVO ipAddress = _publicIpAddressDao.findById(loadBalancer.getIpAddress());
//        DomainRouterVO router = _routerDao.findBy(loadBalancer.getAccountId(), ipAddress.getDataCenterId());
//        LoadBalancerParam param = new LoadBalancerParam(userId, router.getId(), loadBalancerId, instanceIds);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//    	job.setUserId(UserContext.current().getUserId());
//    	job.setAccountId(loadBalancer.getAccountId());
//        job.setCmd("RemoveFromLoadBalancer");
//        job.setCmdInfo(gson.toJson(param));
//        
//        return _asyncMgr.submitAsyncJob(job, true);
//    }

//    @Override @DB
//    public boolean deleteLoadBalancer(long userId, long loadBalancerId) {
//        Transaction txn = Transaction.currentTxn();
//        LoadBalancerVO loadBalancer = null;
//        LoadBalancerVO loadBalancerLock = null;
//        try {
//            loadBalancer = _loadBalancerDao.findById(loadBalancerId);
//            if (loadBalancer == null) {
//                return false;
//            }
//
//            IPAddressVO ipAddress = _publicIpAddressDao.findById(loadBalancer.getIpAddress());
//            if (ipAddress == null) {
//                return false;
//            }
//
//            DomainRouterVO router = _routerDao.findBy(ipAddress.getAccountId(), ipAddress.getDataCenterId());
//            List<FirewallRuleVO> fwRules = _firewallRulesDao.listByLoadBalancerId(loadBalancerId);
//
//            txn.start();
//
//            if ((fwRules != null) && !fwRules.isEmpty()) {
//                for (FirewallRuleVO fwRule : fwRules) {
//                    fwRule.setEnabled(false);
//                    _firewallRulesDao.update(fwRule.getId(), fwRule);
//                }
//
//                List<FirewallRuleVO> allLbRules = new ArrayList<FirewallRuleVO>();
//                List<IPAddressVO> ipAddrs = _networkMgr.listPublicIpAddressesInVirtualNetwork(loadBalancer.getAccountId(), ipAddress.getDataCenterId(), null);
//                for (IPAddressVO ipv : ipAddrs) {
//                    List<FirewallRuleVO> rules = _firewallRulesDao.listIPForwarding(ipv.getAddress(), false);
//                    allLbRules.addAll(rules);
//                }
//
//                _networkMgr.updateFirewallRules(loadBalancer.getIpAddress(), allLbRules, router);
//
//                // firewall rules are updated, lock the load balancer as the mappings are updated
//                loadBalancerLock = _loadBalancerDao.acquire(loadBalancerId);
//                if (loadBalancerLock == null) {
//                    s_logger.warn("deleteLoadBalancer: failed to lock load balancer " + loadBalancerId + ", deleting mappings anyway...");
//                }
//
//                // remove all loadBalancer->VM mappings
//                _loadBalancerVMMapDao.remove(loadBalancerId);
//
//                // Save and create the event
//                String description;
//                String type = EventTypes.EVENT_NET_RULE_DELETE;
//                String ruleName = "load balancer";
//                String level = EventVO.LEVEL_INFO;
//                Account account = _accountDao.findById(loadBalancer.getAccountId());
//
//                for (FirewallRuleVO updatedRule : fwRules) {
//                    _firewallRulesDao.remove(updatedRule.getId());
//
//                    description = "deleted " + ruleName + " rule [" + updatedRule.getPublicIpAddress() + ":" + updatedRule.getPublicPort() + "]->["
//                                  + updatedRule.getPrivateIpAddress() + ":" + updatedRule.getPrivatePort() + "]" + " " + updatedRule.getProtocol();
//
//                    EventUtils.saveEvent(userId, account.getId(), level, type, description);
//                }
//            }
//
//            txn.commit();
//        } catch (Exception ex) {
//            txn.rollback();
//            s_logger.error("Unexpected exception deleting load balancer " + loadBalancerId, ex);
//            return false;
//        } finally {
//            if (loadBalancerLock != null) {
//                _loadBalancerDao.release(loadBalancerId);
//            }
//        }
//
//        boolean success = _loadBalancerDao.remove(loadBalancerId);
//
//        // save off an event for removing the security group
//        EventVO event = new EventVO();
//        event.setUserId(userId);
//        event.setAccountId(loadBalancer.getAccountId());
//        event.setType(EventTypes.EVENT_LOAD_BALANCER_DELETE);
//        if (success) {
//            event.setLevel(EventVO.LEVEL_INFO);
//            String params = "id="+loadBalancer.getId();
//            event.setParameters(params);
//            event.setDescription("Successfully deleted load balancer " + loadBalancer.getName() + " (id:" + loadBalancer.getId() + ")");
//        } else {
//            event.setLevel(EventVO.LEVEL_ERROR);
//            event.setDescription("Failed to delete load balancer " + loadBalancer.getName() + " (id:" + loadBalancer.getId() + ")");
//        }
//        _eventDao.persist(event);
//        return success;
//    }
//
//    @Override
//    public long deleteLoadBalancerAsync(long userId, long loadBalancerId) {
//        LoadBalancerVO loadBalancer = _loadBalancerDao.findById(loadBalancerId);
//        IPAddressVO ipAddress = _publicIpAddressDao.findById(loadBalancer.getIpAddress());
//        DomainRouterVO router = _routerDao.findBy(loadBalancer.getAccountId(), ipAddress.getDataCenterId());
//        LoadBalancerParam param = new LoadBalancerParam(userId, router.getId(), loadBalancerId, null);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//    	job.setUserId(UserContext.current().getUserId());
//    	job.setAccountId(loadBalancer.getAccountId());
//        job.setCmd("DeleteLoadBalancer");
//        job.setCmdInfo(gson.toJson(param));
//        
//        return _asyncMgr.submitAsyncJob(job, true);
//    }

    @Override
    public List<UserVmVO> listLoadBalancerInstances(ListLoadBalancerRuleInstancesCmd cmd) throws PermissionDeniedException {
        Account account = (Account)UserContext.current().getAccountObject();
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
            } else if (account.getId().longValue() != lbAcctId) {
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
        Account account = (Account)UserContext.current().getAccountObject();
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
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId());
        }

        if (instanceId != null) {
            SearchBuilder<LoadBalancerVMMapVO> lbVMSearch = _loadBalancerVMMapDao.createSearchBuilder();
            lbVMSearch.and("instanceId", lbVMSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
            sb.join("lbVMSearch", lbVMSearch, sb.entity().getId(), lbVMSearch.entity().getLoadBalancerId());
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

                Transaction txn = null;
                try {
                	txn = Transaction.open(Transaction.CLOUD_DB);
                	
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
    public ClusterVO findClusterById(long clusterId) {
        return _clusterDao.findById(clusterId);
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
    public List<AsyncJobVO> searchForAsyncJobs(ListAsyncJobsCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Filter searchFilter = new Filter(AsyncJobVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<AsyncJobVO> sb = _jobDao.createSearchBuilder();

        Object accountId = null;
        Long domainId = cmd.getDomainId();
        Account account = (Account)UserContext.current().getAccountObject();
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
                accountSearch.join("domainSearch", domainSearch, accountSearch.entity().getDomainId(), domainSearch.entity().getId());

                sb.join("accountSearch", accountSearch, sb.entity().getAccountId(), accountSearch.entity().getId());
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
    
//	@Override
//	public boolean deleteSnapshotPolicies(long userId, List<Long> policyIds) throws InvalidParameterValueException {
//		boolean result = true;
//		if (policyIds.contains(Snapshot.MANUAL_POLICY_ID)) {
//		    throw new InvalidParameterValueException("Invalid Policy id given: " + Snapshot.MANUAL_POLICY_ID);
//		}
//		for (long policyId : policyIds) {
//			if (!_snapMgr.deletePolicy(userId, policyId)) {
//				result = false;
//				s_logger.warn("Failed to delete snapshot policy with Id: " + policyId);
//			}
//		}
//		return result;
//	}

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
        long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_START, "starting secondary storage Vm Id: "+instanceId);
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
        long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_STOP, "stopping secondary storage Vm Id: "+instanceId);
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
        long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_REBOOT, "rebooting secondary storage Vm Id: "+instanceId);
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
        long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_DESTROY, "destroying secondary storage Vm Id: "+instanceId);
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

        return (List<? extends VMInstanceVO>)systemVMs;
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
	public VMInstanceVO startSystemVM(StartSystemVMCmd cmd) throws InternalErrorException {
		
		//verify input
		Long id = cmd.getId();

		VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(id, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
        	throw new ServerApiException (BaseCmd.PARAM_ERROR, "unable to find a system vm with id " + id);
        }
		
		if (systemVm.getType().equals(VirtualMachine.Type.ConsoleProxy)){
			long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_START, "Starting console proxy with Id: "+id);
			return startConsoleProxy(id, eventId);
		} else {
			long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_START, "Starting secondary storage Vm Id: "+id);
			return startSecondaryStorageVm(id, eventId);
		}
	}



	@Override
	public boolean stopSystemVM(StopSystemVmCmd cmd) {
		Long id = cmd.getId();
		
	    // verify parameters      
		VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(id, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
        	throw new ServerApiException (BaseCmd.PARAM_ERROR, "unable to find a system vm with id " + id);
        }
        
		if (systemVm.getType().equals(VirtualMachine.Type.ConsoleProxy)){
			long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_STOP, "stopping console proxy with Id: "+id);
			return stopConsoleProxy(id, eventId);
		} else {
			long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_STOP, "stopping secondary storage Vm Id: "+id);
			return stopSecondaryStorageVm(id, eventId);
		}
	}

//	@Override
//	public long stopSystemVmAsync(long instanceId) {
//		VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(instanceId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
//		if (systemVm.getType().equals(VirtualMachine.Type.ConsoleProxy)){
//			return stopConsoleProxyAsync(instanceId);
//		} else {
//			return stopSecondaryStorageVmAsync(instanceId);
//		}
//	}

//	@Override
//	public long rebootSystemVmAsync(long instanceId) {
//		VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(instanceId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
//		if (systemVm.getType().equals(VirtualMachine.Type.ConsoleProxy)){
//			return rebootConsoleProxyAsync(instanceId);
//		} else {
//			return rebootSecondaryStorageVmAsync(instanceId);
//		}
//	}
	
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
	public boolean rebootSystemVM(RebootSystemVmCmd cmd)  {
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

//    @Override
//	public boolean revokeNetworkGroupIngress(AccountVO account, String groupName, String protocol, int startPort, int endPort, String [] cidrList, List<NetworkGroupVO> authorizedGroups) {
//		return _networkGroupMgr.revokeNetworkGroupIngress(account, groupName, protocol, startPort, endPort, cidrList, authorizedGroups);
//	}

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
    public void deleteNetworkGroup(Long groupId, Long accountId) throws ResourceInUseException, PermissionDeniedException {
        _networkGroupMgr.deleteNetworkGroup(groupId, accountId);
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
	public String getNetworkGroupsNamesForVm(long vmId) 
	{

		return _networkGroupMgr.getNetworkGroupsNamesForVm(vmId);
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

//	public boolean preparePrimaryStorageForMaintenance(long primaryStorageId, long userId) {
//		return	_storageMgr.preparePrimaryStorageForMaintenance(primaryStorageId, userId);
//
//	}
//
//	public long preparePrimaryStorageForMaintenanceAsync(long primaryStorageId) throws InvalidParameterValueException 
//	{
//    	StoragePoolVO primaryStorage = _poolDao.findById(primaryStorageId);
//    	
//    	if (primaryStorage == null) {
//            s_logger.debug("Unable to find primary storage id: " + primaryStorageId);
//            throw new InvalidParameterValueException("Unable to find storage pool with ID: " + primaryStorageId + ". Please specify a valid primary storage ID.");
//        }
//        
//        if (_poolDao.countBy(primaryStorage.getId(), Status.PrepareForMaintenance, Status.ErrorInMaintenance, Status.Maintenance) > 0) {
//            throw new InvalidParameterValueException("There are other primary storages in maintenance mode.");
//        }
//        
//        //set the state to maintenance
//        primaryStorage.setStatus(Status.PrepareForMaintenance);
//        _poolDao.persist(primaryStorage);
//        
//        Long param = new Long(primaryStorageId);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//        job.setUserId(UserContext.current().getUserId());
//        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
//        job.setCmd("PreparePrimaryStorageMaintenance");
//        job.setCmdInfo(gson.toJson(param));
//        job.setCmdOriginator(PreparePrimaryStorageForMaintenanceCmd.getResultObjectName());
//        return _asyncMgr.submitAsyncJob(job);
//
//	}
	
//    public boolean cancelPrimaryStorageMaintenance(long primaryStorageId, long userId)
//    {
//		return	_storageMgr.cancelPrimaryStorageForMaintenance(primaryStorageId, userId);
//    }
//	
//	public long cancelPrimaryStorageMaintenanceAsync(long primaryStorageId) throws InvalidParameterValueException 
//	{
//    	StoragePoolVO primaryStorage = _poolDao.findById(primaryStorageId);
//    	
//    	if (primaryStorage == null) {
//            s_logger.debug("Unable to find primary storage id: " + primaryStorageId);
//            throw new InvalidParameterValueException("Unable to find storage pool with ID: " + primaryStorageId + ". Please specify a valid primary storage ID.");
//        }
//        
//        Long param = new Long(primaryStorageId);
//        Gson gson = GsonHelper.getBuilder().create();
//
//        AsyncJobVO job = new AsyncJobVO();
//        job.setUserId(UserContext.current().getUserId());
//        job.setAccountId(Account.ACCOUNT_ID_SYSTEM);
//        job.setCmd("CancelPrimaryStorageMaintenance");
//        job.setCmdInfo(gson.toJson(param));
//        job.setCmdOriginator(CancelPrimaryStorageMaintenanceCmd.getResultObjectName());
//        return _asyncMgr.submitAsyncJob(job);
//
//	}

	@Override
	public boolean validateCustomVolumeSizeRange(long size) throws InvalidParameterValueException {
        if (size<0 || (size>0 && size < 1)) {
            throw new InvalidParameterValueException("Please specify a size of at least 1 Gb.");
        } else if (size > _maxVolumeSizeInGb) {
        	throw new InvalidParameterValueException("The maximum size allowed is " + _maxVolumeSizeInGb + " Gb.");
        }

		return true;
	}

	@Override
	public boolean lockAccount(LockAccountCmd cmd) {

        Account adminAccount = (Account)UserContext.current().getAccountObject();
        Long domainId = cmd.getDomainId();
        String accountName = UserContext.current().getAccountName();

        if ((adminAccount != null) && !_domainDao.isChildDomain(adminAccount.getDomainId(), domainId)) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Failed to lock account " + accountName + " in domain " + domainId + ", permission denied.");
        }

        Account account = _accountDao.findActiveAccount(accountName, domainId);
        if (account == null) {
            throw new ServerApiException (BaseCmd.PARAM_ERROR, "Unable to find active account with name " + accountName + " in domain " + domainId);
        }

        // don't allow modify system account
        if (account.getId().longValue() == Account.ACCOUNT_ID_SYSTEM) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "can not lock system account");
        }

        return lockAccountInternal(account.getId());
	}

	@Override
	public boolean deleteUser(DeleteUserCmd cmd) {
        Long userId = cmd.getId();
        
        //Verify that the user exists in the system
        User user = getUser(userId.longValue());
        if (user == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find user " + userId);
        }
        
        // If the user is a System user, return an error.  We do not allow this
        Account account = _accountDao.findById(user.getAccountId());
        if ((account != null) && (account.getId() == Account.ACCOUNT_ID_SYSTEM)) {
        	throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "user id : " + userId + " is a system account, delete is not allowed");
        }
		
        return deleteUserInternal(userId);
	}

	private Long checkAccountPermissions(long targetAccountId, long targetDomainId, String targetDesc, long targetId) throws ServerApiException {
        Long accountId = null;

        Account account = (Account)UserContext.current().getAccountObject();
        if (account != null) {
            if (!isAdmin(account.getType())) {
                if (account.getId().longValue() != targetAccountId) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find a " + targetDesc + " with id " + targetId + " for this account");
                }
            } else if (!_domainDao.isChildDomain(account.getDomainId(), targetDomainId)) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to perform operation for " + targetDesc + " with id " + targetId + ", permission denied.");
            }
            accountId = account.getId();
        }
    
        return accountId;
    }
}

