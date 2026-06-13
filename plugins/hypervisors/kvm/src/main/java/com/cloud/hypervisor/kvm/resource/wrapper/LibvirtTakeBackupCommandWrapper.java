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
    private static final Integer EXIT_INCREMENTAL_UNSUPPORTED = 21;
    // Backup file is valid, but the host has no bitmap to anchor the next incremental on.
    // Used by the stopped-VM path when qemu-img bitmap --add failed for every source disk.
    private static final Integer EXIT_BITMAP_NOT_SEEDED = 22;

    private static final String MODE_FULL = "full";
    private static final String MODE_INCREMENTAL = "incremental";

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

        // Pre-validate incremental args here rather than relying on the script to error out.
        // Keeps the script agnostic to caller policy (it just does what it's told).
        String validationError = validateBackupArgs(command);
        if (validationError != null) {
            return new BackupAnswer(command, false, validationError);
        }

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

        final String requestedMode = command.getMode();
        Pair<Integer, String> result = runBackupScript(libvirtComputingResource, command, vmName, backupRepoType, backupRepoAddress,
                mountOptions, backupPath, diskPaths, requestedMode,
                command.getBitmapNew(), command.getBitmapParent(), command.getParentPaths(), timeout);

        boolean incrementalFallback = false;
        if (result.first() == EXIT_INCREMENTAL_UNSUPPORTED && MODE_INCREMENTAL.equals(requestedMode)) {
            // Script told us the incremental can't proceed (parent checkpoint can't be
            // re-registered, or VM is stopped). Re-invoke as full with the same --bitmap-new
            // so the chain restarts cleanly. Drop --bitmap-parent + --parent-paths since
            // they no longer apply.
            logger.info("nasbackup.sh signalled incremental unsupported for VM " + vmName + " — retrying as full");
            result = runBackupScript(libvirtComputingResource, command, vmName, backupRepoType, backupRepoAddress,
                    mountOptions, backupPath, diskPaths, MODE_FULL,
                    command.getBitmapNew(), null, null, timeout);
            incrementalFallback = true;
        }

        boolean bitmapSeeded = true;
        if (result.first() == EXIT_BITMAP_NOT_SEEDED) {
            // Backup file is valid; the host just has no bitmap. Treat as success but
            // mark bitmapCreated=null so the orchestrator clears active_checkpoint_id.
            bitmapSeeded = false;
        } else if (result.first() != 0) {
            logger.debug("Failed to take VM backup: " + result.second());
            BackupAnswer answer = new BackupAnswer(command, false, result.second().trim());
            if (result.first() == EXIT_CLEANUP_FAILED) {
                logger.debug("Backup cleanup failed");
                answer.setNeedsCleanup(true);
            }
            return answer;
        }

        String stdout = result.second().trim();
        long backupSize = parseBackupSize(stdout, diskPaths);

        BackupAnswer answer = new BackupAnswer(command, true, stdout);
        answer.setSize(backupSize);
        // bitmapCreated mirrors what we asked the script to create — except when the
        // script exited EXIT_BITMAP_NOT_SEEDED, in which case the host has no bitmap
        // and the orchestrator must clear active_checkpoint_id.
        answer.setBitmapCreated(bitmapSeeded ? command.getBitmapNew() : null);
        answer.setIncrementalFallback(incrementalFallback);
        return answer;
    }

    /**
     * Run nasbackup.sh once with the given args. Returns the exit code + captured stdout.
     */
    private Pair<Integer, String> runBackupScript(LibvirtComputingResource libvirtComputingResource,
            TakeBackupCommand command, String vmName, String backupRepoType, String backupRepoAddress,
            String mountOptions, String backupPath, List<String> diskPaths, String mode,
            String bitmapNew, String bitmapParent, List<String> parentPaths, int timeout) {
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
        if (mode != null && !mode.isEmpty()) {
            argv.add("-M");
            argv.add(mode);
        }
        if (bitmapNew != null && !bitmapNew.isEmpty()) {
            argv.add("--bitmap-new");
            argv.add(bitmapNew);
        }
        if (bitmapParent != null && !bitmapParent.isEmpty()) {
            argv.add("--bitmap-parent");
            argv.add(bitmapParent);
        }
        if (parentPaths != null && !parentPaths.isEmpty()) {
            argv.add("--parent-paths");
            argv.add(String.join(",", parentPaths));
        }

        List<String[]> commands = new ArrayList<>();
        commands.add(argv.toArray(new String[0]));
        return Script.executePipedCommands(commands, timeout);
    }

    /**
     * Return a human-readable validation error string, or {@code null} if the command's
     * incremental-backup args are internally consistent.
     */
    private String validateBackupArgs(TakeBackupCommand command) {
        String mode = command.getMode();
        if (mode == null || mode.isEmpty()) {
            return null; // legacy full-only — no extra args expected
        }
        if (MODE_INCREMENTAL.equals(mode)) {
            if (command.getBitmapNew() == null || command.getBitmapNew().isEmpty()) {
                return "incremental mode requires bitmapNew";
            }
            if (command.getBitmapParent() == null || command.getBitmapParent().isEmpty()) {
                return "incremental mode requires bitmapParent";
            }
            if (command.getParentPaths() == null || command.getParentPaths().isEmpty()) {
                return "incremental mode requires parentPaths";
            }
            return null;
        }
        if (MODE_FULL.equals(mode)) {
            if (command.getBitmapNew() == null || command.getBitmapNew().isEmpty()) {
                return "full mode requires bitmapNew (the bitmap to create for the next incremental)";
            }
            return null;
        }
        return "Unknown backup mode: " + mode;
    }

    /**
     * Sum the per-disk size lines emitted by nasbackup.sh. Single-volume mode emits one
     * line containing just the byte count; multi-volume mode emits one line per disk
     * whose first whitespace-separated token is the byte count.
     */
    private long parseBackupSize(String stdout, List<String> diskPaths) {
        long backupSize = 0L;
        if (CollectionUtils.isNullOrEmpty(diskPaths)) {
            List<String> outputLines = Arrays.asList(stdout.split("\n"));
            if (!outputLines.isEmpty()) {
                backupSize = Long.parseLong(outputLines.get(outputLines.size() - 1).trim());
            }
        } else {
            for (String line : stdout.split("\n")) {
                backupSize = backupSize + Long.parseLong(line.split(" ")[0].trim());
            }
        }
        return backupSize;
    }
}
