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
    private static final String ID = "id";
    private static final String UUID = "uuid";
    private static final String SECURITY_GROUP_ID = "security_group_id";
    private static final String RULE_TYPE = "rule_type";
    private static final String RULE_TARGET = "rule_target";
    private static final String ETHER_TYPE = "ether_type";
    private static final String DEFAULT_RULE = "default_rule";
    protected final SearchBuilder<TungstenSecurityGroupRuleVO> allFieldsSearch;

    protected TungstenSecurityGroupRuleDaoImpl() {
        allFieldsSearch = createSearchBuilder();
        allFieldsSearch.and(ID, allFieldsSearch.entity().getId(), SearchCriteria.Op.EQ);
        allFieldsSearch.and(UUID, allFieldsSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        allFieldsSearch.and(SECURITY_GROUP_ID, allFieldsSearch.entity().getSecurityGroupId(), SearchCriteria.Op.EQ);
        allFieldsSearch.and(RULE_TYPE, allFieldsSearch.entity().getRuleType(), SearchCriteria.Op.EQ);
        allFieldsSearch.and(RULE_TARGET, allFieldsSearch.entity().getRuleTarget(), SearchCriteria.Op.EQ);
        allFieldsSearch.and(ETHER_TYPE, allFieldsSearch.entity().getEtherType(), SearchCriteria.Op.EQ);
        allFieldsSearch.and(DEFAULT_RULE, allFieldsSearch.entity().isDefaultRule(), SearchCriteria.Op.EQ);
        allFieldsSearch.done();
    }

    @Override
    public TungstenSecurityGroupRuleVO findDefaultSecurityRule(final long securityGroupId, final String ruleType
    , final String etherType) {
        SearchCriteria<TungstenSecurityGroupRuleVO> searchCriteria = allFieldsSearch.create();
        searchCriteria.setParameters(SECURITY_GROUP_ID, securityGroupId);
        searchCriteria.setParameters(RULE_TYPE, ruleType);
        searchCriteria.setParameters(ETHER_TYPE, etherType);
        searchCriteria.setParameters(DEFAULT_RULE, true);
        return findOneBy(searchCriteria);
    }

    @Override
    public TungstenSecurityGroupRuleVO findBySecurityGroupAndRuleTypeAndRuleTarget(final long securityGroupId,
        final String ruleType, final String ruleTarget) {
        SearchCriteria<TungstenSecurityGroupRuleVO> searchCriteria = allFieldsSearch.create();
        searchCriteria.setParameters(SECURITY_GROUP_ID, securityGroupId);
        searchCriteria.setParameters(RULE_TYPE, ruleType);
        searchCriteria.setParameters(RULE_TARGET, ruleTarget);
        return findOneBy(searchCriteria);
    }

    @Override
    public List<TungstenSecurityGroupRuleVO> listByRuleTarget(final String ruleTarget) {
        SearchCriteria<TungstenSecurityGroupRuleVO> searchCriteria = allFieldsSearch.create();
        searchCriteria.setParameters(RULE_TARGET, ruleTarget);
        return listBy(searchCriteria);
    }
}
