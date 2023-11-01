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
package org.apache.cloudstack.storage.datastore.db;

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;

public interface SnapshotDataStoreDao extends GenericDao<SnapshotDataStoreVO, Long>,
StateDao<ObjectInDataStoreStateMachine.State, ObjectInDataStoreStateMachine.Event, DataObjectInStore> {

    List<SnapshotDataStoreVO> listByStoreId(long id, DataStoreRole role);

    List<SnapshotDataStoreVO> listByStoreIdAndState(long id, ObjectInDataStoreStateMachine.State state);

    List<SnapshotDataStoreVO> listBySnapshotIdAndState(long id, ObjectInDataStoreStateMachine.State state);

    List<SnapshotDataStoreVO> listActiveOnCache(long id);

    void deletePrimaryRecordsForStore(long id, DataStoreRole role);

    SnapshotDataStoreVO findByStoreSnapshot(DataStoreRole role, long storeId, long snapshotId);

    void removeBySnapshotStore(long snapshotId, long storeId, DataStoreRole role);

    SnapshotDataStoreVO findParent(DataStoreRole role, Long storeId, Long volumeId);

    List<SnapshotDataStoreVO> listBySnapshot(long snapshotId, DataStoreRole role);

    List<SnapshotDataStoreVO> listReadyBySnapshot(long snapshotId, DataStoreRole role);

    SnapshotDataStoreVO findBySourceSnapshot(long snapshotId, DataStoreRole role);

    List<SnapshotDataStoreVO> listDestroyed(long storeId);

    List<SnapshotDataStoreVO> findBySnapshotId(long snapshotId);

    void duplicateCacheRecordsOnRegionStore(long storeId);

    // delete the snapshot entry on primary data store to make sure that next snapshot will be full snapshot
    void deleteSnapshotRecordsOnPrimary();

    SnapshotDataStoreVO findReadyOnCache(long snapshotId);

    List<SnapshotDataStoreVO> listOnCache(long snapshotId);

    void updateStoreRoleToCache(long storeId);

    SnapshotDataStoreVO findLatestSnapshotForVolume(Long volumeId, DataStoreRole role);

    SnapshotDataStoreVO findOldestSnapshotForVolume(Long volumeId, DataStoreRole role);

    void updateVolumeIds(long oldVolId, long newVolId);

    List<SnapshotDataStoreVO> findByVolume(long snapshotId, long volumeId, DataStoreRole role);

    /**
     * List all snapshots in 'snapshot_store_ref' by volume and data store role. Therefore, it is possible to list all snapshots that are in the primary storage or in the secondary storage.
     */
    List<SnapshotDataStoreVO> listAllByVolumeAndDataStore(long volumeId, DataStoreRole role);

    List<SnapshotDataStoreVO> listByState(ObjectInDataStoreStateMachine.State... states);

    List<SnapshotDataStoreVO> findSnapshots(Long storeId, Date start, Date end);

    SnapshotDataStoreVO findDestroyedReferenceBySnapshot(long snapshotId, DataStoreRole role);

    /**
     * Removes the snapshot reference from the database according to its id and data store role.
     * @return true if success, otherwise, false.
     */
    boolean expungeReferenceBySnapshotIdAndDataStoreRole(long snapshotId, long storeId, DataStoreRole dataStorerole);

    /**
     * List all snapshots in 'snapshot_store_ref' with state 'Ready' by volume ID.
     */
    List<SnapshotDataStoreVO> listReadyByVolumeId(long volumeId);

    List<SnapshotDataStoreVO> listBySnasphotStoreDownloadStatus(long snapshotId, long storeId, VMTemplateStorageResourceAssoc.Status... status);

    SnapshotDataStoreVO findOneBySnapshotAndDatastoreRole(long snapshotId, DataStoreRole role);

    void updateDisplayForSnapshotStoreRole(long snapshotId, long storeId, DataStoreRole role, boolean display);
}
