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

import com.cloud.network.as.AutoScalePolicyConditionMapVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = {AutoScalePolicyConditionMapDao.class})
public class AutoScalePolicyConditionMapDaoImpl extends GenericDaoBase<AutoScalePolicyConditionMapVO, Long> implements AutoScalePolicyConditionMapDao {

    private SearchCriteria<AutoScalePolicyConditionMapVO> getSearchCriteria(Long policyId, Long conditionId) {
        SearchCriteria<AutoScalePolicyConditionMapVO> sc = createSearchCriteria();

        if (policyId != null)
            sc.addAnd("policyId", SearchCriteria.Op.EQ, policyId);

        if (conditionId != null)
            sc.addAnd("conditionId", SearchCriteria.Op.EQ, conditionId);

        return sc;
    }

    @Override
    public List<AutoScalePolicyConditionMapVO> listByAll(Long policyId, Long conditionId) {
        return listBy(getSearchCriteria(policyId, conditionId));
    }

    @Override
    public boolean isConditionInUse(Long conditionId) {
        return findOneBy(getSearchCriteria(null, conditionId)) != null;
    }

    @Override
    public boolean removeByAutoScalePolicyId(long policyId) {
        SearchCriteria<AutoScalePolicyConditionMapVO> sc = createSearchCriteria();
        sc.addAnd("policyId", SearchCriteria.Op.EQ, policyId);
        return expunge(sc) > 0;
    }
}
