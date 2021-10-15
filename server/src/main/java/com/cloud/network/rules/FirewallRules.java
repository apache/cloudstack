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

package com.cloud.network.rules;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.network.topology.NetworkTopologyVisitor;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
import com.cloud.network.lb.LoadBalancingRule.LbSslCert;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.utils.net.Ip;

public class FirewallRules extends RuleApplier {

    private final List<? extends FirewallRule> _rules;
    private List<LoadBalancingRule> _loadbalancingRules;

    private Purpose _purpose;

    public FirewallRules(final Network network, final List<? extends FirewallRule> rules) {
        super(network);
        _rules = rules;
    }

    @Override
    public boolean accept(final NetworkTopologyVisitor visitor, final VirtualRouter router) throws ResourceUnavailableException {
        _router = router;

        _purpose = _rules.get(0).getPurpose();

        if (_purpose == Purpose.LoadBalancing) {
            LoadBalancerDao loadBalancerDao = visitor.getVirtualNetworkApplianceFactory().getLoadBalancerDao();
            // for load balancer we have to resend all lb rules for the network or vpc
            final List<LoadBalancerVO> lbs = loadBalancerDao.listByNetworkIdOrVpcIdAndScheme(_network.getId(), _network.getVpcId(), Scheme.Public);

            _loadbalancingRules = new ArrayList<LoadBalancingRule>();

            LoadBalancingRulesManager lbMgr = visitor.getVirtualNetworkApplianceFactory().getLbMgr();
            NetworkModel networkModel = visitor.getVirtualNetworkApplianceFactory().getNetworkModel();

            for (final LoadBalancerVO lb : lbs) {
                final List<LbDestination> dstList = lbMgr.getExistingDestinations(lb.getId());
                final List<LbStickinessPolicy> policyList = lbMgr.getStickinessPolicies(lb.getId());
                final List<LbHealthCheckPolicy> hcPolicyList = lbMgr.getHealthCheckPolicies(lb.getId());
                final LbSslCert sslCert = lbMgr.getLbSslCert(lb.getId());
                final Ip sourceIp = networkModel.getPublicIpAddress(lb.getSourceIpAddressId()).getAddress();
                final LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, policyList, hcPolicyList, sourceIp, sslCert, lb.getLbProtocol());

                _loadbalancingRules.add(loadBalancing);
            }
        }

        return visitor.visit(this);
    }

    public List<? extends FirewallRule> getRules() {
        return _rules;
    }

    public List<LoadBalancingRule> getLoadbalancingRules() {
        return _loadbalancingRules;
    }

    public Purpose getPurpose() {
        return _purpose;
    }
}
