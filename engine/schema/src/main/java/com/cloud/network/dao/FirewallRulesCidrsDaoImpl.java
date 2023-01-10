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
package com.cloud.network.dao;

import java.util.ArrayList;
import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class FirewallRulesCidrsDaoImpl extends GenericDaoBase<FirewallRulesCidrsVO, Long> implements FirewallRulesCidrsDao {
    protected final SearchBuilder<FirewallRulesCidrsVO> CidrsSearch;

    protected FirewallRulesCidrsDaoImpl() {
        CidrsSearch = createSearchBuilder();
        CidrsSearch.and("firewallRuleId", CidrsSearch.entity().getFirewallRuleId(), SearchCriteria.Op.EQ);
        CidrsSearch.done();
    }

    @Override
    @DB
    public List<String> getSourceCidrs(long firewallRuleId) {
        SearchCriteria<FirewallRulesCidrsVO> sc = CidrsSearch.create();
        sc.setParameters("firewallRuleId", firewallRuleId);

        List<FirewallRulesCidrsVO> results = search(sc, null);
        List<String> cidrs = new ArrayList<String>(results.size());
        for (FirewallRulesCidrsVO result : results) {
            cidrs.add(result.getCidr());
        }

        return cidrs;
    }

    @Override @DB
    public List<FirewallRulesCidrsVO> listByFirewallRuleId(long firewallRuleId) {
        SearchCriteria<FirewallRulesCidrsVO> sc = CidrsSearch.create();
        sc.setParameters("firewallRuleId", firewallRuleId);

        List<FirewallRulesCidrsVO> results = search(sc, null);

        return results;
    }

    @Override
    @DB
    public void persist(long firewallRuleId, List<String> sourceCidrs) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();

        txn.start();
        for (String cidr : sourceCidrs) {
            FirewallRulesCidrsVO vo = new FirewallRulesCidrsVO(firewallRuleId, cidr);
            persist(vo);
        }
        txn.commit();
    }
}
