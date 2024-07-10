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

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.network.as.AutoScaleVmGroupVmMapVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class AutoScaleVmGroupVmMapDaoImpl extends GenericDaoBase<AutoScaleVmGroupVmMapVO, Long> implements AutoScaleVmGroupVmMapDao {

    @Inject
    VMInstanceDao vmInstanceDao;

    GenericSearchBuilder<AutoScaleVmGroupVmMapVO, Integer> CountBy;

    SearchBuilder<AutoScaleVmGroupVmMapVO> AllFieldsSearch;

    @PostConstruct
    protected void init() {
        CountBy = createSearchBuilder(Integer.class);
        CountBy.select(null, SearchCriteria.Func.COUNT, CountBy.entity().getId());
        CountBy.and("vmGroupId", CountBy.entity().getVmGroupId(), SearchCriteria.Op.EQ);
        final SearchBuilder<VMInstanceVO> vmSearch = vmInstanceDao.createSearchBuilder();
        vmSearch.and("states", vmSearch.entity().getState(), SearchCriteria.Op.IN);
        CountBy.join("vmSearch", vmSearch, CountBy.entity().getInstanceId(), vmSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        CountBy.done();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("vmGroupId", AllFieldsSearch.entity().getVmGroupId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("instanceId", AllFieldsSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public int countAvailableVmsByGroup(long vmGroupId) {

        SearchCriteria<Integer> sc = CountBy.create();
        sc.setParameters("vmGroupId", vmGroupId);
        sc.setJoinParameters("vmSearch", "states",
                State.Starting, State.Running, State.Stopping, State.Migrating);
        final List<Integer> results = customSearch(sc, null);
        return results.get(0);
    }

    @Override
    public Integer countByGroup(long vmGroupId) {

        SearchCriteria<AutoScaleVmGroupVmMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("vmGroupId", vmGroupId);
        return getCountIncludingRemoved(sc);
    }

    @Override
    public List<AutoScaleVmGroupVmMapVO> listByGroup(long vmGroupId) {
        SearchCriteria<AutoScaleVmGroupVmMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("vmGroupId", vmGroupId);
        return listBy(sc);
    }

    @Override
    public List<AutoScaleVmGroupVmMapVO> listByVm(long vmId) {
        SearchCriteria<AutoScaleVmGroupVmMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("instanceId", vmId);
        return listBy(sc);
    }

    @Override
    public int remove(long vmGroupId, long vmId) {
        SearchCriteria<AutoScaleVmGroupVmMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("vmGroupId", vmGroupId);
        sc.setParameters("instanceId", vmId);
        return remove(sc);
    }

    @Override
    public boolean removeByVm(long vmId) {
        SearchCriteria<AutoScaleVmGroupVmMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("instanceId", vmId);
        return remove(sc) >= 0;
    }

    @Override
    public boolean removeByGroup(long vmGroupId) {
        SearchCriteria<AutoScaleVmGroupVmMapVO> sc = AllFieldsSearch.create();
        sc.setParameters("vmGroupId", vmGroupId);
        return remove(sc) >= 0;
    }

    @Override
    public int expungeByVmList(List<Long> vmIds, Long batchSize) {
        if (CollectionUtils.isEmpty(vmIds)) {
            return 0;
        }
        SearchBuilder<AutoScaleVmGroupVmMapVO> sb = createSearchBuilder();
        sb.and("vmIds", sb.entity().getInstanceId(), SearchCriteria.Op.IN);
        SearchCriteria<AutoScaleVmGroupVmMapVO> sc = sb.create();
        sc.setParameters("vmIds", vmIds.toArray());
        return batchExpunge(sc, batchSize);
    }
}
