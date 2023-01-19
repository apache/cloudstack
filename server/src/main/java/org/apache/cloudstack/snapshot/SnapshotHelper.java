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

package org.apache.cloudstack.snapshot;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.dao.SnapshotDao;

import com.cloud.utils.exception.CloudRuntimeException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class SnapshotHelper {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    protected SnapshotDataStoreDao snapshotDataStoreDao;

    @Inject
    protected SnapshotDataFactory snapshotFactory;

    @Inject
    protected SnapshotService snapshotService;

    @Inject
    protected StorageStrategyFactory storageStrategyFactory;

    @Inject
    protected DataStoreManager dataStorageManager;

    @Inject
    protected SnapshotDao snapshotDao;

    @Inject
    protected PrimaryDataStoreDao primaryDataStoreDao;

    protected boolean backupSnapshotAfterTakingSnapshot = SnapshotInfo.BackupSnapshotAfterTakingSnapshot.value();

    protected final Set<StoragePoolType> storagePoolTypesToValidateWithBackupSnapshotAfterTakingSnapshot = new HashSet<>(Arrays.asList(StoragePoolType.RBD,
            StoragePoolType.PowerFlex));

     /**
     * If the snapshot is a backup from a KVM snapshot that should be kept only in primary storage, expunges it from secondary storage.
     * @param snapInfo the snapshot info to delete.
     */
    public void expungeTemporarySnapshot(boolean kvmSnapshotOnlyInPrimaryStorage, SnapshotInfo snapInfo) {
        if (!kvmSnapshotOnlyInPrimaryStorage) {
            logger.trace(String.format("Snapshot [%s] is not a temporary backup to create a volume from snapshot. Not expunging it.", snapInfo.getId()));
            return;
        }

        if (snapInfo == null) {
            logger.warn("Unable to expunge snapshot due to its info is null.");
            return;
        }

        logger.debug(String.format("Expunging snapshot [%s] due to it is a temporary backup to create a volume from snapshot. It is occurring because the global setting [%s]"
          + " has the value [%s].", snapInfo.getId(), SnapshotInfo.BackupSnapshotAfterTakingSnapshot.key(), backupSnapshotAfterTakingSnapshot));

        try {
            snapshotService.deleteSnapshot(snapInfo);
        } catch (CloudRuntimeException ex) {
            logger.warn(String.format("Unable to delete the temporary snapshot [%s] on secondary storage due to [%s]. We still will expunge the database reference, consider"
              + " manually deleting the file [%s].", snapInfo.getId(), ex.getMessage(), snapInfo.getPath()), ex);
        }

        snapshotDataStoreDao.expungeReferenceBySnapshotIdAndDataStoreRole(snapInfo.getId(), DataStoreRole.Image);
    }

    /**
     * Backup the snapshot to secondary storage if it should be backed up and was not yet or it is a temporary backup to create a volume.
     * @return The parameter snapInfo if the snapshot is not backupable, else backs up the snapshot to secondary storage and returns its info.
     * @throws CloudRuntimeException
     */
    public SnapshotInfo backupSnapshotToSecondaryStorageIfNotExists(SnapshotInfo snapInfo, DataStoreRole dataStoreRole, Snapshot snapshot, boolean kvmSnapshotOnlyInPrimaryStorage) throws CloudRuntimeException {
        if (!isSnapshotBackupable(snapInfo, dataStoreRole, kvmSnapshotOnlyInPrimaryStorage)) {
            logger.trace(String.format("Snapshot [%s] is already on secondary storage or is not a KVM snapshot that is only kept in primary storage. Therefore, we do not back it up."
              + " up.", snapInfo.getId()));

            return snapInfo;
        }

        snapInfo = getSnapshotInfoByIdAndRole(snapshot.getId(), DataStoreRole.Primary);

        SnapshotStrategy snapshotStrategy = storageStrategyFactory.getSnapshotStrategy(snapshot, SnapshotStrategy.SnapshotOperation.BACKUP);
        snapshotStrategy.backupSnapshot(snapInfo);

        return getSnapshotInfoByIdAndRole(snapshot.getId(), kvmSnapshotOnlyInPrimaryStorage ? DataStoreRole.Image : dataStoreRole);
    }

    /**
     * Search for the snapshot info by the snapshot id and {@link DataStoreRole}.
     * @return The snapshot info if it exists, else throws an exception.
     * @throws CloudRuntimeException
     */
    protected SnapshotInfo getSnapshotInfoByIdAndRole(long snapshotId, DataStoreRole dataStoreRole) throws CloudRuntimeException{
        SnapshotInfo snapInfo = snapshotFactory.getSnapshot(snapshotId, dataStoreRole);

        if (snapInfo != null) {
            return snapInfo;
        }

        throw new CloudRuntimeException(String.format("Could not find snapshot [%s] in %s storage. Therefore, we do not back it up.", snapshotId, dataStoreRole));
    }

    /**
     * Verifies if the snapshot is backupable.
     * @return true if snapInfo is null and dataStoreRole is {@link DataStoreRole#Image} or is a KVM snapshot that is only kept in primary storage, else false.
     */
    protected boolean isSnapshotBackupable(SnapshotInfo snapInfo, DataStoreRole dataStoreRole, boolean kvmSnapshotOnlyInPrimaryStorage) {
        return (snapInfo == null && dataStoreRole == DataStoreRole.Image) || kvmSnapshotOnlyInPrimaryStorage;
    }

    /**
     * Verifies if the snapshot was took on KVM and is kept in primary storage.
     * @return true if hypervisor is {@link  HypervisorType#KVM} and data store role is {@link  DataStoreRole#Primary} and global setting "snapshot.backup.to.secondary" is false,
     * else false.
     */
    public boolean isKvmSnapshotOnlyInPrimaryStorage(Snapshot snapshot, DataStoreRole dataStoreRole){
        return snapshot.getHypervisorType() == Hypervisor.HypervisorType.KVM && dataStoreRole == DataStoreRole.Primary && !backupSnapshotAfterTakingSnapshot;
    }

    public DataStoreRole getDataStoreRole(Snapshot snapshot) {
        SnapshotDataStoreVO snapshotStore = snapshotDataStoreDao.findBySnapshot(snapshot.getId(), DataStoreRole.Primary);

        if (snapshotStore == null) {
            return DataStoreRole.Image;
        }

        long storagePoolId = snapshotStore.getDataStoreId();

        StoragePoolVO storagePoolVO = primaryDataStoreDao.findById(storagePoolId);
        if ((storagePoolTypesToValidateWithBackupSnapshotAfterTakingSnapshot.contains(storagePoolVO.getPoolType()) || snapshot.getHypervisorType() == HypervisorType.KVM)
                && !backupSnapshotAfterTakingSnapshot) {
            return DataStoreRole.Primary;
        }

        DataStore dataStore = dataStorageManager.getDataStore(storagePoolId, DataStoreRole.Primary);

        if (dataStore == null) {
            return DataStoreRole.Image;
        }

        Map<String, String> mapCapabilities = dataStore.getDriver().getCapabilities();

        if (MapUtils.isNotEmpty(mapCapabilities) && BooleanUtils.toBoolean(mapCapabilities.get(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString()))) {
            return DataStoreRole.Primary;
        }

        return DataStoreRole.Image;
    }

    /**
     * Verifies if it is a KVM volume that has snapshots only in primary storage.
     * @throws CloudRuntimeException If it is a KVM volume and has at least one snapshot only in primary storage.
     */
    public void checkKvmVolumeSnapshotsOnlyInPrimaryStorage(VolumeVO volumeVo, HypervisorType hypervisorType) throws CloudRuntimeException {
        if (HypervisorType.KVM != hypervisorType) {
            logger.trace(String.format("The %s hypervisor [%s] is not KVM, therefore we will not check if the snapshots are only in primary storage.", volumeVo, hypervisorType));
            return;
        }

        Set<Long> snapshotIdsOnlyInPrimaryStorage = getSnapshotIdsOnlyInPrimaryStorage(volumeVo.getId());

        if (CollectionUtils.isEmpty(snapshotIdsOnlyInPrimaryStorage)) {
            logger.trace(String.format("%s is a KVM volume and all its snapshots exists in the secondary storage, therefore this volume is able for migration.", volumeVo));
            return;
        }

        throwCloudRuntimeExceptionOfSnapshotsOnlyInPrimaryStorage(volumeVo, snapshotIdsOnlyInPrimaryStorage);
    }

    /**
     * Throws a CloudRuntimeException with the volume and the snapshots only in primary storage.
     */
    protected void throwCloudRuntimeExceptionOfSnapshotsOnlyInPrimaryStorage(VolumeVO volumeVo, Set<Long> snapshotIdsOnlyInPrimaryStorage) throws CloudRuntimeException {
        List<SnapshotVO> snapshots = snapshotDao.listByIds(snapshotIdsOnlyInPrimaryStorage.toArray());

        String message = String.format("%s is a KVM volume and has snapshots only in primary storage. Snapshots [%s].%s", volumeVo,
                snapshots.stream().map(snapshot -> new ToStringBuilder(snapshot, ToStringStyle.JSON_STYLE).append("uuid", snapshot.getUuid()).append("name", snapshot.getName())
                        .build()).collect(Collectors.joining(", ")), backupSnapshotAfterTakingSnapshot ? "" : " Consider excluding them to migrate the volume to another storage.");

        logger.error(message);
        throw new CloudRuntimeException(message);
    }

    /**
     * Retrieves the ids of the ready snapshots of the volume that only exists in primary storage.
     * @param volumeId volume id to retrieve the snapshots.
     * @return The ids of the ready snapshots of the volume that only exists in primary storage
     */
    protected Set<Long> getSnapshotIdsOnlyInPrimaryStorage(long volumeId) {
        List<SnapshotDataStoreVO> snapshotsReferences = snapshotDataStoreDao.listReadyByVolumeId(volumeId);
        Map<Long, List<SnapshotDataStoreVO>> referencesGroupBySnapshotId = snapshotsReferences.stream().collect(Collectors.groupingBy(reference -> reference.getSnapshotId()));

        Set<Long> snapshotIdsOnlyInPrimaryStorage = new HashSet<>();
        for (var reference: referencesGroupBySnapshotId.entrySet()) {
            List<SnapshotDataStoreVO> listReferencesBySnapshotId = reference.getValue();

            if  (!listReferencesBySnapshotId.stream().anyMatch(ref -> DataStoreRole.Image == ref.getRole())) {
                snapshotIdsOnlyInPrimaryStorage.add(reference.getKey());
            }
        }

        return snapshotIdsOnlyInPrimaryStorage;
    }
}
