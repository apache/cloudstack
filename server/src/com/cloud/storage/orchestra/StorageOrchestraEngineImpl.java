package com.cloud.storage.orchestra;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

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
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Grouping;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeHostVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.pool.Storage;
import com.cloud.storage.pool.StoragePool;
import com.cloud.storage.pool.Storage.ImageFormat;
import com.cloud.storage.pool.Storage.StoragePoolType;
import com.cloud.storage.pool.StoragePoolManager;
import com.cloud.storage.snapshot.Snapshot;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.storage.volume.Volume;
import com.cloud.storage.volume.VolumeManagerImpl;
import com.cloud.storage.volume.Volume.Event;
import com.cloud.storage.volume.Volume.Type;
import com.cloud.storage.volume.VolumeManager;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.NumbersUtil;
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
	private static final Logger s_logger = Logger.getLogger(StorageOrchestraEngineImpl.class);
    private int _customDiskOfferingMinSize = 1;
    private int _customDiskOfferingMaxSize = 1024;
    private long _maxVolumeSizeInGb;
    
	@Inject
	protected VMInstanceDao _vmDao;
	@Inject
	protected VolumeDao _volumeDao;
	@Inject
	protected AccountDao _accountDao;
	@Inject
	protected DiskOfferingDao _diskOfferingDao;
	@Inject
	protected DataCenterDao _dcDao;
	@Inject
	protected SnapshotDao _snapshotDao;
	@Inject
	protected StoragePoolDao _storagePoolDao;
	@Inject
	protected UsageEventDao _usageEventDao;
	
	
	
	@Inject
	protected StoragePoolManager _storagePoolMgr;
	@Inject
	protected SnapshotManager _snapshotMgr;
	@Inject
	protected AccountManager _accountMgr;
	@Inject
	protected ConfigurationManager _configMgr;
	@Inject
    protected ResourceLimitService _resourceLimitMgr;
	@Inject
	protected TemplateManager _templateMgr;
	@Inject
	protected VolumeManager _volumeMgr;
	@Inject
	protected ResourceManager _resourceMgr;

	
	@Override
	@DB
	@ActionEvent(eventType = EventTypes.EVENT_VOLUME_CREATE, eventDescription = "creating volume", async = true)
	public VolumeVO createVolume(CreateVolumeCmd cmd) {
		VolumeVO volume = _volumeDao.findById(cmd.getEntityId());

		if (cmd.getSnapshotId() != null) {
			volume = createVolumeFromSnapshot(volume, cmd.getSnapshotId());
			if (volume == null || volume.getState() != Volume.State.Ready) {
				s_logger.trace("Decrementing volume resource count for account id=" + volume.getAccountId() + " as volume failed to create on the backend");
				_resourceLimitMgr.decrementResourceCount(volume.getAccountId(), ResourceType.volume);
			}
		}
		return volume;
	}

	@DB
	protected VolumeVO createVolumeFromSnapshot(VolumeVO volume, long snapshotId) {
		int retry = 0;
		String volumeUUID = null;
		
		try {
			_volumeMgr.processEvent(volume, Volume.Event.CreateRequested);
		} catch (NoTransitionException e) {
			s_logger.debug(e.toString());
			return null;
		}
		
		try {
			String volumeFolder = null;
			Long volumeId = volume.getId();
			Account account = _accountDao.findById(volume.getAccountId());
			final HashSet<StoragePool> poolsToAvoid = new HashSet<StoragePool>();
			StoragePool pool = null;
			VolumeVO createdVolume = null;
			DiskOfferingVO diskOffering = _diskOfferingDao.findByIdIncludingRemoved(volume.getDiskOfferingId());
			DataCenterVO dc = _dcDao.findById(volume.getDataCenterId());
			SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
			DiskProfile dskCh = new DiskProfile(volume, diskOffering, snapshot.getHypervisorType());
			
			while (retry < 3) {
				pool = _storagePoolMgr.findStoragePool(dc, dskCh, account, poolsToAvoid);

				volumeFolder = pool.getPath();
				if (s_logger.isDebugEnabled()) {
					s_logger.debug("Attempting to create volume from snapshotId: " + snapshot.getId() + " on storage pool " + pool.getName() + " retry: " + retry);
				}

				try {
					volumeUUID = _snapshotMgr.createVolumeFromSnapshot(UserContext.current().getCallerUserId(), snapshot, pool);
				} catch (CloudRuntimeException e) {
					s_logger.warn("Unable to create volume on pool " + pool.getName() + ", reason: " + e.toString());
				}

				if (volumeUUID != null) {
					if (s_logger.isDebugEnabled()) {
						s_logger.debug("Volume with UUID " + volumeUUID + " was created on storage pool " + pool.getName());
					}
					break;
				}
				retry++;
			}

			if (volumeUUID != null) {
				createdVolume = _volumeDao.findById(volumeId);
				createdVolume.setPodId(pool.getPodId());
				createdVolume.setPoolId(pool.getId());
				createdVolume.setPoolType(pool.getPoolType());
				createdVolume.setFolder(volumeFolder);
				createdVolume.setPath(volumeUUID);
				createdVolume.setDomainId(account.getDomainId());

				_volumeMgr.processEvent(createdVolume, Volume.Event.OperationSucceeded);
			}
			return createdVolume;
		} catch (NoTransitionException e) {
			s_logger.debug(e.toString());
			return null;
		} catch (CloudRuntimeException e) {
			s_logger.debug(e.toString());
			return null;
		} finally {
			if (volumeUUID == null) {
				try {
					_volumeMgr.processEvent(volume, Volume.Event.OperationFailed);
				} catch (NoTransitionException e) {
					s_logger.debug(e.toString());
				}
				return null;
			}
		}
	}
	
	private boolean validateVolumeSizeRange(long size) {
        if (size < 0 || (size > 0 && size < (1024 * 1024 * 1024))) {
            throw new InvalidParameterValueException("Please specify a size of at least 1 Gb.");
        } else if (size > (_maxVolumeSizeInGb * 1024 * 1024 * 1024)) {
            throw new InvalidParameterValueException("volume size " + size + ", but the maximum size allowed is " + _maxVolumeSizeInGb + " Gb.");
        }

        return true;
    }
	
    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_CREATE, eventDescription = "creating volume", create = true)
    public VolumeVO allocVolume(CreateVolumeCmd cmd) throws ResourceAllocationException {
        Account caller = UserContext.current().getCaller();

        long ownerId = cmd.getEntityOwnerId();

        _accountMgr.checkAccess(caller, null, true, _accountMgr.getActiveAccountById(ownerId));

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
        long domainId = ((caller == null) ? Domain.ROOT_DOMAIN : caller.getDomainId());
        
        VolumeVO volume = _volumeMgr.allocateDiskVolume(cmd.getVolumeName(), zoneId, ownerId, domainId, diskOfferingId, size);
        
        if(cmd.getSnapshotId() == null) {
          	//for volume created from snapshot, create usage event after volume creation
          	UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(), diskOfferingId, null, size);
          	_usageEventDao.persist(usageEvent);
          }

        UserContext.current().setEventDetails("Volume Id: " + volume.getId());

        _resourceLimitMgr.incrementResourceCount(volume.getAccountId(), ResourceType.volume);
        return volume;
    }
    
    @Override
    public void allocateVolume(Long vmId,
    		Pair<? extends DiskOfferingVO, Long> rootDiskOffering,
    		List<Pair<DiskOfferingVO, Long>> dataDiskOfferings, Long templateId, Account owner) {
    	_volumeMgr.allocateVolume(vmId, rootDiskOffering, dataDiskOfferings, templateId, owner);
    }
    
    @Override
    public void prepare(VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, boolean recreate) throws StorageUnavailableException, InsufficientStorageCapacityException {
        if (dest == null) {
            throw new CloudRuntimeException("Unable to prepare Volume for vm because DeployDestination is null, vm:" + vm);
        }
        
        List<VolumeVO> vols = _volumeDao.findUsableVolumesForInstance(vm.getId());
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Prepare " + vols.size() + " volumes for " + vm);
        }

        for (VolumeVO vol : vols) {
        	DiskOfferingVO diskOffering = _diskOfferingDao.findById(vol.getDiskOfferingId());
        	StoragePool destPool = dest.getStorageForDisks().get(vol);
        	VolumeVO created = _volumeMgr.createVolume(vol, vm.getTemplateId(), diskOffering, vm.getHypervisorType(), destPool);
            vm.addDisk(new VolumeTO(created, destPool));
        }
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
    	processEvent(volume, Event.CopyRequested);
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
        	processEvent(volume, Event.CopyFailed);
            throw new CloudRuntimeException("Failed to copy the volume from secondary storage to the destination primary storage pool.");
        }

        if (cvAnswer == null || !cvAnswer.getResult()) {
        	processEvent(volume, Event.CopyFailed);
            throw new CloudRuntimeException("Failed to copy the volume from secondary storage to the destination primary storage pool.");
        }        
        Transaction txn = Transaction.currentTxn();
        txn.start();        
        volume.setPath(cvAnswer.getVolumePath());
        volume.setFolder(destPool.getPath());
        volume.setPodId(destPool.getPodId());
        volume.setPoolId(destPool.getId());        
        volume.setPodId(destPool.getPodId());
        processEvent(volume, Event.CopySucceeded); 
        UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(), volume.getDiskOfferingId(), null, volume.getSize());
        _usageEventDao.persist(usageEvent);
        _volumeHostDao.remove(volumeHostVO.getId());
    	txn.commit();
		return volume;
    	
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
        String _customDiskOfferingMinSizeStr = configDao.getValue(Config.CustomDiskOfferingMinSize.toString());
        _customDiskOfferingMinSize = NumbersUtil.parseInt(_customDiskOfferingMinSizeStr, Integer.parseInt(Config.CustomDiskOfferingMinSize.getDefaultValue()));
        
        String _customDiskOfferingMaxSizeStr = configDao.getValue(Config.CustomDiskOfferingMaxSize.toString());
        _customDiskOfferingMaxSize = NumbersUtil.parseInt(_customDiskOfferingMaxSizeStr, Integer.parseInt(Config.CustomDiskOfferingMaxSize.getDefaultValue()));
        
        String maxVolumeSizeInGbString = configDao.getValue("storage.max.volume.size");
        _maxVolumeSizeInGb = NumbersUtil.parseLong(maxVolumeSizeInGbString, 2000);
		return true;
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

   
}
