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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.log4j.Logger;
import org.reflections.Reflections;

import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.kvm.resource.KVMHABase;
import com.cloud.hypervisor.kvm.resource.KVMHABase.PoolType;
import com.cloud.hypervisor.kvm.resource.KVMHAMonitor;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.Volume;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;

public class KVMStoragePoolManager {
    private static final Logger s_logger = Logger.getLogger(KVMStoragePoolManager.class);

    private class StoragePoolInformation {
        String name;
        String host;
        int port;
        String path;
        String userInfo;
        boolean type;
        StoragePoolType poolType;

        public StoragePoolInformation(String name, String host, int port, String path, String userInfo, StoragePoolType poolType, boolean type) {
            this.name = name;
            this.host = host;
            this.port = port;
            this.path = path;
            this.userInfo = userInfo;
            this.type = type;
            this.poolType = poolType;
        }
    }

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
        this._haMonitor = monitor;
        this._storageMapper.put("libvirt", new LibvirtStorageAdaptor(storagelayer));
        // add other storage adaptors here
        // this._storageMapper.put("newadaptor", new NewStorageAdaptor(storagelayer));
        this._storageMapper.put(StoragePoolType.ManagedNFS.toString(), new ManagedNfsStorageAdaptor(storagelayer));
        this._storageMapper.put(StoragePoolType.PowerFlex.toString(), new ScaleIOStorageAdaptor(storagelayer));

        // add any adaptors that wish to register themselves via annotation
        Reflections reflections = new Reflections("com.cloud.hypervisor.kvm.storage");
        Set<Class<? extends StorageAdaptor>> storageAdaptors = reflections.getSubTypesOf(StorageAdaptor.class);
        for (Class<? extends StorageAdaptor> storageAdaptor : storageAdaptors) {
            StorageAdaptorInfo info = storageAdaptor.getAnnotation(StorageAdaptorInfo.class);
            if (info != null && info.storagePoolType() != null) {
                if (this._storageMapper.containsKey(info.storagePoolType().toString())) {
                    s_logger.error("Duplicate StorageAdaptor type " + info.storagePoolType().toString() + ", not loading " + storageAdaptor.getName());
                } else {
                    try {
                        this._storageMapper.put(info.storagePoolType().toString(), storageAdaptor.newInstance());
                    } catch (Exception ex) {
                       throw new CloudRuntimeException(ex.toString());
                    }
                }
            }
        }

        for (Map.Entry<String, StorageAdaptor> adaptors : this._storageMapper.entrySet()) {
            s_logger.debug("Registered a StorageAdaptor for " + adaptors.getKey());
        }
    }

    public boolean connectPhysicalDisk(StoragePoolType type, String poolUuid, String volPath, Map<String, String> details) {
        StorageAdaptor adaptor = getStorageAdaptor(type);
        KVMStoragePool pool = adaptor.getStoragePool(poolUuid);

        return adaptor.connectPhysicalDisk(volPath, pool, details);
    }

    public boolean connectPhysicalDisksViaVmSpec(VirtualMachineTO vmSpec) {
        boolean result = false;

        final String vmName = vmSpec.getName();

        List<DiskTO> disks = Arrays.asList(vmSpec.getDisks());

        for (DiskTO disk : disks) {
            if (disk.getType() == Volume.Type.ISO) {
                result = true;
                continue;
            }

            VolumeObjectTO vol = (VolumeObjectTO)disk.getData();
            PrimaryDataStoreTO store = (PrimaryDataStoreTO)vol.getDataStore();
            if (!store.isManaged() && VirtualMachine.State.Migrating.equals(vmSpec.getState())) {
                result = true;
                continue;
            }

            KVMStoragePool pool = getStoragePool(store.getPoolType(), store.getUuid());
            StorageAdaptor adaptor = getStorageAdaptor(pool.getType());

            result = adaptor.connectPhysicalDisk(vol.getPath(), pool, disk.getDetails());

            if (!result) {
                s_logger.error("Failed to connect disks via vm spec for vm: " + vmName + " volume:" + vol.toString());
                return result;
            }
        }

        return result;
    }

    public boolean disconnectPhysicalDisk(Map<String, String> volumeToDisconnect) {
        for (Map.Entry<String, StorageAdaptor> set : _storageMapper.entrySet()) {
            StorageAdaptor adaptor = set.getValue();

            if (adaptor.disconnectPhysicalDisk(volumeToDisconnect)) {
                return true;
            }
        }

        return false;
    }

    public boolean disconnectPhysicalDiskByPath(String path) {
        for (Map.Entry<String, StorageAdaptor> set : _storageMapper.entrySet()) {
            StorageAdaptor adaptor = set.getValue();

            if (adaptor.disconnectPhysicalDiskByPath(path)) {
                return true;
            }
        }

        return false;
    }

    public boolean disconnectPhysicalDisksViaVmSpec(VirtualMachineTO vmSpec) {
        if (vmSpec == null) {
            /* CloudStack often tries to stop VMs that shouldn't be running, to ensure a known state,
               for example if we lose communication with the agent and the VM is brought up elsewhere.
               We may not know about these yet. This might mean that we can't use the vmspec map, because
               when we restart the agent we lose all of the info about running VMs. */

            s_logger.debug("disconnectPhysicalDiskViaVmSpec: Attempted to stop a VM that is not yet in our hash map");

            return true;
        }

        boolean result = true;

        final String vmName = vmSpec.getName();

        List<DiskTO> disks = Arrays.asList(vmSpec.getDisks());

        for (DiskTO disk : disks) {
            if (disk.getType() != Volume.Type.ISO) {
                s_logger.debug("Disconnecting disk " + disk.getPath());

                VolumeObjectTO vol = (VolumeObjectTO)disk.getData();
                PrimaryDataStoreTO store = (PrimaryDataStoreTO)vol.getDataStore();

                KVMStoragePool pool = getStoragePool(store.getPoolType(), store.getUuid());

                if (pool == null) {
                    s_logger.error("Pool " + store.getUuid() + " of type " + store.getPoolType() + " was not found, skipping disconnect logic");
                    continue;
                }

                StorageAdaptor adaptor = getStorageAdaptor(pool.getType());

                // if a disk fails to disconnect, still try to disconnect remaining

                boolean subResult = adaptor.disconnectPhysicalDisk(vol.getPath(), pool);

                if (!subResult) {
                    s_logger.error("Failed to disconnect disks via vm spec for vm: " + vmName + " volume:" + vol.toString());

                    result = false;
                }
            }
        }

        return result;
    }

    public KVMStoragePool getStoragePool(StoragePoolType type, String uuid) {
        return this.getStoragePool(type, uuid, false);
    }

    public KVMStoragePool getStoragePool(StoragePoolType type, String uuid, boolean refreshInfo) {

        StorageAdaptor adaptor = getStorageAdaptor(type);
        KVMStoragePool pool = null;
        try {
            pool = adaptor.getStoragePool(uuid, refreshInfo);
        } catch (Exception e) {
            StoragePoolInformation info = _storagePools.get(uuid);
            if (info != null) {
                pool = createStoragePool(info.name, info.host, info.port, info.path, info.userInfo, info.poolType, info.type);
            } else {
                throw new CloudRuntimeException("Could not fetch storage pool " + uuid + " from libvirt due to " + e.getMessage());
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
        if (storageUri.getScheme().equalsIgnoreCase("nfs") || storageUri.getScheme().equalsIgnoreCase("NetworkFilesystem")) {
            sourcePath = storageUri.getPath();
            sourcePath = sourcePath.replace("//", "/");
            sourceHost = storageUri.getHost();
            uuid = UUID.nameUUIDFromBytes(new String(sourceHost + sourcePath).getBytes()).toString();
            protocol = StoragePoolType.NetworkFilesystem;
        }

        // secondary storage registers itself through here
        return createStoragePool(uuid, sourceHost, 0, sourcePath, "", protocol, false);
    }

    public KVMPhysicalDisk getPhysicalDisk(StoragePoolType type, String poolUuid, String volName) {
        int cnt = 0;
        int retries = 100;
        KVMPhysicalDisk vol = null;
        //harden get volume, try cnt times to get volume, in case volume is created on other host
        //Poll more frequently and return immediately once disk is found
        String errMsg = "";
        while (cnt < retries) {
            try {
                KVMStoragePool pool = getStoragePool(type, poolUuid);
                vol = pool.getPhysicalDisk(volName);
                if (vol != null) {
                    return vol;
                }
            } catch (Exception e) {
                s_logger.debug("Failed to find volume:" + volName + " due to " + e.toString() + ", retry:" + cnt);
                errMsg = e.toString();
            }

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                s_logger.debug("[ignored] interupted while trying to get storage pool.");
            }
            cnt++;
        }

        KVMStoragePool pool = getStoragePool(type, poolUuid);
        vol = pool.getPhysicalDisk(volName);
        if (vol == null) {
            throw new CloudRuntimeException(errMsg);
        } else {
            return vol;
        }
    }

    public KVMStoragePool createStoragePool(String name, String host, int port, String path, String userInfo, StoragePoolType type) {
        // primary storage registers itself through here
        return createStoragePool(name, host, port, path, userInfo, type, true);
    }

    //Note: due to bug CLOUDSTACK-4459, createStoragepool can be called in parallel, so need to be synced.
    private synchronized KVMStoragePool createStoragePool(String name, String host, int port, String path, String userInfo, StoragePoolType type, boolean primaryStorage) {
        StorageAdaptor adaptor = getStorageAdaptor(type);
        KVMStoragePool pool = adaptor.createStoragePool(name, host, port, path, userInfo, type);

        // LibvirtStorageAdaptor-specific statement
        if (type == StoragePoolType.NetworkFilesystem && primaryStorage) {
            KVMHABase.NfsStoragePool nfspool = new KVMHABase.NfsStoragePool(pool.getUuid(), host, path, pool.getLocalPath(), PoolType.PrimaryStorage);
            _haMonitor.addStoragePool(nfspool);
        }
        StoragePoolInformation info = new StoragePoolInformation(name, host, port, path, userInfo, type, primaryStorage);
        addStoragePool(pool.getUuid(), info);
        return pool;
    }

    public boolean disconnectPhysicalDisk(StoragePoolType type, String poolUuid, String volPath) {
        StorageAdaptor adaptor = getStorageAdaptor(type);
        KVMStoragePool pool = adaptor.getStoragePool(poolUuid);

        return adaptor.disconnectPhysicalDisk(volPath, pool);
    }

    public boolean deleteStoragePool(StoragePoolType type, String uuid) {
        StorageAdaptor adaptor = getStorageAdaptor(type);
        _haMonitor.removeStoragePool(uuid);
        adaptor.deleteStoragePool(uuid);
        synchronized (_storagePools) {
            _storagePools.remove(uuid);
        }
        return true;
    }

    public KVMPhysicalDisk createDiskFromTemplate(KVMPhysicalDisk template, String name, Storage.ProvisioningType provisioningType,
                                                    KVMStoragePool destPool, int timeout) {
        return createDiskFromTemplate(template, name, provisioningType, destPool, template.getSize(), timeout);
    }

    public KVMPhysicalDisk createDiskFromTemplate(KVMPhysicalDisk template, String name, Storage.ProvisioningType provisioningType,
                                                    KVMStoragePool destPool, long size, int timeout) {
        StorageAdaptor adaptor = getStorageAdaptor(destPool.getType());

        // LibvirtStorageAdaptor-specific statement
        if (destPool.getType() == StoragePoolType.RBD) {
            return adaptor.createDiskFromTemplate(template, name,
                    PhysicalDiskFormat.RAW, provisioningType,
                    size, destPool, timeout);
        } else if (destPool.getType() == StoragePoolType.CLVM) {
            return adaptor.createDiskFromTemplate(template, name,
                    PhysicalDiskFormat.RAW, provisioningType,
                    size, destPool, timeout);
        } else if (template.getFormat() == PhysicalDiskFormat.DIR) {
            return adaptor.createDiskFromTemplate(template, name,
                    PhysicalDiskFormat.DIR, provisioningType,
                    size, destPool, timeout);
        } else if (destPool.getType() == StoragePoolType.PowerFlex) {
            return adaptor.createDiskFromTemplate(template, name,
                    PhysicalDiskFormat.RAW, provisioningType,
                    size, destPool, timeout);
        } else {
            return adaptor.createDiskFromTemplate(template, name,
                    PhysicalDiskFormat.QCOW2, provisioningType,
                    size, destPool, timeout);
        }
    }

    public KVMPhysicalDisk createTemplateFromDisk(KVMPhysicalDisk disk, String name, PhysicalDiskFormat format, long size, KVMStoragePool destPool) {
        StorageAdaptor adaptor = getStorageAdaptor(destPool.getType());
        return adaptor.createTemplateFromDisk(disk, name, format, size, destPool);
    }

    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPool, int timeout) {
        StorageAdaptor adaptor = getStorageAdaptor(destPool.getType());
        return adaptor.copyPhysicalDisk(disk, name, destPool, timeout);
    }

    public KVMPhysicalDisk createDiskFromSnapshot(KVMPhysicalDisk snapshot, String snapshotName, String name, KVMStoragePool destPool, int timeout) {
        StorageAdaptor adaptor = getStorageAdaptor(destPool.getType());
        return adaptor.createDiskFromSnapshot(snapshot, snapshotName, name, destPool, timeout);
    }

    public KVMPhysicalDisk createDiskWithTemplateBacking(KVMPhysicalDisk template, String name, PhysicalDiskFormat format, long size,
                                                         KVMStoragePool destPool, int timeout) {
        StorageAdaptor adaptor = getStorageAdaptor(destPool.getType());
        return adaptor.createDiskFromTemplateBacking(template, name, format, size, destPool, timeout);
    }

    public KVMPhysicalDisk createPhysicalDiskFromDirectDownloadTemplate(String templateFilePath, String destTemplatePath, KVMStoragePool destPool, Storage.ImageFormat format, int timeout) {
        StorageAdaptor adaptor = getStorageAdaptor(destPool.getType());
        return adaptor.createTemplateFromDirectDownloadFile(templateFilePath, destTemplatePath, destPool, format, timeout);
    }

}
