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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.storage.datastore.util.ScaleIOUtil;
import org.apache.cloudstack.utils.cryptsetup.CryptSetup;
import org.apache.cloudstack.utils.cryptsetup.CryptSetupException;
import org.apache.cloudstack.utils.cryptsetup.KeyFile;
import org.apache.cloudstack.utils.qemu.QemuImageOptions;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.cloudstack.utils.qemu.QemuObject;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;
import org.libvirt.LibvirtException;

import com.cloud.storage.Storage;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.StorageManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.common.base.Strings;

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

            // try to discover format as written to disk, rather than assuming raw.
            // We support qcow2 for stored primary templates, disks seen as other should be treated as raw.
            QemuImg qemu = new QemuImg(0);
            QemuImgFile qemuFile = new QemuImgFile(diskFilePath);
            Map<String, String> details = qemu.info(qemuFile);
            String detectedFormat = details.getOrDefault(QemuImg.FILE_FORMAT, "none");
            if (detectedFormat.equalsIgnoreCase(QemuImg.PhysicalDiskFormat.QCOW2.toString())) {
                disk.setFormat(QemuImg.PhysicalDiskFormat.QCOW2);
            } else {
                disk.setFormat(QemuImg.PhysicalDiskFormat.RAW);
            }

            long diskSize = getPhysicalDiskSize(diskFilePath);
            disk.setSize(diskSize);

            if (details.containsKey(QemuImg.VIRTUAL_SIZE)) {
                disk.setVirtualSize(Long.parseLong(details.get(QemuImg.VIRTUAL_SIZE)));
            } else {
                disk.setVirtualSize(diskSize);
            }

            return disk;
        } catch (Exception e) {
            LOGGER.error("Failed to get the physical disk: " + volumePath + " on the storage pool: " + pool.getUuid() + " due to " + e.getMessage());
            throw new CloudRuntimeException("Failed to get the physical disk: " + volumePath + " on the storage pool: " + pool.getUuid());
        }
    }

    @Override
    public KVMStoragePool createStoragePool(String uuid, String host, int port, String path, String userInfo, Storage.StoragePoolType type, Map<String, String> details) {
        ScaleIOStoragePool storagePool = new ScaleIOStoragePool(uuid, host, port, path, type, details, this);
        MapStorageUuidToStoragePool.put(uuid, storagePool);
        return storagePool;
    }

    @Override
    public boolean deleteStoragePool(String uuid) {
        return MapStorageUuidToStoragePool.remove(uuid) != null;
    }

    /**
     * ScaleIO doesn't need to communicate with the hypervisor normally to create a volume. This is used only to prepare a ScaleIO data disk for encryption.
     * @param name disk path
     * @param pool pool
     * @param format disk format
     * @param provisioningType provisioning type
     * @param size disk size
     * @param passphrase passphrase
     * @return the disk object
     */
    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, KVMStoragePool pool, QemuImg.PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, byte[] passphrase) {
        if (passphrase == null || passphrase.length == 0) {
            return null;
        }

        if(!connectPhysicalDisk(name, pool, null)) {
            throw new CloudRuntimeException(String.format("Failed to ensure disk %s was present", name));
        }

        KVMPhysicalDisk disk = getPhysicalDisk(name, pool);

        try {
            CryptSetup crypt = new CryptSetup();
            crypt.luksFormat(passphrase, CryptSetup.LuksType.LUKS, disk.getPath());
            disk.setQemuEncryptFormat(QemuObject.EncryptFormat.LUKS);
        } catch (CryptSetupException ex) {
            throw new CloudRuntimeException("Failed to set up encryption for block device " + disk.getPath(), ex);
        }

        return disk;
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
        return false;
    }

    @Override
    public boolean disconnectPhysicalDiskByPath(String localPath) {
        return false;
    }

    @Override
    public boolean deletePhysicalDisk(String uuid, KVMStoragePool pool, Storage.ImageFormat format) {
        return true;
    }

    @Override
    public KVMPhysicalDisk createDiskFromTemplate(KVMPhysicalDisk template, String name, QemuImg.PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, KVMStoragePool destPool, int timeout, byte[] passphrase) {
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
        return copyPhysicalDisk(disk, name, destPool, timeout, null, null);
    }

    @Override
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPool, int timeout, byte[] srcPassphrase, byte[]dstPassphrase) {
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

        // golden copies of templates should be kept as qcow2 in the PowerFlex storage pool
        if (disk.isTemplate()) {
            LOGGER.debug("This is a template copy, storing in powerflex as QCOW2");
            destDisk.setFormat(QemuImg.PhysicalDiskFormat.QCOW2);
        } else {
            LOGGER.debug("This is not a template copy, storing in powerflex as RAW");
            destDisk.setFormat(QemuImg.PhysicalDiskFormat.RAW);
        }

        destDisk.setVirtualSize(disk.getVirtualSize());
        destDisk.setSize(disk.getSize());

        QemuImg qemu = null;
        QemuImgFile srcFile = null;
        QemuImgFile destFile = null;
        String srcKeyName = "sec0";
        String destKeyName = "sec1";
        List<QemuObject> qemuObjects = new ArrayList<>();
        Map<String, String> options = new HashMap<String, String>();
        CryptSetup cryptSetup = null;

        try (KeyFile srcKey = new KeyFile(srcPassphrase); KeyFile dstKey = new KeyFile(dstPassphrase)){
            qemu = new QemuImg(timeout, true, true);
            String srcPath = disk.getPath();
            String destPath = destDisk.getPath();
            QemuImg.PhysicalDiskFormat destFormat = destDisk.getFormat();
            QemuImageOptions qemuImageOpts = new QemuImageOptions(srcPath);

            if (srcKey.isSet()) {
                qemuObjects.add(QemuObject.prepareSecretForQemuImg(disk.getFormat(), null , srcKey.toString(), srcKeyName, options));
                qemuImageOpts = new QemuImageOptions(disk.getFormat(), srcPath, srcKeyName);
            }

            if (dstKey.isSet()) {
                if (qemu.supportsSkipZeros()) {
                    // format and open luks device rather than letting qemu do a slow copy of full image
                    cryptSetup = new CryptSetup();
                    cryptSetup.luksFormat(dstPassphrase, CryptSetup.LuksType.LUKS, destDisk.getPath());
                    cryptSetup.open(dstPassphrase, CryptSetup.LuksType.LUKS, destDisk.getPath(),  name);
                    destPath = String.format("/dev/mapper/%s", name);
                } else {
                    qemuObjects.add(QemuObject.prepareSecretForQemuImg(destDisk.getFormat(), null, dstKey.toString(), destKeyName, options));
                    destFormat = QemuImg.PhysicalDiskFormat.LUKS;
                }
                destDisk.setQemuEncryptFormat(QemuObject.EncryptFormat.LUKS);
            }

            srcFile = new QemuImgFile(srcPath, disk.getFormat());
            destFile = new QemuImgFile(destPath, destFormat);

            boolean forceSourceFormat = srcFile.getFormat() == QemuImg.PhysicalDiskFormat.RAW;
            LOGGER.debug(String.format("Starting copy from source disk %s(%s) to PowerFlex volume %s(%s), forcing source format is %b", srcFile.getFileName(), srcFile.getFormat(), destFile.getFileName(), destFile.getFormat(), forceSourceFormat));
            if (destFile.getFormat() == QemuImg.PhysicalDiskFormat.QCOW2) {
                destFile.setSize(disk.getVirtualSize());
                LOGGER.debug(String.format("Pre-formatting qcow2 block device %s to size %s", destFile.getFileName(), destFile.getSize()));
                qemu.create(destFile);
            }
            qemu.convert(srcFile, destFile, options, qemuObjects, qemuImageOpts,null, forceSourceFormat);
            LOGGER.debug("Succesfully converted source disk image " + srcFile.getFileName() + " to PowerFlex volume: " + destDisk.getPath());
        }  catch (QemuImgException | LibvirtException | IOException | CryptSetupException e) {
            try {
                Map<String, String> srcInfo = qemu.info(srcFile);
                LOGGER.debug("Source disk info: " + Arrays.asList(srcInfo));
            } catch (Exception ignored) {
                LOGGER.warn("Unable to get info from source disk: " + disk.getName());
            }

            String errMsg = String.format("Unable to convert/copy from %s to %s, due to: %s", disk.getName(), name, ((Strings.isNullOrEmpty(e.getMessage())) ? "an unknown error" : e.getMessage()));
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg, e);
        } finally {
            if (cryptSetup != null) {
                try {
                    cryptSetup.close(name);
                } catch (CryptSetupException ex) {
                    LOGGER.warn("Failed to clean up LUKS disk after copying disk", ex);
                }
            }
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
    public KVMPhysicalDisk createDiskFromTemplateBacking(KVMPhysicalDisk template, String name, QemuImg.PhysicalDiskFormat format, long size, KVMStoragePool destPool, int timeout, byte[] passphrase) {
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
            QemuImg qemu = new QemuImg(timeout, true, true);
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
            qemu.info(srcFile);
            /**
             * Even though the disk itself is raw, we store templates on ScaleIO the raw volumes in qcow2 format.
             * This improves performance by reading/writing less data to volume, saves the unused space for encryption header, and
             * nicely encapsulates VM images that might contain LUKS data (as opposed to converting to raw which would look like a LUKS volume).
             */
            destFile = new QemuImgFile(destDisk.getPath(), QemuImg.PhysicalDiskFormat.QCOW2);
            destFile.setSize(srcFile.getSize());

            LOGGER.debug("Starting copy from source downloaded template " + srcFile.getFileName() + " to PowerFlex template volume: " + destDisk.getPath());
            qemu.create(destFile);
            qemu.convert(srcFile, destFile);
            LOGGER.debug("Successfully converted source downloaded template " + srcFile.getFileName() + " to PowerFlex template volume: " + destDisk.getPath());
        }  catch (QemuImgException | LibvirtException e) {
            LOGGER.error("Failed to convert. The error was: " + e.getMessage(), e);
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
