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

import java.util.Date;
import java.util.List;


import com.cloud.storage.SnapshotZoneVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class SnapshotZoneDaoImpl extends GenericDaoBase<SnapshotZoneVO, Long> implements SnapshotZoneDao {
    protected final SearchBuilder<SnapshotZoneVO> ZoneSnapshotSearch;

    public SnapshotZoneDaoImpl() {

        ZoneSnapshotSearch = createSearchBuilder();
        ZoneSnapshotSearch.and("zone_id", ZoneSnapshotSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        ZoneSnapshotSearch.and("snapshot_id", ZoneSnapshotSearch.entity().getSnapshotId(), SearchCriteria.Op.EQ);
        ZoneSnapshotSearch.done();
    }

    @Override
    public SnapshotZoneVO findByZoneSnapshot(long zoneId, long snapshotId) {
        SearchCriteria<SnapshotZoneVO> sc = ZoneSnapshotSearch.create();
        sc.setParameters("zone_id", zoneId);
        sc.setParameters("snapshot_id", snapshotId);
        return findOneBy(sc);
    }

    @Override
    public void addSnapshotToZone(long snapshotId, long zoneId) {
        SnapshotZoneVO snapshotZone = findByZoneSnapshot(zoneId, snapshotId);
        if (snapshotZone == null) {
            snapshotZone = new SnapshotZoneVO(zoneId, snapshotId, new Date());
            persist(snapshotZone);
        } else {
            snapshotZone.setRemoved(GenericDaoBase.DATE_TO_NULL);
            snapshotZone.setLastUpdated(new Date());
            update(snapshotZone.getId(), snapshotZone);
        }
    }

    @Override
    public void removeSnapshotFromZone(long snapshotId, long zoneId) {
        SearchCriteria<SnapshotZoneVO> sc = ZoneSnapshotSearch.create();
        sc.setParameters("zone_id", zoneId);
        sc.setParameters("snapshot_id", snapshotId);
        remove(sc);
    }

    @Override
    public void removeSnapshotFromZones(long snapshotId) {
        SearchCriteria<SnapshotZoneVO> sc = ZoneSnapshotSearch.create();
        sc.setParameters("snapshot_id", snapshotId);
        remove(sc);
    }

    @Override
    public List<SnapshotZoneVO> listBySnapshot(long snapshotId) {
        SearchCriteria<SnapshotZoneVO> sc = ZoneSnapshotSearch.create();
        sc.setParameters("snapshot_id", snapshotId);
        return listBy(sc);
    }
}
