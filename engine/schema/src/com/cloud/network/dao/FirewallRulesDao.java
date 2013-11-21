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
package com.cloud.network.dao;

import java.util.List;

import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.utils.db.GenericDao;

/*
 * Data Access Object for user_ip_address and ip_forwarding tables
 */
public interface FirewallRulesDao extends GenericDao<FirewallRuleVO, Long> {

    List<FirewallRuleVO> listByIpAndPurposeAndNotRevoked(long ipAddressId, FirewallRule.Purpose purpose);

    List<FirewallRuleVO> listByNetworkAndPurposeAndNotRevoked(long networkId, FirewallRule.Purpose purpose);

    boolean setStateToAdd(FirewallRuleVO rule);

    boolean revoke(FirewallRuleVO rule);

    boolean releasePorts(long ipAddressId, String protocol, FirewallRule.Purpose purpose, int[] ports);

    List<FirewallRuleVO> listByIpAndPurpose(long ipAddressId, FirewallRule.Purpose purpose);

    List<FirewallRuleVO> listByNetworkAndPurpose(long networkId, FirewallRule.Purpose purpose);

    List<FirewallRuleVO> listStaticNatByVmId(long vmId);

    List<FirewallRuleVO> listByIpPurposeAndProtocolAndNotRevoked(long ipAddressId, Integer startPort, Integer endPort, String protocol, FirewallRule.Purpose purpose);

    FirewallRuleVO findByRelatedId(long ruleId);

    List<FirewallRuleVO> listSystemRules();

    List<FirewallRuleVO> listByIp(long ipAddressId);

    List<FirewallRuleVO> listByIpAndNotRevoked(long ipAddressId);

    long countRulesByIpId(long sourceIpId);

    long countRulesByIpIdAndState(long sourceIpId, FirewallRule.State state);

    List<FirewallRuleVO> listByNetworkPurposeTrafficTypeAndNotRevoked(long networkId, FirewallRule.Purpose purpose, FirewallRule.TrafficType trafficType);

    List<FirewallRuleVO> listByNetworkPurposeTrafficType(long networkId, FirewallRule.Purpose purpose, FirewallRule.TrafficType trafficType);

    List<FirewallRuleVO> listByIpAndPurposeWithState(Long addressId, FirewallRule.Purpose purpose, FirewallRule.State state);

    void loadSourceCidrs(FirewallRuleVO rule);
}
