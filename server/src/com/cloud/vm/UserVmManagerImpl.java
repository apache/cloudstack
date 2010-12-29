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
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.GetVmStatsAnswer;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.SnapshotCommand;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.storage.CreatePrivateTemplateAnswer;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.api.commands.AttachVolumeCmd;
import com.cloud.api.commands.CreateTemplateCmd;
import com.cloud.api.commands.CreateVMGroupCmd;
import com.cloud.api.commands.DeleteVMGroupCmd;
import com.cloud.api.commands.DeployVMCmd;
import com.cloud.api.commands.DestroyVMCmd;
import com.cloud.api.commands.DetachVolumeCmd;
import com.cloud.api.commands.RebootVMCmd;
import com.cloud.api.commands.RecoverVMCmd;
import com.cloud.api.commands.ResetVMPasswordCmd;
import com.cloud.api.commands.StartVMCmd;
import com.cloud.api.commands.StopVMCmd;
import com.cloud.api.commands.UpdateVMCmd;
import com.cloud.api.commands.UpgradeVMCmd;
import com.cloud.async.AsyncJobExecutor;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.Event;
import com.cloud.event.EventTypes;
import com.cloud.event.EventUtils;
import com.cloud.event.EventVO;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.DetailsDao;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IPAddressVO;
import com.cloud.network.IpAddrAllocator;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.VirtualNetworkApplianceManager;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Storage.StorageResourceType;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.InstanceGroupVMMapDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
@Local(value={UserVmManager.class, UserVmService.class})
public class UserVmManagerImpl implements UserVmManager, UserVmService, Manager {
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
    @Inject DomainDao _domainDao = null;
    @Inject ResourceLimitDao _limitDao = null;
    @Inject UserVmDao _vmDao = null;
    @Inject VolumeDao _volsDao = null;
    @Inject DataCenterDao _dcDao = null;
    @Inject FirewallRulesDao _rulesDao = null;
    @Inject LoadBalancerVMMapDao _loadBalancerVMMapDao = null;
    @Inject LoadBalancerDao _loadBalancerDao = null;
    @Inject IPAddressDao _ipAddressDao = null;
    @Inject HostPodDao _podDao = null;
    @Inject CapacityDao _capacityDao = null;
    @Inject NetworkManager _networkMgr = null;
    @Inject StorageManager _storageMgr = null;
    @Inject SnapshotManager _snapshotMgr = null;
    @Inject AgentManager _agentMgr = null;
    @Inject ConfigurationManager _configMgr = null;
    @Inject AccountDao _accountDao = null;
    @Inject UserDao _userDao = null;
    @Inject SnapshotDao _snapshotDao = null;
    @Inject GuestOSDao _guestOSDao = null;
    @Inject GuestOSCategoryDao _guestOSCategoryDao = null;
    @Inject HighAvailabilityManager _haMgr = null;
    @Inject AlertManager _alertMgr = null;
    @Inject AccountManager _accountMgr;
    @Inject AccountService _accountService;
    @Inject AsyncJobManager _asyncMgr;
    @Inject VlanDao _vlanDao;
    @Inject AccountVlanMapDao _accountVlanMapDao;
    @Inject StoragePoolDao _storagePoolDao;
    @Inject VMTemplateHostDao _vmTemplateHostDao;
    @Inject SecurityGroupManager _networkGroupMgr;
    @Inject ServiceOfferingDao _serviceOfferingDao;
    @Inject NetworkOfferingDao _networkOfferingDao;
    @Inject EventDao _eventDao = null;
    @Inject InstanceGroupDao _vmGroupDao;
    @Inject InstanceGroupVMMapDao _groupVMMapDao;
    @Inject VirtualMachineManager _itMgr;
    @Inject NetworkDao _networkDao;
    @Inject VirtualNetworkApplianceManager _routerMgr;
    @Inject NicDao _nicDao;
    @Inject RulesManager _rulesMgr;
    @Inject LoadBalancingRulesManager _lbMgr;
    @Inject UsageEventDao _usageEventDao;
    
    private IpAddrAllocator _IpAllocator;
    ScheduledExecutorService _executor = null;
    int _expungeInterval;
    int _expungeDelay;
    int _retry = 2;

    String _name;
    String _instance;
    String _zone;
    String _defaultNetworkDomain;

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
    public UserVm resetVMPassword(ResetVMPasswordCmd cmd, String password){
        Account account = UserContext.current().getAccount();
    	Long userId = UserContext.current().getUserId();
    	Long vmId = cmd.getId();
    	UserVmVO userVm = _vmDao.findById(cmd.getId());
    	
    	//Do parameters input validation
    	if (userVm == null) {
    	    throw new InvalidParameterValueException("unable to find a virtual machine with id " + cmd.getId());
    	}
    	
    	VMTemplateVO template = _templateDao.findById(userVm.getTemplateId());
    	if (template == null || !template.getEnablePassword()) {
    	    throw new InvalidParameterValueException("Fail to reset password for the virtual machine, the template is not password enabled");
    	}
    	
    	userId = accountAndUserValidation(vmId, account, userId, userVm);
        
    	boolean result = resetVMPasswordInternal(cmd, password);
       
        if (result) {
            userVm.setPassword(password);
        }
      
        return userVm;
    }

    private boolean resetVMPasswordInternal(ResetVMPasswordCmd cmd, String password) {    	
        Long vmId = cmd.getId();
        Long userId = UserContext.current().getUserId();
        UserVmVO vmInstance = _vmDao.findById(vmId);

        if (password == null || password.equals("")) {
            return false;
        }

        VMTemplateVO template = _templateDao.findById(vmInstance.getTemplateId());
        if (template.getEnablePassword()) {
        	if (vmInstance.getDomainRouterId() == null) {
                /*TODO: add it for external dhcp mode*/
        		return true;
            }
	        if (_routerMgr.savePasswordToRouter(vmInstance.getDomainRouterId(), vmInstance.getPrivateIpAddress(), password)) {
	            // Need to reboot the virtual machine so that the password gets redownloaded from the DomR, and reset on the VM
	            long startId = EventUtils.saveStartedEvent(userId, vmInstance.getAccountId(), EventTypes.EVENT_VM_REBOOT, "Reboot vm with id:"+vmId);
	        	if (!rebootVirtualMachine(userId, vmId)) {
	        	    EventUtils.saveEvent(userId, vmInstance.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_REBOOT, "Failed to reboot vm with id:"+vmId, startId);
	        		if (vmInstance.getState() == State.Stopped) {
	        			return true;
	        		}
	        		return false;
	        	} else {
	                EventUtils.saveEvent(userId, vmInstance.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_VM_REBOOT, "Successfully rebooted vm with id:"+vmId, startId);
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
    public boolean stopVirtualMachine(long userId, long vmId) {
        boolean status = false;
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

        long startEventId = EventUtils.saveStartedEvent(userId, vm.getAccountId(), EventTypes.EVENT_VM_STOP, "stopping Vm with Id: "+vmId);
        
        status = stop(userId, vm);
       
        if(status){
            EventUtils.saveEvent(userId, vm.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_VM_STOP, "Successfully stopped VM instance : " + vmId, startEventId);
            return status;
            }
        else {
            EventUtils.saveEvent(userId, vm.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_STOP, "Error stopping VM instance : " + vmId, startEventId);
            return status;
        }
    }

    
    @Override
    public Volume attachVolumeToVM(AttachVolumeCmd command) {
    	Long vmId = command.getVirtualMachineId();
    	Long volumeId = command.getId();
    	Long deviceId = command.getDeviceId();
    	Account account = UserContext.current().getAccount();
    	
    	// Check that the volume ID is valid
    	VolumeVO volume = _volsDao.findById(volumeId);
        // Check that the volume is a data volume
        if (volume == null || volume.getVolumeType() != VolumeType.DATADISK) {
            throw new InvalidParameterValueException("Please specify a valid data volume.");
        }

        // Check that the volume is stored on shared storage
        if (!Volume.State.Allocated.equals(volume.getState()) && !_storageMgr.volumeOnSharedStoragePool(volume)) {
            throw new InvalidParameterValueException("Please specify a volume that has been created on a shared storage pool.");
        }
        
        // Check that the volume is not currently attached to any VM
        if (volume.getInstanceId() != null) {
            throw new InvalidParameterValueException("Please specify a volume that is not attached to any VM.");
        }

        // Check that the volume is not destroyed
        if (volume.getDestroyed()) {
            throw new InvalidParameterValueException("Please specify a volume that is not destroyed.");
        }

    	// Check that the virtual machine ID is valid and it's a user vm
    	UserVmVO vm = _vmDao.findById(vmId);
    	if (vm == null || vm.getType() != VirtualMachine.Type.User) {
            throw new InvalidParameterValueException("Please specify a valid User VM.");
        }
    	
        // Check that the VM is in the correct state
        if (vm.getState() != State.Running && vm.getState() != State.Stopped) {
        	throw new InvalidParameterValueException("Please specify a VM that is either running or stopped.");
        }

        // Check that the device ID is valid
        if( deviceId != null ) {
            if(deviceId.longValue() == 0) {
                throw new ServerApiException (BaseCmd.PARAM_ERROR, "deviceId can't be 0, which is used by Root device");
            }
        }
        
        // Check that the VM has less than 6 data volumes attached
        List<VolumeVO> existingDataVolumes = _volsDao.findByInstanceAndType(vmId, VolumeType.DATADISK);
        if (existingDataVolumes.size() >= 6) {
            throw new InvalidParameterValueException("The specified VM already has the maximum number of data disks (6). Please specify another VM.");
        }
        
        // Check that the VM and the volume are in the same zone
        if (vm.getDataCenterId() != volume.getDataCenterId()) {
        	throw new InvalidParameterValueException("Please specify a VM that is in the same zone as the volume.");
        }
        
        //Verify account information
        if (volume.getAccountId() != vm.getAccountId()) {
        	throw new PermissionDeniedException ("Virtual machine and volume belong to different accounts, can not attach. Permission denied.");
        }
    	
    	// If the account is not an admin, check that the volume and the virtual machine are owned by the account that was passed in
    	if (account != null) {
    	    if (!isAdmin(account.getType())) {
                if (account.getId() != volume.getAccountId()) {
                    throw new PermissionDeniedException("Unable to find volume with ID: " + volumeId + " for account: " + account.getAccountName() + ". Permission denied.");
                }

                if (account.getId() != vm.getAccountId()) {
                    throw new PermissionDeniedException("Unable to find VM with ID: " + vmId + " for account: " + account.getAccountName() + ". Permission denied");
                }
    	    } else {
    	        if (!_domainDao.isChildDomain(account.getDomainId(), volume.getDomainId()) ||
    	            !_domainDao.isChildDomain(account.getDomainId(), vm.getDomainId())) {
                    throw new PermissionDeniedException("Unable to attach volume " + volumeId + " to virtual machine instance " + vmId + ". Permission denied.");
    	        }
    	    }
    	}

        VolumeVO rootVolumeOfVm = null;
        List<VolumeVO> rootVolumesOfVm = _volsDao.findByInstanceAndType(vmId, VolumeType.ROOT);
        if (rootVolumesOfVm.size() != 1) {
        	throw new CloudRuntimeException("The VM " + vm.getName() + " has more than one ROOT volume and is in an invalid state. Please contact Cloud Support.");
        } else {
        	rootVolumeOfVm = rootVolumesOfVm.get(0);
        }
        
        HypervisorType rootDiskHyperType = _volsDao.getHypervisorType(rootVolumeOfVm.getId());
        
        if (volume.getState().equals(Volume.State.Allocated)) {
    		/*Need to create the volume*/
        	VMTemplateVO rootDiskTmplt = _templateDao.findById(vm.getTemplateId());
        	DataCenterVO dcVO = _dcDao.findById(vm.getDataCenterId());
        	HostPodVO pod = _podDao.findById(vm.getPodId());
        	StoragePoolVO rootDiskPool = _storagePoolDao.findById(rootVolumeOfVm.getPoolId());
        	ServiceOfferingVO svo = _serviceOfferingDao.findById(vm.getServiceOfferingId());
        	DiskOfferingVO diskVO = _diskOfferingDao.findById(volume.getDiskOfferingId());
        	       
        	volume = _storageMgr.createVolume(volume, vm, rootDiskTmplt, dcVO, pod, rootDiskPool.getClusterId(), svo, diskVO, new ArrayList<StoragePoolVO>(), volume.getSize(), rootDiskHyperType);
        	
        	if (volume == null) {
        		throw new CloudRuntimeException("Failed to create volume when attaching it to VM: " + vm.getName());
        	}       	
    	}
        
        HypervisorType dataDiskHyperType = _volsDao.getHypervisorType(volume.getId());
        if (rootDiskHyperType != dataDiskHyperType) {
        	throw new InvalidParameterValueException("Can't attach a volume created by: " + dataDiskHyperType + " to a " + rootDiskHyperType + " vm");
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
        	throw new CloudRuntimeException("There are no storage pools in the VM's " + poolType + " with all of the volume's tags (" + volumeDiskOffering.getTags() + ").");
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
    		volume = _storageMgr.moveVolume(volume, vmRootVolumePool.getDataCenterId(), vmRootVolumePool.getPodId(), vmRootVolumePool.getClusterId(), dataDiskHyperType);
    	}
    	
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if(asyncExecutor != null) {
        	AsyncJobVO job = asyncExecutor.getJob();

        	if(s_logger.isInfoEnabled()) {
                s_logger.info("Trying to attaching volume " + volumeId +" to vm instance:"+vm.getId()+ ", update async job-" + job.getId() + " progress status");
            }
        	
        	_asyncMgr.updateAsyncJobAttachment(job.getId(), "volume", volumeId);
        	_asyncMgr.updateAsyncJobStatus(job.getId(), BaseCmd.PROGRESS_INSTANCE_CREATED, volumeId);
        }
    	
    	String errorMsg = "Failed to attach volume: " + volume.getName() + " to VM: " + vm.getName();
    	boolean sendCommand = (vm.getState() == State.Running);
    	AttachVolumeAnswer answer = null;
    	Long hostId = vm.getHostId();
    	if(hostId  == null) {
    		hostId = vm.getLastHostId();
    		HostVO host = _hostDao.findById(hostId);
    		if(host != null && host.getHypervisorType() == HypervisorType.VmWare) {
                sendCommand = true;
            }
    	}
    	
    	if (sendCommand) {
    		StoragePoolVO volumePool = _storagePoolDao.findById(volume.getPoolId());
    		AttachVolumeCommand cmd = new AttachVolumeCommand(true, vm.getInstanceName(), volume.getPoolType(), volume.getFolder(), volume.getPath(), volume.getName(), deviceId, volume.getChainInfo());
			cmd.setPoolUuid(volumePool.getUuid());
    		
    		try {
    			answer = (AttachVolumeAnswer)_agentMgr.send(hostId, cmd);
    		} catch (Exception e) {
    			throw new CloudRuntimeException(errorMsg + " due to: " + e.getMessage());
    		}
    	}

        if (!sendCommand || (answer != null && answer.getResult())) {
    		// Mark the volume as attached
            if( sendCommand ) {
                _volsDao.attachVolume(volume.getId(), vmId, answer.getDeviceId());
            } else {
                _volsDao.attachVolume(volume.getId(), vmId, deviceId);
            }
            return _volsDao.findById(volumeId);
    	} else {
    		if (answer != null) {
    			String details = answer.getDetails();
    			if (details != null && !details.isEmpty()) {
                    errorMsg += "; " + details;
                }
    		}
    		throw new CloudRuntimeException(errorMsg);
    	}
    }
    
    @Override
    public Volume detachVolumeFromVM(DetachVolumeCmd cmmd) {    	
    	Account account = UserContext.current().getAccount();
    	if ((cmmd.getId() == null && cmmd.getDeviceId() == null && cmmd.getVirtualMachineId() == null) ||
    	    (cmmd.getId() != null && (cmmd.getDeviceId() != null || cmmd.getVirtualMachineId() != null)) ||
    	    (cmmd.getId() == null && (cmmd.getDeviceId()==null || cmmd.getVirtualMachineId() == null))) {
    	    throw new InvalidParameterValueException("Please provide either a volume id, or a tuple(device id, instance id)");
    	}

    	Long volumeId = cmmd.getId();
    	VolumeVO volume = null;
    	
    	if(volumeId != null) {
    		volume = _volsDao.findById(volumeId);
    	} else {
    		volume = _volsDao.findByInstanceAndDeviceId(cmmd.getVirtualMachineId(), cmmd.getDeviceId()).get(0);
    	}

    	Long vmId = null;
    	
    	if (cmmd.getVirtualMachineId() == null) {
    		vmId = volume.getInstanceId();
    	} else {
    		vmId = cmmd.getVirtualMachineId();
    	}

    	boolean isAdmin;
    	if (account == null) {
    		// Admin API call
    		isAdmin = true;
    	} else {
    		// User API call
    		isAdmin = isAdmin(account.getType());
    	}

    	// Check that the volume ID is valid
    	if (volume == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find volume with ID: " + volumeId);
        }

    	// If the account is not an admin, check that the volume is owned by the account that was passed in
    	if (!isAdmin) {
    		if (account.getId() != volume.getAccountId()) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find volume with ID: " + volumeId + " for account: " + account.getAccountName());
            }
    	} else if (account != null) {
    	    if (!_domainDao.isChildDomain(account.getDomainId(), volume.getDomainId())) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to detach volume with ID: " + volumeId + ", permission denied.");
    	    }
    	}

        // Check that the volume is a data volume
        if (volume.getVolumeType() != VolumeType.DATADISK) {
            throw new InvalidParameterValueException("Please specify a data volume.");
        }

        // Check that the volume is stored on shared storage
        if (!_storageMgr.volumeOnSharedStoragePool(volume)) {
            throw new InvalidParameterValueException("Please specify a volume that has been created on a shared storage pool.");
        }

        // Check that the volume is currently attached to a VM
        if (vmId == null) {
            throw new InvalidParameterValueException("The specified volume is not attached to a VM.");
        }

        // Check that the VM is in the correct state
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm.getState() != State.Running && vm.getState() != State.Stopped) {
        	throw new InvalidParameterValueException("Please specify a VM that is either running or stopped.");
        }
        
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if(asyncExecutor != null) {
        	AsyncJobVO job = asyncExecutor.getJob();

        	if(s_logger.isInfoEnabled()) {
                s_logger.info("Trying to attaching volume " + volumeId +"to vm instance:"+vm.getId()+ ", update async job-" + job.getId() + " progress status");
            }
        	
        	_asyncMgr.updateAsyncJobAttachment(job.getId(), "volume", volumeId);
        	_asyncMgr.updateAsyncJobStatus(job.getId(), BaseCmd.PROGRESS_INSTANCE_CREATED, volumeId);
        }
    	
    	String errorMsg = "Failed to detach volume: " + volume.getName() + " from VM: " + vm.getName();
    	boolean sendCommand = (vm.getState() == State.Running);
    	Answer answer = null;
    	
    	if (sendCommand) {
			AttachVolumeCommand cmd = new AttachVolumeCommand(false, vm.getInstanceName(), volume.getPoolType(), volume.getFolder(), volume.getPath(), volume.getName(), 
				cmmd.getDeviceId() != null ? cmmd.getDeviceId() : volume.getDeviceId(), volume.getChainInfo());

			StoragePoolVO volumePool = _storagePoolDao.findById(volume.getPoolId());
			cmd.setPoolUuid(volumePool.getUuid());

			try {
    			answer = _agentMgr.send(vm.getHostId(), cmd);
    		} catch (Exception e) {
    			throw new CloudRuntimeException(errorMsg + " due to: " + e.getMessage());
    		}
    	}
    	
		if (!sendCommand || (answer != null && answer.getResult())) {
			// Mark the volume as detached
    		_volsDao.detachVolume(volume.getId());
    		if(answer != null && answer instanceof AttachVolumeAnswer) {
    			volume.setChainInfo(((AttachVolumeAnswer)answer).getChainInfo());
    			_volsDao.update(volume.getId(), volume);
    		}
    		
            return _volsDao.findById(volumeId);
    	} else {
    		
    		if (answer != null) {
    			String details = answer.getDetails();
    			if (details != null && !details.isEmpty()) {
                    errorMsg += "; " + details;
                }
    		}
    		
    		throw new CloudRuntimeException(errorMsg);
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
        String isoPath;
        VMTemplateVO tmplt = _templateDao.findById(isoId);   
        if ( tmplt == null ) {
            s_logger.warn("ISO: " + isoId +" does not exist");
            return false;
        }
        // Get the path of the ISO
        Pair<String, String> isoPathPair = null;
        if ( tmplt.getTemplateType() == TemplateType.PERHOST ) {
            isoPath = tmplt.getName();
        } else {
            isoPathPair = _storageMgr.getAbsoluteIsoPath(isoId, vm.getDataCenterId()); 	
            if (isoPathPair == null) {
	    	    s_logger.warn("Couldn't get absolute iso path");
	    	    return false;
	    	} else {
	    	    isoPath = isoPathPair.first();
	    	}
        }

    	String vmName = vm.getInstanceName();

    	HostVO host = _hostDao.findById(vm.getHostId());
    	if (host == null) {
            s_logger.warn("Host: " + vm.getHostId() +" does not exist");
            return false;
    	}
    	AttachIsoCommand cmd = new AttachIsoCommand(vmName, isoPath, attach);
    	if (isoPathPair != null) {
    		cmd.setStoreUrl(isoPathPair.second());
    	}
    	Answer a = _agentMgr.easySend(vm.getHostId(), cmd);
    	return (a != null);
    }

 
    private boolean rebootVirtualMachine(long userId, long vmId) {
        UserVmVO vm = _vmDao.findById(vmId);

        if (vm == null || vm.getState() == State.Destroyed || vm.getState() == State.Expunging || vm.getRemoved() != null) {
            return false;
        }

        if (vm.getState() == State.Running && vm.getHostId() != null) {
            RebootCommand cmd = new RebootCommand(vm.getInstanceName());
            RebootAnswer answer = (RebootAnswer)_agentMgr.easySend(vm.getHostId(), cmd);
           
            if (answer != null) {
                return true;
            } else {
                return false;
            }
        } else {
            s_logger.error("Vm id=" + vmId + " is not in Running state, failed to reboot");
            return false;
        }
    }
    @Override
    /*
     * TODO: cleanup eventually - Refactored API call
     */
    public UserVm upgradeVirtualMachine(UpgradeVMCmd cmd) throws ServerApiException, InvalidParameterValueException {
        Long virtualMachineId = cmd.getId();
        Long serviceOfferingId = cmd.getServiceOfferingId();
        Account account = UserContext.current().getAccount();
        Long userId = UserContext.current().getUserId();

        // Verify input parameters
        UserVmVO vmInstance = _vmDao.findById(virtualMachineId);
        if (vmInstance == null) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find a virtual machine with id " + virtualMachineId);
        }       

        userId = accountAndUserValidation(virtualMachineId, account, userId,vmInstance);                         
            
        // Check that the specified service offering ID is valid
        ServiceOfferingVO newServiceOffering = _offeringDao.findById(serviceOfferingId);
        if (newServiceOffering == null) {
        	throw new InvalidParameterValueException("Unable to find a service offering with id " + serviceOfferingId);
        }
            
        // Check that the VM is stopped
        if (!vmInstance.getState().equals(State.Stopped)) {
            s_logger.warn("Unable to upgrade virtual machine " + vmInstance.toString() + " in state " + vmInstance.getState());
            throw new InvalidParameterValueException("Unable to upgrade virtual machine " + vmInstance.toString() + " in state " + vmInstance.getState() + "; make sure the virtual machine is stopped and not in an error state before upgrading.");
        }
        
        // Check if the service offering being upgraded to is what the VM is already running with
        if (vmInstance.getServiceOfferingId() == newServiceOffering.getId()) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Not upgrading vm " + vmInstance.toString() + " since it already has the requested service offering (" + newServiceOffering.getName() + ")");
            }
            
            throw new InvalidParameterValueException("Not upgrading vm " + vmInstance.toString() + " since it already has the requested service offering (" + newServiceOffering.getName() + ")");
        }
        
        // Check that the service offering being upgraded to has the same Guest IP type as the VM's current service offering
        ServiceOfferingVO currentServiceOffering = _offeringDao.findById(vmInstance.getServiceOfferingId());
        if (!currentServiceOffering.getGuestIpType().equals(newServiceOffering.getGuestIpType())) {
        	String errorMsg = "The service offering being upgraded to has a guest IP type: " + newServiceOffering.getGuestIpType();
        	errorMsg += ". Please select a service offering with the same guest IP type as the VM's current service offering (" + currentServiceOffering.getGuestIpType() + ").";
        	throw new InvalidParameterValueException(errorMsg);
        }
        
        // Check that the service offering being upgraded to has the same storage pool preference as the VM's current service offering
        if (currentServiceOffering.getUseLocalStorage() != newServiceOffering.getUseLocalStorage()) {
            throw new InvalidParameterValueException("Unable to upgrade virtual machine " + vmInstance.toString() + ", cannot switch between local storage and shared storage service offerings.  Current offering useLocalStorage=" +
                   currentServiceOffering.getUseLocalStorage() + ", target offering useLocalStorage=" + newServiceOffering.getUseLocalStorage());
        }

        // Check that there are enough resources to upgrade the service offering
        if (!_agentMgr.isVirtualMachineUpgradable(vmInstance, newServiceOffering)) {
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

		UserVmVO vmForUpdate = _vmDao.createForUpdate();
		vmForUpdate.setServiceOfferingId(serviceOfferingId);
		vmForUpdate.setHaEnabled(_serviceOfferingDao.findById(serviceOfferingId).getOfferHA());
		_vmDao.update(vmInstance.getId(), vmForUpdate);
		
		return _vmDao.findById(vmInstance.getId());
    }

	private Long accountAndUserValidation(Long virtualMachineId,Account account, Long userId, UserVmVO vmInstance) throws ServerApiException {
		if (account != null) {
            if (!isAdmin(account.getType()) && (account.getId() != vmInstance.getAccountId())) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find a virtual machine with id " + virtualMachineId + " for this account");
            } else if (!_domainDao.isChildDomain(account.getDomainId(),vmInstance.getDomainId())) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid virtual machine id (" + virtualMachineId + ") given, unable to upgrade virtual machine.");
            }
        }

        // If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }
		return userId;
	}

    @Override
    public HashMap<Long, VmStatsEntry> getVirtualMachineStatistics(long hostId, String hostName, List<Long> vmIds) throws CloudRuntimeException {
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
    protected String acquireGuestIpAddress(long dcId, long accountId, UserVmVO userVm) throws CloudRuntimeException {
    	boolean routerLock = false;
        DomainRouterVO router = _routerDao.findBy(accountId, dcId);
        long routerId = router.getId();
        Transaction txn = Transaction.currentTxn();
    	try {
    		txn.start();
        	router = _routerDao.acquireInLockTable(routerId);
        	if (router == null) {
        		throw new CloudRuntimeException("Unable to lock up the router:" + routerId);
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
        	txn.commit();
        	if (routerLock) {
        		_routerDao.releaseFromLockTable(routerId);
        		routerLock = false;
        	}
        	return ipAddressStr;
        }finally {
        	if (routerLock) {
        		_routerDao.releaseFromLockTable(routerId);
        	}
        }
     }
    
    
    @Override
    public void releaseGuestIpAddress(UserVmVO userVm)  {
    	ServiceOffering offering = _offeringDao.findById(userVm.getServiceOfferingId());
    	
    	if (offering.getGuestIpType() != NetworkOffering.GuestIpType.Virtual) {  		
    		IPAddressVO guestIP = (userVm.getGuestIpAddress() == null) ? null : _ipAddressDao.findById(userVm.getGuestIpAddress());
    		if (guestIP != null && guestIP.getAllocatedTime() != null) {
    			_ipAddressDao.unassignIpAddress(userVm.getGuestIpAddress());
            	s_logger.debug("Released guest IP address=" + userVm.getGuestIpAddress() + " vmName=" + userVm.getName() +  " dcId=" + userVm.getDataCenterId());

            	EventUtils.saveEvent(User.UID_SYSTEM, userVm.getAccountId(), EventTypes.EVENT_NET_IP_RELEASE, "released a public ip: " + userVm.getGuestIpAddress());
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
    	_vmDao.update(userVm.getId(), userVm); 
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
        
        long startEventId = EventUtils.saveStartedEvent(userId, vm.getAccountId(), EventTypes.EVENT_VM_STOP, "stopping Vm with Id: "+vmId);
        
        if (!stop(userId, vm)) {
            s_logger.error("Unable to stop vm so we can't destroy it: " + vmId);
            EventUtils.saveEvent(userId, vm.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_STOP, "Error stopping VM instance : " + vmId, startEventId);
            return false;
        } else {
            EventUtils.saveEvent(userId, vm.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_VM_STOP, "Successfully stopped VM instance : " + vmId, startEventId);
        }
       
        Transaction txn = Transaction.currentTxn();
        txn.start();

        if (!destroy(vm)) {
        	return false;
        }
        UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VM_DESTROY, vm.getAccountId(), vm.getDataCenterId(), vm.getId(), vm.getName(), vm.getServiceOfferingId(), vm.getTemplateId(), null);
        _usageEventDao.persist(usageEvent);
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
    public UserVm recoverVirtualMachine(RecoverVMCmd cmd) throws ResourceAllocationException, CloudRuntimeException {
    	
        Long vmId = cmd.getId();
        Account accountHandle = UserContext.current().getAccount();
   
        //if account is removed, return error
        if(accountHandle!=null && accountHandle.getRemoved() != null) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "The account " + accountHandle.getId()+" is removed");
        }

        // Verify input parameters
        UserVmVO vm = _vmDao.findById(vmId.longValue());
        
        if (vm == null) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find a virtual machine with id " + vmId);
        }

        if ((accountHandle != null) && !_domainDao.isChildDomain(accountHandle.getDomainId(), vm.getDomainId())) {
            // the domain in which the VM lives is not in the admin's domain tree
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to recover virtual machine with id " + vmId + ", invalid id given.");
        }

        if (vm.getRemoved() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find vm or vm is removed: " + vmId);
            }
            throw new InvalidParameterValueException("Unable to find vm by id " + vmId);
        }
        
        if (vm.getState() != State.Destroyed) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("vm is not in the right state: " + vmId);
            }
            throw new InvalidParameterValueException("Vm with id " + vmId + " is not in the right state");
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Recovering vm " + vmId);
        }

        Transaction txn = Transaction.currentTxn();
        AccountVO account = null;
    	txn.start();

        account = _accountDao.lockRow(vm.getAccountId(), true);
        
        //if the account is deleted, throw error
        if(account.getRemoved()!=null) {
            throw new CloudRuntimeException("Unable to recover VM as the account is deleted");
        }
        
    	// First check that the maximum number of UserVMs for the given accountId will not be exceeded
        if (_accountMgr.resourceLimitExceeded(account, ResourceType.user_vm)) {
        	ResourceAllocationException rae = new ResourceAllocationException("Maximum number of virtual machines for account: " + account.getAccountName() + " has been exceeded.");
        	rae.setResourceType("vm");
        	txn.commit();
        	throw rae;
        }
        
        _haMgr.cancelDestroy(vm, vm.getHostId());

        _accountMgr.incrementResourceCount(account.getId(), ResourceType.user_vm);

        if (!_itMgr.stateTransitTo(vm, VirtualMachine.Event.RecoveryRequested, null)) {
            s_logger.debug("Unable to recover the vm because it is not in the correct state: " + vmId);
            throw new InvalidParameterValueException("Unable to recover the vm because it is not in the correct state: " + vmId);
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
            diskOfferingId = volume.getDiskOfferingId();
            long sizeMB = volume.getSize()/(1024*1024);
            StoragePoolVO pool = _storagePoolDao.findById(volume.getPoolId());
            EventUtils.saveEvent(User.UID_SYSTEM, volume.getAccountId(), EventTypes.EVENT_VOLUME_CREATE, "Created volume: "+ volume.getName() +" with size: " + sizeMB + " MB in pool: " + pool.getName());
            
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(), diskOfferingId, templateId , sizeMB);
            _usageEventDao.persist(usageEvent);
        }
        
        _accountMgr.incrementResourceCount(account.getId(), ResourceType.volume, new Long(volumes.size()));
        
        UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VM_CREATE, vm.getAccountId(), vm.getDataCenterId(), vm.getId(), vm.getName(), vm.getServiceOfferingId(), vm.getTemplateId(), null);
        _usageEventDao.persist(usageEvent);
        txn.commit();
        
        return _vmDao.findById(vmId);
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
        
        _defaultNetworkDomain = configs.get("domain");
        if (_defaultNetworkDomain == null) {
            _defaultNetworkDomain = ".myvm.com";
        }
        if (!_defaultNetworkDomain.startsWith(".")) {
            _defaultNetworkDomain = "." + _defaultNetworkDomain;
        }

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
        _itMgr.registerGuru(Type.User, this);
        
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
    	_itMgr.stateTransitTo(vm, VirtualMachine.Event.AgentReportRunning, vm.getHostId());
        _networkGroupMgr.handleVmStateTransition(vm, State.Running);

    }
    
    @Override
    public void completeStopCommand(UserVmVO instance) {
    	completeStopCommand(1L, instance, VirtualMachine.Event.AgentReportStopped);
    }
    
    @Override
    @DB
    public void completeStopCommand(long userId, UserVmVO vm, VirtualMachine.Event e) {
        Transaction txn = Transaction.currentTxn();
        try {
        	String vnet = vm.getVnet();
            vm.setVnet(null);
            vm.setProxyAssignTime(null);
            vm.setProxyId(null);

            txn.start();
            
            if (!_itMgr.stateTransitTo(vm, e, null)) {
            	s_logger.debug("Unable to update ");
            	return;
            }
            
            if ((vm.getDomainRouterId() != null) && _vmDao.listBy(vm.getDomainRouterId(), State.Starting, State.Running).size() == 0) {
            	DomainRouterVO router = _routerDao.findById(vm.getDomainRouterId());
            	if (router.getState().equals(State.Stopped)) {
            		_dcDao.releaseVnet(vnet, router.getDataCenterId(), router.getAccountId(), null);
            	}
            }
            
            txn.commit();
            _networkGroupMgr.handleVmStateTransition(vm, State.Stopped);
        } catch (Throwable th) {
            s_logger.error("Error during stop: ", th);
            throw new CloudRuntimeException("Error during stop: ", th);
        }

        UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VM_STOP, vm.getAccountId(), vm.getDataCenterId(), vm.getId(), vm.getName(), vm.getServiceOfferingId(), vm.getTemplateId(), null);
        _usageEventDao.persist(usageEvent);
        
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
    public UserVmVO start(long vmId) throws StorageUnavailableException, ConcurrentOperationException {
        return null; // FIXME start(1L, vmId, null, null, startEventId);
    }

    @Override
    public UserVm startUserVm(long vmId) throws ConcurrentOperationException, ExecutionException, ResourceUnavailableException, InsufficientCapacityException {
        return startVirtualMachine(vmId); 
    }

    @Override
    public boolean stop(UserVmVO vm) {
        return stop(1L, vm);
    }

    private boolean stop(long userId, UserVmVO vm) {
        State state = vm.getState();
        if (state == State.Stopped) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is already stopped: " + vm.toString());
            }
            return true;
        }
        
        if (state == State.Creating || state == State.Destroyed || state == State.Expunging || state == State.Error) {
        	s_logger.warn("Stopped called on " + vm.toString() + " but the state is " + state.toString());
        	return true;
        }
        
        if (!_itMgr.stateTransitTo(vm, VirtualMachine.Event.StopRequested, vm.getHostId())) {
            s_logger.debug("VM is not in a state to stop: " + vm.getState().toString());
            return false;
        }
        
        if (vm.getHostId() == null) {
        	s_logger.debug("Host id is null so we can't stop it.  How did we get into here?");
        	return false;
        }

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
        	completeStopCommand(userId, vm, VirtualMachine.Event.OperationSucceeded);
        } else
        {
            _itMgr.stateTransitTo(vm, VirtualMachine.Event.OperationFailed, vm.getHostId());
            s_logger.error("Unable to stop vm " + vm.getName());
        }

        return stopped;
    }

    @Override @DB
    public boolean destroy(UserVmVO vm) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Destroying vm " + vm.toString());
        }
        if (!_itMgr.stateTransitTo(vm, VirtualMachine.Event.DestroyRequested, vm.getHostId())) {
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
        StoragePoolVO sp = _storageMgr.getStoragePoolForVm(vm.getId());

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
        if (fromHost.getClusterId() == null) {
            s_logger.debug("The host is not in a cluster");
            return null;
        }
        avoid.add(fromHost);

        while ((vmHost = (HostVO)_agentMgr.findHost(Host.Type.Routing, dc, pod, sp, offering, template, vm, null, avoid)) != null) {
            avoid.add(vmHost);

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Trying to migrate router to host " + vmHost.getName());
            }
            
            _storageMgr.share(vm, vols, vmHost, false);

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

    	if (!_itMgr.stateTransitTo(vm, VirtualMachine.Event.MigrationRequested, vm.getHostId())) {
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
    	for (UserVmVO vm : vms) {
    		long vmId = vm.getId();
    		releaseGuestIpAddress(vm);
            vm.setGuestNetmask(null);
            vm.setGuestMacAddress(null);
    		if (!_itMgr.stateTransitTo(vm, VirtualMachine.Event.ExpungeOperation, null)) {
    			s_logger.info("vm " + vmId + " is skipped because it is no longer in Destroyed or Error state");
    			continue;
    		}

           List<VolumeVO> vols = null;
            try {
                vols = _volsDao.findByInstanceIdDestroyed(vmId);
                _storageMgr.destroy(vm, vols);
                 
                //cleanup port forwarding rules
                if (_rulesMgr.revokePortForwardingRule(vmId)) {
                    s_logger.debug("Port forwarding rules are removed successfully as a part of vm id=" + vmId + " expunge");
                } else {
                    s_logger.warn("Fail to remove port forwarding rules as a part of vm id=" + vmId + " expunge");
                }
                
                //cleanup load balancer rules
                if (_lbMgr.removeVmFromLoadBalancers(vmId)) {
                    s_logger.debug("LB rules are removed successfully as a part of vm id=" + vmId + " expunge");
                } else {
                    s_logger.warn("Fail to remove lb rules as a part of vm id=" + vmId + " expunge");
                }
                
                _vmDao.remove(vm.getId());
                _networkGroupMgr.removeInstanceFromGroups(vm.getId());
                removeInstanceFromGroup(vm.getId());
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
            _itMgr.stateTransitTo(vm, VirtualMachine.Event.AgentReportStopped, null);
            return false;
        }

        State state = answer.getState();
        if (state == State.Stopped) {
            s_logger.warn("Unable to complete migration as we can not detect it on " + host.toString());
            _itMgr.stateTransitTo(vm, VirtualMachine.Event.AgentReportStopped, null);
            return false;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Marking port " + answer.getVncPort() + " on " + host.getId());
        }

        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            _itMgr.stateTransitTo(vm, VirtualMachine.Event.OperationSucceeded, host.getId());
            txn.commit();
            _networkGroupMgr.handleVmStateTransition(vm, State.Running);
            return true;
        } catch(Exception e) {
            s_logger.warn("Exception during completion of migration process " + vm.toString());
            return false;
        }
    }
  
    @Override
    public void cleanNetworkRules(long userId, long instanceId) {
//FIXME        UserVmVO vm = _vmDao.findById(instanceId);
//        String guestIpAddr = vm.getGuestIpAddress();
//        long accountId = vm.getAccountId();
//
//        List<LoadBalancerVMMapVO> loadBalancerMappings = _loadBalancerVMMapDao.listByInstanceId(vm.getId());
//        for (LoadBalancerVMMapVO loadBalancerMapping : loadBalancerMappings) {
//            List<PortForwardingRuleVO> lbRules = _rulesDao.listByLoadBalancerId(loadBalancerMapping.getLoadBalancerId());
//            PortForwardingRuleVO targetLbRule = null;
//            for (PortForwardingRuleVO lbRule : lbRules) {
//                if (lbRule.getDestinationIpAddress().equals(guestIpAddr)) {
//                    targetLbRule = lbRule;
//                    targetLbRule.setEnabled(false);
//                    break;
//                }
//            }
//
//            if (targetLbRule != null) {
//                String ipAddress = targetLbRule.getSourceIpAddress();
//                DomainRouterVO router = _routerDao.findById(vm.getDomainRouterId());
//                _networkMgr.updateFirewallRules(ipAddress, lbRules, router);
//
//                // now that the rule has been disabled, delete it, also remove the mapping from the load balancer mapping table
//                _rulesDao.remove(targetLbRule.getId());
//                _loadBalancerVMMapDao.remove(loadBalancerMapping.getId());
//
//                // save off the event for deleting the LB rule
//                EventVO lbRuleEvent = new EventVO();
//                lbRuleEvent.setUserId(userId);
//                lbRuleEvent.setAccountId(accountId);
//                lbRuleEvent.setType(EventTypes.EVENT_NET_RULE_DELETE);
//                lbRuleEvent.setDescription("deleted load balancer rule [" + targetLbRule.getSourceIpAddress() + ":" + targetLbRule.getSourcePort() +
//                        "]->[" + targetLbRule.getDestinationIpAddress() + ":" + targetLbRule.getDestinationPort() + "]" + " " + targetLbRule.getAlgorithm());
//                lbRuleEvent.setLevel(EventVO.LEVEL_INFO);
//                _eventDao.persist(lbRuleEvent);
//            }
//        }
    }
    
    @Override
    public void deletePrivateTemplateRecord(Long templateId){
        if ( templateId != null) {
            _templateDao.remove(templateId);
        }
    }


    @Override
    public VMTemplateVO createPrivateTemplateRecord(CreateTemplateCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Long userId = UserContext.current().getUserId();
        if (userId == null) {
            userId = User.UID_SYSTEM;
        }

        Account account = UserContext.current().getAccount();
        boolean isAdmin = ((account == null) || isAdmin(account.getType()));
        
    	VMTemplateVO privateTemplate = null;

    	UserVO user = _userDao.findById(userId);
    	
    	if (user == null) {
    		throw new InvalidParameterValueException("User " + userId + " does not exist");
    	}

    	Long volumeId = cmd.getVolumeId();
    	Long snapshotId = cmd.getSnapshotId();
    	if (volumeId == null) {
    	    if (snapshotId == null) {
                throw new InvalidParameterValueException("Failed to create private template record, neither volume ID nor snapshot ID were specified.");
    	    }
    	    SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
    	    if (snapshot == null) {
                throw new InvalidParameterValueException("Failed to create private template record, unable to find snapshot " + snapshotId);
    	    }
    	    volumeId = snapshot.getVolumeId();
    	} else {
    	    if (snapshotId != null) {
                throw new InvalidParameterValueException("Failed to create private template record, please specify only one of volume ID (" + volumeId + ") and snapshot ID (" + snapshotId + ")");
    	    }
    	}

    	VolumeVO volume = _volsDao.findById(volumeId);
    	if (volume == null) {
            throw new InvalidParameterValueException("Volume with ID: " + volumeId + " does not exist");
    	}

        if (!isAdmin) {
            if (account.getId() != volume.getAccountId()) {
                throw new PermissionDeniedException("Unable to create a template from volume with id " + volumeId + ", permission denied.");
            }
        } else if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), volume.getDomainId())) {
            throw new PermissionDeniedException("Unable to create a template from volume with id " + volumeId + ", permission denied.");
        }

        String name = cmd.getTemplateName();
        if ((name == null) || (name.length() > 32)) {
            throw new InvalidParameterValueException("Template name cannot be null and should be less than 32 characters");
        }
        
        String uniqueName = Long.valueOf((userId == null)?1:userId).toString() + Long.valueOf(volumeId).toString() + UUID.nameUUIDFromBytes(name.getBytes()).toString();

        VMTemplateVO existingTemplate = _templateDao.findByTemplateNameAccountId(name, volume.getAccountId());
        if (existingTemplate != null) {
            throw new InvalidParameterValueException("Failed to create private template " + name + ", a template with that name already exists.");
        }

        // do some parameter defaulting
    	Integer bits = cmd.getBits();
    	Boolean requiresHvm = cmd.getRequiresHvm();
    	Boolean passwordEnabled = cmd.isPasswordEnabled();
    	Boolean isPublic = cmd.isPublic();
    	Boolean featured = cmd.isFeatured();

    	HypervisorType hyperType = _volsDao.getHypervisorType(volumeId);
    	int bitsValue = ((bits == null) ? 64 : bits.intValue());
    	boolean requiresHvmValue = ((requiresHvm == null) ? true : requiresHvm.booleanValue());
    	boolean passwordEnabledValue = ((passwordEnabled == null) ? false : passwordEnabled.booleanValue());
        if (isPublic == null) {
            isPublic = Boolean.FALSE;
        }   
        
        if (!isAdmin || featured == null) {
            featured = Boolean.FALSE;
        }

        boolean allowPublicUserTemplates = Boolean.parseBoolean(_configDao.getValue("allow.public.user.templates"));        
        if (!isAdmin && !allowPublicUserTemplates && isPublic) {
            throw new PermissionDeniedException("Failed to create template " + name + ", only private templates can be created.");
        }

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

    	Long guestOSId = cmd.getOsTypeId();
    	GuestOSVO guestOS = _guestOSDao.findById(guestOSId);
    	if (guestOS == null) {
    		throw new InvalidParameterValueException("GuestOS with ID: " + guestOSId + " does not exist.");
    	}

    	Long nextTemplateId = _templateDao.getNextInSequence(Long.class, "id");
    	String description = cmd.getDisplayText();
    	VMTemplateVO template = ApiDBUtils.findTemplateById(volume.getTemplateId());    	
    	boolean isExtractable = template != null && template.isExtractable() && template.getTemplateType() != Storage.TemplateType.SYSTEM ;

        privateTemplate = new VMTemplateVO(nextTemplateId,
                                           uniqueName,
                                           name,
                                           ImageFormat.RAW,
                                           isPublic,
                                           featured,
                                           isExtractable,
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
                                           true,
                                           hyperType);        

        return _templateDao.persist(privateTemplate);
    }

    @Override @DB
    public VMTemplateVO createPrivateTemplate(CreateTemplateCmd command) throws CloudRuntimeException {
        Long userId = UserContext.current().getUserId();
        if (userId == null) {
            userId = User.UID_SYSTEM;
        }
        long templateId = command.getEntityId();
        Long volumeId = command.getVolumeId();
        Long snapshotId = command.getSnapshotId();
        SnapshotVO snapshot = null;

        // Verify input parameters
        if (snapshotId != null) {
            snapshot = _snapshotDao.findById(snapshotId);

            // Set the volumeId to that of the snapshot. All further input parameter checks will be done w.r.t the volume.
            volumeId = snapshot.getVolumeId();
        }
        
        // The volume below could be destroyed or removed.
        VolumeVO volume = _volsDao.findById(volumeId);
        String vmName = _storageMgr.getVmNameOnVolume(volume);

        // If private template is created from Volume, check that the volume will not be active when the private template is created
        if (snapshotId == null && !_storageMgr.volumeInactive(volume)) {
            String msg = "Unable to create private template for volume: " + volume.getName() + "; volume is attached to a non-stopped VM.";

            if (s_logger.isInfoEnabled()) {
                s_logger.info(msg);
            }
            throw new CloudRuntimeException(msg);
        }

        SnapshotCommand cmd = null;        
        VMTemplateVO privateTemplate = null;
    	long zoneId = volume.getDataCenterId();
    	String uniqueName = getRandomPrivateTemplateName();

    	HostVO secondaryStorageHost = _storageMgr.getSecondaryStorageHost(zoneId);
        String secondaryStorageURL = _storageMgr.getSecondaryStorageURL(zoneId);
        if (secondaryStorageHost == null || secondaryStorageURL == null) {
            throw new CloudRuntimeException("Did not find the secondary storage URL in the database for zoneId "
                    + zoneId);
        }

        if (snapshotId != null) {
            volume = _volsDao.findById(volumeId);
            StringBuilder userFolder = new StringBuilder();
            Formatter userFolderFormat = new Formatter(userFolder);
            userFolderFormat.format("u%06d", snapshot.getAccountId());

            String name = command.getTemplateName();
            String backupSnapshotUUID = snapshot.getBackupSnapshotId();
            if (backupSnapshotUUID == null) {
                throw new CloudRuntimeException("Unable to create private template from snapshot " + snapshotId + " due to there is no backupSnapshotUUID for this snapshot");
            }

            // We are creating a private template from a snapshot which has been
            // backed up to secondary storage.
            Long dcId = volume.getDataCenterId();
            Long accountId = volume.getAccountId();

            String origTemplateInstallPath = null;

            cmd = new CreatePrivateTemplateFromSnapshotCommand(_storageMgr.getPrimaryStorageNameLabel(volume),
                    secondaryStorageURL, dcId, accountId, snapshot.getVolumeId(), backupSnapshotUUID, snapshot.getName(),
                    origTemplateInstallPath, templateId, name);
        } else if (volumeId != null) {
            volume = _volsDao.findById(volumeId);
            if( volume == null ) {
                throw new CloudRuntimeException("Unable to find volume for Id " + volumeId);
            }
            if( volume.getPoolId() == null ) {
                _templateDao.remove(templateId);
                throw new CloudRuntimeException("Volume " + volumeId + " is empty, can't create template on it");
            }
            Long instanceId = volume.getInstanceId();
            if (instanceId != null){
            	VMInstanceVO vm = _vmDao.findById(instanceId);
            	State vmState = vm.getState();
            	if( !vmState.equals(State.Stopped) && !vmState.equals(State.Destroyed)) {
            		throw new CloudRuntimeException("Please put VM " + vm.getName() + " into Stopped state first");
            	}
            }           
            cmd = new CreatePrivateTemplateFromVolumeCommand(secondaryStorageURL, templateId, volume.getAccountId(),
                    command.getTemplateName(), uniqueName, volume.getPath(), vmName);

        } else {
            throw new CloudRuntimeException("Creating private Template need to specify snapshotId or volumeId");
        }
        // FIXME: before sending the command, check if there's enough capacity
        // on the storage server to create the template

        // This can be sent to a KVM host too.
        CreatePrivateTemplateAnswer answer = (CreatePrivateTemplateAnswer) _storageMgr.sendToHostsOnStoragePool(volume
                .getPoolId(), cmd, null);

        if ((answer != null) && answer.getResult()) {
            privateTemplate = _templateDao.findById(templateId);
            Long origTemplateId = volume.getTemplateId();
            VMTemplateVO origTemplate = null;
            if (origTemplateId != null) {
                origTemplate = _templateDao.findById(origTemplateId);
            }

            if ((origTemplate != null) && !Storage.ImageFormat.ISO.equals(origTemplate.getFormat())) {
                privateTemplate.setRequiresHvm(origTemplate.requiresHvm());
                privateTemplate.setBits(origTemplate.getBits());
            } else {
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
            } else {
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
            templateHostVO.setPhysicalSize(answer.getphysicalSize());
            _templateHostDao.persist(templateHostVO);

            // Increment the number of templates
            _accountMgr.incrementResourceCount(volume.getAccountId(), ResourceType.template);

        } else {

            // Remove the template record
            _templateDao.remove(templateId);
            throw new CloudRuntimeException("Creating private Template failed due to " + answer.getDetails());
        }

        return privateTemplate;
    }

    //used for vm transitioning to error state
	private void updateVmStateForFailedVmCreation(Long vmId) {
		UserVmVO vm = _vmDao.findById(vmId);
		if(vm != null){
			if(vm.getState().equals(State.Stopped)){
				_itMgr.stateTransitTo(vm, VirtualMachine.Event.OperationFailed, null);
			}
		}
	}
    
	protected class ExpungeTask implements Runnable {
    	UserVmManagerImpl _vmMgr;
    	public ExpungeTask(UserVmManagerImpl vmMgr) {
    		_vmMgr = vmMgr;
    	}

		@Override
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

	private static boolean isAdmin(short accountType) {
	    return ((accountType == Account.ACCOUNT_TYPE_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
	}
	
    @Override
    public UserVm updateVirtualMachine(UpdateVMCmd cmd) {
        String displayName = cmd.getDisplayName();
        String group = cmd.getGroup();
        Boolean ha = cmd.getHaEnable();
        Long id = cmd.getId();
        Account account = UserContext.current().getAccount();
        Long userId = UserContext.current().getUserId();
    
        //Input validation
        UserVmVO vmInstance = null;

        // Verify input parameters
        try  {
        	vmInstance = _vmDao.findById(id.longValue());
        } catch (Exception ex1) {
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "unable to find virtual machine by id");
        }

        if (vmInstance == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find virtual machine with id " + id);
        }

        userId = accountAndUserValidation(id, account, userId,vmInstance);  
        
    	if (displayName == null) {
    		displayName = vmInstance.getDisplayName();
    	}

    	if (ha == null) {
    		ha = vmInstance.isHaEnabled();
    	}

    	long accountId = vmInstance.getAccountId();
        UserVmVO vm = _vmDao.findById(id);
        if (vm == null) {
            throw new CloudRuntimeException("Unable to find virual machine with id " + id);
        }

        String description = "";
        
        if(displayName != vmInstance.getDisplayName()){
            description += "New display name: "+displayName+". ";
        }
        
        if(ha != vmInstance.isHaEnabled()){
            if(ha){
                description += "Enabled HA. ";
            } else {
                description += "Disabled HA. ";
            }
        }

        
        if (group != null) {
            if(addInstanceToGroup(id, group)){
                description += "Added to group: "+group+".";
            }
        }

        _vmDao.updateVM(id, displayName, ha);

         // create a event for the change in HA Enabled flag
        EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_INFO, EventTypes.EVENT_VM_UPDATE, "Successfully updated virtual machine: "+vm.getName()+". "+description);
        return _vmDao.findById(id);
    }

	@Override
	public UserVm stopVirtualMachine(StopVMCmd cmd) throws ServerApiException, ConcurrentOperationException{
	    return stopVirtualMachine(cmd.getId());
	}

	@Override
	public UserVm startVirtualMachine(StartVMCmd cmd) throws ExecutionException, ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
	    return startVirtualMachine(cmd.getId());
	}

	@Override
	public UserVm rebootVirtualMachine(RebootVMCmd cmd) {
        Account account = UserContext.current().getAccount();
        Long userId = UserContext.current().getUserId();
        Long vmId = cmd.getId();
        
        //Verify input parameters
        UserVmVO vmInstance = _vmDao.findById(vmId.longValue());
        if (vmInstance == null) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find a virtual machine with id " + vmId);
        }

        userId = accountAndUserValidation(vmId, account, userId, vmInstance);
        
        boolean status = rebootVirtualMachine(userId, vmId);
        
        if (status) {
        	return _vmDao.findById(vmId);
        } else {
        	throw new CloudRuntimeException("Failed to reboot vm with id: " + vmId);
        }
	}

	@Override
	public UserVm destroyVm(DestroyVMCmd cmd) throws ResourceUnavailableException, ConcurrentOperationException {
	    return destroyVm(cmd.getId());
	}

    @Override @DB
    public InstanceGroupVO createVmGroup(CreateVMGroupCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Account account = UserContext.current().getAccount();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;
        String groupName = cmd.getGroupName();
        
        if (account == null) {
            account = _accountDao.findById(1L);
        }

        if (account != null) {
            if (isAdmin(account.getType())) {
                if ((domainId != null) && (accountName != null)) {
                    if (!_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                        throw new PermissionDeniedException("Unable to create vm group in domain " + domainId + ", permission denied.");
                    }

                    Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                    if (userAccount != null) {
                        accountId = userAccount.getId();
                    } else {
                        throw new InvalidParameterValueException("Failed to create vm group " + groupName + ", unable to find account " + accountName + " in domain " + domainId);
                    }
                } else {
                    // the admin must be creating the vm group
                    accountId = account.getId();
                }
            } else {
                accountId = account.getId();
            }
        }

        if (accountId == null) {
            throw new InvalidParameterValueException("Failed to create vm group " + groupName + ", unable to find account for which to create a group.");
        }

        //Check if name is already in use by this account
        boolean isNameInUse = _vmGroupDao.isNameInUse(accountId, groupName);

        if (isNameInUse) {
            throw new InvalidParameterValueException("Unable to create vm group, a group with name " + groupName + " already exisits for account " + accountId);
        }

        return createVmGroup(groupName, accountId);
    }

    @DB
	private InstanceGroupVO createVmGroup(String groupName, long accountId) {
        Account account = null;
	    final Transaction txn = Transaction.currentTxn();
		txn.start();
		try {
			account = _accountDao.acquireInLockTable(accountId); //to ensure duplicate vm group names are not created.
			if (account == null) {
				s_logger.warn("Failed to acquire lock on account");
				return null;
			}
			InstanceGroupVO group = _vmGroupDao.findByAccountAndName(accountId, groupName);
			if (group == null){
				group = new InstanceGroupVO(groupName, accountId);
				group =  _vmGroupDao.persist(group);
			}
			return group;
		} finally {
			if (account != null) {
				_accountDao.releaseFromLockTable(accountId);
			}
			txn.commit();
		}
    }

    @Override
    public boolean deleteVmGroup(DeleteVMGroupCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Account account = UserContext.current().getAccount();
        Long groupId = cmd.getId();

        // Verify input parameters
        InstanceGroupVO group = _vmGroupDao.findById(groupId);
        if ((group == null) || (group.getRemoved() != null)) {
            throw new InvalidParameterValueException("unable to find a vm group with id " + groupId);
        }

        if (account != null) {
            Account tempAccount = _accountDao.findById(group.getAccountId());
            if (!isAdmin(account.getType()) && (account.getId() != group.getAccountId())) {
                throw new PermissionDeniedException("unable to find a group with id " + groupId);
            } else if (!_domainDao.isChildDomain(account.getDomainId(), tempAccount.getDomainId())) {
                throw new PermissionDeniedException("Invalid group id (" + groupId + ") given, unable to update the group.");
            }
        }

        return deleteVmGroup(groupId);
    }

    @Override
    public boolean deleteVmGroup(long groupId) {    	
    	//delete all the mappings from group_vm_map table
        List<InstanceGroupVMMapVO> groupVmMaps = _groupVMMapDao.listByGroupId(groupId);
        for (InstanceGroupVMMapVO groupMap : groupVmMaps) {
	        SearchCriteria<InstanceGroupVMMapVO> sc = _groupVMMapDao.createSearchCriteria();
	        sc.addAnd("instanceId", SearchCriteria.Op.EQ, groupMap.getInstanceId());
	        _groupVMMapDao.expunge(sc);
        }
    	
    	if (_vmGroupDao.remove(groupId)) {
    		return true;
    	} else {
    		return false;
    	}
    }

	@Override @DB
	public boolean addInstanceToGroup(long userVmId, String groupName) {		
		UserVmVO vm = _vmDao.findById(userVmId);

        InstanceGroupVO group = _vmGroupDao.findByAccountAndName(vm.getAccountId(), groupName);
    	//Create vm group if the group doesn't exist for this account
        if (group == null) {
        	group = createVmGroup(groupName, vm.getAccountId());
        }

		if (group != null) {
			final Transaction txn = Transaction.currentTxn();
			txn.start();
			UserVm userVm = _vmDao.acquireInLockTable(userVmId);
			if (userVm == null) {
				s_logger.warn("Failed to acquire lock on user vm id=" + userVmId);
			}
			try {
				//don't let the group be deleted when we are assigning vm to it.
				InstanceGroupVO ngrpLock = _vmGroupDao.lockRow(group.getId(), false);
				if (ngrpLock == null) {
					s_logger.warn("Failed to acquire lock on vm group id=" + group.getId() + " name=" + group.getName());
					txn.rollback();
					return false;
				}
				
				//Currently don't allow to assign a vm to more than one group
				if (_groupVMMapDao.listByInstanceId(userVmId) != null) {
					//Delete all mappings from group_vm_map table
			        List<InstanceGroupVMMapVO> groupVmMaps = _groupVMMapDao.listByInstanceId(userVmId);
			        for (InstanceGroupVMMapVO groupMap : groupVmMaps) {
				        SearchCriteria<InstanceGroupVMMapVO> sc = _groupVMMapDao.createSearchCriteria();
				        sc.addAnd("instanceId", SearchCriteria.Op.EQ, groupMap.getInstanceId());
				        _groupVMMapDao.expunge(sc);
			        }
				}
				InstanceGroupVMMapVO groupVmMapVO = new InstanceGroupVMMapVO(group.getId(), userVmId);
				_groupVMMapDao.persist(groupVmMapVO);
				
				txn.commit();
				return true;
			} finally {
				if (userVm != null) {
					_vmDao.releaseFromLockTable(userVmId);
				}
			}
	    }
		return false;
	}
	
	@Override
	public InstanceGroupVO getGroupForVm(long vmId) {
		//TODO - in future releases vm can be assigned to multiple groups; but currently return just one group per vm
		try {
			List<InstanceGroupVMMapVO> groupsToVmMap =  _groupVMMapDao.listByInstanceId(vmId);

            if(groupsToVmMap != null && groupsToVmMap.size() != 0){
            	InstanceGroupVO group = _vmGroupDao.findById(groupsToVmMap.get(0).getGroupId());
            	return group;
            } else {
            	return null;
            }
		}
		catch (Exception e){
			s_logger.warn("Error trying to get group for a vm: "+e);
			return null;
		}
	}
	
	@Override
	public void removeInstanceFromGroup(long vmId) {
		try {
			List<InstanceGroupVMMapVO> groupVmMaps = _groupVMMapDao.listByInstanceId(vmId);
	        for (InstanceGroupVMMapVO groupMap : groupVmMaps) {
		        SearchCriteria<InstanceGroupVMMapVO> sc = _groupVMMapDao.createSearchCriteria();
		        sc.addAnd("instanceId", SearchCriteria.Op.EQ, groupMap.getInstanceId());
		        _groupVMMapDao.expunge(sc);
	        }
		} catch (Exception e){
			s_logger.warn("Error trying to remove vm from group: "+e);
		}
	}
	
    private boolean validPassword(String password) {
        for (int i = 0; i < password.length(); i++) {
            if (password.charAt(i) == ' ') {
                return false;
            }
        }
        return true;
    }
    
	@Override @DB
    public UserVm createVirtualMachine(DeployVMCmd cmd) throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException {
        Account caller = UserContext.current().getAccount();
        
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        List<Long> networkList = cmd.getNetworkIds();
        
        Account owner = _accountDao.findActiveAccount(accountName, domainId);
        if (owner == null) {
            throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
        }
        
        _accountMgr.checkAccess(caller, owner);
        long accountId = owner.getId();

        DataCenterVO dc = _dcDao.findById(cmd.getZoneId());
        if (dc == null) {
            throw new InvalidParameterValueException("Unable to find zone: " + cmd.getZoneId());
        }
        
        if (dc.getDomainId() != null) {
            DomainVO domain = _domainDao.findById(dc.getDomainId());
            if (domain == null) {
                throw new CloudRuntimeException("Unable to find the domain " + dc.getDomainId() + " for the zone: " + dc);
            }
            _accountMgr.checkAccess(caller, domain);
            _accountMgr.checkAccess(owner, domain);
        }
        
        ServiceOfferingVO offering = _serviceOfferingDao.findById(cmd.getServiceOfferingId());
        if (offering == null || offering.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find service offering: " + cmd.getServiceOfferingId());
        }
        
        VMTemplateVO template = _templateDao.findById(cmd.getTemplateId());
        // Make sure a valid template ID was specified
        if (template == null || template.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to use template " + cmd.getTemplateId());
        }
        boolean isIso = Storage.ImageFormat.ISO == template.getFormat();
        if (isIso && !template.isBootable()) {
            throw new InvalidParameterValueException("Installing from ISO requires an ISO that is bootable: " + template.getId());
        }
        
        // If the template represents an ISO, a disk offering must be passed in, and will be used to create the root disk
        // Else, a disk offering is optional, and if present will be used to create the data disk
        Pair<DiskOfferingVO, Long> rootDiskOffering = new Pair<DiskOfferingVO, Long>(null, null);
        List<Pair<DiskOfferingVO, Long>> dataDiskOfferings = new ArrayList<Pair<DiskOfferingVO, Long>>();
        
        if (isIso) {
            if (cmd.getDiskOfferingId() == null) {
                throw new InvalidParameterValueException("Installing from ISO requires a disk offering to be specified for the root disk.");
            }
            DiskOfferingVO diskOffering = _diskOfferingDao.findById(cmd.getDiskOfferingId());
            if (diskOffering == null) {
                throw new InvalidParameterValueException("Unable to find disk offering " + cmd.getDiskOfferingId());
            }
            Long size = null;
            if (diskOffering.getDiskSize() == 0) {
                size = cmd.getSize();
                if (size == null) {
                    throw new InvalidParameterValueException("Disk offering " + diskOffering + " requires size parameter.");
                }
            }
            rootDiskOffering.first(diskOffering);
            rootDiskOffering.second(size);
        } else {
            rootDiskOffering.first(offering);
            if (cmd.getDiskOfferingId() != null) {
                DiskOfferingVO diskOffering = _diskOfferingDao.findById(cmd.getDiskOfferingId());
                if (diskOffering == null) {
                    throw new InvalidParameterValueException("Unable to find disk offering " + cmd.getDiskOfferingId());
                }
                Long size = null;
                if (diskOffering.getDiskSize() == 0) {
                    size = cmd.getSize();
                    if (size == null) {
                        throw new InvalidParameterValueException("Disk offering " + diskOffering + " requires size parameter.");
                    }
                }
                dataDiskOfferings.add(new Pair<DiskOfferingVO, Long>(diskOffering, size));
            }
        }

        // Check that the password was passed in and is valid
        String password = PasswordGenerator.generateRandomPassword(6);
        if (!template.getEnablePassword()) {
            password = "saved_password";
        }
        if (password == null || password.equals("") || (!validPassword(password))) {
            throw new InvalidParameterValueException("A valid password for this virtual machine was not provided.");
        }

        String networkDomain = null;
        if (networkDomain == null) {
            networkDomain = "v" + Long.toHexString(owner.getId()) + _defaultNetworkDomain;
        }

        String userData = cmd.getUserData();
        byte [] decodedUserData = null;
        if (userData != null) {
            if (userData.length() >= 2 * MAX_USER_DATA_LENGTH_BYTES) {
                throw new InvalidParameterValueException("User data is too long");
            }
            decodedUserData = org.apache.commons.codec.binary.Base64.decodeBase64(userData.getBytes());
            if (decodedUserData.length > MAX_USER_DATA_LENGTH_BYTES){
                throw new InvalidParameterValueException("User data is too long");
            }
            if (decodedUserData.length < 1) {
                throw new InvalidParameterValueException("User data is too short");
            }
        }
        
        _accountMgr.checkAccess(caller, template);
        
        DataCenterDeployment plan = new DataCenterDeployment(dc.getId());
        
        s_logger.debug("Allocating in the DB for vm");
          
        if (dc.getNetworkType() == NetworkType.Basic && networkList == null) {
            Long singleNetworkId = null;
            SearchBuilder<NetworkVO> sb = _networkDao.createSearchBuilder();
            sb.and("broadcastDomainType", sb.entity().getId(), SearchCriteria.Op.EQ);
            sb.and("dataCenterId", sb.entity().getName(), SearchCriteria.Op.EQ);
            SearchCriteria<NetworkVO> sc = sb.create();
            sc.setParameters("broadcastDomainType", BroadcastDomainType.Native);
            sc.setParameters("dataCenterId", dc.getId());

            List<NetworkVO> networks = _networkDao.search(sc, null);
            if (networks!= null && networks.isEmpty()) {
                throw new InvalidParameterValueException("Unable to find a network to start a vm");
            } else {
            	networkList = new ArrayList<Long>();
                singleNetworkId = networks.get(0).getId();
                networkList.add(singleNetworkId);
            }
        }
        
        
        if (networkList == null || networkList.isEmpty()) {
            throw new InvalidParameterValueException("NetworkIds have to be specified");
        }
        
        List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>();
        for (Long networkId : networkList) {
            NetworkVO network = _networkDao.findById(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Unable to find network by id " + networkId);
            } else {
                if (!network.isShared()) {
                    //Check account permissions
                    List<NetworkVO> networkMap = _networkDao.listBy(accountId, networkId);
                    if (networkMap == null || networkMap.isEmpty()) {
                        throw new PermissionDeniedException("Unable to create a vm using network with id " + networkId + ", permission denied");
                    }
                } 
                networks.add(new Pair<NetworkVO, NicProfile>(network, null));
            }
        }
        
        long id = _vmDao.getNextInSequence(Long.class, "id");
        
        String hostName = cmd.getName();
        String instanceName = VirtualMachineName.getVmName(id, owner.getId(), _instance);
        if (hostName == null) {
            hostName = instanceName;
        } else {
            hostName = hostName.toLowerCase();
            //verify hostName
            UserVm vm = _vmDao.findVmByZoneIdAndName(dc.getId(), hostName);
            if (vm != null && !(vm.getState() == State.Expunging || vm.getState() == State.Error)) {
                throw new InvalidParameterValueException("Vm instance with name \"" + hostName + "\" already exists in zone " + dc.getId());
            } else if (!NetUtils.verifyHostName(hostName)) {
                throw new InvalidParameterValueException("Invalid name. Vm name can contain ASCII letters 'a' through 'z', the digits '0' through '9', " +
                		                                "and the hyphen ('-'), must be between 1 and 63 characters long, and can't start or end with \"-\"");
            }
        }
        
        UserVmVO vm = new UserVmVO(id, instanceName, cmd.getDisplayName(),
                                   template.getId(), template.getGuestOSId(), offering.getOfferHA(), domainId, owner.getId(), offering.getId(), userData, hostName);


        if (_itMgr.allocate(vm, template, offering, rootDiskOffering, dataDiskOfferings, networks, null, plan, cmd.getHypervisor(), owner) == null) {
            return null;
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Successfully allocated DB entry for " + vm);
        }
        UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VM_CREATE, accountId, dc.getId(), vm.getId(), vm.getName(), offering.getId(), template.getId(), null);
        _usageEventDao.persist(usageEvent);
        
        _accountMgr.incrementResourceCount(accountId, ResourceType.user_vm);
        return vm;
	}
	
	@Override
	public UserVm startVirtualMachine(DeployVMCmd cmd) throws ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException {
	    long vmId = cmd.getEntityId();
	    UserVmVO vm = _vmDao.findById(vmId);
	    
        // Check that the password was passed in and is valid
        VMTemplateVO template = _templateDao.findById(vm.getTemplateId());
        
        String password = "saved_password";
        if (template.getEnablePassword()) {
            password = generateRandomPassword();
        }

        if (password == null || password.equals("") || (!validPassword(password))) {
            throw new InvalidParameterValueException("A valid password for this virtual machine was not provided.");
        }
        vm.setPassword(password);
        
	    long userId = UserContext.current().getUserId();
	    UserVO caller = _userDao.findById(userId);
	    
	    AccountVO owner = _accountDao.findById(vm.getAccountId());
	    
	    try {
			vm = _itMgr.start(vm, null, caller, owner, cmd.getHypervisor());
		} finally {
			updateVmStateForFailedVmCreation(vm.getId());
		}
		vm.setPassword(password);
	    return vm;
	}
	
	@Override
	public boolean finalizeVirtualMachineProfile(VirtualMachineProfile<UserVmVO> profile, DeployDestination dest, ReservationContext context) {
		UserVmVO vo = profile.getVirtualMachine();
		VirtualMachineTemplate template = profile.getTemplate();
		if (vo.getIsoId() != null) {
			template = _templateDao.findById(vo.getIsoId());
		}
		if (template != null && template.getFormat() == ImageFormat.ISO  && template.isBootable()) {
			String isoPath = null;
			Pair<String, String> isoPathPair = _storageMgr.getAbsoluteIsoPath(template.getId(), vo.getDataCenterId()); 	
			if (isoPathPair == null) {
				s_logger.warn("Couldn't get absolute iso path");
				return false;
			} else {
				isoPath = isoPathPair.first();
			}
			profile.setBootLoaderType(BootloaderType.CD);
			VolumeTO iso = new VolumeTO(profile.getId(), Volume.VolumeType.ISO, StorageResourceType.STORAGE_POOL, StoragePoolType.ISO, null, template.getName(), null, isoPath,
										0, null);
			iso.setDeviceId(3);
			profile.addDisk(iso);
			vo.setIsoId(template.getId());
		} else {
			/*create a iso placeholder*/
			VolumeTO iso = new VolumeTO(profile.getId(), Volume.VolumeType.ISO, StorageResourceType.STORAGE_POOL, StoragePoolType.ISO, null, template.getName(), null, null,
					0, null);
			iso.setDeviceId(3);
			profile.addDisk(iso);
		}
		
		return true;
	}
	
	@Override
	public boolean finalizeDeployment(Commands cmds, VirtualMachineProfile<UserVmVO> profile, DeployDestination dest, ReservationContext context) {
		UserVmVO userVm = profile.getVirtualMachine();
		List<NicVO> nics = _nicDao.listBy(userVm.getId());
		for (NicVO nic : nics) {
			NetworkVO network = _networkDao.findById(nic.getNetworkId());
			if (network.getTrafficType() == TrafficType.Guest) {
				userVm.setPrivateIpAddress(nic.getIp4Address());
				userVm.setPrivateNetmask(nic.getNetmask());
				userVm.setPrivateMacAddress(nic.getMacAddress());
			}
		}
		_vmDao.update(userVm.getId(), userVm);
	
		return true;
	}

    @Override
    public boolean finalizeStart(Commands cmds, VirtualMachineProfile<UserVmVO> profile, DeployDestination dest, ReservationContext context) {
        return true;
    }
    
    @Override
    public UserVmVO persist(UserVmVO vm) {
        return _vmDao.persist(vm);
    }
    
    @Override
    public UserVmVO findById(long id) {
        return _vmDao.findById(id);
    }
    
    @Override
    public UserVmVO findByName(String name) {
        if (!VirtualMachineName.isValidVmName(name)) {
            return null;
        }
        return findById(VirtualMachineName.getVmId(name));
    }

    @Override
    public UserVm stopVirtualMachine(long vmId) throws ConcurrentOperationException {
        
        //Input validation
        Account caller = UserContext.current().getAccount();
        Long userId = UserContext.current().getUserId();
        
        //if account is removed, return error
        if (caller != null && caller.getRemoved() != null) {
            throw new PermissionDeniedException("The account " + caller.getId()+" is removed");
        }
                
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
        }
        
        userId = accountAndUserValidation(vmId, caller, userId, vm);
        UserVO user = _userDao.findById(userId);

        try {
            _itMgr.stop(vm, user, caller);
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("Unable to contact the agent to stop the virtual machine " + vm, e);
        } 
        
        return _vmDao.findById(vmId);
    }
    
    @Override
    public void finalizeStop(VirtualMachineProfile<UserVmVO> profile, long hostId, String reservationId) {
    }
    
    public String generateRandomPassword() {
        return PasswordGenerator.generateRandomPassword(6);
    }

    
    @Override
    public UserVm startVirtualMachine(long vmId) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        //Input validation
        Account account = UserContext.current().getAccount();
        Long userId = UserContext.current().getUserId();
        
        //if account is removed, return error
        if(account!=null && account.getRemoved() != null) {
            throw new PermissionDeniedException("The account " + account.getId()+" is removed");
        }
                
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find a virtual machine with id " + vmId);
        }

        userId = accountAndUserValidation(vmId, account, userId, vm);
        UserVO user = _userDao.findById(userId);
        VolumeVO disk = _volsDao.findByInstance(vmId).get(0);
        HypervisorType hyperType = _volsDao.getHypervisorType(disk.getId());
        return _itMgr.start(vm, null, user, account, hyperType);
    }
    
    @Override
    public UserVm destroyVm(long vmId) throws ResourceUnavailableException, ConcurrentOperationException {
        Account account = UserContext.current().getAccount();
        Long userId = UserContext.current().getUserId();
        
        //Verify input parameters
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find a virtual machine with id " + vmId);
        }

        userId = accountAndUserValidation(vmId, account, userId, vm);
        User caller = _userDao.findById(userId);
        
        boolean status;
        status = _itMgr.destroy(vm, caller, account);
        
        if (status) {
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VM_DESTROY, vm.getAccountId(), vm.getDataCenterId(), vm.getId(), vm.getName(), vm.getServiceOfferingId(), vm.getTemplateId(), null);
            _usageEventDao.persist(usageEvent);
            _accountMgr.decrementResourceCount(vm.getAccountId(), ResourceType.user_vm);
            return _vmDao.findById(vmId);
        } else {
            throw new CloudRuntimeException("Failed to destroy vm with id " + vmId);
        }
    }
    
//  @Override
//  public OperationResponse executeRebootVM(RebootVMExecutor executor, VMOperationParam param) {
//    
//      final UserVmVO vm = _vmDao.findById(param.getVmId());
//      String resultDescription;
//      
//      if (vm == null || vm.getState() == State.Destroyed || vm.getState() == State.Expunging || vm.getRemoved() != null) {
//        resultDescription = "VM does not exist or in destroying state";
//        executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
//            AsyncJobResult.STATUS_FAILED, 0, resultDescription);
//        if(s_logger.isDebugEnabled())
//            s_logger.debug("Execute asynchronize Reboot VM command: " +resultDescription);
//        return new OperationResponse(OperationResponse.STATUS_FAILED, resultDescription);
//      }
//      
//      if (vm.getState() == State.Running && vm.getHostId() != null) {
//          RebootCommand cmd = new RebootCommand(vm.getInstanceName());
//          try {
//            long seq = _agentMgr.send(vm.getHostId(), new Commands(cmd), new VMOperationListener(executor, param, vm, 0));
//            resultDescription = "Execute asynchronize Reboot VM command: sending command to agent, seq - " + seq;
//            if(s_logger.isDebugEnabled())
//                s_logger.debug(resultDescription);
//            return new OperationResponse(OperationResponse.STATUS_IN_PROGRESS, resultDescription);
//        } catch (AgentUnavailableException e) {
//            resultDescription = "Agent is not available";
//            executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
//                AsyncJobResult.STATUS_FAILED, 0, resultDescription);
//            return new OperationResponse(OperationResponse.STATUS_FAILED, resultDescription);
//        }
//      }
//      resultDescription = "VM is not running or agent host is disconnected";
//    executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
//        AsyncJobResult.STATUS_FAILED, 0, resultDescription);
//    return new OperationResponse(OperationResponse.STATUS_FAILED, resultDescription);
//  }
    
//  @Override @DB
//  public OperationResponse executeDestroyVM(DestroyVMExecutor executor, VMOperationParam param) {
//      UserVmVO vm = _vmDao.findById(param.getVmId());
//      State state = vm.getState();
//      OperationResponse response; 
//      String resultDescription = "Success";               
//      
//      if (vm == null || state == State.Destroyed || state == State.Expunging || vm.getRemoved() != null) {
//          if (s_logger.isDebugEnabled()) {
//              s_logger.debug("Unable to find vm or vm is destroyed: " + param.getVmId());
//          }
//          resultDescription = "VM does not exist or already in destroyed state";
//          response = new OperationResponse(OperationResponse.STATUS_FAILED, resultDescription);
//        executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
//            AsyncJobResult.STATUS_FAILED, 0, resultDescription);
//        return response;
//      }
//      
//      if(state == State.Stopping) {
//          if (s_logger.isDebugEnabled()) {
//              s_logger.debug("VM is being stopped: " + param.getVmId());
//          }
//          resultDescription = "VM is being stopped, please re-try later";
//          response = new OperationResponse(OperationResponse.STATUS_FAILED, resultDescription);
//        executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
//            AsyncJobResult.STATUS_FAILED, 0, resultDescription);
//        return response;
//      }
//
//      if (state == State.Running) {
//          if (vm.getHostId() == null) {
//            resultDescription = "VM host is null (invalid VM)";
//            response = new OperationResponse(OperationResponse.STATUS_FAILED, resultDescription);
//            executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
//                    AsyncJobResult.STATUS_FAILED, 0, resultDescription);
//            if(s_logger.isDebugEnabled())
//                s_logger.debug("Execute asynchronize destroy VM command: " + resultDescription);
//              return response;
//          }
//        
//          if (!_vmDao.updateIf(vm, Event.StopRequested, vm.getHostId())) {
//            resultDescription = "Failed to issue stop command, please re-try later";
//            response = new OperationResponse(OperationResponse.STATUS_FAILED, resultDescription);
//            executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
//                    AsyncJobResult.STATUS_FAILED, 0, resultDescription);                        
//            if(s_logger.isDebugEnabled())
//                s_logger.debug("Execute asynchronize destroy VM command:" + resultDescription);             
//              return response;
//          }
//          long childEventId = EventUtils.saveStartedEvent(param.getUserId(), param.getAccountId(),
//                EventTypes.EVENT_VM_STOP, "stopping vm " + vm.getName(), 0);
//          param.setChildEventId(childEventId);
//          StopCommand cmd = new StopCommand(vm, vm.getInstanceName(), vm.getVnet());
//          try {
//            long seq = _agentMgr.send(vm.getHostId(), new Command[] {cmd}, true,
//                new VMOperationListener(executor, param, vm, 0));
//            resultDescription = "Execute asynchronize destroy VM command: sending stop command to agent, seq - " + seq;
//            if(s_logger.isDebugEnabled())
//                s_logger.debug(resultDescription);
//            response = new OperationResponse(OperationResponse.STATUS_IN_PROGRESS, resultDescription);
//            return response;
//        } catch (AgentUnavailableException e) {
//            resultDescription = "Agent is not available";
//            response = new OperationResponse(OperationResponse.STATUS_FAILED, resultDescription);
//            executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
//                AsyncJobResult.STATUS_FAILED, 0, resultDescription);
//            return response;
//        }
//      }
//      
//      Transaction txn = Transaction.currentTxn();
//      txn.start();        
//      
//      _accountMgr.decrementResourceCount(vm.getAccountId(), ResourceType.user_vm);
//      if (!_vmDao.updateIf(vm, VirtualMachine.Event.DestroyRequested, vm.getHostId()) ) {
//        resultDescription = "Unable to destroy the vm because it is not in the correct state";
//          s_logger.debug(resultDescription + vm.toString());
//          
//          txn.rollback();
//          response = new OperationResponse(OperationResponse.STATUS_FAILED, resultDescription);
//        executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
//                AsyncJobResult.STATUS_FAILED, 0, resultDescription);
//          return response;
//      }
//
//      // Now that the VM is destroyed, clean the network rules associated with it.
//      cleanNetworkRules(param.getUserId(), vm.getId());
//
//      // Mark the VM's root disk as destroyed
//      List<VolumeVO> volumes = _volsDao.findByInstanceAndType(vm.getId(), VolumeType.ROOT);
//      for (VolumeVO volume : volumes) {
//        _storageMgr.destroyVolume(volume);
//      }
//      
//      // Mark the VM's data disks as detached
//      volumes = _volsDao.findByInstanceAndType(vm.getId(), VolumeType.DATADISK);
//      for (VolumeVO volume : volumes) {
//        _volsDao.detachVolume(volume.getId());
//      }
//      
//      txn.commit();
//      response = new OperationResponse(OperationResponse.STATUS_SUCCEEDED, resultDescription);
//    executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
//        AsyncJobResult.STATUS_SUCCEEDED, 0, "success");
//    return response;
//  }
}
