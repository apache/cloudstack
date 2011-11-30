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
package com.cloud.network.lb;

import java.util.List;

import com.cloud.api.commands.CreateLoadBalancerRuleCmd;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.rules.LbStickinessMethod;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.user.Account;

public interface LoadBalancingRulesManager extends LoadBalancingRulesService {
    
    LoadBalancer createLoadBalancer(CreateLoadBalancerRuleCmd lb, boolean openFirewall) throws NetworkRuleConflictException;
    
    boolean removeAllLoadBalanacersForIp(long ipId, Account caller, long callerUserId);
    boolean removeAllLoadBalanacersForNetwork(long networkId, Account caller, long callerUserId);
    List<LbDestination> getExistingDestinations(long lbId);
    List<LbStickinessPolicy> getStickinessPolicies(long lbId);
    List<LbStickinessMethod> getStickinessMethods(long networkid);
    
    /**
     * Remove vm from all load balancers
     * @param vmId
     * @return true if removal is successful
     */
    boolean removeVmFromLoadBalancers(long vmId);
    
    boolean applyLoadBalancersForNetwork(long networkId) throws ResourceUnavailableException;
}
