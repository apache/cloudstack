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
import com.cloud.network.element.TungstenProviderVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.vm.VMInstanceVO;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricAddressGroupResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricApplicationPolicySetResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricFirewallPolicyResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricFirewallRuleResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricPolicyResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricRuleResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricServiceGroupResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricTagResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricTagTypeResponse;
import org.apache.cloudstack.network.tungsten.dao.TungstenFabricLBHealthMonitorVO;
import org.apache.cloudstack.network.tungsten.model.TungstenRule;

import java.util.List;

public interface TungstenService {

    String getTungstenProjectFqn(Network network);

    List<TungstenProviderVO> getTungstenProviders();

    boolean createPublicNetwork(long zoneId);

    boolean addPublicNetworkSubnet(VlanVO vlanVO);

    boolean removePublicNetworkSubnet(VlanVO vlanVO);

    boolean deletePublicNetwork(long zoneId);

    boolean createManagementNetwork(long zoneId);

    boolean deleteManagementNetwork(long zoneId);

    boolean addManagementNetworkSubnet(HostPodVO pod);

    boolean removeManagementNetworkSubnet(HostPodVO pod);

    boolean updateLoadBalancer(Network network, LoadBalancingRule rule);

    boolean updateLoadBalancerSsl(Network network, LoadBalancingRule loadBalancingRule);

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

    TungstenFabricFirewallRuleResponse createTungstenFirewallRule(long zoneId, final String firewallPolicyUuid, String name, String serviceGroupUuid,
        String action, String srcTagUuid, String srcAddressGroupUuid, String srcNetworkUuid, String direction, String destTagUuid,
        String destAddressGroupUuid, String destNetworkUuid, String tagTypeUuid, int sequence);

    TungstenFabricFirewallPolicyResponse createTungstenFirewallPolicy(long zoneId, String applicationPolicySetUuid, String name, int sequence);

    TungstenFabricApplicationPolicySetResponse createTungstenApplicationPolicySet(long zoneId, String name);

    TungstenFabricRuleResponse addTungstenPolicyRule(final long zoneId, final String policyUuid, final String action,
        final String direction, final String protocol, final String srcNetwork, final String srcIpPrefix,
        final int srcIpPrefixLen, final int srcStartPort, final int srcEndPort, final String destNetwork,
        final String destIpPrefix, final int destIpPrefixLen, final int destStartPort, final int destEndPort);

    List<BaseResponse> listTungstenPolicy(long zoneId, final Long networkId, final Long addressId,
        final String policyUuid);

    List<BaseResponse> listTungstenNetwork(long zoneId, final String networkUuid, final boolean listAll);

    List<BaseResponse> listTungstenNic(long zoneId, final String nicUuid);

    List<BaseResponse> listTungstenVm(long zoneId, final String vmUuid);

    List<BaseResponse> listTungstenPolicyRule(final long zoneId, final String policyUuid, final String ruleUuid);

    List<BaseResponse> listTungstenTags(final long zoneId, final String networkUuid, final String vmUuid,
        final String nicUuid, final String policyUuid, final String applicationPolicySetUuid, final String tagUuid);

    List<BaseResponse> listTungstenTagTypes(final long zoneId, final String tagTypeUuid);

    List<BaseResponse> listTungstenApplicationPolicySet(final long zoneId, final String applicationPolicySetUuid);

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
        final List<String> vmUuids, final List<String> nicUuids, final String policyUuid, final String applicationPolicySetUuid, final String tagUuid);

    TungstenFabricPolicyResponse removeTungstenPolicy(final long zoneId, final String networkUuid,
        final String policyUuid);

    TungstenFabricTagResponse removeTungstenTag(final long zoneId, final List<String> networkUuids,
        final List<String> vmUuids, final List<String> nicUuids, final String policyUuid, final String applicationPolicySetUuid, final String tagUuid);

    void subscribeTungstenEvent();

    boolean createSharedNetwork(Network network, Vlan vlan);

    boolean addTungstenVmSecurityGroup(VMInstanceVO vm);

    boolean removeTungstenVmSecurityGroup(VMInstanceVO vm);

    boolean allocateDnsIpAddress(Network network, Pod pod, String subnetName);

    void deallocateDnsIpAddress(Network network, Pod pod, String subnetName);

    BaseResponse createRoutingLogicalRouter(final long zoneId, final String projectFqn, final String name);

    BaseResponse addNetworkGatewayToLogicalRouter(final long zoneId, final String networkUuid,
        final String logicalRouterUuid);

    List<BaseResponse> listRoutingLogicalRouter(final long zoneId, final String networkUuid, final String logicalRouterUuid);

    BaseResponse removeNetworkGatewayFromLogicalRouter(final long zoneId, final String networkUuid,
        final String logicalRouterUuid);

    boolean deleteLogicalRouter(final long zoneId, final String logicalRouterUuid);

    List<String> listConnectedNetworkFromLogicalRouter(final long zoneId, final String logicalRouterUuid);

    TungstenFabricLBHealthMonitorVO updateTungstenFabricLBHealthMonitor(final long lbId, final String type, final int retry, final int timeout, final int interval, final String httpMethod, final String expectedCode, final String urlPath);

    boolean applyLBHealthMonitor(final long lbId);

    List<BaseResponse> listTungstenFabricLBHealthMonitor(final long lbId);

    String MESSAGE_APPLY_NETWORK_POLICY_EVENT = "Message.ApplyNetworkPolicy.Event";
    String MESSAGE_SYNC_TUNGSTEN_DB_WITH_DOMAINS_AND_PROJECTS_EVENT = "Message.SyncTungstenDnWithDomainsAndProjects.Event";

    enum MonitorType {
        PING, TCP, HTTP
    }

    enum HttpType {
        GET, HEAD
    }
}
