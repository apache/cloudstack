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

import org.springframework.stereotype.Component;

import com.cloud.agent.api.Command;
import com.cloud.agent.manager.Commands;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.DhcpEntryRules;
import com.cloud.network.rules.DhcpPvlanRules;
import com.cloud.network.rules.DhcpSubNetRules;
import com.cloud.network.rules.NetworkAclsRules;
import com.cloud.network.rules.PasswordToRouterRules;
import com.cloud.network.rules.PrivateGatewayRules;
import com.cloud.network.rules.SshKeyToRouterRules;
import com.cloud.network.rules.UserdataPwdRules;
import com.cloud.network.rules.UserdataToRouterRules;
import com.cloud.network.rules.VpcIpAssociationRules;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineProfile;

@Component
public class AdvancedNetworkVisitor extends BasicNetworkVisitor {

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
    public boolean visit(final SshKeyToRouterRules sshkey) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final PasswordToRouterRules pwd) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final NetworkAclsRules acls) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final VpcIpAssociationRules vpcip) throws ResourceUnavailableException {
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
    public boolean visit(final DhcpPvlanRules dhcp) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final DhcpSubNetRules subnet) throws ResourceUnavailableException {
        return false;
    }
}