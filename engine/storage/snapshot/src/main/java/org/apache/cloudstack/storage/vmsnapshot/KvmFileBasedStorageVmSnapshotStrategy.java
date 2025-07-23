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
package org.apache.cloudstack.storage.vmsnapshot;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.agent.api.storage.CreateDiskOnlyVmSnapshotAnswer;
import com.cloud.agent.api.storage.CreateDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.storage.DeleteDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.storage.MergeDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.storage.RevertDiskOnlyVmSnapshotAnswer;
import com.cloud.agent.api.storage.RevertDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.storage.SnapshotMergeTreeTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.configuration.Resource;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.uservm.UserVm;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotDetailsVO;
import com.cloud.vm.snapshot.VMSnapshotVO;
import org.apache.cloudstack.backup.BackupOfferingVO;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VMSnapshotOptions;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.snapshot.SnapshotObject;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.commons.collections.CollectionUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

public class KvmFileBasedStorageVmSnapshotStrategy extends StorageVMSnapshotStrategy {

    private static final List<Storage.StoragePoolType> supportedStoragePoolTypes = List.of(Storage.StoragePoolType.Filesystem, Storage.StoragePoolType.NetworkFilesystem, Storage.StoragePoolType.SharedMountPoint);

    @Inject
    protected SnapshotDataStoreDao snapshotDataStoreDao;

    @Inject
    protected ResourceLimitService resourceLimitManager;

    @Inject
    protected BackupOfferingDao backupOfferingDao;

    @Override
    public VMSnapshot takeVMSnapshot(VMSnapshot vmSnapshot) {
        Map<VolumeInfo, SnapshotObject> volumeInfoToSnapshotObjectMap = new HashMap<>();
        try {
            return takeVmSnapshotInternal(vmSnapshot, volumeInfoToSnapshotObjectMap);
        } catch (CloudRuntimeException | NullPointerException | NoTransitionException ex) {
            for (VolumeInfo volumeInfo : volumeInfoToSnapshotObjectMap.keySet()) {
                volumeInfo.stateTransit(Volume.Event.OperationFailed);
                SnapshotObject snapshot = volumeInfoToSnapshotObjectMap.get(volumeInfo);
                try {
                    snapshot.processEvent(Snapshot.Event.OperationFailed);
                } catch (NoTransitionException e) {
                    logger.error("Failed to change snapshot [{}] state due to [{}].", snapshot.getUuid(), e.getMessage(), e);
                }
                snapshot.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
            }
            try {
                vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed);
            } catch (NoTransitionException e) {
                throw new CloudRuntimeException(e);
            }
            throw new CloudRuntimeException(ex);
        }
    }

    @Override
    public boolean deleteVMSnapshot(VMSnapshot vmSnapshot) {
        logger.info("Starting VM snapshot delete process for snapshot [{}].", vmSnapshot.getUuid());
        UserVmVO userVm = userVmDao.findById(vmSnapshot.getVmId());
        VMSnapshotVO vmSnapshotBeingDeleted = (VMSnapshotVO) vmSnapshot;
        Long hostId = vmSnapshotHelper.pickRunningHost(vmSnapshotBeingDeleted.getVmId());
        long virtualSize = 0;
        boolean isCurrent = vmSnapshotBeingDeleted.getCurrent();

        transitStateWithoutThrow(vmSnapshotBeingDeleted, VMSnapshot.Event.ExpungeRequested);

        List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(vmSnapshotBeingDeleted.getVmId());
        List<VMSnapshotVO> snapshotChildren = vmSnapshotDao.listByParentAndStateIn(vmSnapshotBeingDeleted.getId(), VMSnapshot.State.Ready, VMSnapshot.State.Hidden);

        long realSize = getVMSnapshotRealSize(vmSnapshotBeingDeleted);
        int numberOfChildren = snapshotChildren.size();

        List<SnapshotVO> volumeSnapshotVos = new ArrayList<>();
        if (isCurrent && numberOfChildren == 0) {
            volumeSnapshotVos = mergeCurrentDeltaOnSnapshot(vmSnapshotBeingDeleted, userVm, hostId, volumeTOs);
        } else if (numberOfChildren == 0) {
            logger.debug("Deleting VM snapshot [{}] as no snapshots/volumes depend on it.", vmSnapshot.getUuid());
            volumeSnapshotVos = deleteSnapshot(vmSnapshotBeingDeleted, hostId);
            mergeOldSiblingWithOldParentIfOldParentIsDead(vmSnapshotDao.findByIdIncludingRemoved(vmSnapshotBeingDeleted.getParent()), userVm, hostId, volumeTOs);
        } else if (!isCurrent && numberOfChildren == 1) {
            VMSnapshotVO childSnapshot = snapshotChildren.get(0);
            volumeSnapshotVos = mergeSnapshots(vmSnapshotBeingDeleted, childSnapshot, userVm, volumeTOs, hostId);
        }

        for (SnapshotVO snapshotVO : volumeSnapshotVos) {
            snapshotVO.setState(Snapshot.State.Destroyed);
            snapshotDao.update(snapshotVO.getId(), snapshotVO);
        }

        for (VolumeObjectTO volumeTo : volumeTOs) {
            publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_DELETE, vmSnapshotBeingDeleted, userVm, volumeTo);
            virtualSize += volumeTo.getSize();
        }

        publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_OFF_PRIMARY, vmSnapshotBeingDeleted, userVm, realSize, virtualSize);

        if (numberOfChildren > 1 || (isCurrent && numberOfChildren == 1)) {
            transitStateWithoutThrow(vmSnapshotBeingDeleted, VMSnapshot.Event.Hide);
            return true;
        }

        transitStateWithoutThrow(vmSnapshotBeingDeleted, VMSnapshot.Event.OperationSucceeded);

        vmSnapshotDetailsDao.removeDetails(vmSnapshotBeingDeleted.getId());

        vmSnapshotBeingDeleted.setRemoved(DateUtil.now());
        vmSnapshotDao.update(vmSnapshotBeingDeleted.getId(), vmSnapshotBeingDeleted);

        return true;
    }

    @Override
    public boolean revertVMSnapshot(VMSnapshot vmSnapshot) {
        UserVmVO userVm = userVmDao.findById(vmSnapshot.getVmId());
        if (!VirtualMachine.State.Stopped.equals(userVm.getState())) {
            throw new CloudRuntimeException("VM must be stopped to revert disk-only VM snapshot.");
        }

        VMSnapshotVO vmSnapshotBeingReverted = (VMSnapshotVO) vmSnapshot;
        Long hostId = vmSnapshotHelper.pickRunningHost(vmSnapshotBeingReverted.getVmId());

        transitStateWithoutThrow(vmSnapshotBeingReverted, VMSnapshot.Event.RevertRequested);

        List<SnapshotDataStoreVO> volumeSnapshots = getVolumeSnapshotsAssociatedWithVmSnapshot(vmSnapshotBeingReverted);
        List<SnapshotObjectTO> volumeSnapshotTos = volumeSnapshots.stream()
                .map(snapshot -> (SnapshotObjectTO) snapshotDataFactory.getSnapshot(snapshot.getSnapshotId(), snapshot.getDataStoreId(), DataStoreRole.Primary).getTO())
                .collect(Collectors.toList());

        RevertDiskOnlyVmSnapshotCommand revertDiskOnlyVMSnapshotCommand = new RevertDiskOnlyVmSnapshotCommand(volumeSnapshotTos, userVm.getName());
        Answer answer = agentMgr.easySend(hostId, revertDiskOnlyVMSnapshotCommand);

        if (answer == null || !answer.getResult()) {
            transitStateWithoutThrow(vmSnapshotBeingReverted, VMSnapshot.Event.OperationFailed);
            logger.error(answer != null ? answer.getDetails() : String.format("Communication failure with host [%s].", hostId));
            throw new CloudRuntimeException(String.format("Error reverting VM snapshot [%s].", vmSnapshot.getUuid()));
        }

        RevertDiskOnlyVmSnapshotAnswer revertDiskOnlyVMSnapshotAnswer = (RevertDiskOnlyVmSnapshotAnswer) answer;

        for (VolumeObjectTO volumeObjectTo : revertDiskOnlyVMSnapshotAnswer.getVolumeObjectTos()) {
            VolumeVO volumeVO = volumeDao.findById(volumeObjectTo.getVolumeId());
            volumeVO.setPath(volumeObjectTo.getPath());
            updateSizeIfNeeded(volumeSnapshots, volumeVO, volumeObjectTo);

            volumeDao.update(volumeVO.getId(), volumeVO);
            publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_REVERT, vmSnapshotBeingReverted, userVm, volumeObjectTo);
        }

        transitStateWithoutThrow(vmSnapshotBeingReverted, VMSnapshot.Event.OperationSucceeded);

        VMSnapshotVO currentVmSnapshot = vmSnapshotDao.findCurrentSnapshotByVmId(userVm.getId());
        currentVmSnapshot.setCurrent(false);
        vmSnapshotBeingReverted.setCurrent(true);

        vmSnapshotDao.update(currentVmSnapshot.getId(), currentVmSnapshot);
        vmSnapshotDao.update(vmSnapshotBeingReverted.getId(), vmSnapshotBeingReverted);

        mergeOldSiblingWithOldParentIfOldParentIsDead(currentVmSnapshot, userVm, hostId, vmSnapshotHelper.getVolumeTOList(userVm.getId()));
        return true;
    }

    /**
     * Updates the volume size if it changed due to the snapshot reversion.
     * */
    private void updateSizeIfNeeded(List<SnapshotDataStoreVO> volumeSnapshots, VolumeVO volumeVO, VolumeObjectTO volumeObjectTO) {
        SnapshotDataStoreVO snapshotRef = volumeSnapshots.stream().filter(snapshotDataStoreVO -> snapshotDataStoreVO.getVolumeId() == volumeVO.getId()).
                findFirst().
                orElseThrow(() -> new CloudRuntimeException(String.format("Unable to map any snapshot to volume [%s].", volumeVO)));

        if (volumeVO.getSize() == snapshotRef.getSize()) {
            logger.debug("No need to update the volume size and launch a resize event as the snapshot [{}] and volume [{}] size are equal.", snapshotRef.getSnapshotId(), volumeVO.getUuid());
            return;
        }

        long delta = volumeVO.getSize() - snapshotRef.getSize();
        if (delta < 0) {
            resourceLimitManager.incrementResourceCount(volumeVO.getAccountId(), Resource.ResourceType.primary_storage, -delta);
        } else {
            resourceLimitManager.decrementResourceCount(volumeVO.getAccountId(), Resource.ResourceType.primary_storage, delta);
        }
        volumeVO.setSize(snapshotRef.getSize());
        volumeObjectTO.setSize(snapshotRef.getSize());
        volumeDao.update(volumeVO.getId(), volumeVO);
        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_RESIZE, volumeVO.getAccountId(), volumeVO.getDataCenterId(), volumeVO.getId(), volumeVO.getName(),
                volumeVO.getDiskOfferingId(), volumeVO.getTemplateId(), volumeVO.getSize(), Volume.class.getName(), volumeVO.getUuid());
    }

    private void mergeOldSiblingWithOldParentIfOldParentIsDead(VMSnapshotVO oldParent, UserVmVO userVm, Long hostId, List<VolumeObjectTO> volumeTOs) {
        if (oldParent == null || oldParent.getRemoved() != null || !VMSnapshot.State.Hidden.equals(oldParent.getState())) {
            return;
        }

        List<SnapshotVO> snapshotVos;

        if (oldParent.getCurrent()) {
            snapshotVos = mergeCurrentDeltaOnSnapshot(oldParent, userVm, hostId, volumeTOs);
        } else {
            List<VMSnapshotVO> oldSiblings = vmSnapshotDao.listByParentAndStateIn(oldParent.getId(), VMSnapshot.State.Ready, VMSnapshot.State.Hidden);

            if (oldSiblings.size() > 1) {
                logger.debug("The old snapshot [{}] is dead and still has more than one live child snapshot. We will keep it on storage still.", oldParent.getUuid());
                return;
            }

            if (oldSiblings.isEmpty()) {
                logger.warn("The old snapshot [{}] is dead, but it only had one child. This is an inconsistency and should be analysed/reported.", oldParent.getUuid());
                return;
            }

            VMSnapshotVO oldSibling = oldSiblings.get(0);
            logger.debug("Merging VM snapshot [{}] with [{}] as the former was hidden and only the latter depends on it.", oldParent.getUuid(), oldSibling.getUuid());

            snapshotVos = mergeSnapshots(oldParent, oldSibling, userVm, volumeTOs, hostId);
        }

        for (SnapshotVO snapshotVO : snapshotVos) {
            snapshotVO.setState(Snapshot.State.Destroyed);
            snapshotDao.update(snapshotVO.getId(), snapshotVO);
        }

        vmSnapshotDetailsDao.removeDetails(oldParent.getId());

        oldParent.setRemoved(DateUtil.now());
        vmSnapshotDao.update(oldParent.getId(), oldParent);

        transitStateWithoutThrow(oldParent, VMSnapshot.Event.ExpungeRequested);
        transitStateWithoutThrow(oldParent, VMSnapshot.Event.OperationSucceeded);
    }

    @Override
    public StrategyPriority canHandle(VMSnapshot vmSnapshot) {
        if (!VMSnapshot.State.Allocated.equals(vmSnapshot.getState())) {
            List<VMSnapshotDetailsVO> vmSnapshotDetails = vmSnapshotDetailsDao.findDetails(vmSnapshot.getId(), KVM_FILE_BASED_STORAGE_SNAPSHOT);
            if (CollectionUtils.isEmpty(vmSnapshotDetails)) {
                logger.debug("KVM file based storage VM snapshot strategy cannot handle [{}] as it is not a KVM file based storage VM snapshot.",
                        vmSnapshot.getUuid());
                return StrategyPriority.CANT_HANDLE;
            }
            return StrategyPriority.HIGHEST;
        }

        long vmId = vmSnapshot.getVmId();
        boolean memorySnapshot = VMSnapshot.Type.DiskAndMemory.equals(vmSnapshot.getType());
        return canHandle(vmId, null, memorySnapshot);
    }

    @Override
    public StrategyPriority canHandle(Long vmId, Long rootPoolId, boolean snapshotMemory) {
        VirtualMachine vm = userVmDao.findById(vmId);

        String cantHandleLog = String.format("KVM file based storage VM snapshot strategy cannot handle VM snapshot for [%s]", vm);
        if (snapshotMemory) {
            logger.debug("{} as a snapshot with memory was requested.", cantHandleLog);
            return StrategyPriority.CANT_HANDLE;
        }

        if (!Hypervisor.HypervisorType.KVM.equals(vm.getHypervisorType())) {
            logger.debug("{} as the hypervisor is not KVM.", cantHandleLog);
            return StrategyPriority.CANT_HANDLE;
        }

        if (CollectionUtils.isNotEmpty(vmSnapshotDao.findByVmAndByType(vmId, VMSnapshot.Type.DiskAndMemory))) {
            logger.debug("{} as there is already a VM snapshot with disk and memory.", cantHandleLog);
            return StrategyPriority.CANT_HANDLE;
        }

        List<VolumeVO> volumes = volumeDao.findByInstance(vmId);
        for (VolumeVO volume : volumes) {
            StoragePoolVO storagePoolVO = storagePool.findById(volume.getPoolId());
            if (!supportedStoragePoolTypes.contains(storagePoolVO.getPoolType())) {
                logger.debug(String.format("%s as the VM has a volume that is in a storage with unsupported type [%s].", cantHandleLog, storagePoolVO.getPoolType()));
                return StrategyPriority.CANT_HANDLE;
            }
            List<SnapshotVO> snapshots = snapshotDao.listByVolumeIdAndTypeNotInAndStateNotRemoved(volume.getId(), Snapshot.Type.GROUP);
            if (CollectionUtils.isNotEmpty(snapshots)) {
                logger.debug("{} as VM has a volume with snapshots {}. Volume snapshots and KvmFileBasedStorageVmSnapshotStrategy are not compatible, as restoring volume snapshots will erase VM " +
                        "snapshots and cause data loss.", cantHandleLog, snapshots);
                return StrategyPriority.CANT_HANDLE;
            }
        }

        BackupOfferingVO backupOffering = backupOfferingDao.findById(vm.getBackupOfferingId());
        if (backupOffering != null) {
            logger.debug("{} as the VM has a backup offering. This strategy does not support snapshots on VMs with current backup providers.", cantHandleLog);
            return StrategyPriority.CANT_HANDLE;
        }

        return StrategyPriority.HIGHEST;
    }

    private List<SnapshotVO> deleteSnapshot(VMSnapshotVO vmSnapshotVO, Long hostId) {
        List<SnapshotDataStoreVO> volumeSnapshots = getVolumeSnapshotsAssociatedWithVmSnapshot(vmSnapshotVO);
        List<DataTO> volumeSnapshotTOList = volumeSnapshots.stream()
                .map(snapshotDataStoreVO -> snapshotDataFactory.getSnapshot(snapshotDataStoreVO.getSnapshotId(), snapshotDataStoreVO.getDataStoreId(), DataStoreRole.Primary).getTO())
                .collect(Collectors.toList());

        DeleteDiskOnlyVmSnapshotCommand deleteSnapshotCommand = new DeleteDiskOnlyVmSnapshotCommand(volumeSnapshotTOList);
        Answer answer = agentMgr.easySend(hostId, deleteSnapshotCommand);
        if (answer == null || !answer.getResult()) {
            logger.error("Failed to delete VM snapshot [{}] due to {}.", vmSnapshotVO.getUuid(), answer != null ? answer.getDetails() : "Communication failure");
            throw new CloudRuntimeException(String.format("Failed to delete VM snapshot [%s].", vmSnapshotVO.getUuid()));
        }

        logger.debug("Updating metadata of VM snapshot [{}].", vmSnapshotVO.getUuid());
        List<SnapshotVO> snapshotVOList = new ArrayList<>();
        for (SnapshotDataStoreVO snapshotDataStoreVO : volumeSnapshots) {
            snapshotDataStoreDao.remove(snapshotDataStoreVO.getId());
            snapshotVOList.add(snapshotDao.findById(snapshotDataStoreVO.getSnapshotId()));
        }
        return snapshotVOList;
    }

    private List<SnapshotVO> mergeSnapshots(VMSnapshotVO vmSnapshotVO, VMSnapshotVO childSnapshot, UserVmVO userVm, List<VolumeObjectTO> volumeObjectTOS, Long hostId) {
        logger.debug("Merging VM snapshot [{}] with its child [{}].", vmSnapshotVO.getUuid(), childSnapshot.getUuid());

        List<VMSnapshotVO> snapshotGrandChildren = vmSnapshotDao.listByParentAndStateIn(childSnapshot.getId(), VMSnapshot.State.Ready, VMSnapshot.State.Hidden);

        if (VirtualMachine.State.Running.equals(userVm.getState()) && !snapshotGrandChildren.isEmpty()) {
            logger.debug("Removing VM snapshots that are part of the VM's [{}] current backing chain from the list of snapshots to be rebased.", userVm.getUuid());
            removeCurrentBackingChainSnapshotFromVmSnapshotList(snapshotGrandChildren, userVm);
        }

        List<SnapshotMergeTreeTO> snapshotMergeTreeToList = generateSnapshotMergeTrees(vmSnapshotVO, childSnapshot, snapshotGrandChildren);

        if (childSnapshot.getCurrent() && !VirtualMachine.State.Running.equals(userVm.getState())) {
            for (VolumeObjectTO volumeObjectTO : volumeObjectTOS) {
                snapshotMergeTreeToList.stream().filter(snapshotTree -> Objects.equals(((SnapshotObjectTO) snapshotTree.getParent()).getVolume().getId(), volumeObjectTO.getId()))
                        .findFirst()
                        .orElseThrow(() -> new CloudRuntimeException(String.format("Failed to find volume snapshot for volume [%s].", volumeObjectTO.getUuid())))
                        .addGrandChild(volumeObjectTO);
            }
        }

        MergeDiskOnlyVmSnapshotCommand mergeDiskOnlyVMSnapshotCommand = new MergeDiskOnlyVmSnapshotCommand(snapshotMergeTreeToList, userVm.getState(), userVm.getName());
        Answer answer = agentMgr.easySend(hostId, mergeDiskOnlyVMSnapshotCommand);
        if (answer == null || !answer.getResult()) {
            throw new CloudRuntimeException(String.format("Failed to merge VM snapshot [%s] due to %s.", vmSnapshotVO.getUuid(), answer != null ? answer.getDetails() : "Communication failure"));
        }

        logger.debug("Updating metadata of VM snapshot [{}] and its child [{}].", vmSnapshotVO.getUuid(), childSnapshot.getUuid());
        List<SnapshotVO> snapshotVOList = new ArrayList<>();
        for (SnapshotMergeTreeTO snapshotMergeTreeTO : snapshotMergeTreeToList) {
            SnapshotObjectTO childTO = (SnapshotObjectTO) snapshotMergeTreeTO.getChild();
            SnapshotObjectTO parentTO = (SnapshotObjectTO) snapshotMergeTreeTO.getParent();

            SnapshotDataStoreVO childSnapshotDataStoreVO = snapshotDataStoreDao.findBySnapshotIdInAnyState(childTO.getId(), DataStoreRole.Primary);
            childSnapshotDataStoreVO.setInstallPath(parentTO.getPath());
            snapshotDataStoreDao.update(childSnapshotDataStoreVO.getId(), childSnapshotDataStoreVO);

            snapshotDataStoreDao.expungeReferenceBySnapshotIdAndDataStoreRole(parentTO.getId(), childSnapshotDataStoreVO.getDataStoreId(), DataStoreRole.Primary);
            snapshotVOList.add(snapshotDao.findById(parentTO.getId()));
        }

        childSnapshot.setParent(vmSnapshotVO.getParent());
        vmSnapshotDao.update(childSnapshot.getId(), childSnapshot);

        return snapshotVOList;
    }

    private List<SnapshotVO> mergeCurrentDeltaOnSnapshot(VMSnapshotVO vmSnapshotVo, UserVmVO userVmVO, Long hostId, List<VolumeObjectTO> volumeObjectTOS) {
        logger.debug("Merging VM snapshot [{}] with the current volume delta.", vmSnapshotVo.getUuid());
        List<SnapshotMergeTreeTO> snapshotMergeTreeTOList = new ArrayList<>();
        List<SnapshotDataStoreVO> volumeSnapshots = getVolumeSnapshotsAssociatedWithVmSnapshot(vmSnapshotVo);

        for (VolumeObjectTO volumeObjectTO : volumeObjectTOS) {
            SnapshotDataStoreVO volumeParentSnapshot = volumeSnapshots.stream().filter(snapshot -> Objects.equals(snapshot.getVolumeId(), volumeObjectTO.getId()))
                    .findFirst()
                    .orElseThrow(() -> new CloudRuntimeException(String.format("Failed to find volume snapshot for volume [%s].", volumeObjectTO.getUuid())));
            DataTO parentSnapshot = snapshotDataFactory.getSnapshot(volumeParentSnapshot.getSnapshotId(), volumeParentSnapshot.getDataStoreId(), DataStoreRole.Primary).getTO();
            snapshotMergeTreeTOList.add(new SnapshotMergeTreeTO(parentSnapshot, volumeObjectTO, new ArrayList<>()));
        }

        MergeDiskOnlyVmSnapshotCommand mergeDiskOnlyVMSnapshotCommand = new MergeDiskOnlyVmSnapshotCommand(snapshotMergeTreeTOList, userVmVO.getState(), userVmVO.getName());

        Answer answer = agentMgr.easySend(hostId, mergeDiskOnlyVMSnapshotCommand);
        if (answer == null || !answer.getResult()) {
            throw new CloudRuntimeException(String.format("Failed to delete VM snapshot [%s] due to %s.", vmSnapshotVo.getUuid(), answer != null ? answer.getDetails() : "Communication failure"));
        }

        logger.debug("Updating metadata of VM snapshot [{}].", vmSnapshotVo.getUuid());
        List<SnapshotVO> snapshotVOList = new ArrayList<>();
        for (SnapshotMergeTreeTO snapshotMergeTreeTO : snapshotMergeTreeTOList) {
            VolumeObjectTO volumeObjectTO = (VolumeObjectTO) snapshotMergeTreeTO.getChild();
            SnapshotObjectTO parentTO = (SnapshotObjectTO) snapshotMergeTreeTO.getParent();

            VolumeVO volumeVO = volumeDao.findById(volumeObjectTO.getId());
            volumeVO.setPath(parentTO.getPath());
            volumeDao.update(volumeVO.getId(), volumeVO);

            snapshotDataStoreDao.expungeReferenceBySnapshotIdAndDataStoreRole(parentTO.getId(), volumeVO.getPoolId(), DataStoreRole.Primary);
            snapshotVOList.add(snapshotDao.findById(parentTO.getId()));
        }

        vmSnapshotVo.setCurrent(false);
        if (vmSnapshotVo.getParent() != null) {
            VMSnapshotVO parentSnapshot = vmSnapshotDao.findById(vmSnapshotVo.getParent());
            parentSnapshot.setCurrent(true);
            vmSnapshotDao.update(parentSnapshot.getId(), parentSnapshot);
        }

        return snapshotVOList;
    }

    /**
     * Takes a disk-only VM snapshot, exceptions thrown will be caught deeper in the stack and treated there.
     * @param vmSnapshot the definition of the VM Snapshot that will be created.
     * @param volumeInfoToSnapshotObjectMap Empty map of VolumeInfo to SnapshotObject, will be populated within the method, used for treating the exceptions thrown.
     * @return the VM Snapshot created.
     * */
    protected VMSnapshot takeVmSnapshotInternal(VMSnapshot vmSnapshot, Map<VolumeInfo, SnapshotObject> volumeInfoToSnapshotObjectMap) throws NoTransitionException {
        UserVm userVm = userVmDao.findById(vmSnapshot.getVmId());

        logger.info("Starting disk-only VM snapshot process for VM [{}].", userVm.getUuid());

        Long hostId = vmSnapshotHelper.pickRunningHost(vmSnapshot.getVmId());
        VMSnapshotVO vmSnapshotVO = (VMSnapshotVO) vmSnapshot;
        List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(userVm.getId());

        transitStateWithoutThrow(vmSnapshot, VMSnapshot.Event.CreateRequested);

        VMSnapshotTO parentSnapshotTo = null;
        VMSnapshotVO parentSnapshotVo = vmSnapshotDao.findCurrentSnapshotByVmId(userVm.getId());
        if (parentSnapshotVo != null) {
            parentSnapshotTo = vmSnapshotHelper.getSnapshotWithParents(parentSnapshotVo);
            vmSnapshotVO.setParent(parentSnapshotTo.getId());
        }

        VMSnapshotOptions options = ((VMSnapshotVO) vmSnapshot).getOptions();
        boolean quiesceVm = false;
        if (options != null) {
            quiesceVm = options.needQuiesceVM();
        }

        long virtualSize = createVolumeSnapshotMetadataAndCalculateVirtualSize(vmSnapshot, volumeInfoToSnapshotObjectMap, volumeTOs);

        VMSnapshotTO target = new VMSnapshotTO(vmSnapshot.getId(), vmSnapshot.getName(), vmSnapshot.getType(), null, vmSnapshot.getDescription(), false, parentSnapshotTo, quiesceVm);

        CreateDiskOnlyVmSnapshotCommand ccmd = new CreateDiskOnlyVmSnapshotCommand(userVm.getInstanceName(), target, volumeTOs, null, userVm.getState());

        logger.info("Sending disk-only VM snapshot creation of VM Snapshot [{}] command for host [{}].", vmSnapshot.getUuid(), hostId);
        Answer answer = agentMgr.easySend(hostId, ccmd);

        if (answer != null && answer.getResult()) {
            CreateDiskOnlyVmSnapshotAnswer createDiskOnlyVMSnapshotAnswer = (CreateDiskOnlyVmSnapshotAnswer) answer;
            return processCreateVmSnapshotAnswer(vmSnapshot, volumeInfoToSnapshotObjectMap, createDiskOnlyVMSnapshotAnswer, userVm, vmSnapshotVO, virtualSize, parentSnapshotVo);
        }

        logger.error("Disk-only VM snapshot for VM [{}] failed{}.", userVm.getUuid(), answer != null ? " due to" + answer.getDetails() : "");
        throw new CloudRuntimeException(String.format("Disk-only VM snapshot for VM [%s] failed.", userVm.getUuid()));
    }

    /**
     * Updates the needed metadata of the given VM Snapshot and its associated volume snapshots.
     * */
    private VMSnapshot processCreateVmSnapshotAnswer(VMSnapshot vmSnapshot, Map<VolumeInfo, SnapshotObject> volumeInfoToSnapshotObjectMap, CreateDiskOnlyVmSnapshotAnswer answer, UserVm userVm, VMSnapshotVO vmSnapshotVO, long virtualSize, VMSnapshotVO parentSnapshotVo) throws NoTransitionException {
        logger.debug("Processing CreateDiskOnlyVMSnapshotCommand answer for disk-only VM snapshot [{}].", vmSnapshot.getUuid());
        Map<String, Pair<Long, String>> volumeUuidToSnapshotSizeAndNewVolumePathMap = answer.getMapVolumeToSnapshotSizeAndNewVolumePath();
        long vmSnapshotSize = 0;

        for (VolumeInfo volumeInfo : volumeInfoToSnapshotObjectMap.keySet()) {
            VolumeVO volumeVO = (VolumeVO) volumeInfo.getVolume();
            Pair<Long, String> snapSizeAndNewVolumePath = volumeUuidToSnapshotSizeAndNewVolumePathMap.get(volumeVO.getUuid());

            SnapshotObject snapshot = volumeInfoToSnapshotObjectMap.get(volumeInfo);
            snapshot.markBackedUp();

            logger.debug("Updating metadata for volume [{}] and its corresponding snapshot [{}].", volumeVO, snapshot.getSnapshotVO());

            SnapshotDataStoreVO snapshotDataStoreVO = snapshotDataStoreDao.findBySnapshotId(snapshot.getId()).get(0);
            snapshotDataStoreVO.setInstallPath(volumeVO.getPath());
            snapshotDataStoreVO.setPhysicalSize(snapSizeAndNewVolumePath.first());
            snapshotDataStoreVO.setState(ObjectInDataStoreStateMachine.State.Ready);
            snapshotDataStoreDao.update(snapshotDataStoreVO.getId(), snapshotDataStoreVO);

            vmSnapshotSize += snapSizeAndNewVolumePath.first();

            volumeVO.setPath(snapSizeAndNewVolumePath.second());
            volumeDao.update(volumeVO.getId(), volumeVO);
            volumeInfo.stateTransit(Volume.Event.OperationSucceeded);

            vmSnapshotDetailsDao.persist(new VMSnapshotDetailsVO(vmSnapshot.getId(), KVM_FILE_BASED_STORAGE_SNAPSHOT, String.valueOf(snapshot.getId()), true));

            publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_CREATE, vmSnapshot, userVm, (VolumeObjectTO) volumeInfo.getTO());
        }

        vmSnapshotVO.setCurrent(true);
        vmSnapshotDao.persist(vmSnapshotVO);

        if (parentSnapshotVo != null) {
            parentSnapshotVo.setCurrent(false);
            vmSnapshotDao.persist(parentSnapshotVo);
        }

        vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationSucceeded);

        publishUsageEvent(EventTypes.EVENT_VM_SNAPSHOT_ON_PRIMARY, vmSnapshot, userVm, vmSnapshotSize, virtualSize);

        return vmSnapshot;
    }

    private long createVolumeSnapshotMetadataAndCalculateVirtualSize(VMSnapshot vmSnapshot, Map<VolumeInfo, SnapshotObject> volumeInfoToSnapshotObjectMap, List<VolumeObjectTO> volumeTOs) throws NoTransitionException {
        long virtualSize = 0;
        for (VolumeObjectTO volumeObjectTO : volumeTOs) {
            VolumeInfo volumeInfo = volumeDataFactory.getVolume(volumeObjectTO.getId());
            volumeInfo.stateTransit(Volume.Event.SnapshotRequested);
            virtualSize += volumeInfo.getSize();

            String snapshotName = String.format("%s_%s", vmSnapshot.getId(), volumeObjectTO.getUuid());
            SnapshotVO snapshot = new SnapshotVO(volumeInfo.getDataCenterId(), volumeInfo.getAccountId(), volumeInfo.getDomainId(), volumeInfo.getId(),
                    volumeInfo.getDiskOfferingId(), snapshotName, (short) Snapshot.Type.GROUP.ordinal(), Snapshot.Type.GROUP.name(), volumeInfo.getSize(), volumeInfo.getMinIops(),
                    volumeInfo.getMaxIops(), Hypervisor.HypervisorType.KVM, null);

            logger.debug("Creating snapshot metadata [{}] as part of the disk-only snapshot process for VM [{}].", snapshot, volumeObjectTO.getVmName());

            snapshot = snapshotDao.persist(snapshot);

            SnapshotInfo snapshotInfo = snapshotDataFactory.getSnapshot(snapshot.getId(), volumeInfo.getDataStore());
            SnapshotObject snapshotOnPrimary = (SnapshotObject) snapshotInfo.getDataStore().create(snapshotInfo);

            snapshotOnPrimary.processEvent(Snapshot.Event.CreateRequested);
            snapshotOnPrimary.processEvent(ObjectInDataStoreStateMachine.Event.CreateOnlyRequested);

            volumeInfoToSnapshotObjectMap.put(volumeInfo, snapshotOnPrimary);
        }
        return virtualSize;
    }

    private List<SnapshotMergeTreeTO> generateSnapshotMergeTrees(VMSnapshotVO parent, VMSnapshotVO child, List<VMSnapshotVO> grandChildren) throws NoSuchElementException {
        logger.debug("Generating list of Snapshot Merge Trees for the merge process of VM Snapshot [{}].", parent.getUuid());

        List<SnapshotMergeTreeTO> snapshotMergeTrees = new ArrayList<>();
        List<SnapshotDataStoreVO> parentVolumeSnapshots = getVolumeSnapshotsAssociatedWithVmSnapshot(parent);
        List<SnapshotDataStoreVO> childVolumeSnapshots = getVolumeSnapshotsAssociatedWithVmSnapshot(child);
        List<SnapshotDataStoreVO> grandChildrenVolumeSnapshots = new ArrayList<>();

        for (VMSnapshotVO grandChild : grandChildren) {
            grandChildrenVolumeSnapshots.addAll(getVolumeSnapshotsAssociatedWithVmSnapshot(grandChild));
        }

        for (SnapshotDataStoreVO parentSnapshotDataStoreVO : parentVolumeSnapshots) {
            DataTO parentTO = snapshotDataFactory.getSnapshot(parentSnapshotDataStoreVO.getSnapshotId(), parentSnapshotDataStoreVO.getDataStoreId(), DataStoreRole.Primary).getTO();

            DataTO childTO = childVolumeSnapshots.stream()
                    .filter(childSnapshot -> Objects.equals(parentSnapshotDataStoreVO.getVolumeId(), childSnapshot.getVolumeId()))
                    .map(snapshotDataStoreVO -> snapshotDataFactory.getSnapshot(snapshotDataStoreVO.getSnapshotId(), snapshotDataStoreVO.getDataStoreId(), DataStoreRole.Primary).getTO())
                    .findFirst().orElseThrow(() -> new CloudRuntimeException(String.format("Could not find child snapshot of parent [%s].", parentSnapshotDataStoreVO.getSnapshotId())));

            List<DataTO> grandChildrenTOList = grandChildrenVolumeSnapshots.stream()
                    .filter(grandChildSnapshot -> Objects.equals(parentSnapshotDataStoreVO.getVolumeId(), grandChildSnapshot.getVolumeId()))
                    .map(snapshotDataStoreVO -> snapshotDataFactory.getSnapshot(snapshotDataStoreVO.getSnapshotId(), snapshotDataStoreVO.getDataStoreId(), DataStoreRole.Primary).getTO())
                    .collect(Collectors.toList());

            snapshotMergeTrees.add(new SnapshotMergeTreeTO(parentTO, childTO, grandChildrenTOList));
        }

        logger.debug("Generated the following list of Snapshot Merge Trees for the VM snapshot [{}]: [{}].", parent.getUuid(), snapshotMergeTrees);
        return snapshotMergeTrees;
    }

    /**
     * For a given {@code VMSnapshotVO}, populates the {@code associatedVolumeSnapshots} list with all the volume snapshots that are
     * part of the VMSnapshot.
     * @param vmSnapshot the VMSnapshotVO that will have its size calculated
     * @return the list that will be populated with the volume snapshots associated with the VM snapshot.
     * */
    private List<SnapshotDataStoreVO> getVolumeSnapshotsAssociatedWithVmSnapshot(VMSnapshotVO vmSnapshot) {
        List<SnapshotDataStoreVO> associatedVolumeSnapshots = new ArrayList<>();
        List<VMSnapshotDetailsVO> snapshotDetailList = vmSnapshotDetailsDao.findDetails(vmSnapshot.getId(), KVM_FILE_BASED_STORAGE_SNAPSHOT);
        for (VMSnapshotDetailsVO vmSnapshotDetailsVO : snapshotDetailList) {
            SnapshotDataStoreVO snapshot = snapshotDataStoreDao.findOneBySnapshotAndDatastoreRole(Long.parseLong(vmSnapshotDetailsVO.getValue()), DataStoreRole.Primary);
            if (snapshot == null) {
                throw new CloudRuntimeException(String.format("Could not find snapshot for VM snapshot [%s].", vmSnapshot.getUuid()));
            }
            associatedVolumeSnapshots.add(snapshot);
        }
        return associatedVolumeSnapshots;
    }

    /**
     * For a given {@code VMSnapshotVO}, returns the real size of the snapshot.
     * @param vmSnapshot the VMSnapshotVO that will have its size calculated
     * */
    private long getVMSnapshotRealSize(VMSnapshotVO vmSnapshot) {
        long realSize = 0;
        List<VMSnapshotDetailsVO> snapshotDetailList = vmSnapshotDetailsDao.findDetails(vmSnapshot.getId(), KVM_FILE_BASED_STORAGE_SNAPSHOT);
        for (VMSnapshotDetailsVO vmSnapshotDetailsVO : snapshotDetailList) {
            SnapshotDataStoreVO snapshot = snapshotDataStoreDao.findOneBySnapshotAndDatastoreRole(Long.parseLong(vmSnapshotDetailsVO.getValue()), DataStoreRole.Primary);
            if (snapshot == null) {
                throw new CloudRuntimeException(String.format("Could not find snapshot for VM snapshot [%s].", vmSnapshot.getUuid()));
            }
            realSize += snapshot.getPhysicalSize();
        }
        return realSize;
    }

    /**
     * Given a list of VM snapshots, will remove any that are part of the current direct backing chain (all the direct ancestors of the current vm snapshot).
     * This is done because, when using <a href="https://libvirt.org/html/libvirt-libvirt-domain.html#virDomainBlockCommit">virDomainBlockCommit</a>}, Libvirt will maintain
     * the current backing chain consistent; thus we only need to rebase the snapshots that are not on the current backing chain.
     * */
    private void removeCurrentBackingChainSnapshotFromVmSnapshotList(List<VMSnapshotVO> vmSnapshotList, UserVm userVm) {
        VMSnapshotVO currentSnapshotVO = vmSnapshotDao.findCurrentSnapshotByVmId(vmSnapshotList.get(0).getVmId());
        VMSnapshotTO currentSnapshotTO = vmSnapshotHelper.getSnapshotWithParents(currentSnapshotVO);

        List<VMSnapshotTO> currentBranch = new ArrayList<>();
        currentBranch.add(currentSnapshotTO);
        VMSnapshotTO parent = currentSnapshotTO.getParent();
        while (parent != null) {
            currentBranch.add(parent);
            parent = parent.getParent();
        }

        for (VMSnapshotVO vmSnapshotVO : vmSnapshotList) {
            if (currentBranch.stream().anyMatch(currentBranchSnap -> Objects.equals(currentBranchSnap.getId(), vmSnapshotVO.getId()))) {
                logger.trace("Removing snapshot [{}] from the list of VM snapshots of VM [{}] being rebased.", vmSnapshotVO.getUuid(), userVm.getUuid());
                vmSnapshotList.remove(vmSnapshotVO);
                return;
            }
        }
    }

    private void transitStateWithoutThrow(VMSnapshot vmSnapshot, VMSnapshot.Event event) {
        try {
            vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, event);
        } catch (NoTransitionException e) {
            String msg = String.format("Failed to change VM snapshot [%s] state with event [%s].", vmSnapshot, event.toString());
            logger.error(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
    }
}
