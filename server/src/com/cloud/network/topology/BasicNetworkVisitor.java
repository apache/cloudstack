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

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.DhcpRules;
import com.cloud.network.rules.FirewallRules;
import com.cloud.network.rules.IpAssociationRules;
import com.cloud.network.rules.LoadBalancingRules;
import com.cloud.network.rules.NetworkAclsRules;
import com.cloud.network.rules.PasswordToRouterRules;
import com.cloud.network.rules.PrivateGatewayRules;
import com.cloud.network.rules.SshKeyToRouterRules;
import com.cloud.network.rules.StaticNatRules;
import com.cloud.network.rules.UserdataPwdRules;
import com.cloud.network.rules.UserdataToRouterRules;
import com.cloud.network.rules.VpcIpAssociationRules;
import com.cloud.network.rules.VpnRules;

public class BasicNetworkVisitor extends NetworkTopologyVisitor {

    public BasicNetworkVisitor(final NetworkTopology networkTopology) {
        super(networkTopology);
    }

    @Override
    public boolean visit(final StaticNatRules nat) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final LoadBalancingRules nat) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final FirewallRules nat) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final IpAssociationRules nat) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final UserdataPwdRules nat) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(final DhcpRules nat) throws ResourceUnavailableException {
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
        Network network = userdata.getNetwork();
        VirtualRouter router = userdata.getRouter();

        //return sendCommandsToRouter(router, cmds);
        return false;
    }

    @Override
    public boolean visit(PrivateGatewayRules userdata) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean visit(VpnRules userdata) throws ResourceUnavailableException {
        return false;
    }
}