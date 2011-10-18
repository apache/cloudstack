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
import java.util.TimeZone;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BackupSnapshotAnswer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.DeleteSnapshotBackupCommand;
import com.cloud.agent.api.DeleteSnapshotsDirCommand;
import com.cloud.agent.api.ManageSnapshotAnswer;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.api.commands.CreateSnapshotPolicyCmd;
import com.cloud.api.commands.DeleteSnapshotCmd;
import com.cloud.api.commands.DeleteSnapshotPoliciesCmd;
import com.cloud.api.commands.ListRecurringSnapshotScheduleCmd;
import com.cloud.api.commands.ListSnapshotPoliciesCmd;
import com.cloud.api.commands.ListSnapshotsCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Snapshot.Status;
import com.cloud.storage.Snapshot.Type;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotScheduleVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.SwiftVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.SnapshotScheduleDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.SwiftDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.DateUtil.IntervalType;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;

@Local(value = { SnapshotManager.class, SnapshotService.class })
public class SnapshotManagerImpl implements SnapshotManager, SnapshotService, Manager {
    private static final Logger s_logger = Logger.getLogger(SnapshotManagerImpl.class);
    @Inject
    protected VMTemplateDao _templateDao;
    @Inject
    protected HostDao _hostDao;
    @Inject
    protected UserVmDao _vmDao;
    @Inject
    protected VolumeDao _volsDao;
    @Inject
    protected AccountDao _accountDao;
    @Inject
    protected DataCenterDao _dcDao;
    @Inject
    protected DiskOfferingDao _diskOfferingDao;
    @Inject
    protected UserDao _userDao;
    @Inject
    protected SnapshotDao _snapshotDao;
    @Inject
    protected StoragePoolDao _storagePoolDao;
    @Inject
    protected EventDao _eventDao;
    @Inject
    protected SnapshotPolicyDao _snapshotPolicyDao = null;
    @Inject
    protected SnapshotScheduleDao _snapshotScheduleDao;
    @Inject
    protected HostDetailsDao _detailsDao;
    @Inject
    protected DomainDao _domainDao;
    @Inject
    protected StorageManager _storageMgr;
    @Inject
    protected AgentManager _agentMgr;
    @Inject
    protected SnapshotScheduler _snapSchedMgr;
    @Inject
    protected AsyncJobManager _asyncMgr;
    @Inject
    protected AccountManager _accountMgr;
    @Inject
    protected ClusterDao _clusterDao;
    @Inject
    private UsageEventDao _usageEventDao;
    @Inject
    private SwiftDao _swiftDao;
    String _name;
    private int _totalRetries;
    private int _pauseInterval;
    private int _deltaSnapshotMax;
    private int _backupsnapshotwait;

    protected SearchBuilder<SnapshotVO> PolicySnapshotSearch;
    protected SearchBuilder<SnapshotPolicyVO> PoliciesForSnapSearch;

    private boolean isVolumeDirty(long volumeId, Long policy) {
        VolumeVO volume = _volsDao.findById(volumeId);
        boolean runSnap = true;

        if (volume.getInstanceId() == null) {
            long lastSnapId = _snapshotDao.getLastSnapshot(volumeId, 0);
            SnapshotVO lastSnap = _snapshotDao.findByIdIncludingRemoved(lastSnapId);
            if (lastSnap != null) {
                Date lastSnapTime = lastSnap.getCreated();
                if (lastSnapTime.after(volume.getUpdated())) {
                    runSnap = false;
                    s_logger.debug("Volume: " + volumeId + " is detached and last snap time is after Volume detach time. Skip snapshot for recurring policy");
                }
            }
        } else if (_storageMgr.volumeInactive(volume)) {
            // Volume is attached to a VM which is in Stopped state.
            long lastSnapId = _snapshotDao.getLastSnapshot(volumeId, 0);
            SnapshotVO lastSnap = _snapshotDao.findByIdIncludingRemoved(lastSnapId);
            if (lastSnap != null) {
                Date lastSnapTime = lastSnap.getCreated();
                VMInstanceVO vmInstance = _vmDao.findById(volume.getInstanceId());
                if (vmInstance != null) {
                    if (lastSnapTime.after(vmInstance.getUpdateTime())) {
                        runSnap = false;
                        s_logger.debug("Volume: " + volumeId + " is inactive and last snap time is after VM update time. Skip snapshot for recurring policy");
                    }
                }
            }
        }
        if (volume.getState() == Volume.State.Destroy || volume.getRemoved() != null) {
            s_logger.debug("Volume: " + volumeId + " is destroyed/removed. Not taking snapshot");
            runSnap = false;
        }

        return runSnap;
    }

    protected Answer sendToPool(Volume vol, Command cmd) {
        StoragePool pool = _storagePoolDao.findById(vol.getPoolId());
        VMInstanceVO vm = _vmDao.findById(vol.getInstanceId());

        long[] hostIdsToTryFirst = null;
        if (vm != null && vm.getHostId() != null) {
            hostIdsToTryFirst = new long[] { vm.getHostId() };
        }

        List<Long> hostIdsToAvoid = new ArrayList<Long>();
        for (int retry = _totalRetries; retry >= 0; retry--) {
            try {
                Pair<Long, Answer> result = _storageMgr.sendToPool(pool, hostIdsToTryFirst, hostIdsToAvoid, cmd);
                if (result.second().getResult()) {
                    return result.second();
                }
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("The result for " + cmd.getClass().getName() + " is " + result.second().getDetails() + " through " + result.first());
                }
                hostIdsToAvoid.add(result.first());
            } catch (StorageUnavailableException e1) {
                s_logger.warn("Storage unavailable ", e1);
                return null;
            }

            try {
                Thread.sleep(_pauseInterval * 1000);
            } catch (InterruptedException e) {
            }

            s_logger.debug("Retrying...");
        }

        s_logger.warn("After " + _totalRetries + " retries, the command " + cmd.getClass().getName() + " did not succeed.");

        return null;
    }

    @Override
    public SnapshotVO createSnapshotOnPrimary(VolumeVO volume, Long policyId, Long snapshotId) {
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
        if (snapshot == null) {
            throw new CloudRuntimeException("Can not find snapshot " + snapshotId);
        }
        // Send a ManageSnapshotCommand to the agent
        String vmName = _storageMgr.getVmNameOnVolume(volume);
        long volumeId = volume.getId();
        long preId = _snapshotDao.getLastSnapshot(volumeId, snapshotId);

        String preSnapshotPath = null;
        SnapshotVO preSnapshotVO = null;
        if (preId != 0) {
            preSnapshotVO = _snapshotDao.findByIdIncludingRemoved(preId);
            if (preSnapshotVO != null && preSnapshotVO.getBackupSnapshotId() != null) {
                preSnapshotPath = preSnapshotVO.getPath();
            }
        }
        StoragePoolVO srcPool = _storagePoolDao.findById(volume.getPoolId());
        ManageSnapshotCommand cmd = new ManageSnapshotCommand(snapshotId, volume.getPath(), srcPool, preSnapshotPath, snapshot.getName(), vmName);
      
        ManageSnapshotAnswer answer = (ManageSnapshotAnswer) sendToPool(volume, cmd);
        // Update the snapshot in the database
        if ((answer != null) && answer.getResult()) {
            // The snapshot was successfully created
            if (preSnapshotPath != null && preSnapshotPath.equals(answer.getSnapshotPath())) {
                // empty snapshot
                s_logger.debug("CreateSnapshot: this is empty snapshot ");
                snapshot.setPath(preSnapshotPath);
                snapshot.setBackupSnapshotId(preSnapshotVO.getBackupSnapshotId());
                snapshot.setStatus(Snapshot.Status.BackedUp);
                snapshot.setPrevSnapshotId(preId);
                snapshot.setSecHostId(preSnapshotVO.getSecHostId());
                _snapshotDao.update(snapshotId, snapshot);
            } else {
                long preSnapshotId = 0;
                if (preSnapshotVO != null && preSnapshotVO.getBackupSnapshotId() != null) {
                    preSnapshotId = preId;
                    // default delta snap number is 16
                    int deltaSnap = _deltaSnapshotMax;

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
                        preSnapshotVO = _snapshotDao.findByIdIncludingRemoved(preSSId);
                    }
                    if (i >= deltaSnap) {
                        preSnapshotId = 0;
                    }
                }
                snapshot = updateDBOnCreate(snapshotId, answer.getSnapshotPath(), preSnapshotId);
            }
            // Get the snapshot_schedule table entry for this snapshot and
            // policy id.
            // Set the snapshotId to retrieve it back later.
            if (policyId != Snapshot.MANUAL_POLICY_ID) {
                SnapshotScheduleVO snapshotSchedule = _snapshotScheduleDao.getCurrentSchedule(volumeId, policyId, true);
                assert snapshotSchedule != null;
                snapshotSchedule.setSnapshotId(snapshotId);
                _snapshotScheduleDao.update(snapshotSchedule.getId(), snapshotSchedule);
            }

        } else {
            if (answer != null) {
                s_logger.error(answer.getDetails());
            }

            // delete from the snapshots table
            _snapshotDao.expunge(snapshotId);
            throw new CloudRuntimeException("Creating snapshot for volume " + volumeId + " on primary storage failed.");
        }

        return snapshot;
    }

    public SnapshotVO createSnapshotImpl(long volumeId, long policyId) throws ResourceAllocationException {
        return null;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_SNAPSHOT_CREATE, eventDescription = "creating snapshot", async = true)
    public SnapshotVO createSnapshot(Long volumeId, Long policyId, Long snapshotId) {
        VolumeVO v = _volsDao.findById(volumeId);
        Account owner = _accountMgr.getAccount(v.getAccountId());
        SnapshotVO snapshot = null;
        VolumeVO volume = null;
        boolean backedUp = false;
        // does the caller have the authority to act on this volume
        _accountMgr.checkAccess(UserContext.current().getCaller(), null, v);
        try {
            if (v != null && _volsDao.getHypervisorType(v.getId()).equals(HypervisorType.KVM)) {
                /* KVM needs to lock on the vm of volume, because it takes snapshot on behalf of vm, not volume */
                UserVmVO uservm = _vmDao.findById(v.getInstanceId());
                if (uservm != null) {
                    UserVmVO vm = _vmDao.acquireInLockTable(uservm.getId(), 10);
                    if (vm == null) {
                        throw new CloudRuntimeException("Creating snapshot failed due to volume:" + volumeId + " is being used, try it later ");
                    }
                }
            }
            Long poolId = v.getPoolId();
            if (poolId == null) {
                throw new CloudRuntimeException("You cannot take a snapshot of a volume until it has been attached to an instance");
            }

            if (_volsDao.getHypervisorType(v.getId()).equals(HypervisorType.KVM)) {
                StoragePoolVO storagePool = _storagePoolDao.findById(v.getPoolId());
                ClusterVO cluster = _clusterDao.findById(storagePool.getClusterId());
                List<HostVO> hosts = _hostDao.listByCluster(cluster.getId());
                if (hosts != null && !hosts.isEmpty()) {
                    HostVO host = hosts.get(0);
                    if (!hostSupportSnapsthot(host)) {
                        _snapshotDao.expunge(snapshotId);
                        throw new CloudRuntimeException("KVM Snapshot is not supported on cluster: " + host.getId());
                    }
                }
            }

            // if volume is attached to a vm in destroyed or expunging state; disallow
            if (v.getInstanceId() != null) {
                UserVmVO userVm = _vmDao.findById(v.getInstanceId());
                if (userVm != null) {
                    if (userVm.getState().equals(State.Destroyed) || userVm.getState().equals(State.Expunging)) {
                        _snapshotDao.expunge(snapshotId);
                        throw new CloudRuntimeException("Creating snapshot failed due to volume:" + volumeId + " is associated with vm:" + userVm.getInstanceName() + " is in "
                                + userVm.getState().toString() + " state");
                    }
                    
                    if(userVm.getHypervisorType() == HypervisorType.VMware) {
                    	List<SnapshotVO> activeSnapshots = _snapshotDao.listByInstanceId(v.getInstanceId(), Snapshot.Status.Creating,  Snapshot.Status.CreatedOnPrimary,  Snapshot.Status.BackingUp);
                    	if(activeSnapshots.size() > 1)
                            throw new CloudRuntimeException("There is other active snapshot tasks on the instance to which the volume is attached, please try again later");
                    }
                }
            }

            volume = _volsDao.acquireInLockTable(volumeId, 10);
            if (volume == null) {
                _snapshotDao.expunge(snapshotId);
                volume = _volsDao.findById(volumeId);
                if (volume == null) {
                    throw new CloudRuntimeException("Creating snapshot failed due to volume:" + volumeId + " doesn't exist");
                } else {
                    volume = null;
                    throw new CloudRuntimeException("Creating snapshot failed due to volume:" + volumeId + " is being used, try it later ");
                }
            }

            snapshot = createSnapshotOnPrimary(volume, policyId, snapshotId);
            if (snapshot != null) {
                if (snapshot.getStatus() == Snapshot.Status.CreatedOnPrimary) {
                    backedUp = backupSnapshotToSecondaryStorage(snapshot);
                } else if (snapshot.getStatus() == Snapshot.Status.BackedUp) {
                    // For empty snapshot we set status to BackedUp in createSnapshotOnPrimary
                    backedUp = true;
                } else {
                    snapshot.setStatus(Status.Error);
                    _snapshotDao.update(snapshot.getId(), snapshot);
                    throw new CloudRuntimeException("Failed to create snapshot: " + snapshot + " on primary storage");
                }
                if (!backedUp) {
                    snapshot.setStatus(Status.Error);
                    _snapshotDao.update(snapshot.getId(), snapshot);
                    throw new CloudRuntimeException("Created snapshot: " + snapshot + " on primary but failed to backup on secondary");
                }
            } else {
                throw new CloudRuntimeException("Failed to create snapshot: " + snapshot + " on primary storage");
            }
        } finally {
            // Cleanup jobs to do after the snapshot has been created; decrement resource count
            if (snapshot != null) {
                postCreateSnapshot(volumeId, snapshot.getId(), policyId, backedUp);
                //Check if the snapshot was removed while backingUp. If yes, do not log snapshot create usage event
                SnapshotVO freshSnapshot = _snapshotDao.findById(snapshot.getId());
                if ((freshSnapshot != null) && backedUp) {
                    UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_SNAPSHOT_CREATE, snapshot.getAccountId(), snapshot.getDataCenterId(), snapshotId, snapshot.getName(), null, null,
                            v.getSize());
                    _usageEventDao.persist(usageEvent);
                }
                if( !backedUp ) {

                    snapshot.setStatus(Status.Error);
                    _snapshotDao.update(snapshot.getId(), snapshot);
                } else {
                    _accountMgr.incrementResourceCount(owner.getId(), ResourceType.snapshot);
                }
            } else {
            	snapshot = _snapshotDao.findById(snapshotId);
            	if (snapshot != null) {
            		snapshot.setStatus(Status.Error);
            		_snapshotDao.update(snapshotId, snapshot);
            	}
            }
            
            if ( volume != null ) {
                _volsDao.releaseFromLockTable(volumeId);
            }
        }

        return snapshot;
    }

    private SnapshotVO updateDBOnCreate(Long id, String snapshotPath, long preSnapshotId) {
        SnapshotVO createdSnapshot = _snapshotDao.findByIdIncludingRemoved(id);
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
            backupSnapshotToSecondaryStorage(snapshot);
            break;
        case BackedUp:
            // No need to do anything as snapshot has already been backed up.
        }
    }
    
    
    void setupSnapshotChain(SnapshotVO ss, List<String> snapshots){
        
    }
    
    
    void downloadSnapshotFromSwift(SnapshotVO ss) {
    }
    @Override
    public void downloadSnapshotsFromSwift(SnapshotVO ss) {
        
        List<String> snapshots = new ArrayList<String>(20);
        SnapshotVO tss = ss;
        try {
            while(true) {
                assert tss.getSwiftName() != null : " SwiftName is null";
                downloadSnapshotFromSwift(tss);
                snapshots.add(tss.getSwiftName().split("_")[0]);
                if( tss.getPrevSnapshotId() == 0) 
                    break;
                Long id = tss.getPrevSnapshotId();
                tss = _snapshotDao.findById(id);
                assert tss != null : " can not find snapshot " + id;
            }
            
            setupSnapshotChain(ss, snapshots);
        } catch (Exception e) {
            throw new CloudRuntimeException("downloadSnapshotsFromSwift failed due to " + e.toString());
        }
        
        
        
    }
    
    private SwiftTO toSwiftTO(SwiftVO swift) {
        return new SwiftTO(swift.getHostName(), swift.getAccount(), swift.getUserName(), swift.getToken());
    }

    @Override
    @DB
    public boolean backupSnapshotToSecondaryStorage(SnapshotVO ss) {
        long snapshotId = ss.getId();
        SnapshotVO snapshot = _snapshotDao.acquireInLockTable(snapshotId);
        if (snapshot == null) {
            throw new CloudRuntimeException("Can not acquire lock for snapshot: " + ss);
        }
        try {

            snapshot.setStatus(Snapshot.Status.BackingUp);
            _snapshotDao.update(snapshot.getId(), snapshot);

            long volumeId = snapshot.getVolumeId();
            VolumeVO volume = _volsDao.lockRow(volumeId, true);

            String primaryStoragePoolNameLabel = _storageMgr.getPrimaryStorageNameLabel(volume);
            Long dcId = volume.getDataCenterId();
            Long accountId = volume.getAccountId();
            
            HostVO secHost = getSecHost(volumeId, volume.getDataCenterId());
            
            String secondaryStoragePoolUrl = secHost.getStorageUrl();
            String snapshotUuid = snapshot.getPath();
            // In order to verify that the snapshot is not empty,
            // we check if the parent of the snapshot is not the same as the parent of the previous snapshot.
            // We pass the uuid of the previous snapshot to the plugin to verify this.
            SnapshotVO prevSnapshot = null;
            String prevSnapshotUuid = null;
            String prevBackupUuid = null;


            SwiftVO swift= _swiftDao.findById(1L);
            
            long prevSnapshotId = snapshot.getPrevSnapshotId();
            if (prevSnapshotId > 0) {
                prevSnapshot = _snapshotDao.findByIdIncludingRemoved(prevSnapshotId);
                if ( prevSnapshot.getBackupSnapshotId() != null && swift == null) {
                    if (prevSnapshot.getVersion() != null && prevSnapshot.getVersion().equals("2.2")) {                   
                        prevBackupUuid = prevSnapshot.getBackupSnapshotId();
                        prevSnapshotUuid = prevSnapshot.getPath();
                    }
                } else if ( prevSnapshot.getSwiftName() != null && swift != null ) {
                    prevBackupUuid = prevSnapshot.getSwiftName();
                    prevSnapshotUuid = prevSnapshot.getPath();
                }
            }
            boolean isVolumeInactive = _storageMgr.volumeInactive(volume);
            String vmName = _storageMgr.getVmNameOnVolume(volume);
            StoragePoolVO srcPool = _storagePoolDao.findById(volume.getPoolId());
            BackupSnapshotCommand backupSnapshotCommand = new BackupSnapshotCommand(primaryStoragePoolNameLabel, secondaryStoragePoolUrl, dcId, accountId, volumeId, snapshot.getId(), volume.getPath(), srcPool, snapshotUuid,
                    snapshot.getName(), prevSnapshotUuid, prevBackupUuid, isVolumeInactive, vmName, _backupsnapshotwait);

            if ( swift != null ) {
                backupSnapshotCommand.setSwift(toSwiftTO(swift));
            }
            
            String backedUpSnapshotUuid = null;
            // By default, assume failed.
            boolean backedUp = false;
            BackupSnapshotAnswer answer = (BackupSnapshotAnswer) sendToPool(volume, backupSnapshotCommand);
            if (answer != null && answer.getResult()) {
                backedUpSnapshotUuid = answer.getBackupSnapshotName();
                if (backedUpSnapshotUuid != null) {
                    backedUp = true;
                }
            } else if (answer != null) {
                s_logger.error(answer.getDetails());
            }
            // Update the status in all cases.
            Transaction txn = Transaction.currentTxn();
            txn.start();

            if (backedUp) {
                if (backupSnapshotCommand.getSwift() != null ) {
                    snapshot.setSwiftId(1L);
                    snapshot.setSwiftName(backedUpSnapshotUuid);
                } else {
                    snapshot.setSecHostId(secHost.getId());
                    snapshot.setBackupSnapshotId(backedUpSnapshotUuid);
                }
                if (answer.isFull()) {
                    snapshot.setPrevSnapshotId(0);
                }
                snapshot.setStatus(Snapshot.Status.BackedUp);
                _snapshotDao.update(snapshotId, snapshot);

            } else {
                s_logger.warn("Failed to back up snapshot on secondary storage, deleting the record from the DB");
                _snapshotDao.remove(snapshotId);
                UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_SNAPSHOT_DELETE, snapshot.getAccountId(), snapshot.getDataCenterId(), snapshotId, snapshot.getName(), null, null, 0L);
                _usageEventDao.persist(usageEvent);
            }
            txn.commit();

            return backedUp;
        } finally {
            if (snapshot != null) {
                _snapshotDao.releaseFromLockTable(snapshotId);
            }
        }

    }

    private HostVO getSecHost(long volumeId, long dcId) {
        Long id = _snapshotDao.getSecHostId(volumeId);
        if ( id != null) { 
            return _hostDao.findById(id);
        }
        return _storageMgr.getSecondaryStorageHost(dcId);
    }

    private Long getSnapshotUserId() {
        Long userId = UserContext.current().getCallerUserId();
        if (userId == null) {
            return User.UID_SYSTEM;
        }
        return userId;
    }

    @Override
    @DB
    public void postCreateSnapshot(Long volumeId, Long snapshotId, Long policyId, boolean backedUp) {
        Long userId = getSnapshotUserId();
        SnapshotVO snapshot = _snapshotDao.findByIdIncludingRemoved(snapshotId);
        // Even if the current snapshot failed, we should schedule the next
        // recurring snapshot for this policy.
        
        if (snapshot.isRecursive()) {
            postCreateRecurringSnapshotForPolicy(userId, volumeId, snapshotId, policyId);
        }
    }

    private void postCreateRecurringSnapshotForPolicy(long userId, long volumeId, long snapshotId, long policyId) {
        // Use count query
        SnapshotVO spstVO = _snapshotDao.findById(snapshotId);
        Type type = spstVO.getType();
        int maxSnaps = type.getMax();

        List<SnapshotVO> snaps = listSnapsforVolumeType(volumeId, type);
        SnapshotPolicyVO policy = _snapshotPolicyDao.findById(policyId);
        if (policy != null && policy.getMaxSnaps() < maxSnaps) {
            maxSnaps = policy.getMaxSnaps();
        }
        while (snaps.size() > maxSnaps && snaps.size() > 1) {
            SnapshotVO oldestSnapshot = snaps.get(0);
            long oldSnapId = oldestSnapshot.getId();
            s_logger.debug("Max snaps: " + policy.getMaxSnaps() + " exceeded for snapshot policy with Id: " + policyId + ". Deleting oldest snapshot: " + oldSnapId);
            deleteSnapshotInternal(oldSnapId);
            snaps.remove(oldestSnapshot);
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_SNAPSHOT_DELETE, eventDescription = "deleting snapshot", async = true)
    public boolean deleteSnapshot(DeleteSnapshotCmd cmd) {
        Long snapshotId = cmd.getId();
        Account caller = UserContext.current().getCaller();

        // Verify parameters
        Snapshot snapshotCheck = _snapshotDao.findByIdIncludingRemoved(snapshotId.longValue());
        if (snapshotCheck == null) {
            throw new InvalidParameterValueException("unable to find a snapshot with id " + snapshotId);
        }
        if( !Status.BackedUp.equals(snapshotCheck.getStatus() ) ) {
            throw new InvalidParameterValueException("Can't delete snapshotshot " + snapshotId + " due to it is not in BackedUp Status");
        }

       _accountMgr.checkAccess(caller, null, snapshotCheck);

        return deleteSnapshotInternal(snapshotId);
    }

    @DB
    private boolean deleteSnapshotInternal(Long snapshotId) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Calling deleteSnapshot for snapshotId: " + snapshotId);
        }
        SnapshotVO lastSnapshot = null;
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
        if (snapshot.getBackupSnapshotId() != null) {
            List<SnapshotVO> snaps = _snapshotDao.listByBackupUuid(snapshot.getVolumeId(), snapshot.getBackupSnapshotId());
            if ( snaps != null && snaps.size() > 1 ) {
                snapshot.setBackupSnapshotId(null);
                _snapshotDao.update(snapshot.getId(), snapshot);
            }
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();
        _snapshotDao.remove(snapshotId);
        if (snapshot.getStatus() == Snapshot.Status.BackedUp) {
        	UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_SNAPSHOT_DELETE, snapshot.getAccountId(), snapshot.getDataCenterId(), snapshotId, snapshot.getName(), null, null, 0L);
        	_usageEventDao.persist(usageEvent);
        }
        _accountMgr.decrementResourceCount(snapshot.getAccountId(), ResourceType.snapshot);
        txn.commit();

        long lastId = snapshotId;
        boolean destroy = false;
        while (true) {
            lastSnapshot = _snapshotDao.findNextSnapshot(lastId);
            if (lastSnapshot == null) {
                // if all snapshots after this snapshot in this chain are removed, remove those snapshots.
                destroy = true;
                break;
            }
            if (lastSnapshot.getRemoved() == null) {
                // if there is one child not removed, then can not remove back up snapshot.
                break;
            }
            lastId = lastSnapshot.getId();
        }
        if (destroy) {
            lastSnapshot = _snapshotDao.findByIdIncludingRemoved(lastId);
            while (lastSnapshot.getRemoved() != null) {
                String BackupSnapshotId = lastSnapshot.getBackupSnapshotId();
                if (BackupSnapshotId != null) {
                    List<SnapshotVO> snaps = _snapshotDao.listByBackupUuid(lastSnapshot.getVolumeId(), BackupSnapshotId);
                    if ( snaps != null && snaps.size() > 1) {
                        lastSnapshot.setBackupSnapshotId(null);
                        _snapshotDao.update(lastSnapshot.getId(), lastSnapshot);
                    } else {
                        if (destroySnapshotBackUp(lastId)) {

                        } else {
                            s_logger.debug("Destroying snapshot backup failed " + lastSnapshot);
                            break;
                        }
                    }
                }
                lastId = lastSnapshot.getPrevSnapshotId();
                if (lastId == 0) {
                    break;
                }
                lastSnapshot = _snapshotDao.findByIdIncludingRemoved(lastId);
            }
        }
        return true;
    }

    @Override
    @DB
    public boolean destroySnapshot(long userId, long snapshotId, long policyId) {
        return true;
    }

    @Override
    public String getSecondaryStorageURL(SnapshotVO snapshot) {
        HostVO secHost = _hostDao.findById(snapshot.getSecHostId());
        return secHost.getStorageUrl();
        
    }
    
    @Override
    @DB
    public boolean destroySnapshotBackUp(long snapshotId) {
        boolean success = false;
        String details;
        SnapshotVO snapshot = _snapshotDao.findByIdIncludingRemoved(snapshotId);
        if (snapshot == null) {
            throw new CloudRuntimeException("Destroying snapshot " + snapshotId + " backup failed due to unable to find snapshot ");
        }

        String secondaryStoragePoolUrl = getSecondaryStorageURL(snapshot);
        Long dcId = snapshot.getDataCenterId();
        Long accountId = snapshot.getAccountId();
        Long volumeId = snapshot.getVolumeId();
        HypervisorType hvType = snapshot.getHypervisorType();

        String backupOfSnapshot = snapshot.getBackupSnapshotId();
        if (backupOfSnapshot == null) {
            return true;
        }
        DeleteSnapshotBackupCommand cmd = new DeleteSnapshotBackupCommand(null, secondaryStoragePoolUrl, dcId, accountId, volumeId, backupOfSnapshot, snapshot.getName());

        snapshot.setBackupSnapshotId(null);
        _snapshotDao.update(snapshotId, snapshot);

        Answer answer = _agentMgr.sendTo(dcId, hvType, cmd);

        if ((answer != null) && answer.getResult()) {

            // This is not the last snapshot.
            success = true;
            details = "Successfully deleted snapshot " + snapshotId + " for volumeId: " + volumeId;
            s_logger.debug(details);
        } else if (answer != null) {
            details = "Failed to destroy snapshot id:" + snapshotId + " for volume: " + volumeId + " due to ";
            if (answer.getDetails() != null) {
                details += answer.getDetails();
            }
            s_logger.error(details);
        }
        return success;

    }

    @Override
    public List<SnapshotVO> listSnapshots(ListSnapshotsCmd cmd) {
        Long volumeId = cmd.getVolumeId();
        Boolean isRecursive = cmd.isRecursive();

        // Verify parameters
        if (volumeId != null) {
            VolumeVO volume = _volsDao.findById(volumeId);
            if (volume != null) {
                _accountMgr.checkAccess(UserContext.current().getCaller(), null, volume);
            }
        }

        Account account = UserContext.current().getCaller();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;
        if ((account == null) || _accountMgr.isAdmin(account.getType())) {
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Unable to list templates for domain " + domainId + ", permission denied.");
                }
            } else if ((account != null) && ((account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || (account.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN))) {
                domainId = account.getDomainId();
                isRecursive = true;
            }

            if (domainId != null && accountName != null) {
                Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                if (userAccount != null) {
                    accountId = userAccount.getId();
                } else {
                    throw new InvalidParameterValueException("Could not find account:" + accountName + " in domain:" + domainId);
                }
            }
        } else {
            accountId = account.getId();
        }

        if (isRecursive == null) {
            isRecursive = false;
        }

        Object name = cmd.getSnapshotName();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Object snapshotTypeStr = cmd.getSnapshotType();
        Object intervalTypeStr = cmd.getIntervalType();

        Filter searchFilter = new Filter(SnapshotVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<SnapshotVO> sb = _snapshotDao.createSearchBuilder();
        sb.and("status", sb.entity().getStatus(), SearchCriteria.Op.EQ);
        sb.and("volumeId", sb.entity().getVolumeId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("snapshotTypeEQ", sb.entity().getsnapshotType(), SearchCriteria.Op.IN);
        sb.and("snapshotTypeNEQ", sb.entity().getsnapshotType(), SearchCriteria.Op.NEQ);

        if ((accountId == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<AccountVO> accountSearch = _accountDao.createSearchBuilder();
            sb.join("accountSearch", accountSearch, sb.entity().getAccountId(), accountSearch.entity().getId(), JoinType.INNER);

            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            if (isRecursive) {
                domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            } else {
                domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.EQ);
            }
            accountSearch.join("domainSearch", domainSearch, accountSearch.entity().getDomainId(), domainSearch.entity().getId(), JoinType.INNER);
        }

        SearchCriteria<SnapshotVO> sc = sb.create();

        if (volumeId != null) {
            sc.setParameters("volumeId", volumeId);
        }

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (keyword != null) {
            SearchCriteria<SnapshotVO> ssc = _snapshotDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (accountId != null) {
            sc.setParameters("accountId", accountId);
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            SearchCriteria<?> joinSearch = sc.getJoin("accountSearch");
            if (isRecursive) {
                joinSearch.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
            } else {
                joinSearch.setJoinParameters("domainSearch", "path", domain.getPath());
            }
        }

        if (snapshotTypeStr != null) {
            Type snapshotType = SnapshotVO.getSnapshotType((String) snapshotTypeStr);
            if (snapshotType == null) {
                throw new InvalidParameterValueException("Unsupported snapshot type " + snapshotTypeStr);
            }
            if (snapshotType == Type.RECURRING) {
                sc.setParameters("snapshotTypeEQ", Type.HOURLY.ordinal(), Type.DAILY.ordinal(), Type.WEEKLY.ordinal(), Type.MONTHLY.ordinal());
            } else {
                sc.setParameters("snapshotTypeEQ", snapshotType.ordinal());
            }
        } else if (intervalTypeStr != null && volumeId != null) {
            Type type = SnapshotVO.getSnapshotType((String) intervalTypeStr);
            if (type == null) {
                throw new InvalidParameterValueException("Unsupported snapstho interval type " + intervalTypeStr);
            }
            sc.setParameters("snapshotTypeEQ", type.ordinal());
        } else {
            // Show only MANUAL and RECURRING snapshot types
            sc.setParameters("snapshotTypeNEQ", Snapshot.Type.TEMPLATE.ordinal());
        }

        return _snapshotDao.search(sc, searchFilter);
    }

    @Override
    public boolean deleteSnapshotDirsForAccount(long accountId) {

        List<VolumeVO> volumes = _volsDao.findByAccount(accountId);
        // The above call will list only non-destroyed volumes.
        // So call this method before marking the volumes as destroyed.
        // i.e Call them before the VMs for those volumes are destroyed.
        boolean success = true;
        for (VolumeVO volume : volumes) {
            if (volume.getPoolId() == null) {
                continue;
            }
            Long volumeId = volume.getId();
            Long dcId = volume.getDataCenterId();
            String primaryStoragePoolNameLabel = _storageMgr.getPrimaryStorageNameLabel(volume);
            if (_snapshotDao.listByVolumeIdIncludingRemoved(volumeId).isEmpty()) {
                // This volume doesn't have any snapshots. Nothing do delete.
                continue;
            }
            
            List<HostVO> ssHosts = _hostDao.listSecondaryStorageHosts(dcId);
            for ( HostVO ssHost : ssHosts ) {           
                DeleteSnapshotsDirCommand cmd = new DeleteSnapshotsDirCommand(primaryStoragePoolNameLabel, ssHost.getStorageUrl(), dcId, accountId, volumeId, volume.getPath());
                Answer answer = null;
                Long poolId = volume.getPoolId();
                if (poolId != null) {
                    // Retry only once for this command. There's low chance of failure because of a connection problem.
                    try {
                        answer = _storageMgr.sendToPool(poolId, cmd);
                    } catch (StorageUnavailableException e) {
                    }
                } else {
                    s_logger.info("Pool id for volume id: " + volumeId + " belonging to account id: " + accountId + " is null. Assuming the snapshotsDir for the account has already been deleted");
                }
    
                if (success) {
                    // SnapshotsDir has been deleted for the volumes so far.
                    success = (answer != null) && answer.getResult();
                    if (success) {
                        s_logger.debug("Deleted snapshotsDir for volume: " + volumeId + " under account: " + accountId);
                    } else if (answer != null) {
                        s_logger.error(answer.getDetails());
                    }
                }
            }

            // Either way delete the snapshots for this volume.
            List<SnapshotVO> snapshots = listSnapsforVolume(volumeId);
            for (SnapshotVO snapshot : snapshots) {
                if (_snapshotDao.expunge(snapshot.getId())) {
                    if (snapshot.getType() == Type.MANUAL) {
                        _accountMgr.decrementResourceCount(accountId, ResourceType.snapshot);
                    }

                    // Log event after successful deletion
                    UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_SNAPSHOT_DELETE, snapshot.getAccountId(), volume.getDataCenterId(), snapshot.getId(), snapshot.getName(), null, null,
                            volume.getSize());
                    _usageEventDao.persist(usageEvent);
                }
            }
        }

        // Returns true if snapshotsDir has been deleted for all volumes.
        return success;
    }

    @Override
    @DB
    public SnapshotPolicyVO createPolicy(CreateSnapshotPolicyCmd cmd) {
        Long volumeId = cmd.getVolumeId();
        VolumeVO volume = _volsDao.findById(cmd.getVolumeId());
        if (volume == null) {
            throw new InvalidParameterValueException("Failed to create snapshot policy, unable to find a volume with id " + volumeId);
        }

        if (volume.getState() != Volume.State.Ready) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " is not in " + Volume.State.Ready + " state but " + volume.getState() + ". Cannot take snapshot.");
        }

        if ( volume.getTemplateId() != null ) {
            VMTemplateVO  template = _templateDao.findById(volume.getTemplateId());
            if( template != null && template.getTemplateType() == Storage.TemplateType.SYSTEM ) {
                throw new InvalidParameterValueException("VolumeId: " + volumeId + " is for System VM , Creating snapshot against System VM volumes is not supported");
            }
        }

        AccountVO owner = _accountDao.findById(volume.getAccountId());
        DomainVO domain = _domainDao.findById(owner.getDomainId());
        
        _accountMgr.checkAccess(UserContext.current().getCaller(), null, volume);

        Long instanceId = volume.getInstanceId();
        if (instanceId != null) {
            // It is not detached, but attached to a VM
            if (_vmDao.findById(instanceId) == null) {
                // It is not a UserVM but a SystemVM or DomR
                throw new InvalidParameterValueException("Failed to create snapshot policy, snapshots of volumes attached to System or router VM are not allowed");
            }
        }
        IntervalType intvType = DateUtil.IntervalType.getIntervalType(cmd.getIntervalType());
        if (intvType == null) {
            throw new InvalidParameterValueException("Unsupported interval type " + cmd.getIntervalType());
        }
        Type type = getSnapshotType(intvType);

        TimeZone timeZone = TimeZone.getTimeZone(cmd.getTimezone());
        String timezoneId = timeZone.getID();
        if (!timezoneId.equals(cmd.getTimezone())) {
            s_logger.warn("Using timezone: " + timezoneId + " for running this snapshot policy as an equivalent of " + cmd.getTimezone());
        }
        try {
            DateUtil.getNextRunTime(intvType, cmd.getSchedule(), timezoneId, null);
        } catch (Exception e) {
            throw new InvalidParameterValueException("Invalid schedule: " + cmd.getSchedule() + " for interval type: " + cmd.getIntervalType());
        }

        if (cmd.getMaxSnaps() <= 0) {
            throw new InvalidParameterValueException("maxSnaps should be greater than 0");
        }

        int intervalMaxSnaps = type.getMax();
        if (cmd.getMaxSnaps() > intervalMaxSnaps) {
            throw new InvalidParameterValueException("maxSnaps exceeds limit: " + intervalMaxSnaps + " for interval type: " + cmd.getIntervalType());
        }

        // Verify that max doesn't exceed domain and account snapshot limits
        long accountLimit = _accountMgr.findCorrectResourceLimit(owner.getId(), ResourceType.snapshot);
        long domainLimit = _accountMgr.findCorrectResourceLimit(domain, ResourceType.snapshot);
        int max = cmd.getMaxSnaps().intValue();
        if (owner.getType() != Account.ACCOUNT_TYPE_ADMIN && ((accountLimit != -1 && max > accountLimit) || (domainLimit != -1 && max > domainLimit))) {
            throw new InvalidParameterValueException("Max number of snapshots shouldn't exceed the domain/account level snapshot limit");
        }

        SnapshotPolicyVO policy = _snapshotPolicyDao.findOneByVolumeInterval(volumeId, intvType);
        if (policy == null) {
            policy = new SnapshotPolicyVO(volumeId, cmd.getSchedule(), timezoneId, intvType, cmd.getMaxSnaps());
            policy = _snapshotPolicyDao.persist(policy);
            _snapSchedMgr.scheduleNextSnapshotJob(policy);
        } else {
            try {
                policy = _snapshotPolicyDao.acquireInLockTable(policy.getId());
                policy.setSchedule(cmd.getSchedule());
                policy.setTimezone(timezoneId);
                policy.setInterval((short) intvType.ordinal());
                policy.setMaxSnaps(cmd.getMaxSnaps());
                policy.setActive(true);
                _snapshotPolicyDao.update(policy.getId(), policy);
            } finally {
                if (policy != null) {
                    _snapshotPolicyDao.releaseFromLockTable(policy.getId());
                }
            }

        }
        return policy;
    }

    @Override
    public boolean deletePolicy(long userId, Long policyId) {
        SnapshotPolicyVO snapshotPolicy = _snapshotPolicyDao.findById(policyId);
        _snapSchedMgr.removeSchedule(snapshotPolicy.getVolumeId(), snapshotPolicy.getId());
        return _snapshotPolicyDao.remove(policyId);
    }

    @Override
    public List<SnapshotPolicyVO> listPoliciesforVolume(ListSnapshotPoliciesCmd cmd) {
        Long volumeId = cmd.getVolumeId();
        VolumeVO volume = _volsDao.findById(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Unable to find a volume with id " + volumeId);
        }
        _accountMgr.checkAccess(UserContext.current().getCaller(), null, volume);
        return listPoliciesforVolume(cmd.getVolumeId());
    }

    @Override
    public List<SnapshotPolicyVO> listPoliciesforVolume(long volumeId) {
        return _snapshotPolicyDao.listByVolumeId(volumeId);
    }

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

    @Override
    public List<SnapshotVO> listSnapsforVolume(long volumeId) {
        return _snapshotDao.listByVolumeId(volumeId);
    }

    public List<SnapshotVO> listSnapsforVolumeType(long volumeId, Type type) {
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
    public List<SnapshotScheduleVO> findRecurringSnapshotSchedule(ListRecurringSnapshotScheduleCmd cmd) {
        Long volumeId = cmd.getVolumeId();
        Long policyId = cmd.getSnapshotPolicyId();
        Account account = UserContext.current().getCaller();

        // Verify parameters
        VolumeVO volume = _volsDao.findById(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Failed to list snapshot schedule, unable to find a volume with id " + volumeId);
        }

        if (account != null) {
            long volAcctId = volume.getAccountId();
            if (_accountMgr.isAdmin(account.getType())) {
                Account userAccount = _accountDao.findById(Long.valueOf(volAcctId));
                if (!_domainDao.isChildDomain(account.getDomainId(), userAccount.getDomainId())) {
                    throw new PermissionDeniedException("Unable to list snapshot schedule for volume " + volumeId + ", permission denied.");
                }
            } else if (account.getId() != volAcctId) {
                throw new PermissionDeniedException("Unable to list snapshot schedule, account " + account.getAccountName() + " does not own volume id " + volAcctId);
            }
        }

        // List only future schedules, not past ones.
        List<SnapshotScheduleVO> snapshotSchedules = new ArrayList<SnapshotScheduleVO>();
        if (policyId == null) {
            List<SnapshotPolicyVO> policyInstances = listPoliciesforVolume(volumeId);
            for (SnapshotPolicyVO policyInstance : policyInstances) {
                SnapshotScheduleVO snapshotSchedule = _snapshotScheduleDao.getCurrentSchedule(volumeId, policyInstance.getId(), false);
                snapshotSchedules.add(snapshotSchedule);
            }
        } else {
            snapshotSchedules.add(_snapshotScheduleDao.getCurrentSchedule(volumeId, policyId, false));
        }
        return snapshotSchedules;
    }

    @Override
    public SnapshotPolicyVO getPolicyForVolume(long volumeId) {
        return _snapshotPolicyDao.findOneByVolume(volumeId);
    }

    public Type getSnapshotType(Long policyId) {
        if (policyId.equals(Snapshot.MANUAL_POLICY_ID)) {
            return Type.MANUAL;
        } else {
            SnapshotPolicyVO spstPolicyVO = _snapshotPolicyDao.findById(policyId);
            IntervalType intvType = DateUtil.getIntervalType(spstPolicyVO.getInterval());
            return getSnapshotType(intvType);
        }
    }

    public Type getSnapshotType(IntervalType intvType) {
        if (intvType.equals(IntervalType.HOURLY)) {
            return Type.HOURLY;
        } else if (intvType.equals(IntervalType.DAILY)) {
            return Type.DAILY;
        } else if (intvType.equals(IntervalType.WEEKLY)) {
            return Type.WEEKLY;
        } else if (intvType.equals(IntervalType.MONTHLY)) {
            return Type.MONTHLY;
        }
        return null;
    }

    @Override
    public SnapshotVO allocSnapshot(Long volumeId, Long policyId) throws ResourceAllocationException {
        Account caller = UserContext.current().getCaller();
        VolumeVO volume = _volsDao.findById(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Creating snapshot failed due to volume:" + volumeId + " doesn't exist");
        }
        if (volume.getState() != Volume.State.Ready) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " is not in " + Volume.State.Ready + " state but " + volume.getState() + ". Cannot take snapshot.");
        }

        if ( volume.getTemplateId() != null ) {
            VMTemplateVO  template = _templateDao.findById(volume.getTemplateId());
            if( template != null && template.getTemplateType() == Storage.TemplateType.SYSTEM ) {
                throw new InvalidParameterValueException("VolumeId: " + volumeId + " is for System VM , Creating snapshot against System VM volumes is not supported");
            }
        }
        
        UserVmVO vm = _vmDao.findById(volume.getInstanceId());
        if (vm.getHypervisorType() == HypervisorType.Ovm) {
        	throw new InvalidParameterValueException("Ovm won't support taking snapshot");
        }

        StoragePoolVO storagePoolVO = _storagePoolDao.findById(volume.getPoolId());
        if (storagePoolVO == null) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " please attach this volume to a VM before create snapshot for it");
        }

        // Verify permissions
        _accountMgr.checkAccess(caller, null, volume);

        Account owner = _accountMgr.getAccount(volume.getAccountId());
        if (_accountMgr.resourceLimitExceeded(owner, ResourceType.snapshot)) {
            ResourceAllocationException rae = new ResourceAllocationException("Maximum number of snapshots for account: " + owner.getAccountName() + " has been exceeded.");
            rae.setResourceType("snapshot");
            throw rae;
        }

        // Determine the name for this snapshot
        // Snapshot Name: VMInstancename + volumeName + timeString
        String timeString = DateUtil.getDateDisplayString(DateUtil.GMT_TIMEZONE, new Date(), DateUtil.YYYYMMDD_FORMAT);

        VMInstanceVO vmInstance = _vmDao.findById(volume.getInstanceId());
        String vmDisplayName = "detached";
        if (vmInstance != null) {
            vmDisplayName = vmInstance.getHostName();
        }
        String snapshotName = vmDisplayName + "_" + volume.getName() + "_" + timeString;

        // Create the Snapshot object and save it so we can return it to the
        // user
        Type snapshotType = getSnapshotType(policyId);
        HypervisorType hypervisorType = this._volsDao.getHypervisorType(volumeId);
        SnapshotVO snapshotVO = new SnapshotVO(volume.getDataCenterId(), volume.getAccountId(), volume.getDomainId(), volume.getId(), volume.getDiskOfferingId(), null, snapshotName,
                (short) snapshotType.ordinal(), snapshotType.name(), volume.getSize(), hypervisorType);
        SnapshotVO snapshot = _snapshotDao.persist(snapshotVO);

        return snapshot;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;

        ComponentLocator locator = ComponentLocator.getCurrentLocator();

        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        if (configDao == null) {
            throw new ConfigurationException("Unable to get the configuration dao.");
        }
        
        String value = configDao.getValue(Config.BackupSnapshotWait.toString());
        _backupsnapshotwait = NumbersUtil.parseInt(value, Integer.parseInt(Config.BackupSnapshotWait.getDefaultValue()));

        Type.HOURLY.setMax(NumbersUtil.parseInt(configDao.getValue("snapshot.max.hourly"), HOURLYMAX));
        Type.DAILY.setMax(NumbersUtil.parseInt(configDao.getValue("snapshot.max.daily"), DAILYMAX));
        Type.WEEKLY.setMax(NumbersUtil.parseInt(configDao.getValue("snapshot.max.weekly"), WEEKLYMAX));
        Type.MONTHLY.setMax(NumbersUtil.parseInt(configDao.getValue("snapshot.max.monthly"), MONTHLYMAX));
        _deltaSnapshotMax = NumbersUtil.parseInt(configDao.getValue("snapshot.delta.max"), DELTAMAX);
        _totalRetries = NumbersUtil.parseInt(configDao.getValue("total.retries"), 4);
        _pauseInterval = 2 * NumbersUtil.parseInt(configDao.getValue("ping.interval"), 60);

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

    @Override
    public boolean deleteSnapshotPolicies(DeleteSnapshotPoliciesCmd cmd) {
        Long policyId = cmd.getId();
        List<Long> policyIds = cmd.getIds();
        Long userId = getSnapshotUserId();

        if ((policyId == null) && (policyIds == null)) {
            throw new InvalidParameterValueException("No policy id (or list of ids) specified.");
        }

        if (policyIds == null) {
            policyIds = new ArrayList<Long>();
            policyIds.add(policyId);
        } else if (policyIds.size() <= 0) {
            // Not even sure how this is even possible
            throw new InvalidParameterValueException("There are no policy ids");
        }

        for (Long policy : policyIds) {
            SnapshotPolicyVO snapshotPolicyVO = _snapshotPolicyDao.findById(policy);
            if (snapshotPolicyVO == null) {
                throw new InvalidParameterValueException("Policy id given: " + policy + " does not exist");
            }
            VolumeVO volume = _volsDao.findById(snapshotPolicyVO.getVolumeId());
            if (volume == null) {
                throw new InvalidParameterValueException("Policy id given: " + policy + " does not belong to a valid volume");
            }

            _accountMgr.checkAccess(UserContext.current().getCaller(), null, volume);
        }

        boolean success = true;

        if (policyIds.contains(Snapshot.MANUAL_POLICY_ID)) {
            throw new InvalidParameterValueException("Invalid Policy id given: " + Snapshot.MANUAL_POLICY_ID);
        }

        for (Long pId : policyIds) {
            if (!deletePolicy(userId, pId)) {
                success = false;
                s_logger.warn("Failed to delete snapshot policy with Id: " + policyId);
                return success;
            }
        }

        return success;
    }

    private boolean hostSupportSnapsthot(HostVO host) {
        if (host.getHypervisorType() != HypervisorType.KVM) {
            return true;
        }
        // Determine host capabilities
        String caps = host.getCapabilities();

        if (caps != null) {
            String[] tokens = caps.split(",");
            for (String token : tokens) {
                if (token.contains("snapshot")) {
                    return true;
                }
            }
        }
        return false;
    }

}
