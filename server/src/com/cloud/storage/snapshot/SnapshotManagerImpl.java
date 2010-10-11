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

package com.cloud.storage.snapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BackupSnapshotAnswer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotAnswer;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.DeleteSnapshotBackupCommand;
import com.cloud.agent.api.DeleteSnapshotsDirCommand;
import com.cloud.agent.api.ManageSnapshotAnswer;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.agent.api.ValidateSnapshotAnswer;
import com.cloud.agent.api.ValidateSnapshotCommand;
import com.cloud.api.BaseCmd;
import com.cloud.api.commands.CreateSnapshotCmd;
import com.cloud.api.commands.CreateVolumeCmd;
import com.cloud.async.AsyncInstanceCreateStatus;
import com.cloud.async.AsyncJobExecutor;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.async.executor.SnapshotOperationParam;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.EventState;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.DetailsDao;
import com.cloud.host.dao.HostDao;
import com.cloud.serializer.GsonHelper;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotPolicyRefVO;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotScheduleVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Snapshot.SnapshotType;
import com.cloud.storage.Snapshot.Status;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Volume.MirrorState;
import com.cloud.storage.Volume.StorageResourceType;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.SnapshotPolicyRefDao;
import com.cloud.storage.dao.SnapshotScheduleDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.UserVmDao;
import com.google.gson.Gson;

@Local(value={SnapshotManager.class})
public class SnapshotManagerImpl implements SnapshotManager {
    private static final Logger s_logger = Logger.getLogger(SnapshotManagerImpl.class);

    @Inject protected HostDao _hostDao;
    @Inject protected UserVmDao _vmDao;
    @Inject protected VolumeDao _volsDao;
    @Inject protected AccountDao _accountDao;
    @Inject protected DataCenterDao _dcDao;
    @Inject protected DiskOfferingDao _diskOfferingDao;
    @Inject protected UserDao _userDao;
    @Inject protected SnapshotDao _snapshotDao;
    @Inject protected StoragePoolDao _storagePoolDao;
    @Inject protected EventDao _eventDao;
    @Inject protected SnapshotPolicyDao _snapshotPolicyDao =  null;
    @Inject protected SnapshotPolicyRefDao _snapPolicyRefDao =  null;
    @Inject protected SnapshotScheduleDao _snapshotScheduleDao;
    @Inject protected DetailsDao _detailsDao;
    @Inject protected VMTemplateDao _templateDao;
    @Inject protected VMTemplatePoolDao _templatePoolDao;
    @Inject protected VMTemplateHostDao _templateHostDao;
    
    @Inject protected StorageManager _storageMgr;
    @Inject protected AgentManager _agentMgr;
    @Inject protected SnapshotScheduler _snapSchedMgr;
    @Inject protected AsyncJobManager _asyncMgr;
    @Inject protected AccountManager _accountMgr;
    String _name;
    private int _totalRetries;
    private int _pauseInterval;
    
    protected SearchBuilder<SnapshotVO> PolicySnapshotSearch;
    protected SearchBuilder<SnapshotPolicyVO> PoliciesForSnapSearch;

    private final boolean _shouldBeSnapshotCapable = true; // all methods here should be snapshot capable.

    @Override @DB
    public long createSnapshotAsync(long userId, long volumeId, List<Long> policies) {
        VolumeVO volume = _volsDao.findById(volumeId);
        SnapshotOperationParam param = new SnapshotOperationParam(userId, volume.getAccountId(), volumeId, policies);
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(userId);
        job.setAccountId(volume.getAccountId());
        job.setCmd("CreateSnapshot");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(CreateSnapshotCmd.getResultObjectName());
        
        return _asyncMgr.submitAsyncJob(job, true);
    }
    
    private boolean isVolumeDirty(long volumeId, List<Long> policies) {
        VolumeVO volume = _volsDao.findById(volumeId);
        boolean runSnap = true;
        
        if (volume.getInstanceId() == null) {
            long lastSnapId = _snapshotDao.getLastSnapshot(volumeId, 0);
            SnapshotVO lastSnap = _snapshotDao.findById(lastSnapId);
            if (lastSnap != null) {
                Date lastSnapTime = lastSnap.getCreated();
                if (lastSnapTime.after(volume.getUpdated())){
                    runSnap = false;
                    s_logger.debug("Volume: "+ volumeId +" is detached and last snap time is after Volume detach time. Skip snapshot for recurring policy");
                }
            }
        } else if (_storageMgr.volumeInactive(volume)) {
            // Volume is attached to a VM which is in Stopped state.
            long lastSnapId = _snapshotDao.getLastSnapshot(volumeId, 0);
            SnapshotVO lastSnap = _snapshotDao.findById(lastSnapId);
            if (lastSnap != null) {
                Date lastSnapTime = lastSnap.getCreated();
                VMInstanceVO vmInstance = _vmDao.findById(volume.getInstanceId());
                if (vmInstance !=null ) {
                    if (lastSnapTime.after(vmInstance.getUpdateTime())){
                        runSnap = false;
                        s_logger.debug("Volume: "+ volumeId +" is inactive and last snap time is after VM update time. Skip snapshot for recurring policy");
                    }
                }
            }
        }
        
        if (!runSnap){
            if (policies.contains(Snapshot.MANUAL_POLICY_ID)) {
                // Take a snapshot, but only for the manual policy
                policies = new ArrayList<Long>();
                policies.add(Snapshot.MANUAL_POLICY_ID);
                runSnap = true;
                s_logger.debug("Volume: "+ volumeId +" is detached/inactive. Executing snapshot for manual policy");
            }
        }
        if (volume.getDestroyed() || volume.getRemoved() != null) {
            s_logger.debug("Volume: " + volumeId + " is destroyed/removed. Not taking snapshot");
            runSnap = false;
        }
        
        return runSnap;
    }
    
    @Override
    public ImageFormat getImageFormat(Long volumeId) {
        ImageFormat format = null;
        VolumeVO volume = _volsDao.findById(volumeId);
        Long templateId = volume.getTemplateId();
        if (templateId != null) {
            VMTemplateVO template = _templateDao.findById(templateId);
            format = template.getFormat();
        }
        return format;
    }
    
    private boolean shouldRunSnapshot(long userId, VolumeVO volume, List<Long> policies) throws InvalidParameterValueException, ResourceAllocationException {
        boolean runSnap = isVolumeDirty(volume.getId(), policies);
        
        ImageFormat format = getImageFormat(volume.getId());
        if (format != null) {
            if (!(format == ImageFormat.VHD || format == ImageFormat.ISO)) {
                // We only create snapshots for root disks created from templates or ISOs.
                s_logger.error("Currently, a snapshot can be taken from a Root Disk only if it is created from a 1) template in VHD format or 2) from an ISO.");
                runSnap = false;
            }
        }
        
        if (runSnap) {
            List<Long> policiesToBeRemoved = new ArrayList<Long>();
            for (Long policyId : policies) {
                // If it's a manual policy then it won't be there in the volume_snap_policy_ref table.
                // We need to run the snapshot
                if (policyId == Snapshot.MANUAL_POLICY_ID) {
                    // Check if the resource limit for snapshots has been exceeded
                    //UserVO user = _userDao.findById(userId);
                    //AccountVO account = _accountDao.findById(user.getAccountId());
                	AccountVO account = _accountDao.findById(volume.getAccountId());
                	if (_accountMgr.resourceLimitExceeded(account, ResourceType.snapshot)) {
                        throw new ResourceAllocationException("The maximum number of snapshots for account " + account.getAccountName() + " has been exceeded.");
                    }
                }
                else {
                    // Does volume have this policy assigned still
                    SnapshotPolicyVO volPolicy =  _snapshotPolicyDao.findById(policyId);
                    if(volPolicy == null || !volPolicy.isActive()) {
                        // The policy has been removed for the volume. Don't run the snapshot for this policy
                        s_logger.debug("Policy " + policyId + " has been removed for the volume " + volume.getId() + ". Not running snapshot for this policy");
                        // Don't remove while iterating
                        policiesToBeRemoved.add(policyId);
                    }
                }
            }
            // Remove the unnecessary policies out of the iterator.
            policies.removeAll(policiesToBeRemoved);
            if (policies.size() == 0) {
                // There are no valid policies left for the snapshot. Don't execute it.
                runSnap = false;
            }
        }
        
        if (!runSnap) {
            s_logger.warn("Snapshot for volume " + volume.getId() + " not created. No policy assigned currently.");
        }
        
        return runSnap;
    }
    
    @Override @DB
    public SnapshotVO createSnapshot(long userId, long volumeId, List<Long> policyIds) throws InvalidParameterValueException, ResourceAllocationException {
        // Get the async job id from the context.
        Long jobId = null;
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if(asyncExecutor != null) {
            // createSnapshot is always async. Hence asyncExecutor is always not null.
            AsyncJobVO job = asyncExecutor.getJob();
            jobId = job.getId();
        }
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        // set the async_job_id for this in the schedule queue so that it doesn't get scheduled again and block others.
        // mark each of the coinciding schedules as executing in the job queue.
        for (Long policyId : policyIds) {
            SnapshotScheduleVO snapshotSchedule = _snapshotScheduleDao.getCurrentSchedule(volumeId, policyId, false);
            assert snapshotSchedule != null;
            snapshotSchedule.setAsyncJobId(jobId);
            _snapshotScheduleDao.update(snapshotSchedule.getId(), snapshotSchedule);
        }
        txn.commit();

        VolumeVO volume = _volsDao.findById(volumeId);
        
        if (!shouldRunSnapshot(userId, volume, policyIds)) {
            // A null snapshot is interpreted as snapshot creation failed which is what we want to indicate
            return null;
        }
        
        // Gets the most recent snapshot taken. Could return 'removed' snapshots too.
        long lastSnapshotId = _snapshotDao.getLastSnapshot(volumeId, -1);
        Status snapshotStatus = Status.BackedUp;
        if (lastSnapshotId != 0) {
            // There was a previous snapshot.
            SnapshotVO prevSnapshot = _snapshotDao.findById(lastSnapshotId);
            snapshotStatus = prevSnapshot.getStatus();
            if (prevSnapshot.getRemoved() != null && snapshotStatus != Status.BackedUp) {
                // The snapshot was deleted and it was deleted not manually but because backing up failed.
                // Try to back it up again.
                boolean backedUp = backupSnapshotToSecondaryStorage(userId, prevSnapshot);
                if (!backedUp) {
                    // If we can't backup this snapshot, there's not much chance that we can't take another one and back it up again.
                    return null;
                }
            }
        }
        
        SnapshotVO createdSnapshot = null;
        Long id = null;
        
        // Determine the name for this snapshot
        // Snapshot Name: VMInstancename + volumeName + timeString
        String timeString = DateUtil.getDateDisplayString(DateUtil.GMT_TIMEZONE, new Date(), DateUtil.YYYYMMDD_FORMAT);
        
        VMInstanceVO vmInstance = _vmDao.findById(volume.getInstanceId());
        String vmDisplayName = "detached";
        if(vmInstance != null) {
            vmDisplayName = vmInstance.getDisplayName();
        }
        String snapshotName = vmDisplayName + "_" + volume.getName() + "_" + timeString;
        
        // Create the Snapshot object and save it so we can return it to the user
        SnapshotType snapshotType = SnapshotVO.getSnapshotType(policyIds);
        SnapshotVO snapshotVO = new SnapshotVO(volume.getAccountId(), volume.getId(), null, snapshotName, (short)snapshotType.ordinal(), snapshotType.name());
        txn = Transaction.currentTxn();
        txn.start();
        snapshotVO = _snapshotDao.persist(snapshotVO);
        id = snapshotVO.getId();
        assert id != null;
        for (Long policyId : policyIds) {
            // Get the snapshot_schedule table entry for this snapshot and policy id.
            // Set the snapshotId to retrieve it back later.
            SnapshotScheduleVO snapshotSchedule = _snapshotScheduleDao.getCurrentSchedule(volumeId, policyId, true);
            assert snapshotSchedule != null;
            snapshotSchedule.setSnapshotId(id);
            _snapshotScheduleDao.update(snapshotSchedule.getId(), snapshotSchedule);
         
        }
        txn.commit();
        
        // get previous snapshot Path
        long  preId = _snapshotDao.getLastSnapshot(volumeId, id);
        String preSnapshotPath = null;
        SnapshotVO preSnapshotVO = null;
        if( preId != 0) {
            preSnapshotVO = _snapshotDao.findById(preId);
            preSnapshotPath = preSnapshotVO.getPath();

        }

        // Send a ManageSnapshotCommand to the agent
        ManageSnapshotCommand cmd = new ManageSnapshotCommand(ManageSnapshotCommand.CREATE_SNAPSHOT, id, volume.getPath(), preSnapshotPath, snapshotName);
        String basicErrMsg = "Failed to create snapshot for volume: " + volume.getId();
        ManageSnapshotAnswer answer = (ManageSnapshotAnswer) _storageMgr.sendToHostsOnStoragePool(volume.getPoolId(), cmd, basicErrMsg, _totalRetries, _pauseInterval, _shouldBeSnapshotCapable);

        txn = Transaction.currentTxn();
        txn.start();
        
        try {
            // Update the snapshot in the database
            if ((answer != null) && answer.getResult()) {
                // The snapshot was successfully created
                if (preSnapshotPath != null && preSnapshotPath == answer.getSnapshotPath()) {
                    // empty snapshot
                    s_logger.debug("CreateSnapshot: this is empty snapshot, remove it ");
                    // delete from the snapshots table
                    _snapshotDao.delete(id);
                    throw new CloudRuntimeException(
                            " There is no change since last snapshot, please use last snapshot " + preSnapshotPath);

                } else {
                    createdSnapshot = updateDBOnCreate(id, answer.getSnapshotPath());
                }
            } else {
                String msg = "createSnapshotCommand returns null";
                if (answer != null) {
                    msg = answer.getDetails();
                    s_logger.error(msg);
                }
                // delete from the snapshots table
                _snapshotDao.delete(id);
                throw new CloudRuntimeException(" CreateSnapshot failed due to " + msg);
            }

            // Update async status after snapshot creation and before backup
            if (asyncExecutor != null) {
                AsyncJobVO job = asyncExecutor.getJob();

                if (s_logger.isDebugEnabled())
                    s_logger.debug("CreateSnapshot created a new instance " + id + ", update async job-" + job.getId()
                            + " progress status");

                _asyncMgr.updateAsyncJobAttachment(job.getId(), "snapshot", id);
                _asyncMgr.updateAsyncJobStatus(job.getId(), BaseCmd.PROGRESS_INSTANCE_CREATED, id);
            }
        } finally {
            txn.commit();
        }
        
        return createdSnapshot;
    }

    private SnapshotVO updateDBOnCreate(Long id, String snapshotPath) {
        SnapshotVO createdSnapshot = _snapshotDao.findById(id);
        Long volumeId = createdSnapshot.getVolumeId();
        createdSnapshot.setPath(snapshotPath);
        createdSnapshot.setStatus(Snapshot.Status.CreatedOnPrimary);
        createdSnapshot.setPrevSnapshotId(_snapshotDao.getLastSnapshot(volumeId, id));
        _snapshotDao.update(id, createdSnapshot);
        return createdSnapshot;
    }
    
    @Override
    @DB
    @SuppressWarnings("fallthrough")
    public void validateSnapshot(Long userId, SnapshotVO snapshot) {
        assert snapshot != null;
        Long id = snapshot.getId();
        Status status = snapshot.getStatus();
        s_logger.debug("Snapshot scheduler found a snapshot whose actual status is not clear. Snapshot id:" + id + " with DB status: " + status);
        
        switch (status) {
        case Creating:
            // String snapshotUUID = snapshot.getPath();
            // Long prevSnapshotId = snapshot.getPrevSnapshotId();
            // All these will be null.

            // But there the ManageSnapshotCommand might have succeeded on the primary without updating the database
            // Run validatePreviousSnapshotBackup to check if that is the case.
            // Depending on the result, complete the remaining part of the createSnapshot method.
            // Call backupSnapshotToSecondaryStorage, if snapshot was actually taken on primary.
            s_logger.debug("Management Server crashed before the ManageSnapshotCommand returned. Checking if snapshot was created on primary");
            Long volumeId = snapshot.getVolumeId();
            assert volumeId != null;
            VolumeVO volume = _volsDao.findById(volumeId);
            // By default, assume failure
            String actualSnapshotUuid = null;
            boolean createdOnPrimary = false;
            
            ValidateSnapshotAnswer answer = getLastSnapshotDetails(volume, snapshot.getPath());
            
            
            if (answer != null) {
                if (answer.getResult()) {
                    // The expected snapshot details on the primary is the same as it would be if this snapshot was never taken at all
                    // Just delete the entry in the table
                    _snapshotDao.delete(id);
                    // Create an event saying the snapshot failed. ??
                    // An event is not generated when validatePreviousSnapshotBackup fails. So not generating it here too.
                }
                else {
                    // The answer is different from expected.
                    // This may be because the snapshot given was actually taken on primary, but DB update didn't happen.
                    // Now update the DB to denote that snapshot was created on primary and
                    // fall through to the next case.
                    actualSnapshotUuid = answer.getActualSnapshotUuid();
                    if (actualSnapshotUuid != null && !actualSnapshotUuid.isEmpty()) {
                        s_logger.debug("The snapshot " + id + " was actually created on the primary. Updating the DB record and backing it up to secondary");
                        updateDBOnCreate(id, actualSnapshotUuid);
                        createdOnPrimary = true;
                    }
                }
            }
            
            if (!createdOnPrimary) {
                break;
            }
            // else continue to the next case.
        case CreatedOnPrimary:
            // The snapshot has been created on the primary and the DB has been updated.
            // However, it hasn't entered the backupSnapshotToSecondaryStorage, else
            // status would have been backing up.
            // So call backupSnapshotToSecondaryStorage without any fear.
        case BackingUp:
            // It has entered backupSnapshotToSecondaryStorage.
            // But we have no idea whether it was backed up or not.
            // So call backupSnapshotToSecondaryStorage again.
            backupSnapshotToSecondaryStorage(userId, snapshot);
            break;
        case BackedUp:
            // No need to do anything as snapshot has already been backed up.
        }
    }

    @Override
    @DB
    public boolean backupSnapshotToSecondaryStorage(long userId, SnapshotVO snapshot) {
        long id = snapshot.getId();
        
        snapshot.setStatus(Snapshot.Status.BackingUp);
        _snapshotDao.update(snapshot.getId(), snapshot);
        
        long volumeId   = snapshot.getVolumeId();
        VolumeVO volume = _volsDao.findById(volumeId);
        
        String primaryStoragePoolNameLabel = _storageMgr.getPrimaryStorageNameLabel(volume);
        Long dcId                          = volume.getDataCenterId();
        Long accountId                     = volume.getAccountId();
        
        String secondaryStoragePoolUrl = _storageMgr.getSecondaryStorageURL(volume.getDataCenterId());
        String snapshotUuid = snapshot.getPath();
        // In order to verify that the snapshot is not empty,
        // we check if the parent of the snapshot is not the same as the parent of the previous snapshot.
        // We pass the uuid of the previous snapshot to the plugin to verify this.
        SnapshotVO prevSnapshot = null;
        String prevSnapshotUuid = null;
        String prevBackupUuid = null;
        
        boolean isFirstSnapshotOfRootVolume = false;
        long prevSnapshotId = snapshot.getPrevSnapshotId();
        if (prevSnapshotId > 0) {
            prevSnapshot = _snapshotDao.findById(prevSnapshotId);
            prevSnapshotUuid = prevSnapshot.getPath();
            prevBackupUuid = prevSnapshot.getBackupSnapshotId();
        }
        else {
            // This is the first snapshot of the volume.
            if (volume.getVolumeType() == VolumeType.ROOT && getImageFormat(volumeId) != ImageFormat.ISO && volume.getTemplateId() != null) {
                isFirstSnapshotOfRootVolume = true;
                // If the first snapshot of the root volume is empty, it's parent will point to the base template.
                // So pass the template uuid as the fake previous snapshot.
                Long templateId = volume.getTemplateId();
                // ROOT disks are created off templates have templateIds
                assert templateId != null;
                Long poolId = volume.getPoolId();
                if (templateId != null && poolId != null) {
                    VMTemplateStoragePoolVO vmTemplateStoragePoolVO =
                        _templatePoolDao.findByPoolTemplate(poolId, templateId);
                    if (vmTemplateStoragePoolVO != null) {
                        prevSnapshotUuid = vmTemplateStoragePoolVO.getInstallPath();
                    }
                    else {
                        s_logger.warn("Volume id: " + volumeId +
                                      " in pool id: " + poolId +
                                      " based off  template id: " + templateId +
                                      " doesn't have an entry in the template_spool_ref table." +
                                      " Using null as the template.");
                    }
                }
                
            }
        }
        String firstBackupUuid = volume.getFirstSnapshotBackupUuid();
        boolean isVolumeInactive = _storageMgr.volumeInactive(volume);
        BackupSnapshotCommand backupSnapshotCommand =
            new BackupSnapshotCommand(primaryStoragePoolNameLabel,
                                      secondaryStoragePoolUrl,
                                      dcId,
                                      accountId,
                                      volumeId,
                                      snapshotUuid,
                                      prevSnapshotUuid,
                                      prevBackupUuid,
                                      firstBackupUuid,
                                      isFirstSnapshotOfRootVolume,
                                      isVolumeInactive);
        
        String backedUpSnapshotUuid = null;
        // By default, assume failed.
        String basicErrMsg = "Failed to backup snapshot id " + snapshot.getId() + " to secondary storage for volume: " + volumeId;
        boolean backedUp = false;
        BackupSnapshotAnswer answer = (BackupSnapshotAnswer) _storageMgr.sendToHostsOnStoragePool(volume.getPoolId(),
                                                                                                  backupSnapshotCommand,
                                                                                                  basicErrMsg,
                                                                                                  _totalRetries,
                                                                                                  _pauseInterval,
                                                                                                  _shouldBeSnapshotCapable);
        if (answer != null && answer.getResult()) {
            backedUpSnapshotUuid = answer.getBackupSnapshotName();
            if (backedUpSnapshotUuid != null) {
                backedUp = true;
                // is there a snap to be deleted?
                // clean now
                if(prevSnapshot != null && backedUpSnapshotUuid.equalsIgnoreCase(prevSnapshot.getBackupSnapshotId())) {
                    //if new snapshot is same as previous snapshot , delete previous snapshot
                    s_logger.debug("Delete duplicate Snapshot id: " + prevSnapshotId);
                    long pprevSnapshotId = prevSnapshot.getPrevSnapshotId();
                    snapshot.setPrevSnapshotId(pprevSnapshotId);
                    _snapshotDao.update(snapshot.getId(), snapshot);
                    _snapshotDao.delete(prevSnapshot.getId());
                    
                    EventVO event = new EventVO();
                    String eventParams = "id=" + prevSnapshot.getId() + "\nssName=" + prevSnapshot.getName();
                    event.setType(EventTypes.EVENT_SNAPSHOT_DELETE);
                    event.setState(EventState.Completed);
                    event.setDescription("Delete snapshot id: " + prevSnapshot.getId() + " due to new snapshot is same as this one");
                    event.setLevel(EventVO.LEVEL_INFO);
                    event.setParameters(eventParams);
                    _eventDao.persist(event);
                    
                    prevSnapshotId = pprevSnapshotId;
                    if( prevSnapshotId == 0 ) {
                        prevSnapshot = null;
                        prevSnapshotUuid = null;
                        prevBackupUuid = null;
                    } else {
                        prevSnapshot = _snapshotDao.findById(prevSnapshotId);
                        prevSnapshotUuid = prevSnapshot.getPath();
                        prevBackupUuid = prevSnapshot.getBackupSnapshotId();
                    }
                     
                }
                // if previous snapshot is marked as Removed, remove it now
                if(prevSnapshot != null && prevSnapshot.getRemoved() != null) {
                    s_logger.debug("Snapshot id: " + prevSnapshotId + " was marked as removed. Deleting it from the primary/secondary/DB now.");
                    // Get the prevSnapshotId of the snapshot to be deleted.
                    // This will become the prevSnapshotId of the current snapshot
                    long prevValidSnapshotId = prevSnapshot.getPrevSnapshotId();
                    String prevValidSnapshotBackupUuid = null;
                    if (prevValidSnapshotId > 0) {
                        prevValidSnapshotBackupUuid = _snapshotDao.findById(prevValidSnapshotId).getBackupSnapshotId();
                    }
                    
                    snapshot.setPrevSnapshotId(prevValidSnapshotId);
                    _snapshotDao.update(id, snapshot);
                    
                    backedUp = destroyLastSnapshot(prevValidSnapshotBackupUuid, prevSnapshot, backedUpSnapshotUuid);
                    if (!backedUp) {
                        s_logger.debug("Error while deleting last snapshot id: " + prevSnapshotId + " for volume " + volumeId);
                    }
                }
            }
        }
        else if (answer != null) {
            s_logger.error(answer.getDetails());
        }
        // Update the status in all cases.
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        SnapshotVO snapshotVO = _snapshotDao.findById(id);
        snapshotVO.setBackupSnapshotId(backedUpSnapshotUuid);
        if (volume.getFirstSnapshotBackupUuid() == null) {
            // This is the first ever snapshot taken for the volume.
            // Set the first snapshot backup uuid once and for all.
            // XXX: This will get set to non-null only if we were able to backup the first snapshot
            // successfully. If we didn't backup the first snapshot, the volume is essentially
            // screwed as far as snapshots are concerned.
            volume.setFirstSnapshotBackupUuid(backedUpSnapshotUuid);
            _volsDao.update(volumeId, volume);
        }
        
        // Create an event
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(volume.getAccountId());
        event.setType(EventTypes.EVENT_SNAPSHOT_CREATE);
        String snapshotName = snapshotVO.getName();
        
        if (backedUp) {
            snapshotVO.setStatus(Snapshot.Status.BackedUp);
            String eventParams = "id=" + id + "\nssName=" + snapshotName +"\nsize=" + volume.getSize()+"\ndcId=" + volume.getDataCenterId();
            event.setDescription("Backed up snapshot id: " + id + " to secondary for volume " + volumeId);
            event.setLevel(EventVO.LEVEL_INFO);
            event.setParameters(eventParams);
            _snapshotDao.update(id, snapshotVO);
        }
        else {
            // Just mark it as removed in the database. When the next snapshot it taken,
            // validate previous snapshot will fix the state.
            // It will
            // 1) Call backupSnapshotToSecondaryStorage and try again.
            // 2) Create the next Snapshot pretending this is a valid snapshot.
            // 3) backupSnapshotToSecondaryStorage of the next snapshot
            // will take care of cleaning up the state of this snapshot
            _snapshotDao.remove(id);
            event.setLevel(EventVO.LEVEL_ERROR);
            event.setDescription("Failed to backup snapshot id: " + id + " to secondary for volume " + volumeId);
        }
        // Save the event
        _eventDao.persist(event);
        txn.commit();
        
        return backedUp;
    }

    @Override
    @DB
    public void postCreateSnapshot(long userId, long volumeId, long snapshotId, List<Long> policyIds, boolean backedUp) {
        // Update the snapshot_policy_ref table with the created snapshot
        // Get the list of policies for this snapshot
        Transaction txn = Transaction.currentTxn();
        txn.start();
        for (long policyId : policyIds) {
            if (backedUp) {
                // create an entry in snap_policy_ref table
                SnapshotPolicyRefVO snapPolicyRef = new SnapshotPolicyRefVO(snapshotId, volumeId, policyId);
                _snapPolicyRefDao.persist(snapPolicyRef);
                
            	// This is a manual create, so increment the count of snapshots for this account
                if (policyId == Snapshot.MANUAL_POLICY_ID) {
                	Snapshot snapshot = _snapshotDao.findById(snapshotId);
                	_accountMgr.incrementResourceCount(snapshot.getAccountId(), ResourceType.snapshot);
                }
            }
            
            // Even if the current snapshot failed, we should schedule the next recurring snapshot for this policy.
            if (policyId != Snapshot.MANUAL_POLICY_ID) {
                postCreateRecurringSnapshotForPolicy(userId, volumeId, snapshotId, policyId);
            }
            else {
                // Delete the entry from the snapshot_schedule table so that the
                // next manual snapshot can be taken.
                
                // Get the schedule of this snapshot
                SnapshotScheduleVO snapshotSchedule = _snapshotScheduleDao.getCurrentSchedule(volumeId, policyId, true);
                if (snapshotSchedule != null) {
                    // We should lock the row before deleting it as it is also being deleted by the scheduler.
                    _snapshotScheduleDao.delete(snapshotSchedule.getId());
                }
            }
        }
        txn.commit();
    }

    private ValidateSnapshotAnswer getLastSnapshotDetails(VolumeVO volume, String previousSnapshotUuid) {
        // Validate the VDI parent structure for the volume on the primary storage
        Long volumeId = volume.getId();
        String primaryStoragePoolNameLabel = _storageMgr.getPrimaryStorageNameLabel(volume);
        String volumeUuid = volume.getPath();
        Long poolId = volume.getPoolId();
        String firstSnapshotBackupUuid = volume.getFirstSnapshotBackupUuid();
        String templateUuid = null;
        String details = null;
        if (firstSnapshotBackupUuid == null && volume.getVolumeType() == VolumeType.ROOT) {
            Long templateId = volume.getTemplateId();
            VMTemplateVO template = _templateDao.findById(templateId);
            if(template == null) {
                details = "Unable find template id: " + templateId + " for root disk volumeId: " + volumeId;
                s_logger.error(details);
            }
            else if (template.getFormat() == ImageFormat.VHD) {
                // We support creating snapshots of Root Disk created from template only in VHD format.
                VMTemplateStoragePoolVO templateStoragePoolVO = _templatePoolDao.findByPoolTemplate(volume.getPoolId(), templateId);
                if (templateStoragePoolVO != null) {
                    templateUuid = templateStoragePoolVO.getInstallPath();
                }
                else {
                    details = "Template id: " + templateId + " is not present in on the primary storage pool id: " + volume.getPoolId() +
                              " according to the template_spool_ref table";
                    s_logger.error(details);
                }
            }
        }
        ValidateSnapshotAnswer answer = null;
        if (details == null) {
            // EverythingBackup is fine until now. Proceed with command.
            ValidateSnapshotCommand cmd =
                new ValidateSnapshotCommand(primaryStoragePoolNameLabel, volumeUuid, firstSnapshotBackupUuid, previousSnapshotUuid, templateUuid);
            String basicErrMsg = "Failed to validate VDI structure for volumeId: " + volume.getId() + " with UUID: " + volumeUuid;
            
            answer = (ValidateSnapshotAnswer) _storageMgr.sendToHostsOnStoragePool(poolId,
                                                                                   cmd,
                                                                                   basicErrMsg,
                                                                                   _totalRetries,
                                                                                   _pauseInterval,
                                                                                   _shouldBeSnapshotCapable);
        }
        
        return answer;
    }
    
    private void postCreateRecurringSnapshotForPolicy(long userId, long volumeId, long snapshotId, long policyId) {
        //Use count query
    	Filter searchFilter = new Filter(SnapshotVO.class, GenericDaoBase.CREATED_COLUMN, true, null, null);
        List<SnapshotVO> snaps = listSnapsforPolicy(policyId, searchFilter);
        SnapshotPolicyVO policy = _snapshotPolicyDao.findById(policyId);
        
        while(snaps.size() > policy.getMaxSnaps() && snaps.size() > 1) {
            //Delete the oldest snap ref in snap_policy_ref
            SnapshotVO oldestSnapshot = snaps.get(0);
            long oldSnapId = oldestSnapshot.getId();
            s_logger.debug("Max snaps: "+ policy.getMaxSnaps() + " exceeded for snapshot policy with Id: " + policyId + ". Deleting oldest snapshot: " + oldSnapId);
            // Excess snapshot. delete it asynchronously
            destroySnapshotAsync(userId, volumeId, oldSnapId, policyId);
            
            snaps.remove(oldestSnapshot);
        }
        
    }

    @Override @DB
    public boolean deleteSnapshot(long userId, long snapshotId, long policyId) {
        s_logger.debug("Calling deleteSnapshot for snapshotId: " + snapshotId + " and policyId " + policyId);
        long prevSnapshotId = 0;
        SnapshotVO nextSnapshot = null;
        boolean deleted = true;
        boolean actuallyDelete = false;
        List<SnapshotPolicyRefVO> snapPolicyRefs = _snapPolicyRefDao.listBySnapshotId(snapshotId);
        // Destroy snapshot if its not part of any policy other than the given one.
        if(snapPolicyRefs.size() == 1 && (snapPolicyRefs.get(0).getPolicyId() == policyId)) {
            SnapshotVO currentSnapshot = _snapshotDao.findById(snapshotId);
            String backupOfSnapshot = currentSnapshot.getBackupSnapshotId();
            nextSnapshot = _snapshotDao.findNextSnapshot(snapshotId);
            String backupOfNextSnapshot = null;
            if (nextSnapshot != null) {
                backupOfNextSnapshot = nextSnapshot.getBackupSnapshotId();
            }
             
            prevSnapshotId = currentSnapshot.getPrevSnapshotId();
            String backupOfPreviousSnapshot = null;
            if (prevSnapshotId > 0) {
                SnapshotVO prevSnapshot = _snapshotDao.findById(prevSnapshotId);
                backupOfPreviousSnapshot = prevSnapshot.getBackupSnapshotId();
            }
            
            if (backupOfSnapshot != null) {
                if (backupOfNextSnapshot != null && backupOfSnapshot.equals(backupOfNextSnapshot)) {
                    // Both the snapshots point to the same backup VHD file.
                    // There is no difference in the data between them.
                    // We don't want to delete the backup of the older snapshot
                    // as it means that we delete the next snapshot too
                }
                else if (backupOfPreviousSnapshot != null && backupOfSnapshot.equals(backupOfPreviousSnapshot)) {
                    // If we delete the current snapshot, the user will not
                    // be able to recover from the previous snapshot
                    // So don't delete anything
                }
                else {
                    actuallyDelete = true;
                    deleted = destroySnapshot(userId, snapshotId, policyId);
                }
                
                if (!actuallyDelete) {
                    // Don't actually delete the snapshot backup but delete the entry
                    // from both snapshots and snapshot_policy_ref table
                    boolean isLastSnap = (nextSnapshot == null);
                    postDeleteSnapshot(snapshotId, policyId, isLastSnap);
                    // create the event
                    String eventParams = "id=" + snapshotId;
                    EventVO event = new EventVO();
                    
                    event.setUserId(userId);
                    event.setAccountId((currentSnapshot != null) ? currentSnapshot.getAccountId() : 0);
                    event.setType(EventTypes.EVENT_SNAPSHOT_DELETE);
                    event.setDescription("Successfully deleted snapshot " + snapshotId + " for volumeId: " + currentSnapshot.getVolumeId() + " and policyId " + policyId);
                    event.setParameters(eventParams);
                    event.setLevel(EventVO.LEVEL_INFO);
                    _eventDao.persist(event);
                }
            }
        }
        else {
            // Just delete the entry from the snapshot_policy_ref table
            Transaction txn = Transaction.currentTxn();
            txn.start();
            _snapPolicyRefDao.removeSnapPolicy(snapshotId, policyId);
            txn.commit();
        }
        
        return deleted;

    }

    @Override @DB
    public long destroySnapshotAsync(long userId, long volumeId, long snapshotId, long policyId) {
        VolumeVO volume = _volsDao.findById(volumeId);
        SnapshotOperationParam param = new SnapshotOperationParam(userId, volume.getAccountId(), volumeId, snapshotId, policyId);
        Gson gson = GsonHelper.getBuilder().create();
        
        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(userId);
        job.setAccountId(volume.getAccountId());
        job.setCmd("DeleteSnapshot");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(CreateSnapshotCmd.getResultObjectName());
        
        return _asyncMgr.submitAsyncJob(job, true);

    }

    @Override @DB
    public boolean destroySnapshot(long userId, long snapshotId, long policyId) {
        boolean success = false;
        String details = null;
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
        if (snapshot != null) {
            SnapshotVO nextSnapshot = _snapshotDao.findNextSnapshot(snapshotId);
            if(nextSnapshot == null){
                // This is last snapshot.
                // Destroy this snapshot after creation of next snapshot. Only mark as removed in DB
                details = "Successfully deleted snapshot " + snapshotId + " for volumeId: " + snapshot.getVolumeId() + " and policyId " + policyId;
                s_logger.debug("This is last snapshot for volume. Not destroying now: " + snapshot.getId());
                postDeleteSnapshot(snapshotId, policyId, true);
                success = true;
            } else {
                VolumeVO volume = _volsDao.findById(snapshot.getVolumeId());
                String primaryStoragePoolNameLabel = _storageMgr.getPrimaryStorageNameLabel(volume);
                String secondaryStoragePoolUrl = _storageMgr.getSecondaryStorageURL(volume.getDataCenterId());
                Long dcId = volume.getDataCenterId();
                Long accountId = volume.getAccountId();
                Long volumeId = volume.getId();

                String backupOfSnapshot = snapshot.getBackupSnapshotId();
                String backupOfNextSnapshot = null;
                if (nextSnapshot != null) {
                    backupOfNextSnapshot = nextSnapshot.getBackupSnapshotId();
                }

                DeleteSnapshotBackupCommand cmd =
                    new DeleteSnapshotBackupCommand(primaryStoragePoolNameLabel,
                                                    secondaryStoragePoolUrl,
                                                    dcId,
                                                    accountId,
                                                    volumeId,
                                                    backupOfSnapshot,
                                                    backupOfNextSnapshot);
                
                details = "Failed to destroy snapshot id:" + snapshotId + " for volume: " + volume.getId();
                Answer answer = _storageMgr.sendToHostsOnStoragePool(volume.getPoolId(),
                                                                     cmd,
                                                                     details,
                                                                     _totalRetries,
                                                                     _pauseInterval,
                                                                     _shouldBeSnapshotCapable);
                
                if ((answer != null) && answer.getResult()) {
                    // This is not the last snapshot.
                    postDeleteSnapshot(snapshotId, policyId, false);
                    success = true;
                    details = "Successfully deleted snapshot " + snapshotId + " for volumeId: " + volumeId + " and policyId " + policyId;
                    s_logger.debug(details);
                }
                else if (answer != null) {
                    if (answer.getDetails() != null) {
                        details = answer.getDetails();
                    }
                    s_logger.error(details);
                }
            }
        }

        // create the event
        String eventParams = "id=" + snapshotId;
        EventVO event = new EventVO();
        
        event.setUserId(userId);
        event.setAccountId((snapshot != null) ? snapshot.getAccountId() : 0);
        event.setType(EventTypes.EVENT_SNAPSHOT_DELETE);
        event.setDescription(details);
        event.setParameters(eventParams);
        event.setLevel(success ? EventVO.LEVEL_INFO : EventVO.LEVEL_ERROR);
        _eventDao.persist(event);

        return success;

    }
    
    @DB
    protected void postDeleteSnapshot(long snapshotId, long policyId, boolean isLastSnap) {
        // Remove the snapshot from the snapshots table and the snap_policy_ref table.
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
        if(isLastSnap){
            _snapshotDao.remove(snapshotId);
        } else {
            _snapshotDao.delete(snapshotId);
            // In the snapshots table,
            // the last_snapshot_id field of the next snapshot becomes the last_snapshot_id of the deleted snapshot
            long prevSnapshotId = snapshot.getPrevSnapshotId();
            SnapshotVO nextSnapshot = _snapshotDao.findNextSnapshot(snapshotId);
            assert nextSnapshot != null; // That is how lastSnap is decided.
            nextSnapshot.setPrevSnapshotId(prevSnapshotId);
            _snapshotDao.update(nextSnapshot.getId(), nextSnapshot);

        }
        
        _snapPolicyRefDao.removeSnapPolicy(snapshotId, policyId);
        
        // If this is a manual delete, decrement the count of snapshots for this account
        if (policyId == Snapshot.MANUAL_POLICY_ID) {
        	_accountMgr.decrementResourceCount(snapshot.getAccountId(), ResourceType.snapshot);
        }
        
        txn.commit();
    }

    @Override
    public long createVolumeFromSnapshotAsync(long userId, long accountId, long snapshotId, String volumeName) throws InternalErrorException {
        // Precondition the snapshot is valid
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
        VolumeVO volume = _volsDao.findById(snapshot.getVolumeId());
        
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(EventTypes.EVENT_VOLUME_CREATE);
        event.setState(EventState.Scheduled);
        event.setDescription("Scheduled async job for creating volume from snapshot with id: "+snapshotId);
        event = _eventDao.persist(event);

        SnapshotOperationParam param = new SnapshotOperationParam(userId, accountId, volume.getId(), snapshotId, volumeName);
        param.setEventId(event.getId());
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(userId);
        job.setAccountId(snapshot.getAccountId());
        job.setCmd("CreateVolumeFromSnapshot");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(CreateVolumeCmd.getResultObjectName());

        return _asyncMgr.submitAsyncJob(job);
        
    }

   
    @Override
	public boolean deleteSnapshotDirsForAccount(long accountId) {
        
        List<VolumeVO> volumes = _volsDao.findByAccount(accountId);
        // The above call will list only non-destroyed volumes.
        // So call this method before marking the volumes as destroyed.
        // i.e Call them before the VMs for those volumes are destroyed.
        boolean success = true;
        for (VolumeVO volume : volumes) {
        
        	if(volume.getPoolId() == null){
        		continue;
        	}
        	Long volumeId = volume.getId();
        	Long dcId = volume.getDataCenterId();
        	String secondaryStoragePoolURL = _storageMgr.getSecondaryStorageURL(dcId);
        	String primaryStoragePoolNameLabel = _storageMgr.getPrimaryStorageNameLabel(volume);
        	long mostRecentSnapshotId = _snapshotDao.getLastSnapshot(volumeId, -1L);
        	if (mostRecentSnapshotId == 0L) {
        	    // This volume doesn't have any snapshots. Nothing do delete.
        	    continue;
        	}
        	SnapshotVO mostRecentSnapshot = _snapshotDao.findById(mostRecentSnapshotId);
        	if (mostRecentSnapshot == null) {
        	    // Huh. The code should never reach here.
        	    s_logger.error("Volume Id's mostRecentSnapshot with id: " + mostRecentSnapshotId + " turns out to be null");
        	}
        	// even if mostRecentSnapshot.removed() != null, we still have to explicitly remove it from the primary storage.
        	// Then deleting the volume VDI will GC the base copy and nothing will be left on primary storage.
        	String mostRecentSnapshotUuid = mostRecentSnapshot.getPath();
        	DeleteSnapshotsDirCommand cmd = new DeleteSnapshotsDirCommand(primaryStoragePoolNameLabel, secondaryStoragePoolURL, dcId, accountId, volumeId, mostRecentSnapshotUuid);
        	String basicErrMsg = "Failed to destroy snapshotsDir for: " + volume.getId() + " under account: " + accountId;
        	Answer answer = null;
        	Long poolId = volume.getPoolId();
        	if (poolId != null) {
        	    // Retry only once for this command. There's low chance of failure because of a connection problem.
        	    answer = _storageMgr.sendToHostsOnStoragePool(poolId, cmd, basicErrMsg, 1, _pauseInterval, _shouldBeSnapshotCapable);
        	}
        	else {
        	    s_logger.info("Pool id for volume id: " + volumeId + " belonging to account id: " + accountId + " is null. Assuming the snapshotsDir for the account has already been deleted");
        	}
            
        	if (success) {
        		// SnapshotsDir has been deleted for the volumes so far.
        		success = (answer != null) && answer.getResult();
        		if (success) {
        		    s_logger.debug("Deleted snapshotsDir for volume: " + volumeId + " under account: " + accountId);
        		}
        		else if (answer != null) {
        		    s_logger.error(answer.getDetails());
        		}
        	}

        	// Either way delete the snapshots for this volume.
        	List<SnapshotVO> snapshots = listSnapsforVolume(volumeId);
        	for (SnapshotVO snapshot: snapshots) {
        	    if(_snapshotDao.delete(snapshot.getId())){
        	    	_accountMgr.decrementResourceCount(accountId, ResourceType.snapshot);
        	    	
        	        //Log event after successful deletion
        	        String eventParams = "id=" + snapshot.getId();
        	        EventVO event = new EventVO();
        	        event.setUserId(1L);
        	        event.setAccountId(snapshot.getAccountId());
        	        event.setType(EventTypes.EVENT_SNAPSHOT_DELETE);
        	        event.setDescription("Successfully deleted snapshot " + snapshot.getId() + " for volumeId: " + snapshot.getVolumeId());
        	        event.setParameters(eventParams);
        	        event.setLevel(EventVO.LEVEL_INFO);
        	        _eventDao.persist(event);
        	    }
        	}
        }
        
        // Returns true if snapshotsDir has been deleted for all volumes.
        return success;
	}
    
    @Override
    @DB
    public SnapshotPolicyVO createPolicy(long userId, long accountId, long volumeId, String schedule, short interval, int maxSnaps, String timezone) {
        Long policyId = null;
        SnapshotPolicyVO policy = getPolicyForVolumeByInterval(volumeId, (interval));
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        // Create an event
        EventVO event = new EventVO();
        event.setAccountId(accountId);
        event.setUserId(userId);
        
        if( policy != null){
            s_logger.debug("Policy for specified interval already exists. Updating policy to new schedule");
            policyId = policy.getId();
            
            // By default, assume failure.
            event.setType(EventTypes.EVENT_SNAPSHOT_POLICY_UPDATE);
            event.setDescription("Failed to update schedule for Snapshot policy with id: "+policyId);
            event.setLevel(EventVO.LEVEL_ERROR);
            
            // Check if there are any recurring snapshots being currently executed. Race condition.
            SnapshotScheduleVO snapshotSchedule = _snapshotScheduleDao.getCurrentSchedule(volumeId, policyId, true);
            if (snapshotSchedule != null) {
                Date scheduledTimestamp = snapshotSchedule.getScheduledTimestamp();
                String dateDisplay = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, scheduledTimestamp);
                s_logger.debug("Cannot update the policy now. Wait until the current snapshot scheduled at " + dateDisplay + " finishes");
                
                policyId = null;
                policy = null;
            }
            else {
                _snapSchedMgr.removeSchedule(volumeId, policyId);
                policy.setSchedule(schedule);
                policy.setTimezone(timezone);
                policy.setMaxSnaps(maxSnaps);
                policy.setActive(true);
                
                if(_snapshotPolicyDao.update(policy.getId(), policy)){
                    event.setLevel(EventVO.LEVEL_INFO);
                    event.setDescription("Successfully updated snapshot policy with Id: "+policyId);
                }
            }
        } else {
            policy = new SnapshotPolicyVO(volumeId, schedule, timezone, interval, maxSnaps);
            policy = _snapshotPolicyDao.persist(policy);
            policyId = policy.getId();
            event.setType(EventTypes.EVENT_SNAPSHOT_POLICY_CREATE);
            event.setDescription("Successfully created snapshot policy with Id: "+policyId);
        }
                
        _eventDao.persist(event);
        if (policyId != null) {
            _snapSchedMgr.scheduleNextSnapshotJob(policy);
        }
        else {
            s_logger.debug("Failed to update schedule for Snapshot policy with id: " + policyId);
        }
        txn.commit();
        
        return policy;
    }

    
    @Override
    public boolean deletePolicy(long userId, long policyId) {
        SnapshotPolicyVO snapshotPolicy = _snapshotPolicyDao.findById(policyId);
        VolumeVO volume = _volsDao.findById(snapshotPolicy.getVolumeId());
        snapshotPolicy.setActive(false);
        _snapSchedMgr.removeSchedule(snapshotPolicy.getVolumeId(), snapshotPolicy.getId());
        EventVO event = new EventVO();
        event.setAccountId(volume.getAccountId());
        event.setUserId(userId);
        event.setType(EventTypes.EVENT_SNAPSHOT_POLICY_DELETE);
        boolean success = _snapshotPolicyDao.update(policyId, snapshotPolicy);
        if(success){
            event.setLevel(EventVO.LEVEL_INFO);
            event.setDescription("Successfully deleted snapshot policy with Id: "+policyId);
        } else {
            event.setLevel(EventVO.LEVEL_ERROR);
            event.setDescription("Failed to  delete snapshot policy with Id: "+policyId);
        }
        _eventDao.persist(event);
        return success;
    }

    @Override
    public List<SnapshotPolicyVO> listPoliciesforVolume(long volumeId) {
        return _snapshotPolicyDao.listByVolumeId(volumeId);
    }
    
    @Override
    public List<SnapshotPolicyVO> listPoliciesforSnapshot(long snapshotId) {
        SearchCriteria sc = PoliciesForSnapSearch.create();
        sc.setJoinParameters("policyRef", "snapshotId", snapshotId);
        return _snapshotPolicyDao.search(sc, null);
    }

    @Override
    public List<SnapshotVO> listSnapsforPolicy(long policyId, Filter filter) {
        SearchCriteria sc = PolicySnapshotSearch.create();
        sc.setJoinParameters("policy", "policyId", policyId);
        return _snapshotDao.search(sc, filter);
    }


    @Override
    public List<SnapshotVO> listSnapsforVolume(long volumeId) {
        return _snapshotDao.listByVolumeId(volumeId);
    }

    @Override
	public void deletePoliciesForVolume(Long volumeId) {
    	List<SnapshotPolicyVO> policyInstances = listPoliciesforVolume(volumeId);
    	for (SnapshotPolicyVO policyInstance : policyInstances) {
    	    Long policyId = policyInstance.getId();
    		deletePolicy(1L, policyId);
    	}
    	// We also want to delete the manual snapshots scheduled for this volume
    	// We can only delete the schedules in the future, not the ones which are already executing.
    	SnapshotScheduleVO snapshotSchedule = _snapshotScheduleDao.getCurrentSchedule(volumeId, Snapshot.MANUAL_POLICY_ID, false);
    	if (snapshotSchedule != null) {
    	    _snapshotScheduleDao.delete(snapshotSchedule.getId());
    	}
	}
    
    private boolean destroyLastSnapshot(String backupOfPreviousSnapshot, SnapshotVO snapshot, String backupOfNextSnapshot) {
        boolean success = false;
        long snapshotId = snapshot.getId();
        if (snapshot != null) {
            String backupOfSnapshot = snapshot.getBackupSnapshotId();
            if (backupOfSnapshot != null) {
                if (backupOfNextSnapshot != null && backupOfSnapshot.equals(backupOfNextSnapshot)) {
                    // Both the snapshots point to the same backup VHD file.
                    // There is no difference in the data between them.
                    // We don't want to delete the backup of the older snapshot
                    // as it means that we delete the next snapshot too
                    success = true;
                    s_logger.debug("Removed snapshot " + snapshotId +
                                   " is not being destroyed from secondary as " +
                                   "it is the same as the current snapshot uuid: " + backupOfNextSnapshot);
                }
                else if (backupOfPreviousSnapshot != null && backupOfSnapshot.equals(backupOfPreviousSnapshot)) {
                    // If we delete the current snapshot, the user will not
                    // be able to recover from the previous snapshot
                    // So don't delete anything
                    success = true;
                    s_logger.debug("Removed snapshot " + snapshotId +
                                   " is not being destroyed from secondary as " +
                                   "it is the same as it's previous snapshot with uuid: " + backupOfPreviousSnapshot);
                } else {
                    VolumeVO volume = _volsDao.findById(snapshot.getVolumeId());
                    String primaryStoragePoolNameLabel = _storageMgr.getPrimaryStorageNameLabel(volume);
                    String secondaryStoragePoolUrl = _storageMgr.getSecondaryStorageURL(volume.getDataCenterId());
                    Long dcId = volume.getDataCenterId();
                    Long accountId = volume.getAccountId();
                    Long volumeId = volume.getId();

                    DeleteSnapshotBackupCommand cmd =
                        new DeleteSnapshotBackupCommand(primaryStoragePoolNameLabel,
                                secondaryStoragePoolUrl,
                                dcId,
                                accountId,
                                volumeId,
                                backupOfSnapshot,
                                backupOfNextSnapshot);
                    String basicErrMsg = "Failed to destroy snapshot id: " + snapshotId + " for volume id: " + volumeId;
                    Answer answer = _storageMgr.sendToHostsOnStoragePool(volume.getPoolId(), cmd, basicErrMsg, _totalRetries, _pauseInterval, _shouldBeSnapshotCapable);
                    
                    if ((answer != null) && answer.getResult()) {
                        success = true;
                        s_logger.debug("Successfully deleted last snapshot: " + snapshotId + " for volume id: " + volumeId);
                    }
                    else if (answer != null) {
                        s_logger.error(answer.getDetails());
                    }
                }
            }
        }
        if(success){
            _snapshotDao.delete(snapshot.getId());
        }
        return success;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<SnapshotScheduleVO> findRecurringSnapshotSchedule(Long volumeId, Long policyId) {
        // List only future schedules, not past ones.
        List<SnapshotScheduleVO> snapshotSchedules = new ArrayList<SnapshotScheduleVO>();
        if (policyId == null) {
            List<SnapshotPolicyVO> policyInstances = listPoliciesforVolume(volumeId);
            for (SnapshotPolicyVO policyInstance: policyInstances) {
                SnapshotScheduleVO snapshotSchedule =
                    _snapshotScheduleDao.getCurrentSchedule(volumeId, policyInstance.getId(), false);
                snapshotSchedules.add(snapshotSchedule);
            }
        }
        else {
            snapshotSchedules.add(_snapshotScheduleDao.getCurrentSchedule(volumeId, policyId, false));
        }
        return snapshotSchedules;
    }
    
	@Override
	public SnapshotPolicyVO getPolicyForVolumeByInterval(long volumeId,
			short interval) {
	    return _snapshotPolicyDao.findOneByVolumeInterval(volumeId, interval);
	}
        
	@Override
    public boolean configure(String name, Map<String, Object> params)
    throws ConfigurationException {
        _name = name;

        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        if (configDao == null) {
            throw new ConfigurationException("Unable to get the configuration dao.");
        }
        
        DateUtil.IntervalType.HOURLY.setMax(NumbersUtil.parseInt(configDao.getValue("snapshot.max.hourly"), HOURLYMAX));
        DateUtil.IntervalType.DAILY.setMax(NumbersUtil.parseInt(configDao.getValue("snapshot.max.daily"), DAILYMAX));
        DateUtil.IntervalType.WEEKLY.setMax(NumbersUtil.parseInt(configDao.getValue("snapshot.max.weekly"), WEEKLYMAX));
        DateUtil.IntervalType.MONTHLY.setMax(NumbersUtil.parseInt(configDao.getValue("snapshot.max.monthly"), MONTHLYMAX));
        _totalRetries = NumbersUtil.parseInt(configDao.getValue("total.retries"), 4);
        _pauseInterval = 2*NumbersUtil.parseInt(configDao.getValue("ping.interval"), 60);
        
        SearchBuilder<SnapshotPolicyRefVO> policySearch = _snapPolicyRefDao.createSearchBuilder();
        policySearch.and("policyId", policySearch.entity().getPolicyId(), SearchCriteria.Op.EQ);
        
        PolicySnapshotSearch = _snapshotDao.createSearchBuilder();
        PolicySnapshotSearch.join("policy", policySearch, policySearch.entity().getSnapshotId(), PolicySnapshotSearch.entity().getId());
        policySearch.done();
        PolicySnapshotSearch.done();
        
        PoliciesForSnapSearch = _snapshotPolicyDao.createSearchBuilder();
        
        SearchBuilder<SnapshotPolicyRefVO> policyRefSearch = _snapPolicyRefDao.createSearchBuilder();
        policyRefSearch.and("snapshotId", policyRefSearch.entity().getSnapshotId(), SearchCriteria.Op.EQ);
        
        PoliciesForSnapSearch.join("policyRef", policyRefSearch, policyRefSearch.entity().getPolicyId(), PoliciesForSnapSearch.entity().getId());
        policyRefSearch.done();
        PoliciesForSnapSearch.done();
        s_logger.info("Snapshot Manager is configured.");

        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

}
