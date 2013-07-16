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
package com.cloud.consoleproxy;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ConsoleProxyLoadReportCommand;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupProxyCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.proxy.ConsoleProxyLoadAnswer;
import com.cloud.agent.manager.Commands;
import com.cloud.certificate.dao.CertificateDao;
import com.cloud.cluster.ClusterManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.ZoneConfig;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.info.ConsoleProxyConnectionInfo;
import com.cloud.info.ConsoleProxyInfo;
import com.cloud.info.ConsoleProxyLoadInfo;
import com.cloud.info.ConsoleProxyStatus;
import com.cloud.info.RunningHostCountInfo;
import com.cloud.info.RunningHostInfoAgregator;
import com.cloud.info.RunningHostInfoAgregator.ZoneHostInfo;
import com.cloud.keystore.KeystoreDao;
import com.cloud.keystore.KeystoreManager;
import com.cloud.keystore.KeystoreVO;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.rules.RulesManager;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.server.ManagementServer;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteriaService;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.events.SubscriptionMgr;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.SystemVmLoadScanHandler;
import com.cloud.vm.SystemVmLoadScanner;
import com.cloud.vm.SystemVmLoadScanner.AfterScanAction;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineGuru;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;

//
// Possible console proxy state transition cases
//		Stopped --> Starting -> Running
//		HA -> Stopped -> Starting -> Running
//		Migrating -> Running	(if previous state is Running before it enters into Migrating state
//		Migrating -> Stopped	(if previous state is not Running before it enters into Migrating state)
//		Running -> HA			(if agent lost connection)
//		Stopped -> Destroyed
//
// Starting, HA, Migrating, Running state are all counted as "Open" for available capacity calculation
// because sooner or later, it will be driven into Running state
//
@Local(value = { ConsoleProxyManager.class, ConsoleProxyService.class })
public class ConsoleProxyManagerImpl extends ManagerBase implements ConsoleProxyManager,
        VirtualMachineGuru<ConsoleProxyVO>, SystemVmLoadScanHandler<Long>, ResourceStateAdapter {
    private static final Logger s_logger = Logger.getLogger(ConsoleProxyManagerImpl.class);

    private static final int DEFAULT_CAPACITY_SCAN_INTERVAL = 30000; // 30 seconds
    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC = 180; // 3 minutes

    private static final int STARTUP_DELAY = 60000; // 60 seconds

    private int _consoleProxyPort = ConsoleProxyManager.DEFAULT_PROXY_VNC_PORT;

    private String _mgmt_host;
    private int _mgmt_port = 8250;

    @Inject
    private List<ConsoleProxyAllocator> _consoleProxyAllocators;

    @Inject
    private ConsoleProxyDao _consoleProxyDao;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private VMTemplateDao _templateDao;
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
    private TemplateDataStoreDao _vmTemplateStoreDao;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private StorageManager _storageMgr;
    @Inject
    NetworkManager _networkMgr;
    @Inject
    NetworkModel _networkModel;
    @Inject
    AccountManager _accountMgr;
    @Inject
    ServiceOfferingDao _offeringDao;
    @Inject
    DiskOfferingDao _diskOfferingDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    PrimaryDataStoreDao _storagePoolDao;
    @Inject
    UserVmDetailsDao _vmDetailsDao;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    NetworkDao _networkDao;
    @Inject
    RulesManager _rulesMgr;
    @Inject
    TemplateManager templateMgr;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    ManagementServer _ms;

    private ConsoleProxyListener _listener;

    private ServiceOfferingVO _serviceOffering;

    NetworkOffering _publicNetworkOffering;
    NetworkOffering _managementNetworkOffering;
    NetworkOffering _linkLocalNetworkOffering;

    @Inject
    private VirtualMachineManager _itMgr;

    /*
     * private final ExecutorService _requestHandlerScheduler = Executors.newCachedThreadPool(new
     * NamedThreadFactory("Request-handler"));
     */
    private long _capacityScanInterval = DEFAULT_CAPACITY_SCAN_INTERVAL;
    private int _capacityPerProxy = ConsoleProxyManager.DEFAULT_PROXY_CAPACITY;
    private int _standbyCapacity = ConsoleProxyManager.DEFAULT_STANDBY_CAPACITY;

    private boolean _use_lvm;
    private boolean _use_storage_vm;
    private boolean _disable_rp_filter = false;
    private String _instance;

    private int _proxySessionTimeoutValue = DEFAULT_PROXY_SESSION_TIMEOUT;
    private boolean _sslEnabled = true;

    // global load picture at zone basis
    private SystemVmLoadScanner<Long> _loadScanner;
    private Map<Long, ZoneHostInfo> _zoneHostInfoMap; // map <zone id, info about running host in zone>
    private Map<Long, ConsoleProxyLoadInfo> _zoneProxyCountMap; // map <zone id, info about proxy VMs count in zone>
    private Map<Long, ConsoleProxyLoadInfo> _zoneVmCountMap; // map <zone id, info about running VMs count in zone>

    private String _staticPublicIp;
    private int _staticPort;

    private final GlobalLock _allocProxyLock = GlobalLock.getInternLock(getAllocProxyLockName());

    @Inject
    private KeystoreDao _ksDao;
    @Inject
    private KeystoreManager _ksMgr;

    public class VmBasedAgentHook extends AgentHookBase {

        public VmBasedAgentHook(VMInstanceDao instanceDao, HostDao hostDao, ConfigurationDao cfgDao,
                KeystoreManager ksMgr, AgentManager agentMgr, ManagementServer ms) {
            super(instanceDao, hostDao, cfgDao, ksMgr, agentMgr, ms);
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
        public void onAgentDisconnect(long agentId, com.cloud.host.Status state) {

            if (state == com.cloud.host.Status.Alert || state == com.cloud.host.Status.Disconnected) {
                // be it either in alert or in disconnected state, the agent
                // process
                // may be gone in the VM,
                // we will be reacting to stop the corresponding VM and let the
                // scan
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

                        final ConsoleProxyVO proxy = _consoleProxyDao.findById(proxyVmId);
                        if (proxy != null) {

                            // Disable this feature for now, as it conflicts
                            // with
                            // the case of allowing user to reboot console proxy
                            // when rebooting happens, we will receive
                            // disconnect
                            // here and we can't enter into stopping process,
                            // as when the rebooted one comes up, it will kick
                            // off a
                            // newly started one and trigger the process
                            // continue on forever

                            /*
                             * _capacityScanScheduler.execute(new Runnable() {
                             * public void run() { if(s_logger.isInfoEnabled())
                             * s_logger.info("Stop console proxy " +
                             * proxy.getName() +
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

        @Override
        protected HostVO findConsoleProxyHost(StartupProxyCommand startupCmd) {
            long proxyVmId = startupCmd.getProxyVmId();
            ConsoleProxyVO consoleProxy = _consoleProxyDao.findById(proxyVmId);
            if (consoleProxy == null) {
                s_logger.info("Proxy " + proxyVmId + " is no longer in DB, skip sending startup command");
                return null;
            }

            assert (consoleProxy != null);
            return findConsoleProxyHostByName(consoleProxy.getHostName());
        }

    }

    @Override
    public ConsoleProxyInfo assignProxy(final long dataCenterId, final long vmId) {
        ConsoleProxyVO proxy = doAssignProxy(dataCenterId, vmId);
        if (proxy == null) {
            return null;
        }

        if (proxy.getPublicIpAddress() == null) {
            s_logger.warn("Assigned console proxy does not have a valid public IP address");
            return null;
        }

        KeystoreVO ksVo = _ksDao.findByName(ConsoleProxyManager.CERTIFICATE_NAME);
        assert (ksVo != null);

        if (_staticPublicIp == null) {
            return new ConsoleProxyInfo(proxy.isSslEnabled(), proxy.getPublicIpAddress(), _consoleProxyPort, proxy.getPort(), ksVo.getDomainSuffix());
        } else {
            return new ConsoleProxyInfo(proxy.isSslEnabled(), _staticPublicIp, _consoleProxyPort, _staticPort, ksVo.getDomainSuffix());
        }
    }

    public ConsoleProxyVO doAssignProxy(long dataCenterId, long vmId) {
        ConsoleProxyVO proxy = null;
        VMInstanceVO vm = _instanceDao.findById(vmId);

        if (vm == null) {
            s_logger.warn("VM " + vmId + " no longer exists, return a null proxy for vm:" + vmId);
            return null;
        }

        if (vm != null && vm.getState() != State.Running) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Detected that vm : " + vmId + " is not currently at running state, we will fail the proxy assignment for it");
            }
            return null;
        }

        if (_allocProxyLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
            try {
                if (vm.getProxyId() != null) {
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
            } finally {
                _allocProxyLock.unlock();
            }
        } else {
            s_logger.error("Unable to acquire synchronization lock to get/allocate proxy resource for vm :" + vmId + ". Previous console proxy allocation is taking too long");
        }

        if (proxy == null) {
            s_logger.warn("Unable to find or allocate console proxy resource");
            return null;
        }

        // if it is a new assignment or a changed assignment, update the record
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

    private static boolean isInAssignableState(ConsoleProxyVO proxy) {
        // console proxies that are in states of being able to serve user VM
        State state = proxy.getState();
        if (state == State.Running) {
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
            ConsoleProxyVO proxy = _consoleProxyDao.findById(proxyVmId);
            Account systemAcct = _accountMgr.getSystemAccount();
            User systemUser = _accountMgr.getSystemUser();
            if (proxy.getState() == VirtualMachine.State.Running) {
                return proxy;
            }

            String restart = _configDao.getValue(Config.ConsoleProxyRestart.key());
            if (restart != null && restart.equalsIgnoreCase("false")) {
                return null;
            }

            if (proxy.getState() == VirtualMachine.State.Stopped) {
                return _itMgr.start(proxy, null, systemUser, systemAcct);
            }

            // For VMs that are in Stopping, Starting, Migrating state, let client to wait by returning null
            // as sooner or later, Starting/Migrating state will be transited to Running and Stopping will be transited
// to
            // Stopped to allow
            // Starting of it
            s_logger.warn("Console proxy is not in correct state to be started: " + proxy.getState());
            return null;
        } catch (StorageUnavailableException e) {
            s_logger.warn("Exception while trying to start console proxy", e);
            return null;
        } catch (InsufficientCapacityException e) {
            s_logger.warn("Exception while trying to start console proxy", e);
            return null;
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Exception while trying to start console proxy", e);
            return null;
        } catch (CloudRuntimeException e) {
            s_logger.warn("Runtime Exception while trying to start console proxy", e);
            return null;
        }
    }

    public ConsoleProxyVO assignProxyFromRunningPool(long dataCenterId) {

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Assign console proxy from running pool for request from data center : " + dataCenterId);
        }

        ConsoleProxyAllocator allocator = getCurrentAllocator();
        assert (allocator != null);
        List<ConsoleProxyVO> runningList = _consoleProxyDao.getProxyListInStates(dataCenterId, State.Running);
        if (runningList != null && runningList.size() > 0) {
            Iterator<ConsoleProxyVO> it = runningList.iterator();
            while (it.hasNext()) {
                ConsoleProxyVO proxy = it.next();
                if(proxy.getActiveSession() >= _capacityPerProxy){
                    it.remove();
                }
            }
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Running proxy pool size : " + runningList.size());
                for (ConsoleProxyVO proxy : runningList) {
                    s_logger.trace("Running proxy instance : " + proxy.getHostName());
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

        // practically treat all console proxy VM that is not in Running state but can be entering into Running state as
        // candidates
        // this is to prevent launching unneccessary console proxy VMs because of temporarily unavailable state
        List<ConsoleProxyVO> l = _consoleProxyDao.getProxyListInStates(dataCenterId, State.Starting, State.Stopped, State.Migrating, State.Stopping);
        if (l != null && l.size() > 0) {
            return l.get(0);
        }

        return null;
    }

    public ConsoleProxyVO startNew(long dataCenterId) throws ConcurrentOperationException {

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Assign console proxy from a newly started instance for request from data center : " + dataCenterId);
        }

        if (!allowToLaunchNew(dataCenterId)) {
            s_logger.warn("The number of launched console proxy on zone " + dataCenterId + " has reached to limit");
            return null;
        }
        HypervisorType defaultHype = _resourceMgr.getAvailableHypervisor(dataCenterId);

        Map<String, Object> context = createProxyInstance(dataCenterId, defaultHype);

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

            SubscriptionMgr.getInstance().notifySubscribers(ConsoleProxyManager.ALERT_SUBJECT, this,
                    new ConsoleProxyAlertEventArgs(ConsoleProxyAlertEventArgs.PROXY_CREATE_FAILURE, dataCenterId, proxyVmId, null, "Unable to allocate storage"));
        }
        return null;
    }

    protected Map<String, Object> createProxyInstance(long dataCenterId, HypervisorType desiredHyp) throws ConcurrentOperationException {

        long id = _consoleProxyDao.getNextInSequence(Long.class, "id");
        String name = VirtualMachineName.getConsoleProxyName(id, _instance);
        DataCenterVO dc = _dcDao.findById(dataCenterId);
        Account systemAcct = _accountMgr.getSystemAccount();

        DataCenterDeployment plan = new DataCenterDeployment(dataCenterId);

        NetworkVO defaultNetwork = null;
        if (dc.getNetworkType() == NetworkType.Advanced && dc.isSecurityGroupEnabled()) {
            List<NetworkVO> networks = _networkDao.listByZoneSecurityGroup(dataCenterId);
            if (networks == null || networks.size() == 0) {
                throw new CloudRuntimeException("Can not found security enabled network in SG Zone " + dc);
            }
            defaultNetwork = networks.get(0);
        } else {
        TrafficType defaultTrafficType = TrafficType.Public;
        if (dc.getNetworkType() == NetworkType.Basic || dc.isSecurityGroupEnabled()) {
            defaultTrafficType = TrafficType.Guest;
        }
        List<NetworkVO> defaultNetworks = _networkDao.listByZoneAndTrafficType(dataCenterId, defaultTrafficType);

            // api should never allow this situation to happen
        if (defaultNetworks.size() != 1) {
                throw new CloudRuntimeException("Found " + defaultNetworks.size() + " networks of type "
                      + defaultTrafficType + " when expect to find 1");
            }
             defaultNetwork = defaultNetworks.get(0);
        }

        List<? extends NetworkOffering> offerings = _networkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemControlNetwork, NetworkOffering.SystemManagementNetwork);
        List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>(offerings.size() + 1);
        NicProfile defaultNic = new NicProfile();
        defaultNic.setDefaultNic(true);
        defaultNic.setDeviceId(2);

        networks.add(new Pair<NetworkVO, NicProfile>(_networkMgr.setupNetwork(systemAcct, _networkOfferingDao.findById(defaultNetwork.getNetworkOfferingId()), plan, null, null, false).get(0), defaultNic));

        for (NetworkOffering offering : offerings) {
            networks.add(new Pair<NetworkVO, NicProfile>(_networkMgr.setupNetwork(systemAcct, offering, plan, null, null, false).get(0), null));
        }

        VMTemplateVO template = _templateDao.findSystemVMTemplate(dataCenterId, desiredHyp);
        if (template == null) {
            s_logger.debug("Can't find a template to start");
            throw new CloudRuntimeException("Insufficient capacity exception");
        }

        ConsoleProxyVO proxy = new ConsoleProxyVO(id, _serviceOffering.getId(), name, template.getId(), template.getHypervisorType(), template.getGuestOSId(), dataCenterId, systemAcct.getDomainId(),
                systemAcct.getId(), 0, _serviceOffering.getOfferHA());
        try {
            proxy = _itMgr.allocate(proxy, template, _serviceOffering, networks, plan, null, systemAcct);
        } catch (InsufficientCapacityException e) {
            s_logger.warn("InsufficientCapacity", e);
            throw new CloudRuntimeException("Insufficient capacity exception", e);
        }

        Map<String, Object> context = new HashMap<String, Object>();
        context.put("dc", dc);
        HostPodVO pod = _podDao.findById(proxy.getPodIdToDeployIn());
        context.put("pod", pod);
        context.put("proxyVmId", proxy.getId());

        return context;
    }

    private ConsoleProxyAllocator getCurrentAllocator() {
        // for now, only one adapter is supported
        for(ConsoleProxyAllocator allocator : _consoleProxyAllocators) {
            return allocator;
        }

        return null;
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



    public void handleAgentDisconnect(long agentId, com.cloud.host.Status state) {
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

                    final ConsoleProxyVO proxy = _consoleProxyDao.findById(proxyVmId);
                    if (proxy != null) {

                        // Disable this feature for now, as it conflicts with
                        // the case of allowing user to reboot console proxy
                        // when rebooting happens, we will receive disconnect
                        // here and we can't enter into stopping process,
                        // as when the rebooted one comes up, it will kick off a
                        // newly started one and trigger the process
                        // continue on forever

                        /*
                         * _capacityScanScheduler.execute(new Runnable() { public void run() {
                         * if(s_logger.isInfoEnabled())
                         * s_logger.info("Stop console proxy " + proxy.getName() +
                         * " VM because of that the agent running inside it has disconnected" );
                         * stopProxy(proxy.getId()); } });
                         */
                    } else {
                        if (s_logger.isInfoEnabled()) {
                            s_logger.info("Console proxy agent disconnected but corresponding console proxy VM no longer exists in DB, proxy: " + name);
                        }
                    }
                } else {
                    assert (false) : "Invalid console proxy name: " + name;
                }
            }
        }
    }

    private boolean reserveStandbyCapacity() {
        ConsoleProxyManagementState state = getManagementState();
        if (state == null || state != ConsoleProxyManagementState.Auto) {
            return false;
        }

        return true;
    }

    private boolean isConsoleProxyVmRequired(long dcId) {
        DataCenterVO dc = _dcDao.findById(dcId);
        _dcDao.loadDetails(dc);
        String cpvmReq = dc.getDetail(ZoneConfig.EnableConsoleProxyVm.key());
        if (cpvmReq != null) {
            return Boolean.parseBoolean(cpvmReq);
        }
        return true;
    }

    private boolean allowToLaunchNew(long dcId) {
        if (!isConsoleProxyVmRequired(dcId)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Console proxy vm not required in zone " + dcId + " not launching");
            }
            return false;
        }
        List<ConsoleProxyVO> l = _consoleProxyDao.getProxyListInStates(dcId, VirtualMachine.State.Starting, VirtualMachine.State.Running, VirtualMachine.State.Stopping, VirtualMachine.State.Stopped,
                VirtualMachine.State.Migrating, VirtualMachine.State.Shutdowned, VirtualMachine.State.Unknown);

        String value = _configDao.getValue(Config.ConsoleProxyLaunchMax.key());
        int launchLimit = NumbersUtil.parseInt(value, 10);
        return l.size() < launchLimit;
    }

    private HypervisorType currentHypervisorType(long dcId) {
        List<ConsoleProxyVO> l = _consoleProxyDao.getProxyListInStates(dcId, VirtualMachine.State.Starting, VirtualMachine.State.Running, VirtualMachine.State.Stopping, VirtualMachine.State.Stopped,
                VirtualMachine.State.Migrating, VirtualMachine.State.Shutdowned, VirtualMachine.State.Unknown);

        return l.size() > 0 ? l.get(0).getHypervisorType() : HypervisorType.Any;
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

        ConsoleProxyVO proxy = null;
        if (_allocProxyLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
            try {
                proxy = assignProxyFromStoppedPool(dataCenterId);
                if (proxy == null) {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("No stopped console proxy is available, need to allocate a new console proxy");
                    }

                    try {
                        proxy = startNew(dataCenterId);
                    } catch (ConcurrentOperationException e) {
                        s_logger.info("Concurrent Operation caught " + e);
                    }
                } else {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Found a stopped console proxy, bring it up to running pool. proxy vm id : " + proxy.getId());
                    }
                }
            } finally {
                _allocProxyLock.unlock();
            }
        } else {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Unable to acquire proxy allocation lock, skip for next time");
            }
        }

        if (proxy != null) {
            long proxyVmId = proxy.getId();
            proxy = startProxy(proxyVmId);

            if (proxy != null) {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Console proxy " + proxy.getHostName() + " is started");
                }
            }
        }
    }

    public boolean isZoneReady(Map<Long, ZoneHostInfo> zoneHostInfoMap, long dataCenterId) {
        ZoneHostInfo zoneHostInfo = zoneHostInfoMap.get(dataCenterId);
        if (zoneHostInfo != null && isZoneHostReady(zoneHostInfo)) {
            VMTemplateVO template = _templateDao.findSystemVMTemplate(dataCenterId);
            TemplateDataStoreVO templateHostRef = _vmTemplateStoreDao.findByTemplateZoneDownloadStatus(template.getId(), dataCenterId,
                    Status.DOWNLOADED);

            if (templateHostRef != null) {
                List<Pair<Long, Integer>> l = _consoleProxyDao.getDatacenterStoragePoolHostInfo(dataCenterId, _use_lvm);
                if (l != null && l.size() > 0 && l.get(0).second().intValue() > 0) {
                    return true;
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Primary storage is not ready, wait until it is ready to launch console proxy");
                    }
                }
            } else {
                if (s_logger.isDebugEnabled()) {
                    if (template == null) {
                        s_logger.debug("Zone host is ready, but console proxy template is null");
                    } else {
                        s_logger.debug("Zone host is ready, but console proxy template: " + template.getId() + " is not ready on secondary storage.");
                    }
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

        _loadScanner.stop();
        _allocProxyLock.releaseRef();
        _resourceMgr.unregisterResourceStateAdapter(this.getClass().getSimpleName());
        return true;
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
        ConsoleProxyVO proxy = _consoleProxyDao.findById(proxyVmId);
        if (proxy == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Stopping console proxy failed: console proxy " + proxyVmId + " no longer exists");
            }
            return false;
        }

        try {
            return _itMgr.stop(proxy, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount());
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Stopping console proxy " + proxy.getHostName() + " failed : exception " + e.toString());
            return false;
        }
    }

    @Override
    @DB
    public void setManagementState(ConsoleProxyManagementState state) {
        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();

            ConsoleProxyManagementState lastState = getManagementState();
            if (lastState == null) {
                txn.commit();
                return;
            }

            if (lastState != state) {
                _configDao.update(Config.ConsoleProxyManagementLastState.key(), Config.ConsoleProxyManagementLastState.getCategory(), lastState.toString());
                _configDao.update(Config.ConsoleProxyManagementState.key(), Config.ConsoleProxyManagementState.getCategory(), state.toString());
            }

            txn.commit();
        } catch (Throwable e) {
            txn.rollback();
        }
    }

    @Override
    public ConsoleProxyManagementState getManagementState() {
        String value = _configDao.getValue(Config.ConsoleProxyManagementState.key());
        if (value != null) {
            ConsoleProxyManagementState state = ConsoleProxyManagementState.valueOf(value);

            if (state == null) {
                s_logger.error("Invalid console proxy management state: " + value);
            }
            return state;
        }

        s_logger.error("Invalid console proxy management state: " + value);
        return null;
    }

    @Override
    @DB
    public void resumeLastManagementState() {
        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            ConsoleProxyManagementState state = getManagementState();
            ConsoleProxyManagementState lastState = getLastManagementState();
            if (lastState == null) {
                txn.commit();
                return;
            }

            if (lastState != state) {
                _configDao.update(Config.ConsoleProxyManagementState.key(), Config.ConsoleProxyManagementState.getCategory(), lastState.toString());
            }

            txn.commit();
        } catch (Throwable e) {
            txn.rollback();
        }
    }

    private ConsoleProxyManagementState getLastManagementState() {
        String value = _configDao.getValue(Config.ConsoleProxyManagementLastState.key());
        if (value != null) {
            ConsoleProxyManagementState state = ConsoleProxyManagementState.valueOf(value);

            if (state == null) {
                s_logger.error("Invalid console proxy management state: " + value);
            }
            return state;
        }

        s_logger.error("Invalid console proxy management state: " + value);
        return null;
    }

    @Override
    public boolean rebootProxy(long proxyVmId) {
        final ConsoleProxyVO proxy = _consoleProxyDao.findById(proxyVmId);

        if (proxy == null || proxy.getState() == State.Destroyed) {
            return false;
        }

        if (proxy.getState() == State.Running && proxy.getHostId() != null) {
            final RebootCommand cmd = new RebootCommand(proxy.getInstanceName());
            final Answer answer = _agentMgr.easySend(proxy.getHostId(), cmd);

            if (answer != null && answer.getResult()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Successfully reboot console proxy " + proxy.getHostName());
                }

                SubscriptionMgr.getInstance().notifySubscribers(ConsoleProxyManager.ALERT_SUBJECT, this,
                        new ConsoleProxyAlertEventArgs(ConsoleProxyAlertEventArgs.PROXY_REBOOTED, proxy.getDataCenterId(), proxy.getId(), proxy, null));

                return true;
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("failed to reboot console proxy : " + proxy.getHostName());
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
            //expunge the vm
            boolean result = _itMgr.expunge(proxy, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount());
            if (result) {
                HostVO host = _hostDao.findByTypeNameAndZoneId(proxy.getDataCenterId(), proxy.getHostName(),
                        Host.Type.ConsoleProxy);
                if (host != null) {
                    s_logger.debug("Removing host entry for proxy id=" + vmId);
                    result = result && _hostDao.remove(host.getId());
                }
            }

            return result;
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to expunge " + proxy, e);
            return false;
        }
    }

    private String getAllocProxyLockName() {
        return "consoleproxy.alloc";
    }

    private void prepareDefaultCertificate() {
        GlobalLock lock = GlobalLock.getInternLock("consoleproxy.cert.setup");
        try {
            if (lock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
                KeystoreVO ksVo = _ksDao.findByName(CERTIFICATE_NAME);
                if (ksVo == null) {
                    _ksDao.save(CERTIFICATE_NAME, ConsoleProxyVO.certContent, ConsoleProxyVO.keyContent, "realhostip.com");
                    KeystoreVO caRoot = new KeystoreVO();
                    caRoot.setCertificate(ConsoleProxyVO.rootCa);
                    caRoot.setDomainSuffix("realhostip.com");
                    caRoot.setName("root");
                    caRoot.setIndex(0);
                    _ksDao.persist(caRoot);
                }
                lock.unlock();
            }
        } finally {
            lock.releaseRef();
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Start configuring console proxy manager : " + name);
        }

        Map<String, String> configs = _configDao.getConfiguration("management-server", params);

        String value = configs.get(Config.ConsoleProxyCmdPort.key());
        value = configs.get("consoleproxy.sslEnabled");
        if (value != null && value.equalsIgnoreCase("true")) {
            _sslEnabled = true;
        }

        value = configs.get(Config.ConsoleProxyCapacityScanInterval.key());
        _capacityScanInterval = NumbersUtil.parseLong(value, DEFAULT_CAPACITY_SCAN_INTERVAL);

        _capacityPerProxy = NumbersUtil.parseInt(configs.get("consoleproxy.session.max"), DEFAULT_PROXY_CAPACITY);
        _standbyCapacity = NumbersUtil.parseInt(configs.get("consoleproxy.capacity.standby"), DEFAULT_STANDBY_CAPACITY);
        _proxySessionTimeoutValue = NumbersUtil.parseInt(configs.get("consoleproxy.session.timeout"), DEFAULT_PROXY_SESSION_TIMEOUT);

        value = configs.get("consoleproxy.port");
        if (value != null) {
            _consoleProxyPort = NumbersUtil.parseInt(value, ConsoleProxyManager.DEFAULT_PROXY_VNC_PORT);
        }

        value = configs.get(Config.ConsoleProxyDisableRpFilter.key());
        if (value != null && value.equalsIgnoreCase("true")) {
            _disable_rp_filter = true;
        }

        value = configs.get(Config.SystemVMUseLocalStorage.key());
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

        _instance = configs.get("instance.name");
        if (_instance == null) {
            _instance = "DEFAULT";
        }

        prepareDefaultCertificate();

        Map<String, String> agentMgrConfigs = _configDao.getConfiguration("AgentManager", params);
        _mgmt_host = agentMgrConfigs.get("host");
        if (_mgmt_host == null) {
            s_logger.warn("Critical warning! Please configure your management server host address right after you have started your management server and then restart it, otherwise you won't be able to do console access");
        }

        value = agentMgrConfigs.get("port");
        _mgmt_port = NumbersUtil.parseInt(value, 8250);

        _listener =
                new ConsoleProxyListener(new VmBasedAgentHook(_instanceDao, _hostDao, _configDao, _ksMgr,
                        _agentMgr, _ms));
        _agentMgr.registerForHostEvents(_listener, true, true, false);

        _itMgr.registerGuru(VirtualMachine.Type.ConsoleProxy, this);

        boolean useLocalStorage = Boolean.parseBoolean(configs.get(Config.SystemVMUseLocalStorage.key()));

        //check if there is a default service offering configured
        String cpvmSrvcOffIdStr = configs.get(Config.ConsoleProxyServiceOffering.key());
        if (cpvmSrvcOffIdStr != null) {
            DiskOffering diskOffering = _diskOfferingDao.findByUuid(cpvmSrvcOffIdStr);
            if (diskOffering == null)
                diskOffering = _diskOfferingDao.findById(Long.parseLong(cpvmSrvcOffIdStr));
            if (diskOffering != null) {
                _serviceOffering = _offeringDao.findById(diskOffering.getId());
            } else {
                s_logger.warn("Can't find system service offering specified by global config, uuid=" + cpvmSrvcOffIdStr + " for console proxy vm");
            }
        }

        if(_serviceOffering == null || !_serviceOffering.getSystemUse()){
            int ramSize = NumbersUtil.parseInt(_configDao.getValue("console.ram.size"), DEFAULT_PROXY_VM_RAMSIZE);
            int cpuFreq = NumbersUtil.parseInt(_configDao.getValue("console.cpu.mhz"), DEFAULT_PROXY_VM_CPUMHZ);
            _serviceOffering = new ServiceOfferingVO("System Offering For Console Proxy", 1, ramSize, cpuFreq, 0, 0, false, null, useLocalStorage, true, null, true, VirtualMachine.Type.ConsoleProxy, true);
            _serviceOffering.setUniqueName(ServiceOffering.consoleProxyDefaultOffUniqueName);
            _serviceOffering = _offeringDao.persistSystemServiceOffering(_serviceOffering);

            // this can sometimes happen, if DB is manually or programmatically manipulated
            if (_serviceOffering == null) {
                String msg = "Data integrity problem : System Offering For Console Proxy has been removed?";
                s_logger.error(msg);
                throw new ConfigurationException(msg);
            }
        }

        _loadScanner = new SystemVmLoadScanner<Long>(this);
        _loadScanner.initScan(STARTUP_DELAY, _capacityScanInterval);
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);

        _staticPublicIp = _configDao.getValue("consoleproxy.static.publicIp");
        if (_staticPublicIp != null) {
            _staticPort = NumbersUtil.parseInt(_configDao.getValue("consoleproxy.static.port"), 8443);
        }

        if (s_logger.isInfoEnabled()) {
            s_logger.info("Console Proxy Manager is configured.");
        }
        return true;
    }



    protected ConsoleProxyManagerImpl() {
    }

    @Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile<ConsoleProxyVO> profile, DeployDestination dest, ReservationContext context) {

        ConsoleProxyVO vm = profile.getVirtualMachine();
        Map<String, String> details = _vmDetailsDao.findDetails(vm.getId());
        vm.setDetails(details);

        StringBuilder buf = profile.getBootArgsBuilder();
        buf.append(" template=domP type=consoleproxy");
        buf.append(" host=").append(_mgmt_host);
        buf.append(" port=").append(_mgmt_port);
        buf.append(" name=").append(profile.getVirtualMachine().getHostName());
        if (_sslEnabled) {
            buf.append(" premium=true");
        }
        buf.append(" zone=").append(dest.getDataCenter().getId());
        buf.append(" pod=").append(dest.getPod().getId());
        buf.append(" guid=Proxy.").append(profile.getId());
        buf.append(" proxy_vm=").append(profile.getId());
        if (_disable_rp_filter) {
            buf.append(" disable_rp_filter=true");
        }

        boolean externalDhcp = false;
        String externalDhcpStr = _configDao.getValue("direct.attach.network.externalIpAllocator.enabled");
        if (externalDhcpStr != null && externalDhcpStr.equalsIgnoreCase("true")) {
            externalDhcp = true;
        }

        if (Boolean.valueOf(_configDao.getValue("system.vm.random.password"))) {
            buf.append(" vmpassword=").append(_configDao.getValue("system.vm.password"));
        }

        for (NicProfile nic : profile.getNics()) {
            int deviceId = nic.getDeviceId();
            if (nic.getIp4Address() == null) {
                buf.append(" eth").append(deviceId).append("ip=").append("0.0.0.0");
                buf.append(" eth").append(deviceId).append("mask=").append("0.0.0.0");
            } else {
                buf.append(" eth").append(deviceId).append("ip=").append(nic.getIp4Address());
                buf.append(" eth").append(deviceId).append("mask=").append(nic.getNetmask());
            }

            if (nic.isDefaultNic()) {
                buf.append(" gateway=").append(nic.getGateway());
            }

            if (nic.getTrafficType() == TrafficType.Management) {
                String mgmt_cidr = _configDao.getValue(Config.ManagementNetwork.key());
                if (NetUtils.isValidCIDR(mgmt_cidr)) {
                    buf.append(" mgmtcidr=").append(mgmt_cidr);
                }
                buf.append(" localgw=").append(dest.getPod().getGateway());
            }
        }

        /* External DHCP mode */
        if (externalDhcp) {
            buf.append(" bootproto=dhcp");
        }
        DataCenterVO dc = _dcDao.findById(profile.getVirtualMachine().getDataCenterId());
        buf.append(" internaldns1=").append(dc.getInternalDns1());
        if (dc.getInternalDns2() != null) {
            buf.append(" internaldns2=").append(dc.getInternalDns2());
        }
        buf.append(" dns1=").append(dc.getDns1());
        if (dc.getDns2() != null) {
            buf.append(" dns2=").append(dc.getDns2());
        }

        String bootArgs = buf.toString();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Boot Args for " + profile + ": " + bootArgs);
        }

        return true;
    }

    @Override
    public boolean finalizeDeployment(Commands cmds, VirtualMachineProfile<ConsoleProxyVO> profile, DeployDestination dest, ReservationContext context) {

        finalizeCommandsOnStart(cmds, profile);

        ConsoleProxyVO proxy = profile.getVirtualMachine();
        DataCenter dc = dest.getDataCenter();
        List<NicProfile> nics = profile.getNics();
        for (NicProfile nic : nics) {
            if ((nic.getTrafficType() == TrafficType.Public && dc.getNetworkType() == NetworkType.Advanced)
                    || (nic.getTrafficType() == TrafficType.Guest && (dc.getNetworkType() == NetworkType.Basic || dc.isSecurityGroupEnabled()))) {
                proxy.setPublicIpAddress(nic.getIp4Address());
                proxy.setPublicNetmask(nic.getNetmask());
                proxy.setPublicMacAddress(nic.getMacAddress());
            } else if (nic.getTrafficType() == TrafficType.Management) {
                proxy.setPrivateIpAddress(nic.getIp4Address());
                proxy.setPrivateMacAddress(nic.getMacAddress());
            }
        }
        _consoleProxyDao.update(proxy.getId(), proxy);
        return true;
    }

    @Override
    public boolean finalizeCommandsOnStart(Commands cmds, VirtualMachineProfile<ConsoleProxyVO> profile) {

        NicProfile managementNic = null;
        NicProfile controlNic = null;
        for (NicProfile nic : profile.getNics()) {
            if (nic.getTrafficType() == TrafficType.Management) {
                managementNic = nic;
            } else if (nic.getTrafficType() == TrafficType.Control && nic.getIp4Address() != null) {
                controlNic = nic;
            }
        }

        if (controlNic == null) {
            if (managementNic == null) {
                s_logger.error("Management network doesn't exist for the console proxy vm " + profile.getVirtualMachine());
                return false;
            }
            controlNic = managementNic;
        }

        CheckSshCommand check = new CheckSshCommand(profile.getInstanceName(), controlNic.getIp4Address(), 3922);
        cmds.addCommand("checkSsh", check);

        return true;
    }

    @Override
    public boolean finalizeStart(VirtualMachineProfile<ConsoleProxyVO> profile, long hostId, Commands cmds, ReservationContext context) {
        CheckSshAnswer answer = (CheckSshAnswer) cmds.getAnswer("checkSsh");
        if (answer == null || !answer.getResult()) {
            if (answer != null) {
                s_logger.warn("Unable to ssh to the VM: " + answer.getDetails());
            } else {
                s_logger.warn("Unable to ssh to the VM: null answer");
            }
            return false;
        }

        try {
            //get system ip and create static nat rule for the vm in case of basic networking with EIP/ELB
            _rulesMgr.getSystemIpAndEnableStaticNatForVm(profile.getVirtualMachine(), false);
            IPAddressVO ipaddr = _ipAddressDao.findByAssociatedVmId(profile.getVirtualMachine().getId());
            if (ipaddr != null && ipaddr.getSystem()) {
                ConsoleProxyVO consoleVm = profile.getVirtualMachine();
                // override CPVM guest IP with EIP, so that console url's will be prepared with EIP
                consoleVm.setPublicIpAddress(ipaddr.getAddress().addr());
                _consoleProxyDao.update(consoleVm.getId(), consoleVm);
            }
        } catch (Exception ex) {
            s_logger.warn("Failed to get system ip and enable static nat for the vm " + profile.getVirtualMachine() + " due to exception ", ex);
            return false;
        }

        return true;
    }

    @Override
    public void finalizeExpunge(ConsoleProxyVO proxy) {
        proxy.setPublicIpAddress(null);
        proxy.setPublicMacAddress(null);
        proxy.setPublicNetmask(null);
        proxy.setPrivateMacAddress(null);
        proxy.setPrivateIpAddress(null);
        _consoleProxyDao.update(proxy.getId(), proxy);
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
        //release elastic IP here if assigned
        IPAddressVO ip = _ipAddressDao.findByAssociatedVmId(profile.getId());
        if (ip != null && ip.getSystem()) {
            CallContext ctx = CallContext.current();
            try {
                _rulesMgr.disableStaticNat(ip.getId(), ctx.getCallingAccount(), ctx.getCallingUserId(), true);
            } catch (Exception ex) {
                s_logger.warn("Failed to disable static nat and release system ip " + ip + " as a part of vm " + profile.getVirtualMachine() + " stop due to exception ", ex);
            }
        }
    }

    @Override
    public String getScanHandlerName() {
        return "consoleproxy";
    }

    @Override
    public void onScanStart() {
        // to reduce possible number of DB queries for capacity scan, we run following aggregated queries in preparation
// stage
        _zoneHostInfoMap = getZoneHostInfo();

        _zoneProxyCountMap = new HashMap<Long, ConsoleProxyLoadInfo>();
        List<ConsoleProxyLoadInfo> listProxyCounts = _consoleProxyDao.getDatacenterProxyLoadMatrix();
        for (ConsoleProxyLoadInfo info : listProxyCounts) {
            _zoneProxyCountMap.put(info.getId(), info);
        }

        _zoneVmCountMap = new HashMap<Long, ConsoleProxyLoadInfo>();
        List<ConsoleProxyLoadInfo> listVmCounts = _consoleProxyDao.getDatacenterSessionLoadMatrix();
        for (ConsoleProxyLoadInfo info : listVmCounts) {
            _zoneVmCountMap.put(info.getId(), info);
        }
    }

    private void scanManagementState() {
        ConsoleProxyManagementState state = getManagementState();
        if (state != null) {
            switch (state) {
            case Auto:
            case Manual:
            case Suspending:
                break;

            case ResetSuspending:
                handleResetSuspending();
                break;

            default:
                assert (false);
            }
        }
    }

    private void handleResetSuspending() {
        List<ConsoleProxyVO> runningProxies = _consoleProxyDao.getProxyListInStates(State.Running);
        for (ConsoleProxyVO proxy : runningProxies) {
            s_logger.info("Stop console proxy " + proxy.getId() + " because of we are currently in ResetSuspending management mode");
            stopProxy(proxy.getId());
        }

        // check if it is time to resume
        List<ConsoleProxyVO> proxiesInTransition = _consoleProxyDao.getProxyListInStates(State.Running, State.Starting, State.Stopping);
        if (proxiesInTransition.size() == 0) {
            s_logger.info("All previous console proxy VMs in transition mode ceased the mode, we will now resume to last management state");
            resumeLastManagementState();
        }
    }

    @Override
    public boolean canScan() {
        // take the chance to do management-state management
        scanManagementState();

        if (!reserveStandbyCapacity()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Reserving standby capacity is disable, skip capacity scan");
            }
            return false;
        }

        List<StoragePoolVO> upPools = _storagePoolDao.listByStatus(StoragePoolStatus.Up);
        if (upPools == null || upPools.size() == 0) {
            s_logger.debug("Skip capacity scan due to there is no Primary Storage UPintenance mode");
            return false;
        }

        return true;
    }

    @Override
    public Long[] getScannablePools() {
        List<DataCenterVO> zones = _dcDao.listEnabledZones();

        Long[] dcIdList = new Long[zones.size()];
        int i = 0;
        for (DataCenterVO dc : zones) {
            dcIdList[i++] = dc.getId();
        }

        return dcIdList;
    }

    @Override
    public boolean isPoolReadyForScan(Long pool) {
        // pool is at zone basis
        long dataCenterId = pool.longValue();

        if (!isZoneReady(_zoneHostInfoMap, dataCenterId)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Zone " + dataCenterId + " is not ready to launch console proxy yet");
            }
            return false;
        }

        List<ConsoleProxyVO> l = _consoleProxyDao.getProxyListInStates(VirtualMachine.State.Starting, VirtualMachine.State.Stopping);
        if (l.size() > 0) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Zone " + dataCenterId + " has " + l.size() + " console proxy VM(s) in transition state");
            }

            return false;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Zone " + dataCenterId + " is ready to launch console proxy");
        }
        return true;
    }

    @Override
    public Pair<AfterScanAction, Object> scanPool(Long pool) {
        long dataCenterId = pool.longValue();

        ConsoleProxyLoadInfo proxyInfo = _zoneProxyCountMap.get(dataCenterId);
        if (proxyInfo == null) {
            return new Pair<AfterScanAction, Object>(AfterScanAction.nop, null);
        }

        ConsoleProxyLoadInfo vmInfo = _zoneVmCountMap.get(dataCenterId);
        if (vmInfo == null) {
            vmInfo = new ConsoleProxyLoadInfo();
        }

        if (!checkCapacity(proxyInfo, vmInfo)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Expand console proxy standby capacity for zone " + proxyInfo.getName());
            }

            return new Pair<AfterScanAction, Object>(AfterScanAction.expand, null);
        }

        return new Pair<AfterScanAction, Object>(AfterScanAction.nop, null);
    }

    @Override
    public void expandPool(Long pool, Object actionArgs) {
        long dataCenterId = pool.longValue();
        allocCapacity(dataCenterId);
    }

    @Override
    public void shrinkPool(Long pool, Object actionArgs) {
    }

    @Override
    public void onScanEnd() {
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        if (!(cmd[0] instanceof StartupProxyCommand)) {
            return null;
        }

        host.setType(com.cloud.host.Host.Type.ConsoleProxy);
        return host;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details,
            List<String> hostTags) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        // TODO Auto-generated method stub
        return null;
    }

    protected HostVO findConsoleProxyHostByName(String name) {
        SearchCriteriaService<HostVO, HostVO> sc = SearchCriteria2.create(HostVO.class);
        sc.addAnd(sc.getEntity().getType(), Op.EQ, Host.Type.ConsoleProxy);
        sc.addAnd(sc.getEntity().getName(), Op.EQ, name);
        return sc.find();
    }

    @Override
    public void prepareStop(VirtualMachineProfile<ConsoleProxyVO> profile) {
    }

}
