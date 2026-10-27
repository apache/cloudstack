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
import org.apache.cloudstack.storage.volume.VolumeOnStorageTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.libvirt.LibvirtException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ResourceWrapper(handles = CopyRemoteVolumeCommand.class)
public final class LibvirtCopyRemoteVolumeCommandWrapper extends CommandWrapper<CopyRemoteVolumeCommand, Answer, LibvirtComputingResource> {
    private static final List<Storage.StoragePoolType> STORAGE_POOL_TYPES_SUPPORTED = Arrays.asList(Storage.StoragePoolType.Filesystem, Storage.StoragePoolType.NetworkFilesystem);

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
            if (STORAGE_POOL_TYPES_SUPPORTED.contains(storageFilerTO.getType())) {
                String filename = libvirtComputingResource.copyVolume(srcIp, username, password, dstPath, srcFile, tmpPath, timeoutInSecs);
                logger.debug("Volume " + srcFile + " copy successful, copied to file: " + filename);
                final KVMPhysicalDisk vol = pool.getPhysicalDisk(filename);
                final String path = vol.getPath();
                try {
                    KVMPhysicalDisk.checkQcow2File(path);
                } catch (final CloudRuntimeException e) {
                    return new CopyRemoteVolumeAnswer(command, false, "", filename, 0, getVolumeDetails(pool, vol));
                }

                long size = KVMPhysicalDisk.getVirtualSizeFromFile(path);
                return new CopyRemoteVolumeAnswer(command, true, "", filename, size, getVolumeDetails(pool, vol));
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

    private Map<VolumeOnStorageTO.Detail, String> getVolumeDetails(KVMStoragePool pool, KVMPhysicalDisk disk) {
        Map<String, String> info = getDiskFileInfo(pool, disk, true);
        if (MapUtils.isEmpty(info)) {
            return null;
        }

        Map<VolumeOnStorageTO.Detail, String> volumeDetails = new HashMap<>();

        String backingFilePath = info.get(QemuImg.BACKING_FILE);
        if (StringUtils.isNotBlank(backingFilePath)) {
            volumeDetails.put(VolumeOnStorageTO.Detail.BACKING_FILE, backingFilePath);
        }
        String backingFileFormat = info.get(QemuImg.BACKING_FILE_FORMAT);
        if (StringUtils.isNotBlank(backingFileFormat)) {
            volumeDetails.put(VolumeOnStorageTO.Detail.BACKING_FILE_FORMAT, backingFileFormat);
        }
        String clusterSize = info.get(QemuImg.CLUSTER_SIZE);
        if (StringUtils.isNotBlank(clusterSize)) {
            volumeDetails.put(VolumeOnStorageTO.Detail.CLUSTER_SIZE, clusterSize);
        }
        String fileFormat = info.get(QemuImg.FILE_FORMAT);
        if (StringUtils.isNotBlank(fileFormat)) {
            volumeDetails.put(VolumeOnStorageTO.Detail.FILE_FORMAT, fileFormat);
        }
        String encrypted = info.get(QemuImg.ENCRYPTED);
        if (StringUtils.isNotBlank(encrypted) && encrypted.equalsIgnoreCase("yes")) {
            volumeDetails.put(VolumeOnStorageTO.Detail.IS_ENCRYPTED, String.valueOf(Boolean.TRUE));
        }
        Boolean isLocked = isDiskFileLocked(pool, disk);
        volumeDetails.put(VolumeOnStorageTO.Detail.IS_LOCKED, String.valueOf(isLocked));

        return volumeDetails;
    }

    private Map<String, String> getDiskFileInfo(KVMStoragePool pool, KVMPhysicalDisk disk, boolean secure) {
        if (!STORAGE_POOL_TYPES_SUPPORTED.contains(pool.getType())) {
            return new HashMap<>(); // unknown
        }
        try {
            QemuImg qemu = new QemuImg(0);
            QemuImgFile qemuFile = new QemuImgFile(disk.getPath(), disk.getFormat());
            return qemu.info(qemuFile, secure);
        } catch (QemuImgException | LibvirtException ex) {
            logger.error("Failed to get info of disk file: " + ex.getMessage());
            return null;
        }
    }

    private boolean isDiskFileLocked(KVMStoragePool pool, KVMPhysicalDisk disk) {
        Map<String, String> info = getDiskFileInfo(pool, disk, false);
        return info == null;
    }
}
