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
package com.cloud.network.nsx;

import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.vpc.Vpc;

import java.util.List;

public interface NsxService {

    boolean createVpcNetwork(Long zoneId, long accountId, long domainId, Long vpcId, String vpcName, boolean sourceNatEnabled);
    boolean updateVpcSourceNatIp(Vpc vpc, IpAddress address);
    boolean createNetwork(Long zoneId, long accountId, long domainId, Long networkId, String networkName);
    boolean deleteVpcNetwork(Long zoneId, long accountId, long domainId, Long vpcId, String vpcName);
    boolean deleteNetwork(long zoneId, long accountId, long domainId, Network network);
    boolean createStaticNatRule(long zoneId, long domainId, long accountId, Long networkResourceId, String networkResourceName,
                                boolean isVpcResource, long vmId, String publicIp, String vmIp);
    boolean deleteStaticNatRule(long zoneId, long domainId, long accountId, Long networkResourceId, String networkResourceName,
                                boolean isVpcResource);
    boolean createPortForwardRule(NsxNetworkRule nsxNetworkRule);
    boolean deletePortForwardRule(NsxNetworkRule nsxNetworkRule);
    boolean createLbRule(NsxNetworkRule nsxNetworkRule);
    boolean deleteLbRule(NsxNetworkRule nsxNetworkRule);
    boolean addFirewallRules(Network network, List<NsxNetworkRule> netRules);
    boolean deleteFirewallRules(Network network, List<NsxNetworkRule> netRules);
}
