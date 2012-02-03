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

import com.cloud.api.commands.CreateLBStickinessPolicyCmd;
import com.cloud.api.commands.CreateLoadBalancerRuleCmd;
import com.cloud.api.commands.ListLBStickinessPoliciesCmd;
import com.cloud.api.commands.ListLoadBalancerRuleInstancesCmd;
import com.cloud.api.commands.ListLoadBalancerRulesCmd;
import com.cloud.api.commands.UpdateLoadBalancerRuleCmd;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.StickinessPolicy;
import com.cloud.uservm.UserVm;

public interface LoadBalancingRulesService {
    /**
     * Create a load balancer rule from the given ipAddress/port to the given private port
     * 
     * @param openFirewall
     *            TODO
     * @param cmd
     *            the command specifying the ip address, public port, protocol, private port, and algorithm
     * @return the newly created LoadBalancerVO if successful, null otherwise
     * @throws InsufficientAddressCapacityException
     */
    LoadBalancer createLoadBalancerRule(CreateLoadBalancerRuleCmd lb, boolean openFirewall) throws NetworkRuleConflictException, InsufficientAddressCapacityException;

    LoadBalancer updateLoadBalancerRule(UpdateLoadBalancerRuleCmd cmd);

    boolean deleteLoadBalancerRule(long lbRuleId, boolean apply);

    /**
     * Create a stickiness policy to a load balancer from the given stickiness method name and parameters in
     * (name,value) pairs.
     * 
     * @param cmd
     *            the command specifying the stickiness method name, params (name,value pairs), policy name and
     *            description.
     * @return the newly created stickiness policy if successfull, null otherwise
     * @thows NetworkRuleConflictException
     */
    public StickinessPolicy createLBStickinessPolicy(CreateLBStickinessPolicyCmd cmd) throws NetworkRuleConflictException;

    public boolean applyLBStickinessPolicy(CreateLBStickinessPolicyCmd cmd) throws ResourceUnavailableException;

    boolean deleteLBStickinessPolicy(long stickinessPolicyId);

    /**
     * Assign a virtual machine, or list of virtual machines, to a load balancer.
     */
    boolean assignToLoadBalancer(long lbRuleId, List<Long> vmIds);

    boolean removeFromLoadBalancer(long lbRuleId, List<Long> vmIds);

    boolean applyLoadBalancerConfig(long lbRuleId) throws ResourceUnavailableException;

    /**
     * List instances that have either been applied to a load balancer or are eligible to be assigned to a load
     * balancer.
     * 
     * @param cmd
     * @return list of vm instances that have been or can be applied to a load balancer
     */
    List<? extends UserVm> listLoadBalancerInstances(ListLoadBalancerRuleInstancesCmd cmd);

    /**
     * List load balancer rules based on the given criteria
     * 
     * @param cmd
     *            the command that specifies the criteria to use for listing load balancers. Load balancers can be
     *            listed
     *            by id, name, public ip, and vm instance id
     * @return list of load balancers that match the criteria
     */
    List<? extends LoadBalancer> searchForLoadBalancers(ListLoadBalancerRulesCmd cmd);

    /**
     * List stickiness policies based on the given criteria
     * 
     * @param cmd
     *            the command specifies the load balancing rule id.
     * @return list of stickiness policies that match the criteria.
     */
    List<? extends StickinessPolicy> searchForLBStickinessPolicies(ListLBStickinessPoliciesCmd cmd);

    List<LoadBalancingRule> listByNetworkId(long networkId);

    LoadBalancer findById(long LoadBalancer);

}
