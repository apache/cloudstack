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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.UUID;

import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;

import com.cloud.hypervisor.kvm.resource.KVMHABase;
import com.cloud.hypervisor.kvm.resource.KVMHABase.PoolType;
import com.cloud.hypervisor.kvm.resource.KVMHAMonitor;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;

public class KVMStoragePoolManager {
    private static final Logger s_logger = Logger
            .getLogger(KVMStoragePoolManager.class);
    private class StoragePoolInformation {
         String name;
         String host;
         int port;
         String path;
         String userInfo;
         boolean type;
        StoragePoolType poolType;


        public  StoragePoolInformation(String name,
                                       String host,
                                       int port,
                                       String path,
                                       String userInfo,
                                       StoragePoolType poolType,
                                       boolean type) {
            this.name = name;
            this.host = host;
            this.port = port;
            this.path = path;
            this.userInfo = userInfo;
            this.type = type;
            this.poolType = poolType;
        }
    }
    private StorageAdaptor _storageAdaptor;
    private KVMHAMonitor _haMonitor;
    private final Map<String, StoragePoolInformation> _storagePools = new ConcurrentHashMap<String, StoragePoolInformation>();
    private final Map<String, StorageAdaptor> _storageMapper = new HashMap<String, StorageAdaptor>();

    private StorageAdaptor getStorageAdaptor(StoragePoolType type) {
        // type can be null: LibVirtComputingResource:3238
        if (type == null) {
            return _storageMapper.get("libvirt");
        }
        StorageAdaptor adaptor = _storageMapper.get(type.toString());
        if (adaptor == null) {
            // LibvirtStorageAdaptor is selected by default
            adaptor = _storageMapper.get("libvirt");
        }
        return adaptor;
    }

    private void addStoragePool(String uuid, StoragePoolInformation pool) {
        synchronized (_storagePools) {
            if (!_storagePools.containsKey(uuid)) {
                _storagePools.put(uuid, pool);
            }
        }
    }

    public KVMStoragePoolManager(StorageLayer storagelayer, KVMHAMonitor monitor) {
        this._storageAdaptor = new LibvirtStorageAdaptor(storagelayer);
        this._haMonitor = monitor;
        this._storageMapper.put("libvirt", new LibvirtStorageAdaptor(storagelayer));
        // add other storage adaptors here
	// this._storageMapper.put("newadaptor", new NewStorageAdaptor(storagelayer));
    }

    public KVMStoragePool getStoragePool(StoragePoolType type, String uuid) {
        StorageAdaptor adaptor = getStorageAdaptor(type);
        KVMStoragePool pool = null;
        try {
            pool = adaptor.getStoragePool(uuid);
        } catch(Exception e) {
            StoragePoolInformation info = _storagePools.get(uuid);
            if (info != null) {
                pool = createStoragePool(info.name, info.host, info.port, info.path, info.userInfo, info.poolType, info.type);
            }
        }
        return pool;
    }

    public KVMStoragePool getStoragePoolByURI(String uri) {
        URI storageUri = null;

        try {
            storageUri = new URI(uri);
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException(e.toString());
        }

        String sourcePath = null;
        String uuid = null;
        String sourceHost = "";
        StoragePoolType protocol = null;
        if (storageUri.getScheme().equalsIgnoreCase("nfs")) {
            sourcePath = storageUri.getPath();
            sourcePath = sourcePath.replace("//", "/");
            sourceHost = storageUri.getHost();
            uuid = UUID.nameUUIDFromBytes(
                new String(sourceHost + sourcePath).getBytes()).toString();
            protocol = StoragePoolType.NetworkFilesystem;
        }

        // secondary storage registers itself through here
        return createStoragePool(uuid, sourceHost, 0, sourcePath, "", protocol, false);
    }

    public KVMPhysicalDisk getPhysicalDisk(StoragePoolType type, String poolUuid, String volName) {
        int cnt = 0;
        int retries = 10;
        KVMPhysicalDisk vol = null;
        //harden get volume, try cnt times to get volume, in case volume is created on other host
        String errMsg = "";
        while (cnt < retries) {
            try {
                KVMStoragePool pool = getStoragePool(type, poolUuid);
                vol = pool.getPhysicalDisk(volName);
                if (vol != null) {
                    break;
                }

                Thread.sleep(10000);
            } catch (Exception e) {
                s_logger.debug("Failed to find volume:" + volName + " due to" + e.toString() + ", retry:" + cnt);
                errMsg = e.toString();
            }
            cnt++;
        }

        if (vol == null) {
            throw new CloudRuntimeException(errMsg);
        } else {
            return vol;
        }

    }

    public KVMStoragePool createStoragePool( String name, String host, int port,
                                             String path, String userInfo,
                                             StoragePoolType type) {
        // primary storage registers itself through here
        return createStoragePool(name, host, port, path, userInfo, type, true);
    }

    //Note: due to bug CLOUDSTACK-4459, createStoragepool can be called in parallel, so need to be synced.
    private synchronized KVMStoragePool createStoragePool( String name, String host, int port,
                                             String path, String userInfo,
                                             StoragePoolType type, boolean primaryStorage) {
        StorageAdaptor adaptor = getStorageAdaptor(type);
        KVMStoragePool pool = adaptor.createStoragePool(name,
                                host, port, path, userInfo, type);

        // LibvirtStorageAdaptor-specific statement
        if (type == StoragePoolType.NetworkFilesystem && primaryStorage) {
            KVMHABase.NfsStoragePool nfspool = new KVMHABase.NfsStoragePool(
                    pool.getUuid(), host, path, pool.getLocalPath(),
                    PoolType.PrimaryStorage);
            _haMonitor.addStoragePool(nfspool);
        }
        StoragePoolInformation info = new StoragePoolInformation(name, host, port, path, userInfo, type, primaryStorage);
        addStoragePool(pool.getUuid(), info);
        return pool;
    }

    public boolean deleteStoragePool(StoragePoolType type, String uuid) {
        StorageAdaptor adaptor = getStorageAdaptor(type);
        _haMonitor.removeStoragePool(uuid);
        adaptor.deleteStoragePool(uuid);
        _storagePools.remove(uuid);
        return true;
    }

    public KVMPhysicalDisk createDiskFromTemplate(KVMPhysicalDisk template, String name,
                                                    KVMStoragePool destPool) {
        StorageAdaptor adaptor = getStorageAdaptor(destPool.getType());

        // LibvirtStorageAdaptor-specific statement
        if (destPool.getType() == StoragePoolType.RBD) {
            return adaptor.createDiskFromTemplate(template, name,
                    PhysicalDiskFormat.RAW, template.getSize(), destPool);
        } else if (destPool.getType() == StoragePoolType.CLVM) {
            return adaptor.createDiskFromTemplate(template, name,
                                       PhysicalDiskFormat.RAW, template.getSize(),
                                       destPool);
        } else if (template.getFormat() == PhysicalDiskFormat.DIR) {
            return adaptor.createDiskFromTemplate(template, name,
                    PhysicalDiskFormat.DIR,
                    template.getSize(), destPool);
        } else {
            return adaptor.createDiskFromTemplate(template, name,
                    PhysicalDiskFormat.QCOW2,
            template.getSize(), destPool);
        }
    }

    public KVMPhysicalDisk createTemplateFromDisk(KVMPhysicalDisk disk,
            String name, PhysicalDiskFormat format, long size,
            KVMStoragePool destPool) {
        StorageAdaptor adaptor = getStorageAdaptor(destPool.getType());
        return adaptor.createTemplateFromDisk(disk, name, format,
                size, destPool);
    }

    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name,
            KVMStoragePool destPool) {
        StorageAdaptor adaptor = getStorageAdaptor(destPool.getType());
        return adaptor.copyPhysicalDisk(disk, name, destPool);
    }

    public KVMPhysicalDisk createDiskFromSnapshot(KVMPhysicalDisk snapshot,
            String snapshotName, String name, KVMStoragePool destPool) {
        StorageAdaptor adaptor = getStorageAdaptor(destPool.getType());
        return adaptor.createDiskFromSnapshot(snapshot,
                snapshotName, name, destPool);
    }

}
