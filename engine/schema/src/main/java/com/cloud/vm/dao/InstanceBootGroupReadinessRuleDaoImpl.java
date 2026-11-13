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

import java.util.List;

import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupMember;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupReadinessRuleVO;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRule;
import org.springframework.stereotype.Component;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class InstanceBootGroupReadinessRuleDaoImpl extends GenericDaoBase<InstanceBootGroupReadinessRuleVO, Long> implements InstanceBootGroupReadinessRuleDao {

    private final SearchBuilder<InstanceBootGroupReadinessRuleVO> itemSearch;
    private final SearchBuilder<InstanceBootGroupReadinessRuleVO> enabledByItemSearch;
    private final SearchBuilder<InstanceBootGroupReadinessRuleVO> byItemSearch;

    public InstanceBootGroupReadinessRuleDaoImpl() {

        itemSearch = createSearchBuilder();
        itemSearch.and("itemType", itemSearch.entity().getItemType(), SearchCriteria.Op.EQ);
        itemSearch.and("itemId", itemSearch.entity().getItemId(), SearchCriteria.Op.EQ);
        itemSearch.done();

        enabledByItemSearch = createSearchBuilder();
        enabledByItemSearch.and("bootGroupId", enabledByItemSearch.entity().getBootGroupId(), SearchCriteria.Op.EQ);
        enabledByItemSearch.and("itemType", enabledByItemSearch.entity().getItemType(), SearchCriteria.Op.EQ);
        enabledByItemSearch.and("itemId", enabledByItemSearch.entity().getItemId(), SearchCriteria.Op.EQ);
        enabledByItemSearch.and("enabled", enabledByItemSearch.entity().isEnabled(), SearchCriteria.Op.EQ);
        enabledByItemSearch.done();

        byItemSearch = createSearchBuilder();
        byItemSearch.and("bootGroupId", byItemSearch.entity().getBootGroupId(), SearchCriteria.Op.EQ);
        byItemSearch.and("itemType", byItemSearch.entity().getItemType(), SearchCriteria.Op.EQ);
        byItemSearch.and("itemId", byItemSearch.entity().getItemId(), SearchCriteria.Op.EQ);
        byItemSearch.done();
    }

    @Override
    public Pair<List<InstanceBootGroupReadinessRuleVO>, Integer> searchAndCountByBootGroupId(long bootGroupId,
            Long id,
            InstanceBootGroupMember.MemberType itemType,
            Long itemId,
            InstanceBootGroupReadinessRule.RuleType ruleType,
            String keyword,
            Long startIndex,
            Long pageSize) {
        SearchBuilder<InstanceBootGroupReadinessRuleVO> sb = createSearchBuilder();
        sb.and("bootGroupId", sb.entity().getBootGroupId(), SearchCriteria.Op.EQ);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("itemType", sb.entity().getItemType(), SearchCriteria.Op.EQ);
        sb.and("itemId", sb.entity().getItemId(), SearchCriteria.Op.EQ);
        sb.and("ruleType", sb.entity().getRuleType(), SearchCriteria.Op.EQ);
        sb.and("keyword", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.done();

        SearchCriteria<InstanceBootGroupReadinessRuleVO> sc = sb.create();
        sc.setParameters("bootGroupId", bootGroupId);
        if (id != null) {
            sc.setParameters("id", id);
        }
        if (itemType != null) {
            sc.setParameters("itemType", itemType);
        }
        if (itemId != null) {
            sc.setParameters("itemId", itemId);
        }
        if (ruleType != null) {
            sc.setParameters("ruleType", ruleType);
        }
        if (keyword != null) {
            sc.setParameters("keyword", "%" + keyword + "%");
        }

        Filter searchFilter = new Filter(InstanceBootGroupReadinessRuleVO.class, "id", true, startIndex, pageSize);
        return searchAndCount(sc, searchFilter);
    }

    @Override
    public List<InstanceBootGroupReadinessRuleVO> listEnabledByItem(long bootGroupId, InstanceBootGroupMember.MemberType itemType, long itemId) {
        SearchCriteria<InstanceBootGroupReadinessRuleVO> sc = enabledByItemSearch.create();
        sc.setParameters("bootGroupId", bootGroupId);
        sc.setParameters("itemType", itemType);
        sc.setParameters("itemId", itemId);
        sc.setParameters("enabled", true);
        return listBy(sc);
    }

    @Override
    public List<InstanceBootGroupReadinessRuleVO> listByItem(long bootGroupId, InstanceBootGroupMember.MemberType itemType, long itemId) {
        SearchCriteria<InstanceBootGroupReadinessRuleVO> sc = byItemSearch.create();
        sc.setParameters("bootGroupId", bootGroupId);
        sc.setParameters("itemType", itemType);
        sc.setParameters("itemId", itemId);
        return listBy(sc);
    }
}
