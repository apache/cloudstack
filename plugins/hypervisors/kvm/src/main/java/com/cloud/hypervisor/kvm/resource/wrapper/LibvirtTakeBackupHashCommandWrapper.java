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

import com.cloud.utils.exception.CloudRuntimeException;
import com.dynatrace.hash4j.hashing.HashStream128;
import com.dynatrace.hash4j.hashing.HashValue128;
import com.dynatrace.hash4j.hashing.Hashing;
import org.apache.cloudstack.backup.TakeBackupHashCommand;
import org.apache.cloudstack.storage.to.BackupDeltaTO;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@ResourceWrapper(handles = TakeBackupHashCommand.class)
public class LibvirtTakeBackupHashCommandWrapper extends CommandWrapper<TakeBackupHashCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(TakeBackupHashCommand command, LibvirtComputingResource resource) {
        String backupUuid = command.getBackupUuid();
        logger.info("Taking hash of backup [{}].", backupUuid);

        KVMStoragePoolManager storagePoolManager = resource.getStoragePoolMgr();
        KVMStoragePool imagePool = null;
        try {
            imagePool = storagePoolManager.getStoragePoolByURI(command.getBackupDeltaTOList().get(0).getDataStore().getUrl());
            HashStream128 hashStream128 = Hashing.xxh3_128().hashStream();
            for (BackupDeltaTO backupDelta : command.getBackupDeltaTOList()) {
                try (InputStream is = new BufferedInputStream(new FileInputStream(imagePool.getLocalPathFor(backupDelta.getPath())))) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        hashStream128.putBytes(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    throw new CloudRuntimeException(e);
                }
            }
            HashValue128 hash = hashStream128.get();
            String hashString = hash.toString();
            logger.info("The xxHash128 of backup [{}] is [{}].", backupUuid, hashString);
            return new Answer(command, true, hashString);
        } finally {
            if (imagePool != null) {
                storagePoolManager.deleteStoragePool(imagePool.getType(), imagePool.getUuid());
            }
        }
    }
}
