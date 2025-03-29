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
import com.cloud.network.vpc.Vpc;

import java.util.List;

public interface NetrisService {
    boolean createIPAMAllocationsForZoneLevelPublicRanges(long zoneId);

    boolean createVpcResource(long zoneId, long accountId, long domainId, Long vpcId, String vpcName, boolean sourceNatEnabled, String cidr, boolean isVpcNetwork);

    boolean updateVpcResource(long zoneId, long accountId, long domainId, Long vpcId, String vpcName, String previousVpcName);

    boolean deleteVpcResource(long zoneId, long accountId, long domainId, Vpc vpc);

    boolean createVnetResource(Long zoneId, long accountId, long domainId, String vpcName, Long vpcId, String networkName, Long networkId, String cidr, Boolean globalRouting);

    boolean updateVnetResource(Long zoneId, long accountId, long domainId, String vpcName, Long vpcId, String networkName, Long networkId, String prevNetworkName);

    boolean deleteVnetResource(long zoneId, long accountId, long domainId, String vpcName, Long vpcId, String networkName, Long networkId, String cidr);

    boolean createSnatRule(long zoneId, long accountId, long domainId, String vpcName, long vpcId, String networkName, long networkId, boolean isForVpc, String vpcCidr, String sourceNatIp);

    boolean createPortForwardingRule(long zoneId, long accountId, long domainId, String vpcName, long vpcId, String networkName, Long networkId, boolean isForVpc, String vpcCidr, SDNProviderNetworkRule networkRule);

    boolean deletePortForwardingRule(long zoneId, long accountId, long domainId, String vpcName, Long vpcId, String networkName, Long networkId, boolean isForVpc, String vpcCidr, SDNProviderNetworkRule networkRule);

    boolean updateVpcSourceNatIp(Vpc vpc, IpAddress address);

    boolean createStaticNatRule(long zoneId, long accountId, long domainId, String networkResourceName, Long networkResourceId, boolean isForVpc, String vpcCidr, String staticNatIp, String vmIp);

    boolean deleteStaticNatRule(long zoneId, long accountId, long domainId, String networkResourceName, Long networkResourceId, boolean isForVpc, String staticNatIp);

    boolean addFirewallRules(Network network, List<NetrisNetworkRule> firewallRules);
    boolean deleteFirewallRules(Network network, List<NetrisNetworkRule> firewallRules);

    boolean addOrUpdateStaticRoute(long zoneId, long accountId, long domainId, String networkResourceName, Long networkResourceId, boolean isForVpc, String prefix, String nextHop, Long routeId, boolean updateRoute);

    boolean deleteStaticRoute(long zoneId, long accountId, long domainId, String networkResourceName, Long networkResourceId, boolean isForVpc, String prefix, String nextHop, Long routeId);

    boolean releaseNatIp(long zoneId, String publicIp);

    boolean createOrUpdateLbRule(NetrisNetworkRule rule);
    boolean deleteLbRule(NetrisNetworkRule rule);
}
