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

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.cloud.network.rules.FirewallRule;
import com.cloud.utils.db.JoinBuilder;
import org.springframework.stereotype.Component;

import com.cloud.network.rules.FirewallRule.State;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
public class LoadBalancerDaoImpl extends GenericDaoBase<LoadBalancerVO, Long> implements LoadBalancerDao {
    private final SearchBuilder<LoadBalancerVO> ListByIp;
    protected final SearchBuilder<LoadBalancerVO> TransitionStateSearch;
    private SearchBuilder<LoadBalancerVO> ListByVpcId;

    @Inject
    protected FirewallRulesCidrsDao _portForwardingRulesCidrsDao;
    @Inject
    LoadBalancerVMMapDao _loadBalancerVMMapDao;
    @Inject
    NetworkDao _networkDao;

    @PostConstruct
    void init() {
        ListByVpcId = createSearchBuilder();
        ListByVpcId.and("networkId", ListByVpcId.entity().getNetworkId(), SearchCriteria.Op.EQ);
        ListByVpcId.and("scheme", ListByVpcId.entity().getScheme(), SearchCriteria.Op.EQ);
        SearchBuilder<NetworkVO> networkSearch = _networkDao.createSearchBuilder();
        networkSearch.and("vpcId", networkSearch.entity().getVpcId(), SearchCriteria.Op.EQ);
        ListByVpcId.join("network", networkSearch, networkSearch.entity().getId(), ListByVpcId.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
        ListByVpcId.done();
    }

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
    public List<LoadBalancerVO> listByVpcIdAndScheme(long vpcId, Scheme scheme) {
        SearchCriteria<LoadBalancerVO> sc = ListByVpcId.create();
        sc.setJoinParameters("network", "vpcId", vpcId);
        sc.setParameters("scheme", scheme);
        return listBy(sc);
    }

    @Override
    public List<LoadBalancerVO> listByNetworkIdOrVpcIdAndScheme(long networkId, Long vpcId, Scheme scheme) {
        if (vpcId != null) {
            return listByVpcIdAndScheme(vpcId, scheme);
        } else {
            return listByNetworkIdAndScheme(networkId, scheme);
        }
    }

    @Override
    public List<LoadBalancerVO> listInTransitionStateByNetworkIdAndScheme(long networkId, Scheme scheme) {
        SearchCriteria<LoadBalancerVO> sc = TransitionStateSearch.create();
        sc.setParameters("networkId", networkId);
        sc.setParameters("state", State.Add.toString(), State.Revoke.toString());
        sc.setParameters("scheme", scheme);
        return listBy(sc);
    }

    @Override
    public boolean isLoadBalancerRulesMappedToVmGuestIp(long instanceId, String instanceIp, long networkId)
    {
        SearchBuilder<LoadBalancerVMMapVO> lbVmMapSearch = _loadBalancerVMMapDao.createSearchBuilder();
        lbVmMapSearch.and("instanceIp", lbVmMapSearch.entity().getInstanceIp(),SearchCriteria.Op.EQ);
        lbVmMapSearch.and("instanceId", lbVmMapSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);

        SearchBuilder<LoadBalancerVO> firewallRuleIdSearch = createSearchBuilder();
        firewallRuleIdSearch.selectFields(firewallRuleIdSearch.entity().getId());
        firewallRuleIdSearch.and("networkId",firewallRuleIdSearch.entity().getNetworkId(),Op.EQ);
        firewallRuleIdSearch.and("purpose",firewallRuleIdSearch.entity().getPurpose(),Op.EQ);
        firewallRuleIdSearch.and("state",firewallRuleIdSearch.entity().getState(),Op.NEQ);
        firewallRuleIdSearch.join("LoadBalancerRuleList", lbVmMapSearch, lbVmMapSearch.entity().getLoadBalancerId(), firewallRuleIdSearch.entity().getId(), JoinBuilder.JoinType.INNER);

        firewallRuleIdSearch.done();
        lbVmMapSearch.done();

        SearchCriteria<LoadBalancerVO> sc = firewallRuleIdSearch.create();
        sc.setParameters("state", State.Revoke);
        sc.setParameters("networkId", networkId);
        sc.setParameters("purpose", FirewallRule.Purpose.LoadBalancing);

        sc.setJoinParameters("LoadBalancerRuleList", "instanceIp", instanceIp);
        sc.setJoinParameters("LoadBalancerRuleList", "instanceId", instanceId);

        List<LoadBalancerVO> lbRuleList = customSearch(sc, null);

        if(lbRuleList == null || lbRuleList.size() > 0) {
            return true;
        }

        return false;
    }



}
