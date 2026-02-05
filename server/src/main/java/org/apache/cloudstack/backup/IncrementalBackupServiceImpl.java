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
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.CheckpointResponse;
import org.apache.cloudstack.api.response.ImageTransferResponse;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.backup.dao.ImageTransferDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
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
    private VolumeDetailsDao volumeDetailsDao;

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

    private Timer imageTransferTimer;

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

        if (vm.getState() != State.Running && vm.getState() != State.Stopped) {
            throw new CloudRuntimeException("VM must be running or stopped to start backup");
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
        Long fromCheckpointCreateTime = vm.getActiveCheckpointCreateTime();

        backup.setToCheckpointId(toCheckpointId);
        backup.setFromCheckpointId(fromCheckpointId);

        int nbdPort = allocateNbdPort();
        Long hostId = vm.getHostId() != null ? vm.getHostId() : vm.getLastHostId();
        backup.setNbdPort(nbdPort);
        backup.setHostId(hostId);
        // Will be changed later if incremental was done
        backup.setType("FULL");

        backup = backupDao.persist(backup);

        List<VolumeVO> volumes = volumeDao.findByInstance(vmId);
        Map<String, String> diskPathUuidMap = new HashMap<>();
        for (Volume vol : volumes) {
            String volumePath = getVolumePathForFileBasedBackend(vol);
            diskPathUuidMap.put(volumePath, vol.getUuid());
        }

        Host host = hostDao.findById(hostId);
        StartBackupCommand startCmd = new StartBackupCommand(
            vm.getInstanceName(),
            toCheckpointId,
            fromCheckpointId,
            fromCheckpointCreateTime,
            nbdPort,
            diskPathUuidMap,
            host.getPrivateIpAddress(),
            vm.getState() == State.Stopped
        );

        StartBackupAnswer answer;
        try {
            if (dummyOffering) {
                answer = new StartBackupAnswer(startCmd, true, "Dummy answer", System.currentTimeMillis());
            } else {
                answer = (StartBackupAnswer) agentManager.send(hostId, startCmd);
            }
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            backupDao.remove(backup.getId());
            throw new CloudRuntimeException("Failed to communicate with agent", e);
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

        VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM not found: " + vmId);
        }

        boolean dummyOffering = isDummyOffering(backup.getBackupOfferingId());

        List<ImageTransferVO> transfers = imageTransferDao.listByBackupId(backupId);
        if (CollectionUtils.isNotEmpty(transfers)) {
            throw new CloudRuntimeException("Image transfers not finalized for backup: " + backupId);
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
                throw new CloudRuntimeException("Failed to communicate with agent", e);
            }

            if (!answer.getResult()) {
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
            logger.debug("Would delete old checkpoint: " + oldCheckpointId);
        }

        // Delete backup session record
        backupDao.remove(backup.getId());

        return true;

    }

    private ImageTransferVO createDownloadImageTransfer(Long backupId, VolumeVO volume) {
        final String direction = ImageTransfer.Direction.download.toString();
        BackupVO backup = backupDao.findById(backupId);
        if (backup == null) {
            throw new CloudRuntimeException("Backup not found: " + backupId);
        }
        boolean dummyOffering = isDummyOffering(backup.getBackupOfferingId());

        String transferId = UUID.randomUUID().toString();
        Host host = hostDao.findById(backup.getHostId());

        VMInstanceVO vm = vmInstanceDao.findById(backup.getVmId());
        if (vm.getState() == State.Stopped) {
            String volumePath = getVolumePathForFileBasedBackend(volume);
            startNBDServer(transferId, direction, host, volume.getUuid(), volumePath, backup.getNbdPort());
        }

        CreateImageTransferCommand transferCmd = new CreateImageTransferCommand(
                transferId,
                host.getPrivateIpAddress(),
                volume.getUuid(),
                backup.getNbdPort(),
                direction,
                backup.getFromCheckpointId()
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

    private void startNBDServer(String transferId, String direction, Host host, String exportName, String volumePath, int nbdPort) {
        StartNBDServerAnswer nbdServerAnswer;
        StartNBDServerCommand nbdServerCmd = new StartNBDServerCommand(
                transferId,
                host.getPrivateIpAddress(),
                exportName,
                volumePath,
                nbdPort,
                direction
        );
        try {
            nbdServerAnswer = (StartNBDServerAnswer) agentManager.send(host.getId(), nbdServerCmd);
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

    private ImageTransferVO createUploadImageTransfer(VolumeVO volume) {
        final String direction = ImageTransfer.Direction.upload.toString();
        String transferId = UUID.randomUUID().toString();
        int nbdPort = allocateNbdPort();

        Long poolId = volume.getPoolId();
        StoragePoolVO storagePoolVO = primaryDataStoreDao.findById(poolId);
        Host host = getFirstHostFromStoragePool(storagePoolVO);
        String volumePath = getVolumePathForFileBasedBackend(volume);

        startNBDServer(transferId, direction, host, volume.getUuid(), volumePath, nbdPort);

        ImageTransferVO imageTransfer = new ImageTransferVO(
                transferId,
                null,
                volume.getId(),
                host.getId(),
                nbdPort,
                ImageTransferVO.Phase.transferring,
                ImageTransfer.Direction.upload,
                volume.getAccountId(),
                volume.getDomainId(),
                volume.getDataCenterId()
        );

        CreateImageTransferAnswer transferAnswer;
        CreateImageTransferCommand transferCmd = new CreateImageTransferCommand(
                transferId,
                host.getPrivateIpAddress(),
                volume.getUuid(),
                nbdPort,
                direction,
                null
        );

        EndPoint ssvm = _epSelector.findSsvm(volume.getDataCenterId());
        transferAnswer = (CreateImageTransferAnswer) ssvm.sendMessage(transferCmd);

        if (!transferAnswer.getResult()) {
            stopNbdServer(imageTransfer);
            throw new CloudRuntimeException("Failed to create image transfer: " + transferAnswer.getDetails());
        }


        imageTransfer.setTransferUrl(transferAnswer.getTransferUrl());
        imageTransfer.setSignedTicketId(transferAnswer.getImageTransferId());
        imageTransfer = imageTransferDao.persist(imageTransfer);
        return imageTransfer;

    }

    @Override
    public ImageTransferResponse createImageTransfer(CreateImageTransferCmd cmd) {
        ImageTransfer imageTransfer = createImageTransfer(cmd.getVolumeId(), cmd.getBackupId(), cmd.getDirection());
        if (imageTransfer instanceof ImageTransferVO) {
            ImageTransferVO imageTransferVO = (ImageTransferVO) imageTransfer;
            return toImageTransferResponse(imageTransferVO);
        }
        return toImageTransferResponse(imageTransferDao.findById(imageTransfer.getId()));
    }

    @Override
    public ImageTransfer createImageTransfer(long volumeId, Long backupId, ImageTransfer.Direction direction) {
        ImageTransfer imageTransfer;
        VolumeVO volume = volumeDao.findById(volumeId);

        ImageTransferVO existingTransfer = imageTransferDao.findByVolume(volume.getId());
        if (existingTransfer != null) {
            throw new CloudRuntimeException("Image transfer already in progress for volume: " + volume.getUuid());
        }

        if (ImageTransfer.Direction.upload.equals(direction)) {
            imageTransfer = createUploadImageTransfer(volume);
        } else if (ImageTransfer.Direction.download.equals(direction)) {
            imageTransfer = createDownloadImageTransfer(backupId, volume);
        } else {
            throw new CloudRuntimeException("Invalid direction: " + direction);
        }

        return imageTransferDao.findById(imageTransfer.getId());
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
            throw new CloudRuntimeException("Failed to communicate with agent", e);
        }

        VMInstanceVO vm = vmInstanceDao.findById(backup.getVmId());
        if (vm.getState() == State.Stopped) {
            boolean stopNbdServerResult = stopNbdServer(imageTransfer);
            if (!stopNbdServerResult) {
                throw new CloudRuntimeException("Failed to stop the nbd server");
            }
        }
    }

    private boolean stopNbdServer(ImageTransferVO imageTransfer) {
        String transferId = imageTransfer.getUuid();
        int nbdPort = imageTransfer.getNbdPort();
        String direction = imageTransfer.getDirection().toString();
        StopNBDServerCommand stopNbdServerCommand = new StopNBDServerCommand(transferId, direction, nbdPort);
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
        int nbdPort = imageTransfer.getNbdPort();
        String direction = imageTransfer.getDirection().toString();

        boolean stopNbdServerResult = stopNbdServer(imageTransfer);
        if (!stopNbdServerResult) {
            throw new CloudRuntimeException("Failed to stop the nbd server");
        }

        FinalizeImageTransferCommand finalizeCmd = new FinalizeImageTransferCommand(transferId, direction, nbdPort);
        EndPoint ssvm = _epSelector.findSsvm(imageTransfer.getDataCenterId());
        Answer answer = ssvm.sendMessage(finalizeCmd);

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
        return IncrementalBackupService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                ImageTransferPollingInterval
        };
    }
}
