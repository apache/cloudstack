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
    private static final String ATTACH_DISK_COMMAND = " virsh attach-disk %s %s %s --cache none";
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
        List<String> volumePaths = command.getVolumePaths();
        String restoreVolumeUuid = command.getRestoreVolumeUUID();

        String newVolumeId = null;
        if (Objects.isNull(vmExists)) {
            String volumePath = volumePaths.get(0);
            int lastIndex = volumePath.lastIndexOf("/");
            newVolumeId = volumePath.substring(lastIndex + 1);
            restoreVolume(backupPath, backupRepoType, backupRepoAddress, volumePath, diskType, restoreVolumeUuid,
                    new Pair<>(vmName, command.getVmState()));
        } else if (Boolean.TRUE.equals(vmExists)) {
            restoreVolumesOfExistingVM(volumePaths, backupPath, backupRepoType, backupRepoAddress, mountOptions);
        } else {
            restoreVolumesOfDestroyedVMs(volumePaths, vmName, backupPath, backupRepoType, backupRepoAddress, mountOptions);
        }

        return new BackupAnswer(command, true, newVolumeId);
    }

    private void restoreVolumesOfExistingVM(List<String> volumePaths, String backupPath,
                                             String backupRepoType, String backupRepoAddress, String mountOptions) {
        String diskType = "root";
        String mountDirectory = mountBackupDirectory(backupRepoAddress, backupRepoType);
        try {
            for (int idx = 0; idx < volumePaths.size(); idx++) {
                String volumePath = volumePaths.get(idx);
                Pair<String, String> bkpPathAndVolUuid = getBackupPath(mountDirectory, volumePath, backupPath, diskType, null);
                diskType = "datadisk";
                try {
                    replaceVolumeWithBackup(volumePath, bkpPathAndVolUuid.first());
                } catch (IOException e) {
                    throw new CloudRuntimeException(String.format("Unable to revert backup for volume [%s] due to [%s].", bkpPathAndVolUuid.second(), e.getMessage()), e);
                }
            }
        } finally {
            unmountBackupDirectory(mountDirectory);
            deleteTemporaryDirectory(mountDirectory);
        }

    }

    private void restoreVolumesOfDestroyedVMs(List<String> volumePaths, String vmName, String backupPath,
                                              String backupRepoType, String backupRepoAddress, String mountOptions) {
        String mountDirectory = mountBackupDirectory(backupRepoAddress, backupRepoType);
        String diskType = "root";
        try {
            for (int i = 0; i < volumePaths.size(); i++) {
                String volumePath = volumePaths.get(i);
                Pair<String, String> bkpPathAndVolUuid = getBackupPath(mountDirectory, volumePath, backupPath, diskType, null);
                diskType = "datadisk";
                try {
                    replaceVolumeWithBackup(volumePath, bkpPathAndVolUuid.first());
                } catch (IOException e) {
                    throw new CloudRuntimeException(String.format("Unable to revert backup for volume [%s] due to [%s].", bkpPathAndVolUuid.second(), e.getMessage()), e);
                }
            }
        } finally {
            unmountBackupDirectory(mountDirectory);
            deleteTemporaryDirectory(mountDirectory);
        }
    }

    private void restoreVolume(String backupPath, String backupRepoType, String backupRepoAddress, String volumePath,
                               String diskType, String volumeUUID, Pair<String, VirtualMachine.State> vmNameAndState) {
        String mountDirectory = mountBackupDirectory(backupRepoAddress, backupRepoType);
        Pair<String, String> bkpPathAndVolUuid;
        try {
            bkpPathAndVolUuid = getBackupPath(mountDirectory, volumePath, backupPath, diskType, volumeUUID);
            try {
                replaceVolumeWithBackup(volumePath, bkpPathAndVolUuid.first());
                if (VirtualMachine.State.Running.equals(vmNameAndState.second())) {
                    if (!attachVolumeToVm(vmNameAndState.first(), volumePath)) {
                        throw new CloudRuntimeException(String.format("Failed to attach volume to VM: %s", vmNameAndState.first()));
                    }
                }
            } catch (IOException e) {
                throw new CloudRuntimeException(String.format("Unable to revert backup for volume [%s] due to [%s].", bkpPathAndVolUuid.second(), e.getMessage()), e);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to restore volume", e);
        } finally {
            unmountBackupDirectory(mountDirectory);
            deleteTemporaryDirectory(mountDirectory);
        }
    }


    private String mountBackupDirectory(String backupRepoAddress, String backupRepoType) {
        String randomChars = RandomStringUtils.random(5, true, false);
        String mountDirectory = String.format("%s.%s",BACKUP_TEMP_FILE_PREFIX , randomChars);
        try {
            mountDirectory = Files.createTempDirectory(mountDirectory).toString();
            String mount = String.format(MOUNT_COMMAND, backupRepoType, backupRepoAddress, mountDirectory);
            Script.runSimpleBashScript(mount);
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to mount %s to %s", backupRepoType, backupRepoAddress), e);
        }
        return mountDirectory;
    }

    private void unmountBackupDirectory(String backupDirectory) {
        try {
            String umountCmd = String.format(UMOUNT_COMMAND, backupDirectory);
            Script.runSimpleBashScript(umountCmd);
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to unmount backup directory: %s", backupDirectory), e);
        }
    }

    private void deleteTemporaryDirectory(String backupDirectory) {
        try {
            Files.deleteIfExists(Paths.get(backupDirectory));
        } catch (IOException e) {
            throw new CloudRuntimeException(String.format("Failed to delete backup directory: %s", backupDirectory), e);
        }
    }

    private Pair<String, String> getBackupPath(String mountDirectory, String volumePath, String backupPath, String diskType, String volumeUuid) {
        String bkpPath = String.format(FILE_PATH_PLACEHOLDER, mountDirectory, backupPath);
        int lastIndex = volumePath.lastIndexOf(File.separator);
        String volUuid = Objects.isNull(volumeUuid) ? volumePath.substring(lastIndex + 1) : volumeUuid;
        String backupFileName = String.format("%s.%s.qcow2", diskType.toLowerCase(Locale.ROOT), volUuid);
        bkpPath = String.format(FILE_PATH_PLACEHOLDER, bkpPath, backupFileName);
        return new Pair<>(bkpPath, volUuid);
    }

    private void replaceVolumeWithBackup(String volumePath, String backupPath) throws IOException {
        Script.runSimpleBashScript(String.format(RSYNC_COMMAND, backupPath, volumePath));
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
