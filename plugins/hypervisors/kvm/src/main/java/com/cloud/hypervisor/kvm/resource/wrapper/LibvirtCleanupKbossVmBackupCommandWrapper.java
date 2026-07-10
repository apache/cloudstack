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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.backup.CleanupKbossBackupErrorAnswer;
import org.apache.cloudstack.backup.CleanupKbossBackupErrorCommand;
import org.apache.cloudstack.storage.to.BackupDeltaTO;
import org.apache.cloudstack.storage.to.DeltaMergeTreeTO;
import org.apache.cloudstack.storage.to.KbossTO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.commons.lang3.StringUtils;
import org.libvirt.Domain;
import org.libvirt.Error;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtDomainXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;

@ResourceWrapper(handles = CleanupKbossBackupErrorCommand.class)
public class LibvirtCleanupKbossVmBackupCommandWrapper extends CommandWrapper<CleanupKbossBackupErrorCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(CleanupKbossBackupErrorCommand command, LibvirtComputingResource serverResource) {
        List<KbossTO> kbossTOS = command.getKbossTOs();
        KVMStoragePoolManager storagePoolManager = serverResource.getStoragePoolMgr();

        logger.info("Cleaning up backup error for VM [{}].", command.getVmName());
        cleanupBackupDeltasOnSecondary(command, storagePoolManager, kbossTOS);

        if (command.isRunningVM()) {
            Pair<Map<String, Pair<String,Boolean>>, Boolean> volumeTosAndIsVmRunning = cleanupRunningVm(command, serverResource);
            return new CleanupKbossBackupErrorAnswer(command, volumeTosAndIsVmRunning.first(), volumeTosAndIsVmRunning.second());
        }

        return new CleanupKbossBackupErrorAnswer(command, mergeDeltasForStoppedVmIfNeeded(command, serverResource), false);
    }

    private Pair<Map<String, Pair<String,Boolean>>, Boolean> cleanupRunningVm(CleanupKbossBackupErrorCommand command, LibvirtComputingResource serverResource) {
        Domain dm = null;
        try {
            dm = serverResource.getDomain(serverResource.getLibvirtUtilitiesHelper().getConnection(), command.getVmName());
            return new Pair<>(mergeDeltasForRunningVmIfNeeded(command, serverResource, dm), true);
        } catch (LibvirtException e) {
            if (e.getError().getCode() == Error.ErrorNumber.VIR_ERR_NO_DOMAIN && isVmReallyStopped(command, serverResource)) {
                return new Pair<>(mergeDeltasForStoppedVmIfNeeded(command, serverResource), false);
            }
            logger.error("Error while trying to get VM [{}]. Aborting the process.", command.getVmName(), e);
            return new Pair<>(Map.of(), false);
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

    private Map<String, Pair<String,Boolean>> mergeDeltasForStoppedVmIfNeeded(CleanupKbossBackupErrorCommand command, LibvirtComputingResource serverResource) {
        HashMap<String, Pair<String,Boolean>> volumeToChainEnded = new HashMap<>();
        for (KbossTO kbossTO : command.getKbossTOs()) {
            VolumeObjectTO volumeObjectTO = kbossTO.getVolumeObjectTO();
            PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO)volumeObjectTO.getDataStore();
            KVMStoragePool kvmStoragePool = serverResource.getStoragePoolMgr().getStoragePool(primaryDataStoreTO.getPoolType(), primaryDataStoreTO.getUuid());
            boolean volumePathMissing = !Files.exists(Path.of(kvmStoragePool.getLocalPathFor(volumeObjectTO.getPath())));
            boolean deltaPathMissing = !Files.exists(Path.of(kvmStoragePool.getLocalPathFor(kbossTO.getDeltaPathOnPrimary())));
            boolean basePathMissing = kbossTO.getParentDeltaPathOnPrimary() != null && !Files.exists(Path.of(kvmStoragePool.getLocalPathFor(kbossTO.getParentDeltaPathOnPrimary())));
            List<DataTO> grandchildren = kbossTO.getDeltaPaths().isEmpty() ? List.of() : List.of(new BackupDeltaTO(volumeObjectTO.getDataStore(),
                    Hypervisor.HypervisorType.KVM, kbossTO.getDeltaPaths().get(0)));

            Boolean chainEnded = mergeDeltaIfNeeded(serverResource, kbossTO, volumeObjectTO, grandchildren, volumePathMissing, deltaPathMissing, basePathMissing,
                    command.isErrorOnCreate(), false, command.isTopDelta(), command.isEndOfChain());
            volumeToChainEnded.put(volumeObjectTO.getUuid(), new Pair<>(volumeObjectTO.getPath(), chainEnded));
        }
        return volumeToChainEnded;
    }

    private Map<String, Pair<String, Boolean>> mergeDeltasForRunningVmIfNeeded(CleanupKbossBackupErrorCommand command, LibvirtComputingResource serverResource, Domain dm) throws LibvirtException {
        HashMap<String, Pair<String, Boolean>> volumeIdToPathAndChainEnded = new HashMap<>();
        String xmlDesc = dm.getXMLDesc(0);
        LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
        parser.parseDomainXML(xmlDesc);
        for (KbossTO kbossTO : command.getKbossTOs()) {
            VolumeObjectTO volumeObjectTO = kbossTO.getVolumeObjectTO();
            String volumePath = volumeObjectTO.getPath();
            LibvirtVMDef.DiskDef diskDef = parser.getDisks().stream()
                    .filter(disk -> hasPath(disk, volumePath, kbossTO.getDeltaPathOnPrimary(), kbossTO.getParentDeltaPathOnPrimary()))
                    .findFirst().orElse(null);

            if (diskDef == null) {
                logger.warn("Volume [{}] does not match any record we have. This must be manually normalized.", volumeObjectTO.getUuid());
                return Map.of();
            }

            List<String> backingStoreList = diskDef.getBackingStoreList();
            backingStoreList.add(0, diskDef.getDiskPath());

            boolean volumePathMissing = true;
            boolean deltaPathMissing = true;
            boolean basePathMissing = kbossTO.getParentDeltaPathOnPrimary() != null;
            for (String delta : backingStoreList) {
                if (StringUtils.contains(delta, volumePath)) {
                    volumePathMissing = false;
                }
                if (StringUtils.contains(delta, kbossTO.getDeltaPathOnPrimary())) {
                    deltaPathMissing = false;
                }
                if (StringUtils.contains(delta, kbossTO.getParentDeltaPathOnPrimary())) {
                    basePathMissing = false;
                }
            }

            Boolean chainEnded = mergeDeltaIfNeeded(serverResource, kbossTO, volumeObjectTO, List.of(), volumePathMissing, deltaPathMissing, basePathMissing,
                    command.isErrorOnCreate(), true, command.isTopDelta(), command.isEndOfChain());
            volumeIdToPathAndChainEnded.put(volumeObjectTO.getUuid(), new Pair<>(volumeObjectTO.getPath(), chainEnded));
        }

        return volumeIdToPathAndChainEnded;
    }

    private boolean hasPath(LibvirtVMDef.DiskDef diskDef, String... paths) {
        List<String> chain = diskDef.getBackingStoreList();
        chain = chain != null ? chain : new ArrayList<>();
        chain.add(diskDef.getDiskPath());
        for (String delta : chain) {
            if (Arrays.stream(paths).anyMatch(path -> StringUtils.contains(delta, path))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return True if error chain is already ended, false otherwise.
     * */
    private boolean mergeDeltaIfNeeded(LibvirtComputingResource serverResource, KbossTO kbossTO, VolumeObjectTO volumeObjectTO, List<DataTO> grandChildren,
            boolean volumePathMissing, boolean deltaPathMissing, boolean basePathMissing, boolean errorOnCreate, boolean runningVm, boolean isTopDelta, boolean isEndOfChain) {
        String errorMessage = String.format("Volume [%s] is inconsistent in an anomalous way. We cannot normalize it automatically.", volumeObjectTO.getUuid());
        if (!errorOnCreate) {
            // Base should never be missing if it is not an error from creation. If the volume path is missing and it is not the delta that was being removed, it is an anomaly as well.
            if (basePathMissing || (volumePathMissing && !isTopDelta)) {
                logger.warn(errorMessage);
                throw new CloudRuntimeException(String.format ("Unable to find the base delta or the volume path was not found. We cannot normalize it automatically. At least " +
                        "one of these should exist: volume [%s]; base path [%s].", volumeObjectTO.getPath(), kbossTO.getParentDeltaPathOnPrimary()));
            }
            // This means that the delta merge likely succeeded but the host was unable to reply to the Management Server
            if (deltaPathMissing) {
                // This is if the delta being merged was the top delta. Then we must update its path.
                if (volumePathMissing) {
                    volumeObjectTO.setPath(kbossTO.getParentDeltaPathOnPrimary());
                }
                logger.debug("Volume [{}] is already consistent. Its path is [{}].", volumeObjectTO.getUuid(), volumeObjectTO.getPath());
                return true;
            }
            return false;
        }

        DeltaMergeTreeTO deltaMergeTreeTO;
        boolean errorChainFinished;
        if (volumePathMissing && !deltaPathMissing) { // The process was not started for this volume
            DataTO child;
            // If it is the top delta, we should set the volume path as the delta path on primary, as it is the real path. This will get updated later after being merged.
            if (isTopDelta) {
                volumeObjectTO.setPath(kbossTO.getDeltaPathOnPrimary());
                child = volumeObjectTO;
            } else { // Otherwise, we set it as the old path of the volume. In this case, this will be its final path.
                volumeObjectTO.setPath(kbossTO.getOldVolumePath());
                child = new BackupDeltaTO(volumeObjectTO.getDataStore(), Hypervisor.HypervisorType.KVM, kbossTO.getDeltaPathOnPrimary());
            }
            logger.debug("Volume [{}] is consistent, the backup process for it was not started. Its current path is [{}]. We will merge the old backup chain.",
                    volumeObjectTO.getUuid(), volumeObjectTO.getPath());
            deltaMergeTreeTO = new DeltaMergeTreeTO(volumeObjectTO, new BackupDeltaTO(volumeObjectTO.getDataStore(), Hypervisor.HypervisorType.KVM,
                    kbossTO.getParentDeltaPathOnPrimary()), child, grandChildren);
            errorChainFinished = true;
        } else if (!isEndOfChain && !volumePathMissing && (deltaPathMissing || kbossTO.getParentDeltaPathOnPrimary() == null)) { // The process was completed for this volume
            logger.debug("Volume [{}] is consistent, the backup process was completed for it. Its current path is [{}].", volumeObjectTO.getUuid(), volumeObjectTO.getPath());
            return false;
        } else if (isEndOfChain && volumePathMissing && !basePathMissing) { // The process was completed for this volume
            volumeObjectTO.setPath(kbossTO.getParentDeltaPathOnPrimary());
            logger.debug("Volume [{}] is consistent, the backup process was completed for it. Its current path is [{}].", volumeObjectTO.getUuid(), volumeObjectTO.getPath());
            return true;
        } else if (!volumePathMissing && !deltaPathMissing) { // The process stopped midway
            logger.debug("Volume [{}] is inconsistent, but we can normalize it. We will merge the delta created by the last backup with the base volume.",
                    volumeObjectTO.getUuid());
            deltaMergeTreeTO = new DeltaMergeTreeTO(volumeObjectTO, new BackupDeltaTO(volumeObjectTO.getDataStore(), Hypervisor.HypervisorType.KVM,
                    kbossTO.getParentDeltaPathOnPrimary()), new BackupDeltaTO(volumeObjectTO.getDataStore(), Hypervisor.HypervisorType.KVM, kbossTO.getDeltaPathOnPrimary()),
                    grandChildren);
            errorChainFinished = false;
            isTopDelta = false;
        } else {
            logger.warn(errorMessage);
            throw new CloudRuntimeException(errorMessage + " Maybe it is a good idea to open an issue to get help on this.");
        }

        try {
            if (runningVm) {
                serverResource.mergeDeltaForRunningVm(deltaMergeTreeTO, volumeObjectTO.getVmName(), volumeObjectTO);
            } else {
                serverResource.mergeDeltaForStoppedVm(deltaMergeTreeTO);
            }
            if (isTopDelta) {
                volumeObjectTO.setPath(deltaMergeTreeTO.getParent().getPath());
            }
            return errorChainFinished;
        } catch (QemuImgException | IOException | LibvirtException ex) {
            logger.error("Got an exception while trying to merge delta for volume [{}].", volumeObjectTO.getUuid(), ex);
            throw new CloudRuntimeException(ex);
        }
    }

    /**
     * Checks if the VM is really stopped by checking if its root volume has had any writes on the last 30 seconds.
     * */
    private boolean isVmReallyStopped(CleanupKbossBackupErrorCommand command, LibvirtComputingResource serverResource) {
        VolumeObjectTO volume = command.getKbossTOs().stream()
                .filter(kbossTO -> kbossTO.getVolumeObjectTO().getDeviceId() == 0)
                .map(KbossTO::getVolumeObjectTO).findFirst().orElseThrow();
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

    private void cleanupBackupDeltasOnSecondary(CleanupKbossBackupErrorCommand command, KVMStoragePoolManager storagePoolManager, List<KbossTO> kbossTOS) {
        KVMStoragePool storagePool = null;
        try {
            storagePool = storagePoolManager.getStoragePoolByURI(command.getImageStoreUrl());
            for (KbossTO kbossTO : kbossTOS) {
                String deltaPath = storagePool.getLocalPathFor(kbossTO.getDeltaPathOnSecondary());
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
