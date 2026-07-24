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

    public static final String CLVM_SECURE_ZERO_FILL = "clvmsecurezerofill";
    long HeartBeatUpdateTimeoutInMs = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.HEARTBEAT_UPDATE_TIMEOUT);
    long HeartBeatUpdateFreqInMs = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.KVM_HEARTBEAT_UPDATE_FREQUENCY);
    long HeartBeatCheckerTimeoutInMs = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.KVM_HEARTBEAT_CHECKER_TIMEOUT);

    default KVMPhysicalDisk createPhysicalDisk(String volumeUuid, PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, Long usableSize, byte[] passphrase) {
        return createPhysicalDisk(volumeUuid, format, provisioningType, size, passphrase);
    }

    KVMPhysicalDisk createPhysicalDisk(String volumeUuid, PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, byte[] passphrase);

    KVMPhysicalDisk createPhysicalDisk(String volumeUuid, Storage.ProvisioningType provisioningType, long size, byte[] passphrase);

    boolean connectPhysicalDisk(String volumeUuid, Map<String, String> details);

    KVMPhysicalDisk getPhysicalDisk(String volumeUuid);

    boolean disconnectPhysicalDisk(String volumeUuid);

    boolean deletePhysicalDisk(String volumeUuid, Storage.ImageFormat format);

    List<KVMPhysicalDisk> listPhysicalDisks();

    String getUuid();

    long getCapacity();

    long getUsed();

    default Long getCapacityIops() {
        return null;
    }

    default Long getUsedIops() {
        return null;
    }

    long getAvailable();

    boolean refresh();

    /**
     * Whether a freshly created volume on this pool is guaranteed to read back as
     * all zeros. Callers that skip writing zero blocks into a pre-created target
     * (e.g. qemu-img convert --target-is-zero / nbdcopy --destination-is-zero) must
     * only do so when this returns true; block backends that do not zero-initialize
     * new volumes (e.g. LVM-thick) return false so those callers write the zeros and
     * do not leak stale data from previously deleted volumes into unwritten regions.
     * Defaults to false so any backend that has not opted in is treated as unsafe.
     */
    default boolean isVolumeZeroInitialized(String volumeName) {
        return false;
    }

    /**
     * For clustered block storage whose host-local file lock is not authoritative
     * (e.g. Linstor/DRBD), returns the node name on which the given volume is currently
     * in use — attached to a running VM anywhere in the cluster — or null if not in use.
     * Adoption/import callers treat a non-null result as "locked". Defaults to null so
     * the host-local qemu-img lock remains the sole in-use signal for other pool types.
     */
    default String getVolumeInUseNode(String volumeName) {
        return null;
    }

    boolean isExternalSnapshot();

    String getLocalPath();

    String getSourceHost();

    String getSourceDir();

    int getSourcePort();

    String getAuthUserName();

    String getAuthSecret();

    StoragePoolType getType();

    boolean delete();

    PhysicalDiskFormat getDefaultFormat();

    boolean createFolder(String path);

    boolean supportsConfigDriveIso();

    Map<String, String> getDetails();

    default String getLocalPathFor(String relativePath) {
        return String.format("%s%s%s", getLocalPath(), File.separator, relativePath);
    }

    boolean isPoolSupportHA();

    String getHearthBeatPath();

    String createHeartBeatCommand(HAStoragePool primaryStoragePool, String hostPrivateIp, boolean hostValidation);

    String getStorageNodeId();

    Boolean hasHeartBeat(HAStoragePool pool, HostTO host);

    Boolean hasVmActivity(HAStoragePool pool, HostTO host, Duration activityScriptTimeout, String volumeUUIDListString, String vmActivityCheckPath, long duration);

    default LibvirtVMDef.DiskDef.BlockIOSize getSupportedLogicalBlockSize() {
        return null;
    }

    default LibvirtVMDef.DiskDef.BlockIOSize getSupportedPhysicalBlockSize() {
        return null;
    }

    default void customizeLibvirtDiskDef(LibvirtVMDef.DiskDef disk) {
    }
}
