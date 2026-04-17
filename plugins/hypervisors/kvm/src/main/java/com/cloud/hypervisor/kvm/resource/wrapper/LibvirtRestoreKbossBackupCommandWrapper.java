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
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import org.apache.cloudstack.backup.RestoreKbossBackupAnswer;
import org.apache.cloudstack.backup.RestoreKbossBackupCommand;
import org.apache.cloudstack.storage.to.BackupDeltaTO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.jetbrains.annotations.NotNull;
import org.libvirt.LibvirtException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

@ResourceWrapper(handles = RestoreKbossBackupCommand.class)
public class LibvirtRestoreKbossBackupCommandWrapper extends CommandWrapper<RestoreKbossBackupCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(RestoreKbossBackupCommand cmd, LibvirtComputingResource resource) {
        Set<Pair<BackupDeltaTO, VolumeObjectTO>> backupToAndVolumeObjectPairs = cmd.getBackupAndVolumePairs();
        Set<BackupDeltaTO> deltasToRemove = cmd.getDeltasToRemove();
        Set<String> secondaryStorageUrls = cmd.getSecondaryStorageUrls();

        KVMStoragePoolManager storagePoolManager = resource.getStoragePoolMgr();

        Set<String> secondaryStorageUuids = new HashSet<>();
        try {
            KVMStoragePool secondaryStorage = mountSecondaryStorages(secondaryStorageUrls, backupToAndVolumeObjectPairs.stream().findFirst().get().first().getDataStore().getUrl(),
                    storagePoolManager, secondaryStorageUuids);

            restoreVolumes(backupToAndVolumeObjectPairs, secondaryStorage, storagePoolManager, cmd.isQuickRestore(), cmd.getWait() * 1000);

            deleteDeltas(deltasToRemove, storagePoolManager);
        } catch (LibvirtException | QemuImgException | IOException e) {
            return new RestoreKbossBackupAnswer(cmd, e, secondaryStorageUuids);
        } finally {
            if (!cmd.isQuickRestore()) {
                for (String uuid : secondaryStorageUuids) {
                    storagePoolManager.deleteStoragePool(Storage.StoragePoolType.NetworkFilesystem, uuid);
                }
            }
        }
        return new RestoreKbossBackupAnswer(cmd, secondaryStorageUuids);
    }

    protected void restoreVolumes(Set<Pair<BackupDeltaTO, VolumeObjectTO>> backupToAndVolumeObjectPairs, KVMStoragePool secondaryStorage, KVMStoragePoolManager storagePoolManager,
            boolean quickRestore, int timeoutInMillis) throws LibvirtException, QemuImgException {
        for (Pair<BackupDeltaTO, VolumeObjectTO> backupToVolumeToPair : backupToAndVolumeObjectPairs) {
            String fullBackupPath = secondaryStorage.getLocalPathFor(backupToVolumeToPair.first().getPath());

            VolumeObjectTO volumeObjectTO = backupToVolumeToPair.second();
            PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO) volumeObjectTO.getDataStore();
            KVMStoragePool primaryStoragePool = storagePoolManager.getStoragePool(primaryDataStoreTO.getPoolType(), primaryDataStoreTO.getUuid());
            String fullVolumePath = primaryStoragePool.getLocalPathFor(volumeObjectTO.getPath());

            QemuImgFile backup = new QemuImgFile(fullBackupPath, QemuImg.PhysicalDiskFormat.QCOW2);
            QemuImgFile volume = new QemuImgFile(fullVolumePath, QemuImg.PhysicalDiskFormat.QCOW2);

            QemuImg qemuImg = getQemuImg(timeoutInMillis);

            if (quickRestore) {
                logger.info("Creating delta over old volume [{}] at [{}] with backing store stored at [{}].", volumeObjectTO.getUuid(), fullVolumePath, fullBackupPath);
                qemuImg.create(volume, backup);
            } else {
                logger.info("Restoring volume [{}] at [{}] with backup stored at [{}].", volumeObjectTO.getUuid(), fullVolumePath, fullBackupPath);
                qemuImg.convert(backup, volume);
            }
        }
    }

    protected QemuImg getQemuImg(int timeoutInMillis) throws LibvirtException, QemuImgException {
        return new QemuImg(timeoutInMillis);
    }

    protected void deleteDeltas(Set<BackupDeltaTO> deltasToRemove, KVMStoragePoolManager storagePoolManager) throws IOException {
        for (BackupDeltaTO deltaToRemove : deltasToRemove) {
            PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO) deltaToRemove.getDataStore();
            KVMStoragePool primaryStoragePool = storagePoolManager.getStoragePool(primaryDataStoreTO.getPoolType(), primaryDataStoreTO.getUuid());
            String fullDeltaPath = primaryStoragePool.getLocalPathFor(deltaToRemove.getPath());
            logger.debug("Deleting leftover delta [{}].", fullDeltaPath);
            Files.deleteIfExists(Path.of(fullDeltaPath));
        }
    }

    protected KVMStoragePool mountSecondaryStorages(Set<String> parentSecondaryStorageUrls, String secondaryStorageUrl, KVMStoragePoolManager storagePoolManager, Set<String> secondaryStorageUuids) {
        for (String url : parentSecondaryStorageUrls) {
            KVMStoragePool pool = storagePoolManager.getStoragePoolByURI(url);
            secondaryStorageUuids.add(pool.getUuid());
        }
        KVMStoragePool pool = storagePoolManager.getStoragePoolByURI(secondaryStorageUrl);
        secondaryStorageUuids.add(pool.getUuid());
        return pool;
    }
}
