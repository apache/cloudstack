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

import java.util.List;

import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
//import java.io.File;

import com.cloud.agent.api.storage.StorPoolDownloadVolumeCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.hypervisor.kvm.storage.StorPoolStorageAdaptor;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;

@ResourceWrapper(handles = StorPoolDownloadVolumeCommand.class)
public final class StorPoolDownloadVolumeCommandWrapper extends CommandWrapper<StorPoolDownloadVolumeCommand, CopyCmdAnswer, LibvirtComputingResource> {


    @Override
    public CopyCmdAnswer execute(final StorPoolDownloadVolumeCommand cmd, final LibvirtComputingResource libvirtComputingResource) {
        String dstPath = null;
        KVMStoragePool secondaryPool = null;

        try {
            final VolumeObjectTO src = cmd.getSourceTO();
            final VolumeObjectTO dst = cmd.getDestinationTO();
            final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
            SP_LOG("StorpoolDownloadVolumeCommandWrapper.execute: src=" + src.getPath() + " srcName=" + src.getName() + " dst=" + dst.getPath());

            final DataStoreTO srcDataStore = src.getDataStore();
            KVMPhysicalDisk srcDisk = null;

            if(srcDataStore instanceof NfsTO) {
                SP_LOG("StorpoolDownloadVolumeCommandWrapper.execute: srcIsNfsTO");

                final String tmplturl = srcDataStore.getUrl() + srcDataStore.getPathSeparator() + src.getPath();
                final int index = tmplturl.lastIndexOf("/");
                final String mountpoint = tmplturl.substring(0, index);
                String tmpltname = null;
                if (index < tmplturl.length() - 1) {
                    tmpltname = tmplturl.substring(index + 1);
                }

                secondaryPool = storagePoolMgr.getStoragePoolByURI(mountpoint);

                if (tmpltname == null) {
                    secondaryPool.refresh();
                    final List<KVMPhysicalDisk> disks = secondaryPool.listPhysicalDisks();
                    if (disks == null || disks.isEmpty()) {
                        SP_LOG("Failed to get volumes from pool: " + secondaryPool.getUuid());
                        return new CopyCmdAnswer("Failed to get volumes from pool: " + secondaryPool.getUuid());
                    }
                    for (final KVMPhysicalDisk disk : disks) {
                        if (disk.getName().endsWith("qcow2")) {
                            srcDisk = disk;
                            break;
                        }
                    }
                } else {
                    srcDisk = secondaryPool.getPhysicalDisk(tmpltname);
                }
            } else if(srcDataStore instanceof PrimaryDataStoreTO) {
               SP_LOG("SrcDisk is Primary Storage");
               PrimaryDataStoreTO primarySrc = (PrimaryDataStoreTO)srcDataStore;
               SP_LOG("StorpoolDownloadVolumeCommandWrapper.execute primarySrcPoolType=%s, uuid-%s ", primarySrc.getPoolType(), primarySrc.getUuid());
               final KVMStoragePoolManager poolMgr = libvirtComputingResource.getStoragePoolMgr();
               srcDisk = poolMgr.getPhysicalDisk(primarySrc.getPoolType(), srcDataStore.getUuid(), src.getPath());
               SP_LOG("PhysicalDisk: disk=%s", srcDisk );
            } else {
                return new CopyCmdAnswer("Don't know how to copy from " + srcDataStore.getClass().getName() + ", " + src.getPath() );
            }

            if (srcDisk == null) {
                SP_LOG("Failed to get src volume");
                return new CopyCmdAnswer("Failed to get src volume");
            }

            SP_LOG("got src path: " + srcDisk.getPath() + " srcSize " + srcDisk.getVirtualSize());

            String srcPath = null;
            boolean isRBDPool = srcDisk.getPool().getType() == StoragePoolType.RBD;
            if (isRBDPool) {
                KVMStoragePool srcPool = srcDisk.getPool();
                String rbdDestPath = srcPool.getSourceDir() + "/" + srcDisk.getName();
                srcPath = KVMPhysicalDisk.RBDStringBuilder(srcPool.getSourceHost(),
                        srcPool.getSourcePort(),
                        srcPool.getAuthUserName(),
                        srcPool.getAuthSecret(),
                        rbdDestPath);
            } else {
                srcPath = srcDisk.getPath();
            }
            final QemuImgFile srcFile = new QemuImgFile(srcPath, PhysicalDiskFormat.RAW);

            final QemuImg qemu = new QemuImg(cmd.getWaitInMillSeconds());
            StorPoolStorageAdaptor.resize( Long.toString(srcDisk.getVirtualSize()), dst.getPath());

            dstPath = dst.getPath();
            StorPoolStorageAdaptor.attachOrDetachVolume("attach", "volume", dstPath);

            final QemuImgFile dstFile = new QemuImgFile(dstPath, srcFile.getFormat());
            SP_LOG("SRC format=%s, DST format=%s",srcFile.getFormat(), dstFile.getFormat());
            qemu.convert(srcFile, dstFile);
            SP_LOG("StorpoolDownloadVolumeCommandWrapper VolumeObjectTO format=%s, hypervisor=%s", dst.getFormat(), dst.getHypervisorType());
            if (isRBDPool) {
                dst.setFormat(ImageFormat.QCOW2);
            }
            return new CopyCmdAnswer(dst);
        } catch (final Exception e) {
            final String error = "Failed to copy volume to primary: " + e.getMessage();
            SP_LOG(error);
            logger.debug(error);
            return new CopyCmdAnswer(cmd, e);
        } finally {
            if (dstPath != null) {
                StorPoolStorageAdaptor.attachOrDetachVolume("detach", "volume", dstPath);
            }

            if (secondaryPool != null) {
                try {
                    secondaryPool.delete();
                } catch (final Exception e) {
                    logger.debug("Failed to delete secondary storage", e);
                }
            }
        }
    }
}
