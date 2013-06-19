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
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.apache.commons.codec.binary.Base64;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.Secret;
import org.libvirt.StoragePool;
import org.libvirt.StoragePoolInfo;
import org.libvirt.StorageVol;
import org.libvirt.StoragePoolInfo.StoragePoolState;
import com.ceph.rados.Rados;
import com.ceph.rados.RadosException;
import com.ceph.rados.IoCTX;
import com.ceph.rbd.Rbd;
import com.ceph.rbd.RbdImage;
import com.ceph.rbd.RbdException;

import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtSecretDef;
import com.cloud.hypervisor.kvm.resource.LibvirtSecretDef.usage;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolDef;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtStorageVolumeDef;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolDef.poolType;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolDef.authType;
import com.cloud.hypervisor.kvm.resource.LibvirtStorageVolumeDef.volFormat;
import com.cloud.hypervisor.kvm.resource.LibvirtStorageVolumeXMLParser;
import com.cloud.exception.InternalErrorException;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

public class LibvirtStorageAdaptor implements StorageAdaptor {
    private static final Logger s_logger = Logger
            .getLogger(LibvirtStorageAdaptor.class);
    private StorageLayer _storageLayer;
    private String _mountPoint = "/mnt";
    private String _manageSnapshotPath;

    private String rbdTemplateSnapName = "cloudstack-base-snap";

    public LibvirtStorageAdaptor(StorageLayer storage) {
        _storageLayer = storage;
        _manageSnapshotPath = Script.findScript("scripts/storage/qcow2/",
                "managesnapshot.sh");
    }

    @Override
    public boolean createFolder(String uuid, String path) {
        String mountPoint = _mountPoint + File.separator + uuid;
        File f = new File(mountPoint + path);
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

        }
        if (vol == null) {
            storagePoolRefresh(pool);
            try {
                vol = pool.storageVolLookupByName(volName);
            } catch (LibvirtException e) {
                throw new CloudRuntimeException(e.toString());
            }
        }
        return vol;
    }

    public StorageVol createVolume(Connect conn, StoragePool pool, String uuid,
            long size, volFormat format) throws LibvirtException {
        LibvirtStorageVolumeDef volDef = new LibvirtStorageVolumeDef(UUID
                .randomUUID().toString(), size, format, null, null);
        s_logger.debug(volDef.toString());
        return pool.storageVolCreateXML(volDef.toString(), 0);
    }

    public void storagePoolRefresh(StoragePool pool) {
        try {
            synchronized (getStoragePool(pool.getUUIDString())) {
                pool.refresh(0);
            }
        } catch (LibvirtException e) {

        }
    }

    private StoragePool createNfsStoragePool(Connect conn, String uuid,
            String host, String path) throws LibvirtException {
        String targetPath = _mountPoint + File.separator + uuid;
        LibvirtStoragePoolDef spd = new LibvirtStoragePoolDef(poolType.NETFS,
                uuid, uuid, host, path, targetPath);
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
                s_logger.error("Attempting to unmount old mount libvirt is unaware of at "+targetPath);
                String result = Script.runSimpleBashScript("umount " + targetPath );
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
                    s_logger.debug("Failed to undefine nfs storage pool with: "
                        + l.toString());
                }
            }
            return null;
        }
    }

    private StoragePool createSharedStoragePool(Connect conn, String uuid,
            String host, String path) {
        String mountPoint = path;
        if (!_storageLayer.exists(mountPoint)) {
            s_logger.error(mountPoint + " does not exists. Check local.storage.path in agent.properties.");
            return null;
        }
        LibvirtStoragePoolDef spd = new LibvirtStoragePoolDef(poolType.DIR,
                uuid, uuid, host, path, path);
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
                    s_logger.debug("Failed to define shared mount point storage pool with: "
                            + l.toString());
                }
            }
            return null;
        }
    }

    private StoragePool createCLVMStoragePool(Connect conn, String uuid,
            String host, String path) {

        String volgroupPath = "/dev/" + path;
        String volgroupName = path;
        volgroupName = volgroupName.replaceFirst("/", "");

        LibvirtStoragePoolDef spd = new LibvirtStoragePoolDef(poolType.LOGICAL,
                volgroupName, uuid, host, volgroupPath, volgroupPath);
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
                    s_logger.debug("Failed to define clvm storage pool with: "
                            + l.toString());
                }
            }
            return null;
        }

    }

    private StoragePool createRBDStoragePool(Connect conn, String uuid,
        String host, int port, String userInfo, String path) {

        LibvirtStoragePoolDef spd;
        StoragePool sp = null;

        String[] userInfoTemp = userInfo.split(":");
        if (userInfoTemp.length == 2) {
            LibvirtSecretDef sd = new LibvirtSecretDef(usage.CEPH, uuid);

            Secret s = null;

            sd.setCephName(userInfoTemp[0] + "@" + host + ":" + port + "/" + path);

            try {
                s_logger.debug(sd.toString());
                s = conn.secretDefineXML(sd.toString());
                s.setValue(Base64.decodeBase64(userInfoTemp[1]));
            } catch (LibvirtException e) {
                s_logger.error(e.toString());
                if (s != null) {
                    try {
                        s.undefine();
                        s.free();
                    } catch (LibvirtException l) {
                        s_logger.debug("Failed to define secret with: " + l.toString());
                        }
                }
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
            s_logger.debug(e.toString());
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
                    s_logger.debug("Failed to define RBD storage pool with: " + l.toString());
                }
            }
            return null;
        }
    }

    public StorageVol copyVolume(StoragePool destPool,
            LibvirtStorageVolumeDef destVol, StorageVol srcVol, int timeout)
            throws LibvirtException {
        StorageVol vol = destPool.storageVolCreateXML(destVol.toString(), 0);
        String srcPath = srcVol.getKey();
        String destPath = vol.getKey();
        Script.runSimpleBashScript("cp " + srcPath + " " + destPath, timeout);
        return vol;
    }

    public boolean copyVolume(String srcPath, String destPath,
            String volumeName, int timeout) throws InternalErrorException {
        _storageLayer.mkdirs(destPath);
        if (!_storageLayer.exists(srcPath)) {
            throw new InternalErrorException("volume:" + srcPath
                    + " is not exits");
        }
        String result = Script.runSimpleBashScript("cp " + srcPath + " "
                + destPath + File.separator + volumeName, timeout);
        if (result != null) {
            return false;
        } else {
            return true;
        }
    }

    public LibvirtStoragePoolDef getStoragePoolDef(Connect conn,
            StoragePool pool) throws LibvirtException {
        String poolDefXML = pool.getXMLDesc(0);
        LibvirtStoragePoolXMLParser parser = new LibvirtStoragePoolXMLParser();
        return parser.parseStoragePoolXML(poolDefXML);
    }

    public LibvirtStorageVolumeDef getStorageVolumeDef(Connect conn,
            StorageVol vol) throws LibvirtException {
        String volDefXML = vol.getXMLDesc(0);
        LibvirtStorageVolumeXMLParser parser = new LibvirtStorageVolumeXMLParser();
        return parser.parseStorageVolumeXML(volDefXML);
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid) {
        StoragePool storage = null;
        try {
            Connect conn = LibvirtConnection.getConnection();
            storage = conn.storagePoolLookupByUUIDString(uuid);

            if (storage.getInfo().state != StoragePoolState.VIR_STORAGE_POOL_RUNNING) {
                storage.create(0);
            }
            LibvirtStoragePoolDef spd = getStoragePoolDef(conn, storage);
            StoragePoolType type = null;
            if (spd.getPoolType() == LibvirtStoragePoolDef.poolType.NETFS
                    || spd.getPoolType() == LibvirtStoragePoolDef.poolType.DIR) {
                type = StoragePoolType.Filesystem;
            } else if (spd.getPoolType() == LibvirtStoragePoolDef.poolType.RBD) {
                type = StoragePoolType.RBD;
            } else if (spd.getPoolType() == LibvirtStoragePoolDef.poolType.LOGICAL) {
                type = StoragePoolType.CLVM;
            }

            LibvirtStoragePool pool = new LibvirtStoragePool(uuid, storage.getName(),
                                                            type, this, storage);

            if (pool.getType() != StoragePoolType.RBD) {
                pool.setLocalPath(spd.getTargetPath());
            } else {
                pool.setLocalPath("");
                pool.setSourceHost(spd.getSourceHost());
                pool.setSourcePort(spd.getSourcePort());
                pool.setSourceDir(spd.getSourceDir());
                String authUsername = spd.getAuthUserName();
                if (authUsername != null) {
                    Secret secret = conn.secretLookupByUUIDString(spd.getSecretUUID());
                    String secretValue = new String(Base64.encodeBase64(secret.getByteValue()));
                    pool.setAuthUsername(authUsername);
                    pool.setAuthSecret(secretValue);
                }
            }

            pool.refresh();
            pool.setCapacity(storage.getInfo().capacity);
            pool.setUsed(storage.getInfo().allocation);
            pool.setAvailable(storage.getInfo().available);

            return pool;
        } catch (LibvirtException e) {
            throw new CloudRuntimeException(e.toString());
        }
    }

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String volumeUuid,
            KVMStoragePool pool) {
        LibvirtStoragePool libvirtPool = (LibvirtStoragePool) pool;

        try {
            StorageVol vol = this.getVolume(libvirtPool.getPool(), volumeUuid);
            KVMPhysicalDisk disk;
            LibvirtStorageVolumeDef voldef = getStorageVolumeDef(libvirtPool
                    .getPool().getConnect(), vol);
            disk = new KVMPhysicalDisk(vol.getPath(), vol.getName(), pool);
            disk.setSize(vol.getInfo().allocation);
            disk.setVirtualSize(vol.getInfo().capacity);
            if (voldef.getFormat() == null) {
                File diskDir = new File(disk.getPath());
                if (diskDir.exists() && diskDir.isDirectory()) {
                    disk.setFormat(PhysicalDiskFormat.DIR);
                } else if (volumeUuid.endsWith("tar") || volumeUuid.endsWith(("TAR"))) {
                    disk.setFormat(PhysicalDiskFormat.TAR);
                } else {
                    disk.setFormat(pool.getDefaultFormat());
                }
            } else if (pool.getType() == StoragePoolType.RBD) {
                disk.setFormat(PhysicalDiskFormat.RAW);
            } else if (voldef.getFormat() == LibvirtStorageVolumeDef.volFormat.QCOW2) {
                disk.setFormat(PhysicalDiskFormat.QCOW2);
            } else if (voldef.getFormat() == LibvirtStorageVolumeDef.volFormat.RAW) {
                disk.setFormat(PhysicalDiskFormat.RAW);
            }
            return disk;
        } catch (LibvirtException e) {
            throw new CloudRuntimeException(e.toString());
        }

    }

    @Override
    public KVMStoragePool createStoragePool(String name, String host, int port,
                                            String path, String userInfo, StoragePoolType type) {
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
                s_logger.debug("Found existing defined storage pool " + name + ". It wasn't running, so we undefined it.");
            }
            if (sp != null) {
                s_logger.debug("Found existing defined storage pool " + name + ", using it.");
            }
        } catch (LibvirtException e) {
            sp = null;
            s_logger.debug("createStoragePool didn't find existing running pool: " + e + ", need to create it");
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
            s_logger.debug("Didn't find an existing storage pool " + name 
                            + " by UUID, checking for pools with duplicate paths");

            try {
                String[] poolnames = conn.listStoragePools();
                for (String poolname : poolnames) {
                    s_logger.debug("Checking path of existing pool " + poolname 
                                    + " against pool we want to create");
                    StoragePool p = conn.storagePoolLookupByName(poolname);
                    LibvirtStoragePoolDef pdef = getStoragePoolDef(conn, p);

                    String targetPath = pdef.getTargetPath();
                    if (targetPath != null && targetPath.equals(path)) {
                        s_logger.debug("Storage pool utilizing path '" + path + "' already exists as pool "
                                       + poolname + ", undefining so we can re-define with correct name " + name);
                        if (p.isPersistent() == 1) {
                            p.destroy();
                            p.undefine();
                        } else {
                            p.destroy();
                        }
                    }
                }
            } catch (LibvirtException e) {
                s_logger.error("Failure in attempting to see if an existing storage pool might " 
                               + "be using the path of the pool to be created:" + e);
            }

            s_logger.debug("Attempting to create storage pool " + name);

            if (type == StoragePoolType.NetworkFilesystem) {
                try {
                        sp = createNfsStoragePool(conn, name, host, path);
                } catch (LibvirtException e) {
                        s_logger.error("Failed to create mount");
                        s_logger.error(e.getStackTrace());
                        throw new CloudRuntimeException(e.toString());
                }
            } else if (type == StoragePoolType.SharedMountPoint
                    || type == StoragePoolType.Filesystem) {
                sp = createSharedStoragePool(conn, name, host, path);
            } else if (type == StoragePoolType.RBD) {
                sp = createRBDStoragePool(conn, name, host, port, userInfo, path);
            } else if (type == StoragePoolType.CLVM) {
                sp = createCLVMStoragePool(conn, name, host, path);
            }
        }

        try {
            if (sp.isActive() == 0) {
                s_logger.debug("attempting to activate pool " + name);
                sp.create(0);
            }

            LibvirtStoragePoolDef spd = getStoragePoolDef(conn, sp);
            LibvirtStoragePool pool = new LibvirtStoragePool(name,
                    sp.getName(), type, this, sp);

            if (pool.getType() != StoragePoolType.RBD) {
                pool.setLocalPath(spd.getTargetPath());
            } else {
                pool.setLocalPath("");
            }

            pool.setCapacity(sp.getInfo().capacity);
            pool.setUsed(sp.getInfo().allocation);
            pool.setAvailable(sp.getInfo().available);
  
            return pool;
        } catch (LibvirtException e) {
            String error = e.toString();
            if (error.contains("Storage source conflict")) {
                throw new CloudRuntimeException("A pool matching this location already exists in libvirt, "
                                  + " but has a different UUID/Name. Cannot create new pool without first " 
                                  + " removing it. Check for inactive pools via 'virsh pool-list --all'. " 
                                  + error);
            } else {
                throw new CloudRuntimeException(error);
            }
        }
    }

    @Override
    public boolean deleteStoragePool(String uuid) {
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
            return true;
        }

        /*
         * Some storage pools, like RBD also have 'secret' information stored in libvirt
         * Destroy them if they exist
        */
        try {
            s = conn.secretLookupByUUIDString(uuid);
        } catch (LibvirtException e) {
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
            return true;
        } catch (LibvirtException e) {
            // handle ebusy error when pool is quickly destroyed
            if (e.toString().contains("exit status 16")) {
                String targetPath = _mountPoint + File.separator + uuid;
                s_logger.error("deleteStoragePool removed pool from libvirt, but libvirt had trouble"
                               + "unmounting the pool. Trying umount location " + targetPath
                               + "again in a few seconds");
                String result = Script.runSimpleBashScript("sleep 5 && umount " + targetPath );
                if (result == null) {
                    s_logger.error("Succeeded in unmounting " + targetPath);
                    return true;
                }
                s_logger.error("failed in umount retry");
            }
            throw new CloudRuntimeException(e.toString());
        }
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, KVMStoragePool pool,
            PhysicalDiskFormat format, long size) {
        LibvirtStoragePool libvirtPool = (LibvirtStoragePool) pool;
        StoragePool virtPool = libvirtPool.getPool();
        LibvirtStorageVolumeDef.volFormat libvirtformat = null;

        if (pool.getType() == StoragePoolType.RBD) {
            format = PhysicalDiskFormat.RAW;
        }

        if (format == PhysicalDiskFormat.QCOW2) {
            libvirtformat = LibvirtStorageVolumeDef.volFormat.QCOW2;
        } else if (format == PhysicalDiskFormat.RAW) {
            libvirtformat = LibvirtStorageVolumeDef.volFormat.RAW;
        } else if (format == PhysicalDiskFormat.DIR) {
            libvirtformat = LibvirtStorageVolumeDef.volFormat.DIR;
        } else if (format == PhysicalDiskFormat.TAR) {
            libvirtformat = LibvirtStorageVolumeDef.volFormat.TAR;
        }

        LibvirtStorageVolumeDef volDef = new LibvirtStorageVolumeDef(name,
                size, libvirtformat, null, null);
        s_logger.debug(volDef.toString());
        try {
            StorageVol vol = virtPool.storageVolCreateXML(volDef.toString(), 0);
            KVMPhysicalDisk disk = new KVMPhysicalDisk(vol.getPath(),
                    vol.getName(), pool);
            disk.setFormat(format);
            disk.setSize(vol.getInfo().allocation);
            disk.setVirtualSize(vol.getInfo().capacity);
            return disk;
        } catch (LibvirtException e) {
            throw new CloudRuntimeException(e.toString());
        }
    }

    @Override
    public boolean deletePhysicalDisk(String uuid, KVMStoragePool pool) {
        LibvirtStoragePool libvirtPool = (LibvirtStoragePool) pool;
        try {
            StorageVol vol = this.getVolume(libvirtPool.getPool(), uuid);
            vol.delete(0);
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
            String name, PhysicalDiskFormat format, long size, KVMStoragePool destPool) {

        String newUuid = UUID.randomUUID().toString();
        KVMStoragePool srcPool = template.getPool();
        KVMPhysicalDisk disk = null;

        /*
            With RBD you can't run qemu-img convert with an existing RBD image as destination
            qemu-img will exit with the error that the destination already exists.
            So for RBD we don't create the image, but let qemu-img do that for us.

            We then create a KVMPhysicalDisk object that we can return
        */
        try {
            if (destPool.getType() != StoragePoolType.RBD) {
                disk = destPool.createPhysicalDisk(newUuid, format, template.getVirtualSize());
                if (template.getFormat() == PhysicalDiskFormat.TAR) {
                    Script.runSimpleBashScript("tar -x -f " + template.getPath() + " -C " + disk.getPath());
                } else if (template.getFormat() == PhysicalDiskFormat.DIR) {
                    Script.runSimpleBashScript("mkdir -p " + disk.getPath());
                    Script.runSimpleBashScript("chmod 755 " + disk.getPath());
                    Script.runSimpleBashScript("cp -p -r " + template.getPath() + "/* " + disk.getPath());
                } else if (format == PhysicalDiskFormat.QCOW2) {
                    QemuImgFile backingFile = new QemuImgFile(template.getPath(), template.getFormat());
                    QemuImgFile destFile = new QemuImgFile(disk.getPath());
                    QemuImg qemu = new QemuImg();
                    qemu.create(destFile, backingFile);
                } else if (format == PhysicalDiskFormat.RAW) {
                    QemuImgFile sourceFile = new QemuImgFile(template.getPath(), template.getFormat());
                    QemuImgFile destFile = new QemuImgFile(disk.getPath(), PhysicalDiskFormat.RAW);
                    QemuImg qemu = new QemuImg();
                    qemu.convert(sourceFile, destFile);
                }
            } else {
                disk = new KVMPhysicalDisk(destPool.getSourceDir() + "/" + newUuid, newUuid, destPool);
                disk.setFormat(format);
                disk.setSize(template.getVirtualSize());
                disk.setVirtualSize(disk.getSize());

                QemuImg qemu = new QemuImg();
                QemuImgFile srcFile;
                QemuImgFile destFile = new QemuImgFile(KVMPhysicalDisk.RBDStringBuilder(destPool.getSourceHost(),
                        destPool.getSourcePort(),
                        destPool.getAuthUserName(),
                        destPool.getAuthSecret(),
                        disk.getPath()));
                destFile.setFormat(format);

                if (srcPool.getType() != StoragePoolType.RBD) {
                    srcFile = new QemuImgFile(template.getPath(), template.getFormat());
                    qemu.convert(srcFile, destFile);
                } else {

                    /**
                     * We have to find out if the source file is in the same RBD pool and has
                     * RBD format 2 before we can do a layering/clone operation on the RBD image
                     *
                     * This will be the case when the template is already on Primary Storage and
                     * we want to copy it
                     */

                    /* Feature 1<<0 means layering in RBD format 2 */
                    int rbdFeatures = (1<<0);
                    /* Order 0 means 4MB blocks (the default) */
                    int rbdOrder = 0;

                    try {
                        if ((srcPool.getSourceHost().equals(destPool.getSourceHost())) && (srcPool.getSourceDir().equals(destPool.getSourceDir()))) {
                            /* We are on the same Ceph cluster, but we require RBD format 2 on the source image */
                            s_logger.debug("Trying to perform a RBD clone (layering) since we are operating in the same storage pool");
   
                            Rados r = new Rados(srcPool.getAuthUserName());
                            r.confSet("mon_host", srcPool.getSourceHost() + ":" + srcPool.getSourcePort());
                            r.confSet("key", srcPool.getAuthSecret());
                            r.connect();
                            s_logger.debug("Succesfully connected to Ceph cluster at " + r.confGet("mon_host"));

                            IoCTX io = r.ioCtxCreate(srcPool.getSourceDir());
                            Rbd rbd = new Rbd(io);
                            RbdImage srcImage = rbd.open(template.getName());

                            if (srcImage.isOldFormat()) {
                                /* The source image is RBD format 1, we have to do a regular copy */
                                s_logger.debug("The source image " + srcPool.getSourceDir() + "/" + template.getName()
                                               + " is RBD format 1. We have to perform a regular copy (" + template.getVirtualSize() + " bytes)");

                                rbd.create(disk.getName(), template.getVirtualSize(), rbdFeatures, rbdOrder);
                                RbdImage destImage = rbd.open(disk.getName());

                                s_logger.debug("Starting to copy " + srcImage.getName() +  " to " + destImage.getName() + " in Ceph pool " + srcPool.getSourceDir());
                                rbd.copy(srcImage, destImage);

                                s_logger.debug("Finished copying " + srcImage.getName() +  " to " + destImage.getName() + " in Ceph pool " + srcPool.getSourceDir());
                                rbd.close(destImage);
                            } else {
                                s_logger.debug("The source image " + srcPool.getSourceDir() + "/" + template.getName()
                                               + " is RBD format 2. We will perform a RBD clone using snapshot "
                                               + this.rbdTemplateSnapName);
                                /* The source image is format 2, we can do a RBD snapshot+clone (layering) */
                                rbd.clone(template.getName(), this.rbdTemplateSnapName, io, disk.getName(), rbdFeatures, rbdOrder);
                                s_logger.debug("Succesfully cloned " + template.getName() + "@" + this.rbdTemplateSnapName + " to " + disk.getName());
                            }

                            rbd.close(srcImage);
                            r.ioCtxDestroy(io);
                        } else {
                            /* The source pool or host is not the same Ceph cluster, we do a simple copy with Qemu-Img */
                            s_logger.debug("Both the source and destination are RBD, but not the same Ceph cluster. Performing a copy");

                            Rados rSrc = new Rados(srcPool.getAuthUserName());
                            rSrc.confSet("mon_host", srcPool.getSourceHost() + ":" + srcPool.getSourcePort());
                            rSrc.confSet("key", srcPool.getAuthSecret());
                            rSrc.connect();
                            s_logger.debug("Succesfully connected to source Ceph cluster at " + rSrc.confGet("mon_host"));

                            Rados rDest = new Rados(destPool.getAuthUserName());
                            rDest.confSet("mon_host", destPool.getSourceHost() + ":" + destPool.getSourcePort());
                            rDest.confSet("key", destPool.getAuthSecret());
                            rDest.connect();
                            s_logger.debug("Succesfully connected to source Ceph cluster at " + rDest.confGet("mon_host"));

                            IoCTX sIO = rSrc.ioCtxCreate(srcPool.getSourceDir());
                            Rbd sRbd = new Rbd(sIO);

                            IoCTX dIO = rDest.ioCtxCreate(destPool.getSourceDir());
                            Rbd dRbd = new Rbd(dIO);

                            s_logger.debug("Creating " + disk.getName() + " on the destination cluster " + rDest.confGet("mon_host")
                                           + " in pool " + destPool.getSourceDir());
                            dRbd.create(disk.getName(), template.getVirtualSize(), rbdFeatures, rbdOrder);

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
            }
        } catch (QemuImgException e) {
            s_logger.error("Failed to create " + disk.getPath() +
                    " due to a failed executing of qemu-img: " + e.getMessage());
        }

        if (disk == null) {
            throw new CloudRuntimeException("Failed to create " + disk.getPath() + " from template " + template.getName());
        }

        return disk;
    }

    @Override
    public KVMPhysicalDisk createTemplateFromDisk(KVMPhysicalDisk disk,
            String name, PhysicalDiskFormat format, long size,
            KVMStoragePool destPool) {
        return null;
    }

    @Override
    public List<KVMPhysicalDisk> listPhysicalDisks(String storagePoolUuid,
            KVMStoragePool pool) {
        LibvirtStoragePool libvirtPool = (LibvirtStoragePool) pool;
        StoragePool virtPool = libvirtPool.getPool();
        List<KVMPhysicalDisk> disks = new ArrayList<KVMPhysicalDisk>();
        try {
            String[] vols = virtPool.listVolumes();
            for (String volName : vols) {
                KVMPhysicalDisk disk = this.getPhysicalDisk(volName, pool);
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
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name,
            KVMStoragePool destPool) {

        /**
            With RBD you can't run qemu-img convert with an existing RBD image as destination
            qemu-img will exit with the error that the destination already exists.
            So for RBD we don't create the image, but let qemu-img do that for us.

            We then create a KVMPhysicalDisk object that we can return

            It is however very unlikely that the destPool will be RBD, since it isn't supported
            for Secondary Storage
         */

        KVMPhysicalDisk newDisk;
        if (destPool.getType() != StoragePoolType.RBD) {
            if (disk.getFormat() == PhysicalDiskFormat.TAR) {
                newDisk = destPool.createPhysicalDisk(name, PhysicalDiskFormat.DIR, disk.getVirtualSize());
            } else {
                newDisk = destPool.createPhysicalDisk(name, disk.getVirtualSize());
            }
        } else {
            newDisk = new KVMPhysicalDisk(destPool.getSourceDir() + "/" + name, name, destPool);
            newDisk.setFormat(PhysicalDiskFormat.RAW);
            newDisk.setSize(disk.getVirtualSize());
            newDisk.setVirtualSize(disk.getSize());
        }

        KVMStoragePool srcPool = disk.getPool();
        String destPath = newDisk.getPath();
        String sourcePath = disk.getPath();
        PhysicalDiskFormat sourceFormat = disk.getFormat();
        PhysicalDiskFormat destFormat = newDisk.getFormat();

        QemuImg qemu = new QemuImg();
        QemuImgFile srcFile = null;
        QemuImgFile destFile = null;

        if ((srcPool.getType() != StoragePoolType.RBD) && (destPool.getType() != StoragePoolType.RBD)) {
            if (sourceFormat == PhysicalDiskFormat.TAR) {
                Script.runSimpleBashScript("tar -x -f " + sourcePath + " -C " + destPath);
            } else if (sourceFormat == PhysicalDiskFormat.DIR) {
                Script.runSimpleBashScript("mkdir -p " + destPath);
                Script.runSimpleBashScript("chmod 755 " + destPath);
                Script.runSimpleBashScript("cp -p -r " + sourcePath + "/* " + destPath);
            } else {
                srcFile = new QemuImgFile(sourcePath, sourceFormat);
                try {
                    Map<String, String> info = qemu.info(srcFile);
                    String backingFile = info.get(new String("backing_file"));
                    if (sourceFormat.equals(destFormat) && backingFile == null) {
                        Script.runSimpleBashScript("cp -f " + sourcePath + " " + destPath);
                    } else {
                        destFile = new QemuImgFile(destPath, destFormat);
                        try {
                            qemu.convert(srcFile, destFile);
                        } catch (QemuImgException e) {
                            s_logger.error("Failed to convert " + srcFile.getFileName() + " to "
                                    + destFile.getFileName() + " the error was: " + e.getMessage());
                            newDisk = null;
                        }
                    }
                } catch (QemuImgException e) {
                    s_logger.error("Failed to fetch the information of file "
                            + srcFile.getFileName() + " the error was: " + e.getMessage());
                    newDisk = null;
                }
            }
        } else if ((srcPool.getType() != StoragePoolType.RBD) && (destPool.getType() == StoragePoolType.RBD))  {
            /**
              * Qemu doesn't support writing to RBD format 2 directly, so we have to write to a temporary RAW file first
              * which we then convert to RBD format 2.
              *
              * A HUGE performance gain can be achieved here if QCOW2 -> RBD format 2 can be done in one step
              */
            s_logger.debug("The source image is not RBD, but the destination is. We will convert into RBD format 2");
            String tmpFile = "/tmp/" + name;
            int rbdFeatures = (1<<0);
            int rbdOrder = 0;

            try {
                srcFile = new QemuImgFile(sourcePath, sourceFormat);
                destFile = new QemuImgFile(tmpFile);
                s_logger.debug("Converting " + srcFile.getFileName() +  " to " + tmpFile +  " as a temporary file for RBD conversion");
                qemu.convert(srcFile, destFile);

                // We now convert the temporary file to a RBD image with format 2
                Rados r = new Rados(destPool.getAuthUserName());
                r.confSet("mon_host", destPool.getSourceHost() + ":" + destPool.getSourcePort());
                r.confSet("key", destPool.getAuthSecret());
                r.connect();
                s_logger.debug("Succesfully connected to Ceph cluster at " + r.confGet("mon_host"));

                IoCTX io = r.ioCtxCreate(destPool.getSourceDir());
                Rbd rbd = new Rbd(io);

                s_logger.debug("Creating RBD image " + name + " in Ceph pool " + destPool.getSourceDir() + " with RBD format 2");
                rbd.create(name, disk.getVirtualSize(), rbdFeatures, rbdOrder);

                RbdImage image = rbd.open(name);

                // We now read the temporary file and write it to the RBD image
                File fh = new File(tmpFile);
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fh));

                int chunkSize = 4194304;
                long offset = 0;
                s_logger.debug("Reading temporary file " + tmpFile + " (" + fh.length() + " bytes) into RBD image " + name + " in chunks of " + chunkSize + " bytes");
                while(true) {
                    byte[] buf = new byte[chunkSize];

                    int bytes = bis.read(buf);
                    if (bytes <= 0) {
                        break;
                    }
                    image.write(buf, offset, bytes);
                    offset += bytes;
                }
                s_logger.debug("Completed writing " + tmpFile + " to RBD image " + name + ". Bytes written: " + offset);
                bis.close();
                s_logger.debug("Removing temporary file " + tmpFile);
                fh.delete();

                /* Snapshot the image and protect that snapshot so we can clone (layer) from it */
                s_logger.debug("Creating RBD snapshot " + this.rbdTemplateSnapName + " on image " + name);
                image.snapCreate(this.rbdTemplateSnapName);
                s_logger.debug("Protecting RBD snapshot " + this.rbdTemplateSnapName + " on image " + name);
                image.snapProtect(this.rbdTemplateSnapName);

                rbd.close(image);
                r.ioCtxDestroy(io);
            } catch (QemuImgException e) {
                s_logger.error("Failed to do a temp convert from " + srcFile.getFileName() + " to "
                        + destFile.getFileName() + " the error was: " + e.getMessage());
                newDisk = null;
            } catch (RadosException e) {
                s_logger.error("A Ceph RADOS operation failed (" + e.getReturnValue() + "). The error was: " + e.getMessage());
                newDisk = null;
            } catch (RbdException e) {
                s_logger.error("A Ceph RBD operation failed (" + e.getReturnValue() + "). The error was: " + e.getMessage());
                newDisk = null;
            } catch (IOException e) {
                s_logger.error("Failed reading the temporary file during the conversion to RBD: " + e.getMessage());
                newDisk = null;
            }

        } else {
            /**
                We let Qemu-Img do the work here. Although we could work with librbd and have that do the cloning
                it doesn't benefit us. It's better to keep the current code in place which works
             */
            srcFile = new QemuImgFile(KVMPhysicalDisk.RBDStringBuilder(srcPool.getSourceHost(),
                    srcPool.getSourcePort(),
                    srcPool.getAuthUserName(),
                    srcPool.getAuthSecret(),
                    sourcePath));
            srcFile.setFormat(sourceFormat);
            destFile = new QemuImgFile(KVMPhysicalDisk.RBDStringBuilder(destPool.getSourceHost(),
                    destPool.getSourcePort(),
                    destPool.getAuthUserName(),
                    destPool.getAuthSecret(),
                    destPath));
            destFile.setFormat(destFormat);

            try {
                qemu.convert(srcFile, destFile);
            } catch (QemuImgException e) {
                s_logger.error("Failed to convert " + srcFile.getFileName() + " to "
                        + destFile.getFileName() + " the error was: " + e.getMessage());
                newDisk = null;
            }
        }

        if (newDisk == null) {
            throw new CloudRuntimeException("Failed to copy " + disk.getPath() + " to " + name);
        }

        return newDisk;
    }

    @Override
    public KVMPhysicalDisk createDiskFromSnapshot(KVMPhysicalDisk snapshot,
            String snapshotName, String name, KVMStoragePool destPool) {
        return null;
    }

    @Override
    public boolean refresh(KVMStoragePool pool) {
        LibvirtStoragePool libvirtPool = (LibvirtStoragePool) pool;
        StoragePool virtPool = libvirtPool.getPool();
        try {
            virtPool.refresh(0);
        } catch (LibvirtException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteStoragePool(KVMStoragePool pool) {
        return deleteStoragePool(pool.getUuid());
    }

    public boolean deleteVbdByPath(String diskPath) {
        Connect conn;
        try {
            conn = LibvirtConnection.getConnection();
            StorageVol vol = conn.storageVolLookupByPath(diskPath);
            if(vol != null) {
                s_logger.debug("requested delete disk " + diskPath);
                vol.delete(0);
            }
        } catch (LibvirtException e) {
            s_logger.debug("Libvirt error in attempting to find and delete patch disk:" + e.toString());
            return false;
        }
        return true;
    }

}
