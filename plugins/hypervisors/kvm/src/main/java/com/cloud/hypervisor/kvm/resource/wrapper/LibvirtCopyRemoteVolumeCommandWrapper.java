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
import com.cloud.agent.api.CopyRemoteVolumeAnswer;
import com.cloud.agent.api.CopyRemoteVolumeCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.libvirt.LibvirtException;

import java.util.Map;

@ResourceWrapper(handles = CopyRemoteVolumeCommand.class)
public final class LibvirtCopyRemoteVolumeCommandWrapper extends CommandWrapper<CopyRemoteVolumeCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final CopyRemoteVolumeCommand command, final LibvirtComputingResource libvirtComputingResource) {
        String srcIp = command.getRemoteIp();
        String username = command.getUsername();
        String password = command.getPassword();
        String srcFile = command.getSrcFile();
        StorageFilerTO storageFilerTO = command.getStorageFilerTO();
        String tmpPath = command.getTmpPath();
        KVMStoragePoolManager poolMgr = libvirtComputingResource.getStoragePoolMgr();
        KVMStoragePool pool = poolMgr.getStoragePool(storageFilerTO.getType(), storageFilerTO.getUuid());
        String dstPath = pool.getLocalPath();
        int timeoutInSecs = command.getWait();

        try {
            if (storageFilerTO.getType() == Storage.StoragePoolType.Filesystem ||
                    storageFilerTO.getType() == Storage.StoragePoolType.NetworkFilesystem) {
                String filename = libvirtComputingResource.copyVolume(srcIp, username, password, dstPath, srcFile, tmpPath, timeoutInSecs);
                logger.debug("Volume " + srcFile + " copy successful, copied to file: " + filename);
                final KVMPhysicalDisk vol = pool.getPhysicalDisk(filename);
                final String path = vol.getPath();
                long size = getVirtualSizeFromFile(path);
                return new CopyRemoteVolumeAnswer(command, "", filename, size);
            } else {
                String msg = "Unsupported storage pool type: " + storageFilerTO.getType().toString() + ", only local and NFS pools are supported";
                return new Answer(command, false, msg);
            }
        } catch (final Exception e) {
            logger.error("Error while copying volume file from remote host: " + e.getMessage(), e);
            String msg = "Failed to copy volume due to: " + e.getMessage();
            return new Answer(command, false, msg);
        }
    }

    private long getVirtualSizeFromFile(String path) {
        try {
            QemuImg qemu = new QemuImg(0);
            QemuImgFile qemuFile = new QemuImgFile(path);
            Map<String, String> info = qemu.info(qemuFile);
            if (info.containsKey(QemuImg.VIRTUAL_SIZE)) {
                return Long.parseLong(info.get(QemuImg.VIRTUAL_SIZE));
            } else {
                throw new CloudRuntimeException("Unable to determine virtual size of volume at path " + path);
            }
        } catch (QemuImgException | LibvirtException ex) {
            throw new CloudRuntimeException("Error when inspecting volume at path " + path, ex);
        }
    }
}
