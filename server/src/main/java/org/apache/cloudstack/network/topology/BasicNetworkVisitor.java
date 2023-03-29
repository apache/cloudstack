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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.routing.IpAliasTO;
import com.cloud.agent.manager.Commands;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.VpnUser;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.router.CommandSetupHelper;
import com.cloud.network.router.NetworkHelper;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.AdvancedVpnRules;
import com.cloud.network.rules.BasicVpnRules;
import com.cloud.network.rules.DhcpEntryRules;
import com.cloud.network.rules.DhcpPvlanRules;
import com.cloud.network.rules.DhcpSubNetRules;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRules;
import com.cloud.network.rules.IpAssociationRules;
import com.cloud.network.rules.LoadBalancingRules;
import com.cloud.network.rules.NetworkAclsRules;
import com.cloud.network.rules.NicPlugInOutRules;
import com.cloud.network.rules.PasswordToRouterRules;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.PrivateGatewayRules;
import com.cloud.network.rules.SshKeyToRouterRules;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.rules.StaticNatRule;
import com.cloud.network.rules.StaticNatRules;
import com.cloud.network.rules.StaticRoutesRules;
import com.cloud.network.rules.UserdataPwdRules;
import com.cloud.network.rules.UserdataToRouterRules;
import com.cloud.network.rules.VirtualNetworkApplianceFactory;
import com.cloud.network.rules.VpcIpAssociationRules;
import com.cloud.storage.VMTemplateVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicIpAliasVO;

@Component
public class BasicNetworkVisitor extends NetworkTopologyVisitor {

    private static final Logger s_logger = Logger.getLogger(BasicNetworkVisitor.class);

    @Autowired
    @Qualifier("networkHelper")
    protected NetworkHelper _networkGeneralHelper;

    @Inject
    protected VirtualNetworkApplianceFactory _virtualNetworkApplianceFactory;

    @Inject
    protected CommandSetupHelper _commandSetupHelper;

    @Override
    public VirtualNetworkApplianceFactory getVirtualNetworkApplianceFactory() {
        return _virtualNetworkApplianceFactory;
    }

    @Override
    public boolean visit(final StaticNatRules nat) throws ResourceUnavailableException {
        final Network network = nat.getNetwork();
        final VirtualRouter router = nat.getRouter();
        final List<? extends StaticNat> rules = nat.getRules();

        final Commands cmds = new Commands(Command.OnError.Continue);
        _commandSetupHelper.createApplyStaticNatCommands(rules, router, cmds, network.getId());

        return _networkGeneralHelper.sendCommandsToRouter(router, cmds);
    }

    @Override
    public boolean visit(final LoadBalancingRules loadbalancing) throws ResourceUnavailableException {
        final Network network = loadbalancing.getNetwork();
        final DomainRouterVO router = (DomainRouterVO) loadbalancing.getRouter();
        final List<LoadBalancingRule> rules = loadbalancing.getRules();

        final Commands cmds = new Commands(Command.OnError.Continue);
        _commandSetupHelper.createApplyLoadBalancingRulesCommands(rules, router, cmds, network.getId());

        return _networkGeneralHelper.sendCommandsToRouter(router, cmds);
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

            _commandSetupHelper.createApplyLoadBalancingRulesCommands(loadbalancingRules, router, cmds, network.getId());

            return _networkGeneralHelper.sendCommandsToRouter(router, cmds);

        } else if (purpose == Purpose.PortForwarding) {

            _commandSetupHelper.createApplyPortForwardingRulesCommands((List<? extends PortForwardingRule>) rules, router, cmds, network.getId());

            return _networkGeneralHelper.sendCommandsToRouter(router, cmds);

        } else if (purpose == Purpose.StaticNat) {

            _commandSetupHelper.createApplyStaticNatRulesCommands((List<StaticNatRule>) rules, router, cmds, network.getId());

            return _networkGeneralHelper.sendCommandsToRouter(router, cmds);

        } else if (purpose == Purpose.Firewall) {

            _commandSetupHelper.createApplyFirewallRulesCommands(rules, router, cmds, network.getId());

            return _networkGeneralHelper.sendCommandsToRouter(router, cmds);

        } else if (purpose == Purpose.Ipv6Firewall) {

            _commandSetupHelper.createApplyIpv6FirewallRulesCommands(rules, router, cmds, network.getId());

            return _networkGeneralHelper.sendCommandsToRouter(router, cmds);

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

        _commandSetupHelper.createAssociateIPCommands(router, ips, commands, network.getId());
        return _networkGeneralHelper.sendCommandsToRouter(router, commands);
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
            _commandSetupHelper.createPasswordCommand(router, profile, nicVo, commands);
            _commandSetupHelper.createVmDataCommand(router, userVM, nicVo, userVM.getDetail("SSH.PublicKey"), commands);

            return _networkGeneralHelper.sendCommandsToRouter(router, commands);
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
        final boolean remove = dhcp.isRemove();

        if (router != null && (remove || (destination != null && destination.getPod() != null &&
                router.getPodIdToDeployIn() != null &&
                router.getPodIdToDeployIn().longValue() == destination.getPod().getId()))) {
            _commandSetupHelper.createDhcpEntryCommand(router, userVM, nicVo, remove, commands);

            return _networkGeneralHelper.sendCommandsToRouter(router, commands);
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

        if (template != null && template.isEnablePassword()) {
            _commandSetupHelper.createPasswordCommand(router, profile, nicVo, commands);
        }

        _commandSetupHelper.createVmDataCommand(router, userVM, nicVo, sshKeystr, commands);

        return _networkGeneralHelper.sendCommandsToRouter(router, commands);
    }

    @Override
    public boolean visit(final PasswordToRouterRules passwd) throws ResourceUnavailableException {
        final VirtualRouter router = passwd.getRouter();
        final NicVO nicVo = passwd.getNicVo();
        final VirtualMachineProfile profile = passwd.getProfile();

        final Commands cmds = new Commands(Command.OnError.Stop);
        _commandSetupHelper.createPasswordCommand(router, profile, nicVo, cmds);

        return _networkGeneralHelper.sendCommandsToRouter(router, cmds);
    }

    @Override
    public boolean visit(final UserdataToRouterRules userdata) throws ResourceUnavailableException {
        final VirtualRouter router = userdata.getRouter();

        final UserVmVO userVM = userdata.getUserVM();
        final NicVO nicVo = userdata.getNicVo();

        final Commands commands = new Commands(Command.OnError.Stop);
        _commandSetupHelper.createVmDataCommand(router, userVM, nicVo, userVM.getDetail("SSH.PublicKey"), commands);

        return _networkGeneralHelper.sendCommandsToRouter(router, commands);
    }

    @Override
    public boolean visit(final BasicVpnRules vpnRules) throws ResourceUnavailableException {
        final VirtualRouter router = vpnRules.getRouter();
        final List<? extends VpnUser> users = vpnRules.getUsers();

        final Commands cmds = new Commands(Command.OnError.Continue);
        _commandSetupHelper.createApplyVpnUsersCommand(users, router, cmds);

        return _networkGeneralHelper.sendCommandsToRouter(router, cmds);
    }

    @Override
    public boolean visit(final DhcpSubNetRules subnet) throws ResourceUnavailableException {
        final VirtualRouter router = subnet.getRouter();
        final Network network = subnet.getNetwork();
        final NicIpAliasVO nicAlias = subnet.getNicAlias();
        final String routerAliasIp = subnet.getRouterAliasIp();

        final Commands cmds = new Commands(Command.OnError.Stop);

        final List<IpAliasTO> ipaliasTo = new ArrayList<IpAliasTO>();
        ipaliasTo.add(new IpAliasTO(routerAliasIp, nicAlias.getNetmask(), nicAlias.getAliasCount().toString()));

        _commandSetupHelper.createIpAlias(router, ipaliasTo, nicAlias.getNetworkId(), cmds);

        // also add the required configuration to the dnsmasq for supporting
        // dhcp and dns on the new ip.
        _commandSetupHelper.configDnsMasq(router, network, cmds);

        return _networkGeneralHelper.sendCommandsToRouter(router, cmds);
    }

    @Override
    public boolean visit(final DhcpPvlanRules dhcpRules) throws ResourceUnavailableException {
        throw new CloudRuntimeException("DhcpPvlanRules not implemented in Basic Network Topology.");
    }

    @Override
    public boolean visit(final NicPlugInOutRules nicPlugInOutRules) throws ResourceUnavailableException {
        throw new CloudRuntimeException("NicPlugInOutRules not implemented in Basic Network Topology.");
    }

    @Override
    public boolean visit(final NetworkAclsRules aclsRules) throws ResourceUnavailableException {
        throw new CloudRuntimeException("NetworkAclsRules not implemented in Basic Network Topology.");
    }

    @Override
    public boolean visit(final VpcIpAssociationRules ipRules) throws ResourceUnavailableException {
        throw new CloudRuntimeException("VpcIpAssociationRules not implemented in Basic Network Topology.");
    }

    @Override
    public boolean visit(final PrivateGatewayRules pvtGatewayRules) throws ResourceUnavailableException {
        throw new CloudRuntimeException("PrivateGatewayRules not implemented in Basic Network Topology.");
    }

    @Override
    public boolean visit(final StaticRoutesRules staticRoutesRules) throws ResourceUnavailableException {
        throw new CloudRuntimeException("StaticRoutesRules not implemented in Basic Network Topology.");
    }

    @Override
    public boolean visit(final AdvancedVpnRules vpnRules) throws ResourceUnavailableException {
        throw new CloudRuntimeException("AdvancedVpnRules not implemented in Basic Network Topology.");
    }
}
