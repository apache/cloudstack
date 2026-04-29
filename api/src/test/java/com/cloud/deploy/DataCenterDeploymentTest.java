/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.deploy;

import junit.framework.TestCase;
import org.junit.Assert;

public class DataCenterDeploymentTest extends TestCase {

    private long zoneId = 1L;
    private long hostId = 2L;

    DataCenterDeployment plan = new DataCenterDeployment(zoneId);

    private void verifyHostPriority(Integer priority) {
        Assert.assertEquals(priority, plan.getHostPriorities().get(hostId));
    }

    public void testHostPriorities() {
        verifyHostPriority(null);

        plan.adjustHostPriority(hostId, DeploymentPlan.HostPriorityAdjustment.DEFAULT);
        verifyHostPriority(0);

        plan.adjustHostPriority(hostId, DeploymentPlan.HostPriorityAdjustment.HIGHER);
        verifyHostPriority(1);

        plan.adjustHostPriority(hostId, DeploymentPlan.HostPriorityAdjustment.LOWER);
        verifyHostPriority(0);

        plan.adjustHostPriority(hostId, DeploymentPlan.HostPriorityAdjustment.LOWER);
        verifyHostPriority(-1);

        plan.adjustHostPriority(hostId, DeploymentPlan.HostPriorityAdjustment.HIGHER);
        verifyHostPriority(0);

        plan.adjustHostPriority(hostId, DeploymentPlan.HostPriorityAdjustment.HIGHER);
        verifyHostPriority(1);
    }
}
