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
package com.cloud.network.rules.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.network.rules.FirewallRule.State;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.net.Ip;

@Local(value=PortForwardingRulesDao.class)
public class PortForwardingRuleDaoImpl extends GenericDaoBase<PortForwardingRuleVO, Long> implements PortForwardingRulesDao {

    protected final SearchBuilder<PortForwardingRuleVO> AllFieldsSearch;
    protected final SearchBuilder<PortForwardingRuleVO> ApplicationSearch; 
    
    protected PortForwardingRuleDaoImpl() {
        super();
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.done();
        
        ApplicationSearch = createSearchBuilder();
        ApplicationSearch.and("ip", ApplicationSearch.entity().getSourceIpAddress(), Op.EQ);
        ApplicationSearch.and("state", ApplicationSearch.entity().getState(), Op.NEQ);
    }

    @Override
    public List<PortForwardingRuleVO> listForApplication(Ip ip) {
        SearchCriteria<PortForwardingRuleVO> sc = ApplicationSearch.create();
        sc.setParameters("ip", ip);
        sc.setParameters("state", State.Staged);
        
        return listBy(sc, null);
    }
}
