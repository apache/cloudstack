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
import java.util.Map;

import com.cloud.dc.DataCenter;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.RuleApplier;
import com.cloud.network.rules.RuleApplierWrapper;
import com.cloud.network.rules.StaticNat;
import com.cloud.user.Account;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfile.Param;

public interface NetworkTopology {

    List<DomainRouterVO> findOrDeployVirtualRouterInGuestNetwork(final Network guestNetwork, final DeployDestination dest, Account owner, final boolean isRedundant,
            final Map<Param, Object> params) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException;

    StringBuilder createGuestBootLoadArgs(final NicProfile guestNic, final String defaultDns1, final String defaultDns2, DomainRouterVO router);

    String retrieveGuestDhcpRange(final NicProfile guestNic, final Network guestNetwork, final DataCenter dc);

    NicProfile retrieveControlNic(final VirtualMachineProfile profile);

    boolean configDhcpForSubnet(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final DeployDestination dest, final List<DomainRouterVO> routers)
            throws ResourceUnavailableException;

    boolean applyDhcpEntry(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final DeployDestination dest, final List<DomainRouterVO> routers)
            throws ResourceUnavailableException;

    boolean applyUserData(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final DeployDestination dest, final List<DomainRouterVO> routers)
            throws ResourceUnavailableException;

    boolean applyRules(final Network network, final List<? extends VirtualRouter> routers, final String typeString, final boolean isPodLevelException, final Long podId,
            final boolean failWhenDisconnect, RuleApplierWrapper<RuleApplier> ruleApplier) throws ResourceUnavailableException;

    boolean applyLoadBalancingRules(Network network, List<LoadBalancingRule> rules, List<? extends VirtualRouter> routers) throws ResourceUnavailableException;

    boolean applyFirewallRules(final Network network, final List<? extends FirewallRule> rules, final List<? extends VirtualRouter> routers) throws ResourceUnavailableException;

    boolean applyStaticNats(final Network network, final List<? extends StaticNat> rules, final List<? extends VirtualRouter> routers) throws ResourceUnavailableException;
}