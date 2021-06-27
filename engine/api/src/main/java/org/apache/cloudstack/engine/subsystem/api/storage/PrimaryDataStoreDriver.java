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

import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;

import com.cloud.host.Host;
import com.cloud.storage.StoragePool;
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
}
