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

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FirewallRulesDcidrsDaoImpl extends GenericDaoBase<FirewallRulesDestCidrsVO, Long> implements FirewallRulesDcidrsDao {

    protected final SearchBuilder<FirewallRulesDestCidrsVO> cidrsSearch;

    protected FirewallRulesDcidrsDaoImpl(){
        cidrsSearch = createSearchBuilder();
        cidrsSearch.and("firewallRuleId", cidrsSearch.entity().getFirewallRuleId(), SearchCriteria.Op.EQ);
        cidrsSearch.done();

    }

    @Override
    @DB
     public List<String> getDestCidrs(long firewallRuleId){
        SearchCriteria<FirewallRulesDestCidrsVO> sc =cidrsSearch.create();
        sc.setParameters("firewallRuleId", firewallRuleId);

        List<FirewallRulesDestCidrsVO> results = search(sc, null);

        List<String> cidrs = new ArrayList<String>(results.size());
        for (FirewallRulesDestCidrsVO result : results) {
            cidrs.add(result.getCidr());
        }

        return cidrs;
    }

    @Override
    @DB
    public void persist(final long firewallRuleId, final List<String> destCidrs){
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                for(String cidr: destCidrs){
                    FirewallRulesDestCidrsVO vo = new FirewallRulesDestCidrsVO(firewallRuleId, cidr);
                    persist(vo);
                }
            }
        });
    }


}
