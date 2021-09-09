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

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;

import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;

public interface StorageAdaptor {

    public KVMStoragePool getStoragePool(String uuid);

    // Get the storage pool from libvirt, but control if libvirt should refresh the pool (can take a long time)
    public KVMStoragePool getStoragePool(String uuid, boolean refreshInfo);

    // given disk path (per database) and pool, create new KVMPhysicalDisk, populate
    // it with info from local disk, and return it
    public KVMPhysicalDisk getPhysicalDisk(String volumeUuid, KVMStoragePool pool);

    public KVMStoragePool createStoragePool(String name, String host, int port, String path, String userInfo, StoragePoolType type);

    public boolean deleteStoragePool(String uuid);

    public KVMPhysicalDisk createPhysicalDisk(String name, KVMStoragePool pool,
            PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size);

    // given disk path (per database) and pool, prepare disk on host
    public boolean connectPhysicalDisk(String volumePath, KVMStoragePool pool, Map<String, String> details);

    // given disk path (per database) and pool, clean up disk on host
    public boolean disconnectPhysicalDisk(String volumePath, KVMStoragePool pool);

    public boolean disconnectPhysicalDisk(Map<String, String> volumeToDisconnect);

    // given local path to file/device (per Libvirt XML), 1) check that device is
    // handled by your adaptor, return false if not. 2) clean up device, return true
    public boolean disconnectPhysicalDiskByPath(String localPath);

    public boolean deletePhysicalDisk(String uuid, KVMStoragePool pool, Storage.ImageFormat format);

    public KVMPhysicalDisk createDiskFromTemplate(KVMPhysicalDisk template,
            String name, PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size,
            KVMStoragePool destPool, int timeout);

    public KVMPhysicalDisk createTemplateFromDisk(KVMPhysicalDisk disk, String name, PhysicalDiskFormat format, long size, KVMStoragePool destPool);

    public List<KVMPhysicalDisk> listPhysicalDisks(String storagePoolUuid, KVMStoragePool pool);

    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPools, int timeout);

    public KVMPhysicalDisk createDiskFromSnapshot(KVMPhysicalDisk snapshot, String snapshotName, String name, KVMStoragePool destPool, int timeout);

    public boolean refresh(KVMStoragePool pool);

    public boolean deleteStoragePool(KVMStoragePool pool);

    public boolean createFolder(String uuid, String path);

    /**
     * Creates disk using template backing.
     * Precondition: Template is on destPool
     */
    KVMPhysicalDisk createDiskFromTemplateBacking(KVMPhysicalDisk template,
                                                  String name, PhysicalDiskFormat format, long size,
                                                  KVMStoragePool destPool, int timeout);

    /**
     * Create physical disk on Primary Storage from direct download template on the host (in temporary location)
     * @param templateFilePath
     * @param destPool
     * @param format
     * @param timeout
     */
    KVMPhysicalDisk createTemplateFromDirectDownloadFile(String templateFilePath, String destTemplatePath, KVMStoragePool destPool, Storage.ImageFormat format, int timeout);
}
