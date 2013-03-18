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
package com.cloud.server;

import java.lang.reflect.Field;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ApiConstants;

import com.cloud.event.ActionEventUtils;
import org.apache.cloudstack.api.BaseUpdateTemplateOrIsoCmd;

import org.apache.cloudstack.api.command.admin.account.*;
import org.apache.cloudstack.api.command.admin.autoscale.*;
import org.apache.cloudstack.api.command.admin.cluster.*;
import org.apache.cloudstack.api.command.admin.config.*;
import org.apache.cloudstack.api.command.admin.domain.*;
import org.apache.cloudstack.api.command.admin.host.*;
import org.apache.cloudstack.api.command.admin.ldap.*;
import org.apache.cloudstack.api.command.admin.network.*;
import org.apache.cloudstack.api.command.admin.offering.*;
import org.apache.cloudstack.api.command.admin.pod.*;
import org.apache.cloudstack.api.command.admin.region.*;
import org.apache.cloudstack.api.command.admin.resource.*;
import org.apache.cloudstack.api.command.admin.router.*;
import org.apache.cloudstack.api.command.admin.storage.*;
import org.apache.cloudstack.api.command.admin.swift.*;
import org.apache.cloudstack.api.command.admin.systemvm.*;
import org.apache.cloudstack.api.command.admin.template.*;
import org.apache.cloudstack.api.command.admin.usage.*;
import org.apache.cloudstack.api.command.admin.user.*;
import org.apache.cloudstack.api.command.admin.vlan.*;
import org.apache.cloudstack.api.command.admin.vm.*;
import org.apache.cloudstack.api.command.admin.vpc.*;
import org.apache.cloudstack.api.command.admin.zone.*;
import org.apache.cloudstack.api.command.user.account.*;
import org.apache.cloudstack.api.command.user.address.*;
import org.apache.cloudstack.api.command.user.autoscale.*;
import org.apache.cloudstack.api.command.user.config.*;
import org.apache.cloudstack.api.command.user.event.*;
import org.apache.cloudstack.api.command.user.firewall.*;
import org.apache.cloudstack.api.command.user.guest.*;
import org.apache.cloudstack.api.command.user.iso.*;
import org.apache.cloudstack.api.command.user.job.*;
import org.apache.cloudstack.api.command.user.loadbalancer.*;
import org.apache.cloudstack.api.command.user.nat.*;
import org.apache.cloudstack.api.command.user.network.*;
import org.apache.cloudstack.api.command.user.offering.*;
import org.apache.cloudstack.api.command.user.project.*;
import org.apache.cloudstack.api.command.user.region.*;
import org.apache.cloudstack.api.command.user.resource.*;
import org.apache.cloudstack.api.command.user.securitygroup.*;
import org.apache.cloudstack.api.command.user.snapshot.*;
import org.apache.cloudstack.api.command.user.ssh.*;
import org.apache.cloudstack.api.command.user.tag.*;
import org.apache.cloudstack.api.command.user.template.*;
import org.apache.cloudstack.api.command.user.vm.*;
import org.apache.cloudstack.api.command.user.vmgroup.*;
import org.apache.cloudstack.api.command.user.vmsnapshot.CreateVMSnapshotCmd;
import org.apache.cloudstack.api.command.user.vmsnapshot.DeleteVMSnapshotCmd;
import org.apache.cloudstack.api.command.user.vmsnapshot.ListVMSnapshotCmd;
import org.apache.cloudstack.api.command.user.vmsnapshot.RevertToSnapshotCmd;
import org.apache.cloudstack.api.command.user.volume.*;
import org.apache.cloudstack.api.command.user.vpc.*;
import org.apache.cloudstack.api.command.user.vpn.*;
import org.apache.cloudstack.api.command.user.zone.*;
import org.apache.cloudstack.api.response.ExtractResponse;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

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
import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.EventJoinVO;
import com.cloud.async.AsyncJobExecutor;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.capacity.dao.CapacityDaoImpl.SummedCapacity;
import com.cloud.cluster.ClusterManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.Configuration;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ConfigurationVO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.consoleproxy.ConsoleProxyManagementState;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.Vlan;
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
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostTagVO;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.host.dao.HostTagsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorCapabilities;
import com.cloud.hypervisor.HypervisorCapabilitiesVO;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
import com.cloud.info.ConsoleProxyInfo;
import com.cloud.keystore.KeystoreManager;
import com.cloud.network.IpAddress;
import com.cloud.network.as.ConditionVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.projects.Project;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.projects.ProjectManager;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.server.auth.UserAuthenticator;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.GuestOsCategory;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Upload;
import com.cloud.storage.Upload.Mode;
import com.cloud.storage.UploadVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.UploadDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.s3.S3Manager;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.storage.swift.SwiftManager;
import com.cloud.storage.upload.UploadMonitor;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate.TemplateFilter;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.SSHKeyPair;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.Adapter;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ComponentLifecycle;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.SystemIntegrityChecker;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.mgmt.JmxUtil;
import com.cloud.utils.mgmt.ManagementBean;
import com.cloud.utils.net.MacAddress;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.ssh.SSHKeysHelper;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.SecondaryStorageVmVO;
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
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;

public class ManagementServerImpl extends ManagerBase implements ManagementServer {
    public static final Logger s_logger = Logger.getLogger(ManagementServerImpl.class.getName());

    @Inject
    public AccountManager _accountMgr;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private AlertManager _alertMgr;
    @Inject
    private IPAddressDao _publicIpAddressDao;
    @Inject
    private DomainRouterDao _routerDao;
    @Inject
    private ConsoleProxyDao _consoleProxyDao;
    @Inject
    private ClusterDao _clusterDao;
    @Inject
    private SecondaryStorageVmDao _secStorageVmDao;
    @Inject
    public EventDao _eventDao;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private VlanDao _vlanDao;
    @Inject
    private AccountVlanMapDao _accountVlanMapDao;
    @Inject
    private PodVlanMapDao _podVlanMapDao;
    @Inject
    private HostDao _hostDao;
    @Inject
    private HostDetailsDao _detailsDao;
    @Inject
    private UserDao _userDao;
    @Inject
    private UserVmDao _userVmDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private ConsoleProxyManager _consoleProxyMgr;
    @Inject
    private SecondaryStorageVmManager _secStorageVmMgr;
    @Inject
    private SwiftManager _swiftMgr;
    @Inject
    private ServiceOfferingDao _offeringsDao;
    @Inject
    private DiskOfferingDao _diskOfferingDao;
    @Inject
    private VMTemplateDao _templateDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private AccountDao _accountDao;
    @Inject
    public AlertDao _alertDao;
    @Inject
    private CapacityDao _capacityDao;
    @Inject
    private GuestOSDao _guestOSDao;
    @Inject
    private GuestOSCategoryDao _guestOSCategoryDao;
    @Inject
    private PrimaryDataStoreDao _poolDao;
    @Inject
    private NetworkDao _networkDao;
    @Inject
    private StorageManager _storageMgr;
    @Inject
    private VirtualMachineManager _itMgr;
    @Inject
    private HostPodDao _hostPodDao;
    @Inject
    private VMInstanceDao _vmInstanceDao;
    @Inject
    private VolumeDao _volumeDao;
    @Inject
    private AsyncJobManager _asyncMgr;
    private int _purgeDelay;
    private int _alertPurgeDelay;
    @Inject
    private InstanceGroupDao _vmGroupDao;
    @Inject
    private UploadMonitor _uploadMonitor;
    @Inject
    private UploadDao _uploadDao;
    @Inject
    private SSHKeyPairDao _sshKeyPairDao;
    @Inject
    private LoadBalancerDao _loadbalancerDao;
    @Inject
    private HypervisorCapabilitiesDao _hypervisorCapabilitiesDao;

    @Inject
    private List<HostAllocator> _hostAllocators;
    @Inject
    private ConfigurationManager _configMgr;
    @Inject
    private ResourceTagDao _resourceTagDao;

    @Inject
    ProjectManager _projectMgr;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    SnapshotManager _snapshotMgr;
    @Inject
    HighAvailabilityManager _haMgr;
    @Inject
    TemplateManager templateMgr;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    HostTagsDao _hostTagsDao;

    @Inject
    S3Manager _s3Mgr;

/*
    @Inject
    ComponentContext _forceContextRef;			// create a dependency to ComponentContext so that it can be loaded beforehead

    @Inject
    EventUtils	_forceEventUtilsRef;
*/
    private final ScheduledExecutorService _eventExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("EventChecker"));
    private final ScheduledExecutorService _alertExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("AlertChecker"));
    private KeystoreManager _ksMgr;

    private Map<String, String> _configs;

    private Map<String, Boolean> _availableIdsMap;

    @Inject List<UserAuthenticator> _userAuthenticators;

    @Inject ClusterManager _clusterMgr;
    private String _hashKey = null;

    public ManagementServerImpl() {
    	setRunLevel(ComponentLifecycle.RUN_LEVEL_APPLICATION_MAINLOOP);
    }

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {

		_configs = _configDao.getConfiguration();

        String value = _configs.get("event.purge.interval");
        int cleanup = NumbersUtil.parseInt(value, 60 * 60 * 24); // 1 day.

        _purgeDelay = NumbersUtil.parseInt(_configs.get("event.purge.delay"), 0);
        if (_purgeDelay != 0) {
            _eventExecutor.scheduleAtFixedRate(new EventPurgeTask(), cleanup, cleanup, TimeUnit.SECONDS);
        }

        //Alerts purge configurations
        int alertPurgeInterval = NumbersUtil.parseInt(_configDao.getValue(Config.AlertPurgeInterval.key()),
                60 * 60 * 24); // 1 day.
        _alertPurgeDelay = NumbersUtil.parseInt(_configDao.getValue(Config.AlertPurgeDelay.key()), 0);
        if (_alertPurgeDelay != 0) {
            _alertExecutor.scheduleAtFixedRate(new AlertPurgeTask(), alertPurgeInterval, alertPurgeInterval,
                    TimeUnit.SECONDS);
        }

        String[] availableIds = TimeZone.getAvailableIDs();
        _availableIdsMap = new HashMap<String, Boolean>(availableIds.length);
        for (String id : availableIds) {
            _availableIdsMap.put(id, true);
        }

		return true;
	}

    @Override
    public boolean start() {
        s_logger.info("Startup CloudStack management server...");

        enableAdminUser("password");
        return true;
    }

    protected Map<String, String> getConfigs() {
        return _configs;
    }

    @Override
    public String generateRandomPassword() {
        return PasswordGenerator.generateRandomPassword(6);
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

        // s_logger.debug("Checking if " + privateIp +
        // " is a valid private IP address. Guest IP address is: " +
        // _configs.get("guest.ip.network"));
        //
        // if (!NetUtils.isValidPrivateIp(privateIp,
        // _configs.get("guest.ip.network"))) {
        // throw new
        // InvalidParameterValueException("Invalid private ip address");
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

    @Override
    public boolean archiveEvents(ArchiveEventsCmd cmd) {
        List<Long> ids = cmd.getIds();
        boolean result =true;

        List<EventVO> events = _eventDao.listToArchiveOrDeleteEvents(ids, cmd.getType(), cmd.getOlderThan(), cmd.getEntityOwnerId());
        ControlledEntity[] sameOwnerEvents = events.toArray(new ControlledEntity[events.size()]);
        _accountMgr.checkAccess(UserContext.current().getCaller(), null, true, sameOwnerEvents);

        if (ids != null && events.size() < ids.size()) {
            result = false;
            return result;
        }
        _eventDao.archiveEvents(events);
        return result;
    }

    @Override
    public boolean deleteEvents(DeleteEventsCmd cmd) {
        List<Long> ids = cmd.getIds();
        boolean result =true;

        List<EventVO> events = _eventDao.listToArchiveOrDeleteEvents(ids, cmd.getType(), cmd.getOlderThan(), cmd.getEntityOwnerId());
        ControlledEntity[] sameOwnerEvents = events.toArray(new ControlledEntity[events.size()]);
        _accountMgr.checkAccess(UserContext.current().getCaller(), null, true, sameOwnerEvents);

        if (ids != null && events.size() < ids.size()) {
            result = false;
            return result;
        }
        for (EventVO event : events) {
        _eventDao.remove(event.getId());
        }
        return result;
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
    public List<? extends Cluster> searchForClusters(long zoneId, Long startIndex, Long pageSizeVal, String hypervisorType) {
        Filter searchFilter = new Filter(ClusterVO.class, "id", true, startIndex, pageSizeVal);
        SearchCriteria<ClusterVO> sc = _clusterDao.createSearchCriteria();

        zoneId = _accountMgr.checkAccessAndSpecifyAuthority(UserContext.current().getCaller(), zoneId);

        sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        sc.addAnd("hypervisorType", SearchCriteria.Op.EQ, hypervisorType);

        return _clusterDao.search(sc, searchFilter);
    }

    @Override
    public Pair<List<? extends Cluster>, Integer> searchForClusters(ListClustersCmd cmd) {
        Filter searchFilter = new Filter(ClusterVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<ClusterVO> sc = _clusterDao.createSearchCriteria();

        Object id = cmd.getId();
        Object name = cmd.getClusterName();
        Object podId = cmd.getPodId();
        Long zoneId = cmd.getZoneId();
        Object hypervisorType = cmd.getHypervisorType();
        Object clusterType = cmd.getClusterType();
        Object allocationState = cmd.getAllocationState();
        String keyword = cmd.getKeyword();

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

        if (keyword != null) {
            SearchCriteria<ClusterVO> ssc = _clusterDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("hypervisorType", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        Pair<List<ClusterVO>, Integer> result = _clusterDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Cluster>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends Host>, Integer> searchForServers(ListHostsCmd cmd) {

        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(UserContext.current().getCaller(), cmd.getZoneId());
        Object name = cmd.getHostName();
        Object type = cmd.getType();
        Object state = cmd.getState();
        Object pod = cmd.getPodId();
        Object cluster = cmd.getClusterId();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Object resourceState = cmd.getResourceState();
        Object haHosts = cmd.getHaHost();

        Pair<List<HostVO>, Integer> result = searchForServers(cmd.getStartIndex(), cmd.getPageSizeVal(), name, type, state, zoneId, pod, cluster, id, keyword, resourceState, haHosts);
        return new Pair<List<? extends Host>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<Pair<List<? extends Host>, Integer>, List<? extends Host>> listHostsForMigrationOfVM(Long vmId, Long startIndex, Long pageSize) {
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
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find the VM with specified id");
            ex.addProxyObject(vm, vmId, "vmId");
            throw ex;
        }
        // business logic
        if (vm.getState() != State.Running) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is not Running, unable to migrate the vm" + vm);
            }
            InvalidParameterValueException ex = new InvalidParameterValueException("VM is not Running, unable to migrate the vm with specified id");
            ex.addProxyObject(vm, vmId, "vmId");
            throw ex;
        }

        if (!vm.getHypervisorType().equals(HypervisorType.XenServer) && !vm.getHypervisorType().equals(HypervisorType.VMware)
                && !vm.getHypervisorType().equals(HypervisorType.KVM) && !vm.getHypervisorType().equals(HypervisorType.Ovm)) {
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
        // why is this not HostVO?
        Host srcHost = _hostDao.findById(srcHostId);
        if (srcHost == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find the host with id: " + srcHostId + " of this VM:" + vm);
            }
            InvalidParameterValueException ex = new InvalidParameterValueException(
                    "Unable to find the host (with specified id) of VM with specified id");
            ex.addProxyObject(srcHost, srcHostId, "hostId");
            ex.addProxyObject(vm, vmId, "vmId");
            throw ex;
        }
        Long cluster = srcHost.getClusterId();
        Type hostType = srcHost.getType();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Searching for all hosts in cluster: " + cluster + " for migrating VM " + vm);
        }

        Pair<List<HostVO>, Integer> allHostsInClusterPair = searchForServers(startIndex, pageSize, null, hostType, null, null, null, cluster, null, null, null, null);

        // filter out the current host
        List<HostVO> allHostsInCluster = allHostsInClusterPair.first();
        allHostsInCluster.remove(srcHost);
        Pair<List<? extends Host>, Integer> otherHostsInCluster = new Pair<List <? extends Host>, Integer>(allHostsInCluster, new Integer(allHostsInClusterPair.second().intValue()-1));

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Other Hosts in this cluster: " + allHostsInCluster);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Calling HostAllocators to search for hosts in cluster: " + cluster + " having enough capacity and suitable for migration");
        }

        List<Host> suitableHosts = new ArrayList<Host>();

        VirtualMachineProfile<VMInstanceVO> vmProfile = new VirtualMachineProfileImpl<VMInstanceVO>(vm);

        DataCenterDeployment plan = new DataCenterDeployment(srcHost.getDataCenterId(), srcHost.getPodId(), srcHost.getClusterId(), null, null, null);
        ExcludeList excludes = new ExcludeList();
        excludes.addHost(srcHostId);

        for (HostAllocator allocator : _hostAllocators) {
            suitableHosts = allocator.allocateTo(vmProfile, plan, Host.Type.Routing, excludes, HostAllocator.RETURN_UPTO_ALL, false);
            if (suitableHosts != null && !suitableHosts.isEmpty()) {
                break;
            }
        }

        if (suitableHosts.isEmpty()) {
            s_logger.debug("No suitable hosts found");
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Hosts having capacity and suitable for migration: " + suitableHosts);
            }
        }

        return new Pair<Pair<List<? extends Host>, Integer>, List<? extends Host>>(otherHostsInCluster, suitableHosts);
    }

    private Pair<List<HostVO>, Integer> searchForServers(Long startIndex, Long pageSize, Object name, Object type, Object state, Object zone, Object pod, Object cluster, Object id, Object keyword,
            Object resourceState, Object haHosts) {
        Filter searchFilter = new Filter(HostVO.class, "id", Boolean.TRUE, startIndex, pageSize);

        SearchBuilder<HostVO> sb = _hostDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.LIKE);
        sb.and("status", sb.entity().getStatus(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("clusterId", sb.entity().getClusterId(), SearchCriteria.Op.EQ);
        sb.and("resourceState", sb.entity().getResourceState(), SearchCriteria.Op.EQ);

        String haTag = _haMgr.getHaTag();
        SearchBuilder<HostTagVO> hostTagSearch = null;
        if (haHosts != null && haTag != null && !haTag.isEmpty()) {
            hostTagSearch = _hostTagsDao.createSearchBuilder();
            if ((Boolean) haHosts) {
                hostTagSearch.and().op("tag", hostTagSearch.entity().getTag(), SearchCriteria.Op.EQ);
            } else {
                hostTagSearch.and().op("tag", hostTagSearch.entity().getTag(), SearchCriteria.Op.NEQ);
                hostTagSearch.or("tagNull", hostTagSearch.entity().getTag(), SearchCriteria.Op.NULL);
            }

            hostTagSearch.cp();
            sb.join("hostTagSearch", hostTagSearch, sb.entity().getId(), hostTagSearch.entity().getHostId(), JoinBuilder.JoinType.LEFTOUTER);
        }

        SearchCriteria<HostVO> sc = sb.create();

        if (keyword != null) {
            SearchCriteria<HostVO> ssc = _hostDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("status", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("type", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }
        if (type != null) {
            sc.setParameters("type", "%" + type);
        }
        if (state != null) {
            sc.setParameters("status", state);
        }
        if (zone != null) {
            sc.setParameters("dataCenterId", zone);
        }
        if (pod != null) {
            sc.setParameters("podId", pod);
        }
        if (cluster != null) {
            sc.setParameters("clusterId", cluster);
        }

        if (resourceState != null) {
            sc.setParameters("resourceState", resourceState);
        }

        if (haHosts != null && haTag != null && !haTag.isEmpty()) {
            sc.setJoinParameters("hostTagSearch", "tag", haTag);
        }

        return _hostDao.searchAndCount(sc, searchFilter);
    }

    @Override
    public Pair<List<? extends Pod>, Integer> searchForPods(ListPodsByCmd cmd) {
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

        Pair<List<HostPodVO>, Integer> result = _hostPodDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Pod>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends Vlan>, Integer> searchForVlans(ListVlanIpRangesCmd cmd) {
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
                InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find account " + accountName
                        + " in specified domain");
                // Since we don't have a DomainVO object here, we directly set
                // tablename to "domain".
                String tablename = "domain";
                ex.addProxyObject(tablename, domainId, "domainId");
                throw ex;
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

        // set project information
        if (projectId != null) {
            Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find project by id " + projectId);
                ex.addProxyObject(project, projectId, "projectId");
                throw ex;
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
            sb.join("accountVlanMapSearch", accountVlanMapSearch, sb.entity().getId(), accountVlanMapSearch.entity().getVlanDbId(),
                    JoinBuilder.JoinType.INNER);
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

        Pair<List<VlanVO>, Integer> result = _vlanDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Vlan>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends Configuration>, Integer> searchForConfigurations(ListCfgsByCmd cmd) {
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

        Pair<List<ConfigurationVO>, Integer> result = _configDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Configuration>, Integer>(result.first(), result.second());
    }

    @Override
    public Set<Pair<Long, Long>> listIsos(ListIsosCmd cmd) throws IllegalArgumentException, InvalidParameterValueException {
        TemplateFilter isoFilter = TemplateFilter.valueOf(cmd.getIsoFilter());
        Account caller = UserContext.current().getCaller();
        Map<String, String> tags = cmd.getTags();

        boolean listAll = false;
        if (isoFilter != null && isoFilter == TemplateFilter.all) {
            if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
                throw new InvalidParameterValueException("Filter " + TemplateFilter.all + " can be specified by admin only");
            }
            listAll = true;
        }
        List<Long> permittedAccountIds = new ArrayList<Long>();
        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, cmd.getId(), cmd.getAccountName(), cmd.getProjectId(), permittedAccountIds,
                domainIdRecursiveListProject, listAll, false);
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        List<Account> permittedAccounts = new ArrayList<Account>();
        for (Long accountId : permittedAccountIds) {
            permittedAccounts.add(_accountMgr.getAccount(accountId));
        }

        HypervisorType hypervisorType = HypervisorType.getType(cmd.getHypervisor());
        return listTemplates(cmd.getId(), cmd.getIsoName(), cmd.getKeyword(), isoFilter, true, cmd.isBootable(), cmd.getPageSizeVal(),
                cmd.getStartIndex(), cmd.getZoneId(), hypervisorType, true, cmd.listInReadyState(), permittedAccounts, caller,
                listProjectResourcesCriteria, tags);
    }

    @Override
    public Set<Pair<Long, Long>> listTemplates(ListTemplatesCmd cmd) throws IllegalArgumentException, InvalidParameterValueException {
        TemplateFilter templateFilter = TemplateFilter.valueOf(cmd.getTemplateFilter());
        Long id = cmd.getId();
        Map<String, String> tags = cmd.getTags();
        Account caller = UserContext.current().getCaller();

        boolean listAll = false;
        if (templateFilter != null && templateFilter == TemplateFilter.all) {
            if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
                throw new InvalidParameterValueException("Filter " + TemplateFilter.all + " can be specified by admin only");
            }
            listAll = true;
        }

        List<Long> permittedAccountIds = new ArrayList<Long>();
        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccountIds, domainIdRecursiveListProject,
                listAll, false);
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        List<Account> permittedAccounts = new ArrayList<Account>();
        for (Long accountId : permittedAccountIds) {
            permittedAccounts.add(_accountMgr.getAccount(accountId));
        }

        boolean showDomr = ((templateFilter != TemplateFilter.selfexecutable) && (templateFilter != TemplateFilter.featured));
        HypervisorType hypervisorType = HypervisorType.getType(cmd.getHypervisor());

        return listTemplates(id, cmd.getTemplateName(), cmd.getKeyword(), templateFilter, false, null, cmd.getPageSizeVal(), cmd.getStartIndex(),
                cmd.getZoneId(), hypervisorType, showDomr, cmd.listInReadyState(), permittedAccounts, caller, listProjectResourcesCriteria, tags);
    }

    private Set<Pair<Long, Long>> listTemplates(Long templateId, String name, String keyword, TemplateFilter templateFilter, boolean isIso,
            Boolean bootable, Long pageSize, Long startIndex, Long zoneId, HypervisorType hyperType, boolean showDomr, boolean onlyReady,
            List<Account> permittedAccounts, Account caller, ListProjectResourcesCriteria listProjectResourcesCriteria, Map<String, String> tags) {

        VMTemplateVO template = null;
        if (templateId != null) {
            template = _templateDao.findById(templateId);
            if (template == null) {
                throw new InvalidParameterValueException("Please specify a valid template ID.");
            }// If ISO requested then it should be ISO.
            if (isIso && template.getFormat() != ImageFormat.ISO) {
                s_logger.error("Template Id " + templateId + " is not an ISO");
                InvalidParameterValueException ex = new InvalidParameterValueException("Specified Template Id is not an ISO");
                ex.addProxyObject(template, templateId, "templateId");
                throw ex;
            }// If ISO not requested then it shouldn't be an ISO.
            if (!isIso && template.getFormat() == ImageFormat.ISO) {
                s_logger.error("Incorrect format of the template id " + templateId);
                InvalidParameterValueException ex = new InvalidParameterValueException("Incorrect format " + template.getFormat()
                        + " of the specified template id");
                ex.addProxyObject(template, templateId, "templateId");
                throw ex;
            }
        }

        DomainVO domain = null;
        if (!permittedAccounts.isEmpty()) {
            domain = _domainDao.findById(permittedAccounts.get(0).getDomainId());
        } else {
            domain = _domainDao.findById(DomainVO.ROOT_DOMAIN);
        }

        List<HypervisorType> hypers = null;
        if (!isIso) {
            hypers = _resourceMgr.listAvailHypervisorInZone(null, null);
        }
        Set<Pair<Long, Long>> templateZonePairSet = new HashSet<Pair<Long, Long>>();
        if (_swiftMgr.isSwiftEnabled()) {
            if (template == null) {
                templateZonePairSet = _templateDao.searchSwiftTemplates(name, keyword, templateFilter, isIso, hypers, bootable, domain, pageSize,
                        startIndex, zoneId, hyperType, onlyReady, showDomr, permittedAccounts, caller, tags);
                Set<Pair<Long, Long>> templateZonePairSet2 = new HashSet<Pair<Long, Long>>();
                templateZonePairSet2 = _templateDao.searchTemplates(name, keyword, templateFilter, isIso, hypers, bootable, domain, pageSize,
                        startIndex, zoneId, hyperType, onlyReady, showDomr, permittedAccounts, caller, listProjectResourcesCriteria, tags);

                for (Pair<Long, Long> tmpltPair : templateZonePairSet2) {
                    if (!templateZonePairSet.contains(new Pair<Long, Long>(tmpltPair.first(), -1L))) {
                        templateZonePairSet.add(tmpltPair);
                    }
                }

            } else {
                // if template is not public, perform permission check here
                if (!template.isPublicTemplate() && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                    Account owner = _accountMgr.getAccount(template.getAccountId());
                    _accountMgr.checkAccess(caller, null, true, owner);
                }
                templateZonePairSet.add(new Pair<Long, Long>(template.getId(), zoneId));
            }
        } else if (_s3Mgr.isS3Enabled()) {
            if (template == null) {
                templateZonePairSet = _templateDao.searchSwiftTemplates(name, keyword, templateFilter, isIso,
                        hypers, bootable, domain, pageSize, startIndex, zoneId, hyperType, onlyReady, showDomr,
                        permittedAccounts, caller, tags);
                Set<Pair<Long, Long>> templateZonePairSet2 = new HashSet<Pair<Long, Long>>();
                templateZonePairSet2 = _templateDao.searchTemplates(name, keyword, templateFilter, isIso, hypers,
                        bootable, domain, pageSize, startIndex, zoneId, hyperType, onlyReady, showDomr,
                        permittedAccounts, caller, listProjectResourcesCriteria, tags);

                for (Pair<Long, Long> tmpltPair : templateZonePairSet2) {
                    if (!templateZonePairSet.contains(new Pair<Long, Long>(tmpltPair.first(), -1L))) {
                        templateZonePairSet.add(tmpltPair);
                    }
                }
            } else {
                // if template is not public, perform permission check here
                if (!template.isPublicTemplate() && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                    Account owner = _accountMgr.getAccount(template.getAccountId());
                    _accountMgr.checkAccess(caller, null, true, owner);
                }
                templateZonePairSet.add(new Pair<Long, Long>(template.getId(), zoneId));
            }
        } else {
            if (template == null) {
                templateZonePairSet = _templateDao.searchTemplates(name, keyword, templateFilter, isIso, hypers, bootable, domain, pageSize,
                        startIndex, zoneId, hyperType, onlyReady, showDomr, permittedAccounts, caller, listProjectResourcesCriteria, tags);
            } else {
                // if template is not public, perform permission check here
                if (!template.isPublicTemplate() && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                    Account owner = _accountMgr.getAccount(template.getAccountId());
                    _accountMgr.checkAccess(caller, null, true, owner);
                }
                templateZonePairSet.add(new Pair<Long, Long>(template.getId(), zoneId));
            }
        }

        return templateZonePairSet;
    }

    @Override
    public VMTemplateVO updateTemplate(UpdateIsoCmd cmd) {
        return updateTemplateOrIso(cmd);
    }

    @Override
    public VMTemplateVO updateTemplate(UpdateTemplateCmd cmd) {
        return updateTemplateOrIso(cmd);
    }

    private VMTemplateVO updateTemplateOrIso(BaseUpdateTemplateOrIsoCmd cmd) {
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
            InvalidParameterValueException ex = new InvalidParameterValueException("unable to find template/iso with specified id");
            ex.addProxyObject(template, id, "templateId");
            throw ex;
        }

        // Don't allow to modify system template
        if (id == Long.valueOf(1)) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to update template/iso of specified id");
            ex.addProxyObject(template, id, "templateId");
            throw ex;
        }

        // do a permission check
        _accountMgr.checkAccess(account, AccessType.ModifyEntry, true, template);

        boolean updateNeeded = !(name == null && displayText == null && format == null && guestOSId == null && passwordEnabled == null
                && bootable == null && sortKey == null);
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
                throw new InvalidParameterValueException("Image format: " + format + " is incorrect. Supported formats are "
                        + EnumUtils.listValues(ImageFormat.values()));
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
    public Pair<List<? extends IpAddress>, Integer> searchForIPAddresses(ListPublicIpAddressesCmd cmd) {
        Object keyword = cmd.getKeyword();
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        Long associatedNetworkId = cmd.getAssociatedNetworkId();
        Long zone = cmd.getZoneId();
        String address = cmd.getIpAddress();
        Long vlan = cmd.getVlanId();
        Boolean forVirtualNetwork = cmd.isForVirtualNetwork();
        Boolean forLoadBalancing = cmd.isForLoadBalancing();
        Long ipId = cmd.getId();
        Boolean sourceNat = cmd.getIsSourceNat();
        Boolean staticNat = cmd.getIsStaticNat();
        Long vpcId = cmd.getVpcId();
        Map<String, String> tags = cmd.getTags();

        Boolean isAllocated = cmd.isAllocatedOnly();
        if (isAllocated == null) {
            isAllocated = Boolean.TRUE;
        }

        Filter searchFilter = new Filter(IPAddressVO.class, "address", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<IPAddressVO> sb = _publicIpAddressDao.createSearchBuilder();
        Long domainId = null;
        Boolean isRecursive = null;
        List<Long> permittedAccounts = new ArrayList<Long>();
        ListProjectResourcesCriteria listProjectResourcesCriteria = null;
        if (isAllocated) {
            Account caller = UserContext.current().getCaller();

            Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                    cmd.getDomainId(), cmd.isRecursive(), null);
            _accountMgr.buildACLSearchParameters(caller, cmd.getId(), cmd.getAccountName(), cmd.getProjectId(), permittedAccounts,
                    domainIdRecursiveListProject, cmd.listAll(), false);
            domainId = domainIdRecursiveListProject.first();
            isRecursive = domainIdRecursiveListProject.second();
            listProjectResourcesCriteria = domainIdRecursiveListProject.third();
            _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        }

        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("address", sb.entity().getAddress(), SearchCriteria.Op.EQ);
        sb.and("vlanDbId", sb.entity().getVlanId(), SearchCriteria.Op.EQ);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("physicalNetworkId", sb.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);
        sb.and("associatedNetworkIdEq", sb.entity().getAssociatedWithNetworkId(), SearchCriteria.Op.EQ);
        sb.and("isSourceNat", sb.entity().isSourceNat(), SearchCriteria.Op.EQ);
        sb.and("isStaticNat", sb.entity().isOneToOneNat(), SearchCriteria.Op.EQ);
        sb.and("vpcId", sb.entity().getVpcId(), SearchCriteria.Op.EQ);

        if (forLoadBalancing != null && forLoadBalancing) {
            SearchBuilder<LoadBalancerVO> lbSearch = _loadbalancerDao.createSearchBuilder();
            sb.join("lbSearch", lbSearch, sb.entity().getId(), lbSearch.entity().getSourceIpAddressId(), JoinType.INNER);
            sb.groupBy(sb.entity().getId());
        }

        if (keyword != null && address == null) {
            sb.and("addressLIKE", sb.entity().getAddress(), SearchCriteria.Op.LIKE);
        }

        if (tags != null && !tags.isEmpty()) {
            SearchBuilder<ResourceTagVO> tagSearch = _resourceTagDao.createSearchBuilder();
            for (int count = 0; count < tags.size(); count++) {
                tagSearch.or().op("key" + String.valueOf(count), tagSearch.entity().getKey(), SearchCriteria.Op.EQ);
                tagSearch.and("value" + String.valueOf(count), tagSearch.entity().getValue(), SearchCriteria.Op.EQ);
                tagSearch.cp();
            }
            tagSearch.and("resourceType", tagSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
            sb.groupBy(sb.entity().getId());
            sb.join("tagSearch", tagSearch, sb.entity().getId(), tagSearch.entity().getResourceId(), JoinBuilder.JoinType.INNER);
        }

        SearchBuilder<VlanVO> vlanSearch = _vlanDao.createSearchBuilder();
        vlanSearch.and("vlanType", vlanSearch.entity().getVlanType(), SearchCriteria.Op.EQ);
        sb.join("vlanSearch", vlanSearch, sb.entity().getVlanId(), vlanSearch.entity().getId(), JoinBuilder.JoinType.INNER);

        boolean allocatedOnly = false;
        if ((isAllocated != null) && (isAllocated == true)) {
            sb.and("allocated", sb.entity().getAllocatedTime(), SearchCriteria.Op.NNULL);
            allocatedOnly = true;
        }

        VlanType vlanType = null;
        if (forVirtualNetwork != null) {
            vlanType = forVirtualNetwork ? VlanType.VirtualNetwork : VlanType.DirectAttached;
        } else {
            vlanType = VlanType.VirtualNetwork;
        }

        SearchCriteria<IPAddressVO> sc = sb.create();
        if (isAllocated) {
            _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        }

        sc.setJoinParameters("vlanSearch", "vlanType", vlanType);

        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", TaggedResourceType.PublicIpAddress.toString());
            for (String key : tags.keySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), key);
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), tags.get(key));
                count++;
            }
        }

        if (zone != null) {
            sc.setParameters("dataCenterId", zone);
        }

        if (vpcId != null) {
            sc.setParameters("vpcId", vpcId);
        }

        if (ipId != null) {
            sc.setParameters("id", ipId);
        }

        if (sourceNat != null) {
            sc.setParameters("isSourceNat", sourceNat);
        }

        if (staticNat != null) {
            sc.setParameters("isStaticNat", staticNat);
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
            sc.setParameters("associatedNetworkIdEq", associatedNetworkId);
        }

        Pair<List<IPAddressVO>, Integer> result = _publicIpAddressDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends IpAddress>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends GuestOS>, Integer> listGuestOSByCriteria(ListGuestOsCmd cmd) {
        Filter searchFilter = new Filter(GuestOSVO.class, "displayName", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Long id = cmd.getId();
        Long osCategoryId = cmd.getOsCategoryId();
        String description = cmd.getDescription();
        String keyword = cmd.getKeyword();

        SearchCriteria<GuestOSVO> sc = _guestOSDao.createSearchCriteria();

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (osCategoryId != null) {
            sc.addAnd("categoryId", SearchCriteria.Op.EQ, osCategoryId);
        }

        if (description != null) {
            sc.addAnd("displayName", SearchCriteria.Op.LIKE, "%" + description + "%");
        }

        if (keyword != null) {
            sc.addAnd("displayName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
        }

        Pair<List<GuestOSVO>, Integer> result = _guestOSDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends GuestOS>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends GuestOsCategory>, Integer> listGuestOSCategoriesByCriteria(ListGuestOsCategoriesCmd cmd) {
        Filter searchFilter = new Filter(GuestOSCategoryVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Long id = cmd.getId();
        String name = cmd.getName();
        String keyword = cmd.getKeyword();

        SearchCriteria<GuestOSCategoryVO> sc = _guestOSCategoryDao.createSearchCriteria();

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }

        if (keyword != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
        }

        Pair<List<GuestOSCategoryVO>, Integer> result = _guestOSCategoryDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends GuestOsCategory>, Integer>(result.first(), result.second());
    }

    @Override
    public ConsoleProxyInfo getConsoleProxyForVm(long dataCenterId, long userVmId) {
        return _consoleProxyMgr.assignProxy(dataCenterId, userVmId);
    }

    @ActionEvent(eventType = EventTypes.EVENT_PROXY_START, eventDescription = "starting console proxy Vm", async = true)
    private ConsoleProxyVO startConsoleProxy(long instanceId) {
        return _consoleProxyMgr.startProxy(instanceId);
    }

    @ActionEvent(eventType = EventTypes.EVENT_PROXY_STOP, eventDescription = "stopping console proxy Vm", async = true)
    private ConsoleProxyVO stopConsoleProxy(VMInstanceVO systemVm, boolean isForced) throws ResourceUnavailableException, OperationTimedoutException,
    ConcurrentOperationException {

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
        VMInstanceVO vm = _vmInstanceDao.findById(vmId);
        if (vm != null) {
            ConsoleProxyInfo proxy = getConsoleProxyForVm(vm.getDataCenterId(), vmId);
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
    @ActionEvent(eventType = EventTypes.EVENT_DOMAIN_UPDATE, eventDescription = "updating Domain")
    @DB
    public DomainVO updateDomain(UpdateDomainCmd cmd) {
        Long domainId = cmd.getId();
        String domainName = cmd.getDomainName();
        String networkDomain = cmd.getNetworkDomain();

        // check if domain exists in the system
        DomainVO domain = _domainDao.findById(domainId);
        if (domain == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find domain with specified domain id");
            ex.addProxyObject(domain, domainId, "domainId");
            throw ex;
        } else if (domain.getParent() == null && domainName != null) {
            // check if domain is ROOT domain - and deny to edit it with the new
            // name
            throw new InvalidParameterValueException("ROOT domain can not be edited with a new name");
        }

        // check permissions
        Account caller = UserContext.current().getCaller();
        _accountMgr.checkAccess(caller, domain);

        // domain name is unique under the parent domain
        if (domainName != null) {
            SearchCriteria<DomainVO> sc = _domainDao.createSearchCriteria();
            sc.addAnd("name", SearchCriteria.Op.EQ, domainName);
            sc.addAnd("parent", SearchCriteria.Op.EQ, domain.getParent());
            List<DomainVO> domains = _domainDao.search(sc, null);

            boolean sameDomain = (domains.size() == 1 && domains.get(0).getId() == domainId);

            if (!domains.isEmpty() && !sameDomain) {
                InvalidParameterValueException ex = new InvalidParameterValueException("Failed to update specified domain id with name '"
                        + domainName + "' since it already exists in the system");
                ex.addProxyObject(domain, domainId, "domainId");
                throw ex;
            }
        }

        // validate network domain
        if (networkDomain != null && !networkDomain.isEmpty()) {
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
            if (networkDomain.isEmpty()) {
                domain.setNetworkDomain(null);
            } else {
                domain.setNetworkDomain(networkDomain);
            }
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
    public Pair<List<? extends Alert>, Integer> searchForAlerts(ListAlertsCmd cmd) {
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

        sc.addAnd("archived", SearchCriteria.Op.EQ, false);
        Pair<List<AlertVO>, Integer> result = _alertDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Alert>, Integer>(result.first(), result.second());
    }

    @Override
    public boolean archiveAlerts(ArchiveAlertsCmd cmd) {
        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(UserContext.current().getCaller(), null);
        boolean result = _alertDao.archiveAlert(cmd.getIds(), cmd.getType(), cmd.getOlderThan(), zoneId);
        return result;
    }

    @Override
    public boolean deleteAlerts(DeleteAlertsCmd cmd) {
        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(UserContext.current().getCaller(), null);
        boolean result = _alertDao.deleteAlert(cmd.getIds(), cmd.getType(), cmd.getOlderThan(), zoneId);
        return result;
    }

    @Override
    public List<CapacityVO> listTopConsumedResources(ListCapacityCmd cmd) {

        Integer capacityType = cmd.getType();
        Long zoneId = cmd.getZoneId();
        Long podId = cmd.getPodId();
        Long clusterId = cmd.getClusterId();

        if (clusterId != null) {
            throw new InvalidParameterValueException("Currently clusterId param is not suppoerted");
        }
        zoneId = _accountMgr.checkAccessAndSpecifyAuthority(UserContext.current().getCaller(), zoneId);
        List<SummedCapacity> summedCapacities = new ArrayList<SummedCapacity>();

        if (zoneId == null && podId == null) {// Group by Zone, capacity type
            List<SummedCapacity> summedCapacitiesAtZone = _capacityDao.listCapacitiesGroupedByLevelAndType(capacityType, zoneId, podId, clusterId, 1,
                    cmd.getPageSizeVal());
            if (summedCapacitiesAtZone != null) {
                summedCapacities.addAll(summedCapacitiesAtZone);
            }
        }
        if (podId == null) {// Group by Pod, capacity type
            List<SummedCapacity> summedCapacitiesAtPod = _capacityDao.listCapacitiesGroupedByLevelAndType(capacityType, zoneId, podId, clusterId, 2,
                    cmd.getPageSizeVal());
            if (summedCapacitiesAtPod != null) {
                summedCapacities.addAll(summedCapacitiesAtPod);
            }
            List<SummedCapacity> summedCapacitiesForSecStorage = getSecStorageUsed(zoneId, capacityType);
            if (summedCapacitiesForSecStorage != null) {
                summedCapacities.addAll(summedCapacitiesForSecStorage);
            }
        }

        // Group by Cluster, capacity type
        List<SummedCapacity> summedCapacitiesAtCluster = _capacityDao.listCapacitiesGroupedByLevelAndType(capacityType, zoneId, podId, clusterId, 3,
                cmd.getPageSizeVal());
        if (summedCapacitiesAtCluster != null) {
            summedCapacities.addAll(summedCapacitiesAtCluster);
        }

        // Sort Capacities
        Collections.sort(summedCapacities, new Comparator<SummedCapacity>() {
            @Override
            public int compare(SummedCapacity arg0, SummedCapacity arg1) {
                if (arg0.getPercentUsed() < arg1.getPercentUsed()) {
                    return 1;
                } else if (arg0.getPercentUsed() == arg1.getPercentUsed()) {
                    return 0;
                }
                return -1;
            }
        });

        List<CapacityVO> capacities = new ArrayList<CapacityVO>();

        Integer pageSize = null;
        try {
            pageSize = Integer.valueOf(cmd.getPageSizeVal().toString());
        } catch (IllegalArgumentException e) {
            throw new InvalidParameterValueException("pageSize " + cmd.getPageSizeVal() + " is out of Integer range is not supported for this call");
        }

        summedCapacities = summedCapacities.subList(0, summedCapacities.size() < cmd.getPageSizeVal() ? summedCapacities.size() : pageSize);
        for (SummedCapacity summedCapacity : summedCapacities) {
            CapacityVO capacity = new CapacityVO(summedCapacity.getDataCenterId(), summedCapacity.getPodId(), summedCapacity.getClusterId(),
                    summedCapacity.getCapacityType(), summedCapacity.getPercentUsed());
            capacity.setUsedCapacity(summedCapacity.getUsedCapacity());
            capacity.setTotalCapacity(summedCapacity.getTotalCapacity());
            capacities.add(capacity);
        }
        return capacities;
    }

    List<SummedCapacity> getSecStorageUsed(Long zoneId, Integer capacityType) {
        if (capacityType == null || capacityType == Capacity.CAPACITY_TYPE_SECONDARY_STORAGE) {
            List<SummedCapacity> list = new ArrayList<SummedCapacity>();
            if (zoneId != null) {
                DataCenterVO zone = ApiDBUtils.findZoneById(zoneId);
                if (zone == null || zone.getAllocationState() == AllocationState.Disabled) {
                    return null;
                }
                CapacityVO capacity = _storageMgr.getSecondaryStorageUsedStats(null, zoneId);
                if (capacity.getTotalCapacity() != 0) {
                    capacity.setUsedPercentage(capacity.getUsedCapacity() / capacity.getTotalCapacity());
                } else {
                    capacity.setUsedPercentage(0);
                }
                SummedCapacity summedCapacity = new SummedCapacity(capacity.getUsedCapacity(), capacity.getTotalCapacity(),
                        capacity.getUsedPercentage(), capacity.getCapacityType(), capacity.getDataCenterId(), capacity.getPodId(),
                        capacity.getClusterId());
                list.add(summedCapacity);
            } else {
                List<DataCenterVO> dcList = _dcDao.listEnabledZones();
                for (DataCenterVO dc : dcList) {
                    CapacityVO capacity = _storageMgr.getSecondaryStorageUsedStats(null, dc.getId());
                    if (capacity.getTotalCapacity() != 0) {
                        capacity.setUsedPercentage((float) capacity.getUsedCapacity() / capacity.getTotalCapacity());
                    } else {
                        capacity.setUsedPercentage(0);
                    }
                    SummedCapacity summedCapacity = new SummedCapacity(capacity.getUsedCapacity(), capacity.getTotalCapacity(),
                            capacity.getUsedPercentage(), capacity.getCapacityType(), capacity.getDataCenterId(), capacity.getPodId(),
                            capacity.getClusterId());
                    list.add(summedCapacity);
                }// End of for
            }
            return list;
        }
        return null;
    }

    @Override
    public List<CapacityVO> listCapacities(ListCapacityCmd cmd) {

        Integer capacityType = cmd.getType();
        Long zoneId = cmd.getZoneId();
        Long podId = cmd.getPodId();
        Long clusterId = cmd.getClusterId();
        Boolean fetchLatest = cmd.getFetchLatest();

        zoneId = _accountMgr.checkAccessAndSpecifyAuthority(UserContext.current().getCaller(), zoneId);
        if (fetchLatest != null && fetchLatest) {
            _alertMgr.recalculateCapacity();
        }

        List<SummedCapacity> summedCapacities = _capacityDao.findCapacityBy(capacityType, zoneId, podId, clusterId);
        List<CapacityVO> capacities = new ArrayList<CapacityVO>();

        for (SummedCapacity summedCapacity : summedCapacities) {
            CapacityVO capacity = new CapacityVO(null, summedCapacity.getDataCenterId(), podId, clusterId, summedCapacity.getUsedCapacity()
                    + summedCapacity.getReservedCapacity(), summedCapacity.getTotalCapacity(), summedCapacity.getCapacityType());

            if (summedCapacity.getCapacityType() == Capacity.CAPACITY_TYPE_CPU) {
                capacity.setTotalCapacity((long) (summedCapacity.getTotalCapacity() * ApiDBUtils.getCpuOverprovisioningFactor()));
            }
            capacities.add(capacity);
        }

        // op_host_Capacity contains only allocated stats and the real time
        // stats are stored "in memory".
        // Show Sec. Storage only when the api is invoked for the zone layer.
        List<DataCenterVO> dcList = new ArrayList<DataCenterVO>();
        if (zoneId == null && podId == null && clusterId == null) {
            dcList = ApiDBUtils.listZones();
        } else if (zoneId != null) {
            dcList.add(ApiDBUtils.findZoneById(zoneId));
        } else {
            if (clusterId != null) {
                zoneId = ApiDBUtils.findClusterById(clusterId).getDataCenterId();
            } else {
                zoneId = ApiDBUtils.findPodById(podId).getDataCenterId();
            }
            if (capacityType == null || capacityType == Capacity.CAPACITY_TYPE_STORAGE) {
                capacities.add(_storageMgr.getStoragePoolUsedStats(null, clusterId, podId, zoneId));
            }
        }

        for (DataCenterVO zone : dcList) {
            zoneId = zone.getId();
            if ((capacityType == null || capacityType == Capacity.CAPACITY_TYPE_SECONDARY_STORAGE) && podId == null && clusterId == null) {
                capacities.add(_storageMgr.getSecondaryStorageUsedStats(null, zoneId));
            }
            if (capacityType == null || capacityType == Capacity.CAPACITY_TYPE_STORAGE) {
                capacities.add(_storageMgr.getStoragePoolUsedStats(null, clusterId, podId, zoneId));
            }
        }
        return capacities;
    }

    @Override
    public long getMemoryOrCpuCapacityByHost(Long hostId, short capacityType) {

        CapacityVO capacity = _capacityDao.findByHostIdType(hostId, capacityType);
        return capacity == null ? 0 : capacity.getReservedCapacity() + capacity.getUsedCapacity();

    }

    public static boolean isAdmin(short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN) || (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN)
                || (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
    }



    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateAccountCmd.class);
        cmdList.add(DeleteAccountCmd.class);
        cmdList.add(DisableAccountCmd.class);
        cmdList.add(EnableAccountCmd.class);
        cmdList.add(LockAccountCmd.class);
        cmdList.add(UpdateAccountCmd.class);
        cmdList.add(CreateCounterCmd.class);
        cmdList.add(DeleteCounterCmd.class);
        cmdList.add(AddClusterCmd.class);
        cmdList.add(DeleteClusterCmd.class);
        cmdList.add(ListClustersCmd.class);
        cmdList.add(UpdateClusterCmd.class);
        cmdList.add(ListCfgsByCmd.class);
        cmdList.add(ListHypervisorCapabilitiesCmd.class);
        cmdList.add(UpdateCfgCmd.class);
        cmdList.add(UpdateHypervisorCapabilitiesCmd.class);
        cmdList.add(CreateDomainCmd.class);
        cmdList.add(DeleteDomainCmd.class);
        cmdList.add(ListDomainChildrenCmd.class);
        cmdList.add(ListDomainsCmd.class);
        cmdList.add(UpdateDomainCmd.class);
        cmdList.add(AddHostCmd.class);
        cmdList.add(AddSecondaryStorageCmd.class);
        cmdList.add(CancelMaintenanceCmd.class);
        cmdList.add(DeleteHostCmd.class);
        cmdList.add(ListHostsCmd.class);
        cmdList.add(PrepareForMaintenanceCmd.class);
        cmdList.add(ReconnectHostCmd.class);
        cmdList.add(UpdateHostCmd.class);
        cmdList.add(UpdateHostPasswordCmd.class);
        cmdList.add(LDAPConfigCmd.class);
        cmdList.add(LDAPRemoveCmd.class);
        cmdList.add(AddNetworkDeviceCmd.class);
        cmdList.add(AddNetworkServiceProviderCmd.class);
        cmdList.add(CreateNetworkOfferingCmd.class);
        cmdList.add(CreatePhysicalNetworkCmd.class);
        cmdList.add(CreateStorageNetworkIpRangeCmd.class);
        cmdList.add(DeleteNetworkDeviceCmd.class);
        cmdList.add(DeleteNetworkOfferingCmd.class);
        cmdList.add(DeleteNetworkServiceProviderCmd.class);
        cmdList.add(DeletePhysicalNetworkCmd.class);
        cmdList.add(DeleteStorageNetworkIpRangeCmd.class);
        cmdList.add(ListNetworkDeviceCmd.class);
        cmdList.add(ListNetworkServiceProvidersCmd.class);
        cmdList.add(ListPhysicalNetworksCmd.class);
        cmdList.add(ListStorageNetworkIpRangeCmd.class);
        cmdList.add(ListSupportedNetworkServicesCmd.class);
        cmdList.add(UpdateNetworkOfferingCmd.class);
        cmdList.add(UpdateNetworkServiceProviderCmd.class);
        cmdList.add(UpdatePhysicalNetworkCmd.class);
        cmdList.add(UpdateStorageNetworkIpRangeCmd.class);
        cmdList.add(CreateDiskOfferingCmd.class);
        cmdList.add(CreateServiceOfferingCmd.class);
        cmdList.add(DeleteDiskOfferingCmd.class);
        cmdList.add(DeleteServiceOfferingCmd.class);
        cmdList.add(UpdateDiskOfferingCmd.class);
        cmdList.add(UpdateServiceOfferingCmd.class);
        cmdList.add(CreatePodCmd.class);
        cmdList.add(DeletePodCmd.class);
        cmdList.add(ListPodsByCmd.class);
        cmdList.add(UpdatePodCmd.class);
        cmdList.add(AddRegionCmd.class);
        cmdList.add(RemoveRegionCmd.class);
        cmdList.add(UpdateRegionCmd.class);
        cmdList.add(ListAlertsCmd.class);
        cmdList.add(ListCapacityCmd.class);
        cmdList.add(UploadCustomCertificateCmd.class);
        cmdList.add(ConfigureVirtualRouterElementCmd.class);
        cmdList.add(CreateVirtualRouterElementCmd.class);
        cmdList.add(DestroyRouterCmd.class);
        cmdList.add(ListRoutersCmd.class);
        cmdList.add(ListVirtualRouterElementsCmd.class);
        cmdList.add(RebootRouterCmd.class);
        cmdList.add(StartRouterCmd.class);
        cmdList.add(StopRouterCmd.class);
        cmdList.add(UpgradeRouterCmd.class);
        cmdList.add(AddS3Cmd.class);
        cmdList.add(CancelPrimaryStorageMaintenanceCmd.class);
        cmdList.add(CreateStoragePoolCmd.class);
        cmdList.add(DeletePoolCmd.class);
        cmdList.add(ListS3sCmd.class);
        cmdList.add(ListStoragePoolsCmd.class);
        cmdList.add(PreparePrimaryStorageForMaintenanceCmd.class);
        cmdList.add(UpdateStoragePoolCmd.class);
        cmdList.add(AddSwiftCmd.class);
        cmdList.add(ListSwiftsCmd.class);
        cmdList.add(DestroySystemVmCmd.class);
        cmdList.add(ListSystemVMsCmd.class);
        cmdList.add(MigrateSystemVMCmd.class);
        cmdList.add(RebootSystemVmCmd.class);
        cmdList.add(StartSystemVMCmd.class);
        cmdList.add(StopSystemVmCmd.class);
        cmdList.add(UpgradeSystemVMCmd.class);
        cmdList.add(PrepareTemplateCmd.class);
        cmdList.add(AddTrafficMonitorCmd.class);
        cmdList.add(AddTrafficTypeCmd.class);
        cmdList.add(DeleteTrafficMonitorCmd.class);
        cmdList.add(DeleteTrafficTypeCmd.class);
        cmdList.add(GenerateUsageRecordsCmd.class);
        cmdList.add(GetUsageRecordsCmd.class);
        cmdList.add(ListTrafficMonitorsCmd.class);
        cmdList.add(ListTrafficTypeImplementorsCmd.class);
        cmdList.add(ListTrafficTypesCmd.class);
        cmdList.add(ListUsageTypesCmd.class);
        cmdList.add(UpdateTrafficTypeCmd.class);
        cmdList.add(CreateUserCmd.class);
        cmdList.add(DeleteUserCmd.class);
        cmdList.add(DisableUserCmd.class);
        cmdList.add(EnableUserCmd.class);
        cmdList.add(GetUserCmd.class);
        cmdList.add(ListUsersCmd.class);
        cmdList.add(LockUserCmd.class);
        cmdList.add(RegisterCmd.class);
        cmdList.add(UpdateUserCmd.class);
        cmdList.add(CreateVlanIpRangeCmd.class);
        cmdList.add(DeleteVlanIpRangeCmd.class);
        cmdList.add(ListVlanIpRangesCmd.class);
        cmdList.add(AssignVMCmd.class);
        cmdList.add(MigrateVMCmd.class);
        cmdList.add(RecoverVMCmd.class);
        cmdList.add(CreatePrivateGatewayCmd.class);
        cmdList.add(CreateVPCOfferingCmd.class);
        cmdList.add(DeletePrivateGatewayCmd.class);
        cmdList.add(DeleteVPCOfferingCmd.class);
        cmdList.add(UpdateVPCOfferingCmd.class);
        cmdList.add(CreateZoneCmd.class);
        cmdList.add(DeleteZoneCmd.class);
        cmdList.add(MarkDefaultZoneForAccountCmd.class);
        cmdList.add(UpdateZoneCmd.class);
        cmdList.add(AddAccountToProjectCmd.class);
        cmdList.add(DeleteAccountFromProjectCmd.class);
        cmdList.add(ListAccountsCmd.class);
        cmdList.add(ListProjectAccountsCmd.class);
        cmdList.add(AssociateIPAddrCmd.class);
        cmdList.add(DisassociateIPAddrCmd.class);
        cmdList.add(ListPublicIpAddressesCmd.class);
        cmdList.add(CreateAutoScalePolicyCmd.class);
        cmdList.add(CreateAutoScaleVmGroupCmd.class);
        cmdList.add(CreateAutoScaleVmProfileCmd.class);
        cmdList.add(CreateConditionCmd.class);
        cmdList.add(DeleteAutoScalePolicyCmd.class);
        cmdList.add(DeleteAutoScaleVmGroupCmd.class);
        cmdList.add(DeleteAutoScaleVmProfileCmd.class);
        cmdList.add(DeleteConditionCmd.class);
        cmdList.add(DisableAutoScaleVmGroupCmd.class);
        cmdList.add(EnableAutoScaleVmGroupCmd.class);
        cmdList.add(ListAutoScalePoliciesCmd.class);
        cmdList.add(ListAutoScaleVmGroupsCmd.class);
        cmdList.add(ListAutoScaleVmProfilesCmd.class);
        cmdList.add(ListConditionsCmd.class);
        cmdList.add(ListCountersCmd.class);
        cmdList.add(UpdateAutoScalePolicyCmd.class);
        cmdList.add(UpdateAutoScaleVmGroupCmd.class);
        cmdList.add(UpdateAutoScaleVmProfileCmd.class);
        cmdList.add(ListCapabilitiesCmd.class);
        cmdList.add(ListEventsCmd.class);
        cmdList.add(ListEventTypesCmd.class);
        cmdList.add(CreateEgressFirewallRuleCmd.class);
        cmdList.add(CreateFirewallRuleCmd.class);
        cmdList.add(CreatePortForwardingRuleCmd.class);
        cmdList.add(DeleteEgressFirewallRuleCmd.class);
        cmdList.add(DeleteFirewallRuleCmd.class);
        cmdList.add(DeletePortForwardingRuleCmd.class);
        cmdList.add(ListEgressFirewallRulesCmd.class);
        cmdList.add(ListFirewallRulesCmd.class);
        cmdList.add(ListPortForwardingRulesCmd.class);
        cmdList.add(UpdatePortForwardingRuleCmd.class);
        cmdList.add(ListGuestOsCategoriesCmd.class);
        cmdList.add(ListGuestOsCmd.class);
        cmdList.add(AttachIsoCmd.class);
        cmdList.add(CopyIsoCmd.class);
        cmdList.add(DeleteIsoCmd.class);
        cmdList.add(DetachIsoCmd.class);
        cmdList.add(ExtractIsoCmd.class);
        cmdList.add(ListIsoPermissionsCmd.class);
        cmdList.add(ListIsosCmd.class);
        cmdList.add(RegisterIsoCmd.class);
        cmdList.add(UpdateIsoCmd.class);
        cmdList.add(UpdateIsoPermissionsCmd.class);
        cmdList.add(ListAsyncJobsCmd.class);
        cmdList.add(QueryAsyncJobResultCmd.class);
        cmdList.add(AssignToLoadBalancerRuleCmd.class);
        cmdList.add(CreateLBStickinessPolicyCmd.class);
        cmdList.add(CreateLBHealthCheckPolicyCmd .class);
        cmdList.add(CreateLoadBalancerRuleCmd.class);
        cmdList.add(DeleteLBStickinessPolicyCmd.class);
        cmdList.add(DeleteLBHealthCheckPolicyCmd .class);
        cmdList.add(DeleteLoadBalancerRuleCmd.class);
        cmdList.add(ListLBStickinessPoliciesCmd.class);
        cmdList.add(ListLBHealthCheckPoliciesCmd .class);
        cmdList.add(ListLoadBalancerRuleInstancesCmd.class);
        cmdList.add(ListLoadBalancerRulesCmd.class);
        cmdList.add(RemoveFromLoadBalancerRuleCmd.class);
        cmdList.add(UpdateLoadBalancerRuleCmd.class);
        cmdList.add(CreateIpForwardingRuleCmd.class);
        cmdList.add(DeleteIpForwardingRuleCmd.class);
        cmdList.add(DisableStaticNatCmd.class);
        cmdList.add(EnableStaticNatCmd.class);
        cmdList.add(ListIpForwardingRulesCmd.class);
        cmdList.add(CreateNetworkACLCmd.class);
        cmdList.add(CreateNetworkCmd.class);
        cmdList.add(DeleteNetworkACLCmd.class);
        cmdList.add(DeleteNetworkCmd.class);
        cmdList.add(ListNetworkACLsCmd.class);
        cmdList.add(ListNetworkOfferingsCmd.class);
        cmdList.add(ListNetworksCmd.class);
        cmdList.add(RestartNetworkCmd.class);
        cmdList.add(UpdateNetworkCmd.class);
        cmdList.add(ListDiskOfferingsCmd.class);
        cmdList.add(ListServiceOfferingsCmd.class);
        cmdList.add(ActivateProjectCmd.class);
        cmdList.add(CreateProjectCmd.class);
        cmdList.add(DeleteProjectCmd.class);
        cmdList.add(DeleteProjectInvitationCmd.class);
        cmdList.add(ListProjectInvitationsCmd.class);
        cmdList.add(ListProjectsCmd.class);
        cmdList.add(SuspendProjectCmd.class);
        cmdList.add(UpdateProjectCmd.class);
        cmdList.add(UpdateProjectInvitationCmd.class);
        cmdList.add(ListRegionsCmd.class);
        cmdList.add(GetCloudIdentifierCmd.class);
        cmdList.add(ListHypervisorsCmd.class);
        cmdList.add(ListResourceLimitsCmd.class);
        cmdList.add(UpdateResourceCountCmd.class);
        cmdList.add(UpdateResourceLimitCmd.class);
        cmdList.add(AuthorizeSecurityGroupEgressCmd.class);
        cmdList.add(AuthorizeSecurityGroupIngressCmd.class);
        cmdList.add(CreateSecurityGroupCmd.class);
        cmdList.add(DeleteSecurityGroupCmd.class);
        cmdList.add(ListSecurityGroupsCmd.class);
        cmdList.add(RevokeSecurityGroupEgressCmd.class);
        cmdList.add(RevokeSecurityGroupIngressCmd.class);
        cmdList.add(CreateSnapshotCmd.class);
        cmdList.add(CreateSnapshotPolicyCmd.class);
        cmdList.add(DeleteSnapshotCmd.class);
        cmdList.add(DeleteSnapshotPoliciesCmd.class);
        cmdList.add(ListSnapshotPoliciesCmd.class);
        cmdList.add(ListSnapshotsCmd.class);
        cmdList.add(CreateSSHKeyPairCmd.class);
        cmdList.add(DeleteSSHKeyPairCmd.class);
        cmdList.add(ListSSHKeyPairsCmd.class);
        cmdList.add(RegisterSSHKeyPairCmd.class);
        cmdList.add(CreateTagsCmd.class);
        cmdList.add(DeleteTagsCmd.class);
        cmdList.add(ListTagsCmd.class);
        cmdList.add(CopyTemplateCmd.class);
        cmdList.add(CreateTemplateCmd.class);
        cmdList.add(DeleteTemplateCmd.class);
        cmdList.add(ExtractTemplateCmd.class);
        cmdList.add(ListTemplatePermissionsCmd.class);
        cmdList.add(ListTemplatesCmd.class);
        cmdList.add(RegisterTemplateCmd.class);
        cmdList.add(UpdateTemplateCmd.class);
        cmdList.add(UpdateTemplatePermissionsCmd.class);
        cmdList.add(AddNicToVMCmd.class);
        cmdList.add(DeployVMCmd.class);
        cmdList.add(DestroyVMCmd.class);
        cmdList.add(GetVMPasswordCmd.class);
        cmdList.add(ListVMsCmd.class);
        cmdList.add(RebootVMCmd.class);
        cmdList.add(RemoveNicFromVMCmd.class);
        cmdList.add(ResetVMPasswordCmd.class);
        cmdList.add(ResetVMSSHKeyCmd.class);
        cmdList.add(RestoreVMCmd.class);
        cmdList.add(StartVMCmd.class);
        cmdList.add(StopVMCmd.class);
        cmdList.add(UpdateDefaultNicForVMCmd.class);
        cmdList.add(UpdateVMCmd.class);
        cmdList.add(UpgradeVMCmd.class);
        cmdList.add(CreateVMGroupCmd.class);
        cmdList.add(DeleteVMGroupCmd.class);
        cmdList.add(ListVMGroupsCmd.class);
        cmdList.add(UpdateVMGroupCmd.class);
        cmdList.add(AttachVolumeCmd.class);
        cmdList.add(CreateVolumeCmd.class);
        cmdList.add(DeleteVolumeCmd.class);
        cmdList.add(DetachVolumeCmd.class);
        cmdList.add(ExtractVolumeCmd.class);
        cmdList.add(ListVolumesCmd.class);
        cmdList.add(MigrateVolumeCmd.class);
        cmdList.add(ResizeVolumeCmd.class);
        cmdList.add(UploadVolumeCmd.class);
        cmdList.add(CreateStaticRouteCmd.class);
        cmdList.add(CreateVPCCmd.class);
        cmdList.add(DeleteStaticRouteCmd.class);
        cmdList.add(DeleteVPCCmd.class);
        cmdList.add(ListPrivateGatewaysCmd.class);
        cmdList.add(ListStaticRoutesCmd.class);
        cmdList.add(ListVPCOfferingsCmd.class);
        cmdList.add(ListVPCsCmd.class);
        cmdList.add(RestartVPCCmd.class);
        cmdList.add(UpdateVPCCmd.class);
        cmdList.add(AddVpnUserCmd.class);
        cmdList.add(CreateRemoteAccessVpnCmd.class);
        cmdList.add(CreateVpnConnectionCmd.class);
        cmdList.add(CreateVpnCustomerGatewayCmd.class);
        cmdList.add(CreateVpnGatewayCmd.class);
        cmdList.add(DeleteRemoteAccessVpnCmd.class);
        cmdList.add(DeleteVpnConnectionCmd.class);
        cmdList.add(DeleteVpnCustomerGatewayCmd.class);
        cmdList.add(DeleteVpnGatewayCmd.class);
        cmdList.add(ListRemoteAccessVpnsCmd.class);
        cmdList.add(ListVpnConnectionsCmd.class);
        cmdList.add(ListVpnCustomerGatewaysCmd.class);
        cmdList.add(ListVpnGatewaysCmd.class);
        cmdList.add(ListVpnUsersCmd.class);
        cmdList.add(RemoveVpnUserCmd.class);
        cmdList.add(ResetVpnConnectionCmd.class);
        cmdList.add(UpdateVpnCustomerGatewayCmd.class);
        cmdList.add(ListZonesByCmd.class);
        cmdList.add(ListVMSnapshotCmd.class);
        cmdList.add(CreateVMSnapshotCmd.class);
        cmdList.add(RevertToSnapshotCmd.class);
        cmdList.add(DeleteVMSnapshotCmd.class);
        cmdList.add(AddIpToVmNicCmd.class);
        cmdList.add(RemoveIpFromVmNicCmd.class);
        cmdList.add(ListNicsCmd.class);
        cmdList.add(ArchiveAlertsCmd.class);
        cmdList.add(DeleteAlertsCmd.class);
        cmdList.add(ArchiveEventsCmd.class);
        cmdList.add(DeleteEventsCmd.class);
        return cmdList;
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

    protected class AlertPurgeTask implements Runnable {
        @Override
        public void run() {
            try {
                GlobalLock lock = GlobalLock.getInternLock("AlertPurge");
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
                    purgeCal.add(Calendar.DAY_OF_YEAR, - _alertPurgeDelay);
                    Date purgeTime = purgeCal.getTime();
                    s_logger.debug("Deleting alerts older than: " + purgeTime.toString());
                    List<AlertVO> oldAlerts = _alertDao.listOlderAlerts(purgeTime);
                    s_logger.debug("Found " + oldAlerts.size() + " events to be purged");
                    for (AlertVO alert : oldAlerts) {
                        _alertDao.expunge(alert.getId());
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
    public Pair<List<StoragePoolVO>, Integer> searchForStoragePools(Criteria c) {
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

        return _poolDao.searchAndCount(sc, searchFilter);
    }

    @ActionEvent(eventType = EventTypes.EVENT_SSVM_START, eventDescription = "starting secondary storage Vm", async = true)
    public SecondaryStorageVmVO startSecondaryStorageVm(long instanceId) {
        return _secStorageVmMgr.startSecStorageVm(instanceId);
    }

    @ActionEvent(eventType = EventTypes.EVENT_SSVM_STOP, eventDescription = "stopping secondary storage Vm", async = true)
    private SecondaryStorageVmVO stopSecondaryStorageVm(VMInstanceVO systemVm, boolean isForced) throws ResourceUnavailableException,
    OperationTimedoutException, ConcurrentOperationException {

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
    public Pair<List<? extends VirtualMachine>, Integer> searchForSystemVm(ListSystemVMsCmd cmd) {
        String type = cmd.getSystemVmType();
        Long zoneId = _accountMgr.checkAccessAndSpecifyAuthority(UserContext.current().getCaller(), cmd.getZoneId());
        Long id = cmd.getId();
        String name = cmd.getSystemVmName();
        String state = cmd.getState();
        String keyword = cmd.getKeyword();
        Long podId = cmd.getPodId();
        Long hostId = cmd.getHostId();
        Long storageId = cmd.getStorageId();

        Filter searchFilter = new Filter(VMInstanceVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<VMInstanceVO> sb = _vmInstanceDao.createSearchBuilder();

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("hostName", sb.entity().getHostName(), SearchCriteria.Op.LIKE);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodIdToDeployIn(), SearchCriteria.Op.EQ);
        sb.and("hostId", sb.entity().getHostId(), SearchCriteria.Op.EQ);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
        sb.and("nulltype", sb.entity().getType(), SearchCriteria.Op.IN);

        if (storageId != null) {
            SearchBuilder<VolumeVO> volumeSearch = _volumeDao.createSearchBuilder();
            volumeSearch.and("poolId", volumeSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
            sb.join("volumeSearch", volumeSearch, sb.entity().getId(), volumeSearch.entity().getInstanceId(), JoinBuilder.JoinType.INNER);
        }

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

        if (storageId != null) {
            sc.setJoinParameters("volumeSearch", "poolId", storageId);
        }

        Pair<List<VMInstanceVO>, Integer> result = _vmInstanceDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends VirtualMachine>, Integer>(result.first(), result.second());
    }

    @Override
    public VirtualMachine.Type findSystemVMTypeById(long instanceId) {
        VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(instanceId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find a system vm of specified instanceId");
            ex.addProxyObject(systemVm, instanceId, "instanceId");
            throw ex;
        }
        return systemVm.getType();
    }

    @Override
    public VirtualMachine startSystemVM(long vmId) {

        VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(vmId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a system vm with specified vmId");
            ex.addProxyObject(systemVm, vmId, "vmId");
            throw ex;
        }

        if (systemVm.getType() == VirtualMachine.Type.ConsoleProxy) {
            return startConsoleProxy(vmId);
        } else if (systemVm.getType() == VirtualMachine.Type.SecondaryStorageVm) {
            return startSecondaryStorageVm(vmId);
        } else {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find a system vm with specified vmId");
            ex.addProxyObject(systemVm, vmId, "vmId");
            throw ex;
        }
    }

    @Override
    public VMInstanceVO stopSystemVM(StopSystemVmCmd cmd) throws ResourceUnavailableException, ConcurrentOperationException {
        Long id = cmd.getId();

        // verify parameters
        VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(id, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a system vm with specified vmId");
            ex.addProxyObject(systemVm, id, "vmId");
            throw ex;
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
            InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a system vm with specified vmId");
            ex.addProxyObject(systemVm, cmd.getId(), "vmId");
            throw ex;
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
            InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a system vm with specified vmId");
            ex.addProxyObject(systemVm, cmd.getId(), "vmId");
            throw ex;
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
        User user = _accountMgr.getUserIncludingRemoved(userId);
        if ((user == null) || (user.getRemoved() != null)) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find active user of specified id");
            ex.addProxyObject(user, userId, "userId");
            throw ex;
        }

        // check permissions
        _accountMgr.checkAccess(caller, null, true, _accountMgr.getAccount(user.getAccountId()));

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
    public Map<String, Object> listCapabilities(ListCapabilitiesCmd cmd) {
        Map<String, Object> capabilities = new HashMap<String, Object>();

        boolean securityGroupsEnabled = false;
        boolean elasticLoadBalancerEnabled = false;
        String supportELB = "false";
        List<NetworkVO> networks = _networkDao.listSecurityGroupEnabledNetworks();
        if (networks != null && !networks.isEmpty()) {
            securityGroupsEnabled = true;
            String elbEnabled = _configDao.getValue(Config.ElasticLoadBalancerEnabled.key());
            elasticLoadBalancerEnabled = elbEnabled == null ? false : Boolean.parseBoolean(elbEnabled);
            if (elasticLoadBalancerEnabled) {
                String networkType = _configDao.getValue(Config.ElasticLoadBalancerNetwork.key());
                if (networkType != null)
                    supportELB = networkType;
            }
        }

        long diskOffMaxSize = Long.valueOf(_configDao.getValue(Config.CustomDiskOfferingMaxSize.key()));

        String userPublicTemplateEnabled = _configs.get(Config.AllowPublicUserTemplates.key());

        // add some parameters UI needs to handle API throttling
        boolean apiLimitEnabled = Boolean.parseBoolean(_configDao.getValue(Config.ApiLimitEnabled.key()));
        Integer apiLimitInterval = Integer.valueOf(_configDao.getValue(Config.ApiLimitInterval.key()));
        Integer apiLimitMax = Integer.valueOf(_configDao.getValue(Config.ApiLimitMax.key()));

        capabilities.put("securityGroupsEnabled", securityGroupsEnabled);
        capabilities
        .put("userPublicTemplateEnabled", (userPublicTemplateEnabled == null || userPublicTemplateEnabled.equals("false") ? false : true));
        capabilities.put("cloudStackVersion", getVersion());
        capabilities.put("supportELB", supportELB);
        capabilities.put("projectInviteRequired", _projectMgr.projectInviteRequired());
        capabilities.put("allowusercreateprojects", _projectMgr.allowUserToCreateProject());
        capabilities.put("customDiskOffMaxSize", diskOffMaxSize);
        if (apiLimitEnabled) {
            capabilities.put("apiLimitInterval", apiLimitInterval);
            capabilities.put("apiLimitMax", apiLimitMax);
        }

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
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find volume with specified volumeId");
            ex.addProxyObject(volume, volumeId, "volumeId");
            throw ex;
        }

        // perform permission check
        _accountMgr.checkAccess(account, null, true, volume);

        if (_dcDao.findById(zoneId) == null) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }
        if (volume.getPoolId() == null) {
            throw new InvalidParameterValueException("The volume doesnt belong to a storage pool so cant extract it");
        }
        // Extract activity only for detached volumes or for volumes whose
        // instance is stopped
        if (volume.getInstanceId() != null && ApiDBUtils.findVMInstanceById(volume.getInstanceId()).getState() != State.Stopped) {
            s_logger.debug("Invalid state of the volume with ID: " + volumeId
                    + ". It should be either detached or the VM should be in stopped state.");
            PermissionDeniedException ex = new PermissionDeniedException(
                    "Invalid state of the volume with specified ID. It should be either detached or the VM should be in stopped state.");
            ex.addProxyObject(volume, volumeId, "volumeId");
            throw ex;
        }

        if (volume.getVolumeType() != Volume.Type.DATADISK) { // Datadisk dont
            // have any
            // template
            // dependence.

            VMTemplateVO template = ApiDBUtils.findTemplateById(volume.getTemplateId());
            if (template != null) { // For ISO based volumes template = null and
                // we allow extraction of all ISO based
                // volumes
                boolean isExtractable = template.isExtractable() && template.getTemplateType() != Storage.TemplateType.SYSTEM;
                if (!isExtractable && account != null && account.getType() != Account.ACCOUNT_TYPE_ADMIN) { // Global
                    // admins are always allowed to extract
                    PermissionDeniedException ex = new PermissionDeniedException("The volume with specified volumeId is not allowed to be extracted");
                    ex.addProxyObject(volume, volumeId, "volumeId");
                    throw ex;
                }
            }
        }

        Upload.Mode extractMode;
        if (mode == null || (!mode.equals(Upload.Mode.FTP_UPLOAD.toString()) && !mode.equals(Upload.Mode.HTTP_DOWNLOAD.toString()))) {
            throw new InvalidParameterValueException("Please specify a valid extract Mode ");
        } else {
            extractMode = mode.equals(Upload.Mode.FTP_UPLOAD.toString()) ? Upload.Mode.FTP_UPLOAD : Upload.Mode.HTTP_DOWNLOAD;
        }

        // If mode is upload perform extra checks on url and also see if there
        // is an ongoing upload on the same.
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
                throw new IllegalArgumentException(volume.getName()
                        + " upload is in progress. Please wait for some time to schedule another upload for the same");
            }
        }

        long accountId = volume.getAccountId();
        StoragePool srcPool = (StoragePool)this.dataStoreMgr.getPrimaryDataStore(volume.getPoolId());
        HostVO sserver = this.templateMgr.getSecondaryStorageHost(zoneId);
        String secondaryStorageURL = sserver.getStorageUrl();

        List<UploadVO> extractURLList = _uploadDao.listByTypeUploadStatus(volumeId, Upload.Type.VOLUME, UploadVO.Status.DOWNLOAD_URL_CREATED);

        if (extractMode == Upload.Mode.HTTP_DOWNLOAD && extractURLList.size() > 0) {
            return extractURLList.get(0).getId(); // If download url already
            // exists then return
        } else {
            UploadVO uploadJob = _uploadMonitor.createNewUploadEntry(sserver.getId(), volumeId, UploadVO.Status.COPY_IN_PROGRESS, Upload.Type.VOLUME,
                    url, extractMode);
            s_logger.debug("Extract Mode - " + uploadJob.getMode());
            uploadJob = _uploadDao.createForUpdate(uploadJob.getId());

            // Update the async Job

            ExtractResponse resultObj = new ExtractResponse(ApiDBUtils.findVolumeById(volumeId).getUuid(),
                    volume.getName(), ApiDBUtils.findAccountById(accountId).getUuid(), UploadVO.Status.COPY_IN_PROGRESS.toString(),
                    uploadJob.getUuid());
            resultObj.setResponseName(cmd.getCommandName());
            AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
            if (asyncExecutor != null) {
                job = asyncExecutor.getJob();
                _asyncMgr.updateAsyncJobAttachment(job.getId(), Upload.Type.VOLUME.toString(), volumeId);
                _asyncMgr.updateAsyncJobStatus(job.getId(), AsyncJobResult.STATUS_IN_PROGRESS, resultObj);
            }
            String value = _configs.get(Config.CopyVolumeWait.toString());
            int copyvolumewait = NumbersUtil.parseInt(value, Integer.parseInt(Config.CopyVolumeWait.getDefaultValue()));
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

            if (extractMode == Mode.FTP_UPLOAD) { // Now that the volume is
                // copied perform the actual
                // uploading
                _uploadMonitor.extractVolume(uploadJob, sserver, volume, url, zoneId, volumeLocalPath, cmd.getStartEventId(), job.getId(), _asyncMgr);
                return uploadJob.getId();
            } else { // Volume is copied now make it visible under apache and
                // create a URL.
                _uploadMonitor.createVolumeDownloadURL(volumeId, volumeLocalPath, Upload.Type.VOLUME, zoneId, uploadJob.getId());
                return uploadJob.getId();
            }
        }
    }

    private String getFormatForPool(StoragePool pool) {
        ClusterVO cluster = ApiDBUtils.findClusterById(pool.getClusterId());

        if (cluster.getHypervisorType() == HypervisorType.XenServer) {
            return "vhd";
        } else if (cluster.getHypervisorType() == HypervisorType.KVM) {
            return "qcow2";
        } else if (cluster.getHypervisorType() == HypervisorType.VMware) {
            return "ova";
        } else if (cluster.getHypervisorType() == HypervisorType.Ovm) {
            return "raw";
        } else {
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
            InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a vm group with specified groupId");
            ex.addProxyObject(group, groupId, "groupId");
            throw ex;
        }

        _accountMgr.checkAccess(caller, null, true, group);

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
    public String getVersion() {
        final Class<?> c = ManagementServer.class;
        String fullVersion = c.getPackage().getImplementationVersion();
        if (fullVersion != null && fullVersion.length() > 0) {
            return fullVersion;
        }

        return "unknown";
    }

    @Override
    public Long saveStartedEvent(Long userId, Long accountId, String type, String description, long startEventId) {
        return ActionEventUtils.onStartedActionEvent(userId, accountId, type, description, startEventId);
    }

    @Override
    public Long saveCompletedEvent(Long userId, Long accountId, String level, String type, String description, long startEventId) {
        return ActionEventUtils.onCompletedActionEvent(userId, accountId, level, type, description, startEventId);
    }

    @Override
    @DB
    public String uploadCertificate(UploadCustomCertificateCmd cmd) {
        if (cmd.getPrivateKey() != null && cmd.getAlias() != null) {
            throw new InvalidParameterValueException("Can't change the alias for private key certification");
        }

        if (cmd.getPrivateKey() == null) {
            if (cmd.getAlias() == null) {
                throw new InvalidParameterValueException("alias can't be empty, if it's a certification chain");
            }

            if (cmd.getCertIndex() == null) {
                throw new InvalidParameterValueException("index can't be empty, if it's a certifciation chain");
            }
        }

        if (cmd.getPrivateKey() != null && !_ksMgr.validateCertificate(cmd.getCertificate(), cmd.getPrivateKey(), cmd.getDomainSuffix())) {
            throw new InvalidParameterValueException("Failed to pass certificate validation check");
        }

        if (cmd.getPrivateKey() != null) {
            _ksMgr.saveCertificate(ConsoleProxyManager.CERTIFICATE_NAME, cmd.getCertificate(), cmd.getPrivateKey(), cmd.getDomainSuffix());
        } else {
            _ksMgr.saveCertificate(cmd.getAlias(), cmd.getCertificate(), cmd.getCertIndex(), cmd.getDomainSuffix());
        }

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
        // although we may have race conditioning here, database transaction
        // serialization should
        // give us the same key
        if (_hashKey == null) {
            _hashKey = _configDao.getValueAndInitIfNotExist(Config.HashKey.key(), Config.HashKey.getCategory(), UUID.randomUUID().toString());
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
            InvalidParameterValueException ex = new InvalidParameterValueException("A key pair with name '" + cmd.getName()
                    + "' does not exist for account " + owner.getAccountName() + " in specified domain id");
            ex.addProxyObject(owner, owner.getDomainId(), "domainId");
            throw ex;
        }

        return _sshKeyPairDao.deleteByName(owner.getAccountId(), owner.getDomainId(), cmd.getName());
    }

    @Override
    public Pair<List<? extends SSHKeyPair>, Integer> listSSHKeyPairs(ListSSHKeyPairsCmd cmd) {
        String name = cmd.getName();
        String fingerPrint = cmd.getFingerprint();

        Account caller = UserContext.current().getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, null, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject,
                cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        SearchBuilder<SSHKeyPairVO> sb = _sshKeyPairDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        Filter searchFilter = new Filter(SSHKeyPairVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        SearchCriteria<SSHKeyPairVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.EQ, name);
        }

        if (fingerPrint != null) {
            sc.addAnd("fingerprint", SearchCriteria.Op.EQ, fingerPrint);
        }

        Pair<List<SSHKeyPairVO>, Integer> result = _sshKeyPairDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends SSHKeyPair>, Integer>(result.first(), result.second());
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
            InvalidParameterValueException ex = new InvalidParameterValueException("No VM with specified id found.");
            ex.addProxyObject(vm, cmd.getId(), "vmId");
            throw ex;
        }

        // make permission check
        _accountMgr.checkAccess(caller, null, true, vm);

        _userVmDao.loadDetails(vm);
        String password = vm.getDetail("Encrypted.Password");
        if (password == null || password.equals("")) {
            InvalidParameterValueException ex = new InvalidParameterValueException("No password for VM with specified id found.");
            ex.addProxyObject(vm, cmd.getId(), "vmId");
            throw ex;
        }

        return password;
    }

    @Override
    @DB
    public boolean updateHostPassword(UpdateHostPasswordCmd cmd) {
        if (cmd.getClusterId() == null && cmd.getHostId() == null) {
            throw new InvalidParameterValueException("You should provide one of cluster id or a host id.");
        } else if (cmd.getClusterId() == null) {
            HostVO host = _hostDao.findById(cmd.getHostId());
            if (host != null && host.getHypervisorType() == HypervisorType.XenServer) {
                throw new InvalidParameterValueException("You should provide cluster id for Xenserver cluster.");
            } else {
                throw new InvalidParameterValueException("This operation is not supported for this hypervisor type");
            }
        } else {

            ClusterVO cluster = ApiDBUtils.findClusterById(cmd.getClusterId());
            if (cluster == null || cluster.getHypervisorType() != HypervisorType.XenServer) {
                throw new InvalidParameterValueException("This operation is not supported for this hypervisor type");
            }
            // get all the hosts in this cluster
            List<HostVO> hosts = _resourceMgr.listAllHostsInCluster(cmd.getClusterId());
            Transaction txn = Transaction.currentTxn();
            try {
                txn.start();
                for (HostVO h : hosts) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Changing password for host name = " + h.getName());
                    }
                    // update password for this host
                    DetailVO nv = _detailsDao.findDetail(h.getId(), ApiConstants.USERNAME);
                    if (nv.getValue().equals(cmd.getUsername())) {
                        DetailVO nvp = _detailsDao.findDetail(h.getId(), ApiConstants.PASSWORD);
                        nvp.setValue(DBEncryptionUtil.encrypt(cmd.getPassword()));
                        _detailsDao.persist(nvp);
                    } else {
                        // if one host in the cluster has diff username then
                        // rollback to maintain consistency
                        txn.rollback();
                        throw new InvalidParameterValueException(
                                "The username is not same for all hosts, please modify passwords for individual hosts.");
                    }
                }
                txn.commit();
                // if hypervisor is xenserver then we update it in
                // CitrixResourceBase
            } catch (Exception e) {
                txn.rollback();
                throw new CloudRuntimeException("Failed to update password " + e.getMessage());
            }
        }

        return true;
    }

    @Override
    public String[] listEventTypes() {
        Object eventObj = new EventTypes();
        Class<EventTypes> c = EventTypes.class;
        Field[] fields = c.getDeclaredFields();
        String[] eventTypes = new String[fields.length];
        try {
            int i = 0;
            for (Field field : fields) {
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
    public Pair<List<? extends HypervisorCapabilities>, Integer> listHypervisorCapabilities(Long id, HypervisorType hypervisorType, String keyword,
            Long startIndex, Long pageSizeVal) {
        Filter searchFilter = new Filter(HypervisorCapabilitiesVO.class, "id", true, startIndex, pageSizeVal);
        SearchCriteria<HypervisorCapabilitiesVO> sc = _hypervisorCapabilitiesDao.createSearchCriteria();

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (hypervisorType != null) {
            sc.addAnd("hypervisorType", SearchCriteria.Op.EQ, hypervisorType);
        }

        if (keyword != null) {
            SearchCriteria<HypervisorCapabilitiesVO> ssc = _hypervisorCapabilitiesDao.createSearchCriteria();
            ssc.addOr("hypervisorType", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("hypervisorType", SearchCriteria.Op.SC, ssc);
        }

        Pair<List<HypervisorCapabilitiesVO>, Integer> result = _hypervisorCapabilitiesDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends HypervisorCapabilities>, Integer>(result.first(), result.second());
    }

    @Override
    public HypervisorCapabilities updateHypervisorCapabilities(Long id, Long maxGuestsLimit, Boolean securityGroupEnabled) {
        HypervisorCapabilitiesVO hpvCapabilities = _hypervisorCapabilitiesDao.findById(id, true);

        if (hpvCapabilities == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("unable to find the hypervisor capabilities for specified id");
            ex.addProxyObject(hpvCapabilities, id, "Id");
            throw ex;
        }

        boolean updateNeeded = (maxGuestsLimit != null || securityGroupEnabled != null);
        if (!updateNeeded) {
            return hpvCapabilities;
        }

        hpvCapabilities = _hypervisorCapabilitiesDao.createForUpdate(id);

        if (maxGuestsLimit != null) {
            hpvCapabilities.setMaxGuestsLimit(maxGuestsLimit);
        }

        if (securityGroupEnabled != null) {
            hpvCapabilities.setSecurityGroupEnabled(securityGroupEnabled);
        }

        if (_hypervisorCapabilitiesDao.update(id, hpvCapabilities)) {
            hpvCapabilities = _hypervisorCapabilitiesDao.findById(id);
            UserContext.current().setEventDetails("Hypervisor Capabilities id=" + hpvCapabilities.getId());
            return hpvCapabilities;
        } else {
            return null;
        }
    }

    @Override
    public VirtualMachine upgradeSystemVM(UpgradeSystemVMCmd cmd) {
        Long systemVmId = cmd.getId();
        Long serviceOfferingId = cmd.getServiceOfferingId();
        Account caller = UserContext.current().getCaller();

        VMInstanceVO systemVm = _vmInstanceDao.findByIdTypes(systemVmId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
        if (systemVm == null) {
            throw new InvalidParameterValueException("Unable to find SystemVm with id " + systemVmId);
        }

        _accountMgr.checkAccess(caller, null, true, systemVm);

        // Check that the specified service offering ID is valid
        _itMgr.checkIfCanUpgrade(systemVm, serviceOfferingId);

        boolean result = _itMgr.upgradeVmDb(systemVmId, serviceOfferingId);

        if (result) {
            return _vmInstanceDao.findById(systemVmId);
        } else {
            throw new CloudRuntimeException("Unable to upgrade system vm " + systemVm);
        }

    }

    @Override
    public void enableAdminUser(String password) {
        String encodedPassword = null;

        UserVO adminUser = _userDao.getUser(2);
        if (adminUser.getState() == Account.State.disabled) {
            // This means its a new account, set the password using the
            // authenticator

            for (UserAuthenticator  authenticator: _userAuthenticators) {
                encodedPassword = authenticator.encode(password);
                if (encodedPassword != null) {
                    break;
                }
            }

            adminUser.setPassword(encodedPassword);
            adminUser.setState(Account.State.enabled);
            _userDao.persist(adminUser);
            s_logger.info("Admin user enabled");
        }

    }

}
