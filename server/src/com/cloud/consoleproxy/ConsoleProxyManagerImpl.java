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
import java.net.URLDecoder;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ConsoleAccessAuthenticationAnswer;
import com.cloud.agent.api.ConsoleAccessAuthenticationCommand;
import com.cloud.agent.api.ConsoleProxyLoadReportCommand;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.NetworkRulesSystemVmCommand;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.StartConsoleProxyAnswer;
import com.cloud.agent.api.StartConsoleProxyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.proxy.ConsoleProxyLoadAnswer;
import com.cloud.async.AsyncJobExecutor;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.cluster.ClusterManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.DomainVO;
import com.cloud.event.EventState;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.ha.dao.HighAvailabilityDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Host.Type;
import com.cloud.host.dao.HostDao;
import com.cloud.info.ConsoleProxyConnectionInfo;
import com.cloud.info.ConsoleProxyLoadInfo;
import com.cloud.info.ConsoleProxyStatus;
import com.cloud.info.RunningHostCountInfo;
import com.cloud.info.RunningHostInfoAgregator;
import com.cloud.info.RunningHostInfoAgregator.ZoneHostInfo;
import com.cloud.maid.StackMaid;
import com.cloud.network.IpAddrAllocator;
import com.cloud.network.IpAddrAllocator.networkInfo;
import com.cloud.network.NetworkManager;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.ServiceOffering.GuestIpType;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.servlet.ConsoleProxyServlet;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.events.SubscriptionMgr;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.State;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.dao.ConsoleProxyDao;
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
@Local(value = { ConsoleProxyManager.class })
public class ConsoleProxyManagerImpl implements ConsoleProxyManager,
		VirtualMachineManager<ConsoleProxyVO> {
	private static final Logger s_logger = Logger
			.getLogger(ConsoleProxyManagerImpl.class);

	private static final int DEFAULT_FIND_HOST_RETRY_COUNT = 2;
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

	private ConsoleProxyDao _consoleProxyDao;
	private DataCenterDao _dcDao;
	private VlanDao _vlanDao;
	private VMTemplateDao _templateDao;
	private IPAddressDao _ipAddressDao;
	private VolumeDao _volsDao;
	private HostPodDao _podDao;
	private HostDao _hostDao;
	private StoragePoolDao _storagePoolDao;

	private VMInstanceDao _instanceDao;
	private AccountDao _accountDao;

	private VMTemplateHostDao _vmTemplateHostDao;
	private CapacityDao _capacityDao;
	private HighAvailabilityDao _haDao;

	private AgentManager _agentMgr;
	private NetworkManager _networkMgr;
	private StorageManager _storageMgr;
	private HighAvailabilityManager _haMgr;
	private EventDao _eventDao;
        @Inject ConfigurationDao _configDao;
	@Inject ServiceOfferingDao _offeringDao;
        private int _networkRate;
        private int _multicastRate;
	private IpAddrAllocator _IpAllocator;

	private ConsoleProxyListener _listener;

	private ServiceOfferingVO _serviceOffering;
	private VMTemplateVO _template;

	private AsyncJobManager _asyncMgr;

	private final ScheduledExecutorService _capacityScanScheduler = Executors
			.newScheduledThreadPool(1, new NamedThreadFactory("CP-Scan"));
	private final ExecutorService _requestHandlerScheduler = Executors
			.newCachedThreadPool(new NamedThreadFactory("Request-handler"));

	private long _capacityScanInterval = DEFAULT_CAPACITY_SCAN_INTERVAL;
	private int _capacityPerProxy = ConsoleProxyManager.DEFAULT_PROXY_CAPACITY;
	private int _standbyCapacity = ConsoleProxyManager.DEFAULT_STANDBY_CAPACITY;

	private int _proxyRamSize;
	private int _find_host_retry = DEFAULT_FIND_HOST_RETRY_COUNT;
	private int _ssh_retry;
	private int _ssh_sleep;
	private boolean _use_lvm;
	private boolean _use_storage_vm;

	private String _domain;
	private String _instance;

	// private String _privateNetmask;
	private int _proxyCmdPort = DEFAULT_PROXY_CMD_PORT;
	private int _proxySessionTimeoutValue = DEFAULT_PROXY_SESSION_TIMEOUT;
	private boolean _sslEnabled = false;

	private final GlobalLock _capacityScanLock = GlobalLock
			.getInternLock(getCapacityScanLockName());
	private final GlobalLock _allocProxyLock = GlobalLock
			.getInternLock(getAllocProxyLockName());

	public ConsoleProxyVO assignProxy(final long dataCenterId, final long vmId) {

		final Pair<ConsoleProxyManagerImpl, ConsoleProxyVO> result = new Pair<ConsoleProxyManagerImpl, ConsoleProxyVO>(
				this, null);

		_requestHandlerScheduler.execute(new Runnable() {
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
		return result.second();
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
			s_logger.error("Unable to acquire synchronization lock to get/allocate proxy resource for vm :"
				+ vmId + ". Previous console proxy allocation is taking too long");
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
					proxy = startProxy(proxyVmId, 0);
					
					if (proxy == null) {
						//
						// We had a situation with multi-pod configuration, where
						// storage allocation of the console proxy VM may succeed, but later-on starting of it
						// may fail because of running out of computing resource (CPU/memory). We
						// currently don't support moving storage to another pod on the fly, to deal
						// with the situation we will destroy this proxy VM and let it the whole proxy VM
						// creation process re-start again, by hoping that new storage and computing
						// resource may be allocated and assigned in another pod
						//
						if (s_logger.isInfoEnabled())
							s_logger.info("Unable to start console proxy, proxy vm Id : " + proxyVmId + " will recycle it and restart a new one");
						destroyProxy(proxyVmId, 0);
						return null;
					} else {
						if (s_logger.isTraceEnabled())
							s_logger.trace("Console proxy " + proxy.getName() + " is started");

						// if it is a new assignment or a changed assignment, update the
						// record
						if (vm.getProxyId() == null || vm.getProxyId().longValue() != proxy.getId().longValue())
							_instanceDao.updateProxyId(vmId, proxy.getId(), DateUtil.currentGMTTime());

						proxy.setSslEnabled(_sslEnabled);
						if (_sslEnabled)
							proxy.setPort(443);
						else
							proxy.setPort(80);
						return proxy;
					}
				} finally {
					proxyLock.unlock();
				}
			} else {
				s_logger.error("Unable to acquire synchronization lock to start console proxy "
					+ proxyVmId + " for vm: " + vmId + ". It takes too long to start the proxy");
				
				return null;
			}
		} finally {
			proxyLock.releaseRef();
		}
	}

	private ConsoleProxyVO getOrAllocProxyResource(long dataCenterId,
			long vmId, Boolean[] proxyFromStoppedPool) {
		ConsoleProxyVO proxy = null;
		VMInstanceVO vm = this._instanceDao.findById(vmId);

		if (vm != null && vm.getState() != State.Running) {
			if (s_logger.isInfoEnabled())
				s_logger.info("Detected that vm : " + vmId + " is not currently at running state, we will fail the proxy assignment for it");
			return null;
		}

		if (vm != null && vm.getProxyId() != null) {
			proxy = _consoleProxyDao.findById(vm.getProxyId());

			if (proxy != null) {
				if (!isInAssignableState(proxy)) {
					if (s_logger.isInfoEnabled())
						s_logger.info("A previous assigned proxy is not assignable now, reassign console proxy for user vm : " + vmId);
					proxy = null;
				} else {
					// Use proxy actual load info to determine allocation
					// instead of static load (assigned running VMs)
					// Proxy load info will be reported to management server at
					// 5-second interval, the load info used here
					// may be temporarily out of sync with its actual load info
					if (_consoleProxyDao.getProxyActiveLoad(proxy.getId()) < _capacityPerProxy
							|| hasPreviousSession(proxy, vm)) {
						if (s_logger.isTraceEnabled())
							s_logger.trace("Assign previous allocated console proxy for user vm : " + vmId);

						if (proxy.getActiveSession() >= _capacityPerProxy)
							s_logger.warn("Assign overloaded proxy to user VM as previous session exists, user vm : " + vmId);
					} else {
						proxy = null;
					}
				}
			}
		}

		if (proxy == null)
			proxy = assignProxyFromRunningPool(dataCenterId);

		if (proxy == null) {
			if (s_logger.isInfoEnabled())
				s_logger.info("No running console proxy is available, check to see if we can bring up a stopped one for data center : " + dataCenterId);

			proxy = assignProxyFromStoppedPool(dataCenterId);
			if (proxy == null) {
				if (s_logger.isInfoEnabled())
					s_logger.info("No stopped console proxy is available, need to allocate a new console proxy for data center : " + dataCenterId);

				proxy = startNew(dataCenterId);
			} else {
				if (s_logger.isInfoEnabled())
					s_logger.info("Found a stopped console proxy, bring it up to running pool. proxy vm id : "
							+ proxy.getId()
							+ ", data center : "
							+ dataCenterId);

				proxyFromStoppedPool[0] = new Boolean(true);
			}
		}

		return proxy;
	}

	private static boolean isInAssignableState(ConsoleProxyVO proxy) {
		// console proxies that are in states of being able to serve user VM
		State state = proxy.getState();
		if (state == State.Running || state == State.Starting || state == State.Creating || state == State.Migrating)
			return true;

		return false;
	}

	private boolean hasPreviousSession(ConsoleProxyVO proxy, VMInstanceVO vm) {

		ConsoleProxyStatus status = null;
		try {
			GsonBuilder gb = new GsonBuilder();
			gb.setVersion(1.3);
			Gson gson = gb.create();

			byte[] details = proxy.getSessionDetails();
			status = gson.fromJson(details != null ? new String(details,
					Charset.forName("US-ASCII")) : null,
					ConsoleProxyStatus.class);
		} catch (Throwable e) {
			s_logger.warn("Unable to parse proxy session details : "
					+ proxy.getSessionDetails());
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
				if (taggedVmId == vm.getId().longValue())
					return true;
			}

			//
			// even if we are not in the list, it may because we haven't
			// received load-update yet
			// wait until session time
			//
			if (DateUtil.currentGMTTime().getTime() - vm.getProxyAssignTime().getTime() < _proxySessionTimeoutValue)
				return true;

			return false;
		} else {
			s_logger.error("No proxy load info on an overloaded proxy ?");
			return false;
		}
	}

	@Override
	public ConsoleProxyVO startProxy(long proxyVmId, long startEventId) {
		try {
			return start(proxyVmId, startEventId);
		} catch (StorageUnavailableException e) {
			s_logger.warn("Exception while trying to start console proxy", e);
			return null;
		} catch (InsufficientCapacityException e) {
			s_logger.warn("Exception while trying to start console proxy", e);
			return null;
		} catch (ConcurrentOperationException e) {
			s_logger.warn("Exception while trying to start console proxy", e);
			return null;
		}
	}
	
	@Override
	@DB
	public ConsoleProxyVO start(long proxyId, long startEventId)
			throws StorageUnavailableException, InsufficientCapacityException,
			ConcurrentOperationException {

		AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
		if (asyncExecutor != null) {
			AsyncJobVO job = asyncExecutor.getJob();

			if (s_logger.isInfoEnabled())
				s_logger.info("Start console proxy " + proxyId + ", update async job-" + job.getId());
			_asyncMgr.updateAsyncJobAttachment(job.getId(), "console_proxy", proxyId);
		}

		ConsoleProxyVO proxy = _consoleProxyDao.findById(proxyId);
		if (proxy == null || proxy.getRemoved() != null) {
			s_logger.debug("proxy is not found: " + proxyId);
			return null;
		}
/*
 		// don't insert event here, it may be called multiple times!
		saveStartedEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM,
				EventTypes.EVENT_PROXY_START,
				"Starting console proxy with Id: " + proxyId, startEventId);
*/				

		if (s_logger.isTraceEnabled()) {
			s_logger.trace("Starting console proxy if it is not started, proxy vm id : " + proxyId);
		}

		for (int i = 0; i < 2; i++) {

			State state = proxy.getState();

			if (state == State.Starting /* || state == State.Migrating */) {
				if (s_logger.isDebugEnabled())
					s_logger.debug("Waiting console proxy to be ready, proxy vm id : " + proxyId + " proxy VM state : " + state.toString());

				if (proxy.getPrivateIpAddress() == null || connect(proxy.getPrivateIpAddress(), _proxyCmdPort) != null) {
					if (proxy.getPrivateIpAddress() == null)
						s_logger.warn("Retruning a proxy that is being started but private IP has not been allocated yet, proxy vm id : " + proxyId);
					else
						s_logger.warn("Waiting console proxy to be ready timed out, proxy vm id : " + proxyId);

					// TODO, it is very tricky here, if the startup process
					// takes too long and it timed out here,
					// we may give back a proxy that is not fully ready for
					// functioning
				}
				return proxy;
			}

			if (state == State.Running) {
				if (s_logger.isTraceEnabled())
					s_logger.trace("Console proxy is already started: " + proxy.getName());
				return proxy;
			}

			DataCenterVO dc = _dcDao.findById(proxy.getDataCenterId());
			HostPodVO pod = _podDao.findById(proxy.getPodId());
			StoragePoolVO sp = _storageMgr.getStoragePoolForVm(proxy.getId());

			HashSet<Host> avoid = new HashSet<Host>();
			HostVO routingHost = (HostVO) _agentMgr.findHost(Host.Type.Routing,
					dc, pod, sp, _serviceOffering, _template, proxy, null,
					avoid);

			if (routingHost == null) {
				if (s_logger.isDebugEnabled()) {
					s_logger.debug("Unable to find a routing host for " + proxy.toString());
					continue;
				}
			}
			// to ensure atomic state transition to Starting state
			if (!_consoleProxyDao.updateIf(proxy, Event.StartRequested, routingHost.getId())) {
				if (s_logger.isDebugEnabled()) {
					ConsoleProxyVO temp = _consoleProxyDao.findById(proxyId);
					s_logger.debug("Unable to start console proxy " + proxy.getName() + " because it is not in a startable state : "
							+ ((temp != null) ? temp.getState().toString() : "null"));
				}
				continue;
			}

			try {
				Answer answer = null;
				int retry = _find_host_retry;

				// Console proxy VM will be running at routing hosts as routing
				// hosts have public access to outside network
				do {
					if (s_logger.isDebugEnabled()) {
						s_logger.debug("Trying to start console proxy on host " + routingHost.getName());
					}

					String privateIpAddress = allocPrivateIpAddress(
							proxy.getDataCenterId(), routingHost.getPodId(),
							proxy.getId(), proxy.getPrivateMacAddress());
					if (privateIpAddress == null && (_IpAllocator != null && !_IpAllocator.exteralIpAddressAllocatorEnabled())) {
						s_logger.debug("Not enough ip addresses in " + routingHost.getPodId());
						avoid.add(routingHost);
						continue;
					}
					
					proxy.setPrivateIpAddress(privateIpAddress);
					String guestIpAddress = _dcDao.allocateLinkLocalPrivateIpAddress(proxy.getDataCenterId(), routingHost.getPodId(), proxy.getId());
					proxy.setGuestIpAddress(guestIpAddress);

					_consoleProxyDao.updateIf(proxy, Event.OperationRetry,
							routingHost.getId());
					proxy = _consoleProxyDao.findById(proxy.getId());

					List<VolumeVO> vols = _storageMgr.prepare(proxy,
							routingHost);
					if (vols == null) {
						s_logger.debug("Unable to prepare storage for " + routingHost);
						avoid.add(routingHost);
						continue;
					}

					// _storageMgr.share(proxy, vols, null, true);

					// carry the console proxy port info over so that we don't
					// need to configure agent on this
					StartConsoleProxyCommand cmdStart = new StartConsoleProxyCommand(_networkRate, _multicastRate,
							_proxyCmdPort, proxy, proxy.getName(), "", vols,
							Integer.toString(_consoleProxyPort), 
							Integer.toString(_consoleProxyUrlPort),
							_mgmt_host, _mgmt_port, _sslEnabled);

					if (s_logger.isDebugEnabled())
						s_logger.debug("Sending start command for console proxy " + proxy.getName() + " to " + routingHost.getName());
					try {
						answer = _agentMgr.send(routingHost.getId(), cmdStart);
						
						s_logger.debug("StartConsoleProxy Answer: " + (answer != null ? answer : "null"));

						if (s_logger.isDebugEnabled())
							s_logger.debug("Received answer on starting console proxy " + proxy.getName() + " on " + routingHost.getName());

						if (answer != null && answer.getResult()) {
							if (s_logger.isDebugEnabled()) {
								s_logger.debug("Console proxy " + proxy.getName() + " started on "
										+ routingHost.getName());
							}

							if (answer instanceof StartConsoleProxyAnswer) {
								StartConsoleProxyAnswer rAnswer = (StartConsoleProxyAnswer) answer;
								if (rAnswer.getPrivateIpAddress() != null) {
									proxy.setPrivateIpAddress(rAnswer.getPrivateIpAddress());
								}
								if (rAnswer.getPrivateMacAddress() != null) {
									proxy.setPrivateMacAddress(rAnswer.getPrivateMacAddress());
								}
							}

							final EventVO event = new EventVO();
							event.setUserId(User.UID_SYSTEM);
							event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
							event.setType(EventTypes.EVENT_PROXY_START);
							event.setLevel(EventVO.LEVEL_INFO);
							event.setStartId(startEventId);
							event.setDescription("Console proxy started - " + proxy.getName());
							_eventDao.persist(event);
							break;
						}
						s_logger.debug("Unable to start " + proxy.toString() + " on host " + routingHost.toString() + " due to " + answer.getDetails());
					} catch (OperationTimedoutException e) {
						if (e.isActive()) {
							s_logger.debug("Unable to start vm " + proxy.getName()
									+ " due to operation timed out and it is active so scheduling a restart.");
							_haMgr.scheduleRestart(proxy, true);
							return null;
						}
					} catch (AgentUnavailableException e) {
						s_logger.debug("Agent " + routingHost.toString() + " was unavailable to start VM "
							+ proxy.getName());
					}

					avoid.add(routingHost);
					proxy.setPrivateIpAddress(null);
					freePrivateIpAddress(privateIpAddress, proxy.getDataCenterId(), proxy.getId());
					proxy.setGuestIpAddress(null);
					_dcDao.releaseLinkLocalPrivateIpAddress(guestIpAddress, proxy.getDataCenterId(), proxy.getId());
					_storageMgr.unshare(proxy, vols, routingHost);
				} while (--retry > 0 && (routingHost = (HostVO) _agentMgr.findHost(Host.Type.Routing, dc, pod, sp,
						_serviceOffering, _template, proxy, null, avoid)) != null);
				if (routingHost == null || retry <= 0) {

					SubscriptionMgr.getInstance().notifySubscribers(
							ConsoleProxyManager.ALERT_SUBJECT,
							this, 
							new ConsoleProxyAlertEventArgs(
								ConsoleProxyAlertEventArgs.PROXY_START_FAILURE,
								proxy.getDataCenterId(), proxy.getId(), proxy,
								"Unable to find a routing host to run")
							);

					final EventVO event = new EventVO();
					event.setUserId(User.UID_SYSTEM);
					event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
					event.setType(EventTypes.EVENT_PROXY_START);
					event.setLevel(EventVO.LEVEL_ERROR);
					event.setStartId(startEventId);
					event.setDescription("Starting console proxy failed due to unable to find a host - " + proxy.getName());
					_eventDao.persist(event);
					throw new ExecutionException("Couldn't find a routingHost to run console proxy");
				}

				_consoleProxyDao.updateIf(proxy, Event.OperationSucceeded, routingHost.getId());
				if (s_logger.isDebugEnabled()) {
					s_logger.debug("Console proxy is now started, vm id : " + proxy.getId());
				}

				// If starting the console proxy failed due to the external
				// firewall not being reachable, send an alert.
				if (answer != null && answer.getDetails() != null
						&& answer.getDetails().equals("firewall")) {

					SubscriptionMgr.getInstance().notifySubscribers(
						ConsoleProxyManager.ALERT_SUBJECT,
						this,
						new ConsoleProxyAlertEventArgs(
							ConsoleProxyAlertEventArgs.PROXY_FIREWALL_ALERT,
							proxy.getDataCenterId(), proxy
							.getId(), proxy, null)
						);
				}

				SubscriptionMgr.getInstance().notifySubscribers(
					ConsoleProxyManager.ALERT_SUBJECT,
					this,
					new ConsoleProxyAlertEventArgs(
						ConsoleProxyAlertEventArgs.PROXY_UP, proxy.getDataCenterId(), proxy.getId(),
						proxy, null)
					);

				return proxy;
			} catch (Throwable thr) {
				s_logger.warn("Unexpected exception: ", thr);

				SubscriptionMgr.getInstance().notifySubscribers(
						ConsoleProxyManager.ALERT_SUBJECT,
						this,
						new ConsoleProxyAlertEventArgs(
							ConsoleProxyAlertEventArgs.PROXY_START_FAILURE,
							proxy.getDataCenterId(), proxy.getId(), proxy,
							"Unexpected exception: " + thr.getMessage()));

/*				
				final EventVO event = new EventVO();
				event.setUserId(User.UID_SYSTEM);
				event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
				event.setType(EventTypes.EVENT_PROXY_START);
				event.setLevel(EventVO.LEVEL_ERROR);
				event.setStartId(startEventId);
				event.setDescription("Starting console proxy failed due to unhandled exception - "
					+ proxy.getName());
				_eventDao.persist(event);
*/				

				Transaction txn = Transaction.currentTxn();
				try {
					txn.start();
					String privateIpAddress = proxy.getPrivateIpAddress();
					if (privateIpAddress != null) {
						proxy.setPrivateIpAddress(null);
						freePrivateIpAddress(privateIpAddress, proxy.getDataCenterId(), proxy.getId());
					}
					proxy.setStorageIp(null);
					_consoleProxyDao.updateIf(proxy, Event.OperationFailed, null);
					txn.commit();
				} catch (Exception e) {
					s_logger.error("Caught exception during error recovery");
				}

				if (thr instanceof StorageUnavailableException) {
					throw (StorageUnavailableException) thr;
				} else if (thr instanceof ConcurrentOperationException) {
					throw (ConcurrentOperationException) thr;
				} else if (thr instanceof ExecutionException) {
					s_logger.error("Error while starting console proxy due to " + thr.getMessage());
				} else {
					s_logger.error("Error while starting console proxy ", thr);
				}
				return null;
			}
		}

		s_logger.warn("Starting console proxy encounters non-startable situation");
		return null;
	}

	public ConsoleProxyVO assignProxyFromRunningPool(long dataCenterId) {

		if (s_logger.isTraceEnabled())
			s_logger.trace("Assign console proxy from running pool for request from data center : " + dataCenterId);

		ConsoleProxyAllocator allocator = getCurrentAllocator();
		assert (allocator != null);
		List<ConsoleProxyVO> runningList = _consoleProxyDao.getProxyListInStates(dataCenterId, State.Running);
		if (runningList != null && runningList.size() > 0) {
			if (s_logger.isTraceEnabled()) {
				s_logger.trace("Running proxy pool size : " + runningList.size());
				for (ConsoleProxyVO proxy : runningList)
					s_logger.trace("Running proxy instance : " + proxy.getName());
			}

			List<Pair<Long, Integer>> l = _consoleProxyDao.getProxyLoadMatrix();
			Map<Long, Integer> loadInfo = new HashMap<Long, Integer>();
			if (l != null) {
				for (Pair<Long, Integer> p : l) {
					loadInfo.put(p.first(), p.second());

					if (s_logger.isTraceEnabled()) {
						s_logger.trace("Running proxy instance allocation load { proxy id : "
								+ p.first()
								+ ", load : "
								+ p.second()
								+ "}");
					}
				}
			}
			return allocator.allocProxy(runningList, loadInfo, dataCenterId);
		} else {
			if (s_logger.isTraceEnabled())
				s_logger.trace("Empty running proxy pool for now in data center : " + dataCenterId);
		}
		return null;
	}

	public ConsoleProxyVO assignProxyFromStoppedPool(long dataCenterId) {
		List<ConsoleProxyVO> l = _consoleProxyDao.getProxyListInStates(
				dataCenterId, State.Creating, State.Starting, State.Stopped,
				State.Migrating);
		if (l != null && l.size() > 0)
			return l.get(0);

		return null;
	}

	public ConsoleProxyVO startNew(long dataCenterId) {
		if (s_logger.isDebugEnabled())
			s_logger.debug("Assign console proxy from a newly started instance for request from data center : " + dataCenterId);

		Map<String, Object> context = createProxyInstance(dataCenterId);

		long proxyVmId = (Long) context.get("proxyVmId");
		if (proxyVmId == 0) {
			if (s_logger.isTraceEnabled())
				s_logger.trace("Creating proxy instance failed, data center id : " + dataCenterId);

			// release critical system resource on failure
			if (context.get("publicIpAddress") != null)
				freePublicIpAddress((String) context.get("publicIpAddress"), dataCenterId, 0);

			return null;
		}

		ConsoleProxyVO proxy = allocProxyStorage(dataCenterId, proxyVmId);
		if (proxy != null) {
			SubscriptionMgr.getInstance().notifySubscribers(
					ConsoleProxyManager.ALERT_SUBJECT,
					this,
					new ConsoleProxyAlertEventArgs(
							ConsoleProxyAlertEventArgs.PROXY_CREATED,
							dataCenterId, proxy.getId(), proxy, null));
			return proxy;
		} else {
			if (s_logger.isDebugEnabled())
				s_logger.debug("Unable to allocate console proxy storage, remove the console proxy record from DB, proxy id: " + proxyVmId);

			SubscriptionMgr.getInstance().notifySubscribers(
					ConsoleProxyManager.ALERT_SUBJECT,
					this,
					new ConsoleProxyAlertEventArgs(
							ConsoleProxyAlertEventArgs.PROXY_CREATE_FAILURE,
							dataCenterId, proxyVmId, null,
							"Unable to allocate storage"));

			destroyProxyDBOnly(proxyVmId);
		}
		return null;
	}

	@DB
	protected Map<String, Object> createProxyInstance(long dataCenterId) {

		Map<String, Object> context = new HashMap<String, Object>();
		String publicIpAddress = null;

		Transaction txn = Transaction.currentTxn();
		try {
			DataCenterVO dc = _dcDao.findById(dataCenterId);
			assert (dc != null);
			context.put("dc", dc);

			// this will basically allocate the pod based on data center id as
			// we use system user id here
			Set<Long> avoidPods = new HashSet<Long>();
			Pair<HostPodVO, Long> pod = null;
			networkInfo publicIpAndVlan = null;
			
			// About MAC address allocation
			// MAC address used by User VM is inherited from DomR MAC address,
			// with the least 16 bits overrided. to avoid
			// potential conflicts, domP will mask bit 31
			//
			String[] macAddresses = _dcDao.getNextAvailableMacAddressPair(dataCenterId, (1L << 31));
			String privateMacAddress = macAddresses[0];
			String publicMacAddress = macAddresses[1];
			macAddresses = _dcDao.getNextAvailableMacAddressPair(dataCenterId, (1L << 31));
			String guestMacAddress = macAddresses[0];
			while ((pod = _agentMgr.findPod(_template, _serviceOffering, dc,
					Account.ACCOUNT_ID_SYSTEM, avoidPods)) != null) {
				publicIpAndVlan = allocPublicIpAddress(dataCenterId, pod.first().getId(), publicMacAddress);
				if (publicIpAndVlan == null) {
					s_logger.warn("Unable to allocate public IP address for console proxy vm in data center : "
						+ dataCenterId + ", pod=" + pod.first().getId());
					avoidPods.add(pod.first().getId());
				} else {
					break;
				}
			}

			if (pod == null || publicIpAndVlan == null) {
				s_logger.warn("Unable to allocate pod for console proxy vm in data center : " + dataCenterId);

				context.put("proxyVmId", (long) 0);
				return context;
			}
			
			long id = _consoleProxyDao.getNextInSequence(Long.class, "id");

			context.put("publicIpAddress", publicIpAndVlan._ipAddr);
			context.put("pod", pod);
			if (s_logger.isDebugEnabled()) {
				s_logger.debug("Pod allocated " + pod.first().getName());
			}

			String cidrNetmask = NetUtils.getCidrNetmask(pod.first().getCidrSize());

			// Find the VLAN ID, VLAN gateway, and VLAN netmask for
			// publicIpAddress
			publicIpAddress = publicIpAndVlan._ipAddr;
			
			String vlanGateway = publicIpAndVlan._gateWay;
			String vlanNetmask = publicIpAndVlan._netMask;

			txn.start();
			ConsoleProxyVO proxy;
			String name = VirtualMachineName.getConsoleProxyName(id, _instance).intern();
			proxy = new ConsoleProxyVO(id, name, State.Creating, guestMacAddress, null, NetUtils.getLinkLocalNetMask(),
					privateMacAddress, null, cidrNetmask, _template.getId(),
					_template.getGuestOSId(), publicMacAddress,
					publicIpAddress, vlanNetmask, publicIpAndVlan._vlanDbId, publicIpAndVlan._vlanid, pod.first().getId(), dataCenterId,
					vlanGateway, null, dc.getDns1(), dc.getDns2(), _domain,
					_proxyRamSize, 0);
			
			proxy.setLastHostId(pod.second());
			proxy = _consoleProxyDao.persist(proxy);
			long proxyVmId = proxy.getId();

			final EventVO event = new EventVO();
			event.setUserId(User.UID_SYSTEM);
			event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
			event.setType(EventTypes.EVENT_PROXY_CREATE);
			event.setLevel(EventVO.LEVEL_INFO);
			event.setDescription("New console proxy created - "
					+ proxy.getName());
			_eventDao.persist(event);
			txn.commit();

			context.put("proxyVmId", proxyVmId);
			return context;
		} catch (Throwable e) {
			s_logger.error("Unexpected exception : ", e);

			context.put("proxyVmId", (long) 0);
			return context;
		}
	}

	@DB
	protected ConsoleProxyVO allocProxyStorage(long dataCenterId, long proxyVmId) {
		ConsoleProxyVO proxy = _consoleProxyDao.findById(proxyVmId);
		assert (proxy != null);

		DataCenterVO dc = _dcDao.findById(dataCenterId);
		HostPodVO pod = _podDao.findById(proxy.getPodId());
		final AccountVO account = _accountDao.findById(Account.ACCOUNT_ID_SYSTEM);

		try {
			List<VolumeVO> vols = _storageMgr.create(account, proxy, _template,
					dc, pod, _serviceOffering, null);
			if (vols == null) {
				s_logger.error("Unable to alloc storage for console proxy");
				return null;
			}

			Transaction txn = Transaction.currentTxn();
			txn.start();

			// update pool id
			ConsoleProxyVO vo = _consoleProxyDao.findById(proxy.getId());
			_consoleProxyDao.update(proxy.getId(), vo);

			// kick the state machine
			_consoleProxyDao.updateIf(proxy, Event.OperationSucceeded, null);

			txn.commit();
			return proxy;
		} catch (StorageUnavailableException e) {
			s_logger.error("Unable to alloc storage for console proxy: ", e);
			return null;
		} catch (ExecutionException e) {
			s_logger.error("Unable to alloc storage for console proxy: ", e);
			return null;
		}
	}

	private networkInfo allocPublicIpAddress(long dcId, long podId, String macAddr) {
		
		if (_IpAllocator != null && _IpAllocator.exteralIpAddressAllocatorEnabled()) {
			IpAddrAllocator.IpAddr ip = _IpAllocator.getPublicIpAddress(macAddr, dcId, podId);
			networkInfo net = new networkInfo(ip.ipaddr, ip.netMask, ip.gateway, null, "untagged");
			return net;
		}

		Pair<String, VlanVO> ipAndVlan = _vlanDao.assignIpAddress(dcId,
				Account.ACCOUNT_ID_SYSTEM, DomainVO.ROOT_DOMAIN,
				VlanType.VirtualNetwork, true);

		if (ipAndVlan == null) {
			s_logger.debug("Unable to get public ip address (type=Virtual) for console proxy vm for data center  : " + dcId);
			ipAndVlan = _vlanDao.assignPodDirectAttachIpAddress(dcId, podId,
					Account.ACCOUNT_ID_SYSTEM, DomainVO.ROOT_DOMAIN);
			if (ipAndVlan == null)
				s_logger.debug("Unable to get public ip address (type=DirectAttach) for console proxy vm for data center  : " + dcId);
		}
		if (ipAndVlan != null) {
			VlanVO vlan = ipAndVlan.second();
			networkInfo net = new networkInfo(ipAndVlan.first(), vlan.getVlanNetmask(), vlan.getVlanGateway(), vlan.getId(), vlan.getVlanId());
			return net;
		}
		return null;
	}

	private String allocPrivateIpAddress(Long dcId, Long podId, Long proxyId, String macAddr) {
		if (_IpAllocator != null && _IpAllocator.exteralIpAddressAllocatorEnabled()) {
			return _IpAllocator.getPrivateIpAddress(macAddr, dcId, podId).ipaddr;
		} else {
		return _dcDao.allocatePrivateIpAddress(dcId, podId, proxyId);
		}
	}
	
	private void freePrivateIpAddress(String ipAddress, Long dcId, Long podId) {
		if (_IpAllocator != null && _IpAllocator.exteralIpAddressAllocatorEnabled()) {
			 _IpAllocator.releasePrivateIpAddress(ipAddress, dcId, podId);
		} else {
			_dcDao.releasePrivateIpAddress(ipAddress, dcId, podId);
		}
	}
	
	private void freePublicIpAddress(String ipAddress, long dcId, long podId) {
		if (_IpAllocator != null && _IpAllocator.exteralIpAddressAllocatorEnabled()) {
			 _IpAllocator.releasePublicIpAddress(ipAddress, dcId, podId);
		} else {
			_ipAddressDao.unassignIpAddress(ipAddress);
		}
	}

	private ConsoleProxyAllocator getCurrentAllocator() {

		// for now, only one adapter is supported
		Enumeration<ConsoleProxyAllocator> it = _consoleProxyAllocators.enumeration();
		if (it.hasMoreElements())
			return it.nextElement();

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
		if (answer.getDetails() == null)
			return;

		ConsoleProxyStatus status = null;
		try {
			GsonBuilder gb = new GsonBuilder();
			gb.setVersion(1.3);
			Gson gson = gb.create();
			status = gson.fromJson(answer.getDetails(),
					ConsoleProxyStatus.class);
		} catch (Throwable e) {
			s_logger.warn("Unable to parse load info from proxy, proxy vm id : "
							+ answer.getProxyVmId()
							+ ", info : "
							+ answer.getDetails());
		}

		if (status != null) {
			int count = 0;
			if (status.getConnections() != null)
				count = status.getConnections().length;

			byte[] details = null;
			if (answer.getDetails() != null)
				details = answer.getDetails().getBytes(Charset.forName("US-ASCII"));
			_consoleProxyDao.update(answer.getProxyVmId(), count, DateUtil
				.currentGMTTime(), details);
		} else {
			if (s_logger.isTraceEnabled())
				s_logger.trace("Unable to get console proxy load info, id : "
						+ answer.getProxyVmId());

			_consoleProxyDao.update(answer.getProxyVmId(), 0, DateUtil.currentGMTTime(), null);
			// TODO : something is wrong with the VM, restart it?
		}
	}

	public void onLoadReport(ConsoleProxyLoadReportCommand cmd) {
		if (cmd.getLoadInfo() == null)
			return;

		ConsoleProxyStatus status = null;
		try {
			GsonBuilder gb = new GsonBuilder();
			gb.setVersion(1.3);
			Gson gson = gb.create();
			status = gson.fromJson(cmd.getLoadInfo(), ConsoleProxyStatus.class);
		} catch (Throwable e) {
			s_logger.warn("Unable to parse load info from proxy, proxy vm id : "
					+ cmd.getProxyVmId()
					+ ", info : "
					+ cmd.getLoadInfo());
		}

		if (status != null) {
			int count = 0;
			if (status.getConnections() != null)
				count = status.getConnections().length;

			byte[] details = null;
			if (cmd.getLoadInfo() != null)
				details = cmd.getLoadInfo().getBytes(
						Charset.forName("US-ASCII"));
			_consoleProxyDao.update(cmd.getProxyVmId(), count, DateUtil
					.currentGMTTime(), details);
		} else {
			if (s_logger.isTraceEnabled())
				s_logger.trace("Unable to get console proxy load info, id : "
						+ cmd.getProxyVmId());

			_consoleProxyDao.update(cmd.getProxyVmId(), 0, DateUtil
					.currentGMTTime(), null);
		}
	}

	public AgentControlAnswer onConsoleAccessAuthentication(ConsoleAccessAuthenticationCommand cmd) {
		long vmId = 0;
		
		String ticketInUrl = cmd.getTicket();
		if(ticketInUrl == null) {
			s_logger.error("No access ticket found, you could be running an old console proxy. vmId: " + cmd.getVmId());
			return new ConsoleAccessAuthenticationAnswer(cmd, false);
		}
		
		String ticket = ConsoleProxyServlet.genAccessTicket(cmd.getHost(), cmd.getPort(), cmd.getSid(), cmd.getVmId());
		if(!ticket.startsWith(ticketInUrl)) {
			Date now = new Date();
			
			// considering of minute round-up
			String minuteEarlyTicket = ConsoleProxyServlet.genAccessTicket(cmd.getHost(), cmd.getPort(), cmd.getSid(), cmd.getVmId(), 
				new Date(now.getTime() - 60*1000));
			if(!minuteEarlyTicket.startsWith(ticketInUrl)) {
				s_logger.error("Access ticket expired or has been modified. vmId: " + cmd.getVmId());
				return new ConsoleAccessAuthenticationAnswer(cmd, false);
			}
		}

		if (cmd.getVmId() != null && cmd.getVmId().isEmpty()) {
			if (s_logger.isTraceEnabled())
				s_logger.trace("Invalid vm id sent from proxy(happens when proxy session has terminated)");
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
			if (s_logger.isInfoEnabled())
				s_logger.info("Console proxy agent is connected. proxy: " + host.getName());
			
			/*update public/private ip address*/
			if (_IpAllocator != null && _IpAllocator.exteralIpAddressAllocatorEnabled()) {
				try {
					ConsoleProxyVO console = findConsoleProxyByHost(host);
					if (console == null) {
						s_logger.debug("Can't find console proxy ");
						return;
					}
					console.setPrivateIpAddress(cmd.getPrivateIpAddress());
					console.setPrivateNetmask(cmd.getPrivateNetmask());
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
		if (state == com.cloud.host.Status.Alert
				|| state == com.cloud.host.Status.Disconnected) {
			// be it either in alert or in disconnected state, the agent process
			// may be gone in the VM,
			// we will be reacting to stop the corresponding VM and let the scan
			// process to
			HostVO host = _hostDao.findById(agentId);
			if (host.getType() == Type.ConsoleProxy) {
				String name = host.getName();
				if (s_logger.isInfoEnabled())
					s_logger.info("Console proxy agent disconnected, proxy: " + name);
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
						if (s_logger.isInfoEnabled())
							s_logger.info("Console proxy agent disconnected but corresponding console proxy VM no longer exists in DB, proxy: " + name);
					}
				} else {
					assert (false) : "Invalid console proxy name: " + name;
				}
			}
		}
	}

	private void checkPendingProxyVMs() {
		// drive state to change away from transient states
		List<ConsoleProxyVO> l = _consoleProxyDao
				.getProxyListInStates(State.Creating);
		if (l != null && l.size() > 0) {
			for (ConsoleProxyVO proxy : l) {
				if (proxy.getLastUpdateTime() == null || 
					(proxy.getLastUpdateTime() != null && System.currentTimeMillis() - proxy.getLastUpdateTime().getTime() > 60000)) {
					
					try {
						ConsoleProxyVO readyProxy = null;
						if (_allocProxyLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
							try {
								readyProxy = allocProxyStorage(proxy.getDataCenterId(), proxy.getId());
							} finally {
								_allocProxyLock.unlock();
							}

							if (readyProxy != null) {
								GlobalLock proxyLock = GlobalLock.getInternLock(getProxyLockName(readyProxy.getId()));
								try {
									if (proxyLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
										try {
											readyProxy = start(readyProxy.getId(), 0);
										} finally {
											proxyLock.unlock();
										}
									} else {
										if (s_logger.isInfoEnabled())
											s_logger.info("Unable to acquire synchronization lock to start console proxy : " + readyProxy.getName());
									}
								} finally {
									proxyLock.releaseRef();
								}
							}
						} else {
							if (s_logger.isInfoEnabled())
								s_logger.info("Unable to acquire synchronization lock to allocate proxy storage, wait for next turn");
						}
					} catch (StorageUnavailableException e) {
						s_logger.warn("Storage unavailable", e);
					} catch (InsufficientCapacityException e) {
						s_logger.warn("insuffiient capacity", e);
					} catch (ConcurrentOperationException e) {
						s_logger.debug("Concurrent operation: "
								+ e.getMessage());
					}
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
				if (s_logger.isTraceEnabled())
					s_logger.trace("Begin console proxy capacity scan");

				Map<Long, ZoneHostInfo> zoneHostInfoMap = getZoneHostInfo();
				if (isServiceReady(zoneHostInfoMap)) {
					if (s_logger.isTraceEnabled())
						s_logger.trace("Service is ready, check to see if we need to allocate standby capacity");

					if (!_capacityScanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
						if (s_logger.isTraceEnabled())
							s_logger.trace("Capacity scan lock is used by others, skip and wait for my turn");
						return;
					}

					if (s_logger.isTraceEnabled())
						s_logger.trace("*** Begining capacity scan... ***");

					try {
						checkPendingProxyVMs();

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
						if (listVmCounts != null)
							for (ConsoleProxyLoadInfo info : listVmCounts)
								mapVmCounts.put(info.getId(), info);

						for (ConsoleProxyLoadInfo info : l) {
							if (info.getName().equals(_instance)) {
								ConsoleProxyLoadInfo vmInfo = mapVmCounts.get(info.getId());

								if (!checkCapacity(info, vmInfo != null ? vmInfo : new ConsoleProxyLoadInfo())) {
									if (isZoneReady(zoneHostInfoMap, info.getId())) {
										allocCapacity(info.getId());
									} else {
										if (s_logger.isTraceEnabled())
											s_logger.trace("Zone " + info.getId() + " is not ready to alloc standy console proxy");
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
										if (s_logger.isTraceEnabled())
											s_logger.trace("Zone " + info.getId() + " is not ready to alloc standy console proxy");
									}
								}
							}
						}

						if (s_logger.isTraceEnabled())
							s_logger.trace("*** Stop capacity scan ***");
					} finally {
						_capacityScanLock.unlock();
					}

				} else {
					if (s_logger.isTraceEnabled())
						s_logger.trace("Service is not ready for capacity preallocation, wait for next time");
				}

				if (s_logger.isTraceEnabled())
					s_logger.trace("End of console proxy capacity scan");
			}
		};
	}

	private boolean checkCapacity(ConsoleProxyLoadInfo proxyCountInfo,
			ConsoleProxyLoadInfo vmCountInfo) {

		if (proxyCountInfo.getCount() * _capacityPerProxy
				- vmCountInfo.getCount() <= _standbyCapacity)
			return false;

		return true;
	}

	private void allocCapacity(long dataCenterId) {
		if (s_logger.isTraceEnabled())
			s_logger.trace("Allocate console proxy standby capacity for data center : " + dataCenterId);

		boolean proxyFromStoppedPool = false;
		ConsoleProxyVO proxy = assignProxyFromStoppedPool(dataCenterId);
		if (proxy == null) {
			if (s_logger.isInfoEnabled())
				s_logger.info("No stopped console proxy is available, need to allocate a new console proxy");

			if (_allocProxyLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
				try {
					proxy = startNew(dataCenterId);
				} finally {
					_allocProxyLock.unlock();
				}
			} else {
				if (s_logger.isInfoEnabled())
					s_logger.info("Unable to acquire synchronization lock to allocate proxy resource for standby capacity, wait for next scan");
				return;
			}
		} else {
			if (s_logger.isInfoEnabled())
				s_logger.info("Found a stopped console proxy, bring it up to running pool. proxy vm id : " + proxy.getId());
			proxyFromStoppedPool = true;
		}

		if (proxy != null) {
			long proxyVmId = proxy.getId();
			GlobalLock proxyLock = GlobalLock.getInternLock(getProxyLockName(proxyVmId));
			try {
				if (proxyLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
					try {
						proxy = startProxy(proxyVmId, 0);
					} finally {
						proxyLock.unlock();
					}
				} else {
					if (s_logger.isInfoEnabled())
						s_logger.info("Unable to acquire synchronization lock to start proxy for standby capacity, proxy vm id : " + proxy.getId());
					return;
				}
			} finally {
				proxyLock.releaseRef();
			}

			if (proxy == null) {
				if (s_logger.isInfoEnabled())
					s_logger.info("Unable to start console proxy for standby capacity, proxy vm Id : " + proxyVmId
								+ ", will recycle it and start a new one");

				if (proxyFromStoppedPool)
					destroyProxy(proxyVmId, 0);
			} else {
				if (s_logger.isInfoEnabled())
					s_logger.info("Console proxy " + proxy.getName() + " is started");
			}
		}
	}

	public boolean isServiceReady(Map<Long, ZoneHostInfo> zoneHostInfoMap) {
		for (ZoneHostInfo zoneHostInfo : zoneHostInfoMap.values()) {
			if (isZoneHostReady(zoneHostInfo)) {
				if (s_logger.isInfoEnabled())
					s_logger.info("Zone " + zoneHostInfo.getDcId() + " is ready to launch");
				return true;
			}
		}

		return false;
	}

	public boolean isZoneReady(Map<Long, ZoneHostInfo> zoneHostInfoMap,
			long dataCenterId) {
		ZoneHostInfo zoneHostInfo = zoneHostInfoMap.get(dataCenterId);
		if (zoneHostInfo != null && isZoneHostReady(zoneHostInfo)) {
			VMTemplateVO template = _templateDao.findConsoleProxyTemplate();
			HostVO secondaryStorageHost = _storageMgr.getSecondaryStorageHost(dataCenterId);
			boolean templateReady = false;

			if (template != null && secondaryStorageHost != null) {
				VMTemplateHostVO templateHostRef = _vmTemplateHostDao.findByHostTemplate(secondaryStorageHost.getId(), template.getId());
				templateReady = (templateHostRef != null)
						&& (templateHostRef.getDownloadState() == Status.DOWNLOADED);
			}

			if (templateReady) {
				List<Pair<Long, Integer>> l = _consoleProxyDao.getDatacenterStoragePoolHostInfo(dataCenterId, _use_lvm);
				if (l != null && l.size() > 0 && l.get(0).second().intValue() > 0) {
					return true;
				} else {
					if (s_logger.isTraceEnabled())
						s_logger.trace("Primary storage is not ready, wait until it is ready to launch console proxy");
				}
			} else {
				if (s_logger.isTraceEnabled())
					s_logger.trace("Zone host is ready, but console proxy template is not ready");
			}
		}
		return false;
	}

	private boolean isZoneHostReady(ZoneHostInfo zoneHostInfo) {
		int expectedFlags = 0;
		if (_use_storage_vm)
			expectedFlags = RunningHostInfoAgregator.ZoneHostInfo.COMPUTING_HOST_MASK
					| RunningHostInfoAgregator.ZoneHostInfo.ROUTING_HOST_MASK;
		else
			expectedFlags = RunningHostInfoAgregator.ZoneHostInfo.ALL_HOST_MASK;

		return (zoneHostInfo.getFlags() & expectedFlags) == expectedFlags;
	}

	private synchronized Map<Long, ZoneHostInfo> getZoneHostInfo() {
		Date cutTime = DateUtil.currentGMTTime();
		List<RunningHostCountInfo> l = _hostDao.getRunningHostCounts(new Date(cutTime.getTime()
						- ClusterManager.DEFAULT_HEARTBEAT_THRESHOLD));

		RunningHostInfoAgregator aggregator = new RunningHostInfoAgregator();
		if (l.size() > 0)
			for (RunningHostCountInfo countInfo : l)
				aggregator.aggregate(countInfo);

		return aggregator.getZoneHostInfoMap();
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public boolean start() {
		if (s_logger.isInfoEnabled())
			s_logger.info("Start console proxy manager");

		return true;
	}

	@Override
	public boolean stop() {
		if (s_logger.isInfoEnabled())
			s_logger.info("Stop console proxy manager");
		_capacityScanScheduler.shutdownNow();

		try {
			_capacityScanScheduler.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT,
					TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		}

		_capacityScanLock.releaseRef();
		_allocProxyLock.releaseRef();
		return true;
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		if (s_logger.isInfoEnabled())
			s_logger.info("Start configuring console proxy manager : " + name);

		_name = name;

		ComponentLocator locator = ComponentLocator.getCurrentLocator();
		ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
		if (configDao == null) {
			throw new ConfigurationException(
					"Unable to get the configuration dao.");
		}

		Map<String, String> configs = configDao.getConfiguration("management-server", params);

		_proxyRamSize = NumbersUtil.parseInt(configs.get("consoleproxy.ram.size"), DEFAULT_PROXY_VM_RAMSIZE);

		String value = configs.get("start.retry");
		_find_host_retry = NumbersUtil.parseInt(value,
				DEFAULT_FIND_HOST_RETRY_COUNT);

		value = configs.get("consoleproxy.cmd.port");
		_proxyCmdPort = NumbersUtil.parseInt(value, DEFAULT_PROXY_CMD_PORT);

		value = configs.get("consoleproxy.sslEnabled");
		if (value != null && value.equalsIgnoreCase("true"))
			_sslEnabled = true;

		value = configs.get("consoleproxy.capacityscan.interval");
		_capacityScanInterval = NumbersUtil.parseLong(value,
				DEFAULT_CAPACITY_SCAN_INTERVAL);

		_capacityPerProxy = NumbersUtil.parseInt(configs
				.get("consoleproxy.session.max"), DEFAULT_PROXY_CAPACITY);
		_standbyCapacity = NumbersUtil
				.parseInt(configs.get("consoleproxy.capacity.standby"),
						DEFAULT_STANDBY_CAPACITY);
		_proxySessionTimeoutValue = NumbersUtil.parseInt(configs
				.get("consoleproxy.session.timeout"),
				DEFAULT_PROXY_SESSION_TIMEOUT);

		value = configs.get("consoleproxy.port");
		if (value != null)
			_consoleProxyPort = NumbersUtil.parseInt(value,
					ConsoleProxyManager.DEFAULT_PROXY_VNC_PORT);

		value = configs.get("consoleproxy.url.port");
		if (value != null)
			_consoleProxyUrlPort = NumbersUtil.parseInt(value,
					ConsoleProxyManager.DEFAULT_PROXY_URL_PORT);

		value = configs.get("system.vm.use.local.storage");
		if (value != null && value.equalsIgnoreCase("true"))
			_use_lvm = true;

		value = configs.get("secondary.storage.vm");
		if (value != null && value.equalsIgnoreCase("true"))
			_use_storage_vm = true;

		if (s_logger.isInfoEnabled()) {
			s_logger.info("Console proxy max session soft limit : "
					+ _capacityPerProxy);
			s_logger.info("Console proxy standby capacity : "
					+ _standbyCapacity);
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

		Map<String, String> agentMgrConfigs = configDao.getConfiguration(
				"AgentManager", params);
		_mgmt_host = agentMgrConfigs.get("host");
		if (_mgmt_host == null) {
			s_logger.warn("Critical warning! Please configure your management server host address right after you have started your management server and then restart it, otherwise you won't be able to do console access");
		}

		value = agentMgrConfigs.get("port");
		_mgmt_port = NumbersUtil.parseInt(value, 8250);

		_consoleProxyDao = locator.getDao(ConsoleProxyDao.class);
		if (_consoleProxyDao == null) {
			throw new ConfigurationException("Unable to get " + ConsoleProxyDao.class.getName());
		}

		_consoleProxyAllocators = locator.getAdapters(ConsoleProxyAllocator.class);
		if (_consoleProxyAllocators == null || !_consoleProxyAllocators.isSet()) {
			throw new ConfigurationException("Unable to get proxy allocators");
		}

		_dcDao = locator.getDao(DataCenterDao.class);
		if (_dcDao == null) {
			throw new ConfigurationException("Unable to get "
					+ DataCenterDao.class.getName());
		}

		_templateDao = locator.getDao(VMTemplateDao.class);
		if (_templateDao == null) {
			throw new ConfigurationException("Unable to get "
					+ VMTemplateDao.class.getName());
		}

		_ipAddressDao = locator.getDao(IPAddressDao.class);
		if (_ipAddressDao == null) {
			throw new ConfigurationException("Unable to get "
					+ IPAddressDao.class.getName());
		}

		_volsDao = locator.getDao(VolumeDao.class);
		if (_volsDao == null) {
			throw new ConfigurationException("Unable to get "
					+ VolumeDao.class.getName());
		}

		_podDao = locator.getDao(HostPodDao.class);
		if (_podDao == null) {
			throw new ConfigurationException("Unable to get "
					+ HostPodDao.class.getName());
		}

		_hostDao = locator.getDao(HostDao.class);
		if (_hostDao == null) {
			throw new ConfigurationException("Unable to get "
					+ HostDao.class.getName());
		}

		_eventDao = locator.getDao(EventDao.class);
		if (_eventDao == null) {
			throw new ConfigurationException("Unable to get "
					+ EventDao.class.getName());
		}

		_storagePoolDao = locator.getDao(StoragePoolDao.class);
		if (_storagePoolDao == null) {
			throw new ConfigurationException("Unable to find "
					+ StoragePoolDao.class);
		}

		_vmTemplateHostDao = locator.getDao(VMTemplateHostDao.class);
		if (_vmTemplateHostDao == null) {
			throw new ConfigurationException("Unable to get "
					+ VMTemplateHostDao.class.getName());
		}

		_instanceDao = locator.getDao(VMInstanceDao.class);
		if (_instanceDao == null)
			throw new ConfigurationException("Unable to get "
					+ VMInstanceDao.class.getName());

		_capacityDao = locator.getDao(CapacityDao.class);
		if (_capacityDao == null) {
			throw new ConfigurationException("Unable to get "
					+ CapacityDao.class.getName());
		}

		_haDao = locator.getDao(HighAvailabilityDao.class);
		if (_haDao == null) {
			throw new ConfigurationException("Unable to get "
					+ HighAvailabilityDao.class.getName());
		}

		_accountDao = locator.getDao(AccountDao.class);
		if (_accountDao == null) {
			throw new ConfigurationException("Unable to get "
					+ AccountDao.class.getName());
		}

		_vlanDao = locator.getDao(VlanDao.class);
		if (_vlanDao == null) {
			throw new ConfigurationException("Unable to get "
					+ VlanDao.class.getName());
		}

		_agentMgr = locator.getManager(AgentManager.class);
		if (_agentMgr == null) {
			throw new ConfigurationException("Unable to get "
					+ AgentManager.class.getName());
		}

		_networkMgr = locator.getManager(NetworkManager.class);
		if (_networkMgr == null) {
			throw new ConfigurationException("Unable to get "
					+ NetworkManager.class.getName());
		}

		_listener = new ConsoleProxyListener(this);
		_agentMgr.registerForHostEvents(_listener, true, true, false);

		_haMgr = locator.getManager(HighAvailabilityManager.class);
		if (_haMgr == null) {
			throw new ConfigurationException("Unable to get "
					+ HighAvailabilityManager.class.getName());
		}

		_storageMgr = locator.getManager(StorageManager.class);
		if (_storageMgr == null) {
			throw new ConfigurationException("Unable to get "
					+ StorageManager.class.getName());
		}

		_asyncMgr = locator.getManager(AsyncJobManager.class);
		if (_asyncMgr == null) {
			throw new ConfigurationException("Unable to get "
					+ AsyncJobManager.class.getName());
		}
		
		Adapters<IpAddrAllocator> ipAllocators = locator.getAdapters(IpAddrAllocator.class);
		if (ipAllocators != null && ipAllocators.isSet()) {
			Enumeration<IpAddrAllocator> it = ipAllocators.enumeration();
			_IpAllocator = it.nextElement();
		}

		HighAvailabilityManager haMgr = locator.getManager(HighAvailabilityManager.class);
		if (haMgr != null) {
			haMgr.registerHandler(VirtualMachine.Type.ConsoleProxy, this);
		}

		boolean useLocalStorage = Boolean.parseBoolean((String) params.get(Config.SystemVMUseLocalStorage.key()));
        String networkRateStr = _configDao.getValue("network.throttling.rate");
        String multicastRateStr = _configDao.getValue("multicast.throttling.rate");
        _networkRate = ((networkRateStr == null) ? 200 : Integer.parseInt(networkRateStr));
        _multicastRate = ((multicastRateStr == null) ? 10 : Integer.parseInt(multicastRateStr));

		_serviceOffering = new ServiceOfferingVO("Fake Offering For DomP", 1,
				_proxyRamSize, 0, 0, 0, false, null, GuestIpType.Virtualized,
				useLocalStorage, true, null);
		_serviceOffering.setUniqueName("Cloud.com-ConsoleProxy");
		_serviceOffering = _offeringDao.persistSystemServiceOffering(_serviceOffering);
		_template = _templateDao.findConsoleProxyTemplate();
		if (_template == null) {
			throw new ConfigurationException("Unable to find the template for console proxy VMs");
		}

		_capacityScanScheduler.scheduleAtFixedRate(getCapacityScanTask(),
				STARTUP_DELAY, _capacityScanInterval, TimeUnit.MILLISECONDS);

		if (s_logger.isInfoEnabled())
			s_logger.info("Console Proxy Manager is configured.");
		return true;
	}

	protected ConsoleProxyManagerImpl() {
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
	public void completeStartCommand(ConsoleProxyVO vm) {
		_consoleProxyDao.updateIf(vm, Event.AgentReportRunning, vm.getHostId());
	}

	@Override
	public void completeStopCommand(ConsoleProxyVO vm) {
		completeStopCommand(vm, Event.AgentReportStopped);
	}

	@DB
	protected void completeStopCommand(ConsoleProxyVO proxy, Event ev) {
		Transaction txn = Transaction.currentTxn();
		try {
			txn.start();
			String privateIpAddress = proxy.getPrivateIpAddress();
			if (privateIpAddress != null) {
				proxy.setPrivateIpAddress(null);
				freePrivateIpAddress(privateIpAddress, proxy
						.getDataCenterId(), proxy.getId());
			}
			String guestIpAddress = proxy.getGuestIpAddress();
			if (guestIpAddress != null) {
				proxy.setGuestIpAddress(null);
				_dcDao.releaseLinkLocalPrivateIpAddress(guestIpAddress, proxy.getDataCenterId(), proxy.getId());
			}
			proxy.setStorageIp(null);
			if (!_consoleProxyDao.updateIf(proxy, ev, null)) {
				s_logger.debug("Unable to update the console proxy");
				return;
			}
			txn.commit();
		} catch (Exception e) {
			s_logger.error("Unable to complete stop command due to ", e);
		}

		if (_storageMgr.unshare(proxy, null) == null) {
			s_logger.warn("Unable to set share to false for " + proxy.getId());
		}
	}

	@Override
	public ConsoleProxyVO get(long id) {
		return _consoleProxyDao.findById(id);
	}

	@Override
	public Long convertToId(String vmName) {
		if (!VirtualMachineName.isValidConsoleProxyName(vmName, _instance)) {
			return null;
		}
		return VirtualMachineName.getConsoleProxyId(vmName);
	}

	@Override
	public boolean stopProxy(long proxyVmId, long startEventId) {

		AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
		if (asyncExecutor != null) {
			AsyncJobVO job = asyncExecutor.getJob();

			if (s_logger.isInfoEnabled())
				s_logger.info("Stop console proxy " + proxyVmId
						+ ", update async job-" + job.getId());
			_asyncMgr.updateAsyncJobAttachment(job.getId(), "console_proxy",
					proxyVmId);
		}

		ConsoleProxyVO proxy = _consoleProxyDao.findById(proxyVmId);
		if (proxy == null) {
			if (s_logger.isDebugEnabled())
				s_logger.debug("Stopping console proxy failed: console proxy "
						+ proxyVmId + " no longer exists");
			return false;
		}
/*		
		saveStartedEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM,
				EventTypes.EVENT_PROXY_STOP, "Stopping console proxy with Id: "
						+ proxyVmId, startEventId);
*/						
		try {
			return stop(proxy, startEventId);
		} catch (AgentUnavailableException e) {
			if (s_logger.isDebugEnabled())
				s_logger.debug("Stopping console proxy " + proxy.getName()
						+ " faled : exception " + e.toString());
			return false;
		}
	}

	@Override
	public boolean rebootProxy(long proxyVmId, long startEventId) {
		AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
		if (asyncExecutor != null) {
			AsyncJobVO job = asyncExecutor.getJob();

			if (s_logger.isInfoEnabled())
				s_logger.info("Reboot console proxy " + proxyVmId
						+ ", update async job-" + job.getId());
			_asyncMgr.updateAsyncJobAttachment(job.getId(), "console_proxy",
					proxyVmId);
		}

		final ConsoleProxyVO proxy = _consoleProxyDao.findById(proxyVmId);

		if (proxy == null || proxy.getState() == State.Destroyed) {
			return false;
		}

/*
		saveStartedEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM,
				EventTypes.EVENT_PROXY_REBOOT,
				"Rebooting console proxy with Id: " + proxyVmId, startEventId);
*/
		if (proxy.getState() == State.Running && proxy.getHostId() != null) {
			final RebootCommand cmd = new RebootCommand(proxy.getInstanceName());
			final Answer answer = _agentMgr.easySend(proxy.getHostId(), cmd);

			if (answer != null) {
				if (s_logger.isDebugEnabled())
					s_logger.debug("Successfully reboot console proxy "
							+ proxy.getName());

				SubscriptionMgr.getInstance().notifySubscribers(
						ConsoleProxyManager.ALERT_SUBJECT,
						this,
						new ConsoleProxyAlertEventArgs(
								ConsoleProxyAlertEventArgs.PROXY_REBOOTED,
								proxy.getDataCenterId(), proxy.getId(), proxy,
								null));

				final EventVO event = new EventVO();
				event.setUserId(User.UID_SYSTEM);
				event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
				event.setType(EventTypes.EVENT_PROXY_REBOOT);
				event.setLevel(EventVO.LEVEL_INFO);
				event.setStartId(startEventId);
				event.setDescription("Console proxy rebooted - "
						+ proxy.getName());
				_eventDao.persist(event);
				return true;
			} else {
				if (s_logger.isDebugEnabled())
					s_logger.debug("failed to reboot console proxy : "
							+ proxy.getName());

				final EventVO event = new EventVO();
				event.setUserId(User.UID_SYSTEM);
				event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
				event.setType(EventTypes.EVENT_PROXY_REBOOT);
				event.setLevel(EventVO.LEVEL_ERROR);
				event.setStartId(startEventId);
				event.setDescription("Rebooting console proxy failed - "
						+ proxy.getName());
				_eventDao.persist(event);
				return false;
			}
		} else {
			return startProxy(proxyVmId, 0) != null;
		}
	}

	@Override
	public boolean destroy(ConsoleProxyVO proxy)
			throws AgentUnavailableException {
		return destroyProxy(proxy.getId(), 0);
	}

	@Override
	@DB
	public boolean destroyProxy(long vmId, long startEventId) {
		AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
		if (asyncExecutor != null) {
			AsyncJobVO job = asyncExecutor.getJob();

			if (s_logger.isInfoEnabled())
				s_logger.info("Destroy console proxy " + vmId + ", update async job-" + job.getId());
			_asyncMgr.updateAsyncJobAttachment(job.getId(), "console_proxy",
					vmId);
		}

		ConsoleProxyVO vm = _consoleProxyDao.findById(vmId);
		if (vm == null || vm.getState() == State.Destroyed) {
			if (s_logger.isDebugEnabled()) {
				s_logger.debug("Unable to find vm or vm is destroyed: " + vmId);
			}
			return true;
		}
/*		
		saveStartedEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM,
				EventTypes.EVENT_PROXY_DESTROY,
				"Destroying console proxy with Id: " + vmId, startEventId);
*/				
		if (s_logger.isDebugEnabled()) {
			s_logger.debug("Destroying console proxy vm " + vmId);
		} 

		if (!_consoleProxyDao.updateIf(vm, Event.DestroyRequested, null)) {
			s_logger
					.debug("Unable to destroy the vm because it is not in the correct state: "
							+ vmId);
			return false;
		}

		Transaction txn = Transaction.currentTxn();
		List<VolumeVO> vols = null;
		try {
			vols = _volsDao.findByInstance(vmId);
			if (vols.size() != 0) {
				_storageMgr.destroy(vm, vols);
			}

			return true;
		} finally {
			try {
				txn.start();
				// release critical system resources used by the VM before we
				// delete them
				if (vm.getPublicIpAddress() != null)
					freePublicIpAddress(vm.getPublicIpAddress(), vm.getDataCenterId(), vm.getPodId());
				vm.setPublicIpAddress(null);

				_consoleProxyDao.remove(vm.getId());

				final EventVO event = new EventVO();
				event.setUserId(User.UID_SYSTEM);
				event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
				event.setType(EventTypes.EVENT_PROXY_DESTROY);
				event.setLevel(EventVO.LEVEL_INFO);
				event.setStartId(startEventId);
				event.setDescription("Console proxy destroyed - "
						+ vm.getName());
				_eventDao.persist(event);

				txn.commit();
			} catch (Exception e) {
				s_logger.error("Caught this error: ", e);
				txn.rollback();
				return false;
			} finally {
				s_logger.debug("console proxy vm is destroyed : "
						+ vm.getName());
			}
		}
	}

	@DB
	public boolean destroyProxyDBOnly(long vmId) {
		Transaction txn = Transaction.currentTxn();
		try {
			txn.start();
			_volsDao.deleteVolumesByInstance(vmId);

			ConsoleProxyVO proxy = _consoleProxyDao.findById(vmId);
			if (proxy != null) {
				if (proxy.getPublicIpAddress() != null)
					freePublicIpAddress(proxy.getPublicIpAddress(), proxy.getDataCenterId(), proxy.getPodId());

				_consoleProxyDao.remove(vmId);

				final EventVO event = new EventVO();
				event.setUserId(User.UID_SYSTEM);
				event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
				event.setType(EventTypes.EVENT_PROXY_DESTROY);
				event.setLevel(EventVO.LEVEL_INFO);
				event.setDescription("Console proxy destroyed - "
						+ proxy.getName());
				_eventDao.persist(event);
			}

			txn.commit();
			return true;
		} catch (Exception e) {
			s_logger.error("Caught this error: ", e);
			txn.rollback();
			return false;
		} finally {
			s_logger.debug("console proxy vm is destroyed from DB : " + vmId);
		}
	}

	@Override
	public boolean stop(ConsoleProxyVO proxy, long startEventId)
			throws AgentUnavailableException {
		if (!_consoleProxyDao.updateIf(proxy, Event.StopRequested, proxy.getHostId())) {
			s_logger.debug("Unable to stop console proxy: " + proxy.toString());
			return false;
		}

		// IPAddressVO ip = _ipAddressDao.findById(proxy.getPublicIpAddress());
		// VlanVO vlan = _vlanDao.findById(new Long(ip.getVlanDbId()));

		GlobalLock proxyLock = GlobalLock.getInternLock(getProxyLockName(proxy
				.getId()));
		try {
			if (proxyLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
				try {
					StopCommand cmd = new StopCommand(proxy, true, Integer
							.toString(_consoleProxyPort), Integer
							.toString(_consoleProxyUrlPort), proxy
							.getPublicIpAddress());
					try {
						Long proxyHostId = proxy.getHostId();
						if (proxyHostId == null) {
							s_logger.debug("Unable to stop due to proxy " + proxy.getId()
								+ " as host is no longer available, proxy may already have been stopped");
							return false;
						}
						StopAnswer answer = (StopAnswer) _agentMgr.send(
								proxyHostId, cmd);
						if (answer == null || !answer.getResult()) {
							s_logger.debug("Unable to stop due to " + (answer == null ? "answer is null" : answer.getDetails()));

							final EventVO event = new EventVO();
							event.setUserId(User.UID_SYSTEM);
							event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
							event.setType(EventTypes.EVENT_PROXY_STOP);
							event.setLevel(EventVO.LEVEL_ERROR);
							event.setStartId(startEventId);
							event.setDescription("Stopping console proxy failed due to negative answer from agent - " + proxy.getName());
							_eventDao.persist(event);
							return false;
						}
						completeStopCommand(proxy, Event.OperationSucceeded);

						SubscriptionMgr.getInstance().notifySubscribers(
								ConsoleProxyManager.ALERT_SUBJECT,
								this,
								new ConsoleProxyAlertEventArgs(
										ConsoleProxyAlertEventArgs.PROXY_DOWN,
										proxy.getDataCenterId(), proxy.getId(),
										proxy, null));

						final EventVO event = new EventVO();
						event.setUserId(User.UID_SYSTEM);
						event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
						event.setType(EventTypes.EVENT_PROXY_STOP);
						event.setLevel(EventVO.LEVEL_INFO);
						event.setStartId(startEventId);
						event.setDescription("Console proxy stopped - "
								+ proxy.getName());
						_eventDao.persist(event);
						return true;
					} catch (OperationTimedoutException e) {
						final EventVO event = new EventVO();
						event.setUserId(User.UID_SYSTEM);
						event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
						event.setType(EventTypes.EVENT_PROXY_STOP);
						event.setLevel(EventVO.LEVEL_ERROR);
						event.setStartId(startEventId);
						event.setDescription("Stopping console proxy failed due to operation time out - " + proxy.getName());
						_eventDao.persist(event);
						throw new AgentUnavailableException(proxy.getHostId());
					}
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

	@Override
	public boolean migrate(ConsoleProxyVO proxy, HostVO host) {
		HostVO fromHost = _hostDao.findById(proxy.getId());

		if (!_consoleProxyDao.updateIf(proxy, Event.MigrationRequested, proxy.getHostId())) {
			s_logger.debug("State for " + proxy.toString() + " has changed so migration can not take place.");
			return false;
		}

		MigrateCommand cmd = new MigrateCommand(proxy.getInstanceName(), host.getPrivateIpAddress(), false);
		Answer answer = _agentMgr.easySend(fromHost.getId(), cmd);
		if (answer == null) {
			return false;
		}

		_storageMgr.unshare(proxy, fromHost);

		return true;
	}

	@Override
	public boolean completeMigration(ConsoleProxyVO proxy, HostVO host)
			throws AgentUnavailableException, OperationTimedoutException {

		CheckVirtualMachineCommand cvm = new CheckVirtualMachineCommand(proxy
				.getInstanceName());
		NetworkRulesSystemVmCommand nrsvm = new NetworkRulesSystemVmCommand(proxy.getInstanceName());
		Answer [] answers =  _agentMgr.send(host.getId(), new Command[]{cvm, nrsvm}, true);
		CheckVirtualMachineAnswer checkAnswer = (CheckVirtualMachineAnswer)answers[0];
		if (!checkAnswer.getResult()) {
			s_logger.debug("Unable to complete migration for " + proxy.getId());
			_consoleProxyDao.updateIf(proxy, Event.AgentReportStopped, null);
			return false;
		}

		State state = checkAnswer.getState();
		if (state == State.Stopped) {
			s_logger.warn("Unable to complete migration as we can not detect it on " + host.getId());
			_consoleProxyDao.updateIf(proxy, Event.AgentReportStopped, null);
			return false;
		}

		_consoleProxyDao.updateIf(proxy, Event.OperationSucceeded, host.getId());
		
		if (! answers[1].getResult()) {
			s_logger.warn("Migration complete: Failed to program default network rules for system vm " + proxy.getInstanceName());
		} else {
			s_logger.info("Migration complete: Programmed default network rules for system vm " + proxy.getInstanceName());
		}
		return true;
	}

	@Override
	public HostVO prepareForMigration(ConsoleProxyVO proxy)
			throws StorageUnavailableException {

		VMTemplateVO template = _templateDao.findById(proxy.getTemplateId());
		long routerId = proxy.getId();
		boolean mirroredVols = proxy.isMirroredVols();
		DataCenterVO dc = _dcDao.findById(proxy.getDataCenterId());
		HostPodVO pod = _podDao.findById(proxy.getPodId());
		StoragePoolVO sp = _storageMgr.getStoragePoolForVm(proxy.getId());

		List<VolumeVO> vols = _volsDao.findCreatedByInstance(routerId);

		String[] storageIps = new String[2];
		VolumeVO vol = vols.get(0);
		storageIps[0] = vol.getHostIp();
		if (mirroredVols && (vols.size() == 2)) {
			storageIps[1] = vols.get(1).getHostIp();
		}

		PrepareForMigrationCommand cmd = new PrepareForMigrationCommand(proxy.getName(), null, storageIps, vols, mirroredVols);

		HostVO routingHost = null;
		HashSet<Host> avoid = new HashSet<Host>();

		HostVO fromHost = _hostDao.findById(proxy.getHostId());
		if (fromHost.getClusterId() == null) {
			s_logger.debug("The host is not in a cluster");
			return null;
		}
		avoid.add(fromHost);

		while ((routingHost = (HostVO) _agentMgr.findHost(Host.Type.Routing, dc, pod, sp, _serviceOffering,
				template, proxy, fromHost, avoid)) != null) {
			avoid.add(routingHost);

			if (s_logger.isDebugEnabled()) {
				s_logger.debug("Trying to migrate router to host " + routingHost.getName());
			}

			if (!_storageMgr.share(proxy, vols, routingHost, false)) {
				s_logger.warn("Can not share " + proxy.getName());
				throw new StorageUnavailableException(vol.getPoolId());
			}

			Answer answer = _agentMgr.easySend(routingHost.getId(), cmd);
			if (answer != null && answer.getResult()) {
				return routingHost;
			}
			_storageMgr.unshare(proxy, vols, routingHost);
		}

		return null;
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

	private Long saveStartedEvent(Long userId, Long accountId, String type,
			String description, long startEventId) {
		EventVO event = new EventVO();
		event.setUserId(userId);
		event.setAccountId(accountId);
		event.setType(type);
		event.setState(EventState.Started);
		event.setDescription(description);
		event.setStartId(startEventId);
		event = _eventDao.persist(event);
		if (event != null)
			return event.getId();
		return null;
	}
}
