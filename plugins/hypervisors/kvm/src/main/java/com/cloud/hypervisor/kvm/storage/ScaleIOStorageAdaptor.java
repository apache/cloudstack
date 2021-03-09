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

package com.cloud.hypervisor.kvm.storage;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.storage.datastore.util.ScaleIOUtil;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;

import com.cloud.storage.Storage;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.StorageManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.common.base.Strings;
import org.libvirt.LibvirtException;

@StorageAdaptorInfo(storagePoolType= Storage.StoragePoolType.PowerFlex)
public class ScaleIOStorageAdaptor implements StorageAdaptor {
    private static final Logger LOGGER = Logger.getLogger(ScaleIOStorageAdaptor.class);
    private static final Map<String, KVMStoragePool> MapStorageUuidToStoragePool = new HashMap<>();
    private static final int DEFAULT_DISK_WAIT_TIME_IN_SECS = 60;
    private StorageLayer storageLayer;

    public ScaleIOStorageAdaptor(StorageLayer storagelayer) {
        storageLayer = storagelayer;
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid) {
        KVMStoragePool pool = MapStorageUuidToStoragePool.get(uuid);
        if (pool == null) {
            LOGGER.error("Pool: " + uuid + " not found, probably sdc not connected on agent start");
            throw new CloudRuntimeException("Pool: " + uuid + " not found, reconnect sdc and restart agent if sdc not connected on agent start");
        }

        return pool;
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid, boolean refreshInfo) {
        return getStoragePool(uuid);
    }

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String volumePath, KVMStoragePool pool) {
        if (Strings.isNullOrEmpty(volumePath) || pool == null) {
            LOGGER.error("Unable to get physical disk, volume path or pool not specified");
            return null;
        }

        String volumeId = ScaleIOUtil.getVolumePath(volumePath);

        try {
            String diskFilePath = null;
            String systemId = ScaleIOUtil.getSystemIdForVolume(volumeId);
            if (!Strings.isNullOrEmpty(systemId) && systemId.length() == ScaleIOUtil.IDENTIFIER_LENGTH) {
                // Disk path format: /dev/disk/by-id/emc-vol-<SystemID>-<VolumeID>
                final String diskFileName = ScaleIOUtil.DISK_NAME_PREFIX + systemId + "-" + volumeId;
                diskFilePath = ScaleIOUtil.DISK_PATH + File.separator + diskFileName;
                final File diskFile = new File(diskFilePath);
                if (!diskFile.exists()) {
                    LOGGER.debug("Physical disk file: " + diskFilePath + " doesn't exists on the storage pool: " + pool.getUuid());
                    return null;
                }
            } else {
                LOGGER.debug("Try with wildcard filter to get the physical disk: " + volumeId + " on the storage pool: " + pool.getUuid());
                final File dir = new File(ScaleIOUtil.DISK_PATH);
                final FileFilter fileFilter = new WildcardFileFilter(ScaleIOUtil.DISK_NAME_PREFIX_FILTER + volumeId);
                final File[] files = dir.listFiles(fileFilter);
                if (files != null && files.length == 1) {
                    diskFilePath = files[0].getAbsolutePath();
                } else {
                    LOGGER.debug("Unable to find the physical disk: " + volumeId + " on the storage pool: " + pool.getUuid());
                    return null;
                }
            }

            KVMPhysicalDisk disk = new KVMPhysicalDisk(diskFilePath, volumePath, pool);
            disk.setFormat(QemuImg.PhysicalDiskFormat.RAW);

            long diskSize = getPhysicalDiskSize(diskFilePath);
            disk.setSize(diskSize);
            disk.setVirtualSize(diskSize);

            return disk;
        } catch (Exception e) {
            LOGGER.error("Failed to get the physical disk: " + volumePath + " on the storage pool: " + pool.getUuid() + " due to " + e.getMessage());
            throw new CloudRuntimeException("Failed to get the physical disk: " + volumePath + " on the storage pool: " + pool.getUuid());
        }
    }

    @Override
    public KVMStoragePool createStoragePool(String uuid, String host, int port, String path, String userInfo, Storage.StoragePoolType type) {
        ScaleIOStoragePool storagePool = new ScaleIOStoragePool(uuid, host, port, path, type, this);
        MapStorageUuidToStoragePool.put(uuid, storagePool);
        return storagePool;
    }

    @Override
    public boolean deleteStoragePool(String uuid) {
        return MapStorageUuidToStoragePool.remove(uuid) != null;
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, KVMStoragePool pool, QemuImg.PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size) {
        return null;
    }

    @Override
    public boolean connectPhysicalDisk(String volumePath, KVMStoragePool pool, Map<String, String> details) {
        if (Strings.isNullOrEmpty(volumePath) || pool == null) {
            LOGGER.error("Unable to connect physical disk due to insufficient data");
            throw new CloudRuntimeException("Unable to connect physical disk due to insufficient data");
        }

        volumePath = ScaleIOUtil.getVolumePath(volumePath);

        int waitTimeInSec = DEFAULT_DISK_WAIT_TIME_IN_SECS;
        if (details != null && details.containsKey(StorageManager.STORAGE_POOL_DISK_WAIT.toString())) {
            String waitTime = details.get(StorageManager.STORAGE_POOL_DISK_WAIT.toString());
            if (!Strings.isNullOrEmpty(waitTime)) {
                waitTimeInSec = Integer.valueOf(waitTime).intValue();
            }
        }
        return waitForDiskToBecomeAvailable(volumePath, pool, waitTimeInSec);
    }

    private boolean waitForDiskToBecomeAvailable(String volumePath, KVMStoragePool pool, int waitTimeInSec) {
        LOGGER.debug("Waiting for the volume with id: " + volumePath + " of the storage pool: " + pool.getUuid() + " to become available for " + waitTimeInSec + " secs");
        int timeBetweenTries = 1000; // Try more frequently (every sec) and return early if disk is found
        KVMPhysicalDisk physicalDisk = null;

        // Rescan before checking for the physical disk
        ScaleIOUtil.rescanForNewVolumes();

        while (waitTimeInSec > 0) {
            physicalDisk = getPhysicalDisk(volumePath, pool);
            if (physicalDisk != null && physicalDisk.getSize() > 0) {
                LOGGER.debug("Found the volume with id: " + volumePath + " of the storage pool: " + pool.getUuid());
                return true;
            }

            waitTimeInSec--;

            try {
                Thread.sleep(timeBetweenTries);
            } catch (Exception ex) {
                // don't do anything
            }
        }

        physicalDisk = getPhysicalDisk(volumePath, pool);
        if (physicalDisk != null && physicalDisk.getSize() > 0) {
            LOGGER.debug("Found the volume using id: " + volumePath + " of the storage pool: " + pool.getUuid());
            return true;
        }

        LOGGER.debug("Unable to find the volume with id: " + volumePath + " of the storage pool: " + pool.getUuid());
        return false;
    }

    private long getPhysicalDiskSize(String diskPath) {
        if (Strings.isNullOrEmpty(diskPath)) {
            return 0;
        }

        Script diskCmd = new Script("blockdev", LOGGER);
        diskCmd.add("--getsize64", diskPath);

        OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        String result = diskCmd.execute(parser);

        if (result != null) {
            LOGGER.warn("Unable to get the disk size at path: " + diskPath);
            return 0;
        } else {
            LOGGER.info("Able to retrieve the disk size at path:" + diskPath);
        }

        return Long.parseLong(parser.getLine());
    }

    @Override
    public boolean disconnectPhysicalDisk(String volumePath, KVMStoragePool pool) {
        return true;
    }

    @Override
    public boolean disconnectPhysicalDisk(Map<String, String> volumeToDisconnect) {
        return true;
    }

    @Override
    public boolean disconnectPhysicalDiskByPath(String localPath) {
        return true;
    }

    @Override
    public boolean deletePhysicalDisk(String uuid, KVMStoragePool pool, Storage.ImageFormat format) {
        return true;
    }

    @Override
    public KVMPhysicalDisk createDiskFromTemplate(KVMPhysicalDisk template, String name, QemuImg.PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, KVMStoragePool destPool, int timeout) {
        return null;
    }

    @Override
    public KVMPhysicalDisk createTemplateFromDisk(KVMPhysicalDisk disk, String name, QemuImg.PhysicalDiskFormat format, long size, KVMStoragePool destPool) {
        return null;
    }

    @Override
    public List<KVMPhysicalDisk> listPhysicalDisks(String storagePoolUuid, KVMStoragePool pool) {
        return null;
    }

    @Override
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPool, int timeout) {
        if (Strings.isNullOrEmpty(name) || disk == null || destPool == null) {
            LOGGER.error("Unable to copy physical disk due to insufficient data");
            throw new CloudRuntimeException("Unable to copy physical disk due to insufficient data");
        }

        LOGGER.debug("Copy physical disk with size: " + disk.getSize() + ", virtualsize: " + disk.getVirtualSize()+ ", format: " + disk.getFormat());

        KVMPhysicalDisk destDisk = destPool.getPhysicalDisk(name);
        if (destDisk == null) {
            LOGGER.error("Failed to find the disk: " + name + " of the storage pool: " + destPool.getUuid());
            throw new CloudRuntimeException("Failed to find the disk: " + name + " of the storage pool: " + destPool.getUuid());
        }

        destDisk.setFormat(QemuImg.PhysicalDiskFormat.RAW);
        destDisk.setVirtualSize(disk.getVirtualSize());
        destDisk.setSize(disk.getSize());

        QemuImg qemu = new QemuImg(timeout);
        QemuImgFile srcFile = null;
        QemuImgFile destFile = null;

        try {
            srcFile = new QemuImgFile(disk.getPath(), disk.getFormat());
            destFile = new QemuImgFile(destDisk.getPath(), destDisk.getFormat());

            LOGGER.debug("Starting copy from source image " + srcFile.getFileName() + " to PowerFlex volume: " + destDisk.getPath());
            qemu.convert(srcFile, destFile);
            LOGGER.debug("Succesfully converted source image " + srcFile.getFileName() + " to PowerFlex volume: " + destDisk.getPath());
        }  catch (QemuImgException | LibvirtException e) {
            LOGGER.error("Failed to convert from " + srcFile.getFileName() + " to " + destFile.getFileName() + " the error was: " + e.getMessage(), e);
            destDisk = null;
        }

        return destDisk;
    }

    @Override
    public KVMPhysicalDisk createDiskFromSnapshot(KVMPhysicalDisk snapshot, String snapshotName, String name, KVMStoragePool destPool, int timeout) {
        return null;
    }

    @Override
    public boolean refresh(KVMStoragePool pool) {
        return true;
    }

    @Override
    public boolean deleteStoragePool(KVMStoragePool pool) {
        return deleteStoragePool(pool.getUuid());
    }

    @Override
    public boolean createFolder(String uuid, String path) {
        return true;
    }

    @Override
    public KVMPhysicalDisk createDiskFromTemplateBacking(KVMPhysicalDisk template, String name, QemuImg.PhysicalDiskFormat format, long size, KVMStoragePool destPool, int timeout) {
        return null;
    }

    @Override
    public KVMPhysicalDisk createTemplateFromDirectDownloadFile(String templateFilePath, String destTemplatePath, KVMStoragePool destPool, Storage.ImageFormat format, int timeout) {
        if (Strings.isNullOrEmpty(templateFilePath) || Strings.isNullOrEmpty(destTemplatePath) || destPool == null) {
            LOGGER.error("Unable to create template from direct download template file due to insufficient data");
            throw new CloudRuntimeException("Unable to create template from direct download template file due to insufficient data");
        }

        LOGGER.debug("Create template from direct download template - file path: " + templateFilePath + ", dest path: " + destTemplatePath + ", format: " + format.toString());

        File sourceFile = new File(templateFilePath);
        if (!sourceFile.exists()) {
            throw new CloudRuntimeException("Direct download template file " + templateFilePath + " does not exist on this host");
        }

        if (destTemplatePath == null || destTemplatePath.isEmpty()) {
            LOGGER.error("Failed to create template, target template disk path not provided");
            throw new CloudRuntimeException("Target template disk path not provided");
        }

        if (destPool.getType() != Storage.StoragePoolType.PowerFlex) {
            throw new CloudRuntimeException("Unsupported storage pool type: " + destPool.getType().toString());
        }

        if (Storage.ImageFormat.RAW.equals(format) && Storage.ImageFormat.QCOW2.equals(format)) {
            LOGGER.error("Failed to create template, unsupported template format: " + format.toString());
            throw new CloudRuntimeException("Unsupported template format: " + format.toString());
        }

        String srcTemplateFilePath = templateFilePath;
        KVMPhysicalDisk destDisk = null;
        QemuImgFile srcFile = null;
        QemuImgFile destFile = null;
        try {
            destDisk = destPool.getPhysicalDisk(destTemplatePath);
            if (destDisk == null) {
                LOGGER.error("Failed to find the disk: " + destTemplatePath + " of the storage pool: " + destPool.getUuid());
                throw new CloudRuntimeException("Failed to find the disk: " + destTemplatePath + " of the storage pool: " + destPool.getUuid());
            }

            if (isTemplateExtractable(templateFilePath)) {
                srcTemplateFilePath = sourceFile.getParent() + "/" + UUID.randomUUID().toString();
                LOGGER.debug("Extract the downloaded template " + templateFilePath + " to " + srcTemplateFilePath);
                String extractCommand = getExtractCommandForDownloadedFile(templateFilePath, srcTemplateFilePath);
                Script.runSimpleBashScript(extractCommand);
                Script.runSimpleBashScript("rm -f " + templateFilePath);
            }

            QemuImg.PhysicalDiskFormat srcFileFormat = QemuImg.PhysicalDiskFormat.RAW;
            if (format == Storage.ImageFormat.RAW) {
                srcFileFormat = QemuImg.PhysicalDiskFormat.RAW;
            } else if (format == Storage.ImageFormat.QCOW2) {
                srcFileFormat = QemuImg.PhysicalDiskFormat.QCOW2;
            }

            srcFile = new QemuImgFile(srcTemplateFilePath, srcFileFormat);
            destFile = new QemuImgFile(destDisk.getPath(), destDisk.getFormat());

            LOGGER.debug("Starting copy from source downloaded template " + srcFile.getFileName() + " to PowerFlex template volume: " + destDisk.getPath());
            QemuImg qemu = new QemuImg(timeout);
            qemu.convert(srcFile, destFile);
            LOGGER.debug("Succesfully converted source downloaded template " + srcFile.getFileName() + " to PowerFlex template volume: " + destDisk.getPath());
        }  catch (QemuImgException | LibvirtException e) {
            LOGGER.error("Failed to convert from " + srcFile.getFileName() + " to " + destFile.getFileName() + " the error was: " + e.getMessage(), e);
            destDisk = null;
        } finally {
            Script.runSimpleBashScript("rm -f " + srcTemplateFilePath);
        }

        return destDisk;
    }

    private boolean isTemplateExtractable(String templatePath) {
        String type = Script.runSimpleBashScript("file " + templatePath + " | awk -F' ' '{print $2}'");
        return type.equalsIgnoreCase("bzip2") || type.equalsIgnoreCase("gzip") || type.equalsIgnoreCase("zip");
    }

    private String getExtractCommandForDownloadedFile(String downloadedTemplateFile, String templateFile) {
        if (downloadedTemplateFile.endsWith(".zip")) {
            return "unzip -p " + downloadedTemplateFile + " | cat > " + templateFile;
        } else if (downloadedTemplateFile.endsWith(".bz2")) {
            return "bunzip2 -c " + downloadedTemplateFile + " > " + templateFile;
        } else if (downloadedTemplateFile.endsWith(".gz")) {
            return "gunzip -c " + downloadedTemplateFile + " > " + templateFile;
        } else {
            throw new CloudRuntimeException("Unable to extract template " + downloadedTemplateFile);
        }
    }
}
