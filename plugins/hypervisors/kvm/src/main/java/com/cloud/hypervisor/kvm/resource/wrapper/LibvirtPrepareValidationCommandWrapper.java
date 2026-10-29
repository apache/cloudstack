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
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.backup.PrepareValidationCommand;
import org.apache.cloudstack.storage.to.BackupDeltaTO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.libvirt.LibvirtException;

import java.util.ArrayList;
import java.util.List;

@ResourceWrapper(handles =  PrepareValidationCommand.class)
public class LibvirtPrepareValidationCommandWrapper extends CommandWrapper<PrepareValidationCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(PrepareValidationCommand command, LibvirtComputingResource resource) {
        List<Pair<BackupDeltaTO, VolumeObjectTO>> backingFileAndVolumeList = command.getBackupToVolumeList();

        List<KVMStoragePool> secondaryReferences = new ArrayList<>();
        KVMStoragePoolManager storagePoolMgr = resource.getStoragePoolMgr();
        try {
            for (String imageStoreUri : command.getImageStoreSet()) {
                secondaryReferences.add(storagePoolMgr.getStoragePoolByURI(imageStoreUri));
            }

            for (Pair<BackupDeltaTO, VolumeObjectTO> backingFileAndVolume : backingFileAndVolumeList) {
                logger.debug("Preparing volume [{}] for validation.", backingFileAndVolume.second());
                BackupDeltaTO backupDelta = backingFileAndVolume.first();
                DataStoreTO dataStoreTO = backupDelta.getDataStore();
                KVMStoragePool imageStore = storagePoolMgr.getStoragePoolByURI(dataStoreTO.getUrl());
                secondaryReferences.add(imageStore);

                createVolume(command, backingFileAndVolume, imageStore, backupDelta, storagePoolMgr);
            }
        } catch (LibvirtException | QemuImgException e) {
            logger.error("Failed to prepare VM [{}] for validation due to:", backingFileAndVolumeList.get(0).second().getVmName(), e);
            throw new CloudRuntimeException(e);
        } finally {
            for (KVMStoragePool secondary : secondaryReferences) {
                storagePoolMgr.deleteStoragePool(secondary.getType(), secondary.getUuid());
            }
        }
        return new Answer(command);
    }

    private void createVolume(PrepareValidationCommand command, Pair<BackupDeltaTO, VolumeObjectTO> volumeAndBackingFile, KVMStoragePool imageStore, BackupDeltaTO backupDelta,
            KVMStoragePoolManager storagePoolMgr) throws LibvirtException, QemuImgException {
        String fullBackupPath = imageStore.getLocalPathFor(backupDelta.getPath());

        VolumeObjectTO volumeObjectTO = volumeAndBackingFile.second();
        PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO) volumeObjectTO.getDataStore();
        KVMStoragePool primaryStoragePool = storagePoolMgr.getStoragePool(primaryDataStoreTO.getPoolType(), primaryDataStoreTO.getUuid());
        String fullVolumePath = primaryStoragePool.getLocalPathFor(volumeObjectTO.getPath());

        QemuImgFile backup = new QemuImgFile(fullBackupPath, QemuImg.PhysicalDiskFormat.QCOW2);
        QemuImgFile volume = new QemuImgFile(fullVolumePath, QemuImg.PhysicalDiskFormat.QCOW2);

        QemuImg qemuImg = new QemuImg(command.getWait() * 1000);

        qemuImg.create(volume, backup);
    }
}
