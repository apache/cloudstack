//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package org.apache.cloudstack.backup;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.backup.CreateImageTransferCmd;
import org.apache.cloudstack.api.command.admin.backup.DeleteVmCheckpointCmd;
import org.apache.cloudstack.api.command.admin.backup.FinalizeBackupCmd;
import org.apache.cloudstack.api.command.admin.backup.FinalizeImageTransferCmd;
import org.apache.cloudstack.api.command.admin.backup.ListImageTransfersCmd;
import org.apache.cloudstack.api.command.admin.backup.ListVmCheckpointsCmd;
import org.apache.cloudstack.api.command.admin.backup.StartBackupCmd;
import org.apache.cloudstack.api.response.CheckpointResponse;
import org.apache.cloudstack.api.response.ImageTransferResponse;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.backup.dao.ImageTransferDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.api.ApiDBUtils;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeStats;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class KVMBackupExportServiceImpl extends ManagerBase implements KVMBackupExportService {

    @Inject
    private VMInstanceDao vmInstanceDao;

    @Inject
    private BackupDao backupDao;

    @Inject
    private ImageTransferDao imageTransferDao;

    @Inject
    private VolumeDao volumeDao;

    @Inject
    private VolumeDetailsDao volumeDetailsDao;

    @Inject
    private AgentManager agentManager;

    @Inject
    private BackupOfferingDao backupOfferingDao;

    @Inject
    private HostDao hostDao;

    @Inject
    private PrimaryDataStoreDao primaryDataStoreDao;

    private Timer imageTransferTimer;

    private boolean isDummyOffering(Long backupOfferingId) {
        if (backupOfferingId == null) {
            throw new CloudRuntimeException("VM not assigned a backup offering");
        }
        BackupOfferingVO offering = backupOfferingDao.findById(backupOfferingId);
        if (offering == null) {
            throw new CloudRuntimeException("Backup offering not found: " + backupOfferingId);
        }
        if ("dummy".equalsIgnoreCase(offering.getName())) {
            return true;
        }
        return false;
    }

    @Override
    public Backup createBackup(StartBackupCmd cmd) {
        //ToDo: add config check, access check, resource count check, etc.
        Long vmId = cmd.getVmId();

        VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM not found: " + vmId);
        }

        if (vm.getState() != State.Running && vm.getState() != State.Stopped) {
            throw new CloudRuntimeException("VM must be running or stopped to start backup");
        }

        if (vm.getBackupOfferingId() == null) {
            throw new CloudRuntimeException("VM not assigned a backup offering");
        }

        Backup existingBackup = backupDao.findByVmId(vmId);
        if (existingBackup != null && existingBackup.getStatus() == Backup.Status.BackingUp) {
            throw new CloudRuntimeException("Backup already in progress for VM: " + vmId);
        }

        BackupVO backup = new BackupVO();
        backup.setVmId(vmId);
        String name = cmd.getName();
        if (StringUtils.isEmpty(name)) {
            name = vmId + "-" + DateTime.now();
        }
        backup.setName(name);
        final String description = cmd.getDescription();
        if (StringUtils.isNotEmpty(description)) {
            backup.setDescription(description);
        }
        backup.setAccountId(vm.getAccountId());
        backup.setDomainId(vm.getDomainId());
        backup.setZoneId(vm.getDataCenterId());
        backup.setStatus(Backup.Status.Queued);
        backup.setBackupOfferingId(vm.getBackupOfferingId());
        backup.setDate(new Date());

        String toCheckpointId = "ckp-" + UUID.randomUUID().toString().substring(0, 8);
        String fromCheckpointId = vm.getActiveCheckpointId();

        backup.setToCheckpointId(toCheckpointId);
        backup.setFromCheckpointId(fromCheckpointId);

        Long hostId = vm.getHostId() != null ? vm.getHostId() : vm.getLastHostId();
        backup.setHostId(hostId);
        // Will be changed later if incremental was done
        backup.setType("FULL");

        return backupDao.persist(backup);
    }

    protected void removedFailedBackup(BackupVO backup) {
        backup.setStatus(Backup.Status.Error);
        backupDao.update(backup.getId(), backup);
        backupDao.remove(backup.getId());
    }

    @Override
    public Backup startBackup(StartBackupCmd cmd) {
        BackupVO backup = backupDao.findById(cmd.getEntityId());
        Long vmId = cmd.getVmId();
        VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM not found: " + vmId);
        }
        List<VolumeVO> volumes = volumeDao.findByInstance(vmId);
        Map<String, String> diskPathUuidMap = new HashMap<>();
        for (Volume vol : volumes) {
            String volumePath = getVolumePathForFileBasedBackend(vol);
            diskPathUuidMap.put(volumePath, vol.getUuid());
        }
        long hostId = backup.getHostId();

        Host host = hostDao.findById(hostId);
        StartBackupCommand startCmd = new StartBackupCommand(
            vm.getInstanceName(),
            backup.getToCheckpointId(),
            backup.getFromCheckpointId(),
            vm.getActiveCheckpointCreateTime(),
            backup.getUuid(),
            diskPathUuidMap,
            vm.getState() == State.Stopped
        );

        boolean dummyOffering = isDummyOffering(vm.getBackupOfferingId());

        StartBackupAnswer answer;
        try {
            if (dummyOffering) {
                answer = new StartBackupAnswer(startCmd, true, "Dummy answer", System.currentTimeMillis());
            } else {
                answer = (StartBackupAnswer) agentManager.send(hostId, startCmd);
            }
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            removedFailedBackup(backup);
            logger.error("Failed to communicate with agent on {} for {} start", host, backup, e);
            throw new CloudRuntimeException("Failed to communicate with agent", e);
        }

        if (!answer.getResult()) {
            removedFailedBackup(backup);
            logger.error("Failed to start {} due to: {}", backup, answer.getDetails());
            throw new CloudRuntimeException("Failed to start backup: " + answer.getDetails());
        }

        // Update backup with checkpoint creation time
        backup.setCheckpointCreateTime(answer.getCheckpointCreateTime());
        if (Boolean.TRUE.equals(answer.getIncremental())) {
            // todo: set it in the backend
            backup.setType("Incremental");
        }
        updateBackupState(backup, Backup.Status.ReadyForTransfer);
        return backup;
    }

    protected void updateBackupState(BackupVO backup, Backup.Status newStatus) {
        backup.setStatus(newStatus);
        backupDao.update(backup.getId(), backup);
    }

    @Override
    public Backup finalizeBackup(FinalizeBackupCmd cmd) {
        Long vmId = cmd.getVmId();
        Long backupId = cmd.getBackupId();

        // Get backup
        BackupVO backup = backupDao.findById(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Backup not found: " + backupId);
        }

        if (!backup.getVmId().equals(vmId)) {
            throw new CloudRuntimeException("Backup does not belong to VM: " + vmId);
        }

        VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM not found: " + vmId);
        }

        boolean dummyOffering = isDummyOffering(backup.getBackupOfferingId());

        updateBackupState(backup, Backup.Status.FinalizingTransfer);

        List<ImageTransferVO> transfers = imageTransferDao.listByBackupId(backupId);
        for (ImageTransferVO transfer : transfers) {
            if (transfer.getPhase() != ImageTransferVO.Phase.finished) {
                updateBackupState(backup, Backup.Status.Failed);
                throw new CloudRuntimeException(String.format("Image transfer %s not finalized for backup: %s", transfer.getUuid(), backup.getUuid()));
            }
            imageTransferDao.remove(transfer.getId());
        }

        if (vm.getState() == State.Running) {
            StopBackupCommand stopCmd = new StopBackupCommand(vm.getInstanceName(), vmId, backupId);

            StopBackupAnswer answer;
            try {
                if (dummyOffering) {
                    answer = new StopBackupAnswer(stopCmd, true, "Dummy answer");
                } else {
                    answer = (StopBackupAnswer) agentManager.send(backup.getHostId(), stopCmd);
                }

            } catch (AgentUnavailableException | OperationTimedoutException e) {
                updateBackupState(backup, Backup.Status.Failed);
                throw new CloudRuntimeException("Failed to communicate with agent", e);
            }

            if (!answer.getResult()) {
                updateBackupState(backup, Backup.Status.Failed);
                throw new CloudRuntimeException("Failed to stop backup: " + answer.getDetails());
            }
        }

        // Update VM checkpoint tracking
        String oldCheckpointId = vm.getActiveCheckpointId();
        vm.setActiveCheckpointId(backup.getToCheckpointId());
        vm.setActiveCheckpointCreateTime(backup.getCheckpointCreateTime());
        vmInstanceDao.update(vmId, vm);

        // Delete old checkpoint if exists (POC: skip actual libvirt call)
        if (oldCheckpointId != null) {
            // todo: In production: send command to delete oldCheckpointId via virsh checkpoint-delete
            logger.debug("Would delete old checkpoint: {}", oldCheckpointId);
        }

        // Delete backup session record
        updateBackupState(backup, Backup.Status.BackedUp);
        backupDao.remove(backup.getId());

        return backup;

    }

    private ImageTransferVO createDownloadImageTransfer(Long backupId, VolumeVO volume, ImageTransfer.Backend backend) {
        final String direction = ImageTransfer.Direction.download.toString();
        BackupVO backup = backupDao.findById(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Backup not found: " + backupId);
        }
        boolean dummyOffering = isDummyOffering(backup.getBackupOfferingId());
        if (ImageTransfer.Backend.file.equals(backend)) {
            throw new CloudRuntimeException("File backend is not supported for download");
        }

        String transferId = UUID.randomUUID().toString();

        String socket = backup.getUuid();
        VMInstanceVO vm = vmInstanceDao.findById(backup.getVmId());
        if (vm.getState() == State.Stopped) {
            String volumePath = getVolumePathForFileBasedBackend(volume);
            startNBDServer(transferId, direction, backup.getHostId(), volume.getUuid(), volumePath, vm.getActiveCheckpointId());
            socket = transferId;
        }

        CreateImageTransferCommand transferCmd = new CreateImageTransferCommand(
                transferId,
                direction,
                volume.getUuid(),
                socket,
                backup.getFromCheckpointId());

        try {
            CreateImageTransferAnswer answer;
            if (dummyOffering) {
                answer = new CreateImageTransferAnswer(transferCmd, true, "Dummy answer", "image-transfer-id", "nbd://127.0.0.1:10809/vda");
            } else {
                answer = (CreateImageTransferAnswer) agentManager.send(backup.getHostId(), transferCmd);
            }

            if (!answer.getResult()) {
                throw new CloudRuntimeException("Failed to create image transfer: " + answer.getDetails());
            }

            ImageTransferVO imageTransfer = new ImageTransferVO(
                    transferId,
                    backupId,
                    volume.getId(),
                    backup.getHostId(),
                    socket,
                    ImageTransferVO.Phase.transferring,
                    ImageTransfer.Direction.download,
                    backup.getAccountId(),
                    backup.getDomainId(),
                    backup.getZoneId()
            );
            imageTransfer.setTransferUrl(answer.getTransferUrl());
            imageTransfer.setSignedTicketId(answer.getImageTransferId());
            imageTransfer = imageTransferDao.persist(imageTransfer);
            return imageTransfer;

        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException("Failed to communicate with agent", e);
        }
    }

    private HostVO getFirstHostFromStoragePool(StoragePoolVO storagePoolVO) {
        List<HostVO> hosts = null;
        if (storagePoolVO.getScope().equals(ScopeType.CLUSTER)) {
            hosts = hostDao.findByClusterId(storagePoolVO.getClusterId());

        } else if (storagePoolVO.getScope().equals(ScopeType.ZONE)) {
            hosts = hostDao.findByDataCenterId(storagePoolVO.getDataCenterId());
        }
        return hosts.get(0);
    }

    private void startNBDServer(String transferId, String direction, Long hostId, String exportName, String volumePath, String checkpointId) {
        StartNBDServerAnswer nbdServerAnswer;
        if (hostId == null) {
            throw new CloudRuntimeException("Host cannot be determined for starting NBD server");
        }
        HostVO host = hostDao.findById(hostId);
        if (host == null) {
            throw new CloudRuntimeException("Host cannot be found for starting NBD server with ID: " + hostId);
        }
        StartNBDServerCommand nbdServerCmd = new StartNBDServerCommand(
                transferId,
                exportName,
                volumePath,
                transferId,
                direction,
                checkpointId
        );
        try {
            nbdServerAnswer = (StartNBDServerAnswer) agentManager.send(hostId, nbdServerCmd);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException("Failed to communicate with agent", e);
        }
        if (!nbdServerAnswer.getResult()) {
            throw new CloudRuntimeException("Failed to start the NBD server");
        }
    }

    private String getVolumePathForFileBasedBackend(Volume volume) {
        Long poolId = volume.getPoolId();
        StoragePoolVO storagePoolVO = primaryDataStoreDao.findById(poolId);
        // todo: This only works with file based storage (not ceph, linbit)
        String volumePath = String.format("/mnt/%s/%s", storagePoolVO.getUuid(), volume.getPath());
        return volumePath;
    }

    private ImageTransferVO createUploadImageTransfer(VolumeVO volume, ImageTransfer.Backend backend) {
        final String direction = ImageTransfer.Direction.upload.toString();
        String transferId = UUID.randomUUID().toString();

        Long poolId = volume.getPoolId();
        StoragePoolVO storagePoolVO = primaryDataStoreDao.findById(poolId);
        Host host = getFirstHostFromStoragePool(storagePoolVO);
        String volumePath = getVolumePathForFileBasedBackend(volume);

        ImageTransferVO imageTransfer;
        CreateImageTransferCommand transferCmd;
        if (backend.equals(ImageTransfer.Backend.file)) {
            imageTransfer = new ImageTransferVO(
                    transferId,
                    volume.getId(),
                    host.getId(),
                    volumePath,
                    ImageTransferVO.Phase.transferring,
                    ImageTransfer.Direction.upload,
                    volume.getAccountId(),
                    volume.getDomainId(),
                    volume.getDataCenterId());

            transferCmd = new CreateImageTransferCommand(
                    transferId,
                    direction,
                    transferId,
                    volumePath);

        } else {
            startNBDServer(transferId, direction, host.getId(), volume.getUuid(), volumePath, null);
            imageTransfer = new ImageTransferVO(
                    transferId,
                    null,
                    volume.getId(),
                    host.getId(),
                    transferId,
                    ImageTransferVO.Phase.transferring,
                    ImageTransfer.Direction.upload,
                    volume.getAccountId(),
                    volume.getDomainId(),
                    volume.getDataCenterId());

            transferCmd = new CreateImageTransferCommand(
                    transferId,
                    direction,
                    volume.getUuid(),
                    transferId,
                    null);
        }
        CreateImageTransferAnswer transferAnswer;
        try {
            transferAnswer = (CreateImageTransferAnswer) agentManager.send(imageTransfer.getHostId(), transferCmd);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException("Failed to communicate with agent", e);
        }

        if (!transferAnswer.getResult()) {
            if (!backend.equals(ImageTransfer.Backend.file)) {
                stopNBDServer(imageTransfer);
            }
            throw new CloudRuntimeException("Failed to create image transfer: " + transferAnswer.getDetails());
        }

        imageTransfer.setTransferUrl(transferAnswer.getTransferUrl());
        imageTransfer.setSignedTicketId(transferAnswer.getImageTransferId());
        imageTransfer = imageTransferDao.persist(imageTransfer);
        return imageTransfer;

    }

    private ImageTransfer.Backend getImageTransferBackend(ImageTransfer.Format format, ImageTransfer.Direction direction) {
        if (ImageTransfer.Format.cow.equals(format)) {
            if (ImageTransfer.Direction.download.equals(direction)) {
                logger.debug("Using NBD backend for download");
                return ImageTransfer.Backend.nbd;
            }
            return ImageTransfer.Backend.file;
        } else {
            return ImageTransfer.Backend.nbd;
        }
    }

    @Override
    public ImageTransferResponse createImageTransfer(CreateImageTransferCmd cmd) {
        ImageTransfer imageTransfer = createImageTransfer(cmd.getVolumeId(), cmd.getBackupId(), cmd.getDirection(), cmd.getFormat());
        if (imageTransfer instanceof ImageTransferVO) {
            ImageTransferVO imageTransferVO = (ImageTransferVO) imageTransfer;
            return toImageTransferResponse(imageTransferVO);
        }
        return toImageTransferResponse(imageTransferDao.findById(imageTransfer.getId()));
    }

    @Override
    public ImageTransfer createImageTransfer(long volumeId, Long backupId, ImageTransfer.Direction direction, ImageTransfer.Format format) {
        ImageTransfer imageTransfer;
        VolumeVO volume = volumeDao.findById(volumeId);

        ImageTransferVO existingTransfer = imageTransferDao.findUnfinishedByVolume(volume.getId());
        if (existingTransfer != null) {
            throw new CloudRuntimeException("Image transfer already in progress for volume: " + volume.getUuid());
        }

        ImageTransfer.Backend backend = getImageTransferBackend(format, direction);
        if (ImageTransfer.Direction.upload.equals(direction)) {
            imageTransfer = createUploadImageTransfer(volume, backend);
        } else if (ImageTransfer.Direction.download.equals(direction)) {
            imageTransfer = createDownloadImageTransfer(backupId, volume, backend);
        } else {
            throw new CloudRuntimeException("Invalid direction: " + direction);
        }

        return imageTransferDao.findById(imageTransfer.getId());
    }

    @Override
    public boolean cancelImageTransfer(long imageTransferId) {
        ImageTransferVO imageTransfer = imageTransferDao.findById(imageTransferId);
        if (imageTransfer == null) {
            throw new CloudRuntimeException("Image transfer not found: " + imageTransferId);
        }
        // ToDo: Implement cancel logic
        return true;
    }

    private void finalizeDownloadImageTransfer(ImageTransferVO imageTransfer) {

        String transferId = imageTransfer.getUuid();
        FinalizeImageTransferCommand finalizeCmd = new FinalizeImageTransferCommand(transferId);

        BackupVO backup = backupDao.findById(imageTransfer.getBackupId());
        boolean dummyOffering = isDummyOffering(backup.getBackupOfferingId());

        Answer answer;
        try {
            if (dummyOffering) {
                answer = new Answer(finalizeCmd, true, "Image transfer finalized.");
            } else {
                answer = agentManager.send(backup.getHostId(), finalizeCmd);
            }

        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException("Failed to communicate with agent", e);
        }

        if (!answer.getResult()) {
            throw new CloudRuntimeException("Failed to finalize image transfer: " + answer.getDetails());
        }

        VMInstanceVO vm = vmInstanceDao.findById(backup.getVmId());
        if (vm.getState() == State.Stopped) {
            boolean stopNbdServerResult = stopNBDServer(imageTransfer);
            if (!stopNbdServerResult) {
                throw new CloudRuntimeException("Failed to stop the nbd server");
            }
        }
    }

    private boolean stopNBDServer(ImageTransferVO imageTransfer) {
        String transferId = imageTransfer.getUuid();
        String direction = imageTransfer.getDirection().toString();
        StopNBDServerCommand stopNbdServerCommand = new StopNBDServerCommand(transferId, direction);
        Answer answer;
        try {
            answer = agentManager.send(imageTransfer.getHostId(), stopNbdServerCommand);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            logger.error("Failed to stop NBD server on image transfer finalization", e);
            return false;
        }
        return answer.getResult();
    }

    private void finalizeUploadImageTransfer(ImageTransferVO imageTransfer) {
        String transferId = imageTransfer.getUuid();

        boolean stopNbdServerResult = stopNBDServer(imageTransfer);
        if (!stopNbdServerResult) {
            throw new CloudRuntimeException("Failed to stop the nbd server");
        }

        FinalizeImageTransferCommand finalizeCmd = new FinalizeImageTransferCommand(transferId);
        Answer answer;
        try {
            answer = agentManager.send(imageTransfer.getHostId(), finalizeCmd);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException("Failed to communicate with agent", e);
        }

        if (!answer.getResult()) {
            throw new CloudRuntimeException("Failed to finalize image transfer: " + answer.getDetails());
        }
    }

    @Override
    public boolean finalizeImageTransfer(FinalizeImageTransferCmd cmd) {
        return finalizeImageTransfer(cmd.getImageTransferId());
    }

    @Override
    public boolean finalizeImageTransfer(final long imageTransferId) {
        ImageTransferVO imageTransfer = imageTransferDao.findById(imageTransferId);
        if (imageTransfer == null) {
            throw new CloudRuntimeException("Image transfer not found: " + imageTransferId);
        }

        if (imageTransfer.getDirection().equals(ImageTransfer.Direction.download)) {
            finalizeDownloadImageTransfer(imageTransfer);
        } else {
            finalizeUploadImageTransfer(imageTransfer);
        }
        imageTransfer.setPhase(ImageTransferVO.Phase.finished);
        imageTransferDao.update(imageTransfer.getId(), imageTransfer);
        imageTransferDao.remove(imageTransfer.getId());
        return true;
    }

    @Override
    public List<ImageTransferResponse> listImageTransfers(ListImageTransfersCmd cmd) {
        Long id = cmd.getId();
        Long backupId = cmd.getBackupId();

        List<ImageTransferVO> transfers;
        if (id != null) {
            transfers = List.of(imageTransferDao.findById(id));
        } else if (backupId != null) {
            transfers = imageTransferDao.listByBackupId(backupId);
        } else {
            transfers = imageTransferDao.listAll();
        }

        return transfers.stream().map(this::toImageTransferResponse).collect(Collectors.toList());
    }

    @Override
    public List<CheckpointResponse> listVmCheckpoints(ListVmCheckpointsCmd cmd) {
        Long vmId = cmd.getVmId();

        VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM not found: " + vmId);
        }

        // Return active checkpoint (POC: simplified, no libvirt query)
        List<CheckpointResponse> responses = new ArrayList<>();
        if (vm.getActiveCheckpointId() == null) {
            return responses;
        }
        CheckpointResponse response = new CheckpointResponse();
        response.setObjectName("checkpoint");
        response.setId(vm.getActiveCheckpointId());
        Long createTimeSeconds = vm.getActiveCheckpointCreateTime();
        if (createTimeSeconds != null) {
            response.setCreated(Date.from(Instant.ofEpochSecond(createTimeSeconds)));
        } else {
            response.setCreated(new Date());
        }
        response.setIsActive(true);
        responses.add(response);
        return responses;
    }

    @Override
    public boolean deleteVmCheckpoint(DeleteVmCheckpointCmd cmd) {
        // Todo : backend support?
        VMInstanceVO vm = vmInstanceDao.findById(cmd.getVmId());
        if (vm == null) {
            throw new CloudRuntimeException("VM not found: " + cmd.getVmId());
        }
        vm.setActiveCheckpointId(null);
        vm.setActiveCheckpointCreateTime(null);
        vmInstanceDao.update(cmd.getVmId(), vm);
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(StartBackupCmd.class);
        cmdList.add(FinalizeBackupCmd.class);
        cmdList.add(CreateImageTransferCmd.class);
        cmdList.add(FinalizeImageTransferCmd.class);
        cmdList.add(ListImageTransfersCmd.class);
        cmdList.add(ListVmCheckpointsCmd.class);
        cmdList.add(DeleteVmCheckpointCmd.class);
        return cmdList;
    }

    private ImageTransferResponse toImageTransferResponse(ImageTransferVO imageTransferVO) {
        ImageTransferResponse response = new ImageTransferResponse();
        response.setId(imageTransferVO.getUuid());
        Long backupId = imageTransferVO.getBackupId();
        if (backupId != null) {
            // ToDo: Orphan image transfer record if backup is deleted before transfer finalization, need to clean up
            Backup backup = backupDao.findByIdIncludingRemoved(backupId);
            response.setBackupId(backup.getUuid());
        }
        Long volumeId = imageTransferVO.getDiskId();
        // ToDo: fix volume deletion leaving orphan image transfer record
        Volume volume = volumeDao.findByIdIncludingRemoved(volumeId);
        response.setDiskId(volume.getUuid());
        response.setTransferUrl(imageTransferVO.getTransferUrl());
        response.setPhase(imageTransferVO.getPhase().toString());
        response.setProgress(imageTransferVO.getProgress());
        response.setDirection(imageTransferVO.getDirection().toString());
        response.setCreated(imageTransferVO.getCreated());
        return response;
    }

    @Override
    public boolean start() {
        final TimerTask imageTransferPollTask = new ManagedContextTimerTask() {
            @Override
            protected void runInContext() {
                try {
                    pollImageTransferProgress();
                } catch (final Throwable t) {
                    logger.warn("Catch throwable in image transfer poll task ", t);
                }
            }
        };

        imageTransferTimer = new Timer("ImageTransferPollTask");
        long pollingInterval = ImageTransferPollingInterval.value() * 1000L;
        imageTransferTimer.schedule(imageTransferPollTask, pollingInterval, pollingInterval);
        return true;
    }

    @Override
    public boolean stop() {
        if (imageTransferTimer != null) {
            imageTransferTimer.cancel();
            imageTransferTimer = null;
        }
        return true;
    }

    private void pollImageTransferProgress() {
        try {
            List<ImageTransferVO> transferringTransfers = imageTransferDao.listByPhaseAndDirection(
                    ImageTransfer.Phase.transferring, ImageTransfer.Direction.upload);
            if (transferringTransfers == null || transferringTransfers.isEmpty()) {
                return;
            }

            Map<Long, List<ImageTransferVO>> transfersByHost = transferringTransfers.stream()
                    .collect(Collectors.groupingBy(ImageTransferVO::getHostId));
            Map<Long, VolumeVO> transferVolumeMap = new HashMap<>();

            for (Map.Entry<Long, List<ImageTransferVO>> entry : transfersByHost.entrySet()) {
                Long hostId = entry.getKey();
                List<ImageTransferVO> hostTransfers = entry.getValue();

                try {
                    List<String> transferIds = new ArrayList<>();
                    Map<String, String> volumePaths = new HashMap<>();
                    Map<String, Long> volumeSizes = new HashMap<>();

                    for (ImageTransferVO transfer : hostTransfers) {
                        VolumeVO volume = volumeDao.findById(transfer.getDiskId());
                        if (volume == null) {
                            logger.warn("Volume not found for image transfer: " + transfer.getUuid());
                            continue;
                        }
                        transferVolumeMap.put(transfer.getId(), volume);

                        String transferId = transfer.getUuid();
                        transferIds.add(transferId);

                        if (volume.getPath() == null) {
                            logger.warn("Volume path is null for image transfer: " + transfer.getUuid());
                            continue;
                        }
                        String volumePath = getVolumePathForFileBasedBackend(volume);
                        volumePaths.put(transferId, volumePath);
                        volumeSizes.put(transferId, volume.getSize());
                    }

                    if (transferIds.isEmpty()) {
                        continue;
                    }

                    GetImageTransferProgressCommand cmd = new GetImageTransferProgressCommand(transferIds, volumePaths, volumeSizes);
                    GetImageTransferProgressAnswer answer = (GetImageTransferProgressAnswer) agentManager.send(hostId, cmd);

                    if (answer == null || !answer.getResult() || MapUtils.isEmpty(answer.getProgressMap())) {
                        logger.warn("Failed to get progress for transfers on host {}: {}", hostId,
                                answer != null ? answer.getDetails() : "null answer");
                        return;
                    }
                    for (ImageTransferVO transfer : hostTransfers) {
                        String transferId = transfer.getUuid();
                        Long currentSize = answer.getProgressMap().get(transferId);
                        if (currentSize == null) {
                            continue;
                        }
                        VolumeVO volume = transferVolumeMap.get(transfer.getId());
                        long totalSize = getVolumeTotalSize(volume);
                        int progress = Math.max((int)((currentSize * 100) / totalSize), 100);
                        transfer.setProgress(progress);
                        if (currentSize >= 100) {
                            transfer.setPhase(ImageTransfer.Phase.finished);
                            logger.debug("Updated phase for image transfer {} to finished", transferId);
                        }
                        imageTransferDao.update(transfer.getId(), transfer);
                        logger.debug("Updated progress for image transfer {}: {}%", transferId, progress);
                    }

                } catch (AgentUnavailableException | OperationTimedoutException e) {
                    logger.warn("Failed to communicate with host {} for image transfer progress", hostId);
                } catch (Exception e) {
                    logger.error("Error polling image transfer progress for host " + hostId, e);
                }
            }

        } catch (Exception e) {
            logger.error("Error in pollImageTransferProgress", e);
        }
    }

    private long getVolumeTotalSize(VolumeVO volume) {
        VolumeDetailVO detail = volumeDetailsDao.findDetail(volume.getId(), ApiConstants.VIRTUAL_SIZE);
        if (detail != null) {
            long size = NumbersUtil.parseLong(detail.getValue(), 0L);
            if (size > 0) {
                return size;
            }
        }
        ApiDBUtils.getVolumeStatistics(volume.getPath());
        VolumeStats vs = null;
        if (List.of(Storage.ImageFormat.VHD, Storage.ImageFormat.QCOW2, Storage.ImageFormat.RAW).contains(volume.getFormat())) {
            if (volume.getPath() != null) {
                vs = ApiDBUtils.getVolumeStatistics(volume.getPath());
            }
        } else if (volume.getFormat() == Storage.ImageFormat.OVA) {
            if (volume.getChainInfo() != null) {
                vs = ApiDBUtils.getVolumeStatistics(volume.getChainInfo());
            }
        }
        if (vs != null && vs.getPhysicalSize() > 0) {
            return vs.getPhysicalSize();
        }
        return volume.getSize();
    }

    @Override
    public String getConfigComponentName() {
        return KVMBackupExportService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                ImageTransferPollingInterval
        };
    }
}
