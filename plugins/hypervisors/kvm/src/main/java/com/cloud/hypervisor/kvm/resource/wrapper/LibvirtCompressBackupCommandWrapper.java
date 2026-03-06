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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.CompressBackupCommand;
import org.apache.cloudstack.storage.formatinspector.Qcow2Inspector;
import org.apache.cloudstack.storage.to.DeltaMergeTreeTO;
import org.apache.cloudstack.utils.qemu.QemuImageOptions;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.libvirt.LibvirtException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@ResourceWrapper(handles = CompressBackupCommand.class)
public class LibvirtCompressBackupCommandWrapper extends CommandWrapper<CompressBackupCommand, Answer, LibvirtComputingResource>  {
    public static final String COMPRESSION_TYPE = "compression_type";
    private static final int MIN_QCOW_2_VERSION_FOR_ZSTD = 3;

    @Override
    public Answer execute(CompressBackupCommand command, LibvirtComputingResource serverResource) {
        List<KVMStoragePool> secondaryStorages = new ArrayList<>();
        List<DeltaMergeTreeTO> deltas = command.getBackupDeltasToCompress();
        KVMStoragePoolManager storagePoolManager = serverResource.getStoragePoolMgr();

        logger.info("Starting compression for backup deltas [{}].", deltas);
        try {
            QemuImg qemuImg = new QemuImg(command.getWait() * 1000);
            Integer rateLimit = validateAndGetRateLimit(command, qemuImg);

            KVMStoragePool mainSecStorage = storagePoolManager.getStoragePoolByURI(deltas.stream().findFirst().orElseThrow().getChild().getDataStore().getUrl());
            secondaryStorages.add(mainSecStorage);
            secondaryStorages.addAll(command.getBackupChainImageStoreUrls().stream().map(storagePoolManager::getStoragePoolByURI).collect(Collectors.toList()));

            if (!checkAvailableStorage(command, mainSecStorage, storagePoolManager)) {
                return new Answer(command, false, "Not enough available space on secondary.");
            }

            for (DeltaMergeTreeTO delta : deltas) {
                DataTO child = delta.getChild();

                QemuImgFile backingFile = null;
                DataTO parent = delta.getParent();
                if (parent != null) {
                    KVMStoragePool parentSecondaryStorage = storagePoolManager.getStoragePoolByURI(parent.getDataStore().getUrl());
                    secondaryStorages.add(parentSecondaryStorage);
                    backingFile = new QemuImgFile(parentSecondaryStorage.getLocalPathFor(parent.getPath()), QemuImg.PhysicalDiskFormat.QCOW2);
                }

                String fullDeltaPath = mainSecStorage.getLocalPathFor(child.getPath());
                String compressedPath = fullDeltaPath + ".comp";
                QemuImgFile originalBackup = new QemuImgFile(fullDeltaPath, QemuImg.PhysicalDiskFormat.QCOW2);
                QemuImgFile compressedBackup = new QemuImgFile(compressedPath, QemuImg.PhysicalDiskFormat.QCOW2);

                HashMap<String, String> options = new HashMap<>();
                Backup.CompressionLibrary compressionLib = getCompressionLibrary(command, fullDeltaPath);
                setCompressionTypeOptionIfAvailable(qemuImg, options, compressionLib);
                int coroutines = command.getCoroutines();
                logger.info("Starting compression for backup delta [{}] with parent [{}] using [{}] coroutines.", child, parent, coroutines);
                qemuImg.convert(originalBackup, compressedBackup, backingFile, options, null, new QemuImageOptions(originalBackup.getFormat(), originalBackup.getFileName(),
                        null), null, false, false, true, true, coroutines, rateLimit);
            }
        } catch (LibvirtException | QemuImgException e) {
            return new Answer(command, e);
        } finally {
            for (KVMStoragePool secondaryStorage : secondaryStorages) {
                storagePoolManager.deleteStoragePool(secondaryStorage.getType(), secondaryStorage.getUuid());
            }
        }

        return new Answer(command);
    }
    private Integer validateAndGetRateLimit(CompressBackupCommand command, QemuImg qemuImg) {
        if (qemuImg.getVersion() < QemuImg.QEMU_5_20) {
            throw new CloudRuntimeException("Qemu version is lower than 5.2.0, unable to set the rate limit.");
        }
        return command.getRateLimit() < 1 ? null : command.getRateLimit();
    }

    /**
     * Sets the compression type option if qemu-img is at least in version 5.1. Otherwise, will not set it and qemu will use zlib.
     * */
    private void setCompressionTypeOptionIfAvailable(QemuImg qemuImg, HashMap<String, String> options, Backup.CompressionLibrary compressionLib) {
        if (qemuImg.getVersion() >= QemuImg.QEMU_5_10) {
            options.put(COMPRESSION_TYPE, compressionLib.name());
            return;
        }
        logger.warn("Qemu is at a lower version than 5.1, we will not be able to use zstd to compress backups. Only zlib is supported for this version. Current version is [{}].",
                qemuImg.getVersion());
    }

    private Backup.CompressionLibrary getCompressionLibrary(CompressBackupCommand command, String fullDeltaPath) {
        Backup.CompressionLibrary compressionLib = command.getCompressionLib();
        if (compressionLib == Backup.CompressionLibrary.zlib || !Qcow2Inspector.validateQcow2Version(fullDeltaPath, MIN_QCOW_2_VERSION_FOR_ZSTD)) {
            logger.debug("Compression for delta [{}] will use zlib as the compression library.", fullDeltaPath);
            return Backup.CompressionLibrary.zlib;
        }

        logger.debug("Compression for delta [{}] will try to use zstd as the compression library.", fullDeltaPath);
        return Backup.CompressionLibrary.zstd;
    }

    /**
     * Validates available storage. Forces Libvirt to refresh storage info so that we have the most up to date data.
     * */
    private boolean checkAvailableStorage(CompressBackupCommand command, KVMStoragePool mainSecStorage, KVMStoragePoolManager storagePoolManager) {
        logger.debug("Checking available storage [{}].", mainSecStorage);
        mainSecStorage = storagePoolManager.getStoragePool(mainSecStorage.getType(), mainSecStorage.getUuid(), true, false);
        if (mainSecStorage.getAvailable() < command.getMinFreeStorage()) {
            logger.warn("There is not enough available space for compression of backup! Available size is [{}], needed [{}]. Aborting compression.",
                    mainSecStorage.getAvailable(), command.getMinFreeStorage());
            return false;
        }
        return true;
    }
}
