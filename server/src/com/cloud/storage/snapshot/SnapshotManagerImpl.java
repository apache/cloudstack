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
import com.cloud.api.ServerApiException;
import com.cloud.api.commands.CreateSnapshotCmd;
import com.cloud.api.commands.CreateSnapshotPolicyCmd;
import com.cloud.api.commands.DeleteSnapshotCmd;
import com.cloud.api.commands.DeleteSnapshotPoliciesCmd;
import com.cloud.api.commands.ListRecurringSnapshotScheduleCmd;
import com.cloud.api.commands.ListSnapshotPoliciesCmd;
import com.cloud.api.commands.ListSnapshotsCmd;
import com.cloud.async.AsyncInstanceCreateStatus;
import com.cloud.async.AsyncJobManager;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.DetailsDao;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Snapshot.Status;
import com.cloud.storage.Snapshot.Type;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotScheduleVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.SnapshotScheduleDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
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

@Local(value={SnapshotManager.class, SnapshotService.class})
public class SnapshotManagerImpl implements SnapshotManager, SnapshotService, Manager {
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
    @Inject protected DomainDao _domainDao;
    @Inject protected StorageManager _storageMgr;
    @Inject protected AgentManager _agentMgr;
    @Inject protected SnapshotScheduler _snapSchedMgr;
    @Inject protected AsyncJobManager _asyncMgr;
    @Inject protected AccountManager _accountMgr;
    @Inject protected ClusterDao _clusterDao;
    @Inject private UsageEventDao _usageEventDao;
    String _name;
    private int _totalRetries;
    private int _pauseInterval;
    private int _deltaSnapshotMax;

    protected SearchBuilder<SnapshotVO> PolicySnapshotSearch;
    protected SearchBuilder<SnapshotPolicyVO> PoliciesForSnapSearch;

    private final boolean _shouldBeSnapshotCapable = true; // all methods here should be snapshot capable.

    private boolean isVolumeDirty(long volumeId,Long policy) {
        VolumeVO volume = _volsDao.findById(volumeId);
        boolean runSnap = true;
        
        if (volume.getInstanceId() == null) {
            long lastSnapId = _snapshotDao.getLastSnapshot(volumeId, 0);
            SnapshotVO lastSnap = _snapshotDao.findByIdIncludingRemoved(lastSnapId);
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
            SnapshotVO lastSnap = _snapshotDao.findByIdIncludingRemoved(lastSnapId);
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
        if (volume.getState() == Volume.State.Destroy || volume.getRemoved() != null) {
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

    @Override
    public SnapshotVO createSnapshotOnPrimary(VolumeVO volume, Long policyId, Long snapshotId) throws ResourceAllocationException {
        SnapshotVO createdSnapshot = null;
    	Long volumeId = volume.getId();
        if (volume.getStatus() != AsyncInstanceCreateStatus.Created) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " is not in Created state but " + volume.getStatus() + ". Cannot take snapshot.");
        }
        StoragePoolVO storagePoolVO = _storagePoolDao.findById(volume.getPoolId());
        if (storagePoolVO == null) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " does not have a valid storage pool. Is it destroyed?");
        }

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
        Type snapshotType = SnapshotVO.getSnapshotType(policyId);
        HypervisorType hypervisorType = this._volsDao.getHypervisorType(volumeId);
        SnapshotVO snapshotVO = new SnapshotVO(snapshotId, volume.getAccountId(), volume.getId(), null, snapshotName,
                (short) snapshotType.ordinal(), snapshotType.name(), hypervisorType);
        snapshotVO = _snapshotDao.persist(snapshotVO);
        id = snapshotVO.getId();
        assert id != null;

        // Send a ManageSnapshotCommand to the agent
        String vmName = _storageMgr.getVmNameOnVolume(volume);

        long  preId = _snapshotDao.getLastSnapshot(volumeId, id);

        String preSnapshotPath = null;
        SnapshotVO preSnapshotVO = null;
        if( preId != 0) {
            preSnapshotVO = _snapshotDao.findByIdIncludingRemoved(preId);
            if (preSnapshotVO != null && preSnapshotVO.getBackupSnapshotId() != null ) {
                preSnapshotPath = preSnapshotVO.getPath();
            }
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
                createdSnapshot =  _snapshotDao.findByIdIncludingRemoved(id);
                createdSnapshot.setPath(preSnapshotPath);
                createdSnapshot.setBackupSnapshotId(preSnapshotVO.getBackupSnapshotId());
                createdSnapshot.setStatus(Snapshot.Status.BackedUp);
                createdSnapshot.setPrevSnapshotId(preId);
                _snapshotDao.update(id, createdSnapshot);
            } else {
                long preSnapshotId = 0;
                if( preSnapshotVO != null && preSnapshotVO.getBackupSnapshotId() != null) {
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
                createdSnapshot = updateDBOnCreate(id, answer.getSnapshotPath(), preSnapshotId);
            }
            // Get the snapshot_schedule table entry for this snapshot and
            // policy id.
            // Set the snapshotId to retrieve it back later.
            if (policyId != Snapshot.MANUAL_POLICY_ID) {
                SnapshotScheduleVO snapshotSchedule = _snapshotScheduleDao.getCurrentSchedule(volumeId, policyId, true);
                assert snapshotSchedule != null;
                snapshotSchedule.setSnapshotId(id);
                _snapshotScheduleDao.update(snapshotSchedule.getId(), snapshotSchedule);
            }

        } else {
            if (answer != null) {
                s_logger.error(answer.getDetails());
            }
            // The snapshot was not successfully created
            createdSnapshot =  _snapshotDao.findByIdIncludingRemoved(id);
            // delete from the snapshots table
            _snapshotDao.expunge(id);                
            throw new CloudRuntimeException("Creating snapshot for volume " + volumeId + " on primary storage failed.");
        }

        return createdSnapshot;
    }


    public SnapshotVO createSnapshotImpl(long volumeId, long policyId) throws ResourceAllocationException {
        return null;
    }
    
    @Override @DB
    public SnapshotVO createSnapshotImpl(Long volumeId, Long policyId, Long snapshotId) throws ResourceAllocationException {
    	VolumeVO v = _volsDao.findById(volumeId);
    	if ( v != null && _volsDao.getHypervisorType(v.getId()).equals(HypervisorType.KVM)) {
    		/*KVM needs to lock on the vm of volume, because it takes snapshot on behalf of vm, not volume*/
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
            throw new CloudRuntimeException("Volume: " + volumeId + " is empty, don't need to create a snapshot");
        }
        VolumeVO volume = _volsDao.acquireInLockTable(volumeId, 10);       
        if( volume == null ) {
            volume = _volsDao.findById(volumeId);
            if( volume == null ){
                throw new CloudRuntimeException("Creating snapshot failed due to volume:" + volumeId + " doesn't exist");
            } else {
                throw new CloudRuntimeException("Creating snapshot failed due to volume:" + volumeId + " is being used, try it later ");
            }
        }
        
        if (_volsDao.getHypervisorType(volume.getId()).equals(HypervisorType.KVM)) {
        	StoragePoolVO storagePool = _storagePoolDao.findById(volume.getPoolId());
        	ClusterVO cluster = _clusterDao.findById(storagePool.getClusterId());
        	List<HostVO> hosts = _hostDao.listByCluster(cluster.getId());
        	if (hosts != null && !hosts.isEmpty()) {
        		HostVO host = hosts.get(0);
        		if (!hostSupportSnapsthot(host)) {
                    throw new CloudRuntimeException("KVM Snapshot is not supported on cluster: " + host.getId());
                }
        	}
        }
        
        //if volume is attached to a vm in destroyed or expunging state; disallow
        if(v.getInstanceId() != null) {
            UserVmVO userVm = _vmDao.findById(v.getInstanceId());
            if(userVm != null) {
                if(userVm.getState().equals(State.Destroyed) || userVm.getState().equals(State.Expunging)) {
                    throw new CloudRuntimeException("Creating snapshot failed due to volume:" + volumeId + " is associated with vm:"+userVm.getInstanceName()+" which is in "+userVm.getState().toString()+" state");
                }
            }
        }
        
        SnapshotVO snapshot = null;
        boolean backedUp = false;
        try {
	    	snapshot = createSnapshotOnPrimary(volume, policyId, snapshotId);
	        if (snapshot != null && snapshot.getStatus() == Snapshot.Status.CreatedOnPrimary ) {
                snapshotId = snapshot.getId();
	            backedUp = backupSnapshotToSecondaryStorage(snapshot);
	            if (!backedUp) {
	                throw new CloudRuntimeException("Created snapshot: " + snapshotId + " on primary but failed to backup on secondary");
	            }
	        }
        } catch (Exception e){
            throw new CloudRuntimeException("Creating snapshot failed due to " + e.toString());
        } finally {
            // Cleanup jobs to do after the snapshot has been created.
        	if( snapshotId != null) {
        		postCreateSnapshot(volumeId, snapshotId, policyId, backedUp);
        	}
        	_volsDao.releaseFromLockTable(volumeId);
        }

        return snapshot;
    }

    @Override
    public SnapshotVO createSnapshot(CreateSnapshotCmd cmd) throws ResourceAllocationException {
        Long volumeId = cmd.getVolumeId();
        Long policyId = cmd.getPolicyId();
        Long snapshotId = cmd.getEntityId();
        return createSnapshotImpl(volumeId, policyId, snapshotId);
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

    @Override @DB
    public boolean backupSnapshotToSecondaryStorage(SnapshotVO ss) {
        long snapshotId = ss.getId();
        SnapshotVO snapshot = _snapshotDao.acquireInLockTable(snapshotId);
        if( snapshot == null) {
            throw new CloudRuntimeException("Can not acquire lock for snapshot: " + ss);
        }
        try {

            snapshot.setStatus(Snapshot.Status.BackingUp);
            _snapshotDao.update(snapshot.getId(), snapshot);
            
            long volumeId   = snapshot.getVolumeId();
            VolumeVO volume = _volsDao.lockRow(volumeId, true);
            
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
                prevSnapshot = _snapshotDao.findByIdIncludingRemoved(prevSnapshotId);
                prevBackupUuid = prevSnapshot.getBackupSnapshotId();
                if( prevBackupUuid != null ) {               
                    prevSnapshotUuid = prevSnapshot.getPath();
                }
            }

            boolean isVolumeInactive = _storageMgr.volumeInactive(volume);
            String vmName = _storageMgr.getVmNameOnVolume(volume);
            BackupSnapshotCommand backupSnapshotCommand =
                new BackupSnapshotCommand(primaryStoragePoolNameLabel,
                                          secondaryStoragePoolUrl,
                                          dcId,
                                          accountId,
                                          volumeId,
                                          volume.getPath(),
                                          snapshotUuid,
                                          snapshot.getName(),
                                          prevSnapshotUuid,
                                          prevBackupUuid,
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
                }
            } else if (answer != null) {
                s_logger.error(answer.getDetails());
            }
            // Update the status in all cases.
            Transaction txn = Transaction.currentTxn();
            txn.start();
                       
            if (backedUp) {
                snapshot.setBackupSnapshotId(backedUpSnapshotUuid);
                if( answer.isFull()) {
                    snapshot.setPrevSnapshotId(0);
                }
                snapshot.setStatus(Snapshot.Status.BackedUp);
                _snapshotDao.update(snapshotId, snapshot);
                UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_SNAPSHOT_CREATE, snapshot.getAccountId(), volume.getDataCenterId(), snapshotId, snapshot.getName(), null, null, volume.getSize());
                _usageEventDao.persist(usageEvent);

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
            }
            txn.commit();
            
            return backedUp;
        } finally {
            if( snapshot != null ) {
                _snapshotDao.releaseFromLockTable(snapshotId);
            }
        }
        
    }
    private Long getSnapshotUserId(){
        Long userId = UserContext.current().getCallerUserId();
        if(userId == null ) {
            return User.UID_SYSTEM;
        }
        return userId;
    }
    
    @Override
    @DB
    public void postCreateSnapshot(Long volumeId, Long snapshotId, Long policyId, boolean backedUp) {
        Long userId = getSnapshotUserId();
        // Update the snapshot_policy_ref table with the created snapshot
        // Get the list of policies for this snapshot
        Transaction txn = Transaction.currentTxn();
        txn.start();
        if (backedUp) {
            // This is a manual create, so increment the count of snapshots for
            // this account
            if (policyId == Snapshot.MANUAL_POLICY_ID) {
                Snapshot snapshot = _snapshotDao.findByIdIncludingRemoved(snapshotId);
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
        List<SnapshotVO> snaps = listSnapsforVolumeType(volumeId, Type.RECURRING.name());
        SnapshotPolicyVO policy = _snapshotPolicyDao.findById(policyId);
        
        while(snaps.size() > policy.getMaxSnaps() && snaps.size() > 1) {
            //Delete the oldest snap ref in snap_policy_ref
            SnapshotVO oldestSnapshot = snaps.get(0);
            long oldSnapId = oldestSnapshot.getId();
            s_logger.debug("Max snaps: "+ policy.getMaxSnaps() + " exceeded for snapshot policy with Id: " + policyId + ". Deleting oldest snapshot: " + oldSnapId);
            // Excess snapshot. delete it asynchronously
            //destroySnapshotAsync(userId, volumeId, oldSnapId, policyId);
            // create the event
            deleteSnapshotInternal(oldSnapId, policyId);
            snaps.remove(oldestSnapshot);
        }
        
    }
    
    private Long checkAccountPermissions(long targetAccountId, long targetDomainId, String targetDesc, long targetId) throws ServerApiException {
    	Long accountId = null;

    	Account account = UserContext.current().getCaller();
    	if (account != null) {
    		if (!isAdmin(account.getType())) {
    			if (account.getId() != targetAccountId) {
    				throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find a " + targetDesc + " with id " + targetId + " for this account");
    			}
    		} else if (!_domainDao.isChildDomain(account.getDomainId(), targetDomainId)) {
    			throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to perform operation for " + targetDesc + " with id " + targetId + ", permission denied.");
    		}
    		accountId = account.getId();
    	}
	
    	return accountId;
	}

	private static boolean isAdmin(short accountType) {
	    return ((accountType == Account.ACCOUNT_TYPE_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
	}

    @Override @DB
    public boolean deleteSnapshot(DeleteSnapshotCmd cmd) {
    	Long snapshotId = cmd.getId();
    	
        //Verify parameters
        Snapshot snapshotCheck = _snapshotDao.findByIdIncludingRemoved(snapshotId.longValue());
        if (snapshotCheck == null) {
            throw new ServerApiException (BaseCmd.PARAM_ERROR, "unable to find a snapshot with id " + snapshotId);
        }

        // If an account was passed in, make sure that it matches the account of the snapshot
        Account snapshotOwner = _accountDao.findById(snapshotCheck.getAccountId());
        if (snapshotOwner == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Snapshot id " + snapshotId + " does not have a valid account");
        }
        checkAccountPermissions(snapshotOwner.getId(), snapshotOwner.getDomainId(), "snapshot", snapshotId);

        boolean status = true; 
        if (Type.MANUAL.ordinal() == snapshotCheck.getSnapshotType()) {
            status = deleteSnapshotInternal(snapshotId, Snapshot.MANUAL_POLICY_ID);

            if (!status) {
                s_logger.warn("Failed to delete snapshot");
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR,"Failed to delete snapshot:"+snapshotId);
            }
        } else {
            List<SnapshotPolicyVO> policies = listPoliciesforVolume(snapshotCheck.getVolumeId());
            
            for (SnapshotPolicyVO policy : policies) {
                status = deleteSnapshotInternal(snapshotId, policy.getId());
                
                if (!status) {
                    s_logger.warn("Failed to delete snapshot");
                    throw new ServerApiException(BaseCmd.INTERNAL_ERROR,"Failed to delete snapshot:"+snapshotId);
                }
            }
        }

        return status;
    }

	private boolean deleteSnapshotInternal(Long snapshotId, Long policyId) {
	    if (s_logger.isDebugEnabled()) {
	        s_logger.debug("Calling deleteSnapshot for snapshotId: " + snapshotId + " and policyId " + policyId);
	    }
	    SnapshotVO lastSnapshot = null;
        _snapshotDao.remove(snapshotId);
        long lastId = snapshotId;
        boolean destroy = false;
        while( true ) {
            lastSnapshot = _snapshotDao.findNextSnapshot(lastId);
            if( lastSnapshot == null ) {
                // if all snapshots after this snapshot in this chain are removed, remove those snapshots.
                destroy = true;
                break;
            }
            if( lastSnapshot.getRemoved() == null ) {
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
                    if ( snaps.size() > 1 ) {
                        lastSnapshot.setBackupSnapshotId(null);
                        _snapshotDao.update(lastSnapshot.getId(), lastSnapshot);
                    } else {
                        if (destroySnapshotBackUp(lastId, policyId)) {
                            
                        } else {
                            s_logger.debug("Destroying snapshot backup failed " + lastSnapshot);
                            break;
                        }  
                    }
                }
                postDeleteSnapshot(lastId, policyId);
                lastId = lastSnapshot.getPrevSnapshotId();
                if (lastId == 0) {
                    break;
                }
                lastSnapshot = _snapshotDao.findByIdIncludingRemoved(lastId);
            }
        }
        return true;
	}

    @Override @DB
    public boolean destroySnapshot(long userId, long snapshotId, long policyId) {
        return true;
    }

    @Override @DB
    public boolean destroySnapshotBackUp(long snapshotId, long policyId) {
        boolean success = false;
        String details = null;
        SnapshotVO snapshot = _snapshotDao.findByIdIncludingRemoved(snapshotId);

        VolumeVO volume = _volsDao.findByIdIncludingRemoved(snapshot.getVolumeId());
        if ( volume == null ) {
            throw new CloudRuntimeException("Destroying snapshot " + snapshotId + " backup failed due to unable to find volume " + snapshot.getVolumeId());
        }
        String primaryStoragePoolNameLabel = _storageMgr.getPrimaryStorageNameLabel(volume);
        String secondaryStoragePoolUrl = _storageMgr.getSecondaryStorageURL(volume.getDataCenterId());
        Long dcId = volume.getDataCenterId();
        Long accountId = volume.getAccountId();
        Long volumeId = volume.getId();

        String backupOfSnapshot = snapshot.getBackupSnapshotId();
        if ( backupOfSnapshot == null ) {
            return true;
        }
        DeleteSnapshotBackupCommand cmd = new DeleteSnapshotBackupCommand(primaryStoragePoolNameLabel,
                secondaryStoragePoolUrl, dcId, accountId, volumeId, backupOfSnapshot, snapshot.getName());

        snapshot.setBackupSnapshotId(null);
        _snapshotDao.update(snapshotId, snapshot);
        details = "Failed to destroy snapshot id:" + snapshotId + " for volume: " + volume.getId();
        Answer answer = _storageMgr.sendToHostsOnStoragePool(volume.getPoolId(), cmd, details, _totalRetries,
                _pauseInterval, _shouldBeSnapshotCapable, volume.getInstanceId());

        if ((answer != null) && answer.getResult()) {

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

        if(success){
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_SNAPSHOT_DELETE, snapshot.getAccountId(), volume.getDataCenterId(), snapshotId, snapshot.getName(), null, null, volume.getSize());
            _usageEventDao.persist(usageEvent);
        }

        return success;

    }

    @DB
    protected void postDeleteSnapshot(long snapshotId, long policyId) {
        // Remove the snapshot from the snapshots table and the snap_policy_ref table.
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        SnapshotVO snapshot = _snapshotDao.findByIdIncludingRemoved(snapshotId);
        // If this is a manual delete, decrement the count of snapshots for this account
        if (policyId == Snapshot.MANUAL_POLICY_ID) {
        	_accountMgr.decrementResourceCount(snapshot.getAccountId(), ResourceType.snapshot);
        }
        
        txn.commit();
    }
    
    
    @Override
    public List<SnapshotVO> listSnapshots(ListSnapshotsCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Long volumeId = cmd.getVolumeId();
        Boolean isRecursive = cmd.isRecursive();
        
        // Verify parameters
        if(volumeId != null){
            VolumeVO volume = _volsDao.findById(volumeId);
            if (volume == null) {
                throw new InvalidParameterValueException("unable to find a volume with id " + volumeId);
            }
            checkAccountPermissions(volume.getAccountId(), volume.getDomainId(), "volume", volumeId);
        }

        Account account = UserContext.current().getCaller();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;
        if ((account == null) || isAdmin(account.getType())) {
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Unable to list templates for domain " + domainId + ", permission denied.");
                }
            } else if ((account != null) && (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)) {
                domainId = account.getDomainId();
            } 

            if (domainId != null && accountName != null) {
                Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                if (userAccount != null) {
                    accountId = userAccount.getId();
                } else {
                    throw new InvalidParameterValueException("Could not find account:"+accountName+" in domain:"+domainId);
                }
            }
        } else {
            accountId = account.getId();
        }

        if(isRecursive == null)
            isRecursive = false;
        
        Object name = cmd.getSnapshotName();
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Object snapshotTypeStr = cmd.getSnapshotType();

        Filter searchFilter = new Filter(SnapshotVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<SnapshotVO> sb = _snapshotDao.createSearchBuilder();
        sb.and("status", sb.entity().getStatus(), SearchCriteria.Op.EQ);
        sb.and("volumeId", sb.entity().getVolumeId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("snapshotTypeEQ", sb.entity().getSnapshotType(), SearchCriteria.Op.EQ);
        sb.and("snapshotTypeNEQ", sb.entity().getSnapshotType(), SearchCriteria.Op.NEQ);

        if ((accountId == null) && (domainId != null)) {
            // if accountId isn't specified, we can do a domain match for the admin case
            SearchBuilder<AccountVO> accountSearch = _accountDao.createSearchBuilder();
            sb.join("accountSearch", accountSearch, sb.entity().getAccountId(), accountSearch.entity().getId(), JoinType.INNER);

            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            if(isRecursive)
                domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            else
                domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.EQ);
            accountSearch.join("domainSearch", domainSearch, accountSearch.entity().getDomainId(), domainSearch.entity().getId(), JoinType.INNER);
        }

        SearchCriteria<SnapshotVO> sc = sb.create();

        sc.setParameters("status", Snapshot.Status.BackedUp);

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
            if(isRecursive)
                joinSearch.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
            else
                joinSearch.setJoinParameters("domainSearch", "path", domain.getPath());
        }

        if (snapshotTypeStr != null) {
            Type snapshotType = SnapshotVO.getSnapshotType((String)snapshotTypeStr);
            if (snapshotType == null) {
                throw new InvalidParameterValueException("Unsupported snapshot type " + snapshotTypeStr);
            }
            sc.setParameters("snapshotTypeEQ", snapshotType.ordinal());
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
        	if(volume.getPoolId()==null){
        		continue;
        	}
        	Long volumeId = volume.getId();
        	Long dcId = volume.getDataCenterId();
        	String secondaryStoragePoolURL = _storageMgr.getSecondaryStorageURL(dcId);
        	String primaryStoragePoolNameLabel = _storageMgr.getPrimaryStorageNameLabel(volume);
        	if (_snapshotDao.listByVolumeIdIncludingRemoved(volumeId).isEmpty() ) {
        	    // This volume doesn't have any snapshots. Nothing do delete.
        	    continue;
        	}
        	DeleteSnapshotsDirCommand cmd = new DeleteSnapshotsDirCommand(primaryStoragePoolNameLabel, secondaryStoragePoolURL, dcId, accountId, volumeId, volume.getPath());
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
                    UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_SNAPSHOT_DELETE, snapshot.getAccountId(), volume.getDataCenterId(), snapshot.getId(), snapshot.getName(), null, null, volume.getSize());
                    _usageEventDao.persist(usageEvent);
        	    }
        	}
        }
        
        // Returns true if snapshotsDir has been deleted for all volumes.
        return success;
	}

    @Override @DB
    public SnapshotPolicyVO createPolicy(CreateSnapshotPolicyCmd cmd) throws InvalidParameterValueException {
        Long volumeId = cmd.getVolumeId();
        VolumeVO volume = _volsDao.findById(cmd.getVolumeId());
        if (volume == null) {
            throw new InvalidParameterValueException("Failed to create snapshot policy, unable to find a volume with id " + volumeId);
        }

        // If an account was passed in, make sure that it matches the account of the volume
        checkAccountPermissions(volume.getAccountId(), volume.getDomainId(), "volume", volumeId);

        StoragePoolVO storagePoolVO = _storagePoolDao.findById(volume.getPoolId());
        if (storagePoolVO == null) {
            throw new InvalidParameterValueException("Failed to create snapshot policy, volumeId: " + volumeId + " does not have a valid storage pool. Is it destroyed?");
        }
        if (storagePoolVO.isLocal()) {
            throw new InvalidParameterValueException("Failed to create snapshot policy, cannot create a snapshot from a volume residing on a local storage pool, poolId: " + volume.getPoolId());
        }

        Long instanceId = volume.getInstanceId();
        if (instanceId != null) {
            // It is not detached, but attached to a VM
            if (_vmDao.findById(instanceId) == null) {
                // It is not a UserVM but a SystemVM or DomR
                throw new InvalidParameterValueException("Failed to create snapshot policy, snapshots of volumes attached to System or router VM are not allowed");
            }
        }
        
        Long accountId = volume.getAccountId();
        Long userId = getSnapshotUserId();

        IntervalType type =  DateUtil.IntervalType.getIntervalType(cmd.getIntervalType());
        if (type == null) {
            throw new InvalidParameterValueException("Unsupported interval type " + cmd.getIntervalType());
        }

        TimeZone timeZone = TimeZone.getTimeZone(cmd.getTimezone());
        String timezoneId = timeZone.getID();
        if (!timezoneId.equals(cmd.getTimezone())) {
            s_logger.warn("Using timezone: " + timezoneId + " for running this snapshot policy as an equivalent of " + cmd.getTimezone());
        }

        try {
            DateUtil.getNextRunTime(type, cmd.getSchedule(), timezoneId, null);
        } catch (Exception e){
            throw new InvalidParameterValueException("Invalid schedule: "+ cmd.getSchedule() +" for interval type: " + cmd.getIntervalType());
        }

        int intervalMaxSnaps = type.getMax();
        if (cmd.getMaxSnaps() > intervalMaxSnaps) {
            throw new InvalidParameterValueException("maxSnaps exceeds limit: " + intervalMaxSnaps + " for interval type: " + cmd.getIntervalType());
        }

        SnapshotPolicyVO policy = new SnapshotPolicyVO(volumeId, cmd.getSchedule(), timezoneId, (short)type.ordinal(), cmd.getMaxSnaps());
        // Create an event
        try{
            policy = _snapshotPolicyDao.persist(policy);
        } catch (EntityExistsException e ) {
            policy = _snapshotPolicyDao.findOneByVolume(volumeId);
            try {
                policy = _snapshotPolicyDao.acquireInLockTable(policy.getId());
                policy.setSchedule(cmd.getSchedule());
                policy.setTimezone(timezoneId);
                policy.setInterval((short)type.ordinal());
                policy.setMaxSnaps(cmd.getMaxSnaps());
                policy.setActive(true);
                _snapshotPolicyDao.update(policy.getId(), policy);
            } finally {
                if( policy != null) {
                    _snapshotPolicyDao.releaseFromLockTable(policy.getId());
                }
            }
        }
        _snapSchedMgr.scheduleNextSnapshotJob(policy);
        return policy;
    }

    @Override
    public boolean deletePolicy(long userId, Long policyId) {
        SnapshotPolicyVO snapshotPolicy = _snapshotPolicyDao.findById(policyId);
        _snapSchedMgr.removeSchedule(snapshotPolicy.getVolumeId(), snapshotPolicy.getId());
        return _snapshotPolicyDao.remove(policyId);
    }

    @Override
    public List<SnapshotPolicyVO> listPoliciesforVolume(ListSnapshotPoliciesCmd cmd) throws InvalidParameterValueException {
        Long volumeId = cmd.getVolumeId();
        VolumeVO volume = _volsDao.findById(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Unable to find a volume with id " + volumeId);
        }
        checkAccountPermissions(volume.getAccountId(), volume.getDomainId(), "volume", volumeId);
        return listPoliciesforVolume(cmd.getVolumeId());
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
    public List<SnapshotScheduleVO> findRecurringSnapshotSchedule(ListRecurringSnapshotScheduleCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Long volumeId = cmd.getVolumeId();
        Long policyId = cmd.getSnapshotPolicyId();
        Account account = UserContext.current().getCaller();

        //Verify parameters
        VolumeVO volume = _volsDao.findById(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Failed to list snapshot schedule, unable to find a volume with id " + volumeId);
        }

        if (account != null) {
            long volAcctId = volume.getAccountId();
            if (isAdmin(account.getType())) {
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
            for (SnapshotPolicyVO policyInstance: policyInstances) {
                SnapshotScheduleVO snapshotSchedule =
                    _snapshotScheduleDao.getCurrentSchedule(volumeId, policyInstance.getId(), false);
                snapshotSchedules.add(snapshotSchedule);
            }
        } else {
            snapshotSchedules.add(_snapshotScheduleDao.getCurrentSchedule(volumeId, policyId, false));
        }
        return snapshotSchedules;
    }

	@Override
	public SnapshotPolicyVO getPolicyForVolumeByInterval(long volumeId, short interval) {
	    return _snapshotPolicyDao.findOneByVolumeInterval(volumeId, interval);
	}

    @Override
    public SnapshotPolicyVO getPolicyForVolume(long volumeId) {
        return _snapshotPolicyDao.findOneByVolume(volumeId);
    }

    @Override
    public long getNextInSequence(CreateSnapshotCmd cmd) {
        return  _snapshotDao.getNextInSequence(Long.class, "id");
    }
    
	@Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
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
        _deltaSnapshotMax = NumbersUtil.parseInt(configDao.getValue("snapshot.delta.max"), DELTAMAX);
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

	@Override
	public boolean deleteSnapshotPolicies(DeleteSnapshotPoliciesCmd cmd) throws InvalidParameterValueException {
    	Long policyId = cmd.getId();
        List<Long> policyIds = cmd.getIds();
        Long userId = getSnapshotUserId();

        if ((policyId == null) && (policyIds == null)) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "No policy id (or list of ids) specified.");
        }
        
        if (policyIds == null) {
        	policyIds = new ArrayList<Long>();
        	policyIds.add(policyId);
        } else if(policyIds.size()<=0){
        	// Not even sure how this is even possible
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR,"There are no policy ids");
        }
        
        for (Long policy : policyIds) {
            SnapshotPolicyVO snapshotPolicyVO = _snapshotPolicyDao.findById(policy);
            if (snapshotPolicyVO == null) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Policy id given: " + policy + " does not exist");
            }
            VolumeVO volume = _volsDao.findById(snapshotPolicyVO.getVolumeId());
            if (volume == null) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Policy id given: " + policy + " does not belong to a valid volume");
            }
            
            // If an account was passed in, make sure that it matches the account of the volume
            checkAccountPermissions(volume.getAccountId(), volume.getDomainId(), "volume", volume.getId());
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
