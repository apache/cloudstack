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
package com.cloud.network.security.dao;

import com.cloud.network.security.TungstenSecurityGroupRuleVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TungstenSecurityGroupRuleDaoImpl extends GenericDaoBase<TungstenSecurityGroupRuleVO, Long>
    implements TungstenSecurityGroupRuleDao {
    protected final SearchBuilder<TungstenSecurityGroupRuleVO> AllFieldsSearch;

    protected TungstenSecurityGroupRuleDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("uuid", AllFieldsSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("security_group_id", AllFieldsSearch.entity().getSecurityGroupId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("rule_type", AllFieldsSearch.entity().getRuleType(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("rule_target", AllFieldsSearch.entity().getRuleTarget(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("ether_type", AllFieldsSearch.entity().getEtherType(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("default_rule", AllFieldsSearch.entity().isDefaultRule(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public TungstenSecurityGroupRuleVO findDefaultSecurityRule(final long securityGroupId, final String ruleType
    , final String etherType) {
        SearchCriteria<TungstenSecurityGroupRuleVO> searchCriteria = AllFieldsSearch.create();
        searchCriteria.setParameters("security_group_id", securityGroupId);
        searchCriteria.setParameters("rule_type", ruleType);
        searchCriteria.setParameters("ether_type", etherType);
        searchCriteria.setParameters("default_rule", true);
        return findOneBy(searchCriteria);
    }

    @Override
    public TungstenSecurityGroupRuleVO findBySecurityGroupAndRuleTypeAndRuleTarget(final long securityGroupId,
        final String ruleType, final String ruleTarget) {
        SearchCriteria<TungstenSecurityGroupRuleVO> searchCriteria = AllFieldsSearch.create();
        searchCriteria.setParameters("security_group_id", securityGroupId);
        searchCriteria.setParameters("rule_type", ruleType);
        searchCriteria.setParameters("rule_target", ruleTarget);
        return findOneBy(searchCriteria);
    }

    @Override
    public List<TungstenSecurityGroupRuleVO> listByRuleTarget(final String ruleTarget) {
        SearchCriteria<TungstenSecurityGroupRuleVO> searchCriteria = AllFieldsSearch.create();
        searchCriteria.setParameters("rule_target", ruleTarget);
        return listBy(searchCriteria);
    }
}
