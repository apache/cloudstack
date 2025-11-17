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
import com.cloud.agent.api.MigrateSnapshotsBetweenSecondaryStoragesCommand;
import com.cloud.agent.api.MigrateBetweenSecondaryStoragesCommandAnswer;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.utils.qemu.QemuImageOptions;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.libvirt.LibvirtException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ResourceWrapper(handles = MigrateSnapshotsBetweenSecondaryStoragesCommand.class)
public class LibvirtMigrateSnapshotsBetweenSecondaryStoragesCommandWrapper extends CommandWrapper<MigrateSnapshotsBetweenSecondaryStoragesCommand, Answer, LibvirtComputingResource> {

    protected Set<String> filesToRemove;
    protected List<Pair<Long, String>> resourcesToUpdate;
    protected String resourceType;
    protected int wait;

    @Override
    public Answer execute(MigrateSnapshotsBetweenSecondaryStoragesCommand command, LibvirtComputingResource serverResource) {
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

        String parentSnapshotPath = null;
        boolean parentSnapshotWasMigrated = false;
        Set<Long> snapshotsIdToMigrate = command.getSnapshotsIdToMigrate();

        try {
            for (DataTO snapshot : command.getSnapshotChain()) {
                imagePoolUrl = snapshot.getDataStore().getUrl();
                imagePool = storagePoolManager.getStoragePoolByURI(imagePoolUrl);
                imagePools.add(imagePool);

                String resourceCurrentPath = imagePool.getLocalPathFor(snapshot.getPath());

                if (imagePoolUrl.equals(srcDataStore.getUrl()) && snapshotsIdToMigrate.contains(snapshot.getId())) {
                    parentSnapshotPath = copyResourceToDestDataStore(snapshot, resourceCurrentPath, destImagePool, parentSnapshotPath);
                    parentSnapshotWasMigrated = true;
                } else {
                    if (parentSnapshotWasMigrated) {
                        parentSnapshotPath = rebaseResourceToNewParentPath(resourceCurrentPath, parentSnapshotPath);
                    } else {
                        parentSnapshotPath = resourceCurrentPath;
                    }
                    parentSnapshotWasMigrated = false;
                }
            }
        }  catch (LibvirtException | QemuImgException e) {
            logger.error("Exception while migrating snapshots [{}] to secondary storage [{}] due to: [{}].", command.getSnapshotChain(), imagePool, e.getMessage(), e);
            return new MigrateBetweenSecondaryStoragesCommandAnswer(command, false, "Migration of snapshots between secondary storages failed", resourcesToUpdate);
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

    public String copyResourceToDestDataStore(DataTO resource, String resourceCurrentPath, KVMStoragePool destImagePool, String resourceParentPath) throws QemuImgException, LibvirtException {
        String resourceDestDataStoreFullPath = destImagePool.getLocalPathFor(resource.getPath());
        String resourceDestCheckpointPath = resourceDestDataStoreFullPath.replace("snapshots", "checkpoints");

        QemuImgFile resourceOrigin = new QemuImgFile(resourceCurrentPath, QemuImg.PhysicalDiskFormat.QCOW2);
        QemuImgFile resourceDestination = new QemuImgFile(resourceDestDataStoreFullPath, QemuImg.PhysicalDiskFormat.QCOW2);
        QemuImgFile parentResource = null;

        if (resourceParentPath != null) {
            parentResource = new QemuImgFile(resourceParentPath, QemuImg.PhysicalDiskFormat.QCOW2);
        }

        logger.debug("Migrating {} [{}] to [{}] with {}", resourceType, resourceOrigin, resourceDestination, parentResource == null ? "no parent." : String.format("parent [%s].", parentResource));

        long resourceId = resource.getId();

        createDirsIfNeeded(resourceDestDataStoreFullPath, resourceId);

        QemuImg qemuImg = new QemuImg(wait);
        qemuImg.convert(resourceOrigin, resourceDestination, parentResource, null, null,  new QemuImageOptions(resourceOrigin.getFormat(), resourceOrigin.getFileName(), null), null, true, false);

        filesToRemove.add(resourceCurrentPath);

        String resourceCurrentCheckpointPath = resourceCurrentPath.replace("snapshots", "checkpoints");
        createDirsIfNeeded(resourceDestCheckpointPath, resourceId);
        migrateCheckpointFile(resourceCurrentPath, resourceDestDataStoreFullPath);
        filesToRemove.add(resourceCurrentCheckpointPath);
        resourcesToUpdate.add(new Pair<>(resourceId, resourceDestCheckpointPath));

        return resourceDestDataStoreFullPath;
    }

    private void migrateCheckpointFile(String resourceCurrentPath, String resourceDestDataStoreFullPath) {
        resourceCurrentPath = resourceCurrentPath.replace("snapshots", "checkpoints");
        resourceDestDataStoreFullPath = resourceDestDataStoreFullPath.replace("snapshots", "checkpoints");

        String copyCommand = String.format("cp %s %s", resourceCurrentPath, resourceDestDataStoreFullPath);
        Script.runSimpleBashScript(copyCommand);
    }

    public void removeResourceFromSourceDataStore(String resourcePath) {
        logger.debug("Removing file [{}].", resourcePath);
        try {
            Files.deleteIfExists(Path.of(resourcePath));
        } catch (IOException ex) {
            logger.error("Failed to remove {} [{}].", resourceType, resourcePath, ex);
        }
    }

    public String rebaseResourceToNewParentPath(String resourcePath, String parentResourcePath) throws LibvirtException, QemuImgException {
        QemuImgFile resource = new QemuImgFile(resourcePath, QemuImg.PhysicalDiskFormat.QCOW2);
        QemuImgFile parentResource = new QemuImgFile(parentResourcePath, QemuImg.PhysicalDiskFormat.QCOW2);

        QemuImg qemuImg = new QemuImg(wait);
        qemuImg.rebase(resource, parentResource, parentResource.getFormat().toString(), false);

        return resourcePath;
    }

    private void createDirsIfNeeded(String resourceFullPath, Long resourceId) {
        String dirs = resourceFullPath.substring(0, resourceFullPath.lastIndexOf(File.separator));
        try {
            Files.createDirectories(Path.of(dirs));
        } catch (IOException e) {
            throw new CloudRuntimeException(String.format("Error while creating directories for migration of %s [%s].", resourceType, resourceId), e);
        }
    }
}
