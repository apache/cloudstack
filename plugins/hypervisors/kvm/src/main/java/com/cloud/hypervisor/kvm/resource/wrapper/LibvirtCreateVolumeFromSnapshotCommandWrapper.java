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

import java.util.UUID;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateVolumeFromSnapshotAnswer;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;

@ResourceWrapper(handles =  CreateVolumeFromSnapshotCommand.class)
public final class LibvirtCreateVolumeFromSnapshotCommandWrapper extends CommandWrapper<CreateVolumeFromSnapshotCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final CreateVolumeFromSnapshotCommand command, final LibvirtComputingResource libvirtComputingResource) {
        try {

            String snapshotPath = command.getSnapshotUuid();
            final int index = snapshotPath.lastIndexOf("/");
            snapshotPath = snapshotPath.substring(0, index);

            final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
            final KVMStoragePool secondaryPool = storagePoolMgr.getStoragePoolByURI(command.getSecondaryStorageUrl() + snapshotPath);
            final KVMPhysicalDisk snapshot = secondaryPool.getPhysicalDisk(command.getSnapshotName());

            final String primaryUuid = command.getPrimaryStoragePoolNameLabel();

            final StorageFilerTO pool = command.getPool();
            final KVMStoragePool primaryPool = storagePoolMgr.getStoragePool(pool.getType(), primaryUuid);

            final String volUuid = UUID.randomUUID().toString();
            final KVMPhysicalDisk disk = storagePoolMgr.copyPhysicalDisk(snapshot, volUuid, primaryPool, 0);

            if (disk == null) {
                throw new NullPointerException("Disk was not successfully copied to the new storage.");
            }

            return new CreateVolumeFromSnapshotAnswer(command, true, "", disk.getName());
        } catch (final CloudRuntimeException e) {
            return new CreateVolumeFromSnapshotAnswer(command, false, e.toString(), null);
        } catch (final Exception e) {
            return new CreateVolumeFromSnapshotAnswer(command, false, e.toString(), null);
        }
    }
}
