/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General License for more details.
 * 
 * You should have received a copy of the GNU General License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.network.rules;

import java.util.List;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.net.Ip;


/**
 * Rules Manager manages the network rules created for different networks.
 */
public interface RulesManager extends RulesService {
    
    boolean applyPortForwardingRules(Ip ip, boolean continueOnError);
    
    /**
     * detectRulesConflict finds conflicts in networking rules.  It checks for
     * conflicts between the following types of netowrking rules;
     *   1. one to one nat ip forwarding
     *   2. port forwarding
     *   3. load balancing
     *   
     * It is possible for two conflicting rules to be added at the same time
     * and conflicts are detected between those two rules.  In this case, it 
     * is possible for both rules to be rolled back when, technically, we should
     * only roll back one of the rules.  However, the chances of that is low
     * and the user can simply re-add one of the rules themselves.
     * 
     * @param newRule the new rule created.
     * @param ipAddress ip address that back up the new rule.
     * @throws NetworkRuleConflictException
     */    
    void detectRulesConflict(FirewallRule newRule, IpAddress ipAddress) throws NetworkRuleConflictException;
    
    void checkIpAndUserVm(IpAddress ipAddress, UserVm userVm, Account caller) throws InvalidParameterValueException, PermissionDeniedException;
    
    boolean revokeAllRules(Ip ip, long userId) throws ResourceUnavailableException;
    
    List<? extends FirewallRule> listFirewallRulesByIp(Ip ip);
    
    /**
     * Returns a list of port forwarding rules that are ready for application
     * to the network elements for this ip.
     * @param ip
     * @return List of PortForwardingRule
     */
    List<? extends PortForwardingRule> listPortForwardingRulesForApplication(Ip ip);
    
    List<? extends PortForwardingRule> gatherPortForwardingRulesForApplication(List<? extends IpAddress> addrs);

	boolean revokePortForwardingRule(long vmId);
	
	FirewallRule[] reservePorts(IpAddress ip, String protocol, FirewallRule.Purpose purpose, int... ports) throws NetworkRuleConflictException;
}
