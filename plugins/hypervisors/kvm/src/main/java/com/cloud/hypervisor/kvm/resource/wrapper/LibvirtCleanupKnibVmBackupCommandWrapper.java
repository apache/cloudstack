package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtDomainXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.apache.cloudstack.backup.CleanupKnibBackupErrorAnswer;
import org.apache.cloudstack.backup.CleanupKnibBackupErrorCommand;
import org.apache.cloudstack.storage.to.BackupDeltaTO;
import org.apache.cloudstack.storage.to.DeltaMergeTreeTO;
import org.apache.cloudstack.storage.to.KnibTO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.commons.lang3.StringUtils;
import org.libvirt.Domain;
import org.libvirt.Error;
import org.libvirt.LibvirtException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@ResourceWrapper(handles = CleanupKnibBackupErrorCommand.class)
public class LibvirtCleanupKnibVmBackupCommandWrapper extends CommandWrapper<CleanupKnibBackupErrorCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(CleanupKnibBackupErrorCommand command, LibvirtComputingResource serverResource) {
        List<KnibTO> knibTOS = command.getKnibTOs();
        KVMStoragePoolManager storagePoolManager = serverResource.getStoragePoolMgr();

        logger.info("Cleaning up backup error for VM [{}].", command.getVmName());
        cleanupBackupDeltasOnSecondary(command, storagePoolManager, knibTOS);

        if (command.isRunningVM()) {
            return new CleanupKnibBackupErrorAnswer(command, cleanupRunningVm(command, serverResource));
        }

        return new CleanupKnibBackupErrorAnswer(command, mergeDeltasForStoppedVmIfNeeded(command, serverResource));
    }

    private List<VolumeObjectTO> cleanupRunningVm(CleanupKnibBackupErrorCommand command, LibvirtComputingResource serverResource) {
        Domain dm = null;
        try {
            dm = serverResource.getDomain(serverResource.getLibvirtUtilitiesHelper().getConnection(), command.getVmName());
            return mergeDeltasForRunningVmIfNeeded(command, serverResource, dm);
        } catch (LibvirtException e) {
            if (e.getError().getCode() == Error.ErrorNumber.VIR_ERR_NO_DOMAIN && IsVmReallyStopped(command, serverResource)) {
                return mergeDeltasForStoppedVmIfNeeded(command, serverResource);
            }
            logger.error("Error while trying to get VM [{}]. Aborting the process.", command.getVmName(), e);
            return List.of();
        } finally {
            if (dm != null) {
                try {
                    dm.free();
                } catch (LibvirtException e) {
                    logger.warn("Ignoring Libvirt exception.", e);
                }
            }
        }
    }

    private List<VolumeObjectTO> mergeDeltasForStoppedVmIfNeeded(CleanupKnibBackupErrorCommand command, LibvirtComputingResource serverResource) {
        List<VolumeObjectTO> volumeObjectTOList = new ArrayList<>();
        for (KnibTO knibTO : command.getKnibTOs()) {
            VolumeObjectTO volumeObjectTO = knibTO.getVolumeObjectTO();
            PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO)volumeObjectTO.getDataStore();
            KVMStoragePool kvmStoragePool = serverResource.getStoragePoolMgr().getStoragePool(primaryDataStoreTO.getPoolType(), primaryDataStoreTO.getUuid());
            boolean backupErrorDeltaExists = Files.exists(Path.of(kvmStoragePool.getLocalPathFor(volumeObjectTO.getPath())));
            boolean parentBackupDeltaExists = Files.exists(Path.of(kvmStoragePool.getLocalPathFor(knibTO.getDeltaPathOnPrimary())));
            boolean shouldBaseDeltaExist = knibTO.getParentDeltaPathOnPrimary() != null;
            boolean baseDeltaExists = shouldBaseDeltaExist && Files.exists(Path.of(kvmStoragePool.getLocalPathFor(knibTO.getParentDeltaPathOnPrimary())));

            if(!mergeDeltaIfNeeded(serverResource, knibTO, backupErrorDeltaExists, parentBackupDeltaExists, shouldBaseDeltaExist, baseDeltaExists,
                    false, volumeObjectTO, volumeObjectTOList)) {
                return List.of();
            }
        }
        return volumeObjectTOList;
    }

    private List<VolumeObjectTO> mergeDeltasForRunningVmIfNeeded(CleanupKnibBackupErrorCommand command, LibvirtComputingResource serverResource, Domain dm) throws LibvirtException {
        String xmlDesc = dm.getXMLDesc(0);
        LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
        parser.parseDomainXML(xmlDesc);
        List<VolumeObjectTO> volumeObjectTOList = new ArrayList<>();
        for (KnibTO knibTO : command.getKnibTOs()) {
            LibvirtVMDef.DiskDef diskDef = parser.getDisks().stream()
                    .filter(disk -> StringUtils.contains(disk.getDiskPath(), knibTO.getVolumeObjectTO().getPath()) ||
                            StringUtils.contains(disk.getDiskPath(), knibTO.getDeltaPathOnPrimary()) ||
                            StringUtils.contains(disk.getDiskPath(), knibTO.getParentDeltaPathOnPrimary())).findFirst().orElse(null);

            if (diskDef == null) {
                logger.warn("Volume [{}] does not match any record we have. This must be manually normalized.", knibTO.getVolumeObjectTO().getUuid());
                return List.of();
            }

            boolean backupErrorDeltaExists = diskDef.getDiskPath().contains(knibTO.getVolumeObjectTO().getPath());
            boolean parentBackupDeltaExists = diskDef.getDiskPath().contains(knibTO.getDeltaPathOnPrimary()) ||
                    diskDef.getBackingStoreList().stream().anyMatch(path -> path.contains(knibTO.getDeltaPathOnPrimary()));
            boolean shouldBaseDeltaExist = knibTO.getParentDeltaPathOnPrimary() != null;
            boolean baseDeltaExists = shouldBaseDeltaExist && StringUtils.contains(diskDef.getDiskPath(), knibTO.getParentDeltaPathOnPrimary()) ||
                    diskDef.getBackingStoreList().stream().anyMatch(path -> StringUtils.contains(path, knibTO.getParentDeltaPathOnPrimary()));

            mergeDeltaIfNeeded(serverResource, knibTO, backupErrorDeltaExists, parentBackupDeltaExists, shouldBaseDeltaExist, baseDeltaExists, true,
                    knibTO.getVolumeObjectTO(), volumeObjectTOList);
        }

        return volumeObjectTOList;
    }

    private boolean mergeDeltaIfNeeded(LibvirtComputingResource serverResource, KnibTO knibTO, boolean backupErrorDeltaExists,
            boolean parentBackupDeltaExists, boolean shouldBaseDeltaExist, boolean baseDeltaExists, boolean runningVm, VolumeObjectTO volumeObjectTO,
            List<VolumeObjectTO> volumeObjectTOList) {
        DeltaMergeTreeTO deltaMergeTreeTO;
        if (!backupErrorDeltaExists) {
            if (parentBackupDeltaExists && (!shouldBaseDeltaExist || baseDeltaExists)) {
                volumeObjectTO.setPath(knibTO.getDeltaPathOnPrimary());
                logger.debug("Volume [{}] is already consistent. Its path is [{}].", volumeObjectTO.getUuid(), volumeObjectTO.getPath());
                volumeObjectTOList.add(volumeObjectTO);
                return true;
            } else if (baseDeltaExists) {
                volumeObjectTO.setPath(knibTO.getParentDeltaPathOnPrimary());
                logger.debug("Volume [{}] is already consistent. Its path is [{}].", volumeObjectTO.getUuid(), volumeObjectTO.getPath());
                volumeObjectTOList.add(volumeObjectTO);
                return true;
            } else {
                logger.warn("Volume [{}] is inconsistent in an anomalous way. We cannot normalize it automatically.", volumeObjectTO.getUuid());
                return false;
            }
        } else if (parentBackupDeltaExists) {
            logger.debug("Volume [{}] is inconsistent, but we can normalize it. We will merge the delta created by this backup with the delta created by the previous " +
                    "backup.", volumeObjectTO.getUuid());
            deltaMergeTreeTO = new DeltaMergeTreeTO(volumeObjectTO, new BackupDeltaTO(volumeObjectTO.getDataStore(), Hypervisor.HypervisorType.KVM,
                    knibTO.getDeltaPathOnPrimary()), volumeObjectTO, List.of());
        } else if (baseDeltaExists) {
            logger.debug("Volume [{}] is inconsistent, but we can normalize it. We will merge the delta created by this backup with the base volume.",
                    volumeObjectTO.getUuid());
            deltaMergeTreeTO = new DeltaMergeTreeTO(volumeObjectTO, new BackupDeltaTO(volumeObjectTO.getDataStore(), Hypervisor.HypervisorType.KVM,
                    knibTO.getParentDeltaPathOnPrimary()), volumeObjectTO, List.of());
        } else {
            logger.warn("Volume [{}] is inconsistent in an anomalous way. We cannot normalize it automatically.", volumeObjectTO.getUuid());
            return false;
        }

        try {
            if (runningVm) {
                serverResource.mergeDeltaForRunningVm(deltaMergeTreeTO, volumeObjectTO.getVmName(), volumeObjectTO);
            } else {
                serverResource.mergeDeltaForStoppedVm(deltaMergeTreeTO);
            }
            volumeObjectTO.setPath(deltaMergeTreeTO.getParent().getPath());
            volumeObjectTOList.add(volumeObjectTO);
            return true;
        } catch (QemuImgException | IOException | LibvirtException ex) {
            logger.error("Got an exception while trying to merge delta for volume [{}].", volumeObjectTO.getUuid(), ex);
            return false;
        }
    }

    private boolean IsVmReallyStopped(CleanupKnibBackupErrorCommand command, LibvirtComputingResource serverResource) {
        VolumeObjectTO volume = command.getKnibTOs().stream()
                .filter(knibTO -> knibTO.getVolumeObjectTO().getDeviceId() == 0)
                .map(KnibTO::getVolumeObjectTO).findFirst().orElseThrow();
        PrimaryDataStoreTO primary = (PrimaryDataStoreTO)volume.getDataStore();
        KVMStoragePool storage = serverResource.getStoragePoolMgr().getStoragePool(primary.getPoolType(), primary.getUuid());
        KVMPhysicalDisk disk = storage.getPhysicalDisk(volume.getUuid());
        File diskFile = new File(disk.getPath());
        long time1 = diskFile.lastModified();
        try {
            Thread.sleep(30 * 1000);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        long time2 = diskFile.lastModified();
        if (time1 != time2) {
            logger.info("VM root disk [{}] has been modified in the last 30 seconds. It seems the VM is running, even though we were unable to find it. If the VM is " +
                    "running on this host, you can try again later. If the VM was somehow migrated, you should update the database directly.");
            return false;
        }
        logger.warn("VM [{}] was not found by Libvirt and the root disk has not had any writes on the last 30 seconds. Assuming that it is stopped.", command.getVmName());
        return true;
    }

    private void cleanupBackupDeltasOnSecondary(CleanupKnibBackupErrorCommand command, KVMStoragePoolManager storagePoolManager, List<KnibTO> knibTOS) {
        KVMStoragePool storagePool = null;
        try {
            storagePool = storagePoolManager.getStoragePoolByURI(command.getImageStoreUrl());
            for (KnibTO knibTO : knibTOS) {
                String deltaPath = storagePool.getLocalPathFor(knibTO.getDeltaPathOnSecondary());
                logger.debug("Cleaning up file at [{}] if it exists.", deltaPath);
                try {
                    Files.deleteIfExists(Path.of(deltaPath));
                } catch (IOException e) {
                    logger.error("Unable to delete leftover backup delta at [{}].", deltaPath);
                }
            }
        } finally {
            if (storagePool != null) {
                storagePoolManager.deleteStoragePool(storagePool.getType(), storagePool.getUuid());
            }
        }
    }
}
