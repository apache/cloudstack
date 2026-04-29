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
package com.cloud.hypervisor.kvm.resource.wrapper;

import java.io.File;

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.api.storage.LinstorRevertBackupSnapshotCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.datastore.util.LinstorUtil;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.joda.time.Duration;
import org.libvirt.LibvirtException;

@ResourceWrapper(handles = LinstorRevertBackupSnapshotCommand.class)
public final class LinstorRevertBackupSnapshotCommandWrapper
    extends CommandWrapper<LinstorRevertBackupSnapshotCommand, CopyCmdAnswer, LibvirtComputingResource>
{

    private void convertQCow2ToRAW(
            KVMStoragePool pool, final String srcPath, final String dstUuid, int waitMilliSeconds)
        throws LibvirtException, QemuImgException
    {
        final String dstPath = pool.getPhysicalDisk(dstUuid).getPath();
        final QemuImgFile srcQemuFile = new QemuImgFile(
            srcPath, QemuImg.PhysicalDiskFormat.QCOW2);
        boolean zeroedDevice = LinstorUtil.resourceSupportZeroBlocks(pool, LinstorUtil.RSC_PREFIX + dstUuid);
        if (zeroedDevice)
        {
            // blockdiscard the device to ensure the device is filled with zeroes
            Script blkDiscardScript = new Script("blkdiscard", Duration.millis(waitMilliSeconds));
            blkDiscardScript.add("-f");
            blkDiscardScript.add(dstPath);
            blkDiscardScript.execute();
        }
        final QemuImg qemu = new QemuImg(waitMilliSeconds, zeroedDevice, true);
        final QemuImgFile dstFile = new QemuImgFile(dstPath, QemuImg.PhysicalDiskFormat.RAW);
        qemu.convert(srcQemuFile, dstFile);
    }

    @Override
    public CopyCmdAnswer execute(LinstorRevertBackupSnapshotCommand cmd, LibvirtComputingResource serverResource)
    {
        logger.debug("LinstorRevertBackupSnapshotCommandWrapper: " + cmd.getSrcTO().getPath() + " -> " + cmd.getDestTO().getPath());
        final SnapshotObjectTO src = (SnapshotObjectTO) cmd.getSrcTO();
        final VolumeObjectTO dst = (VolumeObjectTO) cmd.getDestTO();
        KVMStoragePool secondaryPool = null;
        final KVMStoragePoolManager storagePoolMgr = serverResource.getStoragePoolMgr();
        KVMStoragePool linstorPool = storagePoolMgr.getStoragePool(Storage.StoragePoolType.Linstor, dst.getDataStore().getUuid());

        if (linstorPool == null) {
            return new CopyCmdAnswer("Unable to get linstor storage pool from destination volume.");
        }

        try
        {
            final DataStoreTO srcDataStore = src.getDataStore();
            File srcFile = new File(src.getPath());
            secondaryPool = storagePoolMgr.getStoragePoolByURI(
                srcDataStore.getUrl() + File.separator + srcFile.getParent());

            convertQCow2ToRAW(
                linstorPool,
                secondaryPool.getLocalPath() + File.separator + srcFile.getName(),
                dst.getPath(),
                cmd.getWaitInMillSeconds());

            final VolumeObjectTO dstVolume = new VolumeObjectTO();
            dstVolume.setPath(dst.getPath());
            return new CopyCmdAnswer(dstVolume);
        } catch (final Exception e) {
            final String error = String.format("Failed to revert snapshot with id [%s] with a pool %s, due to %s",
                cmd.getSrcTO().getId(), cmd.getSrcTO().getDataStore().getUuid(), e.getMessage());
            logger.error(error);
            return new CopyCmdAnswer(cmd, e);
        } finally {
            LinstorBackupSnapshotCommandWrapper.cleanupSecondaryPool(secondaryPool);
        }
    }
}
