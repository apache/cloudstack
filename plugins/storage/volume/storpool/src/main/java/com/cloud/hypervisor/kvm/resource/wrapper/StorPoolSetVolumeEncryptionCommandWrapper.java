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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.cryptsetup.CryptSetup;
import org.apache.cloudstack.utils.cryptsetup.CryptSetupException;
import org.apache.cloudstack.utils.cryptsetup.KeyFile;
import org.apache.cloudstack.utils.qemu.QemuImageOptions;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.cloudstack.utils.qemu.QemuObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.StorPoolSetVolumeEncryptionAnswer;
import com.cloud.agent.api.storage.StorPoolSetVolumeEncryptionCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.hypervisor.kvm.storage.StorPoolStorageAdaptor;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;

@ResourceWrapper(handles = StorPoolSetVolumeEncryptionCommand.class)
public class StorPoolSetVolumeEncryptionCommandWrapper extends
        CommandWrapper<StorPoolSetVolumeEncryptionCommand, StorPoolSetVolumeEncryptionAnswer, LibvirtComputingResource> {
    private static final Logger logger = Logger.getLogger(StorPoolSetVolumeEncryptionCommandWrapper.class);

    @Override
    public StorPoolSetVolumeEncryptionAnswer execute(StorPoolSetVolumeEncryptionCommand command,
            LibvirtComputingResource serverResource) {
        VolumeObjectTO volume = command.getVolumeObjectTO();
        String srcVolumeName = command.getSrcVolumeName();
        try {
            StorPoolStorageAdaptor.attachOrDetachVolume("attach", "volume", volume.getPath());
            KVMStoragePoolManager storagePoolMgr = serverResource.getStoragePoolMgr();
            PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO) volume.getDataStore();
            KVMStoragePool pool = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());
            KVMPhysicalDisk disk = pool.getPhysicalDisk(volume.getPath());
            if (command.isDataDisk()) {
                encryptDataDisk(volume, disk);
            } else {
                disk = encryptRootDisk(command, volume, srcVolumeName, pool, disk);
            }
            logger.debug(String.format("StorPoolSetVolumeEncryptionCommandWrapper disk=%s", disk));
        } catch (Exception e) {
            new Answer(command, e);
        } finally {
            StorPoolStorageAdaptor.attachOrDetachVolume("detach", "volume", volume.getPath());
            volume.clearPassphrase();
        }
        return new StorPoolSetVolumeEncryptionAnswer(volume);
    }

    private KVMPhysicalDisk encryptRootDisk(StorPoolSetVolumeEncryptionCommand command, VolumeObjectTO volume,
            String srcVolumeName, KVMStoragePool pool, KVMPhysicalDisk disk) {
        StorPoolStorageAdaptor.attachOrDetachVolume("attach", "snapshot", srcVolumeName);
        KVMPhysicalDisk srcVolume = pool.getPhysicalDisk(srcVolumeName);
        disk = copyPhysicalDisk(srcVolume, disk, command.getWait() * 1000, null, volume.getPassphrase());
        disk.setQemuEncryptFormat(QemuObject.EncryptFormat.LUKS);
        disk.setFormat(QemuImg.PhysicalDiskFormat.RAW);
        volume.setEncryptFormat(disk.getQemuEncryptFormat().toString());
        StorPoolStorageAdaptor.attachOrDetachVolume("detach", "snapshot", srcVolumeName);
        return disk;
    }

    private void encryptDataDisk(VolumeObjectTO volume, KVMPhysicalDisk disk) throws CryptSetupException {
        CryptSetup crypt = new CryptSetup();
        crypt.luksFormat(volume.getPassphrase(), CryptSetup.LuksType.LUKS, disk.getPath());
        disk.setQemuEncryptFormat(QemuObject.EncryptFormat.LUKS);
        disk.setFormat(QemuImg.PhysicalDiskFormat.RAW);
        volume.setEncryptFormat(disk.getQemuEncryptFormat().toString());
    }

    private KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, KVMPhysicalDisk destDisk, int timeout,
            byte[] srcPassphrase, byte[] dstPassphrase) {

        logger.debug("Copy physical disk with size: " + disk.getSize() + ", virtualsize: " + disk.getVirtualSize()
                + ", format: " + disk.getFormat());

        destDisk.setVirtualSize(disk.getVirtualSize());
        destDisk.setSize(disk.getSize());

        QemuImg qemu = null;
        QemuImgFile srcQemuFile = null;
        QemuImgFile destQemuFile = null;
        String srcKeyName = "sec0";
        String destKeyName = "sec1";
        List<QemuObject> qemuObjects = new ArrayList<>();
        Map<String, String> options = new HashMap<>();

        try (KeyFile srcKey = new KeyFile(srcPassphrase); KeyFile dstKey = new KeyFile(dstPassphrase)) {
            qemu = new QemuImg(timeout, true, false);
            String srcPath = disk.getPath();
            String destPath = destDisk.getPath();

            QemuImageOptions qemuImageOpts = new QemuImageOptions(srcPath);

            srcQemuFile = new QemuImgFile(srcPath, disk.getFormat());
            destQemuFile = new QemuImgFile(destPath);

            if (srcKey.isSet()) {
                qemuObjects.add(QemuObject.prepareSecretForQemuImg(disk.getFormat(), disk.getQemuEncryptFormat(),
                        srcKey.toString(), srcKeyName, options));
                qemuImageOpts = new QemuImageOptions(disk.getFormat(), srcPath, srcKeyName);
            }

            if (dstKey.isSet()) {
                qemu.setSkipZero(false);
                destDisk.setFormat(QemuImg.PhysicalDiskFormat.RAW);
                destQemuFile.setFormat(QemuImg.PhysicalDiskFormat.LUKS);
                qemuObjects.add(QemuObject.prepareSecretForQemuImg(destDisk.getFormat(), QemuObject.EncryptFormat.LUKS,
                        dstKey.toString(), destKeyName, options));
                destDisk.setQemuEncryptFormat(QemuObject.EncryptFormat.LUKS);
            }

            qemu.convert(srcQemuFile, destQemuFile, options, qemuObjects, qemuImageOpts, null, true);
            logger.debug("Successfully converted source disk image " + srcQemuFile.getFileName()
                    + " to StorPool volume: " + destDisk.getPath());

        } catch (QemuImgException | LibvirtException | IOException e) {

            String errMsg = String.format("Unable to convert/copy from %s to %s, due to: %s", disk.getName(),
                    destDisk.getName(), ((StringUtils.isEmpty(e.getMessage())) ? "an unknown error" : e.getMessage()));
            logger.error(errMsg);
            throw new CloudRuntimeException(errMsg, e);
        }

        return destDisk;
    }
}
