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

import java.util.List;

import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.Config;
import com.cloud.dc.DataCenterVO;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
import com.cloud.network.lb.LoadBalancingRule.LbSslCert;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.network.topology.NetworkTopologyVisitor;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.net.Ip;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;

public class LoadBalancingRules extends RuleApplier {

    private final List<LoadBalancingRule> rules;

    public LoadBalancingRules(final Network network, final List<LoadBalancingRule> rules) {
        super(network);
        this.rules = rules;
    }

    @Override
    public boolean accept(final NetworkTopologyVisitor visitor, final VirtualRouter router) throws ResourceUnavailableException {
        this.router = router;

        // For load balancer we have to resend all lb rules for the network
        final List<LoadBalancerVO> lbs = loadBalancerDao.listByNetworkIdAndScheme(network.getId(), Scheme.Public);

        // We are cleaning it before because all the rules have to be sent to
        // the router.
        rules.clear();
        for (final LoadBalancerVO lb : lbs) {
            final List<LbDestination> dstList = lbMgr.getExistingDestinations(lb.getId());
            final List<LbStickinessPolicy> policyList = lbMgr.getStickinessPolicies(lb.getId());
            final List<LbHealthCheckPolicy> hcPolicyList = lbMgr.getHealthCheckPolicies(lb.getId());
            final LbSslCert sslCert = lbMgr.getLbSslCert(lb.getId());
            final Ip sourceIp = networkModel.getPublicIpAddress(lb.getSourceIpAddressId()).getAddress();
            final LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, policyList, hcPolicyList, sourceIp, sslCert, lb.getLbProtocol());

            rules.add(loadBalancing);
        }

        return visitor.visit(this);
    }

    public List<LoadBalancingRule> getRules() {
        return rules;
    }

    public void createApplyLoadBalancingRulesCommands(final List<LoadBalancingRule> rules, final VirtualRouter router, final Commands cmds, final long guestNetworkId) {
        final LoadBalancerTO[] lbs = new LoadBalancerTO[rules.size()];
        int i = 0;
        // We don't support VR to be inline currently
        final boolean inline = false;
        for (final LoadBalancingRule rule : rules) {
            final boolean revoked = rule.getState().equals(FirewallRule.State.Revoke);
            final String protocol = rule.getProtocol();
            final String algorithm = rule.getAlgorithm();
            final String uuid = rule.getUuid();

            final String srcIp = rule.getSourceIp().addr();
            final int srcPort = rule.getSourcePortStart();
            final List<LbDestination> destinations = rule.getDestinations();
            final List<LbStickinessPolicy> stickinessPolicies = rule.getStickinessPolicies();
            final LoadBalancerTO lb = new LoadBalancerTO(uuid, srcIp, srcPort, protocol, algorithm, revoked, false, inline, destinations, stickinessPolicies);
            lbs[i++] = lb;
        }
        String routerPublicIp = null;

        if (router instanceof DomainRouterVO) {
            final DomainRouterVO domr = routerDao.findById(router.getId());
            routerPublicIp = domr.getPublicIpAddress();
        }

        final Network guestNetwork = networkModel.getNetwork(guestNetworkId);
        final Nic nic = nicDao.findByNtwkIdAndInstanceId(guestNetwork.getId(), router.getId());
        final NicProfile nicProfile = new NicProfile(nic, guestNetwork, nic.getBroadcastUri(), nic.getIsolationUri(), networkModel.getNetworkRate(guestNetwork.getId(),
                router.getId()), networkModel.isSecurityGroupSupportedInNetwork(guestNetwork), networkModel.getNetworkTag(router.getHypervisorType(), guestNetwork));
        final NetworkOffering offering = networkOfferingDao.findById(guestNetwork.getNetworkOfferingId());
        String maxconn = null;
        if (offering.getConcurrentConnections() == null) {
            maxconn = configDao.getValue(Config.NetworkLBHaproxyMaxConn.key());
        } else {
            maxconn = offering.getConcurrentConnections().toString();
        }

        final LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(lbs, routerPublicIp, routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()),
                router.getPrivateIpAddress(), itMgr.toNicTO(nicProfile, router.getHypervisorType()), router.getVpcId(), maxconn, offering.isKeepAliveEnabled());

        cmd.lbStatsVisibility = configDao.getValue(Config.NetworkLBHaproxyStatsVisbility.key());
        cmd.lbStatsUri = configDao.getValue(Config.NetworkLBHaproxyStatsUri.key());
        cmd.lbStatsAuth = configDao.getValue(Config.NetworkLBHaproxyStatsAuth.key());
        cmd.lbStatsPort = configDao.getValue(Config.NetworkLBHaproxyStatsPort.key());

        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        final DataCenterVO dcVo = dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand(cmd);
    }
}