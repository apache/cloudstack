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

import com.cloud.network.FirewallRuleVO;
import com.cloud.utils.db.GenericDao;

/*
 * Data Access Object for user_ip_address and ip_forwarding tables
 */
public interface FirewallRulesDao extends GenericDao<FirewallRuleVO, Long> {
	public List<FirewallRuleVO> listIPForwarding(String publicIPAddress, boolean forwarding);
	public List<FirewallRuleVO> listIPForwarding(String publicIPAddress, String port, boolean forwarding);

	public List<FirewallRuleVO> listIPForwarding(long userId);
	public List<FirewallRuleVO> listIPForwarding(long userId, long dcId);
	public void deleteIPForwardingByPublicIpAddress(String ipAddress);
	public List<FirewallRuleVO> listIPForwarding(String publicIPAddress);
	public List<FirewallRuleVO> listIPForwardingForUpdate(String publicIPAddress);
	public void disableIPForwarding(String publicIPAddress);
	public List<FirewallRuleVO> listIPForwardingForUpdate(String publicIp, boolean fwding);
	public List<FirewallRuleVO> listIPForwardingForUpdate(String publicIp, String publicPort, String proto);
	public List<FirewallRuleVO> listLoadBalanceRulesForUpdate(String publicIp, String publicPort, String algo);

	public List<FirewallRuleVO> listRulesExcludingPubIpPort(String publicIpAddress, long securityGroupId);
    public List<FirewallRuleVO> listBySecurityGroupId(long securityGroupId);
    public List<FirewallRuleVO> listByLoadBalancerId(long loadBalancerId);
    public List<FirewallRuleVO> listForwardingByPubAndPrivIp(boolean forwarding, String publicIPAddress, String privateIp);
    public FirewallRuleVO findByGroupAndPrivateIp(long groupId, String privateIp, boolean forwarding);
}
