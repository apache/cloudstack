/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

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

}
