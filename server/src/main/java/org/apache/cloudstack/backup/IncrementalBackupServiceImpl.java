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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.api.command.admin.backup.CreateImageTransferCmd;
import org.apache.cloudstack.api.command.admin.backup.DeleteVmCheckpointCmd;
import org.apache.cloudstack.api.command.admin.backup.FinalizeBackupCmd;
import org.apache.cloudstack.api.command.admin.backup.FinalizeImageTransferCmd;
import org.apache.cloudstack.api.command.admin.backup.ListImageTransfersCmd;
import org.apache.cloudstack.api.command.admin.backup.ListVmCheckpointsCmd;
import org.apache.cloudstack.api.command.admin.backup.StartBackupCmd;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.CheckpointResponse;
import org.apache.cloudstack.api.response.ImageTransferResponse;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.backup.dao.ImageTransferDao;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class IncrementalBackupServiceImpl extends ManagerBase implements IncrementalBackupService {

    @Inject
    private VMInstanceDao vmInstanceDao;

    @Inject
    private BackupDao backupDao;

    @Inject
    private ImageTransferDao imageTransferDao;

    @Inject
    private VolumeDao volumeDao;

    @Inject
    private AgentManager agentManager;

    @Inject
    private BackupOfferingDao backupOfferingDao;

    private static final int NBD_PORT_RANGE_START = 10809;
    private static final int NBD_PORT_RANGE_END = 10909;

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
    public BackupResponse startBackup(StartBackupCmd cmd) {
        Long vmId = cmd.getVmId();

        // Get VM
        VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM not found: " + vmId);
        }

        if (vm.getState() != State.Running) {
            throw new CloudRuntimeException("VM must be running to start backup");
        }

        // Check if backup already in progress
        Backup existingBackup = backupDao.findByVmId(vmId);
        if (existingBackup != null && existingBackup.getStatus() == Backup.Status.BackingUp) {
            throw new CloudRuntimeException("Backup already in progress for VM: " + vmId);
        }

        boolean dummyOffering = isDummyOffering(vm.getBackupOfferingId());

        // Create backup record
        BackupVO backup = new BackupVO();
        backup.setVmId(vmId);
        backup.setName(vmId + "-" + DateTime.now());
        backup.setAccountId(vm.getAccountId());
        backup.setDomainId(vm.getDomainId());
        // todo: set to Increment if it is incremental backup
        backup.setType("FULL");
        backup.setZoneId(vm.getDataCenterId());
        backup.setStatus(Backup.Status.BackingUp);
        backup.setBackupOfferingId(vm.getBackupOfferingId());
        backup.setDate(new Date());

        // Generate checkpoint IDs
        String toCheckpointId = "ckp-" + UUID.randomUUID().toString().substring(0, 8);
        String fromCheckpointId = vm.getActiveCheckpointId(); // null for first full backup

        backup.setToCheckpointId(toCheckpointId);
        backup.setFromCheckpointId(fromCheckpointId);

        // Allocate NBD port
        int nbdPort = allocateNbdPort();
        backup.setNbdPort(nbdPort);
        backup.setHostId(vm.getHostId());

        // Persist backup record
        backup = backupDao.persist(backup);

        // Get disk volume paths
        List<? extends Volume> volumes = volumeDao.findByInstance(vmId);
        Map<Long, String> diskVolumePaths = new HashMap<>();
        for (Volume vol : volumes) {
            diskVolumePaths.put(vol.getId(), vol.getPath());
        }

        // Send StartBackupCommand to agent
        StartBackupCommand startCmd = new StartBackupCommand(
            vm.getInstanceName(),
            vmId,
            toCheckpointId,
            fromCheckpointId,
            nbdPort,
            diskVolumePaths
        );

        try {
            StartBackupAnswer answer;

            if (dummyOffering) {
                answer = new StartBackupAnswer(startCmd, true, "Dummy answer", System.currentTimeMillis(), diskVolumePaths);
            } else {
                answer = (StartBackupAnswer) agentManager.send(vm.getHostId(), startCmd);
            }

            if (!answer.getResult()) {
                backupDao.remove(backup.getId());
                throw new CloudRuntimeException("Failed to start backup: " + answer.getDetails());
            }

            // Update backup with checkpoint creation time
            backup.setCheckpointCreateTime(answer.getCheckpointCreateTime());
            backupDao.update(backup.getId(), backup);

            // Return response
            BackupResponse response = new BackupResponse();
            response.setId(backup.getUuid());
            response.setVmId(vm.getUuid());
            response.setStatus(backup.getStatus());
            return response;

        } catch (AgentUnavailableException | OperationTimedoutException e) {
            backupDao.remove(backup.getId());
            throw new CloudRuntimeException("Failed to communicate with agent: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean finalizeBackup(FinalizeBackupCmd cmd) {
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

        // Get VM
        VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM not found: " + vmId);
        }

        boolean dummyOffering = isDummyOffering(vm.getBackupOfferingId());

        List<ImageTransferVO> transfers = imageTransferDao.listByBackupId(backupId);
        if (CollectionUtils.isNotEmpty(transfers)) {
            throw new CloudRuntimeException("Image transfers not finalized for backup: " + backupId);
        }

        // Send StopBackupCommand to agent
        StopBackupCommand stopCmd = new StopBackupCommand(vm.getInstanceName(), vmId, backupId);

        try {
            StopBackupAnswer answer;
            if (dummyOffering) {
                answer = new StopBackupAnswer(stopCmd, true, "Dummy answer");
            } else {
                answer = (StopBackupAnswer) agentManager.send(vm.getHostId(), stopCmd);
            }

            if (!answer.getResult()) {
                throw new CloudRuntimeException("Failed to stop backup: " + answer.getDetails());
            }

            // Update VM checkpoint tracking
            String oldCheckpointId = vm.getActiveCheckpointId();
            vm.setActiveCheckpointId(backup.getToCheckpointId());
            vm.setActiveCheckpointCreateTime(backup.getCheckpointCreateTime());
            vmInstanceDao.update(vmId, vm);

            // Delete old checkpoint if exists (POC: skip actual libvirt call)
            if (oldCheckpointId != null) {
                // In production: send command to delete oldCheckpointId via virsh checkpoint-delete
                logger.debug("Would delete old checkpoint: " + oldCheckpointId);
            }

            // Delete backup session record
            backupDao.remove(backup.getId());

            return true;

        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException("Failed to communicate with agent: " + e.getMessage(), e);
        }
    }

    @Override
    public ImageTransferResponse createImageTransfer(CreateImageTransferCmd cmd) {
        Long backupId = cmd.getBackupId();
        Long volumeId = cmd.getVolumeId();

        BackupVO backup = backupDao.findById(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Backup not found: " + backupId);
        }

        Volume volume = volumeDao.findById(volumeId);
        if (volume == null) {
            throw new CloudRuntimeException("Volume not found: " + volumeId);
        }

        VMInstanceVO vm = vmInstanceDao.findById(backup.getVmId());
        if (vm == null) {
            throw new CloudRuntimeException("VM not found: " + backup.getVmId());
        }
        boolean dummyOffering = isDummyOffering(vm.getBackupOfferingId());

        // Resolve device name (simplified for POC)
        List<? extends Volume> volumes = volumeDao.findByInstance(backup.getVmId());
        String deviceName = resolveDeviceName(volumes, volumeId);

        // Create CreateImageTransferCommand
        CreateImageTransferCommand transferCmd = new CreateImageTransferCommand(
            backup.getVmId(),
            backupId,
            volumeId,
            deviceName,
            backup.getNbdPort()
        );

        try {
            CreateImageTransferAnswer answer;
            if (dummyOffering) {
                answer = new CreateImageTransferAnswer(transferCmd, true, "Dummy answer", "image-transfer-id", "nbd://127.0.0.1:10809/vda", "initializing");
            } else {
                answer = (CreateImageTransferAnswer) agentManager.send(backup.getHostId(), transferCmd);
            }

            if (!answer.getResult()) {
                throw new CloudRuntimeException("Failed to create image transfer: " + answer.getDetails());
            }

            // Create ImageTransfer record
            ImageTransferVO imageTransfer = new ImageTransferVO(
                backupId,
                backup.getVmId(),
                volumeId,
                deviceName,
                backup.getHostId(),
                backup.getNbdPort(),
                ImageTransferVO.Phase.initializing,
                ImageTransfer.Direction.valueOf(cmd.getDirection()),
                backup.getAccountId(),
                backup.getDomainId()
            );
            imageTransfer.setTransferUrl(answer.getTransferUrl());
            imageTransfer.setSignedTicketId(answer.getImageTransferId());
            imageTransfer = imageTransferDao.persist(imageTransfer);

            // Return response
            ImageTransferResponse response = new ImageTransferResponse();
            response.setId(imageTransfer.getUuid());
            response.setBackupId(backup.getUuid());
            response.setVmId(vm.getUuid());
            response.setDiskId(volume.getUuid());
            response.setDeviceName(deviceName);
            response.setTransferUrl(answer.getTransferUrl());
            response.setPhase(ImageTransferVO.Phase.initializing.toString());
            response.setDirection(imageTransfer.getDirection().toString());
            response.setCreated(imageTransfer.getCreated());
            return response;

        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException("Failed to communicate with agent: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean finalizeImageTransfer(FinalizeImageTransferCmd cmd) {
        Long imageTransferId = cmd.getImageTransferId();

        ImageTransferVO imageTransfer = imageTransferDao.findById(imageTransferId);
        if (imageTransfer == null) {
            throw new CloudRuntimeException("Image transfer not found: " + imageTransferId);
        }

        // Mark as finished (NBD is closed in backup finalize, not here)
        imageTransfer.setPhase(ImageTransferVO.Phase.finished);
        imageTransferDao.update(imageTransferId, imageTransfer);
        imageTransferDao.remove(imageTransferId);

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
        if (vm.getActiveCheckpointId() != null) {
            CheckpointResponse response = new CheckpointResponse();
            response.setCheckpointId(vm.getActiveCheckpointId());
            response.setCreateTime(vm.getActiveCheckpointCreateTime());
            response.setIsActive(true);
            responses.add(response);
        }

        return responses;
    }

    @Override
    public boolean deleteVmCheckpoint(DeleteVmCheckpointCmd cmd) {
        // No-op for normal flow as per spec
        // Kept for API parity with oVirt
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

    // Helper methods

    private int allocateNbdPort() {
        // Simplified port allocation for POC
        Random random = new Random();
        return NBD_PORT_RANGE_START + random.nextInt(NBD_PORT_RANGE_END - NBD_PORT_RANGE_START);
    }

    private String resolveDeviceName(List<? extends Volume> volumes, Long targetDiskId) {
        // Simplified device name resolution for POC
        int index = 0;
        for (Volume vol : volumes) {
            if (Long.valueOf(vol.getId()).equals(targetDiskId)) {
                return "vd" + (char)('a' + index);
            }
            index++;
        }
        return "vda"; // fallback
    }

    private ImageTransferResponse toImageTransferResponse(ImageTransferVO imageTransfer) {
        ImageTransferResponse response = new ImageTransferResponse();
        response.setId(imageTransfer.getUuid());

        BackupVO backup = backupDao.findById(imageTransfer.getBackupId());
        VMInstanceVO vm = vmInstanceDao.findById(imageTransfer.getVmId());
        Volume volume = volumeDao.findById(imageTransfer.getDiskId());

        if (backup != null) response.setBackupId(backup.getUuid());
        if (vm != null) response.setVmId(vm.getUuid());
        if (volume != null) response.setDiskId(volume.getUuid());

        response.setDeviceName(imageTransfer.getDeviceName());
        response.setTransferUrl(imageTransfer.getTransferUrl());
        response.setPhase(imageTransfer.getPhase().toString());
        response.setCreated(imageTransfer.getCreated());

        return response;
    }
}
