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

import static com.cloud.hypervisor.kvm.storage.StorPoolStorageAdaptor.SP_LOG;

import java.io.File;

import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.log4j.Logger;

import com.cloud.agent.api.storage.StorPoolCopyVolumeToSecondaryCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.hypervisor.kvm.storage.StorPoolStorageAdaptor;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles = StorPoolCopyVolumeToSecondaryCommand.class)
public final class StorPoolCopyVolumeToSecondaryCommandWrapper extends CommandWrapper<StorPoolCopyVolumeToSecondaryCommand, CopyCmdAnswer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(StorPoolCopyVolumeToSecondaryCommandWrapper.class);

    @Override
    public CopyCmdAnswer execute(final StorPoolCopyVolumeToSecondaryCommand cmd, final LibvirtComputingResource libvirtComputingResource) {
        String srcPath = null;
        KVMStoragePool secondaryPool = null;

        try {
            final VolumeObjectTO src = cmd.getSourceTO();
            final VolumeObjectTO dst = cmd.getDestinationTO();
            final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
            final String destVolumePath = dst.getPath();

            SP_LOG("StorpoolCopyVolumeToSecondaryCommandWrapper.execute: src=" + src.getPath() + "dst=" + dst.getPath());

            StorPoolStorageAdaptor.attachOrDetachVolume("attach", "snapshot", src.getPath());
            srcPath = src.getPath();

            final QemuImgFile srcFile = new QemuImgFile(srcPath, PhysicalDiskFormat.RAW);

            final DataStoreTO dstDataStore = dst.getDataStore();

            final KVMStoragePoolManager poolMgr = libvirtComputingResource.getStoragePoolMgr();
            SP_LOG("StorpoolCopyVolumeToSecondaryCommandWrapper.execute: KVMStoragePoolManager " + poolMgr);
            KVMStoragePool destPool;
            if( dstDataStore instanceof NfsTO ) {
                destPool = storagePoolMgr.getStoragePoolByURI(dstDataStore.getUrl());
                destPool.createFolder(destVolumePath);
                storagePoolMgr.deleteStoragePool(destPool.getType(), destPool.getUuid());
                destPool = storagePoolMgr.getStoragePoolByURI(dstDataStore.getUrl() + File.separator + destVolumePath);
                SP_LOG("StorpoolCopyVolumeToSecondaryCommandWrapper.execute: Nfs destPool=%s ",destPool);
            } else if( dstDataStore instanceof PrimaryDataStoreTO ) {
                PrimaryDataStoreTO primaryDst = (PrimaryDataStoreTO)dstDataStore;
                destPool = poolMgr.getStoragePool(primaryDst.getPoolType(), dstDataStore.getUuid());
                SP_LOG("StorpoolCopyVolumeToSecondaryCommandWrapper.execute: not Nfs destPool=%s " ,destPool);
            } else {
                return new CopyCmdAnswer("Don't know how to copy to " + dstDataStore.getClass().getName() + ", " + dst.getPath() );
            }
            SP_LOG("StorpoolCopyVolumeToSecondaryCommandWrapper.execute: dstName=%s, dstProvisioningType=%s, srcSize=%s, dstUUID=%s, srcUUID=%s " ,dst.getName(), dst.getProvisioningType(), src.getSize(),dst.getUuid(), src.getUuid());

                KVMPhysicalDisk newDisk = destPool.createPhysicalDisk(dst.getUuid(), dst.getProvisioningType(), src.getSize());
                SP_LOG("NewDisk path=%s, uuid=%s ", newDisk.getPath(), dst.getUuid());
                String destPath = newDisk.getPath();
                newDisk.setPath(dst.getUuid());

                PhysicalDiskFormat destFormat = newDisk.getFormat();
                SP_LOG("StorpoolCopyVolumeToSecondaryCommandWrapper.execute: KVMPhysicalDisk name=%s, format=%s, path=%s, destinationPath=%s " , newDisk.getName(), newDisk.getFormat(), newDisk.getPath(), destPath);
                QemuImgFile destFile = new QemuImgFile(destPath, destFormat);
                QemuImg qemu = new QemuImg(cmd.getWaitInMillSeconds());
                qemu.convert(srcFile, destFile);

                final File file = new File(destPath);
                final long size = file.exists() ? file.length() : 0;
                dst.setPath(destVolumePath + File.separator + dst.getUuid());
                dst.setSize(size);

            return new CopyCmdAnswer(dst);
        } catch (final Exception e) {
            final String error = "Failed to copy volume to secondary storage: " + e.getMessage();
            s_logger.debug(error);
            return new CopyCmdAnswer(error);
        } finally {
            if (srcPath != null) {
                StorPoolStorageAdaptor.attachOrDetachVolume("detach", "snapshot", srcPath);
            }

            if (secondaryPool != null) {
                try {
                    SP_LOG("StorpoolCopyVolumeToSecondaryCommandWrapper.execute: secondaryPool=%s " , secondaryPool);
                    secondaryPool.delete();
                } catch (final Exception e) {
                    s_logger.debug("Failed to delete secondary storage", e);
                }
            }
        }
    }
}
