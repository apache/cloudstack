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
package org.apache.cloudstack.storage.snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotResult;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.storage.command.SnapshotAndCopyAnswer;
import org.apache.cloudstack.storage.command.SnapshotAndCopyCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyTargetsCommand;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.resource.ResourceState;
import com.cloud.server.ManagementService;
import com.cloud.storage.CreateSnapshotPayload;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotService;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.google.common.base.Preconditions;

@Component
public class StorageSystemSnapshotStrategy extends SnapshotStrategyBase {
    private static final Logger s_logger = Logger.getLogger(StorageSystemSnapshotStrategy.class);

    @Inject private AgentManager agentMgr;
    @Inject private ClusterDao clusterDao;
    @Inject private DataStoreManager dataStoreMgr;
    @Inject private HostDao hostDao;
    @Inject private ManagementService mgr;
    @Inject private PrimaryDataStoreDao storagePoolDao;
    @Inject private SnapshotDao snapshotDao;
    @Inject private SnapshotDataFactory snapshotDataFactory;
    @Inject private SnapshotDetailsDao snapshotDetailsDao;
    @Inject private SnapshotDataStoreDao snapshotStoreDao;
    @Inject private VMInstanceDao vmInstanceDao;
    @Inject private VMSnapshotDao vmSnapshotDao;
    @Inject private VMSnapshotService vmSnapshotService;
    @Inject private VolumeDao volumeDao;
    @Inject private VolumeService volService;
    @Inject private VolumeDetailsDao volumeDetailsDaoImpl;

    @Override
    public SnapshotInfo backupSnapshot(SnapshotInfo snapshotInfo) {
        Preconditions.checkArgument(snapshotInfo != null, "'snapshotInfo' cannot be 'null'.");

        if (snapshotInfo.getLocationType() != Snapshot.LocationType.SECONDARY) {
            markAsBackedUp((SnapshotObject)snapshotInfo);

            return snapshotInfo;
        }

        // At this point, the snapshot is either taken as a native
        // snapshot on the storage or exists as a volume on the storage (clone).
        // If archive flag is passed in, we should copy this snapshot to secondary
        // storage and delete it from primary storage.

        HostVO host = getHost(snapshotInfo.getVolumeId());

        boolean canStorageSystemCreateVolumeFromSnapshot = canStorageSystemCreateVolumeFromSnapshot(snapshotInfo.getBaseVolume().getPoolId());

        if (!canStorageSystemCreateVolumeFromSnapshot) {
            String msg = "Cannot archive snapshot: 'canStorageSystemCreateVolumeFromSnapshot' was false.";

            s_logger.warn(msg);

            throw new CloudRuntimeException(msg);
        }

        boolean computeClusterSupportsResign = clusterDao.getSupportsResigning(host.getClusterId());

        if (!computeClusterSupportsResign) {
            String msg = "Cannot archive snapshot: 'computeClusterSupportsResign' was false.";

            s_logger.warn(msg);

            throw new CloudRuntimeException(msg);
        }

        return snapshotSvr.backupSnapshot(snapshotInfo);
    }

    @Override
    public boolean deleteSnapshot(Long snapshotId) {
        Preconditions.checkArgument(snapshotId != null, "'snapshotId' cannot be 'null'.");

        SnapshotVO snapshotVO = snapshotDao.findById(snapshotId);

        if (Snapshot.State.Destroyed.equals(snapshotVO.getState())) {
            return true;
        }

        if (Snapshot.State.Error.equals(snapshotVO.getState())) {
            snapshotDao.remove(snapshotId);

            return true;
        }

        if (!Snapshot.State.BackedUp.equals(snapshotVO.getState())) {
            throw new InvalidParameterValueException("Unable to delete snapshot '" + snapshotId +
                    "' because it is in the following state: " + snapshotVO.getState());
        }

        return cleanupSnapshotOnPrimaryStore(snapshotId);
    }

    /**
     * This cleans up a snapshot which was taken on a primary store.
     *
     * @param snapshotId: ID of snapshot to be removed
     * @return true if snapshot is removed; else, false
     */
    @ActionEvent(eventType = EventTypes.EVENT_SNAPSHOT_OFF_PRIMARY, eventDescription = "deleting snapshot", async = true)
    private boolean cleanupSnapshotOnPrimaryStore(long snapshotId) {
        SnapshotObject snapshotObj = (SnapshotObject)snapshotDataFactory.getSnapshot(snapshotId, DataStoreRole.Primary);

        if (snapshotObj == null) {
            s_logger.debug("Can't find snapshot; deleting it in DB");

            snapshotDao.remove(snapshotId);

            return true;
        }

        if (ObjectInDataStoreStateMachine.State.Copying.equals(snapshotObj.getStatus())) {
            throw new InvalidParameterValueException("Unable to delete snapshot '" + snapshotId + "' because it is in the copying state");
        }

        try {
            snapshotObj.processEvent(Snapshot.Event.DestroyRequested);

            List<VolumeDetailVO> volumesFromSnapshot = volumeDetailsDaoImpl.findDetails("SNAPSHOT_ID", String.valueOf(snapshotId), null);

            if (volumesFromSnapshot.size() > 0) {
                try {
                    snapshotObj.processEvent(Snapshot.Event.OperationFailed);
                } catch (NoTransitionException e1) {
                    s_logger.debug("Failed to change snapshot state: " + e1.toString());
                }

                throw new InvalidParameterValueException("Unable to perform delete operation, Snapshot with id: " + snapshotId + " is in use  ");
            }
        }
        catch (NoTransitionException e) {
            s_logger.debug("Failed to set the state to destroying: ", e);

            return false;
        }

        try {
            snapshotSvr.deleteSnapshot(snapshotObj);

            snapshotObj.processEvent(Snapshot.Event.OperationSucceeded);

            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_SNAPSHOT_OFF_PRIMARY, snapshotObj.getAccountId(), snapshotObj.getDataCenterId(), snapshotId,
                    snapshotObj.getName(), null, null, 0L, snapshotObj.getClass().getName(), snapshotObj.getUuid());
        }
        catch (Exception e) {
            s_logger.debug("Failed to delete snapshot: ", e);

            try {
                snapshotObj.processEvent(Snapshot.Event.OperationFailed);
            }
            catch (NoTransitionException e1) {
                s_logger.debug("Failed to change snapshot state: " + e.toString());
            }

            return false;
        }

        return true;
    }

    private boolean isAcceptableRevertFormat(VolumeVO volumeVO) {
        return ImageFormat.VHD.equals(volumeVO.getFormat()) || ImageFormat.OVA.equals(volumeVO.getFormat())
                || ImageFormat.QCOW2.equals(volumeVO.getFormat()) || ImageFormat.RAW.equals(volumeVO.getFormat());
    }

    private void verifyFormat(VolumeInfo volumeInfo) {
        ImageFormat imageFormat = volumeInfo.getFormat();

        if (imageFormat != ImageFormat.VHD && imageFormat != ImageFormat.OVA && imageFormat != ImageFormat.QCOW2 && imageFormat != ImageFormat.RAW) {
            throw new CloudRuntimeException("Only the following image types are currently supported: " +
                    ImageFormat.VHD.toString() + ", " + ImageFormat.OVA.toString() + ", " + ImageFormat.QCOW2 + ", and " + ImageFormat.RAW);
        }
    }

    private void verifyDiskTypeAndHypervisor(VolumeInfo volumeInfo) {
        ImageFormat imageFormat = volumeInfo.getFormat();
        Volume.Type volumeType = volumeInfo.getVolumeType();

        if (ImageFormat.OVA.equals(imageFormat) && Volume.Type.ROOT.equals(volumeType)) {
            throw new CloudRuntimeException("The hypervisor type is VMware and the disk type is ROOT. For this situation, " +
                "recover the data on the snapshot by creating a new CloudStack volume from the corresponding volume snapshot.");
        }
    }

    private void verifySnapshotType(SnapshotInfo snapshotInfo) {
        if (snapshotInfo.getHypervisorType() == HypervisorType.KVM && snapshotInfo.getDataStore().getRole() != DataStoreRole.Primary) {
            throw new CloudRuntimeException("For the KVM hypervisor type, you can only revert a volume to a snapshot state if the snapshot " +
                "resides on primary storage. For other snapshot types, create a volume from the snapshot to recover its data.");
        }
    }

    private void verifyLocationType(SnapshotInfo snapshotInfo) {
        VolumeInfo volumeInfo = snapshotInfo.getBaseVolume();

        if (snapshotInfo.getLocationType() == Snapshot.LocationType.SECONDARY && volumeInfo.getFormat() != ImageFormat.VHD) {
            throw new CloudRuntimeException("Only the '" + ImageFormat.VHD + "' image type can be used when 'LocationType' is set to 'SECONDARY'.");
        }
    }

    private boolean getHypervisorRequiresResignature(VolumeInfo volumeInfo) {
        return ImageFormat.VHD.equals(volumeInfo.getFormat()) || ImageFormat.OVA.equals(volumeInfo.getFormat());
    }

    @Override
    public boolean revertSnapshot(SnapshotInfo snapshotInfo) {
        VolumeInfo volumeInfo = snapshotInfo.getBaseVolume();

        verifyFormat(volumeInfo);

        verifyDiskTypeAndHypervisor(volumeInfo);

        verifySnapshotType(snapshotInfo);

        SnapshotDataStoreVO snapshotStore = snapshotStoreDao.findBySnapshot(snapshotInfo.getId(), DataStoreRole.Primary);

        if (snapshotStore != null) {
            long snapshotStoragePoolId = snapshotStore.getDataStoreId();

            if (!volumeInfo.getPoolId().equals(snapshotStoragePoolId)) {
                String errMsg = "Storage pool mismatch";

                s_logger.error(errMsg);

                throw new CloudRuntimeException(errMsg);
            }
        }

        boolean storageSystemSupportsCapability = storageSystemSupportsCapability(volumeInfo.getPoolId(), DataStoreCapabilities.CAN_REVERT_VOLUME_TO_SNAPSHOT.toString());

        if (!storageSystemSupportsCapability) {
            String errMsg = "Storage pool revert capability not supported";

            s_logger.error(errMsg);

            throw new CloudRuntimeException(errMsg);
        }

        executeRevertSnapshot(snapshotInfo, volumeInfo);

        return true;
    }

    /**
     * Executes the SnapshotStrategyBase.revertSnapshot(SnapshotInfo) method, and handles the SnapshotVO table update and the Volume.Event state machine (RevertSnapshotRequested).
     */
    protected void executeRevertSnapshot(SnapshotInfo snapshotInfo, VolumeInfo volumeInfo) {
        Long hostId = null;
        boolean success = false;

        SnapshotVO snapshotVO = snapshotDao.acquireInLockTable(snapshotInfo.getId());

        if (snapshotVO == null) {
            String errMsg = "Failed to acquire lock on the following snapshot: " + snapshotInfo.getId();

            s_logger.error(errMsg);

            throw new CloudRuntimeException(errMsg);
        }

        try {
            volumeInfo.stateTransit(Volume.Event.RevertSnapshotRequested);

            if (getHypervisorRequiresResignature(volumeInfo)) {
                hostId = getHostId(volumeInfo);

                if (hostId != null) {
                    HostVO hostVO = hostDao.findById(hostId);
                    DataStore dataStore = dataStoreMgr.getDataStore(volumeInfo.getPoolId(), DataStoreRole.Primary);

                    volService.revokeAccess(volumeInfo, hostVO, dataStore);

                    modifyTarget(false, volumeInfo, hostId);
                }
            }

            success = snapshotSvr.revertSnapshot(snapshotInfo);

            if (!success) {
                String errMsg = String.format("Failed to revert volume [name:%s, format:%s] to snapshot [id:%s] state", volumeInfo.getName(), volumeInfo.getFormat(),
                        snapshotInfo.getSnapshotId());

                s_logger.error(errMsg);

                throw new CloudRuntimeException(errMsg);
            }
        } finally {
            if (getHypervisorRequiresResignature(volumeInfo)) {
                if (hostId != null) {
                    HostVO hostVO = hostDao.findById(hostId);
                    DataStore dataStore = dataStoreMgr.getDataStore(volumeInfo.getPoolId(), DataStoreRole.Primary);

                    volService.grantAccess(volumeInfo, hostVO, dataStore);

                    modifyTarget(true, volumeInfo, hostId);
                }
            }

            if (success) {
                volumeInfo.stateTransit(Volume.Event.OperationSucceeded);
            } else {
                volumeInfo.stateTransit(Volume.Event.OperationFailed);
            }

            snapshotDao.releaseFromLockTable(snapshotInfo.getId());
        }
    }

    private Long getHostId(VolumeInfo volumeInfo) {
        VirtualMachine virtualMachine = volumeInfo.getAttachedVM();

        if (virtualMachine == null) {
            return null;
        }

        Long hostId = virtualMachine.getHostId();

        if (hostId == null) {
            hostId = virtualMachine.getLastHostId();
        }

        return hostId;
    }

    private void modifyTarget(boolean add, VolumeInfo volumeInfo, long hostId) {
        StoragePoolVO storagePoolVO = storagePoolDao.findById(volumeInfo.getPoolId());

        Map<String, String> details = new HashMap<>(3);

        details.put(ModifyTargetsCommand.IQN, volumeInfo.get_iScsiName());
        details.put(ModifyTargetsCommand.STORAGE_HOST, storagePoolVO.getHostAddress());
        details.put(ModifyTargetsCommand.STORAGE_PORT, String.valueOf(storagePoolVO.getPort()));

        List<Map<String, String>> targets = new ArrayList<>(1);

        targets.add(details);

        ModifyTargetsCommand cmd = new ModifyTargetsCommand();

        cmd.setTargets(targets);
        cmd.setApplyToAllHostsInCluster(true);
        cmd.setAdd(add);
        cmd.setTargetTypeToRemove(ModifyTargetsCommand.TargetTypeToRemove.BOTH);

        sendModifyTargetsCommand(cmd, hostId);
    }

    private void sendModifyTargetsCommand(ModifyTargetsCommand cmd, long hostId) {
        Answer answer = agentMgr.easySend(hostId, cmd);

        if (answer == null) {
            throw new CloudRuntimeException("Unable to get an answer to the modify targets command");
        }

        if (!answer.getResult()) {
            String msg = "Unable to modify targets on the following host: " + hostId;

            throw new CloudRuntimeException(msg);
        }
    }

    @Override
    @DB
    public SnapshotInfo takeSnapshot(SnapshotInfo snapshotInfo) {
        VolumeInfo volumeInfo = snapshotInfo.getBaseVolume();

        verifyFormat(volumeInfo);
        verifyLocationType(snapshotInfo);

        final boolean canStorageSystemCreateVolumeFromSnapshot = canStorageSystemCreateVolumeFromSnapshot(volumeInfo.getPoolId());
        final boolean computeClusterSupportsVolumeClone;

        // only XenServer, VMware and KVM are currently supported
        if (volumeInfo.getFormat() == ImageFormat.VHD) {
            HostVO hostVO = getHost(volumeInfo.getId());

            computeClusterSupportsVolumeClone = clusterDao.getSupportsResigning(hostVO.getClusterId());
        }
        else if (volumeInfo.getFormat() == ImageFormat.OVA || volumeInfo.getFormat() == ImageFormat.QCOW2 || volumeInfo.getFormat() == ImageFormat.RAW) {
            computeClusterSupportsVolumeClone = true;
        }
        else {
            throw new CloudRuntimeException("Unsupported format");
        }

        SnapshotVO snapshotVO = snapshotDao.acquireInLockTable(snapshotInfo.getId());

        if (snapshotVO == null) {
            throw new CloudRuntimeException("Failed to acquire lock on the following snapshot: " + snapshotInfo.getId());
        }

        VMSnapshot vmSnapshot = null;

        if (ImageFormat.OVA.equals(volumeInfo.getFormat())) {
            setVmdk(snapshotInfo, volumeInfo);

            vmSnapshot = takeHypervisorSnapshot(volumeInfo);
        }

        SnapshotResult result = null;
        SnapshotInfo snapshotOnPrimary;

        try {
            volumeInfo.stateTransit(Volume.Event.SnapshotRequested);

            // if canStorageSystemCreateVolumeFromSnapshot && computeClusterSupportsVolumeClone, then take a back-end snapshot or create a back-end clone;
            // else, just create a new back-end volume (eventually used to create a new SR on and to copy a VDI to)

            if (canStorageSystemCreateVolumeFromSnapshot && computeClusterSupportsVolumeClone) {
                SnapshotDetailsVO snapshotDetail = new SnapshotDetailsVO(snapshotInfo.getId(),
                    "takeSnapshot",
                    Boolean.TRUE.toString(),
                    false);

                snapshotDetailsDao.persist(snapshotDetail);
            }

            result = snapshotSvr.takeSnapshot(snapshotInfo);

            if (result.isFailed()) {
                s_logger.debug("Failed to take a snapshot: " + result.getResult());

                throw new CloudRuntimeException(result.getResult());
            }

            if (!canStorageSystemCreateVolumeFromSnapshot || !computeClusterSupportsVolumeClone) {
                performSnapshotAndCopyOnHostSide(volumeInfo, snapshotInfo);
            }

            snapshotOnPrimary = result.getSnapshot();
        }
        finally {
            if (result != null && result.isSuccess()) {
                volumeInfo.stateTransit(Volume.Event.OperationSucceeded);
            } else {
                volumeInfo.stateTransit(Volume.Event.OperationFailed);
            }

            if (ImageFormat.OVA.equals(volumeInfo.getFormat())) {
                if (vmSnapshot != null) {
                    deleteHypervisorSnapshot(vmSnapshot);
                }
            }
        }

        snapshotDao.releaseFromLockTable(snapshotInfo.getId());

        return snapshotOnPrimary;
    }

    @Override
    public void postSnapshotCreation(SnapshotInfo snapshot) {
        updateLocationTypeInDb(snapshot);

        if (snapshot.getLocationType() == Snapshot.LocationType.SECONDARY) {
            // remove the snapshot on primary storage
            try {
                snapshotSvr.deleteSnapshot(snapshot);
            } catch (Exception e) {
                s_logger.warn("Failed to clean up snapshot '" + snapshot.getId() + "' on primary storage: " + e.getMessage());
            }
        }

    }

    private VMSnapshot takeHypervisorSnapshot(VolumeInfo volumeInfo) {
        VirtualMachine virtualMachine = volumeInfo.getAttachedVM();

        if (virtualMachine != null && VirtualMachine.State.Running.equals(virtualMachine.getState())) {
            String vmSnapshotName = UUID.randomUUID().toString().replace("-", "");

            VMSnapshotVO vmSnapshotVO =
                    new VMSnapshotVO(virtualMachine.getAccountId(), virtualMachine.getDomainId(), virtualMachine.getId(), vmSnapshotName, vmSnapshotName,
                            vmSnapshotName, virtualMachine.getServiceOfferingId(), VMSnapshot.Type.Disk, null);

            VMSnapshot vmSnapshot = vmSnapshotDao.persist(vmSnapshotVO);

            if (vmSnapshot == null) {
                throw new CloudRuntimeException("Unable to allocate a VM snapshot object");
            }

            vmSnapshot = vmSnapshotService.createVMSnapshot(virtualMachine.getId(), vmSnapshot.getId(), true);

            if (vmSnapshot == null) {
                throw new CloudRuntimeException("Unable to create a hypervisor-side snapshot");
            }

            try {
                Thread.sleep(60000);
            }
            catch (Exception ex) {
                s_logger.warn(ex.getMessage(), ex);
            }

            return vmSnapshot;
        }

        // We didn't need to take a hypervisor-side snapshot. Return 'null' to indicate this.
        return null;
    }

    private void deleteHypervisorSnapshot(VMSnapshot vmSnapshot) {
        boolean success = vmSnapshotService.deleteVMSnapshot(vmSnapshot.getId());

        if (!success) {
            throw new CloudRuntimeException("Unable to delete the hypervisor-side snapshot");
        }
    }

    private void setVmdk(SnapshotInfo snapshotInfo, VolumeInfo volumeInfo) {
        if (!ImageFormat.OVA.equals(volumeInfo.getFormat())) {
            return;
        }

        String search = "]";

        String path = volumeInfo.getPath();
        int startIndex = path.indexOf(search);

        SnapshotDetailsVO snapshotDetail = new SnapshotDetailsVO(snapshotInfo.getId(),
                DiskTO.VMDK,
                path.substring(startIndex + search.length()).trim(),
                false);

        snapshotDetailsDao.persist(snapshotDetail);
    }

    private void updateLocationTypeInDb(SnapshotInfo snapshotInfo) {
        Object objPayload = snapshotInfo.getPayload();

        if (objPayload instanceof CreateSnapshotPayload) {
            CreateSnapshotPayload payload = (CreateSnapshotPayload)objPayload;

            SnapshotVO snapshot = snapshotDao.findById(snapshotInfo.getId());

            snapshot.setLocationType(payload.getLocationType());

            snapshotDao.update(snapshotInfo.getId(), snapshot);
        }
    }

    private boolean canStorageSystemCreateVolumeFromSnapshot(long storagePoolId) {
        return storageSystemSupportsCapability(storagePoolId, DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_SNAPSHOT.toString());
    }

    private boolean storageSystemSupportsCapability(long storagePoolId, String capability) {
        boolean supportsCapability = false;

        DataStore dataStore = dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary);

        Map<String, String> mapCapabilities = dataStore.getDriver().getCapabilities();

        if (mapCapabilities != null) {
            String value = mapCapabilities.get(capability);

            supportsCapability = Boolean.valueOf(value);
        }

        return supportsCapability;
    }

    private void performSnapshotAndCopyOnHostSide(VolumeInfo volumeInfo, SnapshotInfo snapshotInfo) {
        Map<String, String> sourceDetails = null;

        VolumeVO volumeVO = volumeDao.findById(volumeInfo.getId());

        Long vmInstanceId = volumeVO.getInstanceId();
        VMInstanceVO vmInstanceVO = vmInstanceDao.findById(vmInstanceId);

        Long hostId = null;

        // if the volume to snapshot is associated with a VM
        if (vmInstanceVO != null) {
            hostId = vmInstanceVO.getHostId();

            // if the VM is not associated with a host
            if (hostId == null) {
                hostId = vmInstanceVO.getLastHostId();

                if (hostId == null) {
                    sourceDetails = getSourceDetails(volumeInfo);
                }
            }
        }
        // volume to snapshot is not associated with a VM (could be a data disk in the detached state)
        else {
            sourceDetails = getSourceDetails(volumeInfo);
        }

        HostVO hostVO = null;

        if (hostId != null) {
            hostVO = hostDao.findById(hostId);
        }

        if (hostVO == null || !ResourceState.Enabled.equals(hostVO.getResourceState())) {
            Optional<HostVO> optHostVO = getHost(volumeInfo.getDataCenterId(), false);

            if (optHostVO.isPresent()) {
                hostVO = optHostVO.get();
            }
        }

        if (hostVO == null) {
            final String errMsg = "Unable to locate an applicable host";

            s_logger.error("performSnapshotAndCopyOnHostSide: " + errMsg);

            throw new CloudRuntimeException(errMsg);
        }

        long storagePoolId = volumeVO.getPoolId();
        StoragePoolVO storagePoolVO = storagePoolDao.findById(storagePoolId);
        DataStore dataStore = dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary);

        Map<String, String> destDetails = getDestDetails(storagePoolVO, snapshotInfo);

        SnapshotAndCopyCommand snapshotAndCopyCommand = new SnapshotAndCopyCommand(volumeInfo.getPath(), sourceDetails, destDetails);

        SnapshotAndCopyAnswer snapshotAndCopyAnswer;

        try {
            // if sourceDetails != null, we need to connect the host(s) to the volume
            if (sourceDetails != null) {
                volService.grantAccess(volumeInfo, hostVO, dataStore);
            }

            volService.grantAccess(snapshotInfo, hostVO, dataStore);

            snapshotAndCopyAnswer = (SnapshotAndCopyAnswer)agentMgr.send(hostVO.getId(), snapshotAndCopyCommand);
        }
        catch (Exception ex) {
            throw new CloudRuntimeException(ex.getMessage());
        }
        finally {
            try {
                volService.revokeAccess(snapshotInfo, hostVO, dataStore);

                // if sourceDetails != null, we need to disconnect the host(s) from the volume
                if (sourceDetails != null) {
                    volService.revokeAccess(volumeInfo, hostVO, dataStore);
                }
            }
            catch (Exception ex) {
                s_logger.debug(ex.getMessage(), ex);
            }
        }

        if (snapshotAndCopyAnswer == null || !snapshotAndCopyAnswer.getResult()) {
            final String errMsg;

            if (snapshotAndCopyAnswer != null && snapshotAndCopyAnswer.getDetails() != null && !snapshotAndCopyAnswer.getDetails().isEmpty()) {
                errMsg = snapshotAndCopyAnswer.getDetails();
            }
            else {
                errMsg = "Unable to perform host-side operation";
            }

            throw new CloudRuntimeException(errMsg);
        }

        String path = snapshotAndCopyAnswer.getPath(); // for XenServer, this is the VDI's UUID

        SnapshotDetailsVO snapshotDetail = new SnapshotDetailsVO(snapshotInfo.getId(),
                DiskTO.PATH,
                path,
                false);

        snapshotDetailsDao.persist(snapshotDetail);
    }

    private Map<String, String> getSourceDetails(VolumeInfo volumeInfo) {
        Map<String, String> sourceDetails = new HashMap<>();

        VolumeVO volumeVO = volumeDao.findById(volumeInfo.getId());

        long storagePoolId = volumeVO.getPoolId();
        StoragePoolVO storagePoolVO = storagePoolDao.findById(storagePoolId);

        sourceDetails.put(DiskTO.STORAGE_HOST, storagePoolVO.getHostAddress());
        sourceDetails.put(DiskTO.STORAGE_PORT, String.valueOf(storagePoolVO.getPort()));
        sourceDetails.put(DiskTO.IQN, volumeVO.get_iScsiName());
        sourceDetails.put(DiskTO.PROTOCOL_TYPE, (storagePoolVO.getPoolType() != null) ? storagePoolVO.getPoolType().toString() : null);

        ChapInfo chapInfo = volService.getChapInfo(volumeInfo, volumeInfo.getDataStore());

        if (chapInfo != null) {
            sourceDetails.put(DiskTO.CHAP_INITIATOR_USERNAME, chapInfo.getInitiatorUsername());
            sourceDetails.put(DiskTO.CHAP_INITIATOR_SECRET, chapInfo.getInitiatorSecret());
            sourceDetails.put(DiskTO.CHAP_TARGET_USERNAME, chapInfo.getTargetUsername());
            sourceDetails.put(DiskTO.CHAP_TARGET_SECRET, chapInfo.getTargetSecret());
        }

        return sourceDetails;
    }

    private Map<String, String> getDestDetails(StoragePoolVO storagePoolVO, SnapshotInfo snapshotInfo) {
        Map<String, String> destDetails = new HashMap<>();

        destDetails.put(DiskTO.STORAGE_HOST, storagePoolVO.getHostAddress());
        destDetails.put(DiskTO.STORAGE_PORT, String.valueOf(storagePoolVO.getPort()));
        destDetails.put(DiskTO.PROTOCOL_TYPE, (storagePoolVO.getPoolType() != null) ? storagePoolVO.getPoolType().toString() : null);

        long snapshotId = snapshotInfo.getId();

        destDetails.put(DiskTO.IQN, getProperty(snapshotId, DiskTO.IQN));

        destDetails.put(DiskTO.CHAP_INITIATOR_USERNAME, getProperty(snapshotId, DiskTO.CHAP_INITIATOR_USERNAME));
        destDetails.put(DiskTO.CHAP_INITIATOR_SECRET, getProperty(snapshotId, DiskTO.CHAP_INITIATOR_SECRET));
        destDetails.put(DiskTO.CHAP_TARGET_USERNAME, getProperty(snapshotId, DiskTO.CHAP_TARGET_USERNAME));
        destDetails.put(DiskTO.CHAP_TARGET_SECRET, getProperty(snapshotId, DiskTO.CHAP_TARGET_SECRET));

        return destDetails;
    }

    private String getProperty(long snapshotId, String property) {
        SnapshotDetailsVO snapshotDetails = snapshotDetailsDao.findDetail(snapshotId, property);

        if (snapshotDetails != null) {
            return snapshotDetails.getValue();
        }

        return null;
    }

    private HostVO getHost(long volumeId) {
        VolumeVO volumeVO = volumeDao.findById(volumeId);

        Long vmInstanceId = volumeVO.getInstanceId();
        VMInstanceVO vmInstanceVO = vmInstanceDao.findById(vmInstanceId);

        Long hostId = null;

        // if the volume to snapshot is associated with a VM
        if (vmInstanceVO != null) {
            hostId = vmInstanceVO.getHostId();

            // if the VM is not associated with a host
            if (hostId == null) {
                hostId = vmInstanceVO.getLastHostId();
            }
        }

        return getHost(volumeVO.getDataCenterId(), hostId);
    }

    private HostVO getHost(long zoneId, Long hostId) {
        Optional<HostVO> optHostVO = getHost(zoneId, true);

        if (optHostVO.isPresent()) {
            return optHostVO.get();
        }

        HostVO hostVO = hostDao.findById(hostId);

        if (hostVO != null && ResourceState.Enabled.equals(hostVO.getResourceState())) {
            return hostVO;
        }

        optHostVO = getHost(zoneId, false);

        if (optHostVO.isPresent()) {
            return optHostVO.get();
        }

        throw new CloudRuntimeException("Unable to locate an applicable host");
    }

    private Optional<HostVO> getHost(long zoneId, boolean computeClusterMustSupportResign) {
        List<? extends Cluster> clusters = mgr.searchForClusters(zoneId, 0L, Long.MAX_VALUE, HypervisorType.XenServer.toString());

        if (clusters == null) {
            clusters = new ArrayList<>();
        }

        Collections.shuffle(clusters, new Random(System.nanoTime()));

        clusters:
        for (Cluster cluster : clusters) {
            if (cluster.getAllocationState() == AllocationState.Enabled) {
                List<HostVO> hosts = hostDao.findByClusterId(cluster.getId());

                if (hosts != null) {
                    Collections.shuffle(hosts, new Random(System.nanoTime()));

                    for (HostVO host : hosts) {
                        if (ResourceState.Enabled.equals(host.getResourceState())) {
                            if (computeClusterMustSupportResign) {
                                if (clusterDao.getSupportsResigning(cluster.getId())) {
                                    return Optional.of(host);
                                }
                                else {
                                    // no other host in the cluster in question should be able to satisfy our requirements here, so move on to the next cluster
                                    continue clusters;
                                }
                            }
                            else {
                                return Optional.of(host);
                            }
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    private void markAsBackedUp(SnapshotObject snapshotObj) {
        try {
            snapshotObj.processEvent(Snapshot.Event.BackupToSecondary);
            snapshotObj.processEvent(Snapshot.Event.OperationSucceeded);
        }
        catch (NoTransitionException ex) {
            s_logger.debug("Failed to change state: " + ex.toString());

            try {
                snapshotObj.processEvent(Snapshot.Event.OperationFailed);
            }
            catch (NoTransitionException ex2) {
                s_logger.debug("Failed to change state: " + ex2.toString());
            }
        }
    }

    private boolean usingBackendSnapshotFor(long snapshotId) {
        String property = getProperty(snapshotId, "takeSnapshot");

        return Boolean.parseBoolean(property);
    }

    @Override
    public StrategyPriority canHandle(Snapshot snapshot, SnapshotOperation op) {
        Snapshot.LocationType locationType = snapshot.getLocationType();

        // If the snapshot exists on Secondary Storage, we can't delete it.
        if (SnapshotOperation.DELETE.equals(op)) {
            if (Snapshot.LocationType.SECONDARY.equals(locationType)) {
                return StrategyPriority.CANT_HANDLE;
            }

            SnapshotDataStoreVO snapshotStore = snapshotStoreDao.findBySnapshot(snapshot.getId(), DataStoreRole.Image);

            // If the snapshot exists on Secondary Storage, we can't delete it.
            if (snapshotStore != null) {
                return StrategyPriority.CANT_HANDLE;
            }

            snapshotStore = snapshotStoreDao.findBySnapshot(snapshot.getId(), DataStoreRole.Primary);

            if (snapshotStore == null) {
                return StrategyPriority.CANT_HANDLE;
            }

            long snapshotStoragePoolId = snapshotStore.getDataStoreId();

            boolean storageSystemSupportsCapability = storageSystemSupportsCapability(snapshotStoragePoolId, DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString());

            return storageSystemSupportsCapability ? StrategyPriority.HIGHEST : StrategyPriority.CANT_HANDLE;
        }

        long volumeId = snapshot.getVolumeId();

        VolumeVO volumeVO = volumeDao.findByIdIncludingRemoved(volumeId);

        long volumeStoragePoolId = volumeVO.getPoolId();

        if (SnapshotOperation.REVERT.equals(op)) {
            boolean baseVolumeExists = volumeVO.getRemoved() == null;

            if (baseVolumeExists) {
                boolean acceptableFormat = isAcceptableRevertFormat(volumeVO);

                if (acceptableFormat) {
                    SnapshotDataStoreVO snapshotStore = snapshotStoreDao.findBySnapshot(snapshot.getId(), DataStoreRole.Primary);

                    boolean usingBackendSnapshot = usingBackendSnapshotFor(snapshot.getId());

                    if (usingBackendSnapshot) {
                        if (snapshotStore != null) {
                            long snapshotStoragePoolId = snapshotStore.getDataStoreId();

                            boolean storageSystemSupportsCapability = storageSystemSupportsCapability(snapshotStoragePoolId,
                                    DataStoreCapabilities.CAN_REVERT_VOLUME_TO_SNAPSHOT.toString());

                            if (storageSystemSupportsCapability) {
                                return StrategyPriority.HIGHEST;
                            }

                            storageSystemSupportsCapability = storageSystemSupportsCapability(volumeStoragePoolId,
                                    DataStoreCapabilities.CAN_REVERT_VOLUME_TO_SNAPSHOT.toString());

                            if (storageSystemSupportsCapability) {
                                return StrategyPriority.HIGHEST;
                            }
                        }
                    }
                    else {
                        if (snapshotStore != null) {
                            long snapshotStoragePoolId = snapshotStore.getDataStoreId();

                            StoragePoolVO storagePoolVO = storagePoolDao.findById(snapshotStoragePoolId);

                            if (storagePoolVO.isManaged()) {
                                return StrategyPriority.HIGHEST;
                            }
                        }

                        StoragePoolVO storagePoolVO = storagePoolDao.findById(volumeStoragePoolId);

                        if (storagePoolVO.isManaged()) {
                            return StrategyPriority.HIGHEST;
                        }
                    }
                }
            }

            return StrategyPriority.CANT_HANDLE;
        }

        boolean storageSystemSupportsCapability = storageSystemSupportsCapability(volumeStoragePoolId, DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString());

        return storageSystemSupportsCapability ? StrategyPriority.HIGHEST : StrategyPriority.CANT_HANDLE;
    }

}
