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
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import com.cloud.utils.script.OutputInterpreter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.utils.cryptsetup.KeyFile;
import org.apache.cloudstack.utils.qemu.QemuImageOptions;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.cloudstack.utils.qemu.QemuObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Duration;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.Secret;
import org.libvirt.StoragePool;
import org.libvirt.StoragePoolInfo.StoragePoolState;
import org.libvirt.StorageVol;

import com.ceph.rados.IoCTX;
import com.ceph.rados.Rados;
import com.ceph.rados.exceptions.ErrorCode;
import com.ceph.rados.exceptions.RadosException;
import com.ceph.rbd.Rbd;
import com.ceph.rbd.RbdException;
import com.ceph.rbd.RbdImage;
import com.ceph.rbd.jna.RbdImageInfo;
import com.ceph.rbd.jna.RbdSnapInfo;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtSecretDef;
import com.cloud.hypervisor.kvm.resource.LibvirtSecretDef.Usage;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolDef;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolDef.AuthenticationType;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolDef.PoolType;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtStorageVolumeDef;
import com.cloud.hypervisor.kvm.resource.LibvirtStorageVolumeDef.VolumeFormat;
import com.cloud.hypervisor.kvm.resource.LibvirtStorageVolumeXMLParser;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.utils.storage.TemplateDownloaderUtil;

public class LibvirtStorageAdaptor implements StorageAdaptor {
    protected Logger logger = LogManager.getLogger(getClass());
    private StorageLayer _storageLayer;
    private String _mountPoint = "/mnt";
    private String _manageSnapshotPath;
    private static final ConcurrentHashMap<String, Integer> storagePoolRefCounts = new ConcurrentHashMap<>();

    private String rbdTemplateSnapName = "cloudstack-base-snap";
    private static final int RBD_FEATURE_LAYERING = 1;
    private static final int RBD_FEATURE_EXCLUSIVE_LOCK = 4;
    private static final int RBD_FEATURE_OBJECT_MAP = 8;
    private static final int RBD_FEATURE_FAST_DIFF = 16;
    private static final int RBD_FEATURE_DEEP_FLATTEN = 32;
    public static final int RBD_FEATURES = RBD_FEATURE_LAYERING + RBD_FEATURE_EXCLUSIVE_LOCK + RBD_FEATURE_OBJECT_MAP + RBD_FEATURE_FAST_DIFF + RBD_FEATURE_DEEP_FLATTEN;
    private int rbdOrder = 0; /* Order 0 means 4MB blocks (the default) */

    private static final Set<StoragePoolType> poolTypesThatEnableCreateDiskFromTemplateBacking = new HashSet<>(Arrays.asList(StoragePoolType.NetworkFilesystem,
      StoragePoolType.Filesystem));

    public LibvirtStorageAdaptor(StorageLayer storage) {
        _storageLayer = storage;
        _manageSnapshotPath = Script.findScript("scripts/storage/qcow2/", "managesnapshot.sh");
    }

    @Override
    public boolean createFolder(String uuid, String path) {
        return createFolder(uuid, path, null);
    }

    @Override
    public boolean createFolder(String uuid, String path, String localPath) {
        String mountPoint = _mountPoint + File.separator + uuid;

        if (localPath != null) {
            logger.debug(String.format("Pool [%s] is of type local or shared mount point; therefore, we will use the local path [%s] to create the folder [%s] (if it does not"
                    + " exist).", uuid, localPath, path));

            mountPoint = localPath;
        }

        File f = new File(mountPoint + File.separator + path);
        if (!f.exists()) {
            f.mkdirs();
        }
        return true;
    }

    @Override
    public KVMPhysicalDisk createDiskFromTemplateBacking(KVMPhysicalDisk template, String name, PhysicalDiskFormat format, long size,
                                                         KVMStoragePool destPool, int timeout, byte[] passphrase) {
        String volumeDesc = String.format("volume [%s], with template backing [%s], in pool [%s] (%s), with size [%s] and encryption is %s", name, template.getName(), destPool.getUuid(),
          destPool.getType(), size, passphrase != null && passphrase.length > 0);

        if (!poolTypesThatEnableCreateDiskFromTemplateBacking.contains(destPool.getType())) {
            logger.info(String.format("Skipping creation of %s due to pool type is none of the following types %s.", volumeDesc, poolTypesThatEnableCreateDiskFromTemplateBacking.stream()
              .map(type -> type.toString()).collect(Collectors.joining(", "))));

            return null;
        }

        if (format != PhysicalDiskFormat.QCOW2) {
            logger.info(String.format("Skipping creation of %s due to format [%s] is not [%s].", volumeDesc, format, PhysicalDiskFormat.QCOW2));
            return null;
        }

        logger.info(String.format("Creating %s.", volumeDesc));

        String destPoolLocalPath = destPool.getLocalPath();
        String destPath = String.format("%s%s%s", destPoolLocalPath, destPoolLocalPath.endsWith("/") ? "" : "/", name);

        Map<String, String> options = new HashMap<>();
        List<QemuObject> passphraseObjects = new ArrayList<>();
        try (KeyFile keyFile = new KeyFile(passphrase)) {
            QemuImgFile destFile = new QemuImgFile(destPath, format);
            destFile.setSize(size);
            QemuImgFile backingFile = new QemuImgFile(template.getPath(), template.getFormat());

            if (keyFile.isSet()) {
                passphraseObjects.add(QemuObject.prepareSecretForQemuImg(format, QemuObject.EncryptFormat.LUKS, keyFile.toString(), "sec0", options));
            }
            logger.debug(String.format("Passphrase is staged to keyFile: %s", keyFile.isSet()));

            QemuImg qemu = new QemuImg(timeout);
            qemu.create(destFile, backingFile, options, passphraseObjects);
        } catch (QemuImgException | LibvirtException | IOException e) {
            // why don't we throw an exception here? I guess we fail to find the volume later and that results in a failure returned?
            logger.error(String.format("Failed to create %s in [%s] due to [%s].", volumeDesc, destPath, e.getMessage()), e);
        }

        return null;
    }

    /**
     * Extract downloaded template into installPath, remove compressed file
     */
    public static void extractDownloadedTemplate(String downloadedTemplateFile, KVMStoragePool destPool, String destinationFile) {
        String extractCommand = TemplateDownloaderUtil.getExtractCommandForDownloadedFile(downloadedTemplateFile, destinationFile);
        Script.runSimpleBashScript(extractCommand);
        Script.runSimpleBashScript("rm -f " + downloadedTemplateFile);
    }

    @Override
    public KVMPhysicalDisk createTemplateFromDirectDownloadFile(String templateFilePath, String destTemplatePath, KVMStoragePool destPool, Storage.ImageFormat format, int timeout) {
        File sourceFile = new File(templateFilePath);
        if (!sourceFile.exists()) {
            throw new CloudRuntimeException("Direct download template file " + sourceFile + " does not exist on this host");
        }
        String templateUuid = UUID.randomUUID().toString();
        if (Storage.ImageFormat.ISO.equals(format)) {
            templateUuid += ".iso";
        }
        String destinationFile = destPool.getLocalPath() + File.separator + templateUuid;

        if (destPool.getType() == StoragePoolType.NetworkFilesystem || destPool.getType() == StoragePoolType.Filesystem
            || destPool.getType() == StoragePoolType.SharedMountPoint) {
            if (!Storage.ImageFormat.ISO.equals(format) && TemplateDownloaderUtil.isTemplateExtractable(templateFilePath)) {
                extractDownloadedTemplate(templateFilePath, destPool, destinationFile);
            } else {
                Script.runSimpleBashScript("mv " + templateFilePath + " " + destinationFile);
            }
        } else if (destPool.getType() == StoragePoolType.RBD) {
            String temporaryExtractFilePath = sourceFile.getParent() + File.separator + templateUuid;
            extractDownloadedTemplate(templateFilePath, destPool, temporaryExtractFilePath);
            createTemplateOnRBDFromDirectDownloadFile(temporaryExtractFilePath, templateUuid, destPool, timeout);
        }
        return destPool.getPhysicalDisk(templateUuid);
    }

    private void createTemplateOnRBDFromDirectDownloadFile(String srcTemplateFilePath, String templateUuid, KVMStoragePool destPool, int timeout) {
        try {
            QemuImg.PhysicalDiskFormat srcFileFormat = QemuImg.PhysicalDiskFormat.QCOW2;
            QemuImgFile srcFile = new QemuImgFile(srcTemplateFilePath, srcFileFormat);
            QemuImg qemu = new QemuImg(timeout);
            Map<String, String> info = qemu.info(srcFile);
            Long virtualSize = Long.parseLong(info.get(QemuImg.VIRTUAL_SIZE));
            KVMPhysicalDisk destDisk = new KVMPhysicalDisk(destPool.getSourceDir() + "/" + templateUuid, templateUuid, destPool);
            destDisk.setFormat(PhysicalDiskFormat.RAW);
            destDisk.setSize(virtualSize);
            destDisk.setVirtualSize(virtualSize);
            QemuImgFile destFile = new QemuImgFile(KVMPhysicalDisk.RBDStringBuilder(destPool, destDisk.getPath()));
            destFile.setFormat(PhysicalDiskFormat.RAW);
            qemu.convert(srcFile, destFile);
        } catch (LibvirtException | QemuImgException e) {
            String err = String.format("Error creating template from direct download file on pool %s: %s", destPool.getUuid(), e.getMessage());
            logger.error(err, e);
            throw new CloudRuntimeException(err, e);
        }
    }

    public StorageVol getVolume(StoragePool pool, String volName) {
        if (pool == null) {
            logger.debug("LibVirt StoragePool is null (likely CLVM/CLVM_NG virtual pool), cannot lookup volume {} via libvirt", volName);
            return null;
        }

        StorageVol vol = null;

        try {
            vol = pool.storageVolLookupByName(volName);
        } catch (LibvirtException e) {
            logger.debug("Could not find volume " + volName + ": " + e.getMessage());
        }

        /**
         * The volume was not found in the storage pool
         * This can happen when a volume has just been created on a different host and
         * since then the libvirt storage pool has not been refreshed.
         */
        if (vol == null) {
            try {
                logger.debug("Refreshing storage pool " + pool.getName());
                refreshPool(pool);
            } catch (LibvirtException e) {
                logger.debug("Failed to refresh storage pool: " + e.getMessage());
            }

            try {
                vol = pool.storageVolLookupByName(volName);
                if (vol != null) {
                    logger.debug("Found volume " + volName + " in storage pool " + pool.getName() + " after refreshing the pool");
                }
            } catch (LibvirtException e) {
                logger.debug("Volume " + volName + " still not found after pool refresh: " + e.getMessage());
                return null;
            }
        }

        return vol;
    }

    public StorageVol createVolume(Connect conn, StoragePool pool, String uuid, long size, VolumeFormat format) throws LibvirtException {
        LibvirtStorageVolumeDef volDef = new LibvirtStorageVolumeDef(UUID.randomUUID().toString(), size, format, null, null);
        logger.debug(volDef.toString());

        return pool.storageVolCreateXML(volDef.toString(), 0);
    }

    public void storagePoolRefresh(StoragePool pool) {
        try {
            synchronized (getStoragePool(pool.getUUIDString())) {
                refreshPool(pool);
            }
        } catch (LibvirtException e) {
            logger.debug("refresh storage pool failed: " + e.toString());
        }
    }

    private void checkNetfsStoragePoolMounted(String uuid) {
        String targetPath = _mountPoint + File.separator + uuid;
        int mountpointResult = Script.runSimpleBashScriptForExitValue("mountpoint -q " + targetPath);
        if (mountpointResult != 0) {
            String errMsg = String.format("libvirt failed to mount storage pool %s at %s", uuid, targetPath);
            logger.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
    }

    private StoragePool createNetfsStoragePool(PoolType fsType, Connect conn, String uuid, String host, String path, List<String> nfsMountOpts) throws LibvirtException {
        String targetPath = _mountPoint + File.separator + uuid;
        LibvirtStoragePoolDef spd = new LibvirtStoragePoolDef(fsType, uuid, uuid, host, path, targetPath, nfsMountOpts);
        _storageLayer.mkdir(targetPath);
        StoragePool sp = null;
        try {
            logger.debug(spd.toString());
            // check whether the pool is already mounted
            int mountpointResult = Script.runSimpleBashScriptForExitValue("mountpoint -q " + targetPath);
            // if the pool is mounted, try to unmount it
            if(mountpointResult == 0) {
                logger.info("Attempting to unmount old mount at " + targetPath);
                String result = Script.runSimpleBashScript("umount -l " + targetPath);
                if (result == null) {
                    logger.info("Succeeded in unmounting " + targetPath);
                } else {
                    logger.error("Failed in unmounting storage");
                }
            }

            sp = conn.storagePoolCreateXML(spd.toString(), 0);
            return sp;
        } catch (LibvirtException e) {
            logger.error(e.toString());
            throw e;
        }
    }

    private StoragePool createSharedStoragePool(Connect conn, String uuid, String host, String path) {
        String mountPoint = path;
        if (!_storageLayer.exists(mountPoint)) {
            logger.error(mountPoint + " does not exists. Check local.storage.path in agent.properties.");
            return null;
        }
        LibvirtStoragePoolDef spd = new LibvirtStoragePoolDef(PoolType.DIR, uuid, uuid, host, path, path);
        StoragePool sp = null;
        try {
            logger.debug(spd.toString());
            sp = conn.storagePoolCreateXML(spd.toString(), 0);
            return sp;
        } catch (LibvirtException e) {
            logger.error(e.toString());
            if (sp != null) {
                try {
                    if (sp.isPersistent() == 1) {
                        sp.destroy();
                        sp.undefine();
                    } else {
                        sp.destroy();
                    }
                    sp.free();
                } catch (LibvirtException l) {
                    logger.debug("Failed to define shared mount point storage pool with: " + l.toString());
                }
            }
            return null;
        }
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
            logger.info("Created libvirt pool definition for CLVM/CLVM_NG VG: {} (pool will remain inactive, we use direct LVM access)", volgroupName);
            pool.setAutostart(1);
            return pool;
        } catch (LibvirtException e) {
            logger.warn("Failed to define CLVM/CLVM_NG pool in libvirt: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Set pool capacity statistics from VG stats array.
     * Extracts capacity and available from VG stats and calculates used space.
     *
     * @param pool The storage pool to update
     * @param vgStats Array containing [capacity, available] in bytes from getVgStats()
     * @param vgName The VG name for logging purposes
     */
    private void setPoolCapacityFromVgStats(LibvirtStoragePool pool, long[] vgStats, String vgName) {
        long capacity = vgStats[0];
        long available = vgStats[1];
        long used = capacity - available;

        pool.setCapacity(capacity);
        pool.setUsed(used);
        pool.setAvailable(available);

        logger.debug("CLVM/CLVM_NG pool {} - Capacity: {}, Used: {}, Available: {}",
                vgName, toHumanReadableSize(capacity), toHumanReadableSize(used), toHumanReadableSize(available));
    }

    /**
     * Get VG statistics (capacity and available) using direct LVM commands.
     *
     * @param vgName The volume group name
     * @return long array [capacity, available] in bytes
     * @throws CloudRuntimeException if VG statistics cannot be retrieved or parsed
     */
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


    private List<String> getNFSMountOptsFromDetails(StoragePoolType type, Map<String, String> details) {
        List<String> nfsMountOpts = null;
        if (!type.equals(StoragePoolType.NetworkFilesystem) || details == null) {
            return nfsMountOpts;
        }
        if (details.containsKey(ApiConstants.NFS_MOUNT_OPTIONS)) {
            nfsMountOpts = Arrays.asList(details.get(ApiConstants.NFS_MOUNT_OPTIONS).replaceAll("\\s", "").split(","));
        }
        return nfsMountOpts;
    }

    private boolean destroyStoragePoolOnNFSMountOptionsChange(StoragePool sp, Connect conn, List<String> nfsMountOpts) {
        try {
            LibvirtStoragePoolDef poolDef = getStoragePoolDef(conn, sp);
            Set poolNfsMountOpts = poolDef.getNfsMountOpts();
            boolean mountOptsDiffer = false;
            if (poolNfsMountOpts.size() != nfsMountOpts.size()) {
                mountOptsDiffer = true;
            } else {
                for (String nfsMountOpt : nfsMountOpts) {
                    if (!poolNfsMountOpts.contains(nfsMountOpt)) {
                        mountOptsDiffer = true;
                        break;
                    }
                }
            }
            if (mountOptsDiffer) {
                sp.destroy();
                return true;
            }
        } catch (LibvirtException e) {
            logger.error("Failure in destroying the pre-existing storage pool for changing the NFS mount options" + e);
        }
        return false;
    }

    private StoragePool createRBDStoragePool(Connect conn, String uuid, String host, int port, String userInfo, String path) {

        LibvirtStoragePoolDef spd;
        StoragePool sp = null;
        Secret s = null;

        String[] userInfoTemp = userInfo.split(":");
        if (userInfoTemp.length == 2) {
            LibvirtSecretDef sd = new LibvirtSecretDef(Usage.CEPH, uuid);

            sd.setCephName(userInfoTemp[0] + "@" + host + ":" + port + "/" + path);

            try {
                logger.debug(sd.toString());
                s = conn.secretDefineXML(sd.toString());
                s.setValue(Base64.decodeBase64(userInfoTemp[1]));
            } catch (LibvirtException e) {
                logger.error("Failed to define the libvirt secret: " + e.toString());
                if (s != null) {
                    try {
                        s.undefine();
                        s.free();
                    } catch (LibvirtException l) {
                        logger.error("Failed to undefine the libvirt secret: " + l.toString());
                    }
                }
                return null;
            }
            spd = new LibvirtStoragePoolDef(PoolType.RBD, uuid, uuid, host, port, path, userInfoTemp[0], AuthenticationType.CEPH, uuid);
        } else {
            spd = new LibvirtStoragePoolDef(PoolType.RBD, uuid, uuid, host, port, path, "");
        }

        try {
            logger.debug(spd.toString());
            sp = conn.storagePoolCreateXML(spd.toString(), 0);
            return sp;
        } catch (LibvirtException e) {
            logger.error("Failed to create RBD storage pool: " + e.toString());
            if (sp != null) {
                try {
                    if (sp.isPersistent() == 1) {
                        sp.destroy();
                        sp.undefine();
                    } else {
                        sp.destroy();
                    }
                    sp.free();
                } catch (LibvirtException l) {
                    logger.error("Failed to undefine RBD storage pool: " + l.toString());
                }
            }

            if (s != null) {
                try {
                    logger.error("Failed to create the RBD storage pool, cleaning up the libvirt secret");
                    s.undefine();
                    s.free();
                } catch (LibvirtException se) {
                    logger.error("Failed to remove the libvirt secret: " + se.toString());
                }
            }

            return null;
        }
    }

    public StorageVol copyVolume(StoragePool destPool, LibvirtStorageVolumeDef destVol, StorageVol srcVol, int timeout) throws LibvirtException {
        StorageVol vol = destPool.storageVolCreateXML(destVol.toString(), 0);
        String srcPath = srcVol.getKey();
        String destPath = vol.getKey();
        Script.runSimpleBashScript("cp " + srcPath + " " + destPath, timeout);
        return vol;
    }

    public boolean copyVolume(String srcPath, String destPath, String volumeName, int timeout) throws InternalErrorException {
        _storageLayer.mkdirs(destPath);
        if (!_storageLayer.exists(srcPath)) {
            throw new InternalErrorException("volume:" + srcPath + " is not exits");
        }
        String result = Script.runSimpleBashScript("cp " + srcPath + " " + destPath + File.separator + volumeName, timeout);
        return result == null;
    }

    public LibvirtStoragePoolDef getStoragePoolDef(Connect conn, StoragePool pool) throws LibvirtException {
        String poolDefXML = pool.getXMLDesc(0);
        LibvirtStoragePoolXMLParser parser = new LibvirtStoragePoolXMLParser();
        return parser.parseStoragePoolXML(poolDefXML);
    }

    public LibvirtStorageVolumeDef getStorageVolumeDef(Connect conn, StorageVol vol) throws LibvirtException {
        String volDefXML = vol.getXMLDesc(0);
        LibvirtStorageVolumeXMLParser parser = new LibvirtStorageVolumeXMLParser();
        return parser.parseStorageVolumeXML(volDefXML);
    }

    @Override
    public StoragePoolType getStoragePoolType() {
        // This is mapped manually in KVMStoragePoolManager
        return  null;
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid) {
        return this.getStoragePool(uuid, false);
    }

    protected void updateLocalPoolIops(LibvirtStoragePool pool) {
        if (!StoragePoolType.Filesystem.equals(pool.getType()) || StringUtils.isBlank(pool.getLocalPath())) {
            return;
        }
        logger.trace("Updating used IOPS for pool: {}", pool.getName());

        // Run script to get data
        List<String[]> commands = new ArrayList<>();
        commands.add(new String[]{
                Script.getExecutableAbsolutePath("bash"),
                "-c",
                String.format(
                        "%s %s | %s 'NR==2 {print $1}'",
                        Script.getExecutableAbsolutePath("df"),
                        pool.getLocalPath(),
                        Script.getExecutableAbsolutePath("awk")
                )
        });
        String result = Script.executePipedCommands(commands, 1000).second();
        if (StringUtils.isBlank(result)) {
            return;
        }
        result = result.trim();
        commands.add(new String[]{
                Script.getExecutableAbsolutePath("bash"),
                "-c",
                String.format(
                        "%s -z %s 1 2 | %s 'NR==7 {print $2}'",
                        Script.getExecutableAbsolutePath("iostat"),
                        result,
                        Script.getExecutableAbsolutePath("awk")
                )
        });
        result = Script.executePipedCommands(commands, 10000).second();
        logger.trace("Pool used IOPS result: {}", result);
        if (StringUtils.isBlank(result)) {
            return;
        }
        try {
            double doubleValue = Double.parseDouble(result);
            pool.setUsedIops((long) doubleValue);
            logger.debug("Updated used IOPS: {} for pool: {}", pool.getUsedIops(), pool.getName());
        } catch (NumberFormatException e) {
            logger.warn(String.format("Unable to parse retrieved used IOPS: %s for pool: %s", result,
                    pool.getName()));
        }
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid, boolean refreshInfo) {
        logger.info("Trying to fetch storage pool " + uuid + " from libvirt");
        StoragePool storage = null;
        try {
            Connect conn = LibvirtConnection.getConnection();
            storage = conn.storagePoolLookupByUUIDString(uuid);

            LibvirtStoragePoolDef spd = getStoragePoolDef(conn, storage);
            if (spd == null) {
                throw new CloudRuntimeException("Unable to parse the storage pool definition for storage pool " + uuid);
            }

            StoragePoolType type = null;
            if (spd.getPoolType() == LibvirtStoragePoolDef.PoolType.NETFS) {
                type = StoragePoolType.NetworkFilesystem;
            } else if (spd.getPoolType() == LibvirtStoragePoolDef.PoolType.DIR) {
                type = StoragePoolType.Filesystem;
            } else if (spd.getPoolType() == LibvirtStoragePoolDef.PoolType.RBD) {
                type = StoragePoolType.RBD;
            } else if (spd.getPoolType() == LibvirtStoragePoolDef.PoolType.LOGICAL) {
                type = StoragePoolType.CLVM;
            } else if (spd.getPoolType() == LibvirtStoragePoolDef.PoolType.GLUSTERFS) {
                type = StoragePoolType.Gluster;
            } else if (spd.getPoolType() == LibvirtStoragePoolDef.PoolType.POWERFLEX) {
                type = StoragePoolType.PowerFlex;
            }

            // Skip pool activation for CLVM/CLVM_NG - we keep them inactive and use direct LVM commands
            if (storage.getInfo().state != StoragePoolState.VIR_STORAGE_POOL_RUNNING && type != StoragePoolType.CLVM && type != StoragePoolType.CLVM_NG) {
                logger.warn("Storage pool " + uuid + " is not in running state. Attempting to start it.");
                storage.create(0);
            }

            LibvirtStoragePool pool = new LibvirtStoragePool(uuid, storage.getName(), type, this, storage);

            if (pool.getType() != StoragePoolType.RBD && pool.getType() != StoragePoolType.PowerFlex)
                pool.setLocalPath(spd.getTargetPath());
            else
                pool.setLocalPath("");

            if (pool.getType() == StoragePoolType.RBD
                    || pool.getType() == StoragePoolType.Gluster) {
                pool.setSourceHost(spd.getSourceHost());
                pool.setSourcePort(spd.getSourcePort());
                pool.setSourceDir(spd.getSourceDir());
                String authUsername = spd.getAuthUserName();
                if (authUsername != null) {
                    Secret secret = conn.secretLookupByUUIDString(spd.getSecretUUID());
                    String secretValue = new String(Base64.encodeBase64(secret.getByteValue()), Charset.defaultCharset());
                    pool.setAuthUsername(authUsername);
                    pool.setAuthSecret(secretValue);
                }
            }

            /**
             * On large (RBD) storage pools it can take up to a couple of minutes
             * for libvirt to refresh the pool.
             *
             * Refreshing a storage pool means that libvirt will have to iterate the whole pool
             * and fetch information of each volume in there
             *
             * It is not always required to refresh a pool. So we can control if we want to or not
             *
             * By default only the getStorageStats call in the LibvirtComputingResource will ask to
             * refresh the pool
             */
            if (refreshInfo) {
                logger.info("Asking libvirt to refresh storage pool " + uuid);
                pool.refresh();
            }

            if (type == StoragePoolType.CLVM || type == StoragePoolType.CLVM_NG) {
                logger.debug("Getting capacity for CLVM/CLVM_NG pool {} using direct LVM commands", uuid);
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
            } else {
                pool.setCapacity(storage.getInfo().capacity);
                pool.setUsed(storage.getInfo().allocation);
                pool.setAvailable(storage.getInfo().available);

                logger.debug("Successfully refreshed pool {} Capacity: {} Used: {} Available: {}",
                        uuid, toHumanReadableSize(storage.getInfo().capacity),
                        toHumanReadableSize(storage.getInfo().allocation),
                        toHumanReadableSize(storage.getInfo().available));
            }

            updateLocalPoolIops(pool);

            return pool;
        } catch (LibvirtException e) {
            logger.debug("Could not find storage pool " + uuid + " in libvirt");
            throw new CloudRuntimeException(e.toString(), e);
        }
    }

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String volumeUuid, KVMStoragePool pool) {
        LibvirtStoragePool libvirtPool = (LibvirtStoragePool)pool;
        boolean isClvmPool = (pool.getType() == StoragePoolType.CLVM || pool.getType() == StoragePoolType.CLVM_NG);

        // For CLVM pools without libvirt backing, use direct block device access immediately
        if (isClvmPool && libvirtPool.getPool() == null) {
            logger.debug("CLVM/CLVM_NG pool has no libvirt backing, using direct block device access for volume: {}", volumeUuid);
            return getPhysicalDiskViaDirectBlockDevice(volumeUuid, pool);
        }

        try {
            StorageVol vol = getVolume(libvirtPool.getPool(), volumeUuid);
            if (vol == null) {
                logger.debug("Volume " + volumeUuid + " not found in libvirt, will check for CLVM/CLVM_NG fallback");
                if (isClvmPool) {
                    return getPhysicalDiskWithClvmFallback(volumeUuid, pool, libvirtPool);
                }

                throw new CloudRuntimeException("Volume " + volumeUuid + " not found in libvirt pool");
            }

            KVMPhysicalDisk disk;
            LibvirtStorageVolumeDef voldef = getStorageVolumeDef(libvirtPool.getPool().getConnect(), vol);
            disk = new KVMPhysicalDisk(vol.getPath(), vol.getName(), pool);
            disk.setSize(vol.getInfo().allocation);
            disk.setVirtualSize(vol.getInfo().capacity);

            /**
             * libvirt returns format = 'unknow', so we have to force
             * the format to RAW for RBD storage volumes
             */
            if (pool.getType() == StoragePoolType.RBD) {
                disk.setFormat(PhysicalDiskFormat.RAW);
            } else if (voldef.getFormat() == null) {
                File diskDir = new File(disk.getPath());
                if (diskDir.exists() && diskDir.isDirectory()) {
                    disk.setFormat(PhysicalDiskFormat.DIR);
                } else if (volumeUuid.endsWith("tar") || volumeUuid.endsWith(("TAR"))) {
                    disk.setFormat(PhysicalDiskFormat.TAR);
                } else if (volumeUuid.endsWith("raw") || volumeUuid.endsWith(("RAW"))) {
                    disk.setFormat(PhysicalDiskFormat.RAW);
                } else {
                    disk.setFormat(pool.getDefaultFormat());
                }
            } else if (voldef.getFormat() == LibvirtStorageVolumeDef.VolumeFormat.QCOW2) {
                disk.setFormat(PhysicalDiskFormat.QCOW2);
            } else if (voldef.getFormat() == LibvirtStorageVolumeDef.VolumeFormat.RAW) {
                disk.setFormat(PhysicalDiskFormat.RAW);
            }
            return disk;
        } catch (LibvirtException e) {
            logger.debug("Failed to get volume from libvirt: " + e.getMessage());
            if (isClvmPool) {
                return getPhysicalDiskWithClvmFallback(volumeUuid, pool, libvirtPool);
            }

            throw new CloudRuntimeException(e.toString());
        }
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
                KVMPhysicalDisk disk;
                LibvirtStorageVolumeDef voldef = getStorageVolumeDef(libvirtPool.getPool().getConnect(), vol);
                disk = new KVMPhysicalDisk(vol.getPath(), vol.getName(), pool);
                disk.setSize(vol.getInfo().allocation);
                disk.setVirtualSize(vol.getInfo().capacity);
                disk.setFormat(voldef.getFormat() == LibvirtStorageVolumeDef.VolumeFormat.QCOW2 ?
                        PhysicalDiskFormat.QCOW2 : PhysicalDiskFormat.RAW);
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
            throw new CloudRuntimeException(String.format("Could not find volume %s: %s ", volumeUuid, ex.getMessage()));
        }
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

    private void verifyLvExistsInVg(String volumeUuid, String vgName) {
        logger.debug("Checking if volume {} exists in VG {}", volumeUuid, vgName);
        Script checkLvCmd = new Script("/usr/sbin/lvs", 5000, logger);
        checkLvCmd.add("--noheadings");
        checkLvCmd.add("--unbuffered");
        checkLvCmd.add(vgName + "/" + volumeUuid);

        String checkResult = checkLvCmd.execute();
        if (checkResult != null) {
            logger.debug("Volume {} does not exist in VG {}: {}", volumeUuid, vgName, checkResult);
            throw new CloudRuntimeException(String.format("Storage volume not found: no storage vol with matching name '%s'", volumeUuid));
        }
    }

    private String findAccessibleDeviceNode(String volumeUuid, String vgName, KVMStoragePool pool) {
        String lvPath = "/dev/" + vgName + "/" + volumeUuid;
        File lvDevice = new File(lvPath);

        if (!lvDevice.exists()) {
            lvPath = tryDeviceMapperPath(volumeUuid, vgName, lvDevice);

            if (!lvDevice.exists()) {
                lvPath = handleMissingDeviceNode(volumeUuid, vgName, pool);
            }
        }

        return lvPath;
    }

    private String tryDeviceMapperPath(String volumeUuid, String vgName, File lvDevice) {
        String vgNameEscaped = vgName.replace("-", "--");
        String volumeUuidEscaped = volumeUuid.replace("-", "--");
        String mapperPath = "/dev/mapper/" + vgNameEscaped + "-" + volumeUuidEscaped;
        File mapperDevice = new File(mapperPath);

        if (!mapperDevice.exists()) {
            lvDevice = mapperDevice;
        }

        return mapperPath;
    }

    private String handleMissingDeviceNode(String volumeUuid, String vgName, KVMStoragePool pool) {
        if (pool.getType() == StoragePoolType.CLVM_NG && volumeUuid.startsWith("template-")) {
            return activateTemplateAndGetPath(volumeUuid, vgName);
        } else {
            logger.warn("Volume exists in LVM but device node not found: {}", volumeUuid);
            throw new CloudRuntimeException(String.format("Could not find volume %s " +
                    "in VG %s - volume exists in LVM but device node not accessible", volumeUuid, vgName));
        }
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
            throw new CloudRuntimeException(String.format("Failed to activate template volume %s " +
                    "in VG %s: %s", volumeUuid, vgName, e.getMessage()), e);
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
            throw new CloudRuntimeException(String.format("Could not activate template volume %s " +
                    "in VG %s - device node not accessible even after activation", volumeUuid, vgName));
        }

        return lvPath;
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

    private KVMPhysicalDisk createPhysicalDiskFromClvmLv(String lvPath, String volumeUuid,
                                                          KVMStoragePool pool, long size) {
        KVMPhysicalDisk disk = new KVMPhysicalDisk(lvPath, volumeUuid, pool);

        PhysicalDiskFormat diskFormat = (pool.getType() == StoragePoolType.CLVM_NG)
                ? PhysicalDiskFormat.QCOW2
                : PhysicalDiskFormat.RAW;

        logger.debug("{} pool detected, setting disk format to {} for volume {}",
                pool.getType(), diskFormat, volumeUuid);

        disk.setFormat(diskFormat);
        disk.setSize(size);
        disk.setVirtualSize(size);

        logger.info("Successfully accessed CLVM/CLVM_NG volume via direct block device: {} " +
                "with format: {} and size: {} bytes", lvPath, diskFormat, size);

        return disk;
    }

    private void ensureTemplateAccessibility(String volumeUuid, String lvPath, KVMStoragePool pool) {
        if (pool.getType() == StoragePoolType.CLVM_NG && volumeUuid.startsWith("template-")) {
            logger.info("Detected template volume {}. Ensuring it's activated in shared mode for backing file access.",
                    volumeUuid);
            ensureTemplateLvInSharedMode(lvPath, false);
        }
    }

    /**
     * adjust refcount
     */
    private int adjustStoragePoolRefCount(String uuid, int adjustment) {
        final String mutexKey = storagePoolRefCounts.keySet().stream()
                .filter(k -> k.equals(uuid))
                .findFirst()
                .orElse(uuid);
        synchronized (mutexKey) {
            // some access on the storagePoolRefCounts.key(mutexKey) element
            int refCount = storagePoolRefCounts.computeIfAbsent(mutexKey, k -> 0);
            refCount += adjustment;
            if (refCount < 1) {
                storagePoolRefCounts.remove(mutexKey);
            } else {
                storagePoolRefCounts.put(mutexKey, refCount);
            }
            return refCount;
        }
    }
    /**
     * Thread-safe increment storage pool usage refcount
     * @param uuid UUID of the storage pool to increment the count
     */
    private void incStoragePoolRefCount(String uuid) {
        adjustStoragePoolRefCount(uuid, 1);
    }
    /**
     * Thread-safe decrement storage pool usage refcount for the given uuid and return if storage pool still in use.
     * @param uuid UUID of the storage pool to decrement the count
     * @return true if the storage pool is still used, else false.
     */
    private boolean decStoragePoolRefCount(String uuid) {
        return adjustStoragePoolRefCount(uuid, -1) > 0;
    }

    @Override
    public KVMStoragePool createStoragePool(String name, String host, int port, String path, String userInfo, StoragePoolType type, Map<String, String> details, boolean isPrimaryStorage) {
        logger.info("Attempting to create storage pool {} ({}) in libvirt", name, type);
        StoragePool sp;
        Connect conn;
        try {
            conn = LibvirtConnection.getConnection();
        } catch (LibvirtException e) {
            throw new CloudRuntimeException(e.toString());
        }

        try {
            sp = conn.storagePoolLookupByUUIDString(name);
            if (sp != null && sp.isActive() == 0) {
                sp.undefine();
                sp = null;
                logger.info("Found existing defined storage pool " + name + ". It wasn't running, so we undefined it.");
            }
            if (sp != null) {
                logger.info("Found existing defined storage pool " + name + ", using it.");
            }
        } catch (LibvirtException e) {
            sp = null;
            logger.warn("Storage pool " + name + " was not found running in libvirt. Need to create it.");
        }

        // libvirt strips trailing slashes off of path, we will too in order to match
        // existing paths
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if (sp == null) {
            // see if any existing pool by another name is using our storage path.
            // if anyone is, undefine the pool so we can define it as requested.
            // This should be safe since a pool in use can't be removed, and no
            // volumes are affected by unregistering the pool with libvirt.
            logger.info("Didn't find an existing storage pool " + name + " by UUID, checking for pools with duplicate paths");

            try {
                String[] poolnames = conn.listStoragePools();
                for (String poolname : poolnames) {
                    logger.debug("Checking path of existing pool " + poolname + " against pool we want to create");
                    StoragePool p = conn.storagePoolLookupByName(poolname);
                    LibvirtStoragePoolDef pdef = getStoragePoolDef(conn, p);
                    if (pdef == null) {
                        throw new CloudRuntimeException("Unable to parse the storage pool definition for storage pool " + poolname);
                    }

                    String targetPath = pdef.getTargetPath();
                    if (targetPath != null && targetPath.equals(path)) {
                        logger.debug("Storage pool utilizing path '" + path + "' already exists as pool " + poolname +
                                ", undefining so we can re-define with correct name " + name);
                        if (p.isPersistent() == 1) {
                            p.destroy();
                            p.undefine();
                        } else {
                            p.destroy();
                        }
                    }
                }
            } catch (LibvirtException e) {
                logger.error("Failure in attempting to see if an existing storage pool might be using the path of the pool to be created:" + e);
            }
        }

        List<String> nfsMountOpts = getNFSMountOptsFromDetails(type, details);
        if (sp != null && CollectionUtils.isNotEmpty(nfsMountOpts) &&
            destroyStoragePoolOnNFSMountOptionsChange(sp, conn, nfsMountOpts)) {
            sp = null;
        }

        if (sp == null) {

            logger.debug("Attempting to create storage pool " + name);

            if (type == StoragePoolType.NetworkFilesystem) {
                try {
                    sp = createNetfsStoragePool(PoolType.NETFS, conn, name, host, path, nfsMountOpts);
                } catch (LibvirtException e) {
                    logger.error("Failed to create netfs mount: " + host + ":" + path, e);
                    logger.error(e.getStackTrace());
                    throw new CloudRuntimeException(e.toString());
                }
            } else if (type == StoragePoolType.Gluster) {
                try {
                    sp = createNetfsStoragePool(PoolType.GLUSTERFS, conn, name, host, path, null);
                } catch (LibvirtException e) {
                    logger.error("Failed to create glusterlvm_fs mount: " + host + ":" + path, e);
                    logger.error(e.getStackTrace());
                    throw new CloudRuntimeException(e.toString());
                }
            } else if (type == StoragePoolType.SharedMountPoint || type == StoragePoolType.Filesystem) {
                sp = createSharedStoragePool(conn, name, host, path);
            } else if (type == StoragePoolType.RBD) {
                sp = createRBDStoragePool(conn, name, host, port, userInfo, path);
            } else if (type == StoragePoolType.CLVM || type == StoragePoolType.CLVM_NG) {
                sp = createCLVMStoragePool(conn, name, host, path);
                if (sp == null) {
                    logger.info("Falling back to virtual CLVM/CLVM_NG pool without libvirt for: {}", name);
                    return createVirtualClvmPool(name, host, path, type, details);
                }
            }
        }

        if (sp == null) {
            throw new CloudRuntimeException("Failed to create storage pool: " + name);
        }

        try {
            if (!isPrimaryStorage) {
                // only ref count storage pools for secondary storage, as primary storage is assumed
                // to be always mounted, as long the primary storage isn't fully deleted.
                incStoragePoolRefCount(name);
            }
            if (sp.isActive() == 0 && type != StoragePoolType.CLVM && type != StoragePoolType.CLVM_NG) {
                logger.debug("Attempting to activate pool " + name);
                sp.create(0);
            }

            if (type == StoragePoolType.NetworkFilesystem) {
                checkNetfsStoragePoolMounted(name);
            }

            return getStoragePool(name);
        } catch (LibvirtException e) {
            decStoragePoolRefCount(name);
            String error = e.toString();
            if (error.contains("Storage source conflict")) {
                throw new CloudRuntimeException("A pool matching this location already exists in libvirt, " +
                        " but has a different UUID/Name. Cannot create new pool without first " + " removing it. Check for inactive pools via 'virsh pool-list --all'. " +
                        error);
            } else {
                throw new CloudRuntimeException(error);
            }
        }
    }

    private boolean destroyStoragePool(Connect conn, String uuid) throws LibvirtException {
        StoragePool sp;
        try {
            sp = conn.storagePoolLookupByUUIDString(uuid);
        } catch (LibvirtException exc) {
            logger.warn("Storage pool " + uuid + " doesn't exist in libvirt. Assuming it is already removed");
            logger.warn(exc.getStackTrace());
            return true;
        }

        if (sp != null) {
            if (sp.isPersistent() == 1) {
                sp.destroy();
                sp.undefine();
            } else {
                sp.destroy();
            }
            sp.free();

            return true;
        } else {
            logger.warn("Storage pool " + uuid + " doesn't exist in libvirt. Assuming it is already removed");
            return false;
        }
    }

    private boolean destroyStoragePoolHandleException(Connect conn, String uuid)
    {
        try {
            return destroyStoragePool(conn, uuid);
        } catch (LibvirtException e) {
            logger.error(String.format("Failed to destroy libvirt pool %s: %s", uuid, e));
        }
        return false;
    }

    @Override
    public boolean deleteStoragePool(String uuid) {
        logger.info("Attempting to remove storage pool " + uuid + " from libvirt");

        // decrement and check if storage pool still in use
        if (decStoragePoolRefCount(uuid)) {
            logger.info(String.format("deleteStoragePool: Storage pool %s still in use", uuid));
            return true;
        }

        Connect conn = null;
        try {
            conn = LibvirtConnection.getConnection();
        } catch (LibvirtException e) {
            throw new CloudRuntimeException(e.toString());
        }

        Secret s = null;

        /*
         * Some storage pools, like RBD also have 'secret' information stored in libvirt
         * Destroy them if they exist
         */
        try {
            s = conn.secretLookupByUUIDString(uuid);
        } catch (LibvirtException e) {
            logger.info("Storage pool " + uuid + " has no corresponding secret. Not removing any secret.");
        }

        try {
            destroyStoragePool(conn, uuid);
            if (s != null) {
                s.undefine();
                s.free();
            }

            logger.info("Storage pool " + uuid + " was successfully removed from libvirt.");

            return true;
        } catch (LibvirtException e) {
            // handle ebusy error when pool is quickly destroyed
            if (e.toString().contains("exit status 16")) {
                String targetPath = _mountPoint + File.separator + uuid;
                    logger.error("deleteStoragePool removed pool from libvirt, but libvirt had trouble unmounting the pool. Trying umount location " + targetPath +
                        " again in a few seconds");
                String result = Script.runSimpleBashScript("sleep 5 && umount " + targetPath);
                if (result == null) {
                    logger.info("Succeeded in unmounting " + targetPath);
                    destroyStoragePoolHandleException(conn, uuid);
                    return true;
                }
                logger.error("Failed to unmount " + targetPath);
            }
            throw new CloudRuntimeException(e.toString(), e);
        }
    }

    /**
     * Creates a physical disk depending on the {@link StoragePoolType}:
     * <ul>
     *     <li>
     *         <b>{@link StoragePoolType#RBD}</b>
     *         <ul>
     *             <li>
     *                 If it is an erasure code pool, utilizes QemuImg to create the physical disk through the method
     *             {@link LibvirtStorageAdaptor#createPhysicalDiskByQemuImg(String, KVMStoragePool, PhysicalDiskFormat, Storage.ProvisioningType, long, byte[])}
     *             </li>
     *             <li>
     *                 Otherwise, utilize Libvirt to create the physical disk through the method
     *                 {@link LibvirtStorageAdaptor#createPhysicalDiskByLibVirt(String, KVMStoragePool, PhysicalDiskFormat, Storage.ProvisioningType, long)}
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         {@link StoragePoolType#NetworkFilesystem} and {@link StoragePoolType#Filesystem}
     *         <ul>
     *             <li>
     *                 If the format is {@link PhysicalDiskFormat#QCOW2} or {@link PhysicalDiskFormat#RAW}, utilizes QemuImg to create the physical disk through the method
     *             {@link LibvirtStorageAdaptor#createPhysicalDiskByQemuImg(String, KVMStoragePool, PhysicalDiskFormat, Storage.ProvisioningType, long, byte[])}
     *             </li>
     *             <li>
     *                 If the format is {@link PhysicalDiskFormat#DIR} or {@link PhysicalDiskFormat#TAR}, utilize Libvirt to create the physical disk through the method
     *                 {@link LibvirtStorageAdaptor#createPhysicalDiskByLibVirt(String, KVMStoragePool, PhysicalDiskFormat, Storage.ProvisioningType, long)}
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         For the rest of the {@link StoragePoolType} types, utilizes the Libvirt method
     *         {@link LibvirtStorageAdaptor#createPhysicalDiskByLibVirt(String, KVMStoragePool, PhysicalDiskFormat, Storage.ProvisioningType, long)}
     *     </li>
     * </ul>
     */
    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, KVMStoragePool pool,
            PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, byte[] passphrase) {

        logger.info("Attempting to create volume {} ({}) in pool {} with size {}", name, pool.getType().toString(), pool.getUuid(), toHumanReadableSize(size));

        StoragePoolType poolType = pool.getType();
        if (StoragePoolType.RBD.equals(poolType)) {
            Map<String, String> details = pool.getDetails();
            String dataPool = (details == null) ? null : details.get(KVMPhysicalDisk.RBD_DEFAULT_DATA_POOL);

            return (dataPool == null) ?  createPhysicalDiskByLibVirt(name, pool, PhysicalDiskFormat.RAW, provisioningType, size) :
                    createPhysicalDiskByQemuImg(name, pool, PhysicalDiskFormat.RAW, provisioningType, size, passphrase);
        } else if (StoragePoolType.CLVM_NG.equals(poolType)) {
            return createClvmNgDiskWithBacking(name, 0, size, null, pool, provisioningType);
        } else if (StoragePoolType.NetworkFilesystem.equals(poolType) || StoragePoolType.Filesystem.equals(poolType)) {
            switch (format) {
                case QCOW2:
                case RAW:
                    return createPhysicalDiskByQemuImg(name, pool, format, provisioningType, size, passphrase);
                case DIR:
                case TAR:
                    return createPhysicalDiskByLibVirt(name, pool, format, provisioningType, size);
                default:
                    throw new CloudRuntimeException("Unexpected disk format is specified.");
            }
        } else {
            return createPhysicalDiskByLibVirt(name, pool, format, provisioningType, size);
        }
    }

    private KVMPhysicalDisk createPhysicalDiskByLibVirt(String name, KVMStoragePool pool,
            PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size) {
        LibvirtStoragePool libvirtPool = (LibvirtStoragePool) pool;
        StoragePool virtPool = libvirtPool.getPool();
        LibvirtStorageVolumeDef.VolumeFormat libvirtformat = LibvirtStorageVolumeDef.VolumeFormat.getFormat(format);

        String volPath = null;
        String volName = null;
        long volAllocation = 0;
        long volCapacity = 0;

        LibvirtStorageVolumeDef volDef = new LibvirtStorageVolumeDef(name,
                size, libvirtformat, null, null);
        logger.debug(volDef.toString());
        try {
            StorageVol vol = virtPool.storageVolCreateXML(volDef.toString(), 0);
            volPath = vol.getPath();
            volName = vol.getName();
            volAllocation = vol.getInfo().allocation;
            volCapacity = vol.getInfo().capacity;
        } catch (LibvirtException e) {
            throw new CloudRuntimeException(e.toString());
        }

        KVMPhysicalDisk disk = new KVMPhysicalDisk(volPath, volName, pool);
        disk.setFormat(format);
        disk.setSize(volAllocation);
        disk.setVirtualSize(volCapacity);
        return disk;
    }


    private KVMPhysicalDisk createPhysicalDiskByQemuImg(String name, KVMStoragePool pool, PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size,
                                                        byte[] passphrase) {
        String volPath;
        String volName = name;
        long virtualSize = 0;
        long actualSize = 0;
        QemuObject.EncryptFormat encryptFormat = null;
        List<QemuObject> passphraseObjects = new ArrayList<>();
        final int timeout = 0;
        QemuImgFile destFile;

        if (StoragePoolType.RBD.equals(pool.getType())) {
            volPath = pool.getSourceDir() + File.separator + name;
            destFile = new QemuImgFile(KVMPhysicalDisk.RBDStringBuilder(pool, volPath));
        } else {
            volPath = pool.getLocalPath() + File.separator + name;
            destFile = new QemuImgFile(volPath);
        }

        destFile.setFormat(format);
        destFile.setSize(size);
        Map<String, String> options = new HashMap<String, String>();
        if (List.of(StoragePoolType.NetworkFilesystem, StoragePoolType.Filesystem).contains(pool.getType())) {
            options.put(QemuImg.PREALLOCATION, QemuImg.PreallocationType.getPreallocationType(provisioningType).toString());
        }

        try (KeyFile keyFile = new KeyFile(passphrase)) {
            QemuImg qemu = new QemuImg(timeout);
            if (keyFile.isSet()) {
                passphraseObjects.add(QemuObject.prepareSecretForQemuImg(format, QemuObject.EncryptFormat.LUKS, keyFile.toString(), "sec0", options));

                // make room for encryption header on raw format, use LUKS
                if (format == PhysicalDiskFormat.RAW) {
                    destFile.setSize(destFile.getSize() - (16 << 20));
                    destFile.setFormat(PhysicalDiskFormat.LUKS);
                }

                encryptFormat = QemuObject.EncryptFormat.LUKS;
            }
            qemu.create(destFile, null, options, passphraseObjects);
            Map<String, String> info = qemu.info(destFile);
            virtualSize = Long.parseLong(info.get(QemuImg.VIRTUAL_SIZE));
            actualSize = new File(destFile.getFileName()).length();
        } catch (QemuImgException | LibvirtException | IOException e) {
            throw new CloudRuntimeException(String.format("Failed to create %s due to a failed execution of qemu-img", volPath), e);
        }

        KVMPhysicalDisk disk = new KVMPhysicalDisk(volPath, volName, pool);
        disk.setFormat(format);
        disk.setSize(actualSize);
        disk.setVirtualSize(virtualSize);
        disk.setQemuEncryptFormat(encryptFormat);
        return disk;
    }

    @Override
    public boolean connectPhysicalDisk(String name, KVMStoragePool pool, Map<String, String> details, boolean isVMMigrate) {
        // this is for managed storage that needs to prep disks prior to use
        if ((pool.getType() == StoragePoolType.CLVM || pool.getType() == StoragePoolType.CLVM_NG) && isVMMigrate) {
            logger.info("Activating CLVM/CLVM_NG volume {} at location: {} in shared mode for VM migration", name, pool.getLocalPath() + File.separator + name);
            Script activateVolInSharedMode = new Script("lvchange", 5000, logger);
            activateVolInSharedMode.add("-asy");
            activateVolInSharedMode.add(pool.getLocalPath() + File.separator + name);
            String result = activateVolInSharedMode.execute();
            if (result != null) {
                logger.error("Failed to activate CLVM/CLVM_NG volume {} in shared mode for VM migration. Command output: {}", name, result);
                return false;
            }
        }

        if (pool.getType() == StoragePoolType.CLVM_NG) {
            ensureClvmNgBackingFileAccessible(name, pool);
        }

        return true;
    }

    /**
     * Checks if a CLVM_NG QCOW2 volume has a backing file (template) and ensures it's activated in shared mode.
     * This is critical for multi-host deployments where VMs on different hosts need to access the same template.
     * Called during VM deployment to ensure template backing files are accessible on the current host.
     *
     * @param volumeName The name of the volume (e.g., volume-uuid)
     * @param pool The CLVM_NG storage pool
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
                                    logger.info("Volume {} has backing file: {}. Ensuring backing file is in shared mode on this host.",
                                            volumePath, backingFile);
                                    ensureTemplateLvInSharedMode(backingFile, false);
                                } else {
                                    logger.debug("Volume {} has backing file but not a block device path: {}", volumePath, backingFile);
                                }
                            }
                        }
                    } else {
                        logger.debug("Volume {} does not have a backing file (full clone)", volumePath);
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

    @Override
    public boolean disconnectPhysicalDisk(String uuid, KVMStoragePool pool) {
        // this is for managed storage that needs to cleanup disks after use
        return true;
    }

    @Override
    public boolean disconnectPhysicalDisk(Map<String, String> volumeToDisconnect) {
        // this is for managed storage that needs to cleanup disks after use
        return false;
    }

    @Override
    public boolean disconnectPhysicalDiskByPath(String localPath) {
        // we've only ever cleaned up ISOs that are NFS mounted
        String poolUuid = null;
        if (localPath != null && localPath.startsWith(_mountPoint) && localPath.endsWith(".iso")) {
            String[] token = localPath.split("/");

            if (token.length > 3) {
                poolUuid = token[2];
            }
        } else {
            return false;
        }

        if (poolUuid == null) {
            return false;
        }

        try {
            Connect conn = LibvirtConnection.getConnection();

            conn.storagePoolLookupByUUIDString(poolUuid);

            deleteStoragePool(poolUuid);

            return true;
        } catch (LibvirtException ex) {
            return false;
        } catch (CloudRuntimeException ex) {
            return false;
        }
    }

    @Override
    public boolean deletePhysicalDisk(String uuid, KVMStoragePool pool, Storage.ImageFormat format) {

        logger.info("Attempting to remove volume " + uuid + " from pool " + pool.getUuid());

        /**
         * RBD volume can have snapshots and while they exist libvirt
         * can't remove the RBD volume
         *
         * We have to remove those snapshots first
         */
        if (pool.getType() == StoragePoolType.RBD) {
            try {
                logger.info("Unprotecting and Removing RBD snapshots of image " + pool.getSourceDir() + "/" + uuid + " prior to removing the image");

                Rados r = new Rados(pool.getAuthUserName());
                r.confSet("mon_host", pool.getSourceHost() + ":" + pool.getSourcePort());
                r.confSet("key", pool.getAuthSecret());
                r.confSet("client_mount_timeout", "30");
                r.connect();
                logger.debug("Successfully connected to Ceph cluster at " + r.confGet("mon_host"));

                IoCTX io = r.ioCtxCreate(pool.getSourceDir());
                Rbd rbd = new Rbd(io);
                RbdImage image = rbd.open(uuid);
                logger.debug("Fetching list of snapshots of RBD image " + pool.getSourceDir() + "/" + uuid);
                List<RbdSnapInfo> snaps = image.snapList();
                try {
                    for (RbdSnapInfo snap : snaps) {
                        if (image.snapIsProtected(snap.name)) {
                            logger.debug("Unprotecting snapshot " + pool.getSourceDir() + "/" + uuid + "@" + snap.name);
                            image.snapUnprotect(snap.name);
                        } else {
                            logger.debug("Snapshot " + pool.getSourceDir() + "/" + uuid + "@" + snap.name + " is not protected.");
                        }
                        logger.debug("Removing snapshot " + pool.getSourceDir() + "/" + uuid + "@" + snap.name);
                        image.snapRemove(snap.name);
                    }
                    logger.info("Successfully unprotected and removed any remaining snapshots (" + snaps.size() + ") of "
                        + pool.getSourceDir() + "/" + uuid + " Continuing to remove the RBD image");
                } catch (RbdException e) {
                    logger.error("Failed to remove snapshot with exception: " + e.toString() +
                        ", RBD error: " + ErrorCode.getErrorMessage(e.getReturnValue()));
                    throw new CloudRuntimeException(e.toString() + " - " + ErrorCode.getErrorMessage(e.getReturnValue()));
                } finally {
                    logger.debug("Closing image and destroying context");
                    rbd.close(image);
                    r.ioCtxDestroy(io);
                }
            } catch (RadosException e) {
                logger.error("Failed to remove snapshot with exception: " + e.toString() +
                    ", RBD error: " + ErrorCode.getErrorMessage(e.getReturnValue()));
                throw new CloudRuntimeException(e.toString() + " - " + ErrorCode.getErrorMessage(e.getReturnValue()));
            } catch (RbdException e) {
                logger.error("Failed to remove snapshot with exception: " + e.toString() +
                    ", RBD error: " + ErrorCode.getErrorMessage(e.getReturnValue()));
                throw new CloudRuntimeException(e.toString() + " - " + ErrorCode.getErrorMessage(e.getReturnValue()));
            }
        }

        // For CLVM/CLVM_NG pools, always use direct LVM cleanup to ensure secure zero-fill
        if (pool.getType() == StoragePoolType.CLVM || pool.getType() == StoragePoolType.CLVM_NG) {
            logger.info("CLVM/CLVM_NG pool detected - using direct LVM cleanup with secure zero-fill for volume {}", uuid);
            return cleanupCLVMVolume(uuid, pool);
        }

        // For non-CLVM pools, use libvirt deletion
        LibvirtStoragePool libvirtPool = (LibvirtStoragePool)pool;
        try {
            StorageVol vol = getVolume(libvirtPool.getPool(), uuid);
            if (vol == null) {
                logger.warn("Volume {} not found in libvirt pool {}, it may have been already deleted", uuid, pool.getUuid());
                return true;
            }
            logger.debug("Instructing libvirt to remove volume {} from pool {}", uuid, pool.getUuid());
            if(Storage.ImageFormat.DIR.equals(format)){
                deleteDirVol(libvirtPool, vol);
            } else {
                deleteVol(libvirtPool, vol);
            }
            vol.free();
            return true;
        } catch (LibvirtException e) {
            throw new CloudRuntimeException(e.toString());
        }
    }

    private boolean shouldSecureZeroFill(KVMStoragePool pool) {
        Map<String, String> details = pool.getDetails();
        String secureZeroFillStr = (details != null) ? details.get(KVMStoragePool.CLVM_SECURE_ZERO_FILL) : null;
        return Boolean.parseBoolean(secureZeroFillStr);
    }

    /**
     * Clean up CLVM volume and its snapshots directly using LVM commands.
     * This is used as a fallback when libvirt cannot find or delete the volume.
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

            // Check if the LV exists
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

    /**
     * This function copies a physical disk from Secondary Storage to Primary Storage
     * or from Primary to Primary Storage
     *
     * The first time a template is deployed in Primary Storage it will be copied from
     * Secondary to Primary.
     *
     * If it has been created on Primary Storage, it will be copied on the Primary Storage
     */
    @Override
    public KVMPhysicalDisk createDiskFromTemplate(KVMPhysicalDisk template,
            String name, PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, KVMStoragePool destPool, int timeout, byte[] passphrase) {

        logger.info("Creating volume " + name + " from template " + template.getName() + " in pool " + destPool.getUuid() +
                " (" + destPool.getType().toString() + ") with size " + toHumanReadableSize(size));

        KVMPhysicalDisk disk = null;

        if (destPool.getType() == StoragePoolType.RBD) {
            disk = createDiskFromTemplateOnRBD(template, name, format, provisioningType, size, destPool, timeout);
        } else {
            try (KeyFile keyFile = new KeyFile(passphrase)){
                String newUuid = name;
                if (destPool.getType() == StoragePoolType.CLVM_NG && format == PhysicalDiskFormat.QCOW2) {
                    logger.info("Creating CLVM_NG volume {} with backing file from template {}", newUuid, template.getName());
                    String backingFile = getClvmBackingFile(template, destPool);

                    disk = createClvmNgDiskWithBacking(newUuid, timeout, size, backingFile, destPool, provisioningType);
                    return disk;
                }
                List<QemuObject> passphraseObjects = new ArrayList<>();
                disk = destPool.createPhysicalDisk(newUuid, format, provisioningType, template.getVirtualSize(), passphrase);
                if (disk == null) {
                    throw new CloudRuntimeException("Failed to create disk from template " + template.getName());
                }

                if (template.getFormat() == PhysicalDiskFormat.TAR) {
                    Script.runSimpleBashScript("tar -x -f " + template.getPath() + " -C " + disk.getPath(), timeout); // TO BE FIXED to aware provisioningType
                } else if (template.getFormat() == PhysicalDiskFormat.DIR) {
                    Script.runSimpleBashScript("mkdir -p " + disk.getPath());
                    Script.runSimpleBashScript("chmod 755 " + disk.getPath());
                    Script.runSimpleBashScript("tar -x -f " + template.getPath() + "/*.tar -C " + disk.getPath(), timeout);
                } else if (format == PhysicalDiskFormat.QCOW2) {
                    QemuImg qemu = new QemuImg(timeout);
                    QemuImgFile destFile = new QemuImgFile(disk.getPath(), format);
                    if (size > template.getVirtualSize()) {
                        destFile.setSize(size);
                    } else {
                        destFile.setSize(template.getVirtualSize());
                    }
                    Map<String, String> options = new HashMap<String, String>();
                    options.put("preallocation", QemuImg.PreallocationType.getPreallocationType(provisioningType).toString());


                    if (keyFile.isSet()) {
                        passphraseObjects.add(QemuObject.prepareSecretForQemuImg(format, QemuObject.EncryptFormat.LUKS, keyFile.toString(), "sec0", options));
                        disk.setQemuEncryptFormat(QemuObject.EncryptFormat.LUKS);
                    }

                    QemuImgFile srcFile = new QemuImgFile(template.getPath(), template.getFormat());
                    Boolean createFullClone = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.CREATE_FULL_CLONE);
                    switch(provisioningType){
                    case THIN:
                        logger.info("Creating volume [{}] {} backing file [{}] as the property [{}] is [{}].", destFile.getFileName(), createFullClone ? "without" : "with",
                                template.getPath(), AgentProperties.CREATE_FULL_CLONE.getName(), createFullClone);
                        if (createFullClone) {
                            qemu.convert(srcFile, destFile, options, passphraseObjects, null, false);
                        } else {
                            qemu.create(destFile, srcFile, options, passphraseObjects);
                        }
                        break;
                    case SPARSE:
                    case FAT:
                        srcFile = new QemuImgFile(template.getPath(), template.getFormat());
                        qemu.convert(srcFile, destFile, options, passphraseObjects, null, false);
                        break;
                    }
                } else if (format == PhysicalDiskFormat.RAW) {
                    PhysicalDiskFormat destFormat = PhysicalDiskFormat.RAW;
                    Map<String, String> options = new HashMap<String, String>();

                    if (keyFile.isSet()) {
                        destFormat = PhysicalDiskFormat.LUKS;
                        disk.setQemuEncryptFormat(QemuObject.EncryptFormat.LUKS);
                        passphraseObjects.add(QemuObject.prepareSecretForQemuImg(destFormat, QemuObject.EncryptFormat.LUKS, keyFile.toString(), "sec0", options));
                    }

                    QemuImgFile sourceFile = new QemuImgFile(template.getPath(), template.getFormat());
                    QemuImgFile destFile = new QemuImgFile(disk.getPath(), destFormat);
                    if (size > template.getVirtualSize()) {
                        destFile.setSize(size);
                    } else {
                        destFile.setSize(template.getVirtualSize());
                    }
                    QemuImg qemu = new QemuImg(timeout);
                    qemu.convert(sourceFile, destFile, options, passphraseObjects, null, false);
                }
            } catch (QemuImgException | LibvirtException | IOException e) {
                throw new CloudRuntimeException(String.format("Failed to create %s due to a failed execution of qemu-img", name), e);
            }
        }

        return disk;
    }

    private String getClvmBackingFile(KVMPhysicalDisk template, KVMStoragePool destPool) {
        String templateLvName = template.getName();
        KVMPhysicalDisk templateOnPrimary = null;

        try {
            templateOnPrimary = destPool.getPhysicalDisk(templateLvName);
        } catch (CloudRuntimeException e) {
            logger.warn("Template {} not found on CLVM_NG pool {}.", templateLvName, destPool.getUuid());
        }

        String backingFile;
        if (templateOnPrimary != null) {
            backingFile = templateOnPrimary.getPath();
            logger.info("Using template on primary storage as backing file: {}", backingFile);

            ensureTemplateLvInSharedMode(backingFile);
        } else {
            logger.error("Template {} should be on primary storage before creating volumes from it", templateLvName);
            throw new CloudRuntimeException(String.format("Template not found on CLVM_NG primary storage: {}." +
                    "Template must be copied to primary storage first.", templateLvName));
        }
        return backingFile;
    }

    /**
     * Ensures a template LV is activated in shared mode so multiple VMs can use it as a backing file.
     *
     * @param templatePath The full path to the template LV (e.g., /dev/vgname/template-uuid)
     * @param throwOnFailure If true, throws CloudRuntimeException on failure; if false, logs warning and continues
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
                        logger.info("Template LV {} is not in shared mode (attr: {}).",
                                   templatePath, lvAttr);
                        logger.info("Activating template LV {} in shared mode", templatePath);
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

    private KVMPhysicalDisk createDiskFromTemplateOnRBD(KVMPhysicalDisk template,
            String name, PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, KVMStoragePool destPool, int timeout){

        /*
            With RBD you can't run qemu-img convert with an existing RBD image as destination
            qemu-img will exit with the error that the destination already exists.
            So for RBD we don't create the image, but let qemu-img do that for us.

            We then create a KVMPhysicalDisk object that we can return
         */

        KVMStoragePool srcPool = template.getPool();
        KVMPhysicalDisk disk = null;
        String newUuid = name;

        format = PhysicalDiskFormat.RAW;
        disk = new KVMPhysicalDisk(destPool.getSourceDir() + "/" + newUuid, newUuid, destPool);
        disk.setFormat(format);
        if (size > template.getVirtualSize()) {
            disk.setSize(size);
            disk.setVirtualSize(size);
        } else {
            // leave these as they were if size isn't applicable
            disk.setSize(template.getVirtualSize());
            disk.setVirtualSize(disk.getSize());
        }


        QemuImgFile srcFile;
        QemuImgFile destFile = new QemuImgFile(KVMPhysicalDisk.RBDStringBuilder(destPool, disk.getPath()));
        destFile.setFormat(format);

        if (srcPool.getType() != StoragePoolType.RBD) {
            srcFile = new QemuImgFile(template.getPath(), template.getFormat());
            try{
                QemuImg qemu = new QemuImg(timeout);
                qemu.convert(srcFile, destFile);
            } catch (QemuImgException | LibvirtException e) {
                logger.error("Failed to create " + disk.getPath() +
                        " due to a failed executing of qemu-img: " + e.getMessage());
            }
        } else {

            /**
             * We have to find out if the source file is in the same RBD pool and has
             * RBD format 2 before we can do a layering/clone operation on the RBD image
             *
             * This will be the case when the template is already on Primary Storage and
             * we want to copy it
             */

            try {
                if ((srcPool.getSourceHost().equals(destPool.getSourceHost())) && (srcPool.getSourceDir().equals(destPool.getSourceDir()))) {
                    /* We are on the same Ceph cluster, but we require RBD format 2 on the source image */
                    logger.debug("Trying to perform a RBD clone (layering) since we are operating in the same storage pool");

                    Rados r = new Rados(srcPool.getAuthUserName());
                    r.confSet("mon_host", srcPool.getSourceHost() + ":" + srcPool.getSourcePort());
                    r.confSet("key", srcPool.getAuthSecret());
                    r.confSet("client_mount_timeout", "30");
                    r.connect();
                    logger.debug("Successfully connected to Ceph cluster at " + r.confGet("mon_host"));

                    IoCTX io = r.ioCtxCreate(srcPool.getSourceDir());
                    Rbd rbd = new Rbd(io);
                    RbdImage srcImage = rbd.open(template.getName());

                    if (srcImage.isOldFormat()) {
                        /* The source image is RBD format 1, we have to do a regular copy */
                        logger.debug("The source image " + srcPool.getSourceDir() + "/" + template.getName() +
                                " is RBD format 1. We have to perform a regular copy (" + toHumanReadableSize(disk.getVirtualSize()) + " bytes)");

                        rbd.create(disk.getName(), disk.getVirtualSize(), RBD_FEATURES, rbdOrder);
                        RbdImage destImage = rbd.open(disk.getName());

                        logger.debug("Starting to copy " + srcImage.getName() +  " to " + destImage.getName() + " in Ceph pool " + srcPool.getSourceDir());
                        rbd.copy(srcImage, destImage);

                        logger.debug("Finished copying " + srcImage.getName() +  " to " + destImage.getName() + " in Ceph pool " + srcPool.getSourceDir());
                        rbd.close(destImage);
                    } else {
                        logger.debug("The source image " + srcPool.getSourceDir() + "/" + template.getName()
                                + " is RBD format 2. We will perform a RBD snapshot+clone (layering)");


                        logger.debug("Checking if RBD snapshot " + srcPool.getSourceDir() + "/" + template.getName()
                                + "@" + rbdTemplateSnapName + " exists prior to attempting a clone operation.");

                        List<RbdSnapInfo> snaps = srcImage.snapList();
                        logger.debug("Found " + snaps.size() +  " snapshots on RBD image " + srcPool.getSourceDir() + "/" + template.getName());
                        boolean snapFound = false;
                        for (RbdSnapInfo snap : snaps) {
                            if (rbdTemplateSnapName.equals(snap.name)) {
                                logger.debug("RBD snapshot " + srcPool.getSourceDir() + "/" + template.getName()
                                        + "@" + rbdTemplateSnapName + " already exists.");
                                snapFound = true;
                                break;
                            }
                        }

                        if (!snapFound) {
                            logger.debug("Creating RBD snapshot " + rbdTemplateSnapName + " on image " + name);
                            srcImage.snapCreate(rbdTemplateSnapName);
                            logger.debug("Protecting RBD snapshot " + rbdTemplateSnapName + " on image " + name);
                            srcImage.snapProtect(rbdTemplateSnapName);
                        }

                        rbd.clone(template.getName(), rbdTemplateSnapName, io, disk.getName(), RBD_FEATURES, rbdOrder);
                        logger.debug("Successfully cloned " + template.getName() + "@" + rbdTemplateSnapName + " to " + disk.getName());
                        /* We also need to resize the image if the VM was deployed with a larger root disk size */
                        if (disk.getVirtualSize() > template.getVirtualSize()) {
                            RbdImage diskImage = rbd.open(disk.getName());
                            diskImage.resize(disk.getVirtualSize());
                            rbd.close(diskImage);
                            logger.debug("Resized " + disk.getName() + " to " + toHumanReadableSize(disk.getVirtualSize()));
                        }

                    }

                    rbd.close(srcImage);
                    r.ioCtxDestroy(io);
                } else {
                    /* The source pool or host is not the same Ceph cluster, we do a simple copy with Qemu-Img */
                    logger.debug("Both the source and destination are RBD, but not the same Ceph cluster. Performing a copy");

                    Rados rSrc = new Rados(srcPool.getAuthUserName());
                    rSrc.confSet("mon_host", srcPool.getSourceHost() + ":" + srcPool.getSourcePort());
                    rSrc.confSet("key", srcPool.getAuthSecret());
                    rSrc.confSet("client_mount_timeout", "30");
                    rSrc.connect();
                    logger.debug("Successfully connected to source Ceph cluster at " + rSrc.confGet("mon_host"));

                    Rados rDest = new Rados(destPool.getAuthUserName());
                    rDest.confSet("mon_host", destPool.getSourceHost() + ":" + destPool.getSourcePort());
                    rDest.confSet("key", destPool.getAuthSecret());
                    rDest.confSet("client_mount_timeout", "30");
                    rDest.connect();
                    logger.debug("Successfully connected to source Ceph cluster at " + rDest.confGet("mon_host"));

                    IoCTX sIO = rSrc.ioCtxCreate(srcPool.getSourceDir());
                    Rbd sRbd = new Rbd(sIO);

                    IoCTX dIO = rDest.ioCtxCreate(destPool.getSourceDir());
                    Rbd dRbd = new Rbd(dIO);

                    logger.debug("Creating " + disk.getName() + " on the destination cluster " + rDest.confGet("mon_host") + " in pool " +
                            destPool.getSourceDir());
                    dRbd.create(disk.getName(), disk.getVirtualSize(), RBD_FEATURES, rbdOrder);

                    RbdImage srcImage = sRbd.open(template.getName());
                    RbdImage destImage = dRbd.open(disk.getName());

                    logger.debug("Copying " + template.getName() + " from Ceph cluster " + rSrc.confGet("mon_host") + " to " + disk.getName()
                            + " on cluster " + rDest.confGet("mon_host"));
                    sRbd.copy(srcImage, destImage);

                    sRbd.close(srcImage);
                    dRbd.close(destImage);

                    rSrc.ioCtxDestroy(sIO);
                    rDest.ioCtxDestroy(dIO);
                }
            } catch (RadosException e) {
                logger.error("Failed to perform a RADOS action on the Ceph cluster, the error was: " + e.getMessage());
                disk = null;
            } catch (RbdException e) {
                logger.error("Failed to perform a RBD action on the Ceph cluster, the error was: " + e.getMessage());
                disk = null;
            }
        }
        return disk;
    }

    @Override
    public KVMPhysicalDisk createTemplateFromDisk(KVMPhysicalDisk disk, String name, PhysicalDiskFormat format, long size, KVMStoragePool destPool) {
        return null;
    }

    @Override
    public List<KVMPhysicalDisk> listPhysicalDisks(String storagePoolUuid, KVMStoragePool pool) {
        LibvirtStoragePool libvirtPool = (LibvirtStoragePool)pool;
        StoragePool virtPool = libvirtPool.getPool();
        List<KVMPhysicalDisk> disks = new ArrayList<KVMPhysicalDisk>();
        try {
            String[] vols = virtPool.listVolumes();
            for (String volName : vols) {
                KVMPhysicalDisk disk = getPhysicalDisk(volName, pool);
                disks.add(disk);
            }
            return disks;
        } catch (LibvirtException e) {
            throw new CloudRuntimeException(e.toString());
        }
    }

    @Override
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPool, int timeout) {
        return copyPhysicalDisk(disk, name, destPool, timeout, null, null, null);
    }

    /**
     * This copies a volume from Primary Storage to Secondary Storage
     *
     * In theory it could also do it the other way around, but the current implementation
     * in ManagementServerImpl shows that the destPool is always a Secondary Storage Pool
     */
    @Override
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPool, int timeout, byte[] srcPassphrase, byte[] dstPassphrase, Storage.ProvisioningType provisioningType) {

        /**
            With RBD you can't run qemu-img convert with an existing RBD image as destination
            qemu-img will exit with the error that the destination already exists.
            So for RBD we don't create the image, but let qemu-img do that for us.

            We then create a KVMPhysicalDisk object that we can return

            It is however very unlikely that the destPool will be RBD, since it isn't supported
            for Secondary Storage
         */

        KVMStoragePool srcPool = disk.getPool();
        /* Linstor images are always stored as RAW, but Linstor uses qcow2 in DB,
           to support snapshots(backuped) as qcow2 files. */
        PhysicalDiskFormat sourceFormat = srcPool.getType() != StoragePoolType.Linstor ?
                disk.getFormat() : PhysicalDiskFormat.RAW;
        String sourcePath = disk.getPath();

        boolean isSourceClvm = srcPool.getType() == StoragePoolType.CLVM || srcPool.getType() == StoragePoolType.CLVM_NG;
        boolean isDestClvm = destPool.getType() == StoragePoolType.CLVM || destPool.getType() == StoragePoolType.CLVM_NG;

        String clvmLockVolume = null;
        boolean shouldDeactivateSource = false;

        try {
            if (isSourceClvm) {
                logger.info("Activating source CLVM volume {} in shared mode for copy", sourcePath);
                LibvirtComputingResource.setClvmVolumeToSharedMode(sourcePath);
                shouldDeactivateSource = !isDestClvm;
            }

            KVMPhysicalDisk newDisk;
            logger.debug("copyPhysicalDisk: disk size:{}, virtualsize:{} format:{}", toHumanReadableSize(disk.getSize()), toHumanReadableSize(disk.getVirtualSize()), disk.getFormat());
            if (destPool.getType() != StoragePoolType.RBD) {
                if (disk.getFormat() == PhysicalDiskFormat.TAR) {
                    newDisk = destPool.createPhysicalDisk(name, PhysicalDiskFormat.DIR, Storage.ProvisioningType.THIN, disk.getVirtualSize(), null);
                } else {
                    newDisk = destPool.createPhysicalDisk(name, Storage.ProvisioningType.THIN, disk.getVirtualSize(), null);
                }
            } else {
                newDisk = new KVMPhysicalDisk(destPool.getSourceDir() + "/" + name, name, destPool);
                newDisk.setFormat(PhysicalDiskFormat.RAW);
                newDisk.setSize(disk.getVirtualSize());
                newDisk.setVirtualSize(disk.getSize());
            }

            String destPath = newDisk.getPath();
            PhysicalDiskFormat destFormat = newDisk.getFormat();

            if (isDestClvm) {
                logger.info("Activating destination CLVM volume {} in shared mode for copy", destPath);
                LibvirtComputingResource.setClvmVolumeToSharedMode(destPath);
                clvmLockVolume = destPath;
            }

            boolean formatConversion = sourceFormat != destFormat;
            if (formatConversion) {
                logger.info("Format conversion required: {} -> {}", sourceFormat, destFormat);
            }

            return performCopy(disk, name, destPool, timeout, srcPool, sourceFormat,
                    sourcePath, newDisk, destPath, destFormat, formatConversion);

        } finally {
            if (isSourceClvm && shouldDeactivateSource) {
                try {
                    logger.info("Deactivating source CLVM volume {} after copy", sourcePath);
                    LibvirtComputingResource.deactivateClvmVolume(sourcePath);
                } catch (Exception e) {
                    logger.warn("Failed to deactivate source CLVM volume {}: {}", sourcePath, e.getMessage());
                }
            }
            if (isDestClvm && clvmLockVolume != null) {
                try {
                    logger.info("Claiming exclusive lock on destination CLVM volume {} after copy", clvmLockVolume);
                    LibvirtComputingResource.activateClvmVolumeExclusive(clvmLockVolume);
                } catch (Exception e) {
                    logger.warn("Failed to claim exclusive lock on destination CLVM volume {}: {}", clvmLockVolume, e.getMessage());
                }
            }
        }
    }

    private KVMPhysicalDisk performCopy(KVMPhysicalDisk disk, String name, KVMStoragePool destPool, int timeout,
                                        KVMStoragePool srcPool, PhysicalDiskFormat sourceFormat, String sourcePath,
                                        KVMPhysicalDisk newDisk, String destPath, PhysicalDiskFormat destFormat,
                                        boolean formatConversion) {

        QemuImg qemu;

        try {
            qemu = new QemuImg(timeout);
        } catch (QemuImgException | LibvirtException ex ) {
            throw new CloudRuntimeException("Failed to create qemu-img command", ex);
        }
        QemuImgFile srcFile = null;
        QemuImgFile destFile = null;

        if ((srcPool.getType() != StoragePoolType.RBD) && (destPool.getType() != StoragePoolType.RBD)) {
            if(sourceFormat == PhysicalDiskFormat.TAR && destFormat == PhysicalDiskFormat.DIR) { //LXC template
                Script.runSimpleBashScript("cp "+ sourcePath + " " + destPath);
            } else if (sourceFormat == PhysicalDiskFormat.TAR) {
                Script.runSimpleBashScript("tar -x -f " + sourcePath + " -C " + destPath, timeout);
            } else if (sourceFormat == PhysicalDiskFormat.DIR) {
                Script.runSimpleBashScript("mkdir -p " + destPath);
                Script.runSimpleBashScript("chmod 755 " + destPath);
                Script.runSimpleBashScript("cp -p -r " + sourcePath + "/* " + destPath, timeout);
            } else {
                srcFile = new QemuImgFile(sourcePath, sourceFormat);
                try {
                    Map<String, String> info = qemu.info(srcFile);
                    String backingFile = info.get(QemuImg.BACKING_FILE);
                    // qcow2 templates can just be copied into place
                    if (sourceFormat.equals(destFormat) && backingFile == null && sourcePath.endsWith(".qcow2")) {
                        String result = Script.runSimpleBashScript("cp -f " + sourcePath + " " + destPath, timeout);
                        if (result != null) {
                            throw new CloudRuntimeException("Failed to create disk: " + result);
                        }
                    } else {
                        destFile = new QemuImgFile(destPath, destFormat);
                        try {
                            boolean isQCOW2 = PhysicalDiskFormat.QCOW2.equals(sourceFormat);
                            qemu.convert(srcFile, destFile, null, null, new QemuImageOptions(srcFile.getFormat(), srcFile.getFileName(), null),
                                    null, false, isQCOW2);
                            Map<String, String> destInfo = qemu.info(destFile);
                            Long virtualSize = Long.parseLong(destInfo.get(QemuImg.VIRTUAL_SIZE));
                            newDisk.setVirtualSize(virtualSize);
                            newDisk.setSize(virtualSize);
                        } catch (QemuImgException e) {
                            logger.error("Failed to convert [{}] to [{}] due to: [{}].", srcFile.getFileName(), destFile.getFileName(), e.getMessage(), e);
                            newDisk = null;
                        }
                    }
                } catch (QemuImgException e) {
                    logger.error("Failed to fetch the information of file " + srcFile.getFileName() + " the error was: " + e.getMessage());
                    newDisk = null;
                }
            }
        } else if ((srcPool.getType() != StoragePoolType.RBD) && (destPool.getType() == StoragePoolType.RBD)) {
            /**
             * Using qemu-img we copy the QCOW2 disk to RAW (on RBD) directly.
             * To do so it's mandatory that librbd on the system is at least 0.67.7 (Ceph Dumpling)
             */
            logger.debug("The source image is not RBD, but the destination is. We will convert into RBD format 2");
            try {
                srcFile = new QemuImgFile(sourcePath, sourceFormat);
                String rbdDestPath = destPool.getSourceDir() + "/" + name;
                String rbdDestFile = KVMPhysicalDisk.RBDStringBuilder(destPool, rbdDestPath);
                destFile = new QemuImgFile(rbdDestFile, destFormat);

                logger.debug("Starting copy from source image " + srcFile.getFileName() + " to RBD image " + rbdDestPath);
                qemu.convert(srcFile, destFile);
                logger.debug("Successfully converted source image " + srcFile.getFileName() + " to RBD image " + rbdDestPath);

                /* We have to stat the RBD image to see how big it became afterwards */
                Rados r = new Rados(destPool.getAuthUserName());
                r.confSet("mon_host", destPool.getSourceHost() + ":" + destPool.getSourcePort());
                r.confSet("key", destPool.getAuthSecret());
                r.confSet("client_mount_timeout", "30");
                r.connect();
                logger.debug("Successfully connected to Ceph cluster at " + r.confGet("mon_host"));

                IoCTX io = r.ioCtxCreate(destPool.getSourceDir());
                Rbd rbd = new Rbd(io);

                RbdImage image = rbd.open(name);
                RbdImageInfo rbdInfo = image.stat();
                newDisk.setSize(rbdInfo.size);
                newDisk.setVirtualSize(rbdInfo.size);
                logger.debug("After copy the resulting RBD image " + rbdDestPath + " is " + toHumanReadableSize(rbdInfo.size) + " bytes long");
                rbd.close(image);

                r.ioCtxDestroy(io);
            } catch (QemuImgException | LibvirtException e) {
                String srcFilename = srcFile != null ? srcFile.getFileName() : null;
                String destFilename = destFile != null ? destFile.getFileName() : null;
                logger.error(String.format("Failed to convert from %s to %s the error was: %s", srcFilename, destFilename, e.getMessage()));
                newDisk = null;
            } catch (RadosException e) {
                logger.error("A Ceph RADOS operation failed (" + e.getReturnValue() + "). The error was: " + e.getMessage());
                newDisk = null;
            } catch (RbdException e) {
                logger.error("A Ceph RBD operation failed (" + e.getReturnValue() + "). The error was: " + e.getMessage());
                newDisk = null;
            }
        } else {
            /**
                We let Qemu-Img do the work here. Although we could work with librbd and have that do the cloning
                it doesn't benefit us. It's better to keep the current code in place which works
             */
            srcFile = new QemuImgFile(KVMPhysicalDisk.RBDStringBuilder(srcPool, sourcePath));
            srcFile.setFormat(sourceFormat);
            if (destPool.getType() == StoragePoolType.RBD) {
                destFile = new QemuImgFile(KVMPhysicalDisk.RBDStringBuilder(destPool, destPath));
            } else {
                destFile = new QemuImgFile(destPath);
            }
            destFile.setFormat(destFormat);

            try {
                qemu.convert(srcFile, destFile);
            } catch (QemuImgException | LibvirtException e) {
                logger.error("Failed to convert " + srcFile.getFileName() + " to " + destFile.getFileName() + " the error was: " + e.getMessage());
                newDisk = null;
            }
        }

        if (newDisk == null) {
            throw new CloudRuntimeException("Failed to copy " + disk.getPath() + " to " + name);
        }

        return newDisk;
    }

    @Override
    public boolean refresh(KVMStoragePool pool) {
        LibvirtStoragePool libvirtPool = (LibvirtStoragePool)pool;
        StoragePool virtPool = libvirtPool.getPool();
        try {
            refreshPool(virtPool);
        } catch (LibvirtException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteStoragePool(KVMStoragePool pool) {
        return deleteStoragePool(pool.getUuid());
    }

    private void refreshPool(StoragePool pool) throws LibvirtException {
        pool.refresh(0);
        return;
    }

    private void deleteVol(LibvirtStoragePool pool, StorageVol vol) throws LibvirtException {
        vol.delete(0);
    }

    /**
     * Securely zero-fill a volume before deletion to prevent data leakage.
     * Uses blkdiscard (fast TRIM) as primary method, with dd zero-fill as fallback.
     *
     * @param lvPath The full path to the logical volume (e.g., /dev/vgname/lvname)
     * @param volumeUuid The UUID of the volume for logging purposes
     */
    private void secureZeroFillVolume(String lvPath, String volumeUuid) {
        logger.info("Starting secure zero-fill for CLVM volume: {} at path: {}", volumeUuid, lvPath);

        boolean blkdiscardSuccess = false;

        // Try blkdiscard first (fast - sends TRIM commands)
        try {
            Script blkdiscard = new Script("blkdiscard", 300000, logger); // 5 minute timeout
            blkdiscard.add("-f"); // Force flag to suppress confirmation prompts
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

        // Fallback to dd zero-fill (slow)
        if (!blkdiscardSuccess) {
            logger.info("Attempting zero-fill using dd for CLVM volume: {}", volumeUuid);
            try {
                // nice -n 19: lowest CPU priority
                // ionice -c 2 -n 7: best-effort I/O scheduling with lowest priority
                // oflag=direct: bypass cache for more predictable performance
                String command = String.format(
                    "nice -n 19 ionice -c 2 -n 7 dd if=/dev/zero of=%s bs=1M oflag=direct 2>&1 || true",
                    lvPath
                );

                Script ddZeroFill = new Script("/bin/bash", 3600000, logger); // 60 minute timeout for large volumes
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

    private void deleteDirVol(LibvirtStoragePool pool, StorageVol vol) throws LibvirtException {
        Script.runSimpleBashScript("rm -r --interactive=never " + vol.getPath());
    }

    /**
     * Get Physical Extent (PE) from the volume group
     * @param vgName Volume group name
     * @return PE size in bytes, defaults to 4MiB if it cannot be determined
     */
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
     * Volumes use QCOW2-on-LVM with extended_l2=on and need:
     * - Base size (virtual size)
     * - QCOW2 metadata overhead (L1/L2 tables, refcount tables, headers)
     *
     * For QCOW2 with 64k clusters and extended L2 tables (extended_l2=on):
     * - Each 64KB cluster contains data
     * - Each L2 table entry is 16 bytes (extended L2, double the standard 8 bytes)
     * - Each 64KB L2 cluster can hold 4096 entries (64KB / 16 bytes)
     * Formula: Total overhead (MiB) = ((virtualSize_GiB × 1024 × 1024) / (64 × 4096)) × 2 + 2 MiB headers
     *
     * Quick reference (64k clusters, extended_l2=on):
     *   10 GiB virtual  → ~7 MiB overhead    → 2 PEs (8 MiB)
     *   100 GiB virtual → ~52 MiB overhead   → 13 PEs (52 MiB)
     *   1 TiB virtual   → ~514 MiB overhead  → 129 PEs (516 MiB)
     *   2 TiB virtual   → ~1026 MiB overhead → 257 PEs (1028 MiB)
     *
     * @param virtualSize Virtual disk size in bytes (for overhead calculation)
     * @param vgName Volume group name to query PE size
     * @return Size in bytes to allocate for LV
     */
    private long calculateClvmNgLvSize(long virtualSize, String vgName) {
        long peSize = getVgPhysicalExtentSize(vgName);

        long clusterSize = 64 * 1024L;
        // Each L2 entry is 16 bytes (extended_l2=on), and each 64KB cluster holds 4096 entries (64KB / 16 bytes)
        long l2Multiplier = 4096L;

        long numDataClusters = (virtualSize + clusterSize - 1) / clusterSize;
        long numL2Clusters = (numDataClusters + l2Multiplier - 1) / l2Multiplier;
        long l2TableSize = numL2Clusters * clusterSize;
        long refcountTableSize = l2TableSize;

        // Headers and other metadata (L1 table, QCOW2 header, etc.)
        long headerOverhead = 2 * 1024 * 1024L; // 2 MiB for headers
        long metadataOverhead = l2TableSize + refcountTableSize + headerOverhead;
        long targetSize = virtualSize + metadataOverhead;
        long roundedSize = ((targetSize + peSize - 1) / peSize) * peSize;
        long virtualSizeGiB = virtualSize / (1024 * 1024 * 1024L);
        long overheadMiB = metadataOverhead / (1024 * 1024L);

        logger.info("Calculated volume LV size: {} bytes (virtual: {} GiB, " +
                        "QCOW2 metadata overhead: {} MiB (64k clusters, extended_l2=on), rounded to {} PEs, PE size = {} bytes)",
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
        lvcreate.add(vgName);

        String result = lvcreate.execute();
        if (result != null) {
            throw new CloudRuntimeException("Failed to create LV for CLVM_NG volume: " + result);
        }

        Script qemuImg = new Script("qemu-img", Duration.millis(timeout), logger);
        qemuImg.add("create");
        qemuImg.add("-f", "qcow2");

        StringBuilder qcow2Options = new StringBuilder();

        // Set preallocation based on provisioning type
        // THIN: preallocation=off (sparse file, allocate on write)
        // SPARSE / FAT: preallocation=metadata (allocate metadata only)
        String preallocation;
        if (provisioningType == Storage.ProvisioningType.THIN) {
            preallocation = "off";
        } else {
            preallocation = "metadata";
        }

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

        KVMPhysicalDisk disk = new KVMPhysicalDisk(volumePath, volumeUuid, pool);
        disk.setFormat(PhysicalDiskFormat.QCOW2);
        disk.setSize(lvSize);
        disk.setVirtualSize(virtualSize);

        logger.info("Successfully created CLVM_NG volume {} with backing file (LV size: {}, virtual size: {}, provisioning: {}, preallocation: {})",
                    volumeUuid, lvSize, virtualSize, provisioningType, preallocation);

        return disk;
    }

    public void createTemplateOnClvmNg(String templatePath, String templateUuid, int timeout, KVMStoragePool pool) {
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
        long lvSize = virtualSize; // as extended_l2=off and preallocation=off

        logger.info("Template source - Physical: {} bytes, Virtual: {} bytes, LV will be: {} bytes",
                physicalSize, virtualSize, lvSize);

        Script lvcreate = new Script("lvcreate", Duration.millis(timeout), logger);
        lvcreate.add("-n", lvName);
        lvcreate.add("-L", lvSize + "B");
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
        logger.info("Created template LV {} with size {} bytes (source physical: {}, actual virtual: {})", lvName, lvSize, physicalSize, actualVirtualSize);

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

    private boolean lvExists(String lvPath) {
        Script checkLv = new Script("lvs", Duration.millis(5000), logger);
        checkLv.add("--noheadings");
        checkLv.add("--unbuffered");
        checkLv.add(lvPath);
        String checkResult = checkLv.execute();
        return checkResult == null;
    }

    private void removeLvOnFailure(String lvPath, int timeout) {
        Script lvremove = new Script("lvremove", Duration.millis(timeout), logger);
        lvremove.add("-f");
        lvremove.add(lvPath);
        lvremove.execute();
    }
}
