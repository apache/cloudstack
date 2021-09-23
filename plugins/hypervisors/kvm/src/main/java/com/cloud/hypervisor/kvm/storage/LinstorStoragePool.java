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

import com.cloud.storage.Storage;
import org.apache.cloudstack.utils.qemu.QemuImg;

public class LinstorStoragePool implements KVMStoragePool {
    private final String _uuid;
    private final String _sourceHost;
    private final int _sourcePort;
    private final Storage.StoragePoolType _storagePoolType;
    private final StorageAdaptor _storageAdaptor;
    private final String _resourceGroup;

    public LinstorStoragePool(String uuid, String host, int port, String resourceGroup,
                              Storage.StoragePoolType storagePoolType, StorageAdaptor storageAdaptor) {
        _uuid = uuid;
        _sourceHost = host;
        _sourcePort = port;
        _storagePoolType = storagePoolType;
        _storageAdaptor = storageAdaptor;
        _resourceGroup = resourceGroup;
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, QemuImg.PhysicalDiskFormat format,
                                              Storage.ProvisioningType provisioningType, long size)
    {
        return _storageAdaptor.createPhysicalDisk(name, this, format, provisioningType, size);
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String volumeUuid, Storage.ProvisioningType provisioningType, long size)
    {
        return _storageAdaptor.createPhysicalDisk(volumeUuid,this, getDefaultFormat(), provisioningType, size);
    }

    @Override
    public boolean connectPhysicalDisk(String volumeUuid, Map<String, String> details)
    {
        return _storageAdaptor.connectPhysicalDisk(volumeUuid, this, details);
    }

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String volumeUuid)
    {
        return _storageAdaptor.getPhysicalDisk(volumeUuid, this);
    }

    @Override
    public boolean disconnectPhysicalDisk(String volumeUuid)
    {
        return _storageAdaptor.disconnectPhysicalDisk(volumeUuid, this);
    }

    @Override
    public boolean deletePhysicalDisk(String volumeUuid, Storage.ImageFormat format)
    {
        return _storageAdaptor.deletePhysicalDisk(volumeUuid, this, format);
    }

    @Override
    public List<KVMPhysicalDisk> listPhysicalDisks()
    {
        return _storageAdaptor.listPhysicalDisks(_uuid, this);
    }

    @Override
    public String getUuid()
    {
        return _uuid;
    }

    @Override
    public long getCapacity()
    {
        return ((LinstorStorageAdaptor)_storageAdaptor).getCapacity(this);
    }

    @Override
    public long getUsed()
    {
        return ((LinstorStorageAdaptor)_storageAdaptor).getUsed(this);
    }

    @Override
    public long getAvailable()
    {
        return ((LinstorStorageAdaptor)_storageAdaptor).getAvailable(this);
    }

    @Override
    public boolean refresh()
    {
        return _storageAdaptor.refresh(this);
    }

    @Override
    public boolean isExternalSnapshot()
    {
        return true;
    }

    @Override
    public String getLocalPath()
    {
        return null;
    }

    @Override
    public String getSourceHost()
    {
        return _sourceHost;
    }

    @Override
    public String getSourceDir()
    {
        return null;
    }

    @Override
    public int getSourcePort()
    {
        return _sourcePort;
    }

    @Override
    public String getAuthUserName()
    {
        return null;
    }

    @Override
    public String getAuthSecret()
    {
        return null;
    }

    @Override
    public Storage.StoragePoolType getType()
    {
        return _storagePoolType;
    }

    @Override
    public boolean delete()
    {
        return _storageAdaptor.deleteStoragePool(this);
    }

    @Override
    public QemuImg.PhysicalDiskFormat getDefaultFormat()
    {
        return QemuImg.PhysicalDiskFormat.RAW;
    }

    @Override
    public boolean createFolder(String path)
    {
        return _storageAdaptor.createFolder(_uuid, path);
    }

    @Override
    public boolean supportsConfigDriveIso()
    {
        return false;
    }

    public String getResourceGroup() {
        return _resourceGroup;
    }
}
