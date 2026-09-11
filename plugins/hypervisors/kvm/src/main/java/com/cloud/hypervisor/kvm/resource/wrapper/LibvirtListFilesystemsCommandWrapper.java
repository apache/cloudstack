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
import com.cloud.agent.api.to.FilesystemInfoTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import org.apache.cloudstack.backup.ListFilesystemsAnswer;
import org.apache.cloudstack.backup.ListFilesystemsCommand;
import org.apache.cloudstack.utils.qemu.GuestfishClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ResourceWrapper(handles = ListFilesystemsCommand.class)
public class LibvirtListFilesystemsCommandWrapper extends CommandWrapper<ListFilesystemsCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(ListFilesystemsCommand cmd, LibvirtComputingResource serverResource) {
        List<FilesystemInfoTO> filesystemInfoTOList = new ArrayList<>();
        NfsTO nfs = cmd.getDatastore();
        KVMStoragePoolManager storagePoolMgr = serverResource.getStoragePoolMgr();
        Set<String> secondaryStorageUuids = new HashSet<>();

        try {
            KVMStoragePool imagePool = serverResource.mountSecondaryStorages(cmd.getSecondaryStorageUrls(), nfs.getUrl(), storagePoolMgr, secondaryStorageUuids);
            for (Long volumeId : cmd.getVolumeIdAndBackupPath().keySet()) {
                String fullPath = imagePool.getLocalPathFor(cmd.getVolumeIdAndBackupPath().get(volumeId));
                try (GuestfishClient guestfishClient = new GuestfishClient(fullPath, volumeId)) {
                    filesystemInfoTOList.addAll(guestfishClient.listFilesystems());
                }
            }

            return new ListFilesystemsAnswer(cmd, filesystemInfoTOList);
        } finally {
            for (String uuid : secondaryStorageUuids) {
                storagePoolMgr.deleteStoragePool(Storage.StoragePoolType.NetworkFilesystem, uuid);
            }
        }
    }
}
