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
import java.util.UUID;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.AttachVolumeAnswer;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DeleteVolumeCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.api.BaseCmd;
import com.cloud.api.commands.AttachVolumeCmd;
import com.cloud.api.commands.CreateVolumeCmd;
import com.cloud.api.commands.DetachVolumeCmd;
import com.cloud.api.commands.ListVolumesCmd;
import com.cloud.async.AsyncJobExecutor;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.cluster.CheckPointManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
import com.cloud.offering.DiskOffering;
import com.cloud.org.Grouping;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateSwiftVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeHostVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplateSwiftDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeHostDao;
import com.cloud.storage.image.ImageManager;
import com.cloud.storage.StorageMigrationCleanupMaid;
import com.cloud.storage.pool.Storage;
import com.cloud.storage.pool.Storage.ImageFormat;
import com.cloud.storage.pool.Storage.TemplateType;
import com.cloud.storage.pool.StoragePool;
import com.cloud.storage.pool.StoragePoolManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.storage.volume.Volume.Event;
import com.cloud.storage.volume.Volume.Type;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.UserContext;
import com.cloud.uservm.UserVm;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
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
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate;

public class VolumeManagerImpl implements VolumeManager, Manager {
	private static final Logger s_logger = Logger.getLogger(VolumeManagerImpl.class);
	protected int _retry = 2;
	protected int _copyvolumewait;
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
	protected AccountManager _accountMgr;
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
	@Inject
	protected UserVmDao _userVmDao;
	@Inject
	protected StoragePoolDao _storagePoolDao;
	@Inject
	protected VolumeHostDao _volumeHostDao;
	@Inject
	protected DataCenterDao _dcDao;
	@Inject
	protected HostPodDao _podDao;
	@Inject
	protected UsageEventDao _usageEventDao;
	@Inject
	protected ConfigurationDao _configDao;
	@Inject
	protected ResourceLimitService _resourceLimitMgr;
	@Inject
	protected CheckPointManager _checkPointMgr;
	@Inject
	protected VMTemplateSwiftDao _vmTemplateSwiftDao = null;
	@Inject
	protected HypervisorCapabilitiesDao _hypervisorCapabilitiesDao;

	
	private VolumeVO allocateDuplicateVolume(VolumeVO oldVol, Long templateId) {
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

	@DB
	protected void copyVolumeFromSec(VolumeVO volume, StoragePool pool) throws ConcurrentOperationException, StorageUnavailableException {
		try {
			processEvent(volume, Volume.Event.CopyRequested);
		} catch (NoTransitionException e) {
			throw new ConcurrentOperationException("Unable to request a copy operation on volume " + volume);
		}

		boolean success = false;
		try {
			VolumeHostVO volumeHostVO = _volumeHostDao.findByVolumeId(volume.getId());
			HostVO secStorage = _hostDao.findById(volumeHostVO.getHostId());
			String secondaryStorageURL = secStorage.getStorageUrl();
			String[] volumePath = volumeHostVO.getInstallPath().split("/");
			String volumeUUID = volumePath[volumePath.length - 1].split("\\.")[0];

			CopyVolumeCommand cvCmd = new CopyVolumeCommand(volume.getId(), volumeUUID, pool, secondaryStorageURL, false, _copyvolumewait);
			CopyVolumeAnswer cvAnswer;

			cvAnswer = (CopyVolumeAnswer) _storagePoolMgr.sendToPool(pool, cvCmd);
			boolean result = (cvAnswer != null && cvAnswer.getResult()) ? true : false;
			if (result) {
				Transaction txn = Transaction.currentTxn();
				txn.start();
				volume.setPath(cvAnswer.getVolumePath());
				volume.setFolder(pool.getPath());
				volume.setPodId(pool.getPodId());
				volume.setPoolId(pool.getId());
				volume.setPodId(pool.getPodId());

				processEvent(volume, Event.CopySucceeded);

				UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(),
						volume.getDiskOfferingId(), null, volume.getSize());
				_usageEventDao.persist(usageEvent);
				_volumeHostDao.remove(volumeHostVO.getId());
				txn.commit();
			}
			success = result;
		} catch (StorageUnavailableException e) {
			throw e;
		} catch (NoTransitionException e) {
			throw new ConcurrentOperationException("Failed to copy volume: " + volume);
		} finally {
			if (!success) {
				try {
					processEvent(volume, Event.CopyFailed);
				} catch (NoTransitionException e) {
					s_logger.debug("Failed to change volume: " + volume + " state: " + e.toString());
				}
			}
		}
	}

	@Override
	@DB
	public VolumeVO createVolume(VolumeVO volume, long VMTemplateId, DiskOfferingVO diskOffering, HypervisorType hyperType, StoragePool assignedPool) throws StorageUnavailableException,
			ConcurrentOperationException {
		Long existingPoolId = null;
		existingPoolId = volume.getPoolId();

		if (existingPoolId == null && assignedPool == null) {
			throw new CloudRuntimeException("Volume has no pool associate and also no storage pool assigned in DeployDestination, Unable to create.");
		}

		if (assignedPool == null) {
			if (s_logger.isDebugEnabled()) {
				s_logger.debug("No need to recreate the volume: " + volume + ", since it already has a pool assigned: " + volume.getPoolId() + ", adding disk to VM");
			}
			return volume;
		}

		boolean needToCreateVolume = false;
		boolean needToRecreateVolume = false;
		boolean needToMigrateVolume = false;
		boolean needToCopyFromSec = false;
		Volume.State state = volume.getState();
		if (state == Volume.State.Allocated || state == Volume.State.Creating) {
			needToCreateVolume = true;
		} else if (state == Volume.State.UploadOp) {
			needToCopyFromSec = true;
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
					throw new CloudRuntimeException("Can't recreate volume: " + volume + " on local storage");
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
			List<Volume> volumesToMigrate = new ArrayList<Volume>();
			volumesToMigrate.add(volume);
			migrateVolumes(volumesToMigrate, assignedPool);

		} else if (needToCopyFromSec) {
			copyVolumeFromSec(volume, assignedPool);
		}

		newVol = _volumeDao.findById(volume.getId());
		return newVol;
	}

	/**
	 * Give a volume in allocated state, created on specified storage
	 * 
	 * @param toBeCreated
	 * @param offering
	 * @param hypervisorType
	 * @param sPool
	 * @return
	 * @throws StorageUnavailableException
	 */
	private VolumeVO createVolume(VolumeVO toBeCreated, DiskOfferingVO offering, HypervisorType hypervisorType, StoragePool sPool) throws StorageUnavailableException, ConcurrentOperationException {
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
			throw new ConcurrentOperationException("Unable to request a create operation on volume " + toBeCreated);
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

				processEvent(toBeCreated, Volume.Event.OperationSucceeded);
				createdSuccessed = true;
			}
			return toBeCreated;
		} catch (NoTransitionException e) {
			throw new ConcurrentOperationException("Unable to update volume state: " + e.toString());
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

	@Override
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
				String secondaryStorageURL = _storagePoolMgr.getSecondaryStorageURL(volume.getDataCenterId());
				StoragePoolVO srcPool = _storagePoolDao.findById(volume.getPoolId());
				CopyVolumeCommand cvCmd = new CopyVolumeCommand(volume.getId(), volume.getPath(), srcPool, secondaryStorageURL, true, _copyvolumewait);
				CopyVolumeAnswer cvAnswer;
				try {
					cvAnswer = (CopyVolumeAnswer) _storagePoolMgr.sendToPool(srcPool, cvCmd);
				} catch (StorageUnavailableException e1) {
					throw new CloudRuntimeException("Failed to copy the volume from the source primary storage pool to secondary storage.", e1);
				}

				if (cvAnswer == null || !cvAnswer.getResult()) {
					throw new CloudRuntimeException("Failed to copy the volume from the source primary storage pool to secondary storage.");
				}

				String secondaryStorageVolumePath = cvAnswer.getVolumePath();

				// Copy the volume from secondary storage to the destination
				// storage
				// pool
				cvCmd = new CopyVolumeCommand(volume.getId(), secondaryStorageVolumePath, destPool, secondaryStorageURL, false, _copyvolumewait);
				try {
					cvAnswer = (CopyVolumeAnswer) _storagePoolMgr.sendToPool(destPool, cvCmd);
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
				// Need a transaction, make sure all the volumes get migrated to
				// new storage pool
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

		// all the volumes get migrated to new storage pool, need to delete the
		// copy on old storage pool
		for (Pair<StoragePoolVO, DestroyCommand> cmd : destroyCmds) {
			try {
				Answer cvAnswer = _storagePoolMgr.sendToPool(cmd.first(), cmd.second());
			} catch (StorageUnavailableException e) {
				s_logger.debug("Unable to delete the old copy on storage pool: " + e.toString());
			}
		}
		return true;
	}

	private boolean volumeInactive(VolumeVO volume) {
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
	
    private String getVmNameOnVolume(VolumeVO volume) {
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

	private boolean volumeOnSharedStoragePool(VolumeVO volume) {
		boolean result = false;
		if (volume.getState() != Volume.State.Ready) {
			// if it's not ready, we don't care
			result = true;
		}

		Long poolId = volume.getPoolId();
		if (poolId == null) {
			result = false;
		} else {
			StoragePoolVO pool = _storagePoolDao.findById(poolId);

			if (pool == null) {
				result = false;
			} else {
				result = pool.isShared();
			}
		}

		if (!result) {
			throw new InvalidParameterValueException("Please specify a volume that has been created on a shared storage pool.");
		}

		return result;
	}

	public String getRandomVolumeName() {
		return UUID.randomUUID().toString();
	}

	public boolean validateVolume(Account caller, long ownerId, Long zoneId, String volumeName, String url, String format) throws ResourceAllocationException {

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
		if ((!url.toLowerCase().endsWith("vhd")) && (!url.toLowerCase().endsWith("vhd.zip")) && (!url.toLowerCase().endsWith("vhd.bz2")) && (!url.toLowerCase().endsWith("vhd.gz"))
				&& (!url.toLowerCase().endsWith("qcow2")) && (!url.toLowerCase().endsWith("qcow2.zip")) && (!url.toLowerCase().endsWith("qcow2.bz2")) && (!url.toLowerCase().endsWith("qcow2.gz"))
				&& (!url.toLowerCase().endsWith("ova")) && (!url.toLowerCase().endsWith("ova.zip")) && (!url.toLowerCase().endsWith("ova.bz2")) && (!url.toLowerCase().endsWith("ova.gz"))
				&& (!url.toLowerCase().endsWith("img")) && (!url.toLowerCase().endsWith("raw"))) {
			throw new InvalidParameterValueException("Please specify a valid " + format.toLowerCase());
		}

		if ((format.equalsIgnoreCase("vhd") && (!url.toLowerCase().endsWith(".vhd") && !url.toLowerCase().endsWith("vhd.zip") && !url.toLowerCase().endsWith("vhd.bz2") && !url.toLowerCase().endsWith(
				"vhd.gz")))
				|| (format.equalsIgnoreCase("qcow2") && (!url.toLowerCase().endsWith(".qcow2") && !url.toLowerCase().endsWith("qcow2.zip") && !url.toLowerCase().endsWith("qcow2.bz2") && !url
						.toLowerCase().endsWith("qcow2.gz")))
				|| (format.equalsIgnoreCase("ova") && (!url.toLowerCase().endsWith(".ova") && !url.toLowerCase().endsWith("ova.zip") && !url.toLowerCase().endsWith("ova.bz2") && !url.toLowerCase()
						.endsWith("ova.gz"))) || (format.equalsIgnoreCase("raw") && (!url.toLowerCase().endsWith(".img") && !url.toLowerCase().endsWith("raw")))) {
			throw new InvalidParameterValueException("Please specify a valid URL. URL:" + url + " is an invalid for the format " + format.toLowerCase());
		}
		validateUrl(url);

		return false;
	}

	private String validateUrl(String url) {
		try {
			URI uri = new URI(url);
			if ((uri.getScheme() == null) || (!uri.getScheme().equalsIgnoreCase("http") && !uri.getScheme().equalsIgnoreCase("https") && !uri.getScheme().equalsIgnoreCase("file"))) {
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
		// volume.setSize(size);
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

		// Increment resource count during allocation; if actual creation fails,
		// decrement it
		_resourceLimitMgr.incrementResourceCount(volume.getAccountId(), ResourceType.volume);

		txn.commit();
		return volume;
	}

	@Override
	@DB
	public boolean deleteVolume(VolumeVO volume) throws ConcurrentOperationException {
		if (volume.getInstanceId() != null) {
			throw new InvalidParameterValueException("Please detach the volume at first, then delete it.");
		}
		
		if (!_snapshotMgr.canOperateOnVolume(volume)) {
			throw new InvalidParameterValueException("There are snapshot creating on it, Unable to delete the volume");
		}

		if (volume.getState() == Volume.State.UploadOp) {
			VolumeHostVO volumeHost = _volumeHostDao.findByVolumeId(volume.getId());
			if (volumeHost.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS) {
				throw new InvalidParameterValueException("Please specify a volume that is not uploading");
			}
		}

		if (!Volume.State.Destroy.equals(volume.getState())) {
			try {
				processEvent(volume, Volume.Event.DestroyRequested);

				_snapshotMgr.deletePoliciesForVolume(volume.getId());
				Long instanceId = volume.getInstanceId();
				VMInstanceVO vmInstance = null;
				if (instanceId != null) {
					vmInstance = _vmInstanceDao.findById(instanceId);
				}

				if (instanceId == null || (vmInstance.getType().equals(VirtualMachine.Type.User))) {
					_resourceLimitMgr.decrementResourceCount(volume.getAccountId(), ResourceType.volume);
					UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_DELETE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName());
					_usageEventDao.persist(usageEvent);
				}
			} catch (NoTransitionException e) {
				throw new ConcurrentOperationException(e.toString());
			}
		}
		
		return expungeVolume(volume, false);
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

	private DiskProfile toDiskProfile(VolumeVO vol, DiskOffering offering) {
		return new DiskProfile(vol.getId(), vol.getVolumeType(), vol.getName(), offering.getId(), vol.getSize(), offering.getTagsArray(), offering.getUseLocalStorage(), offering.isRecreatable(),
				vol.getTemplateId());
	}

	@Override
	public DiskProfile allocateVolume(Long vmId, Pair<? extends DiskOffering, Long> rootDiskOffering, List<Pair<DiskOffering, Long>> dataDiskOfferings, Long templateId, Account owner) {
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

		for (Pair<DiskOffering, Long> offering : dataDiskOfferings) {
			diskProfile = allocateRawVolume(Type.DATADISK, "DATA-" + vm.getId(), offering.first(), offering.second(), vm, owner);
		}
		return diskProfile;
	}

	public DiskProfile allocateRawVolume(Type type, String name, DiskOffering offering, Long size, VirtualMachine vm, Account owner) {
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

	public DiskProfile allocateTemplatedVolume(Type type, String name, DiskOffering offering, VirtualMachineTemplate template, VirtualMachine vm, Account owner) {
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

	private void deleteVolumeFromSec(VolumeVO vol) {
		VolumeHostVO volumeHost = _volHostDao.findByVolumeId(vol.getId());
		if (volumeHost != null) {
			if (volumeHost.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
				HostVO ssHost = _hostDao.findById(volumeHost.getHostId());
				DeleteVolumeCommand dtCommand = new DeleteVolumeCommand(ssHost.getStorageUrl(), volumeHost.getInstallPath());
				Answer answer = _agentMgr.sendToSecStorage(ssHost, dtCommand);
				if (answer == null || !answer.getResult()) {
					s_logger.debug("Failed to delete " + volumeHost + " due to " + ((answer == null) ? "answer is null" : answer.getDetails()));
					return;
				}
				_volHostDao.remove(volumeHost.getId());
			} else if (volumeHost.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS) {
				s_logger.debug("Volume: " + vol.getName() + " is currently being uploaded; cant' delete it.");
				throw new CloudRuntimeException("Please specify a volume that is not currently being uploaded.");
			}
		}
	}
	
	private boolean destroyVolumeOnBackend(VolumeVO volume, StoragePool pool, String vmName) throws StorageUnavailableException {
		boolean removeVolume = false;
		
		deleteVolumeFromSec(volume);
		
		DestroyCommand cmd = new DestroyCommand(pool, volume, vmName);
		Answer answer = _storagePoolMgr.sendToPool(pool, cmd);
		if (answer != null && answer.getResult()) {
			removeVolume = true;
		} else if (answer != null && !answer.getResult()){
			throw new CloudRuntimeException(answer.getDetails());
		}
		return removeVolume;
	}

	@DB
	protected boolean expungeVolume(VolumeVO vol, boolean force) {
		if (s_logger.isDebugEnabled()) {
			s_logger.debug("Expunging " + vol);
		}

		if (!Volume.State.Destroy.equals(vol.getState())) {
			return false;
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
			return true;
		}

		StoragePoolVO pool = (StoragePoolVO) _storagePoolMgr.getStoragePool(poolId);
		if (pool == null) {
			s_logger.debug("Removing volume as storage pool is gone: " + poolId);
			_volumeDao.remove(vol.getId());
			return true;
		}
	
		boolean removeVolume = false;
		try {
			removeVolume = destroyVolumeOnBackend(vol, pool, vmName);
		} catch (StorageUnavailableException e) {
			s_logger.info("Storage is unavailable currently.  Will retry delete of " + vol + " from " + poolId);
		} catch (RuntimeException ex) {
			s_logger.info("Failed to expunge volume, but marking volume id=" + vol.getId() + ", due to ", ex);
		} finally {
			if (removeVolume || force) {
				if (force) {
					s_logger.debug("Force to expunge volume: " + vol);
				}
				
				_volumeDao.remove(vol.getId());
				removeVolume = true;
				if (s_logger.isDebugEnabled()) {
					s_logger.debug("Volume successfully expunged from " + poolId);
				}
			}
		}
		
		return removeVolume;
	}

	@Override
	@DB
	public void cleanupVolumes(long vmId) throws ConcurrentOperationException {
		if (s_logger.isDebugEnabled()) {
			s_logger.debug("Cleaning storage for vm: " + vmId);
		}
		List<VolumeVO> volumesForVm = _volumeDao.findByInstance(vmId);
		List<VolumeVO> toBeExpunged = new ArrayList<VolumeVO>();

		for (VolumeVO vol : volumesForVm) {
			if (vol.getVolumeType().equals(Type.ROOT)) {
				toBeExpunged.add(vol);
			} else {
				if (s_logger.isDebugEnabled()) {
					s_logger.debug("Detaching " + vol);
				}
				_volumeDao.detachVolume(vol.getId());
			}
		}

		for (VolumeVO expunge : toBeExpunged) {
			deleteVolume(expunge);
		}
	}

	@Override
	public List<VolumeVO> searchForVolumes(ListVolumesCmd cmd) {
		// TODO: remove ACL code
		Account caller = UserContext.current().getCaller();
		List<Long> permittedAccounts = new ArrayList<Long>();

		Long id = cmd.getId();
		Long vmInstanceId = cmd.getVirtualMachineId();
		String name = cmd.getVolumeName();
		String keyword = cmd.getKeyword();
		String type = cmd.getType();
		Map<String, String> tags = cmd.getTags();

		Long zoneId = cmd.getZoneId();
		Long podId = null;

		if (_accountMgr.isAdmin(caller.getType())) {
			podId = cmd.getPodId();
		}

		Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
		_accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
		Long domainId = domainIdRecursiveListProject.first();
		Boolean isRecursive = domainIdRecursiveListProject.second();
		ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

		Filter searchFilter = new Filter(VolumeVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());

		// hack for now, this should be done better but due to needing a join I
		// opted to
		// do this quickly and worry about making it pretty later
		SearchBuilder<VolumeVO> sb = _volumeDao.createSearchBuilder();
		_accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

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
			for (int count = 0; count < tags.size(); count++) {
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
		_accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

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
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		HostTemplateStatesSearch = _vmTemplateHostDao.createSearchBuilder();
		HostTemplateStatesSearch.and("id", HostTemplateStatesSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
		HostTemplateStatesSearch.and("state", HostTemplateStatesSearch.entity().getDownloadState(), SearchCriteria.Op.EQ);

		SearchBuilder<HostVO> HostSearch = _hostDao.createSearchBuilder();
		HostSearch.and("dcId", HostSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);

		HostTemplateStatesSearch.join("host", HostSearch, HostSearch.entity().getId(), HostTemplateStatesSearch.entity().getHostId(), JoinBuilder.JoinType.INNER);
		HostSearch.done();
		HostTemplateStatesSearch.done();

		String value = _configDao.getValue(Config.CopyVolumeWait.toString());
		_copyvolumewait = NumbersUtil.parseInt(value, Integer.parseInt(Config.CopyVolumeWait.getDefaultValue()));
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

	private int getMaxDataVolumesSupported(UserVmVO vm) {
		Long hostId = vm.getHostId();
		if (hostId == null) {
			hostId = vm.getLastHostId();
		}
		HostVO host = _hostDao.findById(hostId);
		Integer maxDataVolumesSupported = null;
		if (host != null) {
			_hostDao.loadDetails(host);
			maxDataVolumesSupported = _hypervisorCapabilitiesDao.getMaxDataVolumesLimit(host.getHypervisorType(), host.getDetail("product_version"));
		}
		if (maxDataVolumesSupported == null) {
			maxDataVolumesSupported = 6; // 6 data disks by default if nothing
											// is specified in
											// 'hypervisor_capabilities' table
		}

		return maxDataVolumesSupported.intValue();
	}

	private boolean reachMaxDataDisks(VolumeVO volume, UserVmVO vm) {
		List<VolumeVO> existingDataVolumes = _volumeDao.findByInstanceAndType(vm.getId(), Volume.Type.DATADISK);
		int maxDataVolumesSupported = getMaxDataVolumesSupported(vm);
		if (existingDataVolumes.size() >= maxDataVolumesSupported) {
			throw new InvalidParameterValueException("The specified VM already has the maximum number of data disks (" + maxDataVolumesSupported + "). Please specify another VM.");
		}
		return true;
	}

	private boolean isVolumeHypevisorMatchVm(VolumeVO volume, UserVmVO vm) {
		HypervisorType dataDiskHyperType = _volumeDao.getHypervisorType(volume.getId());
		if (dataDiskHyperType != HypervisorType.None && vm.getHypervisorType() != dataDiskHyperType) {
			throw new InvalidParameterValueException("Can't attach a volume created by: " + dataDiskHyperType + " to a " + vm.getHypervisorType() + " vm");
		}
		return true;
	}

	private boolean isVolumeReadyToAttach(VolumeVO volume) {
		if (!volume.isAttachedToVm()) {
			throw new InvalidParameterValueException("Please specify a volume that is not attached to any VM.");
		}

		if (!(Volume.State.Allocated.equals(volume.getState()) || Volume.State.Ready.equals(volume.getState()) || Volume.State.UploadOp.equals(volume.getState()))) {
			throw new InvalidParameterValueException("Volume state must be in Allocated, Ready or in Uploaded state");
		}

		if (Volume.State.UploadOp.equals(volume.getState())) {
			VolumeHostVO volHostVO = _volumeHostDao.findByVolumeId(volume.getId());
			if (volHostVO != null) {
				if (!(Status.DOWNLOADED.equals(volHostVO.getDownloadState()))) {
					throw new InvalidParameterValueException("Volume is not uploaded yet. Please try this operation once the volume is uploaded");
				}
			}
		}
		return true;
	}

	private VolumeVO prepareDataDisk(VolumeVO volume, UserVmVO vm, Long clusterId) throws ConcurrentOperationException, StorageUnavailableException {
		DataCenterVO dc = _dcDao.findById(vm.getDataCenterIdToDeployIn());
		HostPodVO pod = _podDao.findById(vm.getPodIdToDeployIn());
		DiskOfferingVO diskVO = _diskOfferingDao.findById(volume.getDiskOfferingId());
		DiskProfile diskProfile = new DiskProfile(volume, diskVO, vm.getHypervisorType());

		StoragePoolVO destStoragePool = _storagePoolMgr.findStoragePool(diskProfile, dc, pod, clusterId, (VMInstanceVO) vm, new HashSet<StoragePool>());
		if (destStoragePool == null) {
			throw new CloudRuntimeException("No available storage in dc: " + dc.getId() + ", pod: " + pod.getId() + ", cluster " + clusterId);
		}

		return createVolume(volume, vm.getTemplateId(), diskVO, vm.getHypervisorType(), destStoragePool);
	}

	private long getDeviceId(VolumeVO volume, UserVmVO vm, Long assignedId) {
		// allocate deviceId
		List<VolumeVO> vols = _volumeDao.findByInstance(vm.getId());
		if (assignedId != null) {
			if (assignedId.longValue() > 15 || assignedId.longValue() == 0 || assignedId.longValue() == 3) {
				throw new InvalidParameterValueException("deviceId should be 1,2,4-15");
			}
			for (VolumeVO vol : vols) {
				if (vol.getDeviceId().equals(assignedId)) {
					throw new InvalidParameterValueException("deviceId " + assignedId + " is used by VM " + vm.getHostName());
				}
			}
		} else {
			// allocate deviceId here
			List<String> devIds = new ArrayList<String>();
			for (int i = 1; i < 15; i++) {
				devIds.add(String.valueOf(i));
			}
			devIds.remove("3");
			for (VolumeVO vol : vols) {
				devIds.remove(vol.getDeviceId().toString().trim());
			}
			assignedId = Long.parseLong(devIds.iterator().next());
		}
		return assignedId.longValue();
	}

	private VolumeVO sendAttachCmd(UserVmVO vm, VolumeVO volume, Long devId) throws AgentUnavailableException, OperationTimedoutException {
		String errorMsg = "Failed to attach volume: " + volume.getName() + " to VM: " + vm.getHostName();
		boolean sendCommand = (vm.getState() == State.Running);
		AttachVolumeAnswer answer = null;
		Long hostId = vm.getHostId();
		if (hostId == null) {
			hostId = vm.getLastHostId();
			HostVO host = _hostDao.findById(hostId);
			if (host != null && host.getHypervisorType() == HypervisorType.VMware) {
				sendCommand = true;
			}
		}

		if (!sendCommand) {
			_volumeDao.attachVolume(volume.getId(), vm.getId(), devId);
			return _volumeDao.findById(volume.getId());
		}

		StoragePoolVO volumePool = _storagePoolDao.findById(volume.getPoolId());
		AttachVolumeCommand cmd = new AttachVolumeCommand(true, vm.getInstanceName(), volume.getPoolType(), volume.getFolder(), volume.getPath(), volume.getName(), devId, volume.getChainInfo());
		cmd.setPoolUuid(volumePool.getUuid());

		answer = (AttachVolumeAnswer) _agentMgr.send(hostId, cmd);

		if (answer != null && answer.getResult()) {
			_volumeDao.attachVolume(volume.getId(), vm.getId(), answer.getDeviceId());
			return _volumeDao.findById(volume.getId());
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
	public Volume attachVolumeToVM(VolumeVO volume, UserVmVO vm, Long deviceId) throws StorageUnavailableException, ConcurrentOperationException, AgentUnavailableException, OperationTimedoutException {
		if (vm.getDataCenterIdToDeployIn() != volume.getDataCenterId()) {
			throw new InvalidParameterValueException("Please specify a VM that is in the same zone as the volume.");
		}

		if (vm.getState() != State.Running && vm.getState() != State.Stopped) {
			throw new InvalidParameterValueException("Please specify a VM that is either running or stopped.");
		}

		long devId = getDeviceId(volume, vm, deviceId);

		isVolumeReadyToAttach(volume);

		reachMaxDataDisks(volume, vm);

		isVolumeHypevisorMatchVm(volume, vm);

		volumeOnSharedStoragePool(volume);

		VolumeVO rootVolumeOfVm = null;
		List<VolumeVO> rootVolumesOfVm = _volumeDao.findByInstanceAndType(vm.getId(), Volume.Type.ROOT);

		rootVolumeOfVm = rootVolumesOfVm.get(0);

		if (rootVolumeOfVm.getState() == Volume.State.Allocated) {
			return volume;
		}
		StoragePoolVO rootDiskPool = _storagePoolDao.findById(rootVolumeOfVm.getPoolId());
		Long clusterId = rootDiskPool.getClusterId();

		volume = prepareDataDisk(volume, vm, clusterId);

		volume = sendAttachCmd(vm, volume, devId);
		return volume;
	}
	
	@Override
	public void detachISOToVM(UserVmVO vm) {
		State vmState = vm.getState();
		if (vmState != State.Running && vmState != State.Stopped) {
			throw new InvalidParameterValueException("Please specify a VM that is either Stopped or Running.");
		}
		
		long isoId = vm.getIsoId();
		VMTemplateVO iso = _templateDao.findById(isoId);
		if (iso == null) {
			throw new CloudRuntimeException("The iso " + isoId + " doesn't exist");
		}

		boolean success = true;
		if (vmState == State.Running) {
			Map<String, String> paths = _templateMgr.getAbsoluteIsoPath(iso, vm.getDataCenterIdToDeployIn());
			HostVO host = _hostDao.findById(vm.getHostId());
			if (host == null) {
				s_logger.warn("Host: " + vm.getHostId() + " does not exist");
				throw new CloudRuntimeException("host " + vm.getHostId() + " does not exist");
			}

			String vmName = vm.getInstanceName();

			AttachIsoCommand cmd = new AttachIsoCommand(vmName, paths.get("isoPath"), false);
			cmd.setStoreUrl(paths.get("secPath"));

			Answer answ = _agentMgr.easySend(vm.getHostId(), cmd);

			success =  (answ != null && answ.getResult()) ? true : false;
		}
		
		if (success) {
			vm.setIsoId(null);
			_userVmDao.update(vm.getId(), vm);
		}    
	}

	@Override
	public void attachISOToVm(UserVmVO vm, VMTemplateVO iso) {
		if (!_templateMgr.isTemplateReady(iso, vm.getDataCenterIdToDeployIn())) {
			throw new InvalidParameterValueException("Please specify an ISO that is Ready state in zone: " + vm.getDataCenterIdToDeployIn());
		}
		
		State vmState = vm.getState();
		if (vmState != State.Running && vmState != State.Stopped) {
			throw new InvalidParameterValueException("Please specify a VM that is either Stopped or Running.");
		}

		if ("xen-pv-drv-iso".equals(iso.getDisplayText()) && vm.getHypervisorType() != Hypervisor.HypervisorType.XenServer){
			throw new InvalidParameterValueException("Cannot attach Xenserver PV drivers to incompatible hypervisor " + vm.getHypervisorType());
		}

		if("vmware-tools.iso".equals(iso.getName()) && vm.getHypervisorType() != Hypervisor.HypervisorType.VMware) {
			throw new InvalidParameterValueException("Cannot attach VMware tools drivers to incompatible hypervisor " + vm.getHypervisorType());
		}

		Map<String, String> paths = _templateMgr.getAbsoluteIsoPath(iso, vm.getDataCenterIdToDeployIn());

		HostVO host = _hostDao.findById(vm.getHostId());
		if (host == null) {
			s_logger.warn("Host: " + vm.getHostId() + " does not exist");
			throw new CloudRuntimeException("host " + vm.getHostId() + " does not exist");
		}

		String vmName = vm.getInstanceName();

		AttachIsoCommand cmd = new AttachIsoCommand(vmName, paths.get("isoPath"), true);
		cmd.setStoreUrl(paths.get("secPath"));

		Answer answ = _agentMgr.easySend(vm.getHostId(), cmd);

		boolean success =  (answ != null && answ.getResult()) ? true : false;
		if (success) {
			vm.setIsoId(iso.getId());
			_userVmDao.update(vm.getId(), vm);
		} else {
			if (answ != null) {
				throw new CloudRuntimeException("Failed to attach iso: " + iso.getId() + " to vm: " + vm.getDisplayName() + ": "+ answ.getDetails());
			}
		}
	}
	
	@Override
    public Volume detachVolumeFromVM(VolumeVO volume, UserVmVO vm) {
        if (volume.getVolumeType() != Volume.Type.DATADISK) {
            throw new InvalidParameterValueException("Please specify a data volume.");
        }

        if (vm.getState() != State.Running && vm.getState() != State.Stopped && vm.getState() != State.Destroyed) {
            throw new InvalidParameterValueException("Please specify a VM that is either running or stopped.");
        }
        
        boolean sendCommand = (vm.getState() == State.Running);
        boolean success = false;
        
        try {
        	if (sendCommand) {
        		Answer answer = null;
        		AttachVolumeCommand cmd = new AttachVolumeCommand(false, vm.getInstanceName(), volume.getPoolType(), volume.getFolder(), volume.getPath(), volume.getName(),
        				volume.getDeviceId(), volume.getChainInfo());

        		StoragePoolVO volumePool = _storagePoolDao.findById(volume.getPoolId());
        		cmd.setPoolUuid(volumePool.getUuid());
        		answer = _agentMgr.send(vm.getHostId(), cmd);
        		success = (answer != null && answer.getResult()) ? true : false;
        	} else {
        		success = true;
        	}

        	if (success) {
        		_volumeDao.detachVolume(volume.getId());
        	}
        	return _volumeDao.findById(volume.getId());
        } catch (AgentUnavailableException e) {
        	throw new CloudRuntimeException(e.toString());
        } catch (OperationTimedoutException e) {
        	throw new CloudRuntimeException(e.toString());
        }
    }
	
	@Override
	public void release(VolumeVO volume) {
		
	}
	
	@Override
	public void recreateVolume(VolumeVO volume, long vmId) throws ConcurrentOperationException {
		VolumeVO newVol = allocateDuplicateVolume(volume, null);
		_volumeDao.attachVolume(newVol.getId(), vmId, newVol.getDeviceId());
		_volumeDao.detachVolume(volume.getId());
		deleteVolume(volume);
	}
}
