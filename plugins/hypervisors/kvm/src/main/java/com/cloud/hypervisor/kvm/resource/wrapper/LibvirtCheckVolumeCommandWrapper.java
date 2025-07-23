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
import com.cloud.agent.api.CheckVolumeAnswer;
import com.cloud.agent.api.CheckVolumeCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import org.apache.log4j.Logger;

@ResourceWrapper(handles = CheckVolumeCommand.class)
public final class LibvirtCheckVolumeCommandWrapper extends CommandWrapper<CheckVolumeCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtCheckVolumeCommandWrapper.class);

    @Override
    public Answer execute(final CheckVolumeCommand command, final LibvirtComputingResource libvirtComputingResource) {
        String result = null;
        String srcFile = command.getSrcFile();
        StorageFilerTO storageFilerTO = command.getStorageFilerTO();
        KVMStoragePoolManager poolMgr = libvirtComputingResource.getStoragePoolMgr();
        KVMStoragePool pool = poolMgr.getStoragePool(storageFilerTO.getType(), storageFilerTO.getUuid());

        try {
            if (storageFilerTO.getType() == Storage.StoragePoolType.Filesystem ||
                    storageFilerTO.getType() == Storage.StoragePoolType.NetworkFilesystem) {
                final KVMPhysicalDisk vol = pool.getPhysicalDisk(srcFile);
                final String path = vol.getPath();
                KVMPhysicalDisk.checkQcow2File(path);
                long size = KVMPhysicalDisk.getVirtualSizeFromFile(path);
                return  new CheckVolumeAnswer(command, "", size);
            } else {
                return new Answer(command, false, "Unsupported Storage Pool");
            }

        } catch (final Exception e) {
            s_logger.error("Error while locating disk: "+ e.getMessage());
            return new Answer(command, false, result);
        }
    }
}
