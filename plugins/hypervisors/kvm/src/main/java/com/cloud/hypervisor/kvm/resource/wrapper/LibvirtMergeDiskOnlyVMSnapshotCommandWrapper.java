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

import com.cloud.agent.api.storage.MergeDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.storage.SnapshotMergeTreeTO;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.commons.lang3.StringUtils;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockJobInfo;
import org.libvirt.LibvirtException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@ResourceWrapper(handles = MergeDiskOnlyVmSnapshotCommand.class)
public class LibvirtMergeDiskOnlyVMSnapshotCommandWrapper extends CommandWrapper<MergeDiskOnlyVmSnapshotCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(MergeDiskOnlyVmSnapshotCommand command, LibvirtComputingResource serverResource) {
        VirtualMachine.State vmState = command.getVmState();

        try {
            if (VirtualMachine.State.Running.equals(vmState)) {
                return mergeDiskOnlySnapshotsForRunningVM(command, serverResource);
            }
            return mergeDiskOnlySnapshotsForStoppedVM(command, serverResource);
        } catch (LibvirtException | QemuImgException | CloudRuntimeException ex) {
            return new Answer(command, ex);
        }
    }

    protected Answer mergeDiskOnlySnapshotsForStoppedVM(MergeDiskOnlyVmSnapshotCommand cmd, LibvirtComputingResource resource) throws QemuImgException, LibvirtException {
        KVMStoragePoolManager storageManager = resource.getStoragePoolMgr();
        List<SnapshotMergeTreeTO> snapshotMergeTreeTOList = cmd.getSnapshotMergeTreeToList();

        logger.debug("Merging disk-only snapshots for stopped VM [{}] using the following Snapshot Merge Trees [{}].", cmd.getVmName(), snapshotMergeTreeTOList);
        long timeout = cmd.getWait() * 1000L;
        for (SnapshotMergeTreeTO snapshotMergeTreeTO : snapshotMergeTreeTOList) {
            if (timeout <= 0) {
                logger.warn("Disk-only VM snapshot for VM [{}] timed out. Consider increasing the timeout and try again later.", cmd.getVmName());
                return new Answer(cmd, false, null);
            }
            long startTime = System.currentTimeMillis();
            QemuImg qemuImg = new QemuImg(timeout);
            SnapshotObjectTO parentTo = (SnapshotObjectTO) snapshotMergeTreeTO.getParent();
            PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO) parentTo.getDataStore();
            KVMStoragePool storagePool = storageManager.getStoragePool(primaryDataStoreTO.getPoolType(), primaryDataStoreTO.getUuid());
            String childLocalPath = storagePool.getLocalPathFor(snapshotMergeTreeTO.getChild().getPath());
            Path childPath = Path.of(childLocalPath);
            QemuImgFile parent = new QemuImgFile(storagePool.getLocalPathFor(parentTo.getPath()), QemuImg.PhysicalDiskFormat.QCOW2);
            QemuImgFile child = new QemuImgFile(childLocalPath, QemuImg.PhysicalDiskFormat.QCOW2);

            if (!childPath.toFile().exists()) {
                logger.info("Volume [{}] was already merged in a previous attempt. Skipping merge for it.", parentTo.getVolume().getUuid());
            } else {
                logger.debug("Committing child delta [{}] into parent snapshot [{}].", parentTo, snapshotMergeTreeTO.getChild());
                qemuImg.commit(child, parent, true);
            }

            List<QemuImgFile> grandChildren = snapshotMergeTreeTO.getGrandChildren().stream()
                    .map(snapshotTo -> new QemuImgFile(storagePool.getLocalPathFor(snapshotTo.getPath()), QemuImg.PhysicalDiskFormat.QCOW2))
                    .collect(Collectors.toList());

            logger.debug("Rebasing grandChildren [{}] into parent at [{}].", grandChildren, parent.getFileName());
            for (QemuImgFile grandChild : grandChildren) {
                qemuImg.rebase(grandChild, parent, parent.getFormat().toString(), false);
            }

            logger.debug("Deleting child at [{}] as it is useless.", childLocalPath);
            try {
                Files.deleteIfExists(childPath);
            } catch (IOException e) {
                return new Answer(cmd, e);
            }
            timeout = timeout - (System.currentTimeMillis() - startTime);
        }
        return new Answer(cmd, true, null);
    }

    protected Answer mergeDiskOnlySnapshotsForRunningVM(MergeDiskOnlyVmSnapshotCommand cmd, LibvirtComputingResource resource) throws LibvirtException, QemuImgException {
        String vmName = cmd.getVmName();
        List<SnapshotMergeTreeTO> snapshotMergeTreeTOList = cmd.getSnapshotMergeTreeToList();

        LibvirtUtilitiesHelper libvirtUtilitiesHelper = resource.getLibvirtUtilitiesHelper();
        Connect conn = libvirtUtilitiesHelper.getConnection();
        Domain domain = resource.getDomain(conn, vmName);
        List<LibvirtVMDef.DiskDef> disks = resource.getDisks(conn, vmName);
        KVMStoragePoolManager storageManager = resource.getStoragePoolMgr();
        long timeout = cmd.getWait() * 1000L;

        logger.debug("Merging disk-only snapshots for running VM [{}] using the following Snapshot Merge Trees [{}].", vmName, snapshotMergeTreeTOList);

        for (SnapshotMergeTreeTO mergeTreeTO : snapshotMergeTreeTOList) {
            if (timeout <= 0) {
                logger.warn("Disk-only VM snapshot for VM [{}] timed out. Consider increasing the timeout and try again later.", vmName);
                return new Answer(cmd, false, null);
            }
            long startTime = System.currentTimeMillis();
            DataTO childTO = mergeTreeTO.getChild();
            SnapshotObjectTO parentSnapshotTO = (SnapshotObjectTO) mergeTreeTO.getParent();
            VolumeObjectTO volumeObjectTO = parentSnapshotTO.getVolume();
            KVMStoragePool storagePool = libvirtUtilitiesHelper.getPrimaryPoolFromDataTo(volumeObjectTO, storageManager);

            boolean active = DataObjectType.VOLUME.equals(childTO.getObjectType());

            LibvirtVMDef.DiskDef diskDef = getDiskDef(disks, volumeObjectTO, childTO, parentSnapshotTO);
            if (diskDef == null) {
                logger.warn("Volume [{}] does not match any record we have. This must be manually normalized.", volumeObjectTO.getUuid());
                return new Answer(cmd, false, null);
            }
            String label = diskDef.getDiskLabel();

            DomainBlockJobInfo jobInfo = domain.getBlockJobInfo(label, 0);
            if (jobInfo == null || jobInfo.type == 0 && jobInfo.end == 0 && jobInfo.cur == 0) {
                logger.debug("No block job found for disk [{}] of VM [{}]. Continuing the merge process.", label, vmName);
            } else {
                logger.debug("Found a block job for disk [{}] of VM [{}]. We will abort it to continue with the VM snapshot merge process.", label, vmName);
                domain.blockJobAbort(label, 0);
            }

            boolean volumeAlreadyMerged = isVolumeAlreadyMerged(diskDef, childTO);
            String parentSnapshotLocalPath = storagePool.getLocalPathFor(parentSnapshotTO.getPath());

            if (volumeAlreadyMerged) {
                logger.info("Volume [{}] was already merged in a previous attempt. Skipping merge for it.", volumeObjectTO.getUuid());
            } else {
                String childDeltaPath = storagePool.getLocalPathFor(childTO.getPath());
                logger.debug("Found label [{}] for [{}]. Will merge delta at [{}] into delta at [{}].", label, volumeObjectTO, parentSnapshotLocalPath, childDeltaPath);
                resource.mergeSnapshotIntoBaseFile(domain, label, parentSnapshotLocalPath, childDeltaPath, active, childTO.getPath(), volumeObjectTO, conn, timeout);
            }

            QemuImgFile parent = new QemuImgFile(parentSnapshotLocalPath, QemuImg.PhysicalDiskFormat.QCOW2);
            List<QemuImgFile> grandChildren = mergeTreeTO.getGrandChildren().stream()
                    .map(snapshotTo -> new QemuImgFile(storagePool.getLocalPathFor(snapshotTo.getPath()), QemuImg.PhysicalDiskFormat.QCOW2))
                    .collect(Collectors.toList());

            logger.debug("Rebasing grandChildren [{}] into parent at [{}].", grandChildren, parentSnapshotLocalPath);
            for (QemuImgFile grandChild : grandChildren) {
                QemuImg qemuImg = new QemuImg(timeout);
                qemuImg.rebase(grandChild, parent, parent.getFormat().toString(), false);
            }
            timeout = timeout - (System.currentTimeMillis() - startTime);
        }

        return new Answer(cmd, true, null);
    }

    protected boolean isVolumeAlreadyMerged(LibvirtVMDef.DiskDef diskDef, DataTO childTO) {
        if (diskDef.getDiskPath().contains(childTO.getPath())) {
            return false;
        }
        boolean isVolumeAlreadyMerged = true;
        for (String filePath : diskDef.getBackingStoreList()) {
            if (filePath.contains(childTO.getPath())) {
                isVolumeAlreadyMerged = false;
            }
        }
        return isVolumeAlreadyMerged;
    }

    protected LibvirtVMDef.DiskDef getDiskDef(List<LibvirtVMDef.DiskDef> disks, VolumeObjectTO volumeObjectTO, DataTO childTO, SnapshotObjectTO parentSnapshotTO) {
        return disks.stream().filter(disk -> StringUtils.contains(disk.getDiskPath(), volumeObjectTO.getPath()) ||
                StringUtils.contains(disk.getDiskPath(), childTO.getPath()) ||
                StringUtils.contains(disk.getDiskPath(), parentSnapshotTO.getPath()))
                .findFirst()
                .orElse(null);
    }
}
