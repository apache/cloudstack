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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.libvirt.StoragePool;

import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;

import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.exception.CloudRuntimeException;

public class LibvirtStoragePool implements KVMStoragePool {
    private static final Logger s_logger = Logger.getLogger(LibvirtStoragePool.class);
    protected String uuid;
    protected long capacity;
    protected long used;
    protected long available;
    protected String name;
    protected String localPath;
    protected PhysicalDiskFormat defaultFormat;
    protected StoragePoolType type;
    protected StorageAdaptor _storageAdaptor;
    protected StoragePool _pool;
    protected String authUsername;
    protected String authSecret;
    protected String sourceHost;
    protected int sourcePort;

    protected String sourceDir;

    public LibvirtStoragePool(String uuid, String name, StoragePoolType type, StorageAdaptor adaptor, StoragePool pool) {
        this.uuid = uuid;
        this.name = name;
        this.type = type;
        this._storageAdaptor = adaptor;
        this.capacity = 0;
        this.used = 0;
        this.available = 0;
        this._pool = pool;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    @Override
    public long getCapacity() {
        return this.capacity;
    }

    public void setUsed(long used) {
        this.used = used;
    }

    public void setAvailable(long available) {
        this.available = available;
    }

    @Override
    public long getUsed() {
        return this.used;
    }

    @Override
    public long getAvailable() {
        return this.available;
    }

    public StoragePoolType getStoragePoolType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    @Override
    public PhysicalDiskFormat getDefaultFormat() {
        if (getStoragePoolType() == StoragePoolType.CLVM || getStoragePoolType() == StoragePoolType.RBD || getStoragePoolType() == StoragePoolType.PowerFlex) {
            return PhysicalDiskFormat.RAW;
        } else {
            return PhysicalDiskFormat.QCOW2;
        }
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name,
            PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size) {
        return this._storageAdaptor
                .createPhysicalDisk(name, this, format, provisioningType, size);
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, Storage.ProvisioningType provisioningType, long size) {
        return this._storageAdaptor.createPhysicalDisk(name, this,
                this.getDefaultFormat(), provisioningType, size);
    }

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String volumeUid) {
        KVMPhysicalDisk disk = null;
        String volumeUuid = volumeUid;
        if ( volumeUid.contains("/") ) {
            String[] tokens = volumeUid.split("/");
            volumeUuid = tokens[tokens.length -1];
        }
        try {
            disk = this._storageAdaptor.getPhysicalDisk(volumeUuid, this);
        } catch (CloudRuntimeException e) {
            if ((this.getStoragePoolType() != StoragePoolType.NetworkFilesystem) && (this.getStoragePoolType() != StoragePoolType.Filesystem)) {
                throw e;
            }
        }

        if (disk != null) {
            return disk;
        }
        s_logger.debug("find volume bypass libvirt volumeUid " + volumeUid);
        //For network file system or file system, try to use java file to find the volume, instead of through libvirt. BUG:CLOUDSTACK-4459
        String localPoolPath = this.getLocalPath();
        File f = new File(localPoolPath + File.separator + volumeUuid);
        if (!f.exists()) {
            s_logger.debug("volume: " + volumeUuid + " not exist on storage pool");
            throw new CloudRuntimeException("Can't find volume:" + volumeUuid);
        }
        disk = new KVMPhysicalDisk(f.getPath(), volumeUuid, this);
        disk.setFormat(PhysicalDiskFormat.QCOW2);
        disk.setSize(f.length());
        disk.setVirtualSize(f.length());
        s_logger.debug("find volume bypass libvirt disk " + disk.toString());
        return disk;
    }

    @Override
    public boolean connectPhysicalDisk(String name, Map<String, String> details) {
        return true;
    }

    @Override
    public boolean disconnectPhysicalDisk(String uuid) {
        return true;
    }

    @Override
    public boolean deletePhysicalDisk(String uuid, Storage.ImageFormat format) {
        return this._storageAdaptor.deletePhysicalDisk(uuid, this, format);
    }

    @Override
    public List<KVMPhysicalDisk> listPhysicalDisks() {
        return this._storageAdaptor.listPhysicalDisks(this.uuid, this);
    }

    @Override
    public boolean refresh() {
        return this._storageAdaptor.refresh(this);
    }

    @Override
    public boolean isExternalSnapshot() {
        if (this.type == StoragePoolType.CLVM || type == StoragePoolType.RBD) {
            return true;
        }
        return false;
    }

    @Override
    public String getLocalPath() {
        return this.localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    @Override
    public String getAuthUserName() {
        return this.authUsername;
    }

    public void setAuthUsername(String authUsername) {
        this.authUsername = authUsername;
    }

    @Override
    public String getAuthSecret() {
        return this.authSecret;
    }

    public void setAuthSecret(String authSecret) {
        this.authSecret = authSecret;
    }

    @Override
    public String getSourceHost() {
        return this.sourceHost;
    }

    public void setSourceHost(String host) {
        this.sourceHost = host;
    }

    @Override
    public int getSourcePort() {
        return this.sourcePort;
    }

    public void setSourcePort(int port) {
        this.sourcePort = port;
    }

    @Override
    public String getSourceDir() {
        return this.sourceDir;
    }

    public void setSourceDir(String dir) {
        this.sourceDir = dir;
    }

    @Override
    public StoragePoolType getType() {
        return this.type;
    }

    public StoragePool getPool() {
        return this._pool;
    }

    public void setPool(StoragePool pool) {
        this._pool = pool;
    }


    @Override
    public boolean delete() {
        try {
            return this._storageAdaptor.deleteStoragePool(this);
        } catch (Exception e) {
            s_logger.debug("Failed to delete storage pool", e);
        }
        return false;
    }

    @Override
    public boolean createFolder(String path) {
        return this._storageAdaptor.createFolder(this.uuid, path);
    }

    @Override
    public boolean supportsConfigDriveIso() {
        if (this.type == StoragePoolType.NetworkFilesystem) {
            return true;
        }
        return false;
    }
}
