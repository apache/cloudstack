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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.NetworkRulesSystemVmCommand;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.SecStorageFirewallCfgCommand;
import com.cloud.agent.api.SecStorageSetupCommand;
import com.cloud.agent.api.StartSecStorageVmAnswer;
import com.cloud.agent.api.StartSecStorageVmCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
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
import com.cloud.host.dao.HostDao;
import com.cloud.info.RunningHostCountInfo;
import com.cloud.info.RunningHostInfoAgregator;
import com.cloud.info.RunningHostInfoAgregator.ZoneHostInfo;
import com.cloud.network.IpAddrAllocator;
import com.cloud.network.NetworkManager;
import com.cloud.network.IpAddrAllocator.networkInfo;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.ServiceOffering.GuestIpType;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.template.TemplateConstants;
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
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.State;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

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
@Local(value={SecondaryStorageVmManager.class})
public class SecondaryStorageManagerImpl implements SecondaryStorageVmManager, VirtualMachineManager<SecondaryStorageVmVO> {
	private static final Logger s_logger = Logger.getLogger(SecondaryStorageManagerImpl.class);

	private static final int DEFAULT_FIND_HOST_RETRY_COUNT = 2;
	private static final int DEFAULT_CAPACITY_SCAN_INTERVAL = 30000; 		// 30 seconds
	private static final int EXECUTOR_SHUTDOWN_TIMEOUT = 1000; 				// 1 second

	private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 3; 	// 3 seconds
	private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC = 180; 		// 3 minutes
	
 	private static final int STARTUP_DELAY = 60000; 							// 60 seconds

	
	
	private String _mgmt_host;
	private int _mgmt_port = 8250;
	private int _secStorageVmCmdPort = 3;

	private String _name;
	private Adapters<SecondaryStorageVmAllocator> _ssVmAllocators;

	private SecondaryStorageVmDao _secStorageVmDao;
	private DataCenterDao _dcDao;
	private VlanDao _vlanDao;
	private VMTemplateDao _templateDao;
	private IPAddressDao _ipAddressDao;
	private VolumeDao _volsDao;
	private HostPodDao _podDao;
	private HostDao _hostDao;
	private StoragePoolDao _storagePoolDao;
	private StoragePoolHostDao _storagePoolHostDao;
	private UserVmDao _userVmDao;
	private VMInstanceDao _instanceDao;
    private AccountDao _accountDao;

	private VMTemplateHostDao _vmTemplateHostDao;
	private CapacityDao _capacityDao;
	private HighAvailabilityDao _haDao;

	private AgentManager _agentMgr;
	private NetworkManager _networkMgr;
	private StorageManager _storageMgr;
    private HighAvailabilityManager _haMgr;
	
	private ClusterManager _clusterMgr;

	private SecondaryStorageListener _listener;
	
    private ServiceOfferingVO _serviceOffering;
    private int _networkRate;
    private int _multicastRate;
    private VMTemplateVO _template;
    @Inject private ConfigurationDao _configDao;
    @Inject private EventDao _eventDao;
    @Inject private ServiceOfferingDao _offeringDao;
    
    private IpAddrAllocator _IpAllocator;
    
    private AsyncJobManager _asyncMgr;

	private final ScheduledExecutorService _capacityScanScheduler = Executors
			.newScheduledThreadPool(1, new NamedThreadFactory("SS-Scan"));

	
	private long _capacityScanInterval = DEFAULT_CAPACITY_SCAN_INTERVAL;


	private int _secStorageVmRamSize;
	private int _find_host_retry = DEFAULT_FIND_HOST_RETRY_COUNT;

	private String _domain;
	private String _instance;
	private boolean _useLocalStorage;
	private boolean _useSSlCopy;
	private String _allowedInternalSites;

	


	private final GlobalLock _capacityScanLock = GlobalLock.getInternLock(getCapacityScanLockName());
	private final GlobalLock _allocLock = GlobalLock.getInternLock(getAllocLockName());

	
	
	@Override
	public SecondaryStorageVmVO startSecStorageVm(long secStorageVmId, long startEventId) {
		try {
	        saveStartedEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_START, "Starting secondary storage Vm Id: "+secStorageVmId, startEventId);
			return start(secStorageVmId, startEventId);
		} catch (StorageUnavailableException e) {
			s_logger.warn("Exception while trying to start secondary storage vm", e);
			return null;
		} catch (InsufficientCapacityException e) {
			s_logger.warn("Exception while trying to start secondary storage vm", e);
			return null;
		} catch (ConcurrentOperationException e) {
			s_logger.warn("Exception while trying to start secondary storage vm", e);
			return null;
		}
	}

	@Override @DB
	public SecondaryStorageVmVO start(long secStorageVmId, long startEventId) throws StorageUnavailableException, InsufficientCapacityException, ConcurrentOperationException {

        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if (asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();

            if (s_logger.isInfoEnabled())
                s_logger.info("Start secondary storage vm " + secStorageVmId + ", update async job-" + job.getId());
            _asyncMgr.updateAsyncJobAttachment(job.getId(), "sec_storage_vm", secStorageVmId);
        }
		
		SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);
		if (secStorageVm == null || secStorageVm.getRemoved() != null) {
			s_logger.debug("secondary storage vm is not found: " + secStorageVmId);
			return null;
		}

		if (s_logger.isTraceEnabled()) {
			s_logger.trace("Starting secondary storage vm if it is not started, secondary storage vm vm id : " + secStorageVmId);
		}

		for (int i = 0; i < 2; i++) {

			State state = secStorageVm.getState();

			if (state == State.Starting /* || state == State.Migrating */) {
				if (s_logger.isDebugEnabled())
					s_logger.debug("Waiting secondary storage vm to be ready, secondary storage vm id : "
						+ secStorageVmId
						+ " secStorageVm VM state : "
						+ state.toString());

				if (secStorageVm.getPrivateIpAddress() == null || connect(secStorageVm.getPrivateIpAddress(), _secStorageVmCmdPort) != null) {
					if (secStorageVm.getPrivateIpAddress() == null)
						s_logger.warn("Retruning a secondary storage vm that is being started but private IP has not been allocated yet, secondary storage vm id : "
							+ secStorageVmId);
					else
						s_logger.warn("Waiting secondary storage vm to be ready timed out, secondary storage vm id : "
							+ secStorageVmId);

					// TODO, it is very tricky here, if the startup process
					// takes too long and it timed out here,
					// we may give back a secondary storage vm that is not fully ready for
					// functioning
				}
				return secStorageVm;
			}

			if (state == State.Running) {
				if (s_logger.isTraceEnabled())
					s_logger.trace("Secondary storage vm is already started: "
							+ secStorageVm.getName());
				return secStorageVm;
			}

			DataCenterVO dc = _dcDao.findById(secStorageVm.getDataCenterId());
			HostPodVO pod = _podDao.findById(secStorageVm.getPodId());
			List<StoragePoolVO> sps = _storageMgr.getStoragePoolsForVm(secStorageVm.getId());
			StoragePoolVO sp = sps.get(0); // FIXME

			HashSet<Host> avoid = new HashSet<Host>();
			HostVO routingHost = (HostVO) _agentMgr.findHost(Host.Type.Routing, dc, pod, sp, _serviceOffering, _template, secStorageVm, null, avoid);

			if (routingHost == null) {
				if (s_logger.isDebugEnabled()) {
					s_logger.debug("Unable to find a routing host for " + secStorageVm.toString());
					continue;
				}
			}
			// to ensure atomic state transition to Starting state
			if (!_secStorageVmDao.updateIf(secStorageVm, Event.StartRequested, routingHost.getId())) {
				if (s_logger.isDebugEnabled()) {
					SecondaryStorageVmVO temp = _secStorageVmDao.findById(secStorageVmId);
					s_logger.debug("Unable to start secondary storage vm "
							+ secStorageVm.getName()
							+ " because it is not in a startable state : "
							+ ((temp != null) ? temp.getState().toString() : "null"));
				}
				continue;
			}

			try {
				Answer answer = null;
				int retry = _find_host_retry;

				// Secondary storage vm VM will be running at routing hosts as routing
				// hosts have public access to outside network
				do {
					if (s_logger.isDebugEnabled()) {
						s_logger.debug("Trying to start secondary storage vm on host "
								+ routingHost.getName());
					}

					String privateIpAddress = allocPrivateIpAddress(
							secStorageVm.getDataCenterId(), routingHost.getPodId(),
							secStorageVm.getId(), secStorageVm.getPrivateMacAddress());
					if (privateIpAddress == null && (_IpAllocator != null && !_IpAllocator.exteralIpAddressAllocatorEnabled())) {
						s_logger.debug("Not enough ip addresses in " + routingHost.getPodId());
						avoid.add(routingHost);
						continue;
					}

					secStorageVm.setPrivateIpAddress(privateIpAddress);
					String guestIpAddress = _dcDao.allocateLinkLocalPrivateIpAddress(secStorageVm.getDataCenterId(), routingHost.getPodId(), secStorageVm.getId());
					secStorageVm.setGuestIpAddress(guestIpAddress);
					_secStorageVmDao.updateIf(secStorageVm, Event.OperationRetry, routingHost.getId());
					secStorageVm = _secStorageVmDao.findById(secStorageVm.getId());

					List<VolumeVO> vols = _storageMgr.prepare(secStorageVm, routingHost);
					if (vols == null || vols.size() == 0) {
                        s_logger.warn("Can not share " + secStorageVm.getName());
                        avoid.add(routingHost);
                        continue;
					}
		            VolumeVO vol = vols.get(0);
		            
					// carry the secondary storage vm port info over so that we don't
					// need to configure agent on this
					StartSecStorageVmCommand cmdStart = new StartSecStorageVmCommand(_networkRate, 
                                               _multicastRate, _secStorageVmCmdPort, secStorageVm, secStorageVm.getName(), "",
							vols, _mgmt_host, _mgmt_port, _useSSlCopy);

					if (s_logger.isDebugEnabled())
						s_logger.debug("Sending start command for secondary storage vm "
								+ secStorageVm.getName()
								+ " to "
								+ routingHost.getName());

	                try {
	                    answer = _agentMgr.send(routingHost.getId(), cmdStart);
	                    s_logger.debug("StartSecStorageVmCommand Answer: " + (answer != null ? answer : "null"));

	                    if (s_logger.isDebugEnabled())
	                        s_logger.debug("Received answer on starting secondary storage vm "
	                            + secStorageVm.getName()
	                            + " on "
	                            + routingHost.getName());

	                    if ( answer != null && answer.getResult() ) {
	                        if (s_logger.isDebugEnabled()) {
	                            s_logger.debug("Secondary storage vm " + secStorageVm.getName()
	                                    + " started on " + routingHost.getName());
	                        }
	                        
	                        if (answer instanceof StartSecStorageVmAnswer){
	                            StartSecStorageVmAnswer rAnswer = (StartSecStorageVmAnswer)answer;
	                            if (rAnswer.getPrivateIpAddress() != null) {
	                                secStorageVm.setPrivateIpAddress(rAnswer.getPrivateIpAddress());
	                            }
	                            if (rAnswer.getPrivateMacAddress() != null) {
	                                secStorageVm.setPrivateMacAddress(rAnswer.getPrivateMacAddress());
	                            }
		                        final EventVO event = new EventVO();
		                        event.setUserId(User.UID_SYSTEM);
		                        event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
		                        event.setType(EventTypes.EVENT_SSVM_START);
		                        event.setLevel(EventVO.LEVEL_INFO);
		                        event.setStartId(startEventId);
		                        event.setDescription("Secondary Storage VM started - " + secStorageVm.getName());
		                        _eventDao.persist(event);
	                        }
	                        break;
	                    }
	                    s_logger.debug("Unable to start " + secStorageVm.toString() + " on host " + routingHost.toString() + " due to " + answer.getDetails());
	                } catch (OperationTimedoutException e) {
	                    if (e.isActive()) {
	                        s_logger.debug("Unable to start vm " + secStorageVm.getName() + " due to operation timed out and it is active so scheduling a restart.");
	                        _haMgr.scheduleRestart(secStorageVm, true);
	                        return null;
	                    }
	                } catch (AgentUnavailableException e) {
	                    s_logger.debug("Agent " + routingHost.toString() + " was unavailable to start VM " + secStorageVm.getName());
	                }

					avoid.add(routingHost);
					secStorageVm.setPrivateIpAddress(null);
					freePrivateIpAddress(privateIpAddress, secStorageVm
							.getDataCenterId(), secStorageVm.getId());
					secStorageVm.setGuestIpAddress(null);
					_dcDao.releaseLinkLocalPrivateIpAddress(guestIpAddress, secStorageVm.getDataCenterId(), secStorageVm.getId());
					_storageMgr.unshare(secStorageVm, vols, routingHost);
				} while (--retry > 0 && (routingHost = (HostVO) _agentMgr.findHost(
								Host.Type.Routing, dc, pod, sp, _serviceOffering, _template,
								secStorageVm, null, avoid)) != null);
				if (routingHost == null || retry <= 0) {
					
				    SubscriptionMgr.getInstance().notifySubscribers(
						ALERT_SUBJECT, this,
						new SecStorageVmAlertEventArgs(
							SecStorageVmAlertEventArgs.SSVM_START_FAILURE,
							secStorageVm.getDataCenterId(), secStorageVm.getId(), secStorageVm, "Unable to find a routing host to run")
					);

                    final EventVO event = new EventVO();
                    event.setUserId(User.UID_SYSTEM);
                    event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
                    event.setType(EventTypes.EVENT_SSVM_START);
                    event.setLevel(EventVO.LEVEL_ERROR);
                    event.setStartId(startEventId);
                    event.setDescription("Starting secondary storage vm failed due to unable to find a host - " + secStorageVm.getName());
                    _eventDao.persist(event);
					throw new ExecutionException(
							"Couldn't find a routingHost to run secondary storage vm");
				}

				_secStorageVmDao.updateIf(secStorageVm, Event.OperationSucceeded, routingHost.getId());
				if (s_logger.isDebugEnabled()) {
					s_logger.debug("Secondary storage vm is now started, vm id : " + secStorageVm.getId());
				}

				SubscriptionMgr.getInstance().notifySubscribers(
					ALERT_SUBJECT, this,
					new SecStorageVmAlertEventArgs(
						SecStorageVmAlertEventArgs.SSVM_UP,
						secStorageVm.getDataCenterId(), secStorageVm.getId(), secStorageVm, null)
				);
				
				return secStorageVm;
			} catch (Throwable thr) {
				s_logger.warn("Unexpected exception: ", thr);
				
				SubscriptionMgr.getInstance().notifySubscribers(
					ALERT_SUBJECT, this,
					new SecStorageVmAlertEventArgs(
						SecStorageVmAlertEventArgs.SSVM_START_FAILURE,
						secStorageVm.getDataCenterId(), secStorageVm.getId(), secStorageVm, "Unexpected exception: " + thr.getMessage())
				);
				
                final EventVO event = new EventVO();
                event.setUserId(User.UID_SYSTEM);
                event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
                event.setType(EventTypes.EVENT_SSVM_START);
                event.setLevel(EventVO.LEVEL_ERROR);
                event.setStartId(startEventId);
                event.setDescription("Starting secondary storage vm failed due to unhandled exception - " + secStorageVm.getName());
                _eventDao.persist(event);

				Transaction txn = Transaction.currentTxn();
				try {
					txn.start();
					String privateIpAddress = secStorageVm.getPrivateIpAddress();
					if (privateIpAddress != null) {
						secStorageVm.setPrivateIpAddress(null);
						freePrivateIpAddress(privateIpAddress, secStorageVm.getDataCenterId(), secStorageVm.getId());
					}
					secStorageVm.setStorageIp(null);
					_secStorageVmDao.updateIf(secStorageVm, Event.OperationFailed, null);
					txn.commit();
				} catch (Exception e) {
					s_logger.error("Caught exception during error recovery");
				}

				if (thr instanceof StorageUnavailableException) {
					throw (StorageUnavailableException) thr;
				} else if (thr instanceof ConcurrentOperationException) {
					throw (ConcurrentOperationException) thr;
				} else if (thr instanceof ExecutionException) {
					s_logger.error("Error while starting secondary storage vm due to " + thr.getMessage());
				} else {
					s_logger.error("Error while starting secondary storage vm ", thr);
				}
				return null;
			}
		}

		s_logger.warn("Starting secondary storage vm encounters non-startable situation");
		return null;
	}
	
	public boolean  generateFirewallConfiguration(Long hostId){
		if (hostId == null) {
			return true;
		}
		boolean success = true;
		List<DataCenterVO> allZones = _dcDao.listAllActive();
		for (DataCenterVO zone: allZones){
			success = success && generateFirewallConfigurationForZone( zone.getId());
		}
        return true;
	}
	
	@Override
	public boolean generateSetupCommand(Long zoneId) {

		List<SecondaryStorageVmVO> zoneSsvms = _secStorageVmDao.listByZoneId(zoneId);
		if (zoneSsvms.size() == 0) {
			return true;
		}
		SecondaryStorageVmVO secStorageVm = zoneSsvms.get(0);//FIXME: assumes one vm per zone.
		if (secStorageVm.getState() != State.Running && secStorageVm.getState() != State.Starting){
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
			String [] cidrs = _allowedInternalSites.split(",");
			for (String cidr: cidrs) {
				if (NetUtils.isValidCIDR(cidr) || NetUtils.isValidIp(cidr)) {
					allowedCidrs.add(cidr);
				}
			}
			String privateCidr = NetUtils.ipAndNetMaskToCidr(secStorageVm.getPrivateIpAddress(), secStorageVm.getPrivateNetmask());
			String publicCidr = NetUtils.ipAndNetMaskToCidr(secStorageVm.getPublicIpAddress(), secStorageVm.getPublicNetmask());
			if (NetUtils.isNetworkAWithinNetworkB(privateCidr, publicCidr) || NetUtils.isNetworkAWithinNetworkB(publicCidr, privateCidr)){
				allowedCidrs.add("0.0.0.0/0");
			}
			setupCmd.setAllowedInternalSites(allowedCidrs.toArray(new String[allowedCidrs.size()]));
		}
		String copyPasswd = _configDao.getValue("secstorage.copy.password");
		setupCmd.setCopyPassword(copyPasswd);
		setupCmd.setCopyUserName(TemplateConstants.DEFAULT_HTTP_AUTH_USER);
		Answer answer = _agentMgr.easySend(storageHost.getId(), setupCmd);
		if (answer != null) {
			if (s_logger.isDebugEnabled())
				s_logger.debug("Successfully programmed http auth into " + secStorageVm.getName());
			return true;
		} else {
			if (s_logger.isDebugEnabled())
				s_logger.debug("failed to program http auth into secondary storage vm : " + secStorageVm.getName());
			return false;
		}
	}

	private boolean generateFirewallConfigurationForZone( Long zoneId) {
		List<SecondaryStorageVmVO> zoneSsvms = _secStorageVmDao.listByZoneId(zoneId);
		if (zoneSsvms.size() == 0) {
			return true;
		}
		SecondaryStorageVmVO secStorageVm = zoneSsvms.get(0);//FIXME: assumes one vm per zone.
		if (secStorageVm.getState() != State.Running && secStorageVm.getState() != State.Starting){
			s_logger.warn("No running secondary storage vms found in zone " + zoneId + " , skip programming firewall rules");
			return true;
		}
		Host storageHost = _hostDao.findSecondaryStorageHost(zoneId);
		if (storageHost == null) {
			s_logger.warn("No storage hosts found in zone " + zoneId + " , skip programming firewall rules");
			return true;
		}
		List<SecondaryStorageVmVO> alreadyRunning = _secStorageVmDao.getSecStorageVmListInStates( State.Running, State.Migrating, State.Creating, State.Starting);

		String copyPort = Integer.toString(TemplateConstants.DEFAULT_TMPLT_COPY_PORT);
		SecStorageFirewallCfgCommand cpc = new SecStorageFirewallCfgCommand();
		for (SecondaryStorageVmVO ssVm: alreadyRunning) {
			if (ssVm.getPublicIpAddress() != null) {
				if (ssVm.getId() == secStorageVm.getId())
					continue;
				cpc.addPortConfig(ssVm.getPublicIpAddress(), copyPort , true, TemplateConstants.DEFAULT_TMPLT_COPY_INTF);
				if (_useSSlCopy){
					cpc.addPortConfig(ssVm.getPublicIpAddress(), "443" , true, TemplateConstants.DEFAULT_TMPLT_COPY_INTF);
				}
			}
		}
		Answer answer = _agentMgr.easySend(storageHost.getId(), cpc);
		if (answer != null) {
			if (s_logger.isDebugEnabled())
				s_logger.debug("Successfully programmed firewall rules into " + secStorageVm.getName());
			return true;
		} else {
			if (s_logger.isDebugEnabled())
				s_logger.debug("failed to program firewall rules into secondary storage vm : " + secStorageVm.getName());
			return false;
		}
		
	}
	



	public SecondaryStorageVmVO startNew(long dataCenterId) {

		if (s_logger.isDebugEnabled())
			s_logger.debug("Assign secondary storage vm from a newly started instance for request from data center : " + dataCenterId);

		Map<String, Object> context = createSecStorageVmInstance(dataCenterId);

		long secStorageVmId = (Long) context.get("secStorageVmId");
		if (secStorageVmId == 0) {
			if (s_logger.isTraceEnabled())
				s_logger.trace("Creating secondary storage vm instance failed, data center id : " + dataCenterId);

			// release critical system resource on failure
			if (context.get("publicIpAddress") != null)
				freePublicIpAddress((String) context.get("publicIpAddress"), dataCenterId, 0);

			return null;
		}

		SecondaryStorageVmVO secStorageVm = allocSecStorageVmStorage(dataCenterId, secStorageVmId);
		if (secStorageVm != null) {
			SubscriptionMgr.getInstance().notifySubscribers(ALERT_SUBJECT, this,
				new SecStorageVmAlertEventArgs(
					SecStorageVmAlertEventArgs.SSVM_CREATED,
					dataCenterId, secStorageVmId, secStorageVm, null)
			);
			return secStorageVm;
		} else {
			if (s_logger.isDebugEnabled())
				s_logger.debug("Unable to allocate secondary storage vm storage, remove the secondary storage vm record from DB, secondary storage vm id: "
					+ secStorageVmId);
			
			SubscriptionMgr.getInstance().notifySubscribers(ALERT_SUBJECT, this,
				new SecStorageVmAlertEventArgs(
					SecStorageVmAlertEventArgs.SSVM_CREATE_FAILURE,
					dataCenterId, secStorageVmId, null, "Unable to allocate storage")
			);
			destroySecStorageVmDBOnly(secStorageVmId);
		}
		return null;
	}

	@DB
	protected Map<String, Object> createSecStorageVmInstance(long dataCenterId) {

		Map<String, Object> context = new HashMap<String, Object>();
		String publicIpAddress = null;
		HostVO secHost = _hostDao.findSecondaryStorageHost(dataCenterId);
        if (secHost == null) {
			String msg = "No secondary storage available in zone " + dataCenterId + ", cannot create secondary storage vm";
			s_logger.warn(msg);
        	throw new CloudRuntimeException(msg);
        }
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
			String[] macAddresses = _dcDao.getNextAvailableMacAddressPair(
					dataCenterId, (1L << 31));
			String privateMacAddress = macAddresses[0];
			String publicMacAddress = macAddresses[1];
			macAddresses = _dcDao.getNextAvailableMacAddressPair(
					dataCenterId, (1L << 31));
			String guestMacAddress = macAddresses[0];
			while ((pod = _agentMgr.findPod(_template, _serviceOffering, dc, Account.ACCOUNT_ID_SYSTEM, avoidPods)) != null){
				avoidPods.add(pod.first().getId());
				publicIpAndVlan = allocPublicIpAddress(dataCenterId, pod.first().getId(), publicMacAddress);
				if (publicIpAndVlan != null) {
					break;
				}
     			s_logger.warn("Unable to allocate public IP address for secondary storage vm in data center : " + dataCenterId + ", pod="+ pod.first().getId());

			}
			
			if (pod == null || publicIpAndVlan == null) {
				s_logger.warn("Unable to allocate pod for secondary storage vm in data center : " + dataCenterId);

				context.put("secStorageVmId", (long) 0);
				return context;
			}
			
			long id = _secStorageVmDao.getNextInSequence(Long.class, "id");
			
			context.put("publicIpAddress", publicIpAndVlan._ipAddr);
			context.put("pod", pod);
			if (s_logger.isDebugEnabled()) {
				s_logger.debug("Pod allocated " + pod.first().getName());
			}

			String cidrNetmask = NetUtils.getCidrNetmask(pod.first().getCidrSize());
			
			// Find the VLAN ID, VLAN gateway, and VLAN netmask for publicIpAddress
			publicIpAddress = publicIpAndVlan._ipAddr;
            
            String vlanGateway = publicIpAndVlan._gateWay;
            String vlanNetmask = publicIpAndVlan._netMask;

			txn.start();
			SecondaryStorageVmVO secStorageVm;
			String name = VirtualMachineName.getSystemVmName(id, _instance, "s").intern();
	        
			secStorageVm = new SecondaryStorageVmVO(id, name, State.Creating, guestMacAddress, null, NetUtils.getLinkLocalNetMask(),
					privateMacAddress, null, cidrNetmask, _template.getId(), _template.getGuestOSId(),
					publicMacAddress, publicIpAddress, vlanNetmask, publicIpAndVlan._vlanDbId, publicIpAndVlan._vlanid,
					pod.first().getId(), dataCenterId, vlanGateway, null,
					dc.getInternalDns1(), dc.getInternalDns2(), _domain, _secStorageVmRamSize, secHost.getGuid(), secHost.getStorageUrl());

			secStorageVm.setLastHostId(pod.second());
			secStorageVm = _secStorageVmDao.persist(secStorageVm);
			long secStorageVmId = secStorageVm.getId();
            final EventVO event = new EventVO();
            event.setUserId(User.UID_SYSTEM);
            event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
            event.setType(EventTypes.EVENT_SSVM_CREATE);
            event.setLevel(EventVO.LEVEL_INFO);
            event.setDescription("New Secondary Storage VM created - " + secStorageVm.getName());
            _eventDao.persist(event);
			txn.commit();

			context.put("secStorageVmId", secStorageVmId);
			return context;
		} catch (Throwable e) {
			s_logger.error("Unexpected exception : ", e);

			context.put("secStorageVmId", (long) 0);
			return context;
		}
	}

	protected SecondaryStorageVmVO allocSecStorageVmStorage(long dataCenterId, long secStorageVmId) {
		SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);
		assert (secStorageVm != null);

		DataCenterVO dc = _dcDao.findById(dataCenterId);
		HostPodVO pod = _podDao.findById(secStorageVm.getPodId());
		
		final AccountVO account = _accountDao.findById(Account.ACCOUNT_ID_SYSTEM);
		
        try {
			List<VolumeVO> vols = _storageMgr.create(account, secStorageVm, _template, dc, pod, _serviceOffering, null);
			if( vols == null ){
				s_logger.error("Unable to alloc storage for secondary storage vm");
				return null;
			}
			
			// kick the state machine
			_secStorageVmDao.updateIf(secStorageVm, Event.OperationSucceeded, null);
			return secStorageVm;
		} catch (StorageUnavailableException e) {
			s_logger.error("Unable to alloc storage for secondary storage vm: ", e);
			return null;
		} catch (ExecutionException e) {
			s_logger.error("Unable to alloc storage for secondary storage vm: ", e);
			return null;
		}
	}

	private networkInfo allocPublicIpAddress(long dcId, long podId, String macAddr) {
		if (_IpAllocator != null && _IpAllocator.exteralIpAddressAllocatorEnabled()) {
			IpAddrAllocator.IpAddr ip = _IpAllocator.getPublicIpAddress(macAddr, dcId, podId);
			networkInfo net = new networkInfo(ip.ipaddr, ip.netMask, ip.gateway, null, "untagged");
			return net;
		}
		
        Pair<String, VlanVO> ipAndVlan = _vlanDao.assignIpAddress(dcId, Account.ACCOUNT_ID_SYSTEM, DomainVO.ROOT_DOMAIN, VlanType.VirtualNetwork, true);
		
        if (ipAndVlan == null) {
        	s_logger.debug("Unable to get public ip address (type=Virtual) for secondary storage vm for data center  : " + dcId);
        	ipAndVlan = _vlanDao.assignPodDirectAttachIpAddress(dcId, podId, Account.ACCOUNT_ID_SYSTEM, DomainVO.ROOT_DOMAIN);
        	if (ipAndVlan == null)
        		s_logger.debug("Unable to get public ip address (type=DirectAttach) for secondary storage vm for data center  : " + dcId);

        }
        if (ipAndVlan != null) {
			VlanVO vlan = ipAndVlan.second();
			networkInfo net = new networkInfo(ipAndVlan.first(), vlan.getVlanNetmask(), vlan.getVlanGateway(), vlan.getId(), vlan.getVlanId());
			return net;
		}
		return null;
	}

	private void freePublicIpAddress(String ipAddress, long dcId, long podId) {
		if (_IpAllocator != null && _IpAllocator.exteralIpAddressAllocatorEnabled()) {
			 _IpAllocator.releasePublicIpAddress(ipAddress, dcId, podId);
		} else {
			_ipAddressDao.unassignIpAddress(ipAddress);
		}
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
	
	private SecondaryStorageVmAllocator getCurrentAllocator() {

		// for now, only one adapter is supported
		Enumeration<SecondaryStorageVmAllocator> it = _ssVmAllocators.enumeration();
		if (it.hasMoreElements())
			return it.nextElement();

		return null;
	}

	protected String connect(String ipAddress, int port) {
		return null;
	}

	
	

	private void checkPendingSecStorageVMs() {
		// drive state to change away from transient states
		List<SecondaryStorageVmVO> l = _secStorageVmDao.getSecStorageVmListInStates(State.Creating);
		if (l != null && l.size() > 0) {
			for (SecondaryStorageVmVO secStorageVm : l) {
				if (secStorageVm.getLastUpdateTime() == null ||
					(secStorageVm.getLastUpdateTime() != null && System.currentTimeMillis() - secStorageVm.getLastUpdateTime().getTime() > 60000)) {
					try {
						SecondaryStorageVmVO readysecStorageVm = null;
						if (_allocLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
							try {
								readysecStorageVm = allocSecStorageVmStorage(secStorageVm.getDataCenterId(), secStorageVm.getId());
							} finally {
								_allocLock.unlock();
							}

							if (readysecStorageVm != null) {
								GlobalLock secStorageVmLock = GlobalLock.getInternLock(getSecStorageVmLockName(readysecStorageVm.getId()));
								try {
									if (secStorageVmLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
										try {
											readysecStorageVm = start(readysecStorageVm.getId(), 0);
										} finally {
											secStorageVmLock.unlock();
										}
									} else {
										if (s_logger.isInfoEnabled())
											s_logger.info("Unable to acquire synchronization lock to start secondary storage vm : " + readysecStorageVm.getName());
									}
								} finally {
									secStorageVmLock.releaseRef();
								}
							}
						} else {
							if (s_logger.isInfoEnabled())
								s_logger.info("Unable to acquire synchronization lock to allocate secondary storage vm storage, wait for next turn");
						}
					} catch (StorageUnavailableException e) {
						s_logger.warn("Storage unavailable", e);
					} catch (InsufficientCapacityException e) {
						s_logger.warn("insuffiient capacity", e);
					} catch (ConcurrentOperationException e) {
						s_logger.debug("Concurrent operation: " + e.getMessage());
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
				} catch(Throwable e) {
					s_logger.warn("Unexpected exception " + e.getMessage(), e);
				} finally {
					txn.close();
				}
			}
			
			private void reallyRun() {
				if (s_logger.isTraceEnabled())
					s_logger.trace("Begin secondary storage vm capacity scan");
				
				Map<Long, ZoneHostInfo> zoneHostInfoMap = getZoneHostInfo();
				if (isServiceReady(zoneHostInfoMap)) {
					if (s_logger.isTraceEnabled())
						s_logger.trace("Sec Storage VM Service is ready, check to see if we need to allocate standby capacity");

					if (!_capacityScanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
						if (s_logger.isTraceEnabled())
							s_logger.trace("Sec Storage VM Capacity scan lock is used by others, skip and wait for my turn");
						return;
					}

					if (s_logger.isTraceEnabled())
						s_logger.trace("*** Begining secondary storage vm capacity scan... ***");

					try {
						checkPendingSecStorageVMs();
						
						List<DataCenterVO> datacenters = _dcDao.listAll();


						for (DataCenterVO dc: datacenters){
							if(isZoneReady(zoneHostInfoMap, dc.getId())) {
								List<SecondaryStorageVmVO> alreadyRunning = _secStorageVmDao.getSecStorageVmListInStates(dc.getId(), State.Running, State.Migrating, State.Creating, State.Starting);
								List<SecondaryStorageVmVO> stopped = _secStorageVmDao.getSecStorageVmListInStates(dc.getId(), State.Stopped, State.Stopping);
								if (alreadyRunning.size() == 0) {
									if (stopped.size() == 0) {
										s_logger.info("No secondary storage vms found in datacenter id=" + dc.getId() + ", starting a new one" );
										allocCapacity(dc.getId());
									} else {
										s_logger.warn("Stopped secondary storage vms found in datacenter id=" + dc.getId() + ", not restarting them automatically" );
									}

								}
							} else {
								if(s_logger.isDebugEnabled())
									s_logger.debug("Zone " + dc.getId() + " is not ready to alloc secondary storage vm");
						}
						}

						if (s_logger.isTraceEnabled())
							s_logger.trace("*** Stop secondary storage vm capacity scan ***");
					} finally {
						_capacityScanLock.unlock();
					}

				} else {
					if (s_logger.isTraceEnabled())
						s_logger.trace("Secondary storage vm service is not ready for capacity preallocation, wait for next time");
				}

				if (s_logger.isTraceEnabled())
					s_logger.trace("End of secondary storage vm capacity scan");
			}
		};
	}



	public SecondaryStorageVmVO assignSecStorageVmFromRunningPool(long dataCenterId) {

		if (s_logger.isTraceEnabled())
			s_logger.trace("Assign  secondary storage vm from running pool for request from data center : " + dataCenterId);

		SecondaryStorageVmAllocator allocator = getCurrentAllocator();
		assert (allocator != null);
		List<SecondaryStorageVmVO> runningList = _secStorageVmDao.getSecStorageVmListInStates(dataCenterId, State.Running);
		if (runningList != null && runningList.size() > 0) {
			if (s_logger.isTraceEnabled()) {
				s_logger.trace("Running secondary storage vm pool size : " + runningList.size());
				for (SecondaryStorageVmVO secStorageVm : runningList)
					s_logger.trace("Running secStorageVm instance : " + secStorageVm.getName());
			}

			Map<Long, Integer> loadInfo = new HashMap<Long, Integer>();
			
			return allocator.allocSecondaryStorageVm(runningList, loadInfo, dataCenterId);
		} else {
			if (s_logger.isTraceEnabled())
				s_logger.trace("Empty running secStorageVm pool for now in data center : " + dataCenterId);
		}
		return null;
	}

	public SecondaryStorageVmVO assignSecStorageVmFromStoppedPool(long dataCenterId) {
		List<SecondaryStorageVmVO> l = _secStorageVmDao.getSecStorageVmListInStates(
				dataCenterId, State.Creating, State.Starting, State.Stopped,
				State.Migrating);
		if (l != null && l.size() > 0)
			return l.get(0);

		return null;
	}

	private void allocCapacity(long dataCenterId) {
		if (s_logger.isTraceEnabled())
			s_logger.trace("Allocate secondary storage vm standby capacity for data center : " + dataCenterId);

		boolean secStorageVmFromStoppedPool = false;
		SecondaryStorageVmVO secStorageVm = assignSecStorageVmFromStoppedPool(dataCenterId);
		if (secStorageVm == null) {
			if (s_logger.isInfoEnabled())
				s_logger.info("No stopped secondary storage vm is available, need to allocate a new secondary storage vm");

			if (_allocLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
				try {
					secStorageVm = startNew(dataCenterId);
				} finally {
					_allocLock.unlock();
				}
			} else {
				if (s_logger.isInfoEnabled())
					s_logger.info("Unable to acquire synchronization lock to allocate secStorageVm resource for standby capacity, wait for next scan");
				return;
			}
		} else {
			if (s_logger.isInfoEnabled())
				s_logger.info("Found a stopped secondary storage vm, bring it up to running pool. secStorageVm vm id : " + secStorageVm.getId());
			secStorageVmFromStoppedPool = true;
		}

		if (secStorageVm != null) {
			long secStorageVmId = secStorageVm.getId();
			GlobalLock secStorageVmLock = GlobalLock.getInternLock(getSecStorageVmLockName(secStorageVmId));
			try {
				if (secStorageVmLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
					try {
						secStorageVm = startSecStorageVm(secStorageVmId, 0);
					} finally {
						secStorageVmLock.unlock();
					}
				} else {
					if (s_logger.isInfoEnabled())
						s_logger.info("Unable to acquire synchronization lock to start secStorageVm for standby capacity, secStorageVm vm id : "
							+ secStorageVm.getId());
					return;
				}
			} finally {
				secStorageVmLock.releaseRef();
			}

			if (secStorageVm == null) {
				if (s_logger.isInfoEnabled())
					s_logger.info("Unable to start secondary storage vm for standby capacity, secStorageVm vm Id : "
						+ secStorageVmId + ", will recycle it and start a new one");

				if (secStorageVmFromStoppedPool)
					destroySecStorageVm(secStorageVmId, 0);
			} else {
				if (s_logger.isInfoEnabled())
					s_logger.info("Secondary storage vm " + secStorageVm.getName() + " is started");
			}
		}
	}

	public boolean isServiceReady(Map<Long, ZoneHostInfo> zoneHostInfoMap) {
		for (ZoneHostInfo zoneHostInfo : zoneHostInfoMap.values()) {
			if ((zoneHostInfo.getFlags() & RunningHostInfoAgregator.ZoneHostInfo.ALL_HOST_MASK) != 0){
				if (s_logger.isInfoEnabled())
					s_logger.info("Zone " + zoneHostInfo.getDcId() + " is ready to launch");
				return true;
			}
		}

		return false;
	}
	
	public boolean isZoneReady(Map<Long, ZoneHostInfo> zoneHostInfoMap, long dataCenterId) {
		ZoneHostInfo zoneHostInfo = zoneHostInfoMap.get(dataCenterId);
		if(zoneHostInfo != null && (zoneHostInfo.getFlags() & RunningHostInfoAgregator.ZoneHostInfo.COMPUTING_HOST_MASK) != 0) {
	        VMTemplateVO template = _templateDao.findConsoleProxyTemplate();
	        HostVO secHost = _hostDao.findSecondaryStorageHost(dataCenterId);
	        if (secHost == null) {
	        	if (s_logger.isDebugEnabled())
					s_logger.debug("No secondary storage available in zone " + dataCenterId + ", wait until it is ready to launch secondary storage vm");
	        	return false;
	        }
	        
	        boolean templateReady = false;
	        if (template != null) {
	        	VMTemplateHostVO templateHostRef = _vmTemplateHostDao.findByHostTemplate(secHost.getId(), template.getId());
	        	templateReady = (templateHostRef != null) && (templateHostRef.getDownloadState() == Status.DOWNLOADED);
	        }
	        
	        if(templateReady) {
	        	
	        	List<Pair<Long, Integer>> l = _storagePoolHostDao.getDatacenterStoragePoolHostInfo(dataCenterId, !_useLocalStorage);
	        	if(l != null && l.size() > 0 && l.get(0).second().intValue() > 0) {
	        		
	        		return true;
	        	} else {
					if (s_logger.isDebugEnabled())
						s_logger.debug("Primary storage is not ready, wait until it is ready to launch secondary storage vm");
	        	}
	        } else {
				if (s_logger.isTraceEnabled())
					s_logger.trace("Zone host is ready, but secondary storage vm template is not ready");
	        }
		}
		return false;
	}
	
	private synchronized Map<Long, ZoneHostInfo> getZoneHostInfo() {
		Date cutTime = DateUtil.currentGMTTime();
		List<RunningHostCountInfo> l = _hostDao.getRunningHostCounts(new Date(cutTime.getTime() - _clusterMgr.getHeartbeatThreshold()));

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
			s_logger.info("Start secondary storage vm manager");

		return true;
	}

	@Override
	public boolean stop() {
		if (s_logger.isInfoEnabled())
			s_logger.info("Stop secondary storage vm manager");
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
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		if (s_logger.isInfoEnabled())
			s_logger.info("Start configuring secondary storage vm manager : " + name);

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
        if ("true".equalsIgnoreCase(useServiceVM)){
        	_useServiceVM = true;
        }
        
        String sslcopy = configDao.getValue("secstorage.encrypt.copy");
        if ("true".equalsIgnoreCase(sslcopy)) {
        	_useSSlCopy = true;
        }
        
        _allowedInternalSites = configDao.getValue("secstorage.allowed.internal.sites");
        
		String value = configs.get("start.retry");
		_find_host_retry = NumbersUtil.parseInt(value, DEFAULT_FIND_HOST_RETRY_COUNT);

		value = configs.get("secstorage.vm.cmd.port");
		_secStorageVmCmdPort = NumbersUtil.parseInt(value, 3922);
		
		
		value = configs.get("secstorage.capacityscan.interval");
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
		if(_mgmt_host == null) {
			s_logger.warn("Critical warning! Please configure your management server host address right after you have started your management server and then restart it, otherwise you won't have access to secondary storage");
		}
		
		value = agentMgrConfigs.get("port");
		_mgmt_port = NumbersUtil.parseInt(value, 8250);

		_secStorageVmDao = locator.getDao(SecondaryStorageVmDao.class);
		if (_secStorageVmDao == null) {
			throw new ConfigurationException("Unable to get " + SecondaryStorageVmDao.class.getName());
		}

		_ssVmAllocators = locator.getAdapters(SecondaryStorageVmAllocator.class);
		if (_ssVmAllocators == null || !_ssVmAllocators.isSet()) {
			throw new ConfigurationException("Unable to get secStorageVm allocators");
		}

		_dcDao = locator.getDao(DataCenterDao.class);
		if (_dcDao == null) {
			throw new ConfigurationException("Unable to get " + DataCenterDao.class.getName());
		}

		_templateDao = locator.getDao(VMTemplateDao.class);
		if (_templateDao == null) {
			throw new ConfigurationException("Unable to get " + VMTemplateDao.class.getName());
		}

		_ipAddressDao = locator.getDao(IPAddressDao.class);
		if (_ipAddressDao == null) {
			throw new ConfigurationException("Unable to get " + IPAddressDao.class.getName());
		}

		_volsDao = locator.getDao(VolumeDao.class);
		if (_volsDao == null) {
			throw new ConfigurationException("Unable to get " + VolumeDao.class.getName());
		}

		_podDao = locator.getDao(HostPodDao.class);
		if (_podDao == null) {
			throw new ConfigurationException("Unable to get " + HostPodDao.class.getName());
		}

		_hostDao = locator.getDao(HostDao.class);
		if (_hostDao == null) {
			throw new ConfigurationException("Unable to get " + HostDao.class.getName());
		}
		
        _storagePoolDao = locator.getDao(StoragePoolDao.class);
        if (_storagePoolDao == null) {
            throw new ConfigurationException("Unable to find " + StoragePoolDao.class);
        }
        
        _storagePoolHostDao = locator.getDao(StoragePoolHostDao.class);
        if (_storagePoolHostDao == null) {
            throw new ConfigurationException("Unable to find " + StoragePoolHostDao.class);
        }

		_vmTemplateHostDao = locator.getDao(VMTemplateHostDao.class);
		if (_vmTemplateHostDao == null) {
			throw new ConfigurationException("Unable to get " + VMTemplateHostDao.class.getName());
		}

		_userVmDao = locator.getDao(UserVmDao.class);
		if (_userVmDao == null)
			throw new ConfigurationException("Unable to get " + UserVmDao.class.getName());

		_instanceDao = locator.getDao(VMInstanceDao.class);
		if (_instanceDao == null)
			throw new ConfigurationException("Unable to get " + VMInstanceDao.class.getName());

		_capacityDao = locator.getDao(CapacityDao.class);
		if (_capacityDao == null) {
			throw new ConfigurationException("Unable to get " + CapacityDao.class.getName());
		}

		_haDao = locator.getDao(HighAvailabilityDao.class);
		if (_haDao == null) {
			throw new ConfigurationException("Unable to get " + HighAvailabilityDao.class.getName());
		}
		
        _accountDao = locator.getDao(AccountDao.class);
        if (_accountDao == null) {
            throw new ConfigurationException("Unable to get " + AccountDao.class.getName());
        }
        
        _vlanDao = locator.getDao(VlanDao.class);
        if (_vlanDao == null) {
            throw new ConfigurationException("Unable to get " + VlanDao.class.getName());
        }

		_agentMgr = locator.getManager(AgentManager.class);
		if (_agentMgr == null) {
			throw new ConfigurationException("Unable to get " + AgentManager.class.getName());
		}
		
		_networkMgr = locator.getManager(NetworkManager.class);
		if (_networkMgr == null) {
			throw new ConfigurationException("Unable to get " + NetworkManager.class.getName());
		}

		_listener = new SecondaryStorageListener(this);
		_agentMgr.registerForHostEvents(_listener, true, true, false);

		_storageMgr = locator.getManager(StorageManager.class);
		if (_storageMgr == null) {
			throw new ConfigurationException("Unable to get " + StorageManager.class.getName());
		}
		
        _haMgr = locator.getManager(HighAvailabilityManager.class);
        if (_haMgr == null) {
            throw new ConfigurationException("Unable to get " + HighAvailabilityManager.class.getName());
        }
        
		_clusterMgr = locator.getManager(ClusterManager.class);
		if (_clusterMgr == null) {
			throw new ConfigurationException("Unable to get " + ClusterManager.class.getName());
		}
		
        _asyncMgr = locator.getManager(AsyncJobManager.class);
		if (_asyncMgr == null) {
			throw new ConfigurationException("Unable to get " + AsyncJobManager.class.getName());
		}

		HighAvailabilityManager haMgr = locator.getManager(HighAvailabilityManager.class);
		if (haMgr != null) {
			haMgr.registerHandler(VirtualMachine.Type.SecondaryStorageVm, this);
		}

		Adapters<IpAddrAllocator> ipAllocators = locator.getAdapters(IpAddrAllocator.class);
		if (ipAllocators != null && ipAllocators.isSet()) {
			Enumeration<IpAddrAllocator> it = ipAllocators.enumeration();
			_IpAllocator = it.nextElement();
		}
		
		boolean useLocalStorage = Boolean.parseBoolean((String)params.get(Config.SystemVMUseLocalStorage.key()));
        String networkRateStr = _configDao.getValue("network.throttling.rate");
        String multicastRateStr = _configDao.getValue("multicast.throttling.rate");
        _networkRate = ((networkRateStr == null) ? 200 : Integer.parseInt(networkRateStr));
        _multicastRate = ((multicastRateStr == null) ? 10 : Integer.parseInt(multicastRateStr));

		_serviceOffering = new ServiceOfferingVO("Fake Offering For Secondary Storage VM", 1, _secStorageVmRamSize, 0, 0, 0, false, null, GuestIpType.Virtualized, useLocalStorage, true, null);
		_serviceOffering.setUniqueName("Cloud.com-SecondaryStorage");
		_serviceOffering = _offeringDao.persistSystemServiceOffering(_serviceOffering);
        _template = _templateDao.findConsoleProxyTemplate();
        if (_template == null) {
            throw new ConfigurationException("Unable to find the template for secondary storage vm VMs");
        }
 
        if (_useServiceVM) {
        	_capacityScanScheduler.scheduleAtFixedRate(getCapacityScanTask(), STARTUP_DELAY,
				_capacityScanInterval, TimeUnit.MILLISECONDS);
        }
		String configValue = _configDao.getValue("system.vm.use.local.storage");
		_useLocalStorage = Boolean.parseBoolean(configValue);
		if (s_logger.isInfoEnabled())
			s_logger.info("Secondary storage vm Manager is configured.");
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
	public void completeStartCommand(SecondaryStorageVmVO vm) {
		_secStorageVmDao.updateIf(vm, Event.AgentReportRunning, vm.getHostId());
	}

	@Override
	public void completeStopCommand(SecondaryStorageVmVO vm) {
		completeStopCommand(vm, Event.AgentReportStopped);
	}

	@DB
	protected void completeStopCommand(SecondaryStorageVmVO secStorageVm, Event ev) {
		Transaction txn = Transaction.currentTxn();
		try {
			txn.start();
			String privateIpAddress = secStorageVm.getPrivateIpAddress();
			if (privateIpAddress != null) {
				secStorageVm.setPrivateIpAddress(null);
				freePrivateIpAddress(privateIpAddress, secStorageVm.getDataCenterId(), secStorageVm.getId());
			}
			String guestIpAddress = secStorageVm.getGuestIpAddress();
			if (guestIpAddress != null) {
				secStorageVm.setGuestIpAddress(null);
				_dcDao.releaseLinkLocalPrivateIpAddress(guestIpAddress, secStorageVm.getDataCenterId(), secStorageVm.getId());
			}
			secStorageVm.setStorageIp(null);
			if (!_secStorageVmDao.updateIf(secStorageVm, ev, null)) {
				s_logger.debug("Unable to update the secondary storage vm");
				return;
			}
			txn.commit();
		} catch (Exception e) {
			s_logger.error("Unable to complete stop command due to ", e);
		}

		if (_storageMgr.unshare(secStorageVm, null) == null) {
			s_logger.warn("Unable to set share to false for " + secStorageVm.getId());
		}
	}

	@Override
	public SecondaryStorageVmVO get(long id) {
		return _secStorageVmDao.findById(id);
	}

	@Override
	public Long convertToId(String vmName) {
		if (!VirtualMachineName.isValidSystemVmName(vmName, _instance, "s")) {
			return null;
		}
		return VirtualMachineName.getSystemVmId(vmName);
	}

	@Override
	public boolean stopSecStorageVm(long secStorageVmId, long startEventId) {
		
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if (asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();

            if (s_logger.isInfoEnabled())
                s_logger.info("Stop secondary storage vm " + secStorageVmId + ", update async job-" + job.getId());
            _asyncMgr.updateAsyncJobAttachment(job.getId(), "secStorageVm", secStorageVmId);
        }
		
		SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);
		if (secStorageVm == null) {
			if (s_logger.isDebugEnabled())
				s_logger.debug("Stopping secondary storage vm failed: secondary storage vm " + secStorageVmId + " no longer exists");
			return false;
		}
        saveStartedEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_STOP, "Stopping secondary storage Vm Id: "+secStorageVmId, startEventId);
		try {
			return stop(secStorageVm, startEventId);
		} catch (AgentUnavailableException e) {
			if (s_logger.isDebugEnabled())
				s_logger.debug("Stopping secondary storage vm " + secStorageVm.getName() + " faled : exception " + e.toString());
			return false;
		}
	}

	@Override
	public boolean rebootSecStorageVm(long secStorageVmId, long startEventId) {
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if (asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();

            if (s_logger.isInfoEnabled())
                s_logger.info("Reboot secondary storage vm " + secStorageVmId + ", update async job-" + job.getId());
            _asyncMgr.updateAsyncJobAttachment(job.getId(), "secstorage_vm", secStorageVmId);
        }
        
		final SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);

		if (secStorageVm == null || secStorageVm.getState() == State.Destroyed) {
			return false;
		}

        saveStartedEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_REBOOT, "Rebooting secondary storage Vm Id: "+secStorageVmId, startEventId);
		
		if (secStorageVm.getState() == State.Running && secStorageVm.getHostId() != null) {
			final RebootCommand cmd = new RebootCommand(secStorageVm.getInstanceName());
			final Answer answer = _agentMgr.easySend(secStorageVm.getHostId(), cmd);

			if (answer != null) {
				if (s_logger.isDebugEnabled())
					s_logger.debug("Successfully reboot secondary storage vm " + secStorageVm.getName());
				
				SubscriptionMgr.getInstance().notifySubscribers(
						ALERT_SUBJECT, this,
						new SecStorageVmAlertEventArgs(
							SecStorageVmAlertEventArgs.SSVM_REBOOTED,
							secStorageVm.getDataCenterId(), secStorageVm.getId(), secStorageVm, null)
					);
				
	            final EventVO event = new EventVO();
	            event.setUserId(User.UID_SYSTEM);
	            event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
	            event.setType(EventTypes.EVENT_SSVM_REBOOT);
	            event.setLevel(EventVO.LEVEL_INFO);
	            event.setStartId(startEventId);
	            event.setDescription("Secondary Storage Vm rebooted - " + secStorageVm.getName());
	            _eventDao.persist(event);
				return true;
			} else {

	            final EventVO event = new EventVO();
	            event.setUserId(User.UID_SYSTEM);
	            event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
	            event.setType(EventTypes.EVENT_SSVM_REBOOT);
	            event.setLevel(EventVO.LEVEL_ERROR);
	            event.setStartId(startEventId);
	            event.setDescription("Rebooting Secondary Storage VM failed - " + secStorageVm.getName());
	            _eventDao.persist(event);
				if (s_logger.isDebugEnabled())
					s_logger.debug("failed to reboot secondary storage vm : " + secStorageVm.getName());
				return false;
			}
		} else {
			return startSecStorageVm(secStorageVmId, 0) != null;
		}
	}

	@Override
	public boolean destroy(SecondaryStorageVmVO secStorageVm)
			throws AgentUnavailableException {
		return destroySecStorageVm(secStorageVm.getId(), 0);
	}

	@Override
	@DB
	public boolean destroySecStorageVm(long vmId, long startEventId) {
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if (asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();

            if (s_logger.isInfoEnabled())
                s_logger.info("Destroy secondary storage vm " + vmId + ", update async job-" + job.getId());
            _asyncMgr.updateAsyncJobAttachment(job.getId(), "secstorage_vm", vmId);
        }
		
		SecondaryStorageVmVO vm = _secStorageVmDao.findById(vmId);
		if (vm == null || vm.getState() == State.Destroyed) {
			if (s_logger.isDebugEnabled()) {
				s_logger.debug("Unable to find vm or vm is destroyed: " + vmId);
			}
			return true;
		}

		saveStartedEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_DESTROY, "Destroying secondary storage Vm Id: "+vmId, startEventId);
		
		if (s_logger.isDebugEnabled()) {
			s_logger.debug("Destroying secondary storage vm vm " + vmId);
		}

		if (!_secStorageVmDao.updateIf(vm, Event.DestroyRequested, null)) {
			s_logger.debug("Unable to destroy the vm because it is not in the correct state: " + vmId);
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

				_secStorageVmDao.remove(vm.getId());
	
	            final EventVO event = new EventVO();
	            event.setUserId(User.UID_SYSTEM);
	            event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
	            event.setType(EventTypes.EVENT_SSVM_DESTROY);
	            event.setLevel(EventVO.LEVEL_INFO);
	            event.setStartId(startEventId);
	            event.setDescription("Secondary Storage Vm destroyed - " + vm.getName());
	            _eventDao.persist(event);
				txn.commit();
			} catch (Exception e) {
				s_logger.error("Caught this error: ", e);
				txn.rollback();
				return false;
			} finally {
				s_logger.debug("secondary storage vm vm is destroyed : "
						+ vm.getName());
			}
		}
	}

	@DB
	public boolean destroySecStorageVmDBOnly(long vmId) {
		Transaction txn = Transaction.currentTxn();
		try {
			txn.start();
			_volsDao.deleteVolumesByInstance(vmId);

			SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(vmId);
			if (secStorageVm != null) {
				if (secStorageVm.getPublicIpAddress() != null)
					freePublicIpAddress(secStorageVm.getPublicIpAddress(), secStorageVm.getDataCenterId(), secStorageVm.getPodId());

				_secStorageVmDao.remove(vmId);
				final EventVO event = new EventVO();
				event.setUserId(User.UID_SYSTEM);
				event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
				event.setType(EventTypes.EVENT_SSVM_DESTROY);
				event.setLevel(EventVO.LEVEL_INFO);
				event.setDescription("Secondary Storage Vm destroyed - " + secStorageVm.getName());
				_eventDao.persist(event);
			}
			txn.commit();
			return true;
		} catch (Exception e) {
			s_logger.error("Caught this error: ", e);
			txn.rollback();
			return false;
		} finally {
			s_logger.debug("secondary storage vm vm is destroyed from DB : " + vmId);
		}
	}

	@Override
	public boolean stop(SecondaryStorageVmVO secStorageVm, long startEventId) throws AgentUnavailableException {
		if (!_secStorageVmDao.updateIf(secStorageVm, Event.StopRequested, secStorageVm.getHostId())) {
			s_logger.debug("Unable to stop secondary storage vm: " + secStorageVm.toString());
			return false;
		}

		// IPAddressVO ip = _ipAddressDao.findById(secStorageVm.getPublicIpAddress());
		// VlanVO vlan = _vlanDao.findById(new Long(ip.getVlanDbId()));

        if (secStorageVm.getHostId() != null) {
            GlobalLock secStorageVmLock = GlobalLock.getInternLock(getSecStorageVmLockName(secStorageVm.getId()));
            try {
                if (secStorageVmLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
                    try {
                        StopCommand cmd = new StopCommand(secStorageVm, true,
                                Integer.toString(0),
                                Integer.toString(0),
                                secStorageVm.getPublicIpAddress());
                        try {
                            StopAnswer answer = (StopAnswer) _agentMgr.send(secStorageVm.getHostId(), cmd);
                            if (answer == null || !answer.getResult()) {
                                s_logger.debug("Unable to stop due to " + (answer == null ? "answer is null" : answer.getDetails()));
                                final EventVO event = new EventVO();
                                event.setUserId(User.UID_SYSTEM);
                                event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
                                event.setType(EventTypes.EVENT_SSVM_STOP);
                                event.setLevel(EventVO.LEVEL_ERROR);
                                event.setStartId(startEventId);
                                event.setDescription("Stopping secondary storage vm failed due to negative answer from agent - " + secStorageVm.getName());
                                _eventDao.persist(event);
                                return false;
                            }
                            completeStopCommand(secStorageVm, Event.OperationSucceeded);
                            
                            SubscriptionMgr.getInstance().notifySubscribers(
                                    SecStorageVmAlertEventArgs.ALERT_SUBJECT, this,
                                    new SecStorageVmAlertEventArgs(
                                        SecStorageVmAlertEventArgs.SSVM_DOWN,
                                        secStorageVm.getDataCenterId(), secStorageVm.getId(), secStorageVm, null)
                                );
                            final EventVO event = new EventVO();
                            event.setUserId(User.UID_SYSTEM);
                            event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
                            event.setType(EventTypes.EVENT_SSVM_STOP);
                            event.setLevel(EventVO.LEVEL_INFO);
                            event.setStartId(startEventId);
                            event.setDescription("Secondary Storage Vm stopped - " + secStorageVm.getName());
                            _eventDao.persist(event);
						return true;
                        } catch (OperationTimedoutException e) {
                        	final EventVO event = new EventVO();
                        	event.setUserId(User.UID_SYSTEM);
                        	event.setAccountId(Account.ACCOUNT_ID_SYSTEM);
                        	event.setType(EventTypes.EVENT_SSVM_STOP);
                        	event.setLevel(EventVO.LEVEL_ERROR);
                        	event.setStartId(startEventId);
                        	event.setDescription("Stopping secondary storage vm failed due to operation time out - " + secStorageVm.getName());
                        	_eventDao.persist(event);
                            throw new AgentUnavailableException(secStorageVm.getHostId());
                        }
                    } finally {
                        secStorageVmLock.unlock();
                    }
                } else {
                    s_logger.debug("Unable to acquire secondary storage vm lock : " + secStorageVm.toString());
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
	public boolean migrate(SecondaryStorageVmVO secStorageVm, HostVO host) {
		HostVO fromHost = _hostDao.findById(secStorageVm.getId());

		if (!_secStorageVmDao.updateIf(secStorageVm, Event.MigrationRequested, secStorageVm.getHostId())) {
			s_logger.debug("State for " + secStorageVm.toString() + " has changed so migration can not take place.");
			return false;
		}

		MigrateCommand cmd = new MigrateCommand(secStorageVm.getInstanceName(), host.getPrivateIpAddress(), false);
		Answer answer = _agentMgr.easySend(fromHost.getId(), cmd);
		if (answer == null) {
			return false;
		}

		_storageMgr.unshare(secStorageVm, fromHost);

		return true;
	}

	@Override
	public boolean completeMigration(SecondaryStorageVmVO secStorageVm, HostVO host)
			throws AgentUnavailableException, OperationTimedoutException {
		CheckVirtualMachineCommand cvm = new CheckVirtualMachineCommand(secStorageVm.getInstanceName());
		NetworkRulesSystemVmCommand nrsvm = new NetworkRulesSystemVmCommand(secStorageVm.getInstanceName());
		Answer [] answers =  _agentMgr.send(host.getId(), new Command[]{cvm, nrsvm}, true);
		CheckVirtualMachineAnswer checkAnswer = (CheckVirtualMachineAnswer)answers[0];
		if (!checkAnswer.getResult()) {
			s_logger.debug("Unable to complete migration for " + secStorageVm.getId());
			_secStorageVmDao.updateIf(secStorageVm, Event.AgentReportStopped, null);
			return false;
		}

		State state = checkAnswer.getState();
		if (state == State.Stopped) {
			s_logger.warn("Unable to complete migration as we can not detect it on " + host.getId());
			_secStorageVmDao.updateIf(secStorageVm, Event.AgentReportStopped, null);
			return false;
		}

		_secStorageVmDao.updateIf(secStorageVm, Event.OperationSucceeded, host.getId());
		if (! answers[1].getResult()) {
			s_logger.warn("Migration complete: Failed to program default network rules for system vm " + secStorageVm.getInstanceName());
		} else {
			s_logger.info("Migration complete: Programmed default network rules for system vm " + secStorageVm.getInstanceName());
		}
		return true;
	}

	@Override
	public HostVO prepareForMigration(SecondaryStorageVmVO secStorageVm) throws StorageUnavailableException {
		
		VMTemplateVO template = _templateDao.findById(secStorageVm.getTemplateId());
		long routerId = secStorageVm.getId();
		boolean mirroredVols = secStorageVm.isMirroredVols();
		DataCenterVO dc = _dcDao.findById(secStorageVm.getDataCenterId());
		HostPodVO pod = _podDao.findById(secStorageVm.getPodId());
        List<StoragePoolVO> sps = _storageMgr.getStoragePoolsForVm(secStorageVm.getId());
        StoragePoolVO sp = sps.get(0); // FIXME

		List<VolumeVO> vols = _volsDao.findCreatedByInstance(routerId);

		String[] storageIps = new String[2];
		VolumeVO vol = vols.get(0);
		storageIps[0] = vol.getHostIp();
		if (mirroredVols && (vols.size() == 2)) {
			storageIps[1] = vols.get(1).getHostIp();
		}

		PrepareForMigrationCommand cmd = new PrepareForMigrationCommand(secStorageVm.getName(), null, storageIps, vols, mirroredVols);

		HostVO routingHost = null;
		HashSet<Host> avoid = new HashSet<Host>();

		HostVO fromHost = _hostDao.findById(secStorageVm.getHostId());
        if (fromHost.getClusterId() == null) {
            s_logger.debug("The host is not in a cluster");
            return null;
        }
		avoid.add(fromHost);

		while ((routingHost = (HostVO) _agentMgr.findHost(Host.Type.Routing,
				dc, pod, sp, _serviceOffering, template, secStorageVm, fromHost, avoid)) != null) {
			avoid.add(routingHost);
			if (s_logger.isDebugEnabled()) {
				s_logger.debug("Trying to migrate router to host " + routingHost.getName());
			}

			if( !_storageMgr.share(secStorageVm, vols, routingHost, false) ) {
				s_logger.warn("Can not share " + vol.getPath() + " to " + secStorageVm.getName());
				throw new StorageUnavailableException(vol.getPoolId());
			}

			Answer answer = _agentMgr.easySend(routingHost.getId(), cmd);
			if (answer != null && answer.getResult()) {
				return routingHost;
			}
			_storageMgr.unshare(secStorageVm, vols, routingHost);
		}

		return null;
	}

	@Override
	public void onAgentConnect(Long dcId, StartupCommand cmd){
		if (_IpAllocator != null && _IpAllocator.exteralIpAddressAllocatorEnabled()) {
			List<SecondaryStorageVmVO> zoneSsvms = _secStorageVmDao.listByZoneId(dcId);
			if (zoneSsvms.size() == 0) {
				return ;
			}
			SecondaryStorageVmVO secStorageVm = zoneSsvms.get(0);//FIXME: assumes one vm per zone.
			secStorageVm.setPrivateIpAddress(cmd.getStorageIpAddress()); /*FIXME: privateipaddress is overwrited with address of secondary storage*/
			secStorageVm.setPrivateNetmask(cmd.getStorageNetmask());
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
	
	private Long saveStartedEvent(Long userId, Long accountId, String type, String description, long startEventId) {
	    EventVO event = new EventVO();
	    event.setUserId(userId);
	    event.setAccountId(accountId);
	    event.setType(type);
	    event.setState(EventState.Started);
	    event.setDescription(description);
	    event.setStartId(startEventId);
	    event = _eventDao.persist(event);
	    if(event != null)
	        return event.getId();
	    return null;
	}
}
