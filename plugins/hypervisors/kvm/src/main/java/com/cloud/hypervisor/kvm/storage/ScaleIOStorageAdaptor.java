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

import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.libvirt.LibvirtException;

import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import org.apache.commons.lang3.StringUtils;

public class ScaleIOStorageAdaptor implements StorageAdaptor {
    protected Logger logger = LogManager.getLogger(getClass());
    private static final Map<String, KVMStoragePool> MapStorageUuidToStoragePool = new HashMap<>();
    private static final int DEFAULT_DISK_WAIT_TIME_IN_SECS = 60;

    public ScaleIOStorageAdaptor() {

    }

    @Override
    public KVMStoragePool getStoragePool(String uuid) {
        KVMStoragePool pool = MapStorageUuidToStoragePool.get(uuid);
        if (pool == null) {
            logger.error("Pool: " + uuid + " not found, probably sdc not connected on agent start");
            throw new CloudRuntimeException("Pool: " + uuid + " not found, reconnect sdc and restart agent if sdc not connected on agent start");
        }

        return pool;
    }

    @Override
    public Storage.StoragePoolType getStoragePoolType() {
        return Storage.StoragePoolType.PowerFlex;
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid, boolean refreshInfo) {
        return getStoragePool(uuid);
    }

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String volumePath, KVMStoragePool pool) {
        if (StringUtils.isEmpty(volumePath) || pool == null) {
            logger.error("Unable to get physical disk, volume path or pool not specified");
            return null;
        }

        String volumeId = ScaleIOUtil.getVolumePath(volumePath);

        try {
            String diskFilePath = null;
            String systemId = ScaleIOUtil.getSystemIdForVolume(volumeId);
            if (StringUtils.isNotEmpty(systemId) && systemId.length() == ScaleIOUtil.IDENTIFIER_LENGTH) {
                // Disk path format: /dev/disk/by-id/emc-vol-<SystemID>-<VolumeID>
                final String diskFileName = ScaleIOUtil.DISK_NAME_PREFIX + systemId + "-" + volumeId;
                diskFilePath = ScaleIOUtil.DISK_PATH + File.separator + diskFileName;
                final File diskFile = new File(diskFilePath);
                if (!diskFile.exists()) {
                    logger.debug("Physical disk file: " + diskFilePath + " doesn't exists on the storage pool: " + pool.getUuid());
                    return null;
                }
            } else {
                logger.debug("Try with wildcard filter to get the physical disk: " + volumeId + " on the storage pool: " + pool.getUuid());
                final File dir = new File(ScaleIOUtil.DISK_PATH);
                final FileFilter fileFilter = new WildcardFileFilter(ScaleIOUtil.DISK_NAME_PREFIX_FILTER + volumeId);
                final File[] files = dir.listFiles(fileFilter);
                if (files != null && files.length == 1) {
                    diskFilePath = files[0].getAbsolutePath();
                } else {
                    logger.debug("Unable to find the physical disk: " + volumeId + " on the storage pool: " + pool.getUuid());
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
            logger.error("Failed to get the physical disk: " + volumePath + " on the storage pool: " + pool.getUuid() + " due to " + e.getMessage());
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

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, KVMStoragePool pool, QemuImg.PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, byte[] passphrase) {
        return createPhysicalDisk(name, pool, format, provisioningType, size, null, passphrase);
    }

    /**
     * ScaleIO doesn't need to communicate with the hypervisor normally to create a volume. This is used only to prepare a ScaleIO data disk for encryption.
     * Thin encrypted volumes are provisioned in QCOW2 format, which insulates the guest from zeroes/unallocated blocks in the block device that would
     * otherwise show up as garbage data through the encryption layer.  As a bonus, encrypted QCOW2 format handles discard.
     * @param name disk path
     * @param pool pool
     * @param format disk format
     * @param provisioningType provisioning type
     * @param size disk size
     * @param usableSize usage disk size
     * @param passphrase passphrase
     * @return the disk object
     */
    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, KVMStoragePool pool, QemuImg.PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, Long usableSize, byte[] passphrase) {
        if (passphrase == null || passphrase.length == 0) {
            return null;
        }

        if(!connectPhysicalDisk(name, pool, null)) {
            throw new CloudRuntimeException(String.format("Failed to ensure disk %s was present", name));
        }

        KVMPhysicalDisk disk = getPhysicalDisk(name, pool);

        if (provisioningType.equals(Storage.ProvisioningType.THIN)) {
            disk.setFormat(QemuImg.PhysicalDiskFormat.QCOW2);
            disk.setQemuEncryptFormat(QemuObject.EncryptFormat.LUKS);
            try (KeyFile keyFile = new KeyFile(passphrase)){
                QemuImg qemuImg = new QemuImg(0, true, false);
                Map<String, String> options = new HashMap<>();
                List<QemuObject> qemuObjects = new ArrayList<>();
                long formattedSize;
                if (usableSize != null && usableSize > 0) {
                    formattedSize = usableSize;
                } else {
                    formattedSize = getUsableBytesFromRawBytes(disk.getSize());
                }

                options.put("preallocation", QemuImg.PreallocationType.Metadata.toString());
                qemuObjects.add(QemuObject.prepareSecretForQemuImg(disk.getFormat(), disk.getQemuEncryptFormat(), keyFile.toString(), "sec0", options));
                QemuImgFile file = new QemuImgFile(disk.getPath(), formattedSize, disk.getFormat());
                qemuImg.create(file, null, options, qemuObjects);
                logger.debug(String.format("Successfully formatted %s as encrypted QCOW2", file.getFileName()));
            } catch (QemuImgException | LibvirtException | IOException ex) {
                throw new CloudRuntimeException("Failed to set up encrypted QCOW on block device " + disk.getPath(), ex);
            }
        } else {
            try {
                CryptSetup crypt = new CryptSetup();
                crypt.luksFormat(passphrase, CryptSetup.LuksType.LUKS, disk.getPath());
                disk.setQemuEncryptFormat(QemuObject.EncryptFormat.LUKS);
                disk.setFormat(QemuImg.PhysicalDiskFormat.RAW);
            } catch (CryptSetupException ex) {
                throw new CloudRuntimeException("Failed to set up encryption for block device " + disk.getPath(), ex);
            }
        }

        return disk;
    }

    @Override
    public boolean connectPhysicalDisk(String volumePath, KVMStoragePool pool, Map<String, String> details) {
        if (StringUtils.isEmpty(volumePath) || pool == null) {
            logger.error("Unable to connect physical disk due to insufficient data");
            throw new CloudRuntimeException("Unable to connect physical disk due to insufficient data");
        }

        volumePath = ScaleIOUtil.getVolumePath(volumePath);

        int waitTimeInSec = DEFAULT_DISK_WAIT_TIME_IN_SECS;
        if (details != null && details.containsKey(StorageManager.STORAGE_POOL_DISK_WAIT.toString())) {
            String waitTime = details.get(StorageManager.STORAGE_POOL_DISK_WAIT.toString());
            if (StringUtils.isNotEmpty(waitTime)) {
                waitTimeInSec = Integer.valueOf(waitTime).intValue();
            }
        }
        return waitForDiskToBecomeAvailable(volumePath, pool, waitTimeInSec);
    }

    private boolean waitForDiskToBecomeAvailable(String volumePath, KVMStoragePool pool, int waitTimeInSec) {
        logger.debug("Waiting for the volume with id: " + volumePath + " of the storage pool: " + pool.getUuid() + " to become available for " + waitTimeInSec + " secs");
        int timeBetweenTries = 1000; // Try more frequently (every sec) and return early if disk is found
        KVMPhysicalDisk physicalDisk = null;

        // Rescan before checking for the physical disk
        ScaleIOUtil.rescanForNewVolumes();

        while (waitTimeInSec > 0) {
            physicalDisk = getPhysicalDisk(volumePath, pool);
            if (physicalDisk != null && physicalDisk.getSize() > 0) {
                logger.debug("Found the volume with id: " + volumePath + " of the storage pool: " + pool.getUuid());
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
            logger.debug("Found the volume using id: " + volumePath + " of the storage pool: " + pool.getUuid());
            return true;
        }

        logger.debug("Unable to find the volume with id: " + volumePath + " of the storage pool: " + pool.getUuid());
        return false;
    }

    private long getPhysicalDiskSize(String diskPath) {
        if (StringUtils.isEmpty(diskPath)) {
            return 0;
        }

        Script diskCmd = new Script("blockdev", logger);
        diskCmd.add("--getsize64", diskPath);

        OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        String result = diskCmd.execute(parser);

        if (result != null) {
            logger.warn("Unable to get the disk size at path: " + diskPath);
            return 0;
        } else {
            logger.info("Able to retrieve the disk size at path:" + diskPath);
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
        return copyPhysicalDisk(disk, name, destPool, timeout, null, null, Storage.ProvisioningType.THIN);
    }

    @Override
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPool, int timeout, byte[] srcPassphrase, byte[]dstPassphrase, Storage.ProvisioningType provisioningType) {
        if (StringUtils.isEmpty(name) || disk == null || destPool == null) {
            logger.error("Unable to copy physical disk due to insufficient data");
            throw new CloudRuntimeException("Unable to copy physical disk due to insufficient data");
        }

        if (provisioningType == null) {
            provisioningType = Storage.ProvisioningType.THIN;
        }

        logger.debug("Copy physical disk with size: " + disk.getSize() + ", virtualsize: " + disk.getVirtualSize()+ ", format: " + disk.getFormat());

        KVMPhysicalDisk destDisk = destPool.getPhysicalDisk(name);
        if (destDisk == null) {
            logger.error("Failed to find the disk: " + name + " of the storage pool: " + destPool.getUuid());
            throw new CloudRuntimeException("Failed to find the disk: " + name + " of the storage pool: " + destPool.getUuid());
        }

        destDisk.setVirtualSize(disk.getVirtualSize());
        destDisk.setSize(disk.getSize());

        QemuImg qemu = null;
        QemuImgFile srcQemuFile = null;
        QemuImgFile destQemuFile = null;
        String srcKeyName = "sec0";
        String destKeyName = "sec1";
        List<QemuObject> qemuObjects = new ArrayList<>();
        Map<String, String> options = new HashMap<String, String>();
        CryptSetup cryptSetup = null;

        try (KeyFile srcKey = new KeyFile(srcPassphrase); KeyFile dstKey = new KeyFile(dstPassphrase)){
            qemu = new QemuImg(timeout, provisioningType.equals(Storage.ProvisioningType.FAT), false);
            String srcPath = disk.getPath();
            String destPath = destDisk.getPath();

            QemuImageOptions qemuImageOpts = new QemuImageOptions(srcPath);

            srcQemuFile = new QemuImgFile(srcPath, disk.getFormat());
            destQemuFile = new QemuImgFile(destPath);

            if (disk.useAsTemplate()) {
                destQemuFile.setFormat(QemuImg.PhysicalDiskFormat.QCOW2);
            }

            if (srcKey.isSet()) {
                qemuObjects.add(QemuObject.prepareSecretForQemuImg(disk.getFormat(), disk.getQemuEncryptFormat(), srcKey.toString(), srcKeyName, options));
                qemuImageOpts = new QemuImageOptions(disk.getFormat(), srcPath, srcKeyName);
            }

            if (dstKey.isSet()) {
                if (!provisioningType.equals(Storage.ProvisioningType.FAT)) {
                    destDisk.setFormat(QemuImg.PhysicalDiskFormat.QCOW2);
                    destQemuFile.setFormat(QemuImg.PhysicalDiskFormat.QCOW2);
                    options.put("preallocation", QemuImg.PreallocationType.Metadata.toString());
                } else {
                    qemu.setSkipZero(false);
                    destDisk.setFormat(QemuImg.PhysicalDiskFormat.RAW);
                    // qemu-img wants to treat RAW + encrypt formatting as LUKS
                    destQemuFile.setFormat(QemuImg.PhysicalDiskFormat.LUKS);
                }
                qemuObjects.add(QemuObject.prepareSecretForQemuImg(destDisk.getFormat(), QemuObject.EncryptFormat.LUKS, dstKey.toString(), destKeyName, options));
                destDisk.setQemuEncryptFormat(QemuObject.EncryptFormat.LUKS);
            }

            boolean forceSourceFormat = srcQemuFile.getFormat() == QemuImg.PhysicalDiskFormat.RAW;
            logger.debug(String.format("Starting copy from source disk %s(%s) to PowerFlex volume %s(%s), forcing source format is %b", srcQemuFile.getFileName(), srcQemuFile.getFormat(), destQemuFile.getFileName(), destQemuFile.getFormat(), forceSourceFormat));
            qemuImageOpts.setImageOptsFlag(true);
            qemu.convert(srcQemuFile, destQemuFile, options, qemuObjects, qemuImageOpts,null, forceSourceFormat);
            logger.debug("Successfully converted source disk image " + srcQemuFile.getFileName() + " to PowerFlex volume: " + destDisk.getPath());

            if (destQemuFile.getFormat() == QemuImg.PhysicalDiskFormat.QCOW2 && !disk.useAsTemplate()) {
                QemuImageOptions resizeOptions = new QemuImageOptions(destQemuFile.getFormat(), destPath, destKeyName);
                resizeQcow2ToVolume(destPath, resizeOptions, qemuObjects, timeout);
                logger.debug("Resized volume at " + destPath);
            }
        }  catch (QemuImgException | LibvirtException | IOException e) {
            try {
                Map<String, String> srcInfo = qemu.info(srcQemuFile);
                logger.debug("Source disk info: " + Arrays.asList(srcInfo));
            } catch (Exception ignored) {
                logger.warn("Unable to get info from source disk: " + disk.getName());
            }

            String errMsg = String.format("Unable to convert/copy from %s to %s, due to: %s", disk.getName(), name, ((StringUtils.isEmpty(e.getMessage())) ? "an unknown error" : e.getMessage()));
            logger.error(errMsg);
            throw new CloudRuntimeException(errMsg, e);
        } finally {
            if (cryptSetup != null) {
                try {
                    cryptSetup.close(name);
                } catch (CryptSetupException ex) {
                    logger.warn("Failed to clean up LUKS disk after copying disk", ex);
                }
            }
        }

        return destDisk;
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
        return createFolder(uuid, path, null);
    }

    @Override
    public boolean createFolder(String uuid, String path, String localPath) {
        return true;
    }


    @Override
    public KVMPhysicalDisk createDiskFromTemplateBacking(KVMPhysicalDisk template, String name, QemuImg.PhysicalDiskFormat format, long size, KVMStoragePool destPool, int timeout, byte[] passphrase) {
        return null;
    }

    @Override
    public KVMPhysicalDisk createTemplateFromDirectDownloadFile(String templateFilePath, String destTemplatePath, KVMStoragePool destPool, Storage.ImageFormat format, int timeout) {
        if (StringUtils.isAnyEmpty(templateFilePath, destTemplatePath) || destPool == null) {
            logger.error("Unable to create template from direct download template file due to insufficient data");
            throw new CloudRuntimeException("Unable to create template from direct download template file due to insufficient data");
        }

        logger.debug("Create template from direct download template - file path: " + templateFilePath + ", dest path: " + destTemplatePath + ", format: " + format.toString());

        File sourceFile = new File(templateFilePath);
        if (!sourceFile.exists()) {
            throw new CloudRuntimeException("Direct download template file " + templateFilePath + " does not exist on this host");
        }

        if (destTemplatePath == null || destTemplatePath.isEmpty()) {
            logger.error("Failed to create template, target template disk path not provided");
            throw new CloudRuntimeException("Target template disk path not provided");
        }

        if (destPool.getType() != Storage.StoragePoolType.PowerFlex) {
            throw new CloudRuntimeException("Unsupported storage pool type: " + destPool.getType().toString());
        }

        if (Storage.ImageFormat.RAW.equals(format) && Storage.ImageFormat.QCOW2.equals(format)) {
            logger.error("Failed to create template, unsupported template format: " + format.toString());
            throw new CloudRuntimeException("Unsupported template format: " + format.toString());
        }

        String srcTemplateFilePath = templateFilePath;
        KVMPhysicalDisk destDisk = null;
        QemuImgFile srcFile = null;
        QemuImgFile destFile = null;
        try {
            QemuImg qemu = new QemuImg(timeout, true, false);
            destDisk = destPool.getPhysicalDisk(destTemplatePath);
            if (destDisk == null) {
                logger.error("Failed to find the disk: " + destTemplatePath + " of the storage pool: " + destPool.getUuid());
                throw new CloudRuntimeException("Failed to find the disk: " + destTemplatePath + " of the storage pool: " + destPool.getUuid());
            }

            if (isTemplateExtractable(templateFilePath)) {
                srcTemplateFilePath = sourceFile.getParent() + "/" + UUID.randomUUID().toString();
                logger.debug("Extract the downloaded template " + templateFilePath + " to " + srcTemplateFilePath);
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
             * Even though the disk itself is raw, we store templates on ScaleIO in qcow2 format.
             * This improves performance by reading/writing less data to volume, saves the unused space for encryption header, and
             * nicely encapsulates VM images that might contain LUKS data (as opposed to converting to raw which would look like a LUKS volume).
             */
            destFile = new QemuImgFile(destDisk.getPath(), QemuImg.PhysicalDiskFormat.QCOW2);
            destFile.setSize(srcFile.getSize());

            logger.debug("Starting copy from source downloaded template " + srcFile.getFileName() + " to PowerFlex template volume: " + destDisk.getPath());
            qemu.create(destFile);
            qemu.convert(srcFile, destFile);
            logger.debug("Successfully converted source downloaded template " + srcFile.getFileName() + " to PowerFlex template volume: " + destDisk.getPath());
        }  catch (QemuImgException | LibvirtException e) {
            logger.error("Failed to convert. The error was: " + e.getMessage(), e);
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

    public void resizeQcow2ToVolume(String volumePath, QemuImageOptions options, List<QemuObject> objects, Integer timeout) throws QemuImgException, LibvirtException {
        long rawSizeBytes = getPhysicalDiskSize(volumePath);
        long usableSizeBytes = getUsableBytesFromRawBytes(rawSizeBytes);
        QemuImg qemu = new QemuImg(timeout);
        qemu.resize(options, objects, usableSizeBytes);
    }

    public Ternary<Boolean, Map<String, String>, String> prepareStorageClient(Storage.StoragePoolType type, String uuid, Map<String, String> details) {
        if (!ScaleIOUtil.isSDCServiceInstalled()) {
            logger.debug("SDC service not installed on host, preparing the SDC client not possible");
            return new Ternary<>(false, null, "SDC service not installed on host");
        }

        if (!ScaleIOUtil.isSDCServiceEnabled()) {
            logger.debug("SDC service not enabled on host, enabling it");
            if (!ScaleIOUtil.enableSDCService()) {
                return new Ternary<>(false, null, "SDC service not enabled on host");
            }
        }

        if (!ScaleIOUtil.isSDCServiceActive()) {
            if (!ScaleIOUtil.startSDCService()) {
                return new Ternary<>(false, null, "Couldn't start SDC service on host");
            }
        } else if (!ScaleIOUtil.restartSDCService()) {
            return new Ternary<>(false, null, "Couldn't restart SDC service on host");
        }

        return new Ternary<>( true, getSDCDetails(details), "Prepared client successfully");
    }

    public Pair<Boolean, String> unprepareStorageClient(Storage.StoragePoolType type, String uuid) {
        if (!ScaleIOUtil.isSDCServiceInstalled()) {
            logger.debug("SDC service not installed on host, no need to unprepare the SDC client");
            return new Pair<>(true, "SDC service not installed on host, no need to unprepare the SDC client");
        }

        if (!ScaleIOUtil.isSDCServiceEnabled()) {
            logger.debug("SDC service not enabled on host, no need to unprepare the SDC client");
            return new Pair<>(true, "SDC service not enabled on host, no need to unprepare the SDC client");
        }

        if (!ScaleIOUtil.stopSDCService()) {
            return new Pair<>(false, "Couldn't stop SDC service on host");
        }

        return new Pair<>(true, "Unprepared SDC client successfully");
    }

    private Map<String, String> getSDCDetails(Map<String, String> details) {
        Map<String, String> sdcDetails = new HashMap<String, String>();
        if (details == null || !details.containsKey(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID))  {
            return sdcDetails;
        }

        String storageSystemId = details.get(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID);
        String sdcId = ScaleIOUtil.getSdcId(storageSystemId);
        if (sdcId != null) {
            sdcDetails.put(ScaleIOGatewayClient.SDC_ID, sdcId);
        } else {
            String sdcGuId = ScaleIOUtil.getSdcGuid();
            if (sdcGuId != null) {
                sdcDetails.put(ScaleIOGatewayClient.SDC_GUID, sdcGuId);
            }
        }
        return sdcDetails;
    }

    /**
     * Calculates usable size from raw size, assuming qcow2 requires 192k/1GB for metadata
     * We also remove 128MiB for encryption/fragmentation/safety factor.
     * @param raw size in bytes
     * @return usable size in bytesbytes
     */
    public static long getUsableBytesFromRawBytes(Long raw) {
        long usable = raw - (128 << 20) - ((raw >> 30) * 200704);
        if (usable < 0) {
            usable = 0L;
        }
        return usable;
    }
}
