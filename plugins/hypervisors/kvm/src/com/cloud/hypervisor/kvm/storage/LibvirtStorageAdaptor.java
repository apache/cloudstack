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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.apache.commons.codec.binary.Base64;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.Secret;
import org.libvirt.StoragePool;
import org.libvirt.StoragePoolInfo;
import org.libvirt.StorageVol;
import org.libvirt.StoragePoolInfo.StoragePoolState;

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
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk.PhysicalDiskFormat;
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
            String host, String path) {
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
                    s_logger.debug("Failed to define nfs storage pool with: "
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
                disk.setFormat(pool.getDefaultFormat());
            } else if (pool.getType() == StoragePoolType.RBD) {
                disk.setFormat(KVMPhysicalDisk.PhysicalDiskFormat.RAW);
            } else if (voldef.getFormat() == LibvirtStorageVolumeDef.volFormat.QCOW2) {
                disk.setFormat(KVMPhysicalDisk.PhysicalDiskFormat.QCOW2);
            } else if (voldef.getFormat() == LibvirtStorageVolumeDef.volFormat.RAW) {
                disk.setFormat(KVMPhysicalDisk.PhysicalDiskFormat.RAW);
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

                    if (pdef.getTargetPath().equals(path)) {
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
                sp = createNfsStoragePool(conn, name, host, path);
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

        if (destPool.getType() != StoragePoolType.RBD) {
            disk = destPool.createPhysicalDisk(newUuid, format, template.getVirtualSize());

            if (format == PhysicalDiskFormat.QCOW2) {
                Script.runSimpleBashScript("qemu-img create -f "
                        + template.getFormat() + " -b  " + template.getPath() + " "
                        + disk.getPath());
            } else if (format == PhysicalDiskFormat.RAW) {
                Script.runSimpleBashScript("qemu-img convert -f "
                                        + template.getFormat() + " -O raw " + template.getPath()
                                        + " " + disk.getPath());
            }
        } else {
            disk = new KVMPhysicalDisk(destPool.getSourceDir() + "/" + newUuid, newUuid, destPool);
            disk.setFormat(format);
            disk.setSize(template.getVirtualSize());
            disk.setVirtualSize(disk.getSize());

            if (srcPool.getType() != StoragePoolType.RBD) {
                Script.runSimpleBashScript("qemu-img convert"
                        + " -f " + template.getFormat()
                        + " -O " + format
                        + " " + template.getPath()
                        + " " + KVMPhysicalDisk.RBDStringBuilder(destPool.getSourceHost(),
                                                destPool.getSourcePort(),
                                                destPool.getAuthUserName(),
                                                destPool.getAuthSecret(),
                                                disk.getPath()));
            } else {
                template.setFormat(PhysicalDiskFormat.RAW);
                Script.runSimpleBashScript("qemu-img convert"
                        + " -f " + template.getFormat()
                        + " -O " + format
                        + " " + KVMPhysicalDisk.RBDStringBuilder(srcPool.getSourceHost(),
                                                srcPool.getSourcePort(),
                                                srcPool.getAuthUserName(),
                                                srcPool.getAuthSecret(),
                                                template.getPath())
                        + " " + KVMPhysicalDisk.RBDStringBuilder(destPool.getSourceHost(),
                                                destPool.getSourcePort(),
                                                destPool.getAuthUserName(),
                                                destPool.getAuthSecret(),
                                                disk.getPath()));
            }
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

    @Override
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name,
            KVMStoragePool destPool) {

        /*
            With RBD you can't run qemu-img convert with an existing RBD image as destination
            qemu-img will exit with the error that the destination already exists.
            So for RBD we don't create the image, but let qemu-img do that for us.

            We then create a KVMPhysicalDisk object that we can return
        */

        KVMPhysicalDisk newDisk;
        if (destPool.getType() != StoragePoolType.RBD) {
            newDisk = destPool.createPhysicalDisk(name, disk.getVirtualSize());
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

        if ((srcPool.getType() != StoragePoolType.RBD) && (destPool.getType() != StoragePoolType.RBD)) {
            if (sourceFormat.equals(destFormat) && 
                Script.runSimpleBashScript("qemu-img info " + sourcePath + "|grep backing") == null) {
                Script.runSimpleBashScript("cp -f " + sourcePath + " " + destPath);

            } else {
                Script.runSimpleBashScript("qemu-img convert -f " + sourceFormat
                    + " -O " + destFormat
                    + " " + sourcePath
                    + " " + destPath);
            }
        } else if ((srcPool.getType() != StoragePoolType.RBD) && (destPool.getType() == StoragePoolType.RBD))  {
            Script.runSimpleBashScript("qemu-img convert -f " + sourceFormat
                    + " -O " + destFormat
                    + " " + sourcePath
                    + " " + KVMPhysicalDisk.RBDStringBuilder(destPool.getSourceHost(),
                                                destPool.getSourcePort(),
                                                destPool.getAuthUserName(),
                                                destPool.getAuthSecret(),
                                                destPath));
        } else {
            Script.runSimpleBashScript("qemu-img convert -f " + sourceFormat
                    + " -O " + destFormat
                    + " " + KVMPhysicalDisk.RBDStringBuilder(srcPool.getSourceHost(),
                                                srcPool.getSourcePort(),
                                                srcPool.getAuthUserName(),
                                                srcPool.getAuthSecret(),
                                                sourcePath)
                    + " " + KVMPhysicalDisk.RBDStringBuilder(destPool.getSourceHost(),
                                                destPool.getSourcePort(),
                                                destPool.getAuthUserName(),
                                                destPool.getAuthSecret(),
                                                destPath));
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
        LibvirtStoragePool libvirtPool = (LibvirtStoragePool) pool;
        StoragePool virtPool = libvirtPool.getPool();
        try {
            virtPool.destroy();
            virtPool.undefine();
            virtPool.free();
        } catch (LibvirtException e) {
            return false;
        }

        return true;
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
