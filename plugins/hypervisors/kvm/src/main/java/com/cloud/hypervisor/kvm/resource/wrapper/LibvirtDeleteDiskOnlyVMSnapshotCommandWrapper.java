/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.DeleteDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.cloud.storage.Volume;

@ResourceWrapper(handles = DeleteDiskOnlyVmSnapshotCommand.class)
public class LibvirtDeleteDiskOnlyVMSnapshotCommandWrapper extends CommandWrapper<DeleteDiskOnlyVmSnapshotCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(DeleteDiskOnlyVmSnapshotCommand command, LibvirtComputingResource resource) {
        List<DataTO> snapshotsToDelete = command.getSnapshots();
        KVMStoragePoolManager storagePoolMgr = resource.getStoragePoolMgr();

        for (DataTO snapshot : snapshotsToDelete) {
            PrimaryDataStoreTO dataStoreTO = (PrimaryDataStoreTO) snapshot.getDataStore();
            KVMStoragePool kvmStoragePool = storagePoolMgr.getStoragePool(dataStoreTO.getPoolType(), dataStoreTO.getUuid());

            try {
                String path = kvmStoragePool.getLocalPathFor(snapshot.getPath());
                logger.debug("Deleting snapshot [{}] file [{}] as part of VM snapshot deletion.", snapshot.getId(), path);
                Files.deleteIfExists(Path.of(path));
            } catch (IOException e) {
                return new Answer(command, e);
            }
        }

        deleteNvramSnapshotIfNeeded(command, resource, storagePoolMgr, snapshotsToDelete);
        return new Answer(command, true, null);
    }

    protected void deleteNvramSnapshotIfNeeded(DeleteDiskOnlyVmSnapshotCommand command, LibvirtComputingResource resource, KVMStoragePoolManager storagePoolMgr,
            List<DataTO> snapshotsToDelete) {
        if (StringUtils.isBlank(command.getNvramSnapshotPath())) {
            return;
        }

        try {
            KVMStoragePool storagePool;
            if (command.getPrimaryDataStore() != null) {
                PrimaryDataStoreTO dataStore = command.getPrimaryDataStore();
                storagePool = storagePoolMgr.getStoragePool(dataStore.getPoolType(), dataStore.getUuid());
            } else {
                SnapshotObjectTO rootVolumeSnapshot = snapshotsToDelete.stream()
                        .map(SnapshotObjectTO.class::cast)
                        .filter(snapshotObjectTO -> Volume.Type.ROOT.equals(snapshotObjectTO.getVolume().getVolumeType()))
                        .findFirst()
                        .orElse(null);

                if (rootVolumeSnapshot == null) {
                    logger.warn("Unable to locate the root volume snapshot while deleting NVRAM snapshot [{}].", command.getNvramSnapshotPath());
                    return;
                }

                storagePool = resource.getLibvirtUtilitiesHelper().getPrimaryPoolFromDataTo(rootVolumeSnapshot, storagePoolMgr);
            }

            Files.deleteIfExists(Path.of(storagePool.getLocalPathFor(command.getNvramSnapshotPath())));
        } catch (Exception e) {
            logger.warn("Failed to delete the UEFI NVRAM snapshot [{}]. It will be left behind on storage.", command.getNvramSnapshotPath(), e);
        }
    }
}
