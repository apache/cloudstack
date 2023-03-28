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
package com.cloud.vm.dao;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.vm.VmStatsVO;

@Component
public class VmStatsDaoImpl extends GenericDaoBase<VmStatsVO, Long> implements VmStatsDao {

    protected SearchBuilder<VmStatsVO> vmIdSearch;
    protected SearchBuilder<VmStatsVO> vmIdTimestampGreaterThanEqualSearch;
    protected SearchBuilder<VmStatsVO> vmIdTimestampLessThanEqualSearch;
    protected SearchBuilder<VmStatsVO> vmIdTimestampBetweenSearch;
    protected SearchBuilder<VmStatsVO> timestampSearch;

    @PostConstruct
    protected void init() {
        vmIdSearch = createSearchBuilder();
        vmIdSearch.and("vmId", vmIdSearch.entity().getVmId(), Op.EQ);
        vmIdSearch.done();

        vmIdTimestampGreaterThanEqualSearch = createSearchBuilder();
        vmIdTimestampGreaterThanEqualSearch.and("vmId", vmIdTimestampGreaterThanEqualSearch.entity().getVmId(), Op.EQ);
        vmIdTimestampGreaterThanEqualSearch.and("timestamp", vmIdTimestampGreaterThanEqualSearch.entity().getTimestamp(), Op.GTEQ);
        vmIdTimestampGreaterThanEqualSearch.done();

        vmIdTimestampLessThanEqualSearch = createSearchBuilder();
        vmIdTimestampLessThanEqualSearch.and("vmId", vmIdTimestampLessThanEqualSearch.entity().getVmId(), Op.EQ);
        vmIdTimestampLessThanEqualSearch.and("timestamp", vmIdTimestampLessThanEqualSearch.entity().getTimestamp(), Op.LTEQ);
        vmIdTimestampLessThanEqualSearch.done();

        vmIdTimestampBetweenSearch = createSearchBuilder();
        vmIdTimestampBetweenSearch.and("vmId", vmIdTimestampBetweenSearch.entity().getVmId(), Op.EQ);
        vmIdTimestampBetweenSearch.and("timestamp", vmIdTimestampBetweenSearch.entity().getTimestamp(), Op.BETWEEN);
        vmIdTimestampBetweenSearch.done();

        timestampSearch = createSearchBuilder();
        timestampSearch.and("timestamp", timestampSearch.entity().getTimestamp(), Op.LT);
        timestampSearch.done();

    }

    @Override
    public List<VmStatsVO> findByVmId(long vmId) {
        SearchCriteria<VmStatsVO> sc = vmIdSearch.create();
        sc.setParameters("vmId", vmId);
        return listBy(sc);
    }

    @Override
    public List<VmStatsVO> findByVmIdOrderByTimestampDesc(long vmId) {
        SearchCriteria<VmStatsVO> sc = vmIdSearch.create();
        sc.setParameters("vmId", vmId);
        Filter orderByFilter = new Filter(VmStatsVO.class, "timestamp", false, null, null);
        return search(sc, orderByFilter, null, false);
    }

    @Override
    public List<VmStatsVO> findByVmIdAndTimestampGreaterThanEqual(long vmId, Date time) {
        SearchCriteria<VmStatsVO> sc = vmIdTimestampGreaterThanEqualSearch.create();
        sc.setParameters("vmId", vmId);
        sc.setParameters("timestamp", time);
        return listBy(sc);
    }

    @Override
    public List<VmStatsVO> findByVmIdAndTimestampLessThanEqual(long vmId, Date time) {
        SearchCriteria<VmStatsVO> sc = vmIdTimestampLessThanEqualSearch.create();
        sc.setParameters("vmId", vmId);
        sc.setParameters("timestamp", time);
        return listBy(sc);
    }

    @Override
    public List<VmStatsVO> findByVmIdAndTimestampBetween(long vmId, Date startTime, Date endTime) {
        SearchCriteria<VmStatsVO> sc = vmIdTimestampBetweenSearch.create();
        sc.setParameters("vmId", vmId);
        sc.setParameters("timestamp", startTime, endTime);
        return listBy(sc);
    }

    @Override
    public void removeAllByVmId(long vmId) {
        SearchCriteria<VmStatsVO> sc = vmIdSearch.create();
        sc.setParameters("vmId", vmId);
        expunge(sc);
    }

    @Override
    public void removeAllByTimestampLessThan(Date limit) {
        SearchCriteria<VmStatsVO> sc = timestampSearch.create();
        sc.setParameters("timestamp", limit);
        expunge(sc);
    }

}
