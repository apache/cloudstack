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

import org.apache.cloudstack.context.CallContext;

import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.rules.LbStickinessMethod;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.user.Account;

public interface LoadBalancingRulesManager extends LoadBalancingRulesService {

    LoadBalancer createPublicLoadBalancer(String xId, String name, String description, 
            int srcPort, int destPort, long sourceIpId, String protocol, String algorithm, boolean openFirewall, CallContext caller)
            throws NetworkRuleConflictException;

    boolean removeAllLoadBalanacersForIp(long ipId, Account caller, long callerUserId);
    boolean removeAllLoadBalanacersForNetwork(long networkId, Account caller, long callerUserId);
    List<LbDestination> getExistingDestinations(long lbId);
    List<LbStickinessPolicy> getStickinessPolicies(long lbId);
    List<LbStickinessMethod> getStickinessMethods(long networkid);
    List<LbHealthCheckPolicy> getHealthCheckPolicies(long lbId);

    /**
     * Remove vm from all load balancers
     * @param vmId
     * @return true if removal is successful
     */
    boolean removeVmFromLoadBalancers(long vmId);
    boolean applyLoadBalancersForNetwork(long networkId, Scheme scheme) throws ResourceUnavailableException;
    String getLBCapability(long networkid, String capabilityName);
    boolean configureLbAutoScaleVmGroup(long vmGroupid, String currentState) throws ResourceUnavailableException;
    boolean revokeLoadBalancersForNetwork(long networkId, Scheme scheme) throws ResourceUnavailableException;

    boolean validateLbRule(LoadBalancingRule lbRule);

    void removeLBRule(LoadBalancer rule);

    void isLbServiceSupportedInNetwork(long networkId, Scheme scheme);
}
