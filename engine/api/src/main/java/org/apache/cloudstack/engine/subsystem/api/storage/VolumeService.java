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

import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.storage.command.CommandResult;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.exception.StorageAccessException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.DiskOffering;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public interface VolumeService {

    String SNAPSHOT_ID = "SNAPSHOT_ID";

    class VolumeApiResult extends CommandResult {
        private final VolumeInfo volume;

        public VolumeApiResult(VolumeInfo volume) {
            super();
            this.volume = volume;
        }

        public VolumeInfo getVolume() {
            return this.volume;
        }
    }

    ChapInfo getChapInfo(DataObject dataObject, DataStore dataStore);

    boolean grantAccess(DataObject dataObject, Host host, DataStore dataStore);

    void revokeAccess(DataObject dataObject, Host host, DataStore dataStore);

    boolean requiresAccessForMigration(DataObject dataObject, DataStore dataStore);

    /**
     * Creates the volume based on the given criteria
     *
     * @return the volume object
     */
    AsyncCallFuture<VolumeApiResult> createVolumeAsync(VolumeInfo volume, DataStore store);

    /**
     * Delete volume
     */
    AsyncCallFuture<VolumeApiResult> expungeVolumeAsync(VolumeInfo volume);

    void ensureVolumeIsExpungeReady(long volumeId);

    boolean cloneVolume(long volumeId, long baseVolId);

    AsyncCallFuture<VolumeApiResult> createVolumeFromSnapshot(VolumeInfo volume, DataStore store, SnapshotInfo snapshot);

    VolumeEntity getVolumeEntity(long volumeId);

    TemplateInfo createManagedStorageTemplate(long srcTemplateId, long destDataStoreId, long destHostId) throws StorageAccessException;

    AsyncCallFuture<VolumeApiResult> createManagedStorageVolumeFromTemplateAsync(VolumeInfo volumeInfo, long destDataStoreId, TemplateInfo srcTemplateInfo, long destHostId) throws StorageAccessException;

    AsyncCallFuture<VolumeApiResult> createVolumeFromTemplateAsync(VolumeInfo volume, long dataStoreId, TemplateInfo template);

    AsyncCallFuture<VolumeApiResult> copyVolume(VolumeInfo srcVolume, DataStore destStore);

    AsyncCallFuture<VolumeApiResult> migrateVolume(VolumeInfo srcVolume, DataStore destStore);

    AsyncCallFuture<CommandResult> migrateVolumes(Map<VolumeInfo, DataStore> volumeMap, VirtualMachineTO vmTo, Host srcHost, Host destHost);

    void destroyVolume(long volumeId);

    AsyncCallFuture<VolumeApiResult> registerVolume(VolumeInfo volume, DataStore store);

    public Pair<EndPoint, DataObject> registerVolumeForPostUpload(VolumeInfo volume, DataStore store);

    AsyncCallFuture<VolumeApiResult> resize(VolumeInfo volume);

    void resizeVolumeOnHypervisor(long volumeId, long newSize, long destHostId, String instanceName);

    void handleVolumeSync(DataStore store);

    SnapshotInfo takeSnapshot(VolumeInfo volume);

    VolumeInfo updateHypervisorSnapshotReserveForVolume(DiskOffering diskOffering, long volumeId, HypervisorType hyperType);

    void unmanageVolume(long volumeId);

    /**
     * After volume migration, copies snapshot policies from the source volume to destination volume; then, it destroys and expunges the source volume.
     * @return If no exception happens, it will return false, otherwise true.
     */
    boolean copyPoliciesBetweenVolumesAndDestroySourceVolumeAfterMigration(ObjectInDataStoreStateMachine.Event destinationEvent, Answer destinationEventAnswer,
      VolumeInfo sourceVolume, VolumeInfo destinationVolume, boolean retryExpungeVolumeAsync);

    void moveVolumeOnSecondaryStorageToAnotherAccount(Volume volume, Account sourceAccount, Account destAccount);

    Pair<String, String> checkAndRepairVolume(VolumeInfo volume);

    void checkAndRepairVolumeBasedOnConfig(DataObject dataObject, Host host);

    void validateChangeDiskOfferingEncryptionType(long existingDiskOfferingId, long newDiskOfferingId);

    /**
     * Transfers exclusive lock for a volume on cluster-based storage (e.g., CLVM/CLVM_NG) from one host to another.
     * This is used for storage that requires host-level lock management for volumes on shared storage pools.
     * For non-CLVM pool types, this method returns false without taking action.
     *
     * @param volume The volume to transfer lock for
     * @param sourceHostId Host currently holding the exclusive lock
     * @param destHostId Host to receive the exclusive lock
     * @return true if lock transfer succeeded or was not needed, false if it failed
     */
    boolean transferVolumeLock(VolumeInfo volume, Long sourceHostId, Long destHostId);

    /**
     * Finds which host currently has the exclusive lock on a CLVM volume.
     * Checks in order: explicit lock tracking, attached VM's host, or first available cluster host.
     *
     * @param volume The CLVM volume
     * @return Host ID that has the exclusive lock, or null if cannot be determined
     */
    Long findVolumeLockHost(VolumeInfo volume);

    /**
     * Performs lightweight CLVM lock migration for a volume to a target host.
     * This transfers the LVM exclusive lock without copying data (CLVM volumes are on shared cluster storage).
     * If the volume already has the lock on the destination host, no action is taken.
     *
     * @param volume The volume to migrate lock for
     * @param destHostId Destination host ID
     * @return Updated VolumeInfo after lock migration
     */
    VolumeInfo performLockMigration(VolumeInfo volume, Long destHostId);

    /**
     * Checks if both storage pools are CLVM type (CLVM or CLVM_NG).
     *
     * @param volumePoolType Storage pool type for the volume
     * @param vmPoolType Storage pool type for the VM
     * @return true if both pools are CLVM type (CLVM or CLVM_NG)
     */
    boolean areBothPoolsClvmType(StoragePoolType volumePoolType, StoragePoolType vmPoolType);

    /**
     * Determines if CLVM lock transfer is required when a volume is already on the correct storage pool.
     *
     * @param volumeToAttach The volume being attached
     * @param volumePoolType Storage pool type for the volume
     * @param vmPoolType Storage pool type for the VM's existing volume
     * @param volumePoolId Storage pool ID for the volume
     * @param vmPoolId Storage pool ID for the VM's existing volume
     * @param vmHostId VM's current host ID (or last host ID if stopped)
     * @return true if CLVM lock transfer is needed
     */
    boolean isLockTransferRequired(VolumeInfo volumeToAttach, StoragePoolType volumePoolType, StoragePoolType vmPoolType,
                                   Long volumePoolId, Long vmPoolId, Long vmHostId);

    /**
     * Determines if lightweight CLVM migration is needed instead of full data copy.
     *
     * @param volumePoolType Storage pool type for the volume
     * @param vmPoolType Storage pool type for the VM
     * @param volumePoolPath Storage pool path for the volume
     * @param vmPoolPath Storage pool path for the VM
     * @return true if lightweight migration should be used
     */
    boolean isLightweightMigrationNeeded(StoragePoolType volumePoolType, StoragePoolType vmPoolType,
                                         String volumePoolPath, String vmPoolPath);
}
