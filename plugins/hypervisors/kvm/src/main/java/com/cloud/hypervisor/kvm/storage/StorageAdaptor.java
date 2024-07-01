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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;

import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;

public interface StorageAdaptor {

    StoragePoolType getStoragePoolType();

    public KVMStoragePool getStoragePool(String uuid);

    // Get the storage pool from libvirt, but control if libvirt should refresh the pool (can take a long time)
    public KVMStoragePool getStoragePool(String uuid, boolean refreshInfo);

    // given disk path (per database) and pool, create new KVMPhysicalDisk, populate
    // it with info from local disk, and return it
    public KVMPhysicalDisk getPhysicalDisk(String volumeUuid, KVMStoragePool pool);

    public KVMStoragePool createStoragePool(String name, String host, int port, String path, String userInfo, StoragePoolType type, Map<String, String> details);

    public boolean deleteStoragePool(String uuid);

    public default KVMPhysicalDisk createPhysicalDisk(String name, KVMStoragePool pool,
                                                      PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, Long usableSize, byte[] passphrase) {
        return createPhysicalDisk(name, pool, format, provisioningType, size, passphrase);
    }

    public KVMPhysicalDisk createPhysicalDisk(String name, KVMStoragePool pool,
            PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, byte[] passphrase);

    // given disk path (per database) and pool, prepare disk on host
    public boolean connectPhysicalDisk(String volumePath, KVMStoragePool pool, Map<String, String> details);

    // given disk path (per database) and pool, clean up disk on host
    public boolean disconnectPhysicalDisk(String volumePath, KVMStoragePool pool);

    public boolean disconnectPhysicalDisk(Map<String, String> volumeToDisconnect);

    /**
     * Given local path to file/device (per Libvirt XML),
     * 1) Make sure to check that device is handled by your adaptor, return false if not.
     * 2) clean up device, return true
     * 3) if clean up fails, then return false
     *
     * If the method wrongly returns true, then there are chances that disconnect will not reach the right storage adapter
     *
     * @param localPath path for the file/device from the disk definition per Libvirt XML.
     * @return true if the operation is successful; false if the operation fails or the adapter fails to handle the path.
     */
    public boolean disconnectPhysicalDiskByPath(String localPath);

    public boolean deletePhysicalDisk(String uuid, KVMStoragePool pool, Storage.ImageFormat format);

    public KVMPhysicalDisk createDiskFromTemplate(KVMPhysicalDisk template,
            String name, PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size,
            KVMStoragePool destPool, int timeout, byte[] passphrase);

    public KVMPhysicalDisk createTemplateFromDisk(KVMPhysicalDisk disk, String name, PhysicalDiskFormat format, long size, KVMStoragePool destPool);

    public List<KVMPhysicalDisk> listPhysicalDisks(String storagePoolUuid, KVMStoragePool pool);

    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPools, int timeout);
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPools, int timeout, byte[] srcPassphrase, byte[] dstPassphrase, Storage.ProvisioningType provisioningType);

    public boolean refresh(KVMStoragePool pool);

    public boolean deleteStoragePool(KVMStoragePool pool);

    public boolean createFolder(String uuid, String path);

    public boolean createFolder(String uuid, String path, String localPath);

    /**
     * Creates disk using template backing.
     * Precondition: Template is on destPool
     */
    KVMPhysicalDisk createDiskFromTemplateBacking(KVMPhysicalDisk template,
                                                  String name, PhysicalDiskFormat format, long size,
                                                  KVMStoragePool destPool, int timeout, byte[] passphrase);

    /**
     * Create physical disk on Primary Storage from direct download template on the host (in temporary location)
     * @param templateFilePath
     * @param destPool
     * @param format
     * @param timeout
     */
    KVMPhysicalDisk createTemplateFromDirectDownloadFile(String templateFilePath, String destTemplatePath, KVMStoragePool destPool, Storage.ImageFormat format, int timeout);

    /**
     * Returns true if storage adaptor supports physical disk copy functionality.
     */
    default boolean supportsPhysicalDiskCopy(StoragePoolType type) {
        return StoragePoolType.PowerFlex == type;
    }

    /**
     * Prepares the storage client.
     * @param type type of the storage pool
     * @param uuid uuid of the storage pool
     * @param details any details of the storage pool that are required for client preparation
     * @return status, client details, & message in case failed
     */
    default Ternary<Boolean, Map<String, String>, String> prepareStorageClient(StoragePoolType type, String uuid, Map<String, String> details) {
        return new Ternary<>(true, new HashMap<>(), "");
    }

    /**
     * Unprepares the storage client.
     * @param type type of the storage pool
     * @param uuid uuid of the storage pool
     * @return status, & message in case failed
     */
    default Pair<Boolean, String> unprepareStorageClient(StoragePoolType type, String uuid) {
        return new Pair<>(true, "");
    }
}
