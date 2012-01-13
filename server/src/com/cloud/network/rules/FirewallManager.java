/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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
package com.cloud.network.rules;

import java.util.List;

import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IPAddressVO;
import com.cloud.network.IpAddress;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.rules.FirewallRule.FirewallRuleType;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.user.Account;

public interface FirewallManager extends FirewallService{
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
    
    void validateFirewallRule(Account caller, IPAddressVO ipAddress, Integer portStart, Integer portEnd, String proto, Purpose purpose, FirewallRuleType type);
    
    boolean applyRules(List<? extends FirewallRule> rules, boolean continueOnError, boolean updateRulesInDB) throws ResourceUnavailableException;

    boolean applyFirewallRules(List<FirewallRuleVO> rules, boolean continueOnError, Account caller);

    public void revokeRule(FirewallRuleVO rule, Account caller, long userId, boolean needUsageEvent);

    boolean revokeFirewallRulesForIp(long ipId, long userId, Account caller) throws ResourceUnavailableException;
    
    /**
     * Revokes a firewall rule 
     * @param ruleId the id of the rule to revoke.
     * @param caller TODO
     * @param userId TODO
     * @return
     */
    boolean revokeFirewallRule(long ruleId, boolean apply, Account caller, long userId);

    FirewallRule createFirewallRule(long ipAddrId, Account caller, String xId, Integer portStart, Integer portEnd, String protocol, List<String> sourceCidrList, Integer icmpCode, Integer icmpType, Long relatedRuleId, FirewallRule.FirewallRuleType type)
            throws NetworkRuleConflictException;

    FirewallRule createRuleForAllCidrs(long ipAddrId, Account caller, Integer startPort, Integer endPort, String protocol, Integer icmpCode, Integer icmpType, Long relatedRuleId) throws NetworkRuleConflictException;

    boolean revokeAllFirewallRulesForNetwork(long networkId, long userId, Account caller) throws ResourceUnavailableException;

    boolean revokeFirewallRulesForVm(long vmId);
    
    boolean addSystemFirewallRules(IPAddressVO ip, Account acct);
}
