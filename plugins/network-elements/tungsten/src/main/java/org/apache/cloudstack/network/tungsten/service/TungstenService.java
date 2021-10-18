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
package org.apache.cloudstack.network.tungsten.service;

import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.network.Network;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.vm.VMInstanceVO;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricAddressGroupResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricApplicationPolicySetResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricFirewallPolicyResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricFirewallRuleResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricInterfaceRouteTableResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricInterfaceStaticRouteResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricNetworkRouteTableResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricNetworkStaticRouteResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricPolicyResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricRoutingPolicyResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricRoutingPolicyTermResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricRuleResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricServiceGroupResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricTagResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricTagTypeResponse;
import org.apache.cloudstack.network.tungsten.model.RoutingPolicyPrefix;
import org.apache.cloudstack.network.tungsten.model.RoutingPolicyThenTerm;
import org.apache.cloudstack.network.tungsten.model.TungstenRule;

import java.util.List;

public interface TungstenService {
    String getTungstenProjectFqn(Network network);

    boolean createPublicNetwork(long zoneId);

    boolean addPublicNetworkSubnet(VlanVO vlanVO);

    boolean removePublicNetworkSubnet(VlanVO vlanVO);

    boolean deletePublicNetwork(long zoneId);

    boolean createManagementNetwork(long zoneId);

    boolean deleteManagementNetwork(long zoneId);

    boolean addManagementNetworkSubnet(HostPodVO pod);

    boolean removeManagementNetworkSubnet(HostPodVO pod);

    boolean updateLoadBalancer(Network network, LoadBalancingRule rule);

    boolean synchronizeTungstenData(Long tungstenProviderId);

    boolean addTungstenDefaultNetworkPolicy(long zoneId, String projectFqn, String policyName, String networkUuid,
        List<TungstenRule> ruleList, int majorSequence, int minorSequence);

    TungstenFabricPolicyResponse createTungstenPolicy(long zoneId, String name);

    TungstenFabricTagResponse createTungstenTag(long zoneId, String tagType, String tagValue);

    TungstenFabricTagTypeResponse createTungstenTagType(long zoneId, String name);

    TungstenFabricAddressGroupResponse createTungstenAddressGroup(long zoneId, String name, String ipPrefix,
        int ipPrefixLen);

    TungstenFabricServiceGroupResponse createTungstenServiceGroup(long zoneId, String name, String protocol,
        int startPort, int endPort);

    TungstenFabricFirewallRuleResponse createTungstenFirewallRule(long zoneId, String name, String serviceGroupUuid,
        String action, String srcTagUuid, String srcAddressGroupUuid, String direction, String destTagUuid,
        String destAddressGroupUuid, String tagTypeUuid);

    TungstenFabricFirewallPolicyResponse createTungstenFirewallPolicy(long zoneId, String name);

    TungstenFabricApplicationPolicySetResponse createTungstenApplicationPolicySet(long zoneId, String name);

    TungstenFabricRuleResponse addTungstenPolicyRule(final long zoneId, final String policyUuid, final String action,
        final String direction, final String protocol, final String srcNetwork, final String srcIpPrefix,
        final int srcIpPrefixLen, final int srcStartPort, final int srcEndPort, final String destNetwork,
        final String destIpPrefix, final int destIpPrefixLen, final int destStartPort, final int destEndPort);

    TungstenFabricApplicationPolicySetResponse addTungstenFirewallPolicy(long zoneId, String applicationPolicySetUuid,
        String firewallPolicyUuid, String tagUuid, int sequence);

    TungstenFabricFirewallPolicyResponse addTungstenFirewallRule(long zoneId, String firewallPolicyUuid,
        String firewallRuleUuid, int sequence);

    List<BaseResponse> listTungstenPolicy(long zoneId, final Long networkId, final Long addressId,
        final String policyUuid);

    List<BaseResponse> listTungstenNetwork(long zoneId, final String networkUuid);

    List<BaseResponse> listTungstenNic(long zoneId, final String nicUuid);

    List<BaseResponse> listTungstenVm(long zoneId, final String vmUuid);

    List<BaseResponse> listTungstenPolicyRule(final long zoneId, final String policyUuid, final String ruleUuid);

    List<BaseResponse> listTungstenTags(final long zoneId, final String networkUuid, final String vmUuid,
        final String nicUuid, final String policyUuid, final String tagUuid);

    List<BaseResponse> listTungstenTagTypes(final long zoneId, final String tagTypeUuid);

    List<BaseResponse> listTungstenApplicationPolicySet(final long zoneId, String applicationPolicySetUuid);

    List<BaseResponse> listTungstenFirewallPolicy(final long zoneId, final String applicationPolicySetUuid,
        final String firewallPolicyUuid);

    List<BaseResponse> listTungstenFirewallRule(final long zoneId, final String firewallPolicyUuid,
        final String firewallRuleUuid);

    List<BaseResponse> listTungstenServiceGroup(final long zoneId, final String serviceGroupUuid);

    List<BaseResponse> listTungstenAddressGroup(final long zoneId, final String addressGroupUuid);

    boolean deleteTungstenPolicy(final long zoneId, final String policyUuid);

    TungstenFabricPolicyResponse removeTungstenPolicyRule(final long zoneId, final String policyUuid,
        final String ruleUuid);

    boolean deleteTungstenTag(final long zoneId, final String tagUuid);

    boolean deleteTungstenTagType(final long zoneId, final String tagTypeUuid);

    boolean deleteTungstenApplicationPolicySet(final long zoneId, final String applicationPolicySetUuid);

    boolean deleteTungstenFirewallPolicy(final long zoneId, final String firewallPolicyUuid);

    boolean deleteTungstenFirewallRule(final long zoneId, final String firewallRuleUuid);

    boolean deleteTungstenServiceGroup(final long zoneId, final String serviceGroupUuid);

    boolean deleteTungstenAddressGroup(final long zoneId, final String addressGroupUuid);

    TungstenFabricPolicyResponse applyTungstenPolicy(final long zoneId, final String networkUuid,
        final String policyUuid, final int majorSequence, final int minorSequence);

    TungstenFabricTagResponse applyTungstenTag(final long zoneId, final List<String> networkUuids,
        final List<String> vmUuids, final List<String> nicUuids, final String policyUuid, final String tagUuid);

    TungstenFabricPolicyResponse removeTungstenPolicy(final long zoneId, final String networkUuid,
        final String policyUuid);

    TungstenFabricTagResponse removeTungstenTag(final long zoneId, final List<String> networkUuids,
        final List<String> vmUuids, final List<String> nicUuids, final String policyUuid, final String tagUuid);

    TungstenFabricApplicationPolicySetResponse removeTungstenFirewallPolicy(final long zoneId,
        final String applicationPolicySetUuid, final String firewallPolicyUuid);

    TungstenFabricFirewallPolicyResponse removeTungstenFirewallRule(final long zoneId, final String firewallPolicyUuid,
        final String firewallRuleUuid);

    TungstenFabricNetworkRouteTableResponse createTungstenFabricNetworkRouteTable(final long zoneId,
        final String networkRouteTableName);

    TungstenFabricInterfaceRouteTableResponse createTungstenFabricInterfaceRouteTable(final long zoneId,
        final String interfaceRouteTableName);

    TungstenFabricNetworkStaticRouteResponse addTungstenFabricNetworkStaticRoute(final long zoneId,
        final String networkRouteTableUuid, final String routePrefix, final String routeNextHop,
        final String routeNextHopType, final String communities);

    List<TungstenFabricInterfaceRouteTableResponse> listTungstenFabricInterfaceRouteTables(final long zoneId,
        final String interfaceRouteTableUuid, final String vmiUuid, final boolean isAttachedToInterface);

    TungstenFabricNetworkStaticRouteResponse removeTungstenFabricNetworkStaticRoute(final long zoneId,
        final String routeTableUuid, final String routePrefix);

    TungstenFabricInterfaceStaticRouteResponse addTungstenFabricInterfaceStaticRoute(final long zoneId,
        final String interfaceRouteTableUuid, final String routePrefix, final String communities);

    List<TungstenFabricNetworkRouteTableResponse> listTungstenFabricNetworkRouteTables(final long zoneId,
        final String networkRouteTableUuid, final String networkUuid, final boolean isAttachedToNetwork);

    List<TungstenFabricInterfaceStaticRouteResponse> listTungstenFabricInterfaceStaticRoute(final long zoneId,
        final String interfaceRouteTableUuid, final String routePrefix);

    List<TungstenFabricNetworkStaticRouteResponse> listTungstenFabricNetworkStaticRoute(final long zoneId,
        final String networkRouteTableUuid, final String routePrefix);

    TungstenFabricInterfaceStaticRouteResponse removeTungstenFabricInterfaceStaticRoute(final long zoneId,
        final String routeTableUuid, final String routePrefix);

    boolean removeTungstenFabricNetworkRouteTable(final long zoneId, final String routeTableUuid);

    boolean removeTungstenFabricInterfaceRouteTable(final long zoneId, final String routeTableUuid);

    TungstenFabricNetworkRouteTableResponse addTungstenFabricRouteTableToNetwork(final long zoneId,
        final String networkUuid, final String routeTableUuid);

    TungstenFabricInterfaceRouteTableResponse addTungstenFabricRouteTableToInterface(final long zoneId,
        final String vmInterfaceUuid, final String routeTableUuid);

    boolean removeTungstenFabricRouteTableFromNetwork(final long zoneId, final String networkUuid,
        final String routeTableUuid);

    boolean removeTungstenFabricRouteTableFromInterface(final long zoneId, final String interfaceUuid,
        final String routeTableUuid);

    List<TungstenFabricRoutingPolicyResponse> listTungstenFabricRoutingPolicies(final long zoneId,
        final String routingPolicyUuid, final String networkUuid, final boolean isAttachedToNetwork);

    TungstenFabricRoutingPolicyResponse createTungstenRoutingPolicy(final long zoneId,
        final String routingPolicyName);

    TungstenFabricRoutingPolicyTermResponse addRoutingPolicyTerm(final long zoneId, String routingPolicyUuid,
        List<String> communities, boolean matchAll, List<String> protocolList, List<RoutingPolicyPrefix> prefixList,
        List<RoutingPolicyThenTerm> routingPolicyThenTerms);

    boolean removeRoutingPolicy(final long zoneId, String routingPolicyUuid);

    boolean removeRoutingPolicyTerm(final long zoneId, String routingPolicyUuid, List<String> communities,
                                    boolean matchAll, List<String> protocolList, List<String> prefixList);

    boolean addRoutingPolicyToNetwork(final long zoneId, String networkUuid, String routingPolicyUuid);

    boolean removeRoutingPolicyFromNetwork(final long zoneId, String networkUuid, String routingPolicyUuid);

    void subscribeTungstenEvent();

    boolean createSharedNetwork(Network network, Vlan vlan);

    boolean addTungstenVmSecurityGroup(VMInstanceVO vm);

    boolean removeTungstenVmSecurityGroup(VMInstanceVO vm);

    boolean allocateDnsIpAddress(Network network, Pod pod, String subnetName);

    void deallocateDnsIpAddress(Network network, Pod pod, String subnetName);

    BaseResponse createRoutingLogicalRouter(final long zoneId, final String projectFqn, final String name);

    BaseResponse addNetworkGatewayToLogicalRouter(final long zoneId, final String networkUuid,
        final String logicalRouterUuid);

    List<BaseResponse> listRoutingLogicalRouter(final long zoneId, final String logicalRouterUuid);

    BaseResponse removeNetworkGatewayFromLogicalRouter(final long zoneId, final String networkUuid,
        final String logicalRouterUuid);

    boolean deleteLogicalRouter(final long zoneId, final String logicalRouterUuid);

    String MESSAGE_APPLY_NETWORK_POLICY_EVENT = "Message.ApplyNetworkPolicy.Event";
    String MESSAGE_SYNC_TUNGSTEN_DB_WITH_DOMAINS_AND_PROJECTS_EVENT =
        "Message.SyncTungstenDnWithDomainsAndProjects" + ".Event";
}
