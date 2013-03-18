// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.network.lb;

import java.util.List;

import org.apache.cloudstack.api.command.user.loadbalancer.CreateLBHealthCheckPolicyCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.CreateLBStickinessPolicyCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.CreateLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLBHealthCheckPoliciesCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLBStickinessPoliciesCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLoadBalancerRuleInstancesCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLoadBalancerRulesCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.UpdateLoadBalancerRuleCmd;

import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.rules.HealthCheckPolicy;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.StickinessPolicy;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;


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

    boolean deleteLBStickinessPolicy(long stickinessPolicyId, boolean apply);

    /**
     * Create a healthcheck policy to a load balancer from the given healthcheck
     * parameters in (name,value) pairs.
     *
     * @param cmd
     *            the command specifying the stickiness method name, params
     *            (name,value pairs), policy name and description.
     * @return the newly created stickiness policy if successfull, null
     *         otherwise
     * @thows NetworkRuleConflictException
     */
    public HealthCheckPolicy createLBHealthCheckPolicy(CreateLBHealthCheckPolicyCmd cmd);
    public boolean applyLBHealthCheckPolicy(CreateLBHealthCheckPolicyCmd cmd) throws ResourceUnavailableException;
    boolean deleteLBHealthCheckPolicy(long healthCheckPolicyId, boolean apply);

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
    Pair<List<? extends LoadBalancer>, Integer> searchForLoadBalancers(ListLoadBalancerRulesCmd cmd);

    /**
     * List stickiness policies based on the given criteria
     *
     * @param cmd
     *            the command specifies the load balancing rule id.
     * @return list of stickiness policies that match the criteria.
     */
    List<? extends StickinessPolicy> searchForLBStickinessPolicies(ListLBStickinessPoliciesCmd cmd);

    /**
     * List healthcheck policies based on the given criteria
     *
     * @param cmd
     *            the command specifies the load balancing rule id.
     * @return list of healthcheck policies that match the criteria.
     */

    List<? extends HealthCheckPolicy> searchForLBHealthCheckPolicies(ListLBHealthCheckPoliciesCmd cmd);

    List<LoadBalancingRule> listByNetworkId(long networkId);

    LoadBalancer findById(long LoadBalancer);
   public void updateLBHealthChecks() throws ResourceUnavailableException;
}
