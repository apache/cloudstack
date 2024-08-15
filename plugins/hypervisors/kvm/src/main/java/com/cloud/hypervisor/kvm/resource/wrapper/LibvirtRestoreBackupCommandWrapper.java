package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.backup.RestoreBackupCommand;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@ResourceWrapper(handles = RestoreBackupCommand.class)
public class LibvirtRestoreBackupCommandWrapper extends CommandWrapper<RestoreBackupCommand, Answer, LibvirtComputingResource> {
    private static final String BACKUP_TEMP_FILE_PREFIx = "csbackup";
//    private static final String MOUNT_COMMAND = "sudo mount -t %s %s %s $([[ ! -z '%s' ]] && echo -o %s)";
    private static final String MOUNT_COMMAND = "sudo mount -t %s %s %s";
    private static final String UMOUNT_COMMAND = "sudo umount %s";
    private static final String VIRSH_DEFINE = "sudo virsh define %s";
    private static final String VM_DOMAIN_XML = "domain-config.xml";

    @Override
    public Answer execute(RestoreBackupCommand command, LibvirtComputingResource serverResource) {
        String vmName = command.getVmName();
        String backupPath = command.getBackupPath();
        String backupRepoAddress = command.getBackupRepoAddress();
        String backupRepoType = command.getBackupRepoType();
        String mountOptions = command.getMountOptions();
        boolean vmExists = command.isVmExists();
        List<String> volumePaths = command.getVolumePaths();

        if (vmExists) {
            restoreVolumesOfExistingVM(volumePaths, backupPath, backupRepoType, backupRepoAddress, mountOptions);
        } else {
            restoreVolumesOfDestroyedVMs(volumePaths, vmName, backupPath, backupRepoType, backupRepoAddress, mountOptions);
        }

        return null;
    }

    private void restoreVolumesOfExistingVM(List<String> volumePaths, String backupPath,
                                             String backupRepoType, String backupRepoAddress, String mountOptions) {
        String diskType = "root";
        String mountDirectory = mountBackupDirectory(backupRepoAddress, backupRepoType);
        try {
            for (int i = 0; i < volumePaths.size(); i++) {
                String volumePath = volumePaths.get(i);
                Pair<String, String> bkpPathAndVolUuid = getBackupPath(mountDirectory, volumePath, backupPath, diskType, i);
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
                Pair<String, String> bkpPathAndVolUuid = getBackupPath(mountDirectory, volumePath, backupPath, diskType, i);
                diskType = "datadisk";
                try {
                    replaceVolumeWithBackup(volumePath, bkpPathAndVolUuid.first());
                } catch (IOException e) {
                    throw new CloudRuntimeException(String.format("Unable to revert backup for volume [%s] due to [%s].", bkpPathAndVolUuid.second(), e.getMessage()), e);
                }
            }
            defineVmDomain(vmName, backupPath);
        } finally {
            unmountBackupDirectory(mountDirectory);
            deleteTemporaryDirectory(mountDirectory);
        }

    }

    private String mountBackupDirectory(String backupRepoAddress, String backupRepoType) {
        String randomChars = RandomStringUtils.random(5, true, false);
        String mountDirectory = String.format("%s.%s",BACKUP_TEMP_FILE_PREFIx , randomChars);
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

    private void defineVmDomain(String vmName, String backupPath) {
        try {
            Script.runSimpleBashScript(String.format(VIRSH_DEFINE, backupPath + VM_DOMAIN_XML));
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to define domain for VM: %s", vmName), e);
        }
    }

    private Pair<String, String> getBackupPath(String mountDirectory, String volumePath, String backupPath, String diskType, int deviceId) {
        String bkpPath = String.format("%s/%s", mountDirectory, backupPath);
        int lastIndex = volumePath.lastIndexOf("/");
        String volUuid = volumePath.substring(lastIndex + 1);
        String backupFileName = String.format("%s.%s.%s", deviceId, diskType, volUuid);
        logger.debug("BACKUP file name: " + backupFileName);
        bkpPath = String.format("%s/%s", bkpPath, backupFileName);
        return new Pair<>(bkpPath, volUuid);
    }

    private void replaceVolumeWithBackup(String volumePath, String backupPath) throws IOException {
        Files.copy(Paths.get(backupPath), Paths.get(volumePath), StandardCopyOption.REPLACE_EXISTING);
    }
}
