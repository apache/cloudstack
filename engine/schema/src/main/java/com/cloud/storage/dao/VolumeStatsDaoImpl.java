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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.storage.VolumeStatsVO;

@Component
public class VolumeStatsDaoImpl extends GenericDaoBase<VolumeStatsVO, Long> implements VolumeStatsDao {

    protected Logger logger = LogManager.getLogger(getClass());

    protected SearchBuilder<VolumeStatsVO> volumeIdSearch;
    protected SearchBuilder<VolumeStatsVO> volumeIdTimestampGreaterThanEqualSearch;
    protected SearchBuilder<VolumeStatsVO> volumeIdTimestampLessThanEqualSearch;
    protected SearchBuilder<VolumeStatsVO> volumeIdTimestampBetweenSearch;
    protected SearchBuilder<VolumeStatsVO> timestampSearch;

    private final static String VOLUME_ID = "volumeId";
    private final static String TIMESTAMP = "timestamp";

    @PostConstruct
    protected void init() {
        volumeIdSearch = createSearchBuilder();
        volumeIdSearch.and(VOLUME_ID, volumeIdSearch.entity().getVolumeId(), Op.EQ);
        volumeIdSearch.done();

        volumeIdTimestampGreaterThanEqualSearch = createSearchBuilder();
        volumeIdTimestampGreaterThanEqualSearch.and(VOLUME_ID, volumeIdTimestampGreaterThanEqualSearch.entity().getVolumeId(), Op.EQ);
        volumeIdTimestampGreaterThanEqualSearch.and(TIMESTAMP, volumeIdTimestampGreaterThanEqualSearch.entity().getTimestamp(), Op.GTEQ);
        volumeIdTimestampGreaterThanEqualSearch.done();

        volumeIdTimestampLessThanEqualSearch = createSearchBuilder();
        volumeIdTimestampLessThanEqualSearch.and(VOLUME_ID, volumeIdTimestampLessThanEqualSearch.entity().getVolumeId(), Op.EQ);
        volumeIdTimestampLessThanEqualSearch.and(TIMESTAMP, volumeIdTimestampLessThanEqualSearch.entity().getTimestamp(), Op.LTEQ);
        volumeIdTimestampLessThanEqualSearch.done();

        volumeIdTimestampBetweenSearch = createSearchBuilder();
        volumeIdTimestampBetweenSearch.and(VOLUME_ID, volumeIdTimestampBetweenSearch.entity().getVolumeId(), Op.EQ);
        volumeIdTimestampBetweenSearch.and(TIMESTAMP, volumeIdTimestampBetweenSearch.entity().getTimestamp(), Op.BETWEEN);
        volumeIdTimestampBetweenSearch.done();

        timestampSearch = createSearchBuilder();
        timestampSearch.and(TIMESTAMP, timestampSearch.entity().getTimestamp(), Op.LT);
        timestampSearch.done();

    }

    @Override
    public List<VolumeStatsVO> findByVolumeId(long volumeId) {
        SearchCriteria<VolumeStatsVO> sc = volumeIdSearch.create();
        sc.setParameters(VOLUME_ID, volumeId);
        return listBy(sc);
    }

    @Override
    public List<VolumeStatsVO> findByVolumeIdOrderByTimestampDesc(long volumeId) {
        SearchCriteria<VolumeStatsVO> sc = volumeIdSearch.create();
        sc.setParameters(VOLUME_ID, volumeId);
        Filter orderByFilter = new Filter(VolumeStatsVO.class, TIMESTAMP, false, null, null);
        return search(sc, orderByFilter, null, false);
    }

    @Override
    public List<VolumeStatsVO> findByVolumeIdAndTimestampGreaterThanEqual(long volumeId, Date time) {
        SearchCriteria<VolumeStatsVO> sc = volumeIdTimestampGreaterThanEqualSearch.create();
        sc.setParameters(VOLUME_ID, volumeId);
        sc.setParameters(TIMESTAMP, time);
        return listBy(sc);
    }

    @Override
    public List<VolumeStatsVO> findByVolumeIdAndTimestampLessThanEqual(long volumeId, Date time) {
        SearchCriteria<VolumeStatsVO> sc = volumeIdTimestampLessThanEqualSearch.create();
        sc.setParameters(VOLUME_ID, volumeId);
        sc.setParameters(TIMESTAMP, time);
        return listBy(sc);
    }

    @Override
    public List<VolumeStatsVO> findByVolumeIdAndTimestampBetween(long volumeId, Date startTime, Date endTime) {
        SearchCriteria<VolumeStatsVO> sc = volumeIdTimestampBetweenSearch.create();
        sc.setParameters(VOLUME_ID, volumeId);
        sc.setParameters(TIMESTAMP, startTime, endTime);
        return listBy(sc);
    }

    @Override
    public void removeAllByVolumeId(long volumeId) {
        SearchCriteria<VolumeStatsVO> sc = volumeIdSearch.create();
        sc.setParameters(VOLUME_ID, volumeId);
        expunge(sc);
    }

    @Override
    public void removeAllByTimestampLessThan(Date limitDate, long limitPerQuery) {
        SearchCriteria<VolumeStatsVO> sc = timestampSearch.create();
        sc.setParameters(TIMESTAMP, limitDate);

        logger.debug(String.format("Starting to remove all volume_stats rows older than [%s].", limitDate));

        long totalRemoved = 0;
        long removed;

        do {
            removed = expunge(sc, limitPerQuery);
            totalRemoved += removed;
            logger.trace(String.format("Removed [%s] volume_stats rows on the last update and a sum of [%s] volume_stats rows older than [%s] until now.", removed, totalRemoved, limitDate));
        } while (limitPerQuery > 0 && removed >= limitPerQuery);

        logger.info(String.format("Removed a total of [%s] volume_stats rows older than [%s].", totalRemoved, limitDate));
    }
}
