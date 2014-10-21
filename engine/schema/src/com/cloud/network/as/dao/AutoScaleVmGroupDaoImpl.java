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

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.network.as.AutoScaleVmGroupVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;

@Component
@Local(value = {AutoScaleVmGroupDao.class})
public class AutoScaleVmGroupDaoImpl extends GenericDaoBase<AutoScaleVmGroupVO, Long> implements AutoScaleVmGroupDao {

    @Override
    public List<AutoScaleVmGroupVO> listByAll(Long loadBalancerId, Long profileId) {
        SearchCriteria<AutoScaleVmGroupVO> sc = createSearchCriteria();

        if (loadBalancerId != null)
            sc.addAnd("loadBalancerId", SearchCriteria.Op.EQ, loadBalancerId);

        if (profileId != null)
            sc.addAnd("profileId", SearchCriteria.Op.EQ, profileId);

        return listBy(sc);
    }

    @Override
    public boolean isProfileInUse(long profileId) {
        SearchCriteria<AutoScaleVmGroupVO> sc = createSearchCriteria();
        sc.addAnd("profileId", SearchCriteria.Op.EQ, profileId);
        return findOneBy(sc) != null;
    }

    @Override
    public boolean isAutoScaleLoadBalancer(Long loadBalancerId) {
        GenericSearchBuilder<AutoScaleVmGroupVO, Long> CountByAccount = createSearchBuilder(Long.class);
        CountByAccount.select(null, Func.COUNT, null);
        CountByAccount.and("loadBalancerId", CountByAccount.entity().getLoadBalancerId(), SearchCriteria.Op.EQ);

        SearchCriteria<Long> sc = CountByAccount.create();
        sc.setParameters("loadBalancerId", loadBalancerId);
        return customSearch(sc, null).get(0) > 0;
    }
}
