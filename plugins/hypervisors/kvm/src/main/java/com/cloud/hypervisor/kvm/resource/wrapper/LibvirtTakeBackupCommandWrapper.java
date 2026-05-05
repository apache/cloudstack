//
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
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import com.amazonaws.util.CollectionUtils;
import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.backup.BackupAnswer;
import org.apache.cloudstack.backup.TakeBackupCommand;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@ResourceWrapper(handles = TakeBackupCommand.class)
public class LibvirtTakeBackupCommandWrapper extends CommandWrapper<TakeBackupCommand, Answer, LibvirtComputingResource> {
    private static final Integer EXIT_CLEANUP_FAILED = 20;
    @Override
    public Answer execute(TakeBackupCommand command, LibvirtComputingResource libvirtComputingResource) {
        final String vmName = command.getVmName();
        final String backupPath = command.getBackupPath();
        final String backupRepoType = command.getBackupRepoType();
        final String backupRepoAddress = command.getBackupRepoAddress();
        final String mountOptions = command.getMountOptions();
        List<PrimaryDataStoreTO> volumePools = command.getVolumePools();
        final List<String> volumePaths = command.getVolumePaths();
        KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
        int timeout = command.getWait() > 0 ? command.getWait() * 1000 : libvirtComputingResource.getCmdsTimeout();

        List<String> diskPaths = new ArrayList<>();
        if (Objects.nonNull(volumePaths)) {
            for (int idx = 0; idx < volumePaths.size(); idx++) {
                PrimaryDataStoreTO volumePool = volumePools.get(idx);
                String volumePath = volumePaths.get(idx);
                if (volumePool.getPoolType() != Storage.StoragePoolType.RBD) {
                    diskPaths.add(volumePath);
                } else {
                    KVMStoragePool volumeStoragePool = storagePoolMgr.getStoragePool(volumePool.getPoolType(), volumePool.getUuid());
                    String rbdDestVolumeFile = KVMPhysicalDisk.RBDStringBuilder(volumeStoragePool, volumePath);
                    diskPaths.add(rbdDestVolumeFile);
                }
            }
        }

        List<String> argv = new ArrayList<>(Arrays.asList(
                libvirtComputingResource.getNasBackupPath(),
                "-o", "backup",
                "-v", vmName,
                "-t", backupRepoType,
                "-s", backupRepoAddress,
                "-m", Objects.nonNull(mountOptions) ? mountOptions : "",
                "-p", backupPath,
                "-q", command.getQuiesce() != null && command.getQuiesce() ? "true" : "false",
                "-d", diskPaths.isEmpty() ? "" : String.join(",", diskPaths)
        ));
        // Incremental NAS backup args (only added when the orchestrator asked for full/inc mode).
        if (command.getMode() != null && !command.getMode().isEmpty()) {
            argv.add("-M");
            argv.add(command.getMode());
        }
        if (command.getBitmapNew() != null && !command.getBitmapNew().isEmpty()) {
            argv.add("--bitmap-new");
            argv.add(command.getBitmapNew());
        }
        if (command.getBitmapParent() != null && !command.getBitmapParent().isEmpty()) {
            argv.add("--bitmap-parent");
            argv.add(command.getBitmapParent());
        }
        if (command.getParentPath() != null && !command.getParentPath().isEmpty()) {
            argv.add("--parent-path");
            argv.add(command.getParentPath());
        }

        List<String[]> commands = new ArrayList<>();
        commands.add(argv.toArray(new String[0]));

        Pair<Integer, String> result = Script.executePipedCommands(commands, timeout);

        if (result.first() != 0) {
            logger.debug("Failed to take VM backup: " + result.second());
            BackupAnswer answer = new BackupAnswer(command, false, result.second().trim());
            if (result.first() == EXIT_CLEANUP_FAILED) {
                logger.debug("Backup cleanup failed");
                answer.setNeedsCleanup(true);
            }
            return answer;
        }

        // Strip out our incremental marker lines before parsing size, so the legacy
        // numeric-suffix parser keeps working.
        String stdout = result.second().trim();
        String bitmapCreated = null;
        String bitmapRecreated = null;
        boolean incrementalFallback = false;
        StringBuilder filtered = new StringBuilder();
        for (String line : stdout.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("BITMAP_CREATED=")) {
                bitmapCreated = trimmed.substring("BITMAP_CREATED=".length());
                continue;
            }
            if (trimmed.startsWith("BITMAP_RECREATED=")) {
                bitmapRecreated = trimmed.substring("BITMAP_RECREATED=".length());
                continue;
            }
            if (trimmed.startsWith("INCREMENTAL_FALLBACK=")) {
                incrementalFallback = true;
                continue;
            }
            if (filtered.length() > 0) {
                filtered.append("\n");
            }
            filtered.append(line);
        }
        String numericOutput = filtered.toString().trim();

        long backupSize = 0L;
        if (CollectionUtils.isNullOrEmpty(diskPaths)) {
            List<String> outputLines = Arrays.asList(numericOutput.split("\n"));
            if (!outputLines.isEmpty()) {
                backupSize = Long.parseLong(outputLines.get(outputLines.size() - 1).trim());
            }
        } else {
            String[] outputLines = numericOutput.split("\n");
            for(String line : outputLines) {
                backupSize = backupSize + Long.parseLong(line.split(" ")[0].trim());
            }
        }

        BackupAnswer answer = new BackupAnswer(command, true, stdout);
        answer.setSize(backupSize);
        answer.setBitmapCreated(bitmapCreated);
        answer.setBitmapRecreated(bitmapRecreated);
        answer.setIncrementalFallback(incrementalFallback);
        return answer;
    }
}
