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

import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.network.security.SecurityGroupRulesVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class SecurityGroupRulesDaoImpl extends GenericDaoBase<SecurityGroupRulesVO, Long> implements SecurityGroupRulesDao {
    private SearchBuilder<SecurityGroupRulesVO> AccountGroupNameSearch;
    private SearchBuilder<SecurityGroupRulesVO> AccountSearch;
    private SearchBuilder<SecurityGroupRulesVO> GroupSearch;

    protected SecurityGroupRulesDaoImpl() {
        AccountGroupNameSearch = createSearchBuilder();
        AccountGroupNameSearch.and("accountId", AccountGroupNameSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountGroupNameSearch.and("name", AccountGroupNameSearch.entity().getName(), SearchCriteria.Op.EQ);
        AccountGroupNameSearch.done();

        AccountSearch = createSearchBuilder();
        AccountSearch.and("accountId", AccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.done();

        GroupSearch = createSearchBuilder();
        GroupSearch.and("groupId", GroupSearch.entity().getId(), SearchCriteria.Op.EQ);
        GroupSearch.done();

    }

    @Override
    public List<SecurityGroupRulesVO> listSecurityGroupRules() {
        Filter searchFilter = new Filter(SecurityGroupRulesVO.class, "id", true, null, null);
        return listAll(searchFilter);
    }

    @Override
    public List<SecurityGroupRulesVO> listSecurityGroupRules(long accountId, String groupName) {
        Filter searchFilter = new Filter(SecurityGroupRulesVO.class, "id", true, null, null);

        SearchCriteria<SecurityGroupRulesVO> sc = AccountGroupNameSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("name", groupName);

        return listBy(sc, searchFilter);
    }

    @Override
    public List<SecurityGroupRulesVO> listSecurityGroupRules(long accountId) {
        Filter searchFilter = new Filter(SecurityGroupRulesVO.class, "id", true, null, null);
        SearchCriteria<SecurityGroupRulesVO> sc = AccountSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc, searchFilter);
    }

    @Override
    public List<SecurityGroupRulesVO> listSecurityRulesByGroupId(long groupId) {
        Filter searchFilter = new Filter(SecurityGroupRulesVO.class, "id", true, null, null);
        SearchCriteria<SecurityGroupRulesVO> sc = GroupSearch.create();
        sc.setParameters("groupId", groupId);
        return listBy(sc, searchFilter);
    }

}
