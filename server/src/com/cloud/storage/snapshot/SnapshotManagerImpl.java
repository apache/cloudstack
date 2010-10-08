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
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;
import javax.persistence.EntityExistsException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BackupSnapshotAnswer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.DeleteSnapshotBackupCommand;
import com.cloud.agent.api.DeleteSnapshotsDirCommand;
import com.cloud.agent.api.ManageSnapshotAnswer;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.api.BaseCmd;
import com.cloud.api.commands.CreateSnapshotCmd;
import com.cloud.api.commands.CreateVolumeCmd;
import com.cloud.async.AsyncJobExecutor;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.async.executor.SnapshotOperationParam;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.EventState;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.dao.DetailsDao;
import com.cloud.host.dao.HostDao;
import com.cloud.serializer.GsonHelper;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotScheduleVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Snapshot.SnapshotType;
import com.cloud.storage.Snapshot.Status;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.SnapshotScheduleDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
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
    public long createSnapshotAsync(long userId, long volumeId, long policyId) {
        VolumeVO volume = _volsDao.findById(volumeId);
        
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(volume.getAccountId());
        event.setType(EventTypes.EVENT_SNAPSHOT_CREATE);
        event.setState(EventState.Scheduled);
        event.setDescription("Scheduled async job of snapshot creation for volume:"+volumeId);
        event = _eventDao.persist(event);
        
        SnapshotOperationParam param = new SnapshotOperationParam(userId, volume.getAccountId(), volumeId, policyId);
        param.setEventId(event.getId());
        Gson gson = GsonHelper.getBuilder().create();

        AsyncJobVO job = new AsyncJobVO();
        job.setUserId(userId);
        job.setAccountId(volume.getAccountId());
        job.setCmd("CreateSnapshot");
        job.setCmdInfo(gson.toJson(param));
        job.setCmdOriginator(CreateSnapshotCmd.getResultObjectName());
        
        return _asyncMgr.submitAsyncJob(job, true);
    }
    
    private boolean isVolumeDirty(long volumeId,Long policy) {
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

    private boolean shouldRunSnapshot(long userId, VolumeVO volume, long policyId)
            throws InvalidParameterValueException, ResourceAllocationException {
        boolean runSnap = isVolumeDirty(volume.getId(), policyId);

        /*
         * // Check if the resource limit for snapshots has been exceeded
         * //UserVO user = _userDao.findById(userId); //AccountVO account =
         * _accountDao.findById(user.getAccountId()); AccountVO account =
         * _accountDao.findById(volume.getAccountId()); if
         * (_accountMgr.resourceLimitExceeded(account, ResourceType.snapshot)) {
         * throw newResourceAllocationException(
         * "The maximum number of snapshots for account " +
         * account.getAccountName() + " has been exceeded."); }
         */
        if (!runSnap) {
            s_logger.warn("Snapshot for volume " + volume.getId() + " not created. No policy assigned currently.");
        }

        return runSnap;
    }
    
    
    @Override @DB
    public SnapshotVO createSnapshot(long userId, long volumeId, long policyId) throws InvalidParameterValueException, ResourceAllocationException {
        // Get the async job id from the context.
        Long jobId = null;
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if(asyncExecutor != null) {
            // createSnapshot is always async. Hence asyncExecutor is always not null.
            AsyncJobVO job = asyncExecutor.getJob();
            jobId = job.getId();
        }
        
        VolumeVO volume = _volsDao.findById(volumeId);

        if (!shouldRunSnapshot(userId, volume, policyId)) {
            // A null snapshot is interpreted as snapshot creation failed which
            // is what we want to indicate
            return null;
        }

        SnapshotVO createdSnapshot = null;
        Long id = null;

        // Determine the name for this snapshot
        // Snapshot Name: VMInstancename + volumeName + timeString
        String timeString = DateUtil.getDateDisplayString(DateUtil.GMT_TIMEZONE, new Date(), DateUtil.YYYYMMDD_FORMAT);

        VMInstanceVO vmInstance = _vmDao.findById(volume.getInstanceId());
        String vmDisplayName = "detached";
        if (vmInstance != null) {
            vmDisplayName = vmInstance.getName();
        }
        String snapshotName = vmDisplayName + "_" + volume.getName() + "_" + timeString;

        // Create the Snapshot object and save it so we can return it to the
        // user
        SnapshotType snapshotType = SnapshotVO.getSnapshotType(policyId);
        SnapshotVO snapshotVO = new SnapshotVO(volume.getAccountId(), volume.getId(), null, snapshotName,
                (short) snapshotType.ordinal(), snapshotType.name());
        snapshotVO = _snapshotDao.persist(snapshotVO);
        id = snapshotVO.getId();
        assert id != null;

        // Send a ManageSnapshotCommand to the agent
        String vmName = _storageMgr.getVmNameOnVolume(volume);
        
        long  preId = _snapshotDao.getLastSnapshot(volumeId, id);
        
        String preSnapshotPath = null;
        // half of maxsnaps are delta snapshot
        // when there are half of maxsnaps or presnapshot has not backed up , create a full snapshot
        SnapshotVO preSnapshotVO = null;
        if( preId != 0) {
            preSnapshotVO = _snapshotDao.findById(preId);
            preSnapshotPath = preSnapshotVO.getPath();

        }

        ManageSnapshotCommand cmd = new ManageSnapshotCommand(id, volume.getPath(), preSnapshotPath, snapshotName, vmName);
        String basicErrMsg = "Failed to create snapshot for volume: " + volume.getId();
        ManageSnapshotAnswer answer = (ManageSnapshotAnswer) _storageMgr.sendToHostsOnStoragePool(volume.getPoolId(),
                cmd, basicErrMsg, _totalRetries, _pauseInterval, _shouldBeSnapshotCapable, volume.getInstanceId());
        // Update the snapshot in the database
        if ((answer != null) && answer.getResult()) {
            // The snapshot was successfully created
            if( preSnapshotPath != null && preSnapshotPath == answer.getSnapshotPath() ){
                //empty snapshot
                s_logger.debug("CreateSnapshot: this is empty snapshot, remove it ");
                createdSnapshot =  _snapshotDao.findById(id);
                // delete from the snapshots table
                _snapshotDao.expunge(id);
                
                createdSnapshot.setStatus(Status.EmptySnapshot);
                
            } else {
                long preSnapshotId = 0;
                if( preSnapshotVO != null && preSnapshotVO.getBackupSnapshotId() != null) {
                    preSnapshotId = preId;
                    // default delta snap number is 4
                    int deltaSnap = 4;
                    if( policyId != Snapshot.MANUAL_POLICY_ID ) {
                        SnapshotPolicyVO snapshotPolicy = _snapshotPolicyDao.findById(policyId);
                        int maxSnap = snapshotPolicy.getMaxSnaps();
                        deltaSnap = (maxSnap + 1) >> 1;
                    } else {
                        // check if there are policy for this volume
                        SnapshotPolicyVO snapshotPolicy = _snapshotPolicyDao.findOneByVolume(volumeId);
                        if( snapshotPolicy != null ) {
                            int maxSnap = snapshotPolicy.getMaxSnaps();
                            deltaSnap = (maxSnap + 1) >> 1;
                        }
                    }
                    int i;
                    for (i = 1; i < deltaSnap; i++) {
                        String prevBackupUuid = preSnapshotVO.getBackupSnapshotId();
                        // previous snapshot doesn't have backup, create a full snapshot
                        if (prevBackupUuid == null) {
                            preSnapshotId = 0;
                            break;
                        }
                        long preSSId = preSnapshotVO.getPrevSnapshotId();
                        if (preSSId == 0) {
                            break;
                        }
                        preSnapshotVO = _snapshotDao.findById(preSSId);
                    }
                    if (i >= deltaSnap) {
                        preSnapshotId = 0;
                    }
                }
                createdSnapshot = updateDBOnCreate(id, answer.getSnapshotPath(), preSnapshotId);
                // Get the snapshot_schedule table entry for this snapshot and
                // policy id.
                // Set the snapshotId to retrieve it back later.
                if( policyId != Snapshot.MANUAL_POLICY_ID) {
                    SnapshotScheduleVO snapshotSchedule = _snapshotScheduleDao.getCurrentSchedule(volumeId, policyId, true);
                    assert snapshotSchedule != null;
                    snapshotSchedule.setSnapshotId(id);
                    _snapshotScheduleDao.update(snapshotSchedule.getId(), snapshotSchedule);
                }
            }

        } else {
            if (answer != null) {
                s_logger.error(answer.getDetails());
            }
            // The snapshot was not successfully created
            createdSnapshot =  _snapshotDao.findById(id);
            // delete from the snapshots table
            _snapshotDao.expunge(id);
            
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

        return createdSnapshot;
    }

    private SnapshotVO updateDBOnCreate(Long id, String snapshotPath, long preSnapshotId) {
        SnapshotVO createdSnapshot = _snapshotDao.findById(id);
//        Long volumeId = createdSnapshot.getVolumeId();
        createdSnapshot.setPath(snapshotPath);
        createdSnapshot.setStatus(Snapshot.Status.CreatedOnPrimary);
        createdSnapshot.setPrevSnapshotId(preSnapshotId);
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
            backupSnapshotToSecondaryStorage(userId, snapshot, 0);
            break;
        case BackedUp:
            // No need to do anything as snapshot has already been backed up.
        }
    }

    @Override
    @DB
    public boolean backupSnapshotToSecondaryStorage(long userId, SnapshotVO ss, long startEventId) {
        long snapshotId = ss.getId();
        SnapshotVO snapshot = null;
        try {
            snapshot = _snapshotDao.acquire(snapshotId);
            snapshot.setStatus(Snapshot.Status.BackingUp);
            _snapshotDao.update(snapshot.getId(), snapshot);
            
            long volumeId   = snapshot.getVolumeId();
            VolumeVO volume = _volsDao.lock(volumeId, true);
            
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
            
            long prevSnapshotId = snapshot.getPrevSnapshotId();
            if (prevSnapshotId > 0) {
                prevSnapshot = _snapshotDao.findById(prevSnapshotId);
                prevSnapshotUuid = prevSnapshot.getPath();
                prevBackupUuid = prevSnapshot.getBackupSnapshotId();

            }
            String firstBackupUuid = volume.getFirstSnapshotBackupUuid();
            boolean isVolumeInactive = _storageMgr.volumeInactive(volume);
            String vmName = _storageMgr.getVmNameOnVolume(volume);
            BackupSnapshotCommand backupSnapshotCommand =
                new BackupSnapshotCommand(primaryStoragePoolNameLabel,
                                          secondaryStoragePoolUrl,
                                          dcId,
                                          accountId,
                                          volumeId,
                                          snapshotUuid,
                                          snapshot.getName(),
                                          prevSnapshotUuid,
                                          prevBackupUuid,
                                          firstBackupUuid,
                                          isVolumeInactive,
                                          vmName);
            
            String backedUpSnapshotUuid = null;
            // By default, assume failed.
            String basicErrMsg = "Failed to backup snapshot id " + snapshot.getId() + " to secondary storage for volume: " + volumeId;
            boolean backedUp = false;
            BackupSnapshotAnswer answer = (BackupSnapshotAnswer) _storageMgr.sendToHostsOnStoragePool(volume.getPoolId(),
                                                                                                      backupSnapshotCommand,
                                                                                                      basicErrMsg,
                                                                                                      _totalRetries,
                                                                                                      _pauseInterval,
                                                                                                      _shouldBeSnapshotCapable,
                                                                                                      volume.getInstanceId());
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
                        _snapshotDao.expunge(prevSnapshot.getId());
                        
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

                }
            }
            else if (answer != null) {
                s_logger.error(answer.getDetails());
            }
            // Update the status in all cases.
            Transaction txn = Transaction.currentTxn();
            txn.start();
            
            SnapshotVO snapshotVO = _snapshotDao.findById(snapshotId);
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
                String eventParams = "id=" + snapshotId + "\nssName=" + snapshotName +"\nsize=" + volume.getSize()+"\ndcId=" + volume.getDataCenterId();
                event.setDescription("Backed up snapshot id: " + snapshotId + " to secondary for volume " + volumeId);
                event.setLevel(EventVO.LEVEL_INFO);
                event.setStartId(startEventId);
                event.setParameters(eventParams);
                _snapshotDao.update(snapshotId, snapshotVO);
            }
            else {
                // Just mark it as removed in the database. When the next snapshot it taken,
                // validate previous snapshot will fix the state.
                // It will
                // 1) Call backupSnapshotToSecondaryStorage and try again.
                // 2) Create the next Snapshot pretending this is a valid snapshot.
                // 3) backupSnapshotToSecondaryStorage of the next snapshot
                // will take care of cleaning up the state of this snapshot
                _snapshotDao.remove(snapshotId);
                event.setLevel(EventVO.LEVEL_ERROR);
                event.setDescription("Failed to backup snapshot id: " + snapshotId + " to secondary for volume " + volumeId);
            }
            // Save the event
            _eventDao.persist(event);
            txn.commit();
            
            return backedUp;
        } finally {
            if( snapshot != null ) {
                _snapshotDao.release(snapshotId);
            }
        }
        
    }

    @Override
    @DB
    public void postCreateSnapshot(long userId, long volumeId, long snapshotId, long policyId, boolean backedUp) {
        // Update the snapshot_policy_ref table with the created snapshot
        // Get the list of policies for this snapshot
        Transaction txn = Transaction.currentTxn();
        txn.start();
        if (backedUp) {
            // This is a manual create, so increment the count of snapshots for
            // this account
            if (policyId == Snapshot.MANUAL_POLICY_ID) {
                Snapshot snapshot = _snapshotDao.findById(snapshotId);
                _accountMgr.incrementResourceCount(snapshot.getAccountId(), ResourceType.snapshot);
            }
        }

        // Even if the current snapshot failed, we should schedule the next
        // recurring snapshot for this policy.
        if (policyId != Snapshot.MANUAL_POLICY_ID) {
            postCreateRecurringSnapshotForPolicy(userId, volumeId, snapshotId, policyId);
        }

        txn.commit();
    }

    
    private void postCreateRecurringSnapshotForPolicy(long userId, long volumeId, long snapshotId, long policyId) {
        //Use count query
        List<SnapshotVO> snaps = listSnapsforVolumeType(volumeId, SnapshotType.RECURRING.name());
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
        SnapshotVO lastSnapshot = null;
        _snapshotDao.remove(snapshotId);
        long lastId = snapshotId;
        while( true ) {
            lastSnapshot = _snapshotDao.findNextSnapshot(lastId);
            // prevsnapshotId equal 0, means it is a full snapshot
            if( lastSnapshot == null || lastSnapshot.getPrevSnapshotId() == 0)
                break;
            lastId = lastSnapshot.getId();
        }
        lastSnapshot = _snapshotDao.findById(lastId);
        while( lastSnapshot.getRemoved() != null ) {
            String BackupSnapshotId = lastSnapshot.getBackupSnapshotId();
            if( BackupSnapshotId != null ) {
                if( destroySnapshotBackUp(userId, lastId, policyId) ) {

                } else {
                    s_logger.debug("Destroying snapshot backup failed " + lastSnapshot);
                    break;
                }
            }
            postDeleteSnapshot(userId, lastId, policyId);
            lastId = lastSnapshot.getPrevSnapshotId();
            if( lastId == 0 ) {
                break;
            }
            lastSnapshot = _snapshotDao.findById(lastId);
        }
        return true;
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
        return true;
    }
    @Override @DB
    public boolean destroySnapshotBackUp(long userId, long snapshotId, long policyId) {
        boolean success = false;
        String details = null;
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);

        VolumeVO volume = _volsDao.findById(snapshot.getVolumeId());
        String primaryStoragePoolNameLabel = _storageMgr.getPrimaryStorageNameLabel(volume);
        String secondaryStoragePoolUrl = _storageMgr.getSecondaryStorageURL(volume.getDataCenterId());
        Long dcId = volume.getDataCenterId();
        Long accountId = volume.getAccountId();
        Long volumeId = volume.getId();

        String backupOfSnapshot = snapshot.getBackupSnapshotId();

        DeleteSnapshotBackupCommand cmd = new DeleteSnapshotBackupCommand(primaryStoragePoolNameLabel,
                secondaryStoragePoolUrl, dcId, accountId, volumeId, backupOfSnapshot, snapshot.getName());

        details = "Failed to destroy snapshot id:" + snapshotId + " for volume: " + volume.getId();
        Answer answer = _storageMgr.sendToHostsOnStoragePool(volume.getPoolId(), cmd, details, _totalRetries,
                _pauseInterval, _shouldBeSnapshotCapable, volume.getInstanceId());

        if ((answer != null) && answer.getResult()) {
            snapshot.setBackupSnapshotId(null);
            _snapshotDao.update(snapshotId, snapshot);
            // This is not the last snapshot.
            success = true;
            details = "Successfully deleted snapshot " + snapshotId + " for volumeId: " + volumeId + " and policyId "
                    + policyId;
            s_logger.debug(details);
        } else if (answer != null) {
            if (answer.getDetails() != null) {
                details = answer.getDetails();
            }
            s_logger.error(details);
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
    protected void postDeleteSnapshot(long userId, long snapshotId, long policyId) {
        // Remove the snapshot from the snapshots table and the snap_policy_ref table.
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
        _snapshotDao.expunge(snapshotId);
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
        	if(volume.getPoolId()==null){
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
        	DeleteSnapshotsDirCommand cmd = new DeleteSnapshotsDirCommand(primaryStoragePoolNameLabel, secondaryStoragePoolURL, dcId, accountId, volumeId, mostRecentSnapshotUuid, mostRecentSnapshot.getName());
        	String basicErrMsg = "Failed to destroy snapshotsDir for: " + volume.getId() + " under account: " + accountId;
        	Answer answer = null;
        	Long poolId = volume.getPoolId();
        	if (poolId != null) {
        	    // Retry only once for this command. There's low chance of failure because of a connection problem.
        	    answer = _storageMgr.sendToHostsOnStoragePool(poolId, cmd, basicErrMsg, 1, _pauseInterval, _shouldBeSnapshotCapable, volume.getInstanceId());
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
        	    if(_snapshotDao.expunge(snapshot.getId())){
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
        SnapshotPolicyVO policy = new SnapshotPolicyVO(volumeId, schedule, timezone, interval, maxSnaps);
        // Create an event
        EventVO event = new EventVO();
        try{
            policy = _snapshotPolicyDao.persist(policy);
            event.setType(EventTypes.EVENT_SNAPSHOT_POLICY_CREATE);
            event.setDescription("Successfully created snapshot policy with Id: "+ policy.getId());
        } catch (EntityExistsException e ) {
            policy = _snapshotPolicyDao.findOneByVolume(volumeId);
            try {
                policy = _snapshotPolicyDao.acquire(policy.getId());
                policy.setSchedule(schedule);
                policy.setTimezone(timezone);
                policy.setInterval(interval);
                policy.setMaxSnaps(maxSnaps);
                policy.setActive(true);
                _snapshotPolicyDao.update(policy.getId(), policy);
            } finally {
                if( policy != null) {
                    _snapshotPolicyDao.release(policy.getId());
                }
            }
            event.setType(EventTypes.EVENT_SNAPSHOT_POLICY_UPDATE);
            event.setDescription("Successfully updated snapshot policy with Id: "+ policy.getId());
        }
        event.setAccountId(accountId);
        event.setUserId(userId);
        event.setLevel(EventVO.LEVEL_INFO);
        _eventDao.persist(event);
        _snapSchedMgr.scheduleNextSnapshotJob(policy);
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
/*    
    @Override
    public List<SnapshotPolicyVO> listPoliciesforSnapshot(long snapshotId) {
        SearchCriteria<SnapshotPolicyVO> sc = PoliciesForSnapSearch.create();
        sc.setJoinParameters("policyRef", "snapshotId", snapshotId);
        return _snapshotPolicyDao.search(sc, null);
    }

    @Override
    public List<SnapshotVO> listSnapsforPolicy(long policyId, Filter filter) {
        SearchCriteria<SnapshotVO> sc = PolicySnapshotSearch.create();
        sc.setJoinParameters("policy", "policyId", policyId);
        return _snapshotDao.search(sc, filter);
    }
*/

    @Override
    public List<SnapshotVO> listSnapsforVolume(long volumeId) {
        return _snapshotDao.listByVolumeId(volumeId);
    }
    
    public List<SnapshotVO> listSnapsforVolumeType(long volumeId, String type) {
        return _snapshotDao.listByVolumeIdType(volumeId, type);
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
    	    _snapshotScheduleDao.expunge(snapshotSchedule.getId());
    	}
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
    public SnapshotPolicyVO getPolicyForVolume(long volumeId) {
        return _snapshotPolicyDao.findOneByVolume(volumeId);
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
