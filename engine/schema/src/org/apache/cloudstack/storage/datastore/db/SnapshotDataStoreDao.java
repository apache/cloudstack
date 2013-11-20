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

import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;

import com.cloud.storage.DataStoreRole;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;

public interface SnapshotDataStoreDao extends GenericDao<SnapshotDataStoreVO, Long>,
        StateDao<ObjectInDataStoreStateMachine.State, ObjectInDataStoreStateMachine.Event, DataObjectInStore> {

    List<SnapshotDataStoreVO> listByStoreId(long id, DataStoreRole role);

    List<SnapshotDataStoreVO> listActiveOnCache(long id);

    void deletePrimaryRecordsForStore(long id, DataStoreRole role);

    SnapshotDataStoreVO findByStoreSnapshot(DataStoreRole role, long storeId, long snapshotId);

    SnapshotDataStoreVO findParent(DataStoreRole role, Long storeId, Long volumeId);

    SnapshotDataStoreVO findBySnapshot(long snapshotId, DataStoreRole role);

    List<SnapshotDataStoreVO> listDestroyed(long storeId);

    List<SnapshotDataStoreVO> findBySnapshotId(long snapshotId);

    void duplicateCacheRecordsOnRegionStore(long storeId);

    // delete the snapshot entry on primary data store to make sure that next snapshot will be full snapshot
    void deleteSnapshotRecordsOnPrimary();

    List<SnapshotDataStoreVO> listOnCache(long snapshotId);

    void updateStoreRoleToCache(long storeId);
}
