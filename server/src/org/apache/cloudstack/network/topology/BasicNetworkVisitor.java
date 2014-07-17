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

package org.apache.cloudstack.network.topology;

import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Command;
import com.cloud.agent.manager.Commands;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.VpnUser;
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
import com.cloud.storage.VMTemplateVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineProfile;

@Component
public class BasicNetworkVisitor extends NetworkTopologyVisitor {

    private static final Logger s_logger = Logger.getLogger(BasicNetworkVisitor.class);

    @Inject
    protected NEWVirtualNetworkApplianceManager _applianceManager;

    @Override
    public boolean visit(final StaticNatRules nat) throws ResourceUnavailableException {
        final Network network = nat.getNetwork();
        final VirtualRouter router = nat.getRouter();
        final List<? extends StaticNat> rules = nat.getRules();

        final Commands cmds = new Commands(Command.OnError.Continue);
        nat.createApplyStaticNatCommands(rules, router, cmds, network.getId());

        return _applianceManager.sendCommandsToRouter(router, cmds);
    }

    @Override
    public boolean visit(final LoadBalancingRules loadbalancing) throws ResourceUnavailableException {
        final Network network = loadbalancing.getNetwork();
        final VirtualRouter router = loadbalancing.getRouter();
        final List<LoadBalancingRule> rules = loadbalancing.getRules();

        final Commands cmds = new Commands(Command.OnError.Continue);
        loadbalancing.createApplyLoadBalancingRulesCommands(rules, router, cmds, network.getId());

        return _applianceManager.sendCommandsToRouter(router, cmds);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(final FirewallRules firewall) throws ResourceUnavailableException {
        final Network network = firewall.getNetwork();
        final VirtualRouter router = firewall.getRouter();
        final List<? extends FirewallRule> rules = firewall.getRules();
        final List<LoadBalancingRule> loadbalancingRules = firewall.getLoadbalancingRules();

        final Purpose purpose = firewall.getPurpose();

        final Commands cmds = new Commands(Command.OnError.Continue);
        if (purpose == Purpose.LoadBalancing) {

            firewall.createApplyLoadBalancingRulesCommands(loadbalancingRules, router, cmds, network.getId());

            return _applianceManager.sendCommandsToRouter(router, cmds);

        } else if (purpose == Purpose.PortForwarding) {

            firewall.createApplyPortForwardingRulesCommands((List<? extends PortForwardingRule>) rules, router, cmds, network.getId());

            return _applianceManager.sendCommandsToRouter(router, cmds);

        } else if (purpose == Purpose.StaticNat) {

            firewall.createApplyStaticNatRulesCommands((List<StaticNatRule>)rules, router, cmds, network.getId());

            return _applianceManager.sendCommandsToRouter(router, cmds);

        } else if (purpose == Purpose.Firewall) {

            firewall.createApplyFirewallRulesCommands(rules, router, cmds, network.getId());

            return _applianceManager.sendCommandsToRouter(router, cmds);

        }
        s_logger.warn("Unable to apply rules of purpose: " + rules.get(0).getPurpose());

        return false;
    }

    @Override
    public boolean visit(final IpAssociationRules ipRules) throws ResourceUnavailableException {
        final Network network = ipRules.getNetwork();
        final VirtualRouter router = ipRules.getRouter();

        final Commands commands = new Commands(Command.OnError.Continue);
        final List<? extends PublicIpAddress> ips = ipRules.getIpAddresses();

        ipRules.createAssociateIPCommands(router, ips, commands, network.getId());
        return _applianceManager.sendCommandsToRouter(router, commands);
    }

    @Override
    public boolean visit(final UserdataPwdRules userdata) throws ResourceUnavailableException {
        final VirtualRouter router = userdata.getRouter();

        final Commands commands = new Commands(Command.OnError.Stop);
        final VirtualMachineProfile profile = userdata.getProfile();
        final NicVO nicVo = userdata.getNicVo();
        final UserVmVO userVM = userdata.getUserVM();
        final DeployDestination destination = userdata.getDestination();

        if (router.getPodIdToDeployIn().longValue() == destination.getPod().getId()) {
            userdata.createPasswordCommand(router, profile, nicVo, commands);
            userdata.createVmDataCommand(router, userVM, nicVo, userVM.getDetail("SSH.PublicKey"), commands);

            return _applianceManager.sendCommandsToRouter(router, commands);
        }

        return true;
    }

    @Override
    public boolean visit(final DhcpEntryRules dhcp) throws ResourceUnavailableException {
        final VirtualRouter router = dhcp.getRouter();

        final Commands commands = new Commands(Command.OnError.Stop);
        final NicVO nicVo = dhcp.getNicVo();
        final UserVmVO userVM = dhcp.getUserVM();
        final DeployDestination destination = dhcp.getDestination();

        if (router.getPodIdToDeployIn().longValue() == destination.getPod().getId()) {
            dhcp.createDhcpEntryCommand(router, userVM, nicVo, commands);

            return _applianceManager.sendCommandsToRouter(router, commands);
        }
        return true;
    }

    @Override
    public boolean visit(final SshKeyToRouterRules sshkey) throws ResourceUnavailableException {
        final VirtualRouter router = sshkey.getRouter();
        final VirtualMachineProfile profile = sshkey.getProfile();
        final String sshKeystr = sshkey.getSshPublicKey();
        final UserVmVO userVM = sshkey.getUserVM();

        final Commands commands = new Commands(Command.OnError.Stop);
        final NicVO nicVo = sshkey.getNicVo();
        final VMTemplateVO template = sshkey.getTemplate();

        if (template != null && template.getEnablePassword()) {
            sshkey.createPasswordCommand(router, profile, nicVo, commands);
        }

        sshkey.createVmDataCommand(router, userVM, nicVo, sshKeystr, commands);

        return _applianceManager.sendCommandsToRouter(router, commands);
    }

    @Override
    public boolean visit(final PasswordToRouterRules passwd) throws ResourceUnavailableException {
        final VirtualRouter router = passwd.getRouter();
        final NicVO nicVo = passwd.getNicVo();
        final VirtualMachineProfile profile = passwd.getProfile();

        final Commands cmds = new Commands(Command.OnError.Stop);
        passwd.createPasswordCommand(router, profile, nicVo, cmds);

        return _applianceManager.sendCommandsToRouter(router, cmds);
    }

    @Override
    public boolean visit(final NetworkAclsRules nat) throws ResourceUnavailableException {
        throw new CloudRuntimeException("NetworkAclsRules not implemented in Basic Network Topology.");
    }

    @Override
    public boolean visit(final VpcIpAssociationRules nat) throws ResourceUnavailableException {
        throw new CloudRuntimeException("VpcIpAssociationRules not implemented in Basic Network Topology.");
    }

    @Override
    public boolean visit(final UserdataToRouterRules userdata) throws ResourceUnavailableException {
        final VirtualRouter router = userdata.getRouter();

        final UserVmVO userVM = userdata.getUserVM();
        final NicVO nicVo = userdata.getNicVo();

        final Commands commands = new Commands(Command.OnError.Stop);
        userdata.createVmDataCommand(router, userVM, nicVo, null, commands);

        return _applianceManager.sendCommandsToRouter(router, commands);
    }

    @Override
    public boolean visit(final PrivateGatewayRules userdata) throws ResourceUnavailableException {
        throw new CloudRuntimeException("PrivateGatewayRules not implemented in Basic Network Topology.");
    }

    @Override
    public boolean visit(final VpnRules vpn) throws ResourceUnavailableException {
        VirtualRouter router = vpn.getRouter();
        List<? extends VpnUser> users = vpn.getUsers();

        final Commands cmds = new Commands(Command.OnError.Continue);
        vpn.createApplyVpnUsersCommand(users, router, cmds);

        return _applianceManager.sendCommandsToRouter(router, cmds);
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