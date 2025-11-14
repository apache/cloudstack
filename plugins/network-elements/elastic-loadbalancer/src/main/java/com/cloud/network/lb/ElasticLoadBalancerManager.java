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

import org.apache.cloudstack.api.command.user.loadbalancer.CreateLoadBalancerRuleCmd;

import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.user.Account;

public interface ElasticLoadBalancerManager {
    public static final int DEFAULT_ELB_VM_RAMSIZE = 128;            // 512 MB
    public static final int DEFAULT_ELB_VM_CPU_MHZ = 256;               // 500 MHz

    public boolean applyLoadBalancerRules(Network network, List<LoadBalancingRule> rules) throws ResourceUnavailableException;

    public LoadBalancer handleCreateLoadBalancerRule(CreateLoadBalancerRuleCmd lb, Account caller, long networkId) throws InsufficientAddressCapacityException,
        NetworkRuleConflictException;

    public void handleDeleteLoadBalancerRule(LoadBalancer lb, long callerUserId, Account caller);

    void expungeLbVmRefs(List<Long> vmIds, Long batchSize);
}
