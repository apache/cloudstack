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

        int removedImages = 0;
        List<String> failedImages = new ArrayList<>();
        for (VmwareCbtDiskTO disk : cmd.getDisks()) {
            String imageName = getRbdCleanupImageName(cmd.getMigrationUuid(), disk.getTargetPath());
            if (StringUtils.isBlank(imageName)) {
                continue;
            }
            try {
                if (targetPool.deletePhysicalDisk(imageName, Storage.ImageFormat.RAW)) {
                    removedImages++;
                } else {
                    failedImages.add(imageName);
                }
            } catch (RuntimeException e) {
                logger.warn("Unable to delete VMware CBT RBD image {} for migration {}: {}",
                        imageName, cmd.getMigrationUuid(), StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
                failedImages.add(imageName);
            }
        }
        if (CollectionUtils.isNotEmpty(failedImages)) {
            throw new IllegalStateException(String.format("Unable to delete VMware CBT RBD target image(s) %s from storage pool %s",
                    StringUtils.join(failedImages, ", "), targetPool.getUuid()));
        }
        return removedImages;
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
