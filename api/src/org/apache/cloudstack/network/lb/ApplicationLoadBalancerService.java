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

package org.apache.cloudstack.network.lb;

import java.util.List;

import org.apache.cloudstack.api.command.user.loadbalancer.ListApplicationLoadBalancersCmd;

import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.utils.Pair;

public interface ApplicationLoadBalancerService {
    
    ApplicationLoadBalancerRule createApplicationLoadBalancer(String name, String description, Scheme scheme, long sourceIpNetworkId, String sourceIp,
            int sourcePort, int instancePort, String algorithm, long networkId, long lbOwnerId) throws InsufficientAddressCapacityException,
            NetworkRuleConflictException, InsufficientVirtualNetworkCapcityException;
    
    boolean deleteApplicationLoadBalancer(long id);
    
    Pair<List<? extends ApplicationLoadBalancerRule>, Integer> listApplicationLoadBalancers(ListApplicationLoadBalancersCmd cmd);
    
    ApplicationLoadBalancerRule getApplicationLoadBalancer(long ruleId);

}
