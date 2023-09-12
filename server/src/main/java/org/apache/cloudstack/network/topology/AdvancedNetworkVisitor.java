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
import com.cloud.agent.manager.Commands;
import com.cloud.dc.DataCenter;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.VpnUser;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.AdvancedVpnRules;
import com.cloud.network.rules.DhcpEntryRules;
import com.cloud.network.rules.DhcpPvlanRules;
import com.cloud.network.rules.NetworkAclsRules;
import com.cloud.network.rules.NicPlugInOutRules;
import com.cloud.network.rules.PrivateGatewayRules;
import com.cloud.network.rules.StaticRoutesRules;
import com.cloud.network.rules.UserdataPwdRules;
import com.cloud.network.rules.VpcIpAssociationRules;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.PrivateIpAddress;
import com.cloud.network.vpc.PrivateIpVO;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineProfile;

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

        _commandSetupHelper.createPasswordCommand(router, profile, nicVo, commands);
        _commandSetupHelper.createVmDataCommand(router, userVM, nicVo, userVM.getDetail("SSH.PublicKey"), commands);

        return _networkGeneralHelper.sendCommandsToRouter(router, commands);
    }

    @Override
    public boolean visit(final DhcpEntryRules dhcp) throws ResourceUnavailableException {
        final VirtualRouter router = dhcp.getRouter();

        final Commands commands = new Commands(Command.OnError.Stop);
        final NicVO nicVo = dhcp.getNicVo();
        final UserVmVO userVM = dhcp.getUserVM();
        final boolean remove = dhcp.isRemove();

        _commandSetupHelper.createDhcpEntryCommand(router, userVM, nicVo, remove, commands);

        return _networkGeneralHelper.sendCommandsToRouter(router, commands);
    }

    @Override
    public boolean visit(final NicPlugInOutRules nicPlugInOutRules) throws ResourceUnavailableException {
        final VirtualRouter router = nicPlugInOutRules.getRouter();

        final Commands commands = nicPlugInOutRules.getNetUsageCommands();

        if (commands.size() > 0) {
            return _networkGeneralHelper.sendCommandsToRouter(router, commands);
        }
        return true;
    }

    @Override
    public boolean visit(final NetworkAclsRules acls) throws ResourceUnavailableException {
        final VirtualRouter router = acls.getRouter();
        final Network network = acls.getNetwork();

        final Commands commands = new Commands(Command.OnError.Continue);
        final List<? extends NetworkACLItem> rules = acls.getRules();
        _commandSetupHelper.createNetworkACLsCommands(rules, router, commands, network.getId(), acls.isPrivateGateway());

        return _networkGeneralHelper.sendCommandsToRouter(router, commands);
    }

    @Override
    public boolean visit(final VpcIpAssociationRules vpcip) throws ResourceUnavailableException {
        final VirtualRouter router = vpcip.getRouter();

        final Commands cmds = new Commands(Command.OnError.Continue);
        final Map<String, String> vlanMacAddress = vpcip.getVlanMacAddress();
        final List<PublicIpAddress> ipsToSend = vpcip.getIpsToSend();

        if (!ipsToSend.isEmpty()) {
            _commandSetupHelper.createVpcAssociatePublicIPCommands(router, ipsToSend, cmds, vlanMacAddress);
            return _networkGeneralHelper.sendCommandsToRouter(router, cmds);
        } else {
            return true;
        }
    }

    @Override
    public boolean visit(final PrivateGatewayRules privateGW) throws ResourceUnavailableException {
        final VirtualRouter router = privateGW.getRouter();
        final NicProfile nicProfile = privateGW.getNicProfile();

        final boolean isAddOperation = privateGW.isAddOperation();

        if (router.getState() == State.Running) {

            final PrivateIpVO ipVO = privateGW.retrivePrivateIP(this);
            final Network network = privateGW.retrievePrivateNetwork(this);

            final String netmask = NetUtils.getCidrNetmask(network.getCidr());
            final PrivateIpAddress ip = new PrivateIpAddress(ipVO, network.getBroadcastUri().toString(), network.getGateway(), netmask, nicProfile.getMacAddress());

            final List<PrivateIpAddress> privateIps = new ArrayList<PrivateIpAddress>(1);
            privateIps.add(ip);

            final Commands cmds = new Commands(Command.OnError.Stop);
            _commandSetupHelper.createVpcAssociatePrivateIPCommands(router, privateIps, cmds, isAddOperation);

            try {
                if (_networkGeneralHelper.sendCommandsToRouter(router, cmds)) {
                    s_logger.debug("Successfully applied ip association for ip " + ip + " in vpc network " + network);
                    return true;
                } else {
                    s_logger.warn("Failed to associate ip address " + ip + " in vpc network " + network);
                    return false;
                }
            } catch (final Exception ex) {
                s_logger.warn("Failed to send  " + (isAddOperation ? "add " : "delete ") + " private network " + network + " commands to rotuer ");
                return false;
            }
        } else if (router.getState() == State.Stopped || router.getState() == State.Stopping) {
            s_logger.debug("Router " + router.getInstanceName() + " is in " + router.getState() + ", so not sending setup private network command to the backend");
        } else {
            s_logger.warn("Unable to setup private gateway, virtual router " + router + " is not in the right state " + router.getState());

            throw new ResourceUnavailableException("Unable to setup Private gateway on the backend," + " virtual router " + router + " is not in the right state",
                    DataCenter.class, router.getDataCenterId());
        }
        return true;
    }

    @Override
    public boolean visit(final DhcpPvlanRules dhcp) throws ResourceUnavailableException {
        final VirtualRouter router = dhcp.getRouter();
        final PvlanSetupCommand setupCommand = dhcp.getSetupCommand();

        // In fact we send command to the host of router, we're not programming
        // router but the host
        final Commands cmds = new Commands(Command.OnError.Stop);
        cmds.addCommand(setupCommand);

        try {
            return _networkGeneralHelper.sendCommandsToRouter(router, cmds);
        } catch (final ResourceUnavailableException e) {
            s_logger.warn("Timed Out", e);
            return false;
        }
    }

    @Override
    public boolean visit(final StaticRoutesRules staticRoutesRules) throws ResourceUnavailableException {
        final VirtualRouter router = staticRoutesRules.getRouter();
        final List<StaticRouteProfile> staticRoutes = staticRoutesRules.getStaticRoutes();

        final Commands cmds = new Commands(Command.OnError.Continue);
        _commandSetupHelper.createStaticRouteCommands(staticRoutes, router, cmds);

        return _networkGeneralHelper.sendCommandsToRouter(router, cmds);
    }

    @Override
    public boolean visit(final AdvancedVpnRules vpnRules) throws ResourceUnavailableException {
        final VirtualRouter router = vpnRules.getRouter();
        final List<? extends VpnUser> users = vpnRules.getUsers();

        final Commands cmds = new Commands(Command.OnError.Continue);
        _commandSetupHelper.createApplyVpnUsersCommand(users, router, cmds);

        // Currently we receive just one answer from the agent. In the future we
        // have to parse individual answers and set
        // results accordingly
        return _networkGeneralHelper.sendCommandsToRouter(router, cmds);
    }
}
