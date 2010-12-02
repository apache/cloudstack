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

import com.cloud.api.commands.ListIpForwardingRulesCmd;
import com.cloud.api.commands.ListPortForwardingRulesCmd;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.utils.net.Ip;

public interface RulesService {
    List<? extends PortForwardingRule> searchForIpForwardingRules(ListIpForwardingRulesCmd cmd);

    /**
     * List port forwarding rules assigned to an ip address
     * @param cmd the command object holding the criteria for listing port forwarding rules (the ipAddress)
     * @return list of port forwarding rules on the given address, empty list if no rules exist
     */
    public List<? extends PortForwardingRule> listPortForwardingRules(ListPortForwardingRulesCmd cmd);

    PortForwardingRule createIpForwardingRuleInDb(String ipAddr, long virtualMachineId);
    
    PortForwardingRule createIpForwardingRuleOnDomr(long ruleId);

    boolean deleteIpForwardingRule(Long id);
    boolean deletePortForwardingRule(Long id, boolean sysContext);
    
    boolean applyFirewallRules(Ip ip, Account caller) throws ResourceUnavailableException;
    boolean applyNatRules(Ip ip, Account caller) throws ResourceUnavailableException;
    boolean applyPortForwardingRules(Ip ip, Account caller) throws ResourceUnavailableException;
    
    /**
     * Creates a port forwarding rule between two ip addresses or between
     * an ip address and a virtual machine.
     * @param rule rule to be created.
     * @param vmId vm to be linked to.  If specified the destination ip address is ignored.
     * @param caller caller 
     * @return PortForwardingRule if created.
     * @throws NetworkRuleConflictException if conflicts in the network rules are detected.
     */
    PortForwardingRule createPortForwardingRule(PortForwardingRule rule, Long vmId, Account caller) throws NetworkRuleConflictException;
    /**
     * Revokes a port forwarding rule 
     * @param ruleId the id of the rule to revoke.
     * @param caller 
     * @return
     */
    PortForwardingRule revokePortForwardingRule(long ruleId, boolean apply, Account caller);

}
