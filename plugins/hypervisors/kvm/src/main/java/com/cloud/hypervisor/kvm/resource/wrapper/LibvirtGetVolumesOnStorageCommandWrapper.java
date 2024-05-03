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
import com.cloud.agent.api.GetVolumesOnStorageAnswer;
import com.cloud.agent.api.GetVolumesOnStorageCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage.StoragePoolType;
import org.apache.cloudstack.storage.volume.VolumeOnStorageTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.commons.lang3.StringUtils;
import org.libvirt.LibvirtException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ResourceWrapper(handles = GetVolumesOnStorageCommand.class)
public final class LibvirtGetVolumesOnStorageCommandWrapper extends CommandWrapper<GetVolumesOnStorageCommand, Answer, LibvirtComputingResource> {

    static final List<StoragePoolType> STORAGE_POOL_TYPES_SUPPORTED_BY_QEMU_IMG = Arrays.asList(StoragePoolType.NetworkFilesystem,
            StoragePoolType.Filesystem, StoragePoolType.RBD);

    @Override
    public Answer execute(final GetVolumesOnStorageCommand command, final LibvirtComputingResource libvirtComputingResource) {

        final StorageFilerTO pool = command.getPool();
        final String volumePath = command.getVolumePath();
        final String keyword = command.getKeyword();

        final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
        final KVMStoragePool storagePool = storagePoolMgr.getStoragePool(pool.getType(), pool.getUuid(), true);

        if (StringUtils.isNotBlank(volumePath)) {
            return addVolumeByVolumePath(command, storagePool, volumePath);
        } else {
            return addAllVolumes(command, storagePool, keyword);
        }
    }

    private GetVolumesOnStorageAnswer addVolumeByVolumePath(final GetVolumesOnStorageCommand command, final KVMStoragePool storagePool, String volumePath) {
        List<VolumeOnStorageTO> volumes = new ArrayList<>();

        KVMPhysicalDisk disk = storagePool.getPhysicalDisk(volumePath);
        if (disk != null) {
            if (!volumePath.equals(disk.getPath()) && !volumePath.equals(disk.getName())) {
                String error = String.format("Volume path mismatch. Expected volume path (%s) is not the same as the actual name (%s) and path (%s)", volumePath, disk.getName(), disk.getPath());
                return new GetVolumesOnStorageAnswer(command, false, error);
            }
            if (!isDiskFormatSupported(disk)) {
                return new GetVolumesOnStorageAnswer(command, false, String.format("disk format %s is unsupported", disk.getFormat()));
            }
            Map<String, String> info = getDiskFileInfo(storagePool, disk, true);
            if (info == null) {
                return new GetVolumesOnStorageAnswer(command, false, "failed to get information of disk file. The disk might be locked or unsupported");
            }
            VolumeOnStorageTO volumeOnStorageTO = new VolumeOnStorageTO(Hypervisor.HypervisorType.KVM, disk.getName(), disk.getName(), disk.getPath(),
                    disk.getFormat().toString(), disk.getSize(), disk.getVirtualSize());
            if (disk.getQemuEncryptFormat() != null) {
                volumeOnStorageTO.setQemuEncryptFormat(disk.getQemuEncryptFormat().toString());
            }
            String backingFilePath = info.get(QemuImg.BACKING_FILE);
            if (StringUtils.isNotBlank(backingFilePath)) {
                volumeOnStorageTO.addDetail(VolumeOnStorageTO.Detail.BACKING_FILE, backingFilePath);
            }
            String backingFileFormat = info.get(QemuImg.BACKING_FILE_FORMAT);
            if (StringUtils.isNotBlank(backingFileFormat)) {
                volumeOnStorageTO.addDetail(VolumeOnStorageTO.Detail.BACKING_FILE_FORMAT, backingFileFormat);
            }
            String clusterSize = info.get(QemuImg.CLUSTER_SIZE);
            if (StringUtils.isNotBlank(clusterSize)) {
                volumeOnStorageTO.addDetail(VolumeOnStorageTO.Detail.CLUSTER_SIZE, clusterSize);
            }
            String fileFormat = info.get(QemuImg.FILE_FORMAT);
            if (StringUtils.isNotBlank(fileFormat)) {
                if (!fileFormat.equalsIgnoreCase(disk.getFormat().toString())) {
                    return new GetVolumesOnStorageAnswer(command, false, String.format("The file format is %s, but expected to be %s", fileFormat, disk.getFormat()));
                }
                volumeOnStorageTO.addDetail(VolumeOnStorageTO.Detail.FILE_FORMAT, fileFormat);
            }
            String encrypted = info.get(QemuImg.ENCRYPTED);
            if (StringUtils.isNotBlank(encrypted) && encrypted.equalsIgnoreCase("yes")) {
                volumeOnStorageTO.addDetail(VolumeOnStorageTO.Detail.IS_ENCRYPTED, String.valueOf(Boolean.TRUE));
            }
            Boolean isLocked = isDiskFileLocked(storagePool, disk);
            volumeOnStorageTO.addDetail(VolumeOnStorageTO.Detail.IS_LOCKED, String.valueOf(isLocked));

            volumes.add(volumeOnStorageTO);
        }
        return new GetVolumesOnStorageAnswer(command, volumes);
    }

    private GetVolumesOnStorageAnswer addAllVolumes(final GetVolumesOnStorageCommand command, final KVMStoragePool storagePool, String keyword) {
        List<VolumeOnStorageTO> volumes = new ArrayList<>();

        List<KVMPhysicalDisk> disks = storagePool.listPhysicalDisks();
        if (StringUtils.isNotBlank(keyword)) {
            disks = disks.stream().filter(disk -> disk.getName().contains(keyword)).collect(Collectors.toList());
        }
        disks.sort(Comparator.comparing(KVMPhysicalDisk::getName));
        for (KVMPhysicalDisk disk: disks) {
            if (!isDiskFormatSupported(disk)) {
                continue;
            }
            VolumeOnStorageTO volumeOnStorageTO = new VolumeOnStorageTO(Hypervisor.HypervisorType.KVM, disk.getName(), disk.getName(), disk.getPath(),
                    disk.getFormat().toString(), disk.getSize(), disk.getVirtualSize());
            if (disk.getQemuEncryptFormat() != null) {
                volumeOnStorageTO.setQemuEncryptFormat(disk.getQemuEncryptFormat().toString());
            }
            volumes.add(volumeOnStorageTO);
        }
        return new GetVolumesOnStorageAnswer(command, volumes);
    }

    private boolean isDiskFormatSupported(KVMPhysicalDisk disk) {
        return PhysicalDiskFormat.QCOW2.equals(disk.getFormat()) || PhysicalDiskFormat.RAW.equals(disk.getFormat());
    }

    private boolean isDiskFileLocked(KVMStoragePool pool, KVMPhysicalDisk disk) {
        Map<String, String> info = getDiskFileInfo(pool, disk, false);
        return info == null;
    }

    private Map<String, String> getDiskFileInfo(KVMStoragePool pool, KVMPhysicalDisk disk, boolean secure) {
        if (!STORAGE_POOL_TYPES_SUPPORTED_BY_QEMU_IMG.contains(pool.getType())) {
            return new HashMap<>(); // unknown
        }
        try {
            QemuImg qemu = new QemuImg(0);
            QemuImgFile qemuFile = new QemuImgFile(disk.getPath(), disk.getFormat());
            if (StoragePoolType.RBD.equals(pool.getType())) {
                String rbdDestFile = KVMPhysicalDisk.RBDStringBuilder(pool.getSourceHost(),
                        pool.getSourcePort(),
                        pool.getAuthUserName(),
                        pool.getAuthSecret(),
                        disk.getPath());
                qemuFile = new QemuImgFile(rbdDestFile, disk.getFormat());
            }
            return qemu.info(qemuFile, secure);
        } catch (QemuImgException | LibvirtException ex) {
            logger.error("Failed to get info of disk file: " + ex.getMessage());
            return null;
        }
    }
}
