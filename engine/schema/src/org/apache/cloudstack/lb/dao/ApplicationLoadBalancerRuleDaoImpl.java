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

package org.apache.cloudstack.lb.dao;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.lb.ApplicationLoadBalancerRuleVO;

import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.State;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.net.Ip;

@Component
@Local(value = {ApplicationLoadBalancerRuleDao.class})
public class ApplicationLoadBalancerRuleDaoImpl extends GenericDaoBase<ApplicationLoadBalancerRuleVO, Long> implements ApplicationLoadBalancerRuleDao {
    protected final SearchBuilder<ApplicationLoadBalancerRuleVO> AllFieldsSearch;
    final GenericSearchBuilder<ApplicationLoadBalancerRuleVO, String> listIps;
    final GenericSearchBuilder<ApplicationLoadBalancerRuleVO, Long> CountBy;
    protected final SearchBuilder<ApplicationLoadBalancerRuleVO> NotRevokedSearch;
    final GenericSearchBuilder<ApplicationLoadBalancerRuleVO, Long> CountNotRevoked;
    final GenericSearchBuilder<ApplicationLoadBalancerRuleVO, Long> CountActive;

    protected ApplicationLoadBalancerRuleDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("sourceIp", AllFieldsSearch.entity().getSourceIp(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("sourceIpNetworkId", AllFieldsSearch.entity().getSourceIpNetworkId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("networkId", AllFieldsSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("scheme", AllFieldsSearch.entity().getScheme(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();

        listIps = createSearchBuilder(String.class);
        listIps.select(null, Func.DISTINCT, listIps.entity().getSourceIp());
        listIps.and("sourceIpNetworkId", listIps.entity().getSourceIpNetworkId(), Op.EQ);
        listIps.and("scheme", listIps.entity().getScheme(), Op.EQ);
        listIps.done();

        CountBy = createSearchBuilder(Long.class);
        CountBy.select(null, Func.COUNT, CountBy.entity().getId());
        CountBy.and("sourceIp", CountBy.entity().getSourceIp(), Op.EQ);
        CountBy.and("sourceIpNetworkId", CountBy.entity().getSourceIpNetworkId(), Op.EQ);
        CountBy.done();

        NotRevokedSearch = createSearchBuilder();
        NotRevokedSearch.and("sourceIp", NotRevokedSearch.entity().getSourceIp(), SearchCriteria.Op.EQ);
        NotRevokedSearch.and("sourceIpNetworkId", NotRevokedSearch.entity().getSourceIpNetworkId(), SearchCriteria.Op.EQ);
        NotRevokedSearch.and("state", NotRevokedSearch.entity().getState(), SearchCriteria.Op.NEQ);
        NotRevokedSearch.done();

        CountNotRevoked = createSearchBuilder(Long.class);
        CountNotRevoked.select(null, Func.COUNT, CountNotRevoked.entity().getId());
        CountNotRevoked.and("sourceIp", CountNotRevoked.entity().getSourceIp(), Op.EQ);
        CountNotRevoked.and("state", CountNotRevoked.entity().getState(), Op.NEQ);
        CountNotRevoked.and("sourceIpNetworkId", CountNotRevoked.entity().getSourceIpNetworkId(), Op.EQ);
        CountNotRevoked.done();

        CountActive = createSearchBuilder(Long.class);
        CountActive.select(null, Func.COUNT, CountActive.entity().getId());
        CountActive.and("sourceIp", CountActive.entity().getSourceIp(), Op.EQ);
        CountActive.and("state", CountActive.entity().getState(), Op.EQ);
        CountActive.and("sourceIpNetworkId", CountActive.entity().getSourceIpNetworkId(), Op.EQ);
        CountActive.done();
    }

    @Override
    public List<ApplicationLoadBalancerRuleVO> listBySrcIpSrcNtwkId(Ip sourceIp, long sourceNetworkId) {
        SearchCriteria<ApplicationLoadBalancerRuleVO> sc = AllFieldsSearch.create();
        sc.setParameters("sourceIp", sourceIp);
        sc.setParameters("sourceIpNetworkId", sourceNetworkId);
        return listBy(sc);
    }

    @Override
    public List<String> listLbIpsBySourceIpNetworkId(long sourceIpNetworkId) {
        SearchCriteria<String> sc = listIps.create();
        sc.setParameters("sourceIpNetworkId", sourceIpNetworkId);
        return customSearch(sc, null);
    }

    @Override
    public long countBySourceIp(Ip sourceIp, long sourceIpNetworkId) {
        SearchCriteria<Long> sc = CountBy.create();
        sc.setParameters("sourceIp", sourceIp);
        sc.setParameters("sourceIpNetworkId", sourceIpNetworkId);
        List<Long> results = customSearch(sc, null);
        return results.get(0);
    }

    @Override
    public List<ApplicationLoadBalancerRuleVO> listBySourceIpAndNotRevoked(Ip sourceIp, long sourceNetworkId) {
        SearchCriteria<ApplicationLoadBalancerRuleVO> sc = NotRevokedSearch.create();
        sc.setParameters("sourceIp", sourceIp);
        sc.setParameters("sourceIpNetworkId", sourceNetworkId);
        sc.setParameters("state", FirewallRule.State.Revoke);
        return listBy(sc);
    }

    @Override
    public List<String> listLbIpsBySourceIpNetworkIdAndScheme(long sourceIpNetworkId, Scheme scheme) {
        SearchCriteria<String> sc = listIps.create();
        sc.setParameters("sourceIpNetworkId", sourceIpNetworkId);
        sc.setParameters("scheme", scheme);
        return customSearch(sc, null);
    }

    @Override
    public long countBySourceIpAndNotRevoked(Ip sourceIp, long sourceIpNetworkId) {
        SearchCriteria<Long> sc = CountNotRevoked.create();
        sc.setParameters("sourceIp", sourceIp);
        sc.setParameters("sourceIpNetworkId", sourceIpNetworkId);
        sc.setParameters("state", State.Revoke);
        List<Long> results = customSearch(sc, null);
        return results.get(0);
    }

    @Override
    public long countActiveBySourceIp(Ip sourceIp, long sourceIpNetworkId) {
        SearchCriteria<Long> sc = CountActive.create();
        sc.setParameters("sourceIp", sourceIp);
        sc.setParameters("sourceIpNetworkId", sourceIpNetworkId);
        sc.setParameters("state", State.Active);
        List<Long> results = customSearch(sc, null);
        return results.get(0);
    }

}
