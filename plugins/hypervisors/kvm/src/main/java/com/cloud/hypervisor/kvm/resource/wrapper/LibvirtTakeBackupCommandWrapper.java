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

    private static final String MODE_FULL = "full";
    private static final String MODE_INCREMENTAL = "incremental";
    // Incremental feature disabled: plain full backup with no QEMU bitmap/checkpoint and no
    // chain metadata. Matches nasbackup.sh's "legacy-full" mode (make_checkpoint=0).
    private static final String MODE_LEGACY_FULL = "legacy-full";

    // Backup methods, orthogonal to the mode. qcow2 is the push-mode path with QEMU dirty bitmaps,
    // also assumed when the management server sends none. rbd is the Ceph-native path: it anchors
    // each cycle on an `rbd snap` and exports the delta with `rbd export-diff`, so bitmapNew and
    // bitmapParent carry RBD snapshot names rather than QEMU bitmap names, and nothing is rebased.
    private static final String METHOD_QCOW2 = "qcow2";
    private static final String METHOD_RBD = "rbd";

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
        // on stdout. Detect it, then strip the marker line before parsing the backup size.
        String rawStdout = result.second();
        boolean incrementalFallback = StringUtils.contains(rawStdout, INCREMENTAL_FALLBACK_MARKER);
        String stdout = stripMarkerLines(rawStdout).trim();
        long backupSize = parseBackupSize(stdout, diskPaths);

        BackupAnswer answer = new BackupAnswer(command, true, stdout);
        answer.setSize(backupSize);
        // A successful run always created command.getBitmapNew() (full and incremental both do;
        // it is null for legacy-full, which the orchestrator treats as "no bitmap").
        answer.setBitmapCreated(command.getBitmapNew());
        answer.setIncrementalFallback(incrementalFallback);
        return answer;
    }

    /** Remove nasbackup.sh's stdout signalling marker lines so they don't pollute size parsing. */
    private String stripMarkerLines(String stdout) {
        if (StringUtils.isBlank(stdout)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String line : stdout.split("\n", -1)) {
            if (line.contains(INCREMENTAL_FALLBACK_MARKER)) {
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
     * Run nasbackup.sh once with the given args. Returns the exit code + captured stdout.
     */
    private Pair<Integer, String> runBackupScript(LibvirtComputingResource libvirtComputingResource,
            TakeBackupCommand command, String vmName, String backupRepoType, String backupRepoAddress,
            String mountOptions, String backupPath, List<String> diskPaths, String mode,
            String bitmapNew, String bitmapParent, List<String> parentPaths, int timeout) {
        boolean quiesce = command.getQuiesce() != null && command.getQuiesce();
        List<String> argv = new ArrayList<>(Arrays.asList(
                libvirtComputingResource.getNasBackupPath(),
                "-o", "backup",
                "-v", vmName,
                "-t", backupRepoType,
                "-s", backupRepoAddress,
                "-m", Objects.nonNull(mountOptions) ? mountOptions : "",
                "-p", backupPath,
                "-q", quiesce ? "true" : "false",
                "-d", CollectionUtils.isEmpty(diskPaths) ? "" : String.join(",", diskPaths)
        ));
        Integer quiesceTimeout = command.getQuiesceTimeout();
        if (quiesce && quiesceTimeout != null && quiesceTimeout > 0) {
            argv.add("--quiesce-timeout");
            argv.add(String.valueOf(quiesceTimeout));
        }
        if (StringUtils.isNotBlank(mode)) {
            argv.add("-M");
            argv.add(mode);
        }
        if (StringUtils.isNotBlank(command.getMethod())) {
            argv.add("--method");
            argv.add(command.getMethod());
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
        String method = StringUtils.defaultIfBlank(command.getMethod(), METHOD_QCOW2);
        if (METHOD_RBD.equals(method)) {
            return validateRbdBackupArgs(command, mode);
        }
        if (!METHOD_QCOW2.equals(method)) {
            return "Unknown backup method: " + method;
        }
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

    /**
     * An RBD chain anchors each cycle on an {@code rbd snap} whose name travels in bitmapNew, and
     * an incremental diffs it against the previous anchor in bitmapParent. No parent file paths:
     * the diff stream is replayed on restore rather than rebased onto its parent.
     */
    private String validateRbdBackupArgs(TakeBackupCommand command, String mode) {
        if (!MODE_FULL.equals(mode) && !MODE_INCREMENTAL.equals(mode)) {
            return "Unknown backup mode for the rbd method: " + mode;
        }
        if (command.getBitmapNew() == null || command.getBitmapNew().isEmpty()) {
            return mode + " mode requires bitmapNew (the RBD snapshot to create as the chain anchor)";
        }
        if (MODE_INCREMENTAL.equals(mode)
                && (command.getBitmapParent() == null || command.getBitmapParent().isEmpty())) {
            return "incremental mode requires bitmapParent (the RBD snapshot to diff against)";
        }
        if (command.getVolumePaths() == null || command.getVolumePaths().isEmpty()) {
            return mode + " mode requires the VM's volume paths";
        }
        return null;
    }

    /**
     * Sum the per-disk size lines emitted by nasbackup.sh. Single-volume mode emits one
     * line containing just the byte count; multi-volume mode emits one line per disk
     * whose first whitespace-separated token is the byte count.
     *
     * <p>Lines that do not start with a number are skipped rather than aborting the parse: the
     * script's stdout also carries whatever the mount produced, and losing an otherwise complete
     * backup over a stray warning line would be worse than reporting a size of 0.</p>
     */
    private long parseBackupSize(String stdout, List<String> diskPaths) {
        long backupSize = 0L;
        if (CollectionUtils.isEmpty(diskPaths)) {
            List<String> outputLines = Arrays.asList(stdout.split("\n"));
            if (!outputLines.isEmpty()) {
                backupSize = Long.parseLong(outputLines.get(outputLines.size() - 1).trim());
            }
        } else {
            for (String line : stdout.split("\n")) {
                String sizeToken = line.trim().split("\\s+")[0];
                if (!sizeToken.matches("\\d+")) {
                    logger.debug("Ignoring non-size line in nasbackup.sh output: {}", line);
                    continue;
                }
                backupSize = backupSize + Long.parseLong(sizeToken);
            }
        }
        return backupSize;
    }
}
