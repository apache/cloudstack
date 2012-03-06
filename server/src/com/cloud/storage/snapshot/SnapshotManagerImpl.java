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
import com.cloud.agent.api.downloadSnapshotFromSwiftCommand;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.alert.AlertManager;
import com.cloud.api.commands.CreateSnapshotPolicyCmd;
import com.cloud.api.commands.DeleteSnapshotPoliciesCmd;
import com.cloud.api.commands.ListRecurringSnapshotScheduleCmd;
import com.cloud.api.commands.ListSnapshotPoliciesCmd;
import com.cloud.api.commands.ListSnapshotsCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.EventUtils;
import com.cloud.event.EventVO;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Grouping;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.projects.ProjectManager;
import com.cloud.resource.ResourceManager;
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
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.SnapshotScheduleDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.swift.SwiftManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.DateUtil.IntervalType;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
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
    private AlertManager _alertMgr;
    @Inject
    protected ClusterDao _clusterDao;
    @Inject
    private UsageEventDao _usageEventDao;
    @Inject
    private ResourceLimitService _resourceLimitMgr;
    @Inject
    private SwiftManager _swiftMgr;
    @Inject
    private ProjectManager _projectMgr;
    @Inject 
    private SecondaryStorageVmManager _ssvmMgr;
    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    private DomainManager _domainMgr;
    @Inject
    private VolumeDao _volumeDao;
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
        if (vm != null) {
            if(vm.getHostId() != null) {
                hostIdsToTryFirst = new long[] { vm.getHostId() };
            } else if(vm.getLastHostId() != null) {
                hostIdsToTryFirst = new long[] { vm.getLastHostId() };
            }
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
        if (preId != 0 && !(volume.getLastPoolId() != null && !volume.getLastPoolId().equals(volume.getPoolId()))) {
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
                snapshot.setSwiftId(preSnapshotVO.getSwiftId());

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
                
                //If the volume is moved around, backup a full snapshot to secondary storage
                if (volume.getLastPoolId() != null && !volume.getLastPoolId().equals(volume.getPoolId())) {
                	preSnapshotId = 0;
                	volume.setLastPoolId(volume.getPoolId());
                	_volumeDao.update(volume.getId(), volume);
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
    public SnapshotVO createSnapshot(Long volumeId, Long policyId, Long snapshotId, Account snapshotOwner) {
        VolumeVO volume = _volsDao.findById(volumeId);   
        if (volume == null) {
        	throw new InvalidParameterValueException("No such volume exist");
        }
        
        if (volume.getState() != Volume.State.Ready) {
        	throw new InvalidParameterValueException("Volume is not in ready state");
        }
        
        SnapshotVO snapshot = null;
     
        boolean backedUp = false;
        UserVmVO uservm = null;
        // does the caller have the authority to act on this volume
        _accountMgr.checkAccess(UserContext.current().getCaller(), null, true, volume);
        
        try {
    
            Long poolId = volume.getPoolId();
            if (poolId == null) {
                throw new CloudRuntimeException("You cannot take a snapshot of a volume until it has been attached to an instance");
            }

            if (_volsDao.getHypervisorType(volume.getId()).equals(HypervisorType.KVM)) {
            	uservm = _vmDao.findById(volume.getInstanceId());
            	if (uservm != null && uservm.getType() != VirtualMachine.Type.User) {
            		throw new CloudRuntimeException("Can't take a snapshot on system vm ");
            	}
            	
                StoragePoolVO storagePool = _storagePoolDao.findById(volume.getPoolId());
                ClusterVO cluster = _clusterDao.findById(storagePool.getClusterId());
                List<HostVO> hosts = _resourceMgr.listAllHostsInCluster(cluster.getId());
                if (hosts != null && !hosts.isEmpty()) {
                    HostVO host = hosts.get(0);
                    if (!hostSupportSnapsthot(host)) {
                        _snapshotDao.expunge(snapshotId);
                        throw new CloudRuntimeException("KVM Snapshot is not supported on cluster: " + host.getId());
                    }
                }
            }

            // if volume is attached to a vm in destroyed or expunging state; disallow
            if (volume.getInstanceId() != null) {
                UserVmVO userVm = _vmDao.findById(volume.getInstanceId());
                if (userVm != null) {
                    if (userVm.getState().equals(State.Destroyed) || userVm.getState().equals(State.Expunging)) {
                        _snapshotDao.expunge(snapshotId);
                        throw new CloudRuntimeException("Creating snapshot failed due to volume:" + volumeId + " is associated with vm:" + userVm.getInstanceName() + " is in "
                                + userVm.getState().toString() + " state");
                    }
                    
                    if(userVm.getHypervisorType() == HypervisorType.VMware || userVm.getHypervisorType() == HypervisorType.KVM) {
                    	List<SnapshotVO> activeSnapshots = _snapshotDao.listByInstanceId(volume.getInstanceId(), Snapshot.Status.Creating,  Snapshot.Status.CreatedOnPrimary,  Snapshot.Status.BackingUp);
                    	if(activeSnapshots.size() > 1)
                            throw new CloudRuntimeException("There is other active snapshot tasks on the instance to which the volume is attached, please try again later");
                    }
                }
            }

            //when taking snapshot, make sure nobody can delete/move the volume
            boolean stateTransit = false;
            /*
            try {
            	stateTransit = _storageMgr.stateTransitTo(volume, Volume.Event.SnapshotRequested);
            } catch (NoTransitionException e) {
            	s_logger.debug("Failed transit volume state: " + e.toString());
            } finally {
            	if (!stateTransit) {
            		_snapshotDao.expunge(snapshotId);           		
            		throw new CloudRuntimeException("Creating snapshot failed due to volume:" + volumeId + " is being used, try it later ");
            	}
            }*/

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
                            volume.getSize());
                    _usageEventDao.persist(usageEvent);
                }
                if( !backedUp ) {

                    snapshot.setStatus(Status.Error);
                    _snapshotDao.update(snapshot.getId(), snapshot);
                } else {
                    _resourceLimitMgr.incrementResourceCount(snapshotOwner.getId(), ResourceType.snapshot);
                }
            } else {
            	snapshot = _snapshotDao.findById(snapshotId);
            	if (snapshot != null) {
            		snapshot.setStatus(Status.Error);
            		_snapshotDao.update(snapshotId, snapshot);
            	}
            }

            /*
            try {
            	_storageMgr.stateTransitTo(volume, Volume.Event.OperationSucceeded);
            } catch (NoTransitionException e) {
            	s_logger.debug("Failed to transit volume state: " + e.toString());
            }*/

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
    public void deleteSnapshotsForVolume (String secondaryStoragePoolUrl, Long dcId, Long accountId, Long volumeId ){
        SwiftTO swift = _swiftMgr.getSwiftTO();
        DeleteSnapshotBackupCommand cmd = new DeleteSnapshotBackupCommand(swift, secondaryStoragePoolUrl, dcId, accountId, volumeId, null, true);
        try {
            Answer ans = _agentMgr.sendToSSVM(dcId, cmd);
            if ( ans == null || !ans.getResult() ) {
                s_logger.warn("DeleteSnapshotBackupCommand failed due to " + ans.getDetails() + " volume id: " + volumeId);
            }
        } catch (Exception e) {
            s_logger.warn("DeleteSnapshotBackupCommand failed due to" + e.toString() + " volume id: " + volumeId);
        }
    }

    @Override
    public void deleteSnapshotsDirForVolume(String secondaryStoragePoolUrl, Long dcId, Long accountId, Long volumeId) {
        DeleteSnapshotsDirCommand cmd = new DeleteSnapshotsDirCommand(secondaryStoragePoolUrl, dcId, accountId, volumeId);
        try {
            Answer ans = _agentMgr.sendToSSVM(dcId, cmd);
            if (ans == null || !ans.getResult()) {
                s_logger.warn("DeleteSnapshotsDirCommand failed due to " + ans.getDetails() + " volume id: " + volumeId);
            }
        } catch (Exception e) {
            s_logger.warn("DeleteSnapshotsDirCommand failed due to" + e.toString() + " volume id: " + volumeId);
        }
    }


    @Override
    public void downloadSnapshotsFromSwift(SnapshotVO ss) {
        long volumeId = ss.getVolumeId();
        VolumeVO volume = _volsDao.findById(volumeId);
        Long dcId = volume.getDataCenterId();
        Long accountId = volume.getAccountId();
        HostVO secHost = _storageMgr.getSecondaryStorageHost(dcId);
        String secondaryStoragePoolUrl = secHost.getStorageUrl();

        Long swiftId = ss.getSwiftId();
        SwiftTO swift = _swiftMgr.getSwiftTO(swiftId);
        SnapshotVO tss = ss;
        List<String> BackupUuids = new ArrayList<String>(30);
        while (true) {
            BackupUuids.add(0, tss.getBackupSnapshotId());
            if (tss.getPrevSnapshotId() == 0)
                break;
            Long id = tss.getPrevSnapshotId();
            tss = _snapshotDao.findById(id);
            assert tss != null : " can not find snapshot " + id;
        }
        String parent = null;
        try {
            for (String backupUuid : BackupUuids) {
                downloadSnapshotFromSwiftCommand cmd = new downloadSnapshotFromSwiftCommand(swift, secondaryStoragePoolUrl, dcId, accountId, volumeId, parent, backupUuid, _backupsnapshotwait);
                Answer answer = _agentMgr.sendToSSVM(dcId, cmd);
                if ((answer == null) || !answer.getResult()) {
                    throw new CloudRuntimeException("downloadSnapshotsFromSwift failed ");
                }
                parent = backupUuid;
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("downloadSnapshotsFromSwift failed due to " + e.toString());
        }
        
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


            SwiftTO swift = _swiftMgr.getSwiftTO();
            
            long prevSnapshotId = snapshot.getPrevSnapshotId();
            if (prevSnapshotId > 0) {
                prevSnapshot = _snapshotDao.findByIdIncludingRemoved(prevSnapshotId);
                if ( prevSnapshot.getBackupSnapshotId() != null && swift == null) {
                    if (prevSnapshot.getVersion() != null && prevSnapshot.getVersion().equals("2.2")) {                   
                        prevBackupUuid = prevSnapshot.getBackupSnapshotId();
                        prevSnapshotUuid = prevSnapshot.getPath();
                    }
                } else if ( prevSnapshot.getSwiftId() != null && swift != null ) {
                    prevBackupUuid = prevSnapshot.getBackupSnapshotId();
                    prevSnapshotUuid = prevSnapshot.getPath();
                }
            }
            boolean isVolumeInactive = _storageMgr.volumeInactive(volume);
            String vmName = _storageMgr.getVmNameOnVolume(volume);
            StoragePoolVO srcPool = _storagePoolDao.findById(volume.getPoolId());
            BackupSnapshotCommand backupSnapshotCommand = new BackupSnapshotCommand(primaryStoragePoolNameLabel, secondaryStoragePoolUrl, dcId, accountId, volumeId, snapshot.getId(), volume.getPath(), srcPool, snapshotUuid,
                    snapshot.getName(), prevSnapshotUuid, prevBackupUuid, isVolumeInactive, vmName, _backupsnapshotwait);

            if ( swift != null ) {
                backupSnapshotCommand.setSwift(swift);
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
                    snapshot.setSwiftId(swift.getId());
                    snapshot.setBackupSnapshotId(backedUpSnapshotUuid);
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
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
        
        if (snapshot != null && snapshot.isRecursive()) {
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
            if(deleteSnapshotInternal(oldSnapId)){
            	//log Snapshot delete event
            	EventUtils.saveEvent(User.UID_SYSTEM, oldestSnapshot.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_SNAPSHOT_DELETE, "Successfully deleted oldest snapshot: " + oldSnapId, 0);
            }
            snaps.remove(oldestSnapshot);
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_SNAPSHOT_DELETE, eventDescription = "deleting snapshot", async = true)
    public boolean deleteSnapshot(long snapshotId) {
        Account caller = UserContext.current().getCaller();

        // Verify parameters
        Snapshot snapshotCheck = _snapshotDao.findById(snapshotId);
        if (snapshotCheck == null) {
            throw new InvalidParameterValueException("unable to find a snapshot with id " + snapshotId);
        }
        
        _accountMgr.checkAccess(caller, null, true, snapshotCheck);
        
        if( !Status.BackedUp.equals(snapshotCheck.getStatus() ) ) {
            throw new InvalidParameterValueException("Can't delete snapshotshot " + snapshotId + " due to it is not in BackedUp Status");
        }
        
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
            if (snaps != null && snaps.size() > 1) {
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
        _resourceLimitMgr.decrementResourceCount(snapshot.getAccountId(), ResourceType.snapshot);
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
                    if (snaps != null && snaps.size() > 1) {
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
    public HostVO getSecondaryStorageHost(SnapshotVO snapshot) {
        HostVO secHost = null;
        if( snapshot.getSwiftId() == null ) {
            secHost = _hostDao.findById(snapshot.getSecHostId());
        } else {
            Long dcId = snapshot.getDataCenterId();
            secHost = _storageMgr.getSecondaryStorageHost(dcId);
        }
        return secHost;
    }

    @Override
    public String getSecondaryStorageURL(SnapshotVO snapshot) {
        HostVO secHost = getSecondaryStorageHost(snapshot);
        if (secHost != null) {
            return secHost.getStorageUrl();
        }
        throw new CloudRuntimeException("Can not find secondary storage");
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
        SwiftTO swift = _swiftMgr.getSwiftTO(snapshot.getSwiftId());
        DeleteSnapshotBackupCommand cmd = new DeleteSnapshotBackupCommand(swift, secondaryStoragePoolUrl, dcId, accountId, volumeId, backupOfSnapshot, false);
        Answer answer = _agentMgr.sendToSSVM(dcId, cmd);

        if ((answer != null) && answer.getResult()) {
            snapshot.setBackupSnapshotId(null);
            _snapshotDao.update(snapshotId, snapshot);
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
        String name = cmd.getSnapshotName();
        Long id = cmd.getId();
        String keyword = cmd.getKeyword();
        String snapshotTypeStr = cmd.getSnapshotType();
        String intervalTypeStr = cmd.getIntervalType();
        
        Account caller = UserContext.current().getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();

        // Verify parameters
        if (volumeId != null) {
            VolumeVO volume = _volsDao.findById(volumeId);
            if (volume != null) {
                _accountMgr.checkAccess(UserContext.current().getCaller(), null, true, volume);
            }
        }

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
       _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
       Long domainId = domainIdRecursiveListProject.first();
       Boolean isRecursive = domainIdRecursiveListProject.second();
       ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();        
        
        Filter searchFilter = new Filter(SnapshotVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<SnapshotVO> sb = _snapshotDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("status", sb.entity().getStatus(), SearchCriteria.Op.EQ);
        sb.and("volumeId", sb.entity().getVolumeId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("snapshotTypeEQ", sb.entity().getsnapshotType(), SearchCriteria.Op.IN);
        sb.and("snapshotTypeNEQ", sb.entity().getsnapshotType(), SearchCriteria.Op.NEQ);

        SearchCriteria<SnapshotVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

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
            if (_snapshotDao.listByVolumeIdIncludingRemoved(volumeId).isEmpty()) {
                // This volume doesn't have any snapshots. Nothing do delete.
                continue;
            }
            List<HostVO> ssHosts = _ssvmMgr.listSecondaryStorageHostsInOneZone(dcId);
            SwiftTO swift = _swiftMgr.getSwiftTO();
            if (swift == null) {
                for (HostVO ssHost : ssHosts) {
                    DeleteSnapshotBackupCommand cmd = new DeleteSnapshotBackupCommand(null, ssHost.getStorageUrl(), dcId, accountId, volumeId, "", true);
                    Answer answer = null;
                    try {
                        answer = _agentMgr.sendToSSVM(dcId, cmd);
                    } catch (Exception e) {
                        s_logger.warn("Failed to delete all snapshot for volume " + volumeId + " on secondary storage " + ssHost.getStorageUrl());
                    }
                    if ((answer != null) && answer.getResult()) {
                        s_logger.debug("Deleted all snapshots for volume: " + volumeId + " under account: " + accountId);
                    } else {
                        success = false;
                        if (answer != null) {
                            s_logger.error(answer.getDetails());
                        }
                    }
                }
            } else {
                DeleteSnapshotBackupCommand cmd = new DeleteSnapshotBackupCommand(swift, "", dcId, accountId, volumeId, "", true);
                Answer answer = null;
                try {
                    answer = _agentMgr.sendToSSVM(dcId, cmd);
                } catch (Exception e) {
                    s_logger.warn("Failed to delete all snapshot for volume " + volumeId + " on swift");
                }
                if ((answer != null) && answer.getResult()) {
                    s_logger.debug("Deleted all snapshots for volume: " + volumeId + " under account: " + accountId);
                } else {
                    success = false;
                    if (answer != null) {
                        s_logger.error(answer.getDetails());
                    }
                }
            }

            // Either way delete the snapshots for this volume.
            List<SnapshotVO> snapshots = listSnapsforVolume(volumeId);
            for (SnapshotVO snapshot : snapshots) {
                if (_snapshotDao.expunge(snapshot.getId())) {
                    if (snapshot.getType() == Type.MANUAL) {
                        _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.snapshot);
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
    public SnapshotPolicyVO createPolicy(CreateSnapshotPolicyCmd cmd, Account policyOwner) {
        Long volumeId = cmd.getVolumeId();
        VolumeVO volume = _volsDao.findById(cmd.getVolumeId());
        if (volume == null) {
            throw new InvalidParameterValueException("Failed to create snapshot policy, unable to find a volume with id " + volumeId);
        }
        
        _accountMgr.checkAccess(UserContext.current().getCaller(), null, true, volume);
        
        if (volume.getState() != Volume.State.Ready) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " is not in " + Volume.State.Ready + " state but " + volume.getState() + ". Cannot take snapshot.");
        }

        if (volume.getTemplateId() != null ) {
            VMTemplateVO  template = _templateDao.findById(volume.getTemplateId());
            if( template != null && template.getTemplateType() == Storage.TemplateType.SYSTEM ) {
                throw new InvalidParameterValueException("VolumeId: " + volumeId + " is for System VM , Creating snapshot against System VM volumes is not supported");
            }
        }

        AccountVO owner = _accountDao.findById(volume.getAccountId());
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
        long accountLimit = _resourceLimitMgr.findCorrectResourceLimitForAccount(owner, ResourceType.snapshot);
        long domainLimit = _resourceLimitMgr.findCorrectResourceLimitForDomain(_domainMgr.getDomain(owner.getDomainId()), ResourceType.snapshot);
        int max = cmd.getMaxSnaps().intValue();
        if (owner.getType() != Account.ACCOUNT_TYPE_ADMIN && ((accountLimit != -1 && max > accountLimit) || (domainLimit != -1 && max > domainLimit))) {
        	String message = "domain/account";
        	if (owner.getType() == Account.ACCOUNT_TYPE_PROJECT) {
        		message = "domain/project";
        	}
        	
            throw new InvalidParameterValueException("Max number of snapshots shouldn't exceed the " + message + " level snapshot limit");
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
        _accountMgr.checkAccess(UserContext.current().getCaller(), null, true, volume);
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
        DataCenter zone = _dcDao.findById(volume.getDataCenterId());
        if (zone == null) {
            throw new InvalidParameterValueException("Can't find zone by id " + volume.getDataCenterId());
        }
        
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getType())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + zone.getName());
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
        
        StoragePoolVO storagePoolVO = _storagePoolDao.findById(volume.getPoolId());
        if (storagePoolVO == null) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " please attach this volume to a VM before create snapshot for it");
        }

        ClusterVO cluster = _clusterDao.findById(storagePoolVO.getClusterId());
        if (cluster != null && cluster.getHypervisorType() == HypervisorType.Ovm) {
        	throw new InvalidParameterValueException("Ovm won't support taking snapshot");
        }
        
        // Verify permissions
        _accountMgr.checkAccess(caller, null, true, volume);
        Type snapshotType = getSnapshotType(policyId);
        Account owner = _accountMgr.getAccount(volume.getAccountId());
        try{
        	_resourceLimitMgr.checkResourceLimit(owner, ResourceType.snapshot);
        } catch (ResourceAllocationException e){
        	if (snapshotType != Type.MANUAL){
        		String msg = "Snapshot resource limit exceeded for account id : " + owner.getId() + ". Failed to create recurring snapshots";
        		s_logger.warn(msg);
        		_alertMgr.sendAlert(AlertManager.ALERT_TYPE_UPDATE_RESOURCE_COUNT, 0L, 0L, msg, 
        				"Snapshot resource limit exceeded for account id : " + owner.getId() + ". Failed to create recurring snapshots; please use updateResourceLimit to increase the limit");
        	}
        	throw e;
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
        HypervisorType hypervisorType = this._volsDao.getHypervisorType(volumeId);
        SnapshotVO snapshotVO = new SnapshotVO(volume.getDataCenterId(), volume.getAccountId(), volume.getDomainId(), volume.getId(), volume.getDiskOfferingId(), null, snapshotName,
                (short) snapshotType.ordinal(), snapshotType.name(), volume.getSize(), hypervisorType);
        SnapshotVO snapshot = _snapshotDao.persist(snapshotVO);
        if (snapshot == null) {
            throw new CloudRuntimeException("Failed to create snapshot for volume: "+volumeId);
        }
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

            _accountMgr.checkAccess(UserContext.current().getCaller(), null, true, volume);
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
    
    @Override
    public boolean canOperateOnVolume(VolumeVO volume) {
    	List<SnapshotVO> snapshots = _snapshotDao.listByStatus(volume.getId(), Status.Creating, Status.CreatedOnPrimary, Status.BackingUp);
    	if (snapshots.size() > 0) {
    		return false;
    	}
    	return true;
    }

}
