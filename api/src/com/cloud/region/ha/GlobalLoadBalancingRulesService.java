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

package com.cloud.region.ha;

import com.cloud.network.rules.LoadBalancer;
import org.apache.cloudstack.api.command.user.region.ha.gslb.*;

import java.util.List;

public interface GlobalLoadBalancingRulesService {

    /*
     * methods for managing life cycle of global load balancing rules
     */
    GlobalLoadBalancerRule createGlobalLoadBalancerRule(CreateGlobalLoadBalancerRuleCmd createGslbCmd);

    boolean deleteGlobalLoadBalancerRule(DeleteGlobalLoadBalancerRuleCmd deleteGslbCmd);

    GlobalLoadBalancerRule updateGlobalLoadBalancerRule(UpdateGlobalLoadBalancerRuleCmd updateGslbCmd);

    boolean revokeAllGslbRulesForAccount(com.cloud.user.Account caller, long accountId)
            throws com.cloud.exception.ResourceUnavailableException;

    /*
     * methods for managing sites participating in global load balancing
     */
    boolean assignToGlobalLoadBalancerRule(AssignToGlobalLoadBalancerRuleCmd assignToGslbCmd);

    boolean removeFromGlobalLoadBalancerRule(RemoveFromGlobalLoadBalancerRuleCmd removeFromGslbCmd);


    GlobalLoadBalancerRule findById(long gslbRuleId);

    List<GlobalLoadBalancerRule> listGlobalLoadBalancerRule(ListGlobalLoadBalancerRuleCmd listGslbCmd);

    List<LoadBalancer> listSiteLoadBalancers(long gslbRuleId);

}
