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
package com.cloud.hypervisor.kvm.resource.wrapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.VmwareCbtCleanupCommand;
import com.cloud.agent.api.VmwareCbtMigrationAnswer;
import com.cloud.agent.api.to.VmwareCbtDiskTO;
import com.cloud.agent.api.to.VmwareCbtTargetStorageType;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = VmwareCbtCleanupCommand.class)
public class LibvirtVmwareCbtCleanupCommandWrapper extends CommandWrapper<VmwareCbtCleanupCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(VmwareCbtCleanupCommand cmd, LibvirtComputingResource serverResource) {
        if (!cmd.getRemovePartialTargetDisks()) {
            String msg = String.format("VMware CBT cleanup for migration %s skipped target disk cleanup by command policy.",
                    cmd.getMigrationUuid());
            logger.info(msg);
            return new VmwareCbtMigrationAnswer(cmd, true, msg, cmd.getMigrationUuid());
        }

        try {
            if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW) {
                int removedImages = deleteRbdTargetImages(cmd, serverResource.getStoragePoolMgr());
                String msg = String.format("VMware CBT cleanup for migration %s removed %s replicated RBD target image(s).",
                        cmd.getMigrationUuid(), removedImages);
                logger.info(msg);
                return new VmwareCbtMigrationAnswer(cmd, true, msg, cmd.getMigrationUuid());
            }

            if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RAW_BLOCK_DEVICE) {
                int removedVolumes = deleteBlockDeviceTargetVolumes(cmd, serverResource.getStoragePoolMgr());
                String msg = String.format("VMware CBT cleanup for migration %s removed %s replicated block device target volume(s).",
                        cmd.getMigrationUuid(), removedVolumes);
                logger.info(msg);
                return new VmwareCbtMigrationAnswer(cmd, true, msg, cmd.getMigrationUuid());
            }

            Set<Path> migrationDirectories = getMigrationDirectories(cmd);
            int removedDirectories = 0;
            for (Path migrationDirectory : migrationDirectories) {
                if (deleteMigrationDirectory(migrationDirectory)) {
                    removedDirectories++;
                }
            }
            String msg = String.format("VMware CBT cleanup for migration %s removed %s replicated target directorie(s).",
                    cmd.getMigrationUuid(), removedDirectories);
            logger.info(msg);
            return new VmwareCbtMigrationAnswer(cmd, true, msg, cmd.getMigrationUuid());
        } catch (Exception e) {
            String msg = String.format("Unable to clean up VMware CBT migration %s on host %s: %s",
                    cmd.getMigrationUuid(), serverResource.getPrivateIp(),
                    StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            logger.error(msg, e);
            return new VmwareCbtMigrationAnswer(cmd, false, msg, cmd.getMigrationUuid());
        }
    }

    private int deleteRbdTargetImages(VmwareCbtCleanupCommand cmd, KVMStoragePoolManager storagePoolMgr) {
        KVMStoragePool targetPool = getTargetStoragePool(cmd, storagePoolMgr);
        if (targetPool == null || targetPool.getType() != Storage.StoragePoolType.RBD) {
            throw new IllegalArgumentException(String.format("VMware CBT migration %s requires an RBD destination storage pool for RBD cleanup",
                    cmd.getMigrationUuid()));
        }
        if (CollectionUtils.isEmpty(cmd.getDisks())) {
            return 0;
        }

        killInFlightCopyProcesses(String.format("cloudstack-cbt-%s-", cmd.getMigrationUuid()), cmd.getMigrationUuid());

        int removedImages = 0;
        List<String> failedImages = new ArrayList<>();
        for (VmwareCbtDiskTO disk : cmd.getDisks()) {
            String imageName = getRbdCleanupImageName(cmd.getMigrationUuid(), disk.getTargetPath());
            if (StringUtils.isBlank(imageName)) {
                continue;
            }
            if (deletePhysicalDiskWithRetries(targetPool, imageName, cmd.getMigrationUuid())) {
                removedImages++;
            } else {
                failedImages.add(imageName);
            }
        }
        if (CollectionUtils.isNotEmpty(failedImages)) {
            throw new IllegalStateException(String.format("Unable to delete VMware CBT RBD target image(s) %s from storage pool %s",
                    StringUtils.join(failedImages, ", "), targetPool.getUuid()));
        }
        return removedImages;
    }

    private int deleteBlockDeviceTargetVolumes(VmwareCbtCleanupCommand cmd, KVMStoragePoolManager storagePoolMgr) {
        KVMStoragePool targetPool = getTargetStoragePool(cmd, storagePoolMgr);
        if (targetPool == null || targetPool.getType() != Storage.StoragePoolType.Linstor) {
            throw new IllegalArgumentException(String.format("VMware CBT migration %s requires a Linstor destination storage pool for block device cleanup",
                    cmd.getMigrationUuid()));
        }
        if (CollectionUtils.isEmpty(cmd.getDisks())) {
            return 0;
        }

        killInFlightCopyProcesses(String.format("cbt-%s-", StringUtils.defaultString(cmd.getMigrationUuid()).replace("-", "").substring(0, 8)),
                cmd.getMigrationUuid());

        int removedVolumes = 0;
        List<String> failedVolumes = new ArrayList<>();
        for (VmwareCbtDiskTO disk : cmd.getDisks()) {
            String volumeName = getBlockDeviceCleanupVolumeName(cmd.getMigrationUuid(), disk.getTargetPath());
            if (StringUtils.isBlank(volumeName)) {
                continue;
            }
            if (deletePhysicalDiskWithRetries(targetPool, volumeName, cmd.getMigrationUuid())) {
                removedVolumes++;
            } else {
                failedVolumes.add(volumeName);
            }
        }
        if (CollectionUtils.isNotEmpty(failedVolumes)) {
            throw new IllegalStateException(String.format("Unable to delete VMware CBT block device target volume(s) %s from storage pool %s",
                    StringUtils.join(failedVolumes, ", "), targetPool.getUuid()));
        }
        return removedVolumes;
    }

    /**
     * Block device target names carry a short migration marker ("cbt-" plus the first
     * eight non-dash characters of the migration UUID) instead of the full RBD-style
     * marker, because LINSTOR resource names are limited to 48 characters.
     */
    private String getBlockDeviceCleanupVolumeName(String migrationUuid, String targetPath) {
        String normalizedTargetPath = StringUtils.defaultString(targetPath).replace('\\', '/');
        String volumeName = StringUtils.contains(normalizedTargetPath, "/") ? StringUtils.substringAfterLast(normalizedTargetPath, "/") : normalizedTargetPath;
        String marker = String.format("cbt-%s-", StringUtils.defaultString(migrationUuid).replace("-", "").substring(0, 8));
        if (!StringUtils.startsWith(volumeName, marker)) {
            logger.warn("Skipping VMware CBT block device cleanup target {} because it does not start with marker {}", targetPath, marker);
            return null;
        }
        return volumeName;
    }

    /**
     * A cancelled migration's initial/delta sync may still be copying into the
     * target volumes when this cleanup runs (the data-plane copy is not aborted by
     * the cancel itself). Any surviving copy keeps the target device/image busy -
     * volume deletion then fails and the copy keeps burning VDDK/network/storage on
     * a dead migration. Kill processes whose command line references this
     * migration's marker-guarded target names before deleting them: this matches
     * the nbdcopy / qemu-img convert / qemu-io processes (their arguments carry the
     * marker-named target); the nbdkit --run parent exits with its child.
     */
    protected void killInFlightCopyProcesses(String marker, String migrationUuid) {
        if (StringUtils.isBlank(marker)) {
            return;
        }
        int exitValue = Script.runSimpleBashScriptForExitValue(String.format("pkill -f -- %s", quoteShellArgument(marker)));
        if (exitValue == 0) {
            logger.info("Killed in-flight copy process(es) matching marker {} for VMware CBT migration {}", marker, migrationUuid);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Retries volume deletion briefly: right after in-flight copies are killed the
     * device may still be held open (DRBD demotion, qemu teardown) for a moment.
     */
    protected boolean deletePhysicalDiskWithRetries(KVMStoragePool targetPool, String volumeName, String migrationUuid) {
        final int attempts = 3;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                if (targetPool.deletePhysicalDisk(volumeName, Storage.ImageFormat.RAW)) {
                    return true;
                }
                logger.warn("Attempt {}/{} to delete VMware CBT target {} for migration {} returned false",
                        attempt, attempts, volumeName, migrationUuid);
            } catch (RuntimeException e) {
                logger.warn("Attempt {}/{} to delete VMware CBT target {} for migration {} failed: {}",
                        attempt, attempts, volumeName, migrationUuid,
                        StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            }
            if (attempt < attempts) {
                try {
                    Thread.sleep(2000L * attempt);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private String quoteShellArgument(String value) {
        return "'" + StringUtils.defaultString(value).replace("'", "'\"'\"'") + "'";
    }

    private KVMStoragePool getTargetStoragePool(VmwareCbtCleanupCommand cmd, KVMStoragePoolManager storagePoolMgr) {
        Storage.StoragePoolType poolType = cmd.getDestinationStoragePoolType();
        String poolUuid = StringUtils.trimToNull(cmd.getDestinationStoragePoolUuid());
        if (poolType != null && StringUtils.isNotBlank(poolUuid)) {
            return storagePoolMgr.getStoragePool(poolType, poolUuid);
        }
        return null;
    }

    private String getRbdCleanupImageName(String migrationUuid, String targetPath) {
        String normalizedTargetPath = StringUtils.defaultString(targetPath).replace('\\', '/');
        String imageName = StringUtils.contains(normalizedTargetPath, "/") ? StringUtils.substringAfterLast(normalizedTargetPath, "/") : normalizedTargetPath;
        String marker = String.format("cloudstack-cbt-%s-", migrationUuid);
        if (!StringUtils.contains(imageName, marker)) {
            logger.warn("Skipping VMware CBT RBD cleanup target {} because it does not contain marker {}", targetPath, marker);
            return null;
        }
        return imageName;
    }

    private Set<Path> getMigrationDirectories(VmwareCbtCleanupCommand cmd) {
        Set<Path> migrationDirectories = new HashSet<>();
        if (CollectionUtils.isEmpty(cmd.getDisks())) {
            return migrationDirectories;
        }

        for (VmwareCbtDiskTO disk : cmd.getDisks()) {
            Path migrationDirectory = getMigrationDirectory(cmd.getMigrationUuid(), disk.getTargetPath());
            if (migrationDirectory != null) {
                migrationDirectories.add(migrationDirectory);
            }
        }
        return migrationDirectories;
    }

    private Path getMigrationDirectory(String migrationUuid, String targetPath) {
        if (StringUtils.isAnyBlank(migrationUuid, targetPath)) {
            return null;
        }

        Path normalizedTargetPath = Path.of(targetPath).normalize();
        String normalized = normalizedTargetPath.toString().replace('\\', '/');
        String marker = String.format("/cloudstack-cbt/%s/", migrationUuid);
        int markerIndex = normalized.indexOf(marker);
        if (markerIndex < 0) {
            logger.warn("Skipping VMware CBT cleanup target {} because it is not under {}", targetPath, marker);
            return null;
        }
        String rootPath = normalized.substring(0, markerIndex + marker.length() - 1);
        return Path.of(rootPath).normalize();
    }

    private boolean deleteMigrationDirectory(Path migrationDirectory) throws Exception {
        if (!Files.isDirectory(migrationDirectory)) {
            return false;
        }
        try (Stream<Path> stream = Files.walk(migrationDirectory)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                Files.deleteIfExists(path);
            }
        }
        return true;
    }
}
