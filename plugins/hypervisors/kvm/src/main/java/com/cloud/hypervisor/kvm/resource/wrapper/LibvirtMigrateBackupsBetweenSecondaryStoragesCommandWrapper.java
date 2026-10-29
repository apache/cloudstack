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
import com.cloud.agent.api.MigrateBackupsBetweenSecondaryStoragesCommand;
import com.cloud.agent.api.MigrateBetweenSecondaryStoragesCommandAnswer;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtMigrateResourceBetweenSecondaryStorages;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.commons.lang3.BooleanUtils;
import org.libvirt.LibvirtException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ResourceWrapper(handles = MigrateBackupsBetweenSecondaryStoragesCommand.class)
public class LibvirtMigrateBackupsBetweenSecondaryStoragesCommandWrapper extends LibvirtMigrateResourceBetweenSecondaryStorages<MigrateBackupsBetweenSecondaryStoragesCommand> {

    @Override
    public Answer execute(MigrateBackupsBetweenSecondaryStoragesCommand command, LibvirtComputingResource serverResource) {
        resourceType = BACKUP;
        filesToRemove = new HashSet<>();
        resourcesToUpdate = new ArrayList<>();
        wait = command.getWait() * 1000;

        DataStoreTO srcDataStore = command.getSrcDataStore();
        DataStoreTO destDataStore = command.getDestDataStore();
        KVMStoragePoolManager storagePoolManager = serverResource.getStoragePoolMgr();

        Set<KVMStoragePool> imagePools = new HashSet<>();
        KVMStoragePool destImagePool = storagePoolManager.getStoragePoolByURI(destDataStore.getUrl());
        imagePools.add(destImagePool);

        String imagePoolUrl;
        KVMStoragePool imagePool = null;

        List<List<DataTO>> backupChains = command.getBackupChain();

        try {
            Map<String, String> parentBackupPathMap = new HashMap<>();
            Map<String, Boolean> parentBackupMigrationMap = new HashMap<>();

            Map<String, String> backupPathMap = new HashMap<>();
            Map<String, Boolean> backupMigrationMap = new HashMap<>();

            for (List<DataTO> chain : backupChains) {
                long lastBackupId = 0;
                boolean backupWasMigrated = false;

                backupPathMap.clear();
                backupMigrationMap.clear();

                for (DataTO backup : chain) {
                    lastBackupId = backup.getId();

                    imagePoolUrl = backup.getDataStore().getUrl();
                    imagePool = storagePoolManager.getStoragePoolByURI(imagePoolUrl);
                    imagePools.add(imagePool);

                    String volumeId = backup.getPath().split("/")[2];
                    String resourceCurrentPath = imagePool.getLocalPathFor(backup.getPath());
                    String resourceParentPath = parentBackupPathMap.get(volumeId);

                    if (imagePoolUrl.equals(srcDataStore.getUrl())) {
                        backupPathMap.put(volumeId, copyResourceToDestDataStore(backup, resourceCurrentPath, destImagePool, resourceParentPath));
                        backupMigrationMap.put(volumeId, true);
                        backupWasMigrated = true;
                    } else {
                        if (BooleanUtils.isTrue(parentBackupMigrationMap.get(volumeId))) {
                            backupPathMap.put(volumeId, rebaseResourceToNewParentPath(resourceCurrentPath, resourceParentPath));
                        } else {
                            backupPathMap.put(volumeId, resourceCurrentPath);
                        }
                        backupMigrationMap.put(volumeId, false);
                    }
                }

                parentBackupPathMap.clear();
                parentBackupPathMap.putAll(backupPathMap);

                parentBackupMigrationMap.clear();
                parentBackupMigrationMap.putAll(backupMigrationMap);

                if (backupWasMigrated) {
                    resourcesToUpdate.add(new Pair<>(lastBackupId, null));
                }
            }
        } catch (LibvirtException | QemuImgException e) {
            logger.error("Exception while migrating backups [{}] to secondary storage [{}] due to: [{}].",
                    command.getBackupChain(), imagePool, e.getMessage(), e);
            return new MigrateBetweenSecondaryStoragesCommandAnswer(command, false, "Migration of backups between secondary storages failed", resourcesToUpdate);
        } finally {
            for (String file : filesToRemove) {
                removeResourceFromSourceDataStore(file);
            }

            for (KVMStoragePool storagePool : imagePools) {
                storagePoolManager.deleteStoragePool(storagePool.getType(), storagePool.getUuid());
            }
        }

        return new MigrateBetweenSecondaryStoragesCommandAnswer(command, true, "success", resourcesToUpdate);
    }
}
