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
import com.cloud.agent.api.PrepareForMigrationCommand;
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
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.event.EventTypes;
import com.cloud.event.EventUtils;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
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
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
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
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.net.NfsUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.State;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineGuru;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachineProfile;
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
@Local(value={SecondaryStorageVmManager.class})
public class SecondaryStorageManagerImpl implements SecondaryStorageVmManager, VirtualMachineGuru<SecondaryStorageVmVO> {
	private static final Logger s_logger = Logger.getLogger(SecondaryStorageManagerImpl.class);

	private static final int DEFAULT_CAPACITY_SCAN_INTERVAL = 30000; 		// 30 seconds
	private static final int EXECUTOR_SHUTDOWN_TIMEOUT = 1000; 				// 1 second

	private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 3; 	// 3 seconds
	private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC = 180; 		// 3 minutes
	
 	private static final int STARTUP_DELAY = 60000; 							// 60 seconds
	
	private String _mgmt_host;
	private int _mgmt_port = 8250;
	private int _secStorageVmCmdPort = 3;

	private String _name;
	@Inject(adapter=SecondaryStorageVmAllocator.class)
	private Adapters<SecondaryStorageVmAllocator> _ssVmAllocators;

	@Inject private SecondaryStorageVmDao _secStorageVmDao;
	@Inject private DataCenterDao _dcDao;
	@Inject private VMTemplateDao _templateDao;
	@Inject private IPAddressDao _ipAddressDao;
	@Inject private VolumeDao _volsDao;
	@Inject private HostPodDao _podDao;
	@Inject private HostDao _hostDao;
	@Inject private StoragePoolHostDao _storagePoolHostDao;
	@Inject private AccountDao _accountDao;

    @Inject private VMTemplateHostDao _vmTemplateHostDao;

	@Inject private AgentManager _agentMgr;
	@Inject private NetworkManager _networkMgr;
	@Inject private StorageManager _storageMgr;
	
    @Inject private ClusterManager _clusterMgr;

	private SecondaryStorageListener _listener;
	
    private ServiceOfferingVO _serviceOffering;
    private VMTemplateVO _template;
    @Inject private ConfigurationDao _configDao;
    @Inject private EventDao _eventDao;
    @Inject private ServiceOfferingDao _offeringDao;
    @Inject private AccountService _accountMgr;
    @Inject private VirtualMachineManager _itMgr;
    @Inject private NicDao _nicDao;
    @Inject private NetworkDao _networkDao;
    
    private IpAddrAllocator _IpAllocator;
    
    private AsyncJobManager _asyncMgr;

	private final ScheduledExecutorService _capacityScanScheduler = Executors
			.newScheduledThreadPool(1, new NamedThreadFactory("SS-Scan"));

	
	private long _capacityScanInterval = DEFAULT_CAPACITY_SCAN_INTERVAL;


	private int _secStorageVmRamSize;

	private String _domain;
	private String _instance;
	private boolean _useLocalStorage;
	private boolean _useSSlCopy;
	private String _secHostUuid;
	private String _nfsShare;
	private String _allowedInternalSites;

	private final GlobalLock _capacityScanLock = GlobalLock.getInternLock(getCapacityScanLockName());
	private final GlobalLock _allocLock = GlobalLock.getInternLock(getAllocLockName());
	
	@Override
	public SecondaryStorageVmVO startSecStorageVm(long secStorageVmId) {
	    boolean started = false;
	    long startEventId = EventUtils.saveStartedEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_START, "Starting secondary storage Vm with Id: "+secStorageVmId);
		try {
		    SecondaryStorageVmVO ssvm = start(secStorageVmId);
		    started = true;
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
			return null;
		} finally {
		    if(started){
		        EventUtils.saveEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventVO.LEVEL_INFO, EventTypes.EVENT_SSVM_START, "Started secondary storage Vm with Id: "+secStorageVmId, startEventId);
		    } else {
		        EventUtils.saveEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventVO.LEVEL_ERROR, EventTypes.EVENT_SSVM_START, "Failed to start secondary storage Vm with Id: "+secStorageVmId, startEventId);
		    }
		}
	}
	
	@Override
    public SecondaryStorageVmVO start(long secStorageVmId) throws ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException {
		SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);
		Account systemAcct = _accountMgr.getSystemAccount();
		User systemUser = _accountMgr.getSystemUser();
		return _itMgr.start(secStorageVm, null, systemUser, systemAcct, null);
	}

	
	@Override
    public boolean  generateFirewallConfiguration(Long hostId){
		if (hostId == null) {
			return true;
		}
		boolean success = true;
		List<DataCenterVO> allZones = _dcDao.listAll();
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
				if (ssVm.getId() == secStorageVm.getId()) {
                    continue;
                }
				cpc.addPortConfig(ssVm.getPublicIpAddress(), copyPort , true, TemplateConstants.DEFAULT_TMPLT_COPY_INTF);
				if (_useSSlCopy){
					cpc.addPortConfig(ssVm.getPublicIpAddress(), "443" , true, TemplateConstants.DEFAULT_TMPLT_COPY_INTF);
				}
			}
		}
		Answer answer = _agentMgr.easySend(storageHost.getId(), cpc);
		if (answer != null) {
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

		long startEventId = EventUtils.saveStartedEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_CREATE, "Creating secondary storage Vm in zone : "+dataCenterId);
		Map<String, Object> context = createSecStorageVmInstance(dataCenterId);

		long secStorageVmId = (Long) context.get("secStorageVmId");
		if (secStorageVmId == 0) {
			if (s_logger.isTraceEnabled()) {
                s_logger.trace("Creating secondary storage vm instance failed, data center id : " + dataCenterId);
            }

			// release critical system resource on failure
			if (context.get("publicIpAddress") != null) {
                freePublicIpAddress((String) context.get("publicIpAddress"), dataCenterId, 0);
            }
			EventUtils.saveEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventVO.LEVEL_ERROR, EventTypes.EVENT_SSVM_CREATE, "Failed to create secondary storage Vm in zone : "+dataCenterId, startEventId);
			return null;
		}

		SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId); 
		//SecondaryStorageVmVO secStorageVm = allocSecStorageVmStorage(dataCenterId, secStorageVmId);
		if (secStorageVm != null) {
			SubscriptionMgr.getInstance().notifySubscribers(ALERT_SUBJECT, this,
				new SecStorageVmAlertEventArgs(
					SecStorageVmAlertEventArgs.SSVM_CREATED,
					dataCenterId, secStorageVmId, secStorageVm, null)
			);
			EventUtils.saveEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventVO.LEVEL_INFO, EventTypes.EVENT_SSVM_CREATE, "Successfully created secondary storage Vm "+ secStorageVm.getName() +" in zone : "+dataCenterId, startEventId);
			return secStorageVm;
		} else {
			if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to allocate secondary storage vm storage, remove the secondary storage vm record from DB, secondary storage vm id: "
					+ secStorageVmId);
            }
			
			SubscriptionMgr.getInstance().notifySubscribers(ALERT_SUBJECT, this,
				new SecStorageVmAlertEventArgs(
					SecStorageVmAlertEventArgs.SSVM_CREATE_FAILURE,
					dataCenterId, secStorageVmId, null, "Unable to allocate storage")
			);
			destroySecStorageVmDBOnly(secStorageVmId);
			EventUtils.saveEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventVO.LEVEL_ERROR, EventTypes.EVENT_SSVM_CREATE, "Failed to create secondary storage Vm in zone : "+dataCenterId, startEventId);
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
	        
	        _secHostUuid = secHost.getGuid();
	        _nfsShare = secHost.getStorageUrl();
	        
	        long id = _secStorageVmDao.getNextInSequence(Long.class, "id");
	        String name = VirtualMachineName.getSystemVmName(id, _instance, "s").intern();
	        Account systemAcct = _accountMgr.getSystemAccount();
	        
	        DataCenterDeployment plan = new DataCenterDeployment(dataCenterId);

	        List<NetworkOfferingVO> defaultOffering = _networkMgr.getSystemAccountNetworkOfferings(NetworkOfferingVO.SystemVmPublicNetwork);
	        List<NetworkOfferingVO> offerings = _networkMgr.getSystemAccountNetworkOfferings(NetworkOfferingVO.SystemVmControlNetwork, NetworkOfferingVO.SystemVmManagementNetwork);
	        List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>(offerings.size() + 1);
	        NicProfile defaultNic = new NicProfile();
	        defaultNic.setDefaultNic(true);
	        defaultNic.setDeviceId(2);
	        try {
    	        networks.add(new Pair<NetworkVO, NicProfile>(_networkMgr.setupNetwork(systemAcct, defaultOffering.get(0), plan, null, null, false).get(0), defaultNic));
                for (NetworkOfferingVO offering : offerings) {
                    networks.add(new Pair<NetworkVO, NicProfile>(_networkMgr.setupNetwork(systemAcct, offering, plan, null, null, false).get(0), null));
                }
	        } catch (ConcurrentOperationException e) {
	            s_logger.info("Unable to setup due to concurrent operation. " + e);
	            return new HashMap<String, Object>();
	        }
	        SecondaryStorageVmVO secStorageVm = new SecondaryStorageVmVO(id, _serviceOffering.getId(), name, _template.getId(), 
	        															 _template.getGuestOSId(), dataCenterId, systemAcct.getDomainId(), systemAcct.getId());
	        try {
	        	secStorageVm = _itMgr.allocate(secStorageVm, _template, _serviceOffering, networks, plan, null, systemAcct);
	        } catch (InsufficientCapacityException e) {
	            s_logger.warn("InsufficientCapacity", e);
	            throw new CloudRuntimeException("Insufficient capacity exception", e);
	        }
	        
	        Map<String, Object> context = new HashMap<String, Object>();
	        context.put("secStorageVmId", secStorageVm.getId());
	        return context;
	    }

	protected SecondaryStorageVmVO allocSecStorageVmStorage(long dataCenterId, long secStorageVmId) {
		SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);
		assert (secStorageVm != null);

		DataCenterVO dc = _dcDao.findById(dataCenterId);
		HostPodVO pod = _podDao.findById(secStorageVm.getPodId());
		
		final AccountVO account = _accountDao.findById(Account.ACCOUNT_ID_SYSTEM);
		
        try {
			List<VolumeVO> vols = _storageMgr.create(account, secStorageVm, _template, dc, pod, _serviceOffering, null,0);
			if( vols == null ){
				s_logger.error("Unable to alloc storage for secondary storage vm");
				return null;
			}
			
			// kick the state machine
			 _itMgr.stateTransitTo(secStorageVm, VirtualMachine.Event.OperationSucceeded, null);
			return secStorageVm;
		} catch (StorageUnavailableException e) {
			s_logger.error("Unable to alloc storage for secondary storage vm: ", e);
			return null;
		} catch (ExecutionException e) {
			s_logger.error("Unable to alloc storage for secondary storage vm: ", e);
			return null;
		}
	}

	private void freePublicIpAddress(String ipAddress, long dcId, long podId) {
		if (_IpAllocator != null && _IpAllocator.exteralIpAddressAllocatorEnabled()) {
			 _IpAllocator.releasePublicIpAddress(ipAddress, dcId, podId);
		} else {
			_ipAddressDao.unassignIpAddress(new Ip(ipAddress));
		}
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
												readysecStorageVm = start(readysecStorageVm.getId());
										} finally {
											secStorageVmLock.unlock();
										}
									} else {
										if (s_logger.isInfoEnabled()) {
                                            s_logger.info("Unable to acquire synchronization lock to start secondary storage vm : " + readysecStorageVm.getName());
                                        }
									}
								} finally {
									secStorageVmLock.releaseRef();
								}
							}
						} else {
							if (s_logger.isInfoEnabled()) {
                                s_logger.info("Unable to acquire synchronization lock to allocate secondary storage vm storage, wait for next turn");
                            }
						}
					} catch (StorageUnavailableException e) {
						s_logger.warn("Storage unavailable", e);
					} catch (InsufficientCapacityException e) {
						s_logger.warn("insuffiient capacity", e);
					} catch (ConcurrentOperationException e) {
						s_logger.debug("Concurrent operation: " + e.getMessage());
					} catch (ResourceUnavailableException e) {
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
						checkPendingSecStorageVMs();
						
						List<DataCenterVO> datacenters = _dcDao.listAllIncludingRemoved();


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
								if(s_logger.isDebugEnabled()) {
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
		List<SecondaryStorageVmVO> l = _secStorageVmDao.getSecStorageVmListInStates(
				dataCenterId, State.Creating, State.Starting, State.Stopped,
				State.Migrating);
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
                    s_logger.info("Unable to start secondary storage vm for standby capacity, secStorageVm vm Id : "
						+ secStorageVmId + ", will recycle it and start a new one");
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
			if ((zoneHostInfo.getFlags() & RunningHostInfoAgregator.ZoneHostInfo.ALL_HOST_MASK) != 0){
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
		if(zoneHostInfo != null && (zoneHostInfo.getFlags() & RunningHostInfoAgregator.ZoneHostInfo.ROUTING_HOST_MASK) != 0) {
	        VMTemplateVO template = _templateDao.findConsoleProxyTemplate();
	        HostVO secHost = _hostDao.findSecondaryStorageHost(dataCenterId);
	        if (secHost == null) {
	        	if (s_logger.isDebugEnabled()) {
                    s_logger.debug("No secondary storage available in zone " + dataCenterId + ", wait until it is ready to launch secondary storage vm");
                }
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
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
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
        if ("true".equalsIgnoreCase(useServiceVM)){
        	_useServiceVM = true;
        }
        
        String sslcopy = configDao.getValue("secstorage.encrypt.copy");
        if ("true".equalsIgnoreCase(sslcopy)) {
        	_useSSlCopy = true;
        }
        
        _allowedInternalSites = configDao.getValue("secstorage.allowed.internal.sites");
        
        String value = configs.get("secstorage.vm.cmd.port");
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

		_listener = new SecondaryStorageListener(this);
		_agentMgr.registerForHostEvents(_listener, true, true, false);

		HighAvailabilityManager haMgr = locator.getManager(HighAvailabilityManager.class);
		if (haMgr != null) {
			haMgr.registerHandler(VirtualMachine.Type.SecondaryStorageVm, this);
		}

		_itMgr.registerGuru(VirtualMachine.Type.SecondaryStorageVm, this);
		 
		
		boolean useLocalStorage = Boolean.parseBoolean(configs.get(Config.SystemVMUseLocalStorage.key()));
		_serviceOffering = new ServiceOfferingVO("System Offering For Secondary Storage VM", 1, _secStorageVmRamSize, 0, 0, 0, true, null, NetworkOffering.GuestIpType.Virtual, useLocalStorage, true, null, true);
		_serviceOffering.setUniqueName("Cloud.com-SecondaryStorage");
		_serviceOffering = _offeringDao.persistSystemServiceOffering(_serviceOffering);
        _template = _templateDao.findConsoleProxyTemplate();
        if (_template == null && _useServiceVM) {
            throw new ConfigurationException("Unable to find the template for secondary storage vm VMs");
        }
 
        if (_useServiceVM) {
        	_capacityScanScheduler.scheduleAtFixedRate(getCapacityScanTask(), STARTUP_DELAY,
				_capacityScanInterval, TimeUnit.MILLISECONDS);
        }
		String configValue = _configDao.getValue("system.vm.use.local.storage");
		_useLocalStorage = Boolean.parseBoolean(configValue);
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
	public void completeStartCommand(SecondaryStorageVmVO vm) {
		 _itMgr.stateTransitTo(vm, VirtualMachine.Event.AgentReportRunning, vm.getHostId());
	}

	@Override
	public void completeStopCommand(SecondaryStorageVmVO vm) {
		completeStopCommand(vm, VirtualMachine.Event.AgentReportStopped);
	}

	@DB
	protected void completeStopCommand(SecondaryStorageVmVO secStorageVm, VirtualMachine.Event ev) {
		Transaction txn = Transaction.currentTxn();
		try {
			txn.start();
			String privateIpAddress = secStorageVm.getPrivateIpAddress();
			if (privateIpAddress != null) {
				secStorageVm.setPrivateIpAddress(null);
				// FIXME: freePrivateIpAddress(privateIpAddress, secStorageVm.getDataCenterId(), secStorageVm.getId());
			}
			String guestIpAddress = secStorageVm.getGuestIpAddress();
			if (guestIpAddress != null) {
				secStorageVm.setGuestIpAddress(null);
				_dcDao.releaseLinkLocalIpAddress(guestIpAddress, secStorageVm.getDataCenterId(), secStorageVm.getId());
			}
			if (! _itMgr.stateTransitTo(secStorageVm, ev, null)) {
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
		} catch (AgentUnavailableException e) {
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

			if (answer != null) {
				if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Successfully reboot secondary storage vm " + secStorageVm.getName());
                }
				
				SubscriptionMgr.getInstance().notifySubscribers(
						ALERT_SUBJECT, this,
						new SecStorageVmAlertEventArgs(
							SecStorageVmAlertEventArgs.SSVM_REBOOTED,
							secStorageVm.getDataCenterId(), secStorageVm.getId(), secStorageVm, null)
					);
				
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
	@DB
	public boolean destroySecStorageVm(long vmId) {
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if (asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Destroy secondary storage vm " + vmId + ", update async job-" + job.getId());
            }
            _asyncMgr.updateAsyncJobAttachment(job.getId(), "secstorage_vm", vmId);
        }
        
		SecondaryStorageVmVO vm = _secStorageVmDao.findById(vmId);
		if (vm == null || vm.getState() == State.Destroyed) {
		    String msg = "Unable to find vm or vm is destroyed: " + vmId;
			if (s_logger.isDebugEnabled()) {
				s_logger.debug(msg);
			}
			return true;
		}
		
		if (s_logger.isDebugEnabled()) {
			s_logger.debug("Destroying secondary storage vm vm " + vmId);
		}

		if (! _itMgr.stateTransitTo(vm, VirtualMachine.Event.DestroyRequested, null)) {
		    String msg = "Unable to destroy the vm because it is not in the correct state: " + vmId;
			s_logger.debug(msg);
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
				if (vm.getPublicIpAddress() != null) {
                    freePublicIpAddress(vm.getPublicIpAddress(), vm.getDataCenterId(), vm.getPodId());
                }
				vm.setPublicIpAddress(null);

				_secStorageVmDao.remove(vm.getId());
	
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
				if (secStorageVm.getPublicIpAddress() != null) {
                    freePublicIpAddress(secStorageVm.getPublicIpAddress(), secStorageVm.getDataCenterId(), secStorageVm.getPodId());
                }

				_secStorageVmDao.remove(vmId);
				EventUtils.saveEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_DESTROY, "Secondary Storage Vm destroyed - " + secStorageVm.getName());
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
	public boolean stop(SecondaryStorageVmVO secStorageVm) throws AgentUnavailableException {
		if (! _itMgr.stateTransitTo(secStorageVm, VirtualMachine.Event.StopRequested, secStorageVm.getHostId())) {
		    String msg = "Unable to stop secondary storage vm: " + secStorageVm.toString();
			s_logger.debug(msg);
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
                                String msg = "Unable to stop due to " + (answer == null ? "answer is null" : answer.getDetails());
                                s_logger.debug(msg);
                                return false;
                            }
                            completeStopCommand(secStorageVm, VirtualMachine.Event.OperationSucceeded);
                            
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
                            event.setDescription("Secondary Storage Vm stopped - " + secStorageVm.getName());
                            _eventDao.persist(event);
						return true;
                        } catch (OperationTimedoutException e) {
                            throw new AgentUnavailableException(secStorageVm.getHostId());
                        }
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
	public boolean migrate(SecondaryStorageVmVO secStorageVm, HostVO host) {
		HostVO fromHost = _hostDao.findById(secStorageVm.getId());

		if (! _itMgr.stateTransitTo(secStorageVm, VirtualMachine.Event.MigrationRequested, secStorageVm.getHostId())) {
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
		CheckVirtualMachineAnswer answer = (CheckVirtualMachineAnswer) _agentMgr.send(host.getId(), cvm);
		if (!answer.getResult()) {
			s_logger.debug("Unable to complete migration for " + secStorageVm.getId());
			 _itMgr.stateTransitTo(secStorageVm, VirtualMachine.Event.AgentReportStopped, null);
			return false;
		}

		State state = answer.getState();
		if (state == State.Stopped) {
			s_logger.warn("Unable to complete migration as we can not detect it on " + host.getId());
			 _itMgr.stateTransitTo(secStorageVm, VirtualMachine.Event.AgentReportStopped, null);
			return false;
		}

		 _itMgr.stateTransitTo(secStorageVm, VirtualMachine.Event.OperationSucceeded, host.getId());
		return true;
	}

	@Override
	public HostVO prepareForMigration(SecondaryStorageVmVO secStorageVm) throws StorageUnavailableException {
		
		VMTemplateVO template = _templateDao.findById(secStorageVm.getTemplateId());
		long routerId = secStorageVm.getId();
		boolean mirroredVols = secStorageVm.isMirroredVols();
		DataCenterVO dc = _dcDao.findById(secStorageVm.getDataCenterId());
		HostPodVO pod = _podDao.findById(secStorageVm.getPodId());
		StoragePoolVO sp = _storageMgr.getStoragePoolForVm(secStorageVm.getId());
 
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
				throw new StorageUnavailableException("Can not share " + vol.getPath() + " to " + secStorageVm.getName(), vol.getPoolId());
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
	public boolean finalizeVirtualMachineProfile(
			VirtualMachineProfile<SecondaryStorageVmVO> profile,
			DeployDestination dest, ReservationContext context) {

		HostVO secHost = _hostDao.findSecondaryStorageHost(dest.getDataCenter().getId());
		assert(secHost != null);
	        
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
        if(externalDhcpStr != null && externalDhcpStr.equalsIgnoreCase("true")) {
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
				if(nic.getIp4Address() != null) {
                    controlNic = nic;
                }
			} else if(nic.getTrafficType() == TrafficType.Public) {
				buf.append(" public.network.device=").append("eth").append(deviceId);
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
		
		DataCenterVO dc = _dcDao.findById(profile.getVirtualMachine().getDataCenterId());
		buf.append(" dns1=").append(dc.getInternalDns1());
		if (dc.getInternalDns2() != null) {
			buf.append(" dns2=").append(dc.getInternalDns2());
		}
		
		String bootArgs = buf.toString();
		if (s_logger.isDebugEnabled()) {
			s_logger.debug("Boot Args for " + profile + ": " + bootArgs);
		}

		if (controlNic == null) {
			throw new CloudRuntimeException("Didn't start a control port");
		}

		profile.setParameter("control.nic", controlNic);
		
		return true;
	}

	@Override
	public boolean finalizeDeployment(Commands cmds,
			VirtualMachineProfile<SecondaryStorageVmVO> profile,
			DeployDestination dest, ReservationContext context) {
		NicProfile controlNic = (NicProfile)profile.getParameter("control.nic");
        CheckSshCommand check = new CheckSshCommand(profile.getInstanceName(), controlNic.getIp4Address(), 3922, 5, 20);
        cmds.addCommand("checkSsh", check);
        
        SecondaryStorageVmVO secVm = profile.getVirtualMachine();
		 List<NicVO> nics = _nicDao.listBy(secVm.getId());
        for (NicVO nic : nics) {
        	NetworkVO network = _networkDao.findById(nic.getNetworkId());
        	if (network.getTrafficType() == TrafficType.Public) {
        		secVm.setPublicIpAddress(nic.getIp4Address());
        		secVm.setPublicNetmask(nic.getNetmask());
        		secVm.setPublicMacAddress(nic.getMacAddress());
        	} else if (network.getTrafficType() == TrafficType.Control) {
        		secVm.setGuestIpAddress(nic.getIp4Address());
        		secVm.setGuestNetmask(nic.getNetmask());
        		secVm.setGuestMacAddress(nic.getMacAddress());
        	} else if (network.getTrafficType() == TrafficType.Management) {
        		secVm.setPrivateIpAddress(nic.getIp4Address());
        		secVm.setPrivateNetmask(nic.getNetmask());
        		secVm.setPrivateMacAddress(nic.getMacAddress());
        	}
        }
        _secStorageVmDao.update(secVm.getId(), secVm);
        return true;
	}

	@Override
	public boolean finalizeStart(Commands cmds,
			VirtualMachineProfile<SecondaryStorageVmVO> profile,
			DeployDestination dest, ReservationContext context) {
		CheckSshAnswer answer = (CheckSshAnswer)cmds.getAnswer("checkSsh");
		if (!answer.getResult()) {
			s_logger.warn("Unable to ssh to the VM: " + answer.getDetails());
			return false;
		}
		
		return true;
	}

	@Override
	public void finalizeStop(
			VirtualMachineProfile<SecondaryStorageVmVO> profile, long hostId,
			String reservationId) {
	}
}
