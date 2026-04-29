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

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolDef;
import com.cloud.hypervisor.kvm.resource.LibvirtStorageVolumeDef;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.joda.time.Duration;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.StoragePool;
import org.libvirt.StorageVol;

/**
 * Storage adaptor for CLVM and CLVM_NG pool types.
 * Extends LibvirtStorageAdaptor and overrides methods with CLVM-specific logic,
 * using direct LVM commands instead of libvirt for volume operations.
 */
public class ClvmStorageAdaptor extends LibvirtStorageAdaptor {

    public ClvmStorageAdaptor(StorageLayer storage) {
        super(storage);
    }

    @Override
    public StoragePoolType getStoragePoolType() {
        // Registered manually for both CLVM and CLVM_NG in KVMStoragePoolManager
        return null;
    }

    @Override
    public KVMStoragePool createStoragePool(String name, String host, int port, String path,
            String userInfo, StoragePoolType type, Map<String, String> details, boolean isPrimaryStorage) {
        logger.info("Attempting to create CLVM/CLVM_NG storage pool {} in libvirt", name);

        Connect conn;
        try {
            conn = LibvirtConnection.getConnection();
        } catch (LibvirtException e) {
            throw new CloudRuntimeException(e.toString());
        }

        StoragePool sp = createCLVMStoragePool(conn, name, host, path);
        if (sp == null) {
            logger.info("Falling back to virtual CLVM/CLVM_NG pool without libvirt for: {}", name);
            return createVirtualClvmPool(name, host, path, type, details);
        }

        try {
            if (!isPrimaryStorage) {
                incStoragePoolRefCount(name);
            }
            // CLVM/CLVM_NG pools are kept inactive in libvirt; we use direct LVM commands
            return getStoragePool(name);
        } catch (Exception e) {
            decStoragePoolRefCount(name);
            throw new CloudRuntimeException("Failed to create CLVM storage pool: " + name, e);
        }
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid, boolean refreshInfo) {
        logger.info("Fetching CLVM/CLVM_NG storage pool {} ", uuid);
        try {
            Connect conn = LibvirtConnection.getConnection();
            StoragePool storage = conn.storagePoolLookupByUUIDString(uuid);

            LibvirtStoragePoolDef spd = getStoragePoolDef(conn, storage);
            if (spd == null) {
                throw new CloudRuntimeException("Unable to parse storage pool definition for pool " + uuid);
            }

            // CLVM pools in libvirt are always LOGICAL type
            StoragePoolType type = StoragePoolType.CLVM;

            // Do NOT activate the pool — CLVM/CLVM_NG pools stay inactive in libvirt
            LibvirtStoragePool pool = new LibvirtStoragePool(uuid, storage.getName(), type, this, storage);
            pool.setLocalPath(spd.getTargetPath());

            // Always read capacity from LVM directly
            String vgName = storage.getName();
            try {
                long[] vgStats = getVgStats(vgName);
                setPoolCapacityFromVgStats(pool, vgStats, vgName);
            } catch (CloudRuntimeException e) {
                logger.warn("Failed to get VG stats for CLVM/CLVM_NG pool {}: {}. Using libvirt values (may be 0)", vgName, e.getMessage());
                pool.setCapacity(storage.getInfo().capacity);
                pool.setUsed(storage.getInfo().allocation);
                pool.setAvailable(storage.getInfo().available);
            }

            return pool;
        } catch (LibvirtException e) {
            // Pool not in libvirt - return virtual pool backed only by LVM
            logger.debug("CLVM/CLVM_NG pool {} not found in libvirt, creating virtual pool", uuid);
            throw new CloudRuntimeException(e.toString(), e);
        }
    }

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String volumeUuid, KVMStoragePool pool) {
        LibvirtStoragePool libvirtPool = (LibvirtStoragePool) pool;

        // Pool has no libvirt backing - go directly to block device
        if (libvirtPool.getPool() == null) {
            logger.debug("CLVM/CLVM_NG pool has no libvirt backing, using direct block device access for volume: {}", volumeUuid);
            return getPhysicalDiskViaDirectBlockDevice(volumeUuid, pool);
        }

        try {
            StorageVol vol = getVolume(libvirtPool.getPool(), volumeUuid);
            if (vol == null) {
                logger.debug("Volume {} not found in libvirt, falling back to CLVM direct access", volumeUuid);
                return getPhysicalDiskWithClvmFallback(volumeUuid, pool, libvirtPool);
            }

            LibvirtStorageVolumeDef voldef = getStorageVolumeDef(libvirtPool.getPool().getConnect(), vol);
            KVMPhysicalDisk disk = new KVMPhysicalDisk(vol.getPath(), vol.getName(), pool);
            disk.setSize(vol.getInfo().allocation);
            disk.setVirtualSize(vol.getInfo().capacity);
            disk.setFormat(voldef.getFormat() == LibvirtStorageVolumeDef.VolumeFormat.QCOW2
                    ? PhysicalDiskFormat.QCOW2 : PhysicalDiskFormat.RAW);
            return disk;
        } catch (LibvirtException e) {
            logger.warn("LibvirtException looking up volume {}: {}", volumeUuid, e.getMessage());
            return getPhysicalDiskWithClvmFallback(volumeUuid, pool, libvirtPool);
        }
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, KVMStoragePool pool,
            PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, byte[] passphrase) {
        logger.info("Creating CLVM/CLVM_NG volume {} in pool {} with size {}", name, pool.getUuid(), toHumanReadableSize(size));

        if (StoragePoolType.CLVM_NG.equals(pool.getType())) {
            return createClvmNgDiskWithBacking(name, 0, size, null, pool, provisioningType);
        } else {
            return createClvmVolume(name, size, pool);
        }
    }

    @Override
    public boolean connectPhysicalDisk(String name, KVMStoragePool pool, Map<String, String> details, boolean isVMMigrate) {
        if (isVMMigrate) {
            logger.info("Activating CLVM/CLVM_NG volume {} in shared mode for VM migration", name);
            Script activateVol = new Script("lvchange", 5000, logger);
            activateVol.add("-asy");
            activateVol.add(pool.getLocalPath() + File.separator + name);
            String result = activateVol.execute();
            if (result != null) {
                logger.error("Failed to activate CLVM/CLVM_NG volume {} in shared mode. Output: {}", name, result);
                return false;
            }
        }

        if (StoragePoolType.CLVM_NG.equals(pool.getType())) {
            ensureClvmNgBackingFileAccessible(name, pool);
        }

        return true;
    }

    @Override
    public KVMPhysicalDisk createDiskFromTemplate(KVMPhysicalDisk template,
            String name, PhysicalDiskFormat format, Storage.ProvisioningType provisioningType,
            long size, KVMStoragePool destPool, int timeout, byte[] passphrase) {

        if (StoragePoolType.CLVM_NG.equals(destPool.getType()) && format == PhysicalDiskFormat.QCOW2) {
            logger.info("Creating CLVM_NG volume {} with backing file from template {}", name, template.getName());
            String backingFile = getClvmBackingFile(template, destPool);
            return createClvmNgDiskWithBacking(name, timeout, size, backingFile, destPool, provisioningType);
        }

        return super.createDiskFromTemplate(template, name, format, provisioningType, size, destPool, timeout, passphrase);
    }

    @Override
    public void createTemplate(String templatePath, String templateUuid, int timeout, KVMStoragePool pool) {
        String vgName = getVgName(pool.getLocalPath());
        String lvName = "template-" + templateUuid;
        String lvPath = "/dev/" + vgName + "/" + lvName;

        if (lvExists(lvPath)) {
            logger.info("Template LV {} already exists in VG {}. Skipping creation.", lvName, vgName);
            return;
        }

        logger.info("Creating new template LV {} in VG {} for template {}", lvName, vgName, templateUuid);

        long virtualSize = getQcow2VirtualSize(templatePath);
        long physicalSize = getQcow2PhysicalSize(templatePath);
        long lvSize = virtualSize;

        logger.info("Template source - Physical: {} bytes, Virtual: {} bytes, LV will be: {} bytes", physicalSize, virtualSize, lvSize);

        Script lvcreate = new Script("lvcreate", Duration.millis(timeout), logger);
        lvcreate.add("-n", lvName);
        lvcreate.add("-L", lvSize + "B");
        lvcreate.add("--yes");
        lvcreate.add(vgName);
        String result = lvcreate.execute();
        if (result != null) {
            throw new CloudRuntimeException("Failed to create LV for CLVM_NG template: " + result);
        }

        Script qemuImgConvert = new Script("qemu-img", Duration.millis(timeout), logger);
        qemuImgConvert.add("convert");
        qemuImgConvert.add(templatePath);
        qemuImgConvert.add("-O", "qcow2");
        qemuImgConvert.add("-o", "cluster_size=64k,extended_l2=off,preallocation=off");
        qemuImgConvert.add(lvPath);
        result = qemuImgConvert.execute();

        if (result != null) {
            removeLvOnFailure(lvPath, timeout);
            throw new CloudRuntimeException("Failed to convert template to CLVM_NG volume: " + result);
        }

        long actualVirtualSize = getQcow2VirtualSize(lvPath);

        try {
            ensureTemplateLvInSharedMode(lvPath, true);
        } catch (CloudRuntimeException e) {
            logger.error("Failed to activate template LV {} in shared mode. Cleaning up.", lvPath);
            removeLvOnFailure(lvPath, timeout);
            throw e;
        }

        KVMPhysicalDisk templateDisk = new KVMPhysicalDisk(lvPath, lvName, pool);
        templateDisk.setFormat(PhysicalDiskFormat.QCOW2);
        templateDisk.setVirtualSize(actualVirtualSize);
        templateDisk.setSize(lvSize);
    }

    private StoragePool createCLVMStoragePool(Connect conn, String uuid, String host, String path) {
        String volgroupPath = "/dev/" + path;
        String volgroupName = path;
        volgroupName = volgroupName.replaceFirst("^/", "");

        Script checkVgExists = new Script("vgs", 5000, logger);
        checkVgExists.add("--noheadings");
        checkVgExists.add("-o", "vg_name");
        checkVgExists.add(volgroupName);
        String vgCheckResult = checkVgExists.execute();

        if (vgCheckResult != null) {
            logger.error("Volume group {} does not exist or is not accessible", volgroupName);
            return null;
        }

        logger.info("Volume group {} verified, creating libvirt pool definition for CLVM/CLVM_NG", volgroupName);
        LibvirtStoragePoolDef poolDef = new LibvirtStoragePoolDef(
            LibvirtStoragePoolDef.PoolType.LOGICAL,
            volgroupName,
            uuid,
            null,
            volgroupName,
            volgroupPath
        );

        try {
            StoragePool pool = conn.storagePoolDefineXML(poolDef.toString(), 0);
            logger.info("Created libvirt pool definition for CLVM/CLVM_NG VG: {} (pool will remain inactive)", volgroupName);
            pool.setAutostart(1);
            return pool;
        } catch (LibvirtException e) {
            logger.warn("Failed to define CLVM/CLVM_NG pool in libvirt: {}", e.getMessage());
            return null;
        }
    }

    private void setPoolCapacityFromVgStats(LibvirtStoragePool pool, long[] vgStats, String vgName) {
        long capacity = vgStats[0];
        long available = vgStats[1];
        long used = capacity - available;

        pool.setCapacity(capacity);
        pool.setAvailable(available);
        pool.setUsed(used);

        logger.debug("CLVM/CLVM_NG pool {} - Capacity: {}, Used: {}, Available: {}",
                vgName, toHumanReadableSize(capacity), toHumanReadableSize(used), toHumanReadableSize(available));
    }

    private long[] getVgStats(String vgName) {
        Script getVgStats = new Script("vgs", 5000, logger);
        getVgStats.add("--noheadings");
        getVgStats.add("--units", "b");
        getVgStats.add("--nosuffix");
        getVgStats.add("-o", "vg_size,vg_free");
        getVgStats.add(vgName);

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = getVgStats.execute(parser);

        if (result != null) {
            String errorMsg = "Failed to get statistics for volume group " + vgName + ": " + result;
            logger.error(errorMsg);
            throw new CloudRuntimeException(errorMsg);
        }

        String output = parser.getLines().trim();
        String[] lines = output.split("\\n");
        String dataLine = null;

        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && Character.isDigit(line.charAt(0))) {
                dataLine = line;
                break;
            }
        }

        if (dataLine == null) {
            String errorMsg = "No valid data line found in vgs output for " + vgName + ": " + output;
            logger.error(errorMsg);
            throw new CloudRuntimeException(errorMsg);
        }

        String[] stats = dataLine.split("\\s+");

        if (stats.length < 2) {
            String errorMsg = "Unexpected output from vgs command for " + vgName + ": " + dataLine;
            logger.error(errorMsg);
            throw new CloudRuntimeException(errorMsg);
        }

        try {
            long capacity = Long.parseLong(stats[0].trim());
            long available = Long.parseLong(stats[1].trim());
            return new long[]{capacity, available};
        } catch (NumberFormatException e) {
            String errorMsg = "Failed to parse VG statistics for " + vgName + ": " + e.getMessage();
            logger.error(errorMsg);
            throw new CloudRuntimeException(errorMsg, e);
        }
    }

    private KVMStoragePool createVirtualClvmPool(String uuid, String host, String path, StoragePoolType type, Map<String, String> details) {
        String volgroupName = path.replaceFirst("^/", "");
        String volgroupPath = "/dev/" + volgroupName;

        logger.info("Creating virtual CLVM/CLVM_NG pool {} without libvirt using direct LVM access", volgroupName);

        long[] vgStats = getVgStats(volgroupName);

        LibvirtStoragePool pool = new LibvirtStoragePool(uuid, volgroupName, type, this, null);
        pool.setLocalPath(volgroupPath);
        setPoolCapacityFromVgStats(pool, vgStats, volgroupName);

        if (details != null) {
            pool.setDetails(details);
        }

        return pool;
    }

    /**
     * CLVM fallback: First tries to refresh libvirt pool to make volume visible,
     * if that fails, accesses volume directly via block device path.
     */
    private KVMPhysicalDisk getPhysicalDiskWithClvmFallback(String volumeUuid, KVMStoragePool pool, LibvirtStoragePool libvirtPool) {
        logger.info("CLVM volume not visible to libvirt, attempting pool refresh for volume: {}", volumeUuid);

        try {
            logger.debug("Refreshing libvirt storage pool: {}", pool.getUuid());
            libvirtPool.getPool().refresh(0);

            StorageVol vol = getVolume(libvirtPool.getPool(), volumeUuid);
            if (vol != null) {
                logger.info("Volume found after pool refresh: {}", volumeUuid);
                LibvirtStorageVolumeDef voldef = getStorageVolumeDef(libvirtPool.getPool().getConnect(), vol);
                KVMPhysicalDisk disk = new KVMPhysicalDisk(vol.getPath(), vol.getName(), pool);
                disk.setSize(vol.getInfo().allocation);
                disk.setVirtualSize(vol.getInfo().capacity);
                disk.setFormat(voldef.getFormat() == LibvirtStorageVolumeDef.VolumeFormat.QCOW2
                        ? PhysicalDiskFormat.QCOW2 : PhysicalDiskFormat.RAW);
                return disk;
            }
        } catch (LibvirtException refreshEx) {
            logger.debug("Pool refresh failed or volume still not found: {}", refreshEx.getMessage());
        }

        logger.info("Falling back to direct block device access for volume: {}", volumeUuid);
        return getPhysicalDiskViaDirectBlockDevice(volumeUuid, pool);
    }

    private String getVgName(String sourceDir) {
        String vgName = sourceDir;
        if (vgName.startsWith("/")) {
            String[] parts = vgName.split("/");
            List<String> tokens = Arrays.stream(parts)
                    .filter(s -> !s.isEmpty()).collect(Collectors.toList());

            vgName = tokens.size() > 1 ? tokens.get(1)
                    : tokens.size() == 1 ? tokens.get(0)
                      : "";
        }
        return vgName;
    }

    private String extractVgNameFromPool(KVMStoragePool pool) {
        String sourceDir = pool.getLocalPath();
        if (sourceDir == null || sourceDir.isEmpty()) {
            throw new CloudRuntimeException("CLVM pool sourceDir is not set, cannot determine VG name");
        }
        String vgName = getVgName(sourceDir);
        logger.debug("Using VG name: {} (from sourceDir: {})", vgName, sourceDir);
        return vgName;
    }

    /**
     * For CLVM volumes that exist in LVM but are not visible to libvirt,
     * access them directly via block device path.
     */
    private KVMPhysicalDisk getPhysicalDiskViaDirectBlockDevice(String volumeUuid, KVMStoragePool pool) {
        try {
            String vgName = extractVgNameFromPool(pool);

            verifyLvExistsInVg(volumeUuid, vgName);

            logger.info("Volume {} exists in LVM but not visible to libvirt, accessing directly", volumeUuid);

            String lvPath = findAccessibleDeviceNode(volumeUuid, vgName, pool);
            long size = getClvmVolumeSize(lvPath);

            KVMPhysicalDisk disk = createPhysicalDiskFromClvmLv(lvPath, volumeUuid, pool, size);
            ensureTemplateAccessibility(volumeUuid, lvPath, pool);

            return disk;
        } catch (CloudRuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Failed to access CLVM volume via direct block device: {}", volumeUuid, ex);
            throw new CloudRuntimeException(String.format("Could not find volume %s: %s", volumeUuid, ex.getMessage()));
        }
    }

    private void verifyLvExistsInVg(String volumeUuid, String vgName) {
        logger.debug("Checking if volume {} exists in VG {}", volumeUuid, vgName);
        Script checkLvCmd = new Script("/usr/sbin/lvs", 5000, logger);
        checkLvCmd.add("--noheadings");
        checkLvCmd.add("--unbuffered");
        checkLvCmd.add(vgName + "/" + volumeUuid);
        String checkResult = checkLvCmd.execute();
        if (checkResult != null) {
            throw new CloudRuntimeException(String.format("Storage volume not found: no storage vol with matching name '%s'", volumeUuid));
        }
    }

    private String findAccessibleDeviceNode(String volumeUuid, String vgName, KVMStoragePool pool) {
        String lvPath = "/dev/" + vgName + "/" + volumeUuid;
        File lvDevice = new File(lvPath);

        if (!lvDevice.exists()) {
            lvPath = tryDeviceMapperPath(volumeUuid, vgName);
            if (!new File(lvPath).exists()) {
                lvPath = handleMissingDeviceNode(volumeUuid, vgName, pool);
            }
        }

        return lvPath;
    }

    private String tryDeviceMapperPath(String volumeUuid, String vgName) {
        String vgNameEscaped = vgName.replace("-", "--");
        String volumeUuidEscaped = volumeUuid.replace("-", "--");
        return "/dev/mapper/" + vgNameEscaped + "-" + volumeUuidEscaped;
    }

    private String handleMissingDeviceNode(String volumeUuid, String vgName, KVMStoragePool pool) {
        if (StoragePoolType.CLVM_NG.equals(pool.getType()) && volumeUuid.startsWith("template-")) {
            return activateTemplateAndGetPath(volumeUuid, vgName);
        }
        throw new CloudRuntimeException(String.format("Could not find volume %s in VG %s - volume exists in LVM but device node not accessible", volumeUuid, vgName));
    }

    private String activateTemplateAndGetPath(String volumeUuid, String vgName) {
        logger.info("Template volume {} device node not found. Attempting to activate in shared mode.", volumeUuid);
        String templateLvPath = "/dev/" + vgName + "/" + volumeUuid;

        try {
            ensureTemplateLvInSharedMode(templateLvPath, false);

            String lvPath = findDeviceNodeAfterActivation(templateLvPath, volumeUuid, vgName);

            logger.info("Successfully activated template volume {} at {}", volumeUuid, lvPath);
            return lvPath;
        } catch (CloudRuntimeException e) {
            throw new CloudRuntimeException(String.format("Failed to activate template volume %s in VG %s: %s", volumeUuid, vgName, e.getMessage()), e);
        }
    }

    private String findDeviceNodeAfterActivation(String templateLvPath, String volumeUuid, String vgName) {
        File lvDevice = new File(templateLvPath);
        String lvPath = templateLvPath;

        if (!lvDevice.exists()) {
            String vgNameEscaped = vgName.replace("-", "--");
            String volumeUuidEscaped = volumeUuid.replace("-", "--");
            lvPath = "/dev/mapper/" + vgNameEscaped + "-" + volumeUuidEscaped;
            lvDevice = new File(lvPath);
        }

        if (!lvDevice.exists()) {
            logger.error("Template volume {} still not accessible after activation attempt", volumeUuid);
            throw new CloudRuntimeException(String.format("Could not activate template volume %s in VG %s - device node not accessible after activation", volumeUuid, vgName));
        }

        return lvPath;
    }

    private void ensureTemplateAccessibility(String volumeUuid, String lvPath, KVMStoragePool pool) {
        if (StoragePoolType.CLVM_NG.equals(pool.getType()) && volumeUuid.startsWith("template-")) {
            logger.info("Detected template volume {}. Ensuring it's activated in shared mode.", volumeUuid);
            ensureTemplateLvInSharedMode(lvPath, false);
        }
    }

    private long getClvmVolumeSize(String lvPath) {
        try {
            Script lvsCmd = new Script("/usr/sbin/lvs", 5000, logger);
            lvsCmd.add("--noheadings");
            lvsCmd.add("--units");
            lvsCmd.add("b");
            lvsCmd.add("-o");
            lvsCmd.add("lv_size");
            lvsCmd.add(lvPath);

            OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
            String result = lvsCmd.execute(parser);

            String output = (result == null) ? parser.getLines() : result;

            if (output != null && !output.isEmpty()) {
                String sizeStr = output.trim().replaceAll("[^0-9]", "");
                if (!sizeStr.isEmpty()) {
                    return Long.parseLong(sizeStr);
                }
            }
        } catch (Exception sizeEx) {
            logger.warn("Failed to get size for CLVM volume via lvs: {}", sizeEx.getMessage());
            File lvDevice = new File(lvPath);
            if (lvDevice.isFile()) {
                return lvDevice.length();
            }
        }
        return 0;
    }

    private KVMPhysicalDisk createPhysicalDiskFromClvmLv(String lvPath, String volumeUuid, KVMStoragePool pool, long size) {
        PhysicalDiskFormat diskFormat = StoragePoolType.CLVM_NG.equals(pool.getType())
                ? PhysicalDiskFormat.QCOW2 : PhysicalDiskFormat.RAW;

        logger.debug("{} pool detected, setting disk format to {} for volume {}", pool.getType(), diskFormat, volumeUuid);

        KVMPhysicalDisk disk = new KVMPhysicalDisk(lvPath, volumeUuid, pool);
        disk.setFormat(diskFormat);
        disk.setSize(size);
        disk.setVirtualSize(size);

        logger.info("Successfully accessed CLVM/CLVM_NG volume via direct block device: {} with format: {} and size: {} bytes",
                lvPath, diskFormat, size);
        return disk;
    }

    /**
     * Checks if a CLVM_NG QCOW2 volume has a backing file (template) and ensures it's activated in shared mode.
     */
    private void ensureClvmNgBackingFileAccessible(String volumeName, KVMStoragePool pool) {
        try {
            String vgName = getVgName(pool.getLocalPath());
            String volumePath = "/dev/" + vgName + "/" + volumeName;

            logger.debug("Checking if CLVM_NG volume {} has a backing file that needs activation", volumePath);

            Script qemuImgInfo = new Script("qemu-img", Duration.millis(10000), logger);
            qemuImgInfo.add("info");
            qemuImgInfo.add("--output=json");
            qemuImgInfo.add(volumePath);

            OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
            String result = qemuImgInfo.execute(parser);

            if (result == null && parser.getLines() != null && !parser.getLines().isEmpty()) {
                String jsonOutput = parser.getLines();

                if (jsonOutput.contains("\"backing-filename\"")) {
                    int backingStart = jsonOutput.indexOf("\"backing-filename\"");
                    if (backingStart > 0) {
                        int valueStart = jsonOutput.indexOf(":", backingStart);
                        if (valueStart > 0) {
                            valueStart = jsonOutput.indexOf("\"", valueStart) + 1;
                            int valueEnd = jsonOutput.indexOf("\"", valueStart);

                            if (valueEnd > valueStart) {
                                String backingFile = jsonOutput.substring(valueStart, valueEnd).trim();
                                if (!backingFile.isEmpty() && backingFile.startsWith("/dev/")) {
                                    logger.info("Volume {} has backing file: {}. Ensuring backing file is in shared mode.", volumePath, backingFile);
                                    ensureTemplateLvInSharedMode(backingFile, false);
                                }
                            }
                        }
                    }
                } else {
                    logger.debug("Volume {} does not have a backing file (full clone)", volumePath);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to check/activate backing file for volume {}: {}. VM deployment may fail if template is not accessible.",
                    volumeName, e.getMessage());
        }
    }

    private String getClvmBackingFile(KVMPhysicalDisk template, KVMStoragePool destPool) {
        String templateLvName = template.getName();
        KVMPhysicalDisk templateOnPrimary = null;

        try {
            templateOnPrimary = destPool.getPhysicalDisk(templateLvName);
        } catch (CloudRuntimeException e) {
            logger.warn("Template {} not found on CLVM_NG pool {}.", templateLvName, destPool.getUuid());
        }

        if (templateOnPrimary != null) {
            String backingFile = templateOnPrimary.getPath();
            logger.info("Using template on primary storage as backing file: {}", backingFile);
            ensureTemplateLvInSharedMode(backingFile);
            return backingFile;
        }

        logger.error("Template {} should be on primary storage before creating volumes from it", templateLvName);
        throw new CloudRuntimeException(String.format("Template not found on CLVM_NG primary storage: %s. Template must be copied to primary storage first.", templateLvName));
    }

    /**
     * Ensures a template LV is activated in shared mode so multiple VMs can use it as a backing file.
     */
    private void ensureTemplateLvInSharedMode(String templatePath, boolean throwOnFailure) {
        try {
            Script checkLvs = new Script("lvs", Duration.millis(5000), logger);
            checkLvs.add("--noheadings");
            checkLvs.add("-o", "lv_attr");
            checkLvs.add(templatePath);

            OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
            String result = checkLvs.execute(parser);

            if (result == null && parser.getLines() != null && !parser.getLines().isEmpty()) {
                String lvAttr = parser.getLines().trim();
                if (lvAttr.length() >= 6) {
                    boolean isActive = (lvAttr.indexOf('a') >= 0);
                    boolean isShared = (lvAttr.indexOf('s') >= 0);

                    if (!isShared || !isActive) {
                        logger.info("Template LV {} is not in shared mode (attr: {}). Activating in shared mode.", templatePath, lvAttr);
                        LibvirtComputingResource.setClvmVolumeToSharedMode(templatePath);
                    } else {
                        logger.debug("Template LV {} is already in shared mode (attr: {})", templatePath, lvAttr);
                    }
                }
            }
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = "Failed to check/ensure template LV shared mode for " + templatePath + ": " + e.getMessage();
            if (throwOnFailure) {
                throw new CloudRuntimeException(errorMsg, e);
            } else {
                logger.warn(errorMsg, e);
            }
        }
    }

    private void ensureTemplateLvInSharedMode(String templatePath) {
        ensureTemplateLvInSharedMode(templatePath, false);
    }

    private long getVgPhysicalExtentSize(String vgName) {
        final long DEFAULT_PE_SIZE = 4 * 1024 * 1024L;
        String warningMessage = String.format("Failed to get PE size for VG %s, defaulting to 4MiB", vgName);

        try {
            Script vgDisplay = new Script("vgdisplay", 300000, logger);
            vgDisplay.add("--units", "b");
            vgDisplay.add("-C");
            vgDisplay.add("--noheadings");
            vgDisplay.add("-o", "vg_extent_size");
            vgDisplay.add(vgName);

            OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
            String result = vgDisplay.execute(parser);

            if (result != null) {
                logger.warn("{}: {}", warningMessage, result);
                return DEFAULT_PE_SIZE;
            }

            String output = parser.getLines();
            if (output == null || output.trim().isEmpty()) {
                logger.warn("{}: empty output", warningMessage);
                return DEFAULT_PE_SIZE;
            }

            output = output.trim();
            if (output.endsWith("B")) {
                output = output.substring(0, output.length() - 1).trim();
            }

            long peSize = Long.parseLong(output);
            logger.debug("Physical Extent size for VG {} is {} bytes", vgName, peSize);
            return peSize;
        } catch (NumberFormatException e) {
            logger.warn("{}: failed to parse PE size", warningMessage, e);
        } catch (Exception e) {
            logger.warn("{}: {}", warningMessage, e.getMessage());
        }

        logger.info("Using default PE size for VG {}: {} bytes (4 MiB)", vgName, DEFAULT_PE_SIZE);
        return DEFAULT_PE_SIZE;
    }

    /**
     * Calculate LVM LV size for CLVM_NG volume allocation.
     */
    private long calculateClvmNgLvSize(long virtualSize, String vgName) {
        long peSize = getVgPhysicalExtentSize(vgName);

        long clusterSize = 64 * 1024L;
        long l2Multiplier = 4096L;

        long numDataClusters = (virtualSize + clusterSize - 1) / clusterSize;
        long numL2Clusters = (numDataClusters + l2Multiplier - 1) / l2Multiplier;
        long l2TableSize = numL2Clusters * clusterSize;
        long refcountTableSize = l2TableSize;

        long headerOverhead = 2 * 1024 * 1024L;
        long metadataOverhead = l2TableSize + refcountTableSize + headerOverhead;
        long targetSize = virtualSize + metadataOverhead;
        long roundedSize = ((targetSize + peSize - 1) / peSize) * peSize;
        long virtualSizeGiB = virtualSize / (1024 * 1024 * 1024L);
        long overheadMiB = metadataOverhead / (1024 * 1024L);

        logger.info("Calculated volume LV size: {} bytes (virtual: {} GiB, QCOW2 metadata overhead: {} MiB, rounded to {} PEs, PE size = {} bytes)",
                roundedSize, virtualSizeGiB, overheadMiB, roundedSize / peSize, peSize);

        return roundedSize;
    }

    private long getQcow2VirtualSize(String imagePath) {
        Script qemuImg = new Script("qemu-img", 300000, logger);
        qemuImg.add("info");
        qemuImg.add("--output=json");
        qemuImg.add(imagePath);

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = qemuImg.execute(parser);

        if (result != null) {
            throw new CloudRuntimeException("Failed to get QCOW2 virtual size for " + imagePath + ": " + result);
        }

        String output = parser.getLines();
        if (output == null || output.trim().isEmpty()) {
            throw new CloudRuntimeException("qemu-img info returned empty output for " + imagePath);
        }

        JsonObject info = JsonParser.parseString(output).getAsJsonObject();
        return info.get("virtual-size").getAsLong();
    }

    private long getQcow2PhysicalSize(String imagePath) {
        Script qemuImg = new Script("qemu-img", Duration.millis(300000), logger);
        qemuImg.add("info");
        qemuImg.add("--output=json");
        qemuImg.add(imagePath);

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = qemuImg.execute(parser);

        if (result != null) {
            throw new CloudRuntimeException("Failed to get QCOW2 physical size for " + imagePath + ": " + result);
        }

        String output = parser.getLines();
        if (output == null || output.trim().isEmpty()) {
            throw new CloudRuntimeException("qemu-img info returned empty output for " + imagePath);
        }

        JsonObject info = JsonParser.parseString(output).getAsJsonObject();
        return info.get("actual-size").getAsLong();
    }

    private KVMPhysicalDisk createClvmNgDiskWithBacking(String volumeUuid, int timeout, long virtualSize, String backingFile,
                                                         KVMStoragePool pool, Storage.ProvisioningType provisioningType) {
        String vgName = getVgName(pool.getLocalPath());
        long lvSize = calculateClvmNgLvSize(virtualSize, vgName);
        String volumePath = "/dev/" + vgName + "/" + volumeUuid;

        logger.debug("Creating CLVM_NG volume {} with LV size {} bytes (virtual size: {} bytes, provisioning: {})",
                volumeUuid, lvSize, virtualSize, provisioningType);

        Script lvcreate = new Script("lvcreate", Duration.millis(timeout), logger);
        lvcreate.add("-n", volumeUuid);
        lvcreate.add("-L", lvSize + "B");
        lvcreate.add("--yes");
        lvcreate.add(vgName);

        String result = lvcreate.execute();
        if (result != null) {
            throw new CloudRuntimeException("Failed to create LV for CLVM_NG volume: " + result);
        }

        Script qemuImg = new Script("qemu-img", Duration.millis(timeout), logger);
        qemuImg.add("create");
        qemuImg.add("-f", "qcow2");

        StringBuilder qcow2Options = new StringBuilder();
        String preallocation = (provisioningType == Storage.ProvisioningType.THIN) ? "off" : "metadata";
        qcow2Options.append("preallocation=").append(preallocation);
        qcow2Options.append(",extended_l2=on");
        qcow2Options.append(",cluster_size=64k");

        if (backingFile != null && !backingFile.isEmpty()) {
            qcow2Options.append(",backing_file=").append(backingFile);
            qcow2Options.append(",backing_fmt=qcow2");
            logger.debug("Creating CLVM_NG volume with backing file: {}", backingFile);
        }

        qemuImg.add("-o", qcow2Options.toString());
        qemuImg.add(volumePath);
        qemuImg.add(virtualSize + "");

        result = qemuImg.execute();
        if (result != null) {
            removeLvOnFailure(volumePath, timeout);
            throw new CloudRuntimeException("Failed to create QCOW2 on CLVM_NG volume: " + result);
        }

        long actualSize = getClvmVolumeSize(volumePath);
        KVMPhysicalDisk disk = new KVMPhysicalDisk(volumePath, volumeUuid, pool);
        disk.setFormat(PhysicalDiskFormat.QCOW2);
        disk.setSize(actualSize);
        disk.setVirtualSize(actualSize);

        logger.info("Successfully created CLVM_NG volume {} (LV size: {}, virtual size: {}, provisioning: {}, preallocation: {})",
                volumeUuid, lvSize, virtualSize, provisioningType, preallocation);

        return disk;
    }

    private boolean lvExists(String lvPath) {
        Script checkLv = new Script("lvs", Duration.millis(5000), logger);
        checkLv.add("--noheadings");
        checkLv.add("--unbuffered");
        checkLv.add(lvPath);
        return checkLv.execute() == null;
    }

    private void removeLvOnFailure(String lvPath, int timeout) {
        Script lvremove = new Script("lvremove", Duration.millis(timeout), logger);
        lvremove.add("-f");
        lvremove.add(lvPath);
        lvremove.execute();
    }

    private KVMPhysicalDisk createClvmVolume(String volumeName, long size, KVMStoragePool pool) {
        String vgName = getVgName(pool.getLocalPath());
        String volumePath = "/dev/" + vgName + "/" + volumeName;
        int timeout = 30000;

        logger.info("Creating CLVM volume {} in VG {} with size {} bytes", volumeName, vgName, size);

        Script lvcreate = new Script("lvcreate", Duration.millis(timeout), logger);
        lvcreate.add("-n", volumeName);
        lvcreate.add("-L", size + "B");
        lvcreate.add("--yes");
        lvcreate.add(vgName);

        String result = lvcreate.execute();
        if (result != null) {
            throw new CloudRuntimeException("Failed to create CLVM volume: " + result);
        }

        logger.info("Successfully created CLVM volume {} at {} with size {}", volumeName, volumePath, toHumanReadableSize(size));

        long actualSize = getClvmVolumeSize(volumePath);
        KVMPhysicalDisk disk = new KVMPhysicalDisk(volumePath, volumeName, pool);
        disk.setFormat(PhysicalDiskFormat.RAW);
        disk.setSize(actualSize);
        disk.setVirtualSize(actualSize);

        return disk;
    }

    @Override
    public boolean deletePhysicalDisk(String uuid, KVMStoragePool pool, Storage.ImageFormat format) {
        logger.info("CLVM/CLVM_NG pool detected - using direct LVM cleanup with secure zero-fill for volume {}", uuid);
        return cleanupCLVMVolume(uuid, pool);
    }

    /**
     * Clean up CLVM volume and its snapshots directly using LVM commands.
     */
    private boolean cleanupCLVMVolume(String uuid, KVMStoragePool pool) {
        logger.info("Starting direct LVM cleanup for CLVM volume: {} in pool: {}", uuid, pool.getUuid());

        try {
            String sourceDir = pool.getLocalPath();
            if (sourceDir == null || sourceDir.isEmpty()) {
                logger.debug("Source directory is null or empty, cannot determine VG name for CLVM pool {}, skipping direct cleanup", pool.getUuid());
                return true;
            }
            String vgName = getVgName(sourceDir);
            logger.info("Determined VG name: {} for pool: {}", vgName, pool.getUuid());

            if (vgName == null || vgName.isEmpty()) {
                logger.warn("Cannot determine VG name for CLVM pool {}, skipping direct cleanup", pool.getUuid());
                return true;
            }

            String lvPath = "/dev/" + vgName + "/" + uuid;
            logger.debug("Volume path: {}", lvPath);

            Script checkLvs = new Script("lvs", 5000, logger);
            checkLvs.add("--noheadings");
            checkLvs.add("--unbuffered");
            checkLvs.add(lvPath);

            logger.info("Checking if volume exists: lvs --noheadings --unbuffered {}", lvPath);
            String checkResult = checkLvs.execute();

            if (checkResult != null) {
                logger.info("CLVM volume {} does not exist in LVM (check returned: {}), considering it as already deleted", uuid, checkResult);
                return true;
            }

            logger.info("Volume {} exists, proceeding with cleanup", uuid);

            boolean secureZeroFillEnabled = shouldSecureZeroFill(pool);

            if (secureZeroFillEnabled) {
                logger.info("Step 1: Zero-filling volume {} for security", uuid);
                secureZeroFillVolume(lvPath, uuid);
            } else {
                logger.info("Secure zero-fill is disabled, skipping zero-filling for volume {}", uuid);
            }

            logger.info("Step 2: Removing volume {}", uuid);
            Script removeLv = new Script("lvremove", 10000, logger);
            removeLv.add("-f");
            removeLv.add(lvPath);

            logger.info("Executing command: lvremove -f {}", lvPath);
            String removeResult = removeLv.execute();

            if (removeResult == null) {
                logger.info("Successfully removed CLVM volume {} using direct LVM cleanup", uuid);
                return true;
            } else {
                logger.warn("Command 'lvremove -f {}' returned error: {}", lvPath, removeResult);
                if (removeResult.contains("not found") || removeResult.contains("Failed to find")) {
                    logger.info("CLVM volume {} not found during cleanup, considering it as already deleted", uuid);
                    return true;
                }
                return false;
            }
        } catch (Exception ex) {
            logger.error("Exception during CLVM volume cleanup for {}: {}", uuid, ex.getMessage(), ex);
            return true;
        }
    }

    private boolean shouldSecureZeroFill(KVMStoragePool pool) {
        Map<String, String> details = pool.getDetails();
        String secureZeroFillStr = (details != null) ? details.get(KVMStoragePool.CLVM_SECURE_ZERO_FILL) : null;
        return Boolean.parseBoolean(secureZeroFillStr);
    }

    /**
     * Securely zero-fill a volume before deletion to prevent data leakage.
     * Uses blkdiscard (fast TRIM) as primary method, with dd zero-fill as fallback.
     */
    private void secureZeroFillVolume(String lvPath, String volumeUuid) {
        logger.info("Starting secure zero-fill for CLVM volume: {} at path: {}", volumeUuid, lvPath);

        boolean blkdiscardSuccess = false;

        try {
            Script blkdiscard = new Script("blkdiscard", 300000, logger);
            blkdiscard.add("-f");
            blkdiscard.add(lvPath);

            String result = blkdiscard.execute();
            if (result == null) {
                logger.info("Successfully zero-filled CLVM volume {} using blkdiscard (TRIM)", volumeUuid);
                blkdiscardSuccess = true;
            } else {
                if (result.contains("Operation not supported") || result.contains("BLKDISCARD ioctl failed")) {
                    logger.info("blkdiscard not supported for volume {} (device doesn't support TRIM/DISCARD), using dd fallback", volumeUuid);
                } else {
                    logger.warn("blkdiscard failed for volume {}: {}, will try dd fallback", volumeUuid, result);
                }
            }
        } catch (Exception e) {
            logger.warn("Exception during blkdiscard for volume {}: {}, will try dd fallback", volumeUuid, e.getMessage());
        }

        if (!blkdiscardSuccess) {
            logger.info("Attempting zero-fill using dd for CLVM volume: {}", volumeUuid);
            try {
                String command = String.format(
                    "nice -n 19 ionice -c 2 -n 7 dd if=/dev/zero of=%s bs=1M oflag=direct 2>&1 || true",
                    lvPath
                );

                Script ddZeroFill = new Script("/bin/bash", 3600000, logger);
                ddZeroFill.add("-c");
                ddZeroFill.add(command);

                OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
                String ddResult = ddZeroFill.execute(parser);
                String output = parser.getLines();

                if (output != null && (output.contains("copied") || output.contains("records in") ||
                    output.contains("No space left on device"))) {
                    logger.info("Successfully zero-filled CLVM volume {} using dd", volumeUuid);
                } else if (ddResult == null) {
                    logger.info("Zero-fill completed for CLVM volume {}", volumeUuid);
                } else {
                    logger.warn("dd zero-fill for volume {} completed with output: {}", volumeUuid,
                        output != null ? output : ddResult);
                }
            } catch (Exception e) {
                logger.warn("Failed to zero-fill CLVM volume {} before deletion: {}. Proceeding with deletion anyway.",
                        volumeUuid, e.getMessage());
            }
        }
    }
}


