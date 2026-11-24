/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.snapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.DeltaMergeTreeTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.agent.api.storage.CreateDiskOnlyVmSnapshotAnswer;
import com.cloud.agent.api.storage.CreateDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.storage.DeleteDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.storage.MergeDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.storage.RevertDiskOnlyVmSnapshotAnswer;
import com.cloud.agent.api.storage.RevertDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.to.DataTO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.CreateSnapshotPayload;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.SnapshotZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

/**
 * Volume snapshots of KVM QCOW2 volumes on file-based primary storage that do not copy the volume (see {@link SnapshotManager#KvmFileBasedVolumeSnapshotNoCopy}).
 * <br/><br/>
 * Taking a snapshot freezes the current file of the volume, which becomes the snapshot, and moves the volume onto a new, empty QCOW2 delta on top of it. Reverting
 * the snapshot replaces the current delta of the volume with a new, empty delta on top of the snapshot. Deleting the snapshot merges its child (the next snapshot of
 * the volume, or the current delta of the volume) into it. The agent side reuses the commands of the KVM disk-only Instance snapshots
 * ({@link org.apache.cloudstack.storage.vmsnapshot.KvmFileBasedStorageVmSnapshotStrategy}) for a single volume.
 * <br/><br/>
 * The snapshots of a volume always form a single chain ordered by creation: only the most recent snapshot of a volume can be reverted, so no tree of deltas is ever
 * created.
 */
public class KvmFileBasedNoCopySnapshotStrategy extends SnapshotStrategyBase {

    /**
     * Snapshot detail holding the position of a no-copy snapshot in the backing chain of its volume.
     */
    protected static final String KVM_FILE_BASED_NO_COPY_SNAPSHOT_SEQUENCE = "kvmFileBasedNoCopySnapshotSequence";

    /**
     * Snapshot detail set when deleting the snapshot failed after the merge may have started: its file may already hold data of its child, so it is no longer a
     * valid point in time and cannot be reverted (deleting it again completes the merge).
     */
    protected static final String KVM_FILE_BASED_NO_COPY_SNAPSHOT_MERGE_INCOMPLETE = "kvmFileBasedNoCopySnapshotMergeIncomplete";

    /**
     * Snapshot detail holding the new volume file of a take whose outcome is unknown (no answer from the host for a running Instance).
     */
    protected static final String KVM_FILE_BASED_NO_COPY_SNAPSHOT_PENDING_VOLUME_PATH = "kvmFileBasedNoCopySnapshotPendingVolumePath";

    /**
     * Wait for the merge command: at least the agent's own merge timeout (qcow2.delta.merge.timeout, 72 hours by default), so that a long merge is not taken for
     * a lost answer.
     */
    protected static final int MERGE_COMMAND_WAIT_SECONDS = 60 * 60 * 72 + 10 * 60;

    protected static final Set<Storage.StoragePoolType> SUPPORTED_POOL_TYPES = Set.of(Storage.StoragePoolType.NetworkFilesystem, Storage.StoragePoolType.SharedMountPoint,
            Storage.StoragePoolType.Filesystem);

    @Inject
    protected SnapshotDao snapshotDao;

    @Inject
    protected SnapshotDataStoreDao snapshotStoreDao;

    @Inject
    protected SnapshotDetailsDao snapshotDetailsDao;

    @Inject
    protected SnapshotZoneDao snapshotZoneDao;

    @Inject
    protected SnapshotDataFactory snapshotDataFactory;

    @Inject
    protected VolumeDao volumeDao;

    @Inject
    protected VolumeDataFactory volumeDataFactory;

    @Inject
    protected VMInstanceDao vmInstanceDao;

    @Inject
    protected VMSnapshotDao vmSnapshotDao;

    @Inject
    protected PrimaryDataStoreDao storagePoolDao;

    @Inject
    protected SnapshotManager snapshotManager;

    @Inject
    protected EndPointSelector endPointSelector;

    @Inject
    protected AgentManager agentManager;

    @Inject
    protected HostDetailsDao hostDetailsDao;

    @Override
    public StrategyPriority canHandle(Snapshot snapshot, Long zoneId, SnapshotOperation op) {
        if (SnapshotOperation.TAKE.equals(op)) {
            return canTakeNoCopySnapshot(snapshot) ? StrategyPriority.HIGHEST : StrategyPriority.CANT_HANDLE;
        }

        if (!snapshotManager.isKvmFileBasedNoCopySnapshot(snapshot.getId())) {
            return StrategyPriority.CANT_HANDLE;
        }

        if (SnapshotOperation.COPY.equals(op)) {
            logger.debug("Snapshot [{}] is a KVM file-based no-copy snapshot, which cannot be copied.", snapshot.getUuid());
            return StrategyPriority.CANT_HANDLE;
        }

        // A no-copy snapshot is part of the backing chain of its volume: no other strategy may delete, revert or back it up.
        return StrategyPriority.HIGHEST;
    }

    /**
     * Checks whether the snapshot can be taken as a KVM file-based no-copy snapshot. Any other snapshot is left to the other strategies, so the behaviour only
     * changes for the storage pools where {@link SnapshotManager#KvmFileBasedVolumeSnapshotNoCopy} is enabled.
     */
    protected boolean canTakeNoCopySnapshot(Snapshot snapshot) {
        SnapshotVO snapshotVO = snapshotDao.findById(snapshot.getId());
        if (snapshotVO == null || Snapshot.Type.GROUP.name().equals(snapshotVO.getTypeDescription())) {
            return false;
        }

        if (!Hypervisor.HypervisorType.KVM.equals(snapshot.getHypervisorType())) {
            return false;
        }

        VolumeVO volume = volumeDao.findById(snapshot.getVolumeId());
        if (volume == null || volume.getPoolId() == null) {
            return false;
        }

        StoragePoolVO storagePool = storagePoolDao.findById(volume.getPoolId());
        if (storagePool == null || storagePool.isManaged() || !SUPPORTED_POOL_TYPES.contains(storagePool.getPoolType())) {
            return false;
        }

        if (!isNoCopySnapshotEnabledForStoragePool(storagePool.getId())) {
            return false;
        }

        String cantHandleLog = String.format("KVM file-based no-copy snapshot strategy cannot take snapshot [%s] of volume [%s], although [%s] is enabled for storage pool [%s],",
                snapshot.getUuid(), volume.getUuid(), SnapshotManager.KvmFileBasedVolumeSnapshotNoCopy.key(), storagePool.getUuid());

        if (!Storage.ImageFormat.QCOW2.equals(volume.getFormat())) {
            logger.debug("{} as the volume format is [{}] and not QCOW2.", cantHandleLog, volume.getFormat());
            return false;
        }

        if (volume.getPassphraseId() != null || volume.getKmsKeyId() != null) {
            logger.debug("{} as the volume is encrypted.", cantHandleLog);
            return false;
        }

        if (snapshotManager.isBackupSnapshotToSecondaryForZone(volume.getDataCenterId())) {
            logger.debug("{} as snapshots are backed up to secondary storage in the zone of the volume ([{}] is true).", cantHandleLog,
                    SnapshotInfo.BackupSnapshotAfterTakingSnapshot.key());
            return false;
        }

        if (!isPayloadSupported(snapshot, cantHandleLog)) {
            return false;
        }

        if (volume.getInstanceId() == null) {
            return true;
        }

        VMInstanceVO vm = vmInstanceDao.findById(volume.getInstanceId());
        if (vm == null) {
            return true;
        }

        if (!VirtualMachine.State.Running.equals(vm.getState()) && !VirtualMachine.State.Stopped.equals(vm.getState())) {
            logger.debug("{} as its Instance [{}] is in state [{}] and not Running or Stopped.", cantHandleLog, vm.getUuid(), vm.getState());
            return false;
        }

        if (CollectionUtils.isNotEmpty(vmSnapshotDao.findByVm(vm.getId()))) {
            logger.debug("{} as its Instance [{}] has Instance snapshots, which also change the backing chain of the volume.", cantHandleLog, vm.getUuid());
            return false;
        }

        if (vm.getBackupOfferingId() != null) {
            logger.debug("{} as its Instance [{}] has a backup offering, whose backups may also change the backing chain of the volume.", cantHandleLog, vm.getUuid());
            return false;
        }

        if (VirtualMachine.State.Running.equals(vm.getState()) && !hostSupportsSingleVolumeDiskOnlySnapshots(vm.getHostId())) {
            logger.info("{} as the host [{}] of its running Instance [{}] does not advertise [{}]; the KVM agent must be upgraded. Taking a regular snapshot instead.",
                    cantHandleLog, vm.getHostId(), vm.getUuid(), Host.HOST_KVM_DISK_ONLY_SNAPSHOT_SINGLE_VOLUME);
            return false;
        }

        return true;
    }

    /**
     * Older KVM agents also take an external snapshot of the disks that are not listed in the snapshot XML, which would move the other volumes of the Instance onto
     * files unknown to CloudStack.
     */
    protected boolean hostSupportsSingleVolumeDiskOnlySnapshots(Long hostId) {
        if (hostId == null) {
            return false;
        }
        DetailVO capability = hostDetailsDao.findDetail(hostId, Host.HOST_KVM_DISK_ONLY_SNAPSHOT_SINGLE_VOLUME);
        return capability != null && Boolean.parseBoolean(capability.getValue());
    }

    protected boolean isNoCopySnapshotEnabledForStoragePool(long storagePoolId) {
        return Boolean.TRUE.equals(SnapshotManager.KvmFileBasedVolumeSnapshotNoCopy.valueIn(storagePoolId));
    }

    protected boolean isPayloadSupported(Snapshot snapshot, String cantHandleLog) {
        if (!(snapshot instanceof SnapshotInfo) || !(((SnapshotInfo) snapshot).getPayload() instanceof CreateSnapshotPayload)) {
            logger.debug("{} as there is no snapshot payload to validate.", cantHandleLog);
            return false;
        }

        CreateSnapshotPayload payload = (CreateSnapshotPayload) ((SnapshotInfo) snapshot).getPayload();
        if (payload.isKvmIncrementalSnapshot()) {
            logger.debug("{} as KVM incremental snapshots are enabled ([{}] is true).", cantHandleLog, SnapshotManager.kvmIncrementalSnapshot.key());
            return false;
        }

        if (CollectionUtils.isNotEmpty(payload.getZoneIds()) || CollectionUtils.isNotEmpty(payload.getStoragePoolIds())) {
            logger.debug("{} as it would have to be copied to other zones or storage pools.", cantHandleLog);
            return false;
        }

        if (payload.getQuiescevm()) {
            logger.debug("{} as quiescing the Instance was requested.", cantHandleLog);
            return false;
        }
        return true;
    }

    @Override
    public SnapshotInfo takeSnapshot(SnapshotInfo snapshot) {
        SnapshotVO snapshotVO = snapshotDao.acquireInLockTable(snapshot.getId());
        if (snapshotVO == null) {
            throw new CloudRuntimeException(String.format("Failed to get lock on snapshot [%s].", snapshot.getUuid()));
        }

        try {
            return takeSnapshotInternal(snapshot);
        } finally {
            snapshotDao.releaseFromLockTable(snapshot.getId());
        }
    }

    protected SnapshotInfo takeSnapshotInternal(SnapshotInfo snapshot) {
        VolumeInfo volumeInfo = snapshot.getBaseVolume();

        logger.info("Taking KVM file-based no-copy snapshot [{}] of volume [{}].", snapshot.getUuid(), volumeInfo.getUuid());

        SnapshotVO snapshotVO = snapshotDao.findById(snapshot.getId());
        reserveVolume(volumeInfo, Volume.Event.SnapshotRequested, snapshotVO);

        SnapshotObject snapshotOnPrimary = null;
        boolean succeeded = false;
        boolean hostSucceeded = false;
        boolean keepVolumeReserved = false;
        try {
            // Marks the snapshot before anything is done on storage, so that operations refused while a volume has no-copy snapshots (e.g. assigning a backup
            // offering) also see a take in progress. Then re-checks what such an operation could have changed concurrently.
            snapshotDetailsDao.addDetail(snapshot.getId(), SnapshotManager.KVM_FILE_BASED_NO_COPY_SNAPSHOT, Boolean.TRUE.toString(), false);
            VMInstanceVO vm = getAttachedVm(volumeInfo);
            if (vm != null && vm.getBackupOfferingId() != null) {
                throw new CloudRuntimeException(String.format("Instance [%s] was assigned to a backup offering while KVM file-based no-copy snapshot [%s] was being " +
                        "taken; the snapshot is not taken.", vm.getUuid(), snapshot.getUuid()));
            }
            boolean vmRunning = isVmRunning(vm);

            // The position of the snapshot in the backing chain, which follows the order of the takes and not the order of the snapshot ids (allocated before
            // the work is queued). Computed while the volume is reserved.
            long sequence = listNoCopySnapshotsOfVolume(volumeInfo.getId()).stream().mapToLong(this::getChainSequence).max().orElse(0L) + 1;
            snapshotOnPrimary = createSnapshotOnPrimary(snapshot);
            VolumeObjectTO volumeTO = (VolumeObjectTO) volumeInfo.getTO();
            String oldVolumePath = volumeTO.getPath();
            String newVolumePath = UUID.randomUUID().toString();

            VMSnapshotTO target = new VMSnapshotTO(snapshot.getId(), snapshot.getUuid(), VMSnapshot.Type.Disk, null, null, false, null, false);
            CreateDiskOnlyVmSnapshotCommand command = new CreateDiskOnlyVmSnapshotCommand(vm != null ? vm.getInstanceName() : null, vm != null ? vm.getUuid() : null, target,
                    List.of(new Pair<>(volumeTO, newVolumePath)), null, vmRunning ? VirtualMachine.State.Running : VirtualMachine.State.Stopped, false);

            // Recorded before the host acts, so that whatever happens next (no answer, management server restart) the new file of the volume can be found.
            snapshotDetailsDao.addDetail(snapshot.getId(), KVM_FILE_BASED_NO_COPY_SNAPSHOT_PENDING_VOLUME_PATH, newVolumePath, false);

            Answer answer = sendToHost(getHostIdForOperation(volumeInfo, vm), command);
            if (answer == null && vmRunning) {
                // The running Instance may already be writing to the new delta: releasing the volume with its old path would make its next start lose those writes.
                keepVolumeReserved = true;
                throw new CloudRuntimeException(String.format("No answer from the host while taking KVM file-based no-copy snapshot [%s] of volume [%s] of running " +
                        "Instance [%s]. The volume stays in state [%s] until an administrator checks whether the Instance uses the new file [%s] (then sets it as the " +
                        "volume path) or still the file [%s], and then sets the volume state back to Ready.", snapshot.getUuid(), volumeInfo.getUuid(), vm.getUuid(),
                        Volume.State.Snapshotting, newVolumePath, oldVolumePath));
            }
            if (!(answer instanceof CreateDiskOnlyVmSnapshotAnswer) || !answer.getResult()) {
                throw new CloudRuntimeException(String.format("Failed to take KVM file-based no-copy snapshot [%s] of volume [%s] due to [%s].", snapshot.getUuid(),
                        volumeInfo.getUuid(), getAnswerDetails(answer)));
            }

            // The volume is now on the new delta: record it first, so that a later failure can never leave the volume pointing to the frozen file. From here on,
            // the snapshot is never marked as failed: its frozen file is in the backing chain of the volume and must not be cleaned up as a failed snapshot.
            hostSucceeded = true;
            VolumeVO volumeVO = volumeDao.findById(volumeInfo.getId());
            volumeVO.setPath(newVolumePath);
            volumeDao.update(volumeVO.getId(), volumeVO);

            Long frozenFileSize = ((CreateDiskOnlyVmSnapshotAnswer) answer).getMapVolumeToSnapshotSize().get(volumeInfo.getUuid());

            SnapshotObjectTO snapshotTO = new SnapshotObjectTO();
            snapshotTO.setPath(oldVolumePath);
            snapshotTO.setPhysicalSize(frozenFileSize);
            snapshotOnPrimary.processEvent(ObjectInDataStoreStateMachine.Event.OperationSucceeded, new CreateObjectAnswer(snapshotTO));
            snapshotOnPrimary.processEvent(Snapshot.Event.OperationSucceeded);

            snapshotDetailsDao.addDetail(snapshot.getId(), KVM_FILE_BASED_NO_COPY_SNAPSHOT_SEQUENCE, String.valueOf(sequence), false);
            snapshotDetailsDao.removeDetail(snapshot.getId(), KVM_FILE_BASED_NO_COPY_SNAPSHOT_PENDING_VOLUME_PATH);

            logger.info("Took KVM file-based no-copy snapshot [{}] of volume [{}]: the snapshot is [{}] and the volume is now on [{}].", snapshot.getUuid(),
                    volumeInfo.getUuid(), oldVolumePath, newVolumePath);
            succeeded = true;
            return snapshotOnPrimary;
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException(String.format("Failed to update the state of snapshot [%s].", snapshot.getUuid()), e);
        } finally {
            if (!keepVolumeReserved) {
                // Once the host has succeeded the volume is on its new file, which is already recorded: the volume is usable even if the snapshot metadata is not.
                volumeInfo.stateTransit(succeeded || hostSucceeded ? Volume.Event.OperationSucceeded : Volume.Event.OperationFailed);
            }
            if (hostSucceeded && !succeeded) {
                logger.error("KVM file-based no-copy snapshot [{}] of volume [{}] was taken on storage, but its metadata could not be fully updated. It is not marked as " +
                        "failed, so that its file (part of the backing chain of the volume) is not cleaned up.", snapshot.getUuid(), volumeInfo.getUuid());
            } else if (!succeeded && !keepVolumeReserved && snapshotOnPrimary != null) {
                markSnapshotOnPrimaryAsFailed(snapshotOnPrimary);
            } else if (!succeeded && !keepVolumeReserved) {
                snapshotDetailsDao.removeDetail(snapshot.getId(), SnapshotManager.KVM_FILE_BASED_NO_COPY_SNAPSHOT);
            }
        }
    }

    protected SnapshotObject createSnapshotOnPrimary(SnapshotInfo snapshot) {
        try {
            SnapshotObject snapshotOnPrimary = (SnapshotObject) snapshot.getDataStore().create(snapshot);
            snapshotOnPrimary.processEvent(Snapshot.Event.CreateRequested);
            snapshotOnPrimary.processEvent(ObjectInDataStoreStateMachine.Event.CreateOnlyRequested);
            return snapshotOnPrimary;
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException(String.format("Failed to update the state of snapshot [%s].", snapshot.getUuid()), e);
        }
    }

    /**
     * Never throws, as it runs in a finally block and must not hide the original failure.
     */
    protected void markSnapshotOnPrimaryAsFailed(SnapshotObject snapshotOnPrimary) {
        try {
            snapshotOnPrimary.processEvent(Snapshot.Event.OperationFailed);
        } catch (NoTransitionException | RuntimeException e) {
            logger.warn("Failed to mark snapshot [{}] as failed.", snapshotOnPrimary.getUuid(), e);
        }
        try {
            snapshotOnPrimary.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
        } catch (RuntimeException e) {
            logger.warn("Failed to mark the primary storage reference of snapshot [{}] as failed.", snapshotOnPrimary.getUuid(), e);
        }
    }

    @Override
    public boolean revertSnapshot(SnapshotInfo snapshot) {
        SnapshotVO snapshotVO = snapshotDao.acquireInLockTable(snapshot.getId());
        if (snapshotVO == null) {
            throw new CloudRuntimeException(String.format("Failed to get lock on snapshot [%s].", snapshot.getUuid()));
        }

        try {
            return revertSnapshotInternal(snapshotVO);
        } finally {
            snapshotDao.releaseFromLockTable(snapshot.getId());
        }
    }

    protected boolean revertSnapshotInternal(SnapshotVO snapshotVO) {
        VolumeInfo volumeInfo = volumeDataFactory.getVolume(snapshotVO.getVolumeId());
        if (volumeInfo == null) {
            throw new CloudRuntimeException(String.format("Unable to find the volume of snapshot [%s].", snapshotVO.getUuid()));
        }

        // Reserve the volume before reading its chain and paths, so that no other snapshot operation can change them underneath.
        reserveVolume(volumeInfo, Volume.Event.RevertSnapshotRequested, snapshotVO);
        boolean succeeded = false;
        boolean keepVolumeReserved = false;
        try {
            volumeInfo = volumeDataFactory.getVolume(snapshotVO.getVolumeId());

            if (snapshotDetailsDao.findDetail(snapshotVO.getId(), KVM_FILE_BASED_NO_COPY_SNAPSHOT_MERGE_INCOMPLETE) != null) {
                throw new InvalidParameterValueException(String.format("Snapshot [%s] cannot be reverted: a previous delete of it failed during the merge, so its file " +
                        "may already hold newer data. Delete it again to complete the merge.", snapshotVO.getUuid()));
            }

            List<SnapshotVO> chain = listNoCopySnapshotsOfVolume(volumeInfo.getId());
            if (chain.isEmpty() || chain.get(chain.size() - 1).getId() != snapshotVO.getId()) {
                throw new InvalidParameterValueException(String.format("Only the most recent KVM file-based no-copy snapshot of volume [%s] can be reverted. Delete the " +
                        "snapshots taken after snapshot [%s] before reverting it.", volumeInfo.getUuid(), snapshotVO.getUuid()));
            }

            VMInstanceVO vm = getAttachedVm(volumeInfo);
            if (vm != null && !VirtualMachine.State.Stopped.equals(vm.getState())) {
                throw new InvalidParameterValueException(String.format("The Instance [%s] of volume [%s] must be stopped to revert snapshot [%s]; it is [%s].", vm.getUuid(),
                        volumeInfo.getUuid(), snapshotVO.getUuid(), vm.getState()));
            }

            logger.info("Reverting volume [{}] to KVM file-based no-copy snapshot [{}].", volumeInfo.getUuid(), snapshotVO.getUuid());

            SnapshotObjectTO snapshotTO = getSnapshotObjectTO(snapshotVO, volumeInfo);
            RevertDiskOnlyVmSnapshotCommand command = new RevertDiskOnlyVmSnapshotCommand(List.of(snapshotTO), vm != null ? vm.getInstanceName() : null,
                    vm != null ? vm.getUuid() : null, false, null);

            Answer answer = sendToHost(getHostIdForOperation(volumeInfo, vm), command);
            if (answer == null) {
                // The host may have replaced the file of the volume already: releasing the volume with its old path could start the Instance on a deleted file.
                keepVolumeReserved = true;
                throw new CloudRuntimeException(String.format("No answer from the host while reverting volume [%s] to snapshot [%s]. The volume stays in state [%s] " +
                        "until an administrator checks which file of its backing chain is its current one (a new file backed by [%s] if the revert happened), sets it as " +
                        "the volume path and sets the volume state back to Ready.", volumeInfo.getUuid(), snapshotVO.getUuid(), Volume.State.RevertSnapshotting,
                        snapshotTO.getPath()));
            }
            if (!(answer instanceof RevertDiskOnlyVmSnapshotAnswer) || !answer.getResult() ||
                    CollectionUtils.isEmpty(((RevertDiskOnlyVmSnapshotAnswer) answer).getVolumeObjectTos())) {
                throw new CloudRuntimeException(String.format("Failed to revert volume [%s] to snapshot [%s] due to [%s].", volumeInfo.getUuid(), snapshotVO.getUuid(),
                        getAnswerDetails(answer)));
            }

            String newVolumePath = ((RevertDiskOnlyVmSnapshotAnswer) answer).getVolumeObjectTos().get(0).getPath();
            VolumeVO volumeVO = volumeDao.findById(volumeInfo.getId());
            volumeVO.setPath(newVolumePath);
            // The size of the volume and the primary storage resource count are updated by SnapshotManagerImpl#revertSnapshot.
            volumeDao.update(volumeVO.getId(), volumeVO);

            logger.info("Reverted volume [{}] to KVM file-based no-copy snapshot [{}]: the volume is now on [{}].", volumeInfo.getUuid(), snapshotVO.getUuid(), newVolumePath);
            succeeded = true;
            return true;
        } finally {
            if (!keepVolumeReserved) {
                volumeInfo.stateTransit(succeeded ? Volume.Event.OperationSucceeded : Volume.Event.OperationFailed);
            }
        }
    }

    @Override
    public boolean deleteSnapshot(Long snapshotId, Long zoneId) {
        SnapshotVO snapshotVO = snapshotDao.findById(snapshotId);
        if (snapshotVO == null || Snapshot.State.Destroyed.equals(snapshotVO.getState())) {
            return true;
        }

        if (zoneId != null && zoneId != snapshotVO.getDataCenterId()) {
            throw new InvalidParameterValueException(String.format("Snapshot [%s] is a KVM file-based no-copy snapshot, which only exists on the primary storage of its volume " +
                    "in zone [%s].", snapshotVO.getUuid(), snapshotVO.getDataCenterId()));
        }

        switch (snapshotVO.getState()) {
            case Allocated:
                snapshotDao.remove(snapshotId);
                return true;
            case Error:
                removeSnapshotMetadata(snapshotVO);
                return true;
            case CreatedOnPrimary:
            case BackedUp:
                break;
            default:
                throw new InvalidParameterValueException(String.format("Snapshot [%s] cannot be deleted while it is in state [%s].", snapshotVO.getUuid(), snapshotVO.getState()));
        }

        snapshotVO = snapshotDao.acquireInLockTable(snapshotId);
        if (snapshotVO == null) {
            throw new CloudRuntimeException(String.format("Failed to get lock on snapshot [%s].", snapshotId));
        }

        try {
            // Re-read under the lock: a concurrent delete may have completed while this one waited, and must not be counted twice.
            SnapshotVO current = snapshotDao.findById(snapshotId);
            if (current == null || !(Snapshot.State.BackedUp.equals(current.getState()) || Snapshot.State.CreatedOnPrimary.equals(current.getState()))) {
                throw new InvalidParameterValueException(String.format("Snapshot [%s] was deleted or changed concurrently (state [%s]).", snapshotVO.getUuid(),
                        current == null ? null : current.getState()));
            }
            return deleteSnapshotInternal(current);
        } finally {
            snapshotDao.releaseFromLockTable(snapshotId);
        }
    }

    protected boolean deleteSnapshotInternal(SnapshotVO snapshotVO) {
        VolumeVO volumeVO = volumeDao.findByIdIncludingRemoved(snapshotVO.getVolumeId());
        SnapshotDataStoreVO snapshotStoreRef = snapshotStoreDao.findOneBySnapshotAndDatastoreRole(snapshotVO.getId(), DataStoreRole.Primary);

        if (snapshotStoreRef == null) {
            logger.debug("KVM file-based no-copy snapshot [{}] has no file on primary storage; only its metadata will be removed.", snapshotVO.getUuid());
            removeSnapshotMetadata(snapshotVO);
            return true;
        }

        if (volumeVO == null || (volumeVO.getRemoved() != null && !Volume.State.Expunged.equals(volumeVO.getState()))) {
            // The volume row is gone, or the volume was removed without being expunged (e.g. unmanaged): its files are not CloudStack's to delete.
            logger.debug("The volume of KVM file-based no-copy snapshot [{}] is no longer managed; only the snapshot metadata will be removed.", snapshotVO.getUuid());
            removeSnapshotMetadata(snapshotVO);
            return true;
        }

        if (Volume.State.Expunged.equals(volumeVO.getState())) {
            // The active file of the volume is gone; its frozen files are not, as nothing else references them (no-copy snapshots cannot be used to create
            // volumes or templates, and template files are never one of them).
            deleteFrozenFileOfExpungedVolume(snapshotVO, snapshotStoreRef);
            removeSnapshotMetadata(snapshotVO);
            return true;
        }

        VolumeInfo volumeInfo = volumeDataFactory.getVolume(volumeVO.getId());
        // Reserve the volume before reading its chain and paths, so that no other snapshot operation can change them underneath.
        reserveVolume(volumeInfo, Volume.Event.SnapshotRequested, snapshotVO);
        boolean succeeded = false;
        boolean keepVolumeReserved = false;
        try {
            volumeInfo = volumeDataFactory.getVolume(volumeVO.getId());
            volumeVO = volumeDao.findById(volumeVO.getId());
            snapshotStoreRef = snapshotStoreDao.findOneBySnapshotAndDatastoreRole(snapshotVO.getId(), DataStoreRole.Primary);

            VMInstanceVO vm = getAttachedVm(volumeInfo);
            if (vm != null && !VirtualMachine.State.Running.equals(vm.getState()) && !VirtualMachine.State.Stopped.equals(vm.getState())) {
                throw new InvalidParameterValueException(String.format("Snapshot [%s] cannot be deleted while the Instance [%s] of its volume is in state [%s].",
                        snapshotVO.getUuid(), vm.getUuid(), vm.getState()));
            }
            boolean vmRunning = isVmRunning(vm);

            List<SnapshotVO> chain = listNoCopySnapshotsOfVolume(volumeVO.getId());
            int index = chain.stream().map(SnapshotVO::getId).collect(Collectors.toList()).indexOf(snapshotVO.getId());
            if (index < 0) {
                throw new CloudRuntimeException(String.format("Unable to find snapshot [%s] in the backing chain of volume [%s].", snapshotVO.getUuid(), volumeVO.getUuid()));
            }

            VolumeObjectTO volumeTO = (VolumeObjectTO) volumeInfo.getTO();
            SnapshotObjectTO parentTO = getSnapshotObjectTO(snapshotVO, volumeInfo);
            SnapshotVO childSnapshot = index + 1 < chain.size() ? chain.get(index + 1) : null;
            DeltaMergeTreeTO deltaMergeTree = createDeltaMergeTree(chain, index, volumeInfo, volumeTO, parentTO);

            logger.info("Deleting KVM file-based no-copy snapshot [{}] of volume [{}] by merging [{}] into it.", snapshotVO.getUuid(), volumeVO.getUuid(),
                    deltaMergeTree.getChild().getPath());

            MergeDiskOnlyVmSnapshotCommand command = new MergeDiskOnlyVmSnapshotCommand(List.of(deltaMergeTree), vmRunning, vm != null ? vm.getInstanceName() : null);
            command.setWait(MERGE_COMMAND_WAIT_SECONDS);
            Answer answer = sendToHost(getHostIdForOperation(volumeInfo, vm), command);
            if (answer == null || !answer.getResult()) {
                // The merge may have started: the file of this snapshot may already hold data of its child, so it is no longer a valid point in time.
                snapshotDetailsDao.addDetail(snapshotVO.getId(), KVM_FILE_BASED_NO_COPY_SNAPSHOT_MERGE_INCOMPLETE, Boolean.TRUE.toString(), false);
            }
            if (answer == null) {
                // The host may still complete the merge and delete the child file: releasing the volume could leave it pointing to a deleted file.
                keepVolumeReserved = true;
                throw new CloudRuntimeException(String.format("No answer from the host while deleting KVM file-based no-copy snapshot [%s] of volume [%s] (merging [%s] " +
                        "into [%s]). The volume stays in state [%s] until an administrator checks whether [%s] still exists: if it does not, set [%s] as the path of %s. " +
                        "Then set the volume state back to Ready and delete the snapshot again.", snapshotVO.getUuid(), volumeVO.getUuid(),
                        deltaMergeTree.getChild().getPath(), parentTO.getPath(), Volume.State.Snapshotting, deltaMergeTree.getChild().getPath(), parentTO.getPath(),
                        childSnapshot == null ? "the volume" : "snapshot " + childSnapshot.getUuid()));
            }
            if (!answer.getResult()) {
                throw new CloudRuntimeException(String.format("Failed to delete KVM file-based no-copy snapshot [%s] of volume [%s] due to [%s]. The snapshot can no " +
                        "longer be reverted; delete it again to complete the merge.", snapshotVO.getUuid(), volumeVO.getUuid(), getAnswerDetails(answer)));
            }

            updateMetadataAfterMerge(snapshotVO, snapshotStoreRef, childSnapshot, volumeVO);
            succeeded = true;
            return true;
        } finally {
            if (!keepVolumeReserved) {
                volumeInfo.stateTransit(succeeded ? Volume.Event.OperationSucceeded : Volume.Event.OperationFailed);
            }
        }
    }

    /**
     * Deletes the frozen file of a snapshot whose volume was expunged, through any host with access to the storage pool of the snapshot.
     */
    protected void deleteFrozenFileOfExpungedVolume(SnapshotVO snapshotVO, SnapshotDataStoreVO snapshotStoreRef) {
        SnapshotInfo snapshotInfo = snapshotDataFactory.getSnapshot(snapshotVO.getId(), snapshotStoreRef.getDataStoreId(), DataStoreRole.Primary);
        if (snapshotInfo == null) {
            logger.warn("Unable to find KVM file-based no-copy snapshot [{}] on primary storage [{}]; its file [{}] may be left behind.", snapshotVO.getUuid(),
                    snapshotStoreRef.getDataStoreId(), snapshotStoreRef.getInstallPath());
            return;
        }

        EndPoint endPoint = endPointSelector.select(snapshotInfo);
        if (endPoint == null) {
            throw new CloudRuntimeException(String.format("Unable to find a host with access to the storage pool of snapshot [%s] to delete its file.", snapshotVO.getUuid()));
        }

        logger.info("Deleting the file [{}] of KVM file-based no-copy snapshot [{}], as its volume was expunged.", snapshotStoreRef.getInstallPath(), snapshotVO.getUuid());
        Answer answer = sendToHost(endPoint.getId(), new DeleteDiskOnlyVmSnapshotCommand(List.of(snapshotInfo.getTO())));
        if (answer == null || !answer.getResult()) {
            throw new CloudRuntimeException(String.format("Failed to delete the file of KVM file-based no-copy snapshot [%s] due to [%s].", snapshotVO.getUuid(),
                    getAnswerDetails(answer)));
        }
    }

    /**
     * Moves the volume out of Ready, which excludes every other snapshot, migration and attach operation on it until it is released. Fails if the volume is not
     * Ready or if another operation changed its state concurrently ({@link VolumeInfo#stateTransit} returns false when its optimistic update loses).
     */
    protected void reserveVolume(VolumeInfo volumeInfo, Volume.Event event, SnapshotVO snapshotVO) {
        boolean reserved;
        try {
            reserved = volumeInfo.stateTransit(event);
        } catch (CloudRuntimeException e) {
            throw new InvalidParameterValueException(String.format("Snapshot [%s] cannot be used while its volume [%s] is in state [%s]: %s", snapshotVO.getUuid(),
                    volumeInfo.getUuid(), volumeInfo.getState(), e.getMessage()));
        }
        if (!reserved) {
            throw new CloudRuntimeException(String.format("Volume [%s] is busy with another operation; retry the operation on snapshot [%s].", volumeInfo.getUuid(),
                    snapshotVO.getUuid()));
        }
    }

    /**
     * The child of the snapshot is the next snapshot of the volume or, for the most recent snapshot, the current delta of the volume. The child is committed into the
     * snapshot, and the file on top of the child (the grandchild) is rebased onto the snapshot.
     */
    protected DeltaMergeTreeTO createDeltaMergeTree(List<SnapshotVO> chain, int index, VolumeInfo volumeInfo, VolumeObjectTO volumeTO, SnapshotObjectTO parentTO) {
        if (index + 1 >= chain.size()) {
            return new DeltaMergeTreeTO(volumeTO, parentTO, volumeTO, new ArrayList<>());
        }

        DataTO childTO = getSnapshotObjectTO(chain.get(index + 1), volumeInfo);
        List<DataTO> grandChildren = new ArrayList<>();
        grandChildren.add(index + 2 < chain.size() ? getSnapshotObjectTO(chain.get(index + 2), volumeInfo) : volumeTO);
        return new DeltaMergeTreeTO(volumeTO, parentTO, childTO, grandChildren);
    }

    /**
     * After the merge, the file of the deleted snapshot holds the data of its child: the child (the next snapshot, or the volume itself) now points to that file.
     */
    protected void updateMetadataAfterMerge(SnapshotVO snapshotVO, SnapshotDataStoreVO snapshotStoreRef, SnapshotVO childSnapshot, VolumeVO volumeVO) {
        String mergedPath = snapshotStoreRef.getInstallPath();
        if (childSnapshot == null) {
            logger.debug("Volume [{}] is now on [{}].", volumeVO.getUuid(), mergedPath);
            volumeVO.setPath(mergedPath);
            volumeDao.update(volumeVO.getId(), volumeVO);
        } else {
            SnapshotDataStoreVO childStoreRef = snapshotStoreDao.findOneBySnapshotAndDatastoreRole(childSnapshot.getId(), DataStoreRole.Primary);
            logger.debug("Snapshot [{}] is now on [{}].", childSnapshot.getUuid(), mergedPath);
            childStoreRef.setInstallPath(mergedPath);
            childStoreRef.setPhysicalSize(childStoreRef.getPhysicalSize() + snapshotStoreRef.getPhysicalSize());
            snapshotStoreDao.update(childStoreRef.getId(), childStoreRef);
        }
        removeSnapshotMetadata(snapshotVO);
    }

    protected void removeSnapshotMetadata(SnapshotVO snapshotVO) {
        List<SnapshotDataStoreVO> storeRefs = snapshotStoreDao.findBySnapshotId(snapshotVO.getId());
        for (SnapshotDataStoreVO storeRef : storeRefs) {
            snapshotStoreDao.expunge(storeRef.getId());
        }
        snapshotZoneDao.removeSnapshotFromZones(snapshotVO.getId());
        snapshotVO.setState(Snapshot.State.Destroyed);
        snapshotDao.update(snapshotVO.getId(), snapshotVO);
    }

    @Override
    public SnapshotInfo backupSnapshot(SnapshotInfo snapshot) {
        throw new CloudRuntimeException(String.format("Snapshot [%s] is a KVM file-based no-copy snapshot, which cannot be backed up to secondary storage.",
                snapshot.getUuid()));
    }

    @Override
    public void postSnapshotCreation(SnapshotInfo snapshot) {
    }

    /**
     * @return the KVM file-based no-copy snapshots of the volume that are part of its backing chain, from the oldest (the bottom of the chain) to the most recent.
     */
    protected List<SnapshotVO> listNoCopySnapshotsOfVolume(long volumeId) {
        return snapshotDao.listByVolumeId(volumeId).stream()
                .filter(snapshot -> Snapshot.State.BackedUp.equals(snapshot.getState()) || Snapshot.State.CreatedOnPrimary.equals(snapshot.getState()))
                .filter(snapshot -> snapshotManager.isKvmFileBasedNoCopySnapshot(snapshot.getId()))
                .sorted(Comparator.comparingLong(this::getChainSequence).thenComparingLong(SnapshotVO::getId))
                .collect(Collectors.toList());
    }

    /**
     * @return the position of the snapshot in the backing chain of its volume, recorded when it was taken.
     */
    protected long getChainSequence(SnapshotVO snapshotVO) {
        SnapshotDetailsVO detail = snapshotDetailsDao.findDetail(snapshotVO.getId(), KVM_FILE_BASED_NO_COPY_SNAPSHOT_SEQUENCE);
        if (detail == null) {
            return snapshotVO.getId();
        }
        try {
            return Long.parseLong(detail.getValue());
        } catch (NumberFormatException e) {
            return snapshotVO.getId();
        }
    }

    protected SnapshotObjectTO getSnapshotObjectTO(SnapshotVO snapshotVO, VolumeInfo volumeInfo) {
        SnapshotInfo snapshotInfo = snapshotDataFactory.getSnapshot(snapshotVO.getId(), volumeInfo.getPoolId(), DataStoreRole.Primary);
        if (snapshotInfo == null) {
            throw new CloudRuntimeException(String.format("Unable to find snapshot [%s] on the primary storage of volume [%s].", snapshotVO.getUuid(), volumeInfo.getUuid()));
        }
        SnapshotObjectTO snapshotObjectTO = (SnapshotObjectTO) snapshotInfo.getTO();
        snapshotObjectTO.setVolume((VolumeObjectTO) volumeInfo.getTO());
        return snapshotObjectTO;
    }

    protected VMInstanceVO getAttachedVm(VolumeInfo volumeInfo) {
        return volumeInfo.getInstanceId() == null ? null : vmInstanceDao.findById(volumeInfo.getInstanceId());
    }

    protected boolean isVmRunning(VMInstanceVO vm) {
        return vm != null && VirtualMachine.State.Running.equals(vm.getState());
    }

    /**
     * A running Instance is snapshotted and merged by the host it runs on; otherwise, any host with access to the storage pool of the volume is used.
     */
    protected long getHostIdForOperation(VolumeInfo volumeInfo, VMInstanceVO vm) {
        if (isVmRunning(vm) && vm.getHostId() != null) {
            return vm.getHostId();
        }

        EndPoint endPoint = endPointSelector.select(volumeInfo);
        if (endPoint == null) {
            throw new CloudRuntimeException(String.format("Unable to find a host with access to the storage pool of volume [%s].", volumeInfo.getUuid()));
        }
        return endPoint.getId();
    }

    protected Answer sendToHost(long hostId, com.cloud.agent.api.Command command) {
        return agentManager.easySend(hostId, command);
    }

    protected String getAnswerDetails(Answer answer) {
        return answer == null ? "no answer from the host" : answer.getDetails();
    }
}
