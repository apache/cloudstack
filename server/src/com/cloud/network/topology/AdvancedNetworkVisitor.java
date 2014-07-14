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

package com.cloud.network.topology;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Command;
import com.cloud.agent.manager.Commands;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.router.NEWVirtualNetworkApplianceManager;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.DhcpEntryRules;
import com.cloud.network.rules.DhcpPvlanRules;
import com.cloud.network.rules.DhcpSubNetRules;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRules;
import com.cloud.network.rules.IpAssociationRules;
import com.cloud.network.rules.LoadBalancingRules;
import com.cloud.network.rules.NetworkAclsRules;
import com.cloud.network.rules.PasswordToRouterRules;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.PrivateGatewayRules;
import com.cloud.network.rules.SshKeyToRouterRules;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.rules.StaticNatRule;
import com.cloud.network.rules.StaticNatRules;
import com.cloud.network.rules.UserdataPwdRules;
import com.cloud.network.rules.UserdataToRouterRules;
import com.cloud.network.rules.VpcIpAssociationRules;
import com.cloud.network.rules.VpnRules;

public class AdvancedNetworkVisitor extends NetworkTopologyVisitor {

    private static final Logger s_logger = Logger.getLogger(AdvancedNetworkVisitor.class);

    protected NEWVirtualNetworkApplianceManager applianceManager;

    public void setApplianceManager(
            final NEWVirtualNetworkApplianceManager applianceManager) {
        this.applianceManager = applianceManager;
    }

    @Override
    public boolean visit(final StaticNatRules nat) throws ResourceUnavailableException {
        Network network = nat.getNetwork();
        VirtualRouter router = nat.getRouter();
        List<? extends StaticNat> rules = nat.getRules();

        final Commands cmds = new Commands(Command.OnError.Continue);
        nat.createApplyStaticNatCommands(rules, router, cmds, network.getId());

        //return sendCommandsToRouter(router, cmds);

        return false;
    }

    @Override
    public boolean visit(final LoadBalancingRules loadbalancing) throws ResourceUnavailableException {
        Network network = loadbalancing.getNetwork();
        VirtualRouter router = loadbalancing.getRouter();
        List<LoadBalancingRule> rules = loadbalancing.getRules();

        final Commands cmds = new Commands(Command.OnError.Continue);
        loadbalancing.createApplyLoadBalancingRulesCommands(rules, router, cmds, network.getId());

        return applianceManager.sendCommandsToRouter(router, cmds);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(final FirewallRules firewall) throws ResourceUnavailableException {
        Network network = firewall.getNetwork();
        VirtualRouter router = firewall.getRouter();
        List<? extends FirewallRule> rules = firewall.getRules();
        List<LoadBalancingRule> loadbalancingRules = firewall.getLoadbalancingRules();

        Purpose purpose = firewall.getPurpose();

        final Commands cmds = new Commands(Command.OnError.Continue);
        if (purpose == Purpose.LoadBalancing) {

            firewall.createApplyLoadBalancingRulesCommands(loadbalancingRules, router, cmds, network.getId());

            return applianceManager.sendCommandsToRouter(router, cmds);

        } else if (purpose == Purpose.PortForwarding) {

            firewall.createApplyPortForwardingRulesCommands((List<? extends PortForwardingRule>) rules, router, cmds, network.getId());

            return applianceManager.sendCommandsToRouter(router, cmds);

        } else if (purpose == Purpose.StaticNat) {

            firewall.createApplyStaticNatRulesCommands((List<StaticNatRule>)rules, router, cmds, network.getId());

            return applianceManager.sendCommandsToRouter(router, cmds);

        } else if (purpose == Purpose.Firewall) {

            firewall.createApplyFirewallRulesCommands(rules, router, cmds, network.getId());

            return applianceManager.sendCommandsToRouter(router, cmds);

        }
        s_logger.warn("Unable to apply rules of purpose: " + rules.get(0).getPurpose());

        return false;
    }

    @Override
    public boolean visit(final IpAssociationRules ipRules) throws ResourceUnavailableException {
        VirtualRouter router = ipRules.getRouter();
        Commands commands = ipRules.getCommands();

        //return sendCommandsToRouter(router, commands);

        return false;
    }

    @Override
    public boolean visit(final UserdataPwdRules nat) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final DhcpEntryRules nat) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final SshKeyToRouterRules nat) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final PasswordToRouterRules nat) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final NetworkAclsRules nat) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final VpcIpAssociationRules nat) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final UserdataToRouterRules userdata) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final PrivateGatewayRules privateGW) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final VpnRules vpn) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final DhcpPvlanRules vpn) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final DhcpSubNetRules vpn) throws ResourceUnavailableException {
        return false;
    }
}