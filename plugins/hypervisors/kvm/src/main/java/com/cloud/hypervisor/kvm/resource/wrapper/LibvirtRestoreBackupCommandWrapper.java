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
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.backup.BackupAnswer;
import org.apache.cloudstack.backup.RestoreBackupCommand;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@ResourceWrapper(handles = RestoreBackupCommand.class)
public class LibvirtRestoreBackupCommandWrapper extends CommandWrapper<RestoreBackupCommand, Answer, LibvirtComputingResource> {
    private static final String BACKUP_TEMP_FILE_PREFIX = "csbackup";
    private static final String MOUNT_COMMAND = "sudo mount -t %s %s %s";
    private static final String UMOUNT_COMMAND = "sudo umount %s";
    private static final String FILE_PATH_PLACEHOLDER = "%s/%s";
    private static final String ATTACH_DISK_COMMAND = " virsh attach-disk %s %s %s --driver qemu --subdriver qcow2 --cache none";
    private static final String CURRRENT_DEVICE = "virsh domblklist --domain %s | tail -n 3 | head -n 1 | awk '{print $1}'";
    private static final String RSYNC_COMMAND = "rsync -az %s %s";

    @Override
    public Answer execute(RestoreBackupCommand command, LibvirtComputingResource serverResource) {
        String vmName = command.getVmName();
        String backupPath = command.getBackupPath();
        String backupRepoAddress = command.getBackupRepoAddress();
        String backupRepoType = command.getBackupRepoType();
        String mountOptions = command.getMountOptions();
        Boolean vmExists = command.isVmExists();
        String diskType = command.getDiskType();
        List<String> backedVolumeUUIDs = command.getBackupVolumesUUIDs();
        List<String> restoreVolumePaths = command.getRestoreVolumePaths();
        String restoreVolumeUuid = command.getRestoreVolumeUUID();
        Integer mountTimeout = command.getMountTimeout() * 1000;

        String newVolumeId = null;
        try {
            String mountDirectory = mountBackupDirectory(backupRepoAddress, backupRepoType, mountOptions, mountTimeout);
            if (Objects.isNull(vmExists)) {
                String volumePath = restoreVolumePaths.get(0);
                int lastIndex = volumePath.lastIndexOf("/");
                newVolumeId = volumePath.substring(lastIndex + 1);
                restoreVolume(backupPath, volumePath, diskType, restoreVolumeUuid,
                        new Pair<>(vmName, command.getVmState()), mountDirectory);
            } else if (Boolean.TRUE.equals(vmExists)) {
                restoreVolumesOfExistingVM(restoreVolumePaths, backedVolumeUUIDs, backupPath, mountDirectory);
            } else {
                restoreVolumesOfDestroyedVMs(restoreVolumePaths, vmName, backupPath, mountDirectory);
            }
        } catch (CloudRuntimeException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "";
            return new BackupAnswer(command, false, errorMessage);
        }

        return new BackupAnswer(command, true, newVolumeId);
    }

    private void verifyBackupFile(String backupPath, String volUuid) {
        if (!checkBackupPathExists(backupPath)) {
            throw new CloudRuntimeException(String.format("Backup file for the volume [%s] does not exist.", volUuid));
        }
        if (!checkBackupFileImage(backupPath)) {
            throw new CloudRuntimeException(String.format("Backup qcow2 file for the volume [%s] is corrupt.", volUuid));
        }
    }

    private void restoreVolumesOfExistingVM(List<String> restoreVolumePaths, List<String> backedVolumesUUIDs,
                                            String backupPath, String mountDirectory) {
        String diskType = "root";
        try {
            for (int idx = 0; idx < restoreVolumePaths.size(); idx++) {
                String restoreVolumePath = restoreVolumePaths.get(idx);
                String backupVolumeUuid = backedVolumesUUIDs.get(idx);
                Pair<String, String> bkpPathAndVolUuid = getBackupPath(mountDirectory, null, backupPath, diskType, backupVolumeUuid);
                diskType = "datadisk";
                verifyBackupFile(bkpPathAndVolUuid.first(), bkpPathAndVolUuid.second());
                if (!replaceVolumeWithBackup(restoreVolumePath, bkpPathAndVolUuid.first())) {
                    throw new CloudRuntimeException(String.format("Unable to restore contents from the backup volume [%s].", bkpPathAndVolUuid.second()));
                }
            }
        } finally {
            unmountBackupDirectory(mountDirectory);
            deleteTemporaryDirectory(mountDirectory);
        }
    }

    private void restoreVolumesOfDestroyedVMs(List<String> volumePaths, String vmName, String backupPath, String mountDirectory) {
        String diskType = "root";
        try {
            for (int i = 0; i < volumePaths.size(); i++) {
                String volumePath = volumePaths.get(i);
                Pair<String, String> bkpPathAndVolUuid = getBackupPath(mountDirectory, volumePath, backupPath, diskType, null);
                diskType = "datadisk";
                verifyBackupFile(bkpPathAndVolUuid.first(), bkpPathAndVolUuid.second());
                if (!replaceVolumeWithBackup(volumePath, bkpPathAndVolUuid.first())) {
                    throw new CloudRuntimeException(String.format("Unable to restore contents from the backup volume [%s].", bkpPathAndVolUuid.second()));
                }
            }
        } finally {
            unmountBackupDirectory(mountDirectory);
            deleteTemporaryDirectory(mountDirectory);
        }
    }

    private void restoreVolume(String backupPath, String volumePath, String diskType, String volumeUUID,
                               Pair<String, VirtualMachine.State> vmNameAndState, String mountDirectory) {
        Pair<String, String> bkpPathAndVolUuid;
        try {
            bkpPathAndVolUuid = getBackupPath(mountDirectory, volumePath, backupPath, diskType, volumeUUID);
            verifyBackupFile(bkpPathAndVolUuid.first(), bkpPathAndVolUuid.second());
            if (!replaceVolumeWithBackup(volumePath, bkpPathAndVolUuid.first())) {
                throw new CloudRuntimeException(String.format("Unable to restore contents from the backup volume [%s].", bkpPathAndVolUuid.second()));
            }
            if (VirtualMachine.State.Running.equals(vmNameAndState.second())) {
                if (!attachVolumeToVm(vmNameAndState.first(), volumePath)) {
                    throw new CloudRuntimeException(String.format("Failed to attach volume to VM: %s", vmNameAndState.first()));
                }
            }
        } finally {
            unmountBackupDirectory(mountDirectory);
            deleteTemporaryDirectory(mountDirectory);
        }
    }


    private String mountBackupDirectory(String backupRepoAddress, String backupRepoType, String mountOptions, Integer mountTimeout) {
        String randomChars = RandomStringUtils.random(5, true, false);
        String mountDirectory = String.format("%s.%s",BACKUP_TEMP_FILE_PREFIX , randomChars);

        try {
            mountDirectory = Files.createTempDirectory(mountDirectory).toString();
        } catch (IOException e) {
            logger.error(String.format("Failed to create the tmp mount directory {} for restore", mountDirectory), e);
            throw new CloudRuntimeException("Failed to create the tmp mount directory for restore on the KVM host");
        }

        String mount = String.format(MOUNT_COMMAND, backupRepoType, backupRepoAddress, mountDirectory);
        if ("cifs".equals(backupRepoType)) {
            if (Objects.isNull(mountOptions) || mountOptions.trim().isEmpty()) {
                mountOptions = "nobrl";
            } else {
                mountOptions += ",nobrl";
            }
        }
        if (Objects.nonNull(mountOptions) && !mountOptions.trim().isEmpty()) {
            mount += " -o " + mountOptions;
        }

        int exitValue = Script.runSimpleBashScriptForExitValue(mount, mountTimeout, false);
        if (exitValue != 0) {
            logger.error(String.format("Failed to mount repository {} of type {} to the directory {}", backupRepoAddress, backupRepoType, mountDirectory));
            throw new CloudRuntimeException("Failed to mount the backup repository on the KVM host");
        }
        return mountDirectory;
    }

    private void unmountBackupDirectory(String backupDirectory) {
        String umountCmd = String.format(UMOUNT_COMMAND, backupDirectory);
        int exitValue = Script.runSimpleBashScriptForExitValue(umountCmd);
        if (exitValue != 0) {
            logger.error(String.format("Failed to unmount backup directory {}", backupDirectory));
            throw new CloudRuntimeException("Failed to unmount the backup directory");
        }
    }

    private void deleteTemporaryDirectory(String backupDirectory) {
        try {
            Files.deleteIfExists(Paths.get(backupDirectory));
        } catch (IOException e) {
            logger.error(String.format("Failed to delete backup directory: %s", backupDirectory), e);
            throw new CloudRuntimeException("Failed to delete the backup directory");
        }
    }

    private Pair<String, String> getBackupPath(String mountDirectory, String volumePath, String backupPath, String diskType, String volumeUuid) {
        String bkpPath = String.format(FILE_PATH_PLACEHOLDER, mountDirectory, backupPath);
        String volUuid = Objects.isNull(volumeUuid) ? volumePath.substring(volumePath.lastIndexOf(File.separator) + 1) : volumeUuid;
        String backupFileName = String.format("%s.%s.qcow2", diskType.toLowerCase(Locale.ROOT), volUuid);
        bkpPath = String.format(FILE_PATH_PLACEHOLDER, bkpPath, backupFileName);
        return new Pair<>(bkpPath, volUuid);
    }

    private boolean checkBackupFileImage(String backupPath) {
        int exitValue = Script.runSimpleBashScriptForExitValue(String.format("qemu-img check %s", backupPath));
        return exitValue == 0;
    }

    private boolean checkBackupPathExists(String backupPath) {
        int exitValue = Script.runSimpleBashScriptForExitValue(String.format("ls %s", backupPath));
        return exitValue == 0;
    }

    private boolean replaceVolumeWithBackup(String volumePath, String backupPath) {
        int exitValue = Script.runSimpleBashScriptForExitValue(String.format(RSYNC_COMMAND, backupPath, volumePath));
        return exitValue == 0;
    }

    private boolean attachVolumeToVm(String vmName, String volumePath) {
        String deviceToAttachDiskTo = getDeviceToAttachDisk(vmName);
        int exitValue = Script.runSimpleBashScriptForExitValue(String.format(ATTACH_DISK_COMMAND, vmName, volumePath, deviceToAttachDiskTo));
        return exitValue == 0;
    }

    private String getDeviceToAttachDisk(String vmName) {
        String currentDevice = Script.runSimpleBashScript(String.format(CURRRENT_DEVICE, vmName));
        char lastChar = currentDevice.charAt(currentDevice.length() - 1);
        char incrementedChar = (char) (lastChar + 1);
        return currentDevice.substring(0, currentDevice.length() - 1) + incrementedChar;
    }
}
