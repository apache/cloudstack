/*  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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

package com.cloud.network.dao;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.network.FirewallRulesCidrsVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;


@Local(value=FirewallRulesCidrsDaoImpl.class)
public class FirewallRulesCidrsDaoImpl extends GenericDaoBase<FirewallRulesCidrsVO, Long> implements FirewallRulesCidrsDao {
    private static final Logger s_logger = Logger.getLogger(FirewallRulesCidrsDaoImpl.class);
    protected final SearchBuilder<FirewallRulesCidrsVO> CidrsSearch;
    
    protected FirewallRulesCidrsDaoImpl() {
        CidrsSearch = createSearchBuilder();
        CidrsSearch.and("firewallRuleId", CidrsSearch.entity().getFirewallRuleId(), SearchCriteria.Op.EQ);
        CidrsSearch.done();        
    }

    @Override
    public List<String> getSourceCidrs(long firewallRuleId) {
        SearchCriteria sc = CidrsSearch.create();
        sc.setParameters("firewallRuleId", firewallRuleId);
        
        List<FirewallRulesCidrsVO> results = search(sc, null);
        List<String> hostTags = new ArrayList<String>(results.size());
        for (FirewallRulesCidrsVO result : results) {
            hostTags.add(result.getCidr());
        }

        return hostTags;
    }
    
    @Override
    public void persist(long firewallRuleId, List<String> sourceCidrs) {
        Transaction txn = Transaction.currentTxn();

        txn.start();
        for (String tag : sourceCidrs) {
            s_logger.info("Saving cidrs " + tag);
            FirewallRulesCidrsVO vo = new FirewallRulesCidrsVO(firewallRuleId, tag);
            persist(vo);
        }
        txn.commit();
    }
}
