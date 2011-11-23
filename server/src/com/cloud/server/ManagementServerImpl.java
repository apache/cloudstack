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

import java.lang.reflect.Field;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.cloud.acl.SecurityChecker.AccessType;
import com.cloud.agent.AgentManager;
import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.alert.Alert;
import com.cloud.alert.AlertManager;
import com.cloud.alert.AlertVO;
import com.cloud.alert.dao.AlertDao;
import com.cloud.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.commands.CreateSSHKeyPairCmd;
import com.cloud.api.commands.DeleteSSHKeyPairCmd;
import com.cloud.api.commands.DestroySystemVmCmd;
import com.cloud.api.commands.ExtractVolumeCmd;
import com.cloud.api.commands.GetVMPasswordCmd;
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
import com.cloud.api.commands.ListIsosCmd;
import com.cloud.api.commands.ListPodsByCmd;
import com.cloud.api.commands.ListPublicIpAddressesCmd;
import com.cloud.api.commands.ListRoutersCmd;
import com.cloud.api.commands.ListSSHKeyPairsCmd;
import com.cloud.api.commands.ListServiceOfferingsCmd;
import com.cloud.api.commands.ListStoragePoolsCmd;
import com.cloud.api.commands.ListSystemVMsCmd;
import com.cloud.api.commands.ListTemplateOrIsoPermissionsCmd;
import com.cloud.api.commands.ListTemplatesCmd;
import com.cloud.api.commands.ListUsersCmd;
import com.cloud.api.commands.ListVMGroupsCmd;
import com.cloud.api.commands.ListVlanIpRangesCmd;
import com.cloud.api.commands.ListVolumesCmd;
import com.cloud.api.commands.ListZonesByCmd;
import com.cloud.api.commands.RebootSystemVmCmd;
import com.cloud.api.commands.RegisterSSHKeyPairCmd;
import com.cloud.api.commands.StopSystemVmCmd;
import com.cloud.api.commands.UpdateDomainCmd;
import com.cloud.api.commands.UpdateHostPasswordCmd;
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
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.capacity.dao.CapacityDaoImpl.SummedCapacity;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationVO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.consoleproxy.ConsoleProxyManagementState;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.EventUtils;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.CloudAuthenticationException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorCapabilities;
import com.cloud.hypervisor.HypervisorCapabilitiesVO;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
import com.cloud.info.ConsoleProxyInfo;
import com.cloud.keystore.KeystoreManager;
import com.cloud.network.IPAddressVO;
import com.cloud.network.LoadBalancerVO;
import com.cloud.network.NetworkVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.LaunchPermissionVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.Upload;
import com.cloud.storage.Upload.Mode;
import com.cloud.storage.UploadVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.LaunchPermissionDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.UploadDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.storage.swift.SwiftManager;
import com.cloud.storage.upload.UploadMonitor;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.template.VirtualMachineTemplate.TemplateFilter;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.SSHKeyPair;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.User;
import com.cloud.user.UserAccountVO;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
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
import com.cloud.utils.net.MacAddress;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.ssh.SSHKeysHelper;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

import edu.emory.mathcs.backport.java.util.Arrays;

public class ManagementServerImpl implements ManagementServer {
    public static final Logger s_logger = Logger.getLogger(ManagementServerImpl.class.getName());

    private final AccountManager _accountMgr;
    private final AgentManager _agentMgr;
    private final AlertManager _alertMgr;
    private final IPAddressDao _publicIpAddressDao;
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
    private final HostDetailsDao _detailsDao;
    private final UserDao _userDao;
    private final UserVmDao _userVmDao;
    private final ConfigurationDao _configDao;
    private final UserVmManager _vmMgr;
    private final ConsoleProxyManager _consoleProxyMgr;
    private final SecondaryStorageVmManager _secStorageVmMgr;
    private final SwiftManager _swiftMgr;
    private final ServiceOfferingDao _offeringsDao;
    private final DiskOfferingDao _diskOfferingDao;
    private final VMTemplateDao _templateDao;
    private final LaunchPermissionDao _launchPermissionDao;
    private final DomainDao _domainDao;
    private final AccountDao _accountDao;
    private final UserAccountDao _userAccountDao;
    private final AlertDao _alertDao;
    private final CapacityDao _capacityDao;
    private final GuestOSDao _guestOSDao;
    private final GuestOSCategoryDao _guestOSCategoryDao;
    private final StoragePoolDao _poolDao;
    private final NicDao _nicDao;
    private final NetworkDao _networkDao;
    private final StorageManager _storageMgr;
    private final VirtualMachineManager _itMgr;
    private final TemplateManager _templateMgr;
    private final HostPodDao _hostPodDao;
    private final VMInstanceDao _vmInstanceDao;
    private final VolumeDao _volumeDao;
    private final AsyncJobDao _jobDao;
    private final AsyncJobManager _asyncMgr;
    private final int _purgeDelay;
    private final InstanceGroupDao _vmGroupDao;
    private final UploadMonitor _uploadMonitor;
    private final UploadDao _uploadDao;
    private final SSHKeyPairDao _sshKeyPairDao;
    private final LoadBalancerDao _loadbalancerDao;
    private final HypervisorCapabilitiesDao _hypervisorCapabilitiesDao;
    private final Adapters<HostAllocator> _hostAllocators;
    @Inject ProjectManager _projectMgr;
    private final ResourceManager _resourceMgr;
    @Inject
    SnapshotManager _snapshotMgr;
    
    private final KeystoreManager _ksMgr;

    private final ScheduledExecutorService _eventExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("EventChecker"));

    private final Map<String, String> _configs;
    
    private final StatsCollector _statsCollector;

    private final Map<String, Boolean> _availableIdsMap;

    private String _hashKey = null;

    protected ManagementServerImpl() {
        ComponentLocator locator = ComponentLocator.getLocator(Name);
        _configDao = locator.getDao(ConfigurationDao.class);
        _routerDao = locator.getDao(DomainRouterDao.class);
        _eventDao = locator.getDao(EventDao.class);
        _dcDao = locator.getDao(DataCenterDao.class);
        _vlanDao = locator.getDao(VlanDao.class);
        _accountVlanMapDao = locator.getDao(AccountVlanMapDao.class);
        _podVlanMapDao = locator.getDao(PodVlanMapDao.class);
        _hostDao = locator.getDao(HostDao.class);
        _detailsDao = locator.getDao(HostDetailsDao.class);
        _hostPodDao = locator.getDao(HostPodDao.class);
        _jobDao = locator.getDao(AsyncJobDao.class);
        _clusterDao = locator.getDao(ClusterDao.class);
        _nicDao = locator.getDao(NicDao.class);
        _networkDao = locator.getDao(NetworkDao.class);
        _loadbalancerDao = locator.getDao(LoadBalancerDao.class);

        _accountMgr = locator.getManager(AccountManager.class);
        _agentMgr = locator.getManager(AgentManager.class);
        _alertMgr = locator.getManager(AlertManager.class);
        _vmMgr = locator.getManager(UserVmManager.class);
        _consoleProxyMgr = locator.getManager(ConsoleProxyManager.class);
        _secStorageVmMgr = locator.getManager(SecondaryStorageVmManager.class);
        _swiftMgr = locator.getManager(SwiftManager.class);
        _storageMgr = locator.getManager(StorageManager.class);
        _publicIpAddressDao = locator.getDao(IPAddressDao.class);
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
        _userAccountDao = locator.getDao(UserAccountDao.class);
        _alertDao = locator.getDao(AlertDao.class);
        _capacityDao = locator.getDao(CapacityDao.class);
        _guestOSDao = locator.getDao(GuestOSDao.class);
        _guestOSCategoryDao = locator.getDao(GuestOSCategoryDao.class);
        _poolDao = locator.getDao(StoragePoolDao.class);
        _vmGroupDao = locator.getDao(InstanceGroupDao.class);
        _uploadDao = locator.getDao(UploadDao.class);
        _configs = _configDao.getConfiguration();
        _vmInstanceDao = locator.getDao(VMInstanceDao.class);
        _volumeDao = locator.getDao(VolumeDao.class);
        _asyncMgr = locator.getManager(AsyncJobManager.class);
        _uploadMonitor = locator.getManager(UploadMonitor.class);
        _sshKeyPairDao = locator.getDao(SSHKeyPairDao.class);
        _itMgr = locator.getManager(VirtualMachineManager.class);
        _ksMgr = locator.getManager(KeystoreManager.class);
        _resourceMgr = locator.getManager(ResourceManager.class);
        
        _hypervisorCapabilitiesDao = locator.getDao(HypervisorCapabilitiesDao.class);
        
        
        _hostAllocators = locator.getAdapters(HostAllocator.class);
        if (_hostAllocators == null || !_hostAllocators.isSet()) {
            s_logger.error("Unable to find HostAllocators");
        }
        
        _templateMgr = locator.getManager(TemplateManager.class);

        String value = _configs.get("account.cleanup.interval");
        int cleanup = NumbersUtil.parseInt(value, 60 * 60 * 24); // 1 day.
        
        _statsCollector = StatsCollector.getInstance(_configs);

        _purgeDelay = NumbersUtil.parseInt(_configs.get("event.purge.delay"), 0);
        if (_purgeDelay != 0) {
            _eventExecutor.scheduleAtFixedRate(new EventPurgeTask(), cleanup, cleanup, TimeUnit.SECONDS);
        }

        String[] availableIds = TimeZone.getAvailableIDs();
        _availableIdsMap = new HashMap<String, Boolean>(availableIds.length);
        for (String id : availableIds) {
            _availableIdsMap.put(id, true);
        }
    }

    protected Map<String, String> getConfigs() {
        return _configs;
    }

    @Override
    public String generateRandomPassword() {
        return PasswordGenerator.generateRandomPassword(6);
    }

    @Override
    public List<DataCenterVO> listDataCenters(ListZonesByCmd cmd) {
        Account account = UserContext.current().getCaller();
        List<DataCenterVO> dcs = null;
        Long domainId = cmd.getDomainId();
        Long id = cmd.getId();
        boolean removeDisabledZones = false;
        if (domainId != null) {
            // for domainId != null
            // right now, we made the decision to only list zones associated with this domain
            dcs = _dcDao.findZonesByDomainId(domainId); // private zones
        } else if ((account == null || account.getType() == Account.ACCOUNT_TYPE_ADMIN)) {
            dcs = _dcDao.listAll(); // all zones
        } else if (account.getType() == Account.ACCOUNT_TYPE_NORMAL) {
            // it was decided to return all zones for the user's domain, and everything above till root
            // list all zones belonging to this domain, and all of its parents
            // check the parent, if not null, add zones for that parent to list
            dcs = new ArrayList<DataCenterVO>();
            DomainVO domainRecord = _domainDao.findById(account.getDomainId());
            if (domainRecord != null) {
                while (true) {
                    dcs.addAll(_dcDao.findZonesByDomainId(domainRecord.getId()));
                    if (domainRecord.getParent() != null) {
                        domainRecord = _domainDao.findById(domainRecord.getParent());
                    } else {
                        break;
                    }
                }
            }
            // add all public zones too
            dcs.addAll(_dcDao.listPublicZones());
            removeDisabledZones = true;
        } else if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || account.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            // it was decided to return all zones for the domain admin, and everything above till root
            dcs = new ArrayList<DataCenterVO>();
            DomainVO domainRecord = _domainDao.findById(account.getDomainId());
            // this covers path till root
            if (domainRecord != null) {
                DomainVO localRecord = domainRecord;
                while (true) {
                    dcs.addAll(_dcDao.findZonesByDomainId(localRecord.getId()));
                    if (localRecord.getParent() != null) {
                        localRecord = _domainDao.findById(localRecord.getParent());
                    } else {
                        break;
                    }
                }
            }
            // this covers till leaf
            if (domainRecord != null) {
                // find all children for this domain based on a like search by path
                List<DomainVO> allChildDomains = _domainDao.findAllChildren(domainRecord.getPath(), domainRecord.getId());
                List<Long> allChildDomainIds = new ArrayList<Long>();
                // create list of domainIds for search
                for (DomainVO domain : allChildDomains) {
                    allChildDomainIds.add(domain.getId());
                }
                // now make a search for zones based on this
                if (allChildDomainIds.size() > 0) {
                    List<DataCenterVO> childZones = _dcDao.findChildZones((allChildDomainIds.toArray()));
                    dcs.addAll(childZones);
                }
            }
            // add all public zones too
            dcs.addAll(_dcDao.listPublicZones());
            removeDisabledZones = true;
        }

        if (removeDisabledZones) {
            dcs.removeAll(_dcDao.listDisabledZones());
        }

        Boolean available = cmd.isAvailable();
        if (account != null) {
            if ((available != null) && Boolean.FALSE.equals(available)) {
                List<DomainRouterVO> routers = _routerDao.listBy(account.getId());
                for (Iterator<DataCenterVO> iter = dcs.iterator(); iter.hasNext();) {
                    DataCenterVO dc = iter.next();
                    boolean found = false;
                    for (DomainRouterVO router : routers) {
                        if (dc.getId() == router.getDataCenterIdToDeployIn()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        iter.remove();
                    }
                }
            }
        }

        if (id != null) {
            List<DataCenterVO> singleZone = new ArrayList<DataCenterVO>();
            for (DataCenterVO zone : dcs) {
                if (zone.getId() == id) {
                    singleZone.add(zone);
                }
            }
            return singleZone;
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

    protected void checkPortParameters(String publicPort, String privatePort, String privateIp, String proto) {

        if (!NetUtils.isValidPort(publicPort)) {
            throw new InvalidParameterValueException("publicPort is an invalid value");
        }
        if (!NetUtils.isValidPort(privatePort)) {
            throw new InvalidParameterValueException("privatePort is an invalid value");
        }

        // s_logger.debug("Checking if " + privateIp + " is a valid private IP address. Guest IP address is: " +
        // _configs.get("guest.ip.network"));
        //
        // if (!NetUtils.isValidPrivateIp(privateIp, _configs.get("guest.ip.network"))) {
        // throw new InvalidParameterValueException("Invalid private ip address");
        // }
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
        Account caller = UserContext.current().getCaller();
        Long domainId = cmd.getDomainId();
        if (domainId != null) {
            Domain domain = _domainDao.findById(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Unable to find domain by id=" + domainId);
            }
            
            _accountMgr.checkAccess(caller, domain);
        } else {
            // default domainId to the admin's domain
            domainId = caller.getDomainId();
        }

        Filter searchFilter = new Filter(UserAccountVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Long id = cmd.getId();
        Object username = cmd.getUsername();
        Object type = cmd.getAccountType();
        Object accountName = cmd.getAccountName();
        Object state = cmd.getState();
        Object keyword = cmd.getKeyword();

        SearchBuilder<UserAccountVO> sb = _userAccountDao.createSearchBuilder();
        sb.and("username", sb.entity().getUsername(), SearchCriteria.Op.LIKE);
        if (id != null && id == 1) {
            // system user should NOT be searchable
            List<UserAccountVO> emptyList = new ArrayList<UserAccountVO>();
            return emptyList;
        } else if (id != null) {
            sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        } else {
            // this condition is used to exclude system user from the search results
            sb.and("id", sb.entity().getId(), SearchCriteria.Op.NEQ);
        }

        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        sb.and("accountName", sb.entity().getAccountName(), SearchCriteria.Op.EQ);
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
            sc.setParameters("username", username);
        }

        if (id != null) {
            sc.setParameters("id", id);
        } else {
            // Don't return system user, search builder with NEQ
            sc.setParameters("id", 1);
        }

        if (type != null) {
            sc.setParameters("type", type);
        }

        if (accountName != null) {
            sc.setParameters("accountName", accountName);
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

    // This method is used for permissions check for both disk and service offerings
    private boolean isPermissible(Long accountDomainId, Long offeringDomainId) {

        if (accountDomainId == offeringDomainId) {
            return true; // account and service offering in same domain
        }

        DomainVO domainRecord = _domainDao.findById(accountDomainId);

        if (domainRecord != null) {
            while (true) {
                if (domainRecord.getId() == offeringDomainId) {
                    return true;
                }

                // try and move on to the next domain
                if (domainRecord.getParent() != null) {
                    domainRecord = _domainDao.findById(domainRecord.getParent());
                } else {
                    break;
                }
            }
        }

        return false;
    }

    @Override
    public List<ServiceOfferingVO> searchForServiceOfferings(ListServiceOfferingsCmd cmd) {

        // Note
        // The list method for offerings is being modified in accordance with discussion with Will/Kevin
        // For now, we will be listing the following based on the usertype
        // 1. For root, we will list all offerings
        // 2. For domainAdmin and regular users, we will list everything in their domains+parent domains ... all the way till
        // root
    	Boolean isAscending = Boolean.parseBoolean(_configDao.getValue("sortkey.algorithm"));
    	isAscending = (isAscending == null ? true : isAscending);
        Filter searchFilter = new Filter(ServiceOfferingVO.class, "sortKey", isAscending, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<ServiceOfferingVO> sc = _offeringsDao.createSearchCriteria();

        Account caller = UserContext.current().getCaller();
        Object name = cmd.getServiceOfferingName();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Long vmId = cmd.getVirtualMachineId();
        Long domainId = cmd.getDomainId();
        Boolean isSystem = cmd.getIsSystem();
        String vm_type_str = cmd.getSystemVmType();

        if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN && isSystem) {
            throw new InvalidParameterValueException("Only ROOT admins can access system's offering");
        } 
        
        // Keeping this logic consistent with domain specific zones
        // if a domainId is provided, we just return the so associated with this domain
        if (domainId != null && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            // check if the user's domain == so's domain || user's domain is a child of so's domain
            if (!isPermissible(caller.getDomainId(), domainId)) {
                throw new PermissionDeniedException("The account:" + caller.getAccountName() + " does not fall in the same domain hierarchy as the service offering");
            }
        }

        // For non-root users
        if ((caller.getType() == Account.ACCOUNT_TYPE_NORMAL || caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            if (isSystem){
                throw new InvalidParameterValueException("Only root admins can access system's offering");
            }
            return searchServiceOfferingsInternal(caller, name, id, vmId, keyword, searchFilter);
        }

        // for root users, the existing flow
        if (caller.getDomainId() != 1 && isSystem){ //NON ROOT admin
            throw new InvalidParameterValueException("Non ROOT admins cannot access system's offering");
        }

        if (keyword != null) {
            SearchCriteria<ServiceOfferingVO> ssc = _offeringsDao.createSearchCriteria();
            ssc.addOr("displayText", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        } else if (vmId != null) {
            UserVmVO vmInstance = _userVmDao.findById(vmId);
            if ((vmInstance == null) || (vmInstance.getRemoved() != null)) {
                throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
            }
            
            _accountMgr.checkAccess(caller, null, vmInstance);

            ServiceOfferingVO offering = _offeringsDao.findByIdIncludingRemoved(vmInstance.getServiceOfferingId());
            sc.addAnd("id", SearchCriteria.Op.NEQ, offering.getId());

            // Only return offerings with the same Guest IP type and storage pool preference
            // sc.addAnd("guestIpType", SearchCriteria.Op.EQ, offering.getGuestIpType());
            sc.addAnd("useLocalStorage", SearchCriteria.Op.EQ, offering.getUseLocalStorage());
        }
        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }
        
        if (isSystem != null) {
            sc.addAnd("systemUse", SearchCriteria.Op.EQ, isSystem);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }
        
        if (domainId != null) {
            sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        }
        
        if (vm_type_str != null){
            sc.addAnd("vm_type", SearchCriteria.Op.EQ, vm_type_str);
        }

        sc.addAnd("systemUse", SearchCriteria.Op.EQ, isSystem);
        sc.addAnd("removed", SearchCriteria.Op.NULL);
        return _offeringsDao.search(sc, searchFilter);

    }

    private List<ServiceOfferingVO> searchServiceOfferingsInternal(Account account, Object name, Object id, Long vmId, Object keyword, Filter searchFilter) {

        // it was decided to return all offerings for the user's domain, and everything above till root (for normal user or
        // domain admin)
        // list all offerings belonging to this domain, and all of its parents
        // check the parent, if not null, add offerings for that parent to list
        List<ServiceOfferingVO> sol = new ArrayList<ServiceOfferingVO>();
        DomainVO domainRecord = _domainDao.findById(account.getDomainId());
        boolean includePublicOfferings = true;
        if (domainRecord != null) {
            while (true) {
                if (id != null) {
                    ServiceOfferingVO so = _offeringsDao.findById((Long) id);
                    if (so != null) {
                        sol.add(so);
                    }
                    return sol;
                }

                SearchCriteria<ServiceOfferingVO> sc = _offeringsDao.createSearchCriteria();

                if (keyword != null) {
                    includePublicOfferings = false;
                    SearchCriteria<ServiceOfferingVO> ssc = _offeringsDao.createSearchCriteria();
                    ssc.addOr("displayText", SearchCriteria.Op.LIKE, "%" + keyword + "%");
                    ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

                    sc.addAnd("name", SearchCriteria.Op.SC, ssc);
                } else if (vmId != null) {
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

                    sc.addAnd("useLocalStorage", SearchCriteria.Op.EQ, offering.getUseLocalStorage());
                }

                // if (id != null) {
                // includePublicOfferings = false;
                // sc.addAnd("id", SearchCriteria.Op.EQ, id);
                // }

                if (name != null) {
                    includePublicOfferings = false;
                    sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
                }
                sc.addAnd("systemUse", SearchCriteria.Op.EQ, false);

                // for this domain
                sc.addAnd("domainId", SearchCriteria.Op.EQ, domainRecord.getId());
                
                //don't return removed service offerings
                sc.addAnd("removed", SearchCriteria.Op.NULL);

                // search and add for this domain
                sol.addAll(_offeringsDao.search(sc, searchFilter));

                // try and move on to the next domain
                if (domainRecord.getParent() != null) {
                    domainRecord = _domainDao.findById(domainRecord.getParent());
                } else {
                    break;// now we got all the offerings for this user/dom adm
                }
            }
        } else {
            s_logger.error("Could not find the domainId for account:" + account.getAccountName());
            throw new CloudAuthenticationException("Could not find the domainId for account:" + account.getAccountName());
        }

        // add all the public offerings to the sol list before returning
        if (includePublicOfferings) {
            sol.addAll(_offeringsDao.findPublicServiceOfferings());
        }

        return sol;
    }

    @Override
    public List<ClusterVO> searchForClusters(ListClustersCmd cmd) {
        Filter searchFilter = new Filter(ClusterVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<ClusterVO> sc = _clusterDao.createSearchCriteria();

        Object id = cmd.getId();
        Object name = cmd.getClusterName();
        Object podId = cmd.getPodId();
        Long zoneId = cmd.getZoneId();
        Object hypervisorType = cmd.getHypervisorType();
        Object clusterType = cmd.getClusterType();
        Object allocationState = cmd.getAllocationState();

        zoneId = _accountMgr.checkAccessAndSpecifyAuthority(UserContext.current().getCaller(), zoneId);

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

        if (hypervisorType != null) {
            sc.addAnd("hypervisorType", SearchCriteria.Op.EQ, hypervisorType);
        }

        if (clusterType != null) {
            sc.addAnd("clusterType", SearchCriteria.Op.EQ, clusterType);
        }

        if (allocationState != null) {
            sc.addAnd("allocationState", SearchCriteria.Op.EQ, allocationState);
        }

        return _clusterDao.search(sc, searchFilter);
    }

    @Override
    public List<HostVO> searchForServers(ListHostsCmd cmd) {

        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(UserContext.current().getCaller(), cmd.getZoneId());
        Object name = cmd.getHostName();
        Object type = cmd.getType();
        Object state = cmd.getState();
        Object pod = cmd.getPodId();
        Object cluster = cmd.getClusterId();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Object allocationState = cmd.getAllocationState();

        return searchForServers(cmd.getStartIndex(), cmd.getPageSizeVal(), name, type, state, zoneId, pod, cluster, id, keyword, allocationState);
    }

    @Override
    public Pair<List<? extends Host>, List<? extends Host>> listHostsForMigrationOfVM(Long vmId, Long startIndex, Long pageSize) {
        // access check - only root admin can migrate VM
        Account caller = UserContext.current().getCaller();
        if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Caller is not a root admin, permission denied to migrate the VM");
            }
            throw new PermissionDeniedException("No permission to migrate VM, Only Root Admin can migrate a VM!");
        }
        
        VMInstanceVO vm = _vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to find the VM by id=" + vmId);
        }        
        // business logic
        if (vm.getState() != State.Running) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is not Running, unable to migrate the vm " + vm);
            }
            throw new InvalidParameterValueException("VM is not Running, unable to migrate the vm " + vm);
        }

        if (!vm.getHypervisorType().equals(HypervisorType.XenServer) && !vm.getHypervisorType().equals(HypervisorType.VMware) && !vm.getHypervisorType().equals(HypervisorType.KVM) && !vm.getHypervisorType().equals(HypervisorType.Ovm)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(vm + " is not XenServer/VMware/KVM/OVM, cannot migrate this VM.");
            }
            throw new InvalidParameterValueException("Unsupported Hypervisor Type for VM migration, we support XenServer/VMware/KVM only");
        }
        ServiceOfferingVO svcOffering = _offeringsDao.findById(vm.getServiceOfferingId());
        if (svcOffering.getUseLocalStorage()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(vm + " is using Local Storage, cannot migrate this VM.");
            }
            throw new InvalidParameterValueException("Unsupported operation, VM uses Local storage, cannot migrate");
        }
        long srcHostId = vm.getHostId();
        Host srcHost = _hostDao.findById(srcHostId);
        if (srcHost == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find the host with id: " + srcHostId + " of this VM:" + vm);
            }
            throw new InvalidParameterValueException("Unable to find the host with id: " + srcHostId + " of this VM:" + vm);
        }
        Long cluster = srcHost.getClusterId();
        Type hostType = srcHost.getType();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Searching for all hosts in cluster: " + cluster + " for migrating VM " + vm);
        }

        List<? extends Host> allHostsInCluster = searchForServers(startIndex, pageSize, null, hostType, null, null, null, cluster, null, null, null);
        // filter out the current host
        allHostsInCluster.remove(srcHost);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Other Hosts in this cluster: " + allHostsInCluster);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Calling HostAllocators to search for hosts in cluster: " + cluster + " having enough capacity and suitable for migration");
        }

        
        List<Host> suitableHosts = new ArrayList<Host>();
        Enumeration<HostAllocator> enHost = _hostAllocators.enumeration();

        VirtualMachineProfile<VMInstanceVO> vmProfile = new VirtualMachineProfileImpl<VMInstanceVO>(vm);

        DataCenterDeployment plan = new DataCenterDeployment(srcHost.getDataCenterId(), srcHost.getPodId(), srcHost.getClusterId(), null, null, null);
        ExcludeList excludes = new ExcludeList();
        excludes.addHost(srcHostId);
        while (enHost.hasMoreElements()) {
            final HostAllocator allocator = enHost.nextElement();
            suitableHosts = allocator.allocateTo(vmProfile, plan, Host.Type.Routing, excludes, HostAllocator.RETURN_UPTO_ALL, false);
            if (suitableHosts != null && !suitableHosts.isEmpty()) {
                break;
            }
        }

        if(suitableHosts.isEmpty()){
            s_logger.debug("No suitable hosts found");
        }else{
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Hosts having capacity and suitable for migration: " + suitableHosts);
            }
        }

        return new Pair<List<? extends Host>, List<? extends Host>>(allHostsInCluster, suitableHosts);
    }

    private List<HostVO> searchForServers(Long startIndex, Long pageSize, Object name, Object type, Object state, Object zone, Object pod, Object cluster, Object id, Object keyword,
            Object allocationState) {
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
            sc.addAnd("type", SearchCriteria.Op.LIKE, "%" + type);
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

        if (allocationState != null) {
            sc.addAnd("hostAllocationState", SearchCriteria.Op.EQ, allocationState);
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
        Object allocationState = cmd.getAllocationState();

        zoneId = _accountMgr.checkAccessAndSpecifyAuthority(UserContext.current().getCaller(), zoneId);

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

        if (allocationState != null) {
            sc.addAnd("allocationState", SearchCriteria.Op.EQ, allocationState);
        }

        return _hostPodDao.search(sc, searchFilter);
    }

    @Override
    public List<VlanVO> searchForVlans(ListVlanIpRangesCmd cmd) {
        // If an account name and domain ID are specified, look up the account
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long accountId = null;
        Long networkId = cmd.getNetworkId();
        Boolean forVirtual = cmd.getForVirtualNetwork();
        String vlanType = null;
        Long projectId = cmd.getProjectId();
        Long physicalNetworkId = cmd.getPhysicalNetworkId();

        if (accountName != null && domainId != null) {
            if (projectId != null) {
                throw new InvalidParameterValueException("Account and projectId can't be specified together");
            }
            Account account = _accountDao.findActiveAccount(accountName, domainId);
            if (account == null) {
                throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
            } else {
                accountId = account.getId();
            }
        }

        if (forVirtual != null) {
            if (forVirtual) {
                vlanType = VlanType.VirtualNetwork.toString();
            } else {
                vlanType = VlanType.DirectAttached.toString();
            }
        }
        
        //set project information
        if (projectId != null) {
            Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                throw new InvalidParameterValueException("Unable to find project by id " + projectId);
            }
            accountId = project.getProjectAccountId();
        }

        Filter searchFilter = new Filter(VlanVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object id = cmd.getId();
        Object vlan = cmd.getVlan();
        Object dataCenterId = cmd.getZoneId();
        Object podId = cmd.getPodId();
        Object keyword = cmd.getKeyword();

        SearchBuilder<VlanVO> sb = _vlanDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("vlan", sb.entity().getVlanTag(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("vlan", sb.entity().getVlanTag(), SearchCriteria.Op.EQ);
        sb.and("networkId", sb.entity().getNetworkId(), SearchCriteria.Op.EQ);
        sb.and("vlanType", sb.entity().getVlanType(), SearchCriteria.Op.EQ);
        sb.and("physicalNetworkId", sb.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);

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

            if (networkId != null) {
                sc.setParameters("networkId", networkId);
            }

            if (accountId != null) {
                sc.setJoinParameters("accountVlanMapSearch", "accountId", accountId);
            }

            if (podId != null) {
                sc.setJoinParameters("podVlanMapSearch", "podId", podId);
            }
            if (vlanType != null) {
                sc.setParameters("vlanType", vlanType);
            }

            if (physicalNetworkId != null) {
                sc.setParameters("physicalNetworkId", physicalNetworkId);
            }
        }

        return _vlanDao.search(sc, searchFilter);
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
    public Set<Pair<Long, Long>> listIsos(ListIsosCmd cmd) throws IllegalArgumentException, InvalidParameterValueException {
        TemplateFilter isoFilter = TemplateFilter.valueOf(cmd.getIsoFilter());
        List<Account> permittedAccounts = new ArrayList<Account>();
        Account caller = UserContext.current().getCaller();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long projectId = cmd.getProjectId();
        
        if (accountName != null && domainId != null) {
            permittedAccounts.add(_accountMgr.finalizeOwner(caller, accountName, domainId, null));
        } 
        
        //set project information
        if (projectId != null) {
            permittedAccounts.clear();
            Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                throw new InvalidParameterValueException("Unable to find project by id " + projectId);
            }
            if (!_projectMgr.canAccessProjectAccount(caller, project.getProjectAccountId())) {
                throw new InvalidParameterValueException("Account " + caller + " can't access project id=" + projectId);
            }
            permittedAccounts.add(_accountMgr.getAccount(project.getProjectAccountId()));
        } else {
            List<Long> permittedAccountIds = _projectMgr.listPermittedProjectAccounts(caller.getId());
            for (Long permittedAccountId : permittedAccountIds) {
                permittedAccounts.add(_accountMgr.getAccount(permittedAccountId));
            }
        } 

        // It is account specific if account is admin type and domainId and accountName are not null
        boolean isAccountSpecific = (isAdmin(caller.getType())) && (accountName != null) && (domainId != null);

        HypervisorType hypervisorType = HypervisorType.getType(cmd.getHypervisor());
        return listTemplates(cmd.getId(), cmd.getIsoName(), cmd.getKeyword(), isoFilter, true, cmd.isBootable(), cmd.getPageSizeVal(), cmd.getStartIndex(), cmd.getZoneId(), hypervisorType, isAccountSpecific,
                true, cmd.listInReadyState(), permittedAccounts, caller);
    }

    @Override
    public Set<Pair<Long, Long>> listTemplates(ListTemplatesCmd cmd) throws IllegalArgumentException, InvalidParameterValueException {
        TemplateFilter templateFilter = TemplateFilter.valueOf(cmd.getTemplateFilter());
        List<Account> permittedAccounts = new ArrayList<Account>();
        Account caller = UserContext.current().getCaller();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long projectId = cmd.getProjectId();
        
        if (accountName != null && domainId != null) {
            permittedAccounts.add(_accountMgr.finalizeOwner(caller, accountName, domainId, null));
        } else {
            permittedAccounts.add(caller);
        }
        
        //set project information
        if (projectId != null) {
            permittedAccounts.clear();
            Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                throw new InvalidParameterValueException("Unable to find project by id " + projectId);
            }
            if (!_projectMgr.canAccessProjectAccount(caller, project.getProjectAccountId())) {
                throw new InvalidParameterValueException("Account " + caller + " can't access project id=" + projectId);
            }
            permittedAccounts.add(_accountMgr.getAccount(project.getProjectAccountId()));
        } else {
            List<Long> permittedAccountIds = _projectMgr.listPermittedProjectAccounts(caller.getId());
            for (Long permittedAccountId : permittedAccountIds) {
                permittedAccounts.add(_accountMgr.getAccount(permittedAccountId));
            }
        } 

        // It is account specific if account is admin type and domainId and accountName are not null
        boolean isAccountSpecific = (caller == null || isAdmin(caller.getType())) && (accountName != null) && (domainId != null);
        boolean showDomr = ((templateFilter != TemplateFilter.selfexecutable) && (templateFilter != TemplateFilter.featured));
        HypervisorType hypervisorType = HypervisorType.getType(cmd.getHypervisor());

        return listTemplates(cmd.getId(), cmd.getTemplateName(), cmd.getKeyword(), templateFilter, false, null, cmd.getPageSizeVal(), cmd.getStartIndex(), cmd.getZoneId(), hypervisorType, isAccountSpecific,
                showDomr, cmd.listInReadyState(), permittedAccounts, caller);
    }

    private Set<Pair<Long, Long>> listTemplates(Long templateId, String name, String keyword, TemplateFilter templateFilter, boolean isIso, Boolean bootable, Long pageSize, Long startIndex,
            Long zoneId, HypervisorType hyperType, boolean isAccountSpecific, boolean showDomr, boolean onlyReady, List<Account> permittedAccounts, Account caller) {
        
        VMTemplateVO template = null;
        if (templateId != null) {
            template = _templateDao.findById(templateId);
            if (template == null) {
                throw new InvalidParameterValueException("Please specify a valid template ID.");
            }// If ISO requested then it should be ISO.
            if (isIso && template.getFormat() != ImageFormat.ISO) {
                s_logger.error("Template Id " + templateId + " is not an ISO");
                throw new InvalidParameterValueException("Template Id " + templateId + " is not an ISO");
            }// If ISO not requested then it shouldn't be an ISO.
            if (!isIso && template.getFormat() == ImageFormat.ISO) {
                s_logger.error("Incorrect format of the template id " + templateId);
                throw new InvalidParameterValueException("Incorrect format " + template.getFormat() + " of the template id " + templateId);
            }
        }

        DomainVO domain = null;
        if (!permittedAccounts.isEmpty()) {
            domain = _domainDao.findById(permittedAccounts.get(0).getDomainId());
        } else {
            domain = _domainDao.findById(DomainVO.ROOT_DOMAIN);
        }
        List<HypervisorType> hypers = null;
        if( ! isIso ) {
            hypers =  _resourceMgr.listAvailHypervisorInZone(null, null);
        }
        Set<Pair<Long, Long>> templateZonePairSet = new HashSet<Pair<Long, Long>>();
        if (_swiftMgr.isSwiftEnabled()) {
            if (template == null) {
                templateZonePairSet = _templateDao.searchSwiftTemplates(name, keyword, templateFilter, isIso, hypers, bootable, domain, pageSize, startIndex, zoneId, hyperType, onlyReady, showDomr,
                        permittedAccounts, caller);
                Set<Pair<Long, Long>> templateZonePairSet2 = new HashSet<Pair<Long, Long>>();
                templateZonePairSet2 = _templateDao.searchTemplates(name, keyword, templateFilter, isIso, hypers, bootable, domain, pageSize, startIndex, zoneId, hyperType, onlyReady, showDomr,
                        permittedAccounts, caller);
                for (Pair<Long, Long> tmpltPair : templateZonePairSet2) {
                    if (!templateZonePairSet.contains(new Pair<Long, Long>(tmpltPair.first(), 0L))) {
                        templateZonePairSet.add(tmpltPair);
                    }
                }

            } else {
                // if template is not public, perform permission check here
                if (!template.isPublicTemplate() && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                    Account owner = _accountMgr.getAccount(template.getAccountId());
                    _accountMgr.checkAccess(caller, null, owner);
                }
                templateZonePairSet.add(new Pair<Long, Long>(template.getId(), zoneId));
            }
        } else {
            if (template == null) {
                templateZonePairSet = _templateDao.searchTemplates(name, keyword, templateFilter, isIso, hypers, bootable, domain, pageSize, startIndex, zoneId, hyperType, onlyReady, showDomr,
                        permittedAccounts, caller);
            } else {
                // if template is not public, perform permission check here
                if (!template.isPublicTemplate() && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                    Account owner = _accountMgr.getAccount(template.getAccountId());
                    _accountMgr.checkAccess(caller, null, owner);
                }
                templateZonePairSet.add(new Pair<Long, Long>(template.getId(), zoneId));
            }
        }

        return templateZonePairSet;
    }

    @Override
    public List<AccountVO> searchForAccounts(ListAccountsCmd cmd) {
        Account caller = UserContext.current().getCaller();
        Long domainId = cmd.getDomainId();
        Long accountId = cmd.getId();
        String accountName = cmd.getSearchName();
        Boolean isRecursive = cmd.isRecursive();

        if (isRecursive == null) {
            isRecursive = false;
        }

        if (accountId != null && accountId.longValue() == 1L) {
            // system account should NOT be searchable
            List<AccountVO> emptyList = new ArrayList<AccountVO>();
            return emptyList;
        }
        
        if (accountId != null) {
            Account account = _accountDao.findById(accountId);
            if (account == null) {
                throw new InvalidParameterValueException("Unable to find account by id " + accountId);
            }

            _accountMgr.checkAccess(caller, null, account);
        }
        
        if (domainId != null) {
            Domain domain = _domainDao.findById(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Domain id=" + domainId + " doesn't exist");
            }
            _accountMgr.checkAccess(caller, domain);

            if (accountName != null) {
                Account account = _accountDao.findActiveAccount(accountName, domainId);
                if (account == null) {
                    throw new InvalidParameterValueException("Unable to find account by name " + accountName + " in domain " + domainId);
                }

                _accountMgr.checkAccess(caller, null, account);
            }
        }

        if (isAdmin(caller.getType())) {
            if (domainId == null) {
                domainId = caller.getDomainId();
                isRecursive = true;
            } 
        } else {
            // regular user is constraint to only his account
            accountId = caller.getId();
        }

        Filter searchFilter = new Filter(AccountVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object type = cmd.getAccountType();
        Object state = cmd.getState();
        Object isCleanupRequired = cmd.isCleanupRequired();
        Object keyword = cmd.getKeyword();

        SearchBuilder<AccountVO> sb = _accountDao.createSearchBuilder();
        sb.and("accountName", sb.entity().getAccountName(), SearchCriteria.Op.EQ);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("nid", sb.entity().getId(), SearchCriteria.Op.NEQ);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("needsCleanup", sb.entity().getNeedsCleanup(), SearchCriteria.Op.EQ);
        sb.and("typeNEQ", sb.entity().getType(), SearchCriteria.Op.NEQ);

        if ((domainId != null) && isRecursive) {
            // do a domain LIKE match for the admin case if isRecursive is true
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        } else if ((domainId != null) && !isRecursive) {
            // do a domain EXACT match for the admin case if isRecursive is true
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.EQ);
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
            sc.setParameters("accountName", accountName);
        }

        if (accountId != null) {
            sc.setParameters("id", accountId);
        }

        if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);

            // I want to join on user_vm.domain_id = domain.id where domain.path like 'foo%'
            if (isRecursive) {
                sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
            } else {
                sc.setJoinParameters("domainSearch", "path", domain.getPath());
            }

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
        
        //don't return account of type project to the end user
        sc.setParameters("typeNEQ", 5);

        return _accountDao.search(sc, searchFilter);
    }

    @Override
    public VMTemplateVO updateTemplate(UpdateIsoCmd cmd) {
        return updateTemplateOrIso(cmd);
    }

    @Override
    public VMTemplateVO updateTemplate(UpdateTemplateCmd cmd) {
        return updateTemplateOrIso(cmd);
    }

    private VMTemplateVO updateTemplateOrIso(UpdateTemplateOrIsoCmd cmd) {
        Long id = cmd.getId();
        String name = cmd.getTemplateName();
        String displayText = cmd.getDisplayText();
        String format = cmd.getFormat();
        Long guestOSId = cmd.getOsTypeId();
        Boolean passwordEnabled = cmd.isPasswordEnabled();
        Boolean bootable = cmd.isBootable();
        Integer sortKey = cmd.getSortKey();
        Account account = UserContext.current().getCaller();

        // verify that template exists
        VMTemplateVO template = _templateDao.findById(id);
        if (template == null || template.getRemoved() != null) {
            throw new InvalidParameterValueException("unable to find template/iso with id " + id);
        }

        // Don't allow to modify system template
        if (id == Long.valueOf(1)) {
            throw new InvalidParameterValueException("Unable to update template/iso with id " + id);
        }

        // do a permission check
        _accountMgr.checkAccess(account, AccessType.ModifyEntry, template);

        boolean updateNeeded = !(name == null && displayText == null && format == null && guestOSId == null && passwordEnabled == null && bootable == null && sortKey == null);
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
        
        if (sortKey != null) {
        	template.setSortKey(sortKey);
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
    public List<EventVO> searchForEvents(ListEventsCmd cmd) {
        Account caller = UserContext.current().getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();
        boolean isAdmin = false;
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long projectId = cmd.getProjectId();

        if ((caller == null) || isAdmin(caller.getType())) {
            isAdmin = true;
            // validate domainId before proceeding
            if (domainId != null) {
                if ((caller != null) && !_domainDao.isChildDomain(caller.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Invalid domain id (" + domainId + ") given, unable to list events.");
                }

                if (accountName != null) {
                    Account userAccount = _accountDao.findAccount(accountName, domainId);
                    if (userAccount != null) {
                        permittedAccounts.add(userAccount.getId());
                    } else {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                }
            } else {
                domainId = ((caller == null) ? DomainVO.ROOT_DOMAIN : caller.getDomainId());
                if (accountName != null) {
                    Account userAccount = _accountDao.findAccount(accountName, domainId);
                    if (userAccount != null) {
                        permittedAccounts.add(userAccount.getId());
                    } else {
                        throw new InvalidParameterValueException("DomainId is not specified. Unable to find account " + accountName + " in default root domain " + domainId);
                    }
                }
            }
        } else {
            permittedAccounts.add(caller.getId());
        }
        
        //set project information
        if (projectId != null) {
            permittedAccounts.clear();
            Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                throw new InvalidParameterValueException("Unable to find project by id " + projectId);
            }
            if (!_projectMgr.canAccessProjectAccount(caller, project.getProjectAccountId())) {
                throw new InvalidParameterValueException("Account " + caller + " can't access project id=" + projectId);
            }
            permittedAccounts.add(project.getProjectAccountId());
        } else if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL){
            permittedAccounts.addAll(_projectMgr.listPermittedProjectAccounts(caller.getId()));
        }

        Filter searchFilter = new Filter(EventVO.class, "createDate", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object id = cmd.getId();
        Object type = cmd.getType();
        Object level = cmd.getLevel();
        Date startDate = cmd.getStartDate();
        Date endDate = cmd.getEndDate();
        Object keyword = cmd.getKeyword();
        Integer entryTime = cmd.getEntryTime();
        Integer duration = cmd.getDuration();

        if ((entryTime != null) && (duration != null)) {
            if (entryTime <= duration) {
                throw new InvalidParameterValueException("Entry time must be greater than duration");
            }
            return listPendingEvents(entryTime, duration);
        }

        SearchBuilder<EventVO> sb = _eventDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("levelL", sb.entity().getLevel(), SearchCriteria.Op.LIKE);
        sb.and("levelEQ", sb.entity().getLevel(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("accountName", sb.entity().getAccountName(), SearchCriteria.Op.LIKE);
        sb.and("domainIdEQ", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
        sb.and("createDateB", sb.entity().getCreateDate(), SearchCriteria.Op.BETWEEN);
        sb.and("createDateG", sb.entity().getCreateDate(), SearchCriteria.Op.GTEQ);
        sb.and("createDateL", sb.entity().getCreateDate(), SearchCriteria.Op.LTEQ);

        if ((permittedAccounts.isEmpty()) && (accountName == null) && (domainId != null) && isAdmin) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<EventVO> sc = sb.create();
        if (id != null) {
            sc.setParameters("id", id);
        }
        if (keyword != null) {
            SearchCriteria<EventVO> ssc = _eventDao.createSearchCriteria();
            ssc.addOr("type", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("level", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("level", SearchCriteria.Op.SC, ssc);
        }

        if (level != null) {
            sc.setParameters("levelEQ", level);
        }

        if (!permittedAccounts.isEmpty()) {
            sc.setParameters("accountId", permittedAccounts.toArray());
        } else if (domainId != null) {
            if (accountName != null) {
                sc.setParameters("domainIdEQ", domainId);
                sc.setParameters("accountName", "%" + accountName + "%");
            } else if (isAdmin) {
                DomainVO domain = _domainDao.findById(domainId);
                sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
            }
        }

        if (type != null) {
            sc.setParameters("type", type);
        }

        if (startDate != null && endDate != null) {            
            sc.setParameters("createDateB", startDate, endDate);
        } else if (startDate != null) {            
            sc.setParameters("createDateG", startDate);
        } else if (endDate != null) {
            sc.setParameters("createDateL", endDate);
        }

        return _eventDao.searchAllEvents(sc, searchFilter);
    }


    @Override
    public List<DomainRouterVO> searchForRouters(ListRoutersCmd cmd) {
        Account caller = UserContext.current().getCaller();
        Pair<List<Long>, Long> accountDomainPair = _accountMgr.finalizeAccountDomainForList(caller, cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId());
        List<Long> permittedAccounts = accountDomainPair.first();
        Long domainId = accountDomainPair.second();

        Filter searchFilter = new Filter(DomainRouterVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object id = cmd.getId();
        Object name = cmd.getRouterName();
        Object state = cmd.getState();
        Object zone = cmd.getZoneId();
        Object pod = cmd.getPodId();
        Object hostId = cmd.getHostId();
        Object keyword = cmd.getKeyword();
        Object networkId = cmd.getNetworkId();

        SearchBuilder<DomainRouterVO> sb = _routerDao.createSearchBuilder();
        sb.and("name", sb.entity().getHostName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterIdToDeployIn(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodIdToDeployIn(), SearchCriteria.Op.EQ);
        sb.and("hostId", sb.entity().getHostId(), SearchCriteria.Op.EQ);

        if ((permittedAccounts.isEmpty()) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        if (networkId != null) {
            SearchBuilder<NicVO> nicSearch = _nicDao.createSearchBuilder();
            nicSearch.and("networkId", nicSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);

            SearchBuilder<NetworkVO> networkSearch = _networkDao.createSearchBuilder();
            networkSearch.and("networkId", networkSearch.entity().getId(), SearchCriteria.Op.EQ);
            nicSearch.join("networkSearch", networkSearch, nicSearch.entity().getNetworkId(), networkSearch.entity().getId(), JoinBuilder.JoinType.INNER);

            sb.join("nicSearch", nicSearch, sb.entity().getId(), nicSearch.entity().getInstanceId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<DomainRouterVO> sc = sb.create();
        if (keyword != null) {
            SearchCriteria<DomainRouterVO> ssc = _routerDao.createSearchCriteria();
            ssc.addOr("hostName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("instanceName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("hostName", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (!permittedAccounts.isEmpty()) {
            sc.setParameters("accountId", permittedAccounts.toArray());
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

        if (networkId != null) {
            sc.setJoinParameters("nicSearch", "networkId", networkId);
        }

        return _routerDao.search(sc, searchFilter);
    }

    @Override
    public List<VolumeVO> searchForVolumes(ListVolumesCmd cmd) {
        Account caller = UserContext.current().getCaller();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        List<Long> permittedAccounts = new ArrayList<Long>();
        boolean isAdmin = false;
        Boolean isRecursive = cmd.isRecursive();
        Long projectId = cmd.getProjectId();

        if (isRecursive == null) {
            isRecursive = false;
        }

        if ((caller == null) || isAdmin(caller.getType())) {
            isAdmin = true;
            if (domainId != null) {
                if ((caller != null) && !_domainDao.isChildDomain(caller.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Invalid domain id (" + domainId + ") given, unable to list volumes.");
                }
                if (accountName != null) {
                    Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                    if (userAccount != null) {
                        permittedAccounts.add(userAccount.getId());
                    } else {
                        throw new InvalidParameterValueException("could not find account " + accountName + " in domain " + domainId);
                    }
                }
            } else {
                domainId = ((caller == null) ? DomainVO.ROOT_DOMAIN : caller.getDomainId());
                isRecursive = true;
            }
        } else {
            permittedAccounts.add(caller.getId());
        }
        
        //set project information
        if (projectId != null) {
            permittedAccounts.clear();
            Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                throw new InvalidParameterValueException("Unable to find project by id " + projectId);
            }
            if (!_projectMgr.canAccessProjectAccount(caller, project.getProjectAccountId())) {
                throw new InvalidParameterValueException("Account " + caller + " can't access project id=" + projectId);
            }
            permittedAccounts.add(project.getProjectAccountId());
        } else if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL){
            permittedAccounts.addAll(_projectMgr.listPermittedProjectAccounts(caller.getId()));
        } 

        Filter searchFilter = new Filter(VolumeVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object id = cmd.getId();
        Long vmInstanceId = cmd.getVirtualMachineId();
        Object name = cmd.getVolumeName();
        Object keyword = cmd.getKeyword();
        Object type = cmd.getType();

        Object zoneId = cmd.getZoneId();
        Object pod = null;
        // Object host = null; TODO
        if (isAdmin) {
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
        sb.and("accountIdIN", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("volumeType", sb.entity().getVolumeType(), SearchCriteria.Op.LIKE);
        sb.and("instanceId", sb.entity().getInstanceId(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);

        // Only return volumes that are not destroyed
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.NEQ);

        SearchBuilder<DiskOfferingVO> diskOfferingSearch = _diskOfferingDao.createSearchBuilder();
        diskOfferingSearch.and("systemUse", diskOfferingSearch.entity().getSystemUse(), SearchCriteria.Op.NEQ);
        sb.join("diskOfferingSearch", diskOfferingSearch, sb.entity().getDiskOfferingId(), diskOfferingSearch.entity().getId(), JoinBuilder.JoinType.LEFTOUTER);

        if (((permittedAccounts.isEmpty()) && (domainId != null) && isRecursive)) {
            // if accountId isn't specified, we can do a domain match for the admin case if isRecursive is true
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        } else if ((permittedAccounts.isEmpty()) && (domainId != null) && !isRecursive) {
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.EQ);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        // display user vm volumes only
        SearchBuilder<VMInstanceVO> vmSearch = _vmInstanceDao.createSearchBuilder();
        vmSearch.and("type", vmSearch.entity().getType(), SearchCriteria.Op.NIN);
        vmSearch.or("nulltype", vmSearch.entity().getType(), SearchCriteria.Op.NULL);
        sb.join("vmSearch", vmSearch, sb.entity().getInstanceId(), vmSearch.entity().getId(), JoinBuilder.JoinType.LEFTOUTER);
        
        // now set the SC criteria...
        SearchCriteria<VolumeVO> sc = sb.create();
        if (keyword != null) {
            SearchCriteria<VolumeVO> ssc = _volumeDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("volumeType", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (!permittedAccounts.isEmpty()) {
            sc.setParameters("accountIdIN", permittedAccounts.toArray());
            sc.setJoinParameters("diskOfferingSearch", "systemUse", 1);
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            if (isRecursive) {
                sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
            } else {
                sc.setJoinParameters("domainSearch", "path", domain.getPath());
            }
        }
        if (type != null) {
            sc.setParameters("volumeType", "%" + type + "%");
        }
        if (vmInstanceId != null) {
            sc.setParameters("instanceId", vmInstanceId);
        }
        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
        }
        if (pod != null) {
            sc.setParameters("podId", pod);
        }

        // Don't return DomR and ConsoleProxy volumes
        sc.setJoinParameters("vmSearch", "type", VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm, VirtualMachine.Type.DomainRouter);

        // Only return volumes that are not destroyed
        sc.setParameters("state", Volume.State.Destroy);

        return _volumeDao.search(sc, searchFilter);
    }


    @Override
    public List<IPAddressVO> searchForIPAddresses(ListPublicIpAddressesCmd cmd) {
        Account caller = UserContext.current().getCaller();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Object keyword = cmd.getKeyword();
        Long projectId = cmd.getProjectId();
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        Long associatedNetworkId = cmd.getAssociatedNetworkId();
        
        List<Long> permittedAccounts = new ArrayList<Long>();

        if (isAdmin(caller.getType())) {
            // validate domainId before proceeding
            if (domainId != null) {
                Domain domain = _domainDao.findById(domainId);
                if (domain == null) {
                    throw new InvalidParameterValueException("Unable to find domain by id " + domainId);
                }
                _accountMgr.checkAccess(caller, domain);

                if (accountName != null) {
                    Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                    if (userAccount != null) {
                        permittedAccounts.add(userAccount.getId());
                    } else {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                }
            } else if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
                domainId = caller.getDomainId();
            }
        } else {
            permittedAccounts.add(caller.getId());
        }
        
        //set project information
        if (projectId != null) {
            permittedAccounts.clear();
            Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                throw new InvalidParameterValueException("Unable to find project by id " + projectId);
            }
            if (!_projectMgr.canAccessProjectAccount(caller, project.getProjectAccountId())) {
                throw new InvalidParameterValueException("Account " + caller + " can't access project id=" + projectId);
            }
            permittedAccounts.add(project.getProjectAccountId());
        } else if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL){
            permittedAccounts.addAll(_projectMgr.listPermittedProjectAccounts(caller.getId()));
        } 

        if (permittedAccounts.isEmpty() && keyword != null) {
            Account userAccount = _accountDao.findActiveAccount((String) keyword, domainId);
            if (userAccount != null) {
                permittedAccounts.add(userAccount.getId());
            }
        }

        Boolean isAllocated = cmd.isAllocatedOnly();
        if (isAllocated == null) {
            isAllocated = Boolean.TRUE;
        }

        Filter searchFilter = new Filter(IPAddressVO.class, "address", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object zone = cmd.getZoneId();
        Object address = cmd.getIpAddress();
        Object vlan = cmd.getVlanId();
        Object forVirtualNetwork = cmd.isForVirtualNetwork();
        Object forLoadBalancing = cmd.isForLoadBalancing();
        Object ipId = cmd.getId();

        SearchBuilder<IPAddressVO> sb = _publicIpAddressDao.createSearchBuilder();
        sb.and("accountIdIN", sb.entity().getAllocatedToAccountId(), SearchCriteria.Op.IN);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("address", sb.entity().getAddress(), SearchCriteria.Op.EQ);
        sb.and("vlanDbId", sb.entity().getVlanId(), SearchCriteria.Op.EQ);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("physicalNetworkId", sb.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);
        sb.and("associatedNetworkId", sb.entity().getAssociatedWithNetworkId(), SearchCriteria.Op.EQ);

        if ((permittedAccounts.isEmpty()) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getAllocatedInDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }
        
        if (forLoadBalancing != null && (Boolean)forLoadBalancing) {
            SearchBuilder<LoadBalancerVO> lbSearch = _loadbalancerDao.createSearchBuilder();
            sb.join("lbSearch", lbSearch, sb.entity().getId(), lbSearch.entity().getSourceIpAddressId(), JoinType.INNER);
            sb.groupBy(sb.entity().getId());
        }

        if (keyword != null && address == null) {
            sb.and("addressLIKE", sb.entity().getAddress(), SearchCriteria.Op.LIKE);
        }

        SearchBuilder<VlanVO> vlanSearch = _vlanDao.createSearchBuilder();
        vlanSearch.and("vlanType", vlanSearch.entity().getVlanType(), SearchCriteria.Op.EQ);
        sb.join("vlanSearch", vlanSearch, sb.entity().getVlanId(), vlanSearch.entity().getId(), JoinBuilder.JoinType.INNER);

        if ((isAllocated != null) && (isAllocated == true)) {
            sb.and("allocated", sb.entity().getAllocatedTime(), SearchCriteria.Op.NNULL);
        }

        SearchCriteria<IPAddressVO> sc = sb.create();
        if (!permittedAccounts.isEmpty()) {
            sc.setParameters("accountIdIN", permittedAccounts.toArray());
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
        }

        VlanType vlanType = null;
        if (forVirtualNetwork != null) {
            vlanType = (Boolean) forVirtualNetwork ? VlanType.VirtualNetwork : VlanType.DirectAttached;
        } else {
            vlanType = VlanType.VirtualNetwork;
        }

        sc.setJoinParameters("vlanSearch", "vlanType", vlanType);

        if (zone != null) {
            sc.setParameters("dataCenterId", zone);
        }

        if (ipId != null) {
            sc.setParameters("id", ipId);
        }

        if (address == null && keyword != null) {
            sc.setParameters("addressLIKE", "%" + keyword + "%");
        }

        if (address != null) {
            sc.setParameters("address", address);
        }

        if (vlan != null) {
            sc.setParameters("vlanDbId", vlan);
        }
        
        if (physicalNetworkId != null) {
            sc.setParameters("physicalNetworkId", physicalNetworkId);
        }
        
        if (associatedNetworkId != null) {
        	sc.setParameters("associatedNetworkId", associatedNetworkId);
        }

        return _publicIpAddressDao.search(sc, searchFilter);
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
            sc.setParameters("id", id);
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
            sc.setParameters("id", id);
        }

        return _guestOSCategoryDao.search(sc, searchFilter);
    }

    @Override
    public ConsoleProxyInfo getConsoleProxyForUserVm(long dataCenterId, long userVmId) {
        return _consoleProxyMgr.assignProxy(dataCenterId, userVmId);
    }

    @ActionEvent(eventType = EventTypes.EVENT_PROXY_START, eventDescription = "starting console proxy Vm", async = true)
    private ConsoleProxyVO startConsoleProxy(long instanceId) {
        return _consoleProxyMgr.startProxy(instanceId);
    }

    @ActionEvent(eventType = EventTypes.EVENT_PROXY_STOP, eventDescription = "stopping console proxy Vm", async = true)
    private ConsoleProxyVO stopConsoleProxy(VMInstanceVO systemVm, boolean isForced) throws ResourceUnavailableException, OperationTimedoutException, ConcurrentOperationException {

        User caller = _userDao.findById(UserContext.current().getCallerUserId());

        if (_itMgr.advanceStop(systemVm, isForced, caller, UserContext.current().getCaller())) {
            return _consoleProxyDao.findById(systemVm.getId());
        }
        return null;
    }

    @ActionEvent(eventType = EventTypes.EVENT_PROXY_REBOOT, eventDescription = "rebooting console proxy Vm", async = true)
    private ConsoleProxyVO rebootConsoleProxy(long instanceId) {
        _consoleProxyMgr.rebootProxy(instanceId);
        return _consoleProxyDao.findById(instanceId);
    }

    @ActionEvent(eventType = EventTypes.EVENT_PROXY_DESTROY, eventDescription = "destroying console proxy Vm", async = true)
    public ConsoleProxyVO destroyConsoleProxy(long instanceId) {
        ConsoleProxyVO proxy = _consoleProxyDao.findById(instanceId);

        if (_consoleProxyMgr.destroyProxy(instanceId)) {
            return proxy;
        }
        return null;
    }

    @Override
    public String getConsoleAccessUrlRoot(long vmId) {
        VMInstanceVO vm = _vmMgr.findById(vmId);
        if (vm != null) {
            ConsoleProxyInfo proxy = getConsoleProxyForUserVm(vm.getDataCenterIdToDeployIn(), vmId);
            if (proxy != null) {
                return proxy.getProxyImageUrl();
            }
        }
        return null;
    }

    @Override
    public Pair<String, Integer> getVncPort(VirtualMachine vm) {
        if (vm.getHostId() == null) {
            s_logger.warn("VM " + vm.getHostName() + " does not have host, return -1 for its VNC port");
            return new Pair<String, Integer>(null, -1);
        }

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Trying to retrieve VNC port from agent about VM " + vm.getHostName());
        }

        GetVncPortAnswer answer = (GetVncPortAnswer) _agentMgr.easySend(vm.getHostId(), new GetVncPortCommand(vm.getId(), vm.getInstanceName()));
        if (answer != null && answer.getResult()) {
            return new Pair<String, Integer>(answer.getAddress(), answer.getPort());
        }

        return new Pair<String, Integer>(null, -1);
    }

    @Override
    public List<DomainVO> searchForDomains(ListDomainsCmd cmd) throws PermissionDeniedException {
        Long domainId = cmd.getId();
        Account account = UserContext.current().getCaller();
        String path = null;

        if (account != null && (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || account.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN)) {
            DomainVO domain = _domainDao.findById(account.getDomainId());
            if (domain != null) {
                path = domain.getPath();
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

        if (path != null) {
            sc.setParameters("path", "%" + path + "%");
        }

        return _domainDao.search(sc, searchFilter);
    }

    @Override
    public List<DomainVO> searchForDomainChildren(ListDomainChildrenCmd cmd) throws PermissionDeniedException {
        Filter searchFilter = new Filter(DomainVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Long domainId = cmd.getId();
        String domainName = cmd.getDomainName();
        Boolean isRecursive = cmd.isRecursive();
        Object keyword = cmd.getKeyword();
        String path = null;

        if (isRecursive == null) {
            isRecursive = false;
        }

        Account account = UserContext.current().getCaller();
        if (account != null) {
            if (domainId != null) {
                if (!_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Unable to list domains children for domain id " + domainId + ", permission denied.");
                }
            } else {
                domainId = account.getDomainId();
            }
        }

        DomainVO domain = _domainDao.findById(domainId);
        if (domain != null && isRecursive) {
            path = domain.getPath();
            domainId = null;
        }

        List<DomainVO> domainList = searchForDomainChildren(searchFilter, domainId, domainName, keyword, path);

        return domainList;
    }

    private List<DomainVO> searchForDomainChildren(Filter searchFilter, Long domainId, String domainName, Object keyword, String path) {
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

        if (path != null) {
            sc.addAnd("path", SearchCriteria.Op.NEQ, path);
            sc.addAnd("path", SearchCriteria.Op.LIKE, path + "%");
        }
         
        return _domainDao.search(sc, searchFilter);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DOMAIN_UPDATE, eventDescription = "updating Domain")
    @DB
    public DomainVO updateDomain(UpdateDomainCmd cmd) {
        Long domainId = cmd.getId();
        String domainName = cmd.getDomainName();
        String networkDomain = cmd.getNetworkDomain();

        // check if domain exists in the system
        DomainVO domain = _domainDao.findById(domainId);
        if (domain == null) {
            throw new InvalidParameterValueException("Unable to find domain " + domainId);
        } else if (domain.getParent() == null && domainName != null) {
            // check if domain is ROOT domain - and deny to edit it with the new name
            throw new InvalidParameterValueException("ROOT domain can not be edited with a new name");
        }

        // check permissions
        Account caller = UserContext.current().getCaller();
        _accountMgr.checkAccess(caller, domain);
        
        //domain name is unique in the cloud
        if (domainName != null) {
            SearchCriteria<DomainVO> sc = _domainDao.createSearchCriteria();
            sc.addAnd("name", SearchCriteria.Op.EQ, domainName);
            List<DomainVO> domains = _domainDao.search(sc, null);
            
            if (!domains.isEmpty()) {
                throw new InvalidParameterValueException("Failed to update domain id=" + domainId + "; domain with name " + domainName + " already exists in the system");
            }
        }

        //validate network domain 
        if (networkDomain != null){
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException(
                        "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                        + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }

        Transaction txn = Transaction.currentTxn();
        
        txn.start();
        
        if (domainName != null) {
            String updatedDomainPath = getUpdatedDomainPath(domain.getPath(), domainName);
            updateDomainChildren(domain, updatedDomainPath);
            domain.setName(domainName);
            domain.setPath(updatedDomainPath);
        }
        
        if (networkDomain != null) {
            domain.setNetworkDomain(networkDomain);
        }
        _domainDao.update(domainId, domain);
        
        txn.commit();

        return _domainDao.findById(domainId);
       
    }

    private String getUpdatedDomainPath(String oldPath, String newName) {
        String[] tokenizedPath = oldPath.split("/");
        tokenizedPath[tokenizedPath.length - 1] = newName;
        StringBuilder finalPath = new StringBuilder();
        for (String token : tokenizedPath) {
            finalPath.append(token);
            finalPath.append("/");
        }
        return finalPath.toString();
    }

    private void updateDomainChildren(DomainVO domain, String updatedDomainPrefix) {
        List<DomainVO> domainChildren = _domainDao.findAllChildren(domain.getPath(), domain.getId());
        // for each child, update the path
        for (DomainVO dom : domainChildren) {
            dom.setPath(dom.getPath().replaceFirst(domain.getPath(), updatedDomainPrefix));
            _domainDao.update(dom.getId(), dom);
        }
    }

    @Override
    public List<? extends Alert> searchForAlerts(ListAlertsCmd cmd) {
        Filter searchFilter = new Filter(AlertVO.class, "lastSent", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<AlertVO> sc = _alertDao.createSearchCriteria();

        Object id = cmd.getId();
        Object type = cmd.getType();
        Object keyword = cmd.getKeyword();

        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(UserContext.current().getCaller(), null);
        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }
        if (zoneId != null) {
            sc.addAnd("data_center_id", SearchCriteria.Op.EQ, zoneId);
        }

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

    	Integer capacityType = cmd.getType();
        Long zoneId = cmd.getZoneId();
        Long podId = cmd.getPodId();
        Long clusterId = cmd.getClusterId();
        Boolean fetchLatest = cmd.getFetchLatest();

        zoneId = _accountMgr.checkAccessAndSpecifyAuthority(UserContext.current().getCaller(), zoneId);
        if (fetchLatest != null && fetchLatest){
        	_alertMgr.recalculateCapacity();
        }
        List<SummedCapacity> summedCapacities = _capacityDao.findCapacityBy(capacityType, zoneId, podId, clusterId);
        List<CapacityVO> capacities = new ArrayList<CapacityVO>();

        for (SummedCapacity summedCapacity : summedCapacities){
        	CapacityVO capacity = new CapacityVO(null, zoneId, podId, clusterId,
        			summedCapacity.getUsedCapacity() + summedCapacity.getReservedCapacity(), 
        			summedCapacity.getTotalCapacity(), summedCapacity.getCapacityType());
        	
        	if ( summedCapacity.getCapacityType() == Capacity.CAPACITY_TYPE_CPU){
        		capacity.setTotalCapacity((long)(summedCapacity.getTotalCapacity() * ApiDBUtils.getCpuOverprovisioningFactor()));
        	}
        	capacities.add(capacity);		
        }

        // op_host_Capacity contains only allocated stats and the real time stats are stored "in memory".
        //Show Sec. Storage only when the api is invoked for the zone layer.
        if ((capacityType == null || capacityType == Capacity.CAPACITY_TYPE_SECONDARY_STORAGE) && podId == null && clusterId == null) {
            capacities.add(_storageMgr.getSecondaryStorageUsedStats(null, zoneId));
        }
        if (capacityType == null || capacityType == Capacity.CAPACITY_TYPE_STORAGE) {
            capacities.add(_storageMgr.getStoragePoolUsedStats(null, clusterId, podId, zoneId));
        }

        return capacities;
    }

    @Override
    public long getMemoryOrCpuCapacityByHost(Long hostId, short capacityType) {

        CapacityVO capacity = _capacityDao.findByHostIdType(hostId, capacityType);
        return capacity == null ? 0 : capacity.getReservedCapacity() + capacity.getUsedCapacity();

    }

    protected boolean templateIsCorrectType(VirtualMachineTemplate template) {
        return true;
    }

    public static boolean isAdmin(short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN) || (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) || (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
    }

    @Override
    @DB
    public boolean updateTemplatePermissions(UpdateTemplatePermissionsCmd cmd) {
        return updateTemplateOrIsoPermissions(cmd);
    }

    @Override
    @DB
    public boolean updateTemplatePermissions(UpdateIsoPermissionsCmd cmd) {
        return updateTemplateOrIsoPermissions(cmd);
    }

    @DB
    protected boolean updateTemplateOrIsoPermissions(UpdateTemplateOrIsoPermissionsCmd cmd) {
        Transaction txn = Transaction.currentTxn();

        // Input validation
        Long id = cmd.getId();
        Account caller = UserContext.current().getCaller();
        List<String> accountNames = cmd.getAccountNames();
        List<Long> projectIds = cmd.getProjectIds();
        Boolean isFeatured = cmd.isFeatured();
        Boolean isPublic = cmd.isPublic();
        Boolean isExtractable = cmd.isExtractable();
        String operation = cmd.getOperation();
        String mediaType = "";

        VMTemplateVO template = _templateDao.findById(id);

        if (template == null || !templateIsCorrectType(template)) {
            throw new InvalidParameterValueException("unable to find " + mediaType + " with id " + id);
        }

        if (cmd instanceof UpdateTemplatePermissionsCmd) {
            mediaType = "template";
            if (template.getFormat().equals(ImageFormat.ISO)) {
                throw new InvalidParameterValueException("Please provide a valid template");
            }
        }
        if (cmd instanceof UpdateIsoPermissionsCmd) {
            mediaType = "iso";
            if (!template.getFormat().equals(ImageFormat.ISO)) {
                throw new InvalidParameterValueException("Please provide a valid iso");
            }
        }
        
        //Can only assign pro
        
        //convert projectIds to accountNames
        if (projectIds != null) {
            for (Long projectId : projectIds) {
                Project project = _projectMgr.getProject(projectId);
                if (project == null) {
                    throw new InvalidParameterValueException("Unable to find project by id " + projectId);
                }
                
                if (!_projectMgr.canAccessProjectAccount(caller, project.getProjectAccountId())) {
                    throw new InvalidParameterValueException("Account " + caller + " can't access project id=" + projectId);
                }
                accountNames.add(_accountMgr.getAccount(project.getProjectAccountId()).getAccountName());
            }
        }

        _accountMgr.checkAccess(caller, AccessType.ModifyEntry, template);

        // If the template is removed throw an error.
        if (template.getRemoved() != null) {
            s_logger.error("unable to update permissions for " + mediaType + " with id " + id + " as it is removed  ");
            throw new InvalidParameterValueException("unable to update permissions for " + mediaType + " with id " + id + " as it is removed ");
        }

        if (id == Long.valueOf(1)) {
            throw new InvalidParameterValueException("unable to update permissions for " + mediaType + " with id " + id);
        }

        boolean isAdmin = isAdmin(caller.getType());
        boolean allowPublicUserTemplates = Boolean.valueOf(_configDao.getValue("allow.public.user.templates"));
        if (!isAdmin && !allowPublicUserTemplates && isPublic != null && isPublic) {
            throw new InvalidParameterValueException("Only private " + mediaType + "s can be created.");
        }

        if (accountNames != null) {
            if ((operation == null) || (!operation.equalsIgnoreCase("add") && !operation.equalsIgnoreCase("remove") && !operation.equalsIgnoreCase("reset"))) {
                throw new InvalidParameterValueException("Invalid operation on accounts, the operation must be either 'add' or 'remove' in order to modify launch permissions."
                        + "  Given operation is: '" + operation + "'");
            }
        }

        Long accountId = template.getAccountId();
        if (accountId == null) {
            // if there is no owner of the template then it's probably already a public template (or domain private template) so
            // publishing to individual users is irrelevant
            throw new InvalidParameterValueException("Update template permissions is an invalid operation on template " + template.getName());
        }

        VMTemplateVO updatedTemplate = _templateDao.createForUpdate();

        if (isPublic != null) {
            updatedTemplate.setPublicTemplate(isPublic.booleanValue());
        }

        if (isFeatured != null) {
            updatedTemplate.setFeatured(isFeatured.booleanValue());
        }
        
       if (isExtractable != null && caller.getType() == Account.ACCOUNT_TYPE_ADMIN) {//Only ROOT admins allowed to change this powerful attribute
           updatedTemplate.setExtractable(isExtractable.booleanValue());
       }else if (isExtractable != null && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
           throw new InvalidParameterValueException("Only ROOT admins are allowed to modify this attribute.");
       }

        _templateDao.update(template.getId(), updatedTemplate);

        Long domainId = caller.getDomainId();
        if ("add".equalsIgnoreCase(operation)) {
            txn.start();
            for (String accountName : accountNames) {
                Account permittedAccount = _accountDao.findActiveAccount(accountName, domainId);
                if (permittedAccount != null) {
                    if (permittedAccount.getId() == caller.getId()) {
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
    public List<String> listTemplatePermissions(ListTemplateOrIsoPermissionsCmd cmd) {
        Account caller = UserContext.current().getCaller();
        Long domainId = cmd.getDomainId();
        String acctName = cmd.getAccountName();
        Long id = cmd.getId();

        if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
            // validate domainId before proceeding
            if (domainId != null) {
                DomainVO domain = _domainDao.findById(domainId);
                if (domain == null) {
                    throw new InvalidParameterValueException("Unable to find domain by id " + domainId);
                }

                _accountMgr.checkAccess(caller, domain);

                if (acctName != null) {
                    Account userAccount = _accountDao.findActiveAccount(acctName, domainId);
                    if (userAccount == null) {
                        throw new PermissionDeniedException("Unable to find account " + acctName + " in domain " + domainId);
                    }
                }
            }
        }

        if (id == Long.valueOf(1)) {
            throw new PermissionDeniedException("unable to list permissions for " + cmd.getMediaType() + " with id " + id);
        }

        VirtualMachineTemplate template = _templateMgr.getTemplate(id);
        if (template == null || !templateIsCorrectType(template)) {
            throw new InvalidParameterValueException("unable to find " + cmd.getMediaType() + " with id " + id);
        }

        if (!template.isPublicTemplate()) {
            _accountMgr.checkAccess(caller, null, template);
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

    private List<DiskOfferingVO> searchDiskOfferingsInternal(Account account, Object name, Object id, Object keyword, Filter searchFilter) {
        // it was decided to return all offerings for the user's domain, and everything above till root (for normal user or
        // domain admin)
        // list all offerings belonging to this domain, and all of its parents
        // check the parent, if not null, add offerings for that parent to list
        List<DiskOfferingVO> dol = new ArrayList<DiskOfferingVO>();
        DomainVO domainRecord = _domainDao.findById(account.getDomainId());
        boolean includePublicOfferings = true;
        if (domainRecord != null) {
            while (true) {
                SearchBuilder<DiskOfferingVO> sb = _diskOfferingDao.createSearchBuilder();

                sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
                sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
                sb.and("removed", sb.entity().getRemoved(), SearchCriteria.Op.NULL);

                SearchCriteria<DiskOfferingVO> sc = sb.create();
                if (keyword != null) {
                    includePublicOfferings = false;
                    SearchCriteria<DiskOfferingVO> ssc = _diskOfferingDao.createSearchCriteria();
                    ssc.addOr("displayText", SearchCriteria.Op.LIKE, "%" + keyword + "%");
                    ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

                    sc.addAnd("name", SearchCriteria.Op.SC, ssc);
                }

                if (name != null) {
                    includePublicOfferings = false;
                    sc.setParameters("name", "%" + name + "%");
                }

                if (id != null) {
                    includePublicOfferings = false;
                    sc.setParameters("id", id);
                }

                // for this domain
                sc.addAnd("domainId", SearchCriteria.Op.EQ, domainRecord.getId());

                // search and add for this domain
                dol.addAll(_diskOfferingDao.search(sc, searchFilter));

                // try and move on to the next domain
                if (domainRecord.getParent() != null) {
                    domainRecord = _domainDao.findById(domainRecord.getParent());
                } else {
                    break;// now we got all the offerings for this user/dom adm
                }
            }
        } else {
            s_logger.error("Could not find the domainId for account:" + account.getAccountName());
            throw new CloudAuthenticationException("Could not find the domainId for account:" + account.getAccountName());
        }

        // add all the public offerings to the sol list before returning
        if (includePublicOfferings) {
            dol.addAll(_diskOfferingDao.findPublicDiskOfferings());
        }

        return dol;

    }

    @Override
    public List<DiskOfferingVO> searchForDiskOfferings(ListDiskOfferingsCmd cmd) {
        // Note
        // The list method for offerings is being modified in accordance with discussion with Will/Kevin
        // For now, we will be listing the following based on the usertype
        // 1. For root, we will list all offerings
        // 2. For domainAdmin and regular users, we will list everything in their domains+parent domains ... all the way till
        // root

    	Boolean isAscending = Boolean.parseBoolean(_configDao.getValue("sortkey.algorithm"));
    	isAscending = (isAscending == null ? true : isAscending);
        Filter searchFilter = new Filter(DiskOfferingVO.class, "sortKey", isAscending, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<DiskOfferingVO> sb = _diskOfferingDao.createSearchBuilder();

        // SearchBuilder and SearchCriteria are now flexible so that the search builder can be built with all possible
        // search terms and only those with criteria can be set. The proper SQL should be generated as a result.
        Account account = UserContext.current().getCaller();
        Object name = cmd.getDiskOfferingName();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Long domainId = cmd.getDomainId();
        // Keeping this logic consistent with domain specific zones
        // if a domainId is provided, we just return the disk offering associated with this domain
        if (domainId != null) {
            if (account.getType() == Account.ACCOUNT_TYPE_ADMIN) {
                return _diskOfferingDao.listByDomainId(domainId);// no perm check
            } else {
                // check if the user's domain == do's domain || user's domain is a child of so's domain
                if (isPermissible(account.getDomainId(), domainId)) {
                    // perm check succeeded
                    return _diskOfferingDao.listByDomainId(domainId);
                } else {
                    throw new PermissionDeniedException("The account:" + account.getAccountName() + " does not fall in the same domain hierarchy as the disk offering");
                }
            }
        }

        // For non-root users
        if ((account.getType() == Account.ACCOUNT_TYPE_NORMAL || account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || account.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            return searchDiskOfferingsInternal(account, name, id, keyword, searchFilter);
        }

        // For root users, preserving existing flow
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("removed", sb.entity().getRemoved(), SearchCriteria.Op.NULL);

        // FIXME: disk offerings should search back up the hierarchy for available disk offerings...
        /*
         * sb.addAnd("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ); if (domainId != null) {
         * SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder(); domainSearch.addAnd("path",
         * domainSearch.entity().getPath(), SearchCriteria.Op.LIKE); sb.join("domainSearch", domainSearch,
         * sb.entity().getDomainId(), domainSearch.entity().getId()); }
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

        // FIXME: disk offerings should search back up the hierarchy for available disk offerings...
        /*
         * if (domainId != null) { sc.setParameters("domainId", domainId); // //DomainVO domain =
         * _domainDao.findById((Long)domainId); // // I want to join on user_vm.domain_id = domain.id where domain.path like
         * 'foo%' //sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%"); // }
         */

        return _diskOfferingDao.search(sc, searchFilter);
    }

    @Override
    public String[] getApiConfig() {
        return new String[] { "commands.properties" };
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
                    s_logger.debug("Deleting events older than: " + purgeTime.toString());
                    List<EventVO> oldEvents = _eventDao.listOlderEvents(purgeTime);
                    s_logger.debug("Found " + oldEvents.size() + " events to be purged");
                    for (EventVO event : oldEvents) {
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
    public List<? extends StoragePoolVO> searchForStoragePools(ListStoragePoolsCmd cmd) {

        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(UserContext.current().getCaller(), cmd.getZoneId());
        Criteria c = new Criteria("id", Boolean.TRUE, cmd.getStartIndex(), cmd.getPageSizeVal());
        c.addCriteria(Criteria.ID, cmd.getId());
        c.addCriteria(Criteria.NAME, cmd.getStoragePoolName());
        c.addCriteria(Criteria.CLUSTERID, cmd.getClusterId());
        c.addCriteria(Criteria.ADDRESS, cmd.getIpAddress());
        c.addCriteria(Criteria.KEYWORD, cmd.getKeyword());
        c.addCriteria(Criteria.PATH, cmd.getPath());
        c.addCriteria(Criteria.PODID, cmd.getPodId());
        c.addCriteria(Criteria.DATACENTERID, zoneId);

        return searchForStoragePools(c);
    }

    @Override
    public List<? extends StoragePoolVO> searchForStoragePools(Criteria c) {
        Filter searchFilter = new Filter(StoragePoolVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());
        SearchCriteria<StoragePoolVO> sc = _poolDao.createSearchCriteria();

        Object id = c.getCriteria(Criteria.ID);
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
            ssc.addOr("poolType", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
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
    public List<AsyncJobVO> searchForAsyncJobs(ListAsyncJobsCmd cmd) {
        Filter searchFilter = new Filter(AsyncJobVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<AsyncJobVO> sb = _jobDao.createSearchBuilder();

        Object accountId = null;
        Long domainId = cmd.getDomainId();
        Account caller = UserContext.current().getCaller();
        if (isAdmin(caller.getType())) {
            String accountName = cmd.getAccountName();

            if ((accountName != null) && (domainId != null)) {
                Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                if (userAccount != null) {
                    accountId = userAccount.getId();
                } else {
                    throw new InvalidParameterValueException("Failed to list async jobs for account " + accountName + " in domain " + domainId + "; account not found.");
                }
            } else if (domainId != null) {
                if (!_domainDao.isChildDomain(caller.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Failed to list async jobs for domain " + domainId + "; permission denied.");
                }
            }
            
            if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN && domainId == null) {
                domainId = caller.getDomainId();
            }
            
        } else {
            accountId = caller.getId();
        }
        
        // we should do domain based search for domain admin
        if (domainId != null) {
            sb.and("accountsIn", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        }
       
        Object keyword = cmd.getKeyword();
        Object startDate = cmd.getStartDate();

        SearchCriteria<AsyncJobVO> sc = sb.create();
        
        if (keyword != null) {
            sc.addAnd("cmd", SearchCriteria.Op.LIKE, "%" + keyword + "%");
        }

        if (accountId != null) {
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        } 
        
        
        if (domainId != null) {
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            
            SearchBuilder<AccountVO> accountSearch = _accountDao.createSearchBuilder();
            accountSearch.join("domainSearch", domainSearch, accountSearch.entity().getDomainId(), domainSearch.entity().getId(), JoinType.INNER);
            
            SearchCriteria<AccountVO> accountSc = accountSearch.create();
            DomainVO domain = _domainDao.findById(domainId);
            
            accountSc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
            
            List<AccountVO> allowedAccounts = _accountDao.search(accountSc, null);
            if (!allowedAccounts.isEmpty()) {
                Long[] accountIds = new Long[allowedAccounts.size()];
                for (int i = 0; i < allowedAccounts.size(); i++) {
                  AccountVO allowedAccount = allowedAccounts.get(i);
                  accountIds[i] = allowedAccount.getId();
                }
                
                sc.setParameters("accountsIn", (Object[])accountIds);
            }
        }

        if (startDate != null) {
            sc.addAnd("created", SearchCriteria.Op.GTEQ, startDate);
        }

        return _jobDao.search(sc, searchFilter);
    }

    @ActionEvent(eventType = EventTypes.EVENT_SSVM_START, eventDescription = "starting secondary storage Vm", async = true)
    public SecondaryStorageVmVO startSecondaryStorageVm(long instanceId) {
        return _secStorageVmMgr.startSecStorageVm(instanceId);
    }

    @ActionEvent(eventType = EventTypes.EVENT_SSVM_STOP, eventDescription = "stopping secondary storage Vm", async = true)
    private SecondaryStorageVmVO stopSecondaryStorageVm(VMInstanceVO systemVm, boolean isForced) throws ResourceUnavailableException, OperationTimedoutException, ConcurrentOperationException {

        User caller = _userDao.findById(UserContext.current().getCallerUserId());

        if (_itMgr.advanceStop(systemVm, isForced, caller, UserContext.current().getCaller())) {
            return _secStorageVmDao.findById(systemVm.getId());
        }
        return null;
    }

    @ActionEvent(eventType = EventTypes.EVENT_SSVM_REBOOT, eventDescription = "rebooting secondary storage Vm", async = true)
    public SecondaryStorageVmVO rebootSecondaryStorageVm(long instanceId) {
        _secStorageVmMgr.rebootSecStorageVm(instanceId);
        return _secStorageVmDao.findById(instanceId);
    }

    @ActionEvent(eventType = EventTypes.EVENT_SSVM_DESTROY, eventDescription = "destroying secondary storage Vm", async = true)
    public SecondaryStorageVmVO destroySecondaryStorageVm(long instanceId) {
        SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(instanceId);
        if (_secStorageVmMgr.destroySecStorageVm(instanceId)) {
            return secStorageVm;
        }
        return null;
    }

    @Override
    public List<? extends VMInstanceVO> searchForSystemVm(ListSystemVMsCmd cmd) {
        String type = cmd.getSystemVmType();
        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(UserContext.current().getCaller(), cmd.getZoneId());
        Long id = cmd.getId();
        String name = cmd.getSystemVmName();
        String state = cmd.getState();
        String keyword = cmd.getKeyword();
        Long podId = cmd.getPodId();
        Long hostId = cmd.getHostId();

        Filter searchFilter = new Filter(VMInstanceVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<VMInstanceVO> sb = _vmInstanceDao.createSearchBuilder();

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("hostName", sb.entity().getHostName(), SearchCriteria.Op.LIKE);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterIdToDeployIn(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodIdToDeployIn(), SearchCriteria.Op.EQ);
        sb.and("hostId", sb.entity().getHostId(), SearchCriteria.Op.EQ);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
        sb.and("nulltype", sb.entity().getType(), SearchCriteria.Op.IN);

        SearchCriteria<VMInstanceVO> sc = sb.create();

        if (keyword != null) {
            SearchCriteria<VMInstanceVO> ssc = _vmInstanceDao.createSearchCriteria();
            ssc.addOr("hostName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("hostName", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (name != null) {
            sc.setParameters("hostName", name);
        }
        if (state != null) {
            sc.setParameters("state", state);
        }
        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
        }
        if (podId != null) {
            sc.setParameters("podId", podId);
        }
        if (hostId != null) {
            sc.setParameters("hostId", hostId);
        }

        if (type != null) {
            sc.setParameters("type", type);
        } else {
            sc.setParameters("nulltype", VirtualMachine.Type.SecondaryStorageVm, VirtualMachine.Type.ConsoleProxy);
        }

        return _vmInstanceDao.search(sc, searchFilter);
    }

    @Override
    public VirtualMachine.Type findSystemVMTypeById(long instanceId) {
        VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(instanceId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
            throw new InvalidParameterValueException("Unable to find a system vm: " + instanceId);
        }
        return systemVm.getType();
    }

    @Override
    public VirtualMachine startSystemVM(long vmId) {

        VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(vmId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
            throw new InvalidParameterValueException("unable to find a system vm with id " + vmId);
        }

        if (systemVm.getType() == VirtualMachine.Type.ConsoleProxy) {
            return startConsoleProxy(vmId);
        } else if (systemVm.getType() == VirtualMachine.Type.SecondaryStorageVm) {
            return startSecondaryStorageVm(vmId);
        } else {
            throw new InvalidParameterValueException("Unable to find a system vm: " + vmId);
        }
    }

    @Override
    public VMInstanceVO stopSystemVM(StopSystemVmCmd cmd) throws ResourceUnavailableException, ConcurrentOperationException {
        Long id = cmd.getId();

        // verify parameters
        VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(id, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
            throw new InvalidParameterValueException("unable to find a system vm with id " + id);
        }

        try {
            if (systemVm.getType() == VirtualMachine.Type.ConsoleProxy) {
                return stopConsoleProxy(systemVm, cmd.isForced());
            } else if (systemVm.getType() == VirtualMachine.Type.SecondaryStorageVm) {
                return stopSecondaryStorageVm(systemVm, cmd.isForced());
            }
            return null;
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Unable to stop " + systemVm, e);
        }
    }

    @Override
    public VMInstanceVO rebootSystemVM(RebootSystemVmCmd cmd) {
        VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(cmd.getId(), VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);

        if (systemVm == null) {
            throw new InvalidParameterValueException("unable to find a system vm with id " + cmd.getId());
        }

        if (systemVm.getType().equals(VirtualMachine.Type.ConsoleProxy)) {
            return rebootConsoleProxy(cmd.getId());
        } else {
            return rebootSecondaryStorageVm(cmd.getId());
        }
    }

    @Override
    public VMInstanceVO destroySystemVM(DestroySystemVmCmd cmd) {
        VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(cmd.getId(), VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);

        if (systemVm == null) {
            throw new InvalidParameterValueException("unable to find a system vm with id " + cmd.getId());
        }

        if (systemVm.getType().equals(VirtualMachine.Type.ConsoleProxy)) {
            return destroyConsoleProxy(cmd.getId());
        } else {
            return destroySecondaryStorageVm(cmd.getId());
        }
    }

    private String signRequest(String request, String key) {
        try {
            s_logger.info("Request: " + request);
            s_logger.info("Key: " + key);

            if (key != null && request != null) {
                Mac mac = Mac.getInstance("HmacSHA1");
                SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "HmacSHA1");
                mac.init(keySpec);
                mac.update(request.getBytes());
                byte[] encryptedBytes = mac.doFinal();
                return new String((Base64.encodeBase64(encryptedBytes)));
            }
        } catch (Exception ex) {
            s_logger.error("unable to sign request", ex);
        }
        return null;
    }

    @Override
    public ArrayList<String> getCloudIdentifierResponse(long userId) {
        Account caller = UserContext.current().getCaller();

        // verify that user exists
        User user = _accountMgr.getUser(userId);
        if ((user == null) || (user.getRemoved() != null)) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }
        
        // check permissions
        _accountMgr.checkAccess(caller, null, _accountMgr.getAccount(user.getAccountId()));

        String cloudIdentifier = _configDao.getValue("cloud.identifier");
        if (cloudIdentifier == null) {
            cloudIdentifier = "";
        }

        String signature = "";
        try {
            // get the user obj to get his secret key
            user = _accountMgr.getActiveUser(userId);
            String secretKey = user.getSecretKey();
            String input = cloudIdentifier;
            signature = signRequest(input, secretKey);
        } catch (Exception e) {
            s_logger.warn("Exception whilst creating a signature:" + e);
        }

        ArrayList<String> cloudParams = new ArrayList<String>();
        cloudParams.add(cloudIdentifier);
        cloudParams.add(signature);

        return cloudParams;
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
        for (EventVO event : startedEvents) {
            EventVO completedEvent = _eventDao.findCompletedEvent(event.getId());
            if (completedEvent == null) {
                pendingEvents.add(event);
            }
        }
        return pendingEvents;
    }

    @Override
    public Map<String, Object> listCapabilities(ListCapabilitiesCmd cmd) {
        Map<String, Object> capabilities = new HashMap<String, Object>();

        boolean securityGroupsEnabled = false;
        boolean elasticLoadBalancerEnabled = false;
        String supportELB = "false";
        List<NetworkVO> networks = _networkDao.listSecurityGroupEnabledNetworks();
        if (networks != null && !networks.isEmpty()) {
            securityGroupsEnabled = true;
            String elbEnabled = _configDao.getValue(Config.ElasticLoadBalancerEnabled.key());
            elasticLoadBalancerEnabled = elbEnabled==null?false:Boolean.parseBoolean(elbEnabled);
            if (elasticLoadBalancerEnabled) {
                String networkType = _configDao.getValue(Config.ElasticLoadBalancerNetwork.key());
                if (networkType != null)
                    supportELB = networkType;
            }
        }

        String firewallRuleUiEnabled = _configs.get(Config.FirewallRuleUiEnabled.key());
        String userPublicTemplateEnabled = _configs.get(Config.AllowPublicUserTemplates.key());

        capabilities.put("securityGroupsEnabled", securityGroupsEnabled);
        capabilities.put("userPublicTemplateEnabled", (userPublicTemplateEnabled == null || userPublicTemplateEnabled.equals("false") ? false : true));
        capabilities.put("cloudStackVersion", getVersion());
        capabilities.put("supportELB", supportELB);
        capabilities.put("firewallRuleUiEnabled", (firewallRuleUiEnabled != null && firewallRuleUiEnabled.equals("true")) ? true : false);
        return capabilities;
    }

    @Override
    public GuestOSVO getGuestOs(Long guestOsId) {
        return _guestOSDao.findById(guestOsId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_EXTRACT, eventDescription = "extracting volume", async = true)
    public Long extractVolume(ExtractVolumeCmd cmd) throws URISyntaxException {
        Long volumeId = cmd.getId();
        String url = cmd.getUrl();
        Long zoneId = cmd.getZoneId();
        AsyncJobVO job = null; // FIXME: cmd.getJob();
        String mode = cmd.getMode();
        Account account = UserContext.current().getCaller();

        if (!_accountMgr.isRootAdmin(account.getType()) && ApiDBUtils.isExtractionDisabled()) {
            throw new PermissionDeniedException("Extraction has been disabled by admin");
        }
        
        VolumeVO volume = _volumeDao.findById(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Unable to find volume with id " + volumeId);
        }
        
        //perform permission check
        _accountMgr.checkAccess(account, null, volume);

        if (_dcDao.findById(zoneId) == null) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }
        if (volume.getPoolId() == null) {
            throw new InvalidParameterValueException("The volume doesnt belong to a storage pool so cant extract it");
        }
        // Extract activity only for detached volumes or for volumes whose instance is stopped
        if (volume.getInstanceId() != null && ApiDBUtils.findVMInstanceById(volume.getInstanceId()).getState() != State.Stopped) {
            s_logger.debug("Invalid state of the volume with ID: " + volumeId + ". It should be either detached or the VM should be in stopped state.");
            throw new PermissionDeniedException("Invalid state of the volume with ID: " + volumeId + ". It should be either detached or the VM should be in stopped state.");
        }
        
        if (volume.getVolumeType() != Volume.Type.DATADISK){ //Datadisk dont have any template dependence.
        	
        	VMTemplateVO template = ApiDBUtils.findTemplateById(volume.getTemplateId());
        	if (template != null){ //For ISO based volumes template = null and we allow extraction of all ISO based volumes
	            boolean isExtractable = template.isExtractable() && template.getTemplateType() != Storage.TemplateType.SYSTEM;
	            if (!isExtractable && account != null && account.getType() != Account.ACCOUNT_TYPE_ADMIN) { // Global admins are always allowed to extract
	                throw new PermissionDeniedException("The volume:" + volumeId + " is not allowed to be extracted");
	            }
        	}
        }

        Upload.Mode extractMode;
        if (mode == null || (!mode.equals(Upload.Mode.FTP_UPLOAD.toString()) && !mode.equals(Upload.Mode.HTTP_DOWNLOAD.toString()))) {
            throw new InvalidParameterValueException("Please specify a valid extract Mode ");
        } else {
            extractMode = mode.equals(Upload.Mode.FTP_UPLOAD.toString()) ? Upload.Mode.FTP_UPLOAD : Upload.Mode.HTTP_DOWNLOAD;
        }
        
        // If mode is upload perform extra checks on url and also see if there is an ongoing upload on the same.
        if (extractMode == Upload.Mode.FTP_UPLOAD) {
            URI uri = new URI(url);
            if ((uri.getScheme() == null) || (!uri.getScheme().equalsIgnoreCase("ftp"))) {
                throw new IllegalArgumentException("Unsupported scheme for url: " + url);
            }

            String host = uri.getHost();
            try {
                InetAddress hostAddr = InetAddress.getByName(host);
                if (hostAddr.isAnyLocalAddress() || hostAddr.isLinkLocalAddress() || hostAddr.isLoopbackAddress() || hostAddr.isMulticastAddress()) {
                    throw new IllegalArgumentException("Illegal host specified in url");
                }
                if (hostAddr instanceof Inet6Address) {
                    throw new IllegalArgumentException("IPV6 addresses not supported (" + hostAddr.getHostAddress() + ")");
                }
            } catch (UnknownHostException uhe) {
                throw new IllegalArgumentException("Unable to resolve " + host);
            }

            if (_uploadMonitor.isTypeUploadInProgress(volumeId, Upload.Type.VOLUME)) {
                throw new IllegalArgumentException(volume.getName() + " upload is in progress. Please wait for some time to schedule another upload for the same");
            }
        }

        long accountId = volume.getAccountId();

        String secondaryStorageURL = _storageMgr.getSecondaryStorageURL(zoneId);
        StoragePoolVO srcPool = _poolDao.findById(volume.getPoolId());
        List<HostVO> storageServers = _resourceMgr.listAllHostsInOneZoneByType(Host.Type.SecondaryStorage, zoneId);
        HostVO sserver = storageServers.get(0);

        List<UploadVO> extractURLList = _uploadDao.listByTypeUploadStatus(volumeId, Upload.Type.VOLUME, UploadVO.Status.DOWNLOAD_URL_CREATED);

        if (extractMode == Upload.Mode.HTTP_DOWNLOAD && extractURLList.size() > 0) {
            return extractURLList.get(0).getId(); // If download url already exists then return
        } else {
            UploadVO uploadJob = _uploadMonitor.createNewUploadEntry(sserver.getId(), volumeId, UploadVO.Status.COPY_IN_PROGRESS, Upload.Type.VOLUME, url, extractMode);
            s_logger.debug("Extract Mode - " + uploadJob.getMode());
            uploadJob = _uploadDao.createForUpdate(uploadJob.getId());

            // Update the async Job
            ExtractResponse resultObj = new ExtractResponse(volumeId, volume.getName(), accountId, UploadVO.Status.COPY_IN_PROGRESS.toString(), uploadJob.getId());
            resultObj.setResponseName(cmd.getCommandName());
            AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
            if (asyncExecutor != null) {
                job = asyncExecutor.getJob();
                _asyncMgr.updateAsyncJobAttachment(job.getId(), Upload.Type.VOLUME.toString(), volumeId);
                _asyncMgr.updateAsyncJobStatus(job.getId(), AsyncJobResult.STATUS_IN_PROGRESS, resultObj);
            }
            String value = _configs.get(Config.CopyVolumeWait.toString());
            int copyvolumewait  = NumbersUtil.parseInt(value, Integer.parseInt(Config.CopyVolumeWait.getDefaultValue()));
            // Copy the volume from the source storage pool to secondary storage
            CopyVolumeCommand cvCmd = new CopyVolumeCommand(volume.getId(), volume.getPath(), srcPool, secondaryStorageURL, true, copyvolumewait);
            CopyVolumeAnswer cvAnswer = null;
            try {
                cvAnswer = (CopyVolumeAnswer) _storageMgr.sendToPool(srcPool, cvCmd);
            } catch (StorageUnavailableException e) {
                s_logger.debug("Storage unavailable");
            }

            // Check if you got a valid answer.
            if (cvAnswer == null || !cvAnswer.getResult()) {
                String errorString = "Failed to copy the volume from the source primary storage pool to secondary storage.";

                // Update the async job.
                resultObj.setResultString(errorString);
                resultObj.setUploadStatus(UploadVO.Status.COPY_ERROR.toString());
                if (asyncExecutor != null) {
                    _asyncMgr.completeAsyncJob(job.getId(), AsyncJobResult.STATUS_FAILED, 0, resultObj);
                }

                // Update the DB that volume couldn't be copied
                uploadJob.setUploadState(UploadVO.Status.COPY_ERROR);
                uploadJob.setErrorString(errorString);
                uploadJob.setLastUpdated(new Date());
                _uploadDao.update(uploadJob.getId(), uploadJob);

                throw new CloudRuntimeException(errorString);
            }

            String volumeLocalPath = "volumes/" + volume.getId() + "/" + cvAnswer.getVolumePath() + "." + getFormatForPool(srcPool);
            // Update the DB that volume is copied and volumePath
            uploadJob.setUploadState(UploadVO.Status.COPY_COMPLETE);
            uploadJob.setLastUpdated(new Date());
            uploadJob.setInstallPath(volumeLocalPath);
            _uploadDao.update(uploadJob.getId(), uploadJob);

            if (extractMode == Mode.FTP_UPLOAD) { // Now that the volume is copied perform the actual uploading
                _uploadMonitor.extractVolume(uploadJob, sserver, volume, url, zoneId, volumeLocalPath, cmd.getStartEventId(), job.getId(), _asyncMgr);
                return uploadJob.getId();
            } else { // Volume is copied now make it visible under apache and create a URL.
                _uploadMonitor.createVolumeDownloadURL(volumeId, volumeLocalPath, Upload.Type.VOLUME, zoneId, uploadJob.getId());
                return uploadJob.getId();
            }
        }
    }

    private String getFormatForPool(StoragePoolVO pool){
    	ClusterVO cluster = ApiDBUtils.findClusterById(pool.getClusterId());
    	
    	if (cluster.getHypervisorType() == HypervisorType.XenServer){
    		return "vhd";
    	}else if (cluster.getHypervisorType() == HypervisorType.KVM){
    		return "qcow2";    		
    	}else if (cluster.getHypervisorType() == HypervisorType.VMware){
    		return "ova";
    	}else if (cluster.getHypervisorType() == HypervisorType.Ovm){
    		return "raw";
    	}else{
    		return null;
    	}
    }
    
    
    @Override
    public InstanceGroupVO updateVmGroup(UpdateVMGroupCmd cmd) {
        Account caller = UserContext.current().getCaller();
        Long groupId = cmd.getId();
        String groupName = cmd.getGroupName();

        // Verify input parameters
        InstanceGroupVO group = _vmGroupDao.findById(groupId.longValue());
        if (group == null) {
            throw new InvalidParameterValueException("unable to find a vm group with id " + groupId);
        }

        _accountMgr.checkAccess(caller, null, group);

        // Check if name is already in use by this account (exclude this group)
        boolean isNameInUse = _vmGroupDao.isNameInUse(group.getAccountId(), groupName);

        if (isNameInUse && !group.getName().equals(groupName)) {
            throw new InvalidParameterValueException("Unable to update vm group, a group with name " + groupName + " already exists for account");
        }

        if (groupName != null) {
            _vmGroupDao.updateVmGroup(groupId, groupName);
        }

        return _vmGroupDao.findById(groupId);
    }

    @Override
    public List<InstanceGroupVO> searchForVmGroups(ListVMGroupsCmd cmd) {
        Account caller = UserContext.current().getCaller();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long projectId = cmd.getProjectId();
        List<Long> permittedAccounts = new ArrayList<Long>(); 
        
        if ((caller == null) || isAdmin(caller.getType())) {
            if (domainId != null) {
                if ((caller != null) && !_domainDao.isChildDomain(caller.getDomainId(), domainId)) {
                    throw new InvalidParameterValueException("Invalid domain id (" + domainId + ") given, unable to list vm groups.");
                }

                if (accountName != null) {
                    caller = _accountDao.findActiveAccount(accountName, domainId);
                    if (caller == null) {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                    permittedAccounts.add(caller.getId());
                }
            } else {
                domainId = ((caller == null) ? DomainVO.ROOT_DOMAIN : caller.getDomainId());
            }
        } else {
            permittedAccounts.add(caller.getId());
            domainId = caller.getDomainId();
        }
        
        //set project information
        if (projectId != null) {
            permittedAccounts.clear();
            Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                throw new InvalidParameterValueException("Unable to find project by id " + projectId);
            }
            if (!_projectMgr.canAccessProjectAccount(caller, project.getProjectAccountId())) {
                throw new InvalidParameterValueException("Account " + caller + " can't access project id=" + projectId);
            }
            permittedAccounts.add(project.getProjectAccountId());
        } else if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL){
            permittedAccounts.addAll(_projectMgr.listPermittedProjectAccounts(caller.getId()));
        } 

        Filter searchFilter = new Filter(InstanceGroupVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object id = cmd.getId();
        Object name = cmd.getGroupName();
        Object keyword = cmd.getKeyword();

        SearchBuilder<InstanceGroupVO> sb = _vmGroupDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.IN);

        if ((permittedAccounts.isEmpty()) && (domainId != null)) {
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

        if (!permittedAccounts.isEmpty()) {
            sc.setParameters("accountId", permittedAccounts.toArray());
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            if (domain != null) {
                sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
            }
        }

        return _vmGroupDao.search(sc, searchFilter);
    }

    @Override
    public String getVersion() {
        final Class<?> c = ManagementServer.class;
        String fullVersion = c.getPackage().getImplementationVersion();
        if (fullVersion.length() > 0) {
            return fullVersion;
        }

        return "unknown";
    }

    @Override
    public Long saveStartedEvent(Long userId, Long accountId, String type, String description, long startEventId) {
        return EventUtils.saveStartedEvent(userId, accountId, type, description, startEventId);
    }

    @Override
    public Long saveCompletedEvent(Long userId, Long accountId, String level, String type, String description, long startEventId) {
        return EventUtils.saveEvent(userId, accountId, level, type, description, startEventId);
    }

    @Override
    @DB
    public String uploadCertificate(UploadCustomCertificateCmd cmd) {
        if (!_ksMgr.validateCertificate(cmd.getCertificate(), cmd.getPrivateKey(), cmd.getDomainSuffix())) {
            throw new InvalidParameterValueException("Failed to pass certificate validation check");
        }

        _ksMgr.saveCertificate(ConsoleProxyManager.CERTIFICATE_NAME, cmd.getCertificate(), cmd.getPrivateKey(), cmd.getDomainSuffix());

        _consoleProxyMgr.setManagementState(ConsoleProxyManagementState.ResetSuspending);
        return "Certificate has been updated, we will stop all running console proxy VMs to propagate the new certificate, please give a few minutes for console access service to be up again";
    }

    @Override
    public List<String> getHypervisors(Long zoneId) {
        List<String> result = new ArrayList<String>();
        String hypers = _configDao.getValue(Config.HypervisorList.key());
        String[] hypervisors = hypers.split(",");

        if (zoneId != null) {
            if (zoneId.longValue() == -1L) {
                List<DataCenterVO> zones = _dcDao.listAll();

                for (String hypervisor : hypervisors) {
                    int hyperCount = 0;
                    for (DataCenterVO zone : zones) {
                        List<ClusterVO> clusters = _clusterDao.listByDcHyType(zone.getId(), hypervisor);
                        if (!clusters.isEmpty()) {
                            hyperCount++;
                        }
                    }
                    if (hyperCount == zones.size()) {
                        result.add(hypervisor);
                    }
                }
            } else {
                List<ClusterVO> clustersForZone = _clusterDao.listByZoneId(zoneId);
                for (ClusterVO cluster : clustersForZone) {
                    result.add(cluster.getHypervisorType().toString());
                }
            }

        } else {
            return Arrays.asList(hypervisors);
        }
        return result;
    }

    @Override
    public String getHashKey() {
        // although we may have race conditioning here, database transaction serialization should
        // give us the same key
        if (_hashKey == null) {
            _hashKey = _configDao.getValueAndInitIfNotExist(Config.HashKey.key(), UUID.randomUUID().toString());
        }
        return _hashKey;
    }

    @Override
    public SSHKeyPair createSSHKeyPair(CreateSSHKeyPairCmd cmd) {
        Account caller = UserContext.current().getCaller();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long projectId = cmd.getProjectId();

        Account owner = _accountMgr.finalizeOwner(caller, accountName, domainId, projectId);

        SSHKeyPairVO s = _sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), cmd.getName());
        if (s != null) {
            throw new InvalidParameterValueException("A key pair with name '" + cmd.getName() + "' already exists.");
        }

        SSHKeysHelper keys = new SSHKeysHelper();

        String name = cmd.getName();
        String publicKey = keys.getPublicKey();
        String fingerprint = keys.getPublicKeyFingerPrint();
        String privateKey = keys.getPrivateKey();

        return createAndSaveSSHKeyPair(name, fingerprint, publicKey, privateKey, owner);
    }

    @Override
    public boolean deleteSSHKeyPair(DeleteSSHKeyPairCmd cmd) {
        Account caller = UserContext.current().getCaller();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long projectId = cmd.getProjectId();
        
        Account owner = _accountMgr.finalizeOwner(caller, accountName, domainId, projectId);

        SSHKeyPairVO s = _sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), cmd.getName());
        if (s == null) {
            throw new InvalidParameterValueException("A key pair with name '" + cmd.getName() + "' does not exist for account " + owner.getAccountName() + " in domain id=" + owner.getDomainId());
        }

        return _sshKeyPairDao.deleteByName(caller.getAccountId(), caller.getDomainId(), cmd.getName());
    }

    @Override
    public List<? extends SSHKeyPair> listSSHKeyPairs(ListSSHKeyPairsCmd cmd) {
        Account caller = UserContext.current().getCaller();
        String name = cmd.getName();
        String fingerPrint = cmd.getFingerprint();
        List<Long> permittedAccounts = new ArrayList<Long>();
        Long domainId = null;
        String path = null;

        if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
            permittedAccounts.add(caller.getId());
            domainId = caller.getDomainId();
        } else if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            DomainVO domain = _domainDao.findById(caller.getDomainId());
            path = domain.getPath();
        }
        
        if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL){
            permittedAccounts.addAll(_projectMgr.listPermittedProjectAccounts(caller.getId()));
        }

        SearchBuilder<SSHKeyPairVO> sb = _sshKeyPairDao.createSearchBuilder();
        Filter searchFilter = new Filter(SSHKeyPairVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        if (path != null) {
            // for domain admin we should show only subdomains information
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<SSHKeyPairVO> sc = sb.create();

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.EQ, name);
        }

        if (!permittedAccounts.isEmpty()) {
            sc.addAnd("accountId", SearchCriteria.Op.IN, permittedAccounts.toArray());
        }

        if (domainId != null) {
            sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        }

        if (fingerPrint != null) {
            sc.addAnd("fingerprint", SearchCriteria.Op.EQ, fingerPrint);
        }

        if (path != null) {
            sc.setJoinParameters("domainSearch", "path", path + "%");
        }

        return _sshKeyPairDao.search(sc, searchFilter);
    }

    @Override
    public SSHKeyPair registerSSHKeyPair(RegisterSSHKeyPairCmd cmd) {
        Account caller = UserContext.current().getCaller();
        
        Account owner = _accountMgr.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId());
        
        SSHKeyPairVO s = _sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), cmd.getName());
        if (s != null) {
            throw new InvalidParameterValueException("A key pair with name '" + cmd.getName() + "' already exists.");
        }

        String name = cmd.getName();
        String publicKey = SSHKeysHelper.getPublicKeyFromKeyMaterial(cmd.getPublicKey());
        
        if (publicKey == null) {
            throw new InvalidParameterValueException("Public key is invalid");
        }
        
        String fingerprint = SSHKeysHelper.getPublicKeyFingerprint(publicKey);

        return createAndSaveSSHKeyPair(name, fingerprint, publicKey, null, owner);
    }

    private SSHKeyPair createAndSaveSSHKeyPair(String name, String fingerprint, String publicKey, String privateKey, Account owner) {
        SSHKeyPairVO newPair = new SSHKeyPairVO();

        newPair.setAccountId(owner.getAccountId());
        newPair.setDomainId(owner.getDomainId());
        newPair.setName(name);
        newPair.setFingerprint(fingerprint);
        newPair.setPublicKey(publicKey);
        newPair.setPrivateKey(privateKey); // transient; not saved.

        _sshKeyPairDao.persist(newPair);

        return newPair;
    }

    @Override
    public String getVMPassword(GetVMPasswordCmd cmd) {
        Account caller = UserContext.current().getCaller();

        UserVmVO vm = _userVmDao.findById(cmd.getId());
        if (vm == null) {
            throw new InvalidParameterValueException("No VM with id '" + cmd.getId() + "' found.");
        }

        // make permission check
        _accountMgr.checkAccess(caller, null, vm);

        _userVmDao.loadDetails(vm);
        String password = vm.getDetail("Encrypted.Password");
        if (password == null || password.equals("")) {
            throw new InvalidParameterValueException("No password for VM with id '" + cmd.getId() + "' found.");
        }

        return password;
    }

    @Override
    @DB
    public boolean updateHostPassword(UpdateHostPasswordCmd cmd) {
        if (cmd.getClusterId() == null && cmd.getHostId() == null) {
            throw new InvalidParameterValueException("You should provide one of cluster id or a host id.");
        } else if (cmd.getClusterId() == null) {
            HostVO h = _hostDao.findById(cmd.getHostId());
            if (h.getHypervisorType() == HypervisorType.XenServer) {
                throw new InvalidParameterValueException("You should provide cluster id for Xenserver cluster.");
            }
            DetailVO nv = _detailsDao.findDetail(h.getId(), ApiConstants.USERNAME);
            if (nv.getValue().equals(cmd.getUsername())) {
                DetailVO nvp = new DetailVO(h.getId(), ApiConstants.PASSWORD, cmd.getPassword());
                nvp.setValue(cmd.getPassword());
                _detailsDao.persist(nvp);
            } else {
                throw new InvalidParameterValueException("The username is not under use by management server.");
            }
        } else {
            // get all the hosts in this cluster
            List<HostVO> hosts = _resourceMgr.listAllHostsInCluster(cmd.getClusterId());
            Transaction txn = Transaction.currentTxn();
            txn.start();
            for (HostVO h : hosts) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Changing password for host name = " + h.getName());
                }
                // update password for this host
                DetailVO nv = _detailsDao.findDetail(h.getId(), ApiConstants.USERNAME);
                if (nv.getValue().equals(cmd.getUsername())) {
                    DetailVO nvp = _detailsDao.findDetail(h.getId(), ApiConstants.PASSWORD);
                    nvp.setValue(cmd.getPassword());
                    _detailsDao.persist(nvp);
                } else {
                    // if one host in the cluster has diff username then rollback to maintain consistency
                    txn.rollback();
                    throw new InvalidParameterValueException("The username is not same for all hosts, please modify passwords for individual hosts.");
                }
            }
            txn.commit();
            // if hypervisor is xenserver then we update it in CitrixResourceBase
        }
        return true;
    }

    @Override
    public String[] listEventTypes(){
        Object eventObj = new EventTypes(); 
        Class<EventTypes> c = EventTypes.class;
        Field[] fields = c.getDeclaredFields();
        String[] eventTypes = new String[fields.length];
        try {
            int i = 0;
            for(Field field : fields){
                eventTypes[i++] = field.get(eventObj).toString();
            }
            return eventTypes;
        } catch (IllegalArgumentException e) {
            s_logger.error("Error while listing Event Types", e);
        } catch (IllegalAccessException e) {
            s_logger.error("Error while listing Event Types", e);
        }
        return null;
    }
    
    @Override
    public List<HypervisorCapabilitiesVO> listHypervisorCapabilities(Long id, HypervisorType hypervisorType, Long startIndex, Long pageSizeVal){
        Filter searchFilter = new Filter(HypervisorCapabilitiesVO.class, "id", true, startIndex, pageSizeVal);
        SearchCriteria<HypervisorCapabilitiesVO> sc = _hypervisorCapabilitiesDao.createSearchCriteria();

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (hypervisorType != null) {
            sc.addAnd("hypervisorType", SearchCriteria.Op.EQ, hypervisorType);
        }

        return _hypervisorCapabilitiesDao.search(sc, searchFilter);
        
    }
    
    @Override
    public HypervisorCapabilities updateHypervisorCapabilities(Long id, Long maxGuestsLimit, Boolean securityGroupEnabled){
        HypervisorCapabilitiesVO hpvCapabilities = _hypervisorCapabilitiesDao.findById(id, true);
        
        if(hpvCapabilities == null){
            throw new InvalidParameterValueException("unable to find the hypervisor capabilities " + id);
        }
        
        boolean updateNeeded = (maxGuestsLimit != null || securityGroupEnabled != null);
        if (!updateNeeded) {
            return hpvCapabilities;
        }
        
        
        hpvCapabilities = _hypervisorCapabilitiesDao.createForUpdate(id);

        if(maxGuestsLimit != null){
            hpvCapabilities.setMaxGuestsLimit(maxGuestsLimit);
        }
        
        if(securityGroupEnabled != null){
            hpvCapabilities.setSecurityGroupEnabled(securityGroupEnabled);
        }

        if(_hypervisorCapabilitiesDao.update(id, hpvCapabilities)){
            hpvCapabilities = _hypervisorCapabilitiesDao.findById(id);
            UserContext.current().setEventDetails("Hypervisor Capabilities id=" + hpvCapabilities.getId());
            return hpvCapabilities;            
        }else{
            return null;
        }
    }
}
