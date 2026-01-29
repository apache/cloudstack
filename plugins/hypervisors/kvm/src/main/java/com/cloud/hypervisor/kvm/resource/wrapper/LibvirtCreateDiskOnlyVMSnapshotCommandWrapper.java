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
import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.agent.api.storage.CreateDiskOnlyVmSnapshotAnswer;
import com.cloud.agent.api.storage.CreateDiskOnlyVmSnapshotCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ResourceWrapper(handles = CreateDiskOnlyVmSnapshotCommand.class)
public class LibvirtCreateDiskOnlyVMSnapshotCommandWrapper extends CommandWrapper<CreateDiskOnlyVmSnapshotCommand, Answer, LibvirtComputingResource> {

    private static final String SNAPSHOT_XML = "<domainsnapshot>\n" +
            "<name>%s</name>\n" +
            "<memory snapshot='no'/>\n" +
            "<disks> \n" +
            "%s" +
            "</disks> \n" +
            "</domainsnapshot>";

    private static final String TAG_DISK_SNAPSHOT = "<disk name='%s' snapshot='external'>\n" +
            "<source file='%s'/>\n" +
            "</disk>\n";

    @Override
    public Answer execute(CreateDiskOnlyVmSnapshotCommand cmd, LibvirtComputingResource resource) {
        VirtualMachine.State state = cmd.getVmState();

        if (VirtualMachine.State.Running.equals(state)) {
            return takeDiskOnlyVmSnapshotOfRunningVm(cmd, resource);
        }

        return takeDiskOnlyVmSnapshotOfStoppedVm(cmd, resource);
    }

    protected Answer takeDiskOnlyVmSnapshotOfRunningVm(CreateDiskOnlyVmSnapshotCommand cmd, LibvirtComputingResource resource) {
        String vmName = cmd.getVmName();
        logger.info("Taking disk-only VM snapshot of running VM [{}].", vmName);

        Domain dm = null;
        try {
            LibvirtUtilitiesHelper libvirtUtilitiesHelper = resource.getLibvirtUtilitiesHelper();
            Connect conn = libvirtUtilitiesHelper.getConnection();
            List<VolumeObjectTO> volumeObjectTOS = cmd.getVolumeTOs();
            List<LibvirtVMDef.DiskDef> disks = resource.getDisks(conn, vmName);

            dm = resource.getDomain(conn, vmName);

            if (dm == null) {
                return new CreateDiskOnlyVmSnapshotAnswer(cmd, false, String.format("Creation of disk-only VM Snapshot failed as we could not find the VM [%s].", vmName), null);
            }

            VMSnapshotTO target = cmd.getTarget();
            Pair<String, Map<String, Pair<Long, String>>> snapshotXmlAndVolumeToNewPathMap = createSnapshotXmlAndNewVolumePathMap(volumeObjectTOS, disks, target, resource);

            dm.snapshotCreateXML(snapshotXmlAndVolumeToNewPathMap.first(), getFlagsToUseForRunningVmSnapshotCreation(target));

            return new CreateDiskOnlyVmSnapshotAnswer(cmd, true, null, snapshotXmlAndVolumeToNewPathMap.second());
        } catch (LibvirtException e) {
            String errorMsg = String.format("Creation of disk-only VM snapshot for VM [%s] failed due to %s.", vmName, e.getMessage());
            logger.error(errorMsg, e);
            if (e.getMessage().contains("QEMU guest agent is not connected")) {
                errorMsg = "QEMU guest agent is not connected. If the VM has been recently started, it might connect soon. Otherwise the VM does not have the" +
                        " guest agent installed; thus the QuiesceVM parameter is not supported.";
                return new CreateDiskOnlyVmSnapshotAnswer(cmd, false, errorMsg, null);
            }
            return new CreateDiskOnlyVmSnapshotAnswer(cmd, false, e.getMessage(), null);
        } finally {
            if (dm != null) {
                try {
                    dm.free();
                } catch (LibvirtException l) {
                    logger.trace("Ignoring libvirt error.", l);
                }
            }
        }
    }

    protected Answer takeDiskOnlyVmSnapshotOfStoppedVm(CreateDiskOnlyVmSnapshotCommand cmd, LibvirtComputingResource resource) {
        String vmName = cmd.getVmName();
        logger.info("Taking disk-only VM snapshot of stopped VM [{}].", vmName);

        Map<String, Pair<Long, String>> mapVolumeToSnapshotSizeAndNewVolumePath = new HashMap<>();

        List<VolumeObjectTO> volumeObjectTos = cmd.getVolumeTOs();
        KVMStoragePoolManager storagePoolMgr = resource.getStoragePoolMgr();
        try {
            for (VolumeObjectTO volumeObjectTO : volumeObjectTos) {
                PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO) volumeObjectTO.getDataStore();
                KVMStoragePool kvmStoragePool = storagePoolMgr.getStoragePool(primaryDataStoreTO.getPoolType(), primaryDataStoreTO.getUuid());

                String snapshotPath = UUID.randomUUID().toString();
                String snapshotFullPath = kvmStoragePool.getLocalPathFor(snapshotPath);
                QemuImgFile newDelta = new QemuImgFile(snapshotFullPath, QemuImg.PhysicalDiskFormat.QCOW2);

                String currentDeltaFullPath = kvmStoragePool.getLocalPathFor(volumeObjectTO.getPath());
                QemuImgFile currentDelta = new QemuImgFile(currentDeltaFullPath, QemuImg.PhysicalDiskFormat.QCOW2);

                QemuImg qemuImg = new QemuImg(0);

                logger.debug("Creating new delta for volume [{}] as part of the disk-only VM snapshot process for VM [{}].", volumeObjectTO.getUuid(), vmName);
                qemuImg.create(newDelta, currentDelta);

                mapVolumeToSnapshotSizeAndNewVolumePath.put(volumeObjectTO.getUuid(), new Pair<>(getFileSize(currentDeltaFullPath), snapshotPath));
            }
        } catch (LibvirtException | QemuImgException e) {
            logger.error("Exception while creating disk-only VM snapshot for VM [{}]. Deleting leftover deltas.", vmName, e);
            for (VolumeObjectTO volumeObjectTO : volumeObjectTos) {
                Pair<Long, String> volSizeAndNewPath = mapVolumeToSnapshotSizeAndNewVolumePath.get(volumeObjectTO.getUuid());
                PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO) volumeObjectTO.getDataStore();
                KVMStoragePool kvmStoragePool = storagePoolMgr.getStoragePool(primaryDataStoreTO.getPoolType(), primaryDataStoreTO.getUuid());

                if (volSizeAndNewPath == null) {
                    continue;
                }
                try {
                    Files.deleteIfExists(Path.of(kvmStoragePool.getLocalPathFor(volSizeAndNewPath.second())));
                } catch (IOException ex) {
                    logger.warn("Tried to delete leftover snapshot at [{}] failed.", volSizeAndNewPath.second(), ex);
                }
            }
            return new Answer(cmd, e);
        }

        return new CreateDiskOnlyVmSnapshotAnswer(cmd, true, null, mapVolumeToSnapshotSizeAndNewVolumePath);
    }

    protected int getFlagsToUseForRunningVmSnapshotCreation(VMSnapshotTO target) {
        int flags = target.getQuiescevm() ? Domain.SnapshotCreateFlags.QUIESCE : 0;
        flags += Domain.SnapshotCreateFlags.DISK_ONLY +
                Domain.SnapshotCreateFlags.ATOMIC +
                Domain.SnapshotCreateFlags.NO_METADATA;
        return flags;
    }

    protected Pair<String, Map<String, Pair<Long, String>>> createSnapshotXmlAndNewVolumePathMap(List<VolumeObjectTO> volumeObjectTOS, List<LibvirtVMDef.DiskDef> disks, VMSnapshotTO target, LibvirtComputingResource resource) {
        StringBuilder stringBuilder = new StringBuilder();
        Map<String, Pair<Long, String>> volumeObjectToNewPathMap = new HashMap<>();

        for (VolumeObjectTO volumeObjectTO : volumeObjectTOS) {
            LibvirtVMDef.DiskDef diskdef = resource.getDiskWithPathOfVolumeObjectTO(disks, volumeObjectTO);
            String newPath = UUID.randomUUID().toString();
            stringBuilder.append(String.format(TAG_DISK_SNAPSHOT, diskdef.getDiskLabel(), resource.getSnapshotTemporaryPath(diskdef.getDiskPath(), newPath)));

            long snapSize = getFileSize(diskdef.getDiskPath());

            volumeObjectToNewPathMap.put(volumeObjectTO.getUuid(), new Pair<>(snapSize, newPath));
        }

        String snapshotXml = String.format(SNAPSHOT_XML, target.getSnapshotName(), stringBuilder);
        return new Pair<>(snapshotXml, volumeObjectToNewPathMap);
    }

    protected long getFileSize(String path) {
        return new File(path).length();
    }
}
