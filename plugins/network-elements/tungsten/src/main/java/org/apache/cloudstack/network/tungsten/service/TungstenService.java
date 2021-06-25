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
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.network.Network;
import com.cloud.network.lb.LoadBalancingRule;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricAddressGroupResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricApplicationPolicySetResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricFirewallPolicyResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricFirewallRuleResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricNetworkResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricNicResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricPolicyResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricRuleResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricServiceGroupResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricTagResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricTagTypeResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricVmResponse;
import org.apache.cloudstack.network.tungsten.model.TungstenRule;

import java.util.List;

public interface TungstenService {
    String getTungstenProjectFqn(Network network);

    boolean createPublicNetwork(long zoneId);

    boolean addPublicNetworkSubnet(Vlan vlan);

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

    List<TungstenFabricPolicyResponse> listTungstenPolicy(long zoneId, final Long networkId, final Long addressId,
        final String policyUuid);

    List<TungstenFabricNetworkResponse> listTungstenNetwork(long zoneId, final String networkUuid);

    List<TungstenFabricNicResponse> listTungstenNic(long zoneId, final String nicUuid);

    List<TungstenFabricVmResponse> listTungstenVm(long zoneId, final String vmUuid);

    List<TungstenFabricRuleResponse> listTungstenPolicyRule(final long zoneId, final String policyUuid,
        final String ruleUuid);

    List<TungstenFabricTagResponse> listTungstenTags(final long zoneId, final String networkUuid, final String vmUuid,
        final String nicUuid, final String policyUuid, final String tagUuid);

    List<TungstenFabricTagTypeResponse> listTungstenTagTypes(final long zoneId, final String tagTypeUuid);

    List<TungstenFabricApplicationPolicySetResponse> listTungstenApplicationPolicySet(final long zoneId,
        String applicationPolicySetUuid);

    List<TungstenFabricFirewallPolicyResponse> listTungstenFirewallPolicy(final long zoneId,
        final String applicationPolicySetUuid, final String firewallPolicyUuid);

    List<TungstenFabricFirewallRuleResponse> listTungstenFirewallRule(final long zoneId,
        final String firewallPolicyUuid, final String firewallRuleUuid);

    List<TungstenFabricServiceGroupResponse> listTungstenServiceGroup(final long zoneId, final String serviceGroupUuid);

    List<TungstenFabricAddressGroupResponse> listTungstenAddressGroup(final long zoneId, final String addressGroupUuid);

    boolean deleteTungstenPolicy(final long zoneId, final String policyUuid);

    TungstenFabricPolicyResponse removeTungstenPolicyRule(final long zoneId, final String policyUuid, final String ruleUuid);

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

    void subscribeTungstenEvent();

    String MESSAGE_APPLY_NETWORK_POLICY_EVENT = "Message.ApplyNetworkPolicy.Event";
    String MESSAGE_SYNC_TUNGSTEN_DB_WITH_DOMAINS_AND_PROJECTS_EVENT =
        "Message.SyncTungstenDnWithDomainsAndProjects" + ".Event";
}
