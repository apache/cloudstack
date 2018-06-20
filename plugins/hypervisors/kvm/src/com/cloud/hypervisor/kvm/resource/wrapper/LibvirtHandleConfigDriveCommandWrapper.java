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
import java.nio.file.Paths;

import org.apache.cloudstack.storage.configdrive.ConfigDriveBuilder;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.HandleConfigDriveIsoCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;

@ResourceWrapper(handles =  HandleConfigDriveIsoCommand.class)
public final class LibvirtHandleConfigDriveCommandWrapper extends CommandWrapper<HandleConfigDriveIsoCommand, Answer, LibvirtComputingResource> {
    private static final Logger LOG = Logger.getLogger(LibvirtHandleConfigDriveCommandWrapper.class);

    @Override
    public Answer execute(final HandleConfigDriveIsoCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
        final KVMStoragePool pool = storagePoolMgr.getStoragePool(Storage.StoragePoolType.NetworkFilesystem, command.getDestStore().getUuid());
        if (pool == null) {
            return new Answer(command, false, "Pool not found, config drive for KVM is only supported for NFS");
        }

        final String mountPoint = pool.getLocalPath();
        final Path isoPath = Paths.get(mountPoint, command.getIsoFile());
        final File isoFile = new File(mountPoint, command.getIsoFile());
        if (command.isCreate()) {
            LOG.debug("Creating config drive: " + command.getIsoFile());
            if (command.getIsoData() == null) {
                return new Answer(command, false, "Invalid config drive ISO data received");
            }
            if (isoFile.exists()) {
                LOG.debug("An old config drive iso already exists");
            }
            try {
                Files.createDirectories(isoPath.getParent());
                ConfigDriveBuilder.base64StringToFile(command.getIsoData(), mountPoint, command.getIsoFile());
            } catch (IOException e) {
                return new Answer(command, false, "Failed due to exception: " + e.getMessage());
            }
        } else {
            try {
                Files.deleteIfExists(isoPath);
            } catch (IOException e) {
                LOG.warn("Failed to delete config drive: " + isoPath.toAbsolutePath().toString());
                return new Answer(command, false, "Failed due to exception: " + e.getMessage());
            }
        }

        return new Answer(command);
    }
}