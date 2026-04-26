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
package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.FreezeThawVMCommand;
import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.agent.api.storage.CreateDiskOnlyVmSnapshotAnswer;
import com.cloud.agent.api.storage.CreateDiskOnlyVmSnapshotCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuCommand;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.commons.lang3.StringUtils;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.cloud.storage.Volume;

@ResourceWrapper(handles = CreateDiskOnlyVmSnapshotCommand.class)
public class LibvirtCreateDiskOnlyVMSnapshotCommandWrapper extends CommandWrapper<CreateDiskOnlyVmSnapshotCommand, Answer, LibvirtComputingResource> {
    protected static final String NVRAM_SNAPSHOT_DIR = ".cloudstack-vm-snapshot-nvram";

    private static final String SNAPSHOT_XML = "<domainsnapshot>\n" +
            "<name>%s</name>\n" +
            "<memory snapshot='no'/>\n" +
            "<disks> \n" +
            "%s" +
            "</disks> \n" +
            "</domainsnapshot>";

    private static final String TAG_DISK_SNAPSHOT = "<disk name='%s' snapshot='external'>\n" +
            "<source file='%s'/>\n" +
            "</disk>\n";

    @Override
    public Answer execute(CreateDiskOnlyVmSnapshotCommand cmd, LibvirtComputingResource resource) {
        VirtualMachine.State state = cmd.getVmState();

        if (VirtualMachine.State.Running.equals(state)) {
            return takeDiskOnlyVmSnapshotOfRunningVm(cmd, resource);
        }

        return takeDiskOnlyVmSnapshotOfStoppedVm(cmd, resource);
    }

    protected Answer takeDiskOnlyVmSnapshotOfRunningVm(CreateDiskOnlyVmSnapshotCommand cmd, LibvirtComputingResource resource) {
        String vmName = cmd.getVmName();
        logger.info("Taking disk-only VM snapshot of running VM [{}].", vmName);

        Domain dm = null;
        String nvramSnapshotPath = null;
        boolean suspendedByThisWrapper = false;
        boolean filesystemsFrozenByThisWrapper = false;
        CreateDiskOnlyVmSnapshotAnswer answer = null;
        String postSnapshotCleanupIssue = null;
        try {
            LibvirtUtilitiesHelper libvirtUtilitiesHelper = resource.getLibvirtUtilitiesHelper();
            Connect conn = libvirtUtilitiesHelper.getConnection();
            List<VolumeObjectTO> volumeObjectTOS = cmd.getVolumeTOs();
            List<LibvirtVMDef.DiskDef> disks = resource.getDisks(conn, vmName);

            dm = resource.getDomain(conn, vmName);

            if (dm == null) {
                answer = new CreateDiskOnlyVmSnapshotAnswer(cmd, false,
                        String.format("Creation of disk-only VM Snapshot failed as we could not find the VM [%s].", vmName), null, null);
                return answer;
            }

            VMSnapshotTO target = cmd.getTarget();
            Pair<String, Map<String, Pair<Long, String>>> snapshotXmlAndVolumeToNewPathMap = createSnapshotXmlAndNewVolumePathMap(volumeObjectTOS, disks, target, resource);
            if (shouldFreezeVmFilesystemsForSnapshot(cmd)) {
                // The guest-agent freeze flushes guest filesystems; suspend below prevents concurrent UEFI NVRAM writes.
                freezeVmFilesystems(dm, vmName);
                filesystemsFrozenByThisWrapper = true;
                verifyVmFilesystemsFrozen(dm, vmName);
            }
            if (shouldSuspendVmForSnapshot(cmd)) {
                suspendedByThisWrapper = suspendVmIfNeeded(dm);
            }
            nvramSnapshotPath = backupNvramIfNeeded(cmd, resource);

            dm.snapshotCreateXML(snapshotXmlAndVolumeToNewPathMap.first(), getFlagsToUseForRunningVmSnapshotCreation(target, filesystemsFrozenByThisWrapper));

            postSnapshotCleanupIssue = recoverVmAfterSnapshot(dm, vmName, suspendedByThisWrapper, filesystemsFrozenByThisWrapper, postSnapshotCleanupIssue);
            filesystemsFrozenByThisWrapper = false;
            suspendedByThisWrapper = false;

            answer = new CreateDiskOnlyVmSnapshotAnswer(cmd, true, null, snapshotXmlAndVolumeToNewPathMap.second(), nvramSnapshotPath);
        } catch (LibvirtException | IOException e) {
            String errorMsg = String.format("Creation of disk-only VM snapshot for VM [%s] failed due to %s.", vmName, e.getMessage());
            logger.error(errorMsg, e);
            cleanupNvramSnapshotIfNeeded(cmd, resource, nvramSnapshotPath);
            if (StringUtils.contains(e.getMessage(), "QEMU guest agent is not connected")) {
                errorMsg = "QEMU guest agent is not connected. If the VM has been recently started, it might connect soon. Otherwise the VM does not have the" +
                        " guest agent installed; thus the QuiesceVM parameter is not supported.";
                answer = new CreateDiskOnlyVmSnapshotAnswer(cmd, false, errorMsg, null, null);
            } else {
                answer = new CreateDiskOnlyVmSnapshotAnswer(cmd, false, e.getMessage(), null, null);
            }
        } finally {
            if (dm != null) {
                postSnapshotCleanupIssue = recoverVmAfterSnapshot(dm, vmName, suspendedByThisWrapper, filesystemsFrozenByThisWrapper, postSnapshotCleanupIssue);
                try {
                    dm.free();
                } catch (LibvirtException l) {
                    logger.trace("Ignoring libvirt error.", l);
                }
            }
        }

        if (answer != null && StringUtils.isNotBlank(postSnapshotCleanupIssue)) {
            answer = new CreateDiskOnlyVmSnapshotAnswer(cmd, answer.getResult(),
                    appendSnapshotOperationIssue(answer.getDetails(), postSnapshotCleanupIssue), answer.getMapVolumeToSnapshotSizeAndNewVolumePath(), answer.getNvramSnapshotPath());
        }
        return answer;
    }

    protected Answer takeDiskOnlyVmSnapshotOfStoppedVm(CreateDiskOnlyVmSnapshotCommand cmd, LibvirtComputingResource resource) {
        String vmName = cmd.getVmName();
        logger.info("Taking disk-only VM snapshot of stopped VM [{}].", vmName);

        Map<String, Pair<Long, String>> mapVolumeToSnapshotSizeAndNewVolumePath = new HashMap<>();
        String nvramSnapshotPath = null;

        List<VolumeObjectTO> volumeObjectTos = cmd.getVolumeTOs();
        KVMStoragePoolManager storagePoolMgr = resource.getStoragePoolMgr();
        try {
            nvramSnapshotPath = backupNvramIfNeeded(cmd, resource);
            for (VolumeObjectTO volumeObjectTO : volumeObjectTos) {
                PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO) volumeObjectTO.getDataStore();
                KVMStoragePool kvmStoragePool = storagePoolMgr.getStoragePool(primaryDataStoreTO.getPoolType(), primaryDataStoreTO.getUuid());

                String snapshotPath = UUID.randomUUID().toString();
                String snapshotFullPath = kvmStoragePool.getLocalPathFor(snapshotPath);
                QemuImgFile newDelta = new QemuImgFile(snapshotFullPath, QemuImg.PhysicalDiskFormat.QCOW2);

                String currentDeltaFullPath = kvmStoragePool.getLocalPathFor(volumeObjectTO.getPath());
                QemuImgFile currentDelta = new QemuImgFile(currentDeltaFullPath, QemuImg.PhysicalDiskFormat.QCOW2);

                QemuImg qemuImg = new QemuImg(0);

                logger.debug("Creating new delta for volume [{}] as part of the disk-only VM snapshot process for VM [{}].", volumeObjectTO.getUuid(), vmName);
                qemuImg.create(newDelta, currentDelta);

                mapVolumeToSnapshotSizeAndNewVolumePath.put(volumeObjectTO.getUuid(), new Pair<>(getFileSize(currentDeltaFullPath), snapshotPath));
            }
        } catch (LibvirtException | QemuImgException | IOException e) {
            logger.error("Exception while creating disk-only VM snapshot for VM [{}]. Deleting leftover deltas.", vmName, e);
            for (VolumeObjectTO volumeObjectTO : volumeObjectTos) {
                Pair<Long, String> volSizeAndNewPath = mapVolumeToSnapshotSizeAndNewVolumePath.get(volumeObjectTO.getUuid());
                PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO) volumeObjectTO.getDataStore();
                KVMStoragePool kvmStoragePool = storagePoolMgr.getStoragePool(primaryDataStoreTO.getPoolType(), primaryDataStoreTO.getUuid());

                if (volSizeAndNewPath == null) {
                    continue;
                }
                try {
                    Files.deleteIfExists(Path.of(kvmStoragePool.getLocalPathFor(volSizeAndNewPath.second())));
                } catch (IOException ex) {
                    logger.warn("Tried to delete leftover snapshot at [{}] failed.", volSizeAndNewPath.second(), ex);
                }
            }
            cleanupNvramSnapshotIfNeeded(cmd, resource, nvramSnapshotPath);
            return new Answer(cmd, e);
        }

        return new CreateDiskOnlyVmSnapshotAnswer(cmd, true, null, mapVolumeToSnapshotSizeAndNewVolumePath, nvramSnapshotPath);
    }

    protected int getFlagsToUseForRunningVmSnapshotCreation(VMSnapshotTO target, boolean filesystemsFrozenByThisWrapper) {
        int flags = target.getQuiescevm() && !filesystemsFrozenByThisWrapper ? Domain.SnapshotCreateFlags.QUIESCE : 0;
        flags += Domain.SnapshotCreateFlags.DISK_ONLY +
                Domain.SnapshotCreateFlags.ATOMIC +
                Domain.SnapshotCreateFlags.NO_METADATA;
        return flags;
    }

    protected Pair<String, Map<String, Pair<Long, String>>> createSnapshotXmlAndNewVolumePathMap(List<VolumeObjectTO> volumeObjectTOS, List<LibvirtVMDef.DiskDef> disks, VMSnapshotTO target, LibvirtComputingResource resource) {
        StringBuilder stringBuilder = new StringBuilder();
        Map<String, Pair<Long, String>> volumeObjectToNewPathMap = new HashMap<>();

        for (VolumeObjectTO volumeObjectTO : volumeObjectTOS) {
            LibvirtVMDef.DiskDef diskdef = resource.getDiskWithPathOfVolumeObjectTO(disks, volumeObjectTO);
            String newPath = UUID.randomUUID().toString();
            stringBuilder.append(String.format(TAG_DISK_SNAPSHOT, diskdef.getDiskLabel(), resource.getSnapshotTemporaryPath(diskdef.getDiskPath(), newPath)));

            long snapSize = getFileSize(diskdef.getDiskPath());

            volumeObjectToNewPathMap.put(volumeObjectTO.getUuid(), new Pair<>(snapSize, newPath));
        }

        String snapshotXml = String.format(SNAPSHOT_XML, target.getSnapshotName(), stringBuilder);
        return new Pair<>(snapshotXml, volumeObjectToNewPathMap);
    }

    protected long getFileSize(String path) {
        return new File(path).length();
    }

    protected String backupNvramIfNeeded(CreateDiskOnlyVmSnapshotCommand cmd, LibvirtComputingResource resource) throws IOException, LibvirtException {
        if (!cmd.isUefiEnabled()) {
            return null;
        }

        String activeNvramPath = resource.getUefiNvramPath(cmd.getVmUuid());
        if (StringUtils.isBlank(activeNvramPath) || !Files.exists(Path.of(activeNvramPath))) {
            throw new IOException(String.format("Unable to find the active UEFI NVRAM file for VM [%s].", cmd.getVmName()));
        }

        VolumeObjectTO rootVolume = getRootVolume(cmd.getVolumeTOs());
        PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO) rootVolume.getDataStore();
        KVMStoragePool storagePool = resource.getStoragePoolMgr().getStoragePool(primaryDataStoreTO.getPoolType(), primaryDataStoreTO.getUuid());

        String nvramSnapshotPath = getNvramSnapshotRelativePath(cmd.getTarget().getId());
        Path targetPath = Path.of(storagePool.getLocalPathFor(nvramSnapshotPath));
        Files.createDirectories(targetPath.getParent());
        Files.copy(Path.of(activeNvramPath), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return nvramSnapshotPath;
    }

    protected void cleanupNvramSnapshotIfNeeded(CreateDiskOnlyVmSnapshotCommand cmd, LibvirtComputingResource resource, String nvramSnapshotPath) {
        if (StringUtils.isBlank(nvramSnapshotPath)) {
            return;
        }

        try {
            VolumeObjectTO rootVolume = getRootVolume(cmd.getVolumeTOs());
            PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO) rootVolume.getDataStore();
            KVMStoragePool storagePool = resource.getStoragePoolMgr().getStoragePool(primaryDataStoreTO.getPoolType(), primaryDataStoreTO.getUuid());
            Files.deleteIfExists(Path.of(storagePool.getLocalPathFor(nvramSnapshotPath)));
        } catch (Exception e) {
            logger.warn("Failed to clean up temporary NVRAM snapshot [{}] for VM [{}].", nvramSnapshotPath, cmd.getVmName(), e);
        }
    }

    protected String getNvramSnapshotRelativePath(Long vmSnapshotId) {
        return String.format("%s/%s.fd", NVRAM_SNAPSHOT_DIR, vmSnapshotId);
    }

    protected VolumeObjectTO getRootVolume(List<VolumeObjectTO> volumeObjectTos) {
        return volumeObjectTos.stream()
                .filter(volumeObjectTO -> Volume.Type.ROOT.equals(volumeObjectTO.getVolumeType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to locate the root volume while handling the VM snapshot."));
    }

    protected boolean shouldSuspendVmForSnapshot(CreateDiskOnlyVmSnapshotCommand cmd) {
        return cmd.isUefiEnabled();
    }

    protected boolean shouldFreezeVmFilesystemsForSnapshot(CreateDiskOnlyVmSnapshotCommand cmd) {
        return cmd.isUefiEnabled() && cmd.getTarget().getQuiescevm();
    }

    protected boolean suspendVmIfNeeded(Domain domain) throws LibvirtException {
        if (domain.getInfo().state == org.libvirt.DomainInfo.DomainState.VIR_DOMAIN_PAUSED) {
            return false;
        }

        domain.suspend();
        return true;
    }

    protected void freezeVmFilesystems(Domain domain, String vmName) throws LibvirtException, IOException {
        String result = getResultOfQemuCommand(FreezeThawVMCommand.FREEZE, domain);
        if (isQemuAgentErrorResponse(result)) {
            throw new IOException(String.format("Failed to freeze VM [%s] filesystems before taking the disk-only VM snapshot. Result: %s", vmName, result));
        }
    }

    protected void verifyVmFilesystemsFrozen(Domain domain, String vmName) throws LibvirtException, IOException {
        String status = getResultOfQemuCommand(FreezeThawVMCommand.STATUS, domain);
        if (StringUtils.isBlank(status)) {
            throw new IOException(String.format("Failed to verify VM [%s] filesystem freeze state before taking the disk-only VM snapshot. Result: %s", vmName, status));
        }

        JsonObject statusObject;
        try {
            JsonElement statusElement = new JsonParser().parse(status);
            if (!statusElement.isJsonObject()) {
                throw new IOException(String.format("Failed to verify VM [%s] filesystem freeze state before taking the disk-only VM snapshot. Result: %s", vmName, status));
            }
            statusObject = statusElement.getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException(String.format("Failed to verify VM [%s] filesystem freeze state before taking the disk-only VM snapshot. Result: %s", vmName, status), e);
        }

        if (statusObject.has("error")) {
            throw new IOException(String.format("Failed to verify VM [%s] filesystem freeze state before taking the disk-only VM snapshot. Result: %s", vmName, status));
        }

        JsonElement returnElement = statusObject.get("return");
        if (returnElement == null || !returnElement.isJsonPrimitive() || !returnElement.getAsJsonPrimitive().isString()) {
            throw new IOException(String.format("Failed to verify VM [%s] filesystem freeze state before taking the disk-only VM snapshot. Result: %s", vmName, status));
        }

        String statusResult = returnElement.getAsString();
        if (!FreezeThawVMCommand.FREEZE.equals(statusResult)) {
            throw new IOException(String.format("Failed to freeze VM [%s] filesystems before taking the disk-only VM snapshot. Status: %s", vmName, statusResult));
        }
    }

    protected boolean thawVmFilesystemsIfNeeded(Domain domain, String vmName, boolean filesystemsFrozenByThisWrapper) {
        if (!filesystemsFrozenByThisWrapper) {
            return true;
        }
        return thawVmFilesystemsIfNeeded(domain, vmName);
    }

    protected boolean thawVmFilesystemsIfNeeded(Domain domain, String vmName) {
        try {
            String result = getResultOfQemuCommand(FreezeThawVMCommand.THAW, domain);
            if (isQemuAgentErrorResponse(result)) {
                logger.warn("Failed to thaw VM [{}] filesystems after taking the disk-only VM snapshot. Result: {}", vmName, result);
                return false;
            }
            return true;
        } catch (LibvirtException e) {
            logger.warn("Failed to thaw VM [{}] filesystems after taking the disk-only VM snapshot.", vmName, e);
            return false;
        }
    }

    protected boolean isQemuAgentErrorResponse(String result) {
        if (StringUtils.isBlank(result) || result.startsWith("error")) {
            return true;
        }

        try {
            JsonElement resultElement = new JsonParser().parse(result);
            return resultElement.isJsonObject() && resultElement.getAsJsonObject().has("error");
        } catch (RuntimeException e) {
            return false;
        }
    }

    protected String getResultOfQemuCommand(String cmd, Domain domain) throws LibvirtException {
        if (cmd.equals(FreezeThawVMCommand.FREEZE)) {
            return domain.qemuAgentCommand(QemuCommand.buildQemuCommand(QemuCommand.AGENT_FREEZE, null), 10, 0);
        } else if (cmd.equals(FreezeThawVMCommand.THAW)) {
            return domain.qemuAgentCommand(QemuCommand.buildQemuCommand(QemuCommand.AGENT_THAW, null), 10, 0);
        } else if (cmd.equals(FreezeThawVMCommand.STATUS)) {
            return domain.qemuAgentCommand(QemuCommand.buildQemuCommand(QemuCommand.AGENT_FREEZE_STATUS, null), 10, 0);
        }
        return null;
    }

    protected boolean resumeVmIfNeeded(Domain domain, String vmName, boolean suspendedByThisWrapper) {
        if (!suspendedByThisWrapper) {
            return true;
        }
        return resumeVmIfNeeded(domain, vmName);
    }

    protected boolean resumeVmIfNeeded(Domain domain, String vmName) {
        try {
            if (domain.getInfo().state == org.libvirt.DomainInfo.DomainState.VIR_DOMAIN_PAUSED) {
                domain.resume();
            }
            return true;
        } catch (LibvirtException e) {
            logger.warn("Failed to resume VM [{}] after taking the disk-only VM snapshot.", vmName, e);
            return false;
        }
    }

    protected String recoverVmAfterSnapshot(Domain domain, String vmName, boolean suspendedByThisWrapper, boolean filesystemsFrozenByThisWrapper, String currentIssue) {
        if (suspendedByThisWrapper && !resumeVmIfNeeded(domain, vmName)) {
            currentIssue = appendSnapshotOperationIssue(currentIssue,
                    String.format("VM [%s] could not be resumed after taking the disk-only snapshot. Guest may still be paused.", vmName));
        }
        if (filesystemsFrozenByThisWrapper && !thawVmFilesystemsIfNeeded(domain, vmName)) {
            currentIssue = appendSnapshotOperationIssue(currentIssue,
                    String.format("VM [%s] filesystems could not be thawed after taking the disk-only snapshot. Guest may still be frozen.", vmName));
        }
        return currentIssue;
    }

    protected String appendSnapshotOperationIssue(String currentIssue, String newIssue) {
        if (StringUtils.isBlank(newIssue)) {
            return currentIssue;
        }
        if (StringUtils.isBlank(currentIssue)) {
            return newIssue;
        }
        return currentIssue + " " + newIssue;
    }
}
