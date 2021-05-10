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

public class IscsiAdmStoragePool implements KVMStoragePool {
    private String _uuid;
    private String _sourceHost;
    private int _sourcePort;
    private StoragePoolType _storagePoolType;
    private StorageAdaptor _storageAdaptor;
    private String _authUsername;
    private String _authSecret;
    private String _sourceDir;
    private String _localPath;

    public IscsiAdmStoragePool(String uuid, String host, int port, StoragePoolType storagePoolType, StorageAdaptor storageAdaptor) {
        _uuid = uuid;
        _sourceHost = host;
        _sourcePort = port;
        _storagePoolType = storagePoolType;
        _storageAdaptor = storageAdaptor;
    }

    @Override
    public String getUuid() {
        return _uuid;
    }

    @Override
    public String getSourceHost() {
        return _sourceHost;
    }

    @Override
    public int getSourcePort() {
        return _sourcePort;
    }

    @Override
    public long getCapacity() {
        return 0;
    }

    @Override
    public long getUsed() {
        return 0;
    }

    @Override
    public long getAvailable() {
        return 0;
    }

    @Override
    public StoragePoolType getType() {
        return _storagePoolType;
    }

    @Override
    public PhysicalDiskFormat getDefaultFormat() {
        return PhysicalDiskFormat.RAW;
    }

    // called from LibvirtComputingResource.copyPhysicalDisk(KVMPhysicalDisk, String, KVMStoragePool) and
    // from LibvirtComputingResource.createDiskFromTemplate(KVMPhysicalDisk, String, PhysicalDiskFormat, long, KVMStoragePool)
    // does not apply for iScsiAdmStoragePool
    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size) {
        throw new UnsupportedOperationException("Creating a physical disk is not supported.");
    }

    // called from LibvirtComputingResource.execute(CreateCommand) and
    // from KVMStorageProcessor.createVolume(CreateObjectCommand)
    // does not apply for iScsiAdmStoragePool
    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, Storage.ProvisioningType provisioningType, long size) {
        throw new UnsupportedOperationException("Creating a physical disk is not supported.");
    }

    @Override
    public boolean connectPhysicalDisk(String name, Map<String, String> details) {
        return this._storageAdaptor.connectPhysicalDisk(name, this, details);
    }

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String volumeUuid) {
        return this._storageAdaptor.getPhysicalDisk(volumeUuid, this);
    }

    @Override
    public boolean disconnectPhysicalDisk(String volumeUuid) {
        return this._storageAdaptor.disconnectPhysicalDisk(volumeUuid, this);
    }

    @Override
    public boolean deletePhysicalDisk(String volumeUuid, Storage.ImageFormat format) {
        return this._storageAdaptor.deletePhysicalDisk(volumeUuid, this, format);
    }

    // does not apply for iScsiAdmStoragePool
    @Override
    public List<KVMPhysicalDisk> listPhysicalDisks() {
        return this._storageAdaptor.listPhysicalDisks(_uuid, this);
    }

    // does not apply for iScsiAdmStoragePool
    @Override
    public boolean refresh() {
        return this._storageAdaptor.refresh(this);
    }

    @Override
    public boolean delete() {
        return this._storageAdaptor.deleteStoragePool(this);
    }

    @Override
    public boolean createFolder(String path) {
        return this._storageAdaptor.createFolder(_uuid, path);
    }

    @Override
    public boolean isExternalSnapshot() {
        return false;
    }

    @Override
    public String getAuthUserName() {
        return _authUsername;
    }

    @Override
    public String getAuthSecret() {
        return _authSecret;
    }

    @Override
    public String getSourceDir() {
        return _sourceDir;
    }

    @Override
    public String getLocalPath() {
        return _localPath;
    }

    @Override
    public boolean supportsConfigDriveIso() {
        return false;
    }
}
