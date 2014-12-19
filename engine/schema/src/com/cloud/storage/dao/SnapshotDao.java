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
package com.cloud.storage.dao;

import java.util.List;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Snapshot.Type;
import com.cloud.storage.SnapshotVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;

public interface SnapshotDao extends GenericDao<SnapshotVO, Long>, StateDao<Snapshot.State, Snapshot.Event, SnapshotVO> {
    List<SnapshotVO> listByVolumeId(long volumeId);

    List<SnapshotVO> listByVolumeId(Filter filter, long volumeId);

    SnapshotVO findNextSnapshot(long parentSnapId);

    long getLastSnapshot(long volumeId, DataStoreRole role);

    List<SnapshotVO> listByVolumeIdType(long volumeId, Type type);

    List<SnapshotVO> listByVolumeIdIncludingRemoved(long volumeId);

    List<SnapshotVO> listByBackupUuid(long volumeId, String backupUuid);

    long updateSnapshotVersion(long volumeId, String from, String to);

    List<SnapshotVO> listByVolumeIdVersion(long volumeId, String version);

    Long getSecHostId(long volumeId);

    long updateSnapshotSecHost(long dcId, long secHostId);

    public Long countSnapshotsForAccount(long accountId);

    List<SnapshotVO> listByInstanceId(long instanceId, Snapshot.State... status);

    List<SnapshotVO> listByStatus(long volumeId, Snapshot.State... status);

    List<SnapshotVO> listAllByStatus(Snapshot.State... status);

    void updateVolumeIds(long oldVolId, long newVolId);

}
