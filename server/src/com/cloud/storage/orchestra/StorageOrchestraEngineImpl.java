package com.cloud.storage.orchestra;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.ConfigurationException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateVolumeFromSnapshotAnswer;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.UpgradeSnapshotCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.api.commands.CreateVolumeCmd;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeHostVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.pool.Storage;
import com.cloud.storage.pool.StoragePool;
import com.cloud.storage.pool.Storage.ImageFormat;
import com.cloud.storage.pool.Storage.StoragePoolType;
import com.cloud.storage.volume.InvalidParameterValueException;
import com.cloud.storage.volume.PermissionDeniedException;
import com.cloud.storage.volume.StorageFilerTO;
import com.cloud.storage.volume.StorageMigrationCleanupMaid;
import com.cloud.storage.volume.VMTemplateStoragePoolVO;
import com.cloud.storage.volume.Volume;
import com.cloud.storage.volume.Volume.Event;
import com.cloud.storage.volume.Volume.Type;
import com.cloud.storage.volume.VolumeManager;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.VMInstanceDao;

public class StorageOrchestraEngineImpl implements StorageOrchestraEngine, Manager {
	
	
	@Inject
	protected TemplateManager _templateMgr;
	@Inject
	protected VolumeManager _volumeMgr;
	@Inject
	protected VMInstanceDao _vmDao;
	 @DB
	    protected Pair<VolumeVO, String> createVolumeFromSnapshot(VolumeVO volume, SnapshotVO snapshot) {
	        VolumeVO createdVolume = null;
	        Long volumeId = volume.getId();

	        String volumeFolder = null;

	        try {
	            stateTransitTo(volume, Volume.Event.CreateRequested);
	        } catch (NoTransitionException e) {
	            s_logger.debug(e.toString());
	            return null;
	        }
	        // Create the Volume object and save it so that we can return it to the user
	        Account account = _accountDao.findById(volume.getAccountId());

	        final HashSet<StoragePool> poolsToAvoid = new HashSet<StoragePool>();
	        StoragePoolVO pool = null;
	        boolean success = false;
	        Set<Long> podsToAvoid = new HashSet<Long>();
	        Pair<HostPodVO, Long> pod = null;
	        String volumeUUID = null;
	        String details = null;

	        DiskOfferingVO diskOffering = _diskOfferingDao.findByIdIncludingRemoved(volume.getDiskOfferingId());
	        DataCenterVO dc = _dcDao.findById(volume.getDataCenterId());
	        DiskProfile dskCh = new DiskProfile(volume, diskOffering, snapshot.getHypervisorType());

	        int retry = 0;
	        // Determine what pod to store the volume in
	        while ((pod = _resourceMgr.findPod(null, null, dc, account.getId(), podsToAvoid)) != null) {
	            podsToAvoid.add(pod.first().getId());
	            // Determine what storage pool to store the volume in
	            while ((pool = findStoragePool(dskCh, dc, pod.first(), null, null, poolsToAvoid)) != null) {
	                poolsToAvoid.add(pool);
	                volumeFolder = pool.getPath();
	                if (s_logger.isDebugEnabled()) {
	                    s_logger.debug("Attempting to create volume from snapshotId: " + snapshot.getId() + " on storage pool " + pool.getName());
	                }

	                // Get the newly created VDI from the snapshot.
	                // This will return a null volumePath if it could not be created
	                Pair<String, String> volumeDetails = createVDIFromSnapshot(UserContext.current().getCallerUserId(), snapshot, pool);

	                volumeUUID = volumeDetails.first();
	                details = volumeDetails.second();

	                if (volumeUUID != null) {
	                    if (s_logger.isDebugEnabled()) {
	                        s_logger.debug("Volume with UUID " + volumeUUID + " was created on storage pool " + pool.getName());
	                    }
	                    success = true;
	                    break; // break out of the "find storage pool" loop
	                } else {
	                    retry++;
	                    if (retry >= 3) {
	                        _volsDao.expunge(volumeId);
	                        String msg = "Unable to create volume from snapshot " + snapshot.getId() + " after retrying 3 times, due to " + details;
	                        s_logger.debug(msg);
	                        throw new CloudRuntimeException(msg);

	                    }
	                }
	                s_logger.warn("Unable to create volume on pool " + pool.getName() + ", reason: " + details);
	            }

	            if (success) {
	                break; // break out of the "find pod" loop
	            }
	        }

	        if (!success) {
	            _volsDao.expunge(volumeId);
	            String msg = "Unable to create volume from snapshot " + snapshot.getId() + " due to " + details;
	            s_logger.debug(msg);
	            throw new CloudRuntimeException(msg);

	        }

	        createdVolume = _volsDao.findById(volumeId);

	        try {
	            if (success) {
	                createdVolume.setPodId(pod.first().getId());
	                createdVolume.setPoolId(pool.getId());
	                createdVolume.setPoolType(pool.getPoolType());
	                createdVolume.setFolder(volumeFolder);
	                createdVolume.setPath(volumeUUID);
	                createdVolume.setDomainId(account.getDomainId());
	                stateTransitTo(createdVolume, Volume.Event.OperationSucceeded);
	            }
	        } catch (NoTransitionException e) {
	            s_logger.debug("Failed to update volume state: " + e.toString());
	            return null;
	        }

	        return new Pair<VolumeVO, String>(createdVolume, details);
	    }
	 
	 @Override
	    @DB
	    public VolumeVO copyVolumeFromSecToPrimary(VolumeVO volume, VMInstanceVO vm, VMTemplateVO template, DataCenterVO dc, HostPodVO pod, Long clusterId, ServiceOfferingVO offering, DiskOfferingVO diskOffering,
	            List<StoragePoolVO> avoids, long size, HypervisorType hyperType) throws NoTransitionException {
	    	
	    	final HashSet<StoragePool> avoidPools = new HashSet<StoragePool>(avoids);
	    	DiskProfile dskCh = createDiskCharacteristics(volume, template, dc, diskOffering);
	    	dskCh.setHyperType(vm.getHypervisorType());
	    	// Find a suitable storage to create volume on 
	    	StoragePoolVO destPool = findStoragePool(dskCh, dc, pod, clusterId, vm, avoidPools);
	    	
	    	// Copy the volume from secondary storage to the destination storage pool    	  	
	    	stateTransitTo(volume, Event.CopyRequested);
	    	VolumeHostVO volumeHostVO = _volumeHostDao.findByVolumeId(volume.getId());
	    	HostVO secStorage = _hostDao.findById(volumeHostVO.getHostId());
	    	String secondaryStorageURL = secStorage.getStorageUrl();
	    	String[] volumePath = volumeHostVO.getInstallPath().split("/");
	    	String volumeUUID = volumePath[volumePath.length - 1].split("\\.")[0];
	    	
	        CopyVolumeCommand cvCmd = new CopyVolumeCommand(volume.getId(), volumeUUID, destPool, secondaryStorageURL, false, _copyvolumewait);
	        CopyVolumeAnswer cvAnswer;
			try {
	            cvAnswer = (CopyVolumeAnswer) sendToPool(destPool, cvCmd);
	        } catch (StorageUnavailableException e1) {
	        	stateTransitTo(volume, Event.CopyFailed);
	            throw new CloudRuntimeException("Failed to copy the volume from secondary storage to the destination primary storage pool.");
	        }

	        if (cvAnswer == null || !cvAnswer.getResult()) {
	        	stateTransitTo(volume, Event.CopyFailed);
	            throw new CloudRuntimeException("Failed to copy the volume from secondary storage to the destination primary storage pool.");
	        }        
	        Transaction txn = Transaction.currentTxn();
	        txn.start();        
	        volume.setPath(cvAnswer.getVolumePath());
	        volume.setFolder(destPool.getPath());
	        volume.setPodId(destPool.getPodId());
	        volume.setPoolId(destPool.getId());        
	        volume.setPodId(destPool.getPodId());
	        stateTransitTo(volume, Event.CopySucceeded); 
	        UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(), volume.getDiskOfferingId(), null, volume.getSize());
	        _usageEventDao.persist(usageEvent);
	        _volumeHostDao.remove(volumeHostVO.getId());
	    	txn.commit();
			return volume;
	    	
	    }
	 
	 
	 protected Pair<String, String> createVDIFromSnapshot(long userId, SnapshotVO snapshot, StoragePoolVO pool) {
	        String vdiUUID = null;
	        Long snapshotId = snapshot.getId();
	        Long volumeId = snapshot.getVolumeId();
	        String primaryStoragePoolNameLabel = pool.getUuid(); // pool's uuid is actually the namelabel.
	        Long dcId = snapshot.getDataCenterId();
	        String secondaryStoragePoolUrl = _snapMgr.getSecondaryStorageURL(snapshot);
	        long accountId = snapshot.getAccountId();

	        String backedUpSnapshotUuid = snapshot.getBackupSnapshotId();
	        snapshot = _snapshotDao.findById(snapshotId);
	        if (snapshot.getVersion().trim().equals("2.1")) {
	            VolumeVO volume = _volsDao.findByIdIncludingRemoved(volumeId);
	            if (volume == null) {
	                throw new CloudRuntimeException("failed to upgrade snapshot " + snapshotId + " due to unable to find orignal volume:" + volumeId + ", try it later ");
	            }
	            if (volume.getTemplateId() == null) {
	                _snapshotDao.updateSnapshotVersion(volumeId, "2.1", "2.2");
	            } else {
	                VMTemplateVO template = _templateDao.findByIdIncludingRemoved(volume.getTemplateId());
	                if (template == null) {
	                    throw new CloudRuntimeException("failed to upgrade snapshot " + snapshotId + " due to unalbe to find orignal template :" + volume.getTemplateId() + ", try it later ");
	                }
	                Long templateId = template.getId();
	                Long tmpltAccountId = template.getAccountId();
	                if (!_snapshotDao.lockInLockTable(snapshotId.toString(), 10)) {
	                    throw new CloudRuntimeException("failed to upgrade snapshot " + snapshotId + " due to this snapshot is being used, try it later ");
	                }
	                UpgradeSnapshotCommand cmd = new UpgradeSnapshotCommand(null, secondaryStoragePoolUrl, dcId, accountId, volumeId, templateId, tmpltAccountId, null, snapshot.getBackupSnapshotId(),
	                        snapshot.getName(), "2.1");
	                Answer answer = null;
	                try {
	                    answer = sendToPool(pool, cmd);
	                } catch (StorageUnavailableException e) {
	                } finally {
	                    _snapshotDao.unlockFromLockTable(snapshotId.toString());
	                }
	                if ((answer != null) && answer.getResult()) {
	                    _snapshotDao.updateSnapshotVersion(volumeId, "2.1", "2.2");
	                } else {
	                    return new Pair<String, String>(null, "Unable to upgrade snapshot from 2.1 to 2.2 for " + snapshot.getId());
	                }
	            }
	        }
	        String basicErrMsg = "Failed to create volume from " + snapshot.getName() + " on pool " + pool;
	        try {
	            if (snapshot.getSwiftId() != null && snapshot.getSwiftId() != 0) {
	                _snapshotMgr.downloadSnapshotsFromSwift(snapshot);
	            }
	            CreateVolumeFromSnapshotCommand createVolumeFromSnapshotCommand = new CreateVolumeFromSnapshotCommand(primaryStoragePoolNameLabel, secondaryStoragePoolUrl, dcId, accountId, volumeId,
	                    backedUpSnapshotUuid, snapshot.getName(), _createVolumeFromSnapshotWait);
	            CreateVolumeFromSnapshotAnswer answer;
	            if (!_snapshotDao.lockInLockTable(snapshotId.toString(), 10)) {
	                throw new CloudRuntimeException("failed to create volume from " + snapshotId + " due to this snapshot is being used, try it later ");
	            }
	            answer = (CreateVolumeFromSnapshotAnswer) sendToPool(pool, createVolumeFromSnapshotCommand);
	            if (answer != null && answer.getResult()) {
	                vdiUUID = answer.getVdi();
	            } else {
	                s_logger.error(basicErrMsg + " due to " + ((answer == null) ? "null" : answer.getDetails()));
	                throw new CloudRuntimeException(basicErrMsg);
	            }
	        } catch (StorageUnavailableException e) {
	            s_logger.error(basicErrMsg);
	        } finally {
	            if (snapshot.getSwiftId() != null) {
	                _snapshotMgr.deleteSnapshotsDirForVolume(secondaryStoragePoolUrl, dcId, accountId, volumeId);
	            }
	            _snapshotDao.unlockFromLockTable(snapshotId.toString());
	        }
	        return new Pair<String, String>(vdiUUID, basicErrMsg);
	    }
	 
	 protected Pair<String, String> createVDIFromSnapshot(long userId, SnapshotVO snapshot, StoragePoolVO pool) {
	        String vdiUUID = null;
	        Long snapshotId = snapshot.getId();
	        Long volumeId = snapshot.getVolumeId();
	        String primaryStoragePoolNameLabel = pool.getUuid(); // pool's uuid is actually the namelabel.
	        Long dcId = snapshot.getDataCenterId();
	        String secondaryStoragePoolUrl = _snapMgr.getSecondaryStorageURL(snapshot);
	        long accountId = snapshot.getAccountId();

	        String backedUpSnapshotUuid = snapshot.getBackupSnapshotId();
	        snapshot = _snapshotDao.findById(snapshotId);
	        if (snapshot.getVersion().trim().equals("2.1")) {
	            VolumeVO volume = _volsDao.findByIdIncludingRemoved(volumeId);
	            if (volume == null) {
	                throw new CloudRuntimeException("failed to upgrade snapshot " + snapshotId + " due to unable to find orignal volume:" + volumeId + ", try it later ");
	            }
	            if (volume.getTemplateId() == null) {
	                _snapshotDao.updateSnapshotVersion(volumeId, "2.1", "2.2");
	            } else {
	                VMTemplateVO template = _templateDao.findByIdIncludingRemoved(volume.getTemplateId());
	                if (template == null) {
	                    throw new CloudRuntimeException("failed to upgrade snapshot " + snapshotId + " due to unalbe to find orignal template :" + volume.getTemplateId() + ", try it later ");
	                }
	                Long templateId = template.getId();
	                Long tmpltAccountId = template.getAccountId();
	                if (!_snapshotDao.lockInLockTable(snapshotId.toString(), 10)) {
	                    throw new CloudRuntimeException("failed to upgrade snapshot " + snapshotId + " due to this snapshot is being used, try it later ");
	                }
	                UpgradeSnapshotCommand cmd = new UpgradeSnapshotCommand(null, secondaryStoragePoolUrl, dcId, accountId, volumeId, templateId, tmpltAccountId, null, snapshot.getBackupSnapshotId(),
	                        snapshot.getName(), "2.1");
	                Answer answer = null;
	                try {
	                    answer = sendToPool(pool, cmd);
	                } catch (StorageUnavailableException e) {
	                } finally {
	                    _snapshotDao.unlockFromLockTable(snapshotId.toString());
	                }
	                if ((answer != null) && answer.getResult()) {
	                    _snapshotDao.updateSnapshotVersion(volumeId, "2.1", "2.2");
	                } else {
	                    return new Pair<String, String>(null, "Unable to upgrade snapshot from 2.1 to 2.2 for " + snapshot.getId());
	                }
	            }
	        }
	        String basicErrMsg = "Failed to create volume from " + snapshot.getName() + " on pool " + pool;
	        try {
	            if (snapshot.getSwiftId() != null && snapshot.getSwiftId() != 0) {
	                _snapshotMgr.downloadSnapshotsFromSwift(snapshot);
	            }
	            CreateVolumeFromSnapshotCommand createVolumeFromSnapshotCommand = new CreateVolumeFromSnapshotCommand(primaryStoragePoolNameLabel, secondaryStoragePoolUrl, dcId, accountId, volumeId,
	                    backedUpSnapshotUuid, snapshot.getName(), _createVolumeFromSnapshotWait);
	            CreateVolumeFromSnapshotAnswer answer;
	            if (!_snapshotDao.lockInLockTable(snapshotId.toString(), 10)) {
	                throw new CloudRuntimeException("failed to create volume from " + snapshotId + " due to this snapshot is being used, try it later ");
	            }
	            answer = (CreateVolumeFromSnapshotAnswer) sendToPool(pool, createVolumeFromSnapshotCommand);
	            if (answer != null && answer.getResult()) {
	                vdiUUID = answer.getVdi();
	            } else {
	                s_logger.error(basicErrMsg + " due to " + ((answer == null) ? "null" : answer.getDetails()));
	                throw new CloudRuntimeException(basicErrMsg);
	            }
	        } catch (StorageUnavailableException e) {
	            s_logger.error(basicErrMsg);
	        } finally {
	            if (snapshot.getSwiftId() != null) {
	                _snapshotMgr.deleteSnapshotsDirForVolume(secondaryStoragePoolUrl, dcId, accountId, volumeId);
	            }
	            _snapshotDao.unlockFromLockTable(snapshotId.toString());
	        }
	        return new Pair<String, String>(vdiUUID, basicErrMsg);
	    }
	 
	 @Override
	    public void prepareForMigration(VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest) {
	        List<VolumeVO> vols = _volsDao.findUsableVolumesForInstance(vm.getId());
	        if (s_logger.isDebugEnabled()) {
	            s_logger.debug("Preparing " + vols.size() + " volumes for " + vm);
	        }

	        for (VolumeVO vol : vols) {
	            StoragePool pool = _storagePoolDao.findById(vol.getPoolId());
	            vm.addDisk(new VolumeTO(vol, pool));
	        }

	        if (vm.getType() == VirtualMachine.Type.User) {
	            UserVmVO userVM = (UserVmVO) vm.getVirtualMachine();
	            if (userVM.getIsoId() != null) {
	                Pair<String, String> isoPathPair = getAbsoluteIsoPath(userVM.getIsoId(), userVM.getDataCenterIdToDeployIn());
	                if (isoPathPair != null) {
	                    String isoPath = isoPathPair.first();
	                    VolumeTO iso = new VolumeTO(vm.getId(), Volume.Type.ISO, StoragePoolType.ISO, null, null, null, isoPath, 0, null, null);
	                    vm.addDisk(iso);
	                }
	            }
	        }
	    }

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
        
		return false;
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

	@Override
    public void prepare(VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, boolean recreate) throws StorageUnavailableException, InsufficientStorageCapacityException {

        if (dest == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("DeployDestination cannot be null, cannot prepare Volumes for the vm: " + vm);
            }
            throw new CloudRuntimeException("Unable to prepare Volume for vm because DeployDestination is null, vm:" + vm);
        }
        List<VolumeVO> vols = _volumeDao.findUsableVolumesForInstance(vm.getId());
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Checking if we need to prepare " + vols.size() + " volumes for " + vm);
        }

        List<VolumeVO> recreateVols = new ArrayList<VolumeVO>(vols.size());

        for (VolumeVO vol : vols) {
            StoragePool assignedPool = null;
            if (dest.getStorageForDisks() != null) {
                assignedPool = dest.getStorageForDisks().get(vol);
            }
            if (assignedPool == null && recreate) {
            	assignedPool = _storagePoolMgr.getStoragePool(vol.getPoolId());
            	
            }
            if (assignedPool != null || recreate) {
                Volume.State state = vol.getState();
                if (state == Volume.State.Allocated || state == Volume.State.Creating) {
                    recreateVols.add(vol);
                } else {
                    if (vol.isRecreatable()) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Volume " + vol + " will be recreated on storage pool " + assignedPool + " assigned by deploymentPlanner");
                        }
                        recreateVols.add(vol);
                    } else {
                        if (assignedPool.getId() != vol.getPoolId()) {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Mismatch in storage pool " + assignedPool + " assigned by deploymentPlanner and the one associated with volume " + vol);
                            }
                            DiskOfferingVO diskOffering = _diskOfferingDao.findById(vol.getDiskOfferingId());
                            if (diskOffering.getUseLocalStorage())
                            {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Local volume " + vol + " will be recreated on storage pool " + assignedPool + " assigned by deploymentPlanner");
                                }
                                recreateVols.add(vol);
                            } else {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Shared volume " + vol + " will be migrated on storage pool " + assignedPool + " assigned by deploymentPlanner");
                                }
                                try {
                                    List<Volume> volumesToMigrate = new ArrayList<Volume>();
                                    volumesToMigrate.add(vol);
                                    migrateVolumes(volumesToMigrate, assignedPool);
                                    vm.addDisk(new VolumeTO(vol, assignedPool));
                                } catch (ConcurrentOperationException e) {
                                    throw new CloudRuntimeException("Migration of volume " + vol + " to storage pool " + assignedPool + " failed", e);
                                }
                            }
                        } else {
                            StoragePoolVO pool = (StoragePoolVO)_storagePoolMgr.getStoragePoolById(vol.getPoolId());
                            vm.addDisk(new VolumeTO(vol, pool));
                        }
                        
                    }
                }
            } else {
                if (vol.getPoolId() == null) {
                    throw new StorageUnavailableException("Volume has no pool associate and also no storage pool assigned in DeployDestination, Unable to create " + vol, Volume.class, vol.getId());
                }
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("No need to recreate the volume: " + vol + ", since it already has a pool assigned: " + vol.getPoolId() + ", adding disk to VM");
                }
                StoragePoolVO pool = (StoragePoolVO)_storagePoolMgr.getStoragePoolById(vol.getPoolId());
                vm.addDisk(new VolumeTO(vol, pool));
            }
        }

        for (VolumeVO vol : recreateVols) {
            VolumeVO newVol;
            StoragePool existingPool = null;
            if (recreate && (dest.getStorageForDisks() == null || dest.getStorageForDisks().get(vol) == null)) {
            	existingPool = (StoragePoolVO)_storagePoolMgr.getStoragePoolById(vol.getPoolId());
            	s_logger.debug("existing pool: " + existingPool.getId());
            }
            
            if (vol.getState() == Volume.State.Allocated || vol.getState() == Volume.State.Creating) {
                newVol = vol;
            } else {
                newVol = switchVolume(vol, vm);
                // update the volume->storagePool map since volumeId has changed
                if (dest.getStorageForDisks() != null && dest.getStorageForDisks().containsKey(vol)) {
                    StoragePool poolWithOldVol = dest.getStorageForDisks().get(vol);
                    dest.getStorageForDisks().put(newVol, poolWithOldVol);
                    dest.getStorageForDisks().remove(vol);
                }
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Created new volume " + newVol + " for old volume " + vol);
                }
            }

            try {
                stateTransitTo(newVol, Volume.Event.CreateRequested);
            } catch (NoTransitionException e) {
                throw new CloudRuntimeException("Unable to create " + e.toString());
            }

            Pair<VolumeTO, StoragePool> created = createVolume(newVol, _diskOfferingDao.findById(newVol.getDiskOfferingId()), vm, vols, dest, existingPool);

            if (created == null) {
                Long poolId = newVol.getPoolId();
                newVol.setPoolId(null);
                try {
                    stateTransitTo(newVol, Volume.Event.OperationFailed);
                } catch (NoTransitionException e) {
                    throw new CloudRuntimeException("Unable to update the failure on a volume: " + newVol, e);
                }
                throw new StorageUnavailableException("Unable to create " + newVol, poolId == null ? -1L : poolId);
            }
            created.first().setDeviceId(newVol.getDeviceId().intValue());
            newVol.setFolder(created.second().getPath());
            newVol.setPath(created.first().getPath());
            newVol.setSize(created.first().getSize());
            newVol.setPoolType(created.second().getPoolType());
            newVol.setPodId(created.second().getPodId());
            try {
                stateTransitTo(newVol, Volume.Event.OperationSucceeded);
            } catch (NoTransitionException e) {
                throw new CloudRuntimeException("Unable to update an CREATE operation succeeded on volume " + newVol, e);
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Volume " + newVol + " is created on " + created.second());
            }

            vm.addDisk(created.first());
        }
    }
	
	 /*
     * Just allocate a volume in the database, don't send the createvolume cmd to hypervisor. The volume will be finally
     * created
     * only when it's attached to a VM.
     */
    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_CREATE, eventDescription = "creating volume", create = true)
    public VolumeVO allocVolume(CreateVolumeCmd cmd) throws ResourceAllocationException {
        // FIXME: some of the scheduled event stuff might be missing here...
        Account caller = UserContext.current().getCaller();

        long ownerId = cmd.getEntityOwnerId();

        // permission check
        _accountMgr.checkAccess(caller, null, true, _accountMgr.getActiveAccountById(ownerId));

        // Check that the resource limit for volumes won't be exceeded
        _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(ownerId), ResourceType.volume);

        Long zoneId = cmd.getZoneId();
        Long diskOfferingId = null;
        Long size = null;

        // validate input parameters before creating the volume
        if ((cmd.getSnapshotId() == null && cmd.getDiskOfferingId() == null) || (cmd.getSnapshotId() != null && cmd.getDiskOfferingId() != null)) {
            throw new InvalidParameterValueException("Either disk Offering Id or snapshot Id must be passed whilst creating volume");
        }

        if (cmd.getSnapshotId() == null) {// create a new volume

            diskOfferingId = cmd.getDiskOfferingId();
            size = cmd.getSize();
            Long sizeInGB = size;
            if (size != null) {
                if (size > 0) {
                    size = size * 1024 * 1024 * 1024; // user specify size in GB
                } else {
                    throw new InvalidParameterValueException("Disk size must be larger than 0");
                }
            }
            if (diskOfferingId == null) {
                throw new InvalidParameterValueException("Missing parameter(s),either a positive volume size or a valid disk offering id must be specified.");
            }
            // Check that the the disk offering is specified
            DiskOfferingVO diskOffering = _diskOfferingDao.findById(diskOfferingId);
            if ((diskOffering == null) || diskOffering.getRemoved() != null || !DiskOfferingVO.Type.Disk.equals(diskOffering.getType())) {
                throw new InvalidParameterValueException("Please specify a valid disk offering.");
            }

            if (diskOffering.isCustomized()) {
                if (size == null) {
                    throw new InvalidParameterValueException("This disk offering requires a custom size specified");
                }
                if ((sizeInGB < _customDiskOfferingMinSize) || (sizeInGB > _customDiskOfferingMaxSize)) {
                    throw new InvalidParameterValueException("Volume size: " + sizeInGB + "GB is out of allowed range. Max: " + _customDiskOfferingMaxSize + " Min:" + _customDiskOfferingMinSize);
                }
            }

            if (!diskOffering.isCustomized() && size != null) {
                throw new InvalidParameterValueException("This disk offering does not allow custom size");
            }

            if (diskOffering.getDomainId() == null) {
                // do nothing as offering is public
            } else {
                _configMgr.checkDiskOfferingAccess(caller, diskOffering);
            }

            if (diskOffering.getDiskSize() > 0) {
                size = diskOffering.getDiskSize();
            }

            if (!validateVolumeSizeRange(size)) {// convert size from mb to gb for validation
                throw new InvalidParameterValueException("Invalid size for custom volume creation: " + size + " ,max volume size is:" + _maxVolumeSizeInGb);
            }
        } else { // create volume from snapshot
            Long snapshotId = cmd.getSnapshotId();
            SnapshotVO snapshotCheck = _snapshotDao.findById(snapshotId);
            if (snapshotCheck == null) {
                throw new InvalidParameterValueException("unable to find a snapshot with id " + snapshotId);
            }

            if (snapshotCheck.getStatus() != Snapshot.Status.BackedUp) {
                throw new InvalidParameterValueException("Snapshot id=" + snapshotId + " is not in " + Snapshot.Status.BackedUp + " state yet and can't be used for volume creation");
            }

            diskOfferingId = (cmd.getDiskOfferingId() != null) ? cmd.getDiskOfferingId() : snapshotCheck.getDiskOfferingId();
            zoneId = snapshotCheck.getDataCenterId();
            size = snapshotCheck.getSize(); // ; disk offering is used for tags purposes

            // check snapshot permissions
            _accountMgr.checkAccess(caller, null, true, snapshotCheck);

            /*
             * // bug #11428. Operation not supported if vmware and snapshots parent volume = ROOT
             * if(snapshotCheck.getHypervisorType() == HypervisorType.VMware
             * && _volumeDao.findByIdIncludingRemoved(snapshotCheck.getVolumeId()).getVolumeType() == Type.ROOT){
             * throw new UnsupportedServiceException("operation not supported, snapshot with id " + snapshotId +
             * " is created from ROOT volume");
             * }
             * 
             */
        }

        // Verify that zone exists
        DataCenterVO zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id " + zoneId);
        }

        // Check if zone is disabled
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getType())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + zoneId);
        }

        // Check that there is a shared primary storage pool in the specified zone
        List<StoragePoolVO> storagePools = _storagePoolDao.listByDataCenterId(zoneId);
        boolean sharedPoolExists = false;
        for (StoragePoolVO storagePool : storagePools) {
            if (storagePool.isShared()) {
                sharedPoolExists = true;
            }
        }

        // Check that there is at least one host in the specified zone
        List<HostVO> hosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByType(Host.Type.Routing, zoneId);
        if (hosts.isEmpty()) {
            throw new InvalidParameterValueException("There is no workable host in data center id " + zoneId + ", please check hosts' agent status and see if they are disabled");
        }

        if (!sharedPoolExists) {
            throw new InvalidParameterValueException("Please specify a zone that has at least one shared primary storage pool.");
        }

        String userSpecifiedName = cmd.getVolumeName();
        if (userSpecifiedName == null) {
            userSpecifiedName = getRandomVolumeName();
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        VolumeVO volume = new VolumeVO(userSpecifiedName, -1, -1, -1, -1, new Long(-1), null, null, 0, Volume.Type.DATADISK);
        volume.setPoolId(null);
        volume.setDataCenterId(zoneId);
        volume.setPodId(null);
        volume.setAccountId(ownerId);
        volume.setDomainId(((caller == null) ? Domain.ROOT_DOMAIN : caller.getDomainId()));
        volume.setDiskOfferingId(diskOfferingId);
        volume.setSize(size);
        volume.setInstanceId(null);
        volume.setUpdated(new Date());
        volume.setDomainId((caller == null) ? Domain.ROOT_DOMAIN : caller.getDomainId());

        volume = _volumeDao.persist(volume);
        if(cmd.getSnapshotId() == null){
        	//for volume created from snapshot, create usage event after volume creation
        	UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(), diskOfferingId, null, size);
        	_usageEventDao.persist(usageEvent);
        }

        UserContext.current().setEventDetails("Volume Id: " + volume.getId());

        // Increment resource count during allocation; if actual creation fails, decrement it
        _resourceLimitMgr.incrementResourceCount(volume.getAccountId(), ResourceType.volume);

        txn.commit();

        return volume;
    }
    
    public Pair<VolumeTO, StoragePool> createVolume(VolumeVO toBeCreated, DiskOfferingVO offering, VirtualMachineProfile<? extends VirtualMachine> vm, List<? extends Volume> alreadyCreated,
            DeployDestination dest, StoragePool sPool) throws StorageUnavailableException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating volume: " + toBeCreated);
        }
        DiskProfile diskProfile = new DiskProfile(toBeCreated, offering, vm.getHypervisorType());

        VMTemplateVO template = null;
        if (toBeCreated.getTemplateId() != null) {
            template = _imageMgr.getImageById(toBeCreated.getTemplateId());
        }
        
        StoragePool pool = null;
        if (sPool != null) {
        	pool = sPool;
        } else {
        	pool = dest.getStorageForDisks().get(toBeCreated);
        }

        if (pool != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Trying to create in " + pool);
            }
            toBeCreated.setPoolId(pool.getId());
            try {
                stateTransitTo(toBeCreated, Volume.Event.OperationRetry);
            } catch (NoTransitionException e) {
                throw new CloudRuntimeException("Unable to retry a create operation on volume " + toBeCreated);
            }

            CreateCommand cmd = null;
            VMTemplateStoragePoolVO tmpltStoredOn = null;

            for (int i = 0; i < 2; i++) {
                if (template != null && template.getFormat() != Storage.ImageFormat.ISO) {
                    tmpltStoredOn = _tmpltMgr.prepareTemplateForCreate(template, pool);
                    if (tmpltStoredOn == null) {
                        s_logger.debug("Cannot use this pool " + pool + " because we can't propagate template " + template);
                        return null;
                    }
                    cmd = new CreateCommand(diskProfile, tmpltStoredOn.getLocalDownloadPath(), new StorageFilerTO(pool));
                } else {
                    if (template != null && Storage.ImageFormat.ISO == template.getFormat()) {
                        VMTemplateHostVO tmpltHostOn = _tmpltMgr.prepareISOForCreate(template, pool);
                        if (tmpltHostOn == null) {
                            throw new CloudRuntimeException("Did not find ISO in secondry storage in zone " + pool.getDataCenterId());
                        }
                    }
                    cmd = new CreateCommand(diskProfile, new StorageFilerTO(pool));
                }
                long[] hostIdsToTryFirst = { dest.getHost().getId() };
                Answer answer = _storagePoolMgr.sendToPool(pool, hostIdsToTryFirst, cmd);
                if (answer.getResult()) {
                    CreateAnswer createAnswer = (CreateAnswer) answer;
                    return new Pair<VolumeTO, StoragePool>(createAnswer.getVolume(), pool);
                } else {
                    if (tmpltStoredOn != null && (answer instanceof CreateAnswer) && ((CreateAnswer) answer).templateReloadRequested()) {
                        if (!_tmpltMgr.resetTemplateDownloadStateOnPool(tmpltStoredOn.getId())) {
                            break; // break out of template-redeploy retry loop
                        }
                    } else {
                        break;
                    }
                }
            }
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Unable to create volume " + toBeCreated);
        }
        return null;
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
                    if (!stateTransitTo(volume, Volume.Event.MigrationRequested)) {
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
                        stateTransitTo(volume, Volume.Event.OperationFailed);
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
                            stateTransitTo(volume, Volume.Event.OperationSucceeded);
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
    public boolean StorageMigration(VirtualMachineProfile<? extends VirtualMachine> vm, StoragePool destPool) throws ConcurrentOperationException {
        List<VolumeVO> vols = _volumeDao.findUsableVolumesForInstance(vm.getId());
        List<Volume> volumesNeedToMigrate = new ArrayList<Volume>();

        for (VolumeVO volume : vols) {
            if (volume.getState() != Volume.State.Ready) {
                s_logger.debug("volume: " + volume.getId() + " is in " + volume.getState() + " state");
                throw new CloudRuntimeException("volume: " + volume.getId() + " is in " + volume.getState() + " state");
            }

            if (volume.getPoolId() == destPool.getId()) {
                s_logger.debug("volume: " + volume.getId() + " is on the same storage pool: " + destPool.getId());
                continue;
            }

            volumesNeedToMigrate.add(volume);
        }

        if (volumesNeedToMigrate.isEmpty()) {
            s_logger.debug("No volume need to be migrated");
            return true;
        }

        return migrateVolumes(volumesNeedToMigrate, destPool);
    }

    
    @DB
    @Override
    public Volume migrateVolume(Long volumeId, Long storagePoolId) throws ConcurrentOperationException {
        VolumeVO vol = _volumeDao.findById(volumeId);
        if (vol == null) {
            throw new InvalidParameterValueException("Failed to find the volume id: " + volumeId);
        }

        if (vol.getState() != Volume.State.Ready) {
            throw new InvalidParameterValueException("Volume must be in ready state");
        }

        if (vol.getInstanceId() != null) {
            throw new InvalidParameterValueException("Volume needs to be dettached from VM");
        }

        StoragePool destPool = _storagePoolMgr.getStoragePool(storagePoolId);
        if (destPool == null) {
            throw new InvalidParameterValueException("Faild to find the destination storage pool: " + storagePoolId);
        }

        List<Volume> vols = new ArrayList<Volume>();
        vols.add(vol);

        migrateVolumes(vols, destPool);
        return vol;
    }

    @Override
    public void allocateVolume(Long vmId,
    		Pair<? extends DiskOfferingVO, Long> rootDiskOffering,
    		List<Pair<DiskOfferingVO, Long>> dataDiskOfferings, Long templateId, Account owner) {
    	
    	_volumeMgr.allocateVolume(vmId, rootDiskOffering, dataDiskOfferings, templateId, owner);

    }
}
