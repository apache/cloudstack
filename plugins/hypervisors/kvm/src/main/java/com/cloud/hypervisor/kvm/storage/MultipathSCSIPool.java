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

import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.joda.time.Duration;

import com.cloud.agent.api.to.HostTO;
import com.cloud.hypervisor.kvm.resource.KVMHABase.HAStoragePool;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ProvisioningType;

public class MultipathSCSIPool implements KVMStoragePool {
    private String uuid;
    private String sourceHost;
    private int sourcePort;
    private String sourceDir;
    private Storage.StoragePoolType storagePoolType;
    private StorageAdaptor storageAdaptor;
    private long capacity;
    private long used;
    private long available;
    private Map<String, String> details;

    public MultipathSCSIPool(String uuid, String host, int port, String path, Storage.StoragePoolType poolType, Map<String, String> poolDetails, StorageAdaptor adaptor) {
        this.uuid = uuid;
        sourceHost = host;
        sourcePort = port;
        sourceDir = path;
        storagePoolType = poolType;
        storageAdaptor = adaptor;
        capacity = 0;
        used = 0;
        available = 0;
        details = poolDetails;
    }

    public MultipathSCSIPool(String uuid, StorageAdaptor adapter) {
        this.uuid = uuid;
        sourceHost = null;
        sourcePort = -1;
        sourceDir = null;
        storagePoolType = Storage.StoragePoolType.FiberChannel;
        details = new HashMap<String,String>();
        this.storageAdaptor = adapter;
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String arg0, ProvisioningType arg1, long arg2, byte[] arg3) {
        return null;
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String arg0, PhysicalDiskFormat arg1, ProvisioningType arg2, long arg3,
            byte[] arg4) {
        return null;
    }

    @Override
    public boolean connectPhysicalDisk(String volumeUuid, Map<String, String> details) {
        return storageAdaptor.connectPhysicalDisk(volumeUuid, this, details);
    }

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String volumeId) {
        return storageAdaptor.getPhysicalDisk(volumeId, this);
    }

    @Override
    public boolean disconnectPhysicalDisk(String volumeUuid) {
        return storageAdaptor.disconnectPhysicalDisk(volumeUuid, this);
    }

    @Override
    public boolean deletePhysicalDisk(String volumeUuid, Storage.ImageFormat format) {
        return true;
    }

    @Override
    public List<KVMPhysicalDisk> listPhysicalDisks() {
        return null;
    }

    @Override
    public String getUuid() {
        return uuid;
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

    @Override
    public long getUsed() {
        return this.used;
    }

    public void setAvailable(long available) {
        this.available = available;
    }

    @Override
    public long getAvailable() {
        return this.available;
    }

    @Override
    public boolean refresh() {
        return false;
    }

    @Override
    public boolean isExternalSnapshot() {
        return true;
    }

    @Override
    public String getLocalPath() {
        return null;
    }

    @Override
    public String getSourceHost() {
        return this.sourceHost;
    }

    @Override
    public String getSourceDir() {
        return this.sourceDir;
    }

    @Override
    public int getSourcePort() {
        return this.sourcePort;
    }

    @Override
    public String getAuthUserName() {
        return null;
    }

    @Override
    public String getAuthSecret() {
        return null;
    }

    @Override
    public Storage.StoragePoolType getType() {
        return storagePoolType;
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public QemuImg.PhysicalDiskFormat getDefaultFormat() {
        return QemuImg.PhysicalDiskFormat.RAW;
    }

    @Override
    public boolean createFolder(String path) {
        return false;
    }

    @Override
    public boolean supportsConfigDriveIso() {
        return false;
    }

    @Override
    public Map<String, String> getDetails() {
        return this.details;
    }

    @Override
    public boolean isPoolSupportHA() {
        return false;
    }

    @Override
    public String getHearthBeatPath() {
        return null;
    }

    @Override
    public String createHeartBeatCommand(HAStoragePool primaryStoragePool, String hostPrivateIp,
            boolean hostValidation) {
        return null;
    }

    @Override
    public String getStorageNodeId() {
        return null;
    }

    @Override
    public Boolean checkingHeartBeat(HAStoragePool pool, HostTO host) {
        return null;
    }

    @Override
    public Boolean vmActivityCheck(HAStoragePool pool, HostTO host, Duration activityScriptTimeout,
            String volumeUUIDListString, String vmActivityCheckPath, long duration) {
        return null;
    }

    public void resize(String path, String vmName, long newSize) {
        ((MultipathSCSIAdapterBase)storageAdaptor).resize(path, vmName, newSize);
    }
}
