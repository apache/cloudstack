// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.storage.snapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.command.user.snapshot.CreateSnapshotPolicyCmd;
import org.apache.cloudstack.api.command.user.snapshot.DeleteSnapshotPoliciesCmd;
import org.apache.cloudstack.api.command.user.snapshot.ListSnapshotPoliciesCmd;
import org.apache.cloudstack.api.command.user.snapshot.ListSnapshotsCmd;
import org.apache.cloudstack.api.command.user.snapshot.UpdateSnapshotPolicyCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy.SnapshotOperation;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.DeleteSnapshotsDirCommand;
import com.cloud.alert.AlertManager;
import com.cloud.api.commands.ListRecurringSnapshotScheduleCmd;
import com.cloud.configuration.Config;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.storage.CreateSnapshotPayload;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Snapshot.Type;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotScheduleVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.SnapshotScheduleDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.DateUtil.IntervalType;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

@Component
@Local(value = {SnapshotManager.class, SnapshotApiService.class})
public class SnapshotManagerImpl extends ManagerBase implements SnapshotManager, SnapshotApiService {
    private static final Logger s_logger = Logger.getLogger(SnapshotManagerImpl.class);
    @Inject
    private VMTemplateDao _templateDao;
    @Inject
    private UserVmDao _vmDao;
    @Inject
    private VolumeDao _volsDao;
    @Inject
    private AccountDao _accountDao;
    @Inject
    private SnapshotDao _snapshotDao;
    @Inject
    private SnapshotDataStoreDao _snapshotStoreDao;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    private final SnapshotPolicyDao _snapshotPolicyDao = null;
    @Inject
    private SnapshotScheduleDao _snapshotScheduleDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private StorageManager _storageMgr;
    @Inject
    private SnapshotScheduler _snapSchedMgr;
    @Inject
    private AccountManager _accountMgr;
    @Inject
    private AlertManager _alertMgr;
    @Inject
    private ClusterDao _clusterDao;
    @Inject
    private ResourceLimitService _resourceLimitMgr;
    @Inject
    private DomainManager _domainMgr;
    @Inject
    private ResourceTagDao _resourceTagDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private VMSnapshotDao _vmSnapshotDao;
    @Inject
    private DataStoreManager dataStoreMgr;
    @Inject
    private SnapshotService snapshotSrv;
    @Inject
    private VolumeDataFactory volFactory;
    @Inject
    private SnapshotDataFactory snapshotFactory;
    @Inject
    private EndPointSelector _epSelector;
    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    private StorageStrategyFactory _storageStrategyFactory;

    private int _totalRetries;
    private int _pauseInterval;

    @Override
    public Answer sendToPool(Volume vol, Command cmd) {
        StoragePool pool = (StoragePool)dataStoreMgr.getPrimaryDataStore(vol.getPoolId());
        long[] hostIdsToTryFirst = null;

        Long vmHostId = getHostIdForSnapshotOperation(vol);

        if (vmHostId != null) {
            hostIdsToTryFirst = new long[] {vmHostId};
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
                s_logger.debug("[ignored] interupted while retry cmd.");
            }

            s_logger.debug("Retrying...");
        }

        s_logger.warn("After " + _totalRetries + " retries, the command " + cmd.getClass().getName() + " did not succeed.");

        return null;
    }

    @Override
    public Long getHostIdForSnapshotOperation(Volume vol) {
        VMInstanceVO vm = _vmDao.findById(vol.getInstanceId());
        if (vm != null) {
            if (vm.getHostId() != null) {
                return vm.getHostId();
            } else if (vm.getLastHostId() != null) {
                return vm.getLastHostId();
            }
        }
        return null;
    }

    @Override
    public boolean revertSnapshot(Long snapshotId) {
        Snapshot snapshot = _snapshotDao.findById(snapshotId);
        if (snapshot == null) {
            throw new InvalidParameterValueException("No such snapshot");
        }

        Volume volume = _volsDao.findById(snapshot.getVolumeId());
        Long instanceId = volume.getInstanceId();

        // If this volume is attached to an VM, then the VM needs to be in the stopped state
        // in order to revert the volume
        if (instanceId != null) {
            UserVmVO vm = _vmDao.findById(instanceId);
            if (vm.getState() != State.Stopped && vm.getState() != State.Shutdowned) {
                throw new InvalidParameterValueException("The VM the specified disk is attached to is not in the shutdown state.");
            }
        }

        SnapshotStrategy snapshotStrategy = _storageStrategyFactory.getSnapshotStrategy(snapshot, SnapshotOperation.REVERT);

        if (snapshotStrategy == null) {
            s_logger.error("Unable to find snaphot strategy to handle snapshot with id '" + snapshotId + "'");
            return false;
        }

        return snapshotStrategy.revertSnapshot(snapshotId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SNAPSHOT_POLICY_UPDATE, eventDescription = "updating snapshot policy", async = true)
    public SnapshotPolicy updateSnapshotPolicy(UpdateSnapshotPolicyCmd cmd) {

        Long id = cmd.getId();
        String customUUID = cmd.getCustomId();
        Boolean display = cmd.getDisplay();

        SnapshotPolicyVO policyVO = _snapshotPolicyDao.findById(id);
        if(display != null){
            boolean previousDisplay = policyVO.isDisplay();
            policyVO.setDisplay(display);
            _snapSchedMgr.scheduleOrCancelNextSnapshotJobOnDisplayChange(policyVO, previousDisplay);
        }

        if(customUUID != null)
            policyVO.setUuid(customUUID);

        _snapshotPolicyDao.update(id, policyVO);

        return policyVO;

    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_SNAPSHOT_CREATE, eventDescription = "creating snapshot", async = true)
    public Snapshot createSnapshot(Long volumeId, Long policyId, Long snapshotId, Account snapshotOwner) {
        VolumeInfo volume = volFactory.getVolume(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("No such volume exist");
        }

        if (volume.getState() != Volume.State.Ready) {
            throw new InvalidParameterValueException("Volume is not in ready state");
        }


        // does the caller have the authority to act on this volume
        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, volume);

        SnapshotInfo snapshot = snapshotFactory.getSnapshot(snapshotId, DataStoreRole.Primary);
        if(snapshot == null)
        {
            s_logger.debug("Failed to create snapshot");
            throw new CloudRuntimeException("Failed to create snapshot");
        }
        try {
            postCreateSnapshot(volumeId, snapshot.getId(), policyId);
            //Check if the snapshot was removed while backingUp. If yes, do not log snapshot create usage event
            SnapshotVO freshSnapshot = _snapshotDao.findById(snapshot.getId());
            if (freshSnapshot != null)  {
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_SNAPSHOT_CREATE, snapshot.getAccountId(), snapshot.getDataCenterId(), snapshotId, snapshot.getName(),
                    null, null, volume.getSize(), snapshot.getClass().getName(), snapshot.getUuid());
            }
            _resourceLimitMgr.incrementResourceCount(snapshotOwner.getId(), ResourceType.snapshot);

        } catch (Exception e) {
            s_logger.debug("Failed to create snapshot", e);
            throw new CloudRuntimeException("Failed to create snapshot", e);
        }

        return snapshot;
    }

    @Override
    public Snapshot backupSnapshot(Long snapshotId) {
        SnapshotInfo snapshot = snapshotFactory.getSnapshot(snapshotId, DataStoreRole.Image);
        if (snapshot != null) {
            throw new CloudRuntimeException("Already in the backup snapshot:" + snapshotId);
        }

         return snapshotSrv.backupSnapshot(snapshot);
    }

    @Override
    public SnapshotVO getParentSnapshot(VolumeInfo volume) {
        long preId = _snapshotDao.getLastSnapshot(volume.getId(), DataStoreRole.Primary);

         SnapshotVO preSnapshotVO = null;
         if (preId != 0 && !(volume.getLastPoolId() != null && !volume.getLastPoolId().equals(volume.getPoolId()))) {
             preSnapshotVO = _snapshotDao.findByIdIncludingRemoved(preId);
         }

         return preSnapshotVO;
    }

    private Long getSnapshotUserId() {
        Long userId = CallContext.current().getCallingUserId();
        if (userId == null) {
            return User.UID_SYSTEM;
        }
        return userId;
    }

    private void postCreateSnapshot(Long volumeId, Long snapshotId, Long policyId) {
        Long userId = getSnapshotUserId();
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
        if (policyId != Snapshot.MANUAL_POLICY_ID) {
            SnapshotScheduleVO snapshotSchedule = _snapshotScheduleDao.getCurrentSchedule(volumeId, policyId, true);
            assert snapshotSchedule != null;
            snapshotSchedule.setSnapshotId(snapshotId);
            _snapshotScheduleDao.update(snapshotSchedule.getId(), snapshotSchedule);
        }

        if (snapshot != null && snapshot.isRecursive()) {
            postCreateRecurringSnapshotForPolicy(userId, volumeId, snapshotId, policyId);
        }
    }

    private void postCreateRecurringSnapshotForPolicy(long userId, long volumeId, long snapshotId, long policyId) {
        // Use count query
        SnapshotVO spstVO = _snapshotDao.findById(snapshotId);
        Type type = spstVO.getRecurringType();
        int maxSnaps = type.getMax();

        List<SnapshotVO> snaps = listSnapsforVolumeType(volumeId, type);
        SnapshotPolicyVO policy = _snapshotPolicyDao.findById(policyId);
        if (policy != null && policy.getMaxSnaps() < maxSnaps) {
            maxSnaps = policy.getMaxSnaps();
        }
        while (snaps.size() > maxSnaps && snaps.size() > 1) {
            SnapshotVO oldestSnapshot = snaps.get(0);
            long oldSnapId = oldestSnapshot.getId();
            if (policy != null) {
            s_logger.debug("Max snaps: " + policy.getMaxSnaps() + " exceeded for snapshot policy with Id: " + policyId + ". Deleting oldest snapshot: " + oldSnapId);
            }
            if (deleteSnapshot(oldSnapId)) {
                //log Snapshot delete event
                ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, oldestSnapshot.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_SNAPSHOT_DELETE,
                    "Successfully deleted oldest snapshot: " + oldSnapId, 0);
            }
            snaps.remove(oldestSnapshot);
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_SNAPSHOT_DELETE, eventDescription = "deleting snapshot", async = true)
    public boolean deleteSnapshot(long snapshotId) {
        Account caller = CallContext.current().getCallingAccount();

        // Verify parameters
        SnapshotVO snapshotCheck = _snapshotDao.findById(snapshotId);

        if (snapshotCheck == null) {
            throw new InvalidParameterValueException("unable to find a snapshot with id " + snapshotId);
        }

        _accountMgr.checkAccess(caller, null, true, snapshotCheck);

        SnapshotStrategy snapshotStrategy = _storageStrategyFactory.getSnapshotStrategy(snapshotCheck, SnapshotOperation.DELETE);

        if (snapshotStrategy == null) {
            s_logger.error("Unable to find snaphot strategy to handle snapshot with id '" + snapshotId + "'");

            return false;
        }

        try {
            boolean result = snapshotStrategy.deleteSnapshot(snapshotId);

            if (result) {
                if (snapshotCheck.getState() == Snapshot.State.BackedUp) {
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_SNAPSHOT_DELETE, snapshotCheck.getAccountId(), snapshotCheck.getDataCenterId(), snapshotId,
                        snapshotCheck.getName(), null, null, 0L, snapshotCheck.getClass().getName(), snapshotCheck.getUuid());
                }

                if (snapshotCheck.getState() != Snapshot.State.Error && snapshotCheck.getState() != Snapshot.State.Destroyed) {
                    _resourceLimitMgr.decrementResourceCount(snapshotCheck.getAccountId(), ResourceType.snapshot);
                }

                if (snapshotCheck.getState() == Snapshot.State.BackedUp) {
                    SnapshotDataStoreVO snapshotStoreRef = _snapshotStoreDao.findBySnapshot(snapshotId, DataStoreRole.Image);

                    if (snapshotStoreRef != null) {
                        _resourceLimitMgr.decrementResourceCount(snapshotCheck.getAccountId(), ResourceType.secondary_storage, new Long(snapshotStoreRef.getPhysicalSize()));
                    }
                }
            }

            return result;
        } catch (Exception e) {
            s_logger.debug("Failed to delete snapshot: " + snapshotCheck.getId() + ":" + e.toString());

            throw new CloudRuntimeException("Failed to delete snapshot:" + e.toString());
        }
    }

    @Override
    public String getSecondaryStorageURL(SnapshotVO snapshot) {
        SnapshotDataStoreVO snapshotStore = _snapshotStoreDao.findBySnapshot(snapshot.getId(), DataStoreRole.Image);
        if (snapshotStore != null) {
            DataStore store = dataStoreMgr.getDataStore(snapshotStore.getDataStoreId(), DataStoreRole.Image);
            if (store != null) {
                return store.getUri();
            }
        }
        throw new CloudRuntimeException("Can not find secondary storage hosting the snapshot");
    }

    @Override
    public Pair<List<? extends Snapshot>, Integer> listSnapshots(ListSnapshotsCmd cmd) {
        Long volumeId = cmd.getVolumeId();
        String name = cmd.getSnapshotName();
        Long id = cmd.getId();
        String keyword = cmd.getKeyword();
        String snapshotTypeStr = cmd.getSnapshotType();
        String intervalTypeStr = cmd.getIntervalType();
        Map<String, String> tags = cmd.getTags();
        Long zoneId = cmd.getZoneId();
        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<Long>();

        // Verify parameters
        if (volumeId != null) {
            VolumeVO volume = _volsDao.findById(volumeId);
            if (volume != null) {
                _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, volume);
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

        sb.and("statusNEQ", sb.entity().getState(), SearchCriteria.Op.NEQ); //exclude those Destroyed snapshot, not showing on UI
        sb.and("volumeId", sb.entity().getVolumeId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("snapshotTypeEQ", sb.entity().getsnapshotType(), SearchCriteria.Op.IN);
        sb.and("snapshotTypeNEQ", sb.entity().getsnapshotType(), SearchCriteria.Op.NEQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);

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

        SearchCriteria<SnapshotVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sc.setParameters("statusNEQ", Snapshot.State.Destroyed);

        if (volumeId != null) {
            sc.setParameters("volumeId", volumeId);
        }

        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", ResourceObjectType.Snapshot.toString());
            for (String key : tags.keySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), key);
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), tags.get(key));
                count++;
            }
        }

        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
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
            Type snapshotType = SnapshotVO.getSnapshotType(snapshotTypeStr);
            if (snapshotType == null) {
                throw new InvalidParameterValueException("Unsupported snapshot type " + snapshotTypeStr);
            }
            if (snapshotType == Type.RECURRING) {
                sc.setParameters("snapshotTypeEQ", Type.HOURLY.ordinal(), Type.DAILY.ordinal(), Type.WEEKLY.ordinal(), Type.MONTHLY.ordinal());
            } else {
                sc.setParameters("snapshotTypeEQ", snapshotType.ordinal());
            }
        } else if (intervalTypeStr != null && volumeId != null) {
            Type type = SnapshotVO.getSnapshotType(intervalTypeStr);
            if (type == null) {
                throw new InvalidParameterValueException("Unsupported snapstho interval type " + intervalTypeStr);
            }
            sc.setParameters("snapshotTypeEQ", type.ordinal());
        } else {
            // Show only MANUAL and RECURRING snapshot types
            sc.setParameters("snapshotTypeNEQ", Snapshot.Type.TEMPLATE.ordinal());
        }

        Pair<List<SnapshotVO>, Integer> result = _snapshotDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Snapshot>, Integer>(result.first(), result.second());
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
            List<DataStore> ssHosts = dataStoreMgr.getImageStoresByScope(new ZoneScope(dcId));
            for (DataStore ssHost : ssHosts) {
                String snapshotDir = TemplateConstants.DEFAULT_SNAPSHOT_ROOT_DIR + "/" + accountId + "/" + volumeId;
                DeleteSnapshotsDirCommand cmd = new DeleteSnapshotsDirCommand(ssHost.getTO(), snapshotDir);
                EndPoint ep = _epSelector.select(ssHost);
                Answer answer = null;
                if (ep == null) {
                    String errMsg = "No remote endpoint to send command, check if host or ssvm is down?";
                    s_logger.error(errMsg);
                    answer = new Answer(cmd, false, errMsg);
                } else {
                    answer = ep.sendMessage(cmd);
                }
                if ((answer != null) && answer.getResult()) {
                    s_logger.debug("Deleted all snapshots for volume: " + volumeId + " under account: " + accountId);
                } else {
                    success = false;
                    if (answer != null) {
                        s_logger.warn("Failed to delete all snapshot for volume " + volumeId + " on secondary storage " + ssHost.getUri());
                        s_logger.error(answer.getDetails());
                    }
                }
            }

            // Either way delete the snapshots for this volume.
            List<SnapshotVO> snapshots = listSnapsforVolume(volumeId);
            for (SnapshotVO snapshot : snapshots) {
                SnapshotStrategy snapshotStrategy = _storageStrategyFactory.getSnapshotStrategy(snapshot, SnapshotOperation.DELETE);
                if (snapshotStrategy == null) {
                    s_logger.error("Unable to find snaphot strategy to handle snapshot with id '" + snapshot.getId() + "'");
                    continue;
                }
                SnapshotDataStoreVO snapshotStoreRef = _snapshotStoreDao.findBySnapshot(snapshot.getId(), DataStoreRole.Image);

                if (snapshotStrategy.deleteSnapshot(snapshot.getId())) {
                    if (Type.MANUAL == snapshot.getRecurringType()) {
                        _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.snapshot);
                        if  (snapshotStoreRef != null) {
                            _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.secondary_storage, new Long(snapshotStoreRef.getPhysicalSize()));
                        }
                    }

                    // Log event after successful deletion
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_SNAPSHOT_DELETE, snapshot.getAccountId(), volume.getDataCenterId(), snapshot.getId(),
                        snapshot.getName(), null, null, volume.getSize(), snapshot.getClass().getName(), snapshot.getUuid());
                }
            }
        }

        // Returns true if snapshotsDir has been deleted for all volumes.
        return success;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_SNAPSHOT_POLICY_CREATE, eventDescription = "creating snapshot policy")
    public SnapshotPolicyVO createPolicy(CreateSnapshotPolicyCmd cmd, Account policyOwner) {
        Long volumeId = cmd.getVolumeId();
        boolean display = cmd.isDisplay();
        VolumeVO volume = _volsDao.findById(cmd.getVolumeId());
        if (volume == null) {
            throw new InvalidParameterValueException("Failed to create snapshot policy, unable to find a volume with id " + volumeId);
        }

        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, volume);

        // If display is false we don't actually schedule snapshots.
        if (volume.getState() != Volume.State.Ready && display) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " is not in " + Volume.State.Ready + " state but " + volume.getState() +
                ". Cannot take snapshot.");
        }

        if (volume.getTemplateId() != null) {
            VMTemplateVO  template = _templateDao.findById(volume.getTemplateId());
            if (template != null && template.getTemplateType() == Storage.TemplateType.SYSTEM) {
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

        // Verify that max doesn't exceed domain and account snapshot limits in case display is on
        if(display){
            long accountLimit = _resourceLimitMgr.findCorrectResourceLimitForAccount(owner, ResourceType.snapshot);
            long domainLimit = _resourceLimitMgr.findCorrectResourceLimitForDomain(_domainMgr.getDomain(owner.getDomainId()), ResourceType.snapshot);
            int max = cmd.getMaxSnaps().intValue();
            if (!_accountMgr.isRootAdmin(owner.getId())&& ((accountLimit != -1 && max > accountLimit) || (domainLimit != -1 && max > domainLimit))) {
                String message = "domain/account";
                if (owner.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                    message = "domain/project";
                }

                throw new InvalidParameterValueException("Max number of snapshots shouldn't exceed the " + message + " level snapshot limit");
            }
        }
        SnapshotPolicyVO policy = _snapshotPolicyDao.findOneByVolumeInterval(volumeId, intvType);
        if (policy == null) {
            policy = new SnapshotPolicyVO(volumeId, cmd.getSchedule(), timezoneId, intvType, cmd.getMaxSnaps(), display);
            policy = _snapshotPolicyDao.persist(policy);
            _snapSchedMgr.scheduleNextSnapshotJob(policy);
        } else {
            try {
                boolean previousDisplay = policy.isDisplay();
                policy = _snapshotPolicyDao.acquireInLockTable(policy.getId());
                policy.setSchedule(cmd.getSchedule());
                policy.setTimezone(timezoneId);
                policy.setInterval((short)intvType.ordinal());
                policy.setMaxSnaps(cmd.getMaxSnaps());
                policy.setActive(true);
                policy.setDisplay(display);
                _snapshotPolicyDao.update(policy.getId(), policy);
                _snapSchedMgr.scheduleOrCancelNextSnapshotJobOnDisplayChange(policy, previousDisplay);
            } finally {
                if (policy != null) {
                    _snapshotPolicyDao.releaseFromLockTable(policy.getId());
                }
            }

        }
        // TODO - Make createSnapshotPolicy - BaseAsyncCreate and remove this.
        CallContext.current().putContextParameter(SnapshotPolicy.class.getName(), policy.getUuid());
        return policy;
    }

    protected boolean deletePolicy(long userId, Long policyId) {
        SnapshotPolicyVO snapshotPolicy = _snapshotPolicyDao.findById(policyId);
        _snapSchedMgr.removeSchedule(snapshotPolicy.getVolumeId(), snapshotPolicy.getId());
        return _snapshotPolicyDao.remove(policyId);
    }

    @Override
    public Pair<List<? extends SnapshotPolicy>, Integer> listPoliciesforVolume(ListSnapshotPoliciesCmd cmd) {
        Long volumeId = cmd.getVolumeId();
        boolean display = cmd.isDisplay();
        Long id = cmd.getId();
        Pair<List<SnapshotPolicyVO>, Integer> result = null;
        // TODO - Have a better way of doing this.
        if(id != null){
            result = _snapshotPolicyDao.listAndCountById(id, display, null);
            if(result != null && result.first() != null && !result.first().isEmpty()){
                SnapshotPolicyVO snapshotPolicy = result.first().get(0);
                volumeId = snapshotPolicy.getVolumeId();
            }
        }
        VolumeVO volume = _volsDao.findById(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Unable to find a volume with id " + volumeId);
        }
        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, volume);
        if(result != null)
            return new Pair<List<? extends SnapshotPolicy>, Integer>(result.first(), result.second());
        result = _snapshotPolicyDao.listAndCountByVolumeId(volumeId, display);
        return new Pair<List<? extends SnapshotPolicy>, Integer>(result.first(), result.second());
    }

    private List<SnapshotPolicyVO> listPoliciesforVolume(long volumeId) {
        return _snapshotPolicyDao.listByVolumeId(volumeId);
    }

    private List<SnapshotVO> listSnapsforVolume(long volumeId) {
        return _snapshotDao.listByVolumeId(volumeId);
    }

    private List<SnapshotVO> listSnapsforVolumeType(long volumeId, Type type) {
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

    @Override
    public List<SnapshotScheduleVO> findRecurringSnapshotSchedule(ListRecurringSnapshotScheduleCmd cmd) {
        Long volumeId = cmd.getVolumeId();
        Long policyId = cmd.getSnapshotPolicyId();
        Account account = CallContext.current().getCallingAccount();

        // Verify parameters
        VolumeVO volume = _volsDao.findById(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Failed to list snapshot schedule, unable to find a volume with id " + volumeId);
        }

        if (account != null) {
            long volAcctId = volume.getAccountId();
            if (_accountMgr.isAdmin(account.getId())) {
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

    private Type getSnapshotType(Long policyId) {
        if (policyId.equals(Snapshot.MANUAL_POLICY_ID)) {
            return Type.MANUAL;
        } else {
            SnapshotPolicyVO spstPolicyVO = _snapshotPolicyDao.findById(policyId);
            IntervalType intvType = DateUtil.getIntervalType(spstPolicyVO.getInterval());
            return getSnapshotType(intvType);
        }
    }

    private Type getSnapshotType(IntervalType intvType) {
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

    private boolean hostSupportSnapsthotForVolume(HostVO host, VolumeInfo volume) {
        if (host.getHypervisorType() != HypervisorType.KVM) {
            return true;
        }

        //Turn off snapshot by default for KVM if the volume attached to vm that is not in the Stopped/Destroyed state,
        //unless it is set in the global flag
        Long vmId = volume.getInstanceId();
        if (vmId != null) {
            VMInstanceVO vm = _vmDao.findById(vmId);
            if (vm.getState() != VirtualMachine.State.Stopped && vm.getState() != VirtualMachine.State.Destroyed) {
                boolean snapshotEnabled = Boolean.parseBoolean(_configDao.getValue("kvm.snapshot.enabled"));
                if (!snapshotEnabled) {
                    s_logger.debug("Snapshot is not supported on host " + host + " for the volume " + volume + " attached to the vm " + vm);
                    return false;
                }
            }
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

    private boolean supportedByHypervisor(VolumeInfo volume) {
        HypervisorType hypervisorType;
        StoragePoolVO storagePool = _storagePoolDao.findById(volume.getDataStore().getId());
        ScopeType scope = storagePool.getScope();
        if (scope.equals(ScopeType.ZONE)) {
            hypervisorType = storagePool.getHypervisor();
        } else {
            hypervisorType = volume.getHypervisorType();
        }

        if (hypervisorType.equals(HypervisorType.Ovm)) {
            throw new InvalidParameterValueException("Ovm won't support taking snapshot");
        }

        if (hypervisorType.equals(HypervisorType.KVM)) {
            List<HostVO> hosts = null;
            if (scope.equals(ScopeType.CLUSTER)) {
                ClusterVO cluster = _clusterDao.findById(storagePool.getClusterId());
                hosts = _resourceMgr.listAllHostsInCluster(cluster.getId());
            } else if (scope.equals(ScopeType.ZONE)) {
                hosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(hypervisorType, volume.getDataCenterId());
            }
            if (hosts != null && !hosts.isEmpty()) {
                HostVO host = hosts.get(0);
                if (!hostSupportSnapsthotForVolume(host, volume)) {
                    throw new CloudRuntimeException("KVM Snapshot is not supported: " + host.getId());
                }
            }
        }

        // if volume is attached to a vm in destroyed or expunging state; disallow
        if (volume.getInstanceId() != null) {
            UserVmVO userVm = _vmDao.findById(volume.getInstanceId());
            if (userVm != null) {
                if (userVm.getState().equals(State.Destroyed) || userVm.getState().equals(State.Expunging)) {
                    throw new CloudRuntimeException("Creating snapshot failed due to volume:" + volume.getId() + " is associated with vm:" + userVm.getInstanceName() +
                        " is in " + userVm.getState().toString() + " state");
                }

                if (userVm.getHypervisorType() == HypervisorType.VMware || userVm.getHypervisorType() == HypervisorType.KVM) {
                    List<SnapshotVO> activeSnapshots =
                        _snapshotDao.listByInstanceId(volume.getInstanceId(), Snapshot.State.Creating, Snapshot.State.CreatedOnPrimary, Snapshot.State.BackingUp);
                    if (activeSnapshots.size() > 0) {
                        throw new InvalidParameterValueException("There is other active snapshot tasks on the instance to which the volume is attached, please try again later");
                    }
                }

                List<VMSnapshotVO> activeVMSnapshots =
                    _vmSnapshotDao.listByInstanceId(userVm.getId(), VMSnapshot.State.Creating, VMSnapshot.State.Reverting, VMSnapshot.State.Expunging);
                if (activeVMSnapshots.size() > 0) {
                    throw new CloudRuntimeException("There is other active vm snapshot tasks on the instance to which the volume is attached, please try again later");
                }
            }
        }

        return true;
    }

    @Override
    @DB
    public SnapshotInfo takeSnapshot(VolumeInfo volume) throws ResourceAllocationException {
        CreateSnapshotPayload payload = (CreateSnapshotPayload)volume.getpayload();
        Long snapshotId = payload.getSnapshotId();
        Account snapshotOwner = payload.getAccount();
        SnapshotInfo snapshot = snapshotFactory.getSnapshot(snapshotId, volume.getDataStore());
        snapshot.addPayload(payload);
        try {
            SnapshotStrategy snapshotStrategy = _storageStrategyFactory.getSnapshotStrategy(snapshot, SnapshotOperation.TAKE);

            if (snapshotStrategy == null) {
                throw new CloudRuntimeException("Can't find snapshot strategy to deal with snapshot:" + snapshotId);
            }

            snapshotStrategy.takeSnapshot(snapshot);

            try {
                postCreateSnapshot(volume.getId(), snapshotId, payload.getSnapshotPolicyId());
                SnapshotDataStoreVO snapshotStoreRef = _snapshotStoreDao.findBySnapshot(snapshotId, DataStoreRole.Image);
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_SNAPSHOT_CREATE, snapshot.getAccountId(), snapshot.getDataCenterId(), snapshotId, snapshot.getName(),
                    null, null, snapshotStoreRef.getPhysicalSize(), volume.getSize(), snapshot.getClass().getName(), snapshot.getUuid());
                // Correct the resource count of snapshot in case of delta snapshots.
                _resourceLimitMgr.decrementResourceCount(snapshotOwner.getId(), ResourceType.secondary_storage, new Long(volume.getSize() - snapshotStoreRef.getPhysicalSize()));
            } catch (Exception e) {
                s_logger.debug("post process snapshot failed", e);
            }
        } catch (Exception e) {
            s_logger.debug("Failed to create snapshot", e);
            _resourceLimitMgr.decrementResourceCount(snapshotOwner.getId(), ResourceType.snapshot);
            _resourceLimitMgr.decrementResourceCount(snapshotOwner.getId(), ResourceType.secondary_storage, new Long(volume.getSize()));
            throw new CloudRuntimeException("Failed to create snapshot", e);
        }
        return snapshot;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        String value = _configDao.getValue(Config.BackupSnapshotWait.toString());

        Type.HOURLY.setMax(NumbersUtil.parseInt(_configDao.getValue("snapshot.max.hourly"), HOURLYMAX));
        Type.DAILY.setMax(NumbersUtil.parseInt(_configDao.getValue("snapshot.max.daily"), DAILYMAX));
        Type.WEEKLY.setMax(NumbersUtil.parseInt(_configDao.getValue("snapshot.max.weekly"), WEEKLYMAX));
        Type.MONTHLY.setMax(NumbersUtil.parseInt(_configDao.getValue("snapshot.max.monthly"), MONTHLYMAX));
        _totalRetries = NumbersUtil.parseInt(_configDao.getValue("total.retries"), 4);
        _pauseInterval = 2 * NumbersUtil.parseInt(_configDao.getValue("ping.interval"), 60);

        s_logger.info("Snapshot Manager is configured.");

        return true;
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

            _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, volume);
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

    @Override
    public boolean canOperateOnVolume(Volume volume) {
        List<SnapshotVO> snapshots = _snapshotDao.listByStatus(volume.getId(), Snapshot.State.Creating, Snapshot.State.CreatedOnPrimary, Snapshot.State.BackingUp);
        if (snapshots.size() > 0) {
            return false;
        }
        return true;
    }

    @Override
    public Snapshot allocSnapshot(Long volumeId, Long policyId, String snapshotName) throws ResourceAllocationException {
        Account caller = CallContext.current().getCallingAccount();
        VolumeInfo volume = volFactory.getVolume(volumeId);
        supportedByHypervisor(volume);

        // Verify permissions
        _accountMgr.checkAccess(caller, null, true, volume);
        Type snapshotType = getSnapshotType(policyId);
        Account owner = _accountMgr.getAccount(volume.getAccountId());

        try {
            _resourceLimitMgr.checkResourceLimit(owner, ResourceType.snapshot);
                _resourceLimitMgr.checkResourceLimit(owner, ResourceType.secondary_storage, new Long(volume.getSize()).longValue());
        } catch (ResourceAllocationException e) {
            if (snapshotType != Type.MANUAL) {
                String msg = "Snapshot resource limit exceeded for account id : " + owner.getId() + ". Failed to create recurring snapshots";
                s_logger.warn(msg);
                _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_UPDATE_RESOURCE_COUNT, 0L, 0L, msg,
                    "Snapshot resource limit exceeded for account id : " + owner.getId() +
                        ". Failed to create recurring snapshots; please use updateResourceLimit to increase the limit");
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
        if (snapshotName == null)
            snapshotName = vmDisplayName + "_" + volume.getName() + "_" + timeString;

        HypervisorType hypervisorType = HypervisorType.None;
        StoragePoolVO storagePool = _storagePoolDao.findById(volume.getDataStore().getId());
        if (storagePool.getScope() == ScopeType.ZONE) {
            hypervisorType = storagePool.getHypervisor();

            // at the time being, managed storage only supports XenServer, ESX(i), and KVM (i.e. not Hyper-V), so the VHD file type can be mapped to XenServer
            if (storagePool.isManaged() && HypervisorType.Any.equals(hypervisorType) && ImageFormat.VHD.equals(volume.getFormat())) {
                hypervisorType = HypervisorType.XenServer;
            }
        } else {
            hypervisorType = volume.getHypervisorType();
        }

        SnapshotVO snapshotVO =
            new SnapshotVO(volume.getDataCenterId(), volume.getAccountId(), volume.getDomainId(), volume.getId(), volume.getDiskOfferingId(), snapshotName,
                (short)snapshotType.ordinal(), snapshotType.name(), volume.getSize(), volume.getMinIops(), volume.getMaxIops(), hypervisorType);

        SnapshotVO snapshot = _snapshotDao.persist(snapshotVO);
        if (snapshot == null) {
            throw new CloudRuntimeException("Failed to create snapshot for volume: " + volume.getId());
        }
        _resourceLimitMgr.incrementResourceCount(volume.getAccountId(), ResourceType.snapshot);
        _resourceLimitMgr.incrementResourceCount(volume.getAccountId(), ResourceType.secondary_storage, new Long(volume.getSize()));
        return snapshot;
    }



}
