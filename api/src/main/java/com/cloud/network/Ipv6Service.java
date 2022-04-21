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
package com.cloud.network;

import java.util.List;

import org.apache.cloudstack.api.command.user.ipv6.CreateIpv6FirewallRuleCmd;
import org.apache.cloudstack.api.command.user.ipv6.ListIpv6FirewallRulesCmd;
import org.apache.cloudstack.api.command.user.ipv6.UpdateIpv6FirewallRuleCmd;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterGuestIpv6Prefix;
import com.cloud.dc.Vlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.vpc.Vpc;
import com.cloud.utils.Pair;
import com.cloud.utils.component.PluggableService;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;

public interface Ipv6Service extends PluggableService, Configurable {

    public static final int IPV6_SLAAC_CIDR_NETMASK = 64;

    static final ConfigKey<Boolean> Ipv6OfferingCreationEnabled = new ConfigKey<Boolean>("Advanced", Boolean.class,
            "ipv6.offering.enabled",
            "false",
            "Indicates whether creation of IPv6 network/VPC offering is enabled or not.",
            true);

    static final ConfigKey<Integer> Ipv6PrefixSubnetCleanupInterval = new ConfigKey<Integer>("Advanced", Integer.class,
            "network.ipv6.prefix.subnet.cleanup.interval",
            "1800",
            "Determines how long (in seconds) to wait before deallocating prefix subnets which are in Allocating state. The default value = 1800 seconds.",
            true);

    Pair<Integer, Integer> getUsedTotalIpv6SubnetForPrefix(DataCenterGuestIpv6Prefix prefix);

    Pair<Integer, Integer> getUsedTotalIpv6SubnetForZone(long zoneId);

    Pair<String, String> preAllocateIpv6SubnetForNetwork(long zoneId) throws ResourceAllocationException;

    void assignIpv6SubnetToNetwork(String subnet, long networkId);

    void releaseIpv6SubnetForNetwork(long networkId);

    List<String> getAllocatedIpv6FromVlanRange(Vlan vlan);

    Nic assignPublicIpv6ToNetwork(Network network, Nic nic);

    void updateNicIpv6(NicProfile nic, DataCenter dc, Network network) throws InsufficientAddressCapacityException;

    void releasePublicIpv6ForNic(Network network, String nicIpv6Address);

    List<String> getPublicIpv6AddressesForNetwork(Network network);

    void updateIpv6RoutesForVpcResponse(Vpc vpc, VpcResponse response);

    void checkNetworkIpv6Upgrade(Network network) throws InsufficientAddressCapacityException, ResourceAllocationException;

    FirewallRule updateIpv6FirewallRule(UpdateIpv6FirewallRuleCmd updateIpv6FirewallRuleCmd);

    Pair<List<? extends FirewallRule>,Integer> listIpv6FirewallRules(ListIpv6FirewallRulesCmd listIpv6FirewallRulesCmd);

    boolean revokeIpv6FirewallRule(Long id);

    FirewallRule createIpv6FirewallRule(CreateIpv6FirewallRuleCmd createIpv6FirewallRuleCmd) throws NetworkRuleConflictException;

    FirewallRule getIpv6FirewallRule(Long entityId);

    boolean applyIpv6FirewallRule(long id);

    void removePublicIpv6PlaceholderNics(Network network);
}
