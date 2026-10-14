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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@ResourceWrapper(handles = TakeBackupCommand.class)
public class LibvirtTakeBackupCommandWrapper extends CommandWrapper<TakeBackupCommand, Answer, LibvirtComputingResource> {
    private static final Integer EXIT_CLEANUP_FAILED = 20;
    // nasbackup.sh prints this on stdout when it could not proceed as an incremental and
    // completed a full backup instead; the orchestrator then records the backup as a full.
    private static final String INCREMENTAL_FALLBACK_MARKER = "INCREMENTAL_FALLBACK=true";
    // nasbackup.sh always reports the backup's total size this way, regardless of whether it took
    // the live or cold path — the two paths' other stdout (e.g. virsh domjobinfo) differ in shape,
    // so a single unambiguous marker is used instead of inferring size from output position/shape.
    private static final String BACKUP_SIZE_MARKER_PREFIX = "BACKUP_SIZE_TOTAL=";

    private static final String MODE_FULL = "full";
    private static final String MODE_INCREMENTAL = "incremental";
    // Incremental feature disabled: plain full backup with no QEMU bitmap/checkpoint and no
    // chain metadata. Matches nasbackup.sh's "legacy-full" mode (make_checkpoint=0).
    private static final String MODE_LEGACY_FULL = "legacy-full";

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
        try {
            if (Objects.nonNull(volumePaths)) {
                for (int idx = 0; idx < volumePaths.size(); idx++) {
                    PrimaryDataStoreTO volumePool = volumePools.get(idx);
                    String volumePath = volumePaths.get(idx);
                    if (volumePool.getPoolType() == Storage.StoragePoolType.RBD) {
                        KVMStoragePool volumeStoragePool = storagePoolMgr.getStoragePool(volumePool.getPoolType(), volumePool.getUuid());
                        String rbdDestVolumeFile = KVMPhysicalDisk.RBDStringBuilder(volumeStoragePool, volumePath);
                        diskPaths.add(rbdDestVolumeFile);
                        continue;
                    }
                    // StorPool (among others) is passed through as-is: nasbackup.sh checks the
                    // VM's actual liveness itself right before acting, and only then — if the VM
                    // turns out to be stopped — clones this into a point-in-time backup source
                    // volume. Doing that here instead would rely on the same stale state read
                    // nasbackup.sh's own check exists to correct for.
                    diskPaths.add(volumePath);
                }
            }

            Pair<Integer, String> result = runBackupScript(libvirtComputingResource, command, vmName, backupRepoType, backupRepoAddress,
                    mountOptions, backupPath, diskPaths, command.getMode(),
                    command.getBitmapNew(), command.getBitmapParent(), command.getParentPaths(), timeout);

            if (result.first() != 0) {
                logger.debug("Failed to take VM backup: " + result.second());
                BackupAnswer answer = new BackupAnswer(command, false, StringUtils.trimToEmpty(result.second()));
                if (EXIT_CLEANUP_FAILED.equals(result.first())) {
                    logger.debug("Backup cleanup failed");
                    answer.setNeedsCleanup(true);
                }
                return answer;
            }

            // The script self-heals to a full backup when an incremental can't proceed (e.g. the
            // parent checkpoint can't be re-registered) and signals it with INCREMENTAL_FALLBACK
            // on stdout. Detect it and the reported size from the raw output, then strip both
            // marker lines before using stdout as the answer's human-facing details.
            String rawStdout = result.second();
            boolean incrementalFallback = StringUtils.contains(rawStdout, INCREMENTAL_FALLBACK_MARKER);
            long backupSize = extractBackupSize(rawStdout);
            String stdout = stripMarkerLines(rawStdout).trim();

            BackupAnswer answer = new BackupAnswer(command, true, stdout);
            answer.setSize(backupSize);
            // A successful run always created command.getBitmapNew() (full and incremental both do;
            // it is null for legacy-full, which the orchestrator treats as "no bitmap").
            answer.setBitmapCreated(command.getBitmapNew());
            answer.setIncrementalFallback(incrementalFallback);
            return answer;
        } catch (RuntimeException e) {
            logger.error("Failed to take VM backup: " + e.getMessage(), e);
            return new BackupAnswer(command, false, e.getMessage());
        }
    }

    /** Remove nasbackup.sh's stdout signalling marker lines so they don't pollute the answer details. */
    private String stripMarkerLines(String stdout) {
        if (StringUtils.isBlank(stdout)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String line : stdout.split("\n", -1)) {
            if (line.contains(INCREMENTAL_FALLBACK_MARKER) || line.startsWith(BACKUP_SIZE_MARKER_PREFIX)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(line);
        }
        return sb.toString();
    }

    /**
     * Find nasbackup.sh's {@code BACKUP_SIZE_TOTAL=<bytes>} marker line. Unlike the old
     * position/shape-based parsing this replaced, it doesn't need to know or guess which of
     * nasbackup.sh's code paths actually ran.
     * <p>
     * The marker is only metadata: a missing/unparseable marker does not mean the backup (which
     * already exited 0) failed, so this logs a warning and reports an unknown size (0) rather
     * than throwing — the caller's generic RuntimeException handler would otherwise turn an
     * on-disk-successful backup into a reported failure.
     */
    private long extractBackupSize(String rawStdout) {
        if (rawStdout != null) {
            for (String line : rawStdout.split("\n", -1)) {
                if (line.startsWith(BACKUP_SIZE_MARKER_PREFIX)) {
                    try {
                        return Long.parseLong(line.substring(BACKUP_SIZE_MARKER_PREFIX.length()).trim());
                    } catch (NumberFormatException e) {
                        logger.warn("nasbackup.sh reported an unparseable {} marker: {}", BACKUP_SIZE_MARKER_PREFIX, line);
                        return 0L;
                    }
                }
            }
        }
        logger.warn("nasbackup.sh did not report a {} marker in its output; backup succeeded but its size is unknown", BACKUP_SIZE_MARKER_PREFIX);
        return 0L;
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
                "-d", CollectionUtils.isEmpty(diskPaths) ? "" : String.join(",", diskPaths)
        ));
        if (StringUtils.isNotBlank(mode)) {
            argv.add("-M");
            argv.add(mode);
        }
        if (StringUtils.isNotBlank(bitmapNew)) {
            argv.add("--bitmap-new");
            argv.add(bitmapNew);
        }
        if (StringUtils.isNotBlank(bitmapParent)) {
            argv.add("--bitmap-parent");
            argv.add(bitmapParent);
        }
        if (CollectionUtils.isNotEmpty(parentPaths)) {
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
        if (StringUtils.isBlank(mode)) {
            return null; // legacy full-only, no extra args expected
        }
        if (MODE_INCREMENTAL.equals(mode)) {
            if (StringUtils.isBlank(command.getBitmapNew())) {
                return "incremental mode requires bitmapNew";
            }
            if (StringUtils.isBlank(command.getBitmapParent())) {
                return "incremental mode requires bitmapParent";
            }
            if (CollectionUtils.isEmpty(command.getParentPaths())) {
                return "incremental mode requires parentPaths";
            }
            return null;
        }
        if (MODE_FULL.equals(mode)) {
            if (StringUtils.isBlank(command.getBitmapNew())) {
                return "full mode requires bitmapNew (the bitmap to create for the next incremental)";
            }
            return null;
        }
        if (MODE_LEGACY_FULL.equals(mode)) {
            return null; // feature-off full backup, no bitmap or chain args expected
        }
        return "Unknown backup mode: " + mode;
    }
}
