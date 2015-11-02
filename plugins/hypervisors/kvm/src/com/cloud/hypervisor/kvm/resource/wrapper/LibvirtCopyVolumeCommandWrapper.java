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

import java.io.File;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;

@ResourceWrapper(handles =  CopyVolumeCommand.class)
public final class LibvirtCopyVolumeCommandWrapper extends CommandWrapper<CopyVolumeCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final CopyVolumeCommand command, final LibvirtComputingResource libvirtComputingResource) {
        /**
         * This method is only used for copying files from Primary Storage TO
         * Secondary Storage
         *
         * It COULD also do it the other way around, but the code in the
         * ManagementServerImpl shows that it always sets copyToSecondary to
         * true
         */
        final boolean copyToSecondary = command.toSecondaryStorage();
        String volumePath = command.getVolumePath();
        final StorageFilerTO pool = command.getPool();
        final String secondaryStorageUrl = command.getSecondaryStorageURL();
        KVMStoragePool secondaryStoragePool = null;
        KVMStoragePool primaryPool = null;

        final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
        try {
            try {
                primaryPool = storagePoolMgr.getStoragePool(pool.getType(), pool.getUuid());
            } catch (final CloudRuntimeException e) {
                if (e.getMessage().contains("not found")) {
                    primaryPool = storagePoolMgr.createStoragePool(pool.getUuid(), pool.getHost(), pool.getPort(), pool.getPath(), pool.getUserInfo(), pool.getType());
                } else {
                    return new CopyVolumeAnswer(command, false, e.getMessage(), null, null);
                }
            }

            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            final String volumeName = libvirtUtilitiesHelper.generateUUIDName();

            if (copyToSecondary) {
                final String destVolumeName = volumeName + ".qcow2";
                final KVMPhysicalDisk volume = primaryPool.getPhysicalDisk(command.getVolumePath());
                final String volumeDestPath = "/volumes/" + command.getVolumeId() + File.separator;

                secondaryStoragePool = storagePoolMgr.getStoragePoolByURI(secondaryStorageUrl);
                secondaryStoragePool.createFolder(volumeDestPath);
                storagePoolMgr.deleteStoragePool(secondaryStoragePool.getType(), secondaryStoragePool.getUuid());
                secondaryStoragePool = storagePoolMgr.getStoragePoolByURI(secondaryStorageUrl + volumeDestPath);
                storagePoolMgr.copyPhysicalDisk(volume, destVolumeName, secondaryStoragePool, 0);

                return new CopyVolumeAnswer(command, true, null, null, volumeName);
            } else {
                volumePath = "/volumes/" + command.getVolumeId() + File.separator;
                secondaryStoragePool = storagePoolMgr.getStoragePoolByURI(secondaryStorageUrl + volumePath);

                final KVMPhysicalDisk volume = secondaryStoragePool.getPhysicalDisk(command.getVolumePath() + ".qcow2");
                storagePoolMgr.copyPhysicalDisk(volume, volumeName, primaryPool, 0);

                return new CopyVolumeAnswer(command, true, null, null, volumeName);
            }
        } catch (final CloudRuntimeException e) {
            return new CopyVolumeAnswer(command, false, e.toString(), null, null);
        } finally {
            if (secondaryStoragePool != null) {
                storagePoolMgr.deleteStoragePool(secondaryStoragePool.getType(), secondaryStoragePool.getUuid());
            }
        }
    }
}