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
package com.cloud.deploy;

import java.util.List;

import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.vm.VirtualMachineProfile;

/**
 */
public interface DeploymentClusterPlanner extends DeploymentPlanner {
    /**
     * This is called to determine list of possible clusters where a virtual
     * machine can be deployed.
     *
     * @param vm
     *            virtual machine.
     * @param plan
     *            deployment plan that tells you where it's being deployed to.
     * @param avoid
     *            avoid these data centers, pods, clusters, or hosts.
     * @return DeployDestination for that virtual machine.
     */
    List<Long> orderClusters(VirtualMachineProfile vm, DeploymentPlan plan, ExcludeList avoid)
            throws InsufficientServerCapacityException;

    PlannerResourceUsage getResourceUsage(VirtualMachineProfile vmProfile,
            DeploymentPlan plan, ExcludeList avoid) throws InsufficientServerCapacityException;

}
