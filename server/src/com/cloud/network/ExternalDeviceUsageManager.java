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
package com.cloud.network;

import com.cloud.utils.component.Manager;

/* ExternalDeviceUsageManager implements a periodic task that retrieves and updates the network usage stats from all external load balancer and firewall devices.
 */

public interface ExternalDeviceUsageManager extends Manager {

    /**
     * updates the network usage stats for a LB rule, associated with an external LB device, that is being revoked as part of Delete LB rule or release IP actions
     * @param loadBalancerRuleId
     */
    public void updateExternalLoadBalancerNetworkUsageStats(long loadBalancerRuleId);

}
