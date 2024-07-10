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

import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.joda.time.Duration;

import com.cloud.agent.api.to.HostTO;
import com.cloud.hypervisor.kvm.resource.KVMHABase.HAStoragePool;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;

public interface KVMStoragePool {

    public static final long HeartBeatUpdateTimeout = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.HEARTBEAT_UPDATE_TIMEOUT);
    public static final long HeartBeatUpdateFreq = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.KVM_HEARTBEAT_UPDATE_FREQUENCY);
    public static final long HeartBeatUpdateMaxTries = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.KVM_HEARTBEAT_UPDATE_MAX_TRIES);
    public static final long HeartBeatUpdateRetrySleep = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.KVM_HEARTBEAT_UPDATE_RETRY_SLEEP);
    public static final long HeartBeatCheckerTimeout = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.KVM_HEARTBEAT_CHECKER_TIMEOUT);

    public default KVMPhysicalDisk createPhysicalDisk(String volumeUuid, PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, Long usableSize, byte[] passphrase) {
        return createPhysicalDisk(volumeUuid, format, provisioningType, size, passphrase);
    }

    public KVMPhysicalDisk createPhysicalDisk(String volumeUuid, PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, byte[] passphrase);

    public KVMPhysicalDisk createPhysicalDisk(String volumeUuid, Storage.ProvisioningType provisioningType, long size, byte[] passphrase);

    public boolean connectPhysicalDisk(String volumeUuid, Map<String, String> details);

    public KVMPhysicalDisk getPhysicalDisk(String volumeUuid);

    public boolean disconnectPhysicalDisk(String volumeUuid);

    public boolean deletePhysicalDisk(String volumeUuid, Storage.ImageFormat format);

    public List<KVMPhysicalDisk> listPhysicalDisks();

    public String getUuid();

    public long getCapacity();

    public long getUsed();

    public long getAvailable();

    public boolean refresh();

    public boolean isExternalSnapshot();

    public String getLocalPath();

    public String getSourceHost();

    public String getSourceDir();

    public int getSourcePort();

    public String getAuthUserName();

    public String getAuthSecret();

    public StoragePoolType getType();

    public boolean delete();

    PhysicalDiskFormat getDefaultFormat();

    public boolean createFolder(String path);

    public boolean supportsConfigDriveIso();

    public Map<String, String> getDetails();

    public boolean isPoolSupportHA();

    public String getHearthBeatPath();

    public String createHeartBeatCommand(HAStoragePool primaryStoragePool, String hostPrivateIp, boolean hostValidation);

    public String getStorageNodeId();

    public Boolean checkingHeartBeat(HAStoragePool pool, HostTO host);

    public Boolean vmActivityCheck(HAStoragePool pool, HostTO host, Duration activityScriptTimeout, String volumeUUIDListString, String vmActivityCheckPath, long duration);

    default LibvirtVMDef.DiskDef.BlockIOSize getSupportedLogicalBlockSize() {
        return null;
    }

    default LibvirtVMDef.DiskDef.BlockIOSize getSupportedPhysicalBlockSize() {
        return null;
    }

    default void customizeLibvirtDiskDef(LibvirtVMDef.DiskDef disk) {
    }
}
