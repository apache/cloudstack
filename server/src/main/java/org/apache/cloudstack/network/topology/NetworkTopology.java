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

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VpnUser;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.RuleApplier;
import com.cloud.network.rules.RuleApplierWrapper;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile;

public interface NetworkTopology {

    NetworkTopologyVisitor getVisitor();

    // ====== USED FOR VPC ONLY ====== //

    boolean setupDhcpForPvlan(final boolean add, final DomainRouterVO router, final Long hostId, final NicProfile nic) throws ResourceUnavailableException;

    boolean configDhcpForSubnet(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final DeployDestination dest, final List<DomainRouterVO> routers)
            throws ResourceUnavailableException;

    boolean applyNetworkACLs(final Network network, final List<? extends NetworkACLItem> rules, final VirtualRouter router, final boolean isPrivateGateway)
            throws ResourceUnavailableException;

    boolean applyStaticRoutes(final List<StaticRouteProfile> staticRoutes, final List<DomainRouterVO> routers) throws ResourceUnavailableException;

    boolean setupPrivateGateway(final PrivateGateway gateway, final VirtualRouter router) throws ConcurrentOperationException, ResourceUnavailableException;

    String[] applyVpnUsers(final RemoteAccessVpn vpn, final List<? extends VpnUser> users, final VirtualRouter router) throws ResourceUnavailableException;

    // ====== USED FOR GUEST NETWORK AND VCP ====== //

    boolean applyDhcpEntry(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final DeployDestination dest, final DomainRouterVO router)
            throws ResourceUnavailableException;

    boolean applyUserData(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final DeployDestination dest, final DomainRouterVO router)
            throws ResourceUnavailableException;

    boolean applyLoadBalancingRules(Network network, List<LoadBalancingRule> rules, VirtualRouter router) throws ResourceUnavailableException;

    boolean applyFirewallRules(final Network network, final List<? extends FirewallRule> rules, final VirtualRouter router) throws ResourceUnavailableException;

    boolean applyStaticNats(final Network network, final List<? extends StaticNat> rules, final VirtualRouter router) throws ResourceUnavailableException;

    boolean associatePublicIP(final Network network, final List<? extends PublicIpAddress> ipAddress, final VirtualRouter router) throws ResourceUnavailableException;

    String[] applyVpnUsers(final Network network, final List<? extends VpnUser> users, final List<DomainRouterVO> routers) throws ResourceUnavailableException;

    boolean savePasswordToRouter(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final VirtualRouter router) throws ResourceUnavailableException;

    boolean saveSSHPublicKeyToRouter(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final VirtualRouter router,
            final String sshPublicKey) throws ResourceUnavailableException;

    boolean saveUserDataToRouter(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final VirtualRouter router)
            throws ResourceUnavailableException;

    boolean applyRules(final Network network, final VirtualRouter router, final String typeString, final boolean isPodLevelException, final Long podId,
            final boolean failWhenDisconnect, RuleApplierWrapper<RuleApplier> ruleApplier) throws ResourceUnavailableException;

    boolean removeDhcpEntry(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final VirtualRouter virtualRouter) throws ResourceUnavailableException;
}
