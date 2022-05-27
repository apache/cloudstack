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
package com.cloud.network.as.dao;

import java.util.Date;
import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.network.as.AutoScaleVmGroupStatisticsVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

import javax.annotation.PostConstruct;

@Component
public class AutoScaleVmGroupStatisticsDaoImpl extends GenericDaoBase<AutoScaleVmGroupStatisticsVO, Long> implements AutoScaleVmGroupStatisticsDao {

    SearchBuilder<AutoScaleVmGroupStatisticsVO> groupAndCounterSearch;

    @PostConstruct
    protected void init() {
        groupAndCounterSearch = createSearchBuilder();
        groupAndCounterSearch.and("vmGroupId", groupAndCounterSearch.entity().getVmGroupId(), Op.EQ);
        groupAndCounterSearch.and("counterId", groupAndCounterSearch.entity().getCounterId(), Op.EQ);
        groupAndCounterSearch.and("createdLT", groupAndCounterSearch.entity().getCreated(), Op.LT);
        groupAndCounterSearch.and("createdGT", groupAndCounterSearch.entity().getCreated(), Op.GT);
        groupAndCounterSearch.done();
    }

    @Override
    public boolean removeByGroupId(long vmGroupId) {
        SearchCriteria<AutoScaleVmGroupStatisticsVO> sc = groupAndCounterSearch.create();
        sc.addAnd("vmGroupId", SearchCriteria.Op.EQ, vmGroupId);

        return expunge(sc) > 0;
    }

    @Override
    public boolean removeByGroupAndCounter(long vmGroupId, long counterId) {
        SearchCriteria<AutoScaleVmGroupStatisticsVO> sc = groupAndCounterSearch.create();
        sc.setParameters("vmGroupId", vmGroupId);
        sc.setParameters("counterId", counterId);
        return expunge(sc) > 0;
    }

    @Override
    public boolean removeByGroupAndCounter(long vmGroupId, long counterId, Date beforeDate) {
        SearchCriteria<AutoScaleVmGroupStatisticsVO> sc = groupAndCounterSearch.create();
        sc.setParameters("vmGroupId", vmGroupId);
        sc.setParameters("counterId", counterId);
        sc.setParameters("createdLT", beforeDate);
        return expunge(sc) > 0;
    }

    @Override
    public List<AutoScaleVmGroupStatisticsVO> listByVmGroupId(long vmGroupId) {
        SearchCriteria<AutoScaleVmGroupStatisticsVO> sc = groupAndCounterSearch.create();
        sc.setParameters("vmGroupId", vmGroupId);
        return listBy(sc);
    }

    @Override
    public List<AutoScaleVmGroupStatisticsVO> listByVmGroupIdAndCounterId(long vmGroupId, long counterId, Date afterDate) {
        SearchCriteria<AutoScaleVmGroupStatisticsVO> sc = groupAndCounterSearch.create();
        sc.setParameters("vmGroupId", vmGroupId);
        sc.setParameters("counterId", counterId);
        sc.setParameters("createdGT", afterDate);
        return listBy(sc);
    }
}
