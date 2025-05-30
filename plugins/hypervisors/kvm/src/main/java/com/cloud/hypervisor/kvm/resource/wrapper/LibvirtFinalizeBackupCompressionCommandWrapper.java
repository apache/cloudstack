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
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

import org.apache.cloudstack.backup.FinalizeBackupCompressionCommand;
import org.apache.cloudstack.storage.to.BackupDeltaTO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@ResourceWrapper(handles = FinalizeBackupCompressionCommand.class)
public class LibvirtFinalizeBackupCompressionCommandWrapper extends CommandWrapper<FinalizeBackupCompressionCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(FinalizeBackupCompressionCommand command, LibvirtComputingResource serverResource) {
        KVMStoragePool storagePool = null;
        KVMStoragePoolManager storagePoolManager = serverResource.getStoragePoolMgr();
        long totalPhysicalSize = 0;

        if (command.isCleanup()) {
            logger.info("Cleaning up compressed backup deltas [{}].", command.getBackupDeltaTOList());
        } else {
            logger.info("Finalizing backup compression for deltas [{}].", command.getBackupDeltaTOList());
        }
        try {
            storagePool = storagePoolManager.getStoragePoolByURI(command.getBackupDeltaTOList().get(0).getDataStore().getUrl());
            for (BackupDeltaTO delta : command.getBackupDeltaTOList()) {
                Path deltaPath = Path.of(storagePool.getLocalPathFor(delta.getPath()));
                Path compressedDeltaPath = Path.of(deltaPath + ".comp");

                if (command.isCleanup()) {
                    logger.debug("Cleaning up backup delta at [{}].", compressedDeltaPath);
                    Files.deleteIfExists(compressedDeltaPath);
                    continue;
                }

                logger.debug("Moving compressed backup delta at [{}] to [{}].", compressedDeltaPath, deltaPath);
                Files.move(compressedDeltaPath, deltaPath, StandardCopyOption.REPLACE_EXISTING);
                totalPhysicalSize += Files.size(deltaPath);
            }
        } catch (IOException e) {
            return new Answer(command, e);
        } finally {
            if (storagePool != null) {
                storagePoolManager.deleteStoragePool(storagePool.getType(), storagePool.getUuid());
            }
        }
        return new Answer(command, true, String.valueOf(totalPhysicalSize));
    }
}
