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

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.storage.VolumeStatsVO;

@Component
public class VolumeStatsDaoImpl extends GenericDaoBase<VolumeStatsVO, Long> implements VolumeStatsDao {

    protected SearchBuilder<VolumeStatsVO> volumeIdSearch;
    protected SearchBuilder<VolumeStatsVO> volumeIdTimestampGreaterThanEqualSearch;
    protected SearchBuilder<VolumeStatsVO> volumeIdTimestampLessThanEqualSearch;
    protected SearchBuilder<VolumeStatsVO> volumeIdTimestampBetweenSearch;
    protected SearchBuilder<VolumeStatsVO> timestampSearch;

    @PostConstruct
    protected void init() {
        volumeIdSearch = createSearchBuilder();
        volumeIdSearch.and("volumeId", volumeIdSearch.entity().getVolumeId(), Op.EQ);
        volumeIdSearch.done();

        volumeIdTimestampGreaterThanEqualSearch = createSearchBuilder();
        volumeIdTimestampGreaterThanEqualSearch.and("volumeId", volumeIdTimestampGreaterThanEqualSearch.entity().getVolumeId(), Op.EQ);
        volumeIdTimestampGreaterThanEqualSearch.and("timestamp", volumeIdTimestampGreaterThanEqualSearch.entity().getTimestamp(), Op.GTEQ);
        volumeIdTimestampGreaterThanEqualSearch.done();

        volumeIdTimestampLessThanEqualSearch = createSearchBuilder();
        volumeIdTimestampLessThanEqualSearch.and("volumeId", volumeIdTimestampLessThanEqualSearch.entity().getVolumeId(), Op.EQ);
        volumeIdTimestampLessThanEqualSearch.and("timestamp", volumeIdTimestampLessThanEqualSearch.entity().getTimestamp(), Op.LTEQ);
        volumeIdTimestampLessThanEqualSearch.done();

        volumeIdTimestampBetweenSearch = createSearchBuilder();
        volumeIdTimestampBetweenSearch.and("volumeId", volumeIdTimestampBetweenSearch.entity().getVolumeId(), Op.EQ);
        volumeIdTimestampBetweenSearch.and("timestamp", volumeIdTimestampBetweenSearch.entity().getTimestamp(), Op.BETWEEN);
        volumeIdTimestampBetweenSearch.done();

        timestampSearch = createSearchBuilder();
        timestampSearch.and("timestamp", timestampSearch.entity().getTimestamp(), Op.LT);
        timestampSearch.done();

    }

    @Override
    public List<VolumeStatsVO> findByVolumeId(long volumeId) {
        SearchCriteria<VolumeStatsVO> sc = volumeIdSearch.create();
        sc.setParameters("volumeId", volumeId);
        return listBy(sc);
    }

    @Override
    public List<VolumeStatsVO> findByVolumeIdOrderByTimestampDesc(long volumeId) {
        SearchCriteria<VolumeStatsVO> sc = volumeIdSearch.create();
        sc.setParameters("volumeId", volumeId);
        Filter orderByFilter = new Filter(VolumeStatsVO.class, "timestamp", false, null, null);
        return search(sc, orderByFilter, null, false);
    }

    @Override
    public List<VolumeStatsVO> findByVolumeIdAndTimestampGreaterThanEqual(long volumeId, Date time) {
        SearchCriteria<VolumeStatsVO> sc = volumeIdTimestampGreaterThanEqualSearch.create();
        sc.setParameters("volumeId", volumeId);
        sc.setParameters("timestamp", time);
        return listBy(sc);
    }

    @Override
    public List<VolumeStatsVO> findByVolumeIdAndTimestampLessThanEqual(long volumeId, Date time) {
        SearchCriteria<VolumeStatsVO> sc = volumeIdTimestampLessThanEqualSearch.create();
        sc.setParameters("volumeId", volumeId);
        sc.setParameters("timestamp", time);
        return listBy(sc);
    }

    @Override
    public List<VolumeStatsVO> findByVolumeIdAndTimestampBetween(long volumeId, Date startTime, Date endTime) {
        SearchCriteria<VolumeStatsVO> sc = volumeIdTimestampBetweenSearch.create();
        sc.setParameters("volumeId", volumeId);
        sc.setParameters("timestamp", startTime, endTime);
        return listBy(sc);
    }

    @Override
    public void removeAllByVolumeId(long volumeId) {
        SearchCriteria<VolumeStatsVO> sc = volumeIdSearch.create();
        sc.setParameters("volumeId", volumeId);
        expunge(sc);
    }

    @Override
    public void removeAllByTimestampLessThan(Date limit) {
        SearchCriteria<VolumeStatsVO> sc = timestampSearch.create();
        sc.setParameters("timestamp", limit);
        expunge(sc);
    }

}