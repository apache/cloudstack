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
import java.io.IOException;

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.api.storage.LinstorBackupSnapshotCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Duration;
import org.libvirt.LibvirtException;

@ResourceWrapper(handles = LinstorBackupSnapshotCommand.class)
public final class LinstorBackupSnapshotCommandWrapper
    extends CommandWrapper<LinstorBackupSnapshotCommand, CopyCmdAnswer, LibvirtComputingResource>
{
    protected static Logger LOGGER = LogManager.getLogger(LinstorBackupSnapshotCommandWrapper.class);

    private String zfsSnapdev(boolean hide, String zfsUrl) {
        Script script = new Script("/usr/bin/zfs", Duration.millis(5000));
        script.add("set");
        script.add("snapdev=" + (hide ? "hidden" : "visible"));
        script.add(zfsUrl.substring(6));  // cutting zfs://
        return script.execute();
    }

    private String qemuShrink(String path, long sizeByte, long timeout) {
        Script qemuImg = new Script("qemu-img", Duration.millis(timeout));
        qemuImg.add("resize");
        qemuImg.add("--shrink");
        qemuImg.add(path);
        qemuImg.add("" + sizeByte);
        return qemuImg.execute();
    }

    static void cleanupSecondaryPool(final KVMStoragePool secondaryPool) {
        if (secondaryPool != null) {
            try {
                secondaryPool.delete();
            } catch (final Exception e) {
                LOGGER.debug("Failed to delete secondary storage", e);
            }
        }
    }

    private String convertImageToQCow2(
        final String srcPath,
        final SnapshotObjectTO dst,
        final KVMStoragePool secondaryPool,
        int waitMilliSeconds
        )
        throws LibvirtException, QemuImgException, IOException
    {
        final String dstDir = secondaryPool.getLocalPath() + File.separator + dst.getPath();
        FileUtils.forceMkdir(new File(dstDir));

        final String dstPath = dstDir + File.separator + dst.getName();
        final QemuImgFile srcFile = new QemuImgFile(srcPath, QemuImg.PhysicalDiskFormat.RAW);
        final QemuImgFile dstFile = new QemuImgFile(dstPath, QemuImg.PhysicalDiskFormat.QCOW2);

        // NOTE: the qemu img will also contain the drbd metadata at the end
        final QemuImg qemu = new QemuImg(waitMilliSeconds);
        qemu.convert(srcFile, dstFile);
        LOGGER.info("Backup snapshot " + srcFile + " to " + dstPath);
        return dstPath;
    }

    private SnapshotObjectTO setCorrectSnapshotSize(final SnapshotObjectTO dst, final String dstPath) {
        final File snapFile = new File(dstPath);
        final long size = snapFile.exists() ? snapFile.length() : 0;

        final SnapshotObjectTO snapshot = new SnapshotObjectTO();
        snapshot.setPath(dst.getPath() + File.separator + dst.getName());
        snapshot.setPhysicalSize(size);
        return snapshot;
    }

    @Override
    public CopyCmdAnswer execute(LinstorBackupSnapshotCommand cmd, LibvirtComputingResource serverResource)
    {
        LOGGER.debug("LinstorBackupSnapshotCommandWrapper: " + cmd.getSrcTO().getPath() + " -> " + cmd.getDestTO().getPath());
        final SnapshotObjectTO src = (SnapshotObjectTO) cmd.getSrcTO();
        final SnapshotObjectTO dst = (SnapshotObjectTO) cmd.getDestTO();
        KVMStoragePool secondaryPool = null;
        final KVMStoragePoolManager storagePoolMgr = serverResource.getStoragePoolMgr();
        KVMStoragePool linstorPool = storagePoolMgr.getStoragePool(Storage.StoragePoolType.Linstor, src.getDataStore().getUuid());
        boolean zfsHidden = false;
        String srcPath = src.getPath();

        if (linstorPool == null) {
            return new CopyCmdAnswer("Unable to get linstor storage pool from destination volume.");
        }

        final DataStoreTO dstDataStore = dst.getDataStore();
        if (!(dstDataStore instanceof NfsTO)) {
            return new CopyCmdAnswer("Backup Linstor snapshot: Only NFS secondary supported at present!");
        }

        try
        {
            // provide the linstor snapshot block device
            // on lvm thin this should already be there in /dev/mapper/vg-snapshotname
            // on zfs we need to unhide the snapshot block device
            LOGGER.info("Src: " + srcPath + " | " + src.getName());
            if (srcPath.startsWith("zfs://")) {
                zfsHidden = true;
                if (zfsSnapdev(false, srcPath) != null) {
                    return new CopyCmdAnswer("Unable to unhide zfs snapshot device.");
                }
                srcPath = "/dev/" + srcPath.substring(6);
            }

            secondaryPool = storagePoolMgr.getStoragePoolByURI(dstDataStore.getUrl());

            String dstPath = convertImageToQCow2(srcPath, dst, secondaryPool, cmd.getWaitInMillSeconds());

            // resize to real volume size, cutting of drbd metadata
            String result = qemuShrink(dstPath, src.getVolume().getSize(), cmd.getWaitInMillSeconds());
            if (result != null) {
                return new CopyCmdAnswer("qemu-img shrink failed: " + result);
            }
            LOGGER.info("Backup shrunk " + dstPath + " to actual size " + src.getVolume().getSize());

            SnapshotObjectTO snapshot = setCorrectSnapshotSize(dst, dstPath);
            return new CopyCmdAnswer(snapshot);
        } catch (final Exception e) {
            final String error = String.format("Failed to backup snapshot with id [%s] with a pool %s, due to %s",
                cmd.getSrcTO().getId(), cmd.getSrcTO().getDataStore().getUuid(), e.getMessage());
            LOGGER.error(error);
            return new CopyCmdAnswer(cmd, e);
        } finally {
            cleanupSecondaryPool(secondaryPool);
            if (zfsHidden) {
                zfsSnapdev(true, src.getPath());
            }
        }
    }
}
