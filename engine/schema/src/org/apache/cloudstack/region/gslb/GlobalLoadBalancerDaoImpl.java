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

package org.apache.cloudstack.region.gslb;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.util.List;

@Component
@Local(value={GlobalLoadBalancerRuleDao.class})
public class GlobalLoadBalancerDaoImpl extends GenericDaoBase<GlobalLoadBalancerRuleVO, Long> implements GlobalLoadBalancerRuleDao {

    private final SearchBuilder<GlobalLoadBalancerRuleVO> listByDomainSearch;
    private final SearchBuilder<GlobalLoadBalancerRuleVO> listByRegionIDSearch;
    private final SearchBuilder<GlobalLoadBalancerRuleVO> AccountIdSearch;

    public GlobalLoadBalancerDaoImpl() {
        listByDomainSearch = createSearchBuilder();
        listByDomainSearch.and("gslbDomain", listByDomainSearch.entity().getGslbDomain(), SearchCriteria.Op.EQ);
        listByDomainSearch.done();

        AccountIdSearch = createSearchBuilder();
        AccountIdSearch.and("account", AccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdSearch.done();

        listByRegionIDSearch = createSearchBuilder();
        listByRegionIDSearch.and("region", listByRegionIDSearch.entity().getRegion(), SearchCriteria.Op.EQ);
        listByRegionIDSearch.done();
    }

    @Override
    public List<GlobalLoadBalancerRuleVO> listByRegionId(int regionId) {
        SearchCriteria<GlobalLoadBalancerRuleVO> sc = listByRegionIDSearch.create();
        sc.setParameters("region", regionId);
        return listBy(sc);
    }

    @Override
    public List<GlobalLoadBalancerRuleVO> listByAccount(long accountId) {
        SearchCriteria<GlobalLoadBalancerRuleVO> sc = AccountIdSearch.create();
        sc.setParameters("account", accountId);
        return listBy(sc, null);
    }

    @Override
    public GlobalLoadBalancerRuleVO findByDomainName(String domainName) {
        SearchCriteria<GlobalLoadBalancerRuleVO> sc = listByDomainSearch.create();
        sc.setParameters("gslbDomain", domainName);
        return findOneBy(sc);
    }
}
