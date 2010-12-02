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

import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.net.Ip;

/*
 * Data Access Object for user_ip_address and ip_forwarding tables
 */
public interface FirewallRulesDao extends GenericDao<FirewallRuleVO, Long> {
    List<FirewallRuleVO> listByIpAndNotRevoked(Ip ip);
    
    boolean setStateToAdd(FirewallRuleVO rule);
    
    boolean revoke(FirewallRuleVO rule);
    
//	public List<PortForwardingRuleVO> listIPForwarding(String publicIPAddress, boolean forwarding);
//	public List<PortForwardingRuleVO> listIPForwarding(String publicIPAddress, String port, boolean forwarding);
//
//	public List<PortForwardingRuleVO> listIPForwarding(long userId);
//	public List<PortForwardingRuleVO> listIPForwarding(long userId, long dcId);
//	public void deleteIPForwardingByPublicIpAddress(String ipAddress);
//	public List<PortForwardingRuleVO> listIPForwarding(String publicIPAddress);
//	public List<PortForwardingRuleVO> listIPForwardingForUpdate(String publicIPAddress);
//	public void disableIPForwarding(String publicIPAddress);
//	public List<PortForwardingRuleVO> listIPForwardingForUpdate(String publicIp, boolean fwding);
//	public List<PortForwardingRuleVO> listIPForwardingForUpdate(String publicIp, String publicPort, String proto);
//	public List<PortForwardingRuleVO> listIPForwardingByPortAndProto(String publicIp, String publicPort, String proto);
//
//	public List<PortForwardingRuleVO> listLoadBalanceRulesForUpdate(String publicIp, String publicPort, String algo);
//	public List<PortForwardingRuleVO> listIpForwardingRulesForLoadBalancers(String publicIp);
//
//
//	public List<PortForwardingRuleVO> listRulesExcludingPubIpPort(String publicIpAddress, long securityGroupId);
//    public List<PortForwardingRuleVO> listBySecurityGroupId(long securityGroupId);
//    public List<PortForwardingRuleVO> listByLoadBalancerId(long loadBalancerId);
//    public List<PortForwardingRuleVO> listForwardingByPubAndPrivIp(boolean forwarding, String publicIPAddress, String privateIp);
//    public PortForwardingRuleVO findByGroupAndPrivateIp(long groupId, String privateIp, boolean forwarding);
//	public List<PortForwardingRuleVO> findByPublicIpPrivateIpForNatRule(String publicIp,String privateIp);
//	public List<PortForwardingRuleVO> listByPrivateIp(String privateIp);
//	public boolean isPublicIpOneToOneNATted(String publicIp);
//	void deleteIPForwardingByPublicIpAndPort(String ipAddress, String port);
//	public List<PortForwardingRuleVO> listIPForwardingForLB(long userId, long dcId);
}
