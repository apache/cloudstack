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
        groupAndCounterSearch.and("policyId", groupAndCounterSearch.entity().getPolicyId(), Op.EQ);
        groupAndCounterSearch.and("counterId", groupAndCounterSearch.entity().getCounterId(), Op.EQ);
        groupAndCounterSearch.and("createdLT", groupAndCounterSearch.entity().getCreated(), Op.LT);
        groupAndCounterSearch.and("createdGT", groupAndCounterSearch.entity().getCreated(), Op.GT);
        groupAndCounterSearch.and("state", groupAndCounterSearch.entity().getState(), Op.EQ);
        groupAndCounterSearch.and("stateNEQ", groupAndCounterSearch.entity().getState(), Op.NEQ);
        groupAndCounterSearch.done();
    }

    @Override
    public boolean removeByGroupId(long vmGroupId) {
        SearchCriteria<AutoScaleVmGroupStatisticsVO> sc = groupAndCounterSearch.create();
        sc.setParameters("vmGroupId", vmGroupId);

        return expunge(sc) > 0;
    }

    @Override
    public boolean removeByGroupId(long vmGroupId, Date beforeDate) {
        SearchCriteria<AutoScaleVmGroupStatisticsVO> sc = groupAndCounterSearch.create();
        sc.setParameters("vmGroupId", vmGroupId);
        if (beforeDate != null) {
            sc.setParameters("createdLT", beforeDate);
        }
        return expunge(sc) > 0;
    }

    @Override
    public boolean removeByGroupAndPolicy(long vmGroupId, long policyId, Date beforeDate) {
        SearchCriteria<AutoScaleVmGroupStatisticsVO> sc = groupAndCounterSearch.create();
        sc.setParameters("vmGroupId", vmGroupId);
        sc.setParameters("policyId", policyId);
        if (beforeDate != null) {
            sc.setParameters("createdLT", beforeDate);
        }
        return expunge(sc) > 0;
    }

    @Override
    public List<AutoScaleVmGroupStatisticsVO> listDummyRecordsByVmGroup(long vmGroupId, Date afterDate) {
        SearchCriteria<AutoScaleVmGroupStatisticsVO> sc = groupAndCounterSearch.create();
        sc.setParameters("vmGroupId", vmGroupId);
        sc.setParameters("policyId", AutoScaleVmGroupStatisticsVO.DUMMY_ID);
        if (afterDate != null) {
            sc.setParameters("createdGT", afterDate);
        }
        sc.setParameters("state", AutoScaleVmGroupStatisticsVO.State.INACTIVE);
        return listBy(sc);
    }

    @Override
    public List<AutoScaleVmGroupStatisticsVO> listInactiveByVmGroupAndPolicy(long vmGroupId, long policyId, Date afterDate) {
        SearchCriteria<AutoScaleVmGroupStatisticsVO> sc = groupAndCounterSearch.create();
        sc.setParameters("vmGroupId", vmGroupId);
        sc.setParameters("policyId", policyId);
        if (afterDate != null) {
            sc.setParameters("createdGT", afterDate);
        }
        sc.setParameters("state", AutoScaleVmGroupStatisticsVO.State.INACTIVE);
        return listBy(sc);
    }

    @Override
    public List<AutoScaleVmGroupStatisticsVO> listByVmGroupAndPolicyAndCounter(long vmGroupId, long policyId, long counterId, Date afterDate) {
        SearchCriteria<AutoScaleVmGroupStatisticsVO> sc = groupAndCounterSearch.create();
        sc.setParameters("vmGroupId", vmGroupId);
        sc.setParameters("policyId", policyId);
        sc.setParameters("counterId", counterId);
        if (afterDate != null) {
            sc.setParameters("createdGT", afterDate);
        }
        sc.setParameters("state", AutoScaleVmGroupStatisticsVO.State.ACTIVE);
        return listBy(sc);
    }

    @Override
    public void updateStateByGroup(Long groupId, Long policyId, AutoScaleVmGroupStatisticsVO.State state) {
        SearchCriteria<AutoScaleVmGroupStatisticsVO> sc = groupAndCounterSearch.create();
        if (groupId != null) {
            sc.setParameters("vmGroupId", groupId);
        }
        if (policyId != null) {
            sc.setParameters("policyId", policyId);
        }
        sc.setParameters("stateNEQ", state);

        AutoScaleVmGroupStatisticsVO vo = createForUpdate();
        vo.setState(state);
        update(vo, sc);
    }

    @Override
    public AutoScaleVmGroupStatisticsVO createInactiveDummyRecord(Long groupId) {
        AutoScaleVmGroupStatisticsVO vo = new AutoScaleVmGroupStatisticsVO(groupId);
        return persist(vo);
    }
}
