/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.engine.subsystem.api.storage;

import java.util.Map;

import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;

import com.cloud.host.Host;
import com.cloud.offering.DiskOffering;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.Pair;

public interface PrimaryDataStoreDriver extends DataStoreDriver {
    enum QualityOfServiceState { MIGRATION, NO_MIGRATION }

    String BASIC_CREATE = "basicCreate";
    String BASIC_DELETE = "basicDelete";
    String BASIC_DELETE_FAILURE = "basicDeleteFailure";
    String BASIC_DELETE_BY_FOLDER = "basicDeleteByFolder";
    String BASIC_GRANT_ACCESS = "basicGrantAccess";
    String BASIC_REVOKE_ACCESS = "basicRevokeAccess";
    String BASIC_IQN = "basicIqn";

    ChapInfo getChapInfo(DataObject dataObject);

    boolean grantAccess(DataObject dataObject, Host host, DataStore dataStore);

    void revokeAccess(DataObject dataObject, Host host, DataStore dataStore);

    default boolean requiresAccessForMigration(DataObject dataObject) {
        return false;
    }

    /**
     * intended for managed storage (cloud.storage_pool.managed = true)
     * if not managed, return volume.getSize()
     */
    long getDataObjectSizeIncludingHypervisorSnapshotReserve(DataObject dataObject, StoragePool storagePool);

    /**
     * intended for zone-wide primary storage that is capable of storing a template once and using it in multiple clusters
     * if not this kind of storage, return 0
     */
    long getBytesRequiredForTemplate(TemplateInfo templateInfo, StoragePool storagePool);

    /**
     * intended for managed storage (cloud.storage_pool.managed = true)
     * if managed storage, return the total number of bytes currently in use for the storage pool in question
     * if not managed storage, return 0
     */
    long getUsedBytes(StoragePool storagePool);

    /**
     * intended for managed storage (cloud.storage_pool.managed = true)
     * if managed storage, return the total number of IOPS currently in use for the storage pool in question
     * if not managed storage, return 0
     */
    long getUsedIops(StoragePool storagePool);

    void takeSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CreateCmdResult> callback);

    void revertSnapshot(SnapshotInfo snapshotOnImageStore, SnapshotInfo snapshotOnPrimaryStore, AsyncCompletionCallback<CommandResult> callback);

    void handleQualityOfServiceForVolumeMigration(VolumeInfo volumeInfo, QualityOfServiceState qualityOfServiceState);

    /**
     * intended for managed storage
     * returns true if the storage can provide the stats (capacity and used bytes)
     */
    boolean canProvideStorageStats();

    /**
     * intended for managed storage
     * returns true if the storage can provide its custom stats
     */
    default boolean poolProvidesCustomStorageStats() {
        return false;
    }

    /**
     * intended for managed storage
     * returns the custom stats if the storage can provide them
     */
    default Map<String, String> getCustomStorageStats(StoragePool pool) {
        return null;
    }

    /**
     * intended for managed storage
     * returns the total capacity and used size in bytes
     */
    Pair<Long, Long> getStorageStats(StoragePool storagePool);

    /**
     * intended for managed storage
     * returns true if the storage can provide the volume stats (physical and virtual size)
     */
    boolean canProvideVolumeStats();

    /**
     * intended for managed storage
     * returns the volume's physical and virtual size in bytes
     */
    Pair<Long, Long> getVolumeStats(StoragePool storagePool, String volumeId);

    /**
     * intended for managed storage
     * returns true if the host can access the storage pool
     */
    boolean canHostAccessStoragePool(Host host, StoragePool pool);

    /**
     * intended for managed storage
     * returns true if the host can prepare storage client to provide access the storage pool
     */
    default boolean canHostPrepareStoragePoolAccess(Host host, StoragePool pool) {
        return false;
    }

    /**
     * Used by storage pools which want to keep VMs' information
     * @return true if additional VM info is needed (intended for storage pools).
     */
    boolean isVmInfoNeeded();

    /**
     * Provides additional info for a VM (intended for storage pools).
     * E.g. the storage pool may want to keep/delete information if the volume is attached/detached to any VM.
     * @param vmId The ID of the virtual machine
     * @param volumeId the ID of the volume
     */
    void provideVmInfo(long vmId, long volumeId);

    /**
     * Returns true if the storage have to know about the VM's tags (intended for storage pools).
     * @param tagKey The name of the tag
     * @return true if the storage have to know about the VM's tags
     */
    boolean isVmTagsNeeded(String tagKey);

    /**
     * Provide VM's tags to storage (intended for storage pools).
     * @param vmId The ID of the virtual machine
     * @param volumeId The ID of the volume
     * @param tagValue The value of the VM's tag
     */
    void provideVmTags(long vmId, long volumeId, String tagValue);

    boolean isStorageSupportHA(StoragePoolType type);

    void detachVolumeFromAllStorageNodes(Volume volume);
    /**
     * Data store driver needs its grantAccess() method called for volumes in order for them to be used with a host.
     * @return true if we should call grantAccess() to use a volume
     */
    default boolean volumesRequireGrantAccessWhenUsed() {
        return false;
    }

    /**
     * Zone-wide data store supports using a volume across clusters without the need for data motion
     * @return true if we don't need to data motion volumes across clusters for zone-wide use
     */
    default boolean zoneWideVolumesAvailableWithoutClusterMotion() {
        return false;
    }

    /**
     * This method returns the actual size required on the pool for a volume.
     *
     * @param volumeSize
     *         Size of volume to be created on the store
     * @param templateSize
     *         Size of template, if any, which will be used to create the volume
     * @param isEncryptionRequired
     *         true if volume is encrypted
     *
     * @return the size required on the pool for the volume
     */
    default long getVolumeSizeRequiredOnPool(long volumeSize, Long templateSize, boolean isEncryptionRequired) {
        return volumeSize;
    }
    default boolean informStorageForDiskOfferingChange() {
        return false;
    }

    default void updateStorageWithTheNewDiskOffering(Volume volume, DiskOffering newDiskOffering) {}
}
