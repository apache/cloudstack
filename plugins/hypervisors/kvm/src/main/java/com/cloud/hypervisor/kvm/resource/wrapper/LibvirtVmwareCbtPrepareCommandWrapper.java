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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.VmwareCbtMigrationAnswer;
import com.cloud.agent.api.VmwareCbtPrepareCommand;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.agent.api.to.RemoteInstanceTO;
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
import org.apache.cloudstack.utils.qemu.QemuImg;

@ResourceWrapper(handles = VmwareCbtPrepareCommand.class)
public class LibvirtVmwareCbtPrepareCommandWrapper extends CommandWrapper<VmwareCbtPrepareCommand, Answer, LibvirtComputingResource> {

    private static final String DEFAULT_CBT_DISK_BASE_PATH = "/var/lib/libvirt/images/cloudstack-cbt";
    private static final Pattern SHA1_FINGERPRINT_PATTERN = Pattern.compile("(?i)(?:SHA1\\s+)?Fingerprint\\s*=\\s*([0-9A-F:]+)");

    @Override
    public Answer execute(VmwareCbtPrepareCommand cmd, LibvirtComputingResource serverResource) {
        if (!serverResource.hostSupportsVddkBlockCopy(cmd.getVddkLibDir())) {
            String msg = String.format("Cannot prepare VMware CBT migration %s on host %s. VDDK, qemu-img, qemu-nbd and qemu-io are required.",
                    cmd.getMigrationUuid(), serverResource.getPrivateIp());
            logger.info(msg);
            return new VmwareCbtMigrationAnswer(cmd, false, msg, cmd.getMigrationUuid());
        }

        try {
            validatePrepareCommand(cmd);
            String vddkLibDir = resolveVddkSetting(cmd.getVddkLibDir(), serverResource.getVddkLibDir());
            if (StringUtils.isBlank(vddkLibDir)) {
                String msg = String.format("Cannot prepare VMware CBT migration %s because no VDDK library directory is configured or detected.",
                        cmd.getMigrationUuid());
                logger.info(msg);
                return new VmwareCbtMigrationAnswer(cmd, false, msg, cmd.getMigrationUuid());
            }

            RemoteInstanceTO sourceInstance = cmd.getSourceInstance();
            String vddkThumbprint = resolveVddkSetting(cmd.getVddkThumbprint(), serverResource.getVddkThumbprint());
            if (StringUtils.isBlank(vddkThumbprint)) {
                vddkThumbprint = getVcenterThumbprint(sourceInstance.getVcenterHost(), getTimeout(cmd), sourceInstance.getInstanceName());
            }
            if (StringUtils.isBlank(vddkThumbprint)) {
                String msg = String.format("Cannot prepare VMware CBT migration %s because the vCenter SSL thumbprint could not be determined.",
                        cmd.getMigrationUuid());
                logger.info(msg);
                return new VmwareCbtMigrationAnswer(cmd, false, msg, cmd.getMigrationUuid());
            }

            KVMStoragePool targetPool = getTargetStoragePool(cmd, serverResource.getStoragePoolMgr());
            Path targetBasePath = null;
            if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW) {
                validateRbdTargetPool(cmd, targetPool);
            } else if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RAW_BLOCK_DEVICE) {
                validateBlockDeviceTargetPool(cmd, targetPool);
            } else {
                targetBasePath = getTargetBasePath(cmd, targetPool);
                Files.createDirectories(targetBasePath);
            }

            String passwordFilePath = writePasswordFile(cmd);
            try {
                List<VmwareCbtDiskSyncResultTO> diskResults = new ArrayList<>();
                long startTime = System.currentTimeMillis();
                for (VmwareCbtDiskTO disk : cmd.getDisks()) {
                    diskResults.add(copyDiskFromVmwareSnapshot(cmd, disk, targetPool, targetBasePath, passwordFilePath,
                            vddkLibDir, vddkThumbprint, serverResource));
                }
                long durationSeconds = Math.max(1L, (System.currentTimeMillis() - startTime) / 1000L);
                String msg = String.format("Initial VDDK full sync for VMware CBT migration %s completed for %s disk(s).",
                        cmd.getMigrationUuid(), diskResults.size());
                logger.info(msg);
                return new VmwareCbtMigrationAnswer(cmd, true, msg, cmd.getMigrationUuid(), 0,
                        0, 0, durationSeconds, false, diskResults);
            } finally {
                Files.deleteIfExists(Path.of(passwordFilePath));
            }
        } catch (Exception e) {
            String msg = String.format("Cannot prepare VMware CBT migration %s: %s",
                    cmd.getMigrationUuid(), StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            logger.error(msg, e);
            return new VmwareCbtMigrationAnswer(cmd, false, msg, cmd.getMigrationUuid());
        }
    }

    private void validatePrepareCommand(VmwareCbtPrepareCommand cmd) {
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
        if (StringUtils.isBlank(cmd.getBaselineSnapshotMor())) {
            throw new IllegalArgumentException("baseline VMware snapshot managed object reference is missing");
        }
        if (CollectionUtils.isEmpty(cmd.getDisks())) {
            throw new IllegalArgumentException("no source disks were provided for initial full sync");
        }
    }

    private long getTimeout(VmwareCbtPrepareCommand cmd) {
        return Math.max(1L, cmd.getWait()) * 1000L;
    }

    private KVMStoragePool getTargetStoragePool(VmwareCbtPrepareCommand cmd, KVMStoragePoolManager storagePoolMgr) {
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

    private void validateRbdTargetPool(VmwareCbtPrepareCommand cmd, KVMStoragePool targetPool) {
        if (targetPool == null || targetPool.getType() != Storage.StoragePoolType.RBD) {
            throw new IllegalArgumentException(String.format("VMware CBT migration %s requires an RBD destination storage pool for RBD target writing",
                    cmd.getMigrationUuid()));
        }
    }

    private void validateBlockDeviceTargetPool(VmwareCbtPrepareCommand cmd, KVMStoragePool targetPool) {
        if (targetPool == null || targetPool.getType() != Storage.StoragePoolType.Linstor) {
            throw new IllegalArgumentException(String.format("VMware CBT migration %s requires a Linstor destination storage pool for block device target writing",
                    cmd.getMigrationUuid()));
        }
    }

    private Path getTargetBasePath(VmwareCbtPrepareCommand cmd, KVMStoragePool targetPool) {
        if (targetPool != null) {
            return Path.of(targetPool.getLocalPath(), "cloudstack-cbt", cmd.getMigrationUuid()).normalize();
        }
        return Path.of(DEFAULT_CBT_DISK_BASE_PATH, cmd.getMigrationUuid()).normalize();
    }

    private String writePasswordFile(VmwareCbtPrepareCommand cmd) throws Exception {
        Path passwordFile = Files.createTempFile(String.format("vmware-cbt-%s-", sanitizeFileName(cmd.getMigrationUuid())),
                ".pass");
        Files.writeString(passwordFile, cmd.getSourceInstance().getVcenterPassword());
        setPosixFilePermissionsIfSupported(passwordFile, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE));
        return passwordFile.toString();
    }

    private void setPosixFilePermissionsIfSupported(Path path, Set<PosixFilePermission> permissions) throws Exception {
        try {
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException e) {
            logger.debug("POSIX permissions are not supported for temporary VMware CBT password file {}", path);
        }
    }

    private VmwareCbtDiskSyncResultTO copyDiskFromVmwareSnapshot(VmwareCbtPrepareCommand cmd, VmwareCbtDiskTO disk,
                                                                 KVMStoragePool targetPool, Path targetBasePath,
                                                                 String passwordFilePath,
                                                                 String vddkLibDir, String vddkThumbprint,
                                                                 LibvirtComputingResource serverResource) throws Exception {
        validateDisk(disk);
        long startTime = System.currentTimeMillis();
        String targetFormat = getTargetFormat(cmd, disk);
        String targetPath;
        String qemuTargetPath;
        boolean preCreatedTarget = false;
        if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW) {
            targetPath = getRbdTargetImageName(cmd, disk);
            deleteRbdTargetIfExists(targetPool, targetPath);
            qemuTargetPath = KVMPhysicalDisk.RBDStringBuilder(targetPool, getRbdImagePath(targetPool, targetPath));
        } else if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RAW_BLOCK_DEVICE) {
            targetPath = getBlockDeviceTargetName(disk);
            qemuTargetPath = createBlockDeviceTarget(targetPool, targetPath, disk);
            preCreatedTarget = true;
        } else {
            Path fileTargetPath = getTargetPath(disk, targetBasePath, targetFormat);
            if (!fileTargetPath.startsWith(targetBasePath)) {
                throw new IllegalArgumentException(String.format("resolved target path %s is outside %s", fileTargetPath, targetBasePath));
            }
            Files.deleteIfExists(fileTargetPath);
            targetPath = fileTargetPath.toString();
            qemuTargetPath = targetPath;
        }

        // Prefer multi-connection nbdcopy over a single-stream qemu-img convert when available:
        // for a pre-created local raw block-device target (Linstor) nbdcopy writes it directly;
        // for an RBD target it copies over a localhost qemu-nbd bridge (see the builder). qcow2
        // file targets keep qemu-img convert.
        boolean nbdcopyAvailable = serverResource.hostSupportsNbdcopy();
        String command = buildNbdkitFullCopyCommand(cmd, disk, passwordFilePath, vddkLibDir, vddkThumbprint,
                StringUtils.defaultIfBlank(cmd.getVddkTransports(), serverResource.getVddkTransports()),
                serverResource.getVddkNbdCompression(), qemuTargetPath, targetFormat, preCreatedTarget, nbdcopyAvailable);
        VmwareCbtCommandResult commandResult = executeLoggedBash(command, getTimeout(cmd),
                String.format("(%s) VMware CBT initial full sync disk %s", cmd.getMigrationUuid(), disk.getDiskId()));
        long durationSeconds = Math.max(1L, (System.currentTimeMillis() - startTime) / 1000L);
        if (commandResult.getExitValue() != 0) {
            String details = String.format("qemu-img conversion from VMware VDDK snapshot failed for source disk %s with exit code %s",
                    disk.getDiskId(), commandResult.getExitValue());
            throw new IllegalStateException(commandResult.appendLastCommandOutput(details));
        }

        return new VmwareCbtDiskSyncResultTO(disk.getDiskId(), targetPath, disk.getChangeId(),
                cmd.getBaselineSnapshotMor(), disk.getCapacityBytes(), durationSeconds, true,
                "Initial VDDK full sync completed");
    }

    private String getTargetFormat(VmwareCbtPrepareCommand cmd, VmwareCbtDiskTO disk) {
        if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW ||
                cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RAW_BLOCK_DEVICE) {
            return "raw";
        }
        return StringUtils.defaultIfBlank(disk.getTargetFormat(), "qcow2");
    }

    private String getBlockDeviceTargetName(VmwareCbtDiskTO disk) {
        String targetPath = StringUtils.trimToNull(disk.getTargetPath());
        if (targetPath == null) {
            throw new IllegalArgumentException(String.format("no block device target name was assigned for source disk %s", disk.getDiskId()));
        }
        String normalizedTargetPath = targetPath.replace('\\', '/');
        return StringUtils.contains(normalizedTargetPath, "/") ? StringUtils.substringAfterLast(normalizedTargetPath, "/") : normalizedTargetPath;
    }

    private String createBlockDeviceTarget(KVMStoragePool targetPool, String targetName, VmwareCbtDiskTO disk) {
        if (disk.getCapacityBytes() <= 0) {
            throw new IllegalArgumentException(String.format("source disk %s does not have a valid capacity, " +
                    "which is required to pre-create the block device target volume", disk.getDiskId()));
        }
        deleteRbdTargetIfExists(targetPool, targetName);
        KVMPhysicalDisk targetDisk = targetPool.createPhysicalDisk(targetName, QemuImg.PhysicalDiskFormat.RAW,
                Storage.ProvisioningType.THIN, disk.getCapacityBytes(), null);
        String devicePath = targetDisk == null ? null : targetDisk.getPath();
        if (StringUtils.isBlank(devicePath)) {
            throw new IllegalStateException(String.format("could not resolve a local device path for block device target volume %s", targetName));
        }
        return devicePath;
    }

    private String getRbdTargetImageName(VmwareCbtPrepareCommand cmd, VmwareCbtDiskTO disk) {
        String targetPath = StringUtils.trimToNull(disk.getTargetPath());
        if (targetPath != null) {
            String normalizedTargetPath = targetPath.replace('\\', '/');
            return StringUtils.contains(normalizedTargetPath, "/") ? StringUtils.substringAfterLast(normalizedTargetPath, "/") : normalizedTargetPath;
        }
        String sourceName = StringUtils.substringAfterLast(StringUtils.defaultIfBlank(disk.getSourceDiskPath(), disk.getDiskId()), "/");
        sourceName = StringUtils.substringBeforeLast(StringUtils.defaultIfBlank(sourceName, disk.getDiskId()), ".");
        return String.format("cloudstack-cbt-%s-%s-%s", sanitizeFileName(cmd.getMigrationUuid()),
                sanitizeFileName(disk.getDiskId()), sanitizeFileName(sourceName));
    }

    private String getRbdImagePath(KVMStoragePool targetPool, String imageName) {
        return String.format("%s/%s", StringUtils.removeEnd(targetPool.getSourceDir(), "/"), imageName);
    }

    private void deleteRbdTargetIfExists(KVMStoragePool targetPool, String imageName) {
        try {
            targetPool.deletePhysicalDisk(imageName, Storage.ImageFormat.RAW);
        } catch (RuntimeException e) {
            logger.debug("No existing RBD image {} was removed before VMware CBT initial sync, or removal was not required: {}",
                    imageName, e.getMessage());
        }
    }

    private Path getTargetPath(VmwareCbtDiskTO disk, Path targetBasePath, String targetFormat) {
        String targetPathValue = StringUtils.trimToNull(disk.getTargetPath());
        Path targetPath = targetPathValue == null ?
                targetBasePath.resolve(getTargetDiskFileName(disk, targetFormat)) :
                Path.of(targetPathValue);
        if (!targetPath.isAbsolute()) {
            targetPath = targetBasePath.resolve(targetPath);
        }
        return targetPath.normalize();
    }

    private void validateDisk(VmwareCbtDiskTO disk) {
        if (disk == null) {
            throw new IllegalArgumentException("source disk cannot be null");
        }
        if (StringUtils.isBlank(disk.getDiskId())) {
            throw new IllegalArgumentException("source disk ID is missing");
        }
        if (StringUtils.isBlank(disk.getSourceDiskPath())) {
            throw new IllegalArgumentException(String.format("source disk path is missing for disk %s", disk.getDiskId()));
        }
    }

    private String buildNbdkitFullCopyCommand(VmwareCbtPrepareCommand cmd, VmwareCbtDiskTO disk,
                                              String passwordFilePath, String vddkLibDir, String vddkThumbprint,
                                              String vddkTransports, String vddkNbdCompression, String targetPath,
                                              String targetFormat, boolean preCreatedTarget, boolean nbdcopyAvailable) {
        RemoteInstanceTO sourceInstance = cmd.getSourceInstance();
        String sourceVmMoref = getMorefValue(sourceInstance.getVmwareMoref());
        String snapshotMoref = getMorefValue(cmd.getBaselineSnapshotMor());
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
        if (StringUtils.isNotBlank(vddkNbdCompression)) {
            appendPluginParameter(nbdkit, "compression", vddkNbdCompression);
        }

        boolean rbdTarget = cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW;

        // RBD target with nbdcopy available: copy over a localhost qemu-nbd bridge (nbdcopy
        // needs both ends as NBD, and qemu-img create - which nbdcopy cannot do - pre-creates
        // the raw image). Multi-connection nbdcopy is typically faster than the single-stream
        // qemu-img convert. The fresh raw image reads as zeros, so --destination-is-zero keeps
        // it sparse.
        if (rbdTarget && nbdcopyAvailable && disk.getCapacityBytes() > 0) {
            return buildRbdNbdcopyBridgeCommand(nbdkit, targetPath, disk.getCapacityBytes());
        }

        // A pre-created target is a freshly provisioned (thin) volume that reads back as
        // zeros, but the tools may not assume that for a block device on their own:
        // without --destination-is-zero / --target-is-zero every zero block is written
        // out and the thin volume becomes fully allocated.
        boolean useNbdcopy = preCreatedTarget && nbdcopyAvailable;
        String runCommand = useNbdcopy
                ? String.format("nbdcopy %s\"$uri\" %s", preCreatedTarget ? "--destination-is-zero " : "", shellQuote(targetPath))
                : String.format("qemu-img convert %s-f raw -O %s \"$uri\" %s",
                        preCreatedTarget ? "-n --target-is-zero " : "", shellQuote(targetFormat), shellQuote(targetPath));
        return nbdkit.append("--run ").append(shellQuote(runCommand)).toString();
    }

    /**
     * Wraps the nbdkit VDDK source with a localhost qemu-nbd bridge over the RBD target so
     * nbdcopy can copy NBD-to-NBD. The returned string is a self-contained bash script (run
     * via bash -c) that pre-creates the raw RBD image, starts qemu-nbd, runs the nbdcopy, and
     * tears the bridge down on exit. Package-private for unit testing.
     */
    String buildRbdNbdcopyBridgeCommand(StringBuilder nbdkitSource, String qemuRbdTarget, long capacityBytes) {
        int port = allocateLocalhostPort();
        nbdkitSource.append("--run ").append(shellQuote(
                String.format("nbdcopy --destination-is-zero \"$uri\" nbd://localhost:%d", port)));
        StringBuilder script = new StringBuilder();
        script.append("set -euo pipefail; ");
        script.append("__pidf=$(mktemp); ");
        script.append("cleanup() { set +e; if [[ -s \"$__pidf\" ]]; then __p=$(cat \"$__pidf\"); ");
        script.append("kill \"$__p\" >/dev/null 2>&1 || true; ");
        script.append("for __a in {1..20}; do kill -0 \"$__p\" >/dev/null 2>&1 || break; sleep 0.1; done; fi; ");
        script.append("rm -f \"$__pidf\"; }; trap cleanup EXIT; ");
        script.append("qemu-img create -f raw ").append(shellQuote(qemuRbdTarget)).append(" ").append(capacityBytes).append("; ");
        script.append("qemu-nbd --fork --persistent --shared=8 --format=raw --bind=127.0.0.1 --port=").append(port)
                .append(" --pid-file=\"$__pidf\" ").append(shellQuote(qemuRbdTarget)).append("; ");
        script.append(nbdkitSource);
        return script.toString();
    }

    private int allocateLocalhostPort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            socket.setReuseAddress(false);
            return socket.getLocalPort();
        } catch (java.io.IOException e) {
            throw new CloudRuntimeException("Unable to allocate a localhost port for the RBD qemu-nbd bridge", e);
        }
    }

    private void appendPluginParameter(StringBuilder command, String key, String value) {
        command.append(key).append("=").append(shellQuote(value)).append(" ");
    }

    private String getTargetDiskFileName(VmwareCbtDiskTO disk, String targetFormat) {
        String sourceName = StringUtils.substringAfterLast(StringUtils.defaultIfBlank(disk.getSourceDiskPath(), disk.getDiskId()), "/");
        sourceName = StringUtils.substringBeforeLast(StringUtils.defaultIfBlank(sourceName, disk.getDiskId()), ".");
        return String.format("%s-%s.%s", sanitizeFileName(disk.getDiskId()), sanitizeFileName(sourceName),
                sanitizeFileName(targetFormat));
    }

    private String sanitizeFileName(String value) {
        String sanitized = StringUtils.defaultIfBlank(value, "disk").replaceAll("[^A-Za-z0-9._-]", "-");
        return StringUtils.defaultIfBlank(sanitized, "disk");
    }

    private String getMorefValue(String moref) {
        String value = StringUtils.trimToNull(moref);
        if (value == null) {
            return null;
        }
        return value.contains(":") ? StringUtils.substringAfter(value, ":") : value;
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

    private String getVcenterThumbprint(String vcenterHost, long timeout, String sourceVmName) {
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
