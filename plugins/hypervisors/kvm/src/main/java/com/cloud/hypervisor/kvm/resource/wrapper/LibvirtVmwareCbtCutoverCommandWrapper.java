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

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.VmwareCbtCutoverCommand;
import com.cloud.agent.api.VmwareCbtMigrationAnswer;
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
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = VmwareCbtCutoverCommand.class)
public class LibvirtVmwareCbtCutoverCommandWrapper extends CommandWrapper<VmwareCbtCutoverCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(VmwareCbtCutoverCommand cmd, LibvirtComputingResource serverResource) {
        if (!serverResource.hostSupportsVddkBlockCopy(cmd.getVddkLibDir())) {
            String msg = String.format("Cannot cut over VMware CBT migration %s on host %s. VDDK, qemu-img, qemu-nbd and qemu-io are required.",
                    cmd.getMigrationUuid(), serverResource.getPrivateIp());
            logger.info(msg);
            return new VmwareCbtMigrationAnswer(cmd, false, msg, cmd.getMigrationUuid());
        }

        if (!cmd.getRunVirtV2vFinalization()) {
            String msg = String.format("VMware CBT cutover finalization for migration %s was skipped by command policy.",
                    cmd.getMigrationUuid());
            logger.info(msg);
            return new VmwareCbtMigrationAnswer(cmd, true, msg, cmd.getMigrationUuid(), cmd.getFinalCycleNumber(),
                    0, 0, 0, true, getDiskResults(cmd.getDisks(), 0, msg));
        }

        long startTime = System.currentTimeMillis();
        Path sourceXmlPath = null;
        Path outputXmlPath = null;
        Path fallbackOutputDir = null;
        Path fallbackStagingDir = null;
        Path rbdNbdBridgeScriptPath = null;
        boolean keepFallbackOutputDir = false;
        try {
            validateCutoverCommand(cmd);
            KVMStoragePool targetPool = getTargetStoragePool(cmd, serverResource.getStoragePoolMgr());
            if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW) {
                validateRbdTargetPool(cmd, targetPool);
            }

            VirtV2vFinalizationMode finalizationMode = getVirtV2vFinalizationMode(serverResource);
            boolean inPlaceFinalization = finalizationMode.isInPlace();
            if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW && !inPlaceFinalization) {
                throw new IllegalArgumentException("RBD target finalization requires virt-v2v in-place support on the selected KVM host; non-in-place fallback finalization cannot write directly back to RBD targets.");
            }
            List<RbdNbdBridge> rbdNbdBridges = cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW ?
                    createRbdNbdBridges(cmd, targetPool) : List.of();
            sourceXmlPath = writeSourceXml(cmd, rbdNbdBridges);

            String command;
            String logSuffix;
            if (inPlaceFinalization) {
                if (finalizationMode == VirtV2vFinalizationMode.VIRT_V2V_IN_PLACE_BINARY) {
                    outputXmlPath = Files.createTempFile(String.format("vmware-cbt-%s-v2v-in-place-output-",
                            sanitizeFileName(cmd.getMigrationUuid())), ".xml");
                }
                command = buildVirtV2vInPlaceCommand(sourceXmlPath, outputXmlPath, serverResource, finalizationMode);
                if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW) {
                    rbdNbdBridgeScriptPath = writeRbdNbdBridgeScript(cmd, rbdNbdBridges, command);
                    command = String.format("bash %s", shellQuote(rbdNbdBridgeScriptPath.toString()));
                }
                logSuffix = String.format("%s finalization", finalizationMode.getDisplayName());
            } else {
                if (!cmd.getAllowNonInPlaceFinalization()) {
                    throw new IllegalArgumentException("Selected KVM host cannot finalize VMware CBT migration in-place. Enable virt-v2v in-place support or explicitly allow non-in-place fallback finalization.");
                }
                validateFallbackCapacity(cmd);
                fallbackOutputDir = getFallbackOutputDir(cmd);
                fallbackStagingDir = getFallbackStagingDir(cmd);
                command = buildVirtV2vFallbackCommand(sourceXmlPath, fallbackOutputDir, fallbackStagingDir, serverResource);
                logSuffix = String.format("%s finalization", finalizationMode.getDisplayName());
            }

            VmwareCbtCommandResult commandResult = executeLoggedBash(command, getTimeout(cmd),
                    String.format("(%s) VMware CBT %s", cmd.getMigrationUuid(), logSuffix));
            long durationSeconds = Math.max(1L, (System.currentTimeMillis() - startTime) / 1000L);
            if (commandResult.getExitValue() != 0) {
                String msg = String.format("%s failed for VMware CBT migration %s with exit code %s.",
                        finalizationMode.getDisplayName(), cmd.getMigrationUuid(), commandResult.getExitValue());
                msg = commandResult.appendLastCommandOutput(msg);
                logger.info(msg);
                return new VmwareCbtMigrationAnswer(cmd, false, msg, cmd.getMigrationUuid(), cmd.getFinalCycleNumber(),
                        0, 0, durationSeconds, false, null);
            }

            List<VmwareCbtDiskSyncResultTO> diskResults = inPlaceFinalization ?
                    getDiskResults(cmd.getDisks(), durationSeconds,
                            String.format("Final %s conversion completed", finalizationMode.getDisplayName())) :
                    getFallbackDiskResults(cmd, fallbackOutputDir, durationSeconds);
            diskResults = relocateFinalizedDiskResultsToStorageRoot(cmd.getMigrationUuid(), diskResults);
            if (!inPlaceFinalization) {
                deleteFallbackSourceDisks(cmd);
                keepFallbackOutputDir = containsDiskResultUnderDirectory(diskResults, fallbackOutputDir);
            }
            String msg = String.format("Final %s conversion completed for VMware CBT migration %s.",
                    finalizationMode.getDisplayName(), cmd.getMigrationUuid());
            logger.info(msg);
            return new VmwareCbtMigrationAnswer(cmd, true, msg, cmd.getMigrationUuid(), cmd.getFinalCycleNumber(),
                    0, 0, durationSeconds, true, diskResults);
        } catch (IllegalArgumentException e) {
            String msg = String.format("Cannot cut over VMware CBT migration %s: %s",
                    cmd.getMigrationUuid(), StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            logger.info(msg);
            return new VmwareCbtMigrationAnswer(cmd, false, msg, cmd.getMigrationUuid(), cmd.getFinalCycleNumber(),
                    0, 0, Math.max(1L, (System.currentTimeMillis() - startTime) / 1000L), false, null);
        } catch (Exception e) {
            String msg = String.format("Cannot cut over VMware CBT migration %s: %s",
                    cmd.getMigrationUuid(), StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            logger.error(msg, e);
            return new VmwareCbtMigrationAnswer(cmd, false, msg, cmd.getMigrationUuid(), cmd.getFinalCycleNumber(),
                    0, 0, Math.max(1L, (System.currentTimeMillis() - startTime) / 1000L), false, null);
        } finally {
            deleteTempFile(sourceXmlPath);
            deleteTempFile(outputXmlPath);
            deleteTempFile(rbdNbdBridgeScriptPath);
            deleteTempTree(fallbackStagingDir);
            if (!keepFallbackOutputDir) {
                deleteTempTree(fallbackOutputDir);
            }
        }
    }

    protected VirtV2vFinalizationMode getVirtV2vFinalizationMode(LibvirtComputingResource serverResource) {
        if (serverResource.hostSupportsVirtV2vInPlaceBinary()) {
            return VirtV2vFinalizationMode.VIRT_V2V_IN_PLACE_BINARY;
        }
        if (serverResource.hostSupportsVirtV2vInPlaceOption()) {
            return VirtV2vFinalizationMode.VIRT_V2V_IN_PLACE_OPTION;
        }
        return VirtV2vFinalizationMode.VIRT_V2V_FALLBACK;
    }

    private void validateCutoverCommand(VmwareCbtCutoverCommand cmd) {
        if (StringUtils.isBlank(cmd.getMigrationUuid())) {
            throw new IllegalArgumentException("migration UUID is missing");
        }
        if (CollectionUtils.isEmpty(cmd.getDisks())) {
            throw new IllegalArgumentException("no target disks were provided for final conversion");
        }

        for (VmwareCbtDiskTO disk : cmd.getDisks()) {
            validateDisk(cmd, disk);
        }
    }

    private void validateDisk(VmwareCbtCutoverCommand cmd, VmwareCbtDiskTO disk) {
        if (disk == null) {
            throw new IllegalArgumentException("target disk cannot be null");
        }
        if (StringUtils.isBlank(disk.getDiskId())) {
            throw new IllegalArgumentException("target disk ID is missing");
        }
        if (StringUtils.isBlank(disk.getTargetPath())) {
            throw new IllegalArgumentException(String.format("target path is missing for disk %s", disk.getDiskId()));
        }
        if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW) {
            String targetFormat = StringUtils.defaultIfBlank(disk.getTargetFormat(), "raw").toLowerCase(Locale.ROOT);
            if (!StringUtils.equals(targetFormat, "raw")) {
                throw new IllegalArgumentException(String.format("target disk %s uses unsupported finalization format %s; only raw RBD targets are supported for RBD finalization",
                        disk.getDiskId(), targetFormat));
            }
            return;
        }
        if (!Files.isRegularFile(Path.of(disk.getTargetPath()))) {
            throw new IllegalArgumentException(String.format("target disk %s does not exist at %s",
                    disk.getDiskId(), disk.getTargetPath()));
        }
        String targetFormat = StringUtils.defaultIfBlank(disk.getTargetFormat(), "qcow2").toLowerCase(Locale.ROOT);
        if (!StringUtils.equals(targetFormat, "qcow2")) {
            throw new IllegalArgumentException(String.format("target disk %s uses unsupported finalization format %s; only qcow2 file targets are currently supported",
                    disk.getDiskId(), targetFormat));
        }
    }

    private Path writeSourceXml(VmwareCbtCutoverCommand cmd, List<RbdNbdBridge> rbdNbdBridges) throws Exception {
        Path sourceXmlPath = Files.createTempFile(String.format("vmware-cbt-%s-v2v-in-place-source-",
                sanitizeFileName(cmd.getMigrationUuid())), ".xml");
        Files.writeString(sourceXmlPath, buildSourceXml(cmd, rbdNbdBridges));
        return sourceXmlPath;
    }

    private String buildSourceXml(VmwareCbtCutoverCommand cmd, List<RbdNbdBridge> rbdNbdBridges) {
        StringBuilder xml = new StringBuilder();
        xml.append("<domain type='kvm'>\n");
        xml.append("  <name>").append(escapeXml(StringUtils.defaultIfBlank(cmd.getMigrationUuid(), "vmware-cbt-migration"))).append("</name>\n");
        xml.append("  <memory unit='KiB'>1048576</memory>\n");
        xml.append("  <vcpu>1</vcpu>\n");
        xml.append("  <os>\n");
        xml.append("    <type>hvm</type>\n");
        xml.append("    <boot dev='hd'/>\n");
        xml.append("  </os>\n");
        xml.append("  <features>\n");
        xml.append("    <acpi/>\n");
        xml.append("    <apic/>\n");
        xml.append("  </features>\n");
        xml.append("  <devices>\n");
        for (int index = 0; index < cmd.getDisks().size(); index++) {
            VmwareCbtDiskTO disk = cmd.getDisks().get(index);
            if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW) {
                appendRbdNbdDiskXml(xml, disk, rbdNbdBridges.get(index), index);
            } else {
                xml.append("    <disk type='file' device='disk'>\n");
                xml.append("      <driver name='qemu' type='qcow2'/>\n");
                xml.append("      <source file='").append(escapeXml(disk.getTargetPath())).append("'/>\n");
                xml.append("      <target dev='").append(getDiskDeviceName(index)).append("' bus='scsi'/>\n");
                xml.append("    </disk>\n");
            }
        }
        xml.append("  </devices>\n");
        xml.append("</domain>\n");
        return xml.toString();
    }

    private void appendRbdNbdDiskXml(StringBuilder xml, VmwareCbtDiskTO disk, RbdNbdBridge rbdNbdBridge, int index) {
        xml.append("    <disk type='network' device='disk'>\n");
        xml.append("      <driver name='qemu' type='raw'/>\n");
        xml.append("      <source protocol='nbd'>\n");
        xml.append("        <host name='localhost' port='").append(rbdNbdBridge.port).append("'/>\n");
        xml.append("      </source>\n");
        xml.append("      <target dev='").append(getDiskDeviceName(index)).append("' bus='scsi'/>\n");
        xml.append("    </disk>\n");
        logger.info("Prepared temporary localhost NBD bridge on port {} for VMware CBT RBD target disk {}",
                rbdNbdBridge.port, disk.getDiskId());
    }

    private List<RbdNbdBridge> createRbdNbdBridges(VmwareCbtCutoverCommand cmd, KVMStoragePool targetPool) throws Exception {
        List<RbdNbdBridge> bridges = new ArrayList<>();
        for (VmwareCbtDiskTO disk : cmd.getDisks()) {
            int port = allocateLocalhostPort();
            Path pidFile = Files.createTempFile(String.format("vmware-cbt-%s-qemu-nbd-",
                    sanitizeFileName(cmd.getMigrationUuid())), ".pid");
            Files.deleteIfExists(pidFile);
            String qemuRbdPath = KVMPhysicalDisk.RBDStringBuilder(targetPool, getRbdImagePath(targetPool, disk.getTargetPath()));
            bridges.add(new RbdNbdBridge(port, pidFile, qemuRbdPath));
        }
        return bridges;
    }

    private int allocateLocalhostPort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(false);
            return socket.getLocalPort();
        }
    }

    private Path writeRbdNbdBridgeScript(VmwareCbtCutoverCommand cmd, List<RbdNbdBridge> rbdNbdBridges,
                                         String virtV2vCommand) throws Exception {
        Path scriptPath = Files.createTempFile(String.format("vmware-cbt-%s-rbd-finalize-",
                sanitizeFileName(cmd.getMigrationUuid())), ".sh");
        StringBuilder script = new StringBuilder();
        script.append("#!/bin/bash\n");
        script.append("set -euo pipefail\n");
        script.append("cleanup() {\n");
        script.append("  set +e\n");
        script.append("  for pid_file in");
        for (RbdNbdBridge bridge : rbdNbdBridges) {
            script.append(" ").append(shellQuote(bridge.pidFile.toString()));
        }
        script.append("; do\n");
        script.append("    if [[ -s \"$pid_file\" ]]; then\n");
        script.append("      pid=$(cat \"$pid_file\")\n");
        script.append("      kill \"$pid\" >/dev/null 2>&1 || true\n");
        script.append("      for attempt in {1..20}; do\n");
        script.append("        kill -0 \"$pid\" >/dev/null 2>&1 || break\n");
        script.append("        sleep 0.1\n");
        script.append("      done\n");
        script.append("    fi\n");
        script.append("    rm -f \"$pid_file\"\n");
        script.append("  done\n");
        script.append("}\n");
        script.append("trap cleanup EXIT\n");
        for (RbdNbdBridge bridge : rbdNbdBridges) {
            script.append("qemu-nbd --fork --persistent --shared=1 --format=raw --bind=127.0.0.1 --port=")
                    .append(bridge.port)
                    .append(" --pid-file=").append(shellQuote(bridge.pidFile.toString()))
                    .append(" ").append(shellQuote(bridge.qemuRbdPath)).append("\n");
        }
        script.append(virtV2vCommand).append("\n");
        Files.writeString(scriptPath, script.toString());
        setPosixFilePermissionsIfSupported(scriptPath, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        return scriptPath;
    }

    private void setPosixFilePermissionsIfSupported(Path path, Set<PosixFilePermission> permissions) throws Exception {
        try {
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException e) {
            logger.debug("POSIX file permissions are not supported for {}", path);
        }
    }

    private static class RbdNbdBridge {
        private final int port;
        private final Path pidFile;
        private final String qemuRbdPath;

        private RbdNbdBridge(int port, Path pidFile, String qemuRbdPath) {
            this.port = port;
            this.pidFile = pidFile;
            this.qemuRbdPath = qemuRbdPath;
        }
    }

    private KVMStoragePool getTargetStoragePool(VmwareCbtCutoverCommand cmd, KVMStoragePoolManager storagePoolMgr) {
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

    private void validateRbdTargetPool(VmwareCbtCutoverCommand cmd, KVMStoragePool targetPool) {
        if (targetPool == null || targetPool.getType() != Storage.StoragePoolType.RBD) {
            throw new IllegalArgumentException(String.format("VMware CBT migration %s requires an RBD destination storage pool for RBD target finalization",
                    cmd.getMigrationUuid()));
        }
    }

    private String getRbdImagePath(KVMStoragePool targetPool, String targetPath) {
        String normalizedTargetPath = StringUtils.defaultString(targetPath).replace('\\', '/');
        String imageName = StringUtils.contains(normalizedTargetPath, "/") ? StringUtils.substringAfterLast(normalizedTargetPath, "/") : normalizedTargetPath;
        return String.format("%s/%s", StringUtils.removeEnd(targetPool.getSourceDir(), "/"), imageName);
    }

    private String getDiskDeviceName(int index) {
        StringBuilder suffix = new StringBuilder();
        int value = index;
        do {
            suffix.insert(0, (char)('a' + (value % 26)));
            value = (value / 26) - 1;
        } while (value >= 0);
        return "sd" + suffix;
    }

    private String buildVirtV2vInPlaceCommand(Path sourceXmlPath, Path outputXmlPath, LibvirtComputingResource serverResource,
                                              VirtV2vFinalizationMode finalizationMode) {
        StringBuilder command = new StringBuilder();
        appendLibguestfsBackend(command, serverResource);
        if (finalizationMode == VirtV2vFinalizationMode.VIRT_V2V_IN_PLACE_OPTION) {
            command.append("virt-v2v --root first -i libvirtxml ");
            command.append(shellQuote(sourceXmlPath.toString())).append(" ");
            command.append("--in-place -v");
        } else {
            command.append("virt-v2v-in-place --root first -i libvirtxml ");
            command.append(shellQuote(sourceXmlPath.toString())).append(" ");
            command.append("-O ").append(shellQuote(outputXmlPath.toString())).append(" ");
            command.append("-v");
        }
        return command.toString();
    }

    private String buildVirtV2vFallbackCommand(Path sourceXmlPath, Path outputDir, Path stagingDir,
                                               LibvirtComputingResource serverResource) {
        StringBuilder command = new StringBuilder();
        appendLibguestfsBackend(command, serverResource);
        command.append("export TMPDIR=").append(shellQuote(stagingDir.toString())).append(" && ");
        command.append("virt-v2v --root first -i libvirtxml ");
        command.append(shellQuote(sourceXmlPath.toString())).append(" ");
        command.append("-o local -os ").append(shellQuote(outputDir.toString())).append(" ");
        command.append("-of qcow2 -v");
        return command.toString();
    }

    private void appendLibguestfsBackend(StringBuilder command, LibvirtComputingResource serverResource) {
        String libguestfsBackend = StringUtils.trimToNull(serverResource.getLibguestfsBackend());
        if (StringUtils.isNotBlank(libguestfsBackend)) {
            command.append("export LIBGUESTFS_BACKEND=").append(shellQuote(libguestfsBackend)).append(" && ");
        }
    }

    private void validateFallbackCapacity(VmwareCbtCutoverCommand cmd) throws Exception {
        Path targetBasePath = getTargetBasePath(cmd);
        long requiredBytes = saturatingMultiply(sumDiskCapacityBytes(cmd), 2L);
        long availableBytes = Files.getFileStore(targetBasePath).getUsableSpace();
        if (availableBytes < requiredBytes) {
            throw new IllegalArgumentException(String.format("Non-in-place fallback finalization requires additional free space on target primary storage. Required: %s bytes, available: %s bytes.",
                    requiredBytes, availableBytes));
        }
    }

    private long sumDiskCapacityBytes(VmwareCbtCutoverCommand cmd) throws Exception {
        long total = 0L;
        for (VmwareCbtDiskTO disk : cmd.getDisks()) {
            long capacity = disk.getCapacityBytes();
            if (capacity <= 0) {
                capacity = Files.size(Path.of(disk.getTargetPath()));
            }
            total = saturatingAdd(total, capacity);
        }
        return total;
    }

    private long saturatingAdd(long left, long right) {
        if (Long.MAX_VALUE - left < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private long saturatingMultiply(long value, long multiplier) {
        if (value > 0 && multiplier > Long.MAX_VALUE / value) {
            return Long.MAX_VALUE;
        }
        return value * multiplier;
    }

    private Path getFallbackOutputDir(VmwareCbtCutoverCommand cmd) throws Exception {
        Path targetBasePath = getTargetBasePath(cmd);
        Path outputDir = Files.createTempDirectory(targetBasePath, "virt-v2v-output-").normalize();
        if (!outputDir.startsWith(targetBasePath)) {
            throw new IllegalArgumentException(String.format("resolved fallback output path %s is outside %s",
                    outputDir, targetBasePath));
        }
        return outputDir;
    }

    private Path getFallbackStagingDir(VmwareCbtCutoverCommand cmd) throws Exception {
        Path targetBasePath = getTargetBasePath(cmd);
        Path stagingDir = Files.createTempDirectory(targetBasePath, "virt-v2v-tmp-").normalize();
        if (!stagingDir.startsWith(targetBasePath)) {
            throw new IllegalArgumentException(String.format("resolved fallback staging path %s is outside %s",
                    stagingDir, targetBasePath));
        }
        return stagingDir;
    }

    private Path getTargetBasePath(VmwareCbtCutoverCommand cmd) {
        Path firstDiskPath = Path.of(cmd.getDisks().get(0).getTargetPath()).normalize();
        Path targetBasePath = firstDiskPath.getParent();
        if (targetBasePath == null) {
            throw new IllegalArgumentException("unable to determine VMware CBT target disk directory");
        }
        return targetBasePath;
    }

    private List<VmwareCbtDiskSyncResultTO> getFallbackDiskResults(VmwareCbtCutoverCommand cmd, Path fallbackOutputDir,
                                                                   long durationSeconds) throws Exception {
        List<Path> outputFiles;
        try (Stream<Path> stream = Files.list(fallbackOutputDir)) {
            outputFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> !StringUtils.endsWithIgnoreCase(path.getFileName().toString(), ".xml"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .collect(Collectors.toList());
        }
        if (outputFiles.size() < cmd.getDisks().size()) {
            throw new IllegalArgumentException(String.format("virt-v2v fallback produced %s disk file(s), expected at least %s",
                    outputFiles.size(), cmd.getDisks().size()));
        }
        List<VmwareCbtDiskSyncResultTO> results = new ArrayList<>();
        for (int index = 0; index < cmd.getDisks().size(); index++) {
            VmwareCbtDiskTO disk = cmd.getDisks().get(index);
            results.add(new VmwareCbtDiskSyncResultTO(disk.getDiskId(), outputFiles.get(index).toString(),
                    disk.getChangeId(), disk.getSnapshotMor(), 0, durationSeconds, true,
                    "Final virt-v2v fallback conversion completed"));
        }
        return results;
    }

    protected List<VmwareCbtDiskSyncResultTO> relocateFinalizedDiskResultsToStorageRoot(String migrationUuid,
                                                                                       List<VmwareCbtDiskSyncResultTO> diskResults) throws Exception {
        if (CollectionUtils.isEmpty(diskResults)) {
            return diskResults;
        }

        List<VmwareCbtDiskSyncResultTO> relocatedResults = new ArrayList<>();
        Set<Path> usedDestinations = new HashSet<>();
        List<FinalizedDiskMove> completedMoves = new ArrayList<>();
        try {
            for (VmwareCbtDiskSyncResultTO diskResult : diskResults) {
                if (diskResult == null || !diskResult.getResult() || StringUtils.isBlank(diskResult.getTargetPath())) {
                    relocatedResults.add(diskResult);
                    continue;
                }

                Path sourcePath = Path.of(diskResult.getTargetPath()).normalize();
                Path storageRoot = getStorageRootForCbtPath(migrationUuid, sourcePath);
                if (storageRoot == null) {
                    relocatedResults.add(diskResult);
                    continue;
                }

                Path destinationPath = getAvailableRootDiskPath(storageRoot, usedDestinations);
                logger.info("Relocating finalized VMware CBT disk {} to primary storage root {}", sourcePath, destinationPath);
                Files.move(sourcePath, destinationPath);
                completedMoves.add(new FinalizedDiskMove(sourcePath, destinationPath));
                relocatedResults.add(new VmwareCbtDiskSyncResultTO(diskResult.getDiskId(), destinationPath.toString(),
                        diskResult.getChangeId(), diskResult.getSnapshotMor(), diskResult.getChangedBytes(),
                        diskResult.getDurationSeconds(), diskResult.getResult(), diskResult.getDetails()));
            }
        } catch (Exception e) {
            rollbackFinalizedDiskMoves(completedMoves);
            throw e;
        }
        return relocatedResults;
    }

    private Path getStorageRootForCbtPath(String migrationUuid, Path targetPath) {
        if (StringUtils.isBlank(migrationUuid) || targetPath == null) {
            return null;
        }
        String normalizedPath = targetPath.normalize().toString().replace('\\', '/');
        String marker = String.format("/cloudstack-cbt/%s/", migrationUuid);
        int markerIndex = normalizedPath.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }
        String storageRoot = StringUtils.trimToNull(normalizedPath.substring(0, markerIndex));
        return storageRoot == null ? null : Path.of(storageRoot).normalize();
    }

    private Path getAvailableRootDiskPath(Path storageRoot, Set<Path> usedDestinations) {
        while (true) {
            Path candidate = storageRoot.resolve(UUID.randomUUID().toString()).normalize();
            if (!Files.exists(candidate) && usedDestinations.add(candidate)) {
                return candidate;
            }
        }
    }

    private boolean containsDiskResultUnderDirectory(List<VmwareCbtDiskSyncResultTO> diskResults, Path directory) {
        if (CollectionUtils.isEmpty(diskResults) || directory == null) {
            return false;
        }
        Path normalizedDirectory = directory.normalize();
        for (VmwareCbtDiskSyncResultTO diskResult : diskResults) {
            if (diskResult != null && StringUtils.isNotBlank(diskResult.getTargetPath()) &&
                    Path.of(diskResult.getTargetPath()).normalize().startsWith(normalizedDirectory)) {
                return true;
            }
        }
        return false;
    }

    private void rollbackFinalizedDiskMoves(List<FinalizedDiskMove> completedMoves) {
        if (CollectionUtils.isEmpty(completedMoves)) {
            return;
        }
        for (int index = completedMoves.size() - 1; index >= 0; index--) {
            FinalizedDiskMove move = completedMoves.get(index);
            try {
                if (Files.exists(move.destinationPath) && !Files.exists(move.sourcePath)) {
                    Files.move(move.destinationPath, move.sourcePath);
                }
            } catch (Exception rollbackException) {
                logger.warn("Failed to roll back finalized VMware CBT disk relocation from {} to {}",
                        move.destinationPath, move.sourcePath, rollbackException);
            }
        }
    }

    private static class FinalizedDiskMove {
        private final Path sourcePath;
        private final Path destinationPath;

        private FinalizedDiskMove(Path sourcePath, Path destinationPath) {
            this.sourcePath = sourcePath;
            this.destinationPath = destinationPath;
        }
    }

    private void deleteFallbackSourceDisks(VmwareCbtCutoverCommand cmd) {
        for (VmwareCbtDiskTO disk : cmd.getDisks()) {
            deleteTempFile(Path.of(disk.getTargetPath()));
        }
    }

    protected VmwareCbtCommandResult executeLoggedBash(String command, long timeout, String logPrefix) {
        Script script = new Script("/bin/bash", timeout, logger);
        script.add("-c");
        script.add(command);
        VmwareCbtCommandOutputLogger outputLogger = new VmwareCbtCommandOutputLogger(logger, logPrefix);
        script.execute(outputLogger);
        return new VmwareCbtCommandResult(script.getExitValue(), outputLogger.getLastRelevantOutputLine());
    }

    private long getTimeout(VmwareCbtCutoverCommand cmd) {
        return Math.max(1L, cmd.getWait()) * 1000L;
    }

    private List<VmwareCbtDiskSyncResultTO> getDiskResults(List<VmwareCbtDiskTO> disks, long durationSeconds,
                                                           String details) {
        List<VmwareCbtDiskSyncResultTO> results = new ArrayList<>();
        if (CollectionUtils.isEmpty(disks)) {
            return results;
        }
        for (VmwareCbtDiskTO disk : disks) {
            results.add(new VmwareCbtDiskSyncResultTO(disk.getDiskId(), disk.getTargetPath(),
                    disk.getChangeId(), disk.getSnapshotMor(), 0, durationSeconds, true, details));
        }
        return results;
    }

    private void deleteTempFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (Exception e) {
            logger.debug("Unable to delete temporary VMware CBT cutover file {}: {}", path, e.getMessage());
        }
    }

    private void deleteTempTree(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            List<Path> paths = stream.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            for (Path entry : paths) {
                Files.deleteIfExists(entry);
            }
        } catch (Exception e) {
            logger.debug("Unable to delete temporary VMware CBT cutover directory {}: {}", path, e.getMessage());
        }
    }

    private String shellQuote(String value) {
        return "'" + StringUtils.defaultString(value).replace("'", "'\"'\"'") + "'";
    }

    private String escapeXml(String value) {
        return StringUtils.defaultString(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String sanitizeFileName(String value) {
        String sanitized = StringUtils.defaultIfBlank(value, "migration").replaceAll("[^A-Za-z0-9._-]", "-");
        return StringUtils.defaultIfBlank(sanitized, "migration");
    }

    protected enum VirtV2vFinalizationMode {
        VIRT_V2V_IN_PLACE_BINARY("virt-v2v-in-place", true),
        VIRT_V2V_IN_PLACE_OPTION("virt-v2v --in-place", true),
        VIRT_V2V_FALLBACK("virt-v2v fallback", false);

        private final String displayName;
        private final boolean inPlace;

        VirtV2vFinalizationMode(String displayName, boolean inPlace) {
            this.displayName = displayName;
            this.inPlace = inPlace;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isInPlace() {
            return inPlace;
        }
    }
}
