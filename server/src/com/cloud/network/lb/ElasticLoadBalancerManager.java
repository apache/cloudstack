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
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.user.Account;

public interface ElasticLoadBalancerManager {
    public static final int DEFAULT_ELB_VM_RAMSIZE = 128;            // 512 MB
    public static final int DEFAULT_ELB_VM_CPU_MHZ = 256;               // 500 MHz

    public boolean applyLoadBalancerRules(Network network, 
            List<? extends FirewallRule> rules) 
            throws ResourceUnavailableException;

    public LoadBalancer handleCreateLoadBalancerRule(CreateLoadBalancerRuleCmd lb, Account caller, long networkId) throws InsufficientAddressCapacityException, NetworkRuleConflictException;
    
    public void handleDeleteLoadBalancerRule(LoadBalancer lb, long callerUserId, Account caller);
}
