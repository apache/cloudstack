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

import static org.apache.cloudstack.backup.BackupManager.BackupFrameworkEnabled;
import static org.apache.cloudstack.backup.BackupManager.BackupProviderPlugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
import org.apache.cloudstack.backup.dao.ImageTransferDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.jobs.AsyncJobExecutionContext;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.VmWorkJobVO;
import org.apache.cloudstack.jobs.JobInfo;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
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
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeStats;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.ReflectionUse;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceDetailVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.VmWork;
import com.cloud.vm.VmWorkConstants;
import com.cloud.vm.VmWorkJobHandler;
import com.cloud.vm.VmWorkJobHandlerProxy;
import com.cloud.vm.VmWorkSerializer;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;

@Component
public class KVMBackupExportServiceImpl extends ManagerBase implements KVMBackupExportService, VmWorkJobHandler {
    public static final String VM_WORK_JOB_HANDLER = KVMBackupExportServiceImpl.class.getSimpleName();
    private static final long BACKUP_FINALIZE_WAIT_CHECK_INTERVAL = 15 * 1000L;

    @Inject
    private VMInstanceDao vmInstanceDao;

    @Inject
    private VMInstanceDetailsDao vmInstanceDetailsDao;

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
    private HostDao hostDao;

    @Inject
    private PrimaryDataStoreDao primaryDataStoreDao;

    @Inject
    private StoragePoolHostDao storagePoolHostDao;

    @Inject
    AccountService accountService;

    @Inject
    AsyncJobManager asyncJobManager;

    private ScheduledExecutorService imageTransferStatusExecutor;

    VmWorkJobHandlerProxy jobHandlerProxy = new VmWorkJobHandlerProxy(this);

    private boolean isKVMBackupExportServiceSupported(Long zoneId) {
        return !BackupFrameworkEnabled.value() || StringUtils.equals("dummy", BackupProviderPlugin.valueIn(zoneId));
    }

    @Override
    public Backup createBackup(StartBackupCmd cmd) {
        Long vmId = cmd.getVmId();

        VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM not found: " + vmId);
        }

        if (!isKVMBackupExportServiceSupported(vm.getDataCenterId())) {
            throw new CloudRuntimeException("Veeam-KVM integration can not be used along with the " + BackupProviderPlugin.valueIn(vm.getDataCenterId()) +
                    " backup provider. Either set backup.framework.enabled to false or set the Zone level config backup.framework.provider.plugin to \"dummy\".");
        }

        if (vm.getState() != State.Running && vm.getState() != State.Stopped) {
            throw new CloudRuntimeException("VM must be running or stopped to start backup");
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
        backup.setBackupOfferingId(0L);
        backup.setDate(new Date());

        String toCheckpointId = "ckp-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, String> vmDetails = vmInstanceDetailsDao.listDetailsKeyPairs(vmId);
        String fromCheckpointId = vmDetails.get(VmDetailConstants.ACTIVE_CHECKPOINT_ID);

        backup.setToCheckpointId(toCheckpointId);
        backup.setFromCheckpointId(fromCheckpointId);
        backup.setType("FULL");

        Long hostId = vm.getHostId() != null ? vm.getHostId() : vm.getLastHostId();
        backup.setHostId(hostId);

        return backupDao.persist(backup);
    }

    protected void removeFailedBackup(BackupVO backup) {
        backup.setStatus(Backup.Status.Error);
        backupDao.update(backup.getId(), backup);
        backupDao.remove(backup.getId());
    }

    protected void queueBackupFinalizeWaitWorkJob(final VMInstanceVO vm, final BackupVO backup) {
        final CallContext context = CallContext.current();
        final Account callingAccount = context.getCallingAccount();
        final long callingUserId = context.getCallingUserId();

        VmWorkJobVO workJob = new VmWorkJobVO(context.getContextId());
        workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
        workJob.setCmd(VmWorkWaitForBackupFinalize.class.getName());
        workJob.setAccountId(callingAccount.getId());
        workJob.setUserId(callingUserId);
        workJob.setStep(VmWorkJobVO.Step.Starting);
        workJob.setVmType(VirtualMachine.Type.User);
        workJob.setVmInstanceId(vm.getId());
        workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

        VmWorkWaitForBackupFinalize workInfo = new VmWorkWaitForBackupFinalize(
                callingUserId, callingAccount.getId(), vm.getId(), VM_WORK_JOB_HANDLER, backup.getId());
        workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

        asyncJobManager.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());
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

        VMInstanceDetailVO lastCheckpointId = vmInstanceDetailsDao.findDetail(vmId, VmDetailConstants.LAST_CHECKPOINT_ID);
        if (lastCheckpointId != null) {
            try {
                sendDeleteCheckpointCommand(vm, lastCheckpointId.getValue());
            } catch (CloudRuntimeException e) {
                logger.warn("Failed to delete last checkpoint {} for VM {}, proceeding with backup start", lastCheckpointId.getValue(), vmId, e);
            }
        }

        Host host = hostDao.findById(hostId);
        Map<String, String> vmDetails = vmInstanceDetailsDao.listDetailsKeyPairs(vmId);
        String activeCkpCreateTimeStr = vmDetails.get(VmDetailConstants.ACTIVE_CHECKPOINT_CREATE_TIME);
        Long fromCheckpointCreateTime = activeCkpCreateTimeStr != null ? NumbersUtil.parseLong(activeCkpCreateTimeStr, 0L) : null;
        StartBackupCommand startCmd = new StartBackupCommand(
            vm.getInstanceName(),
            backup.getToCheckpointId(),
            backup.getFromCheckpointId(),
            fromCheckpointCreateTime,
            backup.getUuid(),
            diskPathUuidMap,
            vm.getState() == State.Stopped
        );

        StartBackupAnswer answer;
        try {
            answer = (StartBackupAnswer) agentManager.send(hostId, startCmd);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            removeFailedBackup(backup);
            logger.error("Failed to communicate with agent on {} for {} start", host, backup, e);
            throw new CloudRuntimeException("Failed to communicate with agent", e);
        }

        if (!answer.getResult()) {
            removeFailedBackup(backup);
            logger.error("Failed to start {} due to: {}", backup, answer.getDetails());
            throw new CloudRuntimeException("Failed to start backup: " + answer.getDetails());
        }

        // Update backup with checkpoint creation time
        backup.setCheckpointCreateTime(answer.getCheckpointCreateTime());
        updateBackupState(backup, Backup.Status.ReadyForTransfer);
        queueBackupFinalizeWaitWorkJob(vm, backup);
        return backup;
    }

    protected void updateBackupState(BackupVO backup, Backup.Status newStatus) {
        backup.setStatus(newStatus);
        backupDao.update(backup.getId(), backup);
    }

    private void updateVmCheckpoints(Long vmId, BackupVO backup) {
        Map<String, String> vmDetails = vmInstanceDetailsDao.listDetailsKeyPairs(vmId);
        String oldCheckpointId = vmDetails.get(VmDetailConstants.ACTIVE_CHECKPOINT_ID);
        String oldCreateTimeStr = vmDetails.get(VmDetailConstants.ACTIVE_CHECKPOINT_CREATE_TIME);
        if (oldCheckpointId != null && oldCreateTimeStr != null) {
            vmInstanceDetailsDao.addDetail(vmId, VmDetailConstants.LAST_CHECKPOINT_ID, oldCheckpointId, false);
            vmInstanceDetailsDao.addDetail(vmId, VmDetailConstants.LAST_CHECKPOINT_CREATE_TIME, oldCreateTimeStr, false);
        }
        String newCheckpointId = backup.getToCheckpointId();
        Long newCreateTime = backup.getCheckpointCreateTime();
        if (newCheckpointId != null && newCreateTime != null) {
            vmInstanceDetailsDao.addDetail(vmId, VmDetailConstants.ACTIVE_CHECKPOINT_ID, backup.getToCheckpointId(), false);
            vmInstanceDetailsDao.addDetail(vmId, VmDetailConstants.ACTIVE_CHECKPOINT_CREATE_TIME, String.valueOf(newCreateTime), false);
        } else {
            logger.error("New checkpoint details are missing for backup {} and vm {}", backup.getId(), vmId);
        }
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

        updateBackupState(backup, Backup.Status.FinalizingTransfer);

        List<ImageTransferVO> transfers = imageTransferDao.listByBackupId(backupId);
        for (ImageTransferVO transfer : transfers) {
            if (transfer.getPhase() != ImageTransferVO.Phase.finished) {
                logger.warn("Finalize called for backup {} while Image transfer {} is not finalized, attempting to finalize it", backup.getUuid(), transfer.getUuid());
                finalizeImageTransfer(transfer.getId());
            }
        }

        if (vm.getState() == State.Running) {
            StopBackupCommand stopCmd = new StopBackupCommand(vm.getInstanceName(), vmId, backupId);

            StopBackupAnswer answer;
            try {
                answer = (StopBackupAnswer) agentManager.send(backup.getHostId(), stopCmd);
            } catch (AgentUnavailableException | OperationTimedoutException e) {
                removeFailedBackup(backup);
                throw new CloudRuntimeException("Failed to communicate with agent", e);
            }

            if (!answer.getResult()) {
                removeFailedBackup(backup);
                throw new CloudRuntimeException("Failed to stop backup: " + answer.getDetails());
            }
        }

        updateVmCheckpoints(vmId, backup);

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
        if (ImageTransfer.Backend.file.equals(backend)) {
            throw new CloudRuntimeException("File backend is not supported for download");
        }

        String transferId = UUID.randomUUID().toString();

        String socket = backup.getUuid();
        VMInstanceVO vm = vmInstanceDao.findById(backup.getVmId());
        if (vm.getState() == State.Stopped) {
            Map<String, String> vmDetails = vmInstanceDetailsDao.listDetailsKeyPairs(backup.getVmId());
            String volumePath = getVolumePathForFileBasedBackend(volume);
            startNBDServer(transferId, direction, backup.getHostId(), volume.getUuid(), volumePath, vmDetails.get(VmDetailConstants.ACTIVE_CHECKPOINT_ID));
            socket = transferId;
        }

        HostVO backupHost = hostDao.findById(backup.getHostId());
        if (backupHost == null) {
            throw new CloudRuntimeException("Host not found for backup: " + backupId);
        }
        int idleTimeoutSec = ImageTransferIdleTimeoutSeconds.valueIn(backupHost.getDataCenterId());
        CreateImageTransferCommand transferCmd = new CreateImageTransferCommand(
                transferId,
                direction,
                volume.getUuid(),
                socket,
                backup.getFromCheckpointId(),
                idleTimeoutSec);

        try {
            CreateImageTransferAnswer answer;
            answer = (CreateImageTransferAnswer) agentManager.send(backup.getHostId(), transferCmd);

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

    private HostVO getRandomHostFromStoragePool(StoragePoolVO storagePool) {
        List<HostVO> hosts;
        switch (storagePool.getScope()) {
            case CLUSTER:
                hosts = hostDao.findByClusterId(storagePool.getClusterId());
                Collections.shuffle(hosts);
                return hosts.get(0);
            case ZONE:
                hosts = hostDao.findByDataCenterId(storagePool.getDataCenterId());
                Collections.shuffle(hosts);
                return hosts.get(0);
            case HOST:
                List<StoragePoolHostVO> storagePoolHostVOs = storagePoolHostDao.listByPoolId(storagePool.getId());
                Collections.shuffle(storagePoolHostVOs);
                return hostDao.findById(storagePoolHostVOs.get(0).getHostId());
            default:
                throw new CloudRuntimeException("Unsupported storage pool scope: " + storagePool.getScope());
        }
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

    private String getVolumePathPrefix(StoragePoolVO storagePool) {
        if (ScopeType.HOST.equals(storagePool.getScope())) {
                return storagePool.getPath();
        }
        switch (storagePool.getPoolType()) {
            case NetworkFilesystem:
                return String.format("/mnt/%s", storagePool.getUuid());
            case SharedMountPoint:
                return storagePool.getPath();
            default:
                throw new CloudRuntimeException("Unsupported storage pool type for file based image transfer: " + storagePool.getPoolType());
        }
    }

    private String getVolumePathForFileBasedBackend(Volume volume) {
        StoragePoolVO storagePool = primaryDataStoreDao.findById(volume.getPoolId());
        String volumePathPrefix = getVolumePathPrefix(storagePool);
        return volumePathPrefix + "/" + volume.getPath();
    }

    private ImageTransferVO createUploadImageTransfer(VolumeVO volume, ImageTransfer.Backend backend) {
        final String direction = ImageTransfer.Direction.upload.toString();
        String transferId = UUID.randomUUID().toString();

        Long poolId = volume.getPoolId();
        StoragePoolVO storagePool = poolId == null ? null : primaryDataStoreDao.findById(poolId);
        if (storagePool == null) {
            throw new CloudRuntimeException("Storage pool cannot be determined for volume: " + volume.getUuid());
        }

        Host host = getRandomHostFromStoragePool(storagePool);
        String volumePath = getVolumePathForFileBasedBackend(volume);
        int idleTimeoutSec = ImageTransferIdleTimeoutSeconds.valueIn(host.getDataCenterId());

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
                    volumePath,
                    idleTimeoutSec);

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
                    null,
                    idleTimeoutSec);
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
        User callingUser = CallContext.current().getCallingUser();
        ImageTransfer imageTransfer;
        VolumeVO volume = volumeDao.findById(volumeId);
        accountService.checkAccess(callingUser, volume);

        if (volume == null) {
            throw new CloudRuntimeException("Volume not found with the specified Id");
        }

        if (!isKVMBackupExportServiceSupported(volume.getDataCenterId())) {
            throw new CloudRuntimeException("Veeam-KVM integration can not be used along with the " + BackupProviderPlugin.valueIn(volume.getDataCenterId()) +
                    " backup provider. Either set backup.framework.enabled to false or set the Zone level config backup.framework.provider.plugin to \"dummy\".");
        }

        ImageTransferVO existingTransfer = imageTransferDao.findByVolume(volume.getId());
        if (existingTransfer != null) {
            throw new CloudRuntimeException("Image transfer already exists for volume: " + volume.getUuid());
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
        finalizeImageTransfer(imageTransferId);
        return true;
    }

    private void finalizeDownloadImageTransfer(ImageTransferVO imageTransfer) {

        String transferId = imageTransfer.getUuid();
        FinalizeImageTransferCommand finalizeCmd = new FinalizeImageTransferCommand(transferId);

        BackupVO backup = backupDao.findById(imageTransfer.getBackupId());

        Answer answer;
        try {
            answer = agentManager.send(backup.getHostId(), finalizeCmd);
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

    private CheckpointResponse createCheckpointResponse(String checkpointId, String createTime, boolean isActive) {
        CheckpointResponse response = new CheckpointResponse();
        response.setObjectName("checkpoint");
        response.setId(checkpointId);
        Long createTimeSeconds = createTime != null ? NumbersUtil.parseLong(createTime, 0L) : 0L;
        response.setCreated(Date.from(Instant.ofEpochSecond(createTimeSeconds)));
        response.setIsActive(isActive);
        return response;
    }

    @Override
    public List<CheckpointResponse> listVmCheckpoints(ListVmCheckpointsCmd cmd) {
        Long vmId = cmd.getVmId();
        VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("VM not found: " + vmId);
        }
        List<CheckpointResponse> responses = new ArrayList<>();

        Map<String, String> details = vmInstanceDetailsDao.listDetailsKeyPairs(vmId);
        String activeCheckpointId = details.get(VmDetailConstants.ACTIVE_CHECKPOINT_ID);
        if (activeCheckpointId != null) {
            responses.add(createCheckpointResponse(activeCheckpointId, details.get(VmDetailConstants.ACTIVE_CHECKPOINT_CREATE_TIME), true));
        }
        String lastCheckpointId = details.get(VmDetailConstants.LAST_CHECKPOINT_ID);
        if (lastCheckpointId != null) {
            responses.add(createCheckpointResponse(lastCheckpointId, details.get(VmDetailConstants.LAST_CHECKPOINT_CREATE_TIME), false));
        }
        return responses;
    }

    private void sendDeleteCheckpointCommand(VMInstanceVO vm, String checkpointId) {
        Long hostId = vm.getHostId() != null ? vm.getHostId() : vm.getLastHostId();

        Map<String, String> diskPathUuidMap = new HashMap<>();
        if (vm.getState() == State.Stopped) {
            List<VolumeVO> volumes = volumeDao.findByInstance(vm.getId());
            for (Volume vol : volumes) {
                diskPathUuidMap.put(getVolumePathForFileBasedBackend(vol), vol.getUuid());
            }
        }

        DeleteVmCheckpointCommand deleteCmd = new DeleteVmCheckpointCommand(
                vm.getInstanceName(),
                checkpointId,
                diskPathUuidMap,
                vm.getState() == State.Stopped);

        Answer answer;
        try {
            answer = agentManager.send(hostId, deleteCmd);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            logger.error("Failed to communicate with agent to delete checkpoint for VM {}", vm.getId(), e);
            throw new CloudRuntimeException("Failed to communicate with agent", e);
        }

        if (answer == null || !answer.getResult()) {
            String err = answer != null ? answer.getDetails() : "null answer";
            throw new CloudRuntimeException("Failed to delete checkpoint: " + err);
        }
    }

    @Override
    public boolean deleteVmCheckpoint(DeleteVmCheckpointCmd cmd) {
        VMInstanceVO vm = vmInstanceDao.findById(cmd.getVmId());
        if (vm == null) {
            throw new CloudRuntimeException("VM not found: " + cmd.getVmId());
        }
        if (!isKVMBackupExportServiceSupported(vm.getDataCenterId())) {
            throw new CloudRuntimeException("Veeam-KVM integration can not be used along with the " + BackupProviderPlugin.valueIn(vm.getDataCenterId()) +
                    " backup provider. Either set backup.framework.enabled to false or set the Zone level config backup.framework.provider.plugin to \"dummy\".");
        }

        if (vm.getState() != State.Running && vm.getState() != State.Stopped) {
            throw new CloudRuntimeException("VM must be running or stopped to delete checkpoint");
        }

        long vmId = cmd.getVmId();
        Map<String, String> details = vmInstanceDetailsDao.listDetailsKeyPairs(vmId);
        String activeCheckpointId = details.get(VmDetailConstants.ACTIVE_CHECKPOINT_ID);
        if (activeCheckpointId == null || !activeCheckpointId.equals(cmd.getCheckpointId())) {
            logger.error("Checkpoint ID {} to delete does not match active checkpoint ID for VM {}", cmd.getCheckpointId(), vmId);
            return true;
        }

        sendDeleteCheckpointCommand(vm, activeCheckpointId);
        revertVmCheckpointDetailsAfterActiveDelete(vmId, details);

        return true;
    }

    private void revertVmCheckpointDetailsAfterActiveDelete(long vmId, Map<String, String> detailsBeforeDelete) {
        String lastId = detailsBeforeDelete.get(VmDetailConstants.LAST_CHECKPOINT_ID);
        String lastTime = detailsBeforeDelete.get(VmDetailConstants.LAST_CHECKPOINT_CREATE_TIME);
        if (lastId != null) {
            vmInstanceDetailsDao.addDetail(vmId, VmDetailConstants.ACTIVE_CHECKPOINT_ID, lastId, false);
            vmInstanceDetailsDao.addDetail(vmId, VmDetailConstants.ACTIVE_CHECKPOINT_CREATE_TIME, lastTime, false);
            vmInstanceDetailsDao.removeDetail(vmId, VmDetailConstants.LAST_CHECKPOINT_ID);
            vmInstanceDetailsDao.removeDetail(vmId, VmDetailConstants.LAST_CHECKPOINT_CREATE_TIME);
        } else {
            vmInstanceDetailsDao.removeDetail(vmId, VmDetailConstants.ACTIVE_CHECKPOINT_ID);
            vmInstanceDetailsDao.removeDetail(vmId, VmDetailConstants.ACTIVE_CHECKPOINT_CREATE_TIME);
        }
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        if (ExposeKVMBackupExportServiceApis.value()) {
            cmdList.add(StartBackupCmd.class);
            cmdList.add(FinalizeBackupCmd.class);
            cmdList.add(CreateImageTransferCmd.class);
            cmdList.add(FinalizeImageTransferCmd.class);
            cmdList.add(ListImageTransfersCmd.class);
            cmdList.add(ListVmCheckpointsCmd.class);
            cmdList.add(DeleteVmCheckpointCmd.class);
        }
        return cmdList;
    }

    private ImageTransferResponse toImageTransferResponse(ImageTransferVO imageTransferVO) {
        ImageTransferResponse response = new ImageTransferResponse();
        response.setId(imageTransferVO.getUuid());
        Long backupId = imageTransferVO.getBackupId();
        if (backupId != null) {
            Backup backup = backupDao.findByIdIncludingRemoved(backupId);
            response.setBackupId(backup.getUuid());
        }
        Long volumeId = imageTransferVO.getVolumeId();
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
        imageTransferStatusExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Image-Transfer-Status-Executor"));
        long pollingInterval = ImageTransferPollingInterval.value();
        imageTransferStatusExecutor.scheduleAtFixedRate(new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                try {
                    pollImageTransferProgress();
                } catch (final Throwable t) {
                    logger.warn("Catch throwable in image transfer poll task ", t);
                }
            }
        }, pollingInterval, pollingInterval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        imageTransferStatusExecutor.shutdown();
        return true;
    }

    @ReflectionUse
    public Pair<JobInfo.Status, String> orchestrateWaitForBackupFinalize(VmWorkWaitForBackupFinalize work) {
        return waitForBackupTerminalState(work.getBackupId());
    }

    @Override
    public Pair<JobInfo.Status, String> handleVmWorkJob(VmWork work) throws Exception {
        return jobHandlerProxy.handleVmWorkJob(work);
    }

    protected Pair<JobInfo.Status, String> waitForBackupTerminalState(final long backupId) {
        while (true) {
            final BackupVO backup = backupDao.findByIdIncludingRemoved(backupId);
            if (backup == null) {
                RuntimeException ex = new CloudRuntimeException(String.format("Backup %d not found while waiting for finalize", backupId));
                return new Pair<>(JobInfo.Status.FAILED, asyncJobManager.marshallResultObject(ex));
            }

            if (backup.getStatus() == Backup.Status.BackedUp) {
                return new Pair<>(JobInfo.Status.SUCCEEDED, asyncJobManager.marshallResultObject(backup.getId()));
            }

            if (backup.getStatus() == Backup.Status.Failed || backup.getStatus() == Backup.Status.Error) {
                RuntimeException ex = new CloudRuntimeException(String.format("Backup %d reached terminal failure state: %s", backupId, backup.getStatus()));
                return new Pair<>(JobInfo.Status.FAILED, asyncJobManager.marshallResultObject(ex));
            }
            logger.debug("{} is not in a terminal state, current state: {}, waiting {}ms to check again",
                    backup, backup.getStatus(), BACKUP_FINALIZE_WAIT_CHECK_INTERVAL);
            try {
                Thread.sleep(BACKUP_FINALIZE_WAIT_CHECK_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                    RuntimeException ex = new CloudRuntimeException(String.format("Interrupted while waiting for backup %d finalize", backupId), e);
                    return new Pair<>(JobInfo.Status.FAILED, asyncJobManager.marshallResultObject(ex));
                }
            }
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
                        VolumeVO volume = volumeDao.findById(transfer.getVolumeId());
                        if (volume == null) {
                            logger.warn("Volume not found for image transfer: {}", transfer.getUuid());
                            imageTransferDao.remove(transfer.getId());
                            continue;
                        }
                        transferVolumeMap.put(transfer.getId(), volume);

                        String transferId = transfer.getUuid();
                        transferIds.add(transferId);

                        if (volume.getPath() == null) {
                            logger.warn("Volume path is null for image transfer: {}", transfer.getUuid());
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
                        continue;
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
                ImageTransferPollingInterval,
                ImageTransferIdleTimeoutSeconds,
                ExposeKVMBackupExportServiceApis
        };
    }
}
