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
package com.cloud.network.rules;

import java.util.List;

import com.cloud.api.commands.ListPortForwardingRulesCmd;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;

public interface RulesService {
    List<? extends FirewallRule> searchStaticNatRules(Long ipId,  Long id, Long vmId, Long start, Long size, String accountName, Long domainId, Long projectId, boolean isRecursive, boolean listAll);

    /**
     * Creates a port forwarding rule between two ip addresses or between
     * an ip address and a virtual machine.
     * @param rule rule to be created.
     * @param vmId vm to be linked to.  If specified the destination ip address is ignored.
     * @param openFirewall TODO
     * @return PortForwardingRule if created.
     * @throws NetworkRuleConflictException if conflicts in the network rules are detected.
     */
    PortForwardingRule createPortForwardingRule(PortForwardingRule rule, Long vmId, boolean openFirewall) throws NetworkRuleConflictException;
    
    /**
     * Revokes a port forwarding rule 
     * @param ruleId the id of the rule to revoke.
     * @param caller 
     * @return
     */
    boolean revokePortForwardingRule(long ruleId, boolean apply);
    /**
     * List port forwarding rules assigned to an ip address
     * @param cmd the command object holding the criteria for listing port forwarding rules (the ipAddress)
     * @return list of port forwarding rules on the given address, empty list if no rules exist
     */
    public List<? extends PortForwardingRule> listPortForwardingRules(ListPortForwardingRulesCmd cmd);

    boolean applyPortForwardingRules(long ipAdddressId, Account caller) throws ResourceUnavailableException;
    
    boolean enableStaticNat(long ipAddressId, long vmId) throws NetworkRuleConflictException, ResourceUnavailableException;
        
    PortForwardingRule getPortForwardigRule(long ruleId);
    FirewallRule getFirewallRule(long ruleId);
    
    StaticNatRule createStaticNatRule(StaticNatRule rule, boolean openFirewall) throws NetworkRuleConflictException;
    
    boolean revokeStaticNatRule(long ruleId, boolean apply);
    
    boolean applyStaticNatRules(long ipAdddressId, Account caller) throws ResourceUnavailableException;
    
    StaticNatRule buildStaticNatRule(FirewallRule rule);
    
    List<String> getSourceCidrs(long ruleId);

	boolean disableStaticNat(long ipId) throws ResourceUnavailableException, NetworkRuleConflictException, InsufficientAddressCapacityException;
  
}
