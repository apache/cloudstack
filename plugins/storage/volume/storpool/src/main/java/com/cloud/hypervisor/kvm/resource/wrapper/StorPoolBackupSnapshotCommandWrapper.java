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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.utils.cryptsetup.KeyFile;
import org.apache.cloudstack.utils.qemu.QemuImageOptions;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.cloudstack.utils.qemu.QemuObject;
import org.apache.cloudstack.utils.qemu.QemuObject.EncryptFormat;
import org.apache.commons.io.FileUtils;

import com.cloud.agent.api.storage.StorPoolBackupSnapshotCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.hypervisor.kvm.storage.StorPoolStorageAdaptor;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles = StorPoolBackupSnapshotCommand.class)
public final class StorPoolBackupSnapshotCommandWrapper extends CommandWrapper<StorPoolBackupSnapshotCommand, CopyCmdAnswer, LibvirtComputingResource> {


    @Override
    public CopyCmdAnswer execute(final StorPoolBackupSnapshotCommand cmd, final LibvirtComputingResource libvirtComputingResource) {
        String srcPath = null;
        KVMStoragePool secondaryPool = null;

        try {
            final SnapshotObjectTO src = cmd.getSourceTO();
            final SnapshotObjectTO dst = cmd.getDestinationTO();
            final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();

            SP_LOG("StorpoolBackupSnapshotCommandWrapper.execute: src=" + src.getPath() + "dst=" + dst.getPath());
            StorPoolStorageAdaptor.attachOrDetachVolume("attach", "snapshot", src.getPath());
            srcPath = src.getPath();
            long size = 0;
            String srcKeyName = "sec0";
            String destKeyName = "sec1";
            List<QemuObject> qemuObjects = new ArrayList<>();
            Map<String, String> options = new HashMap<>();
            QemuImageOptions qemuImageOpts = new QemuImageOptions(srcPath);
            final QemuImg qemu = new QemuImg(cmd.getWaitInMillSeconds());
            final DataStoreTO dstDataStore = dst.getDataStore();
            if (!(dstDataStore instanceof NfsTO)) {
                return new CopyCmdAnswer("Backup Storpool snapshot: Only NFS secondary supported at present!");
            }

            secondaryPool = storagePoolMgr.getStoragePoolByURI(dstDataStore.getUrl());
            try (KeyFile srcKey = new KeyFile(src.getVolume().getPassphrase())) {

                size = convertSnapshot(srcPath, secondaryPool, dst, srcKeyName, qemuObjects, options, qemuImageOpts,
                        qemu, srcKey);
            }

            final SnapshotObjectTO snapshot = new SnapshotObjectTO();
            snapshot.setPath(dst.getPath() + File.separator + dst.getName());
            snapshot.setPhysicalSize(size);

            return new CopyCmdAnswer(snapshot);
        } catch (final Exception e) {
            final String error = String.format("Failed to backup snapshot with id [%s] with a pool %s, due to %s", cmd.getSourceTO().getId(), cmd.getSourceTO().getDataStore().getUuid(), e.getMessage());
            SP_LOG(error);
            logger.debug(error);
            return new CopyCmdAnswer(cmd, e);
        } finally {
            if (srcPath != null) {
                StorPoolStorageAdaptor.attachOrDetachVolume("detach", "snapshot", srcPath);
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

    private long convertSnapshot(String srcPath, KVMStoragePool secondaryPool, final SnapshotObjectTO dst,
            String srcKeyName, List<QemuObject> qemuObjects, Map<String, String> options,
            QemuImageOptions qemuImageOpts, final QemuImg qemu, KeyFile srcKey) throws IOException, QemuImgException {
        long size;
        final QemuImgFile srcFile = new QemuImgFile(srcPath, PhysicalDiskFormat.RAW);

        final String dstDir = secondaryPool.getLocalPath() + File.separator + dst.getPath();
        FileUtils.forceMkdir(new File(dstDir));

        final String dstPath = dstDir + File.separator + dst.getName();
        final QemuImgFile dstFile = new QemuImgFile(dstPath, PhysicalDiskFormat.QCOW2);
        if (srcKey.isSet()) {
            qemuObjects.add(QemuObject.prepareSecretForQemuImg(PhysicalDiskFormat.RAW, EncryptFormat.LUKS,
                    srcKey.toString(), srcKeyName, options));
            qemuImageOpts = new QemuImageOptions(PhysicalDiskFormat.RAW, srcPath, srcKeyName);
            dstFile.setFormat(PhysicalDiskFormat.LUKS);
        }

        qemuImageOpts.setImageOptsFlag(true);
        qemu.convert(srcFile, dstFile, options, qemuObjects, qemuImageOpts, null, true);

        SP_LOG("StorpoolBackupSnapshotCommandWrapper srcFileFormat=%s, dstFileFormat=%s", srcFile.getFormat(), dstFile.getFormat());
        final File snapFile = new File(dstPath);
        size =  snapFile.exists() ? snapFile.length() : 0;
        return size;
    }
}
