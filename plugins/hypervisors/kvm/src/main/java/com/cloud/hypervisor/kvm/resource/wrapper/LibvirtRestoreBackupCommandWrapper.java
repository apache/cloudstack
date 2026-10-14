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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cloudstack.backup.BackupAnswer;
import org.apache.cloudstack.backup.RestoreBackupCommand;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.cloud.utils.RbdUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.vm.VirtualMachine;

@ResourceWrapper(handles = RestoreBackupCommand.class)
public class LibvirtRestoreBackupCommandWrapper extends CommandWrapper<RestoreBackupCommand, Answer, LibvirtComputingResource> {
    private static final String BACKUP_TEMP_FILE_PREFIX = "csbackup";
    private static final String FILE_PATH_PLACEHOLDER = "%s/%s";
    // Detects whether a qcow2 file references a parent in its backing-file metadata.
    // Returns 0 (true) when a backing file is present, 1 when not. Uses --output=json
    // so the test is robust to qemu-img version differences in human-readable output.
    private static final String QEMU_IMG_HAS_BACKING_COMMAND =
            "qemu-img info --output=json %s 2>/dev/null | grep -q '\"backing-filename\"'";
    private static final String FORMAT_RBD_DIFF = "rbddiff";
    private static final String FORMAT_QCOW2 = "qcow2";
    // `rbd info --format json` reports the image size in bytes.
    private static final Pattern RBD_INFO_SIZE = Pattern.compile("\"size\"\\s*:\\s*(\\d+)");
    // Matches the features CloudStack itself creates RBD volumes with
    // (LibvirtStorageAdaptor.RBD_FEATURES), so a restored image behaves like any other. object-map
    // and fast-diff in particular are what make a later `rbd export-diff` cheap.
    private static final List<String> RBD_IMAGE_FEATURES =
            List.of("layering", "exclusive-lock", "object-map", "fast-diff", "deep-flatten");

    /**
     * How the backup being restored is laid out on the NAS.
     *
     * <p>A qcow2 backup is a single self-describing file per volume: incrementals were rebased onto
     * their parent, so following the backing chain from the leaf is enough. An {@code rbddiff}
     * backup is a chain of {@code rbd export-diff} streams with no link between them, so the whole
     * chain travels with the command and every diff is replayed in order.</p>
     *
     * <p>Held as a value object rather than wrapper state: command wrappers are shared singletons
     * and must stay stateless.</p>
     */
    private static final class BackupLayout {
        private final String format;
        private final List<String> chainDirs; // root-first; exactly one entry for qcow2

        private BackupLayout(RestoreBackupCommand command) {
            this.format = FORMAT_RBD_DIFF.equals(command.getBackupFormat()) ? FORMAT_RBD_DIFF : FORMAT_QCOW2;
            List<String> dirs = command.getBackupChainPaths();
            this.chainDirs = (dirs == null || dirs.isEmpty())
                    ? Collections.singletonList(command.getBackupPath())
                    : dirs;
        }

        private boolean isRbdDiff() {
            return FORMAT_RBD_DIFF.equals(format);
        }

        /**
         * Every on-NAS file that has to be read for one volume, oldest first. For qcow2 that is the
         * single leaf file; for rbddiff, the full anchor followed by each incremental.
         */
        private List<String> filesFor(String mountDirectory, String backupFile, String diskType) {
            String fileName = String.format("%s.%s.%s", diskType.toLowerCase(Locale.ROOT), backupFile, format);
            List<String> files = new ArrayList<>(chainDirs.size());
            for (String dir : chainDirs) {
                files.add(String.format(FILE_PATH_PLACEHOLDER, String.format(FILE_PATH_PLACEHOLDER, mountDirectory, dir), fileName));
            }
            return files;
        }
    }

    private String getVolumeUuidFromPath(String volumePath, PrimaryDataStoreTO volumePool) {
        if (Storage.StoragePoolType.Linstor.equals(volumePool.getPoolType())) {
            Path path = Paths.get(volumePath);
            String rscName = path.getParent().getFileName().toString();
            if (rscName.startsWith("cs-")) {
                rscName = rscName.substring(3);
            }
            return rscName;
        } else {
            int lastIndex = volumePath.lastIndexOf("/");
            return volumePath.substring(lastIndex + 1);
        }
    }

    @Override
    public Answer execute(RestoreBackupCommand command, LibvirtComputingResource serverResource) {
        String vmName = command.getVmName();
        String backupRepoAddress = command.getBackupRepoAddress();
        String backupRepoType = command.getBackupRepoType();
        String mountOptions = command.getMountOptions();
        Boolean vmExists = command.isVmExists();
        String diskType = command.getDiskType();
        List<String> backedVolumeUUIDs = command.getBackupVolumesUUIDs();
        List<PrimaryDataStoreTO> restoreVolumePools = command.getRestoreVolumePools();
        List<String> restoreVolumePaths = command.getRestoreVolumePaths();
        Integer mountTimeout = command.getMountTimeout() * 1000;
        int timeout = command.getWait() > 0 ? command.getWait() * 1000 : serverResource.getCmdsTimeout();
        KVMStoragePoolManager storagePoolMgr = serverResource.getStoragePoolMgr();
        List<String> backupFiles = command.getBackupFiles();
        BackupLayout layout = new BackupLayout(command);

        String newVolumeId = null;
        try {
            String mountDirectory = mountBackupDirectory(backupRepoAddress, backupRepoType, mountOptions, mountTimeout);
            if (Objects.isNull(vmExists)) {
                PrimaryDataStoreTO volumePool = restoreVolumePools.get(0);
                String volumePath = restoreVolumePaths.get(0);
                String backupFile = backupFiles.get(0);
                newVolumeId = getVolumeUuidFromPath(volumePath, volumePool);
                Long size = command.getRestoreVolumeSizes().get(0);
                restoreVolume(storagePoolMgr, volumePool, volumePath, diskType, backupFile, size,
                        new Pair<>(vmName, command.getVmState()), mountDirectory, layout, timeout, mountTimeout);
            } else if (Boolean.TRUE.equals(vmExists)) {
                restoreVolumesOfExistingVM(storagePoolMgr, restoreVolumePools, restoreVolumePaths, backedVolumeUUIDs, backupFiles, mountDirectory, layout, timeout, mountTimeout);
            } else {
                restoreVolumesOfDestroyedVMs(storagePoolMgr, restoreVolumePools, restoreVolumePaths, backupFiles, mountDirectory, layout, timeout, mountTimeout);
            }
        } catch (CloudRuntimeException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "";
            return new BackupAnswer(command, false, errorMessage);
        }

        return new BackupAnswer(command, true, newVolumeId);
    }

    /**
     * Check every file the volume will be rebuilt from. qcow2 leaves are additionally run through
     * {@code qemu-img check}; an {@code rbd export-diff} stream is not a disk image, so there the
     * check is existence only — a truncated or corrupt diff is caught by {@code rbd import-diff},
     * which refuses to apply it.
     */
    private void verifyBackupFiles(List<String> backupFiles, BackupLayout layout, String volUuid) {
        for (String file : backupFiles) {
            if (!checkBackupPathExists(file)) {
                throw new CloudRuntimeException(String.format("Backup file for the volume [%s] does not exist: %s", volUuid, file));
            }
        }
        if (layout.isRbdDiff()) {
            return;
        }
        String leaf = backupFiles.get(backupFiles.size() - 1);
        if (!checkBackupFileImage(leaf)) {
            throw new CloudRuntimeException(String.format("Backup qcow2 file for the volume [%s] is corrupt.", volUuid));
        }
    }

    private void restoreVolumesOfExistingVM(KVMStoragePoolManager storagePoolMgr, List<PrimaryDataStoreTO> restoreVolumePools,
                                            List<String> restoreVolumePaths, List<String> backedVolumesUUIDs,
                                            List<String> backupFiles, String mountDirectory, BackupLayout layout, int timeout, Integer mountTimeout) {
        String diskType = "root";
        try {
            for (int idx = 0; idx < restoreVolumePaths.size(); idx++) {
                PrimaryDataStoreTO restoreVolumePool = restoreVolumePools.get(idx);
                String restoreVolumePath = restoreVolumePaths.get(idx);
                String backupFile = backupFiles.get(idx);
                String backupVolumeUuid = backedVolumesUUIDs.get(idx);
                List<String> volumeBackupFiles = layout.filesFor(mountDirectory, backupFile, diskType);
                diskType = "datadisk";

                verifyBackupFiles(volumeBackupFiles, layout, backupVolumeUuid);
                if (!replaceVolumeWithBackup(storagePoolMgr, restoreVolumePool, restoreVolumePath, volumeBackupFiles, layout, timeout)) {
                    throw new CloudRuntimeException(String.format("Unable to restore contents from the backup volume [%s].", backupVolumeUuid));
                }
            }
        } finally {
            unmountBackupDirectory(mountDirectory, mountTimeout);
            deleteTemporaryDirectory(mountDirectory);
        }
    }

    private void restoreVolumesOfDestroyedVMs(KVMStoragePoolManager storagePoolMgr, List<PrimaryDataStoreTO> volumePools,
                                              List<String> volumePaths, List<String> backupFiles, String mountDirectory,
                                              BackupLayout layout, int timeout, Integer mountTimeout) {
        String diskType = "root";
        try {
            for (int i = 0; i < volumePaths.size(); i++) {
                PrimaryDataStoreTO volumePool = volumePools.get(i);
                String volumePath = volumePaths.get(i);
                String backupFile = backupFiles.get(i);
                List<String> volumeBackupFiles = layout.filesFor(mountDirectory, backupFile, diskType);
                String volumeUuid = getVolumeUuidFromPath(volumePath, volumePool);
                diskType = "datadisk";
                verifyBackupFiles(volumeBackupFiles, layout, volumeUuid);
                if (!replaceVolumeWithBackup(storagePoolMgr, volumePool, volumePath, volumeBackupFiles, layout, timeout)) {
                    throw new CloudRuntimeException(String.format("Unable to restore contents from the backup volume [%s].", volumeUuid));
                }
            }
        } finally {
            unmountBackupDirectory(mountDirectory, mountTimeout);
            deleteTemporaryDirectory(mountDirectory);
        }
    }

    private void restoreVolume(KVMStoragePoolManager storagePoolMgr, PrimaryDataStoreTO volumePool, String volumePath, String diskType, String backupFile,
                               Long size, Pair<String, VirtualMachine.State> vmNameAndState, String mountDirectory, BackupLayout layout, int timeout, Integer mountTimeout) {
        List<String> volumeBackupFiles;
        String volumeUuid;
        try {
            volumeBackupFiles = layout.filesFor(mountDirectory, backupFile, diskType);
            volumeUuid = getVolumeUuidFromPath(volumePath, volumePool);
            verifyBackupFiles(volumeBackupFiles, layout, volumeUuid);
            if (!replaceVolumeWithBackup(storagePoolMgr, volumePool, volumePath, volumeBackupFiles, layout, timeout, true, size)) {
                throw new CloudRuntimeException(String.format("Unable to restore contents from the backup volume [%s].", volumeUuid));

            }
            if (VirtualMachine.State.Running.equals(vmNameAndState.second())) {
                if (!attachVolumeToVm(storagePoolMgr, vmNameAndState.first(), volumePool, volumePath)) {
                    throw new CloudRuntimeException(String.format("Failed to attach volume to VM: %s", vmNameAndState.first()));
                }
            }
        } finally {
            unmountBackupDirectory(mountDirectory, mountTimeout);
            deleteTemporaryDirectory(mountDirectory);
        }
    }


    private String mountBackupDirectory(String backupRepoAddress, String backupRepoType, String mountOptions, Integer mountTimeout) {
        String randomChars = RandomStringUtils.random(5, true, false);
        String mountDirectory = String.format("%s.%s",BACKUP_TEMP_FILE_PREFIX , randomChars);

        try {
            mountDirectory = Files.createTempDirectory(mountDirectory).toString();
        } catch (IOException e) {
            logger.error("Failed to create the tmp mount directory {} for restore", mountDirectory, e);
            throw new CloudRuntimeException("Failed to create the tmp mount directory for restore on the KVM host");
        }
        int exitValue;
        try {
            String mountPath = Script.getExecutableAbsolutePath("mount");
            List<String> mountCmd = new ArrayList<>();
            mountCmd.add("sudo");
            mountCmd.add(mountPath);
            mountCmd.add("-t");
            mountCmd.add(backupRepoType);
            mountCmd.add(backupRepoAddress);
            mountCmd.add(mountDirectory);
            if ("cifs".equals(backupRepoType)) {
                if (StringUtils.isBlank(mountOptions)) {
                    mountOptions = "nobrl";
                } else {
                    mountOptions += ",nobrl";
                }
            }
            if (StringUtils.isNotBlank(mountOptions)) {
                mountCmd.add("-o");
                mountCmd.add(mountOptions);
            }
            exitValue = Script.executeCommandForExitValue(mountTimeout, mountCmd.toArray(new String[0]));
        } catch (Exception e) {
            logger.error("Failed to mount repository {} of type {} to the directory {}", backupRepoAddress, backupRepoType, mountDirectory, e);
            throw new CloudRuntimeException("Failed to mount the backup repository on the KVM host");
        }
        if (exitValue != 0) {
            logger.error("Failed to mount repository {} of type {} to the directory {}, mount exited with {}", backupRepoAddress,
                    backupRepoType, mountDirectory, exitValue);
            removeTemporaryDirectoryQuietly(mountDirectory);
            throw new CloudRuntimeException("Failed to mount the backup repository on the KVM host");
        }
        return mountDirectory;
    }

    private void unmountBackupDirectory(String backupDirectory, Integer mountTimeout) {
        int exitValue;
        try {
            String umountPath = Script.getExecutableAbsolutePath("umount");
            String[] umountCmd = new String[] { "sudo", umountPath, backupDirectory };
            exitValue = Script.executeCommandForExitValue(mountTimeout, umountCmd);
        } catch (Exception e) {
            logger.error("Failed to unmount backup directory {}", backupDirectory, e);
            throw new CloudRuntimeException("Failed to unmount the backup directory");
        }
        if (exitValue != 0) {
            logger.error("Failed to unmount backup directory {}, umount exited with {}", backupDirectory, exitValue);
            throw new CloudRuntimeException("Failed to unmount the backup directory");
        }
    }

    private void removeTemporaryDirectoryQuietly(String backupDirectory) {
        try {
            Files.deleteIfExists(Paths.get(backupDirectory));
        } catch (IOException e) {
            logger.warn("Failed to remove the temporary mount directory {} after the mount failed.", backupDirectory, e);
        }
    }

    private void deleteTemporaryDirectory(String backupDirectory) {
        try {
            Files.deleteIfExists(Paths.get(backupDirectory));
        } catch (IOException e) {
            logger.error("Failed to delete backup directory: {}}", backupDirectory, e);
            throw new CloudRuntimeException("Failed to delete the backup directory");
        }
    }

    private boolean checkBackupFileImage(String backupPath) {
        int exitValue = Script.runSimpleBashScriptForExitValue(String.format("qemu-img check %s", backupPath));
        return exitValue == 0;
    }

    private boolean checkBackupPathExists(String backupPath) {
        int exitValue = Script.runSimpleBashScriptForExitValue(String.format("ls %s", backupPath));
        return exitValue == 0;
    }

    private boolean replaceVolumeWithBackup(KVMStoragePoolManager storagePoolMgr, PrimaryDataStoreTO volumePool, String volumePath,
                                            List<String> backupFiles, BackupLayout layout, int timeout) {
        return replaceVolumeWithBackup(storagePoolMgr, volumePool, volumePath, backupFiles, layout, timeout, false, null);
    }

    private boolean replaceVolumeWithBackup(KVMStoragePoolManager storagePoolMgr, PrimaryDataStoreTO volumePool, String volumePath,
                                            List<String> backupFiles, BackupLayout layout, int timeout, boolean createTargetVolume, Long size) {
        if (layout.isRbdDiff()) {
            return restoreRbdImageFromDiffChain(storagePoolMgr, volumePool, volumePath, backupFiles, size);
        }

        String backupPath = backupFiles.get(backupFiles.size() - 1);
        if (List.of(Storage.StoragePoolType.RBD, Storage.StoragePoolType.Linstor).contains(volumePool.getPoolType())) {
            return replaceBlockDeviceWithBackup(storagePoolMgr, volumePool, volumePath, backupPath, timeout, createTargetVolume, size);
        }

        // For NAS-backed incremental backups, the source qcow2 has a backing-file
        // reference to its parent (set by nasbackup.sh's qemu-img rebase). A plain
        // rsync would copy only the differential blocks, leaving a volume that
        // depends on a backing file the primary storage doesn't have. Flatten the
        // chain via qemu-img convert, which follows the backing-file links and
        // produces a single self-contained qcow2.
        if (hasBackingChain(backupPath)) {
            String[] qemuImgCmd = new String[] { Script.getExecutableAbsolutePath("qemu-img"), "convert", "-O", "qcow2", backupPath, volumePath };
            int flattenExit = Script.executeCommandForExitValue(qemuImgCmd);
            return flattenExit == 0;
        }

        String[] rsyncCmd = new String[] { Script.getExecutableAbsolutePath("rsync"), "-az", backupPath, volumePath };
        int exitValue = Script.executeCommandForExitValue(timeout, rsyncCmd);
        return exitValue == 0;
    }

    private boolean hasBackingChain(String qcow2Path) {
        return Script.runSimpleBashScriptForExitValue(
                String.format(QEMU_IMG_HAS_BACKING_COMMAND, qcow2Path)) == 0;
    }

    private boolean replaceBlockDeviceWithBackup(KVMStoragePoolManager storagePoolMgr, PrimaryDataStoreTO volumePool, String volumePath, String backupPath, int timeout, boolean createTargetVolume, Long size) {
        KVMStoragePool volumeStoragePool = storagePoolMgr.getStoragePool(volumePool.getPoolType(), volumePool.getUuid());
        QemuImg qemu;
        try {
            qemu = new QemuImg(timeout, true, false);
            String volumeUuid = getVolumeUuidFromPath(volumePath, volumePool);
            KVMPhysicalDisk disk = null;
            if (createTargetVolume) {
                if (Storage.StoragePoolType.Linstor.equals(volumePool.getPoolType())) {
                    if (size == null) {
                        throw new CloudRuntimeException("Restore volume size is required for Linstor pool when creating target volume");
                    }
                    disk = volumeStoragePool.createPhysicalDisk(volumeUuid, QemuImg.PhysicalDiskFormat.RAW, Storage.ProvisioningType.THIN, size, null);
                }
            } else {
                if (Storage.StoragePoolType.Linstor.equals(volumePool.getPoolType())) {
                    storagePoolMgr.connectPhysicalDisk(volumePool.getPoolType(), volumePool.getUuid(), volumeUuid, null);
                } else {
                    disk = volumeStoragePool.getPhysicalDisk(volumePath);
                }
                qemu.setSkipTargetVolumeCreation(true);
            }
            if (disk != null) {
                logger.debug("Restoring volume: {}", disk.toString());
            }
        } catch (LibvirtException ex) {
            throw new CloudRuntimeException(String.format("Failed to create qemu-img command to restore %s volume with backup", volumePool.getPoolType()), ex);
        }

        QemuImgFile srcBackupFile = null;
        QemuImgFile destVolumeFile = null;
        try {
            srcBackupFile = new QemuImgFile(backupPath, QemuImg.PhysicalDiskFormat.QCOW2);
            String destVolume;
            switch(volumePool.getPoolType()) {
                case Linstor:
                    destVolume = volumePath;
                    break;
                case RBD:
                    destVolume = KVMPhysicalDisk.RBDStringBuilder(volumeStoragePool, volumePath);
                    break;
                default:
                    throw new CloudRuntimeException(String.format("Unsupported storage pool type [%s] for block device restore with backup.", volumePool.getPoolType()));
            }
            destVolumeFile = new QemuImgFile(destVolume, QemuImg.PhysicalDiskFormat.RAW);
            logger.debug("Starting convert backup  {} to volume  {}", backupPath, volumePath);
            qemu.convert(srcBackupFile, destVolumeFile);
            logger.debug("Successfully converted backup {} to volume  {}", backupPath, volumePath);
        } catch (QemuImgException | LibvirtException e) {
            String srcFilename = srcBackupFile != null ? srcBackupFile.getFileName() : null;
            String destFilename = destVolumeFile != null ? destVolumeFile.getFileName() : null;
            logger.error("Failed to convert backup {} to volume {}, the error was: {}", srcFilename, destFilename, e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Rebuild an RBD image by replaying a chain of {@code rbd export-diff} streams into it: the
     * chain's full anchor first, then each incremental in the order it was taken.
     *
     * <p>RBD images are raw and cannot hold a backing-file reference, so — unlike the qcow2 path —
     * a diff carries no pointer to the one before it and the chain cannot be flattened by
     * {@code qemu-img}. Ceph applies the deltas natively instead: each diff ends at the snapshot
     * the backup was taken from, {@code import-diff} recreates that snapshot on the target, and the
     * next diff in the chain is validated against it. A truncated or out-of-order diff is therefore
     * rejected by Ceph rather than silently producing a half-restored image.</p>
     *
     * <p>The image is recreated first. A full diff only carries the extents that were allocated
     * when it was taken, so any block still allocated in the current image would otherwise show
     * through the restore as stale data. Recreating the image also drops any RBD snapshots it
     * carried, including CloudStack volume snapshots kept on primary storage — a restore rewinds
     * the volume wholesale, and there is no way to guarantee the first requirement while keeping
     * snapshots of content that is being replaced.</p>
     */
    private boolean restoreRbdImageFromDiffChain(KVMStoragePoolManager storagePoolMgr, PrimaryDataStoreTO volumePool,
                                                 String volumePath, List<String> diffFiles, Long size) {
        if (!Storage.StoragePoolType.RBD.equals(volumePool.getPoolType())) {
            logger.error("Backup [{}] holds RBD diffs but volume [{}] is on {} storage; it cannot be restored there.",
                    diffFiles.get(0), volumePath, volumePool.getPoolType());
            return false;
        }
        KVMStoragePool pool = storagePoolMgr.getStoragePool(volumePool.getPoolType(), volumePool.getUuid());
        Path keyFile = null;
        try {
            keyFile = writeCephKeyFile(pool.getAuthSecret());
            List<String> connection = rbdConnectionArgs(pool, keyFile);
            // Volume paths of RBD volumes are already "<ceph pool>/<image>".
            String image = volumePath;

            // Prefer the current image's own size: it is what the VM's disk was sized to. Only a
            // restore that creates a brand new volume has no image to read it from.
            Long imageSize = rbdImageSize(connection, image);
            if (imageSize == null) {
                imageSize = size;
            }
            if (imageSize == null || imageSize <= 0) {
                logger.error("Unable to determine the size to recreate RBD image {} with", image);
                return false;
            }

            if (!recreateRbdImage(connection, image, imageSize, rbdDataPool(pool))) {
                return false;
            }

            for (String diff : diffFiles) {
                logger.debug("Applying backup diff {} to RBD image {}", diff, image);
                if (Script.executeCommandForExitValue(rbdCommand(connection, "import-diff", diff, image)) != 0) {
                    logger.error("Failed to apply backup diff {} to RBD image {}", diff, image);
                    return false;
                }
            }

            // The snapshots import-diff recreated only existed to chain the diffs together; the
            // restored volume should look like any other, so drop them.
            if (Script.executeCommandForExitValue(rbdCommand(connection, "snap", "purge", image)) != 0) {
                logger.warn("Failed to purge the snapshots left behind by the restore of RBD image {}", image);
            }
            logger.debug("Successfully restored RBD image {} from {} backup diff(s)", image, diffFiles.size());
            return true;
        } catch (IOException e) {
            logger.error("Failed to prepare the Ceph credentials needed to restore volume [{}]", volumePath, e);
            return false;
        } finally {
            deleteCephKeyFile(keyFile);
        }
    }

    /**
     * Hand the cephx secret to the {@code rbd} CLI through a 0600 temporary file rather than an
     * argument, so it never shows up in the host's process list.
     */
    private Path writeCephKeyFile(String authSecret) throws IOException {
        if (StringUtils.isBlank(authSecret)) {
            return null;
        }
        Path keyFile = Files.createTempFile("csrestore-ceph-", ".key",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        Files.write(keyFile, authSecret.getBytes(StandardCharsets.UTF_8));
        return keyFile;
    }

    private void deleteCephKeyFile(Path keyFile) {
        if (keyFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(keyFile);
        } catch (IOException e) {
            logger.warn("Failed to delete the temporary Ceph key file [{}].", keyFile, e);
        }
    }

    private List<String> rbdConnectionArgs(KVMStoragePool pool, Path keyFile) {
        List<String> args = new ArrayList<>();
        if (StringUtils.isNotBlank(pool.getSourceHost())) {
            args.add("-m");
            args.add(RbdUtils.getRbdMonitors(pool.getSourceHost(), pool.getSourcePort()));
        }
        if (StringUtils.isNotBlank(pool.getAuthUserName())) {
            args.add("--id");
            args.add(pool.getAuthUserName());
            if (keyFile != null) {
                args.add("--keyfile");
                args.add(keyFile.toString());
            }
        }
        return args;
    }

    private String[] rbdCommand(List<String> connection, String... arguments) {
        List<String> command = new ArrayList<>();
        command.add(Script.getExecutableAbsolutePath("rbd"));
        command.addAll(connection);
        command.addAll(Arrays.asList(arguments));
        return command.toArray(new String[0]);
    }

    /** Size of {@code image} in bytes, or {@code null} when the image does not exist. */
    private Long rbdImageSize(List<String> connection, String image) {
        Pair<Integer, String> result = Script.executePipedCommands(
                Collections.singletonList(rbdCommand(connection, "info", image, "--format", "json")), 0);
        if (result.first() == null || result.first() != 0 || result.second() == null) {
            return null;
        }
        Matcher matcher = RBD_INFO_SIZE.matcher(result.second());
        return matcher.find() ? Long.valueOf(matcher.group(1)) : null;
    }

    private boolean recreateRbdImage(List<String> connection, String image, long sizeBytes, String dataPool) {
        // `rbd rm` refuses to delete an image that still has snapshots, and a restore target
        // normally carries the anchor snapshot of the backup chain it belongs to.
        Script.executeCommandForExitValue(rbdCommand(connection, "snap", "purge", image));
        if (Script.executeCommandForExitValue(rbdCommand(connection, "rm", image)) != 0
                && rbdImageSize(connection, image) != null) {
            logger.error("Failed to remove RBD image {} before restoring it", image);
            return false;
        }
        List<String> create = new ArrayList<>(Arrays.asList("create", image, "--size", sizeBytes + "B"));
        for (String feature : RBD_IMAGE_FEATURES) {
            create.add("--image-feature");
            create.add(feature);
        }
        if (StringUtils.isNotBlank(dataPool)) {
            create.add("--data-pool");
            create.add(dataPool);
        }
        if (Script.executeCommandForExitValue(rbdCommand(connection, create.toArray(new String[0]))) != 0) {
            logger.error("Failed to recreate RBD image {} with size {} bytes", image, sizeBytes);
            return false;
        }
        return true;
    }

    private String rbdDataPool(KVMStoragePool pool) {
        Map<String, String> details = pool.getDetails();
        return details == null ? null : details.get(KVMPhysicalDisk.RBD_DEFAULT_DATA_POOL);
    }

    private boolean attachVolumeToVm(KVMStoragePoolManager storagePoolMgr, String vmName, PrimaryDataStoreTO volumePool, String volumePath) {
        String deviceToAttachDiskTo = getDeviceToAttachDisk(vmName);
        if (Storage.StoragePoolType.RBD.equals(volumePool.getPoolType())) {
            return attachRbdVolumeToVm(storagePoolMgr, vmName, volumePool, volumePath, deviceToAttachDiskTo);
        }
        List<String> virshCmd = new ArrayList<>();
        virshCmd.add(Script.getExecutableAbsolutePath("virsh"));
        virshCmd.add("attach-disk");
        virshCmd.add(vmName);
        virshCmd.add(volumePath);
        virshCmd.add(deviceToAttachDiskTo);
        virshCmd.add("--driver");
        virshCmd.add("qemu");
        if (!Storage.StoragePoolType.Linstor.equals(volumePool.getPoolType())) {
            virshCmd.add("--subdriver");
            virshCmd.add("qcow2");
        }
        virshCmd.add("--cache");
        virshCmd.add("none");
        int exitValue = Script.executeCommandForExitValue(virshCmd.toArray(new String[0]));
        return exitValue == 0;
    }

    private boolean attachRbdVolumeToVm(KVMStoragePoolManager storagePoolMgr, String vmName, PrimaryDataStoreTO volumePool, String volumePath,
            String deviceToAttachDiskTo) {
        String xmlForRbdDisk = getXmlForRbdDisk(storagePoolMgr, volumePool, volumePath, deviceToAttachDiskTo);
        logger.debug("RBD disk xml to attach: {}", xmlForRbdDisk);
        // The command is executed without a shell, so the XML cannot be piped in through a
        // here-document. Write it to a temporary file and pass virsh the path instead.
        Path xmlFile = null;
        try {
            xmlFile = Files.createTempFile("csrestore-rbd-", ".xml");
            Files.write(xmlFile, xmlForRbdDisk.getBytes(StandardCharsets.UTF_8));
            String[] virshCmd = new String[] { Script.getExecutableAbsolutePath("virsh"), "attach-device", vmName, xmlFile.toString() };
            return Script.executeCommandForExitValue(virshCmd) == 0;
        } catch (IOException e) {
            logger.error("Failed to write the RBD disk XML used to attach volume [{}] to VM [{}]", volumePath, vmName, e);
            return false;
        } finally {
            if (xmlFile != null) {
                try {
                    Files.deleteIfExists(xmlFile);
                } catch (IOException e) {
                    logger.warn("Failed to delete the temporary RBD disk XML file [{}].", xmlFile, e);
                }
            }
        }
    }

    private String getDeviceToAttachDisk(String vmName) {
        String[] domblkCmd = new String[] { Script.getExecutableAbsolutePath("virsh"), "domblklist", "--domain", vmName };
        String[] tailCmd = new String[] { Script.getExecutableAbsolutePath("tail"), "-n", "3" };
        String[] headCmd = new String[] { Script.getExecutableAbsolutePath("head"), "-n", "1" };
        // The commands are executed without a shell, so the awk program must be passed as a plain
        // argument. Keeping the quotes a shell would have stripped makes awk fail with
        // "invalid char" and produce no output.
        String[] awkCmd = new String[] { Script.getExecutableAbsolutePath("awk"), "{print $1}" };
        Pair<Integer, String> result = Script.executePipedCommands(Arrays.asList(domblkCmd, tailCmd, headCmd, awkCmd), 0);
        // executePipedCommands appends a line separator to every line it reads, so the device
        // name has to be trimmed before the last character can be incremented.
        String currentDevice = result.second() == null ? "" : result.second().trim();
        if (result.first() == null || result.first() != 0 || StringUtils.isBlank(currentDevice)) {
            throw new CloudRuntimeException(String.format("Failed to determine the device to attach the restored volume to on VM [%s].", vmName));
        }
        char lastChar = currentDevice.charAt(currentDevice.length() - 1);
        char incrementedChar = (char) (lastChar + 1);
        return currentDevice.substring(0, currentDevice.length() - 1) + incrementedChar;
    }

    private String getXmlForRbdDisk(KVMStoragePoolManager storagePoolMgr, PrimaryDataStoreTO volumePool, String volumePath, String deviceToAttachDiskTo) {
        StringBuilder diskBuilder = new StringBuilder();
        diskBuilder.append("\n<disk ");
        diskBuilder.append(" device='disk'");
        diskBuilder.append(" type='network'");
        diskBuilder.append(">\n");

        diskBuilder.append("<source ");
        diskBuilder.append(" protocol='rbd'");
        diskBuilder.append(" name='" + volumePath + "'");
        diskBuilder.append(">\n");
        for (String sourceHost : volumePool.getHost().split(",")) {
            diskBuilder.append("<host name='");
            diskBuilder.append(sourceHost.replace("[", "").replace("]", ""));
            if (volumePool.getPort() != 0) {
                diskBuilder.append("' port='");
                diskBuilder.append(volumePool.getPort());
            }
            diskBuilder.append("'/>\n");
        }
        diskBuilder.append("</source>\n");
        String authUserName = null;
        final KVMStoragePool primaryPool = storagePoolMgr.getStoragePool(volumePool.getPoolType(), volumePool.getUuid());
        if (primaryPool != null) {
            authUserName = primaryPool.getAuthUserName();
        }
        if (StringUtils.isNotBlank(authUserName)) {
            diskBuilder.append("<auth username='" + authUserName + "'>\n");
            diskBuilder.append("<secret type='ceph' uuid='" + volumePool.getUuid() + "'/>\n");
            diskBuilder.append("</auth>\n");
        }
        diskBuilder.append("<target dev='" + deviceToAttachDiskTo + "'");
        diskBuilder.append(" bus='virtio'");
        diskBuilder.append("/>\n");
        diskBuilder.append("</disk>\n");
        return diskBuilder.toString();
    }
}
