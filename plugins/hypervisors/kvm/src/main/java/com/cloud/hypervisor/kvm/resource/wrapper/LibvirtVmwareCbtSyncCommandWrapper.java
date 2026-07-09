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
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.VmwareCbtMigrationAnswer;
import com.cloud.agent.api.VmwareCbtSyncCommand;
import com.cloud.agent.api.to.RemoteInstanceTO;
import com.cloud.agent.api.to.VmwareCbtChangedBlockRangeTO;
import com.cloud.agent.api.to.VmwareCbtDiskSyncResultTO;
import com.cloud.agent.api.to.VmwareCbtDiskTO;
import com.cloud.agent.api.to.VmwareCbtTargetStorageType;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = VmwareCbtSyncCommand.class)
public class LibvirtVmwareCbtSyncCommandWrapper extends CommandWrapper<VmwareCbtSyncCommand, Answer, LibvirtComputingResource> {

    private static final long DEFAULT_MAX_COPY_CHUNK_BYTES = 256L * 1024L * 1024L;
    private static final Pattern SHA1_FINGERPRINT_PATTERN = Pattern.compile("(?i)(?:SHA1\\s+)?Fingerprint\\s*=\\s*([0-9A-F:]+)");

    @Override
    public Answer execute(VmwareCbtSyncCommand cmd, LibvirtComputingResource serverResource) {
        if (!serverResource.hostSupportsVddkBlockCopy(cmd.getVddkLibDir())) {
            String msg = String.format("Cannot synchronize VMware CBT migration %s on host %s. VDDK, qemu-img, qemu-nbd and qemu-io are required.",
                    cmd.getMigrationUuid(), serverResource.getPrivateIp());
            logger.info(msg);
            return new VmwareCbtMigrationAnswer(cmd, false, msg, cmd.getMigrationUuid(), cmd.getCycleNumber(),
                    0, 0, 0, false, null);
        }

        long startTime = System.currentTimeMillis();
        List<VmwareCbtChangedBlockRangeTO> changedBlocks = cmd.getChangedBlocks();
        if (changedBlocks == null || changedBlocks.isEmpty()) {
            long durationSeconds = Math.max(1L, (System.currentTimeMillis() - startTime) / 1000L);
            String msg = String.format("VMware CBT cycle %s for migration %s completed with no changed blocks.",
                    cmd.getCycleNumber(), cmd.getMigrationUuid());
            logger.info(msg);
            return new VmwareCbtMigrationAnswer(cmd, true, msg, cmd.getMigrationUuid(), cmd.getCycleNumber(),
                    0, 0, durationSeconds, true, null);
        }

        VmwareCbtSyncPlan syncPlan = VmwareCbtSyncPlan.create(cmd.getDisks(), changedBlocks);
        if (!syncPlan.isValid()) {
            String msg = String.format("Cannot synchronize VMware CBT cycle %s for migration %s: %s",
                    cmd.getCycleNumber(), cmd.getMigrationUuid(), syncPlan.getValidationError());
            logger.info(msg);
            return new VmwareCbtMigrationAnswer(cmd, false, msg, cmd.getMigrationUuid(), cmd.getCycleNumber(),
                    0, 0, 0, false, null);
        }

        try {
            validateSyncCommand(cmd);
            String vddkLibDir = resolveVddkSetting(cmd.getVddkLibDir(), serverResource.getVddkLibDir());
            if (StringUtils.isBlank(vddkLibDir)) {
                String msg = String.format("Cannot synchronize VMware CBT migration %s because no VDDK library directory is configured or detected.",
                        cmd.getMigrationUuid());
                logger.info(msg);
                return new VmwareCbtMigrationAnswer(cmd, false, msg, cmd.getMigrationUuid(), cmd.getCycleNumber(),
                        syncPlan.getChangedBytes(), 0, 0, false, null);
            }

            RemoteInstanceTO sourceInstance = cmd.getSourceInstance();
            String vddkThumbprint = resolveVddkSetting(cmd.getVddkThumbprint(), serverResource.getVddkThumbprint());
            if (StringUtils.isBlank(vddkThumbprint)) {
                vddkThumbprint = getVcenterThumbprint(sourceInstance.getVcenterHost(), getTimeout(cmd),
                        sourceInstance.getInstanceName());
            }
            if (StringUtils.isBlank(vddkThumbprint)) {
                String msg = String.format("Cannot synchronize VMware CBT migration %s because the vCenter SSL thumbprint could not be determined.",
                        cmd.getMigrationUuid());
                logger.info(msg);
                return new VmwareCbtMigrationAnswer(cmd, false, msg, cmd.getMigrationUuid(), cmd.getCycleNumber(),
                        syncPlan.getChangedBytes(), 0, 0, false, null);
            }

            KVMStoragePool targetPool = getTargetStoragePool(cmd, serverResource.getStoragePoolMgr());
            if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW) {
                validateRbdTargetPool(cmd, targetPool);
            } else if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RAW_BLOCK_DEVICE) {
                validateBlockDeviceTargetPool(cmd, targetPool);
            }

            String passwordFilePath = writePasswordFile(cmd);
            try {
                List<VmwareCbtDiskSyncResultTO> diskResults = new ArrayList<>();
                for (VmwareCbtSyncPlan.DiskPlan diskPlan : syncPlan.getDiskPlans()) {
                    diskResults.add(syncDiskChangedBlocks(cmd, diskPlan, targetPool, passwordFilePath, vddkLibDir, vddkThumbprint,
                            StringUtils.defaultIfBlank(cmd.getVddkTransports(), serverResource.getVddkTransports())));
                }
                long durationSeconds = Math.max(1L, (System.currentTimeMillis() - startTime) / 1000L);
                long dirtyRate = getDirtyRateBytesPerSecond(syncPlan.getChangedBytes(), durationSeconds);
                String msg = String.format("VMware CBT cycle %s for migration %s copied %s changed block range(s), " +
                                "coalesced into %s copy range(s) across %s disk(s), totaling %s bytes.",
                        cmd.getCycleNumber(), cmd.getMigrationUuid(), syncPlan.getChangedRangeCount(),
                        syncPlan.getCopyRangeCount(), syncPlan.getDiskPlans().size(), syncPlan.getChangedBytes());
                logger.info(msg);
                return new VmwareCbtMigrationAnswer(cmd, true, msg, cmd.getMigrationUuid(), cmd.getCycleNumber(),
                        syncPlan.getChangedBytes(), dirtyRate, durationSeconds, false, diskResults);
            } finally {
                Files.deleteIfExists(Path.of(passwordFilePath));
            }
        } catch (Exception e) {
            String msg = String.format("Cannot synchronize VMware CBT cycle %s for migration %s: %s",
                    cmd.getCycleNumber(), cmd.getMigrationUuid(),
                    StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            logger.error(msg, e);
            return new VmwareCbtMigrationAnswer(cmd, false, msg, cmd.getMigrationUuid(), cmd.getCycleNumber(),
                    syncPlan.getChangedBytes(), 0, 0, false, null);
        }
    }

    private void validateSyncCommand(VmwareCbtSyncCommand cmd) {
        RemoteInstanceTO sourceInstance = cmd.getSourceInstance();
        if (sourceInstance == null) {
            throw new IllegalArgumentException("source VMware instance information is missing");
        }
        if (StringUtils.isAnyBlank(sourceInstance.getVcenterHost(), sourceInstance.getVcenterUsername(),
                sourceInstance.getVcenterPassword(), sourceInstance.getInstanceName())) {
            throw new IllegalArgumentException("source vCenter host, username, password and VM name are required");
        }
        if (StringUtils.isBlank(sourceInstance.getVmwareMoref())) {
            throw new IllegalArgumentException("source VMware VM managed object reference is missing");
        }
        if (StringUtils.isBlank(cmd.getSnapshotMor())) {
            throw new IllegalArgumentException("VMware snapshot managed object reference is missing");
        }
    }

    private VmwareCbtDiskSyncResultTO syncDiskChangedBlocks(VmwareCbtSyncCommand cmd, VmwareCbtSyncPlan.DiskPlan diskPlan,
                                                            KVMStoragePool targetPool, String passwordFilePath, String vddkLibDir,
                                                            String vddkThumbprint, String vddkTransports) throws Exception {
        VmwareCbtDiskTO disk = diskPlan.getDisk();
        validateDisk(cmd, disk);
        long startTime = System.currentTimeMillis();
        String scriptPath = writeDiskSyncScript(cmd, diskPlan, targetPool);
        try {
            String command = buildNbdkitDeltaSyncCommand(cmd, disk, passwordFilePath, vddkLibDir, vddkThumbprint,
                    vddkTransports, scriptPath);
            VmwareCbtCommandResult commandResult = executeLoggedBash(command, getTimeout(cmd),
                    String.format("(%s) VMware CBT delta sync disk %s", cmd.getMigrationUuid(), disk.getDiskId()));
            long durationSeconds = Math.max(1L, (System.currentTimeMillis() - startTime) / 1000L);
            if (commandResult.getExitValue() != 0) {
                String details = String.format("changed-block copy from VMware VDDK snapshot failed for source disk %s with exit code %s",
                        disk.getDiskId(), commandResult.getExitValue());
                throw new IllegalStateException(commandResult.appendLastCommandOutput(details));
            }
            return new VmwareCbtDiskSyncResultTO(disk.getDiskId(), disk.getTargetPath(), disk.getChangeId(),
                    cmd.getSnapshotMor(), diskPlan.getChangedBytes(), durationSeconds, true,
                    String.format("Copied %s changed bytes in %s range(s)", diskPlan.getChangedBytes(),
                            diskPlan.getChangedBlocks().size()));
        } finally {
            Files.deleteIfExists(Path.of(scriptPath));
        }
    }

    private void validateDisk(VmwareCbtSyncCommand cmd, VmwareCbtDiskTO disk) {
        if (disk == null) {
            throw new IllegalArgumentException("source disk cannot be null");
        }
        if (StringUtils.isBlank(disk.getDiskId())) {
            throw new IllegalArgumentException("source disk ID is missing");
        }
        if (StringUtils.isBlank(disk.getSourceDiskPath())) {
            throw new IllegalArgumentException(String.format("source disk path is missing for disk %s", disk.getDiskId()));
        }
        if (StringUtils.isBlank(disk.getTargetPath())) {
            throw new IllegalArgumentException(String.format("target disk path is missing for disk %s", disk.getDiskId()));
        }
        if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW ||
                cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RAW_BLOCK_DEVICE) {
            if (!StringUtils.equalsIgnoreCase(StringUtils.defaultIfBlank(disk.getTargetFormat(), "raw"), "raw")) {
                throw new IllegalArgumentException(String.format("only raw target disks are supported for VMware CBT delta sync to %s; disk %s uses %s",
                        cmd.getTargetStorageType(), disk.getDiskId(), disk.getTargetFormat()));
            }
            return;
        }
        if (!Files.isRegularFile(Path.of(disk.getTargetPath()))) {
            throw new IllegalArgumentException(String.format("target disk path %s for disk %s does not exist or is not a regular file",
                    disk.getTargetPath(), disk.getDiskId()));
        }
        if (!StringUtils.equalsIgnoreCase(StringUtils.defaultIfBlank(disk.getTargetFormat(), "qcow2"), "qcow2")) {
            throw new IllegalArgumentException(String.format("only qcow2 target disks are supported for VMware CBT delta sync; disk %s uses %s",
                    disk.getDiskId(), disk.getTargetFormat()));
        }
    }

    private String writePasswordFile(VmwareCbtSyncCommand cmd) throws Exception {
        Path passwordFile = Files.createTempFile(String.format("vmware-cbt-%s-", sanitizeFileName(cmd.getMigrationUuid())),
                ".pass");
        Files.writeString(passwordFile, cmd.getSourceInstance().getVcenterPassword());
        setPosixFilePermissionsIfSupported(passwordFile, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE));
        return passwordFile.toString();
    }

    protected String writeDiskSyncScript(VmwareCbtSyncCommand cmd, VmwareCbtSyncPlan.DiskPlan diskPlan,
                                         KVMStoragePool targetPool) throws Exception {
        VmwareCbtDiskTO disk = diskPlan.getDisk();
        Path scriptPath = Files.createTempFile(String.format("vmware-cbt-%s-%s-", sanitizeFileName(cmd.getMigrationUuid()),
                sanitizeFileName(disk.getDiskId())), ".sh");
        StringBuilder script = new StringBuilder();
        script.append("#!/bin/bash\n");
        script.append("set -euo pipefail\n");
        script.append("target_path=").append(shellQuote(getQemuTargetPath(cmd, disk, targetPool))).append("\n");
        script.append("target_format=").append(shellQuote(getTargetFormat(cmd, disk))).append("\n");
        script.append("max_chunk_bytes=").append(DEFAULT_MAX_COPY_CHUNK_BYTES).append("\n");
        script.append("tmp_dir=$(mktemp -d /var/tmp/cloudstack-cbt-").append(sanitizeFileName(cmd.getMigrationUuid()))
                .append("-").append(sanitizeFileName(disk.getDiskId())).append("-XXXXXX)\n");
        script.append("cleanup() {\n");
        script.append("  rm -rf \"$tmp_dir\"\n");
        script.append("}\n");
        script.append("trap cleanup EXIT\n");
        script.append("get_nbd_socket_path() {\n");
        script.append("  local socket_path=\"${uri#*socket=}\"\n");
        script.append("  socket_path=\"${socket_path%%&*}\"\n");
        script.append("  if [[ \"$socket_path\" == \"$uri\" || -z \"$socket_path\" ]]; then\n");
        script.append("    echo \"Cannot parse nbdkit unix socket from uri: $uri\" >&2\n");
        script.append("    exit 1\n");
        script.append("  fi\n");
        script.append("  echo \"$socket_path\"\n");
        script.append("}\n");
        script.append("nbd_socket_path=$(get_nbd_socket_path)\n");
        script.append("copy_range() {\n");
        script.append("  local range_start=\"$1\"\n");
        script.append("  local range_length=\"$2\"\n");
        script.append("  local current_start=\"$range_start\"\n");
        script.append("  local remaining=\"$range_length\"\n");
        script.append("  while (( remaining > 0 )); do\n");
        script.append("    local chunk_length=\"$remaining\"\n");
        script.append("    if (( chunk_length > max_chunk_bytes )); then\n");
        script.append("      chunk_length=\"$max_chunk_bytes\"\n");
        script.append("    fi\n");
        script.append("    local chunk_file=\"$tmp_dir/range-${current_start}-${chunk_length}.raw\"\n");
        script.append("    local source_opts=\"driver=raw,offset=$current_start,size=$chunk_length,file.driver=nbd,file.server.type=unix,file.server.path=$nbd_socket_path\"\n");
        script.append("    qemu-img convert --image-opts -O raw \"$source_opts\" \"$chunk_file\" >/dev/null\n");
        script.append("    qemu-io -f \"$target_format\" -c \"write -s $chunk_file $current_start $chunk_length\" \"$target_path\"\n");
        script.append("    rm -f \"$chunk_file\"\n");
        script.append("    remaining=$((remaining - chunk_length))\n");
        script.append("    current_start=$((current_start + chunk_length))\n");
        script.append("  done\n");
        script.append("}\n");
        for (VmwareCbtChangedBlockRangeTO changedBlock : diskPlan.getChangedBlocks()) {
            script.append("copy_range ").append(changedBlock.getStartOffset()).append(" ")
                    .append(changedBlock.getLength()).append("\n");
        }
        Files.writeString(scriptPath, script.toString());
        setPosixFilePermissionsIfSupported(scriptPath, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        return scriptPath.toString();
    }

    private KVMStoragePool getTargetStoragePool(VmwareCbtSyncCommand cmd, KVMStoragePoolManager storagePoolMgr) {
        Storage.StoragePoolType poolType = cmd.getDestinationStoragePoolType();
        String poolUuid = StringUtils.trimToNull(cmd.getDestinationStoragePoolUuid());
        if (poolType != null && StringUtils.isNotBlank(poolUuid)) {
            KVMStoragePool pool = storagePoolMgr.getStoragePool(poolType, poolUuid);
            if (pool == null) {
                throw new IllegalArgumentException(String.format("destination storage pool %s/%s is not available on this host",
                        poolType, poolUuid));
            }
            return pool;
        }
        return null;
    }

    private void validateRbdTargetPool(VmwareCbtSyncCommand cmd, KVMStoragePool targetPool) {
        if (targetPool == null || targetPool.getType() != Storage.StoragePoolType.RBD) {
            throw new IllegalArgumentException(String.format("VMware CBT migration %s requires an RBD destination storage pool for RBD target writing",
                    cmd.getMigrationUuid()));
        }
    }

    private void validateBlockDeviceTargetPool(VmwareCbtSyncCommand cmd, KVMStoragePool targetPool) {
        if (targetPool == null || targetPool.getType() != Storage.StoragePoolType.Linstor) {
            throw new IllegalArgumentException(String.format("VMware CBT migration %s requires a Linstor destination storage pool for block device target writing",
                    cmd.getMigrationUuid()));
        }
    }

    private String getQemuTargetPath(VmwareCbtSyncCommand cmd, VmwareCbtDiskTO disk, KVMStoragePool targetPool) {
        if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW) {
            return KVMPhysicalDisk.RBDStringBuilder(targetPool, getRbdImagePath(targetPool, disk.getTargetPath()));
        }
        if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RAW_BLOCK_DEVICE) {
            return getBlockDevicePath(targetPool, disk);
        }
        return disk.getTargetPath();
    }

    private String getBlockDevicePath(KVMStoragePool targetPool, VmwareCbtDiskTO disk) {
        String normalizedTargetPath = StringUtils.defaultString(disk.getTargetPath()).replace('\\', '/');
        String volumeName = StringUtils.contains(normalizedTargetPath, "/") ?
                StringUtils.substringAfterLast(normalizedTargetPath, "/") : normalizedTargetPath;
        KVMPhysicalDisk targetDisk = targetPool.getPhysicalDisk(volumeName);
        String devicePath = targetDisk == null ? null : targetDisk.getPath();
        if (StringUtils.isBlank(devicePath)) {
            throw new IllegalStateException(String.format("could not resolve a local device path for block device target volume %s of disk %s",
                    volumeName, disk.getDiskId()));
        }
        return devicePath;
    }

    private String getTargetFormat(VmwareCbtSyncCommand cmd, VmwareCbtDiskTO disk) {
        if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW ||
                cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RAW_BLOCK_DEVICE) {
            return "raw";
        }
        return StringUtils.defaultIfBlank(disk.getTargetFormat(), "qcow2");
    }

    private String getRbdImagePath(KVMStoragePool targetPool, String targetPath) {
        String normalizedTargetPath = StringUtils.defaultString(targetPath).replace('\\', '/');
        String imageName = StringUtils.contains(normalizedTargetPath, "/") ? StringUtils.substringAfterLast(normalizedTargetPath, "/") : normalizedTargetPath;
        return String.format("%s/%s", StringUtils.removeEnd(targetPool.getSourceDir(), "/"), imageName);
    }

    private void setPosixFilePermissionsIfSupported(Path path, Set<PosixFilePermission> permissions) throws Exception {
        try {
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException e) {
            logger.debug("POSIX file permissions are not supported for {}", path);
        }
    }

    private String buildNbdkitDeltaSyncCommand(VmwareCbtSyncCommand cmd, VmwareCbtDiskTO disk,
                                                String passwordFilePath, String vddkLibDir, String vddkThumbprint,
                                                String vddkTransports, String scriptPath) {
        RemoteInstanceTO sourceInstance = cmd.getSourceInstance();
        String sourceVmMoref = getMorefValue(sourceInstance.getVmwareMoref());
        String snapshotMoref = getMorefValue(cmd.getSnapshotMor());
        StringBuilder nbdkit = new StringBuilder("nbdkit -r -U - vddk ");
        appendPluginParameter(nbdkit, "file", disk.getSourceDiskPath());
        appendPluginParameter(nbdkit, "server", sourceInstance.getVcenterHost());
        appendPluginParameter(nbdkit, "user", sourceInstance.getVcenterUsername());
        nbdkit.append("password=+").append(shellQuote(passwordFilePath)).append(" ");
        nbdkit.append("vm=moref=").append(shellQuote(sourceVmMoref)).append(" ");
        appendPluginParameter(nbdkit, "snapshot", snapshotMoref);
        appendPluginParameter(nbdkit, "libdir", vddkLibDir);
        appendPluginParameter(nbdkit, "thumbprint", vddkThumbprint);
        if (StringUtils.isNotBlank(vddkTransports)) {
            appendPluginParameter(nbdkit, "transports", vddkTransports);
        }
        String runCommand = String.format("export uri; bash %s", shellQuote(scriptPath));
        return nbdkit.append("--run ").append(shellQuote(runCommand)).toString();
    }

    private void appendPluginParameter(StringBuilder command, String key, String value) {
        command.append(key).append("=").append(shellQuote(value)).append(" ");
    }

    private long getDirtyRateBytesPerSecond(long changedBytes, long durationSeconds) {
        if (durationSeconds <= 0) {
            return changedBytes;
        }
        return changedBytes / durationSeconds;
    }

    private long getTimeout(VmwareCbtSyncCommand cmd) {
        return Math.max(1L, cmd.getWait()) * 1000L;
    }

    private String getMorefValue(String moref) {
        String value = StringUtils.trimToNull(moref);
        if (value == null) {
            return null;
        }
        return value.contains(":") ? StringUtils.substringAfter(value, ":") : value;
    }

    private String sanitizeFileName(String value) {
        String sanitized = StringUtils.defaultIfBlank(value, "disk").replaceAll("[^A-Za-z0-9._-]", "-");
        return StringUtils.defaultIfBlank(sanitized, "disk");
    }

    private String shellQuote(String value) {
        return "'" + StringUtils.defaultString(value).replace("'", "'\"'\"'") + "'";
    }

    private String resolveVddkSetting(String commandValue, String agentValue) {
        return StringUtils.defaultIfBlank(StringUtils.trimToNull(commandValue), StringUtils.trimToNull(agentValue));
    }

    protected VmwareCbtCommandResult executeLoggedBash(String command, long timeout, String logPrefix) {
        Script script = new Script("/bin/bash", timeout, logger);
        script.add("-c");
        script.add(command);
        VmwareCbtCommandOutputLogger outputLogger = new VmwareCbtCommandOutputLogger(logger, logPrefix);
        script.execute(outputLogger);
        return new VmwareCbtCommandResult(script.getExitValue(), outputLogger.getLastRelevantOutputLine());
    }

    protected String getVcenterThumbprint(String vcenterHost, long timeout, String sourceVmName) {
        if (StringUtils.isBlank(vcenterHost)) {
            return null;
        }
        String endpoint = String.format("%s:443", vcenterHost);
        String command = String.format("openssl s_client -connect %s </dev/null 2>/dev/null | " +
                "openssl x509 -fingerprint -sha1 -noout", shellQuote(endpoint));

        Script script = new Script("/bin/bash", timeout, logger);
        script.add("-c");
        script.add(command);

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        script.execute(parser);

        String output = parser.getLines();
        if (script.getExitValue() != 0) {
            logger.error("({}) Failed to fetch vCenter thumbprint for {}", sourceVmName, vcenterHost);
            return null;
        }

        return extractSha1Fingerprint(output);
    }

    private String extractSha1Fingerprint(String output) {
        String parsedOutput = StringUtils.trimToEmpty(output);
        if (StringUtils.isBlank(parsedOutput)) {
            return null;
        }

        for (String line : parsedOutput.split("\\R")) {
            String trimmedLine = StringUtils.trimToEmpty(line);
            if (StringUtils.isBlank(trimmedLine)) {
                continue;
            }

            Matcher matcher = SHA1_FINGERPRINT_PATTERN.matcher(trimmedLine);
            if (matcher.find()) {
                return matcher.group(1).toUpperCase(Locale.ROOT);
            }

            if (trimmedLine.matches("(?i)[0-9a-f]{2}(:[0-9a-f]{2})+")) {
                return trimmedLine.toUpperCase(Locale.ROOT);
            }
        }
        return null;
    }
}
