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
package org.apache.cloudstack.service;

import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.SDNProviderNetworkRule;
import com.cloud.network.netris.NetrisNetworkRule;
import com.cloud.network.netris.NetrisService;
import com.cloud.network.vpc.StaticRoute;
import com.cloud.network.vpc.Vpc;

import java.util.List;

public class NetrisServiceMockTest implements NetrisService {
    @Override
    public boolean createIPAMAllocationsForZoneLevelPublicRanges(long zoneId) {
        return true;
    }

    @Override
    public boolean createVpcResource(long zoneId, long accountId, long domainId, Long vpcId, String vpcName, boolean sourceNatEnabled, String cidr, boolean isVpcNetwork) {
        return true;
    }

    @Override
    public boolean updateVpcResource(long zoneId, long accountId, long domainId, Long vpcId, String vpcName, String previousVpcName) {
        return true;
    }

    @Override
    public boolean deleteVpcResource(long zoneId, long accountId, long domainId, Vpc vpc) {
        return true;
    }

    @Override
    public boolean createVnetResource(Long zoneId, long accountId, long domainId, String vpcName, Long vpcId, String networkName, Long networkId, String cidr, Boolean globalRouting) {
        return true;
    }

    @Override
    public boolean updateVnetResource(Long zoneId, long accountId, long domainId, String vpcName, Long vpcId, String networkName, Long networkId, String prevNetworkName) {
        return true;
    }

    @Override
    public boolean deleteVnetResource(long zoneId, long accountId, long domainId, String vpcName, Long vpcId, String networkName, Long networkId, String cidr) {
        return true;
    }

    @Override
    public boolean createSnatRule(long zoneId, long accountId, long domainId, String vpcName, long vpcId, String networkName, long networkId, boolean isForVpc, String vpcCidr, String sourceNatIp) {
        return true;
    }

    @Override
    public boolean createPortForwardingRule(long zoneId, long accountId, long domainId, String vpcName, long vpcId, String networkName, Long networkId, boolean isForVpc, String vpcCidr, SDNProviderNetworkRule networkRule) {
        return true;
    }

    @Override
    public boolean deletePortForwardingRule(long zoneId, long accountId, long domainId, String vpcName, Long vpcId, String networkName, Long networkId, boolean isForVpc, String vpcCidr, SDNProviderNetworkRule networkRule) {
        return true;
    }

    @Override
    public boolean updateVpcSourceNatIp(Vpc vpc, IpAddress address) {
        return true;
    }

    @Override
    public boolean createStaticNatRule(long zoneId, long accountId, long domainId, String networkResourceName, Long networkResourceId, boolean isForVpc, String vpcCidr, String staticNatIp, String vmIp, long vmId) {
        return true;
    }

    @Override
    public boolean deleteStaticNatRule(long zoneId, long accountId, long domainId, String networkResourceName, Long networkResourceId, boolean isForVpc, String staticNatIp, long vmId) {
        return true;
    }

    @Override
    public List<StaticRoute> listStaticRoutes(long zoneId, long accountId, long domainId, String networkResourceName, Long networkResourceId, boolean isForVpc, String prefix, String nextHop, Long routeId) {
        return List.of();
    }

    @Override
    public boolean addFirewallRules(Network network, List<NetrisNetworkRule> firewallRules) {
        return true;
    }

    @Override
    public boolean deleteFirewallRules(Network network, List<NetrisNetworkRule> firewallRules) {
        return true;
    }

    @Override
    public boolean addOrUpdateStaticRoute(long zoneId, long accountId, long domainId, String networkResourceName, Long networkResourceId, boolean isForVpc, String prefix, String nextHop, Long routeId, boolean updateRoute) {
        return true;
    }

    @Override
    public boolean deleteStaticRoute(long zoneId, long accountId, long domainId, String networkResourceName, Long networkResourceId, boolean isForVpc, String prefix, String nextHop, Long routeId) {
        return true;
    }

    @Override
    public boolean releaseNatIp(long zoneId, String publicIp) {
        return true;
    }

    @Override
    public boolean createOrUpdateLbRule(NetrisNetworkRule rule) {
        return true;
    }

    @Override
    public boolean deleteLbRule(NetrisNetworkRule rule) {
        return true;
    }
}
