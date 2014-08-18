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
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.PvlanSetupCommand;
import com.cloud.agent.api.routing.IpAliasTO;
import com.cloud.agent.manager.Commands;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.DhcpEntryRules;
import com.cloud.network.rules.DhcpSubNetRules;
import com.cloud.network.rules.NetworkAclsRules;
import com.cloud.network.rules.NicPlugInOutRules;
import com.cloud.network.rules.PrivateGatewayRules;
import com.cloud.network.rules.UserdataPwdRules;
import com.cloud.network.rules.VpcIpAssociationRules;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicIpAliasVO;

@Component
public class AdvancedNetworkVisitor extends BasicNetworkVisitor {

    private static final Logger s_logger = Logger.getLogger(AdvancedNetworkVisitor.class);

    @Override
    public boolean visit(final UserdataPwdRules userdata) throws ResourceUnavailableException {
        final VirtualRouter router = userdata.getRouter();

        final Commands commands = new Commands(Command.OnError.Stop);
        final VirtualMachineProfile profile = userdata.getProfile();
        final NicVO nicVo = userdata.getNicVo();
        final UserVmVO userVM = userdata.getUserVM();

        userdata.createPasswordCommand(router, profile, nicVo, commands);
        userdata.createVmDataCommand(router, userVM, nicVo, userVM.getDetail("SSH.PublicKey"), commands);

        return _applianceManager.sendCommandsToRouter(router, commands);
    }

    @Override
    public boolean visit(final DhcpEntryRules dhcp) throws ResourceUnavailableException {
        final VirtualRouter router = dhcp.getRouter();

        final Commands commands = new Commands(Command.OnError.Stop);
        final NicVO nicVo = dhcp.getNicVo();
        final UserVmVO userVM = dhcp.getUserVM();

        dhcp.createDhcpEntryCommand(router, userVM, nicVo, commands);

        return _applianceManager.sendCommandsToRouter(router, commands);
    }

    @Override
    public boolean visit(final NicPlugInOutRules nicPlugInOutRules) throws ResourceUnavailableException {
        final VirtualRouter router = nicPlugInOutRules.getRouter();

        final Commands commands = nicPlugInOutRules.getNetUsageCommands();

        if (commands.size() > 0) {
            return _applianceManager.sendCommandsToRouter(router, commands);
        }
        return true;
    }

    @Override
    public boolean visit(final NetworkAclsRules acls) throws ResourceUnavailableException {
        final VirtualRouter router = acls.getRouter();
        final Network network = acls.getNetwork();

        Commands commands = new Commands(Command.OnError.Continue);
        List<? extends NetworkACLItem> rules = acls.getRules();
        acls.createNetworkACLsCommands(rules, router, commands, network.getId(), acls.isPrivateGateway());

        return _applianceManager.sendCommandsToRouter(router, commands);
    }

    @Override
    public boolean visit(final VpcIpAssociationRules vpcip) throws ResourceUnavailableException {
        final VirtualRouter router = vpcip.getRouter();

        Commands cmds = new Commands(Command.OnError.Continue);
        Map<String, String> vlanMacAddress = vpcip.getVlanMacAddress();
        List<PublicIpAddress> ipsToSend = vpcip.getIpsToSend();


        if (!ipsToSend.isEmpty()) {
            vpcip.createVpcAssociatePublicIPCommands(router, ipsToSend, cmds, vlanMacAddress);
            return _applianceManager.sendCommandsToRouter(router, cmds);
        } else {
            return true;
        }
    }

    @Override
    public boolean visit(final PrivateGatewayRules privateGW) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final DhcpPvlanRules dhcp) throws ResourceUnavailableException {
        final VirtualRouter router = dhcp.getRouter();
        final PvlanSetupCommand setupCommand = dhcp.getSetupCommand();

        // In fact we send command to the host of router, we're not programming router but the host
        Commands cmds = new Commands(Command.OnError.Stop);
        cmds.addCommand(setupCommand);

        try {
            return _applianceManager.sendCommandsToRouter(router, cmds);
        } catch (final ResourceUnavailableException e) {
            s_logger.warn("Timed Out", e);
            return false;
        }
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

        subnet.createIpAlias(router, ipaliasTo, nicAlias.getNetworkId(), cmds);

        //also add the required configuration to the dnsmasq for supporting dhcp and dns on the new ip.
        subnet.configDnsMasq(router, network, cmds);

        return _applianceManager.sendCommandsToRouter(router, cmds);
    }
}