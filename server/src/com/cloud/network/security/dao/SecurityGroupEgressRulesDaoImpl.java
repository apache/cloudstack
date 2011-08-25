/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.network.security.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.network.security.SecurityGroupEgressRulesVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={SecurityGroupEgressRulesDao.class})
public class SecurityGroupEgressRulesDaoImpl extends GenericDaoBase<SecurityGroupEgressRulesVO, Long> implements SecurityGroupEgressRulesDao {
    private SearchBuilder<SecurityGroupEgressRulesVO> AccountGroupNameSearch;
    private SearchBuilder<SecurityGroupEgressRulesVO> AccountSearch;
    private SearchBuilder<SecurityGroupEgressRulesVO> GroupSearch;

    protected SecurityGroupEgressRulesDaoImpl() {
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
    public List<SecurityGroupEgressRulesVO> listSecurityGroupEgressRules() {
        Filter searchFilter = new Filter(SecurityGroupEgressRulesVO.class, "id", true, null, null);
        return listAll(searchFilter);
    }

    @Override
    public List<SecurityGroupEgressRulesVO> listSecurityGroupEgressRules(long accountId, String groupName) {
        Filter searchFilter = new Filter(SecurityGroupEgressRulesVO.class, "id", true, null, null);

        SearchCriteria<SecurityGroupEgressRulesVO> sc = AccountGroupNameSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("name", groupName);
        return listBy(sc, searchFilter);
    }

    @Override
    public List<SecurityGroupEgressRulesVO> listSecurityGroupEgressRules(long accountId) {
        Filter searchFilter = new Filter(SecurityGroupEgressRulesVO.class, "id", true, null, null);
        SearchCriteria<SecurityGroupEgressRulesVO> sc = AccountSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc, searchFilter);
    }
    
    
    @Override
    public List<SecurityGroupEgressRulesVO> listSecurityEgressRulesByGroupId(long groupId) {
        Filter searchFilter = new Filter(SecurityGroupEgressRulesVO.class, "id", true, null, null);
        SearchCriteria<SecurityGroupEgressRulesVO> sc = GroupSearch.create();
        sc.setParameters("groupId", groupId);
        return listBy(sc, searchFilter);
    }
}
