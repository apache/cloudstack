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


import org.springframework.stereotype.Component;

import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.utils.DateUtil.IntervalType;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class SnapshotPolicyDaoImpl extends GenericDaoBase<SnapshotPolicyVO, Long> implements SnapshotPolicyDao {
    private final SearchBuilder<SnapshotPolicyVO> VolumeIdSearch;
    private final SearchBuilder<SnapshotPolicyVO> VolumeIdIntervalSearch;
    private final SearchBuilder<SnapshotPolicyVO> ActivePolicySearch;
    private final SearchBuilder<SnapshotPolicyVO> SnapshotPolicySearch;

    @Override
    public SnapshotPolicyVO findOneByVolumeInterval(long volumeId, IntervalType intvType) {
        SearchCriteria<SnapshotPolicyVO> sc = VolumeIdIntervalSearch.create();
        sc.setParameters("volumeId", volumeId);
        sc.setParameters("interval", intvType.ordinal());
        return findOneBy(sc);
    }

    @Override
    public SnapshotPolicyVO findOneByVolume(long volumeId) {
        SearchCriteria<SnapshotPolicyVO> sc = VolumeIdSearch.create();
        sc.setParameters("volumeId", volumeId);
        sc.setParameters("active", true);
        return findOneBy(sc);
    }

    @Override
    public List<SnapshotPolicyVO> listByVolumeId(long volumeId) {
        return listByVolumeId(volumeId, null);
    }

    @Override
    public List<SnapshotPolicyVO> listByVolumeId(long volumeId, Filter filter) {
        SearchCriteria<SnapshotPolicyVO> sc = VolumeIdSearch.create();
        sc.setParameters("volumeId", volumeId);
        return listBy(sc, filter);
    }

    @Override
    public Pair<List<SnapshotPolicyVO>, Integer> listAndCountByVolumeId(long volumeId, boolean display) {
        return listAndCountByVolumeId(volumeId, display, null);
    }

    @Override
    public Pair<List<SnapshotPolicyVO>, Integer> listAndCountByVolumeId(long volumeId, boolean display, Filter filter) {
        SearchCriteria<SnapshotPolicyVO> sc = VolumeIdSearch.create();
        sc.setParameters("volumeId", volumeId);
        sc.setParameters("display", display);
        sc.setParameters("active", true);
        return searchAndCount(sc, filter);
    }

    @Override
    public Pair<List<SnapshotPolicyVO>, Integer> listAndCountById(long id, boolean display, Filter filter){
        SearchCriteria<SnapshotPolicyVO> sc = SnapshotPolicySearch.create();
        sc.setParameters("id", id);
        sc.setParameters("display", display);
        return searchAndCount(sc, filter);
    }

    protected SnapshotPolicyDaoImpl() {
        VolumeIdSearch = createSearchBuilder();
        VolumeIdSearch.and("volumeId", VolumeIdSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        VolumeIdSearch.and("active", VolumeIdSearch.entity().isActive(), SearchCriteria.Op.EQ);
        VolumeIdSearch.and("display", VolumeIdSearch.entity().isDisplay(), SearchCriteria.Op.EQ);
        VolumeIdSearch.done();

        VolumeIdIntervalSearch = createSearchBuilder();
        VolumeIdIntervalSearch.and("volumeId", VolumeIdIntervalSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        VolumeIdIntervalSearch.and("interval", VolumeIdIntervalSearch.entity().getInterval(), SearchCriteria.Op.EQ);
        VolumeIdIntervalSearch.done();

        ActivePolicySearch = createSearchBuilder();
        ActivePolicySearch.and("active", ActivePolicySearch.entity().isActive(), SearchCriteria.Op.EQ);
        ActivePolicySearch.done();

        SnapshotPolicySearch = createSearchBuilder();
        SnapshotPolicySearch.and("id", SnapshotPolicySearch.entity().getId(), SearchCriteria.Op.EQ);
        SnapshotPolicySearch.and("display", SnapshotPolicySearch.entity().isDisplay(), SearchCriteria.Op.EQ);
        SnapshotPolicySearch.done();
    }

    @Override
    public List<SnapshotPolicyVO> listActivePolicies() {
        SearchCriteria<SnapshotPolicyVO> sc = ActivePolicySearch.create();
        sc.setParameters("active", true);
        return listIncludingRemovedBy(sc);
    }
}
