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
package com.cloud.network.element;

import java.util.List;

import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.lb.LoadBalancingRule;

public interface LoadBalancingServiceProvider extends NetworkElement, IpDeployingRequester {
    /**
     * Apply rules
     *
     * @param network
     * @param rules
     * @return
     * @throws ResourceUnavailableException
     */
    boolean applyLBRules(Network network, List<LoadBalancingRule> rules) throws ResourceUnavailableException;

    /**
     * Validate rules
     *
     * @param network
     * @param rule
     * @return true/false. true should be return if there are no validations.
     *false should be return if any oneof the validation fails.
     * @throws
     */
    boolean validateLBRule(Network network, LoadBalancingRule rule);

    List<LoadBalancerTO> updateHealthChecks(Network network, List<LoadBalancingRule> lbrules);
}
