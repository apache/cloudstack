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

import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.AffinityConflictException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.utils.component.Manager;
import com.cloud.vm.VirtualMachineProfile;

public interface DeploymentPlanningManager extends Manager {

    /**
     * Manages vm deployment stages: First Process Affinity/Anti-affinity - Call
     * the chain of AffinityGroupProcessor adapters to set deploymentplan scope
     * and exclude list Secondly, Call DeploymentPlanner - to use heuristics to
     * find the best spot to place the vm/volume. Planner will drill down to the
     * write set of clusters to look for placement based on various heuristics.
     * Lastly, Call Allocators - Given a cluster, allocators matches the
     * requirements to capabilities of the physical resource (host, storage
     * pool).
     *
     * @throws AffinityConflictException
     *
     *
     *
     */
    DeployDestination planDeployment(VirtualMachineProfile vmProfile, DeploymentPlan plan,
            ExcludeList avoids, DeploymentPlanner planner) throws InsufficientServerCapacityException, AffinityConflictException;

    String finalizeReservation(DeployDestination plannedDestination,
            VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoids, DeploymentPlanner planner)
            throws InsufficientServerCapacityException, AffinityConflictException;

    void cleanupVMReservations();

    DeploymentPlanner getDeploymentPlannerByName(String plannerName);
}
