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

import com.cloud.network.as.AutoScaleVmGroupPolicyMapVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@Local(value={AutoScaleVmGroupPolicyMapDao.class})
public class AutoScaleVmGroupPolicyMapDaoImpl extends GenericDaoBase<AutoScaleVmGroupPolicyMapVO, Long> implements AutoScaleVmGroupPolicyMapDao {

    @Override
    public boolean removeByGroupId(long vmGroupId) {
        SearchCriteria<AutoScaleVmGroupPolicyMapVO> sc = createSearchCriteria();
        sc.addAnd("vmGroupId", SearchCriteria.Op.EQ, vmGroupId);

        return expunge(sc) > 0;
    }

    @Override
    public boolean removeByGroupAndPolicies(long vmGroupId, List<Long> policyIds) {
        SearchBuilder<AutoScaleVmGroupPolicyMapVO> policySearch = createSearchBuilder();
        policySearch.and("vmGroupId", policySearch.entity().getVmGroupId(), Op.EQ);
        policySearch.and("policyIds", policySearch.entity().getPolicyId(), Op.IN);
        policySearch.done();
        SearchCriteria<AutoScaleVmGroupPolicyMapVO> sc = policySearch.create();
        sc.setParameters("vmGroupId", vmGroupId);
        sc.setParameters("policyIds", policyIds);
        return expunge(sc) > 0;
    }

    @Override
    public List<AutoScaleVmGroupPolicyMapVO> listByVmGroupId(long vmGroupId) {
        SearchCriteria<AutoScaleVmGroupPolicyMapVO> sc = createSearchCriteria();
        sc.addAnd("vmGroupId", SearchCriteria.Op.EQ, vmGroupId);
        return listBy(sc);
    }

    @Override
    public List<AutoScaleVmGroupPolicyMapVO> listByPolicyId(long policyId) {
        SearchCriteria<AutoScaleVmGroupPolicyMapVO> sc = createSearchCriteria();
        sc.addAnd("policyId", SearchCriteria.Op.EQ, policyId);

        return listBy(sc);
    }

    @Override
    public boolean isAutoScalePolicyInUse(long policyId) {
        SearchCriteria<AutoScaleVmGroupPolicyMapVO> sc = createSearchCriteria();
        sc.addAnd("policyId", SearchCriteria.Op.EQ, policyId);
        return findOneBy(sc) != null;
    }

}
