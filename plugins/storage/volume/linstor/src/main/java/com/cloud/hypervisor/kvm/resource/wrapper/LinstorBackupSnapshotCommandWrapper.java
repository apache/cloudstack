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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.apache.cloudstack.utils.cryptsetup.KeyFile;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.cloudstack.utils.qemu.QemuObject;
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

    private static String zfsDatasetName(String zfsFullSnapshotUrl) {
        String zfsFullPath = zfsFullSnapshotUrl.substring(6);
        int atPos = zfsFullPath.indexOf('@');
        return atPos >= 0 ? zfsFullPath.substring(0, atPos) : zfsFullPath;
    }

    private String zfsSnapdev(boolean hide, String zfsUrl) {
        Script script = new Script("zfs", Duration.millis(5000));
        script.add("set");
        script.add("snapdev=" + (hide ? "hidden" : "visible"));
        script.add(zfsDatasetName(zfsUrl));  // cutting zfs:// and @snapshotname
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
        final byte[] passphrase,
        int waitMilliSeconds
        )
        throws LibvirtException, QemuImgException, IOException
    {
        final String dstDir = secondaryPool.getLocalPath() + File.separator + dst.getPath();
        FileUtils.forceMkdir(new File(dstDir));

        final String dstPath = dstDir + File.separator + dst.getName();
        final QemuImgFile srcFile = new QemuImgFile(srcPath, QemuImg.PhysicalDiskFormat.RAW);
        final QemuImgFile dstFile = new QemuImgFile(dstPath, QemuImg.PhysicalDiskFormat.QCOW2);

        final QemuImg qemu = new QemuImg(waitMilliSeconds);
        if (passphrase != null && passphrase.length > 0) {
            // Encrypted volumes are backed up from their decrypted DRBD device, so the snapshot
            // data here is plaintext. Encrypt the destination qcow2 with the volume's passphrase
            // (LUKS), so the snapshot is not stored in clear text on secondary storage.
            try (KeyFile keyFile = new KeyFile(passphrase)) {
                final Map<String, String> options = new HashMap<>();
                final List<QemuObject> qemuObjects = new ArrayList<>();
                qemuObjects.add(QemuObject.prepareSecretForQemuImg(QemuImg.PhysicalDiskFormat.QCOW2,
                    QemuObject.EncryptFormat.LUKS, keyFile.toString(), "sec0", options));
                qemu.convert(srcFile, dstFile, options, qemuObjects, null, true);
            }
        } else {
            // NOTE: the qemu img will also contain the drbd metadata at the end
            qemu.convert(srcFile, dstFile);
        }
        LOGGER.info("Backup snapshot '{}' to '{}'", srcPath, dstPath);
        return dstPath;
    }

    private SnapshotObjectTO setCorrectSnapshotSize(final SnapshotObjectTO dst, final String dstPath) {
        final File snapFile = new File(dstPath);
        long size;
        if (snapFile.exists()) {
            size = snapFile.length();
        } else {
            LOGGER.warn("Snapshot file {} does not exist. Reporting size 0", dstPath);
            size = 0;
        }

        dst.setPath(dst.getPath() + File.separator + dst.getName());
        dst.setPhysicalSize(size);
        return dst;
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
                if (zfsSnapdev(false, src.getPath()) != null) {
                    return new CopyCmdAnswer("Unable to unhide zfs snapshot device.");
                }
                srcPath = "/dev/zvol/" + srcPath.substring(6);
            }

            secondaryPool = storagePoolMgr.getStoragePoolByURI(dstDataStore.getUrl());

            final byte[] passphrase = src.getVolume() != null ? src.getVolume().getPassphrase() : null;
            final boolean encrypted = passphrase != null && passphrase.length > 0;

            String dstPath = convertImageToQCow2(srcPath, dst, secondaryPool, passphrase, cmd.getWaitInMillSeconds());

            if (!encrypted) {
                // resize to real volume size, cutting of drbd metadata
                // For encrypted volumes the source is the decrypted DRBD device (already net-sized,
                // no drbd metadata to cut); shrinking an encrypted qcow2 would also need the secret.
                String result = qemuShrink(dstPath, src.getVolume().getSize(), cmd.getWaitInMillSeconds());
                if (result != null) {
                    return new CopyCmdAnswer("qemu-img shrink failed: " + result);
                }
                LOGGER.info("Backup shrunk " + dstPath + " to actual size " + src.getVolume().getSize());
            }

            SnapshotObjectTO snapshot = setCorrectSnapshotSize(dst, dstPath);
            LOGGER.info("Actual file size for '{}' is {}", dstPath, snapshot.getPhysicalSize());
            return new CopyCmdAnswer(snapshot);
        } catch (final Exception e) {
            final String error = String.format("Failed to backup snapshot with id [%s] with a pool %s, due to %s",
                cmd.getSrcTO().getId(), cmd.getSrcTO().getDataStore().getUuid(), e.getMessage());
            LOGGER.error(error);
            return new CopyCmdAnswer(cmd, e);
        } finally {
            if (src.getVolume() != null) {
                src.getVolume().clearPassphrase();
            }
            cleanupSecondaryPool(secondaryPool);
            if (zfsHidden) {
                zfsSnapdev(true, src.getPath());
            }
        }
    }
}
