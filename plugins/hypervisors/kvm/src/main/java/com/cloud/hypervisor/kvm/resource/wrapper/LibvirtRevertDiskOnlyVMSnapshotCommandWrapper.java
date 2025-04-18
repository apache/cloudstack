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
import com.cloud.agent.api.storage.RevertDiskOnlyVmSnapshotAnswer;
import com.cloud.agent.api.storage.RevertDiskOnlyVmSnapshotCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.libvirt.LibvirtException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@ResourceWrapper(handles = RevertDiskOnlyVmSnapshotCommand.class)
public class LibvirtRevertDiskOnlyVMSnapshotCommandWrapper extends CommandWrapper<RevertDiskOnlyVmSnapshotCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(RevertDiskOnlyVmSnapshotCommand cmd, LibvirtComputingResource resource) {
        List<SnapshotObjectTO> snapshotObjectTos = cmd.getSnapshotObjectTos();

        String vmName = cmd.getVmName();
        logger.info("Reverting disk-only VM snapshot of VM [{}]", vmName);

        KVMStoragePoolManager storagePoolMgr = resource.getStoragePoolMgr();
        LibvirtUtilitiesHelper libvirtUtilitiesHelper = resource.getLibvirtUtilitiesHelper();

        HashMap<SnapshotObjectTO, String> snapshotToNewDeltaPath = new HashMap<>();
        try {
            for (SnapshotObjectTO snapshotObjectTo : snapshotObjectTos) {
                KVMStoragePool kvmStoragePool = libvirtUtilitiesHelper.getPrimaryPoolFromDataTo(snapshotObjectTo, storagePoolMgr);

                String deltaPath = libvirtUtilitiesHelper.generateUUIDName();
                String deltaFullPath = kvmStoragePool.getLocalPathFor(deltaPath);
                QemuImgFile newDelta = new QemuImgFile(deltaFullPath, QemuImg.PhysicalDiskFormat.QCOW2);

                String snapshotFullPath = kvmStoragePool.getLocalPathFor(snapshotObjectTo.getPath());
                QemuImgFile currentDelta = new QemuImgFile(snapshotFullPath, QemuImg.PhysicalDiskFormat.QCOW2);

                QemuImg qemuImg = new QemuImg(0);

                logger.debug("Creating new delta for volume [{}] as part of the disk-only VM snapshot revert process for VM [{}].", snapshotObjectTo.getVolume().getUuid(), vmName);
                qemuImg.create(newDelta, currentDelta);
                snapshotToNewDeltaPath.put(snapshotObjectTo, deltaPath);
            }
        } catch (LibvirtException | QemuImgException e) {
            logger.error("Exception while reverting disk-only VM snapshot for VM [{}]. Deleting leftover deltas.", vmName, e);
            for (SnapshotObjectTO snapshotObjectTo : snapshotObjectTos) {
                String newPath = snapshotToNewDeltaPath.get(snapshotObjectTo);

                if (newPath == null) {
                    continue;
                }

                KVMStoragePool kvmStoragePool = libvirtUtilitiesHelper.getPrimaryPoolFromDataTo(snapshotObjectTo, storagePoolMgr);
                try {
                    Files.deleteIfExists(Path.of(kvmStoragePool.getLocalPathFor(newPath)));
                } catch (IOException ex) {
                    logger.warn("Tried to delete leftover snapshot at [{}] failed.", newPath, ex);
                }
            }
            return new Answer(cmd, e);
        }

        List<VolumeObjectTO> volumeObjectTos = new ArrayList<>();
        for (SnapshotObjectTO snapshotObjectTo : snapshotObjectTos) {
            VolumeObjectTO volumeObjectTo = snapshotObjectTo.getVolume();

            KVMStoragePool kvmStoragePool = libvirtUtilitiesHelper.getPrimaryPoolFromDataTo(snapshotObjectTo, storagePoolMgr);

            try {
                Files.deleteIfExists(Path.of(kvmStoragePool.getLocalPathFor(volumeObjectTo.getPath())));
            } catch (IOException ex) {
                logger.warn("Got an error while trying to delete old volume delta [{}], there might be trash on storage [{}].", volumeObjectTo.getPath(),
                        kvmStoragePool.getUuid());
            }
            volumeObjectTo.setPath(snapshotToNewDeltaPath.get(snapshotObjectTo));
            volumeObjectTos.add(volumeObjectTo);
        }

        return new RevertDiskOnlyVmSnapshotAnswer(cmd, volumeObjectTos);
    }
}
