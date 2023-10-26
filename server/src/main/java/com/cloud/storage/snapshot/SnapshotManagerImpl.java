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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.annotation.AnnotationService;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.user.snapshot.CopySnapshotCmd;
import org.apache.cloudstack.api.command.user.snapshot.CreateSnapshotPolicyCmd;
import org.apache.cloudstack.api.command.user.snapshot.DeleteSnapshotPoliciesCmd;
import org.apache.cloudstack.api.command.user.snapshot.ListSnapshotPoliciesCmd;
import org.apache.cloudstack.api.command.user.snapshot.ListSnapshotsCmd;
import org.apache.cloudstack.api.command.user.snapshot.UpdateSnapshotPolicyCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotResult;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy.SnapshotOperation;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.resourcedetail.SnapshotPolicyDetailVO;
import org.apache.cloudstack.resourcedetail.dao.SnapshotPolicyDetailsDao;
import org.apache.cloudstack.snapshot.SnapshotHelper;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.DeleteSnapshotsDirCommand;
import com.cloud.alert.AlertManager;
import com.cloud.api.commands.ListRecurringSnapshotScheduleCmd;
import com.cloud.api.query.MutualExclusiveIdsManagerBase;
import com.cloud.configuration.Config;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Grouping;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.server.TaggedResourceService;
import com.cloud.storage.CreateSnapshotPayload;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Snapshot.Type;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotScheduleVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.SnapshotScheduleDao;
import com.cloud.storage.dao.SnapshotZoneDao;
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
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotDetailsVO;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDetailsDao;

@Component
public class SnapshotManagerImpl extends MutualExclusiveIdsManagerBase implements SnapshotManager, SnapshotApiService, Configurable {
    private static final Logger s_logger = Logger.getLogger(SnapshotManagerImpl.class);
    @Inject
    VMTemplateDao _templateDao;
    @Inject
    UserVmDao _vmDao;
    @Inject
    VolumeDao _volsDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    SnapshotDao _snapshotDao;
    @Inject
    SnapshotDataStoreDao _snapshotStoreDao;
    @Inject
    PrimaryDataStoreDao _storagePoolDao;
    @Inject
    SnapshotPolicyDao _snapshotPolicyDao;
    @Inject
    SnapshotPolicyDetailsDao snapshotPolicyDetailsDao;
    @Inject
    SnapshotScheduleDao _snapshotScheduleDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    DiskOfferingDao diskOfferingDao;
    @Inject
    StorageManager _storageMgr;
    @Inject
    SnapshotScheduler _snapSchedMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    AlertManager _alertMgr;
    @Inject
    ClusterDao _clusterDao;
    @Inject
    ResourceLimitService _resourceLimitMgr;
    @Inject
    DomainManager _domainMgr;
    @Inject
    ResourceTagDao _resourceTagDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    VMSnapshotDao _vmSnapshotDao;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    SnapshotService snapshotSrv;
    @Inject
    VolumeDataFactory volFactory;
    @Inject
    SnapshotDataFactory snapshotFactory;
    @Inject
    EndPointSelector _epSelector;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    StorageStrategyFactory _storageStrategyFactory;
    @Inject
    public TaggedResourceService taggedResourceService;
    @Inject
    private AnnotationDao annotationDao;

    @Inject
    protected SnapshotHelper snapshotHelper;
    @Inject
    DataCenterDao dataCenterDao;
    @Inject
    SnapshotZoneDao snapshotZoneDao;
    @Inject
    VMSnapshotDetailsDao vmSnapshotDetailsDao;
    @Inject
    SnapshotDataFactory snapshotDataFactory;

    private int _totalRetries;
    private int _pauseInterval;
    private int snapshotBackupRetries, snapshotBackupRetryInterval;

    private ScheduledExecutorService backupSnapshotExecutor;

    protected DataStore getSnapshotZoneImageStore(long snapshotId, long zoneId) {
        List<SnapshotDataStoreVO> snapshotImageStoreList = _snapshotStoreDao.listReadyBySnapshot(snapshotId, DataStoreRole.Image);
        for (SnapshotDataStoreVO ref : snapshotImageStoreList) {
            Long entryZoneId = dataStoreMgr.getStoreZoneId(ref.getDataStoreId(), ref.getRole());
            if (entryZoneId != null && entryZoneId.equals(zoneId)) {
                return dataStoreMgr.getDataStore(ref.getDataStoreId(), ref.getRole());
            }
        }
        return null;
    }

    protected boolean isBackupSnapshotToSecondaryForZone(long zoneId) {
        if (Boolean.FALSE.equals(SnapshotInfo.BackupSnapshotAfterTakingSnapshot.value())) {
            return false;
        }
        DataCenterVO zone = dataCenterDao.findById(zoneId);
        return !DataCenter.Type.Edge.equals(zone.getType());
    }

    @Override
    public String getConfigComponentName() {
        return SnapshotManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {BackupRetryAttempts, BackupRetryInterval, SnapshotHourlyMax, SnapshotDailyMax, SnapshotMonthlyMax, SnapshotWeeklyMax, usageSnapshotSelection,
                SnapshotInfo.BackupSnapshotAfterTakingSnapshot, VmStorageSnapshotKvm};
    }

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
                s_logger.debug("[ignored] interrupted while retry cmd.");
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
    public Snapshot revertSnapshot(Long snapshotId) {
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
        if (snapshot == null) {
            throw new InvalidParameterValueException("No such snapshot");
        }

        if (Type.GROUP.name().equals(snapshot.getTypeDescription())) {
            throw new InvalidParameterValueException(String.format("The snapshot [%s] is part of a [%s] snapshots and cannot be reverted separately", snapshotId, snapshot.getTypeDescription()));
        }
        VolumeVO volume = _volsDao.findById(snapshot.getVolumeId());
        if (volume.getState() != Volume.State.Ready) {
            throw new InvalidParameterValueException("The volume is not in Ready state.");
        }

        Long instanceId = volume.getInstanceId();

        // If this volume is attached to an VM, then the VM needs to be in the stopped state
        // in order to revert the volume
        if (instanceId != null) {
            UserVmVO vm = _vmDao.findById(instanceId);
            if (vm.getState() != State.Stopped && vm.getState() != State.Shutdown) {
                throw new InvalidParameterValueException("The VM the specified disk is attached to is not in the shutdown state.");
            }
            // If target VM has associated VM snapshots then don't allow to revert from snapshot
            List<VMSnapshotVO> vmSnapshots = _vmSnapshotDao.findByVm(instanceId);
            if (vmSnapshots.size() > 0 && !Type.GROUP.name().equals(snapshot.getTypeDescription())) {
                throw new InvalidParameterValueException("Unable to revert snapshot for VM, please remove VM snapshots before reverting VM from snapshot");
            }
        }

        DataStoreRole dataStoreRole = snapshotHelper.getDataStoreRole(snapshot);

        SnapshotInfo snapshotInfo = snapshotFactory.getSnapshotWithRoleAndZone(snapshotId, dataStoreRole, volume.getDataCenterId());

        if (snapshotInfo == null) {
            throw new CloudRuntimeException(String.format("snapshot %s [%s] does not exists in data store", snapshot.getName(), snapshot.getUuid()));
        }

        SnapshotStrategy snapshotStrategy = _storageStrategyFactory.getSnapshotStrategy(snapshot, SnapshotOperation.REVERT);

        if (snapshotStrategy == null) {
            s_logger.error("Unable to find snapshot strategy to handle snapshot with id '" + snapshotId + "'");
            String errorMsg = String.format("Revert snapshot command failed for snapshot with id %d, because this command is supported only for KVM hypervisor", snapshotId);
            throw new CloudRuntimeException(errorMsg);
        }

        boolean result = snapshotStrategy.revertSnapshot(snapshotInfo);
        if (result) {
            // update volume size and primary storage count
            _resourceLimitMgr.decrementResourceCount(snapshot.getAccountId(), ResourceType.primary_storage, new Long(volume.getSize() - snapshot.getSize()));
            volume.setSize(snapshot.getSize());
            _volsDao.update(volume.getId(), volume);
            return snapshotInfo;
        }
        return null;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SNAPSHOT_POLICY_UPDATE, eventDescription = "updating snapshot policy", async = true)
    public SnapshotPolicy updateSnapshotPolicy(UpdateSnapshotPolicyCmd cmd) {

        Long id = cmd.getId();
        String customUUID = cmd.getCustomId();
        Boolean display = cmd.getDisplay();

        SnapshotPolicyVO policyVO = _snapshotPolicyDao.findById(id);
        VolumeInfo volume = volFactory.getVolume(policyVO.getVolumeId());
        if (volume == null) {
            throw new InvalidParameterValueException("No such volume exist");
        }

        // does the caller have the authority to act on this volume
        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, volume);

        if (display != null) {
            boolean previousDisplay = policyVO.isDisplay();
            policyVO.setDisplay(display);
            _snapSchedMgr.scheduleOrCancelNextSnapshotJobOnDisplayChange(policyVO, previousDisplay);
        }

        if (customUUID != null)
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

        SnapshotInfo snapshot = snapshotFactory.getSnapshotOnPrimaryStore(snapshotId);
        if (snapshot == null) {
            s_logger.debug("Failed to create snapshot");
            throw new CloudRuntimeException("Failed to create snapshot");
        }
        try {
            postCreateSnapshot(volumeId, snapshot.getId(), policyId);
            //Check if the snapshot was removed while backingUp. If yes, do not log snapshot create usage event
            SnapshotVO freshSnapshot = _snapshotDao.findById(snapshot.getId());
            if (freshSnapshot != null) {
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_SNAPSHOT_CREATE, snapshot.getAccountId(), snapshot.getDataCenterId(), snapshotId, snapshot.getName(), null, null,
                        volume.getSize(), snapshot.getClass().getName(), snapshot.getUuid());
            }
            _resourceLimitMgr.incrementResourceCount(snapshotOwner.getId(), ResourceType.snapshot);

        } catch (Exception e) {
            s_logger.debug("Failed to create snapshot", e);
            throw new CloudRuntimeException("Failed to create snapshot", e);
        }

        return snapshot;
    }

    @Override
    public Snapshot archiveSnapshot(Long snapshotId) {
        SnapshotInfo snapshotOnPrimary = snapshotFactory.getSnapshotOnPrimaryStore(snapshotId);

        if (snapshotOnPrimary == null || !snapshotOnPrimary.getStatus().equals(ObjectInDataStoreStateMachine.State.Ready)) {
            throw new CloudRuntimeException("Can only archive snapshots present on primary storage. " + "Cannot find snapshot " + snapshotId + " on primary storage");
        }

        SnapshotInfo snapshotOnSecondary = snapshotSrv.backupSnapshot(snapshotOnPrimary);
        SnapshotVO snapshotVO = _snapshotDao.findById(snapshotOnSecondary.getId());
        snapshotVO.setLocationType(Snapshot.LocationType.SECONDARY);
        _snapshotDao.persist(snapshotVO);

        try {
            snapshotSrv.deleteSnapshot(snapshotOnPrimary);
        } catch (Exception e) {
            throw new CloudRuntimeException("Snapshot archived to Secondary Storage but there was an error deleting "
                    + " the snapshot on Primary Storage. Please manually delete the primary snapshot " + snapshotId, e);
        }

        return snapshotOnSecondary;
    }

    @Override
    public Snapshot backupSnapshotFromVmSnapshot(Long snapshotId, Long vmId, Long volumeId, Long vmSnapshotId) {
        VMInstanceVO vm = _vmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Creating snapshot failed due to vm:" + vmId + " doesn't exist");
        }
        if (!HypervisorType.KVM.equals(vm.getHypervisorType())) {
            throw new InvalidParameterValueException("Unsupported hypervisor type " + vm.getHypervisorType() + ". This supports KVM only");
        }

        VMSnapshotVO vmSnapshot = _vmSnapshotDao.findById(vmSnapshotId);
        if (vmSnapshot == null) {
            throw new InvalidParameterValueException("Creating snapshot failed due to vmSnapshot:" + vmSnapshotId + " doesn't exist");
        }
        // check vmsnapshot permissions
        Account caller = CallContext.current().getCallingAccount();
        _accountMgr.checkAccess(caller, null, true, vmSnapshot);

        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
        if (snapshot == null) {
            throw new InvalidParameterValueException("Creating snapshot failed due to snapshot:" + snapshotId + " doesn't exist");
        }

        VolumeInfo volume = volFactory.getVolume(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Creating snapshot failed due to volume:" + volumeId + " doesn't exist");
        }

        if (volume.getState() != Volume.State.Ready) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " is not in " + Volume.State.Ready + " state but " + volume.getState() + ". Cannot take snapshot.");
        }

        DataStore store = volume.getDataStore();
        SnapshotDataStoreVO parentSnapshotDataStoreVO = _snapshotStoreDao.findParent(store.getRole(), store.getId(), volumeId);
        if (parentSnapshotDataStoreVO != null) {
            //Double check the snapshot is removed or not
            SnapshotVO parentSnap = _snapshotDao.findById(parentSnapshotDataStoreVO.getSnapshotId());
            if (parentSnap != null && parentSnapshotDataStoreVO.getInstallPath() != null && parentSnapshotDataStoreVO.getInstallPath().equals(vmSnapshot.getName())) {
                throw new InvalidParameterValueException("Creating snapshot failed due to snapshot : " + parentSnap.getUuid() + " is created from the same vm snapshot");
            }
        }
        SnapshotInfo snapshotInfo = this.snapshotFactory.getSnapshot(snapshotId, store);
        snapshotInfo = (SnapshotInfo)store.create(snapshotInfo);
        SnapshotDataStoreVO snapshotOnPrimaryStore = this._snapshotStoreDao.findByStoreSnapshot(store.getRole(), store.getId(), snapshot.getId());

        StoragePoolVO storagePool = _storagePoolDao.findById(store.getId());
        updateSnapshotInfo(volumeId, vmSnapshotId, vmSnapshot, snapshot, snapshotOnPrimaryStore, storagePool);

        snapshot.setState(Snapshot.State.CreatedOnPrimary);
        _snapshotDao.update(snapshot.getId(), snapshot);
        snapshotInfo = this.snapshotFactory.getSnapshot(snapshotId, store);

        Long snapshotOwnerId = vm.getAccountId();

        try {
            SnapshotStrategy snapshotStrategy = _storageStrategyFactory.getSnapshotStrategy(snapshot, SnapshotOperation.BACKUP);
            if (snapshotStrategy == null) {
                throw new CloudRuntimeException("Unable to find snapshot strategy to handle snapshot with id '" + snapshotId + "'");
            }
            snapshotInfo = snapshotStrategy.backupSnapshot(snapshotInfo);

        } catch (Exception e) {
            s_logger.debug("Failed to backup snapshot from vm snapshot", e);
            _resourceLimitMgr.decrementResourceCount(snapshotOwnerId, ResourceType.snapshot);
            _resourceLimitMgr.decrementResourceCount(snapshotOwnerId, ResourceType.secondary_storage, new Long(volume.getSize()));
            throw new CloudRuntimeException("Failed to backup snapshot from vm snapshot", e);
        } finally {
            if (snapshotOnPrimaryStore != null) {
                _snapshotStoreDao.remove(snapshotOnPrimaryStore.getId());
            }
        }
        return snapshotInfo;
    }

    private void updateSnapshotInfo(Long volumeId, Long vmSnapshotId, VMSnapshotVO vmSnapshot, SnapshotVO snapshot,
            SnapshotDataStoreVO snapshotOnPrimaryStore, StoragePoolVO storagePool) {
        if ((storagePool.getPoolType() == StoragePoolType.NetworkFilesystem || storagePool.getPoolType() == StoragePoolType.Filesystem) && vmSnapshot.getType() == VMSnapshot.Type.Disk) {
            List<VMSnapshotDetailsVO> vmSnapshotDetails = vmSnapshotDetailsDao.findDetails(vmSnapshotId, "kvmStorageSnapshot");
            for (VMSnapshotDetailsVO vmSnapshotDetailsVO : vmSnapshotDetails) {
                SnapshotInfo sInfo = snapshotDataFactory.getSnapshot(Long.parseLong(vmSnapshotDetailsVO.getValue()), DataStoreRole.Primary);
                if (sInfo.getVolumeId() == volumeId) {
                    snapshotOnPrimaryStore.setState(ObjectInDataStoreStateMachine.State.Ready);
                    snapshotOnPrimaryStore.setInstallPath(sInfo.getPath());
                    _snapshotStoreDao.update(snapshotOnPrimaryStore.getId(), snapshotOnPrimaryStore);
                    snapshot.setTypeDescription(Type.FROM_GROUP.name());
                    snapshot.setSnapshotType((short)Type.FROM_GROUP.ordinal());
                }
            }
        } else {
            snapshotOnPrimaryStore.setState(ObjectInDataStoreStateMachine.State.Ready);
            snapshotOnPrimaryStore.setInstallPath(vmSnapshot.getName());
            _snapshotStoreDao.update(snapshotOnPrimaryStore.getId(), snapshotOnPrimaryStore);
        }
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
            if (snapshotSchedule != null) {
                snapshotSchedule.setSnapshotId(snapshotId);
                _snapshotScheduleDao.update(snapshotSchedule.getId(), snapshotSchedule);
            }
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

        List<SnapshotVO> snaps = listSnapsforVolumeTypeNotDestroyed(volumeId, type);
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
            if (deleteSnapshot(oldSnapId, null)) {
                //log Snapshot delete event
                ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, oldestSnapshot.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_SNAPSHOT_DELETE,
                        "Successfully deleted oldest snapshot: " + oldSnapId, oldSnapId, ApiCommandResourceType.Snapshot.toString(), 0);
            }
            snaps.remove(oldestSnapshot);
        }
    }

    protected Pair<List<SnapshotDataStoreVO>, List<Long>> getStoreRefsAndZonesForSnapshotDelete(long snapshotId, Long zoneId) {
        List<SnapshotDataStoreVO> snapshotStoreRefs = new ArrayList<>();
        List<SnapshotDataStoreVO> allSnapshotStoreRefs = _snapshotStoreDao.findBySnapshotId(snapshotId);
        List<Long> zoneIds = new ArrayList<>();
        if (zoneId != null) {
            DataCenterVO zone = dataCenterDao.findById(zoneId);
            if (zone == null) {
                throw new InvalidParameterValueException("Unable to find a zone with the specified id");
            }
            for (SnapshotDataStoreVO snapshotStore : allSnapshotStoreRefs) {
                Long entryZoneId = dataStoreMgr.getStoreZoneId(snapshotStore.getDataStoreId(), snapshotStore.getRole());
                if (zoneId.equals(entryZoneId)) {
                    snapshotStoreRefs.add(snapshotStore);
                }
            }
            zoneIds.add(zoneId);
        } else {
            snapshotStoreRefs = allSnapshotStoreRefs;
            for (SnapshotDataStoreVO snapshotStore : snapshotStoreRefs) {
                Long entryZoneId = dataStoreMgr.getStoreZoneId(snapshotStore.getDataStoreId(), snapshotStore.getRole());
                if (!zoneIds.contains(entryZoneId)) {
                    zoneIds.add(entryZoneId);
                }
            }
        }
        return new Pair<>(snapshotStoreRefs, zoneIds);
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_SNAPSHOT_DELETE, eventDescription = "deleting snapshot", async = true)
    public boolean deleteSnapshot(long snapshotId, Long zoneId) {
        Account caller = CallContext.current().getCallingAccount();

        // Verify parameters
        final SnapshotVO snapshotCheck = _snapshotDao.findById(snapshotId);

        if (snapshotCheck == null) {
            throw new InvalidParameterValueException("unable to find a snapshot with id " + snapshotId);
        }

        if (Type.GROUP.name().equals(snapshotCheck.getTypeDescription())) {
            throw new InvalidParameterValueException(String.format("The snapshot [%s] is part of a [%s] snapshots and cannot be deleted separately", snapshotId, snapshotCheck.getTypeDescription()));
        }

        if (snapshotCheck.getState() == Snapshot.State.Destroyed) {
            throw new InvalidParameterValueException("Snapshot with id: " + snapshotId + " is already destroyed");
        }

        _accountMgr.checkAccess(caller, null, true, snapshotCheck);

        SnapshotStrategy snapshotStrategy = _storageStrategyFactory.getSnapshotStrategy(snapshotCheck, zoneId, SnapshotOperation.DELETE);

        if (snapshotStrategy == null) {
            s_logger.error("Unable to find snapshot strategy to handle snapshot with id '" + snapshotId + "'");

            return false;
        }
        Pair<List<SnapshotDataStoreVO>, List<Long>> storeRefAndZones = getStoreRefsAndZonesForSnapshotDelete(snapshotId, zoneId);
        List<SnapshotDataStoreVO> snapshotStoreRefs = storeRefAndZones.first();
        List<Long> zoneIds = storeRefAndZones.second();

        try {
            boolean result = snapshotStrategy.deleteSnapshot(snapshotId, zoneId);
            if (result) {
                for (Long zId : zoneIds) {
                    if (snapshotCheck.getState() == Snapshot.State.BackedUp) {
                        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_SNAPSHOT_DELETE, snapshotCheck.getAccountId(), zId, snapshotId,
                                snapshotCheck.getName(), null, null, 0L, snapshotCheck.getClass().getName(), snapshotCheck.getUuid());
                    }
                }
                final SnapshotVO postDeleteSnapshotEntry = _snapshotDao.findById(snapshotId);
                if (postDeleteSnapshotEntry == null || Snapshot.State.Destroyed.equals(postDeleteSnapshotEntry.getState())) {
                    annotationDao.removeByEntityType(AnnotationService.EntityType.SNAPSHOT.name(), snapshotCheck.getUuid());

                    if (snapshotCheck.getState() != Snapshot.State.Error && snapshotCheck.getState() != Snapshot.State.Destroyed) {
                        _resourceLimitMgr.decrementResourceCount(snapshotCheck.getAccountId(), ResourceType.snapshot);
                    }
                }
                for (SnapshotDataStoreVO snapshotStoreRef : snapshotStoreRefs) {
                    if (ObjectInDataStoreStateMachine.State.Ready.equals(snapshotStoreRef.getState()) && !DataStoreRole.Primary.equals(snapshotStoreRef.getRole())) {
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

        List<Long> ids = getIdsListFromCmd(cmd.getId(), cmd.getIds());

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(),
                cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter searchFilter = new Filter(SnapshotVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<SnapshotVO> sb = _snapshotDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("statusNEQ", sb.entity().getState(), SearchCriteria.Op.NEQ); //exclude those Destroyed snapshot, not showing on UI
        sb.and("volumeId", sb.entity().getVolumeId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("idIN", sb.entity().getId(), SearchCriteria.Op.IN);
        sb.and("snapshotTypeEQ", sb.entity().getSnapshotType(), SearchCriteria.Op.IN);
        sb.and("snapshotTypeNEQ", sb.entity().getSnapshotType(), SearchCriteria.Op.NIN);
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

        setIdsListToSearchCriteria(sc, ids);

        if (name != null) {
            sc.setParameters("name", name);
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
            sc.setParameters("snapshotTypeNEQ", Snapshot.Type.TEMPLATE.ordinal(), Snapshot.Type.GROUP.ordinal());
        }

        Pair<List<SnapshotVO>, Integer> result = _snapshotDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Snapshot>, Integer>(result.first(), result.second());
    }

    @Override
    public boolean deleteSnapshotDirsForAccount(long accountId) {

        List<VolumeVO> volumes = _volsDao.findIncludingRemovedByAccount(accountId);
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
                    s_logger.error("Unable to find snapshot strategy to handle snapshot with id '" + snapshot.getId() + "'");
                    continue;
                }
                List<SnapshotDataStoreVO> snapshotStoreRefs = _snapshotStoreDao.listReadyBySnapshot(snapshot.getId(), DataStoreRole.Image);

                if (snapshotStrategy.deleteSnapshot(snapshot.getId(), null)) {
                    if (Type.MANUAL == snapshot.getRecurringType()) {
                        _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.snapshot);
                        for (SnapshotDataStoreVO snapshotStoreRef : snapshotStoreRefs) {
                            _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.secondary_storage, new Long(snapshotStoreRef.getPhysicalSize()));
                        }
                    }

                    // Log event after successful deletion
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_SNAPSHOT_DELETE, snapshot.getAccountId(), volume.getDataCenterId(), snapshot.getId(), snapshot.getName(),
                            null, null, volume.getSize(), snapshot.getClass().getName(), snapshot.getUuid());
                }
            }
        }

        // Returns true if snapshotsDir has been deleted for all volumes.
        return success;
    }

    protected void validatePolicyZones(List<Long> zoneIds, VolumeVO volume, Account caller) {
        if (CollectionUtils.isEmpty(zoneIds)) {
            return;
        }
        if (Boolean.FALSE.equals(SnapshotInfo.BackupSnapshotAfterTakingSnapshot.value())) {
            throw new InvalidParameterValueException("Backing up of snapshot has been disabled. Snapshot can not be taken for multiple zones");
        }
        final DataCenterVO zone = dataCenterDao.findById(volume.getDataCenterId());
        if (DataCenter.Type.Edge.equals(zone.getType())) {
            throw new InvalidParameterValueException("Backing up of snapshot is not supported by the zone of the volume. Snapshots can not be taken for multiple zones");
        }
        boolean isRootAdminCaller = _accountMgr.isRootAdmin(caller.getId());
        for (Long zoneId : zoneIds) {
            getCheckedDestinationZoneForSnapshotCopy(zoneId, isRootAdminCaller);
        }
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

        // For now, volumes with encryption don't support snapshot schedules, because they will fail when VM is running
        DiskOfferingVO diskOffering = diskOfferingDao.findByIdIncludingRemoved(volume.getDiskOfferingId());
        if (diskOffering == null) {
            throw new InvalidParameterValueException(String.format("Failed to find disk offering for the volume [%s]", volume.getUuid()));
        } else if(diskOffering.getEncrypt()) {
            throw new UnsupportedOperationException(String.format("Encrypted volumes don't support snapshot schedules, cannot create snapshot policy for the volume [%s]", volume.getUuid()));
        }

        String volumeDescription = volume.getVolumeDescription();

        final Account caller = CallContext.current().getCallingAccount();
        _accountMgr.checkAccess(caller, null, true, volume);

        // If display is false we don't actually schedule snapshots.
        if (volume.getState() != Volume.State.Ready && display) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " is not in " + Volume.State.Ready + " state but " + volume.getState() + ". Cannot take snapshot.");
        }

        if (volume.getTemplateId() != null) {
            VMTemplateVO template = _templateDao.findById(volume.getTemplateId());
            Long instanceId = volume.getInstanceId();
            UserVmVO userVmVO = null;
            if (instanceId != null) {
                userVmVO = _vmDao.findById(instanceId);
            }
            if (template != null && template.getTemplateType() == Storage.TemplateType.SYSTEM && (userVmVO == null || !UserVmManager.CKS_NODE.equals(userVmVO.getUserVmType()))) {
                throw new InvalidParameterValueException("VolumeId: " + volumeId + " is for System VM , Creating snapshot against System VM volumes is not supported");
            }
        }

        AccountVO owner = _accountDao.findById(volume.getAccountId());
        Long instanceId = volume.getInstanceId();
        String intervalType = cmd.getIntervalType();
        if (instanceId != null) {
            // It is not detached, but attached to a VM
            if (_vmDao.findById(instanceId) == null) {
                // It is not a UserVM but a SystemVM or DomR
                throw new InvalidParameterValueException(String.format("Failed to create snapshot policy [%s] for volume %s; Snapshots of volumes attached to System or router VM are not allowed.", intervalType, volumeDescription));
            }
        }

        IntervalType intvType = DateUtil.IntervalType.getIntervalType(intervalType);
        if (intvType == null) {
            throw new InvalidParameterValueException("Unsupported interval type " + intervalType);
        }
        Type type = getSnapshotType(intvType);
        String cmdTimezone = cmd.getTimezone();

        TimeZone timeZone = TimeZone.getTimeZone(cmdTimezone);
        String timezoneId = timeZone.getID();
        if (!timezoneId.equals(cmdTimezone)) {
            s_logger.warn(String.format("Using timezone [%s] for running the snapshot policy [%s] for volume %s, as an equivalent of [%s].", timezoneId, intervalType, volumeDescription,
              cmdTimezone));
        }

        String schedule = cmd.getSchedule();

        try {
            DateUtil.getNextRunTime(intvType, schedule, timezoneId, null);
        } catch (Exception e) {
            throw new InvalidParameterValueException(String.format("%s has an invalid schedule [%s] for interval type [%s].",
              volumeDescription, schedule, intervalType));
        }

        int maxSnaps = cmd.getMaxSnaps();

        if (maxSnaps <= 0) {
            throw new InvalidParameterValueException(String.format("maxSnaps [%s] for volume %s should be greater than 0.", maxSnaps, volumeDescription));
        }

        int intervalMaxSnaps = type.getMax();
        if (maxSnaps > intervalMaxSnaps) {
            throw new InvalidParameterValueException(String.format("maxSnaps [%s] for volume %s exceeds limit [%s] for interval type [%s].", maxSnaps, volumeDescription,
              intervalMaxSnaps, intervalType));
        }

        // Verify that max doesn't exceed domain and account snapshot limits in case display is on
        if (display) {
            long accountLimit = _resourceLimitMgr.findCorrectResourceLimitForAccount(owner, ResourceType.snapshot);
            long domainLimit = _resourceLimitMgr.findCorrectResourceLimitForDomain(_domainMgr.getDomain(owner.getDomainId()), ResourceType.snapshot);
            if (!_accountMgr.isRootAdmin(owner.getId()) && ((accountLimit != -1 && maxSnaps > accountLimit) || (domainLimit != -1 && maxSnaps > domainLimit))) {
                String message = "domain/account";
                if (owner.getType() == Account.Type.PROJECT) {
                    message = "domain/project";
                }

                throw new InvalidParameterValueException("Max number of snapshots shouldn't exceed the " + message + " level snapshot limit");
            }
        }

        final List<Long> zoneIds = cmd.getZoneIds();
        validatePolicyZones(zoneIds, volume, caller);

        Map<String, String> tags = cmd.getTags();
        boolean active = true;

        return persistSnapshotPolicy(volume, schedule, timezoneId, intvType, maxSnaps, display, active, tags, zoneIds);
    }

    protected SnapshotPolicyVO persistSnapshotPolicy(VolumeVO volume, String schedule, String timezone, IntervalType intervalType, int maxSnaps, boolean display, boolean active, Map<String, String> tags, List<Long> zoneIds) {
        long volumeId = volume.getId();
        String volumeDescription = volume.getVolumeDescription();

        GlobalLock createSnapshotPolicyLock = GlobalLock.getInternLock("createSnapshotPolicy_" + volumeId);
        boolean isLockAcquired = createSnapshotPolicyLock.lock(5);

        if (!isLockAcquired) {
            throw new CloudRuntimeException(String.format("Unable to acquire lock for creating snapshot policy [%s] for %s.", intervalType, volumeDescription));
        }

        s_logger.debug(String.format("Acquired lock for creating snapshot policy [%s] for volume %s.", intervalType, volumeDescription));

        try {
            SnapshotPolicyVO policy = _snapshotPolicyDao.findOneByVolumeInterval(volumeId, intervalType);

            if (policy == null) {
                policy = createSnapshotPolicy(volumeId, schedule, timezone, intervalType, maxSnaps, display, zoneIds);
            } else {
                updateSnapshotPolicy(policy, schedule, timezone, intervalType, maxSnaps, active, display, zoneIds);
            }

            createTagsForSnapshotPolicy(tags, policy);
            CallContext.current().putContextParameter(SnapshotPolicy.class, policy.getUuid());

            return policy;
        } finally {
            createSnapshotPolicyLock.unlock();
        }
    }

    protected SnapshotPolicyVO createSnapshotPolicy(long volumeId, String schedule, String timezone, IntervalType intervalType, int maxSnaps, boolean display, List<Long> zoneIds) {
        SnapshotPolicyVO policy = new SnapshotPolicyVO(volumeId, schedule, timezone, intervalType, maxSnaps, display);
        policy = _snapshotPolicyDao.persist(policy);
        if (CollectionUtils.isNotEmpty(zoneIds)) {
            List<SnapshotPolicyDetailVO> details = new ArrayList<>();
            for (Long zoneId : zoneIds) {
                details.add(new SnapshotPolicyDetailVO(policy.getId(), ApiConstants.ZONE_ID, String.valueOf(zoneId)));
            }
            snapshotPolicyDetailsDao.saveDetails(details);
        }
        _snapSchedMgr.scheduleNextSnapshotJob(policy);
        s_logger.debug(String.format("Created snapshot policy %s.", new ReflectionToStringBuilder(policy, ToStringStyle.JSON_STYLE).setExcludeFieldNames("id", "uuid", "active")));
        return policy;
    }

    protected void updateSnapshotPolicy(SnapshotPolicyVO policy, String schedule, String timezone, IntervalType intervalType, int maxSnaps, boolean active, boolean display, List<Long> zoneIds) {
        String previousPolicy = new ReflectionToStringBuilder(policy, ToStringStyle.JSON_STYLE).setExcludeFieldNames("id", "uuid").toString();
        boolean previousDisplay = policy.isDisplay();
        policy.setSchedule(schedule);
        policy.setTimezone(timezone);
        policy.setInterval((short) intervalType.ordinal());
        policy.setMaxSnaps(maxSnaps);
        policy.setActive(active);
        policy.setDisplay(display);
        _snapshotPolicyDao.update(policy.getId(), policy);
        if (CollectionUtils.isNotEmpty(zoneIds)) {
            List<SnapshotPolicyDetailVO> details = snapshotPolicyDetailsDao.listDetails(policy.getId());
            details = details.stream().filter(d -> !ApiConstants.ZONE_ID.equals(d.getName())).collect(Collectors.toList());
            for (Long zoneId : zoneIds) {
                details.add(new SnapshotPolicyDetailVO(policy.getId(), ApiConstants.ZONE_ID, String.valueOf(zoneId)));
            }
            snapshotPolicyDetailsDao.saveDetails(details);
        }

        _snapSchedMgr.scheduleOrCancelNextSnapshotJobOnDisplayChange(policy, previousDisplay);
        taggedResourceService.deleteTags(Collections.singletonList(policy.getUuid()), ResourceObjectType.SnapshotPolicy, null);
        s_logger.debug(String.format("Updated snapshot policy %s to %s.", previousPolicy, new ReflectionToStringBuilder(policy, ToStringStyle.JSON_STYLE)
          .setExcludeFieldNames("id", "uuid")));
    }

    protected void createTagsForSnapshotPolicy(Map<String, String> tags, SnapshotPolicyVO policy) {
        if (MapUtils.isNotEmpty(tags)) {
            taggedResourceService.createTags(Collections.singletonList(policy.getUuid()), ResourceObjectType.SnapshotPolicy, tags, null);
        }
    }

    @Override
    public void copySnapshotPoliciesBetweenVolumes(VolumeVO srcVolume, VolumeVO destVolume){
        IntervalType[] intervalTypes = IntervalType.values();
        List<SnapshotPolicyVO> policies = listPoliciesforVolume(srcVolume.getId());

        s_logger.debug(String.format("Copying snapshot policies %s from volume %s to volume %s.", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(policies,
          "id", "uuid"), srcVolume.getVolumeDescription(), destVolume.getVolumeDescription()));

        for (SnapshotPolicyVO policy : policies) {
            List<SnapshotPolicyDetailVO> details = snapshotPolicyDetailsDao.findDetails(policy.getId(), ApiConstants.ZONE_ID);
            List<Long> zoneIds = details.stream().map(d -> Long.valueOf(d.getValue())).collect(Collectors.toList());
            persistSnapshotPolicy(destVolume, policy.getSchedule(), policy.getTimezone(), intervalTypes[policy.getInterval()], policy.getMaxSnaps(),
                    policy.isDisplay(), policy.isActive(), taggedResourceService.getTagsFromResource(ResourceObjectType.SnapshotPolicy, policy.getId()), zoneIds);
        }
    }

    protected boolean deletePolicy(Long policyId) {
        SnapshotPolicyVO snapshotPolicy = _snapshotPolicyDao.findById(policyId);
        _snapSchedMgr.removeSchedule(snapshotPolicy.getVolumeId(), snapshotPolicy.getId());
        taggedResourceService.deleteTags(Collections.singletonList(snapshotPolicy.getUuid()), ResourceObjectType.SnapshotPolicy, null);
        return _snapshotPolicyDao.remove(policyId);
    }

    @Override
    public Pair<List<? extends SnapshotPolicy>, Integer> listPoliciesforVolume(ListSnapshotPoliciesCmd cmd) {
        Long volumeId = cmd.getVolumeId();
        boolean display = cmd.isDisplay();
        Long id = cmd.getId();
        Pair<List<SnapshotPolicyVO>, Integer> result = null;
        // TODO - Have a better way of doing this.
        if (id != null) {
            result = _snapshotPolicyDao.listAndCountById(id, display, null);
            if (result != null && result.first() != null && !result.first().isEmpty()) {
                SnapshotPolicyVO snapshotPolicy = result.first().get(0);
                volumeId = snapshotPolicy.getVolumeId();
            }
        }
        VolumeVO volume = _volsDao.findById(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Unable to find a volume with id " + volumeId);
        }
        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, volume);
        if (result != null)
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

    private List<SnapshotVO> listSnapsforVolumeTypeNotDestroyed(long volumeId, Type type) {
        return _snapshotDao.listByVolumeIdTypeNotDestroyed(volumeId, type);
    }

    @Override
    public void deletePoliciesForVolume(Long volumeId) {
        List<SnapshotPolicyVO> policyInstances = listPoliciesforVolume(volumeId);
        for (SnapshotPolicyVO policyInstance : policyInstances) {
            deletePolicy(policyInstance.getId());
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

    private boolean hostSupportsSnapsthotForVolume(HostVO host, VolumeInfo volume, boolean isFromVmSnapshot) {
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
                if (!snapshotEnabled && !isFromVmSnapshot) {
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

    private boolean supportedByHypervisor(VolumeInfo volume, boolean isFromVmSnapshot) {
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
                if (!hostSupportsSnapsthotForVolume(host, volume, isFromVmSnapshot)) {
                    throw new CloudRuntimeException(
                            "KVM Snapshot is not supported for Running VMs. It is disabled by default due to a possible volume corruption in certain cases. To enable it set global settings kvm.snapshot.enabled to True. See the documentation for more details.");
                }
            }
        }

        // if volume is attached to a vm in destroyed or expunging state; disallow
        if (volume.getInstanceId() != null) {
            UserVmVO userVm = _vmDao.findById(volume.getInstanceId());
            if (userVm != null) {
                if (userVm.getState().equals(State.Destroyed) || userVm.getState().equals(State.Expunging)) {
                    throw new CloudRuntimeException("Creating snapshot failed due to volume:" + volume.getId() + " is associated with vm:" + userVm.getInstanceName() + " is in "
                            + userVm.getState().toString() + " state");
                }

                if (userVm.getHypervisorType() == HypervisorType.VMware || userVm.getHypervisorType() == HypervisorType.KVM) {
                    List<SnapshotVO> activeSnapshots = _snapshotDao.listByInstanceId(volume.getInstanceId(), Snapshot.State.Creating, Snapshot.State.CreatedOnPrimary,
                            Snapshot.State.BackingUp);
                    if (activeSnapshots.size() > 0) {
                        throw new InvalidParameterValueException("There is other active snapshot tasks on the instance to which the volume is attached, please try again later");
                    }
                }

                List<VMSnapshotVO> activeVMSnapshots = _vmSnapshotDao.listByInstanceId(userVm.getId(), VMSnapshot.State.Creating, VMSnapshot.State.Reverting,
                        VMSnapshot.State.Expunging);
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

        updateSnapshotPayload(volume.getPoolId(), payload);

        Long snapshotId = payload.getSnapshotId();
        Account snapshotOwner = payload.getAccount();
        SnapshotInfo snapshot = snapshotFactory.getSnapshot(snapshotId, volume.getDataStore());
        snapshot.addPayload(payload);
        try {
            SnapshotStrategy snapshotStrategy = _storageStrategyFactory.getSnapshotStrategy(snapshot, SnapshotOperation.TAKE);

            if (snapshotStrategy == null) {
                throw new CloudRuntimeException("Can't find snapshot strategy to deal with snapshot:" + snapshotId);
            }

            SnapshotInfo snapshotOnPrimary = snapshotStrategy.takeSnapshot(snapshot);
            boolean backupSnapToSecondary = isBackupSnapshotToSecondaryForZone(snapshot.getDataCenterId());

            if (backupSnapToSecondary) {
                backupSnapshotToSecondary(payload.getAsyncBackup(), snapshotStrategy, snapshotOnPrimary, payload.getZoneIds());
            } else {
                s_logger.debug("skipping backup of snapshot [uuid=" + snapshot.getUuid() + "] to secondary due to configuration");
                snapshotOnPrimary.markBackedUp();
            }

            try {
                postCreateSnapshot(volume.getId(), snapshotId, payload.getSnapshotPolicyId());
                snapshotZoneDao.addSnapshotToZone(snapshotId, snapshot.getDataCenterId());

                DataStoreRole dataStoreRole = backupSnapToSecondary ? snapshotHelper.getDataStoreRole(snapshot) : DataStoreRole.Primary;

                List<SnapshotDataStoreVO> snapshotStoreRefs = _snapshotStoreDao.listReadyBySnapshot(snapshotId, dataStoreRole);
                if (CollectionUtils.isEmpty(snapshotStoreRefs)) {
                    throw new CloudRuntimeException(String.format("Could not find snapshot %s [%s] on [%s]", snapshot.getName(), snapshot.getUuid(), snapshot.getLocationType()));
                }
                SnapshotDataStoreVO snapshotStoreRef = snapshotStoreRefs.get(0);
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_SNAPSHOT_CREATE, snapshot.getAccountId(), snapshot.getDataCenterId(), snapshotId, snapshot.getName(), null, null,
                        snapshotStoreRef.getPhysicalSize(), volume.getSize(), snapshot.getClass().getName(), snapshot.getUuid());

                // Correct the resource count of snapshot in case of delta snapshots.
                _resourceLimitMgr.decrementResourceCount(snapshotOwner.getId(), ResourceType.secondary_storage, new Long(volume.getSize() - snapshotStoreRef.getPhysicalSize()));

                if (!payload.getAsyncBackup() && backupSnapToSecondary) {
                    copyNewSnapshotToZones(snapshotId, snapshot.getDataCenterId(), payload.getZoneIds());
                }
            } catch (Exception e) {
                s_logger.debug("post process snapshot failed", e);
            }
        } catch (CloudRuntimeException cre) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Failed to create snapshot" + cre.getLocalizedMessage());
            }
            _resourceLimitMgr.decrementResourceCount(snapshotOwner.getId(), ResourceType.snapshot);
            _resourceLimitMgr.decrementResourceCount(snapshotOwner.getId(), ResourceType.secondary_storage, new Long(volume.getSize()));
            throw cre;
        } catch (Exception e) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Failed to create snapshot", e);
            }
            _resourceLimitMgr.decrementResourceCount(snapshotOwner.getId(), ResourceType.snapshot);
            _resourceLimitMgr.decrementResourceCount(snapshotOwner.getId(), ResourceType.secondary_storage, new Long(volume.getSize()));
            throw new CloudRuntimeException("Failed to create snapshot", e);
        }
        return snapshot;
    }

    protected void backupSnapshotToSecondary(boolean asyncBackup, SnapshotStrategy snapshotStrategy, SnapshotInfo snapshotOnPrimary, List<Long> zoneIds) {
        if (asyncBackup) {
            backupSnapshotExecutor.schedule(new BackupSnapshotTask(snapshotOnPrimary, snapshotBackupRetries - 1, snapshotStrategy, zoneIds), 0, TimeUnit.SECONDS);
        } else {
            SnapshotInfo backupedSnapshot = snapshotStrategy.backupSnapshot(snapshotOnPrimary);
            if (backupedSnapshot != null) {
                snapshotStrategy.postSnapshotCreation(snapshotOnPrimary);
            }
        }
    }

    protected class BackupSnapshotTask extends ManagedContextRunnable {
        SnapshotInfo snapshot;
        int attempts;
        SnapshotStrategy snapshotStrategy;

        List<Long> zoneIds;

        public BackupSnapshotTask(SnapshotInfo snap, int maxRetries, SnapshotStrategy strategy, List<Long> zoneIds) {
            snapshot = snap;
            attempts = maxRetries;
            snapshotStrategy = strategy;
            this.zoneIds = zoneIds;
        }

        @Override
        protected void runInContext() {
            try {
                s_logger.debug("Value of attempts is " + (snapshotBackupRetries - attempts));

                SnapshotInfo backupedSnapshot = snapshotStrategy.backupSnapshot(snapshot);

                if (backupedSnapshot != null) {
                    snapshotStrategy.postSnapshotCreation(snapshot);
                    copyNewSnapshotToZones(snapshot.getId(), snapshot.getDataCenterId(), zoneIds);
                }
            } catch (final Exception e) {
                if (attempts >= 0) {
                    s_logger.debug("Backing up of snapshot failed, for snapshot with ID " + snapshot.getSnapshotId() + ", left with " + attempts + " more attempts");
                    backupSnapshotExecutor.schedule(new BackupSnapshotTask(snapshot, --attempts, snapshotStrategy, zoneIds), snapshotBackupRetryInterval, TimeUnit.SECONDS);
                } else {
                    s_logger.debug("Done with " + snapshotBackupRetries + " attempts in  backing up of snapshot with ID " + snapshot.getSnapshotId());
                    snapshotSrv.cleanupOnSnapshotBackupFailure(snapshot);
                }
            }
        }
    }

    private void updateSnapshotPayload(long storagePoolId, CreateSnapshotPayload payload) {
        StoragePoolVO storagePoolVO = _storagePoolDao.findById(storagePoolId);

        if (storagePoolVO.isManaged()) {
            Snapshot.LocationType locationType = payload.getLocationType();

            if (locationType == null) {
                payload.setLocationType(Snapshot.LocationType.PRIMARY);
            }
        } else {
            payload.setLocationType(null);
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        String value = _configDao.getValue(Config.BackupSnapshotWait.toString());

        Type.HOURLY.setMax(SnapshotHourlyMax.value());
        Type.DAILY.setMax(SnapshotDailyMax.value());
        Type.WEEKLY.setMax(SnapshotWeeklyMax.value());
        Type.MONTHLY.setMax(SnapshotMonthlyMax.value());
        _totalRetries = NumbersUtil.parseInt(_configDao.getValue("total.retries"), 4);
        _pauseInterval = 2 * NumbersUtil.parseInt(_configDao.getValue("ping.interval"), 60);

        snapshotBackupRetries = BackupRetryAttempts.value();
        snapshotBackupRetryInterval = BackupRetryInterval.value();
        backupSnapshotExecutor = Executors.newScheduledThreadPool(10, new NamedThreadFactory("BackupSnapshotTask"));
        s_logger.info("Snapshot Manager is configured.");

        return true;
    }

    @Override
    public boolean start() {
        //destroy snapshots in destroying state
        List<SnapshotVO> snapshots = _snapshotDao.listAllByStatus(Snapshot.State.Destroying);
        for (SnapshotVO snapshotVO : snapshots) {
            try {
                if (!deleteSnapshot(snapshotVO.getId(), null)) {
                    s_logger.debug("Failed to delete snapshot in destroying state with id " + snapshotVO.getUuid());
                }
            } catch (Exception e) {
                s_logger.debug("Failed to delete snapshot in destroying state with id " + snapshotVO.getUuid());
            }
        }
        return true;
    }

    @Override
    public boolean stop() {
        backupSnapshotExecutor.shutdown();
        return true;
    }

    @Override
    public boolean deleteSnapshotPolicies(DeleteSnapshotPoliciesCmd cmd) {
        Long policyId = cmd.getId();
        List<Long> policyIds = cmd.getIds();

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

        if (policyIds.contains(Snapshot.MANUAL_POLICY_ID)) {
            throw new InvalidParameterValueException("Invalid Policy id given: " + Snapshot.MANUAL_POLICY_ID);
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

        for (Long pId : policyIds) {
            if (!deletePolicy(pId)) {
                s_logger.warn("Failed to delete snapshot policy with Id: " + policyId);
                return false;
            }
        }

        return true;
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
    public boolean backedUpSnapshotsExistsForVolume(Volume volume) {
        List<SnapshotVO> snapshots = _snapshotDao.listByStatus(volume.getId(), Snapshot.State.BackedUp);
        if (snapshots.size() > 0) {
            return true;
        }
        return false;
    }

    @Override
    public void cleanupSnapshotsByVolume(Long volumeId) {
        List<SnapshotInfo> infos = snapshotFactory.getSnapshotsForVolumeAndStoreRole(volumeId, DataStoreRole.Primary);
        for (SnapshotInfo info : infos) {
            try {
                if (info != null) {
                    snapshotSrv.deleteSnapshot(info);
                }
            } catch (CloudRuntimeException e) {
                String msg = "Cleanup of Snapshot with uuid " + info.getUuid() + " in primary storage is failed. Ignoring";
                s_logger.warn(msg);
            }
        }
    }

    @Override
    public Snapshot allocSnapshot(Long volumeId, Long policyId, String snapshotName, Snapshot.LocationType locationType) throws ResourceAllocationException {
        return allocSnapshot(volumeId, policyId, snapshotName, locationType, false, null);
    }

    @Override
    public Snapshot allocSnapshot(Long volumeId, Long policyId, String snapshotName, Snapshot.LocationType locationType, Boolean isFromVmSnapshot, List<Long> zoneIds) throws ResourceAllocationException {
        Account caller = CallContext.current().getCallingAccount();
        VolumeInfo volume = volFactory.getVolume(volumeId);
        supportedByHypervisor(volume, isFromVmSnapshot);

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
                _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_UPDATE_RESOURCE_COUNT, 0L, 0L, msg, "Snapshot resource limit exceeded for account id : " + owner.getId()
                        + ". Failed to create recurring snapshots; please use updateResourceLimit to increase the limit");
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
            if (storagePool.isManaged() && HypervisorType.Any.equals(hypervisorType)) {
                if (ImageFormat.VHD.equals(volume.getFormat())) {
                    hypervisorType = HypervisorType.XenServer;
                } else if (ImageFormat.OVA.equals(volume.getFormat())) {
                    hypervisorType = HypervisorType.VMware;
                } else if (ImageFormat.QCOW2.equals(volume.getFormat())) {
                    hypervisorType = HypervisorType.KVM;
                }
            }
        } else {
            hypervisorType = volume.getHypervisorType();
        }

        SnapshotVO snapshotVO = new SnapshotVO(volume.getDataCenterId(), volume.getAccountId(), volume.getDomainId(), volume.getId(), volume.getDiskOfferingId(), snapshotName,
                (short)snapshotType.ordinal(), snapshotType.name(), volume.getSize(), volume.getMinIops(), volume.getMaxIops(), hypervisorType, locationType);

        SnapshotVO snapshot = _snapshotDao.persist(snapshotVO);
        if (snapshot == null) {
            throw new CloudRuntimeException("Failed to create snapshot for volume: " + volume.getId());
        }
        CallContext.current().putContextParameter(Snapshot.class, snapshot.getUuid());
        _resourceLimitMgr.incrementResourceCount(volume.getAccountId(), ResourceType.snapshot);
        _resourceLimitMgr.incrementResourceCount(volume.getAccountId(), ResourceType.secondary_storage, new Long(volume.getSize()));
        return snapshot;
    }

    @Override
    public void markVolumeSnapshotsAsDestroyed(Volume volume) {
        List<SnapshotVO> snapshots = _snapshotDao.listByVolumeId(volume.getId());
        for (SnapshotVO snapshot: snapshots) {
            List<SnapshotDataStoreVO> snapshotDataStoreVOs = _snapshotStoreDao.findBySnapshotId(snapshot.getId());
            if (CollectionUtils.isEmpty(snapshotDataStoreVOs)) {
                snapshot.setState(Snapshot.State.Destroyed);
                _snapshotDao.update(snapshot.getId(), snapshot);
            }
        }
    }

    private boolean checkAndProcessSnapshotAlreadyExistInStore(long snapshotId, DataStore dstSecStore) {
        SnapshotDataStoreVO dstSnapshotStore = _snapshotStoreDao.findByStoreSnapshot(DataStoreRole.Image, dstSecStore.getId(), snapshotId);
        if (dstSnapshotStore == null) {
            return false;
        }
        if (dstSnapshotStore.getState() == ObjectInDataStoreStateMachine.State.Ready) {
            if (!dstSnapshotStore.isDisplay()) {
                s_logger.debug(String.format("Snapshot ID: %d is in ready state on image store ID: %d, marking it displayable for view", snapshotId, dstSnapshotStore.getDataStoreId()));
                dstSnapshotStore.setDisplay(true);
                _snapshotStoreDao.update(dstSnapshotStore.getId(), dstSnapshotStore);
            }
            return true; // already downloaded on this image store
        }
        if (List.of(VMTemplateStorageResourceAssoc.Status.ABANDONED,
                VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR,
                VMTemplateStorageResourceAssoc.Status.NOT_DOWNLOADED,
                VMTemplateStorageResourceAssoc.Status.UNKNOWN).contains(dstSnapshotStore.getDownloadState()) ||
                !List.of(ObjectInDataStoreStateMachine.State.Creating,
                        ObjectInDataStoreStateMachine.State.Copying).contains(dstSnapshotStore.getState())) {
            _snapshotStoreDao.removeBySnapshotStore(snapshotId, dstSecStore.getId(), DataStoreRole.Image);
        }
        return false;
    }

    @DB
    private boolean copySnapshotToZone(SnapshotDataStoreVO snapshotDataStoreVO, DataStore srcSecStore,
           DataCenterVO dstZone, DataStore dstSecStore, Account account)
            throws ResourceAllocationException {
        final long snapshotId = snapshotDataStoreVO.getSnapshotId();
        final long dstZoneId = dstZone.getId();
        if (checkAndProcessSnapshotAlreadyExistInStore(snapshotId, dstSecStore)) {
            return true;
        }
        _resourceLimitMgr.checkResourceLimit(account, ResourceType.secondary_storage, snapshotDataStoreVO.getSize());
        // snapshotId may refer to ID of a removed parent snapshot
        SnapshotInfo snapshotOnSecondary = snapshotFactory.getSnapshot(snapshotId, srcSecStore);
        String copyUrl = null;
        try {
            AsyncCallFuture<CreateCmdResult> future = snapshotSrv.queryCopySnapshot(snapshotOnSecondary);
            CreateCmdResult result = future.get();
            if (!result.isFailed()) {
                copyUrl = result.getPath();
            }
        } catch (InterruptedException | ExecutionException | ResourceUnavailableException ex) {
            s_logger.error(String.format("Failed to prepare URL for copy for snapshot ID: %d on store: %s", snapshotId, srcSecStore.getName()), ex);
        }
        if (StringUtils.isEmpty(copyUrl)) {
            s_logger.error(String.format("Unable to prepare URL for copy for snapshot ID: %d on store: %s", snapshotId, srcSecStore.getName()));
            return false;
        }
        s_logger.debug(String.format("Copying snapshot ID: %d to destination zones using download URL: %s", snapshotId, copyUrl));
        try {
            AsyncCallFuture<SnapshotResult> future = snapshotSrv.copySnapshot(snapshotOnSecondary, copyUrl, dstSecStore);
            SnapshotResult result = future.get();
            if (result.isFailed()) {
                s_logger.debug(String.format("Copy snapshot ID: %d failed for image store %s: %s", snapshotId, dstSecStore.getName(), result.getResult()));
                return false;
            }
            snapshotZoneDao.addSnapshotToZone(snapshotId, dstZoneId);
            _resourceLimitMgr.incrementResourceCount(account.getId(), ResourceType.secondary_storage, snapshotDataStoreVO.getSize());
            if (account.getId() != Account.ACCOUNT_ID_SYSTEM) {
                SnapshotVO snapshotVO = _snapshotDao.findByIdIncludingRemoved(snapshotId);
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_SNAPSHOT_COPY, account.getId(), dstZoneId, snapshotId, null, null, null, snapshotVO.getSize(),
                        snapshotVO.getSize(), snapshotVO.getClass().getName(), snapshotVO.getUuid());
            }
            return true;
        } catch (InterruptedException | ExecutionException | ResourceUnavailableException ex) {
            s_logger.debug(String.format("Failed to copy snapshot ID: %d to image store: %s", snapshotId, dstSecStore.getName()));
        }
        return false;
    }

    @DB
    private boolean copySnapshotChainToZone(SnapshotVO snapshotVO, DataStore srcSecStore, DataCenterVO destZone, Account account)
            throws StorageUnavailableException, ResourceAllocationException {
        final long snapshotId = snapshotVO.getId();
        final long destZoneId = destZone.getId();
        SnapshotDataStoreVO currentSnap = _snapshotStoreDao.findByStoreSnapshot(DataStoreRole.Image, srcSecStore.getId(), snapshotId);;
        List<SnapshotDataStoreVO> snapshotChain = new ArrayList<>();
        long size = 0L;
        DataStore dstSecStore = null;
        do {
            dstSecStore = getSnapshotZoneImageStore(currentSnap.getSnapshotId(), destZone.getId());
            if (dstSecStore != null) {
                s_logger.debug(String.format("Snapshot ID: %d is already present in secondary storage: %s" +
                        " in zone %s in ready state, don't need to copy any further",
                        currentSnap.getSnapshotId(), dstSecStore.getName(), destZone));
                if (snapshotId == currentSnap.getSnapshotId()) {
                    checkAndProcessSnapshotAlreadyExistInStore(snapshotId, dstSecStore);
                }
                break;
            }
            snapshotChain.add(currentSnap);
            size += currentSnap.getSize();
            currentSnap = currentSnap.getParentSnapshotId() == 0 ?
                    null :
                    _snapshotStoreDao.findByStoreSnapshot(DataStoreRole.Image, srcSecStore.getId(), currentSnap.getParentSnapshotId());
        } while (currentSnap != null);
        if (CollectionUtils.isEmpty(snapshotChain)) {
            return true;
        }
        try {
            _resourceLimitMgr.checkResourceLimit(account, ResourceType.secondary_storage, size);
        } catch (ResourceAllocationException e) {
            s_logger.error(String.format("Unable to allocate secondary storage resources for snapshot chain for %s with size: %d", snapshotVO, size), e);
            return false;
        }
        Collections.reverse(snapshotChain);
        if (dstSecStore == null) {
            // find all eligible image stores for the destination zone
            List<DataStore> dstSecStores = dataStoreMgr.getImageStoresByScopeExcludingReadOnly(new ZoneScope(destZoneId));
            if (CollectionUtils.isEmpty(dstSecStores)) {
                throw new StorageUnavailableException("Destination zone is not ready, no image store associated", DataCenter.class, destZoneId);
            }
            dstSecStore = dataStoreMgr.getImageStoreWithFreeCapacity(dstSecStores);
            if (dstSecStore == null) {
                throw new StorageUnavailableException("Destination zone is not ready, no image store with free capacity", DataCenter.class, destZoneId);
            }
        }
        s_logger.debug(String.format("Copying snapshot chain for snapshot ID: %d on secondary store: %s of zone ID: %d", snapshotId, dstSecStore.getName(), destZoneId));
        for (SnapshotDataStoreVO snapshotDataStoreVO : snapshotChain) {
            if (!copySnapshotToZone(snapshotDataStoreVO, srcSecStore, destZone, dstSecStore, account)) {
                s_logger.error(String.format("Failed to copy snapshot: %s to zone: %s due to failure to copy snapshot ID: %d from snapshot chain",
                        snapshotVO, destZone, snapshotDataStoreVO.getSnapshotId()));
                return false;
            }
        }
        return true;
    }

    @DB
    private List<String> copySnapshotToZones(SnapshotVO snapshotVO, DataStore srcSecStore, List<DataCenterVO> dstZones) throws StorageUnavailableException, ResourceAllocationException {
        AccountVO account = _accountDao.findById(snapshotVO.getAccountId());
        List<String> failedZones = new ArrayList<>();
        for (DataCenterVO destZone : dstZones) {
            if (!copySnapshotChainToZone(snapshotVO, srcSecStore, destZone, account)) {
                failedZones.add(destZone.getName());
            }
        }
        return failedZones;
    }

    protected Pair<SnapshotVO, Long> getCheckedSnapshotForCopy(final long snapshotId, final List<Long> destZoneIds, Long sourceZoneId) {
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
        if (snapshot == null) {
            throw new InvalidParameterValueException("Unable to find snapshot with id");
        }
        // Verify snapshot is BackedUp and is on secondary store
        if (!Snapshot.State.BackedUp.equals(snapshot.getState())) {
            throw new InvalidParameterValueException("Snapshot is not backed up");
        }
        if (snapshot.getLocationType() != null && !Snapshot.LocationType.SECONDARY.equals(snapshot.getLocationType())) {
            throw new InvalidParameterValueException("Snapshot is not backed up");
        }
        if (CollectionUtils.isEmpty(destZoneIds)) {
            throw new InvalidParameterValueException("Please specify valid destination zone(s).");
        }
        Volume volume = _volsDao.findById(snapshot.getVolumeId());
        if (sourceZoneId == null) {
            sourceZoneId = volume.getDataCenterId();
        }
        if (destZoneIds.contains(sourceZoneId)) {
            throw new InvalidParameterValueException("Please specify different source and destination zones.");
        }
        DataCenterVO sourceZone = dataCenterDao.findById(sourceZoneId);
        if (sourceZone == null) {
            throw new InvalidParameterValueException("Please specify a valid source zone.");
        }
        return new Pair<>(snapshot, sourceZoneId);
    }

    protected DataCenterVO getCheckedDestinationZoneForSnapshotCopy(long zoneId, boolean isRootAdmin) {
        DataCenterVO dstZone = dataCenterDao.findById(zoneId);
        if (dstZone == null) {
            throw new InvalidParameterValueException("Please specify a valid destination zone.");
        }
        if (Grouping.AllocationState.Disabled.equals(dstZone.getAllocationState()) && !isRootAdmin) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + dstZone.getName());
        }
        if (DataCenter.Type.Edge.equals(dstZone.getType())) {
            s_logger.error(String.format("Edge zone %s specified for snapshot copy", dstZone));
            throw new InvalidParameterValueException(String.format("Snapshot copy is not supported by zone %s", dstZone.getName()));
        }
        return dstZone;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SNAPSHOT_COPY, eventDescription = "copying snapshot", create = false)
    public Snapshot copySnapshot(CopySnapshotCmd cmd) throws StorageUnavailableException, ResourceAllocationException {
        final Long snapshotId = cmd.getId();
        Long sourceZoneId = cmd.getSourceZoneId();
        List<Long> destZoneIds = cmd.getDestinationZoneIds();
        Account caller = CallContext.current().getCallingAccount();
        Pair<SnapshotVO, Long> snapshotZonePair = getCheckedSnapshotForCopy(snapshotId, destZoneIds, sourceZoneId);
        SnapshotVO snapshot = snapshotZonePair.first();
        sourceZoneId = snapshotZonePair.second();
        Map<Long, DataCenterVO> dataCenterVOs = new HashMap<>();
        boolean isRootAdminCaller = _accountMgr.isRootAdmin(caller.getId());
        for (Long destZoneId: destZoneIds) {
            DataCenterVO dstZone = getCheckedDestinationZoneForSnapshotCopy(destZoneId, isRootAdminCaller);
            dataCenterVOs.put(destZoneId, dstZone);
        }
        _accountMgr.checkAccess(caller, SecurityChecker.AccessType.OperateEntry, true, snapshot);
        DataStore srcSecStore = getSnapshotZoneImageStore(snapshotId, sourceZoneId);
        if (srcSecStore == null) {
            throw new InvalidParameterValueException(String.format("There is no snapshot ID: %s ready on image store", snapshot.getUuid()));
        }
        List<String> failedZones = copySnapshotToZones(snapshot, srcSecStore, new ArrayList<>(dataCenterVOs.values()));
        if (destZoneIds.size() > failedZones.size()){
            if (!failedZones.isEmpty()) {
                s_logger.error(String.format("There were failures when copying snapshot to zones: %s",
                        StringUtils.joinWith(", ", failedZones.toArray())));
            }
            return snapshot;
        } else {
            throw new CloudRuntimeException("Failed to copy snapshot");
        }
    }

    protected void copyNewSnapshotToZones(long snapshotId, long zoneId, List<Long> destZoneIds) {
        if (CollectionUtils.isEmpty(destZoneIds)) {
            return;
        }
        List<String> failedZones = new ArrayList<>();
        SnapshotVO snapshotVO = _snapshotDao.findById(snapshotId);
        long startEventId = ActionEventUtils.onStartedActionEvent(CallContext.current().getCallingUserId(),
                CallContext.current().getCallingAccountId(), EventTypes.EVENT_SNAPSHOT_COPY,
                String.format("Copying snapshot ID: %s", snapshotVO.getUuid()), snapshotId,
                ApiCommandResourceType.Snapshot.toString(), true, 0);
        DataStore dataStore = getSnapshotZoneImageStore(snapshotId, zoneId);
        String completedEventLevel = EventVO.LEVEL_ERROR;
        String completedEventMsg = String.format("Copying snapshot ID: %s failed", snapshotVO.getUuid());
        if (dataStore == null) {
            s_logger.error(String.format("Unable to find an image store for zone ID: %d where snapshot %s is in Ready state", zoneId, snapshotVO));
            ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(),
                    CallContext.current().getCallingAccountId(), completedEventLevel, EventTypes.EVENT_SNAPSHOT_COPY,
                    completedEventMsg, snapshotId, ApiCommandResourceType.Snapshot.toString(), startEventId);
            return;
        }
        List<DataCenterVO> dataCenterVOs = new ArrayList<>();
        for (Long destZoneId: destZoneIds) {
            DataCenterVO dstZone = dataCenterDao.findById(destZoneId);
            dataCenterVOs.add(dstZone);
        }
        try {
            failedZones = copySnapshotToZones(snapshotVO, dataStore, dataCenterVOs);
            if (CollectionUtils.isNotEmpty(failedZones)) {
                s_logger.error(String.format("There were failures while copying snapshot %s to zones: %s",
                        snapshotVO, StringUtils.joinWith(", ", failedZones.toArray())));
            }
        } catch (ResourceAllocationException | StorageUnavailableException | CloudRuntimeException e) {
            s_logger.error(String.format("Error while copying snapshot %s to zones: %s", snapshotVO, StringUtils.joinWith(",", destZoneIds.toArray())));
        }
        if (failedZones.size() < destZoneIds.size()) {
            final List<String> failedZonesFinal = failedZones;
            String zoneNames = StringUtils.joinWith(", ", dataCenterVOs.stream().filter(x -> !failedZonesFinal.contains(x.getUuid())).map(DataCenterVO::getName).collect(Collectors.toList()));
            completedEventLevel = EventVO.LEVEL_INFO;
            completedEventMsg = String.format("Completed copying snapshot ID: %s to zone(s): %s", snapshotVO.getUuid(), zoneNames);
        }
        ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(),
                CallContext.current().getCallingAccountId(), completedEventLevel, EventTypes.EVENT_SNAPSHOT_COPY,
                completedEventMsg, snapshotId, ApiCommandResourceType.Snapshot.toString(), startEventId);
    }
}
