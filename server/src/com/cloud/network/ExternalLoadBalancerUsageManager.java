/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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

package com.cloud.network;

import com.cloud.utils.component.Manager;

/* ExternalLoadBalancerUsageManager implements a periodic task that retrieves and updates the network usage stats from all external load balancer devices.
 */

public interface ExternalLoadBalancerUsageManager extends Manager{

    /**
     * updates the network usage stats for a LB rule, associated with an external LB device, that is being revoked as part of Delete LB rule or release IP actions
     * @param loadBalancerRuleId
     */
    public void updateExternalLoadBalancerNetworkUsageStats(long loadBalancerRuleId);
    

}
