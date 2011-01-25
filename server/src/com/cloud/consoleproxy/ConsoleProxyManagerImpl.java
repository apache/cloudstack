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

package com.cloud.consoleproxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ConsoleAccessAuthenticationAnswer;
import com.cloud.agent.api.ConsoleAccessAuthenticationCommand;
import com.cloud.agent.api.ConsoleProxyLoadReportCommand;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupProxyCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.proxy.ConsoleProxyLoadAnswer;
import com.cloud.agent.api.proxy.UpdateCertificateCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.api.commands.DestroyConsoleProxyCmd;
import com.cloud.async.AsyncJobExecutor;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.certificate.CertificateVO;
import com.cloud.certificate.dao.CertificateDao;
import com.cloud.cluster.ClusterManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.info.ConsoleProxyConnectionInfo;
import com.cloud.info.ConsoleProxyInfo;
import com.cloud.info.ConsoleProxyLoadInfo;
import com.cloud.info.ConsoleProxyStatus;
import com.cloud.info.RunningHostCountInfo;
import com.cloud.info.RunningHostInfoAgregator;
import com.cloud.info.RunningHostInfoAgregator.ZoneHostInfo;
import com.cloud.maid.StackMaid;
import com.cloud.network.IpAddrAllocator;
import com.cloud.network.Network;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.servlet.ConsoleProxyServlet;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.events.SubscriptionMgr;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineGuru;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

//
// Possible console proxy state transition cases
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
@Local(value = { ConsoleProxyManager.class, ConsoleProxyService.class })
public class ConsoleProxyManagerImpl implements ConsoleProxyManager, ConsoleProxyService, Manager, AgentHook, VirtualMachineGuru<ConsoleProxyVO> {
    private static final Logger s_logger = Logger.getLogger(ConsoleProxyManagerImpl.class);

    private static final int DEFAULT_CAPACITY_SCAN_INTERVAL = 30000; // 30
    // seconds
    private static final int EXECUTOR_SHUTDOWN_TIMEOUT = 1000; // 1 second

    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 3; // 3
    // seconds
    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC = 180; // 3
    // minutes

    private static final int API_WAIT_TIMEOUT = 5000; // 5 seconds (in
    // milliseconds)
    private static final int STARTUP_DELAY = 60000; // 60 seconds

    private int _consoleProxyPort = ConsoleProxyManager.DEFAULT_PROXY_VNC_PORT;
    private int _consoleProxyUrlPort = ConsoleProxyManager.DEFAULT_PROXY_URL_PORT;

    private String _mgmt_host;
    private int _mgmt_port = 8250;

    private String _name;
    private Adapters<ConsoleProxyAllocator> _consoleProxyAllocators;

    @Inject
    private ConsoleProxyDao _consoleProxyDao;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private VMTemplateDao _templateDao;
    @Inject
    private VolumeDao _volsDao;
    @Inject
    private HostPodDao _podDao;
    @Inject
    private HostDao _hostDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private CertificateDao _certDao;
    @Inject
    private VMInstanceDao _instanceDao;
    @Inject
    private AccountDao _accountDao;
    @Inject private VMTemplateHostDao _vmTemplateHostDao;
    @Inject private AgentManager _agentMgr;
    @Inject private StorageManager _storageMgr;
    @Inject NetworkManager _networkMgr;
    @Inject AccountManager _accountMgr;
    @Inject private EventDao _eventDao;
    @Inject GuestOSDao _guestOSDao = null;
    @Inject ServiceOfferingDao _offeringDao;
    @Inject NetworkOfferingDao _networkOfferingDao;
    @Inject NicDao _nicDao;
    @Inject NetworkDao _networkDao;
    private IpAddrAllocator _IpAllocator;

    private ConsoleProxyListener _listener;

    private ServiceOfferingVO _serviceOffering;
    private VMTemplateVO _template;
    
    NetworkOfferingVO _publicNetworkOffering;
    NetworkOfferingVO _managementNetworkOffering;
    NetworkOfferingVO _linkLocalNetworkOffering;

    @Inject
    private AsyncJobManager _asyncMgr;
    
    @Inject
    private VirtualMachineManager _itMgr;
    
    private final ScheduledExecutorService _capacityScanScheduler = Executors.newScheduledThreadPool(1, new NamedThreadFactory("CP-Scan"));
    private final ExecutorService _requestHandlerScheduler = Executors.newCachedThreadPool(new NamedThreadFactory("Request-handler"));

    private long _capacityScanInterval = DEFAULT_CAPACITY_SCAN_INTERVAL;
    private int _capacityPerProxy = ConsoleProxyManager.DEFAULT_PROXY_CAPACITY;
    private int _standbyCapacity = ConsoleProxyManager.DEFAULT_STANDBY_CAPACITY;

    private int _proxyRamSize;
    private int _ssh_retry;
    private int _ssh_sleep;
    private boolean _use_lvm;
    private boolean _use_storage_vm;
    private boolean _disable_rp_filter = false;
    private String _domain;
    private String _instance;
    
    // private String _privateNetmask;
    private int _proxyCmdPort = DEFAULT_PROXY_CMD_PORT;
    private int _proxySessionTimeoutValue = DEFAULT_PROXY_SESSION_TIMEOUT;
    private boolean _sslEnabled = false;

    private final GlobalLock _capacityScanLock = GlobalLock.getInternLock(getCapacityScanLockName());
    private final GlobalLock _allocProxyLock = GlobalLock.getInternLock(getAllocProxyLockName());

    @Override
    public ConsoleProxyInfo assignProxy(final long dataCenterId, final long vmId) {

        final Pair<ConsoleProxyManagerImpl, ConsoleProxyVO> result = new Pair<ConsoleProxyManagerImpl, ConsoleProxyVO>(this, null);

        _requestHandlerScheduler.execute(new Runnable() {
            @Override
            public void run() {
                Transaction txn = Transaction.open(Transaction.CLOUD_DB);
                try {
                    ConsoleProxyVO proxy = doAssignProxy(dataCenterId, vmId);
                    synchronized (result) {
                        result.second(proxy);
                        result.notifyAll();
                    }
                } catch (Throwable e) {
                    s_logger.warn("Unexpected exception " + e.getMessage(), e);
                } finally {
                    StackMaid.current().exitCleanup();
                    txn.close();
                }
            }
        });

        synchronized (result) {
            try {
                result.wait(API_WAIT_TIMEOUT);
            } catch (InterruptedException e) {
                s_logger.info("Waiting for console proxy assignment is interrupted");
            }
        }
        
        ConsoleProxyVO proxy = result.second();
        if (proxy == null) {
            return null;
        }
        
        if(proxy.getPublicIpAddress() == null) {
        	s_logger.warn("Assigned console proxy does not have a valid public IP address");
        	return null;
        }
        
        return new ConsoleProxyInfo(proxy.isSslEnabled(), proxy.getPublicIpAddress(), _consoleProxyPort, proxy.getPort(), _configDao.getValue("consoleproxy.url.domain"));
    }

    public ConsoleProxyVO doAssignProxy(long dataCenterId, long vmId) {
        ConsoleProxyVO proxy = null;
        VMInstanceVO vm = _instanceDao.findById(vmId);
        if (vm == null) {
            s_logger.warn("VM " + vmId + " no longer exists, return a null proxy for vm:" + vmId);
            return null;
        }

        Boolean[] proxyFromStoppedPool = new Boolean[1];
        if (_allocProxyLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
            try {
                proxy = getOrAllocProxyResource(dataCenterId, vmId, proxyFromStoppedPool);
            } finally {
                _allocProxyLock.unlock();
            }
        } else {
            s_logger.error("Unable to acquire synchronization lock to get/allocate proxy resource for vm :" + vmId
                    + ". Previous console proxy allocation is taking too long");
        }

        if (proxy == null) {
            s_logger.warn("Unable to find or allocate console proxy resource");
            return null;
        }

        long proxyVmId = proxy.getId();
        GlobalLock proxyLock = GlobalLock.getInternLock(getProxyLockName(proxyVmId));
        try {
            if (proxyLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
                try {
                    proxy = startProxy(proxyVmId);

                    if (proxy == null) {
                        //
                        // We had a situation with multi-pod configuration,
                        // where
                        // storage allocation of the console proxy VM may
                        // succeed, but later-on starting of it
                        // may fail because of running out of computing resource
                        // (CPU/memory). We
                        // currently don't support moving storage to another pod
                        // on the fly, to deal
                        // with the situation we will destroy this proxy VM and
                        // let it the whole proxy VM
                        // creation process re-start again, by hoping that new
                        // storage and computing
                        // resource may be allocated and assigned in another pod
                        //
                        if (s_logger.isInfoEnabled()) {
                            s_logger.info("Unable to start console proxy, proxy vm Id : " + proxyVmId + " will recycle it and restart a new one");
                        }
                        destroyProxy(proxyVmId);
                        return null;
                    } else {
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("Console proxy " + proxy.getName() + " is started");
                        }

                        // if it is a new assignment or a changed assignment,
                        // update the
                        // record
                        if (vm.getProxyId() == null || vm.getProxyId().longValue() != proxy.getId()) {
                            _instanceDao.updateProxyId(vmId, proxy.getId(), DateUtil.currentGMTTime());
                        }

                        proxy.setSslEnabled(_sslEnabled);
                        if (_sslEnabled) {
                            proxy.setPort(443);
                        } else {
                            proxy.setPort(80);
                        }
                        
                     
                        return proxy;
                    }
                } finally {
                    proxyLock.unlock();
                }
            } else {
                s_logger.error("Unable to acquire synchronization lock to start console proxy " + proxyVmId + " for vm: " + vmId
                        + ". It takes too long to start the proxy");

                return null;
            }
        } finally {
            proxyLock.releaseRef();
        }
    }

    private ConsoleProxyVO getOrAllocProxyResource(long dataCenterId, long vmId, Boolean[] proxyFromStoppedPool) {
        ConsoleProxyVO proxy = null;
        VMInstanceVO vm = this._instanceDao.findById(vmId);

        if (vm != null && vm.getState() != State.Running) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Detected that vm : " + vmId + " is not currently at running state, we will fail the proxy assignment for it");
            }
            return null;
        }

        if (vm != null && vm.getProxyId() != null) {
            proxy = _consoleProxyDao.findById(vm.getProxyId());

            if (proxy != null) {
                if (!isInAssignableState(proxy)) {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("A previous assigned proxy is not assignable now, reassign console proxy for user vm : " + vmId);
                    }
                    proxy = null;
                } else {
                    if (_consoleProxyDao.getProxyActiveLoad(proxy.getId()) < _capacityPerProxy || hasPreviousSession(proxy, vm)) {
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("Assign previous allocated console proxy for user vm : " + vmId);
                        }

                        if (proxy.getActiveSession() >= _capacityPerProxy) {
                            s_logger.warn("Assign overloaded proxy to user VM as previous session exists, user vm : " + vmId);
                        }
                    } else {
                        proxy = null;
                    }
                }
            }
        }

        if (proxy == null) {
            proxy = assignProxyFromRunningPool(dataCenterId);
        }

        if (proxy == null) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("No running console proxy is available, check to see if we can bring up a stopped one for data center : "
                        + dataCenterId);
            }

            proxy = assignProxyFromStoppedPool(dataCenterId);
            if (proxy == null) {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("No stopped console proxy is available, need to allocate a new console proxy for data center : " + dataCenterId);
                }

                try {
                    proxy = startNew(dataCenterId);
                } catch (ConcurrentOperationException e) {
                    s_logger.info("Concurrent operation caught " + e);
                    return null;
                }
            } else {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Found a stopped console proxy, bring it up to running pool. proxy vm id : " + proxy.getId() + ", data center : "
                            + dataCenterId);
                }

                proxyFromStoppedPool[0] = new Boolean(true);
            }
        }

        return proxy;
    }

    private static boolean isInAssignableState(ConsoleProxyVO proxy) {
        // console proxies that are in states of being able to serve user VM
        State state = proxy.getState();
        if (state == State.Running || state == State.Starting || state == State.Migrating) {
            return true;
        }

        return false;
    }

    private boolean hasPreviousSession(ConsoleProxyVO proxy, VMInstanceVO vm) {

        ConsoleProxyStatus status = null;
        try {
            GsonBuilder gb = new GsonBuilder();
            gb.setVersion(1.3);
            Gson gson = gb.create();

            byte[] details = proxy.getSessionDetails();
            status = gson.fromJson(details != null ? new String(details, Charset.forName("US-ASCII")) : null, ConsoleProxyStatus.class);
        } catch (Throwable e) {
            s_logger.warn("Unable to parse proxy session details : " + proxy.getSessionDetails());
        }

        if (status != null && status.getConnections() != null) {
            ConsoleProxyConnectionInfo[] connections = status.getConnections();
            for (int i = 0; i < connections.length; i++) {
                long taggedVmId = 0;
                if (connections[i].tag != null) {
                    try {
                        taggedVmId = Long.parseLong(connections[i].tag);
                    } catch (NumberFormatException e) {
                        s_logger.warn("Unable to parse console proxy connection info passed through tag: " + connections[i].tag, e);
                    }
                }
                if (taggedVmId == vm.getId()) {
                    return true;
                }
            }

            //
            // even if we are not in the list, it may because we haven't
            // received load-update yet
            // wait until session time
            //
            if (DateUtil.currentGMTTime().getTime() - vm.getProxyAssignTime().getTime() < _proxySessionTimeoutValue) {
                return true;
            }

            return false;
        } else {
            s_logger.error("No proxy load info on an overloaded proxy ?");
            return false;
        }
    }
    
    @Override
    public ConsoleProxyVO startProxy(long proxyVmId) {
        try {
            return start(proxyVmId);
        } catch (StorageUnavailableException e) {
            s_logger.warn("Exception while trying to start console proxy", e);
            return null;
        } catch (InsufficientCapacityException e) {
            s_logger.warn("Exception while trying to start console proxy", e);
            return null;
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Exception while trying to start console proxy", e);
            return null;
        }
    }

    @Override
    public ConsoleProxyVO start(long proxyVmId) throws ResourceUnavailableException, InsufficientCapacityException {
        ConsoleProxyVO proxy = _consoleProxyDao.findById(proxyVmId);
        Account systemAcct = _accountMgr.getSystemAccount();
        User systemUser = _accountMgr.getSystemUser();
        if (proxy.getState() == VirtualMachine.State.Running) {
            return proxy;
        }
        return _itMgr.start(proxy, null, systemUser, systemAcct);
    }

    public ConsoleProxyVO assignProxyFromRunningPool(long dataCenterId) {

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Assign console proxy from running pool for request from data center : " + dataCenterId);
        }

        ConsoleProxyAllocator allocator = getCurrentAllocator();
        assert (allocator != null);
        List<ConsoleProxyVO> runningList = _consoleProxyDao.getProxyListInStates(dataCenterId, State.Running);
        if (runningList != null && runningList.size() > 0) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Running proxy pool size : " + runningList.size());
                for (ConsoleProxyVO proxy : runningList) {
                    s_logger.trace("Running proxy instance : " + proxy.getName());
                }
            }

            List<Pair<Long, Integer>> l = _consoleProxyDao.getProxyLoadMatrix();
            Map<Long, Integer> loadInfo = new HashMap<Long, Integer>();
            if (l != null) {
                for (Pair<Long, Integer> p : l) {
                    loadInfo.put(p.first(), p.second());

                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("Running proxy instance allocation load { proxy id : " + p.first() + ", load : " + p.second() + "}");
                    }
                }
            }
            return allocator.allocProxy(runningList, loadInfo, dataCenterId);
        } else {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Empty running proxy pool for now in data center : " + dataCenterId);
            }
        }
        return null;
    }

    public ConsoleProxyVO assignProxyFromStoppedPool(long dataCenterId) {
        List<ConsoleProxyVO> l = _consoleProxyDao.getProxyListInStates(dataCenterId, State.Starting, State.Stopped, State.Migrating);
        if (l != null && l.size() > 0) {
            return l.get(0);
        }

        return null;
    }

    public ConsoleProxyVO startNewConsoleProxy(long dataCenterId) throws ConcurrentOperationException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Assign console proxy from a newly started instance for request from data center : " + dataCenterId);
        }

        Map<String, Object> context = createProxyInstance(dataCenterId);

        long proxyVmId = (Long) context.get("proxyVmId");
        if (proxyVmId == 0) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Creating proxy instance failed, data center id : " + dataCenterId);
            }

            return null;
        }

        ConsoleProxyVO proxy = _consoleProxyDao.findById(proxyVmId); 
        if (proxy != null) {
            SubscriptionMgr.getInstance().notifySubscribers(ConsoleProxyManager.ALERT_SUBJECT, this,
                    new ConsoleProxyAlertEventArgs(ConsoleProxyAlertEventArgs.PROXY_CREATED, dataCenterId, proxy.getId(), proxy, null));
            return proxy;
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to allocate console proxy storage, remove the console proxy record from DB, proxy id: " + proxyVmId);
            }

            SubscriptionMgr.getInstance().notifySubscribers(
                    ConsoleProxyManager.ALERT_SUBJECT,
                    this,
                    new ConsoleProxyAlertEventArgs(ConsoleProxyAlertEventArgs.PROXY_CREATE_FAILURE, dataCenterId, proxyVmId, null,
                            "Unable to allocate storage"));
        }
        return null;
    }

    public ConsoleProxyVO startNew(long dataCenterId) throws ConcurrentOperationException {

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Assign console proxy from a newly started instance for request from data center : " + dataCenterId);
        }

        Map<String, Object> context = createProxyInstance(dataCenterId);

        long proxyVmId = (Long) context.get("proxyVmId");
        if (proxyVmId == 0) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Creating proxy instance failed, data center id : " + dataCenterId);
            }
            return null;
        }

        ConsoleProxyVO proxy = _consoleProxyDao.findById(proxyVmId); 
       // allocProxyStorage(dataCenterId, proxyVmId);
        if (proxy != null) {
            SubscriptionMgr.getInstance().notifySubscribers(ConsoleProxyManager.ALERT_SUBJECT, this,
                    new ConsoleProxyAlertEventArgs(ConsoleProxyAlertEventArgs.PROXY_CREATED, dataCenterId, proxy.getId(), proxy, null));
            return proxy;
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to allocate console proxy storage, remove the console proxy record from DB, proxy id: " + proxyVmId);
            }

            SubscriptionMgr.getInstance().notifySubscribers(
                    ConsoleProxyManager.ALERT_SUBJECT,
                    this,
                    new ConsoleProxyAlertEventArgs(ConsoleProxyAlertEventArgs.PROXY_CREATE_FAILURE, dataCenterId, proxyVmId, null,
                            "Unable to allocate storage"));
        }
        return null;
    }

    protected Map<String, Object> createProxyInstance(long dataCenterId) throws ConcurrentOperationException {

        long id = _consoleProxyDao.getNextInSequence(Long.class, "id");
        String name = VirtualMachineName.getConsoleProxyName(id, _instance);
        DataCenterVO dc = _dcDao.findById(dataCenterId);
        Account systemAcct = _accountMgr.getSystemAccount();
        
        DataCenterDeployment plan = new DataCenterDeployment(dataCenterId);

        List<NetworkOfferingVO> defaultOffering = _networkMgr.getSystemAccountNetworkOfferings(NetworkOfferingVO.SystemPublicNetwork);
        if (dc.getNetworkType() == NetworkType.Basic) {
            defaultOffering = _networkMgr.getSystemAccountNetworkOfferings(NetworkOfferingVO.SysteGuestNetwork);
        }
        
        List<NetworkOfferingVO> offerings = _networkMgr.getSystemAccountNetworkOfferings(NetworkOfferingVO.SystemControlNetwork, NetworkOfferingVO.SystemManagementNetwork);
        List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>(offerings.size() + 1);
        NicProfile defaultNic = new NicProfile();
        defaultNic.setDefaultNic(true);
        defaultNic.setDeviceId(2);
        networks.add(new Pair<NetworkVO, NicProfile>(_networkMgr.setupNetwork(systemAcct, defaultOffering.get(0), plan, null, null, false, false).get(0), defaultNic));
        for (NetworkOfferingVO offering : offerings) {
            networks.add(new Pair<NetworkVO, NicProfile>(_networkMgr.setupNetwork(systemAcct, offering, plan, null, null, false, false).get(0), null));
        }
        ConsoleProxyVO proxy = new ConsoleProxyVO(id, _serviceOffering.getId(), name, _template.getId(), _template.getHypervisorType(), _template.getGuestOSId(), dataCenterId, systemAcct.getDomainId(), systemAcct.getId(), 0);
        try {
            proxy = _itMgr.allocate(proxy, _template, _serviceOffering, networks, plan, null, systemAcct);
        } catch (InsufficientCapacityException e) {
            s_logger.warn("InsufficientCapacity", e);
            throw new CloudRuntimeException("Insufficient capacity exception", e);
        }
        
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("dc", dc);
//        context.put("publicIpAddress", publicIpAndVlan._ipAddr);
        HostPodVO pod = _podDao.findById(proxy.getPodId());
        context.put("pod", pod);
        context.put("proxyVmId", proxy.getId());

        return context;
    }
    
    private ConsoleProxyAllocator getCurrentAllocator() {
        // for now, only one adapter is supported
        Enumeration<ConsoleProxyAllocator> it = _consoleProxyAllocators.enumeration();
        if (it.hasMoreElements()) {
            return it.nextElement();
        }

        return null;
    }

    protected String connect(String ipAddress, int port) {
        for (int i = 0; i <= _ssh_retry; i++) {
            SocketChannel sch = null;
            try {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Trying to connect to " + ipAddress);
                }
                sch = SocketChannel.open();
                sch.configureBlocking(true);
                sch.socket().setSoTimeout(5000);

                InetSocketAddress addr = new InetSocketAddress(ipAddress, port);
                sch.connect(addr);
                return null;
            } catch (IOException e) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Could not connect to " + ipAddress);
                }
            } finally {
                if (sch != null) {
                    try {
                        sch.close();
                    } catch (IOException e) {
                    }
                }
            }
            try {
                Thread.sleep(_ssh_sleep);
            } catch (InterruptedException ex) {
            }
        }

        s_logger.debug("Unable to logon to " + ipAddress);

        return "Unable to connect";
    }

    public void onLoadAnswer(ConsoleProxyLoadAnswer answer) {
        if (answer.getDetails() == null) {
            return;
        }

        ConsoleProxyStatus status = null;
        try {
            GsonBuilder gb = new GsonBuilder();
            gb.setVersion(1.3);
            Gson gson = gb.create();
            status = gson.fromJson(answer.getDetails(), ConsoleProxyStatus.class);
        } catch (Throwable e) {
            s_logger.warn("Unable to parse load info from proxy, proxy vm id : " + answer.getProxyVmId() + ", info : " + answer.getDetails());
        }

        if (status != null) {
            int count = 0;
            if (status.getConnections() != null) {
                count = status.getConnections().length;
            }

            byte[] details = null;
            if (answer.getDetails() != null) {
                details = answer.getDetails().getBytes(Charset.forName("US-ASCII"));
            }
            _consoleProxyDao.update(answer.getProxyVmId(), count, DateUtil.currentGMTTime(), details);
        } else {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Unable to get console proxy load info, id : " + answer.getProxyVmId());
            }

            _consoleProxyDao.update(answer.getProxyVmId(), 0, DateUtil.currentGMTTime(), null);
            // TODO : something is wrong with the VM, restart it?
        }
    }

    @Override
    public void onLoadReport(ConsoleProxyLoadReportCommand cmd) {
        if (cmd.getLoadInfo() == null) {
            return;
        }

        ConsoleProxyStatus status = null;
        try {
            GsonBuilder gb = new GsonBuilder();
            gb.setVersion(1.3);
            Gson gson = gb.create();
            status = gson.fromJson(cmd.getLoadInfo(), ConsoleProxyStatus.class);
        } catch (Throwable e) {
            s_logger.warn("Unable to parse load info from proxy, proxy vm id : " + cmd.getProxyVmId() + ", info : " + cmd.getLoadInfo());
        }

        if (status != null) {
            int count = 0;
            if (status.getConnections() != null) {
                count = status.getConnections().length;
            }

            byte[] details = null;
            if (cmd.getLoadInfo() != null) {
                details = cmd.getLoadInfo().getBytes(Charset.forName("US-ASCII"));
            }
            _consoleProxyDao.update(cmd.getProxyVmId(), count, DateUtil.currentGMTTime(), details);
        } else {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Unable to get console proxy load info, id : " + cmd.getProxyVmId());
            }

            _consoleProxyDao.update(cmd.getProxyVmId(), 0, DateUtil.currentGMTTime(), null);
        }
    }

    @Override
    public AgentControlAnswer onConsoleAccessAuthentication(ConsoleAccessAuthenticationCommand cmd) {
        long vmId = 0;
        
		String ticketInUrl = cmd.getTicket();
		if(ticketInUrl == null) {
			s_logger.error("Access ticket could not be found, you could be running an old version of console proxy. vmId: " + cmd.getVmId());
			return new ConsoleAccessAuthenticationAnswer(cmd, false);
		}
		
        String ticket = ConsoleProxyServlet.genAccessTicket(cmd.getHost(), cmd.getPort(), cmd.getSid(), cmd.getVmId());
		if(!ticket.startsWith(ticketInUrl)) {
			Date now = new Date();
			// considering of minute round-up
			String minuteEarlyTicket = ConsoleProxyServlet.genAccessTicket(cmd.getHost(), cmd.getPort(), cmd.getSid(), cmd.getVmId(), 
				new Date(now.getTime() - 60*1000));
			if(!minuteEarlyTicket.startsWith(ticketInUrl)) {
				s_logger.error("Access ticket expired or has been modified. vmId: " + cmd.getVmId() + "ticket in URL: " + ticketInUrl + ", tickets to check against: " + ticket + "," + minuteEarlyTicket);
				return new ConsoleAccessAuthenticationAnswer(cmd, false);
			}
		}

        if (cmd.getVmId() != null && cmd.getVmId().isEmpty()) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Invalid vm id sent from proxy(happens when proxy session has terminated)");
            }
            return new ConsoleAccessAuthenticationAnswer(cmd, false);
        }

        try {
            vmId = Long.parseLong(cmd.getVmId());
        } catch (NumberFormatException e) {
            s_logger.error("Invalid vm id " + cmd.getVmId() + " sent from console access authentication", e);
            return new ConsoleAccessAuthenticationAnswer(cmd, false);
        }

        // TODO authentication channel between console proxy VM and management
        // server needs to be secured,
        // the data is now being sent through private network, but this is
        // apparently not enough
        VMInstanceVO vm = _instanceDao.findById(vmId);
        if (vm == null) {
            return new ConsoleAccessAuthenticationAnswer(cmd, false);
        }

        if (vm.getHostId() == null) {
            s_logger.warn("VM " + vmId + " lost host info, failed authentication request");
            return new ConsoleAccessAuthenticationAnswer(cmd, false);
        }

        HostVO host = _hostDao.findById(vm.getHostId());
        if (host == null) {
            s_logger.warn("VM " + vmId + "'s host does not exist, fail authentication request");
            return new ConsoleAccessAuthenticationAnswer(cmd, false);
        }

        String sid = cmd.getSid();
        if (sid == null || !sid.equals(vm.getVncPassword())) {
            s_logger.warn("sid " + sid + " in url does not match stored sid " + vm.getVncPassword());
            return new ConsoleAccessAuthenticationAnswer(cmd, false);
        }

        return new ConsoleAccessAuthenticationAnswer(cmd, true);
    }

    private ConsoleProxyVO findConsoleProxyByHost(HostVO host) throws NumberFormatException {
        String name = host.getName();
        long proxyVmId = 0;
        ConsoleProxyVO proxy = null;
        if (name != null && name.startsWith("v-")) {
            String[] tokens = name.split("-");
            proxyVmId = Long.parseLong(tokens[1]);
            proxy = this._consoleProxyDao.findById(proxyVmId);
        }
        return proxy;
    }

    @Override
    public void onAgentConnect(HostVO host, StartupCommand cmd) {
        if (host.getType() == Type.ConsoleProxy) {
            // TODO we can use this event to mark the proxy is up and
            // functioning instead of
            // pinging the console proxy VM command port
            //
            // for now, just log a message
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Console proxy agent is connected. proxy: " + host.getName());
            }

            /* update public/private ip address */
            if (_IpAllocator != null && _IpAllocator.exteralIpAddressAllocatorEnabled()) {
                try {
                    ConsoleProxyVO console = findConsoleProxyByHost(host);
                    if (console == null) {
                        s_logger.debug("Can't find console proxy ");
                        return;
                    }
                    console.setPrivateIpAddress(cmd.getPrivateIpAddress());
                    console.setPublicIpAddress(cmd.getPublicIpAddress());
                    console.setPublicNetmask(cmd.getPublicNetmask());
                    _consoleProxyDao.persist(console);
                } catch (NumberFormatException e) {
                }
            }
        }
    }

    @Override
    public void onAgentDisconnect(long agentId, com.cloud.host.Status state) {
        if (state == com.cloud.host.Status.Alert || state == com.cloud.host.Status.Disconnected) {
            // be it either in alert or in disconnected state, the agent process
            // may be gone in the VM,
            // we will be reacting to stop the corresponding VM and let the scan
            // process to
            HostVO host = _hostDao.findById(agentId);
            if (host.getType() == Type.ConsoleProxy) {
                String name = host.getName();
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Console proxy agent disconnected, proxy: " + name);
                }
                if (name != null && name.startsWith("v-")) {
                    String[] tokens = name.split("-");
                    long proxyVmId = 0;
                    try {
                        proxyVmId = Long.parseLong(tokens[1]);
                    } catch (NumberFormatException e) {
                        s_logger.error("Unexpected exception " + e.getMessage(), e);
                        return;
                    }

                    final ConsoleProxyVO proxy = this._consoleProxyDao.findById(proxyVmId);
                    if (proxy != null) {
                        Long hostId = proxy.getHostId();

                        // Disable this feature for now, as it conflicts with
                        // the case of allowing user to reboot console proxy
                        // when rebooting happens, we will receive disconnect
                        // here and we can't enter into stopping process,
                        // as when the rebooted one comes up, it will kick off a
                        // newly started one and trigger the process
                        // continue on forever

                        /*
                         * _capacityScanScheduler.execute(new Runnable() {
                         * public void run() { if(s_logger.isInfoEnabled())
                         * s_logger.info("Stop console proxy " + proxy.getName()
                         * +
                         * " VM because of that the agent running inside it has disconnected"
                         * ); stopProxy(proxy.getId()); } });
                         */
                    } else {
                        if (s_logger.isInfoEnabled()) {
                            s_logger.info("Console proxy agent disconnected but corresponding console proxy VM no longer exists in DB, proxy: "
                                    + name);
                        }
                    }
                } else {
                    assert (false) : "Invalid console proxy name: " + name;
                }
            }
        }
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
                    StackMaid.current().exitCleanup();
                    txn.close();
                }
            }

            private void reallyRun() {
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Begin console proxy capacity scan");
                }

                // config var for consoleproxy.restart check
                String restart = _configDao.getValue("consoleproxy.restart");
                if (restart != null && restart.equalsIgnoreCase("false")) {
                    s_logger.debug("Capacity scan disabled purposefully, consoleproxy.restart = false. This happens when the primarystorage is in maintenance mode");
                    return;
                }

                Map<Long, ZoneHostInfo> zoneHostInfoMap = getZoneHostInfo();
                if (isServiceReady(zoneHostInfoMap)) {
                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("Service is ready, check to see if we need to allocate standby capacity");
                    }

                    if (!_capacityScanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("Capacity scan lock is used by others, skip and wait for my turn");
                        }
                        return;
                    }

                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("*** Begining capacity scan... ***");
                    }

                    try {
                       // checkPendingProxyVMs();

                        // scan default data center first
                        long defaultId = 0;

                        // proxy count info by data-centers (zone-id, zone-name,
                        // count)
                        List<ConsoleProxyLoadInfo> l = _consoleProxyDao.getDatacenterProxyLoadMatrix();

                        // running VM session count by data-centers (zone-id,
                        // zone-name, count)
                        List<ConsoleProxyLoadInfo> listVmCounts = _consoleProxyDao.getDatacenterSessionLoadMatrix();

                        // indexing load info by data-center id
                        Map<Long, ConsoleProxyLoadInfo> mapVmCounts = new HashMap<Long, ConsoleProxyLoadInfo>();
                        if (listVmCounts != null) {
                            for (ConsoleProxyLoadInfo info : listVmCounts) {
                                mapVmCounts.put(info.getId(), info);
                            }
                        }

                        for (ConsoleProxyLoadInfo info : l) {
                            if (info.getName().equals(_instance)) {
                                ConsoleProxyLoadInfo vmInfo = mapVmCounts.get(info.getId());

                                if (!checkCapacity(info, vmInfo != null ? vmInfo : new ConsoleProxyLoadInfo())) {
                                    if (isZoneReady(zoneHostInfoMap, info.getId())) {
                                        allocCapacity(info.getId());
                                    } else {
                                        if (s_logger.isTraceEnabled()) {
                                            s_logger.trace("Zone " + info.getId() + " is not ready to alloc standy console proxy");
                                        }
                                    }
                                }

                                defaultId = info.getId();
                                break;
                            }
                        }

                        // scan rest of data-centers
                        for (ConsoleProxyLoadInfo info : l) {
                            if (info.getId() != defaultId) {
                                ConsoleProxyLoadInfo vmInfo = mapVmCounts.get(info.getId());

                                if (!checkCapacity(info, vmInfo != null ? vmInfo : new ConsoleProxyLoadInfo())) {
                                    if (isZoneReady(zoneHostInfoMap, info.getId())) {
                                        allocCapacity(info.getId());
                                    } else {
                                        if (s_logger.isTraceEnabled()) {
                                            s_logger.trace("Zone " + info.getId() + " is not ready to alloc standy console proxy");
                                        }
                                    }
                                }
                            }
                        }

                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("*** Stop capacity scan ***");
                        }
                    } finally {
                        _capacityScanLock.unlock();
                    }

                } else {
                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("Service is not ready for capacity preallocation, wait for next time");
                    }
                }

                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("End of console proxy capacity scan");
                }
            }
        };
    }

    private boolean checkCapacity(ConsoleProxyLoadInfo proxyCountInfo, ConsoleProxyLoadInfo vmCountInfo) {

        if (proxyCountInfo.getCount() * _capacityPerProxy - vmCountInfo.getCount() <= _standbyCapacity) {
            return false;
        }

        return true;
    }

    private void allocCapacity(long dataCenterId) {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Allocate console proxy standby capacity for data center : " + dataCenterId);
        }

        boolean proxyFromStoppedPool = false;
        ConsoleProxyVO proxy = assignProxyFromStoppedPool(dataCenterId);
        if (proxy == null) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("No stopped console proxy is available, need to allocate a new console proxy");
            }

            if (_allocProxyLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
                try {
                    proxy = startNew(dataCenterId);
                }  catch (ConcurrentOperationException e) {
                    s_logger.info("Concurrent Operation caught " + e)
                    ;
                } finally {
                    _allocProxyLock.unlock();
                }
            } else {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Unable to acquire synchronization lock to allocate proxy resource for standby capacity, wait for next scan");
                }
                return;
            }
        } else {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Found a stopped console proxy, bring it up to running pool. proxy vm id : " + proxy.getId());
            }
            proxyFromStoppedPool = true;
        }

        if (proxy != null) {
            long proxyVmId = proxy.getId();
            GlobalLock proxyLock = GlobalLock.getInternLock(getProxyLockName(proxyVmId));
            try {
                if (proxyLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
                    try {
                        proxy = startProxy(proxyVmId);
                    } finally {
                        proxyLock.unlock();
                    }
                } else {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Unable to acquire synchronization lock to start proxy for standby capacity, proxy vm id : " + proxy.getId());
                    }
                    return;
                }
            } finally {
                proxyLock.releaseRef();
            }

            if (proxy == null) {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Unable to start console proxy for standby capacity, proxy vm Id : " + proxyVmId
                            + ", will recycle it and start a new one");
                }
                
                if (proxyFromStoppedPool) {
                    destroyProxy(proxyVmId);
                }
            } else {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Console proxy " + proxy.getName() + " is started");
                }
            }
        }
    }

    public boolean isServiceReady(Map<Long, ZoneHostInfo> zoneHostInfoMap) {
        for (ZoneHostInfo zoneHostInfo : zoneHostInfoMap.values()) {
            if (isZoneHostReady(zoneHostInfo)) {
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
        if (zoneHostInfo != null && isZoneHostReady(zoneHostInfo)) {
            VMTemplateVO template = _templateDao.findConsoleProxyTemplate();
            HostVO secondaryStorageHost = _storageMgr.getSecondaryStorageHost(dataCenterId);
            boolean templateReady = false;

            if (template != null && secondaryStorageHost != null) {
                VMTemplateHostVO templateHostRef = _vmTemplateHostDao.findByHostTemplate(secondaryStorageHost.getId(), template.getId());
                templateReady = (templateHostRef != null) && (templateHostRef.getDownloadState() == Status.DOWNLOADED);
            }

            if (templateReady) {
                List<Pair<Long, Integer>> l = _consoleProxyDao.getDatacenterStoragePoolHostInfo(dataCenterId, _use_lvm);
                if (l != null && l.size() > 0 && l.get(0).second().intValue() > 0) {
                    return true;
                } else {
                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("Primary storage is not ready, wait until it is ready to launch console proxy");
                    }
                }
            } else {
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Zone host is ready, but console proxy template is not ready");
                }
            }
        }
        return false;
    }

    private boolean isZoneHostReady(ZoneHostInfo zoneHostInfo) {
        int expectedFlags = 0;
        if (_use_storage_vm) {
            expectedFlags = RunningHostInfoAgregator.ZoneHostInfo.ROUTING_HOST_MASK;
        } else {
            expectedFlags = RunningHostInfoAgregator.ZoneHostInfo.ALL_HOST_MASK;
        }

        return (zoneHostInfo.getFlags() & expectedFlags) == expectedFlags;
    }

    private synchronized Map<Long, ZoneHostInfo> getZoneHostInfo() {
        Date cutTime = DateUtil.currentGMTTime();
        List<RunningHostCountInfo> l = _hostDao.getRunningHostCounts(new Date(cutTime.getTime() - ClusterManager.DEFAULT_HEARTBEAT_THRESHOLD));

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
            s_logger.info("Start console proxy manager");
        }

        return true;
    }

    @Override
    public boolean stop() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Stop console proxy manager");
        }
        _capacityScanScheduler.shutdownNow();

        try {
            _capacityScanScheduler.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }

        _capacityScanLock.releaseRef();
        _allocProxyLock.releaseRef();
        return true;
    }

    @Override
    public Command cleanup(ConsoleProxyVO vm, String vmName) {
        if (vmName != null) {
            return new StopCommand(vm, vmName, VirtualMachineName.getVnet(vmName));
        } else if (vm != null) {
            ConsoleProxyVO vo = vm;
            return new StopCommand(vo, null);
        } else {
            throw new CloudRuntimeException("Shouldn't even be here!");
        }
    }

    @Override
    public Long convertToId(String vmName) {
        if (!VirtualMachineName.isValidConsoleProxyName(vmName, _instance)) {
            return null;
        }
        return VirtualMachineName.getConsoleProxyId(vmName);
    }

    @Override
    public boolean stopProxy(long proxyVmId) {

        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if (asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Stop console proxy " + proxyVmId + ", update async job-" + job.getId());
            }
            _asyncMgr.updateAsyncJobAttachment(job.getId(), "console_proxy", proxyVmId);
        }

        ConsoleProxyVO proxy = _consoleProxyDao.findById(proxyVmId);
        if (proxy == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Stopping console proxy failed: console proxy " + proxyVmId + " no longer exists");
            }
            return false;
        }
        /*
         * saveStartedEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM,
         * EventTypes.EVENT_PROXY_STOP, "Stopping console proxy with Id: " +
         * proxyVmId, startEventId);
         */
        try {
            return stop(proxy);
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Stopping console proxy " + proxy.getName() + " failed : exception " + e.toString());
            return false;
        }
    }

    @Override
    public boolean rebootProxy(long proxyVmId) {
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if (asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Reboot console proxy " + proxyVmId + ", update async job-" + job.getId());
            }
            _asyncMgr.updateAsyncJobAttachment(job.getId(), "console_proxy", proxyVmId);
        }

        final ConsoleProxyVO proxy = _consoleProxyDao.findById(proxyVmId);

        if (proxy == null || proxy.getState() == State.Destroyed) {
            return false;
        }

        if (proxy.getState() == State.Running && proxy.getHostId() != null) {
            final RebootCommand cmd = new RebootCommand(proxy.getInstanceName());
            final Answer answer = _agentMgr.easySend(proxy.getHostId(), cmd);

            if (answer != null && answer.getResult()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Successfully reboot console proxy " + proxy.getName());
                }

                SubscriptionMgr.getInstance()
                        .notifySubscribers(
                                ConsoleProxyManager.ALERT_SUBJECT,
                                this,
                                new ConsoleProxyAlertEventArgs(ConsoleProxyAlertEventArgs.PROXY_REBOOTED, proxy.getDataCenterId(), proxy.getId(),
                                        proxy, null));

                return true;
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("failed to reboot console proxy : " + proxy.getName());
                }

                return false;
            }
        } else {
            return startProxy(proxyVmId) != null;
        }
    }

    @Override
    public boolean destroyProxy(long vmId) {
        ConsoleProxyVO proxy = _consoleProxyDao.findById(vmId);
        try {
            return _itMgr.expunge(proxy, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount());
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to expunge " + proxy, e);
            return false;
        }
    }

    @Override
    public boolean stop(ConsoleProxyVO proxy) throws ResourceUnavailableException {
        GlobalLock proxyLock = GlobalLock.getInternLock(getProxyLockName(proxy.getId()));
        try {
            if (proxyLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
                try {
                    boolean result = _itMgr.stop(proxy, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount());    
                    return result;
                } finally {
                    proxyLock.unlock();
                }
            } else {
                s_logger.debug("Unable to acquire console proxy lock : " + proxy.toString());
                return false;
            }
        } finally {
            proxyLock.releaseRef();
        }
    }

    private String getCapacityScanLockName() {
        // to improve security, it may be better to return a unique mashed
        // name(for example MD5 hashed)
        return "consoleproxy.capacity.scan";
    }

    private String getAllocProxyLockName() {
        // to improve security, it may be better to return a unique mashed
        // name(for example MD5 hashed)
        return "consoleproxy.alloc";
    }

    private String getProxyLockName(long id) {
        return "consoleproxy." + id;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Start configuring console proxy manager : " + name);
        }

        _name = name;

        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        if (configDao == null) {
            throw new ConfigurationException("Unable to get the configuration dao.");
        }

        Map<String, String> configs = configDao.getConfiguration("management-server", params);

        _proxyRamSize = NumbersUtil.parseInt(configs.get("consoleproxy.ram.size"), DEFAULT_PROXY_VM_RAMSIZE);

        String value = configs.get("consoleproxy.cmd.port");
        _proxyCmdPort = NumbersUtil.parseInt(value, DEFAULT_PROXY_CMD_PORT);

        value = configs.get("consoleproxy.sslEnabled");
        if (value != null && value.equalsIgnoreCase("true")) {
            _sslEnabled = true;
        }

        value = configs.get("consoleproxy.capacityscan.interval");
        _capacityScanInterval = NumbersUtil.parseLong(value, DEFAULT_CAPACITY_SCAN_INTERVAL);

        _capacityPerProxy = NumbersUtil.parseInt(configs.get("consoleproxy.session.max"), DEFAULT_PROXY_CAPACITY);
        _standbyCapacity = NumbersUtil.parseInt(configs.get("consoleproxy.capacity.standby"), DEFAULT_STANDBY_CAPACITY);
        _proxySessionTimeoutValue = NumbersUtil.parseInt(configs.get("consoleproxy.session.timeout"), DEFAULT_PROXY_SESSION_TIMEOUT);

        value = configs.get("consoleproxy.port");
        if (value != null) {
            _consoleProxyPort = NumbersUtil.parseInt(value, ConsoleProxyManager.DEFAULT_PROXY_VNC_PORT);
        }

        value = configs.get("consoleproxy.url.port");
        if (value != null) {
            _consoleProxyUrlPort = NumbersUtil.parseInt(value, ConsoleProxyManager.DEFAULT_PROXY_URL_PORT);
        }
        
        value = configs.get(Config.ConsoleProxyDisableRpFilter.key());
        if(value != null && value.equalsIgnoreCase("true")) {
            _disable_rp_filter = true;
        }

        value = configs.get("system.vm.use.local.storage");
        if (value != null && value.equalsIgnoreCase("true")) {
            _use_lvm = true;
        }

        value = configs.get("secondary.storage.vm");
        if (value != null && value.equalsIgnoreCase("true")) {
            _use_storage_vm = true;
        }
        
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Console proxy max session soft limit : " + _capacityPerProxy);
            s_logger.info("Console proxy standby capacity : " + _standbyCapacity);
        }

        _domain = configs.get("domain");
        if (_domain == null) {
            _domain = "foo.com";
        }

        _instance = configs.get("instance.name");
        if (_instance == null) {
            _instance = "DEFAULT";
        }

        value = (String) params.get("ssh.sleep");
        _ssh_sleep = NumbersUtil.parseInt(value, 5) * 1000;

        value = (String) params.get("ssh.retry");
        _ssh_retry = NumbersUtil.parseInt(value, 3);

        Map<String, String> agentMgrConfigs = configDao.getConfiguration("AgentManager", params);
        _mgmt_host = agentMgrConfigs.get("host");
        if (_mgmt_host == null) {
            s_logger.warn("Critical warning! Please configure your management server host address right after you have started your management server and then restart it, otherwise you won't be able to do console access");
        }

        value = agentMgrConfigs.get("port");
        _mgmt_port = NumbersUtil.parseInt(value, 8250);

        _consoleProxyAllocators = locator.getAdapters(ConsoleProxyAllocator.class);
        if (_consoleProxyAllocators == null || !_consoleProxyAllocators.isSet()) {
            throw new ConfigurationException("Unable to get proxy allocators");
        }

        _listener = new ConsoleProxyListener(this);
        _agentMgr.registerForHostEvents(_listener, true, true, false);

        HighAvailabilityManager haMgr = locator.getManager(HighAvailabilityManager.class);
        if (haMgr != null) {
            haMgr.registerHandler(VirtualMachine.Type.ConsoleProxy, this);
        }
        _itMgr.registerGuru(VirtualMachine.Type.ConsoleProxy, this);

        boolean useLocalStorage = Boolean.parseBoolean(configs.get(Config.SystemVMUseLocalStorage.key()));
        _serviceOffering = new ServiceOfferingVO("System Offering For Console Proxy", 1, _proxyRamSize, 0, 0, 0, true, null, Network.GuestIpType.Virtual,
                useLocalStorage, true, null, true);
        _serviceOffering.setUniqueName("Cloud.com-ConsoleProxy");
        _serviceOffering = _offeringDao.persistSystemServiceOffering(_serviceOffering);
        _template = _templateDao.findConsoleProxyTemplate();
        if (_template == null) {
            throw new ConfigurationException("Unable to find the template for console proxy VMs");
        }
        
        _capacityScanScheduler.scheduleAtFixedRate(getCapacityScanTask(), STARTUP_DELAY, _capacityScanInterval, TimeUnit.MILLISECONDS);
        
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Console Proxy Manager is configured.");
        }
        return true;
    }

    @Override
    public boolean destroyConsoleProxy(DestroyConsoleProxyCmd cmd) throws ServerApiException{
        Long proxyId = cmd.getId();
        
        // verify parameters
        ConsoleProxyVO proxy = _consoleProxyDao.findById(proxyId);
        if (proxy == null) {
            throw new ServerApiException (BaseCmd.PARAM_ERROR, "unable to find a console proxy with id " + proxyId);
        }
        
        return destroyProxy(proxyId);
        
    }

    protected ConsoleProxyManagerImpl() {
    }

    @Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile<ConsoleProxyVO> profile, DeployDestination dest, ReservationContext context) {
    	StringBuilder buf = profile.getBootArgsBuilder();
        buf.append(" template=domP type=consoleproxy");
        buf.append(" host=").append(_mgmt_host);
        buf.append(" port=").append(_mgmt_port);
        buf.append(" name=").append(profile.getVirtualMachine().getName());
        if (_sslEnabled) {
            buf.append(" premium=true");
        }
        buf.append(" zone=").append(dest.getDataCenter().getId());
        buf.append(" pod=").append(dest.getPod().getId());
        buf.append(" guid=Proxy.").append(profile.getId());
        buf.append(" proxy_vm=").append(profile.getId());
        if(_disable_rp_filter) {
            buf.append(" disable_rp_filter=true");
        }

        boolean externalDhcp = false;
        String externalDhcpStr = _configDao.getValue("direct.attach.network.externalIpAllocator.enabled");
        if(externalDhcpStr != null && externalDhcpStr.equalsIgnoreCase("true")) {
            externalDhcp = true;
        }
        
        NicProfile controlNic = null;
        NicProfile managementNic = null;
        for (NicProfile nic : profile.getNics()) {
            int deviceId = nic.getDeviceId();
            if(nic.getIp4Address() == null) {
	            buf.append(" eth").append(deviceId).append("ip=").append("0.0.0.0");
	            buf.append(" eth").append(deviceId).append("mask=").append("0.0.0.0");
            } else {
	            buf.append(" eth").append(deviceId).append("ip=").append(nic.getIp4Address());
	            buf.append(" eth").append(deviceId).append("mask=").append(nic.getNetmask());
            }
            
            if (nic.isDefaultNic()) {
                buf.append(" gateway=").append(nic.getGateway());
                buf.append(" dns1=").append(nic.getDns1());
                if (nic.getDns2() != null) {
                    buf.append(" dns2=").append(nic.getDns2());
                }
            }
            
            if (nic.getTrafficType() == TrafficType.Management) {
                buf.append(" localgw=").append(dest.getPod().getGateway());
                managementNic = nic;
            } else if (nic.getTrafficType() == TrafficType.Control) {
                if(nic.getIp4Address() != null) {
                    controlNic = nic;
                }
            }
        }

		/*External DHCP mode*/
		if(externalDhcp) {
            buf.append(" bootproto=dhcp");
        }
        
        if(controlNic == null) {
        	assert(managementNic != null);
        	controlNic = managementNic;
        }
        
        String bootArgs = buf.toString();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Boot Args for " + profile + ": " + bootArgs);
        }
        
        profile.setParameter("control.nic", controlNic);
        
        return true;
    }

    @Override
    public boolean finalizeDeployment(Commands cmds, VirtualMachineProfile<ConsoleProxyVO> profile, DeployDestination dest, ReservationContext context) {
    	NicProfile controlNic = (NicProfile)profile.getParameter("control.nic");
        CheckSshCommand check = new CheckSshCommand(profile.getInstanceName(), controlNic.getIp4Address(), 3922, 5, 20);
        cmds.addCommand("checkSsh", check);
        
        ConsoleProxyVO proxy = profile.getVirtualMachine();
        DataCenter dc = dest.getDataCenter();
        List<NicVO> nics = _nicDao.listBy(proxy.getId());
        for (NicVO nic : nics) {
        	NetworkVO network = _networkDao.findById(nic.getNetworkId());
        	if ((network.getTrafficType() == TrafficType.Public && dc.getNetworkType() == NetworkType.Advanced) || (network.getTrafficType() == TrafficType.Guest && dc.getNetworkType() == NetworkType.Basic)) {
        		proxy.setPublicIpAddress(nic.getIp4Address());
        		proxy.setPublicNetmask(nic.getNetmask());
        		proxy.setPublicMacAddress(nic.getMacAddress());
        	} else if (network.getTrafficType() == TrafficType.Management) {
        		proxy.setPrivateIpAddress(nic.getIp4Address());
        		proxy.setPrivateMacAddress(nic.getMacAddress());
        	}
        }
        _consoleProxyDao.update(proxy.getId(), proxy);
        return true;
    }
    
    @Override
    public boolean finalizeStart(VirtualMachineProfile<ConsoleProxyVO> profile, long hostId, Commands cmds, ReservationContext context) {
        CheckSshAnswer answer = (CheckSshAnswer)cmds.getAnswer("checkSsh");
        if (!answer.getResult()) {
            s_logger.warn("Unable to ssh to the VM: " + answer.getDetails());
            return false;
        }
        
        return true;
    }
    
    @Override
    public boolean applyCustomCertToNewProxy(StartupProxyCommand cmd){
        //this is the case for updating cust cert on each new starting proxy, if such cert exists
		//get cert from db
		CertificateVO cert = _certDao.listAll().get(0);
		
		if(cert.getUpdated().equalsIgnoreCase("Y")){
			String certStr = cert.getCertificate(); 
			long proxyVmId = (cmd).getProxyVmId();
			ConsoleProxyVO consoleProxy = _consoleProxyDao.findById(proxyVmId);
			//find corresponding host
			if(consoleProxy!=null){
				HostVO consoleProxyHost = _hostDao.findConsoleProxyHost(consoleProxy.getName(), Type.ConsoleProxy);
				//now send a command to console proxy host
	    		UpdateCertificateCommand certCmd = new UpdateCertificateCommand(certStr, true);
	    		try {
						Answer updateCertAns = _agentMgr.send(consoleProxyHost.getId(), certCmd);
						if(updateCertAns.getResult() == true)
						{
							//we have the cert copied over on cpvm
							rebootProxy(consoleProxy.getId());
							//when cp reboots, the context will be reinit with the new cert 
							s_logger.info("Successfully rebooted console proxy resource after custom certificate application for proxy:"+cmd.getProxyVmId());
							return true;
						}
				} catch (AgentUnavailableException e) {
					s_logger.warn("Unable to send update certificate command to the console proxy resource for proxy:"+cmd.getProxyVmId(), e);
					return false;
				} catch (OperationTimedoutException e) {
					s_logger.warn("Unable to send update certificate command to the console proxy resource for proxy:"+cmd.getProxyVmId(), e);
					return false;
				}
			}
		}else{
			return false;//no cert entry in the db record
		}
		return false;//cert already applied in previous cycles
    }
    
    @Override
    public ConsoleProxyVO persist(ConsoleProxyVO proxy) {
        return _consoleProxyDao.persist(proxy);
    }
    
    @Override
    public ConsoleProxyVO findById(long id) {
        return _consoleProxyDao.findById(id);
    }
    
    @Override
    public ConsoleProxyVO findByName(String name) {
        if (!VirtualMachineName.isValidConsoleProxyName(name)) {
            return null;
        }
        return findById(VirtualMachineName.getConsoleProxyId(name));
    }
    
    @Override
    public void finalizeStop(VirtualMachineProfile<ConsoleProxyVO> profile, StopAnswer answer) {
    }
}
