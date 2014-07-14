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

import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesVpcCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.to.FirewallRuleTO;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.Config;
import com.cloud.dc.DataCenterVO;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
import com.cloud.network.lb.LoadBalancingRule.LbSslCert;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.network.topology.NetworkTopologyVisitor;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.utils.net.Ip;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;

public class FirewallRules extends RuleApplier {

    private final List<? extends FirewallRule> rules;
    private List<LoadBalancingRule> loadbalancingRules;

    private Purpose purpose;

    public FirewallRules(final Network network, final List<? extends FirewallRule> rules) {
        super(network);
        this.rules = rules;
    }

    @Override
    public boolean accept(final NetworkTopologyVisitor visitor, final VirtualRouter router) throws ResourceUnavailableException {
        this.router = router;

        purpose = rules.get(0).getPurpose();

        if (purpose == Purpose.LoadBalancing) {
            // for load balancer we have to resend all lb rules for the network
            final List<LoadBalancerVO> lbs = loadBalancerDao.listByNetworkIdAndScheme(network.getId(), Scheme.Public);
            loadbalancingRules = new ArrayList<LoadBalancingRule>();
            for (final LoadBalancerVO lb : lbs) {
                final List<LbDestination> dstList = lbMgr.getExistingDestinations(lb.getId());
                final List<LbStickinessPolicy> policyList = lbMgr.getStickinessPolicies(lb.getId());
                final List<LbHealthCheckPolicy> hcPolicyList = lbMgr.getHealthCheckPolicies(lb.getId());
                final LbSslCert sslCert = lbMgr.getLbSslCert(lb.getId());
                final Ip sourceIp = networkModel.getPublicIpAddress(lb.getSourceIpAddressId()).getAddress();
                final LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, policyList, hcPolicyList, sourceIp, sslCert, lb.getLbProtocol());

                loadbalancingRules.add(loadBalancing);
            }
        }

        return visitor.visit(this);
    }

    public List<? extends FirewallRule> getRules() {
        return rules;
    }

    public List<LoadBalancingRule> getLoadbalancingRules() {
        return loadbalancingRules;
    }

    public Purpose getPurpose() {
        return purpose;
    }

    public void createApplyLoadBalancingRulesCommands(final List<LoadBalancingRule> rules, final VirtualRouter router, final Commands cmds, final long guestNetworkId) {

        final LoadBalancerTO[] lbs = new LoadBalancerTO[rules.size()];
        int i = 0;
        // We don't support VR to be inline currently
        final boolean inline = false;
        for (final LoadBalancingRule rule : rules) {
            final boolean revoked = (rule.getState().equals(FirewallRule.State.Revoke));
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
        final NicProfile nicProfile =
                new NicProfile(nic, guestNetwork, nic.getBroadcastUri(), nic.getIsolationUri(), networkModel.getNetworkRate(guestNetwork.getId(), router.getId()),
                        networkModel.isSecurityGroupSupportedInNetwork(guestNetwork), networkModel.getNetworkTag(router.getHypervisorType(), guestNetwork));
        final NetworkOffering offering = networkOfferingDao.findById(guestNetwork.getNetworkOfferingId());
        String maxconn = null;
        if (offering.getConcurrentConnections() == null) {
            maxconn = configDao.getValue(Config.NetworkLBHaproxyMaxConn.key());
        } else {
            maxconn = offering.getConcurrentConnections().toString();
        }

        final LoadBalancerConfigCommand cmd =
                new LoadBalancerConfigCommand(lbs, routerPublicIp, routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()), router.getPrivateIpAddress(), itMgr.toNicTO(
                        nicProfile, router.getHypervisorType()), router.getVpcId(), maxconn, offering.isKeepAliveEnabled());

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

    public void createApplyPortForwardingRulesCommands(final List<? extends PortForwardingRule> rules, final VirtualRouter router, final Commands cmds, final long guestNetworkId) {
        List<PortForwardingRuleTO> rulesTO = new ArrayList<PortForwardingRuleTO>();
        if (rules != null) {
            for (final PortForwardingRule rule : rules) {
                final IpAddress sourceIp = networkModel.getIp(rule.getSourceIpAddressId());
                final PortForwardingRuleTO ruleTO = new PortForwardingRuleTO(rule, null, sourceIp.getAddress().addr());
                rulesTO.add(ruleTO);
            }
        }

        SetPortForwardingRulesCommand cmd = null;

        if (router.getVpcId() != null) {
            cmd = new SetPortForwardingRulesVpcCommand(rulesTO);
        } else {
            cmd = new SetPortForwardingRulesCommand(rulesTO);
        }

        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        final DataCenterVO dcVo = dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

        cmds.addCommand(cmd);
    }

    public void createApplyStaticNatRulesCommands(final List<? extends StaticNatRule> rules, final VirtualRouter router, final Commands cmds, final long guestNetworkId) {
        List<StaticNatRuleTO> rulesTO = new ArrayList<StaticNatRuleTO>();
        if (rules != null) {
            for (final StaticNatRule rule : rules) {
                final IpAddress sourceIp = networkModel.getIp(rule.getSourceIpAddressId());
                final StaticNatRuleTO ruleTO = new StaticNatRuleTO(rule, null, sourceIp.getAddress().addr(), rule.getDestIpAddress());
                rulesTO.add(ruleTO);
            }
        }

        final SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(rulesTO, router.getVpcId());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        final DataCenterVO dcVo = dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand(cmd);
    }

    public void createApplyFirewallRulesCommands(final List<? extends FirewallRule> rules, final VirtualRouter router, final Commands cmds, final long guestNetworkId) {
        List<FirewallRuleTO> rulesTO = new ArrayList<FirewallRuleTO>();
        String systemRule = null;
        Boolean defaultEgressPolicy = false;
        if (rules != null) {
            if (rules.size() > 0) {
                if (rules.get(0).getTrafficType() == FirewallRule.TrafficType.Egress && rules.get(0).getType() == FirewallRule.FirewallRuleType.System) {
                    systemRule = String.valueOf(FirewallRule.FirewallRuleType.System);
                }
            }
            for (final FirewallRule rule : rules) {
                rulesDao.loadSourceCidrs((FirewallRuleVO)rule);
                final FirewallRule.TrafficType traffictype = rule.getTrafficType();
                if (traffictype == FirewallRule.TrafficType.Ingress) {
                    final IpAddress sourceIp = networkModel.getIp(rule.getSourceIpAddressId());
                    final FirewallRuleTO ruleTO = new FirewallRuleTO(rule, null, sourceIp.getAddress().addr(), Purpose.Firewall, traffictype);
                    rulesTO.add(ruleTO);
                } else if (rule.getTrafficType() == FirewallRule.TrafficType.Egress) {
                    final NetworkVO network = networkDao.findById(guestNetworkId);
                    final NetworkOfferingVO offering = networkOfferingDao.findById(network.getNetworkOfferingId());
                    defaultEgressPolicy = offering.getEgressDefaultPolicy();
                    assert (rule.getSourceIpAddressId() == null) : "ipAddressId should be null for egress firewall rule. ";
                    final FirewallRuleTO ruleTO = new FirewallRuleTO(rule, null, "", Purpose.Firewall, traffictype, defaultEgressPolicy);
                    rulesTO.add(ruleTO);
                }
            }
        }

        final SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rulesTO);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        final DataCenterVO dcVo = dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        if (systemRule != null) {
            cmd.setAccessDetail(NetworkElementCommand.FIREWALL_EGRESS_DEFAULT, systemRule);
        } else {
            cmd.setAccessDetail(NetworkElementCommand.FIREWALL_EGRESS_DEFAULT, String.valueOf(defaultEgressPolicy));
        }

        cmds.addCommand(cmd);
    }
}