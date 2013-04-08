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

import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloud.network.rules.FirewallRule.State;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@Local(value = { LoadBalancerDao.class })
public class LoadBalancerDaoImpl extends GenericDaoBase<LoadBalancerVO, Long> implements LoadBalancerDao {
    private final SearchBuilder<LoadBalancerVO> ListByIp;
    protected final SearchBuilder<LoadBalancerVO> TransitionStateSearch;

    @Inject protected FirewallRulesCidrsDao _portForwardingRulesCidrsDao;

    protected LoadBalancerDaoImpl() {
        ListByIp = createSearchBuilder();
        ListByIp.and("ipAddressId", ListByIp.entity().getSourceIpAddressId(), SearchCriteria.Op.EQ);
        ListByIp.and("networkId", ListByIp.entity().getNetworkId(), SearchCriteria.Op.EQ);
        ListByIp.and("scheme", ListByIp.entity().getScheme(), SearchCriteria.Op.EQ);
        ListByIp.done();

        TransitionStateSearch = createSearchBuilder();
        TransitionStateSearch.and("networkId", TransitionStateSearch.entity().getNetworkId(), Op.EQ);
        TransitionStateSearch.and("state", TransitionStateSearch.entity().getState(), Op.IN);
        TransitionStateSearch.and("scheme", TransitionStateSearch.entity().getScheme(), Op.EQ);
        TransitionStateSearch.done();
    }
    
    @Override
    public List<LoadBalancerVO> listByIpAddress(long ipAddressId) {
        SearchCriteria<LoadBalancerVO> sc = ListByIp.create();
        sc.setParameters("ipAddressId", ipAddressId);
        return listBy(sc);
    }

    @Override
    public List<LoadBalancerVO> listByNetworkIdAndScheme(long networkId, Scheme scheme) {
        SearchCriteria<LoadBalancerVO> sc = ListByIp.create();
        sc.setParameters("networkId", networkId);
        sc.setParameters("scheme", scheme);
        return listBy(sc);
    }

    @Override
    public List<LoadBalancerVO> listInTransitionStateByNetworkIdAndScheme(long networkId, Scheme scheme) {
        SearchCriteria<LoadBalancerVO> sc = TransitionStateSearch.create();
        sc.setParameters("networkId", networkId);
        sc.setParameters("state", State.Add.toString(), State.Revoke.toString());
        sc.setParameters("scheme", scheme);
        return listBy(sc);
    }

}
