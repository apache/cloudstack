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
package com.cloud.network.netris;

import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.SDNProviderNetworkRule;
import com.cloud.network.vpc.StaticRoute;
import com.cloud.network.vpc.Vpc;

import java.util.List;

/**
 * Interface for Netris Services that provides methods to manage VPCs, networks,
 * NAT rules, network rules, and static routes in an SDN (Software Defined Networking) environment.
 */

public interface NetrisService {

    /**
     * Creates IPAM (IP Address Management) allocations for zone-level public ranges.
     *
     * @param zoneId the ID of the zone
     * @return true if the operation is successful, false otherwise
     */
    boolean createIPAMAllocationsForZoneLevelPublicRanges(long zoneId);

    /**
     * Creates a VPC (Virtual Private Cloud) resource.
     *
     * @param zoneId           the ID of the zone
     * @param accountId        the ID of the account
     * @param domainId         the ID of the domain
     * @param vpcId            the ID of the VPC
     * @param vpcName          the name of the VPC
     * @param sourceNatEnabled true if source NAT is enabled
     * @param cidr             the CIDR of the VPC
     * @param isVpcNetwork     true if it is a VPC network
     * @return true if the operation is successful, false otherwise
     */
    boolean createVpcResource(long zoneId, long accountId, long domainId, Long vpcId, String vpcName, boolean sourceNatEnabled, String cidr, boolean isVpcNetwork);

    /**
     * Updates an existing VPC resource.
     *
     * @param zoneId           the ID of the zone
     * @param accountId        the ID of the account
     * @param domainId         the ID of the domain
     * @param vpcId            the ID of the VPC
     * @param vpcName          the new name of the VPC
     * @param previousVpcName  the previous name of the VPC
     * @return true if the operation is successful, false otherwise
     */
    boolean updateVpcResource(long zoneId, long accountId, long domainId, Long vpcId, String vpcName, String previousVpcName);

    /**
     * Deletes a VPC resource.
     *
     * @param zoneId   the ID of the zone
     * @param accountId the ID of the account
     * @param domainId the ID of the domain
     * @param vpc      the VPC to delete
     * @return true if the operation is successful, false otherwise
     */
    boolean deleteVpcResource(long zoneId, long accountId, long domainId, Vpc vpc);

    /**
     * Creates a virtual network (vNet) resource.
     *
     * @param zoneId           the ID of the zone
     * @param accountId        the ID of the account
     * @param domainId         the ID of the domain
     * @param vpcName          the name of the VPC
     * @param vpcId            the ID of the VPC
     * @param networkName      the name of the network
     * @param networkId        the ID of the network
     * @param cidr             the CIDR of the network
     * @param globalRouting    true if global routing is enabled
     * @return true if the operation is successful, false otherwise
     */
    boolean createVnetResource(Long zoneId, long accountId, long domainId, String vpcName, Long vpcId, String networkName, Long networkId, String cidr, Boolean globalRouting);

    /**
     * Updates an existing vNet resource.
     *
     * @param zoneId          the ID of the zone
     * @param accountId       the ID of the account
     * @param domainId        the ID of the domain
     * @param vpcName         the name of the VPC
     * @param vpcId           the ID of the VPC
     * @param networkName     the new name of the network
     * @param networkId       the ID of the network
     * @param prevNetworkName the previous name of the network
     * @return true if the operation is successful, false otherwise
     */
    boolean updateVnetResource(Long zoneId, long accountId, long domainId, String vpcName, Long vpcId, String networkName, Long networkId, String prevNetworkName);

    /**
     * Deletes an existing vNet resource.
     *
     * @param zoneId       the ID of the zone
     * @param accountId    the ID of the account
     * @param domainId     the ID of the domain
     * @param vpcName      the name of the VPC
     * @param vpcId        the ID of the VPC
     * @param networkName  the name of the network
     * @param networkId    the ID of the network
     * @param cidr         the CIDR of the network
     * @return true if the operation is successful, false otherwise
     */
    boolean deleteVnetResource(long zoneId, long accountId, long domainId, String vpcName, Long vpcId, String networkName, Long networkId, String cidr);

    /**
     * Creates a source NAT rule for a VPC or network.
     *
     * @param zoneId          the ID of the zone
     * @param accountId       the ID of the account
     * @param domainId        the ID of the domain
     * @param vpcName         the name of the VPC
     * @param vpcId           the ID of the VPC
     * @param networkName     the name of the network
     * @param networkId       the ID of the network
     * @param isForVpc        true if the rule applies to a VPC
     * @param vpcCidr         the VPC CIDR
     * @param sourceNatIp     the source NAT IP
     * @return true if the operation is successful, false otherwise
     */
    boolean createSnatRule(long zoneId, long accountId, long domainId, String vpcName, long vpcId, String networkName, long networkId, boolean isForVpc, String vpcCidr, String sourceNatIp);

    /**
     * Creates a port forwarding rule for a VPC or network.
     *
     * @param zoneId          the ID of the zone
     * @param accountId       the ID of the account
     * @param domainId        the ID of the domain
     * @param vpcName         the name of the VPC
     * @param vpcId           the ID of the VPC
     * @param networkName     the name of the network
     * @param networkId       the ID of the network
     * @param isForVpc        true if the rule applies to a VPC
     * @param vpcCidr         the VPC CIDR
     * @param networkRule     the network rule to forward
     * @return true if the operation is successful, false otherwise
     */
    boolean createPortForwardingRule(long zoneId, long accountId, long domainId, String vpcName, long vpcId, String networkName, Long networkId, boolean isForVpc, String vpcCidr, SDNProviderNetworkRule networkRule);

    /**
     * Deletes a port forwarding rule for a VPC or network.
     *
     * @param zoneId          the ID of the zone
     * @param accountId       the ID of the account
     * @param domainId        the ID of the domain
     * @param vpcName         the name of the VPC
     * @param vpcId           the ID of the VPC
     * @param networkName     the name of the network
     * @param networkId       the ID of the network
     * @param isForVpc        true if the rule applies to a VPC
     * @param vpcCidr         the VPC CIDR
     * @param networkRule     the network rule to remove
     * @return true if the operation is successful, false otherwise
     */
    boolean deletePortForwardingRule(long zoneId, long accountId, long domainId, String vpcName, Long vpcId, String networkName, Long networkId, boolean isForVpc, String vpcCidr, SDNProviderNetworkRule networkRule);

    /**
     * Updates the source NAT IP for a specified VPC.
     *
     * @param vpc     the VPC to updates
     * @param address the new source NAT IP address
     * @return true if the operation is successful, false otherwise
     */
    boolean updateVpcSourceNatIp(Vpc vpc, IpAddress address);

    /**
     * Creates a static NAT rule for a specific VM.
     *
     * @param zoneId             the ID of the zone
     * @param accountId          the ID of the account
     * @param domainId           the ID of the domain
     * @param networkResourceName the name of the network resource
     * @param networkResourceId  the ID of the network resource
     * @param isForVpc           true if the rule applies to a VPC
     * @param vpcCidr            the VPC CIDR
     * @param staticNatIp        the static NAT IP
     * @param vmIp               the VM's IP address
     * @param vmId               the ID of the VM
     * @return true if the operation is successful, false otherwise
     */
    boolean createStaticNatRule(long zoneId, long accountId, long domainId, String networkResourceName, Long networkResourceId, boolean isForVpc, String vpcCidr, String staticNatIp, String vmIp, long vmId);

    /**
     * Deletes a static NAT rule for a specific VM.
     *
     * @param zoneId             the ID of the zone
     * @param accountId          the ID of the account
     * @param domainId           the ID of the domain
     * @param networkResourceName the name of the network resource
     * @param networkResourceId  the ID of the network resource
     * @param isForVpc           true if the rule applies to a VPC
     * @param staticNatIp        the static NAT IP
     * @param vmId               the ID of the VM
     * @return true if the operation is successful, false otherwise
     */
    boolean deleteStaticNatRule(long zoneId, long accountId, long domainId, String networkResourceName, Long networkResourceId, boolean isForVpc, String staticNatIp, long vmId);

    /**
     * Adds firewall rules to a specific network.
     *
     * @param network       the target network
     * @param firewallRules the list of firewall rules to add
     * @return true if the operation is successful, false otherwise
     */
    boolean addFirewallRules(Network network, List<NetrisNetworkRule> firewallRules);

    /**
     * Deletes firewall rules from a specific network.
     *
     * @param network       the target network
     * @param firewallRules the list of firewall rules to delete
     * @return true if the operation is successful, false otherwise
     */
    boolean deleteFirewallRules(Network network, List<NetrisNetworkRule> firewallRules);

    /**
     * Adds or updates a static route for a specific network or VPC.
     *
     * @param zoneId             the ID of the zone
     * @param accountId          the ID of the account
     * @param domainId           the ID of the domain
     * @param networkResourceName the name of the network resource
     * @param networkResourceId  the ID of the network resource
     * @param isForVpc           true if it is for a VPC
     * @param prefix             the IP prefix of the route
     * @param nextHop            the next hop address
     * @param routeId            the ID of the route
     * @param updateRoute        true if the route should be updated
     * @return true if the operation is successful, false otherwise
     */
    boolean addOrUpdateStaticRoute(long zoneId, long accountId, long domainId, String networkResourceName, Long networkResourceId, boolean isForVpc, String prefix, String nextHop, Long routeId, boolean updateRoute);

    /**
     * Deletes a specific static route for a network or VPC.
     *
     * @param zoneId             the ID of the zone
     * @param accountId          the ID of the account
     * @param domainId           the ID of the domain
     * @param networkResourceName the name of the network resource
     * @param networkResourceId  the ID of the network resource
     * @param isForVpc           true if it is for a VPC
     * @param prefix             the IP prefix of the route
     * @param nextHop            the next hop address
     * @param routeId            the ID of the route
     * @return true if the operation is successful, false otherwise
     */
    boolean deleteStaticRoute(long zoneId, long accountId, long domainId, String networkResourceName, Long networkResourceId, boolean isForVpc, String prefix, String nextHop, Long routeId);

    /**
     * Lists static routes for a specific network or VPC.
     *
     * @param zoneId             the ID of the zone
     * @param accountId          the ID of the account
     * @param domainId           the ID of the domain
     * @param networkResourceName the name of the network resource
     * @param networkResourceId  the ID of the network resource
     * @param isForVpc           true if it is for a VPC
     * @param prefix             the IP prefix of the route
     * @param nextHop            the next hop address
     * @param routeId            the ID of the route
     * @return a list of static routes
     */
    List<StaticRoute> listStaticRoutes(long zoneId, long accountId, long domainId, String networkResourceName, Long networkResourceId, boolean isForVpc, String prefix, String nextHop, Long routeId);

    /**
     * Releases a NAT IP address.
     *
     * @param zoneId   the ID of the zone
     * @param publicIp the public NAT IP to release
     * @return true if the operation is successful, false otherwise
     */
    boolean releaseNatIp(long zoneId, String publicIp);

    /**
     * Creates or updates a load balancer (LB) rule.
     *
     * @param rule the network rule for the load balancer
     * @return true if the operation is successful, false otherwise
     */
    boolean createOrUpdateLbRule(NetrisNetworkRule rule);

    /**
     * Deletes a load balancer (LB) rule.
     *
     * @param rule the network rule to delete
     * @return true if the operation is successful, false otherwise
     */
    boolean deleteLbRule(NetrisNetworkRule rule);
}
