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
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
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

    @Inject
    private HostDao hostDao;

    @Inject
    private PrimaryDataStoreDao primaryDataStoreDao;

    @Inject
    EndPointSelector _epSelector;

    private static final int NBD_PORT_RANGE_START = 10809;
    private static final int NBD_PORT_RANGE_END = 10909;
    private static final boolean DATAPLANE_PROXY_MODE = true;

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

        VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM not found: " + vmId);
        }

        if (vm.getState() != State.Running) {
            throw new CloudRuntimeException("VM must be running to start backup");
        }

        Backup existingBackup = backupDao.findByVmId(vmId);
        if (existingBackup != null && existingBackup.getStatus() == Backup.Status.BackingUp) {
            throw new CloudRuntimeException("Backup already in progress for VM: " + vmId);
        }

        boolean dummyOffering = isDummyOffering(vm.getBackupOfferingId());

        BackupVO backup = new BackupVO();
        backup.setVmId(vmId);
        backup.setName(vmId + "-" + DateTime.now());
        backup.setAccountId(vm.getAccountId());
        backup.setDomainId(vm.getDomainId());
        backup.setZoneId(vm.getDataCenterId());
        backup.setStatus(Backup.Status.BackingUp);
        backup.setBackupOfferingId(vm.getBackupOfferingId());
        backup.setDate(new Date());

        String toCheckpointId = "ckp-" + UUID.randomUUID().toString().substring(0, 8);
        String fromCheckpointId = vm.getActiveCheckpointId();

        backup.setToCheckpointId(toCheckpointId);
        backup.setFromCheckpointId(fromCheckpointId);

        int nbdPort = allocateNbdPort();
        backup.setNbdPort(nbdPort);
        backup.setHostId(vm.getHostId());
        // Will be changed later if incremental was done
        backup.setType("FULL");

        backup = backupDao.persist(backup);

        List<VolumeVO> volumes = volumeDao.findByInstance(vmId);
        Map<String, String> diskVolumePaths = new HashMap<>();
        for (Volume vol : volumes) {
            diskVolumePaths.put(vol.getUuid(), vol.getPath());
        }

        Host host = hostDao.findById(vm.getHostId());
        StartBackupCommand startCmd = new StartBackupCommand(
            vm.getInstanceName(),
            toCheckpointId,
            fromCheckpointId,
            nbdPort,
            diskVolumePaths,
            host.getPrivateIpAddress()
        );

        try {
            StartBackupAnswer answer;

            if (dummyOffering) {
                answer = new StartBackupAnswer(startCmd, true, "Dummy answer", System.currentTimeMillis());
            } else {
                answer = (StartBackupAnswer) agentManager.send(vm.getHostId(), startCmd);
            }

            if (!answer.getResult()) {
                backupDao.remove(backup.getId());
                throw new CloudRuntimeException("Failed to start backup: " + answer.getDetails());
            }

            // Update backup with checkpoint creation time
            backup.setCheckpointCreateTime(answer.getCheckpointCreateTime());
            if (Boolean.TRUE.equals(answer.getIncremental())) {
                // todo: set it in the backend
                backup.setType("Incremental");
            }
            backupDao.update(backup.getId(), backup);

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

    private ImageTransferVO createDownloadImageTransfer(CreateImageTransferCmd cmd, VolumeVO volume) {
        Long backupId = cmd.getBackupId();
        BackupVO backup = backupDao.findById(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Backup not found: " + backupId);
        }
        boolean dummyOffering = isDummyOffering(backup.getBackupOfferingId());

        String transferId = UUID.randomUUID().toString();
        Host host = hostDao.findById(backup.getHostId());
        CreateImageTransferCommand transferCmd = new CreateImageTransferCommand(
                transferId,
                host.getPrivateIpAddress(),
                volume.getUuid(),
                backup.getNbdPort(),
                cmd.getDirection().toString()
        );

        try {
            CreateImageTransferAnswer answer;
            if (dummyOffering) {
                answer = new CreateImageTransferAnswer(transferCmd, true, "Dummy answer", "image-transfer-id", "nbd://127.0.0.1:10809/vda");
            } else if (DATAPLANE_PROXY_MODE) {
                EndPoint ssvm = _epSelector.findSsvm(backup.getZoneId());
                answer = (CreateImageTransferAnswer) ssvm.sendMessage(transferCmd);
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
                    backup.getNbdPort(),
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
            throw new CloudRuntimeException("Failed to communicate with agent: " + e.getMessage(), e);
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

    private ImageTransferVO createUploadImageTransfer(CreateImageTransferCmd cmd, VolumeVO volume) {
        String transferId = UUID.randomUUID().toString();

        int nbdPort = allocateNbdPort();
        Long poolId = volume.getPoolId();
        StoragePoolVO storagePoolVO = primaryDataStoreDao.findById(poolId);
        Host host = getFirstHostFromStoragePool(storagePoolVO);

        // todo: This only works with file based storage (not ceph, linbit)
        String volumePath = String.format("/mnt/%s/%s", storagePoolVO.getUuid(), volume.getPath());
        StartNBDServerAnswer nbdServerAnswer;
        StartNBDServerCommand nbdServerCmd = new StartNBDServerCommand(
                transferId,
                host.getPrivateIpAddress(),
                volume.getUuid(),
                volumePath,
                nbdPort,
                cmd.getDirection().toString()
        );

        try {
            nbdServerAnswer = (StartNBDServerAnswer) agentManager.send(host.getId(), nbdServerCmd);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException("Failed to communicate with agent: " + e.getMessage(), e);
        }
        if (!nbdServerAnswer.getResult()) {
            throw new CloudRuntimeException("Failed to start the NBD server");
        }

        CreateImageTransferAnswer transferAnswer;
        CreateImageTransferCommand transferCmd = new CreateImageTransferCommand(
                transferId,
                host.getPrivateIpAddress(),
                volume.getUuid(),
                nbdPort,
                cmd.getDirection().toString()
        );

        EndPoint ssvm = _epSelector.findSsvm(volume.getDataCenterId());
        transferAnswer = (CreateImageTransferAnswer) ssvm.sendMessage(transferCmd);

        if (!transferAnswer.getResult()) {
            StopNBDServerCommand stopNbdServerCommand = new StopNBDServerCommand(transferId, cmd.getDirection().toString(), nbdPort);
            throw new CloudRuntimeException("Failed to create image transfer: " + transferAnswer.getDetails());
        }

        ImageTransferVO imageTransfer = new ImageTransferVO(
                transferId,
                null,
                volume.getId(),
                host.getId(),
                nbdPort,
                ImageTransferVO.Phase.initializing,
                ImageTransfer.Direction.upload,
                volume.getAccountId(),
                volume.getDomainId(),
                volume.getDataCenterId()
        );

        imageTransfer.setTransferUrl(transferAnswer.getTransferUrl());
        imageTransfer.setSignedTicketId(transferAnswer.getImageTransferId());
        imageTransfer = imageTransferDao.persist(imageTransfer);
        return imageTransfer;

    }

    @Override
    public ImageTransferResponse createImageTransfer(CreateImageTransferCmd cmd) {
        ImageTransfer imageTransfer;
        Long volumeId = cmd.getVolumeId();
        VolumeVO volume = volumeDao.findById(cmd.getVolumeId());

        ImageTransferVO existingTransfer = imageTransferDao.findByVolume(volume.getId());
        if (existingTransfer != null) {
            throw new CloudRuntimeException("Image transfer already in progress for volume: " + volume.getUuid());
        }

        if (cmd.getDirection().equals(ImageTransfer.Direction.upload)) {
            imageTransfer = createUploadImageTransfer(cmd, volume);
        } else if (cmd.getDirection().equals(ImageTransfer.Direction.download)) {
            imageTransfer = createDownloadImageTransfer(cmd, volume);
        } else {
            throw new CloudRuntimeException("Invalid direction: " + cmd.getDirection());
        }

        ImageTransferVO imageTransferVO = imageTransferDao.findById(imageTransfer.getId());
        ImageTransferResponse response = toImageTransferResponse(imageTransferVO);
        return response;
    }

    private void finalizeDownloadImageTransfer(ImageTransferVO imageTransfer) {

        String transferId = imageTransfer.getUuid();
        int nbdPort = imageTransfer.getNbdPort();
        String direction = imageTransfer.getDirection().toString();
        FinalizeImageTransferCommand finalizeCmd = new FinalizeImageTransferCommand(transferId, direction, nbdPort);

        BackupVO backup = backupDao.findById(imageTransfer.getBackupId());
        boolean dummyOffering = isDummyOffering(backup.getBackupOfferingId());

        try {
            Answer answer;
            if (dummyOffering) {
                answer = new Answer(finalizeCmd, true, "Image transfer finalized.");
            } else if (DATAPLANE_PROXY_MODE) {
                EndPoint ssvm = _epSelector.findSsvm(backup.getZoneId());
                answer = ssvm.sendMessage(finalizeCmd);
            } else {
                answer = agentManager.send(backup.getHostId(), finalizeCmd);
            }

            if (!answer.getResult()) {
                throw new CloudRuntimeException("Failed to finalize image transfer: " + answer.getDetails());
            }

        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException("Failed to communicate with agent: " + e.getMessage(), e);
        }
    }

    private void finalizeUploadImageTransfer(ImageTransferVO imageTransfer) {
        String transferId = imageTransfer.getUuid();
        int nbdPort = imageTransfer.getNbdPort();
        String direction = imageTransfer.getDirection().toString();

        StopNBDServerCommand stopNbdServerCommand = new StopNBDServerCommand(transferId, direction, nbdPort);
        Answer answer;
        try {
            answer = agentManager.send(imageTransfer.getHostId(), stopNbdServerCommand);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException("Failed to communicate with agent: " + e.getMessage(), e);
        }
        if (!answer.getResult()) {
            throw new CloudRuntimeException("Failed to stop the nbd server");
        }

        FinalizeImageTransferCommand finalizeCmd = new FinalizeImageTransferCommand(transferId, direction, nbdPort);
        EndPoint ssvm = _epSelector.findSsvm(imageTransfer.getDataCenterId());
        answer = ssvm.sendMessage(finalizeCmd);

        if (!answer.getResult()) {
            throw new CloudRuntimeException("Failed to finalize image transfer: " + answer.getDetails());
        }
    }

    @Override
    public boolean finalizeImageTransfer(FinalizeImageTransferCmd cmd) {
        Long imageTransferId = cmd.getImageTransferId();

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

    private int getRandomNbdPort() {
        Random random = new Random();
        return NBD_PORT_RANGE_START + random.nextInt(NBD_PORT_RANGE_END - NBD_PORT_RANGE_START);
    }

    private int allocateNbdPort() {
        int port = getRandomNbdPort();
        while (imageTransferDao.findByNbdPort(port) != null) {
            port = getRandomNbdPort();
        }
        return port;
    }

    private ImageTransferResponse toImageTransferResponse(ImageTransferVO imageTransferVO) {
        ImageTransferResponse response = new ImageTransferResponse();
        response.setId(imageTransferVO.getUuid());
        Long backupId = imageTransferVO.getBackupId();
        if (backupId != null) {
            Backup backup = backupDao.findById(backupId);
            response.setBackupId(backup.getUuid());
        }
        Long volumeId = imageTransferVO.getDiskId();
        Volume volume = volumeDao.findById(volumeId);
        response.setDiskId(volume.getUuid());
        response.setTransferUrl(imageTransferVO.getTransferUrl());
        response.setPhase(ImageTransferVO.Phase.initializing.toString());
        response.setDirection(imageTransferVO.getDirection().toString());
        response.setCreated(imageTransferVO.getCreated());
        return response;
    }
}
