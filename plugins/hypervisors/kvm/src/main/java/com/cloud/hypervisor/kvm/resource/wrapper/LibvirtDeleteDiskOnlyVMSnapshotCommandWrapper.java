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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
        return new Answer(command, true, null);
    }
}
