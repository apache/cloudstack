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
import org.apache.cloudstack.storage.volume.VolumeOnStorageTO;
import org.joda.time.Duration;

import com.cloud.agent.api.to.HostTO;
import com.cloud.hypervisor.kvm.resource.KVMHABase.HAStoragePool;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;

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

    /** Read-only backend import metadata; null path requests all volumes in this pool. */
    default List<VolumeOnStorageTO> getVolumesForImport(String path) {
        throw new UnsupportedOperationException("Backend import metadata is not supported by this storage pool");
    }

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
     * Returned by {@link #getVolumeInUseNode(String)} when the backend could not tell us
     * whether the volume is in use. Callers must fail closed and treat it as "in use":
     * adopting a clustered volume that is actually attached elsewhere would give it two writers.
     */
    String IN_USE_NODE_UNKNOWN = "<unknown>";

    /**
     * For clustered block storage whose host-local file lock is not authoritative
     * (e.g. Linstor/DRBD), returns the node name on which the given volume is currently
     * in use — attached to a running VM anywhere in the cluster — or null if not in use.
     * Adoption/import callers treat a non-null result as "locked".
     *
     * Implementations that cannot establish the cluster-wide state (controller unreachable,
     * empty or malformed response) must return {@link #IN_USE_NODE_UNKNOWN} rather than null,
     * so the ambiguity is not silently read as "free".
     *
     * Defaults to null so the host-local qemu-img lock remains the sole in-use signal for
     * other pool types.
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

    /**
     * Whether the volume {@code volumeUuid}, freshly created in this pool, already reads as zeroes,
     * so a copy onto it may skip zero regions (qemu-img --target-is-zero). Says nothing about a
     * volume that has been written to: that still holds its old data.
     */
    default boolean newVolumeReadsAsZero(String volumeUuid) {
        return false;
    }

    /**
     * Creates the scratch space for a pull-mode backup of the running VM {@code vmName} on this host: one
     * block device per entry of {@code sizes}, at least that many bytes large. libvirt formats each device
     * as the copy-before-write target of one disk, keeping the guest data the backup has not read yet.
     *
     * @return the name to pass to {@link #deleteBackupScratch(String)} and the device paths, in the order
     *         of {@code sizes}
     */
    default Pair<String, List<String>> createBackupScratch(String vmName, List<Long> sizes) {
        throw new CloudRuntimeException(String.format("Storage pool %s of type %s cannot provide backup scratch space",
                getUuid(), getType()));
    }

    /**
     * Removes scratch space created by {@link #createBackupScratch(String, List)}. Only valid once the
     * backup job has ended and libvirt has released the devices.
     */
    default void deleteBackupScratch(String name) {
        throw new CloudRuntimeException(String.format("Storage pool %s of type %s cannot provide backup scratch space",
                getUuid(), getType()));
    }
}
