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
package com.cloud.storage.secondary;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.SecStorageFirewallCfgCommand;
import com.cloud.agent.api.SecStorageSetupCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.async.AsyncJobExecutor;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.cluster.ClusterManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.info.RunningHostCountInfo;
import com.cloud.info.RunningHostInfoAgregator;
import com.cloud.info.RunningHostInfoAgregator.ZoneHostInfo;
import com.cloud.network.IpAddrAllocator;
import com.cloud.network.Network;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.events.SubscriptionMgr;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.net.NfsUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineGuru;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;

//
// Possible secondary storage vm state transition cases
//		Creating -> Destroyed
//		Creating -> Stopped --> Starting -> Running
//		HA -> Stopped -> Starting -> Running
//		Migrating -> Running	(if previous state is Running before it enters into Migrating state
//		Migrating -> Stopped	(if previous state is not Running before it enters into Migrating state)
//		Running -> HA			(if agent lost connection)
//		Stopped -> Destroyed
//
//		Creating state indicates of record creating and IP address allocation are ready, it is a transient
// 		state which will soon be switching towards Running if everything goes well.
//		Stopped state indicates the readiness of being able to start (has storage and IP resources allocated)
//		Starting state can only be entered from Stopped states
//
// Starting, HA, Migrating, Creating and Running state are all counted as "Open" for available capacity calculation
// because sooner or later, it will be driven into Running state
//
@Local(value = { SecondaryStorageVmManager.class })
public class SecondaryStorageManagerImpl implements SecondaryStorageVmManager, VirtualMachineGuru<SecondaryStorageVmVO> {
    private static final Logger s_logger = Logger.getLogger(SecondaryStorageManagerImpl.class);

    private static final int DEFAULT_CAPACITY_SCAN_INTERVAL = 30000; // 30
                                                                     // seconds
    private static final int EXECUTOR_SHUTDOWN_TIMEOUT = 1000; // 1 second

    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 3; // 3
                                                                              // seconds
    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC = 180; // 3
                                                                         // minutes

    private static final int STARTUP_DELAY = 60000; // 60 seconds

    private String _mgmt_host;
    private int _mgmt_port = 8250;

    private String _name;
    @Inject(adapter = SecondaryStorageVmAllocator.class)
    private Adapters<SecondaryStorageVmAllocator> _ssVmAllocators;

    @Inject
    private SecondaryStorageVmDao _secStorageVmDao;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private VMTemplateDao _templateDao;
    @Inject
    private HostDao _hostDao;
    @Inject
    private StoragePoolHostDao _storagePoolHostDao;

    @Inject
    private VMTemplateHostDao _vmTemplateHostDao;

    @Inject
    private AgentManager _agentMgr;
    @Inject
    private NetworkManager _networkMgr;

    @Inject
    private ClusterManager _clusterMgr;

    private SecondaryStorageListener _listener;

    private ServiceOfferingVO _serviceOffering;

    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private ServiceOfferingDao _offeringDao;
    @Inject
    private AccountService _accountMgr;
    @Inject
    private VirtualMachineManager _itMgr;
    @Inject
    private NicDao _nicDao;
    @Inject
    private NetworkDao _networkDao;

    private IpAddrAllocator _IpAllocator;

    private AsyncJobManager _asyncMgr;

    private final ScheduledExecutorService _capacityScanScheduler = Executors.newScheduledThreadPool(1, new NamedThreadFactory("SS-Scan"));

    private long _capacityScanInterval = DEFAULT_CAPACITY_SCAN_INTERVAL;

    private int _secStorageVmRamSize;

    private String _domain;
    private String _instance;
    private boolean _useLocalStorage;
    private boolean _useSSlCopy;
    private String _allowedInternalSites;

    private final GlobalLock _capacityScanLock = GlobalLock.getInternLock(getCapacityScanLockName());
    private final GlobalLock _allocLock = GlobalLock.getInternLock(getAllocLockName());

    @Override
    public SecondaryStorageVmVO startSecStorageVm(long secStorageVmId) {
        try {
            SecondaryStorageVmVO ssvm = start(secStorageVmId);
            return ssvm;
        } catch (StorageUnavailableException e) {
            s_logger.warn("Exception while trying to start secondary storage vm", e);
            return null;
        } catch (InsufficientCapacityException e) {
            s_logger.warn("Exception while trying to start secondary storage vm", e);
            return null;
        } catch (ConcurrentOperationException e) {
            s_logger.warn("Exception while trying to start secondary storage vm", e);
            return null;
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Exception while trying to start secondary storage vm", e);
            return null;
        } catch (Exception e) {
            s_logger.warn("Exception while trying to start secondary storage vm", e);
            return null;
        }
    }

    @Override
    public SecondaryStorageVmVO start(long secStorageVmId) throws ResourceUnavailableException, InsufficientCapacityException,
            ConcurrentOperationException {
        SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);
        Account systemAcct = _accountMgr.getSystemAccount();
        User systemUser = _accountMgr.getSystemUser();
        return _itMgr.start(secStorageVm, null, systemUser, systemAcct);
    }

    @Override
    public boolean generateFirewallConfiguration(Long hostId) {
        if (hostId == null) {
            return true;
        }
        boolean success = true;
        List<DataCenterVO> allZones = _dcDao.listAll();
        for (DataCenterVO zone : allZones) {
            success = success && generateFirewallConfigurationForZone(zone.getId());
        }
        return true;
    }

    @Override
    public boolean generateSetupCommand(Long zoneId) {

        List<SecondaryStorageVmVO> zoneSsvms = _secStorageVmDao.listByZoneId(zoneId);
        if (zoneSsvms.size() == 0) {
            return true;
        }
        SecondaryStorageVmVO secStorageVm = zoneSsvms.get(0);// FIXME: assumes
                                                             // one vm per zone.
        if (secStorageVm.getState() != State.Running && secStorageVm.getState() != State.Starting) {
            s_logger.warn("No running secondary storage vms found in zone " + zoneId + " , skip programming http auth");
            return true;
        }
        Host storageHost = _hostDao.findSecondaryStorageHost(zoneId);
        if (storageHost == null) {
            s_logger.warn("No storage hosts found in zone " + zoneId + " , skip programming http auth");
            return true;
        }
        SecStorageSetupCommand setupCmd = new SecStorageSetupCommand(zoneId);
        if (_allowedInternalSites != null) {
            List<String> allowedCidrs = new ArrayList<String>();
            String[] cidrs = _allowedInternalSites.split(",");
            for (String cidr : cidrs) {
                if (NetUtils.isValidCIDR(cidr) || NetUtils.isValidIp(cidr)) {
                    allowedCidrs.add(cidr);
                }
            }
            Nic privateNic = _networkMgr.getNicForTraffic(secStorageVm.getId(), TrafficType.Management);
            String privateCidr = NetUtils.ipAndNetMaskToCidr(privateNic.getIp4Address(), privateNic.getNetmask());
            String publicCidr = NetUtils.ipAndNetMaskToCidr(secStorageVm.getPublicIpAddress(), secStorageVm.getPublicNetmask());
            if (NetUtils.isNetworkAWithinNetworkB(privateCidr, publicCidr) || NetUtils.isNetworkAWithinNetworkB(publicCidr, privateCidr)) {
                allowedCidrs.add("0.0.0.0/0");
            }
            setupCmd.setAllowedInternalSites(allowedCidrs.toArray(new String[allowedCidrs.size()]));
        }
        String copyPasswd = _configDao.getValue("secstorage.copy.password");
        setupCmd.setCopyPassword(copyPasswd);
        setupCmd.setCopyUserName(TemplateConstants.DEFAULT_HTTP_AUTH_USER);
        Answer answer = _agentMgr.easySend(storageHost.getId(), setupCmd);
        if (answer != null && answer.getResult()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Successfully programmed http auth into " + secStorageVm.getName());
            }
            return true;
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("failed to program http auth into secondary storage vm : " + secStorageVm.getName());
            }
            return false;
        }
    }

    private boolean generateFirewallConfigurationForZone(Long zoneId) {
        List<SecondaryStorageVmVO> zoneSsvms = _secStorageVmDao.listByZoneId(zoneId);
        if (zoneSsvms.size() == 0) {
            return true;
        }
        SecondaryStorageVmVO secStorageVm = zoneSsvms.get(0);// FIXME: assumes
                                                             // one vm per zone.
        if (secStorageVm.getState() != State.Running && secStorageVm.getState() != State.Starting) {
            s_logger.warn("No running secondary storage vms found in zone " + zoneId + " , skip programming firewall rules");
            return true;
        }
        Host storageHost = _hostDao.findSecondaryStorageHost(zoneId);
        if (storageHost == null) {
            s_logger.warn("No storage hosts found in zone " + zoneId + " , skip programming firewall rules");
            return true;
        }
        List<SecondaryStorageVmVO> alreadyRunning = _secStorageVmDao.getSecStorageVmListInStates(State.Running, State.Migrating, State.Starting);

        String copyPort = Integer.toString(TemplateConstants.DEFAULT_TMPLT_COPY_PORT);
        SecStorageFirewallCfgCommand cpc = new SecStorageFirewallCfgCommand();
        for (SecondaryStorageVmVO ssVm : alreadyRunning) {
            if (ssVm.getPublicIpAddress() != null) {
                if (ssVm.getId() == secStorageVm.getId()) {
                    continue;
                }
                cpc.addPortConfig(ssVm.getPublicIpAddress(), copyPort, true, TemplateConstants.DEFAULT_TMPLT_COPY_INTF);
                if (_useSSlCopy) {
                    cpc.addPortConfig(ssVm.getPublicIpAddress(), "443", true, TemplateConstants.DEFAULT_TMPLT_COPY_INTF);
                }
            }
        }
        Answer answer = _agentMgr.easySend(storageHost.getId(), cpc);
        if (answer != null && answer.getResult()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Successfully programmed firewall rules into " + secStorageVm.getName());
            }
            return true;
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("failed to program firewall rules into secondary storage vm : " + secStorageVm.getName());
            }
            return false;
        }

    }

    public SecondaryStorageVmVO startNew(long dataCenterId) {

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Assign secondary storage vm from a newly started instance for request from data center : " + dataCenterId);
        }

        Map<String, Object> context = createSecStorageVmInstance(dataCenterId);

        long secStorageVmId = (Long) context.get("secStorageVmId");
        if (secStorageVmId == 0) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Creating secondary storage vm instance failed, data center id : " + dataCenterId);
            }

            return null;
        }

        SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);
        // SecondaryStorageVmVO secStorageVm =
        // allocSecStorageVmStorage(dataCenterId, secStorageVmId);
        if (secStorageVm != null) {
            SubscriptionMgr.getInstance().notifySubscribers(ALERT_SUBJECT, this,
                    new SecStorageVmAlertEventArgs(SecStorageVmAlertEventArgs.SSVM_CREATED, dataCenterId, secStorageVmId, secStorageVm, null));
            return secStorageVm;
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to allocate secondary storage vm storage, remove the secondary storage vm record from DB, secondary storage vm id: "
                        + secStorageVmId);
            }

            SubscriptionMgr.getInstance().notifySubscribers(
                    ALERT_SUBJECT,
                    this,
                    new SecStorageVmAlertEventArgs(SecStorageVmAlertEventArgs.SSVM_CREATE_FAILURE, dataCenterId, secStorageVmId, null,
                            "Unable to allocate storage"));
        }
        return null;
    }

    protected Map<String, Object> createSecStorageVmInstance(long dataCenterId) {
        HostVO secHost = _hostDao.findSecondaryStorageHost(dataCenterId);
        if (secHost == null) {
            String msg = "No secondary storage available in zone " + dataCenterId + ", cannot create secondary storage vm";
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }

        long id = _secStorageVmDao.getNextInSequence(Long.class, "id");
        String name = VirtualMachineName.getSystemVmName(id, _instance, "s").intern();
        Account systemAcct = _accountMgr.getSystemAccount();

        DataCenterDeployment plan = new DataCenterDeployment(dataCenterId);
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());

        List<NetworkOfferingVO> defaultOffering = _networkMgr.getSystemAccountNetworkOfferings(NetworkOfferingVO.SystemPublicNetwork);
        if (dc.getNetworkType() == NetworkType.Basic) {
            defaultOffering = _networkMgr.getSystemAccountNetworkOfferings(NetworkOfferingVO.SystemGuestNetwork);
        }

        List<NetworkOfferingVO> offerings = _networkMgr.getSystemAccountNetworkOfferings(NetworkOfferingVO.SystemControlNetwork,
                NetworkOfferingVO.SystemManagementNetwork);
        List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>(offerings.size() + 1);
        NicProfile defaultNic = new NicProfile();
        defaultNic.setDefaultNic(true);
        defaultNic.setDeviceId(2);
        try {
            networks.add(new Pair<NetworkVO, NicProfile>(_networkMgr.setupNetwork(systemAcct, defaultOffering.get(0), plan, null, null, false, false)
                    .get(0), defaultNic));
            for (NetworkOfferingVO offering : offerings) {
                networks.add(new Pair<NetworkVO, NicProfile>(_networkMgr.setupNetwork(systemAcct, offering, plan, null, null, false, false).get(0),
                        null));
            }
        } catch (ConcurrentOperationException e) {
            s_logger.info("Unable to setup due to concurrent operation. " + e);
            return new HashMap<String, Object>();
        }
        
        VMTemplateVO template = _templateDao.findSystemVMTemplate(dataCenterId);
        if (template == null) {
            s_logger.debug("Can't find a template to start");
            throw new CloudRuntimeException("Insufficient capacity exception");
        }
        
        SecondaryStorageVmVO secStorageVm = new SecondaryStorageVmVO(id, _serviceOffering.getId(), name, template.getId(),
                template.getHypervisorType(), template.getGuestOSId(), dataCenterId, systemAcct.getDomainId(), systemAcct.getId());
        try {
            secStorageVm = _itMgr.allocate(secStorageVm, template, _serviceOffering, networks, plan, null, systemAcct);
        } catch (InsufficientCapacityException e) {
            s_logger.warn("InsufficientCapacity", e);
            throw new CloudRuntimeException("Insufficient capacity exception", e);
        }

        Map<String, Object> context = new HashMap<String, Object>();
        context.put("secStorageVmId", secStorageVm.getId());
        return context;
    }

    private SecondaryStorageVmAllocator getCurrentAllocator() {

        // for now, only one adapter is supported
        Enumeration<SecondaryStorageVmAllocator> it = _ssVmAllocators.enumeration();
        if (it.hasMoreElements()) {
            return it.nextElement();
        }

        return null;
    }

    protected String connect(String ipAddress, int port) {
        return null;
    }

    private Runnable getCapacityScanTask() {
        return new Runnable() {

            @Override
            public void run() {
                Transaction txn = Transaction.open(Transaction.CLOUD_DB);
                try {
                    reallyRun();
                } catch (Throwable e) {
                    s_logger.warn("Unexpected exception " + e.getMessage(), e);
                } finally {
                    txn.close();
                }
            }

            private void reallyRun() {
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Begin secondary storage vm capacity scan");
                }

                Map<Long, ZoneHostInfo> zoneHostInfoMap = getZoneHostInfo();
                if (isServiceReady(zoneHostInfoMap)) {
                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("Sec Storage VM Service is ready, check to see if we need to allocate standby capacity");
                    }

                    if (!_capacityScanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("Sec Storage VM Capacity scan lock is used by others, skip and wait for my turn");
                        }
                        return;
                    }

                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("*** Begining secondary storage vm capacity scan... ***");
                    }

                    try {
                        // checkPendingSecStorageVMs();

                        List<DataCenterVO> datacenters = _dcDao.listAllIncludingRemoved();

                        for (DataCenterVO dc : datacenters) {
                            if (isZoneReady(zoneHostInfoMap, dc.getId())) {
                                List<SecondaryStorageVmVO> alreadyRunning = _secStorageVmDao.getSecStorageVmListInStates(dc.getId(), State.Running,
                                        State.Migrating, State.Starting);
                                List<SecondaryStorageVmVO> stopped = _secStorageVmDao.getSecStorageVmListInStates(dc.getId(), State.Stopped,
                                        State.Stopping);
                                if (alreadyRunning.size() == 0) {
                                    if (stopped.size() == 0) {
                                        s_logger.info("No secondary storage vms found in datacenter id=" + dc.getId() + ", starting a new one");
                                        allocCapacity(dc.getId());
                                    } else {
                                        s_logger.warn("Stopped secondary storage vms found in datacenter id=" + dc.getId()
                                                + ", not restarting them automatically");
                                    }

                                }
                            } else {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Zone " + dc.getId() + " is not ready to alloc secondary storage vm");
                                }
                            }
                        }

                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("*** Stop secondary storage vm capacity scan ***");
                        }
                    } finally {
                        _capacityScanLock.unlock();
                    }

                } else {
                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("Secondary storage vm service is not ready for capacity preallocation, wait for next time");
                    }
                }

                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("End of secondary storage vm capacity scan");
                }
            }
        };
    }

    public SecondaryStorageVmVO assignSecStorageVmFromRunningPool(long dataCenterId) {

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Assign  secondary storage vm from running pool for request from data center : " + dataCenterId);
        }

        SecondaryStorageVmAllocator allocator = getCurrentAllocator();
        assert (allocator != null);
        List<SecondaryStorageVmVO> runningList = _secStorageVmDao.getSecStorageVmListInStates(dataCenterId, State.Running);
        if (runningList != null && runningList.size() > 0) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Running secondary storage vm pool size : " + runningList.size());
                for (SecondaryStorageVmVO secStorageVm : runningList) {
                    s_logger.trace("Running secStorageVm instance : " + secStorageVm.getName());
                }
            }

            Map<Long, Integer> loadInfo = new HashMap<Long, Integer>();

            return allocator.allocSecondaryStorageVm(runningList, loadInfo, dataCenterId);
        } else {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Empty running secStorageVm pool for now in data center : " + dataCenterId);
            }
        }
        return null;
    }

    public SecondaryStorageVmVO assignSecStorageVmFromStoppedPool(long dataCenterId) {
        List<SecondaryStorageVmVO> l = _secStorageVmDao.getSecStorageVmListInStates(dataCenterId, State.Starting, State.Stopped, State.Migrating);
        if (l != null && l.size() > 0) {
            return l.get(0);
        }

        return null;
    }

    private void allocCapacity(long dataCenterId) {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Allocate secondary storage vm standby capacity for data center : " + dataCenterId);
        }

        boolean secStorageVmFromStoppedPool = false;
        SecondaryStorageVmVO secStorageVm = assignSecStorageVmFromStoppedPool(dataCenterId);
        if (secStorageVm == null) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("No stopped secondary storage vm is available, need to allocate a new secondary storage vm");
            }

            if (_allocLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
                try {
                    secStorageVm = startNew(dataCenterId);
                } finally {
                    _allocLock.unlock();
                }
            } else {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Unable to acquire synchronization lock to allocate secStorageVm resource for standby capacity, wait for next scan");
                }
                return;
            }
        } else {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Found a stopped secondary storage vm, bring it up to running pool. secStorageVm vm id : " + secStorageVm.getId());
            }
            secStorageVmFromStoppedPool = true;
        }

        if (secStorageVm != null) {
            long secStorageVmId = secStorageVm.getId();
            GlobalLock secStorageVmLock = GlobalLock.getInternLock(getSecStorageVmLockName(secStorageVmId));
            try {
                if (secStorageVmLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
                    try {
                        secStorageVm = startSecStorageVm(secStorageVmId);
                    } finally {
                        secStorageVmLock.unlock();
                    }
                } else {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Unable to acquire synchronization lock to start secStorageVm for standby capacity, secStorageVm vm id : "
                                + secStorageVm.getId());
                    }
                    return;
                }
            } finally {
                secStorageVmLock.releaseRef();
            }

            if (secStorageVm == null) {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Unable to start secondary storage vm for standby capacity, secStorageVm vm Id : " + secStorageVmId
                            + ", will recycle it and start a new one");
                }

                if (secStorageVmFromStoppedPool) {
                    destroySecStorageVm(secStorageVmId);
                }
            } else {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Secondary storage vm " + secStorageVm.getName() + " is started");
                }
            }
        }
    }

    public boolean isServiceReady(Map<Long, ZoneHostInfo> zoneHostInfoMap) {
        for (ZoneHostInfo zoneHostInfo : zoneHostInfoMap.values()) {
            if ((zoneHostInfo.getFlags() & RunningHostInfoAgregator.ZoneHostInfo.ALL_HOST_MASK) != 0) {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Zone " + zoneHostInfo.getDcId() + " is ready to launch");
                }
                return true;
            }
        }

        return false;
    }

    public boolean isZoneReady(Map<Long, ZoneHostInfo> zoneHostInfoMap, long dataCenterId) {
        ZoneHostInfo zoneHostInfo = zoneHostInfoMap.get(dataCenterId);
        if (zoneHostInfo != null && (zoneHostInfo.getFlags() & RunningHostInfoAgregator.ZoneHostInfo.ROUTING_HOST_MASK) != 0) {
            VMTemplateVO template = _templateDao.findSystemVMTemplate(dataCenterId);
            HostVO secHost = _hostDao.findSecondaryStorageHost(dataCenterId);
            if (secHost == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("No secondary storage available in zone " + dataCenterId
                            + ", wait until it is ready to launch secondary storage vm");
                }
                return false;
            }

            boolean templateReady = false;
            if (template != null) {
                VMTemplateHostVO templateHostRef = _vmTemplateHostDao.findByHostTemplate(secHost.getId(), template.getId());
                templateReady = (templateHostRef != null) && (templateHostRef.getDownloadState() == Status.DOWNLOADED);
            }

            if (templateReady) {

                List<Pair<Long, Integer>> l = _storagePoolHostDao.getDatacenterStoragePoolHostInfo(dataCenterId, !_useLocalStorage);
                if (l != null && l.size() > 0 && l.get(0).second().intValue() > 0) {

                    return true;
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Primary storage is not ready, wait until it is ready to launch secondary storage vm");
                    }
                }
            } else {
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Zone host is ready, but secondary storage vm template is not ready");
                }
            }
        }
        return false;
    }

    private synchronized Map<Long, ZoneHostInfo> getZoneHostInfo() {
        Date cutTime = DateUtil.currentGMTTime();
        List<RunningHostCountInfo> l = _hostDao.getRunningHostCounts(new Date(cutTime.getTime() - _clusterMgr.getHeartbeatThreshold()));

        RunningHostInfoAgregator aggregator = new RunningHostInfoAgregator();
        if (l.size() > 0) {
            for (RunningHostCountInfo countInfo : l) {
                aggregator.aggregate(countInfo);
            }
        }

        return aggregator.getZoneHostInfoMap();
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Start secondary storage vm manager");
        }

        return true;
    }

    @Override
    public boolean stop() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Stop secondary storage vm manager");
        }
        _capacityScanScheduler.shutdownNow();

        try {
            _capacityScanScheduler.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }

        _capacityScanLock.releaseRef();
        _allocLock.releaseRef();
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Start configuring secondary storage vm manager : " + name);
        }

        _name = name;

        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        if (configDao == null) {
            throw new ConfigurationException("Unable to get the configuration dao.");
        }

        Map<String, String> configs = configDao.getConfiguration("management-server", params);

        _secStorageVmRamSize = NumbersUtil.parseInt(configs.get("secstorage.vm.ram.size"), DEFAULT_SS_VM_RAMSIZE);
        String useServiceVM = configDao.getValue("secondary.storage.vm");
        boolean _useServiceVM = false;
        if ("true".equalsIgnoreCase(useServiceVM)) {
            _useServiceVM = true;
        }

        String sslcopy = configDao.getValue("secstorage.encrypt.copy");
        if ("true".equalsIgnoreCase(sslcopy)) {
            _useSSlCopy = true;
        }

        _allowedInternalSites = configDao.getValue("secstorage.allowed.internal.sites");

        String value = configs.get("secstorage.capacityscan.interval");
        _capacityScanInterval = NumbersUtil.parseLong(value, DEFAULT_CAPACITY_SCAN_INTERVAL);

        _domain = configs.get("domain");
        if (_domain == null) {
            _domain = "foo.com";
        }

        _instance = configs.get("instance.name");
        if (_instance == null) {
            _instance = "DEFAULT";
        }

        Map<String, String> agentMgrConfigs = configDao.getConfiguration("AgentManager", params);
        _mgmt_host = agentMgrConfigs.get("host");
        if (_mgmt_host == null) {
            s_logger.warn("Critical warning! Please configure your management server host address right after you have started your management server and then restart it, otherwise you won't have access to secondary storage");
        }

        value = agentMgrConfigs.get("port");
        _mgmt_port = NumbersUtil.parseInt(value, 8250);

        _listener = new SecondaryStorageListener(this);
        _agentMgr.registerForHostEvents(_listener, true, true, false);

        HighAvailabilityManager haMgr = locator.getManager(HighAvailabilityManager.class);
        if (haMgr != null) {
            haMgr.registerHandler(VirtualMachine.Type.SecondaryStorageVm, this);
        }

        _itMgr.registerGuru(VirtualMachine.Type.SecondaryStorageVm, this);

        _useLocalStorage = Boolean.parseBoolean(configs.get(Config.SystemVMUseLocalStorage.key()));
        _serviceOffering = new ServiceOfferingVO("System Offering For Secondary Storage VM", 1, _secStorageVmRamSize, 0, 0, 0, true, null,
                Network.GuestIpType.Virtual, _useLocalStorage, true, null, true);
        _serviceOffering.setUniqueName("Cloud.com-SecondaryStorage");
        _serviceOffering = _offeringDao.persistSystemServiceOffering(_serviceOffering);

        if (_useServiceVM) {
            _capacityScanScheduler.scheduleAtFixedRate(getCapacityScanTask(), STARTUP_DELAY, _capacityScanInterval, TimeUnit.MILLISECONDS);
        }
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Secondary storage vm Manager is configured.");
        }
        return true;
    }

    protected SecondaryStorageManagerImpl() {
    }

    @Override
    public Command cleanup(SecondaryStorageVmVO vm, String vmName) {
        if (vmName != null) {
            return new StopCommand(vm, vmName, VirtualMachineName.getVnet(vmName));
        } else if (vm != null) {
            SecondaryStorageVmVO vo = vm;
            return new StopCommand(vo, null);
        } else {
            throw new CloudRuntimeException("Shouldn't even be here!");
        }
    }

    @Override
    public Long convertToId(String vmName) {
        if (!VirtualMachineName.isValidSystemVmName(vmName, _instance, "s")) {
            return null;
        }
        return VirtualMachineName.getSystemVmId(vmName);
    }

    @Override
    public boolean stopSecStorageVm(long secStorageVmId) {

        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if (asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Stop secondary storage vm " + secStorageVmId + ", update async job-" + job.getId());
            }
            _asyncMgr.updateAsyncJobAttachment(job.getId(), "secStorageVm", secStorageVmId);
        }

        SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);
        if (secStorageVm == null) {
            String msg = "Stopping secondary storage vm failed: secondary storage vm " + secStorageVmId + " no longer exists";
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(msg);
            }
            return false;
        }
        try {
            return stop(secStorageVm);
        } catch (ResourceUnavailableException e) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Stopping secondary storage vm " + secStorageVm.getName() + " faled : exception " + e.toString());
            }
            return false;
        }
    }

    @Override
    public boolean rebootSecStorageVm(long secStorageVmId) {
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if (asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Reboot secondary storage vm " + secStorageVmId + ", update async job-" + job.getId());
            }
            _asyncMgr.updateAsyncJobAttachment(job.getId(), "secstorage_vm", secStorageVmId);
        }

        final SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);

        if (secStorageVm == null || secStorageVm.getState() == State.Destroyed) {
            return false;
        }

        if (secStorageVm.getState() == State.Running && secStorageVm.getHostId() != null) {
            final RebootCommand cmd = new RebootCommand(secStorageVm.getInstanceName());
            final Answer answer = _agentMgr.easySend(secStorageVm.getHostId(), cmd);

            if (answer != null && answer.getResult()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Successfully reboot secondary storage vm " + secStorageVm.getName());
                }

                SubscriptionMgr.getInstance().notifySubscribers(
                        ALERT_SUBJECT,
                        this,
                        new SecStorageVmAlertEventArgs(SecStorageVmAlertEventArgs.SSVM_REBOOTED, secStorageVm.getDataCenterId(),
                                secStorageVm.getId(), secStorageVm, null));

                return true;
            } else {
                String msg = "Rebooting Secondary Storage VM failed - " + secStorageVm.getName();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(msg);
                }
                return false;
            }
        } else {
            return startSecStorageVm(secStorageVmId) != null;
        }
    }

    @Override
    public boolean destroySecStorageVm(long vmId) {
        SecondaryStorageVmVO ssvm = _secStorageVmDao.findById(vmId);

        try {
            return _itMgr.expunge(ssvm, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount());
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to expunge " + ssvm, e);
            return false;
        }
    }

    @Override
    public boolean stop(SecondaryStorageVmVO secStorageVm) throws ResourceUnavailableException {
        if (secStorageVm.getHostId() != null) {
            GlobalLock secStorageVmLock = GlobalLock.getInternLock(getSecStorageVmLockName(secStorageVm.getId()));
            try {
                if (secStorageVmLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
                    try {
                        boolean result = _itMgr.stop(secStorageVm, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount());
                        if (result) {
                        }

                        return result;
                    } finally {
                        secStorageVmLock.unlock();
                    }
                } else {
                    String msg = "Unable to acquire secondary storage vm lock : " + secStorageVm.toString();
                    s_logger.debug(msg);
                    return false;
                }
            } finally {
                secStorageVmLock.releaseRef();
            }
        }

        // vm was already stopped, return true
        return true;
    }

    @Override
    public void onAgentConnect(Long dcId, StartupCommand cmd) {
        if (_IpAllocator != null && _IpAllocator.exteralIpAddressAllocatorEnabled()) {
            List<SecondaryStorageVmVO> zoneSsvms = _secStorageVmDao.listByZoneId(dcId);
            if (zoneSsvms.size() == 0) {
                return;
            }
            SecondaryStorageVmVO secStorageVm = zoneSsvms.get(0);// FIXME:
                                                                 // assumes one
                                                                 // vm per zone.
            secStorageVm.setPrivateIpAddress(cmd.getStorageIpAddress()); /*
                                                                          * FIXME:
                                                                          * privateipaddress
                                                                          * is
                                                                          * overwrited
                                                                          * with
                                                                          * address
                                                                          * of
                                                                          * secondary
                                                                          * storage
                                                                          */
            secStorageVm.setPublicIpAddress(cmd.getPublicIpAddress());
            secStorageVm.setPublicNetmask(cmd.getPublicNetmask());
            _secStorageVmDao.persist(secStorageVm);
        }
    }

    private String getCapacityScanLockName() {
        // to improve security, it may be better to return a unique mashed
        // name(for example MD5 hashed)
        return "secStorageVm.capacity.scan";
    }

    private String getAllocLockName() {
        // to improve security, it may be better to return a unique mashed
        // name(for example MD5 hashed)
        return "secStorageVm.alloc";
    }

    private String getSecStorageVmLockName(long id) {
        return "secStorageVm." + id;
    }

    @Override
    public SecondaryStorageVmVO findByName(String name) {
        if (!VirtualMachineName.isValidSecStorageVmName(name, null)) {
            return null;
        }
        return findById(VirtualMachineName.getSystemVmId(name));
    }

    @Override
    public SecondaryStorageVmVO findById(long id) {
        return _secStorageVmDao.findById(id);
    }

    @Override
    public SecondaryStorageVmVO persist(SecondaryStorageVmVO vm) {
        return _secStorageVmDao.persist(vm);
    }

    @Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile<SecondaryStorageVmVO> profile, DeployDestination dest,
            ReservationContext context) {

        HostVO secHost = _hostDao.findSecondaryStorageHost(dest.getDataCenter().getId());
        assert (secHost != null);

        StringBuilder buf = profile.getBootArgsBuilder();
        buf.append(" template=domP type=secstorage");
        buf.append(" host=").append(_mgmt_host);
        buf.append(" port=").append(_mgmt_port);
        buf.append(" name=").append(profile.getVirtualMachine().getName());

        buf.append(" zone=").append(dest.getDataCenter().getId());
        buf.append(" pod=").append(dest.getPod().getId());
        buf.append(" guid=").append(secHost.getGuid());
        String nfsMountPoint = null;
        try {
            nfsMountPoint = NfsUtils.url2Mount(secHost.getStorageUrl());
        } catch (Exception e) {
        }

        buf.append(" mount.path=").append(nfsMountPoint);
        buf.append(" resource=com.cloud.storage.resource.NfsSecondaryStorageResource");
        buf.append(" instance=SecStorage");
        buf.append(" sslcopy=").append(Boolean.toString(_useSSlCopy));

        NicProfile controlNic = null;
        NicProfile managementNic = null;

        boolean externalDhcp = false;
        String externalDhcpStr = _configDao.getValue("direct.attach.network.externalIpAllocator.enabled");
        if (externalDhcpStr != null && externalDhcpStr.equalsIgnoreCase("true")) {
            externalDhcp = true;
        }

        for (NicProfile nic : profile.getNics()) {
            int deviceId = nic.getDeviceId();
            if (nic.getIp4Address() == null) {
                buf.append(" eth").append(deviceId).append("mask=").append("0.0.0.0");
                buf.append(" eth").append(deviceId).append("ip=").append("0.0.0.0");
            } else {
                buf.append(" eth").append(deviceId).append("ip=").append(nic.getIp4Address());
                buf.append(" eth").append(deviceId).append("mask=").append(nic.getNetmask());
            }

            buf.append(" eth").append(deviceId).append("mask=").append(nic.getNetmask());
            if (nic.isDefaultNic()) {
                buf.append(" gateway=").append(nic.getGateway());
            }
            if (nic.getTrafficType() == TrafficType.Management) {
                buf.append(" localgw=").append(dest.getPod().getGateway());
                managementNic = nic;
                buf.append(" private.network.device=").append("eth").append(deviceId);
            } else if (nic.getTrafficType() == TrafficType.Control) {
                if (nic.getIp4Address() != null) {
                    controlNic = nic;
                }
            } else if (nic.getTrafficType() == TrafficType.Public) {
                buf.append(" public.network.device=").append("eth").append(deviceId);
            }
        }

        /* External DHCP mode */
        if (externalDhcp) {
            buf.append(" bootproto=dhcp");
        }

        if (controlNic == null) {
            assert (managementNic != null);
            controlNic = managementNic;
        }

        DataCenterVO dc = _dcDao.findById(profile.getVirtualMachine().getDataCenterId());
        buf.append(" dns1=").append(dc.getInternalDns1());
        if (dc.getInternalDns2() != null) {
            buf.append(" dns2=").append(dc.getInternalDns2());
        }

        String bootArgs = buf.toString();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Boot Args for " + profile + ": " + bootArgs);
        }

        profile.setParameter(VirtualMachineProfile.Param.ControlNic, controlNic);

        return true;
    }

    @Override
    public boolean finalizeDeployment(Commands cmds, VirtualMachineProfile<SecondaryStorageVmVO> profile, DeployDestination dest,
            ReservationContext context) {
        NicProfile controlNic = (NicProfile) profile.getParameter(VirtualMachineProfile.Param.ControlNic);
        CheckSshCommand check = new CheckSshCommand(profile.getInstanceName(), controlNic.getIp4Address(), 3922, 5, 20);
        cmds.addCommand("checkSsh", check);

        SecondaryStorageVmVO secVm = profile.getVirtualMachine();
        DataCenter dc = dest.getDataCenter();
        List<NicVO> nics = _nicDao.listBy(secVm.getId());
        for (NicVO nic : nics) {
            NetworkVO network = _networkDao.findById(nic.getNetworkId());
            if ((network.getTrafficType() == TrafficType.Public && dc.getNetworkType() == NetworkType.Advanced)
                    || (network.getTrafficType() == TrafficType.Guest && dc.getNetworkType() == NetworkType.Basic)) {
                secVm.setPublicIpAddress(nic.getIp4Address());
                secVm.setPublicNetmask(nic.getNetmask());
                secVm.setPublicMacAddress(nic.getMacAddress());
            } else if (network.getTrafficType() == TrafficType.Management) {
                secVm.setPrivateIpAddress(nic.getIp4Address());
                secVm.setPrivateMacAddress(nic.getMacAddress());
            }
        }
        _secStorageVmDao.update(secVm.getId(), secVm);
        return true;
    }

    @Override
    public boolean finalizeStart(VirtualMachineProfile<SecondaryStorageVmVO> profile, long hostId, Commands cmds, ReservationContext context) {
        CheckSshAnswer answer = (CheckSshAnswer) cmds.getAnswer("checkSsh");
        if (!answer.getResult()) {
            s_logger.warn("Unable to ssh to the VM: " + answer.getDetails());
            return false;
        }

        return true;
    }

    @Override
    public void finalizeStop(VirtualMachineProfile<SecondaryStorageVmVO> profile, StopAnswer answer) {
    }
    
    @Override
    public void finalizeExpunge(SecondaryStorageVmVO vm) {
    }
}
