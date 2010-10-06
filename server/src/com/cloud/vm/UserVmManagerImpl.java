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
package com.cloud.vm;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.AttachVolumeAnswer;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.GetVmStatsAnswer;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.ManageSnapshotAnswer;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.storage.CreatePrivateTemplateAnswer;
import com.cloud.agent.api.storage.CreatePrivateTemplateCommand;
import com.cloud.alert.AlertManager;
import com.cloud.api.BaseCmd;
import com.cloud.async.AsyncJobExecutor;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.async.executor.DestroyVMExecutor;
import com.cloud.async.executor.RebootVMExecutor;
import com.cloud.async.executor.StartVMExecutor;
import com.cloud.async.executor.StopVMExecutor;
import com.cloud.async.executor.VMExecutorHelper;
import com.cloud.async.executor.VMOperationListener;
import com.cloud.async.executor.VMOperationParam;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventState;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.DetailsDao;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.FirewallRuleVO;
import com.cloud.network.IPAddressVO;
import com.cloud.network.IpAddrAllocator;
import com.cloud.network.LoadBalancerVMMapVO;
import com.cloud.network.NetworkManager;
import com.cloud.network.SecurityGroupVMMapVO;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.SecurityGroupDao;
import com.cloud.network.dao.SecurityGroupVMMapDao;
import com.cloud.network.security.NetworkGroupManager;
import com.cloud.network.security.NetworkGroupVO;
import com.cloud.pricing.dao.PricingDao;
import com.cloud.server.ManagementServer;
import com.cloud.service.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.ServiceOffering.GuestIpType;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Snapshot.SnapshotType;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VirtualMachineTemplate.BootloaderType;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.DiskTemplateDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserStatisticsDao;
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
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouter.Role;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;

@Local(value={UserVmManager.class})
public class UserVmManagerImpl implements UserVmManager {
    private static final Logger s_logger = Logger.getLogger(UserVmManagerImpl.class);
	private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 3; 	// 3 seconds

    @Inject HostDao _hostDao = null;
    @Inject DetailsDao _detailsDao = null;
    @Inject DomainRouterDao _routerDao = null;
    @Inject ServiceOfferingDao _offeringDao = null;
    @Inject DiskOfferingDao _diskOfferingDao = null;
    @Inject UserStatisticsDao _userStatsDao = null;
    @Inject VMTemplateDao _templateDao =  null;
    @Inject VMTemplateHostDao _templateHostDao = null;
    @Inject DiskTemplateDao _diskDao = null;
    @Inject DomainDao _domainDao = null;
    @Inject ResourceLimitDao _limitDao = null;
    @Inject UserVmDao _vmDao = null;
    @Inject VolumeDao _volsDao = null;
    @Inject DataCenterDao _dcDao = null;
    @Inject FirewallRulesDao _rulesDao = null;
    @Inject SecurityGroupDao _securityGroupDao = null;
    @Inject SecurityGroupVMMapDao _securityGroupVMMapDao = null;
    @Inject LoadBalancerVMMapDao _loadBalancerVMMapDao = null;
    @Inject LoadBalancerDao _loadBalancerDao = null;
    @Inject IPAddressDao _ipAddressDao = null;
    @Inject HostPodDao _podDao = null;
    @Inject PricingDao _pricingDao = null;
    @Inject CapacityDao _capacityDao = null;
    @Inject NetworkManager _networkMgr = null;
    @Inject StorageManager _storageMgr = null;
    @Inject SnapshotManager _snapshotMgr = null;
    @Inject AgentManager _agentMgr = null;
    @Inject AccountDao _accountDao = null;
    @Inject UserDao _userDao = null;
    @Inject SnapshotDao _snapshotDao = null;
    @Inject GuestOSDao _guestOSDao = null;
    @Inject GuestOSCategoryDao _guestOSCategoryDao = null;
    @Inject HighAvailabilityManager _haMgr = null;
    @Inject AlertManager _alertMgr = null;
    @Inject AccountManager _accountMgr;
    @Inject AsyncJobManager _asyncMgr;
    @Inject protected StoragePoolHostDao _storagePoolHostDao;
    @Inject VlanDao _vlanDao;
    @Inject StoragePoolDao _storagePoolDao;
    @Inject VMTemplateHostDao _vmTemplateHostDao;
    @Inject NetworkGroupManager _networkGroupManager;
    @Inject ServiceOfferingDao _serviceOfferingDao;
    @Inject EventDao _eventDao = null;
    private IpAddrAllocator _IpAllocator;
    ScheduledExecutorService _executor = null;
    int _expungeInterval;
    int _expungeDelay;
    int _retry = 2;

    String _name;
    String _instance;
    String _zone;

    Random _rand = new Random(System.currentTimeMillis());

    private ConfigurationDao _configDao;

	int _userVMCap = 0;
    final int _maxWeight = 256;

    @Override
    public UserVmVO getVirtualMachine(long vmId) {
        return _vmDao.findById(vmId);
    }

    @Override
    public List<? extends UserVm> getVirtualMachines(long hostId) {
        return _vmDao.listByHostId(hostId);
    }

    @Override
    public boolean resetVMPassword(long userId, long vmId, String password) {
        UserVmVO vm = _vmDao.findById(vmId);
        VMTemplateVO template = _templateDao.findById(vm.getTemplateId());
        if (template.getEnablePassword()) {
        	if (vm.getDomainRouterId() == null)
        		/*TODO: add it for external dhcp mode*/
        		return true;
	        if (_networkMgr.savePasswordToRouter(vm.getDomainRouterId(), vm.getPrivateIpAddress(), password)) {
	            // Need to reboot the virtual machine so that the password gets redownloaded from the DomR, and reset on the VM
	        	if (!rebootVirtualMachine(userId, vmId)) {
	        		if (vm.getState() == State.Stopped) {
	        			return true;
	        		}
	        		return false;
	        	} else {
	        		return true;
	        	}
	        } else {
	        	return false;
	        }
        } else {
        	if (s_logger.isDebugEnabled()) {
        		s_logger.debug("Reset password called for a vm that is not using a password enabled template");
        	}
        	return false;
        }
    }
    
    @Override
    public void attachVolumeToVM(long vmId, long volumeId, Long deviceId, long startEventId) throws InternalErrorException {
    	VolumeVO volume = _volsDao.findById(volumeId);
    	UserVmVO vm = _vmDao.findById(vmId);
    	
        EventVO event = new EventVO();
        event.setType(EventTypes.EVENT_VOLUME_ATTACH);
        event.setUserId(1L);
        event.setAccountId(volume.getAccountId());
        event.setState(EventState.Started);
        event.setStartId(startEventId);
        event.setDescription("Attaching volume: "+volumeId+" to Vm: "+vmId);
        _eventDao.persist(event);
        
        VolumeVO rootVolumeOfVm = null;
        List<VolumeVO> rootVolumesOfVm = _volsDao.findByInstanceAndType(vmId, VolumeType.ROOT);
        if (rootVolumesOfVm.size() != 1) {
        	throw new InternalErrorException("The VM " + vm.getName() + " has more than one ROOT volume and is in an invalid state. Please contact Cloud Support.");
        } else {
        	rootVolumeOfVm = rootVolumesOfVm.get(0);
        }
        List<VolumeVO> vols = _volsDao.findByInstance(vmId);
        if( deviceId != null ) {
            if( deviceId.longValue() > 15 || deviceId.longValue() == 0 || deviceId.longValue() == 3) {
                throw new RuntimeException("deviceId should be 1,2,4-15");
            }
            for (VolumeVO vol : vols) {
                if (vol.getDeviceId().equals(deviceId)) {
                    throw new RuntimeException("deviceId " + deviceId + " is used by VM " + vm.getName());
                }
            }           
        } else {
            // allocate deviceId here
            List<String> devIds = new ArrayList<String>();
            for( int i = 1; i < 15; i++ ) {
                devIds.add(String.valueOf(i));
            }
            devIds.remove("3");
            for (VolumeVO vol : vols) {
                devIds.remove(vol.getDeviceId().toString().trim());
            } 
            deviceId = Long.parseLong(devIds.iterator().next());        
        }
        
        StoragePoolVO vmRootVolumePool = _storagePoolDao.findById(rootVolumeOfVm.getPoolId());
        DiskOfferingVO volumeDiskOffering = _diskOfferingDao.findById(volume.getDiskOfferingId());
        String[] volumeTags = volumeDiskOffering.getTagsArray();
        
        StoragePoolVO sourcePool = _storagePoolDao.findById(volume.getPoolId());
        List<StoragePoolVO> sharedVMPools = _storagePoolDao.findPoolsByTags(vmRootVolumePool.getDataCenterId(), vmRootVolumePool.getPodId(), vmRootVolumePool.getClusterId(), volumeTags, true);
        boolean moveVolumeNeeded = true;
        if (sharedVMPools.size() == 0) {
        	String poolType;
        	if (vmRootVolumePool.getClusterId() != null) {
        		poolType = "cluster";
        	} else if (vmRootVolumePool.getPodId() != null) {
        		poolType = "pod";
        	} else {
        		poolType = "zone";
        	}
        	throw new InternalErrorException("There are no storage pools in the VM's " + poolType + " with all of the volume's tags (" + volumeDiskOffering.getTags() + ").");
        } else {
        	Long sourcePoolDcId = sourcePool.getDataCenterId();
        	Long sourcePoolPodId = sourcePool.getPodId();
        	Long sourcePoolClusterId = sourcePool.getClusterId();
        	for (StoragePoolVO vmPool : sharedVMPools) {
        		Long vmPoolDcId = vmPool.getDataCenterId();
        		Long vmPoolPodId = vmPool.getPodId();
        		Long vmPoolClusterId = vmPool.getClusterId();
        		
        		if (sourcePoolDcId == vmPoolDcId && sourcePoolPodId == vmPoolPodId && sourcePoolClusterId == vmPoolClusterId) {
        			moveVolumeNeeded = false;
        			break;
        		}
        	}
        }
       
    	if (moveVolumeNeeded) {
    		// Move the volume to a storage pool in the VM's zone, pod, or cluster
    		volume = _storageMgr.moveVolume(volume, vmRootVolumePool.getDataCenterId(), vmRootVolumePool.getPodId(), vmRootVolumePool.getClusterId());
    	}
    	
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if(asyncExecutor != null) {
        	AsyncJobVO job = asyncExecutor.getJob();

        	if(s_logger.isInfoEnabled())
        		s_logger.info("Trying to attaching volume " + volumeId +" to vm instance:"+vm.getId()+ ", update async job-" + job.getId() + " progress status");
        	
        	_asyncMgr.updateAsyncJobAttachment(job.getId(), "volume", volumeId);
        	_asyncMgr.updateAsyncJobStatus(job.getId(), BaseCmd.PROGRESS_INSTANCE_CREATED, volumeId);
        }
    	
    	String errorMsg = "Failed to attach volume: " + volume.getName() + " to VM: " + vm.getName();
    	boolean sendCommand = (vm.getState() == State.Running);
    	AttachVolumeAnswer answer = null;
    	Long hostId = vm.getHostId();
    	if (sendCommand) {
    		AttachVolumeCommand cmd = new AttachVolumeCommand(true, vm.getInstanceName(), volume.getPoolType(), volume.getFolder(), volume.getPath(), volume.getName(), deviceId);
    		
    		try {
    			answer = (AttachVolumeAnswer)_agentMgr.send(hostId, cmd);
    		} catch (Exception e) {
    			throw new InternalErrorException(errorMsg + " due to: " + e.getMessage());
    		}
    	}

    	event = new EventVO();
        event.setAccountId(volume.getAccountId());
        event.setUserId(1L);
        event.setType(EventTypes.EVENT_VOLUME_ATTACH);
        event.setState(EventState.Completed);
        event.setStartId(startEventId);
        if (!sendCommand || (answer != null && answer.getResult())) {
    		// Mark the volume as attached
            if( sendCommand ) {
                _volsDao.attachVolume(volume.getId(), vmId, answer.getDeviceId());
            } else {
                _volsDao.attachVolume(volume.getId(), vmId, deviceId);
            }
            if(!vm.getName().equals(vm.getDisplayName()))
            	event.setDescription("Volume: " +volume.getName()+ " successfully attached to VM: "+vm.getName()+"("+vm.getDisplayName()+")");
            else
            	event.setDescription("Volume: " +volume.getName()+ " successfully attached to VM: "+vm.getName());
            event.setLevel(EventVO.LEVEL_INFO);
            _eventDao.persist(event);
    	} else {
    		if (answer != null) {
    			String details = answer.getDetails();
    			if (details != null && !details.isEmpty())
    				errorMsg += "; " + details;
    		}
            event.setDescription(errorMsg);
            event.setLevel(EventVO.LEVEL_ERROR);
            _eventDao.persist(event);
    		throw new InternalErrorException(errorMsg);
    	}
    }
    
    @Override
    public void detachVolumeFromVM(long volumeId, long startEventId) throws InternalErrorException {
    	VolumeVO volume = _volsDao.findById(volumeId);
    	
    	Long vmId = volume.getInstanceId();
    	
    	if (vmId == null) {
    		return;
    	}
    	
        EventVO event = new EventVO();
        event.setType(EventTypes.EVENT_VOLUME_DETACH);
        event.setUserId(1L);
        event.setAccountId(volume.getAccountId());
        event.setState(EventState.Started);
        event.setStartId(startEventId);
        event.setDescription("Detaching volume: "+volumeId+" from Vm: "+vmId);
        _eventDao.persist(event);
    	
    	UserVmVO vm = _vmDao.findById(vmId);
    	
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if(asyncExecutor != null) {
        	AsyncJobVO job = asyncExecutor.getJob();

        	if(s_logger.isInfoEnabled())
        		s_logger.info("Trying to attaching volume " + volumeId +"to vm instance:"+vm.getId()+ ", update async job-" + job.getId() + " progress status");
        	
        	_asyncMgr.updateAsyncJobAttachment(job.getId(), "volume", volumeId);
        	_asyncMgr.updateAsyncJobStatus(job.getId(), BaseCmd.PROGRESS_INSTANCE_CREATED, volumeId);
        }
    	
    	String errorMsg = "Failed to detach volume: " + volume.getName() + " from VM: " + vm.getName();
    	boolean sendCommand = (vm.getState() == State.Running);
    	Answer answer = null;
    	
    	if (sendCommand) {
			AttachVolumeCommand cmd = new AttachVolumeCommand(false, vm.getInstanceName(), volume.getPoolType(), volume.getFolder(), volume.getPath(), volume.getName(), volume.getDeviceId());
			
			try {
    			answer = _agentMgr.send(vm.getHostId(), cmd);
    		} catch (Exception e) {
    			throw new InternalErrorException(errorMsg + " due to: " + e.getMessage());
    		}
    	}
    	
        event = new EventVO();
        event.setAccountId(volume.getAccountId());
        event.setUserId(1L);
        event.setType(EventTypes.EVENT_VOLUME_DETACH);
        event.setState(EventState.Completed);
        event.setStartId(startEventId);
		if (!sendCommand || (answer != null && answer.getResult())) {
			// Mark the volume as detached
    		_volsDao.detachVolume(volume.getId());
            if(!vm.getName().equals(vm.getDisplayName()))
            	event.setDescription("Volume: " +volume.getName()+ " successfully detached from VM: "+vm.getName()+"("+vm.getDisplayName()+")");
            else
            	event.setDescription("Volume: " +volume.getName()+ " successfully detached from VM: "+vm.getName());
            event.setLevel(EventVO.LEVEL_INFO);
            _eventDao.persist(event);
    	} else {
    		
    		if (answer != null) {
    			String details = answer.getDetails();
    			if (details != null && !details.isEmpty())
    				errorMsg += "; " + details;
    		}
    		
            event.setDescription(errorMsg);
            event.setLevel(EventVO.LEVEL_ERROR);
            _eventDao.persist(event);
    		throw new InternalErrorException(errorMsg);
    	}
    }
    
    @Override
    public boolean attachISOToVM(long vmId, long isoId, boolean attach) {
    	UserVmVO vm = _vmDao.findById(vmId);
    	
    	if (vm == null) {
            return false;
    	} else if (vm.getState() != State.Running) {
    		return true;
    	}

        // Get the path of the ISO
    	String isoPath = _storageMgr.getAbsoluteIsoPath(isoId, vm.getDataCenterId());
    	String isoName = _templateDao.findById(isoId).getName();
    	
	    if (isoPath == null) {
	        // we can't send a null path to the ServerResource, so return false if we are unable to find the isoPath
	    	if (isoName.startsWith("xs-tools"))
	    		isoPath = isoName;
	    	else
	    		return false;
	    }

    	String vmName = vm.getInstanceName();

    	HostVO host = _hostDao.findById(vm.getHostId());
    	if (host == null)
    		return false;

    	AttachIsoCommand cmd = new AttachIsoCommand(vmName, isoPath, attach);
    	Answer a = _agentMgr.easySend(vm.getHostId(), cmd);
    	return (a != null);
    }

    @Override
    public UserVmVO startVirtualMachine(long userId, long vmId, String isoPath) throws ExecutionException, StorageUnavailableException, ConcurrentOperationException {
        return startVirtualMachine(userId, vmId, null, isoPath);
    }
    
    @Override
    public boolean executeStartVM(StartVMExecutor executor, VMOperationParam param) {
    	// TODO following implementation only do asynchronized operation at API level
        try {
            UserVmVO vm = start(param.getUserId(), param.getVmId(), null, param.getIsoPath(), param.getEventId());
            if(vm != null)
	        	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
	        		AsyncJobResult.STATUS_SUCCEEDED, 0, VMExecutorHelper.composeResultObject(
	        			executor.getAsyncJobMgr().getExecutorContext().getManagementServer(), vm, null));
            else
            	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
            		AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, "Unable to start vm");
        } catch (StorageUnavailableException e) {
            s_logger.debug("Unable to start vm because storage is unavailable: " + e.getMessage());
            
        	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
        		AsyncJobResult.STATUS_FAILED, BaseCmd.VM_ALLOCATION_ERROR, "Unable to start vm because storage is unavailable");
        } catch (ConcurrentOperationException e) {
        	s_logger.debug(e.getMessage());
        	
        	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
        		AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, e.getMessage());
		} catch (ExecutionException e) {
       	s_logger.debug(e.getMessage());
        	
        	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
        		AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, e.getMessage());
		}
        return true;
    }

    @Override
    public UserVmVO startVirtualMachine(long userId, long vmId, String password, String isoPath) throws ExecutionException, StorageUnavailableException, ConcurrentOperationException {
        try {
            return start(userId, vmId, password, isoPath, 0);
        } catch (StorageUnavailableException e) {
            s_logger.debug("Unable to start vm because storage is unavailable: " + e.getMessage());
            throw e;
        } catch (ConcurrentOperationException e) {
        	s_logger.debug(e.getMessage());
        	throw e;
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
        	s_logger.debug(e.getMessage());
        	throw e;
		}
    }

    @DB
    protected UserVmVO start(long userId, long vmId, String password, String isoPath, long startEventId) throws StorageUnavailableException, ConcurrentOperationException, ExecutionException {
        
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null || vm.getRemoved() != null) {
            s_logger.debug("Unable to find " + vmId);
            return null;
        }
        
        EventVO event = new EventVO();
        event.setType(EventTypes.EVENT_VM_START);
        event.setUserId(userId);
        event.setAccountId(vm.getAccountId());
        event.setState(EventState.Started);
        event.setDescription("Starting Vm with Id: "+vmId);
        event.setStartId(startEventId);
        event = _eventDao.persist(event);
        
        //if there was no schedule event before, set start event as startEventId
        if(startEventId == 0 && event != null){
            startEventId = event.getId();
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Starting VM: " + vmId);
        }

        State state = vm.getState();
        if (state == State.Running) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Starting an already started VM: " + vm.getId() + " - " + vm.getName() + "; state = " + vm.getState().toString());
            }
            return vm;
        }

        if (state.isTransitional()) {
        	throw new ConcurrentOperationException("Concurrent operations on the vm " + vm.getId() + " - " + vm.getName() + "; state = " + state.toString());
        }
        
        DataCenterVO dc = _dcDao.findById(vm.getDataCenterId());
        HostPodVO pod = _podDao.findById(vm.getPodId());
        List<StoragePoolVO> sps = _storageMgr.getStoragePoolsForVm(vm.getId());
        StoragePoolVO sp = sps.get(0); // FIXME

        VMTemplateVO template = _templateDao.findById(vm.getTemplateId());
        ServiceOffering offering = _offeringDao.findById(vm.getServiceOfferingId());
        
        long diskOfferingId = -1;
        
        // If an ISO path is passed in, boot from that ISO
        // Else, check if the VM already has an ISO attached to it. If so, start the VM with that ISO inserted, but don't boot from it.
        boolean bootFromISO = false;
        if (isoPath != null) {
        	bootFromISO = true;
        } else {
            Long isoId = vm.getIsoId();
            if (isoId != null) {
                isoPath = _storageMgr.getAbsoluteIsoPath(isoId, vm.getDataCenterId());
            }
        }
        
        // Determine the VM's OS description
        String guestOSDescription;
        GuestOSVO guestOS = _guestOSDao.findById(vm.getGuestOSId());
        if (guestOS == null) {
        	s_logger.debug("Could not find guest OS description for vm: " + vm.getName());
        	return null;
        } else {
        	guestOSDescription = guestOS.getName();
        }

        HashSet<Host> avoid = new HashSet<Host>();

        HostVO host = (HostVO) _agentMgr.findHost(Host.Type.Routing, dc, pod, sp, offering, template, vm, null, avoid);

        if (host == null) {
            s_logger.error("Unable to find any host for " + vm.toString());
            return null;
        }

        if (!_vmDao.updateIf(vm, Event.StartRequested, host.getId())) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to start VM " + vm.toString() + " because the state is not correct.");
            }
            return null;
        }
        
        boolean started = false;
        Transaction txn = Transaction.currentTxn();
        try {
            String eventParams = "id=" + vm.getId() + "\nvmName=" + vm.getName() + "\nsoId=" + vm.getServiceOfferingId() + "\ndoId=" + diskOfferingId + "\ntId=" + vm.getTemplateId() + "\ndcId=" + vm.getDataCenterId();
            event = new EventVO();
            event.setType(EventTypes.EVENT_VM_START);
            event.setUserId(userId);
            event.setAccountId(vm.getAccountId());
            event.setParameters(eventParams);
            event.setState(EventState.Completed);
            event.setStartId(startEventId);
            
            String vnet = null;
            DomainRouterVO router = null;
            if (vm.getDomainRouterId() != null) {
                router = _networkMgr.addVirtualMachineToGuestNetwork(vm, password);
            	if (router == null) {
            		s_logger.error("Unable to add vm " + vm.getId() + " - " + vm.getName());
            		_vmDao.updateIf(vm, Event.OperationFailed, null);
                    if(!vm.getName().equals(vm.getDisplayName()))
                		event.setDescription("Unable to start VM: " + vm.getName()+"("+vm.getDisplayName()+")" + "; Unable to add VM to guest network");
                    else
                		event.setDescription("Unable to start VM: " + vm.getName() + "; Unable to add VM to guest network");

            		event.setLevel(EventVO.LEVEL_ERROR);
            		_eventDao.persist(event);
            		return null;
            	}

            	vnet = router.getVnet();
            	if(NetworkManager.USE_POD_VLAN){
            		if(vm.getPodId() != router.getPodId()){
            			//VM is in a different Pod
            			if(router.getZoneVlan() == null){
            				//Create Zone Vlan if not created already
            				vnet = _networkMgr.createZoneVlan(router);
            				if (vnet == null) {
            					s_logger.error("Vlan creation failed. Unable to add vm " + vm.getId() + " - " + vm.getName());
            					return null;
            				}
            			} else {
            				//Use existing zoneVlan
            				vnet = router.getZoneVlan();
            			}
            		}
            	}
            }

	        boolean mirroredVols = vm.isMirroredVols();
	        
	        List<VolumeVO> rootVols = _volsDao.findByInstanceAndType(vm.getId(), VolumeType.ROOT);
	        assert rootVols.size() == 1 : "How can we get " + rootVols.size() + " root volume for " + vm.getId();
	        
	        String [] storageIps = new String[2];
	        VolumeVO vol = rootVols.get(0);

	        List<VolumeVO> vols = _volsDao.findCreatedByInstance(vm.getId());

            Answer answer = null;
            int retry = _retry;

            do {

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Trying to start vm " + vm.getName() + " on host " + host.toString());
                }
                txn.start();
                
           	 	
                if (vm.getDomainRouterId() != null) {
                	vm.setVnet(vnet);
                	vm.setInstanceName(VirtualMachineName.attachVnet(vm.getName(), vm.getVnet()));
                } else {
                	vm.setVnet("untagged");
                }
                	
                	
                vm.setStorageIp(storageIps[0]);

                if( retry < _retry) {
                    if (!_vmDao.updateIf(vm, Event.OperationRetry, host.getId())) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Unable to start VM " + vm.toString() + " because the state is not correct.");
                        }
                        return null;
                    }
                }

                txn.commit();

                if( !_storageMgr.share(vm, vols, host, true) ) {
                	s_logger.debug("Unable to share volumes to host " + host.toString());
                	continue;
                }

                int utilization = _userVMCap; //cpu_cap
                //Configuration cpu.uservm.cap is not available in default installation. Using this parameter is not encouraged
                
                int cpuWeight = _maxWeight; //cpu_weight
                
                // weight based allocation
                cpuWeight = (int)((offering.getSpeed()*0.99) / (float)host.getSpeed() * _maxWeight);
                if (cpuWeight > _maxWeight) {
                	cpuWeight = _maxWeight;
                }

                int bits;
                if (template == null) {
                	bits = 64;
                } else {
                	bits = template.getBits();
                }

                StartCommand cmdStart = new StartCommand(vm, vm.getInstanceName(), offering, offering.getRateMbps(), offering.getMulticastRateMbps(), router, storageIps, vol.getFolder(), vm.getVnet(), utilization, cpuWeight, vols, mirroredVols, bits, isoPath, bootFromISO, guestOSDescription);
                if (Storage.ImageFormat.ISO.equals(template.getFormat()) || template.isRequiresHvm()) {
                	cmdStart.setBootloader(BootloaderType.HVM);
                }

                if (vm.getExternalVlanDbId() != null) {
                	final VlanVO externalVlan = _vlanDao.findById(vm.getExternalVlanDbId());
                	cmdStart.setExternalVlan(externalVlan.getVlanId());
                	cmdStart.setExternalMacAddress(vm.getExternalMacAddress());
                }

                try {
	                answer = _agentMgr.send(host.getId(), cmdStart);
	                if (answer.getResult()) {
	                    if (s_logger.isDebugEnabled()) {
	                        s_logger.debug("Started vm " + vm.getName() + " on host " + host.toString());
	                    }
	                    started = true;
	                    break;
	                }
	                
	                s_logger.debug("Unable to start " + vm.toString() + " on host " + host.toString() + " due to " + answer.getDetails());
                } catch (OperationTimedoutException e) {
                	if (e.isActive()) {
                		s_logger.debug("Unable to start vm " + vm.getName() + " due to operation timed out and it is active so scheduling a restart.");
                		_haMgr.scheduleRestart(vm, true);
                		host = null;
                		return null;
                	}
                } catch (AgentUnavailableException e) {
                	s_logger.debug("Agent " + host.toString() + " was unavailable to start VM " + vm.getName());
                }

                avoid.add(host);

                _storageMgr.unshare(vm, vols, host);
            } while (--retry > 0 && (host = (HostVO)_agentMgr.findHost(Host.Type.Routing, dc, pod, sp, offering, template, vm, null, avoid)) != null);

            if (host == null || retry <= 0) {
                if(!vm.getName().equals(vm.getDisplayName()))
                    event.setDescription("Unable to start VM: " + vm.getName()+"("+vm.getDisplayName()+")"+ " Reason: "+answer.getDetails());
                else
                    event.setDescription("Unable to start VM: " + vm.getName()+ " Reason: "+answer.getDetails());
            	
                event.setLevel(EventVO.LEVEL_ERROR);
                _eventDao.persist(event);
                throw new ExecutionException("Unable to start VM: " + vm.getName()+ " Reason: "+answer.getDetails());
            }

            if (!_vmDao.updateIf(vm, Event.OperationSucceeded, host.getId())) {
                if(!vm.getName().equals(vm.getDisplayName()))
                	event.setDescription("unable to start VM: " + vm.getName()+"("+vm.getDisplayName()+")");
                else
                	event.setDescription("unable to start VM: " + vm.getName());
                event.setLevel(EventVO.LEVEL_ERROR);
                _eventDao.persist(event);
            	throw new ConcurrentOperationException("Starting vm " + vm.getName() + " didn't work.");
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Started vm " + vm.getName());
            }

            if(!vm.getName().equals(vm.getDisplayName()))
            	event.setDescription("successfully started VM: " + vm.getName()+"("+vm.getDisplayName()+")");
            else
            	event.setDescription("successfully started VM: " + vm.getName());
            
            _eventDao.persist(event);
            _networkGroupManager.handleVmStateTransition(vm, State.Running);

            return _vmDao.findById(vm.getId());
        } catch (Throwable th) {
            txn.rollback();
            s_logger.error("While starting vm " + vm.getName() + ", caught throwable: ", th);

            if (!started) {
	            vm.setVnet(null);
	            vm.setStorageIp(null);

	            txn.start();
	            if (_vmDao.updateIf(vm, Event.OperationFailed, null)) {
		            txn.commit();
	            }
            }

            if (th instanceof StorageUnavailableException) {
            	throw (StorageUnavailableException)th;
            }
            if (th instanceof ConcurrentOperationException) {
            	throw (ConcurrentOperationException)th;
            }
            if (th instanceof ExecutionException) {
            	s_logger.warn(th.getMessage());
            	throw (ExecutionException)th;
            }
            return null;
        }
    }

    @Override
    public boolean stopVirtualMachine(long userId, long vmId) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Stopping vm=" + vmId);
        }

        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null || vm.getRemoved() != null) {
        	if (s_logger.isDebugEnabled()) {
        		s_logger.debug("VM is either removed or deleted.");
        	}
    		return true;
        }
        
        return stop(userId, vm, 0);
    }
    
    @Override
    public boolean executeStopVM(final StopVMExecutor executor, final VMOperationParam param) {
        final UserVmVO vm = _vmDao.findById(param.getVmId());
        if (vm == null || vm.getRemoved() != null) {
        	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
        		AsyncJobResult.STATUS_SUCCEEDED, 0, "VM is either removed or deleted");
            	
        	if(s_logger.isDebugEnabled())
        		s_logger.debug("Execute asynchronize stop VM command: VM is either removed or deleted");
        	return true;
        }
        
        State state = vm.getState();
        if (state == State.Stopped) {
        	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
        		AsyncJobResult.STATUS_SUCCEEDED, 0, "VM is already stopped");
        	
        	if(s_logger.isDebugEnabled())
        		s_logger.debug("Execute asynchronize stop VM command: VM is already stopped");
            return true;
        }
        
        if (state == State.Creating || state == State.Destroyed || state == State.Expunging) {
        	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
        		AsyncJobResult.STATUS_SUCCEEDED, 0, "VM is not in a stoppable state");
            	
        	if(s_logger.isDebugEnabled())
        		s_logger.debug("Execute asynchronize stop VM command: VM is not in a stoppable state");
        	return true;
        }
        
        if (!_vmDao.updateIf(vm, Event.StopRequested, vm.getHostId())) {
        	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
            		AsyncJobResult.STATUS_FAILED, 0, "VM is not in a state to stop");
                	
        	if(s_logger.isDebugEnabled())
        		s_logger.debug("Execute asynchronize stop VM command: VM is not in a state to stop");
            return true;
        }
        
        if (vm.getHostId() == null) {
        	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
            		AsyncJobResult.STATUS_FAILED, 0, "VM host is null (invalid VM)");
                	
        	if(s_logger.isDebugEnabled())
        		s_logger.debug("Execute asynchronize stop VM command: VM host is null (invalid VM)");
            return true;
        }
        
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if(asyncExecutor != null) {
        	AsyncJobVO job = asyncExecutor.getJob();
        	_asyncMgr.updateAsyncJobAttachment(job.getId(), "vm_instance", vm.getId());
        }
        
        StopCommand cmd = new StopCommand(vm, vm.getInstanceName(), vm.getVnet());
        try {
			long seq = _agentMgr.send(vm.getHostId(), new Command[] {cmd}, true,
				new VMOperationListener(executor, param, vm, 0));
			
        	if(s_logger.isDebugEnabled())
        		s_logger.debug("Execute asynchronize stop VM command: sending command to agent, seq - " + seq);
			
			return false;
		} catch (AgentUnavailableException e) {
        	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
        		AsyncJobResult.STATUS_FAILED, 0, "Agent is not available");
            _vmDao.updateIf(vm, Event.OperationFailed, vm.getHostId());
            
            try {
	            EventVO event = new EventVO();
	            event.setUserId(param.getUserId());
	            event.setAccountId(vm.getAccountId());
	            event.setType(EventTypes.EVENT_VM_STOP);
	            event.setState(EventState.Completed);
	            event.setStartId(param.getEventId());
	            event.setParameters("id="+vm.getId() + "\nvmName=" + vm.getName() + "\nsoId=" + vm.getServiceOfferingId() + "\ntId=" + vm.getTemplateId() + "\ndcId=" + vm.getDataCenterId());
	            event.setDescription("failed to stop VM instance : " + vm.getName());
	            event.setLevel(EventVO.LEVEL_ERROR);
	            _eventDao.persist(event);
            } catch(Exception ex) {
            	s_logger.warn("Unable to save event due to unexpected exception, ", ex);
            }
        	return true;
		}
    }

    @Override
    public boolean rebootVirtualMachine(long userId, long vmId) {
        UserVmVO vm = _vmDao.findById(vmId);

        if (vm == null || vm.getState() == State.Destroyed || vm.getState() == State.Expunging || vm.getRemoved() != null) {
            return false;
        }

        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(vm.getAccountId());
        event.setType(EventTypes.EVENT_VM_REBOOT);
        event.setParameters("id="+vm.getId() + "\nvmName=" + vm.getName() + "\nsoId=" + vm.getServiceOfferingId() + "\ntId=" + vm.getTemplateId() + "\ndcId=" + vm.getDataCenterId());

        if (vm.getState() == State.Running && vm.getHostId() != null) {
            RebootCommand cmd = new RebootCommand(vm.getInstanceName());
            RebootAnswer answer = (RebootAnswer)_agentMgr.easySend(vm.getHostId(), cmd);
           
            if (answer != null) {
            	if(!vm.getName().equals(vm.getDisplayName()))
            		event.setDescription("Successfully rebooted VM instance : " + vm.getName()+"("+vm.getDisplayName()+")");
            	else
            		event.setDescription("Successfully rebooted VM instance : " + vm.getName());
                _eventDao.persist(event);
                return true;
            } else {
            	if(!vm.getName().equals(vm.getDisplayName()))
            		event.setDescription("failed to reboot VM instance : " + vm.getName()+"("+vm.getDisplayName()+")");
            	else
            		event.setDescription("failed to reboot VM instance : " + vm.getName());
                event.setLevel(EventVO.LEVEL_ERROR);
                _eventDao.persist(event);
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean executeRebootVM(RebootVMExecutor executor, VMOperationParam param) {
    	
        final UserVmVO vm = _vmDao.findById(param.getVmId());
        if (vm == null || vm.getState() == State.Destroyed || vm.getState() == State.Expunging || vm.getRemoved() != null) {
        	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
        		AsyncJobResult.STATUS_FAILED, 0, "VM does not exist or in destroying state");
        	
        	if(s_logger.isDebugEnabled())
        		s_logger.debug("Execute asynchronize Reboot VM command: VM does not exist or in destroying state");
        	return true;
        }
        
        if (vm.getState() == State.Running && vm.getHostId() != null) {
            RebootCommand cmd = new RebootCommand(vm.getInstanceName());
            try {
				long seq = _agentMgr.send(vm.getHostId(), new Command[] {cmd}, true,
					new VMOperationListener(executor, param, vm, 0));
				
            	if(s_logger.isDebugEnabled())
            		s_logger.debug("Execute asynchronize Reboot VM command: sending command to agent, seq - " + seq);
				return false;
			} catch (AgentUnavailableException e) {
	        	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
            		AsyncJobResult.STATUS_FAILED, 0, "Agent is not available");
	        	return true;
			}
        }
        
    	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
    		AsyncJobResult.STATUS_FAILED, 0, "VM is not running or agent host is disconnected");
    	return true;
    }

    @Override
    public boolean upgradeVirtualMachine(long vmId, long serviceOfferingId) {
        UserVmVO vm = _vmDao.createForUpdate(vmId);
        vm.setServiceOfferingId(serviceOfferingId);
        vm.setHaEnabled(_serviceOfferingDao.findById(serviceOfferingId).getOfferHA());
        return _vmDao.update(vmId, vm);
    }

    @Override
    public HashMap<Long, VmStatsEntry> getVirtualMachineStatistics(long hostId, String hostName, List<Long> vmIds) throws InternalErrorException {
    	HashMap<Long, VmStatsEntry> vmStatsById = new HashMap<Long, VmStatsEntry>();
    	
    	if (vmIds.isEmpty()) {
    		return vmStatsById;
    	}
    	
    	List<String> vmNames = new ArrayList<String>();
    	
    	for (Long vmId : vmIds) {
    		UserVmVO vm = _vmDao.findById(vmId);
    		vmNames.add(vm.getInstanceName());
    	}
    	
    	Answer answer = _agentMgr.easySend(hostId, new GetVmStatsCommand(vmNames,_hostDao.findById(hostId).getGuid(), hostName));
    	if (answer == null || !answer.getResult()) {
    		s_logger.warn("Unable to obtain VM statistics.");
    		return null;
    	} else {
    		HashMap<String, VmStatsEntry> vmStatsByName = ((GetVmStatsAnswer) answer).getVmStatsMap();

    		if(vmStatsByName == null)
    		{
    			s_logger.warn("Unable to obtain VM statistics.");
        		return null;
    		}
    			
    		for (String vmName : vmStatsByName.keySet()) {
    			vmStatsById.put(vmIds.get(vmNames.indexOf(vmName)), vmStatsByName.get(vmName));
    		}
    	}
    	
    	return vmStatsById;
    }
    
    @DB
    protected String acquireGuestIpAddress(long dcId, long accountId, UserVmVO userVm) throws InternalErrorException {
    	boolean routerLock = false;
        DomainRouterVO router = _routerDao.findBy(accountId, dcId);
        long routerId = router.getId();
        Transaction txn = Transaction.currentTxn();
    	try {
    		txn.start();
        	router = _routerDao.acquire(routerId);
        	if (router == null) {
        		throw new InternalErrorException("Unable to lock up the router:" + routerId+" please try again");
        	}
        	routerLock = true;
        	List<UserVmVO> userVms = _vmDao.listByAccountAndDataCenter(accountId, dcId);
        	Set<Long> allPossibleIps = NetUtils.getAllIpsFromCidr(router.getGuestIpAddress(), NetUtils.getCidrSize(router.getGuestNetmask()));
        	Set<Long> usedIps = new TreeSet<Long> ();
        	for (UserVmVO vm: userVms) {
        		if (vm.getGuestIpAddress() != null) {
        			usedIps.add(NetUtils.ip2Long(vm.getGuestIpAddress()));
        		}
        	}
        	if (usedIps.size() != 0) {
        		allPossibleIps.removeAll(usedIps);
        	}
        	if (allPossibleIps.isEmpty()) {
        		return null;
        	}
        	Iterator<Long> iterator = allPossibleIps.iterator();
        	long ipAddress = iterator.next().longValue();
        	String ipAddressStr = NetUtils.long2Ip(ipAddress);
        	userVm.setGuestIpAddress(ipAddressStr);
        	userVm.setGuestNetmask(router.getGuestNetmask());
            String vmMacAddress = NetUtils.long2Mac(
                	(NetUtils.mac2Long(router.getGuestMacAddress()) & 0xffffffff0000L) | (ipAddress & 0xffff)
                );
            userVm.setGuestMacAddress(vmMacAddress);
        	_vmDao.update(userVm.getId(), userVm);
        	if (routerLock) {
        		_routerDao.release(routerId);
        		routerLock = false;
        	}
        	txn.commit();
        	return ipAddressStr;
        }finally {
        	if (routerLock) {
        		_routerDao.release(routerId);
        	}
        }
     }
    
    
    public void releaseGuestIpAddress(UserVmVO userVm)  {
    	ServiceOffering offering = _offeringDao.findById(userVm.getServiceOfferingId());
    	
    	if (offering.getGuestIpType() != GuestIpType.Virtualized) {  		
    		IPAddressVO guestIP = (userVm.getGuestIpAddress() == null) ? null : _ipAddressDao.findById(userVm.getGuestIpAddress());
    		if (guestIP != null && guestIP.getAllocated() != null) {
    			_ipAddressDao.unassignIpAddress(userVm.getGuestIpAddress());
            	s_logger.debug("Released guest IP address=" + userVm.getGuestIpAddress() + " vmName=" + userVm.getName() +  " dcId=" + userVm.getDataCenterId());

    			EventVO event = new EventVO();
            	event.setUserId(User.UID_SYSTEM);
            	event.setAccountId(userVm.getAccountId());
            	event.setType(EventTypes.EVENT_NET_IP_RELEASE);
            	event.setParameters("guestIPaddress=" + userVm.getGuestIpAddress() + "\nvmName=" + userVm.getName() +  "\ndcId=" + userVm.getDataCenterId());
            	event.setDescription("released a public ip: " + userVm.getGuestIpAddress());
            	_eventDao.persist(event);
    		} else {
    			if (_IpAllocator != null && _IpAllocator.exteralIpAddressAllocatorEnabled()) {
        			String guestIp = userVm.getGuestIpAddress();
        			if (guestIp != null) {
        				_IpAllocator.releasePrivateIpAddress(guestIp, userVm.getDataCenterId(), userVm.getPodId());
        			}
        			
        		}
    		}
    	}
    	
    	userVm.setGuestIpAddress(null);
    	//_vmDao.update(userVm.getId(), userVm); FIXME need an updateIf
    }

    @Override @DB
    public UserVmVO createVirtualMachine(Long vmId, long userId, AccountVO account, DataCenterVO dc, ServiceOfferingVO offering, VMTemplateVO template, DiskOfferingVO diskOffering, String displayName, String group, String userData, List<StoragePoolVO> avoids, long startEventId) throws InternalErrorException, ResourceAllocationException {
        long accountId = account.getId();
        long dataCenterId = dc.getId();
        long serviceOfferingId = offering.getId();
        UserVmVO vm = null;
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating vm for account id=" + account.getId() +
            	", name="+ account.getAccountName() + "; dc=" + dc.getName() +
            	"; offering=" + offering.getId() + "; diskOffering=" + ((diskOffering != null) ? diskOffering.getName() : "none") +
            	"; template=" + template.getId());
        }

        DomainRouterVO router = _routerDao.findBy(accountId, dataCenterId, Role.DHCP_FIREWALL_LB_PASSWD_USERDATA);
        if (router == null) {
            throw new InternalErrorException("Cannot find a router for account (" + accountId + "/" +
            	account.getAccountName() + ") in " + dataCenterId);
        }
        
        // Determine the Guest OS Id
        long guestOSId;
        if (template != null) {
        	guestOSId = template.getGuestOSId();
        } else {
        	throw new InternalErrorException("No template or ISO was specified for the VM.");
        }
        long numVolumes = -1;
        Transaction txn = Transaction.currentTxn();
        long routerId = router.getId();
        
        String name;
        txn.start();
        
        account = _accountDao.lock(accountId, true);
        if (account == null) {
            throw new InternalErrorException("Unable to lock up the account: " + accountId);
        }

        // First check that the maximum number of UserVMs for the given accountId will not be exceeded
        if (_accountMgr.resourceLimitExceeded(account, ResourceType.user_vm)) {
            ResourceAllocationException rae = new ResourceAllocationException("Maximum number of virtual machines for account: " + account.getAccountName() + " has been exceeded.");
            rae.setResourceType("vm");
            throw rae;
        }
        
        boolean isIso = Storage.ImageFormat.ISO.equals(template.getFormat());
        numVolumes = (isIso || (diskOffering == null)) ? 1 : 2;
        _accountMgr.incrementResourceCount(account.getId(), ResourceType.user_vm);
        _accountMgr.incrementResourceCount(account.getId(), ResourceType.volume, numVolumes);
        txn.commit();
        
        name = VirtualMachineName.getVmName(vmId, accountId, _instance);

        String diskOfferingIdentifier = (diskOffering != null) ? String.valueOf(diskOffering.getId()) : "-1";
        String eventParams = "id=" + vmId + "\nvmName=" + name + "\nsoId=" + serviceOfferingId + "\ndoId=" + diskOfferingIdentifier + "\ntId=" + template.getId() + "\ndcId=" + dataCenterId;
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setStartId(startEventId);
        event.setState(EventState.Completed);
        event.setType(EventTypes.EVENT_VM_CREATE);
        event.setParameters(eventParams);

        try {
            Pair<HostPodVO, Long> pod = null;
            long poolid = 0;
            Set<Long> podsToAvoid = new HashSet<Long>();

            while ((pod = _agentMgr.findPod(template, offering, dc, account.getId(), podsToAvoid)) != null) {
                if (vm == null) {
                    vm = new UserVmVO(vmId, name, template.getId(), guestOSId, accountId, account.getDomainId().longValue(),
                    		serviceOfferingId, null, null, router.getGuestNetmask(),
                    		null,null,null,
                    		routerId, pod.first().getId(), dataCenterId,
                    		offering.getOfferHA(), displayName, group, userData);
                    
                    if (diskOffering != null) {
                    	vm.setMirroredVols(diskOffering.isMirrored());
                    }

                    vm.setLastHostId(pod.second());
                    
                    vm = _vmDao.persist(vm);
                } else {
                    vm.setPodId(pod.first().getId());
                    _vmDao.updateIf(vm, Event.OperationRetry, null);
                }
                
                String ipAddressStr = acquireGuestIpAddress(dataCenterId, accountId, vm);
                if (ipAddressStr == null) {
                	s_logger.warn("Failed user vm creation : no guest ip address available");
                 	releaseGuestIpAddress(vm);
                 	ResourceAllocationException rae = new ResourceAllocationException("No guest ip addresses available for " + account.getAccountName() + " (try destroying some instances)");
                	rae.setResourceType("vm");
                	throw rae;
                }

            	poolid = _storageMgr.createUserVM(account, vm, template, dc, pod.first(), offering, diskOffering, avoids);
                if ( poolid != 0) {
                    break;
                }
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to find storage host in pod " + pod.first().getName() + " (id:" + pod.first().getId() + ") while creating " + vm.toString() + ", checking other pods");
                }

                // if it fails at storage allocation round, reset lastHostId to "release"
                // the CPU/memory allocation on the candidate host
                vm.setLastHostId(null);
                _vmDao.update(vm.getId(), vm);
                
                podsToAvoid.add(pod.first().getId());
            }

            if(pod == null){
                throw new ResourceAllocationException("Create VM " + ((vm == null) ? vmId : vm.toString()) + " failed. There are no pods with enough CPU/memory");
            }
            
            if ((vm == null) || (poolid == 0)) {
                throw new ResourceAllocationException("Create VM " + ((vm == null) ? vmId : vm.toString()) + " failed due to no Storage Pool is available");
            }

            txn.start();
            if(vm != null && vm.getName() != null && vm.getDisplayName() != null)
            {
            	if(!vm.getName().equals(vm.getDisplayName()))
            		event.setDescription("successfully created VM instance : " + vm.getName()+"("+vm.getDisplayName()+")");
            	else
            		event.setDescription("successfully created VM instance : " + vm.getName());
            }
            else
            {
            	event.setDescription("successfully created VM instance :"+name);
            }
            
            _eventDao.persist(event);
            
            _vmDao.updateIf(vm, Event.OperationSucceeded, null);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("vm created " + vmId);
            }
            txn.commit();

            return _vmDao.findById(vmId);
        } catch (Throwable th) {
            s_logger.error("Unable to create vm", th);
            if (vm != null) {
            	_vmDao.delete(vmId);
            }
            _accountMgr.decrementResourceCount(account.getId(), ResourceType.user_vm);
            _accountMgr.decrementResourceCount(account.getId(), ResourceType.volume, numVolumes);
            
            String eventDescription = "Failed to create VM: ";
            if (vm == null) {
            	eventDescription += "new instance";
            } else {
            	eventDescription += vm.getName();
            	if (!vm.getName().equals(vm.getDisplayName())) {
            		eventDescription += " (" + vm.getDisplayName() + ")";
            	}
            }
            
            event.setDescription(eventDescription);
            event.setLevel(EventVO.LEVEL_ERROR);
            _eventDao.persist(event);

            if (th instanceof ResourceAllocationException) {
                throw (ResourceAllocationException)th;
            }
            throw new CloudRuntimeException("Unable to create vm", th);
        }
    }
    

    @Override @DB
    public boolean destroyVirtualMachine(long userId, long vmId) {
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null || vm.getState() == State.Destroyed || vm.getState() == State.Expunging || vm.getRemoved() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find vm or vm is destroyed: " + vmId);
            }
            return true;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Destroying vm " + vmId);
        }
        
        if (!stop(userId, vm, 0)) {
        	s_logger.error("Unable to stop vm so we can't destroy it: " + vmId);
        	return false;
        }
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(vm.getAccountId());
        event.setType(EventTypes.EVENT_VM_DESTROY);
        event.setParameters("id="+vm.getId() + "\nvmName=" + vm.getName() + "\nsoId=" + vm.getServiceOfferingId() + "\ntId=" + vm.getTemplateId() + "\ndcId=" + vm.getDataCenterId());
        if(!vm.getName().equals(vm.getDisplayName()))
        	event.setDescription("Successfully destroyed VM instance : " + vm.getName()+"("+vm.getDisplayName()+")");
        else
        	event.setDescription("Successfully destroyed VM instance : " + vm.getName());
        _eventDao.persist(event);

        _accountMgr.decrementResourceCount(vm.getAccountId(), ResourceType.user_vm);

        if (!destroy(vm)) {
        	return false;
        }
        
        cleanNetworkRules(userId, vmId);
        
        // Mark the VM's disks as destroyed
        List<VolumeVO> volumes = _volsDao.findByInstance(vmId);
        for (VolumeVO volume : volumes) {
        	_storageMgr.destroyVolume(volume);
        }

        txn.commit();
        return true;
    }

    @Override @DB
    public boolean executeDestroyVM(DestroyVMExecutor executor, VMOperationParam param) {
        UserVmVO vm = _vmDao.findById(param.getVmId());
        State state = vm.getState();
        if (vm == null || state == State.Destroyed || state == State.Expunging || vm.getRemoved() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find vm or vm is destroyed: " + param.getVmId());
            }
            
        	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
        		AsyncJobResult.STATUS_FAILED, 0, "VM does not exist or already in destroyed state");
        	return true;
        }
        
        if(state == State.Stopping) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is being stopped: " + param.getVmId());
            }
            
        	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
        		AsyncJobResult.STATUS_FAILED, 0, "VM is being stopped, please re-try later");
        	return true;
        }

        if (state == State.Running) {
            if (vm.getHostId() == null) {
            	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
                		AsyncJobResult.STATUS_FAILED, 0, "VM host is null (invalid VM)");
                    	
            	if(s_logger.isDebugEnabled())
            		s_logger.debug("Execute asynchronize destroy VM command: VM host is null (invalid VM)");
                return true;
            }
        	
            if (!_vmDao.updateIf(vm, Event.StopRequested, vm.getHostId())) {
            	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
                		AsyncJobResult.STATUS_FAILED, 0, "Failed to issue stop command, please re-try later");
                    	
            	if(s_logger.isDebugEnabled())
            		s_logger.debug("Execute asynchronize destroy VM command: failed to issue stop command, please re-try later");
                return true;
            }
            
            StopCommand cmd = new StopCommand(vm, vm.getInstanceName(), vm.getVnet());
            try {
    			long seq = _agentMgr.send(vm.getHostId(), new Command[] {cmd}, true,
    				new VMOperationListener(executor, param, vm, 0));
    			
            	if(s_logger.isDebugEnabled())
            		s_logger.debug("Execute asynchronize destroy VM command: sending stop command to agent, seq - " + seq);
            	
            	return false;
    		} catch (AgentUnavailableException e) {
            	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
            		AsyncJobResult.STATUS_FAILED, 0, "Agent is not available");
            	return true;
    		}
        }
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        EventVO event = new EventVO();
        event.setUserId(param.getUserId());
        event.setAccountId(vm.getAccountId());
        event.setType(EventTypes.EVENT_VM_DESTROY);
        event.setStartId(param.getEventId());
        event.setParameters("id="+vm.getId() + "\nvmName=" + vm.getName() + "\nsoId=" + vm.getServiceOfferingId() + "\ntId=" + vm.getTemplateId() + "\ndcId=" + vm.getDataCenterId());
        if(!vm.getName().equals(vm.getDisplayName()))
        	event.setDescription("successfully destroyed VM instance : " + vm.getName()+"("+vm.getDisplayName()+")");
        else
        	event.setDescription("successfully destroyed VM instance : " + vm.getName());
        _eventDao.persist(event);
        
        _accountMgr.decrementResourceCount(vm.getAccountId(), ResourceType.user_vm);
        if (!_vmDao.updateIf(vm, VirtualMachine.Event.DestroyRequested, vm.getHostId())) {
            s_logger.debug("Unable to destroy the vm because it is not in the correct state: " + vm.toString());
            
            txn.rollback();
        	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
            		AsyncJobResult.STATUS_FAILED, 0, "Unable to destroy the vm because it is not in the correct state");
            return true;
        }

        // Now that the VM is destroyed, clean the network rules associated with it.
        cleanNetworkRules(param.getUserId(), vm.getId().longValue());

        // Mark the VM's root disk as destroyed
        List<VolumeVO> volumes = _volsDao.findByInstanceAndType(vm.getId(), VolumeType.ROOT);
        for (VolumeVO volume : volumes) {
        	_storageMgr.destroyVolume(volume);
        }
        
        // Mark the VM's data disks as detached
        volumes = _volsDao.findByInstanceAndType(vm.getId(), VolumeType.DATADISK);
        for (VolumeVO volume : volumes) {
        	_volsDao.detachVolume(volume.getId());
        }
        
        txn.commit();
    	executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
    		AsyncJobResult.STATUS_SUCCEEDED, 0, "success");
    	return true;
    }
    
    @Override @DB
    public boolean recoverVirtualMachine(long vmId) throws ResourceAllocationException {
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null || vm.getRemoved() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find vm or vm is removed: " + vmId);
            }
            return false;
        }
        
        if (vm.getState() != State.Destroyed) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("vm is not in the right state: " + vmId);
            }
        	return true;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Recovering vm " + vmId);
        }

        EventVO event = new EventVO();
        event.setUserId(1L);
        event.setAccountId(vm.getAccountId());
        event.setType(EventTypes.EVENT_VM_CREATE);
        event.setParameters("id="+vm.getId() + "\nvmName=" + vm.getName() + "\nsoId=" + vm.getServiceOfferingId() + "\ntId=" + vm.getTemplateId() + "\ndcId=" + vm.getDataCenterId());
        
        Transaction txn = Transaction.currentTxn();
        AccountVO account = null;
    	txn.start();

        account = _accountDao.lock(vm.getAccountId(), true);
        
    	// First check that the maximum number of UserVMs for the given accountId will not be exceeded
        if (_accountMgr.resourceLimitExceeded(account, ResourceType.user_vm)) {
        	ResourceAllocationException rae = new ResourceAllocationException("Maximum number of virtual machines for account: " + account.getAccountName() + " has been exceeded.");
        	rae.setResourceType("vm");
        	event.setLevel(EventVO.LEVEL_ERROR);
        	if(!vm.getName().equals(vm.getDisplayName()))
        		event.setDescription("Failed to recover VM instance : " + vm.getName()+"("+vm.getDisplayName()+")" + "; the resource limit for account: " + account.getAccountName() + " has been exceeded.");
        	else
        		event.setDescription("Failed to recover VM instance : " + vm.getName() + "; the resource limit for account: " + account.getAccountName() + " has been exceeded.");
        	_eventDao.persist(event);
        	txn.commit();
        	throw rae;
        }
        
        _haMgr.cancelDestroy(vm, vm.getHostId());

        _accountMgr.incrementResourceCount(account.getId(), ResourceType.user_vm);

        if (!_vmDao.updateIf(vm, Event.RecoveryRequested, null)) {
            s_logger.debug("Unable to recover the vm because it is not in the correct state: " + vmId);
            return false;
        }
        
        // Recover the VM's disks
        List<VolumeVO> volumes = _volsDao.findByInstanceIdDestroyed(vmId);
        for (VolumeVO volume : volumes) {
        	_volsDao.recoverVolume(volume.getId());
            // Create an event
            long templateId = -1;
            long diskOfferingId = -1;
            if(volume.getTemplateId() !=null){
                templateId = volume.getTemplateId();
            }
            if(volume.getDiskOfferingId() !=null){
                diskOfferingId = volume.getDiskOfferingId();
            }
            long sizeMB = volume.getSize()/(1024*1024);
            String eventParams = "id=" + volume.getId() +"\ndoId="+diskOfferingId+"\ntId="+templateId+"\ndcId="+volume.getDataCenterId()+"\nsize="+sizeMB;
            EventVO volEvent = new EventVO();
            volEvent.setAccountId(volume.getAccountId());
            volEvent.setUserId(1L);
            volEvent.setType(EventTypes.EVENT_VOLUME_CREATE);
            volEvent.setParameters(eventParams);
            StoragePoolVO pool = _storagePoolDao.findById(volume.getPoolId());
            volEvent.setDescription("Created volume: "+ volume.getName() +" with size: " + sizeMB + " MB in pool: " + pool.getName());
            _eventDao.persist(volEvent);
        }
        
        _accountMgr.incrementResourceCount(account.getId(), ResourceType.volume, new Long(volumes.size()));
        
        event.setLevel(EventVO.LEVEL_INFO);
        if(!vm.getName().equals(vm.getDisplayName()))
        	event.setDescription("successfully recovered VM instance : " + vm.getName()+"("+vm.getDisplayName()+")");
        else
            event.setDescription("successfully recovered VM instance : " + vm.getName());
        _eventDao.persist(event);
        
        txn.commit();
        
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;

        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        _configDao = locator.getDao(ConfigurationDao.class);
        if (_configDao == null) {
            throw new ConfigurationException("Unable to get the configuration dao.");
        }

        Map<String, String> configs = _configDao.getConfiguration("AgentManager", params);

        String value = configs.get("start.retry");
        _retry = NumbersUtil.parseInt(value, 2);

        _instance = configs.get("instance.name");
        if (_instance == null) {
            _instance = "DEFAULT";
        }
        
        String workers = configs.get("expunge.workers");
        int wrks = NumbersUtil.parseInt(workers, 10);
        
        String time = configs.get("expunge.interval");
        _expungeInterval = NumbersUtil.parseInt(time, 86400);
        
        time = configs.get("expunge.delay");
        _expungeDelay = NumbersUtil.parseInt(time, _expungeInterval);
        
        String maxCap = configs.get("cpu.uservm.cap");
        _userVMCap = NumbersUtil.parseInt(maxCap, 0);
        
        _executor = Executors.newScheduledThreadPool(wrks, new NamedThreadFactory("UserVm-Scavenger"));
        
        _haMgr.registerHandler(Type.User, this);
        
        s_logger.info("User VM Manager is configured.");

        Adapters<IpAddrAllocator> ipAllocators = locator.getAdapters(IpAddrAllocator.class);
        if (ipAllocators != null && ipAllocators.isSet()) {
        	Enumeration<IpAddrAllocator> it = ipAllocators.enumeration();
        	_IpAllocator = it.nextElement();
        }
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
    	_executor.scheduleWithFixedDelay(new ExpungeTask(this), _expungeInterval, _expungeInterval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
    	_executor.shutdown();
        return true;
    }

    protected UserVmManagerImpl() {
    }

    @Override
    public Command cleanup(UserVmVO vm, String vmName) {
        if (vmName != null) {
            return new StopCommand(vm, vmName, VirtualMachineName.getVnet(vmName));
        } else if (vm != null) {
            return new StopCommand(vm, vm.getVnet());
        } else {
            throw new CloudRuntimeException("Shouldn't even be here!");
        }
    }

    @Override
    public void completeStartCommand(UserVmVO vm) {
    	_vmDao.updateIf(vm, Event.AgentReportRunning, vm.getHostId());
        _networkGroupManager.handleVmStateTransition(vm, State.Running);

    }
    
    @Override
    public void completeStopCommand(UserVmVO instance) {
    	completeStopCommand(1L, instance, Event.AgentReportStopped, 0);
    }
    
    @Override
    @DB
    public void completeStopCommand(long userId, UserVmVO vm, Event e, long startEventId) {
        Transaction txn = Transaction.currentTxn();
        try {
        	String vnet = vm.getVnet();
            vm.setVnet(null);
            vm.setProxyAssignTime(null);
            vm.setProxyId(null);
            vm.setStorageIp(null);

            txn.start();
            
            if (!_vmDao.updateIf(vm, e, null)) {
            	s_logger.debug("Unable to update ");
            	return;
            }
            
            if ((vm.getDomainRouterId() != null) && _vmDao.listBy(vm.getDomainRouterId(), State.Starting, State.Running).size() == 0) {
            	DomainRouterVO router = _routerDao.findById(vm.getDomainRouterId());
            	if (router.getState().equals(State.Stopped)) {
            		_dcDao.releaseVnet(vnet, router.getDataCenterId(), router.getAccountId());
            	}
            }
            
            txn.commit();
            _networkGroupManager.handleVmStateTransition(vm, State.Stopped);
        } catch (Throwable th) {
            s_logger.error("Error during stop: ", th);
            throw new CloudRuntimeException("Error during stop: ", th);
        }

        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(vm.getAccountId());
        event.setType(EventTypes.EVENT_VM_STOP);
        event.setState(EventState.Completed);
        event.setStartId(startEventId);
        event.setParameters("id="+vm.getId() + "\n" + "vmName=" + vm.getName() + "\nsoId=" + vm.getServiceOfferingId() + "\ntId=" + vm.getTemplateId() + "\ndcId=" + vm.getDataCenterId());
        if(!vm.getName().equals(vm.getDisplayName()))
        	event.setDescription("Successfully stopped VM instance : " + vm.getName()+"("+vm.getDisplayName()+")");
        else
        	event.setDescription("Successfully stopped VM instance : " + vm.getName());
        _eventDao.persist(event);

        if (_storageMgr.unshare(vm, null) == null) {
            s_logger.warn("Unable to set share to false for " + vm.toString());
        }
    }

    @Override
    public UserVmVO get(long id) {
        return getVirtualMachine(id);
    }
    
    public String getRandomPrivateTemplateName() {
    	return UUID.randomUUID().toString();
    }

    @Override
    public Long convertToId(String vmName) {
        if (!VirtualMachineName.isValidVmName(vmName, _instance)) {
            return null;
        }
        return VirtualMachineName.getVmId(vmName);
    }

    @Override
    public UserVmVO start(long vmId, long startEventId) throws StorageUnavailableException, ConcurrentOperationException, ExecutionException {
        return start(1L, vmId, null, null, startEventId);
    }

    @Override
    public boolean stop(UserVmVO vm, long startEventId) {
        return stop(1L, vm, startEventId);
    }

    private boolean stop(long userId, UserVmVO vm, long startEventId) {
        State state = vm.getState();
        if (state == State.Stopped) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is already stopped: " + vm.toString());
            }
            return true;
        }
        
        if (state == State.Creating || state == State.Destroyed || state == State.Expunging) {
        	s_logger.warn("Stopped called on " + vm.toString() + " but the state is " + state.toString());
        	return true;
        }
        
        if (!_vmDao.updateIf(vm, Event.StopRequested, vm.getHostId())) {
            s_logger.debug("VM is not in a state to stop: " + vm.getState().toString());
            return false;
        }
        
        if (vm.getHostId() == null) {
        	s_logger.debug("Host id is null so we can't stop it.  How did we get into here?");
        	return false;
        }

        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(vm.getAccountId());
        event.setType(EventTypes.EVENT_VM_STOP);
        event.setStartId(startEventId);
        event.setParameters("id="+vm.getId() + "\n" + "vmName=" + vm.getName() + "\nsoId=" + vm.getServiceOfferingId() + "\ntId=" + vm.getTemplateId() + "\ndcId=" + vm.getDataCenterId());

        StopCommand stop = new StopCommand(vm, vm.getInstanceName(), vm.getVnet());

        boolean stopped = false;
        try {
            Answer answer = _agentMgr.send(vm.getHostId(), stop);
            if (!answer.getResult()) {
                s_logger.warn("Unable to stop vm " + vm.getName() + " due to " + answer.getDetails());
            } else {
            	stopped = true;
            }
        } catch(AgentUnavailableException e) {
            s_logger.warn("Agent is not available to stop vm " + vm.toString());
        } catch(OperationTimedoutException e) {
        	s_logger.warn("operation timed out " + vm.toString());
        }

        if (stopped) {
        	completeStopCommand(userId, vm, Event.OperationSucceeded, 0);
        } else
        {
        	if(!vm.getName().equals(vm.getDisplayName()))
        		event.setDescription("failed to stop VM instance : " + vm.getName()+"("+vm.getDisplayName()+")");
        	else
        		event.setDescription("failed to stop VM instance : " + vm.getName());
            event.setLevel(EventVO.LEVEL_ERROR);
            _eventDao.persist(event);
            _vmDao.updateIf(vm, Event.OperationFailed, vm.getHostId());
            s_logger.error("Unable to stop vm " + vm.getName());
        }

        return stopped;
    }

    @Override @DB
    public boolean destroy(UserVmVO vm) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Destroying vm " + vm.toString());
        }
        if (!_vmDao.updateIf(vm, VirtualMachine.Event.DestroyRequested, vm.getHostId())) {
            s_logger.debug("Unable to destroy the vm because it is not in the correct state: " + vm.toString());
            return false;
        }

        return true;
    }

    @Override
    public HostVO prepareForMigration(UserVmVO vm) throws StorageUnavailableException {
        long vmId = vm.getId();
        boolean mirroredVols = vm.isMirroredVols();
        DataCenterVO dc = _dcDao.findById(vm.getDataCenterId());
        HostPodVO pod = _podDao.findById(vm.getPodId());
        ServiceOfferingVO offering = _offeringDao.findById(vm.getServiceOfferingId());
        VMTemplateVO template = _templateDao.findById(vm.getTemplateId());
        List<StoragePoolVO> sps = _storageMgr.getStoragePoolsForVm(vm.getId());
        StoragePoolVO sp = sps.get(0); // FIXME
       

        List<VolumeVO> vols = _volsDao.findCreatedByInstance(vmId);

        String [] storageIps = new String[2];
        VolumeVO vol = vols.get(0);
        storageIps[0] = vol.getHostIp();
        if (mirroredVols && (vols.size() == 2)) {
            storageIps[1] = vols.get(1).getHostIp();
        }

        PrepareForMigrationCommand cmd = new PrepareForMigrationCommand(vm.getInstanceName(), vm.getVnet(), storageIps, vols, mirroredVols);

        HostVO vmHost = null;
        HashSet<Host> avoid = new HashSet<Host>();

        HostVO fromHost = _hostDao.findById(vm.getHostId());
        if (fromHost.getHypervisorType() != Hypervisor.Type.KVM && fromHost.getClusterId() == null) {
            s_logger.debug("The host is not in a cluster");
            return null;
        }
        avoid.add(fromHost);

        while ((vmHost = (HostVO)_agentMgr.findHost(Host.Type.Routing, dc, pod, sp, offering, template, vm, null, avoid)) != null) {
            avoid.add(vmHost);

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Trying to migrate router to host " + vmHost.getName());
            }
            
            if( !_storageMgr.share(vm, vols, vmHost, false) ) {
                s_logger.warn("Can not share " + vm.toString() + " on host " + vmHost.getId());
                throw new StorageUnavailableException(vmHost.getId());
            }

            Answer answer = _agentMgr.easySend(vmHost.getId(), cmd);
            if (answer != null && answer.getResult()) {
                return vmHost;
            }

            _storageMgr.unshare(vm, vols, vmHost);

        }

        return null;
    }

    @Override
    public boolean migrate(UserVmVO vm, HostVO host) throws AgentUnavailableException, OperationTimedoutException {
        HostVO fromHost = _hostDao.findById(vm.getHostId());

    	if (!_vmDao.updateIf(vm, Event.MigrationRequested, vm.getHostId())) {
    		s_logger.debug("State for " + vm.toString() + " has changed so migration can not take place.");
    		return false;
    	}
        boolean isWindows = _guestOSCategoryDao.findById(_guestOSDao.findById(vm.getGuestOSId()).getCategoryId()).getName().equalsIgnoreCase("Windows");
        MigrateCommand cmd = new MigrateCommand(vm.getInstanceName(), host.getPrivateIpAddress(), isWindows);
        Answer answer = _agentMgr.send(fromHost.getId(), cmd);
        if (answer == null) {
            return false;
        }

        List<VolumeVO> vols = _volsDao.findCreatedByInstance(vm.getId());
        if (vols.size() == 0) {
            return true;
        }

        _storageMgr.unshare(vm, vols, fromHost);

        return true;
    }

    @DB
    public void expunge() {
    	List<UserVmVO> vms = _vmDao.findDestroyedVms(new Date(System.currentTimeMillis() - ((long)_expungeDelay << 10)));
    	s_logger.info("Found " + vms.size() + " vms to expunge.");
    	for (UserVmVO vm : vms) 
    	{
    		String privateIpAddress = vm.getPrivateIpAddress();
    		long vmId = vm.getId();
    		releaseGuestIpAddress(vm);
            vm.setGuestNetmask(null);
            vm.setGuestMacAddress(null);
    		if (!_vmDao.updateIf(vm, Event.ExpungeOperation, null)) {
    			s_logger.info("vm " + vmId + " is skipped because it is no longer in Destroyed state");
    			continue;
    		}

    		List<FirewallRuleVO> forwardingRules = null;
			forwardingRules = _rulesDao.listByPrivateIp(privateIpAddress);
			
			for(FirewallRuleVO rule: forwardingRules)
			{
				try
				{
					_networkMgr.deleteRule(rule.getId(), Long.valueOf(User.UID_SYSTEM), Long.valueOf(User.UID_SYSTEM));
					if(s_logger.isDebugEnabled())
						s_logger.debug("Rule "+rule.getId()+" for vm:"+vm.getName()+" is deleted successfully during expunge operation");
				}
				catch(Exception e)
				{
					s_logger.warn("Failed to delete rule:"+rule.getId()+" for vm:"+vm.getName());
				}
			}
                    		
    		
            List<VolumeVO> vols = null;
            try {
                vols = _volsDao.findByInstanceIdDestroyed(vmId);
                _storageMgr.destroy(vm, vols);
                
                _vmDao.remove(vm.getId());
                _networkGroupManager.removeInstanceFromGroups(vm.getId());
                
                s_logger.debug("vm is destroyed");
            } catch (Exception e) {
            	s_logger.info("VM " + vmId +" expunge failed due to " + e.getMessage());
			}
    	}
    	
    	List<VolumeVO> destroyedVolumes = _volsDao.findByDetachedDestroyed();
    	s_logger.info("Found " + destroyedVolumes.size() + " detached volumes to expunge.");
		_storageMgr.destroy(null, destroyedVolumes);
    }

    @Override @DB
    public boolean completeMigration(UserVmVO vm, HostVO host) throws AgentUnavailableException, OperationTimedoutException {
        CheckVirtualMachineCommand cvm = new CheckVirtualMachineCommand(vm.getInstanceName());
        CheckVirtualMachineAnswer answer = (CheckVirtualMachineAnswer)_agentMgr.send(host.getId(), cvm);
        if (!answer.getResult()) {
            s_logger.debug("Unable to complete migration for " + vm.toString());
            _vmDao.updateIf(vm, Event.AgentReportStopped, null);
            return false;
        }

        State state = answer.getState();
        if (state == State.Stopped) {
            s_logger.warn("Unable to complete migration as we can not detect it on " + host.toString());
            _vmDao.updateIf(vm, Event.AgentReportStopped, null);
            return false;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Marking port " + answer.getVncPort() + " on " + host.getId());
        }

        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            _vmDao.updateIf(vm, Event.OperationSucceeded, host.getId());
            txn.commit();
            
            return true;
        } catch(Exception e) {
            s_logger.warn("Exception during completion of migration process " + vm.toString());
            return false;
        }
    }


    @Override
    public void cleanNetworkRules(long userId, long instanceId) {
        UserVmVO vm = _vmDao.findById(instanceId);
        String guestIpAddr = vm.getGuestIpAddress();
        long accountId = vm.getAccountId();

        // clean up any load balancer rules and security group mappings for this VM
        List<SecurityGroupVMMapVO> securityGroupMappings = _securityGroupVMMapDao.listByInstanceId(vm.getId());
        for (SecurityGroupVMMapVO securityGroupMapping : securityGroupMappings) {
            String ipAddress = securityGroupMapping.getIpAddress();

            // find the router from the ipAddress
            DomainRouterVO router = null;
            if (vm.getDomainRouterId() != null)
            	router = _routerDao.findById(vm.getDomainRouterId());
            else 
            	continue;
            // grab all the firewall rules
            List<FirewallRuleVO> fwRules = _rulesDao.listForwardingByPubAndPrivIp(true, ipAddress, vm.getGuestIpAddress());
            for (FirewallRuleVO fwRule : fwRules) {
                fwRule.setEnabled(false);
            }

            List<FirewallRuleVO> updatedRules = _networkMgr.updateFirewallRules(ipAddress, fwRules, router);

            // Save and create the event
            String description;
            String type = EventTypes.EVENT_NET_RULE_DELETE;
            String ruleName = "ip forwarding";
            String level = EventVO.LEVEL_INFO;

            if (updatedRules != null) {
                _securityGroupVMMapDao.remove(securityGroupMapping.getId());
                for (FirewallRuleVO updatedRule : updatedRules) {
                _rulesDao.remove(updatedRule.getId());

                    description = "deleted " + ruleName + " rule [" + updatedRule.getPublicIpAddress() + ":" + updatedRule.getPublicPort() +
                              "]->[" + updatedRule.getPrivateIpAddress() + ":" + updatedRule.getPrivatePort() + "]" + " " + updatedRule.getProtocol();

                EventVO fwRuleEvent = new EventVO();
                fwRuleEvent.setUserId(userId);
                fwRuleEvent.setAccountId(accountId);
                fwRuleEvent.setType(type);
                fwRuleEvent.setDescription(description);
                    fwRuleEvent.setLevel(level);
                _eventDao.persist(fwRuleEvent);
            }
            // save off an event for removing the security group
            EventVO event = new EventVO();
            event.setUserId(userId);
            event.setAccountId(vm.getAccountId());
            event.setType(EventTypes.EVENT_PORT_FORWARDING_SERVICE_REMOVE);
            event.setDescription("Successfully removed port forwarding service " + securityGroupMapping.getSecurityGroupId() + " from virtual machine " + vm.getName());
            event.setLevel(EventVO.LEVEL_INFO);
            String params = "sgId="+securityGroupMapping.getSecurityGroupId()+"\nvmId="+vm.getId();
            event.setParameters(params);
            _eventDao.persist(event);
            }
        }

        List<LoadBalancerVMMapVO> loadBalancerMappings = _loadBalancerVMMapDao.listByInstanceId(vm.getId());
        for (LoadBalancerVMMapVO loadBalancerMapping : loadBalancerMappings) {
            List<FirewallRuleVO> lbRules = _rulesDao.listByLoadBalancerId(loadBalancerMapping.getLoadBalancerId());
            FirewallRuleVO targetLbRule = null;
            for (FirewallRuleVO lbRule : lbRules) {
                if (lbRule.getPrivateIpAddress().equals(guestIpAddr)) {
                    targetLbRule = lbRule;
                    targetLbRule.setEnabled(false);
                    break;
                }
            }

            if (targetLbRule != null) {
                String ipAddress = targetLbRule.getPublicIpAddress();
                DomainRouterVO router = _routerDao.findById(vm.getDomainRouterId());
                _networkMgr.updateFirewallRules(ipAddress, lbRules, router);

                // now that the rule has been disabled, delete it, also remove the mapping from the load balancer mapping table
                _rulesDao.remove(targetLbRule.getId());
                _loadBalancerVMMapDao.remove(loadBalancerMapping.getId());

                // save off the event for deleting the LB rule
                EventVO lbRuleEvent = new EventVO();
                lbRuleEvent.setUserId(userId);
                lbRuleEvent.setAccountId(accountId);
                lbRuleEvent.setType(EventTypes.EVENT_NET_RULE_DELETE);
                lbRuleEvent.setDescription("deleted load balancer rule [" + targetLbRule.getPublicIpAddress() + ":" + targetLbRule.getPublicPort() +
                        "]->[" + targetLbRule.getPrivateIpAddress() + ":" + targetLbRule.getPrivatePort() + "]" + " " + targetLbRule.getAlgorithm());
                lbRuleEvent.setLevel(EventVO.LEVEL_INFO);
                _eventDao.persist(lbRuleEvent);
            }
        }
    }
    
    public VMTemplateVO createPrivateTemplateRecord(Long userId, long volumeId, String name, String description, long guestOSId, Boolean requiresHvm, Integer bits, Boolean passwordEnabled, boolean isPublic, boolean featured)
    	throws InvalidParameterValueException {

    	VMTemplateVO privateTemplate = null;

    	UserVO user = _userDao.findById(userId);
    	
    	if (user == null) {
    		throw new InvalidParameterValueException("User " + userId + " does not exist");
    	}

    	VolumeVO volume = _volsDao.findById(volumeId);
    	if (volume == null) {
            throw new InvalidParameterValueException("Volume with ID: " + volumeId + " does not exist");
    	}

    	int bitsValue = ((bits == null) ? 64 : bits.intValue());
    	boolean requiresHvmValue = ((requiresHvm == null) ? true : requiresHvm.booleanValue());
    	boolean passwordEnabledValue = ((passwordEnabled == null) ? false : passwordEnabled.booleanValue());

    	// if the volume is a root disk, try to find out requiresHvm and bits if possible
    	if (Volume.VolumeType.ROOT.equals(volume.getVolumeType())) {
    	    Long instanceId = volume.getInstanceId();
    	    if (instanceId != null) {
    	        UserVm vm = _vmDao.findById(instanceId);
    	        if (vm != null) {
    	            VMTemplateVO origTemplate = _templateDao.findById(vm.getTemplateId());
    	            if (!ImageFormat.ISO.equals(origTemplate.getFormat()) && !ImageFormat.RAW.equals(origTemplate.getFormat())) {
    	                bitsValue = origTemplate.getBits();
    	                requiresHvmValue = origTemplate.requiresHvm();
    	            }
    	        }
    	    }
    	}

    	GuestOSVO guestOS = _guestOSDao.findById(guestOSId);
    	if (guestOS == null) {
    		throw new InvalidParameterValueException("GuestOS with ID: " + guestOSId + " does not exist.");
    	}

        String uniqueName = Long.valueOf((userId == null)?1:userId).toString() + Long.valueOf(volumeId).toString() + UUID.nameUUIDFromBytes(name.getBytes()).toString();
    	Long nextTemplateId = _templateDao.getNextInSequence(Long.class, "id");

        privateTemplate = new VMTemplateVO(nextTemplateId,
                                           uniqueName,
                                           name,
                                           ImageFormat.RAW,
                                           isPublic,
                                           featured,
                                           null,
                                           null,
                                           null,
                                           requiresHvmValue,
                                           bitsValue,
                                           volume.getAccountId(),
                                           null,
                                           description,
                                           passwordEnabledValue,
                                           guestOS.getId(),
                                           true);

        return _templateDao.persist(privateTemplate);
    }

    @Override @DB
    public VMTemplateVO createPrivateTemplate(VMTemplateVO template, Long userId, long snapshotId, String name, String description) {
    	VMTemplateVO privateTemplate = null;
    	long templateId = template.getId();
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
        if (snapshot != null) {
        	Long volumeId = snapshot.getVolumeId();
            VolumeVO volume = _volsDao.findById(volumeId);
            StringBuilder userFolder = new StringBuilder();
            Formatter userFolderFormat = new Formatter(userFolder);
            userFolderFormat.format("u%06d", snapshot.getAccountId());

            String uniqueName = getRandomPrivateTemplateName();

            long zoneId = volume.getDataCenterId();
            HostVO secondaryStorageHost = _storageMgr.getSecondaryStorageHost(zoneId);
            String secondaryStorageURL = _storageMgr.getSecondaryStorageURL(zoneId);

            if (secondaryStorageHost == null || secondaryStorageURL == null) {
            	s_logger.warn("Did not find the secondary storage URL in the database.");
            	return null;
            }
            
            Command cmd = null;
            String backupSnapshotUUID = snapshot.getBackupSnapshotId();
            if (backupSnapshotUUID != null) {
                // We are creating a private template from a snapshot which has been backed up to secondary storage.
                Long dcId = volume.getDataCenterId();
                Long accountId = volume.getAccountId();
                
                String origTemplateInstallPath = null;

                ImageFormat format =  _snapshotMgr.getImageFormat(volumeId);
                if (format != null && format != ImageFormat.ISO) {
                    Long origTemplateId = volume.getTemplateId();
                    VMTemplateHostVO vmTemplateHostVO = _templateHostDao.findByHostTemplate(secondaryStorageHost.getId(), origTemplateId);
                    origTemplateInstallPath = vmTemplateHostVO.getInstallPath();
                }
                
                cmd = new CreatePrivateTemplateFromSnapshotCommand(_storageMgr.getPrimaryStorageNameLabel(volume),
                                                                   secondaryStorageURL,
                                                                   dcId,
                                                                   accountId,
                                                                   volumeId,
                                                                   backupSnapshotUUID,
                                                                   origTemplateInstallPath,
                                                                   templateId,
                                                                   name);
            }
            else {
                cmd = new CreatePrivateTemplateCommand(secondaryStorageURL,
                                                       templateId,
                                                       volume.getAccountId(),
                                                       name,
                                                       uniqueName,
                                                       _storageMgr.getPrimaryStorageNameLabel(volume),
                                                       snapshot.getPath(),
                                                       snapshot.getName(),
                                                       userFolder.toString());
            }
            
            // FIXME: before sending the command, check if there's enough capacity on the storage server to create the template

            String basicErrMsg = "Failed to create template from snapshot: " + snapshot.getName();
            // This can be sent to a KVM host too.
            CreatePrivateTemplateAnswer answer = (CreatePrivateTemplateAnswer) _storageMgr.sendToHostsOnStoragePool(volume.getPoolId(), cmd, basicErrMsg);
            // Don't proceed further if there was an exception above.
            if (answer == null) {
                return null;
            }
            
            String eventParams = "id="+templateId+"\nname=" + name + "\ndcId=" + zoneId +"\nsize="+volume.getSize();
            EventVO event = new EventVO();
            event.setUserId(userId.longValue());
            event.setAccountId(snapshot.getAccountId());
            event.setType(EventTypes.EVENT_TEMPLATE_CREATE);
            event.setParameters(eventParams);

            if ((answer != null) && answer.getResult()) {
                
                privateTemplate = _templateDao.findById(templateId);
                Long origTemplateId = volume.getTemplateId();
                VMTemplateVO origTemplate = null;
                if (origTemplateId != null) {
                	origTemplate = _templateDao.findById(origTemplateId);
                }

                if ((origTemplate != null) && !Storage.ImageFormat.ISO.equals(origTemplate.getFormat())) {
                	// We made a template from a root volume that was cloned from a template
                	privateTemplate.setFileSystem(origTemplate.getFileSystem());
                	privateTemplate.setRequiresHvm(origTemplate.requiresHvm());
                	privateTemplate.setBits(origTemplate.getBits());
                } else {
                	// We made a template from a root volume that was not cloned from a template, or a data volume
                	privateTemplate.setFileSystem(Storage.FileSystem.Unknown);
                	privateTemplate.setRequiresHvm(true);
                	privateTemplate.setBits(64);
                }

                String answerUniqueName = answer.getUniqueName();
                if (answerUniqueName != null) {
                	privateTemplate.setUniqueName(answerUniqueName);
                } else {
                	privateTemplate.setUniqueName(uniqueName);
                }
                ImageFormat format = answer.getImageFormat();
                if (format != null) {
                	privateTemplate.setFormat(format);
                }
                else {
                	// This never occurs.
                	// Specify RAW format makes it unusable for snapshots.
                	privateTemplate.setFormat(ImageFormat.RAW);
                }

                _templateDao.update(templateId, privateTemplate);

                // add template zone ref for this template
                _templateDao.addTemplateToZone(privateTemplate, zoneId);
                VMTemplateHostVO templateHostVO = new VMTemplateHostVO(secondaryStorageHost.getId(), templateId);
                templateHostVO.setDownloadPercent(100);
                templateHostVO.setDownloadState(Status.DOWNLOADED);
                templateHostVO.setInstallPath(answer.getPath());
                templateHostVO.setLastUpdated(new Date());
                templateHostVO.setSize(answer.getVirtualSize());
                _templateHostDao.persist(templateHostVO);
                
                event.setDescription("Created template " + name + " from snapshot " + snapshotId);
                event.setLevel(EventVO.LEVEL_INFO);
                _eventDao.persist(event);
                
                // Increment the number of templates
                _accountMgr.incrementResourceCount(volume.getAccountId(), ResourceType.template);
                
            } else {
                event.setDescription("Failed to create template " + name + " from snapshot " + snapshotId);
                event.setLevel(EventVO.LEVEL_ERROR);
                _eventDao.persist(event);
                
                // Remove the template record
                _templateDao.remove(templateId);
            }

            
        }
        return privateTemplate;
    }
    
    @DB
    @Override
	public UserVmVO createDirectlyAttachedVM(Long vmId, long userId, AccountVO account, DataCenterVO dc, ServiceOfferingVO offering, VMTemplateVO template, DiskOfferingVO diskOffering, String displayName, String group, String userData, List<StoragePoolVO> a, List<NetworkGroupVO>  networkGroups, long startEventId) throws InternalErrorException, ResourceAllocationException {
    	
    	long accountId = account.getId();
	    long dataCenterId = dc.getId();
	    long serviceOfferingId = offering.getId();
	    long templateId = -1;
	    if (template != null)
	    	templateId = template.getId();
	    
	    if (s_logger.isDebugEnabled()) {
	        s_logger.debug("Creating directly attached vm for account id=" + account.getId() +
	        	", name="+ account.getAccountName() + "; dc=" + dc.getName() +
	        	"; offering=" + offering.getId() + "; diskOffering=" + ((diskOffering != null) ? diskOffering.getName() : "none") +
	        	"; template=" + templateId);
	    }
	    
	    // Determine the Guest OS Id
        long guestOSId;
        if (template != null) {
        	guestOSId = template.getGuestOSId();
        } else {
        	throw new InternalErrorException("No template or ISO was specified for the VM.");
        }
	    
	    Transaction txn = Transaction.currentTxn();
        txn.start();
        
        account = _accountDao.lock(accountId, true);
        if (account == null) {
            throw new InternalErrorException("Unable to lock up the account: " + accountId);
        }

        // First check that the maximum number of UserVMs for the given accountId will not be exceeded
        if (_accountMgr.resourceLimitExceeded(account, ResourceType.user_vm)) {
            ResourceAllocationException rae = new ResourceAllocationException("Maximum number of virtual machines for account: " + account.getAccountName() + " has been exceeded.");
            rae.setResourceType("vm");
            throw rae;
        }
        _accountMgr.incrementResourceCount(account.getId(), ResourceType.user_vm);
        boolean isIso = Storage.ImageFormat.ISO.equals(template.getFormat());
        long numVolumes = (isIso || (diskOffering == null)) ? 1 : 2;
        _accountMgr.incrementResourceCount(account.getId(), ResourceType.volume, numVolumes);
        txn.commit();
        
	    try {
	        UserVmVO vm = null;
	    	boolean forZone = false;
	    	final String name = VirtualMachineName.getVmName(vmId, accountId, _instance);
	
	        final String[] macAddresses = _dcDao.getNextAvailableMacAddressPair(dc.getId());
	        long routerId = -1;
	        long poolId = 0;
	        Pair<HostPodVO, Long> pod = null;
	        DomainRouterVO router = null;
            Set<Long> avoids = new HashSet<Long>();
            VlanVO guestVlan = null;
            List<VlanVO> vlansForAccount = _vlanDao.listVlansForAccountByType(dc.getId(), account.getId(), VlanType.DirectAttached);
            List<VlanVO> vlansForPod = null;
            List<VlanVO> zoneWideVlans = null;
            int freeIpCount = 0;
            boolean forAccount = false;
            
            if (vlansForAccount.size() > 0) {
            	//iterate over the vlan to see if there are actually addresses available 
            	for(VlanVO vlan:vlansForAccount)
            	{
            		freeIpCount = (_ipAddressDao.countIPs(dc.getId(), vlan.getId(), false) - _ipAddressDao.countIPs(dc.getId(), vlan.getId(), true));
            		
            		if(freeIpCount>0)
            		{
            			forAccount = true;
                    	guestVlan = vlan;
                    	break;
            		}
            	}
            	
            }
	        
            if(!forAccount)
	        {
	          	//list zone wide vlans that are direct attached and tagged
	          	//if exists pick random one
	          	//set forZone = true
	          	
	          	//note the dao method below does a NEQ on vlan id, hence passing untagged
	        	zoneWideVlans = _vlanDao.searchForZoneWideVlans(dc.getId(),VlanType.DirectAttached.toString(),"untagged");
	          	
	          	if(zoneWideVlans!=null && zoneWideVlans.size()>0)
	          	{
	          		//iterate over zone vlans to see if addresses are available
	          		for(VlanVO vlan : zoneWideVlans)
	          		{
	          			freeIpCount = (_ipAddressDao.countIPs(dc.getId(), vlan.getId(), false) - _ipAddressDao.countIPs(dc.getId(), vlan.getId(), true));
	          		
		          		if(freeIpCount>0)
		          		{
		          			forZone = true;
		          			guestVlan = vlan;
		          			break;
		          		}
	          		}
	          	}
	        }
            
            while ((pod = _agentMgr.findPod(template, offering, dc, account.getId(), avoids)) != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Attempting to create direct attached vm in pod " + pod.first().getName());
                }
        		avoids.add(pod.first().getId());
                if (!forAccount && !forZone) {
                	vlansForPod = _vlanDao.listVlansForPodByType(pod.first().getId(), VlanType.DirectAttached);
                	if (vlansForPod.size() < 1) {
                		if (s_logger.isDebugEnabled()) {
                			s_logger.debug("No direct attached vlans available in pod " + pod.first().getName() + " (id:" + pod.first().getId() + "), checking other pods");
                		}
                		continue;
                	}
                	guestVlan = vlansForPod.get(0);//FIXME: iterate over all vlans
                }
                
                List<DomainRouterVO> rtrs = _routerDao.listByVlanDbId(guestVlan.getId());
                assert rtrs.size() < 2 : "How did we get more than one router per vlan?";
                if (rtrs.size() > 0) {
                	router =  rtrs.get(0);
                	routerId = router.getId();
                } else if (rtrs.size() == 0) {
                	router = _networkMgr.createDhcpServerForDirectlyAttachedGuests(userId, accountId, dc, pod.first(), pod.second(), guestVlan);
                	if (router == null) {
    	                if (s_logger.isDebugEnabled()) {
    	                    s_logger.debug("Unable to create DHCP server in vlan " + guestVlan.getVlanId() + ", pod=" + pod.first().getName() + " (podid:" + pod.first().getId() + "), checking other pods");
    	                }
                		continue;
                	}
                	routerId = router.getId();
                }
                
                
                String guestIp = null;
                                
                if(forAccount)
                {
                	for(VlanVO vlanForAcc : vlansForAccount)
                	{
                		guestIp = _ipAddressDao.assignIpAddress(accountId, account.getDomainId(), vlanForAcc.getId(), false);
                		if(guestIp!=null)
                			break; //got an ip
                	}
                }
                else if(!forAccount && !forZone)
                {
                	//i.e. for pod
                	for(VlanVO vlanForPod : vlansForPod)
                	{
                		guestIp = _ipAddressDao.assignIpAddress(accountId, account.getDomainId(), vlanForPod.getId(), false);
                		if(guestIp!=null)
                			break;//got an ip
                	}
                }
                else
                {
                	//for zone
                	for(VlanVO vlanForZone : zoneWideVlans)
                	{
                		guestIp = _ipAddressDao.assignIpAddress(accountId, account.getDomainId().longValue(), vlanForZone.getId(), false);
                		if(guestIp!=null)
                			break;//found an ip
                	}
                }
                
                if (guestIp == null) {
                	s_logger.debug("No guest IP available in pod id=" + pod.first().getId());
                	continue;
                }
                s_logger.debug("Acquired a guest IP, ip=" + guestIp);
                String guestMacAddress = macAddresses[0];
                String externalMacAddress = macAddresses[1];
                Long externalVlanDbId = null;
            
	            vm = new UserVmVO(vmId, name, templateId, guestOSId, accountId, account.getDomainId().longValue(),
	            		serviceOfferingId, guestMacAddress, guestIp, guestVlan.getVlanNetmask(),
	            		null, externalMacAddress, externalVlanDbId,
	            		routerId, pod.first().getId(), dataCenterId,
	            		offering.getOfferHA(), displayName, group, userData);
	            
	            if (diskOffering != null) {
                	vm.setMirroredVols(diskOffering.isMirrored());
                }
	
	            vm.setLastHostId(pod.second());
	            vm = _vmDao.persist(vm);
	            boolean addedToGroups = _networkGroupManager.addInstanceToGroups(vmId, networkGroups);
	            if (!addedToGroups) {
	            	s_logger.warn("Not all specified network groups can be found");
	            	_vmDao.delete(vm.getId());
	            	throw new InvalidParameterValueException("Not all specified network groups can be found");
	            }
	            
	            vm = _vmDao.findById(vmId);
	            try {
	            	poolId = _storageMgr.createUserVM(account,  vm, template, dc, pod.first(), offering, diskOffering, a);
	            } catch (CloudRuntimeException e) {
	            	_vmDao.delete(vmId);
	                _ipAddressDao.unassignIpAddress(guestIp);
	                s_logger.debug("Released a guest ip address because we could not find storage: ip=" + guestIp);
	                guestIp = null;
	                if (s_logger.isDebugEnabled()) {
	                    s_logger.debug("Unable to find storage host in pod " + pod.first().getName() + " (id:" + pod.first().getId() + "), checking other pods");
	                }
	                continue; // didn't find a storage host in pod, go to the next pod
	            }
	            
	            EventVO event = new EventVO();
		        event.setUserId(userId);
		        event.setAccountId(accountId);
		        event.setType(EventTypes.EVENT_NET_IP_ASSIGN);
		        event.setParameters("guestIPaddress=" + guestIp + "\nvmName=" + vm.getName() +  "\ndcId=" + vm.getDataCenterId());
	            event.setDescription("acquired a public ip: " + guestIp);
	            _eventDao.persist(event);
	            
	            break; // if we got here, we found a host and can stop searching the pods
	        }
	
	        txn.start();
	
	        EventVO event = new EventVO();
	        event.setUserId(userId);
	        event.setAccountId(accountId);
	        event.setType(EventTypes.EVENT_VM_CREATE);
	        event.setStartId(startEventId);
	        event.setState(EventState.Completed);
	        String diskOfferingIdentifier = (diskOffering != null) ? String.valueOf(diskOffering.getId()) : "-1";
	
	        if (poolId == 0) {
	        	if(vm != null && vm.getName()!=null && vm.getDisplayName() != null)
	        	{
	        		if(!vm.getName().equals(vm.getDisplayName()))
	        			event.setDescription("failed to create VM instance : " + name+"("+vm.getInstanceName()+")");
	        		else
	        			event.setDescription("failed to create VM instance : " + name);
	        	}
	        	else
	        	{
	        		event.setDescription("failed to create VM instance : " + name);
	        	}
	            event.setLevel(EventVO.LEVEL_ERROR);
	            _eventDao.persist(event);
		        String eventParams = "\nvmName=" + name + "\nsoId=" + serviceOfferingId + "\ndoId=" + diskOfferingIdentifier + "\ntId=" + templateId + "\ndcId=" + dataCenterId;
		        event.setParameters(eventParams);
	            _accountMgr.decrementResourceCount(account.getId(), ResourceType.user_vm);
	            _accountMgr.decrementResourceCount(account.getId(), ResourceType.volume, numVolumes);
	            txn.commit();
	            return null;
	        }
	        String eventParams = "id=" + vm.getId() + "\nvmName=" + vm.getName() + "\nsoId=" + vm.getServiceOfferingId() + "\ndoId=" + diskOfferingIdentifier + "\ntId=" + vm.getTemplateId() + "\ndcId=" + vm.getDataCenterId();
	        event.setParameters(eventParams);
	        if(!vm.getName().equals(vm.getDisplayName()))
	        	event.setDescription("successfully created VM instance : " + vm.getName()+"("+vm.getInstanceName()+")");
	        else
	        	event.setDescription("successfully created VM instance : " + vm.getName());
	        _eventDao.persist(event);
	        
	        _vmDao.updateIf(vm, Event.OperationSucceeded, null);
	        if (s_logger.isDebugEnabled()) {
	            s_logger.debug("vm created " + vmId);
	        }
	        txn.commit();
	
	        return _vmDao.findById(vmId);
	    } catch (Throwable th) {
            _accountMgr.decrementResourceCount(account.getId(), ResourceType.user_vm);
            _accountMgr.decrementResourceCount(account.getId(), ResourceType.volume, numVolumes);

	        s_logger.error("Unable to create vm", th);
	        throw new CloudRuntimeException("Unable to create vm", th);
	    }
	}
    
    @DB
    @Override
	public UserVmVO createDirectlyAttachedVMExternal(Long vmId, long userId, AccountVO account, DataCenterVO dc, ServiceOfferingVO offering, VMTemplateVO template, DiskOfferingVO diskOffering, String displayName, String group, String userData, List<StoragePoolVO> a, List<NetworkGroupVO>  networkGroups, long startEventId) throws InternalErrorException, ResourceAllocationException {
	    long accountId = account.getId();
	    long dataCenterId = dc.getId();
	    long serviceOfferingId = offering.getId();
	    long templateId = -1;
	    if (template != null)
	    	templateId = template.getId();
	    
	    if (s_logger.isDebugEnabled()) {
	        s_logger.debug("Creating directly attached vm for account id=" + account.getId() +
	        	", name="+ account.getAccountName() + "; dc=" + dc.getName() +
	        	"; offering=" + offering.getId() + "; diskOffering=" + ((diskOffering != null) ? diskOffering.getName() : "none") +
	        	"; template=" + templateId);
	    }
	    
	    // Determine the Guest OS Id
        long guestOSId;
        if (template != null) {
        	guestOSId = template.getGuestOSId();
        } else {
        	throw new InternalErrorException("No template or ISO was specified for the VM.");
        }
	    
        boolean isIso = Storage.ImageFormat.ISO.equals(template.getFormat());
        long numVolumes = (isIso || (diskOffering == null)) ? 1 : 2;
        
	    Transaction txn = Transaction.currentTxn();
	    try {
	        UserVmVO vm = null;
	        txn.start();
	        
	    	account = _accountDao.lock(accountId, true);
	    	if (account == null) {
	    		throw new InternalErrorException("Unable to lock up the account: " + accountId);
	    	}
	
	        // First check that the maximum number of UserVMs for the given accountId will not be exceeded
	        if (_accountMgr.resourceLimitExceeded(account, ResourceType.user_vm)) {
	        	ResourceAllocationException rae = new ResourceAllocationException("Maximum number of virtual machines for account: " + account.getAccountName() + " has been exceeded.");
	        	rae.setResourceType("vm");
	        	throw rae;
	        }
	    	
	    	final String name = VirtualMachineName.getVmName(vmId, accountId, _instance);
	
	        final String[] macAddresses = _dcDao.getNextAvailableMacAddressPair(dc.getId());
	        Long routerId = null;
	        long poolId = 0;
	        Pair<HostPodVO, Long> pod = null;
            Set<Long> avoids = new HashSet<Long>();
            while ((pod = _agentMgr.findPod(template, offering, dc, account.getId(), avoids)) != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Attempting to create direct attached vm in pod " + pod.first().getName());
                }
            	avoids.add(pod.first().getId());
                String guestMacAddress = macAddresses[0];
                String externalMacAddress = macAddresses[1];
                
                IpAddrAllocator.IpAddr publicIp = _IpAllocator.getPrivateIpAddress(guestMacAddress, dc.getId(), pod.first().getId());
                String publicIpAddr = null;
                String publicIpNetMask = null;
                if (publicIp == null) {
                	s_logger.debug("Failed to get public ip address from external dhcp server");
                } else {
                	publicIpAddr = publicIp.ipaddr;
                	publicIpNetMask = publicIp.netMask;
                }
	            vm = new UserVmVO(vmId, name, templateId, guestOSId, accountId, account.getDomainId().longValue(),
	            		serviceOfferingId, guestMacAddress, publicIpAddr, publicIpNetMask,
	            		null, externalMacAddress, null,
	            		routerId, pod.first().getId(), dataCenterId,
	            		offering.getOfferHA(), displayName, group, userData);
	            
	            if (diskOffering != null) {
                	vm.setMirroredVols(diskOffering.isMirrored());
                }
	
	            vm.setLastHostId(pod.second());
	            _vmDao.persist(vm);
	            _networkGroupManager.addInstanceToGroups(vmId, networkGroups);
	            
	            _accountMgr.incrementResourceCount(account.getId(), ResourceType.user_vm);
	            _accountMgr.incrementResourceCount(account.getId(), ResourceType.volume, numVolumes);
	            txn.commit();
	
	            vm = _vmDao.findById(vmId);
	            try {
	            	poolId = _storageMgr.createUserVM(account,  vm, template, dc, pod.first(), offering, diskOffering, a);
	            } catch (CloudRuntimeException e) {
	            	_vmDao.delete(vmId);
	                _accountMgr.decrementResourceCount(account.getId(), ResourceType.user_vm);
	                _accountMgr.decrementResourceCount(account.getId(), ResourceType.volume, numVolumes);
	                if (s_logger.isDebugEnabled()) {
	                    s_logger.debug("Unable to find storage host in pod " + pod.first().getName() + " (id:" + pod.first().getId() + "), checking other pods");
	                }
	                continue; // didn't find a storage host in pod, go to the next pod
	            }
	            break; // if we got here, we found a host and can stop searching the pods
	        }
	
	        txn.start();
	
	        EventVO event = new EventVO();
	        event.setUserId(userId);
	        event.setAccountId(accountId);
	        event.setType(EventTypes.EVENT_VM_CREATE);
	        event.setStartId(startEventId);
            event.setState(EventState.Completed);
	        String diskOfferingIdentifier = (diskOffering != null) ? String.valueOf(diskOffering.getId()) : "-1";
	
	        if (poolId == 0) {
	        	if(vm != null && vm.getName()!=null && vm.getDisplayName() != null)
	        	{
	        		if(!vm.getName().equals(vm.getDisplayName()))
	        			event.setDescription("failed to create VM instance : " + name+"("+vm.getDisplayName()+")");
	        		else
	        			event.setDescription("failed to create VM instance : " + name);
	        	}
	        	else
	        	{
	        		event.setDescription("failed to create VM instance : " + name);
	        	}
	        	
	            event.setLevel(EventVO.LEVEL_ERROR);
	            _eventDao.persist(event);
		        String eventParams = "\nvmName=" + name + "\nsoId=" + serviceOfferingId + "\ndoId=" + diskOfferingIdentifier + "\ntId=" + templateId + "\ndcId=" + dataCenterId;
		        event.setParameters(eventParams);
	            _accountMgr.decrementResourceCount(account.getId(), ResourceType.user_vm);
	            _accountMgr.decrementResourceCount(account.getId(), ResourceType.volume, numVolumes);
	            txn.commit();
	            return null;
	        }
	        String eventParams = "id=" + vm.getId() + "\nvmName=" + vm.getName() + "\nsoId=" + vm.getServiceOfferingId() + "\ndoId=" + diskOfferingIdentifier + "\ntId=" + vm.getTemplateId() + "\ndcId=" + vm.getDataCenterId();
	        event.setParameters(eventParams);
	        if(!vm.getName().equals(vm.getDisplayName()))
	        	event.setDescription("successfully created VM instance : " + vm.getName()+"("+vm.getDisplayName()+")");
	        else
	        	event.setDescription("successfully created VM instance : " + vm.getName());
	        _eventDao.persist(event);
	        
	        _vmDao.updateIf(vm, Event.OperationSucceeded, null);
	        if (s_logger.isDebugEnabled()) {
	            s_logger.debug("vm created " + vmId);
	        }
	        txn.commit();
	
	        return _vmDao.findById(vmId);
	    } catch (ResourceAllocationException rae) {
            _accountMgr.decrementResourceCount(account.getId(), ResourceType.user_vm);
            _accountMgr.decrementResourceCount(account.getId(), ResourceType.volume, numVolumes);
	        if (s_logger.isInfoEnabled()) {
	            s_logger.info("Failed to create VM for account " + accountId + " due to maximum number of virtual machines exceeded.");
	        }
	    	throw rae;
	    } catch (Throwable th) {
            _accountMgr.decrementResourceCount(account.getId(), ResourceType.user_vm);
            _accountMgr.decrementResourceCount(account.getId(), ResourceType.volume, numVolumes);
	        s_logger.error("Unable to create vm", th);
	        throw new CloudRuntimeException("Unable to create vm", th);
	    }
	}

	protected class ExpungeTask implements Runnable {
    	UserVmManagerImpl _vmMgr;
    	public ExpungeTask(UserVmManagerImpl vmMgr) {
    		_vmMgr = vmMgr;
    	}

		public void run() {
			GlobalLock scanLock = GlobalLock.getInternLock("UserVMExpunge");
			try {
				if(scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
					try {
						reallyRun();
					} finally {
						scanLock.unlock();
					}
				}
			} finally {
				scanLock.releaseRef();
			}
		}
    	
    	public void reallyRun() {
    		try {
    			s_logger.info("UserVm Expunge Thread is running.");
				_vmMgr.expunge();
    		} catch (Exception e) {
    			s_logger.error("Caught the following Exception", e);
    		}
    	}
    }
	
}
