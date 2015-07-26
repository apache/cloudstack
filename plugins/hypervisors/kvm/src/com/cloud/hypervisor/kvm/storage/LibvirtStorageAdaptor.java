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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.Secret;
import org.libvirt.StoragePool;
import org.libvirt.StoragePoolInfo.StoragePoolState;
import org.libvirt.StorageVol;

import com.ceph.rados.IoCTX;
import com.ceph.rados.Rados;
import com.ceph.rados.RadosException;
import com.ceph.rbd.Rbd;
import com.ceph.rbd.RbdException;
import com.ceph.rbd.RbdImage;
import com.ceph.rbd.jna.RbdImageInfo;
import com.ceph.rbd.jna.RbdSnapInfo;

import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;

import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtSecretDef;
import com.cloud.hypervisor.kvm.resource.LibvirtSecretDef.usage;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolDef;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolDef.authType;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolDef.poolType;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtStorageVolumeDef;
import com.cloud.hypervisor.kvm.resource.LibvirtStorageVolumeDef.volFormat;
import com.cloud.hypervisor.kvm.resource.LibvirtStorageVolumeXMLParser;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class LibvirtStorageAdaptor implements StorageAdaptor {
    private static final Logger s_logger = Logger.getLogger(LibvirtStorageAdaptor.class);
    private StorageLayer _storageLayer;
    private String _mountPoint = "/mnt";
    private String _manageSnapshotPath;

    private String rbdTemplateSnapName = "cloudstack-base-snap";
    private int rbdFeatures = (1 << 0); /* Feature 1<<0 means layering in RBD format 2 */
    private int rbdOrder = 0; /* Order 0 means 4MB blocks (the default) */

    public LibvirtStorageAdaptor(StorageLayer storage) {
        _storageLayer = storage;
        _manageSnapshotPath = Script.findScript("scripts/storage/qcow2/", "managesnapshot.sh");
    }

    @Override
    public boolean createFolder(String uuid, String path) {
        String mountPoint = _mountPoint + File.separator + uuid;
        File f = new File(mountPoint + File.separator + path);
        if (!f.exists()) {
            f.mkdirs();
        }
        return true;
    }

    public StorageVol getVolume(StoragePool pool, String volName) {
        StorageVol vol = null;

        try {
            vol = pool.storageVolLookupByName(volName);
        } catch (LibvirtException e) {
            s_logger.debug("Could not find volume " + volName + ": " + e.getMessage());
        }

        /**
         * The volume was not found in the storage pool
         * This can happen when a volume has just been created on a different host and
         * since then the libvirt storage pool has not been refreshed.
         */
        if (vol == null) {
            try {
                s_logger.debug("Refreshing storage pool " + pool.getName());
                refreshPool(pool);
            } catch (LibvirtException e) {
                s_logger.debug("Failed to refresh storage pool: " + e.getMessage());
            }

            try {
                vol = pool.storageVolLookupByName(volName);
                s_logger.debug("Found volume " + volName + " in storage pool " + pool.getName() + " after refreshing the pool");
            } catch (LibvirtException e) {
                throw new CloudRuntimeException("Could not find volume " + volName + ": " + e.getMessage());
            }
        }

        return vol;
    }

    public StorageVol createVolume(Connect conn, StoragePool pool, String uuid, long size, volFormat format) throws LibvirtException {
        LibvirtStorageVolumeDef volDef = new LibvirtStorageVolumeDef(UUID.randomUUID().toString(), size, format, null, null);
        s_logger.debug(volDef.toString());

        return pool.storageVolCreateXML(volDef.toString(), 0);
    }

    public void storagePoolRefresh(StoragePool pool) {
        try {
            synchronized (getStoragePool(pool.getUUIDString())) {
                refreshPool(pool);
            }
        } catch (LibvirtException e) {
            s_logger.debug("refresh storage pool failed: " + e.toString());
        }
    }

    private StoragePool createNetfsStoragePool(poolType fsType, Connect conn, String uuid, String host, String path) throws LibvirtException {
        String targetPath = _mountPoint + File.separator + uuid;
        LibvirtStoragePoolDef spd = new LibvirtStoragePoolDef(fsType, uuid, uuid, host, path, targetPath);
        _storageLayer.mkdir(targetPath);
        StoragePool sp = null;
        try {
            s_logger.debug(spd.toString());
            sp = conn.storagePoolCreateXML(spd.toString(), 0);
            return sp;
        } catch (LibvirtException e) {
            s_logger.error(e.toString());
            // if error is that pool is mounted, try to handle it
            if (e.toString().contains("already mounted")) {
                s_logger.error("Attempting to unmount old mount libvirt is unaware of at " + targetPath);
                String result = Script.runSimpleBashScript("umount -l " + targetPath);
                if (result == null) {
                    s_logger.error("Succeeded in unmounting " + targetPath);
                    try {
                        sp = conn.storagePoolCreateXML(spd.toString(), 0);
                        s_logger.error("Succeeded in redefining storage");
                        return sp;
                    } catch (LibvirtException l) {
                        s_logger.error("Target was already mounted, unmounted it but failed to redefine storage:" + l);
                    }
                } else {
                    s_logger.error("Failed in unmounting and redefining storage");
                }
            } else {
                s_logger.error("Internal error occurred when attempting to mount: specified path may be invalid");
                throw e;
            }
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
                    s_logger.debug("Failed to undefine " + fsType.toString() + " storage pool with: " + l.toString());
                }
            }
            return null;
        }
    }

    private StoragePool createSharedStoragePool(Connect conn, String uuid, String host, String path) {
        String mountPoint = path;
        if (!_storageLayer.exists(mountPoint)) {
            s_logger.error(mountPoint + " does not exists. Check local.storage.path in agent.properties.");
            return null;
        }
        LibvirtStoragePoolDef spd = new LibvirtStoragePoolDef(poolType.DIR, uuid, uuid, host, path, path);
        StoragePool sp = null;
        try {
            s_logger.debug(spd.toString());
            sp = conn.storagePoolCreateXML(spd.toString(), 0);
            return sp;
        } catch (LibvirtException e) {
            s_logger.error(e.toString());
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
                    s_logger.debug("Failed to define shared mount point storage pool with: " + l.toString());
                }
            }
            return null;
        }
    }

    private StoragePool createCLVMStoragePool(Connect conn, String uuid, String host, String path) {

        String volgroupPath = "/dev/" + path;
        String volgroupName = path;
        volgroupName = volgroupName.replaceFirst("/", "");

        LibvirtStoragePoolDef spd = new LibvirtStoragePoolDef(poolType.LOGICAL, volgroupName, uuid, host, volgroupPath, volgroupPath);
        StoragePool sp = null;
        try {
            s_logger.debug(spd.toString());
            sp = conn.storagePoolCreateXML(spd.toString(), 0);
            return sp;
        } catch (LibvirtException e) {
            s_logger.error(e.toString());
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
                    s_logger.debug("Failed to define clvm storage pool with: " + l.toString());
                }
            }
            return null;
        }

    }

    private StoragePool createRBDStoragePool(Connect conn, String uuid, String host, int port, String userInfo, String path) {

        LibvirtStoragePoolDef spd;
        StoragePool sp = null;
        Secret s = null;

        String[] userInfoTemp = userInfo.split(":");
        if (userInfoTemp.length == 2) {
            LibvirtSecretDef sd = new LibvirtSecretDef(usage.CEPH, uuid);

            sd.setCephName(userInfoTemp[0] + "@" + host + ":" + port + "/" + path);

            try {
                s_logger.debug(sd.toString());
                s = conn.secretDefineXML(sd.toString());
                s.setValue(Base64.decodeBase64(userInfoTemp[1]));
            } catch (LibvirtException e) {
                s_logger.error("Failed to define the libvirt secret: " + e.toString());
                if (s != null) {
                    try {
                        s.undefine();
                        s.free();
                    } catch (LibvirtException l) {
                        s_logger.debug("Failed to undefine the libvirt secret: " + l.toString());
                    }
                }
                return null;
            }
            spd = new LibvirtStoragePoolDef(poolType.RBD, uuid, uuid, host, port, path, userInfoTemp[0], authType.CEPH, uuid);
        } else {
            spd = new LibvirtStoragePoolDef(poolType.RBD, uuid, uuid, host, port, path, "");
        }

        try {
            s_logger.debug(spd.toString());
            sp = conn.storagePoolCreateXML(spd.toString(), 0);
            return sp;
        } catch (LibvirtException e) {
            s_logger.debug("Failed to create RBD storage pool: " + e.toString());
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
                    s_logger.debug("Failed to undefine RBD storage pool: " + l.toString());
                }
            }

            if (s != null) {
                try {
                    s_logger.debug("Failed to create the RBD storage pool, cleaning up the libvirt secret");
                    s.undefine();
                    s.free();
                } catch (LibvirtException se) {
                    s_logger.debug("Failed to remove the libvirt secret: " + se.toString());
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
    public KVMStoragePool getStoragePool(String uuid) {
        return this.getStoragePool(uuid, false);
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid, boolean refreshInfo) {
        s_logger.info("Trying to fetch storage pool " + uuid + " from libvirt");
        StoragePool storage = null;
        try {
            Connect conn = LibvirtConnection.getConnection();
            storage = conn.storagePoolLookupByUUIDString(uuid);

            if (storage.getInfo().state != StoragePoolState.VIR_STORAGE_POOL_RUNNING) {
                s_logger.warn("Storage pool " + uuid + " is not in running state. Attempting to start it.");
                storage.create(0);
            }
            LibvirtStoragePoolDef spd = getStoragePoolDef(conn, storage);
            if (spd == null) {
                throw new CloudRuntimeException("Unable to parse the storage pool definition for storage pool " + uuid);
            }
            StoragePoolType type = null;
            if (spd.getPoolType() == LibvirtStoragePoolDef.poolType.NETFS) {
                type = StoragePoolType.NetworkFilesystem;
            } else if (spd.getPoolType() == LibvirtStoragePoolDef.poolType.DIR) {
                type = StoragePoolType.Filesystem;
            } else if (spd.getPoolType() == LibvirtStoragePoolDef.poolType.RBD) {
                type = StoragePoolType.RBD;
            } else if (spd.getPoolType() == LibvirtStoragePoolDef.poolType.LOGICAL) {
                type = StoragePoolType.CLVM;
            } else if (spd.getPoolType() == LibvirtStoragePoolDef.poolType.GLUSTERFS) {
                type = StoragePoolType.Gluster;
            }

            LibvirtStoragePool pool = new LibvirtStoragePool(uuid, storage.getName(), type, this, storage);

            if (pool.getType() != StoragePoolType.RBD)
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
                s_logger.info("Asking libvirt to refresh storage pool " + uuid);
                pool.refresh();
            }
            pool.setCapacity(storage.getInfo().capacity);
            pool.setUsed(storage.getInfo().allocation);
            pool.setAvailable(storage.getInfo().available);

            s_logger.debug("Succesfully refreshed pool " + uuid +
                           " Capacity: " + storage.getInfo().capacity +
                           " Used: " + storage.getInfo().allocation +
                           " Available: " + storage.getInfo().available);

            return pool;
        } catch (LibvirtException e) {
            s_logger.debug("Could not find storage pool " + uuid + " in libvirt");
            throw new CloudRuntimeException(e.toString(), e);
        }
    }

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String volumeUuid, KVMStoragePool pool) {
        LibvirtStoragePool libvirtPool = (LibvirtStoragePool)pool;

        try {
            StorageVol vol = getVolume(libvirtPool.getPool(), volumeUuid);
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
            } else if (voldef.getFormat() == LibvirtStorageVolumeDef.volFormat.QCOW2) {
                disk.setFormat(PhysicalDiskFormat.QCOW2);
            } else if (voldef.getFormat() == LibvirtStorageVolumeDef.volFormat.RAW) {
                disk.setFormat(PhysicalDiskFormat.RAW);
            }
            return disk;
        } catch (LibvirtException e) {
            s_logger.debug("Failed to get physical disk:", e);
            throw new CloudRuntimeException(e.toString());
        }

    }

    @Override
    public KVMStoragePool createStoragePool(String name, String host, int port, String path, String userInfo, StoragePoolType type) {
        s_logger.info("Attempting to create storage pool " + name + " (" + type.toString() + ") in libvirt");

        StoragePool sp = null;
        Connect conn = null;
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
                s_logger.info("Found existing defined storage pool " + name + ". It wasn't running, so we undefined it.");
            }
            if (sp != null) {
                s_logger.info("Found existing defined storage pool " + name + ", using it.");
            }
        } catch (LibvirtException e) {
            sp = null;
            s_logger.warn("Storage pool " + name + " was not found running in libvirt. Need to create it.");
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
            s_logger.info("Didn't find an existing storage pool " + name + " by UUID, checking for pools with duplicate paths");

            try {
                String[] poolnames = conn.listStoragePools();
                for (String poolname : poolnames) {
                    s_logger.debug("Checking path of existing pool " + poolname + " against pool we want to create");
                    StoragePool p = conn.storagePoolLookupByName(poolname);
                    LibvirtStoragePoolDef pdef = getStoragePoolDef(conn, p);

                    String targetPath = pdef.getTargetPath();
                    if (targetPath != null && targetPath.equals(path)) {
                        s_logger.debug("Storage pool utilizing path '" + path + "' already exists as pool " + poolname +
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
                s_logger.error("Failure in attempting to see if an existing storage pool might be using the path of the pool to be created:" + e);
            }

            s_logger.debug("Attempting to create storage pool " + name);

            if (type == StoragePoolType.NetworkFilesystem) {
                try {
                    sp = createNetfsStoragePool(poolType.NETFS, conn, name, host, path);
                } catch (LibvirtException e) {
                    s_logger.error("Failed to create netfs mount: " + host + ":" + path , e);
                    s_logger.error(e.getStackTrace());
                    throw new CloudRuntimeException(e.toString());
                }
            } else if (type == StoragePoolType.Gluster) {
                try {
                    sp = createNetfsStoragePool(poolType.GLUSTERFS, conn, name, host, path);
                } catch (LibvirtException e) {
                    s_logger.error("Failed to create glusterfs mount: " + host + ":" + path , e);
                    s_logger.error(e.getStackTrace());
                    throw new CloudRuntimeException(e.toString());
                }
            } else if (type == StoragePoolType.SharedMountPoint || type == StoragePoolType.Filesystem) {
                sp = createSharedStoragePool(conn, name, host, path);
            } else if (type == StoragePoolType.RBD) {
                sp = createRBDStoragePool(conn, name, host, port, userInfo, path);
            } else if (type == StoragePoolType.CLVM) {
                sp = createCLVMStoragePool(conn, name, host, path);
            }
        }

        if (sp == null) {
            throw new CloudRuntimeException("Failed to create storage pool: " + name);
        }

        try {
            if (sp.isActive() == 0) {
                s_logger.debug("Attempting to activate pool " + name);
                sp.create(0);
            }

            return getStoragePool(name);
        } catch (LibvirtException e) {
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

    @Override
    public boolean deleteStoragePool(String uuid) {
        s_logger.info("Attempting to remove storage pool " + uuid + " from libvirt");
        Connect conn = null;
        try {
            conn = LibvirtConnection.getConnection();
        } catch (LibvirtException e) {
            throw new CloudRuntimeException(e.toString());
        }

        StoragePool sp = null;
        Secret s = null;

        try {
            sp = conn.storagePoolLookupByUUIDString(uuid);
        } catch (LibvirtException e) {
            s_logger.warn("Storage pool " + uuid + " doesn't exist in libvirt. Assuming it is already removed");
            return true;
        }

        /*
         * Some storage pools, like RBD also have 'secret' information stored in libvirt
         * Destroy them if they exist
         */
        try {
            s = conn.secretLookupByUUIDString(uuid);
        } catch (LibvirtException e) {
            s_logger.debug("Storage pool " + uuid + " has no corresponding secret. Not removing any secret.");
        }

        try {
            if (sp.isPersistent() == 1) {
                sp.destroy();
                sp.undefine();
            } else {
                sp.destroy();
            }
            sp.free();
            if (s != null) {
                s.undefine();
                s.free();
            }

            s_logger.info("Storage pool " + uuid + " was succesfully removed from libvirt.");

            return true;
        } catch (LibvirtException e) {
            // handle ebusy error when pool is quickly destroyed
            if (e.toString().contains("exit status 16")) {
                String targetPath = _mountPoint + File.separator + uuid;
                s_logger.error("deleteStoragePool removed pool from libvirt, but libvirt had trouble unmounting the pool. Trying umount location " + targetPath +
                        "again in a few seconds");
                String result = Script.runSimpleBashScript("sleep 5 && umount " + targetPath);
                if (result == null) {
                    s_logger.error("Succeeded in unmounting " + targetPath);
                    return true;
                }
                s_logger.error("Failed to unmount " + targetPath);
            }
            throw new CloudRuntimeException(e.toString(), e);
        }
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, KVMStoragePool pool,
            PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size) {

        s_logger.info("Attempting to create volume " + name + " (" + pool.getType().toString() + ") in pool "
                + pool.getUuid() + " with size " + size);

        switch (pool.getType()){
        case RBD:
            return createPhysicalDiskOnRBD(name, pool, format, provisioningType, size);
        case NetworkFilesystem:
        case Filesystem:
            switch (format){
            case QCOW2:
                return createPhysicalDiskByQemuImg(name, pool, format, provisioningType, size);
            case RAW:
                return createPhysicalDiskByQemuImg(name, pool, format, provisioningType, size);
            case DIR:
                return createPhysicalDiskByLibVirt(name, pool, format, provisioningType, size);
            case TAR:
                return createPhysicalDiskByLibVirt(name, pool, format, provisioningType, size);
            default:
                throw new CloudRuntimeException("Unexpected disk format is specified.");
            }
        default:
            return createPhysicalDiskByLibVirt(name, pool, format, provisioningType, size);
        }
    }

    private KVMPhysicalDisk createPhysicalDiskByLibVirt(String name, KVMStoragePool pool,
            PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size) {
        LibvirtStoragePool libvirtPool = (LibvirtStoragePool) pool;
        StoragePool virtPool = libvirtPool.getPool();
        LibvirtStorageVolumeDef.volFormat libvirtformat = LibvirtStorageVolumeDef.volFormat.getFormat(format);

        String volPath = null;
        String volName = null;
        long volAllocation = 0;
        long volCapacity = 0;

        LibvirtStorageVolumeDef volDef = new LibvirtStorageVolumeDef(name,
                size, libvirtformat, null, null);
        s_logger.debug(volDef.toString());
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


    private KVMPhysicalDisk createPhysicalDiskByQemuImg(String name, KVMStoragePool pool,
            PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size) {
        String volPath = pool.getLocalPath() + "/" + name;
        String volName = name;
        long virtualSize = 0;
        long actualSize = 0;

        final int timeout = 0;

        QemuImgFile destFile = new QemuImgFile(volPath);
        destFile.setFormat(format);
        destFile.setSize(size);
        QemuImg qemu = new QemuImg(timeout);
        Map<String, String> options = new HashMap<String, String>();
        if (pool.getType() == StoragePoolType.NetworkFilesystem){
            options.put("preallocation", QemuImg.PreallocationType.getPreallocationType(provisioningType).toString());
        }

        try{
            qemu.create(destFile, options);
            Map<String, String> info = qemu.info(destFile);
            virtualSize = Long.parseLong(info.get(new String("virtual_size")));
            actualSize = new File(destFile.getFileName()).length();
        } catch (QemuImgException e) {
            s_logger.error("Failed to create " + volPath +
                    " due to a failed executing of qemu-img: " + e.getMessage());
        }

        KVMPhysicalDisk disk = new KVMPhysicalDisk(volPath, volName, pool);
        disk.setFormat(format);
        disk.setSize(actualSize);
        disk.setVirtualSize(virtualSize);
        return disk;
    }

    private KVMPhysicalDisk createPhysicalDiskOnRBD(String name, KVMStoragePool pool,
            PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size) {
        String volPath = null;

        /**
         * To have RBD function properly we want RBD images of format 2
         * libvirt currently defaults to format 1
         *
         * This has been fixed in libvirt 1.2.2, but that's not upstream
         * in all distributions
         *
         * For that reason we use the native RBD bindings to create the
         * RBD image until libvirt creates RBD format 2 by default
         */

        try {
            s_logger.info("Creating RBD image " + pool.getSourceDir() + "/" + name + " with size " + size);

            Rados r = new Rados(pool.getAuthUserName());
            r.confSet("mon_host", pool.getSourceHost() + ":" + pool.getSourcePort());
            r.confSet("key", pool.getAuthSecret());
            r.confSet("client_mount_timeout", "30");
            r.connect();
            s_logger.debug("Succesfully connected to Ceph cluster at " + r.confGet("mon_host"));

            IoCTX io = r.ioCtxCreate(pool.getSourceDir());
            Rbd rbd = new Rbd(io);
            rbd.create(name, size, rbdFeatures, rbdOrder);

            r.ioCtxDestroy(io);
        } catch (RadosException e) {
            throw new CloudRuntimeException(e.toString());
        } catch (RbdException e) {
            throw new CloudRuntimeException(e.toString());
        }

        volPath = pool.getSourceDir() + "/" + name;
        KVMPhysicalDisk disk = new KVMPhysicalDisk(volPath, name, pool);
        disk.setFormat(PhysicalDiskFormat.RAW);
        disk.setSize(size);
        disk.setVirtualSize(size);
        return disk;
    }

    @Override
    public boolean connectPhysicalDisk(String name, KVMStoragePool pool, Map<String, String> details) {
        // this is for managed storage that needs to prep disks prior to use
        return true;
    }

    @Override
    public boolean disconnectPhysicalDisk(String uuid, KVMStoragePool pool) {
        // this is for managed storage that needs to cleanup disks after use
        return true;
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

        s_logger.info("Attempting to remove volume " + uuid + " from pool " + pool.getUuid());

        /**
         * RBD volume can have snapshots and while they exist libvirt
         * can't remove the RBD volume
         *
         * We have to remove those snapshots first
         */
        if (pool.getType() == StoragePoolType.RBD) {
            try {
                s_logger.info("Unprotecting and Removing RBD snapshots of image " + pool.getSourceDir() + "/" + uuid + " prior to removing the image");

                Rados r = new Rados(pool.getAuthUserName());
                r.confSet("mon_host", pool.getSourceHost() + ":" + pool.getSourcePort());
                r.confSet("key", pool.getAuthSecret());
                r.confSet("client_mount_timeout", "30");
                r.connect();
                s_logger.debug("Succesfully connected to Ceph cluster at " + r.confGet("mon_host"));

                IoCTX io = r.ioCtxCreate(pool.getSourceDir());
                Rbd rbd = new Rbd(io);
                RbdImage image = rbd.open(uuid);
                s_logger.debug("Fetching list of snapshots of RBD image " + pool.getSourceDir() + "/" + uuid);
                List<RbdSnapInfo> snaps = image.snapList();
                for (RbdSnapInfo snap : snaps) {
                    if (image.snapIsProtected(snap.name)) {
                        s_logger.debug("Unprotecting snapshot " + pool.getSourceDir() + "/" + uuid + "@" + snap.name);
                        image.snapUnprotect(snap.name);
                    } else {
                        s_logger.debug("Snapshot " + pool.getSourceDir() + "/" + uuid + "@" + snap.name + " is not protected.");
                    }
                    s_logger.debug("Removing snapshot " + pool.getSourceDir() + "/" + uuid + "@" + snap.name);
                    image.snapRemove(snap.name);
                }

                rbd.close(image);
                r.ioCtxDestroy(io);

                s_logger.debug("Succesfully unprotected and removed any snapshots of " + pool.getSourceDir() + "/" + uuid +
                        " Continuing to remove the RBD image");
            } catch (RadosException e) {
                throw new CloudRuntimeException(e.toString());
            } catch (RbdException e) {
                throw new CloudRuntimeException(e.toString());
            }
        }

        LibvirtStoragePool libvirtPool = (LibvirtStoragePool)pool;
        try {
            StorageVol vol = getVolume(libvirtPool.getPool(), uuid);
            s_logger.debug("Instructing libvirt to remove volume " + uuid + " from pool " + pool.getUuid());
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
            String name, PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, KVMStoragePool destPool, int timeout) {

        s_logger.info("Creating volume " + name + " from template " + template.getName() + " in pool " + destPool.getUuid() +
                " (" + destPool.getType().toString() + ") with size " + size);

        KVMPhysicalDisk disk = null;

        if (destPool.getType() == StoragePoolType.RBD) {
            disk = createDiskFromTemplateOnRBD(template, name, format, provisioningType, size, destPool, timeout);
        } else {
            try {
                String newUuid = name;
                disk = destPool.createPhysicalDisk(newUuid, format, provisioningType, template.getVirtualSize());
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
                    switch(provisioningType){
                    case THIN:
                        QemuImgFile backingFile = new QemuImgFile(template.getPath(), template.getFormat());
                        qemu.create(destFile, backingFile, options);
                        break;
                    case SPARSE:
                    case FAT:
                        QemuImgFile srcFile = new QemuImgFile(template.getPath(), template.getFormat());
                        qemu.convert(srcFile, destFile, options);
                        break;
                    }
                } else if (format == PhysicalDiskFormat.RAW) {
                    QemuImgFile sourceFile = new QemuImgFile(template.getPath(), template.getFormat());
                    QemuImgFile destFile = new QemuImgFile(disk.getPath(), PhysicalDiskFormat.RAW);
                    if (size > template.getVirtualSize()) {
                        destFile.setSize(size);
                    } else {
                        destFile.setSize(template.getVirtualSize());
                    }
                    QemuImg qemu = new QemuImg(timeout);
                    Map<String, String> options = new HashMap<String, String>();
                    qemu.convert(sourceFile, destFile, options);
                }
            } catch (QemuImgException e) {
                s_logger.error("Failed to create " + disk.getPath() +
                        " due to a failed executing of qemu-img: " + e.getMessage());
            }
        }


        return disk;
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


        QemuImg qemu = new QemuImg(timeout);
        QemuImgFile srcFile;
        QemuImgFile destFile = new QemuImgFile(KVMPhysicalDisk.RBDStringBuilder(destPool.getSourceHost(),
                destPool.getSourcePort(),
                destPool.getAuthUserName(),
                destPool.getAuthSecret(),
                disk.getPath()));
        destFile.setFormat(format);


        if (srcPool.getType() != StoragePoolType.RBD) {
            srcFile = new QemuImgFile(template.getPath(), template.getFormat());
            try{
                qemu.convert(srcFile, destFile);
            } catch (QemuImgException e) {
                s_logger.error("Failed to create " + disk.getPath() +
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
                    s_logger.debug("Trying to perform a RBD clone (layering) since we are operating in the same storage pool");

                    Rados r = new Rados(srcPool.getAuthUserName());
                    r.confSet("mon_host", srcPool.getSourceHost() + ":" + srcPool.getSourcePort());
                    r.confSet("key", srcPool.getAuthSecret());
                    r.confSet("client_mount_timeout", "30");
                    r.connect();
                    s_logger.debug("Succesfully connected to Ceph cluster at " + r.confGet("mon_host"));

                    IoCTX io = r.ioCtxCreate(srcPool.getSourceDir());
                    Rbd rbd = new Rbd(io);
                    RbdImage srcImage = rbd.open(template.getName());

                    if (srcImage.isOldFormat()) {
                        /* The source image is RBD format 1, we have to do a regular copy */
                        s_logger.debug("The source image " + srcPool.getSourceDir() + "/" + template.getName() +
                                " is RBD format 1. We have to perform a regular copy (" + disk.getVirtualSize() + " bytes)");

                        rbd.create(disk.getName(), disk.getVirtualSize(), rbdFeatures, rbdOrder);
                        RbdImage destImage = rbd.open(disk.getName());

                        s_logger.debug("Starting to copy " + srcImage.getName() +  " to " + destImage.getName() + " in Ceph pool " + srcPool.getSourceDir());
                        rbd.copy(srcImage, destImage);

                        s_logger.debug("Finished copying " + srcImage.getName() +  " to " + destImage.getName() + " in Ceph pool " + srcPool.getSourceDir());
                        rbd.close(destImage);
                    } else {
                        s_logger.debug("The source image " + srcPool.getSourceDir() + "/" + template.getName()
                                + " is RBD format 2. We will perform a RBD clone using snapshot "
                                + rbdTemplateSnapName);
                        /* The source image is format 2, we can do a RBD snapshot+clone (layering) */


                        s_logger.debug("Checking if RBD snapshot " + srcPool.getSourceDir() + "/" + template.getName()
                                + "@" + rbdTemplateSnapName + " exists prior to attempting a clone operation.");

                        List<RbdSnapInfo> snaps = srcImage.snapList();
                        s_logger.debug("Found " + snaps.size() +  " snapshots on RBD image " + srcPool.getSourceDir() + "/" + template.getName());
                        boolean snapFound = false;
                        for (RbdSnapInfo snap : snaps) {
                            if (rbdTemplateSnapName.equals(snap.name)) {
                                s_logger.debug("RBD snapshot " + srcPool.getSourceDir() + "/" + template.getName()
                                        + "@" + rbdTemplateSnapName + " already exists.");
                                snapFound = true;
                                break;
                            }
                        }

                        if (!snapFound) {
                            s_logger.debug("Creating RBD snapshot " + rbdTemplateSnapName + " on image " + name);
                            srcImage.snapCreate(rbdTemplateSnapName);
                            s_logger.debug("Protecting RBD snapshot " + rbdTemplateSnapName + " on image " + name);
                            srcImage.snapProtect(rbdTemplateSnapName);
                        }

                        rbd.clone(template.getName(), rbdTemplateSnapName, io, disk.getName(), rbdFeatures, rbdOrder);
                        s_logger.debug("Succesfully cloned " + template.getName() + "@" + rbdTemplateSnapName + " to " + disk.getName());
                        /* We also need to resize the image if the VM was deployed with a larger root disk size */
                        if (disk.getVirtualSize() > template.getVirtualSize()) {
                            RbdImage diskImage = rbd.open(disk.getName());
                            diskImage.resize(disk.getVirtualSize());
                            rbd.close(diskImage);
                            s_logger.debug("Resized " + disk.getName() + " to " + disk.getVirtualSize());
                        }

                    }

                    rbd.close(srcImage);
                    r.ioCtxDestroy(io);
                } else {
                    /* The source pool or host is not the same Ceph cluster, we do a simple copy with Qemu-Img */
                    s_logger.debug("Both the source and destination are RBD, but not the same Ceph cluster. Performing a copy");

                    Rados rSrc = new Rados(srcPool.getAuthUserName());
                    rSrc.confSet("mon_host", srcPool.getSourceHost() + ":" + srcPool.getSourcePort());
                    rSrc.confSet("key", srcPool.getAuthSecret());
                    rSrc.confSet("client_mount_timeout", "30");
                    rSrc.connect();
                    s_logger.debug("Succesfully connected to source Ceph cluster at " + rSrc.confGet("mon_host"));

                    Rados rDest = new Rados(destPool.getAuthUserName());
                    rDest.confSet("mon_host", destPool.getSourceHost() + ":" + destPool.getSourcePort());
                    rDest.confSet("key", destPool.getAuthSecret());
                    rDest.confSet("client_mount_timeout", "30");
                    rDest.connect();
                    s_logger.debug("Succesfully connected to source Ceph cluster at " + rDest.confGet("mon_host"));

                    IoCTX sIO = rSrc.ioCtxCreate(srcPool.getSourceDir());
                    Rbd sRbd = new Rbd(sIO);

                    IoCTX dIO = rDest.ioCtxCreate(destPool.getSourceDir());
                    Rbd dRbd = new Rbd(dIO);

                    s_logger.debug("Creating " + disk.getName() + " on the destination cluster " + rDest.confGet("mon_host") + " in pool " +
                            destPool.getSourceDir());
                    dRbd.create(disk.getName(), disk.getVirtualSize(), rbdFeatures, rbdOrder);

                    RbdImage srcImage = sRbd.open(template.getName());
                    RbdImage destImage = dRbd.open(disk.getName());

                    s_logger.debug("Copying " + template.getName() + " from Ceph cluster " + rSrc.confGet("mon_host") + " to " + disk.getName()
                            + " on cluster " + rDest.confGet("mon_host"));
                    sRbd.copy(srcImage, destImage);

                    sRbd.close(srcImage);
                    dRbd.close(destImage);

                    rSrc.ioCtxDestroy(sIO);
                    rDest.ioCtxDestroy(dIO);
                }
            } catch (RadosException e) {
                s_logger.error("Failed to perform a RADOS action on the Ceph cluster, the error was: " + e.getMessage());
                disk = null;
            } catch (RbdException e) {
                s_logger.error("Failed to perform a RBD action on the Ceph cluster, the error was: " + e.getMessage());
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

    /**
     * This copies a volume from Primary Storage to Secondary Storage
     *
     * In theory it could also do it the other way around, but the current implementation
     * in ManagementServerImpl shows that the destPool is always a Secondary Storage Pool
     */
    @Override
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPool, int timeout) {

        /**
            With RBD you can't run qemu-img convert with an existing RBD image as destination
            qemu-img will exit with the error that the destination already exists.
            So for RBD we don't create the image, but let qemu-img do that for us.

            We then create a KVMPhysicalDisk object that we can return

            It is however very unlikely that the destPool will be RBD, since it isn't supported
            for Secondary Storage
         */

        KVMStoragePool srcPool = disk.getPool();
        PhysicalDiskFormat sourceFormat = disk.getFormat();
        String sourcePath = disk.getPath();

        KVMPhysicalDisk newDisk;
        s_logger.debug("copyPhysicalDisk: disk size:" + disk.getSize() + ", virtualsize:" + disk.getVirtualSize()+" format:"+disk.getFormat());
        if (destPool.getType() != StoragePoolType.RBD) {
            if (disk.getFormat() == PhysicalDiskFormat.TAR) {
                newDisk = destPool.createPhysicalDisk(name, PhysicalDiskFormat.DIR, Storage.ProvisioningType.THIN, disk.getVirtualSize());
            } else {
                    newDisk = destPool.createPhysicalDisk(name, Storage.ProvisioningType.THIN, disk.getVirtualSize());
            }
        } else {
            newDisk = new KVMPhysicalDisk(destPool.getSourceDir() + "/" + name, name, destPool);
            newDisk.setFormat(PhysicalDiskFormat.RAW);
            newDisk.setSize(disk.getVirtualSize());
            newDisk.setVirtualSize(disk.getSize());
        }

        String destPath = newDisk.getPath();
        PhysicalDiskFormat destFormat = newDisk.getFormat();

        QemuImg qemu = new QemuImg(timeout);
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
                    String backingFile = info.get(new String("backing_file"));
                    // qcow2 templates can just be copied into place
                    if (sourceFormat.equals(destFormat) && backingFile == null && sourcePath.endsWith(".qcow2")) {
                        String result = Script.runSimpleBashScript("cp -f " + sourcePath + " " + destPath, timeout);
                        if (result != null) {
                            throw new CloudRuntimeException("Failed to create disk: " + result);
                        }
                    } else {
                        destFile = new QemuImgFile(destPath, destFormat);
                        try {
                            qemu.convert(srcFile, destFile);
                            Map<String, String> destInfo = qemu.info(destFile);
                            Long virtualSize = Long.parseLong(destInfo.get(new String("virtual_size")));
                            newDisk.setVirtualSize(virtualSize);
                            newDisk.setSize(virtualSize);
                        } catch (QemuImgException e) {
                            s_logger.error("Failed to convert " + srcFile.getFileName() + " to " + destFile.getFileName() + " the error was: " + e.getMessage());
                            newDisk = null;
                        }
                    }
                } catch (QemuImgException e) {
                    s_logger.error("Failed to fetch the information of file " + srcFile.getFileName() + " the error was: " + e.getMessage());
                    newDisk = null;
                }
            }
        } else if ((srcPool.getType() != StoragePoolType.RBD) && (destPool.getType() == StoragePoolType.RBD)) {
            /**
             * Using qemu-img we copy the QCOW2 disk to RAW (on RBD) directly.
             * To do so it's mandatory that librbd on the system is at least 0.67.7 (Ceph Dumpling)
             */
            s_logger.debug("The source image is not RBD, but the destination is. We will convert into RBD format 2");
            try {
                srcFile = new QemuImgFile(sourcePath, sourceFormat);
                String rbdDestPath = destPool.getSourceDir() + "/" + name;
                String rbdDestFile = KVMPhysicalDisk.RBDStringBuilder(destPool.getSourceHost(),
                        destPool.getSourcePort(),
                        destPool.getAuthUserName(),
                        destPool.getAuthSecret(),
                        rbdDestPath);
                destFile = new QemuImgFile(rbdDestFile, destFormat);

                s_logger.debug("Starting copy from source image " + srcFile.getFileName() + " to RBD image " + rbdDestPath);
                qemu.convert(srcFile, destFile);
                s_logger.debug("Succesfully converted source image " + srcFile.getFileName() + " to RBD image " + rbdDestPath);

                /* We have to stat the RBD image to see how big it became afterwards */
                Rados r = new Rados(destPool.getAuthUserName());
                r.confSet("mon_host", destPool.getSourceHost() + ":" + destPool.getSourcePort());
                r.confSet("key", destPool.getAuthSecret());
                r.confSet("client_mount_timeout", "30");
                r.connect();
                s_logger.debug("Succesfully connected to Ceph cluster at " + r.confGet("mon_host"));

                IoCTX io = r.ioCtxCreate(destPool.getSourceDir());
                Rbd rbd = new Rbd(io);

                RbdImage image = rbd.open(name);
                RbdImageInfo rbdInfo = image.stat();
                newDisk.setSize(rbdInfo.size);
                newDisk.setVirtualSize(rbdInfo.size);
                s_logger.debug("After copy the resulting RBD image " + rbdDestPath + " is " + rbdInfo.size + " bytes long");
                rbd.close(image);

                r.ioCtxDestroy(io);
            } catch (QemuImgException e) {
                s_logger.error("Failed to convert from " + srcFile.getFileName() + " to " + destFile.getFileName() + " the error was: " + e.getMessage());
                newDisk = null;
            } catch (RadosException e) {
                s_logger.error("A Ceph RADOS operation failed (" + e.getReturnValue() + "). The error was: " + e.getMessage());
                newDisk = null;
            } catch (RbdException e) {
                s_logger.error("A Ceph RBD operation failed (" + e.getReturnValue() + "). The error was: " + e.getMessage());
                newDisk = null;
            }
        } else {
            /**
                We let Qemu-Img do the work here. Although we could work with librbd and have that do the cloning
                it doesn't benefit us. It's better to keep the current code in place which works
             */
            srcFile =
                    new QemuImgFile(KVMPhysicalDisk.RBDStringBuilder(srcPool.getSourceHost(), srcPool.getSourcePort(), srcPool.getAuthUserName(), srcPool.getAuthSecret(),
                            sourcePath));
            srcFile.setFormat(sourceFormat);
            destFile = new QemuImgFile(destPath);
            destFile.setFormat(destFormat);

            try {
                qemu.convert(srcFile, destFile);
            } catch (QemuImgException e) {
                s_logger.error("Failed to convert " + srcFile.getFileName() + " to " + destFile.getFileName() + " the error was: " + e.getMessage());
                newDisk = null;
            }
        }

        if (newDisk == null) {
            throw new CloudRuntimeException("Failed to copy " + disk.getPath() + " to " + name);
        }

        return newDisk;
    }

    @Override
    public KVMPhysicalDisk createDiskFromSnapshot(KVMPhysicalDisk snapshot, String snapshotName, String name, KVMStoragePool destPool) {
        return null;
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

    private void deleteDirVol(LibvirtStoragePool pool, StorageVol vol) throws LibvirtException {
        Script.runSimpleBashScript("rm -r --interactive=never " + vol.getPath());
    }
}
