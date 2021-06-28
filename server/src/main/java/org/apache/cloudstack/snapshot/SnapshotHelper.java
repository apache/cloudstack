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
import static com.cloud.storage.snapshot.SnapshotManager.BackupSnapshotAfterTakingSnapshot;
import com.cloud.utils.exception.CloudRuntimeException;
import java.util.Map;
import javax.inject.Inject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.log4j.Logger;

public class SnapshotHelper {
    private final Logger logger = Logger.getLogger(this.getClass());

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

    protected boolean backupSnapshotAfterTakingSnapshot = BackupSnapshotAfterTakingSnapshot.value();

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
          + " has the value [%s].", snapInfo.getId(), BackupSnapshotAfterTakingSnapshot.key(), backupSnapshotAfterTakingSnapshot));

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
            logger.trace(String.format("Snapshot [%s] is already on secondary storage or is not a KVM snapshot that is only kept in primary storage. Therefore, we do not back it."
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

        if (snapshot.getHypervisorType() == HypervisorType.KVM && !backupSnapshotAfterTakingSnapshot) {
            return DataStoreRole.Primary;
        }

        DataStore dataStore = dataStorageManager.getDataStore(snapshotStore.getDataStoreId(), DataStoreRole.Primary);

        if (dataStore == null) {
            return DataStoreRole.Image;
        }

        Map<String, String> mapCapabilities = dataStore.getDriver().getCapabilities();

        if (MapUtils.isEmpty(mapCapabilities)) {
            return DataStoreRole.Image;
        }

        if (BooleanUtils.toBoolean(mapCapabilities.get(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString()))) {
            return DataStoreRole.Primary;
        }

        return DataStoreRole.Image;
    }
}
