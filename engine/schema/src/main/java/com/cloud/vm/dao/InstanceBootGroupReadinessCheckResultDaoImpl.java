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

import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupReadinessCheckResultVO;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRule;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class InstanceBootGroupReadinessCheckResultDaoImpl extends GenericDaoBase<InstanceBootGroupReadinessCheckResultVO, Long> implements InstanceBootGroupReadinessCheckResultDao {

    private final SearchBuilder<InstanceBootGroupReadinessCheckResultVO> ruleIdSearch;
    private final SearchBuilder<InstanceBootGroupReadinessCheckResultVO> ruleAndVmSearch;

    public InstanceBootGroupReadinessCheckResultDaoImpl() {
        ruleIdSearch = createSearchBuilder();
        ruleIdSearch.and("ruleId", ruleIdSearch.entity().getRuleId(), SearchCriteria.Op.EQ);
        ruleIdSearch.done();

        ruleAndVmSearch = createSearchBuilder();
        ruleAndVmSearch.and("ruleId", ruleAndVmSearch.entity().getRuleId(), SearchCriteria.Op.EQ);
        ruleAndVmSearch.and("vmId", ruleAndVmSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        ruleAndVmSearch.done();
    }

    @Override
    public InstanceBootGroupReadinessCheckResultVO findByRuleAndVm(long ruleId, long vmId) {
        SearchCriteria<InstanceBootGroupReadinessCheckResultVO> sc = ruleAndVmSearch.create();
        sc.setParameters("ruleId", ruleId);
        sc.setParameters("vmId", vmId);
        return findOneBy(sc);
    }

    @Override
    public void upsert(long ruleId, long vmId, InstanceBootGroupReadinessRule.Status status, String message, Date checkedOn) {
        InstanceBootGroupReadinessCheckResultVO existing = findByRuleAndVm(ruleId, vmId);
        if (existing == null) {
            persist(new InstanceBootGroupReadinessCheckResultVO(ruleId, vmId, status, message, checkedOn));
        } else {
            existing.setStatus(status);
            existing.setMessage(message);
            existing.setCheckedOn(checkedOn);
            update(existing.getId(), existing);
        }
    }

    @Override
    public void deleteByRuleId(long ruleId) {
        SearchCriteria<InstanceBootGroupReadinessCheckResultVO> sc = ruleIdSearch.create();
        sc.setParameters("ruleId", ruleId);
        expunge(sc);
    }
}
