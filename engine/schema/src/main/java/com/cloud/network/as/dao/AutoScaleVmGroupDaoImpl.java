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

import org.springframework.stereotype.Component;

import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.network.as.AutoScaleVmGroupVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;

import javax.annotation.PostConstruct;

@Component
public class AutoScaleVmGroupDaoImpl extends GenericDaoBase<AutoScaleVmGroupVO, Long> implements AutoScaleVmGroupDao {

    SearchBuilder<AutoScaleVmGroupVO> AllFieldsSearch;

    @PostConstruct
    public void init() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("loadBalancerId", AllFieldsSearch.entity().getLoadBalancerId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("profileId", AllFieldsSearch.entity().getProfileId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public List<AutoScaleVmGroupVO> listByAll(Long loadBalancerId, Long profileId) {
        SearchCriteria<AutoScaleVmGroupVO> sc = AllFieldsSearch.create();

        if (loadBalancerId != null)
            sc.setParameters("loadBalancerId", loadBalancerId);

        if (profileId != null)
            sc.setParameters("profileId", profileId);

        return listBy(sc);
    }

    @Override
    public boolean isProfileInUse(long profileId) {
        SearchCriteria<AutoScaleVmGroupVO> sc = AllFieldsSearch.create();
        sc.setParameters("profileId", profileId);
        return findOneBy(sc) != null;
    }

    @Override
    public boolean isAutoScaleLoadBalancer(Long loadBalancerId) {
        GenericSearchBuilder<AutoScaleVmGroupVO, Long> countByLoadBalancer = createSearchBuilder(Long.class);
        countByLoadBalancer.select(null, Func.COUNT, null);
        countByLoadBalancer.and("loadBalancerId", countByLoadBalancer.entity().getLoadBalancerId(), SearchCriteria.Op.EQ);

        SearchCriteria<Long> sc = countByLoadBalancer.create();
        sc.setParameters("loadBalancerId", loadBalancerId);
        return customSearch(sc, null).get(0) > 0;
    }

    @Override
    public boolean updateState(long groupId, AutoScaleVmGroup.State oldState, AutoScaleVmGroup.State newState) {
        SearchCriteria<AutoScaleVmGroupVO> sc = AllFieldsSearch.create();
        sc.setParameters("id", groupId);
        sc.setParameters("state", oldState);
        AutoScaleVmGroupVO group = findOneBy(sc);
        if (group == null) {
            return false;
        }
        group.setState(newState);
        return update(groupId, group);
    }

    @Override
    public List<AutoScaleVmGroupVO> listByLoadBalancer(Long loadBalancerId) {
        SearchCriteria<AutoScaleVmGroupVO> sc = AllFieldsSearch.create();
        sc.setParameters("loadBalancerId", loadBalancerId);
        return listBy(sc);
    }

    @Override
    public List<AutoScaleVmGroupVO> listByProfile(Long profileId) {
        SearchCriteria<AutoScaleVmGroupVO> sc = AllFieldsSearch.create();
        sc.setParameters("profileId", profileId);
        return listBy(sc);
    }

    @Override
    public List<AutoScaleVmGroupVO> listByAccount(Long accountId) {
        SearchCriteria<AutoScaleVmGroupVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }
}
