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
package org.apache.cloudstack.network;

import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.network.Network;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.vpc.Vpc;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.Pair;
import com.cloud.utils.component.PluggableService;

import org.apache.cloudstack.api.command.admin.network.CreateIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.CreateIpv4SubnetForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.DedicateIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteIpv4SubnetForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.ListIpv4GuestSubnetsCmd;
import org.apache.cloudstack.api.command.admin.network.ListIpv4SubnetsForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.ReleaseDedicatedIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.UpdateIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.user.network.routing.CreateRoutingFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.network.routing.ListRoutingFirewallRulesCmd;
import org.apache.cloudstack.api.command.user.network.routing.UpdateRoutingFirewallRuleCmd;
import org.apache.cloudstack.api.response.DataCenterIpv4SubnetResponse;
import org.apache.cloudstack.api.response.Ipv4SubnetForGuestNetworkResponse;
import org.apache.cloudstack.datacenter.DataCenterIpv4GuestSubnet;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import java.util.List;

public interface RoutedIpv4Manager extends PluggableService, Configurable {

    ConfigKey<Integer> RoutedNetworkMaxCidrSize = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Integer.class,
            "routed.network.max.cidr.size",
            "30",
            "The maximum cidr size of routed network.",
            true,
            ConfigKey.Scope.Account);

    ConfigKey<Integer> RoutedNetworkMinCidrSize = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Integer.class,
            "routed.network.min.cidr.size",
            "24",
            "The minimum cidr size of routed network.",
            true,
            ConfigKey.Scope.Account);

    ConfigKey<Boolean> RoutedNetworkCidrAutoAllocationEnabled = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Boolean.class,
            "routed.network.cidr.auto.allocation.enabled",
            "true",
            "Indicates whether the auto-allocation of network CIDR for routed network is enabled or not.",
            true,
            ConfigKey.Scope.Account);

    // Methods for DataCenterIpv4GuestSubnet APIs
    DataCenterIpv4GuestSubnet createDataCenterIpv4GuestSubnet(CreateIpv4GuestSubnetCmd createIpv4GuestSubnetCmd);

    DataCenterIpv4SubnetResponse createDataCenterIpv4SubnetResponse(DataCenterIpv4GuestSubnet result);

    boolean deleteDataCenterIpv4GuestSubnet(DeleteIpv4GuestSubnetCmd deleteIpv4GuestSubnetCmd);

    DataCenterIpv4GuestSubnet updateDataCenterIpv4GuestSubnet(UpdateIpv4GuestSubnetCmd updateIpv4GuestSubnetCmd);

    List<? extends DataCenterIpv4GuestSubnet> listDataCenterIpv4GuestSubnets(ListIpv4GuestSubnetsCmd listIpv4GuestSubnetsCmd);

    DataCenterIpv4GuestSubnet dedicateDataCenterIpv4GuestSubnet(DedicateIpv4GuestSubnetCmd dedicateIpv4GuestSubnetCmd);

    DataCenterIpv4GuestSubnet releaseDedicatedDataCenterIpv4GuestSubnet(ReleaseDedicatedIpv4GuestSubnetCmd releaseDedicatedIpv4GuestSubnetCmd);

    // Methods for Ipv4SubnetForGuestNetwork APIs
    Ipv4GuestSubnetNetworkMap createIpv4SubnetForGuestNetwork(CreateIpv4SubnetForGuestNetworkCmd createIpv4SubnetForGuestNetworkCmd);

    boolean deleteIpv4SubnetForGuestNetwork(DeleteIpv4SubnetForGuestNetworkCmd deleteIpv4SubnetForGuestNetworkCmd);

    boolean releaseIpv4SubnetForGuestNetwork(long networkId);

    List<? extends Ipv4GuestSubnetNetworkMap> listIpv4GuestSubnetsForGuestNetwork(ListIpv4SubnetsForGuestNetworkCmd listIpv4SubnetsForGuestNetworkCmd);

    Ipv4SubnetForGuestNetworkResponse createIpv4SubnetForGuestNetworkResponse(Ipv4GuestSubnetNetworkMap subnet);

    // Methods for internal calls
    void getOrCreateIpv4SubnetForGuestNetwork(Network network, String networkCidr);

    Ipv4GuestSubnetNetworkMap getOrCreateIpv4SubnetForGuestNetwork(Network network, Integer networkCidrSize);

    void assignIpv4SubnetToNetwork(String cidr, long networkId);

    // Methods for Routing firewall rules
    FirewallRule createRoutingFirewallRule(CreateRoutingFirewallRuleCmd createRoutingFirewallRuleCmd) throws NetworkRuleConflictException;

    Pair<List<? extends FirewallRule>, Integer> listRoutingFirewallRules(ListRoutingFirewallRulesCmd listRoutingFirewallRulesCmd);

    FirewallRule updateRoutingFirewallRule(UpdateRoutingFirewallRuleCmd updateRoutingFirewallRuleCmd);

    boolean revokeRoutingFirewallRule(Long id);

    boolean applyRoutingFirewallRule(long id);

    boolean isVirtualRouterGateway(Network network);

    boolean isVirtualRouterGateway(NetworkOffering networkOffering);

    boolean isRoutedNetwork(Network network);

    boolean isRoutedVpc(Vpc vpc);

}
