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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import org.apache.cloudstack.backup.ExtractBackupFileCommand;
import org.apache.cloudstack.utils.qemu.GuestfishClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

@ResourceWrapper(handles = ExtractBackupFileCommand.class)
public class LibvirtDownloadBackupFileCommandWrapper extends CommandWrapper<ExtractBackupFileCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(ExtractBackupFileCommand cmd, LibvirtComputingResource serverResource) {
        NfsTO nfs = cmd.getDatastore();
        KVMStoragePoolManager storagePoolMgr = serverResource.getStoragePoolMgr();
        Set<String> secondaryStorageUuids = new HashSet<>();

        try {
            KVMStoragePool imagePool = serverResource.mountSecondaryStorages(cmd.getSecondaryStorageUrls(), nfs.getUrl(), storagePoolMgr, secondaryStorageUuids);
            String fullBackupPath = imagePool.getLocalPathFor(cmd.getBackupPath());
            if (cmd.isCleanup()) {
                return cleanupExtractedFile(cmd, imagePool);
            }
            boolean isDir = false;
            try (GuestfishClient guestfishClient = new GuestfishClient(fullBackupPath, cmd.getVolumeId())) {
                isDir = guestfishClient.extractFile(cmd.getFilesystem(), cmd.getFilePath(), imagePool.getLocalPathFor(cmd.getDestinationPath()), true);
            }

            return new Answer(cmd, true, Boolean.toString(isDir));
        } finally {
            for (String uuid : secondaryStorageUuids) {
                storagePoolMgr.deleteStoragePool(Storage.StoragePoolType.NetworkFilesystem, uuid);
            }
        }
    }

    private Answer cleanupExtractedFile(ExtractBackupFileCommand cmd, KVMStoragePool imagePool) {
        try {
            Files.deleteIfExists(Path.of(imagePool.getLocalPathFor(cmd.getDestinationPath())));
        } catch (IOException e) {
            return new Answer(cmd, e);
        }
        return new Answer(cmd);
    }
}
