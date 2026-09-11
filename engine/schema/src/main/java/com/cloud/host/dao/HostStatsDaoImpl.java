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
package com.cloud.host.dao;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.host.HostStatsVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

/** DAO for the host_stats table. */
@Component
public class HostStatsDaoImpl extends GenericDaoBase<HostStatsVO, Long> implements HostStatsDao {

    protected Logger logger = LogManager.getLogger(getClass());

    protected SearchBuilder<HostStatsVO> hostIdSearch;
    protected SearchBuilder<HostStatsVO> hostIdTimestampGreaterThanEqualSearch;
    protected SearchBuilder<HostStatsVO> hostIdTimestampLessThanEqualSearch;
    protected SearchBuilder<HostStatsVO> hostIdTimestampBetweenSearch;
    protected SearchBuilder<HostStatsVO> timestampSearch;

    @PostConstruct
    protected void init() {
        hostIdSearch = createSearchBuilder();
        hostIdSearch.and("hostId", hostIdSearch.entity().getHostId(), Op.EQ);
        hostIdSearch.done();

        hostIdTimestampGreaterThanEqualSearch = createSearchBuilder();
        hostIdTimestampGreaterThanEqualSearch.and("hostId", hostIdTimestampGreaterThanEqualSearch.entity().getHostId(), Op.EQ);
        hostIdTimestampGreaterThanEqualSearch.and("timestamp", hostIdTimestampGreaterThanEqualSearch.entity().getTimestamp(), Op.GTEQ);
        hostIdTimestampGreaterThanEqualSearch.done();

        hostIdTimestampLessThanEqualSearch = createSearchBuilder();
        hostIdTimestampLessThanEqualSearch.and("hostId", hostIdTimestampLessThanEqualSearch.entity().getHostId(), Op.EQ);
        hostIdTimestampLessThanEqualSearch.and("timestamp", hostIdTimestampLessThanEqualSearch.entity().getTimestamp(), Op.LTEQ);
        hostIdTimestampLessThanEqualSearch.done();

        hostIdTimestampBetweenSearch = createSearchBuilder();
        hostIdTimestampBetweenSearch.and("hostId", hostIdTimestampBetweenSearch.entity().getHostId(), Op.EQ);
        hostIdTimestampBetweenSearch.and("timestamp", hostIdTimestampBetweenSearch.entity().getTimestamp(), Op.BETWEEN);
        hostIdTimestampBetweenSearch.done();

        timestampSearch = createSearchBuilder();
        timestampSearch.and("timestamp", timestampSearch.entity().getTimestamp(), Op.LT);
        timestampSearch.done();
    }

    @Override
    public List<HostStatsVO> findByHostId(long hostId) {
        SearchCriteria<HostStatsVO> sc = hostIdSearch.create();
        sc.setParameters("hostId", hostId);
        return listBy(sc);
    }

    @Override
    public List<HostStatsVO> findByHostIdAndTimestampGreaterThanEqual(long hostId, Date time) {
        SearchCriteria<HostStatsVO> sc = hostIdTimestampGreaterThanEqualSearch.create();
        sc.setParameters("hostId", hostId);
        sc.setParameters("timestamp", time);
        return listBy(sc);
    }

    @Override
    public List<HostStatsVO> findByHostIdAndTimestampLessThanEqual(long hostId, Date time) {
        SearchCriteria<HostStatsVO> sc = hostIdTimestampLessThanEqualSearch.create();
        sc.setParameters("hostId", hostId);
        sc.setParameters("timestamp", time);
        return listBy(sc);
    }

    @Override
    public List<HostStatsVO> findByHostIdAndTimestampBetween(long hostId, Date startTime, Date endTime) {
        SearchCriteria<HostStatsVO> sc = hostIdTimestampBetweenSearch.create();
        sc.setParameters("hostId", hostId);
        sc.setParameters("timestamp", startTime, endTime);
        return listBy(sc);
    }

    @Override
    public void removeAllByTimestampLessThan(Date limitDate, long limitPerQuery) {
        SearchCriteria<HostStatsVO> sc = timestampSearch.create();
        sc.setParameters("timestamp", limitDate);

        logger.debug(String.format("Starting to remove all host_stats rows older than [%s].", limitDate));

        long totalRemoved = batchExpunge(sc, limitPerQuery);

        logger.info(String.format("Removed a total of [%s] host_stats rows older than [%s].", totalRemoved, limitDate));
    }

}
