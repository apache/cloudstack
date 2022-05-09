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

public class StorPoolStoragePool implements KVMStoragePool {
    private String _uuid;
    private String _sourceHost;
    private int _sourcePort;
    private StoragePoolType _storagePoolType;
    private StorageAdaptor _storageAdaptor;
    private String _authUsername;
    private String _authSecret;
    private String _sourceDir;
    private String _localPath;

    public StorPoolStoragePool(String uuid, String host, int port, StoragePoolType storagePoolType, StorageAdaptor storageAdaptor) {
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
        return 100L*(1024L*1024L*1024L*1024L*1024L);
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
    public PhysicalDiskFormat getDefaultFormat() {
        return PhysicalDiskFormat.RAW;
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size) {
        return _storageAdaptor.createPhysicalDisk(name, this, format, provisioningType, size);
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, Storage.ProvisioningType provisioningType, long size) {
        return _storageAdaptor.createPhysicalDisk(name, this, null, provisioningType, size);
    }

    @Override
    public boolean connectPhysicalDisk(String name, Map<String, String> details) {
        return _storageAdaptor.connectPhysicalDisk(name, this, details);
    }

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String volumeUuid) {
        return _storageAdaptor.getPhysicalDisk(volumeUuid, this);
    }

    @Override
    public boolean disconnectPhysicalDisk(String volumeUuid) {
        return _storageAdaptor.disconnectPhysicalDisk(volumeUuid, this);
    }

    @Override
    public boolean deletePhysicalDisk(String volumeUuid, Storage.ImageFormat format) {
        return _storageAdaptor.deletePhysicalDisk(volumeUuid, this, format);
    }

    @Override
    public List<KVMPhysicalDisk> listPhysicalDisks() {
        return _storageAdaptor.listPhysicalDisks(_uuid, this);
    }

    @Override
    public boolean refresh() {
        return _storageAdaptor.refresh(this);
    }

    @Override
    public boolean delete() {
        return _storageAdaptor.deleteStoragePool(this);
    }

    @Override
    public boolean createFolder(String path) {
        return _storageAdaptor.createFolder(_uuid, path);
    }

    @Override
    public boolean isExternalSnapshot() {
        return false;
    }

    public boolean supportsConfigDriveIso() {
        return false;
    }

    @Override
    public Map<String, String> getDetails() {
        return null;
    }
}
