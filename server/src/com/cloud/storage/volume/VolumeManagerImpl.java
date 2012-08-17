package com.cloud.storage.volume;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DeleteVolumeCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.api.commands.CreateVolumeCmd;
import com.cloud.api.commands.ListVolumesCmd;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeHostVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeHostDao;
import com.cloud.storage.image.ImageManager;
import com.cloud.storage.orchestra.StorageMigrationCleanupMaid;
import com.cloud.storage.pool.Storage;
import com.cloud.storage.pool.Storage.ImageFormat;
import com.cloud.storage.pool.StoragePool;
import com.cloud.storage.pool.StoragePoolManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.storage.volume.Volume.Event;
import com.cloud.storage.volume.Volume.Type;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.event.ActionEvent;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate;

public class VolumeManagerImpl implements VolumeManager, Manager {
    private static final Logger s_logger = Logger.getLogger(VolumeManagerImpl.class);
    protected int _retry = 2;
    private StateMachine2<Volume.State, Volume.Event, Volume> _volStateMachine;
    protected SearchBuilder<VMTemplateHostVO> HostTemplateStatesSearch;
    @Inject
    protected VolumeDao _volumeDao;
    @Inject
    protected VMTemplateHostDao _vmTemplateHostDao = null;
    @Inject
    protected HostDao _hostDao;
    @Inject
	protected DiskOfferingDao _diskOfferingDao;
    @Inject
    protected StoragePoolManager _storagePoolMgr;
    @Inject
    protected SnapshotManager _snapshotMgr;
    @Inject
    protected ImageManager _imageMgr;
    @Inject
    protected VolumeHostDao _volHostDao;
    @Inject 
    protected AgentManager _agentMgr;
    @Inject
    protected VMInstanceDao _vmInstanceDao;
    @Inject
    protected ResourceTagDao _resourceTagDao;
    @Inject
    protected TemplateManager _templateMgr;
    @Inject
    protected VMInstanceDao _vmDao;
    @Inject
    protected VMTemplateDao _templateDao;
	
   

	@Override
	@DB
	public boolean destroyVolume(VolumeVO volume) throws ConcurrentOperationException {
		try {
			if (!processEvent(volume, Volume.Event.DestroyRequested)) {
				throw new ConcurrentOperationException("Failed to transit to destroyed state");
			}
		} catch (NoTransitionException e) {
			s_logger.debug("Unable to destoy the volume: " + e.toString());
			return false;
		}

		long volumeId = volume.getId();

		// Delete the recurring snapshot policies for this volume.
		_snapshotMgr.deletePoliciesForVolume(volumeId);

		Long instanceId = volume.getInstanceId();
		VMInstanceVO vmInstance = null;
		if (instanceId != null) {
			vmInstance = _vmInstanceDao.findById(instanceId);
		}

		if (instanceId == null || (vmInstance.getType().equals(VirtualMachine.Type.User))) {
			// Decrement the resource count for volumes belonging user VM's only
			_resourceLimitMgr.decrementResourceCount(volume.getAccountId(), ResourceType.volume);
			// Log usage event for volumes belonging user VM's only
			UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_DELETE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName());
			_usageEventDao.persist(usageEvent);
		}

		try {
			if (!processEvent(volume, Volume.Event.OperationSucceeded)) {
				throw new ConcurrentOperationException("Failed to transit state");

			}
		} catch (NoTransitionException e) {
			s_logger.debug("Unable to change volume state: " + e.toString());
			return false;
		}

		return true;

	}
	
    @Override
    public VolumeVO allocateDuplicateVolume(VolumeVO oldVol, Long templateId) {
        VolumeVO newVol = new VolumeVO(oldVol.getVolumeType(), oldVol.getName(), oldVol.getDataCenterId(), oldVol.getDomainId(), oldVol.getAccountId(), oldVol.getDiskOfferingId(), oldVol.getSize());
        if (templateId != null) {
            newVol.setTemplateId(templateId);
        } else {
            newVol.setTemplateId(oldVol.getTemplateId());
        }
        newVol.setDeviceId(oldVol.getDeviceId());
        newVol.setInstanceId(oldVol.getInstanceId());
        newVol.setRecreatable(oldVol.isRecreatable());
        return _volumeDao.persist(newVol);
    }
    
    @Override
    @DB
    public VolumeVO createVolume(VolumeVO volume, long VMTemplateId, DiskOfferingVO diskOffering,
             HypervisorType hyperType, StoragePool assignedPool) {
         Long existingPoolId = null;
    	 existingPoolId = volume.getPoolId();
         
    	 if (existingPoolId == null && assignedPool == null) {
    		 throw new StorageUnavailableException("Volume has no pool associate and also no storage pool assigned in DeployDestination, Unable to create " + volume, Volume.class, volume.getId());
    	 }
    	 
    	 if (assignedPool == null) {
    		 if (s_logger.isDebugEnabled()) {
                 s_logger.debug("No need to recreate the volume: " + volume + ", since it already has a pool assigned: " + volume.getPoolId() + ", adding disk to VM");
             }
             StoragePoolVO pool = (StoragePoolVO)_storagePoolMgr.getStoragePoolById(volume.getPoolId());
             return volume;
    	 }
    	 
    	 boolean needToCreateVolume = false;
    	 boolean needToRecreateVolume = false;
    	 boolean needToMigrateVolume = false;
    	 Volume.State state = volume.getState();
    	 if (state == Volume.State.Allocated || state == Volume.State.Creating) {
        	 needToCreateVolume = true;
         }

    	 if (volume.isRecreatable()) {
    		 if (s_logger.isDebugEnabled()) {
    			 s_logger.debug("Volume " + volume + " will be recreated on storage pool " + assignedPool + " assigned by deploymentPlanner");
    		 }
    		 if (volume.getPoolId() == null) {
    			 needToCreateVolume = true;
    		 } else {
    			 needToRecreateVolume = true;
    		 }
    	 } else {
    		 if (assignedPool.getId() != volume.getPoolId()) {
    			 if (s_logger.isDebugEnabled()) {
    				 s_logger.debug("Mismatch in storage pool " + assignedPool + " assigned by deploymentPlanner and the one associated with volume " + volume);
    			 }
    			 if (diskOffering.getUseLocalStorage()) {
    				 throw new StorageUnavailableException("Can't recreate volume: " + volume + " on local storage", Volume.class, volume.getId());
    			 } else {
    				 if (s_logger.isDebugEnabled()) {
    	        		 s_logger.debug("Shared volume " + volume + " will be migrated on storage pool " + assignedPool + " assigned by deploymentPlanner");
    	        	 }
    				needToMigrateVolume = true;
    			 }
    		 } else {
    			 return volume;
    		 }
    	 }
    	 
    	 VolumeVO newVol = volume;
    	 if (needToCreateVolume || needToRecreateVolume) {
    		 if (needToRecreateVolume) {
    			 newVol = switchVolume(volume, VMTemplateId);
    		 }
    		 newVol = createVolume(newVol, diskOffering, hyperType, assignedPool);
    	 } else if (needToMigrateVolume) {
    		 try {
        		 List<Volume> volumesToMigrate = new ArrayList<Volume>();
        		 volumesToMigrate.add(volume);
        		 migrateVolumes(volumesToMigrate, assignedPool);
        		 newVol = _volumeDao.findById(volume.getId());
        	 } catch (ConcurrentOperationException e) {
        		 throw new CloudRuntimeException("Migration of volume " + volume + " to storage pool " + assignedPool + " failed", e);
        	 }
         }
    	 
    	 return newVol;
    }
    
    /**
     * Give a volume in allocated state, created on specified storage
     * @param toBeCreated
     * @param offering
     * @param hypervisorType
     * @param sPool
     * @return
     * @throws StorageUnavailableException
     */
    private VolumeVO createVolume(VolumeVO toBeCreated, DiskOfferingVO offering, HypervisorType hypervisorType,
            StoragePool sPool) throws StorageUnavailableException {
        if (sPool == null) {
        	throw new CloudRuntimeException("can't create volume: " + toBeCreated + " on a empty storage");
        }
        
        if (toBeCreated == null) {
        	throw new CloudRuntimeException("volume can't be null");
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating volume: " + toBeCreated + " on pool: " + sPool);
        }
         
        DiskProfile diskProfile = new DiskProfile(toBeCreated, offering, hypervisorType);
        
        toBeCreated.setPoolId(sPool.getId());
        try {
        	processEvent(toBeCreated, Volume.Event.CreateRequested);
        } catch (NoTransitionException e) {
        	throw new CloudRuntimeException("Unable to request a create operation on volume " + toBeCreated);
        }

        boolean createdSuccessed = false;
        try {
        	VMTemplateVO template = null;
        	String templateOnStoragePath = null;
        	if (toBeCreated.getTemplateId() != null) {
        		template = _templateDao.findById(toBeCreated.getTemplateId());
        		templateOnStoragePath = _templateMgr.prepareTemplateOnPool(template, sPool);
        	}
        	
        	CreateCommand cmd = null;
        	cmd = new CreateCommand(diskProfile, templateOnStoragePath, new StorageFilerTO(sPool));

        	Answer answer = _storagePoolMgr.sendToPool(sPool, null, cmd);
        	if (answer.getResult()) {
        		CreateAnswer createAnswer = (CreateAnswer) answer;
        		VolumeTO created = createAnswer.getVolume();
        		
        		toBeCreated.setFolder(sPool.getPath());
        		toBeCreated.setPath(created.getPath());
        		toBeCreated.setSize(created.getSize());
        		toBeCreated.setPoolType(sPool.getPoolType());
        		toBeCreated.setPoolId(sPool.getId());
        		toBeCreated.setPodId(sPool.getPodId());
        		
        		try {
        			processEvent(toBeCreated, Volume.Event.OperationSucceeded);
        			createdSuccessed = true;
        		} catch (NoTransitionException e) {
        			s_logger.debug("Unable to update volume state: " + e.toString());
        			return null;
        		}
        	}
        	return toBeCreated;
        } finally {
        	if (createdSuccessed == false) {
        		try {
        			processEvent(toBeCreated, Volume.Event.OperationFailed);
        		} catch (NoTransitionException e1) {
        			s_logger.debug("Unable to update volume state: " + e1.toString());
        		}
        	}
        }
    }
    
    @DB
    private VolumeVO createVolume1(VolumeVO volume, VirtualMachineProfile<? extends VirtualMachine> vm, VMTemplateVO template, DataCenterVO dc, HostPodVO pod, Long clusterId, ServiceOfferingVO offering, DiskOfferingVO diskOffering,
            List<StoragePoolVO> avoids, long size, HypervisorType hyperType) {
        StoragePoolVO pool = null;
        final HashSet<StoragePool> avoidPools = new HashSet<StoragePool>(avoids);

        try {
            processEvent(volume, Volume.Event.CreateRequested);
        } catch (NoTransitionException e) {
            s_logger.debug("Unable to update volume state: " + e.toString());
            return null;
        }

        if (diskOffering != null && diskOffering.isCustomized()) {
            diskOffering.setDiskSize(size);
        }
        DiskProfile dskCh = null;
        if (volume.getVolumeType() == Type.ROOT && Storage.ImageFormat.ISO != template.getFormat()) {
            dskCh = createDiskCharacteristics(volume, template, dc, offering);
        } else {
            dskCh = createDiskCharacteristics(volume, template, dc, diskOffering);
        }

        dskCh.setHyperType(hyperType);

        VolumeTO created = null;
        int retry = _retry;
        while (--retry >= 0) {
            created = null;

            long podId = pod.getId();
            pod = _podDao.findById(podId);
            if (pod == null) {
                s_logger.warn("Unable to find pod " + podId + " when create volume " + volume.getName());
                break;
            }

            pool = findStoragePool(dskCh, dc, pod, clusterId, vm, avoidPools);
            if (pool == null) {
                s_logger.warn("Unable to find storage poll when create volume " + volume.getName());
                break;
            }

            avoidPools.add(pool);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Trying to create " + volume + " on " + pool);
            }

            CreateCommand cmd = null;
            VMTemplateStoragePoolVO tmpltStoredOn = null;

            for (int i = 0; i < 2; i++) {
                if (volume.getVolumeType() == Type.ROOT && Storage.ImageFormat.ISO != template.getFormat()) {
                    tmpltStoredOn = _tmpltMgr.prepareTemplateForCreate(template, pool);
                    if (tmpltStoredOn == null) {
                        continue;
                    }
                    cmd = new CreateCommand(dskCh, tmpltStoredOn.getLocalDownloadPath(), new StorageFilerTO(pool));
                } else {
                    if (volume.getVolumeType() == Type.ROOT && Storage.ImageFormat.ISO == template.getFormat()) {
                        VMTemplateHostVO tmpltHostOn = _tmpltMgr.prepareISOForCreate(template, pool);
                        if (tmpltHostOn == null) {
                            throw new CloudRuntimeException("Did not find ISO in secondry storage in zone " + pool.getDataCenterId());
                        }
                    }
                    cmd = new CreateCommand(dskCh, new StorageFilerTO(pool));
                }

                try {
                    Answer answer = sendToPool(pool, cmd);
                    if (answer != null && answer.getResult()) {
                        created = ((CreateAnswer) answer).getVolume();
                        break;
                    }

                    if (tmpltStoredOn != null && answer != null && (answer instanceof CreateAnswer) && ((CreateAnswer) answer).templateReloadRequested()) {
                        if (!_tmpltMgr.resetTemplateDownloadStateOnPool(tmpltStoredOn.getId())) {
                            break; // break out of template-redeploy retry loop
                        }
                    } else {
                        break;
                    }
                } catch (StorageUnavailableException e) {
                    s_logger.debug("Storage unavailable for " + pool.getId());
                    break; // break out of template-redeploy retry loop
                }
            }

            if (created != null) {
                break;
            }

            s_logger.debug("Retrying the create because it failed on pool " + pool);
        }

        if (created == null) {
            return null;
        } else {
            volume.setFolder(pool.getPath());
            volume.setPath(created.getPath());
            volume.setSize(created.getSize());
            volume.setPoolType(pool.getPoolType());
            volume.setPoolId(pool.getId());
            volume.setPodId(pod.getId());
            try {
                processEvent(volume, Volume.Event.OperationSucceeded);
            } catch (NoTransitionException e) {
                s_logger.debug("Unable to update volume state: " + e.toString());
                return null;
            }
            return volume;
        }
    }
    
    @DB
    public boolean migrateVolumes(List<Volume> volumes, StoragePool destPool) throws ConcurrentOperationException {
        Transaction txn = Transaction.currentTxn();
        txn.start();

        boolean transitResult = false;
        long checkPointTaskId = -1;
        try {
            List<Long> volIds = new ArrayList<Long>();
            for (Volume volume : volumes) {
                if (!_snapshotMgr.canOperateOnVolume((VolumeVO) volume)) {
                    throw new CloudRuntimeException("There are snapshots creating on this volume, can not move this volume");
                }

                try {
                    if (!processEvent(volume, Volume.Event.MigrationRequested)) {
                        throw new ConcurrentOperationException("Failed to transit volume state");
                    }
                } catch (NoTransitionException e) {
                    s_logger.debug("Failed to set state into migrate: " + e.toString());
                    throw new CloudRuntimeException("Failed to set state into migrate: " + e.toString());
                }
                volIds.add(volume.getId());
            }

            checkPointTaskId = _checkPointMgr.pushCheckPoint(new StorageMigrationCleanupMaid(StorageMigrationCleanupMaid.StorageMigrationState.MIGRATING, volIds));
            transitResult = true;
        } finally {
            if (!transitResult) {
                txn.rollback();
            } else {
                txn.commit();
            }
        }

        // At this stage, nobody can modify volumes. Send the copyvolume command
        List<Pair<StoragePoolVO, DestroyCommand>> destroyCmds = new ArrayList<Pair<StoragePoolVO, DestroyCommand>>();
        List<CopyVolumeAnswer> answers = new ArrayList<CopyVolumeAnswer>();
        try {
            for (Volume volume : volumes) {
                String secondaryStorageURL = getSecondaryStorageURL(volume.getDataCenterId());
                StoragePoolVO srcPool = _storagePoolDao.findById(volume.getPoolId());
                CopyVolumeCommand cvCmd = new CopyVolumeCommand(volume.getId(), volume.getPath(), srcPool, secondaryStorageURL, true, _copyvolumewait);
                CopyVolumeAnswer cvAnswer;
                try {
                    cvAnswer = (CopyVolumeAnswer) sendToPool(srcPool, cvCmd);
                } catch (StorageUnavailableException e1) {
                    throw new CloudRuntimeException("Failed to copy the volume from the source primary storage pool to secondary storage.", e1);
                }

                if (cvAnswer == null || !cvAnswer.getResult()) {
                    throw new CloudRuntimeException("Failed to copy the volume from the source primary storage pool to secondary storage.");
                }

                String secondaryStorageVolumePath = cvAnswer.getVolumePath();

                // Copy the volume from secondary storage to the destination storage
                // pool
                cvCmd = new CopyVolumeCommand(volume.getId(), secondaryStorageVolumePath, destPool, secondaryStorageURL, false, _copyvolumewait);
                try {
                    cvAnswer = (CopyVolumeAnswer) sendToPool(destPool, cvCmd);
                } catch (StorageUnavailableException e1) {
                    throw new CloudRuntimeException("Failed to copy the volume from secondary storage to the destination primary storage pool.");
                }

                if (cvAnswer == null || !cvAnswer.getResult()) {
                    throw new CloudRuntimeException("Failed to copy the volume from secondary storage to the destination primary storage pool.");
                }

                answers.add(cvAnswer);
                destroyCmds.add(new Pair<StoragePoolVO, DestroyCommand>(srcPool, new DestroyCommand(srcPool, volume, null)));
            }
        } finally {
            if (answers.size() != volumes.size()) {
                // this means one of copying volume failed
                for (Volume volume : volumes) {
                    try {
                        processEvent(volume, Volume.Event.OperationFailed);
                    } catch (NoTransitionException e) {
                        s_logger.debug("Failed to change volume state: " + e.toString());
                    }
                }
                _checkPointMgr.popCheckPoint(checkPointTaskId);
            } else {
                // Need a transaction, make sure all the volumes get migrated to new storage pool
                txn = Transaction.currentTxn();
                txn.start();

                transitResult = false;
                try {
                    for (int i = 0; i < volumes.size(); i++) {
                        CopyVolumeAnswer answer = answers.get(i);
                        VolumeVO volume = (VolumeVO) volumes.get(i);
                        Long oldPoolId = volume.getPoolId();
                        volume.setPath(answer.getVolumePath());
                        volume.setFolder(destPool.getPath());
                        volume.setPodId(destPool.getPodId());
                        volume.setPoolId(destPool.getId());
                        volume.setLastPoolId(oldPoolId);
                        volume.setPodId(destPool.getPodId());
                        try {
                            processEvent(volume, Volume.Event.OperationSucceeded);
                        } catch (NoTransitionException e) {
                            s_logger.debug("Failed to change volume state: " + e.toString());
                            throw new CloudRuntimeException("Failed to change volume state: " + e.toString());
                        }
                    }
                    transitResult = true;
                    try {
                        _checkPointMgr.popCheckPoint(checkPointTaskId);
                    } catch (Exception e) {

                    }
                } finally {
                    if (!transitResult) {
                        txn.rollback();
                    } else {
                        txn.commit();
                    }
                }

            }
        }

        // all the volumes get migrated to new storage pool, need to delete the copy on old storage pool
        for (Pair<StoragePoolVO, DestroyCommand> cmd : destroyCmds) {
            try {
                Answer cvAnswer = _storagePoolMgr.sendToPool(cmd.first(), cmd.second());
            } catch (StorageUnavailableException e) {
                s_logger.debug("Unable to delete the old copy on storage pool: " + e.toString());
            }
        }
        return true;
    }
    
    @Override
    public boolean volumeInactive(VolumeVO volume) {
        Long vmId = volume.getInstanceId();
        if (vmId != null) {
            UserVm vm = _userVmDao.findById(vmId);
            if (vm == null) {
                return true;
            }
            State state = vm.getState();
            if (state.equals(State.Stopped) || state.equals(State.Destroyed)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String getVmNameOnVolume(VolumeVO volume) {
        Long vmId = volume.getInstanceId();
        if (vmId != null) {
            VMInstanceVO vm = _vmInstanceDao.findById(vmId);

            if (vm == null) {
                return null;
            }
            return vm.getInstanceName();
        }
        return null;
    }
    
    @Override
    public boolean volumeOnSharedStoragePool(VolumeVO volume) {
        Long poolId = volume.getPoolId();
        if (poolId == null) {
            return false;
        } else {
            StoragePoolVO pool = _storagePoolDao.findById(poolId);

            if (pool == null) {
                return false;
            } else {
                return pool.isShared();
            }
        }
    }
    
    public String getRandomVolumeName() {
        return UUID.randomUUID().toString();
    }
    
    @Override
    public VolumeVO moveVolume(VolumeVO volume, long destPoolDcId, Long destPoolPodId, Long destPoolClusterId, HypervisorType dataDiskHyperType) throws ConcurrentOperationException {

        // Find a destination storage pool with the specified criteria
        DiskOfferingVO diskOffering = _diskOfferingDao.findById(volume.getDiskOfferingId());
        DiskProfile dskCh = new DiskProfile(volume.getId(), volume.getVolumeType(), volume.getName(), diskOffering.getId(), diskOffering.getDiskSize(), diskOffering.getTagsArray(),
                diskOffering.getUseLocalStorage(), diskOffering.isRecreatable(), null);
        dskCh.setHyperType(dataDiskHyperType);
        DataCenterVO destPoolDataCenter = _dcDao.findById(destPoolDcId);
        HostPodVO destPoolPod = _podDao.findById(destPoolPodId);
        StoragePoolVO destPool = findStoragePool(dskCh, destPoolDataCenter, destPoolPod, destPoolClusterId, null, new HashSet<StoragePool>());
        String secondaryStorageURL = getSecondaryStorageURL(volume.getDataCenterId());

        if (destPool == null) {
            throw new CloudRuntimeException("Failed to find a storage pool with enough capacity to move the volume to.");
        }
        if (secondaryStorageURL == null) {
            throw new CloudRuntimeException("Failed to find secondary storage.");
        }

        List<Volume> vols = new ArrayList<Volume>();
        vols.add(volume);
        migrateVolumes(vols, destPool);
        return _volumeDao.findById(volume.getId());
    }
    
    
    public boolean validateVolume(Account caller, long ownerId, Long zoneId, String volumeName, String url, String format) throws ResourceAllocationException{

        // permission check
        _accountMgr.checkAccess(caller, null, true, _accountMgr.getActiveAccountById(ownerId));

        // Check that the resource limit for volumes won't be exceeded
        _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(ownerId), ResourceType.volume);
        

        // Verify that zone exists
        DataCenterVO zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id " + zoneId);
        }

        // Check if zone is disabled
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getType())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + zoneId);
        }
        
		if (url.toLowerCase().contains("file://")) {
			throw new InvalidParameterValueException("File:// type urls are currently unsupported");
		}
		
		ImageFormat imgfmt = ImageFormat.valueOf(format.toUpperCase());
		if (imgfmt == null) {
			throw new IllegalArgumentException("Image format is incorrect " + format + ". Supported formats are " + EnumUtils.listValues(ImageFormat.values()));
		}
		
        String userSpecifiedName = volumeName;
        if (userSpecifiedName == null) {
            userSpecifiedName = getRandomVolumeName();
        }
		if((!url.toLowerCase().endsWith("vhd"))&&(!url.toLowerCase().endsWith("vhd.zip"))
		        &&(!url.toLowerCase().endsWith("vhd.bz2"))&&(!url.toLowerCase().endsWith("vhd.gz")) 
		        &&(!url.toLowerCase().endsWith("qcow2"))&&(!url.toLowerCase().endsWith("qcow2.zip"))
		        &&(!url.toLowerCase().endsWith("qcow2.bz2"))&&(!url.toLowerCase().endsWith("qcow2.gz"))
		        &&(!url.toLowerCase().endsWith("ova"))&&(!url.toLowerCase().endsWith("ova.zip"))
		        &&(!url.toLowerCase().endsWith("ova.bz2"))&&(!url.toLowerCase().endsWith("ova.gz"))
		        &&(!url.toLowerCase().endsWith("img"))&&(!url.toLowerCase().endsWith("raw"))){
		        throw new InvalidParameterValueException("Please specify a valid " + format.toLowerCase());
		    }
			
			if ((format.equalsIgnoreCase("vhd") && (!url.toLowerCase().endsWith(".vhd") && !url.toLowerCase().endsWith("vhd.zip") && !url.toLowerCase().endsWith("vhd.bz2") && !url.toLowerCase().endsWith("vhd.gz") ))
				|| (format.equalsIgnoreCase("qcow2") && (!url.toLowerCase().endsWith(".qcow2") && !url.toLowerCase().endsWith("qcow2.zip") && !url.toLowerCase().endsWith("qcow2.bz2") && !url.toLowerCase().endsWith("qcow2.gz") ))
				|| (format.equalsIgnoreCase("ova") && (!url.toLowerCase().endsWith(".ova") && !url.toLowerCase().endsWith("ova.zip") && !url.toLowerCase().endsWith("ova.bz2") && !url.toLowerCase().endsWith("ova.gz")))
				|| (format.equalsIgnoreCase("raw") && (!url.toLowerCase().endsWith(".img") && !url.toLowerCase().endsWith("raw")))) {
		        throw new InvalidParameterValueException("Please specify a valid URL. URL:" + url + " is an invalid for the format " + format.toLowerCase());
			}
        validateUrl(url);
               
    	return false;
    }
    
    private String validateUrl(String url){
 		try {
 			URI uri = new URI(url);
 			if ((uri.getScheme() == null) || (!uri.getScheme().equalsIgnoreCase("http") 
 				&& !uri.getScheme().equalsIgnoreCase("https") && !uri.getScheme().equalsIgnoreCase("file"))) {
 				throw new IllegalArgumentException("Unsupported scheme for url: " + url);
 			}

 			int port = uri.getPort();
 			if (!(port == 80 || port == 443 || port == -1)) {
 				throw new IllegalArgumentException("Only ports 80 and 443 are allowed");
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
 			
 			return uri.toString();
 		} catch (URISyntaxException e) {
 			throw new IllegalArgumentException("Invalid URL " + url);
 		}
     	
     }
    
    
    public VolumeVO persistVolume(Account caller, long ownerId, Long zoneId, String volumeName, String url, String format) {
     	
        Transaction txn = Transaction.currentTxn();
        txn.start();

        VolumeVO volume = new VolumeVO(volumeName, zoneId, -1, -1, -1, new Long(-1), null, null, 0, Volume.Type.DATADISK);
        volume.setPoolId(null);
        volume.setDataCenterId(zoneId);
        volume.setPodId(null);
        volume.setAccountId(ownerId);
        volume.setDomainId(((caller == null) ? Domain.ROOT_DOMAIN : caller.getDomainId()));
        long diskOfferingId = _diskOfferingDao.findByUniqueName("Cloud.com-Custom").getId();
        volume.setDiskOfferingId(diskOfferingId);        
        //volume.setSize(size);
        volume.setInstanceId(null);
        volume.setUpdated(new Date());
        volume.setDomainId((caller == null) ? Domain.ROOT_DOMAIN : caller.getDomainId());

        volume = _volumeDao.persist(volume);
        try {
			processEvent(volume, Event.UploadRequested);
		} catch (NoTransitionException e) {
			s_logger.debug("Can't create persist volume, due to " + e.toString());
		}
        UserContext.current().setEventDetails("Volume Id: " + volume.getId());

        // Increment resource count during allocation; if actual creation fails, decrement it
        _resourceLimitMgr.incrementResourceCount(volume.getAccountId(), ResourceType.volume);

        txn.commit();
		return volume;
	}
   
   
    
    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_DELETE, eventDescription = "deleting volume")
    public boolean deleteVolume(long volumeId) throws ConcurrentOperationException {
        Account caller = UserContext.current().getCaller();

        // Check that the volume ID is valid
        VolumeVO volume = _volumeDao.findById(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Unable to aquire volume with ID: " + volumeId);
        }

        if (!_snapshotMgr.canOperateOnVolume(volume)) {
            throw new InvalidParameterValueException("There are snapshot creating on it, Unable to delete the volume");
        }

        // permission check
        _accountMgr.checkAccess(caller, null, true, volume);

        // Check that the volume is not currently attached to any VM
        if (volume.getInstanceId() != null) {
            throw new InvalidParameterValueException("Please specify a volume that is not attached to any VM.");
        }

        // Check that volume is completely Uploaded 
        if (volume.getState() == Volume.State.UploadOp){
        	VolumeHostVO volumeHost = _volumeHostDao.findByVolumeId(volume.getId());
            if (volumeHost.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS){
            	throw new InvalidParameterValueException("Please specify a volume that is not uploading");
            }            
        }
        
        // Check that the volume is not already destroyed
        if (volume.getState() != Volume.State.Destroy) {
            if (!destroyVolume(volume)) {
                return false;
            }
        }

        try {
            expungeVolume(volume, false);
        } catch (Exception e) {
            s_logger.warn("Failed to expunge volume:", e);
            return false;
        }

        return true;
    }

    @Override
    public VolumeVO allocateDiskVolume(String volumeName, long zoneId, long ownerId, long domainId, long diskOfferingId, long size) {
    	if (volumeName == null) {
    		volumeName = getRandomVolumeName();
    	}

    	VolumeVO volume = new VolumeVO(volumeName, -1, -1, -1, -1, new Long(-1), null, null, 0, Volume.Type.DATADISK);
    	volume.setPoolId(null);
    	volume.setDataCenterId(zoneId);
    	volume.setPodId(null);
    	volume.setAccountId(ownerId);
    	volume.setDomainId(domainId);
    	volume.setDiskOfferingId(diskOfferingId);
    	volume.setSize(size);
    	volume.setInstanceId(null);
    	volume.setUpdated(new Date());
    	volume.setDomainId(domainId);

    	volume = _volumeDao.persist(volume);
    	return volume;
    }
    
    @Override
    public DiskProfile allocateVolume(Long vmId,
    		Pair<? extends DiskOfferingVO, Long> rootDiskOffering,
    		List<Pair<DiskOfferingVO, Long>> dataDiskOfferings, Long templateId, Account owner) {
    	VirtualMachineTemplate template = _templateMgr.getTemplate(templateId);
    	VirtualMachine vm = _vmDao.findById(vmId);
    	DiskProfile diskProfile = null;
    	if (template.getFormat() == ImageFormat.ISO) {
    		diskProfile = allocateRawVolume(Type.ROOT, "ROOT-" + vm.getId(), rootDiskOffering.first(), rootDiskOffering.second(), vm, owner);
    	} else if (template.getFormat() == ImageFormat.BAREMETAL) {
    		// Do nothing
    	} else {
    		diskProfile = allocateTemplatedVolume(Type.ROOT, "ROOT-" + vm.getId(), rootDiskOffering.first(), template, vm, owner);
    	}

    	for (Pair<DiskOfferingVO, Long> offering : dataDiskOfferings) {
    		diskProfile = allocateRawVolume(Type.DATADISK, "DATA-" + vm.getId(), offering.first(), offering.second(), vm, owner);
    	}
    	return diskProfile;
    }
    
    public DiskProfile allocateRawVolume(Type type, String name, DiskOfferingVO offering, Long size, VirtualMachine vm, Account owner) {
        if (size == null) {
            size = offering.getDiskSize();
        } else {
            size = (size * 1024 * 1024 * 1024);
        }
        VolumeVO vol = new VolumeVO(type, name, vm.getDataCenterIdToDeployIn(), owner.getDomainId(), owner.getId(), offering.getId(), size);
        if (vm != null) {
            vol.setInstanceId(vm.getId());
        }

        if (type.equals(Type.ROOT)) {
            vol.setDeviceId(0l);
        } else {
            vol.setDeviceId(1l);
        }

        vol = _volumeDao.persist(vol);

        // Save usage event and update resource count for user vm volumes
        if (vm instanceof UserVm) {

            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_CREATE, vol.getAccountId(), vol.getDataCenterId(), vol.getId(), vol.getName(), offering.getId(), null, size);
            _usageEventDao.persist(usageEvent);

            _resourceLimitMgr.incrementResourceCount(vm.getAccountId(), ResourceType.volume);
        }
        return toDiskProfile(vol, offering);
    }

    public DiskProfile allocateTemplatedVolume(Type type, String name, DiskOfferingVO offering, VirtualMachineTemplate template, VirtualMachine vm, Account owner) {
        assert (template.getFormat() != ImageFormat.ISO) : "ISO is not a template really....";

        SearchCriteria<VMTemplateHostVO> sc = HostTemplateStatesSearch.create();
        sc.setParameters("id", template.getId());
        sc.setParameters("state", com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        sc.setJoinParameters("host", "dcId", vm.getDataCenterIdToDeployIn());
        List<VMTemplateSwiftVO> tsvs = _vmTemplateSwiftDao.listByTemplateId(template.getId());
        Long size = null;
        if (tsvs != null && tsvs.size() > 0) {
            size = tsvs.get(0).getSize();
        }
        if (size == null) {
            List<VMTemplateHostVO> sss = _vmTemplateHostDao.search(sc, null);
            if (sss == null || sss.size() == 0) {
                throw new CloudRuntimeException("Template " + template.getName() + " has not been completely downloaded to zone " + vm.getDataCenterIdToDeployIn());
            }
            size = sss.get(0).getSize();
        }

        VolumeVO vol = new VolumeVO(type, name, vm.getDataCenterIdToDeployIn(), owner.getDomainId(), owner.getId(), offering.getId(), size);
        if (vm != null) {
            vol.setInstanceId(vm.getId());
        }
        vol.setTemplateId(template.getId());

        if (type.equals(Type.ROOT)) {
            vol.setDeviceId(0l);
            if (!vm.getType().equals(VirtualMachine.Type.User)) {
                vol.setRecreatable(true);
            }
        } else {
            vol.setDeviceId(1l);
        }

        vol = _volumeDao.persist(vol);

        // Create event and update resource count for volumes if vm is a user vm
        if (vm instanceof UserVm) {

            Long offeringId = null;

            if (offering.getType() == DiskOfferingVO.Type.Disk) {
                offeringId = offering.getId();
            }

            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_CREATE, vol.getAccountId(), vol.getDataCenterId(), vol.getId(), vol.getName(), offeringId, template.getId(),
                    vol.getSize());
            _usageEventDao.persist(usageEvent);

            _resourceLimitMgr.incrementResourceCount(vm.getAccountId(), ResourceType.volume);
        }
        return toDiskProfile(vol, offering);
    }

    @DB
    protected VolumeVO switchVolume(VolumeVO existingVolume, long vmTemplateId) throws StorageUnavailableException {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        try {
            processEvent(existingVolume, Volume.Event.DestroyRequested);
        } catch (NoTransitionException e) {
            s_logger.debug("Unable to destroy existing volume: " + e.toString());
        }

        Long templateIdToUse = null;
        Long volTemplateId = existingVolume.getTemplateId();
        if (volTemplateId != null && volTemplateId.longValue() != vmTemplateId) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("switchVolume: Old Volume's templateId: " + volTemplateId + " does not match the VM's templateId: " + vmTemplateId + ", updating templateId in the new Volume");
            }
            templateIdToUse = vmTemplateId;
        }
        VolumeVO newVolume = allocateDuplicateVolume(existingVolume, templateIdToUse);
        txn.commit();
        return newVolume;

    }
    
    public void expungeVolume(VolumeVO vol, boolean force) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Expunging " + vol);
        }
        
        //Find out if the volume is present on secondary storage
        VolumeHostVO volumeHost = _volHostDao.findByVolumeId(vol.getId());
        if(volumeHost != null){
        	if (volumeHost.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED){
        		HostVO ssHost = _hostDao.findById(volumeHost.getHostId());
        		DeleteVolumeCommand dtCommand = new DeleteVolumeCommand(ssHost.getStorageUrl(), volumeHost.getInstallPath());            
        		Answer answer = _agentMgr.sendToSecStorage(ssHost, dtCommand);
        		if (answer == null || !answer.getResult()) {
        			s_logger.debug("Failed to delete " + volumeHost + " due to " + ((answer == null) ? "answer is null" : answer.getDetails()));
        			return;
        		}
        	}else if(volumeHost.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS){									
					s_logger.debug("Volume: " + vol.getName() + " is currently being uploaded; cant' delete it.");
					throw new CloudRuntimeException("Please specify a volume that is not currently being uploaded.");
        	}
            _volHostDao.remove(volumeHost.getId());
            _volumeDao.remove(vol.getId());
            return;             
        }
        
        String vmName = null;
        if (vol.getVolumeType() == Type.ROOT && vol.getInstanceId() != null) {
            VirtualMachine vm = _vmInstanceDao.findByIdIncludingRemoved(vol.getInstanceId());
            if (vm != null) {
                vmName = vm.getInstanceName();
            }
        }

        String volumePath = vol.getPath();
        Long poolId = vol.getPoolId();
        if (poolId == null || volumePath == null || volumePath.trim().isEmpty()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Marking volume that was never created as destroyed: " + vol);
            }
            _volumeDao.remove(vol.getId());
            return;
        }

        StoragePoolVO pool = (StoragePoolVO)_storagePoolMgr.getStoragePool(poolId);
        if (pool == null) {
            s_logger.debug("Removing volume as storage pool is gone: " + poolId);
            _volumeDao.remove(vol.getId());
            return;
        }

        DestroyCommand cmd = new DestroyCommand(pool, vol, vmName);
        boolean removeVolume = false;
        try {
            Answer answer = _storagePoolMgr.sendToPool(pool, cmd);
            if (answer != null && answer.getResult()) {
                removeVolume = true;
            } else {
                s_logger.info("Will retry delete of " + vol + " from " + poolId);
            }
        } catch (StorageUnavailableException e) {
            if (force) {
                s_logger.info("Storage is unavailable currently, but marking volume id=" + vol.getId() + " as expunged anyway due to force=true");
                removeVolume = true;
            } else {
                s_logger.info("Storage is unavailable currently.  Will retry delete of " + vol + " from " + poolId);
            }
        } catch (RuntimeException ex) {
            if (force) {
                s_logger.info("Failed to expunge volume, but marking volume id=" + vol.getId() + " as expunged anyway " +
                		"due to force=true. Volume failed to expunge due to ", ex);
                removeVolume = true;
            } else {
                throw ex;
            }
        } finally {
            if (removeVolume) {
                _volumeDao.remove(vol.getId());
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Volume successfully expunged from " + poolId);
                }
            }
        }

    }
    
    @Override
    @DB
    public void cleanupVolumes(long vmId) throws ConcurrentOperationException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Cleaning storage for vm: " + vmId);
        }
        List<VolumeVO> volumesForVm = _volumeDao.findByInstance(vmId);
        List<VolumeVO> toBeExpunged = new ArrayList<VolumeVO>();
        Transaction txn = Transaction.currentTxn();
        txn.start();
        for (VolumeVO vol : volumesForVm) {
            if (vol.getVolumeType().equals(Type.ROOT)) {
                // This check is for VM in Error state (volume is already destroyed)
                if (!vol.getState().equals(Volume.State.Destroy)) {
                    destroyVolume(vol);
                }
                toBeExpunged.add(vol);
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Detaching " + vol);
                }
                _volumeDao.detachVolume(vol.getId());
            }
        }
        txn.commit();

        for (VolumeVO expunge : toBeExpunged) {
            expungeVolume(expunge, false);
        }
    }
    
    @Override
    public List<VolumeVO> searchForVolumes(ListVolumesCmd cmd) {
    	//TODO: ACL code is removed
        //Account caller = UserContext.current().getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Long id = cmd.getId();
        Long vmInstanceId = cmd.getVirtualMachineId();
        String name = cmd.getVolumeName();
        String keyword = cmd.getKeyword();
        String type = cmd.getType();
        Map<String, String> tags = cmd.getTags();

        Long zoneId = cmd.getZoneId();
        Long podId = null;

        /*
        if (_accountMgr.isAdmin(caller.getType())) {
            podId = cmd.getPodId();
        }
        */

        /*Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
       _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        */
        Filter searchFilter = new Filter(VolumeVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        // hack for now, this should be done better but due to needing a join I opted to
        // do this quickly and worry about making it pretty later
        SearchBuilder<VolumeVO> sb = _volumeDao.createSearchBuilder();
       // _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("volumeType", sb.entity().getVolumeType(), SearchCriteria.Op.LIKE);
        sb.and("instanceId", sb.entity().getInstanceId(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        // Only return volumes that are not destroyed
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.NEQ);

        SearchBuilder<DiskOfferingVO> diskOfferingSearch = _diskOfferingDao.createSearchBuilder();
        diskOfferingSearch.and("systemUse", diskOfferingSearch.entity().getSystemUse(), SearchCriteria.Op.NEQ);
        sb.join("diskOfferingSearch", diskOfferingSearch, sb.entity().getDiskOfferingId(), diskOfferingSearch.entity().getId(), JoinBuilder.JoinType.LEFTOUTER);

        // display UserVM volumes only
        SearchBuilder<VMInstanceVO> vmSearch = _vmInstanceDao.createSearchBuilder();
        vmSearch.and("type", vmSearch.entity().getType(), SearchCriteria.Op.NIN);
        vmSearch.or("nulltype", vmSearch.entity().getType(), SearchCriteria.Op.NULL);
        sb.join("vmSearch", vmSearch, sb.entity().getInstanceId(), vmSearch.entity().getId(), JoinBuilder.JoinType.LEFTOUTER);
        
        if (tags != null && !tags.isEmpty()) {
            SearchBuilder<ResourceTagVO> tagSearch = _resourceTagDao.createSearchBuilder();
            for (int count=0; count < tags.size(); count++) {
                tagSearch.or().op("key" + String.valueOf(count), tagSearch.entity().getKey(), SearchCriteria.Op.EQ);
                tagSearch.and("value" + String.valueOf(count), tagSearch.entity().getValue(), SearchCriteria.Op.EQ);
                tagSearch.cp();
            }
            tagSearch.and("resourceType", tagSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
            sb.groupBy(sb.entity().getId());
            sb.join("tagSearch", tagSearch, sb.entity().getId(), tagSearch.entity().getResourceId(), JoinBuilder.JoinType.INNER);
        }

        // now set the SC criteria...
        SearchCriteria<VolumeVO> sc = sb.create();
        //_accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (keyword != null) {
            SearchCriteria<VolumeVO> ssc = _volumeDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("volumeType", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }

        sc.setJoinParameters("diskOfferingSearch", "systemUse", 1);
        
        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", TaggedResourceType.Volume.toString());
            for (String key : tags.keySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), key);
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), tags.get(key));
                count++;
            }
        }

        if (id != null) {
            sc.setParameters("id", id);
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
        if (podId != null) {
            sc.setParameters("podId", podId);
        }

        // Don't return DomR and ConsoleProxy volumes
        sc.setJoinParameters("vmSearch", "type", VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm, VirtualMachine.Type.DomainRouter);

        // Only return volumes that are not destroyed
        sc.setParameters("state", Volume.State.Destroy);

        return _volumeDao.search(sc, searchFilter);
    }

	@Override
	public Volume copyVolume(Long volumeId, Long destStoragePoolId) {
		// TODO Auto-generated method stub
		return null;
	}
	
    @Override
    public boolean processEvent(Volume vol, Volume.Event event) throws NoTransitionException {
        return _volStateMachine.transitTo(vol, event, null, _volumeDao);
    }

    protected DiskProfile createDiskCharacteristics(VolumeVO volume, VMTemplateVO template, DataCenterVO dc, DiskOfferingVO diskOffering) {
        if (volume.getVolumeType() == Type.ROOT && Storage.ImageFormat.ISO != template.getFormat()) {
            SearchCriteria<VMTemplateHostVO> sc = HostTemplateStatesSearch.create();
            sc.setParameters("id", template.getId());
            sc.setParameters("state", com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            sc.setJoinParameters("host", "dcId", dc.getId());

            List<VMTemplateHostVO> sss = _vmTemplateHostDao.search(sc, null);
            if (sss.size() == 0) {
                throw new CloudRuntimeException("Template " + template.getName() + " has not been completely downloaded to zone " + dc.getId());
            }
            VMTemplateHostVO ss = sss.get(0);

            return new DiskProfile(volume.getId(), volume.getVolumeType(), volume.getName(), diskOffering.getId(), ss.getSize(), diskOffering.getTagsArray(), diskOffering.getUseLocalStorage(),
                    diskOffering.isRecreatable(), Storage.ImageFormat.ISO != template.getFormat() ? template.getId() : null);
        } else {
            return new DiskProfile(volume.getId(), volume.getVolumeType(), volume.getName(), diskOffering.getId(), diskOffering.getDiskSize(), diskOffering.getTagsArray(),
                    diskOffering.getUseLocalStorage(), diskOffering.isRecreatable(), null);
        }
    }

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		HostTemplateStatesSearch = _vmTemplateHostDao.createSearchBuilder();
        HostTemplateStatesSearch.and("id", HostTemplateStatesSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        HostTemplateStatesSearch.and("state", HostTemplateStatesSearch.entity().getDownloadState(), SearchCriteria.Op.EQ);

        SearchBuilder<HostVO> HostSearch = _hostDao.createSearchBuilder();
        HostSearch.and("dcId", HostSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);

        HostTemplateStatesSearch.join("host", HostSearch, HostSearch.entity().getId(), HostTemplateStatesSearch.entity().getHostId(), JoinBuilder.JoinType.INNER);
        HostSearch.done();
        HostTemplateStatesSearch.done();
		return false;
	}
	
	protected void VolumeManagerImple() {
		 _volStateMachine = Volume.State.getStateMachine();
	}

	@Override
	public boolean start() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
}
